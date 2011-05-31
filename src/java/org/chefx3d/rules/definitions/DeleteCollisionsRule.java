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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.rules.util.AutoAddUtility;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.tool.SimpleTool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Check the legal existence of non child collisions found for an entity
 * being removed. If they do not have legal collisions with the entity removed
 * then they too should be removed. Continue to check the tree for this case
 * until all remaining are legal collisions.
 *
 * @author Ben Yarger
 * @version $Revision: 1.23 $
 */
public class DeleteCollisionsRule extends BaseRule  {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public DeleteCollisionsRule(
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

        // Do not evaluate for shadow entities
        Boolean isShadow = (Boolean) entity.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                Entity.SHADOW_ENTITY_FLAG);

        if (isShadow != null && isShadow == true) {
            result.setResult(true);
            return(result);
        }

        // Do not evaluate for auto-span entities
        Boolean isAutoSpan = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        if (isAutoSpan) {
            result.setResult(true);
            return(result);
        }
/*        
        // Don't evaluate for auto added entities
        Boolean isAutoAdd = (Boolean) 
        	RulePropertyAccessor.getRulePropertyValue(
        			entity, 
        			ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);
        
        if (isAutoAdd) {
        	result.setResult(true);
            return(result);
        }
*/
        // Validate that we have a remove command to evaluate
        if (command instanceof RemoveEntityChildCommand) {
            ;
        } else if (command instanceof RemoveEntityChildTransientCommand) {
            ;
        } else {
            result.setResult(true);
            return(result);
        }

        // Get the list of collisions including children
        rch.performCollisionCheck(command, true, false, false);

        if (rch.collisionEntities == null) {
            result.setResult(true);
            return(result);
        }

        ArrayList<Entity> collisions =
            new ArrayList<Entity>(rch.collisionEntities);

        // Start the list of checked entity ID's
        ArrayList<Integer> checkedIDs =
            new ArrayList<Integer>();

        checkedIDs.add(entity.getEntityID());
        checkedIDs.add(entity.getParentEntityID());

        // Start the list of entity ID's that are being removed.
        // This should include all children of a removed entity
        // so we can be highly accurate about the state of the
        // scene during our tests.
        ArrayList<Integer> idsBeingRemoved =
            new ArrayList<Integer>();

        idsBeingRemoved.add(entity.getEntityID());

        // Call our recursive loop to remove collisions
        deleteCollisions(
                entity,
                collisions,
                idsBeingRemoved,
                checkedIDs);

