/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
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
 * Interface implemented by any class that wants to be given a tick every frame
 * to do some form of processing.
 * <p>
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.1 $
 */
public interface PerFrameObserver {

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame();
}
