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
import java.util.ArrayList;

//Internal imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.AUTO_ADD_CONDITION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.ORIENTATION;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.util.RuleCollisionHandler;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.AutoAddResult.TRANSACTION_OR_ENTITY_ID;
import org.chefx3d.rules.util.AutoAddUtility.CONDITION_CHECK_RESULT;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;

/**
 * Utility methods for processing auto add by position operations.
 *
 * @author Ben Yarger
 * @version $Revision: 1.2 $
 */
public abstract class AutoAddByPositionUtility {
	
	/**
	 * Check if the entity is an auto add by position child.
	 * 
	 * @param model WorldModel to reference
	 * @param entity Entity to check
	 * @return True if entity is an auto add position child of its parent, false 
	 * otherwise
	 */
	public static boolean isAutoAddByPositionChild(
			WorldModel model,
			Entity entity) {
		
		// Get and check for the existence of a parent entity
		Entity parentEntity = 
			SceneHierarchyUtility.getExactParent(model, entity);
		
		if (parentEntity == null) {
			return false;
		}
		
		// If the parent doesn't auto add by span then the child is not an
		// auto add by span.
		Boolean performAutoAddByPos = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity, 
				ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
		
		if (!performAutoAddByPos) {
			return false;
		}
		
		// If the child doesn't match any of the parent's auto add by span 
		// tool ID's then it is not an auto add by span.
		String[] autoPlaceObjectsProp = (String[])
        	RulePropertyAccessor.getRulePropertyValue(
                parentEntity,
                ChefX3DRuleProperties.AUTO_ADD_N_PROP);
		
		if (autoPlaceObjectsProp == null) {
			return false;
		}
		
		String toolID = 
			AutoAddUtility.getPrimaryAutoAddToolID(
				entity, parentEntity);
		
		if (toolID == null) {
			return false;
		}
		
		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
			
			if (toolID.equals(autoPlaceObjectsProp[i])) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Check if the entity performs auto add by position operations.
	 * 
	 * @param entity Entity to check
	 * @return True if performs auto add by position, false otherwise
	 */
	public static boolean performsAutoAddByPosition(Entity entity) {
		
		// Check if we do auto add by span, if not just return true
		Boolean placeAutoAddByPosition = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				entity, 
	   				ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
		
		return placeAutoAddByPosition.booleanValue();
	}

	/**
	 * Perform auto add by position.
	 * 
	 * @param model WorldModel to reference
	 * @param parentEntity Parent entity to auto add to
	 * @param parentEntityParentEntity Parent entity of ParentEntity
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @return True if successful, false otherwise
	 */
    public static boolean autoAddByPosition(
            WorldModel model,
            PositionableEntity parentEntity,
            Entity parentEntityParentEntity,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder){
    	
    	// Check if we do auto add by position, if not just return true
		Boolean placeAutoAddByPosition = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				parentEntity, 
	   				ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
	   	
	   	if (!placeAutoAddByPosition) {
	   		return true;
	   	}

	    // NOTE: THE KEY TO THIS WORKING IS THE ORDERING
		// REQUIREMENT THAT ALL AUTO ADD DATA SETS
		// MATCH BY INDEX ORDER.
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_PROP );
		   
		float[] autoAddXPositions = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_X_POS_PROP);
		
