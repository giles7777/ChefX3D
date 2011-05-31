/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2009
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there"s a problem you get to fix it.
 *
 ****************************************************************************/

package demos;

// External Imports
import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.actions.awt.*;
import org.chefx3d.catalog.*;
import org.chefx3d.model.*;
import org.chefx3d.property.*;
import org.chefx3d.property.awt.*;
import org.chefx3d.rules.interpreters.ValidatingCommandInterpreter;
import org.chefx3d.tool.*;
import org.chefx3d.toolbar.*;
import org.chefx3d.toolbar.awt.*;
import org.chefx3d.util.*;
import org.chefx3d.view.*;
import org.chefx3d.view.awt.*;
import org.chefx3d.view.awt.entitytree.ViewTreeAction;
import org.chefx3d.view.awt.av3d.*;

//import org.chefx3d.view.awt.av3d.dynamicGenerators.DefaultDynamicSegmentEntity;

/**
 * A test case for the AV3DView. This is shamelessly ripped off from 
 * the SimpleExample/BaseExample demo and partially de-cruft-ified.
 *
 * @author Rex Melton
 * @version $Revision: 1.20 $
 */
public class AV3DExample extends JFrame implements WindowListener {
	
	/** The content path */
	protected static final String cpath = "catalog/";
	
	/** The image path */
	protected static final String ipath = "images/2d/";
	
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
	
	/** The toolbar */
	protected ToolBar toolbar;
	
	/** The world model */
	protected WorldModel model;
	
	/** The view manager */
	protected ViewManager viewManager;
	
	/** The ToolBarManager */
	protected ToolBarManager toolBarManager;
	
	/** The 2d view */
	protected View viewEditor;
	
	/** The ToolBarFactory to use */
	protected ToolBarFactory toolbarFactory;
	
	protected DefaultPropertyEditor propertyEditor;
	
	/** The Command Controller */
	protected CommandController controller;
	
	/** The ErrorReporter for messages */
	protected ErrorReporter errorReporter;
	
	protected Entity startEntity;
	
