/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2009
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

package org.chefx3d.catalog.util.awt;

// External imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.MalformedURLException;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import java.util.HashMap;

import javax.imageio.ImageIO;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;

import org.jeospace.coordinate.Cartesian;
import org.jeospace.coordinate.Ellipsoidal;

import org.jeospace.geometry.Ellipsoid;

import org.jeospace.measure.Length;
import org.jeospace.measure.LengthUnit;

import org.jeospace.physics.orbit.Earth;

import org.web3d.browser.ScreenCaptureListener;

import org.web3d.vrml.renderer.ogl.browser.OGLStandardBrowserCore;

import org.web3d.x3d.sai.BrowserEvent;
import org.web3d.x3d.sai.BrowserFactory;
import org.web3d.x3d.sai.BrowserListener;
import org.web3d.x3d.sai.ComponentInfo;
import org.web3d.x3d.sai.ExternalBrowser;
import org.web3d.x3d.sai.ProfileInfo;
import org.web3d.x3d.sai.X3DComponent;
import org.web3d.x3d.sai.X3DNode;
import org.web3d.x3d.sai.X3DScene;

import org.web3d.x3d.sai.environmentalsensor.ProximitySensor;

import org.xj3d.sai.Xj3DBrowser;
import org.xj3d.ui.awt.browser.ogl.X3DBrowserJPanel;

// Local imports
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.DefaultCatalogManager;

import org.chefx3d.model.MultiplicityConstraint;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PropertyValidator;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.util.FileLoader;
import org.chefx3d.property.FileNameValidator;

/**
 * An X3D browser application used to select and create a Location
 *
 * @author Rex Melton
 * @version $Revision: 1.15 $
 */
