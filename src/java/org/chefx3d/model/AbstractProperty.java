/*****************************************************************************
 *                        Web3d.org Copyright (c) 2006
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

// Local imports
import org.chefx3d.util.CloneableProperty;

/**
 * Any object implementing this interface is designated an editable property
 * 
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public abstract class AbstractProperty 
    implements 
        CloneableProperty {

    /** Should this property be visible */
    protected boolean visible;   
    
    /** Should this property be editable */
    protected boolean editable;
    
    /** The value of the property */
    protected Object value;
    
    /** The entity */
    protected Entity parentEntity;
    
    /** Are we waiting for a selection event */
    protected boolean associateMode;

    /** The world model */
    protected WorldModel model;
     
    /** The validator to use if necessary */
    protected PropertyValidator validator;
    
    public AbstractProperty() {
        visible = true;
        editable = true;
    }
    
    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------  

    /**
     * Create a copy of the property
     */
    public abstract CloneableProperty clone();

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------  

    /**
     * Initialize the property
     * 
     * @param worldModel The model that contains the scene data
     * @param entity The entity assigned to the property
     */
    public void initialize(WorldModel worldModel, Entity entity) {
        
        this.model = worldModel;
        this.parentEntity = entity;
                
    }
           
    /**
     * Get the value of the property
     * 
     * @return The value Object
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Set the value of the property
     * 
     * @param value The value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Should the property editor display this property
     * 
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets if the property editor should display this property
     * 
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
      * Should the property editor allow changes to this property
      * 
      * @return the editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets if the property editor should allow changes to this property
     * 
     * @param editable the visible to set
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * @return the validator
     */
    public PropertyValidator getValidator() {
        return validator;
    }

    /**
     * @param validator the validator to set
     */
    public void setValidator(PropertyValidator validator) {
        this.validator = validator;
    }

}
