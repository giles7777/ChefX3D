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
import org.j3d.aviatrix3d.NodeUpdateListener;

import org.j3d.aviatrix3d.pipeline.graphics.GraphicsResizeListener;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

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
class PreviewLegendLayerManager extends AbstractLegendLayerManager {

	/** Helper for displaying wall labels on the floor view */
	private FloorLegend floorLegend;
	
	/** Helper for displaying product labels */
	private ProductLegend productLegend;
	
	/** Helper for displaying segment vertical legend */
	private SegmentLegendVertical segVertical;
	
	/** Helper for displaying segment horizontal legend */
	private SegmentLegendHorizontal segHorizontal;
	
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
    PreviewLegendLayerManager(
        int id,
        int[] dim,
        WorldModel model,
        ErrorReporter reporter,
        SceneManagerObserver mgmtObserver,
        NavigationStatusManager navStatusManager ) {

		super(id, dim, model, reporter, mgmtObserver, navStatusManager);
		
		floorLegend = new FloorLegend(mgmtObserver, rootGroup);
		productLegend = new ProductLegend(mgmtObserver, rootGroup);
		segVertical = new SegmentLegendVertical(mgmtObserver, rootGroup);
		segHorizontal = new SegmentLegendHorizontal(null, mgmtObserver, rootGroup);
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
		
        super.setEntityManager(entityManager);
		segVertical.setEntityManager(entityManager);
		segHorizontal.setEntityManager(entityManager);
		floorLegend.setEntityManager(entityManager);
		productLegend.setEntityManager(entityManager);
    }

    /**
     * Set the active zone entity
     *
     * @param ze The active zone entity
     */
    public void setActiveZoneEntity(ZoneEntity ze) {
		
		super.setActiveZoneEntity(ze);
		
		segVertical.setActiveZoneEntity(activeZoneEntity);
		segHorizontal.setActiveZoneEntity(activeZoneEntity);
		floorLegend.setActiveZoneEntity(activeZoneEntity);
		productLegend.setActiveZoneEntity(activeZoneEntity);
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

		super.viewportSizeChanged(frustumCoords);
		configLegendComponents();
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
		
		super.graphicsDeviceResized(x, y, width, height);
		configLegendComponents();
	}

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Configure the display of wall labels on the floor
	 *
	 * @param enable true to display, false to remove
	 */
	void enableFloorLegend(boolean enable) {
		floorLegend.setEnabled(enable);
	}
	
	/**
	 * Configure the display of the segment legend
	 *
	 * @param enable true to display, false to remove
	 */
	void enableSegmentLegend(boolean enable) {
		segVertical.setEnabled(enable);
		segHorizontal.setEnabled(enable);
	}
	
	/**
	 * Configure the display of the product legend
	 *
	 * @param enable true to display, false to remove
	 */
	void enableProductLegend(boolean enable) {
		productLegend.setEnabled(enable);
	}
	
	/**
	 * Configure the view parameters for the legend components
	 */
	private void configLegendComponents() {
		// the frustum to viewport ratio, in meters per pixel
		double ratio = frustum_width / viewport_width;
		productLegend.setViewParam(ratio);
	}
}
