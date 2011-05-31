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
 * @version $Revision: 1.8 $
 */
public class CheckBoxCellEditor extends BaseCellEditor
    implements TableCellEditor, ItemListener{

	/** This is used to listen for when the comboBox is changed */
	private Vector<CellEditorListener> editorListeners;
	
	/** The component used for editing */
    private JCheckBox checkBox;
    
    /**
     * Creates a checkbox table cell editor.
     * 
     * @param worldModel The world model data
     * @param parentFrame The parent frame used for focusing messages
     * @param entityProperty passed in holding the current properties
     */
    public CheckBoxCellEditor(
            WorldModel worldModel, 
            Component parentFrame, 
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {
            
        super(worldModel, parentFrame, entityProperty, unitOfMeasure);

        
        editorListeners = new Vector<CellEditorListener>();
        
        checkBox = new JCheckBox();
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.setBackground(Color.white);
        checkBox.setFont(FontColorUtils.getMediumFont());
	}
	
	// ----------------------------------------------------------
    // Methods required by ItemListener interface
    // ----------------------------------------------------------
	
	/**
	 * Called when the checkBox has been selected or deselected
	 * 
	 * @param e The current state
	 */
	public void itemStateChanged(ItemEvent e){
	     
	    for(int i=0; i<editorListeners.size(); ++i) {
	        editorListeners.elementAt(i).editingStopped(new ChangeEvent(this));
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
	        entityProperty=(EntityProperty)value;
	        checkBox.setSelected((Boolean)entityProperty.propertyValue);
	    } else {	    		
	        checkBox.setSelected((Boolean)value);
	    }
	    return checkBox;
	     
	}
		
    /**
     * Get the current value object
     * 
     * @return The property
     */
	public Object getCellEditorValue() {
	     
        // get the current value
        //entityProperty.previousValue = entityProperty.propertyValue;

        // update to the new value
	    entityProperty.propertyValue = checkBox.isSelected();
	     
		return entityProperty;
	}
	
}
