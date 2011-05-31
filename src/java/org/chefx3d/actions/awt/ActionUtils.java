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

package org.chefx3d.actions.awt;

// External imports
import java.util.ArrayList;
import java.util.List;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;

/**
 * Utilities supporting cut, copy and paste actions.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
public abstract class ActionUtils {

	/**
	 * Aggregate the children of the specified entity 
	 * into the argument list
	 *
	 * @param entity The Entity whose children to gather
	 * @param list The List to place them in
	 */
	public static void getChildren(Entity entity, List<Entity> list) {
		if (entity.hasChildren()) {
			ArrayList<Entity> children = entity.getChildren();
			list.addAll(children);
			for (int i = 0; i < children.size(); i++) {
				Entity child = children.get(i);
				getChildren(child, list);
			}
		}
	}
	
	/**
	 * Clone a set of segments, handle shared vertices
	 *
	 * @param segments The segments to clone
     * @param model The world model
	 * @return The cloned array of segments
	 */
	public static SegmentEntity[] cloneSegments(
		SegmentEntity[] segments, 
		WorldModel model) {
		
		int num_seg = segments.length;
		SegmentEntity[] results = new SegmentEntity[segments.length];
		ArrayList<VertexEntity> vtx = new ArrayList<VertexEntity>(num_seg * 2);
		
		SegmentEntity se = segments[0];
		vtx.add(se.getStartVertexEntity());
		vtx.add(se.getEndVertexEntity());
		results[0] = se.clone(model);
		
		int list_idx = 0;
		for (int i = 1; i < num_seg; i++) {
			
			se = segments[i];
			SegmentEntity result = se.clone(model);
			results[i] = result;
			
			VertexEntity start_ve = se.getStartVertexEntity();
			list_idx = vtx.indexOf(start_ve);
			if (list_idx == -1) {
				vtx.add(start_ve);
			} else {
				vtx.add(null);
				int seg_idx = list_idx / 2;
				boolean start = ((list_idx % 2) == 0);
				SegmentEntity source = results[seg_idx];
				if (start) {
					result.setStartVertex(source.getStartVertexEntity());
				} else {
					result.setStartVertex(source.getEndVertexEntity());
				}
			}
			VertexEntity end_ve = se.getEndVertexEntity();
			list_idx = vtx.indexOf(end_ve);
			if (list_idx == -1) {
				vtx.add(end_ve);
			} else {
				vtx.add(null);
				int seg_idx = list_idx / 2;
				boolean start = ((list_idx % 2) == 0);
				SegmentEntity source = results[seg_idx];
				if (start) {
					result.setEndVertex(source.getStartVertexEntity());
				} else {
					result.setEndVertex(source.getEndVertexEntity());
				}
			}
		}
		return(results);
	}
}
