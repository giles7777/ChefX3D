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

package org.chefx3d.view.boundingbox;

// External imports
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

// Local imports

/**
 * An oriented bounding box representation of an extrusion
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public class ExtrusionBoundingBox extends OrientedBoundingBox {

	/** Scratch vecmath objects */
	private Vector3f vec;
	private AxisAngle4f rotation;
	private Vector3f rotation_axis;
	private Vector3f translation;
	private Matrix4f mtx;
	private Vector3f norm;
	private Vector3f v0;
	private Vector3f v1;
	private Point3f min;
	private Point3f max;
	
	/** Scratch arrays */
	private float[] min_ext;
	private float[] max_ext;
	private float[] p0;
	private float[] p1;
	private float[] p2;
	
	/** Scratch bounding object for sections */
	private OrientedBoundingBox obb;
	
	/** The list of section extents */
	private ArrayList<Point3f[]> extentsList;
	
	/**
	 * Constructor
	 */
	public ExtrusionBoundingBox() {
		
		super();
		
		vec = new Vector3f();
		rotation = new AxisAngle4f();
		rotation_axis = new Vector3f();
		translation = new Vector3f();
		mtx = new Matrix4f();
		norm = new Vector3f();
		v0 = new Vector3f();
		v1 = new Vector3f();
		
		min = new Point3f();
		max = new Point3f();
		
		p0 = new float[3];
		p1 = new float[3];
		p2 = new float[3];
		min_ext = new float[3];
		max_ext = new float[3];
		
		obb = new OrientedBoundingBox();
		extentsList = new ArrayList<Point3f[]>();
	}
	
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
	/**
	 * Configure the proxy bounds object and it's associated matrix to
	 * reflect the dimensions and position respectively of the segment.
	 *
	 * @param cs_extent The extents of the cross section. This can be the cross
	 * section parameters, or it could be the min and max extents of the cross
	 * section. No preference on ordering of data.
	 * @param cs_translation The translation offset of the cross section from 
	 * the spine. For example [x,0,0] where the x value doesn't matter and only
	 * the y and z values are used. 
	 * @param spine The spine of the extrusion (the mitre line).
	 * @param visible Indicators of the visible sections (boolean array length
	 * spine / 3 - 1) to flag the visibility of each section. If null or not
	 * enough parameters it will default all to visible. True for visible, false
	 * for invisible.
	 * @param miterEnable Indicators of the junctions mitered. This is optional
	 * (number of sections - 1) and specifies if the joint between sections
	 * should be a square butt cut or not. If null or not enough parameters it 
	 * will default all to true which results in a correct mitre angle between
	 * sections. If the value is false no mitre angle between sections will be
	 * represented.
	 */
	public void update(
		float[] cs_extent,
		float[] cs_translation,
		float[] spine,
		boolean[] visible,
		boolean[] miterEnable) {
		
		int num_vertex = spine.length / 3;
		int num_sections = num_vertex - 1;
		int num_visible = 0;
		
		if (num_sections > 0) {
				
			// adjust the cross section extents by the translation offset
			int num_cs_coord = cs_extent.length;
			int num_cs_vertex = num_cs_coord / 3;
			float[] cross_section = new float[num_cs_coord];
			int idx = 0;
			for (int i = 0; i < num_cs_vertex; i++) {
				idx = i * 3;
				cross_section[idx] = 0; // enforce the y/z plane convention
				cross_section[idx + 1] = cs_extent[idx + 1] + cs_translation[1];
				cross_section[idx + 2] = cs_extent[idx + 2] + cs_translation[2];
			}
		
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
					
					num_visible++;
					
					createSectionBounds(cross_section, p0, angle[i], p1, angle[i + 1], obb);
					
					// side pocket the local bounding extents to be
					// aggregated later into a single bounding object
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
		updateBounds(num_visible);
	}
	
	/** Update the bounds */
	private void updateBounds(int num) {
		
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
		this.setVertices(min, max);
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
	
	/**
	 * Calculate the geometry for the section
	 *
	 * @param cross_section_coord The adjusted extents of the cross section
	 * @param p0 The left vertex of the section
	 * @param a0 The left miter angle
	 * @param p1 The right vertex of the section
	 * @param a1 The right miter angle
	 * @param bounds The bounds object to initialize
	 */
	 void createSectionBounds(
		float[] cross_section_coord,
		float[] p0, double a0,
		float[] p1, double a1,
		OrientedBoundingBox bounds) {
		
		vec.set(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
		float spine_length = vec.length();
		
		/////////////////////////////////////////////////////
		// setup the transformation that will place the geometry
		rotation_axis.set(0, 1, 0);
		float angle = -(float)Math.atan2(vec.z, vec.x);
		
		rotation.set(rotation_axis, angle);
		translation.set(p0[0], p0[1], p0[2]);
		
		mtx.setIdentity();
		
		mtx.setRotation(rotation);
		mtx.setTranslation(translation);
		
		/////////////////////////////////////////////////////
		// create the end caps
		int num_section_coord = cross_section_coord.length;
		int num_section_vertex = num_section_coord / 3;
		
		float[] left_poly_coord = new float[num_section_coord];
		System.arraycopy(cross_section_coord, 0, left_poly_coord, 0, num_section_coord);
		
		float[] rght_poly_coord = new float[num_section_coord];
		System.arraycopy(cross_section_coord, 0, rght_poly_coord, 0, num_section_coord);
		
		/////////////////////////////////////////////////////
		// adjust the ends for the miter angle
		float x_min = 0;
		if (a0 != Math.PI/2) {
			x_min = processLeftEnd(left_poly_coord, a0);
		}
		boolean doMiter = (a1 != Math.PI/2);
		float x_max = processRightEnd(rght_poly_coord, spine_length, doMiter, a1);
		
		/////////////////////////////////////////////////////
		// bounding parameters
		
		getExtents(left_poly_coord, min_ext, max_ext);
		min_ext[0] = x_min;
		max_ext[0] = x_max;
		
		bounds.setVertices(min_ext, max_ext);
		bounds.transform(mtx);
	}
	
	/** 
	 * Process the coordinates for the left end, miter as necessary
	 * 
	 * @param poly_coord The coord array to process
	 * @param a0 The left miter angle
	 * @return The minimum x value in the left end
	 */
	private float processLeftEnd(float[] poly_coord, double a0) {
		
		int num_coord = poly_coord.length;
		int num_vertex = num_coord / 3;
		
		float x_min = 0;
		
		float tangent = (float)Math.tan(a0);
		
		if (tangent != 0) {
			// if the angle is 0, the ends are square,
			// don't bother with the miter calc
			
			int idx = 0;
			for (int i = 0; i < num_vertex; i++) {
				idx = i * 3;
				float value = poly_coord[idx + 2] * tangent;
				poly_coord[idx] = value;
				if (value < x_min) {
					x_min = value;
				}
			}
		}
		return(x_min);
	}
	
	/** 
	 * Process the coordinates for the right end, miter as necessary
	 * 
	 * @param poly_coord The coord array to process
	 * @param distance The separation between the left and right ends
	 * @param miter Flag indicating whether to miter the edge or not
	 * @param a1 The right miter angle
	 * @return The maximum x value in the right end
	 */
	private float processRightEnd(float[] poly_coord, float distance, boolean miter, double a1) {
		
		int num_coord = poly_coord.length;
		int num_vertex = num_coord / 3;
		
		float x_max = distance;
		
		float tangent = (float)Math.tan(a1);
		if (tangent == 0) {
			// if the angle is 0, the ends are square,
			// don't bother with the miter calc
			miter = false;
		}
		
		int idx = 0;
		for (int i = 0; i < num_vertex; i++) {
			idx = i * 3;
			if (miter) {
				float value = distance - (poly_coord[idx + 2] * tangent);
				poly_coord[idx] = value;
				if (value > x_max) {
					x_max = value;
				}
			} else {
				poly_coord[idx] = distance;
			}
		}
		return(x_max);
	}
	
	/**
	 * Get the extents of the coordinate array.
	 * 
	 * @param poly_coord The coord array to process
	 * @param min_ext Array to initialize with the min values
	 * @param max_ext Array to initialize with the max values
	 */
	private void getExtents(float[] poly_coord, float[] min_ext, float[] max_ext){
		
		float x_min = poly_coord[0];
		float x_max = poly_coord[0];
		float y_min = poly_coord[1];
		float y_max = poly_coord[1];
		float z_min = poly_coord[2];
		float z_max = poly_coord[2];
		
		int num_vertex = poly_coord.length / 3;
		int idx = 0;
		float value;
		for (int i = 1; i < num_vertex; i++) {
			
			idx = i * 3;
			////////////////////////////////////////
			// the x value is computed while mitering,
			// therefore this check is redundant
			/*
			value = poly_coord[idx];
			if (value < x_min) {
				x_min = value;
			}
			if (value > x_max) {
				x_max = value;
			}
			*/
			////////////////////////////////////////
			idx++;
			
			value = poly_coord[idx];
			if (value < y_min) {
				y_min = value;
			}
			if (value > y_max) {
				y_max = value;
			}
			idx++;
			
			value = poly_coord[idx];
			if (value < z_min) {
				z_min = value;
			}
			if (value > z_max) {
				z_max = value;
			}
			idx++;
		}
		min_ext[0] = x_min;
		max_ext[0] = x_max;
		min_ext[1] = y_min;
		max_ext[1] = y_max;
		min_ext[2] = z_min;
		max_ext[2] = z_max;
	}
}
