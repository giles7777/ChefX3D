/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.common;

// External imports
// none

// Internal imports
// none

/**
 * Defines the requirements for application status logging
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public interface StatusReporter {
	
	/**
	 * Set the selection status condition
	 *
	 * @param status The indicator of selection status. A color 
	 * in float array form [r, g, b].
	 */
	public void setSelectionStatus(float[] status);
}
