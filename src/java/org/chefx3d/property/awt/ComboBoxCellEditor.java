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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


//Internal Imports
import org.chefx3d.model.AbstractProperty;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.ComboBoxItem;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * An Editor for the JTable ComboBox 
 * 
 * @link javax.swing.AbstractCellEditor
 * @link javax.swing.table.TableCellEditor 
 * 
 * @author Jonathon Hubbard
 * @version $Revision: 1.19 $
 */
public class ComboBoxCellEditor extends BaseCellEditor 
    implements TableCellEditor, ActionListener {
	    
    /** The comboBox that is displayed. */
    private JComboBox comboBox;
    private JLabel label;  

    /** Stores the string array for the combo box to pull from. */
    private ListProperty propertyValue;
        
    /**
     * Calls the AbstractCellEditor's Constructor then copies information out of 
     * the EntityProperty to be used
     * 
     * @param worldModel - The world mode data
     * @param parentFrame - The parent frame used for focusing messages
     * @param entityProperty passed in holding the current possible ListProperty
     */
    public ComboBoxCellEditor(
            WorldModel worldModel, 
            Component parentFrame, 
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {
            
        super(worldModel, parentFrame, entityProperty, unitOfMeasure);

        // Get the ListProperty
        propertyValue = (ListProperty)entityProperty.propertyValue;     
       
        // get the possible items
        String[] keys = propertyValue.getKeys();
        String[] labels = propertyValue.getLabels();
        ImageIcon[] validImages = propertyValue.getValidImages();

        // convert the items over if necessary
        int len = labels.length;
        ComboBoxItem[] convertedLabels = new ComboBoxItem[len];
        for (int i = 0; i < len; i++) {            
            convertedLabels[i] = 
                new ComboBoxItem(convertToUnitOfMeasure(labels[i]), keys[i]);
        }
        Color foregroundColor = FontColorUtils.getForegroundColor();
        Color backgroundColor = FontColorUtils.getBackgroundColor();
        Font labelFont = FontColorUtils.getMediumFont();
        
        // setup the label
        label = new JLabel(convertToUnitOfMeasure(labels[0]));
        label.setEnabled(false);
        label.setFont(labelFont);
        label.setBackground(backgroundColor);
        label.setForeground(foregroundColor);
    
        // setup the combo box
        comboBox = new JComboBox(convertedLabels);  
        comboBox.setFont(labelFont);
        comboBox.setBackground(backgroundColor);
        comboBox.setForeground(foregroundColor);
      
        // use a custom renderer if images are provided
        if (validImages != null) {
            
            //Create the renderer to use.          
            ComboBoxListRenderer renderer = new ComboBoxListRenderer(propertyValue);
            comboBox.setRenderer(renderer);
            ComboBoxListEditor editor = new ComboBoxListEditor(propertyValue);
            comboBox.setEditor(editor);
            //comboBox.setEditable(true);
            comboBox.setMaximumRowCount(3);
            
        }
               
        // convert the current value if necessary
        Integer index = (Integer)propertyValue.getValue();        

        // set the currently selected item.
        comboBox.setSelectedIndex(index);       
        comboBox.addActionListener(this);
        
    }
    
    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------
    
    /**
     * Sets the value of the ComboBox and returns it
     * 
     * @param table The table displaying the properties
     * @param value - The value being changed
     * @param isSelected - The selected flag
     * @param row - The row number
     * @param column - The column number
     * @return The ComboBox component
     */
    public Component getTableCellEditorComponent(
	         JTable table, 
	         Object value, 
	         boolean isSelected, 
	         int row, 
	         int column){	
	     
	     EntityProperty valueProperty = (EntityProperty)value;
         
         entityProperty = new EntityProperty();
         entityProperty.entityID = valueProperty.entityID;
         entityProperty.propertyName = valueProperty.propertyName;
         entityProperty.propertySheet = valueProperty.propertySheet;
         
         if (propertyValue instanceof AbstractProperty) {
             entityProperty.propertyValue = 
                 ((AbstractProperty)valueProperty.propertyValue).clone();
         } else {
             entityProperty.propertyValue = valueProperty.propertyValue;
         }

	     propertyValue = (ListProperty)entityProperty.propertyValue;
	     
	     if (propertyValue.isEditable()) {
	         Integer index = comboBox.getSelectedIndex();
	         
	         propertyValue.setValue(index);  
	         comboBox.setSelectedIndex(index);

	         return comboBox;
	         
	     } else {
	   
	         Integer index = comboBox.getSelectedIndex();
	         propertyValue.setValue(index);  

	         String text = 
	             convertToUnitOfMeasure(propertyValue.getSelectedValue());
	         label.setText(text);  
	         
	         return label;

	     }
	     
    }		

    /**
     * Get the current value object
     * 
     * @return The property
     */
    public Object getCellEditorValue(){	
	     	  
        Integer index = comboBox.getSelectedIndex();       
        propertyValue.setValue(index);  
		 
        return entityProperty;
   
    }	
    
	//----------------------------------------------------------
	// Methods required by the ActionListener interface
	//----------------------------------------------------------

	/**
	 * An action has been performed.
	 *
	 * @param evt The event that caused this method to be called.
	 */
	public void actionPerformed(ActionEvent evt) {
	    this.fireEditingStopped();
	}  

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------	
	
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

    /**
     * Tries to convert the number into the specified unit
     * of measurement.  Just returns the unmodified String if
     * it fails.
     */
    private String convertToMeters(String number) {
        
        String retVal = number;
        
        // remove the unit of measurement label
        if (number.endsWith(unitLabel)) {
            int index = number.lastIndexOf(unitLabel);
            retVal = number.substring(0, index - 1).trim();
        }
        
        try {
            
            // make sure its in meters to store it
            float floatVal = Float.parseFloat(retVal);
            floatVal = 
                UnitConversionUtilities.convert(
                        unitOfMeasure, 
                        floatVal, 
                        Unit.METERS);   
            retVal = String.valueOf(floatVal);
            
        } catch (NumberFormatException nfe){
            // just use the string value found
        }
        
        return retVal;

    }
	
}


