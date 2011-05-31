/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.tool;

// External Imports
import java.util.HashMap;
import java.util.Map;


// Local imports
import org.chefx3d.model.MultiplicityConstraint;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;

/**
 * Describes a tool.
 *
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
public class SimpleTool
    implements Tool, ToolGroupChild, Comparable<SimpleTool> {

    /** The properties of the tool, sheet -> Map (name -> value) */
    protected Map<String, Map<String, Object>> properties;
 
    /** The user-editable tool properties */
    protected HashMap<String, Object> entityProperties;
    
    /** The non-editable tool parameters */
    protected HashMap<String, Object> entityParams;
  
    /** The user-editable tool user properties */
    protected HashMap<String, Object>  userEditableProperties;
    
    /** The property validation classes */
    protected HashMap<String, Object> propertyValidators;

    /** The hidden associate properties */
    protected HashMap<String, Object> associateProperties;
    
    /** The parent of this tool */
    private ToolGroupChild toolParent;
    
    /**
     * Constructor used by EntityGroupTool, BuildingTool, and SegmentTool,
     * which don't require or use the defaultView - leaving this constructor
     * in place minimizes code changes as opposed to changing all those various
     * class' Tool constructor calls. 
     */
    public SimpleTool(
            String id,
            String name,
            String defaultIcon, 
            String[] interfaceIcons, 
            int toolType, 
            String url, 
            String description, 
            float[] size, 
            float[] scale, 
            MultiplicityConstraint constraint, 
            String category,            
            boolean isFixedAspect,
            boolean isFixedSize, 
            boolean isHelper, 
            boolean isController, 
            HashMap<String, Object> defaultProperties) {
    	    	
    	this(id, name, "defaultViewNotUsed", defaultIcon, interfaceIcons, toolType,
    		url, description, size, scale, constraint, category, isFixedAspect, 
    		isFixedSize, isHelper, isController, defaultProperties);
    }
    
    /**
     * Constructor used by EntityGroupTool, BuildingTool, and SegmentTool,
     * which don't require or use the defaultView - leaving this constructor
     * in place minimizes code changes as opposed to changing all those various
     * class' Tool constructor calls. 
     */
    public SimpleTool(
            String id,
            String name,
            String defaultIcon,
            String[] interfaceIcons, 
            int toolType, 
            String url, 
            String description, 
            float[] size, 
            float[] scale, 
            MultiplicityConstraint constraint, 
            String category,            
            boolean isFixedAspect,
            boolean isFixedSize, 
            boolean isHelper, 
            boolean isController, 
            Map<String, Map<String, Object>> defaultProperties) {
    	
    	this(id, name, "defaultViewNotUsed", defaultIcon, interfaceIcons, toolType,
        	url, description, size, scale, constraint, category, isFixedAspect, 
        	isFixedSize, isHelper, isController, defaultProperties);
    }

    /**
     * Create a new tool for adding item to the scene.  A tool
     * created with this constructor begins with no properties.
     */
    public SimpleTool(HashMap<String, Object> defaultProperties) {
        
        if (defaultProperties == null) {
            entityProperties = new HashMap<String, Object>();
        } else {
            entityProperties = defaultProperties;
        }
        entityParams = new HashMap<String, Object>();
        associateProperties = new HashMap<String, Object>();
        propertyValidators = new HashMap<String, Object>();
        
        // setup the base properties
        properties = new HashMap<String, Map<String, Object>>();        
        properties.put(Entity.DEFAULT_ENTITY_PROPERTIES, entityProperties);
        properties.put(Entity.ENTITY_PARAMS, entityParams);
        properties.put(Entity.ASSOCIATED_ENTITIES, associateProperties);
        properties.put(Entity.PROPERTY_VALIDATORS, propertyValidators);
        
    }
    
    /**
     * Create a new tool for adding an item to the scene. The properties 
     * contain on a single default sheet.
     * 
     * @param name - The name of the tool
     * @param defaultView - The initial viewing frustum of the tool, ie: 
     * GT2DView.Plane.TOP.toString(). Ideally this should BE a GT2DView.Plane,
     * but we do not have access to that class due to build order issues.
     * @param defaultIcon - The icon to use in the editor panel
     * @param interfaceIcons - The icons to use in the catalog toolbox
     * @param toolType - The type of tool
     * @param url - The URL
     * @param description - The description of the tool
     * @param size - The starting size
     * @param scale - The starting scale
     * @param constraint - The MultiplicityConstraint for validation
     * @param category - The category, used for assigning a renderer
     * @param isFixedAspect - Fixed aspect ratio
     * @param isFixedSize - Fixed size (scaling)
     * @param isHelper - Is it a helper tool (renderer order)
     * @param isController - Is it a controller
     * @param defaultProperties - The properties
     */
    public SimpleTool(
            String id,
            String name,
            String defaultView,
            String defaultIcon, 
            String[] interfaceIcons, 
            int toolType, 
            String url, 
            String description, 
            float[] size, 
            float[] scale, 
            MultiplicityConstraint constraint, 
            String category,            
            boolean isFixedAspect,
            boolean isFixedSize, 
            boolean isHelper, 
            boolean isController, 
            HashMap<String, Object> defaultProperties) {

        this(defaultProperties);       
        
        // add the base properties to whatever was passed in
        entityProperties.put(Entity.NAME_PROP, name);
        entityProperties.put(Entity.DESCRIPTION_PROP, description);
                       
        if (isFixedSize) {
            entityParams.put(PositionableEntity.SCALE_PROP, scale);     
        } else {
            entityProperties.put(PositionableEntity.SCALE_PROP, scale);     
        }
        
        // add params
        entityParams.put(Entity.TYPE_PARAM, toolType);
        entityParams.put(PositionableEntity.SIZE_PARAM, size);
        entityParams.put(Entity.CATEGORY_PARAM, category);
        entityParams.put(Entity.MODEL_URL_PARAM, url);
        entityParams.put(Entity.INTERFACE_ICON_PARAM, interfaceIcons);
        entityParams.put(Entity.FIXED_ASPECT_PARAM, isFixedAspect);
        entityParams.put(Entity.FIXED_SIZE_PARAM, isFixedSize);
        entityParams.put(Entity.HELPER_PARAM, isHelper);
        entityParams.put(Entity.CONTROLLER_PARAM, isController);
     
        // EMF: note how the defaultIcon gets added to two locations:
        // the first sets the 'current' icon for this tool, but in
        // the event that the current icon for the tool *changes*,
        // it is also saved off and paired with "defaultView"
        entityParams.put(Entity.ICON_URL_PARAM, defaultIcon);
        entityParams.put(defaultView, defaultIcon);
        
        if (constraint == null) {
            entityParams.put(Entity.CONSTRAINT_PARAM, MultiplicityConstraint.NO_REQUIREMENT);   
        } else {
            entityParams.put(Entity.CONSTRAINT_PARAM, constraint);   
        }
                
    }

    /**
     * Create a new tool for adding an item to the scene. The properties 
     * contain multiple sheets.
     * 
     * @param name - The name of the tool
     * @param defaultView - The initial viewing frustum of the tool, ie: 
     * GT2DView.Plane.TOP.toString(). Ideally this should BE a GT2DView.Plane,
     * but we do not have access to that class due to build order issues.
     * @param defaultIcon - The icon to use in the editor panel
     * @param interfaceIcons - The icons to use in the catalog toolbox
     * @param toolType - The type of tool
     * @param url - The URL
     * @param description - The description of the tool
     * @param size - The starting size
     * @param scale - The starting scale
     * @param constraint - The MultiplicityConstraint for validation
     * @param category - The category, used for assigning a renderer
     * @param isFixedAspect - Fixed aspect ratio
     * @param isFixedSize - Fixed size (scaling)
     * @param isHelper - Is it a helper tool (renderer order)
     * @param isController - Is it a controller
     * @param defaultProperties - The properties
     */
    public SimpleTool(
            String id,
            String name,
            String defaultView,
            String defaultIcon,
            String[] interfaceIcons, 
            int toolType, 
            String url, 
            String description, 
            float[] size, 
            float[] scale, 
            MultiplicityConstraint constraint, 
            String category,            
            boolean isFixedAspect,
            boolean isFixedSize, 
            boolean isHelper, 
            boolean isController, 
            Map<String, Map<String, Object>> defaultProperties) {

        if (defaultProperties.get(Entity.DEFAULT_ENTITY_PROPERTIES) == null) {
            entityProperties = new HashMap<String, Object>();
        } else {
            entityProperties = 
                (HashMap<String, Object>)defaultProperties.get(Entity.DEFAULT_ENTITY_PROPERTIES);
        }
        if (defaultProperties.get(Entity.PROPERTY_VALIDATORS) == null) {
            propertyValidators = new HashMap<String, Object>();
        } else {
            propertyValidators = 
                (HashMap<String, Object>)defaultProperties.get(Entity.PROPERTY_VALIDATORS);
        }
       
        entityParams = new HashMap<String, Object>();
        associateProperties = new HashMap<String, Object>();
               
        // setup the base properties
        properties = defaultProperties;        
        properties.put(Entity.DEFAULT_ENTITY_PROPERTIES, entityProperties);
        properties.put(Entity.ENTITY_PARAMS, entityParams);
        properties.put(Entity.ASSOCIATED_ENTITIES, associateProperties);
        properties.put(Entity.PROPERTY_VALIDATORS, propertyValidators);
	
        // add the base properties to whatever was passed in

        entityProperties.put(Entity.TOOL_ID_PROP, id);
        entityProperties.put(Entity.NAME_PROP, name);
        entityProperties.put(Entity.DESCRIPTION_PROP, description);
        
        if (isFixedSize) {
            entityParams.put(PositionableEntity.SCALE_PROP, scale);     
        } else {
            entityProperties.put(PositionableEntity.SCALE_PROP, scale);     
        }
       
        // add params
        entityParams.put(Entity.TYPE_PARAM, toolType);
        entityParams.put(PositionableEntity.SIZE_PARAM, size);
        entityParams.put(Entity.CATEGORY_PARAM, category);
        entityParams.put(Entity.MODEL_URL_PARAM, url);
        entityParams.put(Entity.INTERFACE_ICON_PARAM, interfaceIcons);
        entityParams.put(Entity.FIXED_ASPECT_PARAM, isFixedAspect);
        entityParams.put(Entity.FIXED_SIZE_PARAM, isFixedSize);
        entityParams.put(Entity.HELPER_PARAM, isHelper);
        entityParams.put(Entity.CONTROLLER_PARAM, isController);
        
        // EMF: note how the defaultIcon gets added to two locations:
        // the first sets the 'current' icon for this tool, but in
        // the event that the current icon for the tool *changes*,
        // it is also saved off and paired with "defaultView"
        entityParams.put(Entity.ICON_URL_PARAM, defaultIcon);
        entityParams.put(defaultView, defaultIcon);
        
        if (constraint == null) {
            entityParams.put(Entity.CONSTRAINT_PARAM, MultiplicityConstraint.NO_REQUIREMENT);   
        } else {
            entityParams.put(Entity.CONSTRAINT_PARAM, constraint);   
        }
                
    }
    
    /**
     * Create a new tool for adding an item to the scene. The properties 
     * contain multiple sheets.
     * 
     * @param name - The name of the tool
     * @param defaultView - The initial viewing frustum of the tool, ie: 
     * GT2DView.Plane.TOP.toString(). Ideally this should BE a GT2DView.Plane,
     * but we do not have access to that class due to build order issues.
     * @param defaultIcon - The icon to use in the editor panel
     * @param interfaceIcons - The icons to use in the catalog toolbox
     * @param toolType - The type of tool
     * @param url - The URL
     * @param description - The description of the tool
     * @param size - The starting size
     * @param scale - The starting scale
     * @param constraint - The MultiplicityConstraint for validation
     * @param category - The category, used for assigning a renderer
     * @param isFixedAspect - Fixed aspect ratio
     * @param isFixedSize - Fixed size (scaling)
     * @param isHelper - Is it a helper tool (renderer order)
     * @param isController - Is it a controller
     * @param defaultProperties - The properties
     */
    public SimpleTool(
            String id,
            String name,
            String defaultView,
            String defaultIcon,
            String[] interfaceIcons, 
            int toolType, 
            String url, 
            String description, 
            float[] size, 
            float[] scale, 
            MultiplicityConstraint constraint, 
            String category,            
            boolean isFixedAspect,
            boolean isFixedSize, 
            boolean isHelper, 
            boolean isController, 
            String editablePropertySheetName,
            Map<String, Map<String, Object>> defaultProperties) {
    	
    	this(id, name, "defaultViewNotUsed", defaultIcon, interfaceIcons, toolType,
           	url, description, size, scale, constraint, category, isFixedAspect, 
           	isFixedSize, isHelper, isController, defaultProperties);
    	
    	
    	 if (defaultProperties.get(editablePropertySheetName) == null) {
         	userEditableProperties = new HashMap<String, Object>();            
         } else {
         	userEditableProperties = 
                 (HashMap<String, Object>)defaultProperties.get(editablePropertySheetName);
         }
    	 properties.put(Entity.EDITABLE_PROPERTIES, userEditableProperties);
    }
   
    //----------------------------------------------------------
    // Methods defined by Tool
    //----------------------------------------------------------
    /**
     * Get the tool's name.
     *
     * @return The name
     */
    public String getName() {
        return (String)entityProperties.get(Entity.NAME_PROP);
    }

    /**
     * Get the tool type. Defined in this class as TYPE_*
     *
     * @return The type
     */
    public int getToolType() {
        return (Integer)entityParams.get(Entity.TYPE_PARAM);
    }
    
    /**
     * Get the unique ID of the tool
     * 
     * @return The ID
     */
    public String getToolID() {
        String id = (String)entityProperties.get(Entity.TOOL_ID_PROP);
        if (id == null) {
            id = getName();
        }
        return id;
    }

    /**
     * Get the string describing the tool.
     *
     * @return The description
     */
    public String getDescription() {
        return (String)entityProperties.get(Entity.DESCRIPTION_PROP);
    }

    /**
     * Get the URL's to use for this tool.
     *
     * @return The list of urls
     */
    public String getURL() {
        return (String)entityParams.get(Entity.MODEL_URL_PARAM);
    }

    /**
     * Get the top down icon for this tool
     *
     * @return The icon
     */
    public String getIcon() {
        return (String)entityParams.get(Entity.ICON_URL_PARAM);
    }

    //----------------------------------------------------------
    // Methods defined by Object
    //----------------------------------------------------------

    /**
     * Calculate the hashcode for this object.
     *
     */
    public int hashCode() {
        // TODO: Not a very good hash
        return getToolID().hashCode();
    }

    /**
     * override of Objects equals test
     */
    public boolean equals(Object o) {

        if (o instanceof SimpleTool) {
            SimpleTool check = (SimpleTool)o;
            
            String a = getToolID();
            String b = check.getToolID();
            
            if (a.equals(b)) {
                return true;
            }
        }
        return false;
    }

    //----------------------------------------------------------
    // Methods defined by Comparable<Tool>
    //----------------------------------------------------------

    /**
     * Return compare based on string ordering
     */
    public int compareTo(SimpleTool t) {
                    
        String id = getToolID();
        String checkID = t.getToolID();
        
        return id.compareTo(checkID);

    }

    //----------------------------------------------------------
    // Methods defined by ToolGroupChild
    //----------------------------------------------------------

    /**
     * Return the parent of this tool group child. If there is no parent
     * reference the parent is either the catalog or this is an orphaned item.
     *
     * @return The current parent of this item
     */
    public ToolGroupChild getParent() {
        return toolParent;
    }

    /**
     * Set the tool parent to be this new object. Null clears the reference.
     * Package private because only ToolGroup should be calling this.
     * 
     * @param parent - The parent ToolGroupChild
     */
    public void setParent(ToolGroupChild parent) {
        toolParent = parent;
    }
    
    /**
     * Get a specific property.
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @return propValue
     */
    public Object getProperty(
            String propSheet, 
            String propName) {
   
        Map<String, Object> sheetProperties = properties.get(propSheet);

        if (sheetProperties == null) {
            return null;
        }
        
        return sheetProperties.get(propName);
        
    }

    
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Set an icon to use for a particular view 
     * @param view The viewing angle of this icon (ie: top-down, side, front)
     * @param icon The icon to be associated with the view
     * @author Eric Fickenscher
     */
    public void setIcon(String view, String icon){
    	entityParams.put(view, icon);
    }
    
    /**
     * This method updates the current icon of the tool so that it 
     * is appropriate to the new view of the Tool.  IE: If the newView
     * is GT2DView.Plane.TOP, then the tool's icon should be a top-down 
     * icon.  Ideally, this would take a more 'controlled' class as a 
     * parameter than a String (GT2DView.Plane, for instance), but build
     * orders being what they are, this must suffice for now.
     * @param currentView The current view of the tool, ie: 
     * GT2DView.Plane.TOP.toString()
     * @author Eric Fickenscher
     */
    public void setCurrentView(String currentView){
    	String currentIcon = (String)entityParams.get(currentView);
    	if(currentIcon != null)
    		entityParams.put(Entity.ICON_URL_PARAM, currentIcon);
    }

    /**
     * Is the aspect ratio of the icon fixed.
     *
     * @return TRUE if its fixed.
     */
    public boolean isFixedAspect() {
        return (Boolean)entityParams.get(Entity.FIXED_ASPECT_PARAM);
    }

    /**
     * Get the isHelper flag
     *
     * @return - The helper property
     */
    public boolean isHelper() {
        return (Boolean)entityParams.get(Entity.HELPER_PARAM);
    }

    /**
     * Get the isController flag
     *
     * @return - The controller property
     */
    public boolean isController() {
        return (Boolean)entityParams.get(Entity.CONTROLLER_PARAM);
    }

    /**
     * Get the isFixedSize flag
     *
     * @return - The fixed size property
     */
    public boolean isFixedSize() {
        return (Boolean)entityParams.get(Entity.FIXED_SIZE_PARAM);
    }

    /**
     * Get the interfaceIcons. 
     *
     * @return The icons
     */
    public String[] getInterfaceIcons() {
        return (String[])entityParams.get(Entity.INTERFACE_ICON_PARAM);
    }

    /**
     * Get the default size of this tool.
     *
     * @return The size
     */
    public float[] getSize() {
        return (float[])entityParams.get(PositionableEntity.SIZE_PARAM);
    }

    /**
     * Get the default scale of this tool.
     *
     * @return The scale
     */
    public float[] getScale() {
        
        if (isFixedSize()) {
            return (float[])entityParams.get(PositionableEntity.SCALE_PROP);
        } else {
            return (float[])entityProperties.get(PositionableEntity.SCALE_PROP);    
        }
        
    }

    /**
     * Get the multiplicity constraint.
     *
     * @return The constraint
     */
     public MultiplicityConstraint getConstraint() {
        return (MultiplicityConstraint)entityParams.get(Entity.CONSTRAINT_PARAM);
     }

    /**
     * Get the category of this tool.  Used for constraint checking.
     *
     * @return The category
     */
     public String getCategory() {
        return (String)entityParams.get(Entity.CATEGORY_PARAM);
     }

     /**
      * Set a specific property.
      *
      * @param propSheet The sheet name
      * @param propName The name of the property to set
      * @param propValue The value to set
      */
     public void setProperty(
             String propSheet, 
             String propName,
             Object propValue) {
  
         Map<String, Object> sheetProperties = properties.get(propSheet);

         if (sheetProperties != null) {
             sheetProperties.put(propName, propValue);
         }

     }
    
    /**
     * Get all the properties of the tool
     * 
     * @return A map of properties (sheet -> map [name -> value])
     */
    public Map<String, Map<String, Object>> getProperties() {
        return properties;
    }
   
    /**
     * A toString method to pretty print.
     * 
     * @return The string representation of the tool
     */
    public String toString() {
        return "Tool: hc: " + hashCode() + " id: " + getToolID() + " name: " + getName();
    }

}
