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
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.rules.properties.accessors.IgnoreRuleList;
import org.chefx3d.tool.*;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Responds to add default entity events. The appropriate command is issued
 * for the event.
 *
 * @author Ben Yarger
 * @version $Revision: 1.41 $
 */
class AddEntityResponse implements TrackerEventResponse {

    /** Reference to world model */
    private WorldModel model;

    /** The controlleer to send commands to */
    private CommandController controller;

    /** Reference to error reporter */
    private ErrorReporter reporter;

    /** Reference to entity builder */
    private EntityBuilder entityBuilder;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** The PickManager */
    private PickManager pickManager;

	/** The zone collision manager */
	private ZoneCollisionManager zcm;

    /** The map of entity wrappers */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;

	/** The zone entity */
	private ZoneEntity zoneEntity;

	/** The working zone entity wrapper */
	private AV3DEntityWrapper zoneWrapper;

    /** List of collision results with children of the zone entity */
    private ArrayList<Entity> collisionList;

    /** Instance of hierarchy transformation calculator */
    private TransformUtils tu;

    /** Scratch vecmath objects */
    private Matrix4f mtx;
	private Vector3f vec;
	private Point3f pnt;

    private float[] scale_array;
    private float[] size_array;

    /** The bounds for the new entity */
    private OrientedBoundingBox obb;

    /** Bound extents */
    private float[] min;
    private float[] max;

	/** Utility for aligning the model with the editor grid */
	private EditorGrid editorGrid;
	
    /**
     * Constructor
     *
     * @param model
     * @param controller
     * @param reporter
	 * @param editorGrid
     */
	AddEntityResponse(
		WorldModel model,
		CommandController controller,
		ErrorReporter reporter,
		EditorGrid editorGrid) {

        this.model = model;
        this.controller = controller;
        this.reporter = reporter;
		this.editorGrid = editorGrid;
        entityBuilder = DefaultEntityBuilder.getEntityBuilder();

        tu = new TransformUtils();
        mtx = new Matrix4f();
		vec = new Vector3f();
		pnt = new Point3f();

		zcm = new ZoneCollisionManager();
        collisionList = new ArrayList<Entity>();

        obb = new OrientedBoundingBox();

        scale_array = new float[3];
        size_array = new float[3];
        min = new float[3];
        max = new float[3];
    }

