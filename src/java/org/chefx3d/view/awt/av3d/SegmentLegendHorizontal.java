/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.awt.Color;
import java.awt.Font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import java.util.prefs.Preferences;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.LineStripArray;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.TransformGroup;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityChildListener;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.EntityUtils;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.ZoneEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.preferences.PersistentPreferenceConstants;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Helper class that produces and displays text labels and guidelines
 * along the top horizontal edge of wall segments during thumbnail capture ops.
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
class SegmentLegendHorizontal implements 
	LegendHandler,
	EntityChildListener,
	EntityPropertyListener,
	NodeUpdateListener, 
	PerFrameObserver {
	
	/** Local debug flag */
	private static final boolean DEBUG = false;
	
	/** Separation of the guage line from the segment edge, in meters */
	private static final float GAUGE_OFFSET = 0.025f;
	
	/** Primary hash mark length, in meters */
	private static final float HASH_MARK_LENGTH = 0.065f;
	
	/** Text height, in meters */
	private static final float TEXT_HEIGHT = 0.065f;
	
    /** The world model */
    protected WorldModel model;

	/** The scene manager observer */
	private SceneManagerObserver mgmtObserver;
	
	/** The manager of the entities to be handled */
	private AV3DEntityManager entityManager;
	
    /** The list of all entities in the active zone */
    private HashMap<Integer, Entity> entityMap;

	/** The root group node */
	private Group group;
	
	/** The local SwitchGroup */
	private SwitchGroup switchGroup;
	
	/** Flag indicating that the switch should be changed */
	private boolean config_switch;
	
	/** The working SwitchGroup index */
	protected int index;
	
	/** The local Content Group */
	private Group contentGroup;
	
	/** The line geometry */
	private LineStripArray gaugeLine;
	
	/** The SegmentEntity to be handled */
	private SegmentEntity segment;
	
	/** Array list of nodes to be incorporated */
	private ArrayList<Node> nodeAddList;
	
	/** Array list of nodes to be removed */
	private ArrayList<Node> nodeRemoveList;
	
	/** Flag indicating that the geometry should be changed */
	private boolean config_line;
	
	/** Flag indicating that the labels should be changed */
	private boolean config_labels;
	
	/** Flag indicating the display state */
	private boolean enabled;
	
	/** Scratch */
	private float[] min;
	private float[] max;
	private float[] offset;
	
	/** Gauge line geometry params */
	private float[] coord;
	private int[] strips;
	private float[] empty_coord;
	private int[] empty_strips;
	
	/** Local transformation utils */
	private TransformUtils tu;
	private EntityUtils eu;
	private Matrix4f mtx;
	private Vector3f translation;
	
	/** Flag indicating the measurement system */
	private boolean isMetric;
	
	/** List of entities that are 'sections' */
	private ArrayList<Entity> sectionList;
	
	/** Flag indicating that this component must watch for updates
	 * to the scene. */
	private boolean isDynamic;
	
	/** Flag indicating that update should be called */
	private boolean config_content;
	
	/**
	 * Constructor
	 *
	 * @param model The world model
	 * @param mgmtObserver The SceneManagerObserver
	 * @param group The grouping node to use for the labels
	 */
	SegmentLegendHorizontal(
		WorldModel model,
		SceneManagerObserver mgmtObserver, 
		Group group) {
		
		this.model = model;
		isDynamic = (model != null);
		
		this.mgmtObserver = mgmtObserver;
		mgmtObserver.addObserver(this);
		
		this.group = group;
		
        entityMap = new HashMap<Integer, Entity>();

		nodeAddList = new ArrayList<Node>();
		nodeRemoveList = new ArrayList<Node>();
		
		sectionList = new ArrayList<Entity>();
		
		index = -1;
		
		min = new float[3];
		max = new float[3];
		offset = new float[3];
		
		empty_coord = new float[0];
		empty_strips = new int[0];
		
		tu = new TransformUtils();
		if (isDynamic) {
			eu = new EntityUtils(model);
		}
		mtx = new Matrix4f();
		translation = new Vector3f();
		
		String appName = (String)ApplicationParams.get(
			ApplicationParams.APP_NAME);
		Preferences prefs = Preferences.userRoot().node(appName);
		String unitLabel = prefs.get(
			PersistentPreferenceConstants.UNIT_OF_MEASUREMENT, 
			PersistentPreferenceConstants.DEFAULT_UNIT_OF_MEASUREMENT);
		Unit unit = UnitConversionUtilities.getUnitByCode(unitLabel);
		String system_string = UnitConversionUtilities.getMeasurementSystem(unit);
		isMetric = system_string.equals(UnitConversionUtilities.METRIC);
		
		initSceneGraph();
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameObserver
	//---------------------------------------------------------------

	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrame() {
		
		if (config_content) {
			update();
			config_content = false;
		}
		if (config_switch) {
			mgmtObserver.requestBoundsUpdate(switchGroup, this);
		} 
		if (config_labels) {
			mgmtObserver.requestBoundsUpdate(contentGroup, this);
		} 
		if (config_line) {
			mgmtObserver.requestBoundsUpdate(gaugeLine, this);
		}
	}
	
	//----------------------------------------------------------
	// Methods defined by NodeUpdateListener
	//----------------------------------------------------------
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that could potentially effect the node's bounds.
	 *
	 * @param src The Node that is to be updated.
	 */
	public void updateNodeBoundsChanges(Object src) {
		
		if (src == group) {
			
			group.addChild(switchGroup);
			
		} else if (src == switchGroup) {
			
			switchGroup.setActiveChild(index);
			
			config_switch = false;
			
		} else if (src == contentGroup) {
			
			int numToAdd = nodeAddList.size();
			if (numToAdd > 0) {
				for (int i = 0; i < numToAdd; i++) {
					Node node = nodeAddList.get(i);
					contentGroup.addChild(node);
				}
			}
			int numToRemove = nodeRemoveList.size();
			if (numToRemove > 0) {
				for (int i = 0; i < numToRemove; i++) {
					Node node = nodeRemoveList.get(i);
					contentGroup.removeChild(node);
				}
				nodeRemoveList.clear();
			}
			
			nodeRemoveList.addAll(nodeAddList);
			nodeAddList.clear();
			
			config_labels = false;
			
		} else if (src == gaugeLine) {
			
			gaugeLine.setVertices(LineStripArray.COORDINATE_3, coord);
			gaugeLine.setStripCount(strips, strips.length);
			
			config_line = false;
		}
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The Node that is to be updated.
	 */
	public void updateNodeDataChanges(Object src) {
	}
	
	//----------------------------------------------------------
	// Methods defined by LegendHandler
	//----------------------------------------------------------
	
	/**
	 * Set the active entity manager 
	 *
	 * @param entityManager The active entity manager 
	 */
	public void setEntityManager(AV3DEntityManager entityManager) {
		
		this.entityManager = entityManager;
	}
	
	/**
	 * Set the active zone entity
	 *
	 * @param ze The active zone entity
	 */
	public void setActiveZoneEntity(ZoneEntity ze) {
		
		boolean zoneChanged = (segment != ze);
		if (zoneChanged) {
			if (segment != null) {
				recursiveRemove(segment);
			}
			if ((ze != null) && (ze.getType() == Entity.TYPE_SEGMENT)) {
				segment = (SegmentEntity)ze;
				recursiveAdd(segment);
			} else {
				segment = null;
			}
			//update();
			config_content = true;
		}
	}
	
	/**
	 * Enable the display of the components
	 *
	 * @param state The display of the components
	 */
	public void setEnabled(boolean state) {
		
		if (enabled != state) {
			
			// the display state has changed
			enabled = state;
			
			if (enabled) {
				// switch on
				index = 0;
				config_switch = true;
				
				// generate the labels and line set on a switch to enabled
				//update();
				config_content = true;
				 
			} else {
				if (nodeRemoveList.size() > 0) {
					// remove the labels, if they exist
					config_labels = true;
				}
				// remove the line set
				coord = empty_coord;
				strips = empty_strips;
				config_line = true;
				
				// switch off
				index = -1;
				config_switch = true;
			}
		}
	}
	
    //----------------------------------------------------------
    // Methods for EntityChildListener
    //----------------------------------------------------------

    /**
     * A child was added.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childAdded(int parentID, int childID) {

        Entity childEntity = model.getEntity(childID);

        if (childEntity != null) {
            boolean sectionAdded = recursiveAdd(childEntity);
			if (sectionAdded) {
				//update();
				config_content = true;
			}
        }
    }

    /**
     * A child was inserted.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     * @param index The index the child was inserted at
     */
    public void childInsertedAt(int parentID, int childID, int index) {

        Entity childEntity = model.getEntity(childID);

        if (childEntity != null) {
            boolean sectionAdded = recursiveAdd(childEntity);
			if (sectionAdded) {
				//update();
				config_content = true;
			}
        }
    }

    /**
     * A child was removed.
     *
     * @param parentID The entity ID of the parent
     * @param childID The entity ID of the child
     */
    public void childRemoved(int parentID, int childID) {

        Entity childEntity = entityMap.get(childID);

        if (childEntity != null) {
			boolean sectionRemoved = recursiveRemove(childEntity);
			if (sectionRemoved) {
				//update();
				config_content = true;
			}
        }
    }

    //----------------------------------------------------------
    // Methods for EntityPropertyListener
    //----------------------------------------------------------

    public void propertiesUpdated(List<EntityProperty> properties) {
        // TODO: should probably do something with this.......
    }

    public void propertyAdded(int entityID, String propertySheet,
        String propertyName) {

    }

    public void propertyRemoved(int entityID, String propertySheet,
        String propertyName) {

    }

    public void propertyUpdated(int entityID, String propertySheet,
		String propertyName, boolean ongoing) {
		
		if (propertyName.equals(PositionableEntity.POSITION_PROP) || 
			propertyName.equals(PositionableEntity.ROTATION_PROP) ||
			propertyName.equals(PositionableEntity.SCALE_PROP)) {
			
			//update();
			config_content = true;
		}
    }

	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/**
	 * Update the state of the displayed components
	 */
	void update() {
		if (enabled) {
			if ((entityManager != null) && (segment != null)) {
				
				// the vertex entities
				VertexEntity left_ve = segment.getStartVertexEntity();
				VertexEntity rght_ve = segment.getEndVertexEntity();
				
				// the vertex heights
				float left_height = left_ve.getHeight();
				float rght_height = rght_ve.getHeight();
				
				float height = Math.max(left_height, rght_height);
				float width = segment.getLength();
				
				float y_min = height + GAUGE_OFFSET;
				float y_max = y_min + HASH_MARK_LENGTH;
				float y_span = y_min + HASH_MARK_LENGTH / 4;
				
				float seg_min = 0;
				float seg_max = width;
				
				ArrayList<float[]> sect_hash_set_list = 
					getSectionHashSets(seg_min, seg_max);
				int num_sect_hash_set = sect_hash_set_list.size();
				int num_sect_hashmark = num_sect_hash_set * 2;
				
				ArrayList<float[]> span_list = 
					getSpans(sect_hash_set_list, seg_min, seg_max);
				int num_span = span_list.size();
				
				int max_num_hashmark = 2 + num_sect_hashmark;
				
				float[] x_hash = new float[max_num_hashmark];
				int hashmark_cnt = 0;
				x_hash[hashmark_cnt++] = seg_min;
				float hash_label_height = y_span + TEXT_HEIGHT;
				for (int i = 0; i < num_sect_hash_set; i++) {
					float[] sect_hash = sect_hash_set_list.get(i);
					float x_min = sect_hash[0];
					float x_max = sect_hash[1];
					if (x_min > seg_min) {
						x_hash[hashmark_cnt++] = x_min;
					}
					if (x_max < seg_max) {
						x_hash[hashmark_cnt++] = x_max;
						// place the hashmark label above and to the 
						// right of the hashmark
						createLabel(x_max, Anchor.BOT_LEFT, x_max, hash_label_height);
					} else if (x_max >= seg_max) {
						createLabel(x_max, Anchor.BOT_LEFT, x_max, hash_label_height);
					}
				}
				x_hash[hashmark_cnt++] = seg_max;
				
				int num_line = num_span + hashmark_cnt;
				int num_vertex = 2 * num_line;
				int num_coord = num_vertex * 3;
				
				coord = new float[num_coord];
				strips = new int[num_line];
				
				// generate the coords of the hash marks, bottom to top
				int idx = 0;
				int s = 0;
				for (int i = 0; i < hashmark_cnt; i++) {
					coord[idx++] = x_hash[i];
					coord[idx++] = y_min;
					coord[idx++] = 0;
					coord[idx++] = x_hash[i];
					coord[idx++] = y_max;
					coord[idx++] = 0;
					strips[s++] = 2;
				}
				// generate the coords of the spans, left to right
				for (int i = 0; i < num_span; i++) {
					float[] span = span_list.get(i);
					float x_min = span[0];
					float x_max = span[1];
					
					coord[idx++] = x_min;
					coord[idx++] = y_span;
					coord[idx++] = 0;
					coord[idx++] = x_max;
					coord[idx++] = y_span;
					coord[idx++] = 0;
					strips[s++] = 2;
					
					float span_width = x_max - x_min;
					float span_center = (x_min + x_max) * 0.5f;
					// place the span label, centered above the mid point of the span
					createLabel(span_width, Anchor.BOT_CENTER, span_center, y_span);
				}
				config_line = true;
				config_labels = true;
				
			} else {
				if (nodeRemoveList.size() > 0) {
					// remove the labels, if they exist
					config_labels = true;
				}
				// remove the line set
				coord = empty_coord;
				strips = empty_strips;
				config_line = true;
			}
		}
	}
	
	/**
	 * Create a label for the specified value
	 *
	 * @param value The value to creat a label for
	 * @param anchor The anchor position of the label
	 * @param x The x position along the gauge line
	 * @param y The y position of the gauge line
	 */
	private void createLabel(float value, Anchor anchor, float x, float y) {
		
		String label;
		if (isMetric) {
			label = UnitConversionUtilities.getFormatedNumberDisplay(Unit.METERS, value);
		} else {
			float value_inches = UnitConversionUtilities.convertMetersTo(value, Unit.INCHES);
			label = UnitConversionUtilities.getFormatedNumberDisplay(Unit.INCHES, value_inches);
		}
		
		// the position to place the label anchor
		offset[0] = x;
		offset[1] = y;
		offset[2] = 0;
		
		LegendText lt = new LegendText(mgmtObserver);
		lt.update(label, TEXT_HEIGHT, anchor, offset);
		
		nodeAddList.add(lt.getShape());
	}
	
    /**
     * Walk through the children of the argument entity,
     * adding listeners as necessary.
     *
     * @param entity The entity to start with
	 * @return true if a section was added, false if not.
     */
    private boolean recursiveAdd(Entity entity) {

		boolean sectionFound = false;
		
        entityMap.put(entity.getEntityID(), entity);

		if (isDynamic) {
        	entity.addEntityChildListener(this);
		}
		if (isSection(entity)) {
			if (isDynamic) {
				entity.addEntityPropertyListener(this);
			}
			sectionList.add(entity);
			sectionFound = true;
		}
        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                sectionFound |= recursiveAdd(child);
            }
        }
		return(sectionFound);
    }

    /**
     * Walk through the children of the argument entity,
     * removing listeners as necessary.
     *
     * @param entity The entity to start with
	 * @return true if a section was removed, false if not.
     */
    private boolean recursiveRemove(Entity entity) {

		boolean sectionFound = false;
		
        entityMap.remove(entity.getEntityID());

		if (isDynamic) {
        	entity.removeEntityChildListener(this);
		}
		if (isSection(entity)) {
			if (isDynamic) {
				entity.removeEntityPropertyListener(this);
			}
			sectionList.remove(entity);
			sectionFound = true;
		}
        if (entity.hasChildren()) {
            ArrayList<Entity> childList = entity.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                sectionFound |= recursiveRemove(child);
            }
        }
		return(sectionFound);
    }
	
	/**
	 * Determine whether the argument is a section 
	 *
	 * @param entity The entity to check
	 * @return true if the entity is a section, false if not
	 */
	private boolean isSection(Entity entity) {
		
		boolean isSection = false;
		if (!DEBUG) {
			Boolean isSection_prop = (Boolean)entity.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.IS_SECTIONAL_PRODUCT);
			if ((isSection_prop != null) && isSection_prop) {
				isSection = true;
			}
		} else {
			String[] cls = (String[])entity.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.CLASSIFICATION_PROP);
			if (cls != null) {
				for (int j = 0; j < cls.length; j++) {
					if (cls[j].equals("par")) {
						isSection = true;
						break;
					}
				}
			}
		}
		return(isSection);
	}

	/**
	 * Return an ordered list of distances to section hashmarks
	 *
	 * @param seg_min The starting point of the segment. Any
	 * section to the left of this point will be ignored.
	 * @param seg_max The ending point of the segment. Any section
	 * to the right of this point will be ignored.
	 * @return An ordered list of distances to section hashmarks.
	 */
	private ArrayList<float[]> getSectionHashSets(float seg_min, float seg_max) {
		
		HashMap<Integer, AV3DEntityWrapper> wrapperMap =
			entityManager.getAV3DEntityWrapperMap();
		
		AV3DEntityWrapper zoneWrapper = 
			wrapperMap.get(segment.getEntityID());
		
		ArrayList<float[]> hash_list = new ArrayList<float[]>();
		int num_section = sectionList.size();
		int idx = 0;
		for (int i = 0; i < num_section; i++) {
			Entity section = sectionList.get(i);
			int id = section.getEntityID();
			AV3DEntityWrapper sectionWrapper = wrapperMap.get(id);
			if (sectionWrapper != null) {
				
				// transform the bounds of the section into the 
				// zone's (and layer's) coordinate system
				//
				if (!isDynamic) {
					tu.getLocalToVworld(
						sectionWrapper.transformGroup, 
						zoneWrapper.transformGroup, 
						mtx);
				} else {
					eu.getTransformToZone(section, mtx);
				}
				
				OrientedBoundingBox bnd = sectionWrapper.getBounds();
				bnd.transform(mtx);
				bnd.getExtents(min, max);
				float min_x = min[0];
				float max_x = max[0];
				
				if ((min_x < seg_max) && (max_x > seg_min))  {
					// if the section is within the 'bounds' of the segment
					// place it on the list, in left to right order
					boolean insert = false;
					
					float[] hash_set = new float[]{min_x, max_x};
					
					for (int j = 0; j < hash_list.size(); j++) {
						float[] h = hash_list.get(j);
						if (min_x < h[0]) {
							hash_list.add(j, hash_set);
							insert = true;
							break;
						}
					}
					if (!insert) {
						hash_list.add(hash_set);
					}
				}
			}
		}
		return(hash_list);
	}
	
	/**
	 * Return the spans between the sections of the argument list
	 *
	 * @param sect_list The ordered section mark sets
	 * @param seg_min The starting point of the segment.
	 * @param seg_max The ending point of the segment.
	 * @return The coordinates of spans between sections.
	 */
	private ArrayList<float[]> getSpans(
		ArrayList<float[]> sect_list, 
		float seg_min, 
		float seg_max) {
		
		ArrayList<float[]> span_list = new ArrayList<float[]>();
		
		float span_start = seg_min;
		int num_section = sect_list.size();
		for (int i = 0; i < num_section; i++) {
			float[] sect = sect_list.get(i);
			float sect_min_x = sect[0];
			float sect_max_x = sect[1];
			if (sect_min_x > span_start) {
				span_list.add(new float[]{span_start, sect_min_x});
			}
			span_start = sect_max_x;
		}
		if (seg_max > span_start) {
			span_list.add(new float[]{span_start, seg_max});
		}
		return(span_list);
	}
	
    /**
     * Initialize the scene graph objects
     */
    private void initSceneGraph() {
		
		contentGroup = new Group();
		
		switchGroup = new SwitchGroup();
		switchGroup.addChild(contentGroup);
		switchGroup.setActiveChild(index);
		
		gaugeLine = new LineStripArray();
		gaugeLine.setSingleColor(false, new float[]{0, 0, 0});
		
		Shape3D gaugeLineShape = new Shape3D();
		gaugeLineShape.setGeometry(gaugeLine);
		
		contentGroup.addChild(gaugeLineShape);
		
		mgmtObserver.requestBoundsUpdate(group, this);
	}
}
