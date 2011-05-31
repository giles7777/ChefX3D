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
import org.j3d.aviatrix3d.*;


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
public class SceneDataHolder extends Group{
	
	
    /** The scene instance that needs to be updated */
    public SceneGraphObject data;

    /** The node component instance that needs to be updated */
    public Shape3D geom;
    
    /** The Entity that needs to be updated*/
    public Entity entity;  
       
}