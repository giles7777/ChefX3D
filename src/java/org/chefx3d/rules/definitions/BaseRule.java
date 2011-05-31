/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.definitions;

//External Imports
import java.util.*;

import java.util.prefs.Preferences;

import org.j3d.util.I18nManager;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.RuleCollisionChecker;

import org.chefx3d.catalog.CatalogManager;

import org.chefx3d.preferences.PersistentPreferenceConstants;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.Rule;

import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.RuleCollisionHandler;
import org.chefx3d.rules.util.ChildrenMatches;

import org.chefx3d.ui.PopUpConfirm;
import org.chefx3d.ui.PopUpMessage;
import org.chefx3d.ui.StatusBar;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.ApplicationParams;

/**
 * Abstract definition for Rule. Rule specifies a single rule and the processes
 * that make up the rule. A Rule returns a boolean response about the condition
 * that determines if a Command can be executed or must be rejected and
 * possibly results in an automatically handled response in the environment.
 *
 * Also contains utility functions for all rule classes.
 *
 * @author Ben Yarger
 * @version $Revision: 1.21 $
 */
public abstract class BaseRule implements Rule {
	
    /** Should we log failures reasons for rules */
    protected static boolean logFailures = false;

	/** Collision checking handler for BaseRule subs */
	protected static RuleCollisionHandler rch;
	
    /** The ChildrenMatches collision results */
    protected static ChildrenMatches collisionResults;
    
    /** Accessor to catalog lookup */
    protected CatalogManager catalogManager;

    /** Error reporting utility */
    protected ErrorReporter errorReporter;

    /** The world model, where the data is stored */
    protected WorldModel model;

    /** Collision checking instance */
    protected RuleCollisionChecker collisionChecker;

    /** AV3DView instance */
    protected EditorView view;
    
    /** A singleton that tracks all the command queues */
    protected CommandSequencer sequencer;

    /** StatusBar messenger */
    protected StatusBar statusBar;

    /** Displays a pop up message */
    protected PopUpMessage popUpMessage;

    /** Displays a pop up confirm message */
    protected PopUpConfirm popUpConfirm;

    /** Translation utility */
    protected I18nManager intl_mgr;

    /** The type of the rule implementation */
    protected RULE_TYPE ruleType;

	/** Map of collision data, per entity */
    protected Map<Entity, ChildrenMatches> matchesMap;
	
    /** The current state of the process list */
    protected RuleEvaluationResult result;
    
	/**
	 * Constructor
	 * 
	 * @param errorReporter
	 * @param model
	 * @param view
	 */
	public BaseRule(
	        ErrorReporter errorReporter,
            WorldModel model,
            EditorView view ) {
	    
        this.errorReporter = errorReporter;
		this.model = model;
        this.view = view;	
		this.catalogManager = view.getCatalogManager();
		
		collisionChecker = view.getRuleCollisionChecker();
	    
		statusBar = StatusBar.getStatusBar();
		popUpMessage = PopUpMessage.getInstance();
		popUpConfirm = PopUpConfirm.getInstance();
		intl_mgr = I18nManager.getManager();
		sequencer = CommandSequencer.getInstance();
	}
	
    //---------------------------------------------------------------
    // Rule methods
    //---------------------------------------------------------------

    /**
     * All instances of Rule should call processRule with the
     * Command and transient state to convert and process.
     * Convert expects the Command to
     * implement RuleDataAccessor. If it doesn't it returns true and
     * logs a note to the console.
     *
     * @param command Command that needs to be converted
     * @param result The state of the rule processing
     * @return A RuleEvaluationResult object containing the results
     */
    public RuleEvaluationResult processRule(
            Command command, 
            RuleEvaluationResult result) {
                
        /*
         * Perform rule override analysis.
         * Return true if overriding standard rules.
         */
        String appName = (String) ApplicationParams.get(
                ApplicationParams.APP_NAME);


        // check here for class names and such
        Class currentClass = this.getClass();

        HashSet<String> ignoreList = command.getIgnoreRuleList();
        if(ignoreList != null) {
           if(ignoreList.contains(currentClass.getName())) {
               result.setResult(true);
               return(result);           
           }
        }

        Preferences prefs = Preferences.userRoot().node(appName);
        Boolean ruleOverride = prefs.getBoolean(
        		PersistentPreferenceConstants.OVERRIDE_RULES_KEY, 
        		false);

        if(ruleType != null &&
                ruleType == RULE_TYPE.STANDARD &&
                ruleOverride == true){
            
            result.setResult(true);
            return(result);           
        }

        if(command instanceof RuleDataAccessor){

            RuleDataAccessor dataAccessor = (RuleDataAccessor)command;

            Entity entity = dataAccessor.getEntity();

            // Attempted catch to pevent NPE described in bugs 97 & 42 (BJY)
            if(entity == null){

                errorReporter.debugReport(
					"Rule found command entity was null. Command description: "+
					command.getDescription(), null);
				
                result.setResult(false);
                return(result);           
            }

            return performCheck(entity, command, result);

        } else {

            errorReporter.debugReport(
                    "CommandToRuleAdapter did not receive an object of type "+
                    "RuleDataAccessor.",
                    null);

            result.setResult(true);
            return(result);           
        }
    }


