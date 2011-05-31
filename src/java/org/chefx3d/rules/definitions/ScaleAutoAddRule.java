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

import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.rules.util.*;

import org.chefx3d.tool.EntityBuilder;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Auto add children to parent base on the scale change of the parent.
 * Auto add can be by span or collision. For collision by span, if there are
 * fixed entities on the auto placed children, the resulting auto add will
 * preserve all children starting from the first fixed auto add child plus
 * all other children in the other direction of the scale. Transient scaling
 * will remove auto add children and the final non-transient scale will add
 * them at the correct interval.
 *
 * @author Ben Yarger
 * @version $Revision: 1.82 $
 */

public class ScaleAutoAddRule extends BaseRule {

	/** Flag if the scale has been clamped because of collision */
	private static boolean SCALE_CLAMP_APPLIED = false;

    /** Scale problem pop up message relating to auto add */
    private static final String POP_UP_NO_SCALE =
        "org.chefx3d.rules.definitions.ScaleAutoAddRule.scaleCanceled";

    /** Scale has been clamped because of collisions */
    private static final String STATUS_BAR_SCALE_CLAMPED =
        "org.chefx3d.rules.definitions.ScaleAutoAddRule.scaleClamped";


    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleAutoAddRule(
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
        
        // Don't perform auto add on miter entities
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (canMiterCut) {
        	result.setResult(true);
            return(result);
        }

        // Reset our class variables used during evaluation
        SCALE_CLAMP_APPLIED = false;

        // If there was some event that prevented the auto add operation we will
        // terminate the action and post a message.
        if (!updateAutoPlaceChildren(
        		model, (PositionableEntity) entity, command)) {

            String msg = intl_mgr.getString(POP_UP_NO_SCALE);
            popUpMessage.showMessage(msg);
            result.setApproved(false);
            result.setResult(true);
            return(result);
        }

        // If the scale clamp is applied, set the status bar message
        if (SCALE_CLAMP_APPLIED) {

            String msg = intl_mgr.getString(STATUS_BAR_SCALE_CLAMPED);
            statusBar.setMessage(msg);
        }

        result.setResult(SCALE_CLAMP_APPLIED);
        return(result);
    }

    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------
    //-------------------------------------------------------------------------

