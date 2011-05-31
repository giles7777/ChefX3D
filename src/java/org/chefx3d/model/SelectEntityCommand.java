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

// Internal Imports
import java.util.HashSet;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for selecting entities.
 *
 * @author Russell Dodds
 * @version $Revision: 1.15 $
 */
public class SelectEntityCommand implements Command, RuleDataAccessor {
    
	/** The model */
    private BaseWorldModel model;
	
    /** The entity */
    private Entity entity;

    /** The flag to indicate selection status */
    private boolean selected;
    
    /** Is this a local add */
    private boolean local;

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
    
    /** Should the command die */
    private boolean shouldDie = false;

    /**
     * Select an entity.
     *
     * @param entityParam The entity to select/unselect
     * @param modelParam reference to the WorldModel
     * @param selectedParam The select or unselect flag
     * @param entityParam The append flag, if false the list is reset
     */
    public SelectEntityCommand(WorldModel worldModel, 
    						   Entity entityParam, 
    						   boolean selectedParam) {

    	model = (BaseWorldModel)worldModel;
        entity = entityParam;
        selected = selectedParam;
                
        description = "Select Entity";

        local = true;

        init();
    }

    /**
     * Common initialization.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        undoableState = false;
        transientState = true;
        
    }

    /**
     * Set the local flag.
     *
     * @param isLocal Is this a local update
     */
    public void setLocal(boolean isLocal) {
        local = isLocal;
    }

    /**
     * Is the command locally generated.
     *
     * @return Is local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * Execute the command.
     */
    public void execute() {         
        // update the selection
        entity.setSelected(selected);
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        // update the selection
        entity.setSelected(!selected);
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        execute();
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
     * Get the undo setting of this <code>Command</code>. true =
     * <code>Command</code> may be undone false = <code>Command</code> may
     * never undone
     */
    public boolean isUndoable() {
        return undoableState;
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

    public boolean isSelected() {
        // TODO Auto-generated method stub
        return selected;
    }
    
    public void setSelected(boolean selected) {
        // TODO Auto-generated method stub
        this.selected = selected;
    }

    public Entity getEntity() {
        // TODO Auto-generated method stub
        return entity;
    }

    /**
     * Return a reference to the world model.
     * Used by emerson.closetmaid.rules.interpreters.
     * ClosetmaidValidatingCommandInterpreter.java
     * so that it can call model.applyCommand(cmd);
     * @author Eric Fickenscher - changed constructor to 
     * take a worldModel reference so that this method would
     * no longer return null.
     * 
     */
    public WorldModel getWorldModel() {

        return model;
    }

    public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

    public void resetToStart() {
        // TODO Auto-generated method stub
        
    }

    /**
     * Set the die state of the command. Setting this to true will
     * only cause the command to die if the rule engine execution
     * returns false.
     * 
     * @param die True to have command die and not execute
     */
	public void setCommandShouldDie(boolean die) {
		
		shouldDie = die;
	}

	/**
	 * Get the die value of the command.
	 * 
	 * @return True to have command die, false otherwise
	 */
	public boolean shouldCommandDie() {

		return shouldDie;
	}
	
	/**
	 * Compare external command to this one to see if they are the same.
	 * 
	 * @param externalCommand command to compare against
	 * @return True if the same, false otherwise
	 */
	public boolean isEqualTo(Command externalCommand) {
		
		if (externalCommand instanceof SelectEntityCommand) {
		
			if (((SelectEntityCommand)externalCommand).getEntity() != 
				this.entity) {
			
				return false;
			}
			
			return true;
		}
		
		return false;
		
	}
	
	/**
	 * Override object's equals method
	 */
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof Command) {
			return isEqualTo((Command)obj);
		}
		
		return false;
	}
}
