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
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if the Entity can change its size, in effect can it change scale?
 * All changes in size are reflected as scalings on the geometry.
 *
 * @author Ben Yarger
 * @version $Revision: 1.19 $
 */
public class CanScaleRule extends BaseRule {

	/** This product cannot change size */
	private static final String NOT_SCALABLE_PROP =
		"org.chefx3d.rules.definitions.CanScaleRule.cannotScale";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public CanScaleRule(
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

		Boolean canScale = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
        		entity,
				ChefX3DRuleProperties.CAN_SCALE_PROP);

		if(canScale == null || canScale.booleanValue() == true){
	        result.setResult(true);
	        return(result);
		}

		String notScalable = intl_mgr.getString(NOT_SCALABLE_PROP);
		popUpMessage.showMessage(notScalable);
		statusBar.setMessage(notScalable);

        result.setResult(false);
        return(result);
	}
}
