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
import java.awt.Color;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

// internal imports

/**
 * An object representation of a location which contains a ViewpointContainerEntity
 * and an Entity that holds all the geometry entities.
 *
 * A LocationEntity should contain only 2 children:
 *      * ViewpointContainerEntity: this holds the set of viewpoints
 *      * BaseEntity: this holds the set of renderable entities as its children
 *
 * @author Russell Dodds
 * @version $Revision: 1.27 $
 */
public class LocationEntity extends BaseEntity implements EnvironmentEntity {

    /** The active zoneID */
    public static final String ACTIVE_ZONE_PROP = "LocationEntity.activeZoneID";

    /** Flag used to determine to use the zone selector or wall selector component */
    public static final String USE_ZONE_SELECTOR_PROP = "LocationEntity.useZoneSelector";

    /** Property name referencing the image map */
    public static final String IMAGE_MAP_PROP = "LocationEntity.imageMap";

    /** The viewpoint container */
    private ViewpointContainerEntity viewpointContainerEntity;

    /** The entity that contains viewable items */
    private ContentContainerEntity contentContainerEntity;

    /** The location needs a default entity to select */
    private Entity defaultSelectionEntity;

    /** Name of the property sheet containing environment properties */
    private String environmentSheetName;

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param roomEntityID The room entity ID
     * @param toolProperties The properties of an entity as defined by the tool
     */
    public LocationEntity(
            int roomEntityID,
            Map<String, Map<String, Object>> toolProperties) {

        this(roomEntityID, DEFAULT_ENTITY_PROPERTIES,
            DEFAULT_ENVIRONMENT_PROPERTIES, toolProperties);

    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The sheet to save default props to
     * @param environmentSheetName The sheet to save environment props to
     * @param toolProperties The properties of an entity as defined by the tool
     */
    public LocationEntity(
            int entityID,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties) {

        this(entityID, DEFAULT_ENTITY_PROPERTIES, propertySheetName, toolProperties);

    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The sheet to save default props to
     * @param environmentSheetName The sheet to save environment props to
     * @param toolProperties The properties of an entity as defined by the tool
     */
    public LocationEntity(
            int entityID,
            String propertySheetName,
            String environmentSheetName,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, propertySheetName, toolProperties);

        this.environmentSheetName = environmentSheetName;

        defaultSelectionEntity = null;

    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The sheet to save default props to
     * @param environmentSheetName The sheet to save environment props to
     * @param toolProperties The properties of an entity as defined by the tool
     */
    private LocationEntity(
            int entityID,
            String propertySheetName,
            Map<String, Object> toolParams,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, propertySheetName, toolParams, toolProperties);

        //this.environmentSheetName = DEFAULT_ENTITY_PROPERTIES;
    }


    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public LocationEntity clone(IdIssuer issuer) {

        int clonedID = issuer.issueEntityID();

        // Create the new copy
        LocationEntity clonedEntity =
            new LocationEntity(clonedID,
                              propertySheetName,
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

        return clonedEntity;
    }

    // ----------------------------------------------------------
    // Methods defined by EnvironmentEntity
    // ----------------------------------------------------------

    /**
     * Get the BackgroundColor property
     *
     * @return The BackgroundColor property
     */
    public Color getBackgroundColor() {
        return (Color)getProperty(environmentSheetName, BACKGROUND_COLOR_PROP);
    }

    /**
     * Set the BackgroundColor property
     *
     * @param color The new color
     */
    public void setBackgroundColor(Color color) {
        setProperty(environmentSheetName, BACKGROUND_COLOR_PROP, color, false);
    }

    /**
     * Get the GroundColor property
     *
     * @return The GroundColor property
     */
    public Color getGroundColor() {
        return (Color)getProperty(environmentSheetName, GROUND_COLOR_PROP);
    }

    /**
     * Set the GroundColor property
     *
     * @param color The new color
     */
    public void setGroundColor(Color color) {
        setProperty(environmentSheetName, GROUND_COLOR_PROP, color, false);
    }

    /**
     * Get the SharedColor1 property
     *
     * @return The SharedColor1 property
     */
    public Color getSharedColor1() {
        return (Color)getProperty(environmentSheetName, SHARED_COLOR1_PROP);
    }

    /**
     * Set the SharedColor1 property
     *
     * @param color The new color
     */
    public void setSharedColor1(Color color) {
        setProperty(environmentSheetName, SHARED_COLOR1_PROP, color, false);
    }

    /**
     * Get the SharedColor2 property
     *
     * @return The SharedColor2 property
     */
    public Color getSharedColor2() {
        return (Color)getProperty(environmentSheetName, SHARED_COLOR2_PROP);
    }

    /**
     * Set the SharedColor2 property
     *
     * @param color The new color
     */
    public void setSharedColor2(Color color) {
        setProperty(environmentSheetName, SHARED_COLOR2_PROP, color, false);
    }

    /**
     * Get the GroundNormal property
     *
     * @return The GroundNormal property
     */
    public float[] getGroundNormal() {
        return (float[])getProperty(environmentSheetName, GROUND_NORMAL_PROP);
    }

    /**
     * Set the GroundNormal property
     *
     * @param normal The new normal
     */
    public void setGroundNormal(float[] normal) {
        setProperty(environmentSheetName, GROUND_NORMAL_PROP, normal, false);
    }

    // ----------------------------------------------------------
    // Overridden Methods
    // ----------------------------------------------------------

    /**
     * Get the type of this entity
     *
     * @return The type property
     */
    public int getType() {
        return Entity.TYPE_LOCATION;
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
     * Add a child to the entity
     *
     * @param entity - The entity being added
     */
    public void addChild(Entity entity) {
        super.addChild(entity);
        if (entity instanceof ViewpointContainerEntity) {
            viewpointContainerEntity = (ViewpointContainerEntity)entity;
        } else if (entity instanceof ContentContainerEntity) {
            contentContainerEntity = (ContentContainerEntity)entity;
        }
    }

    /**
     * Add a child to the entity at a particular location
     *
     * @param index The index to add at
     * @param entity The child being added
     */
    public void insertChildAt(int index, Entity entity) {
        super.insertChildAt(index, entity);
        if (entity instanceof ViewpointContainerEntity) {
            viewpointContainerEntity = (ViewpointContainerEntity)entity;
        } else if (entity instanceof ContentContainerEntity) {
            contentContainerEntity = (ContentContainerEntity)entity;
        }
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Helper method to get the current list of viewpoints
     *
     * @return The list of viewpoints
     */
    public List<ViewpointEntity> getViewpoints() {

        List<ViewpointEntity> viewpoints = new ArrayList<ViewpointEntity>();
        int len = viewpointContainerEntity.getChildCount();
        for (int i = 0; i < len; i++) {
            viewpoints.add((ViewpointEntity)viewpointContainerEntity.getChildAt(i));
        }

        return viewpoints;
    }

    /**
     * Helper method the add new viewpoints
     *
     * @param viewpoint The viewpoint to add
     */
    public void addViewpoint(ViewpointEntity viewpoint) {
        viewpointContainerEntity.addChild(viewpoint);
    }

    /**
     * Get the viewpoint container entity
     *
     * @return The viewpoint container entity
     */
    public ViewpointContainerEntity getViewpointContainerEntity() {
        return viewpointContainerEntity;
    }

    /**
     * Helper method to get the current list of viewable content
     *
     * @return The list of content
     */
    public List<Entity> getContents() {

        List<Entity> contents = new ArrayList<Entity>();
        int len = contentContainerEntity.getChildCount();
        for (int i = 0; i < len; i++) {
            contents.add(contentContainerEntity.getChildAt(i));
        }

        return contents;
    }

    /**
     * Helper method the add new content
     *
     * @param entity The content to add
     */
    public void addContent(Entity content) {
        contentContainerEntity.addChild(content);
    }

    /**
     * Get the content container entity
     *
     * @return The content container entity
     */
    public ContentContainerEntity getContentContainerEntity() {
        return contentContainerEntity;
    }

    /**
     * Get the defaultSelectionEntity that was set.  If it has not been set it
     * tries to get the first child of the contentContainerEntity.  If
     * that has no children then this returns null
     *
     * @return the defaultSelectionEntity
     */
    public Entity getDefaultSelectionEntity() {

        if (defaultSelectionEntity == null && contentContainerEntity != null) {
            defaultSelectionEntity = contentContainerEntity.getChildAt(0);
            if (contentContainerEntity.getChildCount() > 0) {
                ArrayList<Entity> contentList =
                    (ArrayList<Entity>)getContents();
                int len = contentList.size();
                for (int i = 0; i < len; i++) {
                    Entity entity = contentList.get(i);
                    if (entity.isZone()) {
                        defaultSelectionEntity = entity;
                    }
                }
            }
        }
        return defaultSelectionEntity;
    }

    /**
     * Set the default selection entity.
     *
     * @param defaultSelectionEntity the defaultSelectionEntity to set
     */
    public void setDefaultSelectionEntity(Entity defaultSelectionEntity) {
        this.defaultSelectionEntity = defaultSelectionEntity;
    }

    /**
     * @return the activeZoneID
     */
    public int getActiveZoneID() {
        Integer zoneID = (Integer)getProperty(propertySheetName, ACTIVE_ZONE_PROP);

        if (zoneID == null) {
            return getDefaultSelectionEntity().getEntityID();
        }

        return zoneID;
    }

    /**
     * @param activeZoneID the activeZoneID to set
     */
    public void setActiveZoneID(int activeZoneID) {
        setProperty(propertySheetName, ACTIVE_ZONE_PROP, activeZoneID, false);
    }

    /**
     * Get the name of the param sheet, set
     * when the entity is created.
     *
     * @return The name of the param sheet
     */
    public String getParamSheetName() {
        return environmentSheetName;
    }

    /**
     * @return True to use the Zone Selector, False to use the Wall Selector
     */
    public boolean useZoneSelector() {
        Boolean useZoneSelector = (Boolean)getProperty(propertySheetName, USE_ZONE_SELECTOR_PROP);

        if (useZoneSelector == null) {
            return false;
        }

        return useZoneSelector;
    }

    /**
     * @param useZoneSelector the flag to set
     */
    public void setUseZoneSelector(boolean useZoneSelector) {
        setProperty(propertySheetName, USE_ZONE_SELECTOR_PROP, useZoneSelector, false);
    }

    /**
     * Get the path to the image file
     *
     * @return The path to the image file
     */
    public String getImageMap() {
        return (String)getProperty(propertySheetName, IMAGE_MAP_PROP);
    }

    /**
     * Set the path to the image file
     *
     * @param imageCoordPath The path to the image file
     */
    public void setImageMap(String imageMapPath) {
        setProperty(propertySheetName, IMAGE_MAP_PROP, imageMapPath, false);
    }

}
