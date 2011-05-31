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
import javax.vecmath.Vector3f;

// Internal Imports
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

import org.chefx3d.rules.rule.RuleEvaluationResult;

/**
 * Determines if placement uses incremental snaps to specific position values.
 * If so, it updates the position in the command for future checking.
 *
 * @author Ben Yarger
 * @version $Revision: 1.12 $
 */
public class AddIncrementalSnapCheckRule extends BaseRule  {

	/** Notification that movement uses incremental snap */
	private static final String SNAP_MOVE_PROP =
		"org.chefx3d.rules.definitions.MovementUsesIncrementalSnapsRule.usingSnap";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public AddIncrementalSnapCheckRule(
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
     * Check if the Entity moves based on incremental snaps. Returning false
     * here does not mean that the command cannot continue being processed. It
     * instead indicates that no absolute snap adjustment will be made.
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

		// Check if entity is using incremental snaps, return false if not
		Boolean usesIncrementalSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
    			entity,
    			ChefX3DRuleProperties.MOVEMENT_USES_INCREMENTAL_SNAPS_PROP);

		if(usesIncrementalSnaps == null ||
				usesIncrementalSnaps.booleanValue() == false){

	        result.setResult(false);
	        return(result);

		}

		// Get the incremental snap values
		Float xAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
    			entity,
    			ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_X_AXIS_SNAP_PROP);

		Float yAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
    			entity,
    			ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Y_AXIS_SNAP_PROP);

		Float zAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
    			entity,
    			ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Z_AXIS_SNAP_PROP);

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		double[] startPosition = new double[] {0.0, 0.0, 0.0};
		double[] newPosition = new double[] {0.0, 0.0, 0.0};

		// Perform operations depending on if command is transient
		if(entity instanceof PositionableEntity){

			// Begin adjustment
			((PositionableEntity)entity).getPosition(newPosition);
			((PositionableEntity)entity).getStartingPosition(startPosition);

			Vector3f translationVec = new Vector3f(
					(float)(newPosition[0] - startPosition[0]),
					(float)(newPosition[1] - startPosition[1]),
					(float)(newPosition[2] - startPosition[2]));

			applyIncrementalSnaps(
					newPosition,
					translationVec,
					model,
					entity.getParentEntityID(),
					xAxisSnapf,
					yAxisSnapf,
					zAxisSnapf);

			((PositionableEntity)entity).setPosition(newPosition, command.isTransient());
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Process each individual axis with increment data. Hand off each set of
	 * data for processing. The first parameter, position, will be updated
	 * with the increment value correct position.
	 *
	 * @param position double[] xyz position input from user
	 * @param translationVec Vector3f endPos - startPos
	 * @param model WorldModel
	 * @param parentID ParentEntityID
	 * @param xAxisSnapf Float x axis increment value (if null ignored)
	 * @param yAxisSnapf Float y axis increment value (if null ignored)
	 * @param zAxisSnapf Float z axis increment value (if null ignored)
	 */
	private void applyIncrementalSnaps(
			double[] position,
			Vector3f translationVec,
			WorldModel model,
			int parentID,
			Float xAxisSnapf,
			Float yAxisSnapf,
			Float zAxisSnapf){

		// Convert values
		Double xAxisSnap = null;
		Double yAxisSnap = null;
		Double zAxisSnap = null;

		if(xAxisSnapf != null){
			xAxisSnap = (double)xAxisSnapf.floatValue();
		}

		if(yAxisSnapf != null){
			yAxisSnap = (double) yAxisSnapf.floatValue();
		}

		if(zAxisSnapf != null){
			zAxisSnap = (double) zAxisSnapf.floatValue();
		}


		// Process values
		if(xAxisSnap != null){

			position[0] = processIncrementalSnapValues(
					position[0],
					translationVec.x,
					xAxisSnap);
		}

		if(yAxisSnap != null){

			position[1] = processIncrementalSnapValues(
					position[1],
					translationVec.y,
					yAxisSnap);
		}

		if(zAxisSnap != null){

			position[2] = processIncrementalSnapValues(
					position[2],
					translationVec.z,
					zAxisSnap);
		}
	}

	/**
	 * Processes each individual axis increments. Returns the new axis value
	 * for the axis data processed.
	 *
	 * @param axisPosition double value of the input position value
	 * @param axisDirection double value endpos - startpos
	 * @param incrementAxisValue Double value of the increment step
	 * @return new double position value for the axis
	 */
	private double processIncrementalSnapValues(
			double axisPosition,
			double axisDirection,
			Double incrementAxisValue){

		if(incrementAxisValue != null && incrementAxisValue == 0.0){
			return 0.0;
		}

		int index = (int) (axisPosition/incrementAxisValue);
		double val = axisPosition % incrementAxisValue;
		if(val > incrementAxisValue/2.0){

			/*
			 * Consider the direction of the translation when determining the
			 * partial increment step.
			 */
			if(axisDirection > 0){
				index++;
			} else if (axisDirection < 0){
				index--;
			}
		}

		double newPosition = ((double)index) * incrementAxisValue;

		return newPosition;
	}
}
