/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
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
import java.util.*;

// Internal Imports

import org.chefx3d.model.PositionableData;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for scaling an entity.
 *
 * @author Ben Yarger
 * @version $Revision: 1.18 $
 */
public class ScaleEntityCommand implements 
	Command, RuleDataAccessor, RuleBypassFlag {

    /** The model */
    private WorldModel model;

    /** The entity */
    private PositionableEntity entity;

    /** The new position */
    private double[] newPos;

    /** The start position */
    private double[] startPos;

    /** The new scale */
    private float[] newScale;

    /** The start scale */
    private float[] startScale;

    /** The transaction ID */
    private int transactionID;

    /** The entity ID */
    private int entityID;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** Is this a local command */
    private boolean local;

    /** A list of strings of class names of rules to ignore */
    private HashSet<String> ignoreRuleList;

    /** Should the command die */
    private boolean shouldDie = false;

    /** A list of children at the start of a scale */
    private ArrayList<Entity> startChildren;

    /** A map of starting positionable data for children */
    private HashMap<Entity, PositionableData> startPositionableData;
    
    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;

    /**
     * Scale an entity.
     *
     * @param model World model
     * @param transID Transaction ID
     * @param entityID Entity ID
     * @param newPos New entity position
     * @param startPos Old entity position
     * @param newScale New entity scale
     * @param startScale Old entity scale
     */
    public ScaleEntityCommand(
            WorldModel model,
            int transID,
            PositionableEntity entity,
            double[] newPos,
            double[] startPos,
            float[] newScale,
            float[] startScale){

        this.model = model;
        this.transactionID = transID;
        this.entity = entity;

        this.newPos = new double[3];
        this.newPos[0] = newPos[0];
        this.newPos[1] = newPos[1];
        this.newPos[2] = newPos[2];

        this.startPos = new double[3];
        this.startPos[0] = startPos[0];
        this.startPos[1] = startPos[1];
        this.startPos[2] = startPos[2];

        this.newScale = new float[3];
        this.newScale[0] = newScale[0];
        this.newScale[1] = newScale[1];
        this.newScale[2] = newScale[2];

        this.startScale = new float[3];
        this.startScale[0] = startScale[0];
        this.startScale[1] = startScale[1];
        this.startScale[2] = startScale[2];

        startChildren = new ArrayList<Entity>();
        startPositionableData = new HashMap<Entity, PositionableData>();

        PositionableEntity pe;
        PositionableData pd;

        if (entity != null) {
            int len = entity.getChildCount();
            for (int i = 0; i < len; i++) {
                Entity child = entity.getChildAt(i);
                
                startChildren.add(child);
                if (child instanceof PositionableEntity) {
                    pe = (PositionableEntity) child;
                    pd = pe.getPositionableData();

                    startPositionableData.put(pe, pd);
                }

            }
        } else {
            throw new IllegalArgumentException("EntityID: " + entityID + " not found");
        }

        local = true;

        init();
    }

    /**
     * Scale an entity.
     *
     * @param model World model
     * @param transID Transaction ID
     * @param entityID Entity ID
     * @param newPos New entity position
     * @param startPos Old entity position
     * @param newScale New entity scale
     * @param startScale Old entity scale
     * @param startChildren Old entity child list
     */
    public ScaleEntityCommand(
            WorldModel model,
            int transID,
            PositionableEntity entity,
            double[] newPos,
            double[] startPos,
            float[] newScale,
            float[] startScale,
            ArrayList<Entity> startChildren,
            ArrayList<PositionableData> startPositions) {

        this(model, transID, entity, newPos, startPos, newScale, startScale);
        this.startChildren = startChildren;

        // Save the positionable data for each child
        Iterator<Entity> itr = startChildren.iterator();
        Entity child;
        PositionableEntity pe;
        PositionableData pd;

        int cnt = 0;
        while(itr.hasNext()) {
            child = itr.next();
            if (child instanceof PositionableEntity) {
                pe = (PositionableEntity) child;
                pd = startPositions.get(cnt);
                startPositionableData.put(pe, pd);
            }
            cnt++;
        }
    }

    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "ScaleEntity -> " + entityID;

        undoableState = true;
        transientState = false;
    }

    //---------------------------------------------------------------
    // Methods required by command
    //---------------------------------------------------------------

    /**
     * Execute the command.
     */
    public void execute() {
        entity.setPosition(newPos, transientState);
        entity.setStartingPosition(newPos);

        entity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                PositionableEntity.SCALE_PROP,
                newScale,
                transientState);

        ((PositionableEntity)entity).setStartingScale(newScale);

    }

    /**
     * Get the text description of this <code>Command</code>.
     */
    public String getDescription() {

        return description;
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

        return local;
    }

    /**
     * Get the transient state of this <code>Command</code>.
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
     * Redo the effects of this command.
     */
    public void redo() {

        execute();
    }

    /**
     * Set the text description of this <code>Command</code>.
     */
    public void setDescription(String desc) {

        description = desc;
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
     * Set the local flag.
     *
     * @param isLocal Is this a local update
     */
    public void setLocal(boolean isLocal) {

        local = isLocal;
    }
    
    /**
     * Set the start children to restore during an undo operation
     * 
     * @param startChildren Start children entities
     * @param startPositions Matching start position PositionableData
     */
    public void setStartChildren(
    		ArrayList<Entity> startChildren,
            ArrayList<PositionableData> startPositions) {
    	
    	this.startChildren = startChildren;

        // Save the positionable data for each child
        Iterator<Entity> itr = startChildren.iterator();
        Entity child;
        PositionableEntity pe;
        PositionableData pd;
        
        startPositionableData.clear();

        int cnt = 0;
        while(itr.hasNext()) {
            child = itr.next();
            if (child instanceof PositionableEntity) {
                pe = (PositionableEntity) child;
                pd = startPositions.get(cnt);
                startPositionableData.put(pe, pd);
            }
            cnt++;
        }
    }
    
    /**
     * Get the start children and respective positionable data stored with the 
     * command.
     * 
     * @return Start children and positionable data map
     */
    public HashMap<Entity, PositionableData> getStartChildrenData() {
    	
    	HashMap<Entity, PositionableData> results = 
    		new HashMap<Entity, PositionableData>();
    	
    	Object[] keys = startPositionableData.keySet().toArray();
    	
    	for (int i = 0; i < keys.length; i++) {
    		results.put((Entity)keys[i], startPositionableData.get(keys[i]));
    	}
    	
    	return results;
    }

    /**
     * Undo the effects of this command.
     */
    public void undo() {

        entity.setPosition(startPos, transientState);
        entity.setStartingPosition(startPos);

        entity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                PositionableEntity.SCALE_PROP, startScale,
                transientState);

        ((PositionableEntity)entity).setStartingScale(startScale);

        restoreChildren();
    }

    //---------------------------------------------------------------
    // Methods required by RuleDataAccessor
    //---------------------------------------------------------------

    /**
     * Return the entity.
     *
     * @return entity
     */
    public Entity getEntity() {

        return entity;
    }

    /**
     * Get the world model.
     *
     * @return model
     */
    public WorldModel getWorldModel() {

        return model;
    }

    //---------------------------------------------------------------
    // Class methods
    //---------------------------------------------------------------

    /**
     * Get the new position.
     *
     * @param pos Entity position
     */
    public void getNewPosition(double[] newPos){

        newPos[0] = this.newPos[0];
        newPos[1] = this.newPos[1];
        newPos[2] = this.newPos[2];
    }

    /**
     * Set the new position.
     *
     * @param pos New position
     */
    public void setNewPosition(double[] newPos){

        this.newPos[0] = newPos[0];
        this.newPos[1] = newPos[1];
        this.newPos[2] = newPos[2];
    }

    /**
     * Get the new scale
     *
     * @param scale Entity scale
     */
    public void getNewScale(float[] newScale){

        newScale[0] = this.newScale[0];
        newScale[1] = this.newScale[1];
        newScale[2] = this.newScale[2];
    }

    /**
     * Set the new scale
     *
     * @param scale New scale
     */
    public void setNewScale(float[] newScale){

        this.newScale[0] = newScale[0];
        this.newScale[1] = newScale[1];
        this.newScale[2] = newScale[2];
    }

    /**
     * Get the old position.
     *
     * @param pos Entity position
     */
    public void getOldPosition(double[] startPos){

        startPos[0] = this.startPos[0];
        startPos[1] = this.startPos[1];
        startPos[2] = this.startPos[2];
    }

    /**
     * Set the old position.
     *
     * @param pos New position
     */
    public void setOldPosition(double[] startPos){

        this.startPos[0] = startPos[0];
        this.startPos[1] = startPos[1];
        this.startPos[2] = startPos[2];
    }

    /**
     * Get the old scale
     *
     * @param scale Entity scale
     */
    public void getOldScale(float[] scale){

        scale[0] = this.startScale[0];
        scale[1] = this.startScale[1];
        scale[2] = this.startScale[2];
    }

    /**
     * Set the old scale
     *
     * @param scale New scale
     */
    public void setOldScale(float[] scale){

        this.startScale[0] = scale[0];
        this.startScale[1] = scale[1];
        this.startScale[2] = scale[2];
    }

    /**
     * Get the list of rules to ignore.
     *
     * @return The list of ignored rules
     */
    public HashSet<String> getIgnoreRuleList() {
        return ignoreRuleList;
    }

    /**
     * Set the list of rules to ignore.
     *
     * @param ignoreRuleList The name of the rules to ignore
     */
    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

    /**
     * Reset the entity to the starting values.  The command will still get its
     * execute called.
     */
    public void resetToStart() {

        this.newPos[0] = startPos[0];
        this.newPos[1] = startPos[1];
        this.newPos[2] = startPos[2];

        this.newScale[0] = startScale[0];
        this.newScale[1] = startScale[1];
        this.newScale[2] = startScale[2];

        restoreChildren();
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
        ScaleEntityCommand cmd = (ScaleEntityCommand) externalCommand;

        if (cmd.getTransactionID() == this.transactionID) {
            return true;
        }

        if (cmd.getEntity() != this.entity)
            return false;

        double[] startPosition = new double[3];
        cmd.getOldPosition(startPosition);
        if (!Arrays.equals(startPosition, this.startPos))
            return false;

        double[] endPosition = new double[3];
        cmd.getNewPosition(endPosition);
        if (!Arrays.equals(endPosition, this.newPos))
            return false;

        float[] startScale = new float[3];
        cmd.getOldScale(startScale);
        if (!Arrays.equals(startScale, this.startScale))
            return false;

        float[] endScale = new float[3];
        cmd.getNewScale(endScale);
        if (!Arrays.equals(endScale, this.newScale))
            return false;

        return true;
    }

    /**
     * Override object's equals method
     */
    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ScaleEntityCommand) {
            return isEqualTo((Command)obj);
        }

        return false;
    }

    /**
     * Restore children list to initial value.
     */
    private void restoreChildren() {
        ArrayList<Entity> children = entity.getChildren();
        int len = startChildren.size();
        for (int i = 0; i < len; i++) {
            Entity startChild = startChildren.get(i);
            if (!children.contains(startChild)) {
                entity.addChild(startChild);
            }

            PositionableData pd = startPositionableData.get(startChild);

            if (!(startChild instanceof PositionableEntity)) {
                // ignore non positionable entities
            } else if (pd == null) {
                errorReporter.errorReport("Positionable Data not found for child in ScaleEntityCommand: " + entity, null);
            } else {

                PositionableEntity pe = (PositionableEntity) startChild;

                // Restore Positionable data if its changed
                double[] cPos = new double[3];
                pe.getPosition(cPos);

                if (pd.pos[0] != cPos[0] || pd.pos[1] != cPos[1] || pd.pos[2] != cPos[2])
                    pe.setPosition(pd.pos, false);

                float[] cRot = new float[4];
                pe.getRotation(cRot);

                if (pd.rot[0] != cRot[0] || pd.rot[1] != cRot[1]
                   ||pd.rot[2] != cRot[2] || pd.rot[3] != cRot[3]) {

                    pe.setRotation(pd.rot, false);
                }

                float[] cScale = new float[3];
                pe.getScale(cScale);

                if (pd.scale[0] != cScale[0] || pd.scale[1] != cScale[1]
                   || pd.scale[2] != cScale[2]) {

                    pe.setScale(pd.scale);
                }
            }
        }

        // Remove any entities that are no longer needed
        children = entity.getChildren();

        len = children.size();
        //for (int i = 0; i < len; i++) {
        for (int i = len-1; i >= 0; i--) {
            Entity child = children.get(i);
            if (!startChildren.contains(child)) {
                entity.removeChild(child);
            }
        }

    }
    
    //--------------------------------------------------------------------------
    // Methods required by RuleBypassFlag
    //--------------------------------------------------------------------------

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
