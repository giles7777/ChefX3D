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

// External Imports
import java.util.HashMap;
import junit.framework.TestCase;
import org.w3c.dom.Document;

// Internal Imports
import org.chefx3d.PropertyPanelDescriptor;
import org.chefx3d.tool.MultiplicityConstraint;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.XPathEvaluator;

/**
 * Test cases to validate basic segmented entity functionality
 *
 * @author Russell
 * @version $Revision: 1.10 $
 */
public class TestFenceEntityWorldModel extends TestCase {

    private BaseWorldModel model;
    private Tool fenceToolFixed;
    private Tool fenceToolFree;
    private boolean local;

    /**
     * Basic constructor
     *
     * @param name
     */
    public TestFenceEntityWorldModel(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    public void setUp() {

        // create the command controller
        CommandController controller = new DefaultCommandController();

        // Create the Model
        model = new DefaultWorldModel(controller);
        local = true;

        String cpath = "catalog/";
        Document properties = null;
        HashMap<String, String> styles;
        String[] toolParams = null;
        PropertyPanelDescriptor[] propertyPanels;
        PropertyPanelDescriptor[] segmentPanels;
        PropertyPanelDescriptor[] vertexPanels;

        // Create a Box Tool
        properties = DOMUtils
            .parseXML("<ChefX3D><EntityParams><Fence height='2' /></EntityParams></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);

        properties = DOMUtils
            .parseXML("<ChefX3D><SegmentParams><FenceSegment span='TRUE'/></SegmentParams></ChefX3D>");
        segmentPanels = new PropertyPanelDescriptor[1];
        segmentPanels[0] = new PropertyPanelDescriptor("Segment", null, properties);

        properties = DOMUtils
            .parseXML("<ChefX3D>" +
                    "   <VertexParams>" +
                    "       <FenceVertex vertexID='0' cornerType='Curled'>" +
                    "           <position x='0.0' y='0.0' z='0.0' />" +
                    "           <rotation x='0.0' y='1.0' z='0.0' angle='0.0' />" +
                    "       </FenceVertex>" +
                    "   </VertexParams>" +
                    "</ChefX3D>");
        vertexPanels = new PropertyPanelDescriptor[1];
        vertexPanels[0] = new PropertyPanelDescriptor("Vertex", null, properties);

        styles = new HashMap<String, String>();
        styles.put("x3d", cpath + "Barrier/Fence/Fence.xslt");
        styles.put("x3d_global", cpath + "Barrier/Fence/Fence_global.xslt");
        styles.put("x3d_view", cpath + "Barrier/Fence/Fence_view.xslt");

        toolParams = new String[1];
        toolParams[0] = "0"; // Segment length is unrestricted if set to 0

        fenceToolFree = new Tool("New Fence", "images/segment_point.png",
                null, false,
                Tool.TYPE_MULTI_SEGMENT, new String[] { cpath
                        + "Barrier/Fence/Fence.x3dv" }, 0, "Fence", propertyPanels,
                null, vertexPanels, styles, "SMAL", "0.3",
                "/ChefX3D/EntityParams/Fence/@height", "0.3", toolParams,
                MultiplicityConstraint.NO_REQUIREMENT, "Barrier", false, false, false);

        toolParams[0] = "2"; // Segment length is unrestricted if set to 0

        fenceToolFixed = new Tool("New Fence", "images/segment_point.png",
               null, false,
                Tool.TYPE_MULTI_SEGMENT, new String[] { cpath
                        + "Barrier/Fence/Fence.x3dv" }, 0, "Fence", propertyPanels,
                null, vertexPanels, styles, "SMAL", "0.3",
                "/ChefX3D/EntityParams/Fence/@height", "0.3", toolParams,
                MultiplicityConstraint.NO_REQUIREMENT, "Barrier", false, false, false);

    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    public void tearDown() {
        model = null;
        fenceToolFree = null;
        fenceToolFixed = null;
    }

    public void testAddFence() {

        int entityID = model.issueEntityID();

        // create the entity
        EntityBuilder builder = EntityBuilder.getEntityBuilder();
        Entity fence = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, fenceToolFree);

        model.addEntity(local, fence, null);

        // Check results
        Entity[] entityList = model.getModelData();

        assertEquals("Should be 1 entity in the model.", 1, entityList.length);
        assertEquals("Fence should be the same.", fence, model.getEntity(0));
        assertTrue("Should be a segmented entity.", model.getEntity(0) instanceof SegmentableEntity);

    }

    public void testAddVertex() {

        // add the entity
        testAddFence();

        // add the vertex
        double[] newPos = new double[] {0, 0, 0};
        double[] checkPos;

        model.addSegmentVertex(local, 0, 0, newPos, null);

        // Check results
        Entity fence = model.getEntity(0);
        checkPos = ((SegmentableEntity)fence).getSegmentSequence().getVertex(0).getPosition();

        assertNotNull("A position should be returned.", checkPos);
        assertEquals("x-axis check.", newPos[0], checkPos[0]);
        assertEquals("y-axis check.", newPos[1], checkPos[1]);
        assertEquals("z-axis check.", newPos[2], checkPos[2]);

    }

    public void testAddMultipleVertexFree() {
        addMultipleVertex(false);
    }

    public void testAddMultipleVertexFixed() {
        addMultipleVertex(true);
    }

    private void addMultipleVertex(boolean fixed) {

        Entity fence;
        double[] pos1 = new double[] {0, 0, 0};
        double[] pos2 = new double[] {2, 0, 0};
        double[] pos3 = new double[] {-2, 0, 2};
        double[] checkPos;

        EntityBuilder builder = EntityBuilder.getEntityBuilder();

        int entityID = model.issueEntityID();

        // add the entity
        if (fixed) {
            // create the entity
            fence = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, fenceToolFixed);
        } else {
            fence = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, fenceToolFree);
        }

        model.addEntity(local, fence, null);

        // add vertices
        model.addSegmentVertex(local, 0, 0, pos1, null);
        model.addSegmentVertex(local, 0, 1, pos2, null);
        model.addSegmentVertex(local, 0, 2, pos3, null);

        // Check results
        fence = model.getEntity(0);

        checkPos = ((SegmentableEntity)fence).getSegmentSequence().getVertex(0).getPosition();
        assertNotNull("A position should be returned.", checkPos);
        assertEquals("x-axis check.", pos1[0], checkPos[0]);
        assertEquals("y-axis check.", pos1[1], checkPos[1]);
        assertEquals("z-axis check.", pos1[2], checkPos[2]);

        checkPos = ((SegmentableEntity)fence).getSegmentSequence().getVertex(1).getPosition();
        assertNotNull("A position should be returned.", checkPos);
        assertEquals("x-axis check.", pos2[0], checkPos[0]);
        assertEquals("y-axis check.", pos2[1], checkPos[1]);
        assertEquals("z-axis check.", pos2[2], checkPos[2]);

        checkPos = ((SegmentableEntity)fence).getSegmentSequence().getVertex(2).getPosition();
        assertNotNull("A position should be returned.", checkPos);
        assertEquals("x-axis check.", pos3[0], checkPos[0]);
        assertEquals("y-axis check.", pos3[1], checkPos[1]);
        assertEquals("z-axis check.", pos3[2], checkPos[2]);

    }

