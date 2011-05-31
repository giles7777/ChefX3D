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

// External imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.vecmath.*;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Node;

import org.j3d.device.input.TrackerState;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.view.common.NearestNeighborMeasurement;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Allows the user to request a list of neighbors along the X and Y Axis
 * relative to the selected Entity
 * and be handed back a sorted list of entities in order of nearest entity
 *
 * @author jonhubba
 * @version $Revision: 1.5 $
 */
class NearestNeighborMeasurementImpl implements NearestNeighborMeasurement {

    /** The smallest dimension along the axis of interest for the bounds sorting */
    private static final float THRESHOLD = 0.010f;

    /** A hashmap of the entity wrappers*/
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;

    /** The PickManager */
    private PickManager pickManager;

    /** The zone collision manager */
    private ZoneCollisionManager zcm;

    /** Entity hierarchy utils */
    private EntityUtils entityUtils;

    /** Working bounds object */
    private OrientedBoundingBox bounds;

    /** Local transformation utils */
    private TransformUtils tu;

    /** Scratch vecmath objects */
    private Matrix4f mtx;
    private Matrix4f mtx0;
    private Point3f pnt;
    private Point3f pnt1;
    private Point3f pnt2;
    private AxisAngle4f rotation;
    private Vector3f translation;
    private Vector3f orientation;

    /** Scratch arrays */
    private double[] pos_array;
    private float[] rot_array;
    private float[] size;
    private float[] scale;
    private float[] min;
    private float[] max;

    /**
     * Constructor
     *
     * @param wrapperMap
     */
    NearestNeighborMeasurementImpl(WorldModel model, Group rootGroup) {

        tu = new TransformUtils();
        mtx = new Matrix4f();
        mtx0 = new Matrix4f();
        pnt = new Point3f();
        pnt1 = new Point3f();
        pnt2 = new Point3f();
        translation = new Vector3f();
        rotation = new AxisAngle4f();
        orientation = new Vector3f();

        pos_array = new double[3];
        rot_array = new float[4];
        size = new float[3];
        scale = new float[3];
        min = new float[3];
        max = new float[3];

        entityUtils = new EntityUtils(model);
        pickManager = new PickManager(model, rootGroup);

        bounds = new OrientedBoundingBox();

        zcm = new ZoneCollisionManager();
    }

    // ---------------------------------------------------------------
    // Local Public Methods
    // ---------------------------------------------------------------

