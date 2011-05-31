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

// Standard Imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.chefx3d.model.MultiplicityConstraint;

// Application specific imports


/**
 * A entity grouping tool.
 *
 * @author Russell Dodds
 * @version $Revision: 1.9 $
 */
 public class EntityGroupTool extends SimpleTool {

     /** list of children Tools */
     private ArrayList<SimpleTool> children;

     /**
      * Create a new tool for adding a grouped items to the scene.  This
      * and all its children will be added to the scene. The properties 
      * contain on a single default sheet.
      *  
      * @param name - The unique name of the tool
      * @param topDownIcon - The 2D icon used when placing the item
      * @param interfaceIcons - The list of icons to use as GUI (Swing) images)
      * @param toolType - The type of tool
      * @param url - The model URL (most of the time this is an X3D file)
      * @param description - The description of the model
      * @param size - The starting size
      * @param scale - The starting scale
      * @param constraint - The MultiplicityConstraint for validation
      * @param category - The category, used for assigning a renderer
      * @param isFixedAspect - Fixed aspect ratio
      * @param isFixedSize - Fixed size (scaling)
      * @param isHelper - Is it a helper tool (renderer order)
      * @param isController - Is it a controller
      * @param entityProperties - The properties
      */
     public EntityGroupTool(
             String id, 
             String name, 
             String topDownIcon, 
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
             HashMap<String, Object> entityProperties) {

         // setup the base properties
         super(id, name, topDownIcon, interfaceIcons, toolType, url, description, size, scale, 
                 constraint, category, isFixedAspect, isFixedSize, isHelper, isController, entityProperties);
                 
         children = new ArrayList<SimpleTool>();

     }
     
     /**
      * Create a new tool for adding a grouped items to the scene.  This
      * and all its children will be added to the scene. The properties 
      * contain multiple sheets.
      *  
      * @param name - The unique name of the tool
      * @param topDownIcon - The 2D icon used when placing the item
      * @param interfaceIcons - The list of icons to use as GUI (Swing) images)
      * @param toolType - The type of tool
      * @param url - The model URL (most of the time this is an X3D file)
      * @param description - The description of the model
      * @param size - The starting size
      * @param scale - The starting scale
      * @param constraint - The MultiplicityConstraint for validation
      * @param category - The category, used for assigning a renderer
      * @param isFixedAspect - Fixed aspect ratio
      * @param isFixedSize - Fixed size (scaling)
      * @param isHelper - Is it a helper tool (renderer order)
      * @param isController - Is it a controller
      * @param entityProperties - The properties
      */
     public EntityGroupTool(
             String id, 
             String name, 
             String topDownIcon, 
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
             Map<String,Map<String, Object>> entityProperties) {

         // setup the base properties
         super(id, name, topDownIcon, interfaceIcons, toolType, url, description, size, scale, 
                 constraint, category, isFixedAspect, isFixedSize, isHelper, isController, entityProperties);
                 
         children = new ArrayList<SimpleTool>();

    }

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

    /**
     * Add a child to the tool
     *
     * @param tool - The child tool
     */
    public void addChild(SimpleTool tool) {
        children.add(tool);
    }

    /**
     * Remove a child from the tool
     *
     * @param tool - The child tool
     */
    public void removeChild(SimpleTool tool) {
        children.remove(tool);
    }

    /**
     * Get the index of a child Tool
     * 
     * @param tool
     * @return The index
     */
    public int getChildIndex(SimpleTool tool) {
        return children.indexOf(tool);
    }

    /**
     * Get a Tool at the index, returns null if not found
     *
     * @param index The index
     * @return The Tool
     */
    public SimpleTool getChildAt(int index) {
        if (children.size() > index) {
            return children.get(index);
        }
        return null;
    }

    /**
     * Get a list of all children of this Tool
     *
     * @return The list of children
     */
    public ArrayList<SimpleTool> getChildren() {
        return children;
    }

    /**
     * Get the number of children of this Tool
     *
     * @return The number of children
     */
    public int getChildCount() {
       return children.size();
    }
    
    /**
     * Does this Tool have any children
     *
     * @return true if it has children, false otherwise
     */
    public boolean hasChildren() {
        if (children.size() > 0) {
            return true;
        }
       return false;
    }

 }