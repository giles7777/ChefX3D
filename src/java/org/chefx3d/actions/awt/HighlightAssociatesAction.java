/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
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
 * @version $Revision: 1.20 $
 */
public class HighlightAssociatesAction extends AbstractAction
    implements ModelListener {

    /** Name of the property to get the action description */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.HighlightAssociatesAction.description";

    /** Name of the property to get the action name when showing */
    private static final String SHOW_TITLE_PROP =
        "org.chefx3d.actions.awt.HighlightAssociatesAction.showTitle";

    /** Name of the property to get the action name when showing */
    private static final String HIDE_TITLE_PROP =
        "org.chefx3d.actions.awt.HighlightAssociatesAction.hideTitle";

    /** The world model */
    private WorldModel model;

    /** The ID of the selected Entity in the model. A
    * value of -1 means that no Entity is selected. */
    private int entityID = -1;

    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param worldModel The world model
     */
    public HighlightAssociatesAction(boolean iconOnly, 
    		Icon icon, WorldModel worldModel) {

        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(SHOW_TITLE_PROP));

        model = worldModel;
        model.addModelListener(this);

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_H, 0);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_H));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));

        setEnabled(false);
    }

    /**
     * Create an instance of the action class.
     *
     * @param standAlone Is this standalone or in a menu
     * @param icon The icon
     * @param model The world model
     * @param entityID The entity to use
     */
    public HighlightAssociatesAction(boolean standAlone, Icon icon,
            WorldModel model, int entityID) {

        this(standAlone, icon, model);
        this.entityID = entityID;

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

//System.out.println("HighlightAssociatesAction.actionPerformed()");
//System.out.println("    selectedID: " + entityID);

        if (entityID == -1) {
            return;
        }

        Entity entity = model.getEntity(entityID);

        // the list of selections
        List<Selection> selectionList = new ArrayList<Selection>();

        //List<Selection> list = new ArrayList<Selection>(associateIDs.length + 1);

        int segmentID = -1;
        int vertexID = -1;
        if (entity instanceof SegmentableEntity) {
            // TODO: fix this to use new selection model
            //segmentID = ((SegmentableEntity)entity).getSelectedSegmentID();
            //vertexID = ((SegmentableEntity)entity).getSelectedVertexID();
        }

        // add the current entity to the list
        Selection selection =
            new Selection(entityID, segmentID, vertexID);

        selectionList.add(selection);

        // now find all the possible associations
        List<EntityProperty> properties = entity.getProperties();
        for (int i = 0; i < properties.size(); i++) {

            if (properties.get(i).propertyValue instanceof AssociateProperty) {
                AssociateProperty associate =
                    (AssociateProperty)properties.get(i).propertyValue;

                if (associate == null) {
                    continue;
                }

                Object value = associate.getValue();
                if (value == null) {
                    continue;
                }

                Integer intValue = 0;
                if (value instanceof SegmentableEntity) {
                    intValue = ((SegmentableEntity)value).getEntityID();
                } else if (value instanceof Integer) {
                    intValue = (Integer)value;
                }

                Selection select =
                    new Selection(intValue, -1, -1);
                selectionList.add(select);

            }

        }

        // TODO: we need to modify this or add a new method that allows us to send
        // highlight events for the extra items
        //SelectEntityCommand cmdSelect = 
        //    new SelectEntityCommand(model, selectionList);
        //model.applyCommand(cmdSelect);


    }

    //----------------------------------------------------------
    // Methods required by the ModelListener interface
    //----------------------------------------------------------

    /**
     * Ignored.
     */
    public void entityAdded(boolean local, Entity entity){
    }

    /**
     * An entity was removed. If the removed Entity was selected,
     * disable the copy function until a new Entity is selected.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {
        int removedID = entity.getEntityID();
        if(removedID == entityID) {
            entityID = -1;
        }
    }

    /**
     * Ignored.
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
    }

    /**
     * Ignored.
     */
    public void masterChanged(boolean local, long viewID) {
    }

    /**
     * Ignored.
     */
    public void modelReset(boolean local) {
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

}