    /**
     * Returns a sorted list of neighboring entities that intersect
     * the 'extended' bounds of the specified entity along a
     * particular direction.
     *
     * @param model Holds all the entity data
     * @param activeZone The current zone being edited
     * @param currentEntity The current entity being checked
     * @param direction The axis direction to check
     * @param boundsAdj The adjustment to the current entity's bounds.
     * @return The list of neighboring entities
     */
    public ArrayList<Entity> nearestNeighbors(
        WorldModel model,
        Entity activeZone,
        PositionableEntity currentEntity,
        int direction,
        float[] boundsAdj) {

        if (boundsAdj == null) {
            return(nearestNeighbors(model, activeZone, currentEntity, direction));
        }

        ArrayList<Entity> returnList = new ArrayList<Entity>();

        AV3DEntityWrapper parentWrapper =
            wrapperMap.get(currentEntity.getParentEntityID());

        // this is bad, return an empty list
        if (parentWrapper == null) {
            return returnList;
        }

        // get the entity's position
        currentEntity.getPosition(pos_array);
        pnt.x = (float)pos_array[0];
        pnt.y = (float)pos_array[1];
        pnt.z = (float)pos_array[2];

        // the pick orientation, i.e. which direction to
        // check for an adjacent wall
        orientation.set(0, 0, 0);
        switch(direction) {
            case POS_X:
                orientation.x = 1;
                break;
            case NEG_X:
                orientation.x = -1;
                break;
            case POS_Y:
                orientation.y = 1;
                break;
            case NEG_Y:
                orientation.y = -1;
                break;
            case POS_Z:
                orientation.z = 1;
                break;
            case NEG_Z:
                orientation.z = -1;
                break;
        }

        // transform the entity's position and the direction to check
        // into world space coordinates
        tu.getLocalToVworld(parentWrapper.transformGroup, mtx);
        mtx.transform(pnt);
        mtx.transform(orientation);

        // do a pick from this entity's center along the specified axis
        TrackerState trackerState = new TrackerState();

        trackerState.worldPos[0] = pnt.x;
        trackerState.worldPos[1] = pnt.y;
        trackerState.worldPos[2] = pnt.z;

        trackerState.worldOri[0] = orientation.x;
        trackerState.worldOri[1] = orientation.y;
        trackerState.worldOri[2] = orientation.z;

        pickManager.doPickProxy(trackerState);

        ArrayList<PickData> pickList = pickManager.getResults();
        int num_pick = pickList.size();

        ZoneEntity adjacentZone = null;
        for (int i = 0; i < num_pick; i++) {
            // find the zone adjacent to the working zone
            // in the direction of the pick
            PickData tpd = pickList.get(i);
            if ((tpd.object instanceof ZoneEntity)) {
                adjacentZone = (ZoneEntity)tpd.object;
                break;
            }
        }
        /////////////////////////////////////////////////////
        currentEntity.getSize(size);
        currentEntity.getScale(scale);

        float x = boundsAdj[0] + size[0] / 2;
        float y = boundsAdj[1] + size[1] / 2;
        float z = boundsAdj[2] + size[2] / 2;

        min[0] = -x;
        min[1] = -y;
        min[2] = -z;

        max[0] = x;
        max[1] = y;
        max[2] = z;

        // make the bounds 'directional', i.e. truncate the
        // bounds in the opposite direction
        switch(direction) {
            case POS_X:
                min[0] = 0;
                break;
            case NEG_X:
                max[0] = 0;
                break;
            case POS_Y:
                min[1] = 0;
                break;
            case NEG_Y:
                max[1] = 0;
                break;
            case POS_Z:
                min[2] = 0;
                break;
            case NEG_Z:
                max[2] = 0;
                break;
        }

        bounds.setVertices(min, max);
        bounds.setScale(scale);

        //currentEntity.getPosition(pos_array);
        currentEntity.getRotation(rot_array);

        rotation.set(rot_array);
        translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);

        mtx0.setIdentity();
        mtx0.setRotation(rotation);
        mtx0.setTranslation(translation);

        mtx0.mul(mtx, mtx0);

        // transform the bounds into world space
        bounds.transform(mtx0);
        /////////////////////////////////////////////////////

        // check against the current zone's children
        zcm.setWrapperMap(wrapperMap);
        zcm.setActiveZoneEntity((ZoneEntity)activeZone);
        zcm.check(activeZone, currentEntity, bounds, returnList);

        if (adjacentZone != null) {
            // if an adjacent zone exists, check it's entities as well
            zcm.setActiveZoneEntity(adjacentZone);
            zcm.check(adjacentZone, currentEntity, bounds, returnList);

            // check the zone separately
            int id = adjacentZone.getEntityID();
            AV3DEntityWrapper wrapper = wrapperMap.get(id);
            if (wrapper != null) {
                OrientedBoundingBox obb = wrapper.getBounds();
                tu.getLocalToVworld(wrapper.transformGroup, mtx);
                obb.transform(mtx);
                if (bounds.intersect(obb, true)) {
                    returnList.add(adjacentZone);
                }
            }
        }

