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

import javax.vecmath.Vector3d;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.RuleUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* If a SegmentEntity changes size, update the children of that SegmentEntity
* so that their relative position reflects the inverse of the transform
* applied. This will keep child objects in their real world position based on
* their relative transform. This is only required for movement of a given
* SegmentEntity starting VertexEntity. Because of the relative transforms
* used, only movement of the left VertexEntity causes movement of the child
* objects.
*
* @author Ben Yarger
* @version $Revision: 1.45 $
*/
public class UpdateSegmentChildrenRelativePositionRule extends BaseRule  {

	/** Confirmation pop up message */
	private static final String CONFIRM_MSG =
		"org.chefx3d.rules.definitions.UpdateSegmentChildrenRelativePositionRule.confirmRemoval";

	/**
	 * Stores the last non-transient command transaction ID to make sure final
	 * commands are not rechecked in an endless loop.
	 */
	private static int previousTransactionID = -1;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public UpdateSegmentChildrenRelativePositionRule(
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

		/*
		 * Only perform operation on VertexEntities or moving SegmentEntities
		 */
		if (command instanceof MoveVertexCommand ||
		        command instanceof MoveVertexTransientCommand) {

			VertexEntity vEntity = (VertexEntity)entity;

			int parentID = vEntity.getParentEntityID();
			SegmentableEntity segmentableEntity =
				(SegmentableEntity) model.getEntity(parentID);

	        // not sure why but sometimes this is null
            // return false for this frame and it should
            // reset to the correct state the next frame.
            if (segmentableEntity == null) {
                result.setResult(true);
                return(result);
            }

			ArrayList<SegmentEntity> segmentList =
				segmentableEntity.getSegments();

			ArrayList<Command> cmdList = new ArrayList<Command>();

			/*
			 * Update children of any SegmentEntity whose starting vertex
			 * is the one that moved.
			 *
			 * If we find that the moving vertex is a right hand vertex,
			 * perform delete operations as well.
			 */
			for(SegmentEntity segment : segmentList){

				if(segment.getStartVertexEntity().getEntityID() ==
					vEntity.getEntityID()){

				    updateChildrenRelativePositions(
						model,
						command,
						vEntity,
						segment.getEndVertexEntity(),
						segment,
						cmdList);
				} else if (segment.getEndVertexEntity().getEntityID() ==
					vEntity.getEntityID()) {

					deleteStrandedChildren(
							model,
							command,
							segment,
							cmdList);

				}
			}

			/*
			 * Display messages and give the user the opportunity to cancel
			 * the operation if they don't want to remove product from wall
			 */

			if(!(command.isTransient())){

			    boolean hasDeletes = false;
			    for (int i = 0; i < cmdList.size(); i++) {
			        if (cmdList.get(i) instanceof RemoveEntityChildCommand) {
			            hasDeletes = true;
			            break;
			        }
			    }

			    if (hasDeletes) {

		            String msg = intl_mgr.getString(CONFIRM_MSG);
		            if (!(popUpConfirm.showMessage(msg))) {

		                popUpConfirm.setDisplayPopUp(false);

	                    /*
	                     * If the user cancels the move vertex operation
	                     * because they do not want to remove any products
	                     * from the wall then we need to reset the position
	                     * of the vertex and revert the position
	                     * of all the children to their starting positions.
	                     */
	                    MoveVertexCommand mvVertexCmd =
	                        (MoveVertexCommand)command;

	                    double[] pos = new double[3];
	                    mvVertexCmd.getStartPosition(pos);
	                    mvVertexCmd.setEndPosition(pos);

	                    /*
	                     * Reset the position of all children either
	                     * by changing the position values of the related
	                     * MoveEntityCommands or replacing the
	                     * RemoveEntityChildCommands with MoveEntityCommands.
	                     * All new MoveEntityCommands will be added to the
	                     * newMvCmds list to be absorbed by the cmdList before
	                     * issuing the MultiCommand.
	                     */
	                    ArrayList<Command>  newMvCmds =
	                        new ArrayList<Command>();

	                    for(int w = cmdList.size()-1; w >= 0; w--){

	                        Command tmpCmd = cmdList.get(w);

	                        if(tmpCmd instanceof MoveEntityCommand){

	                            MoveEntityCommand mvCmd =
	                                (MoveEntityCommand) tmpCmd;

	                            double[] tmpPos = new double[3];
	                            mvCmd.getStartPosition(tmpPos);
	                            mvCmd.setEndPosition(tmpPos);

	                        } else if (tmpCmd instanceof
	                                RemoveEntityChildCommand){

	                            double[] tmpStartPos = new double[3];

	                            PositionableEntity pe =
	                                (PositionableEntity)
	                                ((RemoveEntityChildCommand)
	                                        tmpCmd).getEntity();

	                            pe.getStartingPosition(tmpStartPos);

	                            int transactionID = model.issueTransactionID();
	                            MoveEntityCommand moveCmd =
	                                new MoveEntityCommand(
	                                    model,
	                                    transactionID,
	                                    pe,
	                                    tmpStartPos,
	                                    tmpStartPos);

	                            newMvCmds.add(moveCmd);

	                            cmdList.remove(w);

	                        } else if (tmpCmd instanceof ScaleEntityCommand){

	                            result.setApproved(false);
	                            //((ScaleEntityCommand)tmpCmd).resetToStart();
	                            //newMvCmds.add(tmpCmd);

	                        }
	                    }

	                    addNewlyIssuedCommand(newMvCmds);

	                    result.setResult(false);
	                    return(result);
	                }
			    }
			}

			if(cmdList.size() > 0){
				if (!(command.getTransactionID() == previousTransactionID)) {

					if(command instanceof MoveVertexCommand){
						previousTransactionID = command.getTransactionID();
					}

					addNewlyIssuedCommand(cmdList);
			        result.setResult(true);
			        return(result);

				}
			}

		} else if (command instanceof MoveSegmentCommand ||
		        command instanceof MoveSegmentTransientCommand){


			SegmentEntity sEntity = (SegmentEntity)entity;

			VertexEntity mvSegStartVertexEntity =
				sEntity.getStartVertexEntity();

			VertexEntity mvSegEndVertexEntity =
				sEntity.getEndVertexEntity();

			int parentID = sEntity.getParentEntityID();
			SegmentableEntity segmentableEntity =
				(SegmentableEntity) model.getEntity(parentID);

			ArrayList<SegmentEntity> segmentList =
			    segmentableEntity.getSegments();

			ArrayList<Command> cmdList = new ArrayList<Command>();

			/*
			 * Update children of any SegmentEntity whose starting vertex
			 * is the one that moved.
			 *
			 * If we find that the moving vertex is a right hand vertex,
			 * perform delete operations as well.
			 */
			for(SegmentEntity segment : segmentList){

				if(segment.getEntityID() == sEntity.getEntityID()){
					continue;
				}

				if(segment.getStartVertexEntity().getEntityID() ==
					mvSegStartVertexEntity.getEntityID()){

				    updateChildrenRelativePositions(
						model,
						command,
						mvSegStartVertexEntity,
						segment.getEndVertexEntity(),
						segment,
						cmdList);

				} else if (segment.getEndVertexEntity().getEntityID() ==
					mvSegStartVertexEntity.getEntityID()) {

					deleteStrandedChildren(
							model,
							command,
							segment,
							cmdList);
				}

				if(segment.getStartVertexEntity().getEntityID() ==
					mvSegEndVertexEntity.getEntityID()){

				    updateChildrenRelativePositions(
						model,
						command,
						mvSegEndVertexEntity,
						segment.getEndVertexEntity(),
						segment,
						cmdList);
				} else if (segment.getEndVertexEntity().getEntityID() ==
					mvSegEndVertexEntity.getEntityID()) {

					deleteStrandedChildren(
							model,
							command,
							segment,
							cmdList);
				}
			}

			/*
			 * Display messages and give the user the opportunity to cancel
			 * the operation if they don't want to remove product from wall
			 */
			if (!(command.isTransient())) {

	            boolean hasDeletes = false;
	            for (int i = 0; i < cmdList.size(); i++) {
	                if (cmdList.get(i) instanceof RemoveEntityChildCommand) {
	                    hasDeletes = true;
	                    break;
	                }
	            }

	            if (hasDeletes) {

	                String msg = intl_mgr.getString(CONFIRM_MSG);
	                if(!(popUpConfirm.showMessage(msg))){
	                    /*
	                     * If the user cancels the move vertex operation
	                     * because they do not want to remove any products
	                     * from the wall then we need to reset the position
	                     * of the vertex and revert the position
	                     * of all the children to their starting positions.
	                     */
                        MoveSegmentCommand mvSegmentCmd =
                            (MoveSegmentCommand)command;

                        //mvSegmentCmd.resetToStart();

                        /*
                         * Reset the position of all children either
                         * by changing the position values of the related
                         * MoveEntityCommands or replacing the
                         * RemoveEntityChildCommands with MoveEntityCommands.
                         * All new MoveEntityCommands will be added to the
                         * newMvCmds list to be absorbed by the cmdList before
                         * issuing the MultiCommand.
                         */
                        ArrayList<Command> newMvCmds =
                            new ArrayList<Command>();

                        for(int w = cmdList.size()-1; w >= 0; w--){

                            Command tmpCmd = cmdList.get(w);

                            if (tmpCmd instanceof MoveEntityCommand) {

                                MoveEntityCommand mvCmd =
                                    (MoveEntityCommand) tmpCmd;

                                double[] tmpPos = new double[3];
                                mvCmd.getStartPosition(tmpPos);
                                mvCmd.setEndPosition(tmpPos);

                                newMvCmds.add(mvCmd);

                            } else if (tmpCmd instanceof RemoveEntityChildCommand){

                                double[] tmpStartPos = new double[3];

                                PositionableEntity pe =
                                    (PositionableEntity)
                                    ((RemoveEntityChildCommand)
                                            tmpCmd).getEntity();

                                pe.getStartingPosition(tmpStartPos);

                                int transactionID = model.issueTransactionID();
                                MoveEntityCommand moveCmd =
                                    new MoveEntityCommand(
                                        model,
                                        transactionID,
                                        pe,
                                        tmpStartPos,
                                        tmpStartPos);

                                newMvCmds.add(moveCmd);

                                cmdList.remove(w);

                            } else if (tmpCmd instanceof ScaleEntityCommand){

                                result.setApproved(false);
                                //((ScaleEntityCommand)tmpCmd).resetToStart();
                                //newMvCmds.add(tmpCmd);

                            }
                        }

                        addNewlyIssuedCommand(newMvCmds);

                        result.setResult(false);
                        return(result);
                    }

                    popUpConfirm.setDisplayPopUp(false);

                }
			}

			if(cmdList.size() > 0){
				if(!(command.getTransactionID() == previousTransactionID)){

					if(command instanceof MoveSegmentCommand){

						previousTransactionID = command.getTransactionID();
					}

					addNewlyIssuedCommand(cmdList);
			        result.setResult(true);
			        return(result);
				}
			}
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Update the position of children of the SegmentEntity.
	 *
	 * @param model
	 * @param command
	 * @param vertexEntity Vertex (presumably left) that is moving
	 * @param endVertexEntity Vertex (presumably right) that isn't moving
	 * @param segmentEntity
	 * @param cmdList ArrayList<Command> to add to
	 * @return True if remove commands authored, false otherwise
	 */
	private void updateChildrenRelativePositions(
			WorldModel model,
			Command command,
			VertexEntity vertexEntity,
			VertexEntity endVertexEntity,
			SegmentEntity segmentEntity,
			ArrayList<Command> cmdList){

		ArrayList<Entity> segmentChildren = segmentEntity.getChildren();

		/*
		 * Calculate the relative transform to apply to all children
		 */
		double[] leftVertexStartPos = new double[3];
		double[] leftVertexEndPos = new double[3];
		double[] rightVertexPos = new double[3];

		if(command instanceof MoveVertexCommand){

			vertexEntity.getStartingPosition(leftVertexStartPos);
			((MoveVertexCommand)command).getEndPosition(leftVertexEndPos);

		} else if (command instanceof MoveVertexTransientCommand){

			vertexEntity.getStartingPosition(leftVertexStartPos);
			((MoveVertexTransientCommand)command).getPosition(
					leftVertexEndPos);

		} else if (command instanceof MoveSegmentCommand){

			SegmentEntity movingSegment =
				(SegmentEntity) ((MoveSegmentCommand)command).getEntity();

			VertexEntity tmpStartVertex = movingSegment.getStartVertexEntity();
//			VertexEntity tmpEndVertex = movingSegment.getEndVertexEntity();

			if(vertexEntity.getEntityID() == tmpStartVertex.getEntityID()){

				((MoveSegmentCommand)command).getStartVertexStartPosition(
						leftVertexStartPos);

				((MoveSegmentCommand)command).getStartVertexEndPosition(
						leftVertexEndPos);

			} else {

				((MoveSegmentCommand)command).getEndVertexStartPosition(
						leftVertexStartPos);

				((MoveSegmentCommand)command).getEndVertexEndPosition(
						leftVertexEndPos);

			}

		} else if (command instanceof MoveSegmentTransientCommand){

			SegmentEntity movingSegment =
				(SegmentEntity)
				((MoveSegmentTransientCommand)command).getEntity();

			VertexEntity tmpStartVertex = movingSegment.getStartVertexEntity();
//			VertexEntity tmpEndVertex = movingSegment.getEndVertexEntity();

			vertexEntity.getStartingPosition(leftVertexStartPos);

			if(vertexEntity.getEntityID() == tmpStartVertex.getEntityID()){

				((MoveSegmentTransientCommand)
						command).getStartVertexEndPosition(leftVertexEndPos);

			} else {

				((MoveSegmentTransientCommand)
						command).getEndVertexEndPosition(leftVertexEndPos);

			}

		}

		endVertexEntity.getPosition(rightVertexPos);

		/*
		 * Determine last good full wall distance (x axis difference) and
		 * subtract the x axis transform of the entity to determine the
		 * offset from the fixed right vector.
		 */
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

		/*
		 * Process accordingly for transient and non-transient commands
		 *
		 * For transient commands, just move the object back the inverse
		 * translation amount.
		 *
		 * For non-transient commands, generate a MultiCommand of
		 * MoveEntityCommand for each object so that it can be undone.
		 * If the result of the updated position no longer has the product
		 * on the wall, delete it.
		 */
		if(command.isTransient()){

			for(Entity entity : segmentChildren){

				if(entity instanceof BasePositionableEntity){

					double[] pos = new double[3];
					((BasePositionableEntity)entity).getStartingPosition(pos);

					double rightHandOffset = originalWallLength - pos[0];
					double xAxisTransform = newWallLength - rightHandOffset;

					pos[0] = xAxisTransform;

					((BasePositionableEntity)entity).setPosition(
							pos,
							false);

 				}
			}
		} else {  // non-transient commands:

			for(Entity entity : segmentChildren){

				if(entity instanceof BasePositionableEntity){

					double[] pos = new double[3];
					double[] startPos = new double[3];
					float[] rotation = new float[4];

					((PositionableEntity)entity).getRotation(rotation);
					((BasePositionableEntity)entity).getStartingPosition(pos);
					((BasePositionableEntity)entity).getStartingPosition(
							startPos);

					double rightHandOffset = originalWallLength - pos[0];
					double xAxisTransform = newWallLength - rightHandOffset;

					// we need to adjust this slightly so that when the vectors
					// are created in the performSegmentBoundsCheck we get a
					// good result.
					pos[0] = xAxisTransform - 0.000001;

	                ((BasePositionableEntity)entity).setPosition(
	                        pos,
	                        command.isTransient());

					if(!BoundsUtils.performSegmentBoundsCheck(
							model,
							(PositionableEntity) entity,
							null,
							null,
							newWallLength)){

						if(!scaleProductWidthIntoBounds(
								model,
								cmdList,
								segmentEntity,
								entity,
								command,
								newWallLength)){

							// Exclude auto span special cases
							Boolean autoSpan = (Boolean)
								RulePropertyAccessor.getRulePropertyValue(
									entity,
									ChefX3DRuleProperties.SPAN_OBJECT_PROP);

							if(autoSpan != null && autoSpan == true){
								continue;
							}

							// Exclude kits and templates
							if (RuleUtils.isKitOrTemplate(entity)) {
								continue;
							}

							RemoveEntityChildCommand delCmd =
								new RemoveEntityChildCommand(
										model,
										segmentEntity,
										entity);

							cmdList.add(delCmd);

						}

					} else {

						int transactionID = model.issueTransactionID();
						MoveEntityCommand moveCmd = new MoveEntityCommand(
								model,
								transactionID,
								(PositionableEntity)entity,
								pos,
								startPos);

						cmdList.add(moveCmd);
					}
				}
			}
		}

	}

	/**
	 * Deletes stranded children, typically for a right hand vertex translation
	 * that doesn't require adjusting the children positions first before
	 * evaluating.
	 *
	 * @param model
	 * @param command
	 * @param segmentEntity
	 * @param cmdList
	 * @return True if commands authored, false otherwise
	 */
	private void deleteStrandedChildren(
			WorldModel model,
			Command command,
			SegmentEntity segmentEntity,
			ArrayList<Command> cmdList){

        double[] leftVertexEndPos = new double[3];
        double[] rightVertexPos = new double[3];
        Double newWallLength = null;
		if (command instanceof MoveSegmentCommand){

		    segmentEntity.getStartVertexEntity().getPosition(leftVertexEndPos);

           ((MoveSegmentCommand)command).getStartVertexEndPosition(
                       rightVertexPos);
           /*
            * Determine last good full wall distance (x axis difference) and
            * subtract the x axis transform of the entity to determine the
            * offset from the fixed right vector.
            */

           Vector3d wallAdjustedVector = new Vector3d(
                   leftVertexEndPos[0] - rightVertexPos[0],
                   leftVertexEndPos[1] - rightVertexPos[1],
                   0.0);

           newWallLength = wallAdjustedVector.length();
        }


		/*
		 * Only perform operation if issuing command is not transient
		 */
		if(!command.isTransient()){

			ArrayList<Entity> segmentChildren = segmentEntity.getChildren();

			for(Entity entity : segmentChildren){

				// Ignore kits and template entities
				if (RuleUtils.isKitOrTemplate(entity)) {
					continue;
				}

				float[] rotation = new float[4];

				((PositionableEntity)entity).getRotation(rotation);
				if(!BoundsUtils.performSegmentBoundsCheck(
						model,
						(PositionableEntity) entity,
						null,
						null,
						newWallLength)){
					if(!scaleProductWidthIntoBounds(
							model,
							cmdList,
							segmentEntity,
							entity,
							command,
							newWallLength)){
						Boolean autoSpan = (Boolean)
							RulePropertyAccessor.getRulePropertyValue(
								entity,
								ChefX3DRuleProperties.SPAN_OBJECT_PROP);

						if(autoSpan != null && autoSpan == true){
							continue;
						}

						RemoveEntityChildCommand delCmd =
							new RemoveEntityChildCommand(
									model,
									segmentEntity,
									entity);

						cmdList.add(delCmd);
					}

				}
			}
		}
	}

	/**
	 * Generate scale commands for products that can be scaled back to fit
	 * on the wall.
	 *
	 * @param model WorldModel
	 * @param cmdList ArrayList<Command> to add commands to
	 * @param segmentEntity SegmentableEntity being adjusted
	 * @param entity Entity to try to fit on wall
	 * @param command Command causing the vertex movement
	 * @return True if scale was added to cmdList, false otherwise
	 */
	private boolean scaleProductWidthIntoBounds(
			WorldModel model,
			ArrayList<Command> cmdList,
			SegmentEntity segmentEntity,
			Entity entity,
			Command command,
			Double newWallLength){

		// Don't bother with transient commands
		if(command.isTransient()){
			return false;
		}

		/*
		 * If entity can't scale, no point in doing any more work
		 */
		Boolean isEditable = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.IS_EDITABLE_PROP);

		Boolean canScale = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.CAN_SCALE_PROP);

		if(isEditable == null ||
				isEditable.booleanValue() != true ||
				canScale == null ||
				canScale.booleanValue() != true){

			return false;
		}

		ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES scaleRestriction =
			(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES)
				RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.SCALE_RESTRICTION_PROP);

