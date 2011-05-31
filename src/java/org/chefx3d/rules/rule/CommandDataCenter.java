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

/**
 * Defines the requirements for mapping Commands to RuleEngines.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public interface CommandDataCenter {
	
    /**
     * Look for a rule engine associated with the command type passed in.
     *
     * @param command The command to match
     * @return RuleEngine The associated RuleEngine, or null if no match
     */
    public RuleEngine matchCommand(Command command);
}
