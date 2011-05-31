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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;

// Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.WorldModel;

/**
 * Factory for creating Property Editors.
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public interface PropertyTableCellFactory {
    
    /**
     * Create a Property Cell Editor based on the EntityProperty.
     *
     * @param model - The model
     * @param entityProperty - The property
     *
     * @return TableCellEditor
     */
    public TableCellEditor createCellEditor(WorldModel model, EntityProperty entityProperty);

    /**
     * Create a Property Cell Renderer based on the EntityProperty.
     *
     * @param model - The model
     * @param entityProperty - The property
     *
     * @return TableCellRenderer
     */
    public TableCellRenderer createCellRenderer(WorldModel model, EntityProperty entityProperty);

}
