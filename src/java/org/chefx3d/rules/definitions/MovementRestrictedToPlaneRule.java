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

import org.chefx3d.view.common.EditorView;

/**
 * Defines the Movement Restricted To Plane check. If the movement is restricted
 * to a specific plane, and the plane of movement is not the plane specified by
 * the entity, then movement is not allowed.
 *
 * @author Ben Yarger
 * @version $Revision: 1.41 $
 */
public class MovementRestrictedToPlaneRule extends BaseRule  {

    private static final double TOLLERANCE = 0.00001;

    /** Status message when movement is restricted */
    private static final String MV_RESTRICTED_PROP =
        "org.chefx3d.rules.definitions.MovementRestrictedToPlaneRule.movementRestricted";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementRestrictedToPlaneRule(
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

        ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES moveRestriction =
            (ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES)
            	RulePropertyAccessor.getRulePropertyValue(
            		entity,
                    ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_PLANE_PROP);

        Boolean ignoreMoveRestriction = (Boolean)
        	RulePropertyAccessor.getRulePropertyValue(
        			entity,
                    ChefX3DRuleProperties.MOVEMENT_IGNORE_RESTRICT_ON_ADD);

        if (moveRestriction == null) {
            result.setResult(true);
            return(result);
        }

        // Get the transient position to move to
        double[] newPosition = new double[] {0.0, 0.0, 0.0};
        double[] startPosition = new double[] {0.0, 0.0, 0.0};

        Boolean isShadow =
            (Boolean) entity.getProperty(
                    entity.getParamSheetName(),
                    Entity.SHADOW_ENTITY_FLAG);

        if (ignoreMoveRestriction == null) {
            ignoreMoveRestriction = true;
        }

        if (isShadow == null) {
            isShadow = true;
        }

        if (ignoreMoveRestriction.booleanValue() && isShadow.booleanValue()) {
            result.setResult(true);
            return(result);
        }

        boolean showMessage = true;

        String[] stackClassifications = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
                ChefX3DRuleProperties.STACK_PROP);

        if(stackClassifications != null && stackClassifications.length > 0){
            showMessage = false;
        }

