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

// External Imports
// Local Imports

/**
 * Singleton container for shared editor state variables
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
class LocationEditorState implements AV3DConstants {
	
	/** The class instance */
	private static LocationEditorState instance;
	
    /** Current mode for user input */
    EditorMode currentMode;

    /** Previous mode for user input */
    EditorMode previousMode;

    /** The anchor that the mouse is currently over, or null */
    AV3DAnchorInformation mouseOverAnchor;

	/**
	 * Restricted Constructor
	 */
	private LocationEditorState() {
        currentMode = EditorMode.INACTIVE;
	}

	/**
	 * Return the class instance
	 *
	 * @return The class instance
	 */
	static LocationEditorState getInstance() {
		if (instance == null) {
			instance = new LocationEditorState();
		}
		return(instance);
	}
}
