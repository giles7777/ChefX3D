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

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.ViewEnvironment;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityChildListener;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;

import org.chefx3d.util.ApplicationParams;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Handler for configuring entity visibility based on the viewpoint
 *
 * @author Rex Melton
 * @version $Revision: 1.20 $
 */
class PreviewVisibilityHandler implements
    NavigationStatusListener,
    EntityChildListener,
    SegmentListener,
    PerFrameObserver {

    /** Transparency value param key */
    private static final String PREVIEW_TRANSPARENCY_VALUE_KEY =
        "previewTransparencyValue";

    /** Start fad angle */
    private static final float START = (float)(Math.PI / 2); // 90 degrees

    /** End fade angle */
    private static final float END = (float)(120 * Math.PI / 180); // 120 degrees

    /** Product visibility threshold */
    private static final float THRESHOLD = (float)(100 * Math.PI / 180); // 100 degrees

    /** Transparency value for opaque walls */
    private static final float OPAQUE = 1.0f;

    /** Transparency value for semi-transparent walls */
    private static final float DEFAULT_TRANSPARENCY_VALUE = 0.0f;

    /** The scene manager observer */
    private SceneManagerObserver mgmtObserver;

    /** The position of the viewpoint */
    private Vector3f position;

    /** The normal of the segment */
    private Vector3f normal;

    /** The center of the segment */
    private Point3f center;

    /** The vector of the viewpoint to the segment start vertex */
    private Vector3f vector;

    /** Base transform */
    private Matrix4f xform;

    /** The current view matrix */
    private Matrix4f viewMatrix;

    /** scratch arrays */
    private double[] vtx0;
    private double[] vtx1;

    /** The manager of the entities to be handled */
    private AV3DEntityManager entityManager;

    /** The manager of the segment entities */
    private MultisegmentManager segmentManager;

    /** wrapper map of the active entity manager */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;

    /** Map of entities (segments and their children), keyed by id */
    private HashMap<Integer, Entity> entityMap;

    /** Scratch array for holding segment wrappers */
    private SegmentEntityWrapper[] seg_wrappers;

    /** True to automatically hide walls when the viewer is behind them */
    private boolean autoHideWalls;

    /**
     * True to automatically hide products on a wall  when the viewer
     * is behind them. This only works if autoHideWalls is enabled.
     */
    private boolean autoHideProducts;

    /** Flags indicating that something about the scene has changed
     *  that warrants a reconfiguration */
    private boolean viewChanged;
    private boolean contentChanged;
	private boolean resetSwitches;

    /** Working transparency value */
    private float transparencyValue;

	/** Enabled flag */
	private boolean enabled;
	
    /**
     * Construct a default instance of this handler with wall hiding enabled
     *
     * @param mgmtObserver The SceneManagerObserver
     * @param matrix Base transform to apply to viewpoints
     */
    PreviewVisibilityHandler(
        SceneManagerObserver mgmtObserver,
        Matrix4f matrix) {

        this.mgmtObserver = mgmtObserver;
		
        xform = new Matrix4f();
		if (matrix != null) {
			xform.set(matrix);
		} else {
			xform.setIdentity();
		}
        autoHideProducts = false;
        autoHideWalls = true;
		enabled = true;

        seg_wrappers = new SegmentEntityWrapper[0];
        entityMap = new HashMap<Integer, Entity>();

        viewMatrix = new Matrix4f();
        position = new Vector3f();
        normal = new Vector3f();
        vector = new Vector3f();
        center = new Point3f();

        vtx0 = new double[3];
        vtx1 = new double[3];

        Object transparencyValue_object =
            ApplicationParams.get(PREVIEW_TRANSPARENCY_VALUE_KEY);
        if ((transparencyValue_object != null) &&
            (transparencyValue_object instanceof Float)) {

            float tmp_value = ((Float)transparencyValue_object).floatValue();
            if ((tmp_value >= 0) && (tmp_value <= 1.0f)) {
                transparencyValue = tmp_value;
            } else {
                transparencyValue = DEFAULT_TRANSPARENCY_VALUE;
            }
        } else {
            transparencyValue = DEFAULT_TRANSPARENCY_VALUE;
        }

        mgmtObserver.addObserver(this);
    }

    //----------------------------------------------------------
    // Methods defined by NavigationStatusListener
    //----------------------------------------------------------

    /**
     * Notification that the view Transform has changed
     *
     * @param mtx The new view Transform matrix
     */
    public void viewMatrixChanged(Matrix4f mtx) {
        viewMatrix.set(mtx);
        viewChanged = true;
    }

    /**
     * Notification that the orthographic viewport size has changed and
     * this is the new frustum details.
     *
     * @param frustumCoords The new coordinates to use in world space
     */
    public void viewportSizeChanged(double[] frustumCoords) {
        // Ignored
    }

    //----------------------------------------------------------
    // Methods defined by SegmentListener
    //----------------------------------------------------------

    /**
     * Segment has been added to the scene
     *
     * @param se The segment entity
     */
    public void segmentAdded(SegmentEntity se) {
        recursiveAdd(se);
        contentChanged = true;
    }

    /**
     * Segment has been removed from the scene
     *
     * @param se The segment entity
     */
    public void segmentRemoved(SegmentEntity se) {
        recursiveRemove(se);
    }

    /**
     * Segment has been modified
     *
     * @param se The segment entity
     */
    public void segmentChanged(SegmentEntity se) {
        contentChanged = true;
    }

    //----------------------------------------------------------
    // EntityChildListener Methods
    //----------------------------------------------------------

    /**
     * Adds the child to the parent and then starts the model loading process.
     *
     * @param parent Entity ID of parent
     * @param child Entity ID of child
     */
    public void childAdded(int parent, int child) {

        Entity parentEntity = entityMap.get(parent);
        if (parentEntity != null) {
            int index = parentEntity.getChildIndex(child);
            childInsertedAt(parent, child, index);
        }
    }

    /**
     * Add the child at the specific location in the list of parent object
     * children.
     *
     * @param parent Entity ID of parent
     * @param child Entity ID of child
     * @param index index to add the child to in the parent child list
     */
    public void childInsertedAt(int parent, int child, int index) {

        Entity parentEntity = entityMap.get(parent);
        if (parentEntity != null) {
            Entity childEntity = parentEntity.getChildAt(index);
            recursiveAdd(childEntity);
            contentChanged = true;
        }
    }

    /**
     * Removes the child from the parent. The request to remove the model is
     * made and on the next render pass it will be removed from the scene.
     *
     * @param parent Entity ID of the parent
     * @param child Entity ID of the child
     */
    public void childRemoved(int parent, int child) {

        Entity childEntity = entityMap.get(child);
        if (childEntity != null) {
            recursiveRemove(childEntity);
        }
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
	public void processNextFrame() {
		if (enabled) {
			boolean reconfigure = false;
			if (resetSwitches) {
				for (Iterator<AV3DEntityWrapper> i = wrapperMap.values().iterator(); i.hasNext();) {
					AV3DEntityWrapper wrapper = i.next();
					int type = wrapper.entity.getType();
					switch (type) {
					case Entity.TYPE_GROUNDPLANE_ZONE:
					case Entity.TYPE_SEGMENT:
					case Entity.TYPE_MODEL:
					case Entity.TYPE_MODEL_WITH_ZONES:
						wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
					}
				}
				reconfigure = true;
				resetSwitches = false;
			}
			if (viewChanged) {
				reconfigure = true;
				viewChanged = false;
			}
			if (contentChanged) {
				reconfigure = true;
				contentChanged = false;
			}
			if (reconfigure) {
				if (entityManager != null) {
					viewMatrix.get(position);
					xform.transform(position);
					
					HashMap<Integer, SegmentEntityWrapper> segmentMap =
						entityManager.getSegmentEntityWrapperMap();
					
					int num_seg = segmentMap.size();
					seg_wrappers = segmentMap.values().toArray(seg_wrappers);
					for (int i = 0; i < num_seg; i++) {
						
						SegmentEntityWrapper sew = seg_wrappers[i];
						seg_wrappers[i] = null;
						SegmentEntity se = sew.segment;
						sew.getWorldNormal(normal);
						sew.getWorldCenter(center);
						
						vector.set(
							(position.x - center.x),
							(position.y - center.y),
							(position.z - center.z));
						
						float angle = vector.angle(normal);
						
						float wallTransparency = OPAQUE;
						boolean isObjectVisible = true;
						if (autoHideWalls) {
							if (angle <= START) {
								// opaque
								wallTransparency = OPAQUE;
							} else if ((angle > START) && (angle < END)) {
								// fade
								wallTransparency = OPAQUE - (OPAQUE - transparencyValue) *
									(angle - START)/(END - START);
								isObjectVisible = false;
							} else {
								// semi transparent
								wallTransparency = transparencyValue;
								isObjectVisible = false;
							}
						}
						sew.setTransparency(wallTransparency);
						
						boolean isProductVisible = true;
						if (autoHideProducts) {
							if (angle > THRESHOLD) {
								isProductVisible = false;
							}
						}
						
						if (se.hasChildren()) {
							setVisibility(se.getChildren(), isProductVisible, isObjectVisible);
						}
					}
				}
			}
		}
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Set the enabled state
	 * 
	 * @param enabled The state
	 */
	void setEnabled(boolean enabled) {
		this.enabled = enabled;
		//if (enabled) {
		if (wrapperMap != null) {
			resetSwitches = true;
		}
	}
	
	/**
	 * Set the navigation transform to use
	 * 
	 * @param matrix The navigation transform to use
	 */
	void setNavigationMatrix(Matrix4f matrix) {
		if (matrix != null) {
			xform.set(matrix);
		} else {
			xform.setIdentity();
		}
		viewChanged = true;
	}
	
    /**
     * Set the active entity manager
     *
     * @param em The active entity manager
     */
    void setEntityManager(AV3DEntityManager em) {
        if (entityManager != null) {
            SegmentableEntity multisegment = entityManager.getSegmentableEntity();
            if (multisegment != null) {
                ArrayList<SegmentEntity> seg_list = multisegment.getSegments();
                if (seg_list != null) {
                    int num_seg = seg_list.size();
                    for (int i = 0; i < num_seg; i++) {
                        SegmentEntity se = seg_list.get(i);
                        recursiveRemove(se);
                    }
                }
            }
        }
        if (segmentManager != null) {
            segmentManager.removeSegmentListener(this);
        }
        entityManager = em;
        segmentManager = null;
        wrapperMap = null;

        if (entityManager != null) {

            segmentManager = entityManager.getMultisegmentManager();
            segmentManager.addSegmentListener(this);
            wrapperMap = entityManager.getAV3DEntityWrapperMap();

            SegmentableEntity multisegment = entityManager.getSegmentableEntity();
            if (multisegment != null) {
                ArrayList<SegmentEntity> seg_list = multisegment.getSegments();
                if (seg_list != null) {
                    int num_seg = seg_list.size();
                    for (int i = 0; i < num_seg; i++) {
                        SegmentEntity se = seg_list.get(i);
                        recursiveAdd(se);
                    }
                }
            }
        }
		resetSwitches = true;
		segmentChanged(null);
    }

    /**
     * Set auto hide walls
     *
     * @param state The enable state for auto hide walls
     */
    void setAutoHideWalls(boolean state) {
        autoHideWalls = state;
    }

    /**
     * Set auto hide products
     *
     * @param state The enable state for auto hide products
     */
    void setAutoHideProducts(boolean state) {
        autoHideProducts = state;
    }

    /**
     * Set the model geometry of the list of entities to the
     * specified visibility state
     *
     * @param children The list of entities
     * @param pstate Visibility state for products, true for visible, false for not
     * @param ostate Visibility state for objects, true for visible, false for not
     */
    private void setVisibility(ArrayList<Entity> children, boolean pstate, boolean ostate) {
        for (int i = 0; i < children.size(); i++) {
            Entity child = children.get(i);
            if ((child != null) && (child.isModel())) {
                // wall product & props
                AV3DEntityWrapper wrapper = wrapperMap.get(child.getEntityID());
                if (wrapper != null) {
					String category = child.getCategory();
					boolean state;
					if (category.equals("Category.Window") || 
						category.equals("Category.Door") ||
						category.equals("Category.Object")) {
						// object
						state = ostate;
					} else {
						// product
						state = pstate;
					}
					
                    if (state) {
                        wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_MODEL);
                    } else {
                        wrapper.setSwitchGroupIndex(AV3DEntityWrapper.CONTENT_NONE);
                    }
                }
            }
            if (child.hasChildren()) {
                setVisibility(child.getChildren(), pstate, ostate);
            }
        }
    }

    /**
     * Walk through the children of the argument entity,
     * adding listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveAdd(Entity entity) {

        entity.addEntityChildListener(this);

        entityMap.put(entity.getEntityID(), entity);

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveAdd(child);
            }
        }
    }

    /**
     * Walk through the children of the argument entity,
     * removing listeners as necessary.
     *
     * @param entity The entity to start with
     */
    private void recursiveRemove(Entity entity) {

        entity.removeEntityChildListener(this);

        entityMap.remove(entity.getEntityID());

        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                recursiveRemove(child);
            }
        }
    }
}
