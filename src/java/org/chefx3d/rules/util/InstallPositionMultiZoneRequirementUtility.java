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

package org.chefx3d.rules.util;

//External Imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.ui.PopUpMessage;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;
import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.EntityWrapper;
import org.j3d.util.I18nManager;

/**
* Shared routines related installation position requirements.
*
* @author Ben Yarger
* @version $Revision: 1.1 $
*/
public abstract class InstallPositionMultiZoneRequirementUtility {
	
    /**
     * Magic does not exist (DNE) value.
     * Special value used to denote no position specific requirement exists
     * for matched relationship. All three x,y,z values will be set to this
     * for the case to be ignored.
     */
    public static double MAGIC_DNE_VALUE = -1000.0;
    
    private static final String POP_UP_BOUNDS_EXCEEDED =
        "org.chefx3d.rules.definitions.InstallPositioinRequirementRule.boundsExceeded";
    
    private static final String POP_UP_MISSING_DATA =
        "org.chefx3d.rules.definitions.InstallPositionMultiZoneRequirementRule";
    
    /**
     * Check if the analysisEntity has dependencies because it is satisfying
     * a multi zone position installation requirement. Tested on starting state
     * values.
     * 
     * @param model WorldModel to reference
     * @param command Command acting on analysisEntity
     * @param analysisEntity Entity to check for dependencies on
     * @param rch RuleCollisionHandler to reference
     * @param view EditorView to reference
     * @return True if there are dependencies, false otherwise
     */
    public static boolean hasPositionCollisionRequirementsImposed(
    		WorldModel model,
    		Command command,
    		Entity analysisEntity,
            RuleCollisionHandler rch,
            EditorView view) {
    	
    	// Create a dummy command at the starting state of the analysisEntity
    	Command dummyCmd = createDummyCommand(model, analysisEntity, command);
    	
    	// Perform collision check to see what we are working with.
        // Requires doing collision analysis
        rch.performCollisionCheck(dummyCmd, false, false, true);
        
        if (rch.collisionEntities == null) {
        	return false;
        }

        // Make a copy so we don't lose any in future collision checks
        ArrayList<Entity> collisions = new ArrayList<Entity>();
        collisions.addAll(rch.collisionEntities);
        
        for (Entity entity: collisions) {
        	
        	Boolean usesPositionMZRule = (Boolean)
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.
	                COLLISION_POSITION_MULTI_ZONE_REQUIREMENTS);
        	
        	if (!usesPositionMZRule) {
        		continue;
        	}
        	
	        String[] classRelationship = (String[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
			
	        int[] relationshipAmount = (int[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP);
			
	        Enum[] relModifier = (Enum[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP);
	
	        // Perform collision analysis, if returns false it is because
	        // requisite data could not be extracted.
	        if(!rch.performCollisionAnalysisHelper(
	                entity,
	                null,
	                false,
	                null,
	                true)){
				
	            return false;
	        }
	
	        // If no collisions exist, don't process any further
	        if(rch.collisionEntities == null || 
	        		rch.collisionEntities.size() == 0){
	
	            if(command.isTransient()){
	                return true;
	            } else {
	                return false;
	            }
	        }
	
	        // If illegal collision results exist don't process any further
	        if(rch.hasIllegalCollisions(
	                classRelationship,
	                relationshipAmount,
	                relModifier)){
	
	            if(command.isTransient()){
	                return true;
	            } else {
	                return false;
	            }
	        }
	
	        // Retrieve the legal classification index. If -1 stop execution
	        int legalIndex = rch.getLegalRelationshipIndex(entity);
	
	        if (legalIndex < 0) {
	        	I18nManager intl_mgr = I18nManager.getManager();
	        	PopUpMessage popUpMessage = PopUpMessage.getInstance();
	            String msg = intl_mgr.getString(POP_UP_BOUNDS_EXCEEDED);
	            popUpMessage.showMessage(msg);
				
	            return false;
	        }
	
	        // Get the starting and ending index of position values that match
	        // the relationship being analyzed.
	        String classRelationshipVal = classRelationship[legalIndex];
	        int relationshipAmountVal = relationshipAmount[legalIndex];
	
	        int startPosIndex =
	            calculateStartPosIndex(
	                classRelationship,
	                relationshipAmount,
	                legalIndex);
	
	        int endPosIndex =
	            calculateEndPosIndex(
	                    classRelationship,
	                    relationshipAmount,
	                    legalIndex);
	
	        // Grab required data sets
	        float[] xPosValues = (float[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.COLLISION_POSITION_X_AXIS_VALUES);
	
	        float[] yPosValues = (float[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.COLLISION_POSITION_Y_AXIS_VALUES);
	
	        float[] zPosValues = (float[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.COLLISION_POSITION_Z_AXIS_VALUES);
	
	        float[] posTolerance = (float[])
	            RulePropertyAccessor.getRulePropertyValue(
	                entity,
	                ChefX3DRuleProperties.COLLISION_POSITION_TOLERANCE);
	
	        Enum[] targetAdjustmentAxis = (Enum[])
	            RulePropertyAccessor.getRulePropertyValue(
	                    entity,
	                    ChefX3DRuleProperties.COLLISION_TARGET_ADJUSTMENT_AXIS);
	
	        if(xPosValues == null ||
	                yPosValues == null ||
	                zPosValues == null ||
	                posTolerance == null ||
	                targetAdjustmentAxis == null){
	
	        	I18nManager intl_mgr = I18nManager.getManager();
	        	PopUpMessage popUpMessage = PopUpMessage.getInstance();
	            String msg = intl_mgr.getString(POP_UP_MISSING_DATA);
	            popUpMessage.showMessage(msg);
				
	            return false;
	        }
	
	        // Generate the precise list of relationships to satisfy
	        String[] relationships =
	            buildFullRelationshipList(
	                    classRelationshipVal,
	                    relationshipAmountVal);
	        
	        double[] entityAdjustment = new double[3];
	
	        HashMap<PositionableEntity, Vector3f> processResult = evaluate(
	        		model,
	                entity,
	                xPosValues,
	                yPosValues,
	                zPosValues,
	                posTolerance,
	                targetAdjustmentAxis,
	                startPosIndex,
	                endPosIndex,
	                relationships,
	                entityAdjustment,
	                rch,
	                view);
	
	        if (processResult == null) {
	        	return false;
	        }
	
	        // See if the analysisEntity is matched anywhere in the 
	        // processResults
	        Object[] keys = processResult.keySet().toArray();
	        
	        for (int i = 0; i < keys.length; i++) {
	        	
	        	if (keys[i] != null) {
	        		
	        		if (((PositionableEntity) keys[i]).getEntityID() == 
	        			analysisEntity.getEntityID()) {
	        			
	        			return true;
	        		}
	        	}
	        }
        }
        
        return false;
    }

	/**
     * Evaluate the entities in collision with the argument entity to 
	 * determine whether the requirements for supporting it are 
	 * available. If so, return a map of the supporting entities and the
	 * adjustments that must be made to their positions to align with the
	 * argument entity. If the requirements are not fulfilled, return null.
     *
     * @param model WorldModel to reference
     * @param entity Entity with specific relative positions to check
     * @param xPosValues X axis position set
     * @param yPosValues Y axis position set
     * @param zPosValues Z axis position set
     * @param posTolerance Position tolerance set
     * @param targetAdjustmentAxis Target adjustment axis set
     * @param startPosIndex Position data start index
     * @param endPosIndex Position data end index
     * @param relationships Relationships to match against
	 * @param entity_adjustment The adjustment to apply to the argument entity
	 * @param rch RuleCollisionHander to reference
	 * @param view EditorView to reference
     * @return A map of the supporting entities that satisfy the requirements 
     * with an adjustment value to position the entity appropriately, or null 
     * if the positioning requirements could not be fulfilled.
     */
    public static HashMap<PositionableEntity, Vector3f> evaluate(
    		WorldModel model,
            Entity entity,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships,
			double[] entity_adjustment,
			RuleCollisionHandler rch,
			EditorView view) {
			
		////////////////////////////////////////////////////////////////////////
		// rem: This is where the calculations are performed to determine
		// whether a set of supporting entities exist that are in contact with
		// the argument entity and are of the proper classification.
		//
		// This is NOT very general purpose - which is a bad thing - but it 
		// seems to work for the current test cases. Some assumptions
		// embedded in this method that restrict it's usage:
		// 
		// - the entities are to be adjusted in the X/Z plane of the current
		// active zone. any Y axis evaluations are ignored.
		//
		// - the axis of evaluation determines the zone in which the supporting
		// entity is parented to. X axis evaluations are presumed to be looking 
		// for entities in the current active zone (with the evaluation entity).
		// Z axis evaluations are presumed to be in an adjacent zone.
		
		////////////////////////////////////////////////////////////////////////
		// reduce the relationship/positioning data to the set that 
		// must be evaluated
		
		// the max number of relationships that must be satisfied
		// note: this -should also- be equal to (endPosIndex - startPosIndex)
        int max_rel = relationships.length; 
		
		// the actual relationships that must be satisfied
		int num_req_rel = 0;
		int[] req_rel_idx = new int[max_rel];
		for (int i = 0; i < max_rel; i++) {
			
			int idx = i + startPosIndex;
			// Perform check to see if the MAGIC_DNE_VALUE
			// applies to the positions set. If it does, then we ignore
			// any position requirements for this relationship and don't
			// bother creating position collision data for it.
			if ((xPosValues[idx] != MAGIC_DNE_VALUE) &&
				(yPosValues[idx] != MAGIC_DNE_VALUE) &&
				(zPosValues[idx] != MAGIC_DNE_VALUE)) {
				
				req_rel_idx[num_req_rel++] = i;
			}
		}
		if (num_req_rel > 0) { 
			// reconfigure the value arrays to contain only
			// the data to be evaluated
			float[] req_xPosValues = new float[num_req_rel];
			float[] req_yPosValues = new float[num_req_rel];
			float[] req_zPosValues = new float[num_req_rel];
			float[] req_posTolerance = new float[num_req_rel];
			Enum[] req_targetAdjustmentAxis = new Enum[num_req_rel];
			String[] req_relationships = new String[num_req_rel] ;
			
			for (int i = 0; i < num_req_rel; i++) {
				
				int idx = req_rel_idx[i];
				req_relationships[i] = relationships[idx];
				
				idx += startPosIndex;
				req_xPosValues[i] = xPosValues[idx];
				req_yPosValues[i] = yPosValues[idx];
				req_zPosValues[i] = zPosValues[idx];
				req_posTolerance[i] = posTolerance[idx];
				req_targetAdjustmentAxis[i] = targetAdjustmentAxis[idx];
			}
			relationships = req_relationships;
			
			xPosValues = req_xPosValues;
			yPosValues = req_yPosValues;
			zPosValues = req_zPosValues;
			posTolerance = req_posTolerance;
			targetAdjustmentAxis = req_targetAdjustmentAxis;
		} else {
			// no requirements to meet, nothing to do
			return(null);
		}
		////////////////////////////////////////////////////////////////////////
		// reduce the collision entity set to only those that
		// meet the classification requirements
		
        ArrayList<Entity> collisionList = new ArrayList<Entity>();

        ArrayList<Entity> entityMatches =
        	rch.getChildrenMatches().getEntityMatches();

        ArrayList<Entity> wallEntityMatches =
        	rch.getChildrenMatches().getWallEntityMatches();

		// rem: why use this if/else statement ?
        if (entityMatches.size() > 0) {
            collisionList.addAll(entityMatches);
        } else if (wallEntityMatches.size() > 0) {
            collisionList.addAll(wallEntityMatches);
        }

        for (int i = collisionList.size() - 1; i >= 0; i--) {

            Entity collisionEntity = collisionList.get(i);

            String[] classifications = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                        collisionEntity,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

            // attempt to match the classification of the collision entity
			// to the required relationship parameters
			boolean matchFound = false;
            for (int j = 0; j < classifications.length; j++) {
                for (int k = 0; k < num_req_rel; k++) {
                    if (relationships[k].equals(classifications[j])) {
						matchFound = true;
						break;
					}
				}
				if (matchFound) {
					break;
				}
			}
			if (!matchFound) {
				collisionList.remove(i);
			}
		}
		
		int num_collision = collisionList.size();
		if (num_collision < num_req_rel) {
			// there are not sufficient intersections with entities of the 
			// specified classifications to fulfill the requirements
			return(null);
		}
		////////////////////////////////////////////////////////////////////////
        // check the collision entities to determine if the positioning
		// requirements are met
		
		// entity wrappers, from which to obtain bounding objects
		HashMap<Integer, EntityWrapper> wmap = view.getEntityWrapperMap();

		// get the world to zone transforms
		Matrix4f zone_mtx = new Matrix4f();
		Entity zone = SceneHierarchyUtility.getActiveZoneEntity(model);
		EntityUtils entityUtils = new EntityUtils(model);
		entityUtils.getTransformToRoot(zone, zone_mtx);
		
		Matrix4f inv_zone_mtx = new Matrix4f(zone_mtx);
		inv_zone_mtx.invert();
		
		// get the bounds of the collision entities relative to 
		// the active zone
		OrientedBoundingBox[] bounds = new OrientedBoundingBox[num_collision];
		for (int i = 0; i < num_collision; i++) {

            Entity e = collisionList.get(i);
			EntityWrapper wrapper = wmap.get(e.getEntityID());
			bounds[i] = wrapper.getBounds();
			
			// transform into the active zone
			Matrix4f mtx = new Matrix4f();
			entityUtils.getTransformToRoot(e, mtx);
			mtx.mul(inv_zone_mtx, mtx);
			bounds[i].transform(mtx);
		}
		
		// get the position of the entity relative to the active zone
		double[] entity_position = 
			TransformUtils.getExactPositionRelativeToZone(
			model,
			entity);
		
		// parameters of the collision entity bounds
		Point3f bnd_center = new Point3f();
		Point3f bnd_min = new Point3f();
		Point3f bnd_max = new Point3f();
		
		// bounds and entity intersection points
		Point3f bnd_pnt = new Point3f();
		Point3f entity_pnt = new Point3f();
		
		// adjusted position of the action entity
		Point3f adj_entity_pos = new Point3f();
		
		// distances of bnd_pnt to entity_pnt, used to pick the shortest
		float[] distance = new float[num_req_rel];
		Arrays.fill(distance, Float.MAX_VALUE);
		
		// arrays of closest entities and their associated intersection point
		Entity[] rel_entity = new Entity[num_req_rel];
		Point3f[] bnd_pos = new Point3f[num_req_rel];
			
		// find the closest matches of entity attachment points to
		// the bounds edges of the 'potential' supporting entities
		for (int i = 0; i < num_req_rel; i++) {
			
			entity_pnt.x = (float)entity_position[0] + xPosValues[i];
			//entity_pnt.y = 0;
			entity_pnt.z = (float)entity_position[2] + zPosValues[i];
			
			ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS targetPosAdjAxis =
				(ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)
				targetAdjustmentAxis[i];
			
			for (int j = 0; j < num_collision; j++) {
				
				Entity ce = collisionList.get(j);
				ZoneEntity ze = entityUtils.getZoneEntity(ce);
				
				OrientedBoundingBox obb = bounds[j];
				obb.getCenter(bnd_center);
				obb.getExtents(bnd_min, bnd_max);
				
				switch(targetPosAdjAxis) {
				case XAXIS:
					if (zone == ze) {
						//bnd_pnt.y = 0;
						bnd_pnt.z = bnd_center.z;
						if (entity_position[0] > bnd_center.x) {
							bnd_pnt.x = bnd_max.x;
						} else {
							bnd_pnt.x = bnd_min.x;
						}
						float dst = entity_pnt.distance(bnd_pnt);
						// rem: ignoring the tolerance for now
						//if (dst < posTolerance[i]) {
						if (dst < distance[i]) {
							distance[i] = dst;
							rel_entity[i] = ce;
							adj_entity_pos.z = bnd_center.z - zPosValues[i];
							bnd_pos[i] = new Point3f(bnd_pnt);
						}
						//}
					}
					break;
					
				case ZAXIS:
					if (zone != ze) {
						//bnd_pnt.y = 0;
						bnd_pnt.x = bnd_center.x;
						/////////////////////////////////////////////
						// rem: hard wiring the cross zone connection
						// to always be on the -z side of the bounds
						bnd_pnt.z = bnd_min.z;
						/////////////////////////////////////////////
						//if (entity_position[2] > bnd_center.z) {
						//	bnd_pnt.z = bnd_max.z;
						//} else {
						//	bnd_pnt.z = bnd_min.z;
						//}
						/////////////////////////////////////////////
						float dst = entity_pnt.distance(bnd_pnt);
						// rem: ignoring the tolerance for now
						//if (dst < posTolerance[i]) {
						if (dst < distance[i]) {
							distance[i] = dst;
							rel_entity[i] = ce;
							adj_entity_pos.x = bnd_center.x - xPosValues[i];
							bnd_pos[i] = new Point3f(bnd_pnt);
						}
						//}
					}
					break;
				}
			}
		}
		
		// validate that a unique entity fulfills each requirement
		boolean valid = true;
		for (int i = 0; i < num_req_rel; i++) {
			Entity e = rel_entity[i];
			if (e != null) {
				for (int j = i + 1; j < num_req_rel; j++) {
					if (e == rel_entity[j]) {
						// an entity is duplicated on the rel_entity list
						valid = false;
						break;
					}
				}
				if (!valid) {
					break;
				}
			} else {
				// an entity was not found to fulfill a relationship requirement
				valid = false;
				break;
			}
		}
		if (!valid) {
			return(null);
		}
		////////////////////////////////////////////////////////////////////////
		// all requirements have been met, calculate the adjustments
		
		HashMap<PositionableEntity, Vector3f> entityAdjustmentMap = 
			new HashMap<PositionableEntity, Vector3f>();
		for (int i = 0; i < num_req_rel; i++) {
			
			PositionableEntity pe = (PositionableEntity)rel_entity[i];
			
			entity_pnt.x = adj_entity_pos.x + xPosValues[i];
			//entity_pnt.y = 0;
			entity_pnt.z = adj_entity_pos.z + zPosValues[i];
			
			ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS targetPosAdjAxis =
				(ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)
				targetAdjustmentAxis[i];
			
			Vector3f adj = new Vector3f();
			switch(targetPosAdjAxis) {
			case XAXIS:
				adj.x = entity_pnt.x - bnd_pos[i].x;
				break;
				
			case ZAXIS:
				////////////////////////////////////////////////
				// rem: unless some margin is shaved off the z,
				// subsequent moves and placement of shelves
				// fail for not intersecting the cross zone
				// entity. 1mm seems to do it.
				adj.z = entity_pnt.z - bnd_pos[i].z - 0.001f;
				////////////////////////////////////////////////
				break;
			}
			
			Entity parent = SceneHierarchyUtility.getExactParent(model, pe);
			if (parent != zone) {
				// get the adjustment into the entity's coordinate system
				zone_mtx.transform(adj);
				Matrix4f mtx = new Matrix4f();
				entityUtils.getTransformToRoot(parent, mtx);
				mtx.invert();
				mtx.transform(adj);
			}
			entityAdjustmentMap.put(pe, adj);
		}
		entity_adjustment[0] = adj_entity_pos.x - entity_position[0];
		entity_adjustment[1] = 0;
		entity_adjustment[2] = adj_entity_pos.z - entity_position[2];
		
		return(entityAdjustmentMap);
    }
    
    /**
     * Determine the starting index for position value analysis
     *
     * @param classRelationships Assigned class relationships
     * @param relationshipAmounts Assigned relationship amounts
     * @param matchingClassIndex Legal class relationship index
     * @return starting position index to work from
     */
    public static int calculateStartPosIndex(
            String[] classRelationships,
            int[] relationshipAmounts,
            int matchingClassIndex){

        int startIndex = 0;

        for (int i = 0; i < matchingClassIndex; i++){

            if (classRelationships[i].contains(":")){

                String[] splitList = classRelationships[i].split(":");

                startIndex += (splitList.length * relationshipAmounts[i]);

            } else {

                startIndex += relationshipAmounts[i];
            }
        }

        return startIndex;
    }

    /**
     * Determine the ending index for position value analysis
     *
     * @param classRelationships Assigned class relationships
     * @param relationshipAmounts Assigned relationship amounts
     * @param matchingClassIndex Legal class relationship index
     * @return starting position index to work from
     */
    public static int calculateEndPosIndex(
            String[] classRelationships,
            int[] relationshipAmounts,
            int matchingClassIndex){

        int endIndex = 0;

        for (int i = 0; i < matchingClassIndex+1; i++){

            if (classRelationships[i].contains(":")){

                String[] splitList = classRelationships[i].split(":");

                endIndex += (splitList.length * relationshipAmounts[i]);

            } else {

                endIndex += relationshipAmounts[i];
            }
        }

        return endIndex;
    }
    
    /**
     * Build the full relationship set that we need to line up positions with.
     * Splits list up into string array.
     *
     * @param classRelationship Single class relationship satisfied by
     * collisions
     * @param relationshipAmount Single relationship amount tied to
     * classRelationship
     * @return String[] of relationships
     */
    public static String[] buildFullRelationshipList(
            String classRelationship,
            int relationshipAmount){

        String[] relationships;

        if (classRelationship.contains(":")){

            String[] tokens = classRelationship.split(":");
            relationships =
                new String[tokens.length*relationshipAmount];

            for(int i = 0; i < relationships.length; i++){

                relationships[i] = tokens[((int)(i/relationshipAmount))];
            }

        } else {

            relationships = new String[relationshipAmount];

            for(int i = 0; i < relationships.length; i++){

                relationships[i] = classRelationship;
            }
        }

        return relationships;
    }
    
    /**
     * Creates a dummy command that is used to determine the starting collisions
     * All commands are created with the start pos, start scale and start rot,
     * depending on the command.
     * None of these commands are ever actually fired.
     *
     * @param model WorldModel to reference
     * @param entity Entity affected by command
     * @param command Command affecting entity
     * @return Resulting dummy command
     */
    public static Command createDummyCommand(
    		WorldModel model,
    		Entity entity,
    		Command command) {

        double[] pos = new double[3];
        float[] scale = new float[3];
        float[] rot = new float[4];

        double[] cpos = new double[3];

        PositionableEntity pe;

        if(!(entity instanceof PositionableEntity)) {
            return null;
        }

        pe = (PositionableEntity) entity;

        pe.getStartingPosition(pos);
        pe.getStartingScale(scale);
        pe.getStartingRotation(rot);

        pe.getPosition(cpos);

        Command returnCmd = null;
        if (command instanceof MoveEntityCommand) {
            returnCmd = new MoveEntityCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    pos);

        } else if(command instanceof MoveEntityTransientCommand) {
            returnCmd = new MoveEntityTransientCommand(
                    model,
                    command.getTransactionID(),
                    entity.getEntityID(),
                    pos,
                    new float[3]);

        } else if(command instanceof ScaleEntityCommand) {

            returnCmd = new ScaleEntityCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    pos,
                    scale,
                    scale);

        } else if(command instanceof ScaleEntityTransientCommand) {


            returnCmd = new ScaleEntityTransientCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    scale);

        } else if(command instanceof RotateEntityCommand) {


            returnCmd = new RotateEntityCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    rot,
                    rot);

        } else if(command instanceof RotateEntityTransientCommand) {


            returnCmd = new RotateEntityTransientCommand(
                    model,
                    command.getTransactionID(),
                    entity.getEntityID(),
                    rot);

        } else if(command instanceof TransitionEntityChildCommand) {
            TransitionEntityChildCommand tecc =
            	(TransitionEntityChildCommand) command;

            tecc.getStartPosition(pos);
            tecc.getStartingRotation(rot);

            returnCmd = new TransitionEntityChildCommand(
                    model,
                    ((PositionableEntity)entity),
                    ((TransitionEntityChildCommand)command).getStartParentEntity(),
                    pos,
                    rot,
                    ((TransitionEntityChildCommand)command).getStartParentEntity(),
                    pos,
                    rot,
                    command.isTransient());

        } else if(command instanceof RemoveEntityChildCommand) {
            Entity parent = model.getEntity(entity.getParentEntityID());
            returnCmd = new RemoveEntityChildCommand(model,parent,entity,true);

        }

        return returnCmd;
    }
}
