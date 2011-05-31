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
import java.util.List;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.rules.util.AutoAddResult.TRANSACTION_OR_ENTITY_ID;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.ORIENTATION;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.SCALE_CHANGE_MODEL_AXIS_VALUES;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.view.common.EditorView;


/**
* Shared routines related to auto place actions. All auto add rules extend
* this class.
*
* @author Ben Yarger
* @version $Revision: 1.14 $
*/
public abstract class AutoAddUtility{

//    private static final String LIST_PROPERTY_LABEL =
//        "org.chefx3d.tool.ToolSwitch.productChoicePropertyName";

    /** Amount of buffered space to add to force fit adjustments */
    private static final double FORCE_FIT_BUFFER = 0.01;

    /** Maximum offset of a force fit allowed */
    private static final double MAX_FORCE_FIT_OFFSET = 0.09;

    /** Possible results for auto add condition check */
    public static enum CONDITION_CHECK_RESULT {PASSED, FAILED, CONTINUE};

    /**
     * Check if the entity performs any of the possible auto add operations.
     * Will return true on the first instance of a used auto add.
     *
     * @param entity Entity to check
     * @return True if using, false otherwise
     */
    public static boolean performsAutoAddOperations(Entity entity) {

        if (AutoAddBySpanUtility.performsAutoAddBySpan(
                entity)) {
            return true;
        } else if (AutoAddByCollisionUtility.performsAutoAddByCollision(
                entity)){
            return true;
        } else if (AutoAddByPositionUtility.performsAutoAddByPosition(
                entity)) {
            return true;
        } else if (AutoAddEndsUtility.performsAutoAddEnds(
                entity)) {
            return true;
        } else if (AutoAddInvisibleChildrenUtility.performsAutoAddInvisible(
                entity)) {
            return true;
        }

        return false;
    }

    /**
     * Create the AddEntityChildCommand and apply it to the model. Will
     * automatically add the new command to the newly issued command stack and
     * a surrogate to the collision manager.
     *
     * @param model WorldModel
     * @param parentEntity Entity that is the parent
     * @param parentEntityParentEntity parent entities parent entity
     * @param simpleTool SimpleTool to create child
     * @param pos double[3] Position of child
     * @param rot float[4] Rotation of child
     * @param boolean forceFit Forces extra calculation to attempt placement
     * @param adjAxis axis to attempt forceFit adjustments along
     * @param orientation Orientation to attempt to use
     * @param rch Rule collision handler to use for collision analysis
     * @param entityBuilder Entity builder to create entities with
     * @return AutoAddResult
     */
    public static AutoAddResult issueNewAutoAddChildCommand(
            WorldModel model,
            Entity parentEntity,
            Entity parentEntityParentEntity,
            SimpleTool simpleTool,
            double[] pos,
            float[] rot,
            boolean forceFit,
            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS adjAxis,
            ORIENTATION orientation,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder){

        int entityID = model.issueEntityID();
        
        if (simpleTool == null) {
        	return (new AutoAddResult(
                    0,
                    TRANSACTION_OR_ENTITY_ID.FAILURE));
        }

        Entity newEntity = entityBuilder.createEntity(
                model,
                entityID,
                pos,
                rot,
                simpleTool);

        // Get the orientation specific version of the model
        newEntity =
            getOrientatedModel(model, newEntity, orientation, entityBuilder);
        
        // If we are colliding with another same root tool id of parent at this
        // location then bail out and return a status with the found entity id.
        AddEntityChildCommand initialTestCmd = 
        	new AddEntityChildCommand(
        			model, 
        			model.issueTransactionID(),
        			parentEntity, 
        			newEntity,
        			true);
        
        rch.performCollisionCheck(initialTestCmd, true, true, true);
        String primaryAutoAddID = 
        	AutoAddUtility.getPrimaryAutoAddToolID(newEntity, parentEntity);
        
        if (rch.collisionEntities != null) {
        	
	        for (Entity colEntity : rch.collisionEntities) {
	        	
	        	if (AutoAddUtility.isAutoAddChildOfParent(
	        			colEntity, parentEntity)) {
	        		
	        		if (primaryAutoAddID.equals(
	        				AutoAddUtility.getPrimaryAutoAddToolID(
	        						colEntity, parentEntity))) {
	        			
	        			return (new AutoAddResult(
	                            colEntity.getEntityID(),
	                            TRANSACTION_OR_ENTITY_ID.ENTITY));
	        		}
	        	}
	        }
        }

        // Try to find the best size, if there are options, to try and place.
        Entity bestSizeEntity = findBestSizeEntity(
                model,
                (PositionableEntity)newEntity,
                (PositionableEntity)parentEntity,
                pos,
                rot,
                null,
                rch,
                entityBuilder);

        if (bestSizeEntity != null) {
            newEntity = bestSizeEntity;
        }
        
        // clear out the 'bogus' property
        newEntity.setProperty(
                newEntity.getParamSheetName(),
                Entity.SHADOW_ENTITY_FLAG,
                false,
                false);

        // Set the is auto add flag
        newEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT,
                true,
                false);

