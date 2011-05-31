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
import java.io.OutputStream;
import java.util.HashSet;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for clearing the command queue history in the 
 * midst of command processing.
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public class ClearHistoryCommand implements Command {

    /** The controller to clear */
    private CommandController controller;
    
    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;

    /**
     * Clear the controller's command history.
     *
     * @param controller The CommandController
     */
    public ClearHistoryCommand(CommandController controller) {

        this.controller = controller;

        description = "Clear History";

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        undoableState = false;
        transientState = false;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        
        controller.clear();
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
       // ignore
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        // ignore
    }

    /**
     * Get the text description of this <code>Command</code>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the text description of this <code>Command</code>.
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Get the state of this <code>Command</code>.
     */
    public boolean isTransient() {
        return transientState;
    }

    /**
     * Get the transactionID for this command.
     *
     * @return The transactionID
     */
    public int getTransactionID() {
        return 0;
    }

    /**
     * Is the command locally generated.
     * 
     * @return Is local
     */
    public boolean isLocal() {
        return false;
    }

    /**
     * Set the local flag.
     * 
     * @param isLocal Is this a local update
     */
    public void setLocal(boolean isLocal) {
        // ignore
    }
    
    /**
     * Get the undo setting of this <code>Command</code>. true =
     * <code>Command</code> may be undone false = <code>Command</code> may
     * never undone
     */
    public boolean isUndoable() {
        return undoableState;
    }

    /**
     * Serialize this command.
     *
     * @param method What method should we use
     * @param os The stream to output to
     */
    public void serialize(int method, OutputStream os) {
        errorReporter.messageReport("Networking Unsupported");
    }

    /**
     * Deserialize a stream
     *
     * @param st The xml string to deserialize
     */
    public void deserialize(String st) {
        errorReporter.messageReport("Networking Unsupported");
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

    public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }
}
