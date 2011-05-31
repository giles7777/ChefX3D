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

// External imports
import org.j3d.aviatrix3d.*;

// Local Imports
// None

/**
 * Data holder class for everything we need to know about a lighting render
 * pass from the high quality rendering.
 *
 * @author Justin Couch
 * @version $Revision: 1.3 $
 */
class LightRenderPassDetails {

    /** The render pass we are representing */
    RenderPass renderPass;

    /** The radius of the light */
    float lightRadius;

    /** The light position in world space */
    float[] lightPosition;

    /** Is this pass currently enabled or not */
    boolean enabled;

    /** The scissor dimensions for this render pass */
    int[] scissors;

    /**
     * Create a pass details object that takes a copy of the data
     *
     * @param pass The render pass we're managing
     * @param rad The radius of the light
     * @param pos The position of the light in world space
     */
    LightRenderPassDetails(RenderPass pass, float rad, float[] pos) {
        renderPass = pass;

        lightRadius = rad;
        lightPosition = new float[3];
        lightPosition[0]  = pos[0];
        lightPosition[1]  = pos[1];
        lightPosition[2]  = pos[2];

        enabled = pass.isEnabled();
    }
}
