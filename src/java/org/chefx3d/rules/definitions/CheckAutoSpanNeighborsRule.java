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

import javax.vecmath.Vector3d;

import org.chefx3d.model.BasePositionableEntity;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.MoveSegmentCommand;
import org.chefx3d.model.MoveSegmentTransientCommand;
import org.chefx3d.model.MoveVertexCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.RemoveEntityCommand;
import org.chefx3d.model.RemoveSegmentCommand;
import org.chefx3d.model.RemoveVertexCommand;
import org.chefx3d.model.ScaleEntityCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.AutoSpanUtility;
import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
 * Looks for nearest neighbors for auto span objects and provides appropriate
 * response for particular case based on what is moving and the requirements of
 * the auto span object.
 *
 * @author Ben Yarger
 * @version $Revision: 1.71 $
 */
public class CheckAutoSpanNeighborsRule extends BaseRule  {

    /** Movement result in removal pop up */
//    private static final String POP_UP_MOVE_REMOVE =
//        "org.chefx3d.rules.definitions.CheckAutoSpanNeighborsRule.movementRemoveAutoSpan";

    /** Scale result in removal pop up */
//    private static final String POP_UP_SCALE_REMOVE =
//        "org.chefx3d.rules.definitions.CheckAutoSpanNeighborsRule.scaleRemoveAutoSpan";

    /** Delete result in removal pop up */
    private static final String POP_UP_DELETE_REMOVE =
        "org.chefx3d.rules.definitions.CheckAutoSpanNeighborsRule.deleteRemoveAutoSpan";

    /** Wall move result in removal pop up */
//    private static final String POP_UP_WALL_MOVE_REMOVE =
//        "org.chefx3d.rules.definitions.CheckAutoSpanNeighborsRule.wallMoveRemoveAutoSpan";

    /** List of auto-spans found attached to the moving item */
    private ArrayList<Entity> autoSpanEntities;

    /** List of move commands that have been added to the stack so far */
    private ArrayList<Command> moveCommands;

    /** Set commands to ignore */
    private HashSet<String> ignoreRuleList;
    
    /** Set commands to ignore for remove commands generated here */
    private HashSet<String> ignoreRemoveRuleList;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public CheckAutoSpanNeighborsRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
        autoSpanEntities = new ArrayList<Entity>();
        moveCommands = new ArrayList<Command>();
        
        ignoreRuleList = new HashSet<String>();
        ignoreRuleList.add("org.chefx3d.rules.definitions.NudgeAutoAddRule");
        
        ignoreRemoveRuleList = new HashSet<String>();
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.CanDeleteRule");
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.ReparentHiddenProductsRule");
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.DeleteAutoAddRule");
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.CheckAutoSpanNeighborsRule");
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.DeleteCollisionsRule");
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.MiterCutRule");
        ignoreRemoveRuleList.add("org.chefx3d.rules.definitions.NudgeAutoAddRule");

    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

