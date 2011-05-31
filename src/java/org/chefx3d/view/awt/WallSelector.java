/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package org.chefx3d.view.awt;

// external imports
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.io.InputStream;

import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JComponent;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Point2f;
import javax.vecmath.Vector3f;

// local imports

import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;
import org.j3d.aviatrix3d.VertexGeometry;
import org.j3d.renderer.aviatrix3d.util.AVIntersectionUtils;

/**
 * A panel that shows a SegmentableEntity and allows for wall selection
 * 
 * @author Eric Fickenscher
 * @version $Revision: 1.58 $
 */
public class WallSelector extends JPanel 
    implements 
        MouseListener, 
        ActionListener, 
        EntityPropertyListener {

    /** The path to append to the media hostname param */
    private static final String MEDIA_SERVICE = "/vdst/core/mediaNG";

    /** Default stroke style to use as the brush stroke for the lines */
    private static final BasicStroke DEFAULT_SEGMENT_BRUSH =
        new BasicStroke(10, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);

    /** Half the size of the shape to draw */
    private static final int LINE_THICKNESS = 4;

    /** Default stroke style to use as the brush stroke for the selected rectangled */
    private static final BasicStroke DEFAULT_SELECTED_BRUSH =
        new BasicStroke(LINE_THICKNESS / 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);


    /** SegmentableEntity */
    private SegmentableEntity segmentableEntity;

    /** List of zone entities to display */
    private ArrayList<PositionableEntity> zoneEntityList;

    /** The world model */
    protected WorldModel model;

    /** The CommandController */
    private CommandController controller;

    /** Number of pixels per meter */
    double pixelsPerMeter;

    /** The 'real-world' minimum x-value of the panel */
    double minX;
    
    /** The 'real-world' maximum y-value of the panel */
    double maxY;

    /** The current design entity */
    private ContentContainerEntity contentContainerEntity;

    /** Maps the polygons to entityIDs */
    private HashMap<Shape, Entity> shapeToEntityMap;
    
    /** Is the default selection to be used? */
    private boolean allowDefaultSelection;

    /** The current location selected */
    private LocationEntity locationEntity;

    /** The currently selected entity */
    private Entity currentEntity;

    /** The currently selected zone entity */
    private Entity currentZoneEntity;

    /** The percent of the panel filled by the image of the walls.
     * IE: .8d means the wall image takes up 80% of the panel */
    private double percentPanelUsed;
    
    private Color wallColor;
    private Color selectedColor;
    
    /** The URL to the media service */
    private String mediaService;

    private FileLoader fileLookup;
    
    /** map and entity to a top down buffered image */
    private Map<String, BufferedImage> pathImageMap;

    /** Handler for intersection testing */
    private AVIntersectionUtils iutils;

	/** Utility methods for dealing with entity hierarchies */
	private EntityUtils entityUtils;
		
    /**
     * Constructor
     */
    public WallSelector(WorldModel wm,
                        CommandController cc,
                        ErrorReporter errorReporter, 
                        Color wallColor, 
                        Color selectedColor) {
        model = wm;
        controller = cc;
        this.wallColor = wallColor;
        this.selectedColor = selectedColor;
        
        addMouseListener(this);

        setBorder(BorderFactory.createEmptyBorder());
        
        zoneEntityList = new ArrayList<PositionableEntity>();
        pathImageMap = new HashMap<String, BufferedImage>();
        
        shapeToEntityMap = new HashMap<Shape, Entity>();
        
        currentEntity = null;
        
        percentPanelUsed = 0.8f;
        
        // build the media service URL
        mediaService =
            (String)ApplicationParams.get("mediaHostname") +
            MEDIA_SERVICE;
        
        fileLookup = new FileLoader(1);
        
        iutils = new AVIntersectionUtils();

		entityUtils = new EntityUtils(model);

    }

    //----------------------------------------------------------
    // Methods defined by JComponent
    //----------------------------------------------------------

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
 
        // set up the basic brush stroke to use
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(DEFAULT_SEGMENT_BRUSH);
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(wallColor);

        // configure the basic bounds of the panel
        configureBounds();

        shapeToEntityMap.clear();

        // draw segments and such if the segmentable entity is not null
        if (segmentableEntity != null) {
                        
            // draw all the vertices
            drawVertices(g2d);
            
            // draw all the segments in order
            drawSegments(g2d);
            
        }
          
        // draw all the zone entities 
        drawZoneEntities(g2d);   
        
        // select something
        highlightSelection(g2d);

    }
    
    //----------------------------------------------------------
    // Methods defined by MouseListener
    //----------------------------------------------------------

    /**
     * Invoked when the mouse button has been clicked
     * (pressed and released) on a component.
     * Ignored.
     */
    public void mouseClicked(MouseEvent e) {}

    /**
     * Invoked when the mouse enters a component.
     * Ignored.
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Invoked when the mouse exits a component.
     * Ignored.
     */
    public void mouseExited(MouseEvent e) {}
    
    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent me) {

        // get the mouse click point
        Point mousePos = me.getPoint();

        // look through segment list
        int len = shapeToEntityMap.size();
        Shape[] shapes = new Shape[len];
        shapeToEntityMap.keySet().toArray(shapes);
              
        List<Entity> entitiesFound = new ArrayList<Entity>();
        
        for (int i = 0; i < len; i++) {
            Shape check = shapes[i];
            if (check.contains(mousePos)) {                   
                boolean segments = checkForSegments(check, entitiesFound);
                if (!segments)
                    entitiesFound.add(shapeToEntityMap.get(check));                
            }
        }
        
        // order the list by z-depth
        entitiesFound = orderListByDepth(entitiesFound);
        
        len = entitiesFound.size();
        if (len > 0) {
            
            // if just 1 then we don't need the product menu
            if (len == 1) {
                
                Entity entity = entitiesFound.get(0);
                
                Integer zoneCount = 
                    (Integer)RulePropertyAccessor.getRulePropertyValue(
                            entity, 
                            ChefX3DRuleProperties.PRODUCT_ZONE_COUNT);

                // just 1 zone, so select it
                if (zoneCount > 1) {
                    
                    // we need to create the zone list menu
                    JPopupMenu zoneMenu = new JPopupMenu();                   
                    createZoneMenu(zoneMenu, entity);
                    zoneMenu.show(this, mousePos.x, mousePos.y);                     

                } else if (zoneCount == 1) {
                        
                    // update the current selection variables
                    currentEntity = entity;
                        
                    // find the 1 zone and select it
                    List<Entity> children = entity.getChildren();
                    
                    len = children.size();
                    for (int i = 0; i < len; i++) {
                        Entity child = children.get(i);
                        if (child.isZone() && 
                                child.getType() == Entity.TYPE_MODEL_ZONE) {
                            
                            currentZoneEntity = child;
                            
                            SelectZoneCommand zoneCommand = new SelectZoneCommand(
                                    locationEntity, child.getEntityID());
                            controller.execute(zoneCommand);
                        }
                    }
                    
                    repaint();

                } else {
                    
                    changeSelection(entity);

                }
                
                // unselect everything
                EntitySelectionHelper.getEntitySelectionHelper().clearSelectedList();
                                               
            } else {
                
                // we need to create product and zone menus
                
                JPopupMenu zoneMenu = new JPopupMenu();                
                
                for (int i = 0; i < len; i++) {
                    
                    Entity child = entitiesFound.get(i);
                                       
                    JMenu productMenu = new JMenu(child.getName());   
                             
                    StringBuilder builder = new StringBuilder();
                    builder.append(mediaService);
                    builder.append("/media/");
                    builder.append(child.getToolID());
                    builder.append("/");
                    builder.append(child.getToolID());         
                    builder.append("_zone");       
                    String imagePath = builder.toString();

                    BufferedImage productImage = getImage(imagePath);
                                       
                    if (productImage != null) {
                        productImage = scalePretty (productImage, 50, 50);
                        productMenu.setIcon(new ImageIcon(productImage));
                    }

                    createZoneMenu(productMenu, child);
                    
                    zoneMenu.add(productMenu);
                }
                
                zoneMenu.show(this, mousePos.x, mousePos.y);      
                                
            }
                        
        } else {
            
            // send the default selection only if it is allowed
            if (allowDefaultSelection) {
                
                Entity defaultSelection =
                    locationEntity.getDefaultSelectionEntity();

                changeSelection(defaultSelection);
                
            }

        }
        
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * Ignored.
     */
    public void mouseReleased(MouseEvent e) {}
          
    //  ----------------------------------------------------------
    //  Methods implemented by ActionListener
    //  ----------------------------------------------------------

    /**
     * Invoked when an action event occurs
     */
    public void actionPerformed(ActionEvent e) {
        
        if (e.getSource() instanceof JMenuItem) {
            SelectZoneMenuAction zoneAction = 
                (SelectZoneMenuAction)((JMenuItem)e.getSource()).getAction();
                        
            // update the current selection variables
            currentZoneEntity = zoneAction.getEntity();
            currentEntity = zoneAction.getParent();

            //Need to repaint the component
            repaint();

        }
        
    }

    //  ----------------------------------------------------------
    //  Methods implemented by EntityPropertyListener
    //  ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this property update an ongoing change like a transient position or the final value
     */
    public void propertyUpdated(int entityID,
            String propertySheet, String propertyName, boolean ongoing) {

        if (propertyName.equals(PositionableEntity.POSITION_PROP) || 
                propertyName.equals(PositionableEntity.ROTATION_PROP)) {
            
            repaint();
  
        } else if (propertyName.equals(LocationEntity.ACTIVE_ZONE_PROP)) {
                       
            Entity entity = model.getEntity(locationEntity.getActiveZoneID());
            if (entity.getType() == Entity.TYPE_MODEL_ZONE) {
                currentEntity =  model.getEntity(entity.getParentEntityID());
                currentZoneEntity = entity;
            } else {
                currentEntity = entity;
            }
            
            repaint();
        }
 
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        // ignored
    }
    
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Calls the SelectEntityCommand for the new selected entity, or entities
     * @param list a list of entities selected
     */
    public void changeSelection(Entity entity){
                    
        // update the current selection variables
        currentEntity = entity;

        //Need to repaint the component
        repaint();

        // send out notification
        SelectZoneCommand zoneCommand = 
            new SelectZoneCommand(locationEntity, currentEntity.getEntityID());
        controller.execute(zoneCommand);
        
    }

    /**
     * @return the allowDefaultSelection
     */
    public boolean isAllowDefaultSelection() {
        return allowDefaultSelection;
    }

    /**
     * @param allowDefaultSelection the allowDefaultSelection to set
     */
    public void setAllowDefaultSelection(boolean allowDefaultSelection) {
        this.allowDefaultSelection = allowDefaultSelection;
    }

    /**
     * Update the location entity being used, looks for the segmentable entity 
     * and repaints the window.
     * 
     * @param locationEntity
     */
    public void setLocationEntity(LocationEntity locationEntity) {
        
        this.locationEntity = locationEntity;
        this.locationEntity.addEntityPropertyListener(this);
        
        // get the content container
        contentContainerEntity =
            locationEntity.getContentContainerEntity();

        // loop through children for a multi-segment
        int len = contentContainerEntity.getChildCount();
        for (int i = 0; i < len; i++) {
            Entity child = contentContainerEntity.getChildAt(i);

            if (child == null)
                continue;
            
            if (child instanceof SegmentableEntity) {
                
                segmentableEntity = (SegmentableEntity)child;
                currentEntity = null;
                
            } else {
                
                // clear the list
                zoneEntityList.clear();
                
                // check for zones
                ArrayList<Entity> children = child.getChildren();
                
                for (int j = 0; j < children.size(); j++) {
                    
                    child = children.get(j);
              
                    Integer zoneCount = 
                        (Integer)RulePropertyAccessor.getRulePropertyValue(
                                child, 
                                ChefX3DRuleProperties.PRODUCT_ZONE_COUNT);

                    if (zoneCount > 0) {

                        // listen for movement events
                        child.addEntityPropertyListener(this);
                        
                        // add to list to draw
                        zoneEntityList.add((PositionableEntity)child);
                        
                    }
                    
                }

            }
            
        }
        
        repaint();
        
    }
    
    /**
     * Add an entity zone to the list of zones to display
     * 
     * @param zoneEntity An entity that has zones
     */
    public void addEntityZone(PositionableEntity zoneEntity) {
        
        // listen for movement events
        zoneEntity.addEntityPropertyListener(this);
        
        // add to list to draw
        zoneEntityList.add(zoneEntity);
                
        repaint();
    }

    /**
     * Remove an entity zone from the list of those displayed
     * 
     * @param zoneEntity An entity that has zones
     */
    public void removeEntityZone(PositionableEntity zoneEntity) {
        
        // don't listen for movement events
        zoneEntity.removeEntityPropertyListener(this);
        
        // remove from list to draw
        zoneEntityList.remove(zoneEntity);
        
        // deal with the current zone being deleted
        if (currentEntity == zoneEntity) {           
            currentEntity = null;                       
        }
        
        repaint();
        
    }
    
    /**
     * Perform a bubble sort using the entity's z-depth position data.  Will 
     * return a list of highest to lowest.  will ignore any non-positionable 
     * entities.
     * 
     * @param entityList the list the check
     * @return the sorted version of the list
     */
    private List<Entity> orderListByDepth(List<Entity> entityList) {
        
        // first we only can check positonable items
        List<PositionableEntity> positionableList = new ArrayList<PositionableEntity>();
        for (int i = 0; i < entityList.size(); i++) {
            Entity entity = entityList.get(i);
            if (entity instanceof PositionableEntity) {
                positionableList.add((PositionableEntity)entity);
            }           
        }
 
        // if there are no valid entities then just stop trying to sort them
        int len = positionableList.size();
        if (len <= 0) {
            return entityList;
        }

        PositionableEntity[] entities = new PositionableEntity[len];
        positionableList.toArray(entities);
 
        // now perform a bubble sort based on the z-depth.  this will put the
        // highest item first and the lowest item last.
        boolean doMore = true;
        while (doMore) {
            // assume this is last pass over array
            doMore = false;  
            for (int i = 0; i < entities.length - 1; i++) {
                    
                // get the positions relative to the zone
                double[] pos1 = 
					entityUtils.getPositionRelativeToZone(entities[i]);
                double[] pos2 = 
					entityUtils.getPositionRelativeToZone(entities[i+1]);
                
                if (pos1 != null && pos2 != null) {
                    // compare the heights
                    if (pos1[2] < pos2[2]) {
                        // exchange elements
                        PositionableEntity temp = entities[i];  
                        entities[i] = entities[i+1];  
                        entities[i+1] = temp;
                        // after an exchange, must look again 
                        doMore = true;  
                    }
                }
            }
        }

        // finally put back together a list to return
        List<Entity> returnList = new ArrayList<Entity>();
        for (int i = 0; i < entities.length; i++) {
            returnList.add(entities[i]);
        }

        return returnList;
    }
    
    /**
     * Determine the pixel per meter and min X and max Y values.
     * 
     * @param vertexList
     * @return True if successful, false otherwise
     */
    private void configureBounds() {
        
        // TODO: need a better way of determining the bounds if there are 
        // no walls
        
        pixelsPerMeter = 
            (getWidth() < getHeight()) ? getWidth() : getHeight();

        minX = (getWidth() / 2) / pixelsPerMeter;
        maxY = (getHeight() / 2) / pixelsPerMeter;

        // define some working variables
        double worldPosMinX = 0;
        double worldPosMaxX = 0;
        double worldPosMinY = 0;
        double worldPosMaxY = 0;
        float[] min = new float[3];
        float[] max = new float[3];
        float[] extents = new float[6];
      
        // the list of all bounds used to determine pixels per meter
        ArrayList<Entity> zoneList = new ArrayList<Entity>();
        
        // use the wall bounds to determine the pixels per meter
        if (segmentableEntity != null) {

            // store for use
            zoneList.add(segmentableEntity);

        }
        
        // use the product zone items to determine the pixels per meter
        zoneList.addAll(zoneEntityList);
        
        // if we found no zones then just return with the default value
        // already calculated
        int len = zoneList.size();
        if (len == 0) 
            return;
        
        // get the first value and set the min and max values with it
        int index = 0;
        boolean found = false;
        while (!found) {
            
            if (index >= len)
                break;

            Entity entity = zoneList.get(index); 
            if (entity instanceof SegmentableEntity) {
                
                double[] wallBounds = ((SegmentableEntity)entity).getBounds();
                if (wallBounds != null) {
                    
                    worldPosMinX = wallBounds[0];
                    worldPosMaxX = wallBounds[1];
                    worldPosMinY = wallBounds[2];
                    worldPosMaxY = wallBounds[3];
                    
                    found = true;
                    
                } 
                
            } else if (entity instanceof PositionableEntity) {
                
                extents = getWorldExtents((PositionableEntity)entity);
                
                worldPosMinX = extents[0];
                worldPosMaxX = extents[1];
                worldPosMinY = extents[2];
                worldPosMaxY = extents[3];

                found = true;
                
            }
            
            index++;

        }
        
        // loop through the remaining items and adjust the min and max
        for (int i = index; i < len; i++) { 
            
            // the remaining items can only be product zones
            PositionableEntity posEntity = (PositionableEntity)zoneList.get(i);             
            extents = getWorldExtents(posEntity);
                        
            if (extents[0] < worldPosMinX) {
                worldPosMinX = extents[0];
            }
            if (extents[1] > worldPosMaxX) {
                worldPosMaxX = extents[1];
            }
            if (extents[2] < worldPosMinY) {
                worldPosMinY = extents[2];
            }
            if (extents[3] > worldPosMaxY) {
                worldPosMaxY = extents[3];
            }
                        
        }
     
        // calculate the total width and height of all zones includes
        double totalWidth  = worldPosMaxX - worldPosMinX;
        if (totalWidth == 0) {
            totalWidth = 1;
        }
        double totalHeight = worldPosMaxY - worldPosMinY;
        if (totalHeight == 0) {
            totalHeight = 1;
        }

        // calculate the scalar values
        double widthScalar = (getWidth() * percentPanelUsed)  / totalWidth;
        double heightScalar = (getHeight() * percentPanelUsed) / totalHeight;

        //
        // we want to use the smaller value, otherwise the segmentEntity
        // would scale outside the bounds of the panel, yaknow?
        pixelsPerMeter = 
            (heightScalar < widthScalar) ? heightScalar : widthScalar;

        double panelWidth =  getWidth() / pixelsPerMeter;
        double panelHeight = getHeight() / pixelsPerMeter;

        minX = worldPosMinX - ((panelWidth - totalWidth) / 2);
        maxY = worldPosMaxY + ((panelHeight - totalHeight) / 2);

        
    }
    
    /**
     * Draw all the vertices
     * 
     * @param g2d
     * @param vertexList
     */
    private void drawVertices(Graphics2D g2d) {
        
        ArrayList<VertexEntity> vertexList = segmentableEntity.getVertices();
        
        if (vertexList == null)
            return;
        
        //
        // Draw all vertices
        //
        for (int i = 0; i < vertexList.size(); i++) {

            // get the location of the start of the segment
            VertexEntity vertex = vertexList.get(i);
            double[] vertexWorldPos = new double[3];
            vertex.getPosition(vertexWorldPos);

            // convert world coordinates to panel
            // coordinates with pixelsPerMeter
            int x = (int)Math.round((vertexWorldPos[0] - minX) *
                                    pixelsPerMeter);
            int y = (int)Math.round((maxY - vertexWorldPos[1]) *
                                    pixelsPerMeter);

            // use the panelToMapAdjustment to convert the map
            // coordinates to panel coordinates    
            g2d.fillRect(x - LINE_THICKNESS,
                       y - LINE_THICKNESS,
                       LINE_THICKNESS * 2,
                       LINE_THICKNESS * 2);
        }

    }
    
    /**
     * Draw all the segments for the wall
     * 
     * @param g2d
     * @param segmentList
     */
    private void drawSegments(Graphics2D g2d) {
        
        ArrayList<SegmentEntity> segmentList = segmentableEntity.getSegments();     
        
        if (segmentList == null)
            return;

        //
        // Draw all segments
        //
        int len = segmentList.size();
        for (int i = 0; i < len; i++) {

            SegmentEntity segment = segmentList.get(i);

            // get the location of the start of the segment
            VertexEntity startVertex = segment.getStartVertexEntity();

            double[] segmentStartWorldPos = new double[3];
            startVertex.getPosition(segmentStartWorldPos);

            // get the location of the end of the segment
            VertexEntity endVertex = segment.getEndVertexEntity();

            if (endVertex == null)
                continue;

            double[] segmentEndWorldPos = new double[3];
            endVertex.getPosition(segmentEndWorldPos);

            // convert world coordinates to panel coordinates with pixelsPerMeter
            int segmentStartX = (int)Math.round(
                (segmentStartWorldPos[0] - minX ) * pixelsPerMeter);
            int segmentStartY = (int)Math.round(
                (maxY - segmentStartWorldPos[1] ) * pixelsPerMeter );

            int segmentEndX = (int)Math.round(
                (segmentEndWorldPos[0] - minX ) * pixelsPerMeter );
            int segmentEndY = (int)Math.round(
                (maxY - segmentEndWorldPos[1] ) * pixelsPerMeter );

            Polygon line = new Polygon();

            float a = segmentStartX - segmentEndX;
            float b = segmentStartY - segmentEndY;

            // create a direction vector
            Vector2f direction = new Vector2f(a, b);
            direction.normalize();

            // rotate it 90 degrees
            b = -direction.x;
            a = direction.y;

            // get the point
            float x = segmentStartX + (LINE_THICKNESS * a);
            float y = segmentStartY + (LINE_THICKNESS * b);

            line.addPoint(Math.round(x), Math.round(y));

            x = segmentEndX + (LINE_THICKNESS * a);
            y = segmentEndY + (LINE_THICKNESS * b);

            line.addPoint(Math.round(x), Math.round(y));

            // rotate it 90 degrees
            b = direction.x;
            a = -direction.y;

            x = segmentEndX + (LINE_THICKNESS * a);
            y = segmentEndY + (LINE_THICKNESS * b);

            line.addPoint(Math.round(x), Math.round(y));

            x = segmentStartX + (LINE_THICKNESS * a);
            y = segmentStartY + (LINE_THICKNESS * b);

            line.addPoint(Math.round(x), Math.round(y));

            // draw the polygon
            g2d.setColor(wallColor);
            g2d.fillPolygon(line);

            // store polygon and id for finding later
            shapeToEntityMap.put(line, segment);
            
        }
        
    }
    
    /**
     * Draw all the zone entities
     * 
     * @param g2d
     */
    private void drawZoneEntities(Graphics2D g2d) {
        
        //
        // Draw all zone entities
        //
        int len = zoneEntityList.size();    
        
        // convert to an array so we can be sure the length is correct
        PositionableEntity[] list = new PositionableEntity[len];
        zoneEntityList.toArray(list);
                
        for (int i = 0; i < len; i++) {

            PositionableEntity posEntity = list[i];
            
            if (posEntity == null)
                continue;
            
            // get the position
            double[] worldPos = 
				entityUtils.getPositionRelativeToZone(posEntity);
                        
            // get the bounds
            float[] bounds = new float[6];
            posEntity.getBounds(bounds);
  
            // get the rotation
            float[] rotation = new float[4];
            posEntity.getRotation(rotation);

            // calculate the size
            float[] size = new float[3];
            size[0] = bounds[1] - bounds[0];
            size[1] = bounds[3] - bounds[2];
            size[2] = bounds[5] - bounds[4];
            
            // get the upper left of the bounds
            double[] upperLeftPos = new double[2];
            upperLeftPos[0] = worldPos[0] + bounds[0];
            upperLeftPos[1] = worldPos[1] - bounds[2];

            // convert world coordinates to panel
            // coordinates with pixelsPerMeter
            int x = (int)Math.round((upperLeftPos[0] - minX) * pixelsPerMeter);
            int y = (int)Math.round((maxY - upperLeftPos[1]) * pixelsPerMeter);

            // convert the width and height to pixelsPerMeter
            int width = (int)Math.round(size[0] * pixelsPerMeter);
            int height = (int)Math.round(size[1] * pixelsPerMeter);
            
            String toolID = posEntity.getToolID();
            
            // create the URL to lookup the image
            StringBuilder builder = new StringBuilder();
            builder.append(mediaService);
            builder.append("/media/");
            builder.append(toolID);
            builder.append("/");
            builder.append(toolID);         
            builder.append("_");       
            builder.append("topdown_zoneIcon");       
            String imagePath = builder.toString();

            // get the image file
            BufferedImage image = getImage(imagePath);
                 
            // create the un-rotated object
            Rectangle rect = new Rectangle(x, y, width, height);

            // setup the transform needed for the rotation
            AffineTransform at = new AffineTransform();
            at.rotate(-rotation[3], x + width * 0.5, y + height * 0.5);

            // create the final shape
            Shape shape = at.createTransformedShape(rect);
            
            shapeToEntityMap.put(shape, posEntity);
            
            // use the icon
            if (image != null) {
                
                // scale to correct size
                image = scalePretty(image, width, height);

                // setup the transform needed for the rotation
                at = new AffineTransform();

                // move it to the right spot
                at.translate(x, y);
                
                // rotate it
                at.rotate(-rotation[3], width * 0.5, height * 0.5);
                                   
                // draw it on screen
                g2d.drawImage(image, at, null);
                                    
            } else {
                               
                // create the buffered image in memory
                image = (BufferedImage)createImage(width, height);
                
                // get the graphics context
                Graphics2D gc = image.createGraphics();
                
                // set the brush stoke
                gc.setStroke(DEFAULT_SEGMENT_BRUSH);
                gc.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                gc.setColor(wallColor);

                // fill and dispose
                rect = new Rectangle(0, 0, width, height);
                Shape shape1 = at.createTransformedShape(rect);
                
                gc.fill(shape1);
                gc.dispose();               
                                        
                // retain the image for continued use
                pathImageMap.put(imagePath, image);
               
                // create the rectangle to represent the product with zones, 
                // this will be replaced next repaint by the image version
                // stored in the image map
                g2d.fill(shape);

            }           
        }
    }
    
    /**
     * Draw a box around the currently selected wall
     * @param g2d
     * @param segmentList
     */
    private void highlightSelection(Graphics2D g2d) {

        // set up brush stroke
        g2d.setStroke(DEFAULT_SELECTED_BRUSH);
        g2d.setColor(selectedColor);
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentEntity instanceof SegmentEntity) {
            selectSegment(g2d, (SegmentEntity)currentEntity);
        } else if (currentEntity == null || currentEntity instanceof ZoneEntity) {
            selectFloorZone(g2d);   
        } else {
            selectZone(g2d, (PositionableEntity)currentEntity);
        }
        
    }
    
    /**
     * Logic to select a particular segment.  Places a dot on the side 
     * of the segment that is facing to the user
     * 
     * @param g2d the graphics context to draw to
     * @param segment the segment to select
     */
    private void selectSegment(Graphics2D g2d, SegmentEntity segment) {
        
        // get the location of the start of the segment
        VertexEntity startVertex = segment.getStartVertexEntity();

        double[] segmentStartWorldPos = new double[3];
        startVertex.getPosition(segmentStartWorldPos);

        // get the location of the end of the segment
        VertexEntity endVertex = segment.getEndVertexEntity();

        double[] segmentEndWorldPos = new double[3];
        endVertex.getPosition(segmentEndWorldPos);

        // convert world coordinates to panel coordinates with pixelsPerMeter
        int segmentStartX = (int)Math.round(
            (segmentStartWorldPos[0] - minX ) * pixelsPerMeter );
        int segmentStartY = (int)Math.round(
            (maxY - segmentStartWorldPos[1] ) * pixelsPerMeter );

        int segmentEndX = (int)Math.round(
            (segmentEndWorldPos[0] - minX ) * pixelsPerMeter );
        int segmentEndY = (int)Math.round(
            (maxY - segmentEndWorldPos[1] ) * pixelsPerMeter );

        // now define the selection box
        Polygon selectionBox = new Polygon();

        float a = segmentStartX - segmentEndX;
        float b = segmentStartY - segmentEndY;
        float scalar = 3f;
        // create a direction vector
        Vector2f direction = new Vector2f(a, b);
        direction.normalize();

        float forwardX = direction.x * (LINE_THICKNESS + 7);
        float forwardY = direction.y * (LINE_THICKNESS + 7);

        // rotate it 90 degrees
        b = -direction.x;
        a = direction.y;

        // get the point
        float x = segmentStartX + (scalar * LINE_THICKNESS * a) + forwardX;
        float y = segmentStartY + (scalar * LINE_THICKNESS * b) + forwardY;

        selectionBox.addPoint(Math.round(x), Math.round(y));

        float x1 = segmentEndX + (scalar * LINE_THICKNESS * a) - forwardX;
        float y1 = segmentEndY + (scalar * LINE_THICKNESS * b) - forwardY;

        selectionBox.addPoint(Math.round(x1), Math.round(y1));
        
        // create a vector to the mid point of the segment
        Vector2f mp = new Vector2f(x1 - x, y1 - y);
        mp.scale(0.5f);
        
        // add the calculated vector to the start point
        Point2f start = new Point2f(x, y);
        start.add(mp);
        
        // now draw a dot to indicate a wall facing
        g2d.fillOval(
                Math.round(start.x - LINE_THICKNESS), 
                Math.round(start.y - LINE_THICKNESS), 
                LINE_THICKNESS * 2, 
                LINE_THICKNESS * 2);

        // rotate it 90 degrees
        b = direction.x;
        a = -direction.y;

        x = segmentEndX + (scalar * LINE_THICKNESS * a) - forwardX;
        y = segmentEndY + (scalar * LINE_THICKNESS * b) - forwardY;

        selectionBox.addPoint(Math.round(x), Math.round(y));

        x = segmentStartX + (scalar * LINE_THICKNESS * a) + forwardX;
        y = segmentStartY + (scalar * LINE_THICKNESS * b) + forwardY;

        selectionBox.addPoint(Math.round(x), Math.round(y));
        g2d.drawPolygon(selectionBox);
        
    }
    
    /**
     * Logic to select a product zone.  Draws a line around the product.
     * 
     * @param g2d the graphics context to draw to
     * @param entity the entity to select
     */
    private void selectZone(Graphics2D g2d, PositionableEntity entity) {
       
        if (entity == null) {
            selectFloorZone(g2d);
            return;
        }          

        float scale = 0.07f;
        
        // get the position
        double[] worldPos = 
            //RulePositionUtils.getPositionRelativeToZone(model, entity);
			entityUtils.getPositionRelativeToZone(entity);
                           
        // get the bounds
        float[] bounds = new float[6];
        entity.getBounds(bounds);
        
        // adjust the bounds to be slightly larger
        bounds[0] -= scale;
        bounds[1] += scale;
        bounds[2] -= scale;
        bounds[3] += scale;
        bounds[4] -= scale;
        bounds[5] += scale;

        // get the rotation
        float[] rotation = new float[4];
        entity.getRotation(rotation);

        // calculate the size
        float[] size = new float[3];
        size[0] = bounds[1] - bounds[0];
        size[1] = bounds[3] - bounds[2];
        size[2] = bounds[5] - bounds[4];

        // get the upper left of the bounds
        double[] upperLeftPos = new double[2];
        upperLeftPos[0] = worldPos[0] + bounds[0];
        upperLeftPos[1] = worldPos[1] - bounds[2];

        // convert world coordinates to panel
        // coordinates with pixelsPerMeter
        int x = (int)Math.round((upperLeftPos[0] - minX) * pixelsPerMeter);
        int y = (int)Math.round((maxY - upperLeftPos[1]) * pixelsPerMeter);

        // convert the width and height to pixelsPerMeter
        int width = (int)Math.round(size[0] * pixelsPerMeter);
        int height = (int)Math.round(size[1] * pixelsPerMeter);
                     
        // create the un-rotated object
        Rectangle rect = new Rectangle(x, y, width, height);

        // setup the transform needed for the rotation
        AffineTransform at = new AffineTransform();
        
        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;
        at.rotate(-rotation[3], centerX, centerY);

        // create the final shape
        Shape shape = at.createTransformedShape(rect);

        // create the rectangle to represent the zone
        g2d.draw(shape);
        
        // get the index to look up       
        String[] zoneNames = 
            (String[])RulePropertyAccessor.getRulePropertyValue(
                    entity, 
                    ChefX3DRuleProperties.PRODUCT_ZONE_NAMES);
        int index = -1;
        for (int i = 0; i < zoneNames.length; i++) {
            if (zoneNames[i].equals(currentZoneEntity.getName())) {
                index = i;
                break;
            }
        }
        
        // get the zone normal and position
        if (index >= 0) {
                        
            float[] zoneXPos = (float[]) 
            RulePropertyAccessor.getRulePropertyValue(
                    entity, 
                    ChefX3DRuleProperties.PRODUCT_ZONE_POINT_X);
        
            float[] zoneYPos = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                        entity, 
                        ChefX3DRuleProperties.PRODUCT_ZONE_POINT_Y);
            
            float[] zoneZPos = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                        entity, 
                        ChefX3DRuleProperties.PRODUCT_ZONE_POINT_Z);
            
            float[] zoneXNormal = (float[]) 
            RulePropertyAccessor.getRulePropertyValue(
                    entity, 
                    ChefX3DRuleProperties.PRODUCT_ZONE_NORMAL_X);
        
            float[] zoneYNormal = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                        entity, 
                        ChefX3DRuleProperties.PRODUCT_ZONE_NORMAL_Y);
            
            float[] zoneZNormal = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                        entity, 
                        ChefX3DRuleProperties.PRODUCT_ZONE_NORMAL_Z);

            // cast ray from center of product with zones.  If the zone is
            // defined outside the bounds using the center of the zone will 
            // hit the wrong side of the bounds
            float[] pos = new float[] {0, 0, 0};
