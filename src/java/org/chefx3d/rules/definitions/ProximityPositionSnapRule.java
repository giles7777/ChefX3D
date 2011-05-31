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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.Preferences;

import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.preferences.SessionPreferenceConstants;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.PROX_SNAP_HEIGHT_OPT;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.PROX_SNAP_STYLE;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Handles proximity snapping between specified classifications. Uses absolute
 * snaps or incremental snaps exclusively, checking in that order for existence
 * , to provide snap positions. If within tolerance of that snap offset, then
 * position will be adjusted to that position.
 *
 * @author Ben Yarger
 * @version $Revision: 1.27 $
 */
public class ProximityPositionSnapRule extends BaseRule  {

	/** The preferences accessor used to query session values. */
	private Preferences prefs;

	/** Direction to perform nearest neighbor measurement in. */
	private int measurementDirection;

	/** The specified snap style for proximity snapping. */
	private PROX_SNAP_STYLE snapStyle;

	/** +/- range considered equal to the angle amounts */
	private static final double ANGLE_THRESHOLD = 0.0001;

	/** Radian equivalent of 90 degrees. */
	private static final double NINETY_RAD = Math.toRadians(90.0);

	/** Radian equivalent of 135 degrees. */
	private static final double ONE_THIRTY_FIVE_RAD = Math.toRadians(135.0);

	/** Static threshold for height snapping */
	private static final double HEIGHT_THRESHOLD = 0.125;

	/** Axis specifiers for analysis. */
	private static enum AXIS {X, Y, Z};
	
	/** 
	 * Value to multiply the threshold related to zoom by to increase or 
	 * decrease the snap field around the entity. > 1 to increase < 1 to 
	 * decrease.
	 */
	private static double THRESHOLD_ADJ_FACTOR = 0.25;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ProximityPositionSnapRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        prefs = Preferences.userRoot().node(
        		SessionPreferenceConstants.SESSION_PREFERENCES_NODE);

        measurementDirection =
        	SessionPreferenceConstants.DEFAULT_MEASUREMENT_DIRECTION;

        ruleType = RULE_TYPE.INVIOLABLE;
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