		ArrayList<ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES>
			legalScaleDirections =
			new ArrayList<ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES>();

		legalScaleDirections.add(
				ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.XAXIS);

		legalScaleDirections.add(
				ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.XYPLANE);

		legalScaleDirections.add(
				ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.XZPLANE);

		legalScaleDirections.add(
				ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.UNIFORM);

		legalScaleDirections.add(
				ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.NONE);

		if(!legalScaleDirections.contains(scaleRestriction)){
			return false;
		}

		// Get vertex data
		VertexEntity leftVertex = segmentEntity.getStartVertexEntity();
		VertexEntity rightVertex = segmentEntity.getEndVertexEntity();

		double[] leftVertexPosition = new double[3];
		double[] rightVertexPosition = new double[3];

		leftVertex.getStartingPosition(leftVertexPosition);
		rightVertex.getStartingPosition(rightVertexPosition);

		Vector3d tmpVec = new Vector3d(
				rightVertexPosition[0] - leftVertexPosition[0],
				rightVertexPosition[1] - leftVertexPosition[1],
				0.0);

		leftVertexPosition[0] = 0.0;
		leftVertexPosition[1] = 1.0;
		leftVertexPosition[2] = 0.0;

		rightVertexPosition[0] = tmpVec.length();
		rightVertexPosition[1] = 1.0;
		rightVertexPosition[2] = 0.0;

