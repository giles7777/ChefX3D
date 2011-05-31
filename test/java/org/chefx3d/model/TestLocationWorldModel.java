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
import org.junit.*;
import org.w3c.dom.Document;
import junit.framework.TestCase;

// Internal Imports
import org.chefx3d.PropertyPanelDescriptor;
import org.chefx3d.tool.MultiplicityConstraint;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.DOMUtils;

/**
 * Test cases to validate location functionality
 * 
 * @author Russell
 * @version $Revision: 1.7 $
 */
public class TestLocationWorldModel extends TestCase {

    private BaseWorldModel model;    
    private Tool locationTool;

    /**
     * Basic constructor
     * 
     * @param name
     */
    public TestLocationWorldModel(String name) {
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
 
        String cpath = "catalog/";
        Document properties = null;
        HashMap<String, String> styles;
        String[] toolParams = null;
        PropertyPanelDescriptor[] propertyPanels;

        // Create a Location Tool   
        properties = DOMUtils.parseXML("<ChefX3D><Grid /></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);
  
        styles = new HashMap<String, String>();
        styles.put("x3d", "catalog/common/x3d_default_world.xslt");
        
        toolParams = null;
        
        locationTool = new Tool("Grid", "images/Grid.png", null, false, Tool.TYPE_WORLD,
                new String[] { cpath + "Locations/Grid/Grid.x3dv" }, 0, "Grid",
                propertyPanels, null, null, styles, "SMAL", "16", "0.1", "16", toolParams,
                MultiplicityConstraint.SINGLETON, "World", false, false, false);

    }

    /**
     * Tears down the test fixture. 
     * (Called after every test case method.)
     */
    public void teardown() {
        model = null;
        locationTool = null;
    }
        
    public void testAddLocation() {
        
        // Create a builder
        EntityBuilder builder = EntityBuilder.getEntityBuilder();

        int entityID = model.issueEntityID();

        // add the entity
        Entity location = builder.createEntity(model, entityID, new double[3], new float[] {0,1,0,0}, locationTool);
        model.addEntity(true, location, null);

        // Check results
        Entity[] entityList = model.getModelData();        
        assertEquals("Should only be 1 entity in the model", 1, entityList.length);
        assertEquals("Location should be the same", location, entityList[location.getEntityID()]);
    }

    public void testRemoveLocation() {
        
        // add the entity
        testAddLocation();
               
        Entity location =   model.getModelData()[0]; 
        assertEquals("Should be entityID 0", 0, location.getEntityID());
       
        model.removeEntity(true, location, null);

        assertNull("Should be no entites in the model",  model.getModelData()[location.getEntityID()]);
       
    }
 
    public void testReplaceLocation() {

        // Create a builder
        EntityBuilder builder = EntityBuilder.getEntityBuilder();

        // add the entity
        testAddLocation();

        // clear the model
        testClearModel();   
   
        int entityID = model.issueEntityID();

        // add the entity
        Entity location = builder.createEntity(model, entityID, new double[3], new float[] {0,1,0,0}, locationTool);
        model.addEntity(true, location, null);

        // Check results
        Entity[] entityList = model.getModelData();        
        assertEquals("Should only be 1 entity in the model", 1, entityList.length);
        assertEquals("Location should be the same", location, entityList[location.getEntityID()]);

    }

    public void testClearModel() {
   
        // clear the model
        model.clear(true, null);

        // Check results
        Entity[] entityList = model.getModelData();        
        
        for (int i = 0; i < entityList.length; i++) {
            Assert.assertNull("Entity " + i, entityList[i]);
        }
        
    }

}