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
import java.util.HashSet;
import java.util.Iterator;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.geom.triangulation.GeometryInfo;

import org.j3d.util.TriangleUtils;

// Local imports
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Utility class to calculate the geometry of a Segment
 *
 * @author Rex Melton
 * @version $Revision: 1.14 $
 */
class SegmentGeom {
	
	/** Flag indicating that bump map tex coords should be generated */
	private static final boolean ENABLE_BUMP_MAP_TEXTURE = true;
	
	/** Internal representation of no windows or doors */
	private static final float[][] EMPTY_SET = 
		new float[0][];
	
	/** Default texture coords for each side panel section */
	private static final float[] SIDE_SECTION_TC = new float[]{ 
		0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1,};
	
	/** The transform to be applied to the geometry */
	private Matrix4f mtx;
	
	/** Scratch vecmath objects */
	private Vector3f vec;
	private AxisAngle4f rotation;
	private Vector3f translation;
	private Point3f pnt;
	
	private Vector3f v0;
	private Vector3f v1;
	private Vector3f n;
	
    private Point3f[] p;
	
	private double[] position;
	private float[] size;
	private float[] scale;
	private Matrix4f inv;
	private Point3f center;
	
	/** Bounding parameters, used for selection box */
	private float[] local_center;
	private float[] dim;
			
	/** The full 3D mesh */
	private float[] coord;
	private float[] normal;
	private float[] texCoord;
	private float[] tangent;
	
	/** The 2D facade */
	private float[] facade_coord;
	private float[] facade_normal;
	private float[] facade_texCoord;
	private float[] facade_tangent;
	
    /** Local set of windows */
    private HashSet<PositionableEntity> windows;

    /** Local set of doors */
    private HashSet<PositionableEntity> doors;

	/** The default bounds object */
	private OrientedBoundingBox bounds;
	
	/** Bounding extents */
	private float[] min;
	private float[] max;
	
	/**
	 * Constructor
	 */
	SegmentGeom() {
		
		vec = new Vector3f();
		rotation = new AxisAngle4f();
		translation = new Vector3f();
		mtx = new Matrix4f();
		pnt = new Point3f();
		
		v0 = new Vector3f();
		v1 = new Vector3f();
		n = new Vector3f();
		
        p = new Point3f[3];
        p[0] = new Point3f();
        p[1] = new Point3f();
        p[2] = new Point3f();
		
		position = new double[3];
		size = new float[3];
		scale = new float[3];
		inv = new Matrix4f();
		center = new Point3f();
		
		local_center = new float[3];
		dim = new float[3];
		
		windows = new HashSet<PositionableEntity>();
		doors = new HashSet<PositionableEntity>();
		
		bounds = new OrientedBoundingBox();
		min = new float[3];
		max = new float[3];
	}
	
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
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
	 * Return the facade vertex coordinates
	 *
	 * @return The facade vertex coordinates
	 */
	float[] getFacadeCoords() {
		return(facade_coord);
	}
	
	/**
	 * Return the facade normal vectors
	 *
	 * @return The facade normal vectors
	 */
	float[] getFacadeNormals() {
		return(facade_normal);
	}
	
	/**
	 * Return the facade texture coordinates
	 *
	 * @return The facade texture coordinates
	 */
	float[] getFacadeTexCoords() {
		return(facade_texCoord);
	}
	
