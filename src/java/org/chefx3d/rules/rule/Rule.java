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

package org.chefx3d.rules.rule;

// External Imports
// none

// Internal Imports
import org.chefx3d.model.Command;
import org.chefx3d.rules.rule.RuleEvaluationResult;

/**
 * Defines the requirements for Rule. Rule specifies a single rule and the processes
 * that make up the rule. A Rule returns a boolean response about the condition
 * that determines if a Command can be executed or must be rejected and
 * possibly results in an automatically handled response in the environment.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public interface Rule {
	
    /** Enumerated rule types */
    public static enum RULE_TYPE {INVIOLABLE, STANDARD, INFORMATIONAL};

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
    public RuleEvaluationResult processRule(Command command, RuleEvaluationResult result);
    
}
