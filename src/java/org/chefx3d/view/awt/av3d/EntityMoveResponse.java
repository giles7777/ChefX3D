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

//External imports
import java.util.ArrayList;

import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.MoveSegmentCommand;
import org.chefx3d.model.MoveVertexCommand;
import org.chefx3d.model.MoveVertexTransientCommand;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to Entity move events. This is only for non-transient move
 * events. The appropriate command is issued for the event.
 *
 * @author Ben Yarger
 * @version $Revision: 1.24 $
 *
 */
public class EntityMoveResponse implements TrackerEventResponse {

    private WorldModel model;
    private ErrorReporter reporter;
    private CommandController controller;
    private ViewEnvironment viewEnv;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** Scratch vecmath objects */
    private Vector3f up;
    private Vector3f right;
    private Vector3f vec;
	
    /** Scratch arrays */
    private double[] frustum;
    private double[] newEntityPosition;
    private double[] originalEntityPosition;
    private double[] newVertexPosition;
    private double[] originalVertexPosition;
    private float[] velocity;

	/** Utility for aligning the model with the editor grid */
	private EditorGrid editorGrid;
	
    /**
     * Constructor
	 *
     * @param model
     * @param controller
     * @param reporter
     * @param viewEnv
	 * @param editorGrid
     */
    public EntityMoveResponse(
        WorldModel model,
        CommandController controller,
        ErrorReporter reporter,
        ViewEnvironment viewEnv,
		EditorGrid editorGrid) {

        this.model = model;
        this.reporter = reporter;
        this.controller = controller;
        this.viewEnv = viewEnv;
		this.editorGrid = editorGrid;

        up = new Vector3f();
        right = new Vector3f();
        vec = new Vector3f();
        frustum = new double[6];
		
        newEntityPosition = new double[3];
        originalEntityPosition = new double[3];
        newVertexPosition = new double[3];
        originalVertexPosition = new double[3];
		velocity = new float[3];
    }

    //---------------------------------------------------------------
    // Methods defined by TrackerEventResponse
    //---------------------------------------------------------------

    /**
     * Begins the processing required to generate a command in response
     * to the input received.
     *
     * @param trackerID The id of the tracker calling the original handler
     * @param trackerState The event that started this whole thing off
     * @param entities The array of entities to handle
     * @param tool The tool that is used in the action (can be null)
     */
	public void doEventResponse(
		int trackerID,
		TrackerState trackerState,
		Entity[] entities,
		Tool tool) {

		float[] newMouseDevicePosition = trackerState.devicePos;
		
        float deltaRt = (newMouseDevicePosition[0] - actionData.mouseDevicePosition[0]);
        float deltaUp = (actionData.mouseDevicePosition[1] - newMouseDevicePosition[1]);

        // get the list of entity data, assume same order as selection list
        ArrayList<ActionData.EntityData> entityDataList = actionData.entityList;                
 
        //
        // if the mouse hasn't moved, there is no need to perform a move response
        //
        if ((deltaRt != 0) || (deltaUp != 0)) {
			
			viewEnv.getViewFrustum(frustum);
			
			float f_width = (float)(frustum[1] - frustum[0]);
			float f_height = (float)(frustum[3] - frustum[2]);

        	deltaRt *= f_width;
        	deltaUp *= f_height;

			// Issue a command for each individual entity
			for (int i = 0; i < entities.length; i++) {
				
				Entity entity = entities[i];
				if (entity != null) {
					
					Command cmd = null;
					if (entity instanceof VertexEntity) {
						VertexEntity vertex = (VertexEntity)entity;
						vertex.getStartingPosition(originalEntityPosition);
						
						newEntityPosition[0] = originalEntityPosition[0] + deltaRt;
						newEntityPosition[1] = originalEntityPosition[1] + deltaUp;
						newEntityPosition[2] = originalEntityPosition[2];
						
						SegmentableEntity multi = 
							(SegmentableEntity)model.getEntity(vertex.getParentEntityID());
						ArrayList<SegmentEntity> segment_list = multi.getSegments(vertex);
						if ((segment_list != null) && (segment_list.size() == 1)) {
							// adjust for the grid spacing if the vertex is used by
							// precisely 1 segment, otherwise, not sure what to do
							SegmentEntity se = segment_list.get(0);
							VertexEntity fixed_vertex;
							if (vertex == se.getStartVertexEntity()) {
								fixed_vertex = se.getEndVertexEntity();
							} else {
								fixed_vertex = se.getStartVertexEntity();
							}
							double[] fixed_position = new double[3];
							fixed_vertex.getPosition(fixed_position);
							editorGrid.alignVectorToGridSpacing(newEntityPosition, fixed_position);
						}
						cmd = new MoveVertexCommand(
							model,
							model.issueTransactionID(),
							vertex,
							newEntityPosition,
							originalEntityPosition);
						
					} else if (entity instanceof SegmentEntity) {
						
						SegmentEntity segment = (SegmentEntity)entity;
						VertexEntity left_ve = segment.getStartVertexEntity();
						VertexEntity rght_ve = segment.getEndVertexEntity();
						
						left_ve.getStartingPosition(originalEntityPosition);
						
						newEntityPosition[0] = originalEntityPosition[0] + deltaRt;
						newEntityPosition[1] = originalEntityPosition[1] + deltaUp;
						newEntityPosition[2] = originalEntityPosition[2];
						
						rght_ve.getStartingPosition(originalVertexPosition);
						
						newVertexPosition[0] = originalVertexPosition[0] + deltaRt;
						newVertexPosition[1] = originalVertexPosition[1] + deltaUp;
						newVertexPosition[2] = originalVertexPosition[2];
						
						cmd = new MoveSegmentCommand(
							model,
							model.issueTransactionID(),
							segment,
							newEntityPosition,
							originalEntityPosition,
							newVertexPosition,
							originalVertexPosition);
						
					} else if (entity instanceof PositionableEntity) {
						
						((PositionableEntity)entity).getStartingPosition(originalEntityPosition);
						
						// the position change along the vertical vector
						up.set(actionData.zoneOri.up);
						up.scale(deltaUp);
						
						// the position change along the horizontal vector
						right.set(actionData.zoneOri.right);
						right.scale(deltaRt);
						
						vec.set(
							(float)originalEntityPosition[0],
							(float)originalEntityPosition[1],
							(float)originalEntityPosition[2]);
						
						vec.add(up);
						vec.add(right);
						
						newEntityPosition[0] = vec.x;
						newEntityPosition[1] = vec.y;
						newEntityPosition[2] = vec.z;
						
		                // get the original children list
		                ArrayList<Entity> startChildren = new ArrayList<Entity>();
		                if (entityDataList != null && entityDataList.size() - 1 >= i) {
		                    startChildren = actionData.entityList.get(i).startChildren;                
		                }

						editorGrid.alignPositionToGrid(newEntityPosition);
		
						cmd = new MoveEntityCommand(
							model,
							model.issueTransactionID(),
							(PositionableEntity)entity,
							newEntityPosition,
							originalEntityPosition, 
							startChildren);
					}
					
					if (cmd != null) {
						cmd.setErrorReporter(reporter);
						controller.execute(cmd);
					}
				}
			}
		}
    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Initialize in preparation for a response
     *
     * @param ad The initial device position of the mouse
     */
    void setActionData(ActionData ad) {
        actionData = ad;
    }
}

