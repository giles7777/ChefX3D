/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

//External imports
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to transient Entity move events.
 *
 * @author Rex Melton
 * @version $Revision: 1.9 $
 */
class ModelMoveTransientResponse implements TrackerEventResponse {

	private WorldModel model;
	private ErrorReporter reporter;
	private CommandController controller;
	private ViewEnvironment viewEnv;

	/** Multi-Command list */
	private ArrayList<Command> cmdList;

	/** The initial conditions for this action */
	private ActionData actionData;

	/** Instance of hierarchy transformation calculator */
	private TransformUtils tu;

	/** Scratch vecmath objects */
	private Matrix4f mtx;
	private Matrix4f emtx;
	private Point3f pnt;
	private Point3f position;
	private Vector3f vec;
	private Vector3f translation;
	private AxisAngle4f rotation;

	/** Scratch arrays */
	private double[] newEntityPosition;
	private double[] originalEntityPosition;
	private float[] scale;
	private float[] size;
	private float[] velocity;
	private float[] rotation_array;
	private double[] position_array;
	private double[] frustum;

	/** Flag indicating that the transition is in progress */
	private boolean inProgress;

	/** The zone collision manager */
	private ZoneCollisionManager zcm;

	/** List of collision results with children of the zone entity */
	private ArrayList<Entity> collisionList;

	/** Utility for aligning the model with the editor grid */
	private EditorGrid editorGrid;
	
	/**
	 * Constructor.
	 *
     * @param model
     * @param controller
     * @param reporter
     * @param viewEnv
	 * @param editorGrid
	 */
	ModelMoveTransientResponse(
		WorldModel model,
		CommandController controller,
		ErrorReporter reporter,
		ViewEnvironment viewEnv,
		EditorGrid editorGrid) {

		this.model = model;
		this.reporter = reporter;
		this.controller = controller;
		this.viewEnv = viewEnv;
		this.editorGrid = editorGrid;

		tu = new TransformUtils();
		mtx = new Matrix4f();
		emtx = new Matrix4f();
		pnt = new Point3f();
		position = new Point3f();
		vec = new Vector3f();
		translation = new Vector3f();
		rotation = new AxisAngle4f();

		newEntityPosition = new double[3];
		originalEntityPosition = new double[3];
		scale = new float[3];
		size = new float[3];
		velocity = new float[3];
		rotation_array = new float[4];
		position_array = new double[3];
		frustum = new double[6];

		cmdList = new ArrayList<Command>();

		zcm = new ZoneCollisionManager();
		collisionList = new ArrayList<Entity>();
	}

	//---------------------------------------------------------------
	// Methods defined by TrackerEventResponse
	//---------------------------------------------------------------

