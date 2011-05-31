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
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.preferences.PersistentPreferenceConstants;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Helper class that produces and displays text labels and guidelines
 * along the left vertical edge of wall segments during thumbnail capture ops.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
class SegmentLegendVertical implements 
	LegendHandler, 
	NodeUpdateListener, 
	PerFrameObserver {
	
	/** Separation of the gauge line from the segment edge, in meters */
	private static final float GAUGE_OFFSET = 0.025f;
	
	/** Primary hash mark length, in meters */
	private static final float PRIMARY_HASH_MARK_LENGTH = 0.050f;
	
	/** The number of increments between primary marks */
	private static final int INC_METRIC = 20;
	
	/** Secondary hash mark length, in meters */
	private static final float SECONDARY_HASH_MARK_LENGTH = 0.025f;
	
	/** The number of increments between primary marks */
	private static final int INC_IMPERIAL = 4;
	
	/** Text height, in meters */
	private static final float TEXT_HEIGHT = 0.065f;
	
	/** The scene manager observer */
	private SceneManagerObserver mgmtObserver;
	
	/** The group node to place content into */
	private Group group;
	
	/** The SegmentEntity to be handled */
	private SegmentEntity segment;
	
	/** Array list of nodes to be incorporated */
	private ArrayList<Node> nodeAddList;
	
	/** Array list of nodes to be removed */
	private ArrayList<Node> nodeRemoveList;
	
	/** Flag indicating that the geometry should be changed */
	private boolean config;
	
	/** Flag indicating the display state */
	private boolean enabled;
	
	/** Scratch */
	private float[] min;
	private float[] max;
	private float[] offset;
	private Matrix4f mtx;
	
	/** Flag indicating the measurement system */
	private boolean isMetric;
	
	/** Distance between hash marks */
	private float hash_separation;
	
	/** Number of hashmarks between primary marks */
	private int hash_increment;
	
	/**
	 * Constructor
	 *
	 * @param mgmtObserver The SceneManagerObserver
	 * @param group The grouping node to use for the labels
	 */
	SegmentLegendVertical(SceneManagerObserver mgmtObserver, Group group) {
		
		this.mgmtObserver = mgmtObserver;
		mgmtObserver.addObserver(this);
		
		this.group = group;
		
		nodeAddList = new ArrayList<Node>();
		nodeRemoveList = new ArrayList<Node>();
		
		min = new float[3];
		max = new float[3];
		offset = new float[3];
		mtx = new Matrix4f();
		
		String appName = (String)ApplicationParams.get(
                ApplicationParams.APP_NAME);
        Preferences prefs = Preferences.userRoot().node(appName);
        String unitLabel = prefs.get(
        		PersistentPreferenceConstants.UNIT_OF_MEASUREMENT, 
        		PersistentPreferenceConstants.DEFAULT_UNIT_OF_MEASUREMENT);
        Unit unit = UnitConversionUtilities.getUnitByCode(unitLabel);
		String system_string = UnitConversionUtilities.getMeasurementSystem(unit);
        isMetric = system_string.equals(UnitConversionUtilities.METRIC);
		
		if (isMetric) {
			hash_increment = INC_METRIC;
			hash_separation = 1.0f / hash_increment;
		} else {
			hash_increment = INC_IMPERIAL;
			hash_separation = 
				UnitConversionUtilities.convertUnitsToMeters(
				(1.0f / hash_increment), 
				Unit.FEET);
		}
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameObserver
	//---------------------------------------------------------------
	
	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrame() {
		
		if ((config) && (group != null)) {
			mgmtObserver.requestBoundsUpdate(group, this);
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
			
			int numToAdd = nodeAddList.size();
			if (numToAdd > 0) {
				for (int i = 0; i < numToAdd; i++) {
					Node node = nodeAddList.get(i);
					group.addChild(node);
				}
			}
			int numToRemove = nodeRemoveList.size();
			if (numToRemove > 0) {
				for (int i = 0; i < numToRemove; i++) {
					Node node = nodeRemoveList.get(i);
					group.removeChild(node);
				}
				nodeRemoveList.clear();
			}
			
			nodeRemoveList.addAll(nodeAddList);
			nodeAddList.clear();
			
			config = false;
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
	}
	
	/**
	 * Set the active zone entity
	 *
	 * @param ze The active zone entity
	 */
	public void setActiveZoneEntity(ZoneEntity ze) {
		
		if ((ze != null) && (ze.getType() == Entity.TYPE_SEGMENT)) {
			this.segment = (SegmentEntity)ze;
		} else {
			this.segment = null;
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
				// generate the labels on a switch to enabled
				
				if (segment != null) {
					
					// the vertex entities
					VertexEntity left_ve = segment.getStartVertexEntity();
					VertexEntity rght_ve = segment.getEndVertexEntity();
					
					// the vertex heights
					float left_height = left_ve.getHeight();
					float rght_height = rght_ve.getHeight();
					
					float height = Math.max(left_height, rght_height);
					
					int num_hash = 1 + (int)(height / hash_separation);
					
					int num_line = 1 + num_hash;
					int num_vertex = 2 * num_line;
					int num_coord = num_vertex * 3;
					float[] coord = new float[num_coord];
					int[] strips = new int[num_line];
					
					float x_max = -GAUGE_OFFSET;
					float x_min_primary = x_max - PRIMARY_HASH_MARK_LENGTH;
					float x_min_secondary = x_max - SECONDARY_HASH_MARK_LENGTH;
					
					// generate the coords of the hash marks, bottom to top
					int idx = 0;
					int primary_cnt = 0;
					for (int i = 0; i < num_hash; i++) {
						float hash_height = i * hash_separation;
						if (primary_cnt == 0) {
							labelHashMark(hash_height);
							coord[idx++] = x_min_primary;
							primary_cnt = hash_increment;
						} else {
							coord[idx++] = x_min_secondary;
						}
						coord[idx++] = hash_height;
						coord[idx++] = 0;
						coord[idx++] = x_max;
						coord[idx++] = hash_height;
						coord[idx++] = 0;
						strips[i] = 2;
						primary_cnt--;
					}
					// add the coords of the vertical strip
					coord[idx++] = x_max;
					coord[idx++] = 0;
					coord[idx++] = 0;
					coord[idx++] = x_max;
					coord[idx++] = height;
					coord[idx++] = 0;
					strips[num_hash] = 2;
					
					LineStripArray gauge_line = new LineStripArray();
					gauge_line.setVertices(LineStripArray.COORDINATE_3, coord);
					gauge_line.setStripCount(strips, num_line);
					gauge_line.setSingleColor(false, new float[]{0, 0, 0});
					
					Shape3D gauge_line_shape = new Shape3D();
					gauge_line_shape.setGeometry(gauge_line);
					
					nodeAddList.add(gauge_line_shape);
					config = true;
				}
			} else {
				// on a switch to disabled, queue a request to remove 
				// the labels - if they exist
				if (nodeRemoveList.size() > 0) {
					config = true;
				}
			}
		}
	}
	
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/**
	 * Create labels for the hashmarks
	 *
	 * @param height The height of the hashmark
	 */
	private void labelHashMark(float height) {
		
		String label;
		if (isMetric) {
			label = UnitConversionUtilities.getFormatedNumberDisplay(Unit.METERS, height);
		} else {
			float height_inches = UnitConversionUtilities.convertMetersTo(height, Unit.INCHES);
			label = UnitConversionUtilities.getFormatedNumberDisplay(Unit.INCHES, height_inches);
		}
		
		// the scene position to place the label anchor,
		// directly to the left of the hashmark
		offset[0] = -(PRIMARY_HASH_MARK_LENGTH + GAUGE_OFFSET);
		offset[1] = height;
		offset[2] = 0;
		
		LegendText lt = new LegendText(mgmtObserver);
		lt.update(label, TEXT_HEIGHT, Anchor.CENTER_RIGHT, offset);

		nodeAddList.add(lt.getShape());
	}
}
