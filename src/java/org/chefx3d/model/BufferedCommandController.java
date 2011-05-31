/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import org.j3d.util.I18nManager;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
* A history of commands changed within the model.
*
* @author Russell Dodds
* @version $Revision: 1.9 $
*/
public class BufferedCommandController implements CommandController {

    private static final int DEFAULT_SIZE = 200;

    /** Message that a command cannot be redone */
    protected static final String CANNOT_REDO_COMMAND_MSG =
        "org.chefx3d.model.BufferedCommandController.cannotRedoMsg";

    /** Message that a command cannot be undone */
    protected static final String CANNOT_UNDO_COMMAND_MSG =
        "org.chefx3d.model.BufferedCommandController.cannotUndoMsg";

    /** The size of the stack of <code>Command</code>s. */
    protected int maxSize;

    /** The list of <code>Command</code>s able to undo. */
    protected Stack<Command> undoCommands;

    /** The list of <code>Command</code>s able to redo. */
    protected Stack<Command> redoCommands;

    /** The list of <code>CommandWrapper</code>s to process. */
    protected ArrayList<CommandWrapper> bufferedCommands;

    /** The list of CommandHistoryListeners. */
    protected ArrayList<CommandListener> commandListeners;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;

	/** I18N manager for sourcing messages */
	protected I18nManager i18n_mgr;
	
    /**
     * Flag to indicate if the state is saved or if we
     * need to save to be current.
     */
    protected boolean saveUpToDate;

    protected boolean isProcessing = false;
    protected boolean isChained = false;

    /**
     * Create a commandHistory object. Set the maximum size to
     * <code>DEFAULT_SIZE</code> and create the data structure to store the
     * <code>Command</code>s
     */
    public BufferedCommandController() {
        this(DEFAULT_SIZE);
    }

    /**
     * Create a commandHistory object. Set the maximum size to <code>size</code>
     * and create the data structure to store the <code>Command</code>s
     *
     * @param size The maximum size of the stack
     */
    public BufferedCommandController(int size) {

        saveUpToDate = false;
        maxSize = size;

        undoCommands = new Stack<Command>();
        redoCommands = new Stack<Command>();
        bufferedCommands = new ArrayList<CommandWrapper>();
        
        commandListeners = new ArrayList<CommandListener>();

		i18n_mgr = I18nManager.getManager();
        errorReporter = DefaultErrorReporter.getDefaultReporter();

    }

    /**
     * Execute all buffered commands
     *
     */
    public void processCommands() {

        CommandWrapper cmd;

        if (bufferedCommands.size() > 0) {
        	
//System.out.println("BufferedCommandController.processCommands");
//System.out.println("bufferedCommands.size: " + bufferedCommands.size());

            saveUpToDate = false;

            for (int i = 0; i < bufferedCommands.size(); i++) {
                cmd = bufferedCommands.get(i);
                cmd.process();
            }

            bufferedCommands.clear();

            isProcessing = false;
        }

    }

    /**
     * Add a <code>Command</code> to the history list without performing it.
     *
     * @param Command the action to remember
     */
    public void execute(Command command) {

//System.out.println("BufferedCommandController.execute");

    	isProcessing = true;

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
    
    /**
     * Request a method to be validated against the current rules set
     * 
     * @param cmd The command to validate
     * @return True if valid, false otherwise
     */
    public boolean validate(Command cmd) {
        return true;        
    }
    
    /**
     * Force execution of a command without putting it on the bufferedCommands
     * stack.
     * 
     * @param command The command
     */
    public void forceExecution(Command command) {
    	
    	ExecuteCommand cmd = new ExecuteCommand(
    			command, 
    			maxSize, 
    			undoCommands, 
    			redoCommands, 
    			commandListeners, 
    			errorReporter);
    	
    	cmd.process();
    }

    /**
     * Execute the <code>Command</code> at the top of the stacks
     */
    public void undo() {

//System.out.println("BufferedCommandController.undo");

        if (canUndo() == true) {

        	isProcessing = true;

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

            // Add the wrapper to the list of buffered commands
            bufferedCommands.add(cmd);

        } else {
			String msg = i18n_mgr.getString(CANNOT_UNDO_COMMAND_MSG);
            errorReporter.messageReport(msg);
        }

    }

    /**
     * Returns true if there are any <code>Command</code>s to undo
     */
    public boolean canUndo() {
        if (undoCommands.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Execute the <code>Command</code> at the top of the redo stack
     */
    public void redo() {

        if (canRedo() == true) {

        	isProcessing = true;

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

        } else {
			String msg = i18n_mgr.getString(CANNOT_REDO_COMMAND_MSG);
            errorReporter.messageReport(msg);
        }
    }

    /**
     * Returns true if there are any <code>Command</code>s to redo
     */
    public boolean canRedo() {
        if (redoCommands.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove all <code>Command</code>s from the history
     */
    public void clear() {

        undoCommands.clear();
        redoCommands.clear();

        // finally, notify listeners of the change
        for (int i = 0; i < commandListeners.size(); i++) {
            CommandListener l = commandListeners.get(i);
            l.commandCleared();
        }

    }

    /**
     * Set the maximum size of the command history
     *
     * @param size The new size to assign
     */
    public void setSize(int size) {
        maxSize = size;
    }

    /**
     * Return the maximum size assigned to the history
     */
    public int getSize() {
        return maxSize;
    }

    /**
     * Return the description of the <code>Command</code> to be executed if
     * <code>undo()</code> is called.
     */
    public String getUndoDescription() {
        if (canUndo() == true) {
            return undoCommands.peek().getDescription();
        } else {
            return null;
        }
    }

    /**
     * Return the description of the <code>Command</code> to be executed if
     * <code>redo()</code> is called.
     */
    public String getRedoDescription() {
        if (canRedo() == true) {
            return redoCommands.peek().getDescription();
        } else {
            return null;
        }
    }

    /**
     * Add a listener for Property changes. Duplicates will be ignored.
     *
     * @param l The listener.
     */
    public void addCommandHistoryListener(CommandListener l) {
        if (!commandListeners.contains(l)) {
            commandListeners.add(l);
        }
    }

    /**
     * Remove a listener for Property changes.
     *
     * @param l The listener.
     */
    public void removeCommandHistoryListener(CommandListener l) {
        commandListeners.remove(l);
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Is the current scene in the same state as what
     * was last saved?
     *
     * @return current save state
     */
    public boolean isSaveUpToDate() {
        return saveUpToDate;
    }

    /**
     * Set the current save state
     *
     * @param current save state
     */
    public void setSaveUpToDate(boolean upToDate) {
        saveUpToDate = upToDate;
    }

    /**
     * Are there commands being processed
     * @return
     */
    public boolean isProcessingCommand() {   
    	boolean processing = isProcessing || isChained;
System.out.println("isProcessing: " + isProcessing);   	
System.out.println("isChained: " + isChained);   	
System.out.println("is processing commands: " + processing); 
System.out.println("");
    	return processing;
    }

    /**
     * Are there a set of chained commands being processed
     * @param isChained
     */
    public void setProcessingChainedCommands(boolean isChained) {
    	this.isChained = isChained;
    }
    
}
