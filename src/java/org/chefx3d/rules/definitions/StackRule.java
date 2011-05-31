/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.definitions;

//External Imports
import java.util.List;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.av3d.AV3DConstants;

import org.chefx3d.view.common.EditorView;

/**
* Stacks entities that have classifications matching the stack rule.
*
* @author Ben Yarger
* @version $Revision: 1.42 $
*/
public class StackRule extends BaseRule  {

    /** The overlap percentage to use */
    private static float MINIMUM_OVERLAP_PERCENTAGE = 0.1f;

	/** Vertical stack adjustment value */
	private static float STACK_ADJUSTMENT = 0.001f;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public StackRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

    /**
     * Perform the rule check
     *
     * @param entity Entity object
     * @param command Command object
     * @param result The state of the rule processing
     * @return boolean True if rule passes, false otherwise
     */
    protected RuleEvaluationResult performCheck(
            Entity entity,
            Command command,
            RuleEvaluationResult result) {

        this.result = result;

		if(validateCommand(command)){

			String[] stackClassifications = (String[])
				RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.STACK_PROP);

			if(stackClassifications != null && stackClassifications.length > 0){

			    double[] pos = new double[3];
			    double[] origPos = new double[3];
			    Entity parentZone = null;
			    Entity origParent = null;
			    
				if(command instanceof TransitionEntityChildCommand){

                   ((TransitionEntityChildCommand)command).getEndPosition(origPos);

                    // get the bounds of the item being top be stacked on
                    float[] bounds = new float[6];
                    ((PositionableEntity)entity).getBounds(bounds);

                    // adjust the item
                    pos[0] = origPos[0];
                    pos[1] = Math.abs(bounds[2]) - STACK_ADJUSTMENT;
                    pos[2] = Math.abs(bounds[4]) - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

                    ((TransitionEntityChildCommand)command).setEndPosition(pos);
                    parentZone = SceneHierarchyUtility.findZoneEntity(model,
                            ((TransitionEntityChildCommand)command).getEndParentEntity());
				} else if (command instanceof MoveEntityTransientCommand){

					((MoveEntityTransientCommand)command).getPosition(origPos);

				    // get the bounds of the item being top be stacked on
					float[] bounds = new float[6];
			        ((PositionableEntity)entity).getBounds(bounds);

			        // adjust the item
                    pos[0] = origPos[0];
                    pos[1] = Math.abs(bounds[2]) - STACK_ADJUSTMENT;
                    pos[2] = Math.abs(bounds[4]) - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

					((MoveEntityTransientCommand)command).setPosition(pos);
					parentZone = SceneHierarchyUtility.findZoneEntity(model, entity);

                } else if (command instanceof MoveEntityCommand) {

                    ((MoveEntityCommand)command).getEndPosition(origPos);

                    // get the bounds of the item being top be stacked on
                    float[] bounds = new float[6];
                    ((PositionableEntity)entity).getBounds(bounds);

                    // adjust the item
                    pos[0] = origPos[0];
                    pos[1] = Math.abs(bounds[2]) - STACK_ADJUSTMENT;
                    pos[2] = Math.abs(bounds[4]) - AV3DConstants.DEFAULT_EMBEDDING_DEPTH;

                    ((MoveEntityCommand)command).setEndPosition(pos);
                    parentZone = SceneHierarchyUtility.findZoneEntity(model, entity);

				} else if (command instanceof AddEntityChildCommand) {

				    // get the position is zone coords by looking at the 
				    // start position data				    
				    ((PositionableEntity)entity).getStartingPosition(pos);
                    ((PositionableEntity)entity).getPosition(origPos);
                    
                    // get the original parent
                    origParent = 
                        ((AddEntityChildCommand)command).getParentEntity();

                    // adjust the parent to be the zone
                    parentZone = 
                        SceneHierarchyUtility.findExactZoneEntity(
                                model,
                                origParent);
                    
                    if (parentZone == null) {
                        parentZone = 
                            SceneHierarchyUtility.getActiveZoneEntity(model);
                    }               

                    // if they are all zero then assume this is a load
                    if (origPos[0] == 0 && origPos[1] == 0 && origPos[2] == 0) {
                        
                        ((PositionableEntity)entity).setPosition(pos, false);
                        
                    } else {
                        
                        if (pos[0] == 0 && pos[1] == 0 && pos[2] == 0) {
                            // do nothing
                        } else {
                            // adjust the position to be zone coordinates
                            ((PositionableEntity)entity).setPosition(pos, false);
                            ((AddEntityChildCommand)command).setParentEntity(parentZone);
                            
                            if (parentZone == null) {
                                System.out.println("foo");
                            }
                        }
                        
                    }
                    

				}


				/*
				 * Perform collision check - this rule should be performed
				 * early on so usually will require collision check.
				 */
				rch.performCollisionCheck(command, true, false, false);

				if(command instanceof TransitionEntityChildCommand){

					((TransitionEntityChildCommand)command).setEndPosition(origPos);

                } else if (command instanceof MoveEntityCommand){

                    ((MoveEntityCommand)command).setEndPosition(origPos);

                } else if (command instanceof MoveEntityTransientCommand){

                    ((MoveEntityTransientCommand)command).setPosition(origPos);

                } else if (command instanceof AddEntityChildCommand){

                    ((AddEntityChildCommand)command).setParentEntity(origParent);                   
                    ((PositionableEntity)entity).setPosition(origPos, false);

				}

				/*
				 * Look for a match to stack on
				 */
				if(rch.collisionEntities != null){

					for(Entity tmpEntity : rch.collisionEntities){

						String[] tmpClassifications = (String[])
							RulePropertyAccessor.getRulePropertyValue(
								tmpEntity,
								ChefX3DRuleProperties.CLASSIFICATION_PROP);

						if (tmpClassifications != null) {

    						for(int i = 0; i < stackClassifications.length; i++){

    							for(int j = 0; j < tmpClassifications.length; j++){

    								if(stackClassifications[i].equalsIgnoreCase(
    										tmpClassifications[j])){

    								    boolean isValid = true;

    								    // check to make sure the collision is not a child
    								    List<Entity> children =
    							            SceneHierarchyUtility.getExactChildren(entity);

    								    int len = children.size();
    								    for(int k = 0; k < len; k++) {
    								        if (children.get(k) == tmpEntity) {
    								            isValid = false;
    								        }
    								    }

    								    // check to make sure the two items are in the same zone
                                        Entity stackOnZone = SceneHierarchyUtility.findZoneEntity(model, tmpEntity);
    								    if (stackOnZone != parentZone) {
    								        isValid = false;
    								    }

    								    // this is a valid stack, so do it
    								    if (isValid) {
    								        isValid = stackEntities(
    								                model, 
    								                command, 
    								                (PositionableEntity)entity, 
    								                (PositionableEntity)tmpEntity);
                                            result.setResult(isValid);
                                            return(result);
    								    }
    								}
    							}
    						}
						}
					}
				}

		        ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES moveRestriction =
		            (ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES)
		            	RulePropertyAccessor.getRulePropertyValue(
		            		entity,
		                    ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_PLANE_PROP);

				if(moveRestriction != null){

					undoStackAdjustment(
							model,
							command,
							entity,
							moveRestriction);
				}
			}
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Validates the command against list of commands rule responds to.
	 *
	 * @param command Command passed to rule
	 * @return True if valid, false otherwise
	 */
	private boolean validateCommand(Command command){

		if(command instanceof MoveEntityTransientCommand ||
                command instanceof MoveEntityCommand ||
                command instanceof AddEntityChildCommand ||
				command instanceof TransitionEntityChildCommand){

			return true;
		}

		return false;
	}

	/**
	 * Performing stack position adjustment
	 *
	 * @param model WorldModel
	 * @param command Command that was issued
	 * @param toStack Entity to stack
	 * @param stackOn Entity to stack on top of
	 */
	private boolean stackEntities(
			WorldModel model,
			Command command,
			PositionableEntity toStack,
			PositionableEntity stackOn){

        double[] stackOnPos = new double[3];
	    float[] stackOnBounds = new float[6];
	    float[] stackOnGroupBounds = new float[6];

	    // TODO: There are several nested traversals to get the
	    // information needed.  It would be nice to incorporate
	    // them all into a single traversal.

	    // first determine the actual stackOn entity
	    Entity stackOnTop = getStackOnEntity(stackOn, toStack);
	    
	    // This is done here so that all the properties needed to be retrieved
	    // by stack on top is only done once.
	    if(command instanceof TransitionEntityChildCommand) {
	        Entity entity = ((TransitionEntityChildCommand) command).getEntity();
	        if(entity == stackOnTop) {
	            stackOnTop =
	                ((TransitionEntityChildCommand)command).getStartParentEntity();
	        }
	    }
	    
//System.out.println("");
//System.out.println("**************** STACK ENTITIES ****************");
//System.out.println("command: " + command);
//System.out.println("toStack (item to stack): " + toStack);
//System.out.println("stackOnTop (item to stack on top of, can be anywhere in the stack though): " + stackOnTop);

        // next, we need to get the base stack item, this is used
        // for the x-axis position data
        Entity stackRootEntity = getRootStackEntity(model, stackOn);

        Entity stackRootEntityZone =
        	SceneHierarchyUtility.findZoneEntity(model, stackRootEntity);

        if (stackRootEntityZone != null &&
        		stackRootEntityZone.getType() == Entity.TYPE_SEGMENT) {
        	stackOnPos = TransformUtils.getPositionRelativeToZone(model, stackRootEntity);
        } else {
        	((PositionableEntity)stackRootEntity).getPosition(stackOnPos);
        }

		// make sure not to try to stack itself
//		if (toStack == stackRootEntity) {
//		    return true;
//		}

//System.out.println("stackRootEntity (the base of the stack): " + stackRootEntity);

		// get the bounds of the item being top be stacked on
		((PositionableEntity)stackOnTop).getBounds(stackOnBounds);


//System.out.println("stackOnBounds (bounds of the item being stack on): " + java.util.Arrays.toString(stackOnBounds));

		// get the bounds of include all items in the stack
		stackOnGroupBounds = getStackOnBounds(model, stackOnTop, stackOnBounds);

//System.out.println("stackOnGroupBounds (bounds of the entire stack): " + java.util.Arrays.toString(stackOnGroupBounds));

		// figure out how many items are stacked so we can correctly
		// multiple the stack adjustment constant
        int adjustments = 0;
        adjustments = getStackCount(model, stackOnTop, adjustments);

//System.out.println("adjustments (the number of items in the stack): " + adjustments);

		// get the bounds of the item being stacked
        float[] toStackBounds = new float[6];
        ((PositionableEntity)toStack).getBounds(toStackBounds);

        // the position to use for the various commands
        double[] toStackPos = new double[3];
        	toStackPos = TransformUtils.getExactPosition(
        		(PositionableEntity)toStack);

        // check to make sure we have overlapped enough
        boolean validOverlap = true;
        if (command instanceof MoveEntityTransientCommand ||
                command instanceof MoveEntityCommand) {
            
            validOverlap = checkStackOverlap(
		            stackOnPos,
		            stackOnBounds,
		            toStackPos,
		            toStackBounds);
            
            if (!validOverlap) {
                return true;
            }
        }
        

//System.out.println("toStackBounds (bounds of the item being stacked): " + java.util.Arrays.toString(toStackBounds));

        /*
		 * Command specific details
		 */
		if (command instanceof TransitionEntityChildCommand){

	        // movement is relative to the parent entity and should
		    // only be adjusted by it's on bounds

		    // triggered when an item is already in the scene and is
		    // being dragged.  the mouse release fires this command

//System.out.println("TransitionEntityChildCommand");

			if(!command.isTransient()){

				toStackPos[0] = 0.0;
	            toStackPos[1] =
	                stackOnBounds[3] + Math.abs(toStackBounds[2]) - STACK_ADJUSTMENT;
				toStackPos[2] = 0.0;

				((TransitionEntityChildCommand)command).setEndPosition(toStackPos);
				((TransitionEntityChildCommand)command).setEndParentEntity(stackOnTop);

			}

        } else if (command instanceof MoveEntityTransientCommand){

            // movement is relative to the zone and should be adjusted
            // by the entire stack group

//System.out.println("MoveEntityTransientCommand");
                
            // the height of the group
            float groupHeight =
                (Math.abs(stackOnGroupBounds[2]) + stackOnGroupBounds[3]) -
                (STACK_ADJUSTMENT * adjustments);

            toStackPos[0] = stackOnPos[0];
            toStackPos[1] =
                groupHeight + Math.abs(toStackBounds[2]) - STACK_ADJUSTMENT;
            toStackPos[2] = stackOnPos[2];

            ((MoveEntityTransientCommand)command).setPosition(toStackPos);

        } else if (command instanceof MoveEntityCommand){

            // there are two ways to reach this case:
            // 1. a nudge has been fired.  the potential parent and 
            // current part will match, so leave it exactly where it is.
            // 2. a move was done in a way that the mouse was not over the 
            // potential parent.  we need to issue a transition entity command
            // to re-parent correctly, then let that new command adjust the 
            // position correctly
 
            // compare the stackOnTop to the current parent
            if (stackOnTop.getEntityID() != toStack.getParentEntityID()) {
                
//System.out.println("MoveEntityCommand");

                // get the current position
                ((MoveEntityCommand)command).getEndPosition(toStackPos);
                
                // need to zero out the non-stack axis
                toStackPos[0] = 0;
                toStackPos[2] = 0;
                               
                // get the current rotation
                float[] rotation = new float[4];
                toStack.getRotation(rotation);

                SceneManagementUtility.moveEntityChangeParent(
                        model, 
                        collisionChecker, 
                        toStack, 
                        stackOnTop, 
                        toStackPos, 
                        rotation, 
                        false, 
                        false);
                
                // stop the move command from processing, but allow the newly 
                // issued command to get processed.
                result.setResult(false);
                result.setApproved(false);
                result.setNotApprovedAction(
                        RuleEvaluationResult.NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);
              
            }
           
        } else if (command instanceof AddEntityChildCommand) {

	        // placement is relative to the parent entity and should
            // only be adjusted by it's on bounds

//System.out.println("AddEntityChildCommand");

			toStackPos[0] = 0.0;
			toStackPos[1] =
			    stackOnBounds[3] + Math.abs(toStackBounds[2]) - STACK_ADJUSTMENT;
			toStackPos[2] = 0.0;

			((AddEntityChildCommand)command).setParentEntity(stackOnTop);

			((PositionableEntity)toStack).setPosition(
					toStackPos,
					command.isTransient());

		}

		return true;

//System.out.println("");

	}

	/**
	 * Undo stacking with stacking collision doesn't exist and
	 * move plane constraint is in place.
	 *
	 * @param model
	 * @param command
	 * @param toStack
	 */
	private void undoStackAdjustment(
			WorldModel model,
			Command command,
			Entity toStack,
			ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES planeRes){

		double[] origPos = new double[3];
		double[] toStackPos = new double[3];
		float[] toStackBounds = new float[6];
		float[] toStackRot = new float[4];

		((PositionableEntity)toStack).getBounds(toStackBounds);
		((PositionableEntity)toStack).getRotation(toStackRot);
		((PositionableEntity)toStack).getStartingPosition(origPos);

		switch(planeRes){

			case XZPLANE:
				toStackPos[1] = Math.abs(toStackBounds[2]);
//				toStackPos[2] = Math.abs(toStackBounds[4]);
				break;
			case YZPLANE:
			case XYPLANE:
			case NONE:
				return;
		}

		/*
		 * Command specific details
		 */
        if (command instanceof MoveEntityTransientCommand){

            double[] pos = new double[3];
            ((MoveEntityTransientCommand)command).getPosition(pos);

            pos[1] = toStackPos[1];
//          pos[2] = toStackPos[2];

            ((MoveEntityTransientCommand)command).setPosition(pos);

        } else if (command instanceof MoveEntityCommand){

                double[] pos = new double[3];
                ((MoveEntityCommand)command).getPosition(pos);

                pos[1] = toStackPos[1];
//              pos[2] = toStackPos[2];

                ((MoveEntityCommand)command).setPosition(pos);

		} else if (command instanceof TransitionEntityChildCommand){

			double[] pos = new double[3];
			((TransitionEntityChildCommand)command).getEndPosition(pos);

            Entity newParent =
                ((TransitionEntityChildCommand)command).getEndParentEntity();

            double[] newParentZonePos =
            	TransformUtils.getPositionRelativeToZone(model, newParent);

            pos[1] = Math.abs(toStackBounds[2]) - newParentZonePos[1];

			((TransitionEntityChildCommand)command).setEndPosition(pos);

        }

	}

	/**
	 * Find the leaf entity, that is who we should stack on.
	 *
	 * @param entity The place in the hierarchy to check from
	 * @return the entity to stack on to.
	 */
	private Entity getStackOnEntity(Entity entity, Entity toStack) {

	    List<Entity> children =
	        SceneHierarchyUtility.getExactChildren(entity);

	    int len = children.size();
	    if (len > 0) {

	        for (int i = 0; i < len; i++) {
	            Entity child = children.get(i);
	            boolean match = entityMatch(entity, child);
	            if (match) {
	                
	                if (child == toStack) {
	                    return entity;
	                } else {	                
	                    return getStackOnEntity(child, toStack);
	                }
	            }
	        }

	    }

	    return entity;

	}

	/**
     * Get the total bounds of the stack.
     *
     * @param model The world data model, used to lookup entities
     * @param entity The place in the hierarchy to check from
     * @param bounds The total bounds so far
     * @return The total bounds modified by the current entity
     */
    private float[] getStackOnBounds(
            WorldModel model,
            Entity entity,
            float[] bounds) {

        float[] retVal = new float[6];

        // get the parent
        int parentID = entity.getParentEntityID();
        Entity parentEntity = model.getEntity(parentID);

        boolean match = entityMatch(entity, parentEntity);
        if (match) {
            ((PositionableEntity)entity).getBounds(bounds);
            retVal = getStackOnBounds(model, parentEntity, bounds);
        }

        retVal[2] += bounds[2];
        retVal[3] += bounds[3];

        return retVal;

    }

    /**
     * Find the root entity, the first descendant in the hierarchy
     *
     * @param model The world data model, used to lookup entities
     * @param entity The place in the hierarchy to check from
     * @return the root entity.
     */
    private Entity getRootStackEntity(
            WorldModel model,
            Entity entity) {

        Entity retVal = entity;

        // get the parent
        int parentID = entity.getParentEntityID();
        Entity parentEntity = model.getEntity(parentID);

        boolean match = entityMatch(entity, parentEntity);
        if (match) {
            retVal = getRootStackEntity(model, parentEntity);
        }

        return retVal;

    }

    /**
     * Find the number of items in the stack.  Will recurse the parents until
     * the classifications do not match.
     *
     * @param model The world data model, used to lookup entities
     * @param entity The place in the hierarchy to check
     * @param count The count so far
     * @return The count after matching
     */
    private int getStackCount(
            WorldModel model,
            Entity entity,
            int count) {

        // get the parent
        int parentID = entity.getParentEntityID();
        Entity parentEntity = model.getEntity(parentID);

        boolean match = entityMatch(entity, parentEntity);
        if (match) {
            return getStackCount(model, parentEntity, count + 1);
        }

        return count;

    }

    /**
     * Do the two items share a common classification.  If they
     * match then stacking can occur.
     *
     * @param entity1 The first entity to check
     * @param entity2 The second entity to check
     * @return True if they are same classification, false otherwise
     */
	private boolean entityMatch(
            Entity entity1,
            Entity entity2) {

        boolean entityStackMatch = false;

        if(entity1 != null && entity2 != null){

            // get the first entity's list of stackable targets
            String[] stackClassifications = (String[])
            	RulePropertyAccessor.getRulePropertyValue(
            			entity1,
                        ChefX3DRuleProperties.STACK_PROP);

            // get the second entity's list of classifications
            String[] parentClassifications = (String[])
            	RulePropertyAccessor.getRulePropertyValue(
            			entity2,
            			ChefX3DRuleProperties.CLASSIFICATION_PROP);

            // if any of the targets matches a classifications then
            // this is a valid stack
            if (stackClassifications != null &&
                    parentClassifications != null) {

                for(int i = 0; i < stackClassifications.length; i++){

                    for(int w = 0; w < parentClassifications.length; w++){

                        if(stackClassifications[i].equalsIgnoreCase(
                                parentClassifications[w])){

                            entityStackMatch = true;
                            break;
                        }
                    }

                    if(entityStackMatch){
                        break;
                    }
                }
            }
        }

        return entityStackMatch;
	}

	/**
	 * Make sure the two products are overlapped enough
	 *
	 * @param stackOnGroupBounds
	 * @param toStackBounds
	 * @return
	 */
	private boolean checkStackOverlap(
	        double[] stackOnPos,
	        float[] stackOnBounds,
	        double[] toStackPos,
	        float[] toStackBounds) {

	    // first get the distance value to check
	    float width = stackOnBounds[1] - stackOnBounds[0];
	    float overlap = width - (width * MINIMUM_OVERLAP_PERCENTAGE);
	    float distance = (float)Math.abs(toStackPos[0] - stackOnPos[0]);

	    if (distance <= overlap) {
	        return true;
	    }

	    return false;

	}

}
