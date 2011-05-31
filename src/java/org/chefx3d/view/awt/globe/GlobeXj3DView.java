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

package org.chefx3d.view.awt.globe;

// Standard Imports
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import java.io.StringWriter;
import java.io.File;
import java.net.URL;

import org.w3c.dom.Document;

import org.web3d.x3d.sai.*;
import org.xj3d.sai.Xj3DBrowser;

// Application specific imports
import org.chefx3d.PropertyPanelDescriptor;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.MultiplicityConstraint;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;
import org.chefx3d.view.ViewConfig;
import org.chefx3d.view.ViewX3D;
import org.chefx3d.view.WorldLoaderListener;
import org.chefx3d.view.ViewManager;
import org.chefx3d.view.awt.ControlAction;
import org.chefx3d.model.*;
import org.chefx3d.util.DOMUtils;

import org.web3d.sai.util.SceneUtils;

import org.web3d.util.IntHashMap;

import org.web3d.x3d.sai.environmentalsensor.ProximitySensor;

import org.web3d.x3d.sai.grouping.Group;
import org.web3d.x3d.sai.grouping.Transform;

import org.web3d.x3d.sai.navigation.Viewpoint;

import org.web3d.x3d.sai.networking.Inline;
import org.web3d.x3d.sai.networking.LoadSensor;

import org.web3d.x3d.sai.pickingsensor.LinePicker;

import org.web3d.x3d.sai.rendering.Coordinate;
import org.web3d.x3d.sai.rendering.IndexedLineSet;
import org.web3d.x3d.sai.geospatial.GeoLocation;
import org.web3d.x3d.sai.geospatial.GeoOrigin;

import org.web3d.x3d.sai.shape.Shape;

import org.web3d.x3d.sai.time.TimeSensor;
import org.xj3d.sai.Xj3DCursorFilter;
import org.xj3d.sai.Xj3DCursorUIManager;

