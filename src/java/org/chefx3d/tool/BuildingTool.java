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
import java.util.HashMap;
import java.util.Map;

// Application specific imports
import org.chefx3d.model.MultiplicityConstraint;
import org.chefx3d.model.SegmentableEntity;

/**
 * A building tool based off of the segment tool concepts
 *
 * @author Russell Dodds
 * @version $Revision: 1.11 $
 */
 public class BuildingTool extends SimpleTool {
     
     /** The tool properties */
     private VertexTool vertexTool;
     private SegmentTool segmentTool;

     /** Flag to indicate the builder should auto-create walls */
     private boolean createDefaultShape;
     
     /**
      * Create a new tool for adding a building to the scene.  The properties 
      * contain on a single default sheet.
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
      * @param entityProps - The properties
      * @param segmentProps - The segment properties (used to clone)
      * @param vertexProps - The vertex properties (used to clone)
      * @param createDefaultShape - Create a square building?
      */
     public BuildingTool(
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
             HashMap<String, Object> entityProps, 
             SegmentTool segmentTool, 
             VertexTool vertexTool, 
             boolean createDefaultShape) {

        // setup the base properties
        super(id, name, topDownIcon, interfaceIcons, toolType, url, 
                description, size, scale, constraint, category, isFixedAspect, 
                isFixedSize, isHelper, isController, entityProps);
               
        entityParams.put(SegmentableEntity.TOOL_NAME_PROP, name);
        
        // setup the segment specific properties
        this.segmentTool = segmentTool;
         
        // setup the vertex specific properties
        this.vertexTool = vertexTool;
        
        this.createDefaultShape = createDefaultShape;
        
    }

     /**
      * Create a new tool for adding a building to the scene. The properties 
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
      * @param segmentProps - The segment properties (used to clone)
      * @param vertexProps - The vertex properties (used to clone)
      * @param createDefaultShape - Create a square building?
      */
    public BuildingTool(
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
            Map<String, Map<String, Object>> defaultProperties,
            SegmentTool segmentTool, 
            VertexTool vertexTool, 
            boolean createDefaultShape) {

        // setup the base properties
        super(id, name, topDownIcon, interfaceIcons, toolType, url, 
                description, size, scale, constraint, category, isFixedAspect, 
                isFixedSize, isHelper, isController, defaultProperties);
               
        entityParams.put(SegmentableEntity.TOOL_NAME_PROP, name);
        
        // setup the segment specific properties
        this.segmentTool = segmentTool;
         
        // setup the vertex specific properties
        this.vertexTool = vertexTool;
        
        this.createDefaultShape = createDefaultShape;
        
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Does this tool create a basic square shape by default.
     * 
     * @return the createDefaultShape
     */
    public boolean isCreateDefaultShape() {
        return createDefaultShape;
    }

    /**
     * Sets whether this tool create a basic square shape by default.
     * 
     * @param createDefaultShape the createDefaultShape to set
     */
    public void setCreateDefaultShape(boolean createDefaultShape) {
        this.createDefaultShape = createDefaultShape;
    }

    /**
     * Get the segment tool
     * 
     * @return the tool
     */
    public SegmentTool getSegmentTool() {
        return segmentTool;
    }

    /**
     * Get the vertex tool
     * 
     * @return the tool
     */
    public VertexTool getVertexTool() {
        return vertexTool;
    }
        
}