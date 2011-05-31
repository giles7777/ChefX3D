/*****************************************************************************
 *                        Web3d.org Copyright (c) 2006
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

// External Imports

// Local imports
// None

/**
 * Cloneable the property
 * 
 * @author Russell Dodds
 * @version $Revision: 1.4 $
 */
public interface CloneableProperty extends Cloneable {

    /**
     * Create a copy of the Entity
     */
    public CloneableProperty clone();
    
}