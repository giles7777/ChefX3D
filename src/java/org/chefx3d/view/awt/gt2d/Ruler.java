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

package org.chefx3d.view.awt.gt2d;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;

import java.text.DecimalFormat;

/**
 * Class Ruler, which represents a varying amount of distance on the
 * screen.
 * @author Eric Fickenscher
 * $Revision: 1.4 $
 *
 */
public class Ruler extends JComponent {

    /** The default background color for the ruler */
    private static final Color DEFAULT_BACKGROUND_COLOR = Color.white;

    /** The default font style */
    private static final Font DEFAULT_FONT_STYLE =
        new Font("SansSerif", Font.PLAIN, 10);

    /** The default color used for font and for ruler hash marks */
    private static final Color DEFAULT_HASH_MARK_COLOR = Color.black;

    /** The default color used for the mouse-indicator lines */
    private static final Color DEFAULT_MOUSE_INDICATOR_COLOR = Color.red;

    /** The amount of available space, in pixels, on which the units
     * are drawn - ie, the counterpart to ruler length.  This can be
     * either height or width depending on the ruler orientation.  */
    private static final int RULER_FATNESS = 35;

    /** The number of feet per meter, used in the setValueOfRuler() method */
    private static final double FEET_PER_METER = 3.2808399;

    /** The length, in pixels, of the tick marks on a horizontal ruler */
    private static final int HORIZONTAL_RULER_TICK_LENGTH = 10;

    /** The length, in pixels, of the tick marks on a vertically-aligned ruler */
    private static final int VERTICAL_RULER_TICK_LENGTH = 7;

    /** Hash marks will never be closer to one another
     * than this number of pixels */
    private static final int MIN_PIXELS_BETWEEN_HASH_MARKS = 35;

    /** A list of the possible ruler orientations */
    public static enum Orientation { VERTICAL, HORIZONTAL};

    /** A list of the available measurement systems */
    public static enum MeasurementSystem { METRIC, IMPERIAL};

    /** The current ruler orientation */
    private Orientation orientation;

    /** The current measurement system */
    private MeasurementSystem measurementSystem;

    /** Each pixel represents this many meters of space */
    private double distancePerPixel;

    /** This amount of represented distance (in meters) exists between each
     * hash mark (or 'tick mark') on the ruler*/
    private double incrementDistance;

    /** The pixel length of the ruler, not counting buffer space to show units */
    private int availablePixels;

    /** Length of ruler, in pixels - which could be either ruler
    * height or width, depending on the ruler's orientation. */
    private int length;

    /** The ruler represents a varying distance, which should always be
     * a positive value but can be both variable and arbitrary.  The total
     * number of units the ruler should represent is given by this value */
    private double representedDistance;

    /** Display values up to two decimal places */
    private DecimalFormat decimalFormat;

    /** If this variable is true, the panel containing the Ruler
     * should make use of the updateMouseCoords method because
     * mouse-position indicator lines will be drawn directly onto the ruler */
    private boolean displayMousePosition;

    /**  This variable stores the most-recently
     * updated x-value of the mousePosition  */
    private int mouseX;

    /**  This variable stores the most-recently
     * updated x-value of the mousePosition  */
    private int mouseY;

    /** The ruler begins mesauring at this value */
    private double firstHashMarkValue;

    /**
     * Constructor
     * @param o The orientation of the ruler
     * @param m The measurement system used by the ruler
     * @param lengthOfRuler the length of ruler, in pixels - could be
     * either height or width, depending on the ruler's orientation
     */
    public Ruler(Orientation o,
                 MeasurementSystem m,
                 int lengthOfRuler,
                 boolean showMouseIndicatorLines){
        orientation = o;
        measurementSystem = m;
        length = lengthOfRuler;
        representedDistance = 0;
        decimalFormat = new DecimalFormat("0.##");
        displayMousePosition = showMouseIndicatorLines;
        mouseX = 0;
        mouseY = 0;
        firstHashMarkValue = 0;

        if(orientation == Orientation.VERTICAL)
            setPreferredSize(new Dimension(RULER_FATNESS, length));
        else
            setPreferredSize(new Dimension(length, RULER_FATNESS));
    }

    //----------------------------------------------------------
    // Methods defined by JComponent
    //----------------------------------------------------------

