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

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Prevents scale values from being set to zero or negative.
 *
 * @author Ben Yarger
 * @version $Revision: 1.9 $
 */
public class PositiveScaleOnlyRule extends BaseRule  {

	/** Lowest legal scale value */
	private static final float MIN_SCALE_VALUE = 0.001f;

	/** This product cannot change size */
	private static final String ILLEGAL_SCALE_PROP =
		"org.chefx3d.rules.definitions.PositiveScaleOnlyRule.illegalScale";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public PositiveScaleOnlyRule(
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

		float[] newScale = new float[3];
		double[] newPos = new double[3];
		double[] startPos = new double[3];
		float[] size = new float[3];
		float[] startBounds = new float[6];

		if(command instanceof ScaleEntityCommand){

			((ScaleEntityCommand)command).getNewScale(newScale);
			((ScaleEntityCommand)command).getNewPosition(newPos);

		} else {

			((ScaleEntityTransientCommand)command).getScale(newScale);
			((ScaleEntityTransientCommand)command).getPosition(newPos);

		}

		/*
		 * Correct scale values
		 */
		boolean illegalScaleValue = false;
		boolean[] updatedFlag = new boolean[] {false, false, false};

		for(int i = 0; i < 3; i++) {

			if(newScale[i] < MIN_SCALE_VALUE){
				newScale[i] = MIN_SCALE_VALUE;
				updatedFlag[i] = true;
				illegalScaleValue = true;
			}
		}

		/*
		 * If scale values were corrected, fix positions
		 */
		if(illegalScaleValue){

			String notScalable = intl_mgr.getString(ILLEGAL_SCALE_PROP);
			statusBar.setMessage(notScalable);

			((PositionableEntity)entity).getStartingPosition(startPos);
			((PositionableEntity)entity).getSize(size);
			((PositionableEntity)entity).getStartingBounds(startBounds);

			double newWidthX = size[0] * newScale[0] / 2.0;
			double newHeightY = size[1] * newScale[1] / 2.0;
			double newDepthZ = size[2] * newScale[2] / 2.0;

			double[] updatedPos = new double[3];
			updatedPos[0] = startBounds[1] - newWidthX;
			updatedPos[1] = startBounds[3] - newHeightY;
			updatedPos[2] = startBounds[5] - newDepthZ;

			/*
			 * Correct position values for limit cases.
			 * Check for directional vector for limit cases. If no update
			 * should occur for positions, use the incoming value.
			 */
			for(int i = 0; i < 3; i++){

				if(updatedFlag[i]){
					if((newPos[i] - startPos[i]) < 0){
						updatedPos[i] = updatedPos[i] * -1 + startPos[i];
					} else {
						updatedPos[i] = updatedPos[i] + startPos[i];
					}
				} else {
					updatedPos[i] = newPos[i];
				}
			}

			/*
			 * Set the updated position and scale values.
			 */
			if(command instanceof ScaleEntityCommand){

				((ScaleEntityCommand)command).setNewScale(newScale);
				((ScaleEntityCommand)command).setNewPosition(updatedPos);

			} else {

				((ScaleEntityTransientCommand)command).setScale(newScale);
				((ScaleEntityTransientCommand)command).setPosition(updatedPos);

			}

            result.setResult(false);
            return(result);
		}

        result.setResult(true);
        return(result);
	}

}
