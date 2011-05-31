/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

//External Imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.model.RemoveVertexCommand;

/**
 * A set of helper methods for executing model Commands
 *
 * @author Russell Dodds
 * @version $Revision: 1.32 $
 */
public class CommandUtils  {

    /** The model */
    private BaseWorldModel model;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** A helper class to handle selection easier */
    private EntitySelectionHelper seletionHelper;
    
    /**
     * Creates a utility class for:
     *      - deleting entities based on the current selection
     *      - reseting the model 
     * 
     * @param worldModel - The WorldModel containing the entities
     */
    public CommandUtils(WorldModel worldModel) {

        // Cast to package definition to access protected methods
        model = (BaseWorldModel) worldModel;

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        seletionHelper = EntitySelectionHelper.getEntitySelectionHelper();
    }

    // ----------------------------------------------------------
    // Local methods
    // ----------------------------------------------------------

    /**
     * A helper method to clear the entire model. 
     *
     * @param local - Is the request local
     * @param listener - The model listener
     */
    public void resetModel(boolean local, ModelListener listener) {

        model.clear(local, listener);
        model.clearHistory();

    }

    /**
     * A helper method to delete the currently selected entity, if one is
     * selected
     *
     * @param local - Is the request local
     * @param listener - The model listener
     */
    public void removeSelectedEntity(boolean local, ModelListener listener) {

        ArrayList<Command> commandList = new ArrayList<Command>();
        
        // Get the currentlySelected Entity
        List<Entity> selectedList = 
        	Collections.synchronizedList(seletionHelper.getSelectedList());
        
        int len = selectedList.size();
        
        // Setup segment removal specific fields in case we need them
        ArrayList<Entity> selectedSegments = new ArrayList<Entity>();
        ArrayList<Entity> verticesDeleted = new ArrayList<Entity>();
        
        for(int i = 0; i < len; i++){
        	
        	Entity tempEntity = selectedList.get(i);
        	
        	if(tempEntity.getType() == Entity.TYPE_SEGMENT){
        		selectedSegments.add(tempEntity);
        	}
        }
        
        // Begin actual processing
        for (int i = 0; i < len; i++) {
            Entity selectedEntity = selectedList.get(i);
        
            if (selectedEntity == null)
                continue;
            
            // undo the selection
            SelectEntityCommand selectCmd = new SelectEntityCommand(model, selectedEntity, false);
            commandList.add(selectCmd);

        	if (selectedEntity instanceof VertexEntity) {
        	    
        		SegmentableEntity parent = 
        		    (SegmentableEntity)model.getEntity(selectedEntity.getParentEntityID());
                
                ArrayList<SegmentEntity> segments = parent.getSegments();
                
                ArrayList<SegmentEntity> connectedSegments = new ArrayList<SegmentEntity>();                
                int len1 = segments.size();
                for (int j = 0; j < len1; j++) {
                	
                    SegmentEntity segment = segments.get(j);
                	if (segment.getStartVertexEntity() == selectedEntity ||
                	    segment.getEndVertexEntity() == selectedEntity) {
                	    
                		connectedSegments.add(segments.get(j));                		
                	}                	
                }
                
                if (connectedSegments.size() == 1) {
                    
                    RemoveSegmentCommand segmentCmd =
                        new RemoveSegmentCommand(
                        		model,
                                parent, 
                                connectedSegments.get(0).getEntityID());
                    segmentCmd.setErrorReporter(errorReporter);
   
                    commandList.add(segmentCmd);
   
                    // finally, add the remove vertex command                     
                    RemoveVertexCommand vertexCmd = 
                        new RemoveVertexCommand(
                        		model,
                                parent, 
                                selectedEntity.getEntityID());
                    vertexCmd.setErrorReporter(errorReporter);
   
                    commandList.add(vertexCmd);
                                              
                }
                
        	} else if (selectedEntity instanceof SegmentEntity){
        	    
        		 SegmentableEntity parent = 
        		     (SegmentableEntity)model.getEntity(selectedEntity.getParentEntityID());
        		                
        		 VertexEntity startVertex = ((SegmentEntity)selectedEntity).getStartVertexEntity();
                 VertexEntity endVertex = ((SegmentEntity)selectedEntity).getEndVertexEntity();
                 
                 RemoveSegmentCommand segmentCmd =
                     new RemoveSegmentCommand(
                    		 model,
                             parent, 
                             selectedEntity.getEntityID());
                 segmentCmd.setErrorReporter(errorReporter);
                 
                 commandList.add(segmentCmd);
                 
                 /*
                  * Look at vertices to determine if they should also be 
                  * removed. Check against other selected segments that will
                  * be deleted.
                  */
                 int startVertexShareCount = 
                	 parent.getSegmentCount(startVertex);
                 
                 int endVertexShareCount = 
                	 parent.getSegmentCount(endVertex);
                 
                 // Start vertex
        		 if (startVertexShareCount == 1){
        			 
        			 RemoveVertexCommand vertexCmd = 
        			     new RemoveVertexCommand(
        			    		 model,
        			    		 parent, 
        			    		 startVertex.getEntityID());
                     vertexCmd.setErrorReporter(errorReporter);
                       
                     commandList.add(vertexCmd);
                     
        		 } else if (startVertexShareCount > 1) {
        			 
        			 int segmentUsingCount = 1;
        			 Entity tmpE;
        			 
        			 for(int w = 0; w < selectedSegments.size(); w++){
        				 
        				 tmpE = selectedSegments.get(w);
        				 
        				 if(tmpE.getEntityID() != selectedEntity.getEntityID()){
        					 
        					 Entity tmpVE = 
        						 ((SegmentEntity)tmpE).getStartVertexEntity();
        					 
        					 if(tmpVE.getEntityID() == startVertex.getEntityID()){
        						 segmentUsingCount++;
        					 }
        					 
        					 tmpVE =
        						 ((SegmentEntity)tmpE).getEndVertexEntity();
        					 
        					 if(tmpVE.getEntityID() == startVertex.getEntityID()){
        						 segmentUsingCount++;
        					 }
        				 }
        			 }
        			 
        			 if(segmentUsingCount == startVertexShareCount){
        				 
        				 if(!verticesDeleted.contains(startVertex)){

	        				 RemoveVertexCommand vertexCmd = 
	            			     new RemoveVertexCommand(
	            			    		 model,
	            			    		 parent, 
	            			    		 startVertex.getEntityID());
	                         vertexCmd.setErrorReporter(errorReporter);
	                           
	                         commandList.add(vertexCmd);
	                         
	                         verticesDeleted.add(startVertex);
        				 }
        			 }
        		 }
        		 
        		 // End vertex
        		 if(endVertexShareCount == 1){
      			     RemoveVertexCommand vertexCmd = 
                         new RemoveVertexCommand(
                        		 model,
                        		 parent, 
                        		 endVertex.getEntityID());
                     vertexCmd.setErrorReporter(errorReporter);
                     
                     commandList.add(vertexCmd);
        		 
        		 } else if (endVertexShareCount > 1){
        			 
        			 int segmentUsingCount = 1;
        			 Entity tmpE;
        			 
        			 for(int w = 0; w < selectedSegments.size(); w++){
        				 
        				 tmpE = selectedSegments.get(w);
        				 
        				 if(tmpE.getEntityID() != selectedEntity.getEntityID()){
        					 
        					 Entity tmpVE = 
        						 ((SegmentEntity)tmpE).getStartVertexEntity();
        					 
        					 if(tmpVE.getEntityID() == endVertex.getEntityID()){
        						 segmentUsingCount++;
        					 }
        					 
        					 tmpVE =
        						 ((SegmentEntity)tmpE).getEndVertexEntity();
        					 
        					 if(tmpVE.getEntityID() == endVertex.getEntityID()){
        						 segmentUsingCount++;
        					 }
        				 }
        			 }
        			 
        			 if(segmentUsingCount == endVertexShareCount){
        				 
        				 if(!verticesDeleted.contains(endVertex)){

	        				 RemoveVertexCommand vertexCmd = 
	            			     new RemoveVertexCommand(
	            			    		 model,
	            			    		 parent, 
	            			    		 endVertex.getEntityID());
	                         vertexCmd.setErrorReporter(errorReporter);
	                           
	                         commandList.add(vertexCmd);
	                         
	                         verticesDeleted.add(endVertex);
        				 }
        			 }
        		 }
                              
        	} else {
        	    
        	    // Create the remove command
        	    Entity parent = model.getEntity(selectedEntity.getParentEntityID());
                RemoveEntityChildCommand cmd = new RemoveEntityChildCommand(model, parent, selectedEntity);
                commandList.add(cmd);
         	}
        }
        
        if (commandList.size() > 0) {
            // finally stack it all together and send out
            MultiCommand cmd = 
                new MultiCommand(
                        commandList, 
                        "Delete");
            model.applyCommand(cmd);

        }
        
    }   
    
    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }
}
