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
import java.util.Arrays;
import java.util.HashMap;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if movement uses incremental snaps to specific position values.
 * If so, it updates the position in the command for future checking.
 *
 * @author Ben Yarger
 * @version $Revision: 1.65 $
 */
class MovementUsesIncrementalSnapsRule extends BaseRule{

	/** Constant threshold value used to calculate THRESHOLD */
	private static final double INIT_THRESHOLD = 0.25;

	/** Threshold used in snap calculations */
	private static double THRESHOLD = 0.05;

	/** Notification that movement uses incremental snap */
	private static final String SNAP_MOVE_PROP =
		"org.chefx3d.rules.definitions.MovementUsesIncrementalSnapsRule.usingSnap";

	/** Map of sticky indices, keyed by Entity. */
	private HashMap<Entity, int[]> stickyIndexMap;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementUsesIncrementalSnapsRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.STANDARD;
        stickyIndexMap = new HashMap<Entity, int[]>();

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

		// Check if entity is using incremental snaps, return false if not
		Boolean usesIncrementalSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_USES_INCREMENTAL_SNAPS_PROP);

		if(usesIncrementalSnaps == null ||
				usesIncrementalSnaps.booleanValue() == false){
            result.setResult(false);
            return(result);
		}

		// Calculate the threshold based on the zoom amount
		double zoom = view.getZoneViewZoomAmount();
		THRESHOLD = INIT_THRESHOLD / zoom;

		// Set status bar message
		String usesIncSnapMsg = intl_mgr.getString(SNAP_MOVE_PROP);
		statusBar.setMessage(usesIncSnapMsg);

		// Get the incremental snap values
		Float xAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_X_AXIS_SNAP_PROP);

		Float yAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Y_AXIS_SNAP_PROP);

		Float zAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Z_AXIS_SNAP_PROP);

		// Get the incremental snap exclusion values
		int[] xAxisExclusions = (int[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.INC_X_SNAP_EXCLUDE_PROP);

		int[] yAxisExclusions = (int[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.INC_Y_SNAP_EXCLUDE_PROP);

		int[] zAxisExclusions = (int[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.INC_Z_SNAP_EXCLUDE_PROP);

		// Check if entity is using sticky snaps
		Boolean usesStickySnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.USES_STICKY_SNAPS_PROP);

		int[] stickySnapIndex = null;
		if ((usesStickySnaps != null) && usesStickySnaps.booleanValue()) {
			Object obj =
				RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.STICKY_SNAP_INDEX_PROP);
			if ((obj != null) && (obj instanceof int[])) {
				int[] array = (int[])obj;
				if (array.length > 0) {
					stickySnapIndex = array;
				}
			}
		}

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		double[] newPosition = new double[3];

		// Perform operations depending on if command is transient
		if(command instanceof MoveEntityCommand){

			MoveEntityCommand moveCommand = (MoveEntityCommand)command;

            // get the position data
            moveCommand.getEndPosition(newPosition);

            double[] endParentPos = null;

            // get the parent entity, should already be in the scene
            int parentEntityID = entity.getParentEntityID();
            Entity parentEntity = model.getEntity(parentEntityID);

            if (parentEntity == null) {
                result.setResult(true);
                return(result);
            }

            // get the relative position
            endParentPos = getRelativePosToSnapTarget(command, entity, parentEntity);
            
            // adjust the child to be relative to the pick parent so we can
            // calculate the snap position relative to it.
            if (endParentPos != null) {
                newPosition[0] -= endParentPos[0];
                newPosition[1] -= endParentPos[1];
                newPosition[2] -= endParentPos[2];
            }
            
            // apply the snaps
            applyIncrementalSnaps(
            		model,
                    entity,
                    newPosition,
                    xAxisSnapf,
                    yAxisSnapf,
                    zAxisSnapf,
                    xAxisExclusions,
                    yAxisExclusions,
                    zAxisExclusions,
                    stickySnapIndex);

            // need to set the position back to be relative to the zone
            if (endParentPos != null) {
                newPosition[0] += endParentPos[0];
                newPosition[1] += endParentPos[1];
                newPosition[2] += endParentPos[2];
            }
            
            // now actually set the modified position
			((MoveEntityCommand)command).setEndPosition(newPosition);

		} else if (command instanceof MoveEntityTransientCommand){

			MoveEntityTransientCommand moveTransient =
				(MoveEntityTransientCommand)command;

            // get the position data
            moveTransient.getPosition(newPosition);

            double[] endParentPos = null;

            // get the parent entity, should already be in the scene
            Entity parentEntity = moveTransient.getPickParentEntity();

            if (parentEntity == null) {
                result.setResult(true);
                return(result);
            }

            // get the relative position
            endParentPos = getRelativePosToSnapTarget(command, entity, parentEntity);
            
            // adjust the child to be relative to the pick parent so we can
            // calculate the snap position relative to it.
            if (endParentPos != null) {
                newPosition[0] -= endParentPos[0];
                newPosition[1] -= endParentPos[1];
                newPosition[2] -= endParentPos[2];
            }
            
            // apply the snaps
            applyIncrementalSnaps(
                    model,
                    entity,
                    newPosition,
                    xAxisSnapf,
                    yAxisSnapf,
                    zAxisSnapf,
                    xAxisExclusions,
                    yAxisExclusions,
                    zAxisExclusions,
                    stickySnapIndex);

            // need to set the position back to be relative to the zone
            if (endParentPos != null) {
                newPosition[0] += endParentPos[0];
                newPosition[1] += endParentPos[1];
                newPosition[2] += endParentPos[2];
            }

            // now actually set the modified position
            moveTransient.setPosition(newPosition);

        } else if (command instanceof TransitionEntityChildCommand &&
                !command.isTransient()) {

            TransitionEntityChildCommand transitionChildCommand =
                (TransitionEntityChildCommand)command;
            
            // get the position data
            transitionChildCommand.getEndPosition(newPosition);
            
            double[] endParentPos = null;
            
            // get the parent entity, should already be in the scene
            Entity parentEntity =
                transitionChildCommand.getEndParentEntity();
            
            if (parentEntity == null) {
                result.setResult(true);
                return(result);
            }
            
            // get the relative position
            endParentPos = getRelativePosToSnapTarget(command, entity, parentEntity);
            
            // adjust the child to be relative to the pick parent so we can
            // calculate the snap position relative to it.
            if (endParentPos != null) {
                newPosition[0] -= endParentPos[0];
                newPosition[1] -= endParentPos[1];
                newPosition[2] -= endParentPos[2];
            }
            
            // apply the snaps
            applyIncrementalSnaps(
                model,
                entity,
                newPosition,
                xAxisSnapf,
                yAxisSnapf,
                zAxisSnapf,
                xAxisExclusions,
                yAxisExclusions,
                zAxisExclusions,
                stickySnapIndex);
            
            // need to set the position back to be relative to the zone
            if (endParentPos != null) {
                newPosition[0] += endParentPos[0];
                newPosition[1] += endParentPos[1];
                newPosition[2] += endParentPos[2];
            }
            
            // now actually set the modified position
            transitionChildCommand.setEndPosition(newPosition);
            
		} else if (command instanceof AddEntityChildCommand){

		    // get the child entity being added
			PositionableEntity childEntity = (PositionableEntity) entity;

			// get the position data
	        childEntity.getPosition(newPosition);

            AddEntityChildCommand addChildCmd =
                (AddEntityChildCommand)command;

            // get the parent entity, should already be in the scene
            Entity parentEntity = addChildCmd.getParentEntity();

            double[] endParentPos = null;

            // get the relative position
            endParentPos = getRelativePosToSnapTarget(command, entity, parentEntity);
            
            // adjust the child to be relative to the pick parent so we can
            // calculate the snap position relative to it.
            if (endParentPos != null) {
                newPosition[0] -= endParentPos[0];
                newPosition[1] -= endParentPos[1];
                newPosition[2] -= endParentPos[2];
            }

            // apply the snaps
            applyIncrementalSnaps(
                    model,
                    entity,
                    newPosition,
                    xAxisSnapf,
                    yAxisSnapf,
                    zAxisSnapf,
                    xAxisExclusions,
                    yAxisExclusions,
                    zAxisExclusions,
                    stickySnapIndex);

            // need to set the position back to be relative to the zone
            if (endParentPos != null) {
                newPosition[0] += endParentPos[0];
                newPosition[1] += endParentPos[1];
                newPosition[2] += endParentPos[2];
            }

            // now actually set the modified position
			childEntity.setPosition(newPosition, command.isTransient());

		} else if(command instanceof AddEntityChildTransientCommand){


            // get the child entity being added
            PositionableEntity childEntity = (PositionableEntity) entity;

            // get the position data
            childEntity.getPosition(newPosition);

            AddEntityChildTransientCommand addChildCmd =
                (AddEntityChildTransientCommand)command;

            // get the parent entity, should already be in the scene
            Entity parentEntity = addChildCmd.getParentEntity();

            double[] endParentPos = null;

            // get the relative position
            endParentPos = getRelativePosToSnapTarget(command, entity, parentEntity);
            
            // adjust the child to be relative to the pick parent so we can
            // calculate the snap position relative to it.
            if (endParentPos != null) {
                newPosition[0] -= endParentPos[0];
                newPosition[1] -= endParentPos[1];
                newPosition[2] -= endParentPos[2];
            }
            
            // apply the snaps
            applyIncrementalSnaps(
                    model,
                    entity,
                    newPosition,
                    xAxisSnapf,
                    yAxisSnapf,
                    zAxisSnapf,
                    xAxisExclusions,
                    yAxisExclusions,
                    zAxisExclusions,
                    stickySnapIndex);

            // need to set the position back to be relative to the zone
            if (endParentPos != null) {
                newPosition[0] += endParentPos[0];
                newPosition[1] += endParentPos[1];
                newPosition[2] += endParentPos[2];
            }
            
            // now actually set the modified position
            childEntity.setPosition(newPosition, command.isTransient());

        }

        result.setResult(true);
        return(result);
	}
    
    /**
     * 
     * @param command
     * @param parentEntity
     * @return
     */
    private double[] getRelativePosToSnapTarget(
            Command command, Entity entity, Entity parentEntity) {
        
        double[] endParentPos = new double[3];
        
        // Flag to set all snaps relative to floor
        Boolean snapToZone = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SNAP_RELATIVE_TO_FLOOR);

        // Flag to set all snaps relative to model
        Boolean snapToParent = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SNAP_TO_MODEL_ONLY);

        if ((snapToZone && snapToParent) || (!snapToZone && !snapToParent)) {
            
            // Do independent collision check
            rch.performCollisionCheck(command, true, false, false);

            // If collisionEntities is null (no collisions occurred) then return false
            if(rch.collisionEntities == null){
                return null;
            }
            
            /*
             * Perform collision analysis
             */
            rch.performCollisionAnalysisHelper(
                    parentEntity, 
                    null, 
                    false, 
                    null, 
                    true);

            /// get all the entities
            ArrayList<Entity> entityMatches =
                collisionResults.getEntityMatches();
            
            if (entityMatches != null && entityMatches.size() > 0) {
            
                // find the center of the bounds
                float[] multiBounds = new float[6];            
                BoundsUtils.getMultiBounds(
                        model,
                        multiBounds, 
                        endParentPos, 
                        entityMatches, 
                        (PositionableEntity) parentEntity,
                        true);
            }

        } else if (snapToZone) {
            
            // since its in the scene, we can get the position
            // relative to the zone
            endParentPos =
                TransformUtils.getPositionRelativeToZone(model, parentEntity);

        } else if (snapToParent) {
            
            if (command instanceof AddEntityChildCommand || 
                    command instanceof TransitionEntityChildCommand) {
                
                // the position data is already relative to the parent so leave it
                endParentPos = null;

            } else {
                
                // the position data needs to be relative to parent
                endParentPos = 
                    TransformUtils.getPositionRelativeToZone(model, parentEntity);

            }

        }

        return endParentPos;
        
    }

	/**
	 * Process each individual axis with increment data. Hand off each set of
	 * data for processing. The first parameter, position, will be updated
	 * with the increment value correct position.
	 *
	 * @param model WorldModel to reference
	 * @param entity The Entity being positioned
	 * @param position double[] xyz position input from user
	 * @param xAxisSnapf Float x axis increment value (if null ignored)
	 * @param yAxisSnapf Float y axis increment value (if null ignored)
	 * @param zAxisSnapf Float z axis increment value (if null ignored)
	 * @param xAxisExclusions increment indexes to skip
	 * @param yAxisExclusions increment indexes to skip
	 * @param zAxisExclusions increment indexes to skip
	 * @param stickySnapIndex The indices of the sticky snaps, or null.
	 * @return True if applied, false otherwise
	 */
	private boolean applyIncrementalSnaps(
			WorldModel model,
			Entity entity,
			double[] position,
			Float xAxisSnapf,
			Float yAxisSnapf,
			Float zAxisSnapf,
			int[] xAxisExclusions,
			int[] yAxisExclusions,
			int[] zAxisExclusions,
			int[] stickySnapIndex){

		// Convert values
		Double xAxisSnap = null;
		Double yAxisSnap = null;
		Double zAxisSnap = null;

		if(xAxisSnapf != null){
			xAxisSnap = (double)xAxisSnapf.floatValue();
		}

		if(yAxisSnapf != null){
			yAxisSnap = (double) yAxisSnapf.floatValue();
		}

		if(zAxisSnapf != null){
			zAxisSnap = (double) zAxisSnapf.floatValue();
		}

		int[] sindex = null;
		if (stickySnapIndex != null) {
			sindex = stickyIndexMap.get(entity);
			if (sindex == null) {
				sindex = new int[]{-1, -1, -1};
				stickyIndexMap.put(entity, sindex);
			}
		}

		// Process values
		if(xAxisSnap != null){

	        // Get the buffer value and add it to the result
            Float horizontalBuffer =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.CENTER_HORIZONTAL_POS_BUFF_PROP);

			position[0] = processIncrementalSnapValues(
					position[0],
					xAxisSnap,
					horizontalBuffer, 
					xAxisExclusions,
					stickySnapIndex,
					0,
					sindex);

		}

		if(yAxisSnap != null){

            // Get the buffer value
            Float verticalBuffer =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.CENTER_VERTICAL_POS_BUFF_PROP);

			position[1] = processIncrementalSnapValues(
					position[1],
					yAxisSnap,
					verticalBuffer, 
					yAxisExclusions,
					stickySnapIndex,
					1,
					sindex);

		}

		if(zAxisSnap != null){

            // Get the buffer value
            Float depthBuffer =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.CENTER_DEPTH_POS_BUFF_PROP);

			position[2] = processIncrementalSnapValues(
					position[2],
					zAxisSnap,
					depthBuffer, 
					zAxisExclusions,
					stickySnapIndex,
					2,
					sindex);

		}



		return true;
	}

	/**
	 * Processes each individual axis increments. Returns the new axis value
	 * for the axis data processed.
	 *
	 * @param movement double distance being moved (increment evaluated)
	 * @param incrementAxisValue Double value of the increment step
	 * @param exclusions step indexes to avoid
	 * @param stickySnapIndex The indices of the sticky snaps
	 * @param axis Index of the axis being processed (0 = X, 1 = Y, 2 = Z)
	 * @param sindex The snap index values that have already been assigned
	 * @return new double position value for the axis
	 */
	private double processIncrementalSnapValues(
			double movement,
			Double incrementAxisValue,
			float bufferAxisValue, 
			int[] exclusions,
			int[] stickySnapIndex,
			int axis,
			int[] sindex){

		////////////////////////////////////////////////////////////
		// rem: current usage, this can't be null
		//if(incrementAxisValue != null && incrementAxisValue == 0.0){
		if(incrementAxisValue == 0.0){
			return 0.0;
		}
		////////////////////////////////////////////////////////////
		int sign = 1;

		if(movement < 0){
			sign = -1;
		}
		////////////////////////////////////////////////////////////
		// rem: something funky?
		//int index =
		//	(int) (((movement-startPos+startPos)+incrementAxisValue/2.0*sign)/
		//			(incrementAxisValue));
		int index = sign * (int)((movement - bufferAxisValue + incrementAxisValue * 0.5) / incrementAxisValue);

		// BJY: in the negative direction we need to include an additional
		// index to account for the error in calculation
		if (sign < 0 && index != 0) {
			index++;
		}
		////////////////////////////////////////////////////////////
		
		// Check exclusions
		if(exclusions != null && exclusions.length > 0){

			Arrays.sort(exclusions);

			boolean exclusionMatchExists = false;
			int exclusionIndex = 0;

			/*
			 * Search for exclusion match
			 */
			for(int x = 0; x < exclusions.length; x++){

				if(exclusions[x] == index){

					exclusionIndex = x;
					exclusionMatchExists = true;
					break;

				} else if (exclusions[x] > index) {

					break;

				}
			}

			if(exclusionMatchExists){

				int upperBoundsIndex = exclusions.length+1;
				int lowerBoundsIndex = -1;

				/*
				 * Determine the upper and lower exclusion bounds for the
				 * series expressed. For example, given the exclusion set
				 * [-4 -3 -2 2 3 4] and the index calculated to 3 we want
				 * to know what the outside bounds of the series are. In
				 * this case it would be 2 and 4 for the lower and upper
				 * bounds. We need to know this in order to decide how to
				 * set the increment step when the step is an exception.
				 */

				for(int i = exclusionIndex+1; i < exclusions.length; i++){

					int nextSequentialValue =
						exclusions[exclusionIndex]+ i - exclusionIndex;

					if(nextSequentialValue != exclusions[i]){
						upperBoundsIndex = i;
						break;
					}
				}

				for(int i = exclusionIndex - 1; i >= 0; i--){

					int nextSequentialValue =
						exclusions[exclusionIndex] - (exclusionIndex - i);

					if(nextSequentialValue != exclusions[i]){
						lowerBoundsIndex = i;
						break;
					}
				}

				/*
				 * Sort out the correct index to use
				 */
				if(lowerBoundsIndex == -1 &&
						upperBoundsIndex == (exclusions.length + 1)){

					int halfWayIndex = exclusions.length/2 - 1;

					// Check to avoid index out of bounds exception
					if(halfWayIndex < 0){
						halfWayIndex = 0;
					}

					if(index >= exclusions[halfWayIndex]){

						index = exclusions[exclusions.length -1]+1;

					} else {

						index = exclusions[0] - 1;
					}

				} else if (lowerBoundsIndex == -1){

					int lowIndex = exclusions[0] - 1;
					int highIndex = upperBoundsIndex;

					if(Math.abs(lowIndex - index) > Math.abs(highIndex-index)){
						index = highIndex;
					} else {
						index = lowIndex;
					}

				} else if (upperBoundsIndex == (exclusions.length + 1)){

					int lowIndex = lowerBoundsIndex;
					int highIndex = exclusions[exclusions.length-1]+1;

					if(Math.abs(lowIndex - index) > Math.abs(highIndex-index)){
						index = highIndex;
					} else {
						index = lowIndex;
					}

				} else {

					if(Math.abs(lowerBoundsIndex - index) > Math.abs(upperBoundsIndex - index)){

						index = upperBoundsIndex;

					} else if (Math.abs(lowerBoundsIndex - index) < Math.abs(upperBoundsIndex - index)){

						index = lowerBoundsIndex;

					} else {

						if(sign < 0){

							index = lowerBoundsIndex;

						} else {

							index = upperBoundsIndex;
						}
					}
				}

			}
		}

		if (stickySnapIndex != null) {

			// if sticky snaps are active, figure out what to do....
			int previous_snap_idx = sindex[axis];
			int current_snap_idx = index;
			boolean useCurrent = true;

			if (previous_snap_idx == current_snap_idx) {
				// the previous is still the closest
				useCurrent = false;
			} else {
				// the current closest is different than the previous,
				// check if the previous was sticky
				boolean previousSnapIsSticky = false;
				if (previous_snap_idx != -1) {
					for (int i = 0; i < stickySnapIndex.length; i++) {
						if (previous_snap_idx == stickySnapIndex[i]) {
							previousSnapIsSticky = true;
							break;
						}
					}
				}

				if (previousSnapIsSticky) {
					// the previous snap position IS sticky and is different from
					// the current closest - decide what to do
					if (current_snap_idx > previous_snap_idx) {
						// the new current snap position is in the positive direction
						if (movement < incrementAxisValue * current_snap_idx) {
							// don't switch until current value passes the next snap
							useCurrent = false;
						}
					} else {
						// the new current snap position is in the negative direction
						if (movement > incrementAxisValue * current_snap_idx) {
							// don't switch until current value passes the next snap
							useCurrent = false;
						}
					}
				}
			}
			if (useCurrent) {
				// use the new snap position
				index = current_snap_idx;
				sindex[axis] = current_snap_idx;
			} else {
				// use the previous snap position
				index = previous_snap_idx;
			}
		}

		// BJY: needed to match the sign
		double newPosition = sign * index * incrementAxisValue + bufferAxisValue;

		return newPosition;
	}
}
