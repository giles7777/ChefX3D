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

//External imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Local imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.view.awt.av3d.AV3DConstants;

/**
 * Utility methods for traversing and accessing various entities in the scene
 * hierarchy.
 *
 * @author Ben Yarger
 * @version $Revision: 1.28 $
 */
public abstract class SceneHierarchyUtility {

	/**
     * Used by the RulePositionUtils.isWallAtSpecificAngle method to find the 
     * adjacent wall.
     * 
     * @param segmentable The segmentable Entity
     * @param vertex Either the start or end vertex.
     * @param start A boolean flag to determine if the method should check
     * the segments start vertex or end vertex.
     * @return The segment entity adjacent to the current wall or null if not 
     * found
     */
    public static SegmentEntity findAdjacentWall(
    		SegmentableEntity segmentable, 
    		VertexEntity vertex, 
    		boolean start) {

        ArrayList<SegmentEntity> completeSegmentList = 
        	segmentable.getSegments();
        
        int vertexID = vertex.getEntityID();

        for (SegmentEntity segment: completeSegmentList) {

            if (start) {

                if (segment.getStartVertexEntity().getEntityID() == vertexID) {
                    return segment;
                }

            } else {
                if (segment.getEndVertexEntity().getEntityID() == vertexID) {
                    return segment;
                }
            }

        }

        return null;
    }
    
    /**
     * Finds the zone of an entity, if it doesn't have one it will eventually 
     * return null. This search does not consider any commands that are part
     * of the current evaluation. This will give a result that is guaranteed
     * correct for the previous frame only.
     * 
     * @param entity Entity of which you want to know the parent zone
     * @return the parent Zone (or Segment), or null if none exist
     */
    public static ZoneEntity findZoneEntity(
    		WorldModel worldModel, Entity entity) {
        
        if(entity == null || entity.isZone())
           return (ZoneEntity) entity;
        
        // Check for initial parent ID if this is an add instance that
        // is not yet in the scene. This is identified by the parent entity ID 
        // being equal to -1. In this case, look for side pocketed parent ID.
        int parentEntityID = entity.getParentEntityID();
        
        if (parentEntityID == -1) {
        	
        	Integer tmpParentEntityID = (Integer)
        		RulePropertyAccessor.getRulePropertyValue(
        				entity, 
        				ChefX3DRuleProperties.INITAL_ADD_PARENT);
        	
        	if (tmpParentEntityID != null) {
        		parentEntityID = tmpParentEntityID;
        	}
        }
        
        return findZoneEntity( worldModel,
        	worldModel.getEntity(parentEntityID));
    }
    
    /**
     * Finds the zone of an entity, if it doesn't have one it will eventually 
     * return null. This search considers all commands that are part of the 
     * current evaluation. The result is guaranteed to be correct for the 
     * current frame.
     * 
     * @param entity Entity of which you want to know the parent zone
     * @return the parent Zone (or Segment), or null if none exist
     */
    public static ZoneEntity findExactZoneEntity(
    		WorldModel model, Entity entity) {
    	
    	if(entity == null || entity.isZone())
           return (ZoneEntity) entity;
        
        return findZoneEntity(
        		model,
        		getExactParent(model, entity));
    }
    
    /**
     * Determines the number of steps from the entity to its zone. This search 
     * does not consider any commands that are part of the current evaluation. 
     * This will give a result that is guaranteed correct for the previous frame
     *  only.
     *
     * @param model WorldModel to look up entities by ID
     * @param entity Entity to start search with
     * @return The number of steps from the entity to its parent zone
     */
    public static int getToZoneCount(WorldModel model, Entity entity){

        int count = 0;
        if (entity instanceof SegmentEntity ||
                entity instanceof ZoneEntity) {

            return count;
        }
        count++;
        int parentEntityID = entity.getParentEntityID();
        Entity parentEntity = model.getEntity(parentEntityID);

        while(!(parentEntity instanceof ZoneEntity)){

            if(parentEntity instanceof Entity){
                parentEntityID = parentEntity.getParentEntityID();
                parentEntity = model.getEntity(parentEntityID);
                count++;
            } else {
                return 0;
            }
        }

        return count;
     }
     
    /**
     * Get the active zone entity for the given world model.
     * 
     * @param model WorldModel to reference
     * @return Active zone entity, or null if not found
     */
	public static Entity getActiveZoneEntity(WorldModel model) {
		
		Entity zoneEntity = null;
		
		SceneEntity sceneEntity = (SceneEntity)model.getSceneEntity();
		if (sceneEntity != null) {
			LocationEntity location = sceneEntity.getActiveLocationEntity();
			
			if (location != null) {
				int activeZoneID = location.getActiveZoneID();
				zoneEntity = model.getEntity(activeZoneID);
			}
		}
		return(zoneEntity);
	}
     
