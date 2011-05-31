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

package org.chefx3d.rules.util;

//External Imports

//Internal Imports
import java.util.ArrayList;
import java.util.Arrays;

import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.view.common.DefaultSurrogateEntityWrapper;
import org.chefx3d.view.common.EntityWrapper;
import org.chefx3d.view.common.RuleCollisionChecker;
import org.chefx3d.view.common.SurrogateEntityWrapper;

/**
 * Singleton utility class used by rules to control surrogates and command 
 * lists.
 * 
 * A surrogate is a temporary updated representation of an entity state in the 
 * scene. For every command that passes through the rule engine 1+n number of
 * surrogates will be created. There will always be 1 surrogate created for the
 * command so that the commands following it in the rule evaluations can 
 * observe the impact of that change. Otherwise, all observations are based on 
 * the current AV3D scene and that is not a true reflection of the impact of 
 * scene state that would be expected had each command evaluated in the current
 * evaluation set executed individually. Because an evaluation set is fully 
 * evaluated before executing any of the individual commands, surrogates allow
 * for the scene to be observed with the impact of each command preceding the
 * current one being evaluated. Once the current evaluation set is 
 * completed, the surrogates are removed, the AV3D scene is updated and the 
 * process starts over again with the next command waiting on the stack.
 * 
 * Command lists are those defined by CommandSequencer that
 * exist as a result of evaluating an evaluation set. 
 * 
 * This class provides expert level access controls over the surrogates and
 * command lists for various functions such as maintaining, editing, adding and
 * removing nodes from the surrogate and command lists. These routines should
 * only be used if the SceneManagementUtility does not sufficiently handle a
 * specific case. Even then, SceneManagementUtility should be updated for that
 * case so it can be reused.
 * 
 * SceneManagementUtility uses the methods defined here to perform its maintenance
 * operations and should be the principle utility class for scene management.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.16 $
 */
public class ExpertSceneManagementUtility {

    //-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Public methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------	
    
    /**
     * Remove dead entities from the command queues. Dead commands are commands
     * that incorporate entities that are being removed by a remove command.
     * This is a cleanup operation to prevent actions from occurring against
     * entities that will not exist the next frame.
     * 
     * @param model WorldModel to reference
     * @param rch RuleCollisionChecker to use
     * @param entity Entity being removed to check against queue commands with
     */
    public static void removedDeadCommands(
    		WorldModel model,
    		RuleCollisionChecker rcc, 
    		Entity entity) {
    	
		CommandSequencer sequencer = CommandSequencer.getInstance();
			
    	ArrayList<Command> fullCmdList = (ArrayList<Command>)
    		sequencer.getFullCommandList(false);
    	
    	for (Command command : fullCmdList) {
    		
    		ArrayList<Entity> allEntities = new ArrayList<Entity>();
    		
			try {
				
				allEntities = 
					CommandDataExtractor.extractAllEntities(command);
				
			} catch (NoSuchFieldException e) {
				
				// rem: WTF is this all about?????????
				DefaultErrorReporter.getDefaultReporter().debugReport(
						"RemoveDeadCommands NON FATAL " +
						"EXCEPTION, continuing execution.", e);
			}
			
			for (int i = 0; i < allEntities.size(); i++) {
    			
				Entity testEntity = allEntities.get(i);
				
				// If the entity and test entity are the same remove the cmd.
				// If the testEntity is a child of entity, remove the cmd.
				if (entity.equals(testEntity)) {
	    			
					sequencer.addCleansedCommand(
							command);
					
					SceneManagementUtility.removeSurrogate(
							rcc, 
							(PositionableEntity)testEntity);
					
					Entity cmd_entity = ((RuleDataAccessor)command).getEntity();
					if (!allEntities.contains(cmd_entity)) {
						removedDeadCommands(
							model,
							rcc,
							cmd_entity);
					}
	    		} else if (
					
	    			SceneHierarchyUtility.isEntityChildOfParent(
	    					model, testEntity, entity, true)) {
	    			
	    			sequencer.addCleansedCommand(
							command);
					
					SceneManagementUtility.removeSurrogate(
							rcc, 
							(PositionableEntity)testEntity);

	    			removedDeadCommands(
							model,
							rcc,
							((RuleDataAccessor)command).getEntity());
	    		}
    		}
    	}
    }
	
