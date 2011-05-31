/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.awt.Font;
import java.awt.Color;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point2f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.LineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.QuadArray;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

//Local imports
import org.chefx3d.model.*;

import org.chefx3d.preferences.PersistentPreferenceConstants;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ConfigManager;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.View;
import org.chefx3d.view.ViewManager;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.*;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Implementation of the overlay LayerManager
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.136 $
 */
public class OverlayLayerManager extends AbstractLayerManager implements
    UserInputHandler,
    PerFrameUIObserver,
    ModelListener,
    EntityChildListener,
    EntitySelectionListener,
    EntityPropertyListener,
    NavigationStatusListener,
    ResizeListener,
    NodeUpdateListener,
    View,
    MouseListener {

    /** show the dimensional indicators.  replace with a properties lookup
     * using a boolean given at start-up.    */
    public static final Boolean SHOW_DIMENSIONAL_INDICATORS = true;

    /** show the vertical ruler?  replace with a properties lookup
     * using a boolean given at start-up.    */
    public static final Boolean SHOW_VERTICAL_RULER = true;

    /** show the horizonal ruler?  replace with a properties lookup
     * using a boolean given at start-up.    */
    public static final Boolean SHOW_HORIZONTAL_RULER = true;

    /** show the tape measure.  replace with a properties lookup
     * that changes this depending if we're in 'tape measure' mode or not */
    public static final Boolean SHOW_TAPE_MEASURE = false;

    /** show the dimensional guides.  replace with a properties lookup
     * that changes this depending if we're in 'tape measure' mode or not */
    public static final Boolean SHOW_DIMENSIONAL_GUIDES = true;

    /** show the grid overlay.  replace with a properties lookup
     * that changes this depending if we're in 'tape measure' mode or not */
    public static final Boolean SHOW_GRID_OVERLAY = true;

    /** show the nearest neighbor overlay.  replace with a properties lookup
     * that changes this depending if we're in 'tape measure' mode or not */
    public static final Boolean SHOW_NEAREST_NEIGHBOR = true;

    /** Grid overlay depth position in the scene.  */
    private static final float GRID_OVERLAY_Z_POS = -20f;

    /** Dimensional guide depth position in the scene. */
    private static final float DIMENSIONAL_GUIDE_Z_POS = -19f;
    
    /** Should we draw the fonts with fractional metrics or not? */
    private static final boolean FRACTIONAL_METRICS = true;

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /** The device manager */
    private DeviceManager deviceManager;

    /** The world model */
    protected WorldModel model;

    /** List of commands that have been buffered */
    protected CommandController bufferedCommands;

    /** Float array of length 6 containing the view frustum of the
     * location layer, as set by the NavigationStatusManager    */
    private double[] locationLayerViewFrustum;

    /** Float array of length 6 containing the view frustum of the
     * overlay layer, as set by the NavigationStatusManager    */
    private double[] overlayViewFrustum;
    
    /** distance, in meters, between each line of the grid overlay */
    private float gridOverlayInterval;

    //------------------------------------------------------------------------
    // Variables for the various overlay components
    //------------------------------------------------------------------------

    /** The horizontal ruler component */
    private Ruler horizRuler;

    /** The vertical ruler component */
    private Ruler vertiRuler;

	/** The ruler label text */
	private LegendText rulerLabelText;
	
    /** The x-positioning of the rulerLabelGroup */
    private int rlgX;

    /** The y-positioning of the rulerLabelGroup */
    private int rlgY;

    /** The tape measure component */
    private TapeMeasure tapeMeasure;

    /** The DimensionalIndicator component */
    private DimensionalIndicator dimensionalIndicator;
    
    /** The SegmentDimensionalIndicator component */
    private SegmentDimensionalIndicators segmentDimensionalIndicator;

    /** sticky snap lines overlay component */
    private StickySnaps stickySnaps;

    /** grid overlay component */
    private GridOverlay gridOverlay;

    /** vertical mouse position geometry group */
    private LineArray vertMousePositionGeom;
    
    /** horizontal mouse position geometry group */
    private LineArray horzMousePositionGeom;

    /** mouse position Switch group */
    private SwitchGroup mousePositionSwitchGroup;

    /** nearest neighbor overlay */
    private NearestNeighborOverlay nearestNeighbor;

    private Point3f northStart, northEnd, southStart, southEnd,
    eastStart,  eastEnd,  westStart,  westEnd;

    /**
     * The view matrix passed around via viewEnvironmentChanged event.
     * AKA the inverse of the "world matrix"
     * Observe that matrix.transform(on-screen coordinates) produces
     * real-world coordinates. <p> This matrix transforms points from
     * camera space to world space.  We use it to find world origins
     * in camera space.
     */
    private Matrix4f matrix;

    /**
     * The "world matrix", representing the camera's position and orientation.
     * AKA the inverse of the viewMatrix passed around by viewEnvironmentChanged
     * events. Observe that inverseMatrix.transform(real-world coordinates)
     * produces on-screen coordinates. <p> This inverseMatrix transforms points
     * from world space to camera space.  We use it to find the camera origin
     * (or position) in world space.
     */
    private Matrix4f inverseMatrix;

    /**
     * As of 3/23/2010, entity positioning information is all
     * relative to the parent of the currently-selected entity.<p>
     * For instance, an obstacle with center coordinates of (3, 2, 0) 
     * is offset from its parent by three units along the x-axis and 
     * 2 units along the y-axis.<p> Thus, to calculate actual on-screen 
     * positioning information we use the following code (from the
     * {@link #selectionChanged(int, boolean)} method) to grab the
     * transform relative to the world:<p>
     * <code>
     * AV3DEntityWrapper entityWrapper = 
     * entityWrapperMap.get(currentEntity.getParentEntityID());<p>
     * 
     * if( entityWrapper != null) {<br>&nbsp;&nbsp;
     * // get the transform to the root, and set the inverse<br>&nbsp;&nbsp;
     *		transformUtils.getLocalToVworld(<br>&nbsp;&nbsp;&nbsp;&nbsp;
     *			entityWrapper.transformGroup, transformToRootMtx);<br>                       
     *}</code><p>
     * Then, to calculate on-screen coords, we take our position and 
     * transform it by the transformToRootMatrix relativeToVworld, and then
     * transform THAT by the view matrix to get on-screen coordinates.
     */
    private Matrix4f transformToRootMtx;
    private Matrix4f transformToRootInverse;
    
    /**
     * This matrix is somewhat similar to the transformToRootMtx.  It is used
     * to store the matrix need to position the camera to face the currently
     * active zone.  This can be either top-down floor view, or a directly
     * front-on view whenever a wall segment is selected.<p> Used in the 
     * {@link #viewMatrixChanged(Matrix4f)} method in order to calculate the 
     * lower-left portion of a selected wall segment, so that we can offset 
     * the {@link #horizRuler} and {@link #vertiRuler} properly.
     */
    private Matrix4f transformToWallMtx;

    /** Transformation Utility class */
    private TransformUtils transformUtils;

    /** working variable used to calculate values for dimensional indicator */
    private double[] position;

    /** working variable used to calculate values for dimensional indicator */
    private float[] rotation;

    /** working variable used to calculate values for dimensional indicator.
     * contains float values! */
    private float[] bounds;

    /** Center of the overlay layer's view frustum.  Since the overlay layer
     * has dimensions equal to one meter per pixel, this means that this
     * variable translates to the center of the overlay in PIXEL SPACE.  IE:
     * if the overlay layer is 400 pixels wide by 600 pixels tall, then the
     * centerOfOverlayViewFrustum would be equal to (200, 300).             */
    Point2d centerOfOverlayViewFrustum = new Point2d();
    
    /** This variable represents the location layer's frustum offset from the origin. */
    Point2f centerOfLocationLayer = new Point2f();

    /** Which child of mousePositionSwitchGroup should be active? */
    private int activeMousePositionChild;

    /** float array containing positioning information for the mouse indicator */
    private float[] horzMouseIndicatorArray;

    /** float array containing positioning information for the mouse indicator */
    private float[] vertMouseIndicatorArray;

    /** the ratio of (overlayWidth / locationWidth)  */
    double widthRatio;

    /** the ratio of (overlayHeight / locationHeight)  */
    double heightRatio;

    /** The currently - selected entity */
    private Entity currentEntity;
    
    /** The list of all entities in the scene */
    private HashMap<Integer, Entity> entityMap;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper seletionHelper;

    /** Currently active zone */
    private Entity activeZone;

    /** This point represents the real-world location of the center
     * of the scene.  It is used to set the ruler viewpoints as well as to
     * calculate positioning of various items in the scene.  */
    private Point4f overlayCenterPoint;
    
    /** If TRUE, the nearest neighbor has been hidden by a mouse exit event */
    boolean nearestNeighborHiddenByMouseExit;

    /** How long is the currently-active segment? */
    private float segmentLength;
    
    /** Height of the active segment. Once used to position grid lines. */
    private float segmentHeight;
    
    /** Real world coordinates of the startVertex of the currently-active segment */
    private double[] segmentVertex;
    
    /** Set during {@link #setTool(Tool)}, used by StickySnaps */
    private Tool tool;

    /** A reference to the location layer manager (needed to get the
     * EntityWrapperMap, I believe) */
    private LocationLayerManager locationLayerReference;

    /** The current unit of measurement labeled by the horizontal ruler */
    private Unit currentUnit;
    
    private Unit fixedUnit;
    
    private Boolean staticUnitDisplay;
	
	/** Scratch vars */
	private Matrix4f mtx;
	private Vector3f translation;
	private EntityUtils eu;
	
    /**
     * Constructor
     *
     * @param id The layer id
     * @param dim The initial viewport dimensions in [x, y, width, height]
     * @param model The WorldModel
     * @param controller The CommandController
     * @param reporter The ErrorReporter instance to use or null
     * @param mgmtObserver The SceneManagerObserver
     * @param deviceManager The DeviceManager
     */
    OverlayLayerManager(
        int id,
        int[] dim,
        WorldModel worldModel,
        CommandController controller,
        ErrorReporter reporter,
        SceneManagerObserver mgmtObserverParam,
        DeviceManager deviceManagerParam,
        LocationLayerManager location) {

        super(id, dim);

        setErrorReporter(reporter);
        seletionHelper = EntitySelectionHelper.getEntitySelectionHelper();
        
        model = worldModel;
        model.addModelListener(this);
		
		eu = new EntityUtils(model);
		
        bufferedCommands = controller;

        deviceManager = deviceManagerParam;
        mgmtObserver = mgmtObserverParam;
        //mgmtObserver.addObserver(this);
        mgmtObserver.addUIObserver(this);

        //
        // variables for the dimensional indicators
        //
        position = new double[]{0, 0, 0};
        rotation = new float[]{0, 0, 0, 0};
        bounds = new float[]{0, 0, 0, 0, 0, 0};
		
		mtx = new Matrix4f();
		translation = new Vector3f();
		
        segmentVertex = new double[3];

        centerOfOverlayViewFrustum = new Point2d();

        entityMap = new HashMap<Integer, Entity>();

        //
        // initialize the basics of the ortho view environment
        //
        ViewEnvironment ve = scene.getViewEnvironment();
        ve.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        ve.setAspectRatio(0);
        ve.setClipDistance(0.001, 1000.0);

        locationLayerReference = location;
        overlayCenterPoint = new Point4f();
        transformUtils = new TransformUtils();
        transformToRootInverse = new Matrix4f();
        transformToRootInverse.setIdentity();

        //
        // these values are used for converting between the
        // location layer's "real-world" values and the
        // two-dimensional overlay layer's "one-to-one" values
        //
        locationLayerViewFrustum = new double[6];
        overlayViewFrustum = new double[6];
        inverseMatrix = new Matrix4f();
        inverseMatrix.setIdentity();
        transformToRootMtx = new Matrix4f();
        transformToRootMtx.setIdentity();
        transformToWallMtx = new Matrix4f();
        transformToWallMtx.setIdentity();

        //
        // what unit types are going to be displayed?
        //
        String appName = (String) ApplicationParams.get(
                ApplicationParams.APP_NAME);
        Preferences prefs = Preferences.userRoot().node(appName);
        String unitLabel = prefs.get(
        		PersistentPreferenceConstants.UNIT_OF_MEASUREMENT, 
        		PersistentPreferenceConstants.DEFAULT_UNIT_OF_MEASUREMENT);
        Unit unit = UnitConversionUtilities.getUnitByCode(unitLabel);
		
		String currentSystem = UnitConversionUtilities.getMeasurementSystem(unit);
        if (currentSystem == UnitConversionUtilities.IMPERIAL){
            // Imperial (American) units
            rlgX = 2;
            gridOverlayInterval = 
                UnitConversionUtilities.convertUnitsToMeters(1, Unit.FEET);            
        } else {
            // Metric units
            rlgX = 0;
            gridOverlayInterval = 1.0f;
        }
        rlgY = 13;

        staticUnitDisplay = false;
        
        boolean checkStaticUnit =
        	UnitConversionUtilities.isStaticOverlayUnitOfMeasure();            
        if (checkStaticUnit) {
        	fixedUnit = UnitConversionUtilities.getStaticOverlayUnitOfMeasure();
        	
        	// check to ensure the unit is for the current system
        	String fixedSystem = 
        		UnitConversionUtilities.getMeasurementSystem(fixedUnit);
        	
        	if (fixedSystem.equals(currentSystem)) {
        		staticUnitDisplay = true;
        	}
        }
        
    	if (staticUnitDisplay) {
    		unit = fixedUnit;
    	}
        
        //
        // create the avBuilder and the rulerGroup.  Note that rulerGroup
        // will set the default color and appearance for its own use,
        // the other OverlayComponents will have to specify their own
        // appearances.
        //
        AVGeometryBuilder avBuilder = new AVGeometryBuilder(FRACTIONAL_METRICS);
        horizRuler = new Ruler(mgmtObserverParam,
                               avBuilder,
                               unit,
                               Ruler.Orientation.HORIZONTAL,
                               500,
                               10,
                               0);
        horizRuler.enable(SHOW_HORIZONTAL_RULER);
        rootGroup.addChild(horizRuler.getSwitchGroup());

        vertiRuler = new Ruler(mgmtObserverParam,
                               avBuilder,
                               unit,
                               Ruler.Orientation.VERTICAL,
                               500,
                               10,
                               0);
        vertiRuler.enable(SHOW_VERTICAL_RULER);
        rootGroup.addChild(vertiRuler.getSwitchGroup());

		//
		// create the label for the ruler's unit of measurement
		initRulerLabel();
		
        //
        // create the mouse indicator group and an appropriate appearance
        //
		initMousePosition();
        
        //
        // create the dimensional indicator
        //
        dimensionalIndicator = new DimensionalIndicator(mgmtObserverParam,
                                                        avBuilder,
                                                        unit);
        dimensionalIndicator.enable(SHOW_DIMENSIONAL_INDICATORS);
        rootGroup.addChild(dimensionalIndicator.getSwitchGroup());
        
        // create the dimensional indicator
        //
        segmentDimensionalIndicator = 
        	new SegmentDimensionalIndicators(mgmtObserverParam,
        									 avBuilder,
        									 unit);
        segmentDimensionalIndicator.enable(true);
        rootGroup.addChild(segmentDimensionalIndicator.getSwitchGroup());

        //
        // create and set the four components:
        //
        
        //
        // now create the tape measure and pocket the tape measure appearance
        //
        tapeMeasure = new TapeMeasure(mgmtObserverParam,
                                      avBuilder,
                                      unit);
        tapeMeasure.enable(SHOW_TAPE_MEASURE);
        rootGroup.addChild(tapeMeasure.getSwitchGroup());

        //
        // create the dimensional guide
        //
        stickySnaps = new StickySnaps(mgmtObserverParam,
                                                avBuilder,
                                                unit);
        stickySnaps.enable(SHOW_DIMENSIONAL_GUIDES);
        rootGroup.addChild(stickySnaps.getSwitchGroup());

        //
        // create the grid overlay
        //
        gridOverlay = new GridOverlay(mgmtObserverParam, avBuilder, unit);
        gridOverlay.enable(SHOW_GRID_OVERLAY);
        rootGroup.addChild(gridOverlay.getSwitchGroup());

        //
        // create the nearest neighbor overlay
        //
        nearestNeighbor = new NearestNeighborOverlay(mgmtObserverParam,
                                                     avBuilder,
                                                     unit);
        nearestNeighbor.enable(SHOW_NEAREST_NEIGHBOR);
        rootGroup.addChild(nearestNeighbor.getSwitchGroup());


        ViewManager.getViewManager().addView(this);
    }


    //------------------------------------------------------------------------
    // Methods required by ResizeListener
    //------------------------------------------------------------------------

    /**
     * This may eventually be changed. <p> Currently resize Listener
     * works by passing the ratio of new width to old width and the ratio of
     * new height to old height.  In the case of the "Real-world" location
     * layer, scaling the height and width of the viewfrustum by floating point
     * values accumulates round-off errors.<p>
     *
     * However, these floating-point ratios are acceptable for use
     * of overlay layer manager.  Since this "one-to-one" overlay layer has
     * one meter per pixel, it uses Math.round() to convert the floating
     * point values into integer values. <p>
     *
     *  Note that if the current height was brought down to zero and then up
     *  again, the ratio of new height to old height would be NaN.  Due to
     *  the minimum size of the window, this can't currently happen.
     *  Defensive coding should be used instead, to prepare for the
     *  case of resizing down to zero and back again instead of
     *  abusing prior knowledge of minimum window size.
     */
    public void sizeChanged(int newWidth, int newHeight){

        ViewEnvironment ve = scene.getViewEnvironment();

        // width and height should never be negative
        if (newWidth <= 0 || newHeight <= 0) {
            return;
        }
        
        ve.setOrthoParams(0,
                          newWidth,
                          0,
                          newHeight);

        horizRuler.setScreenPixelSize(newWidth);
        vertiRuler.setScreenPixelSize(newHeight);

        // since the overlay view frustum has changed, update the ratios
        ve.getViewFrustum(overlayViewFrustum);
        updateOverlayToLocationRatios();
    }

    /**
     * Call this method internally whenever the overlay frustum changes size
     * (as it does after resizing) and whenever the location frustum changes
     * size (as it does after zooming in and out).
     * Update both the widthRatio, which is the ratio of
     * (overlayWidth / locationWidth), and heightRatio, which is the ratio of
     * (overlayHeight / locationHeight).
     */
    private void updateOverlayToLocationRatios(){
        //
        // compare the width of the overlay to the width of the locationLayer
        //
        double overlayWidth  = overlayViewFrustum[1] - overlayViewFrustum[0];
        double overlayHeight = overlayViewFrustum[3] - overlayViewFrustum[2];

        double locationWidth  = locationLayerViewFrustum[1] - locationLayerViewFrustum[0];
        double locationHeight = locationLayerViewFrustum[3] - locationLayerViewFrustum[2];

        widthRatio = overlayWidth / locationWidth;
        heightRatio = overlayHeight / locationHeight;

        // grab center of the overlay layer
        centerOfOverlayViewFrustum.set(
			(overlayViewFrustum[0]+overlayViewFrustum[1]) * 0.5f,
            (overlayViewFrustum[2]+overlayViewFrustum[3]) * 0.5f);

        //
        // update certain overlay components
        //
        updateGridOverlay();
        updateDimensionalIndicator();
        updateNearestNeighborOverlay();
        updateSegmentDimensionalIndicator(currentEntity);
        
        // update sticky snaps only if current entity is still selected
        ArrayList<Entity> selectedList =
            seletionHelper.getSelectedList();
        
        if(currentEntity != null && 
        		selectedList.size() == 1 && 
        		selectedList.get(0) == currentEntity){

            updateStickySnaps(true);
        }

        if(tapeMeasure.display) {
            tapeMeasure.drawMeasure(widthRatio);
		}
        horizRuler.redraw();
        vertiRuler.redraw();
		
		Unit newUnit = horizRuler.getUnit();
		if (newUnit != currentUnit) {
			currentUnit = newUnit;
			
			if (staticUnitDisplay) {
				currentUnit = fixedUnit;
				horizRuler.setUnitOfMeasurement(fixedUnit);
				vertiRuler.setUnitOfMeasurement(fixedUnit);
			}
			
			rulerLabelText.update(
				currentUnit.getLabel(), 
				0, 
				Anchor.TOP_LEFT, 
				null);
			
			dimensionalIndicator.setUnitOfMeasurement(currentUnit);
			nearestNeighbor.setUnitOfMeasurement(currentUnit);
			tapeMeasure.setUnitOfMeasurement(currentUnit);
			segmentDimensionalIndicator.setUnitOfMeasurement(currentUnit);
		}
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

        locationLayerViewFrustum[0] = frustumCoords[0];
        locationLayerViewFrustum[1] = frustumCoords[1];
        locationLayerViewFrustum[2] = frustumCoords[2];
        locationLayerViewFrustum[3] = frustumCoords[3];
        locationLayerViewFrustum[4] = frustumCoords[4];
        locationLayerViewFrustum[5] = frustumCoords[5];

        // update the ruler's width and height
        horizRuler.setScreenSizeMeters( (float)
            (locationLayerViewFrustum[1] - locationLayerViewFrustum[0]));

        vertiRuler.setScreenSizeMeters( (float)
                (locationLayerViewFrustum[3] - locationLayerViewFrustum[2]));

        // since the locationLayer frustum has changed, update the ratios
        updateOverlayToLocationRatios();
    }


    /**
     * @param mtx The location layer's view matrix
     */
    public void viewMatrixChanged(Matrix4f mtx) {

        matrix = mtx;//new Matrix4f(mtx);

        inverseMatrix = new Matrix4f();
        inverseMatrix.invert(mtx);

        overlayCenterPoint.x = mtx.m03;
        overlayCenterPoint.y = mtx.m13;
        overlayCenterPoint.z = mtx.m23;
        overlayCenterPoint.w = 0;

        inverseMatrix.transform(overlayCenterPoint);

        Point3f zero = new Point3f(0, 0, 0);
        transformToWallMtx.transform(zero);
        inverseMatrix.transform(zero);
        
        centerOfLocationLayer.set(-zero.x, -zero.y);
        
        horizRuler.setScreenCenter(centerOfLocationLayer.x);
        vertiRuler.setScreenCenter(centerOfLocationLayer.y);

        updateOverlayToLocationRatios();
    }


    //---------------------------------------------------------------
    // Methods defined by PerFrameUIObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrameUI() {
        deviceManager.processTrackers(id, this);
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

        recursiveAdd(entity);
    }

    /**
     * An entity was removed.
     *
     * @param local Is the request local
     * @param entity The entity to remove
     */
    public void entityRemoved(boolean local, Entity entity) {

        recursiveRemove(entity);
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
//      System.out.println("modelReset");
    }

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
//      System.out.println("viewChanged");
    }

    /**
     * The master view has changed.
     *
     * @param local Is the request local
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
//      System.out.println("masterChanged");
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

        Entity parentEntity = entityMap.get(parentID);
        int index = parentEntity.getChildIndex(childID);
        Entity childEntity = parentEntity.getChildAt(index);

        recursiveAdd(childEntity);
    }

    
    /** A child was inserted. */
    public void childInsertedAt(int parentID, int childID, int index) {

        Entity parentEntity = entityMap.get(parentID);
        Entity childEntity = parentEntity.getChildAt(index);

        recursiveAdd(childEntity);
    }

    
    /**
     * A child was removed.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childRemoved(int parentID, int childID) {

        if ((currentEntity != null) && (childID == currentEntity.getEntityID())) {
            if(currentEntity instanceof VertexEntity ||
                    currentEntity instanceof SegmentEntity) {
                segmentDimensionalIndicator.display(false);
            }
            
            currentEntity = null;
            dimensionalIndicator.display(false);  
        }
        Entity childEntity = entityMap.get(childID);
        if (childEntity != null) {
            recursiveRemove(childEntity);
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

        ArrayList<Entity> selectedList =
            seletionHelper.getSelectedList();

        // if unselected, or during multi-selection, hide components
        if ( !selected || selectedList.size() > 1) {
            dimensionalIndicator.display(false);
            nearestNeighbor.display(false);
            nearestNeighborHiddenByMouseExit = false;
            segmentDimensionalIndicator.display(false);
            segmentDimensionalIndicator.createTextNodes(0);
            stickySnaps.display(false);

            // EMF: we don't want to clear the locationEntity's
        	// entity Property list or we will miss zone change updates
            if (currentEntity != null && !(currentEntity instanceof LocationEntity)){
                currentEntity.removeEntityPropertyListener(this);
                currentEntity = null;
            }

        } else if(selectedList.size() > 0) {

            // use the selected entity
            Entity entity = entityMap.get(entityID);

            if (entity != null) {

                // EMF: we don't want to clear the locationEntity's
            	// entity Property list or we will miss zone change updates
                if (currentEntity != null && !(currentEntity instanceof LocationEntity))
                    currentEntity.removeEntityPropertyListener(this);


                if(currentEntity instanceof SegmentEntity){
                    SegmentEntity segmentEntity = ((SegmentEntity)currentEntity);
                    VertexEntity v1 = segmentEntity.getStartVertexEntity();
                    VertexEntity v2 = segmentEntity.getEndVertexEntity();
                    v1.removeEntityPropertyListener(this);
                    v2.removeEntityPropertyListener(this);
                }

                currentEntity = entity;
                currentEntity.addEntityPropertyListener(this);
                

                if(currentEntity instanceof SegmentEntity){
                    SegmentEntity segmentEntity = ((SegmentEntity)currentEntity);
                    VertexEntity v1 = segmentEntity.getStartVertexEntity();
                    VertexEntity v2 = segmentEntity.getEndVertexEntity();
                    v1.addEntityPropertyListener(this);
                    v2.addEntityPropertyListener(this);
                }
                
                int type = currentEntity.getType();
                if(type == Entity.TYPE_VERTEX||
                        type == Entity.TYPE_SEGMENT) {
                    
                    int size = 0;
                    size = findAllTouchingSegments(currentEntity).size();
                   
                    segmentDimensionalIndicator.createTextNodes(size);
                }

                // draw components if and only if there is a single-selection event
                if(selected && selectedList.size() < 2){

                    updateSegmentDimensionalIndicator(currentEntity);
                    updateDimensionalIndicator();
                    updateNearestNeighborOverlay();
                    updateStickySnaps(true);
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
    // Methods for EntityPropertyListener
    //----------------------------------------------------------

    /** a set of properties have updated */
    public void propertiesUpdated(List<EntityProperty> properties) {

    }

    /** a property was added */
    public void propertyAdded(int entityID, String propertySheet,
        String propertyName) {

    }

    /** a property was removed */
    public void propertyRemoved(int entityID, String propertySheet,
        String propertyName) {

    }

    /** A property was updated. <br>
     * EMF: Note that we register as EntityPropertyListeners in one of two
     * places: {@link #selectionChanged(int, boolean)} when an entity is 
     * selected, and {@link #recursiveAdd(Entity)} in the case that we're
     * placing a new wall by moving a shadow vertex */
    public void propertyUpdated(int entityID, String propertySheet,
        String propertyName, boolean ongoing) {
        
        if (propertyName.equals(PositionableEntity.POSITION_PROP) ||
            propertyName.equals(PositionableEntity.ROTATION_PROP) ||
            propertyName.equals(PositionableEntity.SCALE_PROP)){
            
            updateDimensionalIndicator();
            updateNearestNeighborOverlay();
            
            //
            // If this is a vertex entity, then we need to update the 
            // length indicator overlay, to help customers with wall placement.
            //
            updateSegmentDimensionalIndicator(model.getEntity(entityID));
            
            // TODO: CALL THIS LESS FREQUENTLY - ONLY WHEN
            // PARENT CHANGES
            updateStickySnaps(true);
                       
            
        } else if (propertyName.equals(LocationEntity.ACTIVE_ZONE_PROP)) {
            nearestNeighborHiddenByMouseExit = false;
//        	System.out.println("zone change, active zone property change");
            zoneChanged(entityID);
        }
    }

    
    //---------------------------------------------------------------
    // Methods defined by UserInputHandler
    //---------------------------------------------------------------

    /**
     * Process a tracker press event. This may be used to start a touchSensor,
     * start of a drag sensor or navigation
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerPressed(int tracker, TrackerState evt) {
        //System.out.println("trackerPressed: " + java.util.Arrays.toString(evt.worldPos));
        
        Boolean printMousePos = 
            (Boolean)ApplicationParams.get("printMousePos");
        if (printMousePos != null && printMousePos) {
            System.out.println(java.util.Arrays.toString(evt.worldPos));
        }

    }


    /**
     * Process a tracker move event to update the tapeMeasure and the
     * mouse position indicators.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerMoved(int tracker, TrackerState evt) {
        
//        System.out.println("trackerMoved: "+ java.util.Arrays.toString(evt.worldPos));
         horzMouseIndicatorArray[0] = 0;
         horzMouseIndicatorArray[1] = evt.worldPos[1];
         //horzMouseIndicator[2] = evt.worldPos[2];
         horzMouseIndicatorArray[3] = Ruler.RULER_FATNESS;
         horzMouseIndicatorArray[4] = evt.worldPos[1];
         //horzMouseIndicator[5] = evt.worldPos[2];

         vertMouseIndicatorArray[0] = evt.worldPos[0];
         vertMouseIndicatorArray[1] = 0;
         //vertMouseIndicator[2] = evt.worldPos[2];
         vertMouseIndicatorArray[3] = evt.worldPos[0];
         vertMouseIndicatorArray[4] = Ruler.RULER_FATNESS;
         //vertMouseIndicator[5] = evt.worldPos[2];

         //
         // update the tape measure (it won't display unless both
         // tapeMeasure.display && tapeMeasure.enabled are true)
         //
         if(tapeMeasure.isStarted()){
             tapeMeasure.continueTape(evt.worldPos);
             tapeMeasure.drawMeasure(widthRatio);
         }

        updateMousePositionIndicators();
    }

    /**
     * Process a tracker press event. This may be used to start a touchtracker,
     * start of a drag tracker or navigation
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerDragged(int tracker, TrackerState evt) {
		//System.out.println("trackerDragged: "+ state);
		horzMouseIndicatorArray[0] = 0;
		horzMouseIndicatorArray[1] = evt.worldPos[1];
		//horzMouseIndicator[2] = evt.worldPos[2];
		horzMouseIndicatorArray[3] = Ruler.RULER_FATNESS;
		horzMouseIndicatorArray[4] = evt.worldPos[1];
		//horzMouseIndicator[5] = evt.worldPos[2];
		
		vertMouseIndicatorArray[0] = evt.worldPos[0];
		vertMouseIndicatorArray[1] = 0;
		//vertMouseIndicator[2] = evt.worldPos[2];
		vertMouseIndicatorArray[3] = evt.worldPos[0];
		vertMouseIndicatorArray[4] = Ruler.RULER_FATNESS;
		//vertMouseIndicator[5] = evt.worldPos[2];
		
		updateMousePositionIndicators();
    }

    /**
     * Process a tracker release event. This may be used to
     * start a touchtracker, start of a drag tracker or navigation.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerReleased(int tracker, TrackerState evt) {
//        System.out.println("trackerReleased: " + java.util.Arrays.toString(evt.worldPos));
    }

    /**
     * Process a tracker click event. The click is used only on touch trackers
     * and anchors. We treat it like a cross between a select and unselect.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerClicked(int tracker, TrackerState evt) {
        // TODO: Tracker clicked events are currently NOT firing.
        // Work around is to used the mouseClicked method; to actually examine
        // why the deviceManager isn't getting fired, pursue the MouseDeviceClass

        //System.out.println("trackerClicked: " + java.util.Arrays.toString(evt.worldPos));
    }

    /**
     * Process a tracker orientation event. This is for trackers like HMDs that
     * can change orientation without changing position or other state.
     *
     * @param tracker The id of the tracker calling this handler
     * @param evt The event that caused the method to be called
     */
    public void trackerOrientation(int tracker, TrackerState evt) {
//    	System.out.println("trackerOrientation: " + java.util.Arrays.toString(evt.worldPos));
    }

    /**
     * Process the buttons on a tracker.  No other state will be read.
     *
     * @param tracker The id of the tracker calling this handler
     * @param state The current state.
     */
    public void trackerButton(int tracker, TrackerState state) {
//        System.out.println("trackerButton: "+ state);
    }

    /**
     * Process the wheel on a tracker.  No other state will be read.
     *
     * @param tracker The id of the tracker calling this handler
     * @param state The current state.
     */
    public void trackerWheel(int tracker, TrackerState state) {
//        System.out.println("trackerWheel: "+ state);
    }

    //------------------------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //------------------------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {

        if (src == vertMousePositionGeom) {
            
            vertMousePositionGeom.setVertices (
				LineArray.COORDINATE_3, 
				vertMouseIndicatorArray);
            
        } else if (src == horzMousePositionGeom) {
			
            horzMousePositionGeom.setVertices(
				LineArray.COORDINATE_3, 
				horzMouseIndicatorArray);
			
        } else if ( src == mousePositionSwitchGroup) {
			
            mousePositionSwitchGroup.setActiveChild(activeMousePositionChild);
        }
    }
	
    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
        // ignored for now
    }

    //------------------------------------------------------------------------
    // Methods defined by MouseListener
    //------------------------------------------------------------------------

    /**
     * Notification that the pointing device has entered the
     * graphics component
     */
    public void mouseEntered(MouseEvent e) {
        activeMousePositionChild = -0;

        if(nearestNeighborHiddenByMouseExit){
            nearestNeighbor.display(true);
            nearestNeighborHiddenByMouseExit = false;
        }
		
        mgmtObserver.requestBoundsUpdate(mousePositionSwitchGroup, this);
    }

    
    /**
     * Notification that the pointing device has left the
     * graphics component
     */
    public void mouseExited(MouseEvent e) {
        activeMousePositionChild = -1;

        if(nearestNeighbor.display && nearestNeighbor.enabled){
            nearestNeighbor.display(false);
            nearestNeighborHiddenByMouseExit = true;
        }
		
        mgmtObserver.requestBoundsUpdate(mousePositionSwitchGroup, this);
    }

    
    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e){

    	float[] worldPos = new float[]{ e.getX(), 
        								(float)overlayViewFrustum[3]-e.getY()};
        //Point2f p = convertOverlayCoordsToWorldPos(worldPos);
        
        if( e.getButton() == MouseEvent.BUTTON3 ){
        	//
        	// hide the tape measure on right-click
        	//
        	tapeMeasure.endTape(worldPos);
        	tapeMeasure.display(false);
        	
        } else if(tapeMeasure.isStarted()){

            tapeMeasure.endTape(worldPos);
            tapeMeasure.drawMeasure(widthRatio);
        }
        else {
            tapeMeasure.startTape(worldPos);
        }
    }

    
    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e){
        // ignored
    }

    
    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e){
        // ignored
    }
    
    //------------------------------------------------------------------------
    // Methods defined by View: ignored, other than setTool()
    //------------------------------------------------------------------------
    
    public void shutdown(){
        // ignored
    }

    public void controlChanged(int newMode) {
        // ignored
    }

    public void disableAssociateMode() {
        // ignored
    }

    public void enableAssociateMode(String[] validTools, String propertyGroup,
            String propertyName) {
        // ignored
    }

    public EntityBuilder getEntityBuilder() {
        // ignored
        return null;
    }

    public long getViewID() {
        // ignored
        return 0;
    }

    public void setEntityBuilder(EntityBuilder entityBuilder) {
        // ignored
    }

    public void setHelperDisplayMode(int mode) {
        // ignored
    }

    public void setTool(Tool newTool) {
        tool = newTool;
        updateStickySnaps(false);

//        System.out.println("\n\n");
//        org.j3d.renderer.aviatrix3d.util.ScenePrinter sp = new org.j3d.renderer.aviatrix3d.util.ScenePrinter();
//        sp.enableBoundsPrinting(true);
//        sp.enableHashCodePrinting(true);
//        sp.enableDashPrinting(true);
//        if(locationLayerReference != null && locationLayerReference.rootGroup != null)
//          sp.dumpGraph(locationLayerReference.rootGroup);
    }


    //------------------------------------------------------------------------
    // Local Methods
    //------------------------------------------------------------------------    


    /**
     *  Zone has changed?  Grab the activeLocationEntity and from there
     *  grab the activeZoneID.  Use that to configure the zoneView and the
     *  ZoneOrientation.
     */
    private void zoneChanged(int entityID){

        LocationEntity activeLocationEntity =
            (LocationEntity)model.getEntity(entityID);

        if (activeLocationEntity != null) {

            int zoneID = activeLocationEntity.getActiveZoneID();
            activeZone = model.getEntity(zoneID);

            //
            // Get the entityWrapperMap instance from the AV3DEntityManager
            // in order to lookup the wrappers
            //
            HashMap<Integer, AV3DEntityWrapper> entityWrapperMap =
                locationLayerReference.getAV3DEntityWrapperMap();
            //
            // with the entity, look up its wrapper in the map
            //
            AV3DEntityWrapper entityWrapper =
                entityWrapperMap.get(zoneID);
            if( entityWrapper == null) {
                return;
			}
            //
            // get the transform to the root, and set the inverse.
            // also repeat the process, saving information for the wall 
            // matrix.
            // the transformToWallMtx is used to help line up rulers 
            // when a wall segment is the active zone.
            //
            transformUtils.getLocalToVworld(entityWrapper.transformGroup,
                                            transformToRootMtx);
            transformUtils.getLocalToVworld(entityWrapper.transformGroup,
                    						transformToWallMtx);
            
            transformToRootInverse = new Matrix4f();
            transformToRootInverse.invert(transformToRootMtx);
            
            updateNearestNeighborOverlay();

            if( activeZone instanceof SegmentEntity ){
            	
            	SegmentEntity activeSeg = (SegmentEntity)activeZone; 
            	segmentLength = activeSeg.getLength();
            	segmentHeight = activeSeg.getHeight();
            	activeSeg.getStartVertexEntity().getPosition(segmentVertex);

                horizRuler.setRulerStartAndEnd(0, segmentLength);
                vertiRuler.setRulerStartAndEnd(0, segmentHeight);

            } else {
                horizRuler.drawFullRuler();
                vertiRuler.drawFullRuler();
            }

            // redraw or remove the dimensional indicators and other components
            dimensionalIndicator.display(false);
            segmentDimensionalIndicator.display(false);
            tapeMeasure.display(false);
            setTool(tool);
        }
    }
        

    /**
     * Finds all the segments that are connected to a vertex and returns them in 
     * an array List
     * @author Jon Hubba
     * @param vertex
     * @return
     */
    private ArrayList<SegmentEntity> findAllTouchingSegments(Entity entity){
        ArrayList<SegmentEntity> returnList = new ArrayList<SegmentEntity>();
        
        SegmentableEntity segmentableEntity =
            (SegmentableEntity)model.getEntity(entity.getParentEntityID());
        ArrayList<SegmentEntity> allSegments = segmentableEntity.getSegments();
        
        if(entity instanceof VertexEntity) {
            int vertexID = entity.getEntityID();
            
            
            for(int i = 0; i < allSegments.size();i++) {
                
                int startID = allSegments.get(i).getStartVertexEntity().getEntityID();
                int endID = allSegments.get(i).getEndVertexEntity().getEntityID();
                
                if(vertexID == startID || vertexID == endID) {
                    returnList.add(allSegments.get(i));
                }
                
            }
        } else if( entity instanceof SegmentEntity ) {
            int startVertexID = ((SegmentEntity)entity).getStartVertexEntity().getEntityID();
            int endVertexID = ((SegmentEntity)entity).getEndVertexEntity().getEntityID();
            
            for(int i = 0; i < allSegments.size();i++) {
                
                int startID = allSegments.get(i).getStartVertexEntity().getEntityID();
                int endID = allSegments.get(i).getEndVertexEntity().getEntityID();
                
                if(startVertexID == startID || 
                        startVertexID == endID||
                        endVertexID == startID || 
                        endVertexID == endID) {
                    returnList.add(allSegments.get(i));
                }
            }
        }
        return returnList;
    }

    /**
     * Return the UserInputHandler for this layer
     *
     * @return The UserInputHandler
     */
    UserInputHandler getUserInputHandler() {
        return this;
    }
    
    
    /**
     * Update the mouse position indicators. 
     */
    public void updateMousePositionIndicators(){
        mgmtObserver.requestBoundsUpdate(vertMousePositionGeom, this);
        mgmtObserver.requestBoundsUpdate(horzMousePositionGeom, this);
    }

    /**
     * Perform actions that need to occur during step selection events,
     * in particular hide certain overlay components.
     */
    public void stepSelected(){
        //
        // perform actions that need to trigger during step selection events
        //
        tapeMeasure.display(false);
        nearestNeighborHiddenByMouseExit = false;
    }

    /**
     * Switch the grid overlay on or off
     * @param display if TRUE, enable the grid overlay;
     * else disable the grid overlay.  Grid overlay will also
     * call display() to guarantee that it is shown if
     * enabled is TRUE.
     */
    public void enableGridOverlay(boolean display){
        gridOverlay.enable(display);
        gridOverlay.display(display);
    }


    /**
     * Switch the tapeMeasure on or off
     * @param enable if TRUE, enable the tape measure;
     * else disable the tape measure.
     */
    public void enableTapeMeasure(boolean enable){
        tapeMeasure.enable(enable);
        if( !enable ) // do this to set isMeasuring to false
            tapeMeasure.endTape(new float[]{0, 0});
    }


    /**
     * Switch the nearest neighbor overlay on and off
     * @param enable if TRUE, enable the nearest neighbor overlay;
     * else disable the nearest neighbor overlay.
     */
    public void enableNearestNeighbor(boolean enable){
        nearestNeighbor.enable(enable);
        nearestNeighbor.display(enable);
    }


    /**
     * Switch the dimensional guides overlay on and off
     * @param enable if TRUE, enable the dimensional guide;
     * else disable the dimensional guide.
     */
    public void enableDimensionalGuide(boolean enable){
        stickySnaps.enable(enable);
        stickySnaps.display(enable);
    }


    /**
     * Switch the dimensional indicators overlay on and off
     * @param enable if TRUE, enable the dimensional guide;
     * else disable the dimensional guide.
     */
    public void enableDimensionalIndicator(boolean enable){
//		updateSegmentDimensionalIndicator(null);
//    	segmentDimensionalIndicator.enable(enable);
//      segmentDimensionalIndicator.display(enable);
        dimensionalIndicator.enable(enable);
        dimensionalIndicator.display(enable);
        
    }


    /**
     * 
     * @param entitySelected boolean - if TRUE, then an entity
     * that already exists has been selected.  If FALSE, then 
     * a tool has been selected but an entity has not yet been placed
     * into the scene.
     */
    public void updateStickySnaps(boolean entitySelected){

        if(matrix == null) return;
        
        if( !entitySelected && tool == null) {
        	stickySnaps.display(false);
        	return;
        }
        
        //
        // First off, does this entity or tool use sticky snaps?
        // Also, why do we even need to look at the usesStickySnaps
        Boolean usesStickySnaps = (Boolean)RulePropertyAccessor.getProperty(
                tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.USES_STICKY_SNAPS_PROP);
        
        if( usesStickySnaps == null || usesStickySnaps == false){
        	stickySnaps.display(false);
        	return;
        } else {
        	stickySnaps.display(true);
        }
        
        // Get the sticky snap indices
        int[] stickySnapIndices = (int[])RulePropertyAccessor.getProperty(
        	tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
            ChefX3DRuleProperties.STICKY_SNAP_INDEX_PROP);
        
        // Check if snap should be calculated relative to floor or parent
		Boolean snapToFloor = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					currentEntity, 
					ChefX3DRuleProperties.SNAP_RELATIVE_TO_FLOOR);
        
        //
        // variables for calculating the number and position of snaps
        Boolean usesIncrementalSnaps = null;
        Boolean usesAbsoluteSnaps = null;
        Float xAxisIncSnap = null;
        Float yAxisIncSnap = null;
        Float zAxisIncSnap = null;
        int numHorizLines = 0;
        int numVertiLines = 0;
        float[] xAxisAbsSnap = null;
        float[] yAxisAbsSnap = null;
        float[] zAxisAbsSnap = null;        

        //
        // worldPosStartVertex should represent the lower-left corner of 
        // the parent - often the segment entity, or wall
        //
        double[] worldPosStartVertex = new double[]{
        		segmentVertex[0], segmentVertex[1], 0};

        float[] overlayPosStartVertex = 
        	convertWorldPosToOverlayCoords(worldPosStartVertex);
        
        //
        // extract better position data when a non-wall parent exists.
        // Do this only if snapToFloor is false.
        //
        if(entitySelected && !snapToFloor){
            
            // get parent
            PositionableEntity parentEntity = (PositionableEntity)
            	currentEntity.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES, 
                        "POSSIBLE_PARENT");

            // If we have a parent, align snaps relative to it's bottom left 
            // corner. If we don't, then disable sticky snap display and
            // return.
            if (parentEntity != null) {

            	double[] localToZonePos = new double[] {0.0, 0.0, 0.0};

            	boolean result = 
                	TransformUtils.getLocalToZone(
                			model, 
                			parentEntity, 
                			localToZonePos);
                
                if (result) {
                
                	if (parentEntity.isModel()) {
                		
                		float[] parentBounds = new float[6];
                		parentEntity.getBounds(parentBounds);
                		
                		// Adjust by left and bottom bounds edges
                		localToZonePos[0] += parentBounds[0];
                		localToZonePos[1] += parentBounds[2];
                	}
	                
	                Point3f position = new Point3f(
	                        (float)localToZonePos[0], 
	                        (float)localToZonePos[1], 
	                        (float)localToZonePos[2]);

	                // convert from local zone space to overlay space
	                convertLocalPosToOverlayCoords(position);

	                overlayPosStartVertex[0] = position.x;
	                overlayPosStartVertex[1] = position.y;
	                overlayPosStartVertex[2] = position.z;
	                
                } else {
                	stickySnaps.display(false);
    	        	return;
                }
                
	        } else {
	        	stickySnaps.display(false);
	        	return;
	        }
        }
        
        //
        // Get the snap type values
        //
    	usesIncrementalSnaps = (Boolean) RulePropertyAccessor.getProperty(
    		tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
            ChefX3DRuleProperties.MOVEMENT_USES_INCREMENTAL_SNAPS_PROP);
            
        usesAbsoluteSnaps = (Boolean) RulePropertyAccessor.getProperty(
        	tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
            ChefX3DRuleProperties.MOVEMENT_USES_ABSOLUTE_SNAPS_PROP);
        
        if (usesIncrementalSnaps == null) {
        	usesIncrementalSnaps = false;
		}
        if (usesAbsoluteSnaps == null) {
        	usesAbsoluteSnaps = false;
		}
        
        if( usesIncrementalSnaps){
        	
        	//
        	// Get the incremental snap values
        	//
        	xAxisIncSnap = (Float) RulePropertyAccessor.getProperty(
                tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_X_AXIS_SNAP_PROP);
        	
    		yAxisIncSnap = (Float) RulePropertyAccessor.getProperty(
    	        tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
    			ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Y_AXIS_SNAP_PROP);

    		zAxisIncSnap = (Float) RulePropertyAccessor.getProperty(
    	        tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
    			ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Z_AXIS_SNAP_PROP);
        	        	
        	/*
            // If this entity uses incremental snaps, draw one line 
        	// per incremental snap as appropriate
            //
        	if(xAxisIncSnap != null && xAxisIncSnap > 0)
            	numVertiLines = (int)(segmentLength / xAxisIncSnap);
            if(yAxisIncSnap != null && yAxisIncSnap > 0)
            	numHorizLines = (int)(segmentHeight / yAxisIncSnap);*/
        	
        } else if( usesAbsoluteSnaps){

        	//
        	// Get the absolute snap values
        	//
        	xAxisAbsSnap = (float[]) RulePropertyAccessor.getProperty(
        	    tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_X_AXIS_SNAP_PROP);

            yAxisAbsSnap = (float[]) RulePropertyAccessor.getProperty(
        	    tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Y_AXIS_SNAP_PROP);
                	
            zAxisAbsSnap = (float[]) RulePropertyAccessor.getProperty(
        	    tool, currentEntity, Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.MOVEMENT_ABSOLUTE_Z_AXIS_SNAP_PROP);
                
            /*
            // If this entity uses absolute snaps, draw one line 
        	// per absolute snap as appropriate
            //
        	if(xAxisAbsSnap != null )
        		numVertiLines = xAxisAbsSnap.length;
        	if(yAxisAbsSnap != null )
        		numHorizLines = yAxisAbsSnap.length;*/
        }
        
        //
        // Draw one line per sticky snap
        //
        if ( xAxisIncSnap != null || xAxisAbsSnap != null ) {
        	numVertiLines = stickySnapIndices.length;
		}
        if ( yAxisIncSnap != null || yAxisAbsSnap != null ) {
        	numHorizLines = stickySnapIndices.length;
		}
        float[] f = new float[numHorizLines * 6 + numVertiLines * 6];
        
        // 
        // 1. to draw horizontal lines we need to calculate the 
        // different 'y' values for each line, since 
        // the x values in this case will always stretch from 
        // overlayViewFrustum[0] to overlayViewFrustum[1]
        //
        // 2. to draw vertical lines we need to calculate the 
        // different 'x' values for each line, since 
        // the y values in this case will always stretch from 
        // overlayViewFrustum[2] to overlayViewFrustum[3]
        //
        float xMin = (float)overlayViewFrustum[0];
        float xMax = (float)overlayViewFrustum[1];
        float yMin = (float)overlayViewFrustum[2];
        float yMax = (float)overlayViewFrustum[3];
        int i = 0;
        
        //
        // draw all the horizontal lines
        //
    	float yVal = overlayPosStartVertex[1];
    	int indx = 0;

    	while ( i < (numHorizLines * 6)) {
    		if ( usesIncrementalSnaps) {
    			yVal = overlayPosStartVertex[1] +
    				(float)heightRatio*yAxisIncSnap*stickySnapIndices[indx++];
			} else if ( usesAbsoluteSnaps ) {
    			yVal = overlayPosStartVertex[1] + 
    				((float)heightRatio*yAxisAbsSnap[stickySnapIndices[indx++]]);
			}
    		f[i++] = xMin;
    		f[i++] = yVal;
    		f[i++] = DIMENSIONAL_GUIDE_Z_POS;
    		f[i++] = xMax;
    		f[i++] = yVal;
    		f[i++] = DIMENSIONAL_GUIDE_Z_POS;
    	}
    	//
    	// draw all the vertical lines
    	//
    	float xVal = overlayPosStartVertex[0];
    	indx = 0;
    	
    	while (i < f.length) {
    		if ( usesIncrementalSnaps ) {
    			xVal = overlayPosStartVertex[0] +
    				(float)widthRatio*xAxisIncSnap*stickySnapIndices[indx++];
			} else if ( usesAbsoluteSnaps ) {
    			xVal = overlayPosStartVertex[0] +
    				((float)widthRatio * xAxisAbsSnap[stickySnapIndices[indx++]]);
			}
    		f[i++] = xVal;
    		f[i++] = yMin;
    		f[i++] = DIMENSIONAL_GUIDE_Z_POS;
    		f[i++] = xVal;
    		f[i++] = yMax;
    		f[i++] = DIMENSIONAL_GUIDE_Z_POS;
    	}
        
        stickySnaps.drawStickySnaps(f);
    }

    
    /**
     * build the real-world points and convert them into
     * overlay-layer coordinates in order to
     * draw the grid overlay
     */
    public void updateGridOverlay(){
        if (matrix == null) return;

        // calculate the length of the horizontal lines
        double min = (locationLayerViewFrustum[0] + overlayCenterPoint.x);
        double max = (locationLayerViewFrustum[1] + overlayCenterPoint.x);

        float horizontalIntervalsFromMinToZero =
            (float)Math.floor(min / gridOverlayInterval);
        float horizontalIntervalsFromMaxToZero =
            (float)Math.ceil(max / gridOverlayInterval);

        // draw lines from leftmostX to rightmostX
        float leftmostX = horizontalIntervalsFromMinToZero * gridOverlayInterval;
        float rightmostX =  horizontalIntervalsFromMaxToZero * gridOverlayInterval;

        // calculate the length of the vertical lines
        min = (locationLayerViewFrustum[2] + overlayCenterPoint.y);
        max = (locationLayerViewFrustum[3] + overlayCenterPoint.y);

        float verticalIntervalsFromMinToZero =
            (float)Math.floor(min / gridOverlayInterval);
        float verticalIntervalsFromMaxToZero =
            (float)Math.ceil(max / gridOverlayInterval);

        // and draw lines from bottomY to topY
        float bottomY = verticalIntervalsFromMinToZero * gridOverlayInterval;
        float topY =  verticalIntervalsFromMaxToZero * gridOverlayInterval;


        //
        // now we have most of the data necessary to draw our lines, but
        // we need to convert them from world screen space into overlay pixel
        // space.
        // In other words, we have the coordinates of location layer screen
        // space right now.
        //
        int horizontalDifference =
            (int)Math.abs(horizontalIntervalsFromMinToZero -
                          horizontalIntervalsFromMaxToZero);
        int verticalDifference =
            (int)Math.abs(verticalIntervalsFromMinToZero -
                          verticalIntervalsFromMaxToZero);

        Point4f leftBottom = new Point4f(leftmostX, bottomY, 0, 0);
        Point4f rightBottom = new Point4f(rightmostX, bottomY, 0, 0);

        Point4f leftTop = new Point4f(leftmostX, topY, 0, 0);
        //Point4f rightTop = new Point4f(rightmostX, topY, 0, 0);

        matrix.transform(leftBottom);
        matrix.transform(rightBottom);
        matrix.transform(leftTop);

        //
        // differences between the left top and left bottom
        //
        float xDiff = leftTop.x - leftBottom.x;
        float yDiff = leftTop.y - leftBottom.y;
        float zDiff = leftTop.z - leftBottom.z;

        //
        // Calculate the difference between each horizontal line.
        // Note that the spacing, or increment, between each horizontal
        // line is based on the amount of vertical space available, as is
        // the total number of horizontal lines.
        //
        float horizontalXIncrement = (xDiff/verticalDifference);
        float horizontalYIncrement = (yDiff/verticalDifference);
        float horizontalZIncrement = (zDiff/verticalDifference);

        float[] f = new float[(verticalDifference+horizontalDifference+2)*6];
        int horizPoints = (verticalDifference+1) * 6;

        Point2f overlayCoords;
        Point4f worldCoords = new Point4f();
        Point3f left  = new Point3f( leftBottom.x,  leftBottom.y,  leftBottom.z);
        Point3f right = new Point3f(rightBottom.x, rightBottom.y, rightBottom.z);

        int i = 0;
        for ( ; i < horizPoints; ){

            worldCoords.set(left.x, left.y, left.z, 0);
            overlayCoords = convertWorldPosToOverlayCoords(worldCoords);

            f[i++] = overlayCoords.x;
            f[i++] = overlayCoords.y;
            f[i++] = GRID_OVERLAY_Z_POS;

            worldCoords.set(right.x, right.y, right.z, 0);
            overlayCoords = convertWorldPosToOverlayCoords(worldCoords);

            f[i++] = overlayCoords.x;
            f[i++] = overlayCoords.y;
            f[i++] = GRID_OVERLAY_Z_POS;

            left.x += horizontalXIncrement;
            left.y += horizontalYIncrement;
            left.z += horizontalZIncrement;
            right.x += horizontalXIncrement;
            right.y += horizontalYIncrement;
            right.z += horizontalZIncrement;

        }

        //
        // differences between the left bottom and the right bottom
        //
        xDiff = rightBottom.x - leftBottom.x;
        yDiff = rightBottom.y - leftBottom.y;
        zDiff = rightBottom.z - leftBottom.z;

        //
        // calculate the difference between each vertical line.
        // Note that the spacing, or increment, between each vertical line
        // is based on the amount of horizontal space available, as is
        // the total number of vertical lines.
        //
        float verticalXIncrement = (xDiff/horizontalDifference);
        float verticalYIncrement = (yDiff/horizontalDifference);
        float verticalZIncrement = (zDiff/horizontalDifference);


        Point3f bottom = new Point3f( leftBottom.x,  leftBottom.y,  leftBottom.z);
        Point3f top = new Point3f( leftTop.x,  leftTop.y,  leftTop.z);

        for ( ; i < f.length; ){

            worldCoords.set(bottom.x, bottom.y, bottom.z, 0);
            overlayCoords = convertWorldPosToOverlayCoords(worldCoords);

            f[i++] = overlayCoords.x;
            f[i++] = overlayCoords.y;
            f[i++] = GRID_OVERLAY_Z_POS;

            worldCoords.set(top.x, top.y, top.z, 0);
            overlayCoords = convertWorldPosToOverlayCoords(worldCoords);

            f[i++] = overlayCoords.x;
            f[i++] = overlayCoords.y;
            f[i++] = GRID_OVERLAY_Z_POS;

            bottom.x += verticalXIncrement;
            bottom.y += verticalYIncrement;
            bottom.z += verticalZIncrement;
            top.x += verticalXIncrement;
            top.y += verticalYIncrement;
            top.z += verticalZIncrement;

        }
        gridOverlay.drawGridOverlay(f);
    }

    
    /**
     * Updates the segmentDimensional Indicators. This method calculates the 
     * points and rotation for the lines and text positions needed 
     * by the SegmentDimensionalIndicator.
     * 
     * Comments by Eric: I believe it works like this: <ul> 
     * <li>grab all the segments that connect to this vertex</li>
     * <li>for each segment, calc. the distance between this vertex and 
     * the other vertex.</li><li>convert the points to overlay coords</li>
     * <li>update the component appropriately</li></ul>
     * 
     * @author Jon Hubba 
     * @param entity - the entity being passed in, used mainly for property updates
     * in case there is no current Entity selected
     */
    private void updateSegmentDimensionalIndicator(Entity entity){
    	
        Entity useEntity = entity;

        if (currentEntity != null) {
            useEntity = currentEntity;
        }

        if ((useEntity == null) ||
			((useEntity.getType() != Entity.TYPE_SEGMENT) &&
			(useEntity.getType() != Entity.TYPE_VERTEX))) {
            return;
        }

        SegmentableEntity segmentableEntity =
            (SegmentableEntity)model.getEntity(useEntity.getParentEntityID());

        // don't display an indicator if this is the first and only vertex
        if(segmentableEntity.getVertices().size() < 1) {
            return;
        }

        // Point in the overlay 
        ArrayList<Point2f> startPoints = new ArrayList<Point2f>();
        ArrayList<Point2f> endPoints = new ArrayList<Point2f>();
        ArrayList<Vector3d> directionVectors = new ArrayList<Vector3d>();
        ArrayList<Float> wallLength = new ArrayList<Float>();
        ArrayList<Point2f> textPoints = new ArrayList<Point2f>();
        ArrayList<AxisAngle4d> rotate = new ArrayList<AxisAngle4d>();

        ArrayList<SegmentEntity> connectedSegments = new ArrayList<SegmentEntity>();
        connectedSegments= findAllTouchingSegments(useEntity);

        segmentDimensionalIndicator.createTextNodes(connectedSegments.size());

        //
        // grab segments connected to this vertex -
        // for the moment grab all segments
        //
        for(int i = 0; i < connectedSegments.size();i++) {
            SegmentEntity segment =   connectedSegments.get(i);

            // calculate distance between this vertex and 
            // connected vertices - use .getLength
            wallLength.add(segment.getLength());

            double[] startPos = new double[3];
            double[] endPos = new double[3];
            double[] textPos = new double[3];

            // calculate the  center of the wall
            VertexEntity startVertex = segment.getStartVertexEntity();
            VertexEntity endVertex = segment.getEndVertexEntity(); 

            startVertex.getPosition(startPos);
            endVertex.getPosition(endPos);

            // note: the Z value not important for overlay 
            startPos[2] = 0;
            endPos[2] = 0;


            //offset center by X along the normal of the wall 
            //(so should always be on outside)
            Vector3d directionVector = segment.getFaceVector();
            directionVector.normalize();
            Vector3d perpendicularNormal = new Vector3d(directionVector.x,
                    directionVector.y,
                    directionVector.z);


            Vector3d basicNormal = new Vector3d(1,0,0);
            double angle = perpendicularNormal.angle(basicNormal);
            perpendicularNormal.cross(perpendicularNormal, basicNormal);


            AxisAngle4d test = new AxisAngle4d(perpendicularNormal,angle);

            rotate.add(test);


            directionVectors.add(directionVector);

            directionVector.set(-directionVector.y, directionVector.x,
                    directionVector.z);

            //
            //Retrieve thickness to use as the offset of the position
            //
            ListProperty thicknessProperty = (ListProperty)segment.getProperty(
                    Entity.EDITABLE_PROPERTIES,
                    SegmentEntity.WALL_THICKNESS_PROP);

            Float thickness = 
                2 * Float.parseFloat(thicknessProperty.getSelectedValue());

            startPos[0] += directionVector.x *thickness;
            startPos[1] += directionVector.y *thickness;

            endPos[0] += directionVector.x *thickness;
            endPos[1] += directionVector.y *thickness; 

            //
            // convert points to overlay coordinates:
            //
            // first convert the start point to overlay coords
            //
            float[] overlayCoordinates =convertWorldPosToOverlayCoords(startPos);
            Point2f overLayPoint =
                new Point2f(overlayCoordinates[0],overlayCoordinates[1]);
            startPoints.add(overLayPoint);

            // convert the end point to overlay coords
            //
            overlayCoordinates =convertWorldPosToOverlayCoords(endPos);
            overLayPoint =
                new Point2f(overlayCoordinates[0],overlayCoordinates[1]);
            endPoints.add(overLayPoint);

            //calculates and convert the text point to overlay coords
            //
            textPos[0] = ((startPos[0] + endPos[0])/2);
            textPos[1] = (startPos[1] + endPos[1])/2;

            overlayCoordinates =convertWorldPosToOverlayCoords(textPos);            
            overlayCoordinates[0] += (directionVector.x *5);
            overlayCoordinates[1] += (directionVector.y *15);

            overLayPoint =
                new Point2f(overlayCoordinates[0],overlayCoordinates[1]);
            textPoints.add(overLayPoint);

        }
        
        segmentDimensionalIndicator.drawBounds(
				startPoints,
				endPoints,
				directionVectors,
				wallLength,
				textPoints,
				rotate);
    }
    
    /**
     * Draw the nearest neighbor overlay - an overlay component with
     * four lines: one to the north, one to the south, one to the east,
     * and one to the west. This method calculates the appropriate overlay
     * numbers and then calls nearestNeighbor.drawLines().
     */
    public void updateNearestNeighborOverlay() {

        //
        // if any null values are found, remove indicator, or if
        // the mouse is not yet within the window, don't display indicator
        //
		boolean isExtrusion = isExtrusion(currentEntity);
		
        if((activeZone == null) || (currentEntity == null) ||
            (currentEntity instanceof VertexEntity) ||
            (currentEntity instanceof TemplateEntity) ||
			(activeMousePositionChild == -1) ||
			isExtrusion) {
//System.out.println(activeMousePositionChild + ",\t" + 
//		currentEntity );//+ "\t," + activeZone);
            nearestNeighbor.display(false);
            return;
        }
        
        //
        // if the current entity is 'within' another object, remove indicator
        //
        //if(currentEntity.getParentEntityID() != activeZone.getEntityID())
        //    return;
        
        if(currentEntity instanceof PositionableEntity) {

            PositionableEntity positionableEntity = (PositionableEntity)currentEntity;

            //
            // Nearest neighbor draws four lines, which we will call
            // NORTH, SOUTH, EAST, WEST for their similarity with other java
            // directional values.  The START of each of these lines originates
            // from the currently-selected object, and the END of each of these
            // lines terminate at the intersection with the nearest neighbor.
            //
            float[] minExtents = new float[3];
            float[] maxExtents = new float[3];
			/////////////////////////////////////////////////////////////////
            //positionableEntity.getExtents(minExtents, maxExtents);
			HashMap<Integer, AV3DEntityWrapper> entityWrapperMap =
                locationLayerReference.getAV3DEntityWrapperMap();
			
			int entityID = currentEntity.getEntityID();
			AV3DEntityWrapper entityWrapper = entityWrapperMap.get(entityID);
			
			eu.getTransformToZone(currentEntity, mtx);
			
			OrientedBoundingBox bnd = entityWrapper.getBounds();
			bnd.transform(mtx);
			bnd.getExtents(minExtents, maxExtents);
			/////////////////////////////////////////////////////////////////

            float xMiddle = (minExtents[0] + maxExtents[0]) / 2;
            float yMiddle = (minExtents[1] + maxExtents[1]) / 2;
            float zMiddle = (minExtents[2] + maxExtents[2]) / 2;

            northStart = new Point3f(xMiddle, maxExtents[1], zMiddle);
            southStart = new Point3f(xMiddle, minExtents[1], zMiddle);
            eastStart = new Point3f(maxExtents[0], yMiddle, zMiddle);
            westStart = new Point3f(minExtents[0], yMiddle, zMiddle);

            convertLocalPosToOverlayCoords(northStart);
            convertLocalPosToOverlayCoords(southStart);
            convertLocalPosToOverlayCoords(eastStart);
            convertLocalPosToOverlayCoords(westStart);

			int zone_type = activeZone.getType();
            //
            // get the zone extents
            //
            //if (activeZone instanceof SegmentEntity) {
			if (zone_type == Entity.TYPE_SEGMENT) {

				ZoneEntity ze = (ZoneEntity)activeZone;
                float[] zoneBounds = new float[6];
				ze.getBounds(zoneBounds);

                Point3f upperBounds = new Point3f(zoneBounds[1],
                                                  zoneBounds[3],
                                                  zoneBounds[5]);
                Point3f lowerBounds = new Point3f(zoneBounds[0],
                                                  zoneBounds[2],
                                                  zoneBounds[4]);

                transformToRootInverse.transform(lowerBounds);
                transformToRootInverse.transform(upperBounds);

                northEnd = new Point3f(xMiddle, upperBounds.y + lowerBounds.y, zMiddle);
                southEnd = new Point3f(xMiddle, 0, zMiddle);
                eastEnd = new Point3f(upperBounds.x + lowerBounds.x, yMiddle, zMiddle);
                westEnd = new Point3f(0, yMiddle, zMiddle);

            } else if (zone_type == Entity.TYPE_GROUNDPLANE_ZONE) {

                northEnd = new Point3f(xMiddle, overlayCenterPoint.y +
                        (float)locationLayerViewFrustum[3], zMiddle);
                southEnd = new Point3f(xMiddle, overlayCenterPoint.y +
                        (float)locationLayerViewFrustum[2], zMiddle);
                eastEnd = new Point3f(overlayCenterPoint.x +
                        (float)locationLayerViewFrustum[1], yMiddle, zMiddle);
                westEnd = new Point3f(overlayCenterPoint.x +
                        (float)locationLayerViewFrustum[0], yMiddle, zMiddle);
				
			} else if (zone_type == Entity.TYPE_MODEL_ZONE) {

				ZoneEntity ze = (ZoneEntity)activeZone;
                float[] zoneBounds = new float[6];
				ze.getBounds(zoneBounds);

                Point3f upperBounds = new Point3f(zoneBounds[1],
                                                  zoneBounds[3],
                                                  zoneBounds[5]);
                Point3f lowerBounds = new Point3f(zoneBounds[0],
                                                  zoneBounds[2],
                                                  zoneBounds[4]);

                //transformToRootInverse.transform(lowerBounds);
                //transformToRootInverse.transform(upperBounds);

                northEnd = new Point3f(xMiddle, upperBounds.y, zMiddle);
                southEnd = new Point3f(xMiddle, lowerBounds.y, zMiddle);
                eastEnd = new Point3f(upperBounds.x, yMiddle, zMiddle);
                westEnd = new Point3f(lowerBounds.x, yMiddle, zMiddle);

            } else {
                // hide nearestNeighborOverlay if activeZone
                // is not a segmentEntity or a floor (type ZONE)
                nearestNeighbor.display(false);
                return;
            }
            //
            // convert zone extents and see if other
            // objects in the scene are closer
            //
            convertLocalPosToOverlayCoords(northEnd);
            convertLocalPosToOverlayCoords(southEnd);
            convertLocalPosToOverlayCoords(eastEnd);
            convertLocalPosToOverlayCoords(westEnd);

            nearestNeighbors(minExtents, maxExtents);

			nearestNeighbor.drawLines(
                northStart, northEnd,
                southStart, southEnd,
                eastStart, eastEnd,
                westStart, westEnd,
				widthRatio);
        } else {
            nearestNeighbor.display(false);
        }
    }

	/**
	 * Finds all the children Entities
	 *
	 * @param entity
	 * @param descendantList
	 */
	private void findAllDescendants(
		Entity entity,
		ArrayList<Entity> descendantList) {
		
		if (entity.hasChildren()) {
			
			ArrayList<Entity> childrenList = entity.getChildren();
			
			for (int i = 0; i < childrenList.size(); i++) {
				Entity child = childrenList.get(i);
				descendantList.add(child);
				findAllDescendants(child, descendantList);
			}            
		}
	}
	
    /**
     * Find the nearest neighbor to the currently selected entity
     */
    public void nearestNeighbors(float[] minExtents, float[] maxExtents){

        ArrayList<Entity> children = new ArrayList<Entity>();
        //findChildrenOfZone(activeZone, activeZone, children);
        findAllDescendants(activeZone, children);

        //
        // pocket data about actize zone ID to
        // avoid duplicate look ups in the for loop
        //
        int activeZoneType = activeZone.getType();
        int activeZoneID = activeZone.getEntityID();

        //
        // if the active zone is floor, recursive search on the parent
        // so that we end up grabbing the wall segments
        //
        //if(activeZone instanceof ZoneEntity){
        //    Entity parentEntity = entityMap.get(activeZone.getParentEntityID());
        //    findChildrenOfZone(parentEntity, parentEntity, children);
        //}
		if (activeZoneType == Entity.TYPE_GROUNDPLANE_ZONE) {
            AV3DEntityManager em = locationLayerReference.getAV3DEntityManager();
			SegmentableEntity mse = em.getSegmentableEntity();
			children.addAll(mse.getSegments());
        }

        if (children.size() == 0) {
            return;
		}
		
        //float[] minExtents = new float[3];
        //float[] maxExtents = new float[3];
        //((PositionableEntity)currentEntity).getExtents(minExtents, maxExtents);

        Point3f currentMax = new Point3f(maxExtents[0], maxExtents[1], maxExtents[2]);
        Point3f currentMin = new Point3f(minExtents[0], minExtents[1], minExtents[2]);
        convertLocalPosToOverlayCoords(currentMax);
        convertLocalPosToOverlayCoords(currentMin);

        BasePositionableEntity closestNeighborToNorth = null;
        BasePositionableEntity closestNeighborToSouth = null;
        BasePositionableEntity closestNeighborToEast = null;
        BasePositionableEntity closestNeighborToWest = null;

        float northX = northStart.x;
        float southX = southStart.x;
        float eastY = eastStart.y;
        float westY = westStart.y;

        BasePositionableEntity child;
        Point3f childMax;
        Point3f childMin;

        for (int i = 0 ; i < children.size(); i++) {

            Entity childEntity = children.get(i);

            if ((currentEntity == childEntity) ||
               !(childEntity instanceof BasePositionableEntity)) {
                continue;
			} else {
                child = (BasePositionableEntity)childEntity;
			}
            //
            // examine the types to do special case handling
            //
            int childType = child.getType();
			/*
            if (activeZoneType != Entity.TYPE_GROUNDPLANE_ZONE) {
                //
                // if active zone is a wall...
                if (childType == Entity.TYPE_SEGMENT ) {
                    continue; // ignore all segments
				}
                if (childEntity.getParentEntityID() != activeZoneID) {
                    continue; // ignore objects on adjacent walls
				}
            }
			*/
            //
            // if active zone is a floor...
			/*
            if (activeZoneType == Entity.TYPE_GROUNDPLANE_ZONE) {
                if ((childEntity.getParentEntityID() != activeZoneID) &&
					(childType != Entity.TYPE_SEGMENT))
                    continue; // ignore non-segment objects on adjacent walls
            }
			*/
            //
            // Vertices are far too large, for whatever reason, making the
            // results look wrong if they are included in the neighbor list.
            // Also ignore the world and zone types - this prevents us from
            // finding intersections with the floor.
            //
			/*
            if (childType == Entity.TYPE_VERTEX ||
                childType == Entity.TYPE_WORLD  ||
                childType == Entity.TYPE_GROUNDPLANE_ZONE ) {
                continue;
			}
			*/
			/////////////////////////////////////////////////////////////////
            //child.getExtents(minExtents, maxExtents);
			HashMap<Integer, AV3DEntityWrapper> entityWrapperMap =
                locationLayerReference.getAV3DEntityWrapperMap();
			
			int childID = child.getEntityID();
			AV3DEntityWrapper childWrapper = entityWrapperMap.get(childID);
			
			if (activeZoneType == Entity.TYPE_GROUNDPLANE_ZONE) {
				eu.getTransformToRoot(child, mtx);
			} else {
				eu.getTransformToZone(child, mtx);
			}
			
			OrientedBoundingBox bnd = childWrapper.getBounds();
			bnd.transform(mtx);
			bnd.getExtents(minExtents, maxExtents);
			/////////////////////////////////////////////////////////////////
            childMax = new Point3f(maxExtents[0], maxExtents[1], maxExtents[2]);
            childMin = new Point3f(minExtents[0], minExtents[1], minExtents[2]);
            convertLocalPosToOverlayCoords(childMax);
            convertLocalPosToOverlayCoords(childMin);

            //
            // does this entity qualify to be the closest neighbor to the north or south?
            //
            if ((currentMax.x > childMin.x) &&
				(currentMin.x < childMax.x)){

                if (childMin.y > currentMax.y) {
// System.out.println("found " + child.getEntityID() + " to the NORTH!");

                    if ((closestNeighborToNorth == null) ||
						(childMin.y < northEnd.y)){
                        
                        closestNeighborToNorth = child;
                        northEnd.y = childMin.y;
                        
                        // pretty adjustment to get the lines to match nicely
                        if (childMin.x > northStart.x) {
                            northX = childMin.x;
						} else if (childMax.x < northStart.x) {
                            northX = childMax.x;
						}
                    }
				} else if (childMax.y > currentMax.y) {
// System.out.println("found " + child.getEntityID() + " to the NORTH!");

                    if ((closestNeighborToNorth == null) ||
						(childMax.y < northEnd.y)){
                        
                        closestNeighborToNorth = child;
                        northEnd.y = childMax.y;
                        
                        // pretty adjustment to get the lines to match nicely
                        if (childMin.x > northStart.x) {
                            northX = childMin.x;
						} else if (childMax.x < northStart.x) {
                            northX = childMax.x;
						}
                    }
                }
				if (childMax.y < currentMin.y) {
// System.out.println("found " + child.getEntityID() + " to the SOUTH!");

                    if ((closestNeighborToSouth == null) ||
						(childMax.y > southEnd.y)){
                     
                        closestNeighborToSouth = child;
                        southEnd.y = childMax.y;

                        // pretty adjustment to get the lines to match nicely
                        if (childMin.x > southStart.x) {
                            southX = childMin.x;
						} else if (childMax.x < southStart.x) {
                            southX = childMax.x;
						}
                    }
				} else if (childMin.y < currentMin.y) {
// System.out.println("found " + child.getEntityID() + " to the SOUTH!");

                    if ((closestNeighborToSouth == null) ||
						(childMin.y > southEnd.y)){
                     
                        closestNeighborToSouth = child;
                        southEnd.y = childMin.y;

                        // pretty adjustment to get the lines to match nicely
                        if (childMin.x > southStart.x) {
                            southX = childMin.x;
						} else if (childMax.x < southStart.x) {
                            southX = childMax.x;
						}
                    }
                }
            }

            //
            // does this entity qualify to be the closest neighbor to the east or west?
            //
            if((currentMax.y > childMin.y) &&
			   (currentMin.y < childMax.y)) {

                if (childMin.x > currentMax.x) {
// System.out.println("found " + child.getEntityID() + " to the EAST!");

                    if ((closestNeighborToEast == null) || 
						(childMin.x < eastEnd.x)) {
                        
                        closestNeighborToEast = child;
                        eastEnd.x = childMin.x;

                        // get the lines to match nicely
                        if (childMin.y > eastStart.y) {
                            eastY = childMin.y;
						} else if (childMax.y < eastStart.y) {
                            eastY = childMax.y;
						}
                    }
				} else if (childMax.x > currentMax.x) {
// System.out.println("found " + child.getEntityID() + " to the EAST!");

                    if ((closestNeighborToEast == null) || 
						(childMax.x < eastEnd.x)) {
                        
                        closestNeighborToEast = child;
                        eastEnd.x = childMax.x;

                        // get the lines to match nicely
                        if (childMin.y > eastStart.y) {
                            eastY = childMin.y;
						} else if (childMax.y < eastStart.y) {
                            eastY = childMax.y;
						}
                    }
                }
				if (childMax.x < currentMin.x) {
// System.out.println("found " + child.getEntityID() + " to the WEST!");

                    if ((closestNeighborToWest == null) ||
						(childMax.x > westEnd.x)){
                        
                        closestNeighborToWest = child;
                        westEnd.x = childMax.x;

                        // get the lines to match nicely
                        if (childMin.y > westStart.y) {
                            westY = childMin.y;
						} else if(childMax.y < westStart.y) {
                            westY = childMax.y;
						}
                    }
				} else if (childMin.x < currentMin.x) {
// System.out.println("found " + child.getEntityID() + " to the WEST!");

                    if ((closestNeighborToWest == null) ||
						(childMin.x > westEnd.x)){
                        
                        closestNeighborToWest = child;
                        westEnd.x = childMin.x;

                        // get the lines to match nicely
                        if (childMin.y > westStart.y) {
                            westY = childMin.y;
						} else if(childMax.y < westStart.y) {
                            westY = childMax.y;
						}
                    }
                }
            }
            northStart.x = northX;
            northEnd.x   = northX;
            southEnd.x   = southX;
            southStart.x = southX;
            eastStart.y  = eastY;
            eastEnd.y    = eastY;
            westStart.y  = westY;
            westEnd.y    = westY;
        }
    }

	/**
	 * Finds all the children Entities in a zone and stores them in a global list
	 * @param zoneEntity
	 * @param entity
	 * @param zoneChildren
	 */
	/*
	private void findChildrenOfZone(
		Entity zoneEntity, 
		Entity entity, 
		ArrayList<Entity> zoneChildren) {
		
		if ((entity != null) && entity.hasChildren()) {
			
			ArrayList<Entity> childrenList = entity.getChildren();
			
			for ( int i =0; i < childrenList.size(); i++) {
				findChildrenOfZone(zoneEntity, childrenList.get(i), zoneChildren);
			}            
		}
		if (entity != zoneEntity) {
			zoneChildren.add(entity);
		}
	}
	*/
	
    /**
     * Take the local bounds and convert it into overlay coordinates.
     * First we convert the local bounds into an on-screen position
     * relative to the center of the screen, and then we convert this
     * on-screen position into overlay coordinates.
     *
     * @param bounds a float array with at least six points representing
     * a local position relative to some parent zone
     */
    private void convertLocalBoundsToOverlayCoords(float[] bounds){

        //
        // create a local position representing the lower bounds,
        // then convert the local position to on-screen position
        //
        Point3f point = new Point3f(bounds[0], bounds[2], bounds[4]);
        convertLocalPosToOnScreenCoords(point);
        bounds[0] = point.x;
        bounds[2] = point.y;
        bounds[4] = point.z;

        //
        // now create a local position representing the upper bounds,
        // then convert the local position to on-screen position
        //
        point = new Point3f(bounds[1], bounds[3], bounds[5]);
        convertLocalPosToOnScreenCoords(point);
        bounds[1] = point.x;
        bounds[3] = point.y;
        bounds[5] = point.z;

        //
        // finally, convert the on-screen position into overlay coordinates
        //
        convertOnScreenBoundsToOverlayCoords(bounds);
    }

    private void convertOnScreenBoundsToOverlayCoords(float[] bounds){
        //
        // we use width ratio and height ratio to convert each point's offset
        // from the on-screen center point into it's offset from the center
        // of the overlay_layer.
        //
        // finally, we use the offset from the center to get the point's postion
        // as overlay coordinates
        //
        bounds[0] *= widthRatio;
        bounds[0] += centerOfOverlayViewFrustum.x;
        bounds[1] *= widthRatio;
        bounds[1] += centerOfOverlayViewFrustum.x;
        bounds[2] *= heightRatio;
        bounds[2] += centerOfOverlayViewFrustum.y;
        bounds[3] *= heightRatio;
        bounds[3] += centerOfOverlayViewFrustum.y;
    }

    /**
     * As of 3/23/2010, entity positioning information is all
     * relative to the parent of the currently-selected entity.<p>
     * For instance, an obstacle with center coordinates of (3, 2, 0) 
     * is offset from its parent by three units along the x-axis and 
     * 2 units along the y-axis.<p> Thus, to calculate actual on-screen 
     * positioning information we use the following code (from the
     * {@link #selectionChanged(int, boolean)} method):<p>
     * <code>
     * AV3DEntityWrapper entityWrapper = 
     * entityWrapperMap.get(currentEntity.getParentEntityID());<p>
     * 
     * if( entityWrapper != null) {<br>&nbsp;&nbsp;
     * // get the transform to the root, and set the inverse<br>&nbsp;&nbsp;
     *		transformUtils.getLocalToVworld(<br>&nbsp;&nbsp;&nbsp;&nbsp;
     *			entityWrapper.transformGroup, transformToRootMtx);<br>                       
     *}</code><p>
     * 
     * @param position a Point3f representing a local position relative to 
     * an entity's parent.  Assumes transformToRootMtx has been set properly.
     */
    private void convertLocalPosToOnScreenCoords(Point3f position){
        //
        // these two matrix transformations give us the on-screen position
        // (relative to the center of the screen) of the given point.
        //
        transformToRootMtx.transform(position);
        inverseMatrix.transform(position);
    }


    /**
     * Take a local position and convert it into overlay coordinates.
     * First we convert the local position into an on-screen position
     * relative to the center of the screen, and then we convert this
     * on-screen position into overlay coordinates.
     *
     * @param position a Point3f representing a local position relative to some
     * parent zone
     */
    private void convertLocalPosToOverlayCoords(Point3f position){

        // first, convert the local position to on-screen position
        convertLocalPosToOnScreenCoords(position);

        // then we can convert the on-screen position into overlay coordinates
        convertOnScreenPosToOverlayCoords(position);
    }


    /**
     * Convert an on-screen position (relative to the center of the screen -
     * IE, considering the center of the viewable area as the origin) into
     * overlay coordinates representing the same point in overlay space.
     * <p>
     * Example: if position(0, 0, 0) was passed in as a parameter, it would
     * be converted to a point equal to centerOfOverlayViewFrustum, since the
     * point representing the center of viewable location-layer space is
     * obviously equal to the center of overlay layer space!
     * @param position an on-screen position relative to the center of the screen -
     * IE, considering the center of the viewable area as the origin.
     * @param position a Point3f representing an on-screen position relative
     * to the center of the screen as the origin.
     */
    private void convertOnScreenPosToOverlayCoords(Point3f position){
        //
        // we use width ratio and height ratio to convert each point's offset
        // from the on-screen center point into it's offset from the center
        // of the overlay_layer.
        //
        // finally, we use the offset from the center to get the point's postion
        // as overlay coordinates
        //
        position.x *= widthRatio;
        position.x += centerOfOverlayViewFrustum.x;
        position.y *= heightRatio;
        position.y += centerOfOverlayViewFrustum.y;
    }

    /**
     * Convert world bounds coordinates to overlay coordinates
     * based on the view frustum of both
     * @param bounds float array of length six containing bounds
     * of the object
     */
    private void convertWorldPosToOverlayCoords(float[] bounds){

        //
        // From the bounds, create a point for the minimum bounds and a point
        // for the maximum bounds.
        // Apply the inverse of the locationLayer's viewMatrix to the two
        // points in question.  Add on a dummy value of '0' for the fourth
        // value in the array.  Doing so means the multiplication of the
        // matrix with the point will ignore everything except the rotation.
        // ( an alternate way to do this would be to grab the 3x3 rotation part
        // of the Matrix4f and use that to multiply with the 3-dimensional points).
        //
        Point4f minPoint = new Point4f(bounds[0],
                                       bounds[2],
                                       bounds[4],
                                       0);
        Point4f maxPoint = new Point4f(bounds[1],
                                       bounds[3],
                                       bounds[5],
                                       0);

        Point2d minPointCenterOffset =
            new Point2d(minPoint.x - overlayCenterPoint.x,
                        minPoint.y - overlayCenterPoint.y);

        Point2d maxPointCenterOffset =
            new Point2d(maxPoint.x - overlayCenterPoint.x,
                        maxPoint.y - overlayCenterPoint.y);

        //
        // we use width ratio and height ratio to convert each point's offset
        // from the center of location_layer into it's offset from the center
        // of the overlay_layer.
        //
        minPointCenterOffset.x *= widthRatio;
        minPointCenterOffset.y *= heightRatio;
        maxPointCenterOffset.x *= widthRatio;
        maxPointCenterOffset.y *= heightRatio;

        //
        // now use the offset from the center to get the point's postion
        // as overlay coordinates
        //

        bounds[0] = (float)centerOfOverlayViewFrustum.x;
        bounds[0] += (minPointCenterOffset.x < maxPointCenterOffset.x)?
                      minPointCenterOffset.x : maxPointCenterOffset.x;
        bounds[1] = (float)centerOfOverlayViewFrustum.x;
        bounds[1] += (minPointCenterOffset.x > maxPointCenterOffset.x)?
                      minPointCenterOffset.x : maxPointCenterOffset.x;

        bounds[2] = (float)centerOfOverlayViewFrustum.y;
        bounds[2] += (minPointCenterOffset.y < maxPointCenterOffset.y)?
                      minPointCenterOffset.y : maxPointCenterOffset.y;
        bounds[3] = (float)centerOfOverlayViewFrustum.y;
        bounds[3] += (minPointCenterOffset.y > maxPointCenterOffset.y)?
                      minPointCenterOffset.y : maxPointCenterOffset.y;

        bounds[4] = -5;
        bounds[5] = -5;

    }

    /**
     * Convert an overlay position (which means a pixel coordinate in
     * overlay space, where the origin [0,0] is in the lower-left-hand
     * corner) into world coordinates used by the locationLayer. 
     * @param pos A float array of minimum length two - any indices 
     * beyond pos[0] and pos[1] will be ignored.
     * @return a Point2f representing an X,Y point in world space.
     */
    private Point2f convertOverlayCoordsToWorldPos(float[] pos){
    	Point4f position = new Point4f(pos[0], pos[1], 0, 0);

    	inverseMatrix.transform(position);

    	Point2d offsetFromCenter =
    		new Point2d(position.x - centerOfOverlayViewFrustum.x,
    					position.y - centerOfOverlayViewFrustum.y);
    	
    	offsetFromCenter.x /= widthRatio;
        offsetFromCenter.y /= heightRatio;

        //
        // now use the offset from the center to get the point's position
        // as location layer coordinates
        //
        return new Point2f(
            centerOfLocationLayer.x + (float)offsetFromCenter.x,
            centerOfLocationLayer.y + (float)offsetFromCenter.y);
    }
    
    /**
     * Convert a world coordinate to overlay coordinates based on
     * the view frustum differences between location layer and overlay layer.
     * @param position Point4f where the fourth value should just be zero ->
     * only the x, y, and z values matter.
     * @return a point2f containing the x and y overlay coordinates.  Since
     * the overlay layer is two dimensional, there is no need for a 'z' value.
     */
    private Point2f convertWorldPosToOverlayCoords(Point4f position){
        inverseMatrix.transform(position);

        Point2d offsetFromCenter =
            new Point2d(position.x - overlayCenterPoint.x,
                        position.y - overlayCenterPoint.y);

        //
        // we use width ratio and height ratio to convert each point's offset
        // from the center of location_layer into it's offset from the center
        // of the overlay_layer.
        //
        offsetFromCenter.x *= widthRatio;
        offsetFromCenter.y *= heightRatio;

        //
        // now use the offset from the center to get the point's position
        // as location layer coordinates
        //
        return new Point2f(
            (float)(centerOfOverlayViewFrustum.x + offsetFromCenter.x),
            (float)(centerOfOverlayViewFrustum.y + offsetFromCenter.y));
    }

    /**
     * Convert world bounds coordinates to overlay coordinates
     * based on the view frustum of both
     * @param position double array of length three containing
     * x, y, and z coordinates
     * @return a float array containing the converted x y and z values
     */
    private float[] convertWorldPosToOverlayCoords(double[] position){
//System.out.println(java.util.Arrays.toString(position));
        //
        // From the bounds, create a point for the minimum bounds and a point
        // for the maximum bounds.
        // Apply the inverse of the locationLayer's viewMatrix to the two
        // points in question.  Add on a dummy value of '0' for the fourth
        // value in the array.  Doing so means the multiplication of the
        // matrix with the point will ignore everything except the rotation.
        // ( an alternate way to do this would be to grab the 3x3 rotation part
        // of the Matrix4f and use that to multiply with the 3-dimensional points).
        //
        Point4f point = new Point4f( (float)position[0],
                (float)position[1],
                (float)position[2],
                0);
        inverseMatrix.transform(point);

        Point2d offsetFromCenter =
            new Point2d(point.x - overlayCenterPoint.x,
                        point.y - overlayCenterPoint.y);

        //
        // we use width ratio and height ratio to convert each point's offset
        // from the center of location_layer into it's offset from the center
        // of the overlay_layer.
        //
        offsetFromCenter.x *= widthRatio;
        offsetFromCenter.y *= heightRatio;

        //
        // now use the offset from the center to get the point's position
        // as overlay coordinates
        //
        return new float[] {
                (float)(centerOfOverlayViewFrustum.x + offsetFromCenter.x),
                (float)(centerOfOverlayViewFrustum.y + offsetFromCenter.y),
                0 };

    }


    /**
     * Recalculate the coordinates for the dimensional indicator
     */
    public void updateDimensionalIndicator(){

        //
        // first, check for null. remove indicator if
        // current entity is null.  also,
        // ignore location and zone selections
        //
		int type = -1;
		boolean isExtrusion = false;
		if (currentEntity != null) {
			
			type = currentEntity.getType();
			isExtrusion = isExtrusion(currentEntity);
		}
		
        if ((currentEntity == null) ||
			(type == Entity.TYPE_LOCATION) ||
			(type == Entity.TYPE_GROUNDPLANE_ZONE) ||
			(type == Entity.TYPE_VERTEX) ||
			(type == Entity.TYPE_SEGMENT) ||
			isExtrusion) {

            dimensionalIndicator.display(false);

        } else if (currentEntity instanceof SegmentEntity) {

            SegmentEntity segment = (SegmentEntity)currentEntity;
            segment.getBounds(bounds);
            float[] size = new float[3];
            segment.getSize(size);
            float[] scale = new float[3];
            segment.getScale(scale);
//System.out.println("size:\t" + java.util.Arrays.toString(size));
//System.out.println("scale:\t" + java.util.Arrays.toString(scale));
//System.out.println("bounds:\t" + java.util.Arrays.toString(bounds));
            float verticalLabel = (float)(bounds[3] - bounds[2]);
            float horizLabel = (float)(bounds[1] - bounds[0]);

            convertLocalBoundsToOverlayCoords(bounds);

            dimensionalIndicator.drawBounds(bounds,
                                            new float[4],
                                            new float[4],
                                            horizLabel,
                                            verticalLabel);

        } else if (currentEntity instanceof PositionableEntity){

        	//
        	// Begin by grabbing all the relevant positional data
        	//        	
        	PositionableEntity positionableEntity = (PositionableEntity)currentEntity;

        	position[0] = 0;
        	position[1] = 0;
        	position[2] = 0;
            boolean pos = TransformUtils.getLocalToZone(
            		model, positionableEntity, position);

            if( !pos )
            	positionableEntity.getPosition(position);
            
            positionableEntity.getRotation(rotation);
            positionableEntity.getBounds(bounds);
            
            
            //////////////////////////////////////////////////////////
            //
            // now we have to account for rotation!
            //
            // How to handle this correctly:
            //
            // 1. set the bounds of the object as if the object was
            // centered over the origin of the overlayLayer.
            //
            // 2. apply rotation
            //
            // 3. translate the object BACK to its actual on-screen
            // position so that it appears correctly in the scene.
            //
            //////////////////////////////////////////////////////////
            
            float verticalLabel = (float)(bounds[3] - bounds[2]);
            float horizLabel = (float)(bounds[1] - bounds[0]);


            //
            // 1. We want to adjust the bounds to center over the origin
            // of the overlay layer (which appears in the lower left of
            // the screen).  So what are the on-screen coordinates of the
            // lower left of the screen [relative to the origin appearing
            // at the center of the viewable area?
            //
            bounds[0] += locationLayerViewFrustum[0];
            bounds[1] += locationLayerViewFrustum[0];
            bounds[2] += locationLayerViewFrustum[2];
            bounds[3] += locationLayerViewFrustum[2];

            //
            // Now convert the on-screen positions into overlay coordinates.
            //
            convertOnScreenBoundsToOverlayCoords(bounds);


            //
            // 2, 3: get the actual on-screen overlay position.
            // Pass this to drawBounds(); the dimensional indicator
            // will then draw the rotated guide in the correct location.  
            //
            Point3f centerPos = new Point3f((float)position[0],
                                             (float)position[1],
                                             (float)position[2]);

            convertLocalPosToOverlayCoords(centerPos);

            float overlayPos[] = new float[]{centerPos.x, centerPos.y, centerPos.z};

            dimensionalIndicator.drawBounds(bounds,
                    rotation,
                    overlayPos,
                    horizLabel,
                    verticalLabel);
            
            // DEBUG GOODNESS            
//          double originOffset[] = new double[3];
//          positionableEntity.getOriginOffset(originOffset);
//          System.out.println(java.util.Arrays.toString(originOffset));
//          System.out.println("position: " + java.util.Arrays.toString(position));
//          System.out.println("rotation: " + java.util.Arrays.toString(rotation));
//          System.out.println("bounds: " + java.util.Arrays.toString(bounds));

        } else {
            dimensionalIndicator.display(false);
        }
    }

    

    /** EMF: Who added this?  */
    public Object getComponent() {
        // ignored
        return null;
    }
    
    
    /**
     * Walk through the children of the argument entity,
     * adding listeners as necessary.
     * This is called by ModelListener's {@link #entityAdded(boolean, Entity)}
     * method and EntityChildListener's {@link #childAdded(int, int)} method.
     * <br>
     * If an entity is a Vertex shadow entity, then we should become an 
     * EntityPropertyListener to listen for movement changes.  This way we will
     * be able to correctly update the LengthIndicator wall placement helper
     * overlay.
     * 
     * @param entity The entity to start with
     */
    private void recursiveAdd(Entity entity) {

        if (entity instanceof ViewpointContainerEntity) {

            return;
        }

        entity.addEntityChildListener(this);
        entity.addEntitySelectionListener(this);

        
        //
        // If this is a vertex shadow entity, then we need to update the 
        // length indicator overlay, to help customers with wall placement.
        //
        if(entity instanceof VertexEntity){
                        
            VertexEntity vertex = (VertexEntity)entity;
            Boolean shadow = (Boolean)
                vertex.getProperty(entity.getParamSheetName(),
                        Entity.SHADOW_ENTITY_FLAG); 
            if(shadow != null && shadow) {
                vertex.addEntityPropertyListener(this);
			}
        }


        entityMap.put(entity.getEntityID(), entity);

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveAdd(child);
            }
        }
    }

    /**
     * Walk through the children of the argument entity,
     * removing listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveRemove(Entity entity) {

        entity.removeEntityChildListener(this);
        entity.removeEntitySelectionListener(this);

        int entityID = entity.getEntityID();

        entityMap.remove(entityID);

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveRemove(child);
            }
        }
    }
	
	/**
	 * Determine whether the argument entity is an extrusion type
	 *
	 * @param entity The entity to evaluate
	 * @return true is the entity is an extrusion type, false otherwise.
	 */
	private boolean isExtrusion(Entity entity) {
		boolean isExtrusion = false;
		if (entity != null) {
			Object isExtrusionProp = currentEntity.getProperty(
				currentEntity.getParamSheetName(),
				ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);
			
			if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
				isExtrusion = ((Boolean)isExtrusionProp).booleanValue();
			}
		}
		return(isExtrusion);
	}
	
	/**
	 * Initialize the scenegraph objects for the ruler label,
	 * displaying the units of measurement
	 */
	private void initRulerLabel() {
		
		// ruler label background geometry
		float[] color = Color.white.getRGBComponents(null);
		
		Material box_material = new Material();
		box_material.setDiffuseColor(color);
    	box_material.setEmissiveColor(color);
		
		Appearance box_appearance = new Appearance();
		box_appearance.setMaterial(box_material);
		
		float x_min = 0;
		float x_max = Ruler.RULER_FATNESS;
		float y_min = 0;
		float y_max = Ruler.RULER_FATNESS;
		float z = -1;
		
		float[] box_vertices = new float[]{
			x_min, y_min, z,
			x_max, y_min, z,
			x_max, y_max, z,
			x_min, y_max, z};
		
		float[] box_normals = new float[] { 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1};
		
		QuadArray box_geom = new QuadArray();
		box_geom.setVertices(QuadArray.COORDINATE_3, box_vertices, 4);
		box_geom.setNormals(box_normals);
		
		Shape3D box_shape = new Shape3D();
		box_shape.setAppearance(box_appearance);
		box_shape.setGeometry(box_geom);
		
		Group rulerLabelGroup = new Group();
		rulerLabelGroup.addChild(box_shape);
		
		// the label text
		rulerLabelText = new LegendText(mgmtObserver);
		rulerLabelText.setFont(new Font("Arial", Font.PLAIN, 10));
		rulerLabelText.update("m", 0, Anchor.TOP_LEFT, null);
		
		TransformGroup rulerLabelTransform = new TransformGroup();
		
		translation.set(rlgX, rlgY, -0.5f);
		mtx.setIdentity();
		mtx.setTranslation(translation);
		rulerLabelTransform.setTransform(mtx);
		
		rulerLabelTransform.addChild(rulerLabelText.getShape());
		
		rulerLabelGroup.addChild(rulerLabelTransform);
		
        rootGroup.addChild(rulerLabelGroup);
	}
	
	/**
	 * Initialize the scenegraph objects for the mouse position indicator
	 */
	private void initMousePosition() {
		
		float[] color = Color.red.getRGBComponents(null);
		
		Material line_material = new Material();
		line_material.setDiffuseColor(color);
    	line_material.setEmissiveColor(color);
		
		LineAttributes line_attributes = new LineAttributes();
		line_attributes.setLineWidth(3);
        line_attributes.setStipplePattern(LineAttributes.PATTERN_SOLID);
		
		Appearance line_appearance = new Appearance();
		line_appearance.setMaterial(line_material);
		line_appearance.setLineAttributes(line_attributes);
		
        vertMouseIndicatorArray = new float[]{0, 0, -.001f, 0, 0, -.001f};
		
		vertMousePositionGeom = new LineArray();
        vertMousePositionGeom.setVertices(LineArray.COORDINATE_3, vertMouseIndicatorArray);
		
		Shape3D vert_shape = new Shape3D();
		vert_shape.setAppearance(line_appearance);
		vert_shape.setGeometry(vertMousePositionGeom);
		
        horzMouseIndicatorArray = new float[]{0, 0, -.001f, 0, 0, -.001f};
		
		horzMousePositionGeom = new LineArray();
        horzMousePositionGeom.setVertices(LineArray.COORDINATE_3, horzMouseIndicatorArray);
		
		Shape3D horz_shape = new Shape3D();
		horz_shape.setAppearance(line_appearance);
		horz_shape.setGeometry(horzMousePositionGeom);
		
        Group mousePositionGroup = new Group();
        
        mousePositionGroup.addChild(vert_shape);
        mousePositionGroup.addChild(horz_shape);
		
        mousePositionSwitchGroup = new SwitchGroup();
        mousePositionSwitchGroup.addChild(mousePositionGroup);
		
        activeMousePositionChild = 0;
        mousePositionSwitchGroup.setActiveChild(activeMousePositionChild);
		
        rootGroup.addChild(mousePositionSwitchGroup);
	}
}
