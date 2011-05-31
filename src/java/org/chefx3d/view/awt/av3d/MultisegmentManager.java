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

package org.chefx3d.view.awt.av3d;

// External imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector3d;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.TransformGroup;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;


import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Manager to coordinate updates to Segment's geometry. Additionally,
 * a utility class to calculate parameters applicable to multisegment
 * (aka Segmentable) entities.
 *
 * @author Rex Melton
 * @version $Revision: 1.16 $
 */
class MultisegmentManager implements 
    NodeUpdateListener,
    SegmentDetails, 
    EntityPropertyListener, 
    PerFrameObserver  {
    
	/** Maximum angle between segments, above which mitering is disabled */
	private static final double MITER_THRESHOLD = 165 * Math.PI / 180; // 165 degrees
	
    /** The world model */
    private WorldModel model;

    /** Reference to the scene management observer created in AV3DView */
    private SceneManagerObserver mgmtObserver;

    /** The root Group for this manager */
    private Group rootGroup;

    /** The TransformGroup */
    private TransformGroup transformGroup;

    /** Map of the segment entity wrappers in the scene */
    private HashMap<Integer, SegmentEntityWrapper> segmentWrapperMap;
    
    /** The SegmentableEntity for this manager */
    private SegmentableEntity multisegment;

    /** Local map of Segments */
    private HashMap<Integer, SegmentEntity> segmentMap;

    /** The set of segments that require updating during the observer callback */
    private ArrayList<Integer> updateList;
        
    /** Scratch variables */
    private double[] rght;
    private double[] left;
    private double[] pos;
    private Vector3d norm;
	private Vector3d v0;
	private Vector3d v1;
	
    
    /** Array list of entity models to add to the scene */
    private ArrayList<AV3DEntityWrapper> entityToAddList;

    /** Array list of entity models to remove from the scene */
    private ArrayList<AV3DEntityWrapper> entityToRemoveList;

	/** The set of listeners concerned about changes to the
	 *  segment content. */
	private ArrayList<SegmentListener> segmentListeners;
	
    /**
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param model The WorldModel
     * @param rootGroup The Group node that is our root
     * @param segmentWrapperMap Map of segment wrappers, shared with
     * the entity manager.
     */
    MultisegmentManager(
        SceneManagerObserver mgmtObserver,
        WorldModel model,
        Group rootGroup,
        HashMap<Integer, SegmentEntityWrapper> segmentWrapperMap) {
        
        this.mgmtObserver = mgmtObserver;
        this.mgmtObserver.addObserver(this);
        this.model = model;
        this.rootGroup = rootGroup;
        this.segmentWrapperMap = segmentWrapperMap;
        
        entityToAddList = new ArrayList<AV3DEntityWrapper>();
        entityToRemoveList = new ArrayList<AV3DEntityWrapper>();
        
        segmentMap = new HashMap<Integer, SegmentEntity>();
        updateList = new ArrayList<Integer>();
		segmentListeners = new ArrayList<SegmentListener>();
        
        rght = new double[3];
        left = new double[3];
        pos = new double[3];
        norm = new Vector3d();
        v0 = new Vector3d();
        v1 = new Vector3d();
        
        init();
    }

    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds. Generally speaking
     * it is assumed in most cases that the src Object passed in is a
     * SharedNode and is generally treated like one.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {

        if (src == transformGroup) {

            int numToRemove = entityToRemoveList.size();
            if (numToRemove > 0) {
                for(int i = 0; i < numToRemove; i++) {
                    AV3DEntityWrapper wrapper = entityToRemoveList.get(i);
                    Node node = wrapper.sharedNode;
                    transformGroup.removeChild(node);
                }
                entityToRemoveList.clear();
            }

            int numToAdd = entityToAddList.size();
            if (numToAdd > 0) {
                for(int i = 0; i < numToAdd; i++) {
                    AV3DEntityWrapper wrapper = entityToAddList.get(i);
                    Node node = wrapper.sharedNode;
                    transformGroup.addChild(node);
                    /////////////////////////////////////////////////////////////
                    // rem: this is a workaround. when loading the set of walls 
                    // for the predefined locations, the parent node of the
                    // segment does not seem to get set until an update of the
                    // segment occurs. this forces a recalculation of the 
                    // geometry the next update cycle after the wall has been 
                    // added - and forces the scenegraph hierarchy to finalize 
                    // it's parenting.
                    updateList.add(wrapper.entity.getEntityID());
                    /////////////////////////////////////////////////////////////
                }
                entityToAddList.clear();
            }
        }
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
    }

    //---------------------------------------------------------------
    // Methods required by SegmentDetails
    //---------------------------------------------------------------
    
    /**
     * Return the miter angles of the segment ends in an 
     * array double[left, right]
     *
     * @param segment The segment entity
     * @param result The array to initialize with the results, or null
     * and a new array will be allocated.
     * @return the array of miter angles
     */
    public double[] getSegmentMiter(SegmentEntity segment, double[] result) {
        
        if ((result == null) || (result.length < 2)) {
            result = new double[2];
        } else {
            result[0] = 0;
            result[1] = 0;
        }
        if (multisegment != null) {
            
            ArrayList<SegmentEntity> seg_list = multisegment.getSegments();
            if (seg_list.size() > 1) {
                
                VertexEntity rght_ve = segment.getEndVertexEntity();
                VertexEntity left_ve = segment.getStartVertexEntity();
                
                Vector3d face = segment.getFaceVector();
				float thickness = getSegmentThickness(segment);
                
                for (int i = 0; i < seg_list.size(); i++) {
                    SegmentEntity se = seg_list.get(i);
                    
                    if (se != segment) {
                        VertexEntity ve_r = se.getEndVertexEntity();
                        if (ve_r == left_ve) {
                            Vector3d test = se.getFaceVector();
                            double angle = getAngle(test, face);
							if ((angle > MITER_THRESHOLD) || (angle < -MITER_THRESHOLD)) {
								// prevent 'extreme' miter angle cases
                            	result[0] = 0;
							} else {
								float t = getSegmentThickness(se);
								if (thickness == t) {
									// simple case, the walls are the same depth
									result[0] = angle / 2;
								} else {
									// different thickness walls, get the vector
									// from the common vertex on the face to the
									// intersection point of the back faces
									double n_angle = Math.abs(angle);
									if (n_angle > Math.PI/2) {
										n_angle -= Math.PI/2;
									}
									double n_sin = Math.sin(n_angle);
									
									v0.set(test);
									v0.normalize();
									v0.scale(thickness / n_sin);
									
									v1.set(face);
									v1.negate();
									v1.normalize();
									v1.scale(t / n_sin);
									
									v0.add(v1);
									
									result[0] = face.angle(v0) - Math.PI/2;
								}
							}
						} else if (ve_r == rght_ve) {
							// the segments have opposite facing properties,
							// skip the miter
                            result[1] = 0;
                        }
                        VertexEntity ve_l = se.getStartVertexEntity();
                        if (ve_l == rght_ve) {
                            Vector3d test = se.getFaceVector();
                            double angle = getAngle(face, test);
							if ((angle > MITER_THRESHOLD) || (angle < -MITER_THRESHOLD)) {
								// prevent 'extreme' miter angle cases
                            	result[1] = 0;
							} else {
								float t = getSegmentThickness(se);
								if (thickness == t) {
									// simple case, the walls are the same depth
									result[1] = angle / 2;
								} else {
									// different thickness walls, get the vector
									// from the common vertex on the face to the
									// intersection point of the back faces
									double n_angle = Math.abs(angle);
									if (n_angle > Math.PI/2) {
										n_angle -= Math.PI/2;
									}
									double n_sin = Math.sin(n_angle);
									
									v0.set(face);
									v0.normalize();
									v0.scale(t / n_sin);
									
									v1.set(test);
									v1.negate();
									v1.normalize();
									v1.scale(thickness / n_sin);
									
									v0.add(v1);
									
									result[1] = (Math.PI - face.angle(v0)) - Math.PI/2;
								}
							}
						} else if (ve_l == left_ve) {
							// the segments have opposite facing properties,
							// skip the miter
                            result[0] = 0;
                        }
                    }
                }
            }
        }
        return(result); 
    }
	
    /**
     * Return the vertex in local coordinates
     *
     * @param ve The VertexEntity
     * @param local The array to initialize with the vertex coordinates
     * @return The array containing the local vertex coordinates
     */
    public float[] toLocal(VertexEntity ve, float[] local) {
        
        if ((local == null) || (local.length < 3)) {
            local = new float[3];
        }
        ve.getPosition(pos);
        
        //////////////////////////////////////////////////////////////
        // rem: hard coded, relationship between the walls and the floor
        // alternatively (and more generally), the transform matrix of
        // this could be inverted and used to transform the point
        local[0] = (float)pos[0];
        local[1] = (float)pos[2];
        local[2] = -(float)pos[1];
        //////////////////////////////////////////////////////////////
        
        return(local);
    }
    
    /**
     * Return the parent TransformGroup
     *
     * @return The parent TransformGroup
     */
    public TransformGroup getParent() {
        return(transformGroup);
    }
    
    //----------------------------------------------------------
    // Methods for EntityPropertyListener
    //----------------------------------------------------------
    
    public void propertiesUpdated(List<EntityProperty> properties) {
    }
    
    public void propertyAdded(
        int entityID,
        String propertySheet,
        String propertyName) {
    }
    
    public void propertyRemoved(
        int entityID,
        String propertySheet,
        String propertyName) {
    }
    
    public void propertyUpdated(
        int entityID,
        String propertySheet,
        String propertyName,
        boolean ongoing) {
		
        if (propertyName.equals(PositionableEntity.POSITION_PROP) || 
            propertyName.equals(VertexEntity.HEIGHT_PROP) || 
            propertyName.equals(SegmentEntity.WALL_THICKNESS_PROP) || 
            propertyName.equals(SegmentEntity.STANDARD_FACING_PROP)) {
            
			Entity entity = model.getEntity(entityID);
			update(entity);
        }
    }
    
    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {
        for (int i = 0; i < updateList.size(); i++) {
            SegmentEntityWrapper sew = segmentWrapperMap.get(updateList.get(i));
            if (sew != null) {
                sew.updateSegment();
				fireSegmentChanged(sew.segment);
            }
        }
        updateList.clear();
    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------
    
    /**
     * Eliminate any references that this object may have to make it
     * eligible for GC.
     */
    void dispose() {
        
        mgmtObserver.removeObserver(this);
        
        for (Iterator<SegmentEntity> i = segmentMap.values().iterator(); 
			i.hasNext();) {
			
            SegmentEntity se = i.next();
            se.removeEntityPropertyListener(this);
			
			fireSegmentRemoved(se);
            
            VertexEntity v0 = se.getEndVertexEntity();
            v0.removeEntityPropertyListener(this);
            
            VertexEntity v1 = se.getStartVertexEntity();
            v1.removeEntityPropertyListener(this);
        }
        segmentMap.clear();
        multisegment = null;
    }
    
    /**
     * Add a child entity's representation to this transform
     *
     * @param wrapper The entity wrapper to add
     */
    void addChild(AV3DEntityWrapper wrapper) {
        
        SegmentEntity se = (SegmentEntity)wrapper.entity;
        segmentMap.put(se.getEntityID(), se);
        se.addEntityPropertyListener(this);
        
        VertexEntity v0 = se.getEndVertexEntity();
        v0.addEntityPropertyListener(this);
        
        VertexEntity v1 = se.getStartVertexEntity();
        v1.addEntityPropertyListener(this);
        
        entityToAddList.add(wrapper);
		
		update(se);
		((SegmentEntityWrapper)wrapper).updateSegment();
		fireSegmentAdded(se);
		
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
    }
    
    /**
     * Remove a child entity's representation from this transform
     *
     * @param wrapper The entity wrapper to remove
     */
    void removeChild(AV3DEntityWrapper wrapper) {
        
        // TODO: check the segment's vertex entitys for use in other segments -
        // if unused remove the listener - if still in use, don't remove
        // the listener
        SegmentEntity se = (SegmentEntity)wrapper.entity;
        int id = se.getEntityID();
        if (segmentMap.containsKey(id)) {
            segmentMap.remove(id);
            se.removeEntityPropertyListener(this);
			update(se);
			fireSegmentRemoved(se);
        }
		
		if (entityToAddList.contains(wrapper)) {
			entityToAddList.remove(wrapper);
		} else {
			entityToRemoveList.add(wrapper);
		}
		
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
    }
    
    /**
     * Set the active SegmentableEntity
     *
     * @param multisegment The active SegmentableEntity
     */
    void setSegmentableEntity(SegmentableEntity multisegment) {
        this.multisegment =  multisegment;
    }
        
	/**
	 * Add a SegmentListener
	 *
	 * @param sl The listener to add
	 */
	void addSegmentListener(SegmentListener sl) {
		if (!segmentListeners.contains(sl)) {
			segmentListeners.add(sl);
		}
	}
	
	/**
	 * Remove a SegmentListener
	 *
	 * @param cl The listener to remove
	 */
	void removeSegmentListener(SegmentListener sl) {
		segmentListeners.remove(sl);
	}
	
    /**
     * Notify the SegmentListener of a change
     */
    void fireSegmentAdded(SegmentEntity se) {

        int num = segmentListeners.size();
        SegmentListener sl;

        for (int i = 0; i < num; i++) {
            sl = segmentListeners.get(i);
            sl.segmentAdded(se);
        }
    }
	
    /**
     * Notify the SegmentListener of a change
     */
    void fireSegmentRemoved(SegmentEntity se) {

        int num = segmentListeners.size();
        SegmentListener sl;

        for (int i = 0; i < num; i++) {
            sl = segmentListeners.get(i);
            sl.segmentRemoved(se);
        }
    }
	
    /**
     * Notify the SegmentListener of a change
     */
    void fireSegmentChanged(SegmentEntity se) {

        int num = segmentListeners.size();
        SegmentListener sl;

        for (int i = 0; i < num; i++) {
            sl = segmentListeners.get(i);
            sl.segmentChanged(se);
        }
    }
	
    /**
     * Calculate the angle between the vectors
     *
     * @param v0 A vector
     * @param v1 A vector
     * @return The angle between
     */
    private double getAngle(Vector3d v0, Vector3d v1) {
        
        double angle = 0;
        norm.cross(v0, v1);
        if (norm.z != 0) {
            int sign = (norm.z < 0) ? 1 : -1;
            angle = v0.angle(v1);
            if (angle == Math.PI) {
                angle = 0;
            } else {
                angle *= sign;
            }
        }
        return(angle);
    }
    
    /**
     * Queue up all the segment geometry for an update
     */
    private void updateAll() {
        // this is brute force, and probably more work than necessary,
        // but at least each segment will only be updated once.
        Set<Integer> ids = segmentMap.keySet();
        for (Iterator<Integer> i = ids.iterator(); i.hasNext();) {
            Integer id = i.next();
            if (!updateList.contains(id)) {
                updateList.add(id);
            }
        }
    }
    
    /**
     * Queue up segment geometry for an update
     */
    private void update(Entity entity) {
		
		if (entity != null) {
			VertexEntity[] v = new VertexEntity[2];
			if (entity instanceof VertexEntity) {
				v[0] = (VertexEntity)entity;
				for (Iterator<SegmentEntity> i = segmentMap.values().iterator(); i.hasNext();) {
					SegmentEntity se = i.next();
					VertexEntity v0 = se.getStartVertexEntity();
					if (v0 == v[0]) {
						v[1] = se.getEndVertexEntity();
						break;
					}
					VertexEntity v1 = se.getEndVertexEntity();
					if (v1 == v[0]) {
						v[1] = se.getStartVertexEntity();
						break;
					}
				}
			} else if (entity instanceof SegmentEntity) {
				SegmentEntity se = (SegmentEntity)entity;
				int id = se.getEntityID();
				if (!updateList.contains(id)) {
					updateList.add(id);
				}
				v[0] = se.getStartVertexEntity();
				v[1] = se.getEndVertexEntity();
			}
			for (Iterator<SegmentEntity> i = segmentMap.values().iterator(); i.hasNext();) {
				SegmentEntity se = i.next();
				VertexEntity v0 = se.getStartVertexEntity();
				VertexEntity v1 = se.getEndVertexEntity();
				for (int j = 0; j < 2; j++) {
					if ((v[j] == v0) || (v[j] == v1)) {
						int id = se.getEntityID();
						if (!updateList.contains(id)) {
							updateList.add(id);
						}
						break;
					}
				}
			}
		}
    }
    
    /**
     * Initialize the local nodes
     */
    private void init() {
        
        transformGroup = new TransformGroup();
        //////////////////////////////////////////////////////////////
        // rem: hard coded, relationship between the walls and the floor
        Matrix4f mtx = new Matrix4f();
        mtx.setIdentity();
        AxisAngle4f rot = new AxisAngle4f(1, 0, 0, (float)Math.PI/2);
        mtx.setRotation(rot);
        transformGroup.setTransform(mtx);
        //////////////////////////////////////////////////////////////
        
        rootGroup.addChild(transformGroup);
    }
		
    /**
     * Return the segment depth
     *
	 * @param segment The segment to get the data from.
     * @return The segment depth
     */
    private float getSegmentThickness(SegmentEntity segment) {

        Object prop = segment.getProperty(
        	Entity.EDITABLE_PROPERTIES,
        	SegmentEntity.WALL_THICKNESS_PROP);

        float depth = SegmentEntity.DEFAULT_WALL_THICKNESS;
        if (prop instanceof ListProperty) {
            ListProperty list = (ListProperty)prop;
            depth = Float.parseFloat(list.getSelectedValue());
        }

        return(depth);
    }
}
