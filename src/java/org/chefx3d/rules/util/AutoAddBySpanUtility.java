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
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.util.*;
import org.chefx3d.rules.util.AutoAddResult.TRANSACTION_OR_ENTITY_ID;
import org.chefx3d.rules.util.AutoAddUtility.CONDITION_CHECK_RESULT;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;

/**
 * Utility methods for processing auto add by span operations.
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public abstract class AutoAddBySpanUtility {
	
	/** Tracks internal results generated */
	private static ArrayList<AutoAddResult> autoAddResults = 
		new ArrayList<AutoAddResult>();
	
	/**
	 * Check if the entity is an auto add by span child.
	 * 
	 * @param model WorldModel to reference
	 * @param entity Entity to check
	 * @return True if entity is an auto span child of its parent, false 
	 * otherwise
	 */
	public static boolean isAutoAddBySpanChild(
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
		Boolean performAutoAddBySpan = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity, 
				ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_USE);
		
		if (!performAutoAddBySpan) {
			return false;
		}
		
		// If the child doesn't match any of the parent's auto add by span 
		// tool ID's then it is not an auto add by span.
		String[] autoPlaceObjectsProp = (String[])
        	RulePropertyAccessor.getRulePropertyValue(
                parentEntity,
                ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_PROP );
		
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
	 * Check if the entity performs auto add by span operations.
	 * 
	 * @param entity Entity to check
	 * @return True if performs auto add by span, false otherwise
	 */
	public static boolean performsAutoAddBySpan(Entity entity) {
		
		// Check if we do auto add by span, if not just return true
		Boolean placeAutoAddBySpan = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				entity, 
	   				ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_USE);
		
		return placeAutoAddBySpan.booleanValue();
	}

	/**
	 * Perform the auto add by span operations.
	 * 
	 * @param model WorldModel to reference
	 * @param parentEntity Parent entity to parent to, also the entity affected
	 * by the command being evaluated.
	 * @param parentEntityParentEntity Parent entity of parentEntity
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @return True if successful, false otherwise
	 */
	public static boolean autoAddBySpan(
		   WorldModel model,
           PositionableEntity parentEntity,
           Entity parentEntityParentEntity,
           RuleCollisionHandler rch,
           EntityBuilder entityBuilder){

		autoAddResults.clear();
		
		// Check if we do auto add by span, if not just return true
		Boolean placeAutoAddBySpan = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				parentEntity, 
	   				ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_USE);
	   	
	   	if (!placeAutoAddBySpan) {
	   		return true;
	   	}

       // NOTE: THE KEY TO THIS WORKING IS THE ORDERING
       // REQUIREMENT THAT ALL AUTO ADD DATA SETS
       // MATCH BY INDEX ORDER.
       String[] autoPlaceObjectsProp = (String[])
           RulePropertyAccessor.getRulePropertyValue(
                   parentEntity,
                   ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_PROP );

       Enum[] autoAddAxis = (Enum[])
           RulePropertyAccessor.getRulePropertyValue(
                   parentEntity,
                   ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_AXIS);
       
       float[] autoAddStepSize = (float[])
       		RulePropertyAccessor.getRulePropertyValue(
	               parentEntity,
	               ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_STEP_SIZE);
       
       float[] autoAddNegOffset = (float[])
           RulePropertyAccessor.getRulePropertyValue(
                   parentEntity,
                   ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_NEG_OFFSET);

       float[] autoAddPosOffset = (float[])
           RulePropertyAccessor.getRulePropertyValue(
                   parentEntity,
                   ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_POS_OFFSET);

       int[] autoAddForceFit = (int[])
	       RulePropertyAccessor.getRulePropertyValue(
	               parentEntity,
	               ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_FORCE_FIT);

	   // This next two not index matched
	   Integer autoAddMinRequired = (Integer) 
	   		RulePropertyAccessor.getRulePropertyValue(
	   			parentEntity,
	   			ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_MIN_REQUIRED);
	   
	   AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
	   		RulePropertyAccessor.getRulePropertyValue(
	   			parentEntity,
	   			ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_CONDITIONS);

       // defensive coding: check if we should break early
       // in case the required properties are not set.
	   // The following three properties are required for auto add by span
	   // to execute.
       if ((autoPlaceObjectsProp == null) ||
           (autoAddAxis == null) ||
           (autoAddStepSize == null)) {

           return false;
       }
	   
	   // Begin auto add span process
       float negOffset = 0;
       float posOffset = 0;
       
       for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
       
    	   SimpleTool simpleTool = 
    		   RuleUtils.getSimpleToolByName(autoPlaceObjectsProp[i]);
    	   
    	   boolean forceFit = false;
    	   
    	   if (autoAddForceFit != null && autoAddForceFit[i] == 1) {
    		   forceFit = true;
    	   }
    	   
           //
           // "Normal" (non-collision) handling.
           //
           // Depending on the value of autoAddAxis[i], this will
           // perform either horizontal placement - adds products
           // right to left, vertical placement - add products top
           // to bottom, or depth placement - add products front to
           // back
           //
    	   
    	   // Establish the offsets is supplied
    	   if (autoAddNegOffset != null) {
    		   negOffset = autoAddNegOffset[i];
    	   }
    	   
    	   if (autoAddPosOffset != null) {
    		   posOffset = autoAddPosOffset[i];
    	   }
    	   
           createChildrenAlongSpan(
        		   model,
        		   simpleTool,
                   (BasePositionableEntity) parentEntity,
                   parentEntityParentEntity,
                   (TARGET_ADJUSTMENT_AXIS) autoAddAxis[i],
                   autoAddStepSize[i],
                   forceFit,
                   negOffset,
                   posOffset,
                   rch,
                   entityBuilder);
           
           boolean result = true;
           
           // Make sure we placed more than the minimum amount required
           int amountPlaced = 0;
           
           for (int j = 0; j < autoAddResults.size(); j++) {
        	   
        	   if (autoAddResults.get(j).getType() == 
        		   TRANSACTION_OR_ENTITY_ID.FAILURE) {
        		   
        		   result = false;
        	   } else {
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
			
			AutoAddResult[] autoAddResultArray = 
				new AutoAddResult[autoAddResults.size()];
			autoAddResults.toArray(autoAddResultArray);
			
			CONDITION_CHECK_RESULT conditionResult = 
				AutoAddUtility.evaluateAutoAddCondition(
						autoAddCondition, result, endOfSet, autoAddResultArray);
			
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
	 * Perform auto add by span adjustment for movement commands.
	 * 
	 * @param model WorldModel to reference
	 * @param command Command being checked
	 * @param entity Entity to add entities to
	 * @param parentEntity Parent entity of entity
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
			return autoAddBySpan(
					model, 
					entity, 
					parentEntity, 
					rch, 
					entityBuilder);
		}
		
		return true;
	}
	
	/**
	 * Perform auto add by span adjustment for scale commands.
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
			RuleCollisionHandler rch,
			EntityBuilder entityBuilder) {
/*		
		// Move all auto add by collision entities back the inverse of the
		// change of position caused by the scale.
		ArrayList<Entity> autoAddColChildren = 
			AutoAddUtility.getAutoAddChildren(entity);
		
		// Get rid of non auto add by span auto adds
		for (int i = (autoAddColChildren.size() - 1); i >= 0; i--) {

			if (!isAutoAddBySpanChild(model, autoAddColChildren.get(i))) {
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
		posAdj[0] = newPos[0] - startPos[0];
		posAdj[1] = newPos[1] - startPos[1];
		posAdj[2] = newPos[2] - startPos[2];
		
		// Adjust each entity
		for (Entity autoAddColChild : autoAddColChildren) {

			double[] aaccPos = new double[3];
			((PositionableEntity)autoAddColChild).getStartingPosition(aaccPos);
			
			aaccPos[0] -= posAdj[0];
			aaccPos[1] -= posAdj[1];
			aaccPos[2] -= posAdj[2];
			
			// We ignore rule evaluation for these cases since the adjustment to
	        // the position is strictly to keep the entities where they are.
	        // Also, this prevents stutter from position snaps.
			SceneManagementUtility.moveEntity(
					model, 
					rch.getRuleCollisionChecker(), 
					(PositionableEntity) autoAddColChild, 
					null, 
					aaccPos, 
					command.isTransient(), 
					true);
		}
*/		
		// If the command is not transient, then do the adds.
		if (!command.isTransient()) {
			return autoAddBySpan(
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
					ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_USE);
		   
		if (!performAutoAddByCollision) {
			return null;
		}
		   
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_PROP);
		
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
		
		// Check if we need to auto add by span, if not return true
		Boolean performAutoAddBySpan = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_USE);
		   
		if (!performAutoAddBySpan) {
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
					ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_MIN_REQUIRED);
		
		if (siblings.size() < autoAddMinRequired.intValue()) {

			return false;
		}
		
		// Make sure the conditions are met
		AUTO_ADD_CONDITION autoAddCondition = (AUTO_ADD_CONDITION) 
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity,
					ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_CONDITIONS); 
		
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
					ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_PROP);
	
		Enum[] autoAddAxis = (Enum[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_AXIS);
		
		float[] autoAddStepSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_STEP_SIZE);
		
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
   
   //---------------------------------------------------------------------------
   // Private methods
   //---------------------------------------------------------------------------
   
	/**
	 * Create the children along the entire span in segments as requrired by
	 * existing instances of the chosen tool.
	 * 
	 * @param model WorldModel to reference.
	 * @param simpleTool Tool to use when creating the entities.
	 * @param parentEntity Parent entity of new entity.
	 * @param parentEntityParentEntity Parent entity of parentEntity.
	 * @param axis The axis to add the entities along.
	 * @param stepSize The spacing between each entity, center-to-center.
	 * @param forceFit Flag to attempt to force fit the entity if it doesn't 
	 * fit via the standard add.
	 * @param negativeOffset Offset from the negative end of the span.
	 * @param positiveOffset Offset from the positive end of the span.
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 */
	private static void createChildrenAlongSpan(
			WorldModel model,
    		SimpleTool simpleTool, 
            BasePositionableEntity parentEntity,
            Entity parentEntityParentEntity,
            TARGET_ADJUSTMENT_AXIS axis,
            float stepSize,
            boolean forceFit,
            float negativeOffset,
            float positiveOffset,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder) {
		
		// Span segment edges
		double negativeEdgePosition = 0.0;
		double positiveEdgePosition = 0.0;
		
		// General outline of procedure:
		// Auto add by span always adds in a negative end to positive end 
		// direction. Any existing auto add by span products found will be
		// considered when adding products of the same primary auto add tool id 
		// around it.
		
		// Get all auto add children matching the tool ID we want to add 
		// entities with.
		ArrayList<Entity> autoAddMatches = 
			AutoAddUtility.getAllMatchingPrimaryAutoAddIDChildren(
					model, parentEntity, simpleTool.getToolID());
		
		if (autoAddMatches == null) {
			autoAddMatches = new ArrayList<Entity>();
		}
		
		// Update the auto add results with the existing auto adds
		for (Entity match : autoAddMatches) {
			
			autoAddResults.add(new AutoAddResult(
                    match.getEntityID(),
                    TRANSACTION_OR_ENTITY_ID.ENTITY));
		}
		
		// This will sort the entities positive to negative in terms of position
		autoAddMatches = 
			TransformUtils.sortDescendingPosValueOrder(autoAddMatches, axis);
		
		if (autoAddMatches == null) {
			autoAddMatches = new ArrayList<Entity>();
		}
		
		// Get the bounds so we can begin placing from the negative side.
		float[] parentBounds = BoundsUtils.getBounds(parentEntity, true);
		
		// Calculate the initial left edge position
		switch (axis) {
		
		case XAXIS:
			negativeEdgePosition = parentBounds[0]+negativeOffset;
			break;
			
		case YAXIS:
			negativeEdgePosition = parentBounds[2]+negativeOffset;
			break;
			
		case ZAXIS:
			negativeEdgePosition = parentBounds[4]+negativeOffset;
			break;
		
		case NONE:
			return;
		}
		
		// Figure out the right edge position. It is either going to be the
		// far right edge, or the position of the first autoAddMatch found.
		boolean doPositiveEdgeCase = true;
		boolean doNegativeEdgeCase = true;
		
		if (autoAddMatches.size() > 0) {
			
			PositionableEntity autoAddMatch =
				(PositionableEntity) autoAddMatches.get(
						(autoAddMatches.size() - 1));
			
			double[] currentMatchPos = 
				TransformUtils.getExactPosition(autoAddMatch);
					
			switch (axis) {
			
			case XAXIS:
				positiveEdgePosition = currentMatchPos[0];
				break;
				
			case YAXIS:
				positiveEdgePosition = currentMatchPos[1];
				break;
				
			case ZAXIS:
				positiveEdgePosition = currentMatchPos[2];
				break;

			case NONE:
				return;
			}
			
			// Here we want to evaluate the negative edge case in the event an
			// auto add is required between the left most existing auto add
			// and the end of the parent span. Therefore, we want to evaluate
			// the negative edge case, but not the positive.
			doPositiveEdgeCase = false;
			doNegativeEdgeCase = true;
				
			createChildrenAlongSegment(
					model, 
					simpleTool, 
					parentEntity, 
					parentEntityParentEntity, 
					axis, 
					stepSize, 
					forceFit, 
					negativeEdgePosition, 
					positiveEdgePosition, 
					rch, 
					entityBuilder,
					doPositiveEdgeCase,
					doNegativeEdgeCase);
			
		} else {

			// Calculate the final right edge position
			switch (axis) {
			
			case XAXIS:
				positiveEdgePosition = parentBounds[1]-positiveOffset;
				break;
				
			case YAXIS:
				positiveEdgePosition = parentBounds[3]-positiveOffset;
				break;
				
			case ZAXIS:
				positiveEdgePosition = parentBounds[5]-positiveOffset;
				break;

			case NONE:
				return;
			}
			
			// Here we want to evaluate both edge cases since no matching auto 
			// adds were identified as children.
			doPositiveEdgeCase = true;
			doNegativeEdgeCase = true;
			
			createChildrenAlongSegment(
					model, 
					simpleTool, 
					parentEntity, 
					parentEntityParentEntity, 
					axis, 
					stepSize, 
					forceFit, 
					negativeEdgePosition, 
					positiveEdgePosition, 
					rch, 
					entityBuilder,
					doPositiveEdgeCase,
					doNegativeEdgeCase);
			
			// We have filled the whole span, so exit now.
			return;
		}
		
		// Check all the middle spans
		double[] currentMatchPos = new double[3];
		double[] nextMatchPos = new double[3];
		
		for (int i = (autoAddMatches.size() - 1); i > 0; i--) {
			
			PositionableEntity currentMatch = 
				(PositionableEntity) autoAddMatches.get(i);
			PositionableEntity nextMatch = 
				(PositionableEntity) autoAddMatches.get(i-1);
			
			currentMatchPos = TransformUtils.getExactPosition(currentMatch);
			nextMatchPos = TransformUtils.getExactPosition(nextMatch);
			
			// Calculate the axis specific distance
			double distance = 0.0;
			
			switch (axis) {
			
			case XAXIS:
				distance = nextMatchPos[0] - currentMatchPos[0];
				negativeEdgePosition = currentMatchPos[0];
				positiveEdgePosition = nextMatchPos[0];
				break;
				
			case YAXIS:
				distance = nextMatchPos[1] - currentMatchPos[1];
				negativeEdgePosition = currentMatchPos[1];
				positiveEdgePosition = nextMatchPos[1];
				break;
				
			case ZAXIS:
				distance = nextMatchPos[2] - currentMatchPos[2];
				negativeEdgePosition = currentMatchPos[2];
				positiveEdgePosition = nextMatchPos[2];
				break;

			case NONE:
				return;
			}
			
			if (distance >= stepSize) {
				
				// We never want to do the positive or negative matches for
				// middle spanning cases.
				doPositiveEdgeCase = false;
				doNegativeEdgeCase = false;
				
				createChildrenAlongSegment(
						model, 
						simpleTool, 
						parentEntity, 
						parentEntityParentEntity, 
						axis, 
						stepSize, 
						forceFit, 
						negativeEdgePosition, 
						positiveEdgePosition, 
						rch, 
						entityBuilder,
						doPositiveEdgeCase,
						doNegativeEdgeCase);
			}

		}
		
		// Do the final right edge span
		currentMatchPos = TransformUtils.getExactPosition(
				(PositionableEntity) autoAddMatches.get(0));
				
		switch (axis) {
			
			case XAXIS:
				negativeEdgePosition = currentMatchPos[0];
				positiveEdgePosition = parentBounds[1]-positiveOffset;
				break;
				
			case YAXIS:
				negativeEdgePosition = currentMatchPos[1];
				positiveEdgePosition = parentBounds[3]-positiveOffset;
				break;
				
			case ZAXIS:
				negativeEdgePosition = currentMatchPos[2];
				positiveEdgePosition = parentBounds[5]-positiveOffset;
				break;

			case NONE:
				return;
		}

		// Here we want to evaluate the positive edge case in the event an
		// auto add is required between the right most existing auto add
		// and the end of the parent span. Therefore, we want to evaluate
		// the positive edge case, but not the negative.
		doPositiveEdgeCase = true;
		doNegativeEdgeCase = false;
		
		createChildrenAlongSegment(
				model, 
				simpleTool, 
				parentEntity, 
				parentEntityParentEntity, 
				axis, 
				stepSize, 
				forceFit, 
				negativeEdgePosition, 
				positiveEdgePosition, 
				rch, 
				entityBuilder,
				doPositiveEdgeCase,
				doNegativeEdgeCase);
		
	}
 
	/**
	 * Create the children along the specified axis for the specified segment
	 * that is a sub part of the span.
	 * 
	 * @param model WorldModel to reference.
	 * @param simpleTool Tool to use when creating the entities.
	 * @param parentEntity Parent entity of new entity.
	 * @param parentEntityParentEntity Parent entity of parentEntity.
	 * @param axis The axis to add the entities along.
	 * @param stepSize The spacing between each entity, center-to-center.
	 * @param forceFit Flag to attempt to force fit the entity if it doesn't 
	 * fit via the standard add.
	 * @param negativeEdge Negative span edge position relative to parentEntity.
	 * @param positiveEdge Positive span edge position relative to parentEntity.
	 * @param rch RuleCollisionHandler to use
	 * @param entityBuilder EntityBuilder to use
	 * @param doPositiveEdgeCase True to attempt to place the positive edge auto
	 *  add case. False to skip.
	 * @param doNegativeEdgeCase True to attempt to place the negative edge auto
	 *  add case. False to skip.
	 */
    private static void createChildrenAlongSegment(
    		WorldModel model,
    		SimpleTool simpleTool, 
            BasePositionableEntity parentEntity,
            Entity parentEntityParentEntity,
            TARGET_ADJUSTMENT_AXIS axis,
            float stepSize,
            boolean forceFit,
            double negativeEdge,
            double positiveEdge,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder,
            boolean doPositiveEdgeCase,
            boolean doNegativeEdgeCase){

        // Avoid divide by zero
        if(stepSize == 0.0){
            return;
        }

        double spanDistance = positiveEdge - negativeEdge;
        
        if (spanDistance == 0.0) {
        	return;
        }

        // Calculate number of entities to add along span
        int quantityToAdd = (int) (spanDistance / stepSize) + 1;

        // If there is a remainder, add an extra entity
        double remainder = (spanDistance % stepSize);
        if(remainder > 0.0){
            quantityToAdd++;
        }

        // Distance between end products added
        double distanceBetween = 0.0;

        // Calculate first child position
        double[] firstChildPos = new double[3];

        // if both offsets are the same and equal to the width of the parent 
        // object the children will swap places. Do the positive one first.
        switch(axis){
            case XAXIS:
                firstChildPos[0] = positiveEdge;
                firstChildPos[1] = firstChildPos[1];
                firstChildPos[2] = firstChildPos[2];
                break;
            case YAXIS:
                firstChildPos[0] = firstChildPos[0];
                firstChildPos[1] = positiveEdge;
                firstChildPos[2] = firstChildPos[2];
                break;
            case ZAXIS:
                firstChildPos[0] = firstChildPos[0];
                firstChildPos[1] = firstChildPos[1];
                firstChildPos[2] = positiveEdge;
                break;
        }

        // Calculate second child position, this is the negative one.
        double[] secondChildPos = new double[3];

        switch(axis){
            case XAXIS:
                secondChildPos[0] = negativeEdge;
                secondChildPos[1] = secondChildPos[1];
                secondChildPos[2] = secondChildPos[2];

                distanceBetween = firstChildPos[0] - secondChildPos[0];
                break;
            case YAXIS:
                secondChildPos[0] = secondChildPos[0];
                secondChildPos[1] = negativeEdge;
                secondChildPos[2] = secondChildPos[2];

                distanceBetween = firstChildPos[1] - secondChildPos[1];
                break;
            case ZAXIS:
                secondChildPos[0] = secondChildPos[0];
                secondChildPos[1] = secondChildPos[1];
                secondChildPos[2] = negativeEdge;

                distanceBetween = firstChildPos[2] - secondChildPos[2];
                break;
        }

        // Add first product
        if(quantityToAdd > 0 && doPositiveEdgeCase){

        	autoAddResults.add(
                AutoAddUtility.issueNewAutoAddChildCommand(
                        model,
                        parentEntity,
                        parentEntityParentEntity,
                        simpleTool,
                        firstChildPos,
                        new float[] {0.0f, 0.0f, 1.0f, 0.0f},
                        forceFit,
                        axis,
                        ORIENTATION.DEFAULT,
                        rch,
                        entityBuilder));
        }

        // Add second product
        if(quantityToAdd > 1 && doNegativeEdgeCase){

        	autoAddResults.add(
                AutoAddUtility.issueNewAutoAddChildCommand(
                        model,
                        parentEntity,
                        parentEntityParentEntity,
                        simpleTool,
                        secondChildPos,
                        new float[] {0.0f, 0.0f, 1.0f, 0.0f},
                        forceFit,
                        axis,
                        ORIENTATION.DEFAULT,
                        rch,
                        entityBuilder));

        }

        if(quantityToAdd > 2){
        	
            // Add remaining in between
            double posStep = distanceBetween/(quantityToAdd-1);

            for(int i = 2; i < quantityToAdd; i++){

                double offset = posStep * (i - 1);

                double[] childPos = new double[3];

                switch(axis){
                    case XAXIS:
                        childPos[0] = firstChildPos[0] - offset;
                        childPos[1] = firstChildPos[1];
                        childPos[2] = firstChildPos[2];
                        break;
                    case YAXIS:
                        childPos[0] = firstChildPos[0];
                        childPos[1] = firstChildPos[1] - offset;
                        childPos[2] = firstChildPos[2];
                        break;
                    case ZAXIS:
                        childPos[0] = firstChildPos[0];
                        childPos[1] = firstChildPos[1];
                        childPos[2] = firstChildPos[2] - offset;
                        break;
                }

                autoAddResults.add(
                    AutoAddUtility.issueNewAutoAddChildCommand(
                            model,
                            parentEntity,
                            parentEntityParentEntity,
                            simpleTool,
                            childPos,
                            new float[] {0.0f, 0.0f, 1.0f, 0.0f},
                            forceFit,
                            axis,
                            ORIENTATION.DEFAULT,
                            rch,
                            entityBuilder));
            }
        }
    }
    
}
