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
import java.util.HashSet;

import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.ScaleEntityCommand;
import org.chefx3d.model.ScaleEntityTransientCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.AutoSpanUtility;
import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;
import org.chefx3d.view.common.EditorView;

/**
 * Attempt to span entity between nearest neighbors. Applies to add and move
 * commands.
 *
 * @author Ben Yarger
 * @version $Revision: 1.87 $
 */
public class AutoSpanRule extends BaseRule  {

    /** This product cannot fit in space status message */
    private static final String STATUS_MSG_SPACE_TOO_SMALL =
        "org.chefx3d.rules.definitions.AutoSpanRule.statusMsgMinSpanLmt";

    /** This product cannot span space status message */
    private static final String STATUS_MSG_SPACE_TOO_LARGE =
        "org.chefx3d.rules.definitions.AutoSpanRule.statusMsgMaxSpanLmt";

    /** This product cannot be spanned in it's current position */
    private static final String STATUS_MSG_INVALID_POS =
        "org.chefx3d.rules.definitions.AutoSpanRule.invalidPosition";

    /** This product cannot fit in space pop up message */
    private static final String POP_UP_MSG_SPACE_TOO_SMALL =
        "org.chefx3d.rules.definitions.AutoSpanRule.popUpMsgMinSpanLmt";

    /** This product cannot span space pop up message */
    private static final String POP_UP_MSG_SPACE_TOO_LARGE =
        "org.chefx3d.rules.definitions.AutoSpanRule.popUpMsgMaxSpanLmt";

    /** This product cannot span space pop up message */
    private static final String POP_UP_MSG_INVALID_WALL_ANGLE=
        "org.chefx3d.rules.definitions.AutoSpanRule.invalidWallAngle";

    /** The negative x neighbor */
    private Entity negXNeighbor;

    /** The positive x neighbor */
    private Entity posXNeighbor;

    /** Set commands to ignore */
    private HashSet<String> ignoreRuleList;
    
    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public AutoSpanRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
        
        ignoreRuleList = new HashSet<String>();
        ignoreRuleList.add("org.chefx3d.rules.definitions.SegmentBoundsCheckRule");
        ignoreRuleList.add("org.chefx3d.rules.definitions.NudgeAutoAddRule");

    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

    /**
     * Perform the check
     *
     * @param entity Entity object
     * @param model WorldModel object
     * @param command Command object
     * @param result The state of the rule processing
     * @return boolean True if rule passes, false otherwise
     */
    protected RuleEvaluationResult performCheck(
            Entity entity,
            Command command,
            RuleEvaluationResult result) {

        this.result = result;

        Boolean autoSpan = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        if(autoSpan == null || autoSpan == false){
            result.setResult(true);
            return(result);
        }
        float[] size = new float[3];
        double[] pos = new double[3];
        float[] scale = new float[3];

        /*
         * If not a positionable entity, don't bother with rule execution
         */
        if(entity instanceof PositionableEntity){

            ((PositionableEntity)entity).getSize(size);
            ((PositionableEntity)entity).getScale(scale);

        } else {
            result.setResult(true);
            return(result);
        }

        /*
         * Extract position, if unable to based on command type don't bother
         * with rule execution
         */
        if(command instanceof AddEntityChildCommand){

            ((PositionableEntity)entity).getPosition(pos);

        } else if (command instanceof MoveEntityCommand){

            ((MoveEntityCommand)command).getEndPosition(pos);

        } else if (command instanceof MoveEntityTransientCommand){

            ((MoveEntityTransientCommand)command).getPosition(pos);

        } else if (!command.isTransient() && command instanceof TransitionEntityChildCommand){
            
            ((TransitionEntityChildCommand)command).getEndPosition(pos);

        } else {
            result.setResult(true);
            return(result);
        }
                
        // find the parent zone of the entity  
        Entity startParent = 
            SceneHierarchyUtility.getExactParent(model, entity);
      
        Entity endParent = 
            SceneHierarchyUtility.getWallOrFloorParent(model, entity);
        
        if (startParent != endParent) {
            // Convert the position to local coordinates relative to 
            // the new parent.
            pos = TransformUtils.convertToCoordinateSystem(
                    model, pos, (PositionableEntity)startParent, (PositionableEntity)endParent, true);
        }
 
        Float zOffset = (Float)
        RulePropertyAccessor.getRulePropertyValue(
            entity,
            ChefX3DRuleProperties.SPAN_OBJECT_DEPTH_OFFSET_PROP);

        pos[2] = zOffset;

        /*
         * Calculate the horizontal span
         */
        boolean spanGood =
            calculateXSpan(
                pos,
                size,
                scale,
                model,
                entity,
                command, 
                true);
        
        if (!spanGood && !command.isTransient()) {
            
            result.setNotApprovedAction(
                    NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);               
            result.setApproved(false);
 
        }

        result.setResult(spanGood);
        return(result);

    }

