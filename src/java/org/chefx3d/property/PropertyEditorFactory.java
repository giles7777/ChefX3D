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

package org.chefx3d.property;

// External Imports
import java.awt.Component;

// Internal Imports
import org.chefx3d.model.WorldModel;

/**
 * Factory for creating Property Editors.
 *
 * @author Alan Hudson
 * @version $Revision: 1.15 $
 */
public interface PropertyEditorFactory {
    
    /**
     * Create a Property Editor.
     *
     * @param model The model
     *
     * @return The editor
     */
    public PropertyEditor createEditor(WorldModel model, Component parentFrame);

}
