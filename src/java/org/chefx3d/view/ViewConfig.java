/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view;

// Standard library imports
// None

// Application specific imports
// None

/**
 * Defines the requirements for View configuration options.
 * 
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public interface ViewConfig {
    
	/**
	 * Enable the automated configuration of the elevation for added
	 * and moved entities.
	 *
	 * @param enable The enabled state
	 */
	public void setConfigElevation(boolean enable);
	
	/**
	 * Return the state of automated configuration of the elevation 
	 * for added and moved entities.
	 *
	 * @return The enabled state
	 */
	public boolean getConfigElevation();
}