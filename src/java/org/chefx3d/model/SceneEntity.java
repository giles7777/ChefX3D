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
import java.util.ArrayList;
import java.util.Map;

// internal imports
// none

/**
 * A SceneEntity is the top-level data store of a scene.  This will contain
 * all the locations within a scene.
 *
 * @author Russell Dodds
 * @version $Revision: 1.15 $
 */
public class SceneEntity extends BaseEntity implements EntitySelectionListener {

    /** The aviatrix3d rendering style prop */
    public static final String RENDER_STYLE_PROP = "RenderStyle";

    /** The aviatrix3d hide walls prop */
    public static final String HIDE_WALLS_PROP = "AutoHideWalls";

    /** The aviatrix3d hide products prop */
    public static final String HIDE_PRODUCTS_PROP = "AutoHideProducts";

    /** The aviatrix3d full color rendering style */
    public static final int RENDER_STYLE_COLOR = 0;

    /** The aviatrix3d fax ready (line art) rendering style */
    public static final int RENDER_STYLE_FAX = 1;

    /** The aviatrix3d hide walls default value */
    public static final boolean HIDE_WALLS_DEFAULT = true;

    /** The aviatrix3d hide products default value */
    public static final boolean HIDE_PRODUCTS_DEFAULT = true;

    /** The active location - the last selected location */
    private ArrayList<LocationEntity> locationList;

    /** The active location - the last selected location */
    private LocationEntity activeLocation;

    /**
     * Create an entity the represents the root of the scene graph
     *
     * @param entityID The entityID
     * @param toolProperties The properties that define the entity
     */
    public SceneEntity(
            int entityID,
            Map<String, Map<String, Object>> toolProperties) {

        super(entityID, toolProperties);

        locationList = new ArrayList<LocationEntity>();
    }

    /**
     * Create an entity the represents the root of the scene graph with
     * a custom property sheet name.
     *
     * @param entityID
     * @param propertySheetName
     * @param toolProperties
     */
    public SceneEntity(
            int entityID,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties) {
        super(entityID, propertySheetName, toolProperties);

        locationList = new ArrayList<LocationEntity>();
    }

    /**
     * Create an entity the represents the root of the scene graph with
     * a custom property sheet name.
     *
     * @param entityID
     * @param propertySheetName
     * @param toolProperties
     */
    private SceneEntity(
            int entityID,
            String propertySheetName,
            Map<String, Object> toolParams,
            Map<String, Map<String, Object>> toolProperties) {
        super(entityID, propertySheetName, toolParams, toolProperties);

        locationList = new ArrayList<LocationEntity>();
    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public SceneEntity clone(IdIssuer issuer) {

        int clonedID = issuer.issueEntityID();

        // Create the new copy
        SceneEntity clonedEntity =
            new SceneEntity(clonedID,
                              propertySheetName,
                              params,
                              properties);

        // copy all the other data over
        clonedEntity.children = new ArrayList<Entity>();

        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity child = children.get(i);
            Entity clone = child.clone(issuer);
            clone.setParentEntityID(clonedID);
            clonedEntity.children.add(clone);
            if (clone instanceof LocationEntity) {
                LocationEntity le = (LocationEntity)clone;
                locationList.add(le);
                if (child == activeLocation) {
                    clonedEntity.activeLocation = le;
                }
            }
        }

        return clonedEntity;
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
        return Entity.TYPE_WORLD;
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
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.addEntitySelectionListener(this);
            locationList.add(le);
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
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.addEntitySelectionListener(this);
            locationList.add(le);
        }
    }

    /**
     * Remove a child from the entity
     *
     * @param entity - The entity being removed
     */
    public void removeChild(Entity entity) {
        super.removeChild(entity);
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.removeEntitySelectionListener(this);
            locationList.remove(le);
            if (le == activeLocation) {
                activeLocation = null;
            }
        }
    }

    // ---------------------------------------------------------------
    // Methods defined by EntitySelectionListener
    // ---------------------------------------------------------------

    /**
     * An entity has been selected
     *
     * @param entityID The entity which changed
     * @param selected Status of selecting
     */
    public void selectionChanged(int entityID, boolean selected) {
        if (selected) {
            for (int i = 0; i < locationList.size(); i++) {
                LocationEntity le = locationList.get(i);
                if (le.getEntityID() == entityID) {
                    activeLocation = le;
                }
            }
        }
    }

    /**
     * An entity has been highlighted
     *
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Check the children of the scene for a location with the
     * name provided, return it if found, return null otherwise
     *
     * @param entityID The ID of the location
     */
    public LocationEntity getLocationEntity(int entityID) {

        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity entity = children.get(i);
            if (entity != null && entity instanceof LocationEntity) {
                if (entity.getEntityID() == entityID) {
                    return (LocationEntity)entity;
                }
            }
        }

        return null;
    }

    /**
     * Return the last selected LocationEntity, or null if none have been
     * selected.
     *
     * @return The last selected LocationEntity, or null if none have been
     * selected.
     */
    public LocationEntity getActiveLocationEntity() {

        return(activeLocation);
    }
}
