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
import java.awt.Font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Helper class that produces and displays text labels for the
 * wall segments during thumbnail capture ops.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
class FloorLegend implements
	LegendHandler, 
	NodeUpdateListener, 
	PerFrameObserver {
	
	/** The scene manager observer */
	private SceneManagerObserver mgmtObserver;
	
	/** The group node to place content into */
	private Group group;
	
	/** The manager of the entities to be handled */
	private AV3DEntityManager entityManager;
	
	/** The active zone Entity */
	private ZoneEntity activeZoneEntity;
	
	/** Array list of label geometry nodes */
	private ArrayList<Node> nodeList;
	
	/** Flag indicating that the label geometry should be changed */
	private boolean config;
	
	/** Flag indicating that the label display state */
	private boolean enabled;
	
	/** Scratch */
	private double[] left_pos;
	private double[] rght_pos;
	private double[] position;
	private Vector3f norm;
	private Matrix3f rmtx;
	private Matrix4f mtx;
	private Vector3f offset;
	private float[] min;
	private float[] max;
	private float[] xlate;
	
	/** The font to use for generating labels */
	private Font font;
	
	/**
	 * Constructor
	 *
	 * @param mgmtObserver The SceneManagerObserver
	 * @param group The grouping node to use for the labels
	 */
	FloorLegend(SceneManagerObserver mgmtObserver, Group group) {
		
		this.mgmtObserver = mgmtObserver;
		mgmtObserver.addObserver(this);
		
		this.group = group;
		
		nodeList = new ArrayList<Node>();
		font = new Font("Arial", Font.PLAIN, 40);
		
		left_pos = new double[3];
		rght_pos = new double[3];
		position = new double[3];
		norm = new Vector3f();
		rmtx = new Matrix3f();
		mtx = new Matrix4f();
		offset = new Vector3f();
		min = new float[3];
		max = new float[3];
		xlate = new float[3];
		
		AxisAngle4f rot = new AxisAngle4f(0, 0, 1, (float)-Math.PI/2);
		rmtx.set(rot);
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
			if (enabled) {
				int numToAdd = nodeList.size();
				if (numToAdd > 0) {
					for (int i = 0; i < numToAdd; i++) {
						Node node = nodeList.get(i);
						group.addChild(node);
					}
				}
			} else {
				int numToRemove = nodeList.size();
				if (numToRemove > 0) {
					for (int i = 0; i < numToRemove; i++) {
						Node node = nodeList.get(i);
						group.removeChild(node);
					}
					nodeList.clear();
				}
			}
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
		this.entityManager = entityManager;
	}
	
	/**
	 * Set the active zone entity
	 *
	 * @param ze The active zone entity
	 */
	public void setActiveZoneEntity(ZoneEntity ze) {
		
		if ((ze != null) && (ze.getType() == Entity.TYPE_GROUNDPLANE_ZONE)) {
			activeZoneEntity = ze;
		} else {
			activeZoneEntity = null;
		}
	}
	
	/**
	 * Enable the display of the components
	 *
	 * @param state The display of the components
	 */
	public void setEnabled(boolean state) {
		
		if ((entityManager != null) && (activeZoneEntity != null)) {
			if (enabled != state) {
				
				// the display state has changed
				enabled = state;
				
				if (enabled) {
					
					HashMap<Integer, SegmentEntityWrapper> segmentWrapperMap = 
						entityManager.getSegmentEntityWrapperMap();
					int num_segment = segmentWrapperMap.size();
					if (num_segment > 0) {
						SegmentEntityWrapper[] sew = 
							new SegmentEntityWrapper[num_segment];
						segmentWrapperMap.values().toArray(sew);
						
						// find the extents of the walls in the floor plane
						SegmentableEntity mse = entityManager.getSegmentableEntity();
						ArrayList<VertexEntity> vertices = mse.getVertices();
						int num_vertex = vertices.size();
						
						min[0] = Float.MAX_VALUE;
						max[0] = -Float.MAX_VALUE;
						min[1] = Float.MAX_VALUE;
						max[1] = -Float.MAX_VALUE;
						for (int i = 0; i < num_vertex; i++) {
							VertexEntity vtx = vertices.get(i);
							if (vtx != null) {
								vtx.getPosition(position);
								if (position[0] < min[0]) {
									min[0] = (float)position[0];
								}
								if (position[0] > max[0]) {
									max[0] = (float)position[0];
								}
								if (position[1] < min[1]) {
									min[1] = (float)position[1];
								}
								if (position[1] > max[1]) {
									max[1] = (float)position[1];
								}
							}
						}
						float x_width = max[0] - min[0];
						float y_width = max[1] - min[1];
						float max_width = Math.max(x_width, y_width);
						float scale = max_width / 
							(AV3DConstants.THUMBNAIL_HEIGHT * AV3DConstants.FILL_PERCENT);
						for (int i = 0; i < num_segment; i++) {
							
							String desc = sew[i].segment.getDescription();
							int desc_len = desc.length();
							
							// position adjacent to the center of the wall
							VertexEntity left_ve = sew[i].segment.getStartVertexEntity();
							VertexEntity rght_ve = sew[i].segment.getEndVertexEntity();
							
							left_ve.getPosition(left_pos);
							rght_ve.getPosition(rght_pos);
							
							norm.x = (float)(rght_pos[0] - left_pos[0]);
							norm.y = (float)(rght_pos[1] - left_pos[1]);
							norm.z = 0;
							norm.normalize();
							rmtx.transform(norm);
							norm.scale(0.1f * desc_len);
							
							norm.x += (float)(left_pos[0] + rght_pos[0]) * 0.5f;
							norm.y += (float)(left_pos[1] + rght_pos[1]) * 0.5f;
							norm.z = 0;
							///////////////////////////////////////////////////
							
							xlate[0] = norm.x;
							xlate[1] = norm.y;
							xlate[2] = norm.z;
							
							LegendText lt = new LegendText(mgmtObserver);
							lt.setFont(font);
							lt.update(desc, 0.2f, Anchor.CENTER, xlate);
							
							nodeList.add(lt.getShape());
							config = true;
						}
					}
				}
			}
			// on a switch to disabled, queue a request to remove 
			// the labels - if they exist
			if (nodeList.size() > 0) {
				config = true;
			}
		}
	}
}
