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
import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.w3c.dom.Document;

// Internal Imports
import org.chefx3d.PropertyPanelDescriptor;
import org.chefx3d.tool.*;
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.XPathEvaluator;
import org.chefx3d.catalog.*;

/**
 * Test cases to validate import / export capability
 *
 * @author Alan Hudson
 * @version $Revision: 1.15 $
 */
public class TestImportExport extends TestCase {

    private BaseWorldModel model;
    private Tool boxTool;
    private Tool fence1Tool;
    private Tool fence2Tool;
    private Tool gridTool;
    private Tool controllerTool;
    private boolean local;

    /**
     * Basic constructor
     *
     * @param name
     */
    public TestImportExport(String name) {
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

        ArrayList chapters = new ArrayList();
        ArrayList tools = new ArrayList();

        String cpath = "catalog/";
        Document properties = null;
        HashMap<String, String> styles;
        String[] toolParams = null;
        PropertyPanelDescriptor[] propertyPanels;
        PropertyPanelDescriptor[] segmentPanels;
        PropertyPanelDescriptor[] vertexPanels;

        // Create a Box Tool
        properties = DOMUtils
            .parseXML("<ChefX3D><EntityParams><Sheet name='SMAL'><Box x='2' y='2' z='2' solid='TRUE' /></Sheet></EntityParams></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[3];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);

        properties = DOMUtils
            .parseXML("<ChefX3D><EntityParams><Sheet name='Behavior'><SimulationAgent name='foo' /></Sheet></EntityParams></ChefX3D>");
        propertyPanels[1] = new PropertyPanelDescriptor("Behavior", null, properties);

        properties = DOMUtils
            .parseXML("<ChefX3D><EntityParams><Sheet name='Another'><Test name='bar' foo='bar'/></Sheet></EntityParams></ChefX3D>");
        propertyPanels[2] = new PropertyPanelDescriptor("Another", null, properties);

        styles = new HashMap<String, String>();
        styles.put("x3d", cpath + "Primitives/Box/Box.xslt");
        styles.put("x3d_view", cpath + "Primitives/Box/Box_view.xslt");

        toolParams = null;

        boxTool = new Tool("Box", "images/Box.png", null, false, Tool.TYPE_MODEL,
                new String[] { cpath + "Primitives/Box/Box.x3d" }, 0, "Box",
                propertyPanels, null, null, styles, "SMAL", "/ChefX3D/EntityParams/Sheet[@name='SMAL']/Box/@x",
                "/ChefX3D/EntityParams/Box/@y", "/ChefX3D/EntityParams/Box/@z",
                toolParams,MultiplicityConstraint.NO_REQUIREMENT, "Model", false, false, false);

        properties = DOMUtils
            .parseXML("<ChefX3D><EntityParams><Sheet name='SMAL'><Fence height='2' /></Sheet></EntityParams></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);

        properties = DOMUtils
            .parseXML("<ChefX3D><SegmentParams><FenceSegment span='TRUE'/></SegmentParams></ChefX3D>");
        segmentPanels = new PropertyPanelDescriptor[1];
        segmentPanels[0] = new PropertyPanelDescriptor("Segment", null, properties);

        // create a MULTI_SEGMENT_TOOL
        properties = DOMUtils
            .parseXML("<ChefX3D>" +
                    "   <VertexParams>" +
                    "       <Sheet name='Post' >"+
                    "          <FenceVertex vertexID='0' cornerType='Curled'>" +
                    "             <position x='0.0' y='0.0' z='0.0' />" +
                    "             <rotation x='0.0' y='1.0' z='0.0' angle='0.0' />" +
                    "          </FenceVertex>" +
                    "       </Sheet>" +
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
        fence1Tool = new MultiSegmentTool("New Fixed Fence", "images/segment_point.png",
                null, false,
                Tool.TYPE_MULTI_SEGMENT, new String[] { cpath
                        + "Barrier/Fence/Fence.x3dv" }, 0, "Fence", propertyPanels,
                null, vertexPanels, styles, "SMAL", "0.3",
                "/ChefX3D/EntityParams/Sheet[@name='SMAL']/Fence/@height", "0.3", toolParams,
                MultiplicityConstraint.NO_REQUIREMENT, "SMAL", false, false, false, false);

        // create a WORLD tool
        properties = DOMUtils.parseXML("<ChefX3D><EntityParams><Sheet name='SMAL'><Grid /></Sheet></EntityParams></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);
        styles.put("x3d", "catalog/common/x3d_default_world.xslt");

        gridTool = new Tool("Grid", "images/Grid.png", null, false, Tool.TYPE_WORLD,
                new String[] { cpath + "Locations/Grid/Grid.x3dv" }, 0, "Grid",
                propertyPanels, null, null, styles, "SMAL", "16", "0.1", "16", toolParams,
                MultiplicityConstraint.SINGLETON, "World", false, false, false);

        // create a non fixed MULTI_SEGMENT_TOOL
        properties = DOMUtils
            .parseXML("<ChefX3D>" +
                    "   <VertexParams>" +
                    "       <Sheet name='Post' >"+
                    "         <FenceVertex vertexID='0' cornerType='Curled'>" +
                    "             <position x='0.0' y='0.0' z='0.0' />" +
                    "             <rotation x='0.0' y='1.0' z='0.0' angle='0.0' />" +
                    "         </FenceVertex>" +
                    "       </Sheet>" +
                    "   </VertexParams>" +
                    "</ChefX3D>");

        vertexPanels = new PropertyPanelDescriptor[1];
        vertexPanels[0] = new PropertyPanelDescriptor("Vertex", null, properties);

        styles = new HashMap<String, String>();
        styles.put("x3d", cpath + "Barrier/Fence/Fence.xslt");
        styles.put("x3d_global", cpath + "Barrier/Fence/Fence_global.xslt");
        styles.put("x3d_view", cpath + "Barrier/Fence/Fence_view.xslt");
        toolParams = new String[1];
        toolParams[0] = "2";
        fence2Tool = new MultiSegmentTool("New Free Fence", "images/segment_point.png",
                null, false,
                Tool.TYPE_MULTI_SEGMENT, new String[] { cpath
                        + "Barrier/Fence/Fence.x3dv" }, 0, "Fence", propertyPanels,
                null, vertexPanels, styles, "SMAL", "0.3",
                "/ChefX3D/EntityParams/Sheet[@name='SMAL']/Fence/@height", "0.3", toolParams,
                MultiplicityConstraint.NO_REQUIREMENT, "Barrier", false, false, false, false);

        // create a WORLD tool
        properties = DOMUtils.parseXML("<ChefX3D><Grid /></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);
        styles.put("x3d", "catalog/common/x3d_default_world.xslt");


        // create a CONTROLLER tool
        properties = DOMUtils.parseXML("<ChefX3D><EntityParams><Sheet name='SMAL'><EntityDefinition><TerroristCellPlanner endCondition='High Value Target Damaged' timeout='2500' /></EntityDefinition></Sheet></EntityParams></ChefX3D>");

        // TODO: These have to be SMAL properties right now as the SMAL importer puts them there
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL",null,properties);

        controllerTool = new Tool("Terrorist Cell Planner","build/images/spy_vs_spy.png",null,
                  false, Tool.TYPE_MODEL, new String[] {""},
                  0,"TerroristCellPlanner", propertyPanels, null, null, styles, "SMAL",
                  "1",
                  "1",
                  "1",
                  toolParams, MultiplicityConstraint.SINGLETON, "EndCondition", false, false, true);


        chapters.add(gridTool);
        chapters.add(controllerTool);
        chapters.add(boxTool);
        chapters.add(fence1Tool);
        chapters.add(fence2Tool);

        ToolGroup td = new ToolGroup("All Tools", chapters);
        tools.add(td);
        Catalog catalog = new Catalog("Test", 1, 0);
        catalog.addTools(tools);
        CatalogManager manager = CatalogManager.getCatalogManager();
        manager.addCatalog(catalog);
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    public void tearDown() {
        model = null;
        boxTool = null;
    }

    /**
     *  Test exporting a model and then reimporting.
     */

    public void testExportImport() {
        double TOLERANCE = 0.0001;

        // Create a builder
        EntityBuilder builder = EntityBuilder.getEntityBuilder();

        // add a WORLD
        int entityID = model.issueEntityID();

        Entity grid = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, gridTool);
        model.addEntity(local, grid, null);

        // add a MODEL
        entityID = model.issueEntityID();

        Entity box = builder.createEntity(model, entityID, new double[] {1.5, 2 ,3}, new float[] {0,1,0,0.1f}, boxTool);
        model.addEntity(local, box, null);

        // add a fixed MULTI_SEGMENT_CREATE
        entityID = model.issueEntityID();

        Entity fence = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, fence1Tool);
        model.addEntity(local, fence, null);

        model.addSegmentVertex(local, entityID, 0, new double[] {1.12, 2 ,3}, null);
        model.addSegmentVertex(local, entityID, 1, new double[] {3.12, 2 ,3}, null);
		model.addSegment(local, entityID, 0, 0, 1, null);

        // add a CONTROLLER
        entityID = model.issueEntityID();

        Entity controller = builder.createEntity(model, entityID, new double[] {4.234, 5, 6}, new float[] {0,0,1,1.57f}, controllerTool);
        model.addEntity(local, fence, null);

        // add a fixed MULTI_SEGMENT_CREATE
        entityID = model.issueEntityID();

        Entity fence2 = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, fence2Tool);
        model.addEntity(local, fence2, null);

        model.addSegmentVertex(local, entityID, 0, new double[] {1.12, 2 ,3}, null);
        model.addSegmentVertex(local, entityID, 1, new double[] {2, 3.333 ,4}, null);
        model.addSegmentVertex(local, entityID, 2, new double[] {2.2, 3.533 ,6}, null);
		model.addSegment(local, entityID, 0, 0, 1, null);
		model.addSegment(local, entityID, 1, 1, 2, null);

        StringWriter writer = new StringWriter();

        SMALExporter smalExporter = new SMALExporter();
        smalExporter.export(model, writer);

        String export = writer.getBuffer().toString();


System.out.println("Exported: \n: " + export);

        // create the command controller
        CommandController commandController = new DefaultCommandController();

        // Create a new model to import to
        model = new DefaultWorldModel(commandController);
        setUp();

        SMALImporter smalImporter = new SMALImporter();
        StringReader reader = new StringReader(export);
        try {
            smalImporter.importModel(model, reader);
        } catch (Exception e) {

        }


        // Now test that imported correctly

        // Grid
        Compare.Entity("Grid", grid, model.getEntity(0));

        // Box
        Compare.Entity("Box", box, model.getEntity(1));

        // Fixed Fence
        Compare.Entity("Fixed Fence", fence, model.getEntity(2));

        // Free Fence
        Compare.Entity("Free Fence", fence2, model.getEntity(4));

        // Controller
        Compare.Entity("Controller", controller, model.getEntity(3));
    }
}