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

package org.chefx3d.view.awt.scenemanager;

// External Imports
// None

// Local Imports
// None

/**
 * Defines the requirements for receiving status on navigation events
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.2 $
 */
public interface ResizeListener {

	/**
	 * Notification that the width and height have changed by the
	 * percent indicated by the parameters
	 *
	 * @param newWidth new width, in pixels
	 * @param newHeight new height, in pixels
	 */
    public void sizeChanged(int newWidth, int newHeight);

}