		float[] autoAddYPositions = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_Y_POS_PROP);
		
		float[] autoAddZPositions = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_Z_POS_PROP);
		
		// This next two not index matched
		Integer autoAddMinRequired = (Integer) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_MIN_REQUIRED);
		   
		AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_CONDITIONS);
		
		// defensive coding: check if we should break early
		// in case the required properties are not set
		if ((autoPlaceObjectsProp == null) ||
				(autoAddXPositions == null) ||
				(autoAddYPositions == null) ||
				(autoAddZPositions == null)) {
		
			return false;
		}
		   
		if ((autoPlaceObjectsProp.length != autoAddXPositions.length) &&
				(autoPlaceObjectsProp.length != autoAddYPositions.length) &&
				(autoPlaceObjectsProp.length != autoAddZPositions.length)) {
		
			return false;
		}
	   	
	   	// Begin auto add position process
		AutoAddResult autoAddResult;
		
		// Make sure we placed more than the minimum amount required
		int amountPlaced = 0;

		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
		
			autoAddResult = 
				createAutoPlacePositionProducts(
						model, 
						autoPlaceObjectsProp[i], 
						autoAddXPositions[i], 
						autoAddYPositions[i], 
						autoAddZPositions[i], 
						parentEntity, 
						parentEntityParentEntity, 
						rch, 
						entityBuilder);
			
			boolean result = true;
			
			if (autoAddResult.getType() == TRANSACTION_OR_ENTITY_ID.FAILURE) {
				result = false;
			} else {
				amountPlaced++;
			}
			
			// Check against the condition set
			boolean endOfSet = false;
			
			if (i == (autoPlaceObjectsProp.length - 1)) {
				endOfSet = true;
			}
			
			// If we haven't placed enough yet, don't bother checking the 
			// condition.
			if (autoAddMinRequired.intValue() > amountPlaced) {
				break;
			}
			
			CONDITION_CHECK_RESULT conditionResult = 
				AutoAddUtility.evaluateAutoAddCondition(
						autoAddCondition, 
						result, 
						endOfSet, 
						new AutoAddResult[] {autoAddResult});
			
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
	 * Perform auto add by position adjustment for movement commands.
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
		
		// Do nothing. Auto add by position is an add operation only.
		
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
		
		// Do nothing. Auto add by position is an add operation only.
		
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
					ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
		   
		if (!performAutoAddByCollision) {
			return null;
		}
		   
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_N_PROP);
		
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
		
		// Check if we need to auto add by position, if not return true
		Boolean performAutoAddByPosition = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
		   
		if (!performAutoAddByPosition) {
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
		
		// Make sure minimum requirement is observed
		Integer autoAddMinRequired = (Integer) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_MIN_REQUIRED);
		
		if (siblings.size() < autoAddMinRequired.intValue()) {
			return false;
		}
		
		// Make sure the conditions are met
		AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_N_CONDITIONS); 
		
		boolean conditionResult = 
			AutoAddUtility.evaluateRemoveAutoAddCondition(
				autoAddCondition, siblings.size());
		
		if (!conditionResult) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check if the child entity is an auto add by position child of the 
	 * parent entity.
	 * 
	 * @param model WorldModel to reference.
	 * @param child Child entity to evaluate.
	 * @param parent Parent entity to evaluate against.
	 * @return True if it is an auto add by position child of the parent,
	 * false otherwise.
	 */
	public static boolean isAutoAddChildOfParent(
			WorldModel model,
			Entity child, 
			Entity parent) {
		
		// If the parent isn't correct, then return false
		if (SceneHierarchyUtility.getExactParent(model, child) != parent) {
			return false;
		}
		
		// If the parent doesn't auto add by position then there is no match
		Boolean placeAutoAddByPosition = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				parent, 
	   				ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
	   	
	   	if (!placeAutoAddByPosition) {
	   		return false;
	   	}

	   	// Attempt to match the primary auto add id of the child to the 
	   	// auto add id's of the parent
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
					parent,
					ChefX3DRuleProperties.AUTO_ADD_N_PROP );
		
		if (autoPlaceObjectsProp == null) {
			return false;
		}
		
		String toolID = AutoAddUtility.getPrimaryAutoAddToolID(child, parent);
		
		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
			if (autoPlaceObjectsProp[i].equals(toolID)) {
				return true;
			}
		}
		
		return false;
	}
    
    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------
	
	/**
	 * Issue the add command for each auto add by position product specified.
	 * 
	 * @param model WorldModel to reference
	 * @param toolID ToolID to create
	 * @param xPosition X position to create entity at
	 * @param yPosition Y position to create entity at
	 * @param zPosition Z position to create entity at
	 * @param parentEntity ParentEntity to create new entity as child of
	 * @param parentEntityParentEntity Parent entity of parentEntity
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @return AutoAddResult
	 */
    private static AutoAddResult createAutoPlacePositionProducts(
    		WorldModel model,
    		String toolID,
    		float xPosition,
    		float yPosition,
    		float zPosition,
            PositionableEntity parentEntity,
            Entity parentEntityParentEntity,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder){


        SimpleTool simpleTool = RuleUtils.getSimpleToolByName(toolID);

        // safety check
        if (simpleTool == null) {

        	return (new AutoAddResult(0, TRANSACTION_OR_ENTITY_ID.FAILURE));
        }

        double[] childPos = new double[] {xPosition, yPosition, zPosition};
        
        AutoAddResult autoAddResult =
            AutoAddUtility.issueNewAutoAddChildCommand(
                    model,
                    parentEntity,
                    parentEntityParentEntity,
                    simpleTool,
                    childPos,
                    new float[] {0.0f, 0.0f, 1.0f, 0.0f},
                    false,
                    null,
                    ORIENTATION.DEFAULT,
                    rch,
                    entityBuilder);
        
        return autoAddResult;

    }
}
