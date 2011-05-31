/*
Copyright (c) 2005-2006 Yumetech.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
 * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.MovesInstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
 */

package org.chefx3d.actions.awt;

// External imports
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.KeyStroke;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.view.ViewConfig;
import org.chefx3d.view.ViewX3D;

/**
 * An action that can be used to launch the 3D viewer.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Rex Melton
 * @version $Revision: 1.5 $
 */
public class ConfigView3DAction extends AbstractAction implements MenuListener {

    /** Name of the property to get the action description */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.ConfigView3DAction.description";

    /** Name of the property to get the action name */
    private static final String TITLE_PROP =
        "org.chefx3d.actions.awt.ConfigView3DAction.title";

    /** The action that handles the view. */
    private View3DAction source;

    /** The parent MenuItem of this */
    private JCheckBoxMenuItem checkBox;

    /**
     * Create an instance of the action class.
     *
     * @param source The source for the view instance.
     * @param menu The menu that this action is a child of.
     * @param checkBox The menu item that this is the action for.
     */
    public ConfigView3DAction(View3DAction source, JMenu menu, JCheckBoxMenuItem checkBox) {

        I18nManager intl_mgr = I18nManager.getManager();

        this.source = source;
        this.checkBox = checkBox;

        if (menu != null) {
            menu.addMenuListener(this);
        }

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_H,
            KeyEvent.ALT_MASK);

        putValue(Action.NAME, intl_mgr.getString(TITLE_PROP));
        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_H));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));
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
        ViewX3D view = (ViewX3D)source.getView();
        if((view != null) && (view instanceof ViewConfig) && checkBox.isEnabled())  {
            ViewConfig vc = (ViewConfig)view;
            vc.setConfigElevation(checkBox.isSelected());
        } else {
            // System.out.println("Do not process request");
            checkBox.setSelected(false);
            checkBox.setEnabled(false);
        }
    }

    //----------------------------------------------------------
    // Methods required by the MenuListener interface
    //----------------------------------------------------------

    /**
     * Ignored. Invoked when the menu is canceled.
     *
     * @param evt The event that caused this method to be called.
     */
    public void menuCanceled(MenuEvent evt) {
    }

    /**
     * Invoked when the menu is deselected. Enable the action when
     * the parent menu is not visible to support key bound events.
     *
     * @param evt The event that caused this method to be called.
     */
    public void menuDeselected(MenuEvent evt) {
        checkBox.setEnabled(true);
    }

    /**
     * Invoked when a menu is selected. Enable this item if an
     * Entity is available in the copy buffer.
     *
     * @param evt The event that caused this method to be called.
     */
    public void menuSelected(MenuEvent evt) {
        ViewX3D view = (ViewX3D)source.getView();
        if((view != null) && (view instanceof ViewConfig) && checkBox.isEnabled()) {
            checkBox.setEnabled(true);
            ViewConfig vc = (ViewConfig)view;
            checkBox.setSelected(vc.getConfigElevation());
        } else {
            checkBox.setEnabled(false);
        }
    }
}
