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

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for adding a child to an entity.
 *
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
public class RemoveEntityChildTransientCommand implements Command, RuleDataAccessor{

    /** The model */
    private WorldModel model;
	
    /** The parent entity */
    private Entity parent;
    
    /** The child entity */
    private Entity child;

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
     * Add a property to an entity.
     *
     * @param model The WorldModel
     * @param parent The entity to change
     * @param child The entity to add
     */
    public RemoveEntityChildTransientCommand(
            WorldModel model, 
            Entity parent, 
            Entity child) {
        
        this.model = model;
        this.parent = parent;
        this.child = child;
        
        description = "RemoveEntityChild -> " + child.getName();

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
     * Execute the command.
     */
    public void execute() {
        if (parent != null)
            parent.removeChild(child);
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        // ignored
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        // ignored
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

    /**
     * Retrieve the child entity being removed.
     * 
     * @return Entity
     */
	public Entity getEntity() {
		
		return child;
	}
	
	/**
	 * Retrieve the parent entity the child is being removed from.
	 * 
	 * @return Entity
	 */
	public Entity getParentEntity(){
	
		return parent;
	}

	/**
	 * Retrieve the WorldModel associated with command.
	 * 
	 * @return WorldModel
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
		
        // Check for appropriate command type(s)
        if (externalCommand instanceof RemoveEntityChildTransientCommand) {
            
            if (((RuleDataAccessor)externalCommand).getEntity() != child) {
                
                return false;
            }
            
        } else {
            // Not a command we can every be equal to
            return false;
        }
        
        return true;
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
