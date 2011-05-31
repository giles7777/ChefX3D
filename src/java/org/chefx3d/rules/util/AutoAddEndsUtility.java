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

package org.chefx3d.rules.util;

//External imports

//Internal imports
import java.util.ArrayList;

import org.chefx3d.model.BasePositionableEntity;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.ScaleEntityCommand;
import org.chefx3d.model.ScaleEntityTransientCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.AUTO_ADD_CONDITION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.END_OPTION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.ORIENTATION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.util.RuleCollisionHandler;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.rules.util.AutoAddResult.TRANSACTION_OR_ENTITY_ID;
import org.chefx3d.rules.util.AutoAddUtility.CONDITION_CHECK_RESULT;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;

/**
 * Utility methods for processing auto add end operations.
 *
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */
public abstract class AutoAddEndsUtility {
	
	/** 
	 * Tracks the result of the command used to create the positive end 
	 * command.
	 */
	private static AutoAddResult positiveEndTransactionID;;
	
	/**
	 * Tracks the result of the command used to create the negative end 
	 * command.
	 */
    private static AutoAddResult negativeEndTransactionID;
    
    /**
	 * Check if the entity performs auto add ends operations.
	 * 
	 * @param entity Entity to check
	 * @return True if performs auto add ends, false otherwise
	 */
	public static boolean performsAutoAddEnds(Entity entity) {
		
		// Check if we do auto add by span, if not just return true
		Boolean placeAutoAddEnds = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				entity, 
	   				ChefX3DRuleProperties.AUTO_ADD_ENDS_PLACEMENT_USE);
		
