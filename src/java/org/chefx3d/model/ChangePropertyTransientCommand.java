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

// External imports
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

import org.w3c.dom.*;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DOMUtils;

/**
 * A transient command for changing a property
 *
 * @author Alan Hudson
 * @version $Revision: 1.15 $
 */
public class ChangePropertyTransientCommand implements Command, RuleDataAccessor {

	/** The model */
    private WorldModel model;
    
    /** The new entity */
    private Entity entity;

    /** The property sheet */
    private String propertySheet;

    /** The property name */
    private String propertyName;

    /** The new value */
    private Object newValue;

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
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;
    
    /** Should the command die */
    private boolean shouldDie = false;

    /**
     * Change a property of an entity, cannot be undone/redone.
     *
     * @param entity - The entity whose property changed
     * @param propertySheet - The property sheet changed.
     * @param propertyName - The property which changed.
     * @param newValue - The new value.
     * @param model - The WorldModel
     */
    public ChangePropertyTransientCommand(
            Entity entity,
            String propertySheet,
            String propertyName,
            Object newValue,
            WorldModel model) {

/*
System.out.println("ChangePropertyTransientCommand()");
System.out.println("    entityID: " + entityID);
System.out.println("    propertySheet: " + propertySheet);
System.out.println("    propertyName: " + propertyName);
*/
        // Cast to package definition to access protected methods
        this.entity = entity;
        this.propertyName = propertyName;
        this.propertySheet = propertySheet;
        this.newValue = newValue;
        this.model = model;

        local = true;

        init();

    }

    public ChangePropertyTransientCommand(Entity entity) {

//System.out.println("ChangePropertyCommand network constructor");

        // Cast to package definition to access protected methods
        this.entity = (Entity) entity;

        init();
    }

    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "ChangeProperty -> " + propertyName;

        undoableState = false;
        transientState = true;
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
     * Execute the command.
     */
    public void execute() {

        entity.setProperty(propertySheet, propertyName, newValue, transientState);

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
            /*
             * <ChangePropertyCommand entityID='' propSheet=''>
             *  <propName></propName>
             *  <originalValue></originalValue>
             *  <newValue></newValue>
             * </ChangePropertyCommand>
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<ChangePropertyCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' propSheet='");
            sbuff.append(propertySheet);
            sbuff.append("'>");
            sbuff.append("<propName>");
            sbuff.append(propertyName);
            sbuff.append("</propName>");
            sbuff.append("<newValue>");
            sbuff.append(newValue.toString());
            sbuff.append("</newValue>");
            sbuff.append("</ChangePropertyCommand>");

            //System.out.println(sbuff.toString());

            PrintStream ps = new PrintStream(os);
            ps.print(sbuff.toString());

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
        NodeList list;

        Document doc = DOMUtils.parseXML(st);

        Element e = (Element) doc.getFirstChild();

        // TODO: need to serialize/deserialize an Entity correctly

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        propertySheet = e.getAttribute("propSheet");

        list = doc.getElementsByTagName("propName");
        e = (Element) list.item(0);
        propertyName = e.getTextContent();

        list = doc.getElementsByTagName("newValue");
        e = (Element) list.item(0);
        newValue = e.getTextContent();

        local = false;
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
     * Accessor method for propertyName
     * @return String
     */
    public String getPropertyName(){
    	return propertyName;
    }
    
    /**
     * Accessor method for propertySheet
     * @return String
     */
    public String getPropertySheet(){
    	return propertySheet;
    }

    /**
     * Accessor method for the new propertyValue
     * @return Object The new propertyValue
     */
    public Object getPropertyValue(){
    	return(newValue);
    }

    /**
     * Retrieve the entity
     *
     * @return Entity
     */
    public Entity getEntity(){
        return entity;
    }
    
	/**
	 * Get the new value that the command wants to change to.
	 * 
	 * @return Object
	 */
	public Object getNewValue(){
		return newValue;
	}
	
	/**
	 * Set the new value that the command will change to.
	 * 
	 * @param object
	 */
	public void setNewValue(Object object){
		newValue = object;
	}

	/**
	 * Get the WorldModel.
	 * 
	 * @return WorldModel
	 */
	public WorldModel getWorldModel() {

		return model;
	}

	public HashSet<String> getIgnoreRuleList() {
	        // TODO Auto-generated method stub
	      return ignoreRuleList;
	}

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

    public void resetToStart() {
        
    }
    
    /**
     * Set the die state of the command. Setting this to true will
     * only cause the command to die if the rule engine execution
     * returns false.
     * 
     * @param die True to have command die and not execute
     */
	public void setCommandShouldDie(boolean die) {
		
		shouldDie = die;
	}

	/**
	 * Get the die value of the command.
	 * 
	 * @return True to have command die, false otherwise
	 */
	public boolean shouldCommandDie() {

		return shouldDie;
	}
	
	/**
	 * Compare external command to this one to see if they are the same.
	 * 
	 * @param externalCommand command to compare against
	 * @return True if the same, false otherwise
	 */
	public boolean isEqualTo(Command externalCommand) {
		
		if (externalCommand instanceof ChangePropertyCommand) {
		
			if (((ChangePropertyCommand)externalCommand).getEntity() != 
				this.entity) {
				
				return false;
				
			} else if (((ChangePropertyCommand)externalCommand).getNewValue() 
					!= this.newValue) {
				
				return false;
				
			} else if (!((ChangePropertyCommand)externalCommand).
					getPropertyName().equals(this.propertyName)){
				
				return false;
				
			} else if (!((ChangePropertyCommand)externalCommand).
					getPropertySheet().equals(this.propertySheet)) {
				
				return false;
				
			}
			
			return true;
		}
		
		return false;
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