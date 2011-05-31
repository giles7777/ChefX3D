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

// External Imports
// None

// Local Imports
import org.chefx3d.model.SegmentEntity;

/**
 * Defines the requirements for receiving status on visible scene changes
 * due to segments being modified.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public interface SegmentListener {
	
	/**
	 * Segment has been added to the scene
	 *
	 * @param se The segment entity
	 */
	public void segmentAdded(SegmentEntity se);

	/**
	 * Segment has been removed from the scene
	 *
	 * @param se The segment entity
	 */
	public void segmentRemoved(SegmentEntity se);
	
	/**
	 * Segment has been modified
	 *
	 * @param se The segment entity
	 */
	public void segmentChanged(SegmentEntity se);
}
