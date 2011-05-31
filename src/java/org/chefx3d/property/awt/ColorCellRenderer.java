
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
 * @version $Revision: 1.4 $
 */
public class ColorCellRenderer extends JLabel
    implements TableCellRenderer {

    /** The unselected border style */
    private Border unselectedBorder;
    
    /** The selected border style */
    private Border selectedBorder;
    	
    /**
     * Calls the JLabel Constructor, then sets the alignment background 
     * and border of the component
     */
    public ColorCellRenderer() {
        super();
        setOpaque(true); //MUST do this for background to show up.
        setFont(FontColorUtils.getMediumFont());
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
     * @return The JLabel component
     */
    public Component getTableCellRendererComponent(
            JTable table, 
            Object value,
            boolean isSelected, 
            boolean hasFocus,
            int row, 
            int column) {
        
        // get the color
        Color newColor = new Color(0, 0, 0);        
        if(value instanceof EntityProperty){
            EntityProperty valueProperty = (EntityProperty)value;           
            newColor = (Color)valueProperty.propertyValue;           
        } else {           
            newColor = (Color)value;
        }
        setBackground(newColor);

        // draw the border as needed
        if (isSelected) {
            if (selectedBorder == null) {
                selectedBorder = 
                    BorderFactory.createMatteBorder(
                            2,5,2,5,
                            table.getSelectionBackground());
            }
            setBorder(selectedBorder);
        } else {
            if (unselectedBorder == null) {
                unselectedBorder = 
                    BorderFactory.createMatteBorder(
                            2,5,2,5,
                            table.getBackground());
            }
            setBorder(unselectedBorder);
        }
        
        // add a helper tooltip
        setToolTipText("RGB value: " + newColor.getRed() + ", "
                                     + newColor.getGreen() + ", "
                                     + newColor.getBlue());       
               
        return this;
        
    }
    
}
