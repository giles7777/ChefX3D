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

//Internal Imports
import java.util.*;

import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.AutoSpanUtility;
import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

/**
* Determines if Entity is colliding with other objects and responds
* with correction if required. Also performs selection highlighting for transient
* commands that are causing a collection.
*
* @author Ben Yarger
* @version $Revision: 1.92 $
*/
public class MovementHasObjectCollisionsRule extends BaseRule {

    /** Status message when mouse button released and collisions exist */
    private static final String MV_PLACE_COL_PROP =
        "org.chefx3d.rules.definitions.MovementHasObjectCollisionsRule.cannotMove";

    /** Status message when illegal collisions exist for transient commands */
    private static final String MV_TRANS_COL_PROP =
        "org.chefx3d.rules.definitions.MovementHasObjectCollisionsRule.collisionsExist";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementHasObjectCollisionsRule(
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

        if(entity.getType() == Entity.TYPE_VERTEX||
                entity.getType() == Entity.TYPE_SEGMENT ) {
            result.setResult(true);
            return(result);
        }

        // get the collision list after the move is done
        rch.performExtendedCollisionCheck(command, true, true, false);

        // If collisionEntities is null (no collisions occurred) then return false
        if (rch.collisionEntitiesMap == null) {
            result.setResult(true);
            return(result);
        }
        
        // Debug
//      rch.printCollisionEntitiesMap(false);

        // copy the results into a local collection
        Map<Entity, ArrayList<Entity>> endCollisionList = 
        	new HashMap<Entity, ArrayList<Entity>>();
        
        if (rch.collisionEntitiesMap != null) {
        	endCollisionList.putAll(rch.collisionEntitiesMap);
        }
        
        // create a command that puts the entity where it started
        Command mvCmd = createTempCommand(model, entity, command);

        // Temporarily add the surrogate for the entity so the children will
        // evaluate correctly according to starting state
        SceneManagementUtility.addTempSurrogate(collisionChecker, mvCmd);
        
        // get the collision list before the move is done
        rch.performExtendedCollisionCheck(mvCmd, true, true, true);
        
        SceneManagementUtility.removeTempSurrogate(
        		collisionChecker, (PositionableEntity)entity);
    
        // Copy the starting state collision results into a local collection
        Map<Entity, ArrayList<Entity>> startCollisionList = 
        	new HashMap<Entity, ArrayList<Entity>>();
        
        if (rch.collisionEntitiesMap != null) {
        	startCollisionList.putAll(rch.collisionEntitiesMap);
        }

        // get the list of connected auto-spans to ignore
        int[] ignoreEntities = null;
        boolean isValid = true;
        
        ArrayList<Entity> autoSpanEntities = new ArrayList<Entity>();

        Iterator<Map.Entry<Entity, ArrayList<Entity>>> itr =
            endCollisionList.entrySet().iterator();
                
        // Do our special auto span testing
        while (itr.hasNext()) {
        	
            Map.Entry<Entity, ArrayList<Entity>> next = itr.next();
            
            Entity checkEntity = (Entity) next.getKey();
            
            ArrayList<Entity> checkEndList = next.getValue();
            ArrayList<Entity> checkStartList = 
            	startCollisionList.get(checkEntity);
            
            for(int i = 0; i < checkEndList.size(); i++){

                Entity tmpEntity = checkEndList.get(i);

                Boolean autoSpan =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            tmpEntity,
                            ChefX3DRuleProperties.SPAN_OBJECT_PROP);
                
                if (autoSpan) {

                	// check the relationships to make sure the collision is legal
                    if (AutoSpanUtility.isAllowedAutoSpanCollision(tmpEntity, checkEntity)) {
                        continue;
                    }
                    
                    // now check to see if the auto spanning entity was 
                    // adjusted somewhere during this evaluation and thus
                    // is not a good starting point evaluation case. If this is
                    // so, then assume this adjustment is now valid.
                    ArrayList<Command> fullCmdList = (ArrayList<Command>) 
                    	CommandSequencer.getInstance().getFullCommandList(true);
                    
                    Entity cmdEvalEntity = null;
                    boolean match = false;
                    
                    for (int w = 0; w < fullCmdList.size(); w++) {
                    	
                    	if (fullCmdList.get(w) instanceof RuleDataAccessor) {
                    		
                    		cmdEvalEntity = 
                    			((RuleDataAccessor)fullCmdList.get(w)
                    					).getEntity();
                    		
                    		if (cmdEvalEntity.getEntityID() == tmpEntity.getEntityID()) {
                    			match = true;
                    			break;
                    		}
                    	}
                    }
                    
                    if (match) {
                    	continue;
                    }
                    
                    // now check to make sure it started in collision, if it 
                    // didn't then it was not a mount point
                    if (!checkStartList.contains(tmpEntity)) {                    	
                    	isValid = false;
                    	break;
                    }
                    
                    // Do the standard procedure to add the auto span to the ignore list
                    autoSpanEntities.add(tmpEntity);
            
                }
            }
            
            if (!isValid)
            	break;
        }

