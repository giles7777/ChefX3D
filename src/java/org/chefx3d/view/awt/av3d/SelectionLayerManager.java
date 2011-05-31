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
import java.util.HashMap;
import java.util.Iterator;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.LineStripArray;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.view.awt.scenemanager.*;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.ApplicationParams;

import org.chefx3d.view.awt.av3d.AV3DConstants.ActionMode;

import org.chefx3d.view.common.EditorConstants;
import org.chefx3d.view.common.EditorGrid;
import org.chefx3d.view.common.StatusReporter;

/**
 * Implementation of the selection LayerManager. Handles the creation,
 * add and remove of the selection box object as well as the responses
 * for anchor events.
 *
 * @author Rex Melton
 * @version $Revision: 1.40 $
 */
class SelectionLayerManager extends AbstractLayerManager implements
    AV3DConstants,
    EditorConstants,
    NavigationStatusListener,
    NodeUpdateListener,
    PerFrameObserver,
    PerFrameUIObserver,
    UserInputHandler,
    StatusReporter,
    ModelListener,
    EntityChildListener,
    EntitySelectionListener,
    ResizeListener,
	ConfigListener {

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /** The world model */
    private WorldModel model;

    /** The command queue */
    protected CommandController controller;

    /** The device manager */
    private DeviceManager deviceManager;

    /** The cursor manager */
    private AV3DCursorManager cursorManager;

    /** The navigation status manager */
    private NavigationStatusManager navStatusManager;

    /** The PickManager */
    private PickManager pickManager;

    /** Container of editor state variables */
    private LocationEditorState editorState;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper selectionHelper;

    /** The active zone Entity */
    private ZoneEntity activeZoneEntity;

    /** Scratch vecmath objects */
    private Matrix4f mtx0;
    private Point3f position;
    private Matrix4f viewMatrix;
    private Vector3f translation;

    /** Viewport dimensions */
    private int viewport_width;
    private int viewport_height;

    /** Instance of hierarchy transformation calculator */
    private TransformUtils tu;

    /** The manager of the entities to be handled */
    private AV3DEntityManager entityManager;

    /** The map of entity wrappers */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;

    /** Our ViewEnvironment */
    private ViewEnvironment viewEnv;

    /** view frustum values */
    private double[] frustum;

    /** State flag indicating that the view transform must be updated */
    private boolean configMatrix;

    /** Array list of nodes to add to the scene */
    private ArrayList<AV3DSelectionBox> nodeAddList;

    /** Array list of nodes to remove from the scene */
    private ArrayList<AV3DSelectionBox> nodeRemoveList;

    /** Map of all entities in the scene, keyed by id */
    private HashMap<Integer, Entity> entityMap;

    /** Map of selection boxes, keyed by entity id */
    private HashMap<Integer, AV3DSelectionBox> selectionMap;

    /** Identifier of the active mouse button */
    private int activeButton;

    /** Flag indicating that a drag operation has been initialized */
    private boolean dragInProgress;

    /** Flag indicating that a press operation has been processed this frame */
    private boolean pressProcessed;

    /** Flag indicating that a release operation has been processed this frame */
    private boolean releaseProcessed;

    /** The selected entity state at the last mouse press */
    private ActionData initialActionData;

    /** The anchor that has been selected with a mouse press */
    private AV3DAnchorInformation selectedAnchor;

    /** Stores the list of activeAnchors*/
    private boolean[] activeAnchors;

    /////////////////////////////////////////////////////////////////////
    // Event responders

    /** Handles the removal of Entities */
    private RemoveEntityResponse removeEntityResponse;

    /** Handles all scaling for segment entities */
    private SegmentEntityScaleResponse segmentEntityScaleResponse;

	/** Handles all transient scaling for segment entities */
    private SegmentEntityScaleTransientResponse
        segmentEntityScaleTransientResponse;

    /** Handles transient rotation events for default entities */
    private DefaultEntityRotationTransientResponse
        defaultEntityRotateTransientResponse;

    /** Handles transient rotation events for default entities */
    private DefaultEntityRotationResponse
        defaultEntityRotateResponse;

    /** Handles scale events for default entities */
    private DefaultEntityScaleResponse scaleResponse;

    /** Handles transient scale events for default entities */
    private DefaultEntityScaleTransientResponse scaleTransientResponse;

    /////////////////////////////////////////////////////////////////////

    /**
     * Constructor
     *
     * @param id The layer id
     * @param dim The initial viewport dimensions in [x, y, width, height]
     * @param model The WorldModel
     * @param controller The CommandController
     * @param reporter The ErrorReporter instance to use or null
     * @param mgmtObserver The SceneManagerObserver
     * @param deviceManager The DeviceManager
     * @param cursorManager The CursorManager
     * @param navStatusManager The NavigationStatusManager
     */
    SelectionLayerManager(
        int id,
        int[] dim,
        WorldModel model,
        CommandController controller,
        ErrorReporter reporter,
        SceneManagerObserver mgmtObserver,
        DeviceManager deviceManager,
        AV3DCursorManager cursorManager,
        NavigationStatusManager navStatusManager ) {

        super(id, dim);
        viewport_width = dim[2];
        viewport_height = dim[3];
        setErrorReporter(reporter);

        selectionHelper =
            EntitySelectionHelper.getEntitySelectionHelper();

        this.model = model;
        this.controller = controller;
        this.deviceManager = deviceManager;
        this.cursorManager = cursorManager;

        this.navStatusManager = navStatusManager;
        this.navStatusManager.addNavigationStatusListener(this);

        this.mgmtObserver = mgmtObserver;
        mgmtObserver.addObserver(this);
        mgmtObserver.addUIObserver(this);

        pickManager = new PickManager(model, rootGroup);
        editorState = LocationEditorState.getInstance();

        initialActionData = new ActionData();
        initialActionData.zoneOri = ZoneOrientation.VERTICAL;
        initialActionData.model = model;

        tu = new TransformUtils();
        mtx0 = new Matrix4f();
        viewMatrix = new Matrix4f();
        translation = new Vector3f();
        position = new Point3f();

        viewEnv = scene.getViewEnvironment();
        viewEnv.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        viewEnv.setAspectRatio(0);
        viewEnv.setClipDistance(0.001, 1000.0);

        frustum = new double[6];

        // the default viewpoint
        translation.set(0, 0, 10);
        viewMatrix.setIdentity();
        viewMatrix.setTranslation(translation);
        viewpointTransform.setTransform(viewMatrix);

        nodeAddList = new ArrayList<AV3DSelectionBox>();
        nodeRemoveList = new ArrayList<AV3DSelectionBox>();

        entityMap = new HashMap<Integer, Entity>();

        selectionMap = new HashMap<Integer, AV3DSelectionBox>();

        activeAnchors = new boolean[]{true, true, true, true, true,
                                    true, true, true, true, true};

		EditorGrid editorGrid = new EditorGrid();
		
        ////////////////////////////////////////////////////////////////
        // event responders

        removeEntityResponse =
            new RemoveEntityResponse(model, reporter);

        scaleResponse =
            new DefaultEntityScaleResponse(model, controller, reporter, viewEnv, editorGrid);

        scaleTransientResponse =
            new DefaultEntityScaleTransientResponse(model, controller, reporter, viewEnv, editorGrid);

        segmentEntityScaleResponse =
            new SegmentEntityScaleResponse(model, controller, reporter, viewEnv, editorGrid);

        segmentEntityScaleTransientResponse =
            new SegmentEntityScaleTransientResponse(model, controller, reporter, viewEnv, editorGrid);

        defaultEntityRotateTransientResponse =
            new DefaultEntityRotationTransientResponse(model, controller, reporter);

        defaultEntityRotateResponse =
            new DefaultEntityRotationResponse(model, controller, reporter);

        ////////////////////////////////////////////////////////////////

        model.addModelListener(this);

        Boolean debug = (Boolean)ApplicationParams.get(ApplicationParams.DEBUG_MODE);
        if (debug != null && debug == true) {
            init();
        }

    }

    //----------------------------------------------------------
    // Methods defined by NavigationStatusListener
    //----------------------------------------------------------

    /**
     * Notification that the orthographic viewport size has changed.
     *
     * @param frustumCoords The new coordinates to use in world space
     */
    public void viewportSizeChanged(double[] frustumCoords) {

        // use the location layer's ortho params
        viewEnv.setOrthoParams(frustumCoords[0],
                               frustumCoords[1],
                               frustumCoords[2],
                               frustumCoords[3]);
		
		frustum[0] = frustumCoords[0];
        frustum[1] = frustumCoords[1];
        frustum[2] = frustumCoords[2];
        frustum[3] = frustumCoords[3];
		
        setAnchorScale();
    }

    /**
     * Notification that the view Transform has changed
     *
     * @param mtx The new view Transform matrix
     */
    public void viewMatrixChanged(Matrix4f mtx) {

        if ((wrapperMap != null) && (activeZoneEntity != null)) {
            AV3DEntityWrapper wrapper =
                wrapperMap.get(activeZoneEntity.getEntityID());

            if (wrapper != null) {

                // convert the location layer's viewpoint from world
                // to local coordinates
                tu.getLocalToVworld(wrapper.transformGroup, mtx0);
                mtx0.invert();

                mtx.get(translation);
                position.set(translation);

                // maintain a fixed distance from the geometry
                mtx0.transform(position);
                translation.set(position);
                translation.z = 10;

                viewMatrix.setIdentity();
                viewMatrix.setTranslation(translation);

                configMatrix = true;
            }
        }
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {
        if (configMatrix) {
            mgmtObserver.requestBoundsUpdate(viewpointTransform, this);
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

        if (src == viewpointTransform) {
            if (configMatrix) {
                viewpointTransform.setTransform(viewMatrix);
                configMatrix = false;
            }
        } else if (src == rootGroup) {

            int numToRemove = nodeRemoveList.size();
            if (numToRemove > 0) {
                for(int i = 0; i < numToRemove; i++) {
                    AV3DSelectionBox sb = nodeRemoveList.get(i);
                    rootGroup.removeChild(sb.switchGroup);
                }
                nodeRemoveList.clear();
            }

            int numToAdd = nodeAddList.size();
            if (numToAdd > 0) {
                for(int i = 0; i < numToAdd; i++) {
                    AV3DSelectionBox sb = nodeAddList.get(i);
                    rootGroup.addChild(sb.switchGroup);
                }
                nodeAddList.clear();
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
    // Methods defined by PerFrameUIObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed
     */
    public void processNextFrameUI() {

        // clear any previous pick results
        pickManager.reset();

        // determine the ui state
        pressProcessed = false;
        releaseProcessed = false;
        deviceManager.processTrackers(id, this);
    }

    //---------------------------------------------------------------
    // Methods defined by UserInputHandler
    //---------------------------------------------------------------

    /**
     * Process a tracker press event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerPressed(int tracker, TrackerState evt) {

        if (pressProcessed) {
            // don't do this twice in one frame
            return;
        } else {
            pressProcessed = true;
        }

		if (dragInProgress) {
			// ignore extraneous presses during a drag
			return;
		}

        for (int i = 0; i < evt.numButtons; i++) {
            if (evt.buttonState[i]) {
                activeButton = i;
				break;
            }
        }

        if (activeButton == 0) {
            if (editorState.currentMode == EditorMode.SELECTION) {
                if (pickManager.doPick(evt)) {

                    PickData pd = pickManager.getResult();

                    if (pd.object instanceof AV3DAnchorInformation) {

                        selectedAnchor = (AV3DAnchorInformation)pd.object;
                        AnchorData ad = selectedAnchor.getAnchorDataFlag();
                        cursorManager.setCursorMode(ad);

                        /*
                        ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

                        Entity entity = null;
                        // TODO: use the first selected entity for now
                        if (selectedList.size() > 0) {
                            entity = selectedList.get(0);
                        }
                        */
                        Entity entity = selectedAnchor.getEntity();

                        if (entity != null) {
                            if (entity instanceof PositionableEntity) {
                                PositionableEntity pe = (PositionableEntity)entity;
                                initialActionData.setMouseDevicePosition(evt.devicePos);
                                initialActionData.setMouseWorldPosition(evt.worldPos);
                                initialActionData.setEntity(pe);
                            }
                            if (entity instanceof SegmentEntity) {

                                // Get the wall thickness
                                Object prop = entity.getProperty(
                                        Entity.EDITABLE_PROPERTIES,
                                        SegmentEntity.WALL_THICKNESS_PROP);

                                initialActionData.thickness = SegmentEntity.DEFAULT_WALL_THICKNESS;
                                if (prop instanceof ListProperty) {
                                    ListProperty list = (ListProperty)prop;
                                    initialActionData.thickness = Float.parseFloat(list.getSelectedValue());
                                }

                            }
                        }
                        editorState.currentMode = EditorMode.ANCHOR_TRANSFORM;
                    }
                }
            }
        }
    }

    /**
     * Process a tracker move event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerMoved(int tracker, TrackerState evt) {

        if (pickManager.doPick(evt)) {

            PickData pd = pickManager.getResult();

            if (pd.object instanceof AV3DAnchorInformation) {
                editorState.mouseOverAnchor = (AV3DAnchorInformation)pd.object;
            } else {
                editorState.mouseOverAnchor = null;
            }
        } else {
            editorState.mouseOverAnchor = null;
        }
    }

    /**
     * Process a tracker press event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerDragged(int tracker, TrackerState evt) {

        if (activeButton == 0) {
            if (editorState.currentMode == EditorMode.ANCHOR_TRANSFORM) {
                if (selectedAnchor != null) {
                    /*
                    ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

                    Entity entity = null;
                    // TODO: use the first selected entity for now
                    if (selectedList.size() > 0) {
                        entity = selectedList.get(0);
                    }
                    */
                    Entity entity = selectedAnchor.getEntity();
                    Entity[] entityArr = new Entity[] {entity};

                    // Handle anchor events
                    switch (selectedAnchor.getAnchorDataFlag()) {

                    case NORTH:
                    case SOUTH:
                    case EAST:
                    case WEST:

                        if (entity.getType() == Entity.TYPE_SEGMENT) {
                            if (!dragInProgress) {
                                segmentEntityScaleTransientResponse.setActionData(
                                    initialActionData);

                                segmentEntityScaleTransientResponse.setAnchor(
                                    selectedAnchor);
                            }
                            segmentEntityScaleTransientResponse.doEventResponse(
                                tracker,
                                evt,
                                entityArr,
                                null);
                        } else {
                            if (!dragInProgress) {
								scaleTransientResponse.setActionData(initialActionData);
                                scaleTransientResponse.setAnchor(selectedAnchor);
                            }
							scaleTransientResponse.doEventResponse(
                                tracker,
                                evt,
                                entityArr,
                                null);
                        }
                        break;

                    case NORTHEAST:
                    case NORTHWEST:
                    case SOUTHEAST:
                    case SOUTHWEST:

                        if (!dragInProgress) {
							scaleTransientResponse.setActionData(initialActionData);
                            scaleTransientResponse.setAnchor(selectedAnchor);
                        }
						scaleTransientResponse.doEventResponse(
                            tracker,
                            evt,
                            entityArr,
                            null);

                        break;

                    case ROTATE:

                        if (!dragInProgress) {
                            defaultEntityRotateTransientResponse.setActionData(
                                initialActionData);

                            defaultEntityRotateTransientResponse.setAnchor(
                                selectedAnchor);
                        }
                        defaultEntityRotateTransientResponse.doEventResponse(
                            tracker,
                            evt,
                            entityArr,
                            null);

                        break;
                    }
                }
            }
        	dragInProgress = true;
        }
    }

    /**
     * Process a tracker release event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerReleased(int tracker, TrackerState evt) {
        dragInProgress = false;

        if (releaseProcessed) {
            // don't do this twice in one frame
            return;
        } else {
            releaseProcessed = true;
        }

        if (activeButton == 0) {
            if (editorState.currentMode == EditorMode.ANCHOR_TRANSFORM) {
                if (selectedAnchor != null) {
                    /*
                    ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

                    Entity entity = null;
                    // TODO: use the first selected entity for now
                    if (selectedList.size() > 0) {
                        entity = selectedList.get(0);
                    }
                    */
                    Entity entity = selectedAnchor.getEntity();
                    Entity[] entityArr = new Entity[] {entity};

                    switch (selectedAnchor.getAnchorDataFlag()) {

                    case NORTH:
                    case SOUTH:
                    case EAST:
                    case WEST:
                        if (entity.getType() == Entity.TYPE_SEGMENT) {

                            segmentEntityScaleResponse.setActionData(
                                initialActionData);

                            segmentEntityScaleResponse.setAnchor(
                                selectedAnchor);

                            segmentEntityScaleResponse.doEventResponse(
                                tracker, evt, entityArr, null);
                        } else {
							scaleResponse.setActionData(initialActionData);
                            scaleResponse.setAnchor(selectedAnchor);

							scaleResponse.doEventResponse(
                                tracker, evt, entityArr, null);
                        }
                        break;

                    case NORTHEAST:
                    case NORTHWEST:
                    case SOUTHEAST:
                    case SOUTHWEST:
						scaleResponse.setActionData(initialActionData);
                        scaleResponse.setAnchor(selectedAnchor);

						scaleResponse.doEventResponse(
                            tracker, evt, entityArr, null);
                        break;

                    case ROTATE:
                        defaultEntityRotateResponse.setActionData(
                            initialActionData);

                        defaultEntityRotateResponse.setAnchor(
                            selectedAnchor);

                        defaultEntityRotateResponse.doEventResponse(
                            tracker, evt, entityArr, null);
                        break;

                    case DELETE:
                        removeEntityResponse.doEventResponse(
                            tracker, evt, entityArr, null);
                        break;

                    }
                    selectedAnchor = null;
                }
            }
        }

        cursorManager.setCursorMode(ActionMode.NONE);
        releaseProcessed = true;
    }

    /**
     * Process a tracker click event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerClicked(int tracker, TrackerState evt) {
        trackerPressed(tracker, evt);
        trackerReleased(tracker, evt);
    }

    /**
     * Process a tracker orientation event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerOrientation(int tracker, TrackerState evt) {
    }

    /**
     * Process the buttons on a tracker.
     *
     * @param tracker The id of the tracker calling this handler
     * @param state The current state.
     */
    public void trackerButton(int tracker, TrackerState state) {
    }

    /**
     * Process the wheel on a tracker.
     *
     * @param tracker The id of the tracker calling this handler
     * @param state The current state.
     */
    public void trackerWheel(int tracker, TrackerState state) {
    }

    //------------------------------------------------------------------------
    // Methods required by ResizeListener
    //------------------------------------------------------------------------

    /**
     * window size has been updated
     */
    public void sizeChanged(int newWidth, int newHeight) {

        viewport_width = newWidth;
        viewport_height = newHeight;

        setAnchorScale();
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

        Entity parentEntity = entityMap.get(parentID);
        int index = parentEntity.getChildIndex(childID);

        childInsertedAt(parentID, childID, index);
    }

    /**
     * A child was inserted.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     * @param index The index the child was inserted at
     */
    public void childInsertedAt(int parentID, int childID, int index) {

        Entity parentEntity = entityMap.get(parentID);
        Entity childEntity = parentEntity.getChildAt(index);

        recursiveAdd(childEntity);
    }

    /**
     * A child was removed.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childRemoved(int parentID, int childID) {

        Entity childEntity = entityMap.get(childID);

        if (childEntity != null) {
            recursiveRemove(childEntity);
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
		
        Entity entity = entityMap.get(entityID);
        if (entity == null) {
            return;
        }

        if (wrapperMap != null) {

            AV3DEntityWrapper wrapper = wrapperMap.get(entityID);
            if (wrapper != null) {

                if (!selected) {

                    remove(entityID);

                } else {

                    int type = entity.getType();
                    switch(type) {
                        case Entity.TYPE_VERTEX:
                        case Entity.TYPE_SEGMENT:
                        case Entity.TYPE_MODEL:
                        case Entity.TYPE_MODEL_WITH_ZONES:

                            add(wrapper);
                    }
                }
            }
        }
    }

    /**
     * Ignored
     */
    public void highlightChanged(int entityID, boolean highlighted) {
    }

    //----------------------------------------------------------
    // Methods required by ModelListener
    //----------------------------------------------------------

    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity
     */
    public void entityAdded(boolean local, Entity entity){

        recursiveAdd(entity);
    }

    /**
     * An entity was removed.
     *
     * @param local Is the request local
     * @param entity The entity to remove
     */
    public void entityRemoved(boolean local, Entity entity) {

        recursiveRemove(entity);
    }

    /**
     * Ignored
     */
    public void modelReset(boolean local) {
    }

    /**
     * Ignored
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
    }

    /**
     * Ignored
     */
    public void masterChanged(boolean local, long viewID) {
    }

    //------------------------------------------------------------------------
    // Methods required by StatusReporter
    //------------------------------------------------------------------------

    /**
     * Set the selection status condition
     *
     * @param status The indicator of selection status. A color
     * in float array form [r, g, b].
     */
    public void setSelectionStatus(float[] status) {

        ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

        for (int i = 0; i < selectedList.size(); i++) {

            Entity entity = selectedList.get(i);

            if ((entity != null) && (wrapperMap != null)) {
                AV3DEntityWrapper wrapper =
                    wrapperMap.get(entity.getEntityID());

                if (wrapper != null) {

                    AV3DSelectionBox sb = get(wrapper);
                    if (sb != null) {
                        sb.setColor(status);
                    }
                }
            }
        }
    }

    //----------------------------------------------------------
    // Methods defined by ConfigListener
    //----------------------------------------------------------

    /**
     * Set the active entity manager
     *
     * @param entityManager The active entity manager
     */
    public void setEntityManager(AV3DEntityManager entityManager) {
        this.entityManager = entityManager;
        if (entityManager != null) {
            wrapperMap = entityManager.getAV3DEntityWrapperMap();
            initialActionData.wrapperMap = wrapperMap;
        } else {
            wrapperMap = null;
            initialActionData.wrapperMap = null;
        }
    }

    /**
     * Set the active zone entity, this will also cause all
     * selection boxes to be cleared from the scene
     *
     * @param ze The active zone entity
     */
    public void setActiveZoneEntity(ZoneEntity ze) {
		
        activeZoneEntity = ze;
        if (activeZoneEntity != null) {

            AV3DEntityWrapper zoneWrapper =
                wrapperMap.get(activeZoneEntity.getEntityID());
            initialActionData.zoneWrapper = zoneWrapper;

        } else {

            initialActionData.zoneWrapper = null;
        }
        
        // TODO: we really shouldn't be managing the data model from this UI
        // component.  ideally this would get issued by whatever code is
        // requesting the zone update.  but for some reason issuing this
        // from the rules space does not act the same is when it is here.
        EntitySelectionHelper.getEntitySelectionHelper().clearSelectedList();
        //clear();
        
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Get the selected anchor data identifying which anchor
     * is selected. Either NORTH, SOUTH, SOUTHWEST etc.
     * 
     * @return AV3DConstants.AnchorData identifier for selected anchor, or
     * null if no anchor is selected
     */
    public EditorConstants.AnchorData getSelectedAnchorData() {
    	
    	if (selectedAnchor == null) {
    		return null;
    	}
    	
    	return selectedAnchor.getAnchorDataFlag();
    }
    
    /**
     * Sets the anchor boxes that are visible on the selection box
     *
     * @param entity The entity whose selection box is to be configured
     * @param anchorFlags
     */
    void setSelectionAnchors(Entity entity, boolean[] anchorFlags) {

        this.activeAnchors = anchorFlags;

        AV3DSelectionBox sb = selectionMap.get(entity.getEntityID());
        if (sb != null) {
            sb.setActiveAnchors(anchorFlags);
        }
    }

    /**
     * Return the UserInputHandler for this layer
     *
     * @return The UserInputHandler
     */
    UserInputHandler getUserInputHandler() {
        return this;
    }

    /**
     * Create and add a selection box to the scene if one
     * does not exist. Return the selection box for the
     * argument wrapper.
     *
     * @param wrapper The entity wrapper to add
     * @return The selection box for the argument wrapper
     */
    private AV3DSelectionBox add(AV3DEntityWrapper wrapper) {

        AV3DSelectionBox sb = null;

        int entityID = wrapper.entity.getEntityID();
        if (selectionMap.containsKey(entityID)) {

            sb = selectionMap.get(entityID);

        } else {
           AV3DEntityWrapper parentWrapper =
                wrapperMap.get(wrapper.entity.getParentEntityID());

            AV3DEntityWrapper zoneWrapper =
                wrapperMap.get(activeZoneEntity.getEntityID());

            float w_ratio = (float)(viewport_width / (frustum[1] - frustum[0]));

            sb = new AV3DSelectionBox(
                mgmtObserver,
                wrapper,
                parentWrapper,
                zoneWrapper,
                w_ratio,
                activeAnchors);

            selectionMap.put(entityID, sb);

            nodeAddList.add(sb);
            mgmtObserver.requestBoundsUpdate(rootGroup, this);
        }
        return(sb);
    }

    /**
     * Remove a selection box from the scene
     *
     * @param wrapper The entity wrapper to remove
     */
    private void remove(int entityID) {
		
        if (selectionMap.containsKey(entityID)) {

            AV3DSelectionBox sb = selectionMap.remove(entityID);
            sb.dispose();

            if (nodeAddList.contains(sb)) {
                nodeAddList.remove(sb);
            } else {
                nodeRemoveList.add(sb);
                mgmtObserver.requestBoundsUpdate(rootGroup, this);
            }
        }
    }

    /**
     * Return the selection box associated with an entity
     *
     * @return The selection box
     */
    private AV3DSelectionBox get(AV3DEntityWrapper wrapper) {

        return(selectionMap.get(wrapper.entity.getEntityID()));
    }

    /**
     * Return the number of active selection boxes
     *
     * @return The number of active selection boxes
     */
    private int getNumSelected() {

        return(selectionMap.size());
    }

    /**
     * Return the map of active selection boxes
     *
     * @return The map of active selection boxes
     */
    private HashMap<Integer, AV3DSelectionBox> getSelectionMap() {

        return(selectionMap);
    }

    /**
     * Remove all selection boxes from the scene
     */
    private void clear() {
        if (!selectionMap.isEmpty()) {
            for (Iterator<AV3DSelectionBox> i = selectionMap.values().iterator(); i.hasNext();) {

                AV3DSelectionBox sb = i.next();
                sb.dispose();
                //nodeRemoveList.add(sb);
				
				if (nodeAddList.contains(sb)) {
					nodeAddList.remove(sb);
				} else {
					nodeRemoveList.add(sb);
				}
            }
            mgmtObserver.requestBoundsUpdate(rootGroup, this);

            selectionMap.clear();
        }
    }

    /**
     * Calculate the scale factor for the viewport and
     * push to any active SelectionBoxes
     */
    private void setAnchorScale() {
        if (!selectionMap.isEmpty()) {

            double f_width = frustum[1] - frustum[0];
            double f_height = frustum[3] - frustum[2];

            double w_ratio = viewport_width / f_width;
            double h_ratio = viewport_height / f_height;

            for (Iterator<AV3DSelectionBox> i = selectionMap.values().iterator();
                i.hasNext();) {

                AV3DSelectionBox sb = i.next();
                // pick one, both ratios 'should' be the same
                sb.setScale((float)w_ratio);
            }
        }
    }

    /**
     * Add crosshairs along the x-y axis
     */
    private void init() {

        float[] x_axis_coord = new float[]{
            -100, 0, 0,
            100, 0, 0};

        LineStripArray x_axis = new LineStripArray();
        x_axis.setVertices(LineStripArray.COORDINATE_3, x_axis_coord);
        x_axis.setStripCount(new int[]{2}, 1);
        x_axis.setSingleColor(false, new float[]{1, 0, 0});

        Shape3D x_axis_shape = new Shape3D();
        x_axis_shape.setPickMask(0);
        x_axis_shape.setGeometry(x_axis);

        float[] y_axis_coord = new float[]{
            0, 100, 0,
            0, -100, 0};

        LineStripArray y_axis = new LineStripArray();
        y_axis.setVertices(LineStripArray.COORDINATE_3, y_axis_coord);
        y_axis.setStripCount(new int[]{2}, 1);
        y_axis.setSingleColor(false, new float[]{0, 1, 0});

        Shape3D y_axis_shape = new Shape3D();
        y_axis_shape.setPickMask(0);
        y_axis_shape.setGeometry(y_axis);

        float[] z_axis_coord = new float[]{
            0, 0, -100,
            0, 0, 100};

        LineStripArray z_axis = new LineStripArray();
        z_axis.setVertices(LineStripArray.COORDINATE_3, z_axis_coord);
        z_axis.setStripCount(new int[]{2}, 1);
        z_axis.setSingleColor(false, new float[]{0, 0, 1});

        Shape3D z_axis_shape = new Shape3D();
        z_axis_shape.setPickMask(0);
        z_axis_shape.setGeometry(z_axis);

        rootGroup.addChild(x_axis_shape);
        rootGroup.addChild(y_axis_shape);
        rootGroup.addChild(z_axis_shape);

        rootGroup.setPickMask(TransformGroup.GENERAL_OBJECT);
    }

    /**
     * Walk through the children of the argument entity,
     * adding listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveAdd(Entity entity) {

        if (entity instanceof ViewpointContainerEntity) {

            return;
        }

        entity.addEntityChildListener(this);
        entity.addEntitySelectionListener(this);

        entityMap.put(entity.getEntityID(), entity);

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveAdd(child);
            }
        }
    }

    /**
     * Walk through the children of the argument entity,
     * removing listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveRemove(Entity entity) {
		
        entity.removeEntityChildListener(this);
        entity.removeEntitySelectionListener(this);

        int entityID = entity.getEntityID();
        remove(entityID);
        entityMap.remove(entityID);

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveRemove(child);
            }
        }
    }
}
