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
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.*;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;

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
 * @version $Revision: 1.7 $
 */
public class ResetAction extends AbstractAction
    implements ModelListener {

    /** Name of the property to get the action name */
    private static final String TITLE_PROP =
        "org.chefx3d.actions.awt.ResetAction.title";

    /** Name of the property to get the action name */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.ResetAction.description";

    /** The world model */
    private WorldModel model;

    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param model The world model
     */
    public ResetAction(boolean iconOnly, Icon icon, WorldModel model) {

        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if(!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(TITLE_PROP));

        this.model = model;
        this.model.addModelListener(this);

        //KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        //putValue(ACCELERATOR_KEY, acc_key);
        //putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_D));

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
        util.resetModel(true, null);

        setEnabled(false);
    }

    //----------------------------------------------------------
    // Methods required by the ModelListener interface
    //----------------------------------------------------------

    /**
     * Ignored.
     */
    public void entityAdded(boolean local, Entity entity){
        setEnabled(true);
    }

    /**
     * An entity was removed. If the removed Entity was selected,
     * disable the copy function until a new Entity is selected.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {

        Entity[] entities = model.getModelData();
        Entity check;

        setEnabled(false);
        for (int i = 0; i < entities.length; i++) {
            check = entities[i];

            if (check != null) {
                setEnabled(true);
                break;
            }
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
