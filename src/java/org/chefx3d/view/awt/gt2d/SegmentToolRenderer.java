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
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.SegmentEntity;

/**
 * Renders a vertex or set of vertices to screen, with the option of custom
 * colouring.
 *
 * @author Russell Dodds
 * @version $Revision: 1.5 $
 */
public class SegmentToolRenderer extends AbstractToolRenderer {

    /** Debugging flag to show the area that is responsible for being selected */
    private static final boolean DISPLAY_SELECTION_AREAS = false;

    /** The default width of the renderer in pixels */
    private static final int DEFAULT_WIDTH = 10;

    /** The default height of the renderer in pixels */
    private static final int DEFAULT_HEIGHT = 10;

    /** Pixel size of a vertex box */
    private static final int VERTEX_PIXEL_SIZE = 10;

    private static final Color DEFAULT_VERTEX_COLOR = Color.ORANGE;
    private static final Color DEFAULT_LINE_COLOR = Color.WHITE;
    private static final Color DEFAULT_HIGHLIGHT_COLOR = Color.YELLOW;

    /** The vertex highlight color */
    private Color highlightColor;

    /** The vertex color */
    private Color vertexColor;

    /** The line color */
    private Color lineColor;

    /** The current view */
    private ViewingFrustum.Plane currentView;

    /**
     * Contruct a default instance with default rendering colours.
     */
    public SegmentToolRenderer(ViewingFrustum.Plane view) {
        this(DEFAULT_LINE_COLOR, DEFAULT_VERTEX_COLOR, DEFAULT_HIGHLIGHT_COLOR);
        currentView = view;
    }

