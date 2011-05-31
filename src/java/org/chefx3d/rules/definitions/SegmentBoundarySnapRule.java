/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.util.Iterator;
import java.util.HashMap;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityUtils;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.ExpertSceneManagementUtility;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.EntityWrapper;
import org.chefx3d.view.common.RuleCollisionChecker;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;
import org.chefx3d.view.common.SurrogateEntityWrapper;

/**
 *
 */
public class SegmentBoundarySnapRule extends BaseRule  {

	/** Message key */
	private static final String ALIGN_PRODUCT_WITH_WALL_MSG =
		"org.chefx3d.rules.definitions.SegmentBoundarySnapRule.alignProductWithWall";

	/** Requisite relationships */
	private static final String[] RELATIONSHIPS = new String[]{
		"floor:wall", "wall:floor"};

	/** Up vector, from the perspective of the floor */
	private static final Vector3f UP = new Vector3f(0, 0, 1);

	/** Constant value for the snap range */
	private static final float RANGE = 0.05f;

	/** Entity utils */
	private EntityUtils entityUtils;

	/** The vertices of the object of interest - projected to the floor */
	private Point3f[] vtx;

	/** Bounding box used for extended segment bounds */
	private OrientedBoundingBox test_bounds;

	/** List of segments to process */
	private ArrayList<SegmentEntity> seg_list;

	/** List of segments that intersect with the subject entity */
	private ArrayList<SegmentEntity> intersecting_seg_list;

	/** List of segments that the subject entity has been 'fitted' to */
	private ArrayList<SegmentEntity> adjustment_list;

	/** Scratch calculation objects */
	private Matrix4f mtx;
	private Vector3f adj;

	private AxisAngle4f rot0;
	private AxisAngle4f rot1;
	private Vector3d cross_prod;
	private float[] min;
	private float[] max;
	private float[] rot_array;
	private double[] pos_array;
	private float[] size_array;
	private float[] scale_array;

	/**
	 * Constructor
	 *
	 * @param errorReporter
	 * @param model
	 * @param view
	 */
	public SegmentBoundarySnapRule(
		ErrorReporter errorReporter,
		WorldModel model,
		EditorView view){

		super(errorReporter, model, view);

		mtx = new Matrix4f();
		adj = new Vector3f();

		min = new float[3];
		max = new float[3];
		rot_array = new float[4];
		pos_array = new double[3];
		size_array = new float[3];
		scale_array = new float[3];
		vtx = new Point3f[4];
		vtx[0] = new Point3f();
		vtx[1] = new Point3f();
		vtx[2] = new Point3f();
		vtx[3] = new Point3f();
		rot0 = new AxisAngle4f();
		rot1 = new AxisAngle4f();
		cross_prod = new Vector3d();

		seg_list = new ArrayList<SegmentEntity>();
		intersecting_seg_list = new ArrayList<SegmentEntity>();
		adjustment_list = new ArrayList<SegmentEntity>();

		test_bounds = new OrientedBoundingBox();

		ruleType = RULE_TYPE.INVIOLABLE;
	}

	//-----------------------------------------------------------
	// BaseRule Method
	//-----------------------------------------------------------

	/**
	 * Perform the rule check
	 *
	 * @param entity Entity object
	 * @param command Command object
	 * @param result The state of the rule processing
	 * @return The result of the rule processing
	 */
	protected RuleEvaluationResult performCheck(
		Entity entity,
		Command command,
		RuleEvaluationResult result) {

		PositionableEntity p_entity = null;
		Entity parent = null;
		int parentType = -1;

		// validate that the entity is positionable,
		// and identify it's parent
		if (entity instanceof PositionableEntity) {

			p_entity = (PositionableEntity)entity;

			if (command instanceof AddEntityChildCommand) {

				parent = SceneHierarchyUtility.getExactParent(model, entity);
				if (parent != null) {
					parentType = parent.getType();
				}
			} else if ((command instanceof MoveEntityCommand) ||
				(command instanceof MoveEntityTransientCommand)) {

				parent = SceneHierarchyUtility.getExactParent(model, entity);
				
				if (parent == null) {
					result.setResult(true);
					return(result);	
				}
				
				parentType = parent.getType();
			}
		}

		// the necessary conditions for this rule to apply:
		// - the parent is the floor
		// - the relationships are defined as wall:floor, or floor:wall
		// - the ignore wall edge snap property is set false

		if (parentType == Entity.TYPE_GROUNDPLANE_ZONE) {

			String[] relationships =
				(String[])RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
			boolean valid_rel = validRelationships(relationships);

			Boolean ignoreWallEdgeSnap =
				(Boolean) RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.IGNORE_WALL_EDGE_SNAP);

			Boolean restrictToBoundary =
				(Boolean) RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_BOUNDARY);


