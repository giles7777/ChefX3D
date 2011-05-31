/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.gt2d;

// External Imports
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.util.ArrayList;

import java.awt.geom.AffineTransform;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.DefaultBuildingEntity;
import org.chefx3d.model.EntitySelectionHelper;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.ListProperty;
import org.chefx3d.tool.BuildingTool;

/**
 * Renders a vertex or set of vertices to screen, with the option of custom
 * colouring.
 *
 * @author Russell Dodds
 * @version $Revision: 1.14 $
 */
public class BuildingToolRenderer extends AbstractToolRenderer {

    /** Debugging flag to show the area that is responsible for being selected */
    private static final boolean DISPLAY_SELECTION_AREAS = false;

    /** The default width of the renderer in pixels */
    private static final int DEFAULT_WIDTH = 64;

    /** The default height of the renderer in pixels */
    private static final int DEFAULT_HEIGHT = 64;

    /** Pixel size of a vertex box */
    private static final int VERTEX_PIXEL_SIZE = 5;

    /** Line thickness when we draw the selected segment highlight */
    private static final int SEGMENT_LINE_THICKNESS = 2;

    private static final Color DEFAULT_VERTEX_COLOR = Color.RED;
    private static final Color DEFAULT_HIGHLIGHT_COLOR = Color.YELLOW;

    private static final Color DEFAULT_DOOR_COLOR = Color.LIGHT_GRAY;
    private static final Color DEFAULT_WALL_COLOR = Color.BLACK;
    private static final Color DEFAULT_WINDOW_COLOR = Color.GRAY;

    /** The vertex highlight color */
    private Color highlightColor;

    /** The vertex color */
    private Color vertexColor;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper seletionHelper;

    /** Stroke representing the selected segment drawing */
    private static final BasicStroke SEGMENT_STROKE =
        new BasicStroke(SEGMENT_LINE_THICKNESS,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND);

    /** Stroke representing the selected segment drawing */
    private static final BasicStroke SELECTED_SEGMENT_STROKE =
        new BasicStroke(SEGMENT_LINE_THICKNESS,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND);

    /**
     * Contruct a default instance with default rendering colours.
     */
    public BuildingToolRenderer() {
        this(DEFAULT_WALL_COLOR, DEFAULT_VERTEX_COLOR, DEFAULT_HIGHLIGHT_COLOR);
    }

    /**
     * Contruct an instance with a specified set of colours.
     *
     * @param lineColor The line color
     * @param vertexColor The vertex color
     * @param highlightColor The vertex highlight color
     */
    public BuildingToolRenderer(Color lineColor,
                              Color vertexColor,
                              Color highlightColor) {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        this.highlightColor = highlightColor;
        this.vertexColor = vertexColor;
        
        seletionHelper = EntitySelectionHelper.getEntitySelectionHelper();
    }

    //----------------------------------------------------------
    // Methods required by ToolRenderer
    //----------------------------------------------------------