public class LocationBrowser extends JPanel implements 
	Runnable, ActionListener, ScreenCaptureListener {
	
	/** Projection WKT (Well Known Text) */
	private static final String PROJECTION = 
		"LOCAL_CS[\"Cartesian 2D\",LOCAL_DATUM[\"Unknow\", 0],UNIT[\"m\", 1.0],AXIS[\"x\", EAST],AXIS[\"y\", NORTH]]";
	
	/** dimensions for icon production */
	private static final int[] ICON_SIZE = { 16, 32, 64 };
	
	/** The X3D browser UI component */
	private X3DComponent x3dComponent;
	
	/** The X3D browser object */
	private ExternalBrowser browser;
	
	/** The X3D Scene object */
	private X3DScene scene;
	
	/** Proximity sensor at the origin of the globe. Used to
	 * determine the users elevation above the terrain */
	private ProximitySensor ps;
	
	/** Capture initiation button */
	private JButton snapButton;
	
	/** Location name field */
	private JTextField nameField;
	
	/** Width of the captured image */
	private int image_width;
	
	/** Height of the captured image */
	private int image_height;
	
	/** Synchronization flag indicating that an image capture is in progress */
	private boolean captureInProgress;
	
	/** working directory where Location files are placed */
	private File workDir;
	
	/** The name of the catalog that the target tool group is in */
	private String catalogName;
	
	/** The name of the tool group that the produced 'Tool' is added to */
	private String toolGroupName;
	
	/**
	 * Constructor
	 */
	public LocationBrowser(String worldURL, String catalogName, String toolGroupName) {
		super(new BorderLayout());
		
		///////////////////////////////////////////////////////////////////
		// should probably be checking these parameters for null values,
		// otherwise things will likely get fubar later on
		this.catalogName = catalogName;
		this.toolGroupName = toolGroupName;
		///////////////////////////////////////////////////////////////////
		
		System.setProperty("org.xj3d.core.loading.threads", "4");
		
		JPanel controlPanel = new JPanel(new BorderLayout());

        snapButton = new JButton();
		snapButton.setText("Create");
        snapButton.setToolTipText("Select Area");
		snapButton.addActionListener(this);
		controlPanel.add(snapButton, BorderLayout.WEST);
		
		nameField = new JTextField("New_Location");
		nameField.addActionListener(this);
		controlPanel.add(nameField, BorderLayout.CENTER);
		
		add(controlPanel, BorderLayout.NORTH);
		createBrowser(worldURL);
		
		// fix the browser size, it won't be perfectly square because
		// the navigation panel is included in the component......
		Dimension dim = new Dimension(600, 600);
		JPanel browserPanel = new JPanel(new GridLayout(1, 1, 0, 0));
        browserPanel.setSize(dim);
        browserPanel.setPreferredSize(dim);
        browserPanel.setMinimumSize(dim);
        browserPanel.setMaximumSize(dim);
        browserPanel.add((Component)x3dComponent);
		
		add(browserPanel, BorderLayout.CENTER);
		
		// drop the images into a tmp dir off the current user directory
		File dir = new File(System.getProperty("user.dir"));
		workDir = new File(dir, "tmp");
		if (!workDir.exists()) {
			workDir.mkdir();
		}
	}
	
	//----------------------------------------------------------
	// Methods required for Runnable
	//----------------------------------------------------------
	
	/**
	 * A separate thread for sequencing the image capture and location creation
	 */
	public void run() {
		
		try {
			synchronized(this) {
				captureInProgress = true;
				((OGLStandardBrowserCore)((X3DBrowserJPanel)x3dComponent).getBrowserCore()).captureScreenOnce(this);
				while(captureInProgress) {
					// wait until the image is complete before creating
					// the remaining data files
					wait();
				}
			}
		}
		catch (InterruptedException ie) {
		}
		createLocation();
	}
	
	//----------------------------------------------------------
	// Methods required for ActionListener
	//----------------------------------------------------------
	
	/**
	 * Actions for initiating the location creation
	 *
	 * @param evt The event that caused this method to be called.
	 */
	public void actionPerformed(ActionEvent evt) {
		
		Object source = evt.getSource();
		if (source == snapButton) {
			
			// initiate the capture and creation
			Thread thread = new Thread(this);
			thread.start();
			
		} else if (source == nameField) {
			
			// make sure the name is not blank
			String name = nameField.getText();
			if (name.length() > 0) {
				snapButton.setEnabled(true);
			} else {
				snapButton.setEnabled(false);
			}
		}
	}
	
	//----------------------------------------------------------
	// Methods required for ScreenCaptureListener
	//----------------------------------------------------------
	
	/**
	 * Notification of a new screen capture.  This will be in openGL pixel order.
	 *
	 * @param buffer The screen capture
	 */
	public void screenCaptured(Buffer buffer, int width, int height) {

		image_width = width;
		image_height = height;
		
		ByteBuffer pixelsRGB = (ByteBuffer)buffer;
		String filename = nameField.getText();
		
		int[] pixelInts = new int[width * height];
		
		// Convert RGB bytes to ARGB ints with no transparency. 
		// Flip image vertically by reading the rows of pixels 
		// in the byte buffer in reverse - (0,0) is at bottom 
		// left in OpenGL.
		
		int p = width * height * 3; // Points to first byte (red) in each row.
		int q;                  	// Index into ByteBuffer
		int i = 0;                  // Index into target int[]
		int w3 = width*3;         	// Number of bytes in each row
		
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
		
		bufferedImage.setRGB(0, 0, width, height, pixelInts, 0, width);
		
		boolean valid = false;
		
		try {
	        // look to see if we need to validate the data
	        PropertyValidator validator = new FileNameValidator();
	        
	        if(validator != null) {
	        	
	        	valid = validator.validate(filename);
	        	if(!valid) {
	                JOptionPane.showMessageDialog(
	                        this,
	                        validator.getMessage(),
	                        "Data Validation Error",
	                        JOptionPane.OK_OPTION);
	        	} else {
	        		
	    			File outputFile = new File(workDir, filename +".png");
	    			ImageIO.write(bufferedImage, "PNG", outputFile);
	    			
	    			// create icons of the terrain image
	    			for (int j = 0; j < ICON_SIZE.length; j++) {
	    				int size = ICON_SIZE[j];
	    				outputFile = 
	    					new File(workDir, filename + "_"+ size +"x"+ size +".png");
	    				BufferedImage iconImage = 
	    					scaleBufferedImage(bufferedImage, size, size);
	    				ImageIO.write(iconImage, "PNG", outputFile);
	    			}
	        	}
	        }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized(this) {
			if(valid) {
				captureInProgress = false;
				notify();
			}
		}
	}
	
	//---------------------------------------------------------
	// Local Methods
	//---------------------------------------------------------
	
	/**
	 * Shutdown and clean up the x3d browser component
	 */
	public void shutdown() {
		browser.dispose( );
		x3dComponent.shutdown( );
	}
	
	/**
	 * Create the data files associated with a new Location
	 */
	private void createLocation() {

		float[] position = new float[3];
		ps.getPosition(position);
		double distance = Math.sqrt(
			position[0] * position[0] + 
			position[1] * position[1] + 
			position[2] * position[2]);

		Length meter = new Length(1, LengthUnit.METER);
		Cartesian cartesian = new Cartesian(
			-(double)position[2], 
			(double)position[1], 
			(double)position[0], 
			meter);
		Ellipsoid ellipsoid = new Earth().getEllipsoid();
		Ellipsoidal e = new Ellipsoidal(cartesian, ellipsoid);
		
		double elevation = e.distance.getMagnitude(LengthUnit.METER);
		double dim_meters = elevation * Math.tan(Math.PI/8);
		
		CatalogManager cm = DefaultCatalogManager.getCatalogManager();
		Catalog c = cm.getCatalog(catalogName);
		ToolGroup tg = c.getToolGroup(toolGroupName);
		
		String name = nameField.getText();
		File file = new File(workDir, name + ".png");
		URL url = null;
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException mue) {
		}
		
		// create the projection file
		File prjFile = new File(workDir, name + ".prj");
		try {
			BufferedWriter writer = 
				new BufferedWriter(new FileWriter(prjFile));
			writer.write(PROJECTION, 0, PROJECTION.length());
			writer.flush();
			writer.close();
		} catch (IOException ioe) {
			System.out.println( 
				": Exception writing projection file: "+ 
				prjFile +": "+ ioe.getMessage());
		}
		
		// generate the world file parameters
		
		double scale_x = (2 * dim_meters) / image_width;
        double scale_x2 = scale_x / 2;
        double scale_y = (2 * dim_meters) / image_height;
        double scale_y2 = scale_y / 2;
		////////////////////////////////////////////////////////////////////
		// moving the origin from the center of the image
		// to the lower left hand corner
        //double ulx = -dim_meters + scale_x2;
        //double uly = dim_meters - scale_y2;
		double ulx = scale_x2;
		double uly = (2 * dim_meters) - scale_y2;
		////////////////////////////////////////////////////////////////////
		
		StringBuilder wldBuffer = new StringBuilder( );
		wldBuffer.append( Double.toString( scale_x ) + " \n" );
		wldBuffer.append( Double.toString( 0 ) + " \n" );
		wldBuffer.append( Double.toString( 0 ) + " \n" );
		wldBuffer.append( Double.toString( -scale_y ) + " \n" );
		wldBuffer.append( Double.toString( ulx ) + " \n" );
		wldBuffer.append( Double.toString( uly ) + " \n" );
		String worldString = wldBuffer.toString( );

		File wldFile = new File( workDir, name + ".wld" );
		try {
			BufferedWriter writer = 
				new BufferedWriter( new FileWriter( wldFile ) );
			writer.write( worldString, 0, worldString.length( ) );
			writer.flush( );
			writer.close( );
		} catch ( IOException ioe ) {
			System.out.println( 
				": Exception writing world file: "+ 
				wldFile +": "+ ioe.getMessage( ) );
		}
		
		// gin up the icon image urls
        String[] iconURLs = new String[ICON_SIZE.length];
		for (int i = 0; i < ICON_SIZE.length; i++) {
			int size = ICON_SIZE[i];
			File iconFile = 
				new File(workDir, name + "_"+ size +"x"+ size +".png");
			try {
				iconURLs[i] = iconFile.toURI().toURL().toExternalForm();
			} catch (MalformedURLException mue) {
			}
		}

		// create the 'Tool'
		HashMap<String, Object> entityProperties = 
			new HashMap<String, Object>();
		SimpleTool tool = new SimpleTool(
		        name, 
    			name, 
    			url.toExternalForm(), 
    			iconURLs, 
    			Entity.TYPE_WORLD,
    			url.toExternalForm(),
    			name, 
    			new float[] {(float)dim_meters * 2.0f, 0f, (float)dim_meters * 2.0f},
    			new float[] {1f, 1f, 1f},
    			MultiplicityConstraint.SINGLETON, 
    			"World", 
    			false, 
    			false, 
    			false, 
    			false, 
    			entityProperties);
		
		// add the Tool to the "Locations' ToolGroup
		tg.addTool(tool);
	}
	
	/**
	 * Create the X3D browser
	 */
	private void createBrowser(String worldURL) {
		
		HashMap params = new HashMap();
		params.put("Xj3D_ShowConsole", Boolean.FALSE);
		params.put("Xj3D_NavbarShown", Boolean.TRUE);
		params.put("Xj3D_StatusBarShown", Boolean.TRUE);
		params.put("Xj3D_FPSShown", Boolean.TRUE);
		params.put("Xj3D_NavbarPosition", "bottom");
		params.put( "Xj3D_LocationShown", Boolean.FALSE );
		params.put( "Xj3D_LocationPosition", "top" );
		
		x3dComponent = BrowserFactory.createX3DComponent(params);
		browser = x3dComponent.getBrowser();
		
		((Xj3DBrowser)browser).setMinimumFrameInterval(33);
        
		try {
			scene = browser.createX3DFromURL(new String[]{worldURL});
		} catch (Exception e) {
			System.out.println( 
				": Exception reading X3D from: "+ 
				worldURL +": "+ e.getMessage( ) );
		}
		
		// add a prox sensor so we can work out the elevation of
		// the snapshot over the terrain
		ps = (ProximitySensor)scene.createNode("ProximitySensor");
		ps.setSize(new float[]{1E18f, 1E18f, 1E18f});
		scene.addRootNode(ps);
		
		browser.replaceWorld(scene);
	}
	    
    /**
     * Scale a BufferedImage.
     *
     * @param image The original image to scale
     * @param newWidth The new width
     * @param newHeight The new height
     */
    private BufferedImage scaleBufferedImage(
        BufferedImage image,
        int newWidth,
        int newHeight) {
        
        java.awt.Image rimg = image.getScaledInstance(
            newWidth, 
            newHeight, 
            java.awt.Image.SCALE_SMOOTH);
        
        boolean hasAlpha = image.getColorModel().hasAlpha();
        
        BufferedImage ret_image = null;
        if (hasAlpha) {
            ret_image = new BufferedImage(
				newWidth, 
				newHeight, 
				BufferedImage.TYPE_INT_ARGB);
        } else {
            ret_image = new BufferedImage(
				newWidth, 
				newHeight, 
				BufferedImage.TYPE_INT_RGB);
        }
        java.awt.Graphics2D g2 = ret_image.createGraphics();
        g2.drawImage(rimg, 0, 0, null);
        
        return(ret_image);
    }
}

