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
import javax.swing.table.*;

// Internal Imports
import org.chefx3d.model.AssociateProperty;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.UnitConversionUtilities.Unit;
import org.chefx3d.view.View;
import org.chefx3d.view.ViewManager;
import org.chefx3d.property.*;

/**
 * An editor that handles the association task.  When enabled
 * it will notify all Views that an association is requested.  
 * Once the association click is completed this editor will be 
 * canceled and the renderer will be displayed.
 *
 * @author Russell Dodds
 * @version $Revision: 1.11 $
 */
public class AssociateCellEditor extends BaseCellEditor 
    implements 
        TableCellEditor, 
        View  {
     
    /** The default label */
    private JLabel textLabel;
    
    /** The view manager */
    private ViewManager viewManager;

    /**
     * Creates a default table cell renderer.
     */
    public AssociateCellEditor(            
            WorldModel worldModel, 
            Component parentFrame, 
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {
            
        super(worldModel, parentFrame, entityProperty, unitOfMeasure);
             
        viewManager = ViewManager.getViewManager();
        viewManager.addView(this);
        
        textLabel = new JLabel();
        textLabel.setText("Select...");
         
    }

    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------
    
    public Object getCellEditorValue() {

        // get the resulting associate value
        return entityProperty;
        
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                     
        AssociateProperty associate = (AssociateProperty)entityProperty.propertyValue;

        // enable association mode
        viewManager.enableAssociateMode(
                associate.getValidTypes(), 
                entityProperty.propertySheet, 
                entityProperty.propertyName);

        return textLabel;
        
    }
    
    // ----------------------------------------------------------
    // Methods required by View
    // ----------------------------------------------------------

    /**
     * Sit pretty.
     */
    public void shutdown(){
        // ignored
    }
    
    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        // ignore
    }

    /**
     * Go into associate mode. The next mouse click will perform
     * a property update
     *
     * @param validTools A list of the valid tools. null string will be all
     *        valid. empty string will be none.
     * @param propertyGroup The grouping the property is a part of
     * @param propertyName The name of the property being associated
     */
    public void enableAssociateMode(
            String[] validTools, 
            String propertyGroup, 
            String propertyName) {
        // ignore
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
        // Make the renderer reappear -
        //  because the update of the entity property has 
        //  already occurred we just want to cancel, this 
        //  will then get the new value and display it.
        this.fireEditingCanceled();

    }


    /**
     * Get the viewID. This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID() {
        // TODO: What to do here
        return -1;
    }

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode) {
        // ignore
    }

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode) {
        // ignore
    }

    /**
     * Register an error reporter with the view instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        // ignore
    }

    /** 
     * Get a component.
     * 
     * @return This object
     */
    public Object getComponent() {
        return this;
    }

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {
        return null;
        // ignore
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        // ignore
    }

    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
    
}
