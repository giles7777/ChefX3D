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
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.geom.triangulation.GeometryInfo;

import org.j3d.util.TriangleUtils;

// Local imports
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Utility class to calculate the geometry for an extrusion section.
 * By convention, the sections are created to be in a local coordinate
 * system in a plane parallel to the x/y plane.
 *
 * @author Rex Melton
 * @version $Revision: 1.5 $
 */
class ExtrusionGeom {
	
	/** The axis along which the geometry is created */
	private static final Vector3f X_AXIS = new Vector3f(1, 0, 0);
	
	/** The transform to be applied to the geometry */
	private Matrix4f mtx;
	
	/** Scratch vecmath objects */
	private Vector3f vec;
	private AxisAngle4f rotation;
	private Vector3f rotation_axis;
	private Vector3f translation;
	
	private Vector3f v0;
	private Vector3f v1;
	private Vector3f n;
	
	/** Bounding parameters, used for selection box */
	private float[] local_center;
	private float[] dim;
			
	/** The mesh data */
	private float[] coord;
	private float[] normal;
	private float[] texCoord;
	private float[] tangent;
	
	/** The default bounds object */
	private OrientedBoundingBox bounds;
	
	/** Bounding extents */
	private float[] min;
	private float[] max;
	
	/** The cross section geometry */
	private CrossSectionGeom csg;
	
	/**
	 * Constructor
	 */
	ExtrusionGeom() {
		
		vec = new Vector3f();
		rotation = new AxisAngle4f();
		rotation_axis = new Vector3f();
		translation = new Vector3f();
		mtx = new Matrix4f();
		
		v0 = new Vector3f();
		v1 = new Vector3f();
		n = new Vector3f();
		
		local_center = new float[3];
		dim = new float[3];
		
		bounds = new OrientedBoundingBox();
		min = new float[3];
		max = new float[3];
	}
	
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
	void setCrossSectionGeom(CrossSectionGeom csg) {
		this.csg = csg;
	}
	
    /**
     * Return the bounds.
     *
     * @return The bounds
     */
	OrientedBoundingBox getBounds() {
		return(bounds);
	}
	
	/**
	 * Return the segment transform matrix
	 *
	 * @param The matrix object to initialize
	 */
	void getMatrix(Matrix4f matrix) {
		matrix.set(this.mtx);
	}
	
	/**
	 * Return the vertex coordinates
	 *
	 * @return The vertex coordinates
	 */
	float[] getCoords() {
		return(coord);
	}
	
	/**
	 * Return the normal vectors
	 *
	 * @return The normal vectors
	 */
	float[] getNormals() {
		return(normal);
	}
	
	/**
	 * Return the texture coordinates
	 *
	 * @return The texture coordinates
	 */
	float[] getTexCoords() {
		return(texCoord);
	}
	
	/**
	 * Return the tangent vectors
	 *
	 * @return The tangent vectors
	 */
	float[] getTangents() {
		return(tangent);
	}
	
    /**
     * Return the center of the model bounds
	 *
     * @param val The array to initialize with the center
     */
    void getCenter(float[] val) {
        val[0] = local_center[0];
        val[1] = local_center[1];
        val[2] = local_center[2];
    }

    /**
     * Return the dimensions of the model bounds.
	 *
     * @param val The array to initialize with the dimensions
     */
    void getDimensions(float[] val) {
        val[0] = dim[0];
        val[1] = dim[1];
        val[2] = dim[2];
    }

	/**
	 * Get the current position
	 * 
	 * @param pos The array to initialize with the position
	 */
	void getPosition(double[] pos) {
		//translation.get(pos);
		pos[0] = translation.x;
		pos[1] = translation.y;
		pos[2] = translation.z;
	}

	/**
	 * Get the current rotation
	 * 
	 * @param rot The array to initialize with the rotation
	 */
	void getRotation(float[] rot) {
		rotation.get(rot);
	}

	/**
	 * Calculate the geometry for the section
	 *
	 * @param p0 The left vertex of the section
	 * @param a0 The left miter angle
	 * @param p1 The right vertex of the section
	 * @param a1 The right miter angle
	 * @return true if the geometry has been successfully created, 
	 * false otherwise.
	 */
	boolean createGeom(
		float[] p0, double a0,
		float[] p1, double a1) {
		
		vec.set(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
		
		float spine_length = vec.length();
		if (spine_length == 0) {
			return(false);
		}
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
		float[] cross_section_coord = csg.coord;
		int num_section_coord = cross_section_coord.length;
		int num_section_vertex = num_section_coord / 3;
		
		float[] left_poly_coord = new float[num_section_coord];
		System.arraycopy(cross_section_coord, 0, left_poly_coord, 0, num_section_coord);
			
		float[] rght_poly_coord = new float[num_section_coord];
		System.arraycopy(cross_section_coord, 0, rght_poly_coord, 0, num_section_coord);
		
		int num_contour = 1;
		int[] strip_count = new int[num_contour];
		strip_count[0] = num_section_vertex;
		
		// triangulate the left end cap
		GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
		gi.setCoordinates(left_poly_coord);
		gi.setContourCounts(new int[]{num_contour});
		gi.setStripCounts(strip_count);
		gi.convertToIndexedTriangles();
		
		int[] face_tri_indices = gi.getCoordinateIndices();
		
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
		
		getBounds(left_poly_coord, min, max);
		min[0] = x_min;
		max[0] = x_max;
		
		float width = max[0] - min[0];
		float height = max[1] - min[1];
		float depth = max[2] - min[2];
		
		dim[0] = width;
		dim[1] = height;
		dim[2] = depth;
		
		local_center[0] = width / 2;
		local_center[1] = height / 2;
		local_center[2] = depth / 2;
		
		bounds.setVertices(min, max);
		/////////////////////////////////////////////////////
		// convert the resulting indexed triangles into
		// triangle array form for the left side
		int num_face_vertex = face_tri_indices.length;
		int num_face_coord = num_face_vertex * 3;
		float[] left_coord = new float[num_face_coord];
		int off = 0;
		int poly_off = 0;
		for (int i = 0; i < num_face_vertex; i++) {
			
			off = i * 3;
			poly_off = face_tri_indices[i] * 3;
			
			left_coord[off] = left_poly_coord[poly_off];
			left_coord[off+1] = left_poly_coord[poly_off+1];
			left_coord[off+2] = left_poly_coord[poly_off+2];
		}
		float[] left_texCoord = getEndTexCoords(left_coord, true);
		
		// triangle array output for the right side
		float[] rght_coord = new float[num_face_coord];
		int last_i = num_face_vertex - 1;
		for (int i = 0; i < num_face_vertex; i++) {
			
			off = i * 3;
			// invert the ordering of the right 
			poly_off = face_tri_indices[last_i - i] * 3;
			
			rght_coord[off] = rght_poly_coord[poly_off];
			rght_coord[off+1] = rght_poly_coord[poly_off+1];
			rght_coord[off+2] = rght_poly_coord[poly_off+2];
		}
		float[] rght_texCoord = getEndTexCoords(rght_coord, false);
		
		/////////////////////////////////////////////////////
		// produce the side strips to complete the mesh
		//
		//    ^ 5---4 2  r
		// l  | |  / /|  i
		// e  | | / / |  g
		// f  | |/ /  |  h
		// t  t 3 0---1  t
		//      s----->
		//
		int num_side_tris = num_section_vertex * 2;
		int num_side_coord = num_side_tris * 3 * 3;
		float[] side_coord = new float[num_side_coord];
		
		int num_side_texCoord = num_side_tris * 3 * 2;
		float[] side_texCoord = new float[num_side_texCoord];
		
		off = 0;
		int tc_off = 0;
		
		float x;
		int rght_off;
		int left_off;
		
		int c_idx;
		float[] circumference = csg.perimeter_length;
		
		int num_edge_vertex = num_section_vertex;
		int last_edge_vertex = num_edge_vertex - 1;
		for (int j = 0; j < num_edge_vertex; j++) {
			
			c_idx = j;
			
			rght_off = j * 3;
			left_off = j * 3;
			
			// triangle #1
			x = left_poly_coord[left_off];
			side_coord[off++] = x;
			side_coord[off++] = left_poly_coord[left_off+1];
			side_coord[off++] = left_poly_coord[left_off+2];
			
			side_texCoord[tc_off++] = x;
			side_texCoord[tc_off++] = circumference[c_idx];
			
			x = rght_poly_coord[rght_off];
			side_coord[off++] = x;
			side_coord[off++] = rght_poly_coord[rght_off+1];
			side_coord[off++] = rght_poly_coord[rght_off+2];
			
			side_texCoord[tc_off++] = x;
			side_texCoord[tc_off++] = circumference[c_idx];
			
			c_idx = j + 1;
			
			if (j == last_edge_vertex) {
				rght_off = 0;
			} else {
				rght_off += 3;
			}
			x = rght_poly_coord[rght_off];
			side_coord[off++] = x;
			side_coord[off++] = rght_poly_coord[rght_off+1];
			side_coord[off++] = rght_poly_coord[rght_off+2];
			
			side_texCoord[tc_off++] = x;
			side_texCoord[tc_off++] = circumference[c_idx];
			
			// triangle #2
			c_idx = j;
			
			x = left_poly_coord[left_off];
			side_coord[off++] = x;
			side_coord[off++] = left_poly_coord[left_off+1];
			side_coord[off++] = left_poly_coord[left_off+2];
			
			side_texCoord[tc_off++] = x;
			side_texCoord[tc_off++] = circumference[c_idx];
			
			c_idx = j + 1;
			
			x = rght_poly_coord[rght_off];
			side_coord[off++] = x;
			side_coord[off++] = rght_poly_coord[rght_off+1];
			side_coord[off++] = rght_poly_coord[rght_off+2];
			
			side_texCoord[tc_off++] = x;
			side_texCoord[tc_off++] = circumference[c_idx];
			
			if (j == last_edge_vertex) {
				left_off = 0;
			} else {
				left_off += 3;
			}
			x = left_poly_coord[left_off];
			side_coord[off++] = x;
			side_coord[off++] = left_poly_coord[left_off+1];
			side_coord[off++] = left_poly_coord[left_off+2];
			
			side_texCoord[tc_off++] = x;
			side_texCoord[tc_off++] = circumference[c_idx];
		}
		/////////////////////////////////////////////////////
		// aggregate the vertex coordinates
		int num_coord = left_coord.length + rght_coord.length + side_coord.length;
		coord = new float[num_coord];
		
		off = 0;
		int length = left_coord.length;
		System.arraycopy(left_coord, 0, coord, off, length);
		off += length;
		length = rght_coord.length;
		System.arraycopy(rght_coord, 0, coord, off, length);
		off += length;
		length = side_coord.length;
		System.arraycopy(side_coord, 0, coord, off, length);
		
		// aggregate the texture coordinates
		int num_texCoord = left_texCoord.length + rght_texCoord.length + side_texCoord.length;
		texCoord = new float[num_texCoord];
		
		off = 0;
		length = left_texCoord.length;
		System.arraycopy(left_texCoord, 0, texCoord, off, length);
		off += length;
		length = rght_texCoord.length;
		System.arraycopy(rght_texCoord, 0, texCoord, off, length);
		off += length;
		length = side_texCoord.length;
		System.arraycopy(side_texCoord, 0, texCoord, off, length);
		
		// generate per face normals
		normal = getNormals(coord);
		
		// generate tangents
		int num_vertex = coord.length / 3;
		int num_tri = num_vertex / 3;
		tangent = new float[num_vertex * 4];
		TriangleUtils.createTangents(
			num_tri,
			coord,
			normal,
			texCoord,
			tangent);
		
		//////////////////////////////////////////////////////////////
		// rem: NFI why this is necessary, but 'something' must be
		// done to make this geometry appear in line art rendering
		mangleTangents();
		//////////////////////////////////////////////////////////////
		
		return(true);
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
	 * Return the texture coordinates for an end cap
	 *
	 * @param crd The vertex coordinates
	 * @param left Flag indicating the vertex coordinates are
	 * for the left end (true), or the right end (false)
	 * @return The array of texture coordinates
	 */
	private float[] getEndTexCoords(float[] crd, boolean left) {
		
		int num_vertex = crd.length / 3;
		float[] texCoord = new float[num_vertex * 2];

		float y_min = min[1];
		float y_max = max[1];
		float z_min = min[2];
		float z_max = max[2];
		
		float y_span = y_max - y_min;
		float z_span = z_max - z_min;
		
		// generate the texure coordinates
		float y, z;
		float s, t;
		int idx = 0;
		int tc_idx = 0;
		for (int i = 0; i < num_vertex; i++) {
			idx = i * 3;
			tc_idx = i * 2;
			
			y = crd[idx + 1];
			z = crd[idx + 2];
			
			if (left) {
				s = (z - z_min) / z_span;
			} else {
				s = (z_max - z) / z_span;
			}
			t = (y - y_min) / y_span;
			
			texCoord[tc_idx] = s;
			texCoord[tc_idx + 1] = t;
		}
		return(texCoord);
	}
	
	/**
	 * Generate per-face normals for the array of coordinates
	 *
	 * @param crd The coordinate array
	 */
	private float[] getNormals(float[] crd) {
		
		int num_coord = crd.length;
		int num_vertex = num_coord / 3;
		int num_tris = num_vertex / 3;
		
		float[] nrml = new float[num_coord];
		int off = 0;
		int off1 = 0;
		int off2 = 0;
		for (int i = 0; i < num_tris; i++) {
			off = i * 9;
			off1 = off + 3;
			off2 = off1 + 3;
			v0.set(
				crd[off1] - crd[off],
				crd[off1+1] - crd[off+1],
				crd[off1+2] - crd[off+2]);
			v1.set(
				crd[off2] - crd[off1],
				crd[off2+1] - crd[off1+1],
				crd[off2+2] - crd[off1+2]);

			n.cross(v0, v1);
			n.normalize();
			
			nrml[off] = n.x;
			nrml[off+1] = n.y;
			nrml[off+2] = n.z;
			
			nrml[off1] = n.x;
			nrml[off1+1] = n.y;
			nrml[off1+2] = n.z;
			
			nrml[off2] = n.x;
			nrml[off2+1] = n.y;
			nrml[off2+2] = n.z;
		}
		return(nrml);
	}
	
	/**
	 * Get the bounds of the coordinate array into the argument
	 * bounds array.
	 * 
	 * @param poly_coord The coord array to process
	 * @param min Array to initialize with the min values
	 * @param max Array to initialize with the max values
	 */
	private void getBounds(float[] poly_coord, float[] min, float[] max){
		
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
		min[0] = x_min;
		max[0] = x_max;
		min[1] = y_min;
		max[1] = y_max;
		min[2] = z_min;
		max[2] = z_max;
	}
	
	/**
	 * Do something weird to the tangents so that the geometry 
	 * renders in the line art mode
	 */
	void mangleTangents() {
		int num_vtx = tangent.length / 4;
		for (int i = 0; i < num_vtx; i++) {
			int idx = i * 4;
			vec.x = tangent[idx];
			vec.y = tangent[idx + 1];
			vec.z = tangent[idx + 2];
			vec.normalize();
			vec.negate();
			tangent[idx] = vec.x;
			tangent[idx + 1] = vec.y;
			tangent[idx + 2] = vec.z;
		}
	}
}


