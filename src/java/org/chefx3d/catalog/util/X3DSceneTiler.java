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
package org.chefx3d.catalog.util;

// External imports
import it.geosolutions.utils.imagemosaic.MosaicIndexBuilder;

import java.awt.BorderLayout;
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
import java.awt.image.RenderedImage;

import java.io.*;

import java.net.MalformedURLException;
import java.net.URL;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import java.text.NumberFormat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.imageio.ImageIO;

import javax.media.jai.PlanarImage;

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

//import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;

//import org.geotools.gce.geotiff.GeoTiffWriter;

import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.geometry.GeneralEnvelope;

import org.geotools.referencing.CRS;

import org.j3d.aviatrix3d.BoundingBox;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Layer;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.SimpleLayer;
import org.j3d.aviatrix3d.SimpleScene;
import org.j3d.aviatrix3d.SimpleViewport;

import org.j3d.aviatrix3d.management.DisplayCollection;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
import org.web3d.x3d.sai.SFVec3f;
import org.web3d.x3d.sai.X3DComponent;
import org.web3d.x3d.sai.X3DFieldEvent;
import org.web3d.x3d.sai.X3DFieldEventListener;
import org.web3d.x3d.sai.X3DNode;
import org.web3d.x3d.sai.X3DScene;

import org.web3d.x3d.sai.grouping.Transform;

import org.web3d.x3d.sai.navigation.OrthoViewpoint;

// Local imports
// none

