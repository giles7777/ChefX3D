package org.chefx3d.model;

import java.util.ArrayList;
import java.util.Stack;

import org.chefx3d.util.ErrorReporter;

public class UndoCommand 
	implements CommandWrapper {

    /** The command to call */
    private Command command;
    
    /** The size of the stack of <code>Command</code>s. */
    private int maxSize;

    /** The list of commands to undo */
    private Stack<Command> undoCommands;
    
    /** The list of commands to redo */
    private Stack<Command> redoCommands;
    
    /** The list of listeners to notify */
    private ArrayList<CommandListener> commandListeners;
    
    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * Constructor
     * 
     * @param command
     * @param maxSize
     * @param undoCommands
     * @param redoCommands
     * @param commandListeners
     * @param errorReporter
     */
    public UndoCommand(
            Command command, 
            int maxSize, 
            Stack<Command> undoCommands, 
            Stack<Command> redoCommands,
            ArrayList<CommandListener> commandListeners,
            ErrorReporter errorReporter) {
        
        this.command = command;
        this.maxSize = maxSize;
        this.undoCommands = undoCommands;
        this.redoCommands = redoCommands;
        this.commandListeners = commandListeners;
        this.errorReporter = errorReporter;
        
    }
    
    /**
     * Perform the command method
     */
    public void process() {
        
//System.out.println("UndoCommand.undo()");
//System.out.println("    command: " + command.getDescription());
//System.out.println("    isTransient: " + command.isTransient());
//System.out.println("    isUndoable: " + command.isUndoable());

        try {
                 
            command.undo();

            // Move the command to the top of the redo stack
            redoCommands.push(command);

            // Trim the redo stack
            if (redoCommands.size() > maxSize) {
                redoCommands.setSize(maxSize);
            }               
    
            // finally, notify listeners of the change
            for (int i = 0; i < commandListeners.size(); i++) {
                CommandListener l = commandListeners.get(i);
                l.commandExecuted(command);
            }

        } catch (Exception e) {
            errorReporter.errorReport("Undo of " + command.getDescription() + " command failed.", e);
        }

    }    
  
    /**
     * Reject the command
     */
    public void reject() {
        
        // finally, notify listeners of the change
        for (int i = 0; i < commandListeners.size(); i++) {
            CommandListener l = commandListeners.get(i);
            l.commandFailed(command);
        }

    }
    
    /**
     * Update the command that is wrapped by this
     * 
     * @param update
     */
    public void updateCommand(Command command) {
        this.command = command;
    }


}

