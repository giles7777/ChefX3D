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
package org.chefx3d.model;

// external imports
import java.util.Map;

// internal imports
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.view.boundingbox.ExtrusionBoundingBox;

/**
 * A base entity that can have size and position
 *
 * @author Russell Dodds
 * @version $Revision: 1.32 $
 */
public abstract class BasePositionableEntity extends BaseEntity implements
        PositionableEntity {

    protected String positionPropertySheet;

    /** Last known good position */
    protected double[] fixedPosition = new double[3];

    /** Last known good rotation */
    protected float[] fixedRotation = new float[4];

    /** Last known good scale */
    protected float[] fixedScale = new float[3];

    /**
     * Construct with default position and param sheet names.
     *
     * @param entityID
     * @param toolProperties
     */
    public BasePositionableEntity(
            int entityID,
            Map<String, Map<String, Object>> toolProperties) {

        this(entityID, DEFAULT_ENTITY_PROPERTIES, DEFAULT_ENTITY_PROPERTIES, toolProperties);
    }

    /**
     * Construct with default param sheet name.
     *
     * @param entityID
     * @param positionParamsSheet
     * @param toolProperties
     */
    public BasePositionableEntity(
            int entityID,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties) {

        this(entityID, propertySheetName, DEFAULT_ENTITY_PROPERTIES,
                toolProperties);
    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID
     *            The entity ID
     * @param baseSheetName
     *            The name of the sheet that contains the basic properties
     * @param positonableSheetName
     *            The name of the sheet that contains the position properties
     * @param toolProperties
     *            The properties of an entity as defined by the tool
     */
    public BasePositionableEntity(
            int entityID,
            String propertySheetName,
            String positionPropertySheet,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, propertySheetName, toolProperties);

        if (isFixedSize()) {
            params.put(SCALE_PROP, toolProperties.get(positionPropertySheet).get(SCALE_PROP));
            properties.get(positionPropertySheet).remove(SCALE_PROP);
        }
        this.positionPropertySheet = positionPropertySheet;
    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID
     *            The entity ID
     * @param baseSheetName
     *            The name of the sheet that contains the basic properties
     * @param positonableSheetName
     *            The name of the sheet that contains the position properties
     * @param toolProperties
     *            The properties of an entity as defined by the tool
     * @param toolParams
     *            The params of an entity
     */
    protected BasePositionableEntity(
            int entityID,
            String propertySheet,
            String positionPropertySheet,
            Map<String, Object> toolParams,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, propertySheet, toolParams, toolProperties);
        if (isFixedSize()) {
            params.put(SCALE_PROP, toolProperties.get(propertySheet).get(SCALE_PROP));
            properties.get(propertySheet).remove(SCALE_PROP);
        }
        this.positionPropertySheet = positionPropertySheet;

    }

    /**
     * Print a String representation of the Entity
     */
    public String toString() {

        double[] pos = new double[3];
        this.getPosition(pos);

        return
            this.getClass().toString() +
            " [entityID=" + entityID +
            ", name=" + getName() +
            ", parent=" + getParentEntityID() +
            ", pos=" + java.util.Arrays.toString(pos) +
            "]";
    }


    // ---------------------------------------------------------------
    // Methods defined by PositionableEntity
    // ---------------------------------------------------------------

    /**
     * Get the position of the entity.
     *
     * @param pos
     *            The position
     */
    public void getPosition(double[] pos) {

        double[] currentPos = (double[]) getProperty(positionPropertySheet,
                POSITION_PROP);

        if (currentPos == null) {
            pos[0] = 0;
            pos[1] = 0;
            pos[2] = 0;
        } else {
            pos[0] = currentPos[0];
            pos[1] = currentPos[1];
            pos[2] = currentPos[2];
        }

    }

    /**
     * DO NOT USE - Use Commands Set the current position of the entity
     *
     * @param pos
     *            The position
     * @param ongoing
     *            Is this an ongoing change or the final value?
     */
    public void setPosition(double[] pos, boolean ongoing) {
        double[] currentPos = new double[3];
        currentPos[0] = pos[0];
        currentPos[1] = pos[1];
        currentPos[2] = pos[2];

        setProperty(positionPropertySheet, POSITION_PROP, currentPos, ongoing);

    }

    /**
     * Get the current rotation of the entity
     *
     * @param rot
     *            - The rotation to return
     */
    public void getRotation(float[] rot) {

        float[] currentRot = (float[]) getProperty(positionPropertySheet,
                ROTATION_PROP);

        if (currentRot == null) {
            rot[0] = 0;
            rot[1] = 0;
            rot[2] = 1;
            rot[3] = 0;
        } else {
            rot[0] = currentRot[0];
            rot[1] = currentRot[1];
            rot[2] = currentRot[2];
            rot[3] = currentRot[3];
        }

    }

    /**
     * DO NOT USE - Use Commands Set the current rotation of the entity
     *
     * @param rot
     *            - The rotation to set
     * @param ongoing
     *            - Is this a transient change or the final value
     */
    public void setRotation(float[] rot, boolean ongoing) {

        float[] currentRot = new float[4];
        currentRot[0] = rot[0];
        currentRot[1] = rot[1];
        currentRot[2] = rot[2];
        currentRot[3] = rot[3];

        setProperty(positionPropertySheet, ROTATION_PROP, currentRot, ongoing);

    }

    /**
     * Get the size of this entity.
     *
     * @param size
     *            - The size to return
     */
    public void getSize(float[] size) {

        float[] currentSize = (float[]) params.get(SIZE_PARAM);

        if (currentSize == null) {
            size[0] = 0.000001f;
            size[0] = 0.000001f;
            size[0] = 0.000001f;
        } else {
            size[0] = currentSize[0];
            size[1] = currentSize[1];
            size[2] = currentSize[2];
        }

    }


    /**
     * Get the scale of this entity.
     *
     * @param scale
     *            The scale to return
     */
    public void getScale(float[] scale) {

        float[] currentScale;

        if (isFixedSize()) {
            currentScale = (float[]) params.get(SCALE_PROP);
        } else {
            currentScale = (float[]) getProperty(positionPropertySheet,
                    SCALE_PROP);
        }

        if (currentScale == null) {
            scale[0] = 1;
            scale[1] = 1;
            scale[2] = 1;
        } else {
            scale[0] = currentScale[0];
            scale[1] = currentScale[1];
            scale[2] = currentScale[2];
        }
    }

    /**
     * Get the local bounds of this entity.  Note: this
     * method does NOT use getStartingPosition() to adjust for
     * objects that do not begin at (0, 0, 0).  It assumes all
     * objects are initially centered at the origin!
     * <p>
     * In order to get the extents of the entity in world coordinates,
     * simply add the positioning information from getPosition() to
     * each of the bounds.
     *
     * @param bounds a float array of length six to hold the
     * local bounds of this entity.
     */
    public void getBounds( float[] bounds ){

        float[] size = new float[3];
        float[] scale = new float[3];
        double[] originOffset = new double[3];

        // Handle the special extrusion bounds case, otherwise do standard
        // processing.
        Object isExtrusionProp = getProperty(
            getParamSheetName(),
            ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);

        if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
            boolean isExtrusion = ((Boolean)isExtrusionProp).booleanValue();

            if (isExtrusion) {

                ExtrusionBoundingBox ebb = new ExtrusionBoundingBox();

                // Get the translation applied to the extrusion shape
                float[] cs_translation = (float[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);

                if (cs_translation == null) {
                    cs_translation = new float[] {0.0f, 0.0f, 0.0f};
                }

                // Calculate the extrusion shape min and max extents
                float[] sizeParam = new float[3];
                getSize(sizeParam);

                float[] cs_extent = new float[6];
                // min extents
                cs_extent[0] = 0.0f;
                cs_extent[1] = -(sizeParam[1]/2.0f);
                cs_extent[2] = -(sizeParam[2]/2.0f);

                // max extents
                cs_extent[3] = 0.0f;
                cs_extent[4] = (sizeParam[1]/2.0f);
                cs_extent[5] = (sizeParam[2]/2.0f);

                // Get the spline of the extrusion
                float[] spine = (float[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.SPINE_VERTICES_PROP);

                // Get the visibility values of the extrusion
                boolean[] visible = (boolean[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.VISIBLE_PROP);

                boolean[] miterEnable = (boolean[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.MITER_ENABLE_PROP);

                ebb.update(
                        cs_extent,
                        cs_translation,
                        spine,
                        visible,
                        miterEnable);

                float[] min = new float[3];
                float[] max = new float[3];
                ebb.getExtents(min, max);

                bounds[0] = -(max[0] - min[0])/2.0f;
                bounds[1] = (max[0] - min[0])/2.0f;
                bounds[2] = -(max[1] - min[1])/2.0f;
                bounds[3] = (max[1] - min[1])/2.0f;
                bounds[4] = -(max[2] - min[2])/2.0f;
                bounds[5] = (max[2] - min[2])/2.0f;
            }

        } else {

            // Standard handling
            getSize(size);
            getScale(scale);
            getOriginOffset(originOffset);

            float halfXWidth = (size[0]*scale[0])/2f;
            float halfYWidth = (size[1]*scale[1])/2f;
            float halfZWidth = (size[2]*scale[2])/2f;

            bounds[0] = -halfXWidth + (float)originOffset[0];
            bounds[1] = halfXWidth + (float)originOffset[0];
            bounds[2] = -halfYWidth + (float)originOffset[1];
            bounds[3] = halfYWidth + (float)originOffset[1];
            bounds[4] = -halfZWidth + (float)originOffset[2];
            bounds[5] = halfZWidth + (float)originOffset[2];
        }
    }

    /**
     * Get the starting bounds of this entity.  Note: this
     * method does NOT use getStartingPosition() to adjust for
     * objects that do not begin at (0, 0, 0).  It assumes all
     * objects are initially centered at the origin!
     * <p>
     * In order to get the extents of the entity in world coordinates,
     * simply add the positioning information from getPosition() to
     * each of the bounds.
     *
     * @param bounds a float array of length six to hold the
     * local bounds of this entity.
     */
    public void getStartingBounds(float[] bounds){

        float[] size = new float[3];
        float[] scale = new float[3];
        double[] originOffset = new double[3];

        // Handle the special extrusion bounds case, otherwise do standard
        // processing.
        Object isExtrusionProp = getProperty(
            getParamSheetName(),
            ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);

        if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
            boolean isExtrusion = ((Boolean)isExtrusionProp).booleanValue();

            if (isExtrusion) {

                ExtrusionBoundingBox ebb = new ExtrusionBoundingBox();

                // Get the translation applied to the extrusion shape
                float[] cs_translation = (float[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);

                if (cs_translation == null) {
                    cs_translation = new float[] {0.0f, 0.0f, 0.0f};
                }

                // Calculate the extrusion shape min and max extents
                float[] sizeParam = new float[3];
                getSize(sizeParam);

                float[] cs_extent = new float[6];
                // min extents
                cs_extent[0] = 0.0f;
                cs_extent[1] = -(sizeParam[1]/2.0f) - cs_translation[1];
                cs_extent[2] = -(sizeParam[2]/2.0f) - cs_translation[2];

                // max extents
                cs_extent[3] = 0.0f;
                cs_extent[4] = (sizeParam[1]/2.0f) + cs_translation[1];
                cs_extent[5] = (sizeParam[2]/2.0f) + cs_translation[2];

                // Get the spline of the extrusion
                float[] spine = (float[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);

                // Get the visibility values of the extrusion
                boolean[] visible = (boolean[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.VISIBLE_PROP);

                boolean[] miterEnable = (boolean[])
                    getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ExtrusionEntity.MITER_ENABLE_PROP);

                ebb.update(
                        cs_extent,
                        cs_translation,
                        spine,
                        visible,
                        miterEnable);

                float[] min = new float[3];
                float[] max = new float[3];
                ebb.getExtents(min, max);

                bounds[0] = -(max[0] - min[0])/2.0f;
                bounds[1] = (max[0] - min[0])/2.0f;
                bounds[2] = -(max[1] - min[1])/2.0f;
                bounds[3] = (max[1] - min[1])/2.0f;
                bounds[4] = -(max[2] - min[2])/2.0f;
                bounds[5] = (max[2] - min[2])/2.0f;
            }

        } else {

            getSize(size);
            getStartingScale(scale);
            getOriginOffset(originOffset);

            float halfXWidth = (size[0]*scale[0])/2f;
            float halfYWidth = (size[1]*scale[1])/2f;
            float halfZWidth = (size[2]*scale[2])/2f;

            bounds[0] = -halfXWidth + (float)originOffset[0];
            bounds[1] = halfXWidth + (float)originOffset[0];
            bounds[2] = -halfYWidth + (float)originOffset[1];
            bounds[3] = halfYWidth + (float)originOffset[1];
            bounds[4] = -halfZWidth + (float)originOffset[2];
            bounds[5] = halfZWidth + (float)originOffset[2];
        }
    }

    /**
     * Get the extends of this entity.  This simply adds the positioning
     * information from getPosition() to each of the bounds.
     *
     * @param min a float array of length three to hold the min extends of this entity.
     * @param max a float array of length three to hold the max extends of this entity.
     */
    public void getExtents(float[] min, float[] max){

        double[] pos = new double[3];
        getPosition(pos);

        float[] bounds = new float[6];
        getBounds(bounds);

        min[0] = (float)pos[0] + bounds[0];
        min[1] = (float)pos[1] + bounds[2];
        min[2] = (float)pos[2] + bounds[4];

        max[0] = (float)pos[0] + bounds[1];
        max[1] = (float)pos[1] + bounds[3];
        max[2] = (float)pos[2] + bounds[5];

    }

    /**
     * DO NOT USE - Use Commands Set the current scale of the entity
     *
     * @param scale
     *            - The scale to set
     */
    public void setScale(float[] scale) {

        float[] currentScale = new float[3];
        currentScale[0] = scale[0];
        currentScale[1] = scale[1];
        currentScale[2] = scale[2];

        setProperty(positionPropertySheet, SCALE_PROP, currentScale, false);

    }

    /**
     * Compare positioning of this Entity with the provide Entity.
     *
     * @param compare
     *            The Entity to compare to
     * @return true if same location, false otherwise
     */
    public boolean samePosition(PositionableEntity compare) {

        // check the entity positions
        double[] position = new double[3];
        getPosition(position);

        double[] comparePosition = new double[3];
        if (compare instanceof PositionableEntity) {
            ((PositionableEntity) compare).getPosition(comparePosition);
        } else {
            return false;
        }

        if ((position[0] != comparePosition[0])
                || (position[1] != comparePosition[1])
                || (position[2] != comparePosition[2])) {
            return false;
        }

        return true;

    }

    /**
     * Set the position held by the entity when not moving.
     * Should only be set after an AddEntityCommand or a MoveEntityCommand.
     * Never set as from Transient commands.
     *
     * @param startingPosition The fixed position value
     */
    public void setStartingPosition(double[] startingPosition){

        double[] startPos = new double[3];
        startPos[0] = startingPosition[0];
        startPos[1] = startingPosition[1];
        startPos[2] = startingPosition[2];

        setProperty(
                positionPropertySheet,
                START_POSITION_PROP,
                startPos,
                false);
    }

    /**
     * Get the fixed position held by the entity before any movement has
     * occurred.
     *
     * @param startingPosition The fixed position value
     */
    public void getStartingPosition(double[] startingPosition){

        double[] startPos = (double[]) getProperty(
                positionPropertySheet,
                START_POSITION_PROP);

        if (startPos == null) {
            startingPosition[0] = 0;
            startingPosition[1] = 0;
            startingPosition[2] = 0;
        } else {
            startingPosition[0] = startPos[0];
            startingPosition[1] = startPos[1];
            startingPosition[2] = startPos[2];
        }
     }

    /**
     * Get the original rotation value before current changes were made.
     *
     * @param startingRotation
     */
    public void setStartingRotation(float[] startingRotation){

        float[] startRot = new float[4];
        startRot[0] = startingRotation[0];
        startRot[1] = startingRotation[1];
        startRot[2] = startingRotation[2];
        startRot[3] = startingRotation[3];

        setProperty(
                positionPropertySheet,
                START_ROTATION_PROP,
                startRot,
                false);

    }

    /**
     * Set the last known good rotation value to new value.
     *
     * @param startingRotation
     */
    public void getStartingRotation(float[] startingRotation){

        float[] startRot = (float[]) getProperty(
                positionPropertySheet,
                START_ROTATION_PROP);

        if (startRot == null) {
            startingRotation[0] = 0;
            startingRotation[1] = 1;
            startingRotation[2] = 0;
            startingRotation[3] = 0;
        } else {
            startingRotation[0] = startRot[0];
            startingRotation[1] = startRot[1];
            startingRotation[2] = startRot[2];
            startingRotation[3] = startRot[3];
        }
    }

    /**
     * Get the original scale value before current changes were made.
     *
     * @param startingScale
     */
    public void setStartingScale(float[] startingScale){

        float[] startScale = new float[3];
        startScale[0] = startingScale[0];
        startScale[1] = startingScale[1];
        startScale[2] = startingScale[2];

        setProperty(
                positionPropertySheet,
                START_SCALE_PROP,
                startScale,
                false);

    }

    /**
     * Set the last known good scale value to new value.
     *
     * @param startingScale
     */
    public void getStartingScale(float[] startingScale){

        float[] startScale = (float[]) getProperty(
                positionPropertySheet,
                START_SCALE_PROP);

        if (startScale == null) {
            startingScale[0] = 0;
            startingScale[1] = 1;
            startingScale[2] = 0;
        } else {
            startingScale[0] = startScale[0];
            startingScale[1] = startScale[1];
            startingScale[2] = startScale[2];
        }
    }

    /**
     * Get the origin offset value.
     *
     * @param originOffset double[3] xyz offset
     */
    public void getOriginOffset(double[] originOffset) {

        double[] offset = (double[])params.get(ORIGIN_OFFSET_PROP);

        if(offset == null){
            return;
        }

        originOffset[0] = offset[0];
        originOffset[1] = offset[1];
        originOffset[2] = offset[2];
    }

    /**
     * Set the origin offset value.
     *
     * @param originOffset double[3] xyz offset
     */
    public void setOriginOffset(double[] originOffset) {

        params.put(ORIGIN_OFFSET_PROP, originOffset);
    }

    /**
     * Get all positionable information.  This will be a deep copy
     * so that changes to the entity will not affect the returned object.
     *
     * @return The positionable info
     */
    public PositionableData getPositionableData() {
        double[] currentPos = (double[]) getProperty(positionPropertySheet,
                POSITION_PROP);

        if (currentPos == null) {
            currentPos = new double[3];
        }

        float[] currentRot = (float[]) getProperty(positionPropertySheet,
                ROTATION_PROP);

        if (currentRot == null) {
            currentRot = new float[] {0,0,1,0};
        }

        float[] scale = new float[3];
        getScale(scale);

        PositionableData data = new PositionableData(currentPos, currentRot,
            scale);

        return data;
    }

    /**
     * Set all positionable information.  Replaces all positionable values.
     *
     * @param data The positionable info
     */
    public void setPositionableData(PositionableData data) {
        setPosition(data.pos, false);
        setScale(data.scale);
        setRotation(data.rot, false);
    }
}
