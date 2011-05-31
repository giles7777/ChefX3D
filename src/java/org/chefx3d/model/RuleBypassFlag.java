/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

//External Imports

//Internal Imports

/**
 * Check used by rules to determine if the incoming command should be checked 
 * by the rules. Used to force command execution without any rule intervention.
 *
 * @author Ben Yarger
 * @version $Revision: 1.2 $
 */
public interface RuleBypassFlag {

	/**
	 * Should the rule engine allow the command to execute without any check.
	 * 
	 * @return True if rule evaluation should be skipped, false otherwise
	 */
	public boolean bypassRules();
	
	/**
     * Should the rule engine allow the command to execute without any check.
     * 
     * @return True if rule evaluation should be skipped, false otherwise
     */
    public void setBypassRules(boolean bypass);

}
