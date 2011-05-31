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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Looks at the results of the last perform collision analysis and based
 * on the existence of entities in replaceEntityMatches issues remove
 * commands then sends back through the original command.
 *
 * @author Ben Yarger
 * @version $Revision: 1.29 $
 */
public class ReplaceEntityRule extends BaseRule  {

	/** Status message for status bar */
	private static final String STATUS_BAR_MSG =
		"org.chefx3d.rules.definitions.ReplaceEntityRule.statusMessage";

	/** Status message for pop up message */
	private static final String POP_UP_MSG =
		"org.chefx3d.rules.definitions.ReplaceEntityRule.popUpMessage";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ReplaceEntityRule(
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

		// If any of these are null, do not proceed with collision check
		if(classRelationship == null ||
				relationshipAmount == null ||
				relModifier == null){

            result.setResult(true);
            return(result);
		}

		// Maximum index limit for loops
		int maxIndex = Math.min(
				(Math.min(classRelationship.length, relationshipAmount.length)),
				relModifier.length);

		// Perform collision check - initial point at which a check should
		// be performed for movement rules.
		rch.performCollisionCheck(command, true, false, false);

		// Analyze class relationship data against collisions
		// (first time needs to be performed)
		rch.performCollisionAnalysis(
				classRelationship,
				null,
				null,
				colReplaceClass,
				maxIndex,
				null,
				false,
				null);

		ArrayList<Entity> replaceEntityMatches =
			collisionResults.getReplaceEntityMatches();

		if(replaceEntityMatches.size() > 0){

			// If command is transient, just update the status bar and exit
			// otherwise, prompt to replace. If canceled, reset command back
			// to last known good.
			if(command.isTransient()){

				String msg = intl_mgr.getString(STATUS_BAR_MSG);
				statusBar.setMessage(msg);

				if (command instanceof MoveEntityTransientCommand) {

				    float maxDepthBuffer = 0;

				    // look through all the items to be replaced and get
				    // the largest depth buffer adjustment
                    for(int i = 0; i < replaceEntityMatches.size(); i++){

                        Entity childEntity = replaceEntityMatches.get(i);

                        Float depthBuffer = (Float)
                        	RulePropertyAccessor.getRulePropertyValue(
                        			childEntity,
                        			ChefX3DRuleProperties.CENTER_DEPTH_POS_BUFF_PROP);

                        if (depthBuffer != null && depthBuffer > maxDepthBuffer) {
                            maxDepthBuffer = depthBuffer;
                        }

                    }

				    double[] pos = new double[3];
				    ((MoveEntityTransientCommand)command).getPosition(pos);

                    // adjust the item by the max depth buffer found
					///////////////////////////////////////////////////////////
					// rem: think this should be plus instead of minus,
					// like the SetRelativePositionRule's usage
                    //pos[2] -= maxDepthBuffer;
					pos[2] += maxDepthBuffer;
					///////////////////////////////////////////////////////////

                    ((MoveEntityTransientCommand)command).setPosition(pos);

				}

			} else {

				String msg = intl_mgr.getString(POP_UP_MSG);

				if(popUpConfirm.showMessage(msg)){

					// Issue all of the new remove commands resulting from the
					// replace operation
					for(int i = 0; i < replaceEntityMatches.size(); i++){

						Entity childEntity = replaceEntityMatches.get(i);

						int parentEntityID = childEntity.getParentEntityID();
						Entity parentEntity = model.getEntity(parentEntityID);

						if(childEntity != null && parentEntity != null){

							SceneManagementUtility.removeChild(
									model, 
									collisionChecker, 
									childEntity, 
									false);
						}
					}

					// Perform special case command handling for reparenting
					if(command instanceof AddEntityChildCommand ||
							command instanceof AddEntityChildTransientCommand){

						Entity origParentEntity =
							((AddEntityChildCommand)command).getParentEntity();

						// If the replaceEntityMatches contains the parent that
						// the new entity is to be a child of, get the parent
						// of that parent and set that as the new parent of the
						// new add command.
						if(replaceEntityMatches.contains(origParentEntity)){

							// Get the new parent entity to set
							int newParentId =
								origParentEntity.getParentEntityID();

							Entity newParentEntity =
								model.getEntity(newParentId);

							((AddEntityChildCommand)command).setParentEntity(
									newParentEntity);

							// Correct the position
							double[] originalParentPos = new double[3];
							((PositionableEntity)origParentEntity).getPosition(
									originalParentPos);

							double[] entityPos = new double[3];
							((PositionableEntity)entity).getPosition(
									entityPos);

							entityPos[0] = originalParentPos[0] + entityPos[0];
							entityPos[1] = originalParentPos[1] + entityPos[1];
							entityPos[2] = originalParentPos[2];

							((PositionableEntity)entity).setPosition(
									entityPos,
									false);

						}

					} else if (command instanceof TransitionEntityChildCommand) {

						Entity origParentEntity =
							((TransitionEntityChildCommand)command).
							getEndParentEntity();

						// If the replaceEntityMatches contains the parent that
						// the new entity is to be a child of, get the parent
						// of that parent and set that as the new parent of the
						// transition command.
						if(replaceEntityMatches.contains(origParentEntity)){

							// Get the new parent entity to set
							int newParentId =
								origParentEntity.getParentEntityID();

							Entity newParentEntity =
								model.getEntity(newParentId);

							TransitionEntityChildCommand tranCmd =
								(TransitionEntityChildCommand) command;

							tranCmd.setEndParentEntity(newParentEntity);

							// Correct the position
							double[] originalParentPos = new double[3];
							((PositionableEntity)origParentEntity).getPosition(
									originalParentPos);

							double[] entityPos = new double[3];
							tranCmd.getEndPosition(entityPos);

							entityPos[0] = originalParentPos[0] + entityPos[0];
							entityPos[1] = originalParentPos[1] + entityPos[1];
							entityPos[2] = originalParentPos[2];

							tranCmd.setEndPosition(entityPos);
						}
					}

		            result.setResult(true);
		            return(result);

				} else {
					// Reset to last known good
					if(command instanceof AddEntityCommand ||
							command instanceof AddEntityChildCommand ||
							command instanceof AddEntityChildTransientCommand ||
							command instanceof TransitionEntityChildCommand){

					    result.setApproved(false);
			            result.setResult(false);
			            return(result);
					}
				}
			}
		}

        result.setResult(true);
        return(result);
	}
}
