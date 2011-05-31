/*****************************************************************************
 *                        Web3d.org Copyright (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.ui;

// External Imports
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

import org.chefx3d.util.FontColorUtils;
import org.j3d.util.I18nManager;

// Local imports

/**
 * StatusBar that show short messages to the user.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.2 $
 */
public class StatusBar extends JLabel {
	
	/** protected const for minimum widget width */
	protected static final int MINIMUM_WIDTH = 250;
	
	/** protected const for preferred widget width */
	protected static final int PREFERRED_WIDTH = 250;
	
	/** protected const for minimum widget height */
	protected static final int MINIMUM_HEIGHT = 16;
	
	/** protected const for preferred widget height */
	protected static final int PREFERRED_HEIGHT = 16;

	/** protected const for maximum number of chars to display */
	protected static final int MAXIMUM_CHAR = 170;
	
	/** protected static instance */
	protected static StatusBar statusBar = new StatusBar();
	
	/** Translation utility */
	I18nManager intl_mgr;
	
	/** Ready status message */
	private static final String READY_PROP = 
		"org.chefx3d.messaging.StatusBar.ready";
	
	
	/**
	 * Constructor - calls init to setup JLabel
	 */
	private StatusBar(){
		super();
		
		intl_mgr = I18nManager.getManager();
		init();
	}
	
	/**
	 * Retrieves static class instance
	 * 
	 * @return StatusBar (JLabel)
	 */
	public static StatusBar getStatusBar(){
		return statusBar;
	}
	
	/**
	 * Sets the status message - limited to maximum characater limit
	 * 
	 * @param msg String message to set
	 */
	public void setMessage(String msg){
		
		if(msg.length() > MAXIMUM_CHAR){
			
			msg = msg.substring(0, MAXIMUM_CHAR);
		}
		
		setText(" "+msg);
	}
	
	/**
	 * Clears the status bar
	 */
	public void clearStatusBar(){
		setText(" ");
	}
	
	/**
	 * Initializes the widget. Can be overwritten to provide specific gui 
	 * customization.
	 */
	protected void init(){
		
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
		setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT));
		setBorder(new BevelBorder(BevelBorder.LOWERED));
		setFont(FontColorUtils.getSmallFont());
		
		String msg = intl_mgr.getString(READY_PROP);
		
        setMessage(msg);
    }
}
