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

//External Imports

//Internal Imports
import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.ToolGroup;

/**
 * Filter the catalog based on rules.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */
public interface CatalogFilter {

    /**
     * Sets the currently active group
     * 
     * @param model
     * @param toolGroup
     * @param flatPanel
     */
	public void setCurrentToolGroup(
			WorldModel model, 
			ToolGroup toolGroup, 
			ToolIconPanel flatPanel);
	
    /**
     * Refreshes the status of the currently active group
     */
	public void refreshCurrentToolGroup();

    /**
     * Process the catalog to enable whatever should be enabled
     * 
     * @param The state to set, true is enabled.
     */
	public void setEnabled(boolean flag);
}
