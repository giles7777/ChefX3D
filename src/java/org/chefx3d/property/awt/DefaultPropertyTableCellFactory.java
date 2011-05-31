/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property.awt;

//External Imports
import java.awt.Component;
import java.awt.Color;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

//Internal Imports
import org.chefx3d.model.AssociateProperty;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.property.PropertyTableCellFactory;

import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * 
 * @author Russell Dodds
 * @version $Revision: 1.11 $
 */
public class DefaultPropertyTableCellFactory 
    implements
        PropertyTableCellFactory {
    
    /** The component used to center dialogs */
    private Component frame;
    
    /** The current unit of measurement, default is meters */
    private Unit unitOfMeasure;
    
    public DefaultPropertyTableCellFactory(Component parentFrame) {
        this(parentFrame, Unit.METERS);
    }
 
    public DefaultPropertyTableCellFactory(Component parentFrame, Unit unitOfMeasure) {
        this.frame = parentFrame;
        this.unitOfMeasure = unitOfMeasure;
    }

    // ----------------------------------------------------------
    // Methods required by PropertyTableCellFactory
    // ----------------------------------------------------------

    /** 
     * Create a Property Cell Editor based on the EntityProperty.
     * 
     * @param model - The model
     * @param entityProperty - The property
     * 
     * @return TableCellEditor
     */
    public TableCellEditor createCellEditor(WorldModel model, EntityProperty entityProperty) {
        
        Object value = entityProperty.propertyValue; 
        if (value instanceof float[]) {
            return new ArrayCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if (value instanceof Color) {
            return new ColorCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if (value instanceof double[]) {
            return new ArrayCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if (value instanceof int[]) {
            return new ArrayCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if (value instanceof String[]) {
            return new ArrayCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if (value instanceof AssociateProperty) {
        	return new AssociateCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if(value instanceof Boolean){
        	return new CheckBoxCellEditor(model, frame, entityProperty, unitOfMeasure);
        } else if(value instanceof ListProperty){      
            return new ComboBoxCellEditor(model, frame, entityProperty, unitOfMeasure);
        }
         // else...
        return new DefaultPropertyCellEditor(model, frame, entityProperty, unitOfMeasure);

    }

    /**
     * Create a Property Cell Renderer based on the EntityProperty.
     *
     * @param model - The model
     * @param entityProperty - The property
     *
     * @return TableCellRenderer
     */
    public TableCellRenderer createCellRenderer(WorldModel model, EntityProperty entityProperty) {
        
        Object value = entityProperty.propertyValue;    
        if (value instanceof float[]) {
            return new ArrayCellRenderer(unitOfMeasure);
        } else if (value instanceof Color){           
            return new ColorCellRenderer();
        } else if (value instanceof double[]) {
            return new ArrayCellRenderer(unitOfMeasure);
        } else if (value instanceof int[]) {
            return new ArrayCellRenderer(unitOfMeasure);
        } else if (value instanceof String[]) {
            return new ArrayCellRenderer(unitOfMeasure);
        } else if (value instanceof AssociateProperty) {
            return new AssociateCellRenderer();
        } else if (value instanceof Boolean)  {
        	return new CheckBoxCellRender();
        } else if (value instanceof ListProperty){           
            return new ComboBoxCellRenderer(unitOfMeasure);
        } 
        	
        // else...
        return new DefaultPropertyCellRenderer(unitOfMeasure);

    }

}
