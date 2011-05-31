/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.interpreters;

//External Imports
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.DefaultRuleEvaluationResult;
import org.chefx3d.rules.rule.CommandDataCenter;

import org.chefx3d.rules.rule.RuleEngine;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.rules.util.CommandDataExtractor;
import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.CheckStatusReportElevation;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.RuleCollisionChecker;
import org.chefx3d.view.common.StatusReporter;

import org.chefx3d.ui.PopUpConfirm;
import org.chefx3d.ui.PopUpMessage;
import org.chefx3d.ui.StatusBar;

/**
 * Implementation of CommandValidator. Has a list of commands to
 * look for and will search the list for a matching command. If found, the
 * assigned RuleEngine is extracted and used to process the command.
 *
 * @author Ben Yarger
 * @version $Revision: 1.42 $
 */
public class ValidatingCommandInterpreter implements CommandValidator {

    /** CommandDataCenter reference for looking up Command/RuleEngine matches */
    protected CommandDataCenter commandDataCenter;

    /** Access to selection object status color */
    protected StatusReporter statusReporter;

    /** Error reporting utility */
    protected ErrorReporter errorReporter;

	/** The ediotr object */
	protected EditorView view;
	
    /** Manages the status levels to report back as color to the selection box */
    protected CheckStatusReportElevation statusManager;

    /** Contains all commands issued by rule engines for the current command */
    protected ArrayList<Command> validationNewlyIssuedCommands;

    /** Reference to the collision checker used by the AV3D view */
    protected RuleCollisionChecker collisionChecker;
    
    /** A singleton that tracks all the command queues */
    protected CommandSequencer sequencer;
    
    /** The status bar ui widget used to display messages */
    protected StatusBar statusBar;
    
    /** Displays a pop up message */
    protected PopUpMessage popUpMessage;

    /** Displays a pop up confirm message */
    protected PopUpConfirm popUpConfirm;
    
    protected Command validatedCmd;
    
    /**
     * Default constructor
     */
    public ValidatingCommandInterpreter(
		CommandDataCenter commandDataCenter, 
		ErrorReporter errorReporter, 
		EditorView view,
		CheckStatusReportElevation statusManager) {

		this.commandDataCenter = commandDataCenter;
		this.view = view;
		this.errorReporter = errorReporter;
		this.statusManager = statusManager;

        collisionChecker = view.getRuleCollisionChecker();
        statusReporter = view.getStatusReporter();
        statusBar = StatusBar.getStatusBar();
        sequencer = CommandSequencer.getInstance();
        validationNewlyIssuedCommands = new ArrayList<Command>();
        popUpMessage = PopUpMessage.getInstance();
        popUpConfirm = PopUpConfirm.getInstance();

        // Do this so the auto add utility can do its own separate validations
        // before deciding to add a command onto the queue.
        RuleUtils.setCommandDataCenter(commandDataCenter);
        RuleUtils.setCatalogManager(view.getCatalogManager());
    }

    //----------------------------------------------------------
    //  Methods defined by CommandValidator
    //----------------------------------------------------------

