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

import javax.vecmath.*;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.util.AutoAddByPositionUtility;
import org.chefx3d.rules.util.AutoAddInvisibleChildrenUtility;
import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * When a parent item is scaled, this rule should be called in order to
 * preserve the /world/ position of child objects.  Doing this requires
 * us to modify the relative position of the child objects.
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.29 $
 */
public class ChildPositionCorrectionRule extends BaseRule  {
	
	/** 
	 * Hard coded limit for how close to the edge of the multi bounds we
	 * can get before we consider there to be an overlap.
	 */
	private static final double ALLOWED_EDGE_ERROR = 0.0;//0.0001;

    /** Scratch vars for rotation calcs */
    private Matrix4d mat;
    private AxisAngle4d axis;
    private Vector3d diffVec;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ChildPositionCorrectionRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        mat = new Matrix4d();
        axis = new AxisAngle4d();
        diffVec = new Vector3d();

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
        
        // Only operate on Scale commands
        if (!(command instanceof ScaleEntityCommand) && 
        		!(command instanceof ScaleEntityTransientCommand)) {
        	
        	result.setResult(true);
            return(result);
        }
        
        // Keep the scale from going smaller than the children that must remain
        // in their current locations
        checkScaleAgainstChildren((PositionableEntity)entity, command);

        //
        // calculate the difference between the original parent position
        // and the current parent position
        //
        double[] currentParentPos = 
        	TransformUtils.getExactPosition((PositionableEntity) entity);
        
        double[] initialParentPos = 
        	TransformUtils.getExactStartPosition(
        			(PositionableEntity) entity);

        //
        // this variable records the change of movement between the
        // parent's initial position and the parent's current position
        //
        double[] diff = new double[]{
                (initialParentPos[0] - currentParentPos[0]),
                (initialParentPos[1] - currentParentPos[1]),
                (initialParentPos[2] - currentParentPos[2])};

        float[] rot = new float[4];
        ((PositionableEntity)entity).getRotation(rot);

        if (rot[3] != 0.0) {
            axis.x = (double) rot[0];
            axis.y = (double) rot[1];
            axis.z = (double) rot[2];
            axis.angle = (double) rot[3];

            mat.set(axis);
            mat.invert();
            diffVec.x = diff[0];
            diffVec.y = diff[1];
            diffVec.z = diff[2];

            mat.transform(diffVec);

            diff[0] = diffVec.x;
            diff[1] = diffVec.y;
            diff[2] = diffVec.z;
        }

        // Get the exact set of children and adjust them accordingly by the 
        // change in position of the parent.
        ArrayList<Entity> children = 
        	SceneHierarchyUtility.getExactChildren(entity);
        
        ArrayList<Command> multiCmd = new ArrayList<Command>();

