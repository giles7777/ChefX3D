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

import java.util.ArrayList;
import java.util.Map;

/**
 * Used the group a set of children together, yet the entity itself has
 * no visual representation
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public class TemplateEntity extends BasePositionableEntity {

    /** Property containing the template bounds */
    public static final String TEMPLATE_BOUNDS =
        "TemplateEntity.templateBounds";

    /** Property containing the template entity delta position */
    public static final String TEMPLATE_ENTITY_DELTA_POSITION =
        "TemplateEntity.templateEntityDelta";

    /** Property containing the template properties */
    public static final String TEMPLATE_PROPERTIES =
        "TemplateEntity.templateProperties";

    /**
     * Holds the list of entities that otherwise are children but not
     * accounted for in the children list to allow for alternative
     * parenting considerations.
     */
    public static final String TEMPLATE_ENTITIES =
        "TemplateEntity.entitiesContained";

    /** the classificationID  */
    private int classificationID;

    /**
     * Used to create the entity when you want to use the
     * default sheet names for helper methods
     *
     * @param entityID The current entityID
     * @param positionPropertySheet The sheet used for helper position methods
     * @param toolProperties The property sheets
     */
    public TemplateEntity(
            int entityID,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, toolProperties);

    }

    /**
     * Used to create the entity when you want to name just the
     * position helper sheet
     *
     * @param entityID The current entityID
     * @param positionPropertySheet The sheet used for helper position methods
     * @param toolProperties The property sheets
     */
    public TemplateEntity(
            int entityID,
            String positionParamsSheet,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, positionParamsSheet, toolProperties);

    }

    /**
     * Used to create the entity when you want to name both the
     * default and position helper sheets
     *
     * @param entityID The current entityID
     * @param propertySheet The sheet used to for helper methods
     * @param positionPropertySheet The sheet used for helper position methods
     * @param toolProperties The property sheets
     */
    public TemplateEntity(
            int entityID,
            String paramSheetName,
            String positionParamsSheet,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, paramSheetName, positionParamsSheet, toolProperties);

    }

    /**
     * Used by the clone method to deep copy the entity
     *
     * @param entityID The current entityID
     * @param propertySheet The sheet used to for helper methods
     * @param positionPropertySheet The sheet used for helper position methods
     * @param toolParams The params sheet
     * @param toolProperties The property sheets
     */
    public TemplateEntity(
            int entityID,
            String propertySheet,
            String positionPropertySheet,
            Map<String, Object> toolParams,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, propertySheet, positionPropertySheet, toolParams, toolProperties);

    }

    /**
     * Get the type, always Entity.TYPE_TEMPLATE_CONTAINER
     */
    @Override
    public int getType() {
        return Entity.TYPE_TEMPLATE_CONTAINER;
    }

    /**
     * Check if the type of this entity is one of the zone types
     *
     * @return True if one of the zone types, false otherwise
     */
    public boolean isZone() {
        return false;
    }

    /**
     * Check if the type of this entity is one of the model types
     *
     * @return True if one of the model types, false otherwise
     */
    public boolean isModel() {
        return false;
    }

    /**
     * Get the local bounds of this entity, considering all of the
     * entities that make up the template.
     *
     * Note: this
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
    @Override
    public void getBounds( float[] bounds ){

        // If there are no children bounds is zero
        if (children == null || children.size() == 0) {

            bounds[0] = 0.0f;
            bounds[1] = 0.0f;
            bounds[2] = 0.0f;
            bounds[3] = 0.0f;
            bounds[4] = 0.0f;
            bounds[5] = 0.0f;

            return;
        }

        // Track our max and min bounds
        float[] maxBounds = new float[3];
        float[] minBounds = new float[3];

        // Data extraction objects
        double[] childPos = new double[3];
        float[] childBounds = new float[6];
        int startIndex = 0;
        PositionableEntity child;

        // get the first non-assembly entity
        do {
            child = (PositionableEntity)children.get(startIndex++);
        } while (child.getCategory().equals("Category.Kit") ||
                child.getCategory().equals("Category.Template"));

        child.getPosition(childPos);
        child.getBounds(childBounds);

        // Establish the baseline for comparison
        maxBounds[0] = (float)childPos[0] + childBounds[1];
        maxBounds[1] = (float)childPos[1] + childBounds[3];
        maxBounds[2] = (float)childPos[2] + childBounds[5];

        minBounds[0] = (float)childPos[0] + childBounds[0];
        minBounds[1] = (float)childPos[1] + childBounds[2];
        minBounds[2] = (float)childPos[2] + childBounds[4];

        // Process all remaining children
        for (int i = startIndex - 1; i < children.size(); i++) {

            child = (PositionableEntity) children.get(i);

            child.getPosition(childPos);
            child.getBounds(childBounds);

            maxBounds[0] =
                Math.max(maxBounds[0], ((float)childPos[0] + childBounds[1]));
            maxBounds[1] =
                Math.max(maxBounds[1], ((float)childPos[1] + childBounds[3]));
            maxBounds[2] =
                Math.max(maxBounds[2], ((float)childPos[2] + childBounds[5]));

            minBounds[0] =
                Math.min(minBounds[0], ((float)childPos[0] + childBounds[0]));
            minBounds[1] =
                Math.min(minBounds[1], ((float)childPos[1] + childBounds[2]));
            minBounds[2] =
                Math.min(minBounds[2], ((float)childPos[2] + childBounds[4]));
        }

        bounds[0] = -((maxBounds[0] - minBounds[0])/2.0f);
        bounds[1] = ((maxBounds[0] - minBounds[0])/2.0f);
        bounds[2] = -((maxBounds[1] - minBounds[1])/2.0f);
        bounds[3] = ((maxBounds[1] - minBounds[1])/2.0f);
        bounds[4] = -((maxBounds[2] - minBounds[2])/2.0f);
        bounds[5] = ((maxBounds[2] - minBounds[2])/2.0f);
    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public TemplateEntity clone(IdIssuer issuer) {

        int clonedID = issuer.issueEntityID();

        // Create the new copy
        TemplateEntity clonedEntity =
            new TemplateEntity(
                    clonedID,
                    propertySheetName,
                    positionPropertySheet,
                    params,
                    properties);

        // copy all the other data over
        clonedEntity.children = new ArrayList<Entity>();

        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity clone = children.get(i).clone(issuer);
            clone.setParentEntityID(clonedID);
            clonedEntity.children.add(clone);
        }

        return(clonedEntity);

    }

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

    /**
     * @return the classificationID
     */
    public int getClassificationID() {
        return classificationID;
    }

    /**
     * @param classificationID the classificationID to set
     */
    public void setClassificationID(int classificationID) {
        this.classificationID = classificationID;
    }


}
