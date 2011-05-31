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
import java.awt.Color;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.vecmath.*;

import org.j3d.aviatrix3d.*;

import org.j3d.device.input.TrackerState;

import org.j3d.util.I18nManager;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.SegmentTool;
import org.chefx3d.tool.SegmentableTool;
import org.chefx3d.tool.Tool;

import org.chefx3d.toolbar.ToolBarManager;

import org.chefx3d.ui.LoadingProgressListener;
import org.chefx3d.ui.PopUpConfirm;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.ViewManager;

import org.chefx3d.view.awt.av3d.ActionData.EntityData;
import org.chefx3d.view.awt.scenemanager.DeviceManager;
import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.PerFrameUIObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;
import org.chefx3d.view.awt.scenemanager.ResizeListener;
import org.chefx3d.view.awt.scenemanager.UserInputHandler;

import org.chefx3d.view.common.EditorConstants;
import org.chefx3d.view.common.EditorGrid;
import org.chefx3d.view.common.EntityWrapper;
import org.chefx3d.view.common.NearestNeighborMeasurement;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Implementation of the location LayerManager
 *
 * @author Rex Melton
 * @version $Revision: 1.303 $
 */
class LocationLayerManager extends AbstractLayerManager
    implements
        ModelListener,
        NodeUpdateListener,
        UserInputHandler,
        PerFrameObserver,
        EntityPropertyListener,
        EntityChildListener,
        EntitySelectionListener,
        AV3DConstants,
        EditorConstants,
        PerFrameUIObserver,
        ResizeListener,
        MouseListener {

    /** How far should we zoom? */
    public static final float ZOOM_AMOUNT = 0.2f;

    /** Default background color */
    private static final float[] DEFAULT_BACKGROUND_COLOR = new float[]{0, 0, 0};

    /** Default background color */
    private static final int FRAMES_BEFORE_HOVER_OCCURS = 10;

    /** Confirmation pop up message */
    private static final String CONFIRM_MSG =
        "org.chefx3d.view.awt.av3d.LocationLayerManager.confirmSplit";

    /** Default name of the lighting file to go read */
    private static final String DEFAULT_LIGHTING_FILE =
        "config/view/av3d/editor_lighting.xml";

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /** Translation utility */
    private I18nManager intlMgr;

    /** The device manager */
    private DeviceManager deviceManager;

    /** The navigation status manager */
    private NavigationStatusManager navStatusManager;

    /** The world model */
    protected WorldModel model;

    /** List of commands that have been buffered */
    protected CommandController controller;

    /** The current tool */
    protected Tool currentTool;

    /** Utility class to construct entities from tools */
    protected EntityBuilder entityBuilder;

    /** Collision detector for rules checking */
    private EntityCollisionManager collisionManager;

    /** Container of editor state variables */
    private LocationEditorState editorState;

    /** Flag indicating that the background should be reconfigured */
    private boolean configBackground;

    /** Background node*/
    private ColorBackground background;

    /** Array used for setting up the background color*/
    private float backgroundColorArray[];

    /** What is the active button */
    private int activeButton;

    /** What is the active button modifier.
     * 0 is no modifier, 1 is the Ctrl Key, 2 is the Alt Key */
    private int activeButtonModifier;

    /** Used to keep track of how many frames the mouse has remained unmoved*/
    private int frameCount;

    /** The NavigationManager */
    private DefaultNavigationManager navManager;

    /** The PickManager */
    private PickManager pickManager;

    /** Are we in a transient command */
    private boolean inTransient;

    /** The current transactionID for transient commands.  Assume only one can be active. */
    private int transactionID;

    /** The mouse posisition, used for the ghost segments
     *  so that they do not start at 0,0,0*/
    private double[] shadowSegmentMousePosition;

    /** The cursor manager */
    private AV3DCursorManager cursorManager;

    private int mouseWheelClick;

    /** Flag indicating that the user has requested a viewpoint reset */
    private boolean doNavigationReset;

    /** The scene entity */
    private SceneEntity sceneEntity;

    /** Location Entitys, key'ed by Entity ID */
    private HashMap<Integer, LocationEntity> locationEntityMap;

    /** Location Group nodes, key'ed by Entity ID */
    private HashMap<Integer, Group> locationGroupMap;

    /** Location AV3DEntityManagers, key'ed by Entity ID */
    private HashMap<Integer, AV3DEntityManager> locationManagerMap;

    /** The active location Entity */
    private LocationEntity activeLocationEntity;

    /** The active location AV3DEntityManager */
    private AV3DEntityManager activeEntityManager;

    /** Map of entity wrappers from the activeEntityManager */
     private HashMap<Integer, AV3DEntityWrapper> wrapperMap;

    /** The active zone Entity */
    private ZoneEntity activeZoneEntity;

    /** The active zone's inverse transform matrix */
    private Matrix4f invZoneMtx;

    /** The current mouse position relative to the zone */
    private Point3f zoneRelativeMousePosition;

    /** The active location Group */
    private Group activeLocationGroup;

    /** The visibility handler */
    private LocationVisibilityHandler visHandler;

    /** The entity that the mouse is currently over */
    private Entity mouseOverEntity;

    /** Array list of nodes to add to the scene */
    private ArrayList<Node> nodeAddList;

    /** Array list of nodes to remove from the scene */
    private ArrayList<Node> nodeRemoveList;

    /** Helper class for configuring view based on the editing zone */
    private ZoneView zoneView;

    /** The filter to use for url requests, null use baseURL logic instead */
    private URLFilter urlFilter;

    /** The selected entity state at the last mouse press */
    private ActionData initialActionData;

    /** flag indicating that a drag operation has been initialized */
    private boolean dragInProgress;

    /** Flag indicating that a press operation has been processed this frame */
    private boolean pressProcessed;

    /** Flag indicating that a release operation has been processed this frame */
    private boolean releaseProcessed;

    /** The shadow entity, precursor to an actual live entity in the scene */
    private PositionableEntity shadowEntity;

	/////////////////////////////////////////////////
	// undocumented Jon vars
    private double[] shadowEntityLastPosition;
    private int shadowSegmentStartVertexID;
    private int shadowVertexEntityID;
    private boolean splitSegment;
    private boolean newWall;
	/////////////////////////////////////////////////

	/** The vertex that is the start of a new segment */
	private VertexEntity highlightedVertex;
	
    /** The shadow segment entity, precursor to an actual live segment entity in the scene */
    private SegmentEntity shadowSegmentEntity;

    /** Flag indicating that the pointing device is over the graphics surface */
    private boolean trackerIsOver;

    /** Flag indicating that the shadow geometry is active in the scene */
    private boolean shadowMoveInProgress;

    /** Flag indicating that the shadow geometry is visible in the scene */
    private boolean shadowIsVisible;

    /** The list of all entities in the scene */
    private HashMap<Integer, Entity> entityMap;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper selectionHelper;

    /** The set of selectable entity categories */
    private Set<String> categorySet;

    /** Scratch vecmath objects */
    private Matrix4f mtx;
    private Point3f pnt;

    /** Instance of hierarchy transformation calculator */
    private TransformUtils tu;

    /** Displays a pop up confirm message */
    private PopUpConfirm popUpConfirm = PopUpConfirm.getInstance();

    //-----------------------------------------------------
    // Event responders
    //-----------------------------------------------------

    /** Handles final move events for default entities */
    private EntityMoveResponse entityMoveResponse;

    /** Handles transient move events for default entities */
    private EntityMoveTransientResponse entityMoveTransientResponse;

    /** Handles add entity events */
    private AddEntityResponse addEntityResponse;

    /** Handles add segmentable entity events */
    private AddSegmentableEntityResponse addSegmentableEntityResponse;

    /** Handles add vertex to segmentable entity events */
    private AddVertexEntityResponse addVertexEntityResponse;

    /** Handles add auto span entity events */
    private AddAutoSpanEntityResponse addAutoSpanEntityResponse;

    /** Handles final move events for default entities */
    private ModelTransitionResponse modelTransitionResponse;

    /** Handles transient move events for default entities */
    private ModelTransitionTransientResponse modelTransitionTransientResponse;

    /** Handles add template entity events */
    private AddTemplateEntityResponse addTemplateEntityResponse;

    /** Handles transient move events for default entities */
    private ModelMoveTransientResponse modelMoveTransientResponse;

    /////////////////////////////////////////////////////////////////////////////
    // variables for resizing

    /** Has the view frustum been created yet */
    private boolean initialViewFrustumSet;

    /** the last-resized width */
    private int oldPixelWidth;

    /** the last-resized height */
    private int oldPixelHeight;

    /** array of length six to hold the view frustum */
    double[] viewFrustum;

    /** A progress bar notification */
    private LoadingProgressListener progressListener;

    /** variables to set and unset zoom mode */
    private NavigationMode currentNavMode, previousNavMode;

    /** are we zooming in or out? */
    private int zoom;

    /** controller to reset zoom mode on and off */
    private NavigationModeController currentNavController;

    /** Special trackerState to handle zooming */
    private TrackerState trackerState;

    /** How many zooms need processing? */
    private int moveCount;

	/** Nearest neighbor class */
	private NearestNeighborMeasurementImpl neighbor;
	
	/** Selection monitor, to handle selection of layered entities */
	private SelectionMonitor selectionMonitor;
	
	/** Entity hierarchy utils */
	private EntityUtils entityUtils;
	
	/** Entity configuration listeners */
	private ArrayList<ConfigListener> configListenerList;
	
    /**
     * Constructor
     *
     * @param id The layer id
     * @param dim The initial viewport dimensions in [x, y, width, height]
     * @param view_id Identifier of the view this is in
     * @param worldModel The WorldModel
     * @param commandController The CommandController
     * @param reporter The ErrorReporter instance to use or null
     * @param mgmtObserver The SceneManagerObserver
     * @param deviceMngr The DeviceManager
     * @param cursorMngr The CursorManager
     * @param navStatusMngr The NavigationStatusManager
     * @param urlField The urlFilter to use for resource loading
     */
    LocationLayerManager(
        int id,
        int[] dim,
        String view_id,
        WorldModel worldModel,
        CommandController commandController,
        ErrorReporter reporter,
        SceneManagerObserver sceneMgmtObserver,
        DeviceManager deviceMngr,
        AV3DCursorManager cursorMngr,
        NavigationStatusManager navStatusMngr,
        URLFilter url_filter,
        LoadingProgressListener loadingProgressListener) {

        super(id, dim);

        intlMgr = I18nManager.getManager();

        setErrorReporter(reporter);

        selectionHelper =
            EntitySelectionHelper.getEntitySelectionHelper();

        model = worldModel;
        controller = commandController;
        deviceManager = deviceMngr;
        cursorManager = cursorMngr;
        navStatusManager = navStatusMngr;
        urlFilter = url_filter;
        progressListener = loadingProgressListener;
        mgmtObserver = sceneMgmtObserver;
        mgmtObserver.addObserver(this);
        //mgmtObserver.addUIObserver(this);

		configListenerList = new ArrayList<ConfigListener>();
		
        ////////////////////////////////////////////////////////////////
        // scene objects managed by this

        background = new ColorBackground(DEFAULT_BACKGROUND_COLOR);
        scene.setActiveBackground(background);
        rootGroup.addChild(background);

        LightConfig lc = new LightConfig(
            DEFAULT_LIGHTING_FILE,
            errorReporter);

        if (lc.isEnabled() && lc.isConfigured()) {
            SwitchGroup lightSwitch = lc.getSwitchGroup();
            rootGroup.addChild(lightSwitch);
        } else {
            // just use ambient light for lighting
            viewpoint.setGlobalAmbientLightEnabled(true);
            viewpoint.setGlobalAmbientColor(new float[] {0.9f, 0.9f, 0.9f});
        }
        ////////////////////////////////////////////////////////////////
        // variables for resizing
        initialViewFrustumSet = false;
        viewFrustum = new double[6];

        ////////////////////////////////////////////////////////////////
        // hierarchy transformation objects
        tu = new TransformUtils();
        mtx = new Matrix4f();
        pnt = new Point3f();
        invZoneMtx = new Matrix4f();
        zoneRelativeMousePosition = new Point3f();

        ////////////////////////////////////////////////////////////////
        // managers and handlers
        visHandler = new LocationVisibilityHandler(mgmtObserver);
		visHandler.setProximityEnabled(true);
		configListenerList.add(visHandler);
		
        NavigationCollisionManager nc =
            new SimpleNavigationCollisionManager(rootGroup, 0.1f);

        // any configuration of the layer viewpoint transform group
        // should occur -before- instantiating the nav manager

        navManager = new DefaultNavigationManager(
            view_id,
            model,
            controller,
            errorReporter,
            this,
            rootGroup,
            null,
            mgmtObserver,
            nc,
            navStatusManager);
        navManager.setNavigationMode(NavigationMode.PANZOOM);

        // configure the border increment used for each
        // bounding box during intersection testing
        OrientedBoundingBox.setEpsilon(
                AV3DConstants.DEFAULT_EMBEDDING_DEPTH + 0.000001f);
        collisionManager = new EntityCollisionManager(
            model,
            mgmtObserver,
            errorReporter);

        pickManager = new PickManager(model, rootGroup);
        editorState = LocationEditorState.getInstance();

        initialActionData = new ActionData();
        initialActionData.zoneOri = ZoneOrientation.VERTICAL;
        initialActionData.model = model;
        initialActionData.pickManager = pickManager;

        ViewEnvironment viewEnv = scene.getViewEnvironment();
        viewEnv.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);

        zoneView = new ZoneView(
            viewEnv,
            model,
            controller,
            navStatusManager);

        ///////////////////////////////////////////////////////////////////
        //SHADOW SEGMENTENTITY

        shadowSegmentMousePosition = new double[3];

        shadowEntityLastPosition = new double[3];
        shadowSegmentStartVertexID = -1;
        shadowVertexEntityID = -1;

        newWall = false;

        ////////////////////////////////////////////////////////////////
        // event responders

		EditorGrid editorGrid = new EditorGrid();
		
        addEntityResponse =
            new AddEntityResponse(model, controller, reporter, editorGrid);

        addSegmentableEntityResponse =
            new AddSegmentableEntityResponse(model, controller, reporter, editorGrid);

        addVertexEntityResponse =
            new AddVertexEntityResponse(model, controller, reporter, editorGrid);

        addAutoSpanEntityResponse =
            new AddAutoSpanEntityResponse(model, controller, reporter, editorGrid);

        entityMoveResponse =
            new EntityMoveResponse(model, controller, reporter, viewEnv, editorGrid);

        entityMoveTransientResponse =
            new EntityMoveTransientResponse(model, controller, reporter, viewEnv, editorGrid);

        modelTransitionResponse =
            new ModelTransitionResponse(model, controller, reporter, viewEnv, editorGrid);

        modelTransitionTransientResponse =
            new ModelTransitionTransientResponse(model, controller, reporter, viewEnv, editorGrid);

        addTemplateEntityResponse =
            new AddTemplateEntityResponse(model, controller, reporter, editorGrid);

        modelMoveTransientResponse =
            new ModelMoveTransientResponse(model, controller, reporter, viewEnv, editorGrid);

        ////////////////////////////////////////////////////////////////
        // local collections
        locationEntityMap = new HashMap<Integer, LocationEntity>();
        locationGroupMap = new HashMap<Integer, Group>();
        locationManagerMap = new HashMap<Integer, AV3DEntityManager>();

        entityMap = new HashMap<Integer, Entity>();

        nodeAddList = new ArrayList<Node>();
        nodeRemoveList = new ArrayList<Node>();

        ////////////////////////////////////////////////////////////////
        // check for the existence of a SceneEntity, initialize
        Entity[] rootEntities = model.getModelData();
        for (int i = 0; i < rootEntities.length; i++) {
            if (rootEntities[i] instanceof SceneEntity) {
                // there should only be one....
                setSceneEntity((SceneEntity)rootEntities[i]);
                break;
            }
        }

        model.addModelListener(this);

        // variables for zoom
        trackerState = new TrackerState();
        moveCount = 0;
		
		neighbor = new NearestNeighborMeasurementImpl(model, rootGroup);
		selectionMonitor = new SelectionMonitor();
		entityUtils = new EntityUtils(model);
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameUIObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed
     */
    public void processNextFrameUI() {

        //
        // Handle mouse wheel zooms
        //
        if (trackerState.ctrlModifier == true){

            if (moveCount < 0) {
                currentNavController.start(trackerState);
            } else {
                if (zoom == -1) {
                    trackerState.devicePos[1] -= ZOOM_AMOUNT;
				} else {
                    trackerState.devicePos[1] += ZOOM_AMOUNT;
				}
                currentNavController.move(trackerState);
            }
            if (moveCount > 0) {
                currentNavController = null;

                // restore the previous mode
                if (previousNavMode != currentNavMode) {
                    navManager.setNavigationMode(previousNavMode);
                }
                // switch out of zoom mode
                trackerState.ctrlModifier = false;
            }
        }
        moveCount++;

        // clear any previous pick results
        pickManager.reset();

        // reconfigure the viewpoint first, in case any
        // subsequent operations rely on the view parameters
        if (doNavigationReset) {
            if (activeZoneEntity != null) {
                zoneView.configView(activeZoneEntity);
                doNavigationReset = false;
            }
        }

        // establish mode of operation based on current mode
        // and variable values
        if (activeZoneEntity == null) {
            editorState.currentMode = EditorMode.INACTIVE;
        } else {
            switch (editorState.currentMode) {
            case INACTIVE:
                if (activeZoneEntity != null) {
                    editorState.currentMode = EditorMode.SELECTION;
                }
                break;
            case SELECTION:
                if (currentTool != null) {
                    editorState.currentMode = EditorMode.PLACEMENT;
                }
                break;
            case PLACEMENT:
                if (currentTool == null) {
                    editorState.currentMode = EditorMode.SELECTION;
                }
                break;
            }
        }
        // if in an active mode, process the user input
        if (editorState.currentMode != EditorMode.INACTIVE) {

            Entity moe = mouseOverEntity;

            pressProcessed = false;
            releaseProcessed = false;
            deviceManager.processTrackers(id, this);

            boolean moeCheck = (moe != null) && (moe == mouseOverEntity);
            boolean anchorCheck = (editorState.mouseOverAnchor != null);
            if (moeCheck || anchorCheck) {
                frameCount++;
                if (frameCount == FRAMES_BEFORE_HOVER_OCCURS) {
                    if (mouseOverEntity != null ) {

                        //mouseOverEntity.setHighlighted(true);

                    } else if (editorState.mouseOverAnchor != null) {

                        AnchorData ad = editorState.mouseOverAnchor.getAnchorDataFlag();
                        cursorManager.setCursorMode(ad);
                    }
                }
            } else {
                frameCount = 0;
            }
            ///////////////////////////////////////////////////////////////////

            if (shadowMoveInProgress && !trackerIsOver) {
				
                AV3DEntityWrapper wrapper = null;
                if (shadowSegmentEntity != null) {
                    wrapper =
                        wrapperMap.get(shadowSegmentEntity.getEntityID());
                    if (wrapper == null) {
                        System.out.println("Can't find mapped entity: " + shadowSegmentEntity);
                    }
                } else {
                    wrapper =
                        wrapperMap.get(shadowEntity.getEntityID());

                    if (wrapper == null) {
                        System.out.println("Can't find mapped entity: " + shadowEntity);
                    }
                }

                if (wrapper != null) {
                    wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_NONE);

                    shadowIsVisible = false;
                    shadowMoveInProgress = false;
                }

            } else if (shadowMoveInProgress && !shadowIsVisible) {

                AV3DEntityWrapper wrapper = null;

                if (shadowSegmentEntity != null) {
                    wrapper =
                        wrapperMap.get(shadowSegmentEntity.getEntityID());
                } else {
                    wrapper =
                        wrapperMap.get(shadowEntity.getEntityID());
                }

                if (wrapper != null) {
                    wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);

                    shadowIsVisible = true;
                }
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

        if (configBackground) {
            mgmtObserver.requestDataUpdate(background, this);
        }
        if ((activeEntityManager != null) &&
            activeEntityManager.hasHierarchyChanged()) {

            visHandler.setEntityChanged();
        }
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

        // get the active button
        for (int i = 0; i < evt.numButtons; i++) {
            if (evt.buttonState[i]) {
                activeButton = i;
                break;
            }
        }

        // get the active modifier, if any
        activeButtonModifier = 0;
        if (evt.ctrlModifier) {
            activeButtonModifier = 1;
        } else if (evt.altModifier) {
            activeButtonModifier = 2;
        }

        if (activeButtonModifier == 0) {
/*
 
 	BJY:
 	This section has been commented out in favor of removing the shadow entity
 	at the same time we create the replacement. Each is handled on different
 	command stack so the removal of the shadow occurs before the add so we 
 	avoid any confusion.
 	
 	This section has been moved down to the trackerReleased() method, ln 1325.
 	Also, the AddEntityResponse class was updated with checks to prevent a 
 	shadow entity from being picked as the parent.
 
        	////////////////////////////////////////////////////////////////////
            if (shadowEntity != null ) {

                Entity parent = model.getEntity(
                    shadowEntity.getParentEntityID());

                // Don't continue if the entity is null
                if(parent == null){
                    return;
                }

                if(shadowEntity instanceof VertexEntity) {
                    ArrayList<Command> cmdList = new ArrayList<Command>();

                    Command vertexCmd = new RemoveVertexCommand(
                            model,
                            (SegmentableEntity)parent,
                            shadowEntity.getEntityID(), false);

                    vertexCmd.setErrorReporter(errorReporter);
                    cmdList.add(vertexCmd);


                    if(shadowSegmentEntity != null) {
                        Command segmentCmd = new RemoveSegmentCommand(
                                model,
                                (SegmentableEntity)parent,
                                shadowSegmentEntity.getEntityID(), false);
                        segmentCmd.setErrorReporter(errorReporter);
                        cmdList.add(segmentCmd);
                    }

                    MultiTransientCommand multi =
                        new MultiTransientCommand(
                                cmdList,
                                "Removing Entity -> " +shadowEntity.getEntityID());

                    controller.execute(multi);

                    shadowEntityWasVertex = true;
                    shadowEntity.getPosition(shadowEntityLastPosition);

                } else {
                    ///////////////////////////////////////////////////
                    // rem: save the last known position in the tool
                    if (currentTool != null) {
                        double[] pstn = new double[3];
                        shadowEntity.getPosition(pstn);
                        pstn[2] = 0;

                        currentTool.setProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            PositionableEntity.POSITION_PROP,
                            pstn);
                    }
                    ///////////////////////////////////////////////////
                    Command cmd = new RemoveEntityChildTransientCommand(
                        model, parent, shadowEntity);

                    cmd.setErrorReporter(errorReporter);
                    controller.execute(cmd);
                }

                shadowEntity = null;
                shadowSegmentEntity = null;
                shadowMoveInProgress = false;
                shadowIsVisible = false;
            }
            ////////////////////////////////////////////////////////////////////
*/
            if (activeButton == 0) {
                if (editorState.currentMode == EditorMode.SELECTION) {
					
					pickManager.doPick(evt, false, false);
					// only allow selections from the active zone
					if (activeZoneEntity.getType() == Entity.TYPE_MODEL_ZONE) {
						pickManager.filterResultsByParentZone(activeZoneEntity);
					}
                    if (pickManager.hasResults()) {

                        PickData pd = pickManager.getResult();
                        if (pd.object instanceof Entity) {

                            Entity entity = (Entity)pd.object;
                            String category = entity.getCategory();
							
                            // only allow a selection event on specific types of entities.
                            if (categorySet.contains(category)) {

                                ArrayList<Entity> selectedEntityList = new ArrayList<Entity>();
                                ArrayList<Entity> unselectedEntityList = new ArrayList<Entity>();

                                ArrayList<Entity> currentList = new ArrayList<Entity>();
                                currentList.addAll(selectionHelper.getSelectedList());

                                if (evt.shiftModifier) {

                                    if (currentList.contains(entity)) {
                                        unselectedEntityList.add(entity);
                                        currentList.remove(entity);
                                    } else {
                                        selectedEntityList.add(entity);
                                        currentList.add(entity);
                                    }
                                    changeSelection(selectedEntityList, unselectedEntityList);
                                } else {
									//////////////////////////////////////////////////////////////
									// process sub part selection
									
									// prune the pick list to only entities of the 
									// appropriate category
									ArrayList<PickData> pick_list = pickManager.getResults();
									for (int i = pick_list.size() - 1; i >= 0; i--) {
										PickData pdx = pick_list.get(i);
										if (pdx.object instanceof Entity) {
											String cat = ((Entity)pdx.object).getCategory();
											if (!categorySet.contains(cat)) {
												pick_list.remove(i);
											}
										} else {
											pick_list.remove(i);
										}
									}
									entity = selectionMonitor.check(evt, pick_list);
									
									//////////////////////////////////////////////////////////////
                                    unselectedEntityList.addAll(currentList);
                                    if (!currentList.contains(entity)) {
                                        selectedEntityList.add(entity);
                                        changeSelection(selectedEntityList, unselectedEntityList);
                                    } else {
                                        unselectedEntityList.remove(entity);
                                        changeSelection(selectedEntityList, unselectedEntityList);
                                    }
                                    currentList.clear();
                                    currentList.add(entity);
                                }
                                int num_selected = currentList.size();
                                boolean isMultiSelection = (num_selected > 1);
                                if (isMultiSelection) {
                                    // wait for a mouse release to switch to an
                                    // active mode
                                    //editorState.currentMode = EditorMode.INACTIVE;

                                    initialActionData.setEntities(currentList);
                                    initialActionData.setMouseDevicePosition(evt.devicePos);
                                    initialActionData.setMouseWorldPosition(evt.worldPos);
                                    initialActionData.zoneWrapper =
                                        wrapperMap.get(activeZoneEntity.getEntityID());
                                    initialActionData.wrapperMap = wrapperMap;

                                    for (int i = 0; i < num_selected; i++) {
                                        entity = currentList.get(i);

                                        if (entity instanceof PositionableEntity && 
                                        		entity.isModel()) {
                                                
                                        	editorState.currentMode = EditorMode.ENTITY_TRANSITION;     
                                        }
                                    }

                                } else {
                                    // on single selection, the object representing the
                                    // entity may be manipulated by the mouse in the editor
                                    if (entity instanceof PositionableEntity) {

                                        initialActionData.setEntities(currentList);
                                        initialActionData.setMouseDevicePosition(evt.devicePos);
                                        initialActionData.setMouseWorldPosition(evt.worldPos);
                                        initialActionData.zoneWrapper =
                                            wrapperMap.get(activeZoneEntity.getEntityID());
                                        initialActionData.wrapperMap = wrapperMap;

                                        int type = entity.getType();

                                        boolean isAutoSpan = false;
                                        if (currentTool != null) {
                                            Object obj = currentTool.getProperty(
                                                Entity.DEFAULT_ENTITY_PROPERTIES,
                                                ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                                            if ((obj != null) && (obj instanceof Boolean)){
                                                isAutoSpan = (Boolean)obj;
                                            }
                                        }

                                        if (isAutoSpan) {
                                            editorState.currentMode = EditorMode.ENTITY_TRANSFORM;
                                        } else if ((type == Entity.TYPE_MODEL) ||
                                        		(type == Entity.TYPE_MODEL_WITH_ZONES) ||
                                        		(type == Entity.TYPE_TEMPLATE_CONTAINER)) {
                                            editorState.currentMode = EditorMode.ENTITY_TRANSITION;
                                        } else {
                                            editorState.currentMode = EditorMode.ENTITY_TRANSFORM;
                                        }
                                    }
                                }

                            } else {
                                editorState.currentMode = EditorMode.NAVIGATION;
                            }
                        }
                    } else {
                        if (editorState.currentMode != EditorMode.ANCHOR_TRANSFORM) {
                            // if the selection layer has NOT set the mode to transform
                            // then we must be going to navigation
                            editorState.currentMode = EditorMode.NAVIGATION;
                        }
                    }
                } else if ((editorState.currentMode != EditorMode.ANCHOR_TRANSFORM) &&
                            (editorState.currentMode != EditorMode.ENTITY_TRANSFORM) &&
                            (editorState.currentMode != EditorMode.PLACEMENT)) {

                    editorState.currentMode = EditorMode.NAVIGATION;
                }
                if (editorState.currentMode == EditorMode.NAVIGATION) {
                    changeSelection(EMPTY_ENTITY_LIST, true);
                    navManager.trackerPressed(tracker, evt);
                }
            }

        } else {
            editorState.previousMode = editorState.currentMode;
            editorState.currentMode = EditorMode.NAVIGATION;
        }
    }

    /**
     * Process a tracker move event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerMoved(int tracker, TrackerState evt) {

        // determine what's under the mouse
        if (editorState.mouseOverAnchor != null) {
            cursorManager.setCursorMode(ActionMode.NONE);
        }

        // sidepocket the zone relative mouse position
        zoneRelativeMousePosition.set(evt.worldPos);
        invZoneMtx.transform(zoneRelativeMousePosition);

        boolean isPickResult = pickManager.doPick(evt);
        trackerIsOver = isPickResult;

        if (editorState.currentMode == EditorMode.PLACEMENT) {
            if ((shadowEntity != null) && trackerIsOver) {

                double[] position = new double[3];
                float[] size = new float[3];
                shadowEntity.getSize(size);

                Command cmd = null;

                boolean isAutoSpan = false;
                if (currentTool != null) {
                    Object obj = currentTool.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                    if ((obj != null) && (obj instanceof Boolean)){
                        isAutoSpan = (Boolean)obj;
                    }
                }

                boolean shadowPlaced = false;
                if (!isAutoSpan && isPickResult) {
                    // place the shadow where the product would be placed
                    PickData pd = pickManager.getResult();
                    if (shadowEntity instanceof VertexEntity) {

                        int parentID = shadowEntity.getParentEntityID();
                        SegmentableEntity segmentableEntity =
                            (SegmentableEntity)model.getEntity(parentID);

                        if (segmentableEntity == null) {
                            return;
                        }

                        // always place vertices against the zone
                        if (pd.object == shadowEntity) {
                            pd = pickManager.getResult(activeZoneEntity);
                        }

                        if (pd != null) {
                         // convert the mouse position from world to zone coordinates
                            pnt.set(evt.worldPos);
                            toZoneRelative(pnt);

                            position[0] = pnt.x; //pd.point.x;
                            position[1] = pnt.y; //pd.point.y;

                            position[2] = pnt.z; //pd.point.z + (size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH);

                            cmd = new MoveVertexTransientCommand(
                                model,
                                transactionID,
                                (VertexEntity)shadowEntity,
                                position,
                                new float[3]);

                            shadowPlaced = true;
                        }
                    } else {

                        PickData zone_pd = pickManager.getResult(activeZoneEntity);
                        if (zone_pd != null) {

                            // check to make sure its in the data model already
                            Entity check = model.getEntity(shadowEntity.getEntityID());
                            if (check == null) {
                                return;
                            }

                            Entity[] entityArr = new Entity[]{shadowEntity};
                            //if (!shadowMoveInProgress) {
                                initialActionData.zoneWrapper =
                                    wrapperMap.get(activeZoneEntity.getEntityID());
                                initialActionData.wrapperMap = wrapperMap;
                                modelMoveTransientResponse.setActionData(
                                    initialActionData);
                            //}
                            modelMoveTransientResponse.doEventResponse(
                                tracker,
                                evt,
                                entityArr,
                                null);

                            shadowPlaced = true;
                        }
                    }
                }
                if (!shadowPlaced) {
                    // convert the mouse position from world to zone coordinates
                    pnt.set(evt.worldPos);
                    toZoneRelative(pnt);

                    // place the shadow in the zone's plane,
                    // plus half the object dimension
                    position[0] = pnt.x;
                    position[1] = pnt.y;
                    position[2] = (size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH);

                    cmd = new MoveEntityTransientCommand(
                        model,
                        model.issueTransactionID(),
                        shadowEntity.getEntityID(),
                        position,
                        new float[3]);
                }

                if (cmd != null) {
                    cmd.setErrorReporter(errorReporter);
                    controller.execute(cmd);
                }

                shadowMoveInProgress = true;

            }

        } else {

            if (isPickResult) {

                PickData pd = pickManager.getResult();
                if (pd.object instanceof Entity) {

                    mouseOverEntity = (Entity)pd.object;

                } else {

                    if ((mouseOverEntity != null) && mouseOverEntity.isHighlighted()) {
                        mouseOverEntity.setHighlighted(false);
                    }

                    mouseOverEntity = null;
                }
            } else {

                if ((mouseOverEntity != null) && mouseOverEntity.isHighlighted()) {
                    mouseOverEntity.setHighlighted(false);
                }
                mouseOverEntity = null;
            }
        }
        shadowSegmentMousePosition[0] = evt.worldPos[0];
        shadowSegmentMousePosition[1] = evt.worldPos[1];
        shadowSegmentMousePosition[2] = evt.worldPos[2];
    }

    /**
     * Process a tracker drag event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerDragged(int tracker, TrackerState evt) {

        // sidepocket the zone relative mouse position
        zoneRelativeMousePosition.set(evt.worldPos);
        invZoneMtx.transform(zoneRelativeMousePosition);

        if (activeButton == 0) {
            if (editorState.currentMode == EditorMode.NAVIGATION) {

                navManager.trackerDragged(tracker, evt);

            } else if (editorState.currentMode == EditorMode.ENTITY_TRANSFORM) {

                ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

                // can only be in this mode if there is a single selection
                if (selectedList != null && selectedList.size() == 1) {
                    Entity entity = selectedList.get(0);
                    Entity[] entityArr = new Entity[] {entity};

                    if (!dragInProgress) {
                        cursorManager.setCursorMode(ActionMode.MOVEMENT);
                        entityMoveTransientResponse.setActionData(
                            initialActionData);
                    }
                    entityMoveTransientResponse.doEventResponse(
                        tracker,
                        evt,
                        entityArr,
                        null);
                }

            } else if (editorState.currentMode == EditorMode.ENTITY_TRANSITION) {

                pickManager.doPick(evt);

                ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

                Entity[] entityArr = new Entity[selectedList.size()];
                selectedList.toArray(entityArr);

                if (!dragInProgress) {
                    cursorManager.setCursorMode(ActionMode.MOVEMENT);
                    modelTransitionTransientResponse.setActionData(
                        initialActionData);
                }
                modelTransitionTransientResponse.doEventResponse(
                    tracker,
                    evt,
                    entityArr,
                    null);
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

        boolean hasMoved = dragInProgress;
        dragInProgress = false;

        if (releaseProcessed) {
            // don't do this twice in one frame
            return;
        } else {
            releaseProcessed = true;
        }

        if (activeButtonModifier == 0) {

            if (activeButton == 0) {

                ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

                Entity entity = null;
                // TODO: use the first selected entity for now
                if (selectedList.size() > 0) {
                    entity = selectedList.get(0);
                }

                if (editorState.currentMode == EditorMode.NAVIGATION) {

                    navManager.trackerReleased(tracker, evt);

                } else if (editorState.currentMode == EditorMode.ENTITY_TRANSFORM) {
                    if (hasMoved) {
                        // Prevent multi-selections from calculating transforms
                        if (selectedList.size() == 1) {

                            entityMoveResponse.setActionData(initialActionData);
                            entityMoveResponse.doEventResponse(
                                tracker,
                                evt,
                                new Entity[]{entity},
                                null);

                            cursorManager.setCursorMode(ActionMode.NONE);
                        }
                    }
                } else if (editorState.currentMode == EditorMode.ENTITY_TRANSITION) {
					
                    if (hasMoved) {
                        Entity[] list = new Entity[selectedList.size()];
                        selectedList.toArray(list);

                        // allow multi-selections from calculating transforms
                        pickManager.doPick(evt);

                        modelTransitionResponse.setActionData(
                            initialActionData);
                        modelTransitionResponse.doEventResponse(
                            tracker,
                            evt,
                            list,
                            null);

                        cursorManager.setCursorMode(ActionMode.NONE);
                    }
                } else if (editorState.currentMode == EditorMode.PLACEMENT) {

                	if (shadowEntity != null ) {

                        Entity parent = model.getEntity(
                            shadowEntity.getParentEntityID());

                        // Don't continue if the entity is null
                        if (parent == null) {
                            return;
                        }

                        if (!(shadowEntity instanceof VertexEntity)) {
                            ///////////////////////////////////////////////////
                            // rem: save the last known position in the tool
                            if (currentTool != null) {
                                double[] pstn = new double[3];
                                shadowEntity.getPosition(pstn);
                                pstn[2] = 0;

                                currentTool.setProperty(
                                    Entity.DEFAULT_ENTITY_PROPERTIES,
                                    PositionableEntity.POSITION_PROP,
                                    pstn);
                            }
                            ///////////////////////////////////////////////////
                            Command cmd = new RemoveEntityChildTransientCommand(
                                model, parent, shadowEntity);

                            cmd.setErrorReporter(errorReporter);
                            controller.execute(cmd);
							
							shadowEntity = null;
                        }

                        shadowMoveInProgress = false;
                        shadowIsVisible = false;
                    }
     
					boolean clearTool = true;
                    switch(currentTool.getToolType()) {

                    case Entity.TYPE_WORLD:
                    case Entity.TYPE_LOCATION:
                    case Entity.TYPE_ENVIRONMENT:
                        // should never see this
                        System.out.println("Unknown error, we're going down.");
                        break;

                    case Entity.TYPE_CONTENT_ROOT:
                        //
                        break;

                    case Entity.TYPE_MULTI_SEGMENT:
                        
						VertexEntity shadowVertex = null;
						SegmentEntity shadowSegment = shadowSegmentEntity;
						if (shadowEntity instanceof VertexEntity) {
							
							shadowVertex = (VertexEntity)shadowEntity;
							shadowVertex.getPosition(shadowEntityLastPosition);
							
							// queue up the shadow removes before adding any new
							clearShadowSegment();
							shadowEntity = null;
						}
						
						SegmentableEntity segmentableEntity = null;
						if (activeEntityManager != null) {
							segmentableEntity = 
								activeEntityManager.getSegmentableEntity();
						}
						if (segmentableEntity == null) {
							if (activeLocationEntity != null) {
								ContentContainerEntity cce = 
									activeLocationEntity.getContentContainerEntity();
								if (cce != null) {
									// create a segmentable if it does not already exist
									addSegmentableEntityResponse.setEntityBuilder(entityBuilder);
									addSegmentableEntityResponse.doEventResponse(
										tracker,
										evt,
										new Entity[] {cce},
										currentTool);
								}
							}
						} else {
							ArrayList<VertexEntity> vertexList =
								((SegmentableEntity)segmentableEntity).getVertices();
							
							int num_vertex = 0;
							if (vertexList != null) {
								num_vertex = vertexList.size();
							}
							//////////////////////////////////////////////////////////////////////////////
							boolean vertexCheck = false;  // boolean segmentCheck = false;
							
							if ((shadowVertex != null) && (num_vertex > 1)) {
								
								// rem: this is apparently a check to determine whether 
								// vertex 'merging' is called for. I have no idea what
								// that means though........
								
								for (int i = 0; i < vertexList.size(); i++) {
									
									VertexEntity ve1 = vertexList.get(i);
									
									int lastVertexID = ve1.getEntityID();
									// SOMETIMES THE SHADOW VERTEX HASN'T BEEN REMOVED YET
									// Nor do we want it merging with the vertex it is connected too.
									if ((shadowVertexEntityID == lastVertexID) ||
										(shadowSegmentStartVertexID == lastVertexID) ) {
										
										continue;
										
									} else if (withinTolerance(shadowEntityLastPosition, ve1)) {
										
										VertexEntity ve0 = shadowSegment.getStartVertexEntity();
										
										addVertexEntityResponse.doEventResponse(
											tracker,
											evt,
											new Entity[] {segmentableEntity, ve0, ve1},
											currentTool);
										
										vertexCheck = true;
										break;
									}
								}
							}
							//////////////////////////////////////////////////////////////////////////////
							if (!vertexCheck ) {
								if (shadowSegment == null) {
									// if there is no shadow segment, then the shadow vertex
									// must be the only vertex in the scene. add a shadow
									// segment
									updateShadowSegment(currentTool, shadowVertex);
									
								} else {
									// if there is a shadow segment, invoke the response to
									// figure out what to do....
									addVertexEntityResponse.setEntityBuilder(entityBuilder);
									addVertexEntityResponse.doEventResponse(
										tracker,
										evt,
										new Entity[] {segmentableEntity, shadowSegment},
										currentTool);
								}
								clearTool = false;
							}
						}
                        newWall = false;
                        break;

                    case Entity.TYPE_INTERSECTION:
                        //Entity segmentableEntity = null;
                        segmentableEntity =
                            activeEntityManager.getSegmentableEntity();

                        if (splitSegment) {

                            // convert the mouse position from world to zone
                            // coordinates
                            pnt.set(evt.worldPos);
                            toZoneRelative(pnt);

                            double[] position = new double[3];
                            position[0] = pnt.x;
                            position[1] = pnt.y;

                            ArrayList<SegmentEntity> segmentList = ((SegmentableEntity)segmentableEntity).getSegments();

                            for(int i =0; i< segmentList.size(); i++) {
                                SegmentEntity splitSegmentEntity = segmentList.get(i);
                                if (withinTolerance(position, splitSegmentEntity)) {

                                    if(splitSegmentEntity.hasChildren()) {

                                        String popupMessage = intlMgr.getString(CONFIRM_MSG);
                                        if(!(popUpConfirm.showMessage(popupMessage))){
                                            return;
                                        }
                                    }

                                    addVertexEntityResponse.setSplittingSegment((SegmentEntity)splitSegmentEntity);
                                    addVertexEntityResponse.setEntityBuilder(entityBuilder);
                                    addVertexEntityResponse.doEventResponse(
                                        tracker,
                                        evt,
                                        new Entity[] {segmentableEntity},
                                        currentTool);
                                    break;
                                }
                            }
                        }

                        break;

                    case Entity.TYPE_BUILDING:
                        // TODO: support buildings
                        break;

                    case Entity.TYPE_TEMPLATE_CONTAINER:
                        ////////////////////////////////////////////////////////////
                        // rem: skip converting the mouse position in the case of a
                        // template. the last configured position has been set to
                        // the tool during mousePressed
                        /*
                        // convert the mouse position from world to zone coordinates
                        pnt.set(evt.worldPos);
                        toZoneRelative(pnt);

                        // place the template in the zone's plane
                        double[] pstn = new double[3];
                        pstn[0] = pnt.x;
                        pstn[1] = pnt.y;
                        pstn[2] = 0;

                        currentTool.setProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            PositionableEntity.POSITION_PROP,
                            pstn);
                        */
                        ////////////////////////////////////////////////////////////

                        initialActionData.zoneWrapper =
                            wrapperMap.get(activeZoneEntity.getEntityID());
                        initialActionData.wrapperMap = wrapperMap;
                        addTemplateEntityResponse.setActionData(initialActionData);
                        addTemplateEntityResponse.setEntityBuilder(entityBuilder);
                        addTemplateEntityResponse.doEventResponse(
                            tracker,
                            evt,
                            null,
                            currentTool);

                        break;

                    case Entity.TYPE_MODEL:
                    case Entity.TYPE_MODEL_WITH_ZONES:

                        boolean isAutoSpan = false;
                        if (currentTool != null) {
                            Object obj = currentTool.getProperty(
                                Entity.DEFAULT_ENTITY_PROPERTIES,
                                ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                            if ((obj != null) && (obj instanceof Boolean)){
                                isAutoSpan = (Boolean)obj;
                            }
                        }

                        if (isAutoSpan) {
                            initialActionData.zoneWrapper =
                                wrapperMap.get(activeZoneEntity.getEntityID());
                            addAutoSpanEntityResponse.setActionData(initialActionData);
                            addAutoSpanEntityResponse.setEntityBuilder(entityBuilder);

                            addAutoSpanEntityResponse.doEventResponse(
                                tracker,
                                evt,
                                null,
                                currentTool);
                        } else {
                            initialActionData.zoneWrapper =
                                wrapperMap.get(activeZoneEntity.getEntityID());
                            initialActionData.wrapperMap = wrapperMap;
                            addEntityResponse.setActionData(initialActionData);
                            addEntityResponse.setEntityBuilder(entityBuilder);

                            addEntityResponse.doEventResponse(
                                tracker,
                                evt,
                                null,
                                currentTool);
                        }
                        break;
                    }
					//////////////////////////////////////////////////////////////////////////
					/*
                    String toolName =
                        intlMgr.getString("com.yumetech.chefx3d.editor.catalog.tool.addWall");
                    if (toolName == null) {
                        toolName = "Add Wall";
                    }

                    if ((currentTool != null) && (shadowEntity == null) &&
                        !(currentTool.getName().equals(toolName))) {
						// this allows the tool to be cleared
						// after a placement, with the exception of the tool that
						// adds walls. it is desired that walls can be extended until
						// the tool is expressly dismissed.......
                        ViewManager.getViewManager().setTool(null);
                        ToolBarManager.getToolBarManager().setTool(null);
                    }
					*/
					// rem: this seems simpler
					if (clearTool) {
						ViewManager.getViewManager().setTool(null);
                        ToolBarManager.getToolBarManager().setTool(null);
					}
					//////////////////////////////////////////////////////////////////////////
                }
            } else if (activeButton == 2) {

                changeSelection(EMPTY_ENTITY_LIST, true);
                ViewManager.getViewManager().setTool(null);
                ToolBarManager.getToolBarManager().setTool(null);
            }
            editorState.currentMode = EditorMode.SELECTION;
            
        } else if (activeButtonModifier == 1) {

            // the mac uses ctrl-click to simulate a right click
            if (activeButton == 0) {
                changeSelection(EMPTY_ENTITY_LIST, true);
                ViewManager.getViewManager().setTool(null);
                ToolBarManager.getToolBarManager().setTool(null);    
                
                editorState.currentMode = EditorMode.SELECTION;
            }
            
        } else {
            editorState.currentMode = editorState.previousMode;
        }
        
		if (hasMoved) {
			selectionMonitor.reset();
		}
        activeButton = -1;
    }

    /**
     * Process a tracker click event.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerClicked(int tracker, TrackerState evt) {
        // TODO: not sure this is the correct thing to do yet,
        // for now treat a click as a full press and release
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

        if (editorState.currentMode == EditorMode.ENTITY_TRANSFORM) {
            // TODO: hax for now to stop rotation on walls
            // this should be rules-based.
            if (activeZoneEntity.getType() == Entity.TYPE_SEGMENT) {
                return;
            }

            mouseWheelClick += state.wheelClicks;

            if (!inTransient) {
                transactionID = model.issueTransactionID();
                inTransient = true;
            }

            ArrayList<Entity> selectedList = selectionHelper.getSelectedList();

            Entity entity = null;
            // TODO: use the first selected entity for now
            if (selectedList.size() > 0) {
                entity = selectedList.get(0);
            }

            if(entity != null){
                float[] tmpRot = new float[4];
                tmpRot[0] = 0;
                tmpRot[1] = 0;
                tmpRot[2] = 1;
                tmpRot[3] = (float) ((mouseWheelClick*10) / 180.0f) * (float) Math.PI;

                RotateEntityTransientCommand cmd = new RotateEntityTransientCommand(
                    model,
                    transactionID,
                    entity.getEntityID(),
                    tmpRot);

                cmd.setErrorReporter(errorReporter);
                model.applyCommand(cmd);
            }
        } else if(editorState.currentMode == EditorMode.SELECTION ){

            //
            // Initialize for navigation. Reset the tracker object,
            // setup the navigation mode.
            //
            trackerState.devicePos[0] = 0;
            trackerState.devicePos[1] = 0;
            trackerState.devicePos[2] = 0;

            currentNavMode = NavigationMode.PANZOOM;
            trackerState.ctrlModifier = true;


            previousNavMode = navManager.getNavigationMode();

            if (previousNavMode != currentNavMode) {
                navManager.setNavigationMode(currentNavMode);
            }

            //
            // Start call backs on the view's update thread
            //
            moveCount = -1;
            //mgmtObserver.addUIObserver(this);
            currentNavController =
                navManager.getNavigationModeController();

            //
            // Set the zoom: are we zooming in or out?
            //
            zoom = state.wheelClicks;
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

        if (src == rootGroup) {

            int numToRemove = nodeRemoveList.size();
            if (numToRemove > 0) {
                for(int i = 0; i < numToRemove; i++) {
                    Node node = nodeRemoveList.get(i);
                    rootGroup.removeChild(node);
                }
                nodeRemoveList.clear();
            }

            int numToAdd = nodeAddList.size();
            if (numToAdd > 0) {
                for(int i = 0; i < numToAdd; i++) {
                    Node node = nodeAddList.get(i);
                    rootGroup.addChild(node);
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

        if ((src == background) && configBackground) {

            background.setColor(backgroundColorArray);
            configBackground = false;

        }
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

        Entity entity = model.getEntity(entityID);

        if (entity instanceof LocationEntity) {
            if (entity == activeLocationEntity) {

                if (propertyName.equals(LocationEntity.ACTIVE_ZONE_PROP)) {
                    // update the active zone
                    int zoneID = activeLocationEntity.getActiveZoneID();
                    zoneSelected(zoneID);

                } else if (propertyName.equals(LocationEntity.BACKGROUND_COLOR_PROP)) {
                    // configure the background
                    Color color = activeLocationEntity.getBackgroundColor();
                    float[] backgroundColor;
                    if (color != null) {
                        backgroundColor = color.getColorComponents(null);
                    } else {
                        backgroundColor = DEFAULT_BACKGROUND_COLOR;
                    }
                    configBackground(backgroundColor);
                }
            }
        }
    }

    //------------------------------------------------------------------------
    // Methods required by ResizeListener
    //------------------------------------------------------------------------

    /**
     * window size has been updated
     */
    public void sizeChanged(int newWidth, int newHeight){

        if ((newWidth > 0) && (newHeight > 0)) {

            ViewEnvironment ve = scene.getViewEnvironment();

            if (!initialViewFrustumSet){

                ve.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
                ve.setOrthoParams(-newWidth*.01f,
                    newWidth*.01f,
                    -newHeight*.01f,
                    newHeight*.01f);
                ve.setAspectRatio(0);
                ve.setClipDistance(0.001, 1000.0);

                // initialization of scene is to the minimum size
                initialViewFrustumSet = true;
            } else {

                doNavigationReset = true;
                float widthRatio = 1;
                float heightRatio = 1;

                if (oldPixelWidth > 0) {
                    widthRatio = (float)newWidth / (float)oldPixelWidth;
				}
                if (oldPixelHeight > 0) {
                    heightRatio = (float)newHeight / (float)oldPixelHeight;
				}

                ve.setOrthoParams(viewFrustum[0] * widthRatio,
                    viewFrustum[1] * widthRatio,
                    viewFrustum[2] * heightRatio,
                    viewFrustum[3] * heightRatio);
            }

            oldPixelWidth = newWidth;
            oldPixelHeight = newHeight;

            double[] tmp = new double[6];
            ve.getViewFrustum(tmp);

            navStatusManager.fireViewportSizeChanged(tmp);

            ve.getViewFrustum(viewFrustum);
            //      System.out.println("locationview frustum: " + java.util.Arrays.toString(viewFrustum));
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

        if (parentEntity instanceof SceneEntity &&
            childEntity instanceof LocationEntity) {

            LocationEntity le = (LocationEntity)childEntity;

            le.addEntityPropertyListener(this);

            recursiveAdd(le);

            addLocationEntity(le);

            if (activeLocationEntity == null) {
                setActiveLocationEntity(le);
            }

        } else if (parentEntity instanceof LocationEntity &&
            childEntity instanceof ViewpointContainerEntity) {

            if (parentEntity == activeLocationEntity) {
                ViewpointContainerEntity vce =
                    (ViewpointContainerEntity)childEntity;
                navManager.setViewpointContainerEntity(vce);
            }

        } else {
            // all others get the default set of listeners
            recursiveAdd(childEntity);
        }

        // TODO: once we redo the template locations we need to clean up the special
        // case for add wall tool.
        // Russell: removing for now.  this code was trying to reset the catalog 
        // after a location was loaded.
//        String toolName =
//            intlMgr.getString("com.yumetech.chefx3d.editor.catalog.tool.addWall");
//        if (toolName == null) {
//            toolName = "Add Wall";
//        }
//
//        if (currentTool != null &&
//                shadowEntity == null &&
//                !currentTool.getName().equals(toolName)) {
//            ViewManager.getViewManager().setTool(null);
//            ToolBarManager.getToolBarManager().setTool(null);
//        }
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
            if (childEntity instanceof LocationEntity) {

                LocationEntity le = locationEntityMap.remove(childID);

                le.removeEntityPropertyListener(this);

                AV3DEntityManager mngr = locationManagerMap.remove(childID);
                mngr.clear();

                if (le == activeLocationEntity) {
                    setActiveLocationEntity(null);
                }

                locationGroupMap.remove(childID);

            } 
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
		if (entity != null) {
			
			if (selected) {
				
				int type = entity.getType();
				switch (type) {
				case Entity.TYPE_LOCATION:
					LocationEntity le = (LocationEntity)entity;
					if (le != activeLocationEntity) {
						setActiveLocationEntity(le);
					}
					break;
					
				case Entity.TYPE_VERTEX:
					if (shadowEntity == null) {
						updateShadowSegment(currentTool, (VertexEntity)entity);
					}
					break;
				}
			}
		}
		
		////////////////////////////////////////////////////////////////
		// rem: this is a hack to mitigate the NPE of issue #1185		
		// russ: only update if the action data does not contain the entity
		
		boolean matched = false;
		ArrayList<EntityData> dataList = initialActionData.entityList;
		for (int i = 0; i < dataList.size(); i++) {
		    EntityData data = dataList.get(i);
		    if (data.entity == entity) {
		        matched = true;
		    }
		}
				
		if (!matched && editorState.currentMode == EditorMode.ENTITY_TRANSITION) {
			ArrayList<Entity> currentList = new ArrayList<Entity>();
			currentList.addAll(selectionHelper.getSelectedList());
			initialActionData.setEntities(currentList);
		}
		////////////////////////////////////////////////////////////////
    }

    /**
     * An entity has been highlighted
     *
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
        //Entity entity = entityMap.get(entityID);
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

        if (entity instanceof SceneEntity) {
            setSceneEntity((SceneEntity)entity);
        }
    }

    /**
     * An entity was removed.
     *
     * @param local Is the request local
     * @param entity The entity to remove
     */
    public void entityRemoved(boolean local, Entity entity) {

        if (entity == sceneEntity) {
            clear();
        }
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {

    }

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {

    }

    /**
     * The master view has changed.
     *
     * @param local Is the request local
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {

    }

    //------------------------------------------------------------------------
    // Methods defined by MouseListener
    //------------------------------------------------------------------------

    /**
     * Notification that the pointing device has entered the
     * graphics component
     */
    public void mouseEntered(MouseEvent e) {
        trackerIsOver = true;
    }

    /**
     * Notification that the pointing device has left the
     * graphics component
     */
    public void mouseExited(MouseEvent e) {
        trackerIsOver = false;
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e){
        // ignored
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e){
        // ignored
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e){
        // ignored
    }

    //------------------------------------------------------------------------
    // Local Methods
    //------------------------------------------------------------------------
	
	/** 
	 * Add a config listener
	 *
	 * @param cl The listener to add
	 */
	void addConfigListener(ConfigListener cl) {
		configListenerList.add(cl);
	}

	/** 
	 * Remove a config listener
	 *
	 * @param cl The listener to remove
	 */
	void removeConfigListener(ConfigListener cl) {
		configListenerList.remove(cl);
	}

    /**
     * Return the zone relative mouse position in the argument array.
     * If the argument is null or less than length 3, a new array
     * will be allocated and returned.
     *
     * @param An array for the return value
     * @return The array containing the return value
     */
    float[] getZoneRelativeMousePosition(float[] position) {
        if ((position == null) || (position.length  < 3)) {
            position = new float[3];
        }
        zoneRelativeMousePosition.get(position);
        return(position);
    }

    /**
     * Return the Entity collision manager
     *
     * @return the Entity collision manager
     */
    EntityCollisionManager getEntityCollisionManager() {
        return collisionManager;
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
     * zoneSelected should be triggered by selection a portion of either
     * the zone or wall selection panel.
     *
     * @param entityID The entity associated with the zone
     */
    private void zoneSelected(int entityID) {

        // the current wall/zone has been updated
		Entity entity = model.getEntity(entityID);
		if (entity != null) {
			
			int type = entity.getType();
			
			//////////////////////////////////////////////////////////////////
			//
			// RUSS: the thumb nail processor needs to set the active zone
			// even if it is the current one, otherwise the viewpoint does not
			// send out a property update.
			//
			// stop as nothing has changed
			//if (activeZoneEntity == entity) {
			//	  System.out.println("activeZone1: " + activeZoneEntity);
			//	  return;
			//}
			//////////////////////////////////////////////////////////////////
			
			boolean zoneChanged = false;
			switch (type) {
			case Entity.TYPE_ZONE:
			case Entity.TYPE_SEGMENT:
			case Entity.TYPE_GROUNDPLANE_ZONE:
			case Entity.TYPE_MODEL_ZONE:
			    
			    // turn off current zone
                if (activeZoneEntity != null && activeZoneEntity.getType() == Entity.TYPE_MODEL_ZONE) {
                    AV3DEntityWrapper activeZoneWrapper =
                        wrapperMap.get(activeZoneEntity.getEntityID());
                    if (activeZoneWrapper != null)
                    	activeZoneWrapper.setTransparency(0);
                }
				
				activeZoneEntity = (ZoneEntity)entity;
				
				AV3DEntityWrapper activeZoneWrapper =
					wrapperMap.get(activeZoneEntity.getEntityID());
				
				if (activeZoneWrapper == null) {
					break;
				}
				
				tu.getLocalToVworld(activeZoneWrapper.transformGroup, invZoneMtx);
				invZoneMtx.invert();
				
                if (activeZoneEntity.getType() == Entity.TYPE_MODEL_ZONE) {
                    activeZoneWrapper.setTransparency(1);
                }
				
				zoneChanged = true;
				break;
				
			default:
				System.out.println("Unknown type, not setting activeZoneEntity type: " + type);
			}
			
			if (zoneChanged) {
			    				
				// inform those that need to know
				zoneView.configView(activeZoneEntity);
				
				for (int i = 0; i < configListenerList.size(); i++) {
					ConfigListener cl = configListenerList.get(i);
					cl.setActiveZoneEntity(activeZoneEntity);
				}
				
				// clear the tool
				ViewManager.getViewManager().setTool(null);
				ToolBarManager.getToolBarManager().setTool(null);
			}
		}
    }

    /**
     * Initialize from the SceneEntity
     *
     * @param se The new SceneEntity
     */
    private void setSceneEntity(SceneEntity se) {

        if (sceneEntity != null) {
            // a scene already existed, cleanup
            clear();
        }

        LocationEntity newActiveLocationEntity = null;

        if (se != null) {
            sceneEntity = se;

            entityMap.put(sceneEntity.getEntityID(), sceneEntity);

            sceneEntity.addEntityChildListener(this);

            ArrayList<Entity> entityList = sceneEntity.getChildren();
            for (int i = 0; i < entityList.size(); i++) {
                Entity e = entityList.get(i);
                if (e instanceof LocationEntity) {
                    LocationEntity le = (LocationEntity)e;
                    addLocationEntity(le);
                    if (newActiveLocationEntity == null) {
                        // pick the first one
                        newActiveLocationEntity = le;
                    }
                }
            }
        }
        setActiveLocationEntity(newActiveLocationEntity);
    }

    /**
     * Remove objects, references, etc. from the scenegraph
     */
	private void clear() {
		
		sceneEntity.removeEntityChildListener(this);
		
		for (Iterator<Integer> i = locationEntityMap.keySet().iterator();
			i.hasNext();) {
			
			LocationEntity le = locationEntityMap.get(i.next());
			le.removeEntityPropertyListener(this);
			recursiveRemove(le);
		}
		for (Iterator<Integer> i = locationManagerMap.keySet().iterator();
			i.hasNext();) {
			
			AV3DEntityManager mngr = locationManagerMap.get(i.next());
			mngr.clear();
		}
		setActiveLocationEntity(null);
		
		locationEntityMap.clear();
		locationGroupMap.clear();
		locationManagerMap.clear();
		entityMap.clear();
		
		activeZoneEntity = null;
		activeLocationEntity = null;
		activeLocationGroup = null;
		sceneEntity = null;
	}

    /**
     * Add a new LocationEntity
     *
     * @param le The LocationEntity to add
     */
    private void addLocationEntity(LocationEntity le) {

        Integer id = new Integer(le.getEntityID());
        locationEntityMap.put(id, le);

        Group group = new Group();
        locationGroupMap.put(id, group);

        AV3DEntityManager em = new AV3DEntityManager(
            mgmtObserver,
            model,
            le,
            group,
            urlFilter,
            progressListener,
            errorReporter);

        locationManagerMap.put(id, em);
    }

    /**
     * Get the active LocationEntity
     *
     * @return The active LocationEntity
     */
    LocationEntity getActiveLocationEntity() {
        return activeLocationEntity;
    }

    /**
     * Set the active LocationEntity
     *
     * @param le The new active LocationEntity
     */
    private void setActiveLocationEntity(LocationEntity le) {

        if (le == activeLocationEntity) {
            return;
        }

        boolean locationIsActive = (activeLocationEntity != null);
        boolean scheduleGroupChange = false;

        if (locationIsActive) {

            scheduleGroupChange = true;
            if (nodeAddList.contains(activeLocationGroup)) {
                nodeAddList.remove(activeLocationGroup);
            } else {
                nodeRemoveList.add(activeLocationGroup);
            }
            activeLocationGroup = null;
        }

        activeLocationEntity = le;
        zoneView.setLocationEntity(activeLocationEntity);

        if (activeLocationEntity != null) {

            int id = activeLocationEntity.getEntityID();

            activeLocationGroup = locationGroupMap.get(id);
            if (nodeRemoveList.contains(activeLocationGroup)) {
                nodeRemoveList.remove(activeLocationGroup);
            } else {
                nodeAddList.add(activeLocationGroup);
            }
            scheduleGroupChange = true;

            Color color = activeLocationEntity.getBackgroundColor();
            float[] backgroundColor;
            if (color != null) {
                backgroundColor = color.getColorComponents(null);
            } else {
                backgroundColor = DEFAULT_BACKGROUND_COLOR;
            }
            configBackground(backgroundColor);

            ViewpointContainerEntity vce =
                activeLocationEntity.getViewpointContainerEntity();
            navManager.setViewpointContainerEntity(vce);

            activeEntityManager = locationManagerMap.get(id);
            wrapperMap = activeEntityManager.getAV3DEntityWrapperMap();

            collisionManager.setEntityManager(activeEntityManager);
            zoneView.setEntityManager(activeEntityManager);
			
			for (int i = 0; i < configListenerList.size(); i++) {
				ConfigListener cl = configListenerList.get(i);
				cl.setEntityManager(activeEntityManager);
			}
			
			neighbor.setWrapperMap(wrapperMap);

            int zoneID = activeLocationEntity.getActiveZoneID();
            zoneSelected(zoneID);

        } else {
            navManager.setViewpointContainerEntity(null);
            configBackground(DEFAULT_BACKGROUND_COLOR);

            activeEntityManager = null;
            activeZoneEntity = null;
            wrapperMap = null;

            collisionManager.setEntityManager(null);
            zoneView.setEntityManager(null);
			
			for (int i = 0; i < configListenerList.size(); i++) {
				ConfigListener cl = configListenerList.get(i);
				cl.setEntityManager(null);
			}
        }

        if (scheduleGroupChange) {
            mgmtObserver.requestBoundsUpdate(rootGroup, this);
        }
    }

    /**
     * Return the NavigationManager
     *
     * @return The NavigationManager
     */
    NavigationManager getNavigationManager() {
        return(navManager);
    }

    /**
     * Set the active navigation mode
     *
     * @param activeMode The new navigation mode
     */
    void setNavigationMode(NavigationMode mode) {
        navManager.setNavigationMode(mode);
    }

    /**
     * Return the active navigation mode
     *
     * @return The active navigation mode
     */
    NavigationMode getNavigationMode() {
        return(navManager.getNavigationMode());
    }

    /**
     * Reset the viewpoint to the default for the editing zone.
     */
    void resetNavigation() {
        doNavigationReset = true;
    }

    /**
     * The set of entity categories that can be selected.
     * Pass-through to the pick manager
     *
     * @param categorySet The set of categories
     */
    void setSelectionCategories(Set<String> categorySet) {
        this.categorySet = categorySet;
    }

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
	void setTool(Tool tool) {
		
		// only allow the current tool to be set once
		if (currentTool != tool) {
			////////////////////////////////////////////////////////////////////
			if (shadowEntity != null) {
				
				Entity parent = model.getEntity(shadowEntity.getParentEntityID());
				if (parent != null) {
					if (shadowEntity instanceof VertexEntity) {
						
						clearShadowSegment();
						
					} else {
						Command cmd = new RemoveEntityChildTransientCommand(
							model, 
							parent, 
							shadowEntity);
						cmd.setErrorReporter(errorReporter);
						controller.execute(cmd);
					}
				}
				
				shadowEntity = null;
				shadowMoveInProgress = false;
				shadowIsVisible = false;
			}
			////////////////////////////////////////////////////////////////////
			
			if (tool != null) {
				int type = tool.getToolType();
				if (type != Entity.TYPE_WORLD ||
					type != Entity.TYPE_TEMPLATE) {
					
					currentTool = tool;
					
					cursorManager.setToolCursor(currentTool);
					
					///////////////////////////////////////////////////////////////////
					if (activeZoneEntity != null &&
						(type == Entity.TYPE_MODEL ||
						type == Entity.TYPE_MODEL_WITH_ZONES ||
						type == Entity.TYPE_MULTI_SEGMENT ||
						type == Entity.TYPE_INTERSECTION ||
						type == Entity.TYPE_TEMPLATE_CONTAINER)) {
						
						if (type == Entity.TYPE_MULTI_SEGMENT) {
							
							updateShadowSegment(tool, null);
							newWall = true;
							
						} else if (type == Entity.TYPE_INTERSECTION) {
							
							//place holder
							ArrayList<Entity> selectionList = selectionHelper.getSelectedList();
							
							boolean getLast = false;
							splitSegment = true;
							
						} else {
							// place the shadow somewhere remote
							double[] position = new double[]{-1000, 0, 0};
							float[] rotation = new float[]{0, 1, 0, 0};
							
							ArrayList<Command> cmdList = new ArrayList<Command>();
							
							// the shadow entity....
							shadowEntity = (PositionableEntity)entityBuilder.createEntity(
								model,
								model.issueEntityID(),
								position,
								rotation,
								currentTool);
							
							// set a property that the entity manager
							// understands - and will create a model in shadow
							// mode
							AV3DUtils.setShadowState(shadowEntity, true);
							
							shadowEntity.setStartingPosition(position);
							
							Command cmd = new AddEntityChildTransientCommand(
								model,
								activeZoneEntity,
								shadowEntity);
							cmd.setErrorReporter(errorReporter);
							cmdList.add(cmd);
							
							// the shadow is selected to show the selection box
							cmd = new SelectEntityCommand(model, shadowEntity, true);
							cmd.setErrorReporter(errorReporter);
							cmdList.add(cmd);
							
							// clear all selections first
							selectionHelper.clearSelectedList();
							
							// now execute the command
							MultiTransientCommand multiCmd =
								new MultiTransientCommand(
								cmdList,
								"Add Entity -> " + shadowEntity.getEntityID());
							multiCmd.setErrorReporter(errorReporter);
							
							controller.execute(multiCmd);
						}
					}
					///////////////////////////////////////////////////////////////////
				}
				editorState.currentMode = EditorMode.PLACEMENT;
				
			} else {
				
				currentTool = null;
				cursorManager.setToolCursor(null);
				changeSelection(EMPTY_ENTITY_LIST, true);
				editorState.currentMode = EditorMode.SELECTION;
			}
		}
	}

    /**
     * Return the EntityBuilder
     *
     * @return the entityBuilder
     */
    EntityBuilder getEntityBuilder() {
        if (entityBuilder == null) {
            entityBuilder = DefaultEntityBuilder.getEntityBuilder();
        }

        return entityBuilder;
    }

    /**
     * Set the class used to create entities from tools
     *
     * @param entityBuilder The entityBuilder to set
     */
    void setEntityBuilder(EntityBuilder entityBuilder) {
        this.entityBuilder = entityBuilder;
    }

    /**
     * Calls the SelectEntityCommand for the new selected entity, or entities
	 *
     * @param list a list of entities selected
     * @param clearList true clears the selectionHelper, false keeps it intact
     */
    private void changeSelection(List<Entity> list, boolean clearList){

        if(clearList){
            selectionHelper.clearSelectedList();
        }

        for (int i = 0; i < list.size(); i++) {

            Entity entity = list.get(i);
            SelectEntityCommand cmdSelect =
                new SelectEntityCommand(model, entity, true);
            controller.execute(cmdSelect);
        }
    }

    /**
     * Calls the SelectEntityCommand for the new selected entity, or entities
     * @param list a list of entities selected
     * @param clearList true clears the selectionHelper, false keeps it intact
     */
    private void changeSelection(List<Entity> selectList, List<Entity> unselectList){

        // unselection list
        for (int i = 0; i < unselectList.size(); i++) {
            Entity entity = unselectList.get(i);
            SelectEntityCommand cmdSelect =
                new SelectEntityCommand(model, entity, false);
            controller.execute(cmdSelect);
        }

        // selection list
        for (int i = 0; i < selectList.size(); i++) {
            Entity entity = selectList.get(i);
            SelectEntityCommand cmdSelect =
                new SelectEntityCommand(model, entity, true);
            controller.execute(cmdSelect);
        }
    }

    /**
     * Initialize the background
     *
     * @param backgroundColor The color value
     */
    private void configBackground(float[] backgroundColor) {

        if ((backgroundColor == null) || (backgroundColor.length < 3)) {
            backgroundColorArray = DEFAULT_BACKGROUND_COLOR;
        } else {
            backgroundColorArray = backgroundColor;
        }
        configBackground = true;
    }

    /**
     * Convert the argument from world coordinates to
     * zone relative coordinates in the active editing zone.
     *
     * @param point A position in world coordinates
     */
    private void toZoneRelative(Point3f point) {

        AV3DEntityWrapper wrapper =
            wrapperMap.get(activeZoneEntity.getEntityID());

        // transform the position to be relative to
        // the zone's coordinate system
        tu.getLocalToVworld(wrapper.transformGroup, mtx);
        mtx.invert();
        mtx.transform(point);
    }

    /**
     * Checks tolerance for both the intersection and merge functions of the segments
     * The first if check , determines the distance between the segment and the mouse position
     * The second check determines the distance between the mouse and vertex
     */
    private boolean withinTolerance(double[] position, Entity checkAgainst){

        boolean result = false;
        double[] checkAgainstPosition = new double[3];
        float tolerance = .15f;
        if (checkAgainst instanceof SegmentEntity){

            double[] startVertexPos = new double[3];
            double[] endVertexPos = new double[3];
            VertexEntity startVertex = ((SegmentEntity)checkAgainst).getStartVertexEntity();
            VertexEntity endVertex = ((SegmentEntity)checkAgainst).getEndVertexEntity();

            if ((startVertex == null) || (endVertex == null)) {
                return false;
            }

            startVertex.getPosition(startVertexPos);
            endVertex.getPosition(endVertexPos);

            double distanceBetweenMouseAndStartX = position[0] - startVertexPos[0];
            double distanceBeTweenMouseAndStartY = position[1] - startVertexPos[1];
            double deltaX = endVertexPos[0] - startVertexPos[0];
            double deltaY = endVertexPos[1] - startVertexPos[1];

            double dot = distanceBetweenMouseAndStartX * deltaX
                    + distanceBeTweenMouseAndStartY * deltaY;
            double len_sq = deltaX * deltaX + deltaY * deltaY;
            double param = dot / len_sq;

            double newX, newY;
            if (param < 0) {
                newX = startVertexPos[0];
                newY = startVertexPos[1];
            } else if (param > 1) {
                newX = endVertexPos[0];
                newY = endVertexPos[1];
            } else {
                newX = startVertexPos[0] + param * deltaX;
                newY = startVertexPos[1] + param * deltaY;
            }
            double distance =Math.sqrt( ((position[0] - newX) * (position[0] - newX)) +
                    ((position[1] - newY) * (position[1] - newY)) ) ;

            result = (tolerance > distance);

        } else if (checkAgainst instanceof PositionableEntity){
            ((PositionableEntity)checkAgainst).getPosition(checkAgainstPosition);

            //Distance equation equivalent of
            //dist = sqrt((x2-x1)^2 + (y2-y1)^2 + (z2-z1)^2)
            // no z used due to top down though
            double xDelta = checkAgainstPosition[0] - position[0];
            double yDelta = checkAgainstPosition[1] - position[1];
            double distance = Math.sqrt((xDelta * xDelta) + (yDelta * yDelta));
            result = (tolerance > distance);
        }

        return(result);
    }

    /**
     * Used to reset the shadow entity and segment
     */
    private void updateShadowSegment(Tool tool, VertexEntity startVertex) {
		
        if ((tool != null) && (tool instanceof SegmentableTool)) {
			
			SegmentableTool mseg_tool = (SegmentableTool)tool;
			ArrayList<Command> cmdList = new ArrayList<Command>();

			double[] position;
			float[] rotation = new float[4];
            if (trackerIsOver) {
                position = shadowSegmentMousePosition;
			} else {
				position = new double[3];
			}
			
            int entityId =  model.issueEntityID();
            // the shadow entity....
            shadowEntity = (PositionableEntity)entityBuilder.createEntity(
                model,
                entityId,
                position,
                rotation,
                mseg_tool.getVertexTool());
			
			VertexEntity shadowVertex = (VertexEntity)shadowEntity;
			AV3DUtils.setShadowState(shadowVertex, true);
            shadowVertex.setStartingPosition(position);

            SegmentableEntity parent =
                activeEntityManager.getSegmentableEntity();
			
			if (startVertex == null) {
				
				startVertex = findStartVertex(parent);
			}
			if (startVertex == null) {
				// if no starting vertex can be identified, then
				// this must be the first vertex in the scene
				
                // place the shadow somewhere remote
                position = new double[]{-1000, 0, 0};
                shadowVertex.setPosition(position, false);

                Command cmd = new AddVertexTransientCommand(
                    parent,
                    shadowVertex,
                    0);
                cmd.setErrorReporter(errorReporter);
                cmdList.add(cmd);

                // the shadow is selected to show the selection box
                cmd = new SelectEntityCommand(model, shadowVertex, true);
                cmd.setErrorReporter(errorReporter);
                cmdList.add(cmd);
				
			} else {
				///////////////////////////////////////////////////////
				// set the highlighted var to identify the vertex
				// as the beginning of a segment
				if (highlightedVertex != null) {
					highlightedVertex.setHighlighted(false);
				}
				highlightedVertex = startVertex;
				highlightedVertex.setHighlighted(true);
				///////////////////////////////////////////////////////
				
				int startVertexID = startVertex.getEntityID();
				int vertexOrder = parent.getVertexIndex(startVertexID) + 1;
				
				// unselect the start
                Command cmd = new SelectEntityCommand(model, startVertex, false);
                cmd.setErrorReporter(errorReporter);
                cmdList.add(cmd);
				
				// add the new end
				cmd = new AddVertexTransientCommand(
					parent,
					shadowVertex,
					vertexOrder);
				cmd.setErrorReporter(errorReporter);
				cmdList.add(cmd);
				
				// select the new end
                cmd = new SelectEntityCommand(model, shadowVertex, true);
                cmd.setErrorReporter(errorReporter);
                cmdList.add(cmd);
				
				// create and add the segment
				SegmentTool segmentTool = (SegmentTool)mseg_tool.getSegmentTool();
				
				shadowSegmentEntity = (SegmentEntity)entityBuilder.createEntity(
					model,
					model.issueEntityID(),
					position,
					rotation,
					segmentTool);
				
				AV3DUtils.setShadowState(shadowSegmentEntity, true);
				
				shadowSegmentStartVertexID = startVertexID;
				shadowVertexEntityID = shadowEntity.getEntityID();
				
				shadowSegmentEntity.setStartVertex(startVertex);
				shadowSegmentEntity.setEndVertex(shadowVertex);
				
				cmd = new AddSegmentTransientCommand(
					model,
					parent,
					shadowSegmentEntity);
				cmd.setErrorReporter(errorReporter);
				cmdList.add(cmd);
				
			}
			MultiTransientCommand multiCmd = new MultiTransientCommand(
				cmdList,
				"Shadow Segment -> " + shadowVertex.getEntityID());
			multiCmd.setErrorReporter(errorReporter);
			
			controller.execute(multiCmd);
        }
    }

	/**
	 * Perform the incantations that will reveal the identity
	 * of the mystical, magical starting vertex entity.
	 *
	 * @param multisegment The parent Segmentable entity
	 * @return The starting VertexEntity.... if it can be found.
	 * null otherwise.
	 */
	private VertexEntity findStartVertex(SegmentableEntity multisegment) {
		
		VertexEntity startVertex = null;
		if (multisegment != null) {
			
			// first check the selected list for a suitable candidate
			ArrayList<Entity> selectedList = selectionHelper.getSelectedList();
			int num_selected = selectedList.size();
			for (int i = 0; i < num_selected; i++) {
				
				Entity entity = selectedList.get(i);
				if (entity instanceof VertexEntity) {
					if (!AV3DUtils.isShadow(entity)) {
						startVertex = (VertexEntity)entity;
						break;
					}
				} else if (entity instanceof SegmentEntity) {
					
					startVertex = ((SegmentEntity)entity).getEndVertexEntity();
				}
			}
			// if none are found, work through the list of existing vertices
			if (startVertex == null) {
				ArrayList<VertexEntity> vertex_list = multisegment.getVertices();
				if (vertex_list != null) {
					int num_vertex = vertex_list.size();
					// purge the vertex list of shadows
					for (int i = num_vertex - 1; i >= 0; i--) {
						VertexEntity ve = vertex_list.get(i);
						if (!AV3DUtils.isShadow(ve)) {
							startVertex = ve;
							break;
						}
					}
				}
			}
		}
		return(startVertex);
	}
	
	/**
	 * Remove any components of a shadowed segment
	 */
    void clearShadowSegment() {
		
        if (shadowEntity != null) {

            if (shadowEntity instanceof VertexEntity) {
				
                SegmentableEntity segmentableEntity = 
					activeEntityManager.getSegmentableEntity();

                ArrayList<Command> cmd_list = new ArrayList<Command>();
				
				Command cmd = new RemoveVertexCommand(
					model,
					segmentableEntity,
					shadowEntity.getEntityID(),
					false);
                cmd_list.add(cmd);

				if (shadowSegmentEntity != null) {
					
					VertexEntity startVertex = 
						shadowSegmentEntity.getStartVertexEntity();
					
					if (AV3DUtils.isShadow(startVertex)) {
						cmd = new RemoveVertexCommand(
							model,
							segmentableEntity,
							startVertex.getEntityID(),
							false);
						cmd_list.add(cmd);
					}
					
					cmd = new RemoveSegmentCommand(
						model,
						segmentableEntity,
						shadowSegmentEntity.getEntityID(),
						false);
	                cmd_list.add(cmd);
				}
				
				MultiTransientCommand multi = new MultiTransientCommand(
					cmd_list,
					"Removing Entity -> " + shadowEntity.getEntityID());
				multi.setErrorReporter(errorReporter);

                controller.execute(multi);

                shadowSegmentEntity = null;
            }
        }
    }

    void clearShadowEntities() {
		
        if (shadowEntity != null) {

            if (shadowEntity.getType() == Entity.TYPE_VERTEX) {
				
				clearShadowSegment();
				
            } else {
				
                Entity parent = model.getEntity(shadowEntity.getParentEntityID());

                RemoveEntityChildTransientCommand removeEntityCommand =
                    new RemoveEntityChildTransientCommand(model, parent, shadowEntity);
                removeEntityCommand.setErrorReporter(errorReporter);
                controller.execute(removeEntityCommand);
            }
            shadowEntity = null;
            shadowMoveInProgress = false;
            shadowIsVisible = false;
        }
    }

    /**
     * Returns the map of EntityWrappers
	 *
     * @return The map of EntityWrappers
     */
    HashMap<Integer, EntityWrapper> getEntityWrapperMap(){
        return(activeEntityManager.getEntityWrapperMap());
    }

    /**
     * Returns the map of AV3DEntityWrappers
	 * 
     * @return The map of AV3DEntityWrappers
     */
    HashMap<Integer, AV3DEntityWrapper> getAV3DEntityWrapperMap(){
        return(wrapperMap);
    }
	
    /**
     * Return the EntityManager
	 *
     * @return The EntityManager
     */
    AV3DEntityManager getAV3DEntityManager(){
        return(activeEntityManager);
    }

    /**
     * Return the NearestNeighborMeasurement object
     * 
     * @return The NearestNeighborMeasurement object
     */
    NearestNeighborMeasurement getNearestNeighborMeasurement() {
		return(neighbor);
	}
    
    /**
     * Walk through the children of the argument entity,
     * adding listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveAdd(Entity entity) {

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

        entityMap.remove(entity.getEntityID());

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveRemove(child);
            }
        }
    }

    /**
     * Get the zoom amount from ZoneView.
     *
     * @return double zoom amount
     */
    double getZoneViewZoom(){

        return zoneView.getZoomAmount();
    }
	
	/**
	 * Relocate the shadow entity in the case that the pointer device
	 * is not over the editor panel
	 */
	private void configShadow() {
		if ((shadowEntity != null) && (shadowEntity instanceof VertexEntity)) {
			
			double[] position = new double[3];
			shadowEntity.getPosition(position);
			
			position[0] = 0; 
			position[1] = 0;
			
			Command cmd = new MoveVertexTransientCommand(
				model,
				transactionID,
				(VertexEntity)shadowEntity,
				position,
				new float[3]);
			
			cmd.setErrorReporter(errorReporter);
            controller.execute(cmd);
		}
	}
}
