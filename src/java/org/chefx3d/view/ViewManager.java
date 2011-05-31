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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

// Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * The manager of all views. Used to communicate to all views.
 *
 * @author Alan Hudson
 * @version $Revision: 1.25 $
 */
public class ViewManager {
    /** The singleton manager */
    private static ViewManager manager;

    /** The world model */
    private WorldModel model;

    /** The list of managed views */
    private ArrayList<View> views;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** Utility class to construct entities from tools */
    private EntityBuilder entityBuilder;


    /**
     * Private Constructor.
     */
    private ViewManager() {       
        views = new ArrayList<View>();
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the ViewManager.
     *
     * @return The singleton view manager
     */
    public static ViewManager getViewManager() {
        if (manager == null) {
            manager = new ViewManager();
        }

        return manager;
    }

    /**
     * Sets the WorldModel to use as the backing ot he views
     * 
     * @param model
     */
    public void setWorldModel(WorldModel model) {
        this.model = model;
    }
    
    /**
     * Add a view to be managed. Duplicates will be ignored.
     *
     * @param view The view to add
     */
    public void addView(View view) {
        if (!views.contains(view))
            views.add(view);
    }

    /**
     * Remove a view from management. If the view is not managed it will be
     * ignored.
     *
     * @param view The view to remove.
     */
    public void removeView(View view) {
        views.remove(view);
    }

    /**
     * Clear the WorldModel and Views
     */
    public void clear() {
        
        model = null;
        views.clear();
        
    }
    
    /**
     * System is closing; shutdown threads as necessary
     */
    public void shutdown(){
    	for (Iterator<View> list = views.iterator(); list.hasNext();) {
            View view = list.next();
            view.shutdown();
        }
    }
    
    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        if (tool != null) {

            int type = tool.getToolType();
    
            if (type == Entity.TYPE_WORLD) {
    
                System.out.println("*** Adding World!");
    
                // YES by default
                int answer = JOptionPane.YES_OPTION;
    
                Entity[] entities = model.getModelData();
    
                for (int i = 0; i < entities.length; i++) {
    
                    if ((entities[i] != null) &&
                        (entities[i].getType() == Entity.TYPE_WORLD) &&
                        (entities.length > 1)) {
    
                        answer = JOptionPane.showConfirmDialog(null,
                            "Are you sure you want to change locations?\n" +
                            "This will clear the current model and you cannot undo this command.",
                            "Select Location Action",
                            JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                }
    
                // If still YES then clear model
                if (answer == JOptionPane.YES_OPTION) {
                    ClearModelCommand clearCmd = new ClearModelCommand(model);
                    clearCmd.setErrorReporter(errorReporter);
                    model.applyCommand(clearCmd);
    
                    int entityID = model.issueEntityID();
    
                    EntityBuilder builder = getEntityBuilder();
    
                    Entity newEntity =
                        builder.createEntity(model, entityID, new double[3],
                                new float[] {0,1,0,0}, (SimpleTool)tool);
    
                    AddEntityCommand cmd = new AddEntityCommand(model, newEntity);
                    model.applyCommand(cmd);
                        
                    // send the selecting command
                    SelectEntityCommand cmdSelect = 
                        new SelectEntityCommand(model, newEntity, true);
                    model.applyCommand(cmdSelect);
                   
                    // cleanup the history
                    model.clearHistory();
                }
            }
        }
        
        for (Iterator<View> list = views.iterator(); list.hasNext();) {
            View view = list.next();
            view.setTool(tool);
        }
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
        //System.out.println("ViewManager.enableAssociateMode()");
        for (Iterator<View> list = views.iterator(); list.hasNext();) {
            View view = list.next();
            view.enableAssociateMode(validTools, propertyGroup, propertyName);
        }
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
        //System.out.println("ViewManager.disableAssociateMode()");
        for (Iterator<View> list = views.iterator(); list.hasNext();) {
            View view = list.next();
            view.disableAssociateMode();
        }
    }

    /**
     * Set the helper display mode on all views.
     *
     * @param mode The helper mode
     */
    public void setHelperDisplayMode(int mode) {
        for (Iterator<View> list = views.iterator(); list.hasNext();) {
            View view = list.next();
            view.setHelperDisplayMode(mode);
        }
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

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {
        
        if (entityBuilder == null) {
            entityBuilder = DefaultEntityBuilder.getEntityBuilder();
        }

        return entityBuilder;
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        this.entityBuilder = entityBuilder;
    }

}