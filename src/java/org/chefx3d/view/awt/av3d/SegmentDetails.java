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
import org.j3d.aviatrix3d.TransformGroup;

// Local imports
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;

/**
 * Defines the requirments for providing segment rendering 
 * information that is determined by the properties and
 * placement of other segments.
 *
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public interface SegmentDetails {

	/**
	 * Return the miter angles of the segment ends in an 
	 * array double[left, right]
	 *
	 * @param segment The segment entity
	 * @param result The array to initialize with the results, or null
	 * and a new array will be allocated.
	 * @return the array of miter angles
	 */
	public double[] getSegmentMiter(SegmentEntity segment, double[] result);
	
	/**
	 * Return the vertex in local coordinates
	 *
	 * @param ve The VertexEntity
	 * @param local The array to initialize with the vertex coordinates
	 * @return The array containing the local vertex coordinates
	 */
	public float[] toLocal(VertexEntity ve, float[] local);
	
	/**
	 * Return the parent TransformGroup
	 *
	 * @return The parent TransformGroup
	 */
	public TransformGroup getParent();
}
