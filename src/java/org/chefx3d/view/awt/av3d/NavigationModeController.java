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
import org.j3d.device.input.TrackerState;

// Local Imports
// None

/**
 * Define the requirements for controlling a Viewpoint.
 * 
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */	
public interface NavigationModeController {
	
	/**
	 * Return the text identifier of this mode
	 * 
	 * @return The text identifier of this mode
	 */
	public String getName();
	
	/**
	 * Navigation has started, initialize the mode state
	 * 
     * @param state The container for device input parameters
	 */
	public void start(TrackerState state);
	
	/**
	 * Process the movement for this mode
	 *
     * @param state The container for device input parameters
	 */
	public void move(TrackerState state);
	
	/**
	 * Navigation has completed, terminate any processing
	 *
     * @param state The container for device input parameters
	 */
	public void finish(TrackerState state);

	/**
	 * Return the current center-of-rotation in the argument array.
	 * If the arry is null or undersized, a new array will be 
	 * allocated
	 *
	 * @param cor An array to initialize with the value.
	 * @return the current center-of-rotation
	 */
	public float[] getCenterOfRotation(float[] cor);
	
	/**
	 * Set the current center-of-rotation.
	 *
	 * @param cor An array containing the center-of-rotation.
	 */
	public void setCenterOfRotation(float[] cor);
}
