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
import java.util.List;


// Internal Imports
// None

/**
 * A handler to service EntityPropertyListener(s) in a thread-safe manner.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
class EntityPropertyListenerHandler implements EntityPropertyListener {
	
	/** Initial null array of listener's */
	private final static EntityPropertyListener[] NULL_LISTENER_ARRAY = 
		new EntityPropertyListener[0];
	
	/** The working array of listeners */
	private EntityPropertyListener[] listenerArray;
	
	/** 
	 * Constructor 
	 */
	EntityPropertyListenerHandler() {
		listenerArray = NULL_LISTENER_ARRAY;
	}
	
    //----------------------------------------------------------
    // Methods defined by EntityPropertyListener
    //----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID, String propertySheet, String propertyName) {
        
		EntityPropertyListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].propertyAdded(entityID, propertySheet, propertyName);
		}
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID, String propertySheet, String propertyName) {
        
		EntityPropertyListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].propertyRemoved(entityID, propertySheet, propertyName);
		}     
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this property update an ongoing change 
	 * like a transient position or the final value
     */
    public void propertyUpdated(int entityID,
    	String propertySheet, String propertyName, boolean ongoing) {
        
		EntityPropertyListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].propertyUpdated(entityID, propertySheet, propertyName, ongoing);
		}
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        
		EntityPropertyListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].propertiesUpdated(properties);
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
	void add(EntityPropertyListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (listenerArray == NULL_LISTENER_ARRAY) { 
					listenerArray = new EntityPropertyListener[]{listener}; 
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
						EntityPropertyListener[] tmp = new EntityPropertyListener[length + 1];
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
	void remove(EntityPropertyListener listener) {
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
						EntityPropertyListener[] tmp = new EntityPropertyListener[newLength];
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

