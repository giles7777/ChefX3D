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

package org.chefx3d.model;

// External imports
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

// Local imports
// none

/**
 * Misc. utility methods for doing lookups and calculations on an 
 * Entity hierarchy
 *
 * @author Rex Melton
 * @version $Revision: 1.6 $
 */
public class EntityUtils {
	
	/** The active WorldModel */
	private WorldModel model;
	
    /** Scratch objects used for transform calculations */
    private Vector3f translation;
    private AxisAngle4f rotation;
    private Matrix4f mtx;
    private Quat4f quat;
    
    private double[] pos_array;
    private float[] rot_array;
	
	/** Stack of matrix objects */
	private ArrayList<Matrix4f> mtx_list;
	
	/**
	 * Constructor
	 * 
	 * @param model The WorldModel
	 */
	public EntityUtils(WorldModel model) {
		
		if (model == null) {
			throw new IllegalArgumentException("model argument cannot be null");
		}
		this.model = model;
		
        translation = new Vector3f();
        rotation = new AxisAngle4f();
		mtx = new Matrix4f();
		quat = new Quat4f();

        pos_array = new double[3];
        rot_array = new float[4];
		
		mtx_list = new ArrayList<Matrix4f>();
	}

    /**
     * Return the zone type Entity that is the closest ancestor of the 
	 * argument entity.
	 *
     * @param entity The Entity for which to determine the zone ancestor
     * @return The ancestor ZoneEntity, or null if none exists
     */
    public ZoneEntity getZoneEntity(Entity entity) {
		ZoneEntity ze = null;
        if (entity != null) {
        	int parentID = entity.getParentEntityID();
        	Entity parent = model.getEntity(parentID);
			if (parent != null) {
				if (parent.isZone()) {
					ze = (ZoneEntity)parent;
				} else {
					ze = getZoneEntity(parent);
				}
			}
		}
		return(ze);
    }
	
    /**
     * Return the root Entity of the argument entity.
	 *
     * @param entity The Entity for which to determine the root ancestor
     * @return The root ancestor Entity, or null if none exists
     */
    public Entity getRootEntity(Entity entity) {
		Entity root = null;
        if (entity != null) {
        	int parentID = entity.getParentEntityID();
        	Entity parent = model.getEntity(parentID);
			if (parent != null) {
				if (parent.getType() == Entity.TYPE_CONTENT_ROOT) {
					root = parent;
				} else {
					root = getRootEntity(parent);
				}
			}
		}
		return(root);
    }
	
    /**
     * Return the position of an entity relative to it's closest ancestor zone.
     *
     * @param entity The Entity for which to determine the position
     * @return The position relative to the zone, or null if it
     * cannot be determined
     */
    public double[] getPositionRelativeToZone(Entity entity){

		double[] position = null;
		if (configMatrixEntityToZone(entity)) {
			mtx.get(translation);
			position = new double[3];
			position[0] = translation.x;
			position[1] = translation.y;
			position[2] = translation.z;
		}
		return(position);
    }
	
    /**
     * Return the rotation of an entity relative to it's closest ancestor zone.
     *
     * @param entity The Entity for which to determine the rotation
     * @return The rotation relative to the zone, or null if it
     * cannot be determined
     */
    public float[] getRotationRelativeToZone(Entity entity){

		float[] rot = null;
		if (configMatrixEntityToZone(entity)) {
			mtx.get(quat);
			rotation.set(quat);
			rot = new float[4];
			rot[0] = rotation.x;
			rot[1] = rotation.y;
			rot[2] = rotation.z;
			rot[3] = rotation.angle;
		}
		return(rot);
    }
	
    /**
     * Return the transform of an entity relative to the root entity.
     *
     * @param entity The Entity for which to determine the transform
	 * @param matrix The matrix to initialize 
	 * @return true if the matrix has been initialized, false if the
	 * path to root could not be determined.
     */
    public boolean getTransformToRoot(Entity entity, Matrix4f matrix){

		boolean success = configMatrixEntityToRoot(entity);
		if (success) {
			matrix.setIdentity();
			matrix.set(mtx);
		}
		return(success);
    }
	
    /**
     * Return the transform of an entity relative to the closest 
	 * ancestor zone entity.
     *
     * @param entity The Entity for which to determine the transform
	 * @param matrix The matrix to initialize 
	 * @return true if the matrix has been initialized, false if the
	 * path to root could not be determined.
     */
    public boolean getTransformToZone(Entity entity, Matrix4f matrix){

		boolean success = configMatrixEntityToZone(entity);
		if (success) {
			matrix.setIdentity();
			matrix.set(mtx);
		}
		return(success);
    }
	
	/**
	 * Initialize the specified matrix with the position and rotation
	 * of the entity.
	 *
	 * @param pe The entity to calculate the matrix values for
	 * @param idx The index of the matrix on the local matrix list
	 * to initialize.
	 */
	private void initMatrix(PositionableEntity pe, int idx) {
		
		Matrix4f mtx;
		if (mtx_list.size() <= idx) {
			mtx = new Matrix4f();
			mtx_list.add(mtx);
		} else {
			mtx = mtx_list.get(idx);
		}
		pe.getRotation(rot_array);
		pe.getPosition(pos_array);
		rotation.set(rot_array);
		translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);
		
		mtx.setIdentity();
		mtx.setRotation(rotation);
		mtx.setTranslation(translation);
	}
	
	/**
	 * Initialize the local matrix object for the entity to zone transform
	 *
	 * @param entity the entity
	 * @return true of the matrix has been configured, false otherwise
	 */
	private boolean configMatrixEntityToZone(Entity entity) {
		
		boolean config = false;
		if (entity != null) {
			ZoneEntity ze = getZoneEntity(entity);
			if (ze != null) {
				int idx = 0;
				Entity e = entity;
				while (e != ze) {
					if (e instanceof PositionableEntity) {
						initMatrix((PositionableEntity)e, idx);
						idx++;
					}
					int parentID = e.getParentEntityID();
        			e = model.getEntity(parentID);
				}
				if (idx > 0) {
        			mtx.setIdentity();
       				for (int i = idx - 1; i >= 0; i--) {
            			Matrix4f m = mtx_list.get(i);
            			mtx.mul(m);
        			}
					config = true;
				}
			}
		}
		return(config);
	}
	
	/**
	 * Initialize the local matrix object for the entity to root transform
	 *
	 * @param entity the entity
	 * @return true of the matrix has been configured, false otherwise
	 */
	private boolean configMatrixEntityToRoot(Entity entity) {
		
		boolean config = false;
		if (entity != null) {
			Entity root = getRootEntity(entity);
			if (root != null) {
				int idx = 0;
				Entity e = entity;
				while (e != root) {
					if (e instanceof PositionableEntity) {
						initMatrix((PositionableEntity)e, idx);
						idx++;
					} else if (e instanceof SegmentableEntity) {
						// special case of segmentable entity, must be hard coded.....
						Matrix4f m = new Matrix4f();
        				m.setIdentity();
        				AxisAngle4f r = new AxisAngle4f(1, 0, 0, (float)Math.PI/2);
        				m.setRotation(r);
						
						if (mtx_list.size() <= idx) {
							mtx_list.add(m);
						} else {
							mtx_list.set(idx, m);
						}
						idx++;
					}
					int parentID = e.getParentEntityID();
        			e = model.getEntity(parentID);
				}
				if (idx > 0) {
        			mtx.setIdentity();
       				for (int i = idx - 1; i >= 0; i--) {
            			Matrix4f m = mtx_list.get(i);
            			mtx.mul(m);
        			}
					config = true;
				}
			}
		}
		return(config);
	}
}
