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

// External Imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

// Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.EDGE_FACE_SNAP_ADJ_AXIS;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.EntityWrapper;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Applies edge snapping to products to keep them positioned such that their
 * bounds is correctly aligned and not overlapping.
 *
 * @author Ben Yarger
 * @version $Revision: 1.15 $
 */
public class EdgeSnapRule extends BaseRule {

	/**
	 * Distance tolerance required for an acceptable calculation. This is the
	 * distance between edges.
	 */
	private static final float DISTANCE_TOLERANCE = 0.001f;

	/**
	 * Distance to extend each end of the test segment. This is the distance to
	 * extend each end of the line segment used for testing so we don't end up
	 * always in a collision position.
	 */
	private static final double EXTENSION_DISTANCE = 0.5;

	/** Epsilon value */
	private static final float EPSILON = 0.001f;

	/** Max number of iterations allowed to determine a 'collision-less' position */
	private static final int MAX_ITERATIONS = 1000;

	/** Entity utils instance */
	private EntityUtils entityUtils;

	/** Vertices of the object of interest - projected to the zone */
	private Point3f[] vtx;

	/** Scratch vecmath objects */
	private Matrix4f mtx;
	private Vector3f entity_position;
	private AxisAngle4f entity_rotation;
	private Vector3f znormal;
	private Point3f targetCenter;
	private Vector3f direction;
	private Vector3f adjustment;
	private Vector3f outer;
	private Vector3f inner;
	private Vector3f vec;
	
	private Matrix4f zone_mtx;
	private Matrix4f inv_zone_mtx;
	private Point3f min;
	private Point3f max;
	private Point3f pos;
		
	/**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
	public EdgeSnapRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view) {

		super(errorReporter, model, view);

		entityUtils = new EntityUtils(model);

		mtx = new Matrix4f();
		entity_position = new Vector3f();
		entity_rotation = new AxisAngle4f();
		znormal = new Vector3f(0, 0, 1);
		targetCenter = new Point3f();
		direction = new Vector3f();
		adjustment = new Vector3f();
		outer = new Vector3f();
		inner = new Vector3f();
		vec = new Vector3f();
		
		zone_mtx = new Matrix4f();
		inv_zone_mtx = new Matrix4f();
		min = new Point3f();
		max = new Point3f();
		pos = new Point3f();
		
		vtx = new Point3f[4];
		vtx[0] = new Point3f();
		vtx[1] = new Point3f();
		vtx[2] = new Point3f();
		vtx[3] = new Point3f();
		
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

		if (!(entity instanceof PositionableEntity)) {
			result.setResult(true);
	        return(result);
		}

		// If edge face snapping is not enabled, bail
		Boolean useEdgeSnap = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.USE_EDGE_FACE_SNAP);

		if (useEdgeSnap) {
			beginEdgeSnapProcessing(model, (PositionableEntity)entity, command);
		}

		result.setResult(true);
        return(result);
	}

	//--------------------------------------------------------------------------
	// Private methods
	//--------------------------------------------------------------------------

	/**
	 * Start the actual processing of the commands and calculation of the
	 * edge face snap.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity to check against targets
	 * @param command Command affecting entity
	 */
	private void beginEdgeSnapProcessing(
			WorldModel model,
			PositionableEntity entity,
			Command command) {

		// If entity isn't a positionable entity there is nothing we can do
		if (!(entity instanceof PositionableEntity)) {
			return;
		}

		PositionableEntity pEntity = (PositionableEntity) entity;

		// Get the edge case classifications to match, we can't do anything
		// without it
		String[] edgeSnapClassifications = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.EDGE_FACE_TARGET_CLASSIFICATIONS);

		if (edgeSnapClassifications == null) {
			return;
		}

		// Get the collisions, and remove any that don't match the edge snap
		// collision classifications. This will leave us with just the
		// collisions that are edge snap targets.
		rch.performCollisionCheck(command, true, false, false);
		ArrayList<Entity> collisionSet =
			new ArrayList<Entity>(rch.collisionEntities);

