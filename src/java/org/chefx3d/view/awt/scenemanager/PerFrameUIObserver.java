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
package org.chefx3d.view.awt.scenemanager;

// External imports

// Local imports
// None

/**
 * Defines the requirements for a callback to process user input per frame
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public interface PerFrameUIObserver {

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrameUI();
}
