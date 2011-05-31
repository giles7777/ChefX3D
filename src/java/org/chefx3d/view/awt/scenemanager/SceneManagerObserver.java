/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.scenemanager;

// External imports
import java.util.ArrayList;
import java.util.PriorityQueue;

import org.j3d.aviatrix3d.ApplicationUpdateObserver;
import org.j3d.aviatrix3d.Geometry;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.SceneGraphObject;

import org.j3d.renderer.aviatrix3d.util.PerformanceMonitor;
import org.j3d.renderer.aviatrix3d.util.SystemPerformanceListener;

// Local imports
import org.chefx3d.model.CommandController;
import org.chefx3d.model.BufferedCommandController;
import org.chefx3d.view.awt.SceneEffectsRequest;


/**
 * Manager for controlling command processing and scene graph updates.
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.25 $
 */
public class SceneManagerObserver implements ApplicationUpdateObserver {

    /** The update interval for performance monitoring. In milliseconds */
    private static final int PERF_UPDATE_INTERVAL = 5000;

    /** Device manager */
    private DeviceManager deviceManager;

    /** List of commands that have been buffered */
    private CommandController bufferedCommands;

    /**
     * List of objects that need to be scheduled for bounds changed callbacks
     * during the next frame.
     */
    private ArrayList<NodeData> boundsUpdateRequests;

    /**
     * List of objects that need to be scheduled for data changed callbacks
     * during the next frame.
     */
    private ArrayList<NodeData> dataUpdateRequests;

    /** Things that need to be called every frame */
    private ArrayList<PerFrameUIObserver> frameUIObservers;

    /** Things that need to be called every frame */
    private ArrayList<PerFrameObserver> frameObservers;

    /** Things that need to be called every frame */
    private PerformanceMonitor perfMon;

    /** Scene effects request, used to set and remove
     * the RenderEffectsProcessor of a scene.     */
    private SceneEffectsRequest sceneEffectsRequest;

    /** TRUE if we should add a RenderEffectsProcessor (via the
     * addEffectsRequest variable) next time through the
     * updateSceneGraph() method)     */
    private boolean addRenderEffects;

    /** TRUE if we should remove a RenderEffectsProcessor (via the
     * removeEffectsRequest variable) next time through the
     * updateSceneGraph() method)     */
    private boolean removeRenderEffects;

    /**
     * Constructor
     *
     * @param renderManager The SceneManager instance
     * @param deviceManager The input device handler
     * @param bufferedCommands The Command queue
     */
    public SceneManagerObserver(
            DeviceManager deviceManager,
            CommandController bufferedCommands) {

        this.deviceManager = deviceManager;
        this.bufferedCommands = bufferedCommands;

        frameObservers = new ArrayList<PerFrameObserver>();
        frameUIObservers = new ArrayList<PerFrameUIObserver>();

        addRenderEffects = false;
        removeRenderEffects = false;

        perfMon = new PerformanceMonitor();
        perfMon.setUpdateInterval(PERF_UPDATE_INTERVAL);
    }

    //---------------------------------------------------------------
    // Methods defined by ApplicationUpdateObserver
    //---------------------------------------------------------------

    /**
     * Notification that now is a good time to update the scene graph.
     */
    public void updateSceneGraph() {

        perfMon.updateMetrics();

        // do UI processing first
        deviceManager.beginTrackerProcessing();
        for(int i = 0; i < frameUIObservers.size(); i++) {
            PerFrameUIObserver obs = frameUIObservers.get(i);
            obs.processNextFrameUI();
        }
        deviceManager.endTrackerProcessing();

        // execute the buffered model commands
        if (bufferedCommands != null) {
            ((BufferedCommandController)bufferedCommands).processCommands();
        }

        // notify frames to process
        for(int i = 0; i < frameObservers.size(); i++) {
            PerFrameObserver obs = frameObservers.get(i);
            obs.processNextFrame();
        }

        //
        // use the SceneEffectsRequest to remove the
        // renderEffectsProcessor from the scene.
        //
        if(removeRenderEffects){
            sceneEffectsRequest.removeRenderEffectsProcessor();
            removeRenderEffects = false;
        }


        if(addRenderEffects) {
            //ArrayList<SceneEffectsRequest> l = sceneEffectsRequest;
            //sceneEffectsRequest = null;
            //for (SceneEffectsRequest ser : l) {
            //      ser.setRenderEffectsProcessor();
            //}
            sceneEffectsRequest.setRenderEffectsProcessor();
            addRenderEffects = false;
        }

        // Take a local reference and then delete the global reference. Done to
        // avoid multithreading issues of having a load request come in
        // asynchronously as we're doing this and having the clear method delete
        // it before it is processed here.
        if(boundsUpdateRequests != null) {
            ArrayList<NodeData> l = boundsUpdateRequests;
            boundsUpdateRequests = null;

            for(int i = 0; i < l.size(); i++) {
                NodeData data = l.get(i);
                if (data.node != null) {
                    Node node = data.node;
                    NodeUpdateListener nul = data.listener;
                    if (node.isLive()) {
                        node.boundsChanged(nul);
                    }
                } else if (data.geom != null) {
                    Geometry geom = data.geom;
                    NodeUpdateListener nul = data.listener;
                    if (geom.isLive()) {
                        geom.boundsChanged(nul);
                    }
                }
            }
        }

        // Take a local reference and then delete the global reference. Done to
        // avoid multithreading issues of having a load request come in
        // asynchronously as we're doing this and having the clear method delete
        // it before it is processed here.
        if(dataUpdateRequests != null) {
            ArrayList<NodeData> l = dataUpdateRequests;
            dataUpdateRequests = null;

            for(int i = 0; i < l.size(); i++) {
                NodeData data = l.get(i);
                SceneGraphObject sgo = data.data;
                NodeUpdateListener nul = data.listener;
                if (sgo.isLive()) {
                    sgo.dataChanged(nul);
                }
            }
        }
    }

    /**
     * Notification that the AV3D internal shutdown handler has detected a
     * system-wide shutdown. The aviatrix code has already terminated rendering
     * at the point this method is called, only the user's system code needs to
     * terminate before exiting here.
     */
    public void appShutdown() {
        // do nothing
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Get the command controller that this scene manager is using.
     *
     * @return The scene manager that was given to us in the constructor
     */
    public CommandController getCommandController() {
        return bufferedCommands;
    }

    /**
     * Place a new object onto the data update request list.
     *
     * @param d The scene graph object to be updated
     * @param l The listener to be called when this object is to be updated
     */
    public void requestDataUpdate(SceneGraphObject d, NodeUpdateListener l) {
        if (!d.isLive()) {
            l.updateNodeDataChanges(d);
        } else {

            NodeData data = new NodeData();
            data.data = d;
            data.listener = l;

            if(dataUpdateRequests == null)
                dataUpdateRequests = new ArrayList<NodeData>();
            dataUpdateRequests.add(data);
        }
    }

    /**
     * Place a new object onto the bounds update request list.
     *
     * @param d The scene graph object to be updated
     * @param l The listener to be called when this object is to be updated
     */
    public void requestBoundsUpdate(Node n, NodeUpdateListener l) {
        if(!n.isLive()) {
            l.updateNodeBoundsChanges(n);
        } else {

            NodeData data = new NodeData();
            data.node = n;
            data.listener = l;

            if(boundsUpdateRequests == null)
                boundsUpdateRequests = new ArrayList<NodeData>();
            boundsUpdateRequests.add(data);
        }
    }

    /**
     * Place a new object onto the bounds update request list.
     *
     * @param d The scene graph object to be updated
     * @param l The listener to be called when this object is to be updated
     */
    public void requestBoundsUpdate(Geometry g, NodeUpdateListener l) {
        if (!g.isLive()) {
            l.updateNodeBoundsChanges(g);
        } else {

            if(boundsUpdateRequests == null)
                boundsUpdateRequests = new ArrayList<NodeData>();

            NodeData data = new NodeData();
            data.geom = g;
            data.listener = l;

            boundsUpdateRequests.add(data);
        }
    }

    /**
     * Add a per-frame observer to the list to be processed. Duplicate entries
     * are ignored.
     *
     * @param obs The new observer instance to be handled
     */
    public void addObserver(PerFrameObserver obs) {
        if(obs != null && !frameObservers.contains(obs))
            frameObservers.add(obs);
    }

    /**
     * Remove the given observer from the current processing list. If it is not
     * currently added, it is silently ignored.
     *
     * @param obs The observer instance to be removed
     */
    public void removeObserver(PerFrameObserver obs) {
        frameObservers.remove(obs);
    }

    /**
     * Add a per-frame UI observer to the list to be processed. Duplicate entries
     * are ignored.
     *
     * @param obs The new observer instance to be handled
     */
    public void addUIObserver(PerFrameUIObserver obs) {
        if(obs != null && !frameUIObservers.contains(obs))
            frameUIObservers.add(obs);
    }

    /**
     * Remove the given UI observer from the current processing list. If it is not
     * currently added, it is silently ignored.
     *
     * @param obs The observer instance to be removed
     */
    public void removeUIObserver(PerFrameUIObserver obs) {
        frameUIObservers.remove(obs);
    }

    /**
     * Add a system performance listener to the list to be processed. Duplicate
     * entries are ignored. A higher number for the priority increases its
     * chances of being called before the other listeners for controlling
     * performance.
     *
     * @param l The new listener instance to be handled
     */
    public synchronized void addPerformanceListener(SystemPerformanceListener l,
                                                    int priority) {
        perfMon.addPerformanceListener(l, priority);
    }

    /**
     * Remove a system performance listener from the current processing list.
     * If it is not currently added, it is silently ignored.
     *
     * @param l The listener instance to be removed
     */
    public void removePerformanceListener(SystemPerformanceListener l) {
        perfMon.removePerformanceListener(l);
    }

    /**
     * Get the registered device manager for the system.
     *
     * @return The current device manager instance
     */
    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    /**
     * The next time through the ApplicationUpdateObserver's primary loop
     * [method updateSceneGraph()], set the scene's RenderEffectProcessor
     * via a SceneEffectsRequest call.
     * @param request
     */
    public void addSceneEffectsRequest(SceneEffectsRequest request) {
        if (request != null) {
            sceneEffectsRequest = request;
            addRenderEffects = true;
        }
    }

    /**
     * Must be called AFTER setting a non-null sceneEffectsRequest with the
     * {@link #addSceneEffectsRequest(SceneEffectsRequest)} method to have
     * any effect.
     * The next time through the ApplicationUpdateObserver's primary loop
     * [method updateSceneGraph()], set the scene's RenderEffectProcessor
     * to NULL via a SceneEffectsRequest call.
     * @param request
     */
    public void removeSceneEffectsRequest(){
        if(sceneEffectsRequest == null)
            return;
        removeRenderEffects = true;
    }
}
