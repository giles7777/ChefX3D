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
import java.awt.image.BufferedImage;

/**
 * A listener for thumbnail generation.
 *
 * @author Christopher Shankland
 *
 */
public interface ThumbnailListener {

    /**
     * The thumbnail has been generated, and is contained in the Image argument.
     *
     * @param thumbnail - The thumbnail that was generated
     */
    public void thumbnailGenerated(BufferedImage thumbnail);
}