		for (int i = (collisionSet.size() - 1); i >= 0; i--) {

			Entity collision = collisionSet.get(i);

			String[] classifications = (String[])
				RulePropertyAccessor.getRulePropertyValue(
						collision,
						ChefX3DRuleProperties.CLASSIFICATION_PROP);

			if (classifications == null) {
				collisionSet.remove(i);
				continue;
			}

			List<String> classList = Arrays.asList(classifications);

			boolean match = false;

			for (int j = 0; j < edgeSnapClassifications.length; j++) {

				if (classList.contains(edgeSnapClassifications[j])) {
					match = true;
					break;
				}
			}

			if (!match) {
				collisionSet.remove(i);
			}
		}

		// If there are no edge collisions for us to interact with then quit
		if (collisionSet.size() == 0) {
			return;
		}

		// Extract the remaining edge property values, if and only if there is
		// a single collision case.

		Matrix4f optionalTransform = null;
		AxisAngle4f axisAngleRotation = null;

		if (collisionSet.size() == 1) {

			Boolean edgeSnapUseVector;
			float[] edgeSnapVectorValues;
			EDGE_FACE_SNAP_ADJ_AXIS edgeSnapAxis;

			edgeSnapUseVector = (Boolean)
	    		RulePropertyAccessor.getRulePropertyValue(
	    			entity,
	    			ChefX3DRuleProperties.EDGE_FACE_USE_EDGE_VECTOR);

			edgeSnapVectorValues = (float[])
				RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.EDGE_FACE_EDGE_VECTOR);

