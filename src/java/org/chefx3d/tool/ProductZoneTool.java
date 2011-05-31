/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.util.Map;

// Internal Imports
import org.chefx3d.model.Entity;

/**
 * A Product Zone tool. Creates product zones for products with zones.
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public class ProductZoneTool extends SimpleTool { 
    
    /** The tool name and ID to use */
    public static final String TOOL_ID = "ProductZone";

    /**
     * 
     * Create a new tool for adding a product zones to the scene. 
     * The properties contain multiple sheets.
     * 
     * @param id The tool id
     * @param name The name of the tool
     * @param description The description of the tool
     * @param size The starting size
     * @param scale The starting scale
     * @param defaultProperties The properties
     */
    public ProductZoneTool(
            String name, 
            String description, 
            float[] size, 
            float[] scale, 
            Map<String, Map<String, Object>> defaultProperties) {
        
        // setup the base properties
    	super(TOOL_ID, 
    	        name, 
    			null, 
    			null, 
    			null,
    			Entity.TYPE_MODEL_ZONE, 
    			null, 
    			description, 
                size, 
                scale,  
                null, 
                "Category.ProductZone", 
                false, 
                false, 
                false, 
                false, 
                defaultProperties);
    }
}
