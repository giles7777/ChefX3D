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
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Prevents object from being deleted if rule is set.
 *
 * @author Ben Yarger
 * @version $Revision: 1.12 $
 */
public class CanDeleteRule extends BaseRule  {

	/** Status message when rotation is restricted */
	private static final String POP_UP_MSG =
		"org.chefx3d.rules.definitions.CanDeleteRule.popUpMessage";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public CanDeleteRule(
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

		/*
		 * If remove command of an entity with CAN_DELETE_PROP set false don't
		 * allow delete operation.
		 */
		if(command instanceof RemoveEntityCommand ||
				command instanceof RemoveEntityChildCommand){


			Boolean canDelete = (Boolean)
				RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.CAN_DELETE_PROP);

            Boolean isShadow = (Boolean) entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    Entity.SHADOW_ENTITY_FLAG);
            if (isShadow == null) {
                isShadow = false;
            }

			if(!canDelete && !isShadow){

				String msg = intl_mgr.getString(POP_UP_MSG);
				popUpMessage.showMessage(msg);
				result.setApproved(false);
				result.setNotApprovedAction(
						NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
	            result.setResult(false);
	            return(result);
			}
		}

        result.setResult(true);
        return(result);
	}

}
