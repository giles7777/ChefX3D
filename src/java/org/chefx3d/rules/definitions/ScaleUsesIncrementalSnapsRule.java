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

import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if scale uses incremental snaps to specific position values.
 * If so, it updates the scale in the command for future checking.
 *
 * @author Ben Yarger
 * @version $Revision: 1.16 $
 */
public class ScaleUsesIncrementalSnapsRule extends BaseRule  {

	/** Notification that movement uses incremental snap */
	private static final String SNAP_SCALE_PROP =
		"org.chefx3d.rules.definitions.ScaleUsesIncrementalSnaps.statusMsg";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleUsesIncrementalSnapsRule(
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

		/*
		 * Make sure entity is a PositionableEntity
		 */
		if(!(entity instanceof PositionableEntity)){
	        result.setResult(true);
	        return(result);
		}

		// Check if entity is using incremental snaps, return false if not
		Boolean usesIncrementalSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_USES_INCREMENTAL_SNAPS_PROP);

		if(usesIncrementalSnaps == null ||
				usesIncrementalSnaps.booleanValue() == false){
	        result.setResult(false);
	        return(result);
		}

		// Set status bar message
		String usesIncSnapMsg = intl_mgr.getString(SNAP_SCALE_PROP);
		statusBar.setMessage(usesIncSnapMsg);

