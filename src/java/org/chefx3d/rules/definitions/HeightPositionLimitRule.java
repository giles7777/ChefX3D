/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.definitions;

//External Imports
import java.util.*;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Restricts positioning based on height limit and bounds to prevent items
 * from being placed above or below a certain height.
 *
 * @author Ben Yarger
 * @version $Revision: 1.33 $
 */
public class HeightPositionLimitRule extends BaseRule  {

    /** Status message shown for transient actions */
    private static final String STATUS_MSG =
        "org.chefx3d.rules.definitions.HeightPositionLimitRule.statusMessage";

    /** Status message shown for non transient actions */
    private static final String POP_UP_MSG =
        "org.chefx3d.rules.definitions.HeightPositionLimitRule.popUpMessage";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public HeightPositionLimitRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.STANDARD;
    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

    /**
     * Perform the rule check
     *
     * @param entity Entity object
     * @param command Command object
     * @param result The state of the rule processing
     * @return boolean True if rule passes, false otherwise
     */
    protected RuleEvaluationResult performCheck(
            Entity entity,
            Command command,
            RuleEvaluationResult result) {

        this.result = result;

        // default return is true
        result.setResult(true);

        // bail out for non-model entities
        if (!entity.isModel()) {
            return result;
        }

        if(entity instanceof PositionableEntity){

            // Get the zone entity so we can determine correct evaluation
            // method.
        	Entity zoneEntity =
            	SceneHierarchyUtility.getActiveZoneEntity(model);

            // If we cannot locate zone entity go ahead and allow
            if(zoneEntity == null){
                return result;
            }

            Float maximumHeight =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.MAXIMUM_HEIGHT_PLACE_PROP);

            if (maximumHeight > 0) {

                // Check if height is legal
                if(!isHeightLegal(
                		model, command, maximumHeight, true, zoneEntity)){

                    // Deal with illegal heights
                    illegalHeightResponse(command);
                    return result;

                }
            }

            Float minimumHeight =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.MINIMUM_HEIGHT_PLACE_PROP);

