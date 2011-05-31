/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005 - 2010
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

// Internal Imports

/**
 * Data class that holds a property update
 * 
 * @author Russell Dodds
 * @version $Revision: 1.9 $
 */
public class EntityProperty {
       
    /** The Entity ID */
    public int entityID;

    /** The property sheet  */
    public String propertySheet;

    /** The property name */
    public String propertyName;
    
    /** The property value */
    public Object propertyValue;
            
	/**
	 * Default Constructor
	 */
	public EntityProperty() {
		this(0, null, null, null);
	}
	
	/**
	 * Constructor
	 *
	 * @param entityID The Entity ID
	 * @param propertySheet The property sheet
	 * @param propertyName The property name
	 * @param propertyValue The property value
	 */
	public EntityProperty(
		int entityID,
		String propertySheet,
		String propertyName,
		Object propertyValue) {
		
		this.entityID = entityID;
		this.propertySheet = propertySheet;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}
	
    /**
     * Compares the propertySheet, and propertyName to see
     * if the match
     * 
     * @param compare The EntityProperty to compare with
     * @return true if the propertySheet and propertyName match
     */
    public boolean sharedProperty(EntityProperty compare) {       
        if (propertySheet.equals(compare.propertySheet) && 
            propertyName.equals(compare.propertyName)) {               
            return true;
        }     
        return false;       
    }   
}