    //-------------------------------------------------------------------------
    // Abstract method definitions
    //-------------------------------------------------------------------------

    /**
     * Abstract definition of performCheck which plucks apart the Entity and
     * WorldModel for the data expected by the rule the adapter communicates
     * with.
     *
     * @param entity Entity object
     * @param command Command object
     * @param result The state of the rule processing
     * @return A RuleEvaluationResult object containing the results
     */
    protected abstract RuleEvaluationResult performCheck(
            Entity entity,
            Command command, 
            RuleEvaluationResult result);

    //-------------------------------------------------------------------------
    // Public methods
    //-------------------------------------------------------------------------

	/**
	 * Initialize
	 */
	public static void initialize(RuleCollisionHandler col_handler) {
		rch = col_handler;
		collisionResults = rch.getCollisionResults();
	}

    //-------------------------------------------------------------------------
    // Protected methods
    //-------------------------------------------------------------------------

    /**
     * Set the result object 
     * 
     * @param result
     */
    protected void setResult(RuleEvaluationResult result) {
        this.result = result;        
    }

    /**
     * Get the newlyIssuedCommands queue
     *
     * @return List<Command> of newly issued commands
     */
    protected List<Command> getNewlyIssuedCommandsFullHistory(){
        return sequencer.getNewlyIssuedCommandList();
    }
    
    /**
     * Get the newlyIssuedCommands queue
     *
     * @return List<Command> of newly issued commands
     */
    protected List<Command> getNewlyIssuedCommands(){
        return sequencer.getNewlyIssuedCommandList();
    }


    /**
     * Add a command to the newlyIssuedCommands queue
     *
     * @param command Command to add
     */
    protected void addNewlyIssuedCommand(Command command){
        sequencer.addNewlyIssuedCommand(command);
    }

    /**
     * Remove a command from the newlyIssuedCommands queue
     *
     * @param command Command to remove
     */
    protected void removeNewlyIssuedCommand(Command command){
        sequencer.removeNewlyIssuedCommand(command);
    }

    /**
     * Add a list of commands to the newlyIssuedCommands ArrayLsit
     * @param cmdList ArrayList<Command> of commands
     */
    protected void addNewlyIssuedCommand(ArrayList<Command> cmdList){
        for (int i = 0; i < cmdList.size(); i++) {
            addNewlyIssuedCommand(cmdList.get(i));
        }        
    }
    

    /**
     * Get the list of entities that are set to be removed with the next
     * command execution.
     *
     * @return ArrayList<Entity> of entities set to be removed
     */
    protected ArrayList<Entity> getNewlyIssuedRemoveCommandEntities() {

        List<Command> newlyIssuedCommands = 
            sequencer.getNewlyIssuedCommandList();
        
        ArrayList<Entity> removeEntityList =
            new ArrayList<Entity>();

        for (int i = 0; i < newlyIssuedCommands.size(); i++) {

            Command cmd = newlyIssuedCommands.get(i);

            if (cmd instanceof RemoveEntityChildCommand) {

                removeEntityList.add(
                        ((RemoveEntityChildCommand) cmd).getEntity());
            } else if (cmd instanceof RemoveEntityChildTransientCommand) {

                removeEntityList.add(
                        ((RemoveEntityChildTransientCommand) cmd).getEntity());
            }
        }

        return removeEntityList;
    }

    /**
     * Log a failure to the console.
     */
	
    protected void logFailure(String st) {
        System.out.println("Rule Failure: " + getClass().getSimpleName() + " reason: " + st);
    }
}
