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

package org.chefx3d.view.common;

// External imports
import java.util.HashMap;

// Local Imports
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.model.Entity;
import org.chefx3d.model.LocationEntity;

import org.chefx3d.view.View;

/**
 * Defines the requirements for an editing view.
 * 
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */	
public interface EditorView extends View {
	
    /**
     * Return the currently active LocationEntity
     *
     * @return The currently active LocationEntity
     */
    public LocationEntity getActiveLocationEntity();

    /**
     * Return the RuleCollisionChecker
     *
     * @return The RuleCollisionChecker
     */
    public RuleCollisionChecker getRuleCollisionChecker();

    /**
     * Return the CatalogManager
     *
     * @return The CatalogManager
     */
    public CatalogManager getCatalogManager();

    /**
     * Return the StatusReporter
     *
     * @return The StatusReporter
     */
    public StatusReporter getStatusReporter();

    /**
     * Return the entity wrapper map.
     *
     * @return HashMap<Integer, EntityWrapper>
     */
    public HashMap<Integer, EntityWrapper> getEntityWrapperMap();
	
    /**
     * Get the zoom amount of the current view.
     *
     * @return double zoom amount
     */
    public double getZoneViewZoomAmount();
	
	/**
	 * Return the zone relative mouse position in the argument array.
	 * If the arguement is null or less than length 3, a new array
	 * will be allocated and returned.
	 * 
	 * @param An array for the return value
	 * @return The array containing the return value
	 */
	public float[] getZoneRelativeMousePosition(float[] position);

    /**
     * It would be awesome if this had some proper javadoc! Just
     * imagine what all those booleans in the anchorFlags array
     * might mean.
     */
    public void setSelectionAnchors(Entity entity, boolean[] anchorFlags);

    /**
     * Get the selected anchor data identifying which anchor
     * is selected. Either NORTH, SOUTH, SOUTHWEST etc.
     * 
     * @return EditorConstants.AnchorData identifier for selected anchor
     */
    public EditorConstants.AnchorData getSelectedAnchorData();
    	
    /**
     * Return the NearestNeighborMeasurement object
     * 
     * @return The NearestNeighborMeasurement object
     */
    public NearestNeighborMeasurement getNearestNeighborMeasurement();
    	
	/**
	 * Return the editor object to it's initial state
	 */
	public void reset();
}
