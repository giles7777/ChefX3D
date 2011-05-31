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
import java.util.Arrays;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.AddEntityChildTransientCommand;
import org.chefx3d.model.AddEntityCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.ScaleEntityCommand;
import org.chefx3d.model.ScaleEntityTransientCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
 * Snap entities to the nearest wall edge within a given tolerance. Will not
 * evaluate entities whose parent is not a zone.
 *
 * Can be prevented by setting the IGNORE_WALL_EDGE_SNAP rule to true.
 *
 * @author Ben Yarger
 * @version $Revision: 1.45 $
 */
public class SegmentEdgeSnapRule extends BaseRule  {

	/** Constant threshold value used to calculate THRESHOLD (override Rule)*/
	private static final double THRESHOLD = 0.05;

	/** Threshold value used in evaluation, can be set. */
	private double threshold;

	/**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SegmentEdgeSnapRule(
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

		// Check for ignore wall edge snap flag.
		Boolean ignoreWallEdgeSnap =
			(Boolean) RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.IGNORE_WALL_EDGE_SNAP);

        // Check for ignore wall edge snap flag.
        Boolean restrictToBoundary =
            (Boolean) RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_BOUNDARY);

        // Check for no model
        Boolean noModel =
            (Boolean) RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.NO_MODEL_PROP);

        // Check for no model
        Boolean autoSpan =
            (Boolean) RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        // if we are ignoring wall snaps and the wall boundary
        // then just let the item move outside the wall
		if(noModel || autoSpan || (ignoreWallEdgeSnap && !restrictToBoundary)){
	        result.setResult(true);
	        return(result);
		}

		// if we are using wall snaps then the adjustment is the snap value,
		// otherwise just keep the item on the wall with a 0 adjustment
		if (!ignoreWallEdgeSnap) {
		    threshold = THRESHOLD;
		} else {
		    threshold = 0;
		}

		// If not PositionableEntity, exit
		if(!(entity instanceof PositionableEntity)) {
	        result.setResult(true);
	        return(result);
		}
		
		// Begin snap operations
		ZoneEntity activeZoneEntity = null;
		double[] entityPos = new double[3];
		float[] entityBounds = new float[6];
		
		// Get the exact position relative to the zone.
		// We are operating under the assumption that the active zone is a 
		// parent somewhere along the way for the entity being altered.		
		entityPos = 
			TransformUtils.getExactPositionRelativeToZone(model, command);
		
		entityBounds = BoundsUtils.getBounds(
				(PositionableEntity) entity, 
				true);
		
		activeZoneEntity = 
			SceneHierarchyUtility.findExactZoneEntity(model, entity);

		// Safety check data
		if (entityPos == null || 
				entityBounds == null || 
				activeZoneEntity == null) {
			
			result.setResult(true);
	        return(result);
		}

		// Hand of for analysis and possible data adjustment
		performSnapAnalysis(
				command,
				model,
				(PositionableEntity)entity,
				entityPos,
				entityBounds,
				activeZoneEntity);

        result.setResult(true);
        return(result);
	}

	/**
	 * Perform the snap analysis and adjustment if needed.
	 *
	 * @param command Issuing command
	 * @param model WorldModel to reference
	 * @param entity Entity to check snap against
	 * @param entityPos Position of entity
	 * @param entityBounds Bounds of entity
	 * @param zoneEntity ZoneEntity parent of entity
	 */
	private void performSnapAnalysis(
			Command command,
			WorldModel model,
			PositionableEntity entity,
			double[] entityPos,
			float[] entityBounds,
			ZoneEntity zoneEntity){

		// Zone bounds values to use in analysis
		double zoneLeftEdge = 0.0;
		double zoneRightEdge = 0.0;
		double zoneTopEdge = 0.0;
		double zoneBottomEdge = 0.0;

		if (zoneEntity instanceof SegmentEntity) {
			
			SegmentEntity segmentEntity = (SegmentEntity) zoneEntity;
			// Get vectors and extract positions
			VertexEntity startVertexEntity = 
				segmentEntity.getStartVertexEntity();
			VertexEntity endVertexEntity = 
				segmentEntity.getEndVertexEntity();
	
			double[] startVertexPos = new double[3];
			double[] endVertexPos = new double[3];
	
			startVertexEntity.getPosition(startVertexPos);
			endVertexEntity.getPosition(endVertexPos);
	
			Vector3d wallVec = new Vector3d(
					endVertexPos[0] - startVertexPos[0],
					endVertexPos[1] - startVertexPos[1],
					0.0);
	
			double wallLength = wallVec.length();
	
			// Create the 4 wall coordinates
			double topLeftHeight = 0.0;
			double topRightHeight = 0.0;
	
			topLeftHeight = startVertexEntity.getHeight();
			topRightHeight = endVertexEntity.getHeight();
	
			// Get the top left and right entity vectors
			Vector3f topLeftVec = new Vector3f(
					(float) (entityBounds[0] + entityPos[0]),
					(float) (entityBounds[3] + entityPos[1]),
					(float) 0.0);
	
			Vector3f topRightVec = new Vector3f(
					(float) (entityBounds[1] + entityPos[0]),
					(float) (entityBounds[3] + entityPos[1]),
					(float)0.0);
			
			// Calculate top edge
			double m = (topRightHeight - topLeftHeight) / wallLength;

			double leftX = topLeftVec.x;
			double rightX = topRightVec.x;

			if(leftX < 0.0){
				leftX = 0.0;
			}

			if(rightX > wallLength){
				rightX = wallLength;
			}

			double y1 = m * leftX + topLeftHeight;
			double y2 = m * rightX + topLeftHeight;
			
			// Set values
			zoneRightEdge = wallLength;
			zoneLeftEdge = 0.0;
			zoneTopEdge = Math.min(y1, y2);
			zoneBottomEdge = 0.0;

		} else if (zoneEntity.getType() == Entity.TYPE_GROUNDPLANE_ZONE) {
			
			//TODO: complete implementation if required. At this time it is not.
			return;
			
		} else {
			
			float[] bounds = new float[6];
			zoneEntity.getBounds(bounds);
			
			zoneRightEdge = bounds[1];
			zoneLeftEdge = bounds[0];
			zoneTopEdge = bounds[3];
			zoneBottomEdge = bounds[2];
		}

		//----------------------------------------------------------------------
		// Perform snap adjustments in the following order:
		// 1) right
		// 2) bottom
		// 3) left
		// 4) top (special case for slope)
		//
		// If scaling do a check to see which anchor is doing the scaling. Don't 
		// evaulate the edge case for an anchor that doesn't match.
		//
		// 1) If isScaleIncreasing == true and isPositiveDirection == true 
		// then the scale is growing via the positive anchor.
		// 2) If isScaleIncreasing == false and isPositiveDirection == false
		// then the scale is shrinking via the positive anchor.
		// 3) If isScaleIncreasing == false and isPositiveDirection == true
		// then the scale is shrinking via the negative anchor.
		// 4) If isScaleIncreasing == true and isPositiveDirection == false
		// then the scale is growing via the negative anchor.
		//----------------------------------------------------------------------
		
		if (command instanceof ScaleEntityCommand || 
				command instanceof ScaleEntityTransientCommand) {
			
			float[] newScale = TransformUtils.getExactScale(entity);
			float[] startScale = TransformUtils.getStartingScale(entity);
			double[] newPos = TransformUtils.getExactPosition(entity);
			double[] startPos = TransformUtils.getStartPosition(entity);
	
			Boolean isScaleIncreasingVertically = 
				TransformUtils.isScaleIncreasing(
						newScale, 
						startScale, 
						TARGET_ADJUSTMENT_AXIS.YAXIS);
			
			Boolean isScaleIncreasingHorizontally = 
				TransformUtils.isScaleIncreasing(
						newScale, 
						startScale, 
						TARGET_ADJUSTMENT_AXIS.XAXIS);
			
			Boolean isPositiveDirectionVertically = 
				TransformUtils.isScaleInPositiveDirection(
						newPos, 
						startPos, 
						TARGET_ADJUSTMENT_AXIS.YAXIS);
			
			Boolean isPositiveDirectionHorizontally = 
				TransformUtils.isScaleInPositiveDirection(
						newPos, 
						startPos, 
						TARGET_ADJUSTMENT_AXIS.XAXIS);
			
			// Correction to apply in each case
			double[] correction = new double[3];
	
			//----------------
			// Right edge case
			//----------------
			double rightEdge = entityPos[0] + entityBounds[1];
	
			if(rightEdge > (zoneRightEdge - threshold) &&
					(isPositiveDirectionHorizontally != null && 
							isScaleIncreasingHorizontally != null) &&
					(isPositiveDirectionHorizontally && 
							isScaleIncreasingHorizontally)){
				
				correction[0] = zoneRightEdge - rightEdge;
				correction[1] = 0.0;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction, 
						null, 
						zoneRightEdge, 
						null, 
						null);
			}
	
			//-----------------
			// Bottom edge case
			//-----------------
			double bottomEdge = entityPos[1] + entityBounds[2];
	
			if(bottomEdge < (zoneBottomEdge + threshold) && 
					(isPositiveDirectionVertically != null && 
							isScaleIncreasingVertically != null) &&
					(!isPositiveDirectionVertically && 
							isScaleIncreasingVertically)){
	
				correction[0] = 0.0;
				correction[1] = zoneBottomEdge - bottomEdge;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction, 
						null, 
						null, 
						zoneBottomEdge, 
						null);
			}
	
