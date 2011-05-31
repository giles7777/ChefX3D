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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *  Holds the utilities methods for S2DView and GT2DView
 * @author Jonathon Hubbard
 *  @version $Revision: 1.2 $ *
 */
public class ViewUtils {

    //////////////////////////////////////////////////////////////////////
    // Typical pan direction angles

    /** The azimuth of a pan UP */
    public static final double PAN_UP_DIRECTION = 0.0;

    /** The azimuth of a pan LEFT */
    public static final double PAN_LEFT_DIRECTION = Math.PI/2;

    /** The azimuth of a pan RIGHT */
    public static final double PAN_RIGHT_DIRECTION = Math.PI;

    /** The azimuth of a pan DOWN */
    public static final double PAN_DOWN_DIRECTION = 3*Math.PI/2;

    //////////////////////////////////////////////////////////////////////
    
    /** The referencing system in use */
    private CoordinateReferenceSystem crs;
 
    /** How far to zoom in or out per level */
    private double zoomFactor = 1.5;
    
    public ViewUtils(  ) {
        this.crs = null;
    }
    
    public ViewUtils( CoordinateReferenceSystem crs ) {
        this.crs = crs;
    }
    
    /**
     * Set the zoom factor.
     *
     * @param zoomFactor The new value
     */
    public void setZoomFactor( double zoomFactor ) {
        this.zoomFactor = zoomFactor;
    }
    
    /**
     * Return the zoom factor.
     *
     * @return zoomFactor The zoomFactor value
     */
    public double getZoomFactor( ) {
        return( zoomFactor );
    }

    /**
     * Return the new envelope for rendering a pan of the specified mapArea
     * in the specified direction and distance
     *
     * @param mapArea The current map area displayed on the rendering surface
     * @param direction The direction to pan in radians. An azimuth measured from the North
     * (UP) direction. E.G. Up = 0, Right = Math.PI/2, Down = Math.PI, Left = 3*Math.PI/2
     * @param distance The distance to pan in units of the reference system (typically meters)
     * @return The new map area
     */
    public void pan( Rectangle2D cameraBounds, double direction, double distance ) {

        double shiftX = 0;
        double shiftY = 0;

        if ( direction == PAN_UP_DIRECTION ) {
            shiftX = 0;
            shiftY = -distance;
        } else if ( direction == PAN_DOWN_DIRECTION ) {
            shiftX = 0;
            shiftY = distance;
        } else if ( direction == PAN_LEFT_DIRECTION ) {
            shiftX = -distance;
            shiftY = 0;
        } else if ( direction == PAN_RIGHT_DIRECTION ) {
            shiftX = distance;
            shiftY = 0;
        } else {
            shiftX = distance * Math.sin( direction );
            shiftY = distance * Math.cos( direction );
        }
        
        cameraBounds.setFrameFromDiagonal(
        		cameraBounds.getMinX( ) + shiftX,
        		cameraBounds.getMinY( ) + shiftY,
        		cameraBounds.getMaxX( ) + shiftX,
        		cameraBounds.getMaxY( ) + shiftY);
       
    }
    
    /**
     * Return the new envelope for rendering a pan of the specified mapArea
     * in the specified direction and distance
     *
     * @param mapArea The current map area displayed on the rendering surface
     * @param direction The direction to pan in radians. An azimuth measured from the North
     * (UP) direction. E.G. Up = 0, Right = Math.PI/2, Down = Math.PI, Left = 3*Math.PI/2
     * @param distance The distance to pan in units of the reference system (typically meters)
     * @return The new map area
     */
    public ReferencedEnvelope pan( ReferencedEnvelope mapArea, double direction, double distance ) {

        double shiftX = 0;
        double shiftY = 0;

        if ( direction == PAN_UP_DIRECTION ) {
            shiftX = 0;
            shiftY = distance;
        } else if ( direction == PAN_DOWN_DIRECTION ) {
            shiftX = 0;
            shiftY = -distance;
        } else if ( direction == PAN_LEFT_DIRECTION ) {
            shiftX = -distance;
            shiftY = 0;
        } else if ( direction == PAN_RIGHT_DIRECTION ) {
            shiftX = distance;
            shiftY = 0;
        } else {
            shiftX = distance * Math.sin( direction );
            shiftY = distance * Math.cos( direction );
        }

        return( new ReferencedEnvelope(
            mapArea.getMinX( ) + shiftX,
            mapArea.getMaxX( ) + shiftX,
            mapArea.getMinY( ) + shiftY,
            mapArea.getMaxY( ) + shiftY,
            crs ) );
    }

