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
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.device.input.TrackerState;

import org.j3d.util.MatrixUtils;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.ChangePropertyTransientCommand;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.ViewpointEntity;

/**
 * Implementation of NavigationMode that produces an Examine behavior
 * 
 * @author Rex Melton
 * @version $Revision: 1.8 $
 */	
class ExamineNavigationModeController implements NavigationModeController {
	
	/** The mode name */
	private static final String name = "Examine";
	
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
	
	/** Utilities for doing matrix functions */
	private MatrixUtils matrixUtils;
	
	/** The initial position */
	private float[] startPosition;
	
	/** The current position */
	private float[] currentPosition;
	
	/** Scratch variable containing the direction of the move */
	private float[] direction;
	
	/** The up vector of the view matrix */
	private Vector3f upVector;
	
	/** The horizontal vector of the view matrix */
	private Vector3f rightVector;
	
	/** The rotation about the up vector */
	private AxisAngle4f vrtRot;
	
	/** The rotation about the horizontal vector */
	private AxisAngle4f hrzRot;
	
	/** Scratch matrix for calculating the total rotation */
	private Matrix3f rotMat1;
	
	/** Scratch matrix for calculating the total rotation */
	private Matrix3f rotMat2;
	
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
	
	/**
	 * Constructor
	 * 
	 * @param data The ViewpointData to manipulate
	 * @param cmdCntl The CommandController
	 * @param ve The ViewpointEntity
	 * @param collisionManager The navigation collision detector
	 * @param statusManager The navigation status reporter
	 */
	ExamineNavigationModeController(
		ViewpointData data,  
		CommandController cmdCntl,
		ViewpointEntity ve, 
		NavigationCollisionManager collisionManager, 
		NavigationStatusManager statusManager) {
		
		this.data = data;
		this.cmdCntl = cmdCntl;
		this.ve = ve;
		this.collisionManager = collisionManager;
		this.statusManager = statusManager;
		
		matrixUtils = new MatrixUtils();
		
		startPosition = new float[3];
		currentPosition = new float[3];
		direction = new float[3];
		
		upVector = new Vector3f();
		rightVector = new Vector3f();
		vrtRot = new AxisAngle4f();
		hrzRot = new AxisAngle4f();
		rotMat1 = new Matrix3f();
		rotMat2 = new Matrix3f();
		
		startViewMatrix = new Matrix4f();
		destViewMatrix = new Matrix4f();
		positionVector = new Vector3f();
		positionPoint = new Point3f();
		centerOfRotation = new Point3f();
		
		array = new float[16];
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
		
		direction[0] = -(startPosition[0] - currentPosition[0]);
		direction[1] = -(currentPosition[1] - startPosition[1]);
		direction[2] = 0;
		
		float Y_Rotation = direction[0];
		float X_Rotation = direction[1];
		
		boolean collisionDetected = false;
		if( ( Y_Rotation != 0 ) || ( X_Rotation != 0 ) ) {
			
			double theta_Y = Y_Rotation * Math.PI;
			double theta_X = X_Rotation * Math.PI;
			
			data.viewpointTransform.getTransform(startViewMatrix);
			startViewMatrix.get(positionVector);
			
			upVector.x = startViewMatrix.m01;
			upVector.y = startViewMatrix.m11;
			upVector.z = startViewMatrix.m21;
			upVector.normalize( );
			
			vrtRot.set( upVector, (float)-theta_Y );
			rotMat1.set( vrtRot );
			
			rightVector.x = startViewMatrix.m00;
			rightVector.y = startViewMatrix.m10;
			rightVector.z = startViewMatrix.m20;
			rightVector.normalize( );
			
			hrzRot.set( rightVector, (float)theta_X );
			rotMat2.set( hrzRot );
			
			rotMat1.mul( rotMat2 );
			
			positionVector.x -= centerOfRotation.x;
			positionVector.y -= centerOfRotation.y;
			positionVector.z -= centerOfRotation.z;
			
			rotMat1.transform( positionVector );
			
			positionPoint.x = positionVector.x + centerOfRotation.x;
			positionPoint.y = positionVector.y + centerOfRotation.y;
			positionPoint.z = positionVector.z + centerOfRotation.z;
			
			matrixUtils.lookAt(positionPoint, centerOfRotation, upVector, destViewMatrix);
			matrixUtils.inverse(destViewMatrix, destViewMatrix);
			
			collisionDetected = 
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
		centerOfRotation.set(cor);
	}
}
