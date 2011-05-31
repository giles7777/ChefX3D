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
import java.util.Map;

// Application specific imports
import org.chefx3d.model.AbstractSegmentTool;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiplicityConstraint;

/**
 * A segment tool.
 *
 * @author Russell Dodds
 * @version $Revision: 1.15 $
 */
public class SegmentTool extends SimpleTool implements AbstractSegmentTool {
   
    /**
     * 
     * Create a new tool for adding a segment to the scene. The properties 
     * contain multiple sheets.
     * 
     * @param name - The name of the tool
     * @param topDownIcon - The icon to use in the editor panel
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
     * @param defaultSheetName - The sheet to get properties
     * @param defaultProperties - The properties
     */
    public SegmentTool(
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
            String entitySheetName,
            Map<String,Map<String, Object>> defaultProperties) {
        
        
        // setup the base properties
        super(id, name, topDownIcon, interfaceIcons, toolType, url, 
                description, size, scale,  constraint, category, isFixedAspect, 
                isFixedSize, isHelper, isController, defaultProperties);
                
    }    

    /**
     * 
     * Create a new tool for adding a segment to the scene. The properties 
     * contain multiple sheets.
     * 
     * @param name - The name of the tool
     * @param topDownIcon - The icon to use in the editor panel
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
    public SegmentTool(
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
            Map<String,Map<String, Object>> defaultProperties) {
        
        // setup the base properties
        this(id, name, topDownIcon, interfaceIcons, toolType, url, 
                description, size, scale,  constraint, category, isFixedAspect, 
                isFixedSize, isHelper, isController,
                Entity.DEFAULT_ENTITY_PROPERTIES, defaultProperties);
    }
 
}