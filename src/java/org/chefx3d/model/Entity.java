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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// Internal Imports
import org.chefx3d.util.ErrorReporter;

/**
 * An object representation of an Entity.
 *
 * To create an Entity, use an appropriate command, and the CommandController's
 * execute method.  Values for all the properties defined here should be available
 * in all implementing classes.
 *
 * @author Russell Dodds
 * @version $Revision: 1.102 $
 */
public interface Entity {

    /** The basic property sheet groupings */
	
	/** Default sheet name for properties defined in this interface */
    public static final String ENTITY_PARAMS = "Entity.entityParams";
    
    /** Default sheet name for properties defined in this interface */
    public static final String EDITABLE_PROPERTIES = "Entity.entityEditableProperties";
   
    /** Sheet name for property validators */
    public static final String PROPERTY_VALIDATORS = "Entity.propertyValidators";
    
    /** Sheet name for associated entities */
    public static final String ASSOCIATED_ENTITIES = "Entity.associatedEntities";
    
    /** Default sheet name for properties */
    public static final String DEFAULT_ENTITY_PROPERTIES = "Entity.defaultProperties";

    /** If true the entity has not been added to the scene yet */
    public static final String SHADOW_ENTITY_FLAG = "Shadow State";

    /** a model type, products, objects, other items in the scene */
    public static final int TYPE_MODEL = 0;

    /** a world type, the frame of reference */
    public static final int TYPE_WORLD = 1;
    
    /** a multi-segment type, line, waypoints, closets */
    public static final int TYPE_MULTI_SEGMENT = 2;
    
    /** a building type, inclosed building */
    public static final int TYPE_BUILDING = 3;
    
    /** a location type, room, vehicle, a location */
    public static final int TYPE_LOCATION = 4;
    
    /** a content root type, the base of the renderable scene */
    public static final int TYPE_CONTENT_ROOT = 5;
    
    /** a viewpoint type, viewpoint in the scene */
    public static final int TYPE_VIEWPOINT = 6;

    /** a environment type, background, colors, shared things */
    public static final int TYPE_ENVIRONMENT = 7;
    
    /** a grouping type, container entity style */
    public static final int TYPE_CONTAINER = 8;
    
    /** a segment entity type, like a wall */
    public static final int TYPE_SEGMENT = 9;
    
    /** a vertex entity type, so... a vertex */
    public static final int TYPE_VERTEX = 10;
    
    /** a zone type, some positionable area */
    public static final int TYPE_ZONE = 11;    
   
    /** a switch type, it is a group of tools where only one can be selected/active */
    public static final int TYPE_SWITCH = 12;
    
    /** a template type that is used to position the ghost entity version.  it
     * centers the group on the mouse and allows for movement of the entire
     * set of entities.  this is then translated to a template container that 
     * has not positioning data */
    public static final int TYPE_TEMPLATE = 13;

    /** a template container type, the model will be a xml file that represents
     * the scene structure of that template.  the entity has no position and is
     * used solely as a grouping structure */
    public static final int TYPE_TEMPLATE_CONTAINER = 14;

    /** a intersection type,  */
    public static final int TYPE_INTERSECTION = 15;
    
    /** Like TYPE_MODEL but specifies its own zones */
    public static final int TYPE_MODEL_WITH_ZONES = 16;
    
    /** A zone belonging to an entity of TYPE_MODEL_WITH_ZONES */
    public static final int TYPE_MODEL_ZONE = 17;
    
    /** A groundplane zone entity */
    public static final int TYPE_GROUNDPLANE_ZONE = 18;
    
    /** Property names, for consistant lookup */
    public static final String TOOL_ID_PROP = "Entity.toolId";
    public static final String NAME_PROP = "Entity.name";
    public static final String DESCRIPTION_PROP = "Entity.description";
    public static final String KIT_ENTITY_ID_PROP = "Entity.kitID";
    public static final String TEMPLATE_ENTITY_ID_PROP = "Entity.templateID";
   
    /** Parameter names, for consistant lookup */
    public static final String TYPE_PARAM = "Entity.type";
    public static final String CATEGORY_PARAM = "Entity.category";
    public static final String CONSTRAINT_PARAM = "Entity.multiplictyConstraint";
    public static final String ICON_URL_PARAM = "Entity.iconURL";
    public static final String MODEL_URL_PARAM = "Entity.modelURL";
    public static final String HELPER_PARAM = "Entity.isHelper";
    public static final String CONTROLLER_PARAM = "Entity.isController";
    public static final String FIXED_ASPECT_PARAM = "Entity.isFixedAspect";
    public static final String FIXED_SIZE_PARAM = "Entity.isFixedSize";
    public static final String INTERFACE_ICON_PARAM = "Entity.interfaceIcons";
    
    public static final String SELECTED_PARAM = "Entity.isSelected";
    public static final String HIGHLIGHTED_PARAM = "Entity.isHighlighted";
    public static final String PARENT_ID_PARAM = "Entity.parentId";