		// Check if entity is using snap to class and if so, this is our
		// indicator that the proximity snap should be applied.
		String proximitySnapClass = (String)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SNAP_TO_CLASS_PROP);

		if(proximitySnapClass == null){

            result.setResult(true);
            return(result);
		}

		// Set the evaluation values that we need from session preferences.
		measurementDirection = prefs.getInt(
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_KEY,
				SessionPreferenceConstants.DEFAULT_MEASUREMENT_DIRECTION);

		snapStyle = (PROX_SNAP_STYLE)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PROX_SNAP_STYLE_PROP);

		// Run the proximity snap process, and get the result
		boolean applied = processProximitySnaps(
				proximitySnapClass,
				model,
				entity,
				command);

		// If proximity snaps were applied, set the selection box color to
		// the INFORMATION level color.
		if (applied) {
			result.setStatusValue(ELEVATION_LEVEL.INFORMATION);
		}

        result.setResult(true);
        return(result);
	}

    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

    /**
     * Call the appropriate snap procedure based on which type of snap data is
     * being used.
     *
     * @param proximitySnapClass Classification to snap to
     * @param model WorldModel to reference
     * @param entity Entity to apply proximity snap to
     * @param command Command acting on the entity
     * @return True if proximity snap was applied, false otherwise
     */
    private boolean processProximitySnaps(
    		String proximitySnapClass,
			WorldModel model,
			Entity entity,
			Command command) {

    	boolean applied = false;

    	// Determine if we are doing using absolute or incremental snaps as the
    	// basis for applying proximity snaps. Return the result of the
    	// application of proximity snaps.

    	// Check if entity is using absolute snaps
		Boolean usesAbsoluteSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_USES_ABSOLUTE_SNAPS_PROP);


		if(usesAbsoluteSnaps != null &&
				usesAbsoluteSnaps.booleanValue() == true){

			 applied = applyProximityAbsoluteSnap(
					proximitySnapClass,
					model,
					entity,
					command);
		}


		// Check if entity is using incremental snaps
		Boolean usesIncrementalSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_USES_INCREMENTAL_SNAPS_PROP);

		if(usesIncrementalSnaps != null &&
				usesIncrementalSnaps.booleanValue() == true){

			applied = applyProximityIncremenalSnap(
					proximitySnapClass,
					model,
					entity,
					command);
		}

		return applied;
    }

	/**
	 * Breaks apart command into required parts and hands off for position
	 * snap processing based on extracted data and absolute snap positions.
	 *
	 * @param proximitySnapClass String classification name to look for
	 * @param model WorldModel
	 * @param entity Entity to update with snap position data
	 * @param command Command to extract position data from
	 * @return True if applied, false otherwise
	 */
	private boolean applyProximityAbsoluteSnap(
			String proximitySnapClass,
			WorldModel model,
			Entity entity,
			Command command){

		// Get the absolute snap values
		float[] xAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_X_AXIS_SNAP_PROP);

		float[] yAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Y_AXIS_SNAP_PROP);

		float[] zAxisSnaps = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Z_AXIS_SNAP_PROP);

		// Sort in ascending order if data exists
		if (xAxisSnaps != null) {
			Arrays.sort(xAxisSnaps);
		}

		if (yAxisSnaps != null) {
			Arrays.sort(yAxisSnaps);
		}

		if (zAxisSnaps != null) {
			Arrays.sort(zAxisSnaps);
		}

		return processSnapsByCommand(
				model,
				command,
				entity,
				proximitySnapClass,
				false,
				xAxisSnaps,
				yAxisSnaps,
				zAxisSnaps);
	}

	/**
	 * Breaks apart command into required parts and hands off for position
	 * snap processing based on extracted data and incremental snap positions.
	 *
	 * @param proximitySnapClass String classification name to look for
	 * @param model WorldModel
	 * @param entity Entity to update with snap position data
	 * @param command Command to extract position data from
	 * @return True if applied, false otherwise
	 */
	private boolean applyProximityIncremenalSnap(
			String proximitySnapClass,
			WorldModel model,
			Entity entity,
			Command command){

		// Get the incremental snap values
		Float xAxisInc = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_X_AXIS_SNAP_PROP);

		Float yAxisInc = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Y_AXIS_SNAP_PROP);

		Float zAxisInc = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Z_AXIS_SNAP_PROP);

		float[] xAxisSnaps;
		if (xAxisInc == null) {
			xAxisSnaps = null;
		} else {
			xAxisSnaps = new float[] {xAxisInc};
		}

		float[] yAxisSnaps;
		if (yAxisInc == null) {
			yAxisSnaps = null;
		} else {
			yAxisSnaps = new float[] {yAxisInc};
		}

		float[] zAxisSnaps;
		if (zAxisInc == null) {
			zAxisSnaps = null;
		} else {
			zAxisSnaps = new float[] {zAxisInc};
		}

		return processSnapsByCommand(
				model,
				command,
				entity,
				proximitySnapClass,
				true,
				xAxisSnaps,
				yAxisSnaps,
				zAxisSnaps);
	}

	/**
	 * Process each command, applying the snap if there were adjustments in
	 * accordance with the command type. The bounds adjustment used in the
	 * nearest neighbor adjustment is also calculated here. Note that the
	 * first axis with snap values encountered is the only axis evaluated.
	 * Proximity snaps are applied in only one axis.
	 *
	 * @param model WorldModel to reference
	 * @param command Command to apply adjustment to
	 * @param entity Entity affected by the command
	 * @param proximitySnapClass Proximity snap classification name
	 * @param useIncrementalSnaps True to use incremental snaps, false to use
	 * absolute snaps
	 * @param xAxisSnaps X axis snap values to use, null to not apply
	 * @param yAxisSnaps Y axis snap values to use, null to not apply
	 * @param zAxisSnaps Z axis snap values to use, null to not apply
	 * @return
	 */
	private boolean processSnapsByCommand(
			WorldModel model,
			Command command,
			Entity entity,
			String proximitySnapClass,
			boolean useIncrementalSnaps,
			float[] xAxisSnaps,
			float[] yAxisSnaps,
			float[] zAxisSnaps) {

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		double[] position = new double[] {0.0, 0.0, 0.0};

		Float xAxisSnap = null;
		Float yAxisSnap = null;
		Float zAxisSnap = null;

		// If we are using incremental snaps, extract the incremental snap
		// data.
		if (useIncrementalSnaps) {

			if (xAxisSnaps == null) {
				xAxisSnap = null;
			} else {
				xAxisSnap = xAxisSnaps[0];
			}

			if (yAxisSnaps == null) {
				yAxisSnap = null;
			} else {
				yAxisSnap = yAxisSnaps[0];
			}

			if (zAxisSnaps == null) {
				zAxisSnap = null;
			} else {
				zAxisSnap = zAxisSnaps[0];
			}
		}

		// Establish the bounds adjustment to apply in the nearest neighbor
		// checks performed later. We will set this to the
		PositionableEntity entityParentZone = (PositionableEntity)
			SceneHierarchyUtility.findExactZoneEntity(model, entity);

		float[] bounds = new float[6];
		float[] boundsAdj = new float[] {0.0f, 0.0f, 0.0f};

		if (entityParentZone.getType() == Entity.TYPE_SEGMENT) {

			((SegmentEntity)entityParentZone).getLocalBounds(bounds);

		} else if (entityParentZone.getType() == Entity.TYPE_GROUNDPLANE_ZONE) {

			// TODO: may way to reign this in a bit because floor bounds are
			// extreme!
			entityParentZone.getBounds(bounds);

		} else {

			entityParentZone.getBounds(bounds);
		}

		// Apply bounds adjustment to correct axis based on snap positions
		// provided
		if (xAxisSnaps != null) {
			boundsAdj[0] = bounds[1] - bounds[0];
		} else if (yAxisSnaps != null) {
			boundsAdj[1] = bounds[3] - bounds[2];
		} else if (zAxisSnaps == null) {
			boundsAdj[2] = bounds[5] - bounds[4];
		}

		boolean result = false;

		// Perform operations depending on if command is transient
		if(command instanceof MoveEntityCommand){

			((MoveEntityCommand)command).getEndPosition(position);

			if (useIncrementalSnaps) {
				result = applyIncrementalSnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnap,
						yAxisSnap,
						zAxisSnap,
						boundsAdj);
			} else {
				result = applyAbsoluteProximitySnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						boundsAdj);
			}

			if (result) {
				((MoveEntityCommand)command).setEndPosition(position);
			}

		} else if (command instanceof TransitionEntityChildCommand){

			((TransitionEntityChildCommand)command).getEndPosition(
					position);

			if (useIncrementalSnaps) {
				result = applyIncrementalSnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnap,
						yAxisSnap,
						zAxisSnap,
						boundsAdj);
			} else {
				result = applyAbsoluteProximitySnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						boundsAdj);
			}

			if (result) {
				((TransitionEntityChildCommand)command).setEndPosition(
						position);
			}

		} else if(command instanceof MoveEntityTransientCommand){

			((MoveEntityTransientCommand)command).getPosition(position);

			if (useIncrementalSnaps) {
				result = applyIncrementalSnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnap,
						yAxisSnap,
						zAxisSnap,
						boundsAdj);
			} else {
				result = applyAbsoluteProximitySnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						boundsAdj);
			}

			if (result) {
				((MoveEntityTransientCommand)command).setPosition(position);
			}

		} else if (command instanceof AddEntityCommand){

			((AddEntityCommand)command).getPosition(position);

			if (useIncrementalSnaps) {
				result = applyIncrementalSnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnap,
						yAxisSnap,
						zAxisSnap,
						boundsAdj);
			} else {
				result = applyAbsoluteProximitySnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						boundsAdj);
			}

			if (result) {
				((AddEntityCommand)command).setPosition(position);
			}

		} else if (command instanceof AddEntityChildCommand){

			PositionableEntity childEntity = (PositionableEntity)
				((AddEntityChildCommand)command).getEntity();

			childEntity.getPosition(position);

			if (useIncrementalSnaps) {
				result = applyIncrementalSnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnap,
						yAxisSnap,
						zAxisSnap,
						boundsAdj);
			} else {
				result = applyAbsoluteProximitySnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						boundsAdj);
			}

			if (result) {
				childEntity.setPosition(position, command.isTransient());
			}

		} else if(command instanceof AddEntityChildTransientCommand){

			PositionableEntity childEntity = (PositionableEntity)
				((AddEntityChildTransientCommand)command).getEntity();

			childEntity.getPosition(position);

			if (useIncrementalSnaps) {
				result = applyIncrementalSnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnap,
						yAxisSnap,
						zAxisSnap,
						boundsAdj);
			} else {
				result = applyAbsoluteProximitySnaps(
						model,
						entity,
						proximitySnapClass,
						position,
						xAxisSnaps,
						yAxisSnaps,
						zAxisSnaps,
						boundsAdj);
			}

			if (result) {
				childEntity.setPosition(position, command.isTransient());
			}
		}

		return result;
	}

	/**
	 * Apply absolute snaps based on proximity to other classication products.
	 * If within threshold of absolute position, snap will be made. Will make
	 * the snap to the shortest distance within threshold amongst all possible
	 * options. At this time, z axis is not evaluated. Assumes snaps are
	 * provided in ascending order.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity to adjust position for
	 * @param snapClassification Classification to look for to snap to
	 * @param position double[] xyz position to apply snap to, contains snap
	 * position applied if position is changed because of snap.
	 * @param xAxisSnaps float[] absolute x axis snap offsets
	 * @param yAxisSnaps float[] absolute y axis snap offsets
	 * @param zAxisSnaps float[] absolute z axis snap offsets
	 * @param boundsAdj float[] x,y,z bounds adj for nearest neighbor check
	 * @return True if proximity snap was applied, false otherwise
	 */
	private boolean applyAbsoluteProximitySnaps(
			WorldModel model,
			Entity entity,
			String snapClassification,
			double[] position,
			float[] xAxisSnaps,
			float[] yAxisSnaps,
			float[] zAxisSnaps,
			float[] boundsAdj){

		// Calculate the threshold based on the zoom amount
		double threshold = 
			RuleUtils.getZoomThreshold(view, null) * THRESHOLD_ADJ_FACTOR;

		double[] startingPosition = new double[3];
		((PositionableEntity)entity).getPosition(startingPosition);
		((PositionableEntity)entity).setPosition(position, true);

		// Copy of the incoming position data that should never change
		// so updated positions can be calculated relative to it.
		double[] positionReference = new double[] {
				position[0],
				position[1],
				position[2]};

		boolean applied = false;
		AXIS axis = AXIS.Z;
		Entity targetSidePocket = null;
		
		float[] entityBounds = 
			BoundsUtils.getBounds((PositionableEntity) entity, true);
		
		// This will end up holding the closest edge point of the nearest 
		// target in the direction of analysis. In effect it is the center
		// plus oriented bounds edge closest to our entity.
		double[] closestNeighborAccessPoint = new double[3];

		// Tackle x axis snaps first. Base analysis on the direction of
		// evaluation.
		if(xAxisSnaps != null){

			axis = AXIS.X;
			double shortestXDistance = Double.MAX_VALUE;

			if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_NEGATIVE) {

				ArrayList<Entity> negXNeighbors =
					rch.getNegativeXNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);				
				
				// Sort the resulting negXNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
				
				negXNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							negXNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.XAXIS, 
							true);
				
				if (negXNeighbors == null) {
					return false;
				}
				
				// Grab the closest neighbor in the list and try to snap to it
				if (negXNeighbors.size() > 0) {
					
					Entity negXNeighbor = negXNeighbors.get(0);
					
					double distance[] =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) negXNeighbor,
								axis,
								closestNeighborAccessPoint);
	
					if (distance == null) {
						return false;
					}
	
					for(int i = 0; i < xAxisSnaps.length; i++){
	
						double snapDifference = xAxisSnaps[i] + distance[0];
	
						if(snapDifference <	threshold &&
								snapDifference > -threshold){
	
							if(Math.abs(shortestXDistance) >
								Math.abs(snapDifference)){
	
								if (snapStyle == PROX_SNAP_STYLE.EDGE) {
									
									position[0] =
										closestNeighborAccessPoint[0] + 
										xAxisSnaps[i] - entityBounds[0];
	
								} else {
									
									double[] tmpRelPos = 
										TransformUtils.getExactRelativePosition(
											model, 
											(PositionableEntity) negXNeighbor, 
											(PositionableEntity) entity, 
											false);
									
									position[0] = tmpRelPos[0] + xAxisSnaps[i];
								}
								
								shortestXDistance = snapDifference;
								targetSidePocket = negXNeighbor;
	
								applied = true;
							}
						}
					}
				}
				
			} else if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_POSITIVE) {

				ArrayList<Entity> posXNeighbors =
					rch.getPositiveXNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);

				// Sort the resulting posXNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
				
				posXNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							posXNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.XAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (posXNeighbors.size() > 0) {
					
					Entity posXNeighbor = 
						posXNeighbors.get(posXNeighbors.size()-1);

					double distance[] =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) posXNeighbor,
								axis,
								closestNeighborAccessPoint);

					if (distance == null) {
						return false;
					}

					for(int i = 0; i < xAxisSnaps.length; i++){

						double snapDifference = xAxisSnaps[i] - distance[0];

						if(snapDifference <	threshold &&
								snapDifference > -threshold){

							if(Math.abs(shortestXDistance) >
								Math.abs(snapDifference)){

   								if (snapStyle == PROX_SNAP_STYLE.EDGE) {
									
									position[0] =
										closestNeighborAccessPoint[0] - 
										xAxisSnaps[i] - entityBounds[1];
	
								} else {
									
									double[] tmpRelPos = 
										TransformUtils.getExactRelativePosition(
											model, 
											(PositionableEntity) posXNeighbor, 
											(PositionableEntity) entity, 
											false);
									
									position[0] = tmpRelPos[0] - xAxisSnaps[i];
								}

								shortestXDistance = snapDifference;
								targetSidePocket = posXNeighbor;

								applied = true;
							}
						}
					}
				}
			}
		}

		// Tackle y axis snaps. Start with negative neighbors and then switch
		// to all positive neighbors.
		if(yAxisSnaps != null) {

			axis = AXIS.Y;
			double shortestYDistance = Double.MAX_VALUE;

			if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_NEGATIVE) {

				ArrayList<Entity> negYNeighbors =
					rch.getNegativeYNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);

				// Sort the resulting negYNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
				
				negYNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							negYNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.YAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (negYNeighbors.size() > 0) {
					
					Entity negYNeighbor = negYNeighbors.get(0);

					double distance[] =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) negYNeighbor,
								axis,
								closestNeighborAccessPoint);

					if (distance == null) {
						return false;
					}

					distance[1] = Math.abs(distance[1]);

					for(int i = 0; i < yAxisSnaps.length; i++){

						double snapDifference = yAxisSnaps[i] + distance[1];

						if(snapDifference <	threshold &&
								snapDifference > -threshold){

							if(Math.abs(shortestYDistance) >
								Math.abs(snapDifference)){

   								if (snapStyle == PROX_SNAP_STYLE.EDGE) {
									
									position[1] =
										closestNeighborAccessPoint[1] + 
										yAxisSnaps[i] - entityBounds[2];
	
								} else {
									
									double[] tmpRelPos = 
										TransformUtils.getExactRelativePosition(
											model, 
											(PositionableEntity) negYNeighbor, 
											(PositionableEntity) entity, 
											false);
									
									position[1] = tmpRelPos[1] + yAxisSnaps[i];
								}

								shortestYDistance = snapDifference;
								targetSidePocket = negYNeighbor;

								applied = true;
							}
						}
					}
				}
			} else if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_POSITIVE) {

				ArrayList<Entity> posYNeighbors =
					rch.getPositiveYNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);

				// Sort the resulting negYNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
				
				posYNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							posYNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.YAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (posYNeighbors.size() > 0) {
					
					Entity posYNeighbor = 
						posYNeighbors.get(posYNeighbors.size()-1);

					double distance[] =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) posYNeighbor,
								axis,
								closestNeighborAccessPoint);

					if (distance == null) {
						return false;
					}

					for(int i = 0; i < yAxisSnaps.length; i++){

						double snapDifference = yAxisSnaps[i] - distance[1];

						if(snapDifference <	threshold &&
								snapDifference > -threshold){

							if(Math.abs(shortestYDistance) >
								Math.abs(snapDifference)){

								if (snapStyle == PROX_SNAP_STYLE.EDGE) {
									
									position[1] =
										closestNeighborAccessPoint[1] - 
										yAxisSnaps[i] - entityBounds[3];
	
								} else {
									
									double[] tmpRelPos = 
										TransformUtils.getExactRelativePosition(
											model, 
											(PositionableEntity) posYNeighbor, 
											(PositionableEntity) entity, 
											false);
									
									position[1] = tmpRelPos[1] + yAxisSnaps[i];
								}

								shortestYDistance = snapDifference;
								targetSidePocket = posYNeighbor;

								applied = true;
							}
						}
					}
				}
			}
		}

		((PositionableEntity)entity).setPosition(startingPosition, true);

		// Snap to height only if we are adjusting along the x axis.
		if (applied && axis == AXIS.X) {
			snapToSameHeight(
					model,
					(PositionableEntity) entity,
					(PositionableEntity) targetSidePocket,
					position);
		}

		return applied;
	}

	/**
	 * Apply incremental snaps based on proximity to other classification
	 * products. If within threshold of incremental position, snap will occur.
	 * Will make the snap to the shortest distance within threshold amongst
	 * all possible incremental steps. At this time, z axis is not evaluated.
	 *
	 * @param model WorldModel
	 * @param entity Entity to adjust position for
	 * @param snapClassification Classification to look for to snap to
	 * @param position double[] xyz position to apply snap to
	 * @param xAxisSnaps Float incremental x axis snap offsets
	 * @param yAxisSnaps Float incremental y axis snap offsets
	 * @param zAxisSnaps Float incremental z axis snap offsets
	 * @return True if proximity snap was applied, false otherwise
	 */
	private boolean applyIncrementalSnaps(
			WorldModel model,
			Entity entity,
			String snapClassification,
			double[] position,
			Float xAxisSnap,
			Float yAxisSnap,
			Float zAxisSnap,
			float[] boundsAdj){

		// Calculate the threshold based on the zoom amount
		double threshold = 
			RuleUtils.getZoomThreshold(view, null) * THRESHOLD_ADJ_FACTOR;

		double[] startingPosition = new double[3];
		((PositionableEntity)entity).getPosition(startingPosition);
		((PositionableEntity)entity).setPosition(position, true);

		// Copy of the incoming position data that should never change
		// so updated positions can be calculated relative to it.
		double[] positionReference = new double[] {
				position[0],
				position[1],
				position[2]};

		boolean applied = false;
		AXIS axis = AXIS.Z;
		Entity targetSidePocket = null;
		
		float[] entityBounds = 
			BoundsUtils.getBounds((PositionableEntity) entity, true);
		
		// This will end up holding the closest edge point of the nearest 
		// target in the direction of analysis. In effect it is the center
		// plus oriented bounds edge closest to our entity.
		double[] closestNeighborAccessPoint = new double[3];

		//------------------------------------------------------------------
		// Tackle x axis snaps first. Start with negative neighbors and then
		// check all positive neighbors.
		//------------------------------------------------------------------
		if(xAxisSnap != null){

			axis = AXIS.X;
			double shortestXDistance = Double.MAX_VALUE;

			// Check the negative x direction
			if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_NEGATIVE) {

				ArrayList<Entity> negXNeighbors =
					rch.getNegativeXNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);
				
				// Sort the resulting negXNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
			
				negXNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							negXNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.XAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (negXNeighbors.size() > 0) {
					
					Entity negXNeighbor = negXNeighbors.get(0);
					
					double[] distance =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) negXNeighbor,
								axis,
								closestNeighborAccessPoint);
	
					if (distance == null) {
						return false;
					}
	
					int snapMultiplier =
						(int) ((distance[0] - xAxisSnap/2.0) / xAxisSnap);
					double remainder = xAxisSnap * snapMultiplier - distance[0];
	
					if(remainder < threshold && remainder > -threshold){
	
						if(Math.abs(shortestXDistance) > Math.abs(remainder)){
							
							if (snapStyle == PROX_SNAP_STYLE.EDGE) {
								
								position[0] =
									closestNeighborAccessPoint[0] + 
									snapMultiplier * xAxisSnap - 
									entityBounds[0];

							} else {
								
								double[] tmpRelPos = 
									TransformUtils.getExactRelativePosition(
										model, 
										(PositionableEntity) negXNeighbor, 
										(PositionableEntity) entity, 
										false);
								
								position[0] = 
									tmpRelPos[0] + snapMultiplier * xAxisSnap;
							}
	
							shortestXDistance = remainder;
							targetSidePocket = negXNeighbor;
	
							applied = true;
						}
					}
				}
				
			} else if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_POSITIVE) {

				ArrayList<Entity> posXNeighbors =
					rch.getPositiveXNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);
				
				// Sort the resulting posXNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
			
				posXNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							posXNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.XAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (posXNeighbors.size() > 0) {

					Entity posXNeighbor = 
						posXNeighbors.get(posXNeighbors.size()-1);
					
					double[] distance =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) posXNeighbor,
								axis,
								closestNeighborAccessPoint);

					if (distance == null) {
						return false;
					}

					int snapMultiplier =
						(int) ((distance[0] + xAxisSnap/2.0) / xAxisSnap);
					double remainder = xAxisSnap * snapMultiplier - distance[0];

					if(remainder < threshold && remainder > -threshold){

						if(Math.abs(shortestXDistance) > Math.abs(remainder)){
							
							if (snapStyle == PROX_SNAP_STYLE.EDGE) {
								
								position[0] =
									closestNeighborAccessPoint[0] - 
									snapMultiplier * xAxisSnap - 
									entityBounds[1];

							} else {
								
								double[] tmpRelPos = 
									TransformUtils.getExactRelativePosition(
										model, 
										(PositionableEntity) posXNeighbor, 
										(PositionableEntity) entity, 
										false);
								
								position[0] = 
									tmpRelPos[0] - snapMultiplier * xAxisSnap;
							}

							shortestXDistance = remainder;
							targetSidePocket = posXNeighbor;

							applied = true;
						}
					}
				}
			}
		}

		//-------------------------------------------------------------------
		// Tackle y axis snaps. Start with negative neighbors and then switch
		// to all positive neighbors.
		//-------------------------------------------------------------------
		if(yAxisSnap != null){

			axis = AXIS.Y;
			double shortestYDistance = Double.MAX_VALUE;

			if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_NEGATIVE) {

				ArrayList<Entity> negYNeighbors =
					rch.getNegativeYNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);

				// Sort the resulting negYNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
			
				negYNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							negYNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.YAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (negYNeighbors.size() > 0) {

					Entity negYNeighbor = negYNeighbors.get(0);

					double[] distance =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) negYNeighbor,
								axis,
								closestNeighborAccessPoint);

					if (distance == null) {
						return false;
					}

					int snapMultiplier =
						(int) ((distance[1] - yAxisSnap/2.0) / yAxisSnap);
					double remainder = yAxisSnap * snapMultiplier - distance[1];

					if(remainder < threshold && remainder > -threshold){

						if(Math.abs(shortestYDistance) > Math.abs(remainder)){
							
							if (snapStyle == PROX_SNAP_STYLE.EDGE) {
								
								position[1] =
									closestNeighborAccessPoint[1] + 
									snapMultiplier * yAxisSnap - 
									entityBounds[2];

							} else {
								
								double[] tmpRelPos = 
									TransformUtils.getExactRelativePosition(
										model, 
										(PositionableEntity) negYNeighbor, 
										(PositionableEntity) entity, 
										false);
								
								position[1] = 
									tmpRelPos[1] + snapMultiplier * yAxisSnap;
							}

							shortestYDistance = remainder;
							targetSidePocket = negYNeighbor;

							applied = true;
						}
					}
				}
			} else if (measurementDirection ==
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_POSITIVE) {

				ArrayList<Entity> posYNeighbors =
					rch.getPositiveYNeighbors(
							(PositionableEntity)entity,
							snapClassification,
							boundsAdj);

				// Sort the resulting negXNeighbors
				PositionableEntity activeZone = (PositionableEntity) 
				SceneHierarchyUtility.getActiveZoneEntity(model);
			
				posYNeighbors = 
					TransformUtils.sortDescendingRelativePosValueOrder(
							model, 
							posYNeighbors, 
							activeZone, 
							TARGET_ADJUSTMENT_AXIS.YAXIS, 
							true);
				
				// Grab the closest neighbor in the list and try to snap to it
				if (posYNeighbors.size() > 0) {

					Entity posYNeighbor = 
						posYNeighbors.get(posYNeighbors.size()-1);

					double[] distance =
						calculateDistanceBetweenEntities(
								(PositionableEntity) entity,
								(PositionableEntity) posYNeighbor,
								axis,
								closestNeighborAccessPoint);

					if (distance == null) {
						return false;
					}

					int snapMultiplier =
						(int) ((distance[1] + yAxisSnap/2.0) / yAxisSnap);
					double remainder = yAxisSnap * snapMultiplier - distance[1];

					if(remainder < threshold && remainder > -threshold){

						if(Math.abs(shortestYDistance) > Math.abs(remainder)){
							
							if (snapStyle == PROX_SNAP_STYLE.EDGE) {
								
								position[1] =
									closestNeighborAccessPoint[1] + 
									snapMultiplier * yAxisSnap - 
									entityBounds[3];

							} else {
								
								double[] tmpRelPos = 
									TransformUtils.getExactRelativePosition(
										model, 
										(PositionableEntity) posYNeighbor, 
										(PositionableEntity) entity, 
										false);
								
								position[1] = 
									tmpRelPos[1] + snapMultiplier * yAxisSnap;
							}

							shortestYDistance = remainder;
							targetSidePocket = posYNeighbor;

							applied = true;
						}
					}
				}
			}
		}

		((PositionableEntity)entity).setPosition(startingPosition, true);

		// Snap to height
		if (applied && axis == AXIS.X) {
			snapToSameHeight(
					model,
					(PositionableEntity) entity,
					(PositionableEntity) targetSidePocket,
					position);
		}

		return applied;
	}

	/**
	 * Similar to TransformUtils.getDistanceBetweenEntities() with the exception
	 *  that separate parent zone entities are allowed under special
	 * circumstances. If the parents are two segments and are 90 or 135 degrees
	 * apart, then we will permit the distance check to occur with the distance
	 * result calculated correctly for the angle case. Measured neighbor -
	 * movingEntity. That is the only exception however. Result will be either
	 * center-to-center or edge-to-edge depending on what is specified
	 * by the entity.
	 *
	 * @param movingEntity Entity being moved
	 * @param neighbor Neighbor entity to check against
	 * @param axis AXIS enum specifying which axis snaps are applied in
	 * @param closestNeighborAxisPoint Updated with the point on the neighbor
	 * closest to the movingEntity that is used in the distance calculation. 
	 * Value will be in coordinates relative to movingEntity's parent.
	 * @return Distance between entities, or null if unable to calculate
	 */
	private double[] calculateDistanceBetweenEntities(
			PositionableEntity movingEntity,
			PositionableEntity neighbor,
			AXIS axis,
			double[] closestNeighborAxisPoint) {

		Entity movingEntityZoneParent =
			SceneHierarchyUtility.getWallOrFloorParent(model, movingEntity);
        Entity neighborEntityZoneParent =
        	SceneHierarchyUtility.getWallOrFloorParent(model, neighbor);

        // If the entity zone parent is null, set it to the active zone.
        // This is a safe thing to do considering the parent of any entity with
        // a null parent can expect to be added to the current active zone.
        if (movingEntityZoneParent == null) {

            movingEntityZoneParent =
            	SceneHierarchyUtility.getActiveZoneEntity(model);

            if (movingEntityZoneParent == null) {
            	return null;
            }
        }

        if (neighborEntityZoneParent == null) {

            neighborEntityZoneParent =
            	SceneHierarchyUtility.getActiveZoneEntity(model);

            if (neighborEntityZoneParent == null) {
            	return null;
            }
        }

        // If either of these are null, we cannot compute
        if (movingEntityZoneParent == null ||
        		neighborEntityZoneParent == null) {
            return null;
        }

        double[] distanceVals = null;

        // If these do not share the same zone entity, then we have to make the
        // special wall angle considerations to see if we are supporting it.
        // We are not supporting proximity snapping between between zones for
        // any other case besides walls.
        if(movingEntityZoneParent.getEntityID() !=
            neighborEntityZoneParent.getEntityID()){

        	// If either of these are not segments then we do not support
        	// proximity snapping. CalculateWallAngleDistanceCase will return
        	// null if they are not the same zone, or if the angle is not
        	// legal.
            distanceVals = calculateWallAngleDistanceCase(
            		model,
            		movingEntity,
            		(PositionableEntity) movingEntityZoneParent,
            		neighbor,
            		(PositionableEntity) neighborEntityZoneParent,
            		axis,
            		closestNeighborAxisPoint);

        } else {

	        // Default is to process distance calculated center to center
        	distanceVals = TransformUtils.getDistanceBetweenEntities(
        			model, neighbor, movingEntity, true);

	        // Handle edge based calculation case
	    	if (snapStyle == PROX_SNAP_STYLE.EDGE) {

	    		if (axis == AXIS.X) {

		    		float gap =
		    			BoundsUtils.getHorizontalGapBetweenBoxes(
		    					model, 
		    					movingEntity, 
		    					neighbor, 
		    					true,
		    					closestNeighborAxisPoint);

		    		distanceVals[0] = gap;

	    		} else if (axis == AXIS.Y) {

	    			float gap =
		    			BoundsUtils.getVerticalGapBetweenBoxes(
		    					model, 
		    					movingEntity, 
		    					neighbor, 
		    					true, 
		    					closestNeighborAxisPoint);

		    		distanceVals[1] = gap;

	    		} else {
	    			return null;
	    		}

	    	}
        }

        return distanceVals;
	}

	/**
	 * Check the angle between adjacent wall angles and if it is 90 or 135
	 * degrees then calculate the distance between the entities. Distance is
	 * calculated secondEntity - firstEntity.
	 *
	 * @param model WorldModel model
	 * @param firstEntity Primary entity to subtract position from second entity
	 * @param firstEntityZoneParent Primary entity's zone parent
	 * @param secondEntity Second entity to measure distance to
	 * @param secondEntityZoneParent Second entity's zone parent
	 * @param axis AXIS enum specifying which axis snaps are applied in
	 * @param closestNeighborAxisPoint Updated with the point on the neighbor
	 * closest to the movingEntity that is used in the distance calculation. 
	 * Value will be in coordinates relative to movingEntity's parent.
	 * @return Distance or null if there was a problem
	 */
	private double[] calculateWallAngleDistanceCase(
			WorldModel model,
			PositionableEntity firstEntity,
			PositionableEntity firstEntityZoneParent,
			PositionableEntity secondEntity,
			PositionableEntity secondEntityZoneParent,
			AXIS axis,
			double[] closestNeighborAxisPoint) {

		// Verify both are segments, otherwise return null
    	if (firstEntityZoneParent.getType() != Entity.TYPE_SEGMENT ||
    			secondEntityZoneParent.getType() != Entity.TYPE_SEGMENT) {

    		return null;
    	}

    	// Isolate the segment that is the active zone entity. If we can't then
    	// return null.
    	SegmentEntity activeZoneSegment;
    	SegmentEntity adjacentZoneSegment;

    	int activeZoneID =
    		SceneHierarchyUtility.getActiveZoneEntity(model).getEntityID();

    	if (activeZoneID == firstEntityZoneParent.getEntityID()) {
    		activeZoneSegment = (SegmentEntity) firstEntityZoneParent;
    		adjacentZoneSegment = (SegmentEntity) secondEntityZoneParent;
    	} else if (activeZoneID == secondEntityZoneParent.getEntityID()) {
    		activeZoneSegment = (SegmentEntity) secondEntityZoneParent;
    		adjacentZoneSegment = (SegmentEntity) firstEntityZoneParent;
    	} else {
    		return null;
    	}

    	// Determine if the adjacent zone is to the left or the right of
    	// active zone. If it is neither, return null.
    	boolean beforeActiveZone = true;

    	VertexEntity activeZoneStartVertex =
    		activeZoneSegment.getStartVertexEntity();
    	VertexEntity activeZoneEndVertex =
    		activeZoneSegment.getEndVertexEntity();

    	VertexEntity adjacentZoneStartVertex =
    		adjacentZoneSegment.getStartVertexEntity();
    	VertexEntity adjacentZoneEndVertex =
    		adjacentZoneSegment.getEndVertexEntity();

    	if (adjacentZoneStartVertex == activeZoneEndVertex) {
    		beforeActiveZone = false;
    	} else if (adjacentZoneEndVertex == activeZoneStartVertex) {
    		beforeActiveZone = true;
    	} else {
    		return null;
    	}

    	// Generate the corresponding vectors
    	Vector3f vecA = new Vector3f();
    	Vector3f vecB = new Vector3f();

    	if (beforeActiveZone) {

    		double[] adjStartVertexPos = new double[3];
    		double[] adjEndVertexPos = new double[3];

    		adjacentZoneStartVertex.getPosition(adjStartVertexPos);
    		adjacentZoneEndVertex.getPosition(adjEndVertexPos);

    		vecA.set(
    				(float) (adjStartVertexPos[0] - adjEndVertexPos[0]),
    				(float) (adjStartVertexPos[1] - adjEndVertexPos[1]),
    				(float) (adjStartVertexPos[2] - adjEndVertexPos[2]));

    		double[] actZoneStartVertexPos = new double[3];
    		double[] actZoneEndVertexPos = new double[3];

    		activeZoneStartVertex.getPosition(actZoneStartVertexPos);
    		activeZoneEndVertex.getPosition(actZoneEndVertexPos);

    		vecB.set(
    				(float)
    				(actZoneEndVertexPos[0] - actZoneStartVertexPos[0]),
    				(float)
    				(actZoneEndVertexPos[1] - actZoneStartVertexPos[1]),
    				(float)
    				(actZoneEndVertexPos[2] - actZoneStartVertexPos[2]));

    	} else {

    		double[] adjStartVertexPos = new double[3];
    		double[] adjEndVertexPos = new double[3];

    		adjacentZoneStartVertex.getPosition(adjStartVertexPos);
    		adjacentZoneEndVertex.getPosition(adjEndVertexPos);

    		vecA.set(
    				(float) (adjEndVertexPos[0] - adjStartVertexPos[0]),
    				(float) (adjEndVertexPos[1] - adjStartVertexPos[1]),
    				(float) (adjEndVertexPos[2] - adjStartVertexPos[2]));

    		double[] actZoneStartVertexPos = new double[3];
    		double[] actZoneEndVertexPos = new double[3];

    		activeZoneStartVertex.getPosition(actZoneStartVertexPos);
    		activeZoneEndVertex.getPosition(actZoneEndVertexPos);

    		vecB.set(
    				(float)
    				(actZoneStartVertexPos[0] - actZoneEndVertexPos[0]),
    				(float)
    				(actZoneStartVertexPos[1] - actZoneEndVertexPos[1]),
    				(float)
    				(actZoneStartVertexPos[2] - actZoneEndVertexPos[2]));
    	}

    	// Check the angle between vectors
    	double angle = vecB.angle(vecA);

    	if ((angle < (NINETY_RAD + ANGLE_THRESHOLD)) &&
    			(angle > (NINETY_RAD - ANGLE_THRESHOLD))) {
    		//System.out.println("VALID ANGLE OF 90 Deg");
    		;
    	} else if ((angle < (ONE_THIRTY_FIVE_RAD + ANGLE_THRESHOLD)) &&
    			(angle  > (ONE_THIRTY_FIVE_RAD - ANGLE_THRESHOLD))) {
    		//System.out.println("VALID ANGLE OF 135 Deg");
    		;
    	} else {
    		return null;
    	}

    	// If center-to-center get the distance between entities and
    	// return distance between.
    	// If edge-to-edge get the shortest distance between edges
    	// depending on angle between values.
    	double[] distance = new double[3];

    	// Default is to process distance calculated center to center
    	distance = TransformUtils.getDistanceBetweenEntities(
    			model, secondEntity, firstEntity, true);

        // Handle edge based calculation case
    	if (snapStyle == PROX_SNAP_STYLE.EDGE) {

    		if (axis == AXIS.X) {

    			if (axis == AXIS.X) {

		    		float gap =
		    			BoundsUtils.getHorizontalGapBetweenBoxes(
		    					model, 
		    					firstEntity, 
		    					secondEntity, 
		    					true,
		    					closestNeighborAxisPoint);

		    		distance[0] = gap;

	    		} else if (axis == AXIS.Y) {

	    			float gap =
		    			BoundsUtils.getVerticalGapBetweenBoxes(
		    					model, 
		    					firstEntity, 
		    					secondEntity, 
		    					true,
		    					closestNeighborAxisPoint);

	    			distance[1] = gap;

	    		} else {
	    			return null;
	    		}
    		}

    	}

    	return distance;
	}

	/**
	 * Snap the sourceEntity to the same upper bounds height as the targetEntity
	 * allowing for movement of the same height if the position is greater than
	 * the tolerance range set. Allows for free range of motion above the
	 * upper bounds height of the targetEntity once it moves of the snap until
	 * the sourceEntity comes back to the upper bounds height of the
	 * targetEntity at which point the snap is applied again.
	 *
	 * @param model WorldModel to reference
	 * @param sourceEntity Entity to snap to targetEntity height
	 * @param targetEntity Entity whose upper bounds is the snap height
	 * @param position Position whose Y value is to be adjusted
	 */
	private void snapToSameHeight(
			WorldModel model,
			PositionableEntity sourceEntity,
			PositionableEntity targetEntity,
			double[] position) {

		// Establish bounding boxes in scene coordinates, and get extents in
    	// zone coordinates
    	OrientedBoundingBox sourceBox =
    		BoundsUtils.getOrientedBoundingBox(model, sourceEntity, true, true);

    	OrientedBoundingBox targetBox =
    		BoundsUtils.getOrientedBoundingBox(model, targetEntity, true, true);

    	if (sourceBox == null || targetBox == null) {
    		return;
    	}

    	// Get the parent of the source entity to convert scene coordinates
    	// relative to.
    	PositionableEntity parent = (PositionableEntity)
    		SceneHierarchyUtility.getExactParent(model, sourceEntity);

    	if (parent == null) {
    		return;
    	}

    	// Get the extents of the sourceEntity and targetEntity, note these
    	// extents will be in scene coordinates.
    	float[] sourceEntityMaxExtents = new float[3];
    	float[] sourceEntityMinExtents = new float[3];
    	sourceBox.getExtents(sourceEntityMinExtents, sourceEntityMaxExtents);

    	float[] targetEntityMaxExtents = new float[3];
    	float[] targetEntityMinExtents = new float[3];
    	targetBox.getExtents(targetEntityMinExtents, targetEntityMaxExtents);

    	// Convert to local coordinates of sourceEntity's parent.
    	targetEntityMaxExtents =
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
					model, targetEntityMaxExtents, parent, true);

		sourceEntityMaxExtents =
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
					model, sourceEntityMaxExtents, parent, true);

    	// We always want to align the top edges, so we are concerned with the
    	// second index of both the sourceEntityMaxExtents and the
    	// targetEntityMaxExtents.

    	// Calculate the threshold based on the zoom amount
		double threshold = RuleUtils.getZoomThreshold(view, HEIGHT_THRESHOLD);

		double distance = targetEntityMaxExtents[1] - sourceEntityMaxExtents[1];

		PROX_SNAP_HEIGHT_OPT heightOpt = (PROX_SNAP_HEIGHT_OPT)
		RulePropertyAccessor.getRulePropertyValue(
				sourceEntity,
				ChefX3DRuleProperties.PROXIMITY_HEIGHT_SNAP_ABOVE_BELOW);

    	if (distance < threshold && distance > -threshold) {

    		// If we are above or below in state and respective position,
    		// then don't lock in.
    		if (distance > 0 && heightOpt == PROX_SNAP_HEIGHT_OPT.ABOVE) {
    			return;
    		} else if (distance < 0 && heightOpt == PROX_SNAP_HEIGHT_OPT.BELOW){
    			return;
    		}

    		// Lock in to height
    		position[1] += distance;

    		RulePropertyAccessor.setRuleProperty(
    				sourceEntity,
    				ChefX3DRuleProperties.PROXIMITY_SNAP_TARGET_ENTITY_ID,
    				targetEntity.getEntityID());

    		RulePropertyAccessor.setRuleProperty(
    				sourceEntity,
    				ChefX3DRuleProperties.PROXIMITY_HEIGHT_SNAP_ABOVE_BELOW,
    				PROX_SNAP_HEIGHT_OPT.EVEN);

    		return;

    	} else if (heightOpt == PROX_SNAP_HEIGHT_OPT.EVEN) {

    		if (distance > 0) {
    			RulePropertyAccessor.setRuleProperty(
        				sourceEntity,
        				ChefX3DRuleProperties.PROXIMITY_HEIGHT_SNAP_ABOVE_BELOW,
        				PROX_SNAP_HEIGHT_OPT.ABOVE);
    		} else if (distance < 0) {
    			RulePropertyAccessor.setRuleProperty(
        				sourceEntity,
        				ChefX3DRuleProperties.PROXIMITY_HEIGHT_SNAP_ABOVE_BELOW,
        				PROX_SNAP_HEIGHT_OPT.BELOW);
    		}
    	}
	}
}
