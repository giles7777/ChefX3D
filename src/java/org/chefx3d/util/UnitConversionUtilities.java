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

import java.text.DecimalFormat;
import java.text.NumberFormat;

// external imports
// none

// internal imports
// none

/**
 * This class is used to convert between different units of measurement.
 * @author Eric Fickenscher
 * @version $Revision: 1.9 $
 */
public class UnitConversionUtilities {
	
	/** A code to describe the American standard of measurement */
	public static final String IMPERIAL = "imperial";
	
	/** A code to describe the metric measurement system */
	public static final String METRIC = "metric";
	
	public static NumberFormat measurementFormatter = 
		DecimalFormat.getInstance();


	/** 
	 * A list of possible units of measurements,
	 * including their name, the conversion factor
	 * to get to meters, and whether or not we should
	 * multiply (true) or divide (false) the number of 
	 * units by the given scale in order to calculate
	 * a given distance in meters.
	 */
	public enum Unit {
		INCHES	   ("in", 	 0.0254f, 	true),
		FEET	   ("ft", 	 0.3048f, 	true),
		MILES	   ("mi", 	 1609.344f, true),
		MILLIMETERS("mm",    1000f, 	false),
		CENTIMETERS("cm",    100f, 		false),
		METERS	   ("m", 	 1, 		true),
		KILOMETERS ("km", 	 1000, 		true);
		
		/** Shortened name for the unit */
		private final String label;
		
		/** Scale by which we multiply or divide unit to get number of meters */
		private final float factor;
		
		/** If TRUE, we multiply the unit by its factor to get meters; if FALSE,
		 * we divide the unit by its factor to get meters. */
		private final boolean multiply;
		
		/** Constructor for the enumerated units of measurement.
		 * 
		 * @param name Abbreviated name for the unit (ie: mm for millimeters)
		 * @param scale A factor to use to convert this unit to meters
		 * @param multiplyByScaleToGetMeters if TRUE, we convert this unit to
		 * meters by multiplying by the scale parameter.  If FALSE, we divide
		 * by the scale parameter to convert this unit to meters.
		 */
		Unit(String name, float scale, boolean multiplyByScaleToGetMeters){
			label = name;
			factor = scale;
			multiply = multiplyByScaleToGetMeters;
		}
		
		/**
		 * What is the abbreviation for this type of unit?
		 * @return the name of this type of unit
		 */
		public String getLabel(){
			return label;
		}
		
	}
	
	/**
	 * Convert the given length in "from" units into the
	 * appropriate number of "to" units. 
	 * @param from The units of the length parameter
	 * @param length The number of "from" units
	 * @param to The units to which we want to convert
	 * @return a length converted to the "to" unit of measurement 
	 */
	public static float convert(Unit from, float length, Unit to){
		
		return convertMetersTo(convertUnitsToMeters(length, from), to);
	}
	
	
	/**
	 * Convert the specified length in meters into the length of the
	 * specified unit type.
	 * @param lengthInMeters The input length in meters
	 * @param unit The unit type to which we are converting lengthInMeters
	 * @return the number of units long of the given lengthInMeters
	 */
	public static float convertMetersTo(float lengthInMeters, Unit unit){
		
		if(unit.multiply)
			return lengthInMeters / unit.factor;
		else
			return lengthInMeters * unit.factor;
	}
	
	
	/**
	 * Convert the specified number of the specified unit into meters.
	 * @param length The length of the specified unit
	 * @param unit The measurement unit for the input length
	 * @return The value, in meters, of the specified number of units
	 */
	public static float convertUnitsToMeters(float length, Unit unit){
		
		if(unit.multiply)
			return length * unit.factor;
		else
			return length / unit.factor;
	}
	
	
	/**
	 * What is the abbreviation for this type of unit?
	 * @param unit The measurement unit
	 */
	public static String getLabel(Unit unit){
		return unit.label;
	}
	
	/**
	 * Convert the measurement code into a Unit.  Supported
	 * codes are: in, ft, mi, mm, cm, m, km
	 * 
	 * @param name The unit code
	 * @return The Unit matched, METER if un-matched
	 */
    public static Unit getUnitByCode(String code) {
    	    	
        if (code.equals(Unit.INCHES.getLabel())) {
            return Unit.INCHES;
        } else if (code.equals(Unit.FEET.getLabel())) {
            return Unit.FEET;
        } else if (code.equals(Unit.MILES.getLabel())) {
            return Unit.MILES;
        } else if (code.equals(Unit.MILLIMETERS.getLabel())) {
            return Unit.MILLIMETERS;
        } else if (code.equals(Unit.CENTIMETERS.getLabel())) {
            return Unit.CENTIMETERS;
        } else if (code.equals(Unit.METERS.getLabel())) {
            return Unit.METERS;
        } else if (code.equals(Unit.KILOMETERS.getLabel())) {
            return Unit.KILOMETERS;
        } else 
        	return Unit.METERS;
    }

