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
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorConstants;
import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to anchor manipulations of a Segment
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.9 $
 */
public class SegmentEntityScaleTransientResponse implements
    TrackerEventResponse, EditorConstants {

	/** Minimum wall thickness */
	private static final float MINIMUM_WALL_THICKNESS = 0.05f;
	
	/** Base parameters */
    private WorldModel model;
    private CommandController controller;
    private ErrorReporter reporter;
    private AV3DAnchorInformation anchorNode;
    private ViewEnvironment viewEnv;

    /** The initial conditions for this action */
    private ActionData actionData;
	private AnchorData anchorData;

    /** scratch vecmath objects */
    private Vector3f right;
    private Vector3f up;
    private Vector3f vec;
	
    private AxisAngle4f aa;
    private Matrix3f mtx;
    private Vector3f v;
	
	/** scratch data arrays */
    private double[] frustum;
	private float[] rot_array;
	private double[] newEntityPosition;
	private double[] originalEntityPosition;
	private double[] fixed_position;
	
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
    public SegmentEntityScaleTransientResponse(
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

        right = new Vector3f();
        up = new Vector3f();
        vec = new Vector3f();
		
        frustum = new double[6];
		rot_array = new float[4];

		newEntityPosition = new double[3];
		originalEntityPosition = new double[3];
		fixed_position = new double[3];
		
        aa = new AxisAngle4f();
        mtx = new Matrix3f();
        v = new Vector3f();
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
       
        SegmentEntity segment = ((SegmentEntity)entities[0]);
		int entityID = segment.getEntityID();
		SegmentEntityWrapper wrapper = 
			(SegmentEntityWrapper)actionData.wrapperMap.get(entityID);
		wrapper.getRotation(rot_array);
		
		////////////////////////////////////////////////
		// rem: TODO - make this a general case xform
		float ry = rot_array[1];
		rot_array[1] = -rot_array[2];
		rot_array[2] = ry;
		rot_array[3] = rot_array[3];
		////////////////////////////////////////////////
		
		float[] newMouseDevicePosition = trackerState.devicePos;
		
		viewEnv.getViewFrustum(frustum);
		float f_height = (float)(frustum[3] - frustum[2]);
		float f_width = (float)(frustum[1] - frustum[0]);
		
		float deltaUp = (actionData.mouseDevicePosition[1] - newMouseDevicePosition[1]) * f_height;
		float deltaRt = (newMouseDevicePosition[0] - actionData.mouseDevicePosition[0]) * f_width;
		
		aa.set(rot_array);
		aa.angle = -aa.angle;
		
		mtx.setIdentity();
		mtx.set(aa);
		
		v.set(deltaRt, deltaUp, 0);
		mtx.transform(v);
		
		deltaUp = v.y;
		deltaRt = v.x;
		
		aa.set(rot_array);
		mtx.setIdentity();
		mtx.set(aa);
		
		if ((anchorData == AnchorData.EAST) || (anchorData == AnchorData.WEST)) {
			if (deltaRt != 0) {
				
				VertexEntity moving_vertex;
				VertexEntity fixed_vertex;
				if (anchorData == AnchorData.EAST) {
					moving_vertex = segment.getEndVertexEntity();
					fixed_vertex = segment.getStartVertexEntity();
				} else {
					moving_vertex = segment.getStartVertexEntity();
					fixed_vertex = segment.getEndVertexEntity();
				}
				
				moving_vertex.getStartingPosition(originalEntityPosition);
				
				// determine the new position
				vec.set(
					(float)originalEntityPosition[0],
					(float)originalEntityPosition[1],
					(float)originalEntityPosition[2]);
				
				// the position change along the horizontal vector
				right.set(actionData.zoneOri.right);
				mtx.transform(right);
				right.scale(deltaRt);
				
				vec.add(right);
				
				newEntityPosition[0] = vec.x;
				newEntityPosition[1] = vec.y;
				newEntityPosition[2] = vec.z;
				
				fixed_vertex.getStartingPosition(fixed_position);
				editorGrid.alignVectorToGridSpacing(newEntityPosition, fixed_position);
				
				int transactionID = model.issueTransactionID();
				MoveVertexTransientCommand cmd = new MoveVertexTransientCommand(
					model,
					transactionID,
					moving_vertex,
					newEntityPosition,
					new float[3]);
				cmd.setErrorReporter(reporter);
				
				controller.execute(cmd);
			}
		} else if ((anchorData == AnchorData.NORTH) || (anchorData == AnchorData.SOUTH)) {
			if (deltaUp != 0) {
				
				float thickness;
				if (anchorData == AnchorData.SOUTH) {
					thickness = actionData.thickness - deltaUp;
				} else {
					thickness = actionData.thickness + deltaUp;
				}
				
				if (thickness < MINIMUM_WALL_THICKNESS) {
					return;
				}
				
System.out.println("SegmentEntity.WALL_THICKNESS_PROP is a float now");	

				Command cmd = new ChangePropertyTransientCommand(
					segment,
					Entity.EDITABLE_PROPERTIES,
					SegmentEntity.WALL_THICKNESS_PROP,
					thickness,
					model);
				cmd.setErrorReporter(reporter);
				
				if (anchorData == AnchorData.NORTH) {
					// drag on the north anchor only requires a change
					// to the thickness property
					controller.execute(cmd);
					
				} else {
					// drag on the south anchor requires a change
					// to the thickness property and both vertices
					// must move
					ArrayList<Command> cmdList = new ArrayList<Command>();
					cmdList.add(cmd);
					
					double newEntityPosition[] = new double[3];
					double originalEntityPosition[] = new double[3];
					
					// the position change along the vertical vector
					up.set(actionData.zoneOri.up);
					mtx.transform(up);
					up.scale(deltaUp);
					
					// process the left vertex
					VertexEntity left_ve = segment.getStartVertexEntity();
					left_ve.getStartingPosition(originalEntityPosition);
					
					// determine the new position
					vec.set(
						(float)originalEntityPosition[0],
						(float)originalEntityPosition[1],
						(float)originalEntityPosition[2]);
						
					vec.add(up);
					
					newEntityPosition[0] = vec.x;
					newEntityPosition[1] = vec.y;
					newEntityPosition[2] = vec.z;
					
					int transactionID = model.issueTransactionID();
					cmd = new MoveVertexTransientCommand(
						model,
						transactionID,
						left_ve,
						newEntityPosition,
						new float[3]);
					cmd.setErrorReporter(reporter);
					cmdList.add(cmd);
					
					// process the right vertex
					VertexEntity rght_ve = segment.getEndVertexEntity();
					rght_ve.getStartingPosition(originalEntityPosition);
					
					// determine the new position
					vec.set(
						(float)originalEntityPosition[0],
						(float)originalEntityPosition[1],
						(float)originalEntityPosition[2]);
						
					vec.add(up);
					
					newEntityPosition[0] = vec.x;
					newEntityPosition[1] = vec.y;
					newEntityPosition[2] = vec.z;
					
					transactionID = model.issueTransactionID();
					cmd = new MoveVertexTransientCommand(
						model,
						transactionID,
						rght_ve,
						newEntityPosition,
						new float[3]);
					cmd.setErrorReporter(reporter);
					cmdList.add(cmd);
					
					MultiTransientCommand multi =
						new MultiTransientCommand(cmdList, "Change Wall Thickness");
					controller.execute(multi);
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
    
    /**
     * Set the anchor associated with this response
     *
     * @param anchorPosition The anchor associated with this response
     */
    void setAnchor(AV3DAnchorInformation anchorPosition){
        anchorNode = anchorPosition;
		anchorData = anchorNode.getAnchorDataFlag();
    }
}