    /**
     * Paint the component!  Pass a copy of the <code>Graphics</code>
     * object to protect the rest of the paint code from irrevocable changes
     * (for example, <code>Graphics.translate</code>).
     * <p>
     *
     * @param g the <code>Graphics</code> object to protect
     * @see #paint
     * @see ComponentUI
     */
    protected void paintComponent(Graphics g) {

        if(representedDistance <= 0) return;

        // the number of pixels with which we can work:
        Rectangle availableSpace = g.getClipBounds();

        String label = null;
        switch(measurementSystem){
            case METRIC:
                label = " m";
                break;
            case IMPERIAL:
                label = " ft";
                break;
        }

        // Fill clipping area with background color
        g.setColor(DEFAULT_BACKGROUND_COLOR);
        g.fillRect( availableSpace.x,
                    availableSpace.y,
                    availableSpace.width,
                    availableSpace.height);

        // Set the font of the ruler labels
        g.setFont(DEFAULT_FONT_STYLE);

        // use these variables for the tick marks
        int firstPoint = RULER_FATNESS-1;
        int secondPoint = 8;
        double nextHashMarkDistance = firstHashMarkValue;

        switch(orientation){

            case HORIZONTAL:
                availablePixels = availableSpace.width-RULER_FATNESS;
                calculateHashMarks();

                if(displayMousePosition){
                    g.setColor(DEFAULT_MOUSE_INDICATOR_COLOR);
                    g.drawLine(mouseX+RULER_FATNESS, firstPoint,
                           mouseX+RULER_FATNESS, 0);
                }
                g.setColor(DEFAULT_HASH_MARK_COLOR);

                secondPoint = firstPoint-HORIZONTAL_RULER_TICK_LENGTH;
//              g.drawLine(0, firstPoint, 0, secondPoint);
                g.drawString(label, 2, 21);

                break;

            case VERTICAL:

                availablePixels = availableSpace.height;
                calculateHashMarks();
                nextHashMarkDistance += RULER_FATNESS*distancePerPixel;

                if(displayMousePosition){
                    g.setColor(DEFAULT_MOUSE_INDICATOR_COLOR);
                    g.drawLine(firstPoint, mouseY, 0, mouseY);
                }
                g.setColor(DEFAULT_HASH_MARK_COLOR);

                secondPoint = firstPoint-VERTICAL_RULER_TICK_LENGTH;
//                g.drawLine(firstPoint, 0, secondPoint, 0);
                g.drawString(label, 6, 10);

                break;
        }
        label = null;

        double currentDistance = nextHashMarkDistance - distancePerPixel;
        for( int i = 0; i < availablePixels; i++){

            currentDistance += distancePerPixel;

            if(currentDistance >= nextHashMarkDistance){

                label = decimalFormat.format(nextHashMarkDistance);
                nextHashMarkDistance += incrementDistance;
                int l = RULER_FATNESS + i;

                // draw the next hash mark
                if (orientation == Orientation.HORIZONTAL) {
                    g.drawLine(l, firstPoint, l, secondPoint);
                    if (label != null)
                        g.drawString(label, l-3, 21);
                } else {
                    g.drawLine(firstPoint, l, secondPoint, l);
                    if (label != null)
                        g.drawString(label, 6, l+3);
                }
            }
        }
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Switch Orientation of the ruler
     * @param o
     */
    public void setOrientation( Orientation o){
        orientation = o;
    }


    /**
     * Switch to the given MeasurementSystem
     * @param m
     */
    public void setMeasurementSystem( MeasurementSystem m){
        measurementSystem = m;
    }


    /**
     * This method changes the representation of the ruler.  For
     * instance, if the ruler needs to represent a total length of
     * 53.2 meters, then this method should be called with a parameter
     * value of 53.2.  To fool the ruler into displaying imperial units,
     * we convert meters to feet if needed.
     * @param representedDistance Value of units that the ruler should
     * represent
     * @param min the minimum value of the ruler
     */
    public void setValueOfRuler(double newDistanceToRepresent,
                                double min){

        firstHashMarkValue = min;

        switch(measurementSystem){
            case METRIC:
                representedDistance = newDistanceToRepresent;
                break;
            case IMPERIAL:
                representedDistance = newDistanceToRepresent * FEET_PER_METER;
                break;
        }
    }


    /**
     * Calculate the incrementDistance in between each hash mark on the ruler.
     */
    private void calculateHashMarks(){

        incrementDistance = 1D;
        distancePerPixel = representedDistance / availablePixels;
        double numberOfMarks = representedDistance;
        double pixelsBetweenMarks = availablePixels / numberOfMarks;

        // what if hash marks are too far apart?
        while(pixelsBetweenMarks > MIN_PIXELS_BETWEEN_HASH_MARKS){

            incrementDistance /= 2;
            numberOfMarks = representedDistance / incrementDistance;
            pixelsBetweenMarks = availablePixels / numberOfMarks;
        }

        // what if hash marks are too close together?
        while(pixelsBetweenMarks < MIN_PIXELS_BETWEEN_HASH_MARKS){

            incrementDistance *= 2;
            numberOfMarks = representedDistance / incrementDistance;
            pixelsBetweenMarks = availablePixels / numberOfMarks;
        }
    }

    /**
     * Method updateMouseCoords should be called by the class controlling
     * the Ruler.  Instead of adding the mouseListener directly to Ruler,
     * this allows the class utilizing Ruler.java to adjust the mouse coords
     * to change functionality as desired.  If displayMousePosition is TRUE,
     * then these coords will be used to draw a mouse-position line onto the
     * ruler.
     * @param x the new X-position of the mouse
     * @param y the new Y-position of the mouse
     */
    public void updateMouseCoords(int x, int y){
//      if( x >= 0 && x < this.getWidth() &&
//          y >= 0 && y < this.getHeight()){
        mouseX = x;
        mouseY = y;
        repaint();
    }
}
