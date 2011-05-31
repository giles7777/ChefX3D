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
import org.j3d.aviatrix3d.Node;

// Internal imports
// none

/**
 * Listen for load status.
 *
 * @author Alan Hudson
 * @version
 */
interface LoadListener {
    /**
     * Notification that the model has finished loading.
     *
     * @param nodes The nodes loaded
     */
    public void modelLoaded(Node[] nodes);
}