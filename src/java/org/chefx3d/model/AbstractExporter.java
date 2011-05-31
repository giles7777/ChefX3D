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


//External Imports
import java.io.Writer;
import org.w3c.dom.*;
import java.util.*;
import javax.vecmath.Vector3f;

// Internal Imports
import org.chefx3d.util.*;

/**
 * An Entity Exporter
 *
 * @author Alan Hudson
 * @version $Revision: 1.3 $
 */
public abstract class AbstractExporter implements Exporter {
    // Scratch vars
    private double[] pos;
    private float[] rot;
    private Vector3f tmpVec;
    /** Default direction for segments */
    private Vector3f segmentDirection;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;


    public AbstractExporter() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        pos = new double[3];
        rot = new float[4];

        tmpVec = new Vector3f();
        segmentDirection = new Vector3f(1, 0, 0);
    }

    /**
     * Output a specific entity to the specified stream.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param fw The stream to write to
     */
    public void export(WorldModel model, int entityID, Writer fw) {

        Entity[] toolValues = ((BaseWorldModel) model).getModelData();

        Entity td = toolValues[entityID];

        addParams(td);
    }

    /**
     * Output the World Model to the specified stream.
     *
     * @param model The world model to export
     * @param fw The stream to write to
     */
    public void export(WorldModel model, Writer fw) {
        Entity[] toolValues = ((BaseWorldModel) model).getModelData();

        // Go through and add all params needed first
        int len = toolValues.length;

        for (int i = 0; i < len; i++) {
            Entity td = toolValues[i];

            if (td == null) {
                // Its expected we will have gaps
                continue;
            }

            addParams(td);
        }

        // Add all associations
        for (int i = 0; i < len; i++) {
            Entity td = toolValues[i];

            if (td == null) {
                // Its expected we will have gaps
                continue;
            }

            addAssociations(toolValues, td);
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


    /**
     * Remove any XML header instructions.
     *
     * @param input The input string
     * @return The output string with headers removed
     */
    protected String removeXMLHeader(String input) {
        StringBuffer modString = new StringBuffer(input.length());
        int spos;

        spos = input.indexOf("<?xml");

        if (spos < 0)
            return input;

        int currPos = 0;
        int pos2 = 0;
        // String ps;

        while (spos > -1) {
            pos2 = input.indexOf(">", spos);
            modString.append(input.substring(currPos, spos));

            currPos = pos2 + 1;
            spos = input.indexOf("<?xml", currPos);
            /*
             * if (spos == -1) ps = input.substring(pos2+1); else ps =
             * input.substring(pos2+1,spos); System.out.println("\n: post: " +
             * ps); modString.append(ps); modString.append("\n");
             */
        }

        if (currPos > 0)
            modString.append(input.substring(pos2 + 1));
        return modString.toString();
    }

    /**
     * Add in tool params to finalize the DOM before export.
     *
     * @param props The properties to edit
     * @param td The current tool data
     */
    private void addParams(Entity entity) {
        //Tool ti = td.getTool();

        // TODO: fix this
        /*
        Document props =
            (Document) ((Map.Entry) (entity.getProperties().entrySet()
                    .iterator().next())).getValue();

        if (entity instanceof PositionableEntity) {
            ((PositionableEntity)entity).getPosition(pos);
            ((PositionableEntity)entity).getRotation(rot);
        } else {
            pos = new double[] {0, 0, 0};
            rot = new float[] {0, 0, 0, 0};
        }

        NodeList list = props.getElementsByTagName("ToolParams");
        Node top = null;
        Element tElement = null;

        if (list.getLength() <= 0) {
            // Add in tool specific params

            list = props.getElementsByTagName("ChefX3D");
            if (list.getLength() <= 0) {
                errorReporter.messageReport("No ChefX3D element in: " + entity.getName());
                return;
            }

            Element e = (Element) list.item(0);
            Element e2 = props.createElement("ToolParams");

            e2.setAttribute("name", entity.getName());

            e2.setAttribute("url", entity.getModelURL());
            e2.setAttribute("entityID", Integer.toString(entity.getEntityID()));
            e2.setAttribute("type", String.valueOf(entity.getType()));

            e.appendChild(props.createTextNode("  "));
            e.appendChild(e2);
            e.appendChild(props.createTextNode("\n"));

            top = (Node) e2;

            tElement = props.createElement("Transform");
            top.appendChild(props.createTextNode("\n"));
            top.appendChild(props.createTextNode("    "));
            top.appendChild(tElement);
            top.appendChild(props.createTextNode("\n"));
            top.appendChild(props.createTextNode("  "));
        } else {
            top = (Element) list.item(0);
            list = props.getElementsByTagName("Transform");
            tElement = (Element) list.item(0);
        }

        tElement.setAttribute("translation", Double.toString(pos[0]) + " "
                + Double.toString(pos[1]) + " " + Double.toString(pos[2]));
        tElement.setAttribute("rotation", Float.toString(rot[0]) + " "
                + Float.toString(rot[1]) + " " + Float.toString(rot[2]) + " "
                + Float.toString(rot[3]));

        if (entity.getType() == Tool.TYPE_MULTI_SEGMENT) {

            SegmentSequence segments = ((SegmentableEntity)entity).getSegmentSequence();
            ArrayList<Segment> segmentList = segments.getSegments();
            int len = segmentList.size();

            if (len != 0) {
                list = props.getElementsByTagName("Segments");
                if (list.getLength() > 0) {
                    Element sElement = (Element) list.item(0);

                    //errorReporter.messageReport("Removing Segments element: " + sElement);
                    top.removeChild(sElement);
                }

                Element segmentsElement = props.createElement("Segments");
                top.appendChild(segmentsElement);

                double[] start = null;
                double[] end = null;
                SegmentVertex startVertex = null;
                SegmentVertex endVertex = null;
                int startVertexID;
                int endVertexID;
                Segment segment;

                StringBuffer buff = new StringBuffer();

                for (Iterator<Segment> i = segmentList.iterator(); i.hasNext();) {
                    segment = i.next();

                    startVertexID = segment.getStartIndex();
                    endVertexID = segment.getEndIndex();
                    startVertex = segments.getVertex(startVertexID);
                    endVertex = segments.getVertex(endVertexID);

                    Element e2 = props.createElement("Segment");

                    e2.setAttribute("startId", Integer.toString(startVertex.getVertexID()));

                    start = startVertex.getPosition();
                    buff.setLength(0);
                    buff.append(Double.toString(start[0]));
                    buff.append(" ");
                    buff.append(Double.toString(start[1]));
                    buff.append(" ");
                    buff.append(Double.toString(start[2]));
                    e2.setAttribute("start", buff.toString());

                    e2.setAttribute("endId", Integer.toString(endVertex.getVertexID()));

                    end = endVertex.getPosition();
                    buff.setLength(0);
                    buff.append(Double.toString(end[0]));
                    buff.append(" ");
                    buff.append(Double.toString(end[1]));
                    buff.append(" ");
                    buff.append(Double.toString(end[2]));
                    e2.setAttribute("end", buff.toString());

                    buff.setLength(0);
                    buff.append(Double
                            .toString((start[0] + end[0]) / 2));
                    buff.append(" ");
                    buff.append(Double
                            .toString((start[1] + end[1]) / 2));
                    buff.append(" ");
                    buff.append(Double
                            .toString((start[2] + end[2]) / 2));

                    e2.setAttribute("center", buff.toString());

                    tmpVec.x = (float) (start[0] - end[0]);
                    tmpVec.y = (float) (start[1] - end[1]);
                    tmpVec.z = (float) (start[2] - end[2]);
                    tmpVec.normalize();

                    float angle = segmentDirection.angle(tmpVec);

                    if (end[2] < start[2]) {
                        angle = -angle;
                    }

                    buff.setLength(0);
                    buff.append(0);
                    buff.append(" ");
                    buff.append(1);
                    buff.append(" ");
                    buff.append(0);
                    buff.append(" ");
                    buff.append(Float.toString(angle));

                    e2.setAttribute("rotation", buff.toString());

                    Element e3 = props.createElement("SegmentParams");
                    Map<String, Document> segment_props = segment.getProperties();
                    Iterator itr = segment_props.entrySet().iterator();
                    Map.Entry entry;

                    while(itr.hasNext()) {
                        entry = (Map.Entry) itr.next();
                        String sheet_name = (String) entry.getKey();
                        Document sprops = (Document) entry.getValue();
                        Element sheet = DOMUtils.getSingleElement("Sheet", sprops, false, null, null);
                        Node n = props.importNode(sheet, true);
                        e2.appendChild(n);
                        e2.appendChild(props.createTextNode("\n"));
                    }

                    e2.appendChild(e3);
                    e2.appendChild(props.createTextNode("\n"));

                    Element e4 = props.createElement("StartVertexParams");
                    Map<String, Document> vertex_props = startVertex.getProperties();
                    itr = vertex_props.entrySet().iterator();

                    while(itr.hasNext()) {
                        entry = (Map.Entry) itr.next();
                        String sheet_name = (String) entry.getKey();
                        Document sprops = (Document) entry.getValue();
                        Element sheet = DOMUtils.getSingleElement("Sheet", sprops, false, null, null);
                        Node n = props.importNode(sheet, true);
                        e4.appendChild(n);
                        e4.appendChild(props.createTextNode("\n"));
                    }

                    e2.appendChild(e4);
                    e2.appendChild(props.createTextNode("\n"));

                    Element e5 = props.createElement("EndVertexParams");
                    vertex_props = endVertex.getProperties();

                    itr = vertex_props.entrySet().iterator();

                    while(itr.hasNext()) {
                        entry = (Map.Entry) itr.next();
                        String sheet_name = (String) entry.getKey();
                        Document sprops = (Document) entry.getValue();
                        Element sheet = DOMUtils.getSingleElement("Sheet", sprops, false, null, null);
                        Node n = props.importNode(sheet, true);
                        e5.appendChild(n);
                        e5.appendChild(props.createTextNode("\n"));
                    }
                    e2.appendChild(e5);

                    segmentsElement.appendChild(e2);
                    segmentsElement.appendChild(props
                            .createTextNode("\n"));
                }
            }
        }

        // TODO: Fix in a more general way with schema appInfo for uniqueID's
        // Find or create the entityID attribute

        if (entity.getType() != Tool.TYPE_WORLD) {

            list = props.getElementsByTagName("DisConfiguration");

            if (list.getLength() <= 0) {
            } else {
                tElement = (Element) list.item(0);
                Attr att = tElement.getAttributeNode("entityID");
                if (att == null) {
                    att = props.createAttribute("entityID");
                    att.setValue(Integer.toString(entity.getEntityID()));
                    tElement.setAttributeNode(att);
                }
            }
        }
        */
    }

    /**
     * Add in all associated children.  Multiple calls to this
     * should yield the same information, ie remove old values.
     */
    private void addAssociations(Entity[] toolValues, Entity entity) {

        //TODO: fix this

        /*
        Document props = (Document) ((Map.Entry) (entity.getProperties().entrySet()
                .iterator().next())).getValue();

        // Add Associated entities
        if (entity instanceof AssociatableEntity) {

            int[] children = ((AssociatableEntity)entity).getAssociates();

            NodeList list = props.getElementsByTagName("AssociatedEntities");

            if (list.getLength() > 0) {
                Element e = (Element) list.item(0);
                Node n = e.getParentNode();
                n.removeChild(e);
            }

            list = props.getElementsByTagName("ChefX3D");
            if (list.getLength() <= 0) {
                errorReporter.messageReport("No ChefX3D element in: " + entity.getName());
                return;
            }

            Element e = (Element) list.item(0);

            if (children == null) {
                return;
            }

            int len = children.length;

            if (len > 0) {
                Element e2 = props.createElement("AssociatedEntities");

                for (int i = 0; i < len; i++) {
                    Element e3 = props.createElement("Associate");
                    e2.appendChild(props.createTextNode("\n"));
                    e2.appendChild(props.createTextNode("  "));
                    e2.appendChild(e3);

                    // Copy entity data
                    Entity child = toolValues[children[i]];

                    if (child == null) {
                        errorReporter.messageReport("ERROR: Cannot find associated child: "
                                + children[i]);
                        e3.appendChild(props
                                .createComment("Cannot find associated child: "
                                        + children[i]));
                        continue;
                    }

                    // RUSS: Need to handle all sheets here
                    Document cprops = (Document) ((Map.Entry) (child
                            .getProperties().entrySet().iterator().next()))
                            .getValue();
                    Node n = props.importNode(cprops.getDocumentElement(), true);
                    e3.appendChild(n);
                    e3.appendChild(props.createTextNode("\n"));
                }

                e.appendChild(e2);
            }
        }
        */
    }


    /**
     * Convert an array of strings into a MFString representation.
     *
     * @param st The string array
     * @return A single string representation
     */
    private String convertStringArrayToMFString(String[] st) {
        StringBuffer sb = new StringBuffer();
        int len = st.length;

        for (int i = 0; i < len; i++) {
            sb.append("\"");
            sb.append(st[i]);
            sb.append("\"");
        }

        return sb.toString();
    }

    /**
     * Combine all the property sheets into one document.
     *
     * @param td The entity
     * @return The combined document
     */
    public Document combinePropertySheets(Entity td) {

        /*
        Iterator itr = td.getProperties().entrySet().iterator();

        Document combined_props = null;
        boolean first = true;
        Map.Entry entry;
        Node ep_root = null;

        while(itr.hasNext()) {
            entry = (Map.Entry) itr.next();
            Document props = (Document) entry.getValue();

            //System.out.println("Printing Tool DOM");
            //org.chefx3d.util.DOMUtils.print(props);

            if (first) {
                // Export the whole DOM
                first = false;
                combined_props = (Document) props.cloneNode(true);

                ep_root = XPathEvaluator.getNode("/ChefX3D/EntityParams", true, combined_props);
            } else {
                // Insert the sheet back into the combined structure

                NodeList l = props.getElementsByTagName("Sheet");
                Node n = null;
                int len = l.getLength();

                if (len == 0) {
                    //ignore as its not an Entity Sheet
                } else if (l.getLength() > 1) {
                    System.out.println("*** More then one sheet in a Sheet?");
                } else {
                    n = l.item(0);
                    Node inode = combined_props.importNode(n, true);
                    ep_root.appendChild(inode);
                }
            }
        }

        return combined_props;
        */
        return null;
    }
}
