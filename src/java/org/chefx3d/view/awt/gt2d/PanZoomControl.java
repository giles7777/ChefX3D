/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
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

// External imports
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import java.awt.image.ImageObserver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.URL;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.chefx3d.util.FileLoader;

// Local Imports
// None

/**
 * Navigation controls for pan/zoom operations on a map panel.
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.2 $
 */
public class PanZoomControl implements MouseListener, MouseMotionListener, ImageObserver {

    /** Default control background image file */
    private static final String DEFAULT_CONTROL_IMAGE = "images/2d/navControl.png";

    /** Default zoom knob image file */
    private static final String DEFAULT_ZOOM_KNOB_IMAGE = "images/2d/zoomKnob.png";

    /** Default open hand cursor image file */
    private static final String DEFAULT_OPEN_HAND_CURSOR_IMAGE = "images/2d/openHandCursor.png";

    /** Default closed hand cursor image file */
    private static final String DEFAULT_CLOSED_HAND_CURSOR_IMAGE = "images/2d/closedHandCursor.png";

    /** Control Commands */
    public static final String PAN_UP_COMMAND = "PAN_UP";
    public static final String PAN_LEFT_COMMAND = "PAN_LEFT";
    public static final String RESET_COMMAND = "RESET";
    public static final String PAN_RIGHT_COMMAND = "PAN_RIGHT";
    public static final String PAN_DOWN_COMMAND = "PAN_DOWN";
    public static final String ZOOM_IN_COMMAND = "ZOOM_IN";
    public static final String ZOOM_OUT_COMMAND = "ZOOM_OUT";
    public static final String ZOOM_CHANGE_COMMAND = "ZOOM_CHANGE";

    /** Control Identifers */
    private static final int PAN_UP = 0;
    private static final int PAN_LEFT = 1;
    private static final int RESET = 2;
    private static final int PAN_RIGHT = 3;
    private static final int PAN_DOWN = 4;
    private static final int ZOOM_IN = 5;
    private static final int ZOOM_OUT = 6;
    private static final int ZOOM_CHANGE = 7;

    /** Control Command */
    private static final String[] COMMAND = new String[]{
        PAN_UP_COMMAND,
        PAN_LEFT_COMMAND,
        RESET_COMMAND,
        PAN_RIGHT_COMMAND,
        PAN_DOWN_COMMAND,
        ZOOM_IN_COMMAND,
        ZOOM_OUT_COMMAND,
        ZOOM_CHANGE_COMMAND,
    };

    /////////////////////////////////////////////////////////////////////////////////
    // NOTE: Hard coded parameters specific to the control graphic !

    /** The upper left corner of the control graphic, in x, y order */
    private static final int[][] UPPER_LEFT_CORNER = new int[][] {
        { 20,   0 }, // up
        {  0,  20 }, // left
        { 20,  20 }, // reset
        { 40,  20 }, // right
        { 20,  40 }, // down
        { 20,  65 }, // in
        { 20, 253 }, // out
        { 21,  85 }, // zoom
    };

    /** The area of each control space, in x, y order */
    private static final int[][] AREA = new int[][] {
        { 16,  16 }, // up
        { 16,  16 }, // left
        { 16,  16 }, // reset
        { 16,  16 }, // right
        { 16,  16 }, // down
        { 16,  16 }, // in
        { 16,  16 }, // out
        { 14, 164 }, // zoom
    };

    /** The area of the zoom knob, in x, y order */
    private static final Dimension ZOOM_KNOB_DIM = new Dimension( 17, 9 );

    /** The minimum coordinate of the zoom control (upper left corner)*/
    private static final Point ZOOM_FLOOR = new Point( 20, 87 );

    /** The Y pixel increment for each level of the zoom control */
    private static final int ZOOM_INC = 8;

    /** The total number of increments available to the zoom control */
    private static final int ZOOM_LEVELS = 20;

    /** The control image's width */
    // note: this is minus the shadow effect which adds 2 pixels to the actual image size */
    private static final int WIDTH = 57;

    /** The control image's height */
    // note: this is minus the shadow effect which adds 2 pixels to the actual image size */
    private static final int HEIGHT = 270;

    /** The shadow dimension ( in both x and y ) */
    private static final int SHADOW = 2;

    /////////////////////////////////////////////////////////////////////////////////

    /** The index of the upper left corner x coordinate in the bounds arrays */
    private static final int ULX = 0;

    /** The index of the upper left corner y coordinate in the bounds arrays */
    private static final int ULY = 1;

    /** The index of the lower right corner x coordinate in the bounds arrays */
    private static final int LRX = 2;

