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

import java.awt.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.BitSet;


import org.j3d.aviatrix3d.pipeline.graphics.GraphicsOutputDevice;
import org.j3d.device.input.TrackerState;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.DeviceManager;
import org.chefx3d.view.awt.scenemanager.UserInputHandler;

/**
 * Sits between input devices and the UserInputHandler.
 * Determines which one has control of navigation and picking.
 * Maintains picking state.
 * <p>
 *
 * KeySensor handling is performed separately by the KeyDeviceSensorManager
 * <p>
 * Each sensor will have a current active touch, drag and anchor sensor.
 * It cannot activate another till that's cleared.
 *
 * @author Rex Melton, Justin Couch
 * @version $Revision: 1.6 $
 */
public class DefaultDeviceManager implements DeviceManager {

    /** Map of the user input handler to the tracker list for it */
    private HashMap<UserInputHandler, DeviceInfo> handlerMap;

    /** A scratch tracker state value for performance */
    private TrackerState state;

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /** Current list of trackerDevices */
    private TrackerDevice[] trackerDevices;

    /** Trackers managed by this class.  Unrolled from trackerDevices for speed */
    private Tracker[] trackers;

    /**
     * Create a new device manager.
     */
    public DefaultDeviceManager() {

        handlerMap = new HashMap<UserInputHandler, DeviceInfo>();
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        state = new TrackerState();
    }

    //------------------------------------------------------------------------
    // Methods defined by DeviceManager
    //------------------------------------------------------------------------

    /**
     * Add a new component to track for devices. Useful for combining
     * multiple surfaces under a single manager. If the user input handler is
     * already registered, it will replace the previous surface with this
     * surface.
     *
     * @param surface The surface to track
     * @param uih The user input handler to associate with this surface
     */
    public void addTrackedSurface(GraphicsOutputDevice surface,
                                  UserInputHandler uih) {
        createMouseDevice(surface, uih);
    }

    /**
     * Register an error reporter with the engine so that any errors generated
     * by the loading of script code can be reported in a nice, pretty fashion.
     * Setting a value of null will clear the currently set reporter. If one
     * is already set, the new value replaces the old.
     *
     * @param reporter The instance to use or null
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Notification that tracker processing is starting for a frame.
     */
    public void beginTrackerProcessing() {
        int len = trackers.length;

        for(int i = 0; i < len; i++) {
            trackers[i].beginPolling();
        }
    }

    /**
     * Notification that tracker processing is ending for a frame.
     */
    public void endTrackerProcessing() {
        int len = trackers.length;

        for(int i = 0; i < len; i++) {
            trackers[i].endPolling();
        }
    }

    /**
     * Processes input from sensors and issues commands to the UserInputHandler.
     *
     * @param layerId The layer that we're reading from
     * @param uiHandler The uiHandler for this layer
     */
    public void processTrackers(int layerId, UserInputHandler uiHandler) {

        DeviceInfo info = handlerMap.get(uiHandler);

        if(info == null)
            return;

        for(int i = 0; i < info.trackers.length; i++) {
            info.trackers[i].getState(layerId, 0, state);

            switch(state.actionType) {
                case TrackerState.TYPE_BUTTON:
                    uiHandler.trackerButton(i,state);
                    break;
                case TrackerState.TYPE_PRESS:
                    uiHandler.trackerPressed(i,state);
                    break;
                case TrackerState.TYPE_RELEASE:
                    uiHandler.trackerReleased(i,state);
                    break;
                case TrackerState.TYPE_MOVE:
                    uiHandler.trackerMoved(i,state);
                    break;
                case TrackerState.TYPE_CLICK:
                    uiHandler.trackerClicked(i,state);
                    break;
                case TrackerState.TYPE_DRAG:
                    uiHandler.trackerDragged(i,state);
                    break;
                case TrackerState.TYPE_ORIENTATION:
                    uiHandler.trackerOrientation(i,state);
                    break;
                case TrackerState.TYPE_WHEEL:
                    uiHandler.trackerWheel(i,state);
                    break;
            }
        }
    }

    /**
     * Create a new mouse device for the given surface.
     *
     * @param surface The surface to track
     * @param uih The user input handler to associate with this surface
     */
    private void createMouseDevice(GraphicsOutputDevice surface,
                                   UserInputHandler uih) {

        DeviceInfo info = handlerMap.get(uih);

        if(info == null) {
            info = new DeviceInfo();
            handlerMap.put(uih, info);
        }

        MouseDevice md = new MouseDevice(surface, "Mouse");
        Component cmp = (Component)surface.getSurfaceObject();
        cmp.addMouseListener(md);
        cmp.addMouseMotionListener(md);
        cmp.addMouseWheelListener(md);

        info.outputDevice = surface;
        info.device = md;
        info.trackers = md.getTrackers();

        // Unroll the tracker list to the global var for performance
        if(trackers == null) {
            trackers = new Tracker[info.trackers.length];
            System.arraycopy(info.trackers,
                             0,
                             trackers,
                             0,
                             info.trackers.length);
        } else {
            int new_len = trackers.length + info.trackers.length;
            Tracker[] new_trackers = new Tracker[new_len];

            System.arraycopy(trackers,
                             0,
                             new_trackers,
                             0,
                             trackers.length);

            System.arraycopy(info.trackers,
                             0,
                             new_trackers,
                             trackers.length,
                             info.trackers.length);

            trackers = new_trackers;
        }
    }
}
