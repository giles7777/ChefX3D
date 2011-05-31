/*****************************************************************************
*                        Copyright Yumetech, Inc (c) 2009 - 2010
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
import org.chefx3d.model.MultiCommand;
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
 * @version $Revision: 1.32 $
 */
class ModelTransitionResponse implements TrackerEventResponse {

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
    private Point3f pnt;

    /** Scratch arrays */
    private double[] newEntityPosition;
    private double[] originalEntityPosition;
    private float[] newEntityRotation;
    private float[] originalEntityRotation;
    private float[] scale;
    private float[] size;
    private double[] frustum;

	/** The zone collision manager */
	private ZoneCollisionManager zcm;

    /** List of collision results with children of the zone entity */
    private ArrayList<Entity> collisionList;

    private Matrix4f emtx;
    private Point3f position;
	private Vector3f vec;
	private Vector3f translation;
	private AxisAngle4f rotation;

	private float[] rotation_array;
    private double[] position_array;

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
    ModelTransitionResponse(
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
        pnt = new Point3f();

        newEntityPosition = new double[3];
        originalEntityPosition = new double[3];
        newEntityRotation = new float[4];
        originalEntityRotation = new float[4];
        scale = new float[3];
        size = new float[3];
        frustum = new double[6];

        cmdList = new ArrayList<Command>();
        entityList = new ArrayList<Entity>();
        descendantList = new ArrayList<Entity>();

		zcm = new ZoneCollisionManager();
        collisionList = new ArrayList<Entity>();

        rotation_array = new float[4];
        position_array = new double[3];

        emtx = new Matrix4f();
        position = new Point3f();
		vec = new Vector3f();
		translation = new Vector3f();
		rotation = new AxisAngle4f();
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

        ZoneEntity zoneEntity = (ZoneEntity)actionData.zoneWrapper.entity;

        // should only allow entities of the same type to move.
        PositionableEntity prevEntity = null;
        for (int i = 0; i < entities.length; i++) {
            PositionableEntity entity = (PositionableEntity)entities[i];
            if (prevEntity != null && prevEntity.getType()!= entity.getType()) {
                return;
            }
            prevEntity = entity;
        }

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

        // get the list of entity data, assume same order as selection list
        ArrayList<ActionData.EntityData> entityDataList = actionData.entityList;                

        cmdList.clear();
        // Issue a command for each individual entity
        for (int i = 0; i < entities.length; i++) {

            PositionableEntity entity = (PositionableEntity)entities[i];
            if ((entity != null) && !descendantList.contains(entity)) {
				
                // assemble the list of entities in the hierarchy
                entityList.clear();
                initEntities(entity);

                int parentID = entity.getParentEntityID();
                AV3DEntityWrapper parentWrapper = actionData.wrapperMap.get(parentID);

                if (parentWrapper == null) {
                    continue;
				}
				
				// the current parent -should- always be the zone
                Entity currentParent = parentWrapper.entity;
				
				if (currentParent != zoneEntity) {
					// tmp debug
					System.out.println("MTR: Warning! Current parent is not the zone.");
				}

				// get the current parent transform
				tu.getLocalToVworld(
					parentWrapper.transformGroup,
					mtx);
				
                // determine the original parent of the entity
                Entity originalParent = null;
                ActionData.EntityData dat = null;
                for (int j = 0; j < actionData.entityList.size(); j++) {
                    ActionData.EntityData tmp_dat = actionData.entityList.get(j);
                    if (tmp_dat.entity == entity) {
                        dat = tmp_dat;
                        originalParent = model.getEntity(dat.parentEntityID);
                        break;
                    }
                }
				
				PositionableEntity newParent = null;

				//////////////////////////////////////////////////////////////
				// do a bounds check for an entity that is in contact
				
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
				
				if (!collisionList.isEmpty()) {
					int num_collision = collisionList.size();
					for (int j = num_collision - 1; j >= 0; j--) {
						// prevent other entities in the set being moved
						// from being considered as potential parents
						Entity c_entity = collisionList.get(j);
						for (int k = 0; k < entities.length; k++) {
							if (entities[k] == c_entity) {
								collisionList.remove(j);
								break;
							}
						}
					}
					if (!collisionList.isEmpty()) {
						// if we have found an intersection with another entity,
						// determine the one with the highest elevation and use
						// it as our potential 'parent'
						PositionableEntity collision_entity =
							zcm.findHighest(collisionList);
						
						newParent = collision_entity;
					}
				}
				//////////////////////////////////////////////////////////////

				if (newParent == null) {
					
					// if the collision check did not produce a parent, 
					// do a pick to determine what is under the entity
					
					// do a new pick for this entity
					entity.getStartingPosition(position_array);
					position_array[0] += deltaRt;
					position_array[1] += deltaUp;
					
					pnt.x = (float)position_array[0];
					pnt.y = (float)position_array[1];
					pnt.z = (float)position_array[2] + 100;
					
					mtx.transform(pnt);
					
					trackerState.worldPos[0] = pnt.x;
					trackerState.worldPos[1] = pnt.y;
					trackerState.worldPos[2] = pnt.z;
					
					actionData.pickManager.doPick(trackerState);
					if (zoneEntity.getType() == Entity.TYPE_MODEL_ZONE) {
						actionData.pickManager.filterResultsByParentZone(zoneEntity);
					}
					// determine the new parent. if the pick was not over an entity,
					// this will be null
					ArrayList<PickData> pickList = actionData.pickManager.getResults();
					for (int j = 0; j < pickList.size(); j++) {
						// find the closest object associated with an entity
						// that is NOT the zone entity and is not a part of
						// the entity hierarchy that is being transformed
						PickData tpd = pickList.get(j);
						if ((tpd.object instanceof Entity) &&
							!(entityList.contains(tpd.object)) &&
							!(tpd.object == entity)) {
							
							if (tpd.object != zoneEntity) {
								
								newParent = (PositionableEntity)tpd.object;
							}
							break;
						}
					}
				}
				//////////////////////////////////////////////////////////////
				
				// get the parameters for position calculations
				entity.getStartingScale(scale);
				entity.getSize(size);
				
				size[0] *= scale[0];
				size[1] *= scale[1];
				size[2] *= scale[2];
				
				float elevation_relative_to_parent = 
					size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
				
                // decide whether reparenting is necessary, or just a move
                boolean reparent = false;
                if (newParent == null) {
                    // the mouse is not over an entity, if the original
                    // parent is not the zone, reparent to the zone
                    if (originalParent != zoneEntity) {
                        reparent = true;
                        newParent = (PositionableEntity)zoneEntity;
                    }
					
				} else {
					// the mouse IS over an entity. if anything has changed,
					// reparenting is necessary
					if ((newParent != originalParent) ||
						(newParent != currentParent) ||
						(originalParent != currentParent)) {
						
						reparent = true;
					}
				}
                ///////////////////////////////////////////
                if (reparent) {
					
                	entity.getRotation(originalEntityRotation);
					entity.getPosition(position_array);
					
					float[] p_scale = new float[3];
					float[] p_size = new float[3];
					newParent.getScale(p_scale);
					newParent.getSize(p_size);
					
					p_size[0] *= p_scale[0];
					p_size[1] *= p_scale[1];
					p_size[2] *= p_scale[2];
						
                    // unselect it
                    Command cmd = new SelectEntityCommand(model, entity, false);
                    cmd.setErrorReporter(reporter);
                    cmdList.add(cmd);

                    if (newParent != zoneEntity) {

						// get the elevation in zone relative
						zcm.toZoneRelative(newParent, null, position);
						float parent_elevation = 
							position.z + p_size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
					
						position_array[2] = parent_elevation + elevation_relative_to_parent;
						
                        parentID = newParent.getEntityID();
                        parentWrapper = actionData.wrapperMap.get(parentID);

                        // convert the entity position into parent relative coordinates
                        pnt.x = (float)position_array[0];
                        pnt.y = (float)position_array[1];
                        pnt.z = (float)position_array[2];
						
                        tu.getLocalToVworld(
                            parentWrapper.transformGroup,
                            actionData.zoneWrapper.transformGroup,
                            mtx);
                        mtx.invert();
                        mtx.transform(pnt);

                        newEntityPosition[0] = pnt.x;
                        newEntityPosition[1] = pnt.y;
                        newEntityPosition[2] = pnt.z;

                        // convert the entity rotation into parent relative
                        aa.set(mtx);
                        rmtx0.set(aa);
                        aa.set(originalEntityRotation);
                        rmtx1.set(aa);
                        rmtx0.mul(rmtx1);
                        aa.set(rmtx0);
                        aa.get(newEntityRotation);

					} else {
						
						// the new parent is the zone
						position_array[2] = elevation_relative_to_parent;
						
                        newEntityPosition[0] = position_array[0];
                        newEntityPosition[1] = position_array[1];
                        newEntityPosition[2] = position_array[2];
						
                        newEntityRotation[0] = originalEntityRotation[0];
                        newEntityRotation[1] = originalEntityRotation[1];
                        newEntityRotation[2] = originalEntityRotation[2];
                        newEntityRotation[3] = originalEntityRotation[3];
                    }
                    ///////////////////////////////////////////
                    if (dat != null) {
                        originalEntityPosition[0] = dat.position[0];
                        originalEntityPosition[1] = dat.position[1];
                        originalEntityPosition[2] = dat.position[2];

                        originalEntityRotation[0] = dat.rotation[0];
                        originalEntityRotation[1] = dat.rotation[1];
                        originalEntityRotation[2] = dat.rotation[2];
                        originalEntityRotation[3] = dat.rotation[3];
                    }
                    ///////////////////////////////////////////

                    // get the original children list
                    ArrayList<Entity> startChildren = new ArrayList<Entity>();
                    if (entityDataList != null && entityDataList.size() - 1 >= i) {
                        startChildren = actionData.entityList.get(i).startChildren;                
                    }

                    
					editorGrid.alignPositionToGrid(newEntityPosition);
		
					float[] endScale = new float[3];
					entity.getScale(endScale);
					
                    cmd = new TransitionEntityChildCommand(
                        model,
                        entity,
                        originalParent,
                        originalEntityPosition,
                        originalEntityRotation,
                        scale, 
                        newParent,
                        newEntityPosition,
                        newEntityRotation,
                        endScale, 
                        false);
                    
                    ((TransitionEntityChildCommand)cmd).setStartChildren(startChildren);

                    cmd.setErrorReporter(reporter);
                    cmdList.add(cmd);

                    // select it
                    cmd = new SelectEntityCommand(model, entity, true);
                    cmd.setErrorReporter(reporter);
                    cmdList.add(cmd);

				} else {
					// it's in the zone
					
					newEntityPosition[0] = dat.position[0] + deltaRt;
					newEntityPosition[1] = dat.position[1] + deltaUp;
					newEntityPosition[2] = elevation_relative_to_parent;
					
					int zoneType = zoneEntity.getType();
					if (zoneType == Entity.TYPE_SEGMENT) {
						// certain types of models are embedded into the cut-outs
						// in the segment, rather than being placed on the surface
						String category = entity.getCategory();
						if ((category != null) && (
							category.equals("Category.Window") ||
							category.equals("Category.Door"))) {
							
							newEntityPosition[2] = -elevation_relative_to_parent;
						}
					}
					////////////////////////////////////////////////
					originalEntityPosition[0] = dat.position[0];
					originalEntityPosition[1] = dat.position[1];
					originalEntityPosition[2] = dat.position[2];
					////////////////////////////////////////////////
					
					// get the original children list
					ArrayList<Entity> startChildren = new ArrayList<Entity>();
					if (entityDataList != null && entityDataList.size() - 1 >= i) {
						startChildren = actionData.entityList.get(i).startChildren;                
					}
					
					editorGrid.alignPositionToGrid(newEntityPosition);

					Command cmd = new MoveEntityCommand(
						model,
						model.issueTransactionID(),
						(PositionableEntity)entity,
						newEntityPosition,
						originalEntityPosition, 
						startChildren);
					
					cmd.setErrorReporter(reporter);
					cmdList.add(cmd);
				}
			}
        }
		if (cmdList.size() > 0) {

			MultiCommand multiCmd = new MultiCommand(
				cmdList,
				"Transition Entities");
			multiCmd.setErrorReporter(reporter);

			controller.execute(multiCmd);
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