    /**
     * Draw the entity icon to the screen
     *
     * @param g2d The graphics object used to draw
     * @param eWrapper The wrapped entity to draw
     */
    public void draw(Graphics2D g2d, EntityWrapper eWrapper) {

        Color origColor = g2d.getColor();
        AffineTransform defaultTransform = g2d.getTransform();

        int screenStart[] = new int[2];
        int screenEnd[] = new int[2];

        Entity entity = eWrapper.getEntity();
        if (entity instanceof SegmentableEntity) {

            SegmentableEntity segmentEntity = (SegmentableEntity)entity;

            // get the segments & vertices
            ArrayList<SegmentEntity> segmentList = segmentEntity.getSegments();
            ArrayList<VertexEntity> vertices = segmentEntity.getVertices();

            // get the base position
            double[] entityPos = new double[3];
            int screenEntity[] = new int[2];

            float[] entityRot = new float[4];
            if (entity instanceof PositionableEntity) {
                ((PositionableEntity)entity).getPosition(entityPos);
                ((PositionableEntity)entity).getRotation(entityRot);

                eWrapper.convertWorldPosToScreenPos(entityPos, screenEntity);
            }

            g2d.translate(-screenEntity[0], -screenEntity[1]);


            Polygon building = new Polygon();

            // use the segments to create the background polygons
            int len = segmentList.size();
            for (int i = 0; i < len; i++) {

                SegmentEntity segment = segmentList.get(i);

//System.out.println("    segment.isExteriorSegment(): " + segment.isExteriorSegment());

                if (segment.isExteriorSegment()) {

                    // get the location of the start of the segment
                    int startVertexID = segment.getStartID();
                    VertexEntity startVertex = segmentEntity.getVertex(startVertexID);

                    double[] startPos = new double[3];
                    startVertex.getPosition(startPos);

                    // adjust the location
                    startPos[0] += entityPos[0];
                    startPos[1] += entityPos[1];
                    startPos[2] += entityPos[2];

                    eWrapper.convertWorldPosToScreenPos(startPos, screenStart);


                    // get the location of the end of the segment
                    int endVertexID = segment.getEndIndex();
                    VertexEntity endVertex = segmentEntity.getVertex(endVertexID);

                    double[] endPos = new double[3];
                    endVertex.getPosition(endPos);

                    // adjust the location
                    endPos[0] += entityPos[0];
                    endPos[1] += entityPos[1];
                    endPos[2] += entityPos[2];

                    eWrapper.convertWorldPosToScreenPos(endPos, screenEnd);

                    // add the first point
                    building.addPoint(screenStart[0], screenStart[1]);
                    building.addPoint(screenEnd[0], screenEnd[1]);

                }

            }

            // draw the building background color
            Color buildingColor =
                new Color(
                        Color.LIGHT_GRAY.getRed(),
                        Color.LIGHT_GRAY.getGreen(),
                        Color.LIGHT_GRAY.getBlue(),
                        128);

            if (entity instanceof DefaultBuildingEntity) {
                Object colorObj =
                    ((DefaultBuildingEntity)entity).getColor();

                if (colorObj != null) {
                    int[] colorProp = (int[])colorObj;
                    buildingColor = new Color(
                            colorProp[0],
                            colorProp[1],
                            colorProp[2],
                            128);
                }
            }

            g2d.setColor(buildingColor);
            g2d.fillPolygon(building);

            // Draw all segments
            for (int i = 0; i < segmentList.size(); i++) {

                SegmentEntity segment = segmentList.get(i);

                ListProperty wallList =
                    (ListProperty)segment.getProperty(SegmentEntity.SEGMENT_PROPERTY_SHEET, "Type");

                // get the location of the start of the segment
                int startVertexID = segment.getStartID();
                VertexEntity startVertex = segmentEntity.getVertex(startVertexID);

                double[] startPos = new double[3];
                startVertex.getPosition(startPos);

                // adjust the location
                startPos[0] += entityPos[0];
                startPos[1] += entityPos[1];
                startPos[2] += entityPos[2];

                eWrapper.convertWorldPosToScreenPos(startPos, screenStart);

                // get the location of the end of the segment
                int endVertexID = segment.getEndIndex();
                VertexEntity endVertex = segmentEntity.getVertex(endVertexID);

                if (endVertex == null)
                    continue;

                double[] endPos = new double[3];
                endVertex.getPosition(endPos);

                // adjust the location
                endPos[0] += entityPos[0];
                endPos[1] += entityPos[1];
                endPos[2] += entityPos[2];

                eWrapper.convertWorldPosToScreenPos(endPos, screenEnd);

                // set the color
                String wallType = (String)wallList.getValue();
                if (wallType.equals("Door")) {
                    g2d.setColor(DEFAULT_DOOR_COLOR);
                } else if (wallType.equals("Window")) {
                    g2d.setColor(DEFAULT_WINDOW_COLOR);
                } else {
                    g2d.setColor(DEFAULT_WALL_COLOR);
                }

                g2d.setStroke(SEGMENT_STROKE);

                // draw the segment
                g2d.drawLine(screenStart[0], screenStart[1], screenEnd[0], screenEnd[1]);

                if (DISPLAY_SELECTION_AREAS) {
                    g2d.setColor(Color.RED);
                    int RECT_HEIGHT = 10;

                    int[] x = new int[4];
                    int[] y = new int[4];

                    float slope = ((float)screenEnd[1] - screenStart[1]) /
                                   ((float)screenEnd[0] - screenStart[0]);

                    int xmod = 0;
                    int ymod = 0;

                    // TODO: Should really calculate perpendicular to the line
                    if ((Math.abs(slope)) > 0.5f) {
                        xmod = RECT_HEIGHT / 2 + Math.round(VERTEX_PIXEL_SIZE / 2);
                        ymod = 0;
                    } else {
                        xmod = 0;
                        ymod = RECT_HEIGHT / 2 + Math.round(VERTEX_PIXEL_SIZE / 2);
                    }

                    x[0] = screenStart[0] - xmod;
                    y[0] = screenStart[1] - ymod;

                    x[1] = screenStart[0] + xmod;
                    y[1] = screenStart[1] + ymod;

                    x[2] = screenEnd[0] + xmod;
                    y[2] = screenEnd[1] + ymod;

                    x[3] = screenEnd[0] - xmod;
                    y[3] = screenEnd[1] - ymod;

                    g2d.drawPolygon(x,y,x.length);
                }
            }

            // Draw all vertices
            for (int i = 0; i < vertices.size(); i++) {

                // get the location of vertex
                VertexEntity vertex = vertices.get(i);

                double[] vertexPos = new double[3];
                vertex.getPosition(vertexPos);

                // adjust the location
                vertexPos[0] += entityPos[0];
                vertexPos[1] += entityPos[1];
                vertexPos[2] += entityPos[2];

                eWrapper.convertWorldPosToScreenPos(vertexPos, screenStart);

                // Draw the two vertices entity
                g2d.setColor(vertexColor);
                g2d.fillOval(
                        screenStart[0] - VERTEX_PIXEL_SIZE / 2,
                        screenStart[1] - VERTEX_PIXEL_SIZE / 2,
                        VERTEX_PIXEL_SIZE,
                        VERTEX_PIXEL_SIZE);

            }

        }

        g2d.setTransform(defaultTransform);
        g2d.setColor(origColor);

    }