	/**
	 * Get the appropriate surrogate wrapper for the command.
	 * 
	 * @param ruleCollisionChecker RuleCollisionChecker to act on
	 * @param command Command to wrap
	 * @return Generated DefaultSurrogateEntityWrapper or null if there was
	 * a problem
	 */
	public static DefaultSurrogateEntityWrapper createSurrogate(
			RuleCollisionChecker ruleCollisionChecker,
			Command command) {
		
		WorldModel model;
		Entity entity;
    	Entity parent;
    	double[] position = new double[3];
    	float[] rotation = new float[4];
    	float[] scale = new float[3];
    	
    	Entity initParent;
    	double[] initPos = new double[3];
    	float[] initRot = new float[4];
    	float[] initScale = new float[3];
    	
    	// Get our baseline data
    	if (command instanceof RuleDataAccessor) {
    		
    		model = ((RuleDataAccessor)command).getWorldModel();
    		
    		if (model == null)
    		    return null;
    		
    		entity = 
    			((RuleDataAccessor)command).getEntity();
    		
    		if (entity == null) {
    			return null;
    		}
    		
    		// Get the rest of the data
    		parent = 
    			SceneHierarchyUtility.getEntity(
    					model, entity.getParentEntityID());
    		
    		initParent = 
    			SceneHierarchyUtility.getEntity(
					model, entity.getParentEntityID());
    		
    		if (!(entity instanceof PositionableEntity)) {
    			return null;
    		}
    		
    		((PositionableEntity) entity).getPosition(position);
    		((PositionableEntity) entity).getPosition(initPos);
    		((PositionableEntity) entity).getRotation(rotation);
    		((PositionableEntity) entity).getRotation(initRot);
    		((PositionableEntity) entity).getScale(scale);
    		((PositionableEntity) entity).getScale(initScale);
    		
    	} else {
    		return null;
    	}
    	
    	//---------------------------------------------------------------------
    	// Get specific data related to the command
    	//---------------------------------------------------------------------
    	if (command instanceof MoveEntityCommand) {
    		
    		((MoveEntityCommand)command).getEndPosition(position);
    		
    	} else if (command instanceof MoveEntityTransientCommand) {
    		
    		((MoveEntityTransientCommand)command).getPosition(position);
    		
    	} else if (command instanceof TransitionEntityChildCommand) {
    		
    		parent = (PositionableEntity)
    			((TransitionEntityChildCommand)command).getEndParentEntity();
    		((TransitionEntityChildCommand)command).getEndPosition(position);
    		((TransitionEntityChildCommand)command).getEndScale(scale);
    		
    	} else if (command instanceof ScaleEntityCommand) {
    		
    		((ScaleEntityCommand)command).getNewPosition(position);
    		((ScaleEntityCommand)command).getNewScale(scale);
    		
    	} else if (command instanceof ScaleEntityTransientCommand) {
    		
    		((ScaleEntityTransientCommand)command).getPosition(position);
    		((ScaleEntityTransientCommand)command).getScale(scale);
    		
    	} else if (command instanceof RotateEntityCommand) {
    		
    		((RotateEntityCommand)command).getCurrentRotation(rotation);
    		
    	} else if (command instanceof RotateEntityTransientCommand) {
    		
    		((RotateEntityTransientCommand)command).getCurrentRotation(
    				rotation);
    		
    	} else if (command instanceof AddEntityChildCommand) {

    		Entity tmpParent = 
    			((AddEntityChildCommand)command).getParentEntity();

    		if (tmpParent instanceof PositionableEntity) {
    			parent = (PositionableEntity) tmpParent;
    		} else {
    			return null;
    		}

    	} else if (command instanceof AddEntityChildTransientCommand) {
    		
    		Entity tmpParent = 
    			((AddEntityChildTransientCommand)command).getParentEntity();
    		
    		if (tmpParent instanceof PositionableEntity) {
    			parent = (PositionableEntity) tmpParent;
    		} else {
    			return null;
    		}
    		
    		// Confirm the parent exists by attempting to get it from the
    		// WorldModel.
    		Entity confirmParent = model.getEntity(parent.getEntityID());
    		
    		if (confirmParent == null) {

    			// Could not find the parent in the world model, check the side
    			// pocketed parent entity id.
    			Integer parentEntityParentEntityID = (Integer)
    				RulePropertyAccessor.getRulePropertyValue(
    						parent, 
    						ChefX3DRuleProperties.INITAL_ADD_PARENT);
    			
    			Entity parentParent = 
    				model.getEntity(parentEntityParentEntityID);

    			// If the parent's parent exists, go ahead and use it
    			// otherwise return null. We do it
    			// this way for add commands because there is the known
    			// possibility that the parent's add command is not yet
    			// in the newly issued command stack. This check helps
    			// us handle this case. IF this is the case, then we
    			// are, at this time, going to have only one possible
    			// blind parent.
    			if (parentParent == null) {
    				return null;
    			} else {
    				
    				double[] blindParentPos = new double[3];
    				((PositionableEntity)parent).getPosition(blindParentPos);
    				position[0] += blindParentPos[0];
    				position[1] += blindParentPos[1];
    				position[2] += blindParentPos[2];
    				parent = parentParent;
    				
    			}
    		}
    		
    	} else if (command instanceof RemoveEntityChildCommand) {
    		
    		Entity tmpParent = 
    			((RemoveEntityChildCommand)command).getParentEntity();
    		
    		if (tmpParent instanceof PositionableEntity) {
    			parent = (PositionableEntity) tmpParent;
    		} else {
    			return null;
    		}
    		
    		DefaultSurrogateEntityWrapper surrogate = 
        		new DefaultSurrogateEntityWrapper(
        				(PositionableEntity) entity, 
        				(PositionableEntity) parent,
        				position,
        				rotation, 
        				scale);
    		
    		surrogate.setEnabled(false);
        	return surrogate;
    		
    	} else if (command instanceof RemoveEntityChildTransientCommand) {
    		
    		Entity tmpParent = 
    			((RemoveEntityChildTransientCommand)command).getParentEntity();
    		
    		if (tmpParent instanceof PositionableEntity) {
    			parent = (PositionableEntity) tmpParent;
    		} else {
    			return null;
    		}
    		
    		DefaultSurrogateEntityWrapper surrogate = 
        		new DefaultSurrogateEntityWrapper(
        				(PositionableEntity) entity, 
        				(PositionableEntity) parent,
        				position,
        				rotation, 
        				scale);
    		
    		surrogate.setEnabled(false);
        	return surrogate;
    		
    	} else if (command instanceof RemoveEntityCommand) {
    		
    		DefaultSurrogateEntityWrapper surrogate = 
        		new DefaultSurrogateEntityWrapper(
        				(PositionableEntity) entity, 
        				(PositionableEntity) parent,
        				position,
        				rotation, 
        				scale);
    		
    		surrogate.setEnabled(false);
        	return surrogate;
    		
    	} else {
    		return null;
    	}
    	
    	// Final safety check that the parent is not null
    	if (parent == null) {
			return null;
		}
    	
    	// Check if the entity miter cuts. If so, we need to set the scale to
    	// the starting scale, not the current scale, unless the command is a 
    	// scale command.
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (canMiterCut) {
        	
        	if (!(command instanceof ScaleEntityCommand) && 
        			!(command instanceof ScaleEntityTransientCommand) &&
        			!(command instanceof AddEntityChildCommand)) {
        		
        		scale = 
        			TransformUtils.getStartingScale((PositionableEntity)entity);
        	}
        }
    	
        // Check for the existence of a surrogate for the entity.
        // If one exists, add onto that surrogate.
    	// Otherwise, create and add the surrogate.
        EntityWrapper wrapper = 
        	ruleCollisionChecker.getEntityWrapper(entity.getEntityID());
        
        DefaultSurrogateEntityWrapper surrogate = null;
        
        if (wrapper != null && wrapper instanceof SurrogateEntityWrapper) {
        	
        	surrogate = (DefaultSurrogateEntityWrapper) wrapper;
        	
        	// Checking if the surrogate changes the value of one of the fields
        	// is just doing a check to see if the surrogate value is different
        	// from the previous frame value. If so, the changed check will
        	// return true indicating it is a new value. If the surrogate has
        	// a changed value, but the value we have is not changed from
        	// the previous frame, then don't change the surrogate's value.
        	// In that case, something else has deliberately set the surrogate
        	// to hold that state and we want to keep it. If the new value is
        	// also different from the previous frame, then we will overwrite
        	// the existing surrogate value.
        	
        	if (surrogate != null) {
        		
	        	// Parent case
	        	if (surrogate.parentChanged()) {
	        		
	        		if (initParent != parent) {
	        			surrogate.setParentEntity((PositionableEntity)parent);
	        		}
	        		
	        	} else {
	        		surrogate.setParentEntity((PositionableEntity)parent);
	        	}
	        	
	        	// Position case
	        	if (surrogate.positionChanged()) {
	        		
	        		if (!Arrays.equals(position, initPos)) {
	        			surrogate.setPosition(position);
	        		}
	        		
	        	} else {
	        		surrogate.setPosition(position);
	        	}
	        	
	        	// Rotation case
	        	if (surrogate.rotationChanged()) {
	        		
	        		if (!Arrays.equals(rotation, initRot)) {
	        			surrogate.setRotation(rotation);
	        		}
	        		
	        	} else {
	        		surrogate.setRotation(rotation);
	        	}
	        	
	        	// Scale case
	        	if (surrogate.scaleChanged()) {
	        		
	        		if (!Arrays.equals(scale, initScale)) {
	        			surrogate.setScale(scale);
	        		}
	        		
	        	} else {
	        		surrogate.setScale(scale);
	        	}
        	}
        	
        } else {

	    	surrogate = 
	    		new DefaultSurrogateEntityWrapper(
	    				(PositionableEntity) entity, 
	    				(PositionableEntity) parent,
	    				position,
	    				rotation, 
	    				scale);
        }
        
        return surrogate;

	}
	
	/**
	 * Create an empty surrogate with just an entity reference. Used for
	 * remove surrogate wrapper parameter from the collision checker.
	 * 
	 * @param entity Entity reference to add
	 * @return Surrogate
	 */
	public static DefaultSurrogateEntityWrapper createEmptySurrogate(
			PositionableEntity entity) {
		
		return new DefaultSurrogateEntityWrapper(
				(PositionableEntity)entity, 
				(PositionableEntity)entity,
				new double[] {0.0, 0.0, 0.0},
				new float[] {0.0f, 0.0f, 0.0f, 0.0f}, 
				new float[] {1.0f, 1.0f, 1.0f});
	}
	
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
}