		if(newWallLength != null ) {
		    rightVertexPosition[0] = newWallLength;
		}

		// Provides relative position correction for left vertex movement
		double finalXAxisOffset = 0;

		/*
		 * Now adjust the vertex data according to the command. Since we
		 * know we will only be hitting this method if there is a bounds issue
		 * we know that the command is causing the wall to get smaller. So,
		 * apply the distance offset according to the vertex moved.
		 */
		if(command instanceof MoveVertexCommand){
			VertexEntity ve =
				(VertexEntity) ((MoveVertexCommand)command).getEntity();

			double[] tmpVertexStartPos = new double[3];
			double[] tmpVertexEndPos = new double[3];

			((MoveVertexCommand)command).getStartPosition(tmpVertexStartPos);
			((MoveVertexCommand)command).getEndPosition(tmpVertexEndPos);

			Vector3d tmpMvVertexCmdVec = new Vector3d(
					tmpVertexEndPos[0] - tmpVertexStartPos[0],
					tmpVertexEndPos[1] - tmpVertexStartPos[1],
					0.0);

			// Correct offset
			if(ve.getEntityID() == leftVertex.getEntityID()){

				leftVertexPosition[0] =
					leftVertexPosition[0] + tmpMvVertexCmdVec.length();

				finalXAxisOffset = -tmpMvVertexCmdVec.length();

			} else {

				rightVertexPosition[0] =
					rightVertexPosition[0] - tmpMvVertexCmdVec.length();
			}

		} else if (command instanceof MoveSegmentCommand){
			SegmentEntity movingSegment =
				(SegmentEntity) ((MoveSegmentCommand)command).getEntity();

			VertexEntity tmpStartVertex = movingSegment.getStartVertexEntity();
			VertexEntity tmpEndVertex = movingSegment.getEndVertexEntity();

			VertexEntity ve = null;

			if(leftVertex.getEntityID() == tmpStartVertex.getEntityID() ||
					leftVertex.getEntityID() == tmpEndVertex.getEntityID()){

				ve = leftVertex;

			} else if (
				rightVertex.getEntityID() == tmpStartVertex.getEntityID() ||
				rightVertex.getEntityID() == tmpEndVertex.getEntityID()){

				ve = rightVertex;

			} else {

				return false;
			}


			double[] tmpVertexStartPos = new double[3];
			double[] tmpVertexEndPos = new double[3];

			((MoveSegmentCommand)command).getStartVertexStartPosition(
					tmpVertexStartPos);

			((MoveSegmentCommand)command).getStartVertexEndPosition(
					tmpVertexEndPos);

			Vector3d tmpMvVertexCmdVec = new Vector3d(
					tmpVertexEndPos[0] - tmpVertexStartPos[0],
					tmpVertexEndPos[1] - tmpVertexStartPos[1],
					0.0);

			// Correct offset
			if(ve.getEntityID() == leftVertex.getEntityID()){

				leftVertexPosition[0] =
					leftVertexPosition[0] + tmpMvVertexCmdVec.length();

				finalXAxisOffset = -tmpMvVertexCmdVec.length();

			} else {

				rightVertexPosition[0] =
					rightVertexPosition[0] - tmpMvVertexCmdVec.length();
			}

		}

