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
import java.util.ArrayList;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.util.DefaultErrorReporter;

/**
 * Utility methods for extracting various bits of data from a command.
 * 
 * Provides various methods to extract data from commands.
 * 
 * Provides methods to extract all commands from a MultiCommand, 
 * including any nested MultiCommands. Also provides functionality to extract 
 * the entities affected by the commands in MultiCommands, including all 
 * nested MultiCommands.
 * 
 * Provides methods to pull the primary entity as well as secondary entities
 * out of a command. This will not work for MultiCommands.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */
public class CommandDataExtractor {

	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	// Public methods
	//-------------------------------------------------------------------------
	//-------------------------------------------------------------------------
	
	/**
	 * Expand a single MultiCommand variant into the commands it contains.
	 * Does not expand nested MultiCommand variants.
	 * 
	 * @param multiCommand MultiCommand variant to expand
	 * @return ArrayList of commands pulled out of multiCommand, or empty list
	 * if not a multiCommand variant
	 */
	public static ArrayList<Command> expandSingleMultiCommand(Command multiCommand) {
		
		ArrayList<Command> expandedCommandList = new ArrayList<Command>();
		
		if (multiCommand instanceof MultiCommand) {
			
			expandedCommandList.addAll(
					((MultiCommand)multiCommand).getCommandList());
			
		} else if (multiCommand instanceof MultiTransientCommand) {
			
			expandedCommandList.addAll(
					((MultiTransientCommand)multiCommand).getCommandList());
			
		} else if (multiCommand instanceof MultiRevertCommand) {
			
			expandedCommandList.addAll(
					((MultiRevertCommand)multiCommand).getCommandList());
			
		} else {
			
			// Return an empty command list.
			return expandedCommandList;
			
		}
		
		return expandedCommandList;
	}
	
	/**
	 * Expand MultiCommand variants into all of the commands they contains. 
	 * Any nested MultiCommands will be expanded so the returned list will 
	 * contain only non MultiCommands.
	 * 
	 * @param multiCommand MultiCommand variant to expand.
	 * @return ArrayList of Commands or empty list if not a MultiCommand
	 * variant
	 */
	public static ArrayList<Command> expandAllMultiCommand(Command multiCommand){
		
		ArrayList<Command> expandedCommandList = 
			expandSingleMultiCommand(multiCommand);
		
		ArrayList<Command> extractedMultiCommands = new ArrayList<Command>();
		
		// Extract and then recursively examine any multi commands from the 
		// expansion.
		for (Command command : expandedCommandList) {
			
			if (multiCommand instanceof MultiCommand ||
					multiCommand instanceof MultiTransientCommand ||
					multiCommand instanceof MultiRevertCommand) {
				
				extractedMultiCommands.add(command);
				
			}
		}
		
		for (Command command : extractedMultiCommands) {
			expandedCommandList.addAll(expandAllMultiCommand(command));
		}
		
		return expandedCommandList;
	}
	
	/**
	 * Expand MultiCommand variants into the entities affected by commands.
	 * Can set includeSecondaries flag to true to get the full list of entities
	 * involved in commands, or leave false to get just the primary entities
	 * from each command.
	 * 
	 * This will not grab entities from nested MultiCommand variants.
	 * 
	 * @param multiCommand MultiCommand variant to expand
	 * @param includeSecondaries True to include secondaries entities, false 
	 * for primary entities only
	 * @return ArrayList of Entities
	 */
	public static ArrayList<Entity> expandSingleMultiCommandAsEntities(
			Command multiCommand,
			boolean includeSecondaries) {
		
		ArrayList<Command> commandList = 
			expandSingleMultiCommand(multiCommand);
		
		ArrayList<Entity> entityList = new ArrayList<Entity>();

		if (includeSecondaries) {
			
			for (Command command : commandList) {
				
				try {
					
					entityList.addAll(extractAllEntities(command));
					
				} catch (NoSuchFieldException e) {
					
					DefaultErrorReporter.getDefaultReporter().debugReport(
							"ExpandMultipleCommandAsEntities NON FATAL " +
							"EXCEPTION, continuing execution.", e);
					entityList.clear();
					return entityList;
				}
			}
			
		} else {
			
			for (Command command : commandList) {
				
				try {
					
					entityList.add(extractPrimaryEntity(command));
					
				} catch (NoSuchFieldException e) {
					
					DefaultErrorReporter.getDefaultReporter().debugReport(
							"ExpandMultipleCommandAsEntities NON FATAL " +
							"EXCEPTION, continuing execution.", e);
					entityList.clear();
					return entityList;
				}
			}
			
		}
		
		return entityList;
	}
	
