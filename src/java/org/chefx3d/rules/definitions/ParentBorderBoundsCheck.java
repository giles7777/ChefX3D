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

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Confirm Entity resides inside parent bounds including evaluation of Entity
 * PosBuff values.
 *
 * @author Ben Yarger
 * @version $Revision: 1.16 $
 */
public class ParentBorderBoundsCheck extends BaseRule  {

	/** Status bar message */
	private static final String STATUS_BAR_MSG =
		"org.chefx3d.rules.definitions.ParentBorderBoundsCheck.statusMessage";

	/** Pop up message */
	private static final String POP_UP_MSG =
		"org.chefx3d.rules.definitions.ParentBorderBoundsCheck.popUpMessage";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ParentBorderBoundsCheck(
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

		/*
		 * Check for rule
		 */
		Boolean fitInsideRequired = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MUST_FIT_INSIDE_PARENT);

		if(fitInsideRequired == null ||
				fitInsideRequired == false){

	        result.setResult(true);
	        return(result);
		}

		/*
		 * Process rule
		 */
		if(command.isTransient()){

			if(!extractCommandData(command, model)){

				// Set status bar message
				String msg = intl_mgr.getString(STATUS_BAR_MSG);
				statusBar.setMessage(msg);
			}

		} else {

			if(!extractCommandData(command, model)){

				// Set pop up message
				String msg = intl_mgr.getString(POP_UP_MSG);
				popUpMessage.showMessage(msg);

				result.setStatusValue(ELEVATION_LEVEL.SEVERE);
				result.setApproved(false);
				result.setResult(false);
				return(result);

			}
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Strip the data data out of the command and hand off for bounds check.
	 *
	 * @param command Command being checked
	 * @param model WorldModel instance used by scene
	 * @return True if bounds is respected, false otherwise
	 */
	private boolean extractCommandData(
			Command command,
			WorldModel model){

		PositionableEntity parentEntity = null;
		PositionableEntity childEntity = null;

		Entity tmpParent = null;
		Entity tmpChild = null;

		double[] childPos = new double[3];
		float[] parentBounds = new float[6];
		float[] childBounds = new float[6];

		if(command instanceof AddEntityCommand){

			tmpChild = ((AddEntityCommand)command).getEntity();

			if(!tmpChild.isModel()){

				return true;
			}

			childEntity = (PositionableEntity) tmpChild;

			/*
			 * Extract parent information
			 */
			int parentID = childEntity.getParentEntityID();

			tmpParent = (Entity) model.getEntity(parentID);

			if(!tmpParent.isModel()){

				return true;
			}

			parentEntity = (PositionableEntity) tmpParent;

			parentEntity.getBounds(parentBounds);

			childEntity.getPosition(childPos);
			childEntity.getBounds(childBounds);

		} else if (command instanceof AddEntityChildCommand) {

			tmpChild = ((AddEntityChildCommand)command).getEntity();

			if(!tmpChild.isModel()){

				return true;
			}

			childEntity = (PositionableEntity) tmpChild;

			/*
			 * Extract parent information
			 */
			tmpParent = ((AddEntityChildCommand)command).getParentEntity();

			if(!tmpParent.isModel()){

				return true;
			}

			parentEntity = (PositionableEntity) tmpParent;

			parentEntity.getBounds(parentBounds);

			childEntity.getPosition(childPos);
			childEntity.getBounds(childBounds);

		} else if (command instanceof AddEntityChildTransientCommand) {

			tmpChild =
				((AddEntityChildTransientCommand)command).getEntity();

			if(!tmpChild.isModel()){

				return true;
			}

			childEntity = (PositionableEntity) tmpChild;

			/*
			 * Extract parent information
			 */
			tmpParent =
				((AddEntityChildTransientCommand)command).getParentEntity();

			if(!tmpParent.isModel()){

				return true;
			}

			parentEntity = (PositionableEntity) tmpParent;

			parentEntity.getBounds(parentBounds);

			childEntity.getPosition(childPos);
			childEntity.getBounds(childBounds);

		} else if (command instanceof MoveEntityCommand) {

			tmpChild = ((MoveEntityCommand)command).getEntity();

			if(!tmpChild.isModel()){

				return true;
			}

			childEntity = (PositionableEntity) tmpChild;

			/*
			 * Extract parent information
			 */
			int parentID = childEntity.getParentEntityID();

			tmpParent = model.getEntity(parentID);

			if(!tmpParent.isModel()){

				return true;
			}

			parentEntity = (PositionableEntity) tmpParent;

			parentEntity.getBounds(parentBounds);

			((MoveEntityCommand)command).getEndPosition(childPos);
			childEntity.getBounds(childBounds);

		} else if (command instanceof MoveEntityTransientCommand) {

			tmpChild = ((MoveEntityTransientCommand)command).getEntity();

			if(!tmpChild.isModel()){

				return true;
			}

			childEntity = (PositionableEntity) tmpChild;

			/*
			 * Extract parent information
			 */
			tmpParent =
				((MoveEntityTransientCommand)command).getPickParentEntity();

			if(tmpParent == null ||
					!tmpParent.isModel()){

				return true;
			}

			parentEntity = (PositionableEntity) tmpParent;

			parentEntity.getBounds(parentBounds);

			((MoveEntityTransientCommand)command).getPosition(childPos);
			childEntity.getBounds(childBounds);

		} else if (command instanceof TransitionEntityChildCommand) {

			tmpChild =
				((TransitionEntityChildCommand)command).getEntity();

			if(!tmpChild.isModel()){

				return true;
			}

			childEntity = (PositionableEntity) tmpChild;

			/*
			 * Extract parent information
			 */
			tmpParent =
				((TransitionEntityChildCommand)command).
				getEndParentEntity();


			if(!tmpParent.isModel()){

				return true;
			}

			parentEntity = (PositionableEntity) tmpParent;

			parentEntity.getBounds(parentBounds);

			((TransitionEntityChildCommand)command).getEndPosition(childPos);
			childEntity.getBounds(childBounds);

		} else {

			return true;
		}

		boolean result = boundsCheck(
				parentBounds,
				childPos,
				childBounds,
				childEntity);

		return result;
	}

	/**
	 * Bounds check calculation
	 *
	 * @param parentBounds parent bounds
	 * @param childPos child position
	 * @param childBounds child bounds
	 * @param childEntity child Entity
	 * @return True if inside bounds, false otherwise
	 */
	private boolean boundsCheck(
			float[] parentBounds,
			double[] childPos,
			float[] childBounds,
			PositionableEntity childEntity){

		/*
		 * Extract border buffer data
		 */
		Float frontBuff = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				childEntity,
				ChefX3DRuleProperties.FRONT_POS_BUFFER_PROP);

		Float backBuff = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				childEntity,
				ChefX3DRuleProperties.BACK_POS_BUFFER_PROP);

