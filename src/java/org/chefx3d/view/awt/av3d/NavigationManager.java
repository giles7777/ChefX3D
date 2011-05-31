/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
// None

// Local Imports
import org.chefx3d.model.ViewpointEntity;

/**
 * Defines the requirements of a manager for controlling navigation.
 * 
 * @author Rex Melton
 * @version $Revision: 1.9 $
 */	
public interface NavigationManager extends AV3DConstants {
	
	/**
	 * Set the active navigation mode
	 * 
	 * @param mode The new navigation mode
	 */
	public void setNavigationMode(NavigationMode mode);
	
	/**
	 * Return the active navigation mode
	 *
	 * @return The active navigation mode
	 */
	public NavigationMode getNavigationMode();
	
	/**
	 * Return the active navigation controller
	 *
	 * @return The active navigation controller
	 */
	public NavigationModeController getNavigationModeController();
	
	/**
	 * Set the active ViewpointData
	 * 
	 * @param data The new ViewpointData
	 */
	public void setViewpointData(ViewpointData data);
	
	/**
	 * Return the active ViewpointData
	 *
	 * @return The active ViewpointData
	 */
	public ViewpointData getViewpointData();
	
	/**
	 * Return the active ViewpointEntity
	 *
	 * @return The active ViewpointEntity
	 */
	public ViewpointEntity getViewpointEntity();
	
	/**
	 * Refresh the viewpoint's transform.
	 */
	public void refresh();
}
