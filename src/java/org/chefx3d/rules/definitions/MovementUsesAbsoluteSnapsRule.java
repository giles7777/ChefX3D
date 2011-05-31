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
import java.util.HashMap;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SetRelativePositionUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.rules.util.RuleUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if movement uses absolute snaps to specific position values.
 * If so, it updates the position in the command for future checking. Will
 * also run the new position through the collision detection system to update
 * the collision list if necessary.
 *
 * @author Ben Yarger
 * @version $Revision: 1.44 $
 */
class MovementUsesAbsoluteSnapsRule extends BaseRule {

	/** Notification that movement uses absolute snap */
	private static final String SNAP_MOVE_PROP =
		"org.chefx3d.rules.definitions.MovementUsesAbsoluteSnapsRule.usingSnap";

	/** Map of sticky indices, keyed by Entity. Note that these indices
	 *  point to the sorted snap array, which may be different than the
	 *  unsorted array retrieved from the properties. */
	private HashMap<Entity, int[]> stickyIndexMap;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementUsesAbsoluteSnapsRule(
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

		// Check if entity is using absolute snaps, return false if not
		Boolean usesAbsoluteSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MOVEMENT_USES_ABSOLUTE_SNAPS_PROP);

		if(usesAbsoluteSnaps == null ||
				usesAbsoluteSnaps.booleanValue() == false){
            result.setResult(false);
            return(result);
		}

		// Set status bar message
		String usesAbsSnapMsg = intl_mgr.getString(SNAP_MOVE_PROP);
		statusBar.setMessage(usesAbsSnapMsg);

