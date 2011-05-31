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
import java.awt.Graphics2D;


// Local imports

/**
 * Renders the entity's icon to the screen.
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public class RectangleToolRenderer extends AbstractToolRenderer {

    /** The default width of the renderer in pixels */
    protected static final int DEFAULT_WIDTH = 40;

    /** The default height of the renderer in pixels */
    protected static final int DEFAULT_HEIGHT = 30;

    /**
     * Contruct a default instance of this renderer.
     */
    public RectangleToolRenderer() {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    //----------------------------------------------------------
    // Methods required by ToolRenderer
    //----------------------------------------------------------

    /**
     * Draw the entity icon to the screen
     *
     * @param g2d The graphics object used to draw
     * @param eWrapper The wrapped entity to draw
     */
    public void draw(Graphics2D g2d, EntityWrapper eWrapper) {
        g2d.drawRect(-width / 2, -height / 2, width / 2, height / 2);
    }
    
  
}
