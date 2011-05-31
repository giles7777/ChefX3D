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

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * If product being selected is flagged as not editable then do not allow
 * the selection to occur
 *
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
public class IsEditableSelectionRule extends BaseRule {

	/**
	 * Constructor
	 *
	 * @param errorReporter Error reporter to use
	 * @param model Collision checker to use
	 * @param view AV3D view to reference
	 */
	public IsEditableSelectionRule (
			ErrorReporter errorReporter,
			WorldModel model,
			EditorView view) {

		super(errorReporter, model, view);

		ruleType = RULE_TYPE.INVIOLABLE;

	}

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

	@Override
	protected RuleEvaluationResult performCheck(
			Entity entity,
			Command command,
			RuleEvaluationResult result) {

		this.result = result;

		// check if the entity is a complex product	sub part
		Boolean isEditable =
		    (Boolean)RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.IS_EDITABLE_PROP);

		if (!isEditable && command instanceof SelectEntityCommand) {

		    // just change the current selection command to a false
		    ((SelectEntityCommand)command).setSelected(false);

		}

		result.setResult(true);
		return result;

	}

	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------

}
