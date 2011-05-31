/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2008
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
import java.util.List;
import java.util.StringTokenizer;

import org.j3d.aviatrix3d.pipeline.graphics.SurfaceInfo;

// Internal imports
// None

/**
 * Checks to see if the shader functionality is available in the
 * hardware or not.
 *
 * @author Justin Couch
 * @version $Revision: 1.3 $
 */
class ShaderUtils {

    /** Previously checked for shader availability? */
    static private boolean checkShaderAvailability;

    /**
     * This value is set to true if the shader is available
     * in the user's hardware
     */
    static private boolean shaderAvailable;

    /**
     * Static construction
     */
    static {
        checkShaderAvailability = true;
        shaderAvailable = false;
    }

    /**
     * Checks to see if the shader availability was previously
     * checked or not.
     *
     * @return True if shader was previously checked or else false
     */
    static boolean isShaderAvailabilityChecked() {
        return checkShaderAvailability;
    }

    /**
     * Checks to see if the shader is available or not
     *
     * @param info The information object containing surface details
     * @return True if shader is available or else false
     */
    static boolean checkForShaderAvailability(SurfaceInfo info) {

        if(!checkShaderAvailability)
            return shaderAvailable;

        checkShaderAvailability = false;

        String gl_vendor = info.getVendorString();
        String gl_renderer = info.getDriverInfo();

        gl_vendor = gl_vendor.toLowerCase();

        // Intel cards are broken, so don't bother trying to get shaders
        // to run on them in any form.
        if(gl_vendor.indexOf("intel") != -1) {
            shaderAvailable = false;
            return false;
        }

        // Only enable shaders on cards that are GL 1.5 and support the ARB
        // extensions, or that are greater than 1.x and are not in the dodgy
        // list.
        String[] invalid_nvidia_models = {
             "1500M",
             "2100", "2150", "3000", "4000", "4800", "5300", "5500",
             "6100", "6150", "6200", "6400", "6600", "6700", "6800",
        };

        String[] invalid_ati_models = {
            "X300", "X550", "X600", "X800", "X1050", "X1600"
        };

        if(info.getGLMajorVersion() == 1) {
            shaderAvailable = false;
            return false;
        }

        String[] check_models = {};

        if(gl_vendor.indexOf("nvidia") != -1)
            check_models = invalid_nvidia_models;
        else if(gl_vendor.indexOf("ati") != -1)
            check_models = invalid_ati_models;

        for(int i = 0; i < check_models.length; i++) {
            if(gl_renderer.indexOf(check_models[i]) != -1) {
                shaderAvailable = false;
                return false;
            }
        }

        // Made it through all the checks,
        shaderAvailable = true;

        return shaderAvailable;
    }
}
