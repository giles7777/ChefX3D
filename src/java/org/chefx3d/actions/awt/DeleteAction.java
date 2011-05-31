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
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.tool.SimpleTool;

/**
 * An action that can be used to delete the selected entity
 * from the model
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Russell Dodds
 * @version $Revision: 1.19 $
 */
public class DeleteAction extends AbstractAction
    implements         
        ModelListener, 
        EntityChildListener, 
        EntitySelectionListener {

    /** Name of the property to get the action name */
    private static final String DESCRIPTION_PROP =
        "com.yumetech.chefx3d.editor.MenuPanelConstants.editDelete";

    /** The world model */
    private WorldModel model;

    /** The list of all entities in the scene */
    private HashMap<Integer, Entity> entityMap;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper seletionHelper;

    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param model The world model
     */
    public DeleteAction(boolean iconOnly, Icon icon, WorldModel model) {

        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(DESCRIPTION_PROP));

        this.model = model;
        this.model.addModelListener(this);

        entityMap = new HashMap<Integer, Entity>();

        seletionHelper = 
            EntitySelectionHelper.getEntitySelectionHelper();

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_D));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));

        setEnabled(false);
    }

    //----------------------------------------------------------
    // Methods required by the ActionListener interface
    //----------------------------------------------------------

    /**
     * An action has been performed.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {
        CommandUtils util = new CommandUtils(model);
        util.removeSelectedEntity(true, null);
    }

    //----------------------------------------------------------
    // Methods required by the ModelListener interface
    //----------------------------------------------------------

    /**
     * Ignored.
     */
    public void entityAdded(boolean local, Entity entity){
        recursiveAdd(entity);
    }

    /**
     * An entity was removed. If the removed Entity was selected,
     * disable the copy function until a new Entity is selected.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {
        recursiveRemove(entity);
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
        Entity parentEntity = entityMap.get(parent);
        if (parentEntity != null) {
        	int index = parentEntity.getChildIndex(child);       
        	Entity childEntity = parentEntity.getChildAt(index);
        	recursiveAdd(childEntity);
		}
    }
    
    /**
     * A child was removed.
     * 
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {
        
        Entity childEntity = entityMap.get(child);
        if (childEntity != null) {
			recursiveRemove(childEntity);
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
        Entity parentEntity = entityMap.get(parent);
        if (parentEntity != null) {
        	Entity childEntity = parentEntity.getChildAt(index);
        	recursiveAdd(childEntity);
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
        
        ArrayList<Entity> selectedList = 
            seletionHelper.getSelectedList();
        
        boolean enabled = false;
        
        int len = selectedList.size();
        if (len > 0) {
                        
            // clone the entities
            int len1 = selectedList.size();
            for (int i = 0; i < len1; i++) {
                
                Entity entity = selectedList.get(i);
                          
                // don't enable for shadow state
                Boolean isShadow = 
                    (Boolean)entity.getProperty(entity.getParamSheetName(), 
                            Entity.SHADOW_ENTITY_FLAG);
                if (isShadow != null && isShadow == true) {
                    continue;
                }               
                
                if (entity.getType() == Entity.TYPE_MODEL ||
                	entity.getType() == Entity.TYPE_MODEL_WITH_ZONES ||
                    entity.getType() == Entity.TYPE_SEGMENT || 
                    entity.getType() == Entity.TYPE_TEMPLATE_CONTAINER) {
                    
                    enabled = true;
                    break;
                    
                }
            }
        }
        
        setEnabled(enabled);
        
    }
    
    /**
     * An entity has been highlighted
     * 
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
        // ignore
    } 

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

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
		
        entity.removeEntityChildListener(this);
        entity.removeEntitySelectionListener(this);

        entityMap.remove(entity.getEntityID());

		if (entity.hasChildren()) {
			ArrayList<Entity> childList = entity.getChildren();
			for (int i = 0; i < childList.size(); i++) {
				Entity child = childList.get(i);
				recursiveRemove(child);
			}
		}
	}
}
