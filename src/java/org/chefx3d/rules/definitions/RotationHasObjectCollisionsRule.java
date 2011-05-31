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
import org.chefx3d.model.RotateEntityCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* Determines if Entity is colliding with other objects.
*
* @author Ben Yarger
* @version $Revision: 1.28 $
*/
public class RotationHasObjectCollisionsRule extends BaseRule {

	/** Status message when mouse button released and collisions exist */
	private static final String ROTATE_COL_PROP =
		"org.chefx3d.rules.definitions.RotationHasObjectCollisionsRule.cannotRotate";

	/** Status message when illegal collisions exist for rotate commands */
	private static final String ROTATE_TRANS_COL_PROP =
		"org.chefx3d.rules.definitions.RotationHasObjectCollisionsRule.collisionsExist";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public RotationHasObjectCollisionsRule(
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
		 * Perform collision check - initial point at which a check should
		 * be performed for rotation rules.
		 */
		rch.performExtendedCollisionCheck(command, true, false, false);

		// If collisionEntities is null (no collisions occurred) then return false
		if(rch.collisionEntitiesMap == null){

            result.setResult(false);
            return(result);
		}

		// Perform collision analysis
		rch.performExtendedCollisionAnalysisHelper(null, false, null);

		/*
		 * If we are colliding with objects not in the relationship
		 * classification specified, illegal collisions exist.
		 */
		if(rch.hasIllegalCollisionExtendedHelper()){

			if(command instanceof RotateEntityCommand){

				resetToOriginalRotation(
						(RotateEntityCommand) command,
						(PositionableEntity)entity);

				String msg = intl_mgr.getString(ROTATE_COL_PROP);
				popUpMessage.showMessage(msg);

	            result.setResult(false);
	            return(result);
			} else {

				String msg = intl_mgr.getString(ROTATE_TRANS_COL_PROP);
				statusBar.setMessage(msg);
	            result.setResult(true);
	            return(result);
			}
		}

        result.setResult(false);
        return(result);
	}

	//---------------------------------------------------------------
	// Local methods
	//---------------------------------------------------------------

	/**
	 * Resets the entity back to the last known good rotation.
	 *
	 * @param mvCommand RotateEntityCommand causing the entity to move
	 * @param posEntity PositionableEntity
	 */
	private void resetToOriginalRotation(
			RotateEntityCommand rotCommand,
			PositionableEntity posEntity){

		float[] startingRotation = new float[4];
		posEntity.getStartingRotation(startingRotation);

		rotCommand.setCurrentRotation(
				startingRotation[0],
				startingRotation[1],
				startingRotation[2],
				startingRotation[3]);
	}
}