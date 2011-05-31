/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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

// External Imports

// Internal Imports

/**
 * Defines the requirements for accessing an Entity's position, rotation, or size.
 *
 * @author Russell Dodds
 * @version $Revision: 1.23 $
 */
public interface PositionableEntity extends Entity {

    /** Default sheet name for properties defined in this interface */
    public static final String POSITIONABLE_PARAMS = "PositionableEntity.positionableParams";
    public static final String SIZE_PARAM = "PositionableEntity.size";

    /** Properties that must be defined */
    public static final String POSITION_PROP = "PositionableEntity.position";
    public static final String ROTATION_PROP = "PositionableEntity.rotation";
    public static final String SCALE_PROP = "PositionableEntity.scale";
    public static final String ORIGIN_OFFSET_PROP = "Entity.originOffset";
    public static final String START_POSITION_PROP = "PositionableEntity.startPos";
    public static final String START_ROTATION_PROP = "PositionableEntity.startRot";
    public static final String START_SCALE_PROP = "PositionableEntity.startScale";

    /** Optional property specifying the minimum extent of an object to be
     *  used for picking and bounds intersection testing. Must be a float[]
     * of length 3 if defined */
    public static final String MINIMUM_EXTENT_PROP =
        "PositionableEntity.minimumExtent";

    /** Optional property specifying a border area to be added to the bounding
     * object of an entity. Must be a float[]
     * of length 3 if defined */
    public static final String BOUNDS_BORDER_PROP =
        "PositionableEntity.boundsBorder";

    /**
     * Get the current position of the entity.
     *
     * @param pos The array to place the position in.
     */
    public void getPosition(double[] pos);

    /**
     * Set the current position of the entity.
     *
     * @param pos The new position value.
     * @param ongoing Is this a transient change or the final value
     */
    public void setPosition(double[] pos, boolean ongoing);

    /**
     * Get the current rotation of the entity
     *
     * @param rot The array to place the rotation in.
     */
    public void getRotation(float[] rot);

    /**
     * Set the current rotation of the entity.
     *
     * @param rot The new rotation value.
     * @param ongoing Is this a transient change or the final value
     */
    public void setRotation(float[] rot, boolean ongoing);

    /**
     * Get the size of this entity.
     *
     * @param size The array in which to place the size.
     */
    public void getSize(float[] size);

    /**
     * Get the scale of this entity.
     *
     * @param scale The array in which to place the scale.
     */
    public void getScale(float[] scale);

    /**
     * Get the local bounds of this entity.
     *
     * @param bounds a float array of length six to hold the bounds.
     */
    public void getBounds(float[] bounds);

    /**
     * Get the local bounds before any changes were made.
     *
     * @param bounds a float array of length six to hold the bounds.
     */
    public void getStartingBounds(float[] bounds);

    /**
     * Get the extends of this entity.  This simply adds the positioning
     * information from getPosition() to each of the bounds.
     *
     * @param min a float array of length three to hold the min extends of this entity.
     * @param max a float array of length three to hold the max extends of this entity.
     */
    public void getExtents(float[] min, float[] max);

    /**
     * Set the scale of this entity.
     *
     * @param scale The array to place the scale in.
     */
    public void setScale(float[] scale);

    /**
     * Compare the position of this Entity with the provided Entity.
     *
     * @param compare The PositionableEntity to compare to
     * @return true if same location, false otherwise
     */
    public boolean samePosition(PositionableEntity compare);

    /**
     * Set the position held by the entity when not moving.
     * Should only be set after an AddEntityCommand or a MoveEntityCommand.
     * Never set as from Transient commands.
     *
     * @param startingPosition The fixed position value
     */
    public void setStartingPosition(double[] startingPosition);

    /**
     * Get the fixed position held by the entity before any movement has
     * occurred.
     *
     * @param startingPosition The fixed position value
     */
    public void getStartingPosition(double[] startingPosition);

    /**
     * Set the last known good rotation value to new value.
     *
     * @param startingRotation
     */
    public void setStartingRotation(float[] startingRotation);

    /**
     * Get the original rotation value before current changes were made.
     *
     * @param startingRotation
     */
    public void getStartingRotation(float[] startingRotation);

    /**
     * Set the last known good scale value to new value.
     *
     * @param startingScale
     */
    public void setStartingScale(float[] startingScale);

    /**
     * Get the original scale value before current changes were made.
     *
     * @param startingScale
     */
    public void getStartingScale(float[] startingScale);

    /**
     * Get the origin offset value.
     *
     * @param originOffset double[3] xyz offset
     */
    public void getOriginOffset(double[] originOffset);

    /**
     * Set the origin offset value.
     *
     * @param originOffset double[3] xyz offset
     */
    public void setOriginOffset(double[] originOffset);

    /**
     * Get all positionable information.  This will be a deep copy
     * so that changes to the entity will not affect the returned object.
     *
     * @return The positionable info
     */
    public PositionableData getPositionableData();

    /**
     * Set all positionable information.  Replaces all positionable values.
     *
     * @param data The positionable info
     */
    public void setPositionableData(PositionableData data);

}