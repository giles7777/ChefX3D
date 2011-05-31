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
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.rules.util.BoundsUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
* Makes sure that objects with immediate parents that are segment
* entities respect the planar bounds of the segment entity.
*
* @author Ben Yarger
* @version $Revision: 1.30 $
*/
public class SegmentBoundsCheckRule extends BaseRule  {

	/** Pop up message when illegal bounds exists for place */
	private static final String BOUNDS_PROP =
		"org.chefx3d.rules.definitions.SegmentBoundsCheckRule.outsideBounds";

	/**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SegmentBoundsCheckRule(
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
        
        if (!(entity instanceof PositionableEntity)) {
        	result.setResult(true);
	        return(result);
        }
        
        // Check for ignore wall edge snap flag.
        Boolean restrictToBoundary =
            (Boolean) RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_BOUNDARY);

		if(!restrictToBoundary || model == null){
	        result.setResult(true);
	        return(result);
		}

		int parentEntityID = entity.getParentEntityID();
		Entity parentEntity = model.getEntity(parentEntityID);
		float[] rotation = new float[] {0.0f, 0.0f, 1.0f, 0.0f};

		// TransitionEntityChildCommand case specific fields
		double[] endPos = new double[3];
		double[] currentPos = new double[3];
		int currentParentEntityId = -1;
		//--------------------------------------------------

		if(entity instanceof PositionableEntity){
			((PositionableEntity)entity).getRotation(rotation);
		}

		if(command instanceof AddEntityChildCommand){

			parentEntity =
				((AddEntityChildCommand)command).getParentEntity();
			parentEntityID = parentEntity.getEntityID();
			
            result.setResult(true);
            return(result);

		} else if (command instanceof AddEntityChildTransientCommand){

			parentEntity =
				((AddEntityChildTransientCommand)command).getParentEntity();
			parentEntityID = parentEntity.getEntityID();

		} else if (command instanceof RotateEntityCommand) {

			((RotateEntityCommand)command).getCurrentRotation(rotation);

		} else if (command instanceof RotateEntityTransientCommand) {

			((RotateEntityTransientCommand)command).getCurrentRotation(rotation);

		} else if (command instanceof TransitionEntityChildCommand) {

			TransitionEntityChildCommand tranCmd =
				(TransitionEntityChildCommand) command;

			PositionableEntity posEntity = (PositionableEntity) entity;

			endPos = new double[3];
			currentPos = new double[3];

			tranCmd.getEndPosition(endPos);
			posEntity.getPosition(currentPos);

			currentParentEntityId = posEntity.getParentEntityID();
			int endParentEntityID = tranCmd.getEndParentEntity().getEntityID();

			posEntity.setParentEntityID(endParentEntityID);
			posEntity.setPosition(endPos, false);

			parentEntityID = endParentEntityID;

		}

		/*
		 * Check for out of bounds on wall. If there are more than one
		 * segment collisions, we have no way of knowing which segment
		 * was the intended collision, so just grab the first one in
		 * the list.
		 */
		boolean inBounds =
			BoundsUtils.performSegmentBoundsCheck(
				model,
				(PositionableEntity) entity,
				null,
				null,
				null);

		// Reset values back for entity
		if (command instanceof TransitionEntityChildCommand) {

			PositionableEntity posEntity = (PositionableEntity) entity;

			posEntity.setParentEntityID(currentParentEntityId);
			posEntity.setPosition(currentPos, false);

		}

		if (!inBounds) {

			if(command.isTransient()){

				String msg = intl_mgr.getString(BOUNDS_PROP);
				statusBar.setMessage(msg);

				result.setStatusValue(ELEVATION_LEVEL.SEVERE);
		        result.setResult(false);
		        return(result);

			} else if(command instanceof RotateEntityCommand){

				String msg = intl_mgr.getString(BOUNDS_PROP);
				popUpMessage.showMessage(msg);

				result.setStatusValue(ELEVATION_LEVEL.SEVERE);
				result.setApproved(false);
				result.setNotApprovedAction(
						NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                result.setResult(false);
                return(result);

			} else if(command instanceof MoveEntityCommand){

				String msg = intl_mgr.getString(BOUNDS_PROP);
				popUpMessage.showMessage(msg);

                result.setStatusValue(ELEVATION_LEVEL.SEVERE);
				result.setApproved(false);
				result.setNotApprovedAction(
						NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                result.setResult(false);
                return(result);

			} else if (command instanceof TransitionEntityChildCommand &&
					command.isTransient() == false){

				String msg = intl_mgr.getString(BOUNDS_PROP);
				popUpMessage.showMessage(msg);

				result.setStatusValue(ELEVATION_LEVEL.SEVERE);
				result.setApproved(false);
				result.setNotApprovedAction(
						NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                result.setResult(false);
                return(result);

			} else if (command instanceof ScaleEntityCommand) {

				String msg = intl_mgr.getString(BOUNDS_PROP);
				popUpMessage.showMessage(msg);

				result.setStatusValue(ELEVATION_LEVEL.SEVERE);
				result.setApproved(false);
				result.setNotApprovedAction(
						NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                result.setResult(false);
                return(result);

			} else {

				// Handles add cases
				String msg = intl_mgr.getString(BOUNDS_PROP);
				popUpMessage.showMessage(msg);

				result.setApproved(false);
				result.setNotApprovedAction(
						NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                result.setResult(false);
                return(result);

			}
		}

        result.setResult(true);
        return(result);
	}

	//---------------------------------------------------------------
	// Local methods
	//---------------------------------------------------------------

}
