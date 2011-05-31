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

package org.chefx3d.model;

// external imports
import java.util.Map;
import java.awt.Color;

// internal imports

/**
 * Defines the requirements for configuring common parameters
 * of the rendering environment
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public interface EnvironmentEntity {

    /** The color to use as a background/sky color */
    public static final String BACKGROUND_COLOR_PROP =
        "org.chefx3d.model.EnvironmentEntity.BackgroundColor";

    /** The color to use for a floor/ground color */
    public static final String GROUND_COLOR_PROP =
        "org.chefx3d.model.EnvironmentEntity.GroundColor";

    /** The normal vector for the floor/ground */
    public static final String GROUND_NORMAL_PROP =
        "org.chefx3d.model.EnvironmentEntity.GroundNormal";

    /** The color to use for any shared object appearance */
    public static final String SHARED_COLOR1_PROP =
        "org.chefx3d.model.EnvironmentEntity.SharedColor1";

    /** The color to use for any shared object appearance */
    public static final String SHARED_COLOR2_PROP =
        "org.chefx3d.model.EnvironmentEntity.SharedColor2";

    /** Default sheet name for environment properties */
    public static final String DEFAULT_ENVIRONMENT_PROPERTIES =
        "org.chefx3d.model.EnvironmentEntity.environmentProperties";

    /**
     * Get the BackgroundColor property
     *
     * @return The BackgroundColor property
     */
    public Color getBackgroundColor();

    /**
     * Set the BackgroundColor property
     *
     * @param color The new color
     */
    public void setBackgroundColor(Color color);

    /**
     * Get the GroundColor property
     *
     * @return The GroundColor property
     */
    public Color getGroundColor();

    /**
     * Set the GroundColor property
     *
     * @param color The new color
     */
    public void setGroundColor(Color color);

    /**
     * Get the SharedColor1 property
     *
     * @return The SharedColor1 property
     */
    public Color getSharedColor1();

    /**
     * Set the SharedColor1 property
     *
     * @param color The new color
     */
    public void setSharedColor1(Color color);

    /**
     * Get the SharedColor2 property
     *
     * @return The SharedColor2 property
     */
    public Color getSharedColor2();

    /**
     * Set the SharedColor2 property
     *
     * @param color The new color
     */
    public void setSharedColor2(Color color);

    /**
     * Get the GroundNormal property
     *
     * @return The GroundNormal property
     */
    public float[] getGroundNormal();

    /**
     * Set the GroundNormal property
     *
     * @param normal The new normal
     */
    public void setGroundNormal(float[] normal);

}
