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
import org.j3d.device.input.*;

import org.j3d.renderer.aviatrix3d.device.input.mouse.MouseDevice;

import org.j3d.aviatrix3d.pipeline.graphics.GraphicsOutputDevice;
import org.j3d.device.input.TrackerState;

// Local imports
// None

/**
 * Data holder class used by the DefaultNavigationHandler to hold
 * everything about a surface and the devices it contains.
 * <p>
 *
 * Current definition is very limited and assumes a single device per
 * surface.
 *
 * @author Justin Couch
 * @version $Revision: 1.2 $
 */
class DeviceInfo {

    /** The surface we came from */
    GraphicsOutputDevice outputDevice;

    /** The device that we are managing for this surface */
    TrackerDevice device;

    /** Trackers managed by this class.  Unrolled from device for speed */
    Tracker[] trackers;
}
