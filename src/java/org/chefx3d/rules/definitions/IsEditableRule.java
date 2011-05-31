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
 * Determines if the entity is editable. If it is not editable, adjustments
 * are generally not allowed. There may be special cases where a check of
 * IsEditableRule is not required, but that is up to the specific
 * RuleEngine implementation to decide.
 *
 * @author Ben Yarger
 * @version $Revision: 1.19 $
 */
public class IsEditableRule extends BaseRule {

	/** This product is not editable */
	private static final String NOT_EDITABLE_PROP =
		"org.chefx3d.rules.definitions.IsEditableRule.notEditable";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public IsEditableRule(
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

		Boolean isEditable = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.IS_EDITABLE_PROP);

		if(isEditable == null || isEditable.booleanValue() == true){
            result.setResult(true);
            return(result);
		}

		String notEditable = intl_mgr.getString(NOT_EDITABLE_PROP);
		popUpMessage.showMessage(notEditable);
		statusBar.setMessage(notEditable);

        result.setResult(false);
        return(result);
	}
}
