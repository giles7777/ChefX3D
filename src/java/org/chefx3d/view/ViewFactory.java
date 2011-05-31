/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
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

// External Imports
import java.util.Map;

// Internal Imports
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.ErrorReporter;

/**
 * Factory for creating Views
 *
 * @author Alan Hudson
 * @version $Revision: 1.12 $
 */
public interface ViewFactory {

    public static final int PICTURE_VIEW = 0;

    public static final int OPENMAP_VIEW = 1;

    public static final int TOP_X3D_VIEW = 2;

    public static final int LEFT_X3D_VIEW = 3;

    public static final int RIGHT_X3D_VIEW = 4;

    public static final int PERSPECTIVE_X3D_VIEW = 5;

    public static final int PROPERTY_VIEW = 6;

    public static final int ENTITY_TREE_VIEW = 7;

    public static final int TOP_2D_VIEW = 8;
    
    public static final int TOP_3D_VIEW = 9;

    public static final String PARAM_IMAGES_DIRECTORY = "ViewFactory.imagesDirectory";
    public static final String PARAM_INITIAL_WORLD = "ViewFactory.initialWorld";
    public static final String PARAM_PICKING_TYPE = "ViewFactory.pickingType";
    public static final String PARAM_SHOW_RULER = "ViewFactory.showRuler";
    public static final String PARAM_COMMAND_CONTROLLER = "ViewFactory.controller";
    public static final String PARAM_BACKGROUND_COLOR = "ViewFactory.background";

    /**
     * Create a view.
     *
     * @param type The type of view to create
     * @return The View
     */
    public View createView(WorldModel model, int type);

    /**
     * Create a view.
     *
     * @param type The type of view to create
     * @param params View specific parameters.
     * @return The View
     */
    public View createView(WorldModel model, int type, Map params);

    public void setErrorReporter(ErrorReporter reporter);

}