    /**
     * Perform the check
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

        // only non-transient commands should be evaluated
        if (command.isTransient()) {
            return result;
        }
        
        boolean good = true;

        // Not currently handling transient cases. Too hard a problem to
        // solve without specific reference to span points. Exception
        // is the MoveSegmentTransientCommand only because we need
        // to reposition span products when the left vertex moves since
        // these are not handled by the standard segment movement
        // response class.

        if (command instanceof MoveEntityCommand){

            good = movementCheck(entity, command, model);

        } else if (command instanceof TransitionEntityChildCommand){

            good = movementCheck(entity, command, model);

        } else if (command instanceof ScaleEntityCommand){

            //result = scaleCheck(entity, command, model);

        } else if (command instanceof RemoveEntityCommand) {

            good = removeCheck(entity, command, model);

        } else if (command instanceof RemoveEntityChildCommand){

            good = removeCheck(entity, command, model);

        } else if (command instanceof RemoveSegmentCommand){

            good = removeCheck(entity, command, model);

        } else if (command instanceof RemoveVertexCommand) {

            good = removeCheck(entity, command, model);

        } else if (command instanceof MoveSegmentCommand){

            good = segmentCheck(entity, command, model);

        } else if (command instanceof MoveSegmentTransientCommand){

            // Will correct auto span products for left vertex movement
            good = segmentCheck(entity, command, model);

        } else if (command instanceof MoveVertexCommand){

            good = vertexCheck(entity, command, model);

        }

        if (!good && !command.isTransient()) {
            result.setApproved(false);
        }

        result.setResult(good);
        return(result);
    }

    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

    /**
     * Perform movement command check
     *
     * @param entity Entity acted on
     * @param command Command acting on entity
     * @param model WorldModel
     * @return True if should continue, false otherwise
     */
    private boolean movementCheck(
            Entity entity,
            Command command,
            WorldModel model){

        if(!(entity instanceof PositionableEntity)){
            return true;
        }
		
        // Extract required data and change command values for collision
        // testing
        double[] startPos = new double[3];
        double[] endPos = new double[3];
        double[] autoSpanPos = new double[3];

        boolean is_move_cmd = false;
        // Get the start and end positions of the command
        if (command instanceof MoveEntityCommand){
						
            ((MoveEntityCommand)command).getStartPosition(startPos);
            ((MoveEntityCommand)command).getEndPosition(endPos);
						
			is_move_cmd = true;
			            
        } else if (command instanceof TransitionEntityChildCommand){
            
            // get the parent position relative to the zone
            double[] startParentRelPos =
                TransformUtils.getPositionRelativeToZone(
                        model,
                        ((TransitionEntityChildCommand)command).getStartParentEntity());
            
            // get the start position relative to the parent
            ((TransitionEntityChildCommand)command).getStartPosition(startPos);
            
            // aggregate positions
            startPos[0] += startParentRelPos[0];
            startPos[1] += startParentRelPos[1];
            startPos[2] += startParentRelPos[2];

            // get the parent position relative to the zone
            double[] endParentRelPos =
                TransformUtils.getPositionRelativeToZone(
                        model,
                        ((TransitionEntityChildCommand)command).getEndParentEntity());

            // get the previous frame position position relative to the parent
            ((TransitionEntityChildCommand)command).getEndPosition(endPos);

            // aggregate positions
            endPos[0] += endParentRelPos[0];
            endPos[1] += endParentRelPos[1];
            endPos[2] += endParentRelPos[2];

        } else {
            return true;
        }
        
        autoSpanEntities.clear();
        moveCommands.clear();

        // create a dummy command to represent the starting position of the
        // entity.  this will give us the collision list we started with
        Command tempCommand = createTempCommand(model, entity, command);
        if(tempCommand == null)
            return false;

        // Perform collision check
        rch.performCollisionCheck(tempCommand, false, true, false);

        //now remove the extra bounds data if this is not an auto-span item
        Boolean autoSpan = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
            entity,
            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        // find the amount of change
        double[] delta = new double[3];
        delta[0] = endPos[0] - startPos[0];
        delta[1] = endPos[1] - startPos[1];
        delta[2] = endPos[2] - startPos[2];
        // if nothing has changed then just stop processing
		if (!is_move_cmd) {
        	if (delta[0] == 0 && delta[1] == 0 && delta[2] == 0) {
            	return true;
        	}
		}
		
		// which way are we moving
		boolean posMovement = true;
		if (delta[0] < 0) {
			posMovement = false;
		}

        // Search collisions for any auto span entities
        if(rch.collisionEntities != null){

            for(int i = 0; i < rch.collisionEntities.size(); i++){

                Entity tmpEntity = rch.collisionEntities.get(i);

                autoSpan = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            tmpEntity,
                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                if(autoSpan != null && autoSpan == true){

                	if (!AutoSpanUtility.isAllowedAutoSpanCollision(tmpEntity, entity) && 
                	        !autoSpanEntities.contains(tmpEntity)) {
                		
                		((PositionableEntity)tmpEntity).getPosition(autoSpanPos);
                		if (posMovement) {
                			if (autoSpanPos[0] >= endPos[0]) {
                				autoSpanEntities.add(0, tmpEntity);
                			} else {
                				autoSpanEntities.add(tmpEntity);
                			}
                		} else {
                			if (autoSpanPos[0] <= endPos[0]) {
                				autoSpanEntities.add(0, tmpEntity);
                			} else {
                				autoSpanEntities.add(tmpEntity);
                			}
                		}                		
                	}                	
                }
            }
        }
        
        // recurse to check children
        checkChildren(model, command, entity, delta);
        
        // just keep going if no auto-spans
        if (autoSpanEntities == null || autoSpanEntities.size() <= 0) {
            return true;
        }
                
        // nudge all auto-spans found
        nudgeAutoSpans(delta[0], command.isTransient());

        return true;

    }
    
