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
import java.awt.*;

import javax.swing.*;
import javax.swing.table.TableCellEditor;

// Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.UnitConversionUtilities.Unit;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.util.UnitConversionUtilities;

/**
 * A base editor for the property table editor
 *
 * @author Russell Dodds
 * @version $Revision: 1.10 $
 */
public class DefaultPropertyCellEditor extends BaseCellEditor 
    implements TableCellEditor {

    /** The component to edit */
    private JTextField textField;
    
    /**
     * Creates a default table cell renderer.
     */
    public DefaultPropertyCellEditor(
            WorldModel worldModel, 
            Component parentFrame, 
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {
            
        super(worldModel, parentFrame, entityProperty, unitOfMeasure);
        
        textField = new JTextField();
        textField.setFont(FontColorUtils.getMediumFont());

    }
    
    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------

    /**
     * @link javax.swing.CellEditor
     */
    public Object getCellEditorValue() {
    	
        // get the new value
        String text = textField.getText();
        
        // check if it is valid
        boolean valid = super.validate(text);
        if (!valid) {
            return entityProperty;
        }
        
        // get the current value
        //entityProperty.previousValue = 
        //    new String(entityProperty.propertyValue.toString());
        
        if(entityProperty.propertyValue instanceof Float){
        	
        	try {
        	    
        	    // make sure its in meters to store it
        	    float floatVal = Float.parseFloat(textField.getText());
        	    floatVal = 
        	        UnitConversionUtilities.convert(
        	                unitOfMeasure, 
        	                floatVal, 
        	                Unit.METERS);     	    
	        	entityProperty.propertyValue = floatVal;
	        	
        	} catch (NumberFormatException nfe){
        	    
        		textField.setText(entityProperty.propertyValue.toString());
        		
        	}
        } else {

        	entityProperty.propertyValue = textField.getText();  
        	
        }
        
    	return entityProperty;
        
    }

    /**
     * @link javax.swing.table.TableCellEditor
     */
    public Component getTableCellEditorComponent (
            JTable table, 
            Object value, 
            boolean isSelected, 
            int row, 
            int column) {
                        
    	if(value instanceof EntityProperty){
    	    entityProperty = (EntityProperty)value;
    	    
    	    String propVal = entityProperty.propertyValue.toString();
    	    try {
                
                // make sure its in the display units
                float floatVal = Float.parseFloat(propVal);
                floatVal = 
                    UnitConversionUtilities.convert(
                            Unit.METERS, 
                            floatVal, 
                            unitOfMeasure);           
                propVal = String.valueOf(floatVal);
                
            } catch (NumberFormatException nfe){
                // use the base string value
            }
    	    textField.setText(propVal);
    	} else
    		textField.setText(value.toString());    

        return textField;
        
    }        
    
    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
    
}
