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

package org.chefx3d.model;

// External Imports
import org.w3c.dom.Node;

// Internal Imports
// None

/**
 * Notification of changes in the structure of a property sheet.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public interface PropertyStructureListener {
    /**
     * A property was added.
     * 
     * @param local Was this a local change
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param propertyValue The value being set.
     */
    public void propertyAdded(boolean local, int entityID,
            String propertySheet, String propertyName, Node propertyValue);

    /**
     * A property was removed.
     * 
     * @param local Was this a local change
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(boolean local, int entityID,
            String propertySheet, String propertyName);
}
