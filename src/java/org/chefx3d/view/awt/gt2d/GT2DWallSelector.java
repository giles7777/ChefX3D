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
package org.chefx3d.view.awt.gt2d;

// external imports
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

// local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.VertexEntity;

/**
 * A panel that shows a SegmentableEntity and allows for wall selection
 * @author Eric Fickenscher
 * @version $Revision: 1.3 $
 */
public class GT2DWallSelector extends JPanel implements MouseListener {

	/** Pixel size of a vertex box */
    private static final int VERTEX_PIXEL_SIZE = 10;

    /** Entity wrapper */
    EntityWrapper entityWrapper;

    /** Number of pixels per meter */
	double pixelsPerMeter;

	/** The 'real-world' minimum x-value of the panel */
	double minX;

	/** The 'real-world' maximum y-value of the panel */
	double maxY;

    /**
     * Constructor
     */
    public GT2DWallSelector() {

        setBorder(BorderFactory.createLineBorder(Color.black));
        addMouseListener(this);
    }


    /**
     * Set the entity wrapper
     * @param eWrapper
     */
    public void drawSegments(EntityWrapper eWrapper){
    	entityWrapper = eWrapper;
    	revalidate();
    }

    //----------------------------------------------------------
    // Methods defined by JComponent
    //----------------------------------------------------------

    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);

    	if ( entityWrapper == null)
    		return;

    	Entity entity = entityWrapper.getEntity();
		if (entity instanceof SegmentableEntity) {

			SegmentableEntity segmentEntity = (SegmentableEntity) entity;

			// get the segments & vertices
			ArrayList<SegmentEntity> segmentList = segmentEntity.getSegments();
			ArrayList<VertexEntity> vertices = segmentEntity.getVertices();

			if(vertices.size() == 0)
				return;

			VertexEntity vertex = vertices.get(0);
			double[] vertexPos = new double[3];
			vertex.getPosition(vertexPos);

			//
			// these four values represent the real-world minimum
			// and maximum bounds around the SegmentableEntity.
			//
			double worldPosMinX = vertexPos[0];
			double worldPosMaxX = vertexPos[0];
			double worldPosMinY = vertexPos[2];
			double worldPosMaxY = vertexPos[2];

			for (int i = 1; i < vertices.size(); i++) {
				vertex = vertices.get(i);
				vertex.getPosition(vertexPos);

				if (vertexPos[0] > worldPosMaxX)
					worldPosMaxX = vertexPos[0];
				if (vertexPos[0] < worldPosMinX)
					worldPosMinX = vertexPos[0];
				if (vertexPos[2] > worldPosMaxY)
					worldPosMaxY = vertexPos[2];
				if (vertexPos[2] < worldPosMinY)
					worldPosMinY = vertexPos[2];
			}


			// special case of one vertex
			if( vertices.size() == 1){

				pixelsPerMeter = (this.getWidth() < this.getHeight()) ?
						this.getWidth() : this.getHeight();

				minX = worldPosMinX - (this.getWidth()/2)/pixelsPerMeter ;
				maxY = worldPosMaxY + (this.getHeight()/2)/pixelsPerMeter;
			} else {

				//
				// When there are multiple vertices,
				// Calculate the real-world bounds of the panel by slightly
				// increasing the bounds of the segmentableEntity
				double percentPanelUsed = .8;

				double segmentWidth  = worldPosMaxX - worldPosMinX;
				double segmentHeight = worldPosMaxY - worldPosMinY;

				double widthScalar = ((double)this.getWidth()*percentPanelUsed)  / segmentWidth;
				double heightScalar =((double)this.getHeight()*percentPanelUsed) / segmentHeight;

				//
				// we want to use the smaller value, otherwise the segmentEntity
				// would scale outside the bounds of the panel, yaknow?
				pixelsPerMeter = (heightScalar < widthScalar) ?
									heightScalar : widthScalar;

				double panelWidth =  (double)this.getWidth() / pixelsPerMeter;
				double panelHeight = (double)this.getHeight()/ pixelsPerMeter;

				minX = worldPosMinX - ((panelWidth - segmentWidth)/2);
				maxY = worldPosMaxY + ((panelHeight - segmentHeight)/2);
			}

			//
			// Draw all vertices
			//
			for (int i = 0; i < vertices.size(); i++) {

				// get the location of the start of the segment
				vertex = vertices.get(i);
				double[] vertexWorldPos = new double[3];
				vertex.getPosition(vertexWorldPos);

				// Color the vertices appropriately
				int entityID = vertex.getEntityID();
				if (segmentEntity.getStartVertexID() == entityID) {
					g.setColor(Color.GREEN);
				} else if (segmentEntity.getEndVertexID() == entityID) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.cyan);
				}

				// convert world coordinates to panel coordinates with pixelsPerMeter
		        int x = (int)Math.round( (vertexWorldPos[0] - minX ) * pixelsPerMeter );
		        int y = (int)Math.round( (maxY - vertexWorldPos[2] ) * pixelsPerMeter );

				// use the panelToMapAdjustment to convert the map
				// coordinates to panel coordinates
				g.fillOval(x - VERTEX_PIXEL_SIZE/ 2,
						   y - VERTEX_PIXEL_SIZE/2,
						   VERTEX_PIXEL_SIZE,
						   VERTEX_PIXEL_SIZE);
			}

			//
			// Draw all segments
			//
            for (int i = 0; i < segmentList.size(); i++) {

                SegmentEntity segment = segmentList.get(i);

                // get the location of the start of the segment
                int startVertexID = segment.getStartID();
                VertexEntity startVertex = segmentEntity.getVertex(startVertexID);

                double[] segmentStartWorldPos = new double[3];
                startVertex.getPosition(segmentStartWorldPos);

				// get the location of the end of the segment
                int endVertexID = segment.getEndIndex();
                VertexEntity endVertex = segmentEntity.getVertex(endVertexID);

                if (endVertex == null)
                    continue;

                double[] segmentEndWorldPos = new double[3];
                endVertex.getPosition(segmentEndWorldPos);

				// color the segment appropriately
                if (segmentEntity.getLastSegmentID() == segment.getEntityID()) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.red);
                }

				// convert world coordinates to panel coordinates with pixelsPerMeter
		        int segmentStartX = (int)Math.round(
		        		(segmentStartWorldPos[0] - minX ) * pixelsPerMeter );
		        int segmentStartY = (int)Math.round(
		        		(maxY - segmentStartWorldPos[2] ) * pixelsPerMeter );

		        int segmentEndX = (int)Math.round(
		        		(segmentEndWorldPos[0] - minX ) * pixelsPerMeter );
		        int segmentEndY = (int)Math.round(
		        		(maxY - segmentEndWorldPos[2] ) * pixelsPerMeter );

                // draw the segment
                g.drawLine(segmentStartX,
                		   segmentStartY,
                		   segmentEndX,
                		   segmentEndY);
            }
		}
    }


    //----------------------------------------------------------
    // Methods defined by MouseListener
    //----------------------------------------------------------

    /**
     * Invoked when the mouse button has been clicked
     * (pressed and released) on a component.
     * Ignored.
     */
	public void mouseClicked(MouseEvent e) {

	}

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

    	Point mousePos = me.getPoint();

    	System.out.println("(" + (minX+((double)mousePos.x / pixelsPerMeter)) + ",\t" +
    							 (maxY-((double)mousePos.y / pixelsPerMeter)) + ")");



    	//
    	//TODO: some sort of 'findEntity' method needs to be called

    	//		EntitySearchReturn entReturn = findEntity((mapPosCenterPoint[0] + adjustedXDiff),
    	//												  (mapPosCenterPoint[1] + adjustedYDiff));
    	//		EntityWrapper eWrapper = entReturn.getEntityWrapper();
    	//
    	//		int vertexID = entReturn.getVertexID();
    	//		int segmentID = entReturn.getSegmentID();

		//System.out.println("    eWrapper: " + eWrapper);

    	//		// nothing found, set selection to location
    	//		if (eWrapper == null) {
    	//
    	//			boundInProgress = true;
    	//			boundRectangle.x = lastMousePoint.x;
    	//			boundRectangle.y = lastMousePoint.y;
    	//			boundRectangle.width = 0;
    	//			boundRectangle.height = 0;
    	//
    	//			changeSelection(EMPTY_ENTITY_LIST);
    	//
    	//		} else {
    	//			//   setSelectedEntity(eWrapper, segmentID, vertexID);
    	//
    	//		}
	}

    /**
     * Invoked when a mouse button has been released on a component.
     * Ignored.
     */
	public void mouseReleased(MouseEvent e) {}


}