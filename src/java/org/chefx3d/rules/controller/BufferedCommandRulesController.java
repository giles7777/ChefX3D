/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.controller;

//External Imports

//Internal Imports
import org.chefx3d.model.BufferedCommandController;
import org.chefx3d.model.Command;
import org.chefx3d.model.ExecuteCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.interpreters.DefaultCommandInterpreter;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
* A history of commands changed within the model using rule engine to control 
* action of commands.
*
* @author Ben Yarger
* @version $Revision: 1.7 $
*/
public class BufferedCommandRulesController extends BufferedCommandController {

	/** Entry point to rules engine */
	private DefaultCommandInterpreter commandInterpreter;
	
	/**
     * Create a commandHistory object. Set the maximum size to
     * <code>DEFAULT_SIZE</code> and create the data structure to store the
     * <code>Command</code>s
     */
    public BufferedCommandRulesController(ErrorReporter errorReporter, WorldModel model, EditorView view) {
    	super();
    	this.errorReporter = errorReporter;
    	commandInterpreter = new DefaultCommandInterpreter(errorReporter, model, view);
    }

    /**
     * Create a commandHistory object. Set the maximum size to <code>size</code>
     * and create the data structure to store the <code>Command</code>s
     *
     * @param size The maximum size of the stack
     */
    public BufferedCommandRulesController(int size, ErrorReporter errorReporter, WorldModel model, EditorView view) {
    	super(size);
    	this.errorReporter = errorReporter;
    	commandInterpreter = new DefaultCommandInterpreter(errorReporter, model, view);
    }
    
    /**
     * Add a <code>Command</code> to the history list without performing it.
     *
     * @param Command the action to remember
     */
    public void execute(Command command) {

        RuleEvaluationResult eval = commandInterpreter.approveCommand(command);
        
    	/*
    	 * Check the command against the rules engine. If the command checks 
    	 * out then add it to the ExecuteCommand wrapper and put it on the 
    	 * stack. Otherwise, return without further processing. 
    	 */
    	if(!eval.getResult()){
    		return;
    	}
    	
        // Create the command wrapper
        ExecuteCommand cmd = new ExecuteCommand(
                command,
                maxSize,
                undoCommands,
                redoCommands,
                commandListeners,
                errorReporter);

        // Add the wrapper to the list of buffered commands
        bufferedCommands.add(cmd);

    }
}
