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
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.JCheckBox;
import javax.swing.table.TableCellEditor;

//Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * An Editor for the JTable CheckBox 
 * 
 * @link java.awt.event.ItemListener
 * @link java.awt.Component.JCheckbox
 * @link javax.swing.table.TableCellRenderer
 * 
 * @author Jonathon Hubbard
 * @version $Revision: 1.6 $
 */
public class ColorCellEditor extends BaseCellEditor
    implements TableCellEditor, ActionListener {

    Color currentColor;
    JButton button;
    JColorChooser colorChooser;
    JDialog dialog;
    protected static final String EDIT = "edit";
	
    /**
     * Creates a checkbox table cell editor.
     * 
     * @param worldModel The world model data
     * @param parentFrame The parent frame used for focusing messages
     * @param entityProperty passed in holding the current properties
     */
    public ColorCellEditor(
            WorldModel worldModel, 
            Component parentFrame, 
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {
            
        super(worldModel, parentFrame, entityProperty, unitOfMeasure);

        button = new JButton();
        button.setActionCommand(EDIT);
        button.addActionListener(this);
        button.setBorderPainted(false);
        
        //Set up the dialog that the button brings up.
        colorChooser = new JColorChooser();
        colorChooser.setFont(FontColorUtils.getMediumFont());
        dialog = JColorChooser.createDialog(parentFrame,
                                        "Pick a Color",
                                        true,  //modal
                                        colorChooser,
                                        this,  //OK button handler
                                        null); //no CANCEL button handler
        
	}
	
	// ----------------------------------------------------------
    // Methods required by ActionListener interface
    // ----------------------------------------------------------
	
    /**
     * Handles events from the editor button and from
     * the dialog's OK button.
     */
    public void actionPerformed(ActionEvent e) {
        
        if (EDIT.equals(e.getActionCommand())) {
            //The user has clicked the cell, so
            //bring up the dialog.
            button.setBackground(currentColor);
            colorChooser.setColor(currentColor);
            dialog.setVisible(true);

            //Make the renderer reappear.
            fireEditingStopped();

        } else { //User pressed dialog's "OK" button.
            currentColor = colorChooser.getColor();
        }
        
    }

	
    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------
	
	/**
	 * Sets the value of the CheckBox and returns it
     * 
     * @param table The table displaying the properties
     * @param value - The value being changed
     * @param isSelected - The selected flag
     * @param row - The row number
     * @param column - The column number
     * @return The CheckBox component
     */
	public Component getTableCellEditorComponent(
	        JTable table, 
	        Object value, 
			boolean isSelected,
			int row, 
			int column) {
	    
	    if(value instanceof EntityProperty){
	        entityProperty = (EntityProperty)value;
	        currentColor = (Color)entityProperty.propertyValue;
	    } else {                
	        currentColor = (Color)value;
	    }
        
        return button;
	     
	}
		
    /**
     * Get the current value object
     * 
     * @return The property
     */
	public Object getCellEditorValue() {
	     
        // update to the new value
	    entityProperty.propertyValue = currentColor;
	     
		return entityProperty;
		
	}
	
}
