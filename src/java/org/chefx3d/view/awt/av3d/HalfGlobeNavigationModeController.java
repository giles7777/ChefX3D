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

import javax.vecmath.AxisAngle4d;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.SceneGraphPath;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.VertexGeometry;

import org.j3d.aviatrix3d.picking.PickRequest;

import org.j3d.device.input.TrackerState;

import org.j3d.renderer.aviatrix3d.util.AVIntersectionUtils;

import org.j3d.util.MatrixUtils;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.ChangePropertyTransientCommand;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.ViewpointEntity;

/**
 * Implementation of NavigationModeController that produces an 
 * Examine behavior that is restricted to moving in the 'top'
 * hemisphere.
 * 
 * @author Rex Melton
 * @version $Revision: 1.10 $
 */	
class HalfGlobeNavigationModeController implements NavigationModeController {
	
	/** The mode name */
	private static final String name = "Examine";
	
	/** Vector defining the +Y axis */
	private static final Vector3f Y_AXIS = new Vector3f(0, 1, 0);
	
	/** The ViewpointEntity */
	private ViewpointEntity ve;
	
	/** Command Execution manager */
	private CommandController cmdCntl;
	
	/** The navigation collision manager */
	private NavigationCollisionManager collisionManager;
	
	/** The navigation status manager */
	private NavigationStatusManager statusManager;
	
	/** The active ViewpointData */
	private ViewpointData data;
	
	/** The root content Group */
	private Group rootGroup;
	
    /** The Navigation TransformGroup */
    private TransformGroup navigationTransform;

    /** Handler for intersection testing */
    private AVIntersectionUtils iutils;

	/** Utilities for doing matrix functions */
	private MatrixUtils matrixUtils;
	
	/** The initial position */
	private float[] startPosition;
	
	/** The current position */
	private float[] currentPosition;
	
	/** Scratch variable containing the direction of the move */
	private float[] direction;
	
	/** Scratch rotation */
	private AxisAngle4f rot;
	
	/** Scratch matrix */
	private Matrix3f mtx;
	
	/** Scratch vector */
	private Vector3f vec;
	
	/** Scratch vector */
	private Vector3f inVector;
	
	/** The horizontal vector of the view matrix */
	private Vector3f rightVector;
	
	/** Scratch matrix to retrieve the Viewpoint Transform's matrix */
	private Matrix4f startViewMatrix;
	
	/** Scratch matrix to calculate the new Viewpoint Transform's matrix */
	private Matrix4f destViewMatrix;
	
	/** Scratch Vector used to read the position value from the viewMatrix */
	private Vector3f positionVector;
	
	/** Scratch Point used to represent the position value */
	private Point3f positionPoint;
	
	/** Center of rotation for examine and look at modes */
	private Point3f centerOfRotation;
	
	/** Scratch array for view matrix values */
	private float[] array;
	
	/** Flag indicating whether the center of rotation has been initialized */
	private boolean initialized;
	
	/**
	 * Constructor
	 * 
	 * @param data The ViewpointData to manipulate
	 * @param rootGroup The root content Group
     * @param navigationTransform The navigation TransformGroup
	 * @param cmdCntl The CommandController
	 * @param ve The ViewpointEntity
	 * @param collisionManager The navigation collision detector
	 * @param statusManager The navigation status reporter
	 */
	HalfGlobeNavigationModeController(
		ViewpointData data,
		Group rootGroup,
		TransformGroup navigationTransform,
		CommandController cmdCntl,
		ViewpointEntity ve, 
		NavigationCollisionManager collisionManager, 
		NavigationStatusManager statusManager) {
		
		this.data = data;
		this.rootGroup = rootGroup;
		this.navigationTransform = navigationTransform;
		this.cmdCntl = cmdCntl;
		this.ve = ve;
		this.collisionManager = collisionManager;
		this.statusManager = statusManager;
		
		matrixUtils = new MatrixUtils();
		
		startPosition = new float[3];
		currentPosition = new float[3];
		direction = new float[3];
		
		rot = new AxisAngle4f();
		mtx = new Matrix3f();
		vec = new Vector3f();
		
		inVector = new Vector3f();
		rightVector = new Vector3f();
		
		startViewMatrix = new Matrix4f();
		destViewMatrix = new Matrix4f();
		positionVector = new Vector3f();
		positionPoint = new Point3f();
		centerOfRotation = new Point3f();
		
		array = new float[16];
		
        iutils = new AVIntersectionUtils();
	}
	
	//---------------------------------------------------------------
	// Methods defined by NavigationMode
	//---------------------------------------------------------------
	