    /**
     * Return the new envelope for rendering a pan of the specified mapArea
     * given the bounds of the rendering surface and the point on that surface
     * to center the map on.
     *
     * @param mapArea The current map area displayed on the rendering surface
     * @param bounds The rendering surface bounds
     * @param point The point on the rendering surface to make the center of the map
     * @return The new map area
     */
    public void panToPoint( Rectangle2D mapArea,  Rectangle bounds, Point point ) {

    	// the target position
        double pointX = (double)point.x;
        double pointY = (double)point.y;

        // the map extent
        double mapWidth = mapArea.getWidth( );
        double mapHeight = mapArea.getHeight( );

        // the dimensions of the panel
        double panelWidth = bounds.getWidth( );
        double panelHeight = bounds.getHeight( );

        // translate the target position to map coordinates
        double deltaX = Math.abs(( pointX * mapWidth / panelWidth ) + mapArea.getMinX( ));
        double deltaY = Math.abs(mapArea.getMaxY( ) - ( pointY * mapHeight / panelHeight ));

        double mapWidth2 = mapWidth / 2.0;
        double mapHeight2 = mapHeight / 2.0;
        
        
        // the new region of the map to display - centered on the point
        mapArea.setFrameFromDiagonal(
        		deltaX - mapWidth2,           
        		deltaY - mapHeight2,
        		deltaX + mapWidth2,
        		deltaY + mapHeight2);
       
    }
    
    /**
     * Return the new envelope for rendering a pan of the specified mapArea
     * given the bounds of the rendering surface and the point on that surface
     * to center the map on.
     *
     * @param mapArea The current map area displayed on the rendering surface
     * @param bounds The rendering surface bounds
     * @param point The point on the rendering surface to make the center of the map
     * @return The new map area
     */
    public ReferencedEnvelope panToPoint( ReferencedEnvelope mapArea, Rectangle bounds, Point point ) {

        // the target position
        double pointX = (double)point.x;
        double pointY = (double)point.y;

        // the map extent
        double mapWidth = mapArea.getWidth( );
        double mapHeight = mapArea.getHeight( );

        // the dimensions of the panel
        double panelWidth = bounds.getWidth( );
        double panelHeight = bounds.getHeight( );

        // translate the target position to map coordinates
        double mapX = ( pointX * mapWidth / panelWidth ) + mapArea.getMinX( );
        double mapY = mapArea.getMaxY( ) - ( pointY * mapHeight / panelHeight );

        double mapWidth2 = mapWidth / 2.0;
        double mapHeight2 = mapHeight / 2.0;

        // the new region of the map to display - centered on the point
        return( new ReferencedEnvelope(
            mapX - mapWidth2,
            mapX + mapWidth2,
            mapY - mapHeight2,
            mapY + mapHeight2,
            crs ) );
    }

    /**
     * Calculate the map scale factor given the bounds of the rendering surface and
     * map area. The value will be in map units (typically meters) per pixels
     *
     * @param mapArea The map area displayed on the rendering surface
     * @param bounds The rendering surface bounds
     * @return The map scale factor
     */
    public static double getScale( Rectangle2D mapArea, Rectangle bounds ) {

        // the dimensions of the panel
        double panelWidth = bounds.getWidth( );
        double panelHeight = bounds.getHeight( );

        // the map extent
        double mapWidth = mapArea.getWidth( );
        double mapHeight = mapArea.getHeight( );

        // calculate the new scale
        double scaleX = panelWidth / mapWidth;
        double scaleY = panelHeight / mapHeight;

        double scale = 0;
        // use the smaller scale
        if ( scaleX < scaleY ) {
            scale = 1.0/scaleX;
        } else {
            scale = 1.0/scaleY;
        }

        return( scale );
    }
    
