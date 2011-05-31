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

/**
 * Validates a model against its rules.  Each entity has a multiplicity
 * constraint that must be meet. Other validators can implement
 * this interface for custom logic.
 *
 * @author Alan Hudson
 */

public interface ModelValidator {
    /**
     * Validate a model against its rules.
     *
     * @param model The model validate
     * @param listener A listener for validation errors.  Null values are ignored
     * @return Is the model valid
     */
    public boolean validate(WorldModel model, ValidationErrorListener listener);
    
}