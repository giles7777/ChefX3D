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

// External Imports
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Default implementation of some of the common implementation need to
 * implementant a tool renderer for
 * <p>
 *
 * Entity-specific renderers extend this class to provide a concrete
 * implementation. This class takes care of drawing a selection box
 * around the entity if the specific renderers don't need anything other
 * than that.
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public abstract class AbstractToolRenderer implements ToolRenderer {

    /** The default width of the renderer in pixels */
    protected static final int DEFAULT_WIDTH = 32;

    /** The default height of the renderer in pixels */
    protected static final int DEFAULT_HEIGHT = 32;

    /** Pixel offset from the outside of the image to draw the selection box */
    protected static final int SELECTION_OFFSET = 3;

    /** Line thickness when we draw the selection highlight */
    protected static final int SELECTION_LINE_THICKNESS = 1;

    /** Colour used for drawing the selected segment line */
    protected static final Color SELECTION_COLOR = Color.RED;

    /** Stroke representing the selected line drawing properties */
    protected static final BasicStroke SELECTION_STROKE =
        new BasicStroke(SELECTION_LINE_THICKNESS,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND);

    /** The current width of the renderer */
    protected int width;

    /** The current height of the renderer */
    protected int height;

    /** Common error reporter for use by all */
    protected ErrorReporter errorReporter;

    /**
     * Construct a default implementation of the tool renderer. Makes sure
     * to initialize the error reporter and uses a default width and height
     * of 32.
     */
    protected AbstractToolRenderer() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Construct a default implementation of the tool renderer width a
     * pre-defined width and height. Makes sure to initialize the error
     * reporter.
     *
     * @param width The width of the tool in pixels
     * @param height The height of the tool in pixels
     */
    protected AbstractToolRenderer(int width, int height) {
        this.width = width;
        this.height = height;

        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    //----------------------------------------------------------
    // Methods required by ToolRenderer
    //----------------------------------------------------------

    /**
     * Draw the entity to the screen now. This method does a collection of
     * wrapping and colour management for selection handling and so on. It
     * is recommended that extended classes do not implement this method.
     * Derived classes will then have the local method drawEntity() called
     * at the appropriate time.
     *
     * @param g2d The graphics object used to draw
     * @param eWrapper The wrapped entity to draw
     */
    public void drawSelection(Graphics2D g2d, EntityWrapper eWrapper) {

    	// get the current color so we can save it back when we are done
        Color origColor = g2d.getColor();

        // display a selection box if selected
        g2d.setColor(SELECTION_COLOR);
        g2d.setStroke(SELECTION_STROKE);
        
        width = getWidth();
        height = getHeight();
        
        g2d.drawRect(-width / 2 - SELECTION_OFFSET,
                     -height / 2 - SELECTION_OFFSET,
                     width + SELECTION_OFFSET * 2,
                     height + SELECTION_OFFSET * 2);

        g2d.setColor(origColor);
    }
    
    
    /**
     * Get the width of the tool to be drawn in pixels.
     *
     * @return int A non-negative size in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the tool to be drawn in pixels.
     *
     * @return int A non-negative size in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set an error reporter instance for any rendering/parsing information.
     * Setting a null value returns back to the default reporter.
     *
     * @param reporter The reporter instance to add
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
}
