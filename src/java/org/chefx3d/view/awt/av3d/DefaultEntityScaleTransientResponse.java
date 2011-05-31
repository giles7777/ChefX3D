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

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.ChangePropertyTransientCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.MultiTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.ChangePropertyCommand;
import org.chefx3d.model.ScaleEntityTransientCommand;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorConstants;
import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to transient scale events.
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.6 $
 *
 */
public class DefaultEntityScaleTransientResponse implements
    TrackerEventResponse, EditorConstants {

    private WorldModel model;
    private CommandController controller;
    private ErrorReporter reporter;
    private AV3DAnchorInformation anchorNode;
    private ViewEnvironment viewEnv;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** scratch vectors */
    private Vector3f right;
    private Vector3f up;
    private Vector3f vec;
    private double[] frustum;

    private AxisAngle4f aa;
    private Matrix3f mtx;
    private Vector3f v;

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
    public DefaultEntityScaleTransientResponse(
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

        float[] newMouseDevicePosition = trackerState.devicePos;

        viewEnv.getViewFrustum(frustum);
        float f_height = (float)(frustum[3] - frustum[2]);
        float f_width = (float)(frustum[1] - frustum[0]);

        // the change in mouse position, in world scale, along the vertical viewing axis
        float deltaUp = (actionData.mouseDevicePosition[1] - newMouseDevicePosition[1]) * f_height;
        float deltaRt = (newMouseDevicePosition[0] - actionData.mouseDevicePosition[0]) * f_width;

        double newEntityPosition[] = new double[3];
        double originalEntityPosition[] = new double[3];

		float[] originalEntityRotation = new float[4];

        float[] newEntityScale = new float[3];
        float[] originalEntityScale = new float[3];

        float[] originalEntitySize = new float[3];

        // Issue a command for each individual entity
        for(int i = 0; i < entities.length; i++) {

            // get the current parameters of the entity
            PositionableEntity entity = (PositionableEntity)entities[i];

            if (entity == null) {
                return;
            }
            
            entity.getStartingPosition(originalEntityPosition);
			entity.getStartingRotation(originalEntityRotation);
            entity.getStartingScale(originalEntityScale);
            entity.getSize(originalEntitySize);

            //////////////////////////////////////////////////////////
			// configure the drag amount relative to the
			// orientation of the object
			aa.set(originalEntityRotation);
			aa.angle = -aa.angle;
			
			mtx.setIdentity();
			mtx.set(aa);
			
			v.set(deltaRt, deltaUp, 0);
			mtx.transform(v);
			
			deltaUp = v.y;
			deltaRt = v.x;
			
			aa.set(originalEntityRotation);
			mtx.setIdentity();
			mtx.set(aa);
			//////////////////////////////////////////////////////////
			
            newEntityScale[0] = originalEntityScale[0];
            newEntityScale[1] = originalEntityScale[1];
            newEntityScale[2] = originalEntityScale[2];

            AnchorData anchor = anchorNode.getAnchorDataFlag();
			boolean north = 
				(anchor == AnchorData.NORTH) | 
				(anchor == AnchorData.NORTHEAST) | 
				(anchor == AnchorData.NORTHWEST);
			boolean south = 
				(anchor == AnchorData.SOUTH) | 
				(anchor == AnchorData.SOUTHEAST) | 
				(anchor == AnchorData.SOUTHWEST);
			boolean east = 
				(anchor == AnchorData.EAST) | 
				(anchor == AnchorData.NORTHEAST) | 
				(anchor == AnchorData.SOUTHEAST);
			boolean west = 
				(anchor == AnchorData.WEST) | 
				(anchor == AnchorData.NORTHWEST) | 
				(anchor == AnchorData.SOUTHWEST);
			
            // adjust the current size in the horizontal dimension by the
            // amount of drag, depending on which anchor is selected
            int horizontal_anchor_direction = 0;
            float x_size_start = originalEntitySize[0] * originalEntityScale[0];
            float x_size = x_size_start;

            if (east) {
                x_size += deltaRt;
                horizontal_anchor_direction = -1;
			} else if (west) {
                x_size -= deltaRt;
                horizontal_anchor_direction = 1;
            }
			if (x_size < 0) {
				x_size = 0;
			}
            float horizontal_delta = x_size_start - x_size;

            if (horizontal_delta != 0 ) {
                // new scale value
                newEntityScale[0] = x_size / originalEntitySize[0];
            }

            // adjust the current size in the vertical dimension by the
            // amount of drag, depending on which anchor is selected
            int vertical_anchor_direction = 0;
			float y_size_start = originalEntitySize[1] * originalEntityScale[1];
			float y_size = y_size_start;
			
			if (north) {
				y_size += deltaUp;
				vertical_anchor_direction = -1;
			} else if (south) {
				y_size -= deltaUp;
				vertical_anchor_direction = 1;
			}
			if (y_size < 0) {
				y_size = 0;
			}
			float vertical_delta = y_size_start - y_size;
			
			if (vertical_delta != 0 ) {
				// new scale value
				newEntityScale[1] = y_size / originalEntitySize[1];
			}

            // only issue commands if there has been a change
            if ((vertical_delta != 0) || (horizontal_delta != 0)) {

                // determine the new position
                vec.set(
                    (float)originalEntityPosition[0],
                    (float)originalEntityPosition[1],
                    (float)originalEntityPosition[2]);

                // the position change along the vertical vector
                up.set(actionData.zoneOri.up);
                mtx.transform(up);

                up.scale(vertical_delta * vertical_anchor_direction * 0.5f);

                // the position change along the horizontal vector
                right.set(actionData.zoneOri.right);
                mtx.transform(right);

                right.scale(horizontal_delta * horizontal_anchor_direction * 0.5f);

                vec.add(up);
                vec.add(right);

                newEntityPosition[0] = vec.x;
                newEntityPosition[1] = vec.y;
                newEntityPosition[2] = vec.z;

                int transactionID = model.issueTransactionID();
                
                ScaleEntityTransientCommand scaleCmd = 
                	new ScaleEntityTransientCommand(
                		model, 
                		transactionID, 
                		entity, 
                		newEntityPosition, 
                		newEntityScale);
                
				boolean align = editorGrid.alignPositionToGrid(newEntityPosition);
				if (align) {
					
					transactionID = model.issueTransactionID();
					MoveEntityTransientCommand cmd = 
						new MoveEntityTransientCommand(
							model,
							transactionID,
							entity.getEntityID(),
							newEntityPosition,
							new float[]{0, 0, 0});
					
					ArrayList<Command> cmd_list = new ArrayList<Command>();
					cmd_list.add(scaleCmd);
					//cmd_list.add(cmd);
					
					MultiTransientCommand multi = new MultiTransientCommand(
						cmd_list,
						"Scale Entity -> "+ entity.getEntityID());
		
					multi.setErrorReporter(reporter);
					controller.execute(multi);
					
				} else {
					
					scaleCmd.setErrorReporter(reporter);
                	controller.execute(scaleCmd);
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
    }
}
