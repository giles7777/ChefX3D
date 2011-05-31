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

package org.chefx3d.actions.awt;

// External Imports
// none

// Local Imports
// none

/**
 * Listen for clip board content changes
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public interface ClipboardListener {
	
    /**
     * Notify listeners of a status change to the clip board contents
     * 
     * @param hasEntities true if contents, false otherwise
     */
    public void clipboardUpdated(boolean hasEntities);
	
}
