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
import javax.swing.table.*;

// Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.UnitConversionUtilities.Unit;

/**
 * An 
 *
 * @author Russell Dodds
 * @version $Revision: 1.6 $
 */
public class ArrayCellEditor extends BaseCellEditor 
    implements TableCellEditor {

    private static enum PropertyTypes {INTEGER, FLOAT, DOUBLE, STRING};

    /** The maximum number of digits for an fraction (float or double) */
    private final static int MAX_FRACTION_DIGITS = 2;

    /** Set the formating of numbers for output */
    private NumberFormat numberFormater;
    
    private JTextField textField;
          
    private PropertyTypes type;
    
    /**
     * Creates a array table cell editor.
     * 
     * @param parentFrame
     * @param entityProperty
     */
    public ArrayCellEditor(
            WorldModel worldModel, 
            Component parentFrame, 
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {
            
        super(worldModel, parentFrame, entityProperty, unitOfMeasure);
        
        numberFormater = NumberFormat.getNumberInstance();
        numberFormater.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        numberFormater.setGroupingUsed(false);
                
        textField = new JTextField();

        Object value = entityProperty.propertyValue;              
        StringBuilder buff = new StringBuilder();
        
        if (value instanceof float[]) {
            
            type = PropertyTypes.FLOAT;            
            float[] array_value = (float[])value;
            if (array_value.length == 0) {
                textField.setText("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                textField.setText(buff.toString()); 
            }
           
        } else if (value instanceof double[]) {
           
            type = PropertyTypes.DOUBLE;
            double[] array_value = (double[])value;
            if (array_value.length == 0) {
                textField.setText("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                textField.setText(buff.toString()); 
            }

        } else if (value instanceof int[]) {
           
            type = PropertyTypes.INTEGER;
            int[] array_value = (int[])value;
            
            if (array_value.length == 0) {
                textField.setText("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                textField.setText(buff.toString()); 
            }

           
        } else if (value instanceof String[]) {
           
            type = PropertyTypes.STRING;
            String[] array_value = (String[])value;
            if (array_value.length == 0) {
                textField.setText("");
            } else {
                for (int i = 0; i < array_value.length - 1; i++) {
                    buff.append(numberFormater.format(array_value[i]));
                    buff.append(", ");               
                }
                buff.append(numberFormater.format(array_value[array_value.length - 1]));
                textField.setText(buff.toString()); 
            }

        } else {
            
            textField.setText(value.toString()); 
            
        }        
        
    }

    // ----------------------------------------------------------
    // Methods required by TableCellEditor interface
    // ----------------------------------------------------------
    
    public Object getCellEditorValue() {

        // get the new value
        String text = textField.getText();
        
        // check if it is valid
        boolean valid = super.validate(text);
        if (!valid) {
            return entityProperty;
        }
          
        // if valid then update the property
        String[] items = text.split(",");
                
        switch (type) {
            
            case FLOAT:      
                
                // get the current value
                //float[] fltTmp = (float[])entityProperty.propertyValue;
                //float[] currentFloat = new float[fltTmp.length];
                //for (int i = 0; i < fltTmp.length; i++) {
                //    currentFloat[i] = fltTmp[i];
                //}
                //entityProperty.previousValue = currentFloat;

                float[] floatResult = new float[items.length];
                for (int i = 0; i < items.length; i++) {
                    floatResult[i] = Float.parseFloat(items[i].trim());
                }
                entityProperty.propertyValue = floatResult;
                break;
                
            case INTEGER:
                
                // get the current value
                //int[] intTmp = (int[])entityProperty.propertyValue;
                //int[] currentInt = new int[intTmp.length];
                //for (int i = 0; i < intTmp.length; i++) {
                //    currentInt[i] = intTmp[i];
                //}
                //entityProperty.previousValue = currentInt;

                // update the new value
                int[] intResult = new int[items.length];
                for (int i = 0; i < items.length; i++) {
                    intResult[i] = Integer.parseInt(items[i].trim());
                }
                entityProperty.propertyValue = intResult;
                break;
                
            case DOUBLE:
                
                // get the current value
                //double[] dblTmp = (double[])entityProperty.propertyValue;
                //double[] currentDouble = new double[dblTmp.length];
                //for (int i = 0; i < dblTmp.length; i++) {
                //    currentDouble[i] = dblTmp[i];
                //}
                //entityProperty.previousValue = currentDouble;
                
                // update the new value
                double[] doubleResult = new double[items.length];
                for (int i = 0; i < items.length; i++) {
                    doubleResult[i] = Double.parseDouble(items[i].trim());
                }
                entityProperty.propertyValue = doubleResult;
                break;
                
            case STRING:
                
                // get the current value
                //String[] strTmp = (String[])entityProperty.propertyValue;
                //String[] currentString = new String[strTmp.length];
                //for (int i = 0; i < strTmp.length; i++) {
                //    currentString[i] = strTmp[i];
                //}
                //entityProperty.previousValue = currentString;

                // update the new value
                String[] stringResult = new String[items.length];
                for (int i = 0; i < items.length; i++) {
                    stringResult[i] = items[i].trim();
                }
                entityProperty.propertyValue = stringResult;
                break;
                
            default:
                
                // get the current value
                //entityProperty.previousValue = 
                //    new String(entityProperty.propertyValue.toString());
            
                // update the new value
                entityProperty.propertyValue = text;
                break;
                
        }
        
        return entityProperty;
        
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        
        return textField;
        
    }
    
    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
    
}