    /**
     * Contruct an instance with a specified set of colours.
     *
     * @param lineColor The line color
     * @param vertexColor The vertex color
     * @param highlightColor The vertex highlight color
     */
    public SegmentToolRenderer(Color lineColor,
                              Color vertexColor,
                              Color highlightColor) {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        this.highlightColor = highlightColor;
        this.vertexColor = vertexColor;
        this.lineColor = lineColor;
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

    	if ( currentView != ViewingFrustum.Plane.TOP )
    		return;

        Color origColor = g2d.getColor();

        int screenStart[] = new int[2];
        int screenEnd[] = new int[2];

        Entity entity = eWrapper.getEntity();
        if (entity instanceof SegmentableEntity) {

            SegmentableEntity segmentEntity = (SegmentableEntity)entity;

            // get the segments & vertices
            ArrayList<SegmentEntity> segmentList = segmentEntity.getSegments();
            ArrayList<VertexEntity> vertices = segmentEntity.getVertices();

            // If we only have one vertex we won't have any segments defined yet.
            // This make sure that we will at least rendering that single vertex
            // to screen.
            if(vertices.size() == 1) {
                VertexEntity vtx =  vertices.get(0);

                double[] pos = new double[3];
                vtx.getPosition(pos);
                eWrapper.convertWorldPosToScreenPos(pos, screenStart);

                // Draw the main entity
                g2d.setColor(vertexColor);
                g2d.fillOval(screenStart[0] - VERTEX_PIXEL_SIZE / 2,
                             screenStart[1] - VERTEX_PIXEL_SIZE / 2,
                             VERTEX_PIXEL_SIZE,
                             VERTEX_PIXEL_SIZE);
            } else {

                // Draw all vertices
                for (int i = 0; i < vertices.size(); i++) {

                    // get the location of the start of the segment
                    VertexEntity vertex = vertices.get(i);

                    double[] startPos = new double[3];
                    vertex.getPosition(startPos);
                    eWrapper.convertWorldPosToScreenPos(startPos, screenStart);

                    // Draw the two vertices entity

                    if (segmentEntity.getStartVertexID() == vertex.getEntityID()) {
                        g2d.setColor(Color.GREEN);
                    } else if (segmentEntity.getEndVertexID() == vertex.getEntityID()) {
                        g2d.setColor(Color.RED);
                    } else {
                        g2d.setColor(vertexColor);
                    }

                    g2d.fillOval(
                            screenStart[0] - VERTEX_PIXEL_SIZE / 2,
                            screenStart[1] - VERTEX_PIXEL_SIZE / 2,
                            VERTEX_PIXEL_SIZE,
                            VERTEX_PIXEL_SIZE);

                }

                // Draw all segments
                for (int i = 0; i < segmentList.size(); i++) {

                    SegmentEntity segment = segmentList.get(i);

                    // get the location of the start of the segment
                    int startVertexID = segment.getStartIndex();
                    VertexEntity startVertex = segmentEntity.getVertex(startVertexID);

                    double[] startPos = new double[3];
                    startVertex.getPosition(startPos);
                    eWrapper.convertWorldPosToScreenPos(startPos, screenStart);

                    // get the location of the end of the segment
                    int endVertexID = segment.getEndIndex();
                    VertexEntity endVertex = segmentEntity.getVertex(endVertexID);

                    if (endVertex == null)
                        continue;

                    double[] endPos = new double[3];
                    endVertex.getPosition(endPos);
                    eWrapper.convertWorldPosToScreenPos(endPos, screenEnd);

                    if (segmentEntity.getLastSegmentID() == segment.getEntityID()) {
                        g2d.setColor(Color.BLUE);
                    } else {
                        g2d.setColor(lineColor);
                    }

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
            }
        }

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
    public void drawSelection(Graphics2D g2d, EntityWrapper eWrapper) {
        Color origColor = g2d.getColor();

        Entity entity = eWrapper.getEntity();
        if (entity instanceof SegmentableEntity) {

            SegmentableEntity segmentEntity = (SegmentableEntity)entity;

            int selectedVertex = ((SegmentableEntity)entity).getSelectedVertexID();
            int highlightedVertex = ((SegmentableEntity)entity).getHighlightedVertexID();
            int selectedSegment = ((SegmentableEntity)entity).getSelectedSegmentID();

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
                eWrapper.convertWorldPosToScreenPos(pos, screenPos);

                if (screenPos[0] < minX) {
                    minX = screenPos[0];
                }
                if (screenPos[0] > maxX) {
                    maxX = screenPos[0] + width;
                }
                if (screenPos[1] < minY) {
                    minY = screenPos[1];
                }
                if (screenPos[1] > maxY) {
                    maxY = screenPos[1] + height;
                }
            }

            // If no sub-objects are selected, just draw the bounding box.
            if ((selectedVertex == -1) && (((SegmentableEntity)entity).getSelectedSegmentID() == -1)) {
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
                    eWrapper.convertWorldPosToScreenPos(startPos, lastPos);

                    double[] endPos = new double[3];
                    endVertex.getPosition(endPos);
                    eWrapper.convertWorldPosToScreenPos(endPos, screenPos);

                    g2d.setColor(SELECTION_COLOR);
                    g2d.setStroke(SELECTION_STROKE);
                    g2d.drawLine(lastPos[0], lastPos[1], screenPos[0], screenPos[1]);
                }

                if(highlightedVertex != -1) {
                    g2d.setColor(highlightColor);
                    g2d.setStroke(SELECTION_STROKE);
                    VertexEntity vtx = segmentEntity.getVertex(highlightedVertex);

                    double[] pos = new double[3];
                    vtx.getPosition(pos);
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
                    eWrapper.convertWorldPosToScreenPos(pos, screenPos);


                    g2d.drawRect(screenPos[0] - SELECTION_OFFSET - (VERTEX_PIXEL_SIZE / 2),
                                 screenPos[1] - SELECTION_OFFSET - (VERTEX_PIXEL_SIZE / 2),
                                 VERTEX_PIXEL_SIZE + (SELECTION_OFFSET * 2),
                                 VERTEX_PIXEL_SIZE + (SELECTION_OFFSET * 2));
                }
            }

            g2d.setColor(origColor);

        }

    }




}
