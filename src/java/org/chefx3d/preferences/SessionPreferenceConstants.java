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
 * Session preferences constants shared throughout the application.
 * <p>
 * 
 * These are session specific application preference constants that should be
 * used to set and retrieve preferences. These are not stored between 
 * sessions. There may be class specific preference that are not represented 
 * here. The constants represented here are those preferences shared amongst 
 * classes. 
 *
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */

public abstract class SessionPreferenceConstants {
	
	//-------------------------------------------------------------------------
	// Session Preferences Node
	//-------------------------------------------------------------------------
	
	/** Node identifying session specific preference branch. */
	public static final String SESSION_PREFERENCES_NODE = "session.node";
	
	//-------------------------------------------------------------------------
	// Preference Name Constants
	//-------------------------------------------------------------------------
	
	/** Direction to show measurements in, left or right. */
	public static final String MEASUREMENT_DIRECTION_KEY = "measureDirection";
	
    /** Boolean to indicate if the product list has been loaded. */
    public static final String PRODUCT_PRICING_LIST_LOADED_KEY = "pricingListLoaded";	
	
	//-------------------------------------------------------------------------
	// Possible Preference Values
	//-------------------------------------------------------------------------
	
	/** Negative measurement direction option. */
	public static int MEASUREMENT_DIRECTION_NEGATIVE = 0;
	
	/** Positive measurement direction option. */
	public static int MEASUREMENT_DIRECTION_POSITIVE = 1;
	
	//-------------------------------------------------------------------------
	// Preference Value Defaults
	//-------------------------------------------------------------------------
	
	/** Default measurement direction. */
	public static final int DEFAULT_MEASUREMENT_DIRECTION = 
		MEASUREMENT_DIRECTION_NEGATIVE;
}