	/**
	 * Expand MultiCommand variants into the entities affected by commands.
	 * Can set includeSecondaries flag to true to get the full list of entities
	 * involved in commands, or leave false to get just the primary entities
	 * from each command.
	 * 
	 * This will grab all entities from all nested MultiCommand variants.
	 * 
	 * @param multiCommand MultiCommand variant to expand
	 * @param includeSecondaries True to include secondaries entities, false 
	 * for primary entities only
	 * @return ArrayList of Entities
	 */
	public static ArrayList<Entity> expandAllMultiCommandAsEntities(
			Command multiCommand,
			boolean includeSecondaries) {
		
		ArrayList<Command> commandList = expandAllMultiCommand(multiCommand);
		
		ArrayList<Entity> entityList = new ArrayList<Entity>();

		if (includeSecondaries) {
			
			for (Command command : commandList) {
				
				try {
					
					entityList.addAll(extractAllEntities(command));
					
				} catch (NoSuchFieldException e) {
					
					DefaultErrorReporter.getDefaultReporter().debugReport(
							"ExpandMultipleCommandAsEntities NON FATAL " +
							"EXCEPTION, continuing execution.", e);
					entityList.clear();
					return entityList;
				}
			}
			
		} else {
			
			for (Command command : commandList) {
				
				try {
					
					entityList.add(extractPrimaryEntity(command));
					
				} catch (NoSuchFieldException e) {
					
					DefaultErrorReporter.getDefaultReporter().debugReport(
							"ExpandMultipleCommandAsEntities NON FATAL " +
							"EXCEPTION, continuing execution.", e);
					entityList.clear();
					return entityList;
				}
			}
			
		}
		
		return entityList;
	}
	
	/**
	 * Extract the primary entity from the command. If the command doesn't
	 * have a RuleDataAccessor or is a MultiCommand, a
	 * NoSuchFieldException will be thrown.
	 * 
	 * @param command Command to extract primary entity from
	 * @return Primary entity
	 * @throws NoSuchFieldException if the command is a MultiCommand variant
	 * or does not have a RuleDataAccessor
	 */
	public static Entity extractPrimaryEntity(Command command) 
		throws NoSuchFieldException{
		
		// If the command doesn't have a RuleDataAccessor, 
		// throw NoSuchFieldException
		if (!(command instanceof RuleDataAccessor)) {
			
			throw new NoSuchFieldException(
					"Cannot extract entity from command that doesn't " +
					"implement RuleDataAccessor.");
		}
		
		// If the command is a MultiCommand, 
		// throw NoSuchFieldException
		if (command instanceof MultiCommand ||
				command instanceof MultiRevertCommand ||
				command instanceof MultiTransientCommand) {
			
			throw new NoSuchFieldException(
					"Cannot extract entity from MultiCommand variants.");
		}
		
		return ((RuleDataAccessor)command).getEntity();
		
	}
	
