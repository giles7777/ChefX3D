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
import javax.swing.*;
import javax.swing.table.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

//Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.ListProperty;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * A Renderer for the drop down box. It displays the text in the cell as a JLabel
 * 
 * @link javax.swing.table.DefaultTableCellRenderer
 * 
 * @author Jonathon Hubbard
 * @version $Revision: 1.10 $
 */
public class ComboBoxCellRenderer  extends DefaultTableCellRenderer {
       	
    protected JLabel label;	
	   
    /** The current unit of measurement, default is meters */
    private Unit unitOfMeasure;

    private String unitLabel;

    /**
     * Basic Constructor that calls the DefaultTableCellRenderer construct
     * and initializes the JLabel
     */
    public ComboBoxCellRenderer(Unit unitOfMeasure) {
        
        super();
        label = new JLabel();	
        
        Color foregroundColor = FontColorUtils.getForegroundColor();
        Color backgroundColor = FontColorUtils.getBackgroundColor();
        Font labelFont = FontColorUtils.getMediumFont();

        label.setFont(labelFont);
        label.setBackground(backgroundColor);
        label.setForeground(foregroundColor);
        
        this.unitOfMeasure = unitOfMeasure;
        unitLabel = UnitConversionUtilities.getLabel(unitOfMeasure);

    }
	   
    /**
     * Get the renderer component
     * 
     * @param table The table displaying the properties
     * @param value - The value being changed
     * @param isSelected - The selected flag
     * @param hasFocus - The focused flag
     * @param row - The row number
     * @param column - The column number
     * @return The JLabel component
     */
    public Component getTableCellRendererComponent(
            JTable table, 
            Object value,
            boolean isSelected, 
            boolean hasFocus, 
            int row, 
            int column) {
	   
        EntityProperty currentValue = (EntityProperty)value;
        ListProperty currentList = (ListProperty)currentValue.propertyValue;
	  
        // set if this is enabled
        label.setEnabled(currentList.isEditable());

        if(currentList.getValue() == null) {
            
            label.setText("Please Select an Item");  
            
        } else {
            
            String displayValue = 
                convertToUnitOfMeasure(currentList.getSelectedValue());           
            label.setText(displayValue);  
            
        }
        return label;
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Tries to convert the number into the specified unit
     * of measurement.  Just returns the unmodified String if
     * it fails.
     */
    private String convertToUnitOfMeasure(String number) {
        
        String retVal = number;
        try {
            
            // make sure its in meters to store it
            float floatVal = Float.parseFloat(number);
            floatVal = 
                UnitConversionUtilities.convert(
                        Unit.METERS, 
                        floatVal, 
                        unitOfMeasure);   
            retVal = String.valueOf(floatVal);
            retVal += " " + unitLabel;
            
        } catch (NumberFormatException nfe){
            // just use the string value found
        }
        
        return retVal;

    }

}
