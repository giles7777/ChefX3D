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
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if movement uses snaps to specific position values.
 *
 * @author Ben Yarger
 * @version $Revision: 1.29 $
 */
class MovementUsesSnapsRule extends BaseRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementUsesSnapsRule(
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

		Boolean usesSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.MOVEMENT_USES_SNAPS_PROP);

		if(usesSnaps == null || usesSnaps.booleanValue() == false){
            result.setResult(false);
            return(result);
		}

		// If proximity snaps are in use, don't apply position snaps.
        // Go with what the proximity snap applies instead.
        String proximitySnapClass = (String)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SNAP_TO_CLASS_PROP);

        if (proximitySnapClass != null) {
        	result.setResult(false);
            return(result);
        }

		// Do independent collision check
		rch.performCollisionCheck(command, true, false, false);

		// If collisionEntities is null (no collisions occurred) then return false
		if(rch.collisionEntities == null){

            result.setResult(false);
            return(result);
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

		// If any of these are null, do not proceed
		if(classRelationship == null ||
				relationshipAmount == null ||
				relModifier == null){

            result.setResult(false);
            return(result);
		}

		// Maximum index limit for loops
		int maxIndex = Math.min(
				(Math.min(classRelationship.length, relationshipAmount.length)),
				relModifier.length);

		/*
		 * Perform collision analysis
		 */
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
		 * If we are colliding with objects not in the relationship
		 * classification specified, illegal collisions exist.
		 */
		// Russell: commenting out since the item only
		// needs to be colliding with the parent, other
		// collision are allowed.  After the final placement
		// by the snap rules, then all collisions must be valid.
//		if(rch.hasIllegalCollisions(
//				classRelationship,
//				relationshipAmount,
//				relModifier)){
//
//		    if(!(command.isTransient()))
//		        return false;
//		}
		ArrayList<Entity> floorEntityMatches =
        	collisionResults.getFloorEntityMatches();

        ArrayList<Entity> wallEntityMatches =
        	collisionResults.getWallEntityMatches();

		// Extract the snap to model restriction
		Boolean snapToModelOnly = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SNAP_TO_MODEL_ONLY);

		// If snap to model restriction is in play, and we have legal wall or
		// floor collisions, do not perform snap calculations
		if(snapToModelOnly) {

			if(wallEntityMatches.size() > 0 ||
					floorEntityMatches.size() > 0){

                result.setResult(false);
                return(result);
			}
		}

		// Extract the free float property
		Boolean freeFloating = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.FREE_FLOATING_CONSTRAINED);

		// If this entity is a free floating entity, and there are no collision
		// sets to work with, don't apply the snaps.
		if (freeFloating) {

			if(wallEntityMatches.size() == 0 &&
					floorEntityMatches.size() == 0 &&
					collisionResults.getEntityMatches().size() == 0){

                result.setResult(false);
                return(result);
			}
		}

        result.setResult(true);
        return(result);
	}
}