    /**
     * Calculate the horizontal span adjustment that should occur.
     *
     * @param pos Current entity position
     * @param size Current entity size
     * @param scale Current entity scale
     * @param model WorldModel
     * @param entity Entity being acted on
     * @param command Command causing the action
     * @return True if command should continue, false otherwise
     */
    private boolean calculateXSpan(
            double[] pos,
            float[] size,
            float[] scale,
            WorldModel model,
            Entity entity,
            Command command, 
            boolean passed){
        
        // get the model size, scale, and pos data
        double[] origPos = new double[3];
        ((PositionableEntity)entity).getPosition(origPos);

        float[] origScale = new float[3];
        ((PositionableEntity)entity).getScale(origScale);

        float[] origSize = new float[3];
        ((PositionableEntity)entity).getSize(origSize);

        negXNeighbor = null;
        posXNeighbor = null;
        
        Entity entityParentZone =
            SceneHierarchyUtility.findZoneEntity(model, entity);
        if (entityParentZone == null) {
            entityParentZone = 
                model.getEntity(view.getActiveLocationEntity().getActiveZoneID());
        }

        int tmpID = entity.getParentEntityID();
        entity.setParentEntityID(entityParentZone.getEntityID());
        
        // check to make sure we have a neighbor to each side
        Entity[] neighbors = new Entity[2];
        boolean hasNeighbors = 
        	AutoSpanUtility.getNearestNeighborEntities(
                    model,
                    rch, 
                    (PositionableEntity)entity,
                    pos,
                    neighbors);
        negXNeighbor = neighbors[0];
        posXNeighbor = neighbors[1];

        // revert parent
        entity.setParentEntityID(tmpID);
        
        if (!hasNeighbors) {               
            String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);
            if(command.isTransient()){     
            	result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                statusBar.setMessage(msg);                 
            } else {                
                // issue a transient scale command to ensure it has been 
                // reset correctly
                resetToStartingScale((PositionableEntity)entity);                
                popUpMessage.showMessage(msg);               
            }
            return false;            
        }

        // check the wall angles
        boolean validAngle =
            checkValidAngles(
                    model,
                    command,
                    entity,
                    entityParentZone);

