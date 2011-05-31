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

package org.chefx3d.rules.util;

// External Imports

// Internal Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

/**
 * Position Collision Data object for storing and processing installation 
 * position collision requirement data.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public class PositionCollisionData {

	/** Entity collided with that may or may not be adjustable */
    private Entity entity;

    /** Distance to move the product to get it to line up */
    private double[] distance;

    /** Flag to determine if entity can be moved or is fixed */
    private boolean canBeMoved;

    /** Quick flag check to see if distance to move is 0 */
    private boolean adjustmentNeeded;

    /** Axis any adjustment should occur along */
    private ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS adjAxis;

    /**
     * Constructor
     *
     * @param entity Entity to track
     * @param distance Distance to move entity
     * @param adjAxis Axis along which adjustments should be made
     */
    PositionCollisionData(Entity entity, double[] distance, Enum adjAxis){

        this.distance = new double[3];

        this.setEntity(entity);
        this.setDistance(distance);
        this.setAdjustmentAxis(adjAxis);

        canBeMoved = false;
    }

    /**
     * Default constructor
     */
    PositionCollisionData(){

        this(null, null, null);
    }

    /**
     * Set the entity
     *
     * @param entity Entity to store
     */
    public void setEntity(Entity entity){

        this.entity = entity;
    }

    /**
     * Get the entity stored
     *
     * @return Entity
     */
    public Entity getEntity(){

        return entity;
    }

    /**
     * See if the entity is stored
     *
     * @return True if stored, false otherwise
     */
    public boolean hasEntity(){

        if(entity != null){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set the distance relative to the position requirement
     *
     * @param distance distance from position requirement to store
     */
    public void setDistance(double[] distance){

        if (distance == null) {

            this.distance[0] = 0.0;
            this.distance[1] = 0.0;
            this.distance[2] = 0.0;

        } else {

            this.distance[0] = distance[0];
            this.distance[1] = distance[1];
            this.distance[2] = distance[2];

        }

        // If magically, no adjustment is needed then set the flag
        if(this.distance[0] == 0.0 &&
                this.distance[1] == 0.0 &&
                this.distance[2] == 0.0){

            adjustmentNeeded = false;
        } else {
            adjustmentNeeded = true;
        }
    }

    /**
     * Set the distance relative to the position requirement
     *
     * @param distance distance from position requirement to send back
     */
    public void getDistance(double[] distance){

        distance[0] = this.distance[0];
        distance[1] = this.distance[1];
        distance[2] = this.distance[2];
    }

    /**
     * Set the adjustment axis value
     *
     * @param adjAxis Adjustment axis enum value
     */
    public void setAdjustmentAxis(Enum adjAxis){

        this.adjAxis =
            (ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)
            adjAxis;
    }

    /**
     * Get the adjustment axis
     *
     * @return POSITION_TARGET_ADJUSTMENT_AXIS Enum value
     */
    public ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS
    getAdjustmentAxis(){

        return adjAxis;
    }

    /**
     * Determine if an adjustment to the entity is needed to satisfy
     * the position requirement.
     *
     * @return True if adjustment is needed, false otherwise
     */
    public boolean isAdjustmentNeeded(){
        return adjustmentNeeded;
    }

    /**
     * Determine if the entity can be moved by the position requirement
     * adjustment algorithm.
     *
     * @return True if can be moved, false otherwise.
     */
    public boolean isFixedEntity(){
        return !canBeMoved;
    }

    /**
     * Perform can move entity check. Only case where canBeMoved will be
     * set to true is if the only model entities found in collision are
     * auto place entities. Or if the entity places the auto place
     * products. The samplingEntity passed in is the entity being acted
     * on by the user that should be removed from the list of collision
     * entities found during analysis.
     *
     * @param model WorldModel to reference
     * @param samplingEntity Entity being adjusted by user, to be ignored
     * @param rch RuleCollisionHandler to reference
     * in collision analysis
     */
    public void performCanMoveEntityCheck(
            WorldModel model,
            Entity samplingEntity,
            RuleCollisionHandler rch){

        double[] position = new double[3];

        if(entity instanceof PositionableEntity){

            ((PositionableEntity)entity).getPosition(position);

        } else {
            canBeMoved = false;
            return;
        }

        MoveEntityCommand mvCmd =
            new MoveEntityCommand(
                    model,
                    0,
                    (PositionableEntity)entity,
                    position,
                    position);

        rch.performCollisionCheck(mvCmd, true, false, false);

        if(!rch.performCollisionAnalysisHelper(
                entity,
                null,
                false,
                new int[] {samplingEntity.getEntityID()},
                false)){

            canBeMoved = false;
            return;
        }

        // Only circumstance where it can be moved is if it is only
        // colliding with its parent entity, or the collisions are
        // with entities that have been auto added or auto add.
        for (int i = 0; 
        	i < rch.getCollisionResults().getEntityMatches().size(); 
        	i++) {

        	Entity tmpEntity = 
        		rch.getCollisionResults().getEntityMatches().get(i);

            if(entity.getParentEntityID() == tmpEntity.getEntityID()){
                continue;
            }

            Boolean isAutoAdd = (Boolean)
            	RulePropertyAccessor.getRulePropertyValue(
            			tmpEntity,
            			ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

            Boolean autoAdds =
            	AutoAddUtility.performsAutoAddOperations(tmpEntity);

            if (!isAutoAdd && !autoAdds) {
            	canBeMoved = false;
            	return;
            }

            Boolean useInstallPosRequirements = (Boolean)
            	RulePropertyAccessor.getRulePropertyValue(
            			tmpEntity,
            			ChefX3DRuleProperties.
            			COLLISION_POSITION_REQUIREMENTS);

            if (useInstallPosRequirements) {
            	canBeMoved = false;
            	return;
            }

            Boolean useInstallPosMultiZoneRequirements = (Boolean)
        		RulePropertyAccessor.getRulePropertyValue(
        				tmpEntity,
        				ChefX3DRuleProperties.
        				COLLISION_POSITION_MULTI_ZONE_REQUIREMENTS);

            if (useInstallPosMultiZoneRequirements) {
            	canBeMoved = false;
            	return;
            }
        }

        canBeMoved = true;
    }
}
