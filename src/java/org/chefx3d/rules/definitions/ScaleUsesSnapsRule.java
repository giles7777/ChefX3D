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

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines if scale uses snaps to specific position values. Pipes execution
 * to the respective scale snap type if snaps are used.
 *
 * @author Ben Yarger
 * @version $Revision: 1.11 $
 */
public class ScaleUsesSnapsRule extends BaseRule  {
	
	/** Scale uses absolute snaps instance */
	private ScaleUsesAbsoluteSnapsRule scaleUsesAbsoluteSnaps;
	
	/** Scale uses incremental snaps instance */
	private	ScaleUsesIncrementalSnapsRule scaleUsesIncrementalSnaps;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleUsesSnapsRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
        
        scaleUsesAbsoluteSnaps = 
        	new ScaleUsesAbsoluteSnapsRule(
        			errorReporter, model, view);
        
        scaleUsesIncrementalSnaps = 
        	new ScaleUsesIncrementalSnapsRule(
        			errorReporter, model, view);
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
					ChefX3DRuleProperties.SCALE_USES_SNAPS_PROP);

		if(usesSnaps == null || usesSnaps.booleanValue() == false){
	        result.setResult(false);
	        return(result);
		}
		
		// Check if entity is using absolute snaps or incremental
		Boolean usesAbsoluteSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SCALE_USES_ABSOLUTE_SNAPS_PROP);

		if (usesAbsoluteSnaps != null && usesAbsoluteSnaps == true) {
			
			return (scaleUsesAbsoluteSnaps.performCheck(
						entity, command, result));
		}
		
		Boolean usesIncrementalSnaps = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.SCALE_USES_INCREMENTAL_SNAPS_PROP);

		if (usesIncrementalSnaps != null && usesIncrementalSnaps == true) {
			
			return (scaleUsesIncrementalSnaps.performCheck(
					entity, command, result));
		}

        result.setResult(true);
        return(result);
	}
}
