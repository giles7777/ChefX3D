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

package org.chefx3d.view.boundingbox;

// External imports
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

// Local imports

/**
 * An oriented bounding box representation of a Segment
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public class SegmentBoundingBox extends OrientedBoundingBox {
	
	/** Scratch vecmath objects */
	private Vector3f vec;
	private AxisAngle4f rotation;
	private Vector3f translation;
	private Matrix4f mtx;
	
	/** Scratch bounding extents */
	private float[] min;
	private float[] max;
	
	/**
	 * Constructor
	 */
	public SegmentBoundingBox() {
		
		super();
		
		vec = new Vector3f();
		rotation = new AxisAngle4f();
		translation = new Vector3f();
		mtx = new Matrix4f();
		
		min = new float[3];
		max = new float[3];
	}
	
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
	/**
	 * Return the matrix that will transform the proxy bounds object
	 * to the position and dimensions of the segment.
	 *
	 * @param result_mtx The matrix to initialize, or null and a new
	 * matrix will be returned.
	 * @return The initialized matrix
	 */
	public Matrix4f getMatrix(Matrix4f result_mtx) {
		if (result_mtx == null) {
			result_mtx = new Matrix4f();
		}
		result_mtx.set(mtx);
		return(result_mtx);
	}
	
	/**
	 * Configure the proxy bounds object and it's associated matrix to
	 * reflect the dimensions and position respectively of the segment.
	 *
	 * @param p0 The left vertex of the segment
	 * @param p1 The right vertex of the segment
	 * @param height The height of the segment
	 * @param depth The depth of the segment
	 */
	public void update(float[] p0, float[] p1, float height, float depth) {
		
		vec.set(p1[0] - p0[0], 0, p1[2] - p0[2]);
		
		float width = vec.length();
		
		/////////////////////////////////////////////////////
		// bounding parameters
		min[0] = 0;
		min[1] = 0;
		min[2] = -depth;
		
		max[0] = width;
		max[1] = height;
		max[2] = 0;
		setVertices(min, max);
		/////////////////////////////////////////////////////
		
		vec.normalize();
		
		float angle = -(float)Math.atan2(vec.z, vec.x);
 
		rotation.set(0, 1, 0, angle);
		translation.set(p0[0], 0, p0[2]);
		
        mtx.setIdentity();
        mtx.setRotation(rotation);
        mtx.setTranslation(translation);
	}
}