        if (autoSpanEntities.size() > 0) {
            ignoreEntities = new int[autoSpanEntities.size()];
            for(int i = 0; i < autoSpanEntities.size(); i++) {
                ignoreEntities[i] = autoSpanEntities.get(i).getEntityID();
            }
        }

        // only perform these if the auto-span check succeeded
        if (isValid) {
        	
            // reset the collision list to after the move is done
            rch.collisionEntitiesMap.putAll(endCollisionList);
        	
            // Perform collision analysis
            rch.performExtendedCollisionAnalysisHelper(
            		null, false, ignoreEntities);

            // EMF: fail in the case where there are collisions
            // with 'potential' parents
            if(failFromPotentialParentCollisions(entity)){
            	
                if (!command.isTransient()) {
                    result.setApproved(false);
                    result.setNotApprovedAction(
                    		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                } else {
                    result.setStatusValue(ELEVATION_LEVEL.SEVERE);
                }

                result.setResult(false);
                return(result);

            }

            // If there are no collisionEntities, see if the reserved None
            // relationship classification exists. If so, then we are cool,
            // return true, no illegal collisions.

            if (!rch.legalZeroCollisionExtendedCheck()) {
                result.setResult(true);
                return(result);
            }

            // Make sure collisions are legal - return true if collisions are
            // legal.
            if (!rch.hasIllegalCollisionExtendedHelper()) {
                result.setResult(true);
                return(result);
            }
        }
        
        if(command instanceof MoveEntityCommand){

            String msg = intl_mgr.getString(MV_PLACE_COL_PROP);
            popUpMessage.showMessage(msg);

            result.setApproved(false);
            result.setNotApprovedAction(
            		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
            result.setResult(false);
            return(result);

        } else if (command instanceof TransitionEntityChildCommand &&
                !command.isTransient()){

            String msg = intl_mgr.getString(MV_PLACE_COL_PROP);
            popUpMessage.showMessage(msg);

            result.setApproved(false);
            result.setResult(false);
            result.setNotApprovedAction(
            		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
            return(result);

        } else {

            String msg = intl_mgr.getString(MV_TRANS_COL_PROP);
            statusBar.setMessage(msg);

            result.setStatusValue(ELEVATION_LEVEL.SEVERE);
            result.setResult(false);
            return(result);

        }
    }

    //---------------------------------------------------------------
    // Local methods
    //---------------------------------------------------------------

    /**
     * This method returns TRUE if the entity has a permanent
     * parent set and yet it intersects with one or more
     * potential parents. <p>
     * Example test case: in a situation where a closet rod is
     * already parented to a shelf, if one moves the closetrod
     * so that it intersects another shelf on an adjacent wall,
     * then this method should return TRUE.
     *
     * @author Eric Fickenscher
     * @param entity the currently-moving entity
     * @return TRUE if there is a permanent parent set and there
     * is collision with one or more potential parents.
     */
    private boolean failFromPotentialParentCollisions(Entity entity){

        Boolean permanentParentSet =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.PERMANENT_PARENT_SET);

        Integer initialAddParent =
            (Integer)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.INITAL_ADD_PARENT);


        //
        // If a permanentParent is set, then we do not want to
        // allow the moving entity to collide with any other
        // potential parents
        //
        if( permanentParentSet != null && permanentParentSet){

            String[] list =
                (String[])RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

            if (list == null)
                return false;

            //
            // get the allowed parents
            //
            List<String> allowedParents = Arrays.asList(list);

            //
            // look through the collisions and see if any of them
            // could be potential parents.  If so, this means we
            // have a situation where an entity with a permanent parent
            // set needs its movement restricted because it has
            // a bad collision.
            //
            ArrayList<Entity> collisions = rch.collisionEntitiesMap.get(entity);

            for( Entity collision : collisions ){

                if (collision.getEntityID() == initialAddParent)
                    continue;

                List<String> classifications =
                    Arrays.asList(
                            (String[])RulePropertyAccessor.getRulePropertyValue(
                                    collision,
                                    ChefX3DRuleProperties.CLASSIFICATION_PROP));


                for(String parent : allowedParents){
                    if(classifications.contains(parent))
                        return true;
                }
            }
        } else {
        	//
        	// permanent parent is NOT set, so perhaps we should check that
        	// the moving entity is not colliding with multiple possible
        	// parents?
        }
        return false;
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

        // ignore if not a positional entity
        if(!(entity instanceof PositionableEntity)) {
            return null;
        }
        
        // get the default values
        ((PositionableEntity)entity).getStartingPosition(pos);
        
        Command returnCmd = null;
        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand)command).getStartPosition(pos);

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
    
}
