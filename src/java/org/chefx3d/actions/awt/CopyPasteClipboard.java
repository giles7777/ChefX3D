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
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.chefx3d.model.Entity;

// Local imports


/**
 * An helper class to track the current copy/paste target across multiple action classes
 * <p>
 *
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
public class CopyPasteClipboard  {

    /** The current list of listener */
    private List<ClipboardListener> listenerList;

    /** The current list of the copy */
    private List<Entity> entityList;

    /**
     * Create an instance of the CopyPasteClipboard class
     */
    public CopyPasteClipboard() {
        listenerList = new ArrayList<ClipboardListener>();
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Add a listener for clip board content changes
     *
     * @param ecl
     */
    public void addClipboardListener(ClipboardListener cpl) {
        if(!listenerList.contains(cpl)){
            listenerList.add(cpl);
        }
    }

    /**
     * Remove a listener of clip board content changes
     *
     * @param ecl
     */
    public void removeClipboardListener(ClipboardListener cpl) {
        listenerList.remove(cpl);
    }

    /**
     * @return the entityList
     */
    public List<Entity> getEntityList() {
        return entityList;
    }

    /**
     * @param entityList the entityList to set
     */
    public void setEntityList(List<Entity> entityList) {
        this.entityList = entityList;

        Iterator<ClipboardListener> i = listenerList.iterator();
        while(i.hasNext()) {
            ClipboardListener l = i.next();
            l.clipboardUpdated(hasEntities());
        }

    }

    /**
     * Clear the clipboard of any entities
     */
    public void clearEntityList() {
        if (entityList != null) {
            entityList.clear();

            Iterator<ClipboardListener> i = listenerList.iterator();
            while(i.hasNext()) {
                ClipboardListener l = i.next();
                l.clipboardUpdated(hasEntities());
            }
        }
    }


    /**
     *
     * @return
     */
    private boolean hasEntities() {
        if (entityList != null && entityList.size() > 0 ) {
            return true;
        }
        return false;
    }

}