    //TODO:  Put these in their proper place
    public static final String COLLISION_TRANSPARENCY_OVERRIDE = "Entity.collisionTransparencyOverride";
    public static final String COLLISION_TARGETS_PROPERTY = "Entity.collisionTargets";
    public static final String COLLISION_CONDITION_PROPERTY = "Entity.collisionCondition";
    public static final String COLLISION_PROP = "Entity.collision";
    
    public Entity clone(IdIssuer issuer);

    /**
     * Get this entityID
     *
     * @return The entityID
     */
    public int getEntityID();

    /**
     * DO NOT USE - Use Commands
     * Set this entityID
     *
     * @param entityID - The entityID
     */
    public void setEntityID(int entityID);

    /**
     * Get the toolID of this entity
     *
     * @return The ID property
     */
    public String getToolID();
    
    /**
     * Get the name of this entity
     *
     * @return The name property
     */
    public String getName();
    
    /**
     * Set the name of this entity
     * 
     * @param name - The new name
     */
    public void setName(String name);

    /**
     * Get the entityID of the kit this entity is a part of
     *
     * @return The kitID property
     */
    public int getKitEntityID();
    
    /**
     * Set the ID of the kit this entity is a part of
     * 
     * @param name - The new kitID
     */
    public void setKitEntityID(int kitEntityID);
    
    /**
     * Get the entityID of the template this entity is a part of
     * 
     * @return The tempalteID property
     */
    public int getTemplateEntityID();
    
    /**
     * Set the ID of the template this entity is a part of
     * 
     * @param templateEntityID - The new templateID
     */
    public void setTemplateEntityID(int templateEntityID);

    /**
     * Get the description of this entity
     *
     * @return The description property
     */
    public String getDescription();
    
    /**
     * Set the description of this entity
     * 
     * @param desc
     */
    public void setDescription(String desc);

    /**
     * Get the type of this entity
     *
     * @return The type property
     */
    public int getType();
    
    /**
     * Check if the type of this entity is one of the zone types
     * 
     * @return True if one of the zone types, false otherwise
     */
    public boolean isZone();
    
    /**
     * Check if the type of this entity is one of the model types
     * 
     * @return True if one of the model types, false otherwise
     */
    public boolean isModel();

    /**
     * Get the category of this entity
     *
     * @return The category property
     */
    public String getCategory();

    /**
     * Get the constraint this entity is assigned to
     *
     * @return The multiplicity constraint property
     */
    public MultiplicityConstraint getConstraint();

    /**
     * The URL to the 2D image that represents this entity
     * @param view The current view of this entity's icon
     * @return The icon url property
     */
    public String getIconURL(String view);
    
    /**
     *  Sets The URL to the 2D image that represents this entity
     * @param view The current view of this entity's icon
     * @param The url to store for the icon
     */
    public void setIconURL(String view, String url);

    /**
     * The URL to the 3D model that represents this entity
     *
     * @return The model url property
     */
    public String getModelURL();

    /**
     * Is this entity a helper
     *
     * @return The helper property
     */
    public boolean isHelper();

    /**
     * Is this entity a controller
     *
     * @return The controller property
     */
    public boolean isController();
    
    /**
     * Is the icon a fixed aspect ratio
     *
     * @return The fixed aspect property
     */
    public boolean isFixedAspect();

    /**
     * Get the fixedSize this entity is assigned to
     *
     * @return The fixed size property
     */
    public boolean isFixedSize();

    /**
     * Set whether this entity is selected.
     *
     * @param selected Whether this entity is selected
     */
    public void setSelected(boolean selected);

    /**
     * Get whether this entity is selected.
     *
     * @return Whether this entity is selected
     */
    public boolean isSelected();

    /**
     * Set whether to highlight this entity.
     *
     * @param highlight Whether to highlight this entity
     */
    public void setHighlighted(boolean highlight);

    /**
     * Get whether to highlight this entity.
     *
     * @return Whether to highlight this entity
     */
    public boolean isHighlighted();

    /**
     * Add the specified property to the entity.
     *
     * @param propSheet The sheet name
     * @param propName The property name
     * @param propValue The property value
     */
    public void addProperty(String propSheet, String propName, Object propValue);

    /**
     * Remove the specified property from the entity.
     *
     * @param propSheet The sheet name
     * @param propName The property name
     */
    public void removeProperty(String propSheet, String propName);
    
    /**
     * Remove the specified property sheet from the entity.
     * 
     * @param propSheet The sheet name
     */
    public void removePropertySheet(String propSheet);

    /**
     * Set a specific property.  If no property exists then no update will
     * occur and no call back will be issued.
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @param propValue The value of the property to set
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void setProperty(String propSheet, String propName, Object propValue, boolean ongoing);

    /**
     * Set multiple properties.
     *
     * This will attempt to set each property in
     * the list.  If at least one property exists then a call back to the
     * EntityPropertyListener.propertiesUpdated will be issued, otherwise
     * no event will be issued.
     *
     * If a particular property in the list does not exists then no update will
     * occur.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void setProperties(List<EntityProperty> properties);

    /**
     * Get a specific property.  If no property exists then this will
     * return null.
     *
     * @param propGroup The grouping name
     * @param propName The name of the property to set
     * @return The property value
     */
    public Object getProperty(String propGroup, String propName);