    public void testMoveVertex() {

        // add the fence and vertex to the model
        testAddVertex();

        // create a new postion and move there
        double[] newPos = new double[] {2, 2, 2};
        double[] checkPos;

        model.moveSegmentVertex(local, 0, 0, newPos, null);

        // Check results
        Entity fence = model.getEntity(0);
        checkPos = ((SegmentableEntity)fence).getSegmentSequence().getVertex(0).getPosition();

        assertNotNull("A position should be returned.", checkPos);
        assertEquals("x-axis check.", newPos[0], checkPos[0]);
        assertEquals("y-axis check.", newPos[1], checkPos[1]);
        assertEquals("z-axis check.", newPos[2], checkPos[2]);

    }

    public void testRemoveVertex() {

        // add the fence and vertex to the model
        testAddVertex();

        // remove the vertex
        model.removeSegmentVertex(local, 0, 0, null);

        // Check results
        Entity fence = model.getEntity(0);
        SegmentVertex vertex = ((SegmentableEntity)fence).getSegmentSequence().getVertex(0);

        assertNull("No vertex should be returned.", vertex);

    }

    public void testRemoveInternalVertexFree() {

        SegmentVertex vertex;
        double[] checkPos = new double[3];
        double[] pos1 = new double[] {0, 0, 0};
        double[] pos2 = new double[] {2, 0, 0};
        double[] pos3 = new double[] {-2, 0, 2};

        // add the fence and vertex to the model
        addMultipleVertex(false);

        // remove the middle vertex
        model.removeSegmentVertex(local, 0, 1, null);

        // Check results
        Entity fence = model.getEntity(0);

        vertex = ((SegmentableEntity)fence).getSegmentSequence().getVertex(1);
        assertNull("No vertex should be returned.", vertex);
        assertEquals("Two vertex should remain.", 2, ((SegmentableEntity)fence).getSegmentSequence().getLength());

        vertex = ((SegmentableEntity)fence).getSegmentSequence().getVertex(0);
        assertNotNull("A position should be returned.", vertex.getPosition());
        checkPos = vertex.getPosition();
        assertEquals("x-axis check.", pos1[0], checkPos[0]);
        assertEquals("y-axis check.", pos1[1], checkPos[1]);
        assertEquals("z-axis check.", pos1[2], checkPos[2]);

        vertex = ((SegmentableEntity)fence).getSegmentSequence().getVertex(2);
        assertNotNull("A position should be returned.", vertex.getPosition());
        checkPos = vertex.getPosition();
        assertEquals("x-axis check.", pos3[0], checkPos[0]);
        assertEquals("y-axis check.", pos3[1], checkPos[1]);
        assertEquals("z-axis check.", pos3[2], checkPos[2]);

    }

    public void testRemoveInternalVertexFixed() {

        // add the fence and vertex to the model
        addMultipleVertex(true);

        // remove the middle vertex
        model.removeSegmentVertex(local, 0, 1, null);

        // Check results
        Entity fence = model.getEntity(0);
        SegmentVertex vertex = ((SegmentableEntity)fence).getSegmentSequence().getVertex(1);

        assertNotNull("A vertex should be returned.", vertex);
        assertNotNull("A position should be returned.", vertex.getPosition());
        assertEquals("Three vertex should remain.", 3, ((SegmentableEntity)fence).getSegmentSequence().getLength());
    }

}