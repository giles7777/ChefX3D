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

package org.chefx3d.view.awt.av3d;

// External imports
// none

// Local Imports
// none

/**
 * Defines the requirements for legend overlay components.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
interface LegendHandler extends ConfigListener {
	
	/**
	 * Enable the display of the components
	 *
	 * @param state The display of the components
	 */
	public void setEnabled(boolean state);
		
}
