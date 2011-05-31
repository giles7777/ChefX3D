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
import java.util.*;
import javax.vecmath.*;

import javax.xml.parsers.*;

import org.j3d.aviatrix3d.*;

import java.awt.Color;
import java.io.InputStream;
import java.io.IOException;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;


import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.rendering.BoundingVolume;
import org.j3d.aviatrix3d.pipeline.graphics.GraphicsResizeListener;

import org.j3d.renderer.aviatrix3d.geom.Box;
import org.j3d.renderer.aviatrix3d.util.SystemPerformanceListener;

import org.j3d.util.I18nManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.view.awt.scenemanager.*;

import org.chefx3d.ui.LoadingProgressListener;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

/**
 * Implementation of the preview LayerManager
 *
 * @author Rex Melton
 * @version $Revision: 1.48 $
 */
class PreviewLayerManager
	implements
	ModelListener,
	NodeUpdateListener,
	GraphicsResizeListener,
	EntityPropertyListener,
	EntityChildListener,
	EntitySelectionListener,
	AV3DConstants,
	PerFrameUIObserver,
	PerFrameObserver,
	SystemPerformanceListener,
	LayerManager {
	
	/** The position to begin the viewpoints from */
	private static final float[] STARTING_VIEWPOINT_POSITION = 
		new float[] {0, 1.2f, 4.8f};
	
	/** Error message when we fail to parse a single float in the light file */
	private static final String FLOAT_PARSE_MSG =
		"org.chefx3d.view.awt.av3d.PreviewLayerManager.floatParseMsg";
	
	/** Error message when we fail to parse a vector in the light file */
	private static final String VECTOR_PARSE_MSG =
		"org.chefx3d.view.awt.av3d.PreviewLayerManager.vectorParseMsg";
	
	/** Error message when the light colour is not ]0,1] */
	private static final String COLOUR_RANGE_MSG =
		"org.chefx3d.view.awt.av3d.PreviewLayerManager.lightRangeMsg";
	
	/** Message to inform the user HQ rendering is enabled */
	private static final String USE_HQ_RENDERING_MSG =
		"org.chefx3d.view.awt.av3d.PreviewLayerManager.useHQRenderingMsg";
	
	/** Error message when the XML is bad, parsing the light config file */
	private static final String XML_SAX_ERROR_MSG =
		"org.chefx3d.view.awt.av3d.PreviewLayerManager.lightConfigXMLErrorMsg";
	
	/** Error message when the light config file has a generic IO error */
	private static final String CONFIG_IO_ERROR_MSG =
		"org.chefx3d.view.awt.av3d.PreviewLayerManager.lightConfigIOErrorMsg";
	
	/** Default name of the lighting file to go read */
	private static final String DEFAULT_LIGHTING_FILE =
		"config/view/av3d/preview_lighting.xml";
	
	/** Default up axis for the navigation modes */
	private static final Vector3f NAV_UP_AXIS = new Vector3f(0, 1, 0);
	
	/** Default up axis for the view transform wrapper */
	private static final Vector3f DEFAULT_UP_AXIS = new Vector3f(0, 0, 1);
	
	/** Default background color */
	private static final float[] DEFAULT_BACKGROUND_COLOR = { 0, 0, 0 };
	
	/** The extension string used to determine if attribute arrays are supported */
	private static final String GL_ARB_VERTEX_PROGRAM = "GL_ARB_vertex_program";
	
    /** Line art not supported message */
    private static final String LINE_ART_NOT_SUPPORTED_MSG =
        "org.chefx3d.view.awt.av3d.Preview.lineArtNotSupportedMsg";

	/** Default ambient light definition */
	private LightConfigData ambientLightConfig;
	
	/** Point light definitions */
	private LightConfigData[] pointLightConfig;
	
	/** Directional light definitions */
	private LightConfigData[] directionalLightConfig;
	
	/** spot light definitions */
	private LightConfigData[] spotLightConfig;
	
	// Listing of each of the priority levels. This could have been done as a
	// Enum, but I wanted to use a strict numerical system so that all I have
	// to do is inc/dec a number and these values give us the exact
	// implementation
	
	/** Lowest level functionality: fixed function rendering only */
	private static final int PERF_FIXED_FUNCTION = 0;
	
	/**
	 * Basic shaders for lighting and shading that mostly replicate the
	 * fixed function setup, but with phong shading.
	 */
	private static final int PERF_PHONG_SHADING = 1;
	
	/** Use of a deferred shader, along with FSAA. */
	private static final int PERF_FSAA = 2;
	
	/** Deferred shader with ambient occlusion culling added */
	private static final int PERF_AMBIENT_OCCLUSION = 3;
	
	/** Hard shadows are added */
	private static final int PERF_HARD_SHADOWS = 4;
	
	/** Soft shadows are added */
	private static final int PERF_SOFT_SHADOWS = 5;
	
	/** Add bloom rendering to the mix for HDR visuals */
	private static final int PERF_HDR = 6;
	
	/** The priority to use for the performance monitoring */
	private static final int PERFORMANCE_PRIORITY = 1;
	
	/**
	 * Fixed function pipeline is mandatory rather than optional. If this is
	 * is true, then ignore the performanceRating variable for setting up/
	 * modifying the current scene graph.
	 */
	private boolean mustUseFFRendering;
	
	/**
	 * Current performance rating of the system. 0 is the lowest level of
	 * performance and corresponds to the fixed function pipeline rendering.
	 * Any requests to upgrade or downgrade the rendering mess with this number
	 */
	private int performanceRating;
	
	/**
	 * When we're running in high quality mode, this keeps all the offscreen
	 * buffers sane.
	 */
	private HiQViewportManager hqViewManager;
	
	/**
	 * When we're running in high quality mode, this represents all the multi
	 * pass rendering infrastructure that exits over the root of the real
	 * content. If we are forced to fixed function mode, this will be null.
	 */
	private HiQShadingManager hqShaderManager;
	
	/** The scene manager Observer*/
	private SceneManagerObserver mgmtObserver;
	
	/** The device manager */
	private DeviceManager deviceManager;
	
	/** The world model */
	private WorldModel model;
	
	/** Command queue */
	private CommandController controller;
	
	/** The NavigationManager */
	private DefaultNavigationManager navManager;
	
    /** The navigation status manager */
    private NavigationStatusManager navStatusManager;

	/** The scene entity */
	private SceneEntity sceneEntity;
	
	/** Location Entitys, key'ed by Entity ID */
	private HashMap<Integer, LocationEntity> locationEntityMap;
	
	/** Location Group nodes, key'ed by Entity ID */
	private HashMap<Integer, Group> locationGroupMap;
	
	/** Location AV3DEntityManagers, key'ed by Entity ID */
	private HashMap<Integer, AV3DEntityManager> locationManagerMap;
	
	/** The active location Entity */
	private LocationEntity activeLocationEntity;
	
	/** The active location Group */
	private Group activeLocationGroup;
	
	/** Array list of nodes to add to the scene */
	private ArrayList<Node> nodeAddList;
	
	/** Array list of nodes to remove from the scene */
	private ArrayList<Node> nodeRemoveList;
	
	/** The filter to use for url requests, null use baseURL logic instead */
	private URLFilter urlFilter;
	
	/** The visibility handler */
	private PreviewVisibilityHandler visHandler;
	
	/** Reporter instance for handing out errors */
	private ErrorReporter errorReporter;
	
	/** The layer identifier and index */
	private int id;
	
	/** The base layer */
	private SimpleLayer rootLayer;
	
	/** The Scene instance */
	private SimpleScene layerRootScene;
	
	/**
	 * The viewport instance that is used for the content. This may or
	 * may not be the root viewport for this layer, depending on
	 * whether we are in fixed function rendering mode or not.
	 */
	private SimpleViewport contentViewport;
	
	/** Root group of the entire the layer */
	private Group layerRootGroup;
	
	/**
	 * The root grouping node for the content part of the scene graph.
	 * This may or may not be the root of entire scene graph, depending
	 * on whether fixed function rendering is being used or not.
	 */
	private Group contentRootGroup;
	
	/**
	 * Grouping node that contains the actual scene content. This is a
	 * child of the root group above and does not contain all the other
	 * information like the root viewpoint etc as this group is shared
	 * between the HQ and LQ versions of the scene graph.
	 */
	private Group contentContentGroup;
	
	/**
	 * Switch used to hold the point lights used to illuminate the scene.
	 * Used to turn them off when we are in high quality mode because lights
	 * are manually rendered using the shaders.
	 */
	private SwitchGroup lightSwitch;
	
	/** The Viewpoint's TransformGroup */
	private TransformGroup viewpointTransform;
	
	/** The Navigation TransformGroup */
	private TransformGroup navigationTransform;
	
	/** The Navigation Matrix */
	private Matrix4f navigationMatrix;
	
	/** Flag indicating that the navigation transform should be reconfigured */
	private boolean configNavigation;
	
	/** Flag indicating that the line art renderer is in use */
	private boolean useFaxQ;
	
	/** Manager for the line art renderer */
	private FaxQShadingManager faxQShadingManager;
	
	/** Flag indicating that shaders are supported */
	private boolean shadersSupported;
	
	/** Flag indicating that the viewport should be reconfigured */
	private boolean configViewport;
	
	/** Flag indicating that navigation is enabled */
	private boolean navigationEnabled;
	
	/**
	 * Constructor
	 *
	 * @param id The layer id
	 * @param dim The initial dimensions in [x, y, width, height] of the layer
	 * @param viewId Identifier of the view this is in
	 * @param model The WorldModel object
	 * @param controller The CommandController
	 * @param reporter The ErrorReporter instance to use or null
	 * @param mgmtObserver The SceneManagerObserver
	 * @param deviceManager The DeviceManager
     * @param navStatusManager The NavigationStatusManager
	 * @param urlField The urlFilter to use for resource loading
	 * @param gl_info The GLInfo
	 */
	PreviewLayerManager(
		int id,
		int[] dim,
		String viewId,
		WorldModel model,
		CommandController controller,
		ErrorReporter reporter,
		SceneManagerObserver mgmtObserver,
		DeviceManager deviceManager,
        NavigationStatusManager navStatusManager,
		URLFilter urlFilter, 
		GLInfo gl_info) {
		
		this.id = id;
		
		errorReporter = (reporter != null) ?
			reporter : DefaultErrorReporter.getDefaultReporter();
		
		this.model = model;
		this.controller = controller;
		this.deviceManager = deviceManager;
        this.navStatusManager = navStatusManager;
		this.urlFilter = urlFilter;
		
		this.mgmtObserver = mgmtObserver;
		
		// Assume true unless told otherwise as the safe option
		mustUseFFRendering = true;
		performanceRating = PERF_FIXED_FUNCTION;
		
		/////////////////////////////////////////////////////////////////////
		// rem: just relying on the GL_ARB_vertex_program extension is
		// probably not sufficient to fully qualify whether the fax or
		// HiQ shaders will work. but... it is a start.
		shadersSupported = false;
		if (gl_info != null) {
			String[] gl_extensions = gl_info.getExtensions();
			if (gl_extensions != null) {
				for (int i = 0; i < gl_extensions.length; i++) {
					String ext_string = gl_extensions[i];
					if (GL_ARB_VERTEX_PROGRAM.equalsIgnoreCase(ext_string)) {
						shadersSupported = true;
						break;
					}
				}
				
			}
		}
		if (!shadersSupported) {
			// inform that the line art shaders are disabled
        	I18nManager i18Mgr = I18nManager.getManager();
			String warning_msg = i18Mgr.getString(LINE_ART_NOT_SUPPORTED_MSG);
			errorReporter.warningReport(warning_msg, null);
		}
		/////////////////////////////////////////////////////////////////////
		
		// uncomment this if you want to force a hq render
		//        performanceRating = PERF_PHONG_SHADING;
		
		// Create this anyway, just in case we need it, even on platforms
		// that have no hope in hell of being able to run shaders.
		hqViewManager = new HiQViewportManager(mgmtObserver);
		
		loadLightingConfig();
		buildContentSceneGraph(viewId, dim);
		
		// assuming fixed function to start with
		rootLayer = new SimpleLayer();
		rootLayer.setViewport(contentViewport);
		
		hqViewManager.addFullWindowResize(contentViewport);
		hqViewManager.setErrorReporter(errorReporter);
		
		model.addModelListener(this);
		
		mgmtObserver.addObserver(this);
		mgmtObserver.addObserver(hqViewManager);
		
		Object prop = ApplicationParams.get("enableHQRendering");
		boolean enable_hq = true;
		
		if ((prop != null) && (prop instanceof Boolean)) {
			enable_hq = ((Boolean)prop).booleanValue();
		}
		if (enable_hq) {
			mgmtObserver.addPerformanceListener(this, PERFORMANCE_PRIORITY);
		}
		
		navigationEnabled = true;
	}
	
	//----------------------------------------------------------
	// Methods defined by GraphicsResizeListener
	//----------------------------------------------------------
	
	/**
	 * Notification that the surface resized.
	 */
	public void graphicsDeviceResized(int x, int y, int width, int height) {
		hqViewManager.graphicsDeviceResized(x, y, width, height);
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
		return id;
	}
	
	/**
	 * Return the Layer object
	 *
	 * @return The Layer object
	 */
	public Layer getLayer() {
		return rootLayer;
	}
	
	/**
	 * Return the BoundingVolume of the content in this Layer
	 *
	 * @return The BoundingVolume
	 */
	public BoundingVolume getBounds() {
		return contentRootGroup.getBounds();
	}
	
	/**
	 * Fetch the transform that holds the current viewpoint
	 * information.
	 *
	 * @return Tranform node with a viewpoint under it
	 */
	public TransformGroup getViewpointTransform() {
		return viewpointTransform;
	}
	
	/**
	 * Get the view environment used to render the scene with.
	 *
	 * @return The current configured view environment
	 */
	public ViewEnvironment getViewEnvironment() {
		if (useFaxQ) {
			return(faxQShadingManager.getViewEnvironment());
		} else if (mustUseFFRendering || performanceRating == PERF_FIXED_FUNCTION) {
			SimpleScene scene = contentViewport.getScene();
			return(scene.getViewEnvironment());
		} else {
			return(hqShaderManager.getViewEnvironment());
		}
	}
	
	//----------------------------------------------------------
	// Methods defined by SystemPerformanceListener
	//----------------------------------------------------------
	
	/**
	 * Notification of a performance downgrade is required in the system.
	 * This listener should attempt to reduce it's performance demands now if
	 * it is able to and return true. If not able to reduce it the return
	 * false.
	 *
	 * @return True if the performance demands were decreased
	 */
	public boolean downgradePerformance() {
		if(mustUseFFRendering)
			return false;
		
		performanceRating--;
		
		switch(performanceRating) {
		case PERF_FIXED_FUNCTION:
			mgmtObserver.requestDataUpdate(contentViewport, this);
			break;
			
		case PERF_PHONG_SHADING:
			hqShaderManager.enableFSAA(false);
			break;
			
		case PERF_FSAA:
			hqShaderManager.enableSSAO(false);
			break;
			
		case PERF_AMBIENT_OCCLUSION:
			break;
			
		case PERF_HARD_SHADOWS:
		case PERF_SOFT_SHADOWS:
			// Not implemented yet, so treat like HDR
			hqShaderManager.enableBloom(false);
			break;
			
		case PERF_HDR:
			break;
			
		default:
			// Reset the rating to the max allowed.
			performanceRating++;
		}
		
		return true;
	}
	
	/**
	 * Notification of a performance upgrade is required by the system. This
	 * listener is free to increase performance demands of the system. If it
	 * does upgrade, return true, otherwise return false.
	 *
	 * @return True if the performance demands were increased
	 */
	public boolean upgradePerformance() {
		if(mustUseFFRendering)
			return false;
		
		performanceRating++;
		
		switch(performanceRating) {
		case PERF_FIXED_FUNCTION:
			break;
			
		case PERF_PHONG_SHADING:
			mgmtObserver.requestDataUpdate(contentViewport, this);
			break;
			
		case PERF_FSAA:
			hqShaderManager.enableFSAA(true);
			break;
			
		case PERF_AMBIENT_OCCLUSION:
			hqShaderManager.enableSSAO(true);
			break;
			
		case PERF_HARD_SHADOWS:
		case PERF_SOFT_SHADOWS:
			// Not implemented yet, so treat like HDR
			
		case PERF_HDR:
			hqShaderManager.enableBloom(true);
			break;
			
		default:
			// Reset the rating to the min allowed.
			performanceRating--;
		}
		
		
		return true;
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameUIObserver
	//---------------------------------------------------------------
	
	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrameUI() {
		
		if (navigationEnabled) {
			deviceManager.processTrackers(id, navManager);
		}
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameObserver
	//---------------------------------------------------------------
	
	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrame() {
		
		if (configNavigation) {
			mgmtObserver.requestBoundsUpdate(navigationTransform, this);
		}
		if (configViewport) {
			mgmtObserver.requestDataUpdate(contentViewport, this);
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
		
		if (src == contentContentGroup) {
			int numToRemove = nodeRemoveList.size();
			if (numToRemove > 0) {
				for(int i = 0; i < numToRemove; i++) {
					Node node = nodeRemoveList.get(i);
					contentContentGroup.removeChild(node);
				}
				nodeRemoveList.clear();
			}
			
			int numToAdd = nodeAddList.size();
			if (numToAdd > 0) {
				for(int i = 0; i < numToAdd; i++) {
					Node node = nodeAddList.get(i);
					contentContentGroup.addChild(node);
				}
				nodeAddList.clear();
			}
		} else if ((src == navigationTransform) && configNavigation) {
			
			navigationTransform.setTransform(navigationMatrix);
			configNavigation = false;
		}
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The node or Node Component that is to be updated.
	 */
	public void updateNodeDataChanges(Object src) {
		
		if (src == contentViewport) {
			if (useFaxQ) {
				contentViewport.removeScene();
				contentViewport.setScene(faxQShadingManager.getContainedScene());
			} else if (performanceRating == PERF_FIXED_FUNCTION) {
				contentViewport.removeScene();
				contentViewport.setScene(layerRootScene);
			} else {
				contentViewport.removeScene();
				contentViewport.setScene(hqShaderManager.getContainedScene());
				
				I18nManager intl_mgr = I18nManager.getManager();
				String msg = intl_mgr.getString(USE_HQ_RENDERING_MSG);
				
				errorReporter.messageReport(msg);
			}
			configViewport = false;
		}
	}
	
	//----------------------------------------------------------
	// Methods for EntityPropertyListener
	//----------------------------------------------------------
	
	public void propertiesUpdated(List<EntityProperty> properties) {
		// TODO: should probably do something with this.......
	}
	
	public void propertyAdded(int entityID, String propertySheet,
		String propertyName) {
		
	}
	
	public void propertyRemoved(int entityID, String propertySheet,
		String propertyName) {
		
	}
	
	public void propertyUpdated(int entityID, String propertySheet,
		String propertyName, boolean ongoing) {
		
		Entity entity = model.getEntity(entityID);
		
		if (entity == sceneEntity) {
			boolean refresh = false;
			if (propertyName.equals(SceneEntity.HIDE_WALLS_PROP)) {
				
				boolean value = (Boolean)sceneEntity.getProperty(
					SceneEntity.DEFAULT_ENTITY_PROPERTIES,
					SceneEntity.HIDE_WALLS_PROP);
				visHandler.setAutoHideWalls(value);
				refresh = true;
				
			} else if (propertyName.equals(SceneEntity.HIDE_PRODUCTS_PROP)) {
				
				boolean value = (Boolean)sceneEntity.getProperty(
					SceneEntity.DEFAULT_ENTITY_PROPERTIES,
					SceneEntity.HIDE_PRODUCTS_PROP);
				visHandler.setAutoHideProducts(value);
				refresh = true;
			}
			
			if (refresh) {
				Matrix4f mtx = new Matrix4f();
				viewpointTransform.getTransform(mtx);
				//                navigationTransform.getTransform(mtx);
				visHandler.viewMatrixChanged(mtx);
			}
			
		} else if (entity instanceof LocationEntity) {
			if (entity == activeLocationEntity) {
				if (propertyName.equals(LocationEntity.BACKGROUND_COLOR_PROP)) {
					// configure the background
					Color color = activeLocationEntity.getBackgroundColor();
					
					if (color != null) {
						hqViewManager.backgroundChanged(color.getColorComponents(null));
					} else {
						hqViewManager.backgroundChanged(DEFAULT_BACKGROUND_COLOR);
					}
				} else if (propertyName.equals(LocationEntity.GROUND_NORMAL_PROP)) {
					// configure the navigation transform
					float[] normal = activeLocationEntity.getGroundNormal();
					configNavigation(normal);
				}
			}
		}
	}
	
	//----------------------------------------------------------
	// Methods for EntityChildListener
	//----------------------------------------------------------
	
	/**
	 * A child was added.
	 *
	 * @param parentID The entity ID of the parent
	 * @param childID The entity ID of the child
	 */
	public void childAdded(int parentID, int childID) {
		
		Entity parentEntity = model.getEntity(parentID);
		int index = parentEntity.getChildIndex(childID);
		
		childInsertedAt(parentID, childID, index);
	}
	
	/**
	 * A child was inserted.
	 *
	 * @param parentID The entity ID of the parent
	 * @param childID The entity ID of the child
	 * @param index The index the child was inserted at
	 */
	public void childInsertedAt(int parentID, int childID, int index) {
		
		Entity parentEntity = model.getEntity(parentID);
		Entity childEntity = parentEntity.getChildAt(index);
		
		if (parentEntity instanceof SceneEntity &&
			childEntity instanceof LocationEntity) {
			
			LocationEntity le = (LocationEntity)childEntity;
			addLocationEntity(le);
			
			if (activeLocationEntity == null) {
				setActiveLocationEntity(le);
			}
			
		} else if (parentEntity instanceof LocationEntity &&
			childEntity instanceof ViewpointContainerEntity) {
			
			if (parentEntity == activeLocationEntity) {
				ViewpointContainerEntity vce =
					(ViewpointContainerEntity)childEntity;
				navManager.setViewpointContainerEntity(vce);
				navManager.setNavigationMode(NavigationMode.EXAMINE);
				NavigationModeController nmc =
					navManager.getNavigationModeController();
				// hard coding initial center of rotation
				nmc.setCenterOfRotation(
					new float[] {0, STARTING_VIEWPOINT_POSITION[1], 0});
			}
		}
	}
	
	/**
	 * A child was removed.
	 *
	 * @param parentID The entity ID of the parent
	 * @param childID The entity ID of the child
	 */
	public void childRemoved(int parentID, int childID) {
		
		if (locationEntityMap.containsKey(childID)) {
			
			LocationEntity le = locationEntityMap.get(childID);
			removeLocationEntity(le);
		}
	}
	
	// ---------------------------------------------------------------
	// Methods defined by EntitySelectionListener
	// ---------------------------------------------------------------
	
	/**
	 * An entity has been selected
	 *
	 * @param entityID The entity which changed
	 * @param selected Status of selecting
	 */
	public void selectionChanged(int entityID, boolean selected) {
		
		Entity entity = model.getEntity(entityID);
		if (entity != null) {
			if (selected) {
				if (entity instanceof LocationEntity) {
					LocationEntity le = (LocationEntity)entity;
					if (le != activeLocationEntity) {
						setActiveLocationEntity(le);
					}
				}
			}
		}
	}
	
	/**
	 * An entity has been highlighted
	 *
	 * @param entityID The entity which changed
	 * @param highlighted Status of highlighting
	 */
	public void highlightChanged(int entityID, boolean highlighted) {
	}
	
	//----------------------------------------------------------
	// Methods required by ModelListener
	//----------------------------------------------------------
	
	/**
	 * An entity was added.
	 *
	 * @param local Was this action initiated from the local UI
	 * @param entity The entity
	 */
	public void entityAdded(boolean local, Entity entity){
		
		if (entity instanceof SceneEntity) {
			setSceneEntity((SceneEntity)entity);
		}
	}
	
	/**
	 * An entity was removed.
	 *
	 * @param local Is the request local
	 * @param entity The entity to remove
	 */
	public void entityRemoved(boolean local, Entity entity) {
		
		if (entity == sceneEntity) {
			clear();
		}
	}
	
	/**
	 * The model has been reset.
	 *
	 * @param local Was this action initiated from the local UI
	 */
	public void modelReset(boolean local) {
	}
	
	/**
	 * User view information changed.
	 *
	 * @param pos The position of the user
	 * @param rot The orientation of the user
	 * @param fov The field of view changed(X3D Semantics)
	 */
	public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
	}
	
	/**
	 * The master view has changed.
	 *
	 * @param local Is the request local
	 * @param viewID The view which is master
	 */
	public void masterChanged(boolean local, long viewID) {
	}
	
	//------------------------------------------------------------------------
	// Local Methods
	//------------------------------------------------------------------------
	
	/**
	 * Enable navigation
	 *
	 * @param state true to enable, false to disable
	 */
	void setNavigationEnabled(boolean state) {
		navigationEnabled = state;
	}
	
	/**
	 * Enable navigation
	 *
	 * @param state true to enable, false to disable
	 */
	boolean isNavigationEnabled() {
		return(navigationEnabled);
	}
	
	/**
	 * Register an error reporter with the command instance
	 * so that any errors generated can be reported in a nice manner.
	 *
	 * @param reporter The new ErrorReporter to use.
	 */
	void setErrorReporter(ErrorReporter reporter) {
		errorReporter = (reporter != null) ?
			reporter : DefaultErrorReporter.getDefaultReporter();
		
		hqViewManager.setErrorReporter(errorReporter);
		
		if (hqShaderManager != null) {
			hqShaderManager.setErrorReporter(errorReporter);
		}
	
		if (faxQShadingManager != null) {
			faxQShadingManager.setErrorReporter(errorReporter);
		}
	}
	
	/**
	 * An update has occurred with the external holders, so cause everything
	 * to be refreshed. Needed to deal with the add/remove notify changes to
	 * avoid timing issues.
	 */
	void refreshView() {
		if ((faxQShadingManager != null) || (hqShaderManager != null)) {
			hqViewManager.refreshView();
		}
		navManager.refresh();
		visHandler.setEnabled(true);
	}
	
	/**
	 * Return whether line art rendering is available
	 *
	 * @return true if line art rendering is available,
	 * false otherwise.
	 */
	boolean isLineArtRenderingSupported() {
		return(shadersSupported);
	}
	
	/**
	 * Set the state of line art rendering
	 *
	 * @param enable true to enable line art rendering,
	 * false to return to the primary mode.
	 */
	void enableLineArtRendering(boolean enable) {
		if (enable && shadersSupported) {
			Matrix4f viewMat = new Matrix4f();
			navManager.getViewMatrix(viewMat);
			
			faxQShadingManager = new FaxQShadingManager(errorReporter, hqViewManager, mgmtObserver);
			faxQShadingManager.initialize(
				(SharedNode)contentContentGroup.getParent(),
				viewMat,
				hqViewManager.getWindowWidth(),
				hqViewManager.getWindowHeight());
			
			useFaxQ = true;
			configViewport = true;
			//mgmtObserver.requestDataUpdate(contentViewport, this);
			hqViewManager.refreshView();
			//////////////////////////////////////////////////////
			// rem: apparently, unless the scene graph gets
			// 'tickled' the snapshot does not get processed.
			navManager.refresh();
			//////////////////////////////////////////////////////
		} else {
			if (faxQShadingManager != null) {
				faxQShadingManager.shutdown();
				faxQShadingManager = null;
			}
			useFaxQ = false;
			configViewport = true;
			//mgmtObserver.requestDataUpdate(contentViewport, this);
		}
	}
	
	/**
	 * Toggle the state of fixed vs shader rendering functionality. If
	 * this is called with true then we force the use of the simple
	 * fixed function pipeline.
	 *
	 * @param enable True if we must use fixed function rendering, false
	 *   to allow the system to decide what to use
	 */
	void forceFixedFunctionRendering(boolean enable) {
		
		if (enable) {
			// Change last here so that we have had time to set up the
			// various bits and pieces needed
			
			// Enable first so that if any callbacks happen we know to
			// move straight to fixed function.
			mustUseFFRendering = enable;
			
			if (hqShaderManager != null) {
				hqShaderManager.shutdown();
				hqShaderManager = null;
			}
		} else if (hqShaderManager == null) {
			// Free reign, so make the high quality shader available again.
			// Doesn't mean we automatically add it to the scene, just that
			// it is available for use now.
			float [] colorValues;
			if (activeLocationEntity != null) {
				Color color = activeLocationEntity.getBackgroundColor();
				
				if (color != null) {
					colorValues = color.getColorComponents(null);
					if(colorValues == null || colorValues.length < 3)
						colorValues = DEFAULT_BACKGROUND_COLOR;
				} else {
					colorValues = DEFAULT_BACKGROUND_COLOR;
				}
			} else {
				colorValues = DEFAULT_BACKGROUND_COLOR;
			}
			
			Matrix4f viewMat = new Matrix4f();
			navManager.getViewMatrix(viewMat);
			
			hqShaderManager = new HiQShadingManager(hqViewManager);
			
			// set up defaults here. Normally overrridden by the performance
			// ladder, but for debugging use these.
			//hqShaderManager.useFSAA(true);
			//hqShaderManager.useSSAO(true);
			//hqShaderManager.useBloom(false);
			
			hqShaderManager.initialize((SharedNode)contentContentGroup.getParent(),
				viewMat,
				ambientLightConfig,
				pointLightConfig,
				colorValues,
				hqViewManager.getWindowWidth(),
				hqViewManager.getWindowHeight());
			
			hqShaderManager.setErrorReporter(errorReporter);
			
			// Change last here so that we have had time to set up the
			// various bits and pieces needed
			mustUseFFRendering = enable;
			
			// uncomment this if you want to force a hq render
			//mgmtObserver.requestDataUpdate(contentViewport, this);
		}
	}
	
	/**
	 * Get the root contained scene used for the whole window. Used to
	 * register a callback for thumbnails.
	 *
	 * @return The scene instance at the root of the graph
	 */
	Scene getRootScene() {
		if (useFaxQ) {
			return(faxQShadingManager.getContainedScene());
		} else if (mustUseFFRendering || (performanceRating == PERF_FIXED_FUNCTION)) {
			return(layerRootScene);
		} else {
			return(hqShaderManager.getContainedScene());
		}
	}
	
	/**
	 * Return the entity manager for the specified LocationEntity.
	 *
	 * @param le The LocationEntity to retrieve the manager for. If null,
	 * the current active LocationEntity's manager will be returned. If a 
	 * manager does not exist for a specified LocationEntity, null will be
	 * returned.
	 * @return The entity manager
	 */
	AV3DEntityManager getAV3DEntityManager(LocationEntity le) {
		AV3DEntityManager mngr = null;
		if (le == null) {
			mngr = locationManagerMap.get(activeLocationEntity);
		} else {
			mngr = locationManagerMap.get(le);
		}
		return(mngr);
	}
	
	/**
	 * Initialize from the SceneEntity
	 *
	 * @param se The new SceneEntity
	 */
	private void setSceneEntity(SceneEntity se) {
		
		if (sceneEntity != null) {
			// a scene already exists, cleanup
			clear();
		}
		
		LocationEntity newActiveLocationEntity = null;
		
		if (se != null) {
			sceneEntity = se;
			sceneEntity.addEntityChildListener(this);
			sceneEntity.addEntityPropertyListener(this);
			
			boolean value = (Boolean)sceneEntity.getProperty(
				SceneEntity.DEFAULT_ENTITY_PROPERTIES,
				SceneEntity.HIDE_WALLS_PROP);
			visHandler.setAutoHideWalls(value);
			
			value = (Boolean)sceneEntity.getProperty(
				SceneEntity.DEFAULT_ENTITY_PROPERTIES,
				SceneEntity.HIDE_PRODUCTS_PROP);
			visHandler.setAutoHideProducts(value);
			
			ArrayList<Entity> entityList = sceneEntity.getChildren();
			for (int i = 0; i < entityList.size(); i++) {
				Entity e = entityList.get(i);
				if (e instanceof LocationEntity) {
					LocationEntity le = (LocationEntity)e;
					addLocationEntity(le);
					if (newActiveLocationEntity == null) {
						// pick the first one
						newActiveLocationEntity = le;
					}
				}
			}
		}
		setActiveLocationEntity(newActiveLocationEntity);
	}
	
	/**
	 * Initialize the navigation transform
	 *
	 * @param normal The ground plane normal
	 */
	private void configNavigation(float[] normal) {
		Vector3f normalVector = null;
		if (normal == null) {
			normalVector = DEFAULT_UP_AXIS;
		} else {
			normalVector = new Vector3f(normal);
		}
		navigationMatrix.setIdentity();
		
		if (!normalVector.equals(NAV_UP_AXIS)) {
			Vector3f axis = new Vector3f();
			axis.cross(NAV_UP_AXIS, normalVector);
			axis.normalize();
			float angle = NAV_UP_AXIS.angle(normalVector);
			AxisAngle4f rotation = new AxisAngle4f(axis, angle);
			navigationMatrix.setRotation(rotation);
		}
		
		visHandler.setNavigationMatrix(navigationMatrix);
		hqViewManager.orientationViewChanged(navigationMatrix);
		configNavigation = true;
	}
	
	/**
	 * Remove objects, references, etc. from the scenegraph
	 */
	private void clear() {
		sceneEntity.removeEntityChildListener(this);
		sceneEntity.removeEntityPropertyListener(this);
		
		if (activeLocationGroup != null) {
			nodeRemoveList.add(activeLocationGroup);
			
			mgmtObserver.requestBoundsUpdate(contentContentGroup, this);
		}
		
		for (Iterator<Integer> i = locationEntityMap.keySet().iterator();
			i.hasNext();) {
			
			LocationEntity le = locationEntityMap.get(i.next());
			/////////////////////////////////////////////////////////////////////////
			// rem: this -I think- was the cause of a ConcurrentModificationException
			//removeLocationEntity(le);
			/////////////////////////////////////////////////////////////////////////
			le.removeEntityChildListener(this);
			le.removeEntityPropertyListener(this);
			le.removeEntitySelectionListener(this);
		}
		for (Iterator<Integer> i = locationManagerMap.keySet().iterator();
			i.hasNext();) {
			
			AV3DEntityManager mngr = locationManagerMap.get(i.next());
			mngr.clear();
		}
		setActiveLocationEntity(null);
		
		// just to be sure - clear everything
		locationEntityMap.clear();
		locationGroupMap.clear();
		locationManagerMap.clear();
		
		activeLocationEntity = null;
		activeLocationGroup = null;
		sceneEntity = null;
	}
	
	/**
	 * Add a new LocationEntity
	 *
	 * @param le The LocationEntity to add
	 */
	private void addLocationEntity(LocationEntity le) {
		
		Integer id = new Integer(le.getEntityID());
		locationEntityMap.put(id, le);
		
		Group group = new Group();
		locationGroupMap.put(id, group);
		
		AV3DEntityManager em = new AV3DEntityManager(
			mgmtObserver,
			model,
			le,
			group,
			urlFilter,
			null,
			errorReporter);
		//em.setShadowEntityEnabled(false);
		
		locationManagerMap.put(id, em);
		
		le.addEntityChildListener(this);
		le.addEntityPropertyListener(this);
		le.addEntitySelectionListener(this);
	}
	
	/**
	 * Remove a LocationEntity
	 *
	 * @param le The LocationEntity to remove
	 */
	private void removeLocationEntity(LocationEntity le) {
		
		int loc_id = le.getEntityID();
		
		locationEntityMap.remove(loc_id);
		
		le.removeEntityChildListener(this);
		le.removeEntityPropertyListener(this);
		le.removeEntitySelectionListener(this);
		
		AV3DEntityManager mngr = locationManagerMap.remove(loc_id);
		mngr.clear();
		
		if (le == activeLocationEntity) {
			setActiveLocationEntity(null);
		}
		
		locationGroupMap.remove(loc_id);
	}
	
	/**
	 * Set the active LocationEntity
	 *
	 * @param le The new active LocationEntity
	 */
	private void setActiveLocationEntity(LocationEntity le) {
		
		if (le == activeLocationEntity) {
			return;
		}
		
		boolean locationIsActive = (activeLocationEntity != null);
		boolean scheduleGroupChange = false;
		
		if (locationIsActive) {
			
			scheduleGroupChange = true;
			if (nodeAddList.contains(activeLocationGroup)) {
				nodeAddList.remove(activeLocationGroup);
			} else {
				nodeRemoveList.add(activeLocationGroup);
			}
			activeLocationGroup = null;
		}
		
		activeLocationEntity = le;
		
		if (activeLocationEntity != null) {
			
			int id = activeLocationEntity.getEntityID();
			activeLocationGroup = locationGroupMap.get(id);
			if (nodeRemoveList.contains(activeLocationGroup)) {
				nodeRemoveList.remove(activeLocationGroup);
			} else {
				nodeAddList.add(activeLocationGroup);
			}
			scheduleGroupChange = true;
			
			Color color = activeLocationEntity.getBackgroundColor();
			
			if (color != null) {
				hqViewManager.backgroundChanged(color.getColorComponents(null));
			} else {
				hqViewManager.backgroundChanged(DEFAULT_BACKGROUND_COLOR);
			}
			
			float[] normal = activeLocationEntity.getGroundNormal();
			configNavigation(normal);
			
			AV3DEntityManager mngr = locationManagerMap.get(id);
			visHandler.setEntityManager(mngr);
			
			ViewpointContainerEntity vce =
				activeLocationEntity.getViewpointContainerEntity();
			navManager.setViewpointContainerEntity(vce);
			navManager.setNavigationMode(NavigationMode.EXAMINE);
			
		} else {
			navManager.setViewpointContainerEntity(null);
			navManager.setNavigationMode(NavigationMode.NONE);
			
			hqViewManager.backgroundChanged(DEFAULT_BACKGROUND_COLOR);
			
			visHandler.setEntityManager(null);
		}
		
		if (scheduleGroupChange) {
			mgmtObserver.requestBoundsUpdate(contentContentGroup, this);
		}
	}
	
	/**
	 * Return the UserInputHandler for this layer
	 *
	 * @return The UserInputHandler
	 */
	UserInputHandler getUserInputHandler() {
		return navManager;
	}
	
	/**
	 * Return the NavigationManager
	 *
	 * @return The NavigationManager
	 */
	NavigationManager getNavigationManager() {
		return navManager;
	}
	
	/**
	 * Set the active navigation mode
	 *
	 * @param activeMode The new navigation mode
	 */
	void setNavigationMode(NavigationMode mode) {
		navManager.setNavigationMode(mode);
	}
	
	/**
	 * Return the active navigation mode
	 *
	 * @return The active navigation mode
	 */
	NavigationMode getNavigationMode() {
		return navManager.getNavigationMode();
	}
	
	/**
	 * Return the visibility handler
	 *
	 * @return The visibility handler
	 */
	PreviewVisibilityHandler getVisibilityHandler() {
		return(visHandler);
	}
	
	/**
	 * Load the lighting configuration data from the external config file.
	 * Creates lights dynamically from the file and stores them in the
	 * internal arrays of class variables for later use.
	 */
	private void loadLightingConfig() {
		
		try {
			FileLoader fileLookup = new FileLoader();
			Object[] file = fileLookup.getFileURL(DEFAULT_LIGHTING_FILE, true);
			InputStream is = (InputStream)file[1];
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(is);
			doc.getDocumentElement().normalize();
			
			Element root_element = doc.getDocumentElement();
			
			loadAmbientLight(root_element.getElementsByTagName("ambient"));
			loadPointLights(root_element.getElementsByTagName("pointlight"));
			loadDirectionalLights(root_element.getElementsByTagName("directionallight"));
			loadSpotLights(root_element.getElementsByTagName("spotlight"));
		} catch (SAXException e) {
			I18nManager intl_mgr = I18nManager.getManager();
			String msg = intl_mgr.getString(XML_SAX_ERROR_MSG);
			
			errorReporter.errorReport(msg, e);
		} catch (IOException ioe) {
			I18nManager intl_mgr = I18nManager.getManager();
			String msg = intl_mgr.getString(CONFIG_IO_ERROR_MSG);
			
			errorReporter.errorReport(msg, ioe);
		} catch (ParserConfigurationException pce) {
		}
	}
	
	/**
	 * Process the list of ambient lights in the config file. There is only
	 * allowed to be one ambient light. Everything after the first is ignored.
	 *
	 * @param nodes The list of light definitions in XML form
	 */
	private void loadAmbientLight(NodeList nodes) {
		if(nodes.getLength() == 0)
			return;
		
		Element light = (Element)nodes.item(0);
		
		// Hard code for now. Will come back and load from a file later.
		ambientLightConfig = new LightConfigData();
		ambientLightConfig.type = LightConfigData.LightType.AMBIENT;
		
		
		String attr = light.getAttribute("color");
		float[] col = parseColour(attr);
		
		ambientLightConfig.lightColor[0] = col[0];
		ambientLightConfig.lightColor[1] = col[1];
		ambientLightConfig.lightColor[2] = col[2];
	}
	
	/**
	 * Process the list of point lights in the config file
	 *
	 * @param nodes The list of light definitions in XML form
	 */
	private void loadPointLights(NodeList nodes) {
		int num_lights = nodes.getLength();
		
		pointLightConfig = new LightConfigData[num_lights];
		
		for(int i = 0; i < num_lights; i++) {
			Element light = (Element)nodes.item(i);
			
			pointLightConfig[i] = new LightConfigData();
			
			String pos_str = light.getAttribute("position");
			String col_str = light.getAttribute("color");
			String att_str = light.getAttribute("attenuation");
			String rad_str = light.getAttribute("radius");
			
			pointLightConfig[i].lightRadius = parseFloat(rad_str);
			float[] col = parseColour(col_str);
			
			pointLightConfig[i].lightColor[0] = col[0];
			pointLightConfig[i].lightColor[1] = col[1];
			pointLightConfig[i].lightColor[2] = col[2];
			
			
			float[] pos = parseVector(pos_str);
			pointLightConfig[i].lightPosition[0] = pos[0];
			pointLightConfig[i].lightPosition[1] = pos[1];
			pointLightConfig[i].lightPosition[2] = pos[2];
			
			float[] att = parseVector(att_str);
			pointLightConfig[i].attenuation[0] = att[0];
			pointLightConfig[i].attenuation[1] = att[1];
			pointLightConfig[i].attenuation[2] = att[2];
		}
	}
	
	/**
	 * Process the list of directional lights in the config file
	 *
	 * @param nodes The list of light definitions in XML form
	 */
	private void loadDirectionalLights(NodeList nodes) {
		int num_lights = nodes.getLength();
		
		directionalLightConfig = new LightConfigData[num_lights];
		
		for(int i = 0; i < num_lights; i++) {
			Element light = (Element)nodes.item(i);
			
			directionalLightConfig[i] = new LightConfigData();
			directionalLightConfig[i].type = LightConfigData.LightType.DIRECTIONAL;
			
			String col_str = light.getAttribute("color");
			String dir_str = light.getAttribute("direction");
			
			
			float[] dir = parseVector(dir_str);
			directionalLightConfig[i].direction[0] = dir[0];
			directionalLightConfig[i].direction[1] = dir[1];
			directionalLightConfig[i].direction[2] = dir[2];
			
			float[] col = parseColour(col_str);
			directionalLightConfig[i].lightColor[0] = col[0];
			directionalLightConfig[i].lightColor[1] = col[1];
			directionalLightConfig[i].lightColor[2] = col[2];
		}
	}
	
	/**
	 * Process the list of spot lights in the config file
	 *
	 * @param nodes The list of light definitions in XML form
	 */
	private void loadSpotLights(NodeList nodes) {
		int num_lights = nodes.getLength();
		
		spotLightConfig = new LightConfigData[num_lights];
		
		for(int i = 0; i < num_lights; i++) {
			Element light = (Element)nodes.item(i);
			
			spotLightConfig[i] = new LightConfigData();
			spotLightConfig[i].type = LightConfigData.LightType.SPOT;
			
			String ang_str = light.getAttribute("angle");
			String pos_str = light.getAttribute("position");
			String col_str = light.getAttribute("color");
			String att_str = light.getAttribute("attenuation");
			String dir_str = light.getAttribute("direction");
			
			spotLightConfig[i].angle = parseFloat(ang_str);
			
			float[] dir = parseVector(dir_str);
			spotLightConfig[i].direction[0] = dir[0];
			spotLightConfig[i].direction[1] = dir[1];
			spotLightConfig[i].direction[2] = dir[2];
			
			float[] col = parseColour(col_str);
			spotLightConfig[i].lightColor[0] = col[0];
			spotLightConfig[i].lightColor[1] = col[1];
			spotLightConfig[i].lightColor[2] = col[2];
			
			float[] pos = parseVector(pos_str);
			spotLightConfig[i].lightPosition[0] = pos[0];
			spotLightConfig[i].lightPosition[1] = pos[1];
			spotLightConfig[i].lightPosition[2] = pos[2];
			
			float[] att = parseVector(att_str);
			spotLightConfig[i].attenuation[0] = att[0];
			spotLightConfig[i].attenuation[1] = att[1];
			spotLightConfig[i].attenuation[2] = att[2];
		}
	}
	
	/**
	 * Parse a string containing a single number and return that as a float. If
	 * the parsing fails assume a default value of 0.0.
	 *
	 * @param str The input string to parse
	 * @return The value parsed or 0
	 */
	private float parseFloat(String str) {
		float ret_val = 0;
		
		try {
			ret_val = Float.parseFloat(str);
		} catch(Exception e) {
			I18nManager intl_mgr = I18nManager.getManager();
			String msg_pattern = intl_mgr.getString(FLOAT_PARSE_MSG);
			
			Locale lcl = intl_mgr.getFoundLocale();
			
			Object[] msg_args = { str };
			Format[] fmts = { null };
			MessageFormat msg_fmt =
				new MessageFormat(msg_pattern, lcl);
			msg_fmt.setFormats(fmts);
			String msg = msg_fmt.format(msg_args);
			
			errorReporter.warningReport(msg, null);
		}
		
		return ret_val;
	}
	
	/**
	 * Parse a string containing 3 numbers that are assumed to be [0,1] colour
	 * values.
	 *
	 * @param str The input string to parse
	 * @return An array of length 3 with the parsed numbers
	 */
	private float[] parseColour(String str) {
		float[] ret_val = parseVector(str);
		
		for(int i = 0; i < 3; i++)
			if(ret_val[i] < 0 || ret_val[i] > 1) {
				I18nManager intl_mgr = I18nManager.getManager();
				String msg_pattern = intl_mgr.getString(COLOUR_RANGE_MSG);
				
				Locale lcl = intl_mgr.getFoundLocale();
				
				Object[] msg_args = { str };
				Format[] fmts = { null };
				MessageFormat msg_fmt =
					new MessageFormat(msg_pattern, lcl);
				msg_fmt.setFormats(fmts);
				String msg = msg_fmt.format(msg_args);
				
				errorReporter.warningReport(msg, null);
			}
		
		return ret_val;
	}
	
	/**
	 * Parse a string containing 3 numbers and return that as a float array. If
	 * the parsing fails on any component, assume a default value of 0.0.
	 *
	 * @param str The input string to parse
	 * @return An array of length 3 with the parsed numbers
	 */
	private float[] parseVector(String str) {
		float[] ret_val = new float[3];
		StringTokenizer strtok = new StringTokenizer(str);
		
		for(int i = 0; i < 3; i++) {
			try {
				String num_str = strtok.nextToken();
				ret_val[i] = Float.parseFloat(num_str);
			} catch(Exception e) {
				I18nManager intl_mgr = I18nManager.getManager();
				String msg_pattern = intl_mgr.getString(VECTOR_PARSE_MSG);
				
				Locale lcl = intl_mgr.getFoundLocale();
				
				Object[] msg_args = { str };
				Format[] fmts = { null };
				MessageFormat msg_fmt =
					new MessageFormat(msg_pattern, lcl);
				msg_fmt.setFormats(fmts);
				String msg = msg_fmt.format(msg_args);
				errorReporter.warningReport(msg, null);
			}
		}
		
		return ret_val;
	}
	
	/**
	 * Convenience method to build up the real content part of the scene graph.
	 *
	 * @param viewId Identifier of the view this is in
	 * @param dimensions The dimensions of the viewport to start with
	 */
	private void buildContentSceneGraph(String viewId, int[] dimensions) {
		
		Viewpoint viewpoint = new Viewpoint();
		
		contentContentGroup = new Group();
		
		SharedNode shared_content = new SharedNode();
		shared_content.setChild(contentContentGroup);
		
		contentRootGroup = new Group();
		contentRootGroup.addChild(shared_content);
		
		ColorBackground background =
			new ColorBackground(DEFAULT_BACKGROUND_COLOR);
		contentRootGroup.addChild(background);
		
		hqViewManager.backgroundChanged(DEFAULT_BACKGROUND_COLOR);
		hqViewManager.addBackground(background);
		
		// Pretend to resize the view with our initial dimensions so that
		// any later callers get a non-zero dimension when getWindowWidth()
		// is called.
		hqViewManager.graphicsDeviceResized(dimensions[0],
			dimensions[1],
			dimensions[2],
			dimensions[3]);
		
		SimpleScene scene = new SimpleScene();
		scene.setRenderedGeometry(contentRootGroup);
		scene.setActiveView(viewpoint);
		scene.setActiveBackground(background);
		
		contentViewport = new SimpleViewport();
		contentViewport.setDimensions(dimensions[0],
			dimensions[1],
			dimensions[2],
			dimensions[3]);
		
		contentViewport.setScene(scene);
		layerRootScene = scene;
		//////////////////////////////////////////////////////////////
		// hack to get the viewpoint rotated to the 'new' convention
		// create a transform group to hold that viewpoint
		viewpointTransform = new TransformGroup();
		viewpointTransform.addChild(viewpoint);
		
		navigationMatrix = new Matrix4f();
		navigationMatrix.setIdentity();
		
		navigationTransform = new TransformGroup();
		navigationTransform.addChild(viewpointTransform);
		
		contentRootGroup.addChild(navigationTransform);
		
		// any configuration of the layer viewpoint transform group
		// should occur -before- instantiating the nav manager
		//Vector3f translation = new Vector3f(0, 1.5f, 10);
		Vector3f translation = new Vector3f(STARTING_VIEWPOINT_POSITION);       
		
		Matrix4f mtx = new Matrix4f();
		mtx.setIdentity();
		mtx.setTranslation(translation);
		viewpointTransform.setTransform(mtx);
		///////////////////////////////////////////////////////////////////////
		
		visHandler =
			new PreviewVisibilityHandler(mgmtObserver, navigationMatrix);
		
		///////////////////////////////////////////////////////////////////////
		
		SimpleNavigationCollisionManager nc =
			new SimpleNavigationCollisionManager(contentContentGroup, 0.1f);
		nc.setNavigationTransform(navigationTransform);
		
		///////////////////////////////////////////////////////////////////////
		
		navStatusManager.setErrorReporter(errorReporter);
		navStatusManager.addNavigationStatusListener(visHandler);
		navStatusManager.addNavigationStatusListener(hqViewManager);
		
		///////////////////////////////////////////////////////////////////////
		
		navManager = new DefaultNavigationManager(
			viewId,
			model,
			controller,
			errorReporter,
			this,
			contentRootGroup,
			navigationTransform,
			mgmtObserver,
			nc,
			navStatusManager);
		
		///////////////////////////////////////////////////////////////////////
		locationEntityMap = new HashMap<Integer, LocationEntity>();
		locationGroupMap = new HashMap<Integer, Group>();
		locationManagerMap = new HashMap<Integer, AV3DEntityManager>();
		
		nodeAddList = new ArrayList<Node>();
		nodeRemoveList = new ArrayList<Node>();
		
		// check for the existence of a SceneEntity
		Entity[] rootEntities = model.getModelData();
		for (int i = 0; i < rootEntities.length; i++) {
			if (rootEntities[i] instanceof SceneEntity) {
				// there should only be one....
				setSceneEntity((SceneEntity)rootEntities[i]);
				break;
			}
		}
		
		Group lights = new Group();
		
		if(ambientLightConfig != null) {
			AmbientLight ambientLight = new AmbientLight();
			ambientLight.setAmbientColor(ambientLightConfig.lightColor);
			ambientLight.setGlobalOnly(true);
			ambientLight.setEnabled(true);
			
			lights.addChild(ambientLight);
		}
		
		// Set up bounds for the lights
		
		for(int i = 0; i < pointLightConfig.length; i++) {
			
			PointLight pl = new PointLight(pointLightConfig[i].lightColor,
				pointLightConfig[i].lightPosition);
			
			BoundingVolume bounds =
				new BoundingSphere(pointLightConfig[i].lightRadius);
			
			pl.setEnabled(true);
			pl.setGlobalOnly(true);
			pl.setAttenuation(pointLightConfig[i].attenuation[0],
				pointLightConfig[i].attenuation[1],
				pointLightConfig[i].attenuation[2]);
			
			pl.setBounds(bounds);
			
			lights.addChild(pl);
			
			/*
			// Light place holder
						Appearance app1 = new Appearance();
						Material col1 = new Material();
						col1.setDiffuseColor(new float[] {1.0f, 0.0f, 0.0f});
						app1.setMaterial(col1);
						Box box1 = new Box(2.0f, 2.0f, 2.0f, app1);
						TransformGroup t1 = new TransformGroup();
						Matrix4f mat1 = new Matrix4f();
						mat1.setIdentity();
						Vector3f vec1 = new Vector3f(-20.0f, 20.0f, 20.0f);
						mat1.setTranslation(vec1);
						t1.setTransform(mat1);
						t1.addChild(box1);
						lights.addChild(t1);
			*/
		}
		
		for(int i = 0; i < directionalLightConfig.length; i++) {
			
			DirectionalLight pl = new DirectionalLight(
				directionalLightConfig[i].lightColor,
				directionalLightConfig[i].direction);
			
			BoundingVolume bounds =
				new BoundingSphere(directionalLightConfig[i].lightRadius);
			
			pl.setEnabled(true);
			pl.setGlobalOnly(true);
			
			pl.setBounds(bounds);
			
			lights.addChild(pl);
		}
		
		for(int i = 0; i < spotLightConfig.length; i++) {
			
			SpotLight pl =
				new SpotLight(spotLightConfig[i].lightColor,
				spotLightConfig[i].lightPosition,
				spotLightConfig[i].direction);
			
			BoundingVolume bounds =
				new BoundingSphere(spotLightConfig[i].lightRadius);
			
			pl. setCutOffAngle(spotLightConfig[i].angle);
			pl.setAttenuation(spotLightConfig[i].attenuation[0],
				spotLightConfig[i].attenuation[1],
				spotLightConfig[i].attenuation[2]);
			
			pl.setEnabled(true);
			pl.setGlobalOnly(true);
			pl.setBounds(bounds);
			
			lights.addChild(pl);
		}
		
		lightSwitch = new SwitchGroup();
		lightSwitch.addChild(lights);
		lightSwitch.setActiveChild(0);
		contentRootGroup.addChild(lightSwitch);
	}
}
