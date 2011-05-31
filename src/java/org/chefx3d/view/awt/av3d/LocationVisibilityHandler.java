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

// External Imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.vecmath.Matrix4f;

// Local Imports
import org.chefx3d.model.ContentContainerEntity;
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.util.ConfigManager;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Handler for configuring entity visibility based on the editing zone
 *
 * @author Rex Melton
 * @version $Revision: 1.19 $
 */
class LocationVisibilityHandler implements PerFrameObserver, ConfigListener {
	
	/** boolean to show if the vertices are visible or not at zone level */
	private static final boolean DEFAULT_ZONE_VERTEX_VISIBILITY = false; 
	
	/** Default proximity range value */
	private static final float DEFAULT_PROXIMITY_RANGE = 0.0f; 
	
    /** The scene manager observer */
    private SceneManagerObserver mgmtObserver;

	/** The manager of the entities to be handled */
	private AV3DEntityManager entityManager;
	
	/** wrapper map of the active entity manager */
	private HashMap<Integer, AV3DEntityWrapper> wrapperMap;
	
	/** The active zone entity */
	private ZoneEntity activeZoneEntity;
	
	/** The active zone's parent, if the zone is type product zone */
	private int zoneParentEntityID;
	
	/** Flag indicating that the active zone has changed */
	private boolean zoneChanged;
	
	/** Flag indicating that the entity hierarchy has changed */
	private boolean entityChanged;
	
    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f mtx;
    
	/** Flag indicating whether checking the bounds of objects against the
	 *  active zone for visibility is required */
	private boolean proximityEnabled;
	
	/** Bounding object for testing proximity bounds */
	private OrientedBoundingBox proximityBounds;
	
	/** Scratch arrays for manipulating the bounding objects */
	private float[] min;
	private float[] max;
	private float[] scl;
	
	/** The proximity bounds range increment */
	private float proximityRange;
	
	/**
	 * Constructor
	 *
     * @param mgmtObserver The SceneManagerObserver
	 */
	LocationVisibilityHandler(SceneManagerObserver mgmtObserver) {
		
        this.mgmtObserver = mgmtObserver;
        mgmtObserver.addObserver(this);
		
		proximityBounds = new OrientedBoundingBox();
		
		min = new float[3];
		max = new float[3];
		scl = new float[3];
		
        tu = new TransformUtils();
        mtx = new Matrix4f();
		
		proximityRange = DEFAULT_PROXIMITY_RANGE;
		ConfigManager cm = ConfigManager.getManager();
		String value_string = cm.getProperty("visibleBoundsProximityRange");
		if (value_string != null) {
			try {
				float value = Float.parseFloat(value_string);
				if (value >= 0) {
					proximityRange = value;
				}
			} catch (NumberFormatException nfe) {
				// should throw out a catchy message here......
			}
		}
	}
	
    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {
		if (zoneChanged || entityChanged) {
			configSceneGraph();
			zoneChanged = false;
			entityChanged = false;
		}
	}
	
    //----------------------------------------------------------
    // Methods defined by ConfigListener
    //----------------------------------------------------------

    /**
     * Set the active zone entity
     *
     * @param ze The zone entity that is active
     */
    public void setActiveZoneEntity(ZoneEntity ze) {
		if (activeZoneEntity != ze) {
			activeZoneEntity = ze;
			zoneChanged = true;
			if (activeZoneEntity == null) {
				zoneParentEntityID = -1;
			} else {
				int zoneType = activeZoneEntity.getType();
				if (zoneType == Entity.TYPE_MODEL_ZONE) {
					zoneParentEntityID = ze.getParentEntityID();
				} else {
					zoneParentEntityID = -1;
				}
			}
		}
	}

	/**
	 * Set the active entity manager 
	 *
	 * @param entityManager The active entity manager 
	 */
	public void setEntityManager(AV3DEntityManager entityManager) {
		this.entityManager = entityManager;
		if (entityManager != null) {
			wrapperMap = entityManager.getAV3DEntityWrapperMap();
		} else {
			wrapperMap = null;
		}
	}
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Set the entity changed flag
	 */
	void setEntityChanged() {
		entityChanged = true;
	}
	
	/**
	 * Set the proximity enabled flag
	 *
	 * @param state The new state of the proximity enabled flag
	 */
	void setProximityEnabled(boolean state) {
		if (state != proximityEnabled) {
			proximityEnabled = state;
			if (activeZoneEntity != null) {
				// set a flag to cause the visibility to be re-evaluated
				zoneChanged = true;
			}
		}
	}
	
