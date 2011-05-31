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

package org.chefx3d.rules.definitions;

//External Imports

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if scale uses absolute snaps to specific scale values.
 * If so, it updates the scale in the command for future checking.
 *
 * @author Ben Yarger
 * @version $Revision: 1.17 $
 */
public class ScaleUsesAbsoluteSnapsRule extends BaseRule  {

	/** Notification that scale uses absolute snap */
	private static final String SNAP_SCALE_PROP =
		"org.chefx3d.rules.definitions.ScaleUsesAbsoluteSnaps.statusMsg";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleUsesAbsoluteSnapsRule(
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

		//
		// Make sure entity is a PositionableEntity
		//
		if(!(entity instanceof PositionableEntity)){
	        result.setResult(true);
	        return(result);
		}

		// Check if entity is using absolute snaps, return false if not
		Boolean usesAbsoluteSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_USES_ABSOLUTE_SNAPS_PROP);

		if(usesAbsoluteSnaps == null ||
				usesAbsoluteSnaps.booleanValue() == false){

	        result.setResult(false);
	        return(result);
		}


        // Check if entity is an auto span and if so, don't adjust position
        Boolean ignoreOffsets = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);

		// Set status bar message
		String usesAbsSnapMsg = intl_mgr.getString(SNAP_SCALE_PROP);
		statusBar.setMessage(usesAbsSnapMsg);

		// Get the absolute snap values
		float[] xAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_ABSOLUTE_X_AXIS_SNAP_PROP);

		float[] yAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_ABSOLUTE_Y_AXIS_SNAP_PROP);

		float[] zAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_ABSOLUTE_Z_AXIS_SNAP_PROP);
		
		// Check for uniform scale case so we don't incorrectly adjust position
		if (xAxisSnaps != null) {
			Boolean isUniform = 
				TransformUtils.isUniformScale(
						(PositionableEntity)entity, 
						TARGET_ADJUSTMENT_AXIS.XAXIS);
			
			if (isUniform) {
				ignoreOffsets = true;
			}
		}
		
		if (yAxisSnaps != null) {
			Boolean isUniform = 
				TransformUtils.isUniformScale(
						(PositionableEntity)entity, 
						TARGET_ADJUSTMENT_AXIS.YAXIS);
			
			if (isUniform) {
				ignoreOffsets = true;
			}
		}
		
		if (zAxisSnaps != null) {
			Boolean isUniform = 
				TransformUtils.isUniformScale(
						(PositionableEntity)entity, 
						TARGET_ADJUSTMENT_AXIS.ZAXIS);
			
			if (isUniform) {
				ignoreOffsets = true;
			}
		}

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		float[] newScale = new float[3];
		double[] newPosition = new double[3];
		float[] originalScale = new float[3];
		double[] originalPosition = new double[3];
		float[] size = new float[3];

		((PositionableEntity)entity).getSize(size);

		// Perform operations depending on if command is transient
		if(command instanceof ScaleEntityCommand){

			((ScaleEntityCommand)command).getNewScale(newScale);
			((ScaleEntityCommand)command).getNewPosition(newPosition);
			((ScaleEntityCommand)command).getOldScale(originalScale);
			((ScaleEntityCommand)command).getOldPosition(originalPosition);

			applyAbsoluteSnaps(
					newPosition,
					newScale,
					originalPosition,
					originalScale,
					size,
					xAxisSnaps,
					yAxisSnaps,
					zAxisSnaps,
					ignoreOffsets);

			((ScaleEntityCommand)command).setNewScale(newScale);
			((ScaleEntityCommand)command).setNewPosition(newPosition);

		} else if (command instanceof ScaleEntityTransientCommand){

			((ScaleEntityTransientCommand)command).getScale(newScale);
			((ScaleEntityTransientCommand)command).getPosition(newPosition);

			((PositionableEntity)entity).getStartingPosition(originalPosition);
			((PositionableEntity)entity).getStartingScale(originalScale);

			applyAbsoluteSnaps(
					newPosition,
					newScale,
					originalPosition,
					originalScale,
					size,
					xAxisSnaps,
					yAxisSnaps,
					zAxisSnaps,
					ignoreOffsets);

			((ScaleEntityTransientCommand)command).setScale(newScale);
			((ScaleEntityTransientCommand)command).setPosition(newPosition);
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Process each scale index for the snap value it should inherit.
	 * Updates newPosition and newScale with values to use in accordance
	 * with snaps.
	 *
	 * @param newPosition New position component of scale adjustment
	 * @param newScale New scale adjustment
	 * @param startPosition Start position component of starting scale
	 * @param startScale Start scale
	 * @param size Size of the entity
	 * @param xAxisSnaps X axis snap values
	 * @param yAxisSnaps Y axis snap values
	 * @param zAxisSnaps Z axis snap values
	 * @param ignoreOffset true will skip updating the position
	 */
	private void applyAbsoluteSnaps(
			double[] newPosition,
			float[] newScale,
			double[] startPosition,
			float[] startScale,
			float[] size,
			float[] xAxisSnaps,
			float[] yAxisSnaps,
			float[] zAxisSnaps,
			boolean ignoreOffset){

		double offset, direction;

		if(xAxisSnaps != null){

			//
			// compare snapWidth to startWidth to determine the distance
			// the x-value of the position changes after the scale
			//
			double snapWidth =
				RuleUtils.findClosestValue((size[0] * newScale[0]), xAxisSnaps);
			double startWidth = size[0] * startScale[0];
            offset = Math.abs((snapWidth - startWidth)/2.0);

            if (!ignoreOffset) {
                // does scale move the x-pos in a positive or negative direction?
                direction = newPosition[0] - startPosition[0];

                if (direction < 0) {
                    newPosition[0] = startPosition[0] - offset;
                } else {
                    newPosition[0] = startPosition[0] + offset;
                }

            }

            newScale[0] = (float)snapWidth / size[0];

		}

		if(yAxisSnaps != null){

			//
			// compare snapHeight to startHeight to determine the distance
			// the y-value of the position changes after the scale
			//
			double snapHeight =
				RuleUtils.findClosestValue((size[1] * newScale[1]), yAxisSnaps);
			double startHeight = size[1] * startScale[1];
			offset = Math.abs((snapHeight - startHeight)/2.0);

            if (!ignoreOffset) {

    			// does scale move the y-pos in a positive or negative direction?
    			direction = newPosition[1] - startPosition[1];

                if(direction < 0){
                    newPosition[1] = startPosition[1] - offset;
                } else {
                    newPosition[1] = startPosition[1] + offset;
                }

            }

            newScale[1] = (float)snapHeight / size[1];

		}

		if(zAxisSnaps != null){

			//
			// compare snapDepth to startDepth to determine the distance
			// the z-value of the position changes after the scale
			//
			double snapDepth =
				RuleUtils.findClosestValue((size[2] * newScale[2]), zAxisSnaps);
			double startDepth = size[2] * startScale[2];
			offset = Math.abs((snapDepth - startDepth)/2.0);

	        if (!ignoreOffset) {

    			// does scale move the z-pos in a positive or negative direction?
    			direction = newPosition[2] - startPosition[2];

                if(direction < 0){
                    newPosition[2] = startPosition[2] - offset;
                } else {
                    newPosition[2] = startPosition[2] + offset;
                }

	        }

			newScale[2] = (float)snapDepth / size[2];
		}
	}
}
