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

import org.chefx3d.view.common.EditorView;

/**
 * Determines if rotations should snap to specific rotational increment.
 * Can retrieve the rotational snap based on input provided if rotational
 * snap to an increment values is enabled.
 *
 * @author Ben Yarger
 * @version $Revision: 1.20 $
 */
public class SnapToRotationIncrementRule extends BaseRule {

	/** Notification that rotation uses incremental snap */
	private static final String SNAP_ROTATE_PROP =
		"org.chefx3d.rules.definitions.RotationUsesIncrementalSnapsRule.usingSnap";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SnapToRotationIncrementRule(
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

		Float rotationIncrement = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SNAP_TO_ROTATION_INCREMENT_PROP);

		// If not set or set to zero return
		if(rotationIncrement == null || rotationIncrement == 0.0f){
	        result.setResult(false);
	        return(result);
		}

		// Set status bar message
		String usesIncSnapMsg = intl_mgr.getString(SNAP_ROTATE_PROP);
		statusBar.setMessage(usesIncSnapMsg);

		/*
		 *  Convert current rotation amount to one of the indexed values
		 *  and set it as the new radian value in the rotation command.
		 */
		float[] curRotation = new float[] {0.0f, 0.0f, 0.0f, 0.0f};

		if(command.isTransient()){
			((RotateEntityTransientCommand)command).getCurrentRotation(curRotation);
		} else {
			((RotateEntityCommand)command).getCurrentRotation(curRotation);
		}

		float curRadRotation = curRotation[3];

		// Perform the step calculation with rounding
		int index = (int)(curRadRotation/rotationIncrement);
		double val = curRadRotation % rotationIncrement;
		if(Math.abs(val) > rotationIncrement/2.0){

			if(val < 0){
				index--;
			} else {
				index++;
			}
		}
		curRadRotation = rotationIncrement * index;

		if(command.isTransient()){
			((RotateEntityTransientCommand)command).setCurrentRotation(
					curRotation[0],
					curRotation[1],
					curRotation[2],
					curRadRotation);
		} else {
			((RotateEntityCommand)command).setCurrentRotation(
					curRotation[0],
					curRotation[1],
					curRotation[2],
					curRadRotation);
		}

        result.setResult(true);
        return(result);
	}
}