    /**
     * Manage the scene graph state involved with a entity selection change
     */
    private void configSceneGraph() {
		
        // based on the selected entity (and other state information....)
        // configure the scenegraph as necessary
		
		if ((activeZoneEntity != null) && (entityManager != null)) {
			
			int activeZoneID = activeZoneEntity.getEntityID();
			
			AV3DEntityWrapper zoneWrapper = wrapperMap.get(activeZoneID);
			if (zoneWrapper != null) {
				
				if (proximityEnabled) {
					OrientedBoundingBox zoneBounds = zoneWrapper.getBounds();
					zoneBounds.getVertices(min, max);
					zoneBounds.getScale(scl);
					
					min[0] -= proximityRange;
					min[1] -= proximityRange;
					min[2] -= proximityRange;
					
					max[0] += proximityRange;
					max[1] += proximityRange;
					max[2] += proximityRange;
					
					proximityBounds.set(min, max, scl);
					tu.getLocalToVworld(zoneWrapper.transformGroup, mtx);
					proximityBounds.transform(mtx);
				}
				
				ContentContainerEntity cce =
					entityManager.getContentContainerEntity();
				
				if (cce != null) {
					
					boolean showFloor = false;
					boolean showFloorChildren = false;
					boolean showWall = false;
					boolean showWallFacade = false;
					boolean showVertex = false;
					boolean showWallChildren = false;
					
					int wallToShowID = -1;
					
					int type = activeZoneEntity.getType();
					switch (type) {
					case Entity.TYPE_GROUNDPLANE_ZONE:
						showFloor = true;
						showFloorChildren = true;
						showWall = true;
						showWallFacade = false;
						showVertex = DEFAULT_ZONE_VERTEX_VISIBILITY;
						showWallChildren = false;
						wallToShowID = -1;
						break;
						
					case Entity.TYPE_SEGMENT:
						showFloor = false;
						showFloorChildren = false;
						showWall = true;
						showWallFacade = true;
						showVertex = false;
						showWallChildren = true;
						
						wallToShowID = activeZoneID;
						break;
						
					}
					
					// if there is content, there is something to work with
					
					ArrayList<Entity> contentChildren = cce.getChildren();
					
					for (int i = 0; i < contentChildren.size(); i++) {
						Entity contentChild = contentChildren.get(i);
						if (contentChild != null) {
							int contentType = contentChild.getType();
							if (contentType == Entity.TYPE_GROUNDPLANE_ZONE) {
								
								AV3DEntityWrapper wrapper = wrapperMap.get(contentChild.getEntityID());                                    
								
								boolean isVisible = showFloorChildren;
								if (contentChild == activeZoneEntity) {
									isVisible = true;
									wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
									wrapper.setTransparency(1.0f);
								} else {
									isVisible = false;
									wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_BOUNDS);
								}
								
								// handle the children
								if (contentChild.hasChildren()) {
									setVisibility(contentChild.getChildren(), isVisible);
								}
								
							} else if (contentType == Entity.TYPE_MULTI_SEGMENT) {
								// the wall container
								ArrayList<Entity> msChildren = contentChild.getChildren();
								for (int j = 0; j < msChildren.size(); j++) {
									Entity msc_entity = msChildren.get(j);
									if (msc_entity != null) {
										if (msc_entity.getType() == Entity.TYPE_VERTEX) {
											// a vertex
											
											AV3DEntityWrapper wrapper =
												wrapperMap.get(msc_entity.getEntityID());
											
											if (contentChild instanceof SegmentableEntity) {
												ArrayList<VertexEntity> vertexList =
													((SegmentableEntity)contentChild).getVertices();
												if (vertexList.size() == 1) {
													wrapper.setTransparency(1.0f);
												} else {
													wrapper.setTransparency(0.0f);
												}
												
											} else if (wrapper != null) {
												if (showVertex) {
													wrapper.setTransparency(1.0f);
												} else {
													wrapper.setTransparency(0.0f);
												}
											}
										} else if (msc_entity.getType() == Entity.TYPE_SEGMENT) {
											// a wall
											int seg_id = msc_entity.getEntityID();
											AV3DEntityWrapper wrapper = wrapperMap.get(seg_id);
											if (wrapper != null) {
												if ((showWall) && ((wallToShowID == -1) || (wallToShowID == seg_id))) {
													if (showWallFacade) {
														wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_FACADE);
													} else {
														wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
														wrapper.setTransparency(1.0f);
													}
												} else {
													wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_BOUNDS);
												}
											}
											if (msc_entity.hasChildren()) {
												boolean isVisible = ((showWallChildren) && 
													((seg_id == -1) || (seg_id == wallToShowID)));
												setVisibility(msc_entity.getChildren(), isVisible);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Set the model geometry of the list of entities to the 
	 * specified visibility state
	 *
	 * @param children The list of entities
	 * @param state Visibility state, true for visible, false for not
	 */
	private void setVisibility(ArrayList<Entity> children, boolean state) {
		for (int i = 0; i < children.size(); i++) {
			Entity child = children.get(i);
			if (child != null) {
				int childID = child.getEntityID();
				boolean child_vis_state = state;
				AV3DEntityWrapper wrapper = wrapperMap.get(childID);
				if (wrapper != null) {
					if (child.isModel()) {
						// products & props
						if (childID == zoneParentEntityID) {
							// the parent of a product zone, and it's children
							// are always visible
							wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
							wrapper.setTransparency(1.0f);
							child_vis_state = true;
							
						} else if (state) {
							
							wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
							wrapper.setTransparency(1.0f);
							
						} else {
							
							wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_BOUNDS);
							if (proximityEnabled) {
								OrientedBoundingBox obb = wrapper.getBounds();
								tu.getLocalToVworld(wrapper.transformGroup, mtx);
								obb.transform(mtx);
								
								wrapper.setBoundsVisible(proximityBounds.intersect(obb));
							}
						}
					} else if (child.isZone()) {
						if (child == activeZoneEntity) {
							
							wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
							child_vis_state = true;
							
						} else {
							
							wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_NONE);
							child_vis_state = false;
						}
					}
				}
				if (child.hasChildren()) {
					setVisibility(child.getChildren(), child_vis_state);
				}
			}
		}
	}
}
