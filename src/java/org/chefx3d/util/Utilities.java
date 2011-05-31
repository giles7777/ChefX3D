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

package org.chefx3d.util;

// External Imports
import java.awt.Dimension;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.awt.geom.Point2D;

import java.text.NumberFormat;
import javax.swing.JOptionPane;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;

// Local Imports
// None

/**
 * A holder and manager of application parameters
 * <p>
 *
 * Parameters are, typically, read in during the class start up time from
 * whatever environment started the app. This can then be queried at any
 * time during the running of the application for the values of those
 * properties.
 *
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public class Utilities {

    /**
     * Private constructor to prevent direct instantiation.
     */
    private Utilities() {}

    /**
     * Opens a web page in a default web browser.
     *
     * <p>
     *
     * Since we're running our code on Java 5, we don't have a direct way to access launch a
     * default web browser from Java application.  In Java 6, however, by using Desktop class
     * you can get an access to a default web browser.
     *
     * The source of this code is pulled from:
     * <a href="http://www.centerkey.com/java/browser/">http://www.centerkey.com/java/browser/</a>
     *
     * @param url URL of the web page
     */
    public static void openUrl(final String url) {

        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {

                    String osName = System.getProperty("os.name");
                    try {

                        if (osName.startsWith("Mac OS")) {

                            Class fileMgr = Class.forName("com.apple.eio.FileManager");

                            Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});

                            openURL.invoke(null, new Object[] {url});
                        }
                        else if (osName.startsWith("Windows")) {

                            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                        }
                        else { //assume Unix or Linux

                            String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };

                            String browser = null;

                            for (int count = 0; count < browsers.length && browser == null; count++)
                            {
                                if (Runtime.getRuntime().exec( new String[] {"which", browsers[count]}).waitFor() == 0) {
                                    browser = browsers[count];
                                }
                            }

                            if (browser == null) {
                                throw new Exception("Could not find web browser");
                            }
                            else {
                                Runtime.getRuntime().exec(new String[] {browser, url});
                            }
                        }
                    }
                    catch (Exception e) {

                        JOptionPane.showMessageDialog(null, "Error attempting to launch web browser" + ":\n" + e.getLocalizedMessage());
                        return false;

                    }

                    return true;

                }

            }

        );

    }

    /**
     * Helper function to parse a string of 3 color values to the
     * float array
     *
     * @param colors array of 3 float for RGB color values
     * @return
     */
    public static float[] parseColorString(String colors) {

        // set a working variable
        float[] retVal = new float[] {0, 0, 0};

        // parse the string to primative data
        String[] strColor = colors.split(" ");

        try {
            retVal[0] = Float.parseFloat(strColor[0]);
        } catch (Exception e) {
        }

        try {
            retVal[1] = Float.parseFloat(strColor[1]);
        } catch (Exception e) {
        }

        try {
            retVal[2] = Float.parseFloat(strColor[2]);
        } catch (Exception e) {
        }

        return retVal;

    }

    /**
     * Input: a float and an integer representing number of decimal places.
     * Output: a String of the float rounded to the specified number of decimal places.
     * @author Eric Fickenscher
     * @param f input float number to be rounded
     * @param numberOfDecimalPlaces the number of decimal places to which float f
     * will be rounded
     * @return a String representation of a float rounded to the
     * specified number of decimal places.
     */
    public static String roundFloat(float f, int numberOfDecimalPlaces){
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(numberOfDecimalPlaces);
        formatter.setMinimumFractionDigits(numberOfDecimalPlaces);
        return formatter.format(f);
    }

    /**
     * Input: a double and an integer representing number of decimal places.
     * Output: a String of the double rounded to the specified number of decimal places.
     * @author Eric Fickenscher
     * @param d input double number to be rounded
     * @param numberOfDecimalPlaces the number of decimal places to which double d
     * will be rounded
     * @return a String representation of a double rounded to the
     * specified number of decimal places.
     */
    public static String roundDouble(double d, int numberOfDecimalPlaces){
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(numberOfDecimalPlaces);
        formatter.setMinimumFractionDigits(numberOfDecimalPlaces);
        return formatter.format(d);
    }

    /**
     * Input: two Dimensions.
     * Output: A new dimension with width equal to that of the wider of the
     * two input Dimensions and with height equal to that of the taller of the
     * two input Dimensions.
     *
     * @author Eric Fickenscher
     * @param d1 a dimension
     * @param d2 another dimension
     * @return a Dimension large enough to contain either input Dimension
     */
    public static Dimension getBigger(Dimension d1, Dimension d2) {

        return new Dimension((d1.width < d2.width)  ? d2.width :  d1.width,
                            (d1.height < d2.height) ? d2.height : d1.height);
    }

    /**
     * Finds the distance between two 2-dimensional points
     * with floating point precision
     * @author Jonathon Hubbard
     * @param p1  The first point
     * @param p2  The second point
     * @return The distance between the two points as a float
     */
    public static float distanceTween2Points(Point2f p1,Point2f p2){
        float deltaX = p2.x - p1.x;
        float deltaY = p2.y - p1.y;
        float distance = ( (deltaX * deltaX) + (deltaY * deltaY));
        distance = (float)Math.sqrt(distance);
        return distance;

    }

    /**
     * Finds the distance between two 2-dimensional points
     * with floating point precision
     * @author Jonathon Hubbard, Eric Fickenscher
     * @param p1  The first point
     * @param p2  The second point
     * @return The distance between the two points as a float
     */
    public static float distanceTween2Points(Point2D.Float p1,Point2D.Float p2){
        float deltaX = p2.x - p1.x;
        float deltaY = p2.y - p1.y;
        float distance = ( (deltaX * deltaX) + (deltaY * deltaY));
        distance = (float)Math.sqrt(distance);
        return distance;

    }

    /**
     * Finds the distance between two 3-dimensional points
     * with floating point precision
     * @author Jonathon Hubbard
     * @param p1  The first point
     * @param p2  The second point
     * @return The distance between the two points as a float
     */
    public static float distanceTween2Points(Point3f p1,Point3f p2){
        float deltaX = p2.x - p1.x;
        float deltaY = p2.y - p1.y;
        float deltaZ = p2.z - p1.z;
        float distance = ((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        distance = (float)Math.sqrt(distance);
        return distance;

    }
}
