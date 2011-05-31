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

// External Imports

// Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Handles initial position corrections for add commands.
 *
 * @author Ben Yarger
 * @version $Revision: 1.37 $
 */
public class InitialAddPositionCorrectionRule extends
		InitialPositionCorrectionRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public InitialAddPositionCorrectionRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;

    }

	/**
	 * Entry point for other rules to call that will bypass any ignore rule
	 * restriction and evaluate the rule.
	 *
	 * @param entity Entity to evaluate
	 * @param model WorldModel to reference
	 * @param command Command acting on the entity
	 * @return True if updated, false otherwise
	 */
	/*
	public boolean outsideRuleCall(
			Entity entity,
			WorldModel model,
			Command command) {

		RuleEvaluationResult eval = performCheck(entity, command, result);
		return(eval.getResult());
	}
	*/

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

        //this.result = result;

	    // Only operate on PositionableEntities of type MODEL
    	// variants
        if (!(entity instanceof PositionableEntity) ||
			(!entity.isModel())) {
            result.setResult(true);
            return(result);
        }

		Entity currentParentEntity = null;

        // Only operate on add commands
		if (command instanceof AddEntityChildCommand) {

			currentParentEntity =
				((AddEntityChildCommand)command).getParentEntity();

		} else if (command instanceof AddEntityChildTransientCommand) {

			currentParentEntity =
				((AddEntityChildTransientCommand)command).getParentEntity();
		} else {
            result.setResult(true);
            return(result);
		}

		if (currentParentEntity == null) {
			// punt if the parenting is ambiguous
            result.setResult(true);
            return(result);
		}

		double[] position = new double[3];
		PositionableEntity pEntity = (PositionableEntity) entity;
		pEntity.getPosition(position);

		// In case we need it, side pocket the current parent entity id
		entity.setProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.INITAL_ADD_PARENT,
				currentParentEntity.getEntityID(),
				false);

		// Find the most appropriate entity either in immediate collision
        // or somewhere behind. Find closest entity.
		Entity newParentEntity = rch.findAppropriateParent(command);

		// If the new parent is null, simply return true
		if (newParentEntity == null) {
            result.setResult(true);
            return(result);
		}

		// If the end parent set in the non transient command is not the
        // same as the most appropriate parent, make sure we set the depth
        // offset to be correct for the most appropriate parent. In this
        // case we are looking for the back bounds of the child to be
        // overlapping with the front bounds of the parent.
		if (newParentEntity != currentParentEntity) {

			// Do position work
			position =
				TransformUtils.getPositionInSceneCoordinates(
						model, pEntity, true);
			position =
				TransformUtils.
					convertSceneCoordinatesToLocalCoordinates(
						model,
						position,
						(PositionableEntity)newParentEntity,
						true);
			
			if (position == null) {
				result.setResult(true);
	            return(result);
			}
/*
			float[] bounds = new float[6];
			pEntity.getBounds(bounds);
			position[2] = -bounds[0];
*/
			pEntity.setPosition(position, false);

			// Setup the command for further evaluation
			if (command instanceof AddEntityChildCommand) {

				((AddEntityChildCommand)command).setParentEntity(
						newParentEntity);

			} else if (command instanceof AddEntityChildTransientCommand) {

				((AddEntityChildTransientCommand)command).setParentEntity(
						newParentEntity);

			}

			// Convert the starting position value as well.
			// The starting position of an entity being added is the last
			// position the ghost version of the entity occupied before
			// being removed.
/*			double[] startingPosition = new double[3];
			pEntity.getStartingPosition(startingPosition);

			startingPosition =
				TransformUtils.convertLocalCoordinatesToSceneCoordinates(
						model, pEntity, startingPosition, true);

			startingPosition =
				TransformUtils.
					convertSceneCoordinatesToLocalCoordinates(
						model,
						startingPosition,
						(PositionableEntity)newParentEntity,
						true);

			pEntity.setStartingPosition(startingPosition);
			*/
		}

		// Calculate the resulting depth change that should occur based on the
		// newParentEntity.
		position[2] += calculateDepthChanges(
					command,
					model,
					entity,
					newParentEntity);

		// Catch the special door and window case
		handleDoorAndWindowCase(entity, position, command.isTransient());

        // Catch the auto-span case
        handleAutoSpanCase(entity, position, command.isTransient());

		// Side pocket the new parent's ID
		entity.setProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.INITAL_ADD_PARENT,
				newParentEntity.getEntityID(),
				false);

		// Apply results to appropriate command
		if (command instanceof AddEntityChildCommand) {

			((AddEntityChildCommand)command).setParentEntity(
					newParentEntity);
			((PositionableEntity)entity).setPosition(position, false);

		} else if (command instanceof AddEntityChildTransientCommand) {

			((AddEntityChildTransientCommand)command).setParentEntity(
					newParentEntity);
			((PositionableEntity)entity).setPosition(position, false);
		}

		// Apply results of set relative position to appropriate command
		((PositionableEntity)entity).setPosition(position, false);

		// Set relative position
        generalGenPosCalc(model, command);

        result.setResult(true);
        return(result);
	}
}
