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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.chefx3d.model.EntityProperty;
import org.chefx3d.util.UnitConversionUtilities.Unit;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.util.UnitConversionUtilities;

// Internal Imports

/**
 * An 
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public class DefaultPropertyCellRenderer extends DefaultTableCellRenderer {

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 
    private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

    // We need a place to store the color the JLabel should be returned 
    // to after its foreground and background colors have been set 
    // to the selection background color. 
    // These ivars will be made protected when their names are finalized. 
    private Color unselectedForeground; 
    private Color unselectedBackground; 
    
    /** The current unit of measurement, default is meters */
    protected Unit unitOfMeasure;
    
    protected Font labelFont;
    
    /** Current style of number formatting desired */
    private NumberFormat formatter;

    /**
     * Creates a default table cell renderer.
     */
    public DefaultPropertyCellRenderer(Unit unitOfMeasure) {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());   
       
        
        this.unitOfMeasure = unitOfMeasure;
        labelFont = FontColorUtils.getMediumFont();
        setFont(labelFont);
        
        // how many digits to use for the tape measure length?
        formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
        
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
        
        setFont(labelFont);

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
			if (entityProperty.propertyValue != null) {
				String propertyValue = entityProperty.propertyValue.toString();
				try {
					
					// make sure its in the display units
					float floatVal = Float.parseFloat(propertyValue);
					floatVal = 
						UnitConversionUtilities.convert(
						Unit.METERS, 
						floatVal, 
						unitOfMeasure);          
					
					propertyValue = formatter.format(floatVal);                  
					propertyValue += " " + UnitConversionUtilities.getLabel(unitOfMeasure);
					
				} catch (NumberFormatException nfe){
					// use the base string value
				}
				setValue(propertyValue);
			}
        } else {
            
            setValue(value);
            
        }
                
        return this;
        
    }
        
    
    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
        
    private static Border getNoFocusBorder() {
        if (System.getSecurityManager() != null) {
            return SAFE_NO_FOCUS_BORDER;
        } else {
            return noFocusBorder;
        }
    }
        
}
