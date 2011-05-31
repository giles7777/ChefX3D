/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
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

// External Imports
// None

//Internal Imports
// None

/**
 * Defines the requirements for screening Commands before they are
 * placed on the execution queue of the ValidatingBufferedCommandController.
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public interface CommandValidator {
	
	/**
	 * Return whether this command passes the validation process.
	 * 
	 * @param command The command to check.
	 */
	public boolean validate(Command command);
	
	/**
	 * Get the command that was created by the validation process 
	 * 
	 * @return The transformed command
	 */
	public Command getValidatedCommand();
	
}
