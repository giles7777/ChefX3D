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

package org.chefx3d.model;

// External Imports
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

// Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A help class used to retrieve the current list of selected
 * or highlighted entities
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public class EntitySelectionHelper 
    implements 
        ModelListener, 
        EntityChildListener, 
        EntitySelectionListener {
    
    /** The singleton manager */
    private static EntitySelectionHelper manager;

    /** The world model */
    private WorldModel model;
    
    /** The command controller */
    private CommandController controller;

    /** The list of all entities in the scene */
    private HashMap<Integer, Entity> entityMap;

    /** The list of selected Entities */
    private ArrayList<Entity> selectedList;

    /** The list of highlighted Entities */
    private ArrayList<Entity> highlightList;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /**
     * Private Constructor.
     */
    private EntitySelectionHelper() {      
        entityMap = new HashMap<Integer, Entity>();
        selectedList = new ArrayList<Entity>();
        highlightList = new ArrayList<Entity>();
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the EntitySelectionHelper.  Must call the initialize() and
     * setErrorReporter() the first time the class is instantiated
     *
     * @return The singleton EntitySelectionHelper
     */
    public static EntitySelectionHelper getEntitySelectionHelper() {
        if (manager == null) {
            manager = new EntitySelectionHelper();
        }

        return manager;
    }

    // ---------------------------------------------------------------
    // Methods defined by ModelListener
    // ---------------------------------------------------------------

    /**
     * An entity was added.
     * 
     * @param local Was this action initiated from the local UI
     * @param entity The entity added to the view
     */
    public void entityAdded(boolean local, Entity entity) {
		recursiveAdd(entity);
    }

    /**
     * An entity was removed.
     * 
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {
		recursiveRemove(entity);
    }
    
    /**
     * User view information changed.
     * 
     * @param local Was this action initiated from the local UI
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * The master view has changed.
     * 
     * @param local Was this action initiated from the local UI
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
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

    // ---------------------------------------------------------------
    // Methods defined by EntityChildListener
    // ---------------------------------------------------------------

    /**
     * A child was added.
     * 
     * @param parent The entity which changed
     * @param child The child which was added
     */
    public void childAdded(int parent, int child) {
        Entity parentEntity = entityMap.get(parent);
        
        int index = parentEntity.getChildIndex(child);       
        Entity childEntity = parentEntity.getChildAt(index);
        
        entityAdded(true, childEntity);
    }
    
    /**
     * A child was removed.
     * 
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {
        
        Entity childEntity = entityMap.get(child);
        
        entityRemoved(true, childEntity);
    }

    /**
     * A child was inserted.
     * 
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public void childInsertedAt(int parent, int child, int index) {
        Entity parentEntity = entityMap.get(parent);
        
        Entity childEntity = parentEntity.getChildAt(index);
        
        entityAdded(true, childEntity);     
    }
    
    // ---------------------------------------------------------------
    // Methods defined by EntitySelectionListener
    // ---------------------------------------------------------------

    /**
     * An entity has been selected
     * 
     * @param entityID The entity which changed
     * @param selected Status of selecting
     */
    public void selectionChanged(int entityID, boolean selected) {
        
        Entity entity = entityMap.get(entityID);
        
        if (selected) {
            selectedList.add(entity);
        } else {
            selectedList.remove(entity);
        }
        
    }
    
    /**
     * An entity has been highlighted
     * 
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
        
        Entity entity = entityMap.get(entityID);
        
        if (highlighted) {
            highlightList.add(entity);
        } else {
            highlightList.remove(entity);
        }
     
    } 
    
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
    /**
     * Sets the WorldModel to use as the backing ot he views
     * 
     * @param model
     */
    public void initialize(WorldModel model, CommandController controller) {
        this.model = model;
        this.controller = controller;
        
        this.model.addModelListener(this);
    }
    
    /**
     * Get the list of currently selected entities
     * 
     * @return the selectedList
     */
    public ArrayList<Entity> getSelectedList() {
        return selectedList;
    }

    /**
     * Get the list of currently highlighted entities
     * 
     * @return the highlightList
     */
    public ArrayList<Entity> getHighlightList() {
        return highlightList;
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
     * Clear all the current selections
     */
    public void clearSelectedList() {
        
        ArrayList<Command> commandList = 
            new ArrayList<Command>();
        
        int len = selectedList.size();
        for (int i = 0; i < len; i++) {
            
            Entity entity = selectedList.get(i);
            
            if (entity == null)
                continue;
            
            SelectEntityCommand cmdSelect =
                new SelectEntityCommand(model, entity, false);
            commandList.add(cmdSelect);

        }
        if (commandList.size() > 0) {
        	MultiTransientCommand cmd = 
        	    new MultiTransientCommand(commandList, "Unselect All");
        	controller.execute(cmd);
		}
    }
    
    /**
     * Clear all the current highlights
     */
    public void clearHighlightList() {
        highlightList.clear();
    }

	/**
	 * Walk through the children of the argument entity,
	 * adding listeners as necessary.
	 *
	 * @param entity The entity to start with
	 */
	private void recursiveAdd(Entity entity) {
		
		entity.addEntityChildListener(this);
        entity.addEntitySelectionListener(this);

        entityMap.put(entity.getEntityID(), entity);

		if (entity.hasChildren()) {
			ArrayList<Entity> childList = entity.getChildren();
			for (int i = 0; i < childList.size(); i++) {
				Entity child = childList.get(i);
				recursiveAdd(child);
			}
		}
	}
	
	/**
	 * Walk through the children of the argument entity,
	 * removing listeners as necessary.
	 *
	 * @param entity The entity to start with
	 */
	private void recursiveRemove(Entity entity) {
		
	    if (entity == null)
	        return;
	    
        entity.removeEntityChildListener(this);
        entity.removeEntitySelectionListener(this);

		entityMap.remove(entity.getEntityID());
        selectedList.remove(entity);
        highlightList.remove(entity);
		
		if (entity.hasChildren()) {
			ArrayList<Entity> childList = entity.getChildren();
			for (int i = 0; i < childList.size(); i++) {
				Entity child = childList.get(i);
				recursiveRemove(child);
			}
		}
	}
}