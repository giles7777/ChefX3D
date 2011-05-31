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

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.AutoAddInvisibleChildrenUtility;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* Determines if Entity has legal collision conditions before adding.
* If in collision with other objects, check the relationship and permit
* if relationship exists for the collision.
*
* @author Ben Yarger
* @version $Revision: 1.61 $
*/
public class AddEntityCheckForCollisionsRule extends BaseRule {

	/** Illegal collision conditions exist */
	private static final String ILLEGAL_COL_PROP =
		"org.chefx3d.rules.definitions.AddEntityCheckForCollisionsRule.illegalCollisions";

	/** Collisions required and not found */
	private static final String MISSING_COL_PROP =
		"org.chefx3d.rules.definitions.AddEntityCheckForCollisioinsRule.missingCollisions";

	/** Scratch objects for handling children entity collision calculations */
	private Matrix3f rot_mtx;
	private AxisAngle4f rotation;
	private Vector3f translation;
	private double[] pos_array;
	private float[] rot_array;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public AddEntityCheckForCollisionsRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;

        rot_mtx = new Matrix3f();
        rotation = new AxisAngle4f();
        translation = new Vector3f();
        pos_array = new double[3];
        rot_array = new float[4];
    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

    /**
     * Perform the check
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

		// If any of these are null, do not proceed with collision check
		if(classRelationship == null ||
				relationshipAmount == null ||
				relModifier == null){

            result.setResult(true);
            return(result);

		}

        if (classRelationship.length == 1 &&
                classRelationship[0].equals("empty")){

            result.setResult(true);
            return(result);

        }

		// Perform collision check - initial point at which a check should
		// be performed for movement rules.
		rch.performCollisionCheck(command, true, false, false);

		// Debug print statement
		//rch.printCollisionEntitiesList(true);

		// If collisionEntities is null (no collisions occurred) then return
		// check to see if classRelationship is set to empty. If it is
		// return true, otherwise return false
		if (rch.collisionEntities == null || 
				rch.collisionEntities.size() <= 0) {

			if (rch.legalZeroCollisionCheck(entity)) {
	            result.setResult(true);
	            return(result);
		    }

			String illegalCol = intl_mgr.getString(MISSING_COL_PROP);
			popUpMessage.showMessage(illegalCol);

            result.setResult(false);
            result.setApproved(false);
            result.setNotApprovedAction(
            		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
            return(result);

		}

		// Analyze class relationship data against collisions
		// (first time needs to be performed)
		rch.performCollisionAnalysisHelper(entity, null, false, null, true);

		if (rch.hasIllegalCollisionHelper(entity)) {

		    // If we did not find a legal collision then provide response
	        String illegalCol = intl_mgr.getString(ILLEGAL_COL_PROP);
	        popUpMessage.showMessage(illegalCol);

            result.setResult(false);
            result.setApproved(false);
            result.setNotApprovedAction(
            		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
            return(result);

		}

		// check the children if any
		if (entity.hasChildren() && (entity instanceof PositionableEntity)) {
			

			    // add a temp object to the scene for collision analysis
			    SceneManagementUtility.addTempSurrogate(
			    		rch.getRuleCollisionChecker(), command);

				boolean valid = checkChildren((PositionableEntity)entity);
	            result.setResult(valid);
	            if (!valid) {
	                result.setApproved(false);
	                result.setNotApprovedAction(
	                		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
	            }
	            
	            return(result);

		
		} else {
            result.setResult(true);
            return(result);

		}
	}

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
    
	/**
	 * Check the collision status of the children of the
	 * specified parent Entity. First create temporary surrogates for all of the
	 * children, and then test the collisions.
	 *
	 * @param parent The entity whose children to check
	 * @return True if children check out correctly, false otherwise
	 */
	private boolean checkChildren(PositionableEntity parent) {
	
		// First things first, we must recursively add all of the children that
		// are in the branch under the parent. Then we can attempt to do a 
		// check of the collision state of the branch.
		checkChildrenCreateSurrogates(parent);
		
		// Now call the collision check
		return checkChildrenCollisions(parent);

	}
	
	/**
	 * Create a temporary surrogate for each child in the branch.
	 * 
	 * @param parent Parent with children to create surrogates for
	 */
	private void checkChildrenCreateSurrogates(PositionableEntity parent) {
		
		ArrayList<Entity> children = parent.getChildren();
		
		for (Entity child : children) {
			
			if (!(child instanceof PositionableEntity)) {
				continue;
			}
		
			Command cmd = new AddEntityChildCommand(
					model,
					model.issueTransactionID(),
					parent,
					child,
					true);
			
			SceneManagementUtility.addTempSurrogate(collisionChecker, cmd);
			
			checkChildrenCreateSurrogates((PositionableEntity)child);
		}
	}

	/**
	 * Recursively check the collision status of the children of the
	 * specified parent Entity.
	 *
	 * @param parent The entity whose children to check
	 * @return This rule's evaluation of the children. Any false value found
	 * will return immediately without evaluating any further.
	 */
	private boolean checkChildrenCollisions(PositionableEntity parent) {

		boolean rval = true;

		ArrayList<Entity> children = parent.getChildren();

		int num_children = children.size();
		
		for (int i = 0; i < num_children; i++) {
			
			Entity e = children.get(i);
			
			// Skip auto add invisible children
			if(AutoAddInvisibleChildrenUtility.isAutoAddChildOfParent(
					model, e, parent)) {
				continue;
			}
			
			if (e instanceof PositionableEntity) {

				// check the child for collisions
				Command cmd = new AddEntityChildCommand(
					model,
					model.issueTransactionID(),
					parent,
					e,
					true);
				
				rch.performCollisionCheck(cmd, true, true, false);
				
				rch.performCollisionAnalysisHelper(e, null, false, null, true);
				
				boolean value = true;
				
				if (rch.hasIllegalCollisionHelper(e)) {
					value = false;
				}

				// terminate if illegal collision is found
				if (!value) {
					rval = false;
					break;
				} else if (e.hasChildren() &&
					!checkChildren((PositionableEntity)e)) {
					rval = false;
					break;
				}
			}
		}
		return(rval);
	}

	/**
	 * Configure and return a matrix for the specified entity
	 *
	 * @param pe The entity to produce a matrix for
	 * @return The entity's transform matrix
	 */
	private Matrix4f getTransformMatrix(PositionableEntity pe) {

		Matrix4f mtx = new Matrix4f();
		mtx.setIdentity();

		pe.getRotation(rot_array);
		pe.getPosition(pos_array);

		// configure the transform matrix
		rotation.set(rot_array);
		translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);

		mtx.setRotation(rotation);
		mtx.setTranslation(translation);

		return(mtx);
	}
}

