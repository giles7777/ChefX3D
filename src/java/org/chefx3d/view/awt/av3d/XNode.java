/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/gpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
import java.util.ArrayList;
import java.util.HashMap;

import org.web3d.vrml.lang.FieldConstants;

// Local imports
// None

/**
 * A generic container for X3D content parsed from a file. This representation
 * is minimalistic (i.e. the nodes contain only the fields extracted from the
 * file, no 'default' values are initialized). The field values have been
 * converted from Strings into more 'primitive' forms.
 *
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
public class XNode {

    /** The name of the node */
    private final String nodeName;

    /** The field type map, keyed by field name */
    private final HashMap <String, Integer>fieldInfo;

    /** The array of all field names */
    private String[] allFieldNames;

    /** The field data map, keyed by field name */
    private HashMap <String, Object>fieldData;

    /** Indentation for the print method */
    private static String indent = "";

    /**
     * Constructor
     *
     * @param nodeName The node's name
     * @param fieldInfo The node's field information. A map keyed by the
     * field name with the data type identifier as value.
     */
    XNode(String nodeName, HashMap <String, Integer>fieldInfo) {
        this.nodeName = nodeName;
        this.fieldInfo = fieldInfo;
    }

    /**
     * Return the node name
     *
     * @return The node name
     */
    String getNodeName() {
        return(nodeName);
    }

    /**
     * Return the field type. If the field does not exist,
     * -1 is returned.
     *
     * @param fieldName The field identifer
     * @return The field type.
     */
    int getFieldType(String fieldName) {
        if (fieldInfo.containsKey(fieldName)) {
            return(fieldInfo.get(fieldName));
        } else {
            return(-1);
        }
    }

    /**
     * Return the array of all field names.
     *
     * @return The array of all field names.
     */
    String[] getAllFieldNames() {
        if (allFieldNames == null) {
            allFieldNames = new String[fieldInfo.size()];
            fieldInfo.keySet().toArray(allFieldNames);
        }
        return(allFieldNames);
    }

    /**
     * Return the array of used field names. If none are
     * used, and empty String[] is returned.
     *
     * @return The array of used field names.
     */
    String[] getUsedFieldNames() {
        if (fieldData == null) {
            return(new String[0]);
        } else {
            String[] usedFieldNames = new String[fieldData.size()];
            fieldData.keySet().toArray(usedFieldNames);
            return(usedFieldNames);
        }
    }

    /**
     * Add field data
     *
     * @param fieldName The field identifer
     * @param data The field data
     */
    void addFieldData(String fieldName, Object data) {
        if ((data != null) && (fieldInfo.containsKey(fieldName))) {
            if (fieldData == null) {
                fieldData = new HashMap<String, Object>();
            }
            if (fieldInfo.get(fieldName) == FieldConstants.MFNODE) {
                ArrayList <XNode>nodeList =
                    (ArrayList<XNode>)fieldData.get(fieldName);
                if (nodeList == null) {
                    nodeList = new ArrayList <XNode>();
                    fieldData.put(fieldName, nodeList);
                }
                nodeList.add((XNode)data);
            } else {
                fieldData.put(fieldName, data);
            }
        }
    }

    /**
     * Return field data. If none is available, or the field is unknown,
     * return null.
     * <p>
     * The specific types of field data returned are:
     * <ul>
     * <li>Primitive type field values are returned in their respective
     * Java wrapper.</li>
     * <li>Array type field values are returned in their respective typed
     * arrays.</li>
     * <li>SFNode type field values are returned as XNodes.</li>
     * <li>MFNode type field values are returned in an ArrayList<XNode>.</li>
     * </ul>
     *
     * @param fieldName The field identifer
     * @return The field data
     */
    public Object getFieldData(String fieldName) {
        Object data = null;
        if (fieldData != null) {
            data = fieldData.get(fieldName);
        }
        return(data);
    }

    /**
     * Create a shallow copy, which includes only the nodeName
     * and fieldInfo, but no fieldData.
     *
     * @return a shallow copy of this
     */
    XNode copy() {
        return(new XNode(this.nodeName, this.fieldInfo));
    }

    /**
     * Return a String representation of this. Returns the
     * node name only.
     *
     * @return A String representation of this
     */
    public String toString() {
        return(nodeName);
    }

    ////////////////////////////////////////////////////////////////////////////
    // debugging support

    /**
     * Pretty print the XNode representation
     *
     * @param node The XNode to print
     */
    public static void print(XNode node) {
        System.out.println(indent + node +" {");
        String[] fieldNames = node.getUsedFieldNames();
        changeIndent(1);
        for (int i = 0; i < fieldNames.length; i++) {
            Object data = node.getFieldData(fieldNames[i]);
            if (data instanceof XNode) {
                System.out.print(indent + fieldNames[i]);
                print((XNode)data);
            } else if (data instanceof ArrayList) {
                System.out.println(indent + fieldNames[i] +" [");
                changeIndent(1);
                ArrayList<XNode> list = (ArrayList<XNode>)data;
                for (int j = 0; j < list.size(); j++) {
                    print(list.get(j));
                }
                changeIndent(-1);
                System.out.println(indent +"]");
            } else if ((data instanceof Number) || (data instanceof Boolean) ||
                (data instanceof String)) {
                System.out.println(indent + fieldNames[i] +" "+ data);
            } else if (data instanceof float[]) {
                System.out.println(indent + fieldNames[i] +" "+
                    java.util.Arrays.toString((float[])data));
            } else if (data instanceof int[]) {
                System.out.println(indent + fieldNames[i] +" "+
                    java.util.Arrays.toString((int[])data));
            } else if (data instanceof double[]) {
                System.out.println(indent + fieldNames[i] +" "+
                    java.util.Arrays.toString((double[])data));
            } else if (data instanceof String[]) {
                System.out.println(indent + fieldNames[i] +" "+
                    java.util.Arrays.toString((String[])data));
            }
        }
        changeIndent(-1);
        System.out.println(indent +"}");
    }

    /**
     * Modify the indentation level
     *
     * @param dir The direction. +1 to add a level, -1 to drop a level.
     */
    private static void changeIndent(int dir) {
        if (dir == 1) {
            indent = indent +" ";
        } else if (dir == -1) {
            indent = indent.substring(0, (indent.length() - 1));
        }
    }
    ////////////////////////////////////////////////////////////////////////////
}
