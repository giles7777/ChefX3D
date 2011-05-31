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
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.RuleUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* If the entity uses absolute snaps, place it on a snap location
* closest to where the user clicked.
*
* @author Ben Yarger
* @version $Revision: 1.18 $
*/
public class AddAbsoluteSnapCheckRule extends BaseRule {

    /**
     * Constructor
     * 
     * @param errorReporter
     * @param model
     * @param view
     */
	public AddAbsoluteSnapCheckRule(
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
	 * Perform the check
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

		// Check if entity is using absolute snaps, return false if not
		Boolean usesAbsoluteSnaps = (Boolean) 
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_USES_ABSOLUTE_SNAPS_PROP);

		if(usesAbsoluteSnaps == null ||
				usesAbsoluteSnaps.booleanValue() == false){
		    result.setResult(false);
			return(result);
		}

		// Get the absolute snap values
		float[] xAxisSnaps = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_X_AXIS_SNAP_PROP);

		float[] yAxisSnaps = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Y_AXIS_SNAP_PROP);

		float[] zAxisSnaps = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Z_AXIS_SNAP_PROP);

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		double[] position = new double[] {0.0, 0.0, 0.0};

		if(entity instanceof PositionableEntity){

			((PositionableEntity)entity).getPosition(position);
		} else {
		    result.setResult(false);
            return(result);
		}

		applyAbsoluteSnaps(position, xAxisSnaps, yAxisSnaps, zAxisSnaps);

		((PositionableEntity)entity).setPosition(position, false);

		position = null;

		result.setResult(true);
        return(result);
	}

	/**
	 * Process each position index for the snap value it should inherit.
	 *
	 * @param position Starting xyz double[]
	 * @param xAxisSnaps X axis snap values
	 * @param yAxisSnaps Y axis snap values
	 * @param zAxisSnaps Z axis snap values
	 */
	private void applyAbsoluteSnaps(
			double[] position,
			float[] xAxisSnaps,
			float[] yAxisSnaps,
			float[] zAxisSnaps){

		if(xAxisSnaps != null){
			position[0] = RuleUtils.findClosestValue(position[0], xAxisSnaps);
		}

		if(yAxisSnaps != null){
			position[1] = RuleUtils.findClosestValue(position[1], yAxisSnaps);
		}

		if(zAxisSnaps != null){
			position[2] = RuleUtils.findClosestValue(position[2], zAxisSnaps);
		}
	}

	/**
	 * Finds the closest snap value for the original value.
	 *
	 * @param originalValue Original axis value
	 * @param snaps Associated array of same axis snap values to compare with
	 * @return double replacement value for the original
	 */
/*	private double RuleUtils.findClosestValue(double originalValue, float[] snaps){

		Arrays.sort(snaps);
		double newValue = snaps[0];

		
		// start from the beginning of the ordered snap values and see at what
		// point the original value is less than the snap the snap.
		if(originalValue < snaps[0]){

			newValue = snaps[0];

		} else if(originalValue > snaps[snaps.length-1]){

			newValue = snaps[snaps.length-1];

		} else {

			for(int i = 1; i < snaps.length; i++){

				if(originalValue > snaps[i]){
					continue;
				} else {
					newValue = snaps[i];
					break;
				}
			}
		}

		return newValue;
	}
*/
}
