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
 * Defines the requirements for implementing a rule processor.
 * RuleEngine contains the rules in the
 * optimized sequence to be able to exit the logic path at the earliest 
 * possible point of failure, if it is going to fail the rules checks.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public interface RuleEngine {
	
	/**
	 * Takes a command and performs the ordered 
	 * calls to the appropriate rule adapters.
	 * 
	 * @param command The Command to process
	 * @param result The object to initialize with the evaluation result
	 * @return The result object
	 */
	public RuleEvaluationResult processRules(
			Command command, 
			RuleEvaluationResult result);
}