    /**
     * Return whether this command passes the validation process.
     *
     * @param command The command to check.
     */
    public boolean validate(Command command) {

        // the return value to use
        boolean valid = true;

        // default is return the same command
        validatedCmd = command;

        // If rules are disabled, allow everything.
        Boolean enableRules = 
            (Boolean)ApplicationParams.get(ApplicationParams.ENABLE_RULES);
        
        if (enableRules != null && !enableRules) {
            return valid;
        }
        
        // If the command implements RuleBypassFlag and the flag is true
        // immediately return true and allow the rule to execute.
        if (command instanceof RuleBypassFlag && 
                ((RuleBypassFlag)command).bypassRules()) {
            
            return valid;
        }

        // Always clear out the surrogates first thing
        collisionChecker.clearSurrogates();
        
        // Reset the status manager and clear out the validation class
        // newly issued command list. Lastly set the WorldModel reference
        // to null so we are sure to only reference the one from the current
        // command.
        statusManager.resetElevationLevel();
        
        // Reset any and all commands on the pending, approved, and newlyIssued 
        // queues.  these will be populated through the processing of this 
        // command.
        sequencer.clearAll();
        
        // push all the commands onto the pending queue by first seeing if
        // this is a multi command.  if it is then extract the commands.  if
        // it is not then just add the single command to the queue
        List<Command> commandList = CommandDataExtractor.expandAllMultiCommand(command);
        if (commandList.size() > 0) {           
            int len = commandList.size();
            for (int i = 0; i < len; i++) {
                sequencer.addPendingCommand(commandList.get(i));
            }
        } else {
            sequencer.addPendingCommand(command);
        }
                
        // define working variables to use throughout the loop
        WorldModel model = null;

        // get the list of commands to process
        commandList = sequencer.getPendingCommandList();
        Iterator<Command> itr = commandList.iterator();
        
        // Process each command in the pending list.
        //  if the command is approved 
        //      move it to the approved list
        //      move the newly issued commands to the pending list
        //  else if the command fails
        //      if the kill all flag is set
        //          remove all commands from all queues
        //      else
        //          clear just the newly issued queue
        //      execute the resetToStart and add them to the approved list
        while (itr.hasNext()) {
            
            // clear any status bar messages
            statusBar.clearStatusBar();
            
            // get the command to process
            Command cmd = itr.next();
            
            // get the world model if we can
            if(model == null && cmd instanceof RuleDataAccessor){
                model = ((RuleDataAccessor)cmd).getWorldModel();
            }

            // Look up the rule engine to use
            RuleEngine ruleEngine = commandDataCenter.matchCommand(cmd);

            // process the command through the matched rules engine.  the 
            // command is considered approved if the isApproved flag remains
            // true after all rules are processed
            if (ruleEngine != null) {
                
                // create the status result object used to track state
                RuleEvaluationResult result = new DefaultRuleEvaluationResult();

                // process the list of commands sequentially
                result = ruleEngine.processRules(cmd, result);

                // elevate the status to the max level generated by the rules 
                // processing pass
                statusManager.setElevationLevel(result.getStatusValue());
                
                // if the command failed in some way then we need to decide 
                // how to reset the scene back to the previous state
                if (!result.isApproved()) {
                    
                    // update the UI with the failure status
                    statusReporter.setSelectionStatus(
                            statusManager.getStatusColor());
                    
                    // Perform the is not approved command action
                    NOT_APPROVED_ACTION failureAction = 
                    	result.getNotApprovedAction();
                    
                    boolean resetOriginal = true;
                    
                    switch (failureAction) {
                    
                    case CLEAR_ALL_COMMANDS:
                    	// clear all queues
                        sequencer.clearAll();
                    	break;
                    case CLEAR_CURRENT_COMMAND:
                    	sequencer.removeCommand(cmd);
                    	break;
                    case CLEAR_CURRENT_COMMAND_NO_RESET:
                    	sequencer.removeCommand(cmd);
                    	resetOriginal = false;
                    	break;
                    case CLEAR_NEWLY_ISSUED_COMMANDS:
                    default:
                    	// clear just the new commands created by the one 
                        // that failed
                        sequencer.clearNewlyIssuedCommands();
                    	break;
                    }
                    
                    if (resetOriginal) {
	                    // Reset the original command back to its starting state
	                    List<Command> resetList = resetToStart(command);
	                    
	                    // finally add the reset commands to the approved list
	                    // so we put the scene back the way it should be
	                    for (int i = 0; i < resetList.size(); i++) {
	                        sequencer.addApprovedCommand(resetList.get(i));
	                    }
                    }
                    
                    valid = false;
					
					view.reset();
                                        
                } else {
                    
                    // add the surrogate to the scene so that it is checked 
                    // against for all subsequent commands
                    SceneManagementUtility.addSurrogate(collisionChecker, cmd);
                    
                    // move the command from the pending queue to the 
                    // approved queue
                    sequencer.removePendingCommand(cmd);
                    sequencer.addApprovedCommand(cmd);
                    
                    // move any newly issued commands to the pending list 
                    sequencer.moveNewlyIssuedCommandsToPending();
                    
                }
                
            }
            
        }  
        
        // Process the approved list
        //  wrap all approved commands into a single multi-command
        //  perform a forced execution of the command
        ArrayList<Command> approvedList = 
            (ArrayList<Command>)sequencer.getApprovedCommandList();
        
        validatedCmd = 
            new MultiCommand(
                approvedList,
                command.getDescription(),
                true,
                true);

        return valid;
        
    }
    
    /**
     * Get the command that was created by the validation process 
     * 
     * @return The transformed command
     */
    public Command getValidatedCommand() {
        return validatedCmd;
    }

    //----------------------------------------------------------
    //  Local Methods
    //----------------------------------------------------------

    /**
     * Reset all commands back to their last known good values.
     *
     * @param command Command to reset
     * @return The reset command list
     */
    protected ArrayList<Command> resetToStart(Command command) {

        /*
         * Proceed with rule execution.
         *
         * Add commands are outright rejected. All other cases are handled
         * accordingly.
         */
        ArrayList<Command> commandList;

        if (command instanceof MultiCommand) {

            commandList = ((MultiCommand)command).getCommandList();

        } else if (command instanceof MultiTransientCommand) {

            commandList = ((MultiTransientCommand)command).getCommandList();

        } else if (command instanceof MultiRevertCommand) {

            commandList = ((MultiRevertCommand)command).getCommandList();

        } else {

            commandList = new ArrayList<Command>();
            commandList.add(command);
            
        }

        int len = commandList.size();
        for (int i = len - 1; i >= 0; i--) {

            Command cmd = commandList.get(i);
            
            if (cmd instanceof RuleDataAccessor && 
                    ((RuleDataAccessor)command).shouldCommandDie()) {
                
                commandList.remove(i);
                
            } else if (cmd instanceof AddEntityCommand ||
                    cmd instanceof AddEntityChildCommand ||
                    cmd instanceof AddEntityChildTransientCommand ||
                    cmd instanceof AddSegmentCommand ||
                    cmd instanceof AddSegmentTransientCommand ||
                    cmd instanceof AddVertexCommand ||
                    cmd instanceof AddVertexTransientCommand ||
                    cmd instanceof RemoveEntityChildCommand ||
                    cmd instanceof RemoveEntityCommand) {

                commandList.remove(i);

            } else if (cmd instanceof MultiCommand){
                
                ((MultiCommand)command).resetToStart();

            } else if (cmd instanceof MultiTransientCommand) {

                // do nothing
 
            } else if (cmd instanceof MultiRevertCommand){

                ((MultiRevertCommand)command).resetToStart();

            } else if (cmd instanceof RuleDataAccessor) {
                
                ((RuleDataAccessor)cmd).resetToStart();
                
            }
        }

        return commandList;
    }

}
