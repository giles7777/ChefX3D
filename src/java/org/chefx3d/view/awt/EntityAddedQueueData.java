/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2006 - 2007
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt;

//External Imports

//Internal Imports
import org.chefx3d.model.Entity;
import org.web3d.x3d.sai.X3DNode;

/**
 * Container of data needed to complete X3D generation of newly added scene
 * entities.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public class EntityAddedQueueData {

	/** Entity object */
	private Entity entity;
	
	/** X3DNode to add X3D to */
	private X3DNode node;
	
	/**
	 * Constructor requires Entity and X3DNode
	 * 
	 * @param entity Entity reference from scene
	 * @param node X3DNode reference from scene
	 */
	public EntityAddedQueueData(Entity entity, X3DNode node){
		
		this.entity = entity;
		this.node = node;
	}
	
	/**
	 * Get the Entity
	 * 
	 * @return Entity
	 */
	public Entity getEntity(){
		return entity;
	}
	
	/**
	 * Get the X3DNode
	 * 
	 * @return X3DNode
	 */
	public X3DNode getX3DNode(){
		return node;
	}
	
	/**
	 * Set the Entity
	 * 
	 * @param entity Entity
	 */
	public void setEntity(Entity entity){
		this.entity = entity;
	}
	
	/**
	 * Set the X3DNode
	 * 
	 * @param node X3DNode
	 */
	public void setX3DNode(X3DNode node){
		this.node = node;
	}
}