            if (minimumHeight > 0) {

                // Check if height is legal
                if(!isHeightLegal(
                		model, command, minimumHeight, false, zoneEntity)){

                    // Deal with illegal heights
                    illegalHeightResponse(command);
                    return result;

                }
            }

        }

        return result;
    }

    /**
     * Determine if the current height is legal.
     *
     * @param model WorldModel
     * @param command Command to evaluate for position data
     * @param heightLimit Limit to check against
     * @param checkMax true to check maximum, false to check minimum
     * @param zoneEntity Zone to evaluate against
     * @return True if legal, false otherwise
     */
    private boolean isHeightLegal(
            WorldModel model,
            Command command,
            Float heightLimit,
            boolean checkMax,
            Entity zoneEntity){

        boolean heightIsLegal = true;
        double[] newPosition = new double[3];
        PositionableEntity entity = null;

        // Perform operations depending on if command is transient
        if(command instanceof MoveEntityCommand){
            MoveEntityCommand moveCommand = (MoveEntityCommand)command;

            // get the child entity being added
            entity = (PositionableEntity)moveCommand.getEntity();

            // get the position
            moveCommand.getEndPosition(newPosition);

            // get the parent entity, should already be in the scene
            Entity parentEntity =
                model.getEntity(entity.getParentEntityID());

            // since its in the scene, we can get the position
            // relative to the zone
            double[] endParentPos =
                TransformUtils.getPositionRelativeToZone(model, parentEntity);

            if (endParentPos != null) {
                // adjust the child to be relative to the zone so
            	// we can correctly adjust the snaps to a fixed position
                newPosition[0] = newPosition[0] + endParentPos[0];
                newPosition[1] = newPosition[1] + endParentPos[1];
                newPosition[2] = newPosition[2] + endParentPos[2];
            } else {
                //System.out.println("endParentPos null when expected in HeightPositionLimitRule during TransitionEntityChildCommand");
                //System.out.println("   parentEntity: " + parentEntity);
            }

        } else if (command instanceof TransitionEntityChildCommand &&
                                    !command.isTransient()) {

            TransitionEntityChildCommand transitionChildCommand =
                (TransitionEntityChildCommand)command;

            // get the child entity being added
            entity = (PositionableEntity)transitionChildCommand.getEntity();

            // get the position
            transitionChildCommand.getEndPosition(newPosition);

            // get the parent entity, should already be in the scene
            Entity parentEntity =
                transitionChildCommand.getEndParentEntity();

            // since its in the scene, we can get the position
            // relative to the zone
            double[] endParentPos =
                TransformUtils.getPositionRelativeToZone(model, parentEntity);

            if (endParentPos != null) {
                // adjust the child to be relative to the zone so we
            	// can correctly adjust the snaps to a fixed position
                newPosition[0] = newPosition[0] + endParentPos[0];
                newPosition[1] = newPosition[1] + endParentPos[1];
                newPosition[2] = newPosition[2] + endParentPos[2];
            } else {
                //System.out.println("endParentPos null when expected in HeightPositionLimitRule during TransitionEntityChildCommand");
                //System.out.println("   parentEntity: " + parentEntity);
            }
        } else if (command instanceof MoveEntityTransientCommand){

            MoveEntityTransientCommand moveTransient =
                (MoveEntityTransientCommand)command;

            // get the child entity being added
            entity = (PositionableEntity)moveTransient.getEntity();

            // get the position data
            moveTransient.getPosition(newPosition);

        } else if (command instanceof AddEntityChildCommand){

            // get the child entity being added
            entity =
                (PositionableEntity)
                ((AddEntityChildCommand)command).getEntity();

            // get the position
            entity.getPosition(newPosition);

            Entity parentEntity =
                ((AddEntityChildCommand)command).getParentEntity();

            if(parentEntity != null &&
                    parentEntity.getEntityID() != zoneEntity.getEntityID()) {
                double[] endParentPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (endParentPos != null) {
                    newPosition[0] = newPosition[0] + endParentPos[0];
                    newPosition[1] = newPosition[1] + endParentPos[1];
                    newPosition[2] = newPosition[2] + endParentPos[2];
                } else {
                    //System.out.println("endParentPos null when expected in HeightPositionLimitRule during moveTransient");
                    //System.out.println("   parentEntity: " + parentEntity);
                }
            } else {
                // Keep newPosition the same
            }
        }

        if (entity != null) {

            // get the bounds
            float[] bounds = new float[6];
            entity.getBounds(bounds);

            if (heightLimit > 0) {

                if (checkMax) {

                    if(zoneEntity.getType() == Entity.TYPE_SEGMENT){

                        if(newPosition[1] + bounds[3] > heightLimit){
                            heightIsLegal = false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_GROUNDPLANE_ZONE){

                        if(newPosition[2] + bounds[5] > heightLimit){
                            heightIsLegal = false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_MODEL_ZONE) {
                    	
                    	float[] productZoneBounds = BoundsUtils.getBounds(
                    			(PositionableEntity)zoneEntity, true);

                    	if(newPosition[1] + bounds[3] - productZoneBounds[2] > 
                    		heightLimit){
                    		
                            heightIsLegal = false;
                        }
                    }

                } else {

                    if(zoneEntity.getType() == Entity.TYPE_SEGMENT){

                        if(newPosition[1] + bounds[2] < heightLimit){
                            heightIsLegal = false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_GROUNDPLANE_ZONE){

                        if(newPosition[2] + bounds[4] < heightLimit){
                            heightIsLegal = false;
                        }
                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_MODEL_ZONE) {

                    	float[] productZoneBounds = BoundsUtils.getBounds(
                    			(PositionableEntity)zoneEntity, true);

                    	if(newPosition[1] + bounds[2] - productZoneBounds[2] < 
                    			heightLimit){
                    		
                            heightIsLegal = false;
                        }
                    }

                }
            }

            if (!heightIsLegal) {
                return heightIsLegal;
            }

            // we need to check children as well
            if (entity.hasChildren()) {
                heightIsLegal =
                    checkChildrenHeight(
                    		model,
                    		entity,
                    		zoneEntity,
                    		newPosition,
                    		checkMax);
            }

        }

        return heightIsLegal;

    }

    /**
     * Recurse the children to ensure their height restrictions are valid
     *
     * @param model WorldModel to reference
     * @param entity The entity to check children of
     * @param zoneEntity The active zone
     * @param newPosition The cumulative position from all parents
     * @param checkMax true to check maximum, false to check minimum
     * @return True if ok, false if fail
     */
    private boolean checkChildrenHeight(
    		WorldModel model,
            Entity entity,
            Entity zoneEntity,
            double[] newPosition,
            boolean checkMax) {

        double[] childPos = new double[3];
        boolean heightIsLegal = true;

        // get the list of children
        ArrayList<Entity> children = entity.getChildren();
        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity child = children.get(i);

            if (child instanceof PositionableEntity) {

                // get the current position
                ((PositionableEntity)child).getPosition(childPos);

                // get the relative position to the zone
                double[] endPos = new double[3];
                endPos[0] = newPosition[0] + childPos[0];
                endPos[1] = newPosition[1] + childPos[1];
                endPos[2] = newPosition[2] + childPos[2];

                // get the child's max height restriction
                Float maximumHeight =
                    (Float)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.MAXIMUM_HEIGHT_PLACE_PROP);

                // get the child's min height restriction
                Float minimumHeight =
                    (Float)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.MINIMUM_HEIGHT_PLACE_PROP);

                // get the bounds
                float[] bounds = new float[6];
                ((PositionableEntity)child).getBounds(bounds);

                // check the max height
                if (maximumHeight > 0) {

                    if (zoneEntity.getType() == Entity.TYPE_SEGMENT) {

                        if(endPos[1] + bounds[3] > maximumHeight){
                            return false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_GROUNDPLANE_ZONE) {

                        if(endPos[2] + bounds[5] > maximumHeight){
                            return false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_MODEL_ZONE) {

                        // To apply the limit check correctly we need to know
                        // if the root parent is a wall or floor
                        Entity wallOrFloorEntity =
                    		SceneHierarchyUtility.getWallOrFloorParent(
                    				model, entity);

                    	if (wallOrFloorEntity == null) {
                    		return true;
                    	}

                    	if (wallOrFloorEntity.getType() ==
                    		Entity.TYPE_SEGMENT) {

                    		if(endPos[1] + bounds[3] > maximumHeight){
                                return false;
                            }

                    	} else {

                    		if(endPos[2] + bounds[5] > maximumHeight){
                                return false;
                            }

                    	}
                    }

                }

                // check the min height
                if (minimumHeight > 0) {

                    if (zoneEntity.getType() == Entity.TYPE_SEGMENT) {

                        if(endPos[1] + bounds[2] < minimumHeight){
                            return false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_GROUNDPLANE_ZONE) {

                        if(endPos[2] + bounds[4] < minimumHeight){
                            return false;
                        }

                    } else if (zoneEntity.getType() ==
                    	Entity.TYPE_MODEL_ZONE) {

                        // To apply the limit check correctly we need to know
                        // if the root parent is a wall or floor
                        Entity wallOrFloorEntity =
                    		SceneHierarchyUtility.getWallOrFloorParent(
                    				model, entity);

                    	if (wallOrFloorEntity == null) {
                    		return true;
                    	}

                    	if (wallOrFloorEntity.getType() ==
                    		Entity.TYPE_SEGMENT) {

                    		if(endPos[1] + bounds[2] < minimumHeight){
                                return false;
                            }

                    	} else {

                    		if(endPos[2] + bounds[4] < minimumHeight){
                                return false;
                            }

                    	}
                    }

                }

                if (child.hasChildren()) {
                    heightIsLegal =
                        checkChildrenHeight(
                        		model,
                        		child,
                        		zoneEntity,
                        		endPos,
                        		checkMax);

                    // stop processing
                    if (!heightIsLegal) {
                        return false;
                    }

                }

            }

        }

        return true;

    }

    /**
     * Correct the command to reset gracefully. Ignore transient commands.
     * Add commands will be rejected by the rule returning false and thus
     * prevented from executing.
     *
     * @param command Command to reset
     */
    private void illegalHeightResponse(Command command){

        result.setStatusValue(ELEVATION_LEVEL.SEVERE);
        result.setResult(false);

        if(command.isTransient()){

            String msg = intl_mgr.getString(STATUS_MSG);
            statusBar.setMessage(msg);

        } else {

            String msg = intl_mgr.getString(POP_UP_MSG);
            popUpMessage.showMessage(msg);

            result.setNotApprovedAction(
            		RuleEvaluationResult.NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
            result.setApproved(false);

        }

    }

}
