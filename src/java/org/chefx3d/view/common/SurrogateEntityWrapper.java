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

/**
 * Defines the requirements of an entity wrapper surrogate.
 * 
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public interface SurrogateEntityWrapper extends EntityWrapper {
	
	/** Index to the position state in the paramsChanged() array */
	public static final int POSITION = 0;
	
	/** Index to the rotation state in the paramsChanged() array */
	public static final int ROTATION = 1;
	
	/** Index to the scale state in the paramsChanged() array */
	public static final int SCALE = 2;
	
	/** Index to the parent state in the paramsChanged() array */
	public static final int PARENT = 3;
	
	/**
	 * Return the parent of the Entity for which this wrapper acts as a surrogate.
	 *
	 * @return The parent of the Entity for which this wrapper acts as a surrogate.
	 */
	public PositionableEntity getParentEntity();

	/**
	 * Set the parent of the Entity for which this wrapper acts as a surrogate.
	 *
	 * @param pe The parent of the Entity for which this wrapper acts as a surrogate.
	 */
	public void setParentEntity(PositionableEntity pe);
	
	/**
	 * Return whether the parent Entity referenced in this surrogate wrapper
	 * is different than the Entity's current parent.
	 *
	 * @return true if the parent is different, false if they are the same
	 */
	public boolean parentChanged();
	
	/**
	 * Return the position of the surrogate with respect to it's parent.
	 * The argument array will be initialized with the position. If the
	 * argument is null or insufficiently sized, a new array will be 
	 * created and returned.
	 *
	 * @param pos The array to initialize with the position
	 * @return The position of the surrogate with respect to it's parent.
	 */
	public double[] getPosition(double[] pos);
	
	/**
	 * Set the position of the surrogate with respect to it's parent.
	 *
	 * @param pos The position of the surrogate with respect to it's parent.
	 */
	public void setPosition(double[] pos);
	
	/**
	 * Return whether the position referenced in this surrogate wrapper
	 * is different than the Entity's current position.
	 *
	 * @return true if the position is different, false if they are the same
	 */
	public boolean positionChanged();
	
	/**
	 * Return the rotation of the surrogate with respect to it's parent.
	 * The argument array will be initialized with the rotation. If the
	 * argument is null or insufficiently sized, a new array will be 
	 * created and returned.
	 *
	 * @param rot The array to initialize with the rotation
	 * @return The rotation of the surrogate with respect to it's parent.
	 */
	public float[] getRotation(float[] rot);
	
	/**
	 * Set the rotation of the surrogate with respect to it's parent.
	 *
	 * @param rot The rotation of the surrogate with respect to it's parent.
	 */
	public void setRotation(float[] rot);
	
	/**
	 * Return whether the rotation referenced in this surrogate wrapper
	 * is different than the Entity's current rotation.
	 *
	 * @return true if the rotation is different, false if they are the same
	 */
	public boolean rotationChanged();
	
	/**
	 * Return the scale of the surrogate.
	 * The argument array will be initialized with the scale. If the
	 * argument is null or insufficiently sized, a new array will be 
	 * created and returned.
	 *
	 * @param scl The array to initialize with the scale
	 * @return The scale of the surrogate.
	 */
	public float[] getScale(float[] scl);
	
	/**
	 * Set the scale of the surrogate
	 *
	 * @param scl The scale value
	 */
	public void setScale(float[] scl);
	
	/**
	 * Return whether the scale referenced in this surrogate wrapper
	 * is different than the Entity's current scale.
	 *
	 * @return true if the scale is different, false if they are the same
	 */
	public boolean scaleChanged();
	
	/**
	 * Return the change state of the parameters in this surrogate wrapper. The returned
	 * array will contain the state for each index: POSITION, ROTATION, SCALE, PARENT.
	 *
	 * @return the array of param change states. true if the param is different, 
	 * false if they are the same.
	 */
	public boolean[] paramsChanged();
}
