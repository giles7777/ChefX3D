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

// External imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.PropertyUtilities;

/**
 * An base entity implementation that handles having children
 *
 * @author Russell Dodds
 * @version $Revision: 1.51 $
 */
public abstract class BaseEntity implements Entity, Cloneable {

    /** The property sheet name */
    protected String propertySheetName;

    /** The children list */
    protected ArrayList<Entity> children;

    /** The entityID */
    protected int entityID;

    /** The properties of the entity, sheet -> Map (name -> value) */
    protected Map<String, Map<String, Object>> properties;

    /** The properties of the entity, sheet -> Map (name -> value) */
    protected Map<String, Object> params;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;

    /** The groupListener(s) for group changes at this level */
    //protected EntityPropertyListener entityPropertyListener;

    /** Handler of EntityPropertyListeners */
    protected EntityPropertyListenerHandler propertyListenerHandler;

    /** Handler of EntityChildListeners */
    protected EntityChildListenerHandler childListenerHandler;

    /** Handler of EntitySelectionListeners */
    protected EntitySelectionListenerHandler selectionListenerHandler;

    protected boolean updateChildren;

    /** The starting children list */
    protected ArrayList<Entity> startingChildren;

    /**
     * The most basic constructor, which uses the entity default param sheet
     * name.  This assumes that the values required to be in this sheet are
     * present.
     *
     * @param entityID
     * @param toolProperties
     */
    public BaseEntity(int entityID, Map<String, Map<String, Object>> toolProperties) {
        this(entityID, DEFAULT_ENTITY_PROPERTIES, toolProperties);
    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The name of the sheet that contains the properties
     * @param toolProperties The properties of an entity as defined by the tool
     */
    public BaseEntity(
            int entityID,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties) {

        this.propertySheetName = propertySheetName;
        this.entityID = entityID;

        children = new ArrayList<Entity>();
        startingChildren = new ArrayList<Entity>();

        propertyListenerHandler = new EntityPropertyListenerHandler();
        childListenerHandler = new EntityChildListenerHandler();
        selectionListenerHandler = new EntitySelectionListenerHandler();

        updateChildren = false;

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        // set the properties
        properties = new HashMap<String, Map<String, Object>>();
        params = new HashMap<String, Object>();

        if (toolProperties == null)
            return;

        // clone the tool parameters
        params = PropertyUtilities.clone(toolProperties.get(Entity.ENTITY_PARAMS));

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

        if (params != null && params.get(Entity.SELECTED_PARAM) == null) {
            params.put(Entity.SELECTED_PARAM, false);
        }

    }

    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The name of the sheet that contains the properties
     * @param toolProperties The properties of an entity as defined by the tool
     * @param toolParams The params of an entity
     */
    protected BaseEntity(
            int entityID,
            String propertySheetName,
            Map<String, Object> toolParams,
            Map<String, Map<String, Object>> toolProperties) {

        this(entityID, propertySheetName, toolProperties);

        params = PropertyUtilities.clone(toolParams);
        params.put(Entity.SELECTED_PARAM, false);

    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public abstract BaseEntity clone(IdIssuer issuer);

    /**
     * Compare the given details to this one to see if they are equal. Equality
     * is defined as pointing to the same clipPlane source, with the same
     * transformation value.
     *
     * @param o The object to compare against
     * @return true if these represent identical objects
     */
    public boolean equals(Object o) {
        // Guaranteed a false return unless the object incoming
        // provides the same entityID, since MAX_VALUE is a no-no
        int id = entityID + 1;

        if (o instanceof Integer) {
            id = ((Integer)o).intValue();
        }

        if (o instanceof Entity) {
            id = ((Entity)o).getEntityID();
        }

        return (id == entityID);
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

    /**
     * Print a String representation of the Entity
     */
    public String toString() {
        return
            this.getClass().toString() +
            " [entityID=" + entityID +
            ", name=" + getName() + "]";
    }

    // ---------------------------------------------------------------
    // Methods defined by Entity
    // ---------------------------------------------------------------

    //
    // children methods
    //

    /**
     * Add a listener for entity children changes
     *
     * @param ecl
     */
    public void addEntityChildListener(EntityChildListener ecl) {
        childListenerHandler.add(ecl);
    }

    /**
     * Remove a listener of entity children changes
     *
     * @param ecl
     */
    public void removeEntityChildListener(EntityChildListener ecl) {
        childListenerHandler.remove(ecl);
    }

    /**
     * Add a child to the entity
     *
     * @param entity - The entity being added
     */
    public void addChild(Entity entity) {

        // TODO: need to comment this out to support paste of nested children
        //if (!children.contains(entity)) {

            // add the entityID to the internal data structure
            children.add(entity);
            entity.setParentEntityID(entityID);
            setInitialState(entity);

            childListenerHandler.childAdded(entityID, entity.getEntityID());
        //}

    }

    /**
     * Get an Entity at the index, returns null if not found
     *
     * @param index The index
     * @return The entityID
     */
    public Entity getChildAt(int index) {
        return children.get(index);
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
     * Get the index of the entity
     *
     * @param entityID - The entity to lookup
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
     * Get a copy of list (shallow clone) of all children of this Entity
     *
     * @return The list of children entities
     */
    public ArrayList<Entity> getChildren() {
        // return a cloned copy of the list so that I cannot be altered by
        // whatever code requested the list.  The method should really take an
        // List as a parameter and fill it in with the current values that way
        // we are not cloning the data repetitively

        return (ArrayList<Entity>)children.clone();
    }

    /**
     * Get a list of all childrenIDs of this Entity
     *
     * @return The list of childrenIDs
     */
    public int[] getChildrenIDs() {
        int[] childrenIds = new int[children.size()];
        for (int i = 0; i < childrenIds.length; i++) {
            childrenIds[i] = children.get(i).getEntityID();
        }
        return childrenIds;
    }

    /**
     * Does this Entity have any children
     *
     * @return true if it has children, false otherwise
     */
    public boolean hasChildren() {
        return (children.size() > 0);
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

            entity.setParentEntityID(entityID);
            setInitialState(entity);

            childListenerHandler.childInsertedAt(this.getEntityID(), entity.getEntityID(), actualIndex);
        }
    }

    /**
     * Remove a child from the entity
     *
     * @param entity - The entity being removed
     */
    public void removeChild(Entity entity) {

        if (children.contains(entity)) {

            // remove the entityID from the internal data structure
            setInitialState(entity);
            children.remove(entity);

            childListenerHandler.childRemoved(entityID, entity.getEntityID());

            entity.setParentEntityID(-1);
        }
    }

    /**
     * Get the flag indicating if updates should
     * be applied to the children
     *
     * @return true/false
     */
    public boolean getUpdateChildren() {
        return updateChildren;
    }

    /**
     * Set the flag indicating if updates should
     * be applied to the children
     *
     * @param bool
     */
    public void setUpdateChildren(boolean bool) {
        updateChildren = bool;
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

    //
    // helper methods to access params
    //

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
     * Get the entity's category
     *
     * @return The category property
     */
    public String getCategory() {
        String cat = (String)params.get(CATEGORY_PARAM);
        if (cat == null) {
            return "";
        } else {
            return cat;
        }
    }

    /**
     * Get the constraint this entity is assigned to
     *
     * @return The multiplicity constraint property
     */
    public MultiplicityConstraint getConstraint() {
        MultiplicityConstraint contraint =
            (MultiplicityConstraint)params.get(Entity.CONSTRAINT_PARAM);
        if (contraint == null) {
            return MultiplicityConstraint.NO_REQUIREMENT;
        } else {
            return contraint;
        }
    }

    /**
     * The URL to the 2D image that represents this entity
     *
     * @param view A particular view of this entity's icon.
     * @return The icon URL property
     */
    public String getIconURL(String view) {
        String icon = (String)params.get(view);
        if (icon == null)
            return (String)params.get(ICON_URL_PARAM);
        else return icon;
    }

    /**
     *  Sets The URL to the 2D image that represents this entity
     * @param view The current view of this entity's icon
     * @param The url to store for the icon
     */
    public void setIconURL(String view ,String url) {

        if(view != null)
            params.put(view, url);
        else {
            params.put(ICON_URL_PARAM, url);
        }
    }


    /**
     * The URL to the 3D model that represents this entity
     *
     * @return The model URL property
     */
    public String getModelURL() {
        return (String)params.get(Entity.MODEL_URL_PARAM);
    }

    /**
     * Get the entity's type
     *
     * @return The type property
     */
    public abstract int getType();

    /**
     * Check if the type of this entity is one of the zone types
     *
     * @return True if one of the zone types, false otherwise
     */
    public abstract boolean isZone();

    /**
     * Is this entity a controller
     *
     * @return The controller property
     */
    public boolean isController() {
        Boolean bool = (Boolean)params.get(CONTROLLER_PARAM);
        if (bool == null) {
            return false;
        } else {
            return bool;
        }
    }

    /**
     * Does this entity's icon have a fixed aspect ratio
     *
     * @return The fixedAspect property
     */
    public boolean isFixedAspect() {
        Boolean bool = (Boolean)params.get(FIXED_ASPECT_PARAM);
        if (bool == null) {
            return false;
        } else {
            return bool;
        }
    }

    /**
     * Is this entity a fixed size
     *
     * @return The fixedSize property
     */
    public boolean isFixedSize() {
        if (params == null)
            return false;

        Object bool = params.get(FIXED_SIZE_PARAM);
        if (bool == null) {
            return false;
        } else {
            return (Boolean)bool;
        }
    }

    /**
     * Is this entity a helper
     *
     * @return The helper property
     */
    public boolean isHelper() {
        Boolean bool = (Boolean)params.get(HELPER_PARAM);
        if (bool == null) {
            return false;
        } else {
            return bool;
        }
    }

    //
    // property methods
    //

    /**
     * Add a listener for entity property changes
     *
     * @param epl
     */
    public void addEntityPropertyListener(EntityPropertyListener epl) {
        //entityPropertyListener =
        //    EntityPropertyListenerMulticaster.add(entityPropertyListener, epl);
        propertyListenerHandler.add(epl);
    }

    /**
     * Remove a listener of entity property changes
     *
     * @param epl
     */
    public void removeEntityPropertyListener(EntityPropertyListener epl) {
        //entityPropertyListener =
        //    EntityPropertyListenerMulticaster.remove(entityPropertyListener, epl);
        propertyListenerHandler.remove(epl);
    }

    /**
     * DO NOT USE - Use Commands
     * Add the specified property to the document.
     *
     * @param propSheet The sheet name
     * @param propName The property name
     * @param propValue The property value
     */
    public void addProperty(String propSheet, String propName, Object propValue) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties != null) {
            sheetProperties.put(propName, propValue);

            //if (entityPropertyListener != null)
            //    entityPropertyListener.propertyAdded(entityID, propSheet, propName);
            propertyListenerHandler.propertyAdded(entityID, propSheet, propName);

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

            //if (entityPropertyListener != null)
            //    entityPropertyListener.propertyRemoved(entityID, propSheet, propName);
            propertyListenerHandler.propertyRemoved(entityID, propSheet, propName);
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
            Object propValue,
            boolean ongoing) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties != null) {

            sheetProperties.put(propName, propValue);

            //if (entityPropertyListener != null) {
            //    entityPropertyListener.propertyUpdated(entityID, propSheet, propName, ongoing);
            //}
            propertyListenerHandler.propertyUpdated(entityID, propSheet, propName, ongoing);
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
        //if (entityPropertyListener != null)
        //    entityPropertyListener.propertiesUpdated(properties);
        propertyListenerHandler.propertiesUpdated(properties);
    }

    /**
     * Get a specific property.
     *
     * @param propSheet The grouping name
     * @param propName The name of the property to set
     * @return The property value
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
     * @return The properties
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
     * Get all the properties for a sheet.  If the property sheet does not
     * exist then this will return null.
     *
     * @param propSheet The sheet name
     * @return The properties
     */
    public Map<String, Object> getPropertySheet(String propSheet) {

        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties == null) {
            return null;
        }

        return sheetProperties;

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
     * @return The list of sheets defined
     */
    public List<String> getPropertySheets() {

        ArrayList<String> sheets = new ArrayList<String>();

        Object[] keys = properties.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            sheets.add((String)keys[i]);
        }

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
     * Remove a property sheet, will do nothing if the sheet doesn't exist
     *
     * @param propSheet The name of the sheet
     */
    public void removePropertySheet(String propSheet) {
        properties.remove(propSheet);
    }

    //
    // helper methods to access properties
    //

    /**
     * Get the name of the entity
     *
     * @return The name property
     */
    public String getToolID() {
        String toolID = (String)getProperty(propertySheetName, TOOL_ID_PROP);
        if (toolID == null) {
            return getName();
        } else {
            return toolID;
        }
    }

    /**
     * Get the name of the entity
     *
     * @return The name property
     */
    public String getName() {
        String name = (String)getProperty(propertySheetName, NAME_PROP);
        if (name == null) {
            return Integer.toString(entityID);
        } else {
            return name;
        }
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
     * Set the ID of the kit this entity is a part of
     *
     * @param kitEntityID The new kitID
     */
    public void setKitEntityID(int kitEntityID) {
        setProperty(propertySheetName, KIT_ENTITY_ID_PROP, kitEntityID, false);
    }

    /**
     * Get the description of this entity
     *
     * @return The description property
     */
    public String getDescription() {
        String desc = (String)getProperty(propertySheetName, DESCRIPTION_PROP);
        if (desc == null) {
            return getName();
        } else {
            return desc;
        }
    }

    /**
     * Set the description of this entity
     *
     * @param desc
     */
    public void setDescription(String desc) {
        setProperty(propertySheetName, DESCRIPTION_PROP, desc, false);
    }

    //
    // selection/highlight methods
    //

    /**
     * Add a listener for entity selection changes
     *
     * @param esl
     */
    public void addEntitySelectionListener(EntitySelectionListener esl) {
        selectionListenerHandler.add(esl);
    }

    /**
     * Remove a listener of entity selection changes
     *
     * @param esl
     */
    public void removeEntitySelectionListener(EntitySelectionListener esl) {
        selectionListenerHandler.remove(esl);
    }

    /**
     * Set whether this entity is selected.
     *
     * @param selected Whether to selected this entity
     */
    public void setSelected(boolean selected) {

        Boolean current = (Boolean)params.get(SELECTED_PARAM);

        if (current == null || current != selected) {
            params.put(SELECTED_PARAM, selected);

            selectionListenerHandler.selectionChanged(entityID, selected);
        }
    }

    /**
     * Get whether this entity is selected.
     *
     * @return Whether this entity is selected
     */
    public boolean isSelected() {
        Boolean selected = (Boolean)params.get(SELECTED_PARAM);
        if (selected == null) {
            return(false);
        } else {
            return(selected);
        }
    }

    /**
     * Set whether to highlight this entity.
     *
     * @param highlight Whether to highlight this entity
     */
    public void setHighlighted(boolean highlight) {
        params.put(HIGHLIGHTED_PARAM, highlight);

        selectionListenerHandler.highlightChanged(entityID, highlight);
    }

    /**
     * Get whether to highlight this entity.
     *
     * @return Whether to highlight this entity
     */
    public boolean isHighlighted() {
        Boolean highlighted = (Boolean)params.get(HIGHLIGHTED_PARAM);
        if (highlighted == null) {
            return(false);
        } else {
            return(highlighted);
        }
    }

    //
    // utility methods
    //

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        this.errorReporter = reporter;
    }

    /**
     * Gather all the params for the entity at once.
     *
     * @return A list containing all the params
     */
    public List<EntityProperty> getParams() {

        ArrayList<EntityProperty> propertyList = null;

        if ((params != null) && !params.isEmpty()) {

            propertyList = new ArrayList<EntityProperty>();

            Iterator<Map.Entry<String, Object>> index =
                params.entrySet().iterator();

            EntityProperty entityProperties;
            while (index.hasNext()) {

                Map.Entry<String, Object> mapEntry = index.next();

                // get the key, value pairing
                String propName = mapEntry.getKey();
                Object propValue = mapEntry.getValue();

                entityProperties = new EntityProperty(
                    entityID,
                    Entity.ENTITY_PARAMS,
                    propName,
                    propValue);

                propertyList.add(entityProperties);

            }
        }
        return propertyList;
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
        if(params.containsKey(PARENT_ID_PARAM))
            return (Integer)params.get(PARENT_ID_PARAM);
        else
            return -1;
    }

    /**
     * Get the name of the param sheet, set
     * when the entity is created.
     *
     * @return The name of the param sheet
     */
    public String getParamSheetName() {
        return propertySheetName;
    }

    /**
     * Walk through hierarchy of the argument entity,
     * initializing as necessary.
     *
     * @param entity The entity to start with
     */
    private void setInitialState(Entity entity) {

        if (entity.isSelected()) {
            entity.setSelected(false);
        }

        if (entity.hasChildren()) {
            int parentID = entity.getEntityID();
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                if (child == entity) {
                    // rem: the child is it's own parent??? how does that happen?
                    continue;
                }
                child.setParentEntityID(parentID);
                setInitialState(child);
            }
        }
    }
}
