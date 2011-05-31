/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005 - 2007
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

//External Imports
// none

// Internal Imports
// none

/**
 * An interface used to define a property editors validation
 * class
 * 
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public interface PropertyValidator {
    
    /**
     * Validate the value against a defined test
     * 
     * @param value The value to check
     * @return True if check is valid.
     */
    public boolean validate(Object value);

    /**
     * What message should the user see if this fails
     * 
     * @return The message
     */
    public String getMessage();

}