		// Get the incremental snap values
		Float xAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_INCREMENTAL_X_AXIS_SNAP_PROP);

		Float yAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_INCREMENTAL_Y_AXIS_SNAP_PROP);

		Float zAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_INCREMENTAL_Z_AXIS_SNAP_PROP);

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		double[] newPosition = new double[3];
		float[] newScale = new float[3];
		double[] startPosition = new double[3];
		float[] startScale = new float[3];
		float[] size = new float[3];

		((PositionableEntity)entity).getSize(size);

		// Perform operations depending on if command is transient
		if(command instanceof ScaleEntityCommand){

			((ScaleEntityCommand)command).getNewPosition(newPosition);
			((ScaleEntityCommand)command).getOldPosition(startPosition);
			((ScaleEntityCommand)command).getNewScale(newScale);
			((ScaleEntityCommand)command).getOldScale(startScale);

			applyIncrementalSnaps(
					((PositionableEntity)entity),
					newPosition,
					newScale,
					startPosition,
					startScale,
					size,
					xAxisSnapf,
					yAxisSnapf,
					zAxisSnapf);

			((ScaleEntityCommand)command).setNewPosition(newPosition);
			((ScaleEntityCommand)command).setNewScale(newScale);

		} else if (command instanceof ScaleEntityTransientCommand){

			((ScaleEntityTransientCommand)command).getPosition(newPosition);
			((ScaleEntityTransientCommand)command).getScale(newScale);

			((PositionableEntity)entity).getStartingPosition(startPosition);
			((PositionableEntity)entity).getStartingScale(startScale);

			applyIncrementalSnaps(
					((PositionableEntity)entity),
					newPosition,
					newScale,
					startPosition,
					startScale,
					size,
					xAxisSnapf,
					yAxisSnapf,
					zAxisSnapf);

			((ScaleEntityTransientCommand)command).setPosition(newPosition);
			((ScaleEntityTransientCommand)command).setScale(newScale);
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Process each individual axis with increment data. Hand off each set of
	 * data for processing. The first parameter, position, will be updated
	 * with the increment value correct position.
	 *
	 * @param entity Entity to apply snaps to
	 * @param newPosition New position tied to scale
	 * @param newScale New scale
	 * @param startPosition Original position tied to original scale
	 * @param startScale Original scale
	 * @param xAxisSnapf Float x axis increment value (if null ignored)
	 * @param yAxisSnapf Float y axis increment value (if null ignored)
	 * @param zAxisSnapf Float z axis increment value (if null ignored)
	 */
	private void applyIncrementalSnaps(
			PositionableEntity entity,
			double[] newPosition,
			float[] newScale,
			double[] startPosition,
			float[] startScale,
			float[] size,
			Float xAxisSnapf,
			Float yAxisSnapf,
			Float zAxisSnapf){

		// Process values
		if(xAxisSnapf != null){
			
			Boolean isUniform = 
				TransformUtils.isUniformScale(
						entity, TARGET_ADJUSTMENT_AXIS.XAXIS);

	        Boolean isIncreasingScale =
	        	TransformUtils.isScaleIncreasing(
	        			newScale,
	        			startScale,
	        			TARGET_ADJUSTMENT_AXIS.XAXIS);

	        Boolean isPositiveDirection =
	        	TransformUtils.isScaleInPositiveDirection(
	        			newPosition,
	        			startPosition,
	        			TARGET_ADJUSTMENT_AXIS.XAXIS);

			float scale = size[0] * newScale[0];
			int index = (int) ((scale + xAxisSnapf/2.0f) / xAxisSnapf);
			newScale[0] = (xAxisSnapf * index)/size[0];

			double startSize = size[0] * startScale[0];
			double endSize = size[0] * newScale[0];

			double offset = (endSize - startSize)/2.0;

			// If it is uniform, don't adjust the position.
	        // Otherwise, Look for positive edge scale case first.
	        // Else case is the negative edge scale case.
			if (isUniform != null && isUniform == true) {
				;
			} else if(isIncreasingScale == null || 
					isPositiveDirection == null) {
			    newPosition[0] = startPosition[0] - offset;
			} else if((isPositiveDirection && isIncreasingScale) ||
	                (!isPositiveDirection && !isIncreasingScale)) {
	        	newPosition[0] = startPosition[0] + offset;
	        } else {
	        	newPosition[0] = startPosition[0] - offset;
	        }
		}

		if(yAxisSnapf != null){
			
			Boolean isUniform = 
				TransformUtils.isUniformScale(
						entity, TARGET_ADJUSTMENT_AXIS.YAXIS);

	        Boolean isIncreasingScale =
	        	TransformUtils.isScaleIncreasing(
	        			newScale,
	        			startScale,
	        			TARGET_ADJUSTMENT_AXIS.YAXIS);

	        Boolean isPositiveDirection =
	        	TransformUtils.isScaleInPositiveDirection(
	        			newPosition,
	        			startPosition,
	        			TARGET_ADJUSTMENT_AXIS.YAXIS);

			float scale = size[1] * newScale[1];
			int index = (int) ((scale + yAxisSnapf/2.0f) / yAxisSnapf);
			newScale[1] = (yAxisSnapf * index)/size[1];

			double startSize = size[1] * startScale[1];
			double endSize = size[1] * newScale[1];

			double offset = (endSize - startSize)/2.0;

			// If it is uniform, don't adjust the position.
	        // Otherwise, look for positive edge scale case first.
	        // Else case is the negative edge scale case.
			if (isUniform != null && isUniform == true) {
				;
			} else if(isIncreasingScale == null || 
					isPositiveDirection == null) {
                newPosition[1] = startPosition[1] - offset;
            } else if((isPositiveDirection && isIncreasingScale) ||
	                (!isPositiveDirection && !isIncreasingScale)) {
				newPosition[1] = startPosition[1] + offset;
			} else {
				newPosition[1] = startPosition[1] - offset;
			}
		}

		if(zAxisSnapf != null){
			
			Boolean isUniform = 
				TransformUtils.isUniformScale(
						entity, TARGET_ADJUSTMENT_AXIS.ZAXIS);

	        Boolean isIncreasingScale =
	        	TransformUtils.isScaleIncreasing(
	        			newScale,
	        			startScale,
	        			TARGET_ADJUSTMENT_AXIS.ZAXIS);

	        Boolean isPositiveDirection =
	        	TransformUtils.isScaleInPositiveDirection(
	        			newPosition,
	        			startPosition,
	        			TARGET_ADJUSTMENT_AXIS.ZAXIS);

			float scale = size[2] * newScale[2];
			int index = (int) ((scale + zAxisSnapf/2.0f) / zAxisSnapf);
			newScale[2] = (zAxisSnapf * index)/size[2];

			double startSize = size[2] * startScale[2];
			double endSize = size[2] * newScale[2];

			double offset = (endSize - startSize)/2.0;

			// If it is uniform, don't adjust position.
	        // Otherwise, look for positive edge scale case first.
	        // Else case is the negative edge scale case.
			if (isUniform != null && isUniform == true) {
				;
			} else if(isIncreasingScale == null || 
					isPositiveDirection == null) {
	             newPosition[2] = startPosition[2] - offset;
	        } else if((isPositiveDirection && isIncreasingScale) ||
	                (!isPositiveDirection && !isIncreasingScale)) {
				newPosition[2] = startPosition[2] + offset;
			} else {
				newPosition[2] = startPosition[2] - offset;
			}
		}
	}
}