        result.setResult(true);
        return(result);
    }

    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

    private void deleteCollisions(
            Entity entity,
            ArrayList<Entity> entitiesToCheck,
            ArrayList<Integer> idsBeingRemoved,
            ArrayList<Integer> checkedIDs) {

        for (int i = 0; i < entitiesToCheck.size(); i++) {

            // If this entities ID is in CheckedIDs then go on to the next one.
            Entity testEntity = entitiesToCheck.get(i);
            Integer testEntityID = testEntity.getEntityID();

            if (checkedIDs.contains(testEntityID)) {
                continue;
            }

            // If the entity is not of TYPE_MODEL variant don't check it.
            // This way we avoid deleting zones, etc.
            if (!testEntity.isModel()) {
                continue;
            }

            // We need to have a positionable entity for this to work, so
            // safety check that here.
            if (!(testEntity instanceof PositionableEntity)) {
                continue;
            }
            
            // If the test entity is a legitimate model swap target, then
            // don't check it.
            if (SceneHierarchyUtility.isSwapTarget(
            		model, entity, testEntity, true)) {
            	continue;
            }

            // Add the testEntity entityID to the checkedIDs list.
            checkedIDs.add(testEntityID);

            // Take an entity to check and do a collision check on it.
            // Fake it with a move command at its current position.
            // Get our dummy command to use in the collision test
            Command dummyCmd = 
            	rch.createCollisionDummyCommand(
            			model, 
            			(PositionableEntity) testEntity, 
            			true, 
            			false);

            rch.performCollisionCheck(dummyCmd, true, false, false);

            if (rch.collisionEntities == null) {
                continue;
            }

            // Make a copy of the collisions found
            ArrayList<Entity> testEvalCollisions =
                new ArrayList<Entity>(rch.collisionEntities);

            // Do the collision analysis, ignoring ids already removed
            int[] idsToIgnore = new int[idsBeingRemoved.size()];

            for (int w = 0; w < idsBeingRemoved.size(); w++) {
                idsToIgnore[w] = idsBeingRemoved.get(w);
            }

            rch.performCollisionAnalysisHelper(
            		testEntity, null, false, idsToIgnore, true);


            // If there are illegal collisions with the isdBeingRemoved taken
            // away, add the testEntity id to the idsBeingRemoved list.
            // Next, call deleteCollisions on its collision entities and
            // children. When it returns, add the remove command for the
            // testEntity to the list of removes returned.
            // (Only issue a remove command if testEntity is not a child of
            // the entity parameter passed in).
            if (rch.hasIllegalCollisionHelper(testEntity)) {

                idsBeingRemoved.add(testEntityID);

                // is this an auto-add and
                // it has a required distance that fails
                // remove the parent as well.
                
                // process any potential sub-removal commands
                deleteCollisions(
                    testEntity,
                    testEvalCollisions,
                    idsBeingRemoved,
                    checkedIDs);
                
                SceneManagementUtility.removeChild(
                		model, collisionChecker, testEntity, false);

            } else {

                Boolean autoAddByCol =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            testEntity,
                            ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);

                if (autoAddByCol) {

                    String[] autoPlaceObjectsProp =
                        (String[])RulePropertyAccessor.getRulePropertyValue(
                                testEntity,
                                ChefX3DRuleProperties.AUTO_ADD_COL_PROP);

                    Enum[] autoAddAxis =
                        (Enum[])RulePropertyAccessor.getRulePropertyValue(
                                testEntity,
                                ChefX3DRuleProperties.AUTO_ADD_COL_AXIS);

                    // does the parent have a step size and axis defined
                    float[] autoAddStepSize =
                        (float[])RulePropertyAccessor.getRulePropertyValue(
                                testEntity,
                                ChefX3DRuleProperties.AUTO_ADD_COL_STEP_SIZE);

                    // get the product position
                    double[] pos = new double[3];
                    ((PositionableEntity)testEntity).getPosition(pos);

                    // create a temp command so we can do a collision check on 
                    // the item correctly
                    MoveEntityCommand moveCmd = new MoveEntityCommand(
                            model,
                            model.issueTransactionID(),
                            (PositionableEntity)testEntity,
                            pos,
                            pos);

                    boolean isValid = true;
                    ArrayList<Entity> validCollisions = null;
                    for(int j = 0; j < autoPlaceObjectsProp.length; j++){

                        SimpleTool simpleTool =
                            RuleUtils.getSimpleToolByName(
                            		autoPlaceObjectsProp[j]);

                        ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS adjAxis =
                            (ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)
                            autoAddAxis[j];

                        // TODO: getValidCollisions should be in a utility class
                        //AddAutoAddRule addRule =
                        //    new AddAutoAddRule(
                        //			errorReporter, collisionChecker, view);

                        validCollisions =
                            //addRule.getValidCollisions(
							AutoAddUtility.getValidCollisions(
                                    model,
                                    autoAddByCol,
                                    adjAxis,
                                    autoAddStepSize[j],
                                    simpleTool,
                                    moveCmd,
                                    null,
                                    rch);

                        if (validCollisions.contains(null)) {
                            isValid = false;
                            break;
                        }

                    }

                    // does the item have an overhang limit
                    Boolean checkOverHang =
                        (Boolean)RulePropertyAccessor.getRulePropertyValue(
                                testEntity,
                                ChefX3DRuleProperties.RESPECTS_OVERHANG_LIMIT);


                    // if so then check the overhang limit.
                    if (checkOverHang) {

                        boolean overhangProblem =
                        	BoundsUtils.checkOverhangLimit(
                        			model, 
                        			moveCmd, 
                        			testEntity, 
                        			new int[] {entity.getEntityID()}, 
                        			rch);
                        
                        if (overhangProblem) {
                            isValid = false;
                        }
                    }

                    // the parent is no longer valid, lets remove it as well
                    if (!isValid) {
                    	
                    	SceneManagementUtility.removeChild(
                    			model, collisionChecker, testEntity, false);
                    	
                    }
                }
            }
        }
    }
    
}