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

package org.chefx3d.model;


//External Imports
import static junit.framework.Assert.*;
import java.util.*;

import javax.swing.JPanel;

import org.w3c.dom.*;

// Internal Imports
import org.chefx3d.property.awt.PropertyPanelLayout;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.XPathEvaluator;
import org.chefx3d.util.DOMUtils;

/**
 * Comparison tests that can be used throughout the TestCases
 *
 * @author Russell Dodds
 * @version $Revision: 1.11 $
 */
public class Compare {

    private static double TOLERANCE = 0.0001;

    /**
     * Check to make sure the Entity provided are extactly the same
     *
     * @param message
     * @param entity1
     * @param entity2
     */
    public static void Entity(String message, Entity entity1, Entity entity2) {

        assertNotNull(message + " entity1 null", entity1);
        assertNotNull(message + " entity2 null", entity2);

        // Check Tool
        assertEquals(message + " - check tool name.", entity1.getName(), entity2.getName());
        assertEquals(message + " - check tool type.", entity1.getType(), entity2.getType());
        assertEquals(message + " - check tool category.", entity1.getCategory(), entity2.getCategory());

        // Check validation
        assertEquals(message + " - ID.", entity1.getEntityID(), entity2.getEntityID());
        assertEquals(message + " - category.", entity1.getCategory(), entity2.getCategory());
        assertEquals(message + " - multiplicity.", entity1.getConstraint(), entity2.getConstraint());

        // Check position
        double[] position1 = new double[3];
        double[] position2 = new double[3];
        ((PositionableEntity)entity1).getPosition(position1);
        ((PositionableEntity)entity2).getPosition(position2);

        assertEquals(message + " - position x-axis check.", position1[0], position2[0], TOLERANCE);
        assertEquals(message + " - position y-axis check.", position1[1], position2[1], TOLERANCE);
        assertEquals(message + " - position z-axis check.", position1[2], position2[2], TOLERANCE);

        // Check rotation
        float[] rotation1 = new float[4];
        float[] rotation2 = new float[4];
        ((PositionableEntity)entity1).getRotation(rotation1);
        ((PositionableEntity)entity2).getRotation(rotation2);

        assertEquals(message + " - rotation x-axis check.", rotation1[0], rotation2[0], (float) TOLERANCE);
        assertEquals(message + " - rotation y-axis check.", rotation1[1], rotation2[1], (float) TOLERANCE);
        assertEquals(message + " - rotation z-axis check.", rotation1[2], rotation2[2], (float) TOLERANCE);
        assertEquals(message + " - rotation angle check.", rotation1[3], rotation2[3], (float) TOLERANCE);

        // Check size
        float[] size1 = new float[3];
        float[] size2 = new float[3];
        ((PositionableEntity)entity1).getSize(size1);
        ((PositionableEntity)entity2).getSize(size2);

        assertEquals(message + " - size x-axis check.", size1[0], size2[0], (float) TOLERANCE);
        assertEquals(message + " - size y-axis check.", size1[1], size2[1], (float) TOLERANCE);
        assertEquals(message + " - size z-axis check.", size1[2], size2[2], (float) TOLERANCE);

        // Check associations
        //int[] associates1 = ((AssociatableEntity)entity1).getAssociates();
        //int[] associates2 = ((AssociatableEntity)entity2).getAssociates();
        //assertEquals(message + " - size associates.", associates1.length, associates2.length);
        //for (int i = 0; i < associates1.length; i++) {
        //    assertEquals(message + " - check associate[" + i + "].", associates1[i], associates2[i]);
        //}

        // Check Segments, if necessary
        assertEquals(
                message + " - segment entity.", 
                entity1 instanceof SegmentableEntity , 
                entity2 instanceof SegmentableEntity );
        if (entity1 instanceof SegmentableEntity && entity2 instanceof SegmentableEntity) {
            SegmentSequence(message, ((SegmentableEntity)entity1).getSegmentSequence(), ((SegmentableEntity)entity2).getSegmentSequence());
        }

        // Check the property sheets
        Properties(message, entity1.getProperties(), entity2.getProperties());

    }

    /**
     * Check to make sure the SegmentSequence provided are extactly the same
     *
     * @param message
     * @param segment1
     * @param segment2
     */
    public static void SegmentSequence(String message, SegmentSequence sequence1, SegmentSequence sequence2) {

        assertEquals(message + " - size sequence.", sequence1.getLength(), sequence2.getLength());

        ArrayList<SegmentVertex> vertices1 = sequence1.getVertices();
        ArrayList<SegmentVertex> vertices2 = sequence2.getVertices();

        assertEquals(message + " - size vertices.", vertices1.size(), vertices1.size());

        for (int i = 0; i < vertices1.size(); i++) {
            SegmentVertex(message, vertices1.get(i), vertices2.get(i));
        }

        ArrayList<Segment> segments1 = sequence1.getSegments();
        ArrayList<Segment> segments2 = sequence2.getSegments();

        assertEquals(message + " - size segments.", segments1.size(), segments2.size());

        for(int i=0; i < segments1.size(); i++) {
            Segment(message, segments1.get(i), segments2.get(i));
        }

        double[] position1 = new double[3];
        double[] position2 = new double[3];

        // Check Start Vertex
        position1 = sequence1.getStartPosition();
        position2 = sequence2.getStartPosition();

        assertEquals(message + " - start position x-axis check.", position1[0], position2[0], TOLERANCE);
        assertEquals(message + " - start position y-axis check.", position1[1], position2[1], TOLERANCE);
        assertEquals(message + " - start position z-axis check.", position1[2], position2[2], TOLERANCE);

        // Check End Vertex
        position1 = sequence1.getEndPosition();
        position2 = sequence2.getEndPosition();

        assertEquals(message + " - end position x-axis check.", position1[0], position2[0], TOLERANCE);
        assertEquals(message + " - end position y-axis check.", position1[1], position2[1], TOLERANCE);
        assertEquals(message + " - end position z-axis check.", position1[2], position2[2], TOLERANCE);
    }

