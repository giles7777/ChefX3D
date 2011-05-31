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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Watches for CX.freeFloating rule and if found limits movement and scale to
 * the extends of the original permanent parent along the movement constraint
 * axis.
 *
 * Execution of this rule is contingent on the permanent parent rule being set.
 *
 * @author Ben Yarger
 * @version $Revision: 1.23 $
 */
public class FreeFloatingChildRule extends BaseRule  {

	/** Static scale beyond bounds of parent calc error correction */
	private static float MAX_SIZE_TOL = 0.001f;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public FreeFloatingChildRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
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

        // Make sure permanent parent is in use
        Boolean permanent_parent = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.USES_PERMANENT_PARENT);

        if (permanent_parent == null ||
                permanent_parent.booleanValue() == false) {

            result.setResult(true);
            return(result);
        }

        // Make sure free floating constraint is in use
        Boolean freeFloat = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.FREE_FLOATING_CONSTRAINED);

        if (freeFloat == null ||
                freeFloat.booleanValue() == false) {

            result.setResult(true);
            return(result);
        }

        // Make sure we have a movement constraint to work with
        ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES
        	moveRestriction =
            (ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_PLANE_PROP);


        if(moveRestriction == null ||
                moveRestriction ==
                    ChefX3DRuleProperties.
                    MOVEMENT_PLANE_RESTRICTION_VALUES.NONE){

            result.setResult(true);
            return(result);
        }

        // We can only operate on the TransitionEntityChildCommand, and more
        // specifically the non transient case. We otherwise have no good way
        // of determining what the original parenting should be.
        //
        // For scale cases we can do either the transient or non transient
        // commands because the parent is not in flux.
        //
        if(command instanceof TransitionEntityChildCommand &&
                !command.isTransient()){

            TransitionEntityChildCommand tranCmd =
                (TransitionEntityChildCommand) command;

            Entity originalParent = tranCmd.getStartParentEntity();
            Entity endParent = tranCmd.getEndParentEntity();

            // Only work with parents of a TYPE_MODEL variant
            if(!originalParent.isModel()){
                result.setResult(true);
                return(result);
            }

            if(!(originalParent instanceof PositionableEntity)){
                result.setResult(true);
                return(result);
            }

            PositionableEntity positionableEntity =
                (PositionableEntity) entity;

            // Make sure the end parent is the same as the start parent
            // and if not, correct that issue. Do that by getting the
            // distance between the entity and the original parent, where the
            // entity has the end parent and end parent relative position
            // applied, and replacing that distance as the new end position
            // and setting the end parent back to the original.
            if(endParent.getEntityID() != originalParent.getEntityID()){

                // Get the end distance between positions
                double[] endPos = new double[3];
                double[] currentPos = new double[3];

                int endParentEntityID =
                	tranCmd.getEndParentEntity().getEntityID();
                int currentParentEntityID =
                	positionableEntity.getParentEntityID();

                tranCmd.getEndPosition(endPos);

                positionableEntity.getPosition(currentPos);

                positionableEntity.setPosition(endPos, false);
                positionableEntity.setParentEntityID(endParentEntityID);

                double[] distance =
                    TransformUtils.getDistanceBetweenEntities(
                            model,
                            (PositionableEntity) positionableEntity,
                            (PositionableEntity) originalParent,
                            false);

                if(distance == null){
                	//
                	// EMF: in theory, this can happen when
                	// RuleUtils.getParentZoneEntity(model, positionableEntity)
                	// is somehow different from
                	// RuleUtils.getParentZoneEntity(model, originalParent).
                	//
                	// ...silently ignore

                } else {

                	positionableEntity.setPosition(currentPos, false);
                	positionableEntity.setParentEntityID(
                			currentParentEntityID);

                	// Update the command
                	tranCmd.setEndParentEntity(originalParent);
                	tranCmd.setEndPosition(distance);

                	endParent = originalParent;
                }

            }

            // Get bounds and zone specific position data and make sure
            // position respects bounds of parent along axis constraint
            float[] endParentBounds = new float[6];
            double[] endParentPos = new double[3];

            ((PositionableEntity)endParent).getBounds(endParentBounds);
            ((PositionableEntity)endParent).getPosition(endParentPos);

            float[] entityBounds = new float[6];
            double[] entityEndPos = new double[3];

            positionableEntity.getBounds(entityBounds);
            tranCmd.getEndPosition(entityEndPos);

            // Examine bounds situation
            switch (moveRestriction) {

            case XYPLANE:
                // Nothing to do because we can't lock to x or y
                break;

            case XZPLANE:

                double parentLeftBounds = endParentBounds[0];
                double parentRightBounds = endParentBounds[1];

                double entityLeftBounds = entityEndPos[0] + entityBounds[0];
                double entityRightBounds = entityEndPos[0] + entityBounds[1];

                if (entityLeftBounds < parentLeftBounds) {

                    entityEndPos[0] =
                        entityEndPos[0] -
                        (entityLeftBounds - parentLeftBounds);

                } else if (entityRightBounds > parentRightBounds) {

                    entityEndPos[0] =
                        entityEndPos[0] -
                        (entityRightBounds - parentRightBounds);

                }

                break;

            case YZPLANE:

                double parentDownBounds = endParentBounds[2];
                double parentUpBounds = endParentBounds[3];

                double entityDownBounds = entityEndPos[1] + entityBounds[2];
                double entityUpBounds = entityEndPos[1] + entityBounds[3];

                if (entityDownBounds < parentDownBounds) {

                    entityEndPos[1] =
                        entityEndPos[1] -
                        (entityDownBounds - parentDownBounds);

                } else if (entityUpBounds > parentUpBounds) {

                    entityEndPos[1] =
                        entityEndPos[1] -
                        (entityUpBounds - parentUpBounds);

                }

                break;
            }

            // Update with changes
            tranCmd.setEndPosition(entityEndPos);

        } else if(command instanceof ScaleEntityCommand) {

            ScaleEntityCommand scaleCmd =
                (ScaleEntityCommand) command;

            int parentEntityID = entity.getParentEntityID();
            Entity parent = model.getEntity(parentEntityID);

            // Only operate on TYPE_MODEL variants
            if (!parent.isModel()) {
                result.setResult(true);
                return(result);
            }

            // Get bounds and zone specific position data and make sure
            // position respects bounds of parent along axis constraint
            float[] endParentBounds = new float[6];
            double[] endParentPos = new double[3];

            ((PositionableEntity)parent).getBounds(endParentBounds);
            ((PositionableEntity)parent).getPosition(endParentPos);

            float[] entityScale = new float[3];
            float[] entitySize = new float[3];
            double[] entityPos = new double[3];
            double[] entityStartingPos = new double[3];
            float[] entityStartingScale = new float[3];

            scaleCmd.getNewPosition(entityPos);
            scaleCmd.getNewScale(entityScale);
            scaleCmd.getOldPosition(entityStartingPos);
            scaleCmd.getOldScale(entityStartingScale);
            ((PositionableEntity)entity).getSize(entitySize);

            // Examine bounds situation
            switch (moveRestriction) {

            case XYPLANE:
                // Nothing to do because we can't lock to x or y
                break;

            case XZPLANE:

                double parentLeftBounds = endParentBounds[0];
                double parentRightBounds = endParentBounds[1];

                double entityLeftBounds =
                    entityPos[0] - ((entitySize[0] * entityScale[0])/2.0);

                double entityRightBounds =
                    entityPos[0] + ((entitySize[0] * entityScale[0])/2.0);

                if (entityLeftBounds < parentLeftBounds) {

                    double exceededAmt =
                        Math.abs(parentLeftBounds - entityLeftBounds);

                    double maxSize =
                        (entitySize[0] * entityScale[0]) -
                        exceededAmt - MAX_SIZE_TOL;

                    entityScale[0] = (float) (maxSize/entitySize[0]);

                    entityPos[0] =
                        entityStartingPos[0] -
                        ((entityScale[0] * entitySize[0]) -
                        (entityStartingScale[0] * entitySize[0])) / 2.0;

                } else if (entityRightBounds > parentRightBounds) {

                    double exceededAmt =
                        Math.abs(entityRightBounds - parentRightBounds);

                    double maxSize =
                        (entitySize[0] * entityScale[0]) -
                        exceededAmt - MAX_SIZE_TOL;

                    entityScale[0] = (float) (maxSize/entitySize[0]);

                    entityPos[0] =
                        entityStartingPos[0] +
                        ((entityScale[0] * entitySize[0]) -
                        (entityStartingScale[0] * entitySize[0])) / 2.0;
                }

                break;

            case YZPLANE:

                double parentDownBounds = endParentBounds[2];
                double parentUpBounds = endParentBounds[3];

                double entityDownBounds =
                    entityPos[1] - ((entitySize[1] * entityScale[1])/2.0);

                double entityUpBounds =
                    entityPos[1] + ((entitySize[1] * entityScale[1])/2.0);

                if (entityDownBounds < parentDownBounds) {

                    double exceededAmt =
                        Math.abs(parentDownBounds - entityDownBounds);

                    double maxSize =
                        (entitySize[1] * entityScale[1]) -
                        exceededAmt  - MAX_SIZE_TOL;

                    entityScale[1] = (float) (maxSize/entitySize[1]);

                    entityPos[1] =
                        entityStartingPos[1] -
                        ((entityScale[1] * entitySize[1]) -
                        (entityStartingScale[1] * entitySize[1])) / 2.0;

                } else if (entityUpBounds > parentUpBounds) {

                    double exceededAmt =
                        Math.abs(entityUpBounds - parentUpBounds);

                    double maxSize =
                        (entitySize[1] * entityScale[1]) -
                        exceededAmt  - MAX_SIZE_TOL;

                    entityScale[1] = (float) (maxSize/entitySize[1]);

                    entityPos[1] =
                        entityStartingPos[1] +
                        ((entityScale[1] * entitySize[1]) -
                        (entityStartingScale[1] * entitySize[1])) / 2.0;

                }

                break;
            }

            // Adjust values
            scaleCmd.setNewPosition(entityPos);
            scaleCmd.setNewScale(entityScale);

        } else if(command instanceof ScaleEntityTransientCommand) {

            ScaleEntityTransientCommand scaleCmd =
                (ScaleEntityTransientCommand) command;

            int parentEntityID = entity.getParentEntityID();
            Entity parent = model.getEntity(parentEntityID);

            // Only operate on TYPE_MODEL variants
            if (!parent.isModel()) {
                result.setResult(true);
                return(result);
            }

            // Get bounds and zone specific position data and make sure
            // position respects bounds of parent along axis constraint
            float[] endParentBounds = new float[6];
            double[] endParentPos = new double[3];

            ((PositionableEntity)parent).getBounds(endParentBounds);
            ((PositionableEntity)parent).getPosition(endParentPos);

            float[] entityScale = new float[3];
            float[] entitySize = new float[3];
            double[] entityPos = new double[3];
            double[] entityStartingPos = new double[3];
            float[] entityStartingScale = new float[3];

            scaleCmd.getPosition(entityPos);
            scaleCmd.getScale(entityScale);
            ((PositionableEntity)entity).getSize(entitySize);
            ((PositionableEntity)entity).getStartingPosition(
                    entityStartingPos);
            ((PositionableEntity)entity).getStartingScale(
                    entityStartingScale);

            // Examine bounds situation
            switch (moveRestriction) {

            case XYPLANE:
                // Nothing to do because we can't lock to x or y
                break;

            case XZPLANE:

                double parentLeftBounds = endParentBounds[0];
                double parentRightBounds = endParentBounds[1];

                double entityLeftBounds =
                    entityPos[0] - ((entitySize[0] * entityScale[0])/2.0);

                double entityRightBounds =
                    entityPos[0] + ((entitySize[0] * entityScale[0])/2.0);

                if (entityLeftBounds < parentLeftBounds) {

                    double exceededAmt =
                        Math.abs(parentLeftBounds - entityLeftBounds);

                    double maxSize =
                        (entitySize[0] * entityScale[0]) -
                        exceededAmt  - MAX_SIZE_TOL;

                    entityScale[0] = (float) (maxSize/entitySize[0]);

                    entityPos[0] =
                        entityStartingPos[0] -
                        ((entityScale[0] * entitySize[0]) -
                        (entityStartingScale[0] * entitySize[0])) / 2.0;

                } else if (entityRightBounds > parentRightBounds) {

                    double exceededAmt =
                        Math.abs(entityRightBounds - parentRightBounds);

                    double maxSize =
                        (entitySize[0] * entityScale[0]) -
                        exceededAmt  - MAX_SIZE_TOL;

                    entityScale[0] = (float) (maxSize/entitySize[0]);

                    entityPos[0] =
                        entityStartingPos[0] +
                        ((entityScale[0] * entitySize[0]) -
                        (entityStartingScale[0] * entitySize[0])) / 2.0;

                }

                break;

            case YZPLANE:

                double parentDownBounds = endParentBounds[2];
                double parentUpBounds = endParentBounds[3];

                double entityDownBounds =
                    entityPos[1] - ((entitySize[1] * entityScale[1])/2.0);

                double entityUpBounds =
                    entityPos[1] + ((entitySize[1] * entityScale[1])/2.0);

                if (entityDownBounds < parentDownBounds) {

                    double exceededAmt =
                        Math.abs(parentDownBounds - entityDownBounds);

                    double maxSize =
                        (entitySize[1] * entityScale[1]) -
                        exceededAmt - MAX_SIZE_TOL;

                    entityScale[1] = (float) (maxSize/entitySize[1]);

                    entityPos[1] =
                        entityStartingPos[1] -
                        ((entityScale[1] * entitySize[1]) -
                        (entityStartingScale[1] * entitySize[1])) / 2.0;

                } else if (entityUpBounds > parentUpBounds) {

                    double exceededAmt =
                        Math.abs(entityUpBounds - parentUpBounds);

                    double maxSize =
                        (entitySize[1] * entityScale[1]) -
                        exceededAmt - MAX_SIZE_TOL;

                    entityScale[1] = (float) (maxSize/entitySize[1]);

                    entityPos[1] =
                        entityStartingPos[1] +
                        ((entityScale[1] * entitySize[1]) -
                        (entityStartingScale[1] * entitySize[1])) / 2.0;

                }

                break;
            }

            // Adjust values
            scaleCmd.setPosition(entityPos);
            scaleCmd.setScale(entityScale);

        } else if (command instanceof AddEntityChildCommand) {
            AddEntityChildCommand cmd = (AddEntityChildCommand) command;
            Entity parent = cmd.getParentEntity();

            // Only work with parents of TYPE_MODEL variants
            if(!parent.isModel()){
                result.setResult(true);
                return(result);
            }

            if(!(parent instanceof PositionableEntity)){
                result.setResult(true);
                return(result);
            }

            PositionableEntity positionableEntity =
                (PositionableEntity) entity;


            // Get bounds and zone specific position data and make sure
            // position respects bounds of parent along axis constraint
            float[] parentBounds = new float[6];
            double[] parentPos = new double[3];

            ((PositionableEntity)parent).getBounds(parentBounds);
            ((PositionableEntity)parent).getPosition(parentPos);

            float[] entityBounds = new float[6];
            double[] entityEndPos = new double[3];

            positionableEntity.getBounds(entityBounds);
            ((PositionableEntity)entity).getPosition(entityEndPos);

            boolean outsideBounds = false;

            // Examine bounds situation
            switch (moveRestriction) {

            case XYPLANE:
                // Nothing to do because we can't lock to x or y
                break;

            case XZPLANE:

                double parentLeftBounds = parentBounds[0];
                double parentRightBounds = parentBounds[1];

                double entityLeftBounds = entityEndPos[0] + entityBounds[0];
                double entityRightBounds = entityEndPos[0] + entityBounds[1];

                if (entityLeftBounds < parentLeftBounds) {
                    outsideBounds = true;

                    entityEndPos[0] =
                        entityEndPos[0] -
                        (entityLeftBounds - parentLeftBounds);

                } else if (entityRightBounds > parentRightBounds) {
                    outsideBounds = true;

                    entityEndPos[0] =
                        entityEndPos[0] -
                        (entityRightBounds - parentRightBounds);

                }

                break;

            case YZPLANE:

                double parentDownBounds = parentBounds[2];
                double parentUpBounds = parentBounds[3];

                double entityDownBounds = entityEndPos[1] + entityBounds[2];
                double entityUpBounds = entityEndPos[1] + entityBounds[3];

                if (entityDownBounds < parentDownBounds) {
                    outsideBounds = true;

                    entityEndPos[1] =
                        entityEndPos[1] -
                        (entityDownBounds - parentDownBounds);

                } else if (entityUpBounds > parentUpBounds) {
                    outsideBounds = true;

                    entityEndPos[1] =
                        entityEndPos[1] -
                        (entityUpBounds - parentUpBounds);

                }

                break;
            }

            // TODO: Eventualy we might decide to scale down for the user.
            if (outsideBounds) {
                result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                result.setApproved(false);
                result.setResult(false);
                return(result);
            }

        } else if (command instanceof MoveEntityTransientCommand) {
            MoveEntityTransientCommand cmd =
            	(MoveEntityTransientCommand) command;
            Entity parent = null;

            Integer initalParentEntityID = (Integer)
               RulePropertyAccessor.getRulePropertyValue(
                       entity,
                       ChefX3DRuleProperties.INITAL_ADD_PARENT);

            if (initalParentEntityID != null)
                parent = model.getEntity(initalParentEntityID.intValue());

            if (parent == null) {
                parent = cmd.getPickParentEntity();

                // Only use this parent if its part of the allowed parent list
                if (parent == null) {

                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    result.setResult(false);
                    return(result);
                }

                String[] allowed_parents = (String[]) entity.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

                if (allowed_parents == null || allowed_parents.length == 0) {

                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    result.setResult(false);
                    return(result);
                }

                String[] parent_classes =
                    (String[]) parent.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

                if (parent_classes == null || parent_classes.length == 0) {

                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    result.setApproved(false);
                    result.setResult(false);
                    return(result);
                }

                boolean match = false;

                for(int i=0; i < allowed_parents.length; i++) {
                    for(int j=0; j < parent_classes.length; j++) {

                        if (allowed_parents[i].equalsIgnoreCase(
                        		parent_classes[j])) {

                            match = true;
                            break;
                        }
                    }

                    if (match)
                        break;
                }

                if (!match) {
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    result.setApproved(false);
                    result.setResult(false);
                    return(result);
                }
            }

            PositionableEntity positionableEntity =
                (PositionableEntity) entity;

            // Get bounds and zone specific position data and make sure
            // position respects bounds of parent along axis constraint
            float[] parentBounds = new float[6];
            double[] parentPos =
            	TransformUtils.getPositionRelativeToZone(model, parent);

            ((PositionableEntity)parent).getBounds(parentBounds);
            //((PositionableEntity)parent).getPosition(parentPos);

            float[] entityBounds = new float[6];
            double[] entityEndPos =
            	TransformUtils.getPositionRelativeToZone(
            			model, positionableEntity);

            positionableEntity.getBounds(entityBounds);
            //positionableEntity.getPosition(entityEndPos);

            boolean outsideBounds = false;

            // Examine bounds situation
            switch (moveRestriction) {

            case XYPLANE:
                // Nothing to do because we can't lock to x or y
                break;

            case XZPLANE:

                double parentLeftBounds = parentPos[0] + parentBounds[0];
                double parentRightBounds = parentPos[0] + parentBounds[1];

                double entityLeftBounds = entityEndPos[0] + entityBounds[0];
                double entityRightBounds = entityEndPos[0] + entityBounds[1];

                if (entityLeftBounds < parentLeftBounds) {
                    outsideBounds = true;
                } else if (entityRightBounds > parentRightBounds) {
                    outsideBounds = true;
                }

                break;

            case YZPLANE:

                double parentDownBounds = parentPos[2] + parentBounds[2];
                double parentUpBounds = parentPos[2] + parentBounds[3];

                double entityDownBounds = entityEndPos[1] + entityBounds[2];
                double entityUpBounds = entityEndPos[1] + entityBounds[3];

                if (entityDownBounds < parentDownBounds) {
                    outsideBounds = true;
                } else if (entityUpBounds > parentUpBounds) {
                    outsideBounds = true;
                }
                break;
            }

            if (outsideBounds) {
                result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                result.setApproved(false);
                result.setResult(false);
                return(result);
            }
        }

        result.setResult(true);
        return(result);
    }
}
