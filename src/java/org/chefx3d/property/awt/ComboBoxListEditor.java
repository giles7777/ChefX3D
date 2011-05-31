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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


//Internal Imports
import org.chefx3d.model.AbstractProperty;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.FontColorUtils;

/**
 * An Editor for the JTable ComboBox 
 * 
 * @link javax.swing.AbstractCellEditor
 * @link javax.swing.table.TableCellEditor 
 * 
 * @author Jonathon Hubbard
 * @version $Revision: 1.7 $
 */
public class ComboBoxListEditor extends JLabel 
    implements ComboBoxEditor {
	       
    /** Stores the string array for the combo box to pull from. */
    private ListProperty listProperty;
        

    /**
     * Calls the AbstractCellEditor's Constructor then copies information out of 
     * the EntityProperty to be used
     * 
     * @param worldModel - The world mode data
     * @param parentFrame - The parent frame used for focusing messages
     * @param entityProperty passed in holding the current possible ListProperty
     */
    public ComboBoxListEditor(ListProperty list) {
            
        listProperty = list;
        setFont(FontColorUtils.getMediumFont());
        setBackground(FontColorUtils.getBackgroundColor());
        setForeground(FontColorUtils.getForegroundColor());
        
    }

    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------
    
    /** Return the component that should be added to the tree hierarchy for
     * this editor
     */
    public Component getEditorComponent() {        
        setText(listProperty.getSelectedValue());
        return this;        
    }
   
    /** Set the item that should be edited. Cancel any editing if necessary **/
    public void setItem(Object anObject) {
     // ignore
    }

    /** Return the edited item **/
    public Object getItem() {
        return listProperty.getValue();
    }

    /** Ask the editor to start editing and to select everything **/
    public void selectAll() {
     // ignore
    }

    /** Add an ActionListener. An action event is generated when the edited item changes **/
    public void addActionListener(ActionListener l) {
     // ignore
    }

    /** Remove an ActionListener **/
    public void removeActionListener(ActionListener l) {
        // ignore
    }
}


