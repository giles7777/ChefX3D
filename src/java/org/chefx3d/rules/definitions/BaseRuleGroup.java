/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
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

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.RuleCollisionChecker;

/**
 * Abstract definition for a RuleGroup. RuleGroup specifies set single rules 
 * that are processed together as a cohesive group.
 *
 * @author Russell Dodds
 * @version $Revision: 1.6 $
 */
public abstract class BaseRuleGroup implements Rule {
	
    /** Error reporting utility */
    protected ErrorReporter errorReporter;

    /** Collision checking instance */
    protected RuleCollisionChecker collisionChecker;

    /** AV3DView instance */
    protected EditorView view;
			
    /** The world model, where the data is stored */
    protected WorldModel model;

	/**
	 * Constructor
	 * 
	 * @param errorReporter
	 * @param model
	 * @param view
	 */
	public BaseRuleGroup(
	        ErrorReporter errorReporter,
            WorldModel model,
            EditorView view) {
	    
        this.errorReporter = errorReporter;
		this.model = model;
        this.view = view;
        this.collisionChecker = view.getRuleCollisionChecker();
	    
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
    public abstract RuleEvaluationResult processRule(
            Command command, 
            RuleEvaluationResult result);

    //-------------------------------------------------------------------------
    // Public methods
    //-------------------------------------------------------------------------

    //--------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------

}
