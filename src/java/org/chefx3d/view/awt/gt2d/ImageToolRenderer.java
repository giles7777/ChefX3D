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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.FileLoader;

/**
 * Renders the entity's tool icon to the screen.
 *
 * @author Russell Dodds
 * @version $Revision: 1.15 $
 */
public class ImageToolRenderer extends AbstractToolRenderer {

    /** Internal debug flag to draw a dot on the center */
    private static final boolean SHOW_ENTITY_CENTER = false;

    /** The image to render */
    private Image image;

    /** Flag to say whether we have got a default size yet */
    private boolean hasValidSize;

    /**
     * Contruct a tool renderer that draws the icon image from the
     * given entity
     *
     * @param entity The entity being represented
     */
    public ImageToolRenderer(Entity entity, ViewingFrustum.Plane view) {

        // try to retrieve from the classpath           
        FileLoader fileLookup = new FileLoader();
        
        Object[] file = fileLookup.getFileURL(
        		entity.getIconURL(view.toString()));
        URL iconURL = (URL)file[0];

        ImageIcon icon = new ImageIcon(iconURL);
        hasValidSize = false;

        if (icon != null) {
            image = icon.getImage();
        }
    }

    /**
     * Contruct a tool renderer that draws the icon image from the
     * given tool
     *
     * @param tool The tool being represented
     */
    public ImageToolRenderer(Tool tool) {

        // try to retrieve from the classpath           
        FileLoader fileLookup = new FileLoader();
        
        Object[] file = fileLookup.getFileURL(tool.getIcon());
        URL iconURL = (URL)file[0];
        
        ImageIcon icon = new ImageIcon(iconURL);
        hasValidSize = false;

        if (icon != null) {
            image = icon.getImage();
        }
    }

    //----------------------------------------------------------
    // Methods required by ToolRenderer
    //----------------------------------------------------------

    /**
     * Draw the entity's graphics to the screen. Do not draw selection
     * highlights at this point in time. That will be done in the
     * {@link #drawSelection} method.
     *
     * @param g2d The graphics object used to draw
     * @param eWrapper The wrapped entity to draw
     */
    public void draw(Graphics2D g2d, EntityWrapper eWrapper) {

        if (image == null)
            return;

        Color origColor = g2d.getColor();

        // Half width and height values
        int w2 = getWidth() / 2;
        int h2 = getHeight() / 2;

        g2d.drawImage(image, -w2, -h2, null, null);

        // show center for debuging
        if (SHOW_ENTITY_CENTER) {
            g2d.setColor(Color.BLUE);
            g2d.drawRect(-1, -1, 2, 2);
            g2d.setColor(Color.WHITE);
        }

        if (eWrapper.getEntity().isHighlighted()) {
            g2d.setColor(Color.YELLOW);
            g2d.drawRect(-SELECTION_OFFSET - w2,
                         -SELECTION_OFFSET - h2,
                         (w2 + SELECTION_OFFSET) * 2,
                         (h2 + SELECTION_OFFSET) * 2);
            g2d.setColor(Color.WHITE);
        }

        g2d.setColor(origColor);
    }  
    


    /**
     * Get the width of the tool to be drawn in pixels.
     *
     * @return int A non-negative size in pixels
     */
    public int getWidth() {

//        if ((image != null) && !hasValidSize) {
        if ((image != null)) {
            width = image.getWidth(null);
            hasValidSize = true;
        }

        return width;
    }

    /**
     * Get the height of the tool to be drawn in pixels.
     *
     * @return int A non-negative size in pixels
     */
    public int getHeight() {

//        if ((image != null) && !hasValidSize) {
        if ((image != null)) {
            height = image.getHeight(null);
            hasValidSize = true;
        }

        return height;
    }
}