			//---------------
			// Left edge case
			//---------------
			double leftEdge = entityPos[0] + entityBounds[0];
	
			if(leftEdge < (zoneLeftEdge + threshold) &&
					(isPositiveDirectionHorizontally != null && 
							isScaleIncreasingHorizontally != null) && 
					(!isPositiveDirectionHorizontally &&
							isScaleIncreasingHorizontally)){
	
				correction[0] = zoneLeftEdge - leftEdge;
				correction[1] = 0.0;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction,
						null, 
						null, 
						null, 
						zoneLeftEdge);
			}
	
			// Top edge case
			double topEdge = entityPos[1] + entityBounds[3];
			
			if(topEdge > (zoneTopEdge - threshold) &&
					(isPositiveDirectionVertically != null && 
							isScaleIncreasingVertically != null) &&
					(isPositiveDirectionVertically && 
							isScaleIncreasingVertically)){

				correction[0] = 0.0;
				correction[1] = zoneTopEdge - topEdge;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction, 
						zoneTopEdge,
						null, 
						null, 
						null);
			}
			
		} else {
			
			// Correction to apply in each case
			double[] correction = new double[3];
	
			//----------------
			// Right edge case
			//----------------
			double rightEdge = entityPos[0] + entityBounds[1];
	
			if(rightEdge > (zoneRightEdge - threshold)){
				
				correction[0] = zoneRightEdge - rightEdge;
				correction[1] = 0.0;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction, 
						null, 
						zoneRightEdge, 
						null, 
						null);
			}
	
			//-----------------
			// Bottom edge case
			//-----------------
			double bottomEdge = entityPos[1] + entityBounds[2];
	
			if(bottomEdge < (zoneBottomEdge + threshold)){
	
				correction[0] = 0.0;
				correction[1] = zoneBottomEdge - bottomEdge;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction, 
						null, 
						null, 
						zoneBottomEdge, 
						null);
			}
	
			//---------------
			// Left edge case
			//---------------
			double leftEdge = entityPos[0] + entityBounds[0];
	
			if(leftEdge < (zoneLeftEdge + threshold)){
	
				correction[0] = zoneLeftEdge - leftEdge;
				correction[1] = 0.0;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction,
						null, 
						null, 
						null, 
						zoneLeftEdge);
			}
	
			// Top edge case
			double topEdge = entityPos[1] + entityBounds[3];

			if(topEdge > (zoneTopEdge - threshold)){
	
				correction[0] = 0.0;
				correction[1] = zoneTopEdge - topEdge;
				correction[2] = 0.0;
				
				performAdjustment(
						command, 
						model, 
						entity, 
						correction, 
						zoneTopEdge,
						null, 
						null, 
						null);
			}
			
		}
	}

	/**
	 * Updates the command with the correction specified
	 *
	 * @param command Command issued
	 * @param model WorldModel to reference
	 * @param entity Entity acted on by command
	 * @param correction The position correction that should occur
	 * @param zoneTopEdge The top edge of the zone in zone coords, null if not
	 * exceeded
	 * @param zoneRightEdge The right edge of the zone in zone coords, null if
	 * not exceeded
	 * @param zoneBottomEdge The bottom edge of the zone in zone coords, null if
	 * not exceeded
	 * @param zoneLeftEdge The left edge of the zone in zone coords, null if not
	 * exceeded
	 */
	private void performAdjustment(
			Command command,
			WorldModel model,
			PositionableEntity entity,
			double[] correction,
			Double zoneTopEdge,
			Double zoneRightEdge,
			Double zoneBottomEdge,
			Double zoneLeftEdge){
		
		// Prepare for special handling of miter entity.
		Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);

		if(command instanceof MoveEntityCommand){
			
			MoveEntityCommand mvCmd = (MoveEntityCommand) command;
			
			double[] endPos = new double[3];
			mvCmd.getEndPosition(endPos);
			
			endPos[0] += correction[0];
			endPos[1] += correction[1];
			endPos[2] += correction[2];

			mvCmd.setEndPosition(endPos);

		} else if (command instanceof MoveEntityTransientCommand){

			MoveEntityTransientCommand mvCmd = 
				(MoveEntityTransientCommand) command;
			
			double[] position = new double[3];
			mvCmd.getPosition(position);
			
			position[0] += correction[0];
			position[1] += correction[1];
			position[2] += correction[2];
			
			mvCmd.setPosition(position);

		} else if (command instanceof TransitionEntityChildCommand){
			
			// Don't adjust the transient version of this command because
			// it is just reparenting to the active zone for further 
			// transient movement which is where we will apply this rule.
			if (command.isTransient()) {
				return;
			}
			
			TransitionEntityChildCommand tranCmd = 
				(TransitionEntityChildCommand) command;
			
			double[] endPos = new double[3];
			float[] endScale = new float[3];
			float[] startScale = new float[3];
			
			// Typically, this is all we will need
			tranCmd.getEndPosition(endPos);
			
			// We will still check for a change in scale to see if the 
			// adjustment we need to make is more complex
			tranCmd.getEndScale(endScale);
			tranCmd.getStartScale(startScale);
			
			if (!Arrays.equals(endScale, startScale)) {
			
				float[] entitySize = new float[3];
				entity.getSize(entitySize);
				
				endScale[0] = (float) 
					(endScale[0] * entitySize[0] + correction[0]) / 
					entitySize[0];
				endScale[1] = (float) 
					(endScale[1] * entitySize[1] + correction[1]) / 
					entitySize[1];
				endScale[2] = (float) 
					(endScale[2] * entitySize[2] + correction[2]) / 
					entitySize[2];
				
				tranCmd.setEndScale(endScale);
				
				correction[0] /= 2.0;
				correction[1] /= 2.0;
				correction[2] /= 2.0;
			}
			
			endPos[0] += correction[0];
			endPos[1] += correction[1];
			endPos[2] += correction[2];
			
			tranCmd.setEndPosition(endPos);

		} else if (command instanceof ScaleEntityCommand) {

			ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;
			
			double[] endPos = new double[3];
			float[] endScale = new float[3];
			float[] entitySize = new float[3];
			
			scaleCmd.getNewPosition(endPos);
			scaleCmd.getNewScale(endScale);
			
			entity.getSize(entitySize);
			
			float[] startingBounds = 
				BoundsUtils.getStartingBounds(entity);
			
			if (startingBounds == null) {
				return;
			}
			
			Entity activeZone = 
				SceneHierarchyUtility.getActiveZoneEntity(model);
			
			if (activeZone == null) {
				return;
			}
			
			double[] startPos =
				TransformUtils.getExactRelativePosition(
						model, entity, activeZone, true);
			
			if (startPos == null) {
				return;
			}
			
			if (canMiterCut) {
				
				// Capture parent values to correct final endPos.
				Entity parentEntity = 
					SceneHierarchyUtility.getExactStartParent(model, entity);
				
				if (parentEntity == null) {
					return;
				}
				
				double[] parentStartPos =
					TransformUtils.getExactRelativePosition(
							model, parentEntity, activeZone, true);
				
				if (parentStartPos == null) {
					return;
				}
				
				// Begin the actual work
				double newSize;
				
				if (zoneTopEdge != null) {
					
					newSize = zoneTopEdge - startPos[1] - startingBounds[2];
					
					endPos[1] = zoneTopEdge - newSize / 2.0 - parentStartPos[1];
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneRightEdge != null) {
					
					newSize = zoneRightEdge - startPos[0] - startingBounds[0];
					
					endPos[0] = 
						zoneRightEdge - newSize / 2.0  - parentStartPos[0];
					
					endScale[0] = (float) newSize / entitySize[0];
					
				} else if (zoneBottomEdge != null) {
					
					newSize = startPos[1] + startingBounds[3] - zoneBottomEdge;
					
					endPos[1] = newSize / 2.0 - parentStartPos[1];
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneLeftEdge != null) {
					
					newSize = startPos[0] + startingBounds[1] - zoneLeftEdge;
					
					endPos[0] = newSize / 2.0 - parentStartPos[0];
					
					endScale[0] = (float) newSize / entitySize[0];
				
				} else {
					return;
				}
				
			} else {
			
				// Begin the actual work for non extrusion entities
				double newSize;
				
				if (zoneTopEdge != null) {
					
					newSize = zoneTopEdge - startPos[1] - startingBounds[2];
					
					endPos[1] = 
						(newSize - 
							(startingBounds[3] - startingBounds[2])) / 
							2.0 + startPos[1];
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneRightEdge != null) {
					
					newSize = zoneRightEdge - startPos[0] - startingBounds[0];
					
					endPos[0] = 
						(newSize -
							(startingBounds[1] - startingBounds[0])) / 
							2.0  + startPos[0];
					
					endScale[0] = (float) newSize / entitySize[0];
					
				} else if (zoneBottomEdge != null) {
					
					newSize = startPos[1] + startingBounds[3] - zoneBottomEdge;
					
					endPos[1] =  
						startPos[1] -
						(newSize - (startingBounds[3] - startingBounds[2])) / 
						2.0;
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneLeftEdge != null) {
					
					newSize = startPos[0] + startingBounds[1] - zoneLeftEdge;
					
					endPos[0] = 
						startPos[0] -
						(newSize - (startingBounds[1] - startingBounds[0])) / 
						2.0;
					
					endScale[0] = (float) newSize / entitySize[0];
				
				} else {
					return;
				}
			}
			
			scaleCmd.setNewPosition(endPos);
			scaleCmd.setNewScale(endScale);

		} else if (command instanceof ScaleEntityTransientCommand){

			ScaleEntityTransientCommand scaleCmd = 
				(ScaleEntityTransientCommand) command;
			
			double[] endPos = new double[3];
			float[] endScale = new float[3];
			float[] entitySize = new float[3];
			
			scaleCmd.getPosition(endPos);
			scaleCmd.getScale(endScale);
			
			entity.getSize(entitySize);
			
			float[] startingBounds = 
				BoundsUtils.getStartingBounds(entity);
			
			if (startingBounds == null) {
				return;
			}
			
			Entity activeZone = 
				SceneHierarchyUtility.getActiveZoneEntity(model);
			
			if (activeZone == null) {
				return;
			}
			
			double[] startPos =
				TransformUtils.getExactRelativePosition(
						model, entity, activeZone, true);
			
			if (startPos == null) {
				return;
			}
			
			if (canMiterCut) {
				
				// Capture parent values to correct final endPos.
				Entity parentEntity = 
					SceneHierarchyUtility.getExactStartParent(model, entity);
				
				if (parentEntity == null) {
					return;
				}
				
				double[] parentStartPos =
					TransformUtils.getExactRelativePosition(
							model, parentEntity, activeZone, true);
				
				if (parentStartPos == null) {
					return;
				}
				
				// Begin the actual work
				double newSize;
				
				if (zoneTopEdge != null) {
					
					newSize = zoneTopEdge - startPos[1] - startingBounds[2];
					
					endPos[1] = zoneTopEdge - newSize / 2.0 - parentStartPos[1];
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneRightEdge != null) {

					newSize = zoneRightEdge - startPos[0] - startingBounds[0];
					
					endPos[0] = 
						zoneRightEdge - newSize / 2.0 - parentStartPos[0];
					
					endScale[0] = (float) newSize / entitySize[0];
					
				} else if (zoneBottomEdge != null) {
					
					newSize = startPos[1] + startingBounds[3] - zoneBottomEdge;
					
					endPos[1] = newSize / 2.0 - parentStartPos[1];
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneLeftEdge != null) {

					newSize = startPos[0] + startingBounds[1] - zoneLeftEdge;
					
					endPos[0] = newSize / 2.0 - parentStartPos[0];
					
					endScale[0] = (float) newSize / entitySize[0];
				
				} else {
					return;
				}
				
			} else {
			
				// Begin the actual work for non extrusion entities
				double newSize;
				
				if (zoneTopEdge != null) {
					
					newSize = zoneTopEdge - startPos[1] - startingBounds[2];
					
					endPos[1] = 
						(newSize - 
							(startingBounds[3] - startingBounds[2])) / 
							2.0 + startPos[1];
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneRightEdge != null) {
					
					newSize = zoneRightEdge - startPos[0] - startingBounds[0];
					
					endPos[0] = 
						(newSize -
							(startingBounds[1] - startingBounds[0])) / 
							2.0  + startPos[0];
					
					endScale[0] = (float) newSize / entitySize[0];
					
				} else if (zoneBottomEdge != null) {
					
					newSize = startPos[1] + startingBounds[3] - zoneBottomEdge;
					
					endPos[1] =  
						startPos[1] -
						(newSize - (startingBounds[3] - startingBounds[2])) / 
						2.0;
					
					endScale[1] = (float) newSize / entitySize[1];
					
				} else if (zoneLeftEdge != null) {
					
					newSize = startPos[0] + startingBounds[1] - zoneLeftEdge;
					
					endPos[0] = 
						startPos[0] -
						(newSize - (startingBounds[1] - startingBounds[0])) / 
						2.0;
					
					endScale[0] = (float) newSize / entitySize[0];
				
				} else {
					return;
				}
			}
			
			scaleCmd.setPosition(endPos);
			scaleCmd.setScale(endScale);

		} else if (command instanceof AddEntityCommand){

			double[] pos = new double[3];
			entity.getPosition(pos);
			
			pos[0] += correction[0];
			pos[1] += correction[1];
			pos[2] += correction[2];
			
			entity.setPosition(pos, false);

		} else if (command instanceof AddEntityChildCommand){

			double[] pos = new double[3];
			entity.getPosition(pos);
			
			pos[0] += correction[0];
			pos[1] += correction[1];
			pos[2] += correction[2];
			
			entity.setPosition(pos, false);

		} else if (command instanceof AddEntityChildTransientCommand){

			double[] pos = new double[3];
			entity.getPosition(pos);
			
			pos[0] += correction[0];
			pos[1] += correction[1];
			pos[2] += correction[2];
			
			entity.setPosition(pos, false);
		}
	}
}
