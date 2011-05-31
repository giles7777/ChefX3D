/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.common;

//External Imports

//Internal Imports
import org.chefx3d.model.Entity;

/**
 * Defines requirements for collision results that rule classes look to after
 * performing collision tests.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public interface CollisionResultHandler {
	
	/**
	 * Clear all of the collision results, including replacement and illegal
	 * entities.
	 */
	public void clearAll();
	
	/**
	 * Set the contents of the CollisionResultHandler to the current object.
	 * 
	 * @param rh CollisionResultHandler to copy
	 */
	public void set(CollisionResultHandler rh);
	
	/**
	 * Add the entity to the correct result set.
	 * 
	 * @param entity Entity to store in the correct result set
	 * @param colClass Classification name to associate with this entity
	 */
	public void addEntity(Entity entity, String colClass);
	
	/**
	 * Add an illegal entity to the illegal entity result set.
	 * 
	 * @param entity Entity to add to the illegal entity result set
	 */
	public void addIllegalEntity(Entity entity);
	
	/**
	 * Add an entity to be replaced to the replace entity result set.
	 * 
	 * @param entity Entity to add to the replace entity result set
	 */
	public void addReplaceEntity(Entity entity);
	
	/**
	 * Get the number of valid matches.
	 * 
	 * @return Number of valid matches
	 */
	public int getNumberOfValidMatches();
	
	/**
	 * Print out the contents of the currently held result sets.
	 */
	public void printResults();
}
