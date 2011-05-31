/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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

// External Imports
import java.util.ArrayList;
import java.util.List;

import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

/**
 * Auto-span utilities
 *
 * @author Russell Dodds
 * @version $Revision: 1.5 $
 */
public abstract class AutoSpanUtility {
	        
    /**
     * Determine if we have a neighbor to use on each side (x-axis) of the item.
     * Should set the neighbor entities to return.  This will ignore any items
     * flagged as no model, placed on auto-span, scale model items, and 
     * complex sub parts.
     *
     * @param model the world model
     * @param rch The rule collision handler
     * @param command the command
     * @param entity the entity
     * @param pos the position of the entity
     * @param negXNeighbor The left side neighbor found
     * @param posXNeighbor The right side neighbor found
     * @return return true if a neighbor is found on each side, false otherwise
     */
	public static boolean getNearestNeighborEntities(
            WorldModel model,
            RuleCollisionHandler rch,
            PositionableEntity entity,
            double[] pos,
            Entity[] neighbors) {

        // get the model size, scale, and pos data
        double[] origPos = new double[3];
        ((PositionableEntity)entity).getPosition(origPos);

        float[] origScale = new float[3];
        ((PositionableEntity)entity).getScale(origScale);

        float[] origSize = new float[3];
        ((PositionableEntity)entity).getSize(origSize);

        // Prepare entity by scaling it very small      
        float[] tmpScale = new float[3];
        tmpScale[0] = 0.01f/origSize[0];
        tmpScale[1] = origScale[1];
        tmpScale[2] = origScale[2];
        entity.setScale(tmpScale);

        // put the product where it will based on the command
        entity.setPosition(pos, false);
        
        // Get nearest neighbor entities
        ArrayList<Entity> negativeXNeighbors =
            rch.getNegativeXNeighbors((PositionableEntity) entity, null);

        ArrayList<Entity> positiveXNeighbors =
            rch.getPositiveXNeighbors((PositionableEntity) entity, null);

        // Reset original entity position and scale
        entity.setPosition(origPos, false);
        entity.setScale(origScale);

        // Do legit data check
        if(negativeXNeighbors == null ||
                negativeXNeighbors.size() == 0 ||
                positiveXNeighbors == null ||
                positiveXNeighbors.size() == 0){

            return false;
        }

        List<Entity> negRemoveList = new ArrayList<Entity>();
        List<Entity> posRemoveList = new ArrayList<Entity>();
        
        Entity parentEntity = 
        	SceneHierarchyUtility.getExactParent(model, entity);
        
        
        // look for auto-spans and no-models to remove
        for (int i = 0; i < negativeXNeighbors.size(); i++) {

            Entity check = negativeXNeighbors.get(i);
            
            Boolean goesOnAutoSpan = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        check,
                        ChefX3DRuleProperties.CAN_PLACE_ON_SPAN_OBJECT);

            // Remove all nearest neighbors that go on auto spans
            if (goesOnAutoSpan) {
                negRemoveList.add(check);
                continue;
            }
            
            // remove items set with the no model flag
            Boolean noModel = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        check,
                        ChefX3DRuleProperties.NO_MODEL_PROP);
          
            if (noModel) {
                negRemoveList.add(check);
                continue;
            }
            
