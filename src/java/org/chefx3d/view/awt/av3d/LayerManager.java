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
import org.j3d.aviatrix3d.Layer;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

// Local imports
// None

/**
 * Defines the requirements for implementing an AV3DView managed layer
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public interface LayerManager {

    /**
     * Return the Layer id
     *
     * @return The Layer id
     */
    public int getId();

    /**
     * Return the Layer object
     *
     * @return The Layer object
     */
    public Layer getLayer();

    /**
     * Return the BoundingVolume of the content in this Layer
     *
     * @return The BoundingVolume
     */
    public BoundingVolume getBounds();

    /**
     * Fetch the transform that holds the current viewpoint
     * information.
     *
     * @return Tranform node with a viewpoint under it
     */
    public TransformGroup getViewpointTransform();

    /**
     * Get the view environment used to render the scene with.
     *
     * @return The current configured view environment
     */
    public ViewEnvironment getViewEnvironment();
}
