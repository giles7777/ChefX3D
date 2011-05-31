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


import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A helper class for S2DView. Keeps track of map coordinates 
 * and keeps them on hand when needed
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.2 $
 */
public class S2DMap implements Cloneable {

	/** World Rectangle  , in meters*/
	private Rectangle2D worldRectangle;
	/** Screen rectangle, in pixels*/
	private Rectangle2D screenRectangle;
	/** The Camera of the scene in meters*/
	private Rectangle2D cameraBounds;
	/** Bounds of the panel in pixels*/
	private Rectangle panelBounds;	
	
    /** The current plane the edits effect */ 
    private ViewingFrustum.Plane currentPlane;
	
	public S2DMap(ViewingFrustum.Plane plane){
		
		screenRectangle=new Rectangle2D.Double(0,0,0,0);
		worldRectangle=new Rectangle2D.Double(0,0,0,0);	
		
		cameraBounds = new Rectangle2D.Double(0,0,0,0);
		cameraBounds.setFrameFromDiagonal(0,0,0,0);
		currentPlane=plane;
		
	}
	/** 
	 * Copy Constructor
	 * @param obj
	 */
	private S2DMap(S2DMap obj){
		
		screenRectangle=(Rectangle2D)obj.screenRectangle.clone();
		worldRectangle=(Rectangle2D)obj.worldRectangle.clone();		
		cameraBounds =(Rectangle2D)obj.cameraBounds.clone();		
		currentPlane=obj.currentPlane;
		
	}
	public S2DMap clone() {
		return (new S2DMap(this));
	}
	
	public void setPanelBounds(Rectangle bounds){
		panelBounds = bounds;
	}
	
	/**
	* Sets the screen rectangles Top right and bottom left corner. 
	* Then Updates the world rectangle. 
	* @param minX
	* @param minY
	* @param maxX
	* @param maxY
	*/
	public void setScreenPosition(double minX, double minY, double maxX, double maxY){
		
		Point2D worldMinPosition = new Point2D.Double(0,0);
		Point2D worldMaxPosition = new Point2D.Double(0,0);
		screenRectangle.setFrameFromDiagonal(minX, minY, maxX, maxY);
		
		convertScreenPosToWorldPos( minX, minY, worldMinPosition);
		convertScreenPosToWorldPos( maxX, maxY, worldMaxPosition);
		
		worldRectangle.setFrameFromDiagonal(
				worldMinPosition.x,
				worldMinPosition.y,
				worldMaxPosition.x,
				worldMaxPosition.y);
		
	}
	/**
	 * Sets the screen rectangles Top right and bottom left corner. 
	 * Then Updates the world rectangle. 
	 * @param minX
	 * @param minY
	 * @param maxX
	 * @param maxY
	 */
	public void setScreenPosition(float minX, float minY, float maxX, float maxY){
		
		Point2D worldMinPosition = new Point2D.Double(0,0);
		Point2D worldMaxPosition = new Point2D.Double(0,0);
		screenRectangle.setFrameFromDiagonal(minX, minY, maxX, maxY);
		
		convertScreenPosToWorldPos( minX, minY, worldMinPosition);
		convertScreenPosToWorldPos( maxX, maxY, worldMaxPosition);
		
		worldRectangle.setFrameFromDiagonal(
				worldMinPosition.x,
				worldMinPosition.y,
				worldMaxPosition.x,
				worldMaxPosition.y);		
	}
	
	/**
	 * Sets the World rectangles Top right and bottom left corner. 
	 * Then Updates the Screen rectangle. 
	 * @param minX
	 * @param minY
	 * @param maxX
	 * @param maxY
	 */
	public void setWorldPosition(double minX, double minY, double maxX, double maxY){
		
		Point2D screenMinPosition = new Point2D.Double(0,0);
		Point2D screenMaxPosition = new Point2D.Double(0,0);
		worldRectangle.setFrameFromDiagonal(minX, minY, maxX, maxY);
		
		convertWorldPosToScreenPos( minX, minY, screenMinPosition);
		convertWorldPosToScreenPos( maxX, maxY, screenMaxPosition);	
		
		
		
		screenRectangle.setFrameFromDiagonal(
				screenMinPosition.x,
				screenMinPosition.y,
				screenMaxPosition.x,
				screenMaxPosition.y);
		
		
	}
	/**
	 * Sets the World rectangles Top right and bottom left corner. 
	 * Then Updates the Screen rectangle. 
	 * @param minX
	 * @param minY
	 * @param maxX
	 * @param maxY
	 */
	public void setWorldPosition(float minX, float minY, float maxX, float maxY){
		
		Point2D screenMinPosition = new Point2D.Double(0,0);
		Point2D screenMaxPosition = new Point2D.Double(0,0);
		worldRectangle.setFrameFromDiagonal(minX, minY, maxX, maxY);
		
		convertWorldPosToScreenPos( minX, minY, screenMinPosition);
		convertWorldPosToScreenPos( maxX, maxY, screenMaxPosition);
		
		screenRectangle.setFrameFromDiagonal(
				screenMinPosition.x,
				screenMinPosition.y,
				screenMaxPosition.x,
				screenMaxPosition.y);		
	}
	
	/** 
	 * Returns the World Rectangle
	 * @return
	 */
	public Rectangle2D getWorldPosition(){
		
		return worldRectangle;
	}
	
