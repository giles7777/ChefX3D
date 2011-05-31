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
import java.util.ArrayList;

import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.RuleDataAccessor;
import org.chefx3d.model.ScaleEntityTransientCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.ExpertSceneManagementUtility;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;
import org.chefx3d.view.awt.av3d.AV3DConstants;
import org.chefx3d.view.common.EditorView;

/**
 * Adjusts the orientation of entities 90 degrees clockwise or counter
 * clockwise depending on which edge of the wall they are on. Designed
 * specifically for segment zones and switching at the left and right
 * extends of the active zone.
 *
 * @author Ben Yarger
 * @version $Revision: 1.57 $
 */
public class SegmentEdgeEntityOrientationRule extends BaseRule  {

    /** Status message for pop up message */
    private static final String POP_UP_MSG =
        "org.chefx3d.rules.definitions.SegmentEdgeEntityOrientationRule.illegalCollision";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SegmentEdgeEntityOrientationRule(
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

        // If we don't have a positionable entity, don't bother processing
        if(!(entity instanceof PositionableEntity)){
            result.setResult(true);
            return(result);
        }

        // Check for flag and attempt to exit as early as possible. Only
        // evaluate if true.
        Boolean hasOrientationRequirement = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
                    ChefX3DRuleProperties.HAS_ORIENTATION_REQUIREMENTS);

        if(hasOrientationRequirement == null ||
                hasOrientationRequirement == false){
            result.setResult(true);
            return(result);
        }

