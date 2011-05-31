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

package org.chefx3d.actions.awt;

// External imports
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;
import org.chefx3d.util.ConfigManager;

/**
 * An action that can be used to select all items in the
 * active location
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Russell Dodds
 * @version $Revision: 1.9 $
 */
public class SelectAllAction extends AbstractAction
    implements         
        ModelListener, 
        EntityChildListener, 
        EntitySelectionListener,
        EntityPropertyListener {

    /** Name of the property to get the action description */
    private static final String DESCRIPTION_PROP =
        "com.yumetech.chefx3d.editor.MenuPanelConstants.editSelectAll";

    /** The world model */
    private WorldModel model;   

    /** The current scene entity */
    private SceneEntity currentScene;

    /** The currently selected location */
    private LocationEntity currentLocation;
    
    /** Currently active zone */
    private Entity activeZone;

    /** Map of locations, keyed by id */
    private HashMap<Integer, LocationEntity> locationMap;

     /** The list of commands to issue */
    private ArrayList<Command> commandList;
    
    /** Currently selected step. Not particularly safe :/ */
    private int stepSelected;
    
    /** The configuration manager */
    private ConfigManager configMgr;
    
    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param model The world model
     */
    public SelectAllAction(
            boolean iconOnly, 
            Icon icon, 
            WorldModel worldModel) {
        
        I18nManager intl_mgr = I18nManager.getManager();
        configMgr = ConfigManager.getManager();
        
        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(DESCRIPTION_PROP));


        model = worldModel;        
        model.addModelListener(this);

        KeyStroke acc_key = 
            KeyStroke.getKeyStroke(
                    KeyEvent.VK_A,
                    KeyEvent.CTRL_MASK);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_A));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));
        
        commandList = new ArrayList<Command>();
        locationMap = new HashMap<Integer, LocationEntity>();
    }

    //----------------------------------------------------------
    // Methods required by the ActionListener interface
    //----------------------------------------------------------

    /**
     * An action has been performed. Copy the selected entity.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {
        
        // gather all the items in the current location
        if (currentLocation != null) {
            
            // clear the command list
            commandList.clear(); 
            
            List<Entity> contents = currentLocation.getContents();
            int len = contents.size();
            for (int i = 0; i < len; i++) {
                Entity entity = contents.get(i);
                recursiveSelect(entity);
            }
            
            // finally stack it all together and send out
            MultiCommand cmd = 
                new MultiCommand(
                        commandList, 
                        (String)getValue(SHORT_DESCRIPTION), 
                        false);
            model.applyCommand(cmd);

        }
                
    }

    //----------------------------------------------------------
    // Methods required by the ModelListener interface
    //----------------------------------------------------------

    /**
     * An entity was added. If the removed Entity was selected,
     * disable the copy function until a new Entity is selected.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being added
     */
    public void entityAdded(boolean local, Entity entity) {
		
        if (entity instanceof SceneEntity) {
			if (currentScene != null) {
				currentScene.removeEntityChildListener(this);
				int parent = currentScene.getEntityID();
				ArrayList<Entity> loc_list = currentScene.getChildren();
				int num_children = loc_list.size();
				for (int i = 0; i < num_children; i++) {
					Entity e = loc_list.get(i);
					int child = e.getEntityID();
					childRemoved(parent, child);
				}
			}
            currentScene = (SceneEntity)entity;
			int parent = currentScene.getEntityID();
			ArrayList<Entity> loc_list = currentScene.getChildren();
			int num_children = loc_list.size();
			for (int i = 0; i < num_children; i++) {
				Entity e = loc_list.get(i);
				int child = e.getEntityID();
				childAdded(parent, child);
			}
            entity.addEntityChildListener(this);
        }
    }

    /**
     * An entity was removed. If the removed Entity was selected,
     * disable the copy function until a new Entity is selected.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed
     */
    public void entityRemoved(boolean local, Entity entity) {
		
        if (entity == currentScene) {
			currentScene.removeEntityChildListener(this);
			int parent = currentScene.getEntityID();
			ArrayList<Entity> loc_list = currentScene.getChildren();
			int num_children = loc_list.size();
			for (int i = 0; i < num_children; i++) {
				Entity e = loc_list.get(i);
				if (e instanceof LocationEntity) {
					int child = e.getEntityID();
					childRemoved(parent, child);
				}
			}
            currentScene = null;
        }
    }

    /**
     * Ignored.
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * Ignored.
     */
    public void masterChanged(boolean local, long viewID) {
        // ignore
    }

    /**
     * Ignored.
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
        Entity entity = model.getEntity(child);
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.addEntitySelectionListener(this);
            locationMap.put(child, le);
        }
    }
    
    /**
     * A child was removed.
     * 
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {
        if (locationMap.containsKey(child)) {
            LocationEntity le = locationMap.remove(child);
            le.removeEntitySelectionListener(this);
            if (le == currentLocation) {
                currentLocation = null;
            }
        }
    }

    /**
     * A child was inserted.
     * 
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public void childInsertedAt(int parent, int child, int index) {
        Entity entity = model.getEntity(child);
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.addEntitySelectionListener(this);
            locationMap.put(child, le);
        }
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
        // if it is a LocationEntity, register as a property listener
    	if (locationMap.containsKey(entityID)) {
            LocationEntity le = locationMap.get(entityID);
            if (selected) {
            	
            	if( le != currentLocation){
            		if(currentLocation != null)
            			currentLocation.removeEntityPropertyListener(this);
            		currentLocation = le;
                    currentLocation.addEntityPropertyListener(this);	
            	}
            }   
        }
    }

    /** Ignored. highlightChanged      */
    public void highlightChanged(int entityID, boolean highlighted) {
    }
    
  //----------------------------------------------------------
    // Methods defined by EntityPropertyListener
    //----------------------------------------------------------

    /** a set of properties have updated */
    public void propertiesUpdated(List<EntityProperty> properties) {
    	// ignored
    }

    /** a property was added */
    public void propertyAdded(int entityID, String propertySheet,
        String propertyName) {
    	// ignored
    }

    /** a property was removed */
    public void propertyRemoved(int entityID, String propertySheet,
        String propertyName) {
    	// ignored
    }

    /** a property was updated */
    public void propertyUpdated(int entityID, String propertySheet,
        String propertyName, boolean ongoing) {

    	if (propertyName.equals(LocationEntity.ACTIVE_ZONE_PROP)) {
    		//
            // Zone has changed?  Grab the activeLocationEntity and from there
            // grab the activeZoneID.  Use that to configure the zoneView and the
            // ZoneOrientation.
            //
            LocationEntity activeLocationEntity =
                (LocationEntity)model.getEntity(entityID);

            if (activeLocationEntity != null) {

                int zoneID = activeLocationEntity.getActiveZoneID();
                activeZone = model.getEntity(zoneID);
            }
    	}
    }

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------


    /**
     * Walk through the children of the argument entity,
     * adding listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveSelect(Entity entity) {
        
        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                
                if (!child.isSelected()) {
                	
                	if(selectable(entity, child)){
                	
                		SelectEntityCommand select = 
                			new SelectEntityCommand(model, child, true);
                		commandList.add(select);
                	}
                }

                recursiveSelect(child);
            }
        }
    }
    
    /** 
     * Recursively go up the tree looking for a zone entity
     * 
     * @param entity The entity to check against
     * @return The found entity, or null if not found
     */
    private Entity parentZone(Entity entity) {
        
        if (entity == null || entity instanceof ZoneEntity) {
            return entity;
        } else {
            Entity parent = model.getEntity(entity.getParentEntityID());
            return parentZone(parent);
        }
            
    }
    
    public void setStepSelected(int step){
    	stepSelected = step;
    }
    
    /**
     * 
     * @param parent
     * @param child
     * @return
     */
    public boolean selectable(Entity parent, Entity child){
    	
    	int childType = child.getType();
    	String childCategory = child.getCategory();
    	
    	// never select the vertices
    	if (childType == Entity.TYPE_VERTEX) {
    	    return false;
    	}
    	    	
        // see if the editor is visible
        String zoneOnlyKey = 
            "step" + stepSelected + ".select.zoneOnly";
        Boolean zoneOnly = 
            Boolean.valueOf(configMgr.getProperty(zoneOnlyKey));
        
        // see if the editor is visible
        String categoriesKey = 
            "step" + stepSelected + ".select.categories";
        String categories = configMgr.getProperty(categoriesKey);
        String[] categoriesList = categories.split(",");
        
        if (zoneOnly) {
            
            // look for the parent zone
            parent = parentZone(child);
            if (parent == activeZone) {
                // select just the items in the active zone               
                for (int i = 0; i < categoriesList.length; i++) {
                    String category = categoriesList[i].trim();
                    if (category.equals(childCategory)) {
                        return true;
                    }
                }
            }
            
        } else {
            
            // select just the items in the active zone               
            for (int i = 0; i < categoriesList.length; i++) {
                String category = categoriesList[i].trim();
                if (category.equals(childCategory)) {
                    return true;
                }
            }
             
        }
        
        return false;
    	
    }
    
}
