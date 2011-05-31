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
import java.util.ArrayList;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.SceneGraphPath;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.VertexGeometry;

import org.j3d.aviatrix3d.picking.PickRequest;

import org.j3d.renderer.aviatrix3d.util.AVIntersectionUtils;

// Local Imports
// None

/**
 * Implementation for detecting navigation collisions. Performs a 
 * simple spherical check.
 * 
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */	
class SimpleNavigationCollisionManager implements NavigationCollisionManager {
	
	/** Intersection utilities used for collision detection */
	private AVIntersectionUtils intersector;
	
	/** The root grouping node */
	private Group rootGroup;
	
    /** The Navigation TransformGroup */
    private TransformGroup navigationTransform;

	/** The collision radius */
	private float radius;
	
	/** Pick request object for collision detection */
	private PickRequest collisionPicker;
	
	/** Scratch object for getting the view position */
	private Vector3f startVector;
	
	/** Scratch object for getting the view position */
	private Point3f startPoint;
	
	/** Scratch object for getting the view position */
	private Vector3f destVector;
	
	/** Scratch object for the navigation direction */
	private Vector3f directionVector;
	
	/** Scratch object for getting the intersection point */
	private Point3f intersectionPoint;
	
	/** Scratch matrix to contain the world transform */
	private Matrix4f worldTransform;
	
	/** Working copy of the startViewMatrix */
	private Matrix4f startMatrix;
	
	/** Working copy of the destViewMatrix */
	private Matrix4f destMatrix;
	
	/** Scratch matrix to contain the navigation transform */
	private Matrix4f navMatrix;
	
	/** List for containing pick results */
	private ArrayList pickResults;
	
	/**
	 * Constructor
	 */
	SimpleNavigationCollisionManager(Group rootGroup, float radius) {
		
		this.rootGroup = rootGroup;
		this.radius = radius;
		
		startVector = new Vector3f();
		startPoint = new Point3f();
		destVector = new Vector3f();
		directionVector = new Vector3f();
		intersectionPoint = new Point3f();
		worldTransform = new Matrix4f();
		startMatrix = new Matrix4f();
		destMatrix = new Matrix4f();
		navMatrix = new Matrix4f();
		
		intersector = new AVIntersectionUtils();
		
		pickResults = new ArrayList();
		collisionPicker = new PickRequest();
		collisionPicker.pickType = PickRequest.FIND_COLLIDABLES;
		collisionPicker.pickGeometryType = PickRequest.PICK_SPHERE;
		collisionPicker.pickSortType = PickRequest.SORT_ALL;
		collisionPicker.foundPaths = pickResults;
	}
	
	//---------------------------------------------------------------
	// Methods defined by NavigationCollisionManager
	//---------------------------------------------------------------
	
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
		Matrix4f destViewMatrix) {
		
		startMatrix.set(startViewMatrix);
		destMatrix.set(destViewMatrix);
		
		if (navigationTransform != null) {
			navigationTransform.getTransform(navMatrix);
			startMatrix.mul(navMatrix, startMatrix);
			destMatrix.mul(navMatrix, destMatrix);
		}
		destMatrix.get(destVector);
		
		// first check whether the destination for the
		// viewpoint has a bounds collision with any
		// collidable geometry in the scene. this uses 
		// a simple sphere bounds check
		collisionPicker.origin[0] = destVector.x;
		collisionPicker.origin[1] = destVector.y;
		collisionPicker.origin[2] = destVector.z;
		
		collisionPicker.additionalData = radius;
		
		rootGroup.pickSingle(collisionPicker);
		boolean boundsCollision = (collisionPicker.pickCount > 0);
		
		boolean geomCollision = false;

		if (boundsCollision) {
			
			// if a bounds collision, then walk through the results to
			// determine is an actual intersection with geometry will
			// occur
			for(int i = 0; (i < collisionPicker.pickCount); i++) {
				
				SceneGraphPath path = (SceneGraphPath)pickResults.get(i);
				Node node = path.getTerminalNode();
				if (node instanceof Shape3D) {
					
					Object obj = ((Shape3D)node).getGeometry();
					if (obj instanceof VertexGeometry) {
						
						VertexGeometry geom = (VertexGeometry)obj;
						path.getTransform(worldTransform);
						
						startMatrix.get(startVector);
						startPoint.set(startVector);
						directionVector.sub(destVector, startVector);
						directionVector.normalize();
						
						geomCollision = intersector.rayUnknownGeometry(
							startPoint,
							directionVector,
							radius,
							geom,
							worldTransform,
							intersectionPoint,
							true);

						if (geomCollision) {
							break;
						}
					}
				}
			}
			pickResults.clear();
		}
		return(geomCollision);
	}
	
	//---------------------------------------------------------------
	// Local Methods
	//---------------------------------------------------------------
	
	/**
	 * Set the navigation transform to use
	 * 
	 * @param matrix The navigation transform to use
	 */
	void setNavigationTransform(TransformGroup navigationTransform) {
		this.navigationTransform = navigationTransform;
	}
}