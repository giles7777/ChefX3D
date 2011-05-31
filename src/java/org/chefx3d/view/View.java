/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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
// None

// Internal Imports
import org.chefx3d.AuthoringComponent;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.util.ErrorReporter;

/**
 * A view on the model. Authoring operations can be performed on any view.
 *
 * @author Alan Hudson
 * @version $Revision: 1.21 $
 */
public interface View extends AuthoringComponent {
    
    /** Display all helpers */
    public static final int HELPER_ALL = 0;

    /** Display the selected entities helpers */
    public static final int HELPER_SELECTED = 2;

    /** Display no helpers */
    public static final int HELPER_NONE = 3;

    /** The view is the master view */
    public static final int MODE_MASTER = 0;

    /** The view is a slaved view */
    public static final int MODE_SLAVED = 1;

    /** The view is free navigated */
    public static final int MODE_FREE_NAV = 2;

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool);

    /**
     * Go into associate mode. The next mouse click will perform
     * a property update
     *
     * @param validTools A list of the valid tools. null string will be all
     *        valid. empty string will be none.
     * @param propertyGroup The grouping the property is a part of
     * @param propertyName The name of the property being associated
     */
    public void enableAssociateMode(
            String[] validTools, 
            String propertyGroup, 
            String propertyName);
    
    /**
     * System is going to exit; shut down threads as necessary.
     */
    public void shutdown();
    
    /**
     * Exit associate mode.
     */
    public void disableAssociateMode();

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode);

    /**
     * Get the viewID. This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID();

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode);

    /**
     * Get the class used to create entities from tools
     * 
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder();

    /**
     * Set the class used to create entities from tools
     * 
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder);

    /**
     * Register an error reporter with the view instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter);

}