    /**
     *
     * @param model
     * @param command
     * @param entity
     */
    private void checkChildren(
            WorldModel model,
            Command command,
            Entity entity,
            double[] delta) {

        if (entity.hasChildren()) {

            int len = entity.getChildCount();
            for (int i = 0; i < len; i++) {

                Entity child = entity.getChildAt(i);
                if(!(child instanceof PositionableEntity)){
                    continue;
                }
                
                Boolean autoSpan = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                        ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                if (autoSpan) {
                    continue;
                }

                if (command instanceof MoveEntityCommand || 
                    command instanceof TransitionEntityChildCommand){
					
                    Entity startParent = 
                        SceneHierarchyUtility.getExactStartParent(model, child);
                    
                    Entity endParent = SceneHierarchyUtility.findZoneEntity(model, child);
                    
                    // get the position relative to the zone
                    double[] pos = 
                        TransformUtils.getPositionRelativeToActiveZone(
                                model, (PositionableEntity) child, true);
                   
                    if (pos == null) {
                    	continue;
                    }
                    
                    double[] startPos = new double[3];
                    startPos[0] = pos[0] - delta[0];
                    startPos[1] = pos[1] - delta[1];
                    startPos[2] = pos[2] - delta[2];
                    
                    float[] rot = new float[4];
                    ((PositionableEntity)child).getRotation(rot);
                                        
                    command = new TransitionEntityChildCommand(
                            model,
                            (PositionableEntity)child,
                            startParent, 
                            pos,
                            rot, 
                            endParent, 
                            startPos, 
                            rot, 
                            true);                     

                    // Perform collision check
					rch.performCollisionCheck(command, true, true, true);
                              
                    // Search collisions for any auto span entities
                    if(rch.collisionEntities != null){
						
                        for(int j = 0; j < rch.collisionEntities.size(); j++){

                            Entity tmpEntity = rch.collisionEntities.get(j);

                            autoSpan = 
                                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                                        tmpEntity,
                                        ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                            if (autoSpan) {
                                
                                Entity parent = 
                                    SceneHierarchyUtility.getExactParent(model, tmpEntity);

                                if (!AutoSpanUtility.isAllowedAutoSpanCollision(tmpEntity, entity) && 
                                        !autoSpanEntities.contains(tmpEntity) &&
                                        parent.isZone()) {
                                    autoSpanEntities.add(tmpEntity);
                                }
                            }
                        }
					}
                    
                    checkChildren(model, command, child, delta);

                }
            }
        }
    }

