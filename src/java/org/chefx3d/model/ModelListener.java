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

package org.chefx3d.model;

// Standard imports
import java.util.List;

// Application specific imports

/**
 * Notification of changes in a models state. This does not include internal
 * property changes. The PropertyListener handles those.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.16 $
 */
public interface ModelListener {
    /**
     * An entity was added.
     * 
     * @param local Was this action initiated from the local UI
     * @param entity The entity added to the view
     */
    public void entityAdded(boolean local, Entity entity);

    /**
     * An entity was removed.
     * 
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity);

    /**
     * User view information changed.
     * 
     * @param local Was this action initiated from the local UI
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov);

    /**
     * The master view has changed.
     * 
     * @param local Was this action initiated from the local UI
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID);
    
    /**
     * The model has been reset.
     * 
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local);
    
}
