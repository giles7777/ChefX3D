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
import org.chefx3d.model.Command;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.Rule;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if movement uses snaps and if it does perform the correct
 * snapping rules based on the properties defined
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public class MovementCollisionRuleGroup extends BaseRuleGroup {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementCollisionRuleGroup(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

    }

    //---------------------------------------------------------------
    // Rule methods
    //---------------------------------------------------------------

    /**
     * All instances of Rule should call processRule with the
     * Command and transient state to convert and process.
     * Convert expects the Command to
     * implement RuleDataAccessor. If it doesn't it returns true and
     * logs a note to the console.
     *
     * @param command Command that needs to be converted
     * @param result The state of the rule processing
     * @return A RuleEvaluationResult object containing the results
     */
    public RuleEvaluationResult processRule(
            Command command,
            RuleEvaluationResult result) {

        // first check to see if snaps are even being used
        Rule movementCheckRule =
            new MovementCheckForObjectCollisionsRule(errorReporter, model, view);

        result = movementCheckRule.processRule(command, result);

        // if true, then continue
        // if false, check secondary
        // if false and true, then continue
        // if false and false, then stop processing


        // if the check failed perform a secondary check
        if(!result.getResult()){

            // check absolute snaps first
            movementCheckRule =
                new MovementNoCollisionRule(errorReporter, model, view);

            result = movementCheckRule.processRule(command, result);

            if (!result.getResult() && !command.isTransient()) {
                result.setApproved(false);
                result.setNotApprovedAction(
                		NOT_APPROVED_ACTION.CLEAR_ALL_COMMANDS);
            }

        }

        return result;
	}
}
