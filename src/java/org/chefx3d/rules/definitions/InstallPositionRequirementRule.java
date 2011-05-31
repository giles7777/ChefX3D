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
import java.util.*;

import javax.vecmath.Vector3d;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.ExpertSceneManagementUtility;
import org.chefx3d.rules.util.InstallPositionRequirementUtility;
import org.chefx3d.rules.util.PositionCollisionData;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.rules.util.RuleUtils;

import org.chefx3d.rules.util.AutoAddUtility;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
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
 * The general process implemented for InstallationPositionRequirements is to
 * determine the adjustments required to satisfy the position requirements.
 * Move those entities to those positions, and then cancel and re-issue the
 * current command so it executes after the adjustments have been made. Not
 * doing it in this order causes downstream evaluation issues because the entity
 * is seen as being a dependent entity that can prevent the adjustment of its
 * own supporting products. We also make sure that for non transient move
 * commands the entity and children have surrogates in the removed position
 * so we avoid the same problem with entities already in existence.
 *
 * @author Ben Yarger
 * @version $Revision: 1.68 $
 */
public class InstallPositionRequirementRule extends BaseRule  {

    private static final String POP_UP_TWO_FIXED_TARGETS =
        "org.chefx3d.rules.definitions.InstallPositioinRequirementRule.twoFixedTargets";

    private static final String POP_UP_BOUNDS_EXCEEDED =
        "org.chefx3d.rules.definitions.InstallPositioinRequirementRule.boundsExceeded";

    private static final String POP_UP_MISSING_DATA =
        "org.chefx3d.rules.definitions.InstallPositionRequirementRule";

    private static final String STATUS_BAR_ADJUSTMENT_NOTE =
        "org.chefx3d.rules.definitions.InstallPositionRequirementRule.movingCollisions";

    private static final String STATUS_BAR_FIXED_TARGETS =
    	"org.chefx3d.rules.definitions.InstallPositionRequirementRule.twoFixedTargetsTransient";

    /**
     * Maximum allowable difference between requisite min and max position
     * adjustments required.
     */
    private static double MAX_ADJ_OFFSET_DEVIATION = 0.002;
    
    /** Epsilon test value */
    private static double EPSILON = 0.00001;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public InstallPositionRequirementRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.STANDARD;
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

