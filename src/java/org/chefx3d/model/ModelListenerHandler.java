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
class ModelListenerHandler implements ModelListener {
	
	/** Initial null array of listener's */
	private final static ModelListener[] NULL_LISTENER_ARRAY = 
		new ModelListener[0];
	
	/** The working array of listeners */
	private ModelListener[] listenerArray;
	
	/** 
	 * Constructor 
	 */
	ModelListenerHandler() {
		listenerArray = NULL_LISTENER_ARRAY;
	}
	
    //----------------------------------------------------------
    // Methods defined by ModelListener
    //----------------------------------------------------------

	/**
     * An entity was added.
     * 
     * @param local Was this action initiated from the local UI
     * @param entity The entity added to the view
     */
    public void entityAdded(boolean local, Entity entity) {

		ModelListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].entityAdded(local, entity);
		}
    }

    /**
     * An entity was removed.
     * 
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {

		ModelListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].entityRemoved(local, entity);
		}
    }

    /**
     * User view information changed.
     * 
     * @param local Was this action initiated from the local UI
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {

		ModelListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].viewChanged(local, pos, rot, fov);
		}
    }

    /**
     * The master view has changed.
     * 
     * @param local Was this action initiated from the local UI
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {

		ModelListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].masterChanged(local, viewID);
		}
    }
    
    /**
     * The model has been reset.
     * 
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {

		ModelListener[] listeners = listenerArray;
		int length = listeners.length;
		for (int i = 0; i < length; i++) {
			listeners[i].modelReset(local);
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
	void add(ModelListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (listenerArray == NULL_LISTENER_ARRAY) { 
					listenerArray = new ModelListener[]{listener}; 
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
						ModelListener[] tmp = new ModelListener[length + 1];
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
	void remove(ModelListener listener) {
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
						ModelListener[] tmp = new ModelListener[newLength];
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