        // Perform operations depending on if command is transient
        if(command instanceof MoveEntityCommand){

            ((MoveEntityCommand)command).getEndPosition(newPosition);
            ((MoveEntityCommand)command).getStartPosition(startPosition);

            double[] transPosition = new double[3];

            transPosition[0] = newPosition[0] - startPosition[0];
            transPosition[1] = newPosition[1] - startPosition[1];
            transPosition[2] = newPosition[2] - startPosition[2];

            if(restrictToPlane(
                    transPosition,
                    moveRestriction,
                    isShadow,
                    showMessage)){

                newPosition[0] = startPosition[0] + transPosition[0];
                newPosition[1] = startPosition[1] + transPosition[1];
                newPosition[2] = startPosition[2] + transPosition[2];

                ((MoveEntityCommand)command).setEndPosition(newPosition);
            }

        } else if (command instanceof TransitionEntityChildCommand){

        	TransitionEntityChildCommand tranCmd =
        		(TransitionEntityChildCommand) command;

        	// Get the start and end parent zone relative positions
        	Entity startParentEntity = tranCmd.getStartParentEntity();
        	Entity endParentEntity = tranCmd.getEndParentEntity();

        	double[] startParentPosition =
        		TransformUtils.getPositionRelativeToZone(model, startParentEntity);

        	double[] endParentPosition =
        		TransformUtils.getPositionRelativeToZone(model, endParentEntity);

        	// Get the positions of the entity relative to those parents
        	tranCmd.getEndPosition(newPosition);
        	tranCmd.getStartPosition(startPosition);

            // Establish the full zone relative positions
            double[] startZoneRelativePos = new double[3];
            double[] endZoneRelativePos = new double[3];

            startZoneRelativePos[0] =
            	startParentPosition[0] + startPosition[0];
            startZoneRelativePos[1] =
            	startParentPosition[1] + startPosition[1];
            startZoneRelativePos[2] =
            	startParentPosition[2] + startPosition[2];

            endZoneRelativePos[0] = endParentPosition[0] + newPosition[0];
            endZoneRelativePos[1] = endParentPosition[1] + newPosition[1];
            endZoneRelativePos[2] = endParentPosition[2] + newPosition[2];

            // Set transPosition equal to the difference of the two positions
            double[] transPosition = new double[3];

            transPosition[0] = endZoneRelativePos[0] - startZoneRelativePos[0];
            transPosition[1] = endZoneRelativePos[1] - startZoneRelativePos[1];
            transPosition[2] = endZoneRelativePos[2] - startZoneRelativePos[2];

            if(restrictToPlane(
                    transPosition,
                    moveRestriction,
                    isShadow,
                    showMessage)){

            	double[] restrictedEndPos = new double[3];

            	restrictedEndPos[0] =
            		startZoneRelativePos[0] + transPosition[0];
            	restrictedEndPos[1] =
            		startZoneRelativePos[1] + transPosition[1];
            	restrictedEndPos[2] =
            		startZoneRelativePos[2] + transPosition[2];

            	// Convert this to a relative difference between restricted
            	// and initial zone relative end positions. Add this result
            	// to the newPosition and away we go.
            	restrictedEndPos[0] =
            		restrictedEndPos[0] - endZoneRelativePos[0];
            	restrictedEndPos[1] =
            		restrictedEndPos[1] - endZoneRelativePos[1];
            	restrictedEndPos[2] =
            		restrictedEndPos[2] - endZoneRelativePos[2];

            	newPosition[0] = newPosition[0] + restrictedEndPos[0];
            	newPosition[1] = newPosition[1] + restrictedEndPos[1];
            	newPosition[2] = newPosition[2] + restrictedEndPos[2];

            	tranCmd.setEndPosition(newPosition);
            }

        } else if (command instanceof MoveEntityTransientCommand) {

            ((MoveEntityTransientCommand)command).getPosition(newPosition);
            ((BasePositionableEntity)entity).getPosition(startPosition);

            // Make required adjustments, don't pass along changes if none made
            double[] transPosition = {
            newPosition[0] - startPosition[0],
            newPosition[1] - startPosition[1],
            newPosition[2] - startPosition[2]};

            if(restrictToPlane(
                    transPosition,
                    moveRestriction,
                    isShadow,
                    showMessage)){

                newPosition[0] = startPosition[0] + transPosition[0];
                newPosition[1] = startPosition[1] + transPosition[1];
                newPosition[2] = startPosition[2] + transPosition[2];

                ((MoveEntityTransientCommand)command).setPosition(newPosition);
            }
        }

        result.setResult(true);
        return(result);
    }

    /**
     * Based on the movement restriction, either recalculate so that the fixed
     * plane of movement is preserved or send back that no change was made.
     * The updated values are handed back in the newPosition array.
     *
     * @param newPosition double[] xyz position array - transient position - receives changed values
     * @param moveRestriction ChfX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES
     * @param isShadow True if is a shadow object, false otherwise.
     * If it is a shadow object then the XYPLANE restriction will not display
     * status message as it would be confusing for wall placement.
     * @return False if there is no restriction of movement, true otherwise
     */
    private boolean restrictToPlane(
            double[] newPosition,
            ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES moveRestriction,
            Boolean isShadow,
            boolean showMessage){

        String msg = intl_mgr.getString(MV_RESTRICTED_PROP);

        switch(moveRestriction){

        case NONE:
            return false;
        case XYPLANE:

            if(Math.abs(newPosition[2]) > TOLLERANCE){

                if(showMessage && !isShadow){
                    statusBar.setMessage(msg);
                }
            }
            newPosition[2] = 0.0;
            break;
        case YZPLANE:

            if(Math.abs(newPosition[0]) > TOLLERANCE){

                if(showMessage && !isShadow){
                    statusBar.setMessage(msg);
                }
            }
            newPosition[0] = 0.0;
            break;
        case XZPLANE:

            if(Math.abs(newPosition[1]) > TOLLERANCE){

                if(showMessage && !isShadow){
                    statusBar.setMessage(msg);
                }
            }
            newPosition[1] = 0.0;
            break;
        }

        return true;
    }
}
