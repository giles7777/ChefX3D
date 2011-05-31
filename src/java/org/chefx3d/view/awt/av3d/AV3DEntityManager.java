/****************************************************************************
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
import java.awt.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

import org.j3d.aviatrix3d.*;

// Local Imports
import org.chefx3d.model.*;

import org.chefx3d.ui.LoadingProgressListener;
import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.common.EntityWrapper;

/**
 *  Handler class for the Entity Models. Listens for entity events and makes
 *  sure the scene is updated accordingly.
 *
 * @author jonhubba
 * @version $Revision: 1.96 $
 */
class AV3DEntityManager
    implements
        NodeUpdateListener,
        EntityChildListener,
        AV3DConstants{

    /** Reference to the scene management observer created in AV3DView */
    private SceneManagerObserver mgmtObserver;

    /** Array list of entity models to add to the scene */
    private ArrayList<Node> entityToAddList;

    /** Array list of entity models to remove from the scene */
    private ArrayList<Node> entityToRemoveList;

    /** Array list of wrappers scheduled to remove */
    private ArrayList<AV3DEntityWrapper> wrapperList;

    /** Map of the av3d wrappers in the scene */
    private HashMap<Integer, AV3DEntityWrapper> av3dWrapperMap;

    /** Map of the entity wrappers in the scene */
    private HashMap<Integer, EntityWrapper> entityWrapperMap;

    /** Map of the segment entity wrappers in the scene */
    private HashMap<Integer, SegmentEntityWrapper> segmentWrapperMap;

    /** The world model */
    private WorldModel model;

    /** Reporter instance for handing out errors */
    private ErrorReporter reporter;
    
    /** The root Entity for this manager */
    private Entity rootEntity;

    /** The SegmentableEntity for this manager */
    private SegmentableEntity multisegment;

    /** The ContentContainerEntity for this manager */
    private ContentContainerEntity content;

    /** The EnvironmentEntity for this manager */
    private EnvironmentEntity environment;

    /** The root Group for this manager */
    private Group rootGroup;

    /** The filter to use for url requests, null use baseURL logic instead */
    private URLFilter urlFilter;

    /** Flag indicating the enabled state of shadowed entities */
    private boolean shadowEntityEnabled;

    /** Manager class for segment entities */
    private MultisegmentManager segmentManager;

    /** is debugging enabled? */
    private boolean debug;
    
    /** A progress bar notification */
    private LoadingProgressListener progressListener;

	/** Flag indicating that entities have been added or removed */
	private boolean hierarchyHasChanged;
	
    /**
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param model The WorldModel
     * @param rootEntity The Entity that is our root
     * @param rootGroup The Group node that is our root
     * @param urlFilter The filter to use for resource loading
     * @param reporter The instance to use or null
     */
    AV3DEntityManager(
        SceneManagerObserver mgmtObserver,
        WorldModel model,
        Entity rootEntity,
        Group rootGroup,
        URLFilter urlFilter,
        LoadingProgressListener progressListener, 
        ErrorReporter reporter){

        this.model = model;
        this.mgmtObserver = mgmtObserver;
        this.rootEntity = rootEntity;
        this.rootGroup = rootGroup;
        this.urlFilter = urlFilter;
        this.progressListener = progressListener;

        this.reporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();

        shadowEntityEnabled = true;

        this.rootEntity.addEntityChildListener(this);
        
        if (this.rootEntity instanceof EnvironmentEntity) {
            // the assumption is that the root is a Location
            // and the Location implements Environment
            environment = (EnvironmentEntity)rootEntity;
        }

        entityWrapperMap = new HashMap<Integer, EntityWrapper>();
        av3dWrapperMap = new HashMap<Integer, AV3DEntityWrapper>();
        segmentWrapperMap = new HashMap<Integer, SegmentEntityWrapper>();

        segmentManager = new MultisegmentManager(
            mgmtObserver,
            model,
            rootGroup,
            segmentWrapperMap);

        entityToAddList = new ArrayList<Node>();
        entityToRemoveList = new ArrayList<Node>();
        
        wrapperList = new ArrayList<AV3DEntityWrapper>();
        
        // visible vertices in debug mode
        debug = (Boolean)ApplicationParams.get(ApplicationParams.DEBUG_MODE);

    }

    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds. Generally speaking
     * it is assumed in most cases that the src Object passed in is a
     * SharedNode and is generally treated like one.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {

        if (src == rootGroup) {

            int numToRemove = entityToRemoveList.size();
            if (numToRemove > 0) {
                for(int i = 0; i < numToRemove; i++) {
                    Node node = entityToRemoveList.get(i);
                    rootGroup.removeChild(node);
                }
                entityToRemoveList.clear();
            }

            int numToAdd = entityToAddList.size();
            if (numToAdd > 0) {
                for(int i = 0; i < numToAdd; i++) {
                    Node node = entityToAddList.get(i);
                    rootGroup.addChild(node);
                }
                entityToAddList.clear();
            }
        }
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
    }

    //----------------------------------------------------------
    // EntityChildListener Methods
    //----------------------------------------------------------

    /**
     * Adds the child to the parent and then starts the model loading process.
     *
     * @param parent Entity ID of parent
     * @param child Entity ID of child
     */
    public void childAdded(int parent, int child) {
		
        Entity parentEntity = model.getEntity(parent);
        Entity childEntity =
            parentEntity.getChildAt(parentEntity.getChildIndex(child));

        addModel(parentEntity, childEntity);

        recursiveAdd(childEntity);
		hierarchyHasChanged = true;
    }

    /**
     * Add the child at the specific location in the list of parent object
     * children.
     *
     * @param parent Entity ID of parent
     * @param child Entity ID of child
     * @param index index to add the child to in the parent child list
     */
    public void childInsertedAt(int parent, int child, int index) {
        childAdded(parent, child);
    }

    /**
     * Removes the child from the parent. The request to remove the model is
     * made and on the next render pass it will be removed from the scene.
     *
     * @param parent Entity ID of the parent
     * @param child Entity ID of the child
     */
    public void childRemoved(int parent, int child) {
		
        AV3DEntityWrapper wrapper = av3dWrapperMap.get(child);
        if (wrapper != null) {
            wrapperList.clear();
            wrapperList.add(wrapper);
            getChildrenWrappers(wrapper.entity, wrapperList);
            
            int num = wrapperList.size();
            for (int i = num - 1; i >= 0; i--) {
                wrapper = wrapperList.get(i);                
                wrapper.entity.removeEntityChildListener(this);
                
                if (wrapper instanceof SegmentEntityWrapper) {
                    
                    segmentWrapperMap.remove(child);
                    segmentManager.removeChild(wrapper);
                    
                } else if (wrapper.entity instanceof VertexEntity) {
                    
                    SegmentableEntity parentEntity =
                        (SegmentableEntity)model.getEntity(parent);
                    
                    ArrayList<VertexEntity> vertices = parentEntity.getVertices();
                    if ((vertices != null) && (vertices.size() == 1)) {
                        AV3DEntityWrapper onlyVertexWrapper =
                            av3dWrapperMap.get(vertices.get(0).getEntityID());
                        onlyVertexWrapper.setTransparency(1.0f);
                        
                    }
				}
				int pid = wrapper.entity.getParentEntityID();
				if (av3dWrapperMap.containsKey(pid)) {
					
					AV3DEntityWrapper parentWrapper = av3dWrapperMap.get(pid);
					parentWrapper.removeChild(wrapper);
					
				} else {
					
					entityToRemoveList.add(wrapper.sharedNode);
					
					mgmtObserver.requestBoundsUpdate(rootGroup, this);
				}
				
                av3dWrapperMap.remove(wrapper.entity.getEntityID());
                entityWrapperMap.remove(wrapper.entity.getEntityID());
                wrapper.dispose();
            }
            wrapperList.clear();
			hierarchyHasChanged = true;
        } 
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Load the model geometry either from file or dynamically into the scene.
     * Do this by getting the AV3D nodes and then requesting a boundsUpdate
     * from the SceneManagerObserver if the scene is live, otherwise directly
     * update the scene. All entities to be added to the scene are placed in
     * the entityToAdd stack to await processing in the boundsUpdate routine.
     *
     * @param parentEntity Parent entity to add the entity to, can be null
     * @param entity Entity whose model should be added to the scene
     */
    private void addModel(Entity parentEntity, Entity entity) {
        
        int entityID = entity.getEntityID();
        int type = entity.getType();
        int parentType = parentEntity.getType();
		
        if (type == Entity.TYPE_CONTENT_ROOT) {

            // a container entity that has no model
            // or geometry directly associated

            content = (ContentContainerEntity)entity;
            entity.addEntityChildListener(this);

        } else if (type == Entity.TYPE_MULTI_SEGMENT) {

            // a container entity that has no model
            // or geometry directly associated

            multisegment = (SegmentableEntity)entity;
            segmentManager.setSegmentableEntity(multisegment);
            entity.addEntityChildListener(this);

        } else if (type == Entity.TYPE_VERTEX) {

            AV3DEntityWrapper wrapper = new VertexEntityWrapper(
                mgmtObserver, 
                (VertexEntity)entity, 
                reporter);

            wrapper.transformGroup.setUserData(entity);
            
            ArrayList<VertexEntity> vertexList =
                ((SegmentableEntity)parentEntity).getVertices();
            
            if (vertexList.size() == 1) {
                wrapper.setTransparency(1.0f);
                
            } else if ((vertexList.size() - 1) == 1) {
                
                AV3DEntityWrapper firstVertexWrapper =
                    av3dWrapperMap.get(((SegmentableEntity)parentEntity).getStartVertexID());
                
                if (firstVertexWrapper != null && !debug) 
                    firstVertexWrapper.setTransparency(0.0f);
            }
            
            if (debug) {
                wrapper.setTransparency(1.0f);
            }
            
            
            entityToAddList.add(wrapper.sharedNode);
            av3dWrapperMap.put(entityID, wrapper);
			entityWrapperMap.put(entityID, wrapper);
			
			mgmtObserver.requestBoundsUpdate(rootGroup, this);

        } else if ((type == Entity.TYPE_SEGMENT) &&
            (parentType == Entity.TYPE_MULTI_SEGMENT)) {
            
            SegmentEntityWrapper wrapper = new SegmentEntityWrapper(
                model,
                mgmtObserver,
                (SegmentableEntity)parentEntity,
                (SegmentEntity)entity,
                environment,
                segmentManager, 
                reporter);
			wrapper.updateSegment();

            wrapper.transformGroup.setUserData(entity);

            Boolean shadowState = (Boolean)entity.getProperty(
                entity.getParamSheetName(),
                Entity.SHADOW_ENTITY_FLAG);
            
            if ((shadowState != null) && shadowState) {
                wrapper.setTransparency(.5f);
            }

            av3dWrapperMap.put(entityID, wrapper);
			entityWrapperMap.put(entityID, wrapper);
            segmentWrapperMap.put(entityID, wrapper);

            segmentManager.addChild(wrapper);

            entity.addEntityChildListener(this);

        } else if (entity.isZone()) {

            if (type == Entity.TYPE_GROUNDPLANE_ZONE) {
                FloorEntityWrapper wrapper = 
                    new FloorEntityWrapper(
                            mgmtObserver,
                            (PositionableEntity)entity, 
                            environment,
                            reporter);

                wrapper.transformGroup.setUserData(entity);

                entityToAddList.add(wrapper.sharedNode);

                av3dWrapperMap.put(entityID, wrapper);
				entityWrapperMap.put(entityID, wrapper);
                    
                mgmtObserver.requestBoundsUpdate(rootGroup, this);

                entity.addEntityChildListener(this);   
            
            } else if (type == Entity.TYPE_MODEL_ZONE) {
            	
            	ProductZoneEntityWrapper wrapper = 
            		new ProductZoneEntityWrapper(
            				mgmtObserver, 
            				(PositionableEntity)entity, 
            				reporter);
            	
            	wrapper.transformGroup.setUserData(entity);
            	
            	av3dWrapperMap.put(entityID, wrapper);
				entityWrapperMap.put(entityID, wrapper);
            	
            	AV3DEntityWrapper parentWrapper = (AV3DEntityWrapper)
            		av3dWrapperMap.get(parentEntity.getEntityID());
            		
            	if (parentWrapper != null)
            		parentWrapper.addChild(wrapper);
            	
            	entity.addEntityChildListener(this);
            	
            } else {
                                
                ZoneEntityWrapper wrapper = new ZoneEntityWrapper(
                        mgmtObserver,
                        (PositionableEntity)entity,
                        urlFilter, 
                        progressListener, 
                        reporter);
                wrapper.transformGroup.setUserData(entity);
                
                av3dWrapperMap.put(entityID, wrapper);
				entityWrapperMap.put(entityID, wrapper);

                entity.addEntityChildListener(this);

                int parentID = parentEntity.getEntityID();
                if (av3dWrapperMap.containsKey(parentID)) {
                    // models may be parented by the floor,
                    // segments, or other models
                    AV3DEntityWrapper parentWrapper = av3dWrapperMap.get(parentID);
                    parentWrapper.addChild(wrapper);
                    
                } else {
                    // rem: should this validate the parent, 
                    // rather than just parenting to the location?
                    entityToAddList.add(wrapper.sharedNode);
                    
                    mgmtObserver.requestBoundsUpdate(rootGroup, this);
                }
            }
			
        } else if (entity.isModel() ||
        		type == Entity.TYPE_TEMPLATE_CONTAINER) {

            AV3DEntityWrapper wrapper = null;

			boolean isShadow = false;
			Object isShadowProp = entity.getProperty(
				entity.getParamSheetName(),
				Entity.SHADOW_ENTITY_FLAG);

			if ((isShadowProp != null) && (isShadowProp instanceof Boolean)) {
				isShadow = ((Boolean)isShadowProp).booleanValue();
			}
			
			boolean isExtrusion = false;
			Object isExtrusionProp = entity.getProperty(
				entity.getParamSheetName(),
				ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);
			
			if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
				isExtrusion = ((Boolean)isExtrusionProp).booleanValue();
			}
			
			if (isExtrusion) {
				if (!isShadow || (isShadow && shadowEntityEnabled)) {
					wrapper = new ExtrusionEntityWrapper(
	                    mgmtObserver,
	                    (PositionableEntity)entity,
	                    urlFilter, 
	                    progressListener, 
	                    reporter);
				}
			} else if (isShadow) {
                if (shadowEntityEnabled) {
                    wrapper = new ShadowEntityWrapper(
                        mgmtObserver,
                        (PositionableEntity)entity,
                        urlFilter, 
                        progressListener, 
                        reporter);
                }
            } else {
                wrapper = new AV3DEntityWrapper(
                    mgmtObserver,
                    (PositionableEntity)entity,
                    urlFilter, 
                    progressListener, 
                    reporter);
            }

            if (wrapper != null) {
				if (isShadow) {
					wrapper.transformGroup.setUserData("Shadow");
				} else {
					wrapper.transformGroup.setUserData(entity);
				}
                av3dWrapperMap.put(entityID, wrapper);
				entityWrapperMap.put(entityID, wrapper);

                entity.addEntityChildListener(this);
                
                int parentID = parentEntity.getEntityID();
                if (av3dWrapperMap.containsKey(parentID)) {
                    // models may be parented by the floor,
                    // segments, or other models
                    AV3DEntityWrapper parentWrapper = av3dWrapperMap.get(parentID);
                    parentWrapper.addChild(wrapper);
                    
                } else {
                    // rem: should this validate the parent, 
                    // rather than just parenting to the location?
                    entityToAddList.add(wrapper.sharedNode);
					
					mgmtObserver.requestBoundsUpdate(rootGroup, this);
                }
            }
		}
    }

    /**
     * Cleanup all
     */
    void clear() {
        
        if (rootEntity != null) {
            rootEntity.removeEntityChildListener(this);
            rootEntity = null;
        }
        if (multisegment != null) {
            multisegment.removeEntityChildListener(this);
            multisegment = null;
        }
        if (content != null) {
            content.removeEntityChildListener(this);
            content = null;
        }
        for (Iterator<AV3DEntityWrapper> i = av3dWrapperMap.values().iterator();
            i.hasNext();) {
            
            AV3DEntityWrapper wrapper = i.next();
            wrapper.entity.removeEntityChildListener(this);
            wrapper.dispose();
        }
                
        segmentManager.dispose();
        segmentManager = null;
        
        av3dWrapperMap.clear();
        entityWrapperMap.clear();
        segmentWrapperMap.clear();
    }

    /**
     * Return the entity wrapper map
     *
     * @return The entity wrapper map
     */
    HashMap<Integer, EntityWrapper> getEntityWrapperMap() {
        return(entityWrapperMap);
    }

    /**
     * Return the entity wrapper map
     *
     * @return The entity wrapper map
     */
    HashMap<Integer, AV3DEntityWrapper> getAV3DEntityWrapperMap() {
        return(av3dWrapperMap);
    }

    /**
     * Return the segment entity wrapper map
     *
     * @return The segment entity wrapper map
     */
    HashMap<Integer, SegmentEntityWrapper> getSegmentEntityWrapperMap() {
        return(segmentWrapperMap);
    }

    /**
     * Return the SegmentableEntity in use
     *
     * @return The SegmentableEntity in use
     */
    SegmentableEntity getSegmentableEntity() {
        return(multisegment);
    }

    /**
     * Return the MultisegmentManager
     *
     * @return The MultisegmentManager
     */
    MultisegmentManager getMultisegmentManager() {
        return(segmentManager);
    }

    /**
     * Return the ContentContainerEntity in use
     *
     * @return The ContentContainerEntity in use
     */
    ContentContainerEntity getContentContainerEntity() {
        return(content);
    }

    /**
     * Return the EnvironmentEntity in use
     *
     * @return The CEnvironmentEntity in use
     */
    EnvironmentEntity getEnvironmentEntity() {
        return(environment);
    }

    /**
     * Enable the use of shadowed entities
     *
     * @param state The enabled state of shadowed entities
     */
    void setShadowEntityEnabled(boolean state) {
        shadowEntityEnabled = state;
    }
    
	/**
	 * Return whether the scene has changed as a result of entities 
	 * coming or going since the last time this method was called
	 *
	 * @return true if the entity hierarchy has changed, false if not
	 */
	boolean hasHierarchyChanged() {
		if (hierarchyHasChanged) {
			hierarchyHasChanged = false;
			return(true);
		} else {
			return(false);
		}
	}
	
    /**
     * Walk through the children of the argument entity,
     * adding scene graph elements as necessary.
     *
     * @param parent The entity to start with
     */
    private void recursiveAdd(Entity parent) {
        if (parent.hasChildren()) {
            ArrayList<Entity> childList = parent.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                addModel(parent, child);
                recursiveAdd(child);
            }
        }
    }
    
    /**
     * In preparation for a remove, walk through the
     * children of the argument entity and collect an 
     * ordered list of wrappers that should be removed.
     *
     * @param parent The entity to start with
     * @param wrapperList The list of wrappers to append to
     */
    private void getChildrenWrappers(
        Entity parent, 
        ArrayList<AV3DEntityWrapper> wrapperList) {
        
        if (parent.hasChildren()) {
            ArrayList<Entity> childList = parent.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Entity child = childList.get(i);
                AV3DEntityWrapper wrapper = 
                    av3dWrapperMap.get(child.getEntityID());
                if (wrapper != null) {
                    wrapperList.add(wrapper);
                    getChildrenWrappers(child, wrapperList);
                }
            }
        }
    }
}