        for( Entity e : children ){

            PositionableEntity child = (PositionableEntity)e;

            // Do not mess with autoAdd position or inivisible.
            if (AutoAddInvisibleChildrenUtility.isAutoAddChildOfParent(
            		model, e, entity)) {
            	continue;
            }
            
            if (AutoAddByPositionUtility.isAutoAddByPositionChild(model, e)) {
            	continue;
            }

            // don't mess with complex product sub parts that are not the 
            // principle complex product of a configuration.
            Boolean isComplexProduct = 
            	(Boolean)RulePropertyAccessor.getRulePropertyValue(
            			e, 
            			ChefX3DRuleProperties.IS_COMPLEX_PRODUCT);
            
            Boolean isComplexSubPart =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        e,
                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

            if (!isComplexProduct && isComplexSubPart) {
            	continue;
            }
/*
            //-----------------------------------------------------------------
            // BJY: --
            // Adding special check to not move entities that are colliding
            // with auto add entities. Those cases are handled in the
            // ScaleAutoAddRule. There are some cases like the corner rounder
            // where it may be colliding with auto add entities from another
            // zone so we also check for that case here as well. The only way
            // we skip this evaluation is if there are collisions with auto
            // adds from the same zone only.

            double[] collisionTestPos = new double[3];
            child.getPosition(collisionTestPos);

            MoveEntityCommand collisionTestCmd =
            	new MoveEntityCommand(
            			model,
            			model.issueTransactionID(),
            			(PositionableEntity)child,
            			collisionTestPos,
            			collisionTestPos);

            rch.performCollisionCheck(collisionTestCmd, true, false, false);

            Boolean hasAutoAddCollision = false;
            Boolean allSameZone = true;
            Entity parentZoneEntity = null;

            if (rch.collisionEntities != null) {

            	for (int i = 0; i < rch.collisionEntities.size(); i++) {

            		Boolean autoAddCollision = (Boolean)
            			RulePropertyAccessor.getRulePropertyValue(
            				rch.collisionEntities.get(i),
            				ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

            		if (parentZoneEntity == null) {
            			parentZoneEntity =
            				SceneHierarchyUtility.findZoneEntity(
            						model, rch.collisionEntities.get(i));
            		} else {
            			Entity testZoneEntity =
            				SceneHierarchyUtility.findZoneEntity(
            						model, rch.collisionEntities.get(i));

            			if (testZoneEntity != parentZoneEntity) {
            				allSameZone = false;
            			}
            		}

            		if (autoAddCollision) {
            			hasAutoAddCollision = true;
            		}
            	}
            }

            if (hasAutoAddCollision && allSameZone) {
            	continue;
            }
*/
            // End BJY changes
            //-----------------------------------------------------------------

            // Account for any change of position between the previous and 
            // future frames.
            
            double[] childLastFramePos = TransformUtils.getPosition(child);
            double[] childNextFramePos = TransformUtils.getExactPosition(child);
            
            double[] adjustment = new double[3];
            adjustment[0] = childNextFramePos[0] - childLastFramePos[0];
            adjustment[1] = childNextFramePos[1] - childLastFramePos[1];
            adjustment[2] = childNextFramePos[2] - childLastFramePos[2];
            
            // Get the starting position to offset from
            double[] childStartPos = 
            	TransformUtils.getExactStartPosition(child);
            
            double[] newChildPos =  new double[]{ 
            		childStartPos[0] + diff[0] + adjustment[0],
            		childStartPos[1] + diff[1] + adjustment[1],
            		childStartPos[2] + diff[2] + adjustment[2]};

            if( command instanceof ScaleEntityTransientCommand) {

                MoveEntityTransientCommand metc =
                    new MoveEntityTransientCommand(
                        model,
                        model.issueTransactionID(),
                        child.getEntityID(), 
                        newChildPos,
                        new float[]{0, 0, 0});
                
                metc.setBypassRules(true);
                
                SceneManagementUtility.addSurrogate(collisionChecker, metc);
                multiCmd.add(metc);

            } else if( command instanceof ScaleEntityCommand) {
            	
                MoveEntityCommand mec = new MoveEntityCommand(model,
                        model.issueTransactionID(),
                        (PositionableEntity)child,
                        newChildPos,
                        childStartPos);
                
                mec.setBypassRules(true);
                
                SceneManagementUtility.addSurrogate(collisionChecker, mec);
                multiCmd.add(mec);
            }
        }

        if (multiCmd != null) {
            addNewlyIssuedCommand(multiCmd);
        }

