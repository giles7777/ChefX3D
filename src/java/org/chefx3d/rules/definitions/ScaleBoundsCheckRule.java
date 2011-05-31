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

import org.chefx3d.rules.util.BoundsUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* Makes sure entities respect the bounds of entity parents when changing
* scale size.
*
* @author Ben Yarger
* @version $Revision: 1.19 $
*/
public class ScaleBoundsCheckRule extends BaseRule  {

	/** Pop up message when illegal bounds exists for place */
	private static final String BOUNDS_PROP =
		"org.chefx3d.rules.definitions.ScaleBoundsCheckRule.outsideBounds";

	/** Status message when illegal bounds exist */
	private static final String TRANS_BOUNDS_PROP =
		"org.chefx3d.rules.definitions.ScaleBoundsCheckRule.outsideBoundsTrans";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleBoundsCheckRule(
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

		Boolean scaleBoundToParent = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_BOUND_BY_PARENT_PROP);

		if(scaleBoundToParent != null && scaleBoundToParent == true){

			Entity parentEntity = model.getEntity(entity.getParentEntityID());


			if(!(command.isTransient())) {
//System.out.println("STOP");
			}

			if(!performScaleBoundsCheck(model, entity, parentEntity)){

				if(command.isTransient()) {

					String msg = intl_mgr.getString(TRANS_BOUNDS_PROP);
					statusBar.setMessage(msg);

				} else {

					double[] startPos = new double[3];
					float[] startScale = new float[3];
					((ScaleEntityCommand)command).getOldPosition(startPos);
					((ScaleEntityCommand)command).getOldScale(startScale);
					((ScaleEntityCommand)command).setNewPosition(startPos);
					((ScaleEntityCommand)command).setNewScale(startScale);
// Use Rules's wall bounds check pop up instead for the time being.
//					String msg = intl_mgr.getString(BOUNDS_PROP);
//					popUpMessage.showMessage(msg);


				}

		        result.setResult(false);
		        return(result);
			}
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Checks the child object bounds against the parent bounds to see
	 * if it is within the bounds of the parent. Returns true if it is
	 * within bounds false otherwise. Does not check against SegmentEntity.
	 *
	 * @param model WorldModel
	 * @param childEntity Entity
	 * @param parentEntity Entity
	 * @return True if in bounds, false otherwise
	 */
	private boolean performScaleBoundsCheck(
			WorldModel model,
			Entity childEntity,
			Entity parentEntity){

		if(!(parentEntity instanceof SegmentEntity)){

			return BoundsUtils.performParentBoundsCheck(
					model, childEntity, parentEntity);

		}

		return true;
	}
}
