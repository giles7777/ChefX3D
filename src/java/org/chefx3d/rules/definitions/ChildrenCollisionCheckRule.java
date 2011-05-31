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

//External imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if the command ( Move,Scale,Rotate) is attempting to move a
 * product that is  colliding with a child of another parent
 * (IE a standard colliding with a shelf that is not its parent)
 * If so disallow the move.
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.45 $
 */
public class ChildrenCollisionCheckRule extends BaseRule  {

    private static final String NOT_ABLE_TO_MOVE =
        "org.chefx3d.rules.definitions.ChildrenCollisionCheckRule.statusBarMsg";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ChildrenCollisionCheckRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
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

        // skip checking any auto-span items
        Boolean autoSpan = (Boolean)
        RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SPAN_OBJECT_PROP);
        
        if(autoSpan != null && autoSpan){
            result.setResult(true);
            return(result);
        }

        boolean returnBool = false;

        // if command is an instance of a remove entity child command
        // we need to perform a special case check on it
        if( command instanceof RemoveEntityChildCommand) {

            Boolean shadow = (Boolean)entity.getProperty(
                    entity.getParamSheetName(),
                    Entity.SHADOW_ENTITY_FLAG);

            if(shadow == null || shadow == false) {

                rch.performExtendedCollisionCheck(
                		command,
                		true,
                		false,
                		false);

                if(rch.collisionEntitiesMap != null){
                    returnBool = checkForInvalidSceneRemovalCase(
                            model,
                            entity,
                            command);
                }
            }

        } else {

            //create dummy command
            Command dummyCmd = createDummyCommand(model, entity, command);
            if(dummyCmd == null) {
                result.setResult(true);
                return(result);
            }

            //run extended collision check on dummy command
            rch.performExtendedCollisionCheck(
            		dummyCmd,
            		true,
            		false,
            		false);

            // store a reference to the current list of collisions
            Map<Entity,ArrayList<Entity>> dummyCollisionEntitiesMap =
            	new HashMap<Entity, ArrayList<Entity>>();

            if (rch.collisionEntitiesMap != null) {
            	dummyCollisionEntitiesMap.putAll(rch.collisionEntitiesMap);
            }
            
            rch.performExtendedCollisionCheck(
            		command,
            		true,
            		false,
            		false);

            if(rch.collisionEntitiesMap != null)
            {
                returnBool = checkForInvalidScene(
                        model,
                        entity,
                        command,
                        dummyCollisionEntitiesMap);
            }
        }

        // If the scene is invalid because of the change in the entity,
        // perform the appropriate action here.
        if (returnBool) {
            
        	String msg = "";
            
        	if(command instanceof MoveEntityCommand) {

                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".move");
                popUpMessage.showMessage(msg);

            } else if (command instanceof MoveEntityTransientCommand) {

            	MoveEntityTransientCommand mvCmd = 
            		(MoveEntityTransientCommand) command;
            	
            	double[] startPos = 
            		TransformUtils.getPosition((PositionableEntity)entity);
            	
            	mvCmd.setPosition(startPos);
            	
                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".move" );
                statusBar.setMessage(msg);

            } else if (command instanceof ScaleEntityCommand) {
            	
            	ScaleEntityCommand scaleCmd = 
            		(ScaleEntityCommand) command;
            	
            	double[] startPos = new double[3];
            	float[] startScale = new float[3];
            	
            	scaleCmd.getOldPosition(startPos);
            	scaleCmd.getOldScale(startScale);
            	
            	scaleCmd.setNewPosition(startPos);
            	scaleCmd.setNewScale(startScale);
            	
                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".scale");
                popUpMessage.showMessage(msg);

            } else if (command instanceof ScaleEntityTransientCommand) {

            	ScaleEntityTransientCommand scaleCmd = 
            		(ScaleEntityTransientCommand) command;
            	
            	double[] startPos = 
            		TransformUtils.getStartPosition((PositionableEntity)entity);
            	
            	float[] startScale =
            		TransformUtils.getStartingScale((PositionableEntity)entity);
            	
            	scaleCmd.setPosition(startPos);
            	scaleCmd.setScale(startScale);
            	
                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".scale");
                statusBar.setMessage(msg);

            } else if (command instanceof RotateEntityCommand) {

                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + "rotate");
                popUpMessage.showMessage(msg);

            } else if (command instanceof RotateEntityTransientCommand) {

            	RotateEntityTransientCommand rotCmd = 
            		(RotateEntityTransientCommand) command;
            	
            	float[] startRot = 
            		TransformUtils.getStartRotation((PositionableEntity)entity);
            	
            	rotCmd.setCurrentRotation(
            			startRot[0], 
            			startRot[1], 
            			startRot[2], 
            			startRot[3]);
            	
                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".rotate");
                statusBar.setMessage(msg);

            } else if (command instanceof TransitionEntityChildCommand) {

                if (command.isTransient()) {
                    msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".move" );
                    statusBar.setMessage(msg);
                } else {
                    msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".move");
                    popUpMessage.showMessage(msg);
                }

            } else if(command instanceof RemoveEntityChildCommand) {
                msg = intl_mgr.getString(NOT_ABLE_TO_MOVE + ".delete");
                popUpMessage.showMessage(msg);
            }

            result.setStatusValue(ELEVATION_LEVEL.SEVERE);

            if(!command.isTransient()) {
                result.setApproved(false);
                result.setNotApprovedAction(
                		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
            }

        }

        result.setResult(returnBool);
        return(result);

    }

    /**
     * Creates a dummy command that is used to determine the starting collisions
     * All commands are created with the start pos, start scale and start rot,
     * depending on the command.
     * None of these commands are ever actually fired
     * @param model
     * @param entity
     * @param command
     * @return
     */
    private Command createDummyCommand(WorldModel model, Entity entity, Command command) {

        double[] pos = new double[3];
        float[] scale = new float[3];
        float[] rot = new float[4];


        if(!(entity instanceof PositionableEntity)) {
            return null;
        }

        ((PositionableEntity)entity).getStartingPosition(pos);
        ((PositionableEntity)entity).getStartingScale(scale);
        ((PositionableEntity)entity).getStartingRotation(rot);


        Command returnCmd = null;
        if(command instanceof MoveEntityCommand) {
            returnCmd = new MoveEntityCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    pos);

        }else if(command instanceof MoveEntityTransientCommand  ) {
            returnCmd = new MoveEntityTransientCommand(
                    model,
                    command.getTransactionID(),
                    entity.getEntityID(),
                    pos,
                    new float[3]);

        }else if(command instanceof ScaleEntityCommand  ) {

            returnCmd = new ScaleEntityCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    pos,
                    scale,
                    scale);

        }else if(command instanceof ScaleEntityTransientCommand  ) {


            returnCmd = new ScaleEntityTransientCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    pos,
                    scale);

        }else if(command instanceof RotateEntityCommand  ) {


            returnCmd = new RotateEntityCommand(
                    model,
                    command.getTransactionID(),
                    (PositionableEntity)entity,
                    rot,
                    rot);

        }else if(command instanceof RotateEntityTransientCommand  ) {


            returnCmd = new RotateEntityTransientCommand(
                    model,
                    command.getTransactionID(),
                    entity.getEntityID(),
                    rot);

        }else if(command instanceof TransitionEntityChildCommand  ) {

            TransitionEntityChildCommand tecc = (TransitionEntityChildCommand) command;

            if (!command.isTransient()) {
	            // Get the start position from the command as the entity is relative to
	            // the current parent not the startParent
	            tecc.getStartPosition(pos);
	            tecc.getStartingRotation(rot);

	            returnCmd = new TransitionEntityChildCommand(
	                    model,
	                    ((PositionableEntity)entity),
	                    tecc.getStartParentEntity(),
	                    pos,
	                    rot,
	                    tecc.getStartParentEntity(),
	                    pos,
	                    rot,
	                    command.isTransient());
            } else {
            	return null;
            }

        }else if(command instanceof RemoveEntityChildCommand  ) {
            Entity parent = model.getEntity(entity.getParentEntityID());
            returnCmd = new RemoveEntityChildCommand(model,parent,entity,true);

        }
        return returnCmd;

    }

    /**
     *  Checks to determine if the scene is invalid because an entity has moved
     *  away,or been scaled stranding a child.
     *
     * @param model - The world model
     * @param command - The current command passed to performCheck
     * @param dummyCollisionEntitiesMap - The map of the collisions that the
     *  dummy command felt
     * @return true - if the scene is valid , false if invalid
     */
    private boolean checkForInvalidScene(WorldModel model,
            Entity currentEntity,
            Command command,
            Map<Entity,ArrayList<Entity>> dummyCollisionEntitiesMap) {

        if(dummyCollisionEntitiesMap == null ||
                dummyCollisionEntitiesMap.isEmpty()) {
            return false;
        }

        SceneManagementUtility.addTempSurrogate(collisionChecker, command);

        // Runs through each array list of the map
        Object[] keyArray =  dummyCollisionEntitiesMap.keySet().toArray();
        for(int i = 0; i < keyArray.length; i++) {

            Entity entity = (Entity)keyArray[i];


            ArrayList<Entity> dummyCollisionList =
                dummyCollisionEntitiesMap.get(entity);

            if(dummyCollisionList == null) {
                continue;
            }

            if(!(rch.collisionEntitiesMap.containsKey(entity))) {
                if (logFailures)
                    logFailure("checkForInvalidScene1");

                SceneManagementUtility.removeTempSurrogate(
                		rch.getRuleCollisionChecker(), 
                		(PositionableEntity)currentEntity);
                return true;
            }

            ArrayList<Entity> collisionList =
                rch.collisionEntitiesMap.get(entity);

            for(int j = 0; j < dummyCollisionList.size();j++) {

                Entity dummyEntity = dummyCollisionList.get(j);

                if(dummyEntity.isZone()) {
                    continue;
                }

                // skip moving any auto-span item. Scales are allowed.
                if (!(command instanceof ScaleEntityCommand) && 
                		!(command instanceof ScaleEntityTransientCommand)) {
                	
	                Boolean autoSpan =
	                    (Boolean) RulePropertyAccessor.getRulePropertyValue(
	                            dummyEntity,
	                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);
	
	                if(autoSpan != null && autoSpan){
	                    continue;
	                }
                }

                // If dummy entity is  in the current collision list do nothing
                // Else see if the dummy entity is still valid, through
                //checking its collisions . If not valid, return true, failing the rule

                if(collisionList.contains(dummyEntity)) {
                    //DO NOTHING

                } else {
					if (dummyEntity.getParentEntityID() == -1) {
						// rem: nfi if this is the 'right' thing to do, but 
						// it makes an issue with multizone placement go away.
						// if in this evaluation, an entity being added (in 
						// this case, the corner rounder bar) is the dummy entity,
						// then the collision check goes wrong. the entity 
						// is evaluated relative to the floor, intersects it,
						// and is deemed to be a bad collision. skipping any 
						// newly added entities avoids the condition. presume
						// that new adds are 'properly' evaluated elsewhere.
                        continue;
                    }
                    // Issue a standard move entity dummy command
                	// so we don't have any complications from the various
                	// command types being used to test an entity at the
                	// same position.
                	double[] dummyPosition = new double[3];
                	((PositionableEntity)dummyEntity).getPosition(dummyPosition);

                	MoveEntityCommand dummyCommand = new MoveEntityCommand(
                			model,
                			model.issueTransactionID(),
                			(PositionableEntity)dummyEntity,
                			dummyPosition,
                			dummyPosition);

                    rch.performCollisionCheck(dummyCommand, true, false, false);
                    //Somehow an object is colliding with nothing, definately need to fail
                    if(rch.collisionEntities == null) {
                        if (logFailures)
                            logFailure("checkForInvalidScene1");

                        SceneManagementUtility.removeTempSurrogate(
                        		rch.getRuleCollisionChecker(),
                        		(PositionableEntity)currentEntity);
                        return true;
                    }

                    rch.pruneCollisionList(model,dummyEntity);

                    if(rch.collisionEntities.contains(entity)) {
                        rch.collisionEntities.remove(entity);
                    }

                    List<Command> cmdList = getNewlyIssuedCommands();
                    //if there is a remove entity child command in the list
                    // Execute the command and remove the entity as it should
                    //not be on the stack
                    for(Command cmd: cmdList) {
                        if(cmd instanceof RemoveEntityChildCommand) {
                            Entity cmdEntity = ((RuleDataAccessor)cmd).getEntity();
                            model.forceCommandExecution(cmd);
                            if(rch.collisionEntities.contains(cmdEntity)) {
                                rch.collisionEntities.remove(cmdEntity);
                            }


                        }
                    }


                    rch.performCollisionAnalysisHelper(dummyEntity, null, false, null, true);

                    if(rch.hasIllegalCollisionHelper(dummyEntity)) {
                        if (logFailures)
                            logFailure("checkForInvalidScene2");

                        SceneManagementUtility.removeTempSurrogate(
                        		rch.getRuleCollisionChecker(),
                        		(PositionableEntity)currentEntity);
                        return true;
                    }


                }
            }
        }

        SceneManagementUtility.removeTempSurrogate(
        		rch.getRuleCollisionChecker(),
        		(PositionableEntity)currentEntity);

        return false;
    }

    /**
     *  Checks to determine if the scene is invalid because an entity
     *  has been removed
     *
     * @param model - The world model
     * @param command - The current command passed to performCheck
     * @return true - if the scene is valid , false if invalid
     */
    private boolean checkForInvalidSceneRemovalCase(WorldModel model,
            Entity currentEntity,
            Command command){

        //retrieves all the children connected to current entity
        //includes children of children
        ArrayList<Entity> children = new ArrayList<Entity>();
        findAllChildrenOfEntity(children,currentEntity);

        //No collisions return false
        if(rch.collisionEntitiesMap == null ||
                rch.collisionEntitiesMap.isEmpty()) {
            return false;
        }

        // Runs through each array list of the map
        Object[] keyArray = rch.collisionEntitiesMap.keySet().toArray();
        for(int i = 0; i < keyArray.length; i++) {

            Entity entity = (Entity)keyArray[i];

            ArrayList<Entity> collisionList =
                rch.collisionEntitiesMap.get(entity);

            for(int j = 0; j < collisionList.size();j++) {
                Entity collidingEntity = collisionList.get(j);
                if(collidingEntity.isZone()) {
                    continue;
                }

                // skip moving any auto-span item
                Boolean autoSpan =
                    (Boolean) RulePropertyAccessor.getRulePropertyValue(
                            collidingEntity,
                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);
                if(autoSpan != null && autoSpan){
                    continue;
                }

                // if  colliding entity is a child do nothing
                // else see if the entity is still valid
                //if not return true, so the rule will fail
                if(children.contains(collidingEntity)) {
                    //DO NOTHING

                } else {

                    rch.performCollisionCheck(createDummyCommand(model, collidingEntity, command), true, false, false);
                    if(rch.collisionEntities == null) {
                        if (logFailures)
                            logFailure("checkForInvalidSceneRemovalCase1");

                        return true;
                    }
                    rch.pruneCollisionList(model,collidingEntity);

                    if(rch.collisionEntities.contains(entity)) {
                        rch.collisionEntities.remove(entity);
                    }
                    rch.performCollisionAnalysisHelper(collidingEntity, null, false, null, true);

                    if(rch.hasIllegalCollisionHelper(collidingEntity)) {
                        if (logFailures)
                            logFailure("checkForInvalidSceneRemovalCase2");
                        return true;
                    }

                }
            }
        }


        return false;

    }

    /**
     * Find all children in the branch below the entity.
     * 
     * @param children ArrayList of children to fill up with results
     * @param entity Head of branch to get all children for
     */
    private void findAllChildrenOfEntity(
    		ArrayList<Entity> children , 
    		Entity entity){

        if(entity.hasChildren()== false) {
            return;
        }
        
        ArrayList<Entity> currentChildren = 
        	SceneHierarchyUtility.getExactChildren(entity);

        for(Entity currentEntity:currentChildren) {
            children.add(currentEntity);

            if(currentEntity.hasChildren()) {
                findAllChildrenOfEntity(children,currentEntity);
            }
        }



    }
}