		/*
		 * Adjust vertex position values for leftVertex is aligned with y axis
		 * and each has respective heights. Flatten to z = 0.
		 */
		leftVertexPosition[1] = leftVertex.getHeight();
		rightVertexPosition[1] = rightVertex.getHeight();

		// Build up entity data
		Entity parentEntity = null;
		int parentID = entity.getParentEntityID();

		double[] parentPosSum = new double[] {0.0, 0.0, 0.0};

		while (!(parentEntity instanceof SegmentEntity)){

			parentEntity = model.getEntity(parentID);

			/*
			 * Get parent position to build up transform
			 */
			if(parentEntity instanceof BasePositionableEntity){

				double[] tempPos = new double[3];
				((BasePositionableEntity)parentEntity).getStartingPosition(
						tempPos);

				parentPosSum[0] = parentPosSum[0] + tempPos[0];
				parentPosSum[1] = parentPosSum[1] + tempPos[1];
				parentPosSum[2] = parentPosSum[2] + tempPos[2];
			}

			/*
			 * Get next parent
			 */
			if(parentEntity instanceof Entity){
				parentID = parentEntity.getParentEntityID();
			} else {
				break;
			}
		}

		// Get the entity bounds
		float[] bounds = new float[6];
		((PositionableEntity)entity).getBounds(bounds);

