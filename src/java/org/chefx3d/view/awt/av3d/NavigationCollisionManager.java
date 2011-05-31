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
import javax.vecmath.Matrix4f;

// Local Imports
// None

/**
 * Defines the requirements for detecting navigation collisions.
 * 
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */	
public interface NavigationCollisionManager {
	
	/**
	 * Return whether this view will result in a collision 
	 * with geometry in the scene.
	 *
	 * @param startViewMatrix The starting point
	 * @param destViewMatrix The destination
	 * @return The collision state
	 */
	public boolean checkCollision(
		Matrix4f startViewMatrix, 
		Matrix4f destViewMatrix);
}
