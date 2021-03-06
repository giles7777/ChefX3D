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
import java.io.PrintStream;
import java.util.*;
import org.w3c.dom.*;

//Internal Imports
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for moving an entity.
 *
 * @author Alan Hudson
 * @version $Revision: 1.39 $
 */
public class MoveEntityCommand 
    implements Command, RuleDataAccessor, RuleBypassFlag {
    
    /** The model */
    private BaseWorldModel model;

    /** The new entityID */
    private int entityID;

    /** The entity to update */
    private PositionableEntity entity;

    /** The starting position */
    private double[] startPos;

    /** The ending position */
    private double[] endPos;

    /** The position */
    private double[] pos;

    /** Is this a local add */
    private boolean local;

    /** The transactionID */
    private int transactionID;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** Should the command die */
    private boolean shouldDie = false;

    /** A list of children at the start of a move */
    private ArrayList<Entity> startChildren;

    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;

    /**
     * Add an entity.
     *
     * @param model The model to change
     * @param transID The transactionID
     * @param entityID The unique entityID assigned by the view
     * @param endPosition The end position in world coordinates(meters, Y-UP,
     *        X3D System).
     * @param startPosition The start position in world coordinates(meters,
     *        Y-UP, X3D System).
     */
    public MoveEntityCommand(
            WorldModel model,
            int transID,
            PositionableEntity entity,
            double[] endPosition,
            double[] startPosition) {

        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        this.entity = entity;
        this.transactionID = transID;

        this.pos = new double[3];

        this.startPos = new double[3];
        this.startPos[0] = startPosition[0];
        this.startPos[1] = startPosition[1];
        this.startPos[2] = startPosition[2];

        this.endPos = new double[3];
        this.endPos[0] = endPosition[0];
        this.endPos[1] = endPosition[1];
        this.endPos[2] = endPosition[2];

        local = true;

        startChildren = new ArrayList<Entity>();
        ruleBypassFlag = false;
        
        init();
    }

    /**
     * Add an entity.
     *
     * @param model The model to change
     * @param transID The transactionID
     * @param entityID The unique entityID assigned by the view
     * @param endPosition The end position in world coordinates(meters, Y-UP,
     *        X3D System).
     * @param startPosition The start position in world coordinates(meters,
     *        Y-UP, X3D System).
     * @param startChildren Old entity child list
     */
    public MoveEntityCommand(
            WorldModel model,
            int transID,
            PositionableEntity entity,
            double[] endPosition,
            double[] startPosition, 
            ArrayList<Entity> startChildren) {
        
        this(model, transID, entity, endPosition, startPosition);        
        this.startChildren = startChildren;

    }
    
    public MoveEntityCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        ruleBypassFlag = false;
        init();
    }

    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "MoveEntity -> " + entityID;

        undoableState = true;
        transientState = false;
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

/*
System.out.println("MoveEntityCommand.execute()");
System.out.println("    entityID: " + entityID);
System.out.println("    startPos[0]: " + startPos[0]);
System.out.println("    startPos[1]: " + startPos[1]);
System.out.println("    startPos[2]: " + startPos[2]);
System.out.println("    endPos[0]: " + endPos[0]);
System.out.println("    endPos[1]: " + endPos[1]);
System.out.println("    endPos[2]: " + endPos[2]);
*/
        
        // make the final move of the entity
        this.pos[0] = endPos[0];
        this.pos[1] = endPos[1];
        this.pos[2] = endPos[2];

        entity.setPosition(pos, transientState);
        entity.setStartingPosition(pos);
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        // return the entity to the starting location
        this.pos[0] = startPos[0];
        this.pos[1] = startPos[1];
        this.pos[2] = startPos[2];

        entity.setPosition(pos, transientState);
		entity.setStartingPosition(pos);
		
	    // make sure the children exist
        ArrayList<Entity> children = entity.getChildren();
        int len = startChildren.size();
        for (int i = 0; i < len; i++) {
            Entity child = startChildren.get(i);
            if (!children.contains(child)) {
                entity.addChild(child);
            }
        }

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
        switch (method) {
        case METHOD_XML:
            /*
             * <MoveEntityCommand entityID='1' px='' py='' pz='' />
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<MoveEntityCommand entityID='");
            sbuff.append(entityID);
            sbuff.append("' tID='");
            sbuff.append(transactionID);
            sbuff.append("' px='");
            sbuff.append(pos[0]);
            sbuff.append("' py='");
            sbuff.append(pos[1]);
            sbuff.append("' pz='");
            sbuff.append(pos[2]);
            sbuff.append("' />");

            String st = sbuff.toString();

            PrintStream ps = new PrintStream(os);
            ps.print(st);
            break;
        case METHOD_XML_FAST_INFOSET:
            errorReporter.messageReport("Unsupported serialization method");
            break;
        }
    }

    /**
     * Deserialize a stream
     *
     * @param st The xml string to deserialize
     */
    public void deserialize(String st) {
        Document doc = DOMUtils.parseXML(st);

        Element e = (Element) doc.getFirstChild();

        String d;

        pos = new double[3];
        endPos = new double[3];
        startPos = new double[3];

        d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        endPos[0] = pos[0];
        endPos[1] = pos[1];
        endPos[2] = pos[2];

        entityID = Integer.parseInt(e.getAttribute("entityID"));
        transactionID = Integer.parseInt(e.getAttribute("tID"));
        entity = (PositionableEntity)model.getEntity(entityID);

        local = false;
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
	 * Get the Entity
	 * 
	 * @return Entity or null if it doesn't exist
	 */
	public Entity getEntity(){
		return entity;
	}
	
	/**
	 * Get the WorldModel
	 * 
	 * @return WorldModel or null if it doesn't exist
	 */
	public WorldModel getWorldModel(){
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
        endPos[0] = startPos[0];
        endPos[1] = startPos[1];
        endPos[2] = startPos[2];
        
        if (entity == null)
            return;
        
        // make sure the children exist
        ArrayList<Entity> children = entity.getChildren();
        int len = startChildren.size();
        for (int i = 0; i < len; i++) {
            Entity startChild = startChildren.get(i);
            if (!children.contains(startChild)) {
                entity.addChild(startChild);
            }
        }
        
        // Make sure to reset the scale of all the children too
        float[] scale = new float[3];
        double[] pos = new double[3];
        
        for (Entity e : children) {
        	
        	if (e instanceof PositionableEntity) {
        		PositionableEntity pE = (PositionableEntity) e;
        		
        		// check to make sure it can scale
        		Boolean canScale = (Boolean)pE.getProperty(
        				Entity.DEFAULT_ENTITY_PROPERTIES, 
        				ChefX3DRuleProperties.CAN_SCALE_PROP);
        		
        		if (canScale != null && canScale) {
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
		
		if (externalCommand instanceof MoveEntityCommand) {
		
			if (((MoveEntityCommand)externalCommand).getTransactionID() == 
				this.transactionID) {
				
				return true;
			
			}
			
			double[] startPosition = new double[3];
			double[] endPosition = new double[3];
			
			((MoveEntityCommand)externalCommand).getEndPosition(endPosition);
			((MoveEntityCommand)externalCommand).getStartPosition(startPosition);
			
			if (((MoveEntityCommand)externalCommand).getEntity() != this.entity) {
				
				return false;
				
			} else if (!Arrays.equals(startPosition, this.startPos)) {
				
				return false;
				
			} else if (!Arrays.equals(endPosition, this.endPos)) {
				
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