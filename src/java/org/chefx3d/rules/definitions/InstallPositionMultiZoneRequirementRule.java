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

package org.chefx3d.rules.definitions;

//External Imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.InstallPositionMultiZoneRequirementUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Multizone version of the InstallPositionMultiZoneRequirementRule.
 *
 * Applies specific positioning requirements to the placement of an entity.
 * Requirements are based on the legal collisions found and the ability to
 * place correctly relative to them.
 *
 * Each specified relationship must have a related position x,y,z value index
 * matched to it. For example a classRelationship of [wall standard standardA:
 * standardB] with respective amounts of [1 2 1] would require 5 position
 * values. 1 wall + 2 standards + 1 standardA + 1 standardB. If any of these
 * should not have a position requirement, assign the x,y and z values to the
 * class constant MAGIC_DNE_VALUE, whatever it is set to.
 *
 * Reference Frame of the objects is:
 *
 *       aEntity
 *  ---------- +++
 *               +    cEntity
 *               +
 *               |
 *               |
 *               |    bEntity
 *               |
 *
 * @author Alan Hudson
 * @version $Revision: 1.47 $
 */
public class InstallPositionMultiZoneRequirementRule extends BaseRule  {

//    private static final String POP_UP_TWO_FIXED_TARGETS =
//        "org.chefx3d.rules.definitions.InstallPositioinRequirementRule.twoFixedTargets";

    private static final String POP_UP_BOUNDS_EXCEEDED =
        "org.chefx3d.rules.definitions.InstallPositioinRequirementRule.boundsExceeded";

    private static final String POP_UP_MISSING_DATA =
        "org.chefx3d.rules.definitions.InstallPositionMultiZoneRequirementRule";

    private static final String STATUS_BAR_ADJUSTMENT_NOTE =
        "org.chefx3d.rules.definitions.InstallPositionMultiZoneRequirementRule.movingCollisions";
    
    private static final String FIXED_TARGET_NOTE = 
    	"org.chefx3d.rules.definitions.InstallPositionMultiZoneRequirementRule.fixedTargetNoMove";

    /** Standard status bar message to display for transient cases */
    private static String statusBarMessage;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public InstallPositionMultiZoneRequirementRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.STANDARD;

        statusBarMessage = intl_mgr.getString(STATUS_BAR_ADJUSTMENT_NOTE);
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

        // If the entity isn't a PositionableEntity, then bail.
        if (!(entity instanceof PositionableEntity)) {
        	result.setResult(true);
            return(result);
        }

