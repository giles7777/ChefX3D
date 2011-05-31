/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2006-2007
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there"s a problem you get to fix it.
 *
 ****************************************************************************/

package demos;

// External Imports
import java.util.*;

import java.awt.EventQueue;
import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.tool.*;

import org.chefx3d.model.AbstractSegmentTool;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiplicityConstraint;

import org.chefx3d.property.AssociateProperty;
import org.chefx3d.model.ListProperty;
import org.chefx3d.property.ColorValidator;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.view.awt.gt2d.GT2DView;
import org.chefx3d.view.awt.gt2d.ViewingFrustum;

/**
 * A simple example of how to use ChefX3D
 *
 * @author Alan Hudson
 * @version
 */
public class SimpleExample extends BaseExample {

    /**
     * Create a new basic instance of the example
     */
    public SimpleExample() {
    }

    /**
     * Create ChefX3D tools.
     */
    protected void createTools() {
//System.out.println("createTools");
        // set to true if you want to use the DrillDownToolBar
        // otherwise set to false
        boolean includeRootNode = true;
        
        ArrayList<ToolGroup> tools = new ArrayList<ToolGroup>();
        ArrayList<ToolGroupChild> chapters = null;
        Tool tool;
        ToolGroup td;
        
        ToolGroup root = new ToolGroup("Tools");

        // Locations Menu
        // Grid World
        chapters = new ArrayList<ToolGroupChild>();

        String[] interfaceIcons =
            new String[] {
                ipath + "Grid16x16.png",
                ipath + "Grid32x32.png",
                ipath + "Grid64x64.png"};

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Grid",
                ViewingFrustum.Plane.TOP.toString(),
                ipath + "grid.png",
                interfaceIcons,
                Entity.TYPE_WORLD,
                "Grid.x3d",
                "Basic City Location",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.SINGLETON,
                "World",
                false,
                false,
                false,
                false,
                entityProperties);

        chapters.add(tool);

        td = new ToolGroup("Locations", chapters);
        if (includeRootNode)
            root.addToolGroup(td);
        else
            tools.add(td);
        
        // Primitives Menu
        // Box
        chapters = new ArrayList<ToolGroupChild>();

        interfaceIcons = new String[] {
                ipath + "Box16x16.png",
                ipath + "Box32x32.png",
                ipath + "Box64x64.png"};

        entityProperties = new HashMap<String, Object>();

        // a test associate property
        AssociateProperty associate1 =
            new AssociateProperty("Associated 1", new String[] {"Model", "Waypoint"});
        entityProperties.put("Associated 1", associate1);

        // a test associate property
        AssociateProperty associate2 =
            new AssociateProperty("Associated 2", new String[] {"Model1"});
        entityProperties.put("Associated 2", associate2);

        // a test list property
        String[] items = new String[] {"Box", "Cone", "Cylinder", "Sphere"};
        String[] pictures = new String[] {
                ipath + "Box64x64.png", 
                ipath + "Cone64x64.png", 
                ipath + "Cylinder64x64.png", 
                ipath + "Sphere64x64.png"};
        
        ListProperty list = new ListProperty(items, pictures);
        list.setValue(items[0]);
        entityProperties.put("Selection Test", list);

        // a test checkbox property
        entityProperties.put("CheckBox Test", true);

        // add the color property
        entityProperties.put("Color", new Color(255, 0, 0));

        // assign a validation class for position
        HashMap<String, Object> propertyValidators = new HashMap<String, Object>();
        ColorValidator validColor = new ColorValidator();
        propertyValidators.put("Color", validColor);

        HashMap<String, Map<String, Object>> properties = new HashMap<String, Map<String, Object>>();
        properties.put(Entity.DEFAULT_ENTITY_PROPERTIES, entityProperties);
        properties.put(Entity.PROPERTY_VALIDATORS, propertyValidators);

        String name = "This is a cool red box that has a long name";
        String desc = "This is the Box Primitive hover over text.  This can be very log if we need to.";
        
        tool = new Tool(
                name,
                ViewingFrustum.Plane.TOP.toString(),
                ipath + "Box.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Box.x3d",
                desc,
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model",
                false,
                false,
                false,
                false,
                properties);

        chapters.add(tool);

        // Cone
        interfaceIcons = new String[] {
                ipath + "Cone16x16.png",
                ipath + "Cone32x32.png",
                ipath + "Cone64x64.png"};

        entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Cone",
                ViewingFrustum.Plane.TOP.toString(),
                ipath + "Cone.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Cone.x3d",
                "Cone Primitive",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model",
                false,
                false,
                false,
                false,
                entityProperties);
        tool.setIcon(ViewingFrustum.Plane.FRONT.toString(), ipath + "Cone_Front.png");

        chapters.add(tool);

        // Cylinder
        interfaceIcons = new String[] {
                ipath + "Cylinder16x16.png",
                ipath + "Cylinder32x32.png",
                ipath + "Cylinder64x64.png"};

        entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Cylinder",
                ViewingFrustum.Plane.TOP.toString(),
                ipath + "Cylinder.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Cylinder.x3d",
                "Cylinder Primitive",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model1",
                false,
                false,
                false,
                false,
                entityProperties);

        chapters.add(tool);

        
        
        // Sphere
        interfaceIcons = new String[] {
                ipath + "Sphere16x16.png",
                ipath + "Sphere32x32.png",
                ipath + "Sphere64x64.png"};

        entityProperties = new HashMap<String, Object>();

        tool = new Tool(
                "Sphere",
                ViewingFrustum.Plane.TOP.toString(),
                ipath + "Sphere.png",
                interfaceIcons,
                Entity.TYPE_MODEL,
                "Sphere.x3d",
                "Sphere Primitive",
                new float[] {1f, 1f, 1f},
                new float[] {1f, 1f, 1f},
                MultiplicityConstraint.NO_REQUIREMENT,
                "Model1",
                false,
                false,
                false,
                false,
                entityProperties);

        ToolGroup sub = new ToolGroup("Subgroup");
        chapters.add(sub);
        
        sub.addTool(tool);

        td = new ToolGroup("Primitives", chapters);        
        if (includeRootNode)
            root.addToolGroup(td);
        else
            tools.add(td);

        // Segment Tools
        chapters = new ArrayList<ToolGroupChild>();

        // TODO: fix fencing
        tool = buildWallTool();
        chapters.add(tool);
        
        tool = buildWaypointTool();
        chapters.add(tool);

        tool = createAStarTool();
        chapters.add(tool);

        tool = createBuildingTool();
        chapters.add(tool);

        td = new ToolGroup("Feature Tools", chapters);
        if (includeRootNode)
            root.addToolGroup(td);
        else
            tools.add(td);


        CatalogManager cmanager = CatalogManager.getCatalogManager();
        Catalog catalog = new Catalog("Sample", 1, 0);
        cmanager.addCatalog(catalog);
        
        if (includeRootNode)
            catalog.addToolGroup(root);
        else
            catalog.addTools(tools);
      
        // debug, print the catalog
        catalog.printCatalog();
    }

    /**
     * Create tools for authoring walls
     */
    private SegmentableTool buildWallTool() {

        // vertex tool      
        Map<String,Map<String, Object>> vertexSheets = 
            new HashMap<String,Map<String, Object>>();
        Map<String, Object> vertexProps = new HashMap<String, Object>();
        vertexSheets.put(Entity.DEFAULT_ENTITY_PROPERTIES, vertexProps);

        VertexTool vertexTool = 
            new VertexTool(
                    "Vertex",
                    ipath + "Segment.png",
                    null,
                    Entity.TYPE_VERTEX,
                    "Sphere.x3d",
                    "Vertex",
                    new float[] {1f, 1f, 1f},
                    new float[] {0.3f, 0.3f, 0.3f},
                    MultiplicityConstraint.NO_REQUIREMENT,
                    "Wall",
                    false,
                    false,
                    true,
                    false,
                    vertexSheets);      
        
        // segment tool
        Map<String,Map<String, Object>> segmentSheets = 
            new HashMap<String,Map<String, Object>>();
        Map<String, Object> segmentProps = new HashMap<String, Object>();
        segmentSheets.put(Entity.DEFAULT_ENTITY_PROPERTIES, segmentProps);

        AbstractSegmentTool segmentTool = 
            new SegmentTool(
                    "Segment",
                    ipath + "Segment.png",
                    null,
                    Entity.TYPE_SEGMENT,
                    "Box.x3d",
                    "Segment",
                    new float[] {1f, 1f, 1f},
                    new float[] {0.3f, 0.3f, 0.3f},
                    MultiplicityConstraint.NO_REQUIREMENT,
                    "Segment",
                    false,
                    false,
                    true,
                    false,
                    segmentSheets);
 
        
        // segmentable tool       
        Map<String,Map<String, Object>> segmentableSheets = 
            new HashMap<String,Map<String, Object>>();
        Map<String, Object> segmentableProps = new HashMap<String, Object>();
        segmentableSheets.put(Entity.DEFAULT_ENTITY_PROPERTIES, segmentableProps);

        String[] interfaceIcons = new String[] {
            ipath + "Segment16x16.png",
                ipath + "Segment32x32.png",
                ipath + "Segment64x64.png"};
        
        SegmentableTool tool = 
            new SegmentableTool(
                    "Wall",
                    ipath + "Segment.png",
                    interfaceIcons,
                    Entity.TYPE_MULTI_SEGMENT,
                    "NoRep.x3d",
                    "Wall",
                    new float[] {1f, 1f, 1f},
                    new float[] {0.3f, 0.3f, 0.3f},
                    MultiplicityConstraint.SINGLETON,
                    "Wall",
                    false,
                    false,
                    true,
                    false,
                    0,
                    true,
                    segmentableSheets,
                    segmentTool, 
                    vertexTool);
                
        return tool;
    }

    /**
     * Create tools for authoring waypoints
     */
    private SegmentableTool buildWaypointTool() {

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        HashMap<String, Object> segmentProperties = new HashMap<String, Object>();
        HashMap<String, Object> vertexProperties = new HashMap<String, Object>();
        vertexProperties.put(Entity.MODEL_URL_PARAM, "Sphere.x3d"); 
        // Fence
        String[] interfaceIcons = new String[] {
                ipath + "Segment16x16.png",
                ipath + "Segment32x32.png",
                ipath + "Segment64x64.png"};

//        SegmentableTool tool = new SegmentableTool(
//                "Waypoint",
//                ipath + "Segment.png",
//                interfaceIcons,
//                Entity.TYPE_MULTI_SEGMENT,
//                "Box.x3d",
//                "Waypoint",
//                new float[] {1f, 1f, 1f},
//                new float[] {0.3f, 0.3f, 0.3f},
//                MultiplicityConstraint.NO_REQUIREMENT,
//                "Waypoint",
//                false,
//                false,
//                true,
//                false,
//                0,
//                true,
//                entityProperties,
//                segmentProperties, 
//                vertexProperties);

        return null;
    }

    /**
     * Create tools for authoring a-star networks
     */
    private SegmentableTool createAStarTool() {

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        entityProperties.put("reversible", true);

        HashMap<String, Object> segmentProperties = new HashMap<String, Object>();
        
        HashMap<String, Object> vertexProperties = new HashMap<String, Object>();
        vertexProperties.put("speed", 0);

        // Fence
        String[] interfaceIcons = new String[] {
                ipath + "Segment16x16.png",
                ipath + "Segment32x32.png",
                ipath + "Segment64x64.png"};

//        SegmentableTool tool = new SegmentableTool(
//                "AStar",
//                ipath + "Segment.png",
//                interfaceIcons,
//                Entity.TYPE_MULTI_SEGMENT,
//                "x3d/No3DRep.x3d",
//                "AStar",
//                new float[] {1f, 1f, 1f},
//                new float[] {0.3f, 0.3f, 0.3f},
//                MultiplicityConstraint.NO_REQUIREMENT,
//                "AStar",
//                false,
//                false,
//                true,
//                false,
//                0,
//                false,
//                entityProperties,
//                segmentProperties, 
//                vertexProperties);

        return null;
    }

    /**
     * Create tools for authoring buildings
     */
    private BuildingTool createBuildingTool() {

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        HashMap<String, Object> segmentProperties = new HashMap<String, Object>();
        HashMap<String, Object> vertexProperties = new HashMap<String, Object>();

        // define the building properties

        ListProperty storyList = new ListProperty(new String[] {"1", "2", "3", "4", "5"});
        storyList.setValue("1");
        entityProperties.put("Stories", storyList);

        entityProperties.put("Height per Story", 10.0f);

        ListProperty roofList =
            new ListProperty(new String[] {"Flat", "Sloped", "Single Peak", "Complex"});
        roofList.setValue("Flat");
        entityProperties.put("Roof Line", roofList);

        ListProperty wallList =
            new ListProperty(new String[] {"Wood", "Concrete", "Brick", "Metal"});
        wallList.setValue("Wood");
        entityProperties.put("Exterior Wall", wallList);

        entityProperties.put("Color", new int[] {0, 255, 0});

        // define the segment properties

        wallList =
            new ListProperty(new String[] {"Wall", "Door", "Window"});
        wallList.setValue("Wall");
        segmentProperties.put("Type", wallList);

        segmentProperties.put("Reinforced", false);

        ListProperty openingList =
            new ListProperty(new String[] {"Inward", "Outward", "Sliding"});
        openingList.setValue("Inward");
        segmentProperties.put("Opening Type", openingList);

        // define the vertex properties
        // none

        // building
//        BuildingTool tool = new BuildingTool(
//                "Building",
//                ipath + "BuildingBox.png",
//                null,
//                Entity.TYPE_BUILDING,
//                "x3d/No3DRep.x3d",
//                "Building",
//                new float[] {1f, 1f, 1f},
//                new float[] {1f, 1f, 1f},
//                MultiplicityConstraint.NO_REQUIREMENT,
//                "Building",
//                false,
//                true,
//                true,
//                false,
//                entityProperties,
//                segmentProperties,
//                vertexProperties,
//                true);

        return null;
    }


    public static void main(String args[]) {
      EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Set System L&F
                    UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
                } catch (UnsupportedLookAndFeelException e) {
                    // handle exception
                } catch (ClassNotFoundException e) {
                   // handle exception
                } catch (InstantiationException e) {
                   // handle exception
                } catch (IllegalAccessException e) {
                   // handle exception
                }

                I18nManager intl_mgr = I18nManager.getManager();
                intl_mgr.setApplication("ChefX3DSimpleExample",
                                        "config.i18n.chefx3dResources");

                SimpleExample example = new SimpleExample();
            }
        });
    }
}