    /**
     * Calculate the map scale factor given the bounds of the rendering surface and
     * map area. The value will be in map units (typically meters) per pixels
     *
     * @param mapArea The map area displayed on the rendering surface
     * @param bounds The rendering surface bounds
     * @return The map scale factor
     */
    public static double getScale( ReferencedEnvelope mapArea, Rectangle bounds ) {

        // the dimensions of the panel
        double panelWidth = bounds.getWidth( );
        double panelHeight = bounds.getHeight( );

        // the map extent
        double mapWidth = mapArea.getWidth( );
        double mapHeight = mapArea.getHeight( );

        // calculate the new scale
        double scaleX = panelWidth / mapWidth;
        double scaleY = panelHeight / mapHeight;

        double scale = 0;
        // use the smaller scale
        if ( scaleX < scaleY ) {
            scale = 1.0/scaleX;
        } else {
            scale = 1.0/scaleY;
        }

        return( scale );
    }

    /**
     * Recalculate the map envelope to fit into the argument bounds at a constant scale
     *
     * @param mapArea
     * @param oldBounds
     * @param newBounds
     * @return The rescaled map area
     */
    public void rescaleMapArea(
    		S2DMap mapArea,
    		Rectangle2D cameraBounds, 
            Rectangle oldBounds, 
            Rectangle newBounds ) {

        double scale = getScale( cameraBounds, oldBounds );

        double origMapWidth = mapArea.getWorldPosition().getWidth( );
        double origMapHeight = mapArea.getWorldPosition().getHeight( );
        
        double origCameraWidth = cameraBounds.getWidth( );
        double origCameraHeight = cameraBounds.getHeight( );

        // The map envelope center point
        double centerX = origCameraWidth / 2.0 + cameraBounds.getMinX( );
        double centerY = origCameraHeight / 2.0 + cameraBounds.getMinY( );

        double centerMapX = origCameraWidth / 2.0 + mapArea.getWorldPosition().getMinX( );
        double centerMapY = origCameraHeight / 2.0 + mapArea.getWorldPosition().getMinY( );
        
        // the dimensions of the new panel
        double newPanelWidth = newBounds.getWidth( );
        double newPanelHeight = newBounds.getHeight( );
        
        double deltaPanelWidth = oldBounds.getWidth()-newBounds.getWidth( );
        double deltaPanelHeight = oldBounds.getHeight()-newBounds.getHeight( );

        // the new map dimensions / 2
        double newMapWidth2 = newPanelWidth * scale / 2;
        double newMapHeight2 = newPanelHeight * scale / 2;

        
        // the new region of the map to display
        cameraBounds.setFrameFromDiagonal(
        		centerX - newMapWidth2,            
        		centerY - newMapHeight2,
        		centerX + newMapWidth2,
        		centerY + newMapHeight2);
        
        mapArea.setPanelBounds(newBounds);
        mapArea.setCameraBounds(cameraBounds);
        mapArea.setScreenPosition(
        		mapArea.getScreenPosition().getMinX(),           
        		mapArea.getScreenPosition().getMinY(),
        		mapArea.getScreenPosition().getMaxX() - (deltaPanelWidth),
        		mapArea.getScreenPosition().getMaxY() - (deltaPanelHeight));
        
    }    

    /**
     * Recalculate the map envelope to fit into the argument bounds at a constant scale
     *
     * @param mapArea
     * @param oldBounds
     * @param newBounds
     * @return The rescaled map area
     */
    public ReferencedEnvelope rescaleMapArea(
            ReferencedEnvelope mapArea, 
            Rectangle oldBounds, 
            Rectangle newBounds ) {

        double scale = getScale( mapArea, oldBounds );

        double origMapWidth = mapArea.getWidth( );
        double origMapHeight = mapArea.getHeight( );

        // The map envelope center point
        double centerX = origMapWidth / 2.0 + mapArea.getMinX( );
        double centerY = origMapHeight / 2.0 + mapArea.getMinY( );

        // the dimensions of the new panel
        double newPanelWidth = newBounds.getWidth( );
        double newPanelHeight = newBounds.getHeight( );

        // the new map dimensions / 2
        double newMapWidth2 = newPanelWidth * scale / 2;
        double newMapHeight2 = newPanelHeight * scale / 2;

        // the new region of the map to display
        ReferencedEnvelope newMapArea = new ReferencedEnvelope(
            centerX - newMapWidth2,
            centerX + newMapWidth2,
            centerY - newMapHeight2,
            centerY + newMapHeight2,
            crs );

        return( newMapArea );
    }
}
