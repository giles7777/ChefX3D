/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.toolbar.awt;

// External Imports

// Internal Imports
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.ToolGroup;

/**
 * Can the toolbar be expanded
 * 
 * @author Russell Dodds
 * @version $Revision: 1.5 $
 */
interface ExpandableToolbar {
        
    /**
     * Open the catalog to the parent group of the tool provided
     *
     * @param tool
     */
    public void expandNode(Tool tool);
   
    /**
     * Generate the panel to hold the buttons
     * 
     * @param tool
     */
    public void generatePanel(Tool tool);
    
    /**
     * Set the current group tool ID.
     * 
     * @param toolID
     */
    public void setActiveGroupID(String toolID);
    
    /**
     * Get the current active tool group ID.
     * 
     * @return
     */
    public String getActiveGroupID();
    
}
