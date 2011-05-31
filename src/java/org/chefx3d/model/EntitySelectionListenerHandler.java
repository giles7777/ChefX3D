/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

// External Imports
// None

// Internal Imports
// None

/**
 * A handler to service EntitySelectionListener(s) in a thread-safe manner.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class EntitySelectionListenerHandler implements EntitySelectionListener {
	
	/** Initial null array of listener's */
	private final static EntitySelectionListener[] NULL_LISTENER_ARRAY = 
		new EntitySelectionListener[0];
	
	/** The working array of listeners */
	private EntitySelectionListener[] listenerArray;
	
	/** 
	 * Constructor 
	 */
	EntitySelectionListenerHandler() {
		listenerArray = NULL_LISTENER_ARRAY;
	}
	
    //----------------------------------------------------------
    // Methods defined by EntitySelectionListener
    //----------------------------------------------------------

    /**
     * An entity has been selected
     *
     * @param entityID The entity which changed
     * @param selected Status of selecting
     */
    public void selectionChanged(int entityID, boolean selected) {

		EntitySelectionListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].selectionChanged(entityID, selected);
		}
    }

    /**
     * An entity has been highlighted
     * 
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
		
		EntitySelectionListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].highlightChanged(entityID, highlighted);
		}
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Add a listener
	 *
	 * @param listener The listener to add
	 */
	void add(EntitySelectionListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (listenerArray == NULL_LISTENER_ARRAY) { 
					listenerArray = new EntitySelectionListener[]{listener}; 
				} else {
					boolean found = false;
					for (int i = listenerArray.length - 1; i >= 0; i--) {
						if (listenerArray[i] == listener) {
							found = true;
							break;
						}
					}
					if (!found) {
						int length = listenerArray.length;
						EntitySelectionListener[] tmp = new EntitySelectionListener[length + 1];
						System.arraycopy(listenerArray, 0, tmp, 0, length);
						tmp[length] = listener;
						listenerArray = tmp;
					}
				}
			}
		}
	}
	
	/**
	 * Remove a listener
	 *
	 * @param listener The listener to remove
	 */
	void remove(EntitySelectionListener listener) {
		if (listener != null) {
			synchronized(this) {
				// find the index of the listener to remove
				int index = -1;
				for (int i = this.listenerArray.length - 1; i >= 0; i--) {
					if (listenerArray[i].equals(listener)) {
						index = i;
						break;
					}
				}
				// recreate the array of listeners if necessary
				if (index != -1) {
					int newLength = listenerArray.length - 1;
					if (newLength == 0) { 
						listenerArray = NULL_LISTENER_ARRAY; 
					} else {
						EntitySelectionListener[] tmp = new EntitySelectionListener[newLength];
						System.arraycopy(listenerArray, 0, tmp, 0, index);
						if (index < newLength) {
							System.arraycopy(listenerArray, (index + 1), tmp, index, (newLength - index));
						}
						listenerArray = tmp;
					}
				}
			}
		}
	}
	
	/**
	 * Clear all listeners
	 */
	void clear() {
		if (listenerArray != NULL_LISTENER_ARRAY) {
			synchronized(this) {
				listenerArray = NULL_LISTENER_ARRAY;
			}
		}
	}
}

