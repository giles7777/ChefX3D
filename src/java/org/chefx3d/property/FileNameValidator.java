/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005 - 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property;

import org.chefx3d.model.PropertyValidator;
import org.chefx3d.util.CloneableProperty;

/**
 * A validator that makes sure that the file name provided
 * does not contain any illegal characters.
 *
 * @author Sang Park
 * @version $Revision: 1.3 $
 */
public class FileNameValidator 
	implements PropertyValidator, CloneableProperty {

	/** Lists of default illegal characters */
	public static final char[] ILLEGAL_CHARS = { '*', '|', '?', '<', '>', ':', '"', '/' };
	
    /** The message to return to the user */
    private String message;
    
    /** Illegal characters that will be checked during the validation process */
    private char[] illegalChars;
    
    /**
     * Create a FileNameValidator that will check that file name 
     * does not contain any illegal characters.
     * <p>
     * This constructor function uses the default illegal character list
     * to check if a filename contains any illegal character or not.
     */
    public FileNameValidator() {
    	this(ILLEGAL_CHARS);
    }
    
    /**
     * Create a FileNameValidator that will check that file name 
     * does not contain any illegal characters.
     * <p>
     * This constructor function uses illegal character list passed
     * in by the user to check if a filename contains any illegal
     * character or not.
     * @param illegalChars Arrays of illegal characters to be checked
     * 					   with a file name.
     */
	public FileNameValidator(char [] illegalChars) {
		this.illegalChars = illegalChars;
	}
	
    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------  

    /**
     * Create a copy of the property
     */
    public FileNameValidator clone() {
        
        // Create the new copy
    	FileNameValidator out = new FileNameValidator();
        return out;
    }
    
    // ----------------------------------------------------------
    // Methods required by the PropertyValidator
    // ----------------------------------------------------------
    public boolean validate(Object value) {
        
    	if(value instanceof String) {
    		String fileName = (String)value;
    		
    		for(int i = 0; i < illegalChars.length; i++) {
    			if(fileName.contains(new String(new char[] {illegalChars[i]}))) {
    				
    		        message = "Data Validation Error:\n The filename contains an " +
    		        		  "illegal character.";
    		        
    				return false;
    			}
    		}
    	
    	} else {
    		// File name has to be instance of string.
    		// Thus, not if not instance of string it can not
    		// be validated.
            message = "Data Validation Error:\n " +
            		  "Value provided " + value + " is not an instance of " +
            		  "String.";
    		return false;
    	}
    	
        return true;
    }

    /**
     * The message the user will see if failed
     */
    public String getMessage() {
        return message;
    }
    
    // ----------------------------------------------------------
    // Local methods
    // ----------------------------------------------------------

}
