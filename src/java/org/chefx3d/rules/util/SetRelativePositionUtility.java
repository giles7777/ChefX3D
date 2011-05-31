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

//External Imports
import java.util.ArrayList;

import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

/**
 * Utility for establishing relative position for an entity based on the
 * genPos values assigned. Capable of calculating genPos against a single entity
 * or a list of entities. To calculate against a list of entities, use multi
 * collision positioning will need to be set to true.
 *
 * @author Ben Yarger
 * @version $Revision: 1.15 $
 */
public abstract class SetRelativePositionUtility {

	/**
	 * Capable of calculating genPos against a single entity
	 * or a list of entities. To calculate against a list of entities, use multi
	 * collision positioning will need to be set to true.
	 *
	 * @param model reference to the WorldModel
	 * @param entity Entity to apply genPos to
	 * @param targetSet List of Entity(s) to perform genPos calculation against
	 * @param newPosition double[] array into which we will copy the
	 * newly calculated position.
	 * @return True if the calculation executed successfully, false otherwise
	 */
	public static boolean setRelativePosition(
			WorldModel model,
			PositionableEntity entity,
			PositionableEntity parentEntity,
			ArrayList<Entity> targetSet,
			double[] newPosition) {

			return setPosition(
					model, entity, parentEntity, targetSet, newPosition);
	}