			HashMap<Integer, EntityWrapper> wmap = view.getEntityWrapperMap();

			adjustment_list.clear();
			intersecting_seg_list.clear();

			initSegList();
			int num_segment = seg_list.size();

			if (valid_rel && !ignoreWallEdgeSnap && (num_segment > 0)) {

				// generate a bounding object for the subject entity
				OrientedBoundingBox bounds = createBounds(model, command, entity);

				if (bounds != null) {

					// determine which walls the entity is within snap range of
					for (int i = 0; i < num_segment; i++) {
						SegmentEntity se = seg_list.get(i);
						EntityWrapper wrapper = wmap.get(se.getEntityID());
						if (entityUtils == null) {
							entityUtils = new EntityUtils(model);
						}
						boolean valid = entityUtils.getTransformToRoot(se, mtx);
						if (!valid) {
							// rem: what to do ?
							continue;
						}
						OrientedBoundingBox seg_bounds = wrapper.getBounds();
						seg_bounds.getVertices(min, max);
						// extend the bounds in front of the 'active' wall surface
						max[2] = RANGE;
						test_bounds.setVertices(min, max);
						test_bounds.transform(mtx);

						if (bounds.intersect(test_bounds)) {

							intersecting_seg_list.add(se);
						}
					}
					int num_intersecting_seg = intersecting_seg_list.size();
					if (num_intersecting_seg > 0) {

						// a wall is within range
						SegmentEntity segment = null;

						if (num_intersecting_seg == 1) {
							segment = intersecting_seg_list.get(0);
						} else {
							// can only snap to a single wall.
							// figure out which one
							//segment = getClosestToBoundsCenter(intersecting_seg_list, bounds);
							segment = snapTo(p_entity, intersecting_seg_list);
						}

						VertexEntity left_ve = segment.getStartVertexEntity();
						left_ve.getPosition(pos_array);
						float x1 = (float)pos_array[0];
						float y1 = (float)pos_array[1];

						VertexEntity rght_ve = segment.getEndVertexEntity();
						rght_ve.getPosition(pos_array);
						float x2 = (float)pos_array[0];
						float y2 = (float)pos_array[1];

						float delta_x = x2 - x1;
						float delta_y = y2 - y1;

						float adjustment = 0;
						boolean doAlignment = false;
						boolean isTransient = false;

						if (command instanceof MoveEntityTransientCommand){

							MoveEntityTransientCommand cmd = (MoveEntityTransientCommand)command;
							cmd.getPosition(pos_array);
							isTransient = true;

						} else if (command instanceof MoveEntityCommand){

							MoveEntityCommand cmd = (MoveEntityCommand)command;
							cmd.getEndPosition(pos_array);
							isTransient = false;

							float segment_width = (float)Math.sqrt(delta_x * delta_x + delta_y * delta_y);

							p_entity.getSize(size_array);
							p_entity.getScale(scale_array);
							float entity_width = size_array[0] * scale_array[0];

							if (segment_width >= entity_width) {

								// only allow product alignment with the wall if
								// the wall is longer than the product.

								p_entity.getRotation(rot_array);
								rot1.set(rot_array);

								segment.getRotation(rot_array);
								rot_array[1] = 0;
								rot_array[2] = 1;
								rot0.set(rot_array);

								if (!rot1.epsilonEquals(rot0, 0.0001f)) {
									String msg = intl_mgr.getString(ALIGN_PRODUCT_WITH_WALL_MSG);
									doAlignment = popUpConfirm.showMessage(msg);
								}
							}
						}

						if (doAlignment) {

							SceneManagementUtility.rotateEntity(
								model,
								collisionChecker,
								p_entity,
								rot_array,
								isTransient,
								false);

							p_entity.getSize(size_array);
							p_entity.getScale(scale_array);

							float x0 = (float)pos_array[0];
							float y0 = (float)pos_array[1];

							// the distance from the entity 'origin' to
							// the front wall face
							float distance = getDistance(x0, y0, x1, y1, x2, y2);
							adjustment = distance - (0.5f * size_array[1] * scale_array[1]);

						} else {

							adjustment = Float.MAX_VALUE;
							for (int j = 0; j < vtx.length; j++) {
								float x0 = vtx[j].x;
								float y0 = vtx[j].y;

								// the distance from each corner of the bounding box
								// to the front wall face
								float distance = getDistance(x0, y0, x1, y1, x2, y2);

								if (distance < adjustment) {
									adjustment = distance;
								}
							}
						}
						adjustment =  (-adjustment);
						adj.set(delta_x, delta_y, 0);
						adj.cross(adj, UP);
						adj.normalize();
						adj.scale(adjustment);

						// sidepocket the adjustment info
						adjustment_list.add(segment);

						if (command instanceof MoveEntityTransientCommand){

							MoveEntityTransientCommand cmd =
								(MoveEntityTransientCommand)command;
							cmd.getPosition(pos_array);
							pos_array[0] += adj.x;
							pos_array[1] += adj.y;
							cmd.setPosition(pos_array);

						} else if (command instanceof MoveEntityCommand){

							MoveEntityCommand cmd = (MoveEntityCommand)command;
							cmd.getEndPosition(pos_array);
							pos_array[0] += adj.x;
							pos_array[1] += adj.y;
							cmd.setEndPosition(pos_array);

						} else if (command instanceof AddEntityChildCommand){

							p_entity.getPosition(pos_array);
							pos_array[0] += adj.x;
							pos_array[1] += adj.y;
							p_entity.setPosition(pos_array, false);
						}
						// enable the r-t-b check to keep the object from
						// intersecting adjacent walls as a result of
						// the snap corrections
						restrictToBoundary = true;
						intersecting_seg_list.clear();
					}
				}
			}

