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
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* Determines if Entity is colliding with other objects. If so, will return
* true from test.
*
* @author Ben Yarger
* @version $Revision: 1.23 $
*/
class MovementCheckForObjectCollisionsRule extends BaseRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MovementCheckForObjectCollisionsRule(
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

		// Perform collision check - initial point at which a check should
		// be performed for movement rules.
		rch.performExtendedCollisionCheck(command, true, false, false);

		// Debug
//		rch.printCollisionEntitiesMap();

		// If there were no collisions, return false
		if (rch.collisionEntitiesMap == null) {
            result.setResult(false);
            return(result);
		}

		//  If there are no collisions and collisions are expected
		//  return false. If the reserved relationship of None is
		// applied we will grant an exception.
		if (!rch.legalZeroCollisionExtendedCheck()) {
            result.setResult(false);
            return(result);
		}

        result.setResult(true);
        return(result);
	}
}
