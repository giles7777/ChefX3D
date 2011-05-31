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

package org.chefx3d.property;

// External Imports

// Internal Imports
import org.chefx3d.model.PropertyValidator;
import org.chefx3d.util.CloneableProperty;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A validator that makes sure the data provided is a 3 part 
 * vector with integer values between 0 and 255
 *
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public class ColorValidator 
    implements PropertyValidator, CloneableProperty {

    public static enum numberTypes {INTEGER, LONG, FLOAT, DOUBLE};

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** The message to return to the user */
    private String message;
    
    /**
     * Create a ColorValidator that will check that a 
     * string "x, y, z" are all numbers between 1 and 255
     */
    public ColorValidator() {        
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------  

    /**
     * Create a copy of the property
     */
    public ColorValidator clone() {
        
        // Create the new copy
        ColorValidator out = new ColorValidator();
        return out;

    }
    
    // ----------------------------------------------------------
    // Methods required by the PropertyValidator
    // ----------------------------------------------------------
    public boolean validate(Object value) {

        message = "";

        String[] parts = value.toString().split(",");
        if (parts.length != 3) {
            message = "Data Validation Error:\n The data provided [" + value + 
            "] does not contain 3 numbers separated by a comma.";
            return false;          
        }
                      
        try {
            
            for (int i = 0; i <= 2; i++) {
                
                int checkValue = Integer.parseInt(parts[i].trim());
                
                if (checkValue > 255 || checkValue < 0) {
                    message = "Data Validation Error:\n One or more of the " +
                    "numbers provided [" + value + "] are not between 0 and 255";
                    return false;
                }
            }
            
        } catch (NumberFormatException nfe) {
            
            message = "Data Validation Error:\n One or more of the " +
            		"numbers provided [" + value + "] are not Integers";
            return false;
            
        } 

        return true;
         
    }

    /**
     * The message the user will see if failed
     */
    public String getMessage() {
        return message;
    }
    
    // ----------------------------------------------------------
    // Local methods
    // ----------------------------------------------------------

}