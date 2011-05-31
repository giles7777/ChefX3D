/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2010
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
import java.util.*;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * The base implementation for all WorldModels.  Inward facing interface.
 *
 * @author Alan Hudson
 * @version $Revision: 1.96 $
 */
abstract class BaseWorldModel implements WorldModel {

    /** Handler of ModelListeners */
	protected ModelListenerHandler modelListenerHandler;

	// rem: this listener type seems to be obsolete?
    /** The list of PropertyStructureListeners */
    protected ArrayList<PropertyStructureListener> propertyListeners;

    /** Entity indexed by entityID */
    protected Entity[] entities;

    /** The greatest index in entities[] that possibly contains
     *  a non-null Entity */
    protected int lastEntityIndex;

    /** The selectedEntity */
    private Entity selectedEntity;

    /** The current master pos */
    private double[] masterPos;

    /** The current master orientation */
    private float[] masterRot;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** EntityID to Entity map for children entities */
    private HashMap<Integer, Entity> childrenMap;

    public BaseWorldModel() {

        lastEntityIndex = -1;

        entities = new Entity[150];

		modelListenerHandler = new ModelListenerHandler();
        propertyListeners = new ArrayList<PropertyStructureListener>();

        masterPos = new double[3];
        masterRot = new float[4];

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        childrenMap = new HashMap<Integer, Entity>();
    }

    // ----------------------------------------------------------
    // Methods implementing WorldModel
    // ----------------------------------------------------------

    /**
     * Add a listener for Model changes. Duplicates will be ignored.
     *
     * @param l The listener.
     */
    public void addModelListener(ModelListener l) {
		modelListenerHandler.add(l);
    }

    /**
     * Remove a listener for Model changes.
     *
     * @param l The listener.
     */
    public void removeModelListener(ModelListener l) {
		modelListenerHandler.remove(l);
    }

    /**
     * Add a listener for Property changes. Duplicates will be ignored.
     *
     * @param l The listener.
     */
    public void addPropertyStructureListener(PropertyStructureListener l) {
        if (!propertyListeners.contains(l)) {
            propertyListeners.add(l);
        }
    }

    /**
     * Remove a listener for Property changes.
     *
     * @param l The listener.
     */
    public void removePropertyStructureListener(PropertyStructureListener l) {
        propertyListeners.remove(l);
    }

    /**
     * Get a unique ID for an entity.
     *
     * @return The unique ID
     */
    public synchronized int issueEntityID() {
                
        // In a multiuser context it may have been used so check and skip

        int nextID = lastEntityIndex + 1;

        //errorReporter.messageReport("issue ID  current: " + nextID);

        int max_index = entities.length - 1;
        
        while ( ( nextID <= max_index ) && ( entities[nextID] != null ) ) {
            nextID++;
        }

        lastEntityIndex = nextID;

        return nextID;

    }

    /**
     * Get a unique ID for a transaction. A transaction is a set of transient
     * commands and the final real command. A transactionID only needs to be
     * unique for a short period of time. 0 is reserved as marking a
     * transactionless command.
     *
     * @return The ID
     */
    public int issueTransactionID() {
        // Generate a likely uniqueID. A failure will just result in some
        // transient events being lost

        int ret_val = 0;

        while (ret_val == 0) {
            ret_val = (int) (Math.random() * Integer.MAX_VALUE);
        }

        return ret_val;
    }

    /**
     * Update the selection list.  Currently only uses the first
     * item in the list
     *
     * @param selection The list of selected entities. The last one is the
     *        latest.
     */
//    public void changeSelection(List<Selection> selection) {
//
//        if (selection.size() == 0) {
//
//            selectedEntity = null;
//
//        } else {
//
//            // get the first item for now
//            Selection select = selection.get(0);
//            if (select.getEntityID() < 0) {
//
//                selectedEntity = null;
//
//            } else {
//                
//                selectedEntity = getEntity(select.getEntityID());
//                Entity parent = null;
//                if(selectedEntity.getParentEntityID() != -1)
//                	parent = getEntity(selectedEntity.getParentEntityID());
//                
//                if (selectedEntity != null) {
//
//                    if (selectedEntity instanceof SegmentableEntity) {
//                        ((SegmentableEntity)selectedEntity).setSelectedSegmentID(select.getSegmentID());
//                        ((SegmentableEntity)selectedEntity).setSelectedVertexID(select.getVertexID());
//                    }else if (parent != null && parent instanceof SegmentableEntity) {
//                    	((SegmentableEntity)parent).setSelectedSegmentID(select.getSegmentID());
//                        ((SegmentableEntity)parent).setSelectedVertexID(select.getVertexID());
//                    }
//
//                }
//
//            }
//
//        }
//
//        // send out notifaction
//        int len = modelListeners.size();
//        for (int i = 0; i < len; i++) {
//            ModelListener l =  modelListeners.get(i);
//            l.selectionChanged(selection);
//        }
//
//    }

