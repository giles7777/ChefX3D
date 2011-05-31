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
 
package org.chefx3d.rules.util;


//External Imports
import java.util.*;

//Internal Imports
import org.chefx3d.model.Command;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.MultiRevertCommand;
import org.chefx3d.model.MultiTransientCommand;
import org.chefx3d.model.TransitionEntityChildCommand;

/**
 * Singleton utility for tracking and maintaining command lists built up during
 * an evaluation set. 
 * 
 * There are three queues utilized. First is the approved
 * command queue. This command queue tracks commands that have passed their
 * rule engine evaluations.
 * 
 * Second is the pending command queue. This command queue tracks commands 
 * waiting to be examined by their respective rule engines.
 * 
 * Third is the newly issued command queue. This command queue tracks commands
 * issued by the current rule engine that have not yet been pushed onto the 
 * pending command stack.
 * 
 * Fourth is the cleansedMap. This tracks commands that need to be removed
 * before the actual removal occurs. The removal only occurs when the newly
 * issued commands are pushed onto the pending command queue.
 * 
 * Fifth is a side pocketed MultiCommand list. MultiCommands are immediately
 * expanded when added and only the individual commands are kept. The only way
 * to retrieve the original command is by calling getCommand() with the 
 * transactionID. If you want to remove a MultiCommand from the queues, call
 * the removeMultiCommand().
 * 
 * If a particular command fails, the newly issued command queue can be cleared
 * and still preserve the pending and approved commands. Because we also track
 * the commands to be cleansed instead of immediately removing them, we 
 * preserve the current command state correctly. In the event of a 
 * rejection of the command, both the newly issued command queue and the
 * cleansed map should be cleared.
 * 
 * For the command pulled of the stack into the newly issued command queue, 
 * there may be a single command
 * or a multi command that is expanded out to n number of commands. These
 * all go on the pending queue for the evaluation set. Each one that is
 * approved goes onto the approved queue. During a specific rule engine check
 * n number of newly issued commands may be generated. Until the rule engine
 * returns, these go on the newly issued command stack. As soon as the rule
 * engine completes its examination of the command, those newly issued commands
 * get pushed onto the end of the pending stack and wait for their turn to
 * be evaluated by their respective rule engines.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.12 $
 */
public class CommandSequencer {
	
	/** Approved commands waiting to be bundled and executed */
	private Queue<Command> approvedCommands;
	
	/** Commands waiting to be evaluated */
	private Queue<Command> pendingCommands;
	
	/** Newly issued commands generated during the evaluation sequence */
	private Queue<Command>  newlyIssuedCommands;
	
	/** The command currently being evaluated */
	private Command currentEvaluationCmd;
	
	/** 
	 * Tracks commands that should be cleared out if the newly issued command
	 * queue gets pushed out onto the pending command queue.
	 */
	private HashMap<Command, LinkedList<Command>> cleansedMap;
	
	/** 
	 * MultiCommands that were expanded but are still
	 * needed as references for lookup.
	 */
	private ArrayList<Command> sidePocketedMultiCommands;
	
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Static Class Creation & Access
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	
	/** Static class instance */
	private static CommandSequencer commandSequencer =
		new CommandSequencer();
	
	/** Private constructor */
	private CommandSequencer() {
		
		approvedCommands = new LinkedList<Command>();
		pendingCommands = new LinkedList<Command>();
		newlyIssuedCommands = new LinkedList<Command>();
		cleansedMap = new HashMap<Command, LinkedList<Command>>();
		sidePocketedMultiCommands = new ArrayList<Command>();
		currentEvaluationCmd = null;
	}
	
	/** Class instance accessor */
	public static synchronized CommandSequencer getInstance() {
		return commandSequencer;
	}
	
