/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.AutoAddUtility;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * When a standard is moved check to see if any shelves or other
 * scaleable items are attached to it.  If there are items then extra
 * commands need to be added.  If the standard is the far left or right
 * support for the shelf then we need to scale the shelf.  If it is
 * in the middle of the shelf then we just need to remove the auto-add
 * brackets (added back later).
 *
 * @author Russell Dodds
 * @version $Revision: 1.30 $
 */
public class MovementScalesAttachedChildrenRule extends BaseRule {
    
    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementScalesAttachedChildrenRule(
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
     * When a standard is moved check to see if any shelves or other
     * scaleable items are attached to it.  If there are items then extra
     * commands need to be added.  If the standard is the far left or right
     * support for the shelf then we need to scale the shelf.  If it is
     * in the middle of the shelf then we just need to remove the auto-add
     * brackets (added back later).
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

        // is the moving item an auto-added item
        Boolean isAutoAddProduct =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

        if (!isAutoAddProduct) {
            result.setResult(true);
            return(result);
        }

        // is the moving item supposed to scale attached products
        Boolean movementScalesAttached =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MOVEMENT_SCALES_ATTACHED_PRODUCTS);

        if (!movementScalesAttached) {
            result.setResult(true);
            return(result);
        }
        
        // Validate the command
        if (!(command instanceof MoveEntityCommand) && 
        		!(command instanceof MoveEntityTransientCommand) &&
        		!(command instanceof TransitionEntityChildCommand)) {
        	
        	result.setResult(true);
            return(result);
        }
        
        // Entity has to be a positionable entity
        if (!(entity instanceof PositionableEntity)) {
        	result.setResult(true);
            return(result);
        }
        
        // perform the scale
        scaleAttachments(
        		model,
        		command,
        		(PositionableEntity)entity);
  
        // If the command is not transient, then we have used the valid
        // collisions for the last time in this series of movements so clear
        // it. Also set the lastMovingEntity to null so we are certain to 
        // start over.
        if (!command.isTransient()) {
        	
        	// Reset the side pocketed scale targets to null so we pick up on
        	// the need to get new scale targets next time through.
        	RulePropertyAccessor.setRuleProperty(
        			entity, 
        			ChefX3DRuleProperties.MOVEMENT_SCALE_ATTACHED_TARGETS, 
        			null);

        }

