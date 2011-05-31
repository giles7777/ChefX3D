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
import org.chefx3d.model.AbstractVertexTool;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiplicityConstraint;
import org.chefx3d.model.SegmentableEntity;

/**
 * A vertex tool.
 *
 * @author Russell Dodds
 * @version $Revision: 1.4 $
 */
public class VertexTool extends SimpleTool 
    implements AbstractVertexTool {
          
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
    public VertexTool(
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
                        
        createDefaultShape = false;
                
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
    public VertexTool(
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

    /* (non-Javadoc)
     * @see org.chefx3d.tool.AbstractVertexTool#isLine()
     */
    public boolean isLine() {
        return (Boolean)entityParams.get(SegmentableEntity.IS_LINE_PROP);
    }
    
    /* (non-Javadoc)
     * @see org.chefx3d.tool.AbstractVertexTool#getSegmentLength()
     */
    public float getSegmentLength() {
        return (Float)entityParams.get(SegmentableEntity.FIXED_LENGTH_PROP);
    }
        
    /* (non-Javadoc)
     * @see org.chefx3d.tool.AbstractVertexTool#isCreateDefaultShape()
     */
    public boolean isCreateDefaultShape() {
        return createDefaultShape;
    }

    /* (non-Javadoc)
     * @see org.chefx3d.tool.AbstractVertexTool#setCreateDefaultShape(boolean)
     */
    public void setCreateDefaultShape(boolean createDefaultShape) {
        this.createDefaultShape = createDefaultShape;
    }

}