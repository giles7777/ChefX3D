/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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

//external imports
//none

//internal imports
//none

/**
 * A class that provides a way of comparing floating point
 * values beyond a simple '==' equals check.<br>
 * For instance, almostEqual2sComplement uses an integer 
 * representation comparison.
 * This is better than using the epsilon technique (which asks:
 * are two numbers within some epsilon value of one another?),
 * which can be meaningful for values close to the epsilon value
 * in size, but loses all usefulness for very large floating 
 * point values.
 *   
 * @author Eric Fickenscher
 * @version $Revision: 1.1 $
 */
public class FloatingPointComparison {

	/**
	 * AlmostEqual2sComplement is an effective way of handling floating point 
	 * comparisons that takes advantage of the IEEE float format.  <br>IEEE   
	 * float numbers are “lexicographically ordered”, and this allows us
	 * to 'forgive' the imperfections of floating point math errors:<ul><li>
	 * We take a particular expected result 'A' and accept 'B' as equal
	 * provided it is within 'maxUlps' representable floats above or below 'A'.  
	 * </li></ul><p>   
	 * It has the following characteristics:<br>
	 * <ol>
	 * <li> Measures whether two floats are ‘close’ to each other, where close
	 * is defined by ulps [Units in the Last Place], also interpreted as how 
	 * many representable floats there are in-between the numbers. </li>
	 * <li> Treats infinity as being close to Float.MAX_VALUE </li>
	 * <li> Treats NANs as being four million ulps away from everything 
	 * (assuming the default NAN value for x87), except other NANs </li>
	 * <li> Accepts greater relative error as numbers gradually underflow to 
	 * subnormals (also known as denormals; numbers that are so small that 
	 * they cannot be normalized.)
     * <li> Treats tiny negative numbers as being close to tiny positive 
     * numbers. </li>
     * </ol>
	 * @param A float value
	 * @param B float value
	 * @param maxUlps the maximum error in terms of Units in the Last Place. 
	 * This specifies how big an error we are willing to accept in terms of 
	 * the value of the least significant digit of the floating point number’s 
	 * representation. <br>maxUlps can also be interpreted in terms of how many 
	 * representable floats we are willing to accept between A and B. This 
	 * function will allow maxUlps-1 floats between A and B.<p>
	 * For a normal float number a maxUlps of 1 is equivalent to a maxRelativeError 
	 * of between 1/8,000,000 and 1/16,000,000. The variance is because the accuracy 
	 * of a float varies slightly depending on whether it is near the top or bottom 
	 * of its current exponent’s range.
	 * @return TRUE if there are no more than (maxUlps-1) floats between A 
	 * and B; false otherwise.
	 * @see http://floating-point-gui.de/errors/comparison/
	 * @see http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
	 * @author Bruce Dawson
	 */
	public static boolean almostEqual2sComplement(float A, float B, int maxUlps){

	    // Make sure maxUlps is non-negative and small enough that the
	    // default NAN won't compare as equal to anything.
	    if( maxUlps < 0 || maxUlps > 4 * 1024 * 1024)
	    	throw new NumberFormatException("maxUlps must be between 0 and 4194304");

	    int aInt = Float.floatToIntBits(A);

	    // Make aInt lexicographically ordered as a twos-complement int

	    if (aInt < 0)

	        aInt = 0x80000000 - aInt;

	    // Make bInt lexicographically ordered as a twos-complement int

	    int bInt = Float.floatToIntBits(B);

	    if (bInt < 0)

	        bInt = 0x80000000 - bInt;

	    int intDiff = Math.abs(aInt - bInt);

	    if (intDiff <= maxUlps)
	        return true;

	    return false;
	}

	
	/**
	 * AlmostEqual2sComplement is an effective way of handling floating point
	 * double  comparisons that takes advantage of the IEEE double format.<br>
	 * IEEE double numbers are “lexicographically ordered”, and this allows us
	 * to 'forgive' the imperfections of floating point math errors:<ul><li>
	 * We take a particular expected result 'A' and accept 'B' as equal
	 * provided it is within 'maxUlps' representable doubles above or below 'A'.  
	 * </li></ul><p>   
	 * It has the following characteristics:<br>
	 * <ol>
	 * <li> Measures whether two doubles are ‘close’ to each other, where close
	 * is defined by ulps [Units in the Last Place], also interpreted as how 
	 * many representable floats there are in-between the numbers. </li>
	 * <li> Treats infinity as being close to Double.MAX_VALUE </li>
	 * <li> Treats NANs as being four million ulps away from everything 
	 * (assuming the default NAN value for x87), except other NANs </li>
	 * <li> Accepts greater relative error as numbers gradually underflow to 
	 * subnormals (also known as denormals; numbers that are so small that 
	 * they cannot be normalized.)
     * <li> Treats tiny negative numbers as being close to tiny positive 
     * numbers. </li>
     * </ol>
	 * @param A double value
	 * @param B double value
	 * @param maxUlps the maximum error in terms of Units in the Last Place. 
	 * This specifies how big an error we are willing to accept in terms of 
	 * the value of the least significant digit of the floating point number’s 
	 * representation. <br>maxUlps can also be interpreted in terms of how many 
	 * representable floats we are willing to accept between A and B. This 
	 * function will allow maxUlps-1 floats between A and B.<p>
	 * Since doubles have a 53-bit mantissa a one ulp error implies a relative
	 * error of between 1/4,000,000,000,000,000 and 1/8,000,000,000,000,000.
	 * The variance is because the accuracy of a double varies slightly 
	 * depending on whether it is near the top or bottom of its current 
	 * exponent’s range.
	 * @return TRUE if there are no more than (maxUlps-1) doubles between A 
	 * and B; false otherwise.
	 * @see http://floating-point-gui.de/errors/comparison/
	 * @see http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
	 * @author Bruce Dawson
	 * 
	 */
	public static boolean almostEqual2sComplement(double A, double B, int maxUlps){

	    // Make sure maxUlps is non-negative and small enough that the
	    // default NAN won't compare as equal to anything.
		if( maxUlps < 0 || maxUlps > 4 * 1024 * 1024)
	    	throw new NumberFormatException("maxUlps must be between 0 and 4194304");

	    long aLong = Double.doubleToLongBits(A);

	    // Make aInt lexicographically ordered as a twos-complement int

	    if (aLong < 0)
	    	
	        aLong = 0x8000000000000000L - aLong;

	    // Make bInt lexicographically ordered as a twos-complement int

	    long bLong = Double.doubleToLongBits(B);

	    if (bLong < 0)

	        bLong = 0x8000000000000000L - bLong;

	    long longDiff = Math.abs(aLong - bLong);

	    if (longDiff <= maxUlps)
	        return true;

	    return false;
	}
}
