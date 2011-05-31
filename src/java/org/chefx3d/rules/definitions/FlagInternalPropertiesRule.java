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

import org.chefx3d.rules.properties.*;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.util.AutoAddUtility;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Look at all add entity child commands and transition entity child commands
 * to see if the manual add of a product or transition of a product to see if it
 * needs any of the internal properties set.  This ensures loaded files are the
 * same.  Never put this rule on the ignore list.
 *
 * This is an internal rule flag, and should never be exposed to the user.
 *
 * @author Ben Yarger
 * @version $Revision: 1.13 $
 */
public class FlagInternalPropertiesRule extends BaseRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public FlagInternalPropertiesRule(
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

        boolean commandConfirmed = false;
        boolean setInitialParent = false;
        Entity parent = null;

        // Look at only the commands listed here as these are the only that
        // affect parenting.
        if(command instanceof AddEntityChildCommand){

            commandConfirmed = true;
            setInitialParent = true;
            parent = ((AddEntityChildCommand)command).getParentEntity();

        } else if (command instanceof AddEntityChildTransientCommand){

            commandConfirmed = true;
            setInitialParent = true;
            parent =
                ((AddEntityChildTransientCommand)command).getParentEntity();

        } else if (command instanceof TransitionEntityChildCommand){

            commandConfirmed = true;

            if (command.isTransient()) {

                parent =
                    ((TransitionEntityChildCommand)command).
                    getStartParentEntity();

            } else {

                parent =
                    ((TransitionEntityChildCommand)command).
                    getEndParentEntity();

            }
        }

        // See if the child is in the parent's auto add list.
        // If so set the internal reference flag
        if(commandConfirmed && parent != null){

            checkEntity(entity, parent, setInitialParent);

        }

        result.setResult(true);
        return(result);
    }

    /**
     * Check if the entity needs to have internal auto add properties set.
     *
     * @param entity Entity to examine
     * @param parent Parent of entity
     * @param setInitialParent True to set initial parent, false otherwise
     */
    private void checkEntity(
            Entity entity,
            Entity parent,
            boolean setInitialParent) {

        if(AutoAddUtility.isAutoAddChildOfParent(entity, parent)){

            // Set the is auto add flag to true
            entity.setProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT,
                    true,
                    false);
        } else {

            // Set the is auto add flag to false
            entity.setProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT,
                    false,
                    false);
        }

        if (setInitialParent) {

            // set the parent
            entity.setProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.INITAL_ADD_PARENT,
                    parent.getEntityID(),
                    false);

            // check to see if this is a permanent parent
            Boolean usePermanentParent =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.USES_PERMANENT_PARENT);

            if (usePermanentParent) {
                entity.setProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ChefX3DRuleProperties.PERMANENT_PARENT_SET,
                        true,
                        false);
            }
        }

        if (entity.hasChildren()) {

            int len = entity.getChildCount();
            for (int i = 0; i < len; i++) {

                Entity child = entity.getChildAt(i);
                checkEntity(child, entity, setInitialParent);

            }
        }
    }
}