	/**
	 * Begins the processing required to generate a command in response
	 * to the input received.
	 *
	 * @param trackerID The id of the tracker calling the original handler
	 * @param trackerState The event that started this whole thing off
	 * @param entities The array of entities to handle
	 * @param tool The tool that is used in the action (can be null)
	 */
	public void doEventResponse(
		int trackerID,
		TrackerState trackerState,
		Entity[] entities,
		Tool tool) {

		Entity zoneEntity = actionData.zoneWrapper.entity;

		// Issue a command for each individual entity
		for (int i = 0; i < entities.length; i++) {

			cmdList.clear();

			PositionableEntity entity = (PositionableEntity)entities[i];
			if (entity != null) {

				// get the parameters of the entity
				entity.getStartingScale(scale);
				entity.getSize(size);

				size[0] *= scale[0];
				size[1] *= scale[1];
				size[2] *= scale[2];

				// working variables
				PickData pd = null;
				PickData zone_pd = actionData.pickManager.getResult(zoneEntity);

				ArrayList<PickData> pickList = actionData.pickManager.getResults();
				int num_pick = pickList.size();
				for (int j = 0; j < num_pick; j++) {
					// find the closest object associated with an entity
					// that is NOT the zone entity and is not a part of
					// the entity hierarchy that is being transformed
					PickData tpd = pickList.get(j);
					if ((tpd.object instanceof Entity) &&
						(tpd.object != entity) &&
						(tpd.object != zoneEntity)) {

						pd = tpd;
						break;
					}
				}

				// calculate position relative to the zone
				mtx.invert(zone_pd.mtx);
				mtx.transform(zone_pd.point);

				newEntityPosition[0] = zone_pd.point.x;
				newEntityPosition[1] = zone_pd.point.y;

				boolean useCollision = false;
				double parent_elevation = 0;

//				if (pd == null) {
					// if no entity (not including the zone) is directly
					// beneath the entity position, do a bounds check for
					// an entity that is in contact

					// calculate the bounds transform for the entity
					entity.getPosition(position_array);
					position_array[0] = newEntityPosition[0];
					position_array[1] = newEntityPosition[1];

					entity.getRotation(rotation_array);

					rotation.set(rotation_array);
					translation.set(
						(float)position_array[0],
						(float)position_array[1],
						(float)position_array[2]);

					emtx.setIdentity();
					emtx.setRotation(rotation);
					emtx.setTranslation(translation);

					emtx.mul(zone_pd.mtx, emtx);

					// do a check against the other entities that are
					// in the zone's hierarchy
					zcm.check(entity, emtx, collisionList);

					int num_collision = collisionList.size();
					if (num_collision > 0) {
						// if we have found an intersection with another entity,
						// determine the closest one and use it

						useCollision = true;

						// the zone relative position of the entity
						position.x = (float)position_array[0];
						position.y = (float)position_array[1];
						position.z = (float)position_array[2];

						// find the closest intersecting entity
						PositionableEntity collision_entity =
//							zcm.findClosest(position, collisionList);
							zcm.findHighest(collisionList);

						float[] c_scale = new float[3];
						float[] c_size = new float[3];
						collision_entity.getScale(c_scale);
						collision_entity.getSize(c_size);

						c_size[0] *= c_scale[0];
						c_size[1] *= c_scale[1];
						c_size[2] *= c_scale[2];

						AV3DEntityWrapper wrapper = actionData.wrapperMap.get(
							collision_entity.getParentEntityID());

						if (wrapper != actionData.zoneWrapper) {

							zcm.toZoneRelative(collision_entity, null, pnt);
							parent_elevation = pnt.z + c_size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

						} else {
							parent_elevation = c_size[2] - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
						}
					}
//				}

				float elevation_relative_to_parent = size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
				if (useCollision) {
					newEntityPosition[2] = parent_elevation + elevation_relative_to_parent;
					
				} else if (zone_pd != null && pd != null) {
					// place relative to the entity that has been picked first
					newEntityPosition[2] = zone_pd.distance - pd.distance + elevation_relative_to_parent;
					
				} else {
					// place in the zone's plane
					newEntityPosition[2] = elevation_relative_to_parent;
				}

				Entity pickEntity = null;
				if ((pd != null) && (pd.object != null)) {
					pickEntity = (Entity) pd.object;
				}

				editorGrid.alignPositionToGrid(newEntityPosition);
		
				Command cmd = new MoveEntityTransientCommand(
					model,
					model.issueTransactionID(),
					entity.getEntityID(),
					newEntityPosition,
					velocity,
					pickEntity);

				cmd.setErrorReporter(reporter);
				controller.execute(cmd);
			}
		}
	}

	//---------------------------------------------------------------
	// Local Methods
	//---------------------------------------------------------------

	/**
	 * Initialize in preparation for a response
	 *
	 * @param ad The initial device position of the mouse
	 */
	void setActionData(ActionData ad) {
		actionData = ad;
		inProgress = false;

		zcm.setWrapperMap(actionData.wrapperMap);
		zcm.setActiveZoneEntity((ZoneEntity)actionData.zoneWrapper.entity);
	}
}

