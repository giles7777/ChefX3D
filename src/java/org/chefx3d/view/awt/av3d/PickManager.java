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

// External imports
import java.util.ArrayList;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.SceneGraphPath;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.VertexGeometry;

import org.j3d.aviatrix3d.picking.PickRequest;

import org.j3d.renderer.aviatrix3d.util.AVIntersectionUtils;

import org.j3d.device.input.TrackerState;

// Local Imports
import org.chefx3d.model.*;

/**
 * Handler for picking in the location layer
 *
 * @author Rex Melton
 * @version $Revision: 1.13 $
 */
class PickManager {

    /** The root grouping node */
    private Group rootGroup;

    /** List for containing raw aviatrix pick results */
    private ArrayList pickResults;

    /** List for containing ordered pick results */
    private ArrayList<PickData> orderedResults;

    /** Stores the mouse world position */
    private Vector3f mousePos;

    /** Stores the mouse world orientation*/
    private Vector3f mouseOri;

    /** general mouse picking*/
    private PickRequest mousePickRequest;

    /** Handler for intersection testing */
    private AVIntersectionUtils iutils;

	/** Entity hierarchy utils */
	private EntityUtils entityUtils;
	
    /* Scratch objects */
    private Matrix4f mtx;
    private Point3f intersectPoint;
    private Vector3f intersectVector;
    private Point3f origin;
    private Vector3f direction;

    /**
     * Constructor
     *
     * @param rootGroup The root of the scene to pick against
     */
    PickManager(WorldModel model, Group rootGroup) {

        this.rootGroup = rootGroup;
		entityUtils = new EntityUtils(model);

        mouseOri = new Vector3f();
        mousePos = new Vector3f();

        pickResults = new ArrayList();
        mousePickRequest = new PickRequest();
        mousePickRequest.pickGeometryType = PickRequest.PICK_RAY;
        mousePickRequest.pickSortType = PickRequest.SORT_ORDERED;
        mousePickRequest.pickType = PickRequest.FIND_GENERAL;
        mousePickRequest.useGeometry = false;
        mousePickRequest.foundPaths = pickResults;

        orderedResults = new ArrayList<PickData>();

        iutils = new AVIntersectionUtils();

        mtx = new Matrix4f();
        intersectPoint = new Point3f();
        intersectVector = new Vector3f();
        origin = new Point3f();
        direction = new Vector3f();
    }

    /**
     * Return the pick result, if one exists, that is the closest to
     * the pick origin. Or, null if none are available.
     *
     * @return The pick result closest to the pick origin. Or null if
     * no results are available.
     */
    PickData getResult() {
        PickData result = null;
        if (orderedResults.size() > 0) {
            result = orderedResults.get(0);
        }
        return(result);
    }

    /**
     * Return the pick result, if one exists, that is associated with
     * the argument user data object. Or, null if none are available.
     *
     * @param obj The user data object
     * @return The pick result associated with the user data object. Or null if
     * no result is available.
     */
    PickData getResult(Object obj) {
        PickData result = null;
        for (int i = 0; i < orderedResults.size(); i++) {
            PickData pd = orderedResults.get(i);
            if (pd.object == obj) {
                result = pd;
                break;
            }
        }
        return(result);
    }

    /**
     * Return the ordered pick results
     *
     * @return The list of pick results, ordered by proximity
     * to the pick origin.
     */
    ArrayList<PickData> getResults() {
        return(orderedResults);
    }

    /**
     * Return whether there are ordered pick results
     *
     * @return true if results are available, false otherwise.
     */
    boolean hasResults() {
        return(orderedResults.size() > 0);
    }

    /**
     * Reset the pick results
     */
    void reset() {
        orderedResults.clear();
    }

    /**
     * Perform a pick on the rootGroup
     *
     * @param evt The tracker event that caused the method to be called
     * @return true if the pick intersected a referenced object, false
     * otherwise
     */
    boolean doPick(TrackerState evt) {
       // System.out.println("PICKMANAGER mouseOri: " + this.mouseOri);
       // System.out.println("PICKMANAGER mousePos: " + this.mousePos);
       // System.out.println("PICKMANAGER Origin: " + this.origin);
        return(doPick(evt, true , false));
        
    }
    
    /**
     * Perform a pick on the rootGroup with the proxy geometry that 
     * is swapped in to the scene when a switch is turned off
     * being included in the pick .
     *
     * @param evt The tracker event that caused the method to be called
     * @return true if the pick intersected a referenced object, false
     * otherwise
     */
    boolean doPickProxy(TrackerState evt) {
        
        // bounds picking seems to be off right now,
        // use geometry for this for now       
        return(doPick(evt, true, true ));
        
    }

