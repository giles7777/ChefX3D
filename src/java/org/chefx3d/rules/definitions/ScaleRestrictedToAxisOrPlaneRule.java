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
 * Handles restrictions on the axis or plane of scaling. Will only permit
 * scaling on the axis or plane specified in properies.
 *
 * @author Ben Yarger
 * @version $Revision: 1.16 $
 */
public class ScaleRestrictedToAxisOrPlaneRule extends BaseRule {

	/** Status message when scale is restricted */
	private static final String SCALE_RESTRICTED_PROP =
		"org.chefx3d.rules.definitions.ScaleRestrictedToAxisOrPlaneRule.scaleRestricted";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleRestrictedToAxisOrPlaneRule(
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

		ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES scaleRestriction =
		(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SCALE_RESTRICTION_PROP);

		float[] newScale = new float[3];

		if(command.isTransient()){
			((ScaleEntityTransientCommand)command).getScale(newScale);
		} else {
			((ScaleEntityCommand)command).getNewScale(newScale);
		}

		float[] startingScale = new float[3];
		((PositionableEntity)entity).getStartingScale(startingScale);
		boolean requireStatusUpdate = false;

		String msg = intl_mgr.getString(SCALE_RESTRICTED_PROP);

		if(scaleRestriction != null){

			switch(scaleRestriction){

			case XAXIS:

				if(newScale[1] == startingScale[1] &&
						newScale[2] == startingScale[2]){

					;

				} else {
					newScale[1] = startingScale[1];
					newScale[2] = startingScale[2];
					requireStatusUpdate = true;

				}

				statusBar.setMessage(msg);

				break;

			case YAXIS:

				if(newScale[0] == startingScale[0] &&
						newScale[2] == startingScale[2]){

					;

				} else {

					newScale[0] = startingScale[0];
					newScale[2] = startingScale[2];
					requireStatusUpdate = true;

				}

				statusBar.setMessage(msg);

				break;

			case ZAXIS:

				if(newScale[0] == startingScale[0] &&
						newScale[1] == startingScale[1]){

					;

				} else {

					newScale[0] = startingScale[0];
					newScale[1] = startingScale[1];
					requireStatusUpdate = true;

				}

				statusBar.setMessage(msg);

				break;

			case XYPLANE:

				if(newScale[2] == startingScale[2]){

					;

				} else {

					newScale[2] = startingScale[2];

					requireStatusUpdate = true;

				}

				statusBar.setMessage(msg);

				break;

			case XZPLANE:

				if(newScale[1] == startingScale[1]){

					;

				} else {

					newScale[1] = startingScale[1];
					requireStatusUpdate = true;

				}

				statusBar.setMessage(msg);

				break;

			case YZPLANE:

				if(newScale[0] == startingScale[0]){

					;

				} else {

					newScale[0] = startingScale[0];
					requireStatusUpdate = true;

				}

				statusBar.setMessage(msg);

				break;

			case UNIFORM:
				float largestScaleFactor = 1.0f;


				largestScaleFactor = Math.max(
						(newScale[0]/startingScale[0]),
						(newScale[1]/startingScale[1]));

				largestScaleFactor = Math.max(
						largestScaleFactor,
						(newScale[2]/startingScale[2]));

				newScale[0] = startingScale[0] * largestScaleFactor;
				newScale[1] = startingScale[1] * largestScaleFactor;
				newScale[2] = startingScale[2] * largestScaleFactor;

				statusBar.setMessage(msg);

				break;

			case NONE:
			default:
				break;
			}

			if(command.isTransient()){
				((ScaleEntityTransientCommand)command).setScale(newScale);
			} else {
				((ScaleEntityCommand)command).setNewScale(newScale);

				requireStatusUpdate = false;
			}

	        result.setResult(requireStatusUpdate);
	        return(result);
		}

        result.setResult(true);
        return(result);
	}

}
