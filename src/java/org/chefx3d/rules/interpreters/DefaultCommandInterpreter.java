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

package org.chefx3d.rules.interpreters;

//External Imports

//Internal Imports
import org.chefx3d.model.Command;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.rule.RuleEngine;

import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.DefaultRuleEvaluationResult;
import org.chefx3d.rules.rule.CommandDataCenter;
import org.chefx3d.rules.rule.CommandInterpreter;

import org.chefx3d.util.CheckStatusReportElevation;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Default implementation of CommandInterpreter. Has a list of commands to 
 * look for and will search the list for a matching command. If found, the
 * assigned RuleEngine is extracted and used to process the command.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.13 $
 */
public class DefaultCommandInterpreter extends CommandInterpreter {
	
	/** RuleEngine reference */
	private RuleEngine ruleEngine;
	
	/** Error reporter reference */
	private ErrorReporter errorReporter;
	
	/** CommandDataCenter reference for looking up Command/RuleEngine matches */
	private CommandDataCenter commandDataCenter;
	
	/** Manages the status levels to report back as color to the selection box */
	private CheckStatusReportElevation statusManager;
	
	/**
	 * Default constructor
	 */
	public DefaultCommandInterpreter(
			ErrorReporter errorReporter,
			WorldModel model,
			EditorView view){
		
		this.errorReporter = errorReporter;
		statusManager = new CheckStatusReportElevation();
		commandDataCenter = new DefaultCommandDataCenter(
				errorReporter,
				model,
				view, 
				statusManager);
	}

	/**
	 * Looks for the command in the commandDataList and if a match is made
	 * grabs the RuleEngine and sends the command to be processed.
	 */
	@Override
	protected RuleEvaluationResult matchCommand(Command command) {
		
		ruleEngine = commandDataCenter.matchCommand(command);
		statusManager.resetElevationLevel();
		
        RuleEvaluationResult eval = 
            new DefaultRuleEvaluationResult();

		if(ruleEngine != null){
			
			eval = ruleEngine.processRules(command, eval);
		}
		
		statusManager.setElevationLevel(eval.getStatusValue());
		
		return eval;
	}
}
