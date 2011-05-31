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

import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Defines the Movement Restricted To Parent check. If the movement is restricted
 * to a specific parent, and the new parent is not in the parents specified by
 * the entity, then movement is not allowed.
 *
 * @author Alan Hudson
 * @version $Revision: 1.18 $
 */
public class MovementPermanentParentRule extends BaseRule  {

    private static final String ILLEGAL_REPARENT_PROP =
        "org.chefx3d.rules.definitions.MovementPermanentParentRule.illegalReParent";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementPermanentParentRule(
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

        Boolean permanent_parent = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
                ChefX3DRuleProperties.USES_PERMANENT_PARENT);

        if (permanent_parent == null || permanent_parent.booleanValue() == false) {
            result.setResult(true);
            return(result);
        }

        Boolean parent_set = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
                ChefX3DRuleProperties.PERMANENT_PARENT_SET);


        if (parent_set == null || parent_set.booleanValue() == false) {
            result.setResult(true);
            return(result);
        }

        Boolean is_shadow =
            (Boolean) entity.getProperty(
                    entity.getParamSheetName(),
                    Entity.SHADOW_ENTITY_FLAG);

        if (is_shadow != null && is_shadow) {
            result.setResult(true);
            return(result);
        }

        if(command instanceof MoveEntityCommand) {
            result.setResult(true);
            return(result);
        } else if (command instanceof TransitionEntityChildCommand){
            TransitionEntityChildCommand cmd = (TransitionEntityChildCommand) command;
            Entity start = cmd.getStartParentEntity();
            Entity end = cmd.getEndParentEntity();

            if (command.isTransient()) {
                if (start == end) {
                    result.setResult(true);
                    return(result);
                } else {
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    result.setResult(false);
                    return(result);
                }
            }

            if (start != end) {
                String illegalCol = intl_mgr.getString(ILLEGAL_REPARENT_PROP);
                popUpMessage.showMessage(illegalCol);
            }

            result.setResult(true);
            return(result);

        } else {
            // MoveEntityTransientCommand case.  Ignore
            result.setResult(true);
            return(result);
        }
    }
}
