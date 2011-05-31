/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.net;

// External Imports
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

// Internal Imports
import org.chefx3d.view.View;

/**
 * An action for issuing control commands.
 *
 * @author Alan Hudson
 */
public class ControlAction extends AbstractAction {

    /** version id */
    private static final long serialVersionUID = 1L;

    /** The command to issue when activated */
    private int controlCommand;

    /** The views to control */
    private View[] view;

    public ControlAction(String label, int type, View[] view) {
        super(label);

        controlCommand = type;
        this.view = view;
    }

    // ---------------------------------------------------------------
    // Methods defined by ActionListener
    // ---------------------------------------------------------------

    /**
     * An action has been performed.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {
        for(int i=0; i < view.length; i++) {
            view[i].controlChanged(controlCommand);
        }
    }
}