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
import java.util.*;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DefaultErrorReporter;

import org.chefx3d.view.common.CollisionResultHandler;
import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.NearestNeighborMeasurement;
import org.chefx3d.view.common.RuleCollisionChecker;

/**
 * Common handler for collision checking.
 *
 * @author Ben Yarger
 * @version $Revision: 1.21 $
 */
public class RuleCollisionHandler {
	
	/** Should we log failures reasons for rules */
	private static boolean logFailures = false;
		
	/** The current entity collisions shared between implementing rules */
	public ArrayList<Entity> collisionEntities;
	
	/** The current entity-children collisions shared between implementing rules */
	public Map<Entity,ArrayList<Entity>> collisionEntitiesMap;
	
	/** Error reporting utility */
	private ErrorReporter errorReporter;
	
    /** The world model, where the data is stored */
    private WorldModel model;

	/** Collision checking instance */
	private RuleCollisionChecker collisionChecker;
	
    /** NearestNeighborMeasurement instance */
    private NearestNeighborMeasurement nearestNeighbor;

	/** Map of collision data, per entity */
	private Map<Entity, ChildrenMatches> matchesMap;
	
    /** A singleton that tracks all the command queues */
	private CommandSequencer sequencer;
	
	/** Tracked match sets per evaluation */
	private ChildrenMatches matchSets;

	/**
	 * Constructor
	 */
	public RuleCollisionHandler(
		ErrorReporter reporter, 
		WorldModel model,
		EditorView view) {
		
	    matchSets = new ChildrenMatches();
		
		setErrorReporter(reporter);
		this.model = model;
		collisionChecker = view.getRuleCollisionChecker();
		nearestNeighbor = view.getNearestNeighborMeasurement();
		
	    sequencer = CommandSequencer.getInstance();
	    
		matchesMap = new HashMap<Entity, ChildrenMatches>();
	}
	
	/**
	 * Register an error reporter
	 * so that any errors generated can be reported in a nice manner.
	 *
	 * @param reporter The new ErrorReporter to use.
	 */
	public void setErrorReporter(ErrorReporter reporter) {
		if (this.errorReporter != reporter) {
			errorReporter = (errorReporter == null) ? 
				DefaultErrorReporter.getDefaultReporter() : reporter;
		}
	}
	
	/**
	 * Return the results object
	 *
	 * @return The results object
	 */
	public ChildrenMatches getCollisionResults() {
		return(matchSets);
	}
	
	//-------------------------------------------------------------------------
	// Collision check methods
	//-------------------------------------------------------------------------
	