    /** The index of the lower right corner y coordinate in the bounds arrays */
    private static final int LRY = 3;

    /** The bounding area of the control */
    private int[] cntl_bounds;

    /** The bounding areas of each control function */
    private int[][] func_bounds;

    /** The offset of the navigation control from within it's parent. The
     *  default presumes that the nav control is positioned in the absolute
     *  upper left corner of it parent container i.e. x=0, y=0 */
    private int offsetX = 0;
    private int offsetY = 0;

    /** integral zoom levels */
    private int zoomLevel_max;
    private int zoomLevel_min;

    /** Event listeners */
    private EventListenerList listenerList = new EventListenerList( );

    /** The control's image */
    private Image image;

    /** The zoom knob image */
    private Image zoom_knob_image;

    /** The control's parent component */
    private Component parent;

    /** Index finger cursor, used for selecting 'buttons' */
    private Cursor handCursor;

    /** Open hand cursor, used to indicate that the mouse is in position
    *  over the zoom knob. */
    private Cursor openHandCursor;

    /** Closed hand cursor, used when dragging the zoom knob */
    private Cursor closedHandCursor;

    /** Flag indicating that the zoom knob is being adjusted */
    private boolean zoomIsAdjusting;

    /** The current zoom level */
    private int zoomLevel;

    /** The current zoom knob position */
    private Point zoomPoint;

    /** The zoom knob position while adjusting */
    private Point adjustingZoomPoint;

    /** The minimum zoom knob position */
    private Point zoomPointMin;

    /** The maximum zoom knob position */
    private Point zoomPointMax;

    /** The control function that is currently under the mouse point */
    private int currentFunction = -1;

    /** The function under the last mouse press */
    private int lastPressFunction = -1;

    /** The position of the mouse at the last event */
    private Point mousePoint;

    /** Default Constructor */
    public PanZoomControl( Component parent ) {
        this( parent, 0, 0 );
    }

    /** Constructor */
    public PanZoomControl( Component parent, Point p ) {
        this( parent, p.x, p.y );
    }

    /** Constructor */
    public PanZoomControl( Component parent, int offsetX, int offsetY ) {

        this.parent = parent;

        //parent.addMouseListener( this );
        //parent.addMouseMotionListener( this );

        zoomLevel_min = 0;
        zoomLevel_max = ZOOM_LEVELS - 1;

        zoomPoint = new Point( );
        zoomPointMin = new Point( );
        zoomPointMax = new Point( );
        adjustingZoomPoint = new Point( );

        cntl_bounds = new int[4];

        int num_controls = AREA.length;
        func_bounds = new int[num_controls][];
        for ( int i = 0; i < num_controls; i++ ) {
            func_bounds[i] = new int[4];
        }
        setLocation( offsetX, offsetY );

        Toolkit toolkit = Toolkit.getDefaultToolkit( );
        
        FileLoader loader = new FileLoader();
        
        Object[] file = loader.getFileURL(DEFAULT_CONTROL_IMAGE);       
        
        image = toolkit.createImage((URL)file[0]);

        file = loader.getFileURL(DEFAULT_ZOOM_KNOB_IMAGE);  
        zoom_knob_image = toolkit.createImage((URL)file[0]);

        handCursor = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );

        file = loader.getFileURL(DEFAULT_OPEN_HAND_CURSOR_IMAGE);  
        Image img = toolkit.createImage((URL)file[0] );
        openHandCursor = toolkit.createCustomCursor( img, new Point( 6, 4 ), null );