			edgeSnapAxis = (EDGE_FACE_SNAP_ADJ_AXIS)
				RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.EDGE_FACE_ORIENTATION_LIMIT);


			// Calculate the transform to apply, if any.
			// If EDGE_FACE_USE_EDGE_VECTOR is true, then we apply an optional
			// transform to the bounding box.
			if (edgeSnapUseVector && edgeSnapVectorValues != null) {

				// Gather target entity and local positions
				PositionableEntity targetEntity =
					(PositionableEntity) collisionSet.get(0);

				// get the previous frame positions
				double[] targetPos = TransformUtils.getPosition(targetEntity);
				double[] pEntityPos = TransformUtils.getPosition(pEntity);

				double[] targetDisplacement = new double[3];
				targetDisplacement[0] = targetPos[0] - pEntityPos[0];
				targetDisplacement[1] = targetPos[1] - pEntityPos[1];
				targetDisplacement[2] = targetPos[2] - pEntityPos[2];

				// Establish necessary vectors
				Vector3f orientationVec = new Vector3f(edgeSnapVectorValues);

				// Flatten reltiveDisplaceVec on the z axis. We don't want any
				// Z influence when calculating rotation.
				Vector3f relativeDisplaceVec = new Vector3f(
						(float) (targetDisplacement[0]),
						(float) (targetDisplacement[1]),
						0.0f);

				// Do the epsilon cleanup
				//vector3fEpsilonCleanup(orientationVec);
				//vector3fEpsilonCleanup(relativeDisplaceVec);

				// Depending on the orientation axis, do the appropriate
				// calculations
				switch (edgeSnapAxis) {

					case XAXIS:

						if (relativeDisplaceVec.x != 0) {
							relativeDisplaceVec.y = 0.0f;
							relativeDisplaceVec.normalize();

//							orientationVec.y = 0.0f;
							orientationVec.normalize();

							// To switch back to doing a y axis rotation, just
							// swap the y and z indexes of both axis arrays.
							if (relativeDisplaceVec.x < 0) {

								float[] axis = new float[] {0.0f, 0.0f, 1.0f};
								axisAngleRotation = new AxisAngle4f(
									new Vector3f(axis),
									orientationVec.angle(relativeDisplaceVec));
							} else {

								float[] axis = new float[] {0.0f, 0.0f, -1.0f};
								axisAngleRotation = new AxisAngle4f(
									new Vector3f(axis),
									orientationVec.angle(relativeDisplaceVec));
							}
						}
						break;

					case YAXIS:

						// Do nothing, no rotation required!

						break;
				}
			}
		}

		//-----------------------------------------
		// Do divide and conquer command processing
		//-----------------------------------------
		boolean success = false;

		if (command instanceof AddEntityChildCommand) {
/*
			double[] position =
				TransformUtils.divideAndConquerMoveCommands(
					model,
					pEntity,
					optionalTransform,
					collisionSet,
					DISTANCE_TOLERANCE,
					EXTENSION_DISTANCE);
*/
			double[] position = reposition(pEntity, collisionSet);
			if (position != null) {
				pEntity.setPosition(position, false);
				success = true;
			}

		} else if (command instanceof AddEntityChildTransientCommand) {
/*
			double[] position =
				TransformUtils.divideAndConquerMoveCommands(
					model,
					pEntity,
					optionalTransform,
					collisionSet,
					DISTANCE_TOLERANCE,
					EXTENSION_DISTANCE);
*/
			double[] position = reposition(pEntity, collisionSet);
			if (position != null) {
				pEntity.setPosition(position, false);
				success = true;
			}

		} else if (command instanceof MoveEntityCommand) {
/*
			double[] position =
				TransformUtils.divideAndConquerMoveCommands(
					model,
					pEntity,
					optionalTransform,
					collisionSet,
					DISTANCE_TOLERANCE,
					EXTENSION_DISTANCE);
*/
			double[] position = reposition(pEntity, collisionSet);
			if (position != null) {
				((MoveEntityCommand)command).setEndPosition(position);
				success = true;
			}

		} else if (command instanceof MoveEntityTransientCommand) {
/*
			double[] position =
				TransformUtils.divideAndConquerMoveCommands(
					model,
					pEntity,
					optionalTransform,
					collisionSet,
					DISTANCE_TOLERANCE,
					EXTENSION_DISTANCE);
*/
			double[] position = reposition(pEntity, collisionSet);
			if (position != null) {
				((MoveEntityTransientCommand)command).setPosition(position);
				success = true;
			}

		} else if (command instanceof TransitionEntityChildCommand) {

			// Don't need to do anything for transient case because that
			// is only changing parents, so we skip it.
			if (command.isTransient()) {
				return;
			}
/*
			double[] position =
				TransformUtils.divideAndConquerMoveCommands(
					model,
					pEntity,
					optionalTransform,
					collisionSet,
					DISTANCE_TOLERANCE,
					EXTENSION_DISTANCE);
*/
			double[] position = reposition(pEntity, collisionSet);
			if (position != null) {
				TransitionEntityChildCommand tranCmd =
					(TransitionEntityChildCommand) command;

				tranCmd.setEndPosition(position);
				success = true;
			}

		} else if (command instanceof ScaleEntityTransientCommand) {

			ScaleEntityTransientCommand scaleCmd =
				(ScaleEntityTransientCommand) command;

			double[] newPosition = new double[3];
			double[] oldPosition = new double[3];
			float[] newScale = new float[3];
			float[] oldScale = new float[3];

			scaleCmd.getPosition(newPosition);
			entity.getPosition(oldPosition);
			scaleCmd.getScale(newScale);
			entity.getScale(oldScale);

			double[] finalPosition = new double[3];
			float[] finalScale = new float[3];

			boolean result =
				specialScaleCaseProcessing(
					model,
					pEntity,
					newPosition,
					oldPosition,
					newScale,
					oldScale,
					optionalTransform,
					collisionSet,
					finalPosition,
					finalScale);

			if (result) {
				scaleCmd.setPosition(finalPosition);
				scaleCmd.setScale(finalScale);
				success = true;
			}

		} else if (command instanceof ScaleEntityCommand) {

			ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;

			double[] newPosition = new double[3];
			double[] oldPosition = new double[3];
			float[] newScale = new float[3];
			float[] oldScale = new float[3];

			scaleCmd.getNewPosition(newPosition);
			entity.getPosition(oldPosition);
			scaleCmd.getNewScale(newScale);
			entity.getScale(oldScale);

			double[] finalPosition = new double[3];
			float[] finalScale = new float[3];

			boolean result =
				specialScaleCaseProcessing(
					model,
					pEntity,
					newPosition,
					oldPosition,
					newScale,
					oldScale,
					optionalTransform,
					collisionSet,
					finalPosition,
					finalScale);

			if (result) {
				scaleCmd.setNewPosition(finalPosition);
				scaleCmd.setNewScale(finalScale);
				success = true;
			}

		} else {
			return;
		}

		if (success && axisAngleRotation != null) {

			float[] rotation = new float[4];
			axisAngleRotation.get(rotation);

			SceneManagementUtility.rotateEntity(
					model,
					collisionChecker,
					pEntity,
					rotation,
					command.isTransient(),
					true);
					//false);
		}
	}

	/**
	 * Perform the scale case processing. Return the corrected values in
	 * finalPosition and finalScale if the return is True.
	 *
	 * @param model WorldModel to reference
	 * @param pEntity Entity to check
	 * @param newPosition The new position resulting from the scale command
	 * @param oldPosition The previous frame position
	 * @param newScale The new scale resulting from the scale command
	 * @param oldScale The previous frame scale
	 * @param optionalTransform Optional transforms to apply to the entity's
	 * OrientedBoundingBox
	 * @param collisionSet Targets to test against
	 * @param finalPosition Resulting final position
	 * @param finalScale Resulting final scale
	 * @return True if the finalPosition and finalScale values have been
	 * correctly set, false otherwise
	 */
	private boolean specialScaleCaseProcessing(
			WorldModel model,
			PositionableEntity pEntity,
			double[] newPosition,
			double[] oldPosition,
			float[] newScale,
			float[] oldScale,
			Matrix4f optionalTransform,
			ArrayList<Entity> collisionSet,
			double[] finalPosition,
			float[] finalScale) {

		// Calculate the extension distance based on the total amount of
		// the growth resulting from the scale, divide it in half and add
		// the standard EXTENSION_DISTANCE. This guarantees that we will
		// perform the calculation over the correct positional distance.
		// Note, the total amount of growth is equal to the change in
		// position * 2. Since we want to divide the total amount of growth
		// in half we can just use the length of the change of position
		// vector + EXTENSION_DISTANCE to calculate our total extension
		// distance to use.

		Vector3d changeInPosition = new Vector3d();
		changeInPosition.x = (newPosition[0] - oldPosition[0]);
		changeInPosition.y = (newPosition[1] - oldPosition[1]);
		changeInPosition.z = (newPosition[2] - oldPosition[2]);
/*
		double[] position =
			TransformUtils.divideAndConquerMoveCommands(
				model,
				pEntity,
				optionalTransform,
				collisionSet,
				DISTANCE_TOLERANCE,
				(changeInPosition.length() + EXTENSION_DISTANCE));
*/
		double[] position = reposition(pEntity, collisionSet);
		// If we didn't get a valid position back, then skip processing
		if (position != null) {

			float[] bounds = new float[6];
			float[] size = new float[3];

			// Get all the necessary previous frame data
			pEntity.getBounds(bounds);
			pEntity.getSize(size);
			pEntity.getScale(finalScale);

			// Calculate the full change in position to apply.
			//
			// The full change in position is half way between the position
			// result from the divide and conquer routine and the new position
			// from the command. This is because we are testing collisions
			// against the new scale, not the old. By testing with the new scale
			// size in the divide and conquer process, we get overhang of the
			// entity in the opposite direction of the scale. This overhang is
			// the difference between the test entity bounds and the previous
			// frames entity bounds. Since we are doing all of our calculations
			// based on position, we know that to split that overhang we have
			// to move to a position half way between the new position from the
			// command and the resulting position from the divide and conquer.
			// This will split the overhang. We eliminate it from both ends when
			// we consider the distance from the old outside bounds to the final
			// new position (1/2 distance between new command position and
			// resulting divide and conquer result) and double that to get the
			// final scale.

			// Calculate the final change in position, this is not a
			// displacement vector, but an actual position.
			changeInPosition.x = (
					(newPosition[0] - position[0])/2.0 + position[0]);
			changeInPosition.y = (
					(newPosition[1] - position[1])/2.0 + position[1]);
			changeInPosition.z = (
					(newPosition[2] - position[2])/2.0 + position[2]);

			// Calculate the actual change in position between the previous
			// frame position and the calculated new one. If we are within the
			// epsilon tolerance, then set the net change in position to zero.
			double xSizeVec = Math.abs(changeInPosition.x - oldPosition[0]);
			double ySizeVec = Math.abs(changeInPosition.y - oldPosition[1]);

			if (xSizeVec < EPSILON && xSizeVec > -EPSILON) {
				xSizeVec = 0.0;
				changeInPosition.x = (oldPosition[0]);
			}

			if (ySizeVec < EPSILON && ySizeVec > -EPSILON) {
				ySizeVec = 0.0;
				changeInPosition.y = (oldPosition[1]);
			}

			// Calculate the resulting size change based on the direction of the
			// scale and the appropriate opposite bounds. Have to double this
			// measurement to get the full axis specific scale.
			float xSize;
			float ySize;

			if (changeInPosition.x < 0) {
				xSize = (float) ((bounds[1] + xSizeVec) * 2.0);
			} else if (changeInPosition.x > 0) {
				xSize = (float) ((-bounds[0] + xSizeVec) * 2.0);
			} else {
				xSize = bounds[1] - bounds[0];
			}

			if (changeInPosition.y < 0) {
				ySize = (float) ((bounds[3] + ySizeVec) * 2.0);
			} else if (changeInPosition.y > 0) {
				ySize = (float) ((-bounds[2] + ySizeVec) * 2.0);
			} else {
				ySize = bounds[3] - bounds[2];
			}

			// Set our final parameter values to hand back.
			finalScale[0] = xSize/size[0];
			finalScale[1] = ySize/size[1];

			changeInPosition.get(finalPosition);

			return true;
		}

		return false;
	}

	/**
	 * Flatten values to zero based on epsilon.
	 *
	 * @param vec Vector to examine.
	 */
	private void vector3fEpsilonCleanup(Vector3f vec) {

		if (vec.x < EPSILON && vec.x > -EPSILON) {
			vec.x = (0.0f);
		}

		if (vec.y < EPSILON && vec.y > -EPSILON) {
			vec.y = (0.0f);
		}

		if (vec.z < EPSILON && vec.z > -EPSILON) {
			vec.z = (0.0f);
		}
	}

	/**
	 * Calculate and return a position for the argument entity
	 * that is not in contact with the entities in the collisionSet
	 * and is within the proximity tolerance value.
	 *
	 * @param entity The entity to position
	 * @param targetList The list of entities to evaluate against
	 * @return A new position
	 */
	private double[] reposition(PositionableEntity entity, ArrayList<Entity> targetList) {

		// get the entity's current transformation parameters
		double[] start_entity_position = TransformUtils.getExactPosition(entity);

		// get the world to zone transforms
		Entity zone = SceneHierarchyUtility.getActiveZoneEntity(model);
		entityUtils.getTransformToRoot(zone, zone_mtx);
		
		inv_zone_mtx.set(zone_mtx);
		inv_zone_mtx.invert();
		
		// get the entity's current transformation parameters,
		// in zone relative
		Matrix4f tmtx = TransformUtils.getTransformsInSceneCoordinates(
			model,
			entity,
			true);
		tmtx.mul(inv_zone_mtx, tmtx);
		tmtx.get(entity_position);
		entity_rotation.set(tmtx);
		
		// entity wrappers, from which to obtain bounding objects
		HashMap<Integer, EntityWrapper> wmap = view.getEntityWrapperMap();

		// create a bounds object for the entity. use the utility rather
		// than getting from the map - since in the initial add case -
		// the map does not seem to contain a bounds object
		OrientedBoundingBox entityBounds =
			BoundsUtils.getOrientedBoundingBox(model, entity, true, true);

		// bounding radius of the entity
		float entity_radius = getRadius(entityBounds);

		for (int i = 0; i < targetList.size(); i++) {

			// get the bounds of the collision target, transform to
			// zone relative
			Entity target = targetList.get(i);
			EntityWrapper targetWrapper = wmap.get(target.getEntityID());
			OrientedBoundingBox targetBounds = targetWrapper.getBounds();
			
			entityUtils.getTransformToRoot(target, mtx);
			mtx.mul(inv_zone_mtx, mtx);
			targetBounds.transform(mtx);
			
			// bounding radius of the target
			float target_radius = getRadius(targetBounds);

			// the maximum distance from the target center that
			// the entity must be to avoid intersection
			float max_distance = entity_radius + target_radius + DISTANCE_TOLERANCE;
			
			targetBounds.getVertices(min, max);
			targetBounds.getCenter(targetCenter);

			// the target's bounds vertices, projected on the zone
			////////////////////////////////////////////////////////
			// rem: this is setting up the edges of the bounds, as
			// seen from a top down view of the zone. this will not
			// be correct if the target (or any of it's ancestors to
			// the zone) have been rotated about any axis except the
			// z axis. also, the vertice must be set in a counter-
			// clockwise sequence for the getDirection() method to
			// be successful.
			vtx[0].set(max.x, min.y, max.z);
			mtx.transform(vtx[0]);
			vtx[1].set(max.x, max.y, max.z);
			mtx.transform(vtx[1]);
			vtx[2].set(min.x, max.y, max.z);
			mtx.transform(vtx[2]);
			vtx[3].set(min.x, min.y, max.z);
			mtx.transform(vtx[3]);
			////////////////////////////////////////////////////////
			
			// the entity's zone relative position
			pos.x = entity_position.x;
			pos.y = entity_position.y;
			pos.z = entity_position.z;
			
			// get the direction in which to move the entity
			getDirection(vtx, pos, direction);
			direction.normalize();
			
			adjustment.set(direction);
			adjustment.scale(max_distance);

			// outer - a position outside the target bounds
			outer.add(targetCenter, adjustment);
			outer.z = entity_position.z; // keep the adjustment in the x/y plane

			// inner - a position inside the target bounds
			inner.set(entity_position);

			// adjustment - change to the entity position, ping pongs
			// between inner and outer till the tolerance is reached.
			adjustment.sub(outer, inner);

			boolean intersect = false;
			// adjust the entity to the half way point between the inner
			// and outer position until the desired distance is achieved
			int num = MAX_ITERATIONS;
			for (; num > 0; num--) {

				if (adjustment.length() <= DISTANCE_TOLERANCE) {
					if (!intersect) {
						entity_position.set(inner);
					}
					break;
				}
				// take the mid point
				adjustment.scale(0.5f);

				// reconfigure the bounds
				entity_position.add(adjustment);
				mtx.setIdentity();
				mtx.setTranslation(entity_position);
				mtx.setRotation(entity_rotation);
				entityBounds.transform(mtx);

				// check for intersection
				intersect = targetBounds.intersect(entityBounds);

				// recalculate the adjustment vector for the next iteration
				if (intersect) {
					inner.set(entity_position);
					adjustment.sub(outer, inner);
				} else {
					outer.set(entity_position);
					adjustment.sub(inner, outer);
				}
			}
			if (num == 0) {
				// something has gone wrong
				break;
			}
		}
		
		double[] result = new double[3];
		
		PositionableEntity parentEntity = (PositionableEntity)
			SceneHierarchyUtility.getExactParent(model, entity);
		
		if (parentEntity == zone) {
			// the position is already zone relative
			result[0] = entity_position.x;
			result[1] = entity_position.y;
			result[2] = entity_position.z;
		} else {
			// convert zone relative to scene relative
			pos.set(entity_position);
			zone_mtx.transform(pos);
			
			// convert scene relative to local
			result[0] = pos.x;
			result[1] = pos.y;
			result[2] = pos.z;
			result = TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model,
				result,
				parentEntity,
				false);
		}

		return(result);
	}

	/**
	 * Return the radius of the bounding sphere for the argument
	 *
	 * @param bounds The bounding box to evaluate
	 * @return The radius of the bounding sphere
	 */
	private float getRadius(OrientedBoundingBox bounds) {
		bounds.getVertices(min, max);
		vec.set(max);
		return(vec.length());
	}
	
	/**
	 * Calculate the direction vector to use to push an entity located
	 * at position away from an entity with bounding edges defined by
	 * the corner points.
	 *
	 * @param corner The corners of the bounds
	 * @param pnt The entity position
	 * @param direction The object to initialize with the direction vector
	 */
	private void getDirection(Point3f[] corner, Point3f position, Vector3f direction) {
		
		float x0 = position.x;
		float y0 = position.y;
		
		int num_line = corner.length;
		
		int num_positive = 0;
		int positive_idx = -1;
		
		float closest_distance = -Float.MAX_VALUE;
		int closest_idx = -1;
		
		for (int i = 0; i < num_line; i++) {
			int c0 = i;
			int c1 = i + 1;
			if (i == (num_line - 1)) {
				c1 = 0;
			}
			float x1 = corner[c0].x;
			float y1 = corner[c0].y;
			float x2 = corner[c1].x;
			float y2 = corner[c1].y;
			
			float distance = getDistance(x0, y0, x1, y1, x2, y2);
			if (distance >= 0) {
				// a positive distance means the point is outside the bounds
				num_positive++;
				positive_idx = i;
			}
			if (num_positive < 1) {
				// if none are positive, keep track of which is closest
				if (distance > closest_distance) {
					closest_distance = distance;
					closest_idx = i;
				}
			}
		}
		switch (num_positive) {
		case 0:
			// the point is inside the box, use the closest line
			int c0 = closest_idx;
			int c1 = closest_idx + 1;
			if (closest_idx == (num_line - 1)) {
				c1 = 0;
			}
			vec.x = corner[c1].x - corner[c0].x;
			vec.y = corner[c1].y - corner[c0].y;
			vec.z = 0;
			direction.cross(vec, znormal);
			break;
			
		case 1:
			// the point is outside the box and can be adjusted by the
			// normal to a single line
			c0 = positive_idx;
			c1 = positive_idx + 1;
			if (positive_idx == (num_line - 1)) {
				c1 = 0;
			}
			vec.x = corner[c1].x - corner[c0].x;
			vec.y = corner[c1].y - corner[c0].y;
			vec.z = 0;
			direction.cross(vec, znormal);
			break;
			
		case 2:
			// the point is outside the box, at the corner of two lines.
			// adjust from the corner
			c0 = positive_idx;
			if (c0 == (num_line - 1)) {
				c0 = 0;
			}
			direction.x = position.x - corner[c0].x;
			direction.y = position.y - corner[c0].y;
			direction.z = 0;
			break;
		}
	}
	
	/**
	 * Return the point to line distance
	 *
	 * @param x0 The point x
	 * @param y0 The point y
	 * @param x1 The line x
	 * @param y1 The line y
	 * @param x2 The line x
	 * @param y2 The line y
	 * @return The distance from the point to the line
	 */
	private static float getDistance(float x0, float y0, float x1, float y1, float x2, float y2) {

		float delta_x = x2 - x1;
		float delta_y = y2 - y1;

		// 2 dimensional point-line distance,
		// see: http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
		// equation (14)
		float distance = (float)((delta_x * (y1 - y0) - (x1 - x0) * delta_y) /
			Math.sqrt(delta_x * delta_x + delta_y * delta_y));

		return(distance);
	}
}