/**
 * A View which is backed by a full 3D scene on top of a Globe.
 *
 * 3D Views use the x3d_view stylesheet.  This stylesheet must be self-contained.
 *
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
public class GlobeXj3DView extends JPanel implements ViewX3D, ViewConfig, Runnable, BrowserListener,
    WorldLoaderListener, X3DFieldEventListener, ModelListener, ActionListener,
    Xj3DCursorFilter {

    private static enum MouseMode {NONE, NAVIGATION, PLACEMENT, SELECTION, ASSOCIATE};

    private static double[] GEO_ORIGIN = new double[] { 0.0, 0.0, -6378137 };
    private static String[] GEO_SYSTEM = new String[] {"GDC"};

    /** The initial world to load */
    private String initialWorld;

    /** Time in ms of no navigation activity till we issue a non-transient ChangeViewCommand */
    private static final int CHANGE_VIEW_TIME = 250;

    /** The world model */
    private WorldModel model;

    /** The X3D browser */
    private Xj3DBrowser x3dBrowser;

    /** The current url */
    private String currentURL;

    /** The current scene */
    private X3DScene mainScene;

    /** The GeoTouchSensor used for location */
    private X3DNode geosensor;

    /** The GeoTouchSensor sensor's isActive field */
    private SFBool geoIsActive;

    /** The GeoTouchSensor sensor's enabled field */
    private SFBool geoEnabled;

    /** Are we dropping something */
    private boolean dropping;

    /** The GeoTouchSensor's hitGeoCoord_changed field */
    private SFVec3d hitGeoCoord;

    /** Map between entityID and loaded models Transform node */
    private IntHashMap modelMap;

    /** Map between entityID and loaded models touchSensor node */
    private HashMap<X3DField, Integer> tsMap;

    /** Map between entityID and the enabled field */
    private HashMap<Integer, X3DField> tsEnabledMap;

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

    /////////////////////////////////////////////////////////////////////////
    // picking variables

    /** Debugging flag that makes the pick line visible */
    private static final boolean SHOW_PICK_LINE = false;

    /** Flag indicating whether this view should manage entity
    *  elevation setting */
    private boolean configElevation = false;

    /** The pick sensor */
    private LinePicker pickNode;

    /** The pick sensor's isActive field */
    private SFBool isActive;

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
    private Vector<X3DNode> targetList;

    /** TimeSensor used for detecting event model cycles. Used on new
    *  world loads to manage initialization of scene dependent objects. */
    private TimeSensor timeSensor;

    /** TimeSensor field that listens for detecting event model cycles. */
    private SFTime time;

    /** A frame counting watchdog, to terminate picks that are initiated
     *  but the pick geometry does not intersect a target. */
    private int pickWatchdog;

    /////////////////////////////////////////////////////////////////////////

    // Editing variables

    /** Is a drag of an entity ongoing */
    private boolean entityDragging;

    /** Is a rotation of an entity ongoing */
    private boolean entityRotating;

    /** UI controls to switch modes between pick & place and navigation modes */
    private JToolBar toolBar;

    /** The last geo spatial position of the mouse */
    private double[] lastGeoPos;

    /** A scratch rotation */
    private float[] tmpRot;

    /** The current tool image */
    private Image toolImage;

    /** The current tool */
    private Tool currentTool;

    /** Are we in a multi-segment operation */
    private boolean multiSegmentOp;

    /** Are we in associate mode */
    private boolean associateMode;

    /** current state for mouse clicks */
    private MouseMode currentMode;

    /** previous state for mouse clicks */
    private MouseMode previousMode;

    /** A map of image url to cached Image */
    private HashMap<String, Image> imageMap;

    /** Group for the mode switch buttons */
    private ButtonGroup modeGroup;

    /** Control to enable the pick & place mode */
    private JToggleButton pickAndPlaceButton;

    /** Flag indicating that pick & place mode is active */
    //private boolean pickAndPlaceIsActive;

    /** Control to enable the navigation mode */
    private JToggleButton navigateButton;

    /** Flag indicating that navigation mode is active */
    //private boolean navigateIsActive;

    /** Group for the navigate switch buttons */
    private ButtonGroup navigateGroup;

    /** Control to enable the bounding function */
    private JToggleButton boundButton;

    /** Control to enable pan navigation mode */
    private JToggleButton panButton;

    /** The ToolBarManager */
    private ToolBarManager toolBarManager;

    /** The cursor manager */
    private Xj3DCursorUIManager cursorManager;

    /** Are we in a transient command */
    private boolean inTransient;

    /** The current entity.  Not sure this concept will work */
    private Entity currentEntity;

    /** The keySensor keyRelease field */
    private SFString keyRelease;

    /////////////////////////////////////////////////////////////////////////

    public GlobeXj3DView(WorldModel model, String initialWorld, String globeURL) {
        super(new BorderLayout());

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        if (initialWorld == null) {
            errorReporter.errorReport("No initial world specified for GlobeXj3DView", null);
        }

        this.initialWorld = initialWorld;

        model.addModelListener(this);
        this.model = model;

        // Setup browser parameters
        // TODO: GlobeXj3DView - HashMap has mixed types, is not type safe in 1.5 generics
        HashMap requestedParameters = new HashMap();
        requestedParameters.put("Xj3D_FPSShown",Boolean.TRUE);
        requestedParameters.put("Xj3D_LocationShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_LocationPosition", "top");
        requestedParameters.put("Xj3D_LocationReadOnly", Boolean.FALSE);
        requestedParameters.put("Xj3D_OpenButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ReloadButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ShowConsole",Boolean.FALSE);
        requestedParameters.put("Xj3D_StatusBarShown",Boolean.TRUE);

        // TODO: Necessary till culling bug is fixed
        requestedParameters.put("Xj3D_Culling_Mode","none");

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
        lastGeoPos = new double[3];
        tmpRot = new float[4];

        imageMap = new HashMap<String, Image>();

        terminate = false;
        Thread thread = new Thread(this, "GlobeXj3DView Tasks");
        thread.start();

        ViewManager.getViewManager(model).addView(this);
        toolBarManager = ToolBarManager.getToolBarManager();

        pickQueue = new ArrayList<EntityConfigData>();
        completeQueue = new ArrayList<EntityConfigData>();


        // TODO: I suspect we need to move this to the real catalog.  How to make hidden?
        // Create a default location
        String cpath = "catalog/";

        String[] interfaceIcons = new String[] {"Earth16x16.png",
                "Earth32x32.png","Earth64x64.png"};

        Document properties = DOMUtils.parseXML("" +
                "<ChefX3D>" +
                "   <EntityParams>" +
                "       <Sheet name='SMAL'>" +
                "           <EntityDefinition>" +
                "               <Earth timeOfDay='0' />" +
                "           </EntityDefinition>" +
                "       </Sheet>" +
                "   </EntityParams>" +
                "</ChefX3D>");

        PropertyPanelDescriptor[] propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);
        String[] toolParams = null;

        HashMap<String, String> styles = new HashMap<String, String>();
        styles.put("x3d", cpath + "common/x3d_default_world.xslt");

        Tool tool = new Tool("Earth", "Earth.png", interfaceIcons, false, Tool.TYPE_WORLD,
                new String[] { globeURL, cpath + "Locations/Grid/Grid.x3dv" }, 0, "Earth",
                propertyPanels, null, null, styles, "SMAL", "16", "0.1", "16", toolParams,
                MultiplicityConstraint.SINGLETON, "World", false, false, false);

        int entityID = model.issueEntityID();

        EntityBuilder builder = EntityBuilder.getEntityBuilder();

        Entity newEntity =
            builder.createEntity(model, entityID, new double[3],
                    new float[] {0,1,0,0}, tool);

        AddEntityCommand cmd = new AddEntityCommand(model, newEntity);
        model.applyCommand(cmd);

        List<Entity> selected = new ArrayList<Entity>();
        selected.add(newEntity);

        changeSelection(selected);

    }

    public GlobeXj3DView(HashMap params) {
        super(new BorderLayout());

        init(params);
    }

    /**
     * The current tool changed.
     *
     * @param tool The new tool
     */
    public void setTool(Tool tool) {
System.out.println("setTool: " + tool);
        int type = tool.getToolType();

        if (type == Tool.TYPE_MULTI_SEGMENT) {
            setMode(MouseMode.PLACEMENT, true);
            multiSegmentOp = true;
        } else {
            setMode(MouseMode.PLACEMENT, true);
            multiSegmentOp = false;
        }

        toolImage = getImage(tool.getIcon());
        currentTool = tool;

System.out.println("***SetTool: " + currentTool + " icon: " + tool.getIcon() + " mode: " + currentMode);
    }

    /**
     * Go into associate mode.  The next selection in any view
     * will issue a selection event and do nothing else.
     */
    public void enableAssociateMode(String[] validTools) {
        //associateMode = true;
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
        //associateMode = false;
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

        currentMode = MouseMode.NONE;
        previousMode = MouseMode.NONE;
        associateMode = false;

        // Create an SAI component
        x3dComp = BrowserFactory.createX3DComponent(params);

        // Add the component to the UI
        JComponent x3dPanel = (JComponent)x3dComp.getImplementation();

        add(x3dPanel, BorderLayout.CENTER);

/*
        JPanel buttonPanel = new JPanel(new GridLayout(1,3));
        JButton masterButton = new JButton(
            new ControlAction("Master",MODE_MASTER, this));

        JButton slavedButton = new JButton(
            new ControlAction("Slaved",MODE_SLAVED, this));

        JButton freeButton = new JButton(
            new ControlAction("Free",MODE_FREE_NAV, this));

        buttonPanel.add(masterButton);
        buttonPanel.add(slavedButton);
        buttonPanel.add(freeButton);

        add(buttonPanel, BorderLayout.SOUTH);
*/

        toolBar = new JToolBar();

        Toolkit tk = Toolkit.getDefaultToolkit();

        FileLoader loader = new FileLoader();
        
        Object[] file = loader.getFileURL("images/2d/selectIcon.png");       
        Image image = tk.createImage((URL)file[0]);
        pickAndPlaceButton = new JToggleButton(new ImageIcon(image));
        pickAndPlaceButton.setToolTipText("Pick & Place Mode");
        pickAndPlaceButton.setEnabled(false);
        pickAndPlaceButton.addActionListener(this);

        file = loader.getFileURL("images/2d/panIcon.png");       
        image = tk.createImage((URL)file[0]);
        navigateButton = new JToggleButton(new ImageIcon(image));
        navigateButton.setToolTipText("Navigate Mode");
        navigateButton.setEnabled(false);
        navigateButton.addActionListener(this);

        modeGroup = new ButtonGroup();
        modeGroup.add(navigateButton);
        modeGroup.add(pickAndPlaceButton);

        file = loader.getFileURL("images/2d/openHandIcon.png");       
        image = tk.createImage((URL)file[0]);
        panButton = new JToggleButton(new ImageIcon(image));
        panButton.setToolTipText("Pan");
        panButton.setEnabled(false);
        panButton.addActionListener(this);

        file = loader.getFileURL("images/2d/boundIcon.png");       
        image = tk.createImage((URL)file[0]);
        boundButton = new JToggleButton(new ImageIcon(image));
        boundButton.setToolTipText("Select Area");
        boundButton.setEnabled(false);
        boundButton.addActionListener(this);

        navigateGroup = new ButtonGroup();
        navigateGroup.add(panButton);
        navigateGroup.add(boundButton);

        toolBar.add(pickAndPlaceButton);
        toolBar.add(navigateButton);
        toolBar.addSeparator();
        toolBar.add(panButton);
        toolBar.add(boundButton);

        this.add(toolBar, BorderLayout.NORTH);

        pickAndPlaceButton.setEnabled(true);
        navigateButton.setEnabled(true);
        navigateButton.setSelected(true);
        currentMode = MouseMode.NAVIGATION;

        // Get an external browser
        x3dBrowser = (Xj3DBrowser) x3dComp.getBrowser();
        x3dBrowser.addBrowserListener(this);
        cursorManager = x3dBrowser.getCursorManager();
        cursorManager.setCursorFilter(this);

        x3dBrowser.setMinimumFrameInterval(40);

        modelMap = new IntHashMap(100);
        tsMap = new HashMap<X3DField, Integer>(100);
        tsEnabledMap = new HashMap<Integer, X3DField>(100);

        exporter = new X3DExporter("3.1", "Immersive", new String[] {"Geospatial"}, new int[] {1});

        ViewManager.getViewManager(model).addView(this);

        dropping = false;
    }

    /**
     * Return the X3D component in use.
     *
     * @return The component
     */
    public X3DComponent getX3DComponent() {
        return x3dComp;
    }

    //----------------------------------------------------------
    // Methods required by Xj3DCursorFilter
    //----------------------------------------------------------

    /**
     * The internals of Xj3D have changed the cursor.  This method is
     * a control point for the application to decide on a different
     * cursor.  The cursor loaded will be changed to the returned
     * value.  These values will be cached internally to avoid
     * reloading images.
     *
     * @param cursor The new cursor to load
     * @return The cursor to use instead
     */
    public String cursorChanged(String cursor) {
        System.out.println("Cursor changed: " + cursor);
        String ret_val;

        if (currentMode == MouseMode.SELECTION) {
            ret_val = "PREDEFINED." + Cursor.DEFAULT_CURSOR;
        } else if (currentTool != null && currentMode == MouseMode.PLACEMENT) {
            ret_val = currentTool.getIcon();
            ret_val = "PREDEFINED." + Cursor.DEFAULT_CURSOR;
        } else {
            ret_val = cursor;
        }
        return ret_val;
    }

    //----------------------------------------------------------
    // Methods required by Runnable
    //----------------------------------------------------------

    public void run() {
        model.reissueEvents(this, this);

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

                    //errorReporter.messageReport("***Changing view to: " + java.util.Arrays.toString(tmpPosD));

                    ChangeViewCommand cmd = new ChangeViewCommand(model, transactionID, tmpPosD,
                        newOrientation, (float) Math.PI / 4);
                    model.applyCommand(cmd);

                    transactionID = 0;
                }
            }
        }
    }

    //---------------------------------------------------------
    // Method defined by ActionListener
    //---------------------------------------------------------

    /**
     * UI event handlers
     */
    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource();
        if (source == pickAndPlaceButton) {
            if (previousMode == MouseMode.PLACEMENT) {

                if (currentTool != null) {
                    toolImage = getImage(currentTool.getIcon());
                }

                setMode(MouseMode.PLACEMENT, false);
            } else if (previousMode == MouseMode.ASSOCIATE) {
                setMode(MouseMode.ASSOCIATE, false);
            } else {
                setMode(MouseMode.SELECTION, true);
            }
        } else if (source == navigateButton) {
            previousMode = currentMode;
            setMode(MouseMode.NAVIGATION, false);

        } else if (source == boundButton) {
            //boundIsActive = true;
            //panIsActive = false;
            //setDisplayCursor(crossHairCursor);

        }  else if (source == panButton) {
            //panIsActive = true;
            //boundIsActive = false;
            //setDisplayCursor(openHandCursor);

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
     * Set the location.
     *
     * @param url The url of the location.
     */
    private void setLocation(String url) {
System.out.println("Loading world: " + url);
        //errorReporter.messageReport("Loading Location: " + url);
        worldLoaded = false;

        if (!url.startsWith("file:")) {
            File file = new File(".");

            try {
                String path = file.toURL().toExternalForm();
                path = path.substring(0,path.length() - 2);   // remove ./

                url = path + url;
            } catch(Exception e) {
                errorReporter.errorReport("File Error!", e);
            }
        }

        /////////////////////////////////////////////////////////////////////////////
        // load the initial

System.out.println("Loading initialScene: " + initialWorld);
        X3DScene initialScene = x3dBrowser.createX3DFromURL(new String[] { initialWorld });

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
        // load the new location

System.out.println("Loading location: " + url);
        X3DScene locationScene = x3dBrowser.createX3DFromURL(new String[]{url});

        X3DNode root = initialScene.getNamedNode("ROOT");
        geosensor = initialScene.getNamedNode("TOUCH_SENSOR");
        hitGeoCoord = (SFVec3d) geosensor.getField("hitGeoCoord_changed");
        hitGeoCoord.addX3DEventListener(this);
        geoIsActive = (SFBool) geosensor.getField("isActive");
        geoIsActive.addX3DEventListener(this);
        geoEnabled = (SFBool) geosensor.getField("set_enabled");
        //geoEnabled.setValue(false);

        X3DNode keySensor = initialScene.getNamedNode("KEYSENSOR");
        keyRelease = (SFString) keySensor.getField("keyRelease");
        keyRelease.addX3DEventListener(this);

        copyScene(locationScene, mainScene, root);
        /////////////////////////////////////////////////////////////////////////////
        // build the picker and the pick geometry

        pickNode = (LinePicker)mainScene.createNode("LinePicker");
        pickNode.setIntersectionType("GEOMETRY");
        pickNode.setSortOrder("ALL");

        isActive = (SFBool)pickNode.getField("isActive");
        isActive.addX3DEventListener(this);

        pickedPoint = (MFVec3f)pickNode.getField("pickedPoint");
        pickedPoint.addX3DEventListener(this);

        pickTarget = (MFNode)pickNode.getField("pickTarget");
        pickTarget.addX3DEventListener(this);

        IndexedLineSet ils = (IndexedLineSet)mainScene.createNode("IndexedLineSet");
        coord = (Coordinate)mainScene.createNode("Coordinate");
        coordPoint = (MFVec3f)coord.getField("point");
        coordPoint.addX3DEventListener(this);

        coord.setPoint(line_point);
        ils.setCoord(coord);
        ils.setCoordIndex(line_point_indicies);

        pickNode.setPickingGeometry(ils);
        pickNode.setEnabled(false);

        mainScene.addRootNode(pickNode);

        if ( SHOW_PICK_LINE ) {
            Shape shape = (Shape)mainScene.createNode("Shape");
            shape.setGeometry(ils);

            Group group = (Group)mainScene.createNode("Group");
            group.setChildren( new X3DNode[]{shape} );

            mainScene.addRootNode(group);
        }

        // clear the pick target collection when loading a new world
        targetList = null;

        /////////////////////////////////////////////////////////////////////////////
        // setup a time sensor for tracking cycles through the event model
        // on initialization

        timeSensor = (TimeSensor)mainScene.createNode("TimeSensor");
        timeSensor.setLoop(true);
        timeSensor.setEnabled(false);

        time = (SFTime)timeSensor.getField("time");
        time.addX3DEventListener(this);

        mainScene.addRootNode(timeSensor);

        /////////////////////////////////////////////////////////////////////////////
        // Replace the current world with the new one

        // TODO: Need to figure out how to not add for non networked
        addNetworkNodes();

        x3dBrowser.replaceWorld(mainScene);
    }

    /**
     * Set the location.
     *
     * @param url The url of the location.
     */
    public void oldsetLocation(String url) {
        //errorReporter.messageReport("Loading Location: " + url);

        if (!url.startsWith("file:")) {
            File file = new File(".");

            try {
                String path = file.toURL().toExternalForm();
                path = path.substring(0,path.length() - 2);   // remove ./

                url = path + url;
            } catch(Exception e) {
                errorReporter.errorReport("File Error!", e);
            }
        }

        // Create an X3D scene by loading a file.  Blocks till the world is loaded.
        try {
            mainScene = x3dBrowser.createX3DFromURL(new String[] { url });
        } catch(Exception e) {
            errorReporter.errorReport("Error loading file: " + url, e);
            return;
        }
        // TODO: What if the main scene is not Immersive?

        // Add master,slaved,free viewpoints
        masterViewpoint = (Viewpoint) mainScene.createNode("Viewpoint");
        slavedViewpoint = (Viewpoint) mainScene.createNode("Viewpoint");
        freeViewpoint = (Viewpoint) mainScene.createNode("Viewpoint");

        // Add a ProximitySensor for tracking movement

        proxSensor = (ProximitySensor)mainScene.createNode("ProximitySensor");
//        proxSensor.setSize(new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE });
        // TODO: Need to support infinite size sensors.
        proxSensor.setSize(new float[] { 50000, 50000, 50000 });

        posChanged = (SFVec3f) proxSensor.getField("position_changed");
        posChanged.addX3DEventListener(this);

        oriChanged = (SFRotation) proxSensor.getField("orientation_changed");
        oriChanged.addX3DEventListener(this);
/*
        mainScene.addRootNode(proxSensor);
        mainScene.addRootNode(masterViewpoint);
        mainScene.addRootNode(slavedViewpoint);

        mainScene.addRootNode(freeViewpoint);
*/
        // Replace the current world with the new one
        x3dBrowser.replaceWorld(mainScene);
    }

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

            //errorReporter.messageReport("*** newPos: " + java.util.Arrays.toString(newPosition));

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
        double[] position = new double[3];
        float[] rotation = new float[4];

        if (entity instanceof PositionableEntity) {
            ((PositionableEntity)entity).getPosition(position);
            ((PositionableEntity)entity).getRotation(rotation);
        }

        Map<String,Document> props = entity.getProperties();

        int type = entity.getType();

        //errorReporter.messageReport("Loading Entity: " + entity.getEntityID());

System.out.println("Placing entity at: " + lastGeoPos[0] + " " + lastGeoPos[1] + " " + lastGeoPos[2]);
        if (type == Tool.TYPE_WORLD) {
            String url = entity.getURL();

            modelMap.clear();

            if (url.equals(currentURL))
                return;

            setLocation(url);

            currentURL = url;


            return;
        }


        X3DNode t = (X3DNode) modelMap.get(entityID);

        if (t != null) {
            // ignore dups as we expect them in a networked environment
            return;
        }

        if (mainScene != null) {
            GeoLocation location = (GeoLocation)mainScene.createNode("GeoLocation");
            location.setGeoSystem(GEO_SYSTEM);

            // TODO: need a better way to deal with GeoOrigin

            GeoOrigin geoOrigin = (GeoOrigin) mainScene.createNode("GeoOrigin");
            geoOrigin.setGeoSystem(GEO_SYSTEM);
            geoOrigin.setGeoCoords(GEO_ORIGIN);
            location.setGeoOrigin(geoOrigin);

            MFNode children = (MFNode) location.getField("children");

            if (entity.getType() != Tool.TYPE_MULTI_SEGMENT) {
                // Do not translate segment tools, they are in world coords

                location.setGeoCoords(new double[] {position[0],position[1],position[2]});

                // TODO: Need to handle rotations
                //location.setRotation(rotation);
            }

            generateX3D(entityID, children, mainScene);


            X3DNode ts = mainScene.createNode("TouchSensor");

            children.append(ts);
            SFBool tisActive = (SFBool) ts.getField("isActive");
            tisActive.addX3DEventListener(this);
            tsMap.put(tisActive, new Integer(entityID));
            tsEnabledMap.put(new Integer(entityID), ts.getField("enabled"));

            X3DNode n = mainScene.getNamedNode("ROOT");
            MFNode rootChildren = (MFNode) n.getField("children");
            rootChildren.append(location);

            //mainScene.addRootNode(transform);
            modelMap.put(entityID, location);

            if (!entity.isSegmentedEntity()) {
                if (configElevation) {
                    queueConfigElevationRequest(new EntityConfigData(entityID, position));
                } else {
                    targetList.add(location);
                    pickTargetReady = false;
                    pickNode.setPickTarget((X3DNode[])targetList.toArray(new X3DNode[targetList.size()]));
                }
            }
        }
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

        mainScene.removeRootNode(transform);
        modelMap.remove(entityID);

        // remove the entity from the pickable target collection,
        // note that segmented entities should never get on the list
        if (!entity.isSegmentedEntity()) {
            if (targetList.remove(transform)) {
                pickTargetReady = false;
                pickNode.setPickTarget((X3DNode[])targetList.toArray(new X3DNode[targetList.size()]));
            }
        }
    }

    /**
     * The entity moved.
     *
     * @param entityID the id
     * @param position The position in world coordinates(meters, Y-UP, X3D System).
     */
    public void entityMoved(boolean local, int entityID, double[] position) {
        X3DNode transform = (X3DNode) modelMap.get(entityID);

        ((GeoLocation)transform).setGeoCoords(position);

        if (configElevation) {
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
                    if (targetList.remove(transform)) {
                        pickTargetReady = false;
                        pickNode.setPickTarget((X3DNode[])targetList.toArray(new X3DNode[targetList.size()]));
                    }
                    queueConfigElevationRequest(new EntityConfigData(entityID, position));
                }
            }
        }
    }

    /**
     * A segment was added to the sequence.
     *
     * @param local Was this action initiated from the local UI
     * @param entityID The unique entityID assigned by the view
     * @param segmentID The unique segmentID
     * @param startVertexID The starting vertexID
     * @param endVertexID The starting vertexID
     */
    public void segmentAdded(boolean local, int entityID,
            int segmentID, int startVertexID, int endVertexID) {

        if (mainScene == null)
            return;

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);
    }

    /**
     * A segment was split.
     *
     * @param local Was this action initiated from the local UI
     * @param entityID The unique entityID assigned by the view
     * @param segmentID The unique segmentID
     * @param vertexID The starting vertexID
     */
    public void segmentSplit(boolean local, int entityID,
            int segmentID, int vertexID) {

        if (mainScene == null)
            return;

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);
    }

    /**
     * A vertex was removed.
     *
     * @param local Was this action initiated from the local UI
     * @param entityID The unique entityID assigned by the view
     * @param segmentID The segment removed
     */
    public void segmentRemoved(boolean local, int entityID,
            int segmentID) {

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);
    }

    /**
     * A vertex was added from the segment sequence.
     *
     * @param local Was this a local change
     * @param entityID The unique entityID assigned by the view
     * @param vertexID The unique vertexID assigned by the segment sequence
     * @param pos The x position in world coordinates
     */
    public void segmentVertexAdded(boolean local, int entityID, int vertexID,
            double[] position) {

        if (mainScene == null)
            return;

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);

        if (configElevation) {
            queueConfigElevationRequest(new EntityConfigData(entityID, vertexID, position));
        }
    }

    /**
     * A vertex was updated.
     *
     * @param local Was this a local change
     * @param entityID The unique entityID assigned by the view
     * @param vertexID The unique vertexID assigned by the segment sequence
     * @param propertyName The property name
     * @param propertySheet The property sheet
     * @param propertyValue The property value
     */
    public void segmentVertexUpdated(boolean local, int entityID, int vertexID,
            String propertyName, String propertySheet, String propertyValue) {
        if (mainScene == null)
            return;

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);
    }

    /**
     * A vertex was moved.
     *
     * @param local Was this a local change
     * @param entityID The unique entityID assigned by the view
     * @param vertexID The unique vertexID assigned by the segment sequence
     * @param pos The new position in world coordinates
     */
    public void segmentVertexMoved(boolean local, int entityID, int vertexID,
            double[] position) {
        if (mainScene == null)
            return;

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);

        if (configElevation) {
            boolean moveProcessed = false;
            int num = completeQueue.size();
            // check to see if this move command is a result of an elevation
            // change caused by a pick. if so - ignore it.
            for (int i=0; i<num; i++) {
                EntityConfigData data = completeQueue.get(i);
                if ((data.entityID == entityID)&&
                    (data.vertexID == vertexID)&&
                    (Arrays.equals(data.position, position))) {
                    moveProcessed = true;
                    completeQueue.remove(i);
                    break;
                }
            }
            if (!moveProcessed) {
                synchronized( this ) {
                    // if any picks are queued for this vertex, delete them,
                    // as they are obsolete.
                    num = pickQueue.size( );
                    for(int i = num-1; i>=0; i--) {
                        EntityConfigData data = pickQueue.get(i);
                        if ((data.entityID == entityID)&&
                            (data.vertexID == vertexID)) {

                            pickQueue.remove(i);
                        }
                    }
                    queueConfigElevationRequest(new EntityConfigData(entityID, vertexID, position));
                }
            }
        }
    }

    /**
     * A vertex was removed from the segment sequence.
     *
     * @param local Was this a local change
     * @param entityID The unique entityID assigned by the view
     * @param vertexID The unique vertexID assigned by the segment sequence
     * @param pos The x position in world coordinates
     */
    public void segmentVertexRemoved(boolean local, int entityID, int vertexID) {
        if (mainScene == null)
            return;

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);
    }

    /**
     * An entity has changed size.
     *
     * @param entityID The unique entityID assigned by the view
     * @param size The new size in meters
     */
    public void entitySizeChanged(boolean local, int entityID, float[] size) {
        if (mainScene == null) {
            return;
        }

        X3DNode transform = (X3DNode) modelMap.get(entityID);
        MFNode children = (MFNode) transform.getField("children");

        generateX3D(entityID, children, mainScene);
    }

    /**
     * A property changed.
     *
     * @param local Was this a local change
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The property that has changed
     * @param propertyValue The value being set.
     */
    public void propertyChanged(boolean local, int entityID, String propertySheet,
            String propertyName, Object propertyValue) {

        //System.out.println("propertyChanged for entityID: " + entityID);

        Entity entity = model.getEntity(entityID);

        //TODO: what to do if a propertyChange event impacts the 3D view,
        // right now we just handle segmentEntity.  We issuea separate
        // sizeChnaged event for the Box sizing but later on users can
        // specify 3D updates.
        if (entity.isSegmentedEntity()) {
            if (mainScene == null) {
                return;
            }

            X3DNode transform = (X3DNode) modelMap.get(entityID);
            MFNode children = (MFNode) transform.getField("children");

            generateX3D(entityID, children, mainScene);
        }

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
     * The entity was scaled.
     *
     * @param entityID the id
     * @param scale The scaling factors(x,y,z)
     */
    public void entityScaled(boolean local, int entityID, float[] scale) {
    }

    /**
     * The entity was rotated.
     * @param rotation The rotation(axis + angle in radians)
     */
    public void entityRotated(boolean local, int entityID, float[] rotation) {
        X3DNode transform = (X3DNode) modelMap.get(entityID);
        ((Transform)transform).setRotation(rotation);
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
                    int num = pickNode.getNumPickedPoint();
                    float[] pickPoint = new float[num*3];
                    pickNode.getPickedPoint(pickPoint);

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
                        if (data.isSegmented()) {
                            // since the segmented entity is created 'enmass', a vertex's
                            // elevation cannot be changed directly by it's Transform
                            cmd = new MoveVertexCommand(
                                model,
                                model.issueTransactionID(),
                                data.entityID,
                                data.vertexID,
                                pickPoint_d,
                                data.position);
                        } else {
                            // non-segmented entities have a unique associated Transform,
                            // and can be configured directly
                            ((Transform)transform).setTranslation(pickPoint);
                            cmd = new MoveEntityCommand(
                                model,
                                model.issueTransactionID(),
                                data.entityID,
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
                    if (!data.isSegmented() && !targetList.contains(transform)) {
                        targetList.add(transform);
                        pickTargetReady = false;
                        pickNode.setPickTarget((X3DNode[])targetList.toArray(new X3DNode[targetList.size()]));
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
                        pickNode.setEnabled(false);
                    }
                }
            }
            // process any move commands that have been generated
            if ( cmd != null ) {
                completeQueue.add(data);
                cmd.setErrorReporter(errorReporter);
                model.applyCommand(cmd);
            }
        } else if (src.equals(isActive)) {
            pickNodeIsActive = pickNode.getIsActive( );
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
                x3dBrowser.nextViewpoint();
                worldLoaded = true;

            } else if (pickInProgress && pickGeometryReady && !pickNodeIsActive) {
                // a pick was initiated, the pick geometry is ready,
                // but not intersecting a target
                if (pickWatchdog-- <= 0) {
                    EntityConfigData data = (EntityConfigData)pickedPoint.getUserData();
                    pickedPoint.setUserData(null);
                    if (!data.isSegmented()) {
                        X3DNode transform = (X3DNode)modelMap.get(data.entityID);
                        if(!targetList.contains(transform)) {
                            targetList.add(transform);
                            pickTargetReady = false;
                            pickNode.setPickTarget((X3DNode[])targetList.toArray(new X3DNode[targetList.size()]));
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
                            pickNode.setEnabled(false);
                        }
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
        } else if (src.equals(hitGeoCoord)) {
            hitGeoCoord.getValue(lastGeoPos);

            if (currentEntity != null && entityDragging == true) {
                if (!inTransient) {
                    transactionID = model.issueTransactionID();
                    inTransient = true;
                }

                // TODO: Need to calc velo
                MoveEntityTransientCommand cmd = new MoveEntityTransientCommand(
                    model,
                    transactionID,
                    currentEntity.getEntityID(),
                    lastGeoPos,
                    new float[3]);
                cmd.setErrorReporter(errorReporter);
                model.applyCommand(cmd);
            }

//System.out.println("pos: " + lastGeoPos[0] + " " + lastGeoPos[1]);
            // Place above ground for now
            lastGeoPos[2] = 1000;

        } else if (src.equals(geoIsActive)) {
            if (geoIsActive.getValue()) {
                if (entityDragging) {
                    // TODO: need to restore touchsensor
                    entityDragging = false;

                    MoveEntityCommand cmd = new MoveEntityCommand(
                        model,
                        transactionID,
                        currentEntity.getEntityID(),
                        lastGeoPos,
                        new double[3]);
                    cmd.setErrorReporter(errorReporter);
                    model.applyCommand(cmd);

                    SFBool enabled = (SFBool) tsEnabledMap.get(currentEntity.getEntityID());
                    enabled.setValue(true);
                } else {
                    System.out.println("Set drop to true");
                    dropping = true;
                }
            } else {
                System.out.println("Drop it baby: " + geoIsActive.getValue());
                dropping = false;

                if (currentTool != null) {

                    EntityBuilder builder = EntityBuilder.getEntityBuilder();
                    Entity newEntity;

                    int entityID = model.issueEntityID();

                    tmpRot[0] = 0;
                    tmpRot[1] = 1;
                    tmpRot[2] = 0;
                    tmpRot[3] = 0;

                    // Place new items
                    switch(currentTool.getToolType()) {
                        case Tool.TYPE_MODEL:
                            newEntity = builder.createEntity(model, entityID,
                                    lastGeoPos, tmpRot, currentTool);

                            AddEntityCommand cmd =
                                new AddEntityCommand(model, newEntity);
                            model.applyCommand(cmd);

                            multiSegmentOp = false;

                            List<Entity> selected = new ArrayList<Entity>();
                            selected.add(newEntity);

                            changeSelection(selected);
                            break;
                    }
                }
            }
        } else if (src.equals(keyRelease)) {
            System.out.println("Got key: " + keyRelease.getValue());
            int val = Character.getNumericValue(keyRelease.getValue().charAt(0));
            System.out.println("val: " + val);

            if (val == -1) {
                setMode(MouseMode.SELECTION, true);
                toolBarManager.setTool(null);
                ViewManager.getViewManager(model).disableAssociateMode();
            }
        } else {
            // Check object touch sensors
            Integer entityID = tsMap.get(src);
            if (entityID != null) {
                System.out.println("Found TS");
                SFBool enabled = (SFBool) tsEnabledMap.get(entityID);
                enabled.setValue(false);

                entityDragging = true;
                currentEntity = model.getEntity(entityID.intValue());
            }
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

    //---------------------------------------------------------
    // Methods defined by BrowserListener
    //---------------------------------------------------------

    /** The Browser Listener. */
    public void browserChanged( final BrowserEvent be ) {
        final int id = be.getID( );
        switch (id) {
        case BrowserEvent.INITIALIZED:
            timeSensor.setEnabled(true);
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
        configElevation = enable;
    }

    /**
     * Return the state of automated configuration of the elevation
     * for added and moved entities.
     *
     * @return The enabled state
     */
    public boolean getConfigElevation() {
        return(configElevation);
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Generate the X3D for an entity and replace the children contents with it.
     *
     * @param entityID The entity to generate
     * @param children The children to overwrite
     * @param scene The scene to add routes
     */
    private void generateX3D(int entityID, MFNode children, X3DScene mainScene) {
        StringWriter writer = new StringWriter(1024);

        exporter.export(model, entityID, "view", writer);
        String x3d = writer.toString();

        // if the X3D is empty then exit
        if (x3d.equals(""))
            return;
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

        // Copy the scene into the main one
        X3DNode[] nodes = scene.getRootNodes();

        int len = nodes.length;

        // Nodes must be removed before adding to another scene
//System.out.println("Nodes to remove: " + len);
        for(int i=0; i < len; i++) {
//System.out.println("Node name: " + nodes[i].getNodeName());

            scene.removeRootNode(nodes[i]);
        }

//System.out.println("children.getSize(): " + children.getSize());

        children.setValue(len, nodes);

        // TODO: Need to handle routes and how to remove old ones?
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
        pickNode.setEnabled(true);
    }

    /**
     * Initialize the pick node's target geometry with everything
     * pickable in the main scene
     */
    private void initializePickTargets() {
        // get references to the pickable geometry in the scene
        X3DNode[] node = mainScene.getRootNodes();
        targetList = new Vector<X3DNode>();
        SceneUtils.getPickTargets(node, targetList);
        node = (X3DNode[])targetList.toArray(new X3DNode[targetList.size()]);
        pickTargetReady = false;
        pickNode.setPickTarget(node);
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

    private void addNetworkNodes() {
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

    /**
     * Change the selected entity.
     *
     * @param id The entity selected
     * @param subid The sub entity id
     */
    private void changeSelection(List<Entity> selected) {

        List<Selection> list = new ArrayList<Selection>(selected.size());

        for (int i = 0; i < selected.size(); i++) {
            Entity e = selected.get(i);
            int segmentID = -1;
            int vertexID = -1;
            if (e instanceof SegmentableEntity) {
                segmentID = ((SegmentableEntity)e).getSelectedSegmentID();
                vertexID = ((SegmentableEntity)e).getSelectedVertexID();
            }

            Selection selection =
                new Selection(e.getEntityID(), segmentID, vertexID);

            list.add(selection);
        }

        // if nothing is selected then select the location
        if (list.size() == 0) {
            Entity location = model.getLocationEntity();
            Selection selection = new Selection(location.getEntityID(), -1, -1);

            list.add(selection);
        }

        model.changeSelection(list);
    }

    /**
     * Copy the contents of one scene into another.  This will remove the contents
     * from the old scene.
     *
     * @param src The source scene
     * @param dest The destination scene
     * @param root The root node to place nodes
     */
    private void copyScene(X3DScene src, X3DScene dest, X3DNode root) {
        X3DNode[] relo_node = src.getRootNodes();
        int size;
        MFNode children;

        children = (MFNode) root.getField("children");

        size = relo_node.length;
        for (int i = 0; i < size; i++) {
            X3DNode node = relo_node[i];
            src.removeRootNode(node);
            children.append(node);
        }

        X3DRoute[] routes = src.getRoutes();
        size = routes.length;

        for(int i=0; i < size; i++) {
            X3DRoute route = routes[i];
            src.removeRoute(route);
            dest.addRoute(route.getSourceNode(), route.getSourceField(),
               route.getDestinationNode(), route.getDestinationField());
        }
    }

    /**
     * Resets all entity management state variables.
     */
    private void resetState() {
        toolImage = null;
        currentTool = null;
        multiSegmentOp = false;
        //mapPanel.setDisplayCursor(null);
    }

    /**
     * Create an Image from a url.  Cache the results.
     *
     * @return The Image or null if not found
     */
    private Image getImage(String url) {

        Image image = (Image) imageMap.get(url);
        if (image == null) {
            ImageIcon icon = new ImageIcon(url);

            if (icon == null) {
                errorReporter.messageReport("Can't find image: " + url);
                return null;
            }

            image = icon.getImage();
            imageMap.put(url, image);
        }

        return image;
    }

    /**
     * Set the operational mode of the View
     *
     * @param mode The mode to set. NAVIGATION, PLACEMENT, or SELECTION
     * @param resetState Reset the currect state variables,
     *          selected tool, cursor, etc.
     */
    public void setMode(MouseMode mode, boolean resetState) {

        // initialize state for navigation mode
        if (resetState) {
            resetState();
        }

        resetNavigateState();

        switch(mode) {
            case NAVIGATION:
                currentMode = MouseMode.NAVIGATION;

                navigateButton.setSelected(true);
                panButton.setEnabled(true);
                boundButton.setEnabled(true);

//                geoEnabled.setValue(false);
                cursorManager.setCursor(null,0,0);
/*
                if (boundIsActive) {
                    mapPanel.setDisplayCursor(crossHairCursor);
                } else if (panIsActive) {
                    mapPanel.setDisplayCursor(openHandCursor);
                }
*/
                break;

            case PLACEMENT:
                // initialize state for place mode
                currentMode = MouseMode.PLACEMENT;

                pickAndPlaceButton.setSelected(true);
                panButton.setEnabled(false);
                boundButton.setEnabled(false);

                cursorManager.setCursor("PREDEFINED." + Cursor.DEFAULT_CURSOR,0,0);
                setDisplayCursor(null);
                //geoEnabled.setValue(true);
                break;

            case SELECTION:
                // initialize state for pick mode
                currentMode = MouseMode.SELECTION;

                setDisplayCursor(null);
                cursorManager.setCursor(null,0,0);

                pickAndPlaceButton.setSelected(true);
                panButton.setEnabled(false);
                boundButton.setEnabled(false);

                //entityDragging = false;

                break;

            case ASSOCIATE:

                // initialize state for pick mode
                currentMode = MouseMode.ASSOCIATE;

                pickAndPlaceButton.setSelected(true);
                panButton.setEnabled(false);
                boundButton.setEnabled(false);

                //entityDragging = false;
                //ignoreDrag = true;
                associateMode = true;

                //setDisplayCursor(associateCursor);

                break;

            default:
                errorReporter.messageReport("GT2DView: Unknown Operation Mode");
        }
    }
    /**
     * Reset the navigation control parameters to a default settings
     */
    private void resetNavigateState() {
//        isOverNavControl = false;
//        panInProgress = false;
//        boundInProgress = false;
    }

    /**
     * Set the cursor.
     *
     * @param c The new cursor, null to return to default
     */
    private void setDisplayCursor(Cursor c) {
    }
}
