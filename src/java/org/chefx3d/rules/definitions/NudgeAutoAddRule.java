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
import java.util.ArrayList;
import java.util.List;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * If product being moved, scaled, rotated, added, or deleted then check the
 * collision list of any products that auto-add items.  Those auto-add parents
 * should be nudged to ensure the auto-add products are correctly placed.
 *
 * @author Russell Dodds
 * @version $Revision: 1.15 $
 */
public class NudgeAutoAddRule extends BaseRule {


	/**
	 * Constructor
	 *
	 * @param errorReporter Error reporter to use
	 * @param model Collision checker to use
	 * @param view AV3D view to reference
	 */
	public NudgeAutoAddRule (
			ErrorReporter errorReporter,
			WorldModel model,
			EditorView view) {

		super(errorReporter, model, view);

		ruleType = RULE_TYPE.STANDARD;

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
		
		// If the entity is an auto span product, don't do the nudge on 
		// collisions.
		Boolean autoSpan = (Boolean)
        	RulePropertyAccessor.getRulePropertyValue(
	            entity,
	            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

	    if(autoSpan != null && autoSpan == true){
	        result.setResult(true);
	        return(result);
	    }
	    
	    // check the entity is a miter entity, and don't do the nudge
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (canMiterCut) {
        	result.setResult(true);
	        return(result);
        }

		// check to make sure this is a command we should be processing
		boolean processCmd = validateCommand(command);
		
		if (processCmd) {	    
			
		    List<PositionableEntity> list = 
		    	getNudgeList(model, command, entity);
		    
		    PositionableEntity nudgeTarget = null;

            //look through the list of possible properties
            for (int i = 0; i < list.size(); i++) {
            	
            	nudgeTarget = list.get(i);
            	
            	// check the entity is a miter entity, and don't do the nudge
                canMiterCut = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            nudgeTarget,
                            ChefX3DRuleProperties.MITRE_CAN_CUT);
                
                if (canMiterCut) {
                	continue;
                }
            	
            	// If there is already a move, scale or add command for the entity
        	    // in the newly issued command list, then don't nudge it.
        	    ArrayList<Command> newlyIssuedCommands = (ArrayList<Command>)
        			CommandSequencer.getInstance().getNewlyIssuedCommandList();
        	
        		Entity checkEntity = null;
        		boolean alreadyInCmdList = false;
        		
        		for (Command cmd : newlyIssuedCommands) {
        			
        			if (cmd instanceof MoveEntityCommand ||
        					cmd instanceof ScaleEntityCommand ||
        					cmd instanceof AddEntityChildCommand) {
        				
        				checkEntity = ((RuleDataAccessor)cmd).getEntity();
        				
        				if (checkEntity == nudgeTarget) {
        					
        					alreadyInCmdList = true;
        					break;
        				}
        			}
        		}

        		if (!alreadyInCmdList) {
	        		// Nudge the nudgeTarget
	                SceneManagementUtility.nudgeEntity(
	                        model,
	                        collisionChecker,
	                        nudgeTarget,
	                        command.isTransient());
        		}
            }

		}

		result.setResult(true);
		return result;

	}

	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------

	/**
	 * Get the list of entities to nudge.  An entity should be nudged if
	 * it is possible for it to have auto-add children and is not the parent
	 * of the entity affected by the command.
	 *
	 * @param model WorldModel to reference
	 * @param command The command being validated
	 * @param entity The entity affected by command
	 * @return List<PositionableEntity> list of entities to nudge
	 */
	private List<PositionableEntity> getNudgeList(
			WorldModel model,
			Command command, 
			Entity entity) {

	    List<PositionableEntity> nudegList =
	        new ArrayList<PositionableEntity>();

	    // perform the collision check
	    rch.performCollisionCheck(command, true, false, false);
	    
	    Entity parentEntity = 
	    	SceneHierarchyUtility.getExactParent(model, entity);
	    
	    // Check if the entity performs scale model swaps
	    Boolean scaleChangeModelFlag =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);
	    
	    // Get the model swap target ids in the event that the entity does
	    // perform model swaps.
	    String[] modelSwapProductIDArray = (String[])
        	RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);

        // check for possible children of a orientation
        String[] orientationSwapProducts = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.ORIENTATION_SWAP_ID_LIST);

	    // get the list of collisions and process them
        if(rch.collisionEntities != null) {

            for (int i = 0; i < rch.collisionEntities.size(); i++) {

                PositionableEntity tmpEntity =
                    (PositionableEntity)rch.collisionEntities.get(i);
                
                // don't nudge its parent
                if (tmpEntity == parentEntity) {
                	continue;
                }
                
                // don't nudge its direct children
                if (tmpEntity.getParentEntityID() == entity.getEntityID()) {
                    continue;
                }

                // special case some deletes
                if (command instanceof RemoveEntityChildCommand) {
                                        
                    // don't nudge kit parts
                    if (tmpEntity.getKitEntityID() > 0) {
                        continue;
                    }

                }
                                 
                Boolean noModel = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            tmpEntity,
                            ChefX3DRuleProperties.NO_MODEL_PROP);

                // don't nudge entities with no models
                if (noModel) {
                    continue;
                }
    
                Boolean autoSpan = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            tmpEntity,
                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                // don't nudge auto-span entities, handled by the 
                // CheckAutoSpanNeighborsRule
                if (autoSpan) {
                    continue;
                }

                // Don't nudge entity that is a model swap target
                if (scaleChangeModelFlag && modelSwapProductIDArray != null) {
                    
                    String tmpEntityToolID = tmpEntity.getToolID();
                    boolean matchFound = false;
                    
                    for (int x = 0; x < modelSwapProductIDArray.length; x++) {
                        
                        if (modelSwapProductIDArray[x].equals(
                                tmpEntityToolID)) {
                            matchFound = true;
                            break;
                        }
                    }
                    
                    if (matchFound) {
                        continue;
                    }
                }
                
                // Don't nudge entity that is a model swap target
                if (orientationSwapProducts != null) {
                    
                    String tmpEntityToolID = tmpEntity.getToolID();
                    boolean matchFound = false;
                    
                    for (int x = 0; x < orientationSwapProducts.length; x++) {
                        
                        if (orientationSwapProducts[x].equals(
                                tmpEntityToolID)) {
                            matchFound = true;
                            break;
                        }
                    }
                    
                    if (matchFound) {
                        continue;
                    }
                }

                // Can have auto adds check
                boolean hasAutoAdds = 
                	RuleUtils.canHaveAutoAddChildren(tmpEntity);
                
                if (hasAutoAdds) {
                    nudegList.add(tmpEntity);
                }
            }
        }

	    return nudegList;

	}

	/**
	 * Check to ensure a valid command is being processed
	 *
	 * @param command The command being validated
	 * @return True if valid to continue, false otherwise
	 */
	private boolean validateCommand(Command command) {

	    boolean valid = false;

	    if (!command.isTransient() &&
	            (command instanceof AddEntityChildCommand ||
	            command instanceof RemoveEntityChildCommand ||
	            command instanceof MoveEntityCommand ||
	            command instanceof TransitionEntityChildCommand ||
	            command instanceof ScaleEntityCommand ||
	            command instanceof RotateEntityCommand)) {

	        valid = true;
	    }

	    return valid;

	}

}
