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

import org.j3d.util.I18nManager;

import org.web3d.parser.DefaultFieldParserFactory;

import org.web3d.util.SimpleStack;

import org.web3d.vrml.lang.FieldConstants;
import org.web3d.vrml.lang.VRMLException;

import org.web3d.vrml.parser.VRMLFieldReader;

import org.web3d.vrml.sav.BinaryContentHandler;
import org.web3d.vrml.sav.Locator;
import org.web3d.vrml.sav.ProtoHandler;
import org.web3d.vrml.sav.SAVException;
import org.web3d.vrml.sav.ScriptHandler;
import org.web3d.vrml.sav.StringContentHandler;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Content Handler implementation for decoding X3D files into
 * an intermediate representation for later conversion into
 * -something else-.
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
class X3DContentHandler implements StringContentHandler,
    BinaryContentHandler {

    /** Error message when the content handler encounters an
     *  unknown node in an X3D file */
    private static final String UNSUPPORTED_NODE_MSG =
        "org.chefx3d.view.awt.av3d.X3DContentHandler.unsupportedNodeMsg";

    /** A stack of xnodes */
    private SimpleStack nodeStack;

    /** A stack of field names */
    private SimpleStack fieldStack;

    /** Map of def'ed nodes keyed by DEF Name */
    private HashMap<String, XNode> defMap;

    /** Scene instance */
    private XNode scene;

    /** Node wrapper factory */
    private XNodeFactory factory;

    /** Internal parser of field values */
    private VRMLFieldReader fieldReader;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** I18N manager for sourcing messages */
    private I18nManager i18n_mgr;

    /**
     * Constructor
     */
    X3DContentHandler() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
     */
    X3DContentHandler(ErrorReporter reporter) {

        i18n_mgr = I18nManager.getManager();
        setErrorReporter(reporter);

        nodeStack = new SimpleStack();
        fieldStack = new SimpleStack();
        defMap = new HashMap<String, XNode>();
    }

    //----------------------------------------------------------
    // Methods defined by ContentHandler
    //----------------------------------------------------------

    /**
     * Declaration of the start of the document. The parameters are all of the
     * values that are declared on the header line of the file after the
     * <CODE>#</CODE> start. The type string contains the representation of
     * the first few characters of the file after the #. This allows us to
     * work out if it is VRML97 or the later X3D spec.
     * <p>
     * Version numbers change from VRML97 to X3D and aren't logical. In the
     * first, it is <code>#VRML V2.0</code> and the second is
     * <code>#X3D V1.0</code> even though this second header represents a
     * later spec.
     *
     * @param uri The URI of the file.
     * @param url The base URL of the file for resolving relative URIs
     *    contained in the file
     * @param encoding The encoding of this document - utf8 or binary
     * @param type The bytes of the first part of the file header
     * @param version The VRML version of this document
     * @param comment Any trailing text on this line. If there is none, this
     *    is null.
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void startDocument(String uri,
        String url,
        String encoding,
        String type,
        String version,
        String comment)
        throws SAVException, VRMLException {

        int majorVersion = 3;
        int minorVersion = 2;

        // this is a crude way to get revision numbers.....
        int separator_index = version.indexOf(".");
        try {
            majorVersion = Integer.parseInt(version.substring(separator_index-1, separator_index));
            minorVersion = Integer.parseInt(version.substring(separator_index+1, separator_index+2));
        } catch (NumberFormatException nfe) {
            // this shouldn't happen - since the parser should have caught this first.
            majorVersion = 3;
            minorVersion = 2;
        }

        DefaultFieldParserFactory fac = new DefaultFieldParserFactory();
        fieldReader = fac.newFieldParser(majorVersion, minorVersion);

        factory = XNodeFactory.getInstance();

        // fake a node to serve as the Scene container
        HashMap <String, Integer>fieldInfo = new HashMap<String, Integer>(1);
        fieldInfo.put("children", new Integer(FieldConstants.MFNODE));
        scene = new XNode("Scene", fieldInfo);

        nodeStack.push(scene);
        fieldStack.push("children");
    }

    /**
     * Declaration of the end of the document. There will be no further parsing
     * and hence events after this.
     *
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void endDocument() throws SAVException, VRMLException {

        // should verify that the nodeStack and fieldStack
        // are back to their 'starting' point
        XNode node = (XNode)nodeStack.pop();
        String fieldName = (String)fieldStack.pop();

        //System.out.println("endDocument: "+ node +": "+ fieldName);
    }

    /**
     * Notification of the start of a node. This is the opening statement of a
     * node and it's DEF name. USE declarations are handled in a separate
     * method.
     *
     * @param name The name of the node that we are about to parse
     * @param defName The string associated with the DEF name. Null if not
     *   given for this node.
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void startNode(String name, String defName)
        throws SAVException, VRMLException {

        XNode node = factory.get(name);

        if (node == null) {
            String msg = i18n_mgr.getString(UNSUPPORTED_NODE_MSG) +": "+ name;
            throw new VRMLException(msg);
        }

        XNode parent = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.peek();
        parent.addFieldData(fieldName, node);

        if (defName != null) {
            defMap.put(defName, node);
        }

        nodeStack.push(node);
    }

    /**
     * Notification of the end of a node declaration.
     *
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void endNode() throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.pop();

        String nodeName = node.getNodeName();
        String parentFieldName = (String)fieldStack.peek();
        boolean isChildrenField = parentFieldName.equals("children");
        if (!isChildrenField) {
            fieldStack.pop();
        } else if (nodeName.equals("Transform") || nodeName.equals("Group")) {
            fieldStack.pop();
        }
    }

    /**
     * Notification of a field declaration. This notification is only called
     * if it is a standard node. If the node is a script or PROTO declaration
     * then the {@link ScriptHandler} or {@link ProtoHandler} methods are
     * used.
     *
     * @param name The name of the field declared
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void startField(String name) throws SAVException, VRMLException {

        fieldStack.push(name);
    }

    /**
     * The field value is a USE for the given node name. This is a
     * terminating call for startField as well. The next call will either be
     * another <CODE>startField()</CODE> or <CODE>endNode()</CODE>.
     *
     * @param defName The name of the DEF string to use
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void useDecl(String defName) throws SAVException, VRMLException {

        String fieldName = (String)fieldStack.pop();

        XNode use = defMap.get(defName);
        XNode parent = (XNode)nodeStack.peek();
        if ((parent != null) && (use != null)) {
            parent.addFieldData(fieldName, use);
        }
    }

    /**
     * Notification of the end of a field declaration. This is called only at
     * the end of an MFNode declaration. All other fields are terminated by
     * either {@link #useDecl(String)} or {@link #fieldValue(String)}.
     *
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void endField() throws SAVException, VRMLException {

        //String fieldName = (String)fieldStack.pop();
    }

    /**
     * A profile declaration has been found in the code. IAW the X3D
     * specification, this method will only ever be called once in the lifetime
     * of the parser for this document. The name is the name of the profile
     * for the document to use.
     *
     * @param profileName The name of the profile to use
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void profileDecl(String profileName) throws SAVException,
            VRMLException {
    }

    /**
     * A component declaration has been found in the code. There may be zero
     * or more component declarations in the file, appearing just after the
     * profile declaration. The textual information after the COMPONENT keyword
     * is left unparsed and presented through this call. It is up to the user
     * application to parse the component information.
     *
     * @param componentInfo The name of the component to use
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void componentDecl(String componentInfo) throws SAVException,
            VRMLException {
    }

    /**
     * Set the document locator that can be used by the implementing code to
     * find out information about the current line information. This method
     * is called by the parser to your code to give you a locator to work with.
     * If this has not been set by the time <CODE>startDocument()</CODE> has
     * been called, you can assume that you will not have one available.
     *
     * @param loc The locator instance to use
     */
    public void setDocumentLocator(Locator loc) {
    }

    /**
     * A META declaration has been found in the code. There may be zero
     * or more meta declarations in the file, appearing just after the
     * component declaration. Each meta declaration has a key and value
     * strings. No information is to be implied from this. It is for extra
     * data only.
     *
     * @param key The value of the key string
     * @param value The value of the value string
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void metaDecl(String key, String value) throws SAVException,
            VRMLException {
    }

    /**
     * An IMPORT declaration has been found in the document. All three
     * parameters will always be provided, regardless of whether the AS keyword
     * has been used or not. The parser implementation will automatically set
     * the local import name as needed.
     *
     * @param inline The name of the inline DEF nodes
     * @param exported The exported name from the inlined file
     * @param imported The local name to use for the exported name
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void importDecl(String inline, String exported, String imported)
            throws SAVException, VRMLException {
    }

    /**
     * An EXPORT declaration has been found in the document. Both paramters
     * will always be provided regardless of whether the AS keyword has been
     * used. The parser implementation will automatically set the exported
     * name as needed.
     *
     * @param defName The DEF name of the nodes to be exported
     * @param exported The name to be exported as
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void exportDecl(String defName, String exported)
            throws SAVException, VRMLException {
    }

    //---------------------------------------------------------------
    // Methods defined by StringContentHandler
    //---------------------------------------------------------------

    /**
     * The value of a normal field. This is a string that represents the entire
     * value of the field. MFStrings will have to be parsed. This is a
     * terminating call for startField as well. The next call will either be
     * another <CODE>startField()</CODE> or <CODE>endNode()</CODE>.
     * <p>
     * If this field is an SFNode with a USE declaration you will have the
     * {@link #useDecl(String)} method called rather than this method.
     *
     * @param value The value of this field
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void fieldValue(String value) throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if ((node != null) && (value != null)) {
            int fieldType = node.getFieldType(fieldName);
            if (fieldType != -1) {
                Object data = getData(fieldType, value);
                if (data != null) {
                    node.addFieldData(fieldName, data);
                }
            }
        }
    }

    /**
     * The value of an MFField where the underlying parser knows about how the
     * values are broken up. The parser is not required to support this
     * callback, but implementors of this interface should understand it. The
     * most likely time we will have this method called is for MFString or
     * URL lists. If called, it is guaranteed to split the strings along the
     * SF node type boundaries.
     *
     * @param values The list of string representing the values
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document
     * @throws VRMLException The content provided is invalid for this
     *   part of the document or can't be parsed
     */
    public void fieldValue(String[] values) throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if ((node != null) && (values != null) && (values.length != 0)) {
            int fieldType = node.getFieldType(fieldName);
            if (fieldType != -1) {
                Object data = getData(fieldType, values);
                if (data != null) {
                    node.addFieldData(fieldName, data);
                }
            }
        }
    }

    //---------------------------------------------------------------
    // Methods defined by BinaryContentHandler
    //---------------------------------------------------------------

    /**
     * Set the value of the field at the given index as an integer. This would
     * be used to set SFInt32 field types.
     *
     * @param value The new value to use for the node
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(int value)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            node.addFieldData(fieldName, new Integer(value));
        }
    }

    /**
     * Set the value of the field at the given index as an array of integers.
     * This would be used to set MFInt32 field types.
     *
     * @param value The new value to use for the node
     * @param len The number of valid entries in the value array
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(int[] value, int len)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            if (len == value.length) {
                node.addFieldData(fieldName, value);
            } else {
                int[] new_value = new int[len];
                System.arraycopy(value, 0, new_value, 0, len);
                node.addFieldData(fieldName, new_value);
            }
        }
    }

    /**
     * Set the value of the field at the given index as an boolean. This would
     * be used to set SFBool field types.
     *
     * @param value The new value to use for the node
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(boolean value)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            node.addFieldData(fieldName, Boolean.valueOf(value));
        }
    }

    /**
     * Set the value of the field at the given index as an array of boolean.
     * This would be used to set MFBool field types.
     *
     * @param value The new value to use for the node
     * @param len The number of valid entries in the value array
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(boolean[] value, int len)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            if (len == value.length) {
                node.addFieldData(fieldName, value);
            } else {
                boolean[] new_value = new boolean[len];
                System.arraycopy(value, 0, new_value, 0, len);
                node.addFieldData(fieldName, new_value);
            }
        }
    }

    /**
     * Set the value of the field at the given index as a float. This would
     * be used to set SFFloat field types.
     *
     * @param value The new value to use for the node
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(float value)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            node.addFieldData(fieldName, new Float(value));
        }
    }

    /**
     * Set the value of the field at the given index as an array of floats.
     * This would be used to set MFFloat, SFVec2f, SFVec3f and SFRotation
     * field types.
     *
     * @param value The new value to use for the node
     * @param len The number of valid entries in the value array
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(float[] value, int len)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            if (len == value.length) {
                node.addFieldData(fieldName, value);
            } else {
                float[] new_value = new float[len];
                System.arraycopy(value, 0, new_value, 0, len);
                node.addFieldData(fieldName, new_value);
            }
        }
    }

    /**
     * Set the value of the field at the given index as an long. This would
     * be used to set SFTime field types.
     *
     * @param value The new value to use for the node
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(long value)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            node.addFieldData(fieldName, new Long(value));
        }
    }

    /**
     * Set the value of the field at the given index as an array of longs.
     * This would be used to set MFTime field types.
     *
     * @param value The new value to use for the node
     * @param len The number of valid entries in the value array
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(long[] value, int len)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            if (len == value.length) {
                node.addFieldData(fieldName, value);
            } else {
                long[] new_value = new long[len];
                System.arraycopy(value, 0, new_value, 0, len);
                node.addFieldData(fieldName, new_value);
            }
        }
    }

    /**
     * Set the value of the field at the given index as an double. This would
     * be used to set SFDouble field types.
     *
     * @param value The new value to use for the node
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(double value)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            node.addFieldData(fieldName, new Double(value));
        }
    }

    /**
     * Set the value of the field at the given index as an array of doubles.
     * This would be used to set MFDouble, SFVec2d and SFVec3d field types.
     *
     * @param value The new value to use for the node
     * @param len The number of valid entries in the value array
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(double[] value, int len)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            if (len == value.length) {
                node.addFieldData(fieldName, value);
            } else {
                double[] new_value = new double[len];
                System.arraycopy(value, 0, new_value, 0, len);
                node.addFieldData(fieldName, new_value);
            }
        }
    }

    /**
     * Set the value of the field at the given index as an array of strings.
     * This would be used to set MFString field types.
     *
     * @param value The new value to use for the node
     * @param len The number of valid entries in the value array
     * @throws SAVException This call is taken at the wrong time in the
     *   structure of the document.
     * @throws VRMLException This call is taken at the wrong time in the
     *   structure of the document.
     */
    public void fieldValue(String[] value, int len)
        throws SAVException, VRMLException {

        XNode node = (XNode)nodeStack.peek();
        String fieldName = (String)fieldStack.pop();
        if (node != null) {
            if (len == value.length) {
                node.addFieldData(fieldName, value);
            } else {
                String[] new_value = new String[len];
                System.arraycopy(value, 0, new_value, 0, len);
                node.addFieldData(fieldName, new_value);
            }
        }
    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Return the node object containing the parsed scene
     *
     * @return The node object containing the parsed scene
     */
    XNode getScene() {
        return(scene);
    }

    /**
     * Register an error reporter
     *
     * @param reporter The new ErrorReporter to use.
     */
    void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Return the data values contained in the argument String for the
     * specified fieldType
     *
     * @param The field type
     * @param The String representation of the data
     */
    private Object getData(int fieldType, String value) {

        switch(fieldType) {
            case FieldConstants.SFINT32:
                int i = fieldReader.SFInt32(value);
                return(new Integer(i));

            case FieldConstants.MFINT32:
                int[] i_array = fieldReader.MFInt32(value);
                return(i_array);

            case FieldConstants.SFFLOAT:
                float f = fieldReader.SFFloat(value);
                return(new Float(f));

            case FieldConstants.SFTIME:
                double d = fieldReader.SFTime(value);
                return(new Double(d));

            case FieldConstants.SFDOUBLE:
                d = fieldReader.SFDouble(value);
                return(new Double(d));

            case FieldConstants.MFTIME:
                double[] d_array = fieldReader.MFTime(value);
                return(d_array);

            case FieldConstants.MFDOUBLE:
                d_array = fieldReader.MFDouble(value);
                return(d_array);

            case FieldConstants.SFLONG:
                long l = fieldReader.SFLong(value);
                return(new Long(l));

            case FieldConstants.MFLONG:
                long[] l_array = fieldReader.MFLong(value);
                return(l_array);

            case FieldConstants.SFBOOL:
                boolean b = fieldReader.SFBool(value);
                return(Boolean.valueOf(b));

            case FieldConstants.SFROTATION:
                float[] f_array = fieldReader.SFRotation(value);
                return(f_array);

            case FieldConstants.MFROTATION:
                f_array = fieldReader.MFRotation(value);
                return(f_array);

            case FieldConstants.MFBOOL:
                boolean[] b_array = fieldReader.MFBool(value);
                return(b_array);

            case FieldConstants.MFFLOAT:
                f_array = fieldReader.MFFloat(value);
                return(f_array);

            case FieldConstants.SFVEC2F:
                f_array = fieldReader.SFVec2f(value);
                return(f_array);

            case FieldConstants.SFVEC3F:
                f_array = fieldReader.SFVec3f(value);
                return(f_array);

            case FieldConstants.SFVEC4F:
                f_array = fieldReader.SFVec4f(value);
                return(f_array);

            case FieldConstants.MFVEC2F:
                f_array = fieldReader.MFVec2f(value);
                return(f_array);

            case FieldConstants.MFVEC3F:
                f_array = fieldReader.MFVec3f(value);
                return(f_array);

            case FieldConstants.MFVEC4F:
                f_array = fieldReader.MFVec4f(value);
                return(f_array);

            case FieldConstants.SFVEC3D:
                d_array = fieldReader.SFVec3d(value);
                return(d_array);

            case FieldConstants.SFVEC4D:
                d_array = fieldReader.SFVec4d(value);
                return(d_array);

            case FieldConstants.MFVEC3D:
                d_array = fieldReader.MFVec3d(value);
                return(d_array);

            case FieldConstants.MFVEC4D:
                d_array = fieldReader.MFVec4d(value);
                return(d_array);

            case FieldConstants.SFSTRING:
                return(value);

            case FieldConstants.MFSTRING:
                String[] s_array = fieldReader.MFString(value);
                return(s_array);

            case FieldConstants.SFCOLOR:
                f_array = fieldReader.SFColor(value);
                return(f_array);

            case FieldConstants.MFCOLOR:
                f_array = fieldReader.MFColor(value);
                return(f_array);

            case FieldConstants.SFCOLORRGBA:
                f_array = fieldReader.SFColorRGBA(value);
                return(f_array);

            case FieldConstants.MFCOLORRGBA:
                f_array = fieldReader.MFColorRGBA(value);
                return(f_array);

            case FieldConstants.SFMATRIX3F:
                f_array = fieldReader.SFMatrix3f(value);
                return(f_array);

            case FieldConstants.SFMATRIX4F:
                f_array = fieldReader.SFMatrix4f(value);
                return(f_array);

            case FieldConstants.MFMATRIX3F:
                f_array = fieldReader.MFMatrix3f(value);
                return(f_array);

            case FieldConstants.MFMATRIX4F:
                f_array = fieldReader.MFMatrix4f(value);
                return(f_array);

            case FieldConstants.SFMATRIX3D:
                d_array = fieldReader.SFMatrix3d(value);
                return(d_array);

            case FieldConstants.SFMATRIX4D:
                d_array = fieldReader.SFMatrix4d(value);
                return(d_array);

            case FieldConstants.MFMATRIX3D:
                d_array = fieldReader.MFMatrix3d(value);
                return(d_array);

            case FieldConstants.MFMATRIX4D:
                d_array = fieldReader.MFMatrix4d(value);
                return(d_array);

            // these cases are not primitive types
            case FieldConstants.SFIMAGE:
            case FieldConstants.MFIMAGE:
            case FieldConstants.SFNODE:
            case FieldConstants.MFNODE:
                //throw new IllegalArgumentException(
                //  "fieldType: "+ fieldType +" cannot contain an array");
                return(null);

            default:
                //throw new IllegalArgumentException(
                //    "FieldValueHandler: Unknown fieldType: "+ fieldType);
                return(null);
        }
    }

    /**
     * Return the data values contained in the argument String[] for the
     * specified fieldType
     *
     * @param The field type
     * @param The String[] representation of the data
     */
    private Object getData(int fieldType, String[] value) {

        switch(fieldType) {

            case FieldConstants.MFINT32:
                int[] i_array = fieldReader.MFInt32(value);
                return(i_array);

            case FieldConstants.MFTIME:
                double[] d_array = fieldReader.MFTime(value);
                return(d_array);

            case FieldConstants.MFDOUBLE:
                d_array = fieldReader.MFDouble(value);
                return(d_array);

            case FieldConstants.MFLONG:
                long[] l_array = fieldReader.MFLong(value);
                return(l_array);

            case FieldConstants.SFROTATION:
                float[] f_array = fieldReader.SFRotation(value);
                return(f_array);

            case FieldConstants.MFROTATION:
                f_array = fieldReader.MFRotation(value);
                return(f_array);

            case FieldConstants.MFBOOL:
                boolean[] b_array = fieldReader.MFBool(value);
                return(b_array);

            case FieldConstants.MFFLOAT:
                f_array = fieldReader.MFFloat(value);
                return(f_array);

            case FieldConstants.SFVEC2F:
                f_array = fieldReader.SFVec2f(value);
                return(f_array);

            case FieldConstants.SFVEC3F:
                f_array = fieldReader.SFVec3f(value);
                return(f_array);

            case FieldConstants.SFVEC4F:
                f_array = fieldReader.SFVec4f(value);
                return(f_array);

            case FieldConstants.MFVEC2F:
                f_array = fieldReader.MFVec2f(value);
                return(f_array);

            case FieldConstants.MFVEC3F:
                f_array = fieldReader.MFVec3f(value);
                return(f_array);

            case FieldConstants.MFVEC4F:
                f_array = fieldReader.MFVec4f(value);
                return(f_array);

            case FieldConstants.SFVEC3D:
                d_array = fieldReader.SFVec3d(value);
                return(d_array);

            case FieldConstants.SFVEC4D:
                d_array = fieldReader.SFVec4d(value);
                return(d_array);

            case FieldConstants.MFVEC3D:
                d_array = fieldReader.MFVec3d(value);
                return(d_array);

            case FieldConstants.MFVEC4D:
                d_array = fieldReader.MFVec4d(value);
                return(d_array);

            case FieldConstants.MFSTRING:
                return(value);

            case FieldConstants.SFCOLOR:
                f_array = fieldReader.SFColor(value);
                return(f_array);

            case FieldConstants.MFCOLOR:
                f_array = fieldReader.MFColor(value);
                return(f_array);

            case FieldConstants.SFCOLORRGBA:
                f_array = fieldReader.SFColorRGBA(value);
                return(f_array);

            case FieldConstants.MFCOLORRGBA:
                f_array = fieldReader.MFColorRGBA(value);
                return(f_array);

            case FieldConstants.SFMATRIX3F:
                f_array = fieldReader.SFMatrix3f(value);
                return(f_array);

            case FieldConstants.SFMATRIX4F:
                f_array = fieldReader.SFMatrix4f(value);
                return(f_array);

            case FieldConstants.MFMATRIX3F:
                f_array = fieldReader.MFMatrix3f(value);
                return(f_array);

            case FieldConstants.MFMATRIX4F:
                f_array = fieldReader.MFMatrix4f(value);
                return(f_array);

            case FieldConstants.SFMATRIX3D:
                d_array = fieldReader.SFMatrix3d(value);
                return(d_array);

            case FieldConstants.SFMATRIX4D:
                d_array = fieldReader.SFMatrix4d(value);
                return(d_array);

            case FieldConstants.MFMATRIX3D:
                d_array = fieldReader.MFMatrix3d(value);
                return(d_array);

            case FieldConstants.MFMATRIX4D:
                d_array = fieldReader.MFMatrix4d(value);
                return(d_array);

            // these cases are not primitive array types
            case FieldConstants.SFIMAGE:
            case FieldConstants.MFIMAGE:
            case FieldConstants.SFNODE:
            case FieldConstants.MFNODE:
            case FieldConstants.SFINT32:
            case FieldConstants.SFFLOAT:
            case FieldConstants.SFTIME:
            case FieldConstants.SFDOUBLE:
            case FieldConstants.SFLONG:
            case FieldConstants.SFBOOL:
            case FieldConstants.SFSTRING:
                //throw new IllegalArgumentException(
                //    "fieldType: "+ fieldType +" cannot contain an array");
                return(null);

            default:
                //throw new IllegalArgumentException(
                //    "Unknown fieldType: "+ fieldType );
                return(null);
        }
    }

}
