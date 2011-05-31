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

package org.chefx3d.rules.interpreters;

//External Imports
import java.util.ArrayList;
import java.util.Arrays;

//Internal Imports
import org.chefx3d.model.ChangePropertyCommand;
import org.chefx3d.model.ChangePropertyTransientCommand;
import org.chefx3d.model.Command;
import org.chefx3d.rules.rule.RuleEngine;

/**
 * Defines data object for storing related objects that define Commands and
 * resolve to a specific RuleEngine. Used to encapsulate data for binding 
 * Command data to RuleEngine data.
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public class CommandInterpreterData {

	/** propertyName reference by Command that maps to RuleEngine **/
	private String propertyName;
	
	/** propertySheet referenced by Command that maps to RuleEngine **/
	private String propertySheet;
	
	/** Commands that map to RuleEngine **/
	private ArrayList<Class<? extends Command>> commandList;
	
	/** RuleEngine that Commands and property values map to */
	private RuleEngine ruleEngine;
	
	/**
	 * Primary constructor.
	 * 
	 * @param propertyName
	 * @param propertySheet
	 * @param commands
	 * @param ruleEngine
	 */
	public CommandInterpreterData(String propertyName, String propertySheet, ArrayList<Class<? extends Command>> commandList, RuleEngine ruleEngine){
		
		this.propertyName = propertyName;
		this.propertySheet = propertySheet;
		this.commandList = commandList;
		this.ruleEngine = ruleEngine;
	}
	
	/**
	 * propertyName setter method
	 * @param propertyName
	 */
	public void setPropertyName(String propertyName){
		this.propertyName = propertyName;
	}
	
	/**
	 * propertySheet setter method
	 * @param propertySheet
	 */
	public void setPropertySheet(String propertySheet){
		this.propertySheet = propertySheet;
	}
	
	/**
	 * Command[] setter method
	 * @param commands
	 */
	public void setCommands(Command[] commands){
		commandList = (ArrayList)Arrays.asList(commands);
	}
	
	/**
	 * RuleEngine setter method
	 * @param ruleEngine
	 */
	public void setRuleEngine(RuleEngine ruleEngine){
		this.ruleEngine = ruleEngine;
	}
	
	/**
	 * propertyName accessor method
	 * @return String propertyName
	 */
	public String getPropertyName(){
		return propertyName;
	}
	
	/**
	 * propertySheet accessor method
	 * @return String propertySheet
	 */
	public String getPropertySheet(){
		return propertySheet;
	}
	
	/**
	 * Command[] accessor method
	 * @return Command[]
	 */
	public Command[] getCommands(){
		
		return ((Command[])commandList.toArray());
	}
	
	/**
	 * RuleEngine accessor method
	 * @return RuleEngine ruleEngine
	 */
	public RuleEngine getRuleEngine(){
		return ruleEngine;
	}
	
	/**
	 * Checks for Command in stored Command array. If the command is a 
	 * ChangePropertyCommand a check against the propertyName and propertySheet
	 * is performed to see if we have a Command match.
	 * 
	 * @param command The command to check for
	 * @return True if found, false otherwise
	 */
	public boolean containsCommand(Command command){
		
		for(Class<? extends Command> existingCommand : commandList){

			if(existingCommand.isInstance(command)){
				
				if(command instanceof ChangePropertyCommand){
					
					String cmdPropertyName = ((ChangePropertyCommand)command).getPropertyName();
					String cmdPropertySheet = ((ChangePropertyCommand)command).getPropertySheet();
					
					if((cmdPropertyName.equals(propertyName) || propertyName.equals("")) && 
							cmdPropertySheet.equals(propertySheet)){
						
						return true;
					}
					
				} else if (command instanceof ChangePropertyTransientCommand){
					
					String cmdPropertyName = ((ChangePropertyTransientCommand)command).getPropertyName();
					String cmdPropertySheet = ((ChangePropertyTransientCommand)command).getPropertySheet();
					
					if((cmdPropertyName.equals(propertyName) || propertyName.equals("")) && 
							cmdPropertySheet.equals(propertySheet)){
						
						return true;
					}
					
				} else {
				
					return true;
				}
			}
		}
		
		return false;
	}
}
