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

 package org.chefx3d.view.awt.gt2d;

 /**
  * This class is for enumerating the different viewing angles, or planes,
  * that exist for a given GT2DView - ie: TOP, BOTTOM, FRONT, RIGHT, etc.
  *
  * @author Eric Fickenscher
  * @version $Revision: 1.2 $
  */
 public class ViewingFrustum {

     /** A list of the possible planes to edit */
     public static enum Plane {TOP, LEFT, RIGHT, FRONT};

//     private Plane currentView;
//
//     /**
//      * Constructor, initialize the initial viewing frustum to be
//      * a top-down view.
//      */
//     public ViewingFrustum(){
//         currentView = Plane.TOP;
//     }
//
//     /**
//      * Constructor; initialize the viewing frustum with a particular view
//      * @param initialView the initial viewing frustum
//      */
//     public ViewingFrustum(Plane initialView){
//         currentView = initialView;
//     }
//
//     /**
//      * Set the current viewing frustum
//      * @param newView
//      */
//     public void setCurrentView(Plane newView){
//         currentView = newView;
//     }
//
//     /**
//      * get the current viewing frustum
//      * @return
//      */
//     public Plane getCurrentView(){
//         return currentView;
//     }

 }