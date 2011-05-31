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
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.j3d.aviatrix3d.Geometry;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.IndexedTriangleArray;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.TriangleArray;
import org.j3d.aviatrix3d.TriangleStripArray;

// Local imports
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Utility class for checking entity intersections.
 *
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
class IntersectionUtils {

    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f tmtx;
    private Matrix4f mtx;
    private Point3f pnt;
	
	/** Scratch set of triangle vertices */
	private float[][] vtx;
    
	/** Scratch triangle parameters */
	private int[] index;
	private int[] strip;
	private float[] vertex;
	
	/**
	 * Constructor
	 */
	IntersectionUtils() {
		
        tu = new TransformUtils();
        tmtx = new Matrix4f();
        mtx = new Matrix4f();
        pnt = new Point3f();
		
		vtx = new float[3][];
		vtx[0] = new float[3];
		vtx[1] = new float[3];
		vtx[2] = new float[3];
		
		index = new int[0];
		strip = new int[0];
		vertex = new float[0];
	}
	
	/**
	 * Determine whether the geometry associated with the argument 
	 * entity wrapper intersects with the argument bounds.
	 *
	 * @param bounds The bounds to check against
	 * @param wrapper The wrapper of the entity
	 * @param useEpsilon Flag indicating that the epsilon tolerance 
	 * value should be used in the intersection check
	 * @return true if an intersection is detected, false otherwise
	 */
	boolean check(OrientedBoundingBox bounds, AV3DEntityWrapper wrapper, 
		boolean useEpsilon) {
		
		boolean hasIntersection = false;
		if ((bounds != null) && (wrapper != null)) {
			Node[] nodes = wrapper.contentGroup.getAllChildren();
			tu.getLocalToVworld(wrapper.contentGroup, mtx);
			
			hasIntersection = checkShapes(bounds, nodes, mtx, useEpsilon);
		}
		return(hasIntersection);
	}
		
	/**
	 * Walk down the scenegraph hierarchy of the nodes, check each 
	 * shape as it's found for intersection with the argument bounds.
	 *
	 * @param bounds The bounds to check against 
	 * @param nodes The nodes to check
	 * @param mtx The transformation applied to the Geometry
	 * @param useEpsilon Flag indicating that the epsilon tolerance 
	 * value should be used in the intersection check
	 * @return true if an intersection is detected, false otherwise
	 */
	private boolean checkShapes(OrientedBoundingBox bounds, Node[] nodes, Matrix4f mtx, 
		boolean useEpsilon) {
		
		boolean hasIntersection = false;
		if (nodes != null) {
			for (int i = 0; i < nodes.length; i++) {
				Node node = nodes[i];
				if (node != null) {
					if (node instanceof Group) {
						Group group = (Group)node;
						if (group.numChildren() > 0) {
							if (group instanceof TransformGroup) {
								TransformGroup transform = (TransformGroup)group;
								transform.getTransform(tmtx);
								mtx.mul(tmtx);
							}
							Node[] children = group.getAllChildren();
							hasIntersection = checkShapes(bounds, children, mtx, useEpsilon);
						}
					} else if (node instanceof Shape3D) {
						Shape3D shape = (Shape3D)node;
						Geometry geom = shape.getGeometry();
						if (geom != null) {
							hasIntersection = checkGeometry(bounds, geom, mtx, useEpsilon);
						}
					}
				}
				if (hasIntersection) {
					break;
				}
			}
		}
		return(hasIntersection);
	}
	
