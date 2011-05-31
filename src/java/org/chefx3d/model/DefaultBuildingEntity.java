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

// External Imports
import java.awt.geom.AffineTransform;
import java.util.*;

// Internal Imports
import org.chefx3d.util.*;

/**
 * An object representation of an entity.  A entity consists of:
 *      - parameters that are defined at creation and never changed
 *      - properties that are updated by the end-user or internal processes
 *
 * Properties can be stored in any number of sheets, the sheets are used to
 * group properties an whatever manner is required by the application.
 *
 * Selection and HighLight are not model parameters so they can be public here.
 *
 * @author Russell Dodds
 * @version $Revision: 1.56 $
 */
public class DefaultBuildingEntity
    implements EntityPropertyListener,
               Entity,
               SegmentableEntity,
               PositionableEntity {

    public static final String COLOR_PROP = "BuildingEntity.colorProp";

       /** The tool used to create new vertices */
    protected AbstractVertexTool vertexTool;

    /** The tool used to create new segments */
    protected AbstractSegmentTool segmentTool;

    /** The entityID */
    private int entityID;

    /** The properties of the entity, sheet -> Map (name -> value) */
    private Map<String, Map<String, Object>> properties;

    /** The properties of the entity, sheet -> Map (name -> value) */
    private Map<String, Object> params;

    private Map<String, Object> segmentProperties;
    private Map<String, Object> vertexProperties;

    /** Is this entity selected */
    private boolean selected;

    /** Is this entity highlighted */
    private boolean highlighted;

    /** The currently selected vertex */
    private int selectedVertexID;

    /** The currently highlited vertex */
    private int highlightedVertexID;

    /** The currently selected segment */
    private int selectedSegmentID;

    /** The currently highlited segment */
    private int highlightedSegmentID;

    /** list of children entities */
    private ArrayList<Entity> children;

    /** list of children vertices entities */
    private ArrayList<VertexEntity> vertices;

    /** list of children segment entities */
    private ArrayList<SegmentEntity> segments;

    /** A lookup map of vertices identified by an ID */
    private HashMap<Integer, VertexEntity> vertexMap;

    /** A lookup map of segments identified by an ID */
    private HashMap<Integer, SegmentEntity> segmentMap;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The parameter sheet to use */
    private String propertySheetName;

    /** The segment sheet to use */
    private String segmentSheetName;

    /** The vertex sheet to use */
    private String vertexSheetName;

    /** A lookup map of vertices identified by an ID */
    protected HashMap<VertexEntity, Integer> segmentCount;

    /** The list of EntityPropertyListeners */
    private ArrayList<EntityPropertyListener> propertyListeners;

    /** The list of EntityChildListeners */
    private ArrayList<EntityChildListener> childrenListeners;

    /** The list of EntitySelectionListeners */
    private ArrayList<EntitySelectionListener> selectionListeners;

    /** The transform for the sequence */
    private AffineTransform transform;

    /** The fixed position set after any move cmd, referenced to reset pos */
    private double[] fixedPosition = new double[3];
    
    /** list of starting children entities */
    private ArrayList<Entity> startingChildren;

    /**
     * Create a building entity. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID - The entityID issued by the model
     * @param propertySheetName - The name of the base property sheet
     * @param toolProperties - The properties
     * @param segmentSheetName - The name of the base segment sheet
     * @param segmentProps - The segment properties
     * @param vertexSheetName - The name of the vertex sheet
     * @param vertexProps - the vertex properties
     */
    public DefaultBuildingEntity(
            int entityID,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties,
            AbstractSegmentTool segmentTool,
            AbstractVertexTool vertexTool) {

        this.entityID = entityID;
        this.vertexTool = vertexTool;
        this.segmentTool = segmentTool;

        vertexMap = new HashMap<Integer, VertexEntity>();
        vertices = new ArrayList<VertexEntity>();

        segmentMap = new HashMap<Integer, SegmentEntity>();
        segments = new ArrayList<SegmentEntity>();

        children = new ArrayList<Entity>();

        propertyListeners = new ArrayList<EntityPropertyListener>();
        childrenListeners = new ArrayList<EntityChildListener>();
        selectionListeners = new ArrayList<EntitySelectionListener>();
        segmentCount = new HashMap<VertexEntity, Integer>();
        selected = false;
        highlighted = false;

        // clone the tool parameters
        params = PropertyUtilities.clone(toolProperties.get(Entity.ENTITY_PARAMS));

        // set the properties
        properties = new HashMap<String, Map<String, Object>>();

        // clone the other properties
        Iterator<Map.Entry<String, Map<String, Object>>> index =
            toolProperties.entrySet().iterator();

        Map<String, Object> clonedProperties;
        while (index.hasNext()) {

            Map.Entry<String, Map<String, Object>> mapEntry = index.next();

            // get the key, value pairing
            String sheetName = mapEntry.getKey();
            Map<String, Object> sheetProperties = mapEntry.getValue();

            if (sheetName.equals(Entity.ENTITY_PARAMS)) {
                continue;
            }

            if (sheetProperties == null) {
                clonedProperties = new HashMap<String, Object>();
            } else {
                clonedProperties = PropertyUtilities.clone(sheetProperties);
            }

            properties.put(sheetName, clonedProperties);

        }

        highlightedVertexID = -1;
        highlightedSegmentID = -1;

        selectedVertexID = -1;
        selectedSegmentID = -1;

        errorReporter = DefaultErrorReporter.getDefaultReporter();

    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public DefaultBuildingEntity clone(IdIssuer issuer) {

        int clonedID = issuer.issueEntityID();

        //TODO: deal with cloning issue

        // Create the new copy
        DefaultBuildingEntity clonedEntity =
            new DefaultBuildingEntity(
                    clonedID,
                    propertySheetName,
                    properties,
                    segmentTool,
                    vertexTool);

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

    /**
     * Compare the given details to this one to see if they are equal. Equality
     * is defined as pointing to the same clipPlane source, with the same
     * transformation value.
     *
     * @param o The object to compare against
     * @return true if these represent identical objects
     */
    public boolean equals(Object o) {
        if (!(o instanceof DefaultEntity))
            return false;

        Entity e = (Entity) o;

        if (e.getEntityID() != entityID)
            return false;

        return true;
    }

    /**
     * Calculate the hashcode for this object.
     *
     * @return the entityID
     */
    public int hashCode() {
        // TODO: Not a very good hash
        return entityID;
    }

    // ----------------------------------------------------------
    // Methods required by EntityPropertyListener interface
    // ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(
            int entityID,
            String propertySheet,
            String propertyName) {
        // ignored
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(
            int entityID,
            String propertySheet,
            String propertyName) {
        // ignored
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void propertyUpdated(
            int entityID,
            String propertySheet,
            String propertyName, boolean ongoing) {

        // handle movement of the building
        if (propertyName.equals(PositionableEntity.POSITION_PROP)) {

            Iterator<EntityPropertyListener> i = propertyListeners.iterator();
            while(i.hasNext()) {
                EntityPropertyListener l = i.next();
                l.propertyUpdated(entityID, propertySheet, propertyName, ongoing);
            }

        }

    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        // ignored
    }

    // ---------------------------------------------------------------
    // Methods defined by Entity
    // ---------------------------------------------------------------

    /**
     * Get this entityID
     *
     * @return The entityID
     */
    public int getEntityID() {
        return entityID;
    }

    /**
     * DO NOT USE - Use Commands
     * Set this entityID
     *
     * @param entityID - The entityID
     */
    public void setEntityID(int entityID) {
        this.entityID = entityID;
    }

    /**
     * Get the name of the entity
     *
     * @return The name property
     */
    public String getToolID() {
        return (String)getProperty(propertySheetName, TOOL_ID_PROP);
    }

    /**
     * Get the name of this entity
     *
     * @return The name property
     */
    public String getName() {
        return (String)getProperty(propertySheetName, NAME_PROP);
    }

    /**
     * Set the name of this entity
     *
     * @param name - The new name
     */
    public void setName(String name) {
        setProperty(propertySheetName, NAME_PROP, name, false);
    }

    /**
     * Get the ID of the kit this entity is a part of
     *
     * @return The kitID property
     */
    public int getKitEntityID() {
        Integer id = (Integer)getProperty(propertySheetName, KIT_ENTITY_ID_PROP);
        if (id == null) {
            return -1;
        }
        return id;
    }

    /**
     * Set the ID of the kit this entity is a part of
     *
     * @param name - The new kitID
     */
    public void setKitEntityID(int kitEntityID) {
        setProperty(propertySheetName, KIT_ENTITY_ID_PROP, kitEntityID, false);
    }

    /**
     * Set the ID of the template this entity is a part of
     *
     * @param templateEntityID The new templateID
     */
    public void setTemplateEntityID(int templateEntityID) {

        setProperty(
                propertySheetName,
                TEMPLATE_ENTITY_ID_PROP,
                templateEntityID,
                false);
    }

    /**
     * Get the ID of the template this entity is a part of
     *
     * @return The templateID property
     */
    public int getTemplateEntityID() {

        Integer id = (Integer)getProperty(
                propertySheetName,
                TEMPLATE_ENTITY_ID_PROP);

        if (id == null) {
            return -1;
        }

        return id;
    }

    /**
     * Get the description of this entity
     *
     * @return The description property
     */
    public String getDescription() {
        return (String)getProperty(propertySheetName, DESCRIPTION_PROP);
    }

    /**
     * Set the description of this entity
     *
     * @param desc
     */
    public void setDescription(String desc) {
        setProperty(propertySheetName, DESCRIPTION_PROP, desc, false);
    }

    /**
     * Get the type of this entity
     *
     * @return The type property
     */
    public int getType() {
        return (Integer)params.get(TYPE_PARAM);
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
     * Get the category of this entity
     *
     * @return The category property
     */
    public String getCategory() {
        return (String)params.get(CATEGORY_PARAM);
    }

    /**
     * Get the constraint this entity is assigned to
     *
     * @return The multiplicity constraint property
     */
    public MultiplicityConstraint getConstraint() {
        return (MultiplicityConstraint)params.get(CONSTRAINT_PARAM);
    }

    /**
     * The URL to the 2D image that represents this entity
     * @param view Ignored in this particular class
     * @return The icon URL property
     */
    public String getIconURL(String view) {
        return (String)params.get(ICON_URL_PARAM);
    }

    /**
     * The URL to the 3D model that represents this entity
     *
     * @return The model url property
     */
    public String getModelURL() {
        return (String)params.get(MODEL_URL_PARAM);
    }

    /**
     * Set whether this entity is selected.
     *
     * @param selected Whether to selected this entity
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Get whether this entity is selected.
     *
     * @return Whether this entity is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set whether to highlight this entity.
     *
     * @param highlight Whether to highlight this entity
     */
    public void setHighlighted(boolean highlight) {
        highlighted = highlight;
    }

    /**
     * Get whether to highlight this entity.
     *
     * @return Whether to highlight this entity
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * DO NOT USE - Use Commands
     * Add the specified property to the document.
     *
     * @param propSheet The sheet name
     * @param propName The property name
     * @param propValue The property
     */
    public void addProperty(String propSheet, String propName, Object propValue) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties != null) {
            sheetProperties.put(propName, propValue);

            Iterator<EntityPropertyListener> i = propertyListeners.iterator();
            while(i.hasNext()) {
                EntityPropertyListener l = i.next();
                l.propertyAdded(entityID, propSheet, propName);
            }

        }

    }

    /**
     * DO NOT USE - Use Commands
     * Remove the specified property from the entity.
     *
     * @param propSheet The sheet name
     * @param propName The property name
     */
    public void removeProperty(String propSheet, String propName) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties != null) {
            sheetProperties.remove(propName);

            Iterator<EntityPropertyListener> i = propertyListeners.iterator();
            while(i.hasNext()) {
                EntityPropertyListener l = i.next();
                l.propertyRemoved(entityID, propSheet, propName);
            }

        }

    }

    /**
     * DO NOT USE - Use Commands
     * Set a specific property.
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @param propValue The property value
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void setProperty(
            String propSheet,
            String propName,
            Object propValue, boolean ongoing) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties != null) {
            sheetProperties.put(propName, propValue);

            Iterator<EntityPropertyListener> i = propertyListeners.iterator();
            while(i.hasNext()) {
                EntityPropertyListener l = i.next();
                l.propertyUpdated(entityID, propSheet, propName, ongoing);
            }

        }

    }

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
    public void setProperties(List<EntityProperty> properties) {
        // not implemented
    }

    /**
     * Get a specific property.
     *
     * @param propSheet The grouping name
     * @param propName The name of the property to set
     * @return The value of the property
     */
    public Object getProperty(String propSheet, String propName) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties == null) {
            return null;
        }

        return sheetProperties.get(propName);

    }

    /**
     * Get all the properties for a sheet.  If the property sheet does not
     * exist then this will return null.
     *
     * @param propSheet The sheet name
     * @return The list of properties
     */
    public List<EntityProperty> getProperties(String propSheet) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties == null) {
            return null;
        }

        ArrayList<EntityProperty> propertyList = new ArrayList<EntityProperty>();

        Iterator<Map.Entry<String, Object>> index =
            sheetProperties.entrySet().iterator();

        EntityProperty entityProperties;
        while (index.hasNext()) {

            Map.Entry<String, Object> mapEntry = index.next();

            // get the key, value pairing
            String propName = mapEntry.getKey();
            Object propValue = mapEntry.getValue();

            entityProperties = new EntityProperty(
                entityID,
                propSheet,
                propName,
                propValue);

            propertyList.add(entityProperties);

        }
        return propertyList;

    }


    /**
     * Get all properties.  If no properties exists then this will
     * return null.
     *
     * @return All properties
     */
    public List<EntityProperty> getProperties() {

        if (properties.isEmpty()) {
            return null;
        }

        ArrayList<EntityProperty> propertyList = new ArrayList<EntityProperty>();
        EntityProperty entityProperties;

        Iterator<Map.Entry<String, Map<String, Object>>> index =
            properties.entrySet().iterator();

        while (index.hasNext()) {

            Map.Entry<String, Map<String, Object>> mapEntry = index.next();

            // get the key, value pairing
            String propSheet = mapEntry.getKey();
            Map<String, Object> sheetProps = mapEntry.getValue();

            Iterator<Map.Entry<String, Object>> index1 =
                sheetProps.entrySet().iterator();

            while (index1.hasNext()) {

                Map.Entry<String, Object> mapEntry1 = index1.next();

                // get the key, value pairing
                String propName = mapEntry1.getKey();
                Object propValue = mapEntry1.getValue();

                entityProperties = new EntityProperty(
                    entityID,
                    propSheet,
                    propName,
                    propValue);

                propertyList.add(entityProperties);

            }

        }
        return propertyList;

    }

    /**
     * Get all the properties of the tool
     *
     * @return A map of properties (sheet -> map [name -> value])
     */
    public Map<String, Map<String, Object>> getPropertiesMap() {
        return properties;
    }


    /**
     * Get the list of known property sheets, this should be ordered
     *
     * @return The list of property sheets defined
     */
    public List<String> getPropertySheets() {

        ArrayList<String> sheets = new ArrayList<String>();
        sheets.add(propertySheetName);

        return sheets;

    }

    /**
     * Add a new sheet to the list of property sheets.  The add should be
     * ignored if the sheet already exists
     *
     * @param sheetName - The name of the sheet
     * @param sheetProperties - The map of property name to value
     */
    public void addPropertySheet(String sheetName, Map<String, Object> sheetProperties) {

        List<String> sheets = getPropertySheets();

        if (sheets.contains(sheetName)) {
            return;
        }

        Map<String, Object> clonedProperties;
        if (sheetProperties == null) {
            clonedProperties = new HashMap<String, Object>();
        } else {
            clonedProperties = PropertyUtilities.clone(sheetProperties);
        }

        properties.put(sheetName, clonedProperties);

    }

    /**
     * Add a child to the entity
     *
     * @param entity - The entity to add
     */
    public void addChild(Entity entity) {

        if (!children.contains(entity)) {

            // add the entityID to the internal data structure
            children.add(entity);

            Iterator<EntityChildListener> i = childrenListeners.iterator();
            while(i.hasNext()) {
                EntityChildListener l = i.next();
                l.childAdded(this.getEntityID(), entity.getEntityID());
            }

        }

    }

    /**
     * Remove a child from the entity
     *
     * @param entity - The entity to remove
     */
    public void removeChild(Entity entity) {

        if (children.contains(entity)) {

            // remove the entityID from the internal data structure
            children.remove(entity);

            Iterator<EntityChildListener> i = childrenListeners.iterator();
            while(i.hasNext()) {
                EntityChildListener l = i.next();
                l.childRemoved(this.getEntityID(), entity.getEntityID());
            }

        }

    }

    /**
     * Add a child to the entity at a particular location
     *
     * @param index The index to add at
     * @param entity The child being added
     */
    public void insertChildAt(int index, Entity entity) {

        if (!children.contains(entity)) {

            int actualIndex;

            // add the entityID to the internal data structure
            if (index >= children.size()) {
                actualIndex = children.size();
                children.add(entity);
            } else if (index < 0) {
                actualIndex = 0;
                children.add(0, entity);
            } else {
                actualIndex = index;
                children.add(index, entity);
            }

            Iterator<EntityChildListener> i = childrenListeners.iterator();
            while(i.hasNext()) {
                EntityChildListener l = i.next();
                l.childInsertedAt(this.getEntityID(), entity.getEntityID(), actualIndex);
            }

        }

    }

    /**
     * Get the index of the entity
     *
     * @param entityID - The entity to look for
     * @return the index
     */
    public int getChildIndex(int entityID) {

        Entity check;
        for (int i = 0; i < children.size(); i++) {
            check = children.get(i);

            if (check.getEntityID() == entityID) {
                return i;
            }
        }
        return -1;

    }

    /**
     * Get an Entity at the index, returns null if not found
     *
     * @param index The index
     * @return The entity found, null if not found
     */
    public Entity getChildAt(int index) {
        if (children.size() > index) {
            return children.get(index);
        }
        return null;
    }

    /**
     * Get a list of all childrenIDs of this Entity
     *
     * @return The list of childrenIDs
     */
    public int[] getChildrenIDs() {

        int[] ret_val = new int[children.size()];

        int cnt = 0;

        for (Iterator<Entity> itr = children.iterator(); itr.hasNext();) {
            ret_val[cnt++] = itr.next().getEntityID();
        }

        return ret_val;

    }

    /**
     * Get a list of all children of this Entity
     *
     * @return The list of children entities
     */
    public ArrayList<Entity> getChildren() {

        ArrayList<Entity> entites = new ArrayList<Entity>();

        for (int i = 0; i < children.size(); i++) {
            entites.add(children.get(i));
        }
        return entites;

    }

    /**
     * Get the number of children of this Entity
     *
     * @return The number of children
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Does this Entity have any children
     *
     * @return true if it has children, false otherwise
     */
    public boolean hasChildren() {
        if (getChildCount() > 0) {
            return true;
        }
       return false;
    }
    
    /**
     * Does this Entity have any starting children. Starting children are
     * children that existed before a command starting changing the children.
     * 
     * @return True if it has starting children, false otherwise
     */
    public boolean hasStartingChildren() {
    	return !startingChildren.isEmpty();
    }
    
    /**
     * Set the starting children. Will overwrite any Entities already
     * set.
     * 
     * @param children Children to set as starting children
     */
    public void setStartingChildren(ArrayList<Entity> children) {
    	clearStartingChildren();
    	startingChildren.addAll(children);
    }
    
    /**
     * Add an Entity to the starting children.
     * 
     * @param child Entity to add as a starting child.
     */
    public void addStartingChild(Entity child) {
    	startingChildren.add(child);
    }
    
    /**
     * Get the starting children.
     * 
     * @return Array of starting children.
     */
    public Entity[] getStartingChildren() {
    	
    	Entity[] children = new Entity[startingChildren.size()];
    	startingChildren.toArray(children);
    	return children;
    }
    
    /**
     * Clear out the starting children.
     */
    public void clearStartingChildren() {
    	startingChildren.clear();
    }
    
    /**
     * Remove the Entity from the list of starting children.
     * 
     * @param entity Entity to remove from starting children
     */
    public void removeStartingChild(Entity entity) {
    	
    	startingChildren.remove(entity);
    }

    /**
     * Each entity should notify listeners of property changes
     *
     * @param listener
     */
    public void addEntityPropertyListener(EntityPropertyListener listener) {
        if (!propertyListeners.contains(listener)) {
            propertyListeners.add(listener);
        }
    }

    /**
     * Each entity should notify listeners of property changes
     *
     * @param listener
     */
    public void removeEntityPropertyListener(EntityPropertyListener listener) {
        propertyListeners.remove(listener);
    }

    /**
     * Each entity should notify listeners of children changes
     *
     * @param listener
     */
    public void addEntityChildListener(EntityChildListener listener) {
        if (!childrenListeners.contains(listener)) {
            childrenListeners.add(listener);
        }
    }

    /**
     * Each entity should notify listeners of children changes
     *
     * @param listener
     */
    public void removeEntityChildListener(EntityChildListener listener) {
        childrenListeners.remove(listener);
    }

    /**
     * Each entity should notify listeners of selection changes
     *
     * @param listener
     */
    public void addEntitySelectionListener(EntitySelectionListener listener) {
        if (!selectionListeners.contains(listener)) {
            selectionListeners.add(listener);
        }
    }

    /**
     * Each entity should notify listeners of selection changes
     *
     * @param listener
     */
    public void removeEntitySelectionListener(EntitySelectionListener listener) {
        selectionListeners.remove(listener);
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the flag indicating if updates should
     * be applied to the children
     *
     * @return true/false
     */
    public boolean getUpdateChildren() {

        return false;

    }

    /**
     * Set the flag indicating if updates should
     * be applied to the children
     *
     * @param bool
     */
    public void setUpdateChildren(boolean bool) {
    }

    //----------------------------------------------------------
    // Methods for SegmentableEntity
    //----------------------------------------------------------


    /**
     * Get the bounds of the segmentable entity's vertices.  If the
     * entity contains no vertices it will return null.
     *
     * @return an array of length six containing, in order: the minimum
     * x value, maximum x value, minimum y value, maximum y value, minimum
     * z value and maximum z value.
     */
    public double[] getBounds(){

        // ensure that this segmentable entity contains vertices
        ArrayList<VertexEntity> vertices = this.getVertices();
        if(vertices == null || vertices.size() == 0)
            return null;

        // grab the first vertex
        VertexEntity vertex = vertices.get(0);
        double[] vertexPos = new double[3];
        vertex.getPosition(vertexPos);

        //
        // these six values represent the real-world minimum
        // and maximum bounds around the SegmentableEntity.
        //
        double minX = vertexPos[0];
        double maxX = vertexPos[0];
        double minY = vertexPos[1];
        double maxY = vertexPos[1];
        double minZ = vertexPos[2];
        double maxZ = vertexPos[2];

        // iterate through the list of vertices and update
        // the maximum and minimum bounds
        for(int i = 0; i < vertices.size(); i++){
            vertex = vertices.get(i);
            vertex.getPosition(vertexPos);

            if (vertexPos[0] > maxX)
                maxX = vertexPos[0];
            if (vertexPos[0] < minX)
                minX = vertexPos[0];

            if (vertexPos[1] > maxY)
                maxY = vertexPos[2];
            if (vertexPos[2] < minY)
                minY = vertexPos[2];

            if (vertexPos[2] > maxZ)
                maxZ = vertexPos[2];
            if (vertexPos[2] < minZ)
                minZ = vertexPos[2];
        }

        return new double[]{ minX, maxX, minY, maxY, minZ, maxZ};
    }

    /**
     * Get the starting bounds of the segmentable entity's vertices.  If the
     * entity contains no vertices it will return null.
     *
     * @return an array of length six containing, in order: the minimum
     * x value, maximum x value, minimum y value, maximum y value, minimum
     * z value and maximum z value.
     */
    public double[] getStartingBounds(){

        // ensure that this segmentable entity contains vertices
        ArrayList<VertexEntity> vertices = this.getVertices();
        if(vertices == null || vertices.size() == 0)
            return null;

        // grab the first vertex
        VertexEntity vertex = vertices.get(0);
        double[] vertexPos = new double[3];
        vertex.getStartingPosition(vertexPos);

        //
        // these six values represent the real-world minimum
        // and maximum bounds around the SegmentableEntity.
        //
        double minX = vertexPos[0];
        double maxX = vertexPos[0];
        double minY = vertexPos[1];
        double maxY = vertexPos[1];
        double minZ = vertexPos[2];
        double maxZ = vertexPos[2];

        // iterate through the list of vertices and update
        // the maximum and minimum bounds
        for(int i = 0; i < vertices.size(); i++){
            vertex = vertices.get(i);
            vertex.getStartingPosition(vertexPos);

            if (vertexPos[0] > maxX)
                maxX = vertexPos[0];
            if (vertexPos[0] < minX)
                minX = vertexPos[0];

            if (vertexPos[1] > maxY)
                maxY = vertexPos[2];
            if (vertexPos[2] < minY)
                minY = vertexPos[2];

            if (vertexPos[2] > maxZ)
                maxZ = vertexPos[2];
            if (vertexPos[2] < minZ)
                minZ = vertexPos[2];
        }

        return new double[]{ minX, maxX, minY, maxY, minZ, maxZ};
    }

    /**
     * Is this entity a line tool
     */

    public boolean isLine() {

        return false;

    }

    /**
     * Get the fixedLength this entity is assigned to
     */
    public boolean isFixedLength() {

        return false;

    }

    /**
     * Get the name of the Tool that created this segment entity
     */
    public String getToolName() {

        return (String)params.get(TOOL_NAME_PROP);

    }


    /**
     * Get a segment
     *
     * @param segmentID - The segment to return
     */
    public SegmentEntity getSegment(int segmentID) {

        return segmentMap.get(segmentID);

    }

    /**
     * DO NOT USE - Use Commands
     * Add a segment to this tool.
     *
     * @param segmentID - the world model issued ID
     * @param startVertexID - the world model issue ID of the starting vertex
     * @param endVertexID - the world model issue ID of the ending vertex
     * @param exteriorSegment - is this part of the exterior shape
     * @param segmentProps - the properties of the segment
     */
    public void addSegment(SegmentEntity segment) {

/*
System.out.println("addSegment");
System.out.println("    segmentID: " + segmentID);
System.out.println("    startVertexID: " + startVertexID);
System.out.println("    endVertexID: " + endVertexID);
*/

        // found out where to put it
        int index = -1;
        int len = segments.size();
        for (int i = 0; i < len; i++) {
            SegmentEntity check = segments.get(i);

            if (check.getEndVertexEntity().getEntityID() ==
                    segment.getStartVertexEntity().getEntityID()) {
                index = i + 1;
                break;
            } else if (check.getStartVertexEntity().getEntityID() ==
                    segment.getEndVertexEntity().getEntityID()) {
                index = i;
                break;
            }

        }

        // add the vertex to the lookup map for easy access
        segmentMap.put(segment.getEntityID(), segment);

//System.out.println("    index: " + index);

        if (index > 0) {
            // add the segment to the list at the correct index
            segments.add(index, segment);
        } else {
            // add the segment to the end of the list
            segments.add(segment);
        }

        addChild(segment);

    }

    /**
     * DO NOT USE - Use Commands
     * Remove a segment from this tool.
     *
     * @param segmentID The segment to remove
     */
    public void removeSegment(int segmentID) {

        SegmentEntity segment = segmentMap.get(segmentID);

        if (selectedSegmentID == segmentID)
            selectedSegmentID = -1;

        if (highlightedSegmentID == segmentID)
            highlightedSegmentID = -1;

        if (segmentMap.keySet().contains(segmentID)) {
            segmentMap.remove(segmentID);
        }

        // remove the segment from the list
        segments.remove(segment);
        removeChild(segment);

    }

    /**
     * DO NOT USE - Use Commands
     * Add a segment vertex to this tool.
     *
     * @param pos The position of the segment
     */
    public int addVertex(VertexEntity vertex, int index) {

        int vertexID = vertex.getEntityID();

        vertex.addEntityPropertyListener(this);

/*
System.out.println("addVertex");
System.out.println("    entity: " + this);
System.out.println("    vertexID: " + vertexID);
System.out.println("    pos: " + pos[0] + ", " + pos[1] + ", " + pos[2]);
*/

        // add the vertex to the lookup map for easy access
        vertexMap.put(vertexID, vertex);

        // add the children list
        if (index >= 0 && index < vertices.size()) {

            // add the vertex to the ordered list
            vertices.add(index, vertex);
            insertChildAt(index, vertex);

        } else {

            vertices.add(vertex);
            addChild(vertex);

        }

        return getChildIndex(vertexID);

    }



    /**
     * DO NOT USE - Use Commands
     * Move a vertex of this SegmentSequence.
     *
     * @param vertexID The vertexID
     * @param pos The position of the segment
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void moveVertex(int vertexID, double[] pos, boolean ongoing) {

        VertexEntity vertex = vertexMap.get(vertexID);
        vertex.setPosition(pos, ongoing);

        // TODO: not sure this needs to happen here
        selectedVertexID = vertexID;

    }

    /**
     * DO NOT USE - Use Commands
     *
     * Update a vertex of this building.
     *
     * @param vertexID - The vertexID
     * @param propSheet - The property sheet
     * @param propName - The property name
     * @param propValue - The property value
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void updateVertex(
            int vertexID,
            String propSheet,
            String propName,
            String propValue, boolean ongoing) {

        VertexEntity vertex = vertexMap.get(vertexID);
        vertex.setProperty(propSheet, propName, propValue, ongoing);

    }

    /**
     * DO NOT USE - Use Commands
     *
     * Remove a vertex from this building.
     *
     * @param vertexID The vertexID
     */
    public void removeVertex(int vertexID) {

        if (selectedVertexID == vertexID)
            selectedVertexID = -1;

        if (highlightedVertexID == vertexID)
            highlightedVertexID = -1;

        // get the entity
        VertexEntity vertex = vertexMap.get(vertexID);
        vertex.removeEntityPropertyListener(this);

        // remove from the children list
        removeChild(vertex);

        // remove from the vertices list
        vertices.remove(vertex);

        // remove the vertex from the lookup map
        vertexMap.remove(vertexID);

    }

    /**
     * Get a vertex
     *
     * @param vertexID - The vertexID to return
     */
    public VertexEntity getVertex(int vertexID) {

        return vertexMap.get(vertexID);

    }

    /**
     * Set the currently selected vertex index
     *
     * @param vertexID
     */
    public void setSelectedVertexID(int vertexID) {
        selectedVertexID = vertexID;

        VertexEntity vertex = vertexMap.get(vertexID);
        if (vertex != null)
            vertex.setSelected(true);

    }

    /**
     * Get the currently selected vertex index, -1 if none selected
     *
     * @return selectedVertexID
     */
    public int getSelectedVertexID() {
        return selectedVertexID;
    }

    /**
     * Set the currently selected segment index
     *
     * @param segmentID
     */
    public void setSelectedSegmentID(int segmentID) {
        selectedSegmentID = segmentID;
    }

    /**
     * Get the currently selected segment index, -1 if none selected
     *
     * @return selectedSegmentID
     */
    public int getSelectedSegmentID() {
        return selectedSegmentID;
    }

    /**
     * Set the currently selected vertex index
     *
     * @param vertexID
     */
    public void setHighlightedVertexID(int vertexID) {
        highlightedVertexID = vertexID;

        VertexEntity vertex = vertexMap.get(vertexID);
        if (vertex != null)
            vertex.setHighlighted(true);

    }

    /**
     * Get the currently selected vertex index, -1 if none selected
     *
     * @return selectedVertexID
     */
    public int getHighlightedVertexID() {
        return highlightedVertexID;
    }

    /**
     * Get the currently selected vertex null if none selected
     *
     * @return SegmentVertex
     */
    public VertexEntity getSelectedVertex() {

        if (selectedVertexID >= 0) {
            VertexEntity vertex = vertexMap.get(selectedVertexID);
            return vertex;
        }

        return null;

    }

    /**
     * Set the currently selected segment index
     *
     * @param segmentID
     */
    public void setHighlightedSegmentID(int segmentID) {
        highlightedSegmentID = segmentID;
    }

    /**
     * Get the currently selected segment index, -1 if none selected
     *
     * @return selectedSegmentID
     */
    public int getHighlightedSegmentID() {
        return highlightedSegmentID;
    }


    /**
     * Get the currently selected segment null if none selected
     *
     * @return Segment
     */
    public SegmentEntity getSelectedSegment() {
        if ((segmentMap != null) && (segmentMap.size() > 0)) {
            return segmentMap.get(selectedSegmentID);
        } else {
            return null;
        }
    }


    /**
     * Get the currently selected vertex position, null if none selected
     *
     * @return vertex position is World Coords
     */
    public double[] getSelectedVertexPosition() {

        if (selectedVertexID >= 0) {
            VertexEntity vertex = vertexMap.get(selectedVertexID);

            double[] pos = new double[3];
            vertex.getPosition(pos);

            return pos;
        }

        return null;

    }

    /**
     * Return true if a vertex is currently selected, false otherwise
     *
     * @return selectedVertexID
     */
    public boolean isVertexSelected() {

        if (selectedVertexID > -1) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Return true if a segment is currently selected, false otherwise
     *
     * @return selectedSegmentID
     */
    public boolean isSegmentSelected() {

        if (selectedSegmentID > -1) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Get the list of segments. If it contains no segments it
     * will return null.
     *
     * @return The segments
     */
    public ArrayList<SegmentEntity> getSegments() {
        return segments;
    }

    /**
     * Get the list of segments containing the specified vertex.
	 * If the entity contains no segments or the vertex is not
	 * contained in any available segments, it will return null.
     *
	 * @param ve The VertexEntity to search the segments for.
     * @return The list of segments, or null
     */
    public ArrayList<SegmentEntity> getSegments(VertexEntity ve) {

		ArrayList<SegmentEntity> seg_list = new ArrayList<SegmentEntity>();

		int num = 0;
		int len = segments.size();
		for (int i = 0; i < len; i++) {
			SegmentEntity se = segments.get(i);
			if (ve == se.getStartVertexEntity()) {
				seg_list.add(se);
				num++;
			} else if (ve == se.getEndVertexEntity()) {
				seg_list.add(se);
				num++;
			}
		}
		if (num > 0) {
        	return(seg_list);
		} else {
			return(null);
		}
    }

    /**
     * Get the list of segments. If it contains no segments it
     * will return null.
     *
     * @return The segments
     */
    public ArrayList<VertexEntity> getVertices() {
        return vertices;
    }

    /**
     * Will always return false, there is no start vertex
     *
     * @param vertexID
     * @return false
     */
    public boolean isStart(int vertexID) {
        return false;
    }

    /**
     * Get the first vertex of the sequence
     *
     * @return The position or null if no start
     */
    public double[] getStartPosition() {
        return null;
    }

    /**
     * Will always return false, there is no end vertex
     * for a building
     *
     * @param vertexID
     * @return false
     */
    public boolean isEnd(int vertexID) {
        return false;
    }

    /**
     * Get the last vertex of the sequence
     *
     * @return The position or null if no vertices
     */
    public double[] getEndPosition() {
        return null;
    }

    /**
     * Get the spacial transformation object
     *
     * @return The matrix transform
     */
    public AffineTransform getTransform() {
        return transform;
    }

    /**
     * Set the spacial transformation object
     *
     * @param transform
     */
    public void setTransform(AffineTransform transform) {
        this.transform = transform;
    }

    /**
     * Get the ID of the last vertex of the sequence. If no verticies are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getStartVertexID() {
        return -1;
    }

    /**
     * Get the ID of the last vertex of the sequence. If no verticies are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getEndVertexID() {
        return -1;
    }

    /**
     * Get the ID of the last segment of the sequence. If no segments are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getLastSegmentID() {
        return -1;
    }

    /**
     * Does the sequence contain the position provided
     *
     * @param pos
     * @return True if the position exists
     */
    public boolean contains(double[] pos) {

        int vertexID = getVertexID(pos);

        return (vertexID >= 0);
    }

    /**
     * Get the vertexId for the position specified,
     *  returns the ID of the first vertex matched
     *
     * @param pos position
     * @return The vertexId
     */
    public int getVertexID(double[] pos) {

        int vertexId = -1;

        Iterator<Map.Entry<Integer, VertexEntity>> index =
            vertexMap.entrySet().iterator();

        while (index.hasNext()) {

            Map.Entry<Integer, VertexEntity> mapEntry = index.next();

            VertexEntity vertex = mapEntry.getValue();
            double[] position = new double[3];
            vertex.getPosition(position);

            if ((pos[0] == position[0]) &&
                    (pos[1] == position[1]) &&
                    (pos[2] == position[2])) {

                vertexId = mapEntry.getKey();
                break;
            }

        }

        return vertexId;

    }

    //----------------------------------------------------------
    // Methods Required for PositionableEntity
    //----------------------------------------------------------

    /**
     *
     */
    public void getBounds(float[] bounds){
        double[] doubleBounds = getBounds();
        bounds[0] = (float)doubleBounds[0];
        bounds[1] = (float)doubleBounds[0];
        bounds[2] = (float)doubleBounds[0];
        bounds[3] = (float)doubleBounds[0];
        bounds[4] = (float)doubleBounds[0];
        bounds[5] = (float)doubleBounds[0];
    }

    /**
     * Get the starting bounds of the segmentable entity.
     *
     * @param bounds float array of bounds values
     */
    public void getStartingBounds(float[] bounds){

        double[] doubleBounds = getStartingBounds();
        bounds[0] = (float)doubleBounds[0];
        bounds[1] = (float)doubleBounds[0];
        bounds[2] = (float)doubleBounds[0];
        bounds[3] = (float)doubleBounds[0];
        bounds[4] = (float)doubleBounds[0];
        bounds[5] = (float)doubleBounds[0];
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
        max[2] = (float)pos[1] + bounds[5];

    }

    /**
     * Get the position of the entity.
     *
     * @param pos The position
     */
    public void getPosition(double[] pos) {

        double[] currentPos =
            (double[])getProperty(propertySheetName, POSITION_PROP);

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
     * DO NOT USE - Use Commands
     * Set the current position of the entity
     *
     * @param pos - The position to set
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void setPosition(double[] pos, boolean ongoing) {

        double[] currentPos = new double[3];
        currentPos[0] = pos[0];
        currentPos[1] = pos[1];
        currentPos[2] = pos[2];

        setProperty(propertySheetName, POSITION_PROP, currentPos, ongoing);

    }

    /**
     * Get the current rotation of the entity
     *
     * @param rot - The rotation to return
     */
    public void getRotation(float[] rot) {

        float[] currentRot =
            (float[])getProperty(propertySheetName, ROTATION_PROP);


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
     * DO NOT USE - Use Commands
     * Set the current rotation of the entity
     *
     * @param rot - The rotation to set
     * @param ongoing - Is this a transient change or the final value
     */
    public void setRotation(float[] rot, boolean ongoing) {

        float[] currentRot = new float[4];
        currentRot[0] = rot[0];
        currentRot[1] = rot[1];
        currentRot[2] = rot[2];
        currentRot[3] = rot[3];

        setProperty(propertySheetName, ROTATION_PROP, currentRot, ongoing);

    }

    /**
     * Get the size of this entity.
     *
     * @param size The array to place the size values
     */
    public void getSize(float[] size) {

        float[] currentSize =
            (float[])params.get(SIZE_PARAM);

        size[0] = currentSize[0];
        size[1] = currentSize[1];
        size[2] = currentSize[2];

    }

    /**
     * Get the scale of this entity.
     *
     * @param scale The array to place the scale values
     */
    public void getScale(float[] scale) {

        float[] currentScale;
        if (isFixedSize()) {
            currentScale =
                (float[])params.get(SCALE_PROP);
        } else {
            currentScale =
                (float[])getProperty(propertySheetName, SCALE_PROP);
        }

        scale[0] = currentScale[0];
        scale[1] = currentScale[1];
        scale[2] = currentScale[2];

    }

    /**
     * DO NOT USE - Use Commands
     * Set the current position of the entity
     *
     * @param scale - The scale to set
     */
    public void setScale(float[] scale) {

        float[] currentScale = new float[3];
        currentScale[0] = scale[0];
        currentScale[1] = scale[1];
        currentScale[2] = scale[2];

        setProperty(propertySheetName, SCALE_PROP, currentScale, false);

    }

    /**
     * Compare positioning of this Entity with the provide Entity.
     *
     * @param compare The Entity to compare to
     * @return true if same location, false otherwise
     */
    public boolean samePosition(PositionableEntity compare) {


        // check the entity positions
        double[] position = new double[3];
        getPosition(position);

        double[] comparePosition = new double[3];
        if (compare instanceof PositionableEntity) {
            ((PositionableEntity)compare).getPosition(comparePosition);
        } else {
            return false;
        }

        if ((position[0] != comparePosition[0]) ||
            (position[1] != comparePosition[1]) ||
            (position[2] != comparePosition[2])) {
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

        fixedPosition[0] = startingPosition[0];
        fixedPosition[1] = startingPosition[1];
        fixedPosition[2] = startingPosition[2];
    }

    /**
     * Get the fixed position held by the entity before any movement has
     * occurred.
     *
     * @param startingPosition The fixed position value
     */
    public void getStartingPosition(double[] startingPosition){

        startingPosition[0] = fixedPosition[0];
        startingPosition[1] = fixedPosition[1];
        startingPosition[2] = fixedPosition[2];
    }

    /**
     * Get the original rotation value before current changes were made.
     *
     * @param startingRotation
     */
    public void getStartingRotation(float[] startingRotation){

        startingRotation = null;
    }

    /**
     * Get the original scale value before current changes were made.
     *
     * @param startingScale
     */
    public void getStartingScale(float[] startingScale){

        startingScale = null;
    }

    /**
     * Set the last known good rotation value to new value.
     *
     * @param startingRotation
     */
    public void setStartingRotation(float[] startingRotation){

    }

    /**
     * Set the last known good scale value to new value.
     *
     * @param startingScale
     */
    public void setStartingScale(float[] staringScale){

    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Get the instance of the SegmentEntity
     *
     * @param segmentID ID of the segment entity instance
     * @return Instance of the SegmentEntity that matches
     *         the segment ID.
     */
    public SegmentEntity getSegmentEntity(int segmentID) {
        return segmentMap.get(segmentID);
    }

    /**
     * Get the fixedSize this entity is assigned to
     */
    public boolean isFixedSize() {

        return (Boolean)params.get(FIXED_SIZE_PARAM);

    }

    /**
     * Get the fixedSize this entity is assigned to
     */
    public boolean isHelper() {

        return (Boolean)params.get(HELPER_PARAM);

    }

    /**
     * Get the fixedSize this entity is assigned to
     */
    public boolean isController() {

        return (Boolean)params.get(CONTROLLER_PARAM);

    }

    /**
     * Get the fixedAspect this entity is assigned to
     */
    public boolean isFixedAspect() {

        return (Boolean)params.get(FIXED_ASPECT_PARAM);

    }

    /**
     * Get the color assigned to this entity
     */
    public int[] getColor() {

        return (int[])getProperty(propertySheetName, COLOR_PROP);

    }

    /**
     * Set the color assigned to this entity
     */
    public void setColor(int[] color) {

        setProperty(propertySheetName, COLOR_PROP, color, false);

    }

    /**
     * Remove a property sheet, will do nothing if the sheet doesn't exist
     *
     * @param sheetName The name of the sheet
     */
    public void removePropertySheet(String propSheet) {
        properties.remove(propSheet);
    }

    /**
     * Get the tool used to create vertices
     *
     * @return The VertexTool to use
     */
    public AbstractVertexTool getVertexTool() {
        return vertexTool;
    }

    /**
     * Get the tool used to create segments
     *
     * @return The SegmentTool to use
     */
    public AbstractSegmentTool getSegmentTool() {
        return segmentTool;
    }

    /**
     * Set the parent entity ID
     *
     * @param parentEntityID
     *            Parent entity ID value
     */
    public void setParentEntityID(int parentEntityID) {
        params.put(PARENT_ID_PARAM, parentEntityID);
    }

    /**
     * Get the parent entity ID
     *
     * @return Entity ID of parent
     */
    public int getParentEntityID() {
        return (Integer)params.get(PARENT_ID_PARAM);
    }

    public VertexEntity getVertexByIndex(int vertexIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getVertexIndex(int vertexID) {
        // TODO Auto-generated method stub
        return -1;
    }

    public String getParamSheetName() {
        return propertySheetName;
    }

    /**
     * Get the first segment in the list
     *
     * @return The the first segment ID
     */
    public int getFirstSegmentID() {

        // get the last vertex ID since the vertices
        // are the only guaranteed ordered set
        int firstVertexID = getStartVertexID();

        int len = children.size();
        if (len <= 0)
            return -1;

        for (int i = len - 1; i >= 0; i--) {
            Entity check = children.get(i);
            if (check instanceof SegmentEntity &&
                ((SegmentEntity)check).getStartVertexEntity().getEntityID() == firstVertexID) {

                return check.getEntityID();
            }
        }

        return -1;

    }

    /**
     *  Checks to see if a vertex entity has multiple segments connected to it
     * @param entity the vertex  entity to check
     * @return Returns the numbers of segments connected to the vertex
     */

    public int getSegmentCount(VertexEntity entity){
        if(segmentCount.containsKey(entity))
            return segmentCount.get(entity);
        else
            return 0;
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
        // ignored
        return null;
    }

    /**
     * Set all positionable information.  Replaces all positionable values.
     *
     * @param data The positionable info
     */
    public void setPositionableData(PositionableData data) {
        //  ignored
    }

    public void setIconURL(String view, String url) {
        // TODO Auto-generated method stub

    }


}
