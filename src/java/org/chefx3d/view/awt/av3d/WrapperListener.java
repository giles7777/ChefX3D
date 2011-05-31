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

// External Imports
// none

// Local Imports
// none

/**
 * Defines the requirements for obtaining callbacks on
 * wrapper status events
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
interface WrapperListener {
	
	/**
	 * Notification that the active switch group has changed
	 * on the argument.
	 *
	 * @param src The wrapper object that has experienced a change
	 */
	public void switchGroupChanged(Object src);
	
	/**
	 * Notification that the geometry (and bounds) have changed
	 * on the argument.
	 *
	 * @param src The wrapper object that has experienced a change
	 */
	public void geometryChanged(Object src);
}
