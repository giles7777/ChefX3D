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

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;

/**
* Inspects the circumstances of an object not having any collisions assocated.
* Particularly, we want to verify that it exists in a legal state and not
* outside the location volume.
*
* @author Ben Yarger
* @version $Revision: 1.26 $
*/
class MovementNoCollisionRule extends BaseRule {

	/** Illegal movement because of relationship requirements */
	private static final String REL_REQ_PROP =
		"org.chefx3d.rules.definitions.MovementNoCollisionRule.hasRelationshipRequirement";

	/** Illegal placement because of relationship requirements */
	private static final String ILLEGAL_MOVE_PROP =
		"org.chefx3d.rules.definitions.MovementNoCollisionRule.illegalMove";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementNoCollisionRule(
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

		// If it is length one and equal to empty return true
		if(classRelationship != null &&
				classRelationship.length == 1 &&
	            classRelationship[0].equals("empty")){
            result.setResult(true);
            return(result);
	    }

		/*
		 *  If the object is not transient, reset back
		 *  to the last known good position, otherwise
		 *  return false to change status.
		 */
		if(classRelationship != null){

			if(command instanceof MoveEntityCommand){

				String illegalMove = intl_mgr.getString(ILLEGAL_MOVE_PROP);
				popUpMessage.showMessage(illegalMove);

			} else if (command instanceof TransitionEntityChildCommand &&
					command.isTransient() == false){

				String illegalMove = intl_mgr.getString(ILLEGAL_MOVE_PROP);
				popUpMessage.showMessage(illegalMove);

			} else {

				String requiredRelationship = intl_mgr.getString(REL_REQ_PROP);
				statusBar.setMessage(requiredRelationship);

				result.setStatusValue(ELEVATION_LEVEL.SEVERE);

			}

			if (!command.isTransient()) {
				result.setApproved(false);
			}

            result.setResult(false);
            return(result);

		}

        result.setResult(true);
        return(result);
	}

	//---------------------------------------------------------------
	// Local methods
	//---------------------------------------------------------------

}