	/** Prevent cloning */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException(
				"Cloning of CommandSequencer is not allowed.");
	}
	
    //-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Public methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------	
	
	
	//-----------------------
	// Approved queue methods
	//-----------------------
	
	/**
	 * Add a command to the approved command queue.
	 * WARNING: Doing so will means the command being added
	 * will not be checked by its respective rule engine!
	 * 
	 * MultiCommand variants cannot be added. Only individual commands
	 * can be added to the approved command queue.
	 * 
	 * @param command Command to add to approved command queue
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean addApprovedCommand(Command command) {
		
		if (command instanceof MultiCommand ||
				command instanceof MultiTransientCommand ||
				command instanceof MultiRevertCommand) {
		
			return false;
		}
		
		return approvedCommands.offer(command);
	}
	
	/**
	 * Remove a command from the approved command queue by the command
	 * reference.
	 * 
	 * @param command Command to remove from approved command queue
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removeApprovedCommand(Command command) {
		return approvedCommands.remove(command);
	}
	
	/**
	 * Remove a command from the approved command queue by transactionID.
	 * @param transactionID
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removeApprovedCommand(int transactionID) {
		
		return removeCommandByTransactionID(
				approvedCommands, 
				transactionID);
	}
	
	/**
	 * Get a list of the commands in the approved command queue.
	 * To safely make changes in the queue use the appropriate
	 * approved command queue methods.
	 * 
	 * @return List of commands in the pending command queue.
	 */
	public synchronized List<Command> getApprovedCommandList() {
		
		List<Command> approvedCommandList = 
			new ArrayList<Command>(approvedCommands);
		
		return approvedCommandList;
	}
	
	/**
	 * Replace a command in the approved queue with a new one.
	 * WARNING: Doing so will means the command being added
	 * will not be checked by its respective rule engine!
	 * 
	 * @param targetCommand Command to replace
	 * @param replacementCommand Replacement command
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean replaceApprovedCommand(
			Command targetCommand, 
			Command replacementCommand) {
		
		return replaceCommand(
				approvedCommands, 
				targetCommand, 
				replacementCommand);
	}
	
	//----------------------
	// Pending queue methods
	//----------------------
	
	/**
	 * Add a command to the pending command queue. If a MultiCommand
	 * variant is passed in it will be broken apart and added to the 
	 * queue.
	 * 
	 * @param command Command to add to pending command queue
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean addPendingCommand(Command command) {
		
		boolean result = true;
		
		if (command instanceof MultiCommand ||
				command instanceof MultiTransientCommand ||
				command instanceof MultiRevertCommand) {
			
			ArrayList<Command> expandedCommands = 
				CommandDataExtractor.expandAllMultiCommand(command);
			
			try {
				pendingCommands.addAll(expandedCommands);
			} catch (Exception e) {
				e.printStackTrace();
				result = false;
			}
			
		} else {
		
			result = pendingCommands.offer(command);
		}
		
		return result;
	}
	
	/**
	 * Remove a command from the pending command queue by the command
	 * reference.
	 * 
	 * @param command Command to remove from pending command queue
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removePendingCommand(Command command) {
		return pendingCommands.remove(command);
	}
	
	/**
	 * Remove a command from the pending command queue by transactionID.
	 * @param transactionID
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removePendingCommand(int transactionID) {
		
		return removeCommandByTransactionID(
				pendingCommands, 
				transactionID);
	}
	
	/**
	 * Get a list of the commands in the pending command queue.
	 * To safely make changes in the queue use the appropriate
	 * pending command queue methods.
	 * 
	 * @return List of commands in the pending command queue.
	 */
	public synchronized List<Command> getPendingCommandList() {
		
		List<Command> pendingCommandList = 
			new ArrayList<Command>(pendingCommands);
		
		return pendingCommandList;
	}
	
	/**
	 * Replace a command in the pending queue with a new one.
	 * 
	 * @param targetCommand Command to replace
	 * @param replacementCommand Replacement command
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean replacePendingCommand(
			Command targetCommand, 
			Command replacementCommand) {
		
		return replaceCommand(
				pendingCommands, 
				targetCommand, 
				replacementCommand);
	}
	
	/**
	 * Check if there are more pending commands.
	 * 
	 * @return True if there are more pending commands, false otherwise
	 */
	public synchronized boolean hasNextPendingCommand() {
		return !pendingCommands.isEmpty();
	}
	
	/**
	 * Get the next command on the pending command queue. Getting the
	 * next command removes it from the pending command queue, by calling
	 * the queue's poll method.
	 * 
	 * @return The next command to validate or null if queue is empty
	 */
	public synchronized Command getNextPendingCommand() {
		currentEvaluationCmd = pendingCommands.poll();
		return currentEvaluationCmd;
	}
	
	//-----------------------------------
	// Newly issued command queue methods
	//-----------------------------------
	
	/**
	 * Add a command to the newly issued command queue. If the command is a 
	 * MultiCommand variant it will be split apart and added to the newly 
	 * issued command queue.
	 * 
	 * @param command Command to add to newly issued command queue
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean addNewlyIssuedCommand(Command command) 
		throws NullPointerException{
		
		if (command == null) {
			throw new NullPointerException(
					"CommandSequencer was passed a newly issued null command");
		}
		
		boolean result = true;
		
		if (command instanceof MultiCommand ||
				command instanceof MultiTransientCommand ||
				command instanceof MultiRevertCommand) {
			
			ArrayList<Command> expandedCommands = 
				CommandDataExtractor.expandAllMultiCommand(command);
			
			try {
				newlyIssuedCommands.addAll(expandedCommands);
			} catch (Exception e) {
				e.printStackTrace();
				result = false;
			}
			
		} else {
		
			if (command == null) {
				System.out.println("");
			}
			
			result = newlyIssuedCommands.offer(command);
		}
		
		return result;
	}
	
	/**
	 * Remove a command from the newly issued command queue by the
	 * command reference.
	 * 
	 * @param command Command to remove from newly issued command queue
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removeNewlyIssuedCommand(Command command) {
		return newlyIssuedCommands.remove(command);
	}
	
	/**
	 * Remove a command from the newly issued command queue by transactionID.
	 * @param transactionID
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removeNewlyIssuedCommand(int transactionID) {
		
		return removeCommandByTransactionID(
				newlyIssuedCommands, 
				transactionID);
	}
	
	/**
	 * Get a list of the commands in the pending command queue.
	 * To safely make changes in the queue use the appropriate
	 * pending command queue methods.
	 * 
	 * @return List of commands in the pending command queue.
	 */
	public synchronized List<Command> getNewlyIssuedCommandList() {
		
		List<Command> newlyIssuedCommandList = 
			new ArrayList<Command>(newlyIssuedCommands);
		
		return newlyIssuedCommandList;
	}
	
	/**
	 * Replace a command in the newly issued command queue with a new one.
	 * 
	 * @param targetCommand Command to replace
	 * @param replacementCommand Replacement command
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean replaceNewlyIssuedCommand(
			Command targetCommand, 
			Command replacementCommand) {
		
		return replaceCommand(
				newlyIssuedCommands, 
				targetCommand, 
				replacementCommand);
	}
	
	//------------------------------
	// Cleansed commands map methods
	//------------------------------
	
	/**
	 * Add a command to the cleansed command map.
	 * 
	 * @param command Command to be added to cleansed map
	 * @return True if it was added, false otherwise
	 */
	public synchronized boolean addCleansedCommand(Command command) {
		
		// we want to always leave the transition commands on the stacks 
		// otherwise parenting can get really out of wack
		if (command instanceof TransitionEntityChildCommand && 
				!command.isTransient()) {
			return true;
		}                   		
		
		if (approvedCommands.contains(command)) {
			cleansedMap.put(command, (LinkedList<Command>)approvedCommands);
			approvedCommands.remove(command);
		} else if (pendingCommands.contains(command)) {
			cleansedMap.put(command, (LinkedList<Command>)pendingCommands);
			pendingCommands.remove(command);
		} else if (newlyIssuedCommands.contains(command)) {
			cleansedMap.put(command, (LinkedList<Command>)newlyIssuedCommands);
			newlyIssuedCommands.remove(command);
		} else {
			return false;
		}
		
		return true;
	}
	
	//-----------------------
	// Combined queue methods
	//-----------------------
	
	/**
	 * Get a list of the commands in all of the queues. If includeCurrent is 
	 * true, a copy of the current command being evaluated will be included. 
	 * If false is passed in, this will return the full list of commands minus 
	 * the command being evaluated. Start with the commands in the approved 
	 * command queue, followed by the pending command queue and finishing with 
	 * the newly issued command queue. It is safe to assume that the commands at
	 *  the end of the list are the last ones that will act on the entity 
	 *  affected. If includeCurrent is true the command being evaluated will
	 *  be added between the approved commands and pending commands.
	 * 
	 * No MultiCommand variants will be included because of the imposed 
	 * restriction that no queue can contain MultiCommands.
	 * 
	 * @return List of commands from all queues.
	 */
	public synchronized List<Command> getFullCommandList(
			boolean includeCurrent) {
		
		List<Command> fullCommandList = new ArrayList<Command>();
		
		// Get them in this order so that the latest commands affecting
		// an entity can be found by traversing the list in reverse order.
		fullCommandList.addAll(getApprovedCommandList());
		
		if (currentEvaluationCmd != null && includeCurrent) {
			fullCommandList.add(currentEvaluationCmd);
		}
		
		fullCommandList.addAll(getPendingCommandList());
		fullCommandList.addAll(getNewlyIssuedCommandList());
		
		return fullCommandList;
	}
	
	/**
	 * Replace a command in whichever queue it is first found in. Starts
	 * with the approved command queue, followed by the pending command queue
	 * and finishing with the newly issued command queue.
	 * 
	 * @param targetCommand Command to replace
	 * @param replacementCommand Replacement command
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean replaceCommand(
			Command targetCommand, 
			Command replacementCommand) {
		
		if (replaceApprovedCommand(targetCommand, replacementCommand)) {
			
			return true;
			
		} else if (replacePendingCommand(targetCommand, replacementCommand)) {
			
			return true;
			
		} else if (replaceNewlyIssuedCommand(targetCommand, replacementCommand)) {
			
			return true;
			
		}
		
		return false;
	}
	
	/**
	 * Remove the command, by reference, in whatever queue it is found in.
	 * 
	 * @param command Command to remove
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removeCommand(Command command) {
		
		if (removeApprovedCommand(command)) {
			
			return true;
			
		} else if (removePendingCommand(command)) {
			
			return true;
			
		} else if (removeNewlyIssuedCommand(command)) {
			
			return true;
			
		}
		
		return false;
	}
	
	/**
	 * Remove the command, by transactionID, in whatever queue it is found in.
	 * 
	 * @param transactionID TransactionID of the command to remove
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean removeCommand(int transactionID) {
		
		if (removeApprovedCommand(transactionID)) {
			
			return true;
			
		} else if (removePendingCommand(transactionID)) {
			
			return true;
			
		} else if (removeNewlyIssuedCommand(transactionID)) {
			
			return true;
			
		}
		
		return false;
	}
	
	/**
	 * Return the command from the queues with the matching Transaction ID.
	 * This will also check the side pocketed MultiCommands that were
	 * collected.
	 * 
	 * Note: MultiCommands are expanded and only their contents is kept.
	 * 
	 * @param transactionID Transaction ID of command to return
	 * @return Command matching transactionID, or null if not found
	 */
	public synchronized Command getCommand(int transactionID) {
		
		ArrayList<Command> fullCommandList = 
			(ArrayList<Command>) getFullCommandList(false);
		
		for (Command command : fullCommandList) {
			if (command.getTransactionID() == transactionID) {
				return command;
			}
		}
		
		for (Command command : sidePocketedMultiCommands) {
			if (command.getTransactionID() == transactionID) {
				return command;
			}
		}

		return null;
	}
	
	//--------------------
	// Clear queue methods
	//--------------------
	
	/**
	 * Sets the currentEvaluationCmd to null, effectively clearing it from the
	 * command queues.
	 */
	public synchronized void clearCurrentEvalutionCmd() {
		currentEvaluationCmd = null;
	}
	
	/**
	 * Clear the approved command queue.
	 */
	public synchronized void clearApprovedCommands() {
		approvedCommands.clear();
	}
	
	/**
	 * Clear the pending command queue.
	 */
	public synchronized void clearPendingCommands() {
		pendingCommands.clear();
	}
	
	/**
	 * Clear the newly issued command queue.
	 */
	public synchronized void clearNewlyIssuedCommands() {
		newlyIssuedCommands.clear();
	}
	
	/**
	 * Clear the cleansed commands map.
	 */
	public synchronized void clearCleansedCommands() {
		cleansedMap.clear();
	}
	
	/**
	 * Clear the side pocketed multi commands.
	 */
	public synchronized void clearSidePocketedMultiCommands() {
		sidePocketedMultiCommands.clear();
	}
	
	/**
	 * Clear the appropriate maps and queues in preparation for evaluating
	 * the next command.
	 */
	public synchronized void clearForNextCommandCheck() {
		clearNewlyIssuedCommands();
		clearCleansedCommands();
		clearSidePocketedMultiCommands();
		clearCurrentEvalutionCmd();
	}
	
	/**
	 * Clear everything.
	 */
	public synchronized void clearAll() {
		
		clearApprovedCommands();
		clearPendingCommands();
		clearNewlyIssuedCommands();
		clearCleansedCommands();
		clearSidePocketedMultiCommands();
		clearCurrentEvalutionCmd();
	}
	
	//------------------------------------------------------------------
	// Check for commands on the pending and newly issued command queues
	//------------------------------------------------------------------
	
	/**
	 * Check if there are commands in the pending and newly issued command 
	 * queues that will need to be evaluated by their respective rule engines.
	 * 
	 * @return True if there are commands waiting, false otherwise
	 */
	public synchronized boolean commandsToEvaluate() {
		
		if (pendingCommands.isEmpty() && newlyIssuedCommands.isEmpty()) {
			return false;
		}
		
		return true;
	}
	
	//---------------------------------------------------
	// Remove MultiCommand variant's contents from queues
	//---------------------------------------------------
	
	/**
	 * Remove all commands from queues that belong to the MultiCommand
	 * variant passed in.
	 */
	public synchronized void removeMultiCommand(Command command) {
		
		if (!(command instanceof MultiCommand) ||
				!(command instanceof MultiTransientCommand) ||
				!(command instanceof MultiRevertCommand)) {
			
			return;
		}
		
		ArrayList<Command> expandedCommands = 
			CommandDataExtractor.expandAllMultiCommand(command);
		
		for (Command subCommand : expandedCommands) {
			
			// We don't care what the return value is in this case.
			// If the subCommand doesn't exist anywhere that is because
			// something else removed it and that is ok.
			removeCommand(subCommand);
		}
	}
	
	//-------------------
	// Move queue methods
	//-------------------
	
	/**
	 * Move the commands in the newly issued command queue to the pending
	 * queue.
	 * 
	 * @return True if successful, false otherwise
	 */
	public synchronized boolean moveNewlyIssuedCommandsToPending() {
		
		// The value returned to caller
		boolean finalResult = true;
		
		// Reused fields for looping procedures
		Command command;
		
		// Apply the cleansed map. This removes commands from the queues as a
		// result of specific RemoveEntityCommands being issued.
//		applyCleansedCommands();
		clearCleansedCommands();
		
		// Pull all of the commands out of the newly issued command queue.
		ArrayList<Command> extractedNewlyIssuedCommands = 
			new ArrayList<Command>();
		
		while (!newlyIssuedCommands.isEmpty()) {
			
			command = newlyIssuedCommands.poll();
			
			// Fail safe
			if (command == null) {
				break;
			}
			
			if (command instanceof MultiCommand ||
					command instanceof MultiTransientCommand ||
					command instanceof MultiRevertCommand) {
				
				extractedNewlyIssuedCommands.addAll(
						CommandDataExtractor.expandAllMultiCommand(
								command));
			} else {
				
				extractedNewlyIssuedCommands.add(command);
			}
		}
		
		// Special case handling. In case the queue has an imposed insertion
		// restriction and the adding fails, we will send back false.
		try {
			pendingCommands.addAll(extractedNewlyIssuedCommands);
		} catch (Exception e) {
			e.printStackTrace();
			finalResult = false;
		}
		
		clearNewlyIssuedCommands();
		
		return finalResult;
	}
	
	//--------------------
	// Print queue methods
	//--------------------
	
	public synchronized void printApprovedQueue() {
		
		System.out.println("**[Command Sequence Approved Queue]**");
		printQueueCommands((LinkedList<Command>)approvedCommands);
		System.out.println("** end approved queue **");
	}
	
	public synchronized void printPendingQueue() {
		
		System.out.println("**[Command Sequence Pending Queue]**");
		printQueueCommands((LinkedList<Command>)pendingCommands);
		System.out.println("** end pending queue **");
	}
	
	public synchronized void printNewlyIssuedQueue() {
		
		System.out.println("**[Command Sequence Newly Issued Queue]**");
		printQueueCommands((LinkedList<Command>)newlyIssuedCommands);
		System.out.println("** end newly issued queue **");
	}
	
	public synchronized void printAllQueues() {
		
		System.out.println("**&&[Command Sequence All Queues]&&**");
		printApprovedQueue();
		printPendingQueue();
		printNewlyIssuedQueue();
		System.out.println("**&& end all queues &&**");
	}
	
    //-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------	
	
	/**
	 * Print out the command contents of the queue. Formatted results by
	 * #) Command description (transactionID: #)
	 * 
	 * @param queue Queue to print out
	 */
	private void printQueueCommands(LinkedList<Command> queue) {
		
		Iterator<Command> commandIterator = queue.iterator();
		int counter = 1;
		Command command;
		
		while (commandIterator.hasNext()) {
			
			command = commandIterator.next();
			System.out.println(
					counter+") "+command.getDescription()+
					" (transactionID: "+command.getTransactionID()+")");
			
			counter++;
		}
	}
	
	/**
	 * General replace command routine.
	 * 
	 * @param queue Queue to replace command in
	 * @param targetCommand Command to replace
	 * @param replacementCommand Replacement command
	 * @return True if successful, false otherwise
	 */
	private boolean replaceCommand(
			Queue<Command> queue, 
			Command targetCommand, 
			Command replacementCommand) {
		
		int index = ((LinkedList<Command>)queue).indexOf(targetCommand);
		
		if (index == -1) {
			return false;
		}
		
		((LinkedList<Command>)queue).set(index, replacementCommand);
		
		return true;
	}
	
	/**
	 * General remove command by transactionID routine.
	 * 
	 * @param queue Queue to remove command from
	 * @param transactionID TransactionID of command to remove
	 * @return True if successful, false otherwise
	 */
	private boolean removeCommandByTransactionID(
			Queue<Command> queue, 
			int transactionID) {
		
		Iterator<Command> commandIterator = queue.iterator();
		Command command;
		
		while (commandIterator.hasNext()) {
			
			command = commandIterator.next();
			
			if (command.getTransactionID() == transactionID) {
				queue.remove(command);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Remove the commands stored in the cleansed map from their respective
	 * queues. Until this is called, cleansed commands live in purgatory and
	 * do not affect the queues.
	 */
	private void applyCleansedCommands() {
		
		Map.Entry entry;
		Command command;
		LinkedList<Command> list;
		
		Iterator iterator = cleansedMap.entrySet().iterator();
		
		while (iterator.hasNext()) {
		
			entry = (Map.Entry<Command, LinkedList<Command>>) iterator.next();
			
			command = (Command) entry.getKey();
			list = (LinkedList<Command>) entry.getValue();
			
			list.remove(command);
		}
		
		clearCleansedCommands();		
	}
}