        // Check for rule use
        Boolean usesPositionMZRule = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.COLLISION_POSITION_MULTI_ZONE_REQUIREMENTS);

        if(usesPositionMZRule) {
            // Ignore MZ versions
            result.setResult(true);
            return(result);
        }

        // Check for command case
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

        // Check for rule use
        Boolean usesPositionRule = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS);

        if(usesPositionRule == null || !usesPositionRule){
            result.setResult(true);
            return(result);
        }

        // Set off to process
        boolean valid = startProcessing(model, command, entity);

        if (!valid) {

            result.setStatusValue(ELEVATION_LEVEL.SEVERE);

            if(!command.isTransient()) {
                result.setApproved(false);
            }

            result.setResult(false);
            return(result);

        }

        // If the command isn't transient, then fake the removal of the entity
        // cancel the current command and then add the command to the end
        // of the newly issued command list. Lastly, have the command ignore
        // this rule class so we don't get stuck in a loop.
        // The purpose of all this is to eliminate the evaluation of the 
        // entity in the nudges, etc that occur as a result of adjustments to
        // accommodate the position install requirement part. Then, after all
        // the adjustments have been made, it swings back through and
        // completes the operation.
        if (!command.isTransient()) {
        	
        	if (command instanceof AddEntityChildCommand) {
        	
        		SceneManagementUtility.removeSurrogate(
        				collisionChecker, (PositionableEntity)entity);
        		
        	} else {
        	
	        	// Fake the removal of the entity
	        	Entity parent = SceneHierarchyUtility.getParent(model, entity);
	        	
	        	RemoveEntityChildCommand removeEntitySurrogateCmd = 
	        		new RemoveEntityChildCommand(model, parent, entity);
	        	
	        	ExpertSceneManagementUtility.removedDeadCommands(
	        			model, rch.getRuleCollisionChecker(), entity);
	        	
	        	SceneManagementUtility.addSurrogate(
	        			collisionChecker, 
	        			removeEntitySurrogateCmd);

        	}
        	
        	// Add a copy of the command to the end of the newly issued
        	// command list (without a surrogate for it). Make sure it ignores
        	// this rule class.
        	issueCopyOfCommand(command, true);
        	
        	// Cancel the current command
        	result.setApproved(false);
        	result.setNotApprovedAction(
        			NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);
        	result.setResult(false);
        	
        } else {
        	result.setResult(false);
        }
        
        return(result);

    }



    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

    /**
     * Kick off processing of the rule
     *
     * @param model WorldModel to reference
     * @param command Command that was issued
     * @param entity Entity affected
     * @return True if command should continue, false otherwise
     */
    private boolean startProcessing(
            WorldModel model,
            Command command,
            Entity entity){

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
        if (rch.hasIllegalCollisionHelper(entity)) {

            if(command.isTransient()){
                return true;
            } else {
                return false;
            }
        }

        // Retrieve the legal classification index. If -1 stop execution
        int legalIndex =
            rch.hasLegalCollisions(
            		collisionResults.getEntityMatchCountMap(),
                    classRelationship,
                    relationshipAmount,
                    relModifier);

        if (legalIndex < 0) {

            legalIndex =
                rch.hasLegalCollisions(
                		collisionResults.getWallMatchCountMap(),
                        classRelationship,
                        relationshipAmount,
                        relModifier);

            if(legalIndex < 0){

                legalIndex =
                    rch.checkCrossCategoryCollisions(
                            classRelationship,
                            relationshipAmount,
                            relModifier);

                if (legalIndex < 0) {
                    String msg = intl_mgr.getString(POP_UP_BOUNDS_EXCEEDED);
                    popUpMessage.showMessage(msg);
                    return false;
                }
            }
        }


        // Get the starting and ending index of position values that match
        // the relationship being analyzed.
        String classRelationshipVal = classRelationship[legalIndex];
        int relationshipAmountVal = relationshipAmount[legalIndex];

        int startPosIndex =
            InstallPositionRequirementUtility.calculateStartPosIndex(
                classRelationship,
                relationshipAmount,
                legalIndex);

        int endPosIndex =
        	InstallPositionRequirementUtility.calculateEndPosIndex(
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
            InstallPositionRequirementUtility.buildFullRelationshipList(
                    classRelationshipVal,
                    relationshipAmountVal);

        // Hand off to appropriate processing routine
        boolean processResult = false;

        if(command instanceof AddEntityChildCommand){

            AddEntityChildCommand addEntityChildCmd =
                ((AddEntityChildCommand)command);

            Entity parentEntity = addEntityChildCmd.getParentEntity();
            int parentEntityID = parentEntity.getEntityID();

            processResult = processAdd(
                    model,
                    entity,
                    parentEntityID,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships);

        } else if (command instanceof AddEntityCommand){

            processResult = processAdd(
                    model,
                    entity,
                    entity.getParentEntityID(),
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships);

        } else if (command instanceof MoveEntityCommand){

            processResult = processMove(
                    model,
                    (MoveEntityCommand)command,
                    entity,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships);

        } else if (command instanceof MoveEntityTransientCommand) {

            processResult = processMoveTransient(
                    model,
                    (MoveEntityTransientCommand)command,
                    entity,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships);

        } else if (command instanceof TransitionEntityChildCommand) {

            if (command.isTransient()) {
                processResult = true;
            } else {
                processResult = processTransition(
                        model,
                        (TransitionEntityChildCommand)command,
                        entity,
                        xPosValues,
                        yPosValues,
                        zPosValues,
                        posTolerance,
                        targetAdjustmentAxis,
                        startPosIndex,
                        endPosIndex,
                        relationships);
            }
        }

        return processResult;
    }


    //---------------------------------------------------------------
    // Processing routines
    //---------------------------------------------------------------


    /**
     * Process add commands. If possible adjust the
     * collision parts to assist with placement. Note, no parts will be
     * adjusted if they have other product attached, or if it violates
     * span requirements.
     *
     * Note: startPosIndex and endPosIndex define the specific range
     * of position values to evaluate. These are matched to the list of
     * legal relationships and because we are dealing with a subset of those
     * relationships, we need to know the matching range of position values.
     *
     * @param model WorldModel to reference
     * @param entity Entity affected by command
     * @param xPosValues X axis position requirements
     * @param yPosValues Y axis position requirements
     * @param zPosValues Z axis position requirements
     * @param posTolerance Tolerance values for each position requirement
     * @param targetAdjustmentAxis Adjustment axis for each position
     * @param startPosIndex Starting index for position values
     * @param endPosIndex End index for position values
     * @param relationships Relationships we are fitting to
     * @return True if successful, false otherwise
     */
    private boolean processAdd(
            WorldModel model,
            Entity entity,
            int parentEntityID,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships){

        int originalParentEntityId = entity.getParentEntityID();
        entity.setParentEntityID(parentEntityID);

        // Generate the position collision data objects for evaluation
        PositionCollisionData[] posColData =
            InstallPositionRequirementUtility.buildPositionCollisionData(
                model,
                entity,
                view,
                xPosValues,
                yPosValues,
                zPosValues,
                posTolerance,
                targetAdjustmentAxis,
                startPosIndex,
                endPosIndex,
                relationships,
                rch);

        entity.setParentEntityID(originalParentEntityId);

        // Make sure we got position collision data
        if(posColData == null){
            String msg = intl_mgr.getString(POP_UP_BOUNDS_EXCEEDED);
            popUpMessage.showMessage(msg);
            return false;
        }

        // Begin process of repositioning collision products
        Entity fixedEntity = null;
        double[] fixedEntityAdjustment = new double[3];
        ArrayList<Command> cmdList = new ArrayList<Command>();
        int posColDataIndex = -1;

        double[] pos = new double[3];
        ((PositionableEntity)entity).getPosition(pos);

        // Isolate the sole fixed entity that cannot move.
        // It is possible there may not be one.
        for(int i = 0; i < posColData.length; i++){

            if(posColData[i].isAdjustmentNeeded() &&
                    posColData[i].isFixedEntity()){

                if(fixedEntity != null){

                    boolean result =
                        attemptToSnapToPositions(posColData, pos);

                    if (result) {

                        ((PositionableEntity)entity).setPosition(pos, false);
                        return true;
                    } else {

                        String msg = intl_mgr.getString(POP_UP_TWO_FIXED_TARGETS);
                        popUpMessage.showMessage(msg);
                        return false;
                    }

                } else {

                    fixedEntity = posColData[i].getEntity();
                    posColData[i].getDistance(fixedEntityAdjustment);
                    posColDataIndex = i;

                }
            }
        }

        // Attempt to issue move commands for all of the collision entities
        cmdList =
            updateCollisionPositions(
                    model,
                    entity,
                    posColData,
                    fixedEntityAdjustment,
                    false);

        if(cmdList == null){

            String msg = intl_mgr.getString(POP_UP_TWO_FIXED_TARGETS);
            popUpMessage.showMessage(msg);
            return false;
        }

        // If the collision entity is the parent of the entity everything
        // is moving relative to, move the entity back the same offset
        // as the parent in the opposite direction
        for (int i = 0; i < cmdList.size(); i++) {

            MoveEntityCommand mvCmd = (MoveEntityCommand) cmdList.get(i);

            if (mvCmd.getEntity().getEntityID() == parentEntityID) {

                double[] startPosition = new double[3];
                double[] endPosition = new double[3];

                mvCmd.getStartPosition(startPosition);
                mvCmd.getEndPosition(endPosition);

                double[] offset = new double[3];

                offset[0] = startPosition[0] - endPosition[0];
                offset[1] = startPosition[1] - endPosition[1];
                offset[2] = startPosition[2] - endPosition[2];

                double[] currentPos = new double[3];

                ((PositionableEntity)entity).getPosition(currentPos);

                currentPos[0] = currentPos[0] + offset[0];
                currentPos[1] = currentPos[1] + offset[1];
                currentPos[2] = currentPos[2] + offset[2];

                ((PositionableEntity)entity).setPosition(currentPos, false);

                break;
            }
        }

        // Add newly issued command list to be executed
        addNewlyIssuedCommand(cmdList);

        // If we have a fixedEntity, move the adjustment distance in the
        // axis adjustment direction to position correctly.
        // Otherwise, everything else is moving relative to the entity.
        if(fixedEntity != null){

            double[] endPos = new double[3];
            double[] endPosAdjustment = new double[3];

            ((PositionableEntity)entity).getPosition(endPos);
            posColData[posColDataIndex].getDistance(endPosAdjustment);

            switch(posColData[posColDataIndex].getAdjustmentAxis()){

            case XAXIS:
                endPos[0] += endPosAdjustment[0];
                break;

            case YAXIS:
                endPos[1] += endPosAdjustment[1];
                break;

            case ZAXIS:
                endPos[2] += endPosAdjustment[2];
                break;

            }

            ((PositionableEntity)entity).setPosition(endPos, false);
        }

        return true;
    }

    /**
     * Handle movement (non transient) commands. If possible adjust the
     * collision parts to assist with placement. Note, no parts will be
     * adjusted if they have other product attached, or if it violates
     * span requirements.
     *
     * Note: startPosIndex and endPosIndex define the specific range
     * of position values to evaluate. These are matched to the list of
     * legal relationships and because we are dealing with a subset of those
     * relationships, we need to know the matching range of position values.
     *
     * @param model WorldModel to reference
     * @param command MoveEntityCommand to evaluate
     * @param entity Entity affected by command
     * @param xPosValues X axis position requirements
     * @param yPosValues Y axis position requirements
     * @param zPosValues Z axis position requirements
     * @param posTolerance Tolerance values for each position requirement
     * @param targetAdjustmentAxis Adjustment axis for each position
     * @param startPosIndex Starting index for position values
     * @param endPosIndex End index for position values
     * @param relationships Relationships we are fitting to
     * @return True if successful, false otherwise
     */
    private boolean processMove(
            WorldModel model,
            MoveEntityCommand command,
            Entity entity,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships){

        int parentEntityID = entity.getParentEntityID();

        double[] endPos = new double[3];
        command.getEndPosition(endPos);
        
        double[] currentPos = new double[3];
        ((PositionableEntity)entity).getPosition(currentPos);
        
        ((PositionableEntity)entity).setPosition(endPos, false);

        // Generate the position collision data objects for evaluation
        PositionCollisionData[] posColData =
            InstallPositionRequirementUtility.buildPositionCollisionData(
                model,
                entity,
                view,
                xPosValues,
                yPosValues,
                zPosValues,
                posTolerance,
                targetAdjustmentAxis,
                startPosIndex,
                endPosIndex,
                relationships,
                rch);
        
        ((PositionableEntity)entity).setPosition(currentPos, false);

        // Make sure we got position collision data
        if(posColData == null){

            return false;
        }

        boolean result =
            attemptToSnapToPositions(posColData, endPos);

        if (result) {
            command.setEndPosition(endPos);
            return true;
        }

        // Begin process of repositioning collision products
        Entity fixedEntity = null;
        double[] fixedEntityAdjustment = new double[3];
        ArrayList<Command> cmdList = new ArrayList<Command>();
        int posColDataIndex = -1;

        // Isolate the sole fixed entity that cannot move.
        // It is possible there may not be one.
        for(int i = 0; i < posColData.length; i++){

            if(posColData[i].isAdjustmentNeeded() &&
                    posColData[i].isFixedEntity()){

                if(fixedEntity != null){

                    result =
                        attemptToSnapToPositions(posColData, endPos);

                    if (result) {

                        command.setEndPosition(endPos);
                        return true;
                    } else {
                        String msg = intl_mgr.getString(POP_UP_TWO_FIXED_TARGETS);
                        popUpMessage.showMessage(msg);
                        return false;
                    }

                } else {

                    fixedEntity = posColData[i].getEntity();
                    posColData[i].getDistance(fixedEntityAdjustment);
                    posColDataIndex = i;

                }
            }
        }

        // Attempt to issue move commands for all of the collision entities
        cmdList =
            updateCollisionPositions(
                    model,
                    entity,
                    posColData,
                    fixedEntityAdjustment,
                    false);

        if(cmdList == null){

            String msg = intl_mgr.getString(POP_UP_TWO_FIXED_TARGETS);
            popUpMessage.showMessage(msg);
            return false;
        }

        // If the collision entity is the parent of the entity everything
        // is moving relative to, move the entity back the same offset
        // as the parent in the opposite direction
        for (int i = 0; i < cmdList.size(); i++) {

            MoveEntityCommand mvCmd = (MoveEntityCommand) cmdList.get(i);

            if (mvCmd.getEntity().getEntityID() == parentEntityID) {

                double[] startPosition = new double[3];
                double[] endPosition = new double[3];

                mvCmd.getStartPosition(startPosition);
                mvCmd.getEndPosition(endPosition);

                double[] offset = new double[3];

                offset[0] = startPosition[0] - endPosition[0];
                offset[1] = startPosition[1] - endPosition[1];
                offset[2] = startPosition[2] - endPosition[2];

                endPos[0] = endPos[0] + offset[0];
                endPos[1] = endPos[1] + offset[1];
                endPos[2] = endPos[2] + offset[2];

                command.setEndPosition(endPos);

                break;
            }
        }

        // Add newly issued command list to be executed
        addNewlyIssuedCommand(cmdList);

        // If we have a fixedEntity, move the adjustment distance in the
        // axis adjustment direction to position correctly.
        // Otherwise, everything else is moving relative to the entity.
        if(fixedEntity != null){

            double[] endPosAdjustment = new double[3];

            posColData[posColDataIndex].getDistance(endPosAdjustment);

            switch(posColData[posColDataIndex].getAdjustmentAxis()){

            case XAXIS:
                endPos[0] += endPosAdjustment[0];
                break;

            case YAXIS:
                endPos[1] += endPosAdjustment[1];
                break;

            case ZAXIS:
                endPos[2] += endPosAdjustment[2];
                break;

            }

            command.setEndPosition(endPos);
        }
        
        // Finish by setting the entity in a removed position as a surrogate
        // so we don't have to worry about the moving standards evaluating
        // against it until its move command evaluates, which will be after
        // the standards have moved anyway. This is a temporary surrogate and
        // can be undone.
        SceneManagementUtility.fakeRemoveEntityWithSurrogate(
        		model, collisionChecker, entity);

        return true;
    }

    /**
     * Handle move transient command processing to try and snap product to
     * attachment points along adjustment axis.
     *
     * @param model WorldModel to reference
     * @param command Command changing world model
     * @param entity Entity changed by command
     * @param xPosValues Set of x positions expecting collisions
     * @param yPosValues Set of y positions expecting collisions
     * @param zPosValues Set of z positions expecting collisions
     * @param posTolerance Set of position specific tolerances
     * @param targetAdjustmentAxis Set of adjustment axis for each position
     * @param startPosIndex Starting index of position data
     * @param endPosIndex Starting index of position data
     * @param relationships Set of class relationships to match up with
     * @return True if command position updated, false otherwise
     */
    private boolean processMoveTransient(
            WorldModel model,
            MoveEntityTransientCommand command,
            Entity entity,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships){

        // Perform legal collision check to see if there would be a legal
        // collision and if so display the status bar message that explains
        // placement could require adjustment of installed products.
        rch.performCollisionCheck(command, true, false, false);
        rch.performCollisionAnalysisHelper(entity, null, false, null, true);

        if (!rch.hasIllegalCollisionHelper(entity)) {

            boolean showStatusMessage = true;

            // Don't show the status message if collision is not strictly with
            // TYPE_MODEL variants entities.
            for (int i = 0; i < rch.collisionEntities.size(); i++) {
                if (!rch.collisionEntities.get(i).isModel()) {
                    showStatusMessage = false;
                    break;
                }
            }

            if (showStatusMessage) {
                String msg = intl_mgr.getString(STATUS_BAR_ADJUSTMENT_NOTE);
                statusBar.setMessage(msg);
            }
        }

        // Regular processing
        double[] endPos = new double[3];
        command.getPosition(endPos);

        double[] currentPos = new double[3];
        ((PositionableEntity)entity).getPosition(currentPos);
        ((PositionableEntity)entity).setPosition(endPos, false);

        PositionCollisionData[] posColData =
            InstallPositionRequirementUtility.buildPositionCollisionData(
                model,
                entity,
                view,
                xPosValues,
                yPosValues,
                zPosValues,
                posTolerance,
                targetAdjustmentAxis,
                startPosIndex,
                endPosIndex,
                relationships,
                rch);

        ((PositionableEntity)entity).setPosition(currentPos, false);

        if(posColData == null){
            return true;
        }

        // Begin process of repositioning collision products
        Entity fixedEntity = null;
        double[] fixedEntityAdjustment = new double[3];
        ArrayList<Command> cmdList = new ArrayList<Command>();

        // Isolate the sole fixed entity that cannot move.
        // It is possible there may not be one.
        for(int i = 0; i < posColData.length; i++){

            if(posColData[i].isAdjustmentNeeded() &&
                    posColData[i].isFixedEntity()){

                if(fixedEntity != null){

                    boolean result =
                        attemptToSnapToPositions(posColData, endPos);

                    if (result) {

                        command.setPosition(endPos);
                        return true;
                    } else {

                    	String msg = intl_mgr.getString(STATUS_BAR_FIXED_TARGETS);
                        statusBar.setMessage(msg);

                        return false;
                    }

                } else {

                    fixedEntity = posColData[i].getEntity();
                    posColData[i].getDistance(fixedEntityAdjustment);

                }
            }
        }

        boolean result =
            attemptToSnapToPositions(posColData, endPos);

        if (result) {
            command.setPosition(endPos);
        }

        return true;
    }

    /**
     * Handle TransitionEntityChild commands. If possible adjust the
     * collision parts to assist with placement. Note, no parts will be
     * adjusted if they have other product attached, or if it violates
     * span requirements.
     *
     * Note: startPosIndex and endPosIndex define the specific range
     * of position values to evaluate. These are matched to the list of
     * legal relationships and because we are dealing with a subset of those
     * relationships, we need to know the matching range of position values.
     *
     * @param model WorldModel to reference
     * @param command TransitionEntityChildCommand to evaluate
     * @param entity Entity affected by command
     * @param xPosValues X axis position requirements
     * @param yPosValues Y axis position requirements
     * @param zPosValues Z axis position requirements
     * @param posTolerance Tolerance values for each position requirement
     * @param targetAdjustmentAxis Adjustment axis for each position
     * @param startPosIndex Starting index for position values
     * @param endPosIndex End index for position values
     * @param relationships Relationships we are fitting to
     * @return True if successful, false otherwise
     */
    private boolean processTransition(
            WorldModel model,
            TransitionEntityChildCommand command,
            Entity entity,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships){


        int parentEntityID = command.getEndParentEntity().getEntityID();
        int currentParentEntityID = entity.getParentEntityID();

        double[] endPos = new double[3];
        command.getEndPosition(endPos);

        double[] currentPos = new double[3];
        ((PositionableEntity)entity).getPosition(currentPos);

        ((PositionableEntity)entity).setPosition(endPos, false);
        ((PositionableEntity)entity).setParentEntityID(parentEntityID);

        // Generate the position collision data objects for evaluation
        PositionCollisionData[] posColData =
            InstallPositionRequirementUtility.buildPositionCollisionData(
                model,
                entity,
                view,
                xPosValues,
                yPosValues,
                zPosValues,
                posTolerance,
                targetAdjustmentAxis,
                startPosIndex,
                endPosIndex,
                relationships,
                rch);

        ((PositionableEntity)entity).setPosition(currentPos, false);
        ((PositionableEntity)entity).setParentEntityID(currentParentEntityID);

        // Make sure we got position collision data
        if(posColData == null){

            return false;
        }

        boolean result =
            attemptToSnapToPositions(posColData, endPos);

        if (result) {
            command.setEndPosition(endPos);
            return true;
        }

        // Begin process of repositioning collision products
        Entity fixedEntity = null;
        double[] fixedEntityAdjustment = new double[3];
        ArrayList<Command> cmdList = new ArrayList<Command>();
        int posColDataIndex = -1;

        // Isolate the sole fixed entity that cannot move.
        // It is possible there may not be one.
        for(int i = 0; i < posColData.length; i++){

            if(posColData[i].isAdjustmentNeeded() &&
                    posColData[i].isFixedEntity()){

                if(fixedEntity != null){

                    result =
                        attemptToSnapToPositions(posColData, endPos);

                    if (result) {

                        command.setEndPosition(endPos);
                        return true;
                    } else {
                        String msg = intl_mgr.getString(POP_UP_TWO_FIXED_TARGETS);
                        popUpMessage.showMessage(msg);
                        return false;
                    }

                } else {

                    fixedEntity = posColData[i].getEntity();
                    posColData[i].getDistance(fixedEntityAdjustment);
                    posColDataIndex = i;

                }
            }
        }

        // Attempt to issue move commands for all of the collision entities
        if (!command.isTransient()) {

	        cmdList =
	            updateCollisionPositions(
	                    model,
	                    entity,
	                    posColData,
	                    fixedEntityAdjustment,
	                    false);

	        if(cmdList == null){

	            String msg = intl_mgr.getString(POP_UP_TWO_FIXED_TARGETS);
	            popUpMessage.showMessage(msg);
	            return false;
	        }

	        // If the collision entity is the parent of the entity everything
	        // is moving relative to, move the entity back the same offset
	        // as the parent in the opposite direction
	        for (int i = 0; i < cmdList.size(); i++) {

	            MoveEntityCommand mvCmd = (MoveEntityCommand) cmdList.get(i);

	            if (mvCmd.getEntity().getEntityID() == parentEntityID) {

	                double[] startPosition = new double[3];
	                double[] endPosition = new double[3];

	                mvCmd.getStartPosition(startPosition);
	                mvCmd.getEndPosition(endPosition);

	                double[] offset = new double[3];

	                offset[0] = startPosition[0] - endPosition[0];
	                offset[1] = startPosition[1] - endPosition[1];
	                offset[2] = startPosition[2] - endPosition[2];

	                endPos[0] = endPos[0] + offset[0];
	                endPos[1] = endPos[1] + offset[1];
	                endPos[2] = endPos[2] + offset[2];

	                command.setEndPosition(endPos);

	                break;
	            }
	        }
        }

        // Add newly issued command list to be executed
        addNewlyIssuedCommand(cmdList);

        // If we have a fixedEntity, move the adjustment distance in the
        // axis adjustment direction to position correctly.
        // Otherwise, everything else is moving relative to the entity.
        if(fixedEntity != null){

            double[] endPosAdjustment = new double[3];

            posColData[posColDataIndex].getDistance(endPosAdjustment);

            switch(posColData[posColDataIndex].getAdjustmentAxis()){

            case XAXIS:
                endPos[0] += endPosAdjustment[0];
                break;

            case YAXIS:
                endPos[1] += endPosAdjustment[1];
                break;

            case ZAXIS:
                endPos[2] += endPosAdjustment[2];
                break;

            }

            command.setEndPosition(endPos);
        }
        
        // Finish by setting the entity in a removed position as a surrogate
        // so we don't have to worry about the moving standards evaluating
        // against it until its move command evaluates, which will be after
        // the standards have moved anyway. This is a temporary surrogate and
        // can be undone.
        SceneManagementUtility.fakeRemoveEntityWithSurrogate(
        		model, collisionChecker, entity);

        return true;
    }




    //---------------------------------------------------------------
    // Supporting routines
    //---------------------------------------------------------------



    /**
     * Create the list of move commands to execute on collision entities.
     * If there was an issue with the generation of the commands then null
     * will be returned indicating that the process should not continue. An
     * example would be a resulting illegal condition from moving one of the
     * collision entities. We cannot cause illegal conditions so therefore we
     * stop further processing on the original command and reject it.
     *
     * @param model WorldModel to reference
     * @param entityToIgnore The entity being moved by the user to ignore
     * @param posColData PositionCollisionData array to evaluate
     * @param fixedEntityAdjustment Fixed entity adjustment to consider
     * @return ArrayList<Command> if successful, otherwise null
     */
    private ArrayList<Command> updateCollisionPositions(
            WorldModel model,
            Entity entityToIgnore,
            PositionCollisionData[] posColData,
            double[] fixedEntityAdjustment,
            boolean transientMove){

        ArrayList<Command> cmdList = new ArrayList<Command>();

        // Update all positions relative to fixed entity
        for(int i = 0; i < posColData.length; i++){

            // If this is the fixed entity, skip it
            if(posColData[i].isFixedEntity()){

                continue;
            }

            double[] distance = new double[3];

            Entity colEntity = posColData[i].getEntity();
            posColData[i].getDistance(distance);

            // Adjust distance value based on fixed entity if exists
            distance[0] -= fixedEntityAdjustment[0];
            distance[1] -= fixedEntityAdjustment[1];
            distance[2] -= fixedEntityAdjustment[2];

            // Constraint distance to axis of adjustment
            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS axis =
                posColData[i].getAdjustmentAxis();

            switch (axis) {

            case XAXIS:
                distance[1] = 0.0;
                distance[2] = 0.0;
                break;

            case YAXIS:
                distance[0] = 0.0;
                distance[2] = 0.0;
                break;

            case ZAXIS:
                distance[0] = 0.0;
                distance[1] = 0.0;
                break;
            }

            // Confirm new position wouldn't result in illegal collisions
            double[] startPosition = new double[3];
            double[] endPosition = new double[3];
            ((PositionableEntity)colEntity).getPosition(startPosition);

            endPosition[0] = startPosition[0] - distance[0];
            endPosition[1] = startPosition[1] - distance[1];
            endPosition[2] = startPosition[2] - distance[2];

            // Run the baseline collision check
            MoveEntityCommand baselineCmd =
                new MoveEntityCommand(
                        model,
                        model.issueTransactionID(),
                        (PositionableEntity)colEntity,
                        startPosition,
                        startPosition);

            rch.performCollisionCheck(baselineCmd, true, false, false);

            int ignoreListSize = rch.collisionEntities.size();

            if (entityToIgnore != null) {
                ignoreListSize++;
            }

            int[] idsToIgnore = new int[ignoreListSize];

            for (int j = 0; j < rch.collisionEntities.size(); j++) {
                idsToIgnore[j] = rch.collisionEntities.get(j).getEntityID();
            }

            if (entityToIgnore != null) {
                idsToIgnore[idsToIgnore.length-1] = entityToIgnore.getEntityID();
            }

            // Run the official collision check
            MoveEntityCommand mvCmd =
                new MoveEntityCommand(
                        model,
                        model.issueTransactionID(),
                        (PositionableEntity)colEntity,
                        endPosition,
                        startPosition);

            rch.performCollisionCheck(mvCmd, true, false, false);

            if(!rch.performCollisionAnalysisHelper(
                    colEntity,
                    null,
                    false,
                    idsToIgnore,
                    true)){

                return null;
            }

            if(rch.hasIllegalCollisionHelper(colEntity)){

                return null;
            }

            // Confirm that we still respect the bounds of the parent along the
            // adjustment axis.
            float[] bounds = new float[6];
            ((PositionableEntity)colEntity).getBounds(bounds);

            Entity parentEntity =
                model.getEntity(colEntity.getParentEntityID());

            double[] parentPosition = new double[3];
            float[] parentBounds = new float[6];

            parentPosition = TransformUtils.getPositionRelativeToZone(model, parentEntity);
            ((PositionableEntity)parentEntity).getBounds(parentBounds);

            double maxChildBounds;
            double minChildBounds;
            double maxParentBounds;
            double minParentBounds;

            switch (axis) {

            case XAXIS:

                maxChildBounds = parentPosition[0]+endPosition[0]+bounds[1];
                minChildBounds = parentPosition[0]+endPosition[0]+bounds[0];

                maxParentBounds = parentPosition[0] + parentBounds[1];
                minParentBounds = parentPosition[0] + parentBounds[0];

                if (maxChildBounds > maxParentBounds) {
                    return null;
                } else if (minChildBounds < minParentBounds) {
                    return null;
                }

                break;

            case YAXIS:

                maxChildBounds = parentPosition[1]+endPosition[1]+bounds[3];
                minChildBounds = parentPosition[1]+endPosition[1]+bounds[2];

                maxParentBounds = parentPosition[1] + parentBounds[3];
                minParentBounds = parentPosition[1] + parentBounds[2];

                if (maxChildBounds > maxParentBounds) {
                    return null;
                } else if (minChildBounds < minParentBounds) {
                    return null;
                }

                break;

            case ZAXIS:

                maxChildBounds = parentPosition[2]+endPosition[2]+bounds[5];
                minChildBounds = parentPosition[2]+endPosition[2]+bounds[4];

                maxParentBounds = parentPosition[2] + parentBounds[5];
                minParentBounds = parentPosition[2] + parentBounds[4];

                if (maxChildBounds > maxParentBounds) {
                    return null;
                } else if (minChildBounds < minParentBounds) {
                    return null;
                }

                break;
            }

//            SceneManagementUtility.addSurrogate(
//            		rch.getRuleCollisionChecker(), mvCmd)

            // Issue nudges for all entities in collision that auto place
//            for (int w = 0; w < baseLineCollisions.size(); w++) {
//
//            	PositionableEntity baseLineCollision = null;
//
//            	if (baseLineCollisions.get(w) instanceof PositionableEntity) {
//            		baseLineCollision =
//            			(PositionableEntity) baseLineCollisions.get(w);
//            	} else {
//            		continue;
//            	}
//
//            	if (baseLineCollision.getEntityID() == colEntity.getParentEntityID()) {
//            		continue;
//            	}
//
//            	// If the entity auto places products, then nudge it
//            	String[] autoPlaceProd = (String[])
//	        		RulePropertyAccessor.getRulePropertyValue(
//	        			baseLineCollision,
//	        			ChefX3DRuleProperties.AUTO_PLACE_OBJECTS_PROP);
//
//	            if (autoPlaceProd != null &&
//	            		entityToIgnore != baseLineCollision) {
//
//	            	if( transientMove ){
//	            	    // this transientNudge() apparently
//	            		// causes problems: see ticket 889.
//	            		//
//	            		// It was originally added for tickets 712 and 738,
//	            		// but it no longer seems to be needed....
//	            		//nudgeEntityTransient(model, baseLineCollision, position);
//
//	            	} else {
//
//	            	    // save the request
//	            	    nudgeMap.add(baseLineCollision);
//
//	            	}
//	            }
//            }

//			if( transientMove ){
//				//if transient evaluation, remove surrogate that was added at 2121
//				SceneManagementUtility.removeSurrogate(
//						rch.getRuleCollisionChecker(),
//						(PositionableEntity)entityToIgnore)
//			} else {
				
			if (!transientMove) {

				// Add move command to new list
				cmdList.add(mvCmd);
			}
        }

        // perform the final nudges to add back auto-add items, we only
        // want to nudge the final position of the product
//        Iterator<PositionableEntity> itr = nudgeMap.iterator();
//
//        while (itr.hasNext()) {
//            PositionableEntity entity = itr.next();
//            SceneManagementUtility.nudgeEntity(
//                    model,
//                    collisionChecker,
//                    entity,
//                    false);
//        }

        return cmdList;
    }

    /**
     * Calculates standard deviation for a set of values.
     *
     * @param values Set of values (population)
     * @return Standard deviation of the population
     */
    private double standardDeviation(double[] values) {

        double deviation = 0.0;

        // Null check
        if (values == null || values.length == 0) {
            return Double.MAX_VALUE;
        }

        // calculate average (mean)
        double average = 0.0;

        for (int i = 0; i < values.length; i++) {

            average = average + values[i];
        }

        average = average/values.length;

        // compute the difference from the average (mean)
        double populationStandardDeviation = 0.0;

        for (int i = 0; i < values.length; i++) {

            populationStandardDeviation =
                populationStandardDeviation +
                Math.pow((values[i] - average), 2);
        }

        deviation =
            populationStandardDeviation / values.length;

        deviation = Math.sqrt(deviation);

        return deviation;
    }

    /**
     * Given a set of PositionCollisionData determine if a possible snap to
     * operation can occur and update the endPos value with the adjustment.
     * Used by add command and move transient command cases.
     *
     * @param posColData PositionCollisionData array to analyze
     * @param endPos Position to update with adjustment if possible
     * @return True if adjustment made, false otherwise
     */
    private boolean attemptToSnapToPositions(
            PositionCollisionData[] posColData,
            double[] endPos){

        // Verify the axis specific adjustment values are within acceptable
        // range for adjustment calculation. For transient movement to lock in
        // and adjust the command, we need to have an average adjustment
        // standard deviation of < MAX_ADJ_OFFSET_DIFFERENTIAL.
        //
        // Also, all adjustments will need to be along the same axis for the
        // adjustment to be made.

        ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS adjAxis = null;
        double[] axisPosValues = new double[posColData.length];

        for(int i = 0; i < posColData.length; i++){

            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS tmpAdjAxis;
            double[] tmpOffset = new double[3];

            posColData[i].getDistance(tmpOffset);
            tmpAdjAxis = posColData[i].getAdjustmentAxis();

            // Make sure same axis
            if (i == 0) {
                adjAxis = tmpAdjAxis;
            } else if (adjAxis != tmpAdjAxis) {
                return true;
            }

            switch (adjAxis) {

            case XAXIS:
                axisPosValues[i] = tmpOffset[0];
                break;

            case YAXIS:
                axisPosValues[i] = tmpOffset[1];
                break;

            case ZAXIS:
                axisPosValues[i] = tmpOffset[2];
                break;
            }
        }

        double deviation = standardDeviation(axisPosValues);

        if (deviation > MAX_ADJ_OFFSET_DEVIATION) {
            return false;
        }

        // Determine the average offset
        double averageOffset = 0.0;

        for (int i = 0; i < axisPosValues.length; i++) {
            averageOffset = averageOffset + axisPosValues[i];
        }

        averageOffset = averageOffset / axisPosValues.length;

        // Update position based on targetAdjustmentAxis

        switch (adjAxis) {
        case XAXIS:
            endPos[0] = endPos[0] + averageOffset;
            break;

        case YAXIS:
            endPos[1] = endPos[1] + averageOffset;
            break;

        case ZAXIS:
            endPos[2] = endPos[2] + averageOffset;
            break;
        }

        return true;
    }

    /**
     * Issue a copy of the command onto the end of the newly issued command
     * queue. Copy any child commands as well and put them after the initial
     * parent command.
     * 
     * @param command Command to copy
     * @param ignoreThisRule Force re-evaluation of command copy to ignore
     * this rule the next time through.
     */
    private void issueCopyOfCommand(
    		Command command,
    		boolean ignoreThisRule) {
    	
    	Command cmdCopy = null;
    	
    	HashSet<String> ignoreRuleList = command.getIgnoreRuleList();  
    	
    	if (ignoreRuleList == null) {
    		ignoreRuleList = new HashSet<String>();
    	}
    	
        ignoreRuleList.add("org.chefx3d.rules.definitions.InstallPositionRequirementRule");
    	
    	if(command instanceof AddEntityChildCommand){
    		
    		AddEntityChildCommand addCmd = (AddEntityChildCommand) command;
            
    		cmdCopy = new AddEntityChildCommand(
    				model,
    				model.issueTransactionID(),
    				addCmd.getParentEntity(), 
    				addCmd.getEntity(),
    				true);
    		
    		ignoreRuleList.add("org.chefx3d.rules.definitions.AddEntityCheckForCollisionsRule");
    		
        } else if (command instanceof MoveEntityCommand){
            
        	MoveEntityCommand mvCmd = (MoveEntityCommand) command;
        	double[] endPosition = new double[3];
        	double[] startPosition = new double[3];
        	ArrayList<Entity> startChildren = new ArrayList<Entity>();
        	
        	mvCmd.getEndPosition(endPosition);
        	mvCmd.getStartPosition(startPosition);;
        	Entity[] startChildrenSet = mvCmd.getStartChildren();
        	
        	if (startChildrenSet != null) {
        		
        		for (int i = 0; i < startChildrenSet.length; i++) {
        			
        			startChildren.add(startChildrenSet[i]);
        		}
        	}
        	
        	cmdCopy = new MoveEntityCommand(
        			model, 
        			model.issueTransactionID(), 
        			(PositionableEntity) mvCmd.getEntity(), 
        			endPosition, 
        			startPosition, 
        			startChildren);
        	
        	ignoreRuleList.add("org.chefx3d.rules.definitions.MovementHasObjectCollisionsRule");
        	ignoreRuleList.add("org.chefx3d.rules.definitions.MovementCollisionRuleGroup");
        	ignoreRuleList.add("org.chefx3d.rules.definitions.MovementNoCollisionRule");
        	
        } else if (command instanceof TransitionEntityChildCommand &&
        		!command.isTransient()) {
        	
        	TransitionEntityChildCommand tranCmd = 
        		(TransitionEntityChildCommand) command;
        	
        	double[] startPosition = new double[3];
        	float[] startRot = new float[4];
        	float[] startScale = new float[3];
        	
        	double[] endPosition = new double[3];
        	float[] endRot = new float[4];
        	float[] endScale = new float[3];
        	
        	ArrayList<Entity> startChildren = new ArrayList<Entity>();
        	
        	Entity[] startChildrenSet = tranCmd.getStartChildren();
        	
        	if (startChildrenSet != null) {
        		
        		for (int i = 0; i < startChildrenSet.length; i++) {
        			
        			startChildren.add(startChildrenSet[i]);
        		}
        	}

        	tranCmd.getStartPosition(startPosition);
        	tranCmd.getStartingRotation(startRot);
        	tranCmd.getStartScale(startScale);
        	tranCmd.getEndPosition(endPosition);
        	tranCmd.getCurrentRotation(endRot);
        	tranCmd.getEndScale(endScale);
            
        	if (Arrays.equals(endScale, new float[] {0.0f, 0.0f, 0.0f})) {
        		endScale[0] = 1.0f;
        		endScale[1] = 1.0f;
        		endScale[2] = 1.0f;
        	}
        	
        	if (Arrays.equals(startScale, new float[] {0.0f, 0.0f, 0.0f})) {
        		startScale[0] = 1.0f;
        		startScale[1] = 1.0f;
        		startScale[2] = 1.0f;
        	}
        	
        	cmdCopy = new TransitionEntityChildCommand(
        			model, 
        			(PositionableEntity) tranCmd.getEntity(), 
        			tranCmd.getStartParentEntity(), 
        			startPosition, 
        			startRot, 
        			startScale, 
        			tranCmd.getEndParentEntity(), 
        			endPosition, 
        			endRot,
        			endScale, 
        			false);
        	
        	((TransitionEntityChildCommand)cmdCopy).setStartChildren(
        			startChildren);
        	
        	ignoreRuleList.add("org.chefx3d.rules.definitions.MovementHasObjectCollisionsRule");
        	ignoreRuleList.add("org.chefx3d.rules.definitions.MovementCollisionRuleGroup");
        	ignoreRuleList.add("org.chefx3d.rules.definitions.MovementNoCollisionRule");
        }
    	
    	if (cmdCopy != null) {
    		
    		if (ignoreThisRule) {
	            cmdCopy.setIgnoreRuleList(ignoreRuleList);
    		}
    		
    		CommandSequencer.getInstance().addNewlyIssuedCommand(cmdCopy);
    		
    		// Go about copying and child command operations to the end of the
    		// queue.
    		Entity entity = ((RuleDataAccessor)command).getEntity();
    		
    		ArrayList<Entity> children = 
    			SceneHierarchyUtility.getExactChildren(entity);
    		
    		ArrayList<Command> fullCmdSequence = (ArrayList<Command>)
    			CommandSequencer.getInstance().getFullCommandList(true);
    		
    		for (Entity child : children) {
    			
    			for (Command cmd : fullCmdSequence) {
    				
    				if (cmd instanceof RuleDataAccessor) {
    					
    					if (((RuleDataAccessor)cmd).getEntity() == child) {
    						
    						CommandSequencer.getInstance().removeCommand(cmd);
    						issueCopyOfCommand(cmd, false);
    					}
    				}
    			}
    		}
    	}
    }

}
