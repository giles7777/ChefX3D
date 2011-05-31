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
 * A handler to service EntityChildListener(s) in a thread-safe manner.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class EntityChildListenerHandler implements EntityChildListener {
	
	/** Initial null array of listener's */
	private final static EntityChildListener[] NULL_LISTENER_ARRAY = 
		new EntityChildListener[0];
	
	/** The working array of listeners */
	private EntityChildListener[] listenerArray;
	
	/** 
	 * Constructor 
	 */
	EntityChildListenerHandler() {
		listenerArray = NULL_LISTENER_ARRAY;
	}
	
    //----------------------------------------------------------
    // Methods defined by EntityChildListener
    //----------------------------------------------------------

    /**
     * A child was added.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childAdded(int parentID, int childID) {
		
		EntityChildListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].childAdded(parentID, childID);
		}
    }

    /**
     * A child was inserted.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     * @param index The index the child was inserted at
     */
    public void childInsertedAt(int parentID, int childID, int index) {

		EntityChildListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].childInsertedAt(parentID, childID, index);
		}
    }

    /**
     * A child was removed.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childRemoved(int parentID, int childID) {

		EntityChildListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].childRemoved(parentID, childID);
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
	void add(EntityChildListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (listenerArray == NULL_LISTENER_ARRAY) { 
					listenerArray = new EntityChildListener[]{listener}; 
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
						EntityChildListener[] tmp = new EntityChildListener[length + 1];
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
	void remove(EntityChildListener listener) {
		if (listener != null) {
			synchronized(this) {
				// find the index of the listener to remove
				int index = -1;
				for (int i = listenerArray.length - 1; i >= 0; i--) {
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
						EntityChildListener[] tmp = new EntityChildListener[newLength];
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

