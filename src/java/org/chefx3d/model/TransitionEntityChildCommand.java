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
import java.util.*;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for relocating a child entity from one parent to another.
 *
 * @author Rex Melton
 * @version $Revision: 1.26 $
 */
public class TransitionEntityChildCommand implements 
	Command, RuleDataAccessor, RuleBypassFlag{

    /** The model */
    private WorldModel model;
	
    /** The start parent entity */
    private Entity startParent;
    
    /** The end parent entity */
    private Entity endParent;
    
    /** The child entity */
    private PositionableEntity child;

    /** The starting position */
    private double[] startPos;

    /** The ending position */
    private double[] endPos;

    /** The position */
    private double[] pos;

    /** The current rotation */
    private float[] rot;

    /** The end rotation */
    private float[] endRot;

    /** The start rotation */
    private float[] startRot;
    
    /** The current scale */
    private float[] scale;
    
    /** The end scale */
    private float[] endScale;
    
    /** The start scale */
    private float[] startScale;

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

    /** A list of children at the start of a move */
    private ArrayList<Entity> startChildren;
    
    /** A list of children at the start of a move */
    private ArrayList<Entity> undoAddedChildren;
    
    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;
    
    /** Unique TransactionID */
    private int transactionID;

    /**
     * Relocate an entity from one parent to another.
     *
     * @param model The WorldModel
     * @param child The entity to transition
     * @param startParent The initial parent entity
     * @param startPosition The initial position in parent relative coordinates
     * @param startRot The initial rotation in parent relative axis angle
     * @param endParent The final parent entity
     * @param endPosition The final position in parent relative coordinates
     * @param endRot The final rotation in parent relative axis angle
	 * @param isTransient Transient state
     */
    public TransitionEntityChildCommand(
		WorldModel model, 
		PositionableEntity child, 
		Entity startParent,
        double[] startPosition, 
		float[] startRot, 
		Entity endParent,
        double[] endPosition,
		float[] endRot,
		boolean isTransient) {

    	this.transactionID = model.issueTransactionID();
    	this.model = model;
        this.child = child;
        this.startParent = startParent;
        this.endParent = endParent;

        this.pos = new double[3];

        this.startPos = new double[3];
        this.startPos[0] = startPosition[0];
        this.startPos[1] = startPosition[1];
        this.startPos[2] = startPosition[2];

        this.endPos = new double[3];
        this.endPos[0] = endPosition[0];
        this.endPos[1] = endPosition[1];
        this.endPos[2] = endPosition[2];

        rot = new float[4];

        this.startRot = new float[4];
        this.startRot[0] = startRot[0];
        this.startRot[1] = startRot[1];
        this.startRot[2] = startRot[2];
        this.startRot[3] = startRot[3];

        this.endRot = new float[4];
        this.endRot[0] = endRot[0];
        this.endRot[1] = endRot[1];
        this.endRot[2] = endRot[2];
        this.endRot[3] = endRot[3];
        
        scale = null;
        startScale = null;
        endScale = null;
        startChildren = new ArrayList<Entity>();
        undoAddedChildren = new ArrayList<Entity>();
        
        description = "TransitionEntityChild -> " + child.getName();

        this.transientState = isTransient;
        undoableState = !isTransient;
        
        ruleBypassFlag = false;
		
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
    
    /**
     * Relocate an entity from one parent to another.
     *
     * @param model The WorldModel
     * @param child The entity to transition
     * @param startParent The initial parent entity
     * @param startPosition The initial position in parent relative coordinates
     * @param startRot The initial rotation in parent relative axis angle
     * @param startScale The initial scale amount
     * @param endParent The final parent entity
     * @param endPosition The final position in parent relative coordinates
     * @param endRot The final rotation in parent relative axis angle
     * @param endScale The final scale amount
	 * @param isTransient Transient state
     */
    public TransitionEntityChildCommand(
		WorldModel model, 
		PositionableEntity child, 
		Entity startParent,
        double[] startPosition, 
		float[] startRot, 
		float[] startScale,
		Entity endParent,
        double[] endPosition,
		float[] endRot,
		float[] endScale,
		boolean isTransient) {

    	this(model, child, startParent, startPosition, startRot, 
    	        endParent, endPosition, endRot, isTransient);
        
        this.startScale = new float[3];
        
        this.startScale[0] = startScale[0];
        this.startScale[1] = startScale[1];
        this.startScale[2] = startScale[2];
        
        this.endScale = new float[3];
        
        this.endScale[0] = endScale[0];
        this.endScale[1] = endScale[1];
        this.endScale[2] = endScale[2];

        description = "TransitionEntityChild w scale -> " + child.getName();

        this.transientState = isTransient;
        undoableState = !isTransient;
		
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
    
    /**
     * Relocate an entity from one parent to another.
     *
     * @param model The WorldModel
     * @param child The entity to transition
     * @param startParent The initial parent entity
     * @param startPosition The initial position in parent relative coordinates
     * @param startRot The initial rotation in parent relative axis angle
     * @param endParent The final parent entity
     * @param endPosition The final position in parent relative coordinates
     * @param endRot The final rotation in parent relative axis angle
     * @param isTransient Transient state
     * @param startChildren Old entity child list
     */
    public TransitionEntityChildCommand(
        WorldModel model, 
        PositionableEntity child, 
        Entity startParent,
        double[] startPosition, 
        float[] startRot, 
        Entity endParent,
        double[] endPosition,
        float[] endRot,
        boolean isTransient, 
        ArrayList<Entity> startChildren) {
        
        this(model, child, startParent, startPosition, startRot, 
                endParent, endPosition, endRot, isTransient);
        this.startChildren = startChildren;
        
    }
    

    /**
     * Execute the command.
     */
    public void execute() {
		
		// despite what the command arguments say,
		// figure out the actual parent entity
		Entity realParent = model.getEntity(child.getParentEntityID());
		
		if (realParent != null) {
		    
		    realParent.removeChild(child);
		    
	        // make the move
	        this.pos[0] = endPos[0];
	        this.pos[1] = endPos[1];
	        this.pos[2] = endPos[2];

	        child.setPosition(pos, transientState);
	        child.setStartingPosition(pos);
	        
	        // do the turn
	        rot[0] = endRot[0];
	        rot[1] = endRot[1];
	        rot[2] = endRot[2];
	        rot[3] = endRot[3];

	        child.setRotation(rot, transientState);
	        child.setStartingRotation(rot);
	        
	        // scale
	        if(endScale != null){
	            
	            scale = new float[3];           
	            scale[0] = endScale[0];
	            scale[1] = endScale[1];
	            scale[2] = endScale[2];
	            
	            child.setScale(scale);
	        }
	        
	        // reparent
	        endParent.addChild(child);

		}
		
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
		
        endParent.removeChild(child);
		
        // scale
        if(startScale != null){
        	
        	scale[0] = startScale[0];
        	scale[1] = startScale[1];
        	scale[2] = startScale[2];
        	
        	child.setScale(scale);
        }
        
		// return to the starting rotation
        rot[0] = startRot[0];
        rot[1] = startRot[1];
        rot[2] = startRot[2];
        rot[3] = startRot[3];

        child.setRotation(rot, transientState);
        child.setStartingRotation(rot);
		
		// return the entity to the starting location
        this.pos[0] = startPos[0];
        this.pos[1] = startPos[1];
        this.pos[2] = startPos[2];

        child.setPosition(pos, transientState);
		child.setStartingPosition(pos);
		
		// reparent
        startParent.addChild(child);
        
        // make sure the children exist
        ArrayList<Entity> children = child.getChildren();
        int len = startChildren.size();
        for (int i = 0; i < len; i++) {
            Entity startChild = startChildren.get(i);
            if (!children.contains(startChild)) {
                child.addChild(startChild);
                undoAddedChildren.add(startChild);
            }
        }

    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        
        // clear out any children added as part of the undo
        child.getChildren().removeAll(undoAddedChildren);
                
        // execute the command
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
     * Gets the start position. If the array passed in is not length 3 then
     * the array is returned null.
     *  
     * @param pos xyz indexed array to be set
     */
    public void getStartPosition(double[] pos){
    	
    	pos[0] = startPos[0];
    	pos[1] = startPos[1];
    	pos[2] = startPos[2];
    }
    
    /**
     * Sets the start position for the command.
     * 
     * @param pos xyz indexed array with values to set
     */
    public void setStartPosition(double[] pos){
    	
    	startPos[0] = pos[0];
    	startPos[1] = pos[1];
    	startPos[2] = pos[2];
    }
    
    /**
     * Gets the end position. If the array passed in is not length 3 then
     * the array is returned null.
     *  
     * @param pos xyz indexed array to be set
     */
    public void getEndPosition(double[] pos){
    	
    	pos[0] = endPos[0];
    	pos[1] = endPos[1];
    	pos[2] = endPos[2];
    }
    
    /**
     * Sets the end position for the command.
     * 
     * @param pos xyz indexed array with values to set
     */
    public void setEndPosition(double[] pos){
    	
    	endPos[0] = pos[0];
    	endPos[1] = pos[1];
    	endPos[2] = pos[2];
    }

    /**
     * Gets the position. If the array passed in is not length 3 then
     * the array is returned null.
     *  
     * @param pos xyz indexed array to be set
     */
    public void getPosition(double[] pos){
    	
    	pos[0] = this.pos[0];
    	pos[1] = this.pos[1];
    	pos[2] = this.pos[2];
    }
    
    /**
     * Sets the position for the command.
     * 
     * @param pos xyz indexed array with values to set
     */
    public void setPosition(double[] pos){
    	
    	this.pos[0] = pos[0];
    	this.pos[1] = pos[1];
    	this.pos[2] = pos[2];
    }   
    
    /**
     * Gets the current rotation. If the array passed in is not length 4 then
     * the array is returned null.
     * 
     * @param rotation Given the values of the current rotation
     */
    public void getCurrentRotation(float[] rotation){
    	
    	if(rotation.length != 4){
    		rotation = null;
    		return;
    	}
    	
    	rotation[0] = endRot[0];
    	rotation[1] = endRot[1];
    	rotation[2] = endRot[2];
    	rotation[3] = endRot[3];
    }
    
    /**
     * Sets the current rotation.
     * @param x
     * @param y
     * @param z
     * @param rad
     */
    public void setCurrentRotation(float x, float y, float z, float rad){
    	
    	endRot[0] = x;
    	endRot[1] = y;
    	endRot[2] = z;
    	endRot[3] = rad;
    }
    
    /**
     * Gets the starting rotation
     * @param rotation
     */
    public void getStartingRotation(float[] rotation){
    	
    	rotation[0] = startRot[0];
    	rotation[1] = startRot[1];
    	rotation[2] = startRot[2];
    	rotation[3] = startRot[3];
    }
    
    /**
     * Sets the starting rotation
     * @param rotation
     */
    public void setStartingRotation(float[] rotation){
    	
    	startRot[0] = rotation[0];
    	startRot[1] = rotation[1];
    	startRot[2] = rotation[2];
    	startRot[3] = rotation[3];
    }
    
    /**
     * Gets the starting scale
     * @param scale
     */
    public void getStartScale(float[] scale){
    
    	if(startScale == null)
    		return;
    	
    	scale[0] = startScale[0];
    	scale[1] = startScale[1];
    	scale[2] = startScale[2];
    }
    
    /**
     * Set the start scale
     * @param scale
     */
    public void setStartScale(float[] scale){
    
    	startScale = new float[3];
    	
    	startScale[0] = scale[0];
    	startScale[1] = scale[1];
    	startScale[2] = scale[2];
    }
    
    /**
     * Gets the ending scale
     * @param scale
     */
    public void getEndScale(float[] scale){
    	
    	if(endScale == null)
    		return;
    	
    	scale[0] = endScale[0];
    	scale[1] = endScale[1];
    	scale[2] = endScale[2];
    }
    
    /**
     * Set the end scale
     * @param scale
     */
    public void setEndScale(float[] scale){
    	
    	endScale = new float[3];
    	
    	endScale[0] = scale[0];
    	endScale[1] = scale[1];
    	endScale[2] = scale[2];
    }
    
    /**
     * Gets the current scale
     * @param scale
     */
    public void getCurrentScale(float[] scale){
    	
    	if(scale == null)
    		return;
    	
    	scale[0] = this.scale[0];
    	scale[1] = this.scale[1];
    	scale[2] = this.scale[2];
    }
    
    /**
     * Set the scale
     * @param scale
     */
    public void setCurrentScale(float[] scale){
    	
    	this.scale = new float[3];
    	
    	this.scale[0] = scale[0];
    	this.scale[1] = scale[1];
    	this.scale[2] = scale[2];
    }
    
    /**
     * Retrieve the child entity
     * 
     * @return Entity
     */
	public Entity getEntity() {
		
		return child;
	}
	
	/**
	 * Retrieve the parent entity the child is initially parented to.
	 * 
	 * @return Entity
	 */
	public Entity getStartParentEntity(){
	
		return startParent;
	}
	
	/**
     * Set the start parent entity we began parented to
     * 
     * @param startParent
     */
    public void setStartParentEntity(Entity startParent){
        
        this.startParent = startParent;
    }


	/**
	 * Retrieve the parent entity the child is finally parented to.
	 * 
	 * @return Entity
	 */
	public Entity getEndParentEntity(){
	
		return endParent;
	}
	
	/**
	 * Set the start parent entity to end up parented to
	 * 
	 * @param endParent
	 */
	public void setEndParentEntity(Entity endParent){
		
		this.endParent = endParent;
	}

	/**
	 * Retrieve the WorldModel associated with command.
	 * 
	 * @return WorldModel
	 */
	public WorldModel getWorldModel() {

		return model;
	}
	
    /**
     * Set the starting children. This overwrites the existing children.
     * 
     * @param startChildren Starting children to store.
     */
    public void setStartChildren(ArrayList<Entity> startChildren) {
    	
    	this.startChildren.clear();
    	this.startChildren.addAll(startChildren);
    }
    
    /**
     * Get the starting children.
     * 
     * @return Array of starting children entities.
     */
    public Entity[] getStartChildren() {
    	
    	Entity[] children = new Entity[startChildren.size()];
    	startChildren.toArray(children);
    	return children;
    }
	
	/**
	 * Resets all of the values to the starting values in order to reset
	 * the entity back to where it came from.
	 */
	public void resetToStart(){
		
		endParent = startParent;
		
        endPos[0] = startPos[0];
        endPos[1] = startPos[1];
        endPos[2] = startPos[2];
                
        if (startScale != null) {
            if (endScale == null) 
                endScale = new float[3];

            endScale[0] = startScale[0];
            endScale[1] = startScale[1];
            endScale[2] = startScale[2];
        }
        
		endRot[0] = startRot[0];
		endRot[1] = startRot[1];
		endRot[2] = startRot[2];
		endRot[3] = startRot[3];
		
        // make sure the children exist
        ArrayList<Entity> children = child.getChildren();
        int len = startChildren.size();
        for (int i = 0; i < len; i++) {
            Entity startChild = startChildren.get(i);
            if (!children.contains(startChild)) {
                child.addChild(startChild);
            }
        }
        
        // Make sure to reset the scale of all the children too
        float[] scale = new float[3];
        double[] pos = new double[3];
        
        for (Entity e : children) {
        	
        	if (e instanceof PositionableEntity) {
        		PositionableEntity pE = (PositionableEntity) e;
        		
        		pE.getStartingScale(scale);
        		pE.getPosition(pos);
        		
        		ScaleEntityTransientCommand scaleCmd = 
                    new ScaleEntityTransientCommand(
                            model, 
                            model.issueTransactionID(), 
                            pE, 
                            pos, 
                            scale);
        		
        		model.forceCommandExecution(scaleCmd);
        	}
        }

	}

	public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
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
		if (externalCommand instanceof TransitionEntityChildCommand) {
			
			double[] startPosition = new double[3];
			double[] endPosition = new double[3];
			float[] startRotation = new float[4];
			float[] endRotation = new float[4];
			float[] startScale = new float[3];
			float[] endScale = new float[3];
			
			((TransitionEntityChildCommand)externalCommand).getStartPosition(startPosition);
			((TransitionEntityChildCommand)externalCommand).getEndPosition(endPosition);
			((TransitionEntityChildCommand)externalCommand).getStartingRotation(startRotation);
			((TransitionEntityChildCommand)externalCommand).getCurrentRotation(endRotation);
			((TransitionEntityChildCommand)externalCommand).getStartScale(startScale);
			((TransitionEntityChildCommand)externalCommand).getEndScale(endScale);
			
			if (externalCommand.isTransient() != this.isTransient()) {
				
				return false;
				
			} else if (((TransitionEntityChildCommand)externalCommand).
					getEntity() != child) {
				
				return false;
			
			} else if (((TransitionEntityChildCommand)externalCommand).getEndParentEntity() != this.endParent) {
				
				return false;
				
			} else if (((TransitionEntityChildCommand)externalCommand).getStartParentEntity() != this.startParent) {
				
				return false;
				
			} else if (!Arrays.equals(startPosition, this.startPos)) {
				
				return false;
				
			} else if (!Arrays.equals(endPosition, this.endPos)) {
				
				return false;
				
			} else if (!Arrays.equals(startRotation, this.startRot)) {
				
				return false;
				
			} else if (!Arrays.equals(endRotation, this.endRot)) {
				
				return false;
				
			} else if (!Arrays.equals(startScale, this.startScale)) {
				
				return false;
				
			} else if (!Arrays.equals(endScale, this.endScale)) {
				
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
}
