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
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.chefx3d.property.*;
import org.chefx3d.view.ViewManager;
import org.chefx3d.model.AssociateProperty;
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityProperty;

// Internal Imports

/**
 * An 
 *
 * @author Russell Dodds
 * @version $Revision: 1.5 $
 */
public class AssociateCellRenderer extends DefaultTableCellRenderer {

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 
    protected static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

    // We need a place to store the color the JLabel should be returned 
    // to after its foreground and background colors have been set 
    // to the selection background color. 
    // These ivars will be made protected when their names are finalized. 
    protected Color unselectedForeground; 
    protected Color unselectedBackground; 
    
    /**
     * Creates a default table cell renderer.
     */
    public AssociateCellRenderer() {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());
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
                
                if (propertyValue instanceof AssociateProperty) {
                    propertyValue = ((AssociateProperty)propertyValue).getValue();
                }
                     
                if (propertyValue instanceof Entity) {
                    setValue(((Entity)propertyValue).getName());
                } else {
                    setValue("");
                }
                 
        } else {
            
            setValue(value);
            
        }
                
        return this;
        
    }
        
    
    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
        
    protected static Border getNoFocusBorder() {
        if (System.getSecurityManager() != null) {
            return SAFE_NO_FOCUS_BORDER;
        } else {
            return noFocusBorder;
        }
    }

    
}
