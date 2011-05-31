/****************************************************************************
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
import javax.vecmath.Point3f;

import org.j3d.aviatrix3d.SceneGraphPath;

/**
 * Container for picking results
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
class PickData {
	
	/** The reference object associated with the picked geometry */
	Object object;
	
	/** The <code>SceneGraphPath</code> of the picked object */
	SceneGraphPath path;
	
	/** The intersection point on the picked geometry */
	Point3f point;
	
	/** The world transform of the intersect point */
    Matrix4f mtx;
	
	/** The distance from the pick origin to the intersection point */
	float distance;
	
	/**
	 * Constructor
	 * 
	 * @param object The reference object associated with the picked geometry
	 * @param path The <code>SceneGraphPath</code> of the picked object
	 * @param point The intersection point on the picked geometry
	 * @param mtx The world transform of the intersect point
	 * @param distance The distance from the pick origin to the intersection point
	 */
	PickData(
		Object object, 
		SceneGraphPath path, 
		Point3f point,
		Matrix4f mtx,
		float distance) {
		
		this.object = object;
		this.path = path;
		this.point = point;
		this.mtx = mtx;
		this.distance = distance;
	}
}