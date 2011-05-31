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

import javax.vecmath.Vector3d;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.BoundsUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* If a VertexEntities's VertexEntity.HEIGHT_PROP changes,
				Entity.EDITABLE_PROPERTIES,
*
* @author Ben Yarger
* @version $Revision: 1.35 $
*/
public class VertexHeightPropertyChangeRule extends BaseRule  {

	/** Confirmation pop up message */
	private static final String CONFIRM_MSG =
		"org.chefx3d.rules.definitions.VertexHeightPropertyChangeRule.confirmRemoval";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public VertexHeightPropertyChangeRule(
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
		 * Only perform operation on VertexEntities
		 */

		if(entity instanceof VertexEntity &&
				(command instanceof ChangePropertyCommand||
						command instanceof ChangePropertyTransientCommand)){

			/*
			 * If the property change isn't VertexEntity.HEIGHT_PROP then skip
			 * check.
			 */
			if(command.isTransient()){

				if(!(
					(ChangePropertyTransientCommand)command).getPropertyName().equals(
							VertexEntity.HEIGHT_PROP)){
                    result.setResult(true);
                    return(result);
				}

			} else {

				if(!(
					(ChangePropertyCommand)command).getPropertyName().equals(
							VertexEntity.HEIGHT_PROP)){
                    result.setResult(true);
                    return(result);
				}
			}

			/*
			 * Begin check
			 */
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
			 * Get any segment that uses the updated vertex. Check that
			 * segment's children for bounds against segment.
			 */
			for(SegmentEntity segment : segmentList){

				if(segment.getStartVertexEntity().getEntityID() ==
					vEntity.getEntityID()){
					deleteStrandedChildren(
							model,
							command,
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
			 * Size will only be > 0 if the command is
			 * not transient.
			 */
			if(cmdList.size() > 0){

				String msg = intl_mgr.getString(CONFIRM_MSG);

				/*
				 * Have the user confirm removal of the products no longer
				 * within the bounds of the wall. If they chose to cancel
				 * the operation, put the height prop back at its original
				 * value.
				 */

				if(!( popUpConfirm.showMessage(msg))){
				    Float initialHeight =
						(Float) ((ChangePropertyCommand)command).getOriginalValue();

					((ChangePropertyCommand)command).setNewValue(initialHeight);
					popUpConfirm.setDisplayPopUp(false);
                    result.setResult(false);
                    return(result);
				} else {
					addNewlyIssuedCommand(cmdList);

					popUpConfirm.setDisplayPopUp(false);
                    result.setResult(true);
                    return(result);
				}

			}
		}
        result.setResult(true);
        return(result);
	}



	/**
	 * Deletes stranded children, based on adjustments to the wall height.
	 *
	 * @param model
	 * @param command
	 * @param segmentEntity
	 * @param cmdList
	 */
	private void deleteStrandedChildren(
			WorldModel model,
			Command command,
			SegmentEntity segmentEntity,
			ArrayList<Command> cmdList){

		Float leftVertexHeight = null;
		Float rightVertexHeight = null;
		Entity cmdEntity = null;
		float value = 0.0f;

		if(command.isTransient()){
			cmdEntity = ((ChangePropertyTransientCommand)command).getEntity();

			if(cmdEntity.getEntityID() ==
				segmentEntity.getStartVertexEntity().getEntityID()){

				leftVertexHeight = (Float)((ChangePropertyTransientCommand)command).getNewValue();

			} else if (cmdEntity.getEntityID() ==
				segmentEntity.getEndVertexEntity().getEntityID()){

				rightVertexHeight = (Float)((ChangePropertyTransientCommand)command).getNewValue();
			}

		} else {
			cmdEntity = ((ChangePropertyCommand)command).getEntity();

			if(cmdEntity.getEntityID() ==
				segmentEntity.getStartVertexEntity().getEntityID()){

				leftVertexHeight = (Float)((ChangePropertyCommand)command).getNewValue();

			} else if (cmdEntity.getEntityID() ==
				segmentEntity.getEndVertexEntity().getEntityID()){

				rightVertexHeight = (Float)((ChangePropertyCommand)command).getNewValue();
			}

		}

		/*
		 * Only perform operation if issuing command is not transient
		 */
		if(!(command.isTransient())){

			ArrayList<Entity> segmentChildren = segmentEntity.getChildren();

			for(Entity entity : segmentChildren){

				float[] rotation = new float[4];

				((PositionableEntity)entity).getRotation(rotation);

				if(!BoundsUtils.performSegmentBoundsCheck(
						model,
						(PositionableEntity) entity,
						leftVertexHeight,
						rightVertexHeight,
						null)){

					if(!scaleProductHeightIntoBounds(
							model,
							cmdList,
							segmentEntity,
							entity,
							leftVertexHeight,
							rightVertexHeight)){

					    RemoveEntityChildCommand delCmd =
							new RemoveEntityChildCommand(model, segmentEntity, entity);

					    for(Command cmd:cmdList) {
					        if(delCmd.isEqualTo(cmd)) {
					            return;
					        }
					    }
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
	 * @param leftVertexHeightNew New left vertex height value, can be null
	 * @param rightVertexHeightNew New right vertex height value, can be null
	 * @return True if scale was added to cmdList, false otherwise
	 */
	public boolean scaleProductHeightIntoBounds(
			WorldModel model,
			ArrayList<Command> cmdList,
			SegmentEntity segmentEntity,
			Entity entity,
			Float leftVertexHeightNew,
			Float rightVertexHeightNew){

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
				canScale == null ||
				isEditable.booleanValue() != true ||
				canScale.booleanValue() != true){

			return false;
		}

		ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES scaleRestriction =
			(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES)
				RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.SCALE_RESTRICTION_PROP);

		ArrayList<ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES> legalScaleDirections =
			new ArrayList<ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES>();

		legalScaleDirections.add(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.YAXIS);
		legalScaleDirections.add(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.XYPLANE);
		legalScaleDirections.add(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.YZPLANE);
		legalScaleDirections.add(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.UNIFORM);
		legalScaleDirections.add(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.NONE);

		if(!legalScaleDirections.contains(scaleRestriction)){
			return false;
		}

		// Get vertex data
		VertexEntity leftVertex = segmentEntity.getStartVertexEntity();
		VertexEntity rightVertex = segmentEntity.getEndVertexEntity();

		double[] leftVertexPosition = new double[3];
		double[] rightVertexPosition = new double[3];

		leftVertex.getPosition(leftVertexPosition);
		rightVertex.getPosition(rightVertexPosition);

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

		/*
		 * Adjust vertex position values for leftVertex is aligned with y axis
		 * and each has respective heights. Flatten to z = 0.
		 */
		if(leftVertexHeightNew == null){
			leftVertexPosition[1] = leftVertex.getHeight();
		} else {
			leftVertexPosition[1] = leftVertexHeightNew.doubleValue();
		}
		if(rightVertexHeightNew == null){
			rightVertexPosition[1] = rightVertex.getHeight();
		} else {
			rightVertexPosition[1] = rightVertexHeightNew.doubleValue();
		}

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
				((BasePositionableEntity)parentEntity).getPosition(tempPos);

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
		((PositionableEntity)entity).getPosition(originalPos);

		double[] pos = new double[3];
		pos[0] = parentPosSum[0] + originalPos[0];
		pos[1] = parentPosSum[1] + originalPos[1];
		//pos[2] = parentPosSum[2] + pos[2];
		pos[2] = 0.0;

		/*
		 * Now use the y intersect equation for a line to determine requisite
		 * height for the entity if it is to stay on the wall.
		 */
		double m =
			(rightVertexPosition[1] - leftVertexPosition[1]) /
			rightVertexPosition[0];

		double y1 = m * (pos[0] + bounds[0]) + leftVertexPosition[1];
		double y2 = m * (pos[0] + bounds[1]) + leftVertexPosition[1];

		double y = Math.min(y1, y2);

		/*
		 * Validate that adjustment would not be less than minimum height.
		 * Do not allow any height change to be less than zero.
		 */
		float[] minimumSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MINIMUM_OBJECT_SIZE_PROP);

		double maximumHeight = y - (pos[1] + bounds[2]);

		if(maximumHeight <= 0){
			return false;
		}

		if(minimumSize != null && (minimumSize[1] > maximumHeight)){

			return false;
		}
		System.out.println("C-3-1 maximumHeight : " + maximumHeight);
		/*
		 * Create new scale and position values
		 */
		float[] size = new float[3];
		((PositionableEntity)entity).getSize(size);
System.out.println("C-3-1 size: " + size);
		float[] scale = new float[3];
		((PositionableEntity)entity).getScale(scale);
System.out.println("C-3-1 scale: " + scale);
		float[] newScale = new float[3];
		newScale[0] = scale[0];
		newScale[1] = (float)(maximumHeight/size[1]);
		newScale[2] = scale[2];
System.out.println("C-3-1 newScale: " + newScale);
		double[] newPos = new double[3];
		newPos[0] = originalPos[0];
		newPos[1] = originalPos[1] + bounds[2] + (maximumHeight / 2.0);
		newPos[2] = originalPos[2];
System.out.println("C-3-1 newPos: " + newPos);
		/*
		 * Handle uniform scale case. Assume that if the y axis scale
		 * is legal all others would be otherwise the input data doesn't make
		 * any sense. Minimum size should be the same on all axis for uniform
		 * scale.
		 */
		if(scaleRestriction.equals(ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES.UNIFORM)){

			newScale[0] = newScale[1];
			newScale[2] = newScale[1];

			newPos[2] = originalPos[2] + bounds[4] + (maximumHeight / 2.0) - 0.001;
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
