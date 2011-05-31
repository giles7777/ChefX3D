/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view;

// External imports
import java.util.ArrayList;

/**
 * Notification about thumbnail loading.
 * 
 * @author Sang Park
 * @version $Revision: 1.1 $
 */
public interface ThumbnailListener {
	
	/**
	 * Notifies the listener that thumbnail generation has been completed.
	 * @param thumnails Lists of thumbnail files names.
	 */
    public void thumbnailCreated(ArrayList<String> thumnails);
}
