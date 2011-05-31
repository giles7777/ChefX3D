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

package org.chefx3d.view.awt.av3d;

// External imports
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.TransformGroup;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Manager for collision detection for Entities within editing zones.
 *
 * @author Rex Melton
 * @version $Revision: 1.7 $
 */
class ZoneCollisionManager {
	
    /** The manager of the entities to be handled */
    //private AV3DEntityManager entityManager;
    
    /** The map of entity wrappers */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;
    
	/** The zone entity */
	private ZoneEntity zoneEntity;
	
	/** The working zone entity wrapper */
	private AV3DEntityWrapper zoneWrapper;
	
	/** The bounds of the active zone */
	private OrientedBoundingBox zoneBounds;
	
    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f mtx;
    private double[] position_array;
    
	/**
	 * Constructor
	 */
	ZoneCollisionManager() {
        tu = new TransformUtils();
        mtx = new Matrix4f();
        position_array = new double[3];
	}

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/*
	 * Check the argument Entity, transformed by the argument Matrix, for
	 * bounds collisions against the Entities in the zone. Place the 
	 * results in the argument list.
	 *
	 * @param entity The source Entity to check for collisions.
	 * @param matrix The matrix to use to transform the source Entity's bounds.
	 * @param results The list to populate with collision results.
	 */
	void check(Entity entity, Matrix4f matrix, ArrayList<Entity> results) {
		check(entity, matrix, results, false);
	}
	
	/*
	 * Check the argument Entity, transformed by the argument Matrix, for
	 * bounds collisions against the Entities in the zone. Place the 
	 * results in the argument list.
	 *
	 * @param entity The source Entity to check for collisions.
	 * @param matrix The matrix to use to transform the source Entity's bounds.
	 * @param results The list to populate with collision results.
	 * @param includeSrcChildren check the entitys children (or not)
	 */
	void check(Entity entity, Matrix4f matrix, ArrayList<Entity> results, boolean includeSrcChildren) {
		
		results.clear();
		if ((wrapperMap != null) && (zoneEntity != null)) {
			AV3DEntityWrapper wrapper = wrapperMap.get(entity.getEntityID());
			if (wrapper != null) {
				OrientedBoundingBox bounds = wrapper.getBounds();
				bounds.transform(matrix);
				check(zoneEntity, entity, bounds, results, includeSrcChildren);
			}
		}
	}
	
    /**
     * Set the active wrapper map
     *
     * @param wrapperMap The active wrapper map
     */
    void setWrapperMap(HashMap<Integer, AV3DEntityWrapper> wrapperMap) {
		
        this.wrapperMap = wrapperMap;
    }
	
    /**
     * Set the active zone entity
     *
     * @param ze The zone entity that is active
     */
    void setActiveZoneEntity(ZoneEntity ze) {
		zoneEntity = ze;
		if ((zoneEntity != null) && (wrapperMap != null)) {
			zoneWrapper = wrapperMap.get(zoneEntity.getEntityID());
			if (zoneWrapper != null) {
				zoneBounds = zoneWrapper.getBounds();
				tu.getLocalToVworld(zoneWrapper.transformGroup, mtx);
                zoneBounds.transform(mtx);
			} else {
				zoneBounds = null;
			}
		} else {
			zoneWrapper = null;
			zoneBounds = null;
		}
	}
	
	/**
	 * Initialize the argument pos object with the zone relative 
	 * position of the specified entity
	 *
	 * @param pe The Entity to find the position of
	 * @param offset The local translation of the entities position, or null
	 * if no translation is required.
	 * @param pos The object to initialize with the zone relative position
	 * @return true if the conversion was performed, false if not. 
	 */
	boolean toZoneRelative(PositionableEntity pe, Vector3f offset, Point3f pos) {
		
		if ((wrapperMap != null) && (zoneWrapper != null)) {
			pe.getStartingPosition(position_array);
			
			pos.x = (float)position_array[0];
			pos.y = (float)position_array[1];
			pos.z = (float)position_array[2];
			
			if (offset != null) {
				pos.x += offset.x;
				pos.y += offset.y;
				pos.z += offset.z;
			}
			
			AV3DEntityWrapper parentWrapper = wrapperMap.get(pe.getParentEntityID());
			if (parentWrapper != null) {
				if (parentWrapper != zoneWrapper) {
					tu.getLocalToVworld(
						parentWrapper.transformGroup, 
						zoneWrapper.transformGroup, 
						mtx);
					mtx.transform(pos);
				}
				return(true);
			}
		}
		return(false);
	}
	
