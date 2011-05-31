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
import javax.vecmath.Vector3d;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if movement uses incremental special case snaps.
 * If so, it updates the position in the command for future checking.
 *
 * @author Ben Yarger
 * @version $Revision: 1.38 $
 */
class MovementUsesIncrementalSpecialCaseSnapsRule extends BaseRule{

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementUsesIncrementalSpecialCaseSnapsRule(
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

		// Check if entity is using incremental snaps, return false if not
		Boolean usesIncrementalSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MOVEMENT_USES_INCREMENTAL_SNAPS_PROP);

		if(usesIncrementalSnaps == null ||
				usesIncrementalSnaps.booleanValue() == false){

            result.setResult(false);
            return(result);
		}

		// If we are not colliding with another model exit
		if(!collidingWithModels(model, command, entity)){

            result.setResult(false);
            return(result);
		}

		// Get the incremental snap values
		Float xAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_X_AXIS_SNAP_PROP);

		Float yAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Y_AXIS_SNAP_PROP);

		Float zAxisSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Z_AXIS_SNAP_PROP);

		// Get the incremental snap special case values
		Float xAxisSpecialCaseSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_X_AXIS_SPECIAL_CASE_SNAP_PROP);

		Float yAxisSpecialCaseSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Y_AXIS_SPECIAL_CASE_SNAP_PROP);

		Float zAxisSpecialCaseSnapf = (Float)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_INCREMENTAL_Z_AXIS_SPECIAL_CASE_SNAP_PROP);

		//-----------------------------------------------------------
		// Apply snaps
		//-----------------------------------------------------------
		if(xAxisSpecialCaseSnapf != null || yAxisSpecialCaseSnapf != null || zAxisSpecialCaseSnapf != null){

			double[] newPosition = new double[] {0.0, 0.0, 0.0};
			double[] incPosition = new double[] {0.0, 0.0, 0.0};
			double[] startPosition = new double[] {0.0, 0.0, 0.0};

			// Perform operations depending on if command is transient
			if(command instanceof MoveEntityCommand){

				((MoveEntityCommand)command).getEndPosition(newPosition);
				((MoveEntityCommand)command).getEndPosition(incPosition);
				((MoveEntityCommand)command).getStartPosition(startPosition);

				double[] tmpPos = getPastPosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((MoveEntityCommand)command).setEndPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

		            result.setResult(true);
		            return(result);
				}

				tmpPos = getBeforePosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((MoveEntityCommand)command).setEndPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

		            result.setResult(true);
		            return(result);
				}

				((MoveEntityCommand)command).setEndPosition(incPosition);

			} else if (command instanceof TransitionEntityChildCommand){

				((TransitionEntityChildCommand)command).getEndPosition(newPosition);
				((TransitionEntityChildCommand)command).getEndPosition(incPosition);
				((TransitionEntityChildCommand)command).getStartPosition(startPosition);


			    //if there is collision with a model
                //We want the z value to be the original z value,
                //instead of trying to stack


                if(collidingWithModels(model, command, entity)){

                    newPosition[2] = startPosition[2];
                    incPosition[2] = startPosition[2];
                }

				double[] tmpPos = getPastPosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((TransitionEntityChildCommand)command).setEndPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

		            result.setResult(true);
		            return(result);
				}

				tmpPos = getBeforePosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((TransitionEntityChildCommand)command).setEndPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
                }

				((TransitionEntityChildCommand)command).setEndPosition(incPosition);

			} else if (command instanceof MoveEntityTransientCommand){

				((MoveEntityTransientCommand)command).getPosition(newPosition);
				((MoveEntityTransientCommand)command).getPosition(incPosition);
                ((PositionableEntity)entity).getStartingPosition(startPosition);
				//if there is collision with a model
				//We want the z value to be the original z value,
				//instead of trying to stack


                if(collidingWithModels(model, command, entity)){

                    newPosition[2] = startPosition[2];
                    incPosition[2] = startPosition[2];
                }


                double[] tmpPos = getPastPosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((MoveEntityTransientCommand)command).setPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
                }

				tmpPos = getBeforePosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((MoveEntityTransientCommand)command).setPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				((MoveEntityTransientCommand)command).setPosition(incPosition);

			} else if (command instanceof AddEntityCommand){

				((AddEntityCommand)command).getPosition(newPosition);
				((AddEntityCommand)command).getPosition(incPosition);
				((AddEntityCommand)command).getPosition(startPosition);

				double[] tmpPos = getPastPosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((AddEntityCommand)command).setPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				tmpPos = getBeforePosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				((AddEntityCommand)command).setPosition(tmpPos);

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				((AddEntityCommand)command).setPosition(newPosition);

			} else if (command instanceof AddEntityChildCommand){

				PositionableEntity childEntity = (PositionableEntity)
					((AddEntityChildCommand)command).getEntity();

				childEntity.getPosition(newPosition);
				childEntity.getPosition(incPosition);
				childEntity.getPosition(startPosition);

				double[] tmpPos = getPastPosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				childEntity.setPosition(tmpPos, command.isTransient());

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				tmpPos = getBeforePosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				childEntity.setPosition(tmpPos, command.isTransient());;

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				childEntity.setPosition(newPosition, command.isTransient());

			} else if(command instanceof AddEntityChildTransientCommand){

				PositionableEntity childEntity = (PositionableEntity)
					((AddEntityChildTransientCommand)command).getEntity();

				childEntity.getPosition(newPosition);
				childEntity.getPosition(incPosition);
				childEntity.getPosition(startPosition);

				double[] tmpPos = getPastPosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				childEntity.setPosition(tmpPos, command.isTransient());

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				tmpPos = getBeforePosition(
						newPosition,
						startPosition,
						xAxisSnapf,
						yAxisSnapf,
						zAxisSnapf,
						xAxisSpecialCaseSnapf,
						yAxisSpecialCaseSnapf,
						zAxisSpecialCaseSnapf);

				childEntity.setPosition(tmpPos, command.isTransient());;

				if(!collidingWithModels(model, command, entity)){

                    result.setResult(true);
                    return(result);
				}

				childEntity.setPosition(newPosition, command.isTransient());
			}
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Get the special case increment position after crossing the object.
	 *
	 * @param newPosition
	 * @param startPosition
	 * @param xAxisSnapf
	 * @param yAxisSnapf
	 * @param zAxisSnapf
	 * @param xAxisSpecialSnapf
	 * @param yAxisSpecialSnapf
	 * @param zAxisSpecialSnapf
	 * @return
	 */
	private double[] getPastPosition(
			double[] newPosition,
			double[] startPosition,
			Float xAxisSnapf,
			Float yAxisSnapf,
			Float zAxisSnapf,
			Float xAxisSpecialSnapf,
			Float yAxisSpecialSnapf,
			Float zAxisSpecialSnapf){

		Vector3d transVec = new Vector3d(
				newPosition[0] - startPosition[0],
				newPosition[1] - startPosition[1],
				newPosition[2] - startPosition[2]);

		double[] pastPosition = new double[3];
		pastPosition[0] = newPosition[0];
		pastPosition[1] = newPosition[1];
		pastPosition[2] = newPosition[2];

		if(xAxisSpecialSnapf != null &&
				xAxisSnapf != null){

			if(transVec.x < 0){

				pastPosition[0] =
					pastPosition[0] - (xAxisSpecialSnapf - xAxisSnapf);

			} else if (transVec.x > 0){

				pastPosition[0] =
					pastPosition[0] + (xAxisSpecialSnapf - xAxisSnapf);

			}

		}

		if(yAxisSpecialSnapf != null &&
				yAxisSnapf != null){

			if(transVec.y < 0){

				pastPosition[1] =
					pastPosition[1] - (yAxisSpecialSnapf - yAxisSnapf);

			} else if (transVec.y > 0){

				pastPosition[1] =
					pastPosition[1] + (yAxisSpecialSnapf - yAxisSnapf);

			}

		}

		if(zAxisSpecialSnapf != null &&
				zAxisSnapf != null){

			if(transVec.z < 0){

				pastPosition[2] =
					pastPosition[2] - (zAxisSpecialSnapf - zAxisSnapf);

			} else if (transVec.z > 0){

				pastPosition[2] =
					pastPosition[2] + (zAxisSpecialSnapf - zAxisSnapf);

			}

		}

		return pastPosition;
	}

	/**
	 * Get the special case increment position before crossing the object.
	 *
	 * @param newPosition
	 * @param startPosition
	 * @param xAxisSnapf
	 * @param yAxisSnapf
	 * @param zAxisSnapf
	 * @param xAxisSpecialSnapf
	 * @param yAxisSpecialSnapf
	 * @param zAxisSpecialSnapf
	 * @return
	 */
	private double[] getBeforePosition(
			double[] newPosition,
			double[] startPosition,
			Float xAxisSnapf,
			Float yAxisSnapf,
			Float zAxisSnapf,
			Float xAxisSpecialSnapf,
			Float yAxisSpecialSnapf,
			Float zAxisSpecialSnapf){

		Vector3d transVec = new Vector3d(
				newPosition[0] - startPosition[0],
				newPosition[1] - startPosition[1],
				newPosition[2] - startPosition[2]);

		double[] beforePosition = new double[3];
		beforePosition[0] = newPosition[0];
		beforePosition[1] = newPosition[1];
		beforePosition[2] = newPosition[2];

		if(xAxisSpecialSnapf != null &&
				xAxisSnapf != null){

			if(transVec.x < 0){

				beforePosition[0] =
					beforePosition[0] + (xAxisSpecialSnapf - xAxisSnapf);

			} else if (transVec.x > 0){

				beforePosition[0] =
					beforePosition[0] - (xAxisSpecialSnapf - xAxisSnapf);

			}

		}

		if(yAxisSpecialSnapf != null &&
				yAxisSnapf != null){

			if(transVec.y < 0){

				beforePosition[1] =
					beforePosition[1] + (yAxisSpecialSnapf - yAxisSnapf);

			} else if (transVec.y > 0){

				beforePosition[1] =
					beforePosition[1] - (yAxisSpecialSnapf - yAxisSnapf);

			}

		}

		if(zAxisSpecialSnapf != null &&
				zAxisSnapf != null){

			if(transVec.z < 0){

				beforePosition[2] =
					beforePosition[2] + (zAxisSpecialSnapf - zAxisSnapf);

			} else if (transVec.z > 0){

				beforePosition[2] =
					beforePosition[2] - (zAxisSpecialSnapf - zAxisSnapf);

			}

		}

		return beforePosition;
	}

	/**
	 * Process current command to see if collision with non-relationship models
	 * occurs.
	 *
	 * @param command Command to test
	 * @return True if colliding with models, false otherwise
	 */
	private boolean collidingWithModels(
			WorldModel model,
			Command command,
			Entity entity){

		/*
		 * Perform collision check - now that we have updated the positions,
		 * see if this new position causes a collision.
		 */
		rch.performCollisionCheck(command, true, false, false);

		if(rch.collisionEntities == null){

			return false;
		}

		// Extract the relationship data
		String[] classRelationship = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);

		int[] relationshipAmount = (int[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP);

		Enum[] relModifier = (Enum[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP);

		String[] colReplaceClass = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.REPLACE_PROD_CLASS_PROP);

		/*
		 * If any of these are null, check collision entities. If there are
		 * no collisions, return false, otherwise true.
		 */
		if(classRelationship == null ||
				relationshipAmount == null ||
				relModifier == null){

			if(rch.collisionEntities.size() > 0){
				return true;
			} else {
				return false;
			}
		}

		// Maximum index limit for loops
		int maxIndex = Math.min(
				(Math.min(classRelationship.length, relationshipAmount.length)),
				relModifier.length);

		rch.performCollisionAnalysis(
				classRelationship,
				null,
				entity.getChildren(),
				colReplaceClass,
				maxIndex,
				null,
				false,
				null);

		/*
		 * If we are colliding with legal walls and floors, then permit.
		 */
		if(!rch.hasIllegalCollisions(classRelationship, relationshipAmount, relModifier)){

			return false;
		} else {
			return true;
		}
	}
}
