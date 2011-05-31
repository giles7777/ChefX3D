/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009-2010
 *                               Java Source
 *
 * All rights reserved.
 ****************************************************************************/

package org.chefx3d.view.awt;

// External imports
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;


// Local imports
import org.chefx3d.model.*;

/**
 * 
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
class SelectZoneMenuAction extends AbstractAction {

    private Entity parent;
    private Entity entity;
    private LocationEntity locationEntity;
    private CommandController controller;
    
    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param model The world model
     */
    public SelectZoneMenuAction(
            String title,
            Icon icon,
            Entity parent, 
            Entity entity, 
            LocationEntity locationEntity, 
            CommandController controller) {

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        putValue(NAME, title);
        putValue(SHORT_DESCRIPTION, title);

        this.parent = parent;
        this.entity = entity;
        this.locationEntity = locationEntity;
        this.controller = controller;

    }
    
    //  ----------------------------------------------------------
    //  Methods implemented by ActionListener
    //  ----------------------------------------------------------

    /**
     * Invoked when an action event occurs
     */
    public void actionPerformed(ActionEvent e) {

        // send out notification
        SelectZoneCommand zoneCommand = 
            new SelectZoneCommand(locationEntity, entity.getEntityID());
        controller.execute(zoneCommand);
        
    }

    //  ----------------------------------------------------------
    //  Local Methods
    //  ----------------------------------------------------------

    /**
     * @return the parent
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(Entity parent) {
        this.parent = parent;
    }
    
    /**
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @param parent the parent to set
     */
    public void setEntity(Entity entity) {
        this.entity = entity;
    }

}
