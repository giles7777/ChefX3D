/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

// External imports

package org.chefx3d.catalog.util;

// External Imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import java.text.NumberFormat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.imageio.ImageIO;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.j3d.aviatrix3d.BoundingBox;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Layer;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.SimpleLayer;
import org.j3d.aviatrix3d.SimpleScene;
import org.j3d.aviatrix3d.SimpleViewport;

import org.j3d.aviatrix3d.management.DisplayCollection;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

import org.web3d.browser.BrowserComponent;
import org.web3d.browser.BrowserCore;
import org.web3d.browser.BrowserCoreListener;

import org.web3d.util.MathUtils;

import org.web3d.vrml.nodes.VRMLScene;

import org.web3d.vrml.renderer.ogl.browser.OGLStandardBrowserCore;
import org.web3d.browser.ScreenCaptureListener;

import org.web3d.x3d.sai.BrowserFactory;
import org.web3d.x3d.sai.ComponentInfo;
import org.web3d.x3d.sai.ExternalBrowser;
import org.web3d.x3d.sai.ProfileInfo;
import org.web3d.x3d.sai.SFNode;
import org.web3d.x3d.sai.SFVec3f;
import org.web3d.x3d.sai.X3DComponent;
import org.web3d.x3d.sai.X3DFieldEvent;
import org.web3d.x3d.sai.X3DFieldEventListener;
import org.web3d.x3d.sai.X3DMetadataObject;
import org.web3d.x3d.sai.X3DNode;
import org.web3d.x3d.sai.X3DScene;

import org.web3d.x3d.sai.core.MetadataSet;
import org.web3d.x3d.sai.environmentaleffects.Background;
import org.web3d.x3d.sai.grouping.Transform;
import org.web3d.x3d.sai.grouping.WorldInfo;

import org.web3d.x3d.sai.navigation.OrthoViewpoint;

import org.xj3d.core.loading.ContentLoadManager;

import org.xj3d.ui.awt.browser.ogl.X3DBrowserJPanel;
import org.xj3d.impl.core.loading.FramerateThrottle;

// Local imports
// none

