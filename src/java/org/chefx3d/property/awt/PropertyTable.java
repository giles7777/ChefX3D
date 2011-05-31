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
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;

// Internal Imports
import org.chefx3d.property.PropertyTableCellFactory;
import org.chefx3d.util.FontColorUtils;

import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.WorldModel;

/**
 * A JTable implementation used to view and edit entity properties
 *
 * @author Russell Dodds
 * @version $Revision: 1.14 $
 */
public class PropertyTable extends JTable {
   
    /** The world model */
    private WorldModel model;

    /** The property editor factory used to create the editor & renders */
    private PropertyTableCellFactory editorCellFactory;
        
    /**
     * 
     * @param model
     */
    public PropertyTable(WorldModel model, Component parentFrame) {
        
        super();
      
        this.model = model;
        this.editorCellFactory = new DefaultPropertyTableCellFactory(parentFrame);
        this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        
        Color foregroundColor = FontColorUtils.getForegroundColor();
        Color backgroundColor = FontColorUtils.getBackgroundColor();
        Font labelFont = FontColorUtils.getMediumFont();
        
        setFont(labelFont);
        setBackground(backgroundColor);
        setForeground(foregroundColor);

        
    }

    // ----------------------------------------------------------
    // Methods override of JTable class
    // ----------------------------------------------------------

    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 1) {
            
            Object property = super.getValueAt(row, column);    
    
//System.out.println("PropertyTable.getCellRenderer");
//System.out.println("    row: " + row);
//System.out.println("    col: " + column);

            if (property instanceof EntityProperty) {
                                
                EntityProperty entityProperty = (EntityProperty)property;  
                
//System.out.println("    name: " + entityProperty.propertyName);
//System.out.println("    value: " + entityProperty.propertyValue);

                return editorCellFactory.createCellRenderer(model, entityProperty);
                
            } else { 
            	
            	EntityProperty entityProperty = new EntityProperty();
            	entityProperty.propertyValue=property;
//System.out.println("Property is: "+entityProperty.propertyValue);
            	return editorCellFactory.createCellRenderer(model, entityProperty);
            }
        }
        // else...
        return super.getCellRenderer(row, column);
    }

    public TableCellEditor getCellEditor(int row, int column) {        
        
        if (column == 1) {
            
            Object property = super.getValueAt(row, column);     

//System.out.println("editor property: " + property);

            if (property instanceof EntityProperty) {
                                
                EntityProperty entityProperty = (EntityProperty)property;
                return editorCellFactory.createCellEditor(model, entityProperty);
                
            } else { 
            	EntityProperty entityProperty = new EntityProperty();
            	entityProperty.propertyValue=property;    
//System.out.println("Property is: "+entityProperty.propertyValue);
            	return editorCellFactory.createCellEditor(model, entityProperty);
            }
            	
        }
        // else...
        return super.getCellEditor(row, column);
       
    }
    
    // ----------------------------------------------------------
    // Local Methods 
    // ----------------------------------------------------------
 
    /**
     * Gets the current cell factory used to define cell renderer and editors
     * 
     * @return the editorCellFactory
     */
    public PropertyTableCellFactory getEditorCellFactory() {
        return editorCellFactory;
    }

    /**
     * Override the PropertyEditorFactory with a custom implementation
     * 
     * @param editorCellFactory the editorCellFactory to set
     */
    public void setEditorCellFactory(PropertyTableCellFactory editorCellFactory) {
        this.editorCellFactory = editorCellFactory;
    }
 
}