    /**
     * Perform a pick on the rootGroup
     *
     * @param evt The tracker event that caused the method to be called
     * @param useGeometry true if the pick should determine intersection
     * against geometry, false if the pick should only check the bounds.
     * @param pickProxy  determines whether the proxy models should be picked. 
     * True will pick proxy models False will  not pick against proxy models. 
     * @return true if the pick intersected a referenced object, false
     * otherwise
     */
    boolean doPick(TrackerState evt, boolean useGeometry, boolean pickProxy) {

        // initialize the position and orientation objects,
        // in case they are needed later
        mousePos.x = evt.worldPos[0];
        mousePos.y = evt.worldPos[1];
        mousePos.z = evt.worldPos[2];

        mouseOri.x = evt.worldOri[0];
        mouseOri.y = evt.worldOri[1];
        mouseOri.z = evt.worldOri[2];

        // initialize the pick request
        mousePickRequest.origin[0] = mousePos.x;
        mousePickRequest.origin[1] = mousePos.y;
        mousePickRequest.origin[2] = mousePos.z;

        mousePickRequest.destination[0] = mouseOri.x;
        mousePickRequest.destination[1] = mouseOri.y;
        mousePickRequest.destination[2] = mouseOri.z;

        Object[] pickedObject = null;
        Object[] pickedZone = null;
        boolean foundObject = false;

        pickResults.clear();
        orderedResults.clear();

        rootGroup.pickSingle(mousePickRequest);

        if (mousePickRequest.pickCount > 0) {

            // sort through the bounds intersections
            int num_pick = pickResults.size();
            if (useGeometry) {
                for (int i = 0; i < num_pick; i++) {

                    SceneGraphPath sgp = (SceneGraphPath)pickResults.get(i);
                    sgp.getTransform(mtx);

                    Shape3D shape = (Shape3D)sgp.getTerminalNode();
                    VertexGeometry geom = (VertexGeometry)shape.getGeometry();

                    origin.set(mousePos);
                    direction.set(mouseOri);

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
                        intersectVector.set(
                            intersectPoint.x - origin.x,
                            intersectPoint.y - origin.y,
                            intersectPoint.z - origin.z);

                        float distance = intersectVector.length();
                        for (int j = sgp.getNodeCount() - 1; j >= 0; j--) {
                            Node node = sgp.getNode(j);
                            Object obj = node.getUserData();
                            if (obj != null) {
                                if ((obj instanceof Entity) ||
                                    (obj instanceof AV3DAnchorInformation)) {
									
                                    PickData pd = new PickData(
                                        obj,
                                        sgp,
                                        new Point3f(intersectPoint),
                                        new Matrix4f(mtx),
                                        distance);

                                    boolean added = false;
                                    for (int k = 0; k < orderedResults.size(); k++) {
                                        PickData sequencedPickData = orderedResults.get(k);
                                        if (distance < sequencedPickData.distance) {
                                            orderedResults.add(k, pd);
                                            added = true;
                                            break;
                                        }
                                    }
                                    if (!added) {
                                        orderedResults.add(pd);
                                    }
                                    break;
                                } else if (obj instanceof String) {
                                    // do NOT pick on shadows
                                    String s = (String)obj;
                                    if(pickProxy) {
                                        if (s.equals("Shadow") ) {
                                            break;
                                        }
                                    }else {
                                        if (s.equals("Shadow") || s.startsWith("Bound")) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < num_pick; i++) {

                    SceneGraphPath sgp = (SceneGraphPath)pickResults.get(i);
                    sgp.getTransform(mtx);

                    for (int j = sgp.getNodeCount() - 1; j >= 0; j--) {
                        Node node = sgp.getNode(j);
                        Object obj = node.getUserData();
                        if (obj != null) {
                            if ((obj instanceof Entity) ||
                                (obj instanceof AV3DAnchorInformation)) {
								
                                PickData pd = new PickData(
                                    obj,
                                    sgp,
                                    null,
                                    new Matrix4f(mtx),
                                    0);

                                ///////////////////////////////////////////
                                // rem: the ordering seems to be by
                                // inverse distance in the bounds pick
                                if (obj instanceof ZoneEntity) {
                                    orderedResults.add(pd);
                                } else {
                                    orderedResults.add(0, pd);
                                }
                                ///////////////////////////////////////////

                                break;

                            } else if (obj instanceof String) {
                                // do NOT pick on shadows
                                String s = (String)obj;
                                if(pickProxy) {
                                    if (s.equals("Shadow") ) {
                                        break;
                                    }
                                }else {
                                    if (s.equals("Shadow") || s.startsWith("Bound")) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            pickResults.clear();
        }

        // flip the order if both a segment and vertex are
        // found and the segment is listed first
        if (orderedResults.size() >= 2) {
            PickData a = orderedResults.get(0);
            PickData b = orderedResults.get(1);
            if (a.object instanceof SegmentEntity &&
                    b.object instanceof VertexEntity) {
                orderedResults.set(0, b);
                orderedResults.set(1, a);
            }
        }

        return(orderedResults.size() > 0);
    }
    
	/**
	 * Filter the results to only include the specified zone
	 * entity and it's immediate children.
	 *
	 * @param zone The parent zone entity
	 */
	void filterResultsByParentZone(ZoneEntity zone) {
		for (int i = orderedResults.size() - 1; i >= 0; i--) {
			PickData pdx = orderedResults.get(i);
			if (pdx.object instanceof Entity) {
				Entity e = (Entity)pdx.object;
				if (!((e == zone) || (entityUtils.getZoneEntity(e) == zone))) {
					orderedResults.remove(i);
				}
			} else {
				orderedResults.remove(i);
			}
		}
	}
}
