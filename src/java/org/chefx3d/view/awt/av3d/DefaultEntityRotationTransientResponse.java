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
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RotateEntityTransientCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.ErrorReporter;


/**
 * Responds to DefaultEntity Rotate  events. This is only for
 * transient  events. The appropriate command is issued for the event.
 *
 * @author Jonathon Hubbard, Ben Yarger
 * @author Eric Fickenscher - version 1.4 onward
 * @version $Revision: 1.13 $
 *
 */
public class DefaultEntityRotationTransientResponse implements
        TrackerEventResponse, AV3DConstants {

    /** The world model */
    private WorldModel model;

    /** The command controller */
    private CommandController controller;

    /** The error reporter */
    private ErrorReporter reporter;

    /** The anchor node */
    private AV3DAnchorInformation anchorNode;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** computation variables */
    private Vector3f originalVector;
    private Vector3f currentVector;

	/** Local transformation utils */
	private TransformUtils tu;
	private Matrix4f mtx;
	private Point3f pnt;
	
    private float[] rotation;
    private float[] start_rotation;
    private float[] position_f;
    private double[] position_d;

    /**
     * Default constructor.
     */
    public DefaultEntityRotationTransientResponse(
        WorldModel worldModel,
        CommandController commandController,
        ErrorReporter errorReporter) {

        model = worldModel;
        controller = commandController;
        reporter = errorReporter;

        originalVector = new Vector3f();
        currentVector = new Vector3f();
		
		tu = new TransformUtils();
		mtx = new Matrix4f();
		pnt = new Point3f();
		
		rotation = new float[4];
		start_rotation = new float[4];
		position_f = new float[3];
		position_d = new double[3];
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

        // Issue a command for each individual entity
        for(int i = 0; i < entities.length; i++){

			PositionableEntity entity = (PositionableEntity)entities[i];
			int entityID = entity.getEntityID();
			int parentEntityID = entity.getParentEntityID();
			
			entity.getPosition(position_d);
			
			AV3DEntityWrapper parentWrapper = actionData.wrapperMap.get(parentEntityID);
			if (parentWrapper != actionData.zoneWrapper) {
				// if our parent is not the zone, then
				// calculate the position of the entity 
				// in zone relative coordinates.
				AV3DEntityWrapper wrapper = actionData.wrapperMap.get(entityID);
				tu.getLocalToVworld(
					wrapper.transformGroup, 
					actionData.zoneWrapper.transformGroup, 
					mtx);
				
				pnt.x = (float)position_d[0];
				pnt.y = (float)position_d[1];
				pnt.z = (float)position_d[2];
				mtx.transform(pnt);
				pnt.get(position_f);
				
			} else {
				// if our parent is the zone, then the
				// position is already zone relative
				position_f[0] = (float)position_d[0];
				position_f[1] = (float)position_d[1];
				position_f[2] = (float)position_d[2];
			}
			
            // This vector points from the center of the object
            // to the point that was initially clicked
            originalVector.set(
				actionData.mouseWorldPosition[0] - position_f[0],
                actionData.mouseWorldPosition[1] - position_f[1],
                0);

            // This vector points from the center of the object
            // to the *current* location of the mouse
            currentVector.set(
				trackerState.worldPos[0] - position_f[0],
                trackerState.worldPos[1] - position_f[1],
                0);

            Vector3f crossProd = new Vector3f();
            crossProd.cross(originalVector, currentVector);

            // normalizin'
            originalVector.normalize();
            currentVector.normalize();

            // time for crazy maths.  Which direction is the currentVector rotated?
            // Clockwise from originalVector or counter-clockwise?
            // use the perpendicular vector to decide
            float angle = originalVector.angle(currentVector);

			entity.getStartingRotation(start_rotation);
            float rotationAngle = (crossProd.z > 0 ?  angle : -angle) + start_rotation[3];
            rotation[0] = 0;
            rotation[1] = 0;
            rotation[2] = 1;
            rotation[3] = rotationAngle;

            int transactionID = model.issueTransactionID();
            RotateEntityTransientCommand cmd = new RotateEntityTransientCommand(
                model,
                transactionID,
                entityID,
                rotation);

            cmd.setErrorReporter(reporter);
            controller.execute(cmd);
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
