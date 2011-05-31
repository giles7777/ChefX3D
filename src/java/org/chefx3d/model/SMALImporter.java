/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package org.chefx3d.model;

// External Imports
import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;

import com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl;

// Internal Imports
//import org.chefx3d.catalog.CatalogManager;
//import org.chefx3d.tool.*;
import org.chefx3d.model.ClearModelCommand;
import org.chefx3d.util.*;
import org.chefx3d.model.LoadToolException;

/**
 * Import a world model from the SMAL format.
 *
 * TODO: Right now this is not exactly SMAL. Includes tool params.
 *
 * @author Alan Hudson
 * @version $Revision: 1.43 $
 */
public class SMALImporter implements Importer {

    //private EntityBuilder entityBuilder;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * A new SMAL Importer
     *
     */
    public SMALImporter() {
        //entityBuilder = EntityBuilder.getEntityBuilder();
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Import the file into the provided model.
     *
     * @param wmodel The world model
     * @param rdr The file reader
     */
    public void importModel(WorldModel wmodel, FileReader rdr) throws LoadToolException {
        BaseWorldModel model = (BaseWorldModel) wmodel;

        DocumentBuilderFactory builderFactory;
        DocumentBuilder builder;
        Node n;

        try {
            builderFactory = DocumentBuilderFactory.newInstance();
            builder = builderFactory.newDocumentBuilder();
        } catch (Exception e) {
            errorReporter.errorReport("SMAL Import Error!", e);
            return;
        }

        Document document = null;

        try {
            document = builder.parse(new InputSource(rdr));
        } catch (Exception e) {
            errorReporter.errorReport("Can't parse saved file", e);
            return;
        }

        //System.out.println("Printing Document");
        //org.chefx3d.util.DOMUtils.print(document);
        //System.out.println("\n*****************************************************");

        // Clear the current model
        ClearModelCommand clearCmd = new ClearModelCommand(model);
        model.applyCommand(clearCmd);

        int entityID;
        double[] pos = new double[3];
        double[] trans = new double[3];
        float[] rot = new float[4];
        String toolName;
        //Tool tool;

        NodeList entityList = document.getElementsByTagName("SMAL");

        if (entityList.getLength() < 0) {
            errorReporter.messageReport("No SMAL tag in file");
            return;
        }

        entityList = ((Element) entityList.item(0)).getChildNodes();

        int len = entityList.getLength();
        int entityCount = 0;

        for (int i = 0; i < len; i++) {
            n = entityList.item(i);
            if (n instanceof Element) {
                entityCount++;
            }
        }
        //errorReporter.messageReport("Got entities: " + entityCount);

        NodeList list;
        NodeList toolParamsList;
        NodeList transformList;
        NodeList segmentList;
        NodeList associationList;
        Element entity;
        Element toolParams;
        Element transform;
        Entity[] toolValues = new DefaultEntity[entityCount];

        entityCount = 0;

        //CatalogManager catalogManager = CatalogManager.getCatalogManager();

        for (int i = 0; i < len; i++) {
            n = entityList.item(i);

            if (!(n instanceof Element))
                continue;

            entity = (Element) n;

//System.out.println("***Loading entity: " + entity);

            transformList = entity.getElementsByTagName("Transform");

            if (transformList.getLength() <= 0) {
                errorReporter.messageReport("No Transform found for entity, skipping");
                continue;
            }

            transform = (Element) transformList.item(0);

            String posString = transform.getAttribute("translation");
            if (posString == null) {
                errorReporter.messageReport("Invalid translation");
                continue;
            }
            parseDoublesString(posString, trans);

            String rotString = transform.getAttribute("rotation");
            if (rotString == null) {
                errorReporter.messageReport("Invalid rotation");
                continue;
            }
            parseFloatsString(rotString, rot);

            toolParamsList = entity.getElementsByTagName("ToolParams");

            if (toolParamsList.getLength() <= 0) {
                errorReporter.messageReport("No ToolParams found for entity, skipping");
                continue;
            }

            toolParams = (Element) toolParamsList.item(0);

            toolName = toolParams.getAttribute("name");
            //tool = catalogManager.findTool(toolName);

            //if (tool == null) {
            //    System.out.println("Cannot find tool: " + toolName);
            //    throw new LoadToolException("Cannot find tool: " + toolName);
            //}

            entityID = Integer.parseInt(toolParams.getAttribute("entityID"));
            //errorReporter.messageReport("Find tool: " + toolName + " tool: " + tool);

            //errorReporter.messageReport("translation: " + Arrays.toString(trans));
            //errorReporter.messageReport("rotation: " + Arrays.toString(rot));

            if (toolParamsList.getLength() <= 0) {
                errorReporter.messageReport("No ToolParams found for entity, skipping");
                continue;
            }

            toolParams = (Element) toolParamsList.item(0);

            Document doc = builder.getDOMImplementation().createDocument("",
                    "ChefX3D", null);
            NodeList children = entity.getChildNodes();
            Element root = doc.getDocumentElement();

            // Remove property sheets which aren't the first
            Element entityParams = DOMUtils.getSingleElement("EntityParams", entity,
                true, null, errorReporter);

            Element[] other_sheets = null;
            DOMImplementation dom_impl = null;

            if (entityParams != null) {
                NodeList sheets = entityParams.getElementsByTagName("Sheet");
                int slen = sheets.getLength();

/*
System.out.println("***Available sheets");
for(int j=0; j < slen; j++) {
    String sname = ((Element)sheets.item(j)).getAttribute("name");
    System.out.println(sname);
}
*/

                if (slen > 1) {
                    int os_idx = 0;
                    other_sheets = new Element[slen - 1];

                    // Remove other sheets
//                    System.out.println("*** Sheets: " + sheets.getLength() + " slen: " + slen);
                    for(int j=1; j < slen; j++) {
                        Node node = sheets.item(1);
                        String name = ((Element)node).getAttribute("name");

//                        System.out.println("node: " + node.hashCode() + " name: " + name);
                        other_sheets[os_idx++] = (Element) node.getParentNode().removeChild(node);
//                        System.out.println("Saved sheet: " + other_sheets[os_idx-1]);
//                        System.out.println("Sheet name: " + ((Element)other_sheets[os_idx-1]).getAttribute("name"));
//System.out.println("Get item: " + j + " slen: " + slen + " sheets len: " + sheets.getLength());
                    }

                    try {
                        dom_impl = new DOMImplementationImpl();
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }

                }
            }

            for (int j = 0; j < children.getLength(); j++) {
                n = doc.importNode(children.item(j), true);
                root.appendChild(n);
            }
            // TODO: Not sure why this has to be done after the import.
            // Shouldn't.
            addDataEditors(doc, "SMAL");

            // DOMUtils.print(doc);

//System.out.println("***Creating SMAL dom");
            // TODO: How to get the right propertySheet name for this?
            HashMap<String, Document> props = new HashMap<String, Document>();
            props.put("SMAL", doc);

            if (other_sheets != null) {
//System.out.println("***Other sheets: name: " + toolName + " sheets: " + other_sheets.length);
                for(int k=0; k < other_sheets.length; k++) {
                    // Need to wrap the sheets with ChefX3D/EntityParams
                    Document ps_doc = dom_impl.createDocument(null, "ChefX3D", null);
                    Node ps_root = ps_doc.getDocumentElement();
                    Element e = ps_doc.createElement("EntityParams");
                    ps_root.appendChild(e);
//System.out.println("Other sheet: " + other_sheets[k]);
                    Node node = ps_doc.importNode(other_sheets[k], true);

                    e.appendChild(node);

                    String sheet_name = other_sheets[k].getAttribute("name");

                    //System.out.println("Adding sheet: " + sheet_name);
                    addDataEditors(ps_doc, sheet_name);

                    props.put(sheet_name, ps_doc);
                }
            }
            
//            EntityBuilder entityBuilder = EntityBuilder.getEntityBuilder();
//            Entity newEntity = entityBuilder.createEntity(model, entityID, trans, rot, tool);
//            
//            AddEntityCommand cmd = new AddEntityCommand(model, newEntity);
//            model.applyCommand(cmd);
//
//            toolValues[entityCount++] = newEntity;
//
//            // Handle Segments
//            if (newEntity instanceof SegmentableEntity) {
//                
//                segmentList = toolParams.getElementsByTagName("Segment");
//
//                if (segmentList.getLength() > 0) {
//
//                    int startVertexID;
//                    int endVertexID;
//                    Element segment;
//                    for (int j = 0; j < segmentList.getLength(); j++) {
//                        segment = (Element) segmentList.item(j);
//                        startVertexID = Integer.parseInt(segment.getAttribute("startId"));
//
//                        if (j == 0) {
//                            posString = segment.getAttribute("start");
//                            parseDoublesString(posString, pos);
//                            ((SegmentableEntity)newEntity).addVertex(startVertexID, pos, j, null);
//                        }
//                        endVertexID = Integer.parseInt(segment.getAttribute("endId"));
//                        posString = segment.getAttribute("end");
//
//                        parseDoublesString(posString, pos);
//
//                        ((SegmentableEntity)newEntity).addVertex(endVertexID, pos, j, null);
//                        ((SegmentableEntity)newEntity).addSegment(j, startVertexID, endVertexID, false, null);
//                    }
//                }
//                
//            }

            // Handle associations
            /*
            if (newEntity instanceof AssociatableEntity) {
                     
                associationList = entity.getElementsByTagName("Associate");
    
                //errorReporter.messageReport("association list: " + associationList.getLength());
                if (associationList.getLength() > 0) {
                    Element association;
                    int childID;
    
                    for (int j = 0; j < associationList.getLength(); j++) {
                        association = (Element) associationList.item(j);
                        list = association.getElementsByTagName("ToolParams");
    
                        if (list.getLength() <= 0) {
                            continue;
                        }
                        toolParams = (Element) list.item(0);
    
                        childID = Integer.parseInt(toolParams
                                .getAttribute("entityID"));
                        ((AssociatableEntity)newEntity).addAssociation(childID);
                    }
                }
            }
            */
            //org.chefx3d.util.DOMUtils.print(td.getProperties("SMAL"));
            //System.out.println("");
        }

        model.setModelData(toolValues);
    }

    /**
     * A hook for user code to add data editors. Should be unneeded when schema
     * processing is added for property editors.
     *
     * @param dom The dom
     * @param sheet The sheet name the properties are from
     */
    public void addDataEditors(Document dom, String sheet) {
        // no-op
    }

    /**
     * Parses a String of float data seperated by spaces. Put the results in the
     * preallocated value array.
     *
     * @param f The float string
     * @param val The preallocated array
     */
    private void parseFloatsString(String f, float[] val) {
        if (f == null)
            return;

        StringTokenizer st = new StringTokenizer(f);

        String fstr;

        int i = 0;

        while (st.hasMoreTokens()) {
            fstr = st.nextToken();
            val[i++] = Float.parseFloat(fstr);
        }
    }

    /**
     * Parses a String of double data seperated by spaces. Put the results in
     * the preallocated value array.
     *
     * @param f The float string
     * @param val The preallocated array
     */
    private void parseDoublesString(String f, double[] val) {
        if (f == null)
            return;

        StringTokenizer st = new StringTokenizer(f);

        String fstr;

        int i = 0;

        while (st.hasMoreTokens()) {
            fstr = st.nextToken();
            val[i++] = Double.parseDouble(fstr);
        }
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
}