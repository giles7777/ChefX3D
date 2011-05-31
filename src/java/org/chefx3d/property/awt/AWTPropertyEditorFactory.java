/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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

// External Imports

// Internal Imports
import java.awt.Component;

import org.chefx3d.property.PropertyEditor;
import org.chefx3d.property.PropertyEditorFactory;
import org.chefx3d.model.WorldModel;

/**
 * Factory for creating AWT Property Editors.
 *
 * @author Russell Dodds
 * @version $Revision: 1.12 $
 */
public class AWTPropertyEditorFactory implements PropertyEditorFactory {

    /** The property editor to use */
    private PropertyEditor propertyEditor;
     
    // ----------------------------------------------------------
    // Methods required by PropertyEditorFactory
    // ----------------------------------------------------------

    /**
     * Create a Property Editor.
     *
     * @param model The model
     * @param parentFrame The frame to send messages to
     *
     * @return The editor
     */
    public PropertyEditor createEditor(WorldModel model, Component parentFrame) {

        if (propertyEditor == null) {
            propertyEditor = new DefaultPropertyEditor(model, parentFrame);
        }
        
        return propertyEditor; 

    }
    
    // ----------------------------------------------------------
    // Local Methods 
    // ----------------------------------------------------------
 
    /**
     * Override the PropertyEditorFactory with a custom implementation
     * 
     * @param propertyEditor - The custom PropertyEditorFactory class
     */
    public void setPropertyEditor(PropertyEditor propertyEditor) {
        this.propertyEditor = propertyEditor;
    }

    /**
     * @return the propertyEditor
     */
    public PropertyEditor getPropertyEditor() {
        return propertyEditor;
    }

}
