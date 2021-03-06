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

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;

/**
 * An action that can be used to undo the last modification to
 * the model.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Alan Hudson
 * @version $Revision: 1.10 $
 */
public class UndoAction extends AbstractAction
    implements CommandListener {

    /** Name of the property to get the action name */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.UndoAction.description";

    /** Name of the property to get the action name */
    private static final String TITLE_PROP =
        "org.chefx3d.actions.awt.UndoAction.title";

    /** The Command Controller */
    private CommandController controller;

    /** Is this standalone or in a menu */
    private boolean iconOnly;
    
    /** do we ignore command updates to enable/disable */
    private boolean ignoreUpdates;

    /**
     * Create an instance of the action class.
     *
     * @param standAlone Is this standalone or in a menu
     * @param icon The icon
     * @param controller The controller managing commands
     */
    public UndoAction(boolean iconOnly, Icon icon, CommandController controller) {

        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly) {
            putValue(Action.NAME, intl_mgr.getString(TITLE_PROP));
            
            KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                    KeyEvent.CTRL_MASK);

            putValue(ACCELERATOR_KEY, acc_key);
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_U));
   
        }
        
        this.controller = controller;
        this.controller.addCommandHistoryListener(this);

        this.iconOnly = iconOnly;


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
        controller.undo();
    }

    //----------------------------------------------------------
    // Methods required by the CommandListener interface
    //----------------------------------------------------------

    /**
     * A command was successfully executed
     */
    public void commandExecuted(Command cmd) {
        updateUndoMenu();
    }
    
    /**
     * A command was not successfully executed
     * 
     * @param cmd The command
     */
    public void commandFailed(Command cmd){
        updateUndoMenu();
    }

    /**
     * A command was successfully undone
     */
    public void commandUndone(Command cmd) {
        updateUndoMenu();
    }

    /**
     * A command was successfully redone
     */
    public void commandRedone(Command cmd) {
        updateUndoMenu();
    }

    /**
     * The command stack was cleared
     */
    public void commandCleared() {
        updateUndoMenu();
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

    /**
     * Enable undo menu when here is something in the command
     * history to undo.  Otherwise disable.  Set the description
     * as appropriate.
     */
    public void updateUndoMenu() {

        if (ignoreUpdates)
            return;
        
        if (controller.canUndo()) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }

        if (iconOnly)
            return;

        I18nManager intl_mgr = I18nManager.getManager();
        String name = intl_mgr.getString(TITLE_PROP);

        if (controller.canUndo()) {
            putValue(Action.NAME, name + " " + controller.getUndoDescription());
        } else {
            putValue(Action.NAME, name);
        }
               
    }
    
    /**
     * Set the action to ignore any command updates.  Used
     * to force the button to be enabled or disabled manually
     * 
     * @param flag
     */
    public void setIgnoreUpdates(boolean flag) {
        ignoreUpdates = flag;
    }
}
