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
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * An editor that add's properties to the tree.
 *
 * @author Alan Hudson
 * @version $Revision: 1.3 $
 */
public class InRangeValidator
    implements PropertyValidator, CloneableProperty {

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The start value */
    private double start;
    
    /** The end value */
    private double end;
    
    /** The converted display value */
    private float startDisplay;

    /** The converted display value */
    private float endDisplay;    
    
    /** The current unit of measurement, default is meters */
    private Unit unitOfMeasure;

    /** The value to check */
    private double checkValue;
    
    /** The message to return to the user */
    private String message;
    
    private String unitLabel;
    
    /**
     * Create a InRangeValidator that will check that a 
     * value is within a specified range.
     *
     */
    public InRangeValidator(double start, double end) {     
        this(start, end, Unit.METERS);
    }
    
    /**
     * Create a InRangeValidator that will check that a 
     * value is within a specified range.
     *
     */
    public InRangeValidator(double start, double end, Unit unitOfMeasure) {
        this.start = start;
        this.end = end;
        this.unitOfMeasure = unitOfMeasure;
        
        startDisplay = UnitConversionUtilities.convert(
                Unit.METERS, 
                (float)start, 
                unitOfMeasure);
        endDisplay = UnitConversionUtilities.convert(
                Unit.METERS, 
                (float)end, 
                unitOfMeasure);
        unitLabel = UnitConversionUtilities.getLabel(unitOfMeasure);
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }


    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------  

    /**
     * Create a copy of the property
     */
    public InRangeValidator clone() {
        
        // Create the new copy
        InRangeValidator out = new InRangeValidator(start, end, unitOfMeasure);
        return out;

    }
    
    // ----------------------------------------------------------
    // Methods required by the PropertyValidator
    // ----------------------------------------------------------
    public boolean validate(Object value) {
        
        try {
            
            checkValue = Double.parseDouble(value.toString());           
            float convertedValue = UnitConversionUtilities.convert(
                    unitOfMeasure , 
                    (float)checkValue, 
                    Unit.METERS);
            
            if (convertedValue <= end && convertedValue >= start) {
                return true;
            } else {
                
                StringBuilder builder = new StringBuilder();
                builder.append("Data Validation Error:\n");
                builder.append("The number provided [");
                builder.append(checkValue);
                builder.append(" ");
                builder.append(unitLabel);
                builder.append("] does not fall in the valid range [");
                builder.append(startDisplay);
                builder.append(" ");
                builder.append(unitLabel);
                builder.append(" -> ");
                builder.append(endDisplay);
                builder.append(" ");
                builder.append(unitLabel);
                builder.append("].");
                
                message = builder.toString();
                return false;
            }
            
        } catch (NumberFormatException nfe) {
            
            StringBuilder builder = new StringBuilder();
            builder.append("Data Validation Error:\n");
            builder.append("The data provided [");
            builder.append(checkValue);
            builder.append(" ");
            builder.append(unitLabel);
            builder.append("] must be a number.");
            
            message = builder.toString();
            return false;
        } 
        
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