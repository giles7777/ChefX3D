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
import java.util.HashSet;
import java.util.Map;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for adding a property sheet to an entity.
 * 
 * @author Christopher Shankland
 * @version
 * 
 */
public class AddPropertySheetCommand implements Command {

	/** The entity */
	private Entity entity;

	/** The property sheet */
	private Map<String, Object> propSheet;

	/** The sheet name */
	private String sheetName;

	/** The description of the <code>Command</code> */
	private String description;

	/** The flag to indicate transient status */
	private boolean transientState;

	/** The flag to indicate undoable status */
	private boolean undoableState;

	/** The ErrorReporter for messages */
	private ErrorReporter errorReporter;
	
	/** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;

	/**
	 * Add a new sheet to an Entity.
	 * 
	 * @param entity
	 *            The entity to add the sheet
	 * @param sheetName
	 *            The name of the sheet
	 * @param propSheet
	 *            The values of the sheet
	 */
	public AddPropertySheetCommand(Entity entity, String sheetName,
			Map<String, Object> propSheet) {
		this.entity = entity;
		this.sheetName = sheetName;
		this.propSheet = propSheet;

		description = "AddPropertySheetCommand -> " + entity.getName();

		init();
	}

	/**
	 * Common initialization.
	 */
	private void init() {
		errorReporter = DefaultErrorReporter.getDefaultReporter();

		undoableState = true;
		transientState = false;
	}

	/**
	 * Execute the command.
	 */
	public void execute() {
		entity.addPropertySheet(sheetName, propSheet);
	}

	/**
	 * Register an error reporter with the command instance so that any errors
	 * generated can be reported in a nice manner.
	 * 
	 * @param reporter
	 *            The new ErrorReporter to use.
	 */
	public void setErrorReporter(ErrorReporter reporter) {
		errorReporter = reporter;

		if (errorReporter == null)
			errorReporter = DefaultErrorReporter.getDefaultReporter();
	}

	/**
	 * Redo the affects of this command.
	 */
	public void redo() {
		execute();
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
	 * Get the state of this <code>Command</code>.
	 */
	public boolean isTransient() {
		return transientState;
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
	 * Is the command locally generated.
	 * 
	 * @return Is local
	 */
	public boolean isLocal() {
		return false;
	}

	/**
	 * Set the local flag.
	 * 
	 * @param isLocal
	 *            Is this a local update
	 */
	public void setLocal(boolean isLocal) {
		// ignore
	}

	/**
	 * Get the undo setting of this <code>Command</code>. true =
	 * <code>Command</code> may be undone false = <code>Command</code> may never
	 * undone
	 */
	public boolean isUndoable() {
		return undoableState;
	}

	/**
	 * Undo the affects of this command.
	 */
	public void undo() {
		entity.removePropertySheet(sheetName);
	}

	public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

}