    /**
     * Get all the properties for a sheet.  If the property sheet does not
     * exist then this will return null.
     *
     * @param sheetName The sheet name
     * @return The list of properties
     */
    public List<EntityProperty> getProperties(String sheetName);

    /**
     * Get all properties.  If no properties exists then this will
     * return null.
     *
     * @return All properties
     */
    public List<EntityProperty> getProperties();
        
    /**
     * Get all the properties of the tool
     * 
     * @return A map of properties (sheet -> map [name -> value])
     */
    public Map<String, Map<String, Object>> getPropertiesMap();

    /**
     * Get the list of known property sheets, this should be ordered
     *
     * @return The list of property sheets defined
     */
    public List<String> getPropertySheets();

    /**
     * Add a new sheet to the list of property sheets.  The add should be
     * ignored if the sheet already exists
     *
     * @param sheetName - The name of the sheet
     * @param properties - The map of property name to value
     */
    public void addPropertySheet(String sheetName, Map<String, Object> properties);

    /**
     * Get the flag indicating if updates should
     * be applied to the children
     *
     * @return true/false
     */
    public boolean getUpdateChildren();

    /**
     * Set the flag indicating if updates should
     * be applied to the children
     *
     * @param bool - Do property updated cascade down to children
     */
    public void setUpdateChildren(boolean bool);

    /**
     * Add a child to the entity
     *
     * @param entity - The entity to add
     */
    public void addChild(Entity entity);

    /**
     * Remove a child from the entity
     *
     * @param entity - The entity to remove
     */
    public void removeChild(Entity entity);

    /**
     * Add a child to the entity at a particular location
     *
     * @param index The index to add at
     * @param entity The child being added
     */
    public void insertChildAt(int index, Entity entity);

    /**
     * Get the index of the entity
     *
     * @param entityID - The entity to lookup
     * @return the index
     */
    public int getChildIndex(int entityID);

    /**
     * Get an Entity at the index, returns null if not found
     *
     * @param index The index
     * @return The entity found, null if not found
     */
    public Entity getChildAt(int index);

    /**
     * Get a list of all childrenIDs of this Entity
     *
     * @return The list of childrenIDs
     */
    public int[] getChildrenIDs();

    /**
     * Get a list of all children of this Entity
     *
     * @return The list of children entities
     */
    public ArrayList<Entity> getChildren();

    /**
     * Get the number of children of this Entity
     *
     * @return The number of children
     */
    public int getChildCount();

    /**
     * Does this Entity have any children
     *
     * @return true if it has children, false otherwise
     */
    public boolean hasChildren();
    
    /**
     * Does this Entity have any starting children. Starting children are
     * children that existed before a command starting changing the children.
     * 
     * @return True if it has starting children, false otherwise
     */
    public boolean hasStartingChildren();
    
    /**
     * Set the starting children. Will overwrite any Entities already
     * set.
     * 
     * @param children Children to set as starting children
     */
    public void setStartingChildren(ArrayList<Entity> children);
    
    /**
     * Add an Entity to the starting children.
     * 
     * @param child Entity to add as a starting child.
     */
    public void addStartingChild(Entity child);
    
    /**
     * Get the starting children.
     * 
     * @return Array of starting children.
     */
    public Entity[] getStartingChildren();
    
    /**
     * Clear out the starting children.
     */
    public void clearStartingChildren();
    
    /**
     * Remove the Entity from the list of starting children.
     * 
     * @param entity Entity to remove from starting children
     */
    public void removeStartingChild(Entity entity);
    
    /**
     * Each entity should notify listeners of selection changes
     *
     * @param listener
     */
    public void addEntitySelectionListener(EntitySelectionListener listener);

    /**
     * Each entity should notify listeners of selection changes
     *
     * @param listener
     */
    public void removeEntitySelectionListener(EntitySelectionListener listener);

    /**
     * Each entity should notify listeners of property changes
     *
     * @param listener
     */
    public void addEntityPropertyListener(EntityPropertyListener listener);

    /**
     * Each entity should notify listeners of property changes
     *
     * @param listener
     */
    public void removeEntityPropertyListener(EntityPropertyListener listener);

    /**
     * Each entity should notify listeners of children changes
     *
     * @param listener
     */
    public void addEntityChildListener(EntityChildListener listener);

    /**
     * Each entity should notify listeners of children changes
     *
     * @param listener
     */
    public void removeEntityChildListener(EntityChildListener listener);

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter);
    
    /**
	 * Set the parent entity ID
	 * 
	 * @param parentEntityID
	 *            Parent entity ID value
	 */
    public void setParentEntityID(int parentEntityID);
    
    /**
	 * Get the parent entity ID
	 * 
	 * @return Entity ID of parent
	 */
	public int getParentEntityID();
	
	/**
	 * Get the name of the param sheet, set
	 * when the entity is created.
	 * 
	 * @return The name of the param sheet
	 */
	public String getParamSheetName();
}