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

package org.chefx3d.rules.rule;

//External Imports
import java.util.ArrayList;
import java.util.List;

//Internal Imports
import org.chefx3d.rules.rule.Rule;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

/**
 * The results of a rules evaluation
 *
 * @author Ben Yarger
 * @version $Revision: 1.2 $
 */
public class DefaultRuleEvaluationResult implements RuleEvaluationResult {
	
	/** The result */
	private boolean state;
	
    /** The approved flag */
    private boolean approved;
	
	/** The status value */
	private ELEVATION_LEVEL status;
	
	/** The status message */
	private String message;
	
	/** List of failed rules */
	private List<Rule> failedRules;
	
	/** 
	 * Action to take if not approved, default is set to 
	 * CLEAR_NEWLY_ISSUED_COMMANDS.
	 */
	private NOT_APPROVED_ACTION notApprovedAction;
	
	/**
	 * Constructor
	 */
	public DefaultRuleEvaluationResult() {
		this(true);
	}
	
	/**
	 * Constructor
	 */
	public DefaultRuleEvaluationResult(boolean state) {
		this.state = state;
		approved = true;
		status = ELEVATION_LEVEL.NONE;
		message = null;
		failedRules = new ArrayList<Rule>();
		notApprovedAction = NOT_APPROVED_ACTION.CLEAR_NEWLY_ISSUED_COMMANDS;
	}
	
	/**
	 * Return the pass / fail state
	 * 
	 * @return true for pass, false for fail
	 */
	public boolean getResult() {
		return(state);
	}
	
	/**
	 * Set the pass / fail state
	 * 
	 * @param state true for pass, false for fail
	 */
	public void setResult(boolean state) {
		this.state = state;
	}

    /**
     * Return the continue / stop state.  If true then process the next rule, 
     * otherwise if false stop processing the rule list.
     * 
     * @return true for continue, false for stop
     */
    public boolean isApproved() {
        return(approved);
    }
    
    /**
     * Set the continue / stop state.
     * 
     * @param next true for process next rule, false for stop processing rules
     */
    public void setApproved(boolean next) {
        this.approved = next;
    }

	/**
	 * Return the status array
	 * 
	 * @return The status array
	 */
	public ELEVATION_LEVEL getStatusValue() {
		return(status);
	}
	
	/**
	 * Set the status array
	 * 
	 * @param status The status array
	 */
	public void setStatusValue(ELEVATION_LEVEL status) {
		this.status = status;
	}

	/**
	 * Return the status message
	 * 
	 * @return The status message
	 */
	public String getStatusMessage() {
		return(message);
	}
	
	/**
	 * Set the status message
	 * 
	 * @param message The status message
	 */
	public void setStatusMessage(String message) {
		this.message = message;
	}
	
	/**
     * Add a rule that returned a false result to the list of failed rules
     * 
     * @param rule
     */
    public void addFailedRule(Rule rule) {
        failedRules.add(rule);
    }
    
    /**
     * Get the complete list of failed rules 
     * 
     * @return
     */
    public List<Rule> getFailedRules() {
        return failedRules;
    }

    /**
     * Return the enumerated NOT_APPROVED_ACTION to perform.  
     * 
     * @return NOT_APPROVED_ACTION
     */
	public NOT_APPROVED_ACTION getNotApprovedAction() {
		return notApprovedAction;
	}

	/**
     * Set the NOT_APPROVED_ACTION to apply to a result that is not approved.
     * 
     * @param NOT_APPROVED_ACTION to apply
     */
	public void setNotApprovedAction(NOT_APPROVED_ACTION action) {
		notApprovedAction = action;
	}
}