        sort(min, max, scale, mtx0, direction, returnList, 0);
        return(returnList);
    }

    /**
     * Returns a sorted list of neighbors that fall on this entities
     * axis in the relative zone
     * @param model Holds all the entity data
     * @param activeZone The current zone being edited
     * @param currentEntity The current entity being checked
     * @param direction The direction to check
     * @return
     */
    public ArrayList<Entity> nearestNeighbors(
        WorldModel model,
        Entity activeZone,
        PositionableEntity currentEntity,
        int direction) {

        ArrayList<Entity> returnList = new ArrayList<Entity>();

        AV3DEntityWrapper parentWrapper =
            wrapperMap.get(currentEntity.getParentEntityID());

        // this is bad, return an empty list
        if (parentWrapper == null) {
            return returnList;
        }

        // get the entity's position
        currentEntity.getPosition(pos_array);
        pnt.x = (float)pos_array[0];
        pnt.y = (float)pos_array[1];
        pnt.z = (float)pos_array[2];

        // the pick orientation, i.e. which direction to
        // check for an adjacent wall
        orientation.set(0, 0, 0);
        switch(direction) {
            case POS_X:
                orientation.x = 1;
                break;
            case NEG_X:
                orientation.x = -1;
                break;
            case POS_Y:
                orientation.y = 1;
                break;
            case NEG_Y:
                orientation.y = -1;
                break;
            case POS_Z:
                orientation.z = 1;
                break;
            case NEG_Z:
                orientation.z = -1;
                break;
        }

        // transform the entity's position and the direction to check
        // into world space coordinates
        tu.getLocalToVworld(parentWrapper.transformGroup, mtx);
        mtx.transform(pnt);
        mtx.transform(orientation);

        // do a pick from this entity's center along the specified axis
        TrackerState trackerState = new TrackerState();

        trackerState.worldPos[0] = pnt.x;
        trackerState.worldPos[1] = pnt.y;
        trackerState.worldPos[2] = pnt.z;

        trackerState.worldOri[0] = orientation.x;
        trackerState.worldOri[1] = orientation.y;
        trackerState.worldOri[2] = orientation.z;

        pickManager.doPickProxy(trackerState);

        ArrayList<PickData> pickList = pickManager.getResults();

        for (PickData tpd:pickList) {
            // find the zone adjacent to the working zone
            // in the direction of the pick

            Entity pick = (Entity)tpd.object;
            if (pick !=  currentEntity) {
                returnList.add(pick);
            }

            if ((tpd.object instanceof ZoneEntity)) {
                break;
            }
        }
        return returnList;
    }

    // ---------------------------------------------------------------
    // Local Public Methods
    // ---------------------------------------------------------------

    /**
     * Keeps the wrapper map up to date.
     * @param map
     */
    void setWrapperMap(HashMap<Integer, AV3DEntityWrapper> map) {
        wrapperMap = map;
    }

    // ---------------------------------------------------------------
    // Local Private Methods
    // ---------------------------------------------------------------

    /**
     * Returns the extents of an entity
     */
    private float[] getExtentsOfEntity(
        BasePositionableEntity entity,
        double[] position) {

        float[] adjustedBounds = new float[6];
        entity.getBounds(adjustedBounds);

        adjustedBounds[0] += position[0];
        adjustedBounds[1] += position[0];
        adjustedBounds[2] += position[1];
        adjustedBounds[3] += position[1];
        adjustedBounds[4] += position[2];
        adjustedBounds[5] += position[2];

        return adjustedBounds;
    }

    /**
     * Sorts the list of all the neighbors into a list of the nearest ones.
     * @param entityList
     * @param distanceList
     */
    private void sortNeighborList(
        ArrayList<Entity> entityList,
        ArrayList<Float> distanceList){

        if(entityList.size() != distanceList.size()){
            System.out.println("THIS code is about to crash because the " +
                    "entityList and distanceList are out of Sync. Remove " +
                    "when stable");
            return;
        }

        int size = entityList.size();
        //TODO: BUBBLE SORT OPTIMIZE LATER
        int i = 0;
        boolean swapped;
        do {
            swapped = false;

            for(int j = 1; j < size; j++){
                i = j-1;

                if(distanceList.get(j) < distanceList.get(i)){
                    swapped = true;
                    Entity entityTempI = entityList.get(i);
                    Entity entityTempJ = entityList.get(j);
                    entityList.set(i, entityTempJ);
                    entityList.set(j, entityTempI);

                    Collections.swap(distanceList, j, i);
                }
            }
        } while (swapped);

    }

    /**
     * Takes the active zone and finds the relative trail from the
     * activeZoneEntity down to the parent of the current entity then
     * combines that parent entity with the position of the current entity
     * Combining the positions till the parent of the current entity
     * matches the active zone
     *
     * @param activeZoneEntity
     * @param entity
     * @return
     */
    private double[] getRelativePositionToActiveZone(
        Entity activeZoneEntity,
        Entity entity) {

        double[] position = new double[3];
        Entity currentEntity = entity;
        ((PositionableEntity)currentEntity).getPosition(position);

        AV3DEntityWrapper parentWrapper =
            wrapperMap.get(currentEntity.getParentEntityID());

        Matrix4f currentMatrix = new Matrix4f();

        TransformUtils tu = new TransformUtils();
        AV3DEntityWrapper activeZoneWrapper =
            wrapperMap.get(activeZoneEntity.getEntityID());

        // using the parent entity instead of the current entity because
        // there seemed to be a timing issue with adding some items to
        // the scene that would cause the wrapper to return null
        tu.getLocalToVworld(
            activeZoneWrapper.sharedNode,
            parentWrapper.sharedNode,
            currentMatrix);

        Vector3f translationVector = new Vector3f();
        currentMatrix.get(translationVector);

        position[0] += translationVector.x;
        position[1] += translationVector.y;
        position[2] += translationVector.z;

        return position;
    }

    /**
     * This method uses a pick manager to pick along the orientation passed in
     *
     * if the boolean, adjacent, is true it will pick against the adjacent walls
     * otherwise it will pick the current wall
     *
     * @param activeZone current wall to pick along
     * @param currentEntity current entity to use as a start position
     * @param ori The orientation to pick along
     * @param adjacent Whether to pick against the adjacent wall or current wall
     * @return the list of found entities
     */
    private ArrayList<Entity> checkWall(
        Entity activeZone,
        PositionableEntity currentEntity,
        Vector3f ori,
        boolean adjacent) {

        //sort list
        ArrayList<Entity> entityList = new ArrayList<Entity>();
        Entity currentEntityZone = this.findZoneEntity(currentEntity);

        double[] position_d = new double[3];
        Point3f pnt = new Point3f();

        Matrix4f mtx = new Matrix4f();
        TransformUtils tu = new TransformUtils();

        // do a new pick for this entity
        position_d = getRelativePositionToActiveZone(activeZone, currentEntity);
        pnt.x = (float) position_d[0];
        pnt.y = (float) position_d[1];
        pnt.z = (float) position_d[2];
        TrackerState trackerState = new TrackerState();

//System.out.println("DEBUG_POS_pnt: " + pnt);

        int parentID = currentEntity.getParentEntityID();
        AV3DEntityWrapper parentWrapper = wrapperMap.get(parentID);

        ///////////////////////////////////////////////////////////////
        // rem: this is taking the entity's position, relative to the
        // zone, and transforming it based on it's parent's local to
        // world matrix..... if the parent is not the zone - then this
        // would seem to be wrong.....
        tu.getLocalToVworld(parentWrapper.transformGroup, mtx);

        mtx.transform(pnt);
        mtx.transform(ori);
        ///////////////////////////////////////////////////////////////


        trackerState.worldPos[0] = pnt.x;
        trackerState.worldPos[1] = pnt.y;
        trackerState.worldPos[2] = pnt.z;

        trackerState.worldOri[0] = ori.x;
        trackerState.worldOri[1] = ori.y;
        trackerState.worldOri[2] = ori.z;

        pickManager.doPickProxy(trackerState);

        ArrayList<PickData> pickList = pickManager.getResults();
        int num_pick = pickList.size();

        for (int j = 0; j < num_pick; j++) {

            // find the closest object associated with an entity
            // that is NOT the zone entity and is not a part of
            // the entity hierarchy that is being transformed
            PickData tpd = pickList.get(j);
            if ((tpd.object instanceof Entity)) {

                Entity entity = (Entity) tpd.object;
                Entity entityZone = findZoneEntity(entity);

                if (adjacent) {

                    if (currentEntityZone != entityZone) {

                        entityList.add(entity);
                    }
                } else {
                    if ((entity.getType() != Entity.TYPE_SEGMENT) &&
                        (currentEntityZone == entityZone) &&
                        (currentEntity != entity)) {

                        entityList.add(entity);

                    }
                }
            }
        }
        return entityList;
    }

    /**
     * Sorts the passed in entityList and returns it
     * @param activeZone  The active  zone, used to determine the distance of an entity
     * @param currentEntity The current entity everything is determining the distance from
     * @param type  One of the enums up at the top ( POS_X, NEG_X.... ) to determine which way to calculate distance
     * @param entityList  The entity list to sort.
     */
    private void sortEntityList(
            Entity activeZone,
            Entity currentEntity,
            int type,
            ArrayList<Entity> entityList) {

        ArrayList<Float> distanceList = new ArrayList<Float>();

        Entity wall = null;

        // Calculate all the distances between the current entity and the
        // other entities in the list
        for (int i = 0 ; i < entityList.size(); i++) {
            float distance = 0;

            Entity entity = entityList.get(i);
            if(entity instanceof SegmentEntity) {

                //because only one wall should ever be in a picked list remove it
                //this way it can be added to the end of the list later
                wall = entityList.get(i);
                entityList.remove(i);

            } else {

                distance = distanceBetweenTwoEntities(
                        activeZone, currentEntity, entity, type);
                distanceList.add(distance);

            }

        }

        if (entityList.size() > 1) {
             sortNeighborList(entityList, distanceList);
        }

        //Add the wall back in.
        if (wall != null) {
            entityList.add(wall);
        }
    }

    /**
     * Finds the distance between 2 entities
     *
     * @param activeZone The active zone
     * @param entity1  Uses the position of the y and z and the min x bounds
     * of this position
     * @param entity2 Uses the position of the y and z and the max x bounds
     * for this position
     * @param direction The axis direction being checked
     * @return The distance
     */
    private float distanceBetweenTwoEntities(
        Entity activeZone,
        Entity entity1,
        Entity entity2,
        int direction) {

        Matrix4f mtx = new Matrix4f();

        double[] position;
        float[] bounds = new float[6];
        Point3f pnt1 = new Point3f();
        Point3f pnt2 = new Point3f();

        // use the position and bounds to get a point to check
        position = getRelativePositionToActiveZone(activeZone, entity1);
        bounds = getExtentsOfEntity((BasePositionableEntity)entity1, position);
        switch(direction) {
            case POS_X:
                pnt1.set(bounds[1], (float)position[1], (float)position[2]);
                break;
            case NEG_X:
                pnt1.set(bounds[0], (float)position[1], (float)position[2]);
                break;
            case POS_Y:
                pnt1.set((float)position[0], bounds[3], (float)position[2]);
                break;
            case NEG_Y:
                pnt1.set((float)position[0], bounds[2], (float)position[2]);
                break;
            case POS_Z:
                pnt1.set((float) position[0], (float) position[1], bounds[5]);
                break;
            case NEG_Z:
                pnt1.set((float) position[0], (float) position[1], bounds[4]);
                break;
        }

        // transform the point to the correct Vworld position
        AV3DEntityWrapper parentWrapper =
            wrapperMap.get(entity1.getParentEntityID());
        tu.getLocalToVworld(parentWrapper.transformGroup, mtx);
        mtx.transform(pnt1);

        // use the position and bounds to get a point to check
        position = getRelativePositionToActiveZone(activeZone, entity2);
        bounds = getExtentsOfEntity((BasePositionableEntity)entity2, position);
        switch(direction) {
            case POS_X:
                pnt2.set(bounds[0], (float)position[1], (float)position[2]);
                break;
            case NEG_X:
                pnt2.set(bounds[1], (float)position[1], (float)position[2]);
                break;
            case POS_Y:
                pnt2.set((float)position[0], bounds[2], (float)position[2]);
                break;
            case NEG_Y:
                pnt2.set((float)position[0], bounds[3], (float)position[2]);
                break;
            case POS_Z:
                pnt2.set((float) position[0], (float) position[1], bounds[4]);
                break;
            case NEG_Z:
                pnt2.set((float) position[0], (float) position[1], bounds[5]);
                break;
        }

        // transform the point to the correct Vworld position
        parentWrapper = wrapperMap.get(entity2.getParentEntityID());
        tu.getLocalToVworld(parentWrapper.transformGroup, mtx);
        mtx.transform(pnt2);

        // return the distance between the two points
        return pnt1.distance(pnt2);
    }

    /**
     * Find the zone of an entity
     *
     * @param entity The entity to search from
     * @return The closest zone entity, or null if one cannot be found.
     */
    private Entity findZoneEntity(Entity entity) {

        Entity ze = null;
        if (entity != null) {
            if (entity.isZone()) {
                ze = entity;
            } else {
                ze = entityUtils.getZoneEntity(entity);
            }
        }
        return(ze);
    }

    /**
     * Sort the argument list of entities based on distance
     * along the axis of interest (direction).
     *
     * @param min The untransformed min extent of the bounds to be tested against
     * @param max The untransformed max extent of the bounds to be tested against
     * @param scl The scale factor of the bounds
     * @param mtx The transform to apply to the bounds to place them into world space
     * @param direction The bounds axis along which to determine closest neighbor
     * @param list The set of entities to be sorted
     * @param idx The index within the list to start sorting
     * @return The index within the list of the unsorted entities
     */
    private int sort(
        float[] min,
        float[] max,
        float[] scl,
        Matrix4f mtx,
        int direction,
        ArrayList<Entity> list,
        int idx) {

        int num_total = list.size();
        int num_to_sort = num_total - idx;
        if (num_to_sort > 1) {

            // new extents after subdivision
            float[] near_min = new float[3];
            float[] near_max = new float[3];
            float[] far_min = new float[3];
            float[] far_max = new float[3];

            near_min[0] = min[0];
            near_min[1] = min[1];
            near_min[2] = min[2];

            near_max[0] = max[0];
            near_max[1] = max[1];
            near_max[2] = max[2];

            far_min[0] = min[0];
            far_min[1] = min[1];
            far_min[2] = min[2];

            far_max[0] = max[0];
            far_max[1] = max[1];
            far_max[2] = max[2];

            // bisect along the axis of interest
            float mid_x = (min[0] + max[0]) * 0.5f;
            float mid_y = (min[1] + max[1]) * 0.5f;
            float mid_z = (min[2] + max[2]) * 0.5f;

            // dimension of the bounds along the axis of interest
            float delta_x = mid_x - min[0];
            float delta_y = mid_y - min[1];
            float delta_z = mid_z - min[2];
            boolean threshold_reached = false;

            // establish near and far based on the axis of interest
            switch(direction) {
            case POS_X:
                near_max[0] = mid_x;
                far_min[0] = mid_x;
                threshold_reached = (delta_x <= THRESHOLD);
                break;

            case NEG_X:
                near_min[0] = mid_x;
                far_max[0] = mid_x;
                threshold_reached = (delta_x <= THRESHOLD);
                break;

            case POS_Y:
                near_max[1] = mid_y;
                far_min[1] = mid_y;
                threshold_reached = (delta_y <= THRESHOLD);
                break;

            case NEG_Y:
                near_min[1] = mid_y;
                far_max[1] = mid_y;
                threshold_reached = (delta_y <= THRESHOLD);
                break;

            case POS_Z:
                near_max[2] = mid_z;
                far_min[2] = mid_z;
                threshold_reached = (delta_z <= THRESHOLD);
                break;

            case NEG_Z:
                near_min[2] = mid_z;
                far_max[2] = mid_z;
                threshold_reached = (delta_z <= THRESHOLD);
                break;
            }

            // check the near box
            bounds.setVertices(near_min, near_max);
            bounds.setScale(scl);
            bounds.transform(mtx);

            Entity[] hit_list = new Entity[num_to_sort];
            int num_hit = check(bounds, list, idx, hit_list);
            if ((num_hit == 1) || threshold_reached) {
                for (int i = 0; i < num_hit; i++) {
                    Entity entity = hit_list[i];
                    list.remove(entity);
                    list.add(idx++, entity);
                }
            } else {
                idx = sort(near_min, near_max, scl, mtx, direction, list, idx);
            }

            // check the far box
            bounds.setVertices(far_min, far_max);
            bounds.setScale(scl);
            bounds.transform(mtx);

            num_hit = check(bounds, list, idx, hit_list);
            if ((num_hit == 1) || threshold_reached) {
                for (int i = 0; i < num_hit; i++) {
                    Entity entity = hit_list[i];
                    list.remove(entity);
                    list.add(idx++, entity);
                }
            } else {
                idx = sort(far_min, far_max, scl, mtx, direction, list, idx);
            }
        } else {
            idx = num_total;
        }
        return(idx);
    }

    /**
     * Perform intersection testing of argument bounds against the
     * entities in the list
     *
     * @param bounds The bounds to check for entity intersection against.
     * @param list The list of entities to check
     * @param idx The index within the list to start at
     * @param results Array to be initialized with results
     * @return The number of results that have been placed into the results array
     */
    private int check(
        OrientedBoundingBox bounds,
        ArrayList<Entity> list,
        int idx,
        Entity[] results) {

        int num_result = 0;
        int num_total = list.size();
        for (int i = idx; i < num_total; i++) {
            Entity target = list.get(i);
            int target_id = target.getEntityID();
            AV3DEntityWrapper wrapper = wrapperMap.get(target_id);
            // note: the target bounds have already been transformed
            // by the initial intersection test.
            OrientedBoundingBox target_bounds = wrapper.getBounds();
            if (bounds.intersect(target_bounds, false)) {
                results[num_result++] = target;
            }
        }
        return(num_result);
    }
}
