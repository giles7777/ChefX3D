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

package org.chefx3d.view.awt.scenemanager;

// External imports
import org.j3d.aviatrix3d.SceneGraphObject;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.Geometry;
import org.j3d.aviatrix3d.NodeUpdateListener;

// Local imports
import org.chefx3d.model.Entity;

/**
 * Data holder class used by the scene management observer to track what needs
 * to be updated and the listener for it.
 * <p>
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.1 $
 */
class NodeData {

    /** The scene instance that needs to be updated */
    SceneGraphObject data;
       
    /** The node instance that needs to be updated */
    Node node;

    /** The node component instance that needs to be updated */
    Geometry geom;
    
    /** The Entity that needs to be updated*/
    Entity entity;

    /** The listener to add to that node instance */
    NodeUpdateListener listener;
}
