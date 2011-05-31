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
// none

// Local imports
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

import org.chefx3d.view.common.SurrogateEntityWrapper;

/**
 * Stripped down implementation of an entity wrapper surrogate 
 * used in intersection testing.
 * 
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class CollisionSurrogate implements SurrogateEntityWrapper {
	
	/** The entity */
	private PositionableEntity entity;
		
	/** The entity's parent */
	private PositionableEntity parent;

	/** Alternate setting of parent, to work around the fact that
	 * a SegmentableEntity is not Positionable */
	private int parentID;
	
	/** The entity's position wrt it's parent */
	private double[] position;
	
	/** The entity's rotation wrt it's parent */
	private float[] rotation;
	
	/** The entity's scale */
	private float[] scale;
			
	/** The default bounds object */
	private OrientedBoundingBox bounds;
	
	/** The extended bounds object */
	private OrientedBoundingBox extendedBounds;
	
	/** The enable state of this wrapper. Used to enable intersection testing */
	private boolean enabled;
	
	/** Scratch arrays, used for comparisons */
	private double[] pos_array;
	private float[] rot_array;
	private float[] scl_array;
	
	/**
	 * Constructor
	 */
	public CollisionSurrogate() {
		
		enabled = true;
		
		position = new double[3];
		rotation = new float[4];
		scale = new float[3];
	}
	
    //----------------------------------------------------------
    // Methods defined by EntityWrapper
    //----------------------------------------------------------

	/**
	 * Return the Entity for which this wrapper acts as a surrogate.
	 *
	 * @return The Entity for which this wrapper acts as a surrogate.
	 */
	public PositionableEntity getEntity() {
		return(entity);
	}

	/**
	 * Return the default bounds.
	 *
	 * @return The default bounds
	 */
	public OrientedBoundingBox getBounds() {
		return(bounds);
	}
	
	/**
	 * Return the extended bounds. If no extended bounds are
	 * defined, the default bounds object is returned.
	 *
	 * @return The extended bounds if available. Default bounds
	 * otherwise.
	 */
	public OrientedBoundingBox getExtendedBounds() {
		if (extendedBounds != null) {
			return(extendedBounds);
		} else {
			return(bounds);
		}
	}
	
	/**
	 * Set the enable state of the entity wrapper
	 *
	 * @param state The enable state of the entity wrapper
	 */
	public void setEnabled(boolean state) {
	}
	
	/**
	 * Return the enable state of the entity wrapper
	 *
	 * @return true if this is enabled, false otherwise
	 */
	public boolean isEnabled() {
		return(enabled);
	}
	
    //----------------------------------------------------------
    // Methods defined by SurrogateEntityWrapper
    //----------------------------------------------------------

	/**
	 * Return the parent of the Entity for which this wrapper acts as a surrogate.
	 *
	 * @return The parent of the Entity for which this wrapper acts as a surrogate.
	 */
	public PositionableEntity getParentEntity() {
		return(parent);
	}

	/**
	 * Set the parent of the Entity for which this wrapper acts as a surrogate.
	 *
	 * @param pe The parent of the Entity for which this wrapper acts as a surrogate.
	 */
	public void setParentEntity(PositionableEntity pe) {
		parent = pe;
	}
	
	/**
	 * Return whether the parent Entity referenced in this surrogate wrapper
	 * is different than the Entity's current parent.
	 *
	 * @return always true.
	 */
	public boolean parentChanged() {
		return(true);
	}
	
	/**
	 * Return the position of the surrogate with respect to it's parent.
	 * The argument array will be initialized with the position. If the
	 * argument is null or insufficiently sized, a new array will be 
	 * created and returned.
	 *
	 * @param pos The array to initialize with the position
	 * @return The position of the surrogate with respect to it's parent.
	 */
	public double[] getPosition(double[] pos) {
		if ((pos == null) || (pos.length < 3)) {
			pos = new double[3];
		}
		pos[0] = position[0];
		pos[1] = position[1];
		pos[2] = position[2];
		return(pos);
	}
	
	/**
	 * Set the position of the surrogate with respect to it's parent.
	 *
	 * @param pos The position of the surrogate with respect to it's parent.
	 */
	public void setPosition(double[] pos) {
		position[0] = pos[0];
		position[1] = pos[1];
		position[2] = pos[2];
	}
	
	/**
	 * Return whether the position referenced in this surrogate wrapper
	 * is different than the Entity's current position.
	 *
	 * @return always true.
	 */
	public boolean positionChanged() {
		return(true); 
	}
	
	/**
	 * Return the rotation of the surrogate with respect to it's parent.
	 * The argument array will be initialized with the rotation. If the
	 * argument is null or insufficiently sized, a new array will be 
	 * created and returned.
	 *
	 * @param rot The array to initialize with the rotation
	 * @return The rotation of the surrogate with respect to it's parent.
	 */
	public float[] getRotation(float[] rot) {
		if ((rot == null) || (rot.length < 4)) {
			rot = new float[4];
		}
		rot[0] = rotation[0];
		rot[1] = rotation[1];
		rot[2] = rotation[2];
		rot[3] = rotation[3];
		return(rot);
	}
	
	/**
	 * Set the rotation of the surrogate with respect to it's parent.
	 *
	 * @param rot The rotation of the surrogate with respect to it's parent.
	 */
	public void setRotation(float[] rot) {
		rotation[0] = rot[0];
		rotation[1] = rot[1];
		rotation[2] = rot[2];
		rotation[3] = rot[3];
	}
	
	/**
	 * Return whether the rotation referenced in this surrogate wrapper
	 * is different than the Entity's current rotation.
	 *
	 * @return always true.
	 */
	public boolean rotationChanged() {
		return(true); 
	}
	
	/**
	 * Return the scale of the surrogate.
	 * The argument array will be initialized with the scale. If the
	 * argument is null or insufficiently sized, a new array will be 
	 * created and returned.
	 *
	 * @param scl The array to initialize with the scale
	 * @return The scale of the surrogate.
	 */
	public float[] getScale(float[] scl) {
		if ((scl == null) || (scl.length < 3)) {
			scl = new float[3];
		}
		scl[0] = scale[0];
		scl[1] = scale[1];
		scl[2] = scale[2];
		return(scl);
	}
	
	/**
	 * Set the scale of the surrogate
	 *
	 * @param scl The scale value
	 */
	public void setScale(float[] scl) {
		scale[0] = scl[0];
		scale[1] = scl[1];
		scale[2] = scl[2];
	}
	
	/**
	 * Return whether the scale referenced in this surrogate wrapper
	 * is different than the Entity's current scale.
	 *
	 * @return always true.
	 */
	public boolean scaleChanged() {
		return(true);
	}
	
	/**
	 * Return the change state of the parameters in this surrogate wrapper. The returned
	 * array will contain the state for each index: POSITION, ROTATION, SCALE, PARENT.
	 *
	 * @return the array of param change states. always true.
	 */
	public boolean[] paramsChanged() {
		return(new boolean[]{true, true, true, true});
	}
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Set the Entity for which this wrapper acts as a surrogate.
	 *
	 * @param entity The Entity for which this wrapper acts as a surrogate.
	 */
	public void setEntity(PositionableEntity entity) {
		this.entity = entity;
	}

	/**
	 * Set the default bounds.
	 *
	 * @param bounds The default bounds
	 */
	public void setBounds(OrientedBoundingBox bounds) {
		this.bounds = bounds;
	}
	
	/**
	 * Set the extended bounds.
	 *
	 * @param extendedBounds The extended bounds
	 */
	public void setExtendedBounds(OrientedBoundingBox extendedBounds) {
		this.extendedBounds = extendedBounds;
	}
	
	/**
	 * Set the parent of the Entity for which this wrapper acts as a surrogate.
	 *
	 * @param pe The parent of the Entity for which this wrapper acts as a surrogate.
	 */
	public void setParentEntityID(int parentID) {
		this.parentID = parentID;
	}
	
	/**
	 * Return the parent of the Entity for which this wrapper acts as a surrogate.
	 *
	 * @return The parent of the Entity for which this wrapper acts as a surrogate.
	 */
	public int getParentEntityID() {
		return(parentID);
	}
}