    /**
     * ReIssue all events to catch a model listener up to the current state.
     *
     * @param l The model listener
     */
    public void reissueEvents(ModelListener l) {
        int len = entities.length;

        Entity entity;
        List<VertexEntity> vertices = null;
        VertexEntity vertex;

        for (int i = 0; i < len; i++) {
            entity = entities[i];

            if (entity == null)
                continue;

            addEntity(false, entity, l);

            if (entity instanceof SegmentableEntity) {
                vertices = ((SegmentableEntity)entity).getVertices();

                if (vertices != null) {
                    int cnt = 0;
                    for (Iterator<VertexEntity> j = vertices.iterator(); j.hasNext();) {
                        ((SegmentableEntity)entity).addVertex(j.next(), i);
                    }
                }
            }

        }
    }

    /**
     * Clear the model.
     *
     * @param local Was this action initiated from the local UI
     * @param listener The model listener to inform or null for all
     */
    public void clear(boolean local, ModelListener listener) {
        int len = entities.length;
        for (int i = 0; i < len; i++) {
            if (entities[i] != null) {

                if (listener == null) {
					modelListenerHandler.entityRemoved(local, entities[i]);
                } else {
                    listener.entityRemoved(local, entities[i]);
                }
                entities[i] = null;
            }
        }

        // now send notification of the reset
        if (listener == null) {
			modelListenerHandler.modelReset(local);
        } else {
            listener.modelReset(local);
        }

        lastEntityIndex = -1;
    }

    // ----------------------------------------------------------
    // Local methods
    // ----------------------------------------------------------

    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity to add
     * @param mlistener The model listener or null for all
     */
    protected void addEntity(boolean local, Entity entity, ModelListener mlistener) {
        
        int len = entities.length;
        int entityID = entity.getEntityID();
        //Tool tool = entity.getTool();
     

        if (entityID >= len) {
            Entity[] newValues = new Entity[entityID + 50];

            for (int i = 0; i < len; i++) {
                newValues[i] = entities[i];
            }
            entities = newValues;
        }


        entities[entityID] = entity;

        addChildrenToMap(entity);

        //lastEntityIndex = Math.max( lastEntityIndex, entityID );

        if (mlistener == null) {
			modelListenerHandler.entityAdded(local, entity);
        } else {
            mlistener.entityAdded(local, entity);
        }
    }

    /**
     * Remove an entity
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity to remove
     * @param listener The model listener to inform or null for all
     */
    protected void removeEntity(boolean local, Entity entity,
            ModelListener listener) {

        if (entity != null) {

            // check all entities for associations
            Entity check;
            int[] associates;

            //for (int i = 0; i < entities.length; i++) {
            //    check = entities[i];
            //}

            removeChildrenFromMap(entity);

            // now, remove the entity
            entities[entity.getEntityID()] = null;

            if (listener == null) {
				modelListenerHandler.entityRemoved(local, entity);
            } else {
                listener.entityRemoved(local, entity);
            }

        }

    }

    /**
     * User view information changed.
     *
     * @param local Was this action initiated from the local UI
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     * @param listener The model listener to inform or null for all
     */
    public void setViewParams(boolean local, double[] pos, float[] rot,
            float fov, ModelListener listener) {
        masterPos[0] = pos[0];
        masterPos[1] = pos[1];
        masterPos[2] = pos[2];

        masterRot[0] = rot[0];
        masterRot[1] = rot[1];
        masterRot[2] = rot[2];
        masterRot[3] = rot[3];

        if (listener == null) {
			modelListenerHandler.viewChanged(local, pos, rot, fov);
        } else {
            listener.viewChanged(local, pos, rot, fov);
        }
    }

    /**
     * The master view has changed.
     *
     * @param local Was this action initiated from the local UI
     * @param viewID The view which is master
     * @param listener The model listener to inform or null for all
     */
    public void setMaster(boolean local, long viewID, ModelListener listener) {
        if (listener == null) {
			modelListenerHandler.masterChanged(local, viewID);
        } else {
            listener.masterChanged(local, viewID);
        }
    }

    /**
     * Get the model data.
     *
     * @return Returns the current model data
     */
    public Entity[] getModelData() {
        // Favor faster access to ease of return on this call. Just resize and
        // copy the array
        int max_num_entities = entities.length;
        Entity[] ret_val = new Entity[max_num_entities];

        for (int i = 0; i < max_num_entities; i++) {
            ret_val[i] = entities[i];
        }

        return ret_val;
    }

    /**
     * Get an entity.
     *
     * @param entityID The ID of the entity
     * @return The entity or null if not found
     */
    public Entity getEntity(int entityID) {
        if ( ( entityID >= 0 ) && ( entityID < entities.length ) ) {
            Entity entity = entities[entityID];

            if (entity != null)
                return entity;

            // Check for children
            Entity root = null;
            int len = entities.length;
            for (int i = 0; i < len; i++) {
                root = entities[i];
                if (root != null && root instanceof SceneEntity) {
                    break;
                }
            }

            if (root != null)
                entity = findEntityId(root.getChildren(), entityID);

            return entity;
        } else if (( entityID >= 0 ) && ( entityID <= this.lastEntityIndex )) {
            Entity entity = null;
            Entity root = null;
            int len = entities.length;
            for (int i = 0; i < len; i++) {
                root = entities[i];
                if (root != null && root instanceof SceneEntity) {
                    break;
                }
            }

            if (root != null)
                entity = findEntityId(root.getChildren(), entityID);

            return entity;
        }else {
            return null;
        }

    }

    /**
     * Get the entity that represents the location
     *
     * @return The entity or null if not found
     */
    public Entity getSceneEntity() {

        Entity entity;

        for (int i = 0; i < entities.length; i++) {
            entity = entities[i];

            if (entity != null) {

                if (entity.getType() == Entity.TYPE_WORLD) {
                    return entity;
                }

            }

        }

        return null;

    }

    /**
     * Set the model data.
     */
    protected void setModelData(Entity[] data) {

        int num_entities = data.length;

        // suss out the greatest index needed for the local entity storage array
        int max_id = 0;
        for (int i = 0; i < num_entities; i++) {
            max_id = Math.max( max_id, data[i].getEntityID( ) );
        }

        // increase the local entity storage array if necessary
        if (entities.length <= max_id) {
            entities = new Entity[max_id + 50];
        }

        lastEntityIndex = max_id;

        // initialize the local entity storage array with the new data,
        // NOTE: this presumes that the model has been cleared, otherwise
        // the potential exists for a mix of data from different sources
        for (int i = 0; i < num_entities; i++) {
            Entity entity = data[i];
            int index = entity.getEntityID( );
            entities[index] = entity;
        }

        reissueEvents(null);
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

    /**
     * Add an entities children to the children map.  Add's all sub-children
     * as well.
     *
     * @param entity The entity to process
     */
    private void addChildrenToMap(Entity entity) {
        
        if (entity.getChildCount() > 0) {
            ArrayList children = entity.getChildren();

            int len = children.size();

            for(int i=0; i < len; i++) {
                addChildrenToMap((Entity)children.get(i));
            }

        } else {
            childrenMap.put(new Integer(entity.getEntityID()), entity);
        }
    }

    /**
     * Remove an entities children from the children map.  Removes's all
     * sub-children as well.
     *
     * @param entity The entity to process
     */
    private void removeChildrenFromMap(Entity entity) {
        if (entity.getChildCount() > 0) {
            ArrayList children = entity.getChildren();

            int len = children.size();

            for(int i=0; i < len; i++) {
                removeChildrenFromMap((Entity)children.get(i));
            }

        } 
        childrenMap.remove(new Integer(entity.getEntityID()));
    }
    
    private Entity findEntityId(ArrayList<Entity> childrenList, int entityId){
        Entity returnEntity = null;
        
        int len = childrenList.size();
        for(int i = 0; i < len; i++){
            
            Entity entity = childrenList.get(i);
            if(entity.getEntityID() == entityId){
                returnEntity = entity;
            } else if(entity.hasChildren()){
                returnEntity = findEntityId(entity.getChildren(), entityId);
            }
            if(returnEntity != null)
                break;    			
            
        }
            
        return returnEntity;
    }
    
}
