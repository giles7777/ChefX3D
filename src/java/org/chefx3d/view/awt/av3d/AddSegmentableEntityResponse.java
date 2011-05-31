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
import java.util.HashMap;
import java.util.Map;

import org.j3d.device.input.TrackerState;

// Internal imports
import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.AddVertexCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SegmentableTool;
import org.chefx3d.tool.VertexTool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to add SegmentableEntity event. The appropriate command is issued
 * for the event. In this case a SegmentableEntity is added to the scene
 * with a vertex at the position selected in the world.
 *
 * @author Ben Yarger
 * @version $Revision: 1.26 $
 *
 */
public class AddSegmentableEntityResponse implements TrackerEventResponse, AV3DConstants {

    private WorldModel model;
    private CommandController controller;
    private ErrorReporter reporter;
    private EntityBuilder entityBuilder;

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
    public AddSegmentableEntityResponse(
		WorldModel model, 
    	CommandController controller, 
    	ErrorReporter reporter,
		EditorGrid editorGrid) {

        this.model = model;
        this.controller = controller;
        this.reporter = reporter;
		this.editorGrid = editorGrid;
        entityBuilder = DefaultEntityBuilder.getEntityBuilder();
    }

    /**
     * Begins the processing required to generate a command in response
     * to the input received.
     *
     * @param trackerID The id of the tracker calling the original handler
     * @param trackerState The event that started this whole thing off
     * @param entities The array of entities to handle.  Expects the first item to be
     *      the parent Entity and the second item to be the SegmentableEntity
     * @param tool The tool that is used in the action (can be null)
     */
    public void doEventResponse(
            int trackerID,
            TrackerState trackerState,
            Entity[] entities,
            Tool tool) {

System.out.println("ASER:TYPE_MULTI_SEGMENT");

        SegmentableTool segmentableTool = null;
        if (tool instanceof SegmentableTool) {
            segmentableTool = (SegmentableTool)tool;
        }

        if (segmentableTool == null) {
            return;
        }

        Entity parentEntity;    // ContentContainerEntity
        Entity newEntity;       // SegmentableEntity

        if ((entities != null) && (entities.length == 1)){
            parentEntity = entities[0];
        } else {
            return;
        }

        double position[] = new double[3];
		
        float height = (float) ChefX3DRuleProperties.MAXIMUM_WALL_HEIGHT;

		Object objHeight = (Float) segmentableTool.getVertexTool().getProperty(
			Entity.EDITABLE_PROPERTIES, 
			VertexEntity.HEIGHT_PROP);

		if (objHeight != null) {
			height = (Float) objHeight;
		}
		
		//////////////////////////////////////////////////////////
		// rem: this doesn't look right, the position should be
		// transformed into the zone from world coords
        position[0] = trackerState.worldPos[0];
        position[1] = trackerState.worldPos[1];
        position[2] = height;
		//////////////////////////////////////////////////////////

		editorGrid.alignPositionToGrid(position);
		
        // Create the new Segmentable entity
        int entityID = model.issueEntityID();

        newEntity = entityBuilder.createEntity(
                model,
                entityID,
                position,
                new float[] {0,1,0,0},
                tool);

        // add the SegmentableEntity as a child of the world parent
        Command cmd = new AddEntityChildCommand(
			model,
			model.issueTransactionID(),
			parentEntity, 
			newEntity,
			true);
        cmd.setErrorReporter(reporter);

        // stack the commands together
        ArrayList<Command> commandList = new ArrayList<Command>();
        commandList.add(cmd);

        // add the vertex entity to the SegmentableEntity
        int endVertexID = model.issueEntityID();

        VertexTool vertexTool = segmentableTool.getVertexTool();

        Entity newVertexEntity = entityBuilder.createEntity(
                model,
                endVertexID,
                position,
                new float[] {0,1,0,0},
                vertexTool);
		
		cmd = new AddVertexCommand(
			(SegmentableEntity)newEntity,
			(VertexEntity)newVertexEntity,
			-1);

        cmd.setErrorReporter(reporter);
        commandList.add(cmd);

        cmd = new SelectEntityCommand(model, newVertexEntity, true);
        commandList.add(cmd);

        // Create the MultiCommand and send it
        MultiCommand stack = new MultiCommand(
                commandList,
                "Add Entity -> " +  newEntity.getEntityID());

        stack.setErrorReporter(reporter);
        controller.execute(stack);
    }

    /**
     * Sets the correct entity builder to use
     * @param builder
     */
    public void setEntityBuilder(EntityBuilder builder){
        entityBuilder = builder;
    }
}