	/**
	 * Return the text identifier of this mode
	 * 
	 * @return The text identifier of this mode
	 */
	public String getName() {
		return(name);
	}
	
	/**
	 * Navigation has started, initialize the mode state
	 * 
     * @param state The container for device input parameters
	 */
	public void start(TrackerState state) {
		if (!initialized) {
			setCofR();
			initialized = true;
		}
		startPosition[0] = state.devicePos[0];
		startPosition[1] = state.devicePos[1];
		startPosition[2] = state.devicePos[2];
	}
	
	/**
	 * Process the movement for this mode
	 *
     * @param state The container for device input parameters
	 */
	public void move(TrackerState state) {
		
		currentPosition[0] = state.devicePos[0];
		currentPosition[1] = state.devicePos[1];
		currentPosition[2] = state.devicePos[2];
		
		boolean processMove = false;
		
		boolean isWheel = (state.actionType == TrackerState.TYPE_WHEEL);
		boolean isZoom = (isWheel || state.ctrlModifier);
		
		if (isZoom) {
			
			direction[0] = 0;
			direction[1] = 0;
			
			if (isWheel) {
				direction[2] = state.wheelClicks * 0.1f;
				
			} else if (state.ctrlModifier) {
				direction[2] = (currentPosition[1] - startPosition[1]);
				
			} else {
				direction[2] = 0;
			}
			
			if (direction[2] != 0) {
				
				direction[2] *= 16;
				
				data.viewpointTransform.getTransform(startViewMatrix);
				startViewMatrix.get(positionVector);
				
				inVector.x = startViewMatrix.m02;
				inVector.y = startViewMatrix.m12;
				inVector.z = startViewMatrix.m22;
				inVector.normalize();
				inVector.scale(direction[2]);
				
				positionVector.add(inVector);
				
				// stay above the floor
				if (positionVector.y > 0) {
					destViewMatrix.set(startViewMatrix);
					destViewMatrix.setTranslation(positionVector);
					
					processMove = true;
				}
			}
		} else {
			
			direction[0] = -(startPosition[0] - currentPosition[0]);
			direction[1] = -(currentPosition[1] - startPosition[1]);
			direction[2] = 0;
			
			float Y_Rotation = direction[0];
			float X_Rotation = direction[1];
			
			if( ( Y_Rotation != 0 ) || ( X_Rotation != 0 ) ) {
				
				double theta_Y = -Y_Rotation * Math.PI;
				double theta_X = -X_Rotation * Math.PI;
				
				data.viewpointTransform.getTransform(startViewMatrix);
				startViewMatrix.get(positionVector);
				
				positionVector.x -= centerOfRotation.x;
				positionVector.y -= centerOfRotation.y;
				positionVector.z -= centerOfRotation.z;
				
				if (theta_Y != 0) {

					rot.set(0, 1, 0, (float)theta_Y);
					mtx.set(rot);
					mtx.transform(positionVector);
				}
				
				if (theta_X != 0) {
					
					vec.set(positionVector);
					vec.normalize();
					float angle = vec.angle(Y_AXIS);
					
					if (angle == 0) {
						if (theta_X > 0) {
							rightVector.x = startViewMatrix.m00;
							rightVector.y = startViewMatrix.m10;
							rightVector.z = startViewMatrix.m20;
							rightVector.normalize();
							rot.set(rightVector.x, 0, rightVector.z, (float)theta_X);
						} else {
							rot.set(0, 0, 1, 0);
						}
					} else {
						if ((theta_X + angle) < 0) {
							theta_X = -(angle - 0.0001f);
						}
						vec.y = 0;
						vec.normalize();
						rot.set(vec.z, 0, -vec.x, (float)theta_X);
					}
					mtx.set(rot);
					mtx.transform(positionVector);
				}
				
				positionPoint.x = positionVector.x + centerOfRotation.x;
				positionPoint.y = positionVector.y + centerOfRotation.y;
				positionPoint.z = positionVector.z + centerOfRotation.z;
				
				// don't go below the floor
				if (positionPoint.y > 0) {
					
					matrixUtils.lookAt(positionPoint, centerOfRotation, Y_AXIS, destViewMatrix);
					matrixUtils.inverse(destViewMatrix, destViewMatrix);
					
					processMove = true;
				}
			}
		}
		if (processMove) {
			
			boolean collisionDetected = 
				collisionManager.checkCollision(startViewMatrix, destViewMatrix);
			
			if (!collisionDetected) {
				AV3DUtils.toArray(destViewMatrix, array);
				ChangePropertyTransientCommand cptc = new ChangePropertyTransientCommand(
					ve, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ViewpointEntity.VIEW_MATRIX_PROP,
					array,
					null);
				cmdCntl.execute(cptc);
				if (statusManager != null) {
					statusManager.fireViewMatrixChanged(destViewMatrix);
				}
			}
		}
		startPosition[0] = currentPosition[0];
		startPosition[1] = currentPosition[1];
		startPosition[2] = currentPosition[2];
	}
	
