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

// External Imports

// Internal Imports
import org.chefx3d.util.ErrorReporter;

/**
 * A history of commands changed within the model.
 * 
 * @author Russell Dodds
 * @version $Revision: 1.9 $
 */
public interface CommandController {

    /**
     * Add a <code>Command</code> to the history list without performing it.
     * 
     * @param command the action to remember
     */
    public void execute(Command command);
    
    /**
     * Request a method to be validated against the current rules set
     * 
     * @param cmd The command to validate
     * @return True if valid, false otherwise
     */
    public boolean validate(Command cmd);
    
    /**
     * Force execution of a <code>Command</code> instead of buffering it.
     * 
     * @param command The action to remember
     */
    public void forceExecution(Command command);

    /**
     * Execute the <code>Command</code> at the top of the stacks
     */
    public void undo();

    /**
     * Returns true if there are any <code>Command</code>s to undo
     */
    public boolean canUndo();

    /**
     * Execute the <code>Command</code> at the top of the redo stack
     */
    public void redo();

    /**
     * Returns true if there are any <code>Command</code>s to redo
     */
    public boolean canRedo();

    /**
     * Remove all <code>Command</code>s from the history
     */
    public void clear();

    /**
     * Set the maximum size of the command history
     * 
     * @param size The new size to assign
     */
    public void setSize(int size);

    /**
     * Return the maximum size assigned to the history
     */
    public int getSize();

    /**
     * Return the description of the <code>Command</code> to be executed if
     * <code>undo()</code> is called.
     */
    public String getUndoDescription();

    /**
     * Return the description of the <code>Command</code> to be executed if
     * <code>redo()</code> is called.
     */
    public String getRedoDescription();
    
    /**
     * Add a listener for Property changes. Duplicates will be ignored.
     *
     * @param l The listener.
     */
    public void addCommandHistoryListener(CommandListener l);

    /**
     * Remove a listener for Property changes.
     *
     * @param l The listener.
     */
    public void removeCommandHistoryListener(CommandListener l);
    
    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     * 
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter);

    /**
     * Are there commands being processed
     * @return
     */
    public boolean isProcessingCommand();
    
    /**
     * Are there a set of chained commands being processed
     * @param isChained
     */
    public void setProcessingChainedCommands(boolean isChained);

}