		Float leftBuff = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				childEntity,
				ChefX3DRuleProperties.LEFT_POS_BUFFER_PROP);

		Float rightBuff = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				childEntity,
				ChefX3DRuleProperties.RIGHT_POS_BUFFER_PROP);

		Float topBuff = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				childEntity,
				ChefX3DRuleProperties.TOP_POS_BUFFER_PROP);

		Float bottomBuff = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				childEntity,
				ChefX3DRuleProperties.BOTTOM_POS_BUFFER_PROP);

		if(frontBuff == null){
			frontBuff = 0.0f;
		}

		if(backBuff == null){
			backBuff = 0.0f;
		}

		if(leftBuff == null){
			leftBuff = 0.0f;
		}

		if(rightBuff == null){
			rightBuff = 0.0f;
		}

		if(topBuff == null){
			topBuff = 0.0f;
		}

		if(bottomBuff == null){
			bottomBuff = 0.0f;
		}

		/*
		 * Perform bounds check
		 */
		double parentLeft = parentBounds[0] + leftBuff;
		double childLeft = childPos[0] + childBounds[0];

		double parentRight = parentBounds[1] - rightBuff;
		double childRight = childPos[0] + childBounds[1];

		double parentBottom = parentBounds[2] + bottomBuff;
		double childBottom = childPos[1] + childBounds[2];

		double parentTop = parentBounds[3] - topBuff;
		double childTop = childPos[1] + childBounds[3];

		double parentBack = parentBounds[4] + backBuff;
		double childBack = childPos[2] + childBounds[4];

		double parentFront = parentBounds[5] - frontBuff;
		double childFront = childPos[2] + childBounds[5];

		boolean legalBounds = true;

		if(parentLeft > childLeft){

//			lastLegalPosition[0] = parentLeft - childBounds[0];
			legalBounds = false;

		} else if (parentRight < childRight){

//			lastLegalPosition[0] = parentRight - childBounds[1];
			legalBounds = false;

		}

		if (parentBottom > childBottom){

//			lastLegalPosition[1] = parentBottom - childBounds[2];
			legalBounds = false;

		} else if (parentTop < childTop){

//			lastLegalPosition[1] = parentTop - childBounds[3];
			legalBounds = false;

		}

		if (parentBack > childBack){

//			lastLegalPosition[2] = parentBack - childBounds[4];
			legalBounds = false;

		} else if (parentFront < childFront){

//			lastLegalPosition[2] = parentFront - childBounds[5];
			legalBounds = false;

		}

		return legalBounds;
	}
}
