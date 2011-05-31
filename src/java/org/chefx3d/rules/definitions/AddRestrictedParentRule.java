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

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 *
 * Only allows parents to be from the allowed parents(CX.allowedParentCl)
 * list.
 *
 * @author Alan Hudson
 * @version $Revision: 1.16 $
 */
public class AddRestrictedParentRule extends BaseRule  {

    /** Illegal collision conditions exist */
    private static final String ILLEGAL_PARENT_PROP =
        "org.chefx3d.rules.definitions.AddRestrictedParentRule.illegalParent";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public AddRestrictedParentRule(
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
     * @param model WorldModel object
     * @param command Command object
     * @param result The state of the rule processing
     * @return boolean True if rule passes, false otherwise
     */
    protected RuleEvaluationResult performCheck(
            Entity entity,
            Command command,
            RuleEvaluationResult result) {

        this.result = result;

        String[] allowed_parents = (String[]) entity.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

        if (allowed_parents == null || allowed_parents.length == 0) {
            result.setResult(true);
            return(result);
        }

        Entity parent = null;

        double[] child_pos = new double[3];

        if (command instanceof AddEntityChildCommand) {
            parent = ((AddEntityChildCommand)command).getParentEntity();
        } else {
            System.out.println("Unsupported type in AddRestParent: " + command);
            result.setResult(true);
            return(result);
        }


        if(parent == null){
            result.setResult(true);
            return(result);
        }


        String[] parent_classes =
            (String[]) parent.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.CLASSIFICATION_PROP);

        if (parent_classes == null || parent_classes.length == 0) {
            result.setResult(true);
            return(result);
        }

        boolean match = false;

        for(int i=0; i < allowed_parents.length; i++) {
            for(int j=0; j < parent_classes.length; j++) {
                if (allowed_parents[i].equalsIgnoreCase(parent_classes[j])) {
                    match = true;
                    break;
                }
            }

            if (match)
                break;
        }

        if (match) {
            result.setResult(true);
            return(result);
        } else {
            String illegalCol = intl_mgr.getString(ILLEGAL_PARENT_PROP);
            popUpMessage.showMessage(illegalCol);

            result.setApproved(false);
            result.setResult(false);
            return(result);
        }
    }
}
