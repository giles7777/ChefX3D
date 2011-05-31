/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
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
import java.util.*;
import java.io.OutputStream;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for executing multiple commands together as one. 
 * Each command in the list will be executed/undone/redone as 
 * necessary.
 *
 * @author Russell Dodds
 * @version $Revision: 1.10 $
 */
public class MultiTransientCommand implements Command, RuleBypassFlag, RuleDataAccessor {

    /** The command list */
    private ArrayList<Command> commandList;
    
    /** Is this a local add */
    private boolean local;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** The rule bypass flag */
    private boolean ruleBypassFlag;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;

    /**
     * Create a multiple command list.
     *
     * @param commandList - The list of <code>Command</code>s
     * @param description - The description to display
     */
    public MultiTransientCommand(ArrayList<Command> commandList, String description) {
        
        // Cast to package definition to access protected methods
        this.commandList = (ArrayList<Command>)commandList.clone();
        this.description = description;
        
        local = true;

        init();
    }
    
    /**
     * Create a multiple command list.
     *
     * @param commandList - The list of <code>Command</code>s
     * @param description - The description to display
     * @param ruleBypassFlag Should the command bypass rules
     */
    public MultiTransientCommand(
    		ArrayList<Command> commandList, 
    		String description, 
    		boolean ruleBypassFlag) {
    	
    	this(commandList, description);

        this.ruleBypassFlag = ruleBypassFlag;
    }


    /**
     * Create a multiple command list.
     *
     * @param description - The description to display
     */
    public MultiTransientCommand(String description) {
        
        this.description = description;
        
        local = false;
        
        init();
    }


    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        undoableState = false;
        transientState = true;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        
        int len = commandList.size();
        for (int i = 0; i < len; i++) {
            
            // get the command
            Command command = commandList.get(i);
            
            // execute the command
            command.execute();
            
        }
        
    }

    /**
     * Set the local flag.
     *
     * @param isLocal Is this a local update
     */
    public void setLocal(boolean isLocal) {
        local = isLocal;
    }

    /**
     * Is the command locally generated.
     *
     * @return Is local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        // ignore
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        // ignore
    }

    /**
     * Get the text description of this <code>Command</code>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the text description of this <code>Command</code>.
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Get the transient state of this <code>Command</code>.
     */
    public boolean isTransient() {
        return transientState;
    }

    /**
     * Set the transient state of this <code>Command</code>.
     */
    public void setTransient(boolean bool) {
        transientState = bool;
    }

    /**
     * Get the transactionID for this command.
     *
     * @return The transactionID
     */
    public int getTransactionID() {
        return 0;
    }

    /**
     * Get the undo setting of this <code>Command</code>. true =
     * <code>Command</code> may be undone false = <code>Command</code> may
     * never undone
     */
    public boolean isUndoable() {
        return undoableState;
    }

    /**
     * Serialize this command.
     *
     * @param method What method should we use
     * @param os The stream to output to
     */
    public void serialize(int method, OutputStream os) {
        switch (method) {
        case METHOD_XML:
            errorReporter.messageReport("Unsupported serialization method");
            break;
        case METHOD_XML_FAST_INFOSET:
            errorReporter.messageReport("Unsupported serialization method");
            break;
        }
    }

    /**
     * Deserialize a stream
     *
     * @param st The xml string to deserialize
     */
    public void deserialize(String st) {
        errorReporter.messageReport("Unsupported deserializes method");
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
    
    /**
     * Get the list of commands.
     * 
     * @return ArrayList<Command> list of commands
     */
    public ArrayList<Command> getCommandList(){
    	
    	return commandList;
    }
    
    /**
     * Set the rule bypass flag value.
     * 
     * @param ruleBypassFlag True to bypass rules, false otherwise
     */
    public void setBypassRules(boolean ruleBypassFlag){
    	this.ruleBypassFlag = ruleBypassFlag;
    }

    /**
     * Get the rule bypass flag value.
     * 
     * @return boolean True to bypass, false otherwise
     */
	public boolean bypassRules() {

		return ruleBypassFlag;
	}

	public HashSet<String> getIgnoreRuleList() {
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }
    
    //--------------------------------------------------------------------
    // Routines required by RuleDataAccessor
    //--------------------------------------------------------------------

	public Entity getEntity() {
		// TODO Auto-generated method stub
		return null;
	}

	public WorldModel getWorldModel() {
		// TODO Auto-generated method stub
		return null;
	}

	public void resetToStart() {
		// TODO Auto-generated method stub
		
	}

	public void setCommandShouldDie(boolean die) {
		// TODO Auto-generated method stub
		
	}

	public boolean shouldCommandDie() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Compare external command to this one to see if they are the same.
	 * 
	 * @param externalCommand command to compare against
	 * @return True if the same, false otherwise
	 */
	public boolean isEqualTo(Command externalCommand) {
		
		// Check for appropriate command type(s)
		if (externalCommand instanceof MultiTransientCommand) {
			
			ArrayList<Command> externalCommandList = 
					((MultiTransientCommand)externalCommand).getCommandList();
			
			if (commandList.size() == externalCommandList.size()) {
				
				// Compare each individual command in order
				for (int i=0; i < commandList.size(); i++) {
					
					Command cmd = commandList.get(i);
					Command externalCmd = externalCommandList.get(i);
					
					if (!(cmd instanceof RuleDataAccessor)) {
						return false;
					} else if (!(externalCmd instanceof RuleDataAccessor)) {
						return false;
					}
					
					if (!((RuleDataAccessor)cmd).isEqualTo(externalCmd)) {
						return false;
					}
				}
				
			} else {
				return false;
			}
			
		} else {
			// Not a command we can every be equal to
			return false;
		}
		
		return true;
	}
	
	/**
	 * Override object's equals method
	 */
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof Command) {
			return isEqualTo((Command)obj);
		}
		
		return false;
	}
}