    /**
     * Draw the entity's selection representation to the screen. Overrides the
     * default behaviour because we don't want the selection box drawn, just
     * the vertex or segment.
     *
     * @param g2d The graphics object used to draw
     * @param eWrapper The wrapped entity to draw
     */
    public void drawSelection(Graphics2D g2d,EntityWrapper eWrapper) {

        Color origColor = g2d.getColor();
        AffineTransform defaultTransform = g2d.getTransform();

        Entity entity = eWrapper.getEntity();
        if (entity instanceof SegmentableEntity) {

            SegmentableEntity segmentEntity = (SegmentableEntity)entity;

            // get the base position
            double[] entityPos = new double[3];
            int screenEntity[] = new int[2];

            float[] entityRot = new float[4];
            if (entity instanceof PositionableEntity) {
                ((PositionableEntity)entity).getPosition(entityPos);
                ((PositionableEntity)entity).getRotation(entityRot);

                eWrapper.convertWorldPosToScreenPos(entityPos, screenEntity);
            }

            g2d.translate(-screenEntity[0], -screenEntity[1]);

            int selectedVertex = -1;
            int selectedSegment = -1;
            ArrayList<Entity> selectedList = seletionHelper.getSelectedList();
            int len = selectedList.size();
            for (int i = 0; i < len; i++) {
                Entity check = selectedList.get(i);               
                if (check instanceof VertexEntity) {
                    // TODO: use the first selected vertex for now
                    selectedVertex = check.getEntityID();
                    break;
                } else if (check instanceof VertexEntity) {
                    // TODO: use the first selected segment for now
                    selectedSegment = check.getEntityID();
                    break;                   
                }
            }
            int highlightedVertex = -1;// ((SegmentableEntity)entity).getHighlightedVertexID();
 
            // First find the bounds of the path
            int screenPos[] = new int[2];

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            ArrayList<VertexEntity> vertices = segmentEntity.getVertices();

            for(int i = 0; i < vertices.size(); i++) {
                VertexEntity vtx =  vertices.get(i);
                double[] pos = new double[3];
                vtx.getPosition(pos);

                // adjust the location
                pos[0] += entityPos[0];
                pos[1] += entityPos[1];
                pos[2] += entityPos[2];

                eWrapper.convertWorldPosToScreenPos(pos, screenPos);

                if (screenPos[0] < minX) {
                    minX = screenPos[0] - SELECTION_OFFSET;
                }
                if (screenPos[0] > maxX) {
                    maxX = screenPos[0] + SELECTION_OFFSET;
                }
                if (screenPos[1] < minY) {
                    minY = screenPos[1] - SELECTION_OFFSET;
                }
                if (screenPos[1] > maxY) {
                    maxY = screenPos[1] + SELECTION_OFFSET;
                }

            }

            // If no sub-objects are selected, just draw the bounding box.
            if (selectedVertex == -1 && selectedSegment == -1) {
                g2d.setColor(SELECTION_COLOR);
                g2d.setStroke(SELECTION_STROKE);

                int width = maxX - minX;
                int height = maxY - minY;

                g2d.drawRect(minX - SELECTION_OFFSET,
                             minY - SELECTION_OFFSET,
                             width + SELECTION_OFFSET * 2,
                             height + SELECTION_OFFSET * 2);
            } else {
                if(selectedSegment != -1) {
                    SegmentEntity segment = segmentEntity.getSegment(selectedSegment);
                    int[] lastPos = new int[2];

                    int startVertexID = segment.getStartID();
                    int endVertexID = segment.getEndID();

                    VertexEntity startVertex = segmentEntity.getVertex(startVertexID);
                    VertexEntity endVertex = segmentEntity.getVertex(endVertexID);

                    double[] startPos = new double[3];
                    startVertex.getPosition(startPos);

                    // adjust the location
                    startPos[0] += entityPos[0];
                    startPos[1] += entityPos[1];
                    startPos[2] += entityPos[2];

                    eWrapper.convertWorldPosToScreenPos(startPos, lastPos);

                    double[] endPos = new double[3];
                    endVertex.getPosition(endPos);

                    // adjust the location
                    endPos[0] += entityPos[0];
                    endPos[1] += entityPos[1];
                    endPos[2] += entityPos[2];

                    eWrapper.convertWorldPosToScreenPos(endPos, screenPos);

                    g2d.setColor(SELECTION_COLOR);
                    g2d.setStroke(SELECTED_SEGMENT_STROKE);
                    g2d.drawLine(lastPos[0], lastPos[1], screenPos[0], screenPos[1]);
                }

                if(highlightedVertex != -1) {
                    g2d.setColor(highlightColor);
                    g2d.setStroke(SELECTION_STROKE);
                    VertexEntity vtx = segmentEntity.getVertex(highlightedVertex);

                    double[] pos = new double[3];
                    vtx.getPosition(pos);

                    // adjust the location
                    pos[0] += entityPos[0];
                    pos[1] += entityPos[1];
                    pos[2] += entityPos[2];

                    eWrapper.convertWorldPosToScreenPos(pos, screenPos);

                    g2d.drawRect(screenPos[0] - SELECTION_OFFSET - VERTEX_PIXEL_SIZE / 2,
                                 screenPos[1] - SELECTION_OFFSET - VERTEX_PIXEL_SIZE / 2,
                                 VERTEX_PIXEL_SIZE + SELECTION_OFFSET * 2,
                                 VERTEX_PIXEL_SIZE + SELECTION_OFFSET * 2);
                }

                if(selectedVertex != -1) {
                    g2d.setColor(SELECTION_COLOR);
                    g2d.setStroke(SELECTION_STROKE);

                    VertexEntity vtx = segmentEntity.getVertex(selectedVertex);
                    double[] pos = new double[3];
                    vtx.getPosition(pos);

                    // adjust the location
                    pos[0] += entityPos[0];
                    pos[1] += entityPos[1];
                    pos[2] += entityPos[2];

                    eWrapper.convertWorldPosToScreenPos(pos, screenPos);

                    g2d.drawRect(screenPos[0] - SELECTION_OFFSET - (VERTEX_PIXEL_SIZE / 2),
                                 screenPos[1] - SELECTION_OFFSET - (VERTEX_PIXEL_SIZE / 2),
                                 VERTEX_PIXEL_SIZE + (SELECTION_OFFSET * 2),
                                 VERTEX_PIXEL_SIZE + (SELECTION_OFFSET * 2));
                }
            }

        }

        g2d.setTransform(defaultTransform);
        g2d.setColor(origColor);

    }

    /**
     * Get the width of the tool to be drawn in pixels.
     *
     * @return int A non-negative size in pixels
     */
    public int getWidth() {

        //if (buildings != null) {
        //    Rectangle bounds = building.getBounds();
        //    width = (int)Math.round(bounds.getWidth());
        //}

        return width;
    }

    /**
     * Get the height of the tool to be drawn in pixels.
     *
     * @return int A non-negative size in pixels
     */
    public int getHeight() {

        //if (buildings != null) {
        //    Rectangle bounds = building.getBounds();
        //    height = (int)Math.round(bounds.getHeight());
        //}

        return height;
    }

}
