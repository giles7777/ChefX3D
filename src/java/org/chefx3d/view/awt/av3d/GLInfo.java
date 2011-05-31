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
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;

import java.util.StringTokenizer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;

import javax.swing.JDialog;

// Local imports
// none

/**
 * A class for determining GL properties and capabilities.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public class GLInfo extends JDialog implements 
	GLEventListener, 
	GLCapabilitiesChooser, 
	Runnable {
	
	/** The default dimension */
	private static final int DIM = 2;
	
	/** Max samples detected */
	private int maxSamples = 1;
	
	/** GL info strings */
	private String gl_version;
	private String gl_vendor;
	private String gl_renderer;
	private String[] gl_extensions;
	
	/** The chosen caps */
	private GLCapabilities gl_caps;
	
	/** Flag indicating that the chooser has not completed */
	private boolean inProgress;
	
	/** The gl object */
	private GLCanvas canvas;
		
	/**
	 * Constructor
	 */
    public GLInfo() {
        super((Frame)null, true);
		setUndecorated(true);
        
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        
		inProgress = true;
		
		GLCapabilities caps = new GLCapabilities();
		GLCapabilitiesChooser chooser = this;
		caps.setSampleBuffers(true);
		
		canvas = new GLCanvas(caps, chooser, (GLContext)null, (GraphicsDevice)null);
		canvas.addGLEventListener(this);
		
		contentPane.add(canvas, BorderLayout.CENTER);
		
        setSize(DIM, DIM);
		Dimension screenSize = getToolkit().getScreenSize();
		setLocation((screenSize.width - DIM), (screenSize.height - DIM));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Return the Version GL String
	 *
	 * @return the Version GL String
	 */
	public String getVersion() {
		return(gl_version);
	}
	
	/**
	 * Return the Vendor GL String
	 *
	 * @return the Vendor GL String
	 */
	public String getVendor() {
		return(gl_vendor);
	}
	
	/**
	 * Return the Renderer GL String
	 *
	 * @return the Renderer GL String
	 */
	public String getRenderer() {
		return(gl_renderer);
	}
	
	/**
	 * Return the Extension GL Strings
	 *
	 * @return the Extension GL Strings
	 */
	public String[] getExtensions() {
		return(gl_extensions);
	}
	
	/**
	 * Return the default GLCapabilities
	 *
	 * @return the default GLCapabilities
	 */
	public GLCapabilities getGLCapabilities() {
		return(gl_caps);
	}
	
	/**
	 * Return the max samples supported by the set of available capabilities.
	 * If sample buffers are unsupported, one (1) will be returned.
	 *
	 * @return The max number of sample buffers supported.
	 */
	public int getMaximumNumSamples() {
		return(maxSamples);
	}
	
    //----------------------------------------------------------
    // Methods defined by Runnable
    //----------------------------------------------------------

	/**
	 * Exit the dialog if complete
	 */
	public void run() {
		if (!inProgress) {
			this.dispose();
		} else {
			EventQueue.invokeLater(this);
		}
	}
	
    //----------------------------------------------------------
    // Methods defined by GLEventListener
    //----------------------------------------------------------

	/** Ignored */
    public void display(GLAutoDrawable drawable) {
		//System.out.println("display");
	}
	
	/** Ignored */
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		//System.out.println("displayChanged");
	}
	
	/** Capture the GL Strings at the earliest possible opportunity */
 	public void init(GLAutoDrawable drawable) {
		//System.out.println("init");
		
		GL gl = drawable.getGL();
		
        gl_version = gl.glGetString(GL.GL_VERSION);
        gl_vendor = gl.glGetString(GL.GL_VENDOR);
        gl_renderer = gl.glGetString(GL.GL_RENDERER);
		
		String extensions = gl.glGetString(GL.GL_EXTENSIONS);
		StringTokenizer st = new StringTokenizer(extensions);
		int num_ext = st.countTokens();
		gl_extensions = new String[num_ext];
		for (int i = 0; i < num_ext; i++) {
			gl_extensions[i] = st.nextToken();
		}
		gl_caps = canvas.getChosenGLCapabilities();
		
		EventQueue.invokeLater(this);
	}
	
	/** Ignored */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		//System.out.println("reshape");
	}
	
    //----------------------------------------------------------
    // Methods defined by GLCapabilitiesChooser
    //----------------------------------------------------------

	/**
	 * Determine the max samples supported by the set of
	 * available capabilities and then return a selection.
	 */
	public int chooseCapabilities(
		GLCapabilities desired,
		GLCapabilities[] available,
		int windowSystemRecommendedChoice) {
		
		for (int i = 0; i < available.length; i++) {
			GLCapabilities caps = available[i];
			
			if ((caps != null) && (caps.getNumSamples() > maxSamples)) {
				maxSamples = caps.getNumSamples();
			}
		}
		int selection = 0;
		if ((windowSystemRecommendedChoice > 0) &&
			(windowSystemRecommendedChoice < available.length)) {
				selection = windowSystemRecommendedChoice;
		}
		
		inProgress = false;
		return(selection);
	}
}
