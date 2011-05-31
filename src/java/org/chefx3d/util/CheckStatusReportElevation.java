/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.util;

//External imports

//Internal imports

/**
 * Rule engine for handling movement (translation) commands.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.5 $
 */
public class CheckStatusReportElevation {

	/** 
	 * Available elevation levels. Uses ordinal values to compare severity.
	 * Severe is the highest at 0, None is the lowest severity. Add accordingly. 
	 */
	public static enum ELEVATION_LEVEL {SEVERE, WARNING, INFORMATION, NONE};
	
    /** The illegal condition selection box color - red */
    private static final float[] ILLEGAL_CONDITION_COLOR = 
    	new float[] {1.0f, 0.0f, 0.0f};

    /** The warning condition selection box color - yellow */
    private static final float[] WARNING_CONDITION_COLOR = 
    	new float[] {1.0f, 1.0f, 0.0f};
    
    /** The information condition selection box color - green */
    private static final float[] INFORMATION_CONDITION_COLOR = 
    	new float[] {0.0f, 1.0f, 0.0f};
	
	/** Tracks the current elevation level */
	private ELEVATION_LEVEL elevationLevel;
	
	/**
	 * Default constructor
	 */
	public CheckStatusReportElevation(){
		
		resetElevationLevel();
	}
	
	/**
	 * Reset the elevation level
	 */
	public void resetElevationLevel(){
		
		elevationLevel = ELEVATION_LEVEL.NONE;
	}
	
	/**
	 * Raises the elevation level if the value is a higher severity than
	 * the currently assigned one.
	 * 
	 * @param level Level to elevate to
	 * @return True if elevated, false if below existing level
	 */
	public boolean setElevationLevel(ELEVATION_LEVEL level){
		
		if(level.ordinal() <= elevationLevel.ordinal()){
			
			elevationLevel = level;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the appropriate status color for the current status level.
	 * 
	 * @return float[] color value
	 */
	public float[] getStatusColor(){
		
		switch(elevationLevel){
		
			case SEVERE:
				return ILLEGAL_CONDITION_COLOR;
			case WARNING:
				return WARNING_CONDITION_COLOR;
			case INFORMATION:
				return INFORMATION_CONDITION_COLOR;
			case NONE:
			default:
				return null;
		}
	}
}