        result.setResult(true);
        return(result);
    }
    
    /**
     * Check the scale change of the entity against its children. Adjust the
     * scale accordingly if the bounds of the entity will end up being inside
     * that of the multi bounds of the children.
     * 
     * @param entity Entity to evaluate.
     * @param command Command applied to the entity.
     */
    private void checkScaleAgainstChildren(
    		PositionableEntity entity, 
    		Command command) {
    	
    	// Get the children in the previous frame, we don't have to be concerned
    	// with next frame scenarios when correcting the children of scaling
    	// entities.
    	ArrayList<Entity> children = 
    		SceneHierarchyUtility.getExactChildren(entity);
    	
    	// Don't evaluate any invisible children
    	for (int i = (children.size() - 1); i >= 0; i--) {
    		
    		if (AutoAddInvisibleChildrenUtility.isAutoAddChildOfParent(
    				model, 
    				children.get(i), 
    				entity)) {
    			
    			children.remove(i);
    			continue;
    		}
    		
    		// check if the entity is a complex sub part
    		Boolean isComplexProduct =
    			(Boolean)RulePropertyAccessor.getRulePropertyValue(
    					children.get(i), 
    					ChefX3DRuleProperties.IS_COMPLEX_PRODUCT);
    		
    		Boolean isComplexSubPart =
    		    (Boolean)RulePropertyAccessor.getRulePropertyValue(
    		    		children.get(i),
    					ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

    		if (!isComplexProduct && isComplexSubPart) {
    			children.remove(i);
    			continue;
    		}
    	}
    	
    	// If there are no children, there is no restriction of the scale
    	if (children.size() == 0) {
    		return;
    	}
    	
    	// Calculate the multi bound of the children
    	float[] multiBounds = new float[6];
    	double[] multiCenter = new double[3];
    	
    	Entity activeZone = SceneHierarchyUtility.getActiveZoneEntity(model);
    	
    	if (!(activeZone instanceof PositionableEntity)) {
    		return;
    	}
    	
    	BoundsUtils.getMultiBounds(
    			model, 
    			multiBounds, 
    			multiCenter, 
    			children, 
    			(PositionableEntity)activeZone,
    			false);
    	
    	// Calculate the bounds of the entity and the position relative to
    	// the zone (assuming the zone is the active zone)
    	float[] entityBounds = BoundsUtils.getBounds(entity, true);
    	double[] entityPos = 
    		TransformUtils.getExactPositionRelativeToZone(model, entity);
    	
    	// See if the edges of the entity are inside the multi bounds
    	double multiLeftEdge = 
    		multiBounds[0] + multiCenter[0] - ALLOWED_EDGE_ERROR;
    	double multiRightEdge = 
    		multiBounds[1] + multiCenter[0] - ALLOWED_EDGE_ERROR;
    	
    	double entityLeftEdge = entityBounds[0] + entityPos[0];
    	double entityRightEdge = entityBounds[1] + entityPos[0];
    	
		// Entity is inside the multi bounds so we need to lock it to
		// the nearest edge in the opposite direcition of the scale.
    	
		// 1) If isScaleIncreasing == true and isPositiveDirection == true 
		// then the scale is growing via the right anchor.
		// 2) If isScaleIncreasing == false and isPositiveDirection == false
		// then the scale is shrinking via the right anchor.
		// 3) If isScaleIncreasing == false and isPositiveDirection == true
		// then the scale is shrinking via the left anchor.
		// 4) If isScaleIncreasing == true and isPositiveDirection == false
		// then the scale is growing via the left anchor.
		
		// We only have to worry about the case when the scale is shrinking
		// and in that case lock it to the multi bounds edge of the 
		// children. So, we only have to do cases 2 and 3 from above.
    	
		float[] newScale = TransformUtils.getExactScale(entity);
		float[] startScale = TransformUtils.getStartingScale(entity);
		double[] newPos = TransformUtils.getExactPosition(entity);
		double[] startPos = TransformUtils.getExactStartPosition(entity);
    	
    	Boolean isScaleIncreasing = 
			TransformUtils.isScaleIncreasing(
					newScale, 
					startScale, 
					TARGET_ADJUSTMENT_AXIS.XAXIS);
		
		Boolean isPositiveDirection = 
			TransformUtils.isScaleInPositiveDirection(
					newPos, 
					startPos, 
					TARGET_ADJUSTMENT_AXIS.XAXIS);
    	
    	if ((isScaleIncreasing != null && 
    			isPositiveDirection != null && 
    			isScaleIncreasing == false && 
    			isPositiveDirection == true && 
    			multiLeftEdge < entityLeftEdge) || 
    			(isScaleIncreasing != null && 
    			isPositiveDirection != null && 
    			isScaleIncreasing == false && 
    			isPositiveDirection == false &&
    			multiRightEdge > entityRightEdge)) {
    		
    		float[] size = new float[3];
			entity.getSize(size);
    		
    		if (isScaleIncreasing != null && 
    				isPositiveDirection != null && 
    				isScaleIncreasing == false && 
    				isPositiveDirection == false) {
    		
    			// This is case 2 (scale shrinking via the right anchor)
    			double correctedSize = 
    				multiRightEdge - entityLeftEdge;
    			
    			newScale[0] = ((float) correctedSize) / size[0];
    			newPos[0] += 
    				(multiRightEdge - entityRightEdge) / 2.0;
    			
    		} else if (isScaleIncreasing != null && 
    				isPositiveDirection != null && 
    				isScaleIncreasing == false && 
    				isPositiveDirection == true) {
    		
    			// This is case 3 (scale shrinking via the left anchor)
    			double correctedSize = 
    				entityRightEdge - multiLeftEdge;
    			
    			newScale[0] = ((float) correctedSize) / size[0];
    			newPos[0] += 
    				(multiLeftEdge - entityLeftEdge) / 2.0;
    		} else {
    			return;
    		}
    		
    		// Update the command
    		if (command instanceof ScaleEntityTransientCommand) {
    			
    			ScaleEntityTransientCommand scaleCmd = 
    				(ScaleEntityTransientCommand) command;
    			
    			scaleCmd.setPosition(newPos);
    			scaleCmd.setScale(newScale);
    			
    		} else if (command instanceof ScaleEntityCommand) {
    			
    			ScaleEntityCommand scaleCmd = 
    				(ScaleEntityCommand) command;
    			
    			scaleCmd.setNewPosition(newPos);
    			scaleCmd.setNewScale(newScale);
    		}
    	}
    }
    
}