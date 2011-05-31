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
 * Test cases to validate basic entity functionality
 * 
 * @author Russell
 * @version $Revision: 1.8 $
 */
public class TestEntityWorldModel extends TestCase {

    private BaseWorldModel model;    
    private Tool boxTool;
    private boolean local;
   
    /**
     * Basic constructor
     * 
     * @param name
     */
    public TestEntityWorldModel(String name) {
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

        // Create a Box Tool   
        properties = DOMUtils
            .parseXML("<ChefX3D><EntityParams><Box x='2' y='2' z='2' solid='TRUE' /></EntityParams></ChefX3D>");
        propertyPanels = new PropertyPanelDescriptor[1];
        propertyPanels[0] = new PropertyPanelDescriptor("SMAL", null, properties);
        
        styles = new HashMap<String, String>();
        styles.put("x3d", cpath + "Primitives/Box/Box.xslt");
        styles.put("x3d_view", cpath + "Primitives/Box/Box_view.xslt");
        
        toolParams = null;
        
        boxTool = new Tool("Box", "images/Box.png", null, false, Tool.TYPE_MODEL,
                new String[] { cpath + "Primitives/Box/Box.x3d" }, 0, "Box",
                propertyPanels, null, null, styles, "SMAL", "/ChefX3D/EntityParams/Box/@x",
                "/ChefX3D/EntityParams/Box/@y", "/ChefX3D/EntityParams/Box/@z",
                toolParams,MultiplicityConstraint.NO_REQUIREMENT, "Model", false, false, false);
 
    }

    /**
     * Tears down the test fixture. 
     * (Called after every test case method.)
     */
    public void tearDown() {
        model = null;
        boxTool = null;
    }
        
    public void testAddBox() {
          
        int entityID = model.issueEntityID();
        
        // create the entity
        EntityBuilder builder = EntityBuilder.getEntityBuilder();
        Entity box = builder.createEntity(model, entityID, new double[] {0, 0 ,0}, new float[] {0,1,0,0}, boxTool);
        
        model.addEntity(local, box, null);

        // Check results
        Entity[] entityList = model.getModelData();
        
        assertEquals("Should be 1 entity in the model", 1, entityList.length);
        assertEquals("Box should be the same", box, model.getEntity(0));       
        
    }
 
    public void testRemoveBox() throws ModelChangeException {
                
        // add the entity
        testAddBox();
        
        // remove the entity
        Entity box = model.getEntity(0);
        model.removeEntity(local, box, null);
        
        // Check results        
        box = model.getEntity(0);
        assertNull("The entity should be null", box);
    }
 
    public void testMoveBox() {
        
        // add the box to the model
        testAddBox();
        
        // create a new postion and move there
        double[] newPos = new double[] {2, 2, 2};
        double[] checkPos = new double[3];
        
        model.moveEntity(local, 0, newPos, null);
        
        // Check results
        Entity check = model.getEntity(0);
        ((PositionableEntity)check).getPosition(checkPos);
        
        assertEquals("x-axis check", newPos[0], checkPos[0]);
        assertEquals("y-axis check", newPos[1], checkPos[1]);
        assertEquals("z-axis check", newPos[2], checkPos[2]);
        
    }
 
    public void testRotateBox() {
        
        // add the box to the model
        testAddBox();
        
        // create a new rotation and rotate there
        float[] newRot = new float[] {0, 1, 0, 1.5f};
        float[] checkRot = new float[4];
        
        model.rotateEntity(local, 0, newRot, null);
        
        // Check results
        Entity check = model.getEntity(0);
        ((PositionableEntity)check).getRotation(checkRot);
        
        assertEquals("x-axis check", newRot[0], checkRot[0]);
        assertEquals("y-axis check", newRot[1], checkRot[1]);
        assertEquals("z-axis check", newRot[2], checkRot[2]);
        assertEquals("angle check", newRot[3], checkRot[3]);
        
    }
    
    public void testChangeProperty() {
        
        // add the box to the model
        testAddBox();
        
        // change the property
        Entity box = model.getEntity(0);
        box.setProperties("SMAL", "/ChefX3D/EntityParams/Box/@solid", "FALSE");

        // Check results       
        box = model.getEntity(0);

        String newValue = 
            XPathEvaluator.getString("/ChefX3D/EntityParams/Box/@solid", box.getProperties("SMAL"));

        assertEquals("Check value", "FALSE", newValue);
        
    }

    
}