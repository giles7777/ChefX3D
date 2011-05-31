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

import org.chefx3d.rules.rule.Rule;
import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if movement uses snaps and if it does perform the correct
 * snapping rules based on the properties defined
 *
 * @author Russell Dodds
 * @version $Revision: 1.6 $
 */
public class MovementSnapsRuleGroup extends BaseRuleGroup {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementSnapsRuleGroup(
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
        Rule rule = new MovementUsesSnapsRule(errorReporter, model, view);

        result = rule.processRule(command, result);

        // they are so now we need to check the type being used
        if(result.getResult()){

            // check absolute snaps first
            rule =
                new MovementUsesAbsoluteSnapsRule(errorReporter, model, view);

            result = rule.processRule(command, result);

            if (result.getResult()) {
                // we are done since absolute snaps are being used
            } else {

                // now check incremental snaps
                rule =
                    new MovementUsesIncrementalSnapsRule(errorReporter, model, view);

                result = rule.processRule(command, result);

                if (result.getResult()) {

                    // Check for a collision resulting from the special case snap
                    // See if there is a special case snap specified that will
                    // prevent the collision.

                    rule =
                        new MovementUsesIncrementalSpecialCaseSnapsRule(errorReporter, model, view);

                    result = rule.processRule(command, result);

                }
            }
        }

        // results are only used to decide which to use, should always return true
        result.setResult(true);
        return result;
	}
}