	/**
	 * Determine if the geometry intersects the bounds
	 *
	 * @param bounds The bounds to check against
	 * @param geom The Geometry object 
	 * @param mtx The transformation applied to the Geometry
	 * @param useEpsilon Flag indicating that the epsilon tolerance 
	 * value should be used in the intersection check
	 * @return true if an intersection is detected, false otherwise
	 */
	private boolean checkGeometry(OrientedBoundingBox bounds, Geometry geom, Matrix4f mtx, 
		boolean useEpsilon) {
		
		if (geom instanceof IndexedTriangleArray) {
			
			IndexedTriangleArray ita = (IndexedTriangleArray)geom;
			
			int num_indices = ita.getValidIndexCount();
			int num_vertices = ita.getValidVertexCount();
			
			if ((num_indices > 0) && (num_vertices > 0)) {
				
				resizeIndex(num_indices);
				ita.getIndices(index);
				
				resizeVertex(num_vertices * 3);
				ita.getVertices(vertex);
				
				int num_tri = num_indices / 3;
				int tri_idx = 0;
				int vtx_idx = 0;
				for (int j = 0; j < num_tri; j++) {
					tri_idx = j * 3;
					for (int k = 0; k < 3; k++) {
						vtx_idx = index[tri_idx + k] * 3;
						pnt.x = vertex[vtx_idx];
						pnt.y = vertex[vtx_idx + 1];
						pnt.z = vertex[vtx_idx + 2];
						mtx.transform(pnt);
						pnt.get(vtx[k]);
					}
					if (bounds.checkIntersectionTriangle(vtx[0], vtx[1], vtx[2], useEpsilon)) {
						return(true);
					}
				}
			}
		} else if (geom instanceof TriangleArray) {
			
			TriangleArray ta = (TriangleArray)geom;
			
			int num_vertices = ta.getValidVertexCount();
			
			if (num_vertices > 0) {
				
				resizeVertex(num_vertices * 3);
				ta.getVertices(vertex);
				
				int num_tri = num_vertices / 3;
				int tri_idx = 0;
				int vtx_idx = 0;
				for (int j = 0; j < num_tri; j++) {
					tri_idx = j * 3;
					for (int k = 0; k < 3; k++) {
						vtx_idx = (tri_idx + k) * 3;
						pnt.x = vertex[vtx_idx];
						pnt.y = vertex[vtx_idx + 1];
						pnt.z = vertex[vtx_idx + 2];
						mtx.transform(pnt);
						pnt.get(vtx[k]);
					}
					if (bounds.checkIntersectionTriangle(vtx[0], vtx[1], vtx[2], useEpsilon)) {
						return(true);
					}
				}
			}
		} else if (geom instanceof TriangleStripArray) {
			
			TriangleStripArray tsa = (TriangleStripArray)geom;
			
			int num_strips = tsa.getValidStripCount();
			int num_vertices = tsa.getValidVertexCount();
			
			if ((num_strips > 0) && (num_vertices > 0)) {
				
				resizeStrip(num_strips);
				tsa.getStripCount(strip);
				
				resizeVertex(num_vertices * 3);
				tsa.getVertices(vertex);
				
				boolean even = true;
				int idx = 0;
				int vtx_idx = 0;
				for (int j = 0; j < num_strips; j++) {
					int num_vtx = strip[j];
					int num_tri = num_vtx - 2;
					for (int x = 0; x < num_tri; x++) {
						for (int k = 0; k < 3; k++) {
							if (even) {
								vtx_idx = (idx + k) * 3;
							} else {
								vtx_idx = (idx + 2 - k) * 3;
							}
							pnt.x = vertex[vtx_idx];
							pnt.y = vertex[vtx_idx + 1];
							pnt.z = vertex[vtx_idx + 2];
							mtx.transform(pnt);
							pnt.get(vtx[k]);
						}
						if (bounds.checkIntersectionTriangle(vtx[0], vtx[1], vtx[2], useEpsilon)) {
							return(true);
						}
						even = !even;
						idx++;
					}
					even = true;
					idx += 2;
				}
			}
		}  else {
			System.out.println("IntersectionUtils: Unhandled geometry type: "+ geom);
		}
		return(false);
	}
	
	/**
	 * Ensure the index array has a minimum capacity
	 *
	 * @param capacity The minimum 
	 */
	private final void resizeIndex(int capacity) {
		if (index.length < capacity) {
			index = new int[capacity];
		}
	}
	
	/**
	 * Ensure the strip array has a minimum capacity
	 *
	 * @param capacity The minimum 
	 */
	private final void resizeStrip(int capacity) {
		if (strip.length < capacity) {
			strip = new int[capacity];
		}
	}
	
	/**
	 * Ensure the vertex coordinate array has a minimum capacity
	 *
	 * @param capacity The minimum 
	 */
	private final void resizeVertex(int capacity) {
		if (vertex.length < capacity) {
			vertex = new float[capacity];
		}
	}
}
