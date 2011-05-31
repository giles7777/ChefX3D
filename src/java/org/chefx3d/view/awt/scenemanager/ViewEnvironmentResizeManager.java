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

package org.chefx3d.view.awt.scenemanager;

import java.util.ArrayList;
import org.j3d.aviatrix3d.pipeline.graphics.ViewportResizeManager;

/**
 * Convenience class for managing the resizing of the viewports and
 * ViewEnvironments based on listener feedback from the surface.
 * <p>
 *
 * This class deals only with fullscreen resizing capabilities. All viewports
 * registered for management will resize to the full screen size handed to us
 * by the listener.
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.9 $
 */
public class ViewEnvironmentResizeManager extends ViewportResizeManager {

    /** the width of the resize, in pixels */
    private int newPixelWidth;

    /** the height of the resize, in pixels */
    private int newPixelHeight;

    /** The list of listeners */
    private ArrayList<ResizeListener> listeners;

    /**
     * Constructor -
     * create a new instance of the manager now.
     * Initialize oldPixelWidth and Height with bogus values of -1.
     */
    public ViewEnvironmentResizeManager(){
        super();
        listeners = new ArrayList<ResizeListener>();
    }


    /**
     * Clock the updates that should be sent now. This should only be called
     * during the application update cycle callbacks. Ideally this should be
     * called as the first thing in updateSceneGraph() method so that anything
     * else that relies on view frustum projection will have the latest
     * information.
     */
    public void sendResizeUpdates()
    {
        super.sendResizeUpdates();

        //
        // notify the listeners of a change
        //
        ResizeListener rl;
        for( int i = 0; i < listeners.size(); i++){
            rl = listeners.get(i);
            rl.sizeChanged( newPixelWidth, newPixelHeight);
        }
    }



    /**
     * Add a ResizeListener to be managed.
     *
     * Duplicate requests are ignored, as are
     * null values.
     *
     * @param resizeListener The resizeListner instance to use
     */
    public void addResizeListener(ResizeListener resizeListener)
    {
        if((resizeListener == null) || listeners.contains(resizeListener))
            return;

        listeners.add(resizeListener);
    }


    /**
     * Remove a ResizeListener that is being managed.
     * If the resizeListener is not
     * currently registered, the request is silently ignored.
     *
     * @param resizeListener The ResizeListener instance to remove
     */
    public void removeManagedViewEnvironment(ResizeListener resizeListener)
    {
        if(resizeListener == null)
            return;

        listeners.remove(resizeListener);
    }


    /**
     * Clear all of the current ResizeListeners from the manager.
     * Typically used when
     * you want to completely change the scene.
     */
    public void clear()
    {
        listeners.clear();
        super.clear();
    }


    //---------------------------------------------------------------
    // Methods defined by GraphicsResizeListener
    //---------------------------------------------------------------


    /**
     * Notification that the graphics output device has changed dimensions to
     * the given size. Dimensions are in pixels.
     *
     * @param x The lower left x coordinate for the view
     * @param y The lower left y coordinate for the view
     * @param width The width of the viewport in pixels
     * @param height The height of the viewport in pixels
     */
    public void graphicsDeviceResized(int x, int y, int width, int height){

        // Zeroes cause problems. Ignore it if we see that.
        if((width == 0) || (height == 0))
            return;


        super.graphicsDeviceResized(x,
                                    y,
                                    width,
                                    height);

            newPixelWidth = width;
            newPixelHeight = height;
//System.out.println(width + "\t\t\t" + height);
    }


    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------
}
