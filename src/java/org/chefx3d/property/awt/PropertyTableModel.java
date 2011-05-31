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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javax.swing.table.DefaultTableModel;
import javax.swing.SwingUtilities;

// Internal Imports
import org.chefx3d.model.*;

/**
 * An
 *
 * @author Russell Dodds
 * @version $Revision: 1.25 $
 */
public class PropertyTableModel extends DefaultTableModel
    implements
        EntityPropertyListener {

    /** Are we waiting for a selection event */
    protected boolean associateMode;

    // TODO: this should be dynamic
    private String[] columnNames = new String[] {"Name", "Value"};

    /** The number of rows/properties */
    private int rows;

    /** The world model */
    private WorldModel model;

    /** The entity these properties represent */
    private Entity currentEntity;
    
    /** The complete list of entities */
    private Entity[] currentList;

    /** What row is a property displayed at */
    private HashMap<String, Integer> propertyRowMap;

    /** Should we apply the update */
    public boolean applyCommand;

    /** Should the update be transient */
    public boolean transientCommand;

    /**
      */
    public PropertyTableModel(WorldModel worldModel) {

        super();
        this.model = worldModel;

    }

    /**
     *
     * @param entity
     * @param rows
     */
    public void setEntity(Entity[] currentList, int rows) {

        if (currentEntity != null) {
            currentEntity.removeEntityPropertyListener(this);
        }

        this.currentList = currentList;
        this.currentEntity = currentList[0];
        this.rows = rows;

        currentEntity.addEntityPropertyListener(this);

        Object[][] data = new Object[rows][2];
        super.setDataVector(data, columnNames);

        propertyRowMap = new HashMap<String, Integer>();

        associateMode = false;
        applyCommand = false;

    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return rows;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }
    
    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if (col < 1) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {

        if (value instanceof EntityProperty) {

            // update the property to row mapping
            EntityProperty entityProperty = (EntityProperty)value;
            propertyRowMap.put(entityProperty.propertyName, row);

            // get what is currently stored for this property
            Object previousValue =
                currentEntity.getProperty(
                    entityProperty.propertySheet,
                    entityProperty.propertyName);

//System.out.println("PropertyTableModel.setValueAt");
//System.out.println("    previousValue: " + previousValue + " (" + previousValue.hashCode() + ")");

            if (previousValue != null && previousValue instanceof AbstractProperty) {
                    previousValue = ((AbstractProperty)previousValue).clone();
//System.out.println("    previousValue: " + previousValue + "(" + previousValue.hashCode() + ")");
            }
            
//System.out.println("    row: " + row);
//System.out.println("    col: " + col);
//System.out.println("    name: " + entityProperty.propertyName);
//System.out.println("    value: " + entityProperty.propertyValue);
//System.out.println("    previousValue: " + previousValue);
//System.out.println("    applyCommand: " + applyCommand);

            if (applyCommand) {

                ArrayList<Command> commandList = new ArrayList<Command>();
                
                int len = currentList.length;
                for (int i = 0; i < len; i++) {
                    Entity entity = currentList[i];
                    
                    if (transientCommand) {
                        // create the command to send
                        ChangePropertyTransientCommand cmd =
                            new ChangePropertyTransientCommand(
                                    entity,
                                    entityProperty.propertySheet,
                                    entityProperty.propertyName,
                                    entityProperty.propertyValue,
                                    model);
                        commandList.add(cmd);                  
                    } else {
                        // create the command to send
                        ChangePropertyCommand cmd =
                            new ChangePropertyCommand(
                                    entity,
                                    entityProperty.propertySheet,
                                    entityProperty.propertyName,
                                    previousValue,
                                    entityProperty.propertyValue,
                                    model);
                        commandList.add(cmd);                      
                    }
                }
     
                if (transientCommand) {
                    // finally stack it all together and send out
                    MultiTransientCommand cmd = 
                        new MultiTransientCommand(
                                commandList, 
                                commandList.get(0).getDescription());

                    // apply the change to the model
                    model.applyCommand(cmd);
                } else {
                    // finally stack it all together and send out
                    MultiCommand cmd = 
                        new MultiCommand(
                                commandList, 
                                commandList.get(0).getDescription());

                    // apply the change to the model
                    model.applyCommand(cmd);                   
                }

            }

            // update the data
            super.setValueAt(entityProperty, row, col);

        } else {

            // update the data
            super.setValueAt(value, row, col);

        }

        // force the table to be only renderers and reset
        super.fireTableStructureChanged();

    }

    // ----------------------------------------------------------
    // Methods required by EntityPropertyListener interface
    // ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void propertyUpdated(
            int entityID,
            String propertySheet,
            String propertyName, boolean ongoing) {

//System.out.println("PropertyTableModel.propertyUpdated");
//System.out.println("    entityID: " + entityID);
//System.out.println("    currentEntity.getEntityID(): " + currentEntity.getEntityID());
//System.out.println("    propertySheet: " + propertySheet);
//System.out.println("    propertyName: " + propertyName);

        if (entityID == currentEntity.getEntityID() && propertyRowMap != null) {

            // get the updated value
            Object value = currentEntity.getProperty(propertySheet, propertyName);

//System.out.println("    value: " + value + "(" + value.hashCode() + ")");
//System.out.println("    propertyRowMap: " + propertyRowMap);

            // get the row
            if (!propertyRowMap.containsKey(propertyName))
                return;

            int row = propertyRowMap.get(propertyName);

            // get the stored in the property table
            EntityProperty entityProperty = (EntityProperty)getValueAt(row, 1);
            if (entityProperty == null)
                return;
            
            // update the value
            entityProperty.propertyValue = value;

//System.out.println("    entityProperty: " + entityProperty + "(" + entityProperty.hashCode() + ")");

            // update the data
            super.fireTableDataChanged();

        }

    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        // ignored
    }

}