        // Get the transient orientation
        ChefX3DRuleProperties.ORIENTATION_STATE liveStateOrientation =
            (ChefX3DRuleProperties.ORIENTATION_STATE)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.LIVE_STATE_ORIENTATION);

        //----------------------------------------------------------------
        // Process data and issue swap if necessary
        //----------------------------------------------------------------
        if(command instanceof MoveEntityCommand){

            Entity parentEntity = 
                SceneHierarchyUtility.getExactParent(model, entity);

            moveCommandResponse(
                    liveStateOrientation,
                    (PositionableEntity)entity,
                    parentEntity);

        } else if (command instanceof MoveEntityTransientCommand){

            Entity parentEntity = 
                SceneHierarchyUtility.getExactParent(model, entity);

            moveTransientCommandResponse(
                    liveStateOrientation,
                    (PositionableEntity)entity,
                    parentEntity);
            
        } else if (command instanceof TransitionEntityChildCommand && 
                !command.isTransient()) {
                
            Entity parentEntity =
                ((TransitionEntityChildCommand)command).getEndParentEntity();

            transitionCommandResponse(
                    liveStateOrientation,
                    (PositionableEntity)entity,
                    parentEntity,
                    command.isTransient());

        } else if (command instanceof AddEntityChildCommand){

            Entity parentEntity =
                ((AddEntityChildCommand)command).getParentEntity();

            addCommandResponse(
                    liveStateOrientation,
                    (PositionableEntity)entity,
                    parentEntity);

        }

        return result;

    }
    
    /**
     * Issue add command for move command case. If the swap will cause
     * collisions, then false will be returned indicating that the command
     * should not execute and rule checking should cease.
     *
     * @param setOrientation Original orientation of entity
     * @param liveStateOrientation Last known orientation
     * @param instanceOrientation Current instance orientation
     * @param model WorldModel to reference
     * @param entity Entity to update
     */
    private void moveCommandResponse(
            ChefX3DRuleProperties.ORIENTATION_STATE liveStateOrientation,
            PositionableEntity entity,
            Entity parentEntity){

        boolean validRequest = validateRequest(entity, parentEntity);
        
        if (validRequest) {
            
            // determine if swap should occur
            ChefX3DRuleProperties.ORIENTATION_STATE resultOrientation =
                calculateOrientation(model, parentEntity, entity, liveStateOrientation);
    
            swapModel(entity,
                    parentEntity,
                    resultOrientation);

        } else {
            
            // reset scale
            resetModel(entity);   
      
            // display the error message        
            String msg = intl_mgr.getString(POP_UP_MSG);         
            popUpMessage.showMessage(msg);
            result.setStatusValue(ELEVATION_LEVEL.SEVERE);
            result.setApproved(false);
            result.setResult(false);          

        }
 
    }
    
    /**
     * Processes transient movement commands and generates the appropriate
     * command to adjust the orientation if needed.
     *
     * @param setOrientation Originating orientation state
     * @param liveStateOrientation Last known orientation
     * @param instanceOrientation Orientation state at this instant
     * @param model WorldModel to reference
     * @param entity Entity affected by changes
     */
    private void moveTransientCommandResponse(
            ChefX3DRuleProperties.ORIENTATION_STATE liveStateOrientation,
            PositionableEntity entity,
            Entity parentEntity){

        boolean validRequest = validateRequest(entity, parentEntity);
        
        if (validRequest) {
            
            // determine if swap should occur
            ChefX3DRuleProperties.ORIENTATION_STATE resultOrientation =
                calculateOrientation(model, parentEntity, entity, liveStateOrientation);
    
            /*
             * At this point either we have a change to process or we don't.
             * The original state isn't considered because either the transient
             * orientation was set by a previous adjustment or it wasn't. If it
             * wasn't and we have not instanceOrientation then no orientation
             * change should occur and we catch that with the return above.
             * If instanceOrientation is set and transientOrientation is not
             * then we end up setting it below and issuing a swap.
             * If transientOrientation is set and instanceOrientation is not
             * processing below is skipped.
             */
            if(liveStateOrientation == null ||
                    liveStateOrientation != resultOrientation){

                setLiveStateOrientationProperty(
                        entity,
                        resultOrientation,
                        true);

                scaleModel(
                        entity,
                        parentEntity,
                        resultOrientation);
                
            } 
        }
    }

    /**
     * Issue add command for transition command case. If the swap will cause
     * collisions, then false will be returned indicating that the command
     * should not execute and rule checking should cease.
     *
     * @param setOrientation Original orientation of entity
     * @param liveStateOrientation Last known orientation
     * @param instanceOrientation Current instance orientation
     * @param model WorldModel to reference
     * @param entity Entity to update
     * @param isTransient Flag true if command is transient, false otherwise
     * @return True if successful, false otherwise
     */
    private void transitionCommandResponse(
            ChefX3DRuleProperties.ORIENTATION_STATE liveStateOrientation,
            PositionableEntity entity,
            Entity parentEntity,
            boolean isTransient){

        boolean validRequest = validateRequest(entity, parentEntity);
        
        if (validRequest) {
            
            // determine if swap should occur
            ChefX3DRuleProperties.ORIENTATION_STATE resultOrientation =
                calculateOrientation(model, parentEntity, entity, liveStateOrientation);
    
            swapModel(entity,
                    parentEntity,
                    resultOrientation);
        } else {
            
            // reset scale
            resetModel(entity);   
      
        }

    }

    /**
     * Issue add command for add command case. If the swap will cause
     * collisions, then false will be returned indicating that the command
     * should not execute and rule checking should cease.
     *
     * @param setOrientation Original orientation of entity
     * @param liveStateOrientation Last known orientation
     * @param instanceOrientation Current instance orientation
     * @param model WorldModel to reference
     * @param entity Entity to update
     * @return True if successful, false otherwise
     */
    private void addCommandResponse(
            ChefX3DRuleProperties.ORIENTATION_STATE liveStateOrientation,
            PositionableEntity entity,
            Entity parentEntity){

        boolean validRequest = validateRequest(entity, parentEntity);
        
        if (validRequest) {
            
            // determine if swap should occur
            ChefX3DRuleProperties.ORIENTATION_STATE resultOrientation =
                calculateOrientation(model, parentEntity, entity, liveStateOrientation);
    
            swapModel(entity,
                    parentEntity,
                    resultOrientation);

        } else {
            
            // reset scale
            resetModel(entity);   
      
        }           

    }

    /**
     * Swap the model for the one specified by toolID.
     *
     * @param model WorldModel to reference
     * @param entity Entity to swap out
     * @param parentEntity Parent to the entity
     * @param toolID Tool id of tool to generate new swap entity with
     * @return True if successful, false otherwise
     */
    private void scaleModel(
            Entity entity,
            Entity parentEntity,
            ChefX3DRuleProperties.ORIENTATION_STATE changeToState) {

        // Make sure we get the expected list of ids - should be length 2
        String[] swapIDList = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.ORIENTATION_SWAP_ID_LIST);

        if(swapIDList.length < 2){
            return;
        }

        // Extract the appropriate toolID based on value of changeToState
        String toolID;
        switch(changeToState){

            default:
            case LEFT:
                toolID = swapIDList[0];
                break;

            case RIGHT:
                toolID = swapIDList[1];
                break;
        }

        // Get the tool
        SimpleTool tool =
            (SimpleTool) catalogManager.findTool(toolID);

        if(tool == null){
            return;
        }

        // Get the current position
        if(!(entity instanceof PositionableEntity)){
            return;
        }

        double[] position = new double[3];

        PositionableEntity posEntity = (PositionableEntity)entity;
        posEntity.getPosition(position);

        float[] oldSize = new float[3];
        posEntity.getSize(oldSize);