        // Check for use of multi zone position requirement rule. If not used
        // don't evaluate this rule.
        Boolean usesPositionMZRule = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.
                COLLISION_POSITION_MULTI_ZONE_REQUIREMENTS);
		
        // If we aren't using this rule, still check to see if it is supporting
        // a product that does and issue the correct response to the command
        // being checked.
        if (!usesPositionMZRule) {

            boolean foundMZCollisions = false;

            // Use the initial positions for check
            Command dummyCommand = 
            	InstallPositionMultiZoneRequirementUtility.createDummyCommand(
            			model, entity, command);
            rch.performCollisionCheck(dummyCommand, true, false, false);

            if (rch.collisionEntities != null) {

                boolean colUsesPositionMZRule;

                // touching geometry has this rule then disallow
                // move/transitionEntity

                for(int i = 0; i < rch.collisionEntities.size(); i++) {

                    Entity entityObj = rch.collisionEntities.get(i);

                    colUsesPositionMZRule = (Boolean)
                        RulePropertyAccessor.getRulePropertyValue(
                            entityObj,
                            ChefX3DRuleProperties.
                            COLLISION_POSITION_MULTI_ZONE_REQUIREMENTS);

                    if (colUsesPositionMZRule) {
                    	
                    	if (rch.isDependantFixedEntity(
                    			model, 
                    			(PositionableEntity) entity, 
                    			rch, 
                    			view)) {
                    	
	                        foundMZCollisions = true;
	                        break;
                    	}
                    }
                }

                if (!command.isTransient() && foundMZCollisions) {

                	if (command instanceof MoveEntityCommand ||
                        command instanceof TransitionEntityChildCommand) {

                		String msg = intl_mgr.getString(FIXED_TARGET_NOTE);
                        popUpMessage.showMessage(msg);
                        
                		result.setApproved(false);
                		result.setNotApprovedAction(
                				NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                        result.setResult(false);
                        return(result);
                    }
                } else if (foundMZCollisions) {
                	
                	String msg = intl_mgr.getString(FIXED_TARGET_NOTE);
                	statusBar.setMessage(msg);
                    
                	result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                }
            }

            result.setResult(true);
            return(result);
        }

        // Check for rule use position collision requirements rule. If not used
        // don't evalute this rule.
        Boolean usesPositionRule = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS);

        if(usesPositionRule == null || !usesPositionRule){
            result.setResult(true);
            return(result);
        }

        // Check for correct command case to evaluate
        if(command instanceof AddEntityChildCommand){
            ;
        } else if (command instanceof AddEntityCommand){
            ;
        } else if (command instanceof MoveEntityCommand){
            ;
        } else if (command instanceof MoveEntityTransientCommand){
            ;
        } else if (command instanceof TransitionEntityChildCommand) {
            ;
        } else {

            // If not one of the required types return
            result.setResult(true);
            return(result);
        }

        // Set off processing of command
        boolean valid =
        	startProcessing(
        			command,
        			(PositionableEntity)entity);

        // If not a valid result, set the failure settings of our result and
        // any messages to display.
        if (!valid) {

            result.setStatusValue(ELEVATION_LEVEL.SEVERE);

            if(!command.isTransient()) {
                result.setApproved(false);
                String msg = intl_mgr.getString(POP_UP_BOUNDS_EXCEEDED);
                popUpMessage.showMessage(msg);
            } else {
            	result.setStatusMessage(statusBarMessage);
            }
            result.setResult(false);
            return(result);

        }

        // Otherwise return normal result.
        result.setResult(true);
        return(result);

    }

    /**
     * Kick off processing of the rule
     *
     * @param model WorldModel to reference
     * @param command Command that was issued
     * @param entity Entity affected
     * @return True if command should continue, false otherwise
     */
    private boolean startProcessing(
            Command command,
            PositionableEntity entity) {

        // Perform collision check to see what we are working with.
        // Requires doing collision analysis
        rch.performCollisionCheck(command, false, false, true);

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
        if(rch.collisionEntities == null || rch.collisionEntities.size() == 0){

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
            String msg = intl_mgr.getString(POP_UP_BOUNDS_EXCEEDED);
            popUpMessage.showMessage(msg);
			
            return false;
        }

        // Get the starting and ending index of position values that match
        // the relationship being analyzed.
        String classRelationshipVal = classRelationship[legalIndex];
        int relationshipAmountVal = relationshipAmount[legalIndex];

        int startPosIndex =
            InstallPositionMultiZoneRequirementUtility.calculateStartPosIndex(
                classRelationship,
                relationshipAmount,
                legalIndex);

        int endPosIndex =
        	InstallPositionMultiZoneRequirementUtility.calculateEndPosIndex(
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

            String msg = intl_mgr.getString(POP_UP_MISSING_DATA);
            popUpMessage.showMessage(msg);
			
            return false;
        }

        // Generate the precise list of relationships to satisfy
        String[] relationships =
        	InstallPositionMultiZoneRequirementUtility.
        		buildFullRelationshipList(
                    classRelationshipVal,
                    relationshipAmountVal);

        //-------------------------------------------
        // Hand off to appropriate processing routine
        // based on the command being evaluated.
        //-------------------------------------------
        double[] processResult = new double[3];

        if(command instanceof AddEntityChildCommand){

            AddEntityChildCommand addEntityChildCmd =
                ((AddEntityChildCommand)command);

            Entity parentEntity = addEntityChildCmd.getParentEntity();
            int parentEntityID = parentEntity.getEntityID();

            processResult = processCollisionRequirements(
                    entity,
                    parentEntityID,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships,
                    command.isTransient());

            if (processResult == null) {
            	return false;
            }

            double[] currentPosition = TransformUtils.getExactPosition(entity);

            currentPosition[0] += processResult[0];
            currentPosition[1] += processResult[1];
            currentPosition[2] += processResult[2];

            entity.setPosition(currentPosition, false);

        } else if (command instanceof AddEntityCommand){

            processResult = processCollisionRequirements(
                    entity,
                    entity.getParentEntityID(),
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships,
                    command.isTransient());

            if (processResult == null) {
            	return false;
            }

            double[] currentPosition = TransformUtils.getExactPosition(entity);
            currentPosition[0] += processResult[0];
            currentPosition[1] += processResult[1];
            currentPosition[2] += processResult[2];

            entity.setPosition(currentPosition, false);

        } else if (command instanceof MoveEntityCommand){

        	Entity parentEntity =
        		SceneHierarchyUtility.getExactParent(model, entity);

        	if (parentEntity == null) {
        		return false;
        	}

        	int parentEntityID = parentEntity.getEntityID();

            processResult = processCollisionRequirements(
                    entity,
                    parentEntityID,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships,
                    command.isTransient());

            if (processResult == null) {
            	return false;
            }

            double[] currentPosition = TransformUtils.getExactPosition(entity);
            currentPosition[0] += processResult[0];
            currentPosition[1] += processResult[1];
            currentPosition[2] += processResult[2];

            ((MoveEntityCommand)command).setEndPosition(currentPosition);

        } else if (command instanceof MoveEntityTransientCommand) {

        	Entity parentEntity =
        		SceneHierarchyUtility.getExactParent(model, entity);

        	if (parentEntity == null) {
        		return false;
        	}

        	int parentEntityID = parentEntity.getEntityID();

            processResult = processCollisionRequirements(
                    entity,
                    parentEntityID,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships,
                    command.isTransient());

            if (processResult == null) {
            	return false;
            }

            double[] currentPosition = TransformUtils.getExactPosition(entity);

            currentPosition[0] += processResult[0];
            currentPosition[1] += processResult[1];
            currentPosition[2] += processResult[2];

            ((MoveEntityTransientCommand)command).setPosition(currentPosition);

        } else if (command instanceof TransitionEntityChildCommand) {

        	Entity parentEntity =
        		SceneHierarchyUtility.getExactParent(model, entity);

        	if (parentEntity == null) {
        		return false;
        	}

        	int parentEntityID = parentEntity.getEntityID();

            if (!command.isTransient()) {

                processResult = processCollisionRequirements(
                        entity,
                        parentEntityID,
                        xPosValues,
                        yPosValues,
                        zPosValues,
                        posTolerance,
                        targetAdjustmentAxis,
                        startPosIndex,
                        endPosIndex,
                        relationships,
                        command.isTransient());

                if (processResult == null) {
                	return false;
                }

                double[] currentPosition =
                	TransformUtils.getExactPosition(entity);
                currentPosition[0] += processResult[0];
                currentPosition[1] += processResult[1];
                currentPosition[2] += processResult[2];

                ((TransitionEntityChildCommand)command).setEndPosition(
                		currentPosition);
            }
        }

        return true;
    }

    //---------------------------------------------------------------
    // Processing routines
    //---------------------------------------------------------------

    /**
     * Process collision requirements, by calculating the offical adjustment
     * to apply to the enity and issuing the scale, or move commands needed
     * to adjust the entities satisfying the collision position requirements.
     *
     * @param entity Entity with collision position requirements
     * @param parentEntityID Parent entity ID of entity (done this way for add
     * cases)
     * @param xPosValues The x axis position collision requirements
     * @param yPosValues The y axis position collision requirements
     * @param zPosValues The z axis position collision requirements
     * @param posTolerance The collision position tolerance values
     * @param targetAdjustmentAxis The collision position adjustment axis
     * @param startPosIndex The starting index for the matched relationship to
     * start evaluating indexes at (xPosValues, yPosValues etc)
     * @param endPosIndex The ending index for the matched relationship to
     * stop evaluating indexes at (xPosValues, yPosValues etc)
     * @param relationships The relationships containing a matching collision
     * set found to evaluate against
     * @param isTransient True if command is a transient command, false
     * otherwise
     * @return Adjustment to apply to entity, or null if the installation
     * position multi zone requirements were not satisfied.
     */
    private double[] processCollisionRequirements(
            Entity entity,
            int parentEntityID,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships,
            boolean isTransient){
			
    	// Side pocket to set back later
        int originalParentEntityId = entity.getParentEntityID();

        // Temporarily set the new parent entity ID
        entity.setParentEntityID(parentEntityID);

		// the adjustment for the entity
		double[] entity_adjustment = new double[3];

        // Determine the adjustments necessary for supporting entities
		HashMap<PositionableEntity, Vector3f> adjustmentMap =
            InstallPositionMultiZoneRequirementUtility.evaluate(
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
				entity_adjustment,
				rch,
				view);
		
        // Set back the side pocketed data
        entity.setParentEntityID(originalParentEntityId);

		if (adjustmentMap != null) {
			if (!isTransient) {
				// If the command is not transient, adjust the supporting entities
				for (Iterator<PositionableEntity> i = adjustmentMap.keySet().iterator(); i.hasNext();) {
					
					PositionableEntity pe = i.next();
					Vector3f adjustment = adjustmentMap.get(pe);
					if (!adjust(pe, adjustment)) {
						entity_adjustment = null;
						break;
					}
				
					if (pe.getEntityID() == parentEntityID) {
						// rem: this is going to be wrong in a more general case 
						// where the entity is a descendant of the supporting 
						// entity, but not it's immediate child
						entity_adjustment[0] -= adjustment.x;
						entity_adjustment[1] -= adjustment.y;
						entity_adjustment[2] -= adjustment.z;
					}
				}
			}
		} else {
        	if (!isTransient) {
	            String msg = intl_mgr.getString(POP_UP_BOUNDS_EXCEEDED);
	            popUpMessage.showMessage(msg);
        	}
			entity_adjustment = null;
		}
		return(entity_adjustment);
    }

    //---------------------------------------------------------------
    // Supporting routines
    //---------------------------------------------------------------

	/** 
	 * Modify the position of the specified entity by the argument adjustment
	 *
	 * @param entity The entity to adjust
	 * @param adjustment The change in position to apply to the entity
	 * @return true if the adjustment is possible, false otherwise.
	 */
	private boolean adjust(PositionableEntity entity, Vector3f adjustment) {
			
    	double[] pos = new double[3];
    	pos = TransformUtils.getExactPosition(entity);

		pos[0] += adjustment.x;
		pos[1] += adjustment.y;
		pos[2] += adjustment.z;
		
    	ArrayList<Entity> startChildren =
    		SceneHierarchyUtility.getExactChildren(entity);

    	int transactionID =
    		SceneManagementUtility.moveEntitySaveChildren(
    			model,
    			collisionChecker,
    			entity,
    			null,
    			pos,
    			startChildren,
    			false);
    	
    	Command cmd = CommandSequencer.getInstance().getCommand(transactionID);
    	
    	HashSet<String> ignoreRuleList = new HashSet<String>();
    	ignoreRuleList.add(
    		"org.chefx3d.rules.definitions.InstallPositionMultiZoneRequirementRule");
    	ignoreRuleList.add(
			"org.chefx3d.rules.definitions.ProximityPositionSnapRule");
    	
    	if (cmd instanceof MoveEntityCommand) {
    		
    		((MoveEntityCommand)cmd).setIgnoreRuleList(ignoreRuleList);
    		
    	} else if (cmd instanceof TransitionEntityChildCommand) {
    		
    		((TransitionEntityChildCommand)cmd).setIgnoreRuleList(ignoreRuleList);
    	}
		
		// rem: not sure what would cause this to 'go bad'....
		return((transactionID != -1));
	}

	/**
	 * Return a short String identifier of the argument Entity
	 *
	 * @param entity The entity
	 * @return The identifier
	 */
	private String getIdentifier(Entity entity) {
		return("[id="+ entity.getEntityID() + ", name=\""+ entity.getName() +"\"]");
	}
}