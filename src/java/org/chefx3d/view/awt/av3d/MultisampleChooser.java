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
import java.awt.*;

import javax.media.opengl.*;

/**
 * A class for determining the maximum antialiasing possible.
 *
 * @author Alan Hudson
 * @version $Revision: 1.1 $
 */
public class MultisampleChooser extends DefaultGLCapabilitiesChooser {
	
	/** Max samples field, modified by MultiSampleChooser. */
	private static int maxSamples = 1;
	
	/** Flag indicating that the chooser is active */
	private static boolean inProgress;
	
    //----------------------------------------------------------
    // Methods defined by GLCapabilitiesChooser
    //----------------------------------------------------------

	/**
	 * Determine the max samples supported by the set of available capabilities
	 * and then return the super class's selection.
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
		int selection = super.chooseCapabilities(desired, available, windowSystemRecommendedChoice);
		
		inProgress = false;
		return(selection);
	}
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Return the max samples supported by the set of available capabilities.
	 * If sample buffers are unsupported, one (1) will be returned.
	 *
	 * @return The max number of sample buffers supported.
	 */
	public static int getMaximumNumSamples() {
		
		GLCapabilities caps = new GLCapabilities();
		GLCapabilitiesChooser chooser = new MultisampleChooser();
		caps.setSampleBuffers(true);
		
		inProgress = true;
		Canvas canvas = new GLCanvas(caps, chooser, (GLContext)null, (GraphicsDevice)null);
		Frame frame = new Frame();
		frame.setUndecorated(true);
		canvas.setSize(16, 16);
		frame.add(canvas, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		
		while(inProgress) {
			try {
				Thread.sleep(50);
			} catch(Exception e) {
			}
		}
		
		frame.setVisible(false);
		frame.dispose();
		
		return(maxSamples);
	}
}