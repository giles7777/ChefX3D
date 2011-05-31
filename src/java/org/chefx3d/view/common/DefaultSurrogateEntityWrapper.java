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

package org.chefx3d.view.common;

// External imports
// none

// Local imports
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Default implementation of an entity wrapper surrogate.
 * 
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
public class DefaultSurrogateEntityWrapper implements SurrogateEntityWrapper {
	
	/** The entity */
	protected PositionableEntity entity;
		
	/** The entity's parent */
	protected PositionableEntity parent;

	/** The entity's position wrt it's parent */
	protected double[] position;
	
	/** The entity's rotation wrt it's parent */
	protected float[] rotation;
	
	/** The entity's scale */
	protected float[] scale;
			
	/** The default bounds object */
	protected OrientedBoundingBox bounds;
	
	/** The extended bounds object */
	protected OrientedBoundingBox extendedBounds;
	
	/** The enable state of this wrapper. Used to enable intersection testing */
	protected boolean enabled;
	
	/** Scratch arrays, used for comparisons */
	private double[] pos_array;
	private float[] rot_array;
	private float[] scl_array;
	
	/**
	 * Constructor
	 *
	 * @param entity The Entity for which this wrapper acts as a surrogate.
	 * @param parent The parent of the Entity for which this wrapper acts as a surrogate.
	 * @param position The position of the surrogate with respect to it's parent.
	 * @param rotation The rotation of the surrogate with respect to it's parent.
	 * @param scale The scale of the surrogate.
	 */
	public DefaultSurrogateEntityWrapper(
		PositionableEntity entity,
		PositionableEntity parent,
		double[] position,
		float[] rotation,
		float[] scale) {
		
		this.entity = entity;
		this.parent = parent;
		this.position = position;
		this.rotation = rotation;
		this.scale = scale;
		
		enabled = true;
		
		pos_array = new double[3];
		rot_array = new float[4];
		scl_array = new float[3];
	
		/////////////////////////////////////////////////////////
		// get working entity parameters
		float[] size = new float[3];
		entity.getSize(size);
		
		float[] minimum_extent = null;
		Object obj = entity.getProperty(
			PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
			PositionableEntity.MINIMUM_EXTENT_PROP);
		if ((obj != null) && (obj instanceof float[])) {
			minimum_extent = (float[])obj;
			if (minimum_extent.length < 3) {
				minimum_extent = null;
			}
		}
		
		float[] bounds_border = null;
		obj = entity.getProperty(
			PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
			PositionableEntity.BOUNDS_BORDER_PROP);
		if ((obj != null) && (obj instanceof float[])) {
			bounds_border = (float[])obj;
			if (bounds_border.length < 3) {
				bounds_border = null;
			}
		}
		
		/////////////////////////////////////////////////////////
		// calculate bounds from the entity's parameters
		float x = size[0] / 2;
		float y = size[1] / 2;
		float z = size[2] / 2;
		float[] min = new float[]{-x, -y, -z};
		float[] max = new float[]{x, y, z};
			
		bounds = new OrientedBoundingBox(min, max, scale);
		if (bounds_border != null) {
			bounds.setBorder(bounds_border);
		}
		
		/////////////////////////////////////////////////////////
		// if minimum extents are defined, check whether the entity's
		// extents comply. if not, create extended bounds
		
		boolean adjust = false;
		if (minimum_extent != null) {
			for (int i = 0; i < size.length; i++) {
				if (size[i] < minimum_extent[i]) {
					float diff = minimum_extent[i] - size[i];
					float diff2 = diff * 0.5f;
					size[i] = minimum_extent[i];
					min[i] -= diff2;
					max[i] += diff2;
					adjust = true;
				}
			}
		}
		if (adjust) {
			extendedBounds = new OrientedBoundingBox(min, max, scale);
		}
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
		enabled = state;
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
	 * @return true if the parent is different, false if they are the same
	 */
	public boolean parentChanged() {
		return(parent.getEntityID() != entity.getParentEntityID());
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
	 * @return true if the position is different, false if they are the same
	 */
	public boolean positionChanged() {
		entity.getPosition(pos_array);
		return(
			(pos_array[0] != position[0]) | 
			(pos_array[1] != position[1]) | 
			(pos_array[2] != position[2])); 
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
	 * @return true if the rotation is different, false if they are the same
	 */
	public boolean rotationChanged() {
		entity.getRotation(rot_array);
		return(
			(rot_array[0] != rotation[0]) | 
			(rot_array[1] != rotation[1]) | 
			(rot_array[2] != rotation[2]) | 
			(rot_array[3] != rotation[3])); 
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
	 * @return true if the scale is different, false if they are the same
	 */
	public boolean scaleChanged() {
		entity.getScale(scl_array);
		return(
			(scl_array[0] != scale[0]) | 
			(scl_array[1] != scale[1]) | 
			(scl_array[2] != scale[2])); 
	}
	
	/**
	 * Return the change state of the parameters in this surrogate wrapper. The returned
	 * array will contain the state for each index: POSITION, ROTATION, SCALE, PARENT.
	 *
	 * @return the array of param change states. true if the param is different, 
	 * false if they are the same.
	 */
	public boolean[] paramsChanged() {
		boolean[] change = new boolean[4];
		change[POSITION] = positionChanged();
		change[ROTATION] = rotationChanged();
		change[SCALE] = scaleChanged();
		change[PARENT] = parentChanged();
		return(change);
	}
}