/**
 * Utility for creating image mosaics and pyramids from X3D scenes
 *
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
public class X3DSceneTiler extends JFrame implements
    ScreenCaptureListener, ActionListener, X3DFieldEventListener, BrowserCoreListener, Runnable {

    /** The default local cartesian projection 'well known text' */
    public static final String PROJECTION =
        "LOCAL_CS[\"Cartesian 2D\", \n"+
        "LOCAL_DATUM[\"Unknow\", 0], \n"+
        "UNIT[\"m\", 1.0], \n"+
        "AXIS[\"x\", EAST],  \n"+
        "AXIS[\"y\", NORTH]] ";

    /** the absolute origin */
    private static final float[] ORIGIN = { 0.0f, 0.0f, 0.0f };

    /** Initial browser component dimension */
    private static final Dimension DEFAULT_BROWSER_DIMENSION = new Dimension( 512, 512 );

    /** Initial snapshot dimension */
    private static final Dimension DEFAULT_SNAPSHOT_DIMENSION = new Dimension( 512, 512 );

    /** The number of browser rendering frames to wait between configuring the
    *  viewpoint and initiating the snapshot */
    private static final int DEFAULT_FRAME_DELAY_TILL_SNAP = 2;

    /** The default base name for each tile */
    private static final String DEFAULT_TILE_NAME = "mosaic_";

    /** The default scale for each tile */
    private static final float DEFAULT_TILE_SCALE = 1.0f;

    /** The default base name for each pyramid level's config files */
    private static final String PYRAMID = "pyramid";

    /** The dimension to use for each snapshot */
    private Dimension snapDimension;

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

    /** UI control for the tile filenames */
    private JTextField tileNameField;

    /** The current user directory */
    private File userDir;

    /** The user directory in which to place tile files or the base of a pyramid */
    private File tileDir;

    /** The user directory in which to place tile files for a pyramid level */
    private File mosaicDir;

    /** UI display for the tile output directory */
    private JTextField tileDirField;

    /** UI control for the horizontal image dimension */
    private JFormattedTextField hrzImageDimField;

    /** UI control for the vertical image dimension */
    private JFormattedTextField vrtImageDimField;

    /** UI control for the horizontal center offset dimension */
    private JFormattedTextField hrzOffsetField;

    /** UI control for the vertical center offset dimension */
    private JFormattedTextField vrtOffsetField;

    /** UI control for the horizontal tile dimension */
    private JFormattedTextField hrzTileDimField;

    /** UI control for the vertical tile dimension */
    private JFormattedTextField vrtTileDimField;

    /** UI control for the number of horizontal tiles */
    private JFormattedTextField hrzTileNumField;

    /** UI control for the number of vertical tiles */
    private JFormattedTextField vrtTileNumField;

    /** UI control for the tile scale value */
    private JFormattedTextField tileScaleField;

    /** UI control for the number of levels of the pyramid */
    private JFormattedTextField pyramidLevelField;

    /** UI display of the snapshot progress */
    private JTextField snapStatus;

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

    /** The rendering surface */
    private Component canvas;

    /** The display collection */
    private DisplayCollection displayManager;

    /** The snapshot tile number, used during capture to identify tiles */
    private int tile_num;

    /** The current scene */
    private X3DScene scene;

    /** The viewpoint used to take the scene snapshots */
    private OrthoViewpoint ortho_view;

    /** sync variable, indicating that the next snapshot is 'good to go' */
    private boolean readyToCapture;

    /** flag indicating that a capture cycle has completed */
    private boolean captureComplete;

    /** A 'dummy' node used to stimulate an event from the browser at the
     *  end of an rendering cycle. The event is used to signal that the scene
     *  has rendered following a viewpoint change and is ready for it's photo op. */
    private Transform tickle;

    /** The field of the tickle node that is used */
    private SFVec3f tickle_translation;

    /** sync variable, indicating that the rendering of the scene has completed */
    private boolean sceneHasRendered;

    /** the number of frames to render before setting the sceneHasRendered flag true */
    private int frameCount;

    /** the current world bounds */
    private float[] worldBounds;

    /** the calculated fieldOfView parameters for the OrthoViewpoint for a
    *   pyramid level that are used while taking the snapshots */
    private float[][] field;

    /** scale factor of the X3D scene in units of meters per scene unit */
    private float sceneScale = 1.0f;

    /** scale factor being used in tile capture */
    private float tileScale;

    /** referencing system, used for saving tiles */
    CoordinateReferenceSystem crs;

    /**
     * Constructor
     */
    public X3DSceneTiler( ) {
        super( "X3D Scene Tiler" );

        //String[] imgFmt = ImageIO.getWriterFormatNames( );
        //System.out.println( Arrays.toString( imgFmt ) );

        userDir = new File( System.getProperty( "user.dir" ));

        tileDir = new File( userDir, "tiles" );
        if ( !tileDir.exists( ) ) {
            tileDir.mkdir( );
        }

        System.setProperty( "x3d.sai.factory.class",
            "org.xj3d.ui.awt.browser.ogl.X3DOGLBrowserFactoryImpl" );
        try {
            crs = CRS.parseWKT( PROJECTION );
        } catch ( Exception e ) {
            System.out.println( e.getMessage( ) );
        }
        // configure sizing parameters for creating the snapshot
        GraphicsEnvironment env =
            GraphicsEnvironment.getLocalGraphicsEnvironment( );
        //GraphicsDevice[] devices = env.getScreenDevices( );
        GraphicsDevice device = env.getDefaultScreenDevice();
        DisplayMode mode = device.getDisplayMode( );
        int screenWidth = mode.getWidth( );
        int screenHeight = mode.getHeight( );
        int maxSnapAxisLength = Math.min( screenWidth, screenHeight );
        //snapDimension = new Dimension( maxSnapAxisLength, maxSnapAxisLength );
        snapDimension = DEFAULT_SNAPSHOT_DIMENSION;

        ////////////////////////////////////////////////////////////////////////////////////
        // setup the x3d browser in an external frame
        createBrowser( );
        createBrowserFrame( DEFAULT_BROWSER_DIMENSION, true );
        ////////////////////////////////////////////////////////////////////////////////////

        Container contentPane = this.getContentPane( );
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

        //////////////////////////////////////////////////////////////////////////
        JPanel tilePanel = new JPanel( new GridBagLayout( ) );
        GridBagConstraints cTile = new GridBagConstraints( );
        cTile.fill = GridBagConstraints.HORIZONTAL;
        cTile.weightx = 1; cTile.weighty = 0;
        cTile.insets = new Insets( 2, 2, 2, 2 );
        cTile.gridwidth = GridBagConstraints.REMAINDER;

        TitledBorder tileBorder = new TitledBorder( new EtchedBorder( ) );
        tileBorder.setTitle( "Tile" );
        tilePanel.setBorder( tileBorder );

        JPanel tileOutputPanel = new JPanel( new GridLayout( 2, 1, 2, 2 ) );
        TitledBorder tileOutputBorder = new TitledBorder( new EtchedBorder( ) );
        tileOutputBorder.setTitle( "Output" );
        tileOutputPanel.setBorder( tileOutputBorder );

        JPanel tileDirPanel = new JPanel( new BorderLayout( ) );
        TitledBorder tileDirBorder = new TitledBorder( new EtchedBorder( ) );
        tileDirBorder.setTitle( "Directory" );
        tileDirPanel.setBorder( tileDirBorder );

        URL url = null;
        try {
            url = tileDir.toURI( ).toURL( );
        } catch ( MalformedURLException mue ) {
        }

        tileDirField = new JTextField( url.toExternalForm( ) );
        tileDirField.setEditable( false );
        tileDirField.setHorizontalAlignment( SwingConstants.LEFT );
        tileDirPanel.add( tileDirField, BorderLayout.CENTER );

        tileOutputPanel.add( tileDirPanel );

        JPanel tileNamePanel = new JPanel( new BorderLayout( ) );
        TitledBorder tileNameBorder = new TitledBorder( new EtchedBorder( ) );
        tileNameBorder.setTitle( "Tile Name" );
        tileNamePanel.setBorder( tileNameBorder );

        tileNameField = new JTextField( DEFAULT_TILE_NAME );
        tileNameField.setHorizontalAlignment( SwingConstants.CENTER );
        tileNamePanel.add( tileNameField, BorderLayout.CENTER );

        tileOutputPanel.add( tileNamePanel );

        cTile.gridx = 0; cTile.gridy = 0;
        tilePanel.add( tileOutputPanel, cTile );
        ////////////////////////////////////////////////////////////////////

        JPanel imageSizePanel = new JPanel( new GridLayout( 1, 4, 2, 2 ) );
        TitledBorder imageSizeBorder = new TitledBorder( new EtchedBorder( ) );
        imageSizeBorder.setTitle( "Tile Image Size (Pixels)" );
        imageSizePanel.setBorder( imageSizeBorder );

        NumberFormat fmt = NumberFormat.getIntegerInstance( );

        JLabel hrzImageDimLabel = new JLabel( "X: " );
        hrzImageDimLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        imageSizePanel.add( hrzImageDimLabel );

        hrzImageDimField = new JFormattedTextField( fmt );
        hrzImageDimField.setHorizontalAlignment( SwingConstants.CENTER );
        hrzImageDimField.setEditable( false );
        hrzImageDimField.setValue( new Integer( DEFAULT_SNAPSHOT_DIMENSION.width ) );
        imageSizePanel.add( hrzImageDimField );

        JLabel vrtImageDimLabel = new JLabel( "Z: " );
        vrtImageDimLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        imageSizePanel.add( vrtImageDimLabel );

        vrtImageDimField = new JFormattedTextField( fmt );
        vrtImageDimField.setHorizontalAlignment( SwingConstants.CENTER );
        vrtImageDimField.setEditable( false );
        vrtImageDimField.setValue( new Integer( DEFAULT_SNAPSHOT_DIMENSION.height ) );
        imageSizePanel.add( vrtImageDimField );

        cTile.gridx = 0; cTile.gridy = 1;
        tilePanel.add( imageSizePanel, cTile );
        //////////////////////////////////////////////////////////////////////////
        JPanel tileSizePanel = new JPanel( new GridLayout( 1, 4, 2, 2 ) );
        TitledBorder tileSizeBorder = new TitledBorder( new EtchedBorder( ) );
        tileSizeBorder.setTitle( "Tile Coverage Size (Meters)" );
        tileSizePanel.setBorder( tileSizeBorder );

        fmt = NumberFormat.getIntegerInstance( );

        JLabel hrzTileDimLabel = new JLabel( "X: " );
        hrzTileDimLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        tileSizePanel.add( hrzTileDimLabel );

        hrzTileDimField = new JFormattedTextField( fmt );
        hrzTileDimField.setHorizontalAlignment( SwingConstants.CENTER );
        tileSizePanel.add( hrzTileDimField );

        JLabel vrtTileDimLabel = new JLabel( "Z: " );
        vrtTileDimLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        tileSizePanel.add( vrtTileDimLabel );

        vrtTileDimField = new JFormattedTextField( fmt );
        vrtTileDimField.setHorizontalAlignment( SwingConstants.CENTER );
        tileSizePanel.add( vrtTileDimField );

        cTile.gridx = 0; cTile.gridy = 2;
        tilePanel.add( tileSizePanel, cTile );

        //////////////////////////////////////////////////////////////////////////
        JPanel tileNumPanel = new JPanel( new GridLayout( 1, 4, 2, 2 ) );
        TitledBorder tileNumBorder = new TitledBorder( new EtchedBorder( ) );
        tileNumBorder.setTitle( "Number of Tiles" );
        tileNumPanel.setBorder( tileNumBorder );

        JLabel hrzNumLabel = new JLabel( "X: " );
        hrzNumLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        tileNumPanel.add( hrzNumLabel );

        hrzTileNumField = new JFormattedTextField( fmt );
        hrzTileNumField.setHorizontalAlignment( SwingConstants.CENTER );
        tileNumPanel.add( hrzTileNumField );

        JLabel vrtNumLabel = new JLabel( "Z: " );
        vrtNumLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        tileNumPanel.add( vrtNumLabel );

        vrtTileNumField = new JFormattedTextField( fmt );
        vrtTileNumField.setHorizontalAlignment( SwingConstants.CENTER );
        tileNumPanel.add( vrtTileNumField );

        cTile.gridx = 0; cTile.gridy = 3;
        tilePanel.add( tileNumPanel, cTile );

        //////////////////////////////////////////////////////////////////////////
        JPanel tileOffsetPanel = new JPanel( new GridLayout( 1, 4, 2, 2 ) );
        TitledBorder tileOffsetBorder = new TitledBorder( new EtchedBorder( ) );
        tileOffsetBorder.setTitle( "Center Offset (Meters)" );
        tileOffsetPanel.setBorder( tileOffsetBorder );

        JLabel hrzOffsetLabel = new JLabel( "X: " );
        hrzOffsetLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        tileOffsetPanel.add( hrzOffsetLabel );

        hrzOffsetField = new JFormattedTextField( fmt );
        hrzOffsetField.setHorizontalAlignment( SwingConstants.CENTER );
		hrzOffsetField.setValue( new Integer( 0 ) );
        tileOffsetPanel.add( hrzOffsetField );

        JLabel vrtOffsetLabel = new JLabel( "Z: " );
        vrtOffsetLabel.setHorizontalAlignment( SwingConstants.RIGHT );
        tileOffsetPanel.add( vrtOffsetLabel );

        vrtOffsetField = new JFormattedTextField( fmt );
        vrtOffsetField.setHorizontalAlignment( SwingConstants.CENTER );
		vrtOffsetField.setValue( new Integer( 0 ) );
        tileOffsetPanel.add( vrtOffsetField );

        cTile.gridx = 0; cTile.gridy = 4;
        tilePanel.add( tileOffsetPanel, cTile );

        //////////////////////////////////////////////////////////////////////////
        JPanel tileScalePanel = new JPanel( new GridLayout( 1, 1, 2, 2 ) );
        TitledBorder tileScaleBorder = new TitledBorder( new EtchedBorder( ) );
        tileScaleBorder.setTitle( "Tile Scale (Meters per Pixel)" );
        tileScalePanel.setBorder( tileScaleBorder );

        NumberFormat d_fmt = NumberFormat.getNumberInstance( );

        tileScaleField = new JFormattedTextField( d_fmt );
        tileScaleField.setHorizontalAlignment( SwingConstants.CENTER );
        tileScaleField.setValue( new Float( DEFAULT_TILE_SCALE ) );
        tileScalePanel.add( tileScaleField );

        cTile.gridx = 0; cTile.gridy = 5;
        cTile.weightx = 0.5; cTile.weighty = 0;
        cTile.gridwidth = 1;
        tilePanel.add( tileScalePanel, cTile );

        //////////////////////////////////////////////////////////////////////////
        JPanel pyramidLevelPanel = new JPanel( new GridLayout( 1, 1, 2, 2 ) );
        TitledBorder pyramidLevelBorder = new TitledBorder( new EtchedBorder( ) );
        pyramidLevelBorder.setTitle( "Pyramid Levels" );
        pyramidLevelPanel.setBorder( pyramidLevelBorder );

        pyramidLevelField = new JFormattedTextField( fmt );
        pyramidLevelField.setHorizontalAlignment( SwingConstants.CENTER );
        pyramidLevelPanel.add( pyramidLevelField );

        cTile.gridx = 1; cTile.gridy = 5;
        cTile.weightx = 0.5; cTile.weighty = 0;
        cTile.gridwidth = 1;
        tilePanel.add( pyramidLevelPanel, cTile );

        //////////////////////////////////////////////////////////////////////////
        snapStatus = new JTextField( );
        snapStatus.setHorizontalAlignment( SwingConstants.LEFT );
        snapStatus.setEditable( false );

        cTile.gridx = 0; cTile.gridy = 6;
        cTile.weightx = 1; cTile.weighty = 0;
        cTile.gridwidth = GridBagConstraints.REMAINDER;
        tilePanel.add( snapStatus, cTile );

        c.gridx = 0; c.gridy = 1;
        c.weightx = 1; c.weighty = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        dataPanel.add( tilePanel, c );

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
        controlPanel.add( stopButton );

        JPopupMenu.setDefaultLightWeightPopupEnabled( false );
        JMenuBar mb = new JMenuBar( );
        this.setJMenuBar( mb );

        JMenu fileMenu = new JMenu( "File" );
        mb.add( fileMenu );

        openItem = new JMenuItem( "Open World" );
        openItem.addActionListener( this );
        fileMenu.add( openItem );

        chooser = new JFileChooser( userDir );
/*
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "X3D Files", "wrl", "x3d", "x3dv" );
        chooser.setFileFilter( filter );
*/
        saveItem = new JMenuItem( "Select Output Directory" );
        saveItem.addActionListener( this );
        fileMenu.add( saveItem );

        saveChooser = new JFileChooser( tileDir );
        saveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // this timer runs continuously and the event generated by it will
        // acquire and display the scene bounds if a scene is active. this is
        // inefficient, but far easier than trying to sort out if and when content
        // has completed loading.....
        timer = new Timer( 2000, this );
        timer.start( );
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

        sceneHasRendered = false;

        Dimension size = canvas.getSize( );

        // the number of pyramid levels
        int levels = ((Number)pyramidLevelField.getValue( )).intValue( );

        // the tile dimensions in meter
        int x_tile_meters = ((Number)hrzTileDimField.getValue( )).intValue( );
        int z_tile_meters = ((Number)vrtTileDimField.getValue( )).intValue( );

        // the number of tiles along each axis
        int x_num_tiles = ((Number)hrzTileNumField.getValue( )).intValue( );
        int z_num_tiles = ((Number)vrtTileNumField.getValue( )).intValue( );

		// the offset from the center of the world 
		// to place the center of the capture area
		int x_offset_meters = ((Number)hrzOffsetField.getValue( )).intValue( );
		int z_offset_meters = ((Number)vrtOffsetField.getValue( )).intValue( );
		
        // scale factor for the image tiles in units of meters per pixel
        //float imageScale = ((Number)tileScaleField.getValue( )).floatValue( );
        tileScale = ((Number)tileScaleField.getValue( )).floatValue( );

        // the envelope bounds of the pyramid
        float x_dim = x_tile_meters * x_num_tiles / 2;
        float z_dim = z_tile_meters * z_num_tiles / 2;
        float[] envelope = new float[]{
            -x_dim + x_offset_meters, 
			-z_dim + z_offset_meters, 
			x_dim + x_offset_meters, 
			z_dim + z_offset_meters,
        };

        // pyramid properties describing each level
        float[] scale_per_level = new float[levels];
        String[] levelDirName = new String[levels];

        // create each mosaic level for the pyramid
        for ( int j = 0; j < levels; j++ ) {
            if ( terminate ) {
                break;
            }

            // each level of the pyramid in a separate directory
            String mosaicDirName = Integer.toString( j + 1 );
            levelDirName[j] = mosaicDirName;
            mosaicDir = new File( tileDir, mosaicDirName );
            if ( !mosaicDir.exists( ) ) {
                mosaicDir.mkdir( );
            }

            // create the field of view parameters for the scene ortho viewpoint
            generateSnapshotParameters(
                x_tile_meters,
                z_tile_meters,
				x_offset_meters,
				z_offset_meters,
                x_num_tiles,
                z_num_tiles,
                tileScale );

            tile_num = 0;
            scale_per_level[j] = tileScale;

            // generate the image, world and projection files for each tile of the mosiac
            for ( int i = 0; i < field.length; i++ ) {
                if ( terminate ) {
                    break;
                }
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
                snapStatus.setText( "Processing Level "+ ( j + 1 ) +", Tile Number "+ ( i + 1 ) +" of "+ field.length );

                // Set the viewpoint parameters for the next snapshot
                //System.out.println( Arrays.toString( field[i] ) );
                ortho_view.setFieldOfView( field[i] );
                ortho_view.setPosition( new float[]{ 0, (worldBounds[1] * 2), 0 } );
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
            }
            if ( !terminate ) {
                try {
                    // wait for the signal that the previous snapshot has been processed
                    // before generating the remainder of the configuration files
                    synchronized( this ) {
                        if ( !readyToCapture ) {
                            wait( );
                        }
                    }
                } catch ( InterruptedException ie ) {
                }

                // create the mosaic projection file
                try {
                    File prjFile = new File( mosaicDir, PYRAMID + ".prj" );
                    BufferedWriter writer = new BufferedWriter( new FileWriter( prjFile ) );
                    writer.write( PROJECTION, 0, PROJECTION.length( ) );
                    writer.flush( );
                    writer.close( );
                } catch ( IOException e ) {
                    e.printStackTrace( );
                }

                // create the mosaic index
                MosaicIndexBuilder mib = new MosaicIndexBuilder( );
                mib.setLocationPath( mosaicDir.getAbsolutePath( ) );
                mib.setWildcardString( "*.tif" );
                mib.setIndexName( PYRAMID );
                Thread mibThread = new Thread( mib, "MosaicIndexBuilder" );
                mibThread.start();
                try {
                    mibThread.join();
                } catch ( InterruptedException ie ) {
                    System.out.println( ie.getMessage( ) );
                }

                // re-create the mosaic projection file
                // it apparently gets deleted by the index creation -
                // which is a BAD thing.....
                try {
                    File prjFile = new File( mosaicDir, PYRAMID + ".prj" );
                    BufferedWriter writer = new BufferedWriter( new FileWriter( prjFile ) );
                    writer.write( PROJECTION, 0, PROJECTION.length( ) );
                    writer.flush( );
                    writer.close( );
                } catch ( IOException e ) {
                    e.printStackTrace( );
                }

                // recalculate the parameters used to generate the snapshot
                // field-of-view parameters for the next pyramid level
                x_tile_meters *= 2;
                z_tile_meters *= 2;
                x_num_tiles /= 2;
                z_num_tiles /= 2;
                tileScale *= 2;
            }
        }
        if ( !terminate ) {
            // create the pyramid projection file
            try {
                File prjFile = new File( tileDir, PYRAMID + ".prj" );
                BufferedWriter writer = new BufferedWriter( new FileWriter( prjFile ) );
                writer.write( PROJECTION, 0, PROJECTION.length( ) );
                writer.flush( );
                writer.close( );
            } catch ( IOException e ) {
                e.printStackTrace( );
            }
            // create the pyramid properties file
            Properties properties = new Properties( );

            properties.setProperty( "Name", PYRAMID );

            StringBuffer envBuffer = new StringBuffer( );
            envBuffer.append(
                Float.toString( envelope[0] ) +","+
                Float.toString( envelope[1] ) +" "+
                Float.toString( envelope[2] ) +","+
                Float.toString( envelope[3] ) );
            properties.setProperty( "Envelope2D", envBuffer.toString( ) );

            properties.setProperty( "LevelsNum", Integer.toString( levels ) );

            StringBuffer levelBuffer = new StringBuffer( );
            StringBuffer dirBuffer = new StringBuffer( );
            for (int i = 0; i < levels; i++) {
                String scale = Float.toString( scale_per_level[i] );
                levelBuffer.append( scale +","+ scale +" " );
                dirBuffer.append( levelDirName[i] +" " );
            }
            properties.setProperty( "Levels", levelBuffer.toString( ) );
            properties.setProperty( "LevelsDirs", dirBuffer.toString( ) );

            try {
                File propFile = new File( tileDir, PYRAMID + ".properties" );
//                BufferedWriter writer = new BufferedWriter( new FileWriter( propFile ) );
                BufferedOutputStream writer = new BufferedOutputStream( new FileOutputStream( propFile ) );
                properties.store( writer, "" );
            } catch ( IOException ioe ) {
                System.out.println( ioe.getMessage( ) );
            }
        }
        // restore the browser to it's decorated frame, re-enable the ui
        EventQueue.invokeLater( new Runnable( ) {
                public void run( ) {
                    captureComplete = true;
                    createBrowserFrame( DEFAULT_BROWSER_DIMENSION, true );

                    hrzTileDimField.setEditable( true );
                    vrtTileDimField.setEditable( true );
                    hrzTileNumField.setEditable( true );
                    vrtTileNumField.setEditable( true );
                    tileScaleField.setEditable( true );
                    pyramidLevelField.setEditable( true );

                    stopButton.setEnabled( false );
                }
            } );

        terminate = false;
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

            hrzTileDimField.setEditable( false );
            vrtTileDimField.setEditable( false );
            hrzTileNumField.setEditable( false );
            vrtTileNumField.setEditable( false );
            tileScaleField.setEditable( false );
            pyramidLevelField.setEditable( false );

            createBrowserFrame( snapDimension, false );

            Thread captureThread = new Thread( this, this.getClass( ).getName( ) );
            captureThread.start( );

        } else if ( source == openItem ) {
            int returnVal = chooser.showDialog( this, "Open World" );
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
                }
            }
        } else if ( source == saveItem ) {
            int returnVal = saveChooser.showDialog( this, "Select" );
            if( returnVal == JFileChooser.APPROVE_OPTION ) {
                tileDir = saveChooser.getSelectedFile( );
                URL url = null;
                try {
                    url = tileDir.toURI( ).toURL( );
                } catch ( MalformedURLException mue ) {
                }

                tileDirField.setText( url.toExternalForm( ) );
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
        
        //String fname = tileNameField.getText( ) + tile_num;
        String fname = tileNameField.getText( );
        saveScreen( buffer, fname, width, height );

        synchronized ( this ) {
            tile_num++;
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
        captureButton.setEnabled( true );
        readyToCapture = true;
        if ( captureComplete ) {
            captureComplete = false;
            // this doesn't seem to work, commenting out for now
            //browser.firstViewpoint( );
        }
    }

    /**
     * The tried to load a URL and failed. It is typically because none of
     * the URLs resolved to anything valid or there were network failures.
     *
     * @param msg An error message to go with the failure
     */
    public void urlLoadFailed( String msg ) {
        captureButton.setEnabled( false );
        readyToCapture = false;
    }

    /**
     * The browser has been shut down and the previous content is no longer
     * valid.
     */
    public void browserShutdown( ) {
        captureButton.setEnabled( false );
        readyToCapture = false;
    }

    /**
     * The browser has been disposed, all resources may be freed.
     */
    public void browserDisposed( ) {
        captureButton.setEnabled( false );
        readyToCapture = false;
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

        String filename = basename + tile_num;

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

                pixelInts[i++] = 0xFF000000
                    | ((iR & 0x000000FF) << 16)
                    | ((iG & 0x000000FF) << 8)
                    | (iB & 0x000000FF);
            }

        }

        BufferedImage bufferedImage =
            new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        bufferedImage.setRGB( 0, 0, width, height, pixelInts, 0, width );

        try {
            File outputFile = new File( mosaicDir, filename + ".tif" );
            ImageIO.write( bufferedImage, "TIF", outputFile );
        } catch ( IOException e ) {
            e.printStackTrace( );
        }

        // generate the world file
        float imageScale = tileScale;
        float imageScale2 = imageScale/2;
        float ulx = field[tile_num][0] + imageScale2;
        float uly = field[tile_num][3] - imageScale2;
        StringBuffer wldBuffer = new StringBuffer( );
        wldBuffer.append( Float.toString( imageScale ) + " \n" );
        wldBuffer.append( Float.toString( 0 ) + " \n" );
        wldBuffer.append( Float.toString( 0 ) + " \n" );
        wldBuffer.append( Float.toString( -imageScale ) + " \n" );
        wldBuffer.append( Float.toString( ulx ) + " \n" );
        wldBuffer.append( Float.toString( uly ) + " \n" );
        String worldString = wldBuffer.toString( );

        BufferedWriter writer = null;
        try {
            File wldFile = new File( mosaicDir, filename + ".wld" );
            writer = new BufferedWriter( new FileWriter( wldFile ) );
            writer.write( worldString, 0, worldString.length( ) );
            writer.flush( );
            writer.close( );

            File prjFile = new File( mosaicDir, filename + ".prj" );
            writer = new BufferedWriter( new FileWriter( prjFile ) );
            writer.write( PROJECTION, 0, PROJECTION.length( ) );
            writer.flush( );
            writer.close( );
        } catch ( IOException e ) {
            e.printStackTrace( );
        }

        /*
        PlanarImage pi = PlanarImage.wrapRenderedImage( bufferedImage );

        GridCoverageFactory gcf = new GridCoverageFactory( );

        // gt coords are defined upper left, lower right - rather than
        // lower left, upper right as the orthoviewpoint. also note the
        // y coordinate inversion
        GeneralEnvelope gex = new GeneralEnvelope(
            new GeneralDirectPosition( field[tile_num][0], -field[tile_num][3] ),
            new GeneralDirectPosition( field[tile_num][2], -field[tile_num][1] ) );
        gex.setCoordinateReferenceSystem( crs );

        GridCoverage2D gc = gcf.create( "Tile", (RenderedImage)pi, gex );
        try {
            File outputFile = new File( userDir, filename + ".tif" );
            GeoTiffWriter writerWI = new GeoTiffWriter( outputFile );
            writerWI.write( gc, null );
        } catch ( IOException e ) {
            e.printStackTrace( );
        }
        */
    }

    /**
     * Recalculate the world bounds and update the display and
     * snapshot parameters if necessary.
     */
    private void updateWorldBounds( ) {
        float[] bounds = getWorldBounds( );
        if ( !Arrays.equals( worldBounds, bounds ) ) {
            worldBounds = bounds;
            boundsField.setText( Arrays.toString( worldBounds ) );
            calculateTileDimensions( );
            tileScaleField.setValue( new Float( DEFAULT_TILE_SCALE ) );
        }
    }

    /**
     * Create and load the X3DScene from the given URL and replace the current
     * scene in the browser with it. Setup the additional nodes necessary in the
     * scene to control and monitor the scene snapshot process.
     *
     * @param url The URL of the scene to load.
     */
    private void loadScene( URL url ) {

        readyToCapture = false;

        // if we're reloading - do some clean up
        if ( tickle_translation != null ) {
            tickle_translation.removeX3DEventListener( this );
        }
        String source = url.toExternalForm( );
        worldSourceField.setText( source );
        ///////////////////////////////////////////////////////////////////////////////
        // if the loaded scene doesn't declare Navigation:3 - then OrthoViewpoint
        // won't work, loading the external scene then transfering it's root nodes
        // to a scene we've set up has issues too - missing nodes. - perhaps need
        // a monitor to determine when the external scene is fully loaded before
        // doing the transfer ????
        /*
        ProfileInfo profInfo = browser.getProfile( "Immersive" );
        ComponentInfo cmpInfo = browser.getComponent( "Navigation", 3 );
        scene = browser.createScene( profInfo, new ComponentInfo[]{ cmpInfo } );

        X3DScene external_scene = browser.createX3DFromURL( new String[]{ source } );
        X3DNode[] external_node = external_scene.getRootNodes( );
        for ( int i = 0; i < external_node.length; i++ ) {
            X3DNode node = external_node[i];
            external_scene.removeRootNode( node );
            scene.addRootNode( node );
        }
        */
        ///////////////////////////////////////////////////////////////////////////////
        // presuming that the loaded scene declares the required components for now.
        scene = browser.createX3DFromURL( new String[]{ source } );
        browser.replaceWorld( scene );
        ///////////////////////////////////////////////////////////////////////////////

        ortho_view = (OrthoViewpoint)scene.createNode( "OrthoViewpoint" );
        ortho_view.setOrientation( new float[]{ 1, 0, 0, -1.57075f } );
        scene.addRootNode( ortho_view );

        tickle = (Transform)scene.createNode( "Transform" );
        tickle_translation = (SFVec3f)tickle.getField( "translation" );
        tickle_translation.addX3DEventListener( this );
        scene.addRootNode( tickle );

        if ( core == null ) {
            // initialize objects needed to obtain the world bounds
            BrowserComponent bc = (BrowserComponent)x3dComponent;
            canvas = (Component)bc.getCanvas( );

            core = (OGLStandardBrowserCore)bc.getBrowserCore( );
            core.addCoreListener( this );
        }
    }

    /**
     * Instantiate the x3d browser object
     */
    private void createBrowser( ) {
        HashMap params = new HashMap( );
        params.put( "Xj3D_ShowConsole", Boolean.FALSE );
        params.put( "Xj3D_NavbarShown", Boolean.FALSE );
        params.put( "Xj3D_StatusBarShown", Boolean.FALSE );
        params.put( "Xj3D_FPSShown", Boolean.FALSE );
        params.put( "Xj3D_LocationShown", Boolean.FALSE );
        params.put( "Xj3D_OpenButtonShown", Boolean.FALSE );
        params.put( "Xj3D_ReloadButtonShown", Boolean.FALSE );
        params.put( "Xj3D_Culling_Mode", "none" );

        x3dComponent = BrowserFactory.createX3DComponent( params );
        browser = x3dComponent.getBrowser( );
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
        browserFrame.setDefaultCloseOperation( EXIT_ON_CLOSE );
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
    private float[] getWorldBounds( ) {

        float[] size = new float[3];

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
                return( size );
            }

            currentScene = (SimpleScene)viewport.getScene();
            Group root = currentScene.getRenderedGeometry();

            BoundingVolume bounds = ((Node)root).getBounds();

            if (bounds instanceof BoundingBox) {
                ((BoundingBox)bounds).getSize(size);
            }
        }
        return( size );
    }

    /**
     * Calculate initial tile size params constrained by the desired max
     */
    private void calculateTileDimensions( ) {

        // maximum dimensions for the image along each axis
        int x_max = DEFAULT_SNAPSHOT_DIMENSION.width;
        int z_max = DEFAULT_SNAPSHOT_DIMENSION.height;

        // total size of the world
        int x_world_meters = Math.round( worldBounds[0] * 2 );
        int z_world_meters = Math.round( worldBounds[2] * 2 );

        x_world_meters = MathUtils.nearestPowerTwo( x_world_meters, true );
        z_world_meters = MathUtils.nearestPowerTwo( z_world_meters, true );

        int x_tile_meters = 0;
        int z_tile_meters = 0;

        int x_num_tiles = x_world_meters / x_max;
        int z_num_tiles = z_world_meters / z_max;

        // TODO: this breaks if one dimension is smaller than the max and the other is larger.....
        if ( ( x_num_tiles == 0 ) && ( z_num_tiles == 0 ) ) {
            // the entire scene is smaller than the desired snap size.
            // set the tile size to the world size
            x_num_tiles = 1;
            z_num_tiles = 1;
            x_tile_meters = x_world_meters;
            z_tile_meters = z_world_meters;
        } else {
            if ( ( x_num_tiles * x_max ) < x_world_meters ) {
                x_num_tiles++;
            }
            if ( ( z_num_tiles * z_max ) < z_world_meters ) {
                z_num_tiles++;
            }
            x_tile_meters = x_max;
            z_tile_meters = z_max;
        }
        int x_levels = MathUtils.computeLog( x_world_meters ) - MathUtils.computeLog( x_tile_meters ) + 1;
        if ( x_levels < 1 ) {
            x_levels = 1;
        }
        int z_levels = MathUtils.computeLog( z_world_meters ) - MathUtils.computeLog( z_tile_meters ) + 1;
        if ( z_levels < 1 ) {
            z_levels = 1;
        }
        int levels = Math.min( x_levels, z_levels );

        pyramidLevelField.setValue( new Integer( levels ) );
        hrzTileDimField.setValue( new Integer( x_tile_meters ) );
        vrtTileDimField.setValue( new Integer( z_tile_meters ) );
        hrzTileNumField.setValue( new Integer( x_num_tiles ) );
        vrtTileNumField.setValue( new Integer( z_num_tiles ) );
    }

    /**
     * calculate the fieldOfView and image size parameters for the snapshots
     */
    private void generateSnapshotParameters(
        int x_tile_meters,
        int z_tile_meters,
		int x_offset_meters,
		int z_offset_meters,
        int x_num_tiles,
        int z_num_tiles,
        float imageScale ) {

        // should this be hard coded ???
        int x_tile_pixels = (int)((float)x_tile_meters/imageScale);
        int z_tile_pixels = (int)((float)z_tile_meters/imageScale);

        // the dimension for the browser window taking the snaps
        snapDimension = new Dimension( x_tile_pixels, z_tile_pixels );

        // the starting point, the upper left corner of the 'world'
        int x_min = ( -x_tile_meters * x_num_tiles / 2 ) + x_offset_meters;
        int z_max = ( z_tile_meters * z_num_tiles / 2 ) + z_offset_meters;

        int num_tiles = x_num_tiles * z_num_tiles;
        field = new float[num_tiles][];

        // create the arrays of fov parameters for each tile and initialize
        for ( int z = 0; z < z_num_tiles; z++ ) {
            for ( int x = 0; x < x_num_tiles; x++ ) {
                int tile_idx = x + ( z * x_num_tiles );
                field[tile_idx] = new float[4];
                int llx = x_min + ( x * x_tile_meters );
                int urz = z_max - ( z * z_tile_meters );
                int urx = llx + x_tile_meters;
                int llz = urz - z_tile_meters;
                field[tile_idx][0] = llx;
                field[tile_idx][1] = llz;
                field[tile_idx][2] = urx;
                field[tile_idx][3] = urz;
            }
        }
    }

    /**
     *
     */
    public static void main( String[] args ) {
        X3DSceneTiler frame = new X3DSceneTiler( );
        frame.pack( );
        frame.setDefaultCloseOperation( EXIT_ON_CLOSE );
        Dimension screenSize = frame.getToolkit( ).getScreenSize( );
        Dimension frameSize = frame.getSize( );
        frame.setLocation(
            ( screenSize.width - frameSize.width )/2,
            ( screenSize.height - frameSize.height )/2 );
        frame.setVisible( true );
    }
}