		return placeAutoAddEnds.booleanValue();
	}

	/**
     * Auto place end products for the parentEntity.
     *
     * @param model WorldModel
     * @param parentEntity Parent to add children to
     * @param parentEntityParentEntity Parent entity to parentEntity
     * @param rch RuleCollisionHandler to use
     * @param entityBuilder EntityBuilder to use to create new entities
     * @return True if successful, false otherwise
     */
    public static boolean addAutoAddEnds(
            WorldModel model,
            Entity parentEntity,
            Entity parentEntityParentEntity,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder){
    	
    	// Check if we need to auto add ends, if not just return true
    	Boolean placeAutoAddEnds = (Boolean)
    		RulePropertyAccessor.getRulePropertyValue(
    				parentEntity, 
    				ChefX3DRuleProperties.AUTO_ADD_ENDS_PLACEMENT_USE);
    	
    	if (!placeAutoAddEnds) {
    		return true;
    	}

        // NOTE: THE KEY TO THIS WORKING IS THE ORDERING
        // REQUIREMENT THAT ALL AUTO ADD DATA SETS
        // MATCH BY INDEX ORDER.
        String[] autoPlaceObjectsProp = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.AUTO_ADD_ENDS_PROP);

        Enum[] autoAddAxis = (Enum[])
            RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.AUTO_ADD_ENDS_AXIS);

        float[] autoAddNegOffset = (float[])
            RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.AUTO_ADD_ENDS_NEG_OFFSET);

        float[] autoAddPosOffset = (float[])
            RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.AUTO_ADD_ENDS_POS_OFFSET);

        Enum[] autoAddEndsOption = (Enum[])
        	RulePropertyAccessor.getRulePropertyValue(
        			parentEntity, 
        			ChefX3DRuleProperties.AUTO_ADD_ENDS_OPTION);

        // This one is not index matched
        AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
        	RulePropertyAccessor.getRulePropertyValue(
        			parentEntity,
        			ChefX3DRuleProperties.AUTO_ADD_ENDS_CONDITIONS);

        // defensive coding: check if we should break early
        // in case the required properties are not set
        if ((autoPlaceObjectsProp == null) ||
            (autoAddAxis == null)) {

            return false;
        }
        
        if (autoPlaceObjectsProp.length != autoAddAxis.length) {
        	
        	return false;
        }

        //
        // Generate a command for each auto placements
        //
        float autoAddPosOffsetValue = 0;
        float autoAddNegOffsetValue = 0;
        END_OPTION autoAddEndsOptionValue = (END_OPTION)
        	ChefX3DRuleProperties.getDefaultValue(
        			ChefX3DRuleProperties.AUTO_ADD_ENDS_OPTION);
        
        for(int i = 0; i < autoPlaceObjectsProp.length; i++){

            SimpleTool simpleTool =
                RuleUtils.getSimpleToolByName(autoPlaceObjectsProp[i]);
            
            if (autoAddPosOffset != null) {
            	autoAddPosOffsetValue = autoAddPosOffset[i];
            }
            
            if (autoAddNegOffset != null) {
            	autoAddNegOffsetValue = autoAddNegOffset[i];
            }
            
            if (autoAddEndsOption != null) {
            	autoAddEndsOptionValue = (END_OPTION) autoAddEndsOption[i];
            }

            boolean result = createEnds(
                (ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)autoAddAxis[i],
                autoAddPosOffsetValue,
                autoAddNegOffsetValue,
                autoAddEndsOptionValue,
                (BasePositionableEntity) parentEntity,
                parentEntityParentEntity,
                simpleTool,
                model,
                rch,
                entityBuilder);
            
            // Check against the condition set
			boolean endOfSet = false;
			
			if (i == (autoPlaceObjectsProp.length - 1)) {
				endOfSet = true;
			}
			
			ArrayList<AutoAddResult> conditionCheckResultList = 
				new ArrayList<AutoAddResult>(); 
			
			if (positiveEndTransactionID != null) {
				conditionCheckResultList.add(positiveEndTransactionID);
			}
			
			if (negativeEndTransactionID != null) {
				conditionCheckResultList.add(negativeEndTransactionID);
			}
			
			AutoAddResult[] conditionCheckResults = 
				new AutoAddResult[conditionCheckResultList.size()];
			
			conditionCheckResultList.toArray(conditionCheckResults);
			
			CONDITION_CHECK_RESULT conditionResult = 
				AutoAddUtility.evaluateAutoAddCondition(
						autoAddCondition, 
						result, 
						endOfSet, 
						conditionCheckResults);
			
			switch (conditionResult) {
			
			case PASSED:
				return true;
				
			case FAILED:
				return false;
				
			case CONTINUE:
				break;
			}
        }

        return true;
    }
    
    /**
	 * Perform auto add ends adjustment for movement commands.
	 * 
	 * @param model WorldModel to reference
	 * @param command Command being checked
	 * @param entity Entity to add entities to
	 * @param parentEntity Parent entity of entity
	 * @param parentEntityPos Entity's position, just like autoAddByCollision
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @return True if successful, false otherwise
	 */
	public static boolean moveAutoAdd(
			WorldModel model,
			Command command,
			PositionableEntity entity,
			PositionableEntity parentEntity,
			RuleCollisionHandler rch,
			EntityBuilder entityBuilder) {
		
		// For non transient movement, perform the standard add operation
		if (!command.isTransient()) {
			return addAutoAddEnds(
					model, 
					entity, 
					parentEntity, 
					rch, 
					entityBuilder);
		}
		
		return true;
	}
	
	/**
	 * Perform auto add by position adjustment for scale commands.
	 * 
	 * @param model WorldModel to reference
	 * @param command Command being checked
	 * @param entity Entity to add entities to
	 * @param parentEntity Parent entity of entity
	 * @param parentEntityPos Entity's position, just like autoAddByCollision
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @return True if successful, false otherwise
	 */
	public static boolean scaleAutoAdd(
			WorldModel model,
			Command command,
			PositionableEntity entity,
			PositionableEntity parentEntity,
			double[] parentEntityPos,
			RuleCollisionHandler rch,
			EntityBuilder entityBuilder) {
/*
		// Move all auto add by collision entities back the inverse of the
		// change of position caused by the scale.
		ArrayList<Entity> autoAddColChildren = 
			AutoAddUtility.getAutoAddChildren(entity);
		
		// Get rid of all non auto add by collisions
		for (int i = (autoAddColChildren.size() - 1); i >= 0; i--) {
			
			// Check if we need to auto add by collision, if not return true
			Boolean performAutoAddEnds = (Boolean)
				RulePropertyAccessor.getRulePropertyValue(
						autoAddColChildren.get(i), 
						ChefX3DRuleProperties.AUTO_ADD_ENDS_PLACEMENT_USE);
			   
			if (!performAutoAddEnds) {
				autoAddColChildren.remove(i);
			}
		}
		
		// Determine adjustment to make		
		double[] newPos = new double[3];
		double[] startPos = new double[3];
		
		float[] newScale = new float[3];
		float[] startScale = new float[3];
		
		if (command instanceof ScaleEntityCommand) {
		
			ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;
			
			scaleCmd.getNewPosition(newPos);
			scaleCmd.getOldPosition(startPos);
			scaleCmd.getNewScale(newScale);
			scaleCmd.getOldScale(startScale);
			
		} else if (command instanceof ScaleEntityTransientCommand) {
			
			ScaleEntityTransientCommand scaleCmd = 
				(ScaleEntityTransientCommand) command;
			
			scaleCmd.getPosition(newPos);
			entity.getStartingPosition(startPos);
			scaleCmd.getScale(newScale);
			entity.getStartingScale(startScale);
			
		} else {
			return false;
		}
		
		// Adjust each auto add entity
		for (Entity autoAddColChild : autoAddColChildren) {
			
			// Match the toolID to find the adjustment axis
			String toolID = 
				AutoAddUtility.getPrimaryAutoAddToolID(autoAddColChild, entity);
			
			String[] autoPlaceObjectsProp = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.AUTO_ADD_ENDS_PROP);

	        Enum[] autoAddAxis = (Enum[])
	            RulePropertyAccessor.getRulePropertyValue(
	                    parentEntity,
	                    ChefX3DRuleProperties.AUTO_ADD_ENDS_AXIS);
	        
	        TARGET_ADJUSTMENT_AXIS axis = null;
	        
	        for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
	        	
	        	if (autoPlaceObjectsProp[i].equals(toolID)) {
	        		axis = (TARGET_ADJUSTMENT_AXIS) autoAddAxis[i];
	        		break;
	        	}
	        }
	        
	        // We have a problem if there isn't an axis match, so bail
	        if (axis == null) {
	        	return false;
	        }
			
	        double[] posAdj = new double[3];
	        
			boolean isScaleIncreasing = 
				TransformUtils.isScaleIncreasing(
						newScale, startScale, axis);
			
			// Handle the following cases in the following order:
			// 1) Increasing scale
			// 3) Decreasing scale
			if (isScaleIncreasing) {
				
				switch (axis) {
				case XAXIS:
					posAdj[0] = Math.abs(newPos[0] - startPos[0]);
					break;
				case YAXIS:
					posAdj[1] = Math.abs(newPos[1] - startPos[1]);
					break;
				case ZAXIS:
					posAdj[2] = Math.abs(newPos[2] - startPos[2]);
					break;
				}
				
			} else if (!isScaleIncreasing) {
				
				switch (axis) {
				case XAXIS:
					posAdj[0] = -Math.abs(newPos[0] - startPos[0]);
					break;
				case YAXIS:
					posAdj[1] = -Math.abs(newPos[1] - startPos[1]);
					break;
				case ZAXIS:
					posAdj[2] = -Math.abs(newPos[2] - startPos[2]);
					break;
				}
				
			}
			
			// Adjust the auto add child
			double[] aaccPos = new double[3];
			((PositionableEntity)autoAddColChild).getStartingPosition(aaccPos);
			
			switch (axis) {
			case XAXIS:
				if ((aaccPos[0] - startPos[0]) < 0) {
					aaccPos[0] -= posAdj[0];
				} else {
					aaccPos[0] += posAdj[0];
				}
				break;
			case YAXIS:
				if ((aaccPos[1] - startPos[1]) < 0) {
					aaccPos[1] -= posAdj[1];
				} else {
					aaccPos[1] += posAdj[1];
				}
				break;
			case ZAXIS:
				if ((aaccPos[2] - startPos[2]) < 0) {
					aaccPos[2] -= posAdj[2];
				} else {
					aaccPos[2] += posAdj[2];
				}
				break;
			}
			
			SceneManagementUtility.moveEntity(
					model, 
					rch.getRuleCollisionChecker(), 
					entity, 
					null, 
					aaccPos, 
					command.isTransient(), 
					false);
		}
*/		
		// If the command is not transient, then do the adds.
		if (!command.isTransient()) {
			return addAutoAddEnds(
					model, 
					entity, 
					parentEntity, 
					rch, 
					entityBuilder);
		}
		
		return true;
	}
    
    /**
	 * Get the auto add by collision tool IDs for the parentEntity.
	 * 
	 * @param parentEntity Entity to get auto add by collision tool id's from
	 * @return String[] copy of ids, or null if not found
	 */
	public static String[] getAutoAddToolIDS(Entity parentEntity) {
		   
		// First check if the parentEntity performs auto add by collision
		Boolean performAutoAddByCollision = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_ENDS_PLACEMENT_USE);
		   
		if (!performAutoAddByCollision) {
			return null;
		}
		   
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_ENDS_PROP);
		
		if (autoPlaceObjectsProp == null) {
			return null;
		}
		
		String[] results = new String[autoPlaceObjectsProp.length]; 
		
		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
			results[i] = autoPlaceObjectsProp[i];
		}
		
		return results;
		
	}
	
	/**
	 * Check the validity of the auto add conditions if the entity were removed.
	 * 
	 * @param model WorldModel to reference
	 * @param entity Entity to check
	 * @param rch RuleCollisionHandler to use
	 * @return True if deletion is ok, false otherwise
	 */
	public static boolean checkDeletionValiditiy(
			WorldModel model, 
			Entity entity,
			RuleCollisionHandler rch) {
		
		Entity parentEntity = 
			SceneHierarchyUtility.getExactParent(model, entity);
		
		// Check if we need to auto add ends, if not return true
		Boolean performAutoAddByEnds = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_ENDS_PLACEMENT_USE);
		   
		if (!performAutoAddByEnds) {
			return true;
		}
		
		// Get associated auto add siblings
		ArrayList<Entity> siblings = 
			AutoAddUtility.getAllMatchingPrimaryAutoAddIDChildren(
					model, entity);
		
		// if siblings are null, then we will go ahead and allow the delete
		if (siblings == null) {
			return true;
		}
		
		// Make sure the conditions are met
		AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_ENDS_CONDITIONS); 
		
		boolean conditionResult = 
			AutoAddUtility.evaluateRemoveAutoAddCondition(
				autoAddCondition, siblings.size());
		
		if (!conditionResult) {
			return false;
		}
		
		return true;
	}
    
    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------
    
    /**
     * Create the ends - so that the means may be justified....
     *
     * @param axis SPAN_AXIS enum value
     * @param posOffset The offset along the positive axis
     * @param negOffset The offset along the negative axis
     * @param endOption Placement option for end products
     * @param parentEntity BasePositionableEntity parent to add children to
     * @param parentEntityParentEntity Parent entity to parentEntity
     * @param simpleTool SimpleTool to create children with
     * @param model WorldModel to apply commands
     * @return True if end(s) were created correctly, false if one of the ends
     * was not created
     */
    private static boolean createEnds(
            TARGET_ADJUSTMENT_AXIS axis,
            float posOffset,
            float negOffset,
            END_OPTION endOption,
            BasePositionableEntity parentEntity,
            Entity parentEntityParentEntity,
            SimpleTool simpleTool,
            WorldModel model,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder){

        float[] parentBounds = BoundsUtils.getBounds(parentEntity, true);

        // end positions
        double[] firstEndPos = new double[3];
        double[] secondEndPos = new double[3];

        switch(axis){
        case XAXIS:
            firstEndPos[0] = parentBounds[1] - posOffset;
            secondEndPos[0] = parentBounds[0] + negOffset;
            break;

        case YAXIS:
            firstEndPos[1] = parentBounds[3] - posOffset;
            secondEndPos[1] = parentBounds[2] + negOffset;
            break;

        case ZAXIS:
            firstEndPos[2] = parentBounds[5] - posOffset;
            secondEndPos[2] = parentBounds[4] + negOffset;
            break;
        }
        
        boolean positiveAddSuccess = true;
        boolean negativeAddSuccess = true;

        positiveEndTransactionID = null;
        negativeEndTransactionID = null;
        
        // Add first end (positive direction)
        if (endOption == END_OPTION.BOTH || endOption == END_OPTION.POSITIVE) {
        	
        	positiveEndTransactionID =
	        	AutoAddUtility.issueNewAutoAddChildCommand(
		            model,
		            parentEntity,
		            parentEntityParentEntity,
		            simpleTool,
		            firstEndPos,
		            new float[] {0.0f, 0.0f, 1.0f, 0.0f},
		            false,
		            axis,
		            ORIENTATION.POSITIVE,
		            rch,
		            entityBuilder);
	        
	        if (positiveEndTransactionID.getType() == 
	        	TRANSACTION_OR_ENTITY_ID.FAILURE) {
	        	positiveAddSuccess = false;
	        }
	
        }

        // Add second end (negative direction)
        if (endOption == END_OPTION.BOTH || endOption == END_OPTION.NEGATIVE) {
        	
        	negativeEndTransactionID = 
        		AutoAddUtility.issueNewAutoAddChildCommand(
		            model,
		            parentEntity,
		            parentEntityParentEntity,
		            simpleTool,
		            secondEndPos,
		            new float[] {0.0f, 0.0f, 1.0f, 0.0f},
		            false,
		            axis,
		            ORIENTATION.NEGATIVE,
		            rch,
		            entityBuilder);
	
	        if (negativeEndTransactionID.getType() == 
	        	TRANSACTION_OR_ENTITY_ID.FAILURE) {
	        	negativeAddSuccess = false;
	        }
        }

        if (positiveAddSuccess && negativeAddSuccess) {
        	return true;
        }
        
        return false;
    }

}
