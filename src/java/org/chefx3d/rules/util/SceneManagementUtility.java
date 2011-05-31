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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;

//Internal Imports
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.DefaultSurrogateEntityWrapper;
import org.chefx3d.view.common.RuleCollisionChecker;

/**
 * Singleton utility class used by rules to affect changes in the scene.
 * 
 * Management of the scene consists of issuing appropriate commands, 
 * managing surrogates and maintaining command lists. This utility facilitates
 * those procedures through simple method calls for the action desired. 
 * 
 * Supported operations include, but are not limited to, movement, adding, 
 * removing, swapping and replacing entities in the scene.
 * 
 * This utility should be used instead of the ExpertSceneManagementUtility 
 * unless there is a specific need to do otherwise. In those cases it is best
 * to add a method to this class to support that need so it can be reused.
 * This class is dependent on the functionality of the 
 * ExpertSceneManagementUtility. Developers are discouraged from using
 * ExpertSceneManagementUtility in their class implementations.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.64 $
 */
public class SceneManagementUtility {
	
	//-------------------------------------------------------------------------
	// Public constants
	//-------------------------------------------------------------------------
	
	public static final String SWAP_COMMAND_DESCRIPTION = "SMU:SwapCmd";

	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Public methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	
	//------------------------
	// Command issuing methods
	//------------------------
	
	/**
	 * Add a child to a parent, create the surrogate and put the command
	 * on the newly issued command queue.
	 * 
	 * This is a non transient action.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to apply surrogate to
	 * @param child Child to add to parent
	 * @param parent Parent target
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int addChild(
			WorldModel model, 
			RuleCollisionChecker collisionChecker, 
			Entity child, 
			Entity parent,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, parent)) {
			return -1;
		}
				
		AddEntityChildCommand addCmd = 
			generateAddChildCommand(
					model, collisionChecker, parent, child, bypassRules);
		
		addCmd.setBypassRules(bypassRules);

		CommandSequencer.getInstance().addNewlyIssuedCommand(addCmd);
		
		return addCmd.getTransactionID();
	}
	
	
	/**
	 * Remove a child from a parent, create the surrogate, clean command lists
	 * and put the command on the newly issued command queue. Will only work
	 * for entities already in the scene.
	 * 
	 * This is a non transient action.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to apply surrogate to
	 * @param child Child to remove from parent
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int removeChild(
			WorldModel model, 
			RuleCollisionChecker collisionChecker, 
			Entity child,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, child)) {
            ExpertSceneManagementUtility.removedDeadCommands(
                    model,
                    collisionChecker, 
                    child);
			return -1;
		}
		
		int parentEntityID = child.getParentEntityID();
		
		// Check for initial parent ID if this is an add instance that
        // is not yet in the scene. This is identified by the parent entity ID
        // being equal to -1. In this case, look for side pocketed parent ID.
        if (parentEntityID == -1) {

            Integer tmpParentEntityID = (Integer)
                RulePropertyAccessor.getRulePropertyValue(
                        child,
                        ChefX3DRuleProperties.INITAL_ADD_PARENT);

            if (tmpParentEntityID != null) {
                parentEntityID = tmpParentEntityID;
            } else {
            	return -1;
            }
        }
        
        Entity parent = model.getEntity(parentEntityID);
        
        if (parent == null) {
    		ExpertSceneManagementUtility.removedDeadCommands(
    				model,
    				collisionChecker, 
    				child);
        	return -1;
        }
		
		RemoveEntityChildCommand rmvCmd = 
			generateRemoveCommandAndCleanup(
					model, 
					collisionChecker, 
					parent, 
					child,
					bypassRules);
		
		if (rmvCmd == null) {
			return -1;
		}

		CommandSequencer.getInstance().addNewlyIssuedCommand(rmvCmd);
		        
		return rmvCmd.getTransactionID();
	}		
	
	/**
	 * Generate a command to change a property of an entity. Note that this does
	 * not do any command sequencer cleaning. Any command that should perform
	 * some sort of cleanup should not use changeProperty to affect that change.
	 * This method is strictly to change properties that don't affect position,
	 * scale, rotation, removal etc. No surrogates are created with the command.
	 * 
	 * @param model WorldModel to reference.
	 * @param entity Entity to update.
	 * @param propertySheet Name of the property sheet where the propertyName
	 * can be found.
	 * @param propertyName Name of the property in the property sheet to update.
	 * @param originalValue Optional (only required when isTransient is false) 
	 * value that is the original value already established in the entity.
	 * @param newValue New value to set in the entity.
	 * @param isTransient True to issue a transient change property command,
	 * false to issue a non-transient change property command. If this is false
	 * and originalValue is null, no command will be issued.
	 * @return Transaction ID generated for the command, -1 if there was a 
	 * problem.
	 */
	public static int changeProperty(
			WorldModel model,  
			Entity entity,
			String propertySheet,
			String propertyName,
			Object originalValue,
			Object newValue,
			boolean isTransient) {
		
		Command command = null;
		
		if (isTransient) {
			
			command = new ChangePropertyTransientCommand(
					entity, 
					propertySheet, 
					propertyName, 
					newValue, 
					model);
			
		} else if (!isTransient && originalValue == null) {
			
			return -1;
			
		} else {
			
			command = new ChangePropertyCommand(
					entity, 
					propertySheet, 
					propertyName, 
					originalValue, 
					newValue, 
					model);
			
		}

		CommandSequencer.getInstance().addNewlyIssuedCommand(command);
		
		return command.getTransactionID();
	}
		
