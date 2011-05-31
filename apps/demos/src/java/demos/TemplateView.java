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

package demos;

// External Imports
import javax.swing.*;
import org.chefx3d.model.*;
import org.chefx3d.view.*;

import java.awt.BorderLayout;
import java.util.List;

import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

// Internal Imports
// None yet

/**
 * A template view that can be copied to other applications and used as the base
 * for developing your own custom view.
 *
 * @author Justin Couch
 * @version $Revision: 1.6 $
 */
public class TemplateView extends JPanel
    implements View, ModelListener {

    /** The ViewManager */
    private ViewManager vmanager;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * View the WorldModel in a tree structure
     *
     * @param model The WorldModel that the tree is representing
     */
    public TemplateView(WorldModel model) {
        super(new BorderLayout());

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        ViewManager vmanager = ViewManager.getViewManager();
        vmanager.addView(this);

        model.addModelListener(this);
    }

    //----------------------------------------------------------
    // Methods defined by View
    //----------------------------------------------------------

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        // ignore
    }

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
            String propertyName) {
        
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
    }


    /**
     * Get the viewID. This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID() {
        return -1;
    }

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode) {
        // ignore
    }

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode) {
        // ignore
    }

    /**
     * Return the property data in the required format
     */
    public Object getComponent() {
        return this;
    }

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {
        return null;
        // ignore
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        // ignore
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    //----------------------------------------------------------
    // Methods defined by ModelListener
    //----------------------------------------------------------

    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The unique entityID assigned by the view
     */
    public void entityAdded(boolean local, Entity entity) {
    }

    /**
     * An entity was removed.
     *
     * @param local Was this action initiated from the local UI
     * @param entityID The id
     */
    public void entityRemoved(boolean local, Entity entity) {
    }

    /**
     * The master view has changed.
     *
     * @param view The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
        // ignore
    }

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
        // ignore
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
}
