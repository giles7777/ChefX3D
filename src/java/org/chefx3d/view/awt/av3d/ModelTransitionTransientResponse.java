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
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.MultiTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.RotateEntityCommand;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to Entity transition events.
 *
 * @author Rex Melton
 * @version $Revision: 1.27 $
 */
class ModelTransitionTransientResponse implements TrackerEventResponse {

    private WorldModel model;
    private ErrorReporter reporter;
    private CommandController controller;
    private ViewEnvironment viewEnv;

    /** Multi-Command list */
    private ArrayList<Command> cmdList;

    /** List of entities in a parented hierarchy */
    private ArrayList<Entity> entityList;

    /** List of descendant entities in a parented hierarchy */
    private ArrayList<Entity> descendantList;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** Instance of hierarchy transformation calculator */
    private TransformUtils tu;

    /** Scratch vecmath objects */
    private AxisAngle4f aa;
    private Matrix3f rmtx0;
    private Matrix3f rmtx1;
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
    private float[] newEntityRotation;
    private float[] originalEntityRotation;
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
     * Constructor
	 *
     * @param model
     * @param controller
     * @param reporter
     * @param viewEnv
	 * @param editorGrid
     */
    ModelTransitionTransientResponse(
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
        aa = new AxisAngle4f();
        rmtx0 = new Matrix3f();
        rmtx1 = new Matrix3f();
        mtx = new Matrix4f();
        emtx = new Matrix4f();
        pnt = new Point3f();
        position = new Point3f();
		vec = new Vector3f();
		translation = new Vector3f();
		rotation = new AxisAngle4f();

        newEntityPosition = new double[3];
        originalEntityPosition = new double[3];
        newEntityRotation = new float[4];
        originalEntityRotation = new float[4];
        scale = new float[3];
        size = new float[3];
        velocity = new float[3];
        rotation_array = new float[4];
        position_array = new double[3];
        frustum = new double[6];

        cmdList = new ArrayList<Command>();
        entityList = new ArrayList<Entity>();
        descendantList = new ArrayList<Entity>();

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

        float[] newMouseDevicePosition = trackerState.devicePos;

        float deltaRt = (newMouseDevicePosition[0] - actionData.mouseDevicePosition[0]);
        float deltaUp = (actionData.mouseDevicePosition[1] - newMouseDevicePosition[1]);

        viewEnv.getViewFrustum(frustum);

        float f_width = (float)(frustum[1] - frustum[0]);
        float f_height = (float)(frustum[3] - frustum[2]);

        deltaRt *= f_width;
        deltaUp *= f_height;

        Entity zoneEntity = actionData.zoneWrapper.entity;

        // should only allow entities of the same type to move.
        PositionableEntity prevEntity = null;
        for (int i = 0; i < entities.length; i++) {
            PositionableEntity entity = (PositionableEntity)entities[i];
            if (prevEntity != null && prevEntity.getType()!= entity.getType()) {
                return;
            }
            prevEntity = entity;
        }
        if (!inProgress) {
			entityList.clear();
			descendantList.clear();
			for (int i = 0; i < entities.length; i++) {
				Entity entity = entities[i];
				if (entity != null) {
					// assemble the list of entities in each entity
					// hierarchy. this will be the pick exclusion list
					initEntities(entity);
					getDescendants(entity, descendantList);
				}
			}
		}
        // Issue a command for each individual entity
        for (int i = 0; i < entities.length; i++) {

            cmdList.clear();

            PositionableEntity entity = (PositionableEntity)entities[i];
            if (entity != null) {

                if (!inProgress) {
					if (descendantList.contains(entity)) {

						// deselect it, descendants go along for the ride with their ancestors
						Command cmd = new SelectEntityCommand(model, entity, false);
						cmd.setErrorReporter(reporter);
						controller.execute(cmd);

					} else {
						// the first time through, ensure that the entity
						// is parented to the zone
						int parentID = entity.getParentEntityID();
						if (parentID != zoneEntity.getEntityID()) {

							AV3DEntityWrapper parentWrapper = actionData.wrapperMap.get(parentID);
							Entity parent = parentWrapper.entity;

							// convert the entity position into zone relative coordinates
							entity.getStartingPosition(position_array);
							entity.getStartingRotation(originalEntityRotation);

							pnt.x = (float)position_array[0];
							pnt.y = (float)position_array[1];
							pnt.z = (float)position_array[2];

							tu.getLocalToVworld(
								parentWrapper.transformGroup,
								actionData.zoneWrapper.transformGroup,
								mtx);
							mtx.transform(pnt);

							newEntityPosition[0] = pnt.x;
							newEntityPosition[1] = pnt.y;
							newEntityPosition[2] = pnt.z;

							// convert the entity rotation into zone relative
							aa.set(mtx);
							rmtx0.set(aa);
							aa.set(originalEntityRotation);
							rmtx1.set(aa);
							rmtx0.mul(rmtx1);
							aa.set(rmtx0);
							aa.get(newEntityRotation);

							// unselect it
							Command cmd = new SelectEntityCommand(model, entity, false);
							cmd.setErrorReporter(reporter);
							cmdList.add(cmd);

							entity.getStartingPosition(originalEntityPosition);

							editorGrid.alignPositionToGrid(newEntityPosition);
		
							// reparent
							cmd = new TransitionEntityChildCommand(
								model,
								entity,
								parent,
								originalEntityPosition,
								originalEntityRotation,
								zoneEntity,
								newEntityPosition,
								newEntityRotation,
								true);
							cmd.setErrorReporter(reporter);
							cmdList.add(cmd);

							// select it
							cmd = new SelectEntityCommand(model, entity, true);
							cmd.setErrorReporter(reporter);
							cmdList.add(cmd);

							MultiTransientCommand multiCmd = new MultiTransientCommand(
								cmdList,
								"Transition Entities");
							multiCmd.setErrorReporter(reporter);
							controller.execute(multiCmd);
						}
					}
                } else {

					// get the parameters of the entity
					entity.getStartingPosition(originalEntityPosition);
					entity.getStartingScale(scale);
					entity.getSize(size);

					size[0] *= scale[0];
					size[1] *= scale[1];
					size[2] *= scale[2];

					// working variables
					PickData pd = null;
					PickData zone_pd = null;

					// do a new pick for this entity
					pnt.x = (float)originalEntityPosition[0] + deltaRt;
					pnt.y = (float)originalEntityPosition[1] + deltaUp;
					pnt.z = (float)originalEntityPosition[2] + 100;

					int parentID = entity.getParentEntityID();
					AV3DEntityWrapper parentWrapper = actionData.wrapperMap.get(parentID);

					tu.getLocalToVworld(
						parentWrapper.transformGroup,
						mtx);
					mtx.transform(pnt);

					trackerState.worldPos[0] = pnt.x;
					trackerState.worldPos[1] = pnt.y;
					trackerState.worldPos[2] = pnt.z;

					// do a new pick based on the center of the currently processing
					// entity
					actionData.pickManager.doPick(trackerState);

					ArrayList<PickData> pickList = actionData.pickManager.getResults();
					int num_pick = pickList.size();
					for (int j = 0; j < num_pick; j++) {
						// find the closest object associated with an entity
						// that is NOT the zone entity and is not a part of
						// the entity hierarchy that is being transformed
						PickData tpd = pickList.get(j);
						if ((tpd.object instanceof Entity) &&
							!(entityList.contains(tpd.object)) &&
							!(tpd.object == entity)) {
							if (tpd.object != zoneEntity) {

		                        pd = tpd;
		                        break;
							}
						}
					}
					zone_pd = actionData.pickManager.getResult(zoneEntity);

					boolean useCollision = false;
					double parent_elevation = 0;
					PositionableEntity collision_entity = null;
					
//					if (pd == null) {
						// if no entity (not including the zone) is directly
						// beneath the entity position, do a bounds check for
						// an entity that is in contact

						// calculate the bounds transform for the entity,
						// taking into account the change caused by the
						// current mouse movement
            			entity.getStartingPosition(position_array);
						position_array[0] += deltaRt;
						position_array[1] += deltaUp;
            			entity.getRotation(rotation_array);

            			rotation.set(rotation_array);
            			translation.set(
							(float)position_array[0],
							(float)position_array[1],
							(float)position_array[2]);

						emtx.setIdentity();
            			emtx.setRotation(rotation);
            			emtx.setTranslation(translation);

            			emtx.mul(mtx, emtx);

						// do a check against the other entities that are
						// in the zone's hierarchy
						zcm.check(entity, emtx, collisionList);

						int num_collision = collisionList.size();
						
						if (num_collision > 0) {
							// if we have found an intersection with another entity,
							// determine the closest one and use it as our potential
							// 'parent'

							useCollision = true;

							// get the zone relative position of the entity
							vec.set((float)deltaRt, (float)deltaUp, 0);
							zcm.toZoneRelative(entity, vec, position);

							// find the closest intersecting entity
							collision_entity =
//								zcm.findClosest(position, collisionList);
								zcm.findHighest(collisionList);

							float[] c_scale = new float[3];
							float[] c_size = new float[3];
							collision_entity.getScale(c_scale);
							collision_entity.getSize(c_size);

							c_size[0] *= c_scale[0];
							c_size[1] *= c_scale[1];
							c_size[2] *= c_scale[2];

							parentWrapper = actionData.wrapperMap.get(
								collision_entity.getParentEntityID());

							if (parentWrapper != actionData.zoneWrapper) {

								zcm.toZoneRelative(collision_entity, null, pnt);
								parent_elevation = pnt.z + c_size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

							} else {
								parent_elevation = c_size[2] - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
							}
						}
//					}

					newEntityPosition[0] = originalEntityPosition[0] + deltaRt;
					newEntityPosition[1] = originalEntityPosition[1] + deltaUp;

					float elevation_relative_to_parent = size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
					
					if (useCollision) {
						newEntityPosition[2] = parent_elevation + elevation_relative_to_parent;
						
					} else if ((zone_pd != null) && (pd != null)) {
						// place relative to the entity that has been picked first
						newEntityPosition[2] = zone_pd.distance - pd.distance + elevation_relative_to_parent;
						
					} else {
						// place in the zone's plane
						newEntityPosition[2] = elevation_relative_to_parent;
					}

					Entity pickEntity = null;
					if (useCollision) {
					    pickEntity = collision_entity;
					} else if ((pd != null) && (pd.object != null)) {
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
        inProgress = true;
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

    /**
     * Initialize the list of entities in a hierarchy
     */
    void initEntities(Entity entity) {
        entityList.add(entity);
        if (entity.hasChildren()) {
            ArrayList<Entity> children = entity.getChildren();
            for (int i = 0; i < children.size(); i++) {
                entityList.add(children.get(i));
            }
        }
    }

	/**
	 * Aggregate the children of the specified entity
	 * into the argument list
	 *
	 * @param entity The Entity whose children to gather
	 * @param list The List to place them in
	 */
	void getDescendants(Entity entity, List<Entity> list) {
		if (entity.hasChildren()) {
			ArrayList<Entity> children = entity.getChildren();
			list.addAll(children);
			for (int i = 0; i < children.size(); i++) {
				Entity child = children.get(i);
				getDescendants(child, list);
			}
		}
	}
}

