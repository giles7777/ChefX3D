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
 * An editor that add's properties to the tree.
 *
 * @author Alan Hudson
 * @version $Revision: 1.2 $
 */
public class VectorValidator 
    implements PropertyValidator, CloneableProperty {

    public static enum numberTypes {INTEGER, LONG, FLOAT, DOUBLE};

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The type to validate as */
    private numberTypes type;
     
    /** How many data points are there */
    private int size;
    
    /** The message to return to the user */
    private String message;
    
    /**
     * Create a VectorValidator that will check that a 
     * vector string "x, y, z" are all valid numberic types
     *  
     * @param type The numeric type (int, double, float, long)
     * @param size The number of valid data points
     */
    public VectorValidator(numberTypes type, int size) {
        this.type = type;
        this.size = size;
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------  

    /**
     * Create a copy of the property
     */
    public VectorValidator clone() {
        
        // Create the new copy
        VectorValidator out = new VectorValidator(type, size);
        return out;

    }
    
    // ----------------------------------------------------------
    // Methods required by the PropertyValidator
    // ----------------------------------------------------------
    public boolean validate(Object value) {
             
        String[] parts = value.toString().split(",");
        if (parts.length != size) {
            message = "Data Validation Error:\n The data provided [" + value + 
            "] does not contain " + size + " numbers separated by a comma.";
            return false;          
        }
        
        switch(type) {
            case INTEGER:               
                try {
                    for (int i = 0; i < size; i++) {
                        Integer.parseInt(parts[i]);
                    }
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n One or more of the " +
                    		"numbers provided [" + value + "] are not Integers";
                    return false;
                } 
                break;
            case LONG:               
                try {
                    for (int i = 0; i < size; i++) {
                        Long.parseLong(parts[i]);
                    }
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n One or more of the " +
                    		"numbers provided [" + value + "] are not Longs";
                    return false;
                } 
                break;
            case FLOAT:
                try {
                    for (int i = 0; i < size; i++) {
                        Float.parseFloat(parts[i]);
                    }
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n One or more of the " +
                    		"numbers provided [" + value + "] are not Floats";
                    return false;
                } 
                break;
            case DOUBLE:
                try {
                    for (int i = 0; i < size; i++) {
                        Double.parseDouble(parts[i]);
                    }
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n One or more of the " +
                    		"numbers provided [" + value + "] are not Doubles";
                    return false;
                } 
                break;
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