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

// Application specific imports
import java.util.Map;
import java.util.HashMap;

import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiplicityConstraint;

/**
 * A paste tool.  Used to place a copied item into the setTool process
 * as if the user had selected a tool from the catalog
 *
 * @author Russell Dodds
 * @version $Revision: 1.7 $
 */
public class PasteTool extends SimpleTool {
   
    /** The entity to paste */
    private Entity pasteEntity;
        
    /**
     * 
     * Create a new tool for adding a segment to the scene. The properties 
     * contain multiple sheets.
     * 
     * @param pasteEntity - The entity being pasted
     */
    public PasteTool(
            Entity pasteEntity, 
            float[] size, 
            float[] scale) {        
        
        // setup the base properties
        super(
                pasteEntity.getToolID(), 
                pasteEntity.getName(), 
                pasteEntity.getIconURL(null), 
                null, 
                pasteEntity.getType(), 
                pasteEntity.getModelURL(), 
                pasteEntity.getDescription(),
                size, 
                scale,  
                MultiplicityConstraint.NO_REQUIREMENT, 
                pasteEntity.getCategory(), 
                pasteEntity.isFixedAspect(), 
                pasteEntity.isFixedSize(), 
                pasteEntity.isHelper(), 
                pasteEntity.isController(), 
                new HashMap<String, Map<String, Object>>());
                     
        this.pasteEntity = pasteEntity;
         
    }  
    
    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * @return the pasteEntity
     */
    public Entity getPasteEntity() {
        return pasteEntity;
    }

}