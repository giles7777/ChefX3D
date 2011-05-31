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

package org.chefx3d.view.common;

// External Imports
// None

// Local Imports
// None

/**
 * Constants used by an editor.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public interface EditorConstants {
	
	/** Identifiers of editing anchor positions */
    public static enum AnchorData {
		NONE, 
		NORTH, 
		SOUTH, 
		EAST, 
		WEST, 
		NORTHWEST, 
		NORTHEAST, 
		SOUTHWEST, 
		SOUTHEAST, 
		DELETE, 
		ROTATE,
	};
}
