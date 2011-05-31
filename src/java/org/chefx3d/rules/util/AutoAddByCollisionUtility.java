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

// External imports
import java.util.ArrayList;

//Internal imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.AUTO_ADD_CONDITION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.ORIENTATION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.util.*;
import org.chefx3d.rules.util.AutoAddUtility.CONDITION_CHECK_RESULT;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;

/**
 * Utility methods for processing auto add by collision operations.
 *
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */
public abstract class AutoAddByCollisionUtility {
	
	/** Tracks internal transaction ID's generated */
	private static AutoAddResult[] autoAddResults;
	
	/**
	 * Check if the entity performs auto add by collision operations.
	 * 
	 * @param entity Entity to check
	 * @return True if performs auto add by collision, false otherwise
	 */
	public static boolean performsAutoAddByCollision(Entity entity) {
		
		// Check if we do auto add by span, if not just return true
		Boolean placeAutoAddByCollision = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				entity, 
	   				ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);
		
		return placeAutoAddByCollision.booleanValue();
	}

	/**
	 * Perform auto add by collision
	 * @param model WorldModel to reference
	 * @param command Command being checked
	 * @param parentEntity Parent entity to add entities to
	 * @param parentEntityParentEntity Parent entity of parentEntity
	 * @param parentEntityPos Parent entity's position
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @return True if successful, false otherwise
	 */
	public static boolean autoAddByCollision(
			WorldModel model,
			Command command,
			PositionableEntity parentEntity,
			Entity parentEntityParentEntity,
			double[] parentEntityPos,
			RuleCollisionHandler rch,
			EntityBuilder entityBuilder){
	   
		// Check if we need to auto add by collision, if not return true
		Boolean performAutoAddByCollision = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);
		   
		if (!performAutoAddByCollision) {
			return true;
		}

		// NOTE: THE KEY TO THIS WORKING IS THE ORDERING
		// REQUIREMENT THAT ALL AUTO ADD DATA SETS
		// MATCH BY INDEX ORDER.
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_COL_PROP);
		
		Enum[] autoAddAxis = (Enum[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_AXIS);
		
		float[] autoAddStepSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_STEP_SIZE);

		int[] autoAddForceFit = (int[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_FORCE_FIT);
		
		// This next two not index matched
		Integer autoAddMinRequired = (Integer) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_MIN_REQUIRED);
		   
		AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_CONDITIONS);
		   
		// defensive coding: check if we should break early
		// in case the required properties are not set
		if ((autoPlaceObjectsProp == null) ||
				(autoAddAxis == null)) {
		
			return false;
		}
		
		float stepSize = 0;
   
		// Begin actual collision add process
		for (int i = 0; i < autoPlaceObjectsProp.length; i ++) {
	   
			SimpleTool simpleTool = 
				RuleUtils.getSimpleToolByName(autoPlaceObjectsProp[i]);
			
			if (simpleTool == null) {
				continue;
			}

			if (autoAddStepSize != null) {
				stepSize = autoAddStepSize[i];
			}
       
			ArrayList<Entity> validCollisions = 
				AutoAddUtility.getValidCollisions(
						model, 
						true, 
						(TARGET_ADJUSTMENT_AXIS) autoAddAxis[i], 
						stepSize, 
						simpleTool, 
						command, 
						null,
						rch);
       
			boolean result = true;
       
			autoAddResults = new AutoAddResult[validCollisions.size()];
			int counter = -1;

			if(validCollisions.size() > 0) {

				//
				// Auto place by collision
				//
				for( Entity e : validCollisions ){
	   
					counter++;

					// a null collision means it was invalid
					// If e is not a PositionableEntity that is also invalid
					if (e == null || !(e instanceof PositionableEntity)) {
						return false;
					}
   
					// Calculate the intersection point, relative to the auto
					// adding parent. Do this in scene coordinates then,
					// convert to local coordinates relative to the target
					// parent.
					double[] intersection =
						TransformUtils.getPositionInSceneCoordinates(
								model, (PositionableEntity) e, true);
					
					intersection = 
						TransformUtils.
						convertSceneCoordinatesToLocalCoordinates(
								model, intersection, parentEntity, true);
					
					// Bind to the axis adjustment specified
					switch((TARGET_ADJUSTMENT_AXIS)autoAddAxis[i]){
					case XAXIS:
						//intersection[0] -= parentEntityPos[0];
						intersection[1] = 0;
						intersection[2] = 0;
						break;
					case YAXIS:
						intersection[0] = 0;
						//intersection[1] -= parentEntityPos[1];
						intersection[2] = 0;
						break;
					}

					boolean forceFit = false;

					if (autoAddForceFit != null && autoAddForceFit[i] == 1) {
						forceFit = true;
					}

					autoAddResults[counter] =
						AutoAddUtility.issueNewAutoAddChildCommand(
								model,
								parentEntity,
								parentEntityParentEntity,
								simpleTool,
								intersection,
								new float[] {0.0f, 0.0f, 1.0f, 0.0f},
								forceFit,
								(TARGET_ADJUSTMENT_AXIS) autoAddAxis[i],
								ORIENTATION.DEFAULT,
								rch,
								entityBuilder);
				}
			}
			
			// Make sure we placed more than the minimum amount required
			int amountPlaced = 0;
			for (int x = 0; x < autoAddResults.length; x++) {
				if (autoAddResults[x].wasSuccessful()) {
					amountPlaced++;
				}
			}
			
			if (autoAddMinRequired.intValue() > amountPlaced) {
				return false;
			}
   
			// Check against the condition set
			boolean endOfSet = false;
			
			if (i == (autoPlaceObjectsProp.length - 1)) {
				endOfSet = true;
			}
			
			CONDITION_CHECK_RESULT conditionResult = 
				AutoAddUtility.evaluateAutoAddCondition(
						autoAddCondition, result, endOfSet, autoAddResults);
			
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
	 * Perform auto add by collision adjustment for movement commands.
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
			double[] parentEntityPos,
			RuleCollisionHandler rch,
			EntityBuilder entityBuilder) {
		
		// For non transient movement, perform the standard add operation
		if (!command.isTransient()) {
			return autoAddByCollision(
					model, 
					command, 
					entity, 
					parentEntity, 
					parentEntityPos, 
					rch, 
					entityBuilder);
		}
		
		return true;
	}
	
	/**
	 * Perform auto add by collision adjustment for scale commands.
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
			Boolean performAutoAddByCollision = (Boolean)
				RulePropertyAccessor.getRulePropertyValue(
						autoAddColChildren.get(i), 
						ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);
			   
			if (!performAutoAddByCollision) {
				autoAddColChildren.remove(i);
			}
		}
		
		// Determine adjustment to make
		double[] posAdj = new double[3];
		double[] newPos = new double[3];
		double[] startPos = new double[3];
		
		if (command instanceof ScaleEntityCommand) {
		
			ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;
			
			scaleCmd.getNewPosition(newPos);
			scaleCmd.getOldPosition(startPos);
			
		} else if (command instanceof ScaleEntityTransientCommand) {
			
			ScaleEntityTransientCommand scaleCmd = 
				(ScaleEntityTransientCommand) command;
			
			scaleCmd.getPosition(newPos);
			entity.getStartingPosition(startPos);
			
		} else {
			return false;
		}
		
		// When calculating the adjustment, make sure we calculate it such
		// that we adjust back to the starting position.
		posAdj[0] = startPos[0] - newPos[0];
		posAdj[1] = startPos[1] - newPos[1];
		posAdj[2] = startPos[2] - newPos[2];
		
		// Adjust each entity
		for (Entity autoAddColChild : autoAddColChildren) {
			
			double[] aaccPos = new double[3];
			((PositionableEntity)autoAddColChild).getStartingPosition(aaccPos);
			
			aaccPos[0] += posAdj[0];
			aaccPos[1] += posAdj[1];
			aaccPos[2] += posAdj[2];
			
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
			return autoAddByCollision(
					model, 
					command, 
					entity, 
					parentEntity, 
					parentEntityPos, 
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
					ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);
		   
		if (!performAutoAddByCollision) {
			return null;
		}
		   
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_COL_PROP);
		
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
		
		// Check if we need to auto add by collision, if not return true
		Boolean performAutoAddByCollision = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);
		   
		if (!performAutoAddByCollision) {
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
					ChefX3DRuleProperties.AUTO_ADD_COL_MIN_REQUIRED);
		
		if (siblings.size() < autoAddMinRequired.intValue()) {
			return false;
		}
		
		// Make sure the conditions are met
		AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_CONDITIONS); 
		
		boolean conditionResult = 
			AutoAddUtility.evaluateRemoveAutoAddCondition(
				autoAddCondition, siblings.size());
		
		if (!conditionResult) {
			return false;
		}
		
		// Make sure span requirements are observed.
		// Start by getting the axis, span and add properties.
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_COL_PROP);
	
		Enum[] autoAddAxis = (Enum[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_COL_AXIS);
		
		float[] autoAddStepSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_COL_STEP_SIZE);
		
		if (autoPlaceObjectsProp == null || 
				autoAddAxis == null || 
				autoAddStepSize == null) {
			
			return true;
		}
		
		// Get the primary tool ID used to create the entity and find out
		// which index that occupies in the add props.
		String toolID = 
			AutoAddUtility.getPrimaryAutoAddToolID(entity, parentEntity);
		
		if (toolID == null) {
			return true;
		}
		
		int index = -1;
		
		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
			if (autoPlaceObjectsProp[i].equals(toolID)) {
				index = i;
				break;
			}
		}
		
		if (index == -1) {
			return true;
		}
		
		TARGET_ADJUSTMENT_AXIS axis = 
			(TARGET_ADJUSTMENT_AXIS) autoAddAxis[index];
		
		// Get the full set of matching auto added child entities and sort 
		// them.
		ArrayList<Entity> fullSet = new ArrayList<Entity>();
		fullSet.addAll(siblings);
		fullSet.add(entity);
		
		fullSet = 
			TransformUtils.sortDescendingPosValueOrder(
					siblings, 
					axis);
		
		int entityIndex = fullSet.indexOf(entity);
		
		// Check the span between the negative and positive neighbors.
		// If either positive or negative neighbor is missing then evaluate
		// overhang conditions.
		if (entityIndex < (fullSet.size() - 1) && entityIndex > 0) {
			
			double[] distance = 
				TransformUtils.getDistanceBetweenEntities(
					model, 
					(PositionableEntity) fullSet.get(entityIndex-1), 
					(PositionableEntity) fullSet.get(entityIndex+1), 
					true);
			
			switch (axis) {
			
			case XAXIS:
				if (distance[0] > autoAddStepSize[index]) {
					return false;
				}
				break;
			case YAXIS:
				if (distance[1] > autoAddStepSize[index]) {
					return false;
				}
				break;
			case ZAXIS:
				if (distance[2] > autoAddStepSize[index]) {
					return false;
				}
				break;
			}
		}
		
		// Evaluate overhang conditions
		double[] parentEntityPos = 
			TransformUtils.getExactPosition((PositionableEntity)parentEntity);
		
		MoveEntityCommand testCmd = 
			new MoveEntityCommand(
					model, 
					model.issueTransactionID(), 
					(PositionableEntity) parentEntity, 
					parentEntityPos, 
					parentEntityPos);
		
		boolean overhangResult = BoundsUtils.checkOverhangLimit(
				model, 
				testCmd, 
				parentEntity, 
				new int[] {entity.getEntityID()}, 
				rch);
		
		if (overhangResult) {
			return false;
		}
		
		return true;
	}
}