        if (!validAngle) {
            String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);
            if(command.isTransient()){   
            	result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                statusBar.setMessage(msg);                 
            } else {                
                // issue a transient scale command to ensure it has been 
                // reset correctly
                resetToStartingScale((PositionableEntity)entity);                
                popUpMessage.showMessage(msg);               
            }
            return false;            
        }

        // get the negative neighbor data
        double negXSide =
            getNeighborAdjustment(
                    model,
                    entityParentZone,
                    negXNeighbor,
                    false);

        // get the positive neighbor data
        double posXSide =
            getNeighborAdjustment(
                    model,
                    entityParentZone,
                    posXNeighbor,
                    true);

        // calculate the span and validate it
        double span = posXSide - negXSide;

        boolean validSpan = 
            validateSpan(
                    model,
                    command,
                    (PositionableEntity)entity,
                    pos, 
                    origPos,
                    origScale,
                    span);

        if (!validSpan) {
            // scale reset and messaging already handled in validateSpan method
            return false;
        }

        // calculate the new scale
        scale[0] = (float) (span / size[0]);

        // Calculate the new position. Base this on the left side found                        
        pos[0] = negXSide + span * 0.5;

        return handleCommand(model, command, entity, pos, scale, span, passed);

    }

    /**
     * Handle command responses to span calculation
     *
     * @param model WorldModel
     * @param command Issued command
     * @param entity Entity acted on by command
     * @param pos New entity position
     * @param scale New entity scale
     * @return false is new command issued, true is execution should continue
     */
    private boolean handleCommand(
            WorldModel model,
            Command command,
            Entity entity,
            double[] pos,
            float[] scale,
            double span, 
            boolean passed){

        // need to add this to the current command's ignore list
        HashSet<String> currentList = command.getIgnoreRuleList();
        if (currentList == null) {
            currentList = new HashSet<String>();
        }
        currentList.addAll(ignoreRuleList);
        command.setIgnoreRuleList(currentList);
        
        // add some extra bounds to ensure collision with mount points
        float[] size = new float[3];
        ((PositionableEntity)entity).getSize(size);
        
        float[] extendedBorder = 
        	new float[] {BoundsUtils.SPAN_OVERLAP_THRESHOLD, 0, 0};
                                  
        RulePropertyAccessor.setRuleProperty(
                entity, 
                PositionableEntity.BOUNDS_BORDER_PROP, 
                extendedBorder);
        
        // get the list of possible parents
        ArrayList<Entity> parentList = new ArrayList<Entity>();
        parentList.add(negXNeighbor);
        parentList.add(posXNeighbor);
        //    SceneHierarchyUtility.findPossibleParents(command, model, rch);

        // get the current parent           
        Entity startParent = 
            SceneHierarchyUtility.getWallOrFloorParent(model, entity);
        Entity endParent = startParent;
           
        // only continue if necessary
        if (!command.isTransient() && 
        		parentList != null && 
        		parentList.size() > 1) {
                        
            // get the new parent
            endParent = 
                SceneHierarchyUtility.getSharedParent(
                		model, entity, parentList);
               
            // no shared parent, just use the current zone
            if (endParent == null) {
                endParent = startParent;
            }
            
            if (startParent != endParent) {
                // Convert the position to local coordinates relative to 
                // the new parent.
                pos = TransformUtils.convertToCoordinateSystem(
                        model, 
                        pos, 
                        (PositionableEntity)startParent, 
                        (PositionableEntity)endParent, 
                        true);
            }           
                                               
        }
        
        // Check if we need to examine any scale change model cases.
        // Look for entity flag to do so.
        Boolean scaleChangeModelFlag =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);


        float[] startScale = new float[3];
        ((PositionableEntity)entity).getStartingScale(startScale);
        
        double[] startPos = new double[3];
        ((PositionableEntity)entity).getStartingPosition(startPos);

        float[] currentScale = new float[3];
        ((PositionableEntity)entity).getScale(currentScale);        
       
        // Handle results accordingly. If the command is a scale command
        // and the change in scale is the same as what is already set, just 
        // return true
        if (command instanceof MoveEntityCommand){
            
            // make sure the position is centered
            ((MoveEntityCommand)command).setPosition(pos);
                   
            // --- Remove any children of auto spans
            ArrayList<Entity> childrenToRemove = entity.getChildren();

            for (Entity child : childrenToRemove) {

                Boolean isAutoAdd =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

                // don't remove auto-added products
                if (isAutoAdd)
                    continue;
                
                Boolean isComplexSubPart =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

                // don't remove complex product sub parts but scale them
                if (isComplexSubPart) {
                	continue;
                }
                    
                SceneManagementUtility.removeChild(
                		model, collisionChecker, child, true);
            }
            
            int transID = SceneManagementUtility.scaleEntity(
                    model, 
                    collisionChecker, 
                    (PositionableEntity) entity, 
                    pos, 
                    scale, 
                    false, 
                    false);

            ScaleEntityCommand cmd = 
                (ScaleEntityCommand)CommandSequencer.getInstance().getCommand(transID);        
            cmd.setIgnoreRuleList(ignoreRuleList);
            
            // Make sure the current move command gets terminated and that 
            // we instead operate on just the scale command
            this.result.setApproved(false);
            this.result.setNotApprovedAction(
            		NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);

            return true;
            
        } else if (command instanceof MoveEntityTransientCommand) {

            // make sure the position is centered
            ((MoveEntityTransientCommand)command).setPosition(pos);
                                      
            // --- Remove any children of auto spans
            ArrayList<Entity> childrenToRemove = entity.getChildren();

            // define some working variables
            double[] childPos = new double[3];
            float[] childSize = new float[3];
            float[] childScale = new float[3];

            int intScale = Math.round(scale[0] * 1000);
            int intCurrentScale = Math.round(currentScale[0] * 1000);

            for (Entity child : childrenToRemove) {

                Boolean isAutoAdd =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

                // don't remove auto-added products
                if (isAutoAdd)
                    continue;
     
                Boolean isComplexSubPart =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

                
                // don't remove complex product sub parts but scale them
                if (isComplexSubPart) {
                	
                    if (intScale >= intCurrentScale - BoundsUtils.SPAN_OVERLAP_THRESHOLD * 1000 && 
                    		intScale <= intCurrentScale + BoundsUtils.SPAN_OVERLAP_THRESHOLD * 1000) {
                        // we are done, the change in scale is within the tolerance
                    	continue;
                    }
                	
                	// cast to a PositionableEntity for easy use
                	PositionableEntity pChild = (PositionableEntity)child;
                	
                	// get the position                	
                	pChild.getPosition(childPos);
 
                	// get the size
                	pChild.getSize(childSize);
                	
                	// calculate the scale required to fit the span
                	pChild.getScale(childScale);                	
                	float newScale = (float)span / childSize[0];
                	
                	// only scale if it a marginal amount, otherwise visually
                	// it looks bad
                	if (newScale <= childScale[0] * 2 && 
                			newScale >= childScale[0] * 0.5) {
                		
                    	childScale[0] = newScale;

                    	// execute a transient scale command
                    	SceneManagementUtility.scaleEntity(
                                model, 
                                collisionChecker, 
                                pChild, 
                                childPos, 
                                childScale, 
                                true, 
                                true);
                	}

                	continue;
                }

                SceneManagementUtility.removeChild(
                		model, collisionChecker, child, true);
            }
       
            int transID = SceneManagementUtility.scaleEntity(
                    model, 
                    collisionChecker, 
                    (PositionableEntity) entity, 
                    pos, 
                    scale, 
                    true, 
                    false);
            
            ScaleEntityTransientCommand cmd = 
                (ScaleEntityTransientCommand)CommandSequencer.getInstance().getCommand(transID);           
            cmd.setIgnoreRuleList(ignoreRuleList);

            // Make sure the current move command gets terminated and that 
            // we instead operate on just the scale command
            this.result.setApproved(false);
            this.result.setNotApprovedAction(
            		NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);

            return true;

        } else if (!command.isTransient() && 
        		command instanceof TransitionEntityChildCommand){

            // make sure the data is correctly set up
            ((TransitionEntityChildCommand)command).setEndPosition(pos);
            ((TransitionEntityChildCommand)command).setEndScale(scale);

            ((TransitionEntityChildCommand)command).setStartPosition(startPos);
            ((TransitionEntityChildCommand)command).setStartScale(startScale);

            ((TransitionEntityChildCommand)command).setStartParentEntity(startParent);
            ((TransitionEntityChildCommand)command).setEndParentEntity(endParent);          
            
            int intScale = Math.round(scale[0] * 1000);
            int intStartScale = Math.round(startScale[0] * 1000);
            
            if (intScale == intStartScale) {
            	// we are done
            } else if (scaleChangeModelFlag) {
                    
                // --- Remove any children of auto spans
                ArrayList<Entity> childrenToRemove = entity.getChildren();
    
                for (Entity childToRemove : childrenToRemove) {
    
                    Boolean isAutoAdd =
                        (Boolean)RulePropertyAccessor.getRulePropertyValue(
                                childToRemove,
                                ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
    
                    // don't remove auto-added products
                    if (isAutoAdd)
                        continue;
                    
                    Boolean isComplexSubPart =
                        (Boolean)RulePropertyAccessor.getRulePropertyValue(
                                childToRemove,
                                ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

                    // don't remove complex product sub parts
                    if (isComplexSubPart)
                        continue;
                   
                    SceneManagementUtility.removeChild(
                    		model, collisionChecker, childToRemove, true);
                }
                
                int transID = SceneManagementUtility.scaleEntity(
                		model, 
                		collisionChecker, 
                		(PositionableEntity) entity, 
                		pos, 
                		scale, 
                		!passed, 
                		false);
                
                
            	ScaleEntityCommand cmd = (ScaleEntityCommand) 
            		CommandSequencer.getInstance().getCommand(transID);
                cmd.setIgnoreRuleList(ignoreRuleList);
              
                // Make sure the current move command gets terminated and that 
                // we instead operate on just the scale command
                this.result.setApproved(false);
                this.result.setNotApprovedAction(
                        NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);

            }            

            return true;

        } else if(command instanceof AddEntityChildCommand){

            if (passed) {
            	
            	int intScale = Math.round(scale[0] * 1000);
            	int intCurrentScale = Math.round(currentScale[0] * 1000);

            	// decide if we need to issue the scale command.  if the start
            	// scale is greater than 0 we always want to scale.  if the
            	// start scale is 0 then we only want to scale if the 
            	// current scale and calculated scale are different by more 
            	// than the span threshold.
            	boolean issueScale = false;
            	if (startScale[0] > 0) {
            		issueScale = true;
            	} else if (startScale[0] == 0 && 
            			!(intScale >= intCurrentScale - BoundsUtils.SPAN_OVERLAP_THRESHOLD * 1000 && 
                  		intScale <= intCurrentScale + BoundsUtils.SPAN_OVERLAP_THRESHOLD * 1000)) {
            		issueScale = true;
            	}
                                
                // need to actually issue the scale command
                startScale[0] = scale[0];
                startScale[1] = scale[1];
                startScale[2] = scale[2];

                // set the position
                startPos[0] = pos[0];
                startPos[1] = pos[1];
                startPos[2] = pos[2];

                // make sure to update the entity to the scale
                ((PositionableEntity)entity).setStartingPosition(pos);
                ((PositionableEntity)entity).setPosition(pos, command.isTransient());
                ((PositionableEntity)entity).setStartingScale(scale);
                ((PositionableEntity)entity).setScale(scale);
                
                ((AddEntityChildCommand)command).setParentEntity(endParent);
                
                // if the entity can model swap and a scale change has occurred 
                // then issue a new command to be executed after the add                
                if (issueScale && scaleChangeModelFlag) {
                    
                    // issue a scale command to make sure the auto-swaps can happen
                    int transID = SceneManagementUtility.scaleEntity(
                    		model, 
                    		collisionChecker, 
                    		(PositionableEntity) entity, 
                    		pos, 
                    		scale, 
                    		false, 
                    		false);
                    
                	ScaleEntityCommand cmd = (ScaleEntityCommand) 
                		CommandSequencer.getInstance().getCommand(transID);
                	
                    cmd.setIgnoreRuleList(ignoreRuleList);
                }

                return true;

            } else {
                return false;
            }

        }

        return true;
    }

    /**
     * Check that the span is not negative and that the min and max size
     * is respected.
     *
     * @param model the world model
     * @param command the command being executed
     * @param entity the entity being effected
     * @param origPos the original position of the entity
     * @param origScale the original scale of the entity
     * @param span the span size
     * @return true is valid, false otherwise
     */
    private boolean validateSpan(
            WorldModel model,
            Command command,
            PositionableEntity entity,
            double[] currentPos, 
            double[] origPos,
            float[] origScale,
            double span) {

        // check to make sure it is not negative
        if (span <= 0) {
            String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);

            if(command.isTransient()){
            	result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                statusBar.setMessage(msg); 
            } else {
                popUpMessage.showMessage(msg);

            }
            return false;
        }

        // check the minimum sizes allowed
        float[] maximumSize = (float[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.MAXIMUM_OBJECT_SIZE_PROP);

        if (maximumSize != null) {
            
            int maxSize = Math.round(maximumSize[0] * 1000);
            int spanSize = (int)Math.round(span * 1000) - 1;

            if(spanSize > maxSize){

                if(command.isTransient()){
                    String msg = intl_mgr.getString(STATUS_MSG_SPACE_TOO_LARGE);
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    statusBar.setMessage(msg);                     
                } else {
					
                	// issue a transient scale command to ensure it has been 
                	// reset correctly
					resetToStartingScale(entity);
					
                    String msg = intl_mgr.getString(POP_UP_MSG_SPACE_TOO_LARGE);
                    popUpMessage.showMessage(msg);

                }               
                return false;
            }
        }

        // check the maximum sizes allowed
        float[] minimumSize = (float[])
        RulePropertyAccessor.getRulePropertyValue(
            entity,
            ChefX3DRuleProperties.MINIMUM_OBJECT_SIZE_PROP);

        if (minimumSize != null) {

            int minSize = Math.round(minimumSize[0] * 1000);
            int spanSize = (int)Math.round(span * 1000) + 1;

            if(spanSize < minSize){

                if(command.isTransient()){
                    String msg = intl_mgr.getString(STATUS_MSG_SPACE_TOO_SMALL);
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    statusBar.setMessage(msg);
                } else {
                	
                	// issue a transient scale command to ensure it has been 
                	// reset correctly
					resetToStartingScale(entity);
            	   
                    String msg = intl_mgr.getString(POP_UP_MSG_SPACE_TOO_SMALL);
                    popUpMessage.showMessage(msg);
                    
                }                
                return false;
            }
        }

        // check scale step size to ensure the final scale * size is valid
        
        // Check if we need to examine any scale change model cases.
        // Look for entity flag to do so.
        Boolean scaleAbsSnapFlag =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_USES_ABSOLUTE_SNAPS_PROP);
        
        if (scaleAbsSnapFlag) {
            
            // The rules scale values to determine when a model change needs to swap.
            // These are scale increments that denote a model change.
            float[] ruleScaleValues = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.SCALE_ABSOLUTE_X_AXIS_SNAP_PROP);
            
            boolean matched = false;
            for (int i = 0; i < ruleScaleValues.length; i++) {
                
                float check = ruleScaleValues[i];
                
                if (span >= check - BoundsUtils.SPAN_OVERLAP_THRESHOLD && 
                        span <= check + BoundsUtils.SPAN_OVERLAP_THRESHOLD) {
                    matched = true;  
                    break;
                }                
                
            }
            
            if (!matched) {
                if (command.isTransient()) {                 
                    String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    statusBar.setMessage(msg);                                      
                } else {
                	
                	// issue a transient scale command to ensure it has been 
                	// reset correctly
					resetToStartingScale(entity);
                    
                    String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);
                    popUpMessage.showMessage(msg);                    

                }
                return false;
            }

        }
        
        // Check if we need to examine any scale change model cases.
        // Look for entity flag to do so.
        Boolean scaleIncSnapFlag =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_USES_INCREMENTAL_SNAPS_PROP);
        
        if (scaleIncSnapFlag) {
            
            // The rules scale values to determine when a model change needs to swap.
            // These are scale increments that denote a model change.
            float ruleScaleValue = (Float)
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.SCALE_ABSOLUTE_X_AXIS_SNAP_PROP);
                        
            boolean matched = false;
            if (span % ruleScaleValue == 0) {
                matched = true;                        
            }
            
            if (!matched) {
                if (command.isTransient()) {                   
                    String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                    statusBar.setMessage(msg);                                      
                } else {
                	
                	// issue a transient scale command to ensure it has been 
                	// reset correctly
					resetToStartingScale(entity);
                    
                    String msg = intl_mgr.getString(STATUS_MSG_INVALID_POS);
                    popUpMessage.showMessage(msg);
                    
                }
                return false;
            }

        }

        return true;

    }
    
    /**
     * Get the positionable entity start pos and start scale and issue a 
     * transient scale command using those values.
     * 
     * @param entity The positionable entity to reset
     */
    private void resetToStartingScale(PositionableEntity entity) {
    	
    	float[] startingScale = new float[3];
		entity.getStartingScale(startingScale);
		
		float[] startingRot = new float[4];
		entity.getStartingRotation(startingRot);

    	double[] startingPos = TransformUtils.getExactStartPosition(entity);
		
		Entity startingParent = 
			SceneHierarchyUtility.getExactStartParent(model, entity);
		
		// We have to be fancy about this and trick the command so that when
		// it is reset it will resolve the parenting correctly. We generate
		// the command with the correct parenting, but then have to reverse it
		// after. So grab the transactionID and call up the command from the
		// CommandSequencer and manually reverse the values.
		
		int transactionID =
			SceneManagementUtility.moveEntityChangeParentAndScale(
				model, 
				collisionChecker, 
				entity, 
				startingParent, 
				startingPos,
				startingRot, 
				startingScale, 
				true, 
				true);
		
		if (transactionID == -1) {
			return;
		}
		
		Command tmpCmd = 
			CommandSequencer.getInstance().getCommand(transactionID);
		
		if (tmpCmd != null && 
				(tmpCmd instanceof TransitionEntityChildCommand)) {
			
			TransitionEntityChildCommand tranCmd = 
				(TransitionEntityChildCommand)tmpCmd;
			
			Entity endParent;
			double[] endPosition = new double[3];
			float[] endScale = new float[3];
			float[] endRotation = new float[4];
			
			endParent = tranCmd.getEndParentEntity();
			tranCmd.getEndPosition(endPosition);
			tranCmd.getEndScale(endScale);
			tranCmd.getCurrentRotation(endRotation);
			
			// Set all the values to the same desired end state
			tranCmd.setStartParentEntity(endParent);
			tranCmd.setStartPosition(endPosition);
			tranCmd.setStartingRotation(endRotation);
			tranCmd.setStartScale(endScale);
		}
    }
    
    /**
     * The the position and bounds of the neighbor.
     *
     * @param model the world model
     * @param entityParentZone the parent zone
     * @param neighborEntity the neighbor entity
     * @param isPositive true if in the positive axis, false if the negative axis.
     * @return the adjustment value based on the bounds found.
     */
    private double getNeighborAdjustment(
            WorldModel model,
            Entity entityParentZone,
            Entity neighborEntity,
            boolean isPositive) {

        boolean adjacent = false;
        double[] neighborPos = new double[3];
        float[] neighborBounds = new float[6];

        /*
         * Extract zone respective positions of nearest neighbors
         */
        if (neighborEntity.getType() == Entity.TYPE_SEGMENT) {
            
            if (entityParentZone.getType() == Entity.TYPE_SEGMENT) {                
                
                VertexEntity endVertex =
                    ((SegmentEntity)neighborEntity).getEndVertexEntity();

                if (isPositive) {
                    neighborPos[0] = ((SegmentEntity)entityParentZone).getLength(); 
                } else {
                    neighborPos[0] = 0;
                }
                
                neighborPos[1] = endVertex.getHeight();
                neighborPos[2] = 0.0;

            } else {
                
                float[] parentBounds = 
                    BoundsUtils.getBounds((PositionableEntity)entityParentZone, true);
                
                neighborPos[0] = parentBounds[1];
                neighborPos[1] = parentBounds[3];
                neighborPos[2] = 0;
                
            }  

            neighborBounds[0] = 0.0f;
            neighborBounds[1] = 0.0f;
            neighborBounds[2] = 0.0f;
            neighborBounds[3] = 0.0f;
            neighborBounds[4] = 0.0f;
            neighborBounds[5] = 0.0f;

        } else if (neighborEntity instanceof PositionableEntity) {

            Entity parentZone =
                SceneHierarchyUtility.findExactZoneEntity(model, neighborEntity);

            if (parentZone != entityParentZone) {
                adjacent = true;
            }

            neighborPos =
                TransformUtils.getExactRelativePosition(
                        model,
                        neighborEntity,
                        parentZone,
                        false);

            ((PositionableEntity)neighborEntity).getBounds(neighborBounds);

        }

        double adjustment = 0;
        if (isPositive) {

            //check to see if the positive neighbor is on an adjacent zone
            if (adjacent) {

                //checks to see if the zone is a segment
                //TODO: will need to case out other type of zones as we find them
                if(entityParentZone.getType() == Entity.TYPE_SEGMENT) {

                    // Assumes 90 degree right angle with adjacent wall
                    // Unlike the negAdjacent case, we have to subtract from the
                    // full length of the wall. All wall origins are at the bottom
                    // left corner, so we don't have to account for wall length
                    // in the negAdjacent case.
                    SegmentEntity tmpSegment = (SegmentEntity)entityParentZone;
                    
                    float length = tmpSegment.getLength();             
                    float neighborSize = (float)(neighborPos[2] + neighborBounds[5]);
                    adjustment = length - neighborSize;
/*
Keep this around as we may want it to handle non right angle
adjacent walls. At this time we aren't supporting it.

Matrix3d rotation = new Matrix3d();
rotation.setIdentity();

AxisAngle4d rotateAxis = new AxisAngle4d(0,1,0,angle);

rotation.set(rotateAxis);

Vector3d position = new Vector3d(posXNeighborPos[0],posXNeighborPos[1], posXNeighborPos[2]);
double length = ((SegmentEntity)entityParentZone).getLength();

rotation.transform(position);

int index = 4;
posXSide = (length - position.x)  + posXNeighborBounds[index];
*/
                }
            } else {
                adjustment = neighborPos[0] + neighborBounds[0];
            }

        } else {

            if (adjacent) {
                adjustment = neighborPos[2] + neighborBounds[5];
            } else {
                adjustment = neighborPos[0] + neighborBounds[1];
            }

        }
        
        return adjustment;

    }

    /**
     * Checks the angles of the target walls
     *
     * @param model the world model
     * @param command the command
     * @param entity the entity
     * @param entityParentZone the parent zone
     * @param negXNeighbor the negative neighbor entity
     * @param posXNeighbor the positive neighbor entity
     * @return true is angle is valid, false otherwise
     */
    private boolean checkValidAngles(
            WorldModel model,
            Command command,
            Entity entity,
            Entity entityParentZone) {

        boolean invalidAngle = false;
        int angle = 90;

        // Checks to determine if either wall is not a 90 degree
        // if it is not does not allow the autospan to occur
        if (entityParentZone instanceof SegmentEntity &&
                negXNeighbor.getType() == Entity.TYPE_SEGMENT &&
                negXNeighbor.getParentEntityID() != entity.getParentEntityID()) {

            invalidAngle =
                TransformUtils.isWallAtSpecificAngle(
                        model,
                        entity,
                        entityParentZone,
                        false,
                        angle);

        }

        if (posXNeighbor.getType() == Entity.TYPE_SEGMENT &&
                posXNeighbor.getParentEntityID() != entity.getParentEntityID() &&
                !invalidAngle) {

            invalidAngle =
                TransformUtils.isWallAtSpecificAngle(
                        model,
                        entity,
                        entityParentZone,
                        true,
                        angle);

        }

        if (invalidAngle) {
            return false;
        }

        return true;

    }
}