	/**
	 * Navigation has completed, terminate any processing
	 *
     * @param state The container for device input parameters
	 */
	public void finish(TrackerState state) {
		
	}
	
	/**
	 * Return the current center-of-rotation in the argument array.
	 * If the arry is null or undersized, a new array will be 
	 * allocated
	 *
	 * @param cor An array to initialize with the value.
	 * @return the current center-of-rotation
	 */
	public float[] getCenterOfRotation(float[] cor) {
		if ((cor == null) || (cor.length < 3)) {
			cor = new float[3];
		}
		centerOfRotation.get(cor);
		return(cor);
	}
	
	/**
	 * Set the current center-of-rotation.
	 *
	 * @param cor An array containing the center-of-rotation.
	 */
	public void setCenterOfRotation(float[] cor) {
		//centerOfRotation.set(cor);
	}
	
	//---------------------------------------------------------------
	// Local Methods
	//---------------------------------------------------------------
	
	/**
	 * Configure the center-of-rotation. This method runs a ray pick in
	 * from the eye point and sets the closest geometry intersection as
	 * the center-of-rotation.
	*/
	private void setCofR() {
		
        Vector3f pos_vec = new Vector3f();
        Point3f pos = new Point3f();
        Vector3f dir = new Vector3f();
		Matrix4f view_mtx = new Matrix4f();
		Matrix4f nav_mtx = new Matrix4f();
        Point3f cor = new Point3f();
		
		navigationTransform.getTransform(nav_mtx);
		
		data.viewpointTransform.getTransform(view_mtx);
		view_mtx.get(pos_vec);
		
		// the eye point
		pos.set(pos_vec);
		
		// the eye direction
		dir.x = view_mtx.m02;
		dir.y = view_mtx.m12;
		dir.z = view_mtx.m22;
		dir.negate();
		dir.normalize();
		
		// transform into world space
		nav_mtx.transform(pos);
		nav_mtx.transform(dir);
		
		ArrayList pickResults = new ArrayList();
		PickRequest pickRequest = new PickRequest();
		pickRequest.pickGeometryType = PickRequest.PICK_RAY;
		pickRequest.pickSortType = PickRequest.SORT_ORDERED;
		pickRequest.pickType = PickRequest.FIND_GENERAL;
		pickRequest.useGeometry = false;
		pickRequest.foundPaths = pickResults;
		
		// initialize the pick request
		pickRequest.origin[0] = pos.x;
		pickRequest.origin[1] = pos.y;
		pickRequest.origin[2] = pos.z;
		
		pickRequest.destination[0] = dir.x;
		pickRequest.destination[1] = dir.y;
		pickRequest.destination[2] = dir.z;
		
		rootGroup.pickSingle(pickRequest);
		
		if (pickRequest.pickCount > 0) {
			
        	Point3f intersectPoint = new Point3f();
        	Vector3f intersectVector = new Vector3f();
		
			float min_distance = Float.MAX_VALUE;
			// sort through the bounds intersections
			int num_pick = pickResults.size();
			for (int i = 0; i < num_pick; i++) {
				
				SceneGraphPath sgp = (SceneGraphPath)pickResults.get(i);
				sgp.getTransform(view_mtx);
				
				Shape3D shape = (Shape3D)sgp.getTerminalNode();
				VertexGeometry geom = (VertexGeometry)shape.getGeometry();
				
				//determine if there was an actual geometry intersection
				boolean intersect = iutils.rayUnknownGeometry(
					pos,
					dir,
					0,
					geom,
					view_mtx,
					intersectPoint,
					false);
				
				if (intersect) {
					intersectVector.set(
						intersectPoint.x - pos.x,
						intersectPoint.y - pos.y,
						intersectPoint.z - pos.z);

					float distance = intersectVector.length();
					if (distance < min_distance) {
						min_distance = distance;
						cor.set(intersectPoint);
					}
				}
			} 
			nav_mtx.invert();
			nav_mtx.transform(cor);
		}
		centerOfRotation.set(cor);
	}
}