    /**
     * Move vertex command check
     *
     * @param entity Entity moved
     * @param command Command causing entity to move
     * @param model WorldModel
     * @return True if should continue, false otherwise
     */
    private boolean vertexCheck(
            Entity entity,
            Command command,
            WorldModel model){

        // -- Find all segments that use this vertex
        //
        // --Find all auto span products that attach to each segment using the vertex
        //
        // If the movement of the vertex is anything but perpendicular to the orientation of the span then remove the span

        VertexEntity movingVertex = null;
        SegmentableEntity segmentableEntity = null;
        double[] startingVertexPosition = new double[3];
        double[] endingVertexPosition = new double[3];
        double[] currentVertexPosition = new double[3];


        // Ignoring transient command for now as it is too difficult a
        // problem to solve
        if (command instanceof MoveVertexCommand){

            movingVertex = (VertexEntity) entity;

            int parentEntityID = movingVertex.getParentEntityID();
            segmentableEntity =
                (SegmentableEntity) model.getEntity(parentEntityID);

            ((MoveVertexCommand)command).getStartPosition(
                    startingVertexPosition);

            ((MoveVertexCommand)command).getEndPosition(
                    endingVertexPosition);

            movingVertex.getPosition(currentVertexPosition);

        } else {

            return true;
        }

        // Find all segments that use this vertex
        ArrayList<SegmentEntity> segmentList = segmentableEntity.getSegments();
        ArrayList<SegmentEntity> affectedSegmentsList =
            new ArrayList<SegmentEntity>();

        for(int i = 0; i < segmentList.size(); i++){

            SegmentEntity tmpSegment = segmentList.get(i);

            if(tmpSegment.getStartVertexEntity().getEntityID() ==
                movingVertex.getEntityID()){

                affectedSegmentsList.add(tmpSegment);

            } else if (tmpSegment.getEndVertexEntity().getEntityID() ==
                movingVertex.getEntityID()){

                affectedSegmentsList.add(tmpSegment);
            }
        }

        // Find auto span products colliding with the segments found to be
        // affected by the change of vertex position
        movingVertex.setPosition(startingVertexPosition, false);

        for(int i = 0; i < affectedSegmentsList.size(); i++){

            SegmentEntity tmpSegmentEntity = affectedSegmentsList.get(i);
            int transactionID = model.issueTransactionID();

            VertexEntity tmpStartVertex =
                tmpSegmentEntity.getStartVertexEntity();

            VertexEntity tmpEndVertex =
                tmpSegmentEntity.getEndVertexEntity();

            double[] startVertexStartPos = new double[3];
            double[] endVertexStartPos = new double[3];

            tmpStartVertex.getStartingPosition(startVertexStartPos);
            tmpEndVertex.getStartingPosition(endVertexStartPos);

            MoveSegmentCommand moveSegmentCmd =
                new MoveSegmentCommand(
                        model,
                        transactionID,
                        tmpSegmentEntity,
                        startVertexStartPos,
                        startVertexStartPos,
                        endVertexStartPos,
                        endVertexStartPos);


            // Perform collision check
            rch.performCollisionCheck(moveSegmentCmd, true, false, false);

            // Search collisions for any auto span entities
            ArrayList<Entity> autoSpanEntities = new ArrayList<Entity>();

            if(rch.collisionEntities != null){

                for(int j = 0; j < rch.collisionEntities.size(); j++){

                    Entity tmpEntity = rch.collisionEntities.get(j);

                    Boolean autoSpan = (Boolean)
                        RulePropertyAccessor.getRulePropertyValue(
                                tmpEntity,
                                ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                    if(autoSpan != null && autoSpan == true){

                    	if (!AutoSpanUtility.isAllowedAutoSpanCollision(tmpEntity, entity) && 
                                !autoSpanEntities.contains(tmpEntity)) {
                    		autoSpanEntities.add(tmpEntity);
                    	}
                    }
                }
            }
        }

        movingVertex.setPosition(currentVertexPosition, false);

        return true;
    }

    /**
     * Move segment command check
     *
     * @param entity Entity acted on
     * @param command Command acting on entity
     * @param model WorldModel
     * @return True if should continue, false otherwise
     */
    private boolean segmentCheck(
            Entity entity,
            Command command,
            WorldModel model){

        double[] startVertexStartPos = new double[3];
        double[] startVertexEndPos = new double[3];
        double[] endVertexStartPos = new double[3];
        double[] endVertexEndPos = new double[3];

        // Ignore transient cases as they are too difficult to solve currently
        if (command instanceof MoveSegmentCommand){

            ((MoveSegmentCommand)command).getStartVertexStartPosition(
                    startVertexStartPos);

            ((MoveSegmentCommand)command).getStartVertexEndPosition(
                    startVertexEndPos);

            ((MoveSegmentCommand)command).getEndVertexStartPosition(
                    endVertexStartPos);

            ((MoveSegmentCommand)command).getEndVertexEndPosition(
                    endVertexEndPos);

            // Set to starting positions for collision check
            ((MoveSegmentCommand)command).setStartVertexEndPosition(
                    startVertexStartPos);

            ((MoveSegmentCommand)command).setEndVertexEndPosition(
                    endVertexStartPos);

        } else {

            handleLeftSegmentTransientOffset(model, command, entity);
            return true;
        }
        
        // find the amount of change
        double[] delta = new double[3];
        delta[0] = startVertexEndPos[0] - startVertexStartPos[0];
        delta[1] = startVertexEndPos[1] - startVertexStartPos[1];
        delta[2] = startVertexEndPos[2] - startVertexStartPos[2];

        // if nothing has changed then just stop processing
        if (delta[0] == 0 && delta[1] == 0 && delta[2] == 0) {
            return true;
        }


        // Perform collision check
        rch.performCollisionCheck(command, true, false, false);

        // Reset to end positions originally set
        if (command instanceof MoveSegmentCommand){

            ((MoveSegmentCommand)command).setStartVertexEndPosition(
                    startVertexEndPos);

            ((MoveSegmentCommand)command).setEndVertexEndPosition(
                    endVertexEndPos);

        }

        // Handle repositioning case for left vertex movement now
        // that collision check has been performed.
        handleLeftSegmentTransientOffset(model, command, entity);

        // Search collisions for any auto span entities
        autoSpanEntities.clear();
        
        if(rch.collisionEntities != null){

            for(int i = 0; i < rch.collisionEntities.size(); i++){

                Entity tmpEntity = rch.collisionEntities.get(i);

                Boolean autoSpan = (Boolean)
                    RulePropertyAccessor.getRulePropertyValue(
                            tmpEntity,
                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                if(autoSpan != null && autoSpan == true){

                	if (!AutoSpanUtility.isAllowedAutoSpanCollision(tmpEntity, entity) && 
                            !autoSpanEntities.contains(tmpEntity)) {
                		autoSpanEntities.add(tmpEntity);
                	}
                }
            }
        }
        
        // nudge all auto-spans found
        nudgeAutoSpans(delta[0], command.isTransient());

        return true;
    }

    /**
     * Remove entity command check
     *
     * @param entity Entity acted on
     * @param command Command acting on entity
     * @param model WorldModel
     * @return True if should continue, false otherwise
     */
    private boolean removeCheck(
            Entity entity,
            Command command,
            WorldModel model){

        // clear the list of auto-spans
        autoSpanEntities.clear();

        // Search collisions for any auto span entities
        getAutoSpanToBeRemoved(entity, command);

        // Handle the case where there are collisions with other auto span 
        // entities.
        if(autoSpanEntities.size() > 0){

            String msg = intl_mgr.getString(POP_UP_DELETE_REMOVE);

            if(popUpConfirm.showMessage(msg)){

                for(int i = 0; i < autoSpanEntities.size(); i++){

                    Entity autoSpanEntity = autoSpanEntities.get(i);
                    issueRemoveCommand(model, autoSpanEntity);
                    
                }

            } else {

                return false;
            }
        }

        return true;
    }
    
    /**
     * Get the list of auto-spans that would be removed if the mount point is 
     * removed.
     * 
     * @param entity The entity being removed
     * @param command The command issuing the remove
     */
    private void getAutoSpanToBeRemoved(
            Entity entity,
            Command command) {
        
        // If shadow entity, don't bother with this check.
        Boolean isShadow =
            (Boolean)entity.getProperty(
                    entity.getParamSheetName(),
                    Entity.SHADOW_ENTITY_FLAG);

        if (isShadow != null && isShadow == true) {
            return;
        }
        
        // ignore auto-span items
        Boolean autoSpan = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);
        if (autoSpan) {
            return;
        }
        
        // Check if the entity is swapping to do special case check
        boolean isEntitySwapping = SceneHierarchyUtility.isSwapping(entity);

        // Perform collision check
        rch.performCollisionCheck(command, true, true, false);
        
        // Make a copy of the collisions so we don't lose them by doing more
        // collision checks.
        ArrayList<Entity> collisionSet = 
        	new ArrayList<Entity>(rch.collisionEntities);
        
        if(collisionSet != null){

            for(int i = 0; i < collisionSet.size(); i++){

                Entity autoSpanEntity = collisionSet.get(i);

                autoSpan = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            autoSpanEntity,
                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                if (autoSpan) {
                    if (!AutoSpanUtility.isAllowedAutoSpanCollision(
                    		autoSpanEntity, entity)) {
                    	
                    	// get the current position of the auto-span
                    	double[] pos = 
                    		TransformUtils.getExactPosition((
                    				PositionableEntity) autoSpanEntity);
                    	
                        // Check to make sure the auto span in question is using
                    	// the item as a mount point. Do this by checking to see
                    	// if the nearest neighbor check in either direction 
                    	// comes back as the item.
                        Entity[] neighbors = new Entity[2];
                        boolean hasNeighbors = 
                        	AutoSpanUtility.getNearestNeighborEntities(
                                    model,
                                    rch, 
                                    (PositionableEntity)autoSpanEntity,
                                    pos,
                                    neighbors);
                        
                        // If the entity is swapping, see if the swap target
                        // will satisfy the mount point requirement.
                        // Do this by performing a collision check with the
                        // auto span and for each collision found check if it
                        // is the swap target of the entity. If so, assume
                        // we are satisfying the mount point requirement.
                        if (isEntitySwapping) {
                        	
                        	Command dummyCmd = 
                        		rch.createCollisionDummyCommand(
                        				model, 
                        				(PositionableEntity) autoSpanEntity, 
                        				true, 
                        				true);
                        	
                        	rch.performCollisionCheck(
                        			dummyCmd, true, false, false);
                        	
                        	boolean matchMade = false;
                        	
                        	for (Entity e : rch.collisionEntities) {
                        		
                        		if (SceneHierarchyUtility.isSwapping(e) &&
                        				SceneHierarchyUtility.isSwapTarget(
                        						model, entity, e, false)) {
                        			
                        			matchMade = true;
                        			break;
                        		}
                        	}
                        	
                        	if (matchMade) {
                        		continue;
                        	}
                        }

                        if (hasNeighbors &&
                        		neighbors[0] != null && 
                        		neighbors[1] != null &&
                        		(neighbors[0].equals(entity) || 
                        				neighbors[1].equals(entity))) {
                        	
                        	autoSpanEntities.add(autoSpanEntity);
                        }
                    }
                }
            }
        }
    
        // recurse to check children
        if (entity.hasChildren()) {

            int len = entity.getChildCount();
            for (int i = 0; i < len; i++) {

                Entity child = entity.getChildAt(i);
                if (command instanceof RemoveEntityChildCommand){

                    command = new RemoveEntityChildCommand(
                            model, entity, child, false);

                    getAutoSpanToBeRemoved(child, command);

                }
            }
        }

    }

    /**
     * Apply appropriate offset to keep segment entities in place on wall when
     * left vertex moves.
     *
     * @param model WorldModel to change
     * @param command Command issued
     * @param entity Entity affected by command
     */
    private void handleLeftSegmentTransientOffset(
            WorldModel model,
            Command command,
            Entity entity){

        if(entity instanceof SegmentEntity){

            SegmentEntity movingSegmentEntity =
                (SegmentEntity)entity;

            // Establish the start and end vertices
            VertexEntity startVertex =
                movingSegmentEntity.getStartVertexEntity();

            VertexEntity endVertex =
                movingSegmentEntity.getEndVertexEntity();

            // Extract the segment list
            int segmentableEntityID =
                movingSegmentEntity.getParentEntityID();

            SegmentableEntity segmentableEntity =
                (SegmentableEntity)
                model.getEntity(segmentableEntityID);

            ArrayList<SegmentEntity> segmentList =
                segmentableEntity.getSegments();

            // Look for left vertex movement case on segmentList entities
            for(int i = 0; i < segmentList.size(); i++){

                SegmentEntity tmpSegmentEntity = segmentList.get(i);

                int startVertexEntityID =
                    tmpSegmentEntity.getStartVertexEntity().getEntityID();

                boolean isStartVertex = false;
                double[] leftVertexStartPos = new double[3];
                double[] leftVertexEndPos = new double[3];

                if (startVertex.getEntityID() == startVertexEntityID){

                    if(command instanceof MoveSegmentCommand){

                        ((MoveSegmentCommand)
                                command).getStartVertexStartPosition(
                                        leftVertexStartPos);

                        ((MoveSegmentCommand)
                                command).getStartVertexEndPosition(
                                        leftVertexEndPos);

                    } else if (command instanceof MoveSegmentTransientCommand){

                        ((MoveSegmentTransientCommand)
                                command).getStartVertexStartPosition(
                                        leftVertexStartPos);

                        ((MoveSegmentTransientCommand)
                                command).getStartVertexEndPosition(
                                        leftVertexEndPos);

                    } else {
                        return;
                    }

                    isStartVertex = true;

                } else if (endVertex.getEntityID() == startVertexEntityID){

                    if(command instanceof MoveSegmentCommand){

                        ((MoveSegmentCommand)
                                command).getEndVertexStartPosition(
                                        leftVertexStartPos);

                        ((MoveSegmentCommand)
                                command).getEndVertexEndPosition(
                                        leftVertexEndPos);

                    } else if (command instanceof MoveSegmentTransientCommand){

                        ((MoveSegmentTransientCommand)
                                command).getEndVertexStartPosition(
                                        leftVertexStartPos);

                        ((MoveSegmentTransientCommand)
                                command).getEndVertexEndPosition(
                                        leftVertexEndPos);

                    } else {
                        return;
                    }

                    isStartVertex = true;
                }

                if(isStartVertex){

                    double[] rightVertexPos = new double[3];
                    tmpSegmentEntity.getEndVertexEntity().getPosition(
                            rightVertexPos);

                    // Determine last good full wall distance (x axis
                    // difference) and subtract the x axis transform of
                    // the entity to determine the offset from the fixed
                    // right vector.
                    Vector3d wallVector = new Vector3d(
                            leftVertexStartPos[0] - rightVertexPos[0],
                            leftVertexStartPos[1] - rightVertexPos[1],
                            0.0);

                    Vector3d wallAdjustedVector = new Vector3d(
                            leftVertexEndPos[0] - rightVertexPos[0],
                            leftVertexEndPos[1] - rightVertexPos[1],
                            0.0);

                    double originalWallLength = wallVector.length();
                    double newWallLength = wallAdjustedVector.length();

                    ArrayList<Entity> segmentEntityList =
                        tmpSegmentEntity.getChildren();

                    for(int j = 0; j < segmentEntityList.size(); j++){

                        Entity tmpEntity = segmentEntityList.get(j);

                        Boolean autoSpan = (Boolean)
                            RulePropertyAccessor.getRulePropertyValue(
                                    tmpEntity,
                                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);

                        if(autoSpan == null || autoSpan == false){
                            continue;
                        }

                        if(command.isTransient()){

                            if(tmpEntity instanceof BasePositionableEntity){

                                double[] pos = new double[3];
                                ((BasePositionableEntity)
                                        tmpEntity).getStartingPosition(pos);

                                double rightHandOffset =
                                    originalWallLength - pos[0];

                                double xAxisTransform =
                                    newWallLength - rightHandOffset;

                                pos[0] = xAxisTransform;

                                ((BasePositionableEntity)tmpEntity).setPosition(
                                        pos,
                                        command.isTransient());

                            }

                        } else {

                            if(tmpEntity instanceof BasePositionableEntity){

                                double[] startPos = new double[3];
                                double[] endPos = new double[3];

                                ((BasePositionableEntity)
                                        tmpEntity).getStartingPosition(
                                                startPos);

                                ((BasePositionableEntity)
                                        tmpEntity).getStartingPosition(endPos);

                                double rightHandOffset =
                                    originalWallLength - startPos[0];

                                double xAxisTransform =
                                    newWallLength - rightHandOffset;

                                endPos[0] = xAxisTransform;


                                int transactionID = model.issueTransactionID();

                                MoveEntityCommand mvCmd =
                                    new MoveEntityCommand(
                                            model,
                                            transactionID,
                                            (PositionableEntity)tmpEntity,
                                            endPos,
                                            startPos);

                                addNewlyIssuedCommand(mvCmd);
                            }
                        }
                    }
                }
            }

        } else {
            return;
        }
    }

    /**
     * Generates the remove command.
     *
     * @param model WorldModel
     * @param entity Entity to remove
     */
    private void issueRemoveCommand(WorldModel model, Entity entity){

        Entity parentEntity = model.getEntity(entity.getParentEntityID());

        RemoveEntityChildCommand rmvCmd =
            new RemoveEntityChildCommand(
                    model,
                    parentEntity,
                    entity,
                    true);
        
  
        rmvCmd.setIgnoreRuleList(ignoreRemoveRuleList);
        
        addNewlyIssuedCommand(rmvCmd);
    }

    /**
     * Creates a temp command that is used to determine the starting collisions
     * All commands are created with the start pos, start scale and start rot,
     * depending on the command.
     *
     * None of these commands are ever actually fired
     *
     * @param model
     * @param entity
     * @param command
     * @return The command generated
     */
    private Command createTempCommand(
            WorldModel model,
            Entity entity,
            Command command) {

        double[] pos = new double[3];
        float[] scale = new float[3];
        float[] size = new float[3];
        float[] rot = new float[4];

        // ignore if not a positional entity
        if(!(entity instanceof PositionableEntity)) {
            return null;
        }
        
        // get the default values
        ((PositionableEntity)entity).getPosition(pos);
        ((PositionableEntity)entity).getScale(scale);
        ((PositionableEntity)entity).getSize(size);
        ((PositionableEntity)entity).getRotation(rot);
        
        Command returnCmd = null;
        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand)command).getStartPosition(pos);

        } else if (command instanceof ScaleEntityCommand) {

            ((ScaleEntityCommand)command).getOldPosition(pos);
            ((ScaleEntityCommand)command).getOldScale(scale);

        } else if (command instanceof TransitionEntityChildCommand) {

            // get the parent position relative to the zone
            double[] startParentRelPos =
                TransformUtils.getPositionRelativeToZone(
                        model,
                        ((TransitionEntityChildCommand)command).getStartParentEntity());
            
            // get the start position relative to the parent
            ((TransitionEntityChildCommand)command).getStartPosition(pos);
            
            // aggregate positions
            pos[0] += startParentRelPos[0];
            pos[1] += startParentRelPos[1];
            pos[2] += startParentRelPos[2];

        }
        
        returnCmd = new MoveEntityCommand(
                model,
                command.getTransactionID(),
                (PositionableEntity)entity,
                pos,
                pos);
                
        return returnCmd;

    }
    
    /**
     * 
     * @param delta
     * @param isTransient
     */
    private void nudgeAutoSpans(double delta, boolean isTransient) {
        
        for(int i = 0; i < autoSpanEntities.size(); i++){
            // get the span entity
            PositionableEntity spanEntity = 
                (PositionableEntity)autoSpanEntities.get(i);
            
            // Get the position
            double[] position = 
                TransformUtils.getPosition(spanEntity);
            
            // adjust it by the delta change of the mount item
            position[0] += delta * 0.5f;
    
            // perform the nudge
            SceneManagementUtility.nudgeEntity(
                    model, 
                    collisionChecker, 
                    spanEntity,
                    position, 
                    isTransient);
        }
        
    }

}
