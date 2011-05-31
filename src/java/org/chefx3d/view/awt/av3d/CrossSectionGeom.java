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

/**
 * Class to filter and hold the cross section geometry for an extrusion.
 * By convention, the cross section is in the Y, Z plane. The
 * coordinates of the polygon are arranged in counter-clockwise
 * order. The figure is NOT closed (i.e. the first vertex is
 * not duplicated to the last).
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class CrossSectionGeom {
	
	/** The cross section coords */
	float[] coord;
	
	/** The number of vertices */
	int num_vertex;
	
	/** The length between the first (0-th) vertex and the n-th vertex
	 *  around the perimeter of the polygon. */
	float[] perimeter_length;
	
	/**
	 * Constructor
	 *
	 * @param coord The initial coord set
	 * @param offset The translation offset to be applied to the coord set
	 */
	CrossSectionGeom(float[] coord, float[] offset) {
		
		int num_coord = coord.length;
		num_vertex = num_coord / 3;
		
		// the first coordinate pair
		float y0 = coord[1];
		float z0 = coord[2];
		
		// the last coordinate pair
		int idx = (num_vertex - 1) * 3;
		float y1 = coord[idx + 1];
		float z1 = coord[idx + 2];
		
		if ((y0 == y1) && (z0 == z1)) {
			// do NOT duplicate the first coordinate to the last
			num_vertex--;
		}
		num_coord = num_vertex * 3;
		
		this.coord = new float[num_coord];
		idx = 0;
		for (int i = 0; i < num_vertex; i++) {
			idx = i * 3;
			this.coord[idx] = 0; // enforce the y/z plane convention
			this.coord[idx + 1] = coord[idx + 1] + offset[1];
			this.coord[idx + 2] = coord[idx + 2] + offset[2];
		}
		if (isClockwise()) {
			invertOrder();
		}
		initLength();
	}
	
	/**
     * Determine whether the ordering of the vertices of a polygon
     * is clockwise.
     * <p>
     * See: <a href="http://local.wasp.uwa.edu.au/~pbourke/geometry/clockwise/">
     * Determining whether ...</a>
     *
     * @return true if the ordering is clockwise, false if the ordering
     * is counter-clockwise.
     */
    private boolean isClockwise() {

        float y0;
        float z0;
		
        float y1 = 0;
        float z1 = 0;

        float sum = 0;
        int idx = 0;
        for (int i = 0; i < num_vertex - 1; i++) {

            y0 = coord[idx + 1];
            z0 = coord[idx + 2];
            idx += 3;
            y1 = coord[idx + 1];
            z1 = coord[idx + 2];

            sum += (z0 * y1 - z1 * y0);
        }
		y0 = y1;
		z0 = z1;
		// close the loop, last vertex to first
		y1 = coord[1];
		z1 = coord[2];
		
		sum += (z0 * y1 - z1 * y0);
		
        return((sum < 0));
    }
	
    /**
     * Invert the ordering of the vertices
     */
    private void invertOrder() {

        // swap the vertices end-to-end
		int start_idx = 0;
        int end_idx = (num_vertex - 1) * 3;
        int num_swap = num_vertex / 2;
        for (int i = 0; i < num_swap; i++) {
			
            float x = coord[end_idx];
            float y = coord[end_idx + 1];
            float z = coord[end_idx + 2];
			
            coord[end_idx] = coord[start_idx];
            coord[end_idx + 1] = coord[start_idx + 1];
            coord[end_idx + 2] = coord[start_idx + 2];
			
            coord[start_idx] = x;
            coord[start_idx + 1] = y;
            coord[start_idx + 2] = z;
			
            start_idx += 3;
            end_idx -= 3;
        }
    }
	
	/** 
	 * Initialize the perimeter length array
	 */
	private void initLength() {
		
		perimeter_length = new float[num_vertex + 1];
		
        float y0;
        float z0;
		
        float y1 = 0;
        float z1 = 0;

		float delta_y;
		float delta_z;
		
		float sum = 0;
		int i = 0;
        int idx = 0;
        for (; i < num_vertex - 1; i++) {

            y0 = coord[idx + 1];
            z0 = coord[idx + 2];
            idx += 3;
            y1 = coord[idx + 1];
            z1 = coord[idx + 2];

			delta_y = y1 - y0;
			delta_z = z1 - z0;
			
			sum += (float)Math.sqrt(delta_y * delta_y + delta_z * delta_z);
            perimeter_length[i + 1] = sum;
        }
		y0 = y1;
		z0 = z1;
		// last vertex to first
		y1 = coord[1];
		z1 = coord[2];
		
		delta_y = y1 - y0;
		delta_z = z1 - z0;
		
		sum += (float)Math.sqrt(delta_y * delta_y + delta_z * delta_z);
        perimeter_length[i + 1] = sum;
	}
}
