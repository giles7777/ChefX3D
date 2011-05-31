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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.TemplateEntity;
import org.chefx3d.model.EntityChildListener;
import org.chefx3d.model.EntitySelectionHelper;
import org.chefx3d.model.EntitySelectionListener;
import org.chefx3d.model.ModelListener;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.RemoveEntityCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.tool.SimpleTool;

import org.chefx3d.util.PropertyUtilities;


/**
 * An action that can be used to create a copy of a selected
 * Entity from the model.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Russell Dodds
 * @version $Revision: 1.26 $
 */
public class EntityCopyAction extends AbstractAction
    implements         
        ModelListener, 
        EntityChildListener, 
        EntitySelectionListener {


    /** Name of the property to get the action description */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.EntityCopyAction.description";

    /** Name of the property to get the action name */
    private static final String TITLE_PROP =
        "org.chefx3d.actions.awt.EntityCopyAction.title";

    /** The world model */
    private WorldModel model;
    
    /** The clipboard to remember items */
    private CopyPasteClipboard clipboard;    
    
    /** The list of all entities in the scene */
    private HashMap<Integer, Entity> entityMap;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper seletionHelper;

	/** i18n support */
    private I18nManager intl_mgr;

    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param model The world model
     */
    public EntityCopyAction(
            boolean iconOnly, 
            Icon icon, 
            WorldModel model, 
            CopyPasteClipboard clipboard) {
        
        intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(TITLE_PROP));


        this.model = model;
        this.clipboard = clipboard;
        
        this.model.addModelListener(this);

        entityMap = new HashMap<Integer, Entity>();

        seletionHelper = 
            EntitySelectionHelper.getEntitySelectionHelper();

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_C,
            KeyEvent.CTRL_MASK);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_C));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));

        setEnabled(false);
        
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
        
        ArrayList<Entity> selectedList = 
            seletionHelper.getSelectedList();
       
        List<Entity> entityToCopyList = new ArrayList<Entity>();    
        
        int len = selectedList.size();
        if (len == 1) {
            
            Entity entityToCopy = selectedList.get(0);
            int type = entityToCopy.getType();
                
            if (type == Entity.TYPE_MODEL || 
            	type == Entity.TYPE_MODEL_WITH_ZONES ||
				type == Entity.TYPE_SEGMENT) {
        
                entityToCopyList.add(entityToCopy.clone(model));
            }
			
        } else if (len > 1) {
			
			ArrayList<Entity> selected = new ArrayList<Entity>();
			selected.addAll(selectedList);
			ArrayList<SegmentEntity> segments = new ArrayList<SegmentEntity>();
			ArrayList<Entity> children = new ArrayList<Entity>();
			
			// identify if there are segments in the selection list,
			// remove any selections that are not candidates to copy
			int num = len;
			for (int i = len - 1; i >= 0; i--) {
				Entity entity = selected.get(i);
            	int type = entity.getType();
				switch (type) {
				case Entity.TYPE_MODEL:
				case Entity.TYPE_MODEL_WITH_ZONES:
					ActionUtils.getChildren(entity, children);
					break;
				case Entity.TYPE_SEGMENT:
					SegmentEntity se = (SegmentEntity)selected.remove(i);
					segments.add(se);
					break;
				default:
					selected.remove(i);
				}
			}
			int num_model = selected.size();
			if (num_model > 0) {
				for (int i = 0; i < num_model; i++) {
					Entity entity = selected.get(i);
					if (!children.contains(entity)) {
						entityToCopyList.add(entity.clone(model));
					}
				}
			}
			int num_segment = segments.size();
			if (num_segment == 1) {
				// a single segment
				SegmentEntity se = segments.get(0);
				entityToCopyList.add(se.clone(model));
				
			} else if (num_segment > 1) {
				// multi-segments
				SegmentEntity[] seg_array = new SegmentEntity[num_segment];
				int idx = 0;
				for (int i = 0; i < num_segment; i++) {
					seg_array[i] = segments.get(i);
				}
				seg_array = ActionUtils.cloneSegments(seg_array, model);
				for (int i = 0; i < num_segment; i++) {
					entityToCopyList.add(seg_array[i]);
				}
			}
        }
        // put them on the clip board
        clipboard.setEntityList(entityToCopyList);
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
		for (int i = 0; i < len; i++) {
			
			Entity entity = selectedList.get(i);
			int type = entity.getType();
			
			if (type == Entity.TYPE_MODEL || 
				type == Entity.TYPE_MODEL_WITH_ZONES ||
				type == Entity.TYPE_SEGMENT ||
				type == Entity.TYPE_TEMPLATE_CONTAINER) {
				
                int id = entity.getKitEntityID();
                if (id == -1) {                 
                    enabled = true;
                    break;                  
                } else {                    
                    enabled = false;
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

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

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
