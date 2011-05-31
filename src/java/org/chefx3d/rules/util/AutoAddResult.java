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

package org.chefx3d.rules.util;

//External Imports

//Internal Imports

/**
* Auto add operation result data object used to encapsulate the result of 
* operations with either the transactionID of the resulting command, the 
* entityID of the existing match, or a failure flag if auto add did not 
* succeed.
*
* @author Ben Yarger
* @version $Revision: 1.1 $
 */
public class AutoAddResult {

    /** 
     * Flags the result of the auto add action as either a transaction ID or
     * entity ID, or as a failure.
     */
    public static enum TRANSACTION_OR_ENTITY_ID {TRANSACTION, ENTITY, FAILURE};
    
    /** Integer id value to store. */
    private int value;
    
    /** 
     * Transaction ID, entity ID or failure flag to associate with the value.
     */
    private TRANSACTION_OR_ENTITY_ID type;
    
    /**
     * Default constructor.
     * 
     * @param value Value to store
     * @param type Type identifier of the value
     */
    public AutoAddResult(int value, TRANSACTION_OR_ENTITY_ID type) {
    
    	this.value = value;
    	this.type = type;
    }
    
    /**
     * Accessor for the value field.
     * @return value stored in the result.
     */
    public int getValue() {
    	return value;
    }
	
    /**
     * Accessor for the type field.
     * @return Type stored in the result.
     */
    public TRANSACTION_OR_ENTITY_ID getType() {
    	return type;
    }
    
    /**
     * See if the result was a successful outcome.
     * 
     * @return True if the resulting type is not a failure.
     */
    public boolean wasSuccessful() {
    	
    	if (type != TRANSACTION_OR_ENTITY_ID.FAILURE) {
    		return true;
    	}
    	
    	return false;
    }
}