	/**
	 *  <p>
	 *  If parent entity is specified, we will use the given parentEntity.
	 *  Otherwise we will get the parent via the RuleDataAccessor.
	 *
	 * @author Eric Fickenscher
	 * @param command - expecting some sort of move command, though
	 * any command that implements RuleDataAccessor should be valid.
	 * @param newPosition double[] array into which we will copy the
	 * newly calculated position.
	 * @param model reference to the WorldModel
	 * @param parentEntity If this Entity value is NOT null, we will use
	 * the given entity to calculate our relative position.
	 * Example of use: it is the parent entity found via a
	 * findAppropriateParent(command, model)
	 * call in InitialMovePositionCorrectionRule.java.  If the entity value
	 * IS null, then we will get the parent via the RuleDataAccessor.
	 */
	public static boolean setRelativePosition(
			Command command,
			WorldModel model,
			double[] newPosition,
			PositionableEntity parentEntity,
			RuleCollisionHandler rch) {

		PositionableEntity entity;

		if( command instanceof RuleDataAccessor){
			
			entity = 
				(PositionableEntity) ((RuleDataAccessor)command).getEntity();

			if(parentEntity == null){

				if (command instanceof AddEntityChildCommand)
					parentEntity = (PositionableEntity)
						((AddEntityChildCommand)command).getParentEntity();

				else if (command instanceof AddEntityChildTransientCommand)
					parentEntity = (PositionableEntity)
						((AddEntityChildTransientCommand)
								command).getParentEntity();

				else if (command instanceof TransitionEntityChildCommand)
					parentEntity = (PositionableEntity)
						((TransitionEntityChildCommand)
								command).getEndParentEntity();

				else
					parentEntity = (PositionableEntity) 
						model.getEntity(entity.getParentEntityID());
			}

			// now lets get the target list
			ArrayList<Entity> targetSet = new ArrayList<Entity>();

			// do the collision
			rch.performCollisionCheck(command, true, true, true);

			// do the analysis
			rch.performCollisionAnalysisHelper(
			        entity,
			        null,
			        false,
			        null,
			        true);

			// validate the collisions
			boolean illegalCollisions = rch.hasIllegalCollisionHelper(entity);

			if (!illegalCollisions && rch.collisionEntities != null) {

				Boolean isAutoAdd = (Boolean) 
				RulePropertyAccessor.getRulePropertyValue(
						entity, 
						ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
				
				// if this is an auto-add, always make sure the parent is the
				// first item in the target list
				if (isAutoAdd) {
					targetSet.add(parentEntity);
				}

			    targetSet.addAll(rch.collisionEntities);

    			return setPosition(
    			        model,
    			        (PositionableEntity) entity,
    			        (PositionableEntity) parentEntity,
    			        targetSet,
    			        newPosition);
			} else {
				
				// If the entity is an auto add entity, then we will allow 
				// it to perform genPos since auto adds only have one parent
				// and the genPos will only ever be applied to it.
				
				Boolean isAutoAdd = (Boolean) 
					RulePropertyAccessor.getRulePropertyValue(
							entity, 
							ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
				
				if (isAutoAdd) {
					
					if (AutoAddUtility.isAutoAddChildOfParent(
							entity, parentEntity)) {
						
						targetSet.add(parentEntity);

		    			return setPosition(
		    			        model,
		    			        (PositionableEntity) entity,
		    			        (PositionableEntity) parentEntity,
		    			        targetSet,
		    			        newPosition);
					}
				}
			}

		}

		return false;

	}


	/**
	 * Adjust position.
	 *
	 * @param command Command causing adjustment
	 * @param model WorldModel object
	 * @param newPosition double[] newly calculated pos
	 * @return True if adjustment made, false otherwise
	 */
	public static boolean setRelativePosition(
			Command command,
			WorldModel model,
			double[] newPosition,
			RuleCollisionHandler rch) {

		return setRelativePosition(command, model, newPosition, null, rch);
	}

	//---------------------------------------------------------------
	// Private methods
	//---------------------------------------------------------------

	/**
	 * Generate the genPos position based on the incoming data.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity to apply genPos to
	 * @param
	 * @param targetEntitySet List of entities to calculate genPos against. If
	 * there is only one entity in the list the single collision calculation
	 * will occur, if there are more than one collision and multi collision
	 * positioning is true, we will calculate genPos for the full set of
	 * collisions.
	 * @param newPosition We set the position data into this double array.
	 * @return TRUE is position was set, FALSE otherwise.
	 */
	private static boolean setPosition(
			WorldModel model,
			PositionableEntity entity,
			PositionableEntity parentEntity,
			ArrayList<Entity> targetEntitySet,
			double[] newPosition){

		// Extract the relationship data
		ChefX3DRuleProperties.RELATIVE_GENERAL_POSITION genPosition =
			(ChefX3DRuleProperties.RELATIVE_GENERAL_POSITION)
				RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.GENERAL_POSITION_PROP);

		// If genPosition null, do not proceed
		if(genPosition == null){
			return false;
		}

		// Check for gen pos multi collision flag
		Boolean useMutliColPos = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.USE_MULTI_COLLISION_POSITIONING);

		// Process the multi collision case first if appropriate, otherwise
		// handle the single collision case
		if(targetEntitySet.size() > 1 && useMutliColPos) {

			float[] multiBounds = new float[6];
			double[] multiCenter = new double[3];

			if (!BoundsUtils.getMultiBounds(
					model,
					multiBounds,
					multiCenter,
					targetEntitySet,
					parentEntity,
					true)) {
				return false;
			}

			// Pass off for multi parent positioning
			genPosMultiCollision(
					entity,
					genPosition,
					multiBounds,
					multiCenter,
					newPosition);

			return true;

		} else if (targetEntitySet.size() == 1) {

			entity.setProperty(
	                Entity.DEFAULT_ENTITY_PROPERTIES,
	                ChefX3DRuleProperties.POSSIBLE_PARENT,
	                targetEntitySet.get(0),
	                false);

			// Calculate the general position for newPosition
			genPosSingleCollision(
					model,
					entity,
					(PositionableEntity) targetEntitySet.get(0),
					genPosition,
					newPosition,
					false);

			return true;

		} else if (targetEntitySet.size() > 1 && parentEntity != null) {

			// Handle the case where there is more than one target entity,
			// but multi selection is not selected. In that case, if there
			// is a parentEntity passed in that is also in the target entity
			// set, apply the gen pos to that.
			if (targetEntitySet.contains(parentEntity)) {

				entity.setProperty(
		                Entity.DEFAULT_ENTITY_PROPERTIES,
		                ChefX3DRuleProperties.POSSIBLE_PARENT,
		                parentEntity,
		                false);

				// Calculate the general position for newPosition
				genPosSingleCollision(
						model,
						entity,
						parentEntity,
						genPosition,
						newPosition,
						false);

				return true;
			}

		} else if (targetEntitySet.size() == 0 && parentEntity != null) {
	
			// Only apply this case if there is a none relationship
			String[] relationships = (String[])
				RulePropertyAccessor.getRulePropertyValue(
						entity, 
						ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
			
			if (relationships == null) {
				return false;
			}
			
			boolean hasNoneRelationship = false;
			for (int i = 0; i < relationships.length; i++) {
				
				if (relationships[i].equalsIgnoreCase(
						ChefX3DRuleProperties.RESERVED_NONE_RELATIONSHIP)) {
					hasNoneRelationship = true;
					break;
				}
			}
			
			if (!hasNoneRelationship) {
				return false;
			}
			
			// Calculate the general position for newPosition
			genPosSingleCollision(
					model,
					entity,
					parentEntity,
					genPosition,
					newPosition,
					false);

			return true;
		}

		return false;
	}

	/**
	 * Process the general position for a single collision case.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity causing collision
	 * @param parentEntity Parent entity which should also be the collision obj
	 * @param genPosition General position to calculate
	 * @param newPosition Contains the calculated gen pos to use when returned
	 * @param sumParentPositions Flag to sum parent positions
	 */
	private static void genPosSingleCollision(
			WorldModel model,
			PositionableEntity entity,
			PositionableEntity parentEntity,
			ChefX3DRuleProperties.RELATIVE_GENERAL_POSITION genPosition,
			double[] newPosition,
			boolean sumParentPositions){
		
		Boolean genPosDirection = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity, 
					ChefX3DRuleProperties.GENERAL_POSITION_DIRECTION_PROP);

	    // define a working variable
		double[] pos;

		// Get the buffer values
		Float horizontalBuffer =
            (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CENTER_HORIZONTAL_POS_BUFF_PROP);

		Float verticalBuffer =
            (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CENTER_VERTICAL_POS_BUFF_PROP);

		Float depthBuffer =
            (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CENTER_DEPTH_POS_BUFF_PROP);

        if(horizontalBuffer == null){
            horizontalBuffer = 0.0f;
        }

        if(verticalBuffer == null){
            verticalBuffer = 0.0f;
        }

        if(depthBuffer == null){
            depthBuffer = 0.0f;
        }

        // Adjust the base position based on the gen position picked.  Once
        // that has been calculated then adjust the position by the buffer
        // value.
		switch(genPosition){

			case FRONT:

				pos = singleParentFront(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case FRONT_HORIZONTAL:

				pos = singleParentFront(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case FRONT_VERTICAL:

				pos = singleParentFront(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BACK:

				pos = singleParentBack(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BACK_HORIZONTAL:

				pos = singleParentBack(
						entity,
						parentEntity, 
						model,
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BACK_VERTICAL:

				pos = singleParentBack(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case LEFT:

				pos = singleParentLeft(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case LEFT_HORIZONTAL:

				pos = singleParentLeft(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case RIGHT:

				pos =  singleParentRight(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case RIGHT_HORIZONTAL:

				pos =  singleParentRight(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case TOP:

				pos = singleParentTop(
						entity,
						parentEntity, 
						model, 
						sumParentPositions, 
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case TOP_VERTICAL:

				pos = singleParentTop(
						entity,
						parentEntity, 
						model, 
						sumParentPositions, 
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BOTTOM:

				pos = singleParentBottom(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BOTTOM_VERTICAL:

				pos = singleParentBottom(
						entity,
						parentEntity, 
						model, 
						sumParentPositions,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case CENTER:

				pos = singleParentCenter(
						model, parentEntity, sumParentPositions);

				newPosition[0] = pos[0] + horizontalBuffer;
				newPosition[1] = pos[1] + verticalBuffer;
				newPosition[2] = pos[2] + depthBuffer;

				break;

			case CENTER_HORIZONTAL:

				pos = singleParentCenter(
						model, parentEntity, sumParentPositions);

				newPosition[0] = pos[0] + horizontalBuffer;
				newPosition[1] = newPosition[1] + verticalBuffer;
				newPosition[2] = pos[2] + depthBuffer;

				break;

			case CENTER_VERTICAL:

				pos = singleParentCenter(
						model, parentEntity, sumParentPositions);

				newPosition[0] = newPosition[0] + horizontalBuffer;
				newPosition[1] = pos[1] + verticalBuffer;
				newPosition[2] = pos[2] + depthBuffer;

				break;
		}
    }

	/**
	 * Generate the multi position general position.
	 *
	 * @param entity Entity to generate general position for
	 * @param genPosition General position to apply
	 * @param multiBounds Multi collision bounds
	 * @param multiCenter Multi collision center
	 * @param newPosition Resulting general position that will be populated
	 */
	private static void genPosMultiCollision(
			PositionableEntity entity,
			ChefX3DRuleProperties.RELATIVE_GENERAL_POSITION genPosition,
			float[] multiBounds,
			double[] multiCenter,
			double[] newPosition){
		
		Boolean genPosDirection = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ChefX3DRuleProperties.GENERAL_POSITION_DIRECTION_PROP);

        // define a working variable
        double[] pos;

        /*
         * Get the buffer values
         */
        Float horizontalBuffer =
            (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CENTER_HORIZONTAL_POS_BUFF_PROP);

        Float verticalBuffer =
            (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CENTER_VERTICAL_POS_BUFF_PROP);

        Float depthBuffer =
            (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CENTER_DEPTH_POS_BUFF_PROP);

        if(horizontalBuffer == null){
            horizontalBuffer = 0.0f;
        }

        if(verticalBuffer == null){
            verticalBuffer = 0.0f;
        }

        if(depthBuffer == null){
            depthBuffer = 0.0f;
        }

        // Adjust the base position based on the gen position picked.  Once
        // that has been calculated then adjust the position by the buffer
        // value.
		switch(genPosition){

			case FRONT:

			    pos = multiCollisionFront(
			    		entity,
			    		multiBounds, 
			    		multiCenter,
			    		genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case FRONT_HORIZONTAL:

				pos = multiCollisionFront(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case FRONT_VERTICAL:

				pos = multiCollisionFront(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BACK:

				pos = multiCollisionBack(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

                break;

			case BACK_HORIZONTAL:

				pos = multiCollisionBack(
						entity,
						multiBounds,
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BACK_VERTICAL:

				pos = multiCollisionBack(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case LEFT:

				pos = multiCollisionLeft(
						entity,
						multiBounds,
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case LEFT_HORIZONTAL:

				pos = multiCollisionLeft(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

				newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case RIGHT:

				pos = multiCollisionRight(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case RIGHT_HORIZONTAL:

				pos = multiCollisionRight(
						entity,
						multiBounds,
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                //newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case TOP:

				pos = multiCollisionTop(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case TOP_VERTICAL:

				pos = multiCollisionTop(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BOTTOM:

				pos = multiCollisionBottom(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case BOTTOM_VERTICAL:

				pos = multiCollisionBottom(
						entity,
						multiBounds, 
						multiCenter,
						genPosDirection);

                //newPosition[0] = pos[0] + horizontalBuffer;
                newPosition[1] = pos[1] + verticalBuffer;
                newPosition[2] = pos[2] + depthBuffer;

				break;

			case CENTER:

				pos = multiCollisionCenter(multiBounds, multiCenter);

				newPosition[0] = pos[0] + horizontalBuffer;
				newPosition[1] = pos[1] + verticalBuffer;
				newPosition[2] = pos[2] + depthBuffer;

				break;

			case CENTER_HORIZONTAL:

				pos = multiCollisionCenter(multiBounds, multiCenter);

				newPosition[0] = pos[0] + horizontalBuffer;
				newPosition[1] = newPosition[1] + verticalBuffer;
				newPosition[2] = pos[2] + depthBuffer;

				break;

			case CENTER_VERTICAL:

				pos = multiCollisionCenter(multiBounds, multiCenter);

				newPosition[0] = newPosition[0] + horizontalBuffer;
				newPosition[1] = pos[1] + verticalBuffer;
				newPosition[2] = pos[2] + depthBuffer;

				break;
		}
    }

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to single parent.
	 *
	 * @param model WorldModel
	 * @param parentEntity Entity parent entity
	 * @param relParentSum sum up the position relative to zone
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentCenter(
			WorldModel model,
			Entity parentEntity,
			boolean relParentSum){

		double[] relPos = null;

		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		return relPos;
	}

	/**
	 * Perform multi collision center calculation
	 *
	 * @param multiBounds Bounds of collision set
	 * @param multiCenter Center of collision set
	 * @return Center of collision set
	 */
	private static double[] multiCollisionCenter(
			float[] multiBounds,
			double[] multiCenter){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0];
		newPos[1] = multiCenter[1];
		newPos[2] = multiCenter[2];

		return newPos;

	}

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to parent back.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param parentEntity Parent of entity
	 * @param model WorldModel
	 * @param relParentSum sum up the position relative to zone
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentBack(
			PositionableEntity entity,
			PositionableEntity parentEntity,
			WorldModel model,
			boolean relParentSum,
			boolean genPosDirection){

		double[] relPos = null;
		
		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		if (genPosDirection) {
	
			float[] bounds = BoundsUtils.getBounds(parentEntity, true);
	
			relPos[2] = relPos[2] + bounds[4];
			
		} else {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
	
			relPos[2] = relPos[2] - bounds[4];
			
		}

		return relPos;
	}

	/**
	 * Get the multi collision back position
	 *
	 * @param entity Entity to apply gen pos to
	 * @param multiBounds Multi collision bounds
	 * @param multiCenter Multi collision center
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return Multi collision back position
	 */
	private static double[] multiCollisionBack(
			PositionableEntity entity,
			float[] multiBounds,
			double[] multiCenter,
			boolean genPosDirection){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0];
		newPos[1] = multiCenter[1];
		newPos[2] = multiCenter[2] + multiBounds[4];
		
		if (!genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			newPos[2] = multiCenter[2] - bounds[4];
			
		}

		return newPos;
	}

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to parent front.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param parentEntity Parent of entity
	 * @param model WorldModel
	 * @param relParentSum sum up the position relative to zone
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentFront(
			PositionableEntity entity,
			PositionableEntity parentEntity,
			WorldModel model,
			boolean relParentSum,
			boolean genPosDirection){

		double[] relPos = null;

		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		if (genPosDirection)  {

			float[] bounds = BoundsUtils.getBounds(parentEntity, true);
	
			relPos[2] = relPos[2] + bounds[5];
			
		} else {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			
			relPos[2] = relPos[2] - bounds[5];
			
		}

		return relPos;
	}

	/**
	 * Get the multi collision front position
	 *
	 * @param entity Entity to apply gen pos to
	 * @param multiCenter Multi collision center
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return Multi collision front position
	 */
	private static double[] multiCollisionFront(
			PositionableEntity entity,
			float[] multiBounds,
			double[] multiCenter,
			boolean genPosDirection){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0];
		newPos[1] = multiCenter[1];
		newPos[2] = multiCenter[2] + multiBounds[5];
		
		if (!genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			newPos[2] = multiCenter[2] - bounds[5];
			
		}

		return newPos;
	}

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to parent top.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param parentEntity Parent of entity
	 * @param model WorldModel
	 * @param relParentSum sum up the position relative to zone
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentTop(
			PositionableEntity entity,
			PositionableEntity parentEntity,
			WorldModel model,
			boolean relParentSum,
			boolean genPosDirection){

		double[] relPos = null;

		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		if (genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(parentEntity, true);
	
			relPos[1] = relPos[1] + bounds[3];
		
		} else {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			
			relPos[1] = relPos[1] - bounds[3];	
		}

		return relPos;
	}

	/**
	 * Get the multi collision top position
	 *
	 * @param entity Entity to apply gen pos to
	 * @param multiBounds Multi collision bounds
	 * @param multiCenter Multi collision center
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return Multi collision top position
	 */
	private static double[] multiCollisionTop(
			PositionableEntity entity,
			float[] multiBounds,
			double[] multiCenter,
			boolean genPosDirection){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0];
		newPos[1] = multiCenter[1] + multiBounds[3];
		newPos[2] = multiCenter[2];
		
		if (!genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			newPos[1] = multiCenter[1] - bounds[3];
			
		}

		return newPos;
	}

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to parent bottom.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param parentEntity Parent of entity
	 * @param model WorldModel
	 * @param relParentSum sum up the position relative to zone
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentBottom(
			PositionableEntity entity,
			PositionableEntity parentEntity,
			WorldModel model,
			boolean relParentSum,
			boolean genPosDirection){

		double[] relPos = null;

		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		if (genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(parentEntity, true);
	
			relPos[1] = relPos[1] + bounds[2];
			
		} else {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			
			relPos[1] = relPos[1] - bounds[2];
			
		}

		return relPos;
	}

	/**
	 * Get the multi collision bottom position
	 *
	 * @param entity Entity to apply gen pos to
	 * @param multiBounds Multi collision bounds
	 * @param multiCenter Multi collision center
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return Multi collision bottom position
	 */
	private static double[] multiCollisionBottom(
			PositionableEntity entity,
			float[] multiBounds,
			double[] multiCenter,
			boolean genPosDirection){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0];
		newPos[1] = multiCenter[1] + multiBounds[2];
		newPos[2] = multiCenter[2];
		
		if (!genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			newPos[1] = multiCenter[1] - bounds[2];
			
		}

		return newPos;
	}

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to parent left.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param parentEntity Parent of entity
	 * @param model WorldModel
	 * @param relParentSum sum up the position relative to zone
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentLeft(
			PositionableEntity entity,
			PositionableEntity parentEntity,
			WorldModel model,
			boolean relParentSum,
			boolean genPosDirection){

		double[] relPos = null;

		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		if (genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(parentEntity, true);
	
			relPos[0] = relPos[0] + bounds[0];
			
		} else {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			
			relPos[0] = relPos[0] - bounds[0];
			
		}

		return relPos;
	}

	/**
	 * Get the multi collision left position
	 *
	 * @param entity Entity to apply gen pos to
	 * @param multiBounds Multi collision bounds
	 * @param multiCenter Multi collision center
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return Multi collision left position
	 */
	private static double[] multiCollisionLeft(
			PositionableEntity entity,
			float[] multiBounds,
			double[] multiCenter,
			boolean genPosDirection){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0] + multiBounds[0];
		newPos[1] = multiCenter[1];
		newPos[2] = multiCenter[2];
		
		if (!genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			newPos[0] = multiCenter[0] - bounds[0];
			
		}

		return newPos;
	}

	/**
	 * Perform positioning calculation for single parent.
	 * Centers to parent right.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param parentEntity Parent of entity
	 * @param model WorldModel
	 * @param relParentSum sum up the position relative to zone
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return double[] xyz relative position
	 */
	private static double[] singleParentRight(
			PositionableEntity entity,
			PositionableEntity parentEntity,
			WorldModel model,
			boolean relParentSum,
			boolean genPosDirection){

		double[] relPos = null;

		if(relParentSum){
			Entity zoneEntity =
				SceneHierarchyUtility.findZoneEntity(model, parentEntity);
			relPos =
				TransformUtils.getRelativePosition(
						model, parentEntity, zoneEntity, false);
		}

		if(relPos == null){
			relPos = new double[3];
		}

		if (genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(parentEntity, true);
	
			relPos[0] = relPos[0] + bounds[1];
			
		} else {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			
			relPos[0] = relPos[0] - bounds[1];
			
		}

		return relPos;
	}

	/**
	 * Get the multi collision right position.
	 *
	 * @param entity Entity to apply gen pos to
	 * @param multiBounds Multi collision bounds
	 * @param multiCenter Multi collision center
	 * @param genPosDirection True to do child -> parent, false to do parent
	 * -> child
	 * @return Multi collision right position
	 */
	private static double[] multiCollisionRight(
			PositionableEntity entity,
			float[] multiBounds,
			double[] multiCenter,
			boolean genPosDirection){

		double[] newPos = new double[3];

		newPos[0] = multiCenter[0] + multiBounds[1];
		newPos[1] = multiCenter[1];
		newPos[2] = multiCenter[2];
		
		if (!genPosDirection) {
			
			float[] bounds = BoundsUtils.getBounds(entity, true);
			newPos[0] = multiCenter[0] - bounds[1];
			
		}

		return newPos;
	}
}
