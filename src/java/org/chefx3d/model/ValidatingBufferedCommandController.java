/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

//External Imports

//Internal Imports
import java.util.ArrayList;
import java.util.Stack;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ErrorReporter;

/**
 * An instance of the Command queue controller that optionally will
 * perform a validation step on Commands being submitted for execution.
 *
 * @author Rex Melton
 * @version $Revision: 1.15 $
 */
public class ValidatingBufferedCommandController extends BufferedCommandController {

    /** Scratch array for extracting the commands to execute */
    protected CommandWrapper[] cmd;
    
    /** The command validator, optional, may be null */
    protected CommandValidator validator;
        
    /**
     * Create a Command queue object.
     */
    public ValidatingBufferedCommandController() {
        super();
        init();
    }

    /**
     * Create a commandHistory object. Set the maximum size to <code>size</code>
     * and create the data structure to store the <code>Command</code>s
     *
     * @param size The maximum size of the stack
     */
    public ValidatingBufferedCommandController(int size) {
        super(size);
        init();
    }

    //----------------------------------------------------------
    // Methods defined by CommandController
    //----------------------------------------------------------

    /**
     * Execute all buffered commands on the queue
     */
    public synchronized void processCommands() {
    	
        if (bufferedCommands.size() > 0) {

        	isProcessing = true;

            saveUpToDate = false;
            
            Boolean throttle = 
            	(Boolean) ApplicationParams.get("throttleCommands");
            
            if (throttle != null && throttle == true) {
            
	            CommandWrapper cmd = null;
	            synchronized(bufferedCommands) {
	            	
	            	cmd = bufferedCommands.get(0);
	            	bufferedCommands.remove(0);
	            }
	            
	            boolean isValid = true;
	            if ((validator != null) && (cmd instanceof ExecuteCommand)) {
	                isValid = validator.validate(((ExecuteCommand)cmd).command);
	            }
	            
	            // update the command wrapper with the validated version 
	            // of the command
	            ((ExecuteCommand)cmd).setValidatedCommand(
	                    validator.getValidatedCommand());
	            
	            if (isValid) {	               
	                cmd.process();                    
	            } else {
	                cmd.reject();
	            }
	            
            } else {
 
	            int num;
	            synchronized(bufferedCommands) {
	                num = bufferedCommands.size();
	                resize(num);
	                bufferedCommands.toArray(cmd);
	                bufferedCommands.clear();
	            }
	            
	            for (int i = 0; i < num; i++) {
	                
	                boolean isValid = true;
	                
	                if (cmd[i] instanceof ExecuteCommand) {
	                	
		                Command command = ((ExecuteCommand)cmd[i]).command;
		                
		                // Eliminate redundancy in commands
		                if (i < (num - 1)) {
		                	
		                	if (cmd[i+1] instanceof ExecuteCommand) {
		                		
			                	Command commandNext = 
			                		((ExecuteCommand)cmd[i+1]).command;
			                	
			                	if (command instanceof RuleDataAccessor && 
			                	        command.isTransient()) {
			                		
			                		boolean areEqual = 
			                			((RuleDataAccessor)command).isEqualTo(
			                					commandNext);
			                		
			                		if (areEqual) {
			                			cmd[i] = null;
			                			continue;
			                		}
			                		
			                	}									
		                	}
		                }
	                }
	                
	                // Validate command
	                if ((validator != null) && (cmd[i] instanceof ExecuteCommand)) {
	                    
	                    ExecuteCommand exeCmd = (ExecuteCommand)cmd[i];
	                    
	                    // validate the command
	                    isValid = validator.validate(exeCmd.command);
	                    
	                    // update the command wrapper with the validated version 
	                    // of the command
	                    exeCmd.setValidatedCommand(validator.getValidatedCommand());

	                }
	                
	                if (isValid) {
	                    cmd[i].process();                    
	                } else {
	                    cmd[i].reject();
	                }

	                cmd[i] = null;
	            }
            }
            
            isProcessing = false;
        }        
        
    }

    /**
     * Add a <code>Command</code> to the execution queue without performing it.
     *
     * @param Command the action to remember
     */
    public void execute(Command command) {
        
        synchronized(bufferedCommands) {
            // Create the command wrapper
            ExecuteCommand cmd = new ExecuteCommand(
                command,
                maxSize,
                undoCommands,
                redoCommands,
                commandListeners,
                errorReporter);
            
            // Add the wrapper to the list of buffered commands
            bufferedCommands.add(cmd);
        }
    }
    
    /**
     * Request a command to be validated against the current rules set
     * 
     * @param cmd The command to validate
     * @return True if valid, false otherwise
     */
    public boolean validate(Command cmd) {
        
        boolean isValid = true;
        if ((validator != null)) {
            isValid = validator.validate(cmd);
        }

        return isValid;
        
    }

    /**
     * Execute the <code>Command</code> at the top of the stacks
     */
    public void undo() {

        if (canUndo() == true) {

            synchronized(bufferedCommands) {
                // Get the command from the undo stack and perform undo
                Command command = undoCommands.pop();
                
                // Create the command wrapper
                UndoCommand cmd = new UndoCommand(
                    command,
                    maxSize,
                    undoCommands,
                    redoCommands,
                    commandListeners,
                    errorReporter);
                
                // Add the wrapper to the queue of buffered commands
                bufferedCommands.add(cmd);
            }
        } else {
            String msg = i18n_mgr.getString(CANNOT_UNDO_COMMAND_MSG);
            errorReporter.messageReport(msg);
        }
    }

    /**
     * Move the <code>Command</code> from the top of the redo stack
     * to the execution queue.
     */
    public void redo() {

        if (canRedo() == true) {

            synchronized(bufferedCommands) {
                // Get the command from the redo stack and perform redo
                Command command = redoCommands.pop();
                
                // Create the command wrapper
                RedoCommand cmd = new RedoCommand(
                    command,
                    maxSize,
                    undoCommands,
                    redoCommands,
                    commandListeners,
                    errorReporter);
                
                // Add the wrapper to the list of buffered commands
                bufferedCommands.add(cmd);
            }
        } else {
            String msg = i18n_mgr.getString(CANNOT_REDO_COMMAND_MSG);
            errorReporter.messageReport(msg);
        }
    }
    
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Set the current CommandValidator
     * 
     * @param validator The new CommandValidator. If null,
     * validation is disabled and all commands submitted for
     * execution will be queued.
     */
    public void setCommandValidator(CommandValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Initialize locals
     */
    private void init() {
        cmd = new CommandWrapper[100];
    }
    
    /**
     * Resize the command array as necessary
     * 
     * @param num The number of required entries
     */
    private void resize(int num) {
        if (cmd.length < num) {
            cmd = new CommandWrapper[num];
        }
    }
}
