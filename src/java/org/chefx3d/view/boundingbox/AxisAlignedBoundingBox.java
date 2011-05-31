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
 * An axis-aligned bounding box.
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public class AxisAlignedBoundingBox {
	
	/** Tolerance value for the intersection test */
	protected static float epsilon;
	
    /** The minimum vertex */
    protected final Point3f min_vtx;
    
    /** The maximum vertex */
    protected final Point3f max_vtx;
    
    /** The minimum extent */
    protected final Point3f min_ext;
    
    /** The maximum extent */
    protected final Point3f max_ext;
    
    /** The scale */
    protected final Vector3f scale;
    
    /** The center of the box based on the min/max values */
    protected final Point3f center;
    
	/** Border increment to the extents */
	protected final Vector3f border;
	
    /** The dimensions of the box */
    protected final float[] size;
    
    /** The vertices of the box */
    protected float[] vert;
	
	/* Scratch point object for transforming vertices */
    protected Point3f vtx;
	
    /**
     * Default constructor
     */
    public AxisAlignedBoundingBox() {
		
        min_vtx = new Point3f();
        max_vtx = new Point3f();
		
		scale = new Vector3f(1, 1, 1);
        
        min_ext = new Point3f();
        max_ext = new Point3f();
        
        center = new Point3f();
		border = new Vector3f();
        size = new float[3];
        
        vtx = new Point3f();
        vert = new float[24];
    }
    
    /**
     * Construct a bounding box
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
	 * @param scl The scale to apply to the vertices 
     */
    public AxisAlignedBoundingBox(Point3f min, Point3f max, Vector3f scl) {
		
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
    public AxisAlignedBoundingBox(float[] min, float[] max, float[] scl) {
		
        this();
        
        min_vtx.set(min);
        max_vtx.set(max);
		scale.set(scl);
		
        update();
    }
    
    /**
     * Get the extents of the bounds.
     *
     * @param min The minimum extent of the bounds
     * @param max The maximum extent of the bounds
     */
    public void getExtents(Point3f min, Point3f max) {
		
        min.set(min_ext);
        max.set(max_ext);
    }
    
    /**
     * Get the extents of the bounds.
     *
     * @param min The minimum extent of the bounds
     * @param max The maximum extent of the bounds
     */
    public void getExtents(float[] min, float[] max) {
		
        min[0] = min_ext.x;
        min[1] = min_ext.y;
        min[2] = min_ext.z;
        
        max[0] = max_ext.x;
        max[1] = max_ext.y;
        max[2] = max_ext.z;
    }
    
    /**
     * Get the vertices of the box.
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
     */
    public void getVertices(Point3f min, Point3f max) {
		
        min.set(min_vtx);
        max.set(max_vtx);
    }
    
    /**
     * Get the vertices of the box.
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
     */
    public void getVertices(float[] min, float[] max) {
		
        min[0] = min_vtx.x;
        min[1] = min_vtx.y;
        min[2] = min_vtx.z;
        
        max[0] = max_vtx.x;
        max[1] = max_vtx.y;
        max[2] = max_vtx.z;
    }
    
    /**
     * Get the scale.
     *
	 * @param scl The scale applied to the vertices
     */
    public void getScale(Vector3f scl) {
		
        scl.set(scale);
    }
    
    /**
     * Get the scale.
     *
	 * @param scl The scale applied to the vertices
     */
    public void getScale(float[] scl) {
		
        scale.get(scl);
    }
    
    /**
     * Get the center of the bounds.
     *
     * @param center The center of the bounds
     */
    public void getCenter(Point3f center) {
		
        center.set(this.center);
    }
    
    /**
     * Get the center of the bounds.
     *
     * @param center The center of the bounds
     */
    public void getCenter(float[] center) {
		
        center[0] = this.center.x;
        center[1] = this.center.y;
        center[2] = this.center.z;
    }
    
    /**
     * Get the bounds border dimensions
     *
     * @param border The bounds border dimensions
     */
    public void getBorder(Vector3f border) {
		
        border.set(this.border);
    }
    
    /**
     * Get the bounds border dimensions
     *
     * @param border The bounds border dimensions
     */
    public void getBorder(float[] border) {
		
        border[0] = this.border.x;
        border[1] = this.border.y;
        border[2] = this.border.z;
    }
    
    /**
     * Get the size of the bounds.
     *
     * @param size The size of the bounds
     */
    public void getSize(float[] size) {
		
        size[0] = this.size[0];
        size[1] = this.size[1];
        size[2] = this.size[2];
    }
    
    /**
     * Check for the given AxisAlignedBoundingBox intersecting this.
     *
     * @param that An AxisAlignedBoundingBox to check against
     * @return true if that intersects this, false otherwise
     */
    public boolean intersect(AxisAlignedBoundingBox that) {
        return(intersect(that, false));
    }
    
    /**
     * Check for the given AxisAlignedBoundingBox intersecting this.
     *
     * @param that An AxisAlignedBoundingBox to check against
	 * @param useEpsilon Flag indicating that the epsilon tolerance 
	 * value should be used in the intersection check
     * @return true if that intersects this, false otherwise
     */
    public boolean intersect(AxisAlignedBoundingBox that, boolean useEpsilon) {
		if (useEpsilon) {
			return(
				that.min_ext.x <= (max_ext.x + epsilon) && 
				that.min_ext.y <= (max_ext.y + epsilon) &&
				that.min_ext.z <= (max_ext.z + epsilon) && 
				(that.max_ext.x + epsilon) >= min_ext.x &&
				(that.max_ext.y + epsilon) >= min_ext.y && 
				(that.max_ext.z + epsilon) >= min_ext.z);
		} else {
			// a.min <= b.max && a.max >= b.min
			return(
				that.min_ext.x <= max_ext.x && 
				that.min_ext.y <= max_ext.y &&
				that.min_ext.z <= max_ext.z && 
				that.max_ext.x >= min_ext.x &&
				that.max_ext.y >= min_ext.y && 
				that.max_ext.z >= min_ext.z);
		}
    }
    
    /**
     * Transform the bounds by the argument matrix.
     *
     * @param mat The matrix to transform this bounds by
     */
    public void transform(Matrix4f mat) {
		
		min_ext.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		max_ext.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		
		for (int i = 0; i < vert.length; i += 3) {
			vtx.set(vert[i], vert[i+1], vert[i+2]);
        	mat.transform(vtx);
        	if (vtx.x < min_ext.x) {
				min_ext.x = vtx.x;
			}
        	if (vtx.y < min_ext.y) {
				min_ext.y = vtx.y;
			}
        	if (vtx.z < min_ext.z) {
				min_ext.z = vtx.z;
			}
        	if (vtx.x > max_ext.x) {
				max_ext.x = vtx.x;
			}
        	if (vtx.y > max_ext.y) {
				max_ext.y = vtx.y;
			}
        	if (vtx.z > max_ext.z) {
				max_ext.z = vtx.z;
			}
		}
		
        center.x = (max_ext.x + min_ext.x) * 0.5f;
        center.y = (max_ext.y + min_ext.y) * 0.5f;
        center.z = (max_ext.z + min_ext.z) * 0.5f;
		
        size[0] = (max_ext.x - min_ext.x) * 0.5f;
        size[1] = (max_ext.y - min_ext.y) * 0.5f;
        size[2] = (max_ext.z - min_ext.z) * 0.5f;
    }
    
    /**
     * Set the scale.
     *
	 * @param scl The scale to apply to the vertices
     */
    public void setScale(Vector3f scl) {
		
		if ((scale.x != scl.x) || (scale.y != scl.y) || (scale.z != scl.z)) {
        	scale.set(scl);
        	update();
		}
    }
    
    /**
     * Set the scale.
     *
	 * @param scl The scale to apply to the vertices
     */
    public void setScale(float[] scl) {
		
		if ((scale.x != scl[0]) || (scale.y != scl[1]) || (scale.z != scl[2])) {
        	scale.set(scl);
        	update();
		}
    }
    
	/** 
	 * Set the bounds border dimensions
	 *
	 * @param brdr The bounds border dimensions
	 */
	public void setBorder(Vector3f brdr) {
		
		if ((border.x != brdr.x) || (border.y != brdr.y) || (border.z != brdr.z)) {
        	border.set(brdr);
        	update();
		}
	}
	
	/** 
	 * Set the bounds border dimensions
	 *
	 * @param brdr The bounds border dimensions
	 */
	public void setBorder(float[] brdr) {
		
		if ((border.x != brdr[0]) || (border.y != brdr[1]) || (border.z != brdr[2])) {
        	border.set(brdr);
        	update();
		}
	}
	
    /**
     * Set the vertices of the box.
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
     */
    public void setVertices(Point3f min, Point3f max) {
		
        min_vtx.set(min);
        max_vtx.set(max);
        
        update();
    }
    
    /**
     * Set the vertices of the box.
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
     */
    public void setVertices(float[] min, float[] max) {
		
        min_vtx.x = min[0];
        min_vtx.y = min[1];
        min_vtx.z = min[2];
        
        max_vtx.x = max[0];
        max_vtx.y = max[1];
        max_vtx.z = max[2];
        
        update();
    }
    
    /**
     * Set the vertices and scale of the box.
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
	 * @param scl The scale to apply to the vertices
     */
    public void set(Point3f min, Point3f max, Vector3f scl) {
		
        min_vtx.set(min);
        max_vtx.set(max);
        scale.set(scl);
        
        update();
    }
    
    /**
     * Set the vertices and scale of the box.
     *
     * @param min The minimum vertex of the box
     * @param max The maximum vertex of the box
	 * @param scl The scale to apply to the vertices
     */
    public void set(float[] min, float[] max, float[] scl) {
		
        min_vtx.x = min[0];
        min_vtx.y = min[1];
        min_vtx.z = min[2];
        
        max_vtx.x = max[0];
        max_vtx.y = max[1];
        max_vtx.z = max[2];
        
        scale.set(scl);
		
        update();
    }
    
    /**
     * Generate a string representation of this box.
     *
     * @return A string representing the bounds information
     */
    public String toString() {
        return(
			"AxisAlignedBoundingBox: "+
			"min("+ min_ext.x +' '+ min_ext.y +' '+ min_ext.z +") "+
			"max("+ max_ext.x +' '+ max_ext.y +' '+ max_ext.z +") ");
    }
    
	/**
	 * Copy the contents of that into this
	 *
	 * @param that The object to initialize from
	 */
	public void copy(AxisAlignedBoundingBox that) {
		this.min_vtx.set(that.min_vtx);
    	this.max_vtx.set(that.max_vtx);
    	this.min_ext.set(that.min_ext);
    	this.max_ext.set(that.max_ext);
    	this.scale.set(that.scale);
    	this.center.set(that.center);
    	this.border.set(that.border);
    	this.size[0] = that.size[0];
		this.size[1] = that.size[1];
		this.size[2] = that.size[2];
		System.arraycopy(that.vert, 0, this.vert, 0, 24);
	}
	
	/** 
	 * Set the tolerance value to use for intersection checks
	 *
	 * @param epsilon_value The tolerance value
	 */
	public static void setEpsilon(float epsilon_value) {
		epsilon = epsilon_value;
	}
	
    /**
     * Update the extents, center and vertices of the box based on the
     * current min and max positions.
     */
	protected void update() {
		
		// ensure that the vertices are properly ordered
		float tmp;
		if (max_vtx.x < min_vtx.x) {
			tmp = min_vtx.x;
			min_vtx.x = max_vtx.x;
			max_vtx.x = tmp;
		}
		if (max_vtx.y < min_vtx.y) {
			tmp = min_vtx.y;
			min_vtx.y = max_vtx.y;
			max_vtx.y = tmp;
		}
		if (max_vtx.z < min_vtx.z) {
			tmp = min_vtx.z;
			min_vtx.z = max_vtx.z;
			max_vtx.z = tmp;
		}
		
		// calculate values for accessors
		min_ext.x = min_vtx.x * scale.x - border.x;
		min_ext.y = min_vtx.y * scale.y - border.y;
		min_ext.z = min_vtx.z * scale.z - border.z;
		
		max_ext.x = max_vtx.x * scale.x + border.x;
		max_ext.y = max_vtx.y * scale.y + border.y;
		max_ext.z = max_vtx.z * scale.z + border.z;
		
        center.x = (max_ext.x + min_ext.x) * 0.5f;
        center.y = (max_ext.y + min_ext.y) * 0.5f;
        center.z = (max_ext.z + min_ext.z) * 0.5f;
		
        size[0] = (max_ext.x - min_ext.x) * 0.5f;
        size[1] = (max_ext.y - min_ext.y) * 0.5f;
        size[2] = (max_ext.z - min_ext.z) * 0.5f;
        
		// sidepocket the vertices for transformations
		int i = 0;
        vert[i++] = max_ext.x; vert[i++] = max_ext.y; vert[i++] = max_ext.z;
        vert[i++] = max_ext.x; vert[i++] = max_ext.y; vert[i++] = min_ext.z;
        vert[i++] = max_ext.x; vert[i++] = min_ext.y; vert[i++] = max_ext.z;
        vert[i++] = max_ext.x; vert[i++] = min_ext.y; vert[i++] = min_ext.z;
        vert[i++] = min_ext.x; vert[i++] = max_ext.y; vert[i++] = max_ext.z;
        vert[i++] = min_ext.x; vert[i++] = max_ext.y; vert[i++] = min_ext.z;
        vert[i++] = min_ext.x; vert[i++] = min_ext.y; vert[i++] = max_ext.z;
        vert[i++] = min_ext.x; vert[i++] = min_ext.y; vert[i++] = min_ext.z;
    }
}
