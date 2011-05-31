/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
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

//Internal Imports
import org.chefx3d.util.ErrorReporter;

/**
 * A command to change the model.
 *
 * @author Alan Hudson
 * @version $Revision: 1.15 $
 */
public interface Command {
    public static final int METHOD_XML = 0;

    public static final int METHOD_XML_FAST_INFOSET = 1;
       
    /**
     * Execute the <code>Command</code>.
     */
    public void execute();

    /**
     * Undo the affects of this <code>Command</code>.
     */
    public void undo();

    /**
     * Redo the affects of this <code>Command</code>.
     */
    public void redo();

    /**
     * Get the text description of this <code>Command</code>.
     */
    public String getDescription();

    /**
     * Set the text description of this <code>Command</code>.
     */
    public void setDescription(String desc);

    /**
     * Return transient status.
     */
    public boolean isTransient();

    /**
     * Is the command locally generated.
     *
     * @return Is local
     */
    public boolean isLocal();

    /**
     * Get the transactionID for this command.
     *
     * @return The transactionID
     */
    public int getTransactionID();

    /**
     * Set the local flag.
     *
     * @param isLocal Is this a local update
     */
    public void setLocal(boolean isLocal);

    /**
     * Return undoable status.
     */
    public boolean isUndoable();

    /**
     * Sets the ErrorReporter to use to display messages
     *
     * @param reporter
     */
    public void setErrorReporter(ErrorReporter reporter);
    
    /** 
     * Gets a list of rules to ignore
     * @param ignoreRuleList TODO
     */
    public void setIgnoreRuleList(HashSet<String> ignoreRuleList); 
    
    /** 
     * Gets a list of rules to return
     */
    public HashSet<String> getIgnoreRuleList(); 
}
