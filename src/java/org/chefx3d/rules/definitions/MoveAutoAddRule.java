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
import java.util.ArrayList;

import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.rules.util.AutoAddUtility;
import org.chefx3d.rules.util.SceneHierarchyUtility;

import org.chefx3d.rules.util.*;

import org.chefx3d.tool.EntityBuilder;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Auto add children to product depending on children rules. Results from
 * Move commands.
 *
 * @author Ben Yarger
 * @version $Revision: 1.60 $
 */
public class MoveAutoAddRule extends BaseRule {

    /** Cannot move because of auto add collision message */
    private static final String POP_UP_NO_MOVE =
        "org.chefx3d.rules.definitions.MoveAutoAddRule.moveCanceled";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MoveAutoAddRule(
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
        
        // Don't perform auto add on miter entities
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (canMiterCut) {
        	result.setResult(true);
            return(result);
        }

        // If the command is transient, remove unnecessary auto adds
        if (command.isTransient()) {
        	
        	Entity[] tmpRemovedChildren = 
        		AutoAddUtility.removeNonCriticalAutoAdds(
        				model, entity, rch, view);
        	
        	// Side pocket any removed children to supply to the final
        	// scale command.
        	if (tmpRemovedChildren.length > 0) {
        		
        		for (int i = 0; i < tmpRemovedChildren.length; i++) {
        			entity.addStartingChild(tmpRemovedChildren[i]);
        		}
        	}
        	
        	result.setResult(true);
            return(result);
            
        } else {
        	
        	// If we match the side pocketed scaling entity, attach any
        	// starting children to replace.
        	if (entity.hasStartingChildren()) {
        		
        		ArrayList<Entity> startChildren = new ArrayList<Entity>();
        		
        		Entity[] removedChildren = entity.getStartingChildren();
        		
        		for (int i = 0; i < removedChildren.length; i++) {
        			startChildren.add(removedChildren[i]);
        		}
        		
        		// Set the starting children
            	if (command instanceof MoveEntityCommand) {
            		
            		((MoveEntityCommand)command).setStartChildren(
            				startChildren);
            		
            	} else if (command instanceof TransitionEntityChildCommand) {
            		
            		((TransitionEntityChildCommand)command).setStartChildren(
            				startChildren);
            	}

        	}

        	entity.clearStartingChildren();
        }

        // Do the non transient replacements
        PositionableEntity parentEntity = (PositionableEntity)
        	SceneHierarchyUtility.getExactParent(model, entity);

        if (parentEntity == null) {
        	result.setResult(false);
            return(result);
        }

        EntityBuilder entityBuilder = view.getEntityBuilder();
        boolean autoAddResult = true;

        // Auto add by span
        autoAddResult = AutoAddBySpanUtility.moveAutoAdd(
        		model,
        		command,
        		(PositionableEntity) entity,
        		parentEntity,
        		rch,
        		entityBuilder);

        if (!autoAddResult) {

	        result.setStatusValue(ELEVATION_LEVEL.SEVERE);

			// Reset the command to start and display the message
			//((RuleDataAccessor)command).resetToStart();

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);

           	result.setNotApprovedAction(NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
            result.setApproved(false);
	        result.setResult(false);
	        return(result);
        }

        // Auto add by collision
        autoAddResult = AutoAddByCollisionUtility.moveAutoAdd(
        		model,
        		command,
        		(PositionableEntity) entity,
        		parentEntity,
        		null,
        		rch,
        		entityBuilder);

        if (!autoAddResult) {

	        result.setStatusValue(ELEVATION_LEVEL.SEVERE);

			// Reset the command to start and display the message
			//((RuleDataAccessor)command).resetToStart();

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);

           	result.setNotApprovedAction(NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
            result.setApproved(false);
	        result.setResult(false);
	        return(result);
        }

        // Auto add by position
        autoAddResult = AutoAddByPositionUtility.moveAutoAdd(
        		model,
        		command,
        		(PositionableEntity) entity,
        		parentEntity,
        		rch,
        		entityBuilder);

        if (!autoAddResult) {

	        result.setStatusValue(ELEVATION_LEVEL.SEVERE);

			// Reset the command to start and display the message
			//((RuleDataAccessor)command).resetToStart();

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);

           	result.setNotApprovedAction(NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
            result.setApproved(false);
	        result.setResult(false);
	        return(result);
        }

        // Auto add ends
        autoAddResult = AutoAddEndsUtility.moveAutoAdd(
        		model,
        		command,
        		(PositionableEntity) entity,
        		parentEntity,
        		rch,
        		entityBuilder);

        if (!autoAddResult) {

	        result.setStatusValue(ELEVATION_LEVEL.SEVERE);

			// Reset the command to start and display the message
			//((RuleDataAccessor)command).resetToStart();

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);

           	result.setNotApprovedAction(NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
            result.setApproved(false);
	        result.setResult(false);
	        return(result);
        }

        // Done return all ok
        result.setResult(true);
        return(result);
    }

}
