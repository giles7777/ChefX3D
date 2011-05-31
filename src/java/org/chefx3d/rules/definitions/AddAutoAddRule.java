/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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

import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.rules.util.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.tool.EntityBuilder;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Auto add children to product. Performs all auto add operations for add 
 * operations associated with the product.
 *
 * @author Ben Yarger
 * @version $Revision: 1.77 $
 */
public class AddAutoAddRule extends BaseRule {

    /** Cannot add because of auto add collision message */
    private static final String POP_UP_NO_ADD =
        "org.chefx3d.rules.definitions.AddAutoAddRule.addCanceled";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public AddAutoAddRule(
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
        
        // Don't perform auto add on miter entities
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (canMiterCut) {
        	result.setResult(true);
            return(result);
        }
        
        // Only operate on PositionableEntities
        if (!(entity instanceof PositionableEntity)) {
        	result.setResult(true);
            return(result);
        }
        
        // Perform command validation
        if (!(command instanceof AddEntityChildCommand) &&
        		!(command instanceof AddEntityChildTransientCommand) &&
        		!(command instanceof AddEntityCommand)){
        	
        	result.setResult(true);
            return(result);
        }
        
        // Assembly necessary processing components
        EntityBuilder entityBuilder = view.getEntityBuilder();
        Entity parentEntityParentEntity = null;
        double[] parentEntityPos = null;

        if(command instanceof AddEntityChildCommand){
            parentEntityParentEntity = ((AddEntityChildCommand)command).getParentEntity();
            
            parentEntityPos = 
            	TransformUtils.getExactPositionRelativeToZone(model, entity);

        }
        
        //----------------------------------------------------------------------
        // Do the magic!!
        //----------------------------------------------------------------------
        
        // Add the surrogate to evaluate against
        SceneManagementUtility.addTempSurrogate(
           		collisionChecker, command);
        
        if (!AutoAddBySpanUtility.autoAddBySpan(
        		model, 
        		(PositionableEntity) entity, 
        		parentEntityParentEntity, 
        		rch,
        		entityBuilder)) {
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
            
            // Clear out the surrogate added for the purpose of these checks
            SceneManagementUtility.removeTempSurrogate(
               		collisionChecker, (PositionableEntity) entity);
        	
        	result.setResult(false);
        	result.setApproved(false);
        	return(result);
        }
        
        if (!AutoAddByCollisionUtility.autoAddByCollision(
        		model, 
        		command, 
        		(PositionableEntity) entity, 
        		parentEntityParentEntity, 
        		parentEntityPos, 
        		rch, 
        		entityBuilder)) {
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
            
            // Clear out the surrogate added for the purpose of these checks
            SceneManagementUtility.removeTempSurrogate(
               		collisionChecker, (PositionableEntity) entity);
        	
        	result.setResult(false);
        	result.setApproved(false);
        	return(result);
        }
        
        if (!AutoAddByPositionUtility.autoAddByPosition(
        		model, 
        		(PositionableEntity) entity, 
        		parentEntityParentEntity, 
        		rch,
        		entityBuilder)) {
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
        	
            // Clear out the surrogate added for the purpose of these checks
            SceneManagementUtility.removeTempSurrogate(
               		collisionChecker, (PositionableEntity) entity);
            
        	result.setResult(false);
        	result.setApproved(false);
        	return(result);
        }
        
        if (!AutoAddEndsUtility.addAutoAddEnds(
        		model, 
        		(PositionableEntity) entity, 
        		parentEntityParentEntity, 
        		rch, 
        		entityBuilder)) {
        	
        	// Clear out the surrogate added for the purpose of these checks
        	SceneManagementUtility.removeTempSurrogate(
               		collisionChecker, (PositionableEntity) entity);
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
        	
        	result.setResult(false);
        	result.setApproved(false);
        	return(result);
        }
        
        AutoAddInvisibleChildrenUtility.addInvisibleChildren(
        		model, 
        		(PositionableEntity) entity, 
        		entityBuilder, 
        		rch);
        
        // Clear out the surrogate added for the purpose of these checks
        SceneManagementUtility.removeTempSurrogate(
           		collisionChecker, (PositionableEntity) entity);

        result.setResult(true);
        return(result);
    }

}