        result.setResult(true);
        return(result);

    }
    
    /**
     * Get the map of attached shelves from the entity being moved.  For
     * each of those shelves adjust the scale appropriately.
     *
     * @param model The WorldModel
     * @param command The original command
     * @param entity The entity to adjust
     * @param delta The change is position
     */
    private void scaleAttachments(
            WorldModel model,
            Command command,
            PositionableEntity entity) {

    	// Note, this will be reset to null in the calling method if the 
    	// command is not transient.
    	ArrayList<PositionableEntity> scaleTargets = 
    		(ArrayList<PositionableEntity>)
    		RulePropertyAccessor.getRulePropertyValue(
    				entity, 
    				ChefX3DRuleProperties.MOVEMENT_SCALE_ATTACHED_TARGETS);
    	
    	if (scaleTargets == null) {

	    	// Get the scale targets
	    	scaleTargets =	getScaleTargets(model, command, entity);
	    	
	    	// Side pocket it for the future
	    	RulePropertyAccessor.setRuleProperty(
        			entity, 
        			ChefX3DRuleProperties.MOVEMENT_SCALE_ATTACHED_TARGETS, 
        			scaleTargets);
    	
    	}

    	// If there are no scale targets, get the hell out of dodge!
        if (scaleTargets.size() == 0) {
        	return;
        }
        
        // Get the starting and ending positions of the adjustment
        // in active zone coordinates. Starting position is where it was last
        // frame. Ending position is where it will be the next frame.
    	Entity activeZone = SceneHierarchyUtility.getActiveZoneEntity(model);

    	double[] startEntityPosition =
    		TransformUtils.getExactRelativePosition(
        		model, entity, activeZone, true);
    	
    	double[] endEntityPosition =
    		TransformUtils.getExactRelativePosition(
        		model, entity, activeZone, false);

    	// Calculate the displacement between start and end that should be
    	// applied to all other scale targets.
    	double[] curTranslation = new double[3];
    	curTranslation[0] = endEntityPosition[0] - startEntityPosition[0];
    	curTranslation[1] = endEntityPosition[1] - startEntityPosition[1];
    	curTranslation[2] = 0.0;

    	// Fields used in calcualtion
    	double[] targetStartPosition = new double[3];
    	double[] targetNewPosition = new double[3];
    	float[] targetSize = new float[3];
    	float[] targetStartScale = new float[3];
    	float[] targetStartSize = new float[3];
    	float[] targetNewSize = new float[3];
    	float[] targetNewScale = new float[3];
    	double[] targetParentPos = new double[3];

        // Scale each scale target
        for (int i = 0; i < scaleTargets.size(); i++) {

        	PositionableEntity scaleTarget = scaleTargets.get(i);

        	targetStartPosition =
        		TransformUtils.getExactRelativePosition(
        				model, scaleTarget, activeZone, true);

        	// Establish the full starting size
        	scaleTarget.getSize(targetSize);

        	targetStartScale = TransformUtils.getStartingScale(scaleTarget);

        	targetStartSize[0] = targetSize[0] * targetStartScale[0];
        	targetStartSize[1] = targetSize[1] * targetStartScale[1];
        	targetStartSize[2] = targetSize[2] * targetStartScale[2];

        	//------------------------------------------------------------------
        	// Adjust size per axis based on start position of moving entity
        	// and starting position of scaling target.
        	// If you are looking for the z adjustment, we don't make one via
        	// this process. Doing so wouldn't be a good idea.
        	//------------------------------------------------------------------
        	
        	// Check to see if the starting position of the moving entity
        	// is on the left or right side of the target. Base the change
        	// in scale on which side the moving entity is on.
        	if (startEntityPosition[0] < targetStartPosition[0]) {

        		targetNewSize[0] =
        			targetStartSize[0] - (float) curTranslation[0];
        		
        	} else {

        		targetNewSize[0] =
        			targetStartSize[0] + (float) curTranslation[0];
        	}
        	
        	// Do the same for the y axis case.
        	if (startEntityPosition[1] < targetStartPosition[1]) {

        		targetNewSize[1] =
        			targetStartSize[1] - (float) curTranslation[1];
        	
        	} else  {

        		targetNewSize[1] =
        			targetStartSize[1] + (float) curTranslation[1];
        		
        	}

        	// Set the new target scale. Note that the z axis should never 
        	// be adjusted by this process.
        	targetNewScale[0] = targetNewSize[0] / targetSize[0];
        	targetNewScale[1] = targetNewSize[1] / targetSize[1];
        	targetNewScale[2] = targetStartScale[2];
        	
        	// Adjust the position by half the current translation calculated
        	// for the moving entity. Note that the z axis should never be
        	// adjusted by this process.
        	targetNewPosition[0] = 
        		targetStartPosition[0] + curTranslation[0]/2.0;
        	targetNewPosition[1] = 
        		targetStartPosition[1] + curTranslation[1]/2.0;
        	targetNewPosition[2] = targetStartPosition[2];
        	
        	// Need to convert targetNewPosition to relative position of its 
        	// parent. The value of targetNewPosition is relative to the 
        	// active zone.
        	PositionableEntity scaleTargetParent = (PositionableEntity) 
        		SceneHierarchyUtility.getExactParent(model, scaleTarget);
        	
        	targetParentPos = 
            		TransformUtils.getExactRelativePosition(
            				model, scaleTargetParent, activeZone, false);
        	
        	targetNewPosition[0] -= targetParentPos[0];
        	targetNewPosition[1] -= targetParentPos[1];
        	targetNewPosition[2] -= targetParentPos[2];
     	
        	// Prepare the start children data
        	ArrayList<Entity> startChildren = new ArrayList<Entity>();
        	ArrayList<PositionableData> startPositions = 
        		new ArrayList<PositionableData>();
        	
        	if (!command.isTransient()) {
        		
	        	Entity[] startingChildren = scaleTarget.getStartingChildren();
	        	
	        	PositionableEntity startingChild = null;
	        	
	        	for (int j = 0; j < startingChildren.length; j++) {
	        		
	        		if (startingChildren[j] instanceof PositionableEntity) {
	        			startingChild = 
	        				(PositionableEntity) startingChildren[j];
	        		} else {
	        			continue;
	        		}
	        		
	        		startChildren.add(startingChild);
	        		startPositions.add(startingChild.getPositionableData());
	        	}
	        	
	        	scaleTarget.clearStartingChildren();
        	}

        	SceneManagementUtility.scaleEntity(
    				model, 
    				collisionChecker, 
    				scaleTarget, 
    				targetNewPosition, 
    				targetNewScale, 
    				command.isTransient(),
    				startChildren,
    				startPositions,
    				false);
        }
    }
    
    /**
     * Get the scale targets based on what we are currently in collision with
     * and baseline criteria for what we can get away with scaling.
     * 
     * @param model WorldModel to reference
     * @param command Command moving the entity
     * @param entity Moving entity
     * @return List of PositionableEntities that are the scale targets
     */
    private ArrayList<PositionableEntity> getScaleTargets(
    		WorldModel model,
    		Command command,
    		PositionableEntity entity) {

    	ArrayList<PositionableEntity> scaleTargets =
    		new ArrayList<PositionableEntity>();

    	// Side pocket data to replace original command end data with later   	
    	double[] originalEndPosition = new double[3];
    	Entity originalParentEntity = null;

    	// Get the test data
    	double[] testPosition = TransformUtils.getStartPosition(entity);
    	Entity testParent = SceneHierarchyUtility.getParent(model, entity);

    	// Setup the commands to do the collision check to collect scale targets
    	// from the start data.
    	if (command instanceof MoveEntityCommand) {

    		MoveEntityCommand mvCmd = (MoveEntityCommand) command;

    		mvCmd.getEndPosition(originalEndPosition);
    		mvCmd.getStartPosition(testPosition);
    		mvCmd.setEndPosition(testPosition);

    	} else if (command instanceof MoveEntityTransientCommand) {

    		MoveEntityTransientCommand mvCmd =
    			(MoveEntityTransientCommand) command;

    		mvCmd.getPosition(originalEndPosition);
    		mvCmd.setPosition(testPosition);

    	} else if (command instanceof TransitionEntityChildCommand) {

    		TransitionEntityChildCommand tCmd =
    			(TransitionEntityChildCommand) command;

    		originalParentEntity = tCmd.getEndParentEntity();
    		tCmd.getEndPosition(originalEndPosition);
    		
    		tCmd.getStartPosition(testPosition);
    		testParent = tCmd.getStartParentEntity();

    		tCmd.setEndParentEntity(testParent);
    		tCmd.setEndPosition(testPosition);
    	}

    	rch.performCollisionCheck(command, true, false, false);

    	// Set our original values back
    	if (command instanceof MoveEntityCommand) {

    		MoveEntityCommand mvCmd = (MoveEntityCommand) command;

    		mvCmd.setEndPosition(originalEndPosition);

    	} else if (command instanceof MoveEntityTransientCommand) {

    		MoveEntityTransientCommand mvCmd =
    			(MoveEntityTransientCommand) command;

    		mvCmd.setPosition(originalEndPosition);

    	} else if (command instanceof TransitionEntityChildCommand) {

    		TransitionEntityChildCommand tCmd =
    			(TransitionEntityChildCommand) command;

    		tCmd.setEndParentEntity(originalParentEntity);
    		tCmd.setEndPosition(originalEndPosition);
    	}

    	// Establish the active zone for future use
    	Entity activeZone = SceneHierarchyUtility.getActiveZoneEntity(model);

    	// Start narrowing down the entities.
    	// Scale targets cannot be:
    	// 1) Direct parents of the moving entity
    	// 2) A non model type entity
    	// 3) A non-scalable entity
    	// 4) An auto added entity
    	// 5) A product without a 3D model
    	// 6) Anything but a PositionableEntity instance
    	// 7) In a branch of the hierarchy that does not lead back to the
    	// active zone
    	// 8) Scaling in anything but a single axis (x or y)
    	//
    	// Lastly, the moving entity must be on the outside of the siblings
    	// of the entity target.
    	
    	ArrayList<Entity> possibleScaleTargets = new ArrayList<Entity>();
    	possibleScaleTargets.addAll(rch.collisionEntities);
    	
        for (int i = 0; i < possibleScaleTargets.size(); i++) {

            Entity check = possibleScaleTargets.get(i);

            // skip the parent
            if (check == testParent) {
                continue;
            }

            // skip non-model objects
            if (!check.isModel()) {
                continue;
            }

            // skip non-scalable items
            Boolean isScalable =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        check,
                        ChefX3DRuleProperties.CAN_SCALE_PROP);

            if (!isScalable) {
                continue;
            }
            
        	// skip auto-add products
            Boolean isAutoAddProduct =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        check,
                        ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

            if (isAutoAddProduct) {
                continue;
            }

            // Remove any invisible auto adds
            Boolean noModel =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        check,
                        ChefX3DRuleProperties.NO_MODEL_PROP);

            if (noModel) {
                continue;
            }
            
            // remove any non-positionable entities
            if (!(check instanceof PositionableEntity)) {
                continue;
            }
            
            // Product must have an ancestor that is the active zone
            boolean isChildOfActiveZone =
            	SceneHierarchyUtility.isEntityChildOfParent(
            		model, check, activeZone, true);

            if (!isChildOfActiveZone) {
            	continue;
            }
            
            // Scale must be restricted to a single axis for this to work.
            SCALE_RESTRICTION_VALUES scaleRestriction =
				(SCALE_RESTRICTION_VALUES)
					RulePropertyAccessor.getRulePropertyValue(
					check,
					ChefX3DRuleProperties.SCALE_RESTRICTION_PROP);

			if (scaleRestriction == null) {
				scaleRestriction = SCALE_RESTRICTION_VALUES.NONE;
			}

			TARGET_ADJUSTMENT_AXIS axis = TARGET_ADJUSTMENT_AXIS.XAXIS;

			switch(scaleRestriction) {

			case XAXIS:
				axis = TARGET_ADJUSTMENT_AXIS.XAXIS;
				break;
			case YAXIS:
				axis = TARGET_ADJUSTMENT_AXIS.YAXIS;
				break;
			default:
				continue;

			}

            // Entity must be on the outside of check.
			// Do a collision check and ignore all children of the scaleTarget.
			// If the results don't contain our entity, that is moving and 
			// potentially causing the scale change of the check entity,
			// then add it to the sortList. Sort the sortList in descending
			// position order and see if the moving entity is at the beginning
			// or end of the list. If it is, then it will scale the check 
			// entity.
			
			double[] testPos = 
				TransformUtils.getExactPosition((PositionableEntity) check);
			
			MoveEntityCommand dummyCmd = 
				new MoveEntityCommand(
						model, 
						0, 
						(PositionableEntity) check, 
						testPos, 
						testPos);
			
			rch.performCollisionCheck(dummyCmd, true, false, true);
			
            ArrayList<Entity> sortList = new ArrayList<Entity>();
            sortList.addAll(rch.collisionEntities);
            
            if (!sortList.contains(entity)) {
            	sortList.add(entity);
            }
            
            // Remove all children from the list.
            ArrayList<Entity> children = 
            	SceneHierarchyUtility.getExactChildren(check);
            
            for (Entity child : children) {
            	sortList.remove(child);
            }

            sortList =
            	TransformUtils.sortDescendingRelativePosValueOrder(
            			model, 
            			sortList, 
            			(PositionableEntity) activeZone, 
            			axis, 
            			true);

            
            if (entity != sortList.get(0) &&
            		entity != sortList.get(sortList.size() - 1)) {
            	
            	continue;
            }

            // We passed all the tests, so add the item to the list of scale
            // targets.
            scaleTargets.add((PositionableEntity)check);
            
            // Remove non critical auto adds since we will be scaling the check
            // entity so they will be add back in.
        	AutoAddUtility.removeNonCriticalAutoAdds(
        			model, check, rch, view);
        }

        return scaleTargets;
    }

}