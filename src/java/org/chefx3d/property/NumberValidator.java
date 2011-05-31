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
public class NumberValidator 
    implements PropertyValidator, CloneableProperty {

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    public static enum numberTypes {INTEGER, LONG, FLOAT, DOUBLE};
    
    /** The type to validate as */
    private numberTypes type;
        
    /** The message to return to the user */
    private String message;
    
    /**
     * Create a IsNumberValidator that will check that a 
     * value is the type of number specified.
     *
     */
    public NumberValidator(numberTypes type) {
        this.type = type;
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
    
    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------  

    /**
     * Create a copy of the property
     */
    public NumberValidator clone() {
        
        // Create the new copy
        NumberValidator out = new NumberValidator(type);
        return out;

    }

    // ----------------------------------------------------------
    // Methods required by the PropertyValidator
    // ----------------------------------------------------------
    public boolean validate(Object value) {
                
        switch(type) {
            case INTEGER:               
                try {
                    Integer.parseInt(value.toString());
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n The number provided [" + value + "] is not an Integer";
                    return false;
                } 
                break;
            case LONG:               
                try {
                    Long.parseLong(value.toString());
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n The number provided [" + value + "] is not a Long";
                    return false;
                } 
                break;
            case FLOAT:
                try {
                    Float.parseFloat(value.toString());
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n The number provided [" + value + "] is not a Float";
                    return false;
                } 
                break;
            case DOUBLE:
                try {
                    Double.parseDouble(value.toString());
                } catch (NumberFormatException nfe) {
                    message = "Data Validation Error:\n The number provided [" + value + "] is not a Double";
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