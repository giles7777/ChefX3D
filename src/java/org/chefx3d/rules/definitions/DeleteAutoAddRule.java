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

import org.chefx3d.rules.util.*;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* Makes sure any delete operation on an auto placed object is only allowed
* if the integrity of the installation is preserved. Assumes there will always
* be at least two instances of each auto place items and will always be enough
* auto place items spaced according to the incremental step for a safe install.
*
* Also evaluates auto add product in collision with the product to be removed.
*
* @author Ben Yarger
* @version $Revision: 1.50 $
*/
public class DeleteAutoAddRule extends BaseRule {

	/** Status message when mouse button released and collisions exist */
	private static final String ILLEGAL_DEL =
		"org.chefx3d.rules.definitions.DeleteAutoAddRule.illegalDelete";

	/**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public DeleteAutoAddRule(
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

	    // Ignore shadow entity removals. Those have to be removed to place
        // the live entity.
        Boolean isShadow =
            (Boolean)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    Entity.SHADOW_ENTITY_FLAG);

        if (isShadow != null && isShadow) {
            result.setResult(true);
            return(result);
        }
        
        // If this entity is not an auto add, don't do this check. This check
        // only validates the removal of auto add products
        Boolean isAutoAddProduct = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
        
        if (!isAutoAddProduct) {
        	result.setResult(true);
            return(result);
        }

	    //------------------------------------------------------------
	    // Carry on with the evaluation of our entity being removed...
	    //------------------------------------------------------------

		// Obtain the parent entity
		Entity parentEntity = null;

		if (command instanceof RemoveEntityChildCommand){

			parentEntity =
				((RemoveEntityChildCommand)command).getParentEntity();

		} else {
			result.setResult(true);
            return(result);
		}

		// If the parent is null, don't allow delete to continue.
		// No way to evaluate without the parent and we have confirmed this
		// entity is an auto add entity
		if(parentEntity == null){
            result.setResult(true);
            return(result);
		}


		// Don't do this check if the ignore auto add delete flag is true
		Boolean ignoreDelCheck = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.IGNORE_AUTO_ADD_DEL_RESTRICTION);

		if (ignoreDelCheck) {
            result.setResult(true);
            return(result);
		}

		// Make sure that removal of the given child entity will leave
		// a safe installation behind for its auto adding parent.
		if (!autoAddRemoveCheck(model, entity)) {

			String msg = intl_mgr.getString(ILLEGAL_DEL);
			popUpMessage.showMessage(msg);

			result.setNotApprovedAction(NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
			result.setApproved(false);
            result.setResult(false);
            return(result);
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Perform auto add collision entity removal operation. If legal removal
	 * is not possible for any auto add collision entity, then the originating
	 * removal command passed in will not be allowed to execute either.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity to be removed by command
	 * @return True if successful, false otherwise
	 */
	private boolean autoAddRemoveCheck(
			WorldModel model,
			Entity entity){

		if (!AutoAddBySpanUtility.checkDeletionValiditiy(
        		model, entity, rch)) {
        	return false;
        }

        if (!AutoAddByCollisionUtility.checkDeletionValiditiy(
        		model, entity, rch)) {
        	return false;
        }

        if (!AutoAddByPositionUtility.checkDeletionValiditiy(
        		model, entity, rch)) {
        	return false;
        }

        if (!AutoAddEndsUtility.checkDeletionValiditiy(
        		model, entity, rch)) {
        	return false;
        }

        return true;

	}

}
