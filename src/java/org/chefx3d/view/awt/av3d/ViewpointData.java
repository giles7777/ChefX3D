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
import org.j3d.aviatrix3d.Viewpoint;
import org.j3d.aviatrix3d.TransformGroup;

// Local Imports
// None

/**
 * Container class for Viewpoint objects
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public class ViewpointData {

    /** The identifier of the Viewpoint */
    public final String name;

    /** The Viewpoint's TransformGroup */
    public final TransformGroup viewpointTransform;

    /**
     * Constructor
     */
    public ViewpointData(String name,
        TransformGroup viewpointTransform) {

        this.name = name;
        this.viewpointTransform = viewpointTransform;
    }
}