        file = loader.getFileURL(DEFAULT_CLOSED_HAND_CURSOR_IMAGE);  
        img = toolkit.createImage((URL)file[0]);
        closedHandCursor = toolkit.createCustomCursor( img, new Point( 7, 7 ), null );

    }

    /**
     * Set the location of the control within it's parent Container. This
     * establishes the bounding areas within the control for each individual
     * function to identify itself from mouse events on the parent.
     *
     * @param x The x offset of this control within it's parent
     * @param y The y offset of this control within it's parent
     */
    public void setLocation( int x, int y ) {
        offsetX = x;
        offsetY = y;

        cntl_bounds[ULX] = offsetX;
        cntl_bounds[LRX] = offsetX + WIDTH - 1;

        cntl_bounds[ULY] = offsetY;
        cntl_bounds[LRY] = offsetY + HEIGHT - 1;

        int num_controls = AREA.length;
        for ( int i = 0; i < num_controls; i++ ) {

            int ulx = offsetX + UPPER_LEFT_CORNER[i][0];
            func_bounds[i][ULX] = ulx;

            int uly = offsetY + UPPER_LEFT_CORNER[i][1];
            func_bounds[i][ULY] = uly;

            func_bounds[i][LRX] = ulx + AREA[i][0];

            func_bounds[i][LRY] = uly + AREA[i][1];
        }
        zoomPoint.x = offsetX + ZOOM_FLOOR.x;
        zoomPoint.y = offsetY + ZOOM_FLOOR.y;

        zoomPointMin.setLocation( zoomPoint );

        zoomPointMax.setLocation( zoomPoint );
        zoomPointMax.translate( 0, ( zoomLevel_max * ZOOM_INC ) );
    }

    /**
     * Set the location of the control within it's parent Container. This
     * establishes the bounding areas within the control for each individual
     * function to identify itself from mouse events on the parent.
     *
     * @param p The offset of this control within it's parent
     */
    public void setLocation( Point p ) {
        setLocation( p.x, p.y );
    }

    /**
     * Draw the control image with the argument Graphics object
     *
     * @param g The graphics object to use to draw
     */
    public void paintComponent( Graphics g ) {
        g.drawImage( image, offsetX, offsetY, this );
        int zoom_knob_x;
        int zoom_knob_y;
        if ( zoomIsAdjusting ) {
            zoom_knob_x = adjustingZoomPoint.x;
            zoom_knob_y = adjustingZoomPoint.y;
        } else {
            zoom_knob_x = zoomPoint.x;
            zoom_knob_y = zoomPoint.y;
        }
        g.drawImage( zoom_knob_image, zoom_knob_x, zoom_knob_y, this );
    }

    //---------------------------------------------------------
    // Method defined by ImageObserver
    //---------------------------------------------------------

    /**
     * Force a repaint when the image becomes available.
     */
    public boolean imageUpdate(
        Image img,
        int infoflags,
        int x,
        int y,
        int width,
        int height ) {

        if ( ( infoflags & ImageObserver.ALLBITS ) != 0 ) {
            parent.repaint( offsetX, offsetY, ( WIDTH + SHADOW ), ( HEIGHT + SHADOW ) );
        }
        return( true );
    }

    //---------------------------------------------------------
    // Methods defined by MouseMotionListener
    //---------------------------------------------------------

    /**
     * Handle the dragging of the zoom knob
     */
    public void mouseDragged( MouseEvent me ) {
        if ( zoomIsAdjusting ) {

            int mouseY = me.getY( );

            if ( mouseY < zoomPointMin.y ) {
                mouseY = zoomPointMin.y;

            } else if ( mouseY > zoomPointMax.y ) {
                mouseY = zoomPointMax.y;
            }
            if ( adjustingZoomPoint.y != mouseY ) {
                adjustingZoomPoint.y = mouseY;
                parent.repaint( offsetX, offsetY, ( WIDTH + SHADOW ), ( HEIGHT + SHADOW ) );
            }
        }
    }

    /**
     * Configure the cursor per the position on the control
     */
    public void mouseMoved( MouseEvent me ) {

        mousePoint = me.getPoint( );
        int function = getFunction( mousePoint );

        if ( ( function != currentFunction ) ) {
            parent.setCursor( getCursor( function ) );
            currentFunction = function;
        }
    }

    //---------------------------------------------------------
    // Methods defined by MouseListener
    //---------------------------------------------------------

    public void mouseClicked( MouseEvent me ) {
    }

    /**
     * Configure the cursor per the position on the control
     */
    public void mouseEntered( MouseEvent me ) {
        if ( !zoomIsAdjusting ) {
            mousePoint = me.getPoint( );
            currentFunction = getFunction( mousePoint );
            parent.setCursor( getCursor( currentFunction ) );
        }
    }

    /**
     * Reset function state, unless a zoom drag is active
     */
    public void mouseExited( MouseEvent me ) {
        if ( !zoomIsAdjusting ) {
            currentFunction = -1;
        }
    }

    /**
     * Select a function on the control, process zoom change if selected.
     */
    public void mousePressed( MouseEvent me ) {

        mousePoint = me.getPoint( );
        lastPressFunction = getFunction( mousePoint );

        if ( lastPressFunction == ZOOM_CHANGE ) {
            Rectangle zoom_knob_rect = new Rectangle( zoomPoint, ZOOM_KNOB_DIM );
            if ( zoom_knob_rect.contains( mousePoint ) ) {
                // the user has grabbed the zoom knob
                zoomIsAdjusting = true;
                adjustingZoomPoint.setLocation( zoomPoint );
                parent.setCursor( closedHandCursor );

            } else {
                // the user has selected a point on the zoom scale
                int level = ( mousePoint.y - offsetY - ZOOM_FLOOR.y ) / ZOOM_INC;
                if ( level < zoomLevel_min ) {
                    level = zoomLevel_min;
                }
                if ( level > zoomLevel_max ) {
                    level = zoomLevel_max;
                }
                if ( level != zoomLevel ) {
                    zoomLevel = level;
                    zoomPoint.x = offsetX + ZOOM_FLOOR.x;
                    zoomPoint.y = offsetY + ZOOM_FLOOR.y + ( zoomLevel * ZOOM_INC );
                    fireChangeEvent( );
                    parent.repaint( offsetX, offsetY, ( WIDTH + SHADOW ), ( HEIGHT + SHADOW ) );
                }
            }
        }
    }

    /**
     * Process the selected function, fire events to listeners as necessary
     */
    public void mouseReleased( MouseEvent me ) {

        mousePoint = me.getPoint( );
        int function = getFunction( mousePoint );

        if ( function != -1 ) {
            parent.setCursor( getCursor( function ) );
        }
        if ( zoomIsAdjusting ) {
            // a drag of the zoom knob has completed, recalculate
            zoomIsAdjusting = false;
            int level = ( mousePoint.y - offsetY - ZOOM_FLOOR.y ) / ZOOM_INC;
            if ( level < zoomLevel_min ) {
                level = zoomLevel_min;
            }
            if ( level > zoomLevel_max ) {
                level = zoomLevel_max;
            }
            if ( level != zoomLevel ) {
                zoomLevel = level;
                zoomPoint.x = offsetX + ZOOM_FLOOR.x;
                zoomPoint.y = offsetY + ZOOM_FLOOR.y + ( zoomLevel * ZOOM_INC );
                fireChangeEvent( );
            }
        } else {
            if ( ( function != -1 ) && ( function == lastPressFunction ) ) {
                if ( function == ZOOM_IN ) {
                    if ( zoomLevel > zoomLevel_min ) {
                        zoomLevel--;
                        zoomPoint.x = offsetX + ZOOM_FLOOR.x;
                        zoomPoint.y = offsetY + ZOOM_FLOOR.y + ( zoomLevel * ZOOM_INC );
                        parent.repaint( offsetX, offsetY, ( WIDTH + SHADOW ), ( HEIGHT + SHADOW ) );
                        fireChangeEvent( );
                    }
                } else if ( function == ZOOM_OUT ) {
                    if ( zoomLevel < zoomLevel_max ) {
                        zoomLevel++;
                        zoomPoint.x = offsetX + ZOOM_FLOOR.x;
                        zoomPoint.y = offsetY + ZOOM_FLOOR.y + ( zoomLevel * ZOOM_INC );
                        parent.repaint( offsetX, offsetY, ( WIDTH + SHADOW ), ( HEIGHT + SHADOW ) );
                        fireChangeEvent( );
                    }
                } else if ( function == ZOOM_CHANGE ) {
                    // zoom change is handled in mousePressed
                } else {
                    // all other 'button pushes' are actions
                    fireActionEvent( COMMAND[function] );
                }
            }
        }
    }

    //---------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------

    /**
     * Return the maximum value of the zoom control
     *
     * @return The maximum value
     *
     */
    public int getMaximum( ) {
        return( zoomLevel_max );
    }

    /**
     * Return the minimum value of the zoom control
     *
     * @return The minimum value
     *
     */
    public int getMinimum( ) {
        return( zoomLevel_min );
    }

    /**
     * Return the current value of the zoom control
     *
     * @return The current value of the zoom control
     */
    public int getValue( ) {
        return( zoomLevel );
    }

    /**
     * Set the current value of the zoom control
     *
     * @param value The current value of the zoom control
     */
    public void setValue( int value ) {
        if ( value > zoomLevel_max ) {
            zoomLevel = zoomLevel_max;
        } else if ( value < zoomLevel_min ) {
            zoomLevel = zoomLevel_min;
        } else {
            zoomLevel = value;
        }
        zoomPoint.x = offsetX + ZOOM_FLOOR.x;
        zoomPoint.y = offsetY + ZOOM_FLOOR.y + ( zoomLevel * ZOOM_INC );
    }

    /**
     * Add a listener for <code>ChangeEvent</code>s
     *
     * @param l The listener
     */
    public void addChangeListener( final ChangeListener l ) {
        listenerList.add( ChangeListener.class, l );
    }

    /**
     * Remove a listener for <code>ChangeEvent</code>s
     *
     * @param l The listener
     */
    public void removeChangeListener( final ChangeListener l ) {
        listenerList.remove( ChangeListener.class, l );
    }

    /**
     * Send a <code>ChangeEvent</code> to all registered listeners
     */
    protected void fireChangeEvent( ) {
        // Guaranteed to return a non-null array
        final Object[] listeners = listenerList.getListenerList( );
        // Process the listeners last to first, notifying
        // those that are interested in this event
        ChangeEvent ce = null;
        for ( int i = listeners.length - 2; i >= 0; i-=2 ) {
            if ( listeners[i] == ChangeListener.class ) {
                // Lazily create the event:
                if ( ce == null ) {
                    ce = new ChangeEvent( this );
                }
                ((ChangeListener)listeners[i+1]).stateChanged( ce );
            }
        }
    }

    /**
     * Add a listener for <code>ActionEvent</code>s
     *
     * @param l The listener
     */
    public void addActionListener( final ActionListener l ) {
        listenerList.add( ActionListener.class, l );
    }

    /**
     * Remove a listener for <code>ActionEvent</code>s
     *
     * @param l The listener
     */
    public void removeActionListener( final ActionListener l ) {
        listenerList.remove( ActionListener.class, l );
    }

    /**
     * Send an <code>ActionEvent</code> to all registered listeners
     */
    protected void fireActionEvent( String command ) {
        // Guaranteed to return a non-null array
        final Object[] listeners = listenerList.getListenerList( );
        // Process the listeners last to first, notifying
        // those that are interested in this event
        ActionEvent ae = null;
        for ( int i = listeners.length - 2; i >= 0; i-=2 ) {
            if ( listeners[i] == ActionListener.class ) {
                // Lazily create the event:
                if ( ae == null ) {
                    ae = new ActionEvent( this, ActionEvent.ACTION_PERFORMED, command );
                }
                ((ActionListener)listeners[i+1]).actionPerformed( ae );
            }
        }
    }

    /**
     * Return the Cursor to use for the specified function of the control
     *
     * @param function The function pointed to by the mouse
     * @return The corresponding Cursor object.
     */
    private Cursor getCursor( int function ) {

        Cursor cursor = null;

        switch( function ) {
        case -1:
            break;
        case PAN_UP:
        case PAN_LEFT:
        case RESET:
        case PAN_RIGHT:
        case PAN_DOWN:
        case ZOOM_IN:
        case ZOOM_OUT:
            cursor = handCursor;
            break;
        case ZOOM_CHANGE:
            Rectangle zoom_knob_rect = new Rectangle( zoomPoint, ZOOM_KNOB_DIM );
            if ( zoom_knob_rect.contains( mousePoint ) ) {
                // note: the case of a press within the zoom knob bounds
                // is identified and handled on the mousePressed method
                cursor = openHandCursor;
            } else {
                cursor = handCursor;
            }
            break;
        }
        return( cursor );
    }

    /**
     * Check whether the control contains the specified point.
     *
     * @param p The point
     * @return true If the point is within the control, false otherwise.
     */
    public boolean contains( Point p ) {
        int x = p.x;
        int y = p.y;

        // check to see that the point is within the control bounds
        if ( ( x < cntl_bounds[ULX] ) || ( x > cntl_bounds[LRX] ) ||
            ( y < cntl_bounds[ULY] ) || ( y > cntl_bounds[LRY] ) ) {
            return( false );

        } else {
            return( true );
        }
    }

    /**
     * Return the function that has been selected on this control. If
     * no function has been selected, return -1.
     *
     * @param p The point that has been selected
     * @return The function identifier cooresponding to the selected
     * point, or -1 if the point does not select a function.
     */
    private int getFunction( Point p ) {
        int x = p.x;
        int y = p.y;

        // check to see that the point is within the control bounds
        if ( ( x < cntl_bounds[ULX] ) || ( x > cntl_bounds[LRX] ) ||
            ( y < cntl_bounds[ULY] ) || ( y > cntl_bounds[LRY] ) ) {
            return( -1 );
        }

        // cycle through each function, checking that the point is contained
        // within it's bounds
        int num_funcs = func_bounds.length;
        for ( int i = 0; i < num_funcs; i++ ) {
            if ( ( x >= func_bounds[i][ULX] ) && ( x <= func_bounds[i][LRX] ) &&
                ( y >= func_bounds[i][ULY] ) && ( y <= func_bounds[i][LRY] ) ) {
                // note: hard coded dependency that the function identifiers are
                // the indicies of the bounds array !
                return( i );
            }
        }

        // no functions selected
        return( -1 );
    }
}
