
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
import javax.swing.border.*;
import java.awt.Color;
import java.awt.Component;
//Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.util.FontColorUtils;

/**
 * An Editor for the JTable CheckBox 
 * Extends the JCheckBox and Implements Table Cell Renderer
 * 
 * @link java.awt.Component.JCheckbox
 * @link javax.swing.table.TableCellRenderer
 * 
 * @author Jonathon Hubbard
 * @version $Revision: 1.5 $
 */
public class CheckBoxCellRender  extends JCheckBox implements TableCellRenderer {

	private Border defaultBorder;
	
    /**
     * Calls the JCheckBox Constructor, then sets the alignment background 
     * and border of the checkbox  
     */
    public CheckBoxCellRender(){
        super();
        setHorizontalAlignment(JLabel.CENTER);
        setBackground(Color.white);
        setBorderPainted(true);
        setFont(FontColorUtils.getMediumFont());
        this.setBorder(defaultBorder);
    }

    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------
    
    /**
     * Get the renderer component
     * 
     * @param table The table displaying the properties
     * @param value - The value being changed
     * @param isSelected - The selected flag
     * @param hasFocus - The focused flag
     * @param row - The row number
     * @param column - The column number
     * @return The JCheckBox component
     */
    public Component getTableCellRendererComponent(
            JTable table, 
            Object value,
            boolean isSelected, 
            boolean hasFocus, 
            int row, 
            int column){
    	

        if(value instanceof EntityProperty){
    	    EntityProperty valueProperty= (EntityProperty)value;
       		this.setSelected((Boolean)valueProperty.propertyValue);
        }else
      	    this.setSelected((Boolean)value);
    	
    	return this;    	
    }  
    
}
