/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d;

// External Imports
// None

// Internal Imports
// None

/**
 * An authoring component.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.7 $
 */
public interface AuthoringComponent {
    /**
     * Get a component.
     * 
     * @return The component can be of three forms 1. null 2. hashmap (tabName = JPanel)
     */
    public Object getComponent();
    
}
