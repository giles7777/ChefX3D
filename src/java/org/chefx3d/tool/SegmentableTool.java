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
import org.chefx3d.model.MultiplicityConstraint;
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentableEntity;

/**
 * A multi segment tool.
 *
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public class SegmentableTool extends SimpleTool {
          
    /** The tool properties */
    private VertexTool vertexTool;
    private AbstractSegmentTool segmentTool;

    /** Flag to indicate the builder should auto-create segments */
    private boolean createDefaultShape;
   
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
     * @param segmentLength - Fixed length segments (0 means not fixed)
     * @param isLine - Is it a single line or branching segments (A-star)
     * @param defaultProperties - The properties
     * @param vertexProperites - The vertex properties (used to clone)
     */
    public SegmentableTool(
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
            float segmentLength, 
            boolean isLine, 
            String entitySheetName,
            Map<String,Map<String, Object>> defaultProperties, 
            AbstractSegmentTool segmentTool, 
            VertexTool vertexTool) {
        
        
        // setup the base properties
        super(id, name, topDownIcon, interfaceIcons, toolType, url, 
                description, size, scale,  constraint, category, isFixedAspect, 
                isFixedSize, isHelper, isController, defaultProperties);
                
        entityParams.put(SegmentableEntity.TOOL_NAME_PROP, name);
        entityParams.put(SegmentableEntity.FIXED_LENGTH_PROP, segmentLength);
        entityParams.put(SegmentableEntity.IS_LINE_PROP, isLine);    
        
        createDefaultShape = false;
        
        // setup the segment specific properties
        this.segmentTool = segmentTool;
         
        // setup the vertex specific properties
        this.vertexTool = vertexTool;
        
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
     * @param segmentLength - Fixed length segments (0 means not fixed)
     * @param isLine - Is it a single line or branching segments (A-star)
     * @param defaultProperties - The properties
     * @param vertexProperites - The vertex properties (used to clone)
     */
    public SegmentableTool(
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
            float segmentLength, 
            boolean isLine, 
            Map<String,Map<String, Object>> defaultProperties, 
            AbstractSegmentTool segmentTool, 
            VertexTool vertexTool) {
        
        // setup the base properties
        this(id, name, topDownIcon, interfaceIcons, toolType, url, 
                description, size, scale,  constraint, category, isFixedAspect, 
                isFixedSize, isHelper, isController, segmentLength, isLine, 
                Entity.DEFAULT_ENTITY_PROPERTIES, defaultProperties, 
                segmentTool, vertexTool);

    }

    /**
     * Is the tool a line tool or does it allow multiple paths.
     *
     * @return True if its restricted to a single line
     */
    public boolean isLine() {
        return (Boolean)entityParams.get(SegmentableEntity.IS_LINE_PROP);
    }
    
    /**
     * Is the tool a line tool or does it allow multiple paths.
     *
     * @return True if its restricted to a single line
     */
    public float getSegmentLength() {
        return (Float)entityParams.get(SegmentableEntity.FIXED_LENGTH_PROP);
    }
        
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
    public AbstractSegmentTool getSegmentTool() {
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