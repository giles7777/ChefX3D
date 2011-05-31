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
import java.text.NumberFormat;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.util.UnitConversionUtilities.Unit;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;

// Internal Imports

/**
 * An 
 *
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
public class ArrayCellRenderer extends DefaultTableCellRenderer {

    /** The maximum number of digits for an fraction (float or double) */
    private final static int MAX_FRACTION_DIGITS = 2;

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 
    private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

    /** Set the formating of numbers for output */
    private NumberFormat numberFormater;

    // We need a place to store the color the JLabel should be returned 
    // to after its foreground and background colors have been set 
    // to the selection background color. 
    // These ivars will be made protected when their names are finalized. 
    private Color unselectedForeground; 
    private Color unselectedBackground; 
    
    /**
     * Creates a default table cell renderer.
     */
    public ArrayCellRenderer(Unit unitOfMeasure) {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());
        
        numberFormater = NumberFormat.getNumberInstance();
        numberFormater.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        numberFormater.setGroupingUsed(false);

    }

    // ----------------------------------------------------------
    // Methods override of DefaultTableCellRenderer class
    // ----------------------------------------------------------

    /**
     * Overrides the DefaultTableCellRenderer implementation.
     */
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        
        if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            super.setForeground((unselectedForeground != null) ? 
                    unselectedForeground : table.getForeground());
            super.setBackground((unselectedBackground != null) ? 
                    unselectedBackground : table.getBackground());
        }
 
        setFont(table.getFont());

        if (hasFocus) {
            Border border = null;
            if (isSelected) {
                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
            }
            if (border == null) {
                border = UIManager.getBorder("Table.focusCellHighlightBorder");
            }
            setBorder(border);

            if (!isSelected && table.isCellEditable(row, column)) {
                Color col;
                col = UIManager.getColor("Table.focusCellForeground");
                if (col != null) {
                    super.setForeground(col);
                }
                col = UIManager.getColor("Table.focusCellBackground");
                if (col != null) {
                    super.setBackground(col);
                }
            }
        } else {
            setBorder(getNoFocusBorder());
        }

        if (value instanceof EntityProperty) {
                
                EntityProperty entityProperty = (EntityProperty)value;
                Object propertyValue = entityProperty.propertyValue;    
                
                updateValue(propertyValue);
                
        } else {
            
            updateValue(value);
            
        }
                
        return this;
        
    }
        
    
    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
    
    private void updateValue(Object value) {
        
        StringBuilder buff = new StringBuilder();
        if (value instanceof float[]) {
            
            float[] array_value = (float[])value;
 
            if (array_value.length == 0) {
                setValue("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                setValue(buff.toString()); 
            }
          
        } else if (value instanceof double[]) {
           
            double[] array_value = (double[])value;

            if (array_value.length == 0) {
                setValue("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                setValue(buff.toString()); 
            }

        } else if (value instanceof int[]) {
           
            int[] array_value = (int[])value;
            
            if (array_value.length == 0) {
                setValue("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                setValue(buff.toString());                 
            }
           
        } else if (value instanceof String[]) {
           
            String[] array_value = (String[])value;

            if (array_value.length == 0) {
                setValue("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(array_value[i]);
                    buff.append(", ");               
                }
                buff.append(array_value[array_value.length - 1]);
                setValue(buff.toString()); 
            }

        } else {
            
            setValue(value); 
            
        }        

    }
    
    private static Border getNoFocusBorder() {
        if (System.getSecurityManager() != null) {
            return SAFE_NO_FOCUS_BORDER;
        } else {
            return noFocusBorder;
        }
    }

    
}
