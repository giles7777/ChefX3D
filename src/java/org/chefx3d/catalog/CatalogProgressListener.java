/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/gpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package org.chefx3d.catalog;

// external imports
// internal imports

/**
 * Simple listener interface to allow listeners to respond to
 * the manager (CatalogParser) as it informs them of the items
 * that have loaded so far.
 * 
 * @author Eric Fickenscher
 * @version $Revision: 1.3 $
 */
public interface CatalogProgressListener {

    /**
     * Set the total progress increments required 
     * 
     * @param totalNeeded
     */
    public void setMaximum(int totalNeeded);
    
	/** 
	 * Update the current increment
	 *  
	 * @param value the number of items that have loaded
	 */
    
	public void incrementValue(int value);
	
	/** 
	 * Indicate that progress is done.
	 */
	public void progressComplete();

}
