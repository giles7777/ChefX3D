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
import javax.vecmath.Matrix4f;

import org.j3d.aviatrix3d.ViewEnvironment;

// Local Imports
// None

/**
 * Defines the requirements for receiving status on navigation events
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public interface NavigationStatusListener {

    /**
     * Notification that the view Transform has changed
     *
     * @param mtx The new view Transform matrix
     */
    public void viewMatrixChanged(Matrix4f mtx);

    /**
     * Notification that the orthographic viewport size has changed and
     * this is the new frustum details.
     *
     * @param frustumCoords The new coordinates to use in world space
     */
    public void viewportSizeChanged(double[] frustumCoords);

}
