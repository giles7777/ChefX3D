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
import javax.vecmath.Point3f;

import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.ChangePropertyTransientCommand;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.ViewpointEntity;

/**
 * Implementation of NavigationMode that produces a Zoom behavior
 * in an orthographic Viewport
 * 
 * @author Rex Melton
 * @version $Revision: 1.7 $
 */	
class OrthoZoomNavigationModeController implements NavigationModeController {
	
	/** Hardcoding a static zoom scalar instead of dynamically adjusting it */
	private static final float DEFAULT_STATIC_ZOOM_SCALAR = 1.1f;
	
	/** The mode name */
	private static final String name = "Zoom";
	
	/** The ViewpointEntity */
	private ViewpointEntity ve;
	
	/** Command Execution manager */
	private CommandController cmdCntl;
	
	/** The navigation status manager */
	private NavigationStatusManager statusManager;
	
	/** The active ViewpointData */
	private ViewpointData data;
	
	/** The active ViewEnvironment */
	private ViewEnvironment viewEnv;
	
	/** The initial position */
	private float[] startPosition;
	
	/** The current position */
	private float[] currentPosition;
	
	/** Scratch variable containing the direction of the move */
	private float[] direction;
	
	/** The view frustum */
	private double[] frustum;
	
	/** Center of rotation for examine and look at modes */
	private Point3f centerOfRotation;
	
	
	/**
	 * Constructor
	 * 
	 * @param data The ViewpointData to manipulate
	 * @param viewEnv The ViewEnvironment to manipulate
	 * @param cmdCntl The CommandController
	 * @param ve The ViewpointEntity
	 * @param statusManager The navigation status reporter
	 */
	OrthoZoomNavigationModeController(
		ViewpointData data, 
		ViewEnvironment viewEnv,  
		CommandController cmdCntl,
		ViewpointEntity ve, 
		NavigationStatusManager statusManager) {
		
		this.data = data;
		this.viewEnv = viewEnv;
		this.cmdCntl = cmdCntl;
		this.ve = ve;
		this.statusManager = statusManager;
		
		startPosition = new float[3];
		currentPosition = new float[3];
		direction = new float[3];
		frustum = new double[6];
		
		centerOfRotation = new Point3f();
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
		
		direction[0] = 0;
		direction[1] = 0;
		direction[2] = (currentPosition[1] - startPosition[1]);
		
		if( direction[2] != 0 ) {
			
			// TODO: hard coded scale factor should be 
			// replaced with something more dynamic
			float scale = DEFAULT_STATIC_ZOOM_SCALAR;
			if (direction[2] < 0) {
				scale = 1/scale;
			}

			viewEnv.getViewFrustum(frustum);
			
			frustum[0] *= scale;
            frustum[1] *= scale;
            frustum[2] *= scale;
            frustum[3] *= scale;
            
            // 
			// Set the clipping planes
            //
			viewEnv.setOrthoParams(frustum[0], frustum[1], frustum[2], frustum[3]);
			
			ChangePropertyTransientCommand cptc = 
				new ChangePropertyTransientCommand(
						ve, 
						Entity.DEFAULT_ENTITY_PROPERTIES, 
						ViewpointEntity.ORTHO_PARAMS_PROP,
						frustum,
						null);
			cmdCntl.execute(cptc);
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
