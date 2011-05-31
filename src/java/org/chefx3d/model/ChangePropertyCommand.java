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
 * A command for changing a property
 *
 * @author Alan Hudson
 * @version $Revision: 1.46 $
 */
public class ChangePropertyCommand 
    implements Command, RuleDataAccessor, RuleBypassFlag {

	/** The model */
    private WorldModel model;
    
    /** The new entity */
    private Entity entity;

    /** The property sheet */
    private String propertySheet;

    /** The property name */
    private String propertyName;

    /** The old value */
    private Object originalValue;

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

    /** The rule bypass flag, default is false */
    private boolean ruleBypassFlag;
    
    /** Unique transaction ID identifying the command. */
    private int transactionID;

    /**
     * Change a property of an entity
     *
     * @param entity - The entity whose property changed
     * @param propertySheet - The property sheet changed.
     * @param propertyName - The property which changed.
     * @param originalValue - The current value.
     * @param newValue - The new value.
     * @param model - The WorldModel
     */
    public ChangePropertyCommand(
            Entity entity,
            String propertySheet,
            String propertyName,
            Object originalValue,
            Object newValue,
            WorldModel model) {

/*
System.out.println("ChangePropertyCommand()");
System.out.println("    entity: " + entity);
System.out.println("    propertySheet: " + propertySheet);
System.out.println("    propertyName: " + propertyName);
System.out.println("    originalValue: " + originalValue.getClass().toString());
System.out.println("    newValue: " + newValue.getClass().toString());
*/

        // Cast to package definition to access protected methods
    	this.transactionID = model.issueTransactionID();
        this.entity = entity;
        this.propertyName = propertyName;
        this.propertySheet = propertySheet;
        this.originalValue = originalValue;
        this.newValue = newValue;
        this.model = model;

        local = true;

        init();

    }

    /**
     * Change a property
     *
     * @param entity - The entity whose property changed
     */
    public ChangePropertyCommand(Entity entity) {

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

        undoableState = true;
        transientState = false;
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

        /*
         *  If we are changing a PositionableEntitiy Scale Property then 
         *  update its last known good scale value.
         */
        if(propertyName == PositionableEntity.SCALE_PROP && 
        		entity instanceof PositionableEntity){
        	
        	((PositionableEntity)entity).setStartingScale((float[])newValue);
        }
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {

        entity.setProperty(propertySheet, propertyName, originalValue, transientState);

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
        return transactionID;
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
            sbuff.append("<originalValue>");
            sbuff.append(originalValue.toString());
            sbuff.append("</originalValue>");
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

        list = doc.getElementsByTagName("originalValue");
        e = (Element) list.item(0);
        originalValue = e.getTextContent();

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
	 * Get the WorldModel.
	 * 
	 * @return WorldModel
	 */
	public WorldModel getWorldModel() {

		return model;
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
	 * Get the original value the command wants to update.
	 * 
	 * @return
	 */
	public Object getOriginalValue(){
		return originalValue;
	}

    public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
      return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

    public void resetToStart() {
        // TODO Auto-generated method stub
        newValue = originalValue;
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
				
			} else if (((ChangePropertyCommand)externalCommand).
					getOriginalValue() != this.originalValue) {
				
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
	
    //--------------------------------------------------------------------
    // Routines required by RuleBypassFlag
    //--------------------------------------------------------------------

    /**
     * Set the rule bypass flag value.
     *
     * @param ruleBypassFlag True to bypass rules, false otherwise
     */
    public void setBypassRules(boolean bypass){
        this.ruleBypassFlag = bypass;
    }

    /**
     * Get the rule bypass flag value.
     *
     * @return boolean True to bypass, false otherwise
     */
    public boolean bypassRules() {
        return ruleBypassFlag;
    }

}