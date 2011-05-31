/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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

//External Imports
// None

//Internal Imports
// None

public class ModelChangeException extends Exception {

    /**
     * Constructs a new exception with null as its detail message.
     *
     */
    public ModelChangeException() {
        super();
    }
    
    /**
     * Constructs a new exception with the specified detail message. 
     *
     */
    public ModelChangeException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause. 
     *
     */
    public ModelChangeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new exception with the specified cause and a detail message 
     * of (cause==null ? null : cause.toString()) (which typically contains the 
     * class and detail message of cause).
     *
     */
    public ModelChangeException(Throwable cause) {
        super(cause);
    }     

}
