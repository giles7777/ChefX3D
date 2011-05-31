/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt;

// Standard Imports
import java.util.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.html.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.File;
import java.net.URL;

import org.web3d.x3d.sai.*;
import org.xj3d.sai.Xj3DBrowser;
import org.xj3d.sai.Xj3DViewpoint;
import org.xj3d.sai.Xj3DNavigationUIManager;
import org.xj3d.sai.Xj3DNavigationUIListener;
import org.xj3d.sai.Xj3DScreenCaptureListener;

// Application specific imports
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.ViewConfig;
import org.chefx3d.view.ViewX3D;
import org.chefx3d.view.ThumbnailListener;
import org.chefx3d.view.WorldLoaderListener;
import org.chefx3d.view.ViewManager;
import org.chefx3d.model.*;

import org.web3d.sai.util.SceneUtils;

import org.web3d.util.IntHashMap;
import org.web3d.vrml.scripting.external.sai.SAIBrowser;

import org.web3d.x3d.sai.environmentalsensor.ProximitySensor;

import org.web3d.x3d.sai.geometry3d.IndexedFaceSet;
import org.web3d.x3d.sai.grouping.Group;
import org.web3d.x3d.sai.grouping.Transform;

import org.web3d.x3d.sai.navigation.Viewpoint;


import org.web3d.x3d.sai.pickingsensor.LinePicker;
import org.web3d.x3d.sai.pickingsensor.PrimitivePicker;

import org.web3d.x3d.sai.rendering.Coordinate;
import org.web3d.x3d.sai.rendering.IndexedLineSet;

import org.web3d.x3d.sai.shape.Shape;

import org.web3d.x3d.sai.time.TimeSensor;

/**
 * A View which is backed by a full 3D scene.
 * No editing is possible right now, just viewing.
 *
 * 3D Views use the x3d_view stylesheet.  This stylesheet must be self-contained.
 *
 * @author Alan Hudson
 * @version $Revision: 1.101 $
 */
