/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006-2007
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

//External Imports

//Internal Imports

/**
 * Accessor methods required by the rule engine to access data about Commands 
 * to perform rule analysis. RuleDataAccessor interface is implemented by 
 * commands that are processed by Rules. Otherwise it isn't requried.
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public interface RuleDataAccessor{
	
	/**
	 * Get the Entity
	 * 
	 * @return Entity or null if it doesn't exist
	 */
	public Entity getEntity();
	
	/**
	 * Get the WorldModel
	 * 
	 * @return WorldModel or null if it doesn't exist
	 */
	public WorldModel getWorldModel();
	
	
	/**
	 * Reverts the command back to the last good value
	 */
	public void resetToStart();
	
	/**
	 * Set the die state of the command 
	 * 
	 * @param die True to have command die and not execute at all
	 */
	public void setCommandShouldDie(boolean die);
	
	/**
	 * Check if command should die instead of reverting
	 * 
	 * @return True if command should die, false otherwise
	 */
	public boolean shouldCommandDie();
	
	/**
	 * Check to see if externalCommand is the same as the current command.
	 * 
	 * @param externalCommand Command to compare with
	 * @return True if the same, false otherwise
	 */
	public boolean isEqualTo(Command externalCommand);
}
