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
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.*;

// Local imports
// none

/**
 * Utility class to produce the bounding geometry of a Segment
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class SegmentBoundsGeom {
	
	/** 
	 * Coordinates of a unit sized box. 
	 * Lower left corner at [0, 0, 0].
	 *
	 *              7-------6
	 *             /|      /|
	 *            / |     / |
	 *           /  |    /  |
	 *          /   4---/---5
	 *         /   /   /   /
	 *        3-------2   /
	 *        |  /    |  /
	 *        | /     | /
	 *        |/      |/
	 *        0-------1
	 */
	private static final float[] BOX_COORDS = new float[]{
		0, 0, 0,
		1, 0, 0,
		1, 1, 0,
		0, 1, 0,
		0, 0, -1,
		1, 0, -1,
		1, 1, -1,
		0, 1, -1,
	};
	
	/** Indices */
	private static final int[] BOX_INDICES = new int[]{
		// front
		0, 1, 2,
		0, 2, 3,
		// right
		1, 5, 6,
		1, 6, 2,
		// back
		5, 4, 7,
		5, 7, 6,
		// left
		4, 0, 3,
		4, 3, 7,
		// top 
		3, 2, 6,
		3, 6, 7,
		// bottom
		4, 5, 1,
		4, 1, 0,
	};
	
	/** Scratch vecmath objects */
	private Vector3f vec;
	private AxisAngle4f rotation;
	private Vector3f translation;
	private Matrix4f mtx0;
	
	/** The default bounds proxy model for all segments */
	private TransformGroup boundsProxy;
	
	/**
	 * Constructor
	 */
	SegmentBoundsGeom() {
		
		vec = new Vector3f();
		rotation = new AxisAngle4f();
		translation = new Vector3f();
		mtx0 = new Matrix4f();
		
		boundsProxy = createBoundsProxy();
	}
	
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
	/**
	 * Return the matrix that will transform the proxy bounds object
	 * to the position and dimensions of the segment.
	 *
	 * @param p0 The left vertex of the segment
	 * @param p1 The right vertex of the segment
	 * @param height The height of the segment
	 * @param depth The depth of the segment
	 * @param result_mtx The matrix to initialize, or null and a new
	 * matrix will be returned.
	 * @return The initialized matrix
	 */
	Matrix4f getMatrix(float[] p0, float[] p1, float height, float depth, Matrix4f result_mtx) {
		
		if (result_mtx == null) {
			result_mtx = new Matrix4f();
		}
		// note: this is simplified to a 2D problem, presuming that the segment
		// is normal to the xz plane, with y=0 as the base. a more general 3D
		// calculation is required.
		
		//vec.set(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]);
		vec.set(p1[0] - p0[0], 0, p1[2] - p0[2]);
		
		float width = vec.length();
		
		result_mtx.setIdentity();
		result_mtx.m00 = width;
        result_mtx.m11 = height;
        result_mtx.m22 = depth;
		
		vec.normalize();
		
		float angle = -(float)Math.atan2(vec.z, vec.x);
 
		rotation.set(0, 1, 0, angle);
		translation.set(p0[0], 0, p0[2]);
		
        mtx0.setIdentity();

        mtx0.setRotation(rotation);
        mtx0.setTranslation(translation);

        result_mtx.mul(mtx0, result_mtx);
		
		return(result_mtx);
	}

    /**
     * Return the bounds representation proxy. DO NOT modify
	 * this representation, it is shared.
     *
     * @return The bounds proxy
     */
	TransformGroup getBoundsProxy() {
		return(boundsProxy);
	}
	
    /**
     * Generate the default geometry for a bounds representation.
     *
     * @return The bounds proxy
     */
    private TransformGroup createBoundsProxy() {
		
        IndexedTriangleArray ita = new IndexedTriangleArray(
            false,
            VertexGeometry.VBO_HINT_STATIC);

        ita.setVertices(
            IndexedTriangleArray.COORDINATE_3,
            BOX_COORDS,
            BOX_COORDS.length / 3);
		
		ita.setIndices(
			BOX_INDICES,
			BOX_INDICES.length);

        Shape3D shape = new Shape3D();
        shape.setGeometry(ita);

        TransformGroup tg = new TransformGroup();
		tg.addChild(shape);
		tg.requestBoundsUpdate();
		
        return(tg);
    }
}
