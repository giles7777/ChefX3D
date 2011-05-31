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
package org.chefx3d.view.awt.av3d;

// External Imports
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

import javax.media.opengl.GL;

import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.Scene;
import org.j3d.aviatrix3d.rendering.ProfilingData;
import org.j3d.aviatrix3d.rendering.RenderEffectsProcessor;

/**
 * An implementation of a RenderEffectsProcessor which will capture the framebuffer
 * after a render, and put it into a BufferedImage.
 *
 * @author Christopher Shankland
 * @version $Revision: 1.12 $
 */
public class ThumbnailProcessor implements RenderEffectsProcessor {

    /** Should this processor be active */
    private boolean capture;

    /** Image to store the capture */
    private BufferedImage screenCapture;

    /** Listeners for completed captures */
    private List<ThumbnailListener> listeners;

    /**
     * Constructor - Allows a width and height to be set.
     */
    public ThumbnailProcessor() {
        capture = false;

        listeners = new ArrayList<ThumbnailListener>();
    }

    public void captureNextFrame() {
        capture = true;
        //System.out.println("Number of listeners: " + listeners.size());
    }

    /**
     * Notification that the draw has been completed, the only call remaining
     * is gl.glFlush().  After that, get the frame buffer, and put it into a
     * BufferedImage.  The bytes need to be packed into ints to convert into
     * a format that BufferedImage knows about, as well as reversing the order
     * because of where the origin is located in a BufferedImage, versus OpenGL.
     */
    public void postDraw(GL gl, ProfilingData pData, Object obj) {
//System.out.println("Post Draw called; capture == " + capture);
        if (capture) {
            int[] viewportDims = new int[4];
            gl.glGetIntegerv(GL.GL_VIEWPORT, viewportDims, 0);
            int width = viewportDims[2];
            int height = viewportDims[3];

            // Buffer for the frame
            ByteBuffer frameBuffer = ByteBuffer.allocate(width * height * 3);

            // Make sure that all the drawing is complete
            gl.glFinish();



            // Gather the framebuffer, and put it into our ByteBuffer for access
            gl.glReadBuffer(GL.GL_BACK);
            gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
            gl.glReadPixels(0, 0, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE,
                    frameBuffer);

            // Make sure we're at the beginning
            frameBuffer.rewind();

            // Get an array to pack the bytes into, which BufferedImage knows about
            // will contain all the component colors for each pixel as a single int
            int[] pixelInts = new int[width * height];

            int p = width * height * 3; // Points to first byte (red) in each row.
            int q; // Index into ByteBuffer
            int i = 0; // Index into target int[]
            int w3 = width * 3; // Number of bytes in each row

            // This is copied from the ThumbnailGenerator code, without a background
            // It is my understanding that this is some Rex black magic.  It appears
            // to pack color components together into a single value.  Also, note that
            // all the ints are made in reverse order when compared to the frame buffer's
            // ordering.
            for (int row = 0; row < height; row++) {
                p -= w3;
                q = p;

                for (int col = 0; col < width; col++) {
                    int iR = frameBuffer.get(q++);
                    int iG = frameBuffer.get(q++);
                    int iB = frameBuffer.get(q++);

                    pixelInts[i++] = 0xFF000000 | ((iR & 0x000000FF) << 16)
                            | ((iG & 0x000000FF) << 8) | (iB & 0x000000FF);
                }
            }

            // Make the BufferedImage
            screenCapture = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            screenCapture.setRGB(0, 0, width, height, pixelInts, 0, width);

            capture = false;

            // Notify listeners of the capture
            //System.out.println("Pre Capture");
            thumbnailGenerated(screenCapture);
            //System.out.println("Post capture");
        }
    }

    /**
     * Any preprocessing that needs to be done.
     */
    public void preDraw(GL arg0, Object arg1) {
        // Ignored
    }

    /**
     * Add a ThumbnailListener, duplicates are ignored
     *
     * @param listener - The ThumbnailListener to add
     */
    public void addThumbnailListener(ThumbnailListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    /**
     * Remove a ThumbnailListener, duplicates are ignored
     *
     * @param listener - The ThumbnailListener to remove
     */
    public void removeThumbnailListener(ThumbnailListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }


    /**
     * Notify listeners that a thumbnail has been generated.
     *
     * @param img - Image containing the thumbnail
     */
    private void thumbnailGenerated(BufferedImage img) {
        for (ThumbnailListener listener : listeners) {;
                listener.thumbnailGenerated(img);
        }
    }
}