     /**
      * Get the first wall or floor parent entity encountered. This search 
      * considers all commands that are part of the current evaluation. 
      * The result is guaranteed to be correct for the current frame.
      * 
      * @param model WorldModel to reference
      * @param entity Entity to get wall or floor parent from
      * @return Wall or floor parent entity, null if cannot locate
      */
     public static Entity getWallOrFloorParent(WorldModel model, Entity entity) {
     	
     	 if (entity == null) {
             return null;
         }

         if (entity.getType() == Entity.TYPE_SEGMENT || 
         		entity.getType() == Entity.TYPE_GROUNDPLANE_ZONE) {

             return entity;
         }
        
         Entity parentEntity = getExactParent(model, entity);

         while(parentEntity.getType() != Entity.TYPE_SEGMENT &&
         		parentEntity.getType() != Entity.TYPE_GROUNDPLANE_ZONE){


         	if (!parentEntity.isModel() && !parentEntity.isZone()) {
         		return null;
         	}
                 
             parentEntity = getExactParent(model, parentEntity);
                 
         }
         
         return parentEntity;
     }
     
     /**
      * Get the parent entity of the entity. This search does not consider any 
      * commands that are part of the current evaluation. This will give a 
      * result that is guaranteed correct for the previous frame only.
      * 
      * @param model World model to reference
      * @param entity Entity to get parent for
      * @return Parent entity, or null if cannot be found
      */
     public static Entity getParent(WorldModel model, Entity entity) {
    	 
    	 int parentEntityID = entity.getParentEntityID();

 		// Check for initial parent ID if this is an add instance that
 		// is not yet in the scene. This is identified by the parent entity ID
 		// being equal to -1. In this case, look for side pocketed parent ID.
 		if (parentEntityID == -1) {
 		
 		    Integer tmpParentEntityID = (Integer)
 		        RulePropertyAccessor.getRulePropertyValue(
 		                entity,
 		                ChefX3DRuleProperties.INITAL_ADD_PARENT);
 		
 		    if (tmpParentEntityID != null) {
 		        parentEntityID = tmpParentEntityID;
 		    } else {
 		    	return null;
 		    }
 		}
 		 
 		return model.getEntity(parentEntityID);
     }
     
     /**
      * Get the entity from the data model and include any commands currently
      * in the queue to see if the entity resides there. This is a better 
      * option to model.getEntity(int entityID) in that it looks at the queue
      * of commands related to the current evaluation to see if the entity
      * will exist after the current frame. This way we can retrieve entities 
      * that aren't yet in the scene but are scheduled to be added.
      * 
      * @param model WorldModel to reference.
      * @param entityID Entity ID to retrieve.
      * @return Entity matching ID or null if not found.
      */
     public static Entity getEntity(
    		 WorldModel model, 
    		 int entityID) {
    	 
    	 Entity entity = model.getEntity(entityID);
    	 
    	 // Get all of the commands on the queues
    	 ArrayList<Command> cmdList = (ArrayList<Command>) 
    	 	CommandSequencer.getInstance().getFullCommandList(true);
      	
      	 // By starting at the end of the list and working backwards
      	 // we are guaranteed to get the last command
      	 // issued affecting the entity.
      	 for (int i = (cmdList.size() - 1); i >= 0; i--) {
      		
      		 Command cmd = cmdList.get(i);
      		
      		 if (cmd instanceof RuleDataAccessor) {
      			
      		     if (((RuleDataAccessor)cmd).getEntity() == null) {
      		         continue;
      		     }
      		     
      		 	 // If it isn't an entity match, skip to next command
      		 	 if (((RuleDataAccessor)cmd).getEntity().getEntityID() == 
      		 			entityID) {
      				 entity = ((RuleDataAccessor)cmd).getEntity();
      				 break;
      			 }
      		 }   
      	 }
      	
      	 return entity;
     }
     
