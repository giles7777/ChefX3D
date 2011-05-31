/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property.swt;

// External Imports
import java.awt.Component;

// Internal Imports

import org.chefx3d.property.PropertyEditor;
import org.chefx3d.property.PropertyEditorFactory;
import org.chefx3d.model.WorldModel;

/**
 * Factory for creating SWT based Property Editors.
 *
 * @author Russell Dodds
 * @version $Revision: 1.10 $
 */
public class SWTPropertyEditorFactory implements PropertyEditorFactory {
    
    /**
     * Create a Property Editor.
     *
     * @param model The model
     * @param parentFrame the parent frame to send messages to 
     *
     * @return The editor
     */
    public PropertyEditor createEditor(WorldModel model, Component parentFrame) {
        return null;
    }

}