/**
 * Utility for creating icon images from X3D objects
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public class X3DIconStyler implements 
	ScreenCaptureListener, ActionListener, X3DFieldEventListener, BrowserCoreListener, Runnable {
	
	// file:/C:/www.web3d.org/x3d/content/examples/SavageDefense/ShipsMilitary/PearlHarborSecurityBoat
	
	/** Usage message with command line options */
	private static final String USAGE =
		"Usage: X3DIconStyler [options] [directory] \n" +
		"  -help                   Print out this messaage to the stdout\n" +
		"  -batch                  Specify that this should run in batch mode\n" +
		"  -recurse                Specify that this should recurse throught the sub-directories of the \n"+
		"                          source directory \n" +
		"  -nosmal                 Specify that the loaded scenes are not required to be SMAL\n" +
		"  -ext [name]             The extension of the X3D file type to capture " +
		"[x3d|x3dv|wrl] \n" +
		"  -outdir [dirname]       The output directory name relative to the input directory to save the icons in \n" +
		"  -scale n                The scale of the capture area in meters per pixel\n" +
		"  -w n               	   The width of the icon in pixels\n" +
		"  -h n       		 	   The height of the icon in pixels\n";
	
	/** the absolute origin */
	private static final float[] ORIGIN = { 0.0f, 0.0f, 0.0f };
	
	/** Initial browser component dimension */
	private static final Dimension DEFAULT_BROWSER_DIMENSION = new Dimension( 512, 512 );
	
	/** The default icon size */
	private static final int DEFAULT_ICON_WIDTH = 64;
	private static final int DEFAULT_ICON_HEIGHT = 64;
	
	/** The default icon dimension */
	private static final Dimension DEFAULT_SNAPSHOT_DIMENSION = 
		new Dimension( DEFAULT_ICON_WIDTH, DEFAULT_ICON_HEIGHT );
	
	/** The number of browser rendering frames to wait between configuring the 
	*  viewpoint and initiating the snapshot */
	private static final int DEFAULT_FRAME_DELAY_TILL_SNAP = 10;
	
	/** The default destination for each icon */
	//private static final String DEFAULT_DESTINATION_DIRECTORY = "icons";
    private static final String DEFAULT_DESTINATION_DIRECTORY = "C:/yumetech/cygwin/home/Russell/projects/SavageStudio/build/images/new";

    /** The default source directory */
    private static final String DEFAULT_SOURCE_DIRECTORY = "C:/www.web3d.org/x3d/content/examples/Savage";

	/** The default x3d file type to use */
	private static final String DEFAULT_EXTENSION = "x3d";
	
	/** The default SMAL flag */
	private static final boolean DEFAULT_SMAL = true;
	
	/** The SMAL ID String */
	private static final String SMAL = "SMAL";
	
	/** The default scale for each icon */
	private static final float DEFAULT_ICON_SCALE = 1.0f;
	
	/** The background color in the captured image to make transparent in the icon */
	public static final int TRANSPARENT_COLOR = 0x00FF00;
	
	/** The dimension to use for each snapshot */
	private Dimension snapDimension;
	
	/** The application frame when run in interactive mode */
	private JFrame frame;
	
	/** The X3D browser UI Component */
	private X3DComponent x3dComponent;
	
	/** The X3D browser object */
	private ExternalBrowser browser;
	
	/** The panel containing the browser component - used to enforce the snapshot Dimension */
	private JPanel browserPanel;
	
	/** The external frame component containing the browser */
	private JFrame browserFrame;
	
	/** UI control to initiate the snapshot capture */
	private JButton captureButton;
	
	/** UI control to terminate snapshot capture */
	private JButton stopButton;
	
	/** Flag indicating that snapshot capture should be interrupted */
	private boolean terminate;
	
	/** UI display of the 3D world source */
	private JTextField worldSourceField;
	
	/** UI display of the 3D world bounds */
	private JTextField boundsField;
	
	/** UI control to refresh the world bounds */
	private JButton boundsButton;
	
	/** UI display for the base icon image output directory */
	private JTextField iconBaseDirField;
	
	/** UI display for the icon image output sub-directory */
	private JTextField iconDirField;
	
	/** UI control for the horizontal icon dimension */
	private JFormattedTextField hrzIconDimField;
	
	/** UI control for the vertical icon dimension */
	private JFormattedTextField vrtIconDimField;
	
	/** UI control for the icon scale value */
	private JFormattedTextField iconScaleField;
	
	/** UI control for opening the open file chooser dialog */
	private JMenuItem openItem;
	
	/** file chooser for picking an x3d world to open */
	private JFileChooser chooser;
	
	/** UI control for opening the save file chooser dialog */
	private JMenuItem saveItem;
	
	/** file chooser for selecting a directory to save into */
	private JFileChooser saveChooser;
	
	/** Timer used to trigger a recheck of the world bounds */
	private Timer timer;
	
	/** The render manager */
	private OGLStandardBrowserCore core;
	
	/** The content load manager */
	//private ContentLoadManager loadManager;
	private FramerateThrottle frameThrottle;
	
	/** The rendering surface */
	private Component canvas;
	
	/** The display collection */
	private DisplayCollection displayManager;
	
	/** The current scene */
	private X3DScene scene;
	
	/** The viewpoint used to take the scene snapshots */
	private OrthoViewpoint ortho_view;
	
	/** A 'dummy' node used to stimulate an event from the browser at the
	 *  end of an rendering cycle. The event is used to signal that the scene
	 *  has rendered following a viewpoint change and is ready for it's photo op. */
	private Transform tickle;
	
	/** The field of the tickle node that is used */
	private SFVec3f tickle_translation;
	
	/** flag indicating that the browser initialized event has been received */
	private boolean browserReady;
	
	/** sync variable, indicating that the next snapshot is 'good to go' */
	private boolean readyToCapture;
	
	/** flag indicating that a capture cycle has completed */
	private boolean captureComplete;
	
	/** sync variable, indicating that the rendering of the scene has completed */
	private boolean sceneHasRendered;
	
	/** the number of frames to render before setting the sceneHasRendered flag true */
	private int frameCount;
	
	/** the current world bounds */
	private BoundingBox worldBounds;
	
	/** the calculated fieldOfView parameters for the OrthoViewpoint for an
	*   icon that are used while taking the snapshots */
	private float[] field;
	
	/** Base icon filename, without the extension */
	private String iconName;
	
	///////////////////////////////////////////////////////////////////////////
	// parameters derived from the command line 
	
	/** Flag indicating that this is running in batch mode
	*  or interactive mode */
	private static boolean batchMode;
	
	/** The current working source directory */
	private static File srcDir;
	
	/** The name of the sub-directory within the source directory
	*  in which to place icon image files */
	private static String iconDirName;
	
	/** The directory within which to place icon image files */
	private static File iconDir;
	
	/** The x3d extension to use for identifying the worlds to capture */
	private static String x3d_ext;
	
	/** Flag indicating that the scene must be SMAL in order to be captured */
	private static boolean smalMode = DEFAULT_SMAL;
	
	/** Flag indicating whether to recurse from the source directory for x3d files to capture */
	private static boolean recurse;
	
	/** scale factor being used in capture */
	private static float iconScale;
	
	/** icon width to be captured */
	private static int iconWidth;
	
	/** icon height to be captured */
	private static int iconHeight;
	
	///////////////////////////////////////////////////////////////////////////
	
	/** The filter for x3d files with the desired extension */
	private X3DFileFilter x3d_file_filter;
	
	/**
	 * Constructor
	 */
	public X3DIconStyler( ) {
		
        srcDir = new File(DEFAULT_SOURCE_DIRECTORY);
        
		System.setProperty( "x3d.sai.factory.class", 
			"org.xj3d.ui.awt.browser.ogl.X3DOGLBrowserFactoryImpl" );
		
		System.setProperty("org.xj3d.core.loading.threads", "4");
		
		snapDimension = DEFAULT_SNAPSHOT_DIMENSION;
		////////////////////////////////////////////////////////////////////////////////////
		// setup the x3d browser in an external frame
		
		createBrowser( );
		createBrowserFrame( DEFAULT_BROWSER_DIMENSION, true );
		
		////////////////////////////////////////////////////////////////////////////////////
		
		//x3d_file_filter = new X3DFileFilter( );
		//dir_filter = new DirectoryFilter( );

		x3d_file_filter = new X3DFileFilter();
		
		if ( !batchMode ) {
			
			frame = new JFrame( "X3D Icon Styler" );
			
			// configure sizing parameters for creating the snapshot
			GraphicsEnvironment env =
				GraphicsEnvironment.getLocalGraphicsEnvironment( );
			GraphicsDevice device = env.getDefaultScreenDevice();
			DisplayMode mode = device.getDisplayMode( );
			int screenWidth = mode.getWidth( );
			int screenHeight = mode.getHeight( );
			int maxSnapAxisLength = Math.min( screenWidth, screenHeight );
			
			Container contentPane = frame.getContentPane( );
			contentPane.setLayout( new BorderLayout( ) );
			
			JPanel dataPanel = new JPanel( new GridBagLayout( ) );
			GridBagConstraints c = new GridBagConstraints( );
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1; c.weighty = 0;
			c.insets = new Insets( 2, 2, 2, 2 );
			
			TitledBorder border = new TitledBorder( new EtchedBorder( ) );
			border.setTitle( "Parameters" );
			dataPanel.setBorder( border );
			
			JPanel worldPanel  = new JPanel( new GridLayout( 2, 1, 2, 2 ) );
			TitledBorder worldBorder = new TitledBorder( new EtchedBorder( ) );
			worldBorder.setTitle( "World" );
			worldPanel.setBorder( worldBorder );
			
			JPanel worldSourcePanel  = new JPanel( new BorderLayout( ) );
			TitledBorder worldSourceBorder = new TitledBorder( new EtchedBorder( ) );
			worldSourceBorder.setTitle( "World Source" );
			worldSourcePanel.setBorder( worldSourceBorder );
			
			worldSourceField = new JTextField( 40 );
			worldSourceField.setEditable( false );
			worldSourceField.setHorizontalAlignment( SwingConstants.CENTER );
			worldSourcePanel.add( worldSourceField, BorderLayout.CENTER );
			
			worldPanel.add( worldSourcePanel );
			
			JPanel boundsPanel = new JPanel( new BorderLayout( ) );
			TitledBorder boundsBorder = new TitledBorder( new EtchedBorder( ) );
			boundsBorder.setTitle( "World Bounds" );
			boundsPanel.setBorder( boundsBorder );
			
			boundsField = new JTextField( );
			boundsField.setEditable( false );
			boundsField.setHorizontalAlignment( SwingConstants.CENTER );
			boundsPanel.add( boundsField, BorderLayout.CENTER );
			
			worldPanel.add( boundsPanel );
			
			c.gridx = 0; c.gridy = 0;
			c.weightx = 1; c.weighty = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			dataPanel.add( worldPanel, c );
			
			JPanel iconPanel = new JPanel( new GridBagLayout( ) );
			GridBagConstraints cIcon = new GridBagConstraints( );
			cIcon.fill = GridBagConstraints.HORIZONTAL;
			cIcon.weightx = 1; cIcon.weighty = 0;
			cIcon.insets = new Insets( 2, 2, 2, 2 );
			cIcon.gridwidth = GridBagConstraints.REMAINDER;
			
			TitledBorder iconBorder = new TitledBorder( new EtchedBorder( ) );
			iconBorder.setTitle( "Icon" );
			iconPanel.setBorder( iconBorder );
			
			JPanel iconOutputPanel = new JPanel( new GridLayout( 2, 1, 2, 2 ) );
			TitledBorder iconOutputBorder = new TitledBorder( new EtchedBorder( ) );
			iconOutputBorder.setTitle( "Output" );
			iconOutputPanel.setBorder( iconOutputBorder );
			
			JPanel iconBaseDirPanel = new JPanel( new BorderLayout( ) );
			TitledBorder iconBaseDirBorder = new TitledBorder( new EtchedBorder( ) );
			iconBaseDirBorder.setTitle( "Directory" );
			iconBaseDirPanel.setBorder( iconBaseDirBorder );
			
			iconBaseDirField = new JTextField( srcDir.getPath( ) );
			iconBaseDirField.setEditable( false );
			iconBaseDirField.setHorizontalAlignment( SwingConstants.LEFT );
			iconBaseDirPanel.add( iconBaseDirField, BorderLayout.CENTER );
			
			iconOutputPanel.add( iconBaseDirPanel );
			
			JPanel iconDirPanel = new JPanel( new BorderLayout( ) );
			TitledBorder iconDirBorder = new TitledBorder( new EtchedBorder( ) );
			iconDirBorder.setTitle( "Directory" );
			iconDirPanel.setBorder( iconDirBorder );
			
			iconDirField = new JTextField( iconDirName );
			iconDirField.setEditable( true );
			iconDirField.setHorizontalAlignment( SwingConstants.CENTER );
			iconDirPanel.add( iconDirField, BorderLayout.CENTER );
			
			iconOutputPanel.add( iconDirPanel );
			
			cIcon.gridx = 0; cIcon.gridy = 0;
			iconPanel.add( iconOutputPanel, cIcon );
			
			//////////////////////////////////////////////////////////////////////////
			JPanel iconSizePanel = new JPanel( new GridLayout( 1, 4, 2, 2 ) );
			TitledBorder iconSizeBorder = new TitledBorder( new EtchedBorder( ) );
			iconSizeBorder.setTitle( "Icon Size (Pixels)" );
			iconSizePanel.setBorder( iconSizeBorder );
			
			NumberFormat fmt = NumberFormat.getIntegerInstance( );
			
			JLabel hrzIconDimLabel = new JLabel( "X: " );
			hrzIconDimLabel.setHorizontalAlignment( SwingConstants.RIGHT );
			iconSizePanel.add( hrzIconDimLabel );
			
			hrzIconDimField = new JFormattedTextField( fmt );
			hrzIconDimField.setHorizontalAlignment( SwingConstants.CENTER );
			hrzIconDimField.setValue( new Integer( DEFAULT_SNAPSHOT_DIMENSION.width ) );
			iconSizePanel.add( hrzIconDimField );
			
			JLabel vrtIconDimLabel = new JLabel( "Z: " );
			vrtIconDimLabel.setHorizontalAlignment( SwingConstants.RIGHT );
			iconSizePanel.add( vrtIconDimLabel );
			
			vrtIconDimField = new JFormattedTextField( fmt );
			vrtIconDimField.setHorizontalAlignment( SwingConstants.CENTER );
			vrtIconDimField.setValue( new Integer( DEFAULT_SNAPSHOT_DIMENSION.height ) );
			iconSizePanel.add( vrtIconDimField );
			
			cIcon.gridx = 0; cIcon.gridy = 1;
			iconPanel.add( iconSizePanel, cIcon );
			
			JPanel iconScalePanel = new JPanel( new GridLayout( 1, 1, 2, 2 ) );
			TitledBorder iconScaleBorder = new TitledBorder( new EtchedBorder( ) );
			iconScaleBorder.setTitle( "Icon Scale (Meters per Pixel)" );
			iconScalePanel.setBorder( iconScaleBorder );
			
			NumberFormat d_fmt = NumberFormat.getNumberInstance( );
			
			iconScaleField = new JFormattedTextField( d_fmt );
			iconScaleField.setHorizontalAlignment( SwingConstants.CENTER );
			iconScaleField.setValue( new Float( DEFAULT_ICON_SCALE ) );
			iconScalePanel.add( iconScaleField );
			
			cIcon.gridx = 0; cIcon.gridy = 2;
			cIcon.weightx = 0.5; cIcon.weighty = 0;
			cIcon.gridwidth = 1;
			//iconPanel.add( iconScalePanel, cIcon );
			
			c.gridx = 0; c.gridy = 1;
			c.weightx = 1; c.weighty = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			dataPanel.add( iconPanel, c );
			
			contentPane.add( dataPanel, BorderLayout.CENTER );
			
			JPanel controlPanel = new JPanel( new GridLayout( 1, 1, 1, 1 ) );
			contentPane.add( controlPanel, BorderLayout.SOUTH );
			
			captureButton = new JButton( "Capture" );
			captureButton.setEnabled( false );
			captureButton.addActionListener( this );
			controlPanel.add( captureButton );
			
			stopButton = new JButton( "Stop" );
			stopButton.setEnabled( false );
			stopButton.addActionListener( this );
			//controlPanel.add( stopButton );
			
			JPopupMenu.setDefaultLightWeightPopupEnabled( false );
			JMenuBar mb = new JMenuBar( );
			frame.setJMenuBar( mb );
			
			JMenu fileMenu = new JMenu( "File" );
			mb.add( fileMenu );
			
			openItem = new JMenuItem( "Open World" );
			openItem.addActionListener( this );
			fileMenu.add( openItem );
			
			chooser = new JFileChooser( srcDir );
			//FileNameExtensionFilter filter = new FileNameExtensionFilter(
			//	"X3D Files", "wrl", "x3d", "x3dv" );
			chooser.setFileFilter( x3d_file_filter );
			
			saveItem = new JMenuItem( "Select Output Directory" );
			saveItem.addActionListener( this );
			fileMenu.add( saveItem );
			
			saveChooser = new JFileChooser( iconDir );
			saveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			// this timer runs continuously and the event generated by it will
			// acquire and display the scene bounds if a scene is active. this is
			// inefficient, but far easier than trying to sort out if and when content 
			// has completed loading.....
			timer = new Timer( 2000, this );
			timer.start( );
			
			frame.pack( );
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			Dimension screenSize = frame.getToolkit( ).getScreenSize( );
			Dimension frameSize = frame.getSize( );
			frame.setLocation( 
				( screenSize.width - frameSize.width )/2, 
				( screenSize.height - frameSize.height )/2 );
			frame.setVisible( true );
		}
	}
	
	//---------------------------------------------------------
	// Method defined by X3DFieldEventListener
	//---------------------------------------------------------
	
	/**
	 * Field events handler, used to catch signal that the
	 * previous scene frame has rendered.
	 */
	public void readableFieldChanged( final X3DFieldEvent xfe ) {
		final Object source = xfe.getSource( );
		if ( source.equals( tickle_translation ) ) {
			if ( --frameCount == 0 ) {
				synchronized( this ) {
					sceneHasRendered = true;
					notify( );
				}
			} else {
				tickle.setTranslation( ORIGIN );
			}
		}
	}
	
	//---------------------------------------------------------
	// Method defined by Runnable
	//---------------------------------------------------------
	
	/**
	 * Thread to process the snapshots
	 */
	public void run( ) {
		
        if (iconDirName.startsWith("C:")) {
            System.out.println("opening: " + iconDirName);
            iconDir = new File( iconDirName );         
        } else {
            iconDir = new File( srcDir, iconDirName );
        }

		if ( iconDir.exists( ) && !iconDir.isDirectory( ) ) {
			System.out.println( "Invalid destination directory specified :"+ iconDir 
				+" is not a directory" );
			iconDir = null;
		} else if ( !iconDir.exists( ) ) {
			iconDir.mkdir( );
		}
		
		if ( iconDir != null ) {
			
			sceneHasRendered = false;
			
			Dimension canvasSize = canvas.getSize( );
			
			// generate the icon image
			try {
				// wait for the signal that the previous snapshot has been processed
				synchronized( this ) {
					if ( !readyToCapture ) {
						wait( );
					}
					readyToCapture = false;
				}
			} catch ( InterruptedException ie ) {
			}
			
			// set the number of scene rendering frames to wait until a capture
			frameCount = DEFAULT_FRAME_DELAY_TILL_SNAP;
			
			// modify the field which we listen on for an event that tells
			// us that the last rendering pass has completed.
			tickle.setTranslation( ORIGIN );
			try {
				// wait for the signal that the browser has rendered the scene
				synchronized( this ) {
					if ( !sceneHasRendered ) {
						wait( );
					}
					sceneHasRendered = false;
				}
			} catch ( InterruptedException ie ) {
			}
			
			float[] size = new float[3];
			worldBounds.getSize( size );
			
			// configure the ortho viewpoint parameters
			System.out.println( Arrays.toString( field ) );
			ortho_view.setFieldOfView( field );
			float elevation = size[1] * 2;
			if ( elevation < 10 ) {
				elevation = 10.0f;
			}
			ortho_view.setPosition( new float[]{ 0, elevation, 0 } );
			if ( !ortho_view.getIsBound( ) ) {
				// if this is the first time through, bind the viewpoint
				ortho_view.setBind( true );
			}
			
			// set the number of scene rendering frames to wait until a capture
			frameCount = DEFAULT_FRAME_DELAY_TILL_SNAP;
			
			// modify the field which we listen on for an event that tells
			// us that the last rendering pass has completed.
			tickle.setTranslation( ORIGIN );
			try {
				// wait for the signal that the browser has rendered the scene
				synchronized( this ) {
					if ( !sceneHasRendered ) {
						wait( );
					}
					sceneHasRendered = false;
				}
			} catch ( InterruptedException ie ) {
			}
			
			// start the capture
			core.captureScreenOnce( this );
			
			if ( !terminate ) {
				try {
					// wait for the signal that the previous snapshot has been processed
					// before continuing
					synchronized( this ) {
						if ( !readyToCapture ) {
							wait( );
						}
					}
				} catch ( InterruptedException ie ) {
				}
			}
			
			if ( !batchMode ) {
				// restore the browser to it's decorated frame, re-enable the ui
				EventQueue.invokeLater( new Runnable( ) {
						public void run( ) {
							captureComplete = true;
							createBrowserFrame( DEFAULT_BROWSER_DIMENSION, true );
							
							hrzIconDimField.setEditable( true );
							vrtIconDimField.setEditable( true );
							iconScaleField.setEditable( true );
							iconDirField.setEditable( true );
							
							stopButton.setEnabled( false );
						}
					} );
				
				terminate = false;
			}
		}
	}
	
	//---------------------------------------------------------
	// Method defined by ActionListener
	//---------------------------------------------------------
	
	/**
	 * UI event handlers
	 */
	public void actionPerformed( ActionEvent ae ) {
		Object source = ae.getSource( );
		if ( source == captureButton ) {
			
			stopButton.setEnabled( true );
			
			hrzIconDimField.setEditable( false );
			vrtIconDimField.setEditable( false );
			iconScaleField.setEditable( false );
			iconDirField.setEditable( false );
			
			// the output directory

			iconDirName = iconDirField.getText( );
			
			// the icon dimensions in meter
			int iconWidth = ((Number)hrzIconDimField.getValue( )).intValue( );
			int iconHeight = ((Number)vrtIconDimField.getValue( )).intValue( );
			
			// scale factor for the image icons in units of meters per pixel
			//iconScale = ((Number)iconScaleField.getValue( )).floatValue( );
			
			// create the field of view parameters for the scene ortho viewpoint
			generateSnapshotParameters( );
			
			createBrowserFrame( snapDimension, false );
			
			Thread captureThread = new Thread( this, this.getClass( ).getName( ) );
			captureThread.start( );
			
		} else if ( source == openItem ) {
			int returnVal = chooser.showDialog( frame, "Open World" );
			if( returnVal == JFileChooser.APPROVE_OPTION ) {
				File file = chooser.getSelectedFile( );
				URL url = null;
				try {
					url = file.toURI( ).toURL( );
				} catch ( MalformedURLException mue ) {
					System.out.println( mue.getMessage( ) );
				}
				if ( url != null ) {
					loadScene( url );
					String name = file.getName( );
					int dot = name.indexOf( "." );
					iconName = name.substring( 0, dot );
					srcDir = file.getParentFile( );
					iconBaseDirField.setText( srcDir.getPath( ) );
				}
			}
		} else if ( source == saveItem ) {
			int returnVal = saveChooser.showDialog( frame, "Select" );
			if( returnVal == JFileChooser.APPROVE_OPTION ) {
				iconDir = saveChooser.getSelectedFile( );
				URL url = null;
				try {
					url = iconDir.toURI( ).toURL( );
				} catch ( MalformedURLException mue ) {
				}
                iconDirField.setText(url.toExternalForm( ).replace("file:/", ""));
				//iconDirField.setText( url.toExternalForm( ) );
			}
		} else if ( source == boundsButton ) {
			updateWorldBounds( );
		} else if ( source == timer ) {
			if ( readyToCapture ) {
				updateWorldBounds( );
			}
		} else if ( source == stopButton ) {
			terminate = true;
		}
	}
	
	//----------------------------------------------------------
	// Method required for ScreenCaptureListener
	//----------------------------------------------------------
	
    /**
     * Notification of a new screen capture.  This will be in openGL pixel
     * order.
     *
     * @param buffer The screen capture
     * @param width The width in pixels of the captured screen
     * @param height The height in pixels of the captured screen
     */
    public void screenCaptured(Buffer buffer, int width, int height) {
        		
		Dimension size = canvas.getSize();
		
		saveScreen( buffer, iconName, width, height );
		
		synchronized ( this ) {
			readyToCapture = true;
			notify( );
		}
	}
	

	
	//----------------------------------------------------------
	// Methods defined by BrowserCoreListener
	//----------------------------------------------------------
	
	/**
	 * The browser has been initialised with new content. The content given
	 * is found in the accompanying scene and description.
	 *
	 * @param scene The scene of the new content
	 */
	public void browserInitialized( VRMLScene scene ) {
		//System.out.println( "browserInitialized" );
		if ( !batchMode ) {
			captureButton.setEnabled( true );
		}
		
		synchronized ( this ) {
			browserReady = true;
			notify( );
		}
	}
	
	/**
	 * The tried to load a URL and failed. It is typically because none of
	 * the URLs resolved to anything valid or there were network failures.
	 *
	 * @param msg An error message to go with the failure
	 */
	public void urlLoadFailed( String msg ) {
		if ( !batchMode ) {
			captureButton.setEnabled( false );
		}
		browserReady = false;
	}
	
	/**
	 * The browser has been shut down and the previous content is no longer
	 * valid.
	 */
	public void browserShutdown( ) {
		if ( !batchMode ) {
			captureButton.setEnabled( false );
		}
		browserReady = false;
	}
	
	/**
	 * The browser has been disposed, all resources may be freed.
	 */
	public void browserDisposed( ) {
		if ( !batchMode ) {
			captureButton.setEnabled( false );
		}
		browserReady = false;
	}
	
	//----------------------------------------------------------
	// Local Methods 
	//----------------------------------------------------------
	
	/**
	 * Save a buffer to a filename
	 *
	 * @param buffer The screen capture
	 */
	public void saveScreen(Buffer buffer, String basename, int width, int height) {
		
		String filename = basename;
		
		ByteBuffer pixelsRGB = (ByteBuffer) buffer;
		int[] pixelInts = new int[width * height];
		
		// Convert RGB bytes to ARGB ints with no transparency. Flip image vertically by reading the
		// rows of pixels in the byte buffer in reverse - (0,0) is at bottom left in OpenGL.
		
		int p = width * height * 3; // Points to first byte (red) in each row.
		int q;                  // Index into ByteBuffer
		int i = 0;                  // Index into target int[]
		int w3 = width*3;         // Number of bytes in each row
		
		for (int row = 0; row < height; row++) {
			p -= w3;
			q = p;
			
			for (int col = 0; col < width; col++) {
				int iR = pixelsRGB.get(q++);
				int iG = pixelsRGB.get(q++);
				int iB = pixelsRGB.get(q++);
				
				int color = ((iR & 0x000000FF) << 16)
					| ((iG & 0x000000FF) << 8)
					| (iB & 0x000000FF);
				
				if ( color == TRANSPARENT_COLOR ) {
					pixelInts[i++] = 0x00FFFFFF;
				} else {
					pixelInts[i++] = 0xFF000000 | color;
				}
			}
		}
		
		BufferedImage bufferedImage =
			new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		bufferedImage.setRGB( 0, 0, width, height, pixelInts, 0, width );
		
		try {
			File outputFile = new File( iconDir, filename + ".png" );
			System.out.println( "writing file: "+ outputFile );
			ImageIO.write( bufferedImage, "PNG", outputFile );
		} catch ( IOException e ) {
			e.printStackTrace( );
		}
	}
	
	/**
	 * Recalculate the world bounds and update the display
	 */
	private void updateWorldBounds( ) {
		float[] transientSize = new float[]{ 0, 0, 0 };
		float[] size = new float[3];
		int cycles = 10;
		while ( cycles > 0 ) {
			getWorldBounds( );
			if ( worldBounds != null ) {
				worldBounds.getSize( size );
				if ( Arrays.equals( size, ORIGIN ) ) {
					continue;
				}
				if ( !Arrays.equals( size, transientSize ) ) {
					transientSize = size;
					cycles = 10;
				} else if ( --cycles == 0 ) {
					break;
				}
			}
		}
		if ( !batchMode ) {
			boundsField.setText( Arrays.toString( size ) );
			iconScaleField.setValue( new Float( DEFAULT_ICON_SCALE ) );
		}
		System.out.println( Arrays.toString( size ) );
	}
	
	/**
	 * Create and load the X3DScene from the given URL and replace the current 
	 * scene in the browser with it. Setup the additional nodes necessary in the
	 * scene to control and monitor the scene snapshot process.
	 * 
	 * @param url The URL of the scene to load.
	 * @return Whether the loaded scene has successfully loaded and passes the required SMAL mode.
	 */
	private boolean loadScene( URL url ) {
		System.out.println( "loading: "+ url );
		browserReady = false;
		
		// if we're reloading - do some clean up
		if ( tickle_translation != null ) {
			tickle_translation.removeX3DEventListener( this );
		}
		String source = url.toExternalForm( );
		if ( !batchMode ) {
			worldSourceField.setText( source );
		}
		
		///////////////////////////////////////////////////////////////////////////////
		
		X3DScene external_scene = null;
		try {
			external_scene = browser.createX3DFromURL( new String[]{ source } );
			frameThrottle.startedLoading( );
			
			// wait for the load to complete
			while ( !frameThrottle.isInitialLoadDone( ) ) {
				try {
					//System.out.println( "Sleeping ........................." );
					Thread.currentThread( ).sleep( 200 );
				} catch ( InterruptedException ie ) {
				}
			}
		} catch ( Exception e ) {
			System.out.println( "Exception caught while loading scene: " + source );
			System.out.println( "Exception: "+ e );
			Thread.currentThread( ).dumpStack( );
		}
		
		if ( external_scene != null ) {
			if ( smalMode ) {
				if ( !verifySMAL( external_scene ) ) {
					return( false );
				}
			}
			
			ProfileInfo profInfo = browser.getProfile( "Immersive" );
			ComponentInfo cmpInfo = browser.getComponent( "Navigation", 3 );
			scene = browser.createScene( profInfo, new ComponentInfo[]{ cmpInfo } );
			
			X3DNode[] external_node = external_scene.getRootNodes( );
			for ( int i = 0; i < external_node.length; i++ ) {
				X3DNode node = external_node[i];
				external_scene.removeRootNode( node );
				scene.addRootNode( node );
			}
		} else {
			System.out.println( "scene is null" );
			return( false );
		}
		
		browser.replaceWorld( scene );
		
		///////////////////////////////////////////////////////////////////////////////
		// add the nodes we use for managing the capture
		
		ortho_view = (OrthoViewpoint)scene.createNode( "OrthoViewpoint" );
		ortho_view.setOrientation( new float[]{ 1, 0, 0, -1.57075f } );
		scene.addRootNode( ortho_view );
		
		tickle = (Transform)scene.createNode( "Transform" );
		tickle_translation = (SFVec3f)tickle.getField( "translation" );
		tickle_translation.addX3DEventListener( this );
		scene.addRootNode( tickle );
		
		Background background = (Background)scene.createNode( "Background" );
		background.setSkyColor( Color.GREEN.getRGBComponents( null ) );
		scene.addRootNode( background );
		background.setBind( true );
		
		////////////////////////////////////////////////////////////////////
		try {
			synchronized( this ) {
				if ( !browserReady ) {
					wait( );
				}
				readyToCapture = true;
			}
		} catch ( InterruptedException ie ) {
		}
		////////////////////////////////////////////////////////////////////
		
		updateWorldBounds( );
		return( true );
	}
	
	/**
	 * Verify whether the scene contins SMAL metadata
	 */
	private boolean verifySMAL( X3DScene scene ) {
		X3DNode[] rootNode = scene.getRootNodes( );
		for ( int i = 0; i < rootNode.length; i++ ) {
			X3DNode node = rootNode[i];
			if ( node instanceof WorldInfo ) {
				WorldInfo info = (WorldInfo)node;
				SFNode meta = (SFNode)info.getField( "metadata" );
				if ( meta != null ) {
					X3DNode n = meta.getValue( );
					if ( n instanceof MetadataSet ) {
						MetadataSet ms = (MetadataSet)n;
						String name = ms.getName( );
						if ( SMAL.equals( name ) ) {
							return( true );
						}
					}
				}
			}
		}
		return( false );
	}
	
	/**
	 * Instantiate the x3d browser object 
	 */
	private void createBrowser( ) {
		HashMap params = new HashMap( );
		params.put( "Xj3D_ShowConsole", Boolean.TRUE );
		params.put( "Xj3D_NavbarShown", Boolean.FALSE );
		params.put( "Xj3D_StatusBarShown", Boolean.FALSE );
		params.put( "Xj3D_FPSShown", Boolean.FALSE );
		params.put( "Xj3D_LocationShown", Boolean.FALSE );
		params.put( "Xj3D_OpenButtonShown", Boolean.FALSE );
		params.put( "Xj3D_ReloadButtonShown", Boolean.FALSE );
		params.put( "Xj3D_Culling_Mode", "none" );
		
		x3dComponent = BrowserFactory.createX3DComponent( params );
		browser = x3dComponent.getBrowser( );
		
		if ( core == null ) {
			// initialize objects needed to obtain the world bounds
			// and synchronize the capture process
			BrowserComponent bc = (BrowserComponent)x3dComponent;
			canvas = (Component)bc.getCanvas( );
			
			X3DBrowserJPanel obc = (X3DBrowserJPanel)x3dComponent;
			//frameThrottle = obc.getFramerateThrottle( );
			
			core = (OGLStandardBrowserCore)bc.getBrowserCore( );
			core.addCoreListener( this );
		}
	}
	
	/**
	 * Instantiate a new external browser frame with the browser component
	 * set to the argument dimension.
	 *
	 * @param dim The dimension of the browser component
	 * @param decoration Flag indicating whether the frame should have windows decorations
	 */
	private void createBrowserFrame( Dimension dim, boolean decoration ) {
		if ( browserFrame != null ) {
			// don't throw away the browser component
			browserPanel.remove( (Component)x3dComponent );
			browserFrame.dispose( );
		} 
		browserFrame = new JFrame( );
		if ( decoration ) {
			browserFrame.setTitle( "Xj3D Browser" );
		} else {
			browserFrame.setUndecorated( !decoration );
		}
		
		Container bfContentPane = browserFrame.getContentPane( );
		bfContentPane.setLayout( new GridLayout( 1, 1, 0, 0 ) );
		
		browserPanel = new JPanel( new GridLayout( 1, 1, 0, 0 ) );
		browserPanel.setSize( dim );
		browserPanel.setPreferredSize( dim );
		browserPanel.setMinimumSize( dim );
		browserPanel.setMaximumSize( dim );
		browserPanel.add( (Component)x3dComponent );
		
		bfContentPane.add( browserPanel );
		
		browserFrame.pack( );
		browserFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		Dimension screenSize = browserFrame.getToolkit( ).getScreenSize( );
		Dimension frameSize = browserFrame.getSize( );
		browserFrame.setLocation( 
			( screenSize.width - frameSize.width )/2, 
			( screenSize.height - frameSize.height )/2 );
		browserFrame.setVisible( true );
	}
	
	/**
	 * Return the bounds of the 3D world
	 *
	 * @return the bounds of the 3D world
	 */
	private void getWorldBounds( ) {
		
		if ( displayManager == null ) {
			displayManager = core.getDisplayManager( );
		}
		Layer[] layers = new Layer[displayManager.numLayers()];
		displayManager.getLayers(layers);
		SimpleScene currentScene;
		SimpleViewport viewport;
		SimpleLayer layer;
		
		for(int i=0; i < layers.length; i++) {
			//System.out.println("   Layer: " + i);
			if (!(layers[i] instanceof SimpleLayer)) {
				System.out.println("      Scene printer can only handle SimpleLayers currently");
				continue;
			}
			
			layer = (SimpleLayer)layers[i];
			
			if (!(layer.getViewport() instanceof SimpleViewport)) {
				System.out.println("      Scene printer can only handle SimpleViewports currently");
				continue;
			}
			
			viewport = (SimpleViewport)layer.getViewport();
			
			if (!(viewport.getScene() instanceof SimpleScene)) {
				System.out.println("      Scene printer can only handle SimpleScenes currently" );
				//return( size );
			}
			
			currentScene = (SimpleScene)viewport.getScene();
			Group root = currentScene.getRenderedGeometry();
			
			BoundingVolume bounds = ((Node)root).getBounds();
			
			if ( bounds instanceof BoundingBox ) {
				worldBounds = (BoundingBox)bounds;
			}
		}
	} 
	
	/**
	 * calculate the fieldOfView and image size parameters for the snapshots
	 */
	private void generateSnapshotParameters( ) {
		
		updateWorldBounds( );
		
		// the dimension for the browser window taking the snaps
		snapDimension = new Dimension( iconWidth, iconHeight );
		
		float[] size = new float[3];
		worldBounds.getSize( size );
		
		System.out.println( "size = "+ Arrays.toString( size ) );
		
		// total size of the world
		float x_world_meters = (float)Math.ceil( size[0] * 2 );
		float z_world_meters = (float)Math.ceil( size[2] * 2 );
		
		float world_max = Math.max( x_world_meters, z_world_meters );
		
		float width = world_max;
		float height = world_max;
		
		float[] max = new float[3];
		float[] min = new float[3];
		worldBounds.getExtents( min, max );
		
		System.out.println( "min extents = "+ Arrays.toString( min ) );
		System.out.println( "max extents = "+ Arrays.toString( max ) );
		
		float x_center = ( min[0] + max[0] ) / 2;
		float z_center = ( min[2] + max[2] ) / 2;
		
		// the starting point, the upper left corner of the 'world'
		float x_min = x_center - width / 2;
		float z_max = z_center + height / 2;
		
		field = new float[4];
		
		// create the array of fov parameters for the icon and initialize
		float llx = x_min;
		float urz = z_max;
		float urx = llx + width;
		float llz = urz - height;
		field[0] = llx;
		field[1] = llz;
		field[2] = urx;
		field[3] = urz;
	}
	
	/** 
	 * Parse the arg and initialize, internal parameters, create a new instance
	 */
	public static void main( String[] args ) {
		
		String inDir = null;
		String outDir = null;
		String smal = null;
		String ext = null;
		String scale = null;
		String width = null;
		String height = null;
		
		int argIndex = -1;
		
		// first, sort out a help request, the processing mode and
		// the option strings
		for ( int i = 0; i < args.length; i++ ) {
			String argument = args[i];
			if ( argument.startsWith( "-" ) ) {
				try {
					if ( argument.equals( "-help" ) ) {
						System.out.println( USAGE );
						return;
					} else if ( argument.equals( "-batch" ) ) {
						batchMode = true;
						argIndex = i;
					}  else if ( argument.equals( "-recurse" ) ) {
						recurse = true;
						argIndex = i;
					} else if ( argument.equals( "-nosmal" ) ) {
						smalMode = false;
						argIndex = i;
					} else if( argument.equals( "-outdir" ) ) {
						outDir = args[i+1];
						argIndex = i+1;
					} else if( argument.equals( "-ext" ) ) {
						ext = args[i+1];
						argIndex = i+1;
					} else if( argument.equals( "-w" ) ) {
						width = args[i+1];
						argIndex = i+1;
					} else if( argument.equals( "-h" ) ) {
						height = args[i+1];
						argIndex = i+1;
					} else if( argument.equals( "-scale" ) ) {
						scale = args[i+1];
						argIndex = i+1;
					} else if ( argument.startsWith( "-" ) ) {
						System.out.println( "Unknown argument: " + argument );
						argIndex = i;
					}
				} catch ( Exception e ) {
					// this would be an IndexOutOfBounds
				}
			}
		}
		
		// the input directory should be the last unused arg
		if( (args.length > 0) && (argIndex + 1 < args.length) ) {
			inDir = args[args.length - 1];
		}
		
		// validate and configure operating params
		if ( batchMode ) {
			// content directory
			if ( inDir == null ) {
				System.out.println( "No source directory specified, exiting" );
			} else {
				URI uri = null;
				try {
					uri = (new URL( inDir )).toURI( );
				} catch ( Exception e ) {
					System.out.println( "Invalid source directory specified: "+ inDir 
						+" is not a valid URL" );
					System.exit( 0 );
				}
				srcDir = new File( uri );
				if ( !srcDir.exists( ) ) {
					System.out.println( "Invalid source directory specified: "+ srcDir 
						+" does not exist" );
					System.exit( 0 );
				} else if ( !srcDir.isDirectory( ) ) {
					System.out.println( "Invalid source directory specified :"+ srcDir 
						+" is not a directory" );
					System.exit( 0 );
				}
			} 
			// destination directory
			if ( outDir == null ) {
				System.out.println( "No destination directory specified, using default" );
				iconDirName = DEFAULT_DESTINATION_DIRECTORY;
				
			}  else {
				iconDirName = outDir;
			}
		} else {
			// setup source directory
			if ( inDir == null ) {
				System.out.println( "No source directory specified, using default" );
				srcDir = new File( System.getProperty( "user.dir" ) );
			} else {
				URI uri = null;
				try {
					uri = (new URL( inDir )).toURI( );
				} catch ( Exception e ) {
					System.out.println( "Invalid source directory specified: "+ inDir 
						+" is not a valid URL, using default" );
					srcDir = new File( System.getProperty( "user.dir" ) );
				}
				if ( uri != null ) {
					srcDir = new File( uri );
					if ( !srcDir.exists( ) ) {
						System.out.println( "Invalid source directory specified :"+ srcDir 
							+" does not exist, using default" );
						srcDir = new File( System.getProperty( "user.dir" ) );
					} else if ( !srcDir.isDirectory( ) ) {
						System.out.println( "Invalid source directory specified :"+ srcDir 
							+" is not a directory, using default" );
						srcDir = new File( System.getProperty( "user.dir" ) );
					}
				}
			}
			// destination directory
			if ( outDir == null ) {
				System.out.println( "No destination directory specified, using default" );
				iconDirName = DEFAULT_DESTINATION_DIRECTORY;
				
			} else {
				iconDirName = outDir;
			}
		}
		
		// scale
		if ( scale == null ) {
			iconScale = DEFAULT_ICON_SCALE;
		} else {
			try {
				iconScale = Float.valueOf( scale ).floatValue( );
			} catch ( Exception e ) {
				System.out.println( "Unable to parse scale value :"+ scale 
					+", using default" );
				iconScale = DEFAULT_ICON_SCALE;
			}
		}
		
		// width & height
		if ( ( width == null ) && ( height == null ) ) {
			iconWidth = DEFAULT_ICON_WIDTH;
			iconHeight = DEFAULT_ICON_HEIGHT;
		}
		else {
			try {
				iconWidth = Integer.valueOf( width ).intValue( );
				iconHeight = Integer.valueOf( height ).intValue( );
			} catch( Exception e ) {
				System.out.println( "Unable to parse width & height values :"+ width +", "+ height
					+", using defaults" );
				iconWidth = DEFAULT_ICON_WIDTH;
				iconHeight = DEFAULT_ICON_HEIGHT;
			}
		}
		
		// x3d file extension
		if ( ext == null ) {
			x3d_ext = DEFAULT_EXTENSION;
		} else {
			x3d_ext = ext;
		}
		
		X3DIconStyler styler = new X3DIconStyler( );
		
		if ( batchMode ) {
			styler.process( srcDir );
			System.exit( 0 );
		}
	}
	
	/**
	 * Do the batch mode processing
	 *
	 * @param dir The directory to begin processing at
	 */
	private void process( File dir ) {
		System.out.println( "processing directory: "+ dir );
		File[] x3d_files = dir.listFiles( (FileFilter)x3d_file_filter );
		for ( int i = 0; i < x3d_files.length; i++ ) {
			File x3d_file = x3d_files[i];
			URL url = null;
			try {
				url = x3d_file.toURI( ).toURL( );
			} catch ( MalformedURLException mue ) {
				System.out.println( mue.getMessage( ) );
			}
			if ( url != null ) {
				if ( loadScene( url ) ) {
					System.out.println( "processing: "+ x3d_file );
					
					String name = x3d_file.getName( );
					int dot = name.indexOf( "." );
					iconName = name.substring( 0, dot );
					
					// create the field of view parameters for the scene ortho viewpoint
					generateSnapshotParameters( );
					
					// configure the browser for the dimensions of the snapshot
					createBrowserFrame( snapDimension, false );
					
					// start the thread that grabs the buffer and saves the file
					Thread captureThread = new Thread( this, this.getClass( ).getName( ) );
					captureThread.start( );
					
					try {
						captureThread.join( );
					} catch ( InterruptedException ie ) {
					}
				}
			}
		}
		if ( recurse ) {
			File[] dirs = dir.listFiles( (FileFilter)x3d_file_filter );
			for ( int i = 0; i < dirs.length; i++ ) {
				srcDir = dirs[i];
				process( srcDir );
			}
		}
	}
}