		// Get the absolute snap values
		float[] xAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_X_AXIS_SNAP_PROP);

		// clone them since they will be sorted in place later
		if (xAxisSnaps != null) {
			int num = xAxisSnaps.length;
			float[] tmp = new float[num];
			for (int i = 0; i < num; i++) {
				tmp[i] = xAxisSnaps[i];
			}
			xAxisSnaps = tmp;
		}

		float[] yAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Y_AXIS_SNAP_PROP);

		if (yAxisSnaps != null) {
			int num = yAxisSnaps.length;
			float[] tmp = new float[num];
			for (int i = 0; i < num; i++) {
				tmp[i] = yAxisSnaps[i];
			}
			yAxisSnaps = tmp;
		}

		float[] zAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Z_AXIS_SNAP_PROP);

		if (zAxisSnaps != null) {
			int num = zAxisSnaps.length;
			float[] tmp = new float[num];
			for (int i = 0; i < num; i++) {
				tmp[i] = zAxisSnaps[i];
			}
			zAxisSnaps = tmp;
		}

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
		double[] position = new double[] {0.0, 0.0, 0.0};

		// Perform operations depending on if command is transient
		if(command instanceof MoveEntityCommand){

			((MoveEntityCommand)command).getEndPosition(position);

			//SetRelativePositionUtility.setRelativePosition(command, model, position, rch);

			if (stickySnapIndex == null) {
				applyAbsoluteSnaps(
				        entity, 
				        position,
				        xAxisSnaps,
				        yAxisSnaps,
				        zAxisSnaps);
			} else {
				applyStickySnaps(
					entity,
					position,
					xAxisSnaps,
					yAxisSnaps,
					zAxisSnaps,
					stickySnapIndex);
			}

			((MoveEntityCommand)command).setEndPosition(position);

		} else if (command instanceof TransitionEntityChildCommand){

			if (!command.isTransient()) {

                /*
                 * Create the temp command to send to setRelativePosition
                 */
                double[] startPos = new double[3];
                float[] startRot = new float[4];

                // get the position data
                ((TransitionEntityChildCommand)command).getStartPosition(startPos);
                ((TransitionEntityChildCommand)command).getStartingRotation(startRot);
                ((TransitionEntityChildCommand)command).getEndPosition(position);
                Entity parentEntity =
                	((TransitionEntityChildCommand)command).getEndParentEntity();

                TransitionEntityChildCommand tmpCmd =
                    new TransitionEntityChildCommand(
                            model,
                            (PositionableEntity)((TransitionEntityChildCommand)command).getEntity(),
                            ((TransitionEntityChildCommand)command).getStartParentEntity(),
                            startPos,
                            startRot,
                            parentEntity,
                            position,
                            new float[4],
                            command.isTransient());

                //SetRelativePositionUtility.setRelativePosition(tmpCmd, model, position, rch);

	            // apply the snaps
                if (stickySnapIndex == null) {
					applyAbsoluteSnaps(
					        entity, 
    						position,
    						xAxisSnaps,
    						yAxisSnaps,
    						zAxisSnaps);
				} else {
					applyStickySnaps(
						entity,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						stickySnapIndex);
				}

	            // now actually set the modified position
	            ((TransitionEntityChildCommand)command).setEndPosition(position);

		    }

		} else if(command instanceof MoveEntityTransientCommand){
		    
            // Check if entity is snap to model only
            Boolean snapToModel = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.SNAP_TO_MODEL_ONLY);
            
            // Check if entity is snap to floor
            Boolean snapToFloor = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.SNAP_RELATIVE_TO_FLOOR);
	        
	        if (snapToModel) {
	            
	            rch.performCollisionCheck(command, true, true, false);

	            rch.performCollisionAnalysisHelper(entity, null, false, null, false);

	            restrictCollisionsToParents(entity);

	            /*
	             * Since we can't be certain the parent is correct for this
	             * command case, look to the current collisions.
	             */
	            if (rch.collisionEntities.size() > 0) {
	                ((MoveEntityTransientCommand)command).getPosition(position);

	                Entity tmpE = rch.collisionEntities.get(0);

	                double[] endParentPos = TransformUtils.getRelativePosition(
	                    model,
	                    tmpE,
	                    SceneHierarchyUtility.findZoneEntity(model, tmpE),
	                    false);

	                position[0] = position[0] - endParentPos[0];
	                position[1] = position[1] - endParentPos[1];
	                position[2] = position[2] - endParentPos[2];

	                /*
	                 * Side pocket the current parent id to set back later
	                 */
	                int parentEntityIDTT = entity.getParentEntityID();
	                entity.setParentEntityID(tmpE.getEntityID());

	                //SetRelativePositionUtility.setRelativePosition(command, model, position, rch);

	                entity.setParentEntityID(parentEntityIDTT);

	                if (stickySnapIndex == null) {
	                    applyAbsoluteSnaps(
	                            entity, 
	                            position,
	                            xAxisSnaps,
	                            yAxisSnaps,
	                            zAxisSnaps);
	                } else {
	                    applyStickySnaps(
	                        entity,
	                        position,
	                        xAxisSnaps,
	                        yAxisSnaps,
	                        zAxisSnaps,
	                        stickySnapIndex);
	                }

	                position[0] = endParentPos[0] + position[0];
	                position[1] = endParentPos[1] + position[1];
	                position[2] = endParentPos[2] + position[2];

	                ((MoveEntityTransientCommand)command).setPosition(position);

	            } else {
	                //////////////////////////////////////////////////////////////
	                // rem: seems to fix #571 ?????
	                /*
	                ((MoveEntityTransientCommand)command).getPosition(position);

	                setRelativePosition(command, model, position);

	                if (stickySnapIndex == null) {
	                    applyAbsoluteSnaps(
	                        position,
	                        xAxisSnaps,
	                        yAxisSnaps,
	                        zAxisSnaps);
	                } else {
	                    applyStickySnaps(
	                        entity,
	                        position,
	                        xAxisSnaps,
	                        yAxisSnaps,
	                        zAxisSnaps,
	                        stickySnapIndex);
	                }

	                ((MoveEntityTransientCommand)command).setPosition(position);
	                */
	                result.setResult(false);
	                return(result);
	                //////////////////////////////////////////////////////////////
	            }
	            
	        } else if (snapToFloor) {
	            
	            ((MoveEntityTransientCommand)command).getPosition(position);

	            //SetRelativePositionUtility.setRelativePosition(command, model, position, rch);

	            if (stickySnapIndex == null) {
	                applyAbsoluteSnaps(
	                        entity, 
	                        position,
	                        xAxisSnaps,
	                        yAxisSnaps,
	                        zAxisSnaps);
	            } else {
	                applyStickySnaps(
	                    entity,
	                    position,
	                    xAxisSnaps,
	                    yAxisSnaps,
	                    zAxisSnaps,
	                    stickySnapIndex);
	            }

	            ((MoveEntityTransientCommand)command).setPosition(position);

	        }

		} else if (command instanceof AddEntityCommand){

			((AddEntityCommand)command).getPosition(position);

			//SetRelativePositionUtility.setRelativePosition(command, model, position, rch);

			if (stickySnapIndex == null) {
				applyAbsoluteSnaps(
				        entity, 
    					position,
    					xAxisSnaps,
    					yAxisSnaps,
    					zAxisSnaps);
			} else {
				applyStickySnaps(
					entity,
					position,
					xAxisSnaps,
					yAxisSnaps,
					zAxisSnaps,
					stickySnapIndex);
			}

			((AddEntityCommand)command).setPosition(position);

		} else if (command instanceof AddEntityChildCommand){

			PositionableEntity childEntity = (PositionableEntity)
				((AddEntityChildCommand)command).getEntity();

			childEntity.getPosition(position);

			//SetRelativePositionUtility.setRelativePosition(command, model, position, rch);

			if (stickySnapIndex == null) {
				applyAbsoluteSnaps(
				        entity, 
    					position,
    					xAxisSnaps,
    					yAxisSnaps,
    					zAxisSnaps);
			} else {
				applyStickySnaps(
					entity,
					position,
					xAxisSnaps,
					yAxisSnaps,
					zAxisSnaps,
					stickySnapIndex);
			}

			childEntity.setPosition(position, command.isTransient());

		} else if(command instanceof AddEntityChildTransientCommand){

			PositionableEntity childEntity = (PositionableEntity)
				((AddEntityChildTransientCommand)command).getEntity();

			childEntity.getPosition(position);

			//SetRelativePositionUtility.setRelativePosition(command, model, position, rch);

			if (stickySnapIndex == null) {
				applyAbsoluteSnaps(
				        entity, 
    					position,
    					xAxisSnaps,
    					yAxisSnaps,
    					zAxisSnaps);
			} else {
				applyStickySnaps(
					entity,
					position,
					xAxisSnaps,
					yAxisSnaps,
					zAxisSnaps,
					stickySnapIndex);
			}

			childEntity.setPosition(position, command.isTransient());
		}

        result.setResult(false);
        return(result);
	}

	/**
	 * Process each position index for the snap value it should inherit.
	 *
	 * @param position Current position coordinate to evaluate
	 * @param xAxisSnaps X axis snap values
	 * @param yAxisSnaps Y axis snap values
	 * @param zAxisSnaps Z axis snap values
	 */
	private void applyAbsoluteSnaps(
	        Entity entity, 
			double[] position,
			float[] xAxisSnaps,
			float[] yAxisSnaps,
			float[] zAxisSnaps){
	    
		if (xAxisSnaps != null) {
		    
	        // Get the buffer value and add it to the result
            Float horizontalBuffer = 
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.CENTER_HORIZONTAL_POS_BUFF_PROP);     

            for (int i = 0; i < xAxisSnaps.length; i++) {
                xAxisSnaps[i] += horizontalBuffer;
            }
            
			position[0] = 
			    (float) RuleUtils.findClosestValue(position[0], xAxisSnaps);

		}

		if (yAxisSnaps != null) {
		    
            // Get the buffer value   
            Float verticalBuffer = 
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.CENTER_VERTICAL_POS_BUFF_PROP);

            for (int i = 0; i < yAxisSnaps.length; i++) {
                yAxisSnaps[i] += verticalBuffer;
            }

            position[1] = 
			    (float) RuleUtils.findClosestValue(position[1], yAxisSnaps);
			
		}

		if (zAxisSnaps != null) {
		    
            // Get the buffer value    
            Float depthBuffer = 
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.CENTER_DEPTH_POS_BUFF_PROP);
		    
            for (int i = 0; i < zAxisSnaps.length; i++) {
                zAxisSnaps[i] += depthBuffer;
            }

            position[2] = 
			    (float) RuleUtils.findClosestValue(position[2], zAxisSnaps);
			
		}
	}

	/**
	 * Process each position index for the snap value it should inherit.
	 *
	 * @param entity The Entity being positioned
	 * @param position Current position coordinate to evaluate
	 * @param xAxisSnaps X axis snap values
	 * @param yAxisSnaps Y axis snap values
	 * @param zAxisSnaps Z axis snap values
	 * @param stickySnapIndex The indices of the sticky snaps.
	 */
	private void applyStickySnaps(
			Entity entity,
			double[] position,
			float[] xAxisSnaps,
			float[] yAxisSnaps,
			float[] zAxisSnaps,
			int[] stickySnapIndex){

		int[] sindex = stickyIndexMap.get(entity);
		if (sindex == null) {
			sindex = new int[]{-1, -1, -1};
			stickyIndexMap.put(entity, sindex);
		}
		if (xAxisSnaps != null) {
			position[0] = findStickySnapValue(position[0], xAxisSnaps, stickySnapIndex, 0, sindex);
		}

		if (yAxisSnaps != null) {
			position[1] = findStickySnapValue(position[1], yAxisSnaps, stickySnapIndex, 1, sindex);
		}

		if (zAxisSnaps != null) {
			position[2] = findStickySnapValue(position[2], zAxisSnaps, stickySnapIndex, 2, sindex);
		}
	}

	/**
	 * Find the sticky snap value for the current position.
	 *
	 * @param value Current position along an axis to be evaluated
	 * @param snaps Associated array of same axis snap values to compare with
	 * @param stickySnapIndex The indices of the sticky snaps within the snaps array.
	 * @param axis Index of the axis being processed (0 = X, 1 = Y, 2 = Z)
	 * @param sindex The snap index values that have already been assigned
	 * @return The sticky snap value
	 */
	private double findStickySnapValue(
		double value,
		float[] snaps,
		int[] stickySnapIndex,
		int axis,
		int[] sindex){

		// create a boolean array to identify the sticky snaps
		int num_snaps = snaps.length;
		boolean[] sticky = new boolean[num_snaps];
		for (int i = 0; i < stickySnapIndex.length; i++) {
			int idx = stickySnapIndex[i];
			if ((idx >= 0) && (idx < num_snaps)) {
				sticky[idx] = true;
			}
		}
		// sort the values, along with the sticky flags
		for (int i = 0; i < num_snaps; i++) {
			for (int j = i; j > 0 && snaps[j-1] > snaps[j]; j--) {
				float t_i = snaps[j];
				snaps[j] = snaps[j-1];
				snaps[j-1] = t_i;

				boolean t_b = sticky[j];
				sticky[j] = sticky[j-1];
				sticky[j-1] = t_b;
			}
		}
		int current_snap_idx = 0;

		// find the snap value that is closest to the current position
		for (int i = 1; i < snaps.length; i++) {
			 if (value > ((snaps[i] - snaps[i-1]) * 0.5f + snaps[i-1])){
				 current_snap_idx = i;
			 } else {
				 break;
			 }
		}

		boolean useCurrent = false;
		int previous_snap_idx = sindex[axis];
		if (previous_snap_idx == -1) {
			// no snap position was previously set, use the current
			useCurrent = true;
		} else if (previous_snap_idx == current_snap_idx) {
			// the previous is still the closest
			useCurrent = false;
		} else if (!sticky[previous_snap_idx]) {
			// the previous snap position is NOT sticky, use the closest current
			useCurrent = true;
		} else {
			// the previous snap position IS sticky and is different from
			// the current closest - decide what to do
			if (current_snap_idx > previous_snap_idx) {
				// the new current snap position is in the positive direction
				if (current_snap_idx > previous_snap_idx + 1) {
					// gone beyond a single snap position
					useCurrent = true;
				} else {
					// switch to the current when current value passes the next snap
					if (value < snaps[current_snap_idx]) {
						useCurrent = false;
					} else {
						useCurrent = true;
					}
				}
			} else {
				// the new current snap position is in the negative direction
				if (current_snap_idx < previous_snap_idx - 1) {
					// gone beyond a single snap position
					useCurrent = true;
				} else {
					// switch to the current when current value passes the next snap
					if (value > snaps[current_snap_idx]) {
						useCurrent = false;
					} else {
						useCurrent = true;
					}
				}
			}
		}

		double rvalue;
		if (useCurrent) {
			rvalue = snaps[current_snap_idx];
			sindex[axis] = current_snap_idx;
		} else {
			rvalue = snaps[previous_snap_idx];
		}

		return(rvalue);
	}

	/**
	 * Filter the collision results to only contain legal
	 * parents of the specified entity.
	 *
	 * @param entity The Entity to filter parents for.
	 */
	private void restrictCollisionsToParents(Entity entity) {

		String[] allowedParentClassifications = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

		if (allowedParentClassifications != null) {

			int num = rch.collisionEntities.size();
			for (int i = num - 1; i >= 0; i--) {

				Entity collisionEntity = rch.collisionEntities.get(i);

				String[] collisionEntityClassifications = (String[])
					RulePropertyAccessor.getRulePropertyValue(
						collisionEntity,
						ChefX3DRuleProperties.CLASSIFICATION_PROP);

				if (collisionEntityClassifications != null) {

					boolean couldBeParent = false;

					for (int j = 0; j < collisionEntityClassifications.length; j++) {

						for (int k = 0; k < allowedParentClassifications.length; k++) {

							if (collisionEntityClassifications[j].equalsIgnoreCase(
								allowedParentClassifications[k])) {

								couldBeParent = true;
								break;
							}
						}

						if (couldBeParent) {
							break;
						}
					}
					if (!couldBeParent) {
						rch.collisionEntities.remove(i);
					}
				}
			}
		}
	}
}
