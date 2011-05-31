/*
Copyright (c) 2007 Yumetech.  All rights reserved.

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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.EntitySelectionHelper;
import org.chefx3d.model.WorldModel;

/**
 * An action that can be used to deselect all items
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public class DeselectAllAction extends AbstractAction {


    /** Name of the property to get the action description */
    private static final String DESCRIPTION_PROP =
        "com.yumetech.chefx3d.editor.MenuPanelConstants.editDeselectAll";

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
    public DeselectAllAction(
            boolean iconOnly, 
            Icon icon, 
            WorldModel model) {
        
        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(DESCRIPTION_PROP));

        seletionHelper = 
            EntitySelectionHelper.getEntitySelectionHelper();

        KeyStroke acc_key = 
            KeyStroke.getKeyStroke(
                    KeyEvent.VK_A,
                    KeyEvent.SHIFT_MASK|KeyEvent.CTRL_MASK);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_A));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));
        
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
        
        // clear the current selection list
        seletionHelper.clearSelectedList();
        
    }

}