    /**
     * Check to make sure the SegmentVertex provided are extactly the same
     *
     * @param message
     * @param vertex1
     * @param vertex2
     */
    public static void SegmentVertex(String message, SegmentVertex vertex1, SegmentVertex vertex2) {

        // Check ID
        assertEquals(message + " - check vertex ID.", vertex1.getVertexID(), vertex2.getVertexID());

        // Check position
        double[] position1 = new double[3];
        double[] position2 = new double[3];

        position1 = vertex1.getPosition();
        position2 = vertex2.getPosition();

        assertEquals(message + " - vertex position x-axis check.", position1[0], position2[0], TOLERANCE);
        assertEquals(message + " - vertex position y-axis check.", position1[1], position2[1], TOLERANCE);
        assertEquals(message + " - vertex position z-axis check.", position1[2], position2[2], TOLERANCE);

        // Check rotation
        float[] rotation1 = new float[4];
        float[] rotation2 = new float[4];

        rotation1 = vertex1.getRotation();
        rotation2 = vertex2.getRotation();

        assertEquals(message + " - vertex rotation x-axis check.", rotation1[0], rotation2[0], (float) TOLERANCE);
        assertEquals(message + " - vertex rotation y-axis check.", rotation1[1], rotation2[1], (float) TOLERANCE);
        assertEquals(message + " - vertex rotation z-axis check.", rotation1[2], rotation2[2], (float) TOLERANCE);
        assertEquals(message + " - vertex rotation angle check.", rotation1[3], rotation2[3], (float) TOLERANCE);

    }

    /**
     * Check to make sure the Segment provided are extactly the same
     *
     * @param message
     * @param vertex1
     * @param vertex2
     */
    public static void Segment(String message, Segment segment1, Segment segment2) {

        // Check ID
        assertEquals(message + " - check segment ID.", segment1.getSegmentID(), segment2.getSegmentID());

        // check startVertex
        int startVertex1 = segment1.getStartIndex();
        int startVertex2 = segment2.getStartIndex();

        assertEquals(message + " - startVertex check.", startVertex1, startVertex2);

        // check endVertex
        int endVertex1 = segment1.getEndIndex();
        int endVertex2 = segment2.getEndIndex();

        assertEquals(message + " - endVertex check.", endVertex1, endVertex2);
    }

    /**
     * Check to make sure the properties provided are extactly the same
     *
     * @param message
     * @param properties1
     * @param properties2
     */
    public static void Properties(String message, Map<String, Document> properties1, Map<String, Document> properties2) {

        Iterator itr = properties1.entrySet().iterator();
        Map.Entry<String, Document> entry;

        while (itr.hasNext()) {
            entry = (Map.Entry<String, Document>) itr.next();

            String sheetName1 = (String) entry.getKey();
            Document document1 = (Document) entry.getValue();

            assertTrue(message + " - sheetName check.", properties2.containsKey(sheetName1));
            Document document2 = properties2.get(sheetName1);

            Document(message, document1, document2);
        }

    }

    /**
     * Check to make sure the Documents provided are extactly the same
     *
     * @param message
     * @param vertex1
     * @param vertex2
     */
    public static void Document(String message, Document document1, Document document2) {

        // lets start parsing the property sheet
        NodeList nodes = document1.getChildNodes();

        // get the first node to work with
        Node currentNode = nodes.item(0);
        String xPathRoot = "/" + currentNode.getNodeName();

        // No properties to edit
        if (currentNode == null)
            return;

        // get the list of nodes to process
        nodes = currentNode.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            currentNode = nodes.item(i);
            //xPathRoot += "/" + currentNode.getNodeName();

            if (currentNode instanceof Element) {
                checkNode((Element) currentNode, xPathRoot, message, document1, document2);
            }
        }

    }

    private static void checkNode(Node node, String xPathRoot, String message,
            Document document1, Document document2) {

        String xPathCheck = xPathRoot + "/" + node.getNodeName();

        Node node1 = null;
        Node node2 = null;

        try {
            node1 = XPathEvaluator.getNode(xPathCheck, true, document1);
            node2 = XPathEvaluator.getNode(xPathCheck, true, document2);
        } catch(Exception e) {
            e.printStackTrace();
            fail("Exception in xpath: " + e.getMessage());
        }

        assertEquals(message + " - " + xPathCheck, node1.getNodeName(), node2.getNodeName());

        // check the node's attributes, if any
        NamedNodeMap attributes = node.getAttributes();
        checkAttributes(attributes, xPathCheck, message, document1, document2);

        // Check the node's children
        NodeList nodes = node.getChildNodes();
        Node childNode;
        for (int i = 0; i < nodes.getLength(); i++) {
            childNode = nodes.item(i);

            if (childNode instanceof Element) {
                checkNode(childNode, xPathCheck, message, document1, document2);
            }
        }

    }

   private static void checkAttributes(NamedNodeMap attributes, String xPathRoot, String message,
           Document document1, Document document2) {

       Attr check;
       String xPathCheck;
       String value1;
       String value2;

       for (int i = 0; i < attributes.getLength(); i++) {
           check = (Attr) attributes.item(i);

           xPathCheck = xPathRoot + "/@" + check.getName();

           value1 = XPathEvaluator.getString(xPathCheck, document1);
           value2 = XPathEvaluator.getString(xPathCheck, document2);

           assertEquals(message + " - " + xPathCheck, value1, value2);

       }

    }

}