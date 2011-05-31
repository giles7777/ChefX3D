/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
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

// Standard Imports
// none

// Application specific imports
// none

/**
 * Listen for error reports while validating a model.
 *
 * @author Alan Hudson
 */
 public interface ValidationErrorListener { 
         
     /**
      * An error occured while validating a model.
      *
      * @param msg The error message
      * @param entity The entity that contains the error
      */
     public void error(String msg, Entity entity);
     
 }