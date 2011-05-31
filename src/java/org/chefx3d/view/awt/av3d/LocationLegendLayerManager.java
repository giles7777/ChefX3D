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
 * Implementation of an overlay LayerManager for the Editor. Handles
 * the creation, add and remove of various labels and indicators.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class LocationLegendLayerManager extends AbstractLegendLayerManager {

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
    LocationLegendLayerManager(
        int id,
        int[] dim,
        WorldModel model,
        ErrorReporter reporter,
        SceneManagerObserver mgmtObserver,
        NavigationStatusManager navStatusManager ) {

		super(id, dim, model, reporter, mgmtObserver, navStatusManager);
		
		segHorizontal = new SegmentLegendHorizontal(model, mgmtObserver, rootGroup);
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
		segHorizontal.setEntityManager(entityManager);
    }

    /**
     * Set the active zone entity
     *
     * @param ze The active zone entity
     */
    public void setActiveZoneEntity(ZoneEntity ze) {
		
		super.setActiveZoneEntity(ze);
		
		segHorizontal.setActiveZoneEntity(activeZoneEntity);
		
		if ((ze != null) && (ze.getType() == Entity.TYPE_SEGMENT)) {
			segHorizontal.setEnabled(true);
		} else {
			segHorizontal.setEnabled(false);
		}
    }
}