     /**
      * Get the exact current parent of the entity by evaluating the entity 
      * first and then evaluating the command queues to see if anything there
      * changes the parenting of the entity. This search 
      * considers all commands that are part of the current evaluation. 
      * The result is guaranteed to be correct for the current frame.
      * 
      * @param model WorldModel to reference
      * @param entity Entity to find parent for
      * @return True current parent entity, or null if none found
      */
     public static Entity getExactParent(WorldModel model, Entity entity) {
     	
     	int parentEntityID = entity.getParentEntityID();
     	
		// Check for initial parent ID if this is an add instance that
		// is not yet in the scene. This is identified by the parent entity ID
		// being equal to -1. In this case, look for side pocketed parent ID.
		if (parentEntityID == -1) {
		
		    Integer tmpParentEntityID = (Integer)
		        RulePropertyAccessor.getRulePropertyValue(
		                entity,
		                ChefX3DRuleProperties.INITAL_ADD_PARENT);
		
		    if (tmpParentEntityID != null) {
		        parentEntityID = tmpParentEntityID;
		    }
		}
		 
		Entity parentEntity = getEntity(model, parentEntityID);

		// Get all of the commands on the queues
     	ArrayList<Command> cmdList = (ArrayList<Command>) 
     		CommandSequencer.getInstance().getFullCommandList(true);
     	
     	// By starting at the end of the list and working backwards
     	// we are guaranteed to get the last command
     	// issued affecting the entity.
     	
     	for (int i = (cmdList.size() - 1); i >= 0; i--) {
     		
     		Command cmd = cmdList.get(i);
     		
     		if (cmd instanceof RuleDataAccessor) {
     			
     			// If it isn't an entity match, skip to next command
     			if (((RuleDataAccessor)cmd).getEntity() != entity) {
     				continue;
     			}
     		
     			// Try and match the command to something we care about.
     			// Something that will adjust the parenting of the entity.
 	    		if (cmd instanceof AddEntityChildCommand) {
 	    			
 	    			parentEntity = 
 	    				((AddEntityChildCommand)cmd).getParentEntity();
 	    			break;
 	    			
 	    		} else if (cmd instanceof AddEntityChildTransientCommand) {
 	    			
 	    			parentEntity =
 	    				((AddEntityChildTransientCommand)
 	    						cmd).getParentEntity();
 	    			break;
 	    			
 	    		} else if (cmd instanceof TransitionEntityChildCommand) {
 	    			
 	    			parentEntity =
 	    				((TransitionEntityChildCommand)
 	    						cmd).getEndParentEntity();
 	    			break;
 	    			
 	    		} else if (cmd instanceof RemoveEntityChildCommand) {
 	    			
 	    			parentEntity =
 	    				((RemoveEntityChildCommand)cmd).getParentEntity();
 	    			break;
 	    			
 	    		} else if (cmd instanceof RemoveEntityChildTransientCommand) {
 	    			
 	    			parentEntity = 
 	    				((RemoveEntityChildTransientCommand)
 	    						cmd).getParentEntity();
 	    			break;
 	    		}
     		}   
     	}
     	
     	return parentEntity;
     }
     
     /**
      * Get the exact starting parent of the entity by evaluating the entity 
      * first and then evaluating the command queues to see if anything there
      * changes the parenting of the entity. This search 
      * considers all commands that are part of the current evaluation. 
      * The result is guaranteed to be correct for the current frame.
      * 
      * @param model WorldModel to reference
      * @param entity Entity to find parent for
      * @return True current parent entity, or null if none found
      */
     public static Entity getExactStartParent(WorldModel model, Entity entity) {
     	
     	int parentEntityID = entity.getParentEntityID();
     	
     	// Check for initial parent ID if this is an add instance that
         // is not yet in the scene. This is identified by the parent entity ID
         // being equal to -1. In this case, look for side pocketed parent ID.
         if (parentEntityID == -1) {

             Integer tmpParentEntityID = (Integer)
                 RulePropertyAccessor.getRulePropertyValue(
                         entity,
                         ChefX3DRuleProperties.INITAL_ADD_PARENT);

             if (tmpParentEntityID != null) {
                 parentEntityID = tmpParentEntityID;
             } else {
             	return null;
             }
         }
         
         Entity parentEntity = model.getEntity(parentEntityID);
			
		/////////////////////////////////////////////////////////////
		// rem: allow the command lists to be evaluated for potential
		// parents, rather than exiting early. related to issue @1255
		//if (parentEntity == null) {
		//	return null;
		//}
		/////////////////////////////////////////////////////////////
			
         // Get all of the commands on the queues
     	ArrayList<Command> cmdList = (ArrayList<Command>) 
     		CommandSequencer.getInstance().getFullCommandList(true);
     	
     	// By starting at the end of the list and working backwards
     	// we are guaranteed to get the last command
     	// issued affecting the entity.
     	
     	for (int i = (cmdList.size() - 1); i >= 0; i--) {
     		
     		Command cmd = cmdList.get(i);
     		
     		if (cmd instanceof RuleDataAccessor) {
     			
     			// If it isn't a command match, skip to next command
     			if (((RuleDataAccessor)cmd).getEntity() != entity) {
     				continue;
     			}
     		
     			// Try and match the command to something we care about.
     			// Something that will adjust the parenting of the entity.
 	    		if (cmd instanceof AddEntityChildCommand) {
 	    			
 	    			parentEntity = 
 	    				((AddEntityChildCommand)cmd).getParentEntity();
 	    			break;
 	    			
 	    		} else if (cmd instanceof AddEntityChildTransientCommand) {
 	    			
 	    			parentEntity =
 	    				((AddEntityChildTransientCommand)
 	    						cmd).getParentEntity();
 	    			break;
 	    			
 	    		} else if (cmd instanceof TransitionEntityChildCommand) {
 	    			
 	    			parentEntity =
 	    				((TransitionEntityChildCommand)
 	    						cmd).getStartParentEntity();
 	    			break;
 	    			
 	    		}
     		}   
     	}
     	
     	return parentEntity;
     }
     
