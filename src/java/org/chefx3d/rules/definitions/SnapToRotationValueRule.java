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
import java.util.Arrays;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if rotations should snap to specific rotational values.
 * Can retrieve the rotational snap based on input provided if rotational
 * snap to a specific set of values is enabled.
 *
 * @author Ben Yarger
 * @version $Revision: 1.20 $
 */
public class SnapToRotationValueRule extends BaseRule {

	/** Notification that rotation uses absolute snap */
	private static final String SNAP_ROTATE_PROP =
		"org.chefx3d.rules.definitions.RotationUsesAbsoluteSnapsRule.usingSnap";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SnapToRotationValueRule(
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

		float[] rotationValues = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SNAP_TO_ROTATION_ABSOLUTE_VALUE_PROP);

		// If not set or not of at least length 1 return
		if(rotationValues == null || rotationValues.length < 1){
	        result.setResult(false);
	        return(result);
		}

		// Set status bar message
		String usesAbsSnapMsg = intl_mgr.getString(SNAP_ROTATE_PROP);
		statusBar.setMessage(usesAbsSnapMsg);

		/*
		 *  Convert current rotation amount to one of the indexed values
		 *  and set it as the new radian value in the rotation command.
		 */
		float[] curRotation = new float[4];

		if(command.isTransient()){
			((RotateEntityTransientCommand)command).getCurrentRotation(curRotation);
		} else {
			((RotateEntityCommand)command).getCurrentRotation(curRotation);
		}

		float closestValue = findClosestValue(curRotation[3], rotationValues);

		if(command.isTransient()){

			((RotateEntityTransientCommand)command).setCurrentRotation(
					curRotation[0],
					curRotation[1],
					curRotation[2],
					closestValue);

		} else {

			((RotateEntityCommand)command).setCurrentRotation(
					curRotation[0],
					curRotation[1],
					curRotation[2],
					closestValue);
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Finds the closest snap value for the original value.
	 *
	 * @param originalValue Original axis value
	 * @param snaps Associated array of same axis snap values to compare with
	 * @return double replacement value for the original
	 */
	private float findClosestValue(float originalValue, float[] snaps){

		Arrays.sort(snaps);
		float newValue = snaps[0];

		/*
		 * start from the beginning of the ordered snap values and see at what
		 * point the original value is less than the snap the snap.
		 */
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
}
