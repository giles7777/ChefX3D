/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2006 - 2007
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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

// Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.model.Entity;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.tool.ToolGroupChild;

import org.chefx3d.rules.interpreters.ValidatingCommandInterpreter;

import org.chefx3d.toolbar.*;
import org.chefx3d.toolbar.awt.*;
import org.chefx3d.view.*;
import org.chefx3d.view.awt.*;
import org.chefx3d.property.*;
import org.chefx3d.property.awt.*;
import org.chefx3d.actions.awt.*;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogManager;

import org.chefx3d.view.awt.entitytree.ViewTreeAction;
import org.chefx3d.view.awt.gt2d.GT2DView;
import org.chefx3d.view.awt.gt2d.ViewingFrustum;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

/**
 * A simple example of how to use ChefX3D
 *
 * @author Russell Dodds
 * @version $Revision: 1.31 $
 */
public abstract class BaseExample
    implements ActionListener, MenuListener, ThumbnailListener {

    /** The hardcoded base name for saving files */
    protected static final String BASENAME = "foo";

    /** The content path */
    protected static final String cpath = "catalog/";

    /** The image path */
    protected static final String ipath = "images/2d/";

    private final String[] DISPLAY_PANELS =
        new String[] {"SMAL", "Cost", "Segment", "Vertex"};


    /** The saveX3D menu item */
    protected JMenuItem saveX3D;

    /** The save thumbnails menu item */
    protected JMenuItem saveThumbnail;

    /** The plane edit menus */
    protected JCheckBoxMenuItem editTop;
    protected JCheckBoxMenuItem editLeft;
    protected JCheckBoxMenuItem editRight;
    protected JCheckBoxMenuItem editFront;


    /** The exit menu item */
    protected JMenuItem exit;

    /** The launchX3D menu item */
    protected JMenuItem launchX3D;

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

    /** The elevation config menu item */
    protected JCheckBoxMenuItem elevationControl;

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

    /** The external viewer */
    protected JFrame externalViewer;

    /** The external entityTreeView */
    protected JFrame externalTreeView;

    /** The 2d view */
    protected View viewEditor;

    /** The 3d view */
    protected View view3d;

    /** The simulation viewer */
    protected StandaloneX3DViewer sv3d;

    /** The PropertyEditor to use */
    protected PropertyEditorFactory PropertyEditorFactory;

    /** The ToolBarFactory to use */
    protected ToolBarFactory toolbarFactory;

    /** The ViewFactory to use */
    protected ViewFactory viewFactory;

    protected DefaultPropertyEditor propertyEditor;

    /** The Command Controller */
    protected CommandController controller;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;
    
    protected Entity startEntity;

    private int editorType;

    /**
     * Construct the basic outline of an example applications.
     */
    protected BaseExample() {

        //editorType =  ViewFactory.TOP_2D_VIEW;
        editorType =  ViewFactory.TOP_3D_VIEW;

        // the main frame to attach everything too
        mainFrame = new JFrame("ChefX3D");

        // Create the default error reporter
        errorReporter = DefaultErrorReporter.getDefaultReporter();
        errorReporter.showLevel(ErrorReporter.DEBUG);

        // create the command controller
        switch (editorType) {
            case ViewFactory.TOP_2D_VIEW:
                controller = new DefaultCommandController();
                break;
            case ViewFactory.TOP_3D_VIEW:
            default:

                controller = new ValidatingBufferedCommandController();
                controller.setErrorReporter(errorReporter);

                break;
        }
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


        // try to retrieve from the classpath
        FileLoader fileLookup = new FileLoader();
        Object[] file = fileLookup.getFileURL("InitialWorld.x3dv");
        URL initialWorldURL = (URL)file[0];


       
        
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(ViewFactory.PARAM_INITIAL_WORLD, initialWorldURL.toExternalForm());
        params.put(ViewFactory.PARAM_IMAGES_DIRECTORY, ipath);
        params.put(ViewFactory.PARAM_PICKING_TYPE, "primitive");
        params.put(ViewFactory.PARAM_SHOW_RULER, "false");
        params.put(ViewFactory.PARAM_COMMAND_CONTROLLER, controller);
        params.put(ViewFactory.PARAM_BACKGROUND_COLOR, new float[]{0,0,0});
       
        // Create the ViewFactory for creating specific model views
        viewFactory = new AWTViewFactory();
        viewFactory.setErrorReporter(errorReporter);

        // Create the AV3d view
        viewEditor = viewFactory.createView(
                    model,
                    editorType,
                    params);
        viewEditor.setErrorReporter(errorReporter);

        // Create the 3d view
//        view3d = viewFactory.createView(
//                    model,
//                    ViewFactory.PERSPECTIVE_X3D_VIEW,
//                    params);
//        view3d.setErrorReporter(errorReporter);
//        if (view3d instanceof ViewX3D) {
//            ((ViewX3D)view3d).addThumbnailGenListener(this);
//        }

        // Setup the mainFrame and the contentPane for it
        Container contentPane = mainFrame.getContentPane();

        BorderLayout layout = new BorderLayout();
        contentPane.setLayout(layout);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Now define the external viewer
        externalViewer = new JFrame("ChefX3D - Perspective View");
        externalViewer.setSize(800, 600);
        Container cpViewer = externalViewer.getContentPane();
//        cpViewer.add((JComponent) view3d.getComponent(), BorderLayout.CENTER);
//        externalViewer.setVisible(true);

        // Add the menuBar to the mainFrame
        mainFrame.setJMenuBar(createMenus());

        // create the 2D viewer
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.add((JComponent) viewEditor.getComponent(), BorderLayout.CENTER);

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
        if(editorType == ViewFactory.TOP_3D_VIEW){
        	startEntity = null;
        	createRootEntity();
        }
        

        mainFrame.setSize(1200, 700);
        mainFrame.setLocation(10, 10);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    /**
     * Opens a web page in a default web browser.
     *
     * <p>
     *
     * Since we're running our code on Java 5, we don't have a direct way to access launch a
     * default web browser from Java application.  In Java 6, however, by using Desktop class
     * you can get an access to a default web browser.
     *
     * The source of this code is pulled from:
     * <a href="http://www.centerkey.com/java/browser/">http://www.centerkey.com/java/browser/</a>
     *
     * @param url URL of the web page
     */
    public static void openUrl(final String url) {

        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {

                    String osName = System.getProperty("os.name");
                    try {

                        if (osName.startsWith("Mac OS")) {

                            Class fileMgr = Class.forName("com.apple.eio.FileManager");

                            Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});

                            openURL.invoke(null, new Object[] {url});
                        }
                        else if (osName.startsWith("Windows")) {

                            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                        }
                        else { //assume Unix or Linux

                            String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };

                            String browser = null;

                            for (int count = 0; count < browsers.length && browser == null; count++)
                            {
                                if (Runtime.getRuntime().exec( new String[] {"which", browsers[count]}).waitFor() == 0) {
                                    browser = browsers[count];
                                }
                            }

                            if (browser == null) {
                                throw new Exception("Could not find web browser");
                            }
                            else {
                                Runtime.getRuntime().exec(new String[] {browser, url});
                            }
                        }
                    }
                    catch (Exception e) {

                        JOptionPane.showMessageDialog(null, "Error attempting to launch web browser" + ":\n" + e.getLocalizedMessage());
                        return false;

                    }

                    return true;

                }

            }

        );

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

        // Define the Save Action
        saveX3D = new JMenuItem("Save X3D");
        saveX3D.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.CTRL_MASK));
        saveX3D.setMnemonic(KeyEvent.VK_S);
        saveX3D.addActionListener(this);
        file.add(saveX3D);

        saveThumbnail = new JMenuItem("Save thumbnails");
        saveThumbnail.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
                ActionEvent.CTRL_MASK));
        saveThumbnail.setMnemonic(KeyEvent.VK_T);
        saveThumbnail.addActionListener(this);
        file.add(saveThumbnail);

        // Add a separator
        file.addSeparator();

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

        // Define the Menu Actions