     /**
      * Get the exact children that will exist after the evaluation set is
      * approved. This will be based on the current set of commands in the
      * queues as well as the current children of the entity. Children 
      * affected by commands will be adjusted accordingly in the resulting
      * list. This search 
      * considers all commands that are part of the current evaluation. 
      * The result is guaranteed to be correct for the current frame.
      * 
      * @param entity Entity to get children for
      * @return Array list of child entities
      */
     public static ArrayList<Entity> getExactChildren(Entity entity) {
     	
     	// The children of the entity, and final result list
     	ArrayList<Entity> childrenList = entity.getChildren();
     	
     	// Track any children that need to be added to childrenList
     	ArrayList<Entity> extraChildrenList = new ArrayList<Entity>();
     	
     	// Keep a list of children found so we only use the first command
     	// found acting on a child.
     	ArrayList<Entity> childrenFound = new ArrayList<Entity>();
     	
     	
     	// Get all of the commands on the queues
     	ArrayList<Command> cmdList = (ArrayList<Command>) 
     		CommandSequencer.getInstance().getFullCommandList(true);
     	

     	// By starting at the end of the list and working backwards
     	// we are guaranteed to get the last command
     	// issued affecting the entity.    	
     	for (int i = (cmdList.size() - 1); i >= 0; i--) {
     		
     		Command cmd = cmdList.get(i);
     		
     		// Don't evaluate commands that have no bearing on parenting.
     		if (cmd instanceof ChangePropertyCommand ||
     				cmd instanceof ChangePropertyTransientCommand ||
     				cmd instanceof SelectEntityCommand ||
     				cmd instanceof MoveEntityTransientCommand) {
     			continue;
     		}
     		
     		// Get the entity acted on by the command
     		Entity cmdEntity = null;
 			try {
 				cmdEntity = CommandDataExtractor.extractPrimaryEntity(cmd);
 			} catch (NoSuchFieldException e) {
 				e.printStackTrace();
 				continue;
 			}
     		
     		// If the entity acted on by the command is a child of our entity, 
     		// then see if the child is in childrenFound list
     		// if so skip action
     		// if not
     		//	   apply correct action
     		//	   add child to childrenFound list
     		// if any act on the parent then apply the correct action
     		if (childrenList.contains(cmdEntity) && 
     				!childrenFound.contains(cmdEntity)) {
     			
     			if (cmd instanceof TransitionEntityChildCommand) {
 	    			
 	    			if (((TransitionEntityChildCommand)
 	    						cmd).getEndParentEntity() != entity) {
 	    				
 	    				childrenList.remove(cmdEntity);
 	    			}
 	    			
 	    		} else if (cmd instanceof RemoveEntityChildCommand) {
 	    			
 	    			childrenList.remove(cmdEntity);
 	    			
 	    		} else if (cmd instanceof RemoveEntityChildTransientCommand) {
 	    			
 	    			childrenList.remove(cmdEntity);
 	    		}
     			
     			childrenFound.add(cmdEntity);
     			
     		} else if (!childrenFound.contains(cmdEntity)){
     			
     			// Try and match the command to something we care about.
     			// Something that will adjust the parenting of the entity.
 	    		if (cmd instanceof AddEntityChildCommand) {
 	    			
 	    			if (((AddEntityChildCommand)
 	    					cmd).getParentEntity() == entity) {
 	    				
 	    				extraChildrenList.add(cmdEntity);
 	    			}
 	    			
 	    		} else if (cmd instanceof AddEntityChildTransientCommand) {
 	    			
 	    			if (((AddEntityChildTransientCommand)
 	    						cmd).getParentEntity() == entity) {
 	    				
 	    				extraChildrenList.add(cmdEntity);
 	    			}
 	    			
 	    		} else if (cmd instanceof TransitionEntityChildCommand) {
 	    			
 	    			if (((TransitionEntityChildCommand)
 	    					cmd).getEndParentEntity() == entity) {
 	    				
 	    				extraChildrenList.add(cmdEntity);
 	    			}
 	    			
 	    		} else {
 	    			continue;
 	    		}
 	    		
 	    		childrenFound.add(cmdEntity);
     		}   
     	}
     	
     	childrenList.addAll(extraChildrenList);
     	
     	return childrenList;
     }
     
     /**
      * Get all of the entities of the data model branch beneath the 
      * branchHead entity specified.
      * 
      * @param branchHead Entity to start collecting the branch children from
      * @return ArrayList<Entity> of unsorted entities that are the branch
      * below the branchHead.
      */
     public static ArrayList<Entity> getBranch(Entity branchHead) {
    	 
    	 ArrayList<Entity> branch = new ArrayList<Entity>();
    	 
    	 ArrayList<Entity> children = branchHead.getChildren();
    	 
    	 for (Entity child : children) {
    		 branch.addAll(getBranch(child));
    	 }
    	 
    	 branch.addAll(children);
    	 
    	 return branch;
     }
     
