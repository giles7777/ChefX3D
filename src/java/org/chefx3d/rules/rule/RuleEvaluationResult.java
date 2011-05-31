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
import java.util.List;

//Internal Imports
import org.chefx3d.rules.rule.Rule;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

/**
 * Defines the requirements for delivering the results of a rules evaluation
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public interface RuleEvaluationResult {
	
	/**
	 * Actions performed if a result is not approved.
	 * </br></br>
	 * CLEAR_ALL_COMMANDS - Clear all commands and reset the original to start
	 * </br>
	 * CLEAR_NEWLY_ISSUED_COMMANDS - Clear the newly issued commands and reset
	 * the original to start
	 * </br>
	 * CLEAR_CURRENT_COMMAND - Clear the current command and reset the original
	 *  to start, do not process newly issued commands
	 *  CLEAR_CURRENT_COMMAND_DO_NEWLY_ISSUED_COMMANDS - Clear the current 
	 *  command and reset the original to start, process newly issued commands
	 * </br>
	 * CLEAR_CURRENT_COMMAND_NO_RESET - Clear the current command, don't reset
	 * the original to start
	 * </br>
	 * RESET_TO_START_ALL_COMMANDS - Remove any add and remove commands from the
	 * command queue and then reset to start the remaining commands, moving
	 * all newly issued and pending commands to the approved stack to get
	 * sent out. Will also remove any nudge commands.
	 */
	public static enum NOT_APPROVED_ACTION {
		CLEAR_ALL_COMMANDS, 
		CLEAR_NEWLY_ISSUED_COMMANDS, 
        CLEAR_CURRENT_COMMAND,
        CLEAR_CURRENT_COMMAND_DO_NEWLY_ISSUED_COMMANDS,
		CLEAR_CURRENT_COMMAND_NO_RESET,
		RESET_TO_START_ALL_COMMANDS};
	
	/**
	 * Return the pass / fail state.  This indicates whether all the rules 
	 * were passed.
	 * 
	 * @return true for pass, false for fail
	 */
	public boolean getResult();
	
	/**
	 * Set the pass / fail state
	 * 
	 * @param state true for pass, false for fail
	 */
	public void setResult(boolean state);
	
    /**
     * Return the approved state.  If true then process the next rule, 
     * otherwise if false stop processing the rule list.  This should
     * be true at the end of the entire chain of rules
     * 
     * @return true for continue, false for stop
     */
    public boolean isApproved();
    
    /**
     * Set the continue / stop state.
     * 
     * @param approved true for process next rule, false for stop processing rules
     */
    public void setApproved(boolean approved);

    /**
     * Return the enumerated NOT_APPROVED_ACTION to perform.  
     * 
     * @return NOT_APPROVED_ACTION
     */
    public NOT_APPROVED_ACTION getNotApprovedAction();
    
    /**
     * Set the NOT_APPROVED_ACTION to apply to a result that is not approved.
     * 
     * @param NOT_APPROVED_ACTION to apply
     */
    public void setNotApprovedAction(NOT_APPROVED_ACTION action);
	
	/**
	 * Return the status level
	 * 
	 * @return The status level enum
	 */
	public ELEVATION_LEVEL getStatusValue();
	
	/**
	 * Set the status level
	 * 
	 * @param status The status level enum
	 */
	public void setStatusValue(ELEVATION_LEVEL status);

	/**
	 * Return the status message
	 * 
	 * @return The status message
	 */
	public String getStatusMessage();
	
	/**
	 * Set the status message
	 * 
	 * @param message The status message
	 */
	public void setStatusMessage(String message);
	
	/**
	 * Add a rule that returned a false result to the list of failed rules
	 * 
	 * @param rule
	 */
	public void addFailedRule(Rule rule);
	
	/**
	 * Get the complete list of failed rules 
	 * 
	 * @return
	 */
	public List<Rule> getFailedRules();
}
