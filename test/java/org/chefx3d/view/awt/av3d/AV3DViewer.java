/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2009
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there"s a problem you get to fix it.
 *
 ****************************************************************************/

// External imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;

import javax.media.opengl.GLCapabilities;

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.*;

import org.j3d.aviatrix3d.management.SingleThreadRenderManager;
import org.j3d.aviatrix3d.management.SingleDisplayCollection;

import org.j3d.aviatrix3d.output.graphics.SimpleAWTSurface;

import org.j3d.aviatrix3d.pipeline.graphics.DefaultGraphicsPipeline;
import org.j3d.aviatrix3d.pipeline.graphics.FrustumCullStage;
import org.j3d.aviatrix3d.pipeline.graphics.GraphicsCullStage;
import org.j3d.aviatrix3d.pipeline.graphics.GraphicsOutputDevice;
import org.j3d.aviatrix3d.pipeline.graphics.GraphicsSortStage;
import org.j3d.aviatrix3d.pipeline.graphics.StateAndTransparencyDepthSortStage;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.view.awt.av3d.AV3DLoader;

/**
 * Simple viewer for loading and displaying X3D models imported 
 * with the AV3DLoader.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public class AV3DViewer extends JFrame implements WindowListener, ActionListener,
	ApplicationUpdateObserver, NodeUpdateListener {
	
    /** Manager for the scene graph handling */
    private SingleThreadRenderManager sceneManager;

    /** Manager for the layers etc */
    private SingleDisplayCollection displayManager;

    /** Our drawing surface */
    private GraphicsOutputDevice surface;
	
    /** The content loader */
    private AV3DLoader loader;
	
	/** The root group of the scene */
	private Group rootGroup;
	
	/** The content group of the scene */
	private Group contentGroup;
	
	/** The content */
	private Node[] nodes;
	
	/** Flag indicating that content has been loaded and is ready for display */
	private boolean contentIsAvailable;
	
    /** UI control for opening the open file chooser dialog */
    private JMenuItem openItem;
    
    /** file chooser for picking an x3d world to open */
    private JFileChooser chooser;
    
    /** The file selected by the chooser */
    private File file;
    
    public AV3DViewer() {
        super("AV3DViewer");

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		
        JMenuBar mb = new JMenuBar();
        this.setJMenuBar( mb );
        
        JMenu fileMenu = new JMenu("File");
        mb.add(fileMenu);
        
        openItem = new JMenuItem("Open");
        openItem.addActionListener(this);
        fileMenu.add(openItem);
        
        File dir = new File(System.getProperty("user.dir"));
        chooser = new JFileChooser(dir);
        
        addWindowListener(this);

		loader = new AV3DLoader();
		
        setupAviatrix(contentPane);
        setupSceneGraph();
    }

    //---------------------------------------------------------------
    // Methods defined by ActionListener
    //---------------------------------------------------------------

    /**
     * UI event handler
     */
    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource();
        if (source == openItem) {
            int returnVal = chooser.showDialog(this, "Open");
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                if (file != null) {
					nodes = loader.load(file);
					if (nodes != null) {
						contentIsAvailable = true;
					}
                }
            }
        }
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
        sceneManager.shutdown();
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
        sceneManager.setEnabled(true);
    }

    //---------------------------------------------------------------
    // Methods defined by ApplicationUpdateObserver
    //---------------------------------------------------------------

    /**
     * Notification that now is a good time to update the scene graph.
     */
    public void updateSceneGraph() {
		if(contentIsAvailable) {
			if (rootGroup.isLive()){            	
				rootGroup.boundsChanged(this);
			} else {
				updateNodeBoundsChanges(rootGroup);
			}
		}
    }

    /**
     * Notification that the AV3D internal shutdown handler has detected a
     * system-wide shutdown. The aviatrix code has already terminated rendering
     * at the point this method is called, only the user's system code needs to
     * terminate before exiting here.
     */
    public void appShutdown() {
        // do nothing
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
			if (nodes != null) {
				if (contentGroup != null) {
					rootGroup.removeChild(contentGroup);
				}
				contentGroup = new Group();
				for ( int i = 0; i < nodes.length; i++ ) {
					contentGroup.addChild(nodes[i]);
				}
				rootGroup.addChild(contentGroup);
			}
			contentIsAvailable = false;
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
    // Local methods
    //---------------------------------------------------------------

    /**
     * Setup aviatrix
     */
    private void setupAviatrix(Container contentPane) {
        
		// Assemble a simple single-threaded pipeline.
		GLCapabilities caps = new GLCapabilities();
		caps.setDoubleBuffered(true);
		caps.setHardwareAccelerated(true);
		
		GraphicsCullStage culler = new FrustumCullStage();
		culler.setOffscreenCheckEnabled(true);
		
		GraphicsSortStage sorter = new StateAndTransparencyDepthSortStage();
		
        surface = new SimpleAWTSurface(caps);
        DefaultGraphicsPipeline pipeline = new DefaultGraphicsPipeline();

        pipeline.setCuller(culler);
        pipeline.setSorter(sorter);
        pipeline.setGraphicsOutputDevice(surface);

        displayManager = new SingleDisplayCollection();
        displayManager.addPipeline(pipeline);

        sceneManager = new SingleThreadRenderManager();
        sceneManager.addDisplay(displayManager);
        sceneManager.setMinimumFrameInterval(100);

        // Before putting the pipeline into run mode, put the canvas on
        // screen first.
        Component comp = (Component)surface.getSurfaceObject();
        contentPane.add(comp, BorderLayout.CENTER);
		
		sceneManager.setApplicationObserver(this);
    }

    /**
     * Initialize the scenegraph
     */
    private void setupSceneGraph() {
		
        Viewpoint vp = new Viewpoint();

        Vector3f trans = new Vector3f(0, 0, 10);

        Matrix4f mat = new Matrix4f();
        mat.setIdentity();
        mat.setTranslation(trans);

        TransformGroup tx = new TransformGroup();
        tx.addChild(vp);
        tx.setTransform(mat);

        rootGroup = new Group();
        rootGroup.addChild(tx);

        I18nManager intl_mgr = I18nManager.getManager();
        intl_mgr.setApplication("Bogus",
                                "config.i18n.chefx3dResources");
        
        SimpleScene scene = new SimpleScene();
        scene.setRenderedGeometry(rootGroup);
        scene.setActiveView(vp);

        // Then the basic layer and viewport at the top:
        SimpleViewport view = new SimpleViewport();
        view.setDimensions(0, 0, 600, 600);
        view.setScene(scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(view);

        Layer[] layers = { layer };
        displayManager.setLayers(layers, 1);
    }

    public static void main(String[] args) {
		
        AV3DViewer viewer = new AV3DViewer();
        viewer.setSize(600, 600);
        viewer.setDefaultCloseOperation( EXIT_ON_CLOSE );
        Dimension screenSize = viewer.getToolkit( ).getScreenSize( );
        Dimension frameSize = viewer.getSize( );
        viewer.setLocation( 
            ( screenSize.width - frameSize.width )/2, 
            ( screenSize.height - frameSize.height )/2 );

        // Need to set visible first before starting the rendering thread due
        // to a bug in JOGL. See JOGL Issue #54 for more information on this.
        // http://jogl.dev.java.net
        viewer.setVisible(true);
    }
}
