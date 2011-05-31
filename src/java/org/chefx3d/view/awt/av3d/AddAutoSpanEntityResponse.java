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

//External imports
import java.util.ArrayList;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.WorldModel;

import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to add auto span entity events.
 *
 * @author Rex Melton
 * @version $Revision: 1.7 $
 */
public class AddAutoSpanEntityResponse implements TrackerEventResponse {

    /** Reference to world model */
    private WorldModel model;

    /** The controlleer to send commands to */
    private CommandController controller;

    /** Reference to error reporter */
    private ErrorReporter reporter;

    /** Reference to entity builder */
    private EntityBuilder entityBuilder;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** Instance of hierarchy transformation calculator */
    private TransformUtils tu;
    
    /** Scratch vecmath objects */
    private Matrix4f mtx;
	private Point3f pnt;
	
	/** Utility for aligning the model with the editor grid */
	private EditorGrid editorGrid;
	
    /**
     * Constructor
     *
     * @param model
     * @param controller
     * @param reporter
	 * @param editorGrid
     */
	public AddAutoSpanEntityResponse(
		WorldModel model,
		CommandController controller,
		ErrorReporter reporter,
		EditorGrid editorGrid) {

        this.model = model;
        this.controller = controller;
        this.reporter = reporter;
		this.editorGrid = editorGrid;
        entityBuilder = DefaultEntityBuilder.getEntityBuilder();
		
        tu = new TransformUtils();
        mtx = new Matrix4f();
		pnt = new Point3f();
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

		// the parent is always the zone
        Entity parentEntity = actionData.zoneWrapper.entity;
 
		// convert the mouse position from world to zone coordinates
		pnt.set(trackerState.worldPos);
		tu.getLocalToVworld(actionData.zoneWrapper.transformGroup, mtx);
        mtx.invert();
        mtx.transform(pnt);
		
		// place in the zone's plane,
		// plus half the object dimension
		float[] size = tool.getSize();
        double[] position = new double[3];
        
        double[] ghostPos = (double[])tool.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                PositionableEntity.POSITION_PROP);
        if (ghostPos != null) {
            position[0] = ghostPos[0];
        } else {
            position[0] = pnt.x;
        }
        position[1] = pnt.y;
		position[2] = (size[2] * 0.5f - AV3DConstants.DEFAULT_EMBEDDING_DEPTH);

		editorGrid.alignPositionToGrid(position);
		
        float[] rotation = new float[]{0, 1, 0, 0};

        // create the entity
        Entity newEntity = entityBuilder.createEntity(
            model,
            model.issueEntityID(),
            position,
            rotation,
            tool);
		
		// clear out the 'bogus' property
		AV3DUtils.setShadowState(newEntity, false);
			
        // stack the commands together
        ArrayList<Command> commandList = new ArrayList<Command>();

        Command cmd = new AddEntityChildCommand(
        		model, 
        		model.issueTransactionID(), 
        		parentEntity,
        		newEntity, 
        		true);
        
        commandList.add(cmd);
        
        cmd = new SelectEntityCommand(model, newEntity, true);
        commandList.add(cmd);

        // Create the MultiCommand and send it
        MultiCommand stack = new MultiCommand(
                commandList,
                "Add Entity -> " +  newEntity.getEntityID());

        stack.setErrorReporter(reporter);
        controller.execute(stack);
    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Sets the correct entity builder to use
     * @param builder
     */
    public void setEntityBuilder(EntityBuilder builder){
        entityBuilder = builder;
    }

    /**
     * Initialize in preparation for a response
     *
     * @param ad The initial device position of the mouse
     */
    void setActionData(ActionData ad) {
        actionData = ad;
    }
}
