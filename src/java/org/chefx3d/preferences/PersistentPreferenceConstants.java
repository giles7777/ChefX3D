/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.preferences;

//External Imports

//Internal Imports

/**
 * Persistent preferences constants shared throughout the application.
 * <p>
 * 
 * These are the specific application preference constants that should be
 * used to set and retrieve preferences. These are stored between sessions.
 * There may be class specific preference that are not represented here. The 
 * constants represented here are those preferences shared amongst classes. 
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public abstract class PersistentPreferenceConstants {

	//-------------------------------------------------------------------------
	// Preference Name Constants
	//-------------------------------------------------------------------------
	
	/** Unit of measurement to apply to the system. */
	public static final String UNIT_OF_MEASUREMENT = "unitOfMeasurement";
	
	/** Report logo to use. */
    public static final String REPORT_LOGO = "reportLogo";
    
    /** Report logo url to use. */
    public static final String REPORT_LOGO_URL = "reportLogoURL";
    
    /** Wall angle snap to use. */
    public static final String WALL_ANGLE_SNAP = "wallAngle";

    /** Header to use. */
    public static final String HEADER_PREF_KEY = "reportHeader";
    
    /** Should all pop up messages be blocked. */
    public static final String BLOCK_POP_UP_MSG_KEY = "blockPopUpMsg";
    
    /** Should all rules be overridden. */
    public static final String OVERRIDE_RULES_KEY = "overrideRules";
    
    /** The catalog uuid value. */
    public static final String CATALOGID_KEY = "catalogID";
 
    /** The user uuid value. */
    public static final String USERNAME_KEY = "username";

    /** The catalog uuid value. */
    public static final String CATALOG_UPDATE_VALID = "catalogValid";  
    
    /** Have the startup stats been completely logged */
    public static final String STARTUP_STATS_COMPLETED = "startupStatsCompleted";   
	
	//-------------------------------------------------------------------------
	// Preference Value Defaults
	//-------------------------------------------------------------------------
	
	/** Default value to return when getting unit of measurement. */
	public static final String DEFAULT_UNIT_OF_MEASUREMENT = "not initialized";
	
	/** Default report logo to use. */
    public static final String DEFAULT_REPORT_LOGO = "defaultReportLogo";
    
    /** Default wall angle snap to use. */
    public static final double DEFAULT_WALL_ANGLE_SNAP = 45.0;
    
}