//        launchX3D = new JMenuItem("Launch X3D");
//        launchX3D.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
//                ActionEvent.CTRL_MASK));
//        launchX3D.setMnemonic(KeyEvent.VK_L);
//        launchX3D.addActionListener(this);
//        view.add(launchX3D);

        elevationControl = new JCheckBoxMenuItem("Dynamic Elevation Control");
        elevationControl.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
                ActionEvent.CTRL_MASK));
        elevationControl.setMnemonic(KeyEvent.VK_H);
        elevationControl.addActionListener(this);
        view.addMenuListener(this);
        view.add(elevationControl);

        view.addSeparator();

        // Define the ViewTree Action
        ViewTreeAction viewTreeAction = new ViewTreeAction(model);
        view.add(viewTreeAction);


        // Define the MenuGroup
        JMenu plane = new JMenu("Plane");
        mb.add(plane);

        editTop = new JCheckBoxMenuItem("Top", true);
        editTop.addActionListener(this);
        plane.add(editTop);

        editLeft = new JCheckBoxMenuItem("Left");
        editLeft.addActionListener(this);
        plane.add(editLeft);

        editRight = new JCheckBoxMenuItem("Right");
        editRight.addActionListener(this);
        plane.add(editRight);

        editFront = new JCheckBoxMenuItem("Front");
        editFront.addActionListener(this);
        plane.add(editFront);



        // Define the ViewReport Action
        //ViewCostReportAction viewCostReportAction = new ViewCostReportAction(model);
        //view.add(viewCostReportAction);

        // Get the list of menu items
        //JCheckBoxMenuItem[] menuItems =
        //    ((MultiTabPropertyEditor) propertyEditor).getMenuItems();

        // add them to the menu
        //for (int i = 0; i < menuItems.length; i++) {
        //    view.add(menuItems[i]);
        //}

        return mb;

    }

    // ----------------------------------------------------------
    // Methods required by ActionListener
    // ----------------------------------------------------------
    public void actionPerformed(ActionEvent e) {

        Object action = e.getSource();

        if (action == saveX3D) {
            X3DExporter exporter = new X3DExporter("3.1", "Immersive", null,
                    null);
            try {
                FileWriter writer = new FileWriter(BASENAME + ".x3d");
                exporter.export(model, writer);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else if(action == saveThumbnail) {

            if (view3d instanceof ViewX3D) {
                if(((ViewX3D)view3d).isBrowserInitalized()) {
                    ((ViewX3D)view3d).createThumbnailImages();

                    try {
                        Thread.sleep(500);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }

                } else {
                    JOptionPane.showMessageDialog(mainFrame,
                            "You cannot save a thumbnail image until browser has been initalized.",
                            "Save thumbnail action",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

         } else if (action == launchX3D) {
            // Close the Perspective View
            if (externalViewer != null) {
                externalViewer.setVisible(false);
                externalViewer = null;
            }

            //System.out.println("Saving X3D");
            X3DExporter exporter = new X3DExporter("3.1", "Immersive", null,
                    null);

            try {
                FileWriter writer = new FileWriter(BASENAME + ".x3d");
                exporter.export(model, writer);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            if (sv3d != null) {
                sv3d.load(BASENAME + ".x3d");
            } else {
                sv3d = new StandaloneX3DViewer();
                sv3d.load(BASENAME + ".x3d");
            }

        } else if (action == elevationControl) {
            if((view3d != null) && (view3d instanceof ViewConfig)) {
                ViewConfig vc = (ViewConfig)view3d;
                vc.setConfigElevation(elevationControl.isSelected());
            }
        } else if (action instanceof JCheckBoxMenuItem){

            if (viewEditor instanceof GT2DView) {
                if (action == editTop) {
                    ((GT2DView)viewEditor).setCurrentPlane(ViewingFrustum.Plane.TOP);
                    editTop.setSelected(true);
                } else
                    editTop.setSelected(false);

                if (action == editLeft) {
                    ((GT2DView)viewEditor).setCurrentPlane(ViewingFrustum.Plane.LEFT);
                    editLeft.setSelected(true);
                } else
                    editLeft.setSelected(false);

                if (action == editRight) {
                    ((GT2DView)viewEditor).setCurrentPlane(ViewingFrustum.Plane.RIGHT);
                    editRight.setSelected(true);
                } else
                    editRight.setSelected(false);

                if (action == editFront) {
                    ((GT2DView)viewEditor).setCurrentPlane(ViewingFrustum.Plane.FRONT);
                    editFront.setSelected(true);
                } else
                    editFront.setSelected(false);

            }

         }

    }

    //----------------------------------------------------------
    // Methods required by the MenuListener interface
    //----------------------------------------------------------

    /**
     * Ignored. Invoked when the menu is canceled.
     *
     * @param evt The event that caused this method to be called.
     */
    public void menuCanceled(MenuEvent evt) {
    }

    /**
     * Invoked when the menu is deselected. Enable the action when
     * the parent menu is not visible to support key bound events.
     *
     * @param evt The event that caused this method to be called.
     */
    public void menuDeselected(MenuEvent evt) {
        elevationControl.setEnabled(true);
    }

    /**
     * Invoked when a menu is selected. Enable this item if an
     * Entity is available in the copy buffer.
     *
     * @param evt The event that caused this method to be called.
     */
    public void menuSelected(MenuEvent evt) {
        if((view3d != null) && (view3d instanceof ViewConfig)) {
            elevationControl.setEnabled(true);
            ViewConfig vc = (ViewConfig)view3d;
            elevationControl.setSelected(vc.getConfigElevation());
        } else {
            elevationControl.setEnabled(false);
        }
    }

    //----------------------------------------------------------
    // Methods required by the ThumbnailListener
    //----------------------------------------------------------

    /**
     * Notifies the listener that thumbnail generation has been completed.
     * @param thumnails Lists of thumbnail files names.
     */
    public void thumbnailCreated(ArrayList<String> thumnails) {

        /*BufferedReader br = new BufferedReader();

        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument)htmlKit.createDefaultDocument();

        HTMLEditorKit.HTMLTextAction
        HTMLEditorKit.Parser parser = new ParserDelegator();
        HTMLEditorKit.ParserCallback callback = htmlDoc.getReader(0);
        parser.parse(br, callback, true);

        HTMLDocument.Iterator i;
        i.*/
        for(int i = 0; i < thumnails.size(); i++) {
            openUrl(thumnails.get(i));
        }
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Create ChefX3D tools.
     */
    protected abstract void createTools();
    
    /** Create ChefX3D Root entity*/
    private void createRootEntity(){
    	//System.out.println("createTools");
        // set to true if you want to use the DrillDownToolBar
        // otherwise set to false
    
        Tool tool;
        

       
        String[] interfaceIcons =
            new String[] {
                ipath + "Grid16x16.png",
                ipath + "Grid32x32.png",
                ipath + "Grid64x64.png"};

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        EntityBuilder builder =  DefaultEntityBuilder.getEntityBuilder();
		tool = new Tool(
                "Root Entity",
                ViewingFrustum.Plane.TOP.toString(),
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

        int entityID = model.issueEntityID();
        double[] position = new double[]{0,0,0};
        startEntity =  builder.createEntity(model, entityID,
				position, new float[] {0,1,0,0}, tool);
        
        
        AddEntityCommand cmd = new AddEntityCommand(model, startEntity);
        model.applyCommand(cmd);
        
       
  
         
        
        
      
	
    
    }
}
