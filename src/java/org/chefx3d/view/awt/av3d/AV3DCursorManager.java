/****************************************************************************
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

package org.chefx3d.view.awt.av3d;

import java.awt.*;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.chefx3d.tool.Tool;

import org.chefx3d.view.common.EditorConstants;

public class AV3DCursorManager implements EditorConstants, AV3DConstants {

    /** String path to 4 way crosshair arrow cursor image*/
    private static final String CURSOR_MOVE_ARROW_FILE_PATH=
        //"images/2d/fourWayMouseCursor.gif";
        "images/2d/fourWayAnchoredCursorWithArrow.png";

    /** String path to 2 way vertical arrow cursor image*/
    private static final String CURSOR_SCALE_VERT_ARROW_FILE_PATH=
        "images/2d/twoWayVerticalCursor.gif";

    /** String path to 2 way vertical arrow cursor image*/
    private static final String CURSOR_SCALE_HORI_ARROW_FILE_PATH=
        "images/2d/twoWayHorizontalImage.gif";

    /** String path to 2 way vertical arrow cursor image*/
    private static final String CURSOR_SCALE_DIAG_LEFT_ARROW_FILE_PATH=
          "images/2d/twoWayDiagonalLeftCursor.gif";

    /** String path to 2 way vertical arrow cursor image*/
    private static final String CURSOR_SCALE_DIAG_RIGHT_ARROW_FILE_PATH=
          "images/2d/twoWayDiagonalRightCursor.gif";


    /** The 4 way movement cursor that is used*/
    private Cursor movementCursor;

    /** The  2 way vertical movement and scale cursor that is used*/
    private Cursor vertScaleCursor;

    /** The  2 way horizontal movement and scale cursor that is used*/
    private Cursor horizontalScaleCursor;

    /** The  2 way Diagonal movement and scale cursor that is used*/
    private Cursor diagonalLeftScaleCursor;

    /** The  2 way Diagonal movement and scale cursor that is used*/
    private Cursor diagonalRightScaleCursor;

    /** The tool cursor that is used*/
    private Cursor toolSelectedCursor;

    /** The canvas that we're changing the cursors on */
    private Component canvas;

    /** The current mouse mode*/
    private AnchorData anchorCursor;

    /** The previous mouse Mode*/
    private ActionMode actionCursor;



    /**
     * The constructor
     * @param canvas  canvas component to change the mouse with
     */
    public AV3DCursorManager(Component canvas){

        this.canvas = canvas;
        anchorCursor = AnchorData.NONE;
        actionCursor = ActionMode.NONE;

    }
    /**
     *  Changes the cursor
     * @param mode The mode of the mouse to tell what cursor it should be
     */
    private void changeCursor(){

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        canvas.setCursor(null);

        switch(anchorCursor){

            case SOUTH:
            case NORTH:
                if( vertScaleCursor == null){
                    Image cursorImage = toolkit.getImage(
                            returnImageUrl(CURSOR_SCALE_VERT_ARROW_FILE_PATH));
                    Point cursorHotSpot = new Point(15,15);
                    vertScaleCursor = toolkit.createCustomCursor(
                            cursorImage, cursorHotSpot, "vertScaleCursor");

                }
                canvas.setCursor(vertScaleCursor);

                break;
            case EAST:
            case WEST:
                if( horizontalScaleCursor == null){
                    Image cursorImage = toolkit.getImage(
                            returnImageUrl(CURSOR_SCALE_HORI_ARROW_FILE_PATH));
                    Point cursorHotSpot = new Point(15,15);
                    horizontalScaleCursor = toolkit.createCustomCursor(
                            cursorImage, cursorHotSpot, "horizontalScaleCursor");

                }
                canvas.setCursor(horizontalScaleCursor);

                break;
            case NORTHWEST:
            case SOUTHEAST:
                if( diagonalLeftScaleCursor == null){
                    Image cursorImage = toolkit.getImage(
                            returnImageUrl(CURSOR_SCALE_DIAG_LEFT_ARROW_FILE_PATH));
                    Point cursorHotSpot = new Point(15,15);
                    diagonalLeftScaleCursor = toolkit.createCustomCursor(
                            cursorImage, cursorHotSpot, "diagonalScaleCursor");

                }
                canvas.setCursor(diagonalLeftScaleCursor);

                break;

            case NORTHEAST:
            case SOUTHWEST:
                if( diagonalRightScaleCursor == null){
                    Image cursorImage = toolkit.getImage(
                            returnImageUrl(CURSOR_SCALE_DIAG_RIGHT_ARROW_FILE_PATH));
                    Point cursorHotSpot = new Point(15,15);
                    diagonalRightScaleCursor = toolkit.createCustomCursor(
                            cursorImage, cursorHotSpot, "diagonalScaleCursor");

                }
                canvas.setCursor(diagonalRightScaleCursor);

                break;

        }

        if(actionCursor!=null ){

            switch(actionCursor){

            case MOVEMENT:
                if( movementCursor == null){
                    Image cursorImage = toolkit.getImage(
                            returnImageUrl(CURSOR_MOVE_ARROW_FILE_PATH));
                    Point cursorHotSpot = new Point(15,15);
                    movementCursor = toolkit.createCustomCursor(
                        cursorImage, cursorHotSpot, "movement");

                }
                canvas.setCursor(movementCursor);
                break;

            default:
                break;


            }


        }
    }

    /**
     * Sets the tool cursor
     * @param tool the current tool in which to take the image from
     */
    public void setToolCursor(Tool tool){

        actionCursor = ActionMode.PLACEMENT;
        anchorCursor = null;

        if (tool == null) {
            toolSelectedCursor =
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
            canvas.setCursor(toolSelectedCursor);
        } else  {

            Toolkit toolkit = Toolkit.getDefaultToolkit();

            // get the closet system size for the icon
            Dimension dim = toolkit.getBestCursorSize(32, 32);
            int width = (int)Math.round(dim.getWidth() * 0.499);
            int height = (int)Math.round(dim.getHeight() * 0.499);

            // TODO: need to display the item
//            Image cursorImage = toolkit.getImage(
//                    returnImageUrl(CURSOR_MOVE_ARROW_FILE_PATH));
//            
//            Point cursorHotSpot = new Point(width, height);
//            toolSelectedCursor = toolkit.createCustomCursor(
//                    cursorImage, cursorHotSpot, "toolSelected");
            toolSelectedCursor = 
            	Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
            canvas.setCursor(toolSelectedCursor);
        }

    }

    /**
     * Sets the cursor mode
     * @param mode InputMode
     */
    public void setCursorMode(Object mode){

        if(mode instanceof ActionMode){
            actionCursor = (ActionMode)mode;
            anchorCursor =AnchorData.NONE;
        }

        if(mode instanceof AnchorData){
            anchorCursor = (AnchorData)mode;
            actionCursor = ActionMode.NONE;
        }
        changeCursor();

    }
    /**
     * Return the cursor mode
     * @return InputMode
     */
    public ActionMode getCurrentMode(){
        return actionCursor;
    }

    /**
     * returns a working URL for the image
     * @param filePath
     * @return
     */
    private URL returnImageUrl(final String filePath){

            URL returnURL =
                (URL)AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            return ClassLoader.getSystemResource(filePath);
                        }
                    }
                );

            if(returnURL==null){
                ClassLoader cl = AV3DCursorManager.class.getClassLoader();
                returnURL = cl.getResource(filePath);
            }
            return returnURL;
    }

}
