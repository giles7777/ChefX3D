/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2008
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package demos;

// External Imports
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.*;

// Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.toolbar.*;
import org.chefx3d.toolbar.awt.*;
import org.chefx3d.view.*;
import org.chefx3d.view.awt.*;
import org.chefx3d.property.*;
import org.chefx3d.property.awt.*;
import org.chefx3d.actions.awt.*;
import org.chefx3d.tool.*;
import org.chefx3d.catalog.*;
import org.chefx3d.catalog.util.awt.*;

import org.chefx3d.view.awt.gt2d.GT2DView;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * An example of how to dynamically create new Locations
 *
 * @author Rex Melton
 * @version $Revision: 1.8 $
 */
public class CreateLocationExample implements
    ActionListener, WindowListener, Runnable {

    ///////////////////////////////////////////////////////////////////////////
    // modify this string to use your own world......
    /** Location of the globe entry file */
    protected static final String WORLD_URL_STRING =
        "file:/C:/yumetech/cygwin/home/Russell/projects/ChefX3D/examples/globe/tiles/globe.x3d";
    ///////////////////////////////////////////////////////////////////////////

    /** The hardcoded base name for saving files */
    protected static final String BASENAME = "foo";

    /** The content path */
    //protected static final String cpath = "examples/catalog/";
    protected static final String cpath = "catalog/";

    /** The image path */
    //protected static final String ipath = "examples/images/";
    protected static final String ipath = "images/";

    private final String[] DISPLAY_PANELS =
        new String[] {"SMAL", "Cost", "Segment", "Vertex"};

    /** The exit menu item */
    protected JMenuItem exit;

    /** The Undo command menu item */
    protected JMenuItem undo;

    /** The Redo command menu item */
    protected JMenuItem redo;

    /** The clear command menu item */
    protected JMenuItem clear;

    /** The delete command menu item */
    protected JMenuItem delete;

    /** The launch treeView menu item */
    protected JMenuItem treeView;

    /** location browser selector */
    protected JMenuItem locBrowserMenuItem;

    /** The toolbar */
    protected ToolBar toolbar;

    /** The world model */
    protected WorldModel model;

    /** The view manager */
    protected ViewManager viewManager;

    /** The ToolBarManager */
    protected ToolBarManager toolBarManager;

    /** The main window */
    protected JFrame mainFrame;

    /** The 2d view */
    protected GT2DView view2d;

    /** The PropertyEditor to use */
    protected PropertyEditorFactory PropertyEditorFactory;

    /** The ToolBarFactory to use */
    protected ToolBarFactory toolbarFactory;

    /** The ViewFactory to use */
    protected ViewFactory viewFactory;

    protected DefaultPropertyEditor propertyEditor;

    /** The Command Controller */
    private CommandController controller;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;

    protected LocationBrowser locBrowser;

    public CreateLocationExample() {

        // the main frame to attach everything too
        mainFrame = new JFrame("ChefX3D");

        // Create the default error reporter
        errorReporter = DefaultErrorReporter.getDefaultReporter();
        errorReporter.showLevel(ErrorReporter.DEBUG);

        // create the command controller
        controller = new DefaultCommandController();
        controller.setErrorReporter(errorReporter);

        // Create the 3d world
        model = new DefaultWorldModel(controller);
        model.setErrorReporter(errorReporter);

        // Create a ViewManager
        viewManager = ViewManager.getViewManager();
        viewManager.setErrorReporter(errorReporter);
        viewManager.setWorldModel(model);

        // Create a toolBarManager
        toolBarManager = ToolBarManager.getToolBarManager();
        toolBarManager.setErrorReporter(errorReporter);

        // Create the property editor for the objects placed in the scene
        PropertyEditorFactory editorFactory = new AWTPropertyEditorFactory();
        propertyEditor = (DefaultPropertyEditor)editorFactory.createEditor(model, mainFrame);
        propertyEditor.setErrorReporter(errorReporter);

        // define a custom set of JTable renderers and editors if necessary
        DefaultPropertyTableCellFactory editorCellFactory =
            new DefaultPropertyTableCellFactory(mainFrame);
        propertyEditor.setEditorCellFactory(editorCellFactory);

        // Create the 2d view
        view2d = new GT2DView(model, ipath);
        view2d.setErrorReporter(errorReporter);

        // Setup the mainFrame and the contentPane for it
        Container contentPane = mainFrame.getContentPane();

        BorderLayout layout = new BorderLayout();
        contentPane.setLayout(layout);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add the menuBar to the mainFrame
        mainFrame.setJMenuBar(createMenus());

        // create the 2D viewer
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.add((JComponent) view2d.getComponent(), BorderLayout.CENTER);

        // Create the properties panel
        JPanel propertyPanel = new JPanel(new BorderLayout());
        propertyPanel.add((JComponent) propertyEditor.getComponent(),
                BorderLayout.CENTER);

        // Add the components to the contentPane
        JScrollPane propertyScrollPane = new JScrollPane( propertyPanel );

        JSplitPane contentSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, viewPanel, propertyScrollPane );

        contentSplitPane.setOneTouchExpandable( true );

        // force the resize weight to work by setting the min size
        Dimension viewMinDim = new Dimension( 600, 600 );
        Dimension propMinDim = new Dimension( 0, 600 );
        viewPanel.setMinimumSize( viewMinDim );
        propertyScrollPane.setMinimumSize( propMinDim );

        propertyScrollPane.setPreferredSize( new Dimension( 400, 600 ) );

        // 60% to the view panel, 40% to the property panel
        contentSplitPane.setResizeWeight(0.4);

        JScrollPane toolPanel = createLeftPanel();

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, toolPanel, contentSplitPane );

        splitPane.setOneTouchExpandable( true );

        // force the resize weight to work by setting the min size
        toolPanel.setMinimumSize( propMinDim );
        contentSplitPane.setMinimumSize( propMinDim );

        // 80% to the content pane, 20% to the tool panel
        splitPane.setResizeWeight(0.8);

        contentPane.add( splitPane, BorderLayout.CENTER );

        // Populate the toolPanel
        createTools();

        mainFrame.setSize(1200, 700);
        mainFrame.setLocation(10, 10);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    // ----------------------------------------------------------
    // Methods required by ActionListener
    // ----------------------------------------------------------

    public void actionPerformed(ActionEvent e) {

        Object action = e.getSource();

        if (action == locBrowserMenuItem) {
            EventQueue.invokeLater(this);
        }
    }

    // ----------------------------------------------------------
    // Methods required by Runnable
    // ----------------------------------------------------------

    /**
     * UI reconfiguration that takes place on the event queue
     */
    public void run() {

        locBrowser = new LocationBrowser(WORLD_URL_STRING, "Sample", "Locations");

        JFrame frame = new JFrame("Create Location");
        frame.addWindowListener(this);

        Container contentPane = frame.getContentPane();
        contentPane.add(locBrowser, BorderLayout.CENTER);

        frame.setResizable(false);
        frame.pack();
        frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE);
        Dimension screenSize = frame.getToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frame.setLocation(
            (screenSize.width - frameSize.width)/2,
            (screenSize.height - frameSize.height)/2 );
        frame.setVisible(true);
    }

    // ----------------------------------------------------------
    // Methods required by WindowListener
    // ----------------------------------------------------------

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        //locBrowser.shutdown();
        locBrowserMenuItem.setEnabled(true);
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
        locBrowserMenuItem.setEnabled(false);
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Create ChefX3D tools.
     */
    protected void createTools() {

        ArrayList<ToolGroup> tools = new ArrayList<ToolGroup>();
        ArrayList<ToolGroupChild> chapters = null;
        Tool tool;
        ToolGroup td;

        // Locations Menu
        // Grid World
        chapters = new ArrayList<ToolGroupChild>();

        String[] interfaceIcons =
            new String[] {
                ipath + "Grid16x16.png",
                ipath + "Grid32x32.png",
                ipath + "Grid64x64.png"};

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Grid",
                ipath + "Grid.png",
                interfaceIcons,
                Entity.TYPE_WORLD,
                cpath + "Locations/Grid/Grid.x3dv",
                "Basic City Location",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.SINGLETON,
                "World",
                false,
                false,
                false,
                false,
                entityProperties);

        chapters.add(tool);

        td = new ToolGroup("Locations", chapters);
        tools.add(td);

        // Primitives Menu
        // Box
        chapters = new ArrayList<ToolGroupChild>();

        interfaceIcons = new String[] {
                ipath + "Box16x16.png",
                ipath + "Box32x32.png",
                ipath + "Box64x64.png"};

        entityProperties = new HashMap<String, Object>();

        // a test associate property
        AssociateProperty associate1 =
            new AssociateProperty("Associated 1", new String[] {"Model", "Waypoint"});
        entityProperties.put("Associated 1", associate1);

        // a test associate property
        AssociateProperty associate2 =
            new AssociateProperty("Associated 2", new String[] {"Model1"});
        entityProperties.put("Associated 2", associate2);

        // a test list property
        ListProperty list = new ListProperty(new String[] {"Item1", "Item2", "Item3"});
        list.setValue("Item1");
        entityProperties.put("Selection Test", list);

        // a test checkbox property
        entityProperties.put("CheckBox Test", true);

        // add the color property
        entityProperties.put("Color", new int[] {204, 204, 204});

        // assign a validation class for position
        HashMap<String, Object> propertyValidators = new HashMap<String, Object>();
        ColorValidator validColor = new ColorValidator();
        propertyValidators.put("Color", validColor);

        HashMap<String, Map<String, Object>> properties = new HashMap<String, Map<String, Object>>();
        properties.put(Entity.DEFAULT_ENTITY_PROPERTIES, entityProperties);
        properties.put(Entity.PROPERTY_VALIDATORS, propertyValidators);

        tool = new Tool(
                "Box",
                ipath + "Box.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Primitives/Box/Box.x3d",
                "Box Primative",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model",
                false,
                false,
                false,
                false,
                properties);

        chapters.add(tool);

        // Cone
        interfaceIcons = new String[] {
                ipath + "Cone16x16.png",
                ipath + "Cone32x32.png",
                ipath + "Cone64x64.png"};

        entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Cone",
                ipath + "Cone.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Primitives/Cone/Cone.x3d",
                "Cone Primative",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model",
                false,
                false,
                false,
                false,
                entityProperties);

        chapters.add(tool);

        // Cylinder
        interfaceIcons = new String[] {
                ipath + "Cylinder16x16.png",
                ipath + "Cylinder32x32.png",
                ipath + "Cylinder64x64.png"};

        entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Cylinder",
                ipath + "Cylinder.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Primitives/Cylinder/Cylinder.x3d",
                "Cylinder Primative",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model1",
                false,
                false,
                false,
                false,
                entityProperties);

        chapters.add(tool);

        // Sphere
        interfaceIcons = new String[] {
                ipath + "Sphere16x16.png",
                ipath + "Sphere32x32.png",
                ipath + "Sphere64x64.png"};

        entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Sphere",
                ipath + "Sphere.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Primitives/Sphere/Sphere.x3d",
                "Sphere Primative",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model1",
                false,
                false,
                false,
                false,
                entityProperties);

        chapters.add(tool);

        td = new ToolGroup("Primitives", chapters);

        tools.add(td);

        // Segment Tools
        chapters = new ArrayList<ToolGroupChild>();

        // TODO: fix fencing

        tool = buildWaypointTool();
        chapters.add(tool);

        tool = createAStarTool();
        chapters.add(tool);

        tool = createBuildingTool();
        chapters.add(tool);

        td = new ToolGroup("Feature Tools", chapters);
        tools.add(td);

        CatalogManager cmanager = CatalogManager.getCatalogManager();
        Catalog catalog = new Catalog("Sample", 1, 0);
        cmanager.addCatalog(catalog);
        catalog.addTools(tools);
    }

    /**
     * Create tools for authoring waypoints
     */
    private SegmentableTool buildWaypointTool() {

        // vertex tool      
        Map<String,Map<String, Object>> vertexSheets = 
            new HashMap<String,Map<String, Object>>();
        Map<String, Object> vertexProps = new HashMap<String, Object>();
        vertexSheets.put(Entity.DEFAULT_ENTITY_PROPERTIES, vertexProps);

        VertexTool vertexTool = 
            new VertexTool(
                    "Vertex",
                    ipath + "Segment.png",
                    null,
                    Entity.TYPE_VERTEX,
                    "Sphere.x3d",
                    "Vertex",
                    new float[] {1f, 1f, 1f},
                    new float[] {0.3f, 0.3f, 0.3f},
                    MultiplicityConstraint.NO_REQUIREMENT,
                    "Wall",
                    false,
                    false,
                    true,
                    false,
                    vertexSheets);      
        
        // segment tool
        Map<String,Map<String, Object>> segmentSheets = 
            new HashMap<String,Map<String, Object>>();
        Map<String, Object> segmentProps = new HashMap<String, Object>();
        segmentSheets.put(Entity.DEFAULT_ENTITY_PROPERTIES, segmentProps);

        AbstractSegmentTool segmentTool = 
            new SegmentTool(
                    "Segment",
                    ipath + "Segment.png",
                    null,
                    Entity.TYPE_SEGMENT,
                    "Box.x3d",
                    "Segment",
                    new float[] {1f, 1f, 1f},
                    new float[] {0.3f, 0.3f, 0.3f},
                    MultiplicityConstraint.NO_REQUIREMENT,
                    "Segment",
                    false,
                    false,
                    true,
                    false,
                    segmentSheets);
 
        
        // segmentable tool       
        Map<String,Map<String, Object>> segmentableSheets = 
            new HashMap<String,Map<String, Object>>();
        Map<String, Object> segmentableProps = new HashMap<String, Object>();
        segmentableSheets.put(Entity.DEFAULT_ENTITY_PROPERTIES, segmentableProps);

        String[] interfaceIcons = new String[] {
            ipath + "Segment16x16.png",
                ipath + "Segment32x32.png",
                ipath + "Segment64x64.png"};
        
        SegmentableTool tool = 
            new SegmentableTool(
                    "Wall",
                    ipath + "Segment.png",
                    interfaceIcons,
                    Entity.TYPE_MULTI_SEGMENT,
                    "NoRep.x3d",
                    "Wall",
                    new float[] {1f, 1f, 1f},
                    new float[] {0.3f, 0.3f, 0.3f},
                    MultiplicityConstraint.SINGLETON,
                    "Wall",
                    false,
                    false,
                    true,
                    false,
                    0,
                    true,
                    segmentableSheets,
                    segmentTool, 
                    vertexTool);
                
        return tool;
        
    }

    /**
     * Create tools for authoring a-star networks
     */
    private SegmentableTool createAStarTool() {

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        entityProperties.put("reversible", true);
        
        HashMap<String, Object> segmentProperties = new HashMap<String, Object>();

        HashMap<String, Object> vertexProperties = new HashMap<String, Object>();
        vertexProperties.put("speed", 0);

        // Fence
        String[] interfaceIcons = new String[] {
                ipath + "Segment16x16.png",
                ipath + "Segment32x32.png",
                ipath + "Segment64x64.png"};

//        SegmentableTool tool = new SegmentableTool(
//                "AStar",
//                ipath + "Segment.png",
//                interfaceIcons,
//                Entity.TYPE_MULTI_SEGMENT,
//                "No3DRep.x3d",
//                "AStar",
//                new float[] {1f, 1f, 1f},
//                new float[] {0.3f, 0.3f, 0.3f},
//                MultiplicityConstraint.NO_REQUIREMENT,
//                "AStar",
//                false,
//                false,
//                false,
//                false,
//                0,
//                false,
//                entityProperties,
//                segmentProperties, 
//                vertexProperties);

        return null;
    }

    /**
     * Create tools for authoring buildings
     */
    private BuildingTool createBuildingTool() {

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        HashMap<String, Object> segmentProperties = new HashMap<String, Object>();
        HashMap<String, Object> vertexProperties = new HashMap<String, Object>();

        // define the building properties

        ListProperty storyList = new ListProperty(new String[] {"1", "2", "3", "4", "5"});
        storyList.setValue("1");
        entityProperties.put("Stories", storyList);

        entityProperties.put("Height per Story", 10.0f);

        ListProperty roofList =
            new ListProperty(new String[] {"Flat", "Sloped", "Single Peak", "Complex"});
        roofList.setValue("Flat");
        entityProperties.put("Roof Line", roofList);

        ListProperty wallList =
            new ListProperty(new String[] {"Wood", "Concrete", "Brick", "Metal"});
        wallList.setValue("Wood");
        entityProperties.put("Exterior Wall", wallList);

        entityProperties.put("Color", new int[] {0, 255, 0});

        // define the segment properties

        wallList =
            new ListProperty(new String[] {"Wall", "Door", "Window"});
        wallList.setValue("Wall");
        segmentProperties.put("Type", wallList);

        segmentProperties.put("Reinforced", false);

        ListProperty openingList =
            new ListProperty(new String[] {"Inward", "Outward", "Sliding"});
        openingList.setValue("Inward");
        segmentProperties.put("Opening Type", openingList);

        // define the vertex properties
        // none

        // building
//        BuildingTool tool = new BuildingTool(
//                "Building",
//                ipath + "BuildingBox.png",
//                null,
//                Entity.TYPE_BUILDING,
//                "No3DRep.x3d",
//                "Building",
//                new float[] {1f, 1f, 1f},
//                new float[] {1f, 1f, 1f},
//                MultiplicityConstraint.NO_REQUIREMENT,
//                "Building",
//                false,
//                true,
//                true,
//                false,
//                entityProperties,
//                segmentProperties,
//                vertexProperties,
//                true);

        return null;
    }

    private JScrollPane createLeftPanel() {

        JTabbedPane tabbedPane = new JTabbedPane();

        // Create the IconToolbar specifically
        IconToolBar iconTool = new IconToolBar(model);
        iconTool = new IconToolBar(model);
        iconTool.setCatalog("Sample");

        // Add the toolbar
        JPanel toolPanel = new JPanel(new BorderLayout());
        toolPanel = new JPanel(new BorderLayout());
        toolPanel.setPreferredSize(new Dimension(200, 700));
        toolPanel.add((JComponent) iconTool.getComponent(), BorderLayout.CENTER);

        tabbedPane.addTab("Tools", toolPanel);

        // Create the scroll Panel UI
        JScrollPane scrollPane = new JScrollPane();
        JViewport viewport = scrollPane.getViewport();
        viewport.add(tabbedPane);

        return scrollPane;
    }

    private JMenuBar createMenus() {

        // setup the menu system
        JMenuBar mb = new JMenuBar();

        // Define the FileMenuGroup
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        mb.add(file);

        // Define the Exit Action
        ExitAction exitAction = new ExitAction();
        file.add(exitAction);

        // Define the Edit MenuGroup
        JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        mb.add(edit);

        // Define the Undo Action
        UndoAction undoAction = new UndoAction(false, null, controller);
        edit.add(undoAction);

        // Define the Redo Action
        RedoAction redoAction = new RedoAction(false, null, controller);
        edit.add(redoAction);

        edit.addSeparator();

        // Define the Copy Action
        EntityCopyAction copyAction = new EntityCopyAction(false, null, model);
        edit.add(copyAction);

        // Define the Paste Action
        EntityPasteAction pasteAction = new EntityPasteAction(false, null, model, edit);
        edit.add(pasteAction);

        edit.addSeparator();

        // Define the Delete Action
        DeleteAction deleteAction = new DeleteAction(false, null, model);
        edit.add(deleteAction);

        // Define the Delete All Action
        ResetAction resetAction = new ResetAction(false, null, model);
        edit.add(resetAction);

        // Define the View MenuGroup
        JMenu view = new JMenu("View");
        edit.setMnemonic(KeyEvent.VK_V);
        mb.add(view);

        locBrowserMenuItem = new JMenuItem("Create Location");
        locBrowserMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
                ActionEvent.CTRL_MASK));
        locBrowserMenuItem.setMnemonic(KeyEvent.VK_T);
        locBrowserMenuItem.addActionListener(this);
        view.add(locBrowserMenuItem);

        return mb;

    }

    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Set System L&F
                    UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
                } catch (UnsupportedLookAndFeelException e) {
                    // handle exception
                } catch (ClassNotFoundException e) {
                   // handle exception
                } catch (InstantiationException e) {
                   // handle exception
                } catch (IllegalAccessException e) {
                   // handle exception
                }

                CreateLocationExample example = new CreateLocationExample();
            }
        });
    }
}
