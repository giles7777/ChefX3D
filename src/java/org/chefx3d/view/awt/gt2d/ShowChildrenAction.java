/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.gt2d;

// External imports
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.j3d.util.I18nManager;

// Internal Imports
import org.chefx3d.model.Entity;

/**
 * An action class that is used to show and hide the children being
 * rendered for a given entity.
 *
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>showTitle: Text used on the action when the next option is to show
 *     the children of the selected entity.</li>
 * <li>hideTitle: Text used on the action when the next option is to show
 *     the children of the selected entity.</li>
 * </ul>
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class ShowChildrenAction extends AbstractAction {

    /** Title when the wrapper does not have children shown currently */
    private static final String SHOW_TITLE_PROP =
        "org.chefx3d.view.awt.gt2d.ShowChildrenAction.showTitle";

    /** Title when the wrapper has children shown currently */
    private static final String HIDE_TITLE_PROP =
        "org.chefx3d.view.awt.gt2d.ShowChildrenAction.hideTitle";

    /** The wrapper that this will work on */
    private EntityWrapper entityWrapper;

    /** The panel we're working from */
    private AbstractView.AbstractImagePanel mapPanel;

    /**
     * Construct a new action that handles the given wrapper and
     * talks to the map panel
     */
    ShowChildrenAction(EntityWrapper eWrapper,
                       AbstractView.AbstractImagePanel map) {

        I18nManager intl_mgr = I18nManager.getManager();

        String prop_name = eWrapper.getChildrenShown() ?
                           HIDE_TITLE_PROP :
                           SHOW_TITLE_PROP;

        putValue(NAME, intl_mgr.getString(prop_name));

        mapPanel = map;
        entityWrapper = eWrapper;

        // Only enable this if there are children to show
        Entity entity = eWrapper.getEntity();
        setEnabled(entity.hasChildren());
    }

    //---------------------------------------------------------
    // Methods defined by ActionListener
    //---------------------------------------------------------

    /**
     * Process the mouse click action on this menu item.
     *
     * @param evt The event that caused this method to be called
     */
    public void actionPerformed(ActionEvent evt) {
        entityWrapper.setChildrenShown(!entityWrapper.getChildrenShown());
        mapPanel.entityUpdateRequired();

        String prop_name = entityWrapper.getChildrenShown() ?
                           HIDE_TITLE_PROP :
                           SHOW_TITLE_PROP;

        I18nManager intl_mgr = I18nManager.getManager();
        putValue(NAME, intl_mgr.getString(prop_name));
    }
}