    /**
     * Update auto add children either by calling the appropriate auto add by
     * collision routines or using the span procedure.
     *
     * @param model WorldModel to reference
     * @param parentEntity Parent entity that has auto add children
     * @param command Command acting on parentEntity
     * @return True if successful, false otherwise
     */
    private boolean updateAutoPlaceChildren(
            WorldModel model,
            PositionableEntity parentEntity,
            Command command){
    	
    	// If not a scale command it has no business doing anything here
    	if (!(command instanceof ScaleEntityCommand) && 
    			!(command instanceof ScaleEntityTransientCommand)) {
    		return true;
    	}

    	// If the entity doesn't perform any auto add operations, bail
        if (!AutoAddUtility.performsAutoAddOperations(parentEntity)) {
        	return true;
        }
        
        // Transform to add to non auto add children to keep them in place
        double[] nonAutoAddTransform = new double[3];

        // If the command is transient, remove any non-fixed auto adds
        if (command.isTransient()) {
        	
        	Entity[] tmpRemovedChildren = 
        		AutoAddUtility.removeNonCriticalAutoAdds(
        				model, parentEntity, rch, view);
        	
        	// Side pocket any removed children to supply to the final
        	// scale command.
        	if (tmpRemovedChildren.length > 0) {
        		
        		for (int i = 0; i < tmpRemovedChildren.length; i++) {
        			parentEntity.addStartingChild(tmpRemovedChildren[i]);
        		}
        	}
        	
        	double[] newPos = new double[3];
        	((ScaleEntityTransientCommand)command).getPosition(newPos);
        	
        	parentEntity.getStartingPosition(nonAutoAddTransform);
        	
        	nonAutoAddTransform[0] -= newPos[0];
        	nonAutoAddTransform[1] -= newPos[1];
        	nonAutoAddTransform[2] -= newPos[2];
        	
        } else {
        	
        	ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;
        	
        	double[] newPos = new double[3];
        	scaleCmd.getNewPosition(newPos);
        	
        	scaleCmd.getOldPosition(nonAutoAddTransform);
        	
        	nonAutoAddTransform[0] -= newPos[0];
        	nonAutoAddTransform[1] -= newPos[1];
        	nonAutoAddTransform[2] -= newPos[2];
        	
        	// If we match the side pocketed scaling entity, attach any
        	// starting children to replace.
        	if (parentEntity.hasStartingChildren()) {
        		
        		ArrayList<Entity> startChildren = new ArrayList<Entity>();
        		ArrayList<PositionableData> startPositions = 
        			new ArrayList<PositionableData>();
        		
        		Entity[] removedChildren = parentEntity.getStartingChildren();
        		
        		for (int i = 0; i < removedChildren.length; i++) {
        			startChildren.add(removedChildren[i]);
        			startPositions.add(
        					((PositionableEntity)
        							removedChildren[i]).getPositionableData());
        		}
        		
        		scaleCmd.setStartChildren(startChildren, startPositions);

        	}
        	
        	parentEntity.clearStartingChildren();
        }
        
        // Adjust all non auto add children to keep them in the right place
/*        moveAllNonAutoAddChildren(
        		model, 
        		parentEntity, 
        		nonAutoAddTransform, 
        		command.isTransient());
*/
        // Prevent the collision of the scaling parent with other illegal
        // targets. Doing so will mung up the auto add stuff.
//        performScaleCollisionAdjustment(
//        		command,
//        		model);

        // Prevent scaling smaller than the minimum size allowed by auto adds
//        performMinimumAutoAddParentScaleCheck(

        

        PositionableEntity parentEntityParentEntity = (PositionableEntity)
        	SceneHierarchyUtility.getExactParent(model, parentEntity);

        EntityBuilder entityBuilder = view.getEntityBuilder();

        // Perform each of the auto add operations
        if (!AutoAddBySpanUtility.scaleAutoAdd(
        		model,
        		command,
        		parentEntity,
        		parentEntityParentEntity,
        		rch,
        		entityBuilder)) {

        	return false;
        }

        if(!AutoAddByCollisionUtility.scaleAutoAdd(
        		model,
        		command,
        		parentEntity,
        		parentEntityParentEntity,
        		null,
        		rch,
        		entityBuilder)) {

        	return false;
        }

        if (!AutoAddByPositionUtility.scaleAutoAdd(
        		model,
        		command,
        		parentEntity,
        		parentEntityParentEntity,
        		null,
        		rch,
        		entityBuilder)) {

        	return false;
        }

        if (!AutoAddEndsUtility.scaleAutoAdd(
        		model,
        		command,
        		parentEntity,
        		parentEntityParentEntity,
        		null,
        		rch,
        		entityBuilder)) {

        	return false;
        }

        return true;
    }

    /**
     * Move all of the other immediate children of the parent, that are not
     * auto added entities, by the position correction amount.
     *
     * @param model WorldModel to reference
     * @param parentEntity Parent entity to extract children from
     * @param fixedEntityCorrection Position correction amount
     * @param isTransient Is transient state flag
     */
    private void moveAllNonAutoAddChildren(
            WorldModel model,
            Entity parentEntity,
            double[] fixedEntityCorrection,
            boolean isTransient) {

         ArrayList<Entity> allChildren = 
        	 SceneHierarchyUtility.getExactChildren(parentEntity);

         for (Entity child : allChildren) {

            double[] startPos = new double[3];
            double[] endPos = new double[3];

            // Don't process any other auto adds
            Boolean isAutoAdd = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                        child,
                        ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

            if (isAutoAdd) {
                continue;
            }
            
            // check if the entity is a complex sub part
            Boolean isComplexSubPart =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                		child,
                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);
            
            if (isComplexSubPart) {
                continue;
            }

            // Must be a positionable entity for us to do anything with it
             if (child instanceof PositionableEntity) {

                 ((PositionableEntity)child).getStartingPosition(startPos);

             } else {
                 continue;
             }

             endPos[0] = startPos[0] + fixedEntityCorrection[0];
             endPos[1] = startPos[1] + fixedEntityCorrection[1];
             endPos[2] = startPos[2] + fixedEntityCorrection[2];

             SceneManagementUtility.moveEntity(
            		 model, 
            		 collisionChecker, 
            		 (PositionableEntity) child, 
            		 null, 
            		 endPos, 
            		 isTransient, 
            		 true);

         }
    }
    
}
