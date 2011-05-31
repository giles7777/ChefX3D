/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009 - 2010
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

// External imports
import java.util.ArrayList;
import java.util.Map;

// Local imports
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;

/**
 * Defines the requirements for rule based collision detection.
 * 
 * @author Rex Melton
 * @version $Revision: 1.5 $
 */
public interface RuleCollisionChecker {

	/** 
	 * Submit a command to the collision detection system for
	 * processing and return a list of Entities that the
	 * Command's Entity is in collision with. If no collisions
	 * are occurring, null is returned.
	 *
	 * @param command The Command to test
	 * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
	 * @return The list of Entities in collision, or null.
	 */
	public ArrayList<Entity> submitCommand(
			Command command, 
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds);
    
	/** 
	 * Submit a command to the collision detection system for
	 * processing and return a list of Entities that the
	 * Command's Entity is in collision with. If no collisions
	 * are occurring, null is returned.
	 *
	 * @param command The Command to test
	 * @param useSurrogates Flag indicating that surrogate entities should
	 * be used during collision testing.
	 * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
	 * @return The list of Entities in collision, or null.
	 */
	public ArrayList<Entity> submitCommand(
			Command command, 
			boolean useSurrogates, 
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds);
    
    /**
     * Submit a command to the collision detection system for
     * processing. A Map will be returned that contains a list of
	 * colliding Entities for the Entity that is the subject of the
	 * Command, as well as for any children Entities of the Command
	 * Entity. If no collisions are found the list will be empty for
	 * that Entity.
     *
     * @param command The Command to test
     * @param useSurrogates Flag indicating that surrogate entities should
	 * be used during collision testing.
     * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
     * @return The Map of Entities in collision.
     */
	public Map<Entity, ArrayList<Entity>> submitCommandExtended(
			Command command, 
			boolean useSurrogates,
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds);
	
    /** 
     * Print the current active bounds and collision data
     */
    public void printState();
    
	/**
	 * Add a surrogate to the working set
	 *
	 * @param surrogate The SurrogateEntityWrapper to add
	 */
	public void addSurrogate(SurrogateEntityWrapper surrogate);
	
	/**
	 * Remove a surrogate from the working set
	 *
	 * @param surrogate The SurrogateEntityWrapper to remove
	 */
	public void removeSurrogate(SurrogateEntityWrapper surrogate);
	
	/**
	 * Clear the surrogate working set
	 */
	public void clearSurrogates();
	
	/**
	 * Print the current map of surrogates for debugging rules
	 */
	public void printSurrogates();
	
	/**
	 * Return the EntityWrapper for the specified entity.
	 *
	 * @param entityID The id of the entity
	 * @return The EntityWrapper for the specified entity.
	 */
	public EntityWrapper getEntityWrapper(int entityID);
	
	/**
	 * Return the current surrogate set.
	 *
	 * @return The current surrogate set.
	 */
	public SurrogateEntityWrapper[] getSurrogates();
	
	/**
	 * Get the whole map of entities to the SurrogateEntityWrapper copy that
	 * expresses the original state of the surrogate. Note, the 
	 * SurrogateEntityWrapper is a copy of the original, not a reference to the
	 * original.
	 * 
	 * @return Map of entities to copy of original surrogate.
	 */
	public Map<Entity, SurrogateEntityWrapper> 
		getSidePocketedOriginalSurrogateStates();
	
	/**
	 * Get the original SurrogateEntityWrapper matching the entity specified. If
	 * one doesn't exist, null will be returned. Note, the 
	 * SurrogateEntityWrapper returned is a copy of the original, not a 
	 * reference.
	 *  
	 * @param entity Entity to get original SurrogateEntityWrapper for.
	 * @return SurrogateEntityWrapper or null if none found.
	 */
	public SurrogateEntityWrapper getSidePocketedOriginalSurrogateState(
			PositionableEntity entity);
	
	/**
	 * Create a copy of the SurrogateEntityWrapper and map it to the entity 
	 * as the original state of the surrogate.
	 * 
	 * @param entity Entity to map originalSurrogate to
	 */
	public void setSidePocketedOriginalSurrogateState(
			PositionableEntity entity);
	
	/**
	 * Remove the SurrogateEntityWrapper, matching the entity, from the map
	 * of original surrogate states. Set the original surrogate values back to 
	 * the original state for the surrogate if one existed before the temp 
	 * changes, otherwise, remove the surrogate from the surrogate list.
	 * 
	 * @param entity Entity entry to remove from map.
	 */
	public void removeSidePocketedOriginalSurrogateState(
			PositionableEntity entity);
	
	/**
	 * Clear all original SurrogateEntityWrapper state data, setting each of
	 * the surrogates stored back to their original states, if they still exist
	 * in the live surrogate list.
	 */
	public void clearSidePocketedOriginalSurrogateStates();
}
