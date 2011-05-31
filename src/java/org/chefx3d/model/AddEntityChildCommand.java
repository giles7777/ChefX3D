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
 * @version $Revision: 1.25 $
 */
public class AddEntityChildCommand 
    implements Command, RuleDataAccessor, RuleBypassFlag {

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
    
    /** This add is a part of a group of adds, this is the grouping ID */
    private int groupingID;   
    
    /** The transactionID */
    private int transactionID;

    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;

    /**
     * Add a child entity to a parent entity.
     * 
     * @param model The world model to reference
     * @param parent The parent entity
     * @param child The child entity
     * @param isUndable The undoable flag for the command
     */
    public AddEntityChildCommand(
            WorldModel model, 
            int transactionID,
            Entity parent, 
            Entity child, 
            boolean isUndoable) {

        this.model = model;
        this.parent = parent;
        this.child = child;

        description = "AddEntityChild -> " + child.getName();

        init();
        
        this.undoableState = isUndoable;
        this.transactionID = transactionID;
        
    }

    /**
     * Common initialization.
     */
    private void init() {
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();
        transientState = false;
        groupingID = -1;
    }
    
    //--------------------------------------------------------------------
    // Routines required by RuleBypassFlag
    //--------------------------------------------------------------------

    /**
     * Set the rule bypass flag value.
     *
     * @param ruleBypassFlag True to bypass rules, false otherwise
     */
    public void setBypassRules(boolean bypass){
        this.ruleBypassFlag = bypass;
    }

    /**
     * Get the rule bypass flag value.
     *
     * @return boolean True to bypass, false otherwise
     */
    public boolean bypassRules() {
        return ruleBypassFlag;
    }


    /**
     * Execute the command.
     */
    public void execute() {
		
        parent.addChild(child);
		
        if(child instanceof PositionableEntity){
        	
        	double[] startingPosition = new double[3];
        	((PositionableEntity)child).getPosition(startingPosition);
        	((PositionableEntity)child).setStartingPosition(startingPosition);
        
        	float[] startingRotation = new float[4];
        	((PositionableEntity)child).getRotation(startingRotation);
        	((PositionableEntity)child).setStartingRotation(startingRotation);
        	
        	float[] startingScale = new float[3];
        	((PositionableEntity)child).getScale(startingScale);
        	((PositionableEntity)child).setStartingScale(startingScale);
        }

    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        parent.removeChild(child);
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
        return transactionID;
    }
    
    /**
     * Set the transactionID for this command.
     *
     * @param The transactionID
     */
    public void setTransactionID(int transactionID) {
        this.transactionID = transactionID;
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

	public Entity getEntity() {
		
		return child;
	}

	public Entity getParentEntity() {
		
		return parent;
	}

	public WorldModel getWorldModel() {
		
		return model;
	}
	
	public void setParentEntity(Entity parent){
		
		this.parent = parent;
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
		
		if (externalCommand instanceof AddEntityChildCommand) {
			
			if (((AddEntityChildCommand)externalCommand).getEntity() != 
				this.child) {
				
				return false;
				
			} else if (((AddEntityChildCommand)externalCommand).
					getParentEntity() != this.parent) {
				
				return false;
				
			} else if (externalCommand.isTransient() != this.transientState) {
				
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

    /**
     * @return the groupingID
     */
    public int getGroupingID() {
        return groupingID;
    }

    /**
     * @param groupingID the groupingID to set
     */
    public void setGroupingID(int groupingID) {
        this.groupingID = groupingID;
    }
}
