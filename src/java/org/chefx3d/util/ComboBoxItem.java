/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
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

// External imports
// none

// Local imports
// none

/**
 * A simple data holder class for use with combo boxes
 *
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public class ComboBoxItem  {

    /** file extension */
    private String name;

    /** The filter description */
    private Object value;

    /**
     * Constructor
     *
     * @param extensions The list of supported extensions, null to support all
     * @param description The text to display
     */
    public ComboBoxItem(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    // ----------------------------------------------------------
    // Overridden Methods
    // ----------------------------------------------------------

    /**
     * Basic toString method
     */
    public String toString() {
        return name;
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Get the actual value
     */
    public Object getValue() {
        return value;
    }

}