    /**
     * Begins the processing required to generate a command in response
     * to the input received.
     *
     * @param trackerID The id of the tracker calling the original handler
     * @param trackerState The event that started this whole thing off
     * @param entities The array of entities to handle
     * @param tool The tool that is used in the action
     */
	public void doEventResponse(
		int trackerID,
		TrackerState trackerState,
		Entity[] entities,
		Tool tool) {
		
		pickManager.doPick(trackerState);
		if (zoneEntity.getType() == Entity.TYPE_MODEL_ZONE) {
			pickManager.filterResultsByParentZone(zoneEntity);
		}
		if (pickManager.hasResults()) {

			PickData pd = null;
			PickData zone_pd = pickManager.getResult(zoneEntity);
			if (zone_pd == null) {
				return;
			}

			ArrayList<PickData> pickList = pickManager.getResults();
            int num_pick = pickList.size();
            for (int j = 0; j < num_pick; j++) {
                // find the closest object associated with an entity
                // that is NOT the zone entity and is not a part of
                // the entity hierarchy that is being transformed
                PickData tpd = pickList.get(j);
                
                // Also, don't pick shadow entities
                PositionableEntity eCheck = (PositionableEntity) tpd.object;
                Boolean isShadow = (Boolean) 
                	eCheck.getProperty(
                			eCheck.getParamSheetName(), 
                			Entity.SHADOW_ENTITY_FLAG);
                
                if (isShadow != null && isShadow) {
                	continue;
                }
                
                if (tpd.object instanceof Entity && tpd.object != zoneEntity) {

					pd = tpd;
                    break;
                }
            }

			PositionableEntity parentEntity = null;

			float[] size = tool.getSize();
			double[] position = new double[3];
			float[] rotation = new float[]{0, 1, 0, 0};

			if (pd != null) {
				// parent to the entity (not the zone) that is under the
			    // pick point
				parentEntity = (PositionableEntity)pd.object;
				///////////////////////////////////////////////////////////
				// rem: for extrusion entities, the pick matrix is not
				// the same as the entity's matrix, since the extrusion
				// has multiple pieces of geometry that are transformed 
				// into place
				//mtx.invert(pd.mtx);
				//mtx.transform(pd.point);
				///////////////////////////////////////////////////////////
				AV3DEntityWrapper parentWrapper = wrapperMap.get(
						parentEntity.getEntityID());

				// transform to parent entity relative
				tu.getLocalToVworld(
					parentWrapper.transformGroup,
					mtx);
				mtx.invert();
				mtx.transform(pd.point);
				///////////////////////////////////////////////////////////
				
				position[0] = pd.point.x;
				position[1] = pd.point.y;
				position[2] = pd.point.z + (size[2] * 0.5f);

			} else {
				// no other entity is under the pick point.
				// determine if the bounds of this entity are in
				// contact with another entity (that is not the zone).

				// calculate position relative to the zone
				mtx.invert(zone_pd.mtx);
				mtx.transform(zone_pd.point);

				// transform a bounding box into place
				max[0] = size[0] * 0.5f;
				max[1] = size[1] * 0.5f;
				max[2] = size[2] - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

				min[0] = -max[0];
				min[1] = -max[1];
				min[2] = -AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

				max[0] += zone_pd.point.x;
				max[1] += zone_pd.point.y;
				max[2] += zone_pd.point.z;

				min[0] += zone_pd.point.x;
				min[1] += zone_pd.point.y;
				min[2] += zone_pd.point.z;

				obb.setVertices(min, max);
				obb.transform(zone_pd.mtx);

				// do the intersection test
				collisionList.clear();
				zcm.check(zoneEntity, null, obb, collisionList);

				// remove any shadow entities from the list.
				// We cannot parent to a shadow entity.
				for (int i = (collisionList.size() - 1); i >= 0; i--) {
					
	                Boolean isShadow = (Boolean) 
	                	collisionList.get(i).getProperty(
	                			collisionList.get(i).getParamSheetName(), 
	                			Entity.SHADOW_ENTITY_FLAG);
	                
	                if (isShadow != null && isShadow) {
	                	collisionList.remove(i);
	                }
				}
                
                int num_collision = collisionList.size();
				if (num_collision > 0) {
					// an intersection has been found, determine the closest
					// to this entity's bounds and parent to it.

					// pnt == the center of the bounds, in zone relative coords
					pnt.x = zone_pd.point.x;
					pnt.y = zone_pd.point.y;
					pnt.z = max[2] * 0.5f;

					parentEntity = zcm.findClosest(pnt, collisionList);
					
					// determine the zone relative elevation (z) of the entity
					float[] c_scale = new float[3];
					float[] c_size = new float[3];
					parentEntity.getScale(c_scale);
					parentEntity.getSize(c_size);

					c_size[0] *= c_scale[0];
					c_size[1] *= c_scale[1];
					c_size[2] *= c_scale[2];

					double zone_relative_z = (size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH);
					if (parentEntity.getParentEntityID() != zoneEntity.getEntityID()) {

						zcm.toZoneRelative(parentEntity, null, pnt);
						zone_relative_z += pnt.z + c_size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

					} else {
						zone_relative_z += c_size[2] - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
					}

					pnt.x = zone_pd.point.x;
					pnt.y = zone_pd.point.y;
					pnt.z = (float)zone_relative_z;

					AV3DEntityWrapper zoneWrapper = wrapperMap.get(
						zoneEntity.getEntityID());
					AV3DEntityWrapper parentWrapper = wrapperMap.get(
						parentEntity.getEntityID());

					// transform from zone relative to parent entity relative
					tu.getLocalToVworld(
						parentWrapper.transformGroup,
						zoneWrapper.transformGroup,
						mtx);
					mtx.invert();
					mtx.transform(pnt);

					position[0] = pnt.x;
					position[1] = pnt.y;
					position[2] = pnt.z;

				} else {
					// no bounds intersections, parent to the zone

					parentEntity = zoneEntity;
					int zoneType = zoneEntity.getType();

					position[0] = zone_pd.point.x;
					position[1] = zone_pd.point.y;

					boolean embed = false;
					if (zoneType == Entity.TYPE_SEGMENT) {
						// certain types of models are embedded into the cut-outs
						// in the segment, rather than being placed on the surface
						String category = tool.getCategory();
						if ((category != null) && (
							category.equals("Category.Window") ||
							category.equals("Category.Door"))) {
							embed = true;
						}
					}
					if (embed) {
						position[2] = -(size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH);
					} else {
						position[2] = (size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH);
					}
				}
			}

			editorGrid.alignPositionToGrid(position);
		
			// create the entity
			Entity newEntity = entityBuilder.createEntity(
				model,
				model.issueEntityID(),
				position,
				rotation,
				tool);
			
			// clear out the shadow property that may have been
			// set in the tool
			AV3DUtils.setShadowState(newEntity, false);
			
			// Grab the last position of the shadow entity and set it to the
			// starting postion for our new entity.
			double[] startingPosition = (double[]) 
				tool.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    PositionableEntity.POSITION_PROP);
			
			((PositionableEntity) newEntity).setStartingPosition(
					startingPosition);

			// stack the commands together
			ArrayList<Command> commandList = new ArrayList<Command>();

			Command cmd = new AddEntityChildCommand(
					model, 
					model.issueTransactionID(),
					parentEntity, 
					newEntity,
					true);
			
            if (tool instanceof PasteTool) {
                // create the ignore rule list
                HashSet<String> ignoreRuleList = 
                    IgnoreRuleList.getIgnorePasteRuleList();
                cmd.setIgnoreRuleList(ignoreRuleList);
            }
			commandList.add(cmd);

			cmd = new SelectEntityCommand(model, newEntity, true);
			commandList.add(cmd);

			// Create the MultiCommand and send it
			MultiCommand stack = new MultiCommand(
				commandList,
				"Add Entity -> " +  newEntity.getEntityID());
			
			stack.setErrorReporter(reporter);
			controller.execute(stack);
		}
	}

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Sets the correct entity builder to use
     * @param builder
     */
    void setEntityBuilder(EntityBuilder builder){
        entityBuilder = builder;
    }

    /**
     * Initialize in preparation for a response
     *
     * @param ad The initial device position of the mouse
     */
    void setActionData(ActionData ad) {
        actionData = ad;

		pickManager = actionData.pickManager;
		zoneEntity = (ZoneEntity)actionData.zoneWrapper.entity;
		wrapperMap = actionData.wrapperMap;

		zcm.setWrapperMap(actionData.wrapperMap);
		zcm.setActiveZoneEntity(zoneEntity);
    }
}
