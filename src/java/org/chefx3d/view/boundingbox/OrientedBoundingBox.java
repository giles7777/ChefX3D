/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009 - 2010
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
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

// Local imports
// none

/**
 * An oriented bounding box.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public class OrientedBoundingBox extends AxisAlignedBoundingBox {
	
	/** Value for dot product (cosine of angle) between box axes
	 *  that determines them to be parallel */
	private static final float MAX_COS_VALUE = 0.999999f;
	
	/** Default axis vectors for calculations */
	public static final Vector3f POS_X_AXIS = new Vector3f(1, 0, 0);
	public static final Vector3f POS_Y_AXIS = new Vector3f(0, 1, 0);
	public static final Vector3f POS_Z_AXIS = new Vector3f(0, 0, 1);

    /** The extent along each side of each axis */
    protected final Vector3f extent;

    /** X axis of the Oriented Box. */
    protected final Vector3f xAxis;

    /** Y axis of the Oriented Box. */
    protected final Vector3f yAxis;

    /** Z axis of the Oriented Box. */
    protected final Vector3f zAxis;

	/** Scratch vecmath & array objects for bounds intersection testing */
	private Vector3f v;
	private Vector3f[] A;
	private Vector3f[] B;
	
	private float[] a;
	private float[] b;
	private float[] T;
	
	private float[][] R;
	private float[][] Rabs;
	
	/** Inverted matrix applied to this, used for triangle intersection */
	private Matrix4f i_mtx;
	private Point3f pnt;
	
    /**
     * Default constructor
     */
    public OrientedBoundingBox() {
		
		super();
		
		extent = new Vector3f();
		
		xAxis = new Vector3f(POS_X_AXIS);
		yAxis = new Vector3f(POS_Y_AXIS);
		zAxis = new Vector3f(POS_Z_AXIS);
		
		v = new Vector3f();
		
		a = new float[3];
		b = new float[3];
		T = new float[3];
		
		A = new Vector3f[3];
		A[0] = xAxis;
		A[1] = yAxis;
		A[2] = zAxis;
		
		B = new Vector3f[3];
		
		R = new float[3][];
		R[0] = new float[3];
		R[1] = new float[3];
		R[2] = new float[3];
		
		Rabs = new float[3][];
		Rabs[0] = new float[3];
		Rabs[1] = new float[3];
		Rabs[2] = new float[3];
		
		i_mtx = new Matrix4f();
		pnt = new Point3f();
    }
    
    /**
     * Construct a bounding box
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
	 * @param scl The scale to apply to the vertices 
     */
    public OrientedBoundingBox(Point3f min, Point3f max, Vector3f scl) {
		
        this();
        
        min_vtx.set(min);
        max_vtx.set(max);
		scale.set(scl);
		
        update();
    }
    
    /**
     * Construct a bounding box
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
	 * @param scl The scale to apply to the vertices 
     */
    public OrientedBoundingBox(float[] min, float[] max, float[] scl) {
		
        this();
        
        min_vtx.set(min);
        max_vtx.set(max);
		scale.set(scl);
		
        update();
    }
    
    /**
     * Check for the given OrientedBoundingBox intersecting this.
	 *
     * @param that An OrientedBoundingBox to check against
     * @return true if that intersects this, false otherwise
     */
    public boolean intersect(OrientedBoundingBox that) {
		return(intersect(that, false));
	}
	
    /**
     * Check for the given OrientedBoundingBox intersecting this.
	 * <p>
	 * The intersection algorithm was implemented directly from 
	 * <a href="http://www.gamasutra.com/view/feature/3383/simple_intersection_tests_for_games.php?page=5">
	 * Simple Intersection Tests For Games</a>. It implements a 
	 * check using the 
	 * <a href="http://en.wikipedia.org/wiki/Separating_Axis_Theorem">
	 * Separating Axis Theorem</a>.
     *
     * @param that An OrientedBoundingBox to check against
	 * @param useEpsilon Flag indicating that the epsilon tolerance 
	 * value should be used in the intersection check
     * @return true if that intersects this, false otherwise
     */
    public boolean intersect(OrientedBoundingBox that, boolean useEpsilon) {
		boolean axis_intersect = super.intersect(that, useEpsilon);
		if (axis_intersect) {
			
			this.extent.get(a);
			that.extent.get(b);
			
			B[0] = that.xAxis;
			B[1] = that.yAxis;
			B[2] = that.zAxis;
			
			v.sub(that.center, this.center);
			
			T[0] = v.dot(A[0]);
			T[1] = v.dot(A[1]);
			T[2] = v.dot(A[2]);
			
        	boolean hasParallelAxis = false;
			int i;
			int k;
			float r;
			for (i = 0; i < 3; i++) {
				for (k = 0; k < 3; k++) {
					r = A[i].dot(B[k]);
					R[i][k] = r;
					Rabs[i][k] = Math.abs(r);
					if (Rabs[i][k] > MAX_COS_VALUE) {
						hasParallelAxis = true;
					}
				}
			}
			float ra;
			float rb;
			float t;
			// A's basis vectors
			for (i = 0; i < 3; i++) {
				ra = a[i];
				rb = b[0] * Rabs[i][0] + b[1] * Rabs[i][1] + b[2] * Rabs[i][2];
				t = Math.abs(T[i]);
				if (t > ra + rb) {
//System.out.println("A["+i+"]");
					if (!useEpsilon) {
						return(false);
					} else {
						if ((t - (ra + rb)) > epsilon) {
							return(false);
						}
					}
				}
			}
			// B's basis vectors
			for (k = 0; k < 3; k++) {
				ra = a[0] * Rabs[0][k] + a[1] * Rabs[1][k] + a[2] * Rabs[2][k];
				rb = b[k];
				t = Math.abs(T[0] * R[0][k] + T[1] * R[1][k] + T[2] * R[2][k]);
				if (t > ra + rb) {
//System.out.println("A["+k+"]");
					if (!useEpsilon) {
						return(false);
					} else {
						if ((t - (ra + rb)) > epsilon) {
							return(false);
						}
					}
				}
			}
			
			///////////////////////////////////////////////////////
			// if a pair of oriented axis' are parallel, then
			// the remainder of the checks are unnecessary, and
			// apparently can cause false negatives in some 
			// close cases.
			if (hasParallelAxis) {
				return(true);
			}
			///////////////////////////////////////////////////////
			
			// 9 cross products
			
			//L = A0 x B0
			ra = a[1] * Rabs[2][0] + a[2] * Rabs[1][0];
			rb = b[1] * Rabs[0][2] + b[2] * Rabs[0][1];
			t = Math.abs(T[2] * R[1][0] - T[1] * R[2][0]);
			if (t > ra + rb) {
//System.out.println("A0 x B0");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A0 x B1
			ra = a[1] * Rabs[2][1] + a[2] * Rabs[1][1];
			rb = b[0] * Rabs[0][2] + b[2] * Rabs[0][0];
			t = Math.abs(T[2] * R[1][1] - T[1] * R[2][1]);
			if (t > ra + rb) {
//System.out.println("A0 x B1");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A0 x B2
			ra = a[1] * Rabs[2][2] + a[2] * Rabs[1][2];
			rb = b[0] * Rabs[0][1] + b[1] * Rabs[0][0];
			t = Math.abs(T[2] * R[1][2] - T[1] * R[2][2]);
			if (t > ra + rb) {
//System.out.println("A0 x B2");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A1 x B0
			ra = a[0] * Rabs[2][0] + a[2] * Rabs[0][0];
			rb = b[1] * Rabs[1][2] + b[2] * Rabs[1][1];
			t = Math.abs(T[0] * R[2][0] - T[2] * R[0][0]);
			if (t > ra + rb) {
//System.out.println("A1 x B0");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A1 x B1
			ra = a[0] * Rabs[2][1] + a[2] * Rabs[0][1];
			rb = b[0] * Rabs[1][2] + b[2] * Rabs[1][0];
			t = Math.abs(T[0] * R[2][1] - T[2] * R[0][1]);
			if (t > ra + rb) {
//System.out.println("A1 x B1");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A1 x B2
			ra = a[0] * Rabs[2][2] + a[2] * Rabs[0][2];
			rb = b[0] * Rabs[1][1] + b[1] * Rabs[1][0];
			t = Math.abs(T[0] * R[2][2] - T[2] * R[0][2]);
			if (t > ra + rb) {
//System.out.println("A1 x B2");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A2 x B0
			ra = a[0] * Rabs[1][0] + a[1] * Rabs[0][0];
			rb = b[1] * Rabs[2][2] + b[2] * Rabs[2][1]; 
			t = Math.abs(T[1] * R[0][0] - T[0] * R[1][0]); 
			if (t > ra + rb) {
//System.out.println("A2 x B0");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A2 x B1
			ra = a[0] * Rabs[1][1] + a[1] * Rabs[0][1];
			rb = b[0] * Rabs[2][2] + b[2] * Rabs[2][0];
			t = Math.abs(T[1] * R[0][1] - T[0] * R[1][1]); 
			if (t > ra + rb) {
//System.out.println("A2 x B1");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			//L = A2 x B2
			ra = a[0] * Rabs[1][2] + a[1] * Rabs[0][2];
			rb = b[0] * Rabs[2][1] + b[1] * Rabs[2][0];
			t = Math.abs(T[1] * R[0][2] - T[0] * R[1][2]); 
			if (t > ra + rb) {
//System.out.println("A2 x B2");
				if (!useEpsilon) {
					return(false);
				} else {
					if ((t - (ra + rb)) > epsilon) {
						return(false);
					}
				}
			}
			
			// no separating axis found, the two boxes overlap
			return(true);
			
		} else {
			return(false);
		}
    }
    
    /**
     * Transform the current bounds by the argument matrix.
     *
     * @param mat The matrix to transform this bounds by
     */
    public void transform(Matrix4f mat) {
		
        super.transform(mat);
		
		i_mtx.set(mat);
		i_mtx.invert();
		
		xAxis.set(POS_X_AXIS);
		yAxis.set(POS_Y_AXIS);
		zAxis.set(POS_Z_AXIS);
		
		mat.transform(xAxis);
		mat.transform(yAxis);
		mat.transform(zAxis);
    }
     
    //---------------------------------------------------------------
    // Local methods
    //---------------------------------------------------------------
    
    /**
     * Get the current transform applied to the oriented bounding box.
     * 
     * @return Matrix4f applied to the oriented bounding box.
     */
    public Matrix4f getTransform() {
    	
    	Matrix4f mat = new Matrix4f();
    	mat.set(i_mtx);
    	mat.invert();
    	
    	return mat;
    }
    
    /**
     * Generate a string representation of this box.
     *
     * @return A string representing the bounds information
     */
    public String toString() {
        return(
			"OrientedBoundingBox: "+
			"min("+ min_ext.x +' '+ min_ext.y +' '+ min_ext.z +") "+
			"max("+ max_ext.x +' '+ max_ext.y +' '+ max_ext.z +") ");
    }
    
	/**
	 * Copy the contents of that into this
	 *
	 * @param that The object to initialize from
	 */
	public void copy(OrientedBoundingBox that) {
		super.copy(that);
		this.extent.set(that.extent);
	}
	
    /**
     * Update the extents, center and vertices of the box based on the
     * current min and max positions.
     */
	protected void update() {
		
		super.update();
		
        extent.set(
			max_ext.x - center.x + border.x, 
			max_ext.y - center.y + border.y, 
			max_ext.z - center.z + border.z);

		xAxis.set(POS_X_AXIS);
		yAxis.set(POS_Y_AXIS);
		zAxis.set(POS_Z_AXIS);
    }
	
	/**
	 * Check for the given triangle intersecting this bounds.
	 *
	 * @param v0 The first vertex of the triangle
	 * @param v1 The second vertex of the triangle
	 * @param v2 The third vertex of the triangle
	 * @return true if the triangle intersects this bounds
	 */
	public boolean checkIntersectionTriangle(float[] v0, float[] v1, float[] v2) {
		return(checkIntersectionTriangle(v0, v1, v2, false));
	}
	
	/**
	 * Check for the given triangle intersecting this bounds. 
	 * 
	 * An implementation of this code in Java:<br>
	 * <a href="http://www.cs.lth.se/home/Tomas_Akenine_Moller/code/tribox3.txt">
	 * AABB-triangle overlap test code</a>
	 *
	 * @param v0 The first vertex of the triangle
	 * @param v1 The second vertex of the triangle
	 * @param v2 The third vertex of the triangle
	 * @param useEpsilon Flag indicating that the epsilon tolerance 
	 * value should be used in the intersection check
	 * @return true if the triangle intersects this bounds
	 */
	public boolean checkIntersectionTriangle(float[] v0, float[] v1, float[] v2, 
		boolean useEpsilon) {
		
		////////////////////////////////////////////////////////////////
		// transform the triangle to be axis aligned and centered
		// on this bounding boxes coordinate system(from world space) 
		pnt.set(v0);
		i_mtx.transform(pnt);
		pnt.get(v0);
		float vtx0_x = v0[0];
		float vtx0_y = v0[1];
		float vtx0_z = v0[2];
		
		pnt.set(v1);
		i_mtx.transform(pnt);
		pnt.get(v1);
		float vtx1_x = v1[0];
		float vtx1_y = v1[1];
		float vtx1_z = v1[2];
		
		pnt.set(v2);
		i_mtx.transform(pnt);
		pnt.get(v2);
		float vtx2_x = v2[0];
		float vtx2_y = v2[1];
		float vtx2_z = v2[2];
		////////////////////////////////////////////////////////////////
		
		float edge0_x = vtx1_x - vtx0_x;
		float edge0_y = vtx1_y - vtx0_y;
		float edge0_z = vtx1_z - vtx0_z;
		
		float edge1_x = vtx2_x - vtx1_x;
		float edge1_y = vtx2_y - vtx1_y;
		float edge1_z = vtx2_z - vtx1_z;
		
		float edge2_x = vtx0_x - vtx2_x;
		float edge2_y = vtx0_y - vtx2_y;
		float edge2_z = vtx0_z - vtx2_z;
		
		// extent is the 'half' size along each local axis
		float size_x = extent.x;
		float size_y = extent.y;
		float size_z = extent.z;
		
		if (useEpsilon) {
			size_x += epsilon;
			size_y += epsilon;
			size_z += epsilon;
		}
		
		// start with separating axis tests = one for each edge
		float fex = Math.abs(edge0_x);
		float fey = Math.abs(edge0_y);
		float fez = Math.abs(edge0_z);
		
		if (!axisTest(edge0_z, edge0_y, fez, fey, vtx0_y, vtx0_z, vtx2_y, vtx2_z, size_y, size_z)) {
			return(false);
		}
		
		if (!axisTest(edge0_z, edge0_x, fez, fex, vtx0_x, vtx0_z, vtx2_x, vtx2_z, size_x, size_z)) {
			return(false);
		}
		
		if (!axisTest(edge0_y, edge0_x, fey, fex, vtx1_x, vtx1_y, vtx2_x, vtx2_y, size_x, size_y)) {
			return(false);
		}
		
		fex = Math.abs(edge1_x);
		fey = Math.abs(edge1_y);
		fez = Math.abs(edge1_z);
		
		if (!axisTest(edge1_z, edge1_y, fez, fey, vtx0_y, vtx0_z, vtx2_y, vtx2_z, size_y, size_z)) {
			return(false);
		}
		
		if (!axisTest(edge1_z, edge1_x, fez, fex, vtx0_x, vtx0_z, vtx2_x, vtx2_z, size_x, size_z)) {
			return(false);
		}
		
		if (!axisTest(edge1_y, edge1_x, fey, fex, vtx0_x, vtx0_y, vtx1_x, vtx1_y, size_x, size_y)) {
			return(false);
		}
		
		fex = Math.abs(edge2_x);
		fey = Math.abs(edge2_y);
		fez = Math.abs(edge2_z);
		
		if (!axisTest(edge2_z, edge2_y, fez, fey, vtx0_y, vtx0_z, vtx1_y, vtx1_z, size_y, size_z)) {
			return(false);
		}
		
		if (!axisTest(edge2_z, edge2_x, fez, fex, vtx0_x, vtx0_z, vtx1_x, vtx1_z, size_x, size_z)) {
			return(false);
		}
		
		if (!axisTest(edge2_y, edge2_x, fey, fex, vtx1_x, vtx1_y, vtx2_x, vtx2_y, size_x, size_y)) {
			return(false);
		}
		
		// Test the overlap in the x,y,z axis directions. Find min, max of the
		// triangle in each direction, and test for overlap in that direction.
		// Equiv to testing a minimal AABB around the triangle against this
		// bbox
		float min = vtx0_x;
		float max = vtx0_x;
		
		if (vtx1_x < min) {
			min = vtx1_x;
		}
		if (vtx1_x > max) {
			max = vtx1_x;
		}
		if (vtx2_x < min) {
			min = vtx2_x;
		}
		if (vtx2_x > max) {
			max = vtx2_x;
		}
		
		if ((min > size_x) || (max < -size_x)) {
			return(false);
		}
		
		min = vtx0_y;
		max = vtx0_y;
		
		if (vtx1_y < min) {
			min = vtx1_y;
		}
		if (vtx1_y > max) {
			max = vtx1_y;
		}
		if (vtx2_y < min) {
			min = vtx2_y;
		}
		if (vtx2_y > max) {
			max = vtx2_y;
		}
		
		if ((min > size_y) || (max < -size_y)) {
			return(false);
		}
		
		min = vtx0_z;
		max = vtx0_z;
		
		if (vtx1_z < min) {
			min = vtx1_z;
		}
		if (vtx1_z > max) {
			max = vtx1_z;
		}
		if (vtx2_z < min) {
			min = vtx2_z;
		}
		if (vtx2_z > max) {
			max = vtx2_z;
		}
		
		if ((min > size_z) || (max < -size_z)) {
			return(false);
		}
		
		// Final test - if the box intersects the plane of the triangle.
		float n_x = edge0_y * edge1_z - edge0_z * edge1_y;
		float n_y = edge0_z * edge1_x - edge0_x * edge1_z;
		float n_z = edge0_x * edge1_y - edge0_y * edge1_x;
		
		float min_x, min_y, min_z;
		float max_x, max_y, max_z;
		
		if (n_x > 0) {
			min_x = -size_x - vtx0_x;
			max_x = size_x - vtx0_x;
		} else {
			min_x = size_x - vtx0_x;
			max_x = -size_x - vtx0_x;
		}
		
		if (n_y > 0) {
			min_y = -size_y - vtx0_y;
			max_y = size_y - vtx0_y;
		} else {
			min_y = size_y - vtx0_y;
			max_y = -size_y - vtx0_y;
		}
		
		if (n_z > 0) {
			min_z = -size_z - vtx0_z;
			max_z = size_z - vtx0_z;
		} else {
			min_z = size_z - vtx0_z;
			max_z = -size_z - vtx0_z;
		}
		
		float d = n_x * min_x + n_y * min_y + n_z * min_z;
		
		if (d > 0) {
			return(false);
		}
		
		d = n_x * max_x + n_y * max_y + n_z * max_z;
		
		if (d < 0) {
			return(false);
		}
		
		return(true);
	}
	
	/**
	 * Separating Axis test for the triangle versus the box.
	 */
	private boolean axisTest(
		float a,
		float b,
		float fa,
		float fb,
		float v0a,
		float v0b,
		float v1a,
		float v1b,
		float halfsizeA,
		float halfsizeB) {
		
		float p0 = a * v0a - b * v0b;
		float p1 = a * v1a - b * v1b;
		float min, max;
		
		if (p0 < p1) {
			min = p0;
			max = p1;
		} else {
			min = p1;
			max = p0;
		}
		
		// assumes size is the half size in each direction 
		float rad = fa * halfsizeA + fb * halfsizeB;
		
		boolean result = !((min > rad) || (max < -rad));
		return(result);
	}
}