	/**
	 * Swap an entity for a new one. Remove the source entity and replace it 
	 * with the target. The procedure does not copy over any properties from
	 * the swapOutEntity to the swapInEntity. It is assumed that the 
	 * swapInEntity is already set with the properties it should have.
	 * 
	 * Any children, with the exception of auto add children, are copied over
	 * as well. If copyAutoAdds is set to true, then auto add children will
	 * be copied over as well. Copying over auto adds is usually safe to do
	 * with a swap operation, but be sure your swap targets both support the
	 * auto add entities if you choose to include the auto adds in the copy.
	 * 
	 * Routine creates the commands, adds the surrogates, cleans the command
	 * lists and adds the commands to the newly issued command queue.
	 * 
	 * Process does not process commands acting on children. Any command 
	 * acting on the swapOutEntity or its children will end up being removed.
	 * 
	 * Currently only works for swapOutEntity already in the scene.
	 * 
	 * @param model WorldModel to reference
	 * @param rch RuleCollisionHandler to reference
	 * @param view AV3DView to build entities from
     * @param catalogManager CatalogManager to look up tool ids from
	 * @param swapInEntity Entity to swap in
	 * @param swapOutEntity Entity to swap out
	 * @param startChildrenToReplace Children that were removed as part of a 
	 * command sequence, that should be put back during an undo
	 * @param copyAutoAdds True to copy over auto adds, false to ignore them
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int swapEntity(
			WorldModel model,
			RuleCollisionHandler rch,
			EditorView view,
			CatalogManager catalogManager,
			Entity swapInEntity,
			Entity swapOutEntity,
			Entity[] startChildrenToReplace,
            boolean copyAutoAdds,
			boolean bypassRules) {
	
		// Unique id to associate with swap
		int multiTransactionID = model.issueTransactionID();
		
		// If the entity doesn't exist, then there is no swap to do. It 
		// should instead just be an add which isn't handled here.
		if (!doesEntityExist(model, swapOutEntity)) {
			return -1;
		}
		
//		ArrayList<Command> commandList = new ArrayList<Command>();
		
        // get the exact end parent, this looks at the current parent and 
        // the commands to sort out where it would end up
		Entity endParent = 
          SceneHierarchyUtility.getExactParent(model, swapOutEntity);

		// get the exact start parent, this looks at the current parent and 
		// the commands to sort out where it started from
        Entity startParent = 
            SceneHierarchyUtility.getExactStartParent(model, swapOutEntity);
		               
		if (endParent == null || startParent == null) {
			return -1;
		}
        
        // Get the children - Save the children, save the children!
		// Need to prep them for copying over to the new parent.
        ArrayList<Entity> children = 
        	SceneHierarchyUtility.getExactChildren(swapOutEntity);
        
        // Remove any auto adds, that aren't supporting other products, from
        // the list of children. This will give us the exact list of children
        // we have to copy to the swap in target.
		if (!copyAutoAdds) {
			
			Entity child = null;
			int[] idsToIgnore = new int[1];
			ArrayList<Entity> collisionList = new ArrayList<Entity>();
			
			for (int i = (children.size() - 1); i >= 0; i--) {
				
				child = children.get(i);
				
                boolean isAutoAdd = (Boolean) 
                RulePropertyAccessor.getRulePropertyValue(
                        child, 
                        ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
                
				if (isAutoAdd) {
					
					// Before removing the auto add, see if the collisions the
					// child has are dependent upon it being there.
					Command dummyCmd = rch.createCollisionDummyCommand(
							model, 
							(PositionableEntity) child, 
							true, 
							false);
					
					rch.performCollisionCheck(dummyCmd, true, true, true);
					
					collisionList.clear();
					
					if (rch.collisionEntities != null) {
						collisionList.addAll(rch.collisionEntities);
					}
					
					idsToIgnore[0] = child.getEntityID();
					
					if (AutoAddInvisibleChildrenUtility.isAutoAddChildOfParent(
							model, child, swapOutEntity)) {
						
						children.remove(i);
						
					} else if (!rch.isDependantFixedEntity(
							model, 
							(PositionableEntity) child, 
							rch, 
							view)) {
						
						children.remove(i);
					}
				}
			}
	    }
		
		// Re-organize the children list to have the auto add children first
		// and all other children after that
		ArrayList<Entity> organizedChildrenList = new ArrayList<Entity>();
		
		for (int i = (children.size() - 1); i >= 0; i--) {
			
			boolean isAutoAdd = (Boolean) 
	            RulePropertyAccessor.getRulePropertyValue(
	                    children.get(i), 
	                    ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
            
			if (isAutoAdd) {
				
				organizedChildrenList.add(children.get(i));
				children.remove(i);
			}
		}
		
		// Add the remaining non-auto add children on the end of the list
		organizedChildrenList.addAll(children);
		
		// Take these organized children and calculate the adjustment.
		double[] childPositionAdjustment = null;
			
		if (organizedChildrenList.size() > 0) {
			
			PositionableEntity child = 
				(PositionableEntity) organizedChildrenList.get(0);
			
			double[] exactPos = TransformUtils.getExactPosition(child);
			double[] oldPos = TransformUtils.getPosition(child);
			
			childPositionAdjustment = new double[3];
			childPositionAdjustment[0] = exactPos[0] - oldPos[0];
			childPositionAdjustment[1] = exactPos[1] - oldPos[1];
			childPositionAdjustment[2] = exactPos[2] - oldPos[2];
		}
		
		//----------------------------------------------------------------------
		// Begin issuing the actual commands
		//----------------------------------------------------------------------

		// Remove the swapOutEntity, this will also handle the surrogates
		// correctly for the children.
        RemoveEntityChildCommand rmvCmd = 
        	generateRemoveCommandAndCleanup(
					model, 
					rch.getRuleCollisionChecker(), 
					startParent, 
					swapOutEntity,
					bypassRules);
                
		if (rmvCmd != null) {
			rmvCmd.setDescription(
					rmvCmd.getDescription()+" "+
					SWAP_COMMAND_DESCRIPTION+"["+multiTransactionID+"]");
			CommandSequencer.getInstance().addNewlyIssuedCommand(rmvCmd);
		}
		
		// Adjust selection if the swapOutEntity is selected
		boolean isSelected = swapOutEntity.isSelected();
    	
		if (isSelected) {
			Command deselectCmd = 
				generateSelectCommand(model, swapOutEntity, false);		
			CommandSequencer.getInstance().addNewlyIssuedCommand(deselectCmd);
		}
		
		EntitySelectionHelper.getEntitySelectionHelper().entityRemoved(
				false, swapOutEntity);
        
		// Add swapInEntity	
		AddEntityChildCommand addCmd = 
			generateAddChildCommand(
					model, 
					rch.getRuleCollisionChecker(),
					endParent, 
					swapInEntity,
					bypassRules);
		
		HashSet<String> ignoreList = new HashSet<String>();
		ignoreList.add("org.chefx3d.rules.definitions.InitialAddPositionCorrectionRule");		
		addCmd.setIgnoreRuleList(ignoreList);
		
		if (addCmd != null) {
			addCmd.setDescription(
					addCmd.getDescription()+" "+
					SWAP_COMMAND_DESCRIPTION+"["+multiTransactionID+"]");
			CommandSequencer.getInstance().addNewlyIssuedCommand(addCmd);
		}
		
		if (isSelected) {
			Command selectCmd = 
				generateSelectCommand(model, swapInEntity, true);		
			CommandSequencer.getInstance().addNewlyIssuedCommand(selectCmd);
		}
		
		// Add all of the children to the swap target. Note, these have been
		// updated with their exact position data above.
		ArrayList<Command> commandList = new ArrayList<Command>();
		commandList.addAll(
				copyChildrenToParent(
						model, 
						rch.getRuleCollisionChecker(), 
						view, 
						catalogManager, 
						organizedChildrenList, 
						swapInEntity, 
						childPositionAdjustment, 
						null, 
						true,
						true,
						false, 
						true));
		
		for (Command cmd : commandList) {
			cmd.setDescription(cmd.getDescription()+" "+
					SWAP_COMMAND_DESCRIPTION+"["+multiTransactionID+"]");
			CommandSequencer.getInstance().addNewlyIssuedCommand(cmd);
		}
		
		//----------------------------------------------------------------------
		// Finish up with cleaning data for the swap out target.
		//----------------------------------------------------------------------
		
        // Make sure we go back to where it started, with the original scale
		// and rotation.
		// We also need to make sure all children go back to their starting 
		// points as well.
        if (swapOutEntity instanceof PositionableEntity) {
        	
        	// Set the parent entity back to its starting values.
            PositionableEntity posEntity = (PositionableEntity)swapOutEntity;
            
            double[] startPos = TransformUtils.getStartPosition(posEntity);
            posEntity.setPosition(startPos, false);
            
            float[] startScale = TransformUtils.getStartingScale(posEntity);
            posEntity.setScale(startScale);
            
            float[] startRot = new float[4];
            posEntity.setRotation(startRot, false);
            
            // Set the existing children back to their starting positions
            ArrayList<Entity> resetChildren = 
            	SceneHierarchyUtility.getExactChildren(swapOutEntity);
            
            for (Entity child : resetChildren) {
            	
            	if (!(child instanceof PositionableEntity)) {
            		continue;
            	}
            	
            	double[] startingPosition = new double[3];
            	((PositionableEntity)child).getStartingPosition(
            			startingPosition);
            	((PositionableEntity)child).setPosition(
            			startingPosition, false);
            	
        		float[] startingScale = new float[3];
        		((PositionableEntity)child).getStartingScale(startingScale);
        		((PositionableEntity)child).setScale(startingScale);

            }
            
            // Set the starting children back to their starting positions if
            // provided.
            if (startChildrenToReplace != null) {
            	
            	PositionableEntity startChild = null;
            	
            	for (int i = 0; i < startChildrenToReplace.length; i++) {
            		
            		if (!(startChildrenToReplace[i] instanceof 
            				PositionableEntity)) {
            			continue;
            		}
            		
            		startChild = (PositionableEntity) startChildrenToReplace[i];
            		
            		double[] startingPosition = new double[3];
            		startChild.getStartingPosition(startingPosition);
            		startChild.setPosition(startingPosition, false);
            		
            		float[] startingScale = new float[3];
            		startChild.getStartingScale(startingScale);
            		startChild.setScale(startingScale);
            		
            		if (!swapOutEntity.getChildren().contains(startChild)) {
            			swapOutEntity.addChild(startChild);
            		}
            	}
            }
            
        } else {
        	return -1;
        }
        
		
		// Put all the commands into a MultiCommand
		
/*		
		MultiCommand multCmd = new MultiCommand(
				commandList, 
				"SceneManagementUtility swap command", 
				true, 
				false, 
				multiTransactionID);
		
		multCmd.setBypassRules(bypassRules);
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(multCmd);
*/		
		return multiTransactionID;
	}
	
	/**
	 * Replace an entity with a new one. Remove the target entity and replace
	 * it with the replacement. Target entity and all of its children will be
	 * removed.
	 * 
	 * Any children, with the exception of auto add children, are copied over
	 * as well.
	 * 
	 * Routine creates the commands, adds the surrogates, cleans the command
	 * lists and adds the commands to the newly issued command queue.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to apply surrogate to
	 * @param replacementEntity Replacement for existing entity
	 * @param targetEntity Entity to replace
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int replaceEntity(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			Entity replacementEntity,
			Entity targetEntity,
			boolean bypassRules) {
		
		Entity targetParent = 
			SceneHierarchyUtility.getExactParent(model, targetEntity);
		
		// Safety check
		if (targetParent == null) {
			return -1;
		}
		
		ArrayList<Command> commandList = new ArrayList<Command>();
		
		RemoveEntityChildCommand rmvCmd = 
			generateRemoveCommandAndCleanup(
					model, 
					collisionChecker, 
					targetParent, 
					targetEntity,
					bypassRules);
		
		if (rmvCmd != null) {
			commandList.add(rmvCmd);
			CommandSequencer.getInstance().addNewlyIssuedCommand(rmvCmd);
		}
		
		AddEntityChildCommand addCmd = 
			generateAddChildCommand(
					model, 
					collisionChecker, 
					targetParent, 
					replacementEntity,
					bypassRules);
		
		commandList.add(addCmd);
		
		// Remove the temporarily queued commands from above
		if (rmvCmd != null) {
			CommandSequencer.getInstance().removeNewlyIssuedCommand(
					rmvCmd.getTransactionID());
		}
		
		// Put all the commands into a MultiCommand
		int multiTransactionID = model.issueTransactionID();
		
		MultiCommand multCmd = new MultiCommand(
				commandList, 
				"SceneManagementUtility replace command", 
				true, 
				false, 
				multiTransactionID);
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(multCmd);
		
		return multiTransactionID;
	}
	
	/**
	 * Copy the children from one parent to another. Leaves the source parent's
	 * children in place, but creates independent copies of each and parents 
	 * them to the new parent. Only PositionableEntities are copied. Surrogates
	 * are created with the commands.
	 * 
	 * The position and rotation can be adjusted for the new children, as a 
	 * general transform applied to all copied children. AutoAdd children
	 * can be copied if desired.
	 * 
	 * Deep copies of children and deep copies of child properties are 
	 * optional. Deep copies of children will copy the entire hierarchy
	 * of children instead of the initial children under the source parent.
	 * Deep copies of properties will copy all properties from the source
	 * to the copied child.
	 * 
	 * This is a non transient action.
	 * 
	 * @param model WorldModel to reference
     * @param collisionChecker RuleCollisionChecker to add surrogates to
     * @param view AV3DView to build entities from
     * @param catalogManager CatalogManager to look up tool ids from
     * @param sourceParent Entity to copy children from
     * @param targetParent Entity to add children to
     * @param childrenPositionAdjustment Position adjustment to add to all 
     * copied children
     * @param childrenRotationAdjustment Rotation adjustment to add to all
     * copied children
     * @param includeAutoAddChildren True to include auto add children in copy,
     * false to prevent copying of auto add children
     * @param deepChildCopy True to perform deep copy of children, false to do
     * shallow copy
     * @param deepPropertiesCopy True to perform deep copy of properties, 
     * false to perform shallow copy.
     * @param doPrecisionCopy True to copy children based on command queues, 
     * false to copy only children returned from getChildren()
     * @param bypassRules True to bypass rules, false to evaluate against rules
     * @return TransactionID tied to command created, -1 if failed
	 */
	public static int copyChildren(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			EditorView view,
			CatalogManager catalogManager,
			Entity sourceParent,
			Entity targetParent,
			double[] childrenPositionAdjustment,
			float[] childrenRotationAdjustment,
			boolean includeAutoAddChildren,
			boolean deepChildCopy,
			boolean deepPropertiesCopy,
			boolean doPrecisionCopy,
			boolean skipExistCheck, 
			boolean maintainEntityID, 
			boolean bypassRules) {
		
	    if (!skipExistCheck) {
	        
	        if (!doesEntityExist(model, sourceParent)) {
	            return -1;
	        }
	        
	        if (!doesEntityExist(model, targetParent)) {
	            return -1;
	        }

	    }
		
		ArrayList<Command> commandList = new ArrayList<Command>();
		ArrayList<Entity> children = new ArrayList<Entity>();
		
		if (doPrecisionCopy) {
			children = SceneHierarchyUtility.getExactChildren(sourceParent);
		} else {	
			children = sourceParent.getChildren();
		}
		
		// If auto add children are not to be included, remove them
		// from the list of children to copy over.
		if (!includeAutoAddChildren) {
			//TODO: correct relevant auto add data, any properties that depend 
			// on mappings for auto add will be incorrect currently
			for (int i = children.size() - 1; i >= 0; i--) {
				
				boolean isAutoAdd = (Boolean) 
					RulePropertyAccessor.getRulePropertyValue(
							children.get(i), 
							ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
				
				if (isAutoAdd) {
					children.remove(i);
				}
			}
		}
		
		// Perform the copy operations to generate the add commands
		commandList.addAll(
				copyChildrenToParent(
						model, 
						collisionChecker, 
						view, 
						catalogManager, 
						children, 
						targetParent, 
						childrenPositionAdjustment, 
						childrenRotationAdjustment, 
						deepPropertiesCopy,
						deepChildCopy,
						maintainEntityID, 
						bypassRules));
		
		int transactionID = model.issueTransactionID();
		
		MultiCommand multiCmd = new MultiCommand(
				commandList, 
				"SceneManagementUtility copy children command", 
				true, 
				false, 
				transactionID);
		
		multiCmd.setBypassRules(bypassRules);
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(multiCmd);
		
		return transactionID;
	}
	

	/**
	 * Reparent all non auto add children via a deep child copy operation
	 * to the target parent. Any commands acting on the source children will be
	 * removed. It is impossible to guarantee that the reparented children
	 * would be legal in their new state with existing commands acting on
	 * them. Surrogates are created with commands.
	 * 
	 * This will affect all commands in queues as well that act on any of
	 * the source parent children.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param view AV3DView to build entities from
	 * @param catalogManager CatalogManager to lookup ids from
	 * @param sourceParent Parent to transfer children from
	 * @param targetParent Parent to transfer children to
	 * @param childrenPositionAdjustment Optional position adjustment for all
	 * children
	 * @param childrenRotationAdjustment
	 * Optional rotation adjustment for all children
	 * @param deleteAutoAddChildren True to delete auto add children, false to
	 * leave them alone.
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int reparentChildren(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			EditorView view,
			CatalogManager catalogManager,
			Entity sourceParent,
			Entity targetParent,
			double[] childrenPositionAdjustment,
			float[] childrenRotationAdjustment,
			boolean deleteAutoAddChildren,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, sourceParent)) {
			return -1;
		}
		
		if (!doesEntityExist(model, targetParent)) {
			return -1;
		}

		MultiCommand multiCmd = 
			generateReparentChildren(
					model, 
					collisionChecker, 
					view, 
					catalogManager, 
					sourceParent, 
					targetParent, 
					childrenPositionAdjustment, 
					childrenRotationAdjustment, 
					deleteAutoAddChildren,
					true, 
					bypassRules);
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(multiCmd);
		
		return multiCmd.getTransactionID();
	}
	
	/**
	 * Change the parent for a single child. Cannot reparent auto adds. This
	 * is not a replacement for reparenting due to moving entities.
	 * 
	 * Removes the child and adds a new version of it to the target parent.
	 * Cleans up the command sequencer and puts the new resulting 
	 * MultiCommand on the newly issued command queue. Surrogates are created
	 * for the commands issued.
	 * 
	 * Any commands in the queues affecting the children of the child entity
	 * being reparented will be preserved and updated to be appropriate for
	 * the change to the child entity being reparented.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param view AV3DView to build entities from
	 * @param catalogManager CatalogManager to lookup ids from
	 * @param child Child to move to a new parent
	 * @param sourceParent Parent to transfer children from
	 * @param targetParent Parent to transfer children to
	 * @param childPosition Optional position adjustment for all children
	 * @param childRotation Optional rotation adjustment for all children
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int changeParent(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			EditorView view,
			CatalogManager catalogManager,
			PositionableEntity child,
			Entity sourceParent,
			Entity targetParent,
			double[] childPosition,
			float[] childRotation,
			boolean doFullChildrenSet,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, sourceParent)) {
			return -1;
		}
		
		if (!doesEntityExist(model, targetParent)) {
			return -1;
		}
		
		// Safety check that the child is a child of sourceParent.
		ArrayList<Entity> children = sourceParent.getChildren();
		
		if (!children.contains(child)) {
			return -1;
		}
		
		// Safety check that the child is not an auto add
		boolean isAutoAdd = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					child, 
					ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
		
		if (isAutoAdd) {
			return -1;
		}
		
		//--------------------------------
		// Begin actual change parent work
		//--------------------------------
		
		// Create the copy of the child to parent to the new parent
		EntityBuilder entityBuilder = view.getEntityBuilder();
		int entityID = model.issueEntityID();
		
		String toolID = child.getToolID();
		SimpleTool tool = (SimpleTool) catalogManager.findTool(toolID);
		
		if (childPosition == null) {
			childPosition = new double[3];
			child.getPosition(childPosition);
		}
		
		if (childRotation == null) {
			childRotation = new float[4];
			child.getRotation(childRotation);
		}
		
		Entity childCopy = entityBuilder.createEntity(
				model, 
				entityID, 
				childPosition, 
				childRotation, 
				tool);
			
		// Deep copy properties automatically
		copyProperties(child, childCopy);
		
		// Overwrite the position and rotation data as those end up getting
		// copied over from the copyProperties operation. Do this only if
		// the child is a PositionableEntity.
		if (childPosition != null && 
				(childCopy instanceof PositionableEntity)) {
			((PositionableEntity) childCopy).setPosition(childPosition, false);
		}
		
		if (childRotation != null && 
				(childCopy instanceof PositionableEntity)) {
			((PositionableEntity) childCopy).setRotation(childRotation, false);
		}
		
		// Start issuing the commands.
		ArrayList<Command> commandList = new ArrayList<Command>();
		
		// Remove the child from its current parent
		RemoveEntityChildCommand rmvCmd =
			generateRemoveCommandAndCleanup(
					model, 
					collisionChecker, 
					sourceParent, 
					child,
					bypassRules);
		
		if (rmvCmd != null) {
			commandList.add(rmvCmd);
		} 
		
		// Add new child to new parent
		commandList.add( 
			generateAddChildCommand(
					model, 
					collisionChecker, 
					targetParent,
					childCopy,
					bypassRules));		
		
		// Reparent all children of child to the new child entity
		MultiCommand generatedChildrenMultiCmd = 
			generateReparentChildren(
					model, 
					collisionChecker, 
					view, 
					catalogManager, 
					child, 
					childCopy, 
					null, 
					null, 
					true,
					true, 
					bypassRules);
		
		commandList.addAll(generatedChildrenMultiCmd.getCommandList());
		
		// Issue the final MultiCommand
		int transactionID = model.issueTransactionID();
		
		MultiCommand multiCmd = new MultiCommand(
				commandList, 
				"SceneManagementUtility change parent command", 
				true, 
				false, 
				transactionID);
		
		multiCmd.setBypassRules(bypassRules);
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(multiCmd);
		
		return multiCmd.getTransactionID();
	}
	
	/**
	 * Generate MoveEntityCommand and MoveEntityTransientCommand. General
	 * command issuing request for a moving entity that doesn't change 
	 * parents. Surrogate is created for command issued.
	 * 
	 * MoveEntityTransientCommands should be used between 
	 * TransitionEntityChildCommands to move entities in a transient state.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to move
	 * @param pickParent Optional pickParent for MoveEntityTransientCommands
	 * @param position Position to move entity to relative to parent
	 * @param isTransient True to issue transient command, false for non
	 * transient
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int moveEntity(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			Entity pickParent,
			double[] position,
			boolean isTransient,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		Command command = generateMoveCommand(
				model, 
				collisionChecker, 
				entity, 
				null, 
				pickParent, 
				position, 
				null, 
				null, 
				isTransient, 
				null,
				bypassRules);
		
		((RuleBypassFlag)command).setBypassRules(bypassRules);
		
		command.setDescription("SceneManagementUtility : Move Entity");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(command);
		
		return command.getTransactionID();
	}
	
	/**
	 * Generate a MoveEntityCommand that side pockets the starting
	 * children. This is not an option for MoveEntityTransientCommands. 
	 * Surrogate is created for command issued.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to move
	 * @param pickParent Optional pickParent for MoveEntityTransientCommands
	 * @param position Position to move entity to relative to parent
	 * @param isTransient True to issue transient command, false for non
	 * transient
	 * @param startChildren Children to save
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int moveEntitySaveChildren(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			Entity pickParent,
			double[] position,
			ArrayList<Entity> startChildren,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		Command command = generateMoveCommand(
				model, 
				collisionChecker, 
				entity, 
				null, 
				pickParent, 
				position, 
				null, 
				null, 
				false, 
				startChildren,
				bypassRules);
		
		((RuleBypassFlag)command).setBypassRules(bypassRules);
		
		command.setDescription(
				"SceneManagementUtility : Move Entity Save Children");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(command);
		
		return command.getTransactionID();
	}
	
	/**
	 * Generate the standard TransitionEntityChildCommand. This command
	 * moves and reparents an entity. This should be done on the initial
	 * change of position preceeding MoveEntityTransientCommands and at the end
	 * of a series of MoveEntityTransientCommands with the first 
	 * TransitionEntityChildCommand flagged as transient and the last flagged
	 * as non transient. Surrogate is created for command issued.
	 * 
	 * It is standard practice for the transient TransitionEntityChildCommand
	 * to reparent the entity to the active zone.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to move
	 * @param parent Parent to reparent entity to
	 * @param position Position to move entity relative to parent
	 * @param rotation Rotation to apply to entity
	 * @param isTransient True to issue transient command, false for
	 * non transient
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int moveEntityChangeParent(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			Entity parent,
			double[] position,
			float[] rotation,
			boolean isTransient,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		if (!doesEntityExist(model, parent)) {
			return -1;
		}
		
		Command command = generateMoveCommand(
				model, 
				collisionChecker, 
				entity, 
				parent, 
				null, 
				position, 
				rotation, 
				null, 
				isTransient, 
				null,
				bypassRules);
		
		((RuleBypassFlag)command).setBypassRules(bypassRules);
		
		command.setDescription(
				"SceneManagementUtility : Move Entity Change Parent");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(command);
		
		return command.getTransactionID();
	}
	
	/**
	 * Generate a scaling TransitionEntityChildCommand. This command
	 * moves, scales and reparents an entity. This should be done on the 
	 * initial change of position preceeding MoveEntityTransientCommands and 
	 * at the end of a series of MoveEntityTransientCommands with the first 
	 * TransitionEntityChildCommand flagged as transient and the last flagged
	 * as non transient. Surrogate is created for command issued.
	 * 
	 * It is standard practice for the transient TransitionEntityChildCommand
	 * to reparent the entity to the active zone.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to move
	 * @param parent Parent to reparent entity to
	 * @param position Position to move entity relative to parent
	 * @param rotation Rotation to apply to entity
	 * @param scale Scale to apply to entity
	 * @param isTransient True to issue transient command, false for
	 * non transient
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int moveEntityChangeParentAndScale(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			Entity parent,
			double[] position,
			float[] rotation,
			float[] scale,
			boolean isTransient,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		if (!doesEntityExist(model, parent)) {
			return -1;
		}
		
		Command command = generateMoveCommand(
				model, 
				collisionChecker, 
				entity, 
				parent, 
				null, 
				position, 
				rotation, 
				scale, 
				isTransient, 
				null,
				bypassRules);
		
		if (command == null) {
			return -1;
		}
		
		((RuleBypassFlag)command).setBypassRules(bypassRules);
		
		command.setDescription(
				"SceneManagementUtility : "+
				"Move Entity Change Parent and Scale");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(command);
		
		return command.getTransactionID();
	}
	
	/**
	 * Generate a non transient TransitionEntityChildCommand that side pockets
	 * the starting children. When performing an undo operation the children
	 * specified in this side pocketed list will be restored. This command
	 * moves, and reparents an entity. Side pocketing of children is only 
	 * needed on non transient TransitionEntityChildCommands. Surrogate is 
	 * created for command(s) issued.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to move
	 * @param parent Parent to reparent entity to
	 * @param position Position to move entity relative to parent
	 * @param rotation Rotation to apply to entity
	 * @param startChildren List of children to side pocket
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int moveEntityChangeParentSaveChildren(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			Entity parent,
			double[] position,
			float[] rotation,
			ArrayList<Entity> startChildren,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		if (!doesEntityExist(model, parent)) {
			return -1;
		}
	
		Command command = generateMoveCommand(
				model, 
				collisionChecker, 
				entity, 
				parent, 
				null, 
				position, 
				rotation, 
				null, 
				false, 
				startChildren,
				bypassRules);
		
		command.setDescription(
				"SceneManagementUtility : "+
				"Move Entity Change Parent Save Children");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(command);
		
		return command.getTransactionID();
	}
	
	/**
	 * Issue the appropriate rotate command based on the isTransient value 
	 * passed in. Surrogate is created for command issued.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to rotate
	 * @param rotation Rotation amount [x,y,z,w]
	 * @param isTransient True to issue transient command, false to issue
	 * non transient command
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int rotateEntity(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			float[] rotation,
			boolean isTransient,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		Command rotateCommand;
		int transactionID = model.issueTransactionID();
		
		if (isTransient) {
			
			rotateCommand = new RotateEntityTransientCommand(
					model, 
					transactionID, 
					entity.getEntityID(), 
					rotation);
			
		} else {
			
			float[] startRotation = new float[4];
			entity.getStartingRotation(startRotation);
			
			rotateCommand = new RotateEntityCommand(
					model, 
					transactionID, 
					entity, 
					rotation, 
					startRotation);
			
		}
		
		rotateCommand.setDescription("SceneManagementUtility : Rotate Command");
		
		((RuleBypassFlag)rotateCommand).setBypassRules(bypassRules);
		
		addSurrogate(collisionChecker, rotateCommand);
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(rotateCommand);
		
		return transactionID;
	}
	
	/**
	 * Issue the appropriate scale command based on the isTransient value 
	 * passed in. Surrogate is created for command issued.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to scale
	 * @param position New position of scaled entity [x,y,z]
	 * @param scale Scale amount [x,y,z]
	 * @param isTransient True to issue transient command, false to issue
	 * non transient command
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int scaleEntity(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			double[] position,
			float[] scale,
			boolean isTransient,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		Command scaleCommand = 
			generateScaleCommand(
					model, 
					collisionChecker, 
					entity, 
					position, 
					scale, 
					isTransient, 
					null, 
					null,
					bypassRules);
		
		scaleCommand.setDescription("SceneManagementUtility : Scale Command");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(scaleCommand);
		
		return scaleCommand.getTransactionID();
	}
	
	/**
	 * Issue the appropriate scale command based on the isTransient value 
	 * passed in, and the existence of startChildren and startPositions values.
	 * If startChildren and startPositions values are passed in they are
	 * side pocketed so they can be set back correctly if the scale operation 
	 * is undone. Surrogate is created for command issued.
	 * 
	 * Note, transient scale commands do not side pocket child data. If 
	 * isTransient is set to true, the state of startChildren and 
	 * startPositions is ignored.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to scale
	 * @param position New position of scaled entity [x,y,z]
	 * @param scale Scale amount [x,y,z]
	 * @param isTransient True to issue transient command, false to issue
	 * non transient command
	 * @param startChildren ArrayList of the entities children pre scale
	 * @param startPositions ArrayList of the PositionableData for each child
	 * pre scale
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return TransactionID tied to command created, -1 if failed
	 */
	public static int scaleEntity(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			double[] position,
			float[] scale,
			boolean isTransient,
			ArrayList<Entity> startChildren,
			ArrayList<PositionableData> startPositions,
			boolean bypassRules) {
		
		if (!doesEntityExist(model, entity)) {
			return -1;
		}
		
		Command scaleCommand = 
			generateScaleCommand(
					model, 
					collisionChecker, 
					entity, 
					position, 
					scale, 
					isTransient, 
					startChildren, 
					startPositions,
					bypassRules);
		
		removeOldCommands(scaleCommand);
		
		scaleCommand.setDescription("SceneManagementUtility : Scale Command");
		
		CommandSequencer.getInstance().addNewlyIssuedCommand(scaleCommand);
		
		return scaleCommand.getTransactionID();
	}
	
	/**
     * Some commands require the entity to be moved at the end so that auto
     * add items will be added back in.  This helper utility issues those
     * moves as Transient commands. No surrogate issued as part of this 
     * operation.
     *
     * @param model The WorldModel that holds the data
     * @param collisionChecker RuleCollisionChecker to add surrogate to
     * @param entity The entity to be nudged
     * @param position The position to nudge it to, if null use the entity's
     * current position and don't evaluate the InitialMovePositionCorrectionRule
     * @param isTransient The transient state flag
     * @return TransactionID tied to command created, -1 if failed
     */
    public static int nudgeEntity(
            WorldModel model, 
            RuleCollisionChecker collisionChecker, 
            PositionableEntity entity, 
            double[] position, 
            boolean isTransient) {
        
        if (!doesEntityExist(model, entity)) {
            return -1;
        }
        
        boolean wasPositionNull = false;
        
        if (position == null) {
        	wasPositionNull = true;
        	
        	// Get the position
            position = 
                TransformUtils.getExactPosition(entity);
        }

        ArrayList<Command> commandList = new ArrayList<Command>();
        
        // First remove all auto added children, and create their surrogates.
        // Don't remove any auto add by position children.
        ArrayList<Entity> children = entity.getChildren();

        for (int i = 0; i < children.size(); i++) {

            Boolean isAutoAdd = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                        children.get(i),
                        ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
            
            boolean isAutoAddByPosition = 
            	AutoAddByPositionUtility.isAutoAddByPositionChild(
            			model, children.get(i)); 
            
            boolean isAutoAddInvisible = 
            	AutoAddInvisibleChildrenUtility.isAutoAddChildOfParent(
            			model, children.get(i), entity);

            if (isAutoAdd && !isAutoAddByPosition && !isAutoAddInvisible) {
                
                // Remove the child from its current parent
                RemoveEntityChildCommand rmvCmd =
                    generateRemoveCommandAndCleanup(
                            model, 
                            collisionChecker, 
                            entity, 
                            children.get(i),
                            true);            
                
                if (rmvCmd != null) {
                    commandList.add(rmvCmd);
                }
                
            }
            
        }

        // get the target parent
        PositionableEntity targetParent = 
            (PositionableEntity)SceneHierarchyUtility.getExactParent(model, entity);

        Command mvCmd = generateMoveCommand(
                model, 
                collisionChecker, 
                entity, 
                null, 
                targetParent, 
                position, 
                null, 
                null, 
                isTransient, 
                null,
                false);
        
        mvCmd.setDescription("SceneManagementUtility : Nudge");
        
        if (wasPositionNull) {
	        HashSet<String> ignoreList = new HashSet<String>();
			ignoreList.add("org.chefx3d.rules.definitions.InitialMovePositionCorrectionRule");		
			mvCmd.setIgnoreRuleList(ignoreList);
        }
        
        commandList.add(mvCmd);
        
        // Next, move the entity
        MultiCommand multCmd = new MultiCommand(
                commandList,
                "MultiCmd -> Nudge the entity",
                true,
                false);

        CommandSequencer.getInstance().addNewlyIssuedCommand(multCmd);
        
        return multCmd.getTransactionID();
    }
	
    /**
     * Some commands require the entity to be moved at the end so that auto
     * add items will be added back in.  This helper utility issues those
     * moves as Transient commands. 
     *
     * @param model The WorldModel that holds the data
     * @param collisionChecker RuleCollisionChecker to add surrogate to
     * @param entity The entity to be nudged
     * @param isTransient The transient state flag
     * @return TransactionID tied to command created, -1 if failed
     */
    public static int nudgeEntity(
            WorldModel model, 
            RuleCollisionChecker collisionChecker, 
            PositionableEntity entity, 
            boolean isTransient) {

        return nudgeEntity(model, collisionChecker, entity, null, isTransient);
        
    }
	
	//------------------------
	// Entity specific methods
	//------------------------
	
	/**
	 * Copy all properties from one entity to the other. Uses Map's putAll to
	 * copy the respective maps over and overwrite any existing value with the
	 * value being copied.
	 * 
	 * @param sourceEntity Entity to copy properties from
	 * @param targetEntity Entity to copy properties to
	 */
	public static void copyProperties(
			Entity sourceEntity,
			Entity targetEntity) {
		
		HashMap<String, Map<String, Object>> sourceMap = 
			(HashMap<String, Map<String, Object>>) 
			sourceEntity.getPropertiesMap();
		
		HashMap<String, Map<String, Object>> targetMap = 
			(HashMap<String, Map<String, Object>>)
			targetEntity.getPropertiesMap();
		
		Object[] keys = sourceMap.keySet().toArray();
		
		for (int i = 0; i < keys.length; i++) {
			
			String key = (String) keys[i];
			
			if (targetMap.containsKey(key)) {
				
				targetMap.get(key).putAll(sourceMap.get(key));
			}
		}
	}
	
    /**
     * Get the list of entities that are set to be removed in the newly
     * issued command queue.
     *
     * @return ArrayList<Entity> of entities set to be removed
     */
    public static ArrayList<Entity> getNewlyIssuedRemoveCommandEntities() {

        ArrayList<Entity> removeEntityList =
            new ArrayList<Entity>();
        
        ArrayList<Command> newlyIssuedCommands = (ArrayList<Command>)
        	CommandSequencer.getInstance().getNewlyIssuedCommandList();

        for (int i = 0; i < newlyIssuedCommands.size(); i++) {

            Command cmd = newlyIssuedCommands.get(i);

            if (cmd instanceof RemoveEntityChildCommand) {

                removeEntityList.add(
                        ((RemoveEntityChildCommand) cmd).getEntity());
            } else if (cmd instanceof RemoveEntityChildTransientCommand) {

                removeEntityList.add(
                        ((RemoveEntityChildTransientCommand) cmd).getEntity());
            }
        }

        return removeEntityList;
    }
	
	//------------------
	// Surrogate methods
	//------------------
	
    /**
     * Add the command's entity as a surrogate.
     *
     * @param collisonChecker Rule collision checker to add surrogate to
     * @param command Command to process and turn into a surrogate
     */
    public static void addSurrogate(
    		RuleCollisionChecker collisionChecker, 
    		Command command) {

        DefaultSurrogateEntityWrapper surrogate =
            ExpertSceneManagementUtility.createSurrogate(
            		collisionChecker,
            		command);

        if (surrogate != null) {
            collisionChecker.addSurrogate(surrogate);
        }
    }

    /**
     * Temporarily adjust the surrogates with an adjustment to the specified
     * entity. This can be undone via the removeTempSurrogate() method and
     * will result in the surrogate list being put back to the state it was in
     * prior to the temp surrogate adjustment made here. Use this to temporarily
     * adjust the surrogates for rule testing so the surrogate list can be 
     * restored afterwards for future testing.
     */
    public static void addTempSurrogate(
    		RuleCollisionChecker collisionChecker, 
    		Command command) {
    	
    	if (command instanceof RuleDataAccessor) {
    	
    		Entity entity = ((RuleDataAccessor)command).getEntity();
    		
    		if (entity instanceof PositionableEntity) {
    		
    			collisionChecker.setSidePocketedOriginalSurrogateState(
    					(PositionableEntity) entity);
    			
    			addSurrogate(collisionChecker, command);
    		}
    	}
    }
    
    /**
     * Remove the surrogate representing the entity.
     *
     * @param collisionChecker Rule collision checker to add surrogate to 
     * @param entity Entity to have it surrogate removed
     */
    public static void removeSurrogate(
    		RuleCollisionChecker collisionChecker, 
    		PositionableEntity entity) {

        DefaultSurrogateEntityWrapper surrogate =
            ExpertSceneManagementUtility.createEmptySurrogate(entity);

        if (surrogate != null) {
            collisionChecker.removeSurrogate(surrogate);
        }
    }
    
    /**
     * Removes any temporary surrogate adjustments to the entity. Puts the 
     * surrogate for that entity back to the state it was in before any 
     * temporary adjustments were made.
     */
    public static void removeTempSurrogate(
    		RuleCollisionChecker collisionChecker, 
    		PositionableEntity entity) {
    	
    	collisionChecker.removeSidePocketedOriginalSurrogateState(entity);
    }
    
    /**
     * Clear out all the temporary surrogates. This is the preferred method
     * to call at the end of each rule evaluation if any temporary surrogates
     * have been added. It will remove all temporary surrogates in existence
     * and restore the surrogate list to its state prior to any temporary 
     * surrogates being added.
     */
    public static void clearTempSurrogates(
    		RuleCollisionChecker collisionChecker) {
    	
    	collisionChecker.clearSidePocketedOriginalSurrogateStates();
    }
    
    /**
     * Fake the removal of an entity in the surrogate set for collision testing
     * purposes. Entity must exist in the world for the surrogate to be created.
     * Be sure to call removeTempSurrogate to remove the surrogate created for 
     * the entity. Calling removeSurrogate() instead of removeTempSurrogate() 
     * could corrupt the surrogate list.
     * 
     * @param model WorldModel to reference
     * @param collisionChecker CollisionChecker to reference
     * @param child Entity to fake removal for
     */
    public static void fakeRemoveEntityWithSurrogate(
    		WorldModel model, 
			RuleCollisionChecker collisionChecker, 
			Entity child) {
    		
    		if (!doesEntityExist(model, child)) {
    			return;
    		}
    		
    		Entity parent = SceneHierarchyUtility.getExactParent(model, child);
            
            if (parent == null) {
            	return;
            }
        	
    		RemoveEntityChildCommand rmvCmd = 
    			new RemoveEntityChildCommand(model, parent, child, 0);
    		
    		addTempSurrogate(collisionChecker, rmvCmd);

    }
	
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	
    /**
     * Principle implementation for adding children to parents in the scene.
     * Creates the AddEntityChildCommand, adds it as a surrogate and 
     * returns the command.
     * 
     * This is a non transient action.
     * 
     * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to apply surrogate to
	 * @param parent Parent to remove child from
	 * @param child Child to remove from parent
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return AddEntityChildCommand command
     */
    private static AddEntityChildCommand generateAddChildCommand(
    		WorldModel model,
    		RuleCollisionChecker collisionChecker, 
    		Entity parent,
    		Entity child,
    		boolean bypassRules) {

    	int transactionID = model.issueTransactionID();
		
		AddEntityChildCommand addCmd = 
			new AddEntityChildCommand(
					model, 
					transactionID, 
					parent, 
					child,
					true);
		
		addCmd.setBypassRules(bypassRules);
		
		addSurrogate(collisionChecker, addCmd);
		
		return addCmd;
    }
    
    /**
	 * Principle implementation for reparenting all children via 
	 * a deep child copy operation to the target parent. Any commands acting 
	 * on the source children will be removed. It is otherwise impossible to 
	 * guarantee that the reparented children would be legal in their new state 
	 * with existing commands acting on them. Surrogates are added for each
	 * command issued as part of this operation.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param view AV3DView to build entities from
	 * @param catalogManager CatalogManager to lookup ids from
	 * @param sourceParent Parent to transfer children from
	 * @param targetParent Parent to transfer children to
	 * @param childrenPositionAdjustment Optional position adjustment for all
	 * children, can be null to not change position
	 * @param childrenRotationAdjustment
	 * Optional rotation adjustment for all children, can be null to not
	 * change rotation
     * @param copyAutoAddChildren True to copy auto add children, false to
     * ignore them.
	 * @param doFullReparent True to copy all children including 
	 * evaluation of command queues to get complete picture of child state,
	 * false to reparent just the children returned from getChildren()
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return MultiCommand
	 */
    private static MultiCommand generateReparentChildren(
    		WorldModel model,
			RuleCollisionChecker collisionChecker,
			EditorView view,
			CatalogManager catalogManager,
			Entity sourceParent,
			Entity targetParent,
			double[] childrenPositionAdjustment,
			float[] childrenRotationAdjustment,
            boolean copyAutoAddChildren,
			boolean doFullReparent, 
			boolean bypassRules) {
    	
    	ArrayList<Command> commandList = new ArrayList<Command>();		
		ArrayList<Entity> children = new ArrayList<Entity>();
		
		if (doFullReparent) {
			children = SceneHierarchyUtility.getExactChildren(sourceParent);
		} else {
			children = sourceParent.getChildren();
		}
		
		// Handle auto add cases
		if (!copyAutoAddChildren) {
			
			for (int i = children.size() - 1; i >= 0; i--) {
				
                boolean isAutoAdd = (Boolean) 
                RulePropertyAccessor.getRulePropertyValue(
                        children.get(i), 
                        ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
                
				if (isAutoAdd) {
					
					RemoveEntityChildCommand rmvCmd = 
						generateRemoveCommandAndCleanup(
							model, 
							collisionChecker, 
							sourceParent, 
							children.get(i),
							bypassRules);
					
					if (rmvCmd != null) {
						commandList.add(rmvCmd);
					}
					
					children.remove(i);
				}
			}
	    }
				
		// Perform the copy operations to generate the add commands
		commandList.addAll(
				copyChildrenToParent(
						model, 
						collisionChecker, 
						view, 
						catalogManager, 
						children, 
						targetParent, 
						childrenPositionAdjustment, 
						childrenRotationAdjustment, 
						true,
						true,
						false, 
						bypassRules));
		
		// Remove all of the source children
		for (Entity child : children) {
			
			RemoveEntityChildCommand rmvCmd =
					generateRemoveCommandAndCleanup(
							model, 
							collisionChecker, 
							sourceParent, 
							child,
							bypassRules);
			
			if (rmvCmd != null) {
				commandList.add(rmvCmd);
			}
		}
		
		// Issue the final MultiCommand
		int transactionID = model.issueTransactionID();
		
		MultiCommand multiCmd = new MultiCommand(
				commandList, 
				"SceneManagementUtility generate reparent command", 
				true, 
				false, 
				transactionID);
		
		multiCmd.setBypassRules(bypassRules);
		
		return multiCmd;
    }
    
    /**
     * Makes copies of the children passed in and adds them to the parent
     * specified. Only copies PositionableEntities. Each copy is a unique
     * entity.
     * 
     * Copies can be adjusted as a group by a single position adjustment and
     * rotation adjustment. Copies can also get deep copies of the properties
     * assigned to their source entities. A shallow copy will utilize the
     * default set of properties assigned when the entity is created.
     * 
     * Surrogates are added during this routine and the result is an ArrayList
     * of AddEntityChildCommands. These have not yet been put on any queue.
     * 
     * @param model WorldModel to reference
     * @param collisionChecker RuleCollisionChecker to add surrogates to
     * @param view AV3DView to build entities from
     * @param catalogManager CatalogManager to look up tool ids from
     * @param children Entities to be copied to parent
     * @param targetParent Entity to add children to
     * @param childrenPositionAdjustment Position adjustment to add to all 
     * copied children, can be null to not change position
     * @param childrenRotationAdjustment Rotation adjustment to add to all
     * copied children, can be null to not change rotation
     * @param deepPropertiesCopy True to perform deep copy of properties, 
     * false to perform shallow copy.
     * @param deepChildrenCopy True to perform deep copy of child hierarchy
     * using recursion, false to only the provided copy children list
     * @param bypassRules True to bypass rules, false to evaluate against rules
     * @return ArrayList of commands generated
     */
	private static ArrayList<Command> copyChildrenToParent(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			EditorView view,
			CatalogManager catalogManager,
			ArrayList<Entity> children,
			Entity targetParent,
			double[] childrenPositionAdjustment,
			float[] childrenRotationAdjustment,
			boolean deepPropertiesCopy,
			boolean deepChildrenCopy,
			boolean maintainEntityID, 
			boolean bypassRules) {

		ArrayList<Command> commandList = new ArrayList<Command>();
		EntityBuilder entityBuilder = view.getEntityBuilder();
		
		// Create a copy of each entity by generating a new entity from the 
		// toolID. Only copies PositionableEntities.
		for (Entity child : children) {
			
			if (!(child instanceof PositionableEntity)) {
				continue;
			}
			
			PositionableEntity positionableChild = 
				(PositionableEntity) child;
						
			double[] copyPosition = new double[3];
			float[] copyRotation = new float[4];
			
			positionableChild.getPosition(copyPosition);
			positionableChild.getRotation(copyRotation);
			
			// If specified, apply position adjustment
			if (childrenPositionAdjustment != null) {
				
				copyPosition[0] += childrenPositionAdjustment[0];
				copyPosition[1] += childrenPositionAdjustment[1];
				copyPosition[2] += childrenPositionAdjustment[2];
			}
			
			// If specified, apply rotation adjustment
			if (childrenRotationAdjustment != null) {
				
				AxisAngle4f currentRotation = new AxisAngle4f(copyRotation);
				Matrix4f currentRotationMtx = new Matrix4f();
				currentRotationMtx.set(currentRotation);
				
				AxisAngle4f adjustmentRotation = 
					new AxisAngle4f(childrenRotationAdjustment);
				Matrix4f adjustmentRotationMtx = new Matrix4f();
				adjustmentRotationMtx.set(adjustmentRotation);
				
				currentRotationMtx.add(adjustmentRotationMtx);
				
				AxisAngle4f resultingRotation = new AxisAngle4f();
				resultingRotation.set(currentRotation);
				resultingRotation.get(copyRotation);
			}
			
			// Create the actual copy of the entity
			int entityID = child.getEntityID();
			if (!maintainEntityID) {
			    entityID = model.issueEntityID();
			}
			
			String toolID = child.getToolID();
			SimpleTool tool = (SimpleTool) catalogManager.findTool(toolID);
			
			Entity copyEntity = entityBuilder.createEntity(
					model, 
					entityID, 
					copyPosition, 
					copyRotation, 
					tool);
			
			// Deep copy if requested. A deep copy copies over all properties
			// of the original entity to the new one.
			if (deepPropertiesCopy) {
				copyProperties(child, copyEntity);
			}
			
			// If the child is an auto add, then the copy should be flagged as
			// one as well.
			copyAutoAddData(child, copyEntity, targetParent.getEntityID());
			
			// Set the initial add parent for the copy to the parent it is 
			// being added to.
			RulePropertyAccessor.setProperty(
					copyEntity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.INITAL_ADD_PARENT, 
					targetParent.getEntityID());
			
			// Issue the actual command
			AddEntityChildCommand addCmd = 
				generateAddChildCommand(
						model, 
						collisionChecker, 
						targetParent, 
						copyEntity,
						bypassRules);

			commandList.add(addCmd);	
			
			// Perform deep children copy if requested
			if (deepChildrenCopy) {
				
			    ArrayList<Entity> childChildren = 
			        SceneHierarchyUtility.getExactChildren(child);
			    
				commandList.addAll(
						copyChildrenToParent(
								model, 
								collisionChecker, 
								view, 
								catalogManager, 
								childChildren, 
								copyEntity, 
								childrenPositionAdjustment, 
								childrenRotationAdjustment, 
								deepPropertiesCopy,
								deepChildrenCopy,
								maintainEntityID, 
								bypassRules));
			}
		}
		
		return commandList;
	}
	
	/**
	 * Generate the appropriate move command based on the parameters given.
	 * Returns either MoveEntityTransientCommand, MoveEntityCommand or
	 * TransitionEntityChildCommand based on the combination of parameters
	 * provided. A surrogate is added for the command generated.
	 * 
	 * See command constructors to understand various possibilities.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to move
	 * @param parent Optional parent to reparent to, results in issuing 
	 * a TransitionEntityChildCommand
	 * @param pickParent Optional pick parent set in 
	 * MoveEntityTransientCommands
	 * @param position Final position
	 * @param rotation Final rotation, used only by 
	 * TransitionEntityChildCommands
	 * @param scale Final scale, used only by 
	 * TransitionEntityChildCommands
	 * @param isTransient Should the command be transient or not (True for
	 * transient, false otherwise
	 * @param startChildren Optional list of children for non transient
	 * command results in order to put children back after an undo
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return Resulting move command or null if unable to create it
	 */
	private static Command generateMoveCommand(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			Entity parent,
			Entity pickParent,
			double[] position,
			float[] rotation,
			float[] scale,
			boolean isTransient,
			ArrayList<Entity> startChildren,
			boolean bypassRules) {
		
		Command command;
		int transactionID = model.issueTransactionID();
		
		// 1) Issue MoveEntityTransientCommand case - no pick parent
		// 2) Issue MoveEntityTransientCommand case - with pick parent
		// 3) Issue MoveEntityCommand case - no start children
		// 4) Issue MoveEntityCommand case - with start children
		// 5) Issue standard TransitionEntityChildCommand case
		// 6) Issue scale changing TransitionEntityChildCommand case
		// 7) Issue child changing TransitionEntityChildCommand case
		if (isTransient && parent == null && pickParent == null) {
			
			command = new MoveEntityTransientCommand(
					model, 
					transactionID, 
					entity.getEntityID(), 
					position, 
					new float[] {0.0f, 0.0f, 0.0f});
			
		} else if (isTransient && parent == null && pickParent != null) {
			
			command = new MoveEntityTransientCommand(
					model, 
					transactionID, 
					entity.getEntityID(), 
					position, 
					new float[] {0.0f, 0.0f, 0.0f}, 
					pickParent);
			
		} else if (!isTransient && parent == null && startChildren == null) {
			
			double[] startPosition = new double[3];
			entity.getStartingPosition(startPosition);
			
			command = new MoveEntityCommand(
					model, 
					transactionID, 
					(PositionableEntity)entity, 
					position, 
					startPosition);
			
		} else if (!isTransient && parent == null && startChildren != null) {
			
			double[] startPosition = new double[3];
			entity.getStartingPosition(startPosition);
			
			command = new MoveEntityCommand(
					model, 
					transactionID, 
					(PositionableEntity)entity, 
					position, 
					startPosition, 
					startChildren);
			
		} else if (parent != null && 
				startChildren == null && scale == null) {
			
			Entity startParent = 
				model.getEntity(entity.getParentEntityID());
			
			if (startParent == null) {
				return null;
			}
			
			double[] startPosition = new double[3];
			float[] startRotation = new float[4];
			
			entity.getStartingPosition(startPosition);
			entity.getStartingRotation(startRotation);
			
			command = new TransitionEntityChildCommand(
					model, 
					entity, 
					startParent, 
					startPosition, 
					startRotation, 
					parent, 
					position, 
					rotation, 
					isTransient);
			
		} else if (parent != null && 
				startChildren == null && scale != null) {
			
			Entity startParent = 
				model.getEntity(entity.getParentEntityID());
			
			if (startParent == null) {
				return null;
			}
			
			double[] startPosition = new double[3];
			float[] startRotation = new float[4];
			float[] startScale = new float[3];
			
			entity.getStartingPosition(startPosition);
			entity.getStartingRotation(startRotation);
			entity.getStartingScale(startScale);
			
			command = new TransitionEntityChildCommand(
					model, 
					entity, 
					startParent, 
					startPosition, 
					startRotation, 
					startScale, 
					parent, 
					position, 
					rotation, 
					scale, 
					isTransient);
			
		} else if (parent != null && 
				startChildren != null && scale == null) {
			
			Entity startParent = 
				model.getEntity(entity.getParentEntityID());
			
			if (startParent == null) {
				return null;
			}
			
			double[] startPosition = new double[3];
			float[] startRotation = new float[4];
			
			entity.getStartingPosition(startPosition);
			entity.getStartingRotation(startRotation);
			
			command = new TransitionEntityChildCommand(
					model, 
					entity, 
					startParent, 
					startPosition, 
					startRotation, 
					parent, 
					position, 
					rotation, 
					isTransient, 
					startChildren);
			
		} else {
			
			ErrorReporter errorReporter = 
				DefaultErrorReporter.getDefaultReporter();
			errorReporter.debugReport(
					"** SceneManagementUtility : generateMoveCommand() "+
					"Unable to match command based on parameters given.", 
					null);
			Thread.dumpStack();
			return null;
			
		}
		
		((RuleBypassFlag)command).setBypassRules(bypassRules);
		
		addSurrogate(collisionChecker, command);
		
		return command;
	}
	
	/**
	 * Principle implementation for removing entities from the scene.
	 * Creates the RemoveEntityChildCommand, adds it as a surrogate and
	 * then removes dead references to it in the command queues.
	 * 
	 * This is a non transient action.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to apply surrogate to
	 * @param child Child to remove from parent
	 * @param parent Parent to remove child from
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return RemoveEntityChildCommand command if required to update scene,
	 * null otherwise
	 */
    private static RemoveEntityChildCommand generateRemoveCommandAndCleanup(
    		WorldModel model,
    		RuleCollisionChecker collisionChecker, 
    		Entity parent,
    		Entity child,
    		boolean bypassRules) {
    	
    	if (!doesEntityExist(model, child)) {
    		return null;
    	}
    	
    	int transactionID = model.issueTransactionID();
    	
		RemoveEntityChildCommand rmvCmd = 
			new RemoveEntityChildCommand(model, parent, child, transactionID);
		
		rmvCmd.setBypassRules(bypassRules);
		
		addSurrogate(collisionChecker, rmvCmd);
		ExpertSceneManagementUtility.removedDeadCommands(
				model,
				collisionChecker, 
				child);
		
		// If the child isn't already in the scene, then we don't
		// need to issue command here. The cleanup done
		// in removeDeadCommands is sufficient so we
		// only issue the remove command if the entity is already
		// in the scene.
		if (model.getEntity(child.getEntityID()) == null) {
			return null;
		}
		
		// TODO: remove this once we refactor surrogates to be able to turn
		// off a branch at a specific node. That is the preferred way of doing
		// things.
		// Need to add a remove surrogate for all children so that they are not
		// seen in the scene either
		ArrayList<Entity> deadChildrenNeedingSurrogates = 
			SceneHierarchyUtility.getBranch(child);
		
		for (Entity deadChild : deadChildrenNeedingSurrogates) {
			
			Entity deadChildParent = 
				model.getEntity(deadChild.getParentEntityID());
			
			RemoveEntityChildCommand deadChildRmvCmd = 
				new RemoveEntityChildCommand(model, deadChildParent, deadChild);
			
			addSurrogate(collisionChecker, deadChildRmvCmd);
		}
		
		return rmvCmd;
    }
	
	/**
	 * Issue the appropriate scale command based on the isTransient value 
	 * passed in, and the existence of startChildren and startPositions values.
	 * If startChildren and startPositions values are passed in they are
	 * side pocketed so they can be set back correctly if the scale operation 
	 * is undone. Surrogate is added for command generated.
	 * 
	 * Note, transient scale commands do not side pocket child data. If 
	 * isTransient is set to true, the state of startChildren and 
	 * startPositions is ignored.
	 * 
	 * @param model WorldModel to reference
	 * @param collisionChecker RuleCollisionChecker to add surrogate to
	 * @param entity Entity to scale
	 * @param position New position of scaled entity [x,y,z]
	 * @param scale Scale amount [x,y,z]
	 * @param isTransient True to issue transient command, false to issue
	 * non transient command
	 * @param startChildren ArrayList of the entities children pre scale,
	 * can be null
	 * @param startPositions ArrayList of the PositionableData for each child,
	 * can be null pre scale
	 * @param bypassRules True to bypass rules, false to evaluate against rules
	 * @return Scale command created
	 */
	private static Command generateScaleCommand(
			WorldModel model,
			RuleCollisionChecker collisionChecker,
			PositionableEntity entity,
			double[] position,
			float[] scale,
			boolean isTransient,
			ArrayList<Entity> startChildren,
			ArrayList<PositionableData> startPositions,
			boolean bypassRules) {
		
		Command scaleCommand;
		
		int transactionID = model.issueTransactionID();
		
		if (isTransient) {
			
			scaleCommand = new ScaleEntityTransientCommand(
					model, 
					transactionID, 
					entity, 
					position, 
					scale);
			
		} else {
			
			double[] startPosition = new double[3];
			float[] startScale = new float[3];
			
			entity.getStartingPosition(startPosition);
			entity.getStartingScale(startScale);
			
			// Based on if startChildren and startPositions are included
			// issue a standard ScaleEntityCommand or the version that
			// side pockets child data
			if (startChildren == null || startPositions == null) {
				
				scaleCommand = new ScaleEntityCommand(
						model, 
						transactionID, 
						entity, 
						position, 
						startPosition, 
						scale, 
						startScale);
				
			} else {
				
				scaleCommand = new ScaleEntityCommand(
						model, 
						transactionID, 
						entity, 
						position, 
						startPosition, 
						scale, 
						startScale, 
						startChildren, 
						startPositions);
			}
		}
		
		((RuleBypassFlag)scaleCommand).setBypassRules(bypassRules);
		
		addSurrogate(collisionChecker, scaleCommand);
		
		return scaleCommand;
	}
	
	/**
	 * Issue a SelectEntityCommand.
	 * 
	 * @param model WorldModel to reference
	 * @param entity Entity to select
	 * @param selected True to select entity, false to deselect
	 * @return SelectEntityCommand generated
	 */
	private static SelectEntityCommand generateSelectCommand(
			WorldModel model,
			Entity entity,
			boolean selected) {
		
		SelectEntityCommand selectCmd = 
			new SelectEntityCommand(model, entity, selected);
		
		return selectCmd;
	}
	
	/**
	 * Copy auto add properties that get set during evaluation.
	 * @param sourceEntity Entity to copy properties from
	 * @param targetEntity Entity to copy properties to
	 * @param initialAddParentEntityID parent entity id to set
	 * @return True if successful, false otherwise
	 */
	private static boolean copyAutoAddData(
			Entity sourceEntity, 
			Entity targetEntity,
			int initialAddParentEntityID) {
		
		boolean isAutoAdd = (Boolean) 
			RulePropertyAccessor.getRulePropertyValue(
				sourceEntity, 
				ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
		
		if (!isAutoAdd) {
			return false;
		}
		
		// Copy over the properties, update where needed
		// Note, we don't copy over any auto add rules that dictate what or how
		// auto adds are generated. We only copy over the properties that are
		// set during evaluation of auto add products.
		
		// Handle PERMANENT_PARENT_SET
		boolean permParentSet = 
			(Boolean) RulePropertyAccessor.getRulePropertyValue(
					sourceEntity, 
					ChefX3DRuleProperties.PERMANENT_PARENT_SET);
		
		RulePropertyAccessor.setRuleProperty(
				targetEntity, 
				ChefX3DRuleProperties.PERMANENT_PARENT_SET, 
				permParentSet);

		// Handle INITIAL_ADD_PARENT		
		RulePropertyAccessor.setRuleProperty(
				targetEntity, 
				ChefX3DRuleProperties.INITAL_ADD_PARENT, 
				initialAddParentEntityID);
		
		return true;
	}
	
	/**
	 * Loop through the existing commands in the queue and remove any commands
	 * that match the command type of the command passed in and are acting on
	 * the same entity.
	 * 
	 * @param command Command to check against
	 */
	private static void removeOldCommands(Command command) {
		
		if (!(command instanceof RuleDataAccessor)) {
			return;
		}
		
		Entity entity = ((RuleDataAccessor)command).getEntity();
		
		// See if the entity already has a scale command associated with it
		// in the queue. If so, cancel it and replace it with the new one.
		ArrayList<Command> commands = (ArrayList<Command>) 
			CommandSequencer.getInstance().getFullCommandList(false);
		
		Command tmpCmd = null;
		Entity tmpEntity = null;
		
		for (int i = (commands.size() - 1); i>= 0; i--) {
			
			tmpCmd = commands.get(i);
			
			if (!(tmpCmd instanceof RuleDataAccessor)) {
				continue;
			}
			
			tmpEntity = ((RuleDataAccessor)tmpCmd).getEntity(); 
			
			if (tmpCmd.getClass() == command.getClass() &&
					entity == tmpEntity) {
				
				CommandSequencer.getInstance().removeCommand(
						tmpCmd.getTransactionID());
			}
		}
	}
	
	/**
	 * Check if the entity exists in the scene. Either currently or will be
	 * after the next frame renders.
	 * 
	 * @param model WorldModel to reference
	 * @param entity Entity to check for
	 * @return True if it is in the scene or will be, false otherwise
	 */
	public static boolean doesEntityExist(WorldModel model, Entity entity) {
		
		ArrayList<Command> cmdList = (ArrayList<Command>)
			CommandSequencer.getInstance().getFullCommandList(true);
		
		Entity cmdEntity = null;
		
		// See if there are any commands in the queue that affect the existence
		// of the entity in question. If so, respond accordingly.
		for (Command cmd : cmdList) {
			
			// 1) Look for remove commands to see if entity is a descendant of 
			// any entity being removed. If so, return false.
			// 2) Look for add commands to see if the entity is not yet in the 
			// scene but will be. If so, return true.
			if (cmd instanceof RemoveEntityChildCommand) {
				
				cmdEntity = ((RemoveEntityChildCommand)cmd).getEntity();
				
				if (cmdEntity == entity) {
					return false;
				} else if (SceneHierarchyUtility.isEntityChildOfParent(
						model, entity, cmdEntity, true)) {
					return false;
				}
				
			} else if (cmd instanceof AddEntityChildCommand) {
				
				cmdEntity = ((AddEntityChildCommand)cmd).getEntity();
				
				if (cmdEntity == entity) {
					return true;
				}
				
			} else if (cmd instanceof AddEntityChildTransientCommand) {
				
				cmdEntity = ((AddEntityChildTransientCommand)cmd).getEntity();
				
				if (cmdEntity == entity) {
					return true;
				}
				
			} else {
				continue;
			}
		}
		
		// There aren't any commands affecting the entity that would change
		// its existence in the scene, so we neeed to see if it exists in the
		// scene at all. If we get back, null it is not in the scene and thus
		// does not exist.
		if (model.getEntity(entity.getEntityID()) == null) {
			return false;
		}
		
		// It is in the scene and not affected by any commands that would change
		// that so return true, that it is in the scene.
		return true;
	}
	
}