        // Set the orientation
        newEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.AUTO_ADD_END_ORIENTATION,
                orientation,
                false);

        // Side pocket the initial parent's ID
        newEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.INITAL_ADD_PARENT,
                parentEntity.getEntityID(),
                false);

        AddEntityChildCommand addChildCmd =
            new AddEntityChildCommand(
            		model, 
            		model.issueTransactionID(),
            		parentEntity, 
            		newEntity,
            		true);

        // Perform the collision check to see if there are illegal collision.
        // If so, do some processing (if possible) to avoid failure.
        // Ideally, we want to have the pre-validate method functional to
        // do a through check to validate.
        rch.performCollisionCheck(addChildCmd, true, false, false);
        rch.performCollisionAnalysisHelper(newEntity, null, true, null, true);
        
        if (rch.collisionEntities == null || 
        		rch.collisionEntities.size() == 0) {
        	
        	// check for the None flag, if is exists then allow the add
        	boolean allowZero = false;
        	
        	String[] classRelationship = (String[]) 
        		RulePropertyAccessor.getRulePropertyValue(
        				newEntity, 
        				ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
        	
        	if (classRelationship != null) {
        	
        		for (int w = 0; w < classRelationship.length; w++) {
        			
        			if (classRelationship[w].equals(
        					ChefX3DRuleProperties.RESERVED_NONE_RELATIONSHIP)) {
        				allowZero = true;
        				break;
        			}
        		}
        	}
        	
        	if (!allowZero) {
	        	return (
	                    new AutoAddResult(
	                        0,
	                        TRANSACTION_OR_ENTITY_ID.FAILURE));
        	}
        }

        if(rch.hasIllegalCollisionHelper(newEntity)){

            // Check to see if any of the collisions are with a matching tool ID
            // or associated legal sku swap option.
            int existingMatchResult = findExistingAutoAddMatch(newEntity, rch);

            if (existingMatchResult >= 0) {

                return (
                        new AutoAddResult(
                            existingMatchResult,
                            TRANSACTION_OR_ENTITY_ID.ENTITY));

            } else if (forceFit && adjAxis != null) {

                boolean forceFitResult =
                    forceFitCommand(
                            model,
                            addChildCmd,
                            adjAxis,
                            null,
                            parentEntity,
                            FORCE_FIT_BUFFER,
                            MAX_FORCE_FIT_OFFSET,
                            rch);

                if (!forceFitResult) {

                    return (
                        new AutoAddResult(
                            0,
                            TRANSACTION_OR_ENTITY_ID.FAILURE));
                }

            } else {

                    return (
                            new AutoAddResult(
                                0,
                                TRANSACTION_OR_ENTITY_ID.FAILURE));
            }
        }


/*
        // TODO" get the pre-validation check working
        if (!RuleUtils.preValidateCommand(addChildCmd)) {
            return (
                    new AutoAddResult(
                        0,
                        TRANSACTION_OR_ENTITY_ID.FAILURE));
        }
*/
        // If the parentEntity is a miter entity, then ignore rule evaluation.
        // We have already confirmed that it will be safe to add.
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        // Issue the add command and do all the scene management stuff
        int transactionID = SceneManagementUtility.addChild(
                model,
                rch.getRuleCollisionChecker(),
                newEntity,
                parentEntity,
                canMiterCut.booleanValue());

        return (
                new AutoAddResult(
                    transactionID,
                    TRANSACTION_OR_ENTITY_ID.TRANSACTION));
    }


    /**
     * If a full length product cannot fit because of collision with
     * another product, we need to attempt to place the next smallest
     * size product. In other words, if a product being added cannot
     * fit because of collisions but it uses this rule, attempt to use
     * the next smaller size until no smaller sizes are available to
     * try or a fit was found. Result will come back with setRelativePosition
     * applied.
     *
     * @param model WorldModel
     * @param entity Entity to try and start fit with
     * @param parentEntity Parent entity of entity
     * @param position double[3] Position of entity, can be null to use stored
     * value in entity
     * @param rotation float[4] Rotation of entity, can be null to use stored
     * value in entity
     * @param scale float[3] Scale of entity, can be null to use stored value
     * in entity
     * @param rch RuleCollisionHandler to use for collision checking and result
     * processing
     * @param entityBuilder EntityBuilder to use to create each new entity to
     * test with
     * @return AddEntityChildCommand or null if cannot legally add it
     */
    public static Entity findBestSizeEntity(
            WorldModel model,
            PositionableEntity entity,
            PositionableEntity parentEntity,
            double[] position,
            float[] rotation,
            float[] scale,
            RuleCollisionHandler rch,
            EntityBuilder entityBuilder) {

        // See if the current entity has illegal collisions.
        // If it does, then we will try to find the next smaller size to try.
        // Set the position, rotation and scale if provided.

        if (position != null) {
            entity.setPosition(position, false);
        }

        if (rotation != null) {
            entity.setRotation(rotation, false);
        }

        if (scale != null) {
            entity.setScale(scale);
        }

        // Apply genPos
        ArrayList<Entity> targetSet = new ArrayList<Entity>();
        targetSet.add(parentEntity);

        double[] newPosition = TransformUtils.getPosition(entity);

        SetRelativePositionUtility.setRelativePosition(
                model, entity, parentEntity, targetSet, newPosition);

        entity.setPosition(newPosition, false);

        AddEntityChildCommand testCmd =
            new AddEntityChildCommand(
            		model, 
            		model.issueTransactionID(),
            		parentEntity, 
            		entity,
            		true);

        rch.performCollisionCheck(testCmd, true, false, false);
        rch.performCollisionAnalysisHelper(
                entity, null, true, new int[] {entity.getEntityID()}, true);

        // If there are no illegal collisions, we have found the legal size to
        // return, so do so.
        if (!rch.hasIllegalCollisionHelper(entity)) {
            return entity;
        }

        // This will only work if the scale change model flag is set.
        // If it isn't, then we don't have another size to fall back to.
        // If that is the case, then just return the entity passed in.
        Boolean skuX3DChange = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);

        if(!skuX3DChange) {
            return null;
        }

        // Grab the size of the current entity along the relevant axis
        // look at Enum CX.SKUX3DAxis - X, Y or Z axis
        // and then grab the appropriate dimension from the entity.
        SCALE_CHANGE_MODEL_AXIS_VALUES skuX3DAxis = (SCALE_CHANGE_MODEL_AXIS_VALUES)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_AXIS);

        float[] entitySize = new float[3];
        float[] entityScale = new float[3];

        entity.getSize(entitySize);
        entity.getScale(entityScale);

        float currentSize = 0;

        switch(skuX3DAxis){

        case XAXIS:
            currentSize = entitySize[0] * entityScale[0];
            break;

        case YAXIS:
            currentSize = entitySize[1] * entityScale[1];
            break;

        case ZAXIS:
            currentSize = entitySize[2] * entityScale[2];
            break;
        }

        // we have a collision with the current length.
        // So, try and find a shorter object to take the currentTool's place by
        // iterating through the float[] array CX.SKUX3DSize
        float[] skuX3DSize = (float[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_SIZE);

        // index to the longest SKU that is shorter than current
        int indexToNextShortestSku = -1;

        // length of longest SKU that is shorter than current
        float nextShortestSize = -Float.MAX_VALUE;

        for( int i = 0; i < skuX3DSize.length; i++ ){

            if(skuX3DSize[i] < currentSize &&
               skuX3DSize[i] > nextShortestSize ){
                    nextShortestSize = skuX3DSize[i];
                    indexToNextShortestSku = i;
            }
        }

        // if no items exist shorter than current, we can't place the object!
        if( indexToNextShortestSku < 0 )
            return null;
/*
        // Adjust the position for the next test
        switch( (ChefX3DRuleProperties.ADJUSTMENT_AXIS)skuX3DAxis ){

        case XAXIS:
            position
            currentSize = entitySize[0] * entityScale[0];
            break;

        case YAXIS:
            currentSize = entitySize[1] * entityScale[1];
            break;

        case ZAXIS:
            currentSize = entitySize[2] * entityScale[2];
            break;
        }
*/
        // alternate products to load up matched to size index
        String[] skuX3DProd = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);

        // create a new tool based on the index to
        // the longest SKU shorter than current
        SimpleTool newSimpleTool =
            RuleUtils.getSimpleToolByName(skuX3DProd[indexToNextShortestSku]);

        if (newSimpleTool == null) {
            return null;
        }

        String iconUrl = newSimpleTool.getIcon();

        if(iconUrl == null || iconUrl.equals("")) {
            iconUrl = entity.getIconURL(null);
            newSimpleTool.setIcon(null, iconUrl);
        }

        Entity newEntity = entityBuilder.createEntity(
                model, entity.getEntityID(), position, rotation, newSimpleTool);

        return findBestSizeEntity(
                model,
                (PositionableEntity) newEntity,
                parentEntity,
                position,
                rotation,
                scale,
                rch,
                entityBuilder);
    }

    /**
     * Confirms if the child is an auto place entity of the parent.
     *
     * @param child Child entity to check
     * @param parent Parent entity to check against
     * @return True if it is an auto add entity of the parent, false otherwise
     */
    public static boolean isAutoAddChildOfParent(Entity child, Entity parent){

        // Get the set values to use in every check
        String toolID = child.getToolID();

        // if Boolean CX.SKUX3DChange is TRUE, see if any of this child's
        // swap siblings can be parented by the parent
        Boolean skuX3DChange = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                child,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);

        String[] scaleChangeModelProd = new String[0];

        if( skuX3DChange != null && skuX3DChange ){
            scaleChangeModelProd = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                    child,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);
        }

        // Start checks
        if (doToolIDsMatch(
                toolID,
                AutoAddBySpanUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd)) {
            return true;
        }

        if (doToolIDsMatch(
                toolID,
                AutoAddByCollisionUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd)) {
            return true;
        }

        if (doToolIDsMatch(
                toolID,
                AutoAddByPositionUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd)) {
            return true;
        }

        if (doToolIDsMatch(
                toolID,
                AutoAddEndsUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd)) {
            return true;
        }

        if (doToolIDsMatch(
                toolID,
                AutoAddInvisibleChildrenUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd)) {
            return true;
        }

        return false;
    }

    /**
     * Get the primary auto add tool id responsible for auto adding the child.
     * Note that the child may not have the same tool ID as what is specified
     * by the auto add data, because, it may have model swap targets that have
     * swapped it out. This will get the primary tool ID used to create the
     * auto add child.
     *
     * @param child Child entity to check
     * @param parent Parent entity to check against
     * @return The primary matching auto add tool ID, null if not found
     */
    public static String getPrimaryAutoAddToolID(Entity child, Entity parent){

        // Get the set values to use in every check
        String toolID = child.getToolID();

        // if Boolean CX.SKUX3DChange is TRUE, see if any of this child's
        // swap siblings can be parented by the parent
        Boolean skuX3DChange = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                child,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);

        String[] scaleChangeModelProd = new String[0];

        if( skuX3DChange != null && skuX3DChange ){
            scaleChangeModelProd = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                    child,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);
        }

        String matchedID = null;
        // Start checks
        matchedID = getPrimartyToolID(
                toolID,
                AutoAddBySpanUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd);

        if (matchedID != null) {
            return matchedID;
        }

        matchedID = getPrimartyToolID(
                toolID,
                AutoAddByCollisionUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd);

        if (matchedID != null) {
            return matchedID;
        }

        matchedID = getPrimartyToolID(
                toolID,
                AutoAddByPositionUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd);

        if (matchedID != null) {
            return matchedID;
        }

        matchedID = getPrimartyToolID(
                toolID,
                AutoAddEndsUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd);

        if (matchedID != null) {
            return matchedID;
        }

        matchedID = getPrimartyToolID(
                toolID,
                AutoAddInvisibleChildrenUtility.getAutoAddToolIDS(parent),
                scaleChangeModelProd);

        if (matchedID != null) {
            return matchedID;
        }

        return null;
    }


    /**
     * Piggy-back on the (SegmentEdgeEntityOrientationRule) for swapping
     * between specific positive and negative orientation models. Use our
     * own specific ORIENTATION value to get exactly what we want back.
     *
     * If we can't get a new model back, then we return the entity that
     * came in as the parameter.
     *
     * @param model WorldModel to reference
     * @param entity Entity to attempt to swap
     * @param orientation Orientation flag
     * @param entityBuilder Entity builder to create entities with
     * @return Entity, either the orientation specific or the entity parameter
     * if unable to find orientation specific alternate.
     */
    public static Entity getOrientatedModel(
            WorldModel model,
            Entity entity,
            ORIENTATION orientation,
            EntityBuilder entityBuilder) {

        // Make sure we get the expected list of ids - should be length 2
        String[] swapIDList = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.ORIENTATION_SWAP_ID_LIST);

        if(swapIDList == null || swapIDList.length != 2){
            return entity;
        }

        // Extract the appropriate toolID based on value of changeToState
        String toolID;

        switch(orientation){

            case NEGATIVE:
                toolID = swapIDList[0];
                break;

            case POSITIVE:
                toolID = swapIDList[1];
                break;

            default:
                return entity;
        }

        // Get the tool
        SimpleTool tool = RuleUtils.getSimpleToolByName(toolID);

        if(tool == null || tool.getToolID().equals(toolID)){
            return entity;
        }

        // Get the current position
        if(!(entity instanceof PositionableEntity)){
            return entity;
        }

        double[] position = new double[] {0, 0, 0};

        PositionableEntity posEntity = (PositionableEntity)entity;
        posEntity.getPosition(position);

        // Create the new entity
        Entity newEntity =
            entityBuilder.createEntity(
                    model,
                    model.issueEntityID(),
                    position,
                    new float[] {0.0f, 1.0f, 0.0f, 0.0f},
                    tool);

        if (newEntity == null) {
            return entity;
        }

        // Tell the entity which orientation it is
        newEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.AUTO_ADD_END_ORIENTATION,
                orientation,
                false);


        return newEntity;

    }

    /**
     * Return an array list containing all the valid collisions.
     * Used by auto add by collision cases to determine where all of
     * the valid collisions for auto add are.
     * If we are not adding by collision, or if there are no valid
     * collisions because the autoAddChild has no relationship
     * classes defined (CX.relClass), then the array list returned
     * will be empty.
     *
     * @param model WorldModel to reference
     * @param autoAddByCol TRUE if the parentEntity is auto adding
     * children based on collision.
     * @param adjAxis Axis to do analysis along
     * @param autoAddStepSize Step size to adhere to
     * @param simpleTool The auto-add child object (found in the
     * parentEntity's autoPlObj array).
     * @param command The command being issued
     * @param ignoreEntities List of entities to ignore
     * @return A list of all the valid collisions, or an empty list
     * if we are not adding by collision or if no valid collisions
     * are found.
     * @author Eric Fickenscher
     */
    public static ArrayList<Entity> getValidCollisions(
            WorldModel model,
            boolean autoAddByCol,
            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS adjAxis,
            double autoAddStepSize,
            SimpleTool simpleTool,
            Command command,
            ArrayList<Entity> ignoreEntities,
            RuleCollisionHandler rch){

        ArrayList<Entity> validCollisions = new ArrayList<Entity>();

        String[] relClass = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    simpleTool,
                    ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);

        if( autoAddByCol && relClass != null && relClass.length > 0){

            List<String> allowableCollisionClasses =
                java.util.Arrays.asList(relClass);

            //
            // Perform collision check to see if there are valid
            // collisions.  Requires doing collision analysis
            //
            rch.performCollisionCheck(command, true, false, true);

            // go through the classification names from
            // each collision.  If any of those names are
            // contained in the relationship classification
            // list, then add that entity to validCollisions.
            for( Entity e : rch.collisionEntities){

                // skip any ignore entities
                if (ignoreEntities != null) {
                    boolean matched = false;
                    for (Entity i : ignoreEntities) {
                        if (e.equals(i)) {
                            matched = true;
                            break;
                        }
                    }
                    if (matched)
                        continue;
                }

                String[] collisionClass = ((String[])
                        RulePropertyAccessor.getRulePropertyValue(
                                e,
                                ChefX3DRuleProperties.CLASSIFICATION_PROP));

                if (collisionClass == null)
                    continue;

                boolean collisionAllowed = false;
                for(String s : collisionClass){
                    if( allowableCollisionClasses.contains(s))
                        collisionAllowed = true;
                }
                if(collisionAllowed)
                    validCollisions.add(e);
            }

            // sort the collisions from left to right
            // sort them relative to the active zone
            PositionableEntity activeZone = (PositionableEntity)
                SceneHierarchyUtility.getActiveZoneEntity(model);

            validCollisions =
                TransformUtils.sortDescendingRelativePosValueOrder(
                        model,
                        validCollisions,
                        activeZone,
                        adjAxis,
                        true);

            //
            // Check that the step size is not violated

            double[] firstPos = new double[3];
            double[] secondPos = new double[3];

            int count = validCollisions.size();
            if (count >= 2) {
                PositionableEntity first =
                    (PositionableEntity)validCollisions.get(0);
                for (int i = 1; i < count; i++) {
                    PositionableEntity second =
                        (PositionableEntity)validCollisions.get(i);

                    // check the distance
                    firstPos = TransformUtils.getExactPositionRelativeToZone(model, first);
                    secondPos = TransformUtils.getExactPositionRelativeToZone(model, second);

                    double dist;
                    switch (adjAxis) {
                        default:
                        case XAXIS:
                            dist = Math.abs(secondPos[0] - firstPos[0]);
                            break;
                        case YAXIS:
                            dist = Math.abs(secondPos[1] - firstPos[1]);
                            break;
                    }
                    if (dist > autoAddStepSize && autoAddStepSize > 0.0f) {
                        // set the collision as invalid
                        validCollisions.set(i, null);
                    }

                    first = second;

                }
            }
        }

        return validCollisions;
    }

    /**
     * Remove all of the previously added commands from the queues by their
     * transaction ID's.
     */
    public static void clearCommands(AutoAddResult[] autoAddResults) {

        if (autoAddResults == null) {
            return;
        }

        for (int i = 0; i < autoAddResults.length; i++) {

            if (autoAddResults[i].getType() ==
                TRANSACTION_OR_ENTITY_ID.TRANSACTION) {

                CommandSequencer.getInstance().removeCommand(
                        autoAddResults[i].getValue());
            }
        }
    }

    /**
     * Evaluate the auto add condition set for the auto add procedure. Depending
     * on the condition, the transactionIDS will be removed from the queues.
     *
     * @param autoAddCondition AUTO_ADD_CONDITION to evaluate.
     * @param result True if auto add attempt succeeded, false otherwise.
     * @param endOfSet True if the final auto add attempt was made, false
     * otherwise.
     * @param autoAddResults Array of results so we can remove them
     * if needed.
     * @return CONDITION_CHECK_RESULT: passed if condition has been met and
     * auto add can stop processing, false if condition was not met and
     * auto add should stop processing, continue if condition has been satisfied
     * and auto add should continue processing.
     */
    public static CONDITION_CHECK_RESULT evaluateAutoAddCondition(
            ChefX3DRuleProperties.AUTO_ADD_CONDITION autoAddCondition,
            boolean result,
            boolean endOfSet,
            AutoAddResult[] autoAddResults) {

        // Add first successful and proceed or add none
        // Add first successful, fail if none add
        // Add all products in auto add list or none
        // Add all products, fail if any fail
        switch (autoAddCondition) {

        case FIRST_OR_NONE:
            if (result == true) {
                return CONDITION_CHECK_RESULT.PASSED;
            } else {
                AutoAddUtility.clearCommands(autoAddResults);
                return CONDITION_CHECK_RESULT.CONTINUE;
            }

        case FIRST_OR_FAIL:
            if (result == true) {
                return CONDITION_CHECK_RESULT.PASSED;
            } else if (endOfSet) {
                AutoAddUtility.clearCommands(autoAddResults);
                return CONDITION_CHECK_RESULT.FAILED;
            } else {
                AutoAddUtility.clearCommands(autoAddResults);
                return CONDITION_CHECK_RESULT.CONTINUE;
            }

        case ADD_ALL_POSSIBLE:
            // Keep going cause we don't stop with this case!
            if (result == false) {
                AutoAddUtility.clearCommands(autoAddResults);
            }
            break;

        case ADD_ALL_OR_FAIL:
            if (result == false) {
                AutoAddUtility.clearCommands(autoAddResults);
                return CONDITION_CHECK_RESULT.FAILED;
            }
            break;

        }

        return CONDITION_CHECK_RESULT.CONTINUE;
    }

    /**
     * Check the auto add condition against the number of siblings to see if
     * the remove should be permitted.
     *
     * @param autoAddCondition AUTO_ADD_CONDITION to check
     * @param numberOfSiblings Number of siblings found
     * @return True if remove should be permitted, false otherwise
     */
    public static boolean evaluateRemoveAutoAddCondition(
            ChefX3DRuleProperties.AUTO_ADD_CONDITION autoAddCondition,
            int numberOfSiblings) {

        // Add first successful and proceed or add none
        // Add first successful, fail if none add
        // Add all products in auto add list or none
        // Add all products, fail if any fail
        switch (autoAddCondition) {

        case FIRST_OR_NONE:
            return true;

        case FIRST_OR_FAIL:
            if (numberOfSiblings == 0) {
                return false;
            } else {
                return true;
            }

        case ADD_ALL_POSSIBLE:
            return true;

        case ADD_ALL_OR_FAIL:
            return false;

        }

        return true;
    }

    /**
     * Get all of the children of the given entity that are auto add children.
     *
     * @param entity Entity to get auto add children for.
     * @return ArrayList of entities that are auto add children
     */
    public static ArrayList<Entity> getAutoAddChildren(Entity entity) {

        ArrayList<Entity> children = 
        	SceneHierarchyUtility.getExactChildren(entity);
        
        Entity child = null;

        for (int i = (children.size() - 1); i >= 0; i--) {

        	child = children.get(i);
        	
            Boolean isAutoAddProduct =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        child,
                        ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

            if (!isAutoAddProduct) {
                children.remove(i);
            }
        }

        return children;
    }

    /**
     * Get all of the children of the parentEntity that match the primary auto
     * add id matching that of the toolID passed in.
     *
     * @param model WorldModel to reference
     * @param parentEntity Entity to get matching auto add children for
     * @param toolID Primary auto add tool ID to match to
     * @return ArrayList of entities or null if there was a problem
     */
    public static ArrayList<Entity> getAllMatchingPrimaryAutoAddIDChildren(
            WorldModel model,
            Entity parentEntity,
            String toolID) {

        ArrayList<Entity> autoAddChildren =
            AutoAddUtility.getAutoAddChildren(parentEntity);

        // Check all children for matching primary auto add ID matching toolID.
        // Remove any that don't match.
        for (int i = (autoAddChildren.size() - 1); i >= 0; i--) {

            String checkID =
                AutoAddUtility.getPrimaryAutoAddToolID(
                        autoAddChildren.get(i), parentEntity);

            if (!toolID.equals(checkID)) {
                autoAddChildren.remove(i);
            }
        }

        return autoAddChildren;
    }

    /**
     * Get all of the siblings of the entity that match the primary auto add id
     * associated with the entity.
     *
     * @param model WorldModel to reference
     * @param entity Entity to match to
     * @return ArrayList of entities or null if there was a problem
     */
    public static ArrayList<Entity> getAllMatchingPrimaryAutoAddIDChildren(
            WorldModel model,
            Entity entity) {

        Entity parentEntity =
            SceneHierarchyUtility.getExactParent(model, entity);

        // If the parent entity cannot be found, go ahead and allow the removal
        if (parentEntity == null) {
            return null;
        }

        ArrayList<Entity> autoAddChildren =
            AutoAddUtility.getAutoAddChildren(parentEntity);

        String toolID =
            AutoAddUtility.getPrimaryAutoAddToolID(entity, parentEntity);

        if (toolID == null) {
            return null;
        }

        // Check all children for matching primary auto add ID matching toolID.
        // Remove any that don't match.
        for (int i = (autoAddChildren.size() - 1); i >= 0; i--) {

            String checkID =
                AutoAddUtility.getPrimaryAutoAddToolID(
                        autoAddChildren.get(i), parentEntity);

            if (!toolID.equals(checkID)) {
                autoAddChildren.remove(i);
            }
        }

        return autoAddChildren;
    }

    /**
     * Isolate any collisions with entities that are auto-added children.
     *
     * @param parentEntity Entity parent
     * @param model WorldModel for world access
     * @param command The command being processed
     * @return The list of auto-added children, zero length list if none found.
     */
    public static ArrayList<Entity> isolateAutoAddChildrenCollisions(
            Entity entity,
            WorldModel model,
            Command command,
            RuleCollisionHandler rch){

        ArrayList<Entity> autoAddCollisionList = new ArrayList<Entity>();

        // perform check of collision children, if any of the items currently
        // in collision are auto-add products then we should check to make sure
        // the parent of them
        rch.performCollisionCheck(command, true, false, false);

        // If there are entity matches, then we need to check them
        if (rch.collisionEntities == null) {
            return autoAddCollisionList;
        }

        int count = rch.collisionEntities.size();
        if(count >= 1){

            for (int i = 0; i < count; i++) {
                Entity e = rch.collisionEntities.get(i);
                Boolean isAutoAddProduct =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            e,
                            ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
                if (isAutoAddProduct) {
                    autoAddCollisionList.add(e);
                }
            }
        }

        return autoAddCollisionList;

    }

    /**
     * Remove all auto adds children from an entity that are not required to
     * support collision requirements of other products. Invisible children
     * will not be removed.
     *
     * @param model WorldModel to reference
     * @param entity Entity to remove auto add children from
     * @param rch RuleCollisionHandler to use
     * @param view EditorView to reference
     * @return Auto add entities that were removed
     */
    public static Entity[] removeNonCriticalAutoAdds(
            WorldModel model,
            Entity entity,
            RuleCollisionHandler rch,
            EditorView view) {
    	
    	// Track all of the children that will be removed to return to
    	// calling method.
    	ArrayList<Entity> removedChildren = new ArrayList<Entity>();

        // Get all children of the entity that are auto adds
        ArrayList<Entity> autoAddChildren = getAutoAddChildren(entity);

        for (Entity autoAdd : autoAddChildren) {

            // Don't remove any invisible children
            if (AutoAddInvisibleChildrenUtility.isAutoAddChildOfParent(
            		model, autoAdd, entity)) {
                continue;
            }

            // Don't remove any auto add by position auto adds
            if (AutoAddByPositionUtility.isAutoAddChildOfParent(
                    model, autoAdd, entity)) {
                continue;
            }

            // If the auto add does not have any collision dependencies, then
            // it is ok to remove it.
            if (!rch.isDependantFixedEntity(
                    model, 
                    (PositionableEntity) autoAdd, 
                    rch, 
                    view)) {
            	
            	removedChildren.add(autoAdd);

                SceneManagementUtility.removeChild(
                        model,
                        rch.getRuleCollisionChecker(),
                        autoAdd,
                        true);
            }
        }
        
        Entity[] results = new Entity[removedChildren.size()];
        removedChildren.toArray(results);
        return results;
    }

    //--------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------

    /**
     * Attempts to place the entity to the positive and negative side of the
     * collective collision set and will attempt to do so along the adjustment
     * axis assigned. Applied as an alternate place during auto add if the
     * force fit rule flag is set.
     *
     * @param model WorldModel to reference
     * @param Command Command to process force fit on
     * @param adjAxis Axis to process the force fit along
     * @param vaporEntities Entities, not yet added, to consider for collision
     * @param forceFitBuffer Amount of buffered space to add to force fit
     * adjustment
     * @param forceFitOffset Maximum offset of a force fit allowed
     * @param rch Rule collision handler to use for collision checking
     * @return True if fit was made, false if not
     */
    private static boolean forceFitCommand(
            WorldModel model,
            Command command,
            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS adjAxis,
            Entity[] vaporEntities,
            Entity parentEntity,
            double forceFitBuffer,
            double forceFitOffset,
            RuleCollisionHandler rch) {

        // Get the entity, and if we don't find it return false
        Entity entity = null;

        if (command instanceof AddEntityChildCommand) {

            entity = ((AddEntityChildCommand)command).getEntity();

        } else if (command instanceof AddEntityChildTransientCommand) {

            entity = ((AddEntityChildTransientCommand)command).getEntity();

        } else if (command instanceof MoveEntityCommand) {

            entity = ((MoveEntityCommand)command).getEntity();

        } else if (command instanceof MoveEntityTransientCommand) {

            return true;

        } else {

            return true;
        }

        // Validate extracted data
        if (entity == null) {
            return false;
        }

        if (!(entity instanceof PositionableEntity)) {
            return false;
        }

        PositionableEntity posEntity = (PositionableEntity) entity;

        // Get the illegal collisions and determine the bounds of the illegal
        // collision set. Then try placing to the right, and then left if
        // right doesn't work.
        float[] multiBounds = new float[6];
        double[] multiCenter = new double[3];

        ArrayList<Entity> illegalEntities =
            rch.getChildrenMatches().getIllegalEntities();

        BoundsUtils.getMultiBounds(
                model,
                multiBounds,
                multiCenter,
                illegalEntities,
                (PositionableEntity) parentEntity,
                true);

        // Establish the first and second attempt positions.
        // First attempt pos is always in the positive direction
        // Second attempt pos is always in the negative direction
        double[] firstAttemptPos = new double[3];
        double[] secondAttemptPos = new double[3];

        float[] bounds = new float[6];

        double[] currentPos = new double[3];
        double[] endPos = new double[3];

        // Prepare values for analysis
        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand)command).getEndPosition(endPos);
            posEntity.getPosition(currentPos);
            posEntity.setPosition(endPos, false);

        } else if (command instanceof MoveEntityTransientCommand) {

            ((MoveEntityTransientCommand)command).getPosition(endPos);
            posEntity.getPosition(currentPos);
            posEntity.setPosition(endPos, false);

        } else if (command instanceof AddEntityChildCommand) {

            posEntity.getPosition(currentPos);
            posEntity.getPosition(endPos);
        }

        // Need to get the zone relative position to compare apples and
        // apples with the multi bounds results
        double[] zoneRelativePos;

        Entity parentZoneEntity =
            SceneHierarchyUtility.findZoneEntity(model, entity);

        // if its null then just use the currently active zone
        if (parentZoneEntity == null) {
            parentZoneEntity =
                SceneHierarchyUtility.getActiveZoneEntity(model);

            if (parentZoneEntity == null) {
                return false;
            }
        }

        zoneRelativePos =
            TransformUtils.getPositionRelativeToZone(model, command);

        // if the parent is not in the scene yet then we will have issues
        boolean parentNotInScene = false;

        // if the pos is not found it means the parent is also not
        // in the scene yet.  we need to do some manual calculations
        // to get the pos.
        if (zoneRelativePos == null) {
            zoneRelativePos =
                TransformUtils.getRelativePosition(
                        model,
                        parentEntity,
                        parentZoneEntity,
                        false);

            zoneRelativePos[0] += endPos[0];
            zoneRelativePos[1] += endPos[1];
            zoneRelativePos[2] += endPos[2];

            parentNotInScene = true;
        }

        // Reset values to before analysis state
        posEntity.setPosition(currentPos, false);

        // Continue processing
        posEntity.getBounds(bounds);

        secondAttemptPos[0] = firstAttemptPos[0] = endPos[0];
        secondAttemptPos[1] = firstAttemptPos[1] = endPos[1];
        secondAttemptPos[2] = firstAttemptPos[2] = endPos[2];

        // flag which cases to process
        // if either are false it is because the bounds issue has been violated
        boolean tryFirstAttempt = true;
        boolean trySecondAttempt = true;

        // Use this variable to index according to adjAxis.
        int axis = -1;

        if( adjAxis == TARGET_ADJUSTMENT_AXIS.XAXIS)
            axis = 0;
        else if(adjAxis == TARGET_ADJUSTMENT_AXIS.YAXIS)
            axis = 1;
        else if(adjAxis == TARGET_ADJUSTMENT_AXIS.ZAXIS)
            axis = 2;

        if( axis >= 0){
            double lowMultiBounds = multiCenter[axis] + multiBounds[axis*2];
            double highMultiBounds = multiCenter[axis]+ multiBounds[axis*2 + 1];

            double lowEntityBounds = zoneRelativePos[axis] + bounds[axis*2];
            double highEntityBounds = zoneRelativePos[axis]+ bounds[axis*2 + 1];

            // use the difference of the multiBounds for the first attempt
            firstAttemptPos[axis] +=
                (highMultiBounds - lowMultiBounds) + forceFitBuffer;

            // use the difference of the entityBounds for the second attempt
            secondAttemptPos[axis] -=
                (highEntityBounds - lowEntityBounds) + forceFitBuffer;

            if( forceFitOffset <
                    Math.abs(firstAttemptPos[axis] - endPos[axis])){
                tryFirstAttempt = false;
            }

            if( forceFitOffset <
                    Math.abs(endPos[axis] - secondAttemptPos[axis])){
                trySecondAttempt = false;
            }
        }


        // If first attempt should be made do it here
        if (tryFirstAttempt) {

            // Perform first attempt test
            if (command instanceof AddEntityChildCommand) {

                posEntity.setPosition(firstAttemptPos, false);

            } else if (command instanceof AddEntityChildTransientCommand) {

                posEntity.setPosition(firstAttemptPos, false);

            } else if (command instanceof MoveEntityCommand) {

                ((MoveEntityCommand)command).setEndPosition(firstAttemptPos);

            } else if (command instanceof MoveEntityTransientCommand) {

                ((MoveEntityTransientCommand)command).setPosition(
                        firstAttemptPos);

            }

            rch.performCollisionCheck(command, true, false, false);

            rch.performCollisionAnalysisHelper(
                    entity, vaporEntities, true, null, true);

            // If there are no illegal collisions then we have success.
            // Allow add children to succeed as well if there parent is not
            // in the scene, this just hopes it is valid
            if(!rch.hasIllegalCollisionHelper(entity) || parentNotInScene){

                return true;
            }

        }

        // If second attempt should be made, do it here
        if (trySecondAttempt) {
            // Perform second attempt test
            if (command instanceof AddEntityChildCommand) {

                posEntity.setPosition(secondAttemptPos, false);

            } else if (command instanceof AddEntityChildTransientCommand) {

                posEntity.setPosition(secondAttemptPos, false);

            } else if (command instanceof MoveEntityCommand) {

                ((MoveEntityCommand)command).setEndPosition(secondAttemptPos);

            } else if (command instanceof MoveEntityTransientCommand) {

                ((MoveEntityTransientCommand)command).setPosition(
                        secondAttemptPos);

            }

            rch.performCollisionCheck(command, true, false, false);

            rch.performCollisionAnalysisHelper(
                    entity, vaporEntities, true, null, true);

            // If there are no illegal collisions, then we have success.
            // Allow add children to succeed as well if there parent is not
            // in the scene, this just hopes it is valid
            if(!rch.hasIllegalCollisionHelper(entity) || parentNotInScene){

                return true;
            }
        }

        // Could not force fit it, so revert value
        if (command instanceof AddEntityChildCommand) {

            posEntity.setPosition(endPos, false);

        } else if (command instanceof AddEntityChildTransientCommand) {

            posEntity.setPosition(endPos, false);

        } else if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand)command).setEndPosition(endPos);

        } else if (command instanceof MoveEntityTransientCommand) {

            ((MoveEntityTransientCommand)command).setPosition(endPos);

        }

        return false;
    }

    /**
     * Check if the toolID matches any of the autoPlaceToolIds. If
     * alternateEntityToolIds are provided they will be compared against the
     * autoPlaceToolIds as well. Best use of alternateEntityYoolIds would be
     * to pass in the swap target ids for an entity if it does model swaps.
     *
     * @param toolID Entity tool ID to check
     * @param autoPlaceToolIds Auto place tool ID's of parent
     * @param alternateEntityToolIds Optional alternate entity ids to check, can
     * be null
     * @return True if there is a match, false otherwise
     */
    private static boolean doToolIDsMatch(
            String toolID,
            String[] autoPlaceToolIds,
            String[] alternateEntityToolIds) {

        if (autoPlaceToolIds == null) {
            return false;
        }

        // Begin actual collision add process
        for (int i = 0; i < autoPlaceToolIds.length; i ++) {

            if (autoPlaceToolIds[i].equals(toolID)) {
                return true;
            }

            // At the same time, check the alternateToolIds if provided
            if (alternateEntityToolIds != null) {

                for (int j = 0; j < alternateEntityToolIds.length; j++) {

                    if (alternateEntityToolIds[j].equals(
                            autoPlaceToolIds[i])) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Search the data for a match with autoPlaceToolIds. Return what that
     * match is.
     *
     * @param toolID Entity tool ID to try and match to autoPlaceToolIds
     * @param autoPlaceToolIds String[] of values to try and match to
     * @param alternateEntityToolIds Optional alternate list of tool ID's to try
     * and match to autoPlaceToolIds.
     * @return Matching toolID or null if not found
     */
    private static String getPrimartyToolID(
            String toolID,
            String[] autoPlaceToolIds,
            String[] alternateEntityToolIds) {

        if (autoPlaceToolIds == null) {
            return null;
        }

        // Begin actual collision add process
        for (int i = 0; i < autoPlaceToolIds.length; i ++) {

            if (autoPlaceToolIds[i].equals(toolID)) {
                return autoPlaceToolIds[i];
            }

            // At the same time, check the alternateToolIds if provided
            if (alternateEntityToolIds != null) {

                for (int j = 0; j < alternateEntityToolIds.length; j++) {

                    if (alternateEntityToolIds[j].equals(
                            autoPlaceToolIds[i])) {
                        return autoPlaceToolIds[i];
                    }
                }
            }
        }

        return null;

    }

    /**
     * Find an auto add entity match that is already in existence that would
     * be part of the reason an illegal collision exists when trying to add
     * and auto add entity.
     *
     * @param entity The auto add entity attempting to add
     * @param rch RuleCollisionHander to use
     * @return The entity id of the match, or -1 if it doesn't exist
     */
    private static int findExistingAutoAddMatch(
            Entity entity,
            RuleCollisionHandler rch) {

        // alternate products to examine
        String[] skuX3DProd = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);

        // The current collisions to check against
        ArrayList<Entity> collisionResults = rch.collisionEntities;

        String toolID = entity.getToolID();

        for (Entity collision : collisionResults) {

            String collisionToolID = collision.getToolID();

            if (toolID.equals(collisionToolID)) {

                return collision.getEntityID();

            } else if (skuX3DProd != null) {

                for (int i = 0; i < skuX3DProd.length; i++) {

                    if (skuX3DProd[i].equals(collisionToolID)) {

                        return collision.getEntityID();

                    }
                }
            }
        }

        return -1;
    }
}
