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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Replaces z position value with whatever is stored in rule value.
 * This z value is a zone relative value, so the position specified
 * will always be relative to the wall.
 *
 * @author Ben Yarger
 * @version $Revision: 1.17 $
 */
public class GlobalDepthOffsetRule extends BaseRule  {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public GlobalDepthOffsetRule(
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

		// Confirm use of PARENT_AS_COLLISION_ALTERNATE
		Float globalDepthOffset = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.GLOBAL_DEPTH_OFFSET);

		if(globalDepthOffset == null) {
            result.setResult(true);
            return(result);
		}

		// Make the adjustment
		if(command instanceof MoveEntityCommand) {

			double[] entityZonePos =
				TransformUtils.getPositionRelativeToZone(model, command);

			Entity parentEntity =
				model.getEntity(entity.getParentEntityID());

			double[] parentZonePos =
				TransformUtils.getPositionRelativeToZone(model, parentEntity);

			entityZonePos[2] = globalDepthOffset;

			double[] endPos = new double[3];
			endPos[0] = entityZonePos[0] - parentZonePos[0];
			endPos[1] = entityZonePos[1] - parentZonePos[1];
			endPos[2] = entityZonePos[2] - parentZonePos[2];

			((MoveEntityCommand)command).setEndPosition(endPos);

		} else if (command instanceof MoveEntityTransientCommand){


			double[] entityZonePos =
				TransformUtils.getPositionRelativeToZone(model, command);

			Entity parentEntity =
				model.getEntity(entity.getParentEntityID());

			double[] parentZonePos =
				TransformUtils.getPositionRelativeToZone(model, parentEntity);

			entityZonePos[2] = globalDepthOffset;

			double[] endPos = new double[3];
			endPos[0] = entityZonePos[0] - parentZonePos[0];
			endPos[1] = entityZonePos[1] - parentZonePos[1];
			endPos[2] = entityZonePos[2] - parentZonePos[2];

			((MoveEntityTransientCommand)command).setPosition(endPos);

		} else if (command instanceof TransitionEntityChildCommand) {

			TransitionEntityChildCommand tranCmd =
				(TransitionEntityChildCommand) command;

			double[] entityZonePos =
				TransformUtils.getPositionRelativeToZone(model, tranCmd);

			Entity parentEntity =
				tranCmd.getEndParentEntity();

			double[] parentZonePos =
				TransformUtils.getPositionRelativeToZone(model, parentEntity);

			entityZonePos[2] = globalDepthOffset;

			double[] endPos = new double[3];
			endPos[0] = entityZonePos[0] - parentZonePos[0];
			endPos[1] = entityZonePos[1] - parentZonePos[1];
			endPos[2] = entityZonePos[2] - parentZonePos[2];

			tranCmd.setEndPosition(endPos);

		} else if (command instanceof AddEntityChildCommand) {

			double[] zoneRelativePos =
				TransformUtils.getPositionRelativeToZone(model, command);

			if (zoneRelativePos != null) {

				double[] pos = new double[3];
				((PositionableEntity)entity).getPosition(pos);
				pos[2] += globalDepthOffset - zoneRelativePos[2];
				((PositionableEntity)entity).setPosition(pos, false);
			}

		} else if (command instanceof AddEntityChildTransientCommand) {

			double[] zoneRelativePos =
				TransformUtils.getPositionRelativeToZone(model, command);

			if (zoneRelativePos != null) {

				double[] pos = new double[3];
				((PositionableEntity)entity).getPosition(pos);
				pos[2] += globalDepthOffset - zoneRelativePos[2];
				((PositionableEntity)entity).setPosition(pos, false);
			}

		} else if (command instanceof AddEntityCommand) {

			double[] zoneRelativePos =
				TransformUtils.getPositionRelativeToZone(model, command);

			if (zoneRelativePos != null) {

				double[] pos = new double[3];
				((PositionableEntity)entity).getPosition(pos);
				pos[2] += globalDepthOffset - zoneRelativePos[2];
				((PositionableEntity)entity).setPosition(pos, false);
			}

		}

        result.setResult(true);
        return(result);
	}

}