	/**
	 * Returns the primary entity acted on by the command, and any secondary
	 * entities. TransitionEntityChildCommands are the only ones that have more
	 * than one secondary command. In that case the end parent entity will come
	 * first followed by the start parent entity. The primary entity will 
	 * always be first in the list.
	 * 
	 * If the command doesn't have a RuleDataAccessor or is a MultiCommand* 
	 * a NoSuchFieldException will be thrown.
	 * 
	 * @param command Command to extract entities from
	 * @param includeSecondaries True to include secondaries, false otherwise
	 * @return ArrayList of Entities, primary one always first
	 * @throws NoSuchFieldException if the command is a MultiCommand variant
	 * or does not have a RuleDataAccessor
	 */
	public static ArrayList<Entity> extractAllEntities(Command command) 
		throws NoSuchFieldException{
		
		ArrayList<Entity> extractedEntities = new ArrayList<Entity>();
		
		// If the command doesn't have a RuleDataAccessor, 
		// throw NoSuchFieldException
		if (!(command instanceof RuleDataAccessor)) {
			
			throw new NoSuchFieldException(
					"Cannot extract entity from command that doesn't " +
					"implement RuleDataAccessor.");
		}
		
		// If the command is a MultiCommand, 
		// throw NoSuchFieldException
		if (command instanceof MultiCommand ||
				command instanceof MultiRevertCommand ||
				command instanceof MultiTransientCommand) {
			
			throw new NoSuchFieldException(
				"Cannot extract entity from MultiCommand variants.");
		}
		
		extractedEntities.add(((RuleDataAccessor)command).getEntity());
		
		// Handle all of the secondary entities.
		// These are commands that require more than one entity in 
		// their implementation. We need to extract the entity that
		// wasn't examined from the call to getEntity().
		if (command instanceof AddEntityChildCommand) {
			
			extractedEntities.add(
					((AddEntityChildCommand)command).getParentEntity());
			
		} else if (command instanceof AddEntityChildTransientCommand) {
			
			extractedEntities.add(
					((AddEntityChildTransientCommand)
							command).getParentEntity());
			
		} else if (command instanceof AddSegmentCommand) {
			
			extractedEntities.add(
					((AddSegmentCommand)command).getSegmentableEntity());
			
		} else if (command instanceof AddSegmentTransientCommand) {
			
			extractedEntities.add(
					((AddSegmentTransientCommand)
							command).getSegmentableEntity());
			
		} else if (command instanceof AddVertexCommand) {
			
			extractedEntities.add(
					((AddVertexCommand)command).getSegmentableEntity());
			
		} else if (command instanceof AddVertexTransientCommand) {
			
			extractedEntities.add(
					((AddVertexTransientCommand)
							command).getSegmentableEntity());
			
		} else if (command instanceof MoveEntityTransientCommand) {
			
			// TODO: this case is only here because there is pickEntity
			// If it turns out the pickEntity matching the entity
			// should cause the MoveEntityTransientCommand to be 
			// removed, then this is the place to do it.
			
		} else if (command instanceof RemoveEntityChildCommand) {
			
			extractedEntities.add(
					((RemoveEntityChildCommand)command).getParentEntity());
			
		} else if (command instanceof RemoveEntityChildTransientCommand) {
			
			extractedEntities.add(
					((RemoveEntityChildTransientCommand)
							command).getParentEntity());
			
		} else if (command instanceof RemoveSegmentCommand) {
			
			// TODO: this command doesn't expose the secondary
			// SegmentableEntity. When it does, we will need to implement
			// this portion here.
			
		} else if (command instanceof RemoveVertexCommand) {
			
			// TODO: this command doesn't expose the secondary
			// SegmentableEntity. When it does, we will need to implement
			// this portion here.
			
		} else if (command instanceof TransitionEntityChildCommand) {
			
			extractedEntities.add(
					((TransitionEntityChildCommand)
							command).getEndParentEntity());
			
			extractedEntities.add(
					((TransitionEntityChildCommand)
							command).getStartParentEntity());
			
		}
		
		return extractedEntities;
	}
	
}