//            pos[0] = zoneXPos[index];
//            pos[1] = zoneYPos[index];
//            pos[2] = zoneZPos[index];

            float[] normal = new float[3];
            normal[0] = zoneXNormal[index];
            normal[1] = zoneYNormal[index];
            normal[2] = zoneZNormal[index];
            
            // create a dummy box 
            org.j3d.renderer.aviatrix3d.geom.Box box = new 
                org.j3d.renderer.aviatrix3d.geom.Box(size[0], size[1], size[2]);
            
            VertexGeometry geom = (VertexGeometry)box.getGeometry();

            Matrix4f mtx = new Matrix4f();
            mtx.setIdentity();
                        
            Point3f intersectPoint = new Point3f();
            Point3f origin = new Point3f(pos);
            Vector3f direction = new Vector3f(normal);
            
            //determine if there was an actual geometry intersection
            boolean intersect = iutils.rayUnknownGeometry(
                origin,
                direction,
                0,
                geom,
                mtx,
                intersectPoint,
                false);
            
            if (intersect) {
            
                // convert world coordinates to panel
                // coordinates with pixelsPerMeter
                int x1 = (int)Math.round(centerX + (intersectPoint.x * pixelsPerMeter));
                int y1 = (int)Math.round(centerY + (-intersectPoint.y * pixelsPerMeter));
     
                // now draw a dot to indicate a zone facing
                g2d.rotate(-rotation[3], centerX, centerY);
                g2d.fillOval(
                        Math.round(x1 - LINE_THICKNESS), 
                        Math.round(y1 - LINE_THICKNESS), 
                        LINE_THICKNESS * 2, 
                        LINE_THICKNESS * 2);
                g2d.rotate(rotation[3], centerX, centerY);
                
            }
        }
    }
    
    /**
     * Logic to select the floor.  This is just a line around the border of 
     * the panel.
     * 
     * @param g2d the graphics context to draw to
     */
    private void selectFloorZone(Graphics2D g2d) {
        
        int minX = (int)Math.round(getWidth() * 0.05);
        int minY = (int)Math.round(getHeight() * 0.05);
        int maxX = (int)Math.round(getWidth() * 0.95);
        int maxY = (int)Math.round(getHeight() * 0.95);
        
        Polygon floorSelected = new Polygon();

        floorSelected.addPoint(minX, minY);
        floorSelected.addPoint(minX, maxY);
        floorSelected.addPoint(maxX, maxY);
        floorSelected.addPoint(maxX, minY);
        
        g2d.drawPolygon(floorSelected);

    }

    /**
     * The the extents of the entities.  This gets the position relative
     * the the zone and then adds the bounds of the item.
     * 
     * @param posEntity
     * @return array of extents: minx, maxx, miny, maxy, minz, and maxz
     */
    private float[] getWorldExtents(PositionableEntity posEntity) {
               
        // get the position
        double[] worldPos = 
            entityUtils.getPositionRelativeToZone(posEntity);
                    
        // get the bounds
        float[] bounds = new float[6];
        posEntity.getBounds(bounds);

        // calculate the extents
        float[] extents = new float[6];
        extents[0] = (float)worldPos[0] + bounds[0];
        extents[1] = (float)worldPos[0] + bounds[1];
        extents[2] = (float)worldPos[1] + bounds[2];
        extents[3] = (float)worldPos[1] + bounds[3];
        extents[4] = (float)worldPos[2] + bounds[4];
        extents[5] = (float)worldPos[2] + bounds[5];
        
        return extents;
        
    }

    /**
     * Scale an image using the most pretty method.
     *
     * @param image The image to scale
     * @param newWidth The new width
     * @param newHeight The new height
     */
    private BufferedImage scalePretty(BufferedImage image, int newWidth,
        int newHeight) {

        java.awt.Image rimg = image.getScaledInstance(
            newWidth,
            newHeight,
            java.awt.Image.SCALE_AREA_AVERAGING);

        boolean hasAlpha = image.getColorModel().hasAlpha();

        BufferedImage ret_image = null;
        // Not sure why .getType doesn't work right for this
        if (hasAlpha) {
            ret_image = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        } else {
            ret_image = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        }
        java.awt.Graphics2D g2 = ret_image.createGraphics();
        g2.drawImage(rimg, 0, 0, null);

        return(ret_image);
    }

    /**
     * Create a menu item and add it to the component provided
     */
    private void createZoneMenu(JComponent menu, Entity entity){
                       
        List<Entity> children = entity.getChildren();
        
        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity child = children.get(i);
            if (child.isZone() && child.getType() == Entity.TYPE_MODEL_ZONE) {
                        
                String toolID = entity.getToolID();
                
                StringBuilder builder = new StringBuilder();
                builder.append(mediaService);
                builder.append("/media/");
                builder.append(toolID);
                builder.append("/");
                builder.append(toolID);         
                builder.append("_");       
                builder.append(child.getName());
                builder.append("_zoneIcon");       
                String imagePath = builder.toString();

                BufferedImage zoneImage = getImage(imagePath);
                
                // create the action
                SelectZoneMenuAction action = new SelectZoneMenuAction(
                        child.getName(),
                        null,
                        entity, 
                        child, 
                        locationEntity, 
                        controller);
                
                // create the ui element
                JMenuItem menuItem = new JMenuItem(action);
                
                if (zoneImage != null) {                    
                    zoneImage = scalePretty (zoneImage, 50, 50);
                    menuItem.setIcon(new ImageIcon(zoneImage));
                }
                
                menuItem.addActionListener(this);
                menu.add(menuItem);

            }
            
        }

    }
    
    /**
     * Get the image that represents the entity
     * 
     * @param entity the entity requested
     * @return
     */
    private BufferedImage getImage(String imagePath) {
        
        // check the media service for a top down icon, if it doesn't
        // exist then just use the entity's bounds
        BufferedImage image = pathImageMap.get(imagePath);
        
        if (image == null) {
            
            Object[] fileURL;
            try {
                fileURL = fileLookup.getFileURL(imagePath);
            } catch (IOException ioe) {
                fileURL = new Object[4];
            }
            
            if (fileURL[1] != null && fileURL[1] instanceof InputStream) {
                try {
                    
                    // create the buffered image in memory
                    image = javax.imageio.ImageIO.read((InputStream)fileURL[1]);
                                            
                    // retain the image for continued use
                    pathImageMap.put(imagePath, image);
                    
                } catch (IOException ioe) {
                    
                }              
                
            } 
            
        }
        
        return image;
        
    }
    
    /**
     * Only allow the selection of a single wall
     * 
     * @param check The shape found to be selected
     * @param entitiesFound The current list of selected zones
     * @return True if a segment is already selected, false otherwise
     */
    private boolean checkForSegments(Shape check, List<Entity> entitiesFound) {
        
        Entity entity = shapeToEntityMap.get(check);
        if (entity.getType() == Entity.TYPE_SEGMENT) {
            
            Iterator<Entity> itr = entitiesFound.iterator();
            while (itr.hasNext()) {
                Entity inspect = itr.next();
                if (inspect.getType() == Entity.TYPE_SEGMENT) {
                    return true;
                }
            }
        }
        
        return false;
        
    }

}