     /**
      * Determines if the child entity is a child of the parent
      * if the while loop reaches the zone before the parent
      * this returns false, else if it finds the parent returns true.
      * @param model the world model
      * @param child The entity to check to see if it is the child of the parent
      * @param parent The parent
      * @param exact True to do analysis that is correct for current frame,
      * false to do analysis that is correct for the previous frame
      * @return Returns false if parent is not found before the zone, else
      *           returns true.
      */
     public static boolean isEntityChildOfParent(
  		   WorldModel model, 
  		   Entity child, 
  		   Entity parent,
  		   boolean exact) {

         Entity currentEntity = child;

         while (currentEntity != null && 
                 currentEntity.getType() != Entity.TYPE_LOCATION) {

        	 if (exact) {
        		 currentEntity = getExactParent(model, currentEntity);
        	 } else {
        		 currentEntity = getParent(model, currentEntity);
        	 }
        	 
             if (currentEntity == null) {
                 return false;
             }

             if(currentEntity == parent) {
                 return true;
             }

         }

         return false;

     }
     
     /**
      * Find the list of possible parents based on legal collisions and allowed
      * parents.
      *
      * @param command Command to check collisions with
      * @param model World model to reference
      * @return The list of possible parent entities, empty list if none found,
      * null if there was a problem
      */
     public static ArrayList<Entity> findPossibleParents(
             Command command,
             WorldModel model, 
             RuleCollisionHandler rch) {

         // The parent list to return
         ArrayList<Entity> parentList = new ArrayList<Entity>();

         // current parent entity
         Entity parentEntity = null;

         //------------------------------------------------
         // If the entity is an auto span, it should always
         // parent to the active zone.
         //------------------------------------------------
         Entity entity = ((RuleDataAccessor)command).getEntity();

         Boolean isAutoSpan = (Boolean)
             RulePropertyAccessor.getRulePropertyValue(
                     entity,
                     ChefX3DRuleProperties.SPAN_OBJECT_PROP);

         parentEntity = SceneHierarchyUtility.getActiveZoneEntity(model);

         if (parentEntity == null) {
             return parentList;
         }

         //-------------------------------------------------
         // Try to find a legal parents based on collision
         //-------------------------------------------------
         double[] currentPosition = new double[3];
         float[] bounds = new float[6];
         double[] testPosition = new double[3];

         if (!(entity instanceof PositionableEntity)) {
             return null;
         }

         ((PositionableEntity)entity).getBounds(bounds);
/*
         if (command instanceof MoveEntityCommand) {

             ((MoveEntityCommand)command).getEndPosition(currentPosition);
             testPosition[0] = currentPosition[0];
             testPosition[1] = currentPosition[1];
             testPosition[2] =
                 currentPosition[2] - bounds[5] + bounds[4] +
                 AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
             ((MoveEntityCommand)command).setEndPosition(testPosition);

             // use the active zone as the parent, which has previously been set

         } else if (command instanceof MoveEntityTransientCommand) {

             ((MoveEntityTransientCommand)command).getPosition(currentPosition);
             testPosition[0] = currentPosition[0];
             testPosition[1] = currentPosition[1];
             testPosition[2] =
                 currentPosition[2] - bounds[5] + bounds[4] +
                 AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
             ((MoveEntityTransientCommand)command).setPosition(testPosition);

             // use the active zone as the parent, which has previously been set

         } else if (command instanceof TransitionEntityChildCommand) {

             ((TransitionEntityChildCommand)command).getEndPosition(currentPosition);
             testPosition[0] = currentPosition[0];
             testPosition[1] = currentPosition[1];
             testPosition[2] =
                 currentPosition[2] - bounds[5] + bounds[4] +
                 AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
             ((TransitionEntityChildCommand)command).setEndPosition(testPosition);

             // make sure the final parent is set
             parentEntity =
                 ((TransitionEntityChildCommand)command).getEndParentEntity();

         } else if (command instanceof AddEntityChildCommand) {

             ((PositionableEntity)entity).getPosition(currentPosition);
             testPosition[0] = currentPosition[0];
             testPosition[1] = currentPosition[1];
             testPosition[2] =
                 currentPosition[2] - bounds[5] + bounds[4] +
                 AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
             ((PositionableEntity)entity).setPosition(testPosition, false);

             // make sure the target parent is set
             parentEntity = ((AddEntityChildCommand)command).getParentEntity();

         } else {

             ((PositionableEntity)entity).getPosition(currentPosition);
             testPosition[0] = currentPosition[0];
             testPosition[1] = currentPosition[1];
             testPosition[2] =
                 currentPosition[2] - bounds[5] + bounds[4] +
                 AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
             ((PositionableEntity)entity).setPosition(testPosition, false);

             // use the active zone as the parent, which has previously been set

         }
*/
         rch.performCollisionCheck(command, true, false, false);
/*
         if (command instanceof MoveEntityCommand) {

             ((MoveEntityCommand)command).setEndPosition(currentPosition);

         } else if (command instanceof MoveEntityTransientCommand) {

             ((MoveEntityTransientCommand)command).setPosition(currentPosition);

         } else if (command instanceof TransitionEntityChildCommand) {

             ((TransitionEntityChildCommand)command).setEndPosition(currentPosition);

         } else {

             ((PositionableEntity)entity).setPosition(currentPosition, false);

         }
*/
         if (rch.collisionEntities == null) {
             return null;
         }

         // make sure the current parent is in the list
         if (!rch.collisionEntities.contains(parentEntity) && parentEntity != null) {
             rch.collisionEntities.add(parentEntity);
         }


         Entity commandEntity = ((RuleDataAccessor)command).getEntity();

         String[] allowedParentClassifications = (String[])
             RulePropertyAccessor.getRulePropertyValue(
                     commandEntity,
                     ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

         // Loop through the collisions, ignoring any non model types, and
         // create a list of legal collisions.
         for (int i = 0; i < rch.collisionEntities.size(); i++) {

             Entity collisionEntity = rch.collisionEntities.get(i);

             // Only operate on Entities of type Model.
             if (!collisionEntity.isModel()) {
                 continue;
             }

             // Make sure we don't chose an entity that is parented to us
             int testEntityParentID = collisionEntity.getParentEntityID();

             // Ignore anything with a parent entity id of -1.
             // It hasn't been added yet and will really mung things up.
             if (testEntityParentID == -1) {
                 continue;
             }

             while (testEntityParentID > 0) {

                 if (testEntityParentID == commandEntity.getEntityID()) {
                     break;
                 }

                 Entity tmpEntity = model.getEntity(testEntityParentID);
                 if (tmpEntity == null)
                     break;
                 
                 testEntityParentID = tmpEntity.getParentEntityID();
             }

             if (testEntityParentID == commandEntity.getEntityID()) {
                 continue;
             }

             // If there are no parent classification limits, just add the
             // collision entity to the list. Otherwise, continue to abide
             // by the expressed requirement
             if (allowedParentClassifications == null ||
                     allowedParentClassifications.length < 1) {

                 parentList.add(collisionEntity);
                 continue;
             }

             String[] collisionEntityClassifications = (String[])
                 RulePropertyAccessor.getRulePropertyValue(
                         collisionEntity,
                         ChefX3DRuleProperties.CLASSIFICATION_PROP);

             if (collisionEntityClassifications != null) {
                 boolean matchFound = false;

                 // Check for a legal match of classification name and requirement
                 for (int j = 0; j < collisionEntityClassifications.length; j++) {

                     for (int w = 0; w < allowedParentClassifications.length; w++) {

                         if (collisionEntityClassifications[j].equalsIgnoreCase(
                             allowedParentClassifications[w])) {

                             parentList.add(collisionEntity);
                             matchFound = true;
                             break;
                         }
                     }

                     if (matchFound) {
                         break;
                     }
                 }
             }
         }

         return parentList;
     }
     
     /**
      * Check each parent to see if they are a shaded ancestor.  Return null if
      * no shared ancestor is found. For example, if we are colliding with two
      * standards, we want to parent to the hang track of the standards. This
      * method returns that hand track entity for us.
      *
      * @param model WorldModel to reference
      * @param entity Entity to get shared parent for
      * @param parentList List of possible parents to search through
      * @return Shared parent or null if one doesn't exist
      */
     public static Entity getSharedParent(
             WorldModel model,
             Entity entity,
             ArrayList<Entity> parentList) {

    	 // Shared parent to return
         Entity parentEntity = null;

         // early exit for missing or not enough data
         if (parentList == null || parentList.size() <= 0) {
             return parentEntity;
         }
         
         Map<Entity, Entity[]> parentMap = new HashMap<Entity, Entity[]>();
         
         Entity activeZone = getActiveZoneEntity(model);
         
         // Get all parent path for each parentList entry
         for (int i = 0; i < parentList.size(); i++) {

             Entity parent = parentList.get(i);
             
             if (activeZone != findExactZoneEntity(model, parent)) {
            	 continue;
             }
             
             Entity[] path = getParentPathToZone(model, parent, true);
             
             if (path == null) {
                 return null;    	 
             }
             
             parentMap.put(parent, path);
         }
         
         // Work backwards through each path and look for the first case of
         // non-matches. The entry just before that is the shared parent.
         
         boolean stop = false;
         int counter = 1;
         int testIndex = 0;
         Entity tmpParent = null;
         Object[] keys = parentMap.keySet().toArray();
         Entity[] testPath = null;
         
         // make sure we have some parents to work with
         if (parentMap.size() <= 0) {
        	 return null;
         }
         
         while (!stop) {
        	 
        	 for (int i = 0; i < keys.length; i++) {
        		 
        		 testPath = parentMap.get((Entity)keys[i]);
        		 
        		 testIndex = testPath.length - counter;
        		 
        		 if (i == 0 && testIndex >= 0) {
        			 tmpParent = testPath[testIndex];
        		 } else if (testIndex < 0 || 
        				 testPath[testIndex] != tmpParent) {
        			 stop = true;
        			 break;
        		 }
        	 }
        	 
        	 if (!stop) {
	        	 parentEntity = tmpParent;
	        	 counter++;
        	 }
         }
         
         if (parentEntity == null) {
        	 return null;
         }

         // Make sure the shared parent is a legal classification
         String[] allowedParentClassifications = 
             (String[])RulePropertyAccessor.getRulePropertyValue(
                 entity,
                 ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);
         
         // Get the classificaitons of the parent entity to check against
         String[] classifications = (String[]) 
         	RulePropertyAccessor.getRulePropertyValue(
         			parentEntity, 
         			ChefX3DRuleProperties.CLASSIFICATION_PROP);
         
         // If allowed parent classifications is null, return the parentEntity.
         // If allowed parent classifications is not null, but classifications
         // is null, return null since we can't do our allowed parent check.
         if (allowedParentClassifications == null) {
        	 return parentEntity;
         } else if (classifications == null) {
        	 return null;
         }

         for (int i = 0; i < allowedParentClassifications.length; i++) {
        	 
        	 for (int j = 0; j < classifications.length; j++) {
        		 
        		 if (allowedParentClassifications[i].equals(
        				 classifications[j])) {
        			 return parentEntity;
        		 }
        	 }
         }

         return null;

     }
     
     /**
      * Generate a path from the current entity down to the zone. Returns an
      * array of entities, starting with the parent and ending with the zone.
      * 
      * @param model WorldModel to reference
      * @param entity Entity to track parent path from 
      * @param exact True to get exact parents, false to get previous frame
      * parents.
      * @return Array of entity results ordered initial parent to zone
      */
     public static Entity[] getParentPathToZone(
             WorldModel model,
             Entity entity,
             boolean exact) {
    	 
    	 Entity parent = null;
    	 
    	 if (exact) {
    		 parent = getExactParent(model, entity);
    	 } else {
    		 parent = getParent(model, entity);
    	 }
         
         // if we have no parent, then bail
         if (parent == null) {
             return null;
         }
         
         // Keeps our list of entities
         ArrayList<Entity> parentList = new ArrayList<Entity>();
         
         // Look up parents till we find a zone
         while (parent != null && !parent.isZone()) {
        	 
        	 // If we have hit a location entity, we have gone too far so bail
        	 // out.
        	 if (parent.getType() == Entity.TYPE_LOCATION) {
        		 return null;
        	 }
        	 
        	 parentList.add(parent);
        	 
        	 if (exact) {
        		 parent = getExactParent(model, parent);
        	 } else {
        		 parent = getParent(model, parent);
        	 }
         }
         
         // Add the zone if the parent isn't null
         if (parent != null) {
        	 parentList.add(parent);
         }
         
         Entity[] results = new Entity[parentList.size()];
         parentList.toArray(results);
         return results;
    }
     
     /**
      * Check if the targetEntity is a swap target of the sourceEntity. If 
      * considerChildren is true, the checke will also look to see if the 
      * targetEntity is a child of an entity that is a swapTarget and if so, 
      * that too will return true.
      * 
      * @param model WorldModel to reference
      * @param sourceEntity SorceEntity to consider (the one swapping out)
      * @param targetEntity TargetEntity to consider (the one swapping in)
      * @param considerChildren True to consider children as possible swap
      * targets, false to consider just the targetEntity and not children of
      * the targetEntity.
      * @return True if targetEntity is a swap target of sourceEntity, false
      * otherwise.
      */
     public static boolean isSwapTarget(
    		 WorldModel model,
    		 Entity sourceEntity, 
    		 Entity targetEntity,
    		 boolean considerChildren) {
    	 
    	 // Get the list of known commands and isolate only those that
    	 // are part of a swap set.
    	 // All commands associated with a swap can be identified by the 
    	 // SceneManagementUtility.SWAP_COMMAND_DESCRIPTION constant in the
    	 // command description. If the command description contains this 
    	 // constant then it is an associated part of a greater swap operation.
    	 // This is followed by a unique id in brackets that can be parsed to
    	 // identify related swap commands.
    	 
    	 ArrayList<Command> commandList = (ArrayList<Command>) 
    	 	CommandSequencer.getInstance().getFullCommandList(true);
    	 
    	 // Remove any commands, not identified as swaps, from the list.
    	 // Also remove any command that doesn't implement RuleDataAccessor
    	 for (int i = (commandList.size() - 1); i >= 0; i--) {
    		 
    		 Command cmd = commandList.get(i);
    		 
    		 if (!cmd.getDescription().contains(
    				 SceneManagementUtility.SWAP_COMMAND_DESCRIPTION)) {
    			 
    			 commandList.remove(i);
    		 
    		 } else if (!(cmd instanceof RuleDataAccessor)) {
    			 
    			 commandList.remove(i);
    		 }
    	 }
    	 
    	 // If there aren't any swapping commands exit early and return false
    	 if (commandList.size() == 0) {
    		 return false;
    	 }
    	 
    	 // See if the sourceEntity is part of one of the swap commands. If not
    	 // return false. If it is, get the unique swap id to look for in the
    	 // target command cases.
    	 String uniqueSwapID = null;
    	 
    	 for (Command c : commandList) {
    		 
    		 if (((RuleDataAccessor)c).getEntity().equals(sourceEntity)) {
    			 
    			 uniqueSwapID = getUniqueSwapID(c.getDescription());

    			 break;
    		 }
    	 }
    	 
    	 if (uniqueSwapID == null) {
    		 return false;
    	 }
    	 
    	 // See if the targetEntity matches any of the swap commands. If so
    	 // see if it matches the unique swap id associated with the 
    	 // sourceEntity.    	 
    	 for (Command c : commandList) {
    		 
    		 if (((RuleDataAccessor)c).getEntity().equals(targetEntity)) {
    			 
    			 if (uniqueSwapID.equals(getUniqueSwapID(c.getDescription()))) {
    				 
    				 return true;
    			 }
    		 }
    	 }
    	 
    	 if (considerChildren) {
	    	 // If the targetEntity didn't match any of the swap commands, we 
    		 // should still examine it's hierarchy in the event that one of its
    		 // parents match.
	    	 Entity parentEntity = getExactParent(model, targetEntity);
	    	 
	    	 while (parentEntity != null && parentEntity.isModel()) {
	    		 
	    		 for (Command c : commandList) {
	        		 
	        		 if (((RuleDataAccessor)c).getEntity().equals(
	        				 parentEntity)) {
	        			 
	        			 if (uniqueSwapID.equals(
	        					 getUniqueSwapID(c.getDescription()))) {
	        				 
	        				 return true;
	        			 }
	        		 }
	        	 }
	    		 
	    		 parentEntity = getExactParent(model, parentEntity);
	    	 }
    	 }
    	 
    	 return false;
     }
     
     /**
      * Check if the entity is swapping, either as the swapOut or swapIn
      * target. This does not consider children of swaps.
      * 
      * @param entity Entity to check for swapping
      * @return True if the entity is either a swapOut or swapIn target, false
      * otherwise.
      */
     public static boolean isSwapping(Entity entity) {
    	 
    	// Get the list of known commands and isolate only those that
    	 // are part of a swap set.
    	 // All commands associated with a swap can be identified by the 
    	 // SceneManagementUtility.SWAP_COMMAND_DESCRIPTION constant in the
    	 // command description. If the command description contains this 
    	 // constant then it is an associated part of a greater swap operation.
    	 // This is followed by a unique id in brackets that can be parsed to
    	 // identify related swap commands.
    	 
    	 ArrayList<Command> commandList = (ArrayList<Command>) 
    	 	CommandSequencer.getInstance().getFullCommandList(true);
    	 
    	 // Remove any commands, not identified as swaps, from the list.
    	 // Also remove any command that doesn't implement RuleDataAccessor
    	 for (int i = (commandList.size() - 1); i >= 0; i--) {
    		 
    		 Command cmd = commandList.get(i);
    		 
    		 // NPE safety check
    		 if (cmd.getDescription() == null) {
    			 continue;
    		 }
    		 
    		 if (!cmd.getDescription().contains(
    				 SceneManagementUtility.SWAP_COMMAND_DESCRIPTION)) {
    			 
    			 commandList.remove(i);
    		 
    		 } else if (!(cmd instanceof RuleDataAccessor)) {
    			 
    			 commandList.remove(i);
    		 }
    	 }
    	 
    	 // If there aren't any swapping commands exit early and return false
    	 if (commandList.size() == 0) {
    		 return false;
    	 }
    	 
    	 // See if the entity is part of one of the swap commands. If not
    	 // return false. Otherwise, return true.
    	 for (Command c : commandList) {
    		 
    		 if (((RuleDataAccessor)c).getEntity().equals(entity)) {
    			 
    			 return true;
    		 }
    	 }
    	 
    	 return false;
     }
     
     /**
      * Get the unique swap ID associated with the command via the description.
      * 
      * @param description Description of the command to extract the unique
      * swap ID from.
      * @return
      */
     private static String getUniqueSwapID(String description) {
    	 
    	 String uniqueSwapID;
    	 
		 uniqueSwapID = 
			 description.substring(
					 description.indexOf(
							 SceneManagementUtility.
							 SWAP_COMMAND_DESCRIPTION)+
							 SceneManagementUtility.
							 SWAP_COMMAND_DESCRIPTION.length()+1);
		 
		 uniqueSwapID = 
			 uniqueSwapID.substring(0, uniqueSwapID.indexOf("]"));
		 
		 return uniqueSwapID;
     }
}
