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
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.*;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

// Local imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.ExtrusionEntity;
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.ui.LoadingProgressListener;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * A wrapper for extusion type entity
 *
 * @author Rex Melton
 * @version $Revision: 1.7 $
 */
class ExtrusionEntityWrapper extends AV3DEntityWrapper implements
	LoadListener, PerFrameObserver, EntityPropertyListener {
	
	/** The cross section to use when extruding sections */
	private CrossSectionGeom cs_geom;
	
	/** The extruded section creator */
	private ExtrusionGeom ex_geom;
	
	/** The translation offset to be used to align the cross section
	* with the extrusion spine */
	private float[] cs_translation;
	
	/** The extrusion spine. An array of vertices (in a plane). Each pair 
	*  defines a section. */
	private float[] spine;
	
	/** The flags that determine visibility per section */
	private boolean[] visible;
	
	/** The flags that control mitering per section */
	private boolean[] miterEnable;
	
	/** Flag indicating that the sections should be recomputed */
	private boolean doUpdate;
	
	/** The Appearance to apply to the extrusion geometry */
	private Appearance appearance;
	
	/** scratch calculation vars */
	private Matrix4f mtx;
	private Vector3f norm;
	private Vector3f v0;
	private Vector3f v1;
	private float[] p0;
	private float[] p1;
	private float[] p2;
	private Point3f min;
	private Point3f max;
	
	/** The list of sections to add */
	private ArrayList<Node> sectionList;
	
	/** The list of section extents */
	private ArrayList<Point3f[]> extentsList;
	
	/** Bounding geometry */
	private TransformGroup boundsProxy;
	
	/** Flag indicating that the bounds geometry should be reconfigured */
	private boolean boundsProxyChanged;
	
	/**
	 * Constructor
	 *
	 * @param mgmtObserver Reference to the SceneManagerObserver
	 * @param entity The entity that the wrapper object is based around
	 * @param urlFilter The filter to use for url requests
	 * @param reporter The instance to use or null
	 */
	ExtrusionEntityWrapper(
		SceneManagerObserver mgmtObserver,
		PositionableEntity entity,
		URLFilter urlFilter,
		LoadingProgressListener progressListener,
		ErrorReporter reporter){
		
		super(mgmtObserver, entity, urlFilter, progressListener, reporter);
		
		mtx = new Matrix4f();
		norm = new Vector3f();
		v0 = new Vector3f();
		v1 = new Vector3f();
		
		p0 = new float[3];
		p1 = new float[3];
		p2 = new float[3];
		
		min = new Point3f();
		max = new Point3f();
		
		ex_geom = new ExtrusionGeom();
		sectionList = new ArrayList<Node>();
		extentsList = new ArrayList<Point3f[]>();
		
		String propSheetName = entity.getParamSheetName();
		Object csTranslationProp = entity.getProperty(
			propSheetName,
			ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);
		
		if ((csTranslationProp != null) && (csTranslationProp instanceof float[])) {
			cs_translation = (float[])csTranslationProp;
		}
		
		Object spineProp = entity.getProperty(
			propSheetName,
			ExtrusionEntity.SPINE_VERTICES_PROP);
		
		if ((spineProp != null) && (spineProp instanceof float[])) {
			spine = (float[])spineProp;
		}
		
		Object visibleProp = entity.getProperty(
			propSheetName,
			ExtrusionEntity.VISIBLE_PROP);
		if ((visibleProp != null) && (visibleProp instanceof boolean[])) {
			visible = (boolean[])visibleProp;
		}
		
		Object miterEnableProp = entity.getProperty(
			propSheetName,
			ExtrusionEntity.MITER_ENABLE_PROP);
		
		if ((miterEnableProp != null) && (miterEnableProp instanceof boolean[])) {
			miterEnable = (boolean[])miterEnableProp;
		}
		
		mgmtObserver.addObserver(this);
		
		String url_string = entity.getModelURL();
		if (url_string != null) {
			AV3DLoader loader = new AV3DLoader(progressListener, urlFilter);
			loader.loadThreaded(url_string, true, this);
		}
	}
	
	//----------------------------------------------------------
	// Methods defined by LoadListener
	//----------------------------------------------------------
	
	/**
	 * Notification that the model has finished loading.
	 *
	 * @param nodes The nodes loaded
	 */
	public void modelLoaded(Node[] nodes) {
		if (nodes != null) {
			float[] cs_coord = getCrossSectionCoords(nodes);
			if (cs_coord != null) {
				cs_geom = new CrossSectionGeom(cs_coord, cs_translation);
				ex_geom.setCrossSectionGeom(cs_geom);
            	setSwitchGroupIndex(CONTENT_MODEL);
				doUpdate = true;
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
		
		if (doUpdate && (cs_geom != null)) {
			
			updateSections();
			updateBounds();
			
			boundsProxyChanged = true;
			
			mgmtObserver.requestBoundsUpdate(contentGroup, this);
			mgmtObserver.requestBoundsUpdate(switchGroup, this);
			
			doUpdate = false;
		}
	}
	
	//----------------------------------------------------------
	// Methods defined by NodeUpdateListener
	//----------------------------------------------------------
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The node or Node Component that is to be updated.
	 */
	public void updateNodeDataChanges(Object src) {
		
		super.updateNodeDataChanges(src);
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the nodes bounds.
	 *
	 * @param src The node or Node Component that is to be updated.
	 */
	public void updateNodeBoundsChanges(Object src) {
		
		if (src == contentGroup) {
			
			contentGroup.removeAllChildren();
			for (int i = 0; i < sectionList.size(); i++) {
				contentGroup.addChild(sectionList.get(i));
			}
			sectionList.clear();
			contentGroup.requestBoundsUpdate();
			
		} else if (src == switchGroup) {
			
			if (boundsProxyChanged) {
				switchGroup.setChild(boundsProxy, CONTENT_BOUNDS);
				boundsProxyChanged = false;
			}
			switchGroup.setActiveChild(index);
			
		} else {
			super.updateNodeBoundsChanges(src);
		}
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
		
		if (propertySheet.equals(PositionableEntity.DEFAULT_ENTITY_PROPERTIES)) {
			if (propertyName.equals(ExtrusionEntity.SPINE_VERTICES_PROP)) {
				Object spineProp = entity.getProperty(
					propertySheet,
					ExtrusionEntity.SPINE_VERTICES_PROP);
				
				if ((spineProp != null) && (spineProp instanceof float[])) {
					spine = (float[])spineProp;
				}
				doUpdate = true;
				
			} else if (propertyName.equals(ExtrusionEntity.SPINE_VERTICES_PROP)) {
				Object csTranslationProp = entity.getProperty(
					propertySheet,
					ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);
				
				if ((csTranslationProp != null) && (csTranslationProp instanceof float[])) {
					cs_translation = (float[])csTranslationProp;
				}
				doUpdate = true;
				
			} else if (propertyName.equals(ExtrusionEntity.VISIBLE_PROP)) {
				Object visibleProp = entity.getProperty(
					propertySheet,
					ExtrusionEntity.VISIBLE_PROP);
				
				if ((visibleProp != null) && (visibleProp instanceof boolean[])) {
					visible = (boolean[])visibleProp;
				}
				doUpdate = true;
				
			} else if (propertyName.equals(ExtrusionEntity.MITER_ENABLE_PROP)) {
				Object miterEnableProp = entity.getProperty(
					propertySheet,
					ExtrusionEntity.MITER_ENABLE_PROP);
				
				if ((miterEnableProp != null) && (miterEnableProp instanceof boolean[])) {
					miterEnable = (boolean[])miterEnableProp;
				}
				doUpdate = true;
				
			} else {
				super.propertyUpdated(
					entityID,
					propertySheet,
					propertyName,
					ongoing);
			}
		} else {
			super.propertyUpdated(
				entityID,
				propertySheet,
				propertyName,
				ongoing);
		}
	}
	
	//----------------------------------------------------------
	// Method overrides of AV3DEntityWrapper
	//----------------------------------------------------------
	
	/**
	 * Eliminate any references that this object may have to make it
	 * eligible for GC.
	 */
	protected void dispose() {
		super.dispose();
		mgmtObserver.removeObserver(this);
	}
	
	/**
	 * Load the model from the specified argument into the
	 * Entity scenegraph structure.
	 */
	protected void loadModel() {
		
		bounds = new OrientedBoundingBox();
		
		contentGroup = new Group();
		
		switchGroup = new SwitchGroup();
		switchGroup.addChild(contentGroup);
		switchGroup.addChild(null);
		index = CONTENT_NONE;
		switchGroup.setActiveChild(CONTENT_NONE);
		//index = CONTENT_MODEL;
		//switchGroup.setActiveChild(CONTENT_MODEL);
		
		transformModel = new TransformGroup();
		transformModel.addChild(switchGroup);
		
		configModelMatrix();
		transformModel.setTransform(modelMatrix);
		
		transformGroup = new TransformGroup();
		transformGroup.addChild(transformModel);
		
		configGroupMatrix();
		transformGroup.setTransform(groupMatrix);
		
		sharedNode = new SharedNode();
		sharedNode.setChild(transformGroup);
	}
	
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/** Generate the extrusion geometry */
	private void updateSections() {
		
		int num_vertex = spine.length / 3;
		int num_sections = num_vertex - 1;
		
		if (num_sections > 0) {
			
			if ((visible == null) || (visible.length < num_sections)) {
				// ensure that the visible array is sufficiently sized,
				// default to true
				visible = new boolean[num_sections];
				for (int i = 0; i < num_sections; i++) {
					visible[i] = true;
				}
			}
			
			if (miterEnable != null) {
				int num_junctions = num_sections - 1;
				if (miterEnable.length < num_junctions) {
					// ensure that the miterEnable array is sufficiently sized,
					// default to true
					miterEnable = new boolean[num_junctions];
					for (int i = 0; i < num_junctions; i++) {
						miterEnable[i] = true;
					}
				}
			}
			
			int idx;
			int num_visible = 0;
			// note, the miter angle is 0 (i.e. a square end) at the 
			// beginning of the first section and the end of the last section
			float[] angle = new float[num_vertex];
			for (int i = 0; i < num_sections; i++) {
				
				idx = i * 3;
				p0[0] = spine[idx];
				p0[1] = spine[idx + 1];
				p0[2] = spine[idx + 2];
				
				idx += 3;
				p1[0] = spine[idx];
				p1[1] = spine[idx + 1];
				p1[2] = spine[idx + 2];
				
				if ((num_sections > 1) && (i < num_sections - 1)) {
					
					// calculate the miter angle at each junction
					if ((miterEnable != null) && miterEnable[i]) {
						
						// get the vector along adjoining sections
						v0.set(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
						
						idx += 3;
						p2[0] = spine[idx];
						p2[1] = spine[idx + 1];
						p2[2] = spine[idx + 2];
						
						v1.set(p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]);
						
						// get the angle between the vectors
						float a = getAngle(v0, v1);
						
						// split in half, between the sections
						angle[i + 1] = a * 0.5f;
					}
				}
				if (visible[i]) {
					
					if (ex_geom.createGeom(p0, angle[i], p1, angle[i + 1])) {
						
						num_visible++;
						
						TriangleArray triangleArray = new TriangleArray();
						float[] coord = ex_geom.getCoords();
						triangleArray.setVertices(
							TriangleArray.COORDINATE_3, coord);
						
						float[] normal = ex_geom.getNormals();
						triangleArray.setNormals(normal);
						
						int[] texType = {VertexGeometry.TEXTURE_COORDINATE_2};
						float[][] texCoord = new float[1][];
						texCoord[0] = ex_geom.getTexCoords();
						triangleArray.setTextureCoordinates(texType, texCoord, 1);
						
						float[] tangent = ex_geom.getTangents();
						triangleArray.setAttributes(5, 4, tangent, false);
						
						Shape3D shape = new Shape3D();
						shape.setAppearance(appearance);
						shape.setGeometry(triangleArray);
						
						TransformGroup tg = new TransformGroup();
						ex_geom.getMatrix(mtx);
						tg.setTransform(mtx);
						tg.addChild(shape);
						
						sectionList.add(tg);
						
						// side pocket the local bounding extents to be
						// aggregated later into a single bounding object
						OrientedBoundingBox obb = ex_geom.getBounds();
						obb.transform(mtx);
						Point3f[] ext = null;
						if (extentsList.size() < num_visible) {
							ext = new Point3f[2];
							ext[0] = new Point3f();
							ext[1] = new Point3f();
							extentsList.add(ext);
						} else {
							ext = extentsList.get(num_visible - 1);
						}
						obb.getExtents(ext[0], ext[1]);
					}
				}
			}
		}
	}
	
	/** Update the bounds geometry */
	private void updateBounds() {
		
		int num = sectionList.size();
		if (num > 0) {
			// aggregate the extents of the visible geometry
			// to produce a single bounds object
			Point3f[] ext = extentsList.get(0);
			min.x = ext[0].x;
			min.y = ext[0].y;
			min.z = ext[0].z;
			max.x = ext[1].x;
			max.y = ext[1].y;
			max.z = ext[1].z;
			for (int i = 1; i < num; i++) {
				ext = extentsList.get(i);
				if (ext[0].x < min.x) {
					min.x = ext[0].x;
				}
				if (ext[0].y < min.y) {
					min.y = ext[0].y;
				}
				if (ext[0].z < min.z) {
					min.z = ext[0].z;
				}
				if (ext[1].x > max.x) {
					max.x = ext[1].x;
				}
				if (ext[1].y > max.y) {
					max.y = ext[1].y;
				}
				if (ext[1].z > max.z) {
					max.z = ext[1].z;
				}
			}
		} else {
			min.x = 0;
			min.y = 0;
			min.z = 0;
			max.x = 0;
			max.y = 0;
			max.z = 0;
		}
		
		bounds.setVertices(min, max);
		
		dim[0] = max.x - min.x;
		dim[1] = max.y - min.y;
		dim[2] = max.z - min.z;
		
		center[0] = (max.x + min.x) * 0.5f;
		center[1] = (max.y + min.y) * 0.5f;
		center[2] = (max.z + min.z) * 0.5f;
		
		if (num > 0) {
			Shape3D proxyShape = generateBoundsGeometry(dim, DEFAULT_BOUNDS_COLOR);
			boundsProxy = new TransformGroup();
			mtx.setIdentity();
			v0.set(center);
			mtx.setTranslation(v0);
			boundsProxy.setTransform(mtx);
			boundsProxy.addChild(proxyShape);
			boundsProxy.setUserData("Bound: entity = "+ entity.getEntityID());
		} else {
			boundsProxy = null;
		}
		
		if (statusListeners != null) {
			for (int i = 0; i < statusListeners.size(); i++) {
				WrapperListener wl = statusListeners.get(i);
				wl.geometryChanged(this);
			}
		}
	}
	
	/**
	 * Find and return the cross section coords from within
	 * the argument node array.
	 *
	 * @param node An array of Nodes
	 * @return The cross section coordinates, or null if
	 * none could be found.
	 */
	private float[] getCrossSectionCoords(Node[] node) {
		
		float[] cs_coord = null;
		LineStripArray line = getLineStripArray(node);
		if (line != null) {
			int type = line.getVertexType();
			if (type == LineStripArray.COORDINATE_3) {
				int num_strip = line.getValidStripCount();
				if (num_strip > 0) {
					int[] strips = new int[num_strip];
					line.getStripCount(strips);
					int num_vertex = line.getValidVertexCount();
					int num_coord = num_vertex * 3;
					float[] coord = new float[num_coord];
					line.getVertices(coord);
					
					// take only the first strip
					int num_vertex_s0 = strips[0];
					int num_coord_s0 = num_vertex_s0 * 3;
					cs_coord = new float[num_coord_s0];
					System.arraycopy(coord, 0, cs_coord, 0, num_coord_s0);
				}
			} else {
				// throw a fit !
			}
		}
		return(cs_coord);
	}
	
	/**
	 * Walk the node heirarchy, return the first instance of a 
	 * LineStripArray found.
	 *
	 * @param node An array of Nodes
	 * @return The first instance of a LineStripArray found, or null
	 * if none are found.
	 */
	private LineStripArray getLineStripArray(Node[] node) {
		LineStripArray line = null;
		for (int i = 0; i < node.length; i++) {
			Node n = node[i];
			if (n != null) {
				if (n instanceof Shape3D) {
					Shape3D s = (Shape3D)n;
					Geometry g = s.getGeometry();
					if (g instanceof LineStripArray) {
						line = (LineStripArray)g;
						appearance = s.getAppearance();
						break;
					}
				} else if (n instanceof Group) {
					Node[] child_nodes = ((Group)n).getAllChildren();
					line = getLineStripArray(child_nodes);
					if (line != null) {
						break;
					}
				}
			}
		}
		return(line);
	}
	
	/**
	 * Calculate the angle between the vectors
	 *
	 * @param v0 A vector
	 * @param v1 A vector
	 * @return The angle between
	 */
	private float getAngle(Vector3f v0, Vector3f v1) {
		
		float angle = 0;
		norm.cross(v0, v1);
		if (norm.y != 0) {
			int sign = (norm.y < 0) ? 1 : -1;
			angle = v0.angle(v1);
			if (angle == Math.PI) {
				angle = 0;
			} else {
				angle *= sign;
			}
		}
		return(angle);
	}
}
