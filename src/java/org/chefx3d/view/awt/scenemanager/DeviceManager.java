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

// External Imports
import org.j3d.aviatrix3d.pipeline.graphics.GraphicsOutputDevice;

// Local imports
import org.chefx3d.util.ErrorReporter;

/**
 * Defines the requirements for handling UI devices.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public interface DeviceManager {

    /**
     * Add a new component to track for devices. Useful for combining
     * multiple surfaces under a single manager.
     *
     * @param surface The surface to track
     * @param uih The handler to associate with this surface
     */
    public void addTrackedSurface(GraphicsOutputDevice surface,
                                  UserInputHandler uih);

    /**
     * Register an error reporter with the engine so that any errors generated
     * by the loading of script code can be reported in a nice, pretty fashion.
     * Setting a value of null will clear the currently set reporter. If one
     * is already set, the new value replaces the old.
     *
     * @param reporter The instance to use or null
     */
    public void setErrorReporter(ErrorReporter reporter);

    /**
     * Notification that tracker processing is starting for a frame.
     */
    public void beginTrackerProcessing();

    /**
     * Notification that tracker processing is ending for a frame.
     */
    public void endTrackerProcessing();

    /**
     * Processes input from sensors and issues commands to the UserInputHandler.
     *
     * @param layerId The layer that we're reading from
     * @param uiHandler The uiHandler for this layer
     */
    public void processTrackers(int layerId, UserInputHandler uiHandler);
}
