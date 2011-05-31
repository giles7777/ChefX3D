/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.util.StringTokenizer;

import org.j3d.util.I18nManager;

// Local imports
// none

/**
 * A class for validating the compatibility of GL properties and capabilities
 * with the requirements of the editor and preview views.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public class GLValidator {
	
	/** Invalid GL Version text */
	private static final String INSUFFICIENT_GL_VERSION_MSG_PROP = 
		"org.chefx3d.messaging.PopUpFatalError.insufficientGLVersion";
	
	/** Minimum GL Version text */
	private static final String MINIMUM_GL_VERSION_MSG_PROP = 
		"org.chefx3d.messaging.PopUpFatalError.minimumGLVersion";
	
	/** The minimum acceptable GL Version in String form */
	private static final String MINIMUM_GL_VERSION_STRING = "1.2.0";
	
	/** Translation utility */
	private I18nManager intl_mgr;
	
	/** GLInfo object */
	private GLInfo glinfo;
	
	/** Version valid flag */
	private boolean versionIsValid;
	
	/** The composite error message */
	private String errorMessage;
	
	/**
	 * Constructor
	 *
	 * @param glinfo The working GLInfo object
	 */
	public GLValidator(GLInfo glinfo) {
		
		this.glinfo = glinfo;
		intl_mgr = I18nManager.getManager();
		
		String gl_version = glinfo.getVersion();
		int[] current = parseVersion(gl_version);
		int[] minimum = parseVersion(MINIMUM_GL_VERSION_STRING);
		
		versionIsValid = true;
		for (int i = 0; i < minimum.length; i++) {
			if (i < current.length) {
				if (minimum[i] < current[i]) {
					// we're good
					break;
				} else if (minimum[i] > current[i]) {
					versionIsValid = false;
					break;
				}
				// otherwise, if they are equal, continue to check the next field
			} else {
				if (minimum[i] > 0) {
					versionIsValid = false;
					break;
				}
			}
		}
		if (!versionIsValid) {
			errorMessage = 
				intl_mgr.getString(INSUFFICIENT_GL_VERSION_MSG_PROP) +
				": "+ gl_version +"\n"+
				intl_mgr.getString(MINIMUM_GL_VERSION_MSG_PROP) +
				": "+ MINIMUM_GL_VERSION_STRING +"\n";
		}
	}
	
	//---------------------------------------------------------------
	// Local Methods
	//---------------------------------------------------------------
	
	/**
	 * Return the validation state
	 *
	 * @return The validation state
	 */
	public boolean isValid() {
		return(versionIsValid);
	}
	
	/**
	 * Return the error message
	 *
	 * @return The error message
	 */
	public String getErrorMessage() {
		return(errorMessage);
	}
	
    /**
     * Parse a string containing a GLVersion string
     *
     * @param gl_version The input string to parse
     * @return An int array of length 3 with the parsed numbers
     */
    private int[] parseVersion(String gl_version) {
		StringTokenizer st = new StringTokenizer(gl_version, ".");
		int num_token = st.countTokens();
        int[] ret_val = new int[num_token];

        for (int i = 0; i < num_token; i++) {
			String s = st.nextToken();
			try {
				ret_val[i] = Integer.parseInt(s);
			} catch (NumberFormatException nfe) {
				break;
			}
		}
        return ret_val;
    }
}
