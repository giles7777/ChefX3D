/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view;

// External imports
import org.web3d.x3d.sai.X3DComponent;

// Internal imports
import java.util.List;

/**
 * An X3D View.
 * 
 * @author Alan Hudson
 */
public interface ViewX3D extends View {
    /**
     * Return the X3D component in use.
     * 
     * @return The component
     */
    public X3DComponent getX3DComponent();
    
    /**
     * Creates a thumbnail image out of the current view.
     * A generated image file will be saved into a local
     * hard-drive.
     * 
     * @author Sang Park
     */
    public void createThumbnailImages();
    
    /**
     * Return true if our browser is initalized or false if not.
     * @return True or false.
     * 
     * @author Sang Park
     */
    public boolean isBrowserInitalized();
    
    /**
     * Adds thumbnail listener to the view 
     * @param listener ThumbnailListener
     * 
     * @author Sang Park
     */
    public void addThumbnailGenListener(ThumbnailListener listener);

    public void shutdown();
}