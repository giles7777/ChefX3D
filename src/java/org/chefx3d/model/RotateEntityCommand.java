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
import java.util.HashSet;
import java.util.Arrays;

import org.w3c.dom.*;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DOMUtils;

/**
 * A command for moving an entity.
 *
 * @author Alan Hudson
 * @version $Revision: 1.32 $
 */
public class RotateEntityCommand implements 
	Command, RuleDataAccessor, RuleBypassFlag {
	
    /** The model */
    private BaseWorldModel model;

    /** The entity to update */
    private PositionableEntity entity;

    /** The curent rotation */
    private float[] rot;

    /** The end rotation */
    private float[] endRot;

    /** The start rotation */
    private float[] startRot;

    /** Is this a local add */
    private boolean local;

    /** The transactionID */
    private int transactionID;

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
    
    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;

    /**
     * Rotate an entity.
     *
     * @param model The model to change
     * @param transID The transactionID
     * @param entityID The unique entityID assigned by the view
     * @param endRot The final rotation(axis + angle in radians)
     * @param startRot The starting rotation(axis + angle in radians)
     */
    public RotateEntityCommand(WorldModel model, int transID, PositionableEntity entity,
            float[] endRot, float[] startRot) {

        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        this.entity = entity;
        
        transactionID = transID;

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

        local = true;

        ruleBypassFlag = false;

        init();
    }

    public RotateEntityCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;

        init();
    }

    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "RotateEntity -> " + entity;

        transientState = false;
        undoableState = true;
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
     * Get the transactionID for this command.
     *
     * @return The transactionID
     */
    public int getTransactionID() {
        return transactionID;
    }

    /**
     * Execute the command.
     */
    public void execute() {

        // make the final move of the entity
        rot[0] = endRot[0];
        rot[1] = endRot[1];
        rot[2] = endRot[2];
        rot[3] = endRot[3];

        entity.setRotation(rot, transientState);
        entity.setStartingRotation(rot);

    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {

        // System.out.println("RotateEntityCommand.undo()");
        // System.out.println(" rotate from: " + endRot[3]);
        // System.out.println(" rotate to: " + startRot[3]);

        rot[0] = startRot[0];
        rot[1] = startRot[1];
        rot[2] = startRot[2];
        rot[3] = startRot[3];

        entity.setRotation(rot, transientState);

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
             * <RotateEntityCommand entityID='1' rotx='' roty='' rotz='' />
             */
            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<RotateEntityCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' tID='");
            sbuff.append(transactionID);
            sbuff.append("' rotx='");
            sbuff.append(rot[0]);
            sbuff.append("' roty='");
            sbuff.append(rot[1]);
            sbuff.append("' rotz='");
            sbuff.append(rot[2]);
            sbuff.append("' rota='");
            sbuff.append(rot[3]);
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

        rot = new float[4];
        startRot = new float[4];
        endRot = new float[4];

        d = e.getAttribute("rotx");
        rot[0] = Float.parseFloat(d);
        d = e.getAttribute("roty");
        rot[1] = Float.parseFloat(d);
        d = e.getAttribute("rotz");
        rot[2] = Float.parseFloat(d);
        d = e.getAttribute("rota");
        rot[3] = Float.parseFloat(d);

        endRot[0] = rot[0];
        endRot[1] = rot[1];
        endRot[2] = rot[2];
        endRot[3] = rot[3];

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
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

   
    
    /**
     * Resets the rotation to the last known good.
     */
    public void resetToStart(){
    	
    	endRot[0] = startRot[0];
    	endRot[1] = startRot[1];
    	endRot[2] = startRot[2];
    	endRot[3] = startRot[3];
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
		
		if (externalCommand instanceof RotateEntityCommand) {
		
			if (((RotateEntityCommand)externalCommand).getTransactionID() == 
				this.transactionID) {
				
				return true;
				
			}
			
			float[] endRotation = new float[4];
			
			((RotateEntityCommand)externalCommand).getCurrentRotation(endRotation);
			
			if (((RotateEntityCommand)externalCommand).getEntity() != this.entity) {
				
				return false;
				
			} else if (!Arrays.equals(endRotation, this.endRot)) {
				
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