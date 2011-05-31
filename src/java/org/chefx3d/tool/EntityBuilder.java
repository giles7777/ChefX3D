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

package org.chefx3d.tool;

// External Imports
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

// Internal Imports
import org.chefx3d.model.DefaultBuildingEntity;
import org.chefx3d.model.DefaultEntity;
import org.chefx3d.model.DefaultSegmentableEntity;
import org.chefx3d.model.Entity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;

import org.chefx3d.model.WorldModel;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A helper class to construct entities
 *
 * @author Russell Dodds
 * @version $Revision: 1.19 $
 */
public abstract class EntityBuilder  {

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    protected EntityBuilder() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    // ----------------------------------------------------------
    // Local methods
    // ----------------------------------------------------------
     
    /**
     * Create an Entity and issue the addEntity command
     *
     * @param model The model
     * @param entityID The entity ID
     * @param position The position in 3 dimensions
     * @param rotation The rotation in 3 dimensions
     * @param tool The tool used to create the entity
     * @return The new Entity
     */
    public abstract Entity createEntity(
            WorldModel model,
            int entityID,
            double[] position,
            float[] rotation,
            Tool tool);

    protected VertexEntity createVertexEntity(
            int entityID,
            double[] position,
            float[] rotation,
            SimpleTool tool) {

        Map<String, Map<String, Object>> properties = tool.getProperties();

        // Create the default Entity
        VertexEntity entity =
            new VertexEntity(
                    entityID,
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    properties);

        entity.setPosition(position, false);
        entity.setRotation(rotation, false);

        return entity;

    }

    protected SegmentEntity createSegmentEntity(
            int entityID,
            double[] position,
            float[] rotation,
            SimpleTool tool) {

        Map<String, Map<String, Object>> properties = tool.getProperties();

        // Create the default Entity
        SegmentEntity entity =
            new SegmentEntity(
                    entityID,
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    properties);

        entity.setPosition(position, false);
        entity.setRotation(rotation, false);

        return entity;

    }


    /**
     * Perform any tasks for a default Entity
     *
     * @param entity The entity
     */
    protected DefaultEntity createDefaultEntity(
            int entityID,
            double[] position,
            float[] rotation,
            SimpleTool tool) {

        Map<String, Map<String, Object>> properties = tool.getProperties();

        // Create the default Entity
        DefaultEntity entity =
            new DefaultEntity(
                    entityID,
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    properties);

        entity.setPosition(position, false);
        entity.setRotation(rotation, false);

        return entity;

    }
    

    /**
     * Perform any tasks for a Building Entity
     */
    protected DefaultBuildingEntity createBuildingEntity(
            WorldModel model,
            int entityID,
            double[] position,
            float[] rotation,
            BuildingTool tool) {

        Map<String, Map<String, Object>> properties = tool.getProperties();

        // Create the Building Entity
        DefaultBuildingEntity entity =
            new DefaultBuildingEntity(
                    entityID,
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    properties,
                    tool.getSegmentTool(),
                    tool.getVertexTool());

        entity.setPosition(position, false);

        if (tool.isCreateDefaultShape()) {
        	        	
            float[] size = new float[3];
            entity.getSize(size);

            float x = size[0] * 0.5f;
            float y = 0;
            float z = size[2] * 0.5f;

            VertexTool vertexTool = tool.getVertexTool();
            SegmentTool segmentTool = tool.getSegmentTool();
            
            // now create the 4 vertices, relative to the center         
            int vertex1 = model.issueEntityID();
            VertexEntity vertexEntity1 = 
                (VertexEntity)createEntity(
                        model, 
                        vertex1, 
                        new double[] {-x, y, -z}, 
                        new float[] {0, 0, 1, 0}, 
                        vertexTool);          
            entity.addVertex(vertexEntity1, -1);

            int vertex2 = model.issueEntityID();
            VertexEntity vertexEntity2 = 
                (VertexEntity)createEntity(
                        model, 
                        vertex2, 
                        new double[] {-x, y, -z}, 
                        new float[] {0, 0, 1, 0}, 
                        vertexTool);          
            entity.addVertex(vertexEntity2, -1);

            int vertex3 = model.issueEntityID();
            VertexEntity vertexEntity3 = 
                (VertexEntity)createEntity(
                        model, 
                        vertex3, 
                        new double[] {-x, y, -z}, 
                        new float[] {0, 0, 1, 0}, 
                        vertexTool);          
            entity.addVertex(vertexEntity3, -1);

            int vertex4 = model.issueEntityID();
            VertexEntity vertexEntity4 = 
                (VertexEntity)createEntity(
                        model, 
                        vertex4, 
                        new double[] {-x, y, -z}, 
                        new float[] {0, 0, 1, 0}, 
                        vertexTool);          
            entity.addVertex(vertexEntity4, -1);

            // create the 4 segments
            int segment1 = model.issueEntityID();
            SegmentEntity segmentEntity = 
                (SegmentEntity)createEntity(
                        model, 
                        segment1, 
                        new double[] {1, 1, 1}, 
                        new float[] {0, 0, 1, 0}, 
                        segmentTool);    
            segmentEntity.setStartVertex(vertexEntity1);
            segmentEntity.setEndVertex(vertexEntity2);
            segmentEntity.setExteriorSegment(true);
            entity.addSegment(segmentEntity);
 
            int segment2 = model.issueEntityID();
             segmentEntity = 
                (SegmentEntity)createEntity(
                        model, 
                        segment2, 
                        new double[] {1, 1, 1}, 
                        new float[] {0, 0, 1, 0}, 
                        segmentTool);    
            segmentEntity.setStartVertex(vertexEntity2);
            segmentEntity.setEndVertex(vertexEntity3);
            segmentEntity.setExteriorSegment(true);
            entity.addSegment(segmentEntity);

            int segment3 = model.issueEntityID();
            segmentEntity = 
                (SegmentEntity)createEntity(
                        model, 
                        segment3, 
                        new double[] {1, 1, 1}, 
                        new float[] {0, 0, 1, 0}, 
                        segmentTool);    
            segmentEntity.setStartVertex(vertexEntity3);
            segmentEntity.setEndVertex(vertexEntity4);
            segmentEntity.setExteriorSegment(true);
            entity.addSegment(segmentEntity);

            int segment4 = model.issueEntityID();
            segmentEntity = 
                (SegmentEntity)createEntity(
                        model, 
                        segment4, 
                        new double[] {1, 1, 1}, 
                        new float[] {0, 0, 1, 0}, 
                        segmentTool);    
            segmentEntity.setStartVertex(vertexEntity4);
            segmentEntity.setEndVertex(vertexEntity1);
            segmentEntity.setExteriorSegment(true);
            entity.addSegment(segmentEntity);
 
        }

        return entity;

    }

    /**
     * Perform any tasks for a MultiSegment Entity
     */
    protected DefaultSegmentableEntity createSegmentableEntity(
            int entityID,
            SegmentableTool tool) {
        
        Map<String, Map<String, Object>> properties = tool.getProperties();

        // Create the segmented Entity
        DefaultSegmentableEntity entity =
            new DefaultSegmentableEntity(
                    entityID,
                    properties, 
                    tool.getSegmentTool(), 
                    tool.getVertexTool());

        return entity;
    }

    /**
     * Perform any tasks for an Entity with children
     *
     * @param model The model
     * @param entity The entity
     * @param position The position
     * @param rotation The rotation
     * @param tool The tool used to create the entity
     * @return An entity with children
     */
    protected DefaultEntity createEntityGroupEntity(
            WorldModel model,
            double[] position,
            float[] rotation,
            EntityGroupTool tool) {

        // Create the default Entity
        int entityID = model.issueEntityID();
        DefaultEntity entity = createDefaultEntity(entityID, position, rotation, tool);

        // create all the children
        createChildren(model, entity, position, rotation, tool);

        // update children properties
        entity.setUpdateChildren(true);

        return entity;
    }

    /**
     * Create all the children entities
     *
     * @param model The model
     * @param entity The entity
     * @param position The position
     * @param rotation The rotation
     * @param tool The tool used to create the entity
     */
    private void createChildren(
            WorldModel model,
            Entity entity,
            double[] position,
            float[] rotation,
            EntityGroupTool tool) {

        int entityID;
        DefaultEntity childEntity;
        EntityGroupTool childTool;
        SimpleTool basicTool;
        if (tool.hasChildren()) {

            ArrayList<SimpleTool> children = tool.getChildren();
            Iterator<SimpleTool> itr = children.iterator();

            Object holdingTool;

            while (itr.hasNext()) {

                holdingTool=itr.next();


                if(holdingTool instanceof EntityGroupTool){

                    childTool = (EntityGroupTool)holdingTool;
                    entityID = model.issueEntityID();
                    childEntity = createDefaultEntity(entityID,
                                  position, rotation, childTool);
                    entity.addChild(childEntity);
                    // create any children
                    createChildren(model, childEntity,
                                   position, rotation, childTool);
                }

                if(holdingTool instanceof SimpleTool){
                    basicTool=(SimpleTool)holdingTool;
                    entityID = model.issueEntityID();
                    childEntity = createDefaultEntity(entityID, position,
                                  rotation, basicTool);
                    entity.addChild(childEntity);
                }


            }

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