	/**
	 * Perform collision check on command.  Compares the current entity against the scene.
	 * The result is placed into the collisionEntitiesMap.
	 *
	 * @param command Command to examine
	 * @param useSurrogates Flag to use surrogates or not
	 * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
	 */
	public void performCollisionCheck(
		Command command, 
		boolean useSurrogates, 
		boolean useEntityExtendedBounds,
		boolean useTargetsExtendedBounds){
		
		// Make sure we have an EntityCollisionManager
		if(collisionChecker == null){
			errorReporter.fatalErrorReport(
				"Unable to access the collision detection system from the rules.",
				null);
		}
		
		// make sure we have a valid command
		if (command == null || !(command instanceof RuleDataAccessor)) {
			collisionEntities = null;
			return;
		}
		
		// make sure we have an entity
		Entity entity = ((RuleDataAccessor)command).getEntity();
		if (entity == null) {
			collisionEntities = null;
			return;
		}
		
		useEntityExtendedBounds = 
			useEntityExtendedBoundsCheck(entity, useEntityExtendedBounds);
		useTargetsExtendedBounds = 
			useTargetsExtendedBoundsCheck(entity, useTargetsExtendedBounds);
		
		// Get the collision results
//		String url_string = entity.getModelURL();
		
//		if(url_string != null){
			collisionEntities = collisionChecker.submitCommand(
					command, 
					useSurrogates,
					useEntityExtendedBounds, 
					useTargetsExtendedBounds);

//		} else {
//			collisionEntities = null;
//			return;
//		}
		
		// Remove any mapped child that is set to be removed with the current
		// command execution.
		//
		// Also remove any collision that is set to be removed with the current
		// command execution.
		ArrayList<Entity> entitiesBeingRemoved =
			getRemoveCommandEntities();
		
		if(collisionEntities != null) {
			
			for (int k = (collisionEntities.size() - 1); k >= 0; k--) {
				
				Entity colEntity = collisionEntities.get(k);
				
				for (int j = 0; j < entitiesBeingRemoved.size(); j++) {
					
					if (colEntity == entitiesBeingRemoved.get(j)) {
						collisionEntities.remove(k);
						break;
					}
				}
			}
		}
		
		// Perform the forced parent entity addition to the collision set if
		// 1) Entity is now in a placement mode
		// 2) PARENT_AS_COLLISION_ALTERNATE is true
		// 3) the collision set doesn't already contain the entity
		if(command instanceof RuleDataAccessor){
			
			WorldModel model = ((RuleDataAccessor)command).getWorldModel();
			
			Boolean forceParentCollision = (Boolean)
				RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.PARENT_AS_COLLISION_ALTERNATE);
			
			/*
			 * If shadow entity, don't bother with this check.
			 */
			Boolean isShadow =
				(Boolean)entity.getProperty(
				entity.getParamSheetName(),
				Entity.SHADOW_ENTITY_FLAG);
			
			if (isShadow != null && isShadow == true) {
				return;
			}
			
			if(forceParentCollision != null && forceParentCollision == true){
				
				int parentEntityID = entity.getParentEntityID();
				
				Entity parentEntity = model.getEntity(parentEntityID);
				
				// Need to handle add command sepecial cases
				if(command instanceof AddEntityChildCommand){
					
					parentEntity =
						((AddEntityChildCommand)command).getParentEntity();
					
				} else if (command instanceof AddEntityChildTransientCommand){
					
					parentEntity =
						((AddEntityChildTransientCommand)command).
						getParentEntity();
					
				}
				
				// Never add a null entity to the collision list
				if(parentEntity == null) {
					return;
				}
				
				// Never add a segment or zone to the collision list
				if(!parentEntity.isModel()) {
					return;
				}
				
				for(int i = 0; i < collisionEntities.size(); i++) {
					
					if(collisionEntities.get(i).getEntityID() ==
						parentEntity.getEntityID()){
						
						// Exit and don't bother adding because it is already
						// in the collision set.
						return;
					}
				}
				
				collisionEntities.add(parentEntity);
			}
		}
	}
	
	/**
	 * Perform collision check on command.  Compares the current entity and 
	 * all of its children against the scene.  The result is placed into the 
	 * collisionEntitiesMap.
	 * 
	 * @param command Command to perform extended collision check against
	 * @param useSurrogates True to use surrogates in the collision check, false
	 * to not use surrogates in the collision check
	 * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
	 */
	public void performExtendedCollisionCheck(
			Command command,
			boolean useSurrogates,
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds){
		
		// Make sure we have an EntityCollisionManager
		if(collisionChecker == null){
			
			errorReporter.fatalErrorReport(
				"Unable to access the collision detection system from the rules.",
				null);
		}
		
		// make sure we have an entity
		Entity entity = ((RuleDataAccessor)command).getEntity();
		if (entity == null) {
			collisionEntities = null;
			return;
		}
		
		useEntityExtendedBounds = 
			useEntityExtendedBoundsCheck(entity, useEntityExtendedBounds);
		useTargetsExtendedBounds = 
			useTargetsExtendedBoundsCheck(entity, useTargetsExtendedBounds);
		
		// Get the collision results
		String url_string =
			((RuleDataAccessor)command).getEntity().getModelURL();
		
		if(url_string != null){
			collisionEntitiesMap = 
				collisionChecker.submitCommandExtended(
						command, 
						useSurrogates,
						useEntityExtendedBounds, 
						useTargetsExtendedBounds);
		} else {
			collisionEntitiesMap = null;
		}
		
		// Remove any mapped child that is set to be removed with the current
		// command execution.
		//
		// Also remove any collision that is set to be removed with the current
		// command execution.
		ArrayList<Entity> entitiesBeingRemoved =
			getRemoveCommandEntities();
		
		if(collisionEntitiesMap != null &&
			!collisionEntitiesMap.isEmpty()) {
			
			Object[] keyArray= collisionEntitiesMap.keySet().toArray();
			
			for(int i = 0; i<keyArray.length; i++) {
				
				Entity tmpE = (Entity) keyArray[i];
				boolean tmpERemoved = false;
				
				for (int j = 0; j < entitiesBeingRemoved.size(); j++) {
					
					if (tmpE == entitiesBeingRemoved.get(j)) {
						collisionEntitiesMap.remove(tmpE);
						tmpERemoved = true;
						break;
					}
				}
				
				if (!tmpERemoved) {
					
					ArrayList<Entity> matchedCollisions =
						collisionEntitiesMap.get(tmpE);
					
					if (matchedCollisions != null) {
						
						for (int k = (matchedCollisions.size() - 1);
							k >= 0;
							k--) {
							
							Entity colEntity = matchedCollisions.get(k);
							
							for (int j = 0; j < entitiesBeingRemoved.size(); j++) {
								
								if (colEntity == entitiesBeingRemoved.get(j)) {
									matchedCollisions.remove(k);
									break;
								}
							}
						}
					}
				}
			}
		}
		
		// Remove any mapped child that is an invisible child, in other words
		// has no model.
		if(collisionEntitiesMap != null &&
			!collisionEntitiesMap.isEmpty()) {
			
			Object[] keyArray= collisionEntitiesMap.keySet().toArray();
			
			for(int i = 0; i<keyArray.length; i++) {
				
				Entity currentEntity = (Entity)keyArray[i];
				
				Boolean noModel =
					(Boolean) RulePropertyAccessor.getRulePropertyValue(
					currentEntity,
					ChefX3DRuleProperties.NO_MODEL_PROP);
				
				if (noModel) {
					collisionEntitiesMap.remove(currentEntity);
				}
			}
		}
		
		// Remove any mapped child that is a product zone
		if(collisionEntitiesMap != null &&
				!collisionEntitiesMap.isEmpty()) {
				
			Object[] keyArray= collisionEntitiesMap.keySet().toArray();
			
			for(int i = 0; i<keyArray.length; i++) {
				
				Entity currentEntity = (Entity)keyArray[i];
				
				if (currentEntity.getType() == Entity.TYPE_MODEL_ZONE) {
					collisionEntitiesMap.remove(currentEntity);
				}
			}
		}
	}
	
	
	/**
	 * Collision check used by add commands that results in a
	 * re-parenting action in order to guarantee collision results.
	 * Typically used by auto place actions.
	 *
	 * @param model WorldModel
	 * @param parentEntityParentEntity Parent entity's parent
	 * @param parentEntity Entity's parent Entity
	 * @param entity Entity that started this whole mess.
	 */
	public void performSpecialAddCollisionCheck(
		WorldModel model,
		Entity parentEntityParentEntity,
		Entity parentEntity,
		Entity entity){
		
		/*
		 * Extract and save the original parent entity id and position of the
		 * entity to add back at the end.
		 */
		int originalEntityParentID = entity.getParentEntityID();
		double[] relPosition = new double[3];
		double[] parentPos = new double[3];
		
		((PositionableEntity)entity).getPosition(relPosition);
		((PositionableEntity)parentEntity).getPosition(parentPos);
		
		parentPos[0] = parentPos[0] + relPosition[0];
		parentPos[1] = parentPos[1] + relPosition[1];
		parentPos[2] = parentPos[2] + relPosition[2];
		
		float[] eBounds = new float[6];
		((PositionableEntity)entity).getBounds(eBounds);
		
		entity.setParentEntityID(parentEntityParentEntity.getEntityID());
		((PositionableEntity)entity).setPosition(parentPos, false);
		
		AddEntityChildCommand addChildCmd =
			new AddEntityChildCommand(
					model, 
					model.issueTransactionID(),
					parentEntityParentEntity,
					entity,
					true);
		
		performCollisionCheck(addChildCmd, true, false, false);
		
		/*
		 * Reset the original parent id and position.
		 */
		entity.setParentEntityID(originalEntityParentID);
		((PositionableEntity)entity).setPosition(relPosition, false);
	}
	
	/**
	 * Create a dummy command to use in collision tests with entities that
	 * are not affected by the existing command options. The result is not
	 * to be used in issuing commands. Use SceneManagementUtilties for that.
	 * The resulting command is only to be used in collision checks.
	 * 
	 * @param model WorldModel to reference
	 * @param entity Entity to create dummy command for
	 * @param exact True to use exact position, false to use previous frame
	 * position
	 * @param useStartPosition True to use the start position of the entity
	 * @return Command to use in collision tests
	 */
	public Command createCollisionDummyCommand(
			WorldModel model,
			PositionableEntity entity,
			boolean exact,
			boolean useStartPosition) {
		
		// If not yet in the scene, create an add command at the position
		// already set with the exact parent.
		if (entity.getParentEntityID() == -1) {
			
			Entity parent = SceneHierarchyUtility.getExactParent(model, entity);
			AddEntityChildCommand addCmd = 
				new AddEntityChildCommand(
						model,
						model.issueTransactionID(),
						parent, 
						entity,
						true);
			
			return addCmd;
		}
		
		Entity parent = null;
		double[] position = new double[3];
		float[] rotation = new float[4];
		
		// Since it is already in the scene, we can issue a move command 
		// to do the testing with.
		if (useStartPosition) {
			
			parent = SceneHierarchyUtility.getExactStartParent(model, entity);
			position = TransformUtils.getStartPosition(entity);
			rotation = TransformUtils.getStartRotation(entity);
			
		} else {
			
			if (exact) {
				
				parent = SceneHierarchyUtility.getExactParent(model, entity);
				position = TransformUtils.getExactPosition(entity);
				rotation = TransformUtils.getExactRotation(entity);
				
			} else {
				
				parent = SceneHierarchyUtility.getParent(model, entity);
				position = TransformUtils.getPosition(entity);
				rotation = TransformUtils.getRotation(entity);
				
			}
		}
		
		TransitionEntityChildCommand tranCmd = 
			new TransitionEntityChildCommand(
					model, 
					entity, 
					parent, 
					position, 
					rotation, 
					parent, 
					position, 
					rotation, 
					false);
		
		return tranCmd;
	}
	
	//-------------------------------------------------------------------------
	// Collision analysis methods
	//-------------------------------------------------------------------------
	
	/**
	 * Shortcut helper for most typical collision analysis checks.
	 *
	 * @param entity Entity to extract requisite data from
	 * @param ghostEntities Ghost entities not yet in the scene to consider
	 * @param ignoreSameClass Ignore collision objects with same classification
	 * @param ignoreEntityIdList List of entity id's to ignore in collision set
	 * @param ignoreChildren Flags if the children should be ignored
	 * @return True if succeeded, false if there was a problem extracting data
	 */
	public boolean performCollisionAnalysisHelper(
		Entity entity,
		Entity[] ghostEntities,
		boolean ignoreSameClass,
		int[] ignoreEntityIdList,
		boolean ignoreChildren){
		
		String[] classifications = (String[])
			RulePropertyAccessor.getRulePropertyValue(
			entity,
			ChefX3DRuleProperties.CLASSIFICATION_PROP);
		
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
		
		String[] colReplaceClass = (String[])
			RulePropertyAccessor.getRulePropertyValue(
			entity,
			ChefX3DRuleProperties.REPLACE_PROD_CLASS_PROP);
		
		if(classRelationship == null ||
			relationshipAmount == null ||
			relModifier == null){
			
			return false;
		}
		
		ArrayList<Entity> childrenToIgnore = new ArrayList<Entity>();
		
		if (ignoreChildren) {
			childrenToIgnore = SceneHierarchyUtility.getExactChildren(entity);//entity.getChildren();
		}
		
		int maxIndex = Math.min(
			(Math.min(
			classRelationship.length,
			relationshipAmount.length)),
			relModifier.length);
		
		return performCollisionAnalysis(
			classRelationship,
			classifications,
			childrenToIgnore,
			colReplaceClass,
			maxIndex,
			ghostEntities,
			ignoreSameClass,
			ignoreEntityIdList);
	}
	
	/**
	 * Shortcut helper for most typical collision analysis checks for children.
	 *
	 * @param ghostEntities Ghost entities not yet in the scene to consider
	 * @param ignoreSameClass Ignore collision objects with same classification
	 * @param ignoreEntityIdList List of entity id's to ignore in collision set
	 * @return True if succeeded, false if there was a problem extracting data
	 */
	public boolean performExtendedCollisionAnalysisHelper(
		Entity[] ghostEntities,
		boolean ignoreSameClass,
		int[] ignoreEntityIdList){
		
		if(collisionEntitiesMap == null ||
			collisionEntitiesMap.isEmpty())
			return false;
		
		Object[] keyArray= collisionEntitiesMap.keySet().toArray();
		
		for(int i = 0 ; i<keyArray.length;i++) {
			
			Entity currentEntity = (Entity)keyArray[i];
			
			String[] classifications = (String[])
				RulePropertyAccessor.getRulePropertyValue(
				currentEntity,
				ChefX3DRuleProperties.CLASSIFICATION_PROP);
			
			String[] classRelationship = (String[])
				RulePropertyAccessor.getRulePropertyValue(
				currentEntity,
				ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
			
			int[] relationshipAmount = (int[])
				RulePropertyAccessor.getRulePropertyValue(
				currentEntity,
				ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP);
			
			Enum[] relModifier = (Enum[])
				RulePropertyAccessor.getRulePropertyValue(
				currentEntity,
				ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP);
			
			String[] colReplaceClass = (String[])
				RulePropertyAccessor.getRulePropertyValue(
				currentEntity,
				ChefX3DRuleProperties.REPLACE_PROD_CLASS_PROP);
			
			
			if(classRelationship == null ||
				relationshipAmount == null ||
				relModifier == null){
				
				return false;
			}
			
			int maxIndex = Math.min(
				(Math.min(
				classRelationship.length,
				relationshipAmount.length)),
				relModifier.length);
			
			
			if(matchesMap == null) {
				matchesMap = new HashMap<Entity, ChildrenMatches>();
			}
			
			ChildrenMatches match = new ChildrenMatches();
			
			ArrayList<Entity> childrenList = 
				SceneHierarchyUtility.getExactChildren(currentEntity);
			
			performExtendedCollisionAnalysis(
				collisionEntitiesMap.get(currentEntity),
				match,
				classRelationship,
				classifications,
				childrenList,
				colReplaceClass,
				maxIndex,
				ghostEntities,
				ignoreSameClass,
				ignoreEntityIdList);
			this.matchesMap.put(currentEntity, match);
			
		}
		
		return false;
	}
	
	
	/**
	 * Begin collision results analysis:
	 *
	 * Separate collisions into the following types:
	 * TYPE_MODEL / TYPE_MODEL_WITH_ZONE	(classification relationship matching only)
	 * TYPE_GROUNDPLANE_ZONE        		(floor)
	 * TYPE_SEGMENT     					(wall)
	 * TYPE_MODEL_ZONE						(Model zones)
	 * TYPE_ZONE							(Generic zone)
	 *
	 * @param classRelationship String[] of relationship names of current entity
	 * @param classification String[] of classification names of current entity - used only if ignoreSameClass is true
	 * @param childrenToIgnore ArrayList<Entity> children to remove from collision list
	 * @param classificationToReplace String[] of classifications to replace
	 * @param maxIndex Number of classRelationship indexes to check
	 * @param vaporEntities Entity[] entities not yet in the scene to consider
	 * @param ignoreSameClass If true collision objects with same
	 * classification will be ignored
	 * @param ignoreEntityIdList list of entity id's to ignore
	 * @return True if ok, false if couldn't match valid collisions to categories
	 */
	public boolean performCollisionAnalysis(
		String[] classRelationship,
		String[] classification,
		ArrayList<Entity> childrenToIgnore,
		String[] classificationToReplace,
		int maxIndex,
		Entity[] vaporEntities,
		boolean ignoreSameClass,
		int[] ignoreEntityIdList){
		
		matchSets.clearAll();
		
		// If nothing to compare, stop analysis
		if(classRelationship == null){
			return true;
		}
		
		/*
		 * Add entities to the collision list that otherwise would never
		 * be picked up in a collision case. Allows forced compliance
		 * with collision requirements.
		 */
		if(vaporEntities != null){
			
			if(collisionEntities == null){
				collisionEntities = new ArrayList<Entity>();
			}
			
			for(int i = 0; i < vaporEntities.length; i++){
				
				if(!collisionEntities.contains(vaporEntities[i])){
					collisionEntities.add(vaporEntities[i]);
				}
			}
		}
		
		if(collisionEntities == null){
			return true;
		}
		
		// Remove specific entities from the collision list
		if(ignoreEntityIdList != null){
			
			for(int i = 0; i < ignoreEntityIdList.length; i++){
				
				for(int j = collisionEntities.size() - 1; j >= 0; j--){
					
					if(collisionEntities.get(j).getEntityID() ==
						ignoreEntityIdList[i]){
						
						collisionEntities.remove(j);
					}
				}
			}
		}
		
		/*
		 * Remove children from collision entities list
		 */
		if(childrenToIgnore != null){
			for(int i = 0; i < childrenToIgnore.size(); i++){
				
				if(collisionEntities.contains(childrenToIgnore.get(i))){
					
					collisionEntities.remove(childrenToIgnore.get(i));
				}
			}
		}
		
		/*
		 * Loop through all of the entities in collision with moving entity.
		 * Look for class relationships to define legal collisions and
		 * build up the map of relationship names to entities.
		 */
		ArrayList<String> collisionClassificationList = new ArrayList<String>();
		ArrayList<Entity> collisionEntityList = new ArrayList<Entity>();
		
		for(int w = collisionEntities.size() - 1; w >= 0; w--){
			
			Entity entityObj = collisionEntities.get(w);
			
			String[] colObjClass = (String[])
				RulePropertyAccessor.getRulePropertyValue(
				entityObj,
				ChefX3DRuleProperties.CLASSIFICATION_PROP);
			
			if(colObjClass != null){
				
				/*
				 * Extract children from collision entities that
				 * would be replaced by entity.
				 */
				if(classificationToReplace != null){
					
					boolean classMatchFound = false;
					
					for(int i = 0; i < classificationToReplace.length; i++){
						
						for(int j = 0; j < colObjClass.length; j++){
							
							if(classificationToReplace[i].equals(
								colObjClass[j])){
								
								matchSets.addReplaceEntity(entityObj);
								collisionEntities.remove(w);
								classMatchFound = true;
								break;
							}
						}
						
						if(classMatchFound){
							break;
						}
					}
					
					if(classMatchFound){
						continue;
					}
				}
				
				/*
				 * If ignoreSameClass is set, look for same classification
				 * name and if found skip evaluation for that index, and
				 * remove the entity from the collisionEntities list.
				 */
				if(ignoreSameClass && classification != null){
					
					boolean classMatchFound = false;
					
					for(int i = 0; i < colObjClass.length; i++){
						
						for(int j = 0; j < classification.length; j++){
							
							if(colObjClass[i].equals(classification[j])){
								classMatchFound = true;
								collisionEntities.remove(w);
								break;
							}
						}
						
						if(classMatchFound){
							break;
						}
					}
					
					if(classMatchFound){
						continue;
					}
				}
				
				for(int i = 0; i < colObjClass.length; i++){
					
					collisionClassificationList.add(colObjClass[i]);
					collisionEntityList.add(entityObj);
				}
			}
		}
		
		/*
		 * Make sure collision matches a relationship classification type.
		 * If the classRelationship contains the : character, tokenize the
		 * string and examine each part for a match. Either way, check
		 * matches for the entity type and assign to the correct list.
		 */
		for(int i = 0; i < maxIndex; i++){
			
			// Check for : character
			if(classRelationship[i].contains(":")){
				
				// For proper analysis we cannot have multiple entries with
				// the same name in a relClass so clean it
				HashMap<String, Integer> cleanedClassRelationship =
					cleanClassRelationship(classRelationship[i]);
				
				Object[] keys =
					cleanedClassRelationship.keySet().toArray();
				
				String token;
				
				for (int x = 0; x < keys.length; x++) {
					
					token = (String) keys[x];
					
					for(int w = 0; w < collisionClassificationList.size(); w++){
						
						String colClass = collisionClassificationList.get(w);
						
						if(colClass.equals(token)){
							
							Entity entityObj = collisionEntityList.get(w);
							
							matchSets.addEntity(entityObj, colClass);
						}
					}
				}
				
			} else {
				
				for(int w = 0; w < collisionClassificationList.size(); w++){
					
					String colClass = collisionClassificationList.get(w);
					
					if(colClass.equals(classRelationship[i])){
						
						Entity entityObj = collisionEntityList.get(w);
						
						matchSets.addEntity(entityObj, colClass);
					}
				}
			}
			
			int validMatches = matchSets.getNumberOfValidMatches();
			
			if (validMatches != collisionEntities.size()){
				
				// Build up the illegal collisions consisting of everything in 
				// the original collisionEntities minus everything parsed out
				// into the legal matches.
				
				ArrayList<Entity> illegalEntities = 
					new ArrayList<Entity>(collisionEntities);
				illegalEntities.removeAll(matchSets.getEntityMatches());
				illegalEntities.removeAll(matchSets.getEntityZoneMatches());
				illegalEntities.removeAll(matchSets.getFloorEntityMatches());
				illegalEntities.removeAll(matchSets.getWallEntityMatches());
				illegalEntities.removeAll(matchSets.getZoneMatches());
				
				// If the illegal collisions contain only product zones
				// then this does not constitute an illegal collision
				// and we will return true making this a valid set.
				// Any other entities in the illegal collisions make the
				// collision set illegal and we return false.
				
				boolean allProductZones = true;
				
				for (Entity illegalEntity : illegalEntities) {
					if (illegalEntity.getType() != Entity.TYPE_MODEL_ZONE) {
						allProductZones = false;
						break;
					}
				}
				
				if (allProductZones) {
					return true;
				}
				
				// There are illegal collisions that are not product zones
				// so proceed with standard failure processing.
				
				matchSets.clearAll();
				
				for (Entity illegalEntity : illegalEntities) {
					matchSets.addIllegalEntity(illegalEntity);
				}
				
			} else {
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Begin collision results analysis for collision tree results. These
	 * results come from an entity and all of its children being evaluated for
	 * collisions at the same time.
	 *
	 * Separate collisions into the following types:
	 * TYPE_MODEL / TYPE_MODEL_WITH_ZONE	(classification relationship matching only)
	 * TYPE_GROUNDPLANE_ZONE        		(floor)
	 * TYPE_SEGMENT     					(wall)
	 * TYPE_MODEL_ZONE						(Model zones)
	 * TYPE_ZONE							(Generic zone)
	 *
	 * @param collisionList Collision results for the specific entity
	 * @param matches Specific entity in the tree of entities tested
	 * @param classRelationship String[] of relationship names of current entity
	 * @param classification String[] of classification names of current entity - used only if ignoreSameClass is true
	 * @param childrenToIgnore ArrayList<Entity> children to remove from collision list
	 * @param classificationToReplace String[] of classifications to replace
	 * @param maxIndex Number of classRelationship indexes to check
	 * @param vaporEntities Entity[] entities not yet in the scene to consider
	 * @param ignoreSameClass If true collision objects with same
	 * classification will be ignored
	 * @param ignoreEntityIdList list of entity id's to ignore
	 * @return True if ok, false if couldn't match valid collisions to categories
	 */
	public boolean performExtendedCollisionAnalysis(
		ArrayList<Entity> collisionList,
		CollisionResultHandler matches,
		String[] classRelationship,
		String[] classification,
		ArrayList<Entity> childrenToIgnore,
		String[] classificationToReplace,
		int maxIndex,
		Entity[] vaporEntities,
		boolean ignoreSameClass,
		int[] ignoreEntityIdList){
		
		// Replace the collisionEntities list with the specific collision
		// results for the matched set
		if(collisionEntities == null){
			collisionEntities = new ArrayList<Entity>();
		}
		
		collisionEntities.clear();
		collisionEntities.addAll(collisionList);
		
		boolean result = performCollisionAnalysis(
			classRelationship,
			classification,
			childrenToIgnore,
			classificationToReplace,
			maxIndex,
			vaporEntities,
			ignoreSameClass,
			ignoreEntityIdList);
		
		matches.clearAll();
		matches.set(matchSets);
		
		return result;
	}
	
	//-------------------------------------------------------------------------
	// Has illegal collision checks
	//-------------------------------------------------------------------------	
	
	/**
	 * Helper to extract required rule properties from the entity and hand off
	 * to illegal collision check.
	 *
	 * @param entity Entity to examine
	 * @return True if there are illegal collisions, false otherwise
	 */
	public boolean hasIllegalCollisionHelper(Entity entity){
		
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
		
		
		if(classRelationship == null ||
			relationshipAmount == null ||
			relModifier == null){
			
			// allow model zones to have none by default
			if (entity.getType() == Entity.TYPE_MODEL_ZONE) {
				return false;
			} else {
				return true;
			}
		}
		
		boolean result =
			hasIllegalCollisions(
			classRelationship,
			relationshipAmount,
			relModifier);
		
		return result;
	}
	
	/**
	 * Helper to extract required rule properties from the entity and hand off
	 * to illegal collision check.
	 *
	 * @return True if there are illegal collisions, false otherwise
	 */
	public boolean hasIllegalCollisionExtendedHelper(){
		
		if(collisionEntitiesMap == null)
			return true;
		
		if(collisionEntitiesMap.isEmpty())
			return false;
		
		Object[] keyArray= collisionEntitiesMap.keySet().toArray();
		
		for(int i = 0 ; i<keyArray.length;i++) {
			
			Entity entity = (Entity)keyArray[i];
			String[] classRelationship = (String[])
				RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
			
			int[] relationshipAmount = (int[])
				RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP);
			
			Enum[] relModifier =
				(Enum[])
				RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP);
			
			// allow model zones to have none by default
			if (entity.getType() == Entity.TYPE_MODEL_ZONE) {
				continue;
			}

			//
			// EMF: If there are no collisions, and the relationships
			// do not allow zero collisions, return true: illegal state
			//
			if(collisionEntitiesMap.get(entity).size() == 0){				
				if(!Arrays.asList(classRelationship).contains(
					ChefX3DRuleProperties.RESERVED_NONE_RELATIONSHIP))
					return true;
			}
			
			if(classRelationship == null ||
				relationshipAmount == null ||
				relModifier == null){
				
				return true;
			}
			
			ChildrenMatches match = matchesMap.get(entity);
			
			boolean result =
				hasIllegalCollisions(
				match,
				classRelationship,
				relationshipAmount,
				relModifier);
			
			if(result==true) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Get the index of the legal relationship identified.
	 * 
	 * @param entity Entity to examine
	 * @return Index of the valid relationship, -1 otherwise
	 */
	public int getLegalRelationshipIndex(Entity entity) {
		
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
		
		// Each legal collision check will return the index of the legal case
		// that was found or -1 for nothing to evaluate. If -2 is returned in
		// any of these cases that indicates the data provided did not result
		// in any matches and therefore should be handled appropriately.
		
		int legalModelRes= hasLegalCollisions(
				matchSets.getEntityMatchCountMap(),
				classRelationship,
				relationshipAmount,
				relModifier);
		
		int legalFloorRes = hasLegalCollisions(
				matchSets.getFloorMatchCountMap(), 
				classRelationship, 
				relationshipAmount, 
				relModifier);
		
		int legalSegRes = hasLegalCollisions(
				matchSets.getWallMatchCountMap(),
				classRelationship,
				relationshipAmount,
				relModifier);
		
		int legalModelZoneRes = hasLegalCollisions(
				matchSets.getEntityZoneMatchCountMap(), 
				classRelationship, 
				relationshipAmount, 
				relModifier);
		
		int legalZoneRes = hasLegalCollisions(
				matchSets.getZoneMatchCountMap(),
				classRelationship,
				relationshipAmount,
				relModifier);
		
		int crossCategoryRes = checkCrossCategoryCollisions(
			classRelationship,
			relationshipAmount,
			relModifier);
		
		// If cross category check comes back as valid, then we don't care
		// about anything else. If it doesn't we want to make sure the
		// straight up individual cases are valid, otherwise we do have
		// an illegal collision.
		if (crossCategoryRes > -1) {
			return crossCategoryRes;
		}
		
		// Check individual cases as a backup.
		if(legalModelRes == -2 ||
			legalFloorRes == -2 ||
			legalSegRes == -2 ||
			legalModelZoneRes == -2 ||
			legalZoneRes == -2){
			
			return -1;	
		}
		
		// Cross category results is a special case since a single
		// relationship could be satisfied and yet this could come
		// back as -2 indicating no satisfied cross classification
		// reference is satisfied. Yes, classification and category
		// are used interchangeably here. If we have satisfied one
		// of the earlier cases, then we don't have to worry
		// about the cross category case. That is what we
		// evaluate here.
		if(legalFloorRes >= 0){
			return legalFloorRes; 
		} else if (legalSegRes >= 0){
			return legalSegRes;
		} else if (legalModelRes >= 0){
			return legalModelRes;
		} else if (legalModelZoneRes >= 0) {
			return legalModelZoneRes;
		} else if (legalZoneRes >= 0) {
			return legalZoneRes;
		}
		
		return -1;
	}
	
	/**
	 * Checks if there are any illegal collisions.
	 *
	 * @param classRelationship String[]
	 * @param relationshipAmount int[]
	 * @param relModifier Enum[]
	 * @return true if there are illegal collisions, false otherwise
	 */
	public boolean hasIllegalCollisions(
		String[] classRelationship,
		int[] relationshipAmount,
		Enum[] relModifier){
		
		// Each legal collision check will return the index of the legal case
		// that was found or -1 for nothing to evaluate. If -2 is returned in
		// any of these cases that indicates the data provided did not result
		// in any matches and therefore should be handled appropriately.
		
		int illegalModelRes= hasLegalCollisions(
				matchSets.getEntityMatchCountMap(),
				classRelationship,
				relationshipAmount,
				relModifier);
		
		int illegalFloorRes = hasLegalCollisions(
				matchSets.getFloorMatchCountMap(), 
				classRelationship, 
				relationshipAmount, 
				relModifier);
		
		int illegalSegRes = hasLegalCollisions(
				matchSets.getWallMatchCountMap(),
				classRelationship,
				relationshipAmount,
				relModifier);
		
		int illegalModelZoneRes = hasLegalCollisions(
				matchSets.getEntityZoneMatchCountMap(), 
				classRelationship, 
				relationshipAmount, 
				relModifier);
		
		int illegalZoneRes = hasLegalCollisions(
				matchSets.getZoneMatchCountMap(),
				classRelationship,
				relationshipAmount,
				relModifier);
		
		int crossCategoryRes = checkCrossCategoryCollisions(
			classRelationship,
			relationshipAmount,
			relModifier);
		
		// If cross category check comes back as valid, then we don't care
		// about anything else. If it doesn't we want to make sure the
		// straight up individual cases are valid, otherwise we do have
		// an illegal collision.
		if (crossCategoryRes > -1) {
			return false;
		}
		
		// Check individual cases as a backup.
		if(illegalModelRes == -2 ||
			illegalFloorRes == -2 ||
			illegalSegRes == -2 ||
			illegalModelZoneRes == -2 ||
			illegalZoneRes == -2){
			
			if (logFailures) {
				
				if (illegalModelRes == -2)
					logFailure("hasIllegalCollisions1 Model");
				if (illegalFloorRes == -2)
					logFailure("hasIllegalCollisions1 Floor");
				if (illegalSegRes == -2)
					logFailure("hasIllegalCollisions1 Segment");
				if (illegalModelZoneRes == -2)
					logFailure("hasIllegalCollisions1 Model Zone");
				if (illegalZoneRes == -2)
					logFailure("hasIllegalCollisions1 Zone");
			}
			return true;
			
		} else if (crossCategoryRes == -2){
			
			// Cross category results is a special case since a single
			// relationship could be satisfied and yet this could come
			// back as -2 indicating no satisfied cross classification
			// reference is satisfied. Yes, classification and category
			// are used interchangeably here. If we have satisfied one
			// of the earlier cases, then we don't have to worry
			// about the cross category case. That is what we
			// evaluate here.
			if(illegalFloorRes >= 0){
				;
			} else if (illegalSegRes >= 0){
				;
			} else if (illegalModelRes >= 0){
				;
			} else if (illegalModelZoneRes >= 0) {
				;
			} else if (illegalZoneRes >= 0) {
				;
			} else {
				if (logFailures)
					logFailure("hasIllegalCollisions2");
				
				return true;
			}
		} else if(matchSets.getIllegalEntities().size() > 0) {
			if (logFailures){
				logFailure("hasIllegalCollisions3");
				printCollisionEntitiesMap(null, true);
				printCollisionEntitiesList(null, true);
			}
			// Immediately check for illegal collision matches
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if there are any illegal collisions.
	 *
	 * @param match ChildrenMatches instance to examine, if null the 
	 * internal ChildrenMatches will be examined
	 * @param classRelationship String[]
	 * @param relationshipAmount int[]
	 * @param relModifier Enum[]
	 * @return true if there are illegal collisions, false otherwise
	 */
	public boolean hasIllegalCollisions(
		ChildrenMatches match,
		String[] classRelationship,
		int[] relationshipAmount,
		Enum[] relModifier){
		
		int illegalModelRes = 0;
		int illegalFloorRes = 0;
		int illegalSegRes = 0;
		int illegalModelZoneRes = 0;
		int illegalZoneRes= 0;
		
		int crossCategoryRes = 0;
		
		// Each legal collision check will return the index of the legal case
		// that was found or -1 for nothing to evaluate. If -2 is returned in
		// any of these cases that indicates the data provided did not result
		// in any matches and therefore should be handled appropriately.
		
		// Start off with the case where no specific match set is passed in.
		// In this case, use the internal match set.
		if(match == null) {
			
			illegalModelRes= hasLegalCollisions(
					matchSets.getEntityMatchCountMap(),
					classRelationship,
					relationshipAmount,
					relModifier);
			
			illegalFloorRes = hasLegalCollisions(
					matchSets.getFloorMatchCountMap(), 
					classRelationship, 
					relationshipAmount, 
					relModifier);
			
			illegalSegRes = hasLegalCollisions(
					matchSets.getWallMatchCountMap(),
					classRelationship,
					relationshipAmount,
					relModifier);
			
			illegalModelZoneRes = hasLegalCollisions(
					matchSets.getEntityZoneMatchCountMap(), 
					classRelationship, 
					relationshipAmount, 
					relModifier);
			
			illegalZoneRes = hasLegalCollisions(
					matchSets.getZoneMatchCountMap(),
					classRelationship,
					relationshipAmount,
					relModifier);
			
			crossCategoryRes = checkCrossCategoryCollisions(
					matchSets,
					classRelationship,
					relationshipAmount,
					relModifier);
			
		}else {
			
			illegalModelRes= hasLegalCollisions(
					match.getEntityMatchCountMap(),
					classRelationship,
					relationshipAmount,
					relModifier);
			
			illegalFloorRes = hasLegalCollisions(
					match.getFloorMatchCountMap(), 
					classRelationship, 
					relationshipAmount, 
					relModifier);
			
			illegalSegRes = hasLegalCollisions(
					match.getWallMatchCountMap(),
					classRelationship,
					relationshipAmount,
					relModifier);
			
			illegalModelZoneRes = hasLegalCollisions(
					match.getEntityZoneMatchCountMap(), 
					classRelationship, 
					relationshipAmount, 
					relModifier);
			
			illegalZoneRes = hasLegalCollisions(
					match.getZoneMatchCountMap(),
					classRelationship,
					relationshipAmount,
					relModifier);
			
			crossCategoryRes = checkCrossCategoryCollisions(
				match,
				classRelationship,
				relationshipAmount,
				relModifier);
			
		}
		
		// If cross category check comes back as valid, then we don't care
		// about anything else. If it doesn't we want to make sure the
		// straight up individual cases are valid, otherwise we do have
		// an illegal collision.
		if (crossCategoryRes > -1) {
			return false;
		}
		
		if(illegalModelRes == -2 ||
				illegalFloorRes == -2 ||
				illegalSegRes == -2 ||
				illegalModelZoneRes == -2 ||
				illegalZoneRes == -2){
			
			return true;
			
		} else if (crossCategoryRes == -2){
			
			// Cross category results is a special case since a single
			// relationship could be satisfied and yet this could come
			// back as -2 indicating no satisfied cross classification
			// reference is satisfied. Yes, classification and category
			// are used interchangeably here. If we have satisfied one
			// of the earlier cases, then we don't have to worry
			// about the cross category case. That is what we
			// evaluate here.
			if(illegalFloorRes >= 0){
				;
			} else if (illegalSegRes >= 0){
				;
			} else if (illegalModelRes >= 0){
				;
			} else if (illegalModelZoneRes >= 0) {
				;
			} else if (illegalZoneRes >= 0) {
				;
			} else {
				return true;
			}
			
		} else if(match == null) {
			
			if(matchSets.getIllegalEntities().size() > 0) {
				
				
				//Immediately check for illegal collision matches
				return true;
			}
		}else {
			if(match.getIllegalEntities().size() > 0) {
				
				
				//Immediately check for illegal collision matches
				return true;
			}
		}
		
		return false;
	}
	
	//-------------------------------------------------------------------------
	// Specific legal state checks
	//-------------------------------------------------------------------------
	
	/**
	 * If there aren't any collision results for an entity, it is required the 
	 * None relationship is specified to allow it to be legal.
	 *
	 * @return True if the None relationship is defined, false otherwise
	 */
	public boolean legalZeroCollisionCheck(Entity entity) {
		
		// If the reserved class relationship of None is used then,
		// we will allow this case.
		String[] relClass = (String[])
			RulePropertyAccessor.getRulePropertyValue(
			entity,
			ChefX3DRuleProperties.
			RELATIONSHIP_CLASSIFICATION_PROP);
		
		if(relClass != null) {
			
			for(int i = 0; i < relClass.length; i++) {
				
				if(relClass[i].equalsIgnoreCase(
					ChefX3DRuleProperties.
					RESERVED_NONE_RELATIONSHIP)) {
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Makes sure the collisionEntitiesMap results are appropriate in that
	 * there are results to examine. If there aren't any collision results for
	 * an entity, it is required the None relationship is specified to allow
	 * it to be legal.
	 *
	 * @return True if collision results exist, false otherwise
	 */
	public boolean legalZeroCollisionExtendedCheck() {
		
		if(collisionEntitiesMap.size() > 0){
			
			// clone the other properties
			Iterator<Map.Entry<Entity, ArrayList<Entity>>> collisionSet =
				collisionEntitiesMap.entrySet().iterator();
			
			while (collisionSet.hasNext()) {
				
				Map.Entry<Entity, ArrayList<Entity>> mapEntry =
					collisionSet.next();
				
				Entity entity = mapEntry.getKey();
				
				// allow model zones to have none by default
				if (entity.getType() == Entity.TYPE_MODEL_ZONE) {
					return true;
				}
				
				ArrayList<Entity> collisionResults = mapEntry.getValue();
				
				if (collisionResults.size() < 1) {
					
					// If the reserved class relationship of None is used then,
					// we will allow this case.
					String[] relClass = (String[])
						RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.
						RELATIONSHIP_CLASSIFICATION_PROP);
					
					if(relClass != null) {
						
						for(int i = 0; i < relClass.length; i++) {
							
							if(relClass[i].equalsIgnoreCase(
								ChefX3DRuleProperties.
								RESERVED_NONE_RELATIONSHIP)) {
								
								return true;
							}
						}
					}
					
					return false;
				}
			}
		}
		
		return true;
	}	
	
	/**
	 * Determines if the zone collisions are all legal. If any are not legal
	 * collisions, the check will return either -2 or -1 depending on the
	 * issue.
	 *
	 * @param countMap The count map to check for legal collisions
	 * @param classRelationship String[]
	 * @param relationshipAmount int[]
	 * @param relModifier Enum[]
	 * @return index of matching classification, otherwise -1 if nothing to
	 * check, -2 if illegal collision found
	 */
	public int hasLegalCollisions(
		HashMap<String,Integer> countMap,
		String[] classRelationship,
		int[] relationshipAmount,
		Enum[] relModifier){
		
		for(int i = 0; i < classRelationship.length; i++){
			
			// Check for : character, otherwise standard check
			if(classRelationship[i].contains(":")){
				
				// For proper analysis we cannot have multiple entries with
				// the same name in a relClass so clean it
				HashMap<String, Integer> cleanedClassRelationship =
					cleanClassRelationship(classRelationship[i]);
				
				Object[] keys =
					cleanedClassRelationship.keySet().toArray();
				
				String token;
				
				boolean matchesFound = true;
				
				for (int x = 0; x < keys.length; x++) {
					
					token = (String) keys[x];
					int amt = cleanedClassRelationship.get(token);
					
					if (amt == 1) {
						amt = relationshipAmount[i];
					}
					
					
					if(!RuleUtils.legalAssociationNumber(
						countMap.get(token),
						amt,
						(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
						
						matchesFound = false;
						break;
					}
				}
				
				if(!matchesFound){
					continue;
				}
				
				return i;
				
			} else if(RuleUtils.legalAssociationNumber(
				countMap.get(classRelationship[i]),
				relationshipAmount[i],
				(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
				
				return i;
			}
		}
		
		if(countMap != null && countMap.size() > 0){
			
			if (logFailures) {
				
				StringBuilder sb = new StringBuilder();
				for(int i=0; i < classRelationship.length; i++) {
					sb.append("   ");
					sb.append(classRelationship[i]);
					sb.append(" ");
					sb.append(relModifier[i]);
					sb.append(" ");
					sb.append(relationshipAmount[i]);
				}
				logFailure("hasLegalCollisions1 - Expected a collision with: \n" + sb.toString());
				printCollisionEntitiesMap(null, true);
				printCollisionEntitiesList(null, true);
			}
			
			return -2;
		}
		
		return -1;
	}
	
	/**
	 * Determines if the multiple collisions are all legal. If any are not legal
	 * collisions, the check will return either -2 or -1 depending on the
	 * issue.
	 *
	 * @param classRelationship Classification relationships to check for
	 * @param relationshipAmount Relationship amounts to check for
	 * @param relModifier Relationship modifiers to check for
	 * @return index of matching classification, otherwise -1 if not found,
	 * -2 if illegal collision found
	 */
	public int checkCrossCategoryCollisions(
		String[] classRelationship,
		int[] relationshipAmount,
		Enum[] relModifier){
		
		int iterateSize = Math.min(
			classRelationship.length,
			Math.min(relationshipAmount.length, relModifier.length));
		
		boolean containsJoiner = false;
		
		for(int i = 0; i < iterateSize; i++){
			
			// Check for : character only
			if(classRelationship[i].contains(":")){
				
				containsJoiner = true;
				
				// For proper analysis we cannot have multiple entries with
				// the same name in a relClass so clean it
				HashMap<String, Integer> cleanedClassRelationship =
					cleanClassRelationship(classRelationship[i]);
				
				Object[] keys =
					cleanedClassRelationship.keySet().toArray();
				
				String token;
				
				boolean matchesFound = true;
				
				for (int x = 0; x < keys.length; x++) {
					
					token = (String) keys[x];
					int amt = cleanedClassRelationship.get(token);
					
					if (amt == 1) {
						amt = relationshipAmount[i];
					}
					
					if(RuleUtils.legalAssociationNumber(
						matchSets.getEntityMatchCountMap().get(token),
						amt,
						(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
						
						continue;
						
					} else if(RuleUtils.legalAssociationNumber(
						matchSets.getFloorMatchCountMap().get(token),
						amt,
						(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
						
						continue;
						
					} else if(RuleUtils.legalAssociationNumber(
						matchSets.getWallMatchCountMap().get(token),
						amt,
						(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
						
						continue;
						
					} else if(RuleUtils.legalAssociationNumber(
						matchSets.getEntityZoneMatchCountMap().get(token),
						amt,
						(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
							
							continue;
							
					} else if(RuleUtils.legalAssociationNumber(
						matchSets.getZoneMatchCountMap().get(token),
						amt,
						(ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES)relModifier[i])){
							
							continue;
							
					} else {
						matchesFound = false;
						break;
					}
				}
				
				if(matchesFound){
					return i;
				}
			}
		}
		
		if(containsJoiner &&
			(matchSets.getEntityMatchCountMap().size() > 0 ||
					matchSets.getFloorMatchCountMap().size() > 0 ||
					matchSets.getWallMatchCountMap().size() > 0 ||
					matchSets.getEntityZoneMatchCountMap().size() > 0 ||
					matchSets.getZoneMatchCountMap().size() > 0)){
			
			return -2;
		}
		
		return -1;
	}
	
	/**
	 * Determines if the multiple collisions are all legal. If any are not legal
	 * collisions, the check will return either -2 or -1 depending on the
	 * issue.
	 *
	 * @param cm Children matches to use in the cross category collision check,
	 * if null, the internal children matches are used
	 * @param classRelationship Classification relationships to check for
	 * @param relationshipAmount Relationship amounts to check for
	 * @param relModifier Relationship modifiers to check for
	 * @return index of matching classification, otherwise -1 if not found,
	 * -2 if illegal collision found
	 */
	public int checkCrossCategoryCollisions(
		ChildrenMatches cm,
		String[] classRelationship,
		int[] relationshipAmount,
		Enum[] relModifier){
		
		if(cm != null) {
			
			matchSets.set(cm);
		}
		
		int result =
			checkCrossCategoryCollisions(
			classRelationship,
			relationshipAmount,
			relModifier);
		
		return result;
	}
	
	/**
	 * Prunes the collisionEntities arrayList of all entities
	 *  below the current entity in the tree.
	 *
	 *  Example being, if you have a hang track on a wall and prune from a standard
	 *  Any children of the standards will be removed as they are below the standard
	 *  
	 * @param model WorldModel to reference
	 * @param entity Entity to begin pruning from
	 */
	public void pruneCollisionList(WorldModel model, Entity entity) {
		
		int layerNumber = SceneHierarchyUtility.getToZoneCount(model, entity);
		Object[] entityArray = collisionEntities.toArray();
		
		for(int i = 0; i < entityArray.length; i++) {
			
			Entity currentEntity = (Entity)entityArray[i];
			int currentLayer = 
				SceneHierarchyUtility.getToZoneCount(model, currentEntity);
			if(currentLayer > layerNumber) {
				collisionEntities.remove(currentEntity);
			}
			
			// skip over any auto-span item
			Boolean autoSpan =
				(Boolean) RulePropertyAccessor.getRulePropertyValue(
				currentEntity,
				ChefX3DRuleProperties.SPAN_OBJECT_PROP);
			if(autoSpan != null && autoSpan) {
				collisionEntities.remove(currentEntity);
			}
		}
	}
	
	/**
	 * Check a list of collisions for dependency on the entityIdsToIgnore list.
	 * Will check a list of entities to see if any require a specific collision
	 * that does not exist, either because it's no longer where it was expected
	 * in the scene, or it has been removed in the entityIdsToIgnore list. Can
	 * optionally set the ignoreAutoAdded flag to ignore skip Entities in the
	 * collisionList that are auto added entities.
	 *
	 * Method automatically removes all segment and zone entities from collisionList.
	 *
	 * @param model WorldModel to reference
	 * @param modelCollisionList List of entities to check as dependents
	 * @param entityIdsToIgnore Entity id's to ignore
	 * @param ignoreAutoAdded True to ignore auto added products in check
	 * @param rch RuleCollisionHandler to reference
	 * @param view EditorView to reference
	 * @return False if there are no dependencies, true otherwise
	 */
	public boolean hasDependantProductAttached(
		WorldModel model,
		ArrayList<Entity> collisionList,
		int[] entityIdsToIgnore,
		boolean ignoreAutoAdded,
		RuleCollisionHandler rch,
		EditorView view){
		
		// Copy the list over so that future collision checks
		// don't accidently overwrite the original data.
		ArrayList<Entity> modelCollisionList =
			new ArrayList<Entity>(collisionList);
		
		// Remove any segment and zone entities from list. Only operate on
		// type model
		for(int i = (modelCollisionList.size()-1); i >= 0; i--){
			
			if (!modelCollisionList.get(i).isModel()) {
				modelCollisionList.remove(i);
			}
		}
		
		// See if removing the entity from collision set of all collision
		// entities discovered would cause them to have illegal collisions.
		// If it would, then it has dependent products attached.
		
		for(int i = 0; i < modelCollisionList.size(); i++){
			
			int transID = model.issueTransactionID();
			Entity tmpEntity = modelCollisionList.get(i);
			double[] pos = new double[3];
			
			// If flagged, ignore auto added products
			if(ignoreAutoAdded){
				
				Boolean isAutoAddProduct = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
					tmpEntity,
					ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
				
				if(isAutoAddProduct != null && isAutoAddProduct == true){
					continue;
				}
			}
			
			// Can't operate on product without accessible position data
			if(!(tmpEntity instanceof PositionableEntity)){
				
				return true;
			}
			
			Command tmpMvCmd = createCollisionDummyCommand(
				model, 
				(PositionableEntity) tmpEntity, 
				true, 
				false);
		
			// Always do these checks without the extended bounds imposed by
			// collision position requirements.
			Boolean colPosReq = (Boolean)
				RulePropertyAccessor.getRulePropertyValue(
				tmpEntity,
				ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS);
			
			tmpEntity.setProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS,
				false,
				false);
			
			performCollisionCheck(tmpMvCmd, true, false, false);
			
			// Set collision position requirement value back
			tmpEntity.setProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS,
				colPosReq,
				false);
			
			performCollisionAnalysisHelper(
				tmpEntity,
				null,
				false,
				entityIdsToIgnore,
				true);
			
			// If there are illegal collisions without the specified entity id
			// considered in the collision list then we have a dependancy we
			// must respect, so return true.
			if(hasIllegalCollisionHelper(tmpEntity)){
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check to see if the entity should not be moved because of collisions
	 * that are dependent on it.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity to check for collision dependencies on
	 * @param rch RuleCollisionHandler to reference
	 * @param view EditorView to reference
	 * @return True if other entities are dependent on it, false otherwise
	 */
	public boolean isDependantFixedEntity(
		WorldModel model,
		PositionableEntity entity,
		RuleCollisionHandler rch,
		EditorView view){
		
		int transID = model.issueTransactionID();
		
		double[] pos = TransformUtils.getExactPosition(entity);
		
		Command mvCmd = createCollisionDummyCommand(
			model, 
			(PositionableEntity) entity, 
			true, 
			false);
		
		performCollisionCheck(mvCmd, true, true, true);
		
		if (collisionEntities == null) {
			return false;
		}
		
		ArrayList<Entity> dependentCheckSet =
			new ArrayList<Entity>(collisionEntities);
		
		// Remove parent
		for (int i = 0; i < dependentCheckSet.size(); i++) {
			
			if (dependentCheckSet.get(i).getEntityID() ==
				entity.getParentEntityID()) {
				
				dependentCheckSet.remove(i);
				break;
			}
		}
		
		boolean result =
			hasDependantProductAttached(
			model,
			dependentCheckSet,
			new int[] {entity.getEntityID()},
			false,
			rch,
			view);
		
		if (result) {
			return true;
		}
			
		// Need to test install position requirements.
		MoveEntityCommand tmpMvCmd =
			new MoveEntityCommand(
			model,
			transID,
			entity,
			pos,
			pos);
		
		boolean resultA = InstallPositionRequirementUtility.
			hasPositionCollisionRequirementImposed(
					model, 
					tmpMvCmd, 
					entity, 
					rch, 
					view);
		
		boolean resultB = InstallPositionMultiZoneRequirementUtility.
			hasPositionCollisionRequirementsImposed(
					model, 
					tmpMvCmd, 
					entity, 
					rch, 
					view);
		
		if (resultA || resultB) {
			return true;
		}
		
		return false;
	}


	//////////////////////////////////////////////////////////////////////////////
	// rem: this probably doesn't belong in this class, but it wasn't 
	// quick to get around
	//
	// TODO: the CommandSequencer should have this method implemented
	/**
	 * Get the list of entities that are set to be removed with the next
	 * command execution.
	 *
	 * @return ArrayList<Entity> of entities set to be removed
	 */
	public ArrayList<Entity> getRemoveCommandEntities() {
		
	    List<Command> newlyIssuedCommands = 
	        sequencer.getFullCommandList(false);
	    
		ArrayList<Entity> removeEntityList =
			new ArrayList<Entity>();
		
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
	//////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Clean up the relClass into a representative HashMap of the each
	 * expressed relationship with duplicates accounted for in the Integer
	 * value of the HashMap. When evaluating the relAmt, if the Integer
	 * quantity stored in the HashMap is > 1, then it is used in place
	 * of the relAmt specified.
	 *
	 * @param relClass String to parse
	 * @return HashMap<String, Integer> or null if relClass doesn't contain :
	 */
	private HashMap<String, Integer> cleanClassRelationship(String relClass) {
		
		if (!relClass.contains(":")) {
			return null;
		}
		
		HashMap<String, Integer> cleanedResults =
			new HashMap<String, Integer>();
		
		StringTokenizer st = new StringTokenizer(relClass, ":");
		String token;
		
		while(st.hasMoreTokens()){
			
			token = st.nextToken();
			
			Integer count = cleanedResults.get(token);
			
			if (count == null) {
				cleanedResults.put(token, 1);
			} else {
				count++;
				cleanedResults.put(token, count);
			}
		}
		
		return cleanedResults;
	}
	
	/**
	 * Get the children matches.
	 * 
	 * @return ChildrenMatches
	 */
	public ChildrenMatches getChildrenMatches() {
		return matchSets;
	}
	
	/**
	 * Get the RuleCollisionChecker associated with this collision handler
	 * instance.
	 * 
	 * @return RuleCollisionChecker
	 */
	public RuleCollisionChecker getRuleCollisionChecker() {
		return collisionChecker;
	}
	
	/**
	 * Print out the contents of the collision entities map.
	 * 
	 * @param entity Entity the collision check was performed on, can be null
	 * @param exposesCollisions True to expose properties about each collision
	 * false to just print entity names and ID
	 */
	public void printCollisionEntitiesMap(
			Entity entity,
			boolean exposeCollisions) {
		
		System.out.println("");
		System.out.println("*********************************");
		
		if (entity != null) {
			System.out.println("collision results for: "+entity.getName()+
					" ID: "+entity.getEntityID());
		}
		
		if (collisionEntitiesMap == null) {
			System.out.println("CollisionEntities Map is null");
			return;
		}
		
		//------------------------------------------------------
		// Debug section to print out contents of collision
		// LIKELY WANT TO KEEP THIS AROUND!!
		//------------------------------------------------------
		
		Object[] keys = collisionEntitiesMap.keySet().toArray();

		for (int i = 0; i < keys.length; i++) {
			
			Entity entityKey = (Entity) keys[i];
			ArrayList<Entity> collisions = collisionEntitiesMap.get(entityKey);
			
			System.out.println(" Entity: "+entityKey.getName()+
				" ["+entityKey.getEntityID()+"] "+
				" is colliding with ...");
			
			for (int j = 0; j < collisions.size(); j++) {
				String[] classifications = (String[])
					RulePropertyAccessor.getRulePropertyValue(
							collisions.get(j), 
							ChefX3DRuleProperties.CLASSIFICATION_PROP);
				
				System.out.println("  "+j+") "+collisions.get(j).getName()+
						"  -- id: ["+collisions.get(j).getEntityID()+"] "+
						"classifications: "+
						Arrays.toString(classifications));
				
				if (exposeCollisions) {
					ExposeDataUtility.exposeEntity(collisions.get(j));
				}
			}
		}
	}
	
	/**
	 * Print out the contents of the collision list.
	 * 
	 * @param entity Entity the collision check was performed on, can be null
	 * @param exposesCollisions True to expose properties about each collision
	 * false to just print entity names and ID
	 */
	public void printCollisionEntitiesList(
			Entity entity,
			boolean exposesCollisions) {
		
		System.out.println("");
		System.out.println("*********************************");
		
		if (entity != null) {
			System.out.println("collision results for: "+entity.getName()+
					" ID: "+entity.getEntityID());
		}
		
		if (collisionEntities == null) {
			System.out.println("CollisionEntities is null");
			return;
		}
		
		//------------------------------------------------------
		// Debug section to print out contents of collision
		// LIKELY WANT TO KEEP THIS AROUND!!
		//------------------------------------------------------
		
		for (int i = 0; i < collisionEntities.size(); i++) {
			
			Entity colEntity = collisionEntities.get(i);
			
			System.out.println(" Collision found with: "+colEntity.getName()+
				" ["+colEntity.getEntityID()+"]");
			
			if (exposesCollisions) {
				ExposeDataUtility.exposeEntity(colEntity);
			}
		}
	}
	
    /**
     * Get all neighbors in the positive x direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getPositiveXNeighbors(
            PositionableEntity evalEntity,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        NearestNeighborMeasurement.POS_X,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the positive x direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getPositiveXNeighbors(
            PositionableEntity evalEntity,
            String name,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        name,
                        NearestNeighborMeasurement.POS_X,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the negative x direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNegativeXNeighbors(
            PositionableEntity evalEntity,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        NearestNeighborMeasurement.NEG_X,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the negative x direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNegativeXNeighbors(
            PositionableEntity evalEntity,
            String name,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        name,
                        NearestNeighborMeasurement.NEG_X,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the positive y direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getPositiveYNeighbors(
            PositionableEntity evalEntity,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        NearestNeighborMeasurement.POS_Y,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the positive y direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getPositiveYNeighbors(
            PositionableEntity evalEntity,
            String name,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        name,
                        NearestNeighborMeasurement.POS_Y,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the negative y direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNegativeYNeighbors(
            PositionableEntity evalEntity,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        NearestNeighborMeasurement.NEG_Y,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the negative y direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNegativeYNeighbors(
            PositionableEntity evalEntity,
            String name,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        name,
                        NearestNeighborMeasurement.NEG_Y,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the positive z direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getPositiveZNeighbors(
            PositionableEntity evalEntity,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        NearestNeighborMeasurement.POS_Z,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the positive z direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getPositiveZNeighbors(
            PositionableEntity evalEntity,
            String name,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        name,
                        NearestNeighborMeasurement.POS_Z,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the negative z direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNegativeZNeighbors(
            PositionableEntity evalEntity,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        NearestNeighborMeasurement.NEG_Z,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the negative z direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNegativeZNeighbors(
            PositionableEntity evalEntity,
            String name,
            float[] boundsAdj){

        return(
                getNeighbors(
                        evalEntity,
                        name,
                        NearestNeighborMeasurement.NEG_Z,
                        boundsAdj));
    }

    /**
     * Get all neighbors in the specified direction from the given entity.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param direction The direction to check
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNeighbors(
            PositionableEntity evalEntity,
            int direction,
            float[] boundsAdj){

        Entity zoneEntity = SceneHierarchyUtility.findZoneEntity(
                model,
                evalEntity);

        // If the zone entity found if not null, and parentEntityID is defined
        // immediately do the nearest neighbor check.
        //
        // Otherwise, if zone entity is found, see if the parentEntityID was
        // side pocketed. If so, temporarily set the parentEntityID and do the
        // nearest neighbor check.
        //
        // Otherwise, if zone entity cannot be found default to
        // using the active zone entity and check to see if the parentEntityID
        // was side pocketed.
        if (zoneEntity != null &&
                evalEntity.getParentEntityID() != -1) {

            return nearestNeighbor.nearestNeighbors(
                    model,
                    zoneEntity,
                    evalEntity,
                    direction,
                    boundsAdj);

        } else if (zoneEntity != null) {

            // Attempt to temporarily fake the intended parenting
            int originalParentID = evalEntity.getParentEntityID();

            Integer currentParent = (Integer)
                RulePropertyAccessor.getRulePropertyValue(
                        evalEntity,
                        ChefX3DRuleProperties.INITAL_ADD_PARENT);

            if (currentParent == null) {
                return null;
            }

            evalEntity.setParentEntityID(currentParent);

            ArrayList<Entity> results =
                nearestNeighbor.nearestNeighbors(
                    model,
                    zoneEntity,
                    evalEntity,
                    direction,
                    boundsAdj);

            evalEntity.setParentEntityID(originalParentID);

            return results;

        } else {

            zoneEntity = 
            	SceneHierarchyUtility.getActiveZoneEntity(model);

            if (zoneEntity == null) {

                return null;

            } else {

                // Attempt to temporarily fake the intended parenting
                int originalParentID = evalEntity.getParentEntityID();

                Integer currentParent = (Integer)
                    RulePropertyAccessor.getRulePropertyValue(
                            evalEntity,
                            ChefX3DRuleProperties.INITAL_ADD_PARENT);

                if (currentParent == null) {
                    return null;
                }

                evalEntity.setParentEntityID(currentParent);

                ArrayList<Entity> results =
                    nearestNeighbor.nearestNeighbors(
                        model,
                        zoneEntity,
                        evalEntity,
                        direction,
                        boundsAdj);

                evalEntity.setParentEntityID(originalParentID);

                return results;
            }
        }
    }

    /**
     * Get all neighbors in the specified direction that match the
     * product name or classification type of that product.
     *
     * @param evalEntity PositionableEntity to perform neighbor check on
     * @param name String classification type name
     * @param direction The direction to check
     * @param boundsAdj x,y,z axis additional bounds values to consider,
     * can be null if no adjustment to the entity bounds is needed
     * @return ArrayList<Entity> of neighbors or null if unable to check
     */
    public ArrayList<Entity> getNeighbors(
            PositionableEntity evalEntity,
            String name,
            int direction,
            float[] boundsAdj){

        ArrayList<Entity> neighbors = getNeighbors(
                evalEntity,
                direction,
                boundsAdj);

        if (neighbors != null) {

            String[] classifications = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                        evalEntity,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

            for(int i = neighbors.size()-1; i >= 0; i--){

                String tmpName = neighbors.get(i).getName();

                if(!tmpName.equals(name)){

                    String[] tmpClass = (String[])
                        RulePropertyAccessor.getRulePropertyValue(
                                neighbors.get(i),
                                ChefX3DRuleProperties.CLASSIFICATION_PROP);

                    if(tmpClass != null){

                        boolean classMatchFound = false;

                        for(int w = 0; w < classifications.length; w++){

                            for(int x = 0; x < tmpClass.length; x++){

                                if(tmpClass[x].equals(classifications[w])){

                                    classMatchFound = true;
                                    break;
                                }
                            }

                            if(classMatchFound){
                                break;
                            }
                        }

                        if(classMatchFound){
                            continue;
                        }
                    }

                    neighbors.remove(i);
                }
            }
        }
        return neighbors;
    }

    /**
     * Find the most appropriate parent based on legal collision, allowed
     * parents and the shortest z distance between entities. Will not return
     * segments or zones as parent. If no match in the immediate collision
     * set satisfy the allowed children or parenting requirements, then a
     * negative z nearest neighbor cacluation is performed to see if anything
     * behind the entity is a match.
     *
     * If no other entities of type model are
     * found in the collisions, null will be returned in which case the parent
     * set in the command is the best one to use.
     *
     * @param command Command to check collisions with
     * @return Best parent entity, or null if not found
     */
    public Entity findAppropriateParent(
            Command command) {

        // The parent entity to return
        Entity parentEntity = null;

        PositionableEntity pEntity =
            (PositionableEntity) ((RuleDataAccessor)command).getEntity();

        if (pEntity == null) {
            return null;
        }

        //------------------------------------------------
        // Permanent parent check.
        // If set, we want to see if we are in a ghost state (parent not set)
        // If we are, just return null.
        // If we aren't, then return the inital add parent entity.
        //------------------------------------------------
        Boolean usePermanentParent = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                    pEntity,
                    ChefX3DRuleProperties.USES_PERMANENT_PARENT);

        // if the permanent parent is set and this is not an add then
        // we want to keep the same parent instead of looking for one
        if (usePermanentParent && 
        		!(command instanceof AddEntityChildCommand)) {

            Boolean isShadow = (Boolean)
                pEntity.getProperty(pEntity.getParamSheetName(),
                        Entity.SHADOW_ENTITY_FLAG);

            if (isShadow != null && isShadow == false) {

                Boolean parentSet = (Boolean)
                    RulePropertyAccessor.getRulePropertyValue(
                        pEntity,
                        ChefX3DRuleProperties.PERMANENT_PARENT_SET);


                if (parentSet == null || !parentSet) {
                    return null;
                }

                Integer initialAddParent = (Integer)
                    RulePropertyAccessor.getRulePropertyValue(
                        pEntity,
                        ChefX3DRuleProperties.INITAL_ADD_PARENT);

                if (initialAddParent == null) {
                    return null;
                }

                return (model.getEntity(initialAddParent));
            }
        }
        
        Entity activeZone = SceneHierarchyUtility.getActiveZoneEntity(model);

        // The list of legal collisions
        ArrayList<Entity> legalCollisions = new ArrayList<Entity>();

        performCollisionCheck(command, true, false, false);

        String[] allowedParentClassifications = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    pEntity,
                    ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

        legalCollisions = getLegalAllowedParentCollisions(
                collisionEntities,
                pEntity,
                allowedParentClassifications);

        // Based on the legal collisions, find the closest one as the parent
        if (legalCollisions != null && legalCollisions.size() > 0) {
        	
        	// Remove any zones that aren't the active zone.
            // Do not allow parenting to a zone that is not the active zone.
            for (int j = (legalCollisions.size() - 1); j >= 0; j--) {
            	
            	Entity legalCollision = legalCollisions.get(j);
            	
            	if (legalCollision.isZone() && legalCollision != activeZone) {
            		legalCollisions.remove(j);
            	}
            }

            parentEntity =
				getClosestEntity(legalCollisions, pEntity);

        }

        if (parentEntity == null) {

            // We have ended up without any legal collisions to evaluate for a
            // parent. So, we have to do a negative z nearest neighbor bounds
            // check to see if there is anything behind the entity that is a
            // legal parent.

            // Side pocket current data so we can set it back
            int currentParentID = pEntity.getParentEntityID();
            double[] currentPosition = new double[3];
            pEntity.getPosition(currentPosition);

            // Get the position relative to the active zone for nearest 
            // neighbor checking.
            Entity activeZoneEntity = 
            	SceneHierarchyUtility.getActiveZoneEntity(model);
            
            double[] pos = 
            	TransformUtils.getExactRelativePosition(
            			model, pEntity, activeZoneEntity, false);
            
            if (activeZoneEntity == null || pos == null) {
            	return null;
            }
            
            // Set our test values
            pEntity.setParentEntityID(activeZoneEntity.getEntityID());
            pEntity.setPosition(pos, false);

            // Use extra bounds to create a collision volume that extends back
            // to the active zone.
            float[] extraBounds =
                new float[] {
                    0.0f,
                    0.0f,
                    ((float) pos[2] + 0.125f)};

            ArrayList<Entity> negZNeighbors =
                getNegativeZNeighbors(
                    pEntity,
                    extraBounds);

            // Set back the values
            pEntity.setPosition(currentPosition, false);
            pEntity.setParentEntityID(currentParentID);

            // Get the possible legal collisions
            legalCollisions =
                getLegalAllowedParentCollisions(
                        negZNeighbors,
                        pEntity,
                        allowedParentClassifications);

            // Get the closest parent
            parentEntity =
				getClosestEntity(legalCollisions, pEntity);

        }

        return parentEntity;
    }

	//--------------------------------------------------------------------------
	// Private methods
	//--------------------------------------------------------------------------
	
	/**
	 * Log a failure to the console.
	 */
	private void logFailure(String st) {
		System.out.println("Rule Failure: " + getClass().getSimpleName() + " reason: " + st);
	}
	
	/**
	 * Check for specific rule properties on the entity to determine if we need
	 * to force the useEntityExtendedBounds to be true.
	 * 
	 * @param entity Entity to examine
	 * @param useEntityExtendedBounds Current value of useEntityExtendedBounds
	 * @return True if we need to force useEntityExtendedBounds to be true, 
	 * false otherwise
	 */
	private boolean useEntityExtendedBoundsCheck(
			Entity entity, boolean useEntityExtendedBounds) {
	
		return useEntityExtendedBounds;
	}
	
	/**
	 * Check for specific rule properties on the entity to determine if we need
	 * to force the useTargetsExtendedBounds value to be true.
	 * 
	 * @param entity Entity to examine
	 * @param useTargetsExtendedBounds Current value of useTargetsExtendedBounds
	 * @return True if we need to force useTargetsExtendedBounds to be true, 
	 * false otherwise
	 */
	private boolean useTargetsExtendedBoundsCheck(
			Entity entity, boolean useTargetsExtendedBounds) {
		
		Boolean colPosReq = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity, 
					ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS);
		
		if (colPosReq) {
			return true;
		}
		
		Boolean isSpan = (Boolean) 
			RulePropertyAccessor.getRulePropertyValue(
					entity, ChefX3DRuleProperties.SPAN_OBJECT_PROP);
		
		if (isSpan) {
			return true;
		}
		
		Boolean useEdgeFaceSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity, 
					ChefX3DRuleProperties.USE_EDGE_FACE_SNAP);
		
		if (useEdgeFaceSnaps) {
			return true;
		}
		
		return useTargetsExtendedBounds;
	}
	
    /**
     * Get the legal parent collisions. Gives back a list of collisions that
     * are appropriate for the given allowedParentClassifications.
     *
     * @param baseSet Set of entities to consider
     * @param commandEntity Entity being acted on by user
     * @param allowedParentClassifications Allowed parent classifications
     * @return List of legal parent entities, null if unknown
     */
    private ArrayList<Entity> getLegalAllowedParentCollisions(
            ArrayList<Entity> baseSet,
            Entity commandEntity,
            String[] allowedParentClassifications) {

        if (baseSet == null || baseSet.size() < 1) {
            return null;
        }

        ArrayList<Entity> legalCollisions = new ArrayList<Entity>();
        
        // Get the active zone entity for checking against

        Entity activeZoneEntity = 
        	SceneHierarchyUtility.getActiveZoneEntity(model);

        // Loop through the collisions, and
        // create a list of legal collisions.
        for (int i = 0; i < baseSet.size(); i++) {

            Entity collisionEntity = baseSet.get(i);
            
            // Don't look at anything that parents to a zone that is not
            // the active zone. Or is a zone that is not the active
            // zone
            Entity parentZoneEntity = 
            	SceneHierarchyUtility.findZoneEntity(
            			model, collisionEntity);
            
            if (collisionEntity.isZone() && 
            		collisionEntity != activeZoneEntity) {
            	continue;
            }
            
            if (parentZoneEntity != activeZoneEntity) {
            	continue;
            }

            // Don't look at auto span entities, unless it is part of
            // the allowed parent classifications
            Boolean isAutoSpan = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);

            if (isAutoSpan) {

                // see if it is an allowed collision. Otherwise
                // we are going to ignore it.

                String[] classifications = (String[])
                    RulePropertyAccessor.getRulePropertyValue(
                            collisionEntity,
                            ChefX3DRuleProperties.CLASSIFICATION_PROP);

                boolean autoSpanMatched = false;

                if (classifications != null) {
                    if (allowedParentClassifications == null) {
                        legalCollisions.add(collisionEntity);
                        autoSpanMatched = true;
                    } else {
                        for (int w = 0; w < classifications.length; w++) {

                            for (int x = 0;
                                    x < allowedParentClassifications.length; 
                                    x++) {

                                if (classifications[w].equalsIgnoreCase(
                                        allowedParentClassifications[x])) {

                                    autoSpanMatched = true;
                                    break;
                                }
                            }

                            if (autoSpanMatched) {
                                break;
                            }
                        }
                    }
                }

                if (!autoSpanMatched) {
                    continue;
                }
            }

            // Make sure we don't chose an entity that is parented to us
            int testEntityParentID = collisionEntity.getParentEntityID();

            while (testEntityParentID > 0) {

                if (testEntityParentID == commandEntity.getEntityID()) {
                    break;
                }

                Entity tmpEntity = model.getEntity(testEntityParentID);
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

                legalCollisions.add(collisionEntity);
                continue;
            }

            String[] collisionEntityClassifications = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                        collisionEntity,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

            if (collisionEntityClassifications != null) {
                boolean matchFound = false;

                // Check for a legal match of classification name and requirement
                for (int j = 0; 
                	j < collisionEntityClassifications.length; 
                	j++) {

                    for (int w = 0; 
                    	w < allowedParentClassifications.length; 
                    	w++) {

                        if (collisionEntityClassifications[j].equalsIgnoreCase(
                            allowedParentClassifications[w])) {

                            legalCollisions.add(collisionEntity);
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

        return legalCollisions;
    }

    /**
     * Find the closest entity by distance to commandEntity.
     * Evaluates distance along the z axis.
     *
     * @param entities List of entities to examine
     * @param commandEntity Entity acted on by user
     * @return Closest entity to commandEntity, null if unknown
     */
    private Entity getClosestEntity(
            ArrayList<Entity> entities,
            Entity commandEntity) {

        if (entities == null || entities.size() < 1) {
            return null;
        }

        double[] vec = null;
        Entity parentEntity = null;
        double minLength = 0;

        for (int i = 0; i < entities.size(); i++) {

            vec =
                TransformUtils.getDistanceBetweenEntities(
                    model,
                    (PositionableEntity) entities.get(i),
                    (PositionableEntity) commandEntity,
                    false);

            // We are going to see this happen if
            // the entity is in a different zone
            if (vec == null) {
                continue;
            }

            vec[2] = Math.abs(vec[2]);

            // Handle the first case where parentEntity is null
            if (parentEntity == null) {
                minLength = vec[2];
                parentEntity = entities.get(i);
            } else if (vec[2] < minLength) {
                minLength = vec[2];
                parentEntity = entities.get(i);
            }
        }

        return parentEntity;
    }
}
