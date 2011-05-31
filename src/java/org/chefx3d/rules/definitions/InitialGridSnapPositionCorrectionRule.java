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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Initial position correction to place models on an origin centered grid.
 *
 * @author Rex Melton
 * @version $Revision: 1.6 $
 */
public class InitialGridSnapPositionCorrectionRule extends BaseRule {

	/** Default grid spacing */
	private static final double DEFAULT_GRID_SPACE = 0.003175;

	/** Scratch arrays */
	private double[] e_pos;
	
    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public InitialGridSnapPositionCorrectionRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;

		e_pos = new double[3];
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

        // Only operate on entities of type MODEL

        if (entity.isModel()) {

			if (command instanceof MoveEntityCommand) {

				MoveEntityCommand mec = (MoveEntityCommand)command;
            	mec.getEndPosition(e_pos);

				if (snapToGrid(e_pos, DEFAULT_GRID_SPACE)) {
					mec.setEndPosition(e_pos);
				}
			} else if (command instanceof MoveEntityTransientCommand) {

				MoveEntityTransientCommand metc = (MoveEntityTransientCommand)command;
            	metc.getPosition(e_pos);

				if (snapToGrid(e_pos, DEFAULT_GRID_SPACE)) {
					metc.setPosition(e_pos);
				}
			} else if (command instanceof TransitionEntityChildCommand) {

				TransitionEntityChildCommand tecc = (TransitionEntityChildCommand)command;
            	tecc.getEndPosition(e_pos);

				if (snapToGrid(e_pos, DEFAULT_GRID_SPACE)) {
					tecc.setEndPosition(e_pos);
				}
			}
        }

		result.setResult(true);
        return(result);
	}

    //--------------------------------------------------------------------
    // Local methods
    //--------------------------------------------------------------------

	/**
	 * Modify the argument position value to align with the specified
	 * grid spacing.
	 *
	 * @param pos The position
	 * @grid_spacing The spacing between grid cells in meters
	 * @return true if the pos array has been modified, false if the
	 * array remains unchanged.
	 */
	private boolean snapToGrid(double[] pos, double grid_spacing) {

		// distance from snap position in x and y
		double rem_x = pos[0] % grid_spacing;
		double rem_y = pos[1] % grid_spacing;

		boolean change = false;
		// round up or down to the next snap
		if (rem_x != 0) {
			double per_cent_rem = Math.abs(rem_x / grid_spacing);
			if (per_cent_rem >= 0.5) {
				if (rem_x > 0) {
					pos[0] += grid_spacing - rem_x;
				} else {
					pos[0] -= grid_spacing + rem_x;
				}
			} else {
				pos[0] -= rem_x;
			}
			change = true;
		}
		if (rem_y != 0) {
			double per_cent_rem = Math.abs(rem_y / grid_spacing);
			if (per_cent_rem >= 0.5) {
				if (rem_y > 0) {
					pos[1] += grid_spacing - rem_y;
				} else {
					pos[1] -= grid_spacing + rem_y;
				}
			} else {
				pos[1] -= rem_y;
			}
			change = true;
		}
		return(change);
	}
}
