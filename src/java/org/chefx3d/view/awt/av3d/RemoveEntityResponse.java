/**                        Copyright Yumetech, Inc (c) 2009
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

import org.j3d.device.input.TrackerState;

// Local imports
import org.chefx3d.model.AddSegmentCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.CommandUtils;
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.RemoveEntityCommand;
import org.chefx3d.model.RemoveSegmentCommand;
import org.chefx3d.model.RemoveVertexCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.Selection;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.SegmentTool;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.ErrorReporter;

/**
 * Responds to Remove Entity events.
 * The appropriate command is issued for the event.
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.18 $
 */
public class RemoveEntityResponse implements TrackerEventResponse {

    private WorldModel model;
    private ErrorReporter reporter;
    private EntityBuilder entityBuilder;

    /**
     * Default constructor
     * @param worldModel
     * @param errorReporter
     */
    public RemoveEntityResponse(WorldModel worldModel, 
    							ErrorReporter errorReporter) {

        model = worldModel;
        reporter = errorReporter;
        entityBuilder = DefaultEntityBuilder.getEntityBuilder();
    }

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

        
        CommandUtils util = new CommandUtils(model);
        util.removeSelectedEntity(true, null);

        // TODO: there is a command utility class to help with deletion
        // need to look at using that since that is what the DELETE key
        // board button is going to use as well
        
        // Issue a command for each individual entity
//        for(int i = 0; i < entities.length; i++){
//            
//            Entity entity = entities[i];
//            if (entity instanceof SegmentEntity) {
//                // remove segment entity
//                System.out.println("implement remove segment entity");
//            } else if (entity instanceof VertexEntity) {
//                removeVertexEntity((VertexEntity)entity);
//            } else {
//                removeDefaultEntity(entities[i]);
//            }
//            
//        }

    }

    /**
     * Deletes the parameter entity from the scene
     * 
     * @param entity
     */
    private void removeVertexEntity(VertexEntity entity){
    
        // Create the remove command
         ArrayList<Command> commandList = new ArrayList<Command>();

         int segmentableID = entity.getParentEntityID();
         SegmentableEntity segmentableEntity = 
             (SegmentableEntity)model.getEntity(segmentableID);
         
         if (segmentableEntity == null) {
             return;
         }
        
         // if it is an middle vertex disallow
         int currentVertexID = entity.getEntityID();
         int startVertexID = segmentableEntity.getStartVertexID();
         int endVertexID = segmentableEntity.getEndVertexID();
         if (startVertexID == currentVertexID || endVertexID == currentVertexID) {
             // TODO: need to notify the user of why this was denied
             return;
         }
                 
         // undo the selection
         SelectEntityCommand selectCmd = new SelectEntityCommand(model, entity, false);
         selectCmd.setErrorReporter(reporter);        
         commandList.add(selectCmd);
             
         // if it is a start or end vertex the just remove
         ArrayList<SegmentEntity> list = segmentableEntity.getSegments();
         SegmentEntity segment = null;
         int len = list.size();
         for(int i = 0; i < len; i++){
             SegmentEntity check = list.get(i);
             if (check.getStartVertexEntity().getEntityID() == currentVertexID || 
                 check.getEndVertexEntity().getEntityID() == currentVertexID) {                 
                 segment = list.get(i);
                 break;
             }
         }
         if (segment != null){
             RemoveSegmentCommand removeSegment = 
                 new RemoveSegmentCommand(
                		 model,
                		 segmentableEntity, 
                		 segment.getEntityID());
             commandList.add(removeSegment);
         }     
         
         // finally remove the vertex
         RemoveVertexCommand removeVertex = 
             new RemoveVertexCommand(
            		 model,
                     segmentableEntity,
                     entity.getEntityID());
         commandList.add(removeVertex);

         MultiCommand cmd = new MultiCommand(commandList, "Remove Vertex");
         cmd.setErrorReporter(reporter);
         cmd.execute();
    }

    /**
     * Deletes the parameter entity from the scene
     * @param entity
     */
    private void removeDefaultEntity(Entity entity){
   
        Entity parent = model.getEntity(entity.getParentEntityID());

        ArrayList<Command> commandList = new ArrayList<Command>();
                
        // Create the unselect command
        SelectEntityCommand selectCmd = 
        	new SelectEntityCommand(model, entity, false);
        selectCmd.setErrorReporter(reporter);        
        commandList.add(selectCmd);
       
        // Create the remove command
        RemoveEntityChildCommand removeCmd = 
        	new RemoveEntityChildCommand(model, parent, entity);
        removeCmd.setErrorReporter(reporter);        
        commandList.add(removeCmd);
        
        // stack the command together
        MultiCommand multiCmd = 
        	new MultiCommand(commandList, removeCmd.getDescription());
        multiCmd.setErrorReporter(reporter);     
        model.applyCommand(multiCmd);
        
    }
    
}
