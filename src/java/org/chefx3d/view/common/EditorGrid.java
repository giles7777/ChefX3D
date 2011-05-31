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
import java.util.prefs.Preferences;

import javax.vecmath.Vector2d;

// Local Imports
import org.chefx3d.preferences.PersistentPreferenceConstants;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ConfigManager;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * Utility for configuring positions to the editor grid.
 * 
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */	
public class EditorGrid {
	
	/** Default grid spacing */
	private static final double DEFAULT_GRID_SPACE = 0.0;
	
	/** The current working unit of measurement */
	private Unit uom;
	
	/** The grid spacing parameter for editing operations, in meters */
	private double gridSpacing;
	
	/**
	 * Constructor
	 */
	public EditorGrid() {
		
		String appName = (String)ApplicationParams.get(ApplicationParams.APP_NAME);
        Preferences prefs = Preferences.userRoot().node(appName);
        String unitLabel = prefs.get(
        		PersistentPreferenceConstants.UNIT_OF_MEASUREMENT, 
        		PersistentPreferenceConstants.DEFAULT_UNIT_OF_MEASUREMENT);
        uom = UnitConversionUtilities.getUnitByCode(unitLabel);
		
		gridSpacing = DEFAULT_GRID_SPACE;
		ConfigManager cm = ConfigManager.getManager();
		String gsp = cm.getProperty("gridSpacing."+ uom.getLabel());
		if (gsp != null) {
			try {
				gridSpacing = Double.parseDouble(gsp);
			} catch (NumberFormatException nfe) {
				// should throw out a catchy message here......
				// if we only had an error reporter instance
			}
		}
	}
	
	/**
	 * Return the current working unit of measurement
	 *
	 * @return The current working unit of measurement
	 */
	public Unit getUnitOfMeasurement() {
		return(uom);
	}
	
	/**
	 * Return the working editor grid spacing in meters
	 *
	 * @return The working editor grid spacing in meters
	 */
	public double getGridSpacing() {
		return(gridSpacing);
	}
	
	/**
	 * Modify the argument position value to align with the grid spacing.
	 *
	 * @param pos The position 
	 * @return true if the pos array has been modified, false if the
	 * array remains unchanged.
	 */
	public boolean alignPositionToGrid(double[] pos) {
		
		boolean change = false;
		if (gridSpacing != 0) {
			// distance from snap position in x and y
			double rem_x = pos[0] % gridSpacing;
			double rem_y = pos[1] % gridSpacing;
			
			// round up or down to the next snap
			if (rem_x != 0) {
				double per_cent_rem = Math.abs(rem_x / gridSpacing);
				if (per_cent_rem >= 0.5) {
					if (rem_x > 0) {
						pos[0] += gridSpacing - rem_x;
					} else {
						pos[0] -= gridSpacing + rem_x;
					}
				} else {
					pos[0] -= rem_x;
				}
				change = true;
			}
			if (rem_y != 0) {
				double per_cent_rem = Math.abs(rem_y / gridSpacing);
				if (per_cent_rem >= 0.5) {
					if (rem_y > 0) {
						pos[1] += gridSpacing - rem_y;
					} else {
						pos[1] -= gridSpacing + rem_y;
					}
				} else {
					pos[1] -= rem_y;
				}
				change = true;
			}
		}
		return(change);
	}
	
	/**
	 * Modify the argument moving position value to align with the grid spacing.
	 *
	 * @param moving_pos The position that is in motion
	 * @param fixed_pos The position that is stationary
	 * @return true if the moving_pos array has been modified, false if the
	 * array remains unchanged.
	 */
	public boolean alignVectorToGridSpacing(double[] moving_pos, double[] fixed_pos) {
		
		boolean change = false;
		if (gridSpacing != 0) {
			
			double x0 = fixed_pos[0];
			double y0 = fixed_pos[1];
			double x1 = moving_pos[0];
			double y1 = moving_pos[1];
			double delta_x = x1 - x0;
			double delta_y = y1 - y0;
			
			Vector2d vec = new Vector2d(delta_x, delta_y);
			
			double length = vec.length();
			
			double rem = length % gridSpacing;
			
			// round up or down to the next grid space
			if (rem != 0) {
				double per_cent_rem = rem / gridSpacing;
				double changed_length = length;
				if (per_cent_rem >= 0.5) {
					changed_length += gridSpacing - rem;
				} else {
					changed_length -= rem;
				}
				double scale = changed_length / length;
				vec.scale(scale);
				moving_pos[0] = fixed_pos[0] + vec.x;
				moving_pos[1] = fixed_pos[1] + vec.y;
				
				change = true;
			}
		}
		return(change);
	}
}
