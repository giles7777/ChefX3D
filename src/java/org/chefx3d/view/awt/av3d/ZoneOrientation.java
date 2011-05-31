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
import javax.vecmath.Vector3f;

// Local Imports
// none

/**
 * Container for the orientation vectors of a zone.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
class ZoneOrientation {
	
	/** The default orientation looking at the floor */
	static final ZoneOrientation HORIZONTAL = new ZoneOrientation(
		new Vector3f(0, 1, 0),
		new Vector3f(0, 0, -1),
		new Vector3f(1, 0, 0));
	
	/** The default orientation looking at a wall */
	static final ZoneOrientation VERTICAL = new ZoneOrientation(
		new Vector3f(0, 0, 1),
		new Vector3f(0, 1, 0),
		new Vector3f(1, 0, 0));
	
	/** The zone's normal */
	Vector3f normal;

	/** The zone's up vector */
	Vector3f up;
	
	/** The zone's right vector */
	Vector3f right;
	
	ZoneOrientation(Vector3f normal, Vector3f up, Vector3f right) {
		this.normal = normal;
		this.up = up;
		this.right = right;
	}
}
