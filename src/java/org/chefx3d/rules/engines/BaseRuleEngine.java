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

package org.chefx3d.rules.engines;

// External Imports
import java.util.LinkedHashMap;

// Internal Imports
import org.chefx3d.model.Command;

import org.chefx3d.rules.rule.Rule;
import org.chefx3d.rules.rule.RuleEngine;
import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.ui.StatusBar;

import org.chefx3d.util.CheckStatusReportElevation;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Abstract implementation for RuleEngine. RuleEngine contains the rules in the
 * optimized sequence to be able to exit the logic path at the earliest 
 * possible point of failure, if it is going to fail the rules checks.
 *
 * @author Ben Yarger
 * @version $Revision: 1.7 $
 */
public abstract class BaseRuleEngine implements RuleEngine {
	
	/** Error reporting utility */
	protected ErrorReporter errorReporter;
	
	/** AV3DView instance */
	protected EditorView view;
	
	/** A double linked list map of rules to process */
	protected LinkedHashMap<String, Rule> ruleList;
	
    /** Manages the status level reporting */
    protected CheckStatusReportElevation statusManager;

	/** StatusBar messaging */
	protected StatusBar statusBar;
	
    /**
     * The default constructor
     * 
     * @param errorReporter The error reporter
     * @param view The scene graph
	 * @param statusManager The status manager
     * @param ruleList The list of rules to process
     */
	public BaseRuleEngine(
		ErrorReporter errorReporter,
		EditorView view, 
		CheckStatusReportElevation statusManager, 
		LinkedHashMap<String, Rule> ruleList){

		this.errorReporter = errorReporter;
		this.view = view;
		this.statusManager = statusManager;
		this.ruleList = ruleList;
		
		statusBar = StatusBar.getStatusBar();
	}

    //---------------------------------------------------------------
    // Methods defined by RuleEngine
    //---------------------------------------------------------------
    
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
			RuleEvaluationResult result){
		
		statusBar.clearStatusBar();
		
		return executeRuleLogic(command, result);
	}
	
    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------
    
	/**
	 * Abstract definition that takes a command and performs the ordered
	 * calls to the appropriate rule adapters.
	 *
	 * @param command The Command to process
	 * @param result The object to initialize with the evaluation result
	 * @return The result object
	 */
	protected abstract RuleEvaluationResult executeRuleLogic(
			Command command, 
			RuleEvaluationResult result);
	
}
