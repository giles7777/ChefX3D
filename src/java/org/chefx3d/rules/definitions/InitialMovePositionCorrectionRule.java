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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Handles initial position corrections for move commands.
 *
 * @author Ben Yarger
 * @version $Revision: 1.49 $
 */
public class InitialMovePositionCorrectionRule extends
        InitialPositionCorrectionRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public InitialMovePositionCorrectionRule(
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

        // Only operate on positionable entities of type MODEL
        if (!(entity instanceof PositionableEntity) ||
                !entity.isModel()) {

            result.setResult(true);
            return(result);
        }

        // Find the most appropriate entity either in immediate collision
        // or somewhere behind. Find closest entity.
		Entity newParentEntity = rch.findAppropriateParent(command);

        // If the parentEntity is null, default to the active zone
        if (newParentEntity == null) {
            result.setResult(true);
            return(result);
        }

        // Apply results to appropriate command
        if (command instanceof MoveEntityCommand) {

            //
            // Modify the incoming position
            //
            double[] position = new double[3];
            ((MoveEntityCommand)command).getEndPosition(position);

            // Calculate the resulting depth change that should occur based on
            // the newParentEntity.
            position[2] += calculateDepthChanges(
                        command,
                        model,
                        entity,
                        newParentEntity);

            // Catch the special door and window case
            handleDoorAndWindowCase(entity, position, false);

            // Catch the auto-span case
            handleAutoSpanCase(entity, position, false);

            // Apply adjustments to position
            ((MoveEntityCommand)command).setEndPosition(position);

            // Set relative position
            generalGenPosCalc(model, command);

        } else if (command instanceof MoveEntityTransientCommand) {

            //
            // Modify the position of the command, which is relative to the
            // wall segment.
            //
            double[] position = new double[3];
            ((MoveEntityTransientCommand)command).getPosition(position);

         	// Calculate the resulting depth change that should occur based on
            // the newParentEntity.
            position[2] += calculateDepthChanges(
                        command,
                        model,
                        entity,
                        newParentEntity);

            // Catch the special door and window case
            handleDoorAndWindowCase(entity, position, true);

            // Catch the auto-span case
            handleAutoSpanCase(entity, position, true);

            // Apply the adjustment to the position
            ((MoveEntityTransientCommand)command).setPosition(
                    position);

            ((MoveEntityTransientCommand)command).setPickParentEntity(
            		newParentEntity);

            ((MoveEntityTransientCommand)command).getPosition(position);

            // Set relative position
            generalGenPosCalc(model, command);

        } else if (command instanceof TransitionEntityChildCommand) {

            //
            // Modify the position of the command, which is relative to the
            // wall segment.
            //
            double[] position = new double[3];
            ((TransitionEntityChildCommand)command).getEndPosition(position);

            if (!command.isTransient()) {

            	Entity currentParentEntity =
            		((TransitionEntityChildCommand)
            				command).getEndParentEntity();

	            // If the end parent set in the non transient command is not the
	            // same as the most appropriate parent, then we have to update
            	// the command's position, rotation and parent. We have to
            	// update the position relative to the new parent. We have to
            	// handle the rotation relative to the parent which is to
            	// negate the angle of rotation applied to the parent and
            	// apply it to the child. Lastly we have to set the new parent.
	    		if (newParentEntity != currentParentEntity) {

	    			// Do position work
	    			position =
	    				TransformUtils.getPositionInSceneCoordinates(
	    						model, (PositionableEntity)entity, true);
	    			position =
	    				TransformUtils.
	    					convertSceneCoordinatesToLocalCoordinates(
	    						model,
	    						position,
	    						(PositionableEntity)newParentEntity,
	    						true);

	    			float[] bounds = new float[6];
	    			((PositionableEntity)entity).getBounds(bounds);
	    			position[2] = -bounds[0];

	    			((TransitionEntityChildCommand)command).setEndPosition(
	    					position);

	    			// Do rotation work if the parent is not a zone
	    			if (!newParentEntity.isZone()) {
	                    
	                    float[] rotation =
	                        TransformUtils.getExactRotation(
	                                (PositionableEntity)newParentEntity);

	                    rotation[3] = -rotation[3];

	                    ((TransitionEntityChildCommand)command).setCurrentRotation(
	                            rotation[0], rotation[1], rotation[2], rotation[3]);

	    			}

	    			// Do parent work
	    			((TransitionEntityChildCommand)
		    				command).setEndParentEntity(newParentEntity);
	    		}


            }

            // Calculate the resulting depth change that should occur based on
            // the newParentEntity.
    		position[2] = position[2] +
    			calculateDepthChanges(
    					command,
    					model,
    					entity,
    					newParentEntity);

            // Catch the special door and window case
            handleDoorAndWindowCase(entity, position, command.isTransient());

            // Catch the auto-span case
            handleAutoSpanCase(entity, position, command.isTransient());

            ((TransitionEntityChildCommand)command).setEndPosition(
                    position);

            if (!command.isTransient()) {
            	// Set relative position
            	generalGenPosCalc(model, command);
            }

        }

        result.setResult(true);
        return(result);
    }


    //--------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------

}
