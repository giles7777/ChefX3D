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
import java.util.ArrayList;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * This class determines if a object with a proper overhang value
 * is over hanging to much. If so it resets the object.
 * @author jonhubba
 * @version $Revision: 1.47 $
 */
public class OverhangLimitRule extends BaseRule {

    /** Pop up message when illegal bounds exists for place */
    private static final String ADD_FAIL_PROP =
        "org.chefx3d.rules.definitions.OverHangLimitRule.addPopup";

    /** Pop up message when illegal bounds exists for place */
    private static final String SCALE_FAIL_PROP =
        "org.chefx3d.rules.definitions.OverHangLimitRule.scalePopup";

    /** Pop up message when illegal bounds exists for place */
    private static final String MOVE_FAIL_PROP =
        "org.chefx3d.rules.definitions.OverHangLimitRule.movePopup";
    
    /** Status message when illegal bounds exists */
    private static final String STATUS_MSG_PROP = 
    	"org.chefx3d.rules.definitions.OverHangLimitRule.statusMsg";
    
    /** Status message to display to user during illegal transient bounds */
    private static String STATUS_MSG; 

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public OverhangLimitRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);
        
        STATUS_MSG =
        	this.intl_mgr.getString(STATUS_MSG_PROP);

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

        // does the item have an overhang limit
        Boolean checkOverHang =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.RESPECTS_OVERHANG_LIMIT);
        
        if (!command.isTransient()) {
        	int b = 0;
        	b++;
        }

        boolean overhangValid = true;
        if (checkOverHang) {
            overhangValid = checkOverhangLimit(
            		model, command, entity, null);
        }

        if(!overhangValid && !command.isTransient()) {
        	result.setApproved(false);
        	result.setNotApprovedAction(NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
            result.setResult(false);
            return(result);
        } else if (!overhangValid) {
        	result.setStatusMessage(STATUS_MSG);
        	result.setStatusValue(ELEVATION_LEVEL.SEVERE);
        }

        result.setResult(true);
        return(result);

    }

    /**
     * Uses cumulative bounds logic to get the extents of colliding items
     * Then determines if the bounds of the entity minus the bounds of
     * the colliding obj (usually a standard) are under the overhang limit.
     * 
     * @param model WorldModel to reference
     * @param command Command affecting entity
     * @param entity Entity affected by command
     * @param ignoreEntityIdList Entity ids to ignore in collision check
     * @return True if overhang is valid, false otherwise
     */
    protected boolean checkOverhangLimit (
            WorldModel model,
            Command command,
            Entity entity,
            int[] ignoreEntityIdList) {
  
	    	Float overHangLimit =
	            (Float)RulePropertyAccessor.getRulePropertyValue(
	                    entity,
	                    ChefX3DRuleProperties.OVERHANG_LIMIT);

	        Float overHangMinimum =
	            (Float)RulePropertyAccessor.getRulePropertyValue(
	                    entity,
	                    ChefX3DRuleProperties.OVERHANG_MINIMUM);

            boolean overhangLimitExceeded =
            	BoundsUtils.checkOverhangLimit(
            			model,
            			command,
            			entity,
            			ignoreEntityIdList,
            			rch);

            // check for bounds X AXIS only
            if(overhangLimitExceeded && !command.isTransient()) {

                if (overHangLimit == 0.0f && overHangMinimum == -0.001f) {
                	
                    if(attemptScaleChange(
                            model,
                            entity,
                            command,
                            overHangMinimum)) {

                        return true;
                    }
                }

                // Pop up the message and then do the reset operation
            	String failInfo =
                	this.intl_mgr.getString(SCALE_FAIL_PROP);           

                if(command instanceof ScaleEntityCommand||
                        command instanceof ScaleEntityTransientCommand) {

                	failInfo =
                    	this.intl_mgr.getString(SCALE_FAIL_PROP);
                    revertScaleCommand(command);

                } else if (command instanceof MoveEntityCommand||
                        command instanceof MoveEntityTransientCommand) {

                	failInfo =
                    	this.intl_mgr.getString(MOVE_FAIL_PROP);
                    revertMoveCommand(command);

                } else if (command instanceof
                		TransitionEntityChildCommand) {

                	failInfo =
                    	this.intl_mgr.getString(MOVE_FAIL_PROP);
                    revertTransitionEntityChildCommand(command);

                } else if ( command instanceof AddEntityChildCommand) {
                	
                	failInfo =
                    	this.intl_mgr.getString(ADD_FAIL_PROP);
                }
                
                popUpMessage.showMessage(failInfo);

                return false;

            } else if (overhangLimitExceeded) {
            	
            	if (overHangLimit == 0.0f && 
            			overHangMinimum == -0.001f &&
            			command instanceof MoveEntityTransientCommand) {

            		// Because this is only going to get evaluated for
            		// MoveEntityTransientCommands we will not get any
            		// command action and will just get the evaluation we want.
            		return attemptScaleChange(
                            model,
                            entity,
                            command,
                            overHangMinimum);
                }

            	return false;
            }

        return true;

    }

   //--------------------------------------------------------------------------
   // Private methods
   //--------------------------------------------------------------------------

    /**
     * Reverts the scale command back to previous position and scale
     * @param command - command to revert
     */
   private void revertScaleCommand(Command command) {

       String failInfo = this.intl_mgr.getString(SCALE_FAIL_PROP);

       if (command instanceof ScaleEntityCommand) {

           result.setApproved(false);
           popUpMessage.showMessage(failInfo);

       } else if (command instanceof ScaleEntityTransientCommand) {

           statusBar.setMessage(failInfo);

       }

   }

   /**
    * Reverts the move command back to previous position
    * @param command - command to revert
    */
   private void revertMoveCommand(Command command) {

       String failInfo = intl_mgr.getString(MOVE_FAIL_PROP);

       if (command instanceof MoveEntityCommand) {

           result.setApproved(false);
           popUpMessage.showMessage(failInfo);

       } else if (command instanceof MoveEntityTransientCommand) {

           statusBar.setMessage(failInfo);

       }

   }

   /**
    * Reverts the move command back to previous position
    * @param command - command to revert
    */
   private void revertTransitionEntityChildCommand(Command command) {

       String failInfo = intl_mgr.getString(MOVE_FAIL_PROP);

       if (command instanceof TransitionEntityChildCommand &&
               !command.isTransient()) {

           result.setApproved(false);
           popUpMessage.showMessage(failInfo);

       } else {

           statusBar.setMessage(failInfo);

       }

   }



   /**
    * Attempt to adjust the scale to assist with the positioning of the
    * entity that otherwise would be rejected because of overhang limits.
    *
    * @param model WorldModel to reference
    * @param entity Entity being manipulated
    * @param command Command manipulating entity
    * @param overhangMinimum Minimum overhang
    * @return True if successful, false otherwise
    */
   private boolean attemptScaleChange(
           WorldModel model,
           Entity entity,
           Command command,
           Float overhangMinimum) {

        // Only attempt scale change for collisions with entities.
        // If colliding with zone or segment, this shouldn't be an
        // issue. Need to have at least two to make this work.

       ArrayList<Entity> entityMatches =
       	collisionResults.getEntityMatches();

        if (entityMatches.size() > 1) {

        	// sort them relative to the active zone
            PositionableEntity activeZone = (PositionableEntity)
            	SceneHierarchyUtility.getActiveZoneEntity(model);

            ArrayList<Entity> sortedEntities =
                TransformUtils.sortDescendingRelativePosValueOrder(
                    model,
                    entityMatches,
                    activeZone,
                    ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS.XAXIS,
                    true);

            PositionableEntity farRightEntity = null;
            PositionableEntity farLeftEntity = null;

            // Safety check right and left entity assignments to avoid NPE
            if (sortedEntities.get(0) instanceof PositionableEntity) {
                farRightEntity = (PositionableEntity)
                    sortedEntities.get(0);
            } else {
                return false;
            }

            if (sortedEntities.get(sortedEntities.size()-1)
                    instanceof PositionableEntity) {
                farLeftEntity = (PositionableEntity)
                    sortedEntities.get(sortedEntities.size()-1);
            } else {
                return false;
            }

            // Safety check position data to avoid NPE
            double[] farLeftPos =
                TransformUtils.getPositionRelativeToZone(model, farLeftEntity);

            if (farLeftPos == null) {
                return false;
            }

            double[] farRightPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, farRightEntity);

            if (farRightPos == null) {
                return false;
            }

            // Get the bounds data
            float[] farRightBounds = new float[6];
            float[] farLeftBounds = new float[6];

            farRightEntity.getBounds(farRightBounds);
            farLeftEntity.getBounds(farLeftBounds);

            // Perform the calculation
            double farRightEdge =
                farRightPos[0] +
                (farRightBounds[0] + farRightBounds[1]) / 2.0;
            double farLeftEdge =
                farLeftPos[0] +
                (farLeftBounds[0] + farLeftBounds[1]) / 2.0;

            double horizontalMidpoint = (farRightEdge + farLeftEdge) / 2.0;
            float horizontalWidth = (float)
                (farRightEdge - farLeftEdge + overhangMinimum * 2.0);

            if (!(entity instanceof PositionableEntity)) {
                return false;
            }

            PositionableEntity pEntity = (PositionableEntity) entity;

            if (exceedScaleLimits(
                    pEntity,
                    horizontalWidth,
                    ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS.XAXIS)) {
                return false;
            }

            float[] scale = getNewScale(
                    pEntity,
                    horizontalWidth,
                    ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS.XAXIS);

            // Handle commands accordingly
            if (command instanceof AddEntityChildCommand) {

                AddEntityChildCommand addCmd = (AddEntityChildCommand) command;

                Entity parentEntity = addCmd.getParentEntity();

                double[] parentEntityPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (parentEntityPos == null) {
                    return false;
                }

                double[] entityPos = new double[3];
                pEntity.getPosition(entityPos);
                entityPos[0] = horizontalMidpoint - parentEntityPos[0];
                pEntity.setPosition(entityPos, false);
                pEntity.setScale(scale);

            } else if (command instanceof AddEntityChildTransientCommand) {

                AddEntityChildTransientCommand addCmd =
                    (AddEntityChildTransientCommand) command;

                Entity parentEntity = addCmd.getParentEntity();

                double[] parentEntityPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (parentEntityPos == null) {
                    return false;
                }

                double[] entityPos = new double[3];
                pEntity.getPosition(entityPos);
                entityPos[0] = horizontalMidpoint - parentEntityPos[0];

                pEntity.setPosition(entityPos, false);
                pEntity.setScale(scale);

            } else if (command instanceof MoveEntityCommand) {

                MoveEntityCommand moveCmd = (MoveEntityCommand) command;

                Entity parentEntity =
                    model.getEntity(entity.getParentEntityID());

                if (parentEntity == null) {
                    return false;
                }

                double[] parentEntityPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (parentEntityPos == null) {
                    return false;
                }

                double[] entityPos = new double[3];
                moveCmd.getPosition(entityPos);
                entityPos[0] = horizontalMidpoint - parentEntityPos[0];

                moveCmd.setPosition(entityPos);

                // Fire off the scale command as a result
                fireOffScaleComand(model, pEntity, false, entityPos, scale);

            } else if (command instanceof MoveEntityTransientCommand) {
/*
                MoveEntityTransientCommand moveCmd =
                    (MoveEntityTransientCommand) command;

                double[] entityPos = new double[3];
                moveCmd.getPosition(entityPos);
                entityPos[0] = horizontalMidpoint;

                moveCmd.setPosition(entityPos);

                // Fire off the scale command as a result
                fireOffScaleComand(model, pEntity, true, entityPos, scale);
*/
            } else if (command instanceof TransitionEntityChildCommand) {

                if (!command.isTransient()) {

                    TransitionEntityChildCommand moveCmd =
                        (TransitionEntityChildCommand) command;

                    Entity parentEntity =
                        moveCmd.getEndParentEntity();

                    if (parentEntity == null) {
                        return false;
                    }

                    double[] parentEntityPos =
                        TransformUtils.getPositionRelativeToZone(
                        		model, parentEntity);

                    if (parentEntityPos == null) {
                        return false;
                    }

                    double[] entityPos = new double[3];
                    moveCmd.getEndPosition(entityPos);
                    entityPos[0] = horizontalMidpoint - parentEntityPos[0];

                    moveCmd.setEndPosition(entityPos);

                    // Fire off the scale command as a result
    /*              fireOffScaleComand(
                            model,
                            pEntity,
                            moveCmd.isTransient(),
                            entityPos,
                            scale);
*/
/*
                    // Need to fire off a copy of the
                    // TransitionEntityChildCommand to put it in the right spot
                    // since the scale command will be using the wrong parent.
                    double[] startingPos = new double[3];
                    float[] startingRot = new float[4];

                    moveCmd.getStartPosition(startingPos);
                    moveCmd.getStartingRotation(startingRot);

                    TransitionEntityChildCommand tranCmd =
                        new TransitionEntityChildCommand(
                                model,
                                (PositionableEntity) moveCmd.getEntity(),
                                moveCmd.getStartParentEntity(),
                                startingPos,
                                startingRot,
                                moveCmd.getEndParentEntity(),
                                entityPos,
                                startingRot,
                                moveCmd.isTransient());

                    addNewlyIssuedCommand(tranCmd);
                    */
                }

            } else if (command instanceof ScaleEntityCommand) {

                ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;

                Entity parentEntity =
                    model.getEntity(entity.getParentEntityID());

                if (parentEntity == null) {
                    return false;
                }

                double[] parentEntityPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (parentEntityPos == null) {
                    return false;
                }

                double[] entityPos = new double[3];
                scaleCmd.getNewPosition(entityPos);
                entityPos[0] = horizontalMidpoint - parentEntityPos[0];

                scaleCmd.setNewPosition(entityPos);
                scaleCmd.setNewScale(scale);

            } else if (command instanceof ScaleEntityTransientCommand) {

                ScaleEntityTransientCommand scaleCmd =
                    (ScaleEntityTransientCommand) command;

                Entity parentEntity =
                    model.getEntity(entity.getParentEntityID());

                if (parentEntity == null) {
                    return false;
                }

                double[] parentEntityPos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (parentEntityPos == null) {
                    return false;
                }

                double[] entityPos = new double[3];
                scaleCmd.getPosition(entityPos);
                entityPos[0] = horizontalMidpoint - parentEntityPos[0];

                scaleCmd.setPosition(entityPos);
                scaleCmd.setScale(scale);

            } else {
                return false;
            }

            return true;
        }

        return false;
   }

   /**
    * Check if the newSize is outside the size limits imposed for the entity.
    *
    * @param entity Entity to check newSize against
    * @param newSize New size to check
    * @param axis Axis to check against
    * @return True if exceeds scale limit, false otherwise
    */
   private boolean exceedScaleLimits(
           PositionableEntity entity,
           float newSize,
           ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS axis) {

        float[] maximumSize = (float[])
        RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.MAXIMUM_OBJECT_SIZE_PROP);

        float[] minimumSize = (float[])
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MINIMUM_OBJECT_SIZE_PROP);

        // Early out check if there are no size limits
        if (maximumSize == null && minimumSize == null) {
            return false;
        }

       switch(axis) {
           case XAXIS:
               if (newSize > maximumSize[0] || newSize < minimumSize[0]) {
                   return true;
               }
               break;
           case YAXIS:
               if (newSize > maximumSize[1] || newSize < minimumSize[1]) {
                   return true;
               }
               break;
           case ZAXIS:
               if (newSize > maximumSize[2] || newSize < minimumSize[2]) {
                   return true;
               }
               break;
       }

       return false;
   }

   /**
    * Calculate the resulting scale with the new size.
    *
    * @param entity Entity to change scale for
    * @param newSize New size for the entity
    * @param axis Axis to apply new size to
    * @return New scale
    */
   private float[] getNewScale(
           PositionableEntity entity,
           float newSize,
           ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS axis) {

       float[] scale = new float[3];
       float[] size = new float[3];

       entity.getScale(scale);
       entity.getSize(size);

       switch(axis) {
       case XAXIS:
           scale[0] = newSize / size[0];
           break;
       case YAXIS:
           scale[1] = newSize / size[1];
           break;
       case ZAXIS:
           scale[2] = newSize / size[2];
           break;
       }

       return scale;
   }

   /**
    * Fire off the appropriate scale command for rule evaluation and
    * execution.
    *
    * @param model WorldModel to reference
    * @param entity Entity to issue command for
    * @param isTransient Should be a transient command
    * @param newPos New position
    * @param newScale New scale
    */
   private void fireOffScaleComand(
           WorldModel model,
           PositionableEntity entity,
           boolean isTransient,
           double[] newPos,
           float[] newScale) {

    // get the children now
       ArrayList<Entity> startChildren = new ArrayList<Entity>();
       int len = entity.getChildCount();
       for (int i = 0; i < len; i++) {
           startChildren.add(entity.getChildAt(i));
       }

       ArrayList<PositionableData> startPositions =
        new ArrayList<PositionableData>();
       len = startChildren.size();
       for (int i = 0; i < len; i++) {
           Entity child = startChildren.get(i);

           if (child instanceof PositionableEntity) {
               startPositions.add(
                    ((PositionableEntity)child).
                        getPositionableData());
           }
       }

       double[] startPos = new double[3];
       float[] startScale = new float[3];

       entity.getStartingPosition(startPos);
       entity.getStartingScale(startScale);

       Command command = null;
       ArrayList<Command> commandList = new ArrayList<Command>();

       if (isTransient) {

           ScaleEntityTransientCommand scaleCmd =
               new ScaleEntityTransientCommand(
                   model,
                   model.issueTransactionID(),
                   entity,
                   newPos,
                   newScale);

           commandList.add(scaleCmd);
           command = new MultiTransientCommand(
                   commandList,
                   "MultiCommand -> Transient OverhangLimitRule adjust scale",
                   false);
       } else {
/*
            ScaleEntityCommand scaleCmd =
                new ScaleEntityCommand(
                    model,
                    model.issueTransactionID(),
                    entity.getEntityID(),
                    newPos,
                    startPos,
                    newScale,
                    startScale,
                    startChildren,
                    startPositions);
*/

            ScaleEntityCommand scaleCmd =
                new ScaleEntityCommand(
                    model,
                    model.issueTransactionID(),
                    entity,
                    newPos,
                    startPos,
                    newScale,
                    startScale);

            commandList.add(scaleCmd);
               command = new MultiCommand(
                       commandList,
                       "MultiCommand -> OverhangLimitRule adjust scale",
                       false,
                       false);
       }


       addNewlyIssuedCommand(command);
   }
}