	/** 
	 * Find and return the entity from the argument list that is 
	 * closest to the specified zone relative position
	 *
	 * @param position The zone relative position to test against
	 * @param list The list of entities to check
	 * @return The closest entity
	 */
	PositionableEntity findClosest(Point3f position, ArrayList<Entity> list) {
		
		PositionableEntity closest_entity = null;
		
		int num = list.size();
		switch(num) {
		case 0:
			break;
		case 1:
			closest_entity = (PositionableEntity)list.get(0);
			break;
		default:
			Point3f pos = new Point3f();
			closest_entity = (PositionableEntity)list.get(0);
			toZoneRelative(closest_entity, null, pos);
			
			float closest_distance = position.distanceSquared(pos);
			for (int i = 1; i < num; i++) {
				PositionableEntity pe = (PositionableEntity)list.get(i);
				toZoneRelative(pe, null, pos);
				float distance = position.distanceSquared(pos);
				if (distance < closest_distance) {
					closest_distance = distance;
					closest_entity = pe;
				}
			}
		}
		return(closest_entity);
	}

	/** 
	 * Find and return the entity from the argument list that
	 * has the highest bounds elevation relative to the zone.
	 *
	 * @param list The list of entities to check
	 * @return The highest entity
	 */
	PositionableEntity findHighest(ArrayList<Entity> list) {
		
		PositionableEntity highest_entity = null;
		
		int num = list.size();
		switch(num) {
		case 0:
			break;
		case 1:
			highest_entity = (PositionableEntity)list.get(0);
			break;
		default:
			float[] size = new float[3];
			float[] scale = new float[3];
			Point3f pos = new Point3f();
			highest_entity = (PositionableEntity)list.get(0);
			highest_entity.getSize(size);
			highest_entity.getScale(scale);
			toZoneRelative(highest_entity, null, pos);
			
			float highest_elevation = pos.z + scale[2] * size[2];
			for (int i = 1; i < num; i++) {
				PositionableEntity pe = (PositionableEntity)list.get(i);
				pe.getSize(size);
				pe.getScale(scale);
				toZoneRelative(pe, null, pos);
				float elevation = pos.z + scale[2] * size[2];
				if (elevation > highest_elevation) {
					highest_elevation = elevation;
					highest_entity = pe;
				}
			}
		}
		return(highest_entity);
	}

	/**
	 * Recurse through the parent Entity's hierarchy and find any entities
	 * in collision with the source Entity.
	 * 
	 * @param parent The Entity to start from
	 * @param srcEntity The Entity whose bounds are to be checked
	 * @param srcBounds The bounds of the source Entity
	 * @param results The list of entities in collision with the source
	 */
	void check(
		Entity parent, 
		Entity srcEntity, 
		OrientedBoundingBox srcBounds,
		ArrayList<Entity> results) {
		
		check(parent, srcEntity, srcBounds, results, false);
	}
	
	/**
	 * Recurse through the parent Entity's hierarchy and find any entities
	 * in collision with the source Entity.
	 * 
	 * @param parent The Entity to start from
	 * @param srcEntity The Entity whose bounds are to be checked
	 * @param srcBounds The bounds of the source Entity
	 * @param results The list of entities in collision with the source
	 * @param includeSrcChildren check the srcEntitys children (or not)
	 */
	void check(
		Entity parent, 
		Entity srcEntity, 
		OrientedBoundingBox srcBounds,
		ArrayList<Entity> results,
		boolean includeSrcChildren) {
		
		if (parent.hasChildren()) {
			ArrayList<Entity> children = parent.getChildren();
			for (int i = 0; i < children.size(); i++) {
				Entity child = children.get(i);
				if (child != srcEntity) {
					int id = child.getEntityID();
					AV3DEntityWrapper wrapper = wrapperMap.get(id);
					if (wrapper != null) {
						OrientedBoundingBox bounds = wrapper.getBounds();
						tu.getLocalToVworld(wrapper.transformGroup, mtx);
						bounds.transform(mtx);
						if (srcBounds.intersect(bounds, true)) {
							results.add(child);
						}
					}
					check(child, srcEntity, srcBounds, results);
				} else if (includeSrcChildren) {
					check(child, srcEntity, srcBounds, results);
				}
			}
		}
	}
}
