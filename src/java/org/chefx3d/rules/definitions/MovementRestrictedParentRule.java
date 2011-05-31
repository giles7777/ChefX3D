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
import java.util.ArrayList;

import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.SceneHierarchyUtility;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Defines the Movement Restricted To Parent check. If the movement is restricted
 * to a specific parent, and the new parent is not in the parents specified by
 * the entity, then movement is not allowed.
 *
 * @author Alan Hudson
 * @version $Revision: 1.19 $
 */
public class MovementRestrictedParentRule extends BaseRule  {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementRestrictedParentRule(
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

        String[] allowed_parents = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

        if (allowed_parents == null || allowed_parents.length == 0) {
            result.setResult(true);
            return(result);
        }

        Entity parent = null;

        if(command instanceof MoveEntityCommand) {

            result.setResult(true);
            return(result);
            // No parent for this
            //parent = ((MoveEntityCommand)command).getParentEntity();
        } else if (command instanceof TransitionEntityChildCommand){
            parent = ((TransitionEntityChildCommand)command).getEndParentEntity();
        } else {
            Boolean permanent_parent = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.USES_PERMANENT_PARENT);

            Integer initalParentEntityID = (Integer)
               RulePropertyAccessor.getRulePropertyValue(
                       entity,
                       ChefX3DRuleProperties.INITAL_ADD_PARENT);

            if (permanent_parent && initalParentEntityID != null) {
                result.setResult(true);
                return(result);
            }
            
            parent = ((MoveEntityTransientCommand)command).getPickParentEntity();

        }

        if (parent == null) {
            result.setResult(true);
            return(result);
        }

        Boolean is_shadow =
            (Boolean) entity.getProperty(
                    entity.getParamSheetName(),
                    Entity.SHADOW_ENTITY_FLAG);

        String[] parent_classes = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                parent,
                ChefX3DRuleProperties.CLASSIFICATION_PROP);

        if (is_shadow != null && is_shadow == true) {
            result.setResult(true);
            return(result);
        } else if (parent_classes == null) {
            result.setStatusValue(ELEVATION_LEVEL.SEVERE);
            result.setApproved(false);
            result.setResult(false);
            return(result);
        }

        // Perform operations depending on if command is transient

        boolean match = false;

        for(int i=0; i < allowed_parents.length; i++) {
            for(int j=0; j < parent_classes.length; j++) {
                if (allowed_parents[i].equalsIgnoreCase(parent_classes[j])) {
                    match = true;
                    break;
                }

                if (match)
                    break;
            }
        }

        if (!match && !command.isTransient()) {
            result.setStatusValue(ELEVATION_LEVEL.SEVERE);
        }

        result.setResult(match);
        return(result);

    }
}
