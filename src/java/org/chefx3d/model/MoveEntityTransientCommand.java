/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006-2007
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
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for moving an entity.
 *
 * @author Alan Hudson
 * @version $Revision: 1.23 $
 */
public class MoveEntityTransientCommand implements 
	Command, DeadReckonedCommand, RuleDataAccessor, RuleBypassFlag {
	
    /** The model */
    private BaseWorldModel model;

    /** The new entityID */
    private int entityID;

    /** The entity to update */
    private PositionableEntity entity;

    /** The position */
    private double[] pos;

    /** The velocity */
    private float[] linearVelocity;

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
    
    /** Pick entity that would be the parent if placed at this position */
    private Entity pickEntity = null;
    
    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;

    /**
     * Add an entity.
     *
     * @param model The model to change
     * @param transactionID The transactionID
     * @param entityID The unique entityID assigned by the view
     * @param position The position in world coordinates(meters, Y-UP, X3D
     *        System).
     * @param velocity The velocity vector
     */
    public MoveEntityTransientCommand(
    		WorldModel model, 
    		int transactionID,
            int entityID, 
            double[] position, 
            float[] velocity) {

        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        this.entityID = entityID;
        this.transactionID = transactionID;
        this.pos = new double[3];
        this.pos[0] = position[0];
        this.pos[1] = position[1];
        this.pos[2] = position[2];
        linearVelocity = new float[3];
        linearVelocity[0] = velocity[0];
        linearVelocity[1] = velocity[1];
        linearVelocity[2] = velocity[2];

        description = "MoveEntityTransientCommand -> "+entityID;
        
        local = true;

        entity = (PositionableEntity)model.getEntity(entityID);
        ruleBypassFlag = false;
        
        init();
    }
    
    /**
     * Overloaded constructor in order to specify the potential pick parent.
     * 
     * @param model The model to change
     * @param transactionID The transaction id
     * @param entityID The entity id of the entity to update
     * @param position The position in world coordinates (meters, Y-UP, X3D 
     * 	System)
     * @param velocity The velocity vector
     * @param pickParent The first entity returned from the pick parents
     */
    public MoveEntityTransientCommand(
    		WorldModel model, 
    		int transactionID, 
    		int entityID, 
    		double[] position, 
    		float[] velocity, 
    		Entity pickParent){
    	
    	this(model, transactionID, entityID, position, velocity);
    	
    	pickEntity = pickParent;
    }

    /**
     * Basic constructor.
     * 
     * @param model The model to change
     */
    public MoveEntityTransientCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        ruleBypassFlag = false;
        init();
    }

    /**
     * Common initialization method.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        transientState = true;
        undoableState = false;
    }

    // ----------------------------------------------------------
    // Methods required by DeadReckonedCommand
    // ----------------------------------------------------------
    /**
     * Get the dead reckoning params.
     *
     * @param position The position data
     * @param orientation The orientation data
     * @param lVelocity The linear velocity
     * @param aVelocity The angular velocity
     */
    public void getDeadReckoningParams(double[] position, float[] orientation,
            float[] lVelocity, float[] aVelocity) {

        position[0] = pos[0];
        position[1] = pos[1];
        position[2] = pos[2];

        lVelocity[0] = linearVelocity[0];
        lVelocity[1] = linearVelocity[1];
        lVelocity[2] = linearVelocity[2];
    }

    /**
     * Set the dead reckoning params.
     *
     * @param position The position data
     * @param orientation The orientation data
     * @param lVelocity The linear velocity
     * @param aVelocity The angular velocity
     */
    public void setDeadReckoningParams(double[] position, float[] orientation,
            float[] lVelocity, float[] aVelocity) {

        pos[0] = position[0];
        pos[1] = position[1];
        pos[2] = position[2];

        linearVelocity[0] = lVelocity[0];
        linearVelocity[1] = lVelocity[1];
        linearVelocity[2] = lVelocity[2];
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
     * Get the position.
     *
     * @param position The preallocated array to fill in
     */
    public void getPosition(double[] position) {
        position[0] = pos[0];
        position[1] = pos[1];
        position[2] = pos[2];
    }

    /**
     * Execute the command.
     */
    public void execute() {
        if (entity == null)
            return;
        entity.setPosition(pos, transientState);
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
             * <MoveEntityTransientCommand entityID='1' tID='' px='' py='' pz=''
             * vx='' vy='' vz=''/>
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<MoveEntityTransientCommand entityID='");
            sbuff.append(entityID);
            sbuff.append("' tID='");
            sbuff.append(transactionID);
            sbuff.append("' px='");
            sbuff.append(String.format("%.3f", pos[0]));
            sbuff.append("' py='");
            sbuff.append(String.format("%.3f", pos[1]));
            sbuff.append("' pz='");
            sbuff.append(String.format("%.3f", pos[2]));
            sbuff.append("' vx='");
            sbuff.append(String.format("%.3f", linearVelocity[0]));
            sbuff.append("' vy='");
            sbuff.append(String.format("%.3f", linearVelocity[1]));
            sbuff.append("' vz='");
            sbuff.append(String.format("%.3f", linearVelocity[2]));
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
        d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        String f;

        linearVelocity = new float[3];
        f = e.getAttribute("vx");
        linearVelocity[0] = Float.parseFloat(f);
        f = e.getAttribute("vy");
        linearVelocity[1] = Float.parseFloat(f);
        f = e.getAttribute("vz");
        linearVelocity[2] = Float.parseFloat(f);

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
	 * Get the pick parent entity
	 * 
	 * @return Entity or null if not set
	 */
	public Entity getPickParentEntity(){
		
		return pickEntity;
	}
	
	/**
	 * Set the pick parent entity
	 * 
	 * @param pickParent Pick parent entity to set
	 */
	public void setPickParentEntity(Entity pickParent) {
		pickEntity = pickParent;
	}
	
	/**
	 * Compare external command to this one to see if they are the same.
	 * 
	 * @param externalCommand command to compare against
	 * @return True if the same, false otherwise
	 */
	public boolean isEqualTo(Command externalCommand) {
		
		// Check for appropriate command type(s)
		if (externalCommand instanceof MoveEntityTransientCommand) {
			
			if (((MoveEntityTransientCommand)externalCommand).transactionID == 
				this.transactionID) {
				
				return true;
				
			}
			
			double[] position = new double[3];
			((MoveEntityTransientCommand)externalCommand).getPosition(
					position);
			
			if (((MoveEntityTransientCommand)externalCommand).getEntity() != 
				entity) {
				
				return false;
				
			} else if (((MoveEntityTransientCommand)externalCommand).
					getPickParentEntity() != this.pickEntity) {
				
				return false;
				
			} else if (!Arrays.equals(position, this.pos)) {
				
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