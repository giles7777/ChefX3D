/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2011
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
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Layer;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.SimpleLayer;
import org.j3d.aviatrix3d.SimpleScene;
import org.j3d.aviatrix3d.SimpleViewport;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.ViewEnvironment;
import org.j3d.aviatrix3d.Viewpoint;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

import org.j3d.aviatrix3d.pipeline.graphics.GraphicsResizeListener;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.view.awt.scenemanager.*;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Implementation of an overlay LayerManager for the Preview. Handles
 * the creation, add and remove of various labels and indicators 
 * during the process of taking snapshots of certain orthographic 
 * viewpoints.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
abstract class AbstractLegendLayerManager implements
	LayerManager,
    NavigationStatusListener,
    NodeUpdateListener,
    PerFrameObserver,
    GraphicsResizeListener,
	ConfigListener {

    /** Reporter instance for handing out errors */
    protected ErrorReporter errorReporter;

    /** The layer identifier and index */
    protected int id;

    /** The base layer */
    protected SimpleLayer layer;

    /** The Scene instance */
    protected SimpleScene scene;

    /** The Viewport instance */
    protected SimpleViewport viewport;

    /** The root grouping node */
    protected Group rootGroup;

    /** The initial Viewpoint */
    protected Viewpoint viewpoint;

    /** The Viewpoint's TransformGroup */
    protected TransformGroup viewpointTransform;

	/** The view Matrix */
    protected Matrix4f viewMatrix;
	
	/** Flag indicating that the viewpoint transform should be reconfigured */
	protected boolean configView;
	
    /** The world model */
    protected WorldModel model;

    /** The scene manager Observer*/
    protected SceneManagerObserver mgmtObserver;

    /** The navigation status manager */
    protected NavigationStatusManager navStatusManager;

    /** The active zone Entity */
    protected ZoneEntity activeZoneEntity;

    /** Scratch vecmath objects */
    protected Matrix4f mtx0;
    protected Vector3f translation;

    /** Viewport dimensions (in pixels) */
    protected int viewport_width;
    protected int viewport_height;

	/** Frustum dimensions (in meters) */
	protected double frustum_width;
    protected double frustum_height;
		
    /** Instance of hierarchy transformation calculator */
    protected TransformUtils tu;

    /** The manager of the entities to be handled */
    protected AV3DEntityManager entityManager;

    /** The map of entity wrappers */
    protected HashMap<Integer, AV3DEntityWrapper> wrapperMap;

    /** Our ViewEnvironment */
    protected ViewEnvironment viewEnv;

    /** view frustum values */
    protected double[] frustum;

    /** Flag indicating that the viewport has been resized */
    protected boolean resizeOccured;

    /**
     * Constructor
     *
     * @param id The layer id
     * @param dim The initial viewport dimensions in [x, y, width, height]
     * @param model The WorldModel
     * @param reporter The ErrorReporter instance to use or null
     * @param mgmtObserver The SceneManagerObserver
     * @param navStatusManager The NavigationStatusManager
     */
    AbstractLegendLayerManager(
        int id,
        int[] dim,
        WorldModel model,
        ErrorReporter reporter,
        SceneManagerObserver mgmtObserver,
        NavigationStatusManager navStatusManager ) {

		this.id = id;
		
        viewport_width = dim[2];
        viewport_height = dim[3];
		
        this.model = model;
		
        setErrorReporter(reporter);

        this.navStatusManager = navStatusManager;
        this.navStatusManager.addNavigationStatusListener(this);

        this.mgmtObserver = mgmtObserver;
        mgmtObserver.addObserver(this);

        tu = new TransformUtils();
        mtx0 = new Matrix4f();
        translation = new Vector3f();
        frustum = new double[6];

		initSceneGraph(dim);
    }

    //---------------------------------------------------------------
    // Methods defined by LayerManager
    //---------------------------------------------------------------

    /**
     * Return the Layer id
     *
     * @return The Layer id
     */
    public int getId() {
        return(id);
    }

    /**
     * Return the Layer object
     *
     * @return The Layer object
     */
    public Layer getLayer() {
        return(layer);
    }

    /**
     * Return the BoundingVolume of the content in this Layer
     *
     * @return The BoundingVolume
     */
    public BoundingVolume getBounds() {
        return(rootGroup.getBounds());
    }

    /**
     * Fetch the transform that holds the current viewpoint
     * information.
     *
     * @return Tranform node with a viewpoint under it
     */
    public TransformGroup getViewpointTransform() {
        return(viewpointTransform);
    }

    /**
     * Get the view environment used to render the scene with.
     *
     * @return The current configured view environment
     */
    public ViewEnvironment getViewEnvironment() {
        SimpleViewport viewport = (SimpleViewport)layer.getViewport();
        SimpleScene scene = (SimpleScene)viewport.getScene();

        return(scene.getViewEnvironment());
    }

    //----------------------------------------------------------
    // Methods defined by NavigationStatusListener
    //----------------------------------------------------------

    /**
     * Notification that the orthographic viewport size has changed.
     *
     * @param frustumCoords The new coordinates to use in world space
     */
    public void viewportSizeChanged(double[] frustumCoords) {

        // use the location layer's ortho params
        viewEnv.setOrthoParams(frustumCoords[0],
                               frustumCoords[1],
                               frustumCoords[2],
                               frustumCoords[3]);
		
		frustum[0] = frustumCoords[0];
        frustum[1] = frustumCoords[1];
        frustum[2] = frustumCoords[2];
        frustum[3] = frustumCoords[3];
		
		frustum_width  = frustum[1] - frustum[0];
        frustum_height = frustum[3] - frustum[2];
    }

    /**
     * Notification that the view Transform has changed
     *
     * @param mtx The new view Transform matrix
     */
    public void viewMatrixChanged(Matrix4f mtx) {
		
        if ((wrapperMap != null) && (activeZoneEntity != null)) {
            AV3DEntityWrapper zoneWrapper =
                wrapperMap.get(activeZoneEntity.getEntityID());

            if (zoneWrapper != null) {
				
                // convert the viewpoint from world to local coordinates
                tu.getLocalToVworld(zoneWrapper.transformGroup, mtx0);
                mtx0.invert();

				viewMatrix.mul(mtx0, mtx);
				
                // maintain a fixed distance from the overlay geometry
				viewMatrix.get(translation);
				translation.z = 10;
				
                viewMatrix.setIdentity();
                viewMatrix.setTranslation(translation);

                configView = true;
            }
        }
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {
        if (configView) {
            mgmtObserver.requestBoundsUpdate(viewpointTransform, this);
        }
		if (resizeOccured) {
			viewport.setDimensions(0, 0, viewport_width, viewport_height);
			resizeOccured = false;
		}
    }

    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {

        if ((src == viewpointTransform) && configView){
			
            viewpointTransform.setTransform(viewMatrix);
            configView = false;
				
        }
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
    }

    //---------------------------------------------------------------
    // Methods defined by GraphicsResizeListener
    //---------------------------------------------------------------

    /**
     * Notification that the graphics output device has changed dimensions to
     * the given size. Dimensions are in pixels.
     *
     * @param x The lower left x coordinate for the view
     * @param y The lower left y coordinate for the view
     * @param width The width of the viewport in pixels
     * @param height The height of the viewport in pixels
     */
    public void graphicsDeviceResized(int x, int y, int width, int height) {
		
        viewport_width = width;
        viewport_height = height;
		
		resizeOccured = true;
    }

    //----------------------------------------------------------
    // Methods defined by ConfigListener
    //----------------------------------------------------------

    /**
     * Set the active entity manager
     *
     * @param entityManager The active entity manager
     */
    public void setEntityManager(AV3DEntityManager entityManager) {
		
        this.entityManager = entityManager;
		if (entityManager != null) {
			wrapperMap = entityManager.getAV3DEntityWrapperMap();
		} else {
			wrapperMap = null;
		}
    }

    /**
     * Set the active zone entity
     *
     * @param ze The active zone entity
     */
    public void setActiveZoneEntity(ZoneEntity ze) {
		
		activeZoneEntity = ze;
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

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
     * Initialize the scene graph objects
     *
     * @param dim The initial viewport dimensions in [x, y, width, height]
     */
    private void initSceneGraph(int[] dim) {

        viewpoint = new Viewpoint();
		
		viewMatrix = new Matrix4f();
		viewMatrix.setIdentity();
		
		translation.set(0, 0, 10);
		viewMatrix.setTranslation(translation);
		
		viewpointTransform = new TransformGroup();
		viewpointTransform.setTransform(viewMatrix);
		viewpointTransform.addChild(viewpoint);
		
        rootGroup = new Group();
        rootGroup.addChild(viewpointTransform);

        scene = new SimpleScene();
        scene.setRenderedGeometry(rootGroup);
        scene.setActiveView(viewpoint);

        viewport = new SimpleViewport();
        viewport.setDimensions(dim[0], dim[1], dim[2], dim[3]);
        viewport.setScene(scene);

        layer = new SimpleLayer();
        layer.setViewport(viewport);
		
        viewEnv = scene.getViewEnvironment();
        viewEnv.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        viewEnv.setAspectRatio(0);
        viewEnv.setClipDistance(0.001, 1000.0);
    }
	
	/**
	 * Return a short String identifier of the argument Entity
	 *
	 * @param entity The entity
	 * @return The identifier
	 */
	private String getIdentifier(Entity entity) {
		return("[id="+ entity.getEntityID() + 
			", name=\""+ entity.getName() +"\"" + 
			", desc=\""+ entity.getDescription() +"\"]");
	}
}