    /**
     * Return either METRIC or IMPERIAL, the measurement system
     * @param code The unit code.
     * @return a string equal to either "METRIC" or "IMPERIAL",
     * the measurement system to which the parameter code belongs.
     */
    public static String getMeasurementSystemByCode(String code){
    	return getMeasurementSystem(getUnitByCode(code));
    }
    
    /**
     * Return either METRIC or IMPERIAL, the measurement sytem
     * @param unit The Unit type
     * @return a string equal to either "METRIC" or "IMPERIAL",
     * the measurement system to which the parameter unit belongs.
     */
    public static String getMeasurementSystem(Unit unit){
    	
    	switch (unit){
    		case MILLIMETERS:
    		case CENTIMETERS:
    		case METERS:
    		case KILOMETERS:
    			return METRIC;
    		case INCHES:
    		case FEET:
    		case MILES:
    		default:
    			return IMPERIAL;
    	}
    }
    
    /**
     * Has a fixed unit of measurement been defined.  If so then use that
     * instead of a calculated unit based on the current zoom of the ruler.
     * 
     * If the config file is not set or invalid then false is returned by 
     * default.
     * 
     * @return True is static value is to be used, false otherwise
     */
    public static boolean isStaticOverlayUnitOfMeasure() {
    	return Boolean.valueOf(
    			ConfigManager.getManager().getProperty(
    					"useStaticOverlayUnit", "false"));
    }
    
    /**
     * Has a fixed unit of measurement been defined.  If so then use that
     * instead of a calculated unit based on the current zoom of the ruler.
     * 
     * If the config file is not set or invalid then meter is returned by 
     * default.
     * 
     * @return The unit of measure to use
     */
    public static Unit getStaticOverlayUnitOfMeasure() {
    	
    	String unitLabel = 
    		ConfigManager.getManager().getProperty(
    					"staticOverlayUnit", null);
    	
    	return getUnitByCode(unitLabel);
    					
    }
    
    /**
     * Use the unit and value to determine a display string.  If the unit is
     * metric then the display is merely a string version of the value.  If the 
     * unit is imperial then the remainder fraction is converted to the closest
     * 1/8th.  The whole number plus the fraction is then concatenated and 
     * returned.
     * 
     * @param unit
     * @param value
     * @return
     */
    public static String getFormatedNumberDisplay(Unit unit, float value) {
    	
    	// do not bother with fractional displays if using the metric system
    	if (getMeasurementSystem(unit).equals(METRIC)) {
    		return measurementFormatter.format(value);
    	}
    	
    	// first we need to calculate the remainder using the stipulated unit
    	int wholeNumber = (int)Math.floor(value);
    	if (wholeNumber < 0) {
    		wholeNumber++;
    	}
    	float remainder = Math.abs(value - wholeNumber);
    	
    	String retVal = "";
    	
    	// now look at the remainder and round to the nearest fraction 
    	// supported
    	if (remainder < 0.0625) {
    		retVal = createFraction(wholeNumber, "");
    	} else if (remainder >= 0.0625 && remainder < 0.1875) {
    		retVal = createFraction(wholeNumber, "1/8");
    	} else if (remainder >= 0.1875 && remainder < 0.3125) {
    		retVal = createFraction(wholeNumber, "1/4");
    	} else if (remainder >= 0.3125 && remainder < 0.4375) {
    		retVal = createFraction(wholeNumber, "3/8");
    	} else if (remainder >= 0.4375 && remainder < 0.5625) {
    		retVal = createFraction(wholeNumber, "1/2");
    	} else if (remainder >= 0.5625 && remainder < 0.6875) {
    		retVal = createFraction(wholeNumber, "5/8");
    	} else if (remainder >= 0.6875 && remainder < 0.8125) {
    		retVal = createFraction(wholeNumber, "3/4");
    	} else if (remainder >= 0.8125 && remainder < 0.9375) {
    		retVal = createFraction(wholeNumber, "7/8");
    	} else {
    		retVal = createFraction(wholeNumber + 1, "");
    	}

    	return retVal;
    	
    }
    
    /**
     * Concatenated the whole number and fraction together to create a String.  If 
     * the fraction is empty just the number will be returned.  If the fraction
     * is not empty and the number is 0 just the fraction will be return.  
     * Otherwise the number and fraction will be concatenated with a space in 
     * between.  A null fraction will be treated as an mty string.
     * 
     * @param number
     * @param fraction
     * @return
     */
    private static String createFraction(int number, String fraction) {
    	
    	// treat a null as an empty string
    	if (fraction == null) {
    		fraction = "";
    	}
    	
    	if (fraction.equals("")) {
    		return String.valueOf(number);
    	} else {
    		if (number == 0) {
    			return fraction;
    		}    		
    	}
    	
    	return String.valueOf(number) + " " + fraction;
    }
    
}
