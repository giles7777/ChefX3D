/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package org.chefx3d.toolbar.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

// External Imports


// Local imports

/**
 * Makes use of a weak reference to hold onto the parent reference
 *
 * When components are thrown away, the components will be garbage
 * collected as normal
 */
public class WeakItemListener implements ItemListener {

    Reference<ItemListener> itemListenerRef = null;

    /**
     * Constructs a weak itemlistener, so that components sending updates
     * to this toolbar can be thrown away when they are no longer part
     * of the view.
     *
     * @param itemListener the itemlistener to wrap wreakly
     */
    public WeakItemListener(ItemListener itemListener) {
        this.itemListenerRef = new WeakReference<ItemListener>(itemListener);
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        ItemListener itemListener = itemListenerRef.get();
        if (itemListener != null) {
            itemListener.itemStateChanged(itemEvent);
        }
    }
    
}