class Watcher3DView extends JPanel
    implements
        ViewX3D,
        ViewConfig,
        Runnable,
        BrowserListener,
        WorldLoaderListener,
        X3DFieldEventListener,
        ModelListener,
        EntityPropertyListener,
        EntityChildListener,
        Xj3DScreenCaptureListener,
        Xj3DNavigationUIListener {

    /** Value of transparency when turned on */
    private static final float TRANSPARENCY_ON_VALUE = 0.5f;

    /** Value of transparency when turned off */
    private static final float TRANSPARENCY_OFF_VALUE = 1.0f;

    /** The initial world to load */
    private String initialWorld;

    /**
     * Defines picking type to use in the Watcher3DView
     */
    public enum PickingType {
        NONE, ELEVATION, PRIMITIVE;
    }

    /**
     * The picking type to use (default ELEVATION)
     */
    private PickingType pickingType = PickingType.NONE;

    /** Time in ms of no navigation activity till we issue a non-transient ChangeViewCommand */
    private static final int CHANGE_VIEW_TIME = 250;

    /** The world model */
    private WorldModel model;

    /** The X3D browser */
    private ExternalBrowser x3dBrowser;

    /** The current url */
    private String currentURL;

    /** The current scene */
    private X3DScene mainScene;

    /** The group to assign all content */
    private Group contentGroup;

    /** Map between entityID and loaded models Transform node */
    private IntHashMap modelMap;

    /** The X3DComponent in use */
    private X3DComponent x3dComp;

    /** An X3D exporter */
    private X3DExporter exporter;

    /** Are we in associate mode */
    //private boolean associateMode;

    /** What mode are we in, master, slave, free nav */
    private int navMode;

    /** The unique viewID */
    private long viewID;

    /** The proximity sensor used for determining position */
    private ProximitySensor proxSensor;

    /** The position changed field of the proximitySensor */
    private SFVec3f posChanged;

    /** The orientation changed field of the proximitySensor */
    private SFRotation oriChanged;

    /** The master viewpoint */
    private Viewpoint masterViewpoint;

    /** The slaved viewpoint */
    private Viewpoint slavedViewpoint;

    /** The free viewpoint */
    private Viewpoint freeViewpoint;

    /** Have the network VP's been added */
    private boolean networkedVPAdded;

    /** Scratch var for converting positions */
    private float[] tmpPosF;

    /** Scratch var for converting positions */
    private double[] tmpPosD;

    /** Scratch var for sending linear velocity */
    private float[] linearVelocity;

    /** Scratch var for sending angular velocity */
    private float[] angularVelocity;

    /** The last position */
    private double[] lastPos;

    /** The last orientation */
    private float[] lastOri;

    /** The field count of changed fields */
    private int fieldCnt;

    /** The last position update from X3D */
    private float[] newPosition;

    /** The last orientation update from X3D */
    private float[] newOrientation;

    /** The current transactionID */
    private int transactionID;

    /** The last time of navigation activity */
    private long lastNavigationTime;

    /** The last frame time */
    private long lastFrameTime;

    /** Should we terminate the worker thread */
    private boolean terminate;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** Has the main world loaded */
    private boolean worldLoaded;

    /** Flag indication to see if the browser is initalized or not */
    private boolean isBrowserInitalized;

    /**  Manages X3D scene navigation. */
    private Xj3DNavigationUIManager xj3dNavigationManager;

    /** Maps vertex entity ID to vertex array location of X3D vertex */
    private HashMap<Integer, Integer> vertexEntityIDMap;

    /**
     * List of vertices making up the geometric representation of the
     * multi-segment object
     */
    private float[] vertexList = null;

    /**
     * Reference to the coordinates field that is updated with changes in the
     * vertexList
     */
    private MFVec3f coordinates = null;

    /** First end cap for multi-segment object */
    private SFVec3f startEndCapTranslation;

    /** Second end cap for multi-segment object */
    private SFVec3f endEndCapTranslation;

    /** Queue of new multi-segment objects waiting for SAI processing */
    private ArrayList<EntityAddedQueueData> multiSegmentQueue =
        new ArrayList<EntityAddedQueueData>();

    /** Queue of new picking objects waiting for SAI processing */
    private ArrayList<EntityAddedQueueData> pickingNodeQueue =
        new ArrayList<EntityAddedQueueData>();

    /** Available viewpoints */
    private ArrayList<Xj3DViewpoint> viewpointList;

    /** Generated thumbnail image file names */
    private ArrayList<String> thumbnailNames;

    /** Lists of thumbnail listeners */
    private ArrayList<ThumbnailListener> thumbnailListeners;

    /** Capture frame function called */
    private boolean captureFrame = false;

    /** Keeps track of index of the viewpoint */
    private int thumbnailViewpointIndex;

    /////////////////////////////////////////////////////////////////////////
    // picking variables

    /** Debugging flag that makes the pick line visible */
    private static final boolean SHOW_PICK_LINE = false;

    /** The line pick sensor */
    private LinePicker lpPickNode;

    /** The multi-segment pick sensor */
    private PrimitivePicker multiSegmentPickNode;

    /** Map of entity ids to the primitive picker box geometry */
    private HashMap<Integer, X3DNode> pickBoxGeometryMap = null;

    /** Map of entity ids to the primitive picker shape material node */
    private HashMap<Integer, X3DNode> pickShapeMaterialMap = null;

    /** Map of entity ids to the primitive picker node */
    private HashMap<Integer, X3DNode> pickPrimitiveNodeMap = null;

    /** Map of entity ids to the primitive picker transform node */
    private HashMap<Integer, X3DNode> pickTransformGeometryMap = null;

    /** Map of entity ids to the primitive picker shape geometry */
    private HashMap<Integer, X3DNode> pickTargetProxyMap = null;

    /** The bounding volume associated with the currently selected entity */
    private X3DNode activePickingMaterial = null;

    /** The line pick sensor's isActive field */
    private SFBool linePickIsActive;

    /** The currently selected entityID */
    private int currentlySelectedEntityID;

    /** The sensor pick point output field */
    private MFVec3f pickedPoint;

    /** The sensor pick target field. Used to determine when the
    *  targets have been established for a picking request. */
    private MFNode pickTarget;

    /** Flag indicating that the targets have been established
    *  for a picking request. */
    private boolean pickTargetReady;

    /** Flag indicating that the picking geometry has been relocated
    *  to the desired coordinates for a picking request. */
    private boolean pickGeometryReady;

    /** Flag indicating whether the picking geometry is intersecting
     *  pick targets. */
     private boolean pickNodeIsActive;

    /** The pick line geometry coordinate field */
    private Coordinate coord;

    /** The pick line coordinate point field */
    private MFVec3f coordPoint;

    /** The initial pick line coordinate points */
    private float[] line_point = new float[]{ 0, 10000, 0, 0, -10000, 0 };

    /** The pick line coordinate point indices */
    private int[] line_point_indicies = new int[]{ 0, 1 };

    /** The queue of picks to process */
    private ArrayList<EntityConfigData> pickQueue;

    /** The queue of picks that have been processed and are
    *  awaiting an ack in the form of a move command. */
    private ArrayList<EntityConfigData> completeQueue;

    /** Flag indicating that an elevation pick is active */
    private boolean pickInProgress;

    /** The collection of pickable targets in the scene. This includes
     *  the location geometry and any non-segmented entities. */
     private Vector<X3DNode> targetListLP;

    /** TimeSensor used for detecting event model cycles. Used on new
    *  world loads to manage initialization of scene dependent objects. */
    private TimeSensor timeSensor;

    /** TimeSensor field that listens for detecting event model cycles. */
    private SFTime time;

    /** A frame counting watchdog, to terminate picks that are initiated
     *  but the pick geometry does not intersect a target. */
    private int pickWatchdog;

    /** The entity ID to entity proper */
    private HashMap<Integer, Entity> entityMap;

    private Transform multiSegmentCollisionVolumeTransform = null;

    private float[] multiSegmentBoundingVolumeBoxSize = null;

    private SFVec3f multiSegmentTransformScale = null;

    private SFBool multiSegmentPickIsActive = null;

    private MFNode multiSegmentPickedGeometry = null;

    /**
     * Forces one render frame pause before adding X3D multi-sub to scene.
     */
    private boolean pauseBeforeAddingMultiSegmentPickingVolume = true;

    /**
     * Forces one render frame pause before adding X3D entity to scene.
     */
    private boolean pauseBeforeAddingPickingVolume = true;

    /**
     * Is set true immediately after an entity is added to X3D scene to
     * give time for it to be established in the scene before adding the
     * collision node to it.
     */
    private boolean forceCollisionTargetUpdate = false;

    /**
     * Allows a single picking check to be performed one frame after a new
     * X3D entity, with its picking node, is added to the scene.
     */
    private boolean forcePickingCheck = false;

    /////////////////////////////////////////////////////////////////////////

    public Watcher3DView(HashMap params) {
        super(new BorderLayout());

        init(params);

        viewpointList = new ArrayList<Xj3DViewpoint>();
        thumbnailNames = new ArrayList<String>();
        thumbnailListeners = new ArrayList<ThumbnailListener>();
        thumbnailViewpointIndex = 0;
    }

    public Watcher3DView(WorldModel model, String initialWorld) {
        super(new BorderLayout());

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        if (initialWorld == null) {
            errorReporter.errorReport("No initial world specified for Watcher3D", null);
        }

        this.initialWorld = initialWorld;

        model.addModelListener(this);
        this.model = model;

        // Setup browser parameters
        // TODO: Watcher3DView - HashMap has mixed types, is not type safe in 1.5 generics
        HashMap requestedParameters = new HashMap();
        requestedParameters.put("Xj3D_FPSShown",Boolean.TRUE);
        requestedParameters.put("Xj3D_LocationShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_LocationPosition", "top");
        requestedParameters.put("Xj3D_LocationReadOnly", Boolean.FALSE);
        requestedParameters.put("Xj3D_OpenButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ReloadButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ShowConsole",Boolean.FALSE);
        requestedParameters.put("Xj3D_StatusBarShown",Boolean.TRUE);
        requestedParameters.put("Antialiased", Boolean.TRUE);
        requestedParameters.put("Xj3D_AntialiasingQuality", "high");
        requestedParameters.put("TextureQuality", "high");

        init(requestedParameters);

        viewID = (long) (Math.random() * Long.MAX_VALUE);
        navMode = MODE_SLAVED;
        tmpPosF = new float[3];
        tmpPosD = new double[3];
        linearVelocity = new float[3];
        angularVelocity = new float[4];
        lastPos = new double[3];
        lastOri = new float[4];
        newPosition = new float[3];
        newOrientation = new float[4];
        fieldCnt = 0;

        entityMap = new HashMap<Integer, Entity>();

        terminate = false;
        Thread thread = new Thread(this, "Watcher3D Tasks");
        thread.start();

        ViewManager.getViewManager().addView(this);

        pickQueue = new ArrayList<EntityConfigData>();
        completeQueue = new ArrayList<EntityConfigData>();

        viewpointList = new ArrayList<Xj3DViewpoint>();
        thumbnailNames = new ArrayList<String>();
        thumbnailListeners = new ArrayList<ThumbnailListener>();
        thumbnailViewpointIndex = 0;
    }

    /**
     * Constructor that sets the WorldModel, initial world to load and the
     * PickingType to use.
     *
     * @param model
     * @param initialWorld
     * @param pickingType
     */
    public Watcher3DView(WorldModel model, String initialWorld, PickingType pickingType) {
        super(new BorderLayout());

        this.pickingType = pickingType;
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        if (initialWorld == null) {
            errorReporter.errorReport("No initial world specified for Watcher3D", null);
        }

        this.initialWorld = initialWorld;

        model.addModelListener(this);
        this.model = model;

        // Setup browser parameters
        // TODO: Watcher3DView - HashMap has mixed types, is not type safe in 1.5 generics
        HashMap requestedParameters = new HashMap();
        requestedParameters.put("Xj3D_FPSShown",Boolean.TRUE);
        requestedParameters.put("Xj3D_LocationShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_LocationPosition", "top");
        requestedParameters.put("Xj3D_LocationReadOnly", Boolean.FALSE);
        requestedParameters.put("Xj3D_OpenButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ReloadButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ShowConsole",Boolean.FALSE);
        requestedParameters.put("Xj3D_StatusBarShown",Boolean.TRUE);
        requestedParameters.put("Antialiased", Boolean.TRUE);
        requestedParameters.put("Xj3D_AntialiasingQuality", "high");
        requestedParameters.put("TextureQuality", "high");

        init(requestedParameters);

        viewID = (long) (Math.random() * Long.MAX_VALUE);
        navMode = MODE_SLAVED;
        tmpPosF = new float[3];
        tmpPosD = new double[3];
        linearVelocity = new float[3];
        angularVelocity = new float[4];
        lastPos = new double[3];
        lastOri = new float[4];
        newPosition = new float[3];
        newOrientation = new float[4];
        fieldCnt = 0;

        entityMap = new HashMap<Integer, Entity>();

        terminate = false;
        Thread thread = new Thread(this, "Watcher3D Tasks");
        thread.start();

        ViewManager.getViewManager().addView(this);

        pickQueue = new ArrayList<EntityConfigData>();
        completeQueue = new ArrayList<EntityConfigData>();

        viewpointList = new ArrayList<Xj3DViewpoint>();
        thumbnailNames = new ArrayList<String>();
        thumbnailListeners = new ArrayList<ThumbnailListener>();
        thumbnailViewpointIndex = 0;
    }

    /**
     * The current tool changed.
     *
     * @param tool The new tool
     */
    public void setTool(Tool tool) {
        // ignored
    }

    /**
     * Go into associate mode. The next mouse click will perform
     * a property update
     *
     * @param validTools A list of the valid tools. null string will be all
     *        valid. empty string will be none.
     * @param propertyGroup The grouping the property is a part of
     * @param propertyName The name of the property being associated
     */
    public void enableAssociateMode(
            String[] validTools,
            String propertyGroup,
            String propertyName) {
        //associateMode = true;
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
        //associateMode = false;
    }

    /**
     * Creates a thumbnail image out of the current view.
     * A generated image file will be saved into a local
     * hard-drive.
     */
    public void createThumbnailImages() {
        if(isBrowserInitalized) {

            if(thumbnailNames.size() > 0)
                thumbnailNames.clear();

            // Capture one frame from the current viewpoint and save that as a thumbnail
            if(x3dBrowser instanceof SAIBrowser) {
                SAIBrowser saiBrowser = (SAIBrowser)x3dBrowser;

                thumbnailViewpointIndex = 0;
                captureFrame = true;
                saiBrowser.firstViewpoint();
                saiBrowser.captureFrames(1);
            }
        }
    }

    /**
     * Return true if our view is initalized or false if not.
     * @return True or false.
     *
     */
    public boolean isBrowserInitalized() {
        return isBrowserInitalized;
    }

    /**
     * Return lists of generate thumnail image names.
     * @return Lists of generated thumbnail image names.
     */
    public List<String> getGeneratedThumbnailImageNames() {
        return thumbnailNames;
    }

    /**
     * Adds thumbnail listener to the view
     * @param listener ThumbnailListener
     */
    public void addThumbnailGenListener(ThumbnailListener listener) {
        thumbnailListeners.add(listener);
    }

    /**
     * Shutdown this view.
     */
    public void shutdown() {
        model.removeModelListener(this);

        x3dBrowser.dispose();
        x3dComp.shutdown();

        x3dBrowser = null;
        mainScene = null;
        x3dComp = null;

        terminate = true;
    }

    /**
     * Common initialization code.
     *
     * @param params Xj3D Params to pass through
     */
    private void init(HashMap params) {

        currentlySelectedEntityID = -1;
        multiSegmentQueue.clear();
        pickingNodeQueue.clear();
        networkedVPAdded = false;

        // Create an SAI component
        x3dComp = BrowserFactory.createX3DComponent(params);

        // Add the component to the UI
        JComponent x3dPanel = (JComponent)x3dComp.getImplementation();

        add(x3dPanel, BorderLayout.CENTER);

        // Get an external browser
        x3dBrowser = x3dComp.getBrowser();
        x3dBrowser.addBrowserListener(this);

        // Capture one frame from the current viewpoint and save that as a thumbnail
        if(x3dBrowser instanceof SAIBrowser) {
            SAIBrowser saiBrowser = (SAIBrowser)x3dBrowser;
            saiBrowser.setScreenCaptureListener(this);
            Xj3DNavigationUIManager naivagtion = saiBrowser.getNavigationManager();
            naivagtion.addNavigationUIListener(this);
        }

//        ((Xj3DBrowser)x3dBrowser).setMinimumFrameInterval(40);
        ((Xj3DBrowser)x3dBrowser).setMinimumFrameInterval(100);

        modelMap = new IntHashMap(100);

        String[] componentList = {"PickingSensor"};
        int[] componentLevelList = {3};

        exporter = new X3DExporter("3.2", "Immersive", componentList, componentLevelList);

        ViewManager.getViewManager().addView(this);

        mainScene = null;

        /////////////////////////////////////////////////////////////////////////////
        // load the initial world
        errorReporter.messageReport("Loading inital world: " + initialWorld);

        x3dBrowser.loadURL(new String[] { initialWorld }, params);

    }

    /**
     * Return the X3D component in use.
     *
     * @return The component
     */
    public X3DComponent getX3DComponent() {
        return x3dComp;
    }

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {
        return null;
        // ignore
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        // ignore
    }

    //----------------------------------------------------------
    // Methods required by Runnable
    //----------------------------------------------------------

    public void run() {
        model.reissueEvents(this);

        // Handle issueing of ChangeViewCommands once navigation has stopped */
        while(!terminate) {
            try {
                Thread.sleep(CHANGE_VIEW_TIME);
            } catch(InterruptedException e) {
                // ignore
            }

            if (transactionID != 0 && (System.currentTimeMillis() - lastNavigationTime) >= CHANGE_VIEW_TIME) {
                // TODO: Might have threading issues with transient usage of newPosition/newOrientation vars
                if (navMode == MODE_MASTER) {
                    tmpPosD[0] = newPosition[0];
                    tmpPosD[1] = newPosition[1];
                    tmpPosD[2] = newPosition[2];

                    ChangeViewCommand cmd = new ChangeViewCommand(model, transactionID, tmpPosD,
                        newOrientation, (float) Math.PI / 4);
                    model.applyCommand(cmd);

                    transactionID = 0;
                }
            }
        }
    }

    //----------------------------------------------------------
    // Methods required by WorldLoaderListener
    //----------------------------------------------------------

    public void newWorldLoaded(X3DScene mainScene) {
        // TODO: this does not handle the initialization for
        // automated elevation configuration. BAD things will
        // surely happen if this method is called.
        this.mainScene = mainScene;
    }

    //----------------------------------------------------------
    // Methods required by View
    //----------------------------------------------------------

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode) {
        navMode = newMode;

        // TODO: This was in setLocation, moved her to non networked worlds wouldn
        // get these viewpoints.  Not tested yet
        // Add master,slaved,free viewpoints

        addNetworkNodes();


        if (navMode == MODE_MASTER) {
            ChangeMasterCommand cmd = new ChangeMasterCommand(model, viewID);
            model.applyCommand(cmd);

            posChanged.getValue(newPosition);
            oriChanged.getValue(newOrientation);

            errorReporter.messageReport("*** newPos: " + java.util.Arrays.toString(newPosition));

            // Move all slaved VP's to Masters current location
            tmpPosD[0] = newPosition[0];
            tmpPosD[1] = newPosition[1];
            tmpPosD[2] = newPosition[2];

            // TODO: This doesn't work if the user has never moved.
            transactionID = model.issueTransactionID();
            ChangeViewCommand cmd2 = new ChangeViewCommand(model, transactionID, tmpPosD,
                newOrientation, (float) Math.PI / 4);
            transactionID = 0;

            masterViewpoint.setPosition(newPosition);

            lastPos[0] = newPosition[0];
            lastPos[1] = newPosition[1];
            lastPos[2] = newPosition[2];

            masterViewpoint.setOrientation(newOrientation);

            model.applyCommand(cmd2);

        }
    }

    /**
     * Get the rendering component.
     *
     * @return The rendering component
     */
    public JComponent getComponent() {
        return this;
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
    public void entityAdded(boolean local, Entity entity) {
        int entityID = entity.getEntityID();

        double[] position = new double[] {0, 0, 0};
        float[] rotation = new float[] {0, 0, 0, 0};
        float[] scale = new float[] {1, 1, 1};

        if (entity instanceof PositionableEntity) {

            ((PositionableEntity)entity).getPosition(position);
            ((PositionableEntity)entity).getRotation(rotation);
            ((PositionableEntity)entity).getScale(scale);
        }

        int type = entity.getType();


        if (type == Entity.TYPE_WORLD) {

            String url = entity.getModelURL();

            modelMap.clear();
            entityMap.clear();

            if (url.equals(currentURL))
                return;

            // Remove the initial world from the root nodes if it exists
            if(mainScene != null){
                X3DNode[] rootNodeList = mainScene.getRootNodes();

                for(int i = 0; i < rootNodeList.length; i++){

                    if(rootNodeList[i].getNodeName().equals("Transform")){

                        Transform transform = (Transform) rootNodeList[i];

                        X3DNode[] children = new X3DNode[transform.getNumChildren()];
                        transform.getChildren(children);

                        if(children.length == 1 && children[0].getNodeName().equals("Shape")){
                            mainScene.removeRootNode(rootNodeList[i]);
                        }
                    }
                }
            }

            // load the scene
            if (contentGroup != null) {
                // clean up the content nodes
//                X3DNode[] nodes = new X3DNode[contentGroup.getNumChildren()];
//                contentGroup.removeChildren(nodes);

                // Remove the content group and recreate it to clear the previous scene.
                mainScene.removeRootNode(contentGroup);

                contentGroup = (Group)mainScene.createNode("Group");
                mainScene.addRootNode(contentGroup);

                Group group = (Group)mainScene.createNode("Group");
                MFNode children = (MFNode) group.getField("children");

                generateX3D(entityID, children);
                contentGroup.addChildren(new X3DNode[] {group});
            }

            currentURL = url;


            return;
        }


        X3DNode t = (X3DNode) modelMap.get(entityID);

        if (t != null) {
            // ignore dups as we expect them in a networked environment
            return;
        }

        if (mainScene != null) {
            Group group = (Group)mainScene.createNode("Group");
            MFNode children = (MFNode) group.getField("children");

            generateX3D(entityID, children);

            contentGroup.addChildren(new X3DNode[] {group});
            //mainScene.addRootNode(group);

            modelMap.put(entityID, group);

            if (!(entity instanceof SegmentableEntity)) {
                if(pickingType == PickingType.PRIMITIVE){
                    ;
                } else if (pickingType == PickingType.ELEVATION) {
                    queueConfigElevationRequest(new EntityConfigData(entityID, position));
                } else if(pickingType != PickingType.NONE){
                    // TODO: fix targetList
                    targetListLP.add(group);
                    pickTargetReady = false;
                    lpPickNode.setPickTarget((X3DNode[])targetListLP.toArray(new X3DNode[targetListLP.size()]));
                }
            }
        }

        entity.addEntityPropertyListener(this);
        entity.addEntityChildListener(this);

        entityMap.put(entity.getEntityID(), entity);

        /////////////////////////////////
        if(entity instanceof DefaultSegmentableEntity){

        	DefaultSegmentableEntity dse = (DefaultSegmentableEntity) entity;

            ArrayList<VertexEntity> vertexEntityList = dse.getVertices();

            if(vertexEntityList != null){
                for(Iterator<VertexEntity> vertexEntityIterator = vertexEntityList.iterator(); vertexEntityIterator.hasNext();){

                    VertexEntity vertexEntity = vertexEntityIterator.next();
                    vertexEntity.addEntityPropertyListener(this);
     //             vertexEntity.addEntityChildListener(this);
                    int vertexEntityID = vertexEntity.getEntityID();
                    if(!entityMap.containsKey(vertexEntityID)){
                        entityMap.put(vertexEntityID, vertexEntity);
                    }
                }
            }
        }
        ////////////////
    }

    /**
     * An entity was removed.
     *
     * @param entityID The id
     */
    public void entityRemoved(boolean local, Entity entity) {
        int entityID = entity.getEntityID();
        X3DNode transform = (X3DNode) modelMap.get(entityID);

        if (transform == null) {
            errorReporter.messageReport("Entity not found for removal in Watcher3D: " + entity.getEntityID());
            return;
        }

        X3DNode[] nodes = new X3DNode[] {transform};
        contentGroup.removeChildren(nodes);
        //mainScene.removeRootNode(transform);
        modelMap.remove(entityID);

        pickBoxGeometryMap.remove(entityID);
        pickShapeMaterialMap.remove(entityID);
        pickTransformGeometryMap.remove(entityID);
        pickPrimitiveNodeMap.remove(entityID);
        pickTargetProxyMap.remove(entityID);

        // remove the entity from the pickable target collection,
        // note that segmented entities should never get on the list
        if (!(entity instanceof SegmentableEntity)) {

            if(pickingType == PickingType.ELEVATION){
                lpPickNode.setEnabled(false);
                if (targetListLP.remove(transform)) {

                    pickTargetReady = false;

                    lpPickNode.setPickTarget(
                            (X3DNode[])targetListLP.toArray(
                                    new X3DNode[targetListLP.size()])
                                    );
                }

            }
        }

        entity.removeEntityPropertyListener(this);
        entity.removeEntityChildListener(this);

        entityMap.remove(entity.getEntityID());

    }

    /**
     * An entity was associated with another.
     *
     * @param parent The parent entityID
     * @param child The child entityID
     */
    public void entityAssociated(boolean local, int parent, int child) {
        // ignore
    }

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode) {
        // Retained mode is a bit harder, setup a switch?
        errorReporter.messageReport("Helper display not implemented on Watcher3DView");
    }

    /**
     * Get the viewID.  This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID() {
        return viewID;
    }

    /**
     * An entity was unassociated with another.
     *
     * @param parent The parent entityID
     * @param child The child entityID
     */
    public void entityUnassociated(boolean local, int parent, int child) {
        // ignore
    }

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        if (navMode == MODE_SLAVED) {
            //errorReporter.messageReport("Got new pos: " + Arrays.toString(pos));

            tmpPosF[0] = (float) pos[0];
            tmpPosF[1] = (float) pos[1];
            tmpPosF[2] = (float) pos[2];

            slavedViewpoint.setPosition(tmpPosF);

            slavedViewpoint.setOrientation(rot);

            // TODO: Ignore fov for now
        }
    }

    /**
     * The master view has changed.
     *
     * @param view The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
        addNetworkNodes();

        // TODO: Wait for next frame, not sure how to do correctly here
        try { Thread.sleep(100); } catch (Exception e) {}

        if (this.viewID == viewID) {
            masterViewpoint.setBind(true);

        } else if (navMode == MODE_MASTER) {
            navMode = MODE_SLAVED;
            slavedViewpoint.setBind(true);

        } else if (navMode == MODE_SLAVED) {
            // rebind to vp
            slavedViewpoint.setBind(true);
        }
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
        // TODO: clear model
    }

    // ----------------------------------------------------------
    // Methods required by EntityPropertyListener interface
    // ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void propertyUpdated(
            int entityID,
            String propertySheet,
            String propertyName, boolean ongoing) {

        Entity entity = entityMap.get(entityID);

        // check to see if the entity is of the type PositionableEntity
        if (entity instanceof PositionableEntity) {

            if (entity.getType() == Entity.TYPE_WORLD) {
                // TODO: ignore.  What should we do?
                return;
            }

            PositionableEntity posEntity = (PositionableEntity)entity;

            if (propertyName.equals(PositionableEntity.POSITION_PROP)) {
                // get the current position
                double[] position = new double[3];
                posEntity.getPosition(position);
                float[] pos = new float[] {(float)position[0], (float)position[1], (float)position[2]};

                // get the transform node
                X3DNode group = (X3DNode) modelMap.get(entityID);

                // fail if null, vertices do this for now.
                if (group == null){

                    // If we have a vertex entity, updated the representative geometry
                     if(entity instanceof VertexEntity){

                            VertexEntity vertexEntity = (VertexEntity) entity;
                            int vertexLocation = vertexEntityIDMap.get(vertexEntity.getEntityID());

                            if(vertexList != null){

                                // First vertex
                                vertexList[vertexLocation*6] = pos[0];
                  //            vertexList[vertexLocation*6+1] = 2.15f;
                                vertexList[vertexLocation*6+2] = pos[2];

                                //Second vertex
                                vertexList[vertexLocation*6+3] = pos[0];
                  //            vertexList[vertexLocation*6+4] = 0.0f;
                                vertexList[vertexLocation*6+5] = pos[2];

                                coordinates.setValue((vertexList.length/3), vertexList);

                                float[] volumeSize = new float[3];
                                float[] center = new float[3];

                                getMultiSegmentBoundingVolume(volumeSize, center);

                                if(multiSegmentCollisionVolumeTransform != null){
                                    multiSegmentCollisionVolumeTransform.setTranslation(center);
                                }

                                if(multiSegmentBoundingVolumeBoxSize != null && multiSegmentTransformScale != null){

                                    float[] scale = new float[3];

                                    scale[0] = volumeSize[0]/multiSegmentBoundingVolumeBoxSize[0];
                                    scale[1] = volumeSize[1]/multiSegmentBoundingVolumeBoxSize[1];
                                    scale[2] = volumeSize[2]/multiSegmentBoundingVolumeBoxSize[2];
                                    multiSegmentTransformScale.setValue(scale);

                                }
                            }

                            performWorldCollisionCheck();

                    }
                    return;
                }

                // update the position information
                if(pickTransformGeometryMap != null &&
                    pickTargetProxyMap != null){

                    Transform trans = (Transform) pickTransformGeometryMap.get(entityID);

                    if(trans != null){
                        SFVec3f positionField = (SFVec3f)trans.getField("translation");
                        positionField.setValue(pos);
                    }

                    trans =  (Transform) pickTargetProxyMap.get(entityID);

                    if(trans != null){
                        SFVec3f positionField = (SFVec3f)trans.getField("translation");
                        positionField.setValue(pos);
                    }
                }

                if (pickingType == PickingType.ELEVATION) {

                    boolean moveProcessed = false;
                    int num = completeQueue.size();
                    // check to see if this move command is a result of an elevation
                    // change caused by a pick. if so - ignore it.
                    for (int i=0; i<num; i++) {
                        EntityConfigData data = completeQueue.get(i);
                        if ((data.entityID == entityID)&&
                            (data.vertexID == -1)&&
                            (Arrays.equals(data.position, position))) {
                            moveProcessed = true;
                            completeQueue.remove(i);
                            break;
                        }
                    }
                    if(!moveProcessed) {
                        synchronized( this ) {
                            // if any picks are queued for this entity, delete them,
                            // as they are obsolete.
                            num = pickQueue.size( );
                            for(int i = num-1; i>=0; i--) {
                                EntityConfigData data = pickQueue.get(i);
                                if (data.entityID == entityID) {
                                    pickQueue.remove(i);
                                }
                            }
                            // remove this entity from the target list, and queue the pick request
                            if (targetListLP.remove(group)) {
                                pickTargetReady = false;
                                lpPickNode.setPickTarget((X3DNode[])targetListLP.toArray(new X3DNode[targetListLP.size()]));
                            }
                            queueConfigElevationRequest(new EntityConfigData(entityID, position));
                        }
                    }
                } else if (pickingType == PickingType.PRIMITIVE) {

                    if(!forceCollisionTargetUpdate){
                        performWorldCollisionCheck();
                    }
           }

            } else if (propertyName.equals(PositionableEntity.ROTATION_PROP)) {

                // get the current position
                float[] rotation = new float[4];
                posEntity.getRotation(rotation);

                // get the transform node
                X3DNode group = (X3DNode) modelMap.get(entityID);

                MFNode children = (MFNode) group.getField("children");

                // update the information
                Transform trans = (Transform)children.get1Value(0);

                SFRotation rotationField = (SFRotation)trans.getField("rotation");
                rotationField.setValue(rotation);

            } else if (propertyName.equals(PositionableEntity.SCALE_PROP)) {

                // get the current position
                float[] scale = new float[3];
                posEntity.getScale(scale);

                // get the transform node
                X3DNode group = (X3DNode) modelMap.get(entityID);
                MFNode children = (MFNode) group.getField("children");

                // update the information
                Transform trans = (Transform)children.get1Value(0);
                SFVec3f scaleField = (SFVec3f)trans.getField("scale");
                scaleField.setValue(scale);

            }

        }
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param propertyUpdates - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
//        System.out.println("propertiesUpdated()");
        // ignored
    }


    //----------------------------------------------------------
    // Methods defined by X3DFieldEventListener
    //----------------------------------------------------------

    /**
     * Handle field changes from the X3D world.
     *
     * @param evt The event
     */
    public void readableFieldChanged(X3DFieldEvent evt) {

        Object src = evt.getSource();

        if (src.equals(posChanged)) {
            fieldCnt++;

            posChanged.getValue(newPosition);
            lastNavigationTime = System.currentTimeMillis();
        } else if (src.equals(oriChanged)) {
            fieldCnt++;

            oriChanged.getValue(newOrientation);
            lastNavigationTime = System.currentTimeMillis();
        } else if (src.equals(pickTarget)) {
            // picking targets have been established, enable the pick
            // processing to happen on the next pickedPoint event
            pickTargetReady = true;

        } else if (src.equals(pickedPoint)) {

            Command cmd = null;
            EntityConfigData data = (EntityConfigData)pickedPoint.getUserData();
//System.out.println("pickedPoint");
            if (data != null) {

                synchronized(this) {
                    // check to see if a new request for the same entity has been
                    // queued while we waited for the picking and target geometry
                    // to become ready.
                    int queued = pickQueue.size( );
                    EntityConfigData next = null;
                    for(int i = queued-1; i>=0; i--) {
                        EntityConfigData queued_data = pickQueue.get(i);
                        if ((data.entityID == queued_data.entityID)&&
                            (data.vertexID == queued_data.vertexID)) {
                            // if a new pick request is pending on the same entity,
                            // remove it from the queue, make it the active request,
                            // and don't bother to process this one
                            queued_data = pickQueue.remove(i);
                            if (next == null) {
                                next = queued_data;
                            }
                            break;
                        }
                    }
                    if (next != null) {
                        // configPick() will set pickGeometryReady to false,
                        // and cause the following processing loop to be bypassed
                        configPick(next);
                    }
                }

                if (pickGeometryReady && pickTargetReady) {

                    // clear for the next pick
                    pickedPoint.setUserData(null);

                    // get the picked point
                    int num = lpPickNode.getNumPickedPoint();
                    float[] pickPoint = new float[num*3];
                    lpPickNode.getPickedPoint(pickPoint);

                    // if there are multiple, use the highest elevation point
                    if (num > 1) {
                        float max_elevation = Float.NEGATIVE_INFINITY;
                        int idx = 0;
                        for( int i=0; i<num; i++) {
                            float y = pickPoint[i*3+1];
                            if (y > max_elevation) {
                                idx = i;
                                max_elevation = y;
                            }
                        }
                        float[] max_elevation_point = new float[3];
                        System.arraycopy(pickPoint, idx*3, max_elevation_point, 0, 3);
                        pickPoint = max_elevation_point;
                    }

                    X3DNode transform = (X3DNode)modelMap.get(data.entityID);

                    // cast to double for comparison with the original position
                    double[] pickPoint_d = new double[]{
                        (double)pickPoint[0],
                        (double)pickPoint[1],
                        (double)pickPoint[2]};

                    // if the picked point elevation differs from the initial position,
                    // issue the move commands to pass the data on
                    if (data.position[1] != pickPoint_d[1]) {
                        float[] scale = new float[3];
                        Entity entity = model.getEntity(data.entityID);
                        PositionableEntity posEntity;

                        if (entity instanceof PositionableEntity) {
                            posEntity = (PositionableEntity) entity;
                            posEntity.getScale(scale);

                            pickPoint_d[1] += scale[1] / 2;
                        }

                        if (data.isSegmented()) {
                            // since the segmented entity is created 'enmass', a vertex's
                            // elevation cannot be changed directly by it's Transform

                            // TODO: need to get the entity and vertex from the model here
                            //cmd = new MoveVertexCommand(
                            //    model.issueTransactionID(),
                            //    data.entityID,
                            //    data.vertexID,
                            //    pickPoint_d,
                            //    data.position);
                        } else {
                            // non-segmented entities have a unique associated Transform,
                            // and can be configured directly

                            //Alan: why do this directly?  latency issue, currently wrong place in hierarchy
                            //((Transform)transform).setTranslation(pickPoint);

                            cmd = new MoveEntityCommand(
                                model,
                                model.issueTransactionID(),
                                (PositionableEntity)entity,
                                pickPoint_d,
                                data.position);
                        }
                        // recreate the data object to contain the picked point, this will be used
                        // in the subsequent entityMoved() command processing to identify the
                        // source of the move as this, and to prevent it from picking again.
                        data = new EntityConfigData(data.entityID, data.vertexID, pickPoint_d);
                    }
                    // place the node into the pick targets once a pick is complete, unless:
                    // 1) the entity is segmented, segmented entities are not picked
                    // 2) the entity is already on the list (being defensive - this shouldn't happen)
                    if (!data.isSegmented() && !targetListLP.contains(transform)) {
                        targetListLP.add(transform);
                        pickTargetReady = false;
                        lpPickNode.setPickTarget((X3DNode[])targetListLP.toArray(new X3DNode[targetListLP.size()]));
                    }
                }
                // manage the pickQueue
                synchronized (this) {
                    if (pickQueue.size() != 0) {
                        // if another pick is ready, configure it
                        configPick(pickQueue.remove(0));
                    } else {
                        // otherwise, return the picker state to the idle condition
                        pickInProgress = false;
                        pickGeometryReady = false;
                        lpPickNode.setEnabled(false);
                    }
                }
            }
            // process any move commands that have been generated
            if ( cmd != null ) {
                completeQueue.add(data);
                cmd.setErrorReporter(errorReporter);
                model.applyCommand(cmd);
            }
        } else if (src.equals(linePickIsActive)) {
            pickNodeIsActive = lpPickNode.getIsActive( );
            if (!pickNodeIsActive) {
                if (pickInProgress && pickGeometryReady) {
                    pickedPoint.setUserData(null);
                }
                pickGeometryReady = false;
                pickInProgress = false;
                pickQueue.clear();
            }
        } else if (src.equals(time)) {
            // the initial time event occurs when a new location is loaded,
            // after the browser initialized event
            if (!worldLoaded) {

                //timeSensor.setEnabled(false);
                initializePickTargets();
                //x3dBrowser.nextViewpoint();
                x3dBrowser.firstViewpoint();

                worldLoaded = true;

            } else if (pickInProgress && pickGeometryReady && !pickNodeIsActive) {
                // a pick was initiated, the pick geometry is ready,
                // but not intersecting a target
                if (pickWatchdog-- <= 0) {
                    EntityConfigData data = (EntityConfigData)pickedPoint.getUserData();
                    pickedPoint.setUserData(null);
                    if (!data.isSegmented()) {
                        X3DNode transform = (X3DNode)modelMap.get(data.entityID);
                        if(!targetListLP.contains(transform)) {
                            targetListLP.add(transform);
                            pickTargetReady = false;
                            lpPickNode.setPickTarget((X3DNode[])targetListLP.toArray(new X3DNode[targetListLP.size()]));
                        }
                    }
                    // queue the next pick
                    synchronized (this) {
                        if (pickQueue.size() != 0) {
                            // if another pick is ready, configure it
                            configPick(pickQueue.remove(0));
                        } else {
                            // otherwise, return the picker state to the idle condition
                            pickInProgress = false;
                            pickGeometryReady = false;
                            lpPickNode.setEnabled(false);
                        }
                    }
                }
            }

            //------------------------------------------------------------
            // Complete adding primitive picking nodes to new geometry
            //------------------------------------------------------------
            if(forceCollisionTargetUpdate){

                forceCollisionTargetUpdate = false;

                updatePrimitivePickingTargets();

                forcePickingCheck = true;

            } else if (forcePickingCheck) {

                forcePickingCheck = false;

                performWorldCollisionCheck();

            } else if (multiSegmentQueue.size() > 0 && !forceCollisionTargetUpdate) {

                if(pauseBeforeAddingMultiSegmentPickingVolume) {

                    pauseBeforeAddingMultiSegmentPickingVolume = false;
                } else {

                    generateMultiSegmentGeometry(multiSegmentQueue.get(0));

                    if(multiSegmentQueue != null && multiSegmentQueue.size() > 0) {
                        multiSegmentQueue.remove(0);
                    }

                    pauseBeforeAddingMultiSegmentPickingVolume = true;

                    if(multiSegmentQueue.size() < 1){
                        forceCollisionTargetUpdate = true;
                    }

                }
            } else if (pickingNodeQueue.size() > 0 && !forceCollisionTargetUpdate) {

                if(pauseBeforeAddingPickingVolume) {

                    pauseBeforeAddingPickingVolume = false;

                } else {

                    generatePickingGeometry(pickingNodeQueue.get(0));

                    pickingNodeQueue.remove(0);

                    pauseBeforeAddingPickingVolume = true;

                    if(pickingNodeQueue.size() < 1){
                        forceCollisionTargetUpdate = true;
                    }
                }
            }
        } else if (src.equals(coordPoint)) {
            // each relocation of the pick line will cause this,
            // indicating that the pick geometry is ready
            pickGeometryReady = true;
            // set the watchdog, in case the pick geometry
            // does not interset anything at the coordinate
            pickWatchdog = 2;
        }

        // After both have changed send

        if (fieldCnt == 2) {

            if (navMode == MODE_MASTER) {
                if (transactionID == 0) {
                    transactionID = model.issueTransactionID();
                }

                tmpPosD[0] = newPosition[0];
                tmpPosD[1] = newPosition[1];
                tmpPosD[2] = newPosition[2];

                float factor = 1000f / (System.currentTimeMillis() - lastFrameTime);
                linearVelocity[0] = (float) (newPosition[0] - lastPos[0]) * factor;
                linearVelocity[1] = (float) (newPosition[1] - lastPos[1]) * factor;
                linearVelocity[2] = (float) (newPosition[2] - lastPos[2]) * factor;

                lastFrameTime = System.currentTimeMillis();

                angularVelocity[0] = newOrientation[0] - lastOri[0];
                angularVelocity[1] = newOrientation[1] - lastOri[1];
                angularVelocity[2] = newOrientation[2] - lastOri[2];

                lastPos[0] = newPosition[0];
                lastPos[1] = newPosition[1];
                lastPos[2] = newPosition[2];

                lastOri[0] = newOrientation[0];
                lastOri[1] = newOrientation[1];
                lastOri[2] = newOrientation[2];
                lastOri[3] = newOrientation[3];

//System.out.println("Sending pos: " + Arrays.toString(newPosition) + " ori: " + Arrays.toString(newOrientation));
                ChangeViewTransientCommand cmd = new ChangeViewTransientCommand(model, transactionID, tmpPosD,
                    newOrientation, linearVelocity, angularVelocity, (float) Math.PI / 4);
                model.applyCommand(cmd);
            }

            fieldCnt = 0;
        }
    }

    //----------------------------------------------------------
    // Methods defined by EntityChildListener
    //----------------------------------------------------------

    /**
     * A child was added.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     */
    public void childAdded(int parent, int child) {

        Entity entity = model.getEntity(parent);
        Entity childEntity = entity.getChildAt(entity.getChildIndex(child));

        // register the property listener
        childEntity.addEntityPropertyListener(this);

        entityMap.put(childEntity.getEntityID(), childEntity);

    }

    /**
     * A child was removed.
     *
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {

        Entity entity = model.getEntity(parent);
        int childIndex = entity.getChildIndex(child);

        if(childIndex > 0){
            Entity childEntity = entity.getChildAt(childIndex);

            // register the property listener
            childEntity.removeEntityPropertyListener(this);

            entityMap.remove(childEntity.getEntityID());
        }
    }

    /**
     * A child was inserted.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public void childInsertedAt(int parent, int child, int index) {

        Entity entity = model.getEntity(parent);
        Entity childEntity = entity.getChildAt(index);

        // register the property listener
        childEntity.addEntityPropertyListener(this);

        entityMap.put(childEntity.getEntityID(), childEntity);

    }

    //---------------------------------------------------------
    // Methods defined by BrowserListener
    //---------------------------------------------------------

    /** The Browser Listener. */
    public void browserChanged( final BrowserEvent be ) {

        final int id = be.getID( );

        switch (id) {
        case BrowserEvent.INITIALIZED:

            X3DScene initialScene =
                (X3DScene)x3dBrowser.getExecutionContext();

            vertexEntityIDMap = new HashMap<Integer, Integer>();

            /////////////////////////////////////////////////////////////////////////////
            // validate that the loaded scene meets the minimum requirements.
            // if not, create one that does and populate it with the nodes from
            // the initial scene.
            int version_major = 3;
            int version_minor = 2;
            ProfileInfo profileInfo = x3dBrowser.getProfile("Immersive");
            ComponentInfo[] componentInfo =
                new ComponentInfo[]{x3dBrowser.getComponent("PickingSensor", 1)};

            boolean sceneIsCompatible = SceneUtils.validateSceneCompatibility(
                initialScene,
                version_major,
                version_minor,
                profileInfo,
                componentInfo);

            if (sceneIsCompatible) {
                mainScene = initialScene;
            } else {
                // TODO: should check the initial scene for additional
                // included components that are required......
                mainScene = x3dBrowser.createScene(profileInfo, componentInfo);

                SceneUtils.copyScene(initialScene, mainScene);
            }

            /////////////////////////////////////////////////////////////////////////////
            // build the picker and the pick geometry

            //----------------------------------------------------------------
            // build the picker and the pick geometry
            //----------------------------------------------------------------

            if(pickingType == PickingType.ELEVATION){

                lpPickNode = (LinePicker)mainScene.createNode("LinePicker");

                lpPickNode.setIntersectionType("GEOMETRY");
                lpPickNode.setSortOrder("ALL");

                linePickIsActive = (SFBool)lpPickNode.getField("isActive");
                linePickIsActive.addX3DEventListener(this);

                pickedPoint = (MFVec3f)lpPickNode.getField("pickedPoint");
                pickedPoint.addX3DEventListener(this);

                pickTarget = (MFNode)lpPickNode.getField("pickTarget");
                pickTarget.addX3DEventListener(this);

                IndexedLineSet ils = (IndexedLineSet)mainScene.createNode("IndexedLineSet");
                coord = (Coordinate)mainScene.createNode("Coordinate");
                coordPoint = (MFVec3f)coord.getField("point");
                coordPoint.addX3DEventListener(this);

                coord.setPoint(line_point);
                ils.setCoord(coord);
                ils.setCoordIndex(line_point_indicies);

                lpPickNode.setPickingGeometry(ils);
                lpPickNode.setEnabled(false);

                mainScene.addRootNode(lpPickNode);

                if ( SHOW_PICK_LINE ) {
                    Shape shape = (Shape)mainScene.createNode("Shape");
                    shape.setGeometry(ils);

                    Group group = (Group)mainScene.createNode("Group");
                    group.setChildren( new X3DNode[]{shape} );

                    mainScene.addRootNode(group);
                }

            } else if(pickingType == PickingType.PRIMITIVE){
                pickBoxGeometryMap = new HashMap<Integer, X3DNode>();
                pickShapeMaterialMap = new HashMap<Integer, X3DNode>();
                pickTransformGeometryMap = new HashMap<Integer, X3DNode>();
                pickPrimitiveNodeMap = new HashMap<Integer, X3DNode>();
                pickTargetProxyMap = new HashMap<Integer, X3DNode>();

            } else {

                System.out.println("No matching pick node defined in Watcher3DView");

            }

            //----------------------------------------------------------------
            // clear the pick target collection when loading a new world
            //----------------------------------------------------------------
            targetListLP = null;

            /////////////////////////////////////////////////////////////////////////////
            // setup a time sensor for tracking cycles through the event model
            // on initialization

            timeSensor = (TimeSensor)mainScene.createNode("TimeSensor");
            timeSensor.setLoop(true);
            timeSensor.setEnabled(true);

            time = (SFTime)timeSensor.getField("time");
            time.addX3DEventListener(this);

            mainScene.addRootNode(timeSensor);

            // Create content group
            contentGroup = (Group)mainScene.createNode("Group");
            mainScene.addRootNode(contentGroup);

            isBrowserInitalized = true;

            break;
        case BrowserEvent.SHUTDOWN:

        }

    }

    //----------------------------------------------------------
    // Methods defined by ViewConfig
    //----------------------------------------------------------

    /**
     * Enable the automated configuration of the elevation for added
     * and moved entities.
     *
     * @param enable The enabled state
     */
    public void setConfigElevation(boolean enable) {
        if(enable) {
            pickingType = PickingType.ELEVATION;
        } else {
            pickingType = PickingType.NONE;
        }
    }

    /**
     * Return the state of automated configuration of the elevation
     * for added and moved entities.
     *
     * @return The enabled state
     */
    public boolean getConfigElevation() {
        if(pickingType == PickingType.ELEVATION){
            return true;
        }

        return false;
    }

    //----------------------------------------------------------
    // Methods defined by Xj3DScreenCaptureListener
    //----------------------------------------------------------

    /**
     * Notification of a new screen capture presented as an image. A new
     * image instance will be generated for each frame.
     *
     * @param img The screen captured image
     */
    public void screenCaptured(BufferedImage img) {

        String dir = System.getProperty("user.home");

        // Now save the gray colored image.
        try {
            ColorConvertOp grayOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

            BufferedImage grayImage = grayOp.filter(img, null);

            String name = dir + File.separator + "thumbnail_gray_" + thumbnailViewpointIndex + 1 + ".png";
            thumbnailNames.add(name);
            File outputFile = new File(name);
            ImageIO.write(grayImage, "PNG", outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // First save the colored image.
        try {

            String name = dir + File.separator + "thumbnail_color_" + thumbnailViewpointIndex + 1 + ".png";
            thumbnailNames.add(name);
            File outputFile = new File(name);
            ImageIO.write(img, "PNG", outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        thumbnailViewpointIndex++;

        if(thumbnailViewpointIndex < viewpointList.size()) {
            // Capture one frame from the current viewpoint and save that as a thumbnail
            if(x3dBrowser instanceof SAIBrowser) {

                SAIBrowser saiBrowser = (SAIBrowser)x3dBrowser;
                Xj3DNavigationUIManager navigation = saiBrowser.getNavigationManager();

                saiBrowser.nextViewpoint();
                saiBrowser.firstViewpoint();
                navigation.selectViewpoint(viewpointList.get(thumbnailViewpointIndex));
            }
        }
        else {
            for(int i = 0; i < thumbnailListeners.size(); i++) {
                thumbnailListeners.get(i).thumbnailCreated(thumbnailNames);
                captureFrame = false;
            }
        }
    }

    //----------------------------------------------------------
    // Methods defined by Xj3DNavigationUIListener
    //----------------------------------------------------------

    /**
     * The list of active viewpoints have been changed to this new list. A new
     * list can be given due to either a new world being loaded, or the active
     * navigation layer has been changed. This method is not called if a single
     * viewpoint is added or removed from the scene.
     * <p>
     *
     * If the scene contains no viewpoints at all (except the default bindable),
     * then this will be called with a null parameter.
     * <p>
     *
     * On scene or navigation layer change, it is guaranteed that this method
     * will be called before the notification of the actual bound viewpoint.
     *
     * @param An array of exactly the number of viewpoints in the list
     */
    public void availableViewpointsChanged(Xj3DViewpoint[] viewpoints) {
        // Do nothing.

    }

    /**
     * Notification that the selected viewpoint has been changed to this
     * new instance. There are many different reasons this could happen -
     * new node bound, world changed or even active navigation layer has
     * changed and this is the default in that new layer.
     * <p>
     *
     * If the file contains no viewpoints or the default viewpoint is
     * bound (due to unbinding all real viewpoints on the stack) then this
     * will be called with a null parameter.
     * <p>
     *
     * It is guaranteed that this will always contain something from the
     * currently active viewpoint list. If all change, that callback will be
     * called before this one, to ensure consistency.
     *
     * @param vp The viewpoint instance that is now selected or null
     */
    public void selectedViewpointChanged(Xj3DViewpoint vp) {

        if(captureFrame)
            ((SAIBrowser)x3dBrowser).captureFrames(1);
    }

    /**
     * Notification that this viewpoint has been appended to the list of
     * available viewpoints.
     *
     * @param vp The viewpoint instance that was added
     */
    public void viewpointAdded(Xj3DViewpoint vp) {
        viewpointList.add(vp);
    }

    /**
     * Notification that this viewpoint has been removed from the list of
     * available viewpoints.
     *
     * @param vp The viewpoint instance that was added
     */
    public void viewpointRemoved(Xj3DViewpoint vp) {
        viewpointList.remove(vp);
    }

    /**
     * The list of available navigation states has changed to this new list.
     * The list of states corresponds to the X3D-specification defined list
     * and any additional browser-specific state names. If a state is not
     * listed in this array, then the ability to activate it should be
     * disabled (eg greying out the button that represents it). If no states
     * are currently defined, this will contain the default string "NONE",
     * which corresponds to disabling all user selection of navigation and
     * even the ability to navigate.
     *
     * @param states An array of the available state strings.
     */
    public void availableNavigationStatesChanged(String[] states) {

    }

    /**
     * Notification that the currently selected navigation state has changed
     * to this new value. Selection may be due to the UI interaction,
     * courtesy of a node being bound or the active navigation layer has
     * changed.
     *
     * @param state The name of the state that is now the active state
     */
    public void selectedNavigationStateChanged(String state) {

    }

    //----------------------------------------------------------
    // Local Methods
    //-------------------- -------------------------------------

    /**
     * Generate the 3D scene for a entity.
     *
     * @param entityID The entityID to generate
     * @return A scene containing the entity
     */
    private X3DScene generateX3DScene(int entityID,
                                    MFNode children,
                                    ProfileInfo profileInfo,
                                    ComponentInfo[] componentInfo){

        StringWriter writer = new StringWriter(1024);
        String worldURL = x3dBrowser.getExecutionContext().getWorldURL();

        int index = worldURL.lastIndexOf("/");
        worldURL = worldURL.substring(0, index + 1);

//System.out.println("worldURL: " + worldURL);

        exporter.export(model, entityID, writer, worldURL);
        String x3d = writer.toString();

        // if the X3D is empty then exit
        if (x3d.equals(""))
            return null;
/*
System.out.println("X3D:" + x3d.length());
System.out.println(x3d);
System.out.println("Done X3D");
*/
        X3DScene scene = null;

        try {
            scene = x3dBrowser.createX3DFromString(x3d);
        } catch (Exception e) {
            System.out.println("Error parsing x3d: " + x3d);
            e.printStackTrace();
        }

        /////////////////////////////////////////////////////////////////////////////
        // validate that the loaded scene meets the minimum requirements.
        // if not, create one that does and populate it with the nodes from
        // the initial scene.
        int version_major = 3;
        int version_minor = 2;

        boolean sceneIsCompatible = SceneUtils.validateSceneCompatibility(
            scene,
            version_major,
            version_minor,
            profileInfo,
            componentInfo);

        if (! sceneIsCompatible) {

            System.out.println("Scene is not compatible");

            // TODO: should check the initial scene for additional
            // included components that are required......
            X3DScene tempScene = x3dBrowser.createScene(profileInfo, componentInfo);

            SceneUtils.copyScene(scene, tempScene);
            scene = tempScene;
        }

        return scene;
    }


    /**
     * Generate the X3D for an entity and replace the children contents with it.
     * The incoming entity must have its X3D profile set to 3.2 in order to use
     * the PrimitivePicker.
     *
     * @param entityID The entity to generate
     * @param children The children to overwrite
     * @param scene The scene to add routes
     */
    private void generateX3D(int entityID, MFNode children) {

        X3DScene scene = null;

        Entity entity = model.getEntity(entityID);

        ProfileInfo profileInfo = null;

        try {
            profileInfo = x3dBrowser.getProfile("Immersive");
        } catch(NotSupportedException nse) {
            System.out.println("Immersive Profile not supported");
            nse.printStackTrace();
        }

        scene = generateX3DScene(entityID, children, profileInfo, null);

        if(entity instanceof SegmentableEntity){

            //------------------------------------------------------------
            // Generate multi-segment geometry
            //------------------------------------------------------------

            X3DNode[] nodes = scene.getRootNodes();

            if(nodes != null && nodes.length > 0){
                multiSegmentQueue.add(new EntityAddedQueueData(entity, nodes[0]));
            }

       } else if(pickingType == PickingType.PRIMITIVE){

            //------------------------------------------------------------
            // Add the box geometry used for primitive picking if primitive
            // picking is requested
            //------------------------------------------------------------

            X3DNode[] nodes = scene.getRootNodes();

            if((nodes.length == 1) && (nodes[0] instanceof Transform)){
                pickingNodeQueue.add(new EntityAddedQueueData(entity, nodes[0]));
            }
        }

        // Copy the scene into the main one
        X3DNode[] nodes = new X3DNode[] {};

        if(scene != null){
            nodes = scene.getRootNodes();
        }

        int len = nodes.length;

        // Nodes must be removed before adding to another scene
        for(int i=0; i < len; i++) {

//            System.out.println("removing node from parsed scene: "+nodes[i].getNodeName());
            scene.removeRootNode(nodes[i]);
        }

        scene = null;

        children.setValue(len, nodes);

    }

    private void getMultiSegmentBoundingVolume(float[] volumeSize, float[] center){

        float[] max = new float[] {0.0f, 0.0f, 0.0f};
        float[] min = new float[] {0.0f, 0.0f, 0.0f};

        if(vertexList.length > 0){
            max = new float[] {vertexList[0], vertexList[1], vertexList[2]};
            min = new float[] {vertexList[0], vertexList[1], vertexList[2]};
        }

        for(int i = 3; i < vertexList.length; i+=3){

            max[0] = Math.max(max[0], vertexList[i]);
            min[0] = Math.min(min[0], vertexList[i]);

            max[1] = Math.max(max[1], vertexList[i+1]);
            min[1] = Math.min(min[1], vertexList[i+1]);

            max[2] = Math.max(max[2], vertexList[i+2]);
            min[2] = Math.min(min[2], vertexList[i+2]);
        }

        volumeSize[0] = max[0] - min[0];
        volumeSize[1] = max[1] - min[1];
        volumeSize[2] = max[2] - min[2];

        center[0] = (max[0]-min[0])/2.0f+min[0];
        center[1] = (max[1]-min[1])/2.0f+min[1];
        center[2] = (max[2]-min[2])/2.0f+min[2];
    }

    /**
     * Generate the mutli-segment geometry with end caps.
     */
    private void generateMultiSegmentGeometry(EntityAddedQueueData data){
        synchronized(this){

            Entity entity = data.getEntity();
            X3DNode node = data.getX3DNode();
            MFNode rootTransformNode = (MFNode) node.getField("children");

            SegmentableEntity segmentableEntity = (SegmentableEntity) entity;
            ArrayList<VertexEntity> vertexEntityList = segmentableEntity.getVertices();

            if(vertexEntityList != null){

                VertexEntity[] vertexEntityArray = new VertexEntity[vertexEntityList.size()];
                vertexEntityList.toArray(vertexEntityArray);

                vertexList = new float[vertexEntityArray.length*6];
                int[] vertexIndexList = new int[vertexEntityArray.length*5];

                /*
                 * Generating an indexed face set that consists of unique vertices
                 * and mapped indexes that create a plane for every two
                 * multi-segment vertices.
                 */
                for(int i = 0; i < vertexEntityArray.length; i++){

                    int vertexEntityID = vertexEntityArray[i].getEntityID();
                    vertexEntityIDMap.put(vertexEntityID, i);

                    double[] posOne = new double[3];
                    vertexEntityArray[i].getPosition(posOne);

                    // First vertex
                    vertexList[i*6] = (float)posOne[0];
                    vertexList[i*6+1] = 2.15f;
                    vertexList[i*6+2] = (float)posOne[2];

                    //Second vertex
                    vertexList[i*6+3] = (float)posOne[0];
                    vertexList[i*6+4] = 0.0f;
                    vertexList[i*6+5] = (float)posOne[2];

                    if(i < (vertexEntityArray.length-1)){
                        vertexIndexList[i*5] = i*2;
                        vertexIndexList[i*5+1] = i*2+1;
                        vertexIndexList[i*5+2] = i*2+3;
                        vertexIndexList[i*5+3] = i*2+2;
                        vertexIndexList[i*5+4] = -1;
                    }

                }

                x3dBrowser.beginUpdate();

                //--------------------------------------------------------
                // Create the shape node
                //--------------------------------------------------------
                X3DNode shape = mainScene.createNode("Shape");

                //--------------------------------------------------------
                // create the appearance & material nodes
                //--------------------------------------------------------
                SFNode appearanceField = (SFNode) shape.getField("appearance");

                X3DNode appearanceNode = mainScene.createNode("Appearance");
                X3DNode materialNode = mainScene.createNode("Material");

                // Set the material color
                SFColor color = (SFColor) materialNode.getField("diffuseColor");
                float[] colorValue = {0.1275f, 0.1275f, 0.441f};
                color.setValue(colorValue);


                // Add the material to the appearance node
                SFNode materialField = (SFNode) appearanceNode.getField("material");
                materialField.setValue(materialNode);

                // Add the appearance node to the shape node
                appearanceField.setValue(appearanceNode);

                //---------------------------------------------------------
                // create indexed face set
                //---------------------------------------------------------
                SFNode shape_geometry = (SFNode) shape.getField("geometry");

                IndexedFaceSet indexedFaceSet = (IndexedFaceSet) mainScene.createNode("IndexedFaceSet");
                indexedFaceSet.setCcw(false);

                SFNode coordinateField = (SFNode) indexedFaceSet.getField("coord");

                X3DCoordinateNode coordinateNode = (X3DCoordinateNode) mainScene.createNode("Coordinate");
                coordinates = (MFVec3f) coordinateNode.getField("point");
                coordinates.setValue((vertexList.length/3), vertexList);

                coordinateField.setValue(coordinateNode);
                indexedFaceSet.setCoordIndex(vertexIndexList);

                // add the indexed face set
                shape_geometry.setValue(indexedFaceSet);

                //--------------------------------------------------------
                // Add the shape transform under the root Transform node
                //--------------------------------------------------------
                rootTransformNode.append(shape);

                //--------------------------------------------------------
                // Create the collision volume
                //--------------------------------------------------------
                float[] volumeSize = new float[] {0.0f, 0.0f, 0.0f};
                float[] center = new float[] {0.0f, 0.0f, 0.0f};

                getMultiSegmentBoundingVolume(volumeSize, center);

                multiSegmentCollisionVolumeTransform = (Transform) mainScene.createNode("Transform");
                multiSegmentCollisionVolumeTransform.setTranslation(center);

                multiSegmentTransformScale = (SFVec3f) multiSegmentCollisionVolumeTransform.getField("scale");

                //--------------------------------------------------------
                // Create the collision volume shape node
                //--------------------------------------------------------
                X3DNode collisionVolumeShape = mainScene.createNode("Shape");

                //--------------------------------------------------------
                // create the collision volume appearance & material nodes
                //--------------------------------------------------------
                SFNode collisionVolumeAppearanceField = (SFNode) collisionVolumeShape.getField("appearance");

                X3DNode collisionVolumeAppearanceNode = mainScene.createNode("Appearance");
                X3DNode collisionVolumeMaterialNode = mainScene.createNode("Material");

                // Set the material color
                SFColor collisionVolumeColor = (SFColor) collisionVolumeMaterialNode.getField("diffuseColor");
                float[] collisionVolumeColorValue = {1.0f, 0.0f, 0.0f};
                collisionVolumeColor.setValue(collisionVolumeColorValue);

                // Set the transparency
                SFFloat collisionTransparency = (SFFloat) collisionVolumeMaterialNode.getField("transparency");
                float collisionTransparencyValue = TRANSPARENCY_OFF_VALUE;
                collisionTransparency.setValue(collisionTransparencyValue);

                // Add the material to the appearance node
                SFNode collisionVolumeMaterialField = (SFNode) collisionVolumeAppearanceNode.getField("material");
                collisionVolumeMaterialField.setValue(collisionVolumeMaterialNode);

                // Add the appearance node to the shape node
                collisionVolumeAppearanceField.setValue(collisionVolumeAppearanceNode);

                //--------------------------------------------------------
                // create the collision volume box node
                //--------------------------------------------------------
                SFNode collisionVolumeGeometry = (SFNode) collisionVolumeShape.getField("geometry");
                X3DNode box = mainScene.createNode("Box");

                // set the box size
                multiSegmentBoundingVolumeBoxSize = new float[3];
                multiSegmentBoundingVolumeBoxSize[0] = volumeSize[0];
                multiSegmentBoundingVolumeBoxSize[1] = volumeSize[1];
                multiSegmentBoundingVolumeBoxSize[2] = volumeSize[2];

                SFVec3f boundingVolumeBoxSize = (SFVec3f) box.getField("size");

                boundingVolumeBoxSize.setValue(volumeSize);

                // add the box to the shape node
                collisionVolumeGeometry.setValue(box);

                //--------------------------------------------------------
                // Add the multi-segment pick node
                //--------------------------------------------------------
                multiSegmentPickNode = (PrimitivePicker) mainScene.createNode("PrimitivePicker");

                multiSegmentPickNode.setIntersectionType("GEOMETRY");
                multiSegmentPickNode.setSortOrder("ALL");

                X3DNode[] targetList = new X3DNode[] {};

                if (pickTransformGeometryMap != null && pickTransformGeometryMap.size() > 0){

                    targetList = (X3DNode[]) (pickTransformGeometryMap.values().toArray(new X3DNode[0]));

                }

                multiSegmentPickNode.setPickingGeometry(box);
//              multiSegmentPickNode.setPickTarget(targetList);

                multiSegmentPickNode.setEnabled(true);

                multiSegmentPickIsActive = (SFBool)multiSegmentPickNode.getField("isActive");
                multiSegmentPickIsActive.addX3DEventListener(this);

                multiSegmentPickedGeometry = (MFNode)multiSegmentPickNode.getField("pickedGeometry");
                multiSegmentPickedGeometry.addX3DEventListener(this);

                //--------------------------------------------------------
                // Add the collision volume to the collision transform
                // and the collision transform the group
                //--------------------------------------------------------

                multiSegmentCollisionVolumeTransform.setChildren(new X3DNode[] {collisionVolumeShape, multiSegmentPickNode});

                Group groupT = (Group)mainScene.createNode("Group");
                MFNode groupTChildren = (MFNode) groupT.getField("children");
                groupTChildren.append(multiSegmentCollisionVolumeTransform);

                contentGroup.addChildren(new X3DNode[] {groupT});

 //             contentGroup.addChildren(new X3DNode[] {collisionVolumeTransform});

                //--------------------------------------------------------
                // Create the end caps
                //--------------------------------------------------------
/*
                // create the appearance & material nodes
                X3DNode endCapAppearanceNode = mainScene.createNode("Appearance");
                X3DNode endCapMaterialNode = mainScene.createNode("Material");

                // Set the material color
                SFColor endCapColor = (SFColor) endCapMaterialNode.getField("diffuseColor");
                float[] endCapColorValue = {0.84f, 0.765f, 0.632f};
                endCapColor.setValue(endCapColorValue);

                // Add the material to the appearance node
                SFNode endCapMaterialField = (SFNode) endCapAppearanceNode.getField("material");
                endCapMaterialField.setValue(endCapMaterialNode);




                // First end cap
                X3DNode startEndCapTransform = mainScene.createNode("Transform");
                X3DNode startEndCapShape = mainScene.createNode("Shape");
                X3DNode startEndCapBox = mainScene.createNode("Box");

                SFNode startEndCapAppearanceNode = (SFNode) startEndCapShape.getField("appearance");
                startEndCapAppearanceNode.setValue(endCapAppearanceNode);

                SFVec3f startEndCapBoxSize = (SFVec3f) startEndCapBox.getField("size");
                float[] startEndCapUseBoxSize = new float[] {0.2f, 2.2f, 0.05f};
                startEndCapBoxSize.setValue(startEndCapUseBoxSize);

                SFNode startEndCapShapeGeometry = (SFNode) startEndCapShape.getField("geometry");
                startEndCapShapeGeometry.setValue(startEndCapBox);

                MFNode startEndCapTransformChildren = (MFNode) startEndCapTransform.getField("children");
                startEndCapTransformChildren.append(startEndCapShape);

                startEndCapTranslation = (SFVec3f) startEndCapTransform.getField("translation");
                startEndCapTranslation.setValue(new float[] {vertexList[0], 1.1f, vertexList[2]});


                // Second end cap
                X3DNode endEndCapTransform = mainScene.createNode("Transform");
                X3DNode endEndCapShape = mainScene.createNode("Shape");
                X3DNode endEndCapBox = mainScene.createNode("Box");

                SFNode endEndCapAppearanceNode = (SFNode) endEndCapShape.getField("appearance");
                endEndCapAppearanceNode.setValue(endCapAppearanceNode);

                SFVec3f endEndCapBoxSize = (SFVec3f) endEndCapBox.getField("size");
                float[] endEndCapUseBoxSize = new float[] {0.2f, 2.2f, 0.05f};
                endEndCapBoxSize.setValue(endEndCapUseBoxSize);

                SFNode endEndCapShapeGeometry = (SFNode) endEndCapShape.getField("geometry");
                endEndCapShapeGeometry.setValue(endEndCapBox);

                MFNode endEndCapTransformChildren = (MFNode) endEndCapTransform.getField("children");
                endEndCapTransformChildren.append(endEndCapShape);

                endEndCapTranslation = (SFVec3f) endEndCapTransform.getField("translation");
                endEndCapTranslation.setValue(new float[] {vertexList[vertexList.length-3], 1.1f, vertexList[vertexList.length-1]});

                //--------------------------------------------------------
                // Add the end caps as their own group in the content branch
                //--------------------------------------------------------
                Group group = (Group)mainScene.createNode("Group");
                MFNode groupChildren = (MFNode) group.getField("children");
                groupChildren.append(startEndCapTransform);
                groupChildren.append(endEndCapTransform);

                contentGroup.addChildren(new X3DNode[] {group});
*/
                x3dBrowser.endUpdate();
            }
        }
    }

    /**
     * Generate the picking geometry.
     */
    private void generatePickingGeometry(EntityAddedQueueData data){
        synchronized(this){

            x3dBrowser.beginUpdate();

            Entity entity = data.getEntity();
            int type = entity.getType();
            int entityID = entity.getEntityID();
            X3DNode node = data.getX3DNode();

            if (type == Entity.TYPE_MODEL ||
            		type == Entity.TYPE_MODEL_WITH_ZONES) {

                    MFNode rootTransformNode = (MFNode) node.getField("children");


                    //--------------------------------------------------------
                    // Create the shape node
                    //--------------------------------------------------------
                    X3DNode shape = mainScene.createNode("Shape");

                    //--------------------------------------------------------
                    // create the appearance & material nodes
                    //--------------------------------------------------------
                    SFNode appearanceField = (SFNode) shape.getField("appearance");

                    X3DNode appearanceNode = mainScene.createNode("Appearance");
                    X3DNode materialNode = mainScene.createNode("Material");

                    // Set the material color
                    SFColor color = (SFColor) materialNode.getField("diffuseColor");
                    float[] colorValue = {1.0f, 0.0f, 0.0f};
                    color.setValue(colorValue);

                    // Set the transparency
                    SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                    float transparencyValue = TRANSPARENCY_ON_VALUE;
                    transparency.setValue(transparencyValue);

                    // Add the material to the appearance node
                    SFNode materialField = (SFNode) appearanceNode.getField("material");
                    materialField.setValue(materialNode);

                    // Add the appearance node to the shape node
                    appearanceField.setValue(appearanceNode);

                    //--------------------------------------------------------
                    // create the box node
                    //--------------------------------------------------------
                    SFNode shape_geometry = (SFNode) shape.getField("geometry");
                    X3DNode box = mainScene.createNode("Box");

                    // set the box size
                    SFVec3f boxSize = (SFVec3f) box.getField("size");

                    float[] boxSizeValue = new float[3];
                    ((PositionableEntity)entity).getSize(boxSizeValue);

                    float[] useBoxSize = new float[3];

                    if(boxSizeValue == null){
                        useBoxSize = new float[] {1.0f, 1.0f, 1.0f};
                    } else {
                        useBoxSize = new float[] {boxSizeValue[0], boxSizeValue[1], boxSizeValue[2]};
                    }

                    boxSize.setValue(useBoxSize);

                    // add the box to the shape node
                    shape_geometry.setValue(box);

                    //--------------------------------------------------------
                    // Add the shape transform under the root Transform node
                    //--------------------------------------------------------

                    rootTransformNode.append(shape);

                    //----------------------------------------------------
                    // Add picker
                    //----------------------------------------------------
                    PrimitivePicker pp = (PrimitivePicker) mainScene.createNode("PrimitivePicker");

                    pp.setIntersectionType("GEOMETRY");
                    pp.setSortOrder("ALL");

                    pp.setPickingGeometry(box);
                    pp.setPickTarget(new X3DNode[] {});

                    pp.setEnabled(true);
                    rootTransformNode.append(pp);

                    //--------------------------------------------------------
                    // Store the entity ID and box in map for picking use
                    //--------------------------------------------------------
                    pickBoxGeometryMap.put(entityID, box);
                    pickShapeMaterialMap.put(entityID, materialNode);
                    pickTransformGeometryMap.put(entityID, node);
                    pickPrimitiveNodeMap.put(entityID, pp);






                    //--------------------------------------------------------
                    //--------------------------------------------------------
                    // Create the proxy pick target
                    //--------------------------------------------------------
                    //--------------------------------------------------------

                    X3DNode pickingTargetTransform = mainScene.createNode("Transform");

                    MFNode pickingTargetTransformChildren = (MFNode) pickingTargetTransform.getField("children");

                    if (entity instanceof PositionableEntity){
                        double[] position = new double[3];
                        ((PositionableEntity)entity).getPosition(position);
                        SFVec3f positionField = (SFVec3f)pickingTargetTransform.getField("translation");
                        float[] pos = new float[] {(float)position[0], (float)position[1], (float)position[2]};
                        positionField.setValue(pos);
                    }

                    //--------------------------------------------------------
                    // Create the shape node
                    //--------------------------------------------------------
                    X3DNode proxyShape = mainScene.createNode("Shape");

                    //--------------------------------------------------------
                    // create the appearance & material nodes
                    //--------------------------------------------------------
                    SFNode proxyAppearanceField = (SFNode) proxyShape.getField("appearance");

                    X3DNode proxyAppearanceNode = mainScene.createNode("Appearance");
                    X3DNode proxyMaterialNode = mainScene.createNode("Material");

                    // Set the material color
                    SFColor proxyColor = (SFColor) proxyMaterialNode.getField("diffuseColor");
                    float[] proxyColorValue = {0.0f, 1.0f, 0.0f};
                    proxyColor.setValue(proxyColorValue);

                    // Set the transparency
                    SFFloat proxyTransparency = (SFFloat) proxyMaterialNode.getField("transparency");
                    float proxyTransparencyValue = TRANSPARENCY_OFF_VALUE;
                    proxyTransparency.setValue(proxyTransparencyValue);

                    // Add the material to the appearance node
                    SFNode proxyMaterialField = (SFNode) proxyAppearanceNode.getField("material");
                    proxyMaterialField.setValue(proxyMaterialNode);

                    // Add the appearance node to the shape node
                    proxyAppearanceField.setValue(proxyAppearanceNode);

                    //--------------------------------------------------------
                    // create the box node
                    //--------------------------------------------------------
                    SFNode proxy_shape_geometry = (SFNode) proxyShape.getField("geometry");
                    X3DNode proxyBox = mainScene.createNode("Box");

                    // set the box size
                    SFVec3f proxyBoxSize = (SFVec3f) proxyBox.getField("size");

                    float[] proxyBoxSizeValue = new float[3];
                    ((PositionableEntity)entity).getSize(proxyBoxSizeValue);

                    float[] useProxyBoxSize = new float[3];

                    if(proxyBoxSizeValue == null){
                        useProxyBoxSize = new float[] {1.0f, 1.0f, 1.0f};
                    } else {
                        useProxyBoxSize = new float[] {proxyBoxSizeValue[0], proxyBoxSizeValue[1], proxyBoxSizeValue[2]};
                    }

                    proxyBoxSize.setValue(useProxyBoxSize);

                    // add the box to the shape node
                    proxy_shape_geometry.setValue(proxyBox);

                    //--------------------------------------------------------
                    // Add the shape transform under the root Transform node
                    //--------------------------------------------------------

                    pickingTargetTransformChildren.append(proxyShape);
                    contentGroup.addChildren(new X3DNode[] {pickingTargetTransform});
                    pickTargetProxyMap.put(entityID, pickingTargetTransform);










                    //-------------------------------------------------------
                    // Set the transparency override flag
                    //-------------------------------------------------------
/*                  entity.setProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.COLLISION_TRANSPARENCY_OVERRIDE,
                            true);
    */

                    //----------------------------------------------------
                    // Update the target lists for all picking sensors
                    //----------------------------------------------------
     /*
                    Collection<X3DNode> pickingNodeCollection = pickPrimitiveNodeMap.values();

                    PrimitivePicker nodePick;

                    for(Iterator<X3DNode> nodeIterator = pickingNodeCollection.iterator(); nodeIterator.hasNext();){

                        nodePick = (PrimitivePicker) nodeIterator.next();

                        Set<Map.Entry<Integer, X3DNode>> pickingShapeMap =
                            pickTransformGeometryMap.entrySet();

                        X3DNode[] targetList = new X3DNode[pickingShapeMap.size()];
                        int counter = 0;

                        for (Map.Entry<Integer, X3DNode> pickingNodeEntry : pickingShapeMap){

                            targetList[counter] = pickingNodeEntry.getValue();
                            counter++;
                        }

                        nodePick.setPickTarget(targetList);
                    }
      */      }

            entity = null;
            node = null;

            x3dBrowser.endUpdate();
        }
    }

    /**
     * Update all primitive pick node targets to reflect the current set of
     * targets in the scene.
     */
    private void updatePrimitivePickingTargets(){

        if(pickingType == PickingType.PRIMITIVE){

            if(pickPrimitiveNodeMap == null || pickTargetProxyMap == null){
                System.out.println("Can't update as one of the maps was null");
                return;
            }

            synchronized(this){
                // Picking nodes in scene
                Set<Map.Entry<Integer, X3DNode>> pickingNodeSet = pickPrimitiveNodeMap.entrySet();

                // Shape nodes in the scene
                Set<Map.Entry<Integer, X3DNode>> shapeNodeSet = pickTargetProxyMap.entrySet();

                PrimitivePicker pickNode = null;
                int entityID;

                x3dBrowser.beginUpdate();

                // Update multi-segment picking node
                if(multiSegmentPickNode != null){


                    X3DNode[] targetList = new X3DNode[] {};

                    if (pickTargetProxyMap != null && pickTargetProxyMap.size() > 0){

                        targetList = (X3DNode[]) (pickTargetProxyMap.values().toArray(new X3DNode[0]));

                    }

                    multiSegmentPickNode.setPickTarget(targetList);

                }

                // Update picking nodes (non multi-sub)
                for (Map.Entry<Integer, X3DNode> pickingNodeEntry : pickingNodeSet) {

                    pickNode = (PrimitivePicker) pickingNodeEntry.getValue();
                    entityID = pickingNodeEntry.getKey();

                    pickNode.setEnabled(false);

                    Collection<X3DNode> shapeCollection = pickTargetProxyMap.values();
                    ArrayList<X3DNode> targetListPP = new ArrayList<X3DNode>();
                    targetListPP.clear();
                    X3DNode node;

                    for(Iterator<X3DNode> nodeIterator = shapeCollection.iterator(); nodeIterator.hasNext();){

                        node = (X3DNode)nodeIterator.next();
                        targetListPP.add(node);
                    }

                    X3DNode shapeNodeEquivalent = pickTargetProxyMap.get(entityID);
                    targetListPP.remove(shapeNodeEquivalent);

                    if(targetListPP.size() > 0){

                        pickNode.setPickTarget((X3DNode[])targetListPP.toArray(
                                new X3DNode[targetListPP.size()]));
                    }

                    pickNode.setEnabled(true);

                }

                x3dBrowser.endUpdate();

            }
        }
    }

    /**
     * Check the world for pick nodes and determine the correct state of
     * collision feedback for the scene.
     */
    private void performWorldCollisionCheck(){

        //----------------------------------------------------------------
        // Perform processing of PRIMITIVE pick nodes
        //----------------------------------------------------------------
        if(pickingType == PickingType.PRIMITIVE){

            x3dBrowser.beginUpdate();

            // Picking nodes in scene
            Set<Map.Entry<Integer, X3DNode>> pickingNodeSet = pickPrimitiveNodeMap.entrySet();

            PrimitivePicker pickNode = null;
            int entityID;
            Entity entity = null;
            int numberCollisions;


// ROOM CHECKS
            ArrayList<Integer> entityIdList = null;

            if(multiSegmentPickedGeometry != null &&
                multiSegmentPickIsActive != null &&
                multiSegmentPickIsActive.getValue() == true &&
                multiSegmentPickedGeometry.getSize() > 0){

                X3DNode[] pickedGeometryList = new X3DNode[multiSegmentPickedGeometry.getSize()];
                multiSegmentPickedGeometry.getValue(pickedGeometryList);

                Set<Map.Entry<Integer, X3DNode>> pickShapeGeometrySet = pickTargetProxyMap.entrySet();
                Set<Integer> entityIdKeySet = pickTargetProxyMap.keySet();

                entityIdList = new ArrayList<Integer>();
                for(Integer eID:entityIdKeySet){
                    entityIdList.add(eID.intValue());
                }

                for(int i = 0; i < pickedGeometryList.length; i++){

                    for (Map.Entry<Integer, X3DNode> pickShapeGeometryEntry : pickShapeGeometrySet) {

                        if(pickShapeGeometryEntry.getValue().equals(pickedGeometryList[i])){

                            int eID = pickShapeGeometryEntry.getKey();
                            entityIdList.remove(((Integer)eID));

                            Entity entityInProcess = entityMap.get(eID);

                            String collisionRule =
                                (String) entityInProcess.getProperty(
                                    Entity.DEFAULT_ENTITY_PROPERTIES,
                                    Entity.COLLISION_CONDITION_PROPERTY);

                            Boolean transparencyOverrideSet =
                                (Boolean) entityInProcess.getProperty(
                                        Entity.DEFAULT_ENTITY_PROPERTIES,
                                        Entity.COLLISION_TRANSPARENCY_OVERRIDE);

                            if(collisionRule == null || collisionRule.length() <= 0){

                                X3DNode materialNode = pickShapeMaterialMap.get(eID);

                                SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                                float transparencyValue = TRANSPARENCY_OFF_VALUE;
                                transparency.setValue(transparencyValue);

                            } else if (transparencyOverrideSet != null && !transparencyOverrideSet.booleanValue()) {

                                X3DNode materialNode = pickShapeMaterialMap.get(eID);

                                SFFloat transparency = (SFFloat) materialNode.getField("transparency");
//                              float transparencyValue = TRANSPARENCY_OFF_VALUE;
//                              transparency.setValue(transparencyValue);

                            } else {

                                X3DNode materialNode = pickShapeMaterialMap.get(eID);

                                SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                                float transparencyValue = TRANSPARENCY_ON_VALUE;
                                transparency.setValue(transparencyValue);

                            }

                            break;
                        }
                    }
                }

                for (Integer eID : entityIdList){

                    X3DNode materialNode = pickShapeMaterialMap.get(eID);

                    SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                    float transparencyValue = TRANSPARENCY_ON_VALUE;
                    transparency.setValue(transparencyValue);
                }
            } else {
                Set<Map.Entry<Integer, X3DNode>> pickShapeGeometrySet = pickTargetProxyMap.entrySet();

                for (Map.Entry<Integer, X3DNode> pickShapeGeometryEntry : pickShapeGeometrySet) {

                    int eID = pickShapeGeometryEntry.getKey();

                    Entity entityInProcess = entityMap.get(eID);

                    Boolean transparencyOverrideSet =
                        (Boolean) entityInProcess.getProperty(
                                Entity.DEFAULT_ENTITY_PROPERTIES,
                                Entity.COLLISION_TRANSPARENCY_OVERRIDE);

                    if(transparencyOverrideSet == null || !transparencyOverrideSet){

                        X3DNode materialNode = pickShapeMaterialMap.get(eID);

                        SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                        float transparencyValue = TRANSPARENCY_ON_VALUE;
                        transparency.setValue(transparencyValue);
                    }
                }
            }




            // Check individual nodes for collision with other entities
            for (Map.Entry<Integer, X3DNode> pickingNodeEntry : pickingNodeSet) {

                pickNode = (PrimitivePicker) pickingNodeEntry.getValue();
                entityID = pickingNodeEntry.getKey();
                entity = entityMap.get(entityID);

                numberCollisions = pickNode.getNumPickedGeometry();

                // Process inter-object collision results
                if(numberCollisions > 0){

                    // Collect information about the collisions - pass along entity
                    X3DNode[] pickedNodes = new X3DNode[numberCollisions];
                    pickNode.getPickedGeometry(pickedNodes);

                    Entity[] entityList = new Entity[pickedNodes.length];
                    Entity foundEntity = null;

                    for(int i = 0; i < pickedNodes.length; i++){

                        for(int pickEntityID:pickTargetProxyMap.keySet()){

                            if(pickTargetProxyMap.get(pickEntityID).equals(pickedNodes[i])) {

                                foundEntity = entityMap.get(pickEntityID);

                                entityList[i] = foundEntity;
                                break;
                            }
                        }
                    }

                    // Hand off the category names of collided entities
                    entity.setProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.COLLISION_TARGETS_PROPERTY,
                            entityList, false);

                    // Turn current object red if transparency flag isn't set
                    Boolean flagState = (Boolean) entity.getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.COLLISION_TRANSPARENCY_OVERRIDE);

                    if(flagState == null || !flagState.booleanValue()){

                        X3DNode materialNode = pickShapeMaterialMap.get(entityID);

                        // Set the transparency
                        SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                        float transparencyValue = TRANSPARENCY_ON_VALUE;
                        transparency.setValue(transparencyValue);

                    } else {

                        X3DNode materialNode = pickShapeMaterialMap.get(entityID);

                        // Set the transparency
                        SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                        float transparencyValue = TRANSPARENCY_OFF_VALUE;
                        transparency.setValue(transparencyValue);
                    }

                    entity.setProperty(Entity.DEFAULT_ENTITY_PROPERTIES, Entity.COLLISION_PROP, true, false);

                } else {

                    // Hand off the category names as null
                    entity.setProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.COLLISION_TARGETS_PROPERTY,
                            null, false);

                    // Turn off transparency if transparency flag isn't set
                    Boolean flagState = (Boolean) entity.getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.COLLISION_TRANSPARENCY_OVERRIDE);

                    String collisionRule =
                        (String) entity.getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            Entity.COLLISION_CONDITION_PROPERTY);

                    if(collisionRule == null && flagState != null && !flagState.booleanValue()){

                        X3DNode materialNode = pickShapeMaterialMap.get(entityID);

                        // Set the transparency
                        SFFloat transparency = (SFFloat) materialNode.getField("transparency");
                        float transparencyValue = TRANSPARENCY_ON_VALUE;
                        transparency.setValue(transparencyValue);

                    } else if (collisionRule == null && flagState != null && !flagState.booleanValue()) {
                        entity.setProperty(
                                Entity.DEFAULT_ENTITY_PROPERTIES,
                                Entity.COLLISION_TRANSPARENCY_OVERRIDE,
                                false, false);
                    }

                    entity.setProperty(Entity.DEFAULT_ENTITY_PROPERTIES, Entity.COLLISION_PROP, false, false);
                }
            }

            x3dBrowser.endUpdate();
        }
    }

    /**
     * Queue a request to have an entity's elevation configured.
     *
     * @param data The identifier info of the entity.
     */
    private void queueConfigElevationRequest(EntityConfigData data) {
        synchronized(this) {
            if (!pickInProgress || !pickNodeIsActive) {
                configPick(data);
            } else {
                pickQueue.add(data);
            }
        }
    }

    /**
     * Configure the pick node to get the elevation data at the desired location
     *
     * @param data The configuration request data
     */
    private void configPick(EntityConfigData data) {
        float x = (float)data.position[0];
        float z = (float)data.position[2];
        line_point[0] = x;
        line_point[2] = z;
        line_point[3] = x;
        line_point[5] = z;
        pickInProgress = true;
        pickGeometryReady = false;
        coord.setPoint(line_point);
        pickedPoint.setUserData(data);
        lpPickNode.setEnabled(true);
    }

    /**
     * Initialize the pick node's target geometry with everything
     * pickable in the main scene
     */
    private void initializePickTargets() {

        targetListLP = new Vector<X3DNode>();

        //----------------------------------------------------------------
        // get references to the pickable geometry in the scene
        //----------------------------------------------------------------
        if(pickingType == PickingType.ELEVATION){
            X3DNode[] node = mainScene.getRootNodes();
            SceneUtils.getPickTargets(node, targetListLP);
            node = (X3DNode[])targetListLP.toArray(new X3DNode[targetListLP.size()]);
            pickTargetReady = false;
            lpPickNode.setPickTarget(node);
        }
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Add network viewpoints
     */
    private void addNetworkNodes() {
        if (networkedVPAdded)
            return;

        networkedVPAdded = true;

        masterViewpoint = (Viewpoint) mainScene.createNode("Viewpoint");
        masterViewpoint.setDescription("Networked_Master");
        slavedViewpoint = (Viewpoint) mainScene.createNode("Viewpoint");

        masterViewpoint.setDescription("Networked_Slave");
        freeViewpoint = (Viewpoint) mainScene.createNode("Viewpoint");
        masterViewpoint.setDescription("Networked_Free");

        // Add a ProximitySensor for tracking movement

        proxSensor = (ProximitySensor)mainScene.createNode("ProximitySensor");
//        proxSensor.setSize(new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE });
        // TODO: Need to support infinite size sensors.
        proxSensor.setSize(new float[] { 50000, 50000, 50000 });

        posChanged = (SFVec3f) proxSensor.getField("position_changed");
        posChanged.addX3DEventListener(this);

        oriChanged = (SFRotation) proxSensor.getField("orientation_changed");
        oriChanged.addX3DEventListener(this);

        mainScene.addRootNode(proxSensor);
        mainScene.addRootNode(masterViewpoint);
        mainScene.addRootNode(slavedViewpoint);
        mainScene.addRootNode(freeViewpoint);
    }
}