//////////////////////////////////////////////////////////////////

        //Same block of code is in ScaleChangeModelRule.java

        // Create the new entity
        EntityBuilder entityBuilder = view.getEntityBuilder();

        Entity newEntity =
            entityBuilder.createEntity(
                    model,
                    entity.getEntityID(),
                    position,
                    new float[] {0.0f, 1.0f, 0.0f, 0.0f},
                    tool);

        if (parentEntity instanceof ZoneEntity) {

            // adjust the position by the change in bounds
            PositionableEntity newPosEntity = (PositionableEntity)newEntity;
            double[] newPos = new double[3];

            float[] newSize = new float[3];
            newPosEntity.getSize(newSize);

            float[] delta = new float[] {
                newSize[0] - oldSize[0],
                newSize[1] - oldSize[1],
                newSize[2] - oldSize[2]};

            float[] scale = new float[3];
            scale[0] = 1 - ((oldSize[0] - newSize[0]) / oldSize[0]);
            scale[1] = 1 - ((oldSize[1] - newSize[1]) / oldSize[1]);
            scale[2] = 1 - ((oldSize[2] - newSize[2]) / oldSize[2]);

            switch(changeToState){

                default:
                case LEFT:
                    newPos[0] = position[0] + (delta[0] * 0.5);
                    newPos[1] = position[1] + (delta[1] * 0.5);
                    newPos[2] = newSize[2] * 0.5 -
                        AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
                    break;

                case RIGHT:
                    newPos[0] = position[0] - (delta[0] * 0.5);
                    newPos[1] = position[1] + (delta[1] * 0.5);
                    newPos[2] = newSize[2] * 0.5 -
                        AV3DConstants.DEFAULT_EMBEDDING_DEPTH;
                    break;
            }

            // send a scale command
            ScaleEntityTransientCommand scaleCmd =
                new ScaleEntityTransientCommand(
                        model,
                        model.issueTransactionID(),
                        (PositionableEntity)entity,
                        newPos,
                        scale);

            model.forceCommandExecution(scaleCmd);

            // scale any children of the same tool
            scaleChildren(entity, scale);

        }

    }

    /**
     * Force the scale of all the children
     * 
     * @param model
     * @param entity
     * @param scale
     */
    private void scaleChildren(Entity entity, float[] scale) {

        double[] newPos = new double[3];

        if (entity.hasChildren()) {
            for (int i = 0; i < entity.getChildCount(); i++) {
                Entity child = entity.getChildAt(i);

                ((PositionableEntity)child).getPosition(newPos);

                // send a scale command
                ScaleEntityTransientCommand scaleCmd = new ScaleEntityTransientCommand(
                            model,
                            model.issueTransactionID(),
                            (PositionableEntity)child,
                            newPos,
                            scale);
                model.forceCommandExecution(scaleCmd);

                // continue down potential stack
                scaleChildren(child, scale);

            }
        }

    }

    /**
     * Swap the model for the one specified by toolID.
     *
     * @param entity Entity to swap out
     * @param parentEntity Parent to the entity
     * @param changeToState The orientation to change to
     */
    private void swapModel(
            Entity swapOutEntity,
            Entity parentEntity,
            ChefX3DRuleProperties.ORIENTATION_STATE changeToState) {

        // get the swap in entity
        Entity swapInEntity = 
            createSwapInEntity(swapOutEntity, parentEntity, changeToState);
        
        if (swapInEntity == null) 
            return;
               
        // now do the swap
        SceneManagementUtility.swapEntity(
                model, 
                rch, 
                view, 
                catalogManager, 
                swapInEntity, 
                swapOutEntity, 
                swapOutEntity.getStartingChildren(),
                false, 
                false);
   
        // try to swap the children as necessary
        
        // Get all of the commands on the queues
        ArrayList<Command> cmdList = (ArrayList<Command>) 
            CommandSequencer.getInstance().getNewlyIssuedCommandList();
        
        // By starting at the end of the list and working backwards
        // we are guaranteed to get the last command
        // issued affecting the entity.
        
        for (int i = (cmdList.size() - 1); i >= 0; i--) {
            
            Command cmd = cmdList.get(i);
            
            if (cmd instanceof AddEntityChildCommand) {
                
                Entity check = ((AddEntityChildCommand)cmd).getEntity();
                
                // If the the child is the same type as the one being swapped, 
                // then swap it as well
                if (check.getToolID().equals(swapOutEntity.getToolID())) {
                                        
                    swapOutEntity = 
                        ((AddEntityChildCommand)cmd).getEntity();
                    
                    parentEntity = 
                        ((AddEntityChildCommand)cmd).getParentEntity();
                    
                    // get the swap in entity
                    swapInEntity = 
                        createSwapInEntity(swapOutEntity, parentEntity, changeToState);

                    if (swapInEntity == null) 
                        return;

                    // now do the swap
                    SceneManagementUtility.swapEntity(
                            model, 
                            rch, 
                            view, 
                            catalogManager, 
                            swapInEntity, 
                            swapOutEntity, 
                            swapOutEntity.getStartingChildren(),
                            false, 
                            false);
             
                    // clean up the add command 
                    ExpertSceneManagementUtility.removedDeadCommands(
                            model, collisionChecker, check);

                }
            }
        }
         
        // kill the current command but continue to process the add 
        // and remove that was added to the queue by the swap entity call
        result.setApproved(false);
        result.setResult(false);          
        result.setNotApprovedAction(NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_DO_NEWLY_ISSUED_COMMANDS);                        

    }
        
    /**
     * Create the new entity to replace
     * 
     * @param swapOutEntity The entity being replaced
     * @param parentEntity The parent of the entity being replaced
     * @param changeToState The orientation to set
     * @return The swap in entity created, null if there was a failure
     */
    private Entity createSwapInEntity(
            Entity swapOutEntity, 
            Entity parentEntity, 
            ChefX3DRuleProperties.ORIENTATION_STATE changeToState) {
        
        // Make sure we get the expected list of ids - should be length 2
        String[] swapIDList = 
            (String[])RulePropertyAccessor.getRulePropertyValue(
                    swapOutEntity,
                    ChefX3DRuleProperties.ORIENTATION_SWAP_ID_LIST);

        if (swapIDList.length < 2) {
            
            // reset scale
            resetModel(swapOutEntity);         
            return null;
            
        }

        // Extract the appropriate toolID based on value of changeToState
        String toolID = "";

        switch(changeToState){

            case LEFT:
                toolID = swapIDList[0];
                break;

            case RIGHT:
                toolID = swapIDList[1];
                break;

        }

        // don't need to swap so return
        if (toolID.equals(swapOutEntity.getToolID())) {
            return null;
        }

        // Get the tool
        SimpleTool tool =
            (SimpleTool) catalogManager.findTool(toolID);

        if (tool == null || !(swapOutEntity instanceof PositionableEntity)) {
            
            // reset scale
            resetModel(swapOutEntity);   
            return null;
            
        }

        // first determine the final position
        double[] position = 
            TransformUtils.getExactPosition((PositionableEntity)swapOutEntity);
        
        // create the new entity
        EntityBuilder entityBuilder = view.getEntityBuilder();

        Entity swapInEntity =
            entityBuilder.createEntity(
                    model,
                    model.issueEntityID(),
                    position,
                    new float[] {0.0f, 1.0f, 0.0f, 0.0f},
                    tool);

        // make sure to correctly set the state
        swapInEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.CURRENT_ORIENTATION,
                changeToState,
                false);

        swapInEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.LIVE_STATE_ORIENTATION,
                changeToState,
                false);
        
        // update it's position to be correct if the parent will be the zone
        if (parentEntity instanceof SegmentEntity) {
            
            float[] bounds = new float[6];
            ((PositionableEntity)swapInEntity).getBounds(bounds);

            // the final position is parented to the zone, so use the bounds
            // and zone bounds to calculate the position, leave the height alone
            // since that may have been adjusted else where.
            switch(changeToState){

                case LEFT:
                    position[0] = -bounds[0];                    
                    break;

                case RIGHT:
                    position[0] = 
                        ((SegmentEntity)parentEntity).getLength() - bounds[1];                    
                    break;
                    
            }
            
            ((PositionableEntity)swapInEntity).setPosition(position, false);
            
        } 

        return swapInEntity;
    }
    
    /**
     * Set the live state orientation rule property.
     *
     * @param entity Entity to update property value for
     * @param state ChefX3DRuleProperties.ORIENTATION_STATE value
     * @param ongoing Is change ongoing or not
     */
    private void setLiveStateOrientationProperty(
            Entity entity,
            ChefX3DRuleProperties.ORIENTATION_STATE state,
            boolean ongoing){

        entity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.LIVE_STATE_ORIENTATION,
                state,
                ongoing);
    }

    /**
     * Reset the model scales, fail the command, and pop up a message
     * 
     * @param model
     * @param entity
     */
    private void resetModel(Entity entity) {

        float[] scale = new float[] {1, 1, 1};

        double[] pos = new double[3];
        ((PositionableEntity)entity).getStartingPosition(pos);

        // send a scale command
        ScaleEntityTransientCommand scaleCmd =
            new ScaleEntityTransientCommand(
                    model,
                    model.issueTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    scale);

        model.forceCommandExecution(scaleCmd);

        // scale any children of the same tool
        scaleChildren(entity, scale);
        
        // display the error message        
        String msg = intl_mgr.getString(POP_UP_MSG);         
        popUpMessage.showMessage(msg);
        
        result.setStatusValue(ELEVATION_LEVEL.SEVERE);
        result.setApproved(false);
        result.setResult(false);          

    }

    /**
     * Use the distance to each end of the wall to decide orientation
     *
     * @param parentEntity
     * @param entity
     * @param liveStateOrientation
     * @return
     */
    private ChefX3DRuleProperties.ORIENTATION_STATE calculateOrientation(
            WorldModel model,
            Entity parentEntity,
            PositionableEntity entity,
            ChefX3DRuleProperties.ORIENTATION_STATE liveStateOrientation) {

        // determine swap that should occur
        ChefX3DRuleProperties.ORIENTATION_STATE resultOrientation =
            ChefX3DRuleProperties.ORIENTATION_STATE.RIGHT;

        // get the position and zone
        double[] pos = new double[3];
        entity.getPosition(pos);

        // see if we have crossed the mid point of the wall
        if (parentEntity != null && parentEntity instanceof SegmentEntity) {

            SegmentEntity zone = (SegmentEntity)parentEntity;

            float midpoint = zone.getLength() * 0.5f;

            if (pos[0] >= midpoint) {
                resultOrientation =
                    ChefX3DRuleProperties.ORIENTATION_STATE.RIGHT;
            } else {
                resultOrientation =
                    ChefX3DRuleProperties.ORIENTATION_STATE.LEFT;
            }

        } else {

            // see if the parent has the same toolID, if not then
            // traverse the graph to get the zone
            String[] swapIDList =
                (String[]) RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.ORIENTATION_SWAP_ID_LIST);

            boolean isStack = false;
            int len = swapIDList.length;
            for (int i = 0; i < len; i++) {
                if (parentEntity.getToolID().equals(swapIDList[i])) {
                    isStack = true;
                    break;
                }
            }

            if (isStack) {

                // use the parant's value
                resultOrientation =
                    (ChefX3DRuleProperties.ORIENTATION_STATE)RulePropertyAccessor.getRulePropertyValue(
                            parentEntity,
                            ChefX3DRuleProperties.CURRENT_ORIENTATION);

            } else {

                double[] parentPos =
                    TransformUtils.getPositionRelativeToZone(model, parentEntity);
                parentEntity =
                    SceneHierarchyUtility.findZoneEntity(model, parentEntity);

                if (parentEntity instanceof SegmentEntity) {

                    SegmentEntity zone = (SegmentEntity)parentEntity;

                    float midpoint = zone.getLength() * 0.5f;

                    if (parentPos[0] + pos[0] >= midpoint) {
                        resultOrientation =
                            ChefX3DRuleProperties.ORIENTATION_STATE.RIGHT;
                    } else {
                        resultOrientation =
                            ChefX3DRuleProperties.ORIENTATION_STATE.LEFT;
                    }

                }

            }

        }

        return resultOrientation;
    }

    /**
     * 
     * @param entity
     * @param parentEntity
     * @return
     */
    private boolean validateRequest(
            PositionableEntity entity,
            Entity parentEntity) {
        
        if (parentEntity == null)
            return false;

        Entity zoneEntity =
            SceneHierarchyUtility.findZoneEntity(model, parentEntity);

        // check to see if it is restricted
        ChefX3DRuleProperties.ORIENTATION_STATE currentOrientation =
            (ChefX3DRuleProperties.ORIENTATION_STATE)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CURRENT_ORIENTATION);

        if (zoneEntity instanceof SegmentEntity) {

            boolean positive = false;

            // get position relative to the zone
            double[] entityPos = new double[3];
            entity.getPosition(entityPos);

            double[] parentPos = new double[3];
            parentPos = TransformUtils.getPositionRelativeToZone(model, parentEntity);

            entityPos[0] += parentPos[0];
            entityPos[1] += parentPos[1];
            entityPos[2] += parentPos[2];

            double midpoint = ((SegmentEntity)zoneEntity).getLength() / 2;

            if(entityPos[0] > midpoint) {
                positive = true;
            }
            
            int angle = 90;
            boolean invalidAngle = 
                TransformUtils.isWallAtSpecificAngle(
                        model,
                        entity,
                        parentEntity,
                        positive,
                        angle);
            
            if (invalidAngle) 
                return false;

            if (currentOrientation == ChefX3DRuleProperties.ORIENTATION_STATE.LEFT_ONLY) {

                // see if we have crossed the mid point of the wall
                if (entityPos[0] < midpoint) {
                    return false;
                }

            } else if (currentOrientation == ChefX3DRuleProperties.ORIENTATION_STATE.RIGHT_ONLY) {

                // see if we have crossed the mid point of the wall
                if (entityPos[0] > midpoint) {
                    return false;
                }
            }
        }
        
        return true;
        
    }
}
