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

package org.chefx3d.rules.rule;

//External Imports

//Internal Imports
import org.chefx3d.model.Command;
import org.chefx3d.rules.rule.RuleEvaluationResult;

/**
 * Abstract definition for CommandInterpreters. CommandInterpreters take 
 * Commands and based on the command type pass the Command to a specific
 * RuleEngine for processing. If approveCommand returns false, the command
 * is terminated and not processed.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public abstract class CommandInterpreter {

	/**
	 * All instances send the Command to approveCommand to start the 
	 * analysis process against the RuleEngine.
	 * 
	 * @param command
	 * @return true if Command can continue, false otherwise
	 */
	public RuleEvaluationResult approveCommand(Command command){
		
		System.out.println("CommandInterpreter received command.");
		return matchCommand(command);
	}
	
	/**
	 * matchCommand should contain or delegate the logic required to assign
	 * the appropriate RuleEngine to use.
	 * 
	 * @param command
	 * @return true if Command can continue, false otherwise
	 */
	protected abstract RuleEvaluationResult matchCommand(Command command);
}