	/**
	 * Create a new basic instance of the example
	 */
	public AV3DExample() {
		super("ChefX3D");
		
		I18nManager intl_mgr = I18nManager.getManager();
		intl_mgr.setApplication("AV3DExample",
			"config.i18n.chefx3dResources");
		
		// Create the default error reporter
		errorReporter = DefaultErrorReporter.getDefaultReporter();
		errorReporter.showLevel(ErrorReporter.DEBUG);
		
		// Create the command executor
		controller = new ValidatingBufferedCommandController();
		controller.setErrorReporter(errorReporter);
		
		////////////////////////////////////////////////////////////
		// Create the initial entity data model
		model = new DefaultWorldModel(controller);
		model.setErrorReporter(errorReporter);
		
		Command cmd = null;
		ArrayList<Command> commandList = new ArrayList<Command>();
		
		SceneEntity se = 
			new SceneEntity(model.issueEntityID(), null);
		cmd = new AddEntityCommand(model, se);  
        commandList.add(cmd);
		
		LocationEntity le = 
			new LocationEntity(model.issueEntityID(), null);
		cmd = new AddEntityChildCommand(se, le);  
        commandList.add(cmd); 
		
		ViewpointContainerEntity vce = 
			new ViewpointContainerEntity(model.issueEntityID(), null);
		cmd = new AddEntityChildCommand(le, vce);  
        commandList.add(cmd); 
		
		ContentContainerEntity cce = 
			new ContentContainerEntity(model.issueEntityID(), null);
		cmd = new AddEntityChildCommand(le, cce);  
        commandList.add(cmd); 
		
		Entity floorEntity = 
			new DefaultPositionableEntity(model.issueEntityID(), null);
		cmd = new AddEntityChildCommand(cce, floorEntity);
		commandList.add(cmd); 
		
		// select the floor
        cmd = new SelectEntityCommand(floorEntity, true);        
        commandList.add(cmd);       
  
		cmd = new MultiCommand(commandList, "");
		controller.execute(cmd);
		////////////////////////////////////////////////////////////
		
		// Create a ViewManager
		viewManager = ViewManager.getViewManager();
		viewManager.setErrorReporter(errorReporter);
		viewManager.setWorldModel(model);
		
		// Create a toolBarManager
		toolBarManager = ToolBarManager.getToolBarManager();
		toolBarManager.setErrorReporter(errorReporter);
		
		// Create the EntityBuilder
		EntityBuilder builder = DefaultEntityBuilder.getEntityBuilder();
		
		// Create the AV3d view
		viewEditor = new AV3DView(model, controller);
		viewEditor.setEntityBuilder(builder);
		viewEditor.setErrorReporter(errorReporter);
		
		// wait to set the command validator till after the view has been instantiated
		ValidatingCommandInterpreter validator = 
			new ValidatingCommandInterpreter(errorReporter, (AV3DView)viewEditor);
		((ValidatingBufferedCommandController)controller).setCommandValidator(validator);
		
		// Setup the mainFrame and the contentPane for it
		Container contentPane = getContentPane();
		
		BorderLayout layout = new BorderLayout();
		contentPane.setLayout(layout);
		
		// Add the menuBar to the mainFrame
		setJMenuBar(createMenus());
		
		// create the 2D viewer
		JPanel viewPanel = new JPanel(new BorderLayout());
		viewPanel.add((JComponent)viewEditor.getComponent(), BorderLayout.CENTER);
		
        // Create the property editor for the objects placed in the scene
        PropertyEditorFactory editorFactory = new AWTPropertyEditorFactory();
        propertyEditor = (DefaultPropertyEditor)editorFactory.createEditor(model, this);
        propertyEditor.setErrorReporter(errorReporter);

        // define a custom set of JTable renderers and editors if necessary
        DefaultPropertyTableCellFactory editorCellFactory =
            new DefaultPropertyTableCellFactory(this);
        propertyEditor.setEditorCellFactory(editorCellFactory);

		// Create the properties panel
		JPanel propertyPanel = new JPanel(new BorderLayout());
		propertyPanel.add((JComponent)propertyEditor.getComponent(),
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
		startEntity = null;
		createRootEntity();
	}
	
	//---------------------------------------------------------------
	// Methods defined by WindowListener
	//---------------------------------------------------------------
	
	/**
	 * Ignored
	 */
	public void windowActivated(WindowEvent evt) {
	}
	
	/**
	 * Ignored
	 */
	public void windowClosed(WindowEvent evt) {
	}
	
	/**
	 * Exit the application
	 *
	 * @param evt The event that caused this method to be called.
	 */
	public void windowClosing(WindowEvent evt) {
		((ValidatingBufferedCommandController)controller).shutdown();
		System.exit(0);
	}
	
	/**
	 * Ignored
	 */
	public void windowDeactivated(WindowEvent evt) {
	}
	
	/**
	 * Ignored
	 */
	public void windowDeiconified(WindowEvent evt) {
	}
	
	/**
	 * Ignored
	 */
	public void windowIconified(WindowEvent evt) {
	}
	
	/**
	 * When the window is opened, start everything up.
	 */
	public void windowOpened(WindowEvent evt) {
	}
	
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
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
					
					AV3DExample av3d = new AV3DExample();
					
					av3d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					av3d.setSize(1200, 700);
					av3d.setLocation(10, 10);
					av3d.pack();
					av3d.setVisible(true);
				}
			});
	}
	
	/**
	 * Create ChefX3D tools.
	 */
	protected void createTools() {
		//System.out.println("createTools");
		// set to true if you want to use the DrillDownToolBar
		// otherwise set to false
		boolean includeRootNode = true;
		
		ArrayList<ToolGroup> tools = new ArrayList<ToolGroup>();
		ArrayList<ToolGroupChild> chapters = null;
		SimpleTool tool;
		ToolGroup td;
		
		ToolGroup root = new ToolGroup("Tools");
		
		// Locations Menu
		// Grid World
		chapters = new ArrayList<ToolGroupChild>();
		
		String[] interfaceIcons =
			new String[] {
			ipath + "Grid16x16.png",
				ipath + "Grid32x32.png",
				ipath + "Grid64x64.png"};
		
		HashMap<String, Object> entityProperties = new HashMap<String, Object>();
		
		tool = new SimpleTool(
			"Grid",
			ipath + "grid.png",
			interfaceIcons,
			Entity.TYPE_WORLD,
			"Grid.x3d",
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
		if (includeRootNode)
			root.addToolGroup(td);
		else
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
		String[] items = new String[] {"Box", "Cone", "Cylinder", "Sphere"};
		String[] pictures = new String[] {
			ipath + "Box64x64.png", 
				ipath + "Cone64x64.png", 
				ipath + "Cylinder64x64.png", 
				ipath + "Sphere64x64.png"};
		
		ListProperty list = new ListProperty(items, pictures);
		list.setValue(items[0]);
		entityProperties.put("Selection Test", list);
		
		// a test checkbox property
		entityProperties.put("CheckBox Test", true);
		
		// add the color property
		entityProperties.put("Color", new Color(255, 0, 0));
		
		// assign a validation class for position
		HashMap<String, Object> propertyValidators = new HashMap<String, Object>();
		ColorValidator validColor = new ColorValidator();
		propertyValidators.put("Color", validColor);
		
		HashMap<String, Map<String, Object>> properties = new HashMap<String, Map<String, Object>>();
		properties.put(Entity.DEFAULT_ENTITY_PROPERTIES, entityProperties);
		properties.put(Entity.PROPERTY_VALIDATORS, propertyValidators);
		
        String name = "This is a cool red box that has a long name";
        String desc = "This is the Box Primitive hover over text.  This can be very log if we need to.";

		tool = new SimpleTool(
			name,
			ipath + "Box.png",
			interfaceIcons,
			Entity.TYPE_MODEL,
			"Box.x3d",
			desc,
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
		
		tool = new SimpleTool(
			"Cone",
			ipath + "Cone.png",
			interfaceIcons,
			Entity.TYPE_MODEL,
			"Cone.x3d",
			"Cone Primitive",
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
		
		tool = new SimpleTool(
			"Cylinder",
			ipath + "Cylinder.png",
			interfaceIcons,
			Entity.TYPE_MODEL,
			"Cylinder.x3d",
			"Cylinder Primitive",
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
		
		tool = new SimpleTool(
			"Sphere",
			ipath + "Sphere.png",
			interfaceIcons,
			Entity.TYPE_MODEL,
			"Sphere.x3d",
			"Sphere Primitive",
			new float[] {1f, 1f, 1f},
			new float[] {1f, 1f, 1f},
			MultiplicityConstraint.NO_REQUIREMENT,
			"Model1",
			false,
			false,
			false,
			false,
			entityProperties);
		
		ToolGroup sub = new ToolGroup("Subgroup");
		chapters.add(sub);
		
		sub.addTool(tool);
		
		td = new ToolGroup("Primitives", chapters);        
		if (includeRootNode)
			root.addToolGroup(td);
		else
			tools.add(td);
		
		// Segment Tools
		chapters = new ArrayList<ToolGroupChild>();
		
		// TODO: fix fencing
		tool = buildWallTool();
		chapters.add(tool);
		
		tool = buildWaypointTool();
		chapters.add(tool);
		
		tool = createAStarTool();
		chapters.add(tool);
		
		tool = createBuildingTool();
		chapters.add(tool);
		
		td = new ToolGroup("Feature Tools", chapters);
		if (includeRootNode)
			root.addToolGroup(td);
		else
			tools.add(td);
		
		
		CatalogManager cmanager = CatalogManager.getCatalogManager();
		Catalog catalog = new Catalog("Sample", 1, 0);
		cmanager.addCatalog(catalog);
		
		if (includeRootNode)
			catalog.addToolGroup(root);
		else
			catalog.addTools(tools);
		
		// debug, print the catalog
		catalog.printCatalog();
	}
	
	/**
	 * Create tools for authoring walls
	 */
	private SegmentableTool buildWallTool() {
		
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
	 * Create tools for authoring waypoints
	 */
	private SegmentableTool buildWaypointTool() {
		
		HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        HashMap<String, Object> segmentProperties = new HashMap<String, Object>();
		HashMap<String, Object> vertexProperties = new HashMap<String, Object>();
		vertexProperties.put(Entity.MODEL_URL_PARAM, "Sphere.x3d"); 
		// Fence
		String[] interfaceIcons = new String[] {
			ipath + "Segment16x16.png",
				ipath + "Segment32x32.png",
				ipath + "Segment64x64.png"};
		
//		SegmentTool tool = new SegmentTool(
//			"Waypoint",
//			ipath + "Segment.png",
//			interfaceIcons,
//			Entity.TYPE_MULTI_SEGMENT,
//			"Box.x3d",
//			"Waypoint",
//			new float[] {1f, 1f, 1f},
//			new float[] {0.3f, 0.3f, 0.3f},
//			MultiplicityConstraint.NO_REQUIREMENT,
//			"Waypoint",
//			false,
//			false,
//			true,
//			false,
//			0,
//			true,
//			entityProperties,
//			segmentProperties, 
//			vertexProperties);
//		
		return null;
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
		
//		SegmentTool tool = new SegmentTool(
//			"AStar",
//			ipath + "Segment.png",
//			interfaceIcons,
//			Entity.TYPE_MULTI_SEGMENT,
//			"x3d/No3DRep.x3d",
//			"AStar",
//			new float[] {1f, 1f, 1f},
//			new float[] {0.3f, 0.3f, 0.3f},
//			MultiplicityConstraint.NO_REQUIREMENT,
//			"AStar",
//			false,
//			false,
//			true,
//			false,
//			0,
//			false,
//			entityProperties,
//			segmentProperties, 
//			vertexProperties);
//		
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
		
//		// building
//		BuildingTool tool = new BuildingTool(
//			"Building",
//			ipath + "BuildingBox.png",
//			null,
//			Entity.TYPE_BUILDING,
//			"x3d/No3DRep.x3d",
//			"Building",
//			new float[] {1f, 1f, 1f},
//			new float[] {1f, 1f, 1f},
//			MultiplicityConstraint.NO_REQUIREMENT,
//			"Building",
//			false,
//			true,
//			true,
//			false,
//			entityProperties,
//			segmentProperties,
//			vertexProperties,
//			true);
		
		return null;
	}
	
	private JScrollPane createLeftPanel() {
		
		JTabbedPane tabbedPane = new JTabbedPane();
		
		// Create the toolbar
		//toolbarFactory = new AWTToolBarFactory();
		//toolbarFactory.setErrorReporter(errorReporter);
		//toolbar = toolbarFactory.createToolBar(model, ToolBar.VERTICAL, true);
		
		// Create the IconToolbar specifically
		//IconToolBar iconTool = new IconToolBar(model);
		Dimension breadCrumbSize = new Dimension(200, 20);
		DrillDownToolbar iconTool = new DrillDownToolbar(model, "->", breadCrumbSize);
		iconTool.setCatalog("Sample");
		
		
		// Add the toolbar
		JPanel toolPanel = new JPanel(new BorderLayout());
		toolPanel.setPreferredSize(new Dimension(200, 700));
		toolPanel.add((JComponent) iconTool.getComponent(), BorderLayout.CENTER);
		
		tabbedPane.addTab("Tools", toolPanel);
		
		//TreeToolBar treeTool = new TreeToolBar();
		//treeTool.setCatalog("Sample");
		
		//JPanel treePanel = new JPanel(new BorderLayout());
		//treePanel.setPreferredSize(new Dimension(200, 700));
		//treePanel.add((JComponent) treeTool.getComponent(), BorderLayout.CENTER);
		
		//tabbedPane.addTab("Tool Tree", treePanel);
		
		// Create the scroll Panel UI
		JScrollPane scrollPane = new JScrollPane();
		JViewport viewport = scrollPane.getViewport();
		//viewport.add(toolPanel);
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
		
		// Define the MenuGroup
		JMenu view = new JMenu("View");
		edit.setMnemonic(KeyEvent.VK_V);
		mb.add(view);
		
		view.addSeparator();
		
		// Define the ViewTree Action
		ViewTreeAction viewTreeAction = new ViewTreeAction(model);
		view.add(viewTreeAction);
		
		return mb;
	}
	
	/** Create ChefX3D Root entity*/
	private void createRootEntity(){
		//System.out.println("createTools");
		// set to true if you want to use the DrillDownToolBar
		// otherwise set to false
		
		SimpleTool tool;
		
		String[] interfaceIcons =
			new String[] {
			ipath + "Grid16x16.png",
				ipath + "Grid32x32.png",
				ipath + "Grid64x64.png"};
		
		HashMap<String, Object> entityProperties = new HashMap<String, Object>();
		EntityBuilder builder =  DefaultEntityBuilder.getEntityBuilder();
		tool = new SimpleTool(
			"Root Entity",
			ipath + "grid.png",
			interfaceIcons,
			Entity.TYPE_WORLD,
			null,
			"Root",
			new float[] {1f, 1f, 1f},
			new float[] {1f, 1f, 1f},
			MultiplicityConstraint.SINGLETON,
			"World",
			false,
			false,
			false,
			false,
			entityProperties);
		
		/*
		int entityID = model.issueEntityID();
		double[] position = new double[]{0,0,0};
		startEntity =  builder.createEntity(model, entityID,
			position, new float[] {0,1,0,0}, tool);
		
		AddEntityCommand cmd = new AddEntityCommand(model, startEntity);
		model.applyCommand(cmd);
		*/
	}
}