		// Get the entity position, flatten to xy plane
		double[] originalPos = new double[3];
		((PositionableEntity)entity).getStartingPosition(originalPos);

		double[] pos = new double[3];
		pos[0] = parentPosSum[0] + originalPos[0];
		pos[1] = parentPosSum[1] + originalPos[1];
		//pos[2] = parentPosSum[2] + pos[2];
		pos[2] = 0.0;

		/*
		 * Create new scale and position values
		 */
		float[] size = new float[3];
		((PositionableEntity)entity).getSize(size);

		float[] scale = new float[3];
		((PositionableEntity)entity).getScale(scale);

		float[] newScale = new float[3];
		double[] newPos = new double[3];
		double maximumWidth = 0;

		/*
		 * Check first for straddling bounds on left side. If not the case then
		 * check for straddling right side.
		 */

		if(pos[0]+bounds[0] < leftVertexPosition[0] &&
				pos[0]+bounds[1] > leftVertexPosition[0]){

			maximumWidth = originalPos[0] + bounds[1] - leftVertexPosition[0];

			newScale[0] = (float)(maximumWidth / size[0]);
			newScale[1] = scale[1];
			newScale[2] = scale[2];

			newPos[0] = originalPos[0] + bounds[1] -
				(maximumWidth / 2.0) + finalXAxisOffset;
			newPos[1] = originalPos[1];
			newPos[2] = originalPos[2];

		} else if (pos[0]+bounds[0] < rightVertexPosition[0] &&
				pos[0]+bounds[1] > rightVertexPosition[0]) {

			maximumWidth = rightVertexPosition[0] - (originalPos[0]+bounds[0]);

			newScale[0] = (float)(maximumWidth / size[0]);
			newScale[1] = scale[1];
			newScale[2] = scale[2];

			newPos[0] = originalPos[0] + bounds[0] +
				(maximumWidth / 2.0) + finalXAxisOffset;
			newPos[1] = originalPos[1];
			newPos[2] = originalPos[2];

		} else {

			/*
			 * Handle diagonal ceiling collision case and auto scale down
			 * if possible.
			 */

		    //We do not need to run this check if the heights are the same
		    double leftHeight = segmentEntity.getStartVertexEntity().getHeight();
		    double rightHeight = segmentEntity.getEndVertexEntity().getHeight();

		    if(leftHeight != rightHeight) {

		        // TODO: performCheck should be in a utility class
		        VertexHeightPropertyChangeRule rule =
		            new VertexHeightPropertyChangeRule(errorReporter, model, view);

    			if(rule.scaleProductHeightIntoBounds(
    					model,
    					cmdList,
    					segmentEntity,
    					entity,
    					null,
    					null)){

    				return true;
    			}
		    }

		}