	/** 
	 * Returns the Screen Rectangle
	 * @return
	 */
	public Rectangle2D getScreenPosition(){
		
		return screenRectangle;
	}	

	/** 
	 * Sets the camera bounds
	 * @return
	 */
	public void setCameraBounds(Rectangle2D bounds){
		cameraBounds=(Rectangle2D)bounds.clone();
		
	}

	/** 
	 * Updates the map,
	 *  usually called after the camera or panel bounds were changed
	 */
	public void updateMapArea(){		
		
		
		setWorldPosition(
				worldRectangle.getMinX(),
				worldRectangle.getMinY(),
				worldRectangle.getMaxX(),
				worldRectangle.getMaxY());
	}
	
	
    /**
     * Convert mouse coordinates into world coordinates.
     *
     * TODO: This depends on iconCenterX which per tool.  Needs to be passed in.
     *
     * @param panelX The panel x coordinate
     * @param panelY The panel y coordinate
     * @param position The world position in meters
     */
    private void convertScreenPosToWorldPos(double positionX, double positionY, Point2D position) {

    	
        // the map extent
        double mapWidth = cameraBounds.getWidth();
        double mapHeight = cameraBounds.getHeight();
       

        // the dimensions of the panel
        double panelWidth = panelBounds.getWidth();
        double panelHeight = panelBounds.getHeight();

        // translate the mouse position to map coordinates
        double x = (positionX * mapWidth / panelWidth) + cameraBounds.getMinX();
        double y = ((positionY * mapHeight) / panelHeight) - cameraBounds.getMaxY();

        // update axis based on the current set face
        position.setLocation(x, y);    

    }

    
    /**
     * Convert world coordinates in meters to panel pixel location.
     *
     * TODO: This depends on iconCenterX which per tool.  Needs to be passed in.
     *
     * @param position World coordinates
     * @param pixel Mouse coordinates
     */
    private void convertWorldPosToScreenPos(double positionX, double positionY, Point2D pixel) {
        

        // the map extent
        double mapWidth = cameraBounds.getWidth();
        double mapHeight = cameraBounds.getHeight();
        

        // the dimensions of the panel
        double panelWidth = panelBounds.getWidth();
        double panelHeight = panelBounds.getHeight();
        int x=0;
        int y=0;
        // convert world coordinates to panel coordinates
        switch (currentPlane) {
        	case TOP:
        		double test1=panelWidth / mapWidth;
        		double test2=(positionX + cameraBounds.getMaxX());
        		double test3=test1*test2;
        		
            	x = (int)Math.round(((positionX - cameraBounds.getMinX()) * panelWidth) / mapWidth);
            	y = (int)Math.round(((positionY + cameraBounds.getMaxY()) * panelHeight) / mapHeight);
            	break;
        	case LEFT:
            	x = (int)Math.round(((-positionX - cameraBounds.getMinX()) * panelHeight) / mapHeight);      
            	y = (int)Math.round(((-positionY + cameraBounds.getMaxY()) * panelHeight) / mapHeight);
            	break;
        	case RIGHT:
            	x = (int)Math.round(((positionX - cameraBounds.getMinX()) * panelHeight) / mapHeight);
            	y= (int)Math.round(((-positionY + cameraBounds.getMaxY()) * panelHeight) / mapHeight);
            	break;
        	case FRONT:
        		x = (int)Math.round(((positionX - cameraBounds.getMinX()) * panelWidth) / mapWidth);
        		y = (int)Math.round(((positionY + cameraBounds.getMaxY()) * panelHeight) / mapHeight);
        		break;
           
        }
        pixel.setLocation(x, y); 
/*
System.out.println("S2DView.convertWorldPosToScreenPos()");
System.out.println("    position[0]: " + position[0]);
System.out.println("    mapArea.getMinX(): " +  panelBoundsMeters.getMaxX());
System.out.println("    panelWidth: " + panelWidth);
System.out.println("    mapWidth: " + mapWidth);
System.out.println("    x = ((" + position[0] + " + " + panelBoundsMeters.getMaxX() + ") * " + panelWidth + ") / " + mapWidth);
System.out.println("    x = " + x);
System.out.println("    position[1]: " + position[1]);
System.out.println("    mapArea.getMaxY(): " +  panelBoundsMeters.getMaxY());
System.out.println("    panelHeight: " + panelHeight);
System.out.println("    mapHeight: " + mapHeight);
System.out.println("    y = ((" + position[1] + " + " + panelBoundsMeters.getMaxY() + ") * " + panelHeight + ") / " + mapHeight);
System.out.println("    y = " + y);
System.out.println("    position[2]: " + position[2]);
System.out.println("    mapArea.getMinX(): " +  panelBoundsMeters.getMinX());
System.out.println("    panelHeight: " + panelHeight);
System.out.println("    mapHeight: " + mapHeight);
System.out.println("    z = ((" + position[2] + " + " + panelBoundsMeters.getMaxX() + ") * " + panelHeight + ") / " + mapHeight);
System.out.println("    z1 = " + z1);
System.out.println("    z2 = " + z2);
*/        
        
       
            	
        
        
    }
    /**
     * Sets the current view plane
     * @param newPlane
     */
    public void setCurrentPlane(ViewingFrustum.Plane newPlane){
    	this.currentPlane = newPlane;
    }

	
	
}