	/**
	 * Return the facade tangent vectors
	 *
	 * @return The facade tangent vectors
	 */
	float[] getFacadeTangents() {
		return(facade_tangent);
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
	 * Calculate the geometry for the segment
	 *
	 * @param p0 The left vertex of the segment
	 * @param h0 The left height of the segment
	 * @param a0 The left miter angle
	 * @param p1 The right vertex of the segment
	 * @param h1 The right height of the segment
	 * @param a1 The right miter angle
	 * @param depth The depth of the segment
	 */
	void createGeom(
		float[] p0, float h0, double a0,
		float[] p1, float h1, double a1,
		float depth) {
		
		// presume a 2D model, on the xz plane
		vec.set(p1[0] - p0[0], 0, p1[2] - p0[2]);
		
		float width = vec.length();
		if (width == 0) {
			return;
		}
		float angle = -(float)Math.atan2(vec.z, vec.x);
		
		rotation.set(0, 1, 0, angle);
		translation.set(p0[0], 0, p0[2]);
		
		/////////////////////////////////////////////////////
		// bounding parameters
		float height = Math.max(h0, h1);
		dim[0] = width;
		dim[1] = height;
		dim[2] = depth;
		
		local_center[0] = width / 2;
		local_center[1] = height / 2;
		local_center[2] = -depth / 2;
		
		min[0] = 0;
		min[1] = 0;
		min[2] = -depth;
		
		max[0] = width;
		max[1] = height;
		max[2] = 0;
		bounds.setVertices(min, max);
		/////////////////////////////////////////////////////
		
        mtx.setIdentity();

        mtx.setRotation(rotation);
        mtx.setTranslation(translation);

		// get the exterior outline of the segment
		float[] front_ext_coord = getFrontExterior(width, h0, h1);
		float[] back_ext_coord = getBackExterior(width, h0, a0, h1, a1, depth);
		
		int num_exterior_coord = front_ext_coord.length;
		int num_exterior_vertex = num_exterior_coord / 3;
		
		float[] front_poly_coord = null;
		float[] back_poly_coord = null;
		
		// interiors are holes in the segment
		float[][] front_int_coord = getInteriors();
		float[][] back_int_coord = null;
		
		int num_interiors = front_int_coord.length;
		int num_contour = 1 + num_interiors;
		int[] strip_count = new int[num_contour];
		strip_count[0] = num_exterior_vertex;
		
		if (num_interiors > 0) {
			
			// if there are interiors, stage the coordinates
			// and parameters for the triangulator
			
			int num_coord = front_ext_coord.length;
			
			back_int_coord = new float[num_interiors][];
			for (int i = 0; i < num_interiors; i++) {
				
				int num_interior_coord = front_int_coord[i].length;
				int num_interior_vertex = num_interior_coord / 3;
				strip_count[1 + i] = num_interior_vertex;
				
				num_coord += num_interior_coord;
				back_int_coord[i] = new float[num_interior_coord];
				System.arraycopy(front_int_coord[i], 0, back_int_coord[i], 0, num_interior_coord);
				setDepth(back_int_coord[i], -depth);
			}
			
			front_poly_coord = new float[num_coord];
			back_poly_coord = new float[num_coord];
			
			int offset = 0;
			System.arraycopy(front_ext_coord, 0, front_poly_coord, offset, num_exterior_coord);
			System.arraycopy(back_ext_coord, 0, back_poly_coord, offset, num_exterior_coord);
			offset += front_ext_coord.length;
			
			for (int i = 0; i < num_interiors; i++) {
				System.arraycopy(front_int_coord[i], 0, front_poly_coord, offset, front_int_coord[i].length);
				System.arraycopy(back_int_coord[i], 0, back_poly_coord, offset, back_int_coord[i].length);
				offset += front_int_coord[i].length;
			}
		} else {
			// no interiors. just use the exterior coords.
			front_poly_coord = front_ext_coord;
			back_poly_coord = back_ext_coord;
		}
		int num_poly_vertex = front_poly_coord.length / 3;
		
		// triangulate the front face
		int[] face_tri_indices = null;
		if (num_poly_vertex == 4) {
			// if there are no holes or notches, don't bother
			// with the triangulation, we -know- how it is....
			face_tri_indices = new int[]{0, 1, 2, 0, 2, 3};
			
		} else {
			GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
			gi.setCoordinates(front_poly_coord);
			gi.setContourCounts(new int[]{num_contour});
			gi.setStripCounts(strip_count);
			gi.convertToIndexedTriangles();
			
			face_tri_indices = gi.getCoordinateIndices();
			
			// rem: is it necessary to check the winding?
			//checkCCW(front_poly_coord, face_tri_indices);
		}
		
		// convert the resulting indexed triangles into
		// triangle array form for the front
		int num_face_vertex = face_tri_indices.length;
		int num_face_coord = num_face_vertex * 3;
		float[] front_coord = new float[num_face_coord];
		int off = 0;
		int poly_off = 0;
		for (int i = 0; i < num_face_vertex; i++) {
			
			off = i * 3;
			poly_off = face_tri_indices[i] * 3;
			front_coord[off] = front_poly_coord[poly_off];
			front_coord[off+1] = front_poly_coord[poly_off+1];
			front_coord[off+2] = front_poly_coord[poly_off+2];
		}
		
		float[] front_texCoord;
		if (ENABLE_BUMP_MAP_TEXTURE) {
			front_texCoord = getFaceTexCoords(front_coord, true);
		} else {
			front_texCoord = getTexCoords(front_coord, true);
		}
	
		// triangle array output for the back
		float[] back_coord = new float[num_face_coord];
		int last_i = num_face_vertex - 1;
		for (int i = 0; i < num_face_vertex; i++) {
			
			off = i * 3;
			poly_off = face_tri_indices[last_i - i] * 3;
			// invert the ordering of the back
			back_coord[off] = back_poly_coord[poly_off];
			back_coord[off+1] = back_poly_coord[poly_off+1];
			back_coord[off+2] = back_poly_coord[poly_off+2];
		}
		
		float[] back_texCoord;
		if (ENABLE_BUMP_MAP_TEXTURE) {
			back_texCoord = getFaceTexCoords(back_coord, false);
		} else {
			back_texCoord = getTexCoords(back_coord, false);
		}
	
		// produce the side strips to complete the mesh
		int num_side_tris = num_poly_vertex * 2;
		int num_side_coord = num_side_tris * 3 * 3;
		float[] side_coord = new float[num_side_coord];
		
		int num_side_texCoord = num_side_tris * 3 * 2;
		float[] side_texCoord = new float[num_side_texCoord];
		
		off = 0;
		int tc_off = 0;
		for (int i = 0; i < num_contour; i++) {
			
			float[] front_edge_coord = null;
			float[] back_edge_coord = null;
			
			if (i == 0) {
				
				front_edge_coord = front_ext_coord;
				back_edge_coord = back_ext_coord;
				
			} else {
				
				front_edge_coord = front_int_coord[i - 1];
				back_edge_coord = back_int_coord[i - 1];
			}
			
			int num_edge_vertex = strip_count[i];
			int last_edge_vertex = num_edge_vertex - 1;
			for (int j = 0; j < num_edge_vertex; j++) {
				
				int back_off = j * 3;
				int frnt_off = j * 3;
				// triangle #1
				side_coord[off++] = front_edge_coord[frnt_off];
				side_coord[off++] = front_edge_coord[frnt_off+1];
				side_coord[off++] = front_edge_coord[frnt_off+2];
				
				side_coord[off++] = back_edge_coord[back_off];
				side_coord[off++] = back_edge_coord[back_off+1];
				side_coord[off++] = back_edge_coord[back_off+2];
				
				if (j == last_edge_vertex) {
					back_off = 0;
				} else {
					back_off += 3;
				}
				side_coord[off++] = back_edge_coord[back_off];
				side_coord[off++] = back_edge_coord[back_off+1];
				side_coord[off++] = back_edge_coord[back_off+2];
				
				// triangle #2
				side_coord[off++] = front_edge_coord[frnt_off];
				side_coord[off++] = front_edge_coord[frnt_off+1];
				side_coord[off++] = front_edge_coord[frnt_off+2];
				
				side_coord[off++] = back_edge_coord[back_off];
				side_coord[off++] = back_edge_coord[back_off+1];
				side_coord[off++] = back_edge_coord[back_off+2];
				
				if (j == last_edge_vertex) {
					frnt_off = 0;
				} else {
					frnt_off += 3;
				}
				side_coord[off++] = front_edge_coord[frnt_off];
				side_coord[off++] = front_edge_coord[frnt_off+1];
				side_coord[off++] = front_edge_coord[frnt_off+2];
				
				if (!ENABLE_BUMP_MAP_TEXTURE) {
					for (int k = 0; k < SIDE_SECTION_TC.length; k++) {
						side_texCoord[tc_off++] = SIDE_SECTION_TC[k];
					}
				}
			}
		}
		if (ENABLE_BUMP_MAP_TEXTURE) {
			getSideTexCoord(side_coord, depth, side_texCoord);
		}
		
		// aggregate the vertex coordinates
		int num_coord = front_coord.length + back_coord.length + side_coord.length;
		coord = new float[num_coord];
		
		off = 0;
		int length = front_coord.length;
		System.arraycopy(front_coord, 0, coord, off, length);
		off += length;
		length = back_coord.length;
		System.arraycopy(back_coord, 0, coord, off, length);
		off += length;
		length = side_coord.length;
		System.arraycopy(side_coord, 0, coord, off, length);
		
		// aggregate the texture coordinates
		int num_texCoord = front_texCoord.length + back_texCoord.length + side_texCoord.length;
		texCoord = new float[num_texCoord];
		
		off = 0;
		length = front_texCoord.length;
		System.arraycopy(front_texCoord, 0, texCoord, off, length);
		off += length;
		length = back_texCoord.length;
		System.arraycopy(back_texCoord, 0, texCoord, off, length);
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
		
		// setup the facade data
		facade_coord = front_coord;
		facade_texCoord = front_texCoord;
		facade_normal = getNormals(facade_coord);
		
		int num_facade_vertex = facade_coord.length / 3;
		int num_facade_tri = num_facade_vertex / 3;
		facade_tangent = new float[num_facade_vertex * 4];

		TriangleUtils.createTangents(
			num_facade_tri,
			facade_coord,
			facade_normal,
			facade_texCoord,
			facade_tangent);
	}
	
	/**
	 * Add an entity that represents a window
	 *
	 * @param pe The entity
	 */
	void addWindow(PositionableEntity pe) {
		windows.add(pe);
	}
	
	/**
	 * Add an entity that represents a door
	 *
	 * @param pe The entity
	 */
	void addDoor(PositionableEntity pe) {
		doors.add(pe);
	}
	
	/**
	 * Return whether the argument entity is currently
	 * embedded in the geometry
	 *
	 * @param pe The entity
	 * @return true if the entity is currently embedded in
	 * the geometry, false if not.
	 */
	boolean contains(PositionableEntity pe) {
		return(windows.contains(pe) | doors.contains(pe));
	}
	
	/**
	 * Return whether the argument entity is currently
	 * embedded in the geometry
	 *
	 * @param pe The entity
	 * @return true if the entity is currently embedded in
	 * the geometry, false if not.
	 */
	boolean containsWindow(PositionableEntity pe) {
		return(windows.contains(pe));
	}
	
	/**
	 * Return whether the argument entity is currently
	 * embedded in the geometry
	 *
	 * @param pe The entity
	 * @return true if the entity is currently embedded in
	 * the geometry, false if not.
	 */
	boolean containsDoor(PositionableEntity pe) {
		return(doors.contains(pe));
	}
	
	/**
	 * Remove an entity that represents a window
	 *
	 * @param pe The entity
	 */
	void removeWindow(PositionableEntity pe) {
		windows.remove(pe);
	}
	
	/**
	 * Remove an entity that represents a door
	 *
	 * @param pe The entity
	 */
	void removeDoor(PositionableEntity pe) {
		doors.remove(pe);
	}
	
	
	/**
	 * Calculate the front exterior perimeter for the segment
	 *
	 * @param width The width of the segment
	 * @param h0 The left height of the segment
	 * @param h1 The right height of the segment
	 * @return The front polygon coords
	 */
	private float[] getFrontExterior(float width, float h0, float h1) {
		
		float[][] door = getPerimeterCoords(doors);
		int num_door = door.length;
		int num_coord = 4 * 3 * (1 + num_door);
		float[] coord = new float[num_coord];
		int off = 0;
		
		// lower left corner
		coord[off++] = 0;
		coord[off++] = 0;
		coord[off++] = 0;
		
		// notches for doors
		for (int i = 0; i < num_door; i++) {
			int length = door[i].length;
			System.arraycopy(door[i], 0, coord, off, length);
			off += length;
		}
		// lower right corner
		coord[off++] = width;
		coord[off++] = 0;
		coord[off++] = 0;
		
		// upper right corner
		coord[off++] = width;
		coord[off++] = h1;
		coord[off++] = 0;
		
		// upper left corner
		coord[off++] = 0;
		coord[off++] = h0;
		coord[off++] = 0;
		
		return(coord);
	}
	
	/**
	 * Calculate the back geometry for the segment
	 *
	 * @param width The width of the segment
	 * @param h0 The left height of the segment
	 * @param a0 The left miter angle
	 * @param h1 The right height of the segment
	 * @param a1 The right miter angle
	 * @param depth The depth of the segment
	 * @return The back polygon coords
	 */
	private float[] getBackExterior(
		float width, 
		float h0, double a0,
		float h1, double a1,
		float depth) {
		
		float[][] door = getPerimeterCoords(doors);
		int num_door = door.length;
		int num_coord = 4 * 3 * (1 + num_door);
		float[] coord = new float[num_coord];
		int off = 0;
		
		float left_x = -(depth * (float)Math.tan(a0));
		float rght_x = width + (depth * (float)Math.tan(a1));
		
		// lower left corner
		coord[off++] = left_x;
		coord[off++] = 0;
		coord[off++] = -depth;
		
		// notches for doors
		for (int i = 0; i < num_door; i++) {
			int length = door[i].length;
			setDepth(door[i], -depth);
			System.arraycopy(door[i], 0, coord, off, length);
			off += length;
		}
		// lower right corner
		coord[off++] = rght_x;
		coord[off++] = 0;
		coord[off++] = -depth;
		
		// upper right corner
		coord[off++] = rght_x;
		coord[off++] = h1;
		coord[off++] = -depth;
		
		// upper left corner
		coord[off++] = left_x;
		coord[off++] = h0;
		coord[off++] = -depth;
		
		return(coord);
	}
	
	/**
	 * Set the depth of the array of vertices. Used to
	 * 'drop' front coordinates to the back plane.
	 *
	 * @param coord The vertex coordinates
	 * @param depth The depth
	 */
	private void setDepth(float[] coord, float depth) {
		int num_vrtx = coord.length / 3;
		int idx = 2;
		for (int i = 0; i < num_vrtx; i++) {
			coord[idx] = depth;
			idx += 3;
		}
	}
	
	/**
	 * Return the interior cut outs
	 */
	private float[][] getInteriors() {
		float[][] interiors = getPerimeterCoords(windows);
		return(interiors);
	}
	
	/**
	 * Return the edges of the requested entities. The coordinates
	 * are aligned with the XY plane and ordered clockwise. The perimeter
	 * arrays are ordered left to right (ascending X value)
	 *
	 * @param set The set of entities
	 */
	private float[][] getPerimeterCoords(HashSet<PositionableEntity> set) {
		
		float[][] perimeter = null;
		
		int num = set.size();
		if (num > 0) {
			
			perimeter = new float[num][];
			ArrayList<float[]> ordered_arrays = new ArrayList<float[]>();
			
			int idx = 0;
			for (Iterator<PositionableEntity> i = set.iterator(); i.hasNext();) {
				
				// determine the entity's alignment wrt the segment face
				PositionableEntity pe = i.next();
				pe.getPosition(position);
				center.set((float)position[0], (float)position[1], (float)position[2]);
				pe.getSize(size);
				pe.getScale(scale);
				
				float width2 = (size[0] * scale[0])/2;
				float height2 = (size[1] * scale[1])/2;
				float x_min = center.x - width2;
				float x_max = center.x + width2;
				float y_min = center.y - height2;
				float y_max = center.y + height2;
				
				// the coordinates on the segment face
				float[] data = new float[]{
					x_min, y_min, 0,
					x_min, y_max, 0,
					x_max, y_max, 0,
					x_max, y_min, 0,
				};
				// store the perimeters, left to right
				boolean added = false;
				for (int j = 0; j < idx; j++) {
					float x = ordered_arrays.get(j)[0];
					if (x_min < x) {
						ordered_arrays.add(j, data);
						added = true;
						break;
					}
				}
				if (!added) {
					ordered_arrays.add(data);
				}
				idx++;
			}
			for (int i = 0; i < num; i++) {
				perimeter[i] = ordered_arrays.get(i);
			} 
		} else {
			perimeter = EMPTY_SET;
		}
		return(perimeter);
	}
	
	/**
	 * Transform the coordinate array into place
	 *
	 * @param crd The coordinate array
	 */
	private void xform(float[] crd) {
		
		int num_coord = crd.length;
		int num_vertex = num_coord / 3;
		
		int off = 0;
		for (int i = 0; i < num_vertex; i++) {
			off = i * 3;
			pnt.set(crd[off], crd[off+1], crd[off+2]);
			mtx.transform(pnt);
			crd[off] = pnt.x;
			crd[off+1] = pnt.y;
			crd[off+2] = pnt.z;
		}
	}
	
	/**
	 * Return the texture coordinates for the segment face
	 *
	 * @param crd The vertex coordinates
	 * @param front Flag indicating the vertex coordinates are
	 * for the front (true) of the segment, or the back (false)
	 * @return The array of texture coordinates
	 */
	private float[] getTexCoords(float[] crd, boolean front) {
		
		int num_vertex = crd.length / 3;
		float[] texCoord = new float[num_vertex * 2];
		// determine the extents
		int idx = 0;
		float x, y;
		float x_max = crd[0];
		float x_min = x_max;
		float y_max = crd[1];
		float y_min = y_max;
		for (int i = 1; i < num_vertex; i++) {
			idx = i * 3;
			x = crd[idx];
			y = crd[idx + 1];
			if (x > x_max) {
				x_max = x;
			}
			if (x < x_min) {
				x_min = x;
			}
			if (y > y_max) {
				y_max = y;
			}
			if (y < y_min) {
				y_min = y;
			}
		}
		float x_span = x_max - x_min;
		float y_span = y_max - y_min;
		
		// generate the texure coordinates
		float s, t;
		idx = 0;
		int tc_idx = 0;
		for (int i = 0; i < num_vertex; i++) {
			idx = i * 3;
			tc_idx = i * 2;
			
			x = crd[idx];
			y = crd[idx + 1];
			
			if (front) {
				s = (x - x_min) / x_span;
			} else {
				s = (x_max - x) / x_span;
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
     * Walk the triangles, check each for counter-clockwise ordering.
     * If a triangle is ordered clockwise, invert it's indices.
     *
     * @param coord The vertex coordinates of an indexed triangle set
     * @param index The vertex indices of an indexed triangle set
     */
    public void checkCCW(float[] coord, int[] index) {

        int num_tri = index.length / 3;

        for (int tri = 0; tri < num_tri; tri++) {
			
            int idx = tri * 3;
            for (int vrt = 0; vrt < 3; vrt++) {
				
                int crd = index[idx] * 3;
                p[vrt].set(coord[crd], coord[crd+1], coord[crd+2]);
                idx++;
            }

            v0.sub(p[0], p[1]);
            v1.sub(p[1], p[2]);
            n.cross(v0, v1);

            if (n.z < 0) {
                idx = tri * 3;
                int tmp = index[idx];
                index[idx] = index[idx + 2];
                index[idx + 2] = tmp;
            }
        }
    }
	
	/**
	 * Calculate the bump map text coords for the segment edges. 
	 * The sides are constructed of pairs of triangle sets, 
	 * organized as pictured with each pair consisting 
	 * of 6 separate vertices.
	 *
	 * <pre>
	 *
	 *       front
	 *   ^ 0\  3---5
	 *   | | \  \  |
	 *   | |  \  \ |
	 *   t 1---2  \4
	 *     s------->
	 *        back
	 *
	 * </pre>
	 *
	 * @param side_coord The vertex coords for the sides
	 * @param depth The segment thickness
	 * @param side_texCoord The array to initialize with the tex coords
	 */
	private void getSideTexCoord(float[] side_coord, float depth, float[] side_texCoord) {
		
		int num_side = side_coord.length / (6 * 3);
		int s_idx = 0;
		int t_idx = 0;
		for (int i = 0; i < num_side; i++) {
			
			s_idx = (6 * 3) * i;
			float x0 = side_coord[s_idx];
			s_idx += 3;
			float x1 = side_coord[s_idx];
			float y1 = side_coord[s_idx + 1];
			s_idx += 3;
			float x2 = side_coord[s_idx];
			float y2 = side_coord[s_idx + 1];
			s_idx += 9;
			float x5 = side_coord[s_idx];
			if (((x0 != x1) || (x2 != x5)) && (y1 == y2)) {
				// rem: this is a special case of the segment being
				// mitered at a corner. these conditions seem pretty
				// specific to the wall layout that is implemented
				// for closetmaid. suspect they might not work for
				// something more general case......
				
				// the wall thickness is used as (t)
				// the distance from the minimum x is used as (s)
				float x_min = (x0 < x1) ? x0 : x1;
				side_texCoord[t_idx++] = x0 - x_min;
				side_texCoord[t_idx++] = depth;
				side_texCoord[t_idx++] = x1 - x_min;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = x2 - x_min;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = x0 - x_min;
				side_texCoord[t_idx++] = depth;
				side_texCoord[t_idx++] = x2 - x_min;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = x5 - x_min;
				side_texCoord[t_idx++] = depth;
			} else {
				// the wall thickness is used as (t)
				// the panel width is used as (s) and is calculated by taking
				// the distance from vertex 1 to 2.
				float deltax = x2 - x1;
				float deltay = y2 - y1;
				float width = (float)Math.sqrt(deltax * deltax + deltay * deltay);
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = depth;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = width;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = depth;
				side_texCoord[t_idx++] = width;
				side_texCoord[t_idx++] = 0;
				side_texCoord[t_idx++] = width;
				side_texCoord[t_idx++] = depth;
			}
		}
	}
			
	/**
	 * Return the texture coordinates for the segment face
	 *
	 * @param crd The vertex coordinates
	 * @param front Flag indicating the vertex coordinates are
	 * for the front (true) of the segment, or the back (false)
	 * @return The array of texture coordinates
	 */
	private float[] getFaceTexCoords(float[] crd, boolean front) {
		
		int num_vertex = crd.length / 3;
		float[] texCoord = new float[num_vertex * 2];
		// determine the extents
		int idx = 0;
		float x, y;
		float x_max = crd[0];
		float x_min = x_max;
		for (int i = 1; i < num_vertex; i++) {
			idx = i * 3;
			x = crd[idx];
			if (x > x_max) {
				x_max = x;
			}
			if (x < x_min) {
				x_min = x;
			}
		}
		
		// generate the texure coordinates
		float s, t;
		idx = 0;
		int tc_idx = 0;
		for (int i = 0; i < num_vertex; i++) {
			idx = i * 3;
			tc_idx = i * 2;
			
			x = crd[idx];
			y = crd[idx + 1];
			
			if (front) {
				s = x - x_min;
			} else {
				s = x_max - x;
			}
			t = y;
			
			texCoord[tc_idx] = s;
			texCoord[tc_idx + 1] = t;
		}
		return(texCoord);
	}
}
