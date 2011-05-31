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

package org.chefx3d.view;

// External Imports
// none

// Local Imports
// none

/**
 * Defines the requirements to listening on View status
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public interface ViewListener {
	
	/**
	 * Signal that the view is ready for business
	 *
	 * @param The View object that is ready
	 */
	public void viewInitialized(View view);
}