            // Remove all nearest neighbors that are descendants
            boolean isDescendant = 
                SceneHierarchyUtility.isEntityChildOfParent(model, check, entity, true);
            if (isDescendant) {
                negRemoveList.add(check);
            }
            
        }

        for (int i = 0; i < positiveXNeighbors.size(); i++) {

            Entity check = positiveXNeighbors.get(i);

            Boolean goesOnAutoSpan = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    check,
                    ChefX3DRuleProperties.CAN_PLACE_ON_SPAN_OBJECT);

            if (goesOnAutoSpan) {
                posRemoveList.add(check);
                continue;
            }
            
            // remove items set with the no model flag
            Boolean noModel = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        check,
                        ChefX3DRuleProperties.NO_MODEL_PROP);
          
            if (noModel) {
                posRemoveList.add(check);
                continue;
            }
            
            // Remove all nearest neighbors that are descendants
            boolean isDescendant = 
                SceneHierarchyUtility.isEntityChildOfParent(model, check, entity, true);
            if (isDescendant) {
                posRemoveList.add(check);
            }

        }
                
        // Check to see if any of the entities in the negativeXNeighbors and 
        // positiveXNeighbors lists use model swap and if so, remove from the 
        // list any that can are swap targets.
        Boolean scaleChangeModelFlag =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);
        
        String[] productIDArray = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);
        
        if (scaleChangeModelFlag && productIDArray != null) {
            
            String autoSpanEntityToolID;
            
            for (int i = (negativeXNeighbors.size() - 1); i >= 0; i--) {
                
            	Entity check = negativeXNeighbors.get(i); 
                autoSpanEntityToolID = check.getToolID();
                
                for (int x = 0; x < productIDArray.length; x++) {
                    
                    if (productIDArray[x].equals(autoSpanEntityToolID)) {
                    	
                        // We have a matching tool ID, check to see if the 
                    	// parents match                    	                  	
                        Entity checkParentEntity = 
                        	SceneHierarchyUtility.getExactParent(model, check);

                        // if they have the same parent then remove it
                    	if (parentEntity.equals(checkParentEntity)) {
                    		negRemoveList.add(check);
                            break;
                    	}
                    }
                }
            }
            
            for (int i = (positiveXNeighbors.size() - 1); i >= 0; i--) {
            	
            	Entity check = positiveXNeighbors.get(i);                   	  
                autoSpanEntityToolID = check.getToolID();
                
                for (int x = 0; x < productIDArray.length; x++) {
                    
                    if (productIDArray[x].equals(autoSpanEntityToolID)) {
                        
                        // We have a matching tool ID, check to see if the 
                    	// parents match
                        Entity checkParentEntity = 
                        	SceneHierarchyUtility.getExactParent(model, check);

                        // if they have the same parent then remove it
                    	if (parentEntity.equals(checkParentEntity)) {
                    		posRemoveList.add(check);
                            break;
                    	}
                    }
                }
            }
        }
                
        // now we need to make sure all items being removed also have their 
        // children removed
        for (int i = 0; i < negRemoveList.size(); i++) {

            Entity parent = negRemoveList.get(i);
            
            for (int j = 0; j < negativeXNeighbors.size(); j++) {
                
                Entity child = negativeXNeighbors.get(j);
                
                // Remove all nearest neighbors that are descendants
                boolean isDescendant = 
                    SceneHierarchyUtility.isEntityChildOfParent(model, child, parent, true);
                if (isDescendant) {
                    negRemoveList.add(child);
                }

            }
                        
        }
        
        // remove the matched items now
        negativeXNeighbors.removeAll(negRemoveList);        
       
        // now we need to make sure all items being removed also have their 
        // children removed
        for (int i = 0; i < posRemoveList.size(); i++) {

            Entity parent = posRemoveList.get(i);
            
            for (int j = 0; j < positiveXNeighbors.size(); j++) {
                
                Entity child = positiveXNeighbors.get(j);
                
                // Remove all nearest neighbors that are descendants
                boolean isDescendant = 
                    SceneHierarchyUtility.isEntityChildOfParent(model, child, parent, true);
                if (isDescendant) {
                    posRemoveList.add(child);
                }

            }
                        
        }
        
        // remove the matched items now
        positiveXNeighbors.removeAll(posRemoveList);        

        // Do a legit data check
        if (negativeXNeighbors.size() == 0 || positiveXNeighbors.size() == 0) {
            return false;
        }

        // get the neighbor based on the valid relationships
        neighbors[0] = getNieghborWithRelationship(entity, negativeXNeighbors);
        neighbors[1] = getNieghborWithRelationship(entity, positiveXNeighbors);
       
        if (neighbors[0] == null || neighbors[1] == null) {
        	return false;
        }

        return true;
    }
	
	/**
	 * Get the neighbor based on the valid relationships
	 * 
	 * @param entity The entity being spanned
	 * @param neighborList The current neighbor list
	 * 
	 * @return The matched entity, null if no valid relationship is found
	 */
	private static Entity getNieghborWithRelationship(
			Entity entity, 
			List<Entity> neighborList) {
		
        // now look through the remaining items for the first one that is the 
        // relationships list
		String[] classRelationship = 
			(String[])RulePropertyAccessor.getRulePropertyValue(
        			entity,
        			ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
		
		// flat the list to single entries
		List<String> flattenedRelationships = new ArrayList<String>();
		for (int i = 0; i < classRelationship.length; i++) {
			String classRel = classRelationship[i];
			String[] classRelList = classRel.split(":");
			for (int j = 0; j < classRelList.length; j++) {
				if (!flattenedRelationships.contains(classRelList[j])) {
					flattenedRelationships.add(classRelList[j]);
				}
			}
		}
		
		// make sure to look at the neighbors in order
		for (int i = 0; i < neighborList.size(); i++) {
			Entity neighbor = neighborList.get(i);
			
			String[] classifications = 
				(String[])RulePropertyAccessor.getRulePropertyValue(
						neighbor,
						ChefX3DRuleProperties.CLASSIFICATION_PROP);

			if (classifications != null) {
				
				for (int j = 0; j < classRelationship.length; j++) {
					String classRel = classRelationship[j];
					
					for (int k = 0; k < classifications.length; k++) {
						
						String classification = classifications[k];
						if (classification.equals(classRel)) {
							// matched
							return neighbor;
						}
					}
				}
			}
		}		
		return null;
	}
    
    /**
     * Check if there is a relationship between the auto spanning entity and
     * the non auto spanning entity that would allow their collision to be
     * legal.
     * 
     * @param autoSpanEntity Entity that auto spans
     * @param nonAutoSpanEntity Entity to check agains auto span
     * @return True if they are legal, false if not
     */
    public static boolean isAllowedAutoSpanCollision(
    		Entity autoSpanEntity,
    		Entity nonAutoSpanEntity) {
    	
    	// see if it is an allowed collision.
		
		String[] classifications = (String[]) 
			RulePropertyAccessor.getRulePropertyValue(
					autoSpanEntity, 
					ChefX3DRuleProperties.CLASSIFICATION_PROP);
		
		String[] relationships = (String[]) 
			RulePropertyAccessor.getRulePropertyValue(
				nonAutoSpanEntity, 
				ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
		
		Boolean canPlaceOnAutoSpan = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					nonAutoSpanEntity, 
					ChefX3DRuleProperties.CAN_PLACE_ON_SPAN_OBJECT);
		
		// Entities that can be placed on auto span products
		// have to have this flag set.
		if (!canPlaceOnAutoSpan) {
			return false;
		}
		
		// If either of these data are null, return false
		if (classifications == null ||
				relationships == null) {
			
			return false;
		}
		
		boolean autoSpanMatched = false;
		
		for (int w = 0; w < classifications.length; w++) {
			
			for (int x = 0; 
					x < relationships.length; x++) {
				
				if (classifications[w].equalsIgnoreCase(
						relationships[x])) {

					autoSpanMatched = true;
					break;
				}
			}
			
			if (autoSpanMatched) {
				break;
			}
		}
		
		return autoSpanMatched;
    }
    
    /**
     * Get the list of auto spanning entities in collision with the entity 
     * passed in.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to perform collision test with
     * @param rch RuleCollisionHandler to reference
     * @param exact True to create a dummy collision command with next frame
     * data, false to use previous frame data
     * @param useStartValues True to use starting state values, false to use
     * current state values
     * @return List of auto spanning entities
     */
    public static List<Entity> getCollisionsWithAutoSpans(
    		WorldModel model,
    		PositionableEntity entity,
    		RuleCollisionHandler rch,
    		boolean exact,
    		boolean useStartValues) {
    	
    	Command dummyCmd = 
    		rch.createCollisionDummyCommand(
    				model, entity, exact, useStartValues);
    	
    	rch.performCollisionCheck(dummyCmd, true, false, false);
    	
    	ArrayList<Entity> collisions = 
    		new ArrayList<Entity>(rch.collisionEntities);
    	
    	for (int i = (collisions.size() - 1); i >= 0; i--) {
    		
    		Boolean autoSpan = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SPAN_OBJECT_PROP);

	        if(autoSpan == null || autoSpan == false){
	            collisions.remove(i);
	        }
    	}
    	
    	return collisions;
    }
}