		/*
		 * Validate that adjustment would not be less than minimum width.
		 * Do not allow any width change to be less than zero.
		 */
		float[] minimumSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MINIMUM_OBJECT_SIZE_PROP);

		if(maximumWidth <= 0){
			return false;
		}

		if(minimumSize != null && (minimumSize[0] > maximumWidth)){
			return false;
		}

		/*
		 * Handle uniform scale case. Assume that if the x axis scale
		 * is legal all others would be otherwise the input data doesn't make
		 * any sense. Minimum size should be the same on all axis for uniform
		 * scale.
		 */
		if(scaleRestriction.equals(
				ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.UNIFORM)){

			newScale[1] = newScale[0];
			newScale[2] = newScale[0];

			newPos[2] = originalPos[2] + bounds[4] +
				(maximumWidth / 2.0) - 0.001;
		}

		/*
		 * Create the new scale command
		 */
		int transactionID = model.issueTransactionID();
		ScaleEntityCommand scaleCmd = new ScaleEntityCommand(
				model,
				transactionID,
				(PositionableEntity)entity,
				newPos,
				originalPos,
				newScale,
				scale);

        // TODO: performCheck should be in a utility class
		ScaleAutoAddRule rule =
            new ScaleAutoAddRule(errorReporter, model, view);

		/*
		 * Adjust any auto place pieces as a result of the scale action.
		 */
/*		ArrayList<Command> tmpCmdList = new ArrayList<Command>();
		tmpCmdList =
			rule.pipeInRelatedRuleAccess(
					model,
					entity,
					scaleCmd);
*/
		cmdList.add(scaleCmd);

/*		if(tmpCmdList.size() > 0){
			cmdList.addAll(tmpCmdList);
		}
*/
		return true;
	}

}
