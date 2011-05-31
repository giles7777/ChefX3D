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
 * Data holder class for the basic light definitions read from the
 * configuration file
 * <p>
 *
 * The default configuration file is found in
 * <code>config/view/av3d/preview_lighting.xml</code> but may be
 * overriden at the application's discretion. All units are considered
 * to be meters, and colour is RGB in [0,1] floats.
 *
 * @author Justin Couch
 * @version $Revision: 1.2 $
 */
class LightConfigData {

    enum LightType { SPOT, POINT, DIRECTIONAL, AMBIENT };

    /** the type of light that this data represents */
    LightType type;

    /** The radius of the light */
    float lightRadius;

    /** The light position in world space */
    float[] lightPosition;

    /** The colour of the light in RGB */
    float[] lightColor;

    /**
     * The attenuation factors. Defaults to 1, 0, 0 for constant,
     * linear and quadratic factors
     */
    float[] attenuation;

    /** The direction the light is showing */
    float[] direction;

    /** The cutoff angle used for spot lights */
    float angle;

    /**
     * Construct a new default isntance, initialising all the arrays
     * to be the right length, filled with zeros. Default light type
     * is POINT.
     */
    LightConfigData() {
        type = LightType.POINT;
        lightPosition = new float[3];
        lightColor = new float[3];
        attenuation = new float[3];
        direction = new float[3];

        attenuation[0] = 1.0f;
    }
}