			if (valid_rel && restrictToBoundary && (num_segment > 0)) {

				// generate a bounding object for the subject entity
				OrientedBoundingBox bounds = createBounds(model, command, entity);

				if (bounds != null) {

					// determine which walls the entity is in contact with
					for (int i = 0; i < num_segment; i++) {

						SegmentEntity segment = seg_list.get(i);
						if (adjustment_list.contains(segment)) {
							// only adjust for a segment once
							continue;
						}
						EntityWrapper wrapper = wmap.get(segment.getEntityID());

						if (entityUtils == null) {
							entityUtils = new EntityUtils(model);
						}
						boolean valid = entityUtils.getTransformToRoot(segment, mtx);
						if (!valid) {
							// rem: what to do ?
							continue;
						}

						OrientedBoundingBox seg_bounds = wrapper.getBounds();
						seg_bounds.transform(mtx);
						if (bounds.intersect(seg_bounds)) {

							if (adjustment_list.size() == 0) {

								// if the entity has not been adjusted to any other
								// walls, then just push it directly away from the
								// current segment

								VertexEntity left_ve = segment.getStartVertexEntity();
								left_ve.getPosition(pos_array);
								float x1 = (float)pos_array[0];
								float y1 = (float)pos_array[1];

								VertexEntity rght_ve = segment.getEndVertexEntity();
								rght_ve.getPosition(pos_array);
								float x2 = (float)pos_array[0];
								float y2 = (float)pos_array[1];

								float delta_x = x2 - x1;
								float delta_y = y2 - y1;

								float adjustment = Float.MAX_VALUE;
								for (int j = 0; j < vtx.length; j++) {

									float x0 = vtx[j].x;
									float y0 = vtx[j].y;

									// the distance from each corner of the bounding box
									// to the front wall face
									float distance = getDistance(x0, y0, x1, y1, x2, y2);
									if (distance < adjustment) {
										adjustment = distance;
									}
								}
								adjustment = Math.abs(adjustment);
								adj.set(delta_x, delta_y, 0);
								adj.cross(adj, UP);
								adj.normalize();
								adj.scale(adjustment);

								// sidepocket the adjustment info
								adjustment_list.add(segment);

							} else {

								// any subsequent adjustments must occur parallel to the face
								// of the previously fitted segment, otherwise, the adjustment
								// may push the entity back into the previous segment

								SegmentEntity seg0 = adjustment_list.get(0);
								if (!isConcave(seg0, segment)) {
									// if the segment shares a vertex, and turns in the
									// convex direction, then ignore it.
									continue;
								}

								Vector3d vec0_d = seg0.getFaceVector();
								Vector3f adj0_vector = new Vector3f(vec0_d);

								VertexEntity left_ve = segment.getStartVertexEntity();
								left_ve.getPosition(pos_array);
								float x1 = (float)pos_array[0];
								float y1 = (float)pos_array[1];

								VertexEntity rght_ve = segment.getEndVertexEntity();
								rght_ve.getPosition(pos_array);
								float x2 = (float)pos_array[0];
								float y2 = (float)pos_array[1];

								float delta_x = x2 - x1;
								float delta_y = y2 - y1;

								float adjustment = Float.MAX_VALUE;
								for (int j = 0; j < vtx.length; j++) {

									float x0 = vtx[j].x;
									float y0 = vtx[j].y;

									// the distance from each corner of the bounding box
									// to the front wall face
									float distance = getDistance(x0, y0, x1, y1, x2, y2);
									if (distance < adjustment) {
										adjustment = distance;
									}
								}
								adjustment = Math.abs(adjustment);
								adj.set(delta_x, delta_y, 0);
								adj.cross(adj, UP);
								adj.normalize();

								float angle = adj0_vector.angle(adj);
								if (angle > Math.PI/2) {
									adj0_vector.negate();
									angle = (float)(Math.PI - angle);
									if (angle < 0) {
										angle = 0;
									}
								}
								if (angle != 0) {
									float length = (float)(adjustment / Math.sin(angle));
									adj0_vector.normalize();
									adj0_vector.scale(length);
									adj.set(adj0_vector);
								} else {
									adj.scale(adjustment);
								}
								// sidepocket the adjustment info
								adjustment_list.add(segment);
							}

							if (command instanceof MoveEntityTransientCommand){

								MoveEntityTransientCommand cmd =
									(MoveEntityTransientCommand)command;
								cmd.getPosition(pos_array);
								pos_array[0] += adj.x;
								pos_array[1] += adj.y;
								cmd.setPosition(pos_array);

							} else if (command instanceof MoveEntityCommand){

								MoveEntityCommand cmd = (MoveEntityCommand)command;
								cmd.getEndPosition(pos_array);
								pos_array[0] += adj.x;
								pos_array[1] += adj.y;
								cmd.setEndPosition(pos_array);

							} else if (command instanceof AddEntityChildCommand){

								p_entity.getPosition(pos_array);
								pos_array[0] += adj.x;
								pos_array[1] += adj.y;
								p_entity.setPosition(pos_array, false);
							}
							bounds = createBounds(model, command, entity);
						}
					}
				}
			}
		}
		result.setResult(true);
		return(result);
	}

	/**
	 * Create and initialize a bounds object
	 *
	 * @param entity Entity object
	 * @param model WorldModel object
	 * @param command Command object
	 * @return An initialized and transformed bounding box.
	 */
	private OrientedBoundingBox createBounds(
		WorldModel model,
		Command command,
		Entity entity) {

		OrientedBoundingBox bounds = null;

		SurrogateEntityWrapper sur =
			ExpertSceneManagementUtility.createSurrogate(
					rch.getRuleCollisionChecker(), command);

		if (sur != null) {

			bounds = sur.getBounds();

			Matrix4f mtx = TransformUtils.getTransformsInSceneCoordinates(
				model,
				(PositionableEntity)entity,
				true);

			bounds.transform(mtx);

			bounds.getVertices(min, max);

			// initialize the array of bounds vertices,
			// projected on the floor plane
			vtx[0].set(min[0], min[1], 0);
			mtx.transform(vtx[0]);
			vtx[1].set(min[0], max[1], 0);
			mtx.transform(vtx[1]);
			vtx[2].set(max[0], min[1], 0);
			mtx.transform(vtx[2]);
			vtx[3].set(max[0], max[1], 0);
			mtx.transform(vtx[3]);
		}
		return(bounds);
	}

	/**
	 * Determine whether this should care, based on relationship data
	 *
	 * @param rel Entity relationship data
	 * @return true if the necessary relationships exist, false otherwise
	 */
	private boolean validRelationships(String[] rel) {

		boolean valid = false;
		if (rel != null) {
			for (int i = 0; i < rel.length; i++) {
				String r = rel[i];
				for (int j = 0; j < RELATIONSHIPS.length; j++) {
					if (r.contains(RELATIONSHIPS[j])) {
						valid = true;
						break;
					}
				}
				if (valid) {
					break;
				}
			}
		}
		return(valid);
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
	private float getDistance(float x0, float y0, float x1, float y1, float x2, float y2) {

		float delta_x = x2 - x1;
		float delta_y = y2 - y1;

		// 2 dimensional point-line distance,
		// see: http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
		// equation (14)
		float distance = (float)((delta_x * (y1 - y0) - (x1 - x0) * delta_y) /
			Math.sqrt(delta_x * delta_x + delta_y * delta_y));

		return(distance);
	}

	/**
	 * Return the segment that is closest to the center of the bounds
	 *
	 * @param seg_list The segments to check
	 * @param bounds The bounding object to check against
	 * @return The segment that is closest
	 */
/*	private SegmentEntity getClosestToBoundsCenter(
		ArrayList<SegmentEntity> seg_list,
		OrientedBoundingBox bounds) {

		Point3f center = new Point3f();
		bounds.getCenter(center);
		float x0 = center.x;
		float y0 = center.y;

		SegmentEntity segment = null;
		float min_distance = Float.MAX_VALUE;
		for (int i = 0; i < seg_list.size(); i++) {

			SegmentEntity seg = seg_list.get(i);

			VertexEntity left_ve = seg.getStartVertexEntity();
			left_ve.getPosition(pos_array);
			float x1 = (float)pos_array[0];
			float y1 = (float)pos_array[1];

			VertexEntity rght_ve = seg.getEndVertexEntity();
			rght_ve.getPosition(pos_array);
			float x2 = (float)pos_array[0];
			float y2 = (float)pos_array[1];

			// minimum distance from center point to wall face
			float distance = getDistance(x0, y0, x1, y1, x2, y2);

			if (distance < min_distance) {
				min_distance = distance;
				segment = seg;
			}
		}
		return(segment);
	}
*/
	/**
	 * Initialize the seg_list with the set of SegmentEntity
	 * in the scene.
	 */
	private void initSegList() {
		seg_list.clear();
		HashMap<Integer, EntityWrapper> wmap = view.getEntityWrapperMap();
		for (Iterator<EntityWrapper> i = wmap.values().iterator(); i.hasNext(); ) {
			EntityWrapper wrapper = i.next();
			PositionableEntity pe = wrapper.getEntity();
			if (pe instanceof SegmentEntity) {
				SegmentEntity segment = (SegmentEntity)pe;
				seg_list.add(segment);
			}
		}
	}

	/**
	 * Determine whether the junction between the segments is
	 * concave or convex.
	 *
	 * @param seg0 A segment
	 * @param seg1 Another segment
	 * return true if the segments share a vertex and are concave - or
	 * if the segments do not share a vertex. false if the segments share
	 * a vertex and are convex.
	 */
	private boolean isConcave(SegmentEntity seg0, SegmentEntity seg1) {

		boolean concave = true;

		// if there is a shared vertex, determine the segment ordering,
		// then check the cross product for the direction at the
		// junction

		Vector3d v0 = null;
		Vector3d v1 = null;
		boolean sharedVertex = false;

		VertexEntity start_ve0 = seg0.getStartVertexEntity();
		VertexEntity end_ve1 = seg1.getEndVertexEntity();
		if (start_ve0 == end_ve1) {
			v0 = seg1.getFaceVector();
			v1 = seg0.getFaceVector();
			sharedVertex = true;
		} else {
			VertexEntity start_ve1 = seg1.getStartVertexEntity();
			VertexEntity end_ve0 = seg0.getEndVertexEntity();
			if (start_ve1 == end_ve0) {
				v0 = seg0.getFaceVector();
				v1 = seg1.getFaceVector();
				sharedVertex = true;
			}
		}
		if (sharedVertex) {
			cross_prod.cross(v0, v1);
			if (cross_prod.z > 0) {
				concave = false;
			}
		}
		return(concave);
	}

	/**
	 * Determine which segment from the list has the closest rotation
	 * to the argument entity and is wide enough to 'contain' the entity.
	 * If none of the segments are wide enough - just the rotation is
	 * considered.
	 *
	 * @param entity The entity to check against
	 * @param seg_list The segments to check
	 * @return A segment from the list
	 */
	private SegmentEntity snapTo(PositionableEntity entity, ArrayList<SegmentEntity> seg_list) {

		SegmentEntity se = null;

		int num_seg = seg_list.size();

		// determine which if any of the segments is wide enough
		// to support placing the entity against it
		boolean[] wide_enough = new boolean[num_seg];
		boolean a_segment_is_wide_enough = false;

		entity.getSize(size_array);
		entity.getScale(scale_array);
		float entity_width = size_array[0] * scale_array[0];

		for (int i = 0; i < num_seg; i++) {
			SegmentEntity segment = seg_list.get(i);
			float segment_width = segment.getLength();

			if (segment_width < entity_width) {
				wide_enough[i] = false;
			} else {
				wide_enough[i] = true;
				a_segment_is_wide_enough = true;
			}
		}

		// determine which segment's rotation is the closest
		// match to the entity
		entity.getRotation(rot_array);
		float entity_angle = rot_array[3];
		if (entity_angle < 0) {
			// normalize the angle to within a single full rotation
			entity_angle += 2 * Math.PI;
		}

		float min_angle = Float.MAX_VALUE;
		for (int i = 0; i < num_seg; i++) {
			SegmentEntity segment = seg_list.get(i);
			segment.getRotation(rot_array);
			float segment_angle = rot_array[3];
			if (segment_angle < 0) {
				// normalize the angle to within a single full rotation
				segment_angle += 2 * Math.PI;
			}
			float diff = Math.abs(entity_angle - segment_angle);
			if (diff < min_angle) {
				// filter the rotation results based on whether
				// the segment can contain the entity
				if (a_segment_is_wide_enough) {
					if (wide_enough[i]) {
						min_angle = diff;
						se = segment;
					}
				} else {
					min_angle = diff;
					se = segment;
				}
			}
		}
		return(se);
	}

	/**
	 * Return a short String identifier of the argument Entity
	 *
	 * @param entity The entity
	 * @return The identifier
	 */
/*	private String getIdentifier(Entity entity) {
		return("[id="+ entity.getEntityID() + ", name=\""+ entity.getName() +"\"]");
	}
*/	
}
