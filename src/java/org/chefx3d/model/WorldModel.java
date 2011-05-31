/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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
import java.util.List;

// Internal Imports
import org.chefx3d.util.ErrorReporter;

/**
 * Stores all information that can authored.  Outward facing interface.
 *
 * @author Alan Hudson
 * @version $Revision: 1.32 $
 */
public interface WorldModel extends IdIssuer {
	
    // ---------------------------------------------------------------
    // Methods defined by IdIssuer
    // ---------------------------------------------------------------
  
	/**
     * Get a unique ID for an entity.
     *
     * @return The unique ID
     */
    public int issueEntityID();

    /**
     * Get a unique ID for a transaction. A transaction is a set of transient
     * commands and the final real command. A transactionID only needs to be
     * unique for a short period of time. 0 is reserved as marking a
     * transactionless command.
     *
     * @return The ID
     */
    public int issueTransactionID();
	
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
	
    /**
     * Add a listener for Model changes. Duplicates will be ignored.
     *
     * @param l The listener.
     */
    public void addModelListener(ModelListener l);

    /**
     * Remove a listener for Model changes.
     *
     * @param l The listener.
     */
    public void removeModelListener(ModelListener l);

    /**
     * Add a listener for Property changes. Duplicates will be ignored.
     *
     * @param l The listener.
     */
    public void addPropertyStructureListener(PropertyStructureListener l);

    /**
     * Remove a listener for Property changes.
     *
     * @param l The listener.
     */
    public void removePropertyStructureListener(PropertyStructureListener l);

    /**
     * Apply a command against the model.
     *
     * @param command The command
     */
    public void applyCommand(Command command);
    
    /**
     * Force execution of the command without putting it on the pending stack.
     * 
     * @param command The command
     */
    public void forceCommandExecution(Command command);

    /**
     * Undo the last change.
     */
    public void undo();

    /**
     * Returns true if there are any <code>Command</code>s to undo
     */
    public boolean canUndo();

    /**
     * Return the description of the <code>Command</code> to be executed if
     * <code>undo()</code> is called.
     */
    public String getUndoDescription();

    /**
     * Redo the last change.
     */
    public void redo();

    /**
     * Returns true if there are any <code>Command</code>s to redo
     */
    public boolean canRedo();

    /**
     * Return the description of the <code>Command</code> to be executed if
     * <code>redo()</code> is called.
     */
    public String getRedoDescription();

    /**
     * Clear the model.
     *
     * @param local Is this a local change
     * @param listener The model listener to notify or null for all
     */
    public void clear(boolean local, ModelListener listener);

    /**
     * Flush the undo history.
     */
    public void clearHistory();

    /**
     * Reissue all events to catch a model listener up to the current state.
     *
     * @param l The model listener
     */
    public void reissueEvents(ModelListener l);

    /**
     * Get an entity.
     *
     * @param entityID The ID of the entity
     * @return The entity
     */
    public Entity getEntity(int entityID);
    
    /**
     * Get the entity that represents the scene
     *
     * @return The entity or null if not found
     */
    public Entity getSceneEntity();

    /**
     * Get the model data.
     * 
     * @return Returns the current model data
     * 			Entity array will commonly have holes it,
     *  		make sure to check for null when searching the array 
     */
    public Entity[] getModelData();

    /**
     * Sets the ErrorReporter to use to display messages
     *
     * @param reporter
     */
    public void setErrorReporter(ErrorReporter reporter);

}
