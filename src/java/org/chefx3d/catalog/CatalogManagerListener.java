/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.catalog;

// Standard Imports

// Application specific imports

/**
 * A listener for changes in the overall catalog.
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public interface CatalogManagerListener {
    
    /**
     * A catalog has been added. 
     *
     * @param catalog 
     */
    public void catalogAdded(Catalog catalog);
    
    /**
     * A catalog has been removed. 
     *
     * @param catalog 
     */
    public void catalogRemoved(Catalog catalog);

}
