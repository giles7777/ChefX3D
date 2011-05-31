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
import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 *
 * Add an entity which has a permanent parent.  This uses a list of
 * allowed parents(CX.permParentCl) and latches the first
 * choosen.
 *
 * @author Alan Hudson
 * @version $Revision: 1.15 $
*/
public class AddPermanentParentRule extends BaseRule  {

    private static final String ILLEGAL_REPARENT_PROP =
        "org.chefx3d.rules.definitions.AddPermanentParentRule.illegalReParent";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public AddPermanentParentRule(
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


        Entity parent = null;

        if (command instanceof AddEntityChildCommand) {

            parent = ((AddEntityChildCommand)command).getParentEntity();

            entity.setProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.PERMANENT_PARENT_SET,
                    true,
                    false);

        } else {
            // ignore the rest
            result.setResult(true);
            return(result);
        }

        if (parent_set == null || parent_set.booleanValue() == false) {
            result.setResult(true);
            return(result);
        }


        int parent_id = entity.getParentEntityID();

        if (parent_id < 0) {
            result.setResult(true);
            return(result);
        }
        Entity orig_parent = model.getEntity(entity.getParentEntityID());


        if (orig_parent != parent) {
            String illegalCol = intl_mgr.getString(ILLEGAL_REPARENT_PROP);
            popUpMessage.showMessage(illegalCol);

            result.setApproved(false);
            result.setResult(false);
            return(result);
        }

        result.setResult(true);
        return(result);
    }
}
