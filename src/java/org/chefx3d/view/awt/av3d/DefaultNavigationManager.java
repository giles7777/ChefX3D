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
import java.util.*;

import javax.vecmath.Matrix4f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.SimpleLayer;
import org.j3d.aviatrix3d.SimpleScene;
import org.j3d.aviatrix3d.SimpleViewport;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.ViewEnvironment;
import org.j3d.aviatrix3d.Viewpoint;

import org.j3d.device.input.TrackerState;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;
import org.chefx3d.view.awt.scenemanager.UserInputHandler;

/**
 * Manager class for controlling Navigation.
 *
 * @author Rex Melton
 * @version $Revision: 1.24 $
 */
class DefaultNavigationManager implements
    NavigationManager,
    NodeUpdateListener,
    PerFrameObserver,
    UserInputHandler,
    EntityChildListener,
    EntityPropertyListener,
    EntitySelectionListener {

    /** The view identifier */
    private String viewId;

    /** The world model */
    private WorldModel model;

    /** Command Execution manager */
    private CommandController commandController;

    /** Reporter instance for handing out errors */
    private ErrorReporter reporter;

    /** The layer manager */
    private LayerManager layerManager;

	/** The root content Group */
	private Group rootGroup;
	
    /** The Navigation TransformGroup */
    private TransformGroup navigationTransform;

    /** The scene manager observer */
    private SceneManagerObserver mgmtObserver;

    /** The navigation collision manager */
    private NavigationCollisionManager collisionManager;

    /** The navigation status manager */
    private NavigationStatusManager navStatusManager;

    /** The active navigation mode id */
    private NavigationMode activeMode;

    /** The mode processor */
    private NavigationModeController controller;

    /** The active ViewpointData */
    private ViewpointData data;

    /** Scratch array containing the device position */
    private float[] devicePosition;

    /** Scratch array for transfering the center of rotation */
    private float[] centerOfRotation;

    /** The viewpoint container */
    private ViewpointContainerEntity vce;

    /** The default viewpoint entity */
    private ViewpointEntity activeViewpointEntity;

    /** Default view matrix */
    private Matrix4f defaultViewMatrix;

    /** Scratch view matrix */
    private Matrix4f viewMatrix;

    /** State flag indicating that the view transform must be updated */
    private boolean configMatrix;

	/** Flag indicating that active navigation is in progress */
	private boolean isActive;
	
    /**
     * Constructor
     *
     * @param viewId The identifier of the view that this manages
     * @param model The WorldModel
     * @param commandController The CommandController
     * @param reporter The ErrorReporter instance to use or null
     * @param layerManager The LayerManager
     * @param rootGroup The root content Group
     * @param navigationTransform The navigation TransformGroup
     * @param mgmtObserver The SceneManagerObserver
     * @param collisionManager The navigation collision detector
     * @param navStatusManager The navigation status reporter
     */
    DefaultNavigationManager(
        String viewId,
        WorldModel model,
        CommandController commandController,
        ErrorReporter reporter,
        LayerManager layerManager,
		Group rootGroup,
		TransformGroup navigationTransform,
        SceneManagerObserver mgmtObserver,
        NavigationCollisionManager collisionManager,
        NavigationStatusManager navStatusManager) {

        setErrorReporter(reporter);

        this.viewId = viewId;
        this.model = model;
        this.commandController = commandController;
        this.layerManager = layerManager;
		this.rootGroup = rootGroup;
		this.navigationTransform = navigationTransform;
        this.mgmtObserver = mgmtObserver;
        this.collisionManager = collisionManager;
        this.navStatusManager = navStatusManager;

        mgmtObserver.addObserver(this);

        devicePosition = new float[3];
        centerOfRotation = new float[3];

        viewMatrix = new Matrix4f();
        defaultViewMatrix = new Matrix4f();

        data = new ViewpointData(
            "Default",
            layerManager.getViewpointTransform());

        data.viewpointTransform.getTransform(defaultViewMatrix);

   }

    //---------------------------------------------------------------
    // Methods defined by UserInputHandler
    //---------------------------------------------------------------

    /**
     * Process a tracker press event. This may be used to start a touchSensor,
     * start of a drag sensor or navigation
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerPressed(int tracker, TrackerState evt) {

        if (controller != null) {
            devicePosition[0] = evt.devicePos[0];
            devicePosition[1] = evt.devicePos[1];
            devicePosition[2] = evt.devicePos[2];

            controller.start(evt);
        }
		isActive = true;
    }

    /**
     * Process a tracker move event. This may be used to start a touchtracker,
     * start of a drag tracker or navigation
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerMoved(int tracker, TrackerState evt) {
    }

    /**
     * Process a tracker press event. This may be used to start a touchtracker,
     * start of a drag tracker or navigation
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerDragged(int tracker, TrackerState evt) {
		
        if (controller != null) {
            devicePosition[0] = evt.devicePos[0];
            devicePosition[1] = evt.devicePos[1];
            devicePosition[2] = evt.devicePos[2];

            controller.move(evt);
        }
    }

    /**
     * Process a tracker press event. This may be used to start a touchtracker,
     * start of a drag tracker or navigation
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerReleased(int tracker, TrackerState evt) {
		
        if (controller != null) {
            devicePosition[0] = evt.devicePos[0];
            devicePosition[1] = evt.devicePos[1];
            devicePosition[2] = evt.devicePos[2];

            controller.finish(evt);
        }
		isActive = false;
    }

    /**
     * Process a tracker click event. The click is used only on touch trackers
     * and anchors. We treat it like a cross between a select and unselect.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerClicked(int tracker, TrackerState evt) {
    }

    /**
     * Process a tracker orientation event. This is for trackers like HMDs that
     * can change orientation without changing position or other state.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerOrientation(int tracker, TrackerState evt) {
    }

    /**
     * Process the buttons on a tracker.  No other state will be read.
     *
     * @param tracker The id of the tracker calling this handler
     * @param state The current state.
     */
    public void trackerButton(int tracker, TrackerState state) {
    }

    /**
     * Process the wheel on a tracker.  No other state will be read.
     *
     * @param tracker The id of the tracker calling this handler
     * @param state The current state.
     */
    public void trackerWheel(int tracker, TrackerState state) {
		
        if (controller != null) {
            controller.move(state);
        }
    }

    //---------------------------------------------------------------
    // Methods defined by NavigationManager
    //---------------------------------------------------------------

    /**
     * Set the active navigation mode
     *
     * @param activeMode The new navigation mode
     */
    public void setNavigationMode(NavigationMode mode) {
        this.activeMode = mode;
        configController();
    }

    /**
     * Return the active navigation mode
     *
     * @return The active navigation mode
     */
    public NavigationMode getNavigationMode() {
        return(activeMode);
    }

    /**
     * Return the active navigation controller
     *
     * @return The active navigation controller
     */
    public NavigationModeController getNavigationModeController() {
        return(controller);
    }

    /**
     * Set the active ViewpointData
     *
     * @param data The new ViewpointData
     */
    public void setViewpointData(ViewpointData data) {
        if (data != this.data) {
            this.data = data;
        }
    }

    /**
     * Return the active ViewpointData
     *
     * @return The active ViewpointData
     */
    public ViewpointData getViewpointData() {
        return(data);
    }

    /**
     * Return the active ViewpointEntity
     *
     * @return The active ViewpointEntity
     */
    public ViewpointEntity getViewpointEntity() {
        return(activeViewpointEntity);
    }

	/**
	 * Refresh the viewpoint's transform.
	 */
	public void refresh() {
		configMatrix = true;
	}
	
    //----------------------------------------------------------
    // Methods for EntityPropertyListener
    //----------------------------------------------------------

    public void propertiesUpdated(List<EntityProperty> properties) {
        // TODO: should probably do something with this.......
    }

    public void propertyAdded(int entityID, String propertySheet,
        String propertyName) {

    }

    public void propertyRemoved(int entityID, String propertySheet,
        String propertyName) {

    }

    public void propertyUpdated(int entityID, String propertySheet,
        String propertyName, boolean ongoing) {
		
        if (entityID == activeViewpointEntity.getEntityID()) {
            if (propertyName.equals(ViewpointEntity.VIEW_MATRIX_PROP)) {

                float[] mtx = (float[])activeViewpointEntity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ViewpointEntity.VIEW_MATRIX_PROP);
				
                if (mtx != null) {
                    viewMatrix.set(mtx);
                    configMatrix = true;

                    if (navStatusManager != null) {
                        navStatusManager.fireViewMatrixChanged(viewMatrix);
                    }
                }
            } else if (propertyName.equals(ViewpointEntity.ORTHO_PARAMS_PROP)) {

                double[] frustum = (double[])activeViewpointEntity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ViewpointEntity.ORTHO_PARAMS_PROP);
				
                if (frustum != null) {
                    ViewEnvironment viewEnv = layerManager.getViewEnvironment();

                    viewEnv.setOrthoParams(frustum[0],
                                           frustum[1],
                                           frustum[2],
                                           frustum[3]);

                    double[] tmp = new double[6];
                    viewEnv.getViewFrustum(tmp);
                    if (navStatusManager != null) {
                        navStatusManager.fireViewportSizeChanged(tmp);
                    }
                }
            }
        }
    }

    //----------------------------------------------------------
    // Methods for EntityChildListener
    //----------------------------------------------------------

    /**
     * A child was added.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childAdded(int parentID, int childID) {

        ViewpointEntity ve = vce.getViewpoint(childID);

        if (ve != null) {
            String id = (String)ve.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ViewpointEntity.VIEW_IDENTIFIER_PROP);
            if (id.equals(viewId)) {
                ve.addEntitySelectionListener(this);
            }
        }
    }

    /**
     * A child was inserted.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     * @param index The index the child was inserted at
     */
    public void childInsertedAt(int parentID, int childID, int index) {

        ViewpointEntity ve = vce.getViewpoint(childID);

        if (ve != null) {
            String parent_viewId = (String)ve.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ViewpointEntity.VIEW_IDENTIFIER_PROP);
            if (viewId.equals(parent_viewId)) {
                ve.addEntitySelectionListener(this);
            }
        }
    }

    /**
     * A child was removed.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childRemoved(int parentID, int childID) {

        ViewpointEntity ve = vce.getViewpoint(childID);

        if (ve != null) {
            ve.removeEntitySelectionListener(this);
            if (ve == activeViewpointEntity) {
                ve.removeEntityPropertyListener(this);
            }
        }
    }

    // ---------------------------------------------------------------
    // Methods defined by EntitySelectionListener
    // ---------------------------------------------------------------

    /**
     * An entity has been selected
     *
     * @param entityID The entity which changed
     * @param selected Status of selecting
     */
    public void selectionChanged(int entityID, boolean selected) {

        ViewpointEntity ve = vce.getViewpoint(entityID);
        if (selected) {
            if (ve != null) {
                if (activeViewpointEntity != null) {
                    activeViewpointEntity.removeEntityPropertyListener(this);
                }
                activeViewpointEntity = ve;
                activeViewpointEntity.addEntityPropertyListener(this);
                configController();

                float[] mtx = (float[])activeViewpointEntity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ViewpointEntity.START_VIEW_MATRIX_PROP);

                if (mtx != null) {
                    viewMatrix.set(mtx);
                    configMatrix = true;

                    if (navStatusManager != null) {
                        navStatusManager.fireViewMatrixChanged(viewMatrix);
                    }
                }
				
				String proj_type_string = (String)activeViewpointEntity.getProperty(
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ViewpointEntity.PROJECTION_TYPE_PROP);
				
				int projectionType = ViewEnvironment.ORTHOGRAPHIC_PROJECTION;
				if (proj_type_string != null) {
					if (proj_type_string.equals(AV3DConstants.ORTHOGRAPHIC)) {
						projectionType = ViewEnvironment.ORTHOGRAPHIC_PROJECTION;
					} else if (proj_type_string.equals(AV3DConstants.PERSPECTIVE)) {
						projectionType = ViewEnvironment.PERSPECTIVE_PROJECTION;
					}
				}
			
                ViewEnvironment viewEnv = layerManager.getViewEnvironment();
				
				int currentProjectionType = viewEnv.getProjectionType();
                if (projectionType != currentProjectionType) {
					viewEnv.setProjectionType(projectionType);
					if (projectionType == ViewEnvironment.PERSPECTIVE_PROJECTION) {
						// set an arbitrary parameter that will force 
						// a recalculation of the frustum.
						viewEnv.setFieldOfView(viewEnv.getFieldOfView());
					}
				}
				
                if (projectionType == ViewEnvironment.ORTHOGRAPHIC_PROJECTION) {
                    double[] frustum = (double[])activeViewpointEntity.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ViewpointEntity.ORTHO_PARAMS_PROP);

                    if (frustum != null) {
                        viewEnv.setOrthoParams(frustum[0], frustum[1], frustum[2], frustum[3]);

                        double[] tmp = new double[6];
                        viewEnv.getViewFrustum(tmp);
                        if (navStatusManager != null) {
                            navStatusManager.fireViewportSizeChanged(tmp);
                        }
                    }
                }
            }
        }
    }

    /**
     * An entity has been highlighted
     *
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {
        if (configMatrix == true) {
            mgmtObserver.requestBoundsUpdate(data.viewpointTransform, this);
        }
    }

    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {
        if (src == data.viewpointTransform) {
            if (configMatrix) {
                data.viewpointTransform.setTransform(viewMatrix);
                configMatrix = false;
            }
        }
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        this.reporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

	/**
	 * Return whether navigation is in progress
	 *
	 * @return whether navigation is in progress
	 */
	boolean isActive() {
		return(isActive);
	}
	
    /**
     * Get the current viewpoint matrix. If the supplied matrix is null, ignore
     * the request
     *
     * @param mat A matrix to copy the viewpoint matrix in to
     */
    void getViewMatrix(Matrix4f mat) {
        if(mat == null)
            return;

        mat.set(viewMatrix);
    }

    /**
     * Set the viewpoint container
     *
     * @param entity The ViewpointContainerEntity
     */
    void setViewpointContainerEntity(ViewpointContainerEntity vce) {

        if (this.vce != null) {
            // cleanup any listeners that have been set on the retiring
            // viewpoint entities.
            List<ViewpointEntity> vp_list = this.vce.getViewpoints();
            for (Iterator<ViewpointEntity> i = vp_list.iterator(); i.hasNext();) {
                ViewpointEntity ve = i.next();
                ve.removeEntitySelectionListener(this);
                if (ve == activeViewpointEntity) {
                    ve.removeEntityPropertyListener(this);
                }
            }
            this.vce.removeEntityChildListener(this);
        }
        this.vce = vce;
        activeViewpointEntity = null;
        if (this.vce != null) {

            this.vce.addEntityChildListener(this);

            if (this.vce.hasChildren()) {
                // initialize listeners on the new viewpoints
                boolean defaultFound = false;
                List<ViewpointEntity> vp_list = this.vce.getViewpoints();
                for (Iterator<ViewpointEntity> i = vp_list.iterator(); i.hasNext();) {

                    ViewpointEntity ve = i.next();
                    String parent_viewId = (String)ve.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ViewpointEntity.VIEW_IDENTIFIER_PROP);

                    if (viewId.equals(parent_viewId)) {

                        ve.addEntitySelectionListener(this);
                        String name = (String)ve.getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.NAME_PROP);

                        if (name.equals(AV3DConstants.DEFAULT_VIEWPOINT_NAME)) {
                            if (!defaultFound) {
                                activeViewpointEntity = ve;
                                activeViewpointEntity.addEntityPropertyListener(this);
                                defaultFound = true;

                                configController();

                                // we need to select then unselect viewpoints, that way
                                // they don't interfere with anything else.
                                ArrayList<Command> commandList = new ArrayList<Command>();

                                Command cmd = new SelectEntityCommand(model, ve, true);
                                commandList.add(cmd);

                                cmd = new SelectEntityCommand(model, ve, false);
                                commandList.add(cmd);

                                // Create the MultiCommand and send it
                                MultiCommand stack = new MultiCommand(
                                  commandList,
                                  "Select Viewpoint -> " +  ve.getEntityID(),
                                  false);

                                stack.setErrorReporter(reporter);
                                commandController.execute(stack);

                            } else {
                                // there can be only one!
                                // exterminate the offender!
                                Command cmd = new RemoveEntityChildCommand(
                                    model,
                                    vce,
                                    ve,
                                    false);
                                commandController.execute(cmd);
                            }
                        }
                    }
                }
            }
            if (activeViewpointEntity == null) {
                // if there are no viewpoints for this manager, create a default
                createDefault();
            }
        }
    }

    /**
     * Common method for configuring navigation classes
     */
    private void configController() {

        if ((data != null) && (activeMode != null) &&
            (activeViewpointEntity != null)) {

            if (controller != null) {
                centerOfRotation =
                    controller.getCenterOfRotation(centerOfRotation);
            }

            ViewEnvironment viewEnv = layerManager.getViewEnvironment();

            int projectionType = viewEnv.getProjectionType();
            boolean isOrthographic =
                (projectionType == ViewEnvironment.ORTHOGRAPHIC_PROJECTION);

            switch (activeMode) {
                case EXAMINE:
                    controller = new HalfGlobeNavigationModeController(
                        data,
						rootGroup,
						navigationTransform,
                        commandController,
                        activeViewpointEntity,
                        collisionManager,
                        navStatusManager);
                    break;
                case PAN:
                    controller = new PanNavigationModeController(
                        data,
                        commandController,
                        activeViewpointEntity,
                        collisionManager,
                        navStatusManager);
                    break;
                case ZOOM:
                    if (isOrthographic) {
                        controller = new OrthoZoomNavigationModeController(
                            data,
                            viewEnv,
                            commandController,
                            activeViewpointEntity,
                            navStatusManager);
                    } else {
                        controller = new ZoomNavigationModeController(
                            data,
                            commandController,
                            activeViewpointEntity,
                            collisionManager,
                            navStatusManager);
                    }
                    break;
                case PANZOOM:
                    if (isOrthographic) {
                        controller = new OrthoPanZoomNavigationModeController(
                            data,
                            viewEnv,
                            commandController,
                            activeViewpointEntity,
                            collisionManager,
                            navStatusManager);
                    } else {
                        controller = new PanZoomNavigationModeController(
                            data,
                            commandController,
                            activeViewpointEntity,
                            collisionManager,
                            navStatusManager);
                    }
                    break;

                case NONE:
                default:
                    controller = null;
            }

            if (controller != null) {
                controller.setCenterOfRotation(centerOfRotation);
            }
        }
    }

    /**
     * Create a default ViewpointEntity
     */
    private void createDefault() {
        HashMap<String, Map<String, Object>> vpProps =
            new HashMap<String, Map<String, Object>>();
        HashMap<String, Object> vpParams = new HashMap<String, Object>();

        HashMap<String, Object> vpPropSheet = new HashMap<String, Object>();
        vpPropSheet.put(Entity.NAME_PROP, AV3DConstants.DEFAULT_VIEWPOINT_NAME);
        float[] view_mtx = new float[16];
        AV3DUtils.toArray(defaultViewMatrix, view_mtx);
        vpPropSheet.put(ViewpointEntity.START_VIEW_MATRIX_PROP, view_mtx);

        ViewEnvironment viewEnv = layerManager.getViewEnvironment();
        int projectionType = viewEnv.getProjectionType();
        boolean isOrthographic =
            (projectionType == ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        if (isOrthographic) {
            vpPropSheet.put(
                ViewpointEntity.PROJECTION_TYPE_PROP,
                AV3DConstants.ORTHOGRAPHIC);
        } else {
            vpPropSheet.put(
                ViewpointEntity.PROJECTION_TYPE_PROP,
                AV3DConstants.PERSPECTIVE);
        }
        vpPropSheet.put(ViewpointEntity.VIEW_IDENTIFIER_PROP, viewId);

        vpProps.put(Entity.ENTITY_PARAMS, vpParams);
        vpProps.put(Entity.DEFAULT_ENTITY_PROPERTIES, vpPropSheet);

        ViewpointEntity ve = new ViewpointEntity(
            model.issueEntityID(),
            vpProps);

        //////////////////////////////////////////////
        // initialize the entity directly....

        activeViewpointEntity = ve;
        activeViewpointEntity.addEntityPropertyListener(this);
        vce.addViewpoint(activeViewpointEntity);
        configController();

        if (!isOrthographic) {

            // we need to select then unselect viewpoints, that way
            // they don't interfere with anything else.
            ArrayList<Command> commandList = new ArrayList<Command>();

            Command cmd = new SelectEntityCommand(model, ve, true);
            commandList.add(cmd);

            cmd = new SelectEntityCommand(model, ve, false);
            commandList.add(cmd);

            // Create the MultiCommand and send it
            MultiCommand stack = new MultiCommand(
              commandList,
              "Select Viewpoint -> " +  ve.getEntityID(),
              false);

            stack.setErrorReporter(reporter);
            commandController.execute(stack);

        } else {
            viewMatrix.set(defaultViewMatrix);
            configMatrix = true;
        }
        //////////////////////////////////////////////
        //
        //
        //Command cmd = new AddEntityChildCommand(this.vce, ve);
        //commandList.add(cmd);
        //
        //cmd = new SelectEntityCommand(ve, true);
        //commandList.add(cmd);
        //
        // Create the MultiCommand and send it
        //MultiCommand stack = new MultiCommand(
        //  commandList,
        //  "Add Entity -> " +  ve.getEntityID());
        //
        //stack.setErrorReporter(reporter);
        //commandController.execute(stack);
        //////////////////////////////////////////////
    }
}
