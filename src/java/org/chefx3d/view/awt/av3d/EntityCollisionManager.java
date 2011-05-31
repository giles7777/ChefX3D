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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.AddEntityChildTransientCommand;
import org.chefx3d.model.AddEntityCommand;
import org.chefx3d.model.ChangePropertyCommand;
import org.chefx3d.model.ChangePropertyTransientCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.ContentContainerEntity;
import org.chefx3d.model.Entity;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.MoveEntityCommand;
import org.chefx3d.model.MoveEntityTransientCommand;
import org.chefx3d.model.MoveSegmentCommand;
import org.chefx3d.model.MoveSegmentTransientCommand;
import org.chefx3d.model.MoveVertexCommand;
import org.chefx3d.model.MoveVertexTransientCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.RemoveEntityCommand;
import org.chefx3d.model.RemoveSegmentCommand;
import org.chefx3d.model.RotateEntityCommand;
import org.chefx3d.model.RotateEntityTransientCommand;
import org.chefx3d.model.RuleDataAccessor;
import org.chefx3d.model.ScaleEntityCommand;
import org.chefx3d.model.ScaleEntityTransientCommand;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;
import org.chefx3d.view.common.DefaultSurrogateEntityWrapper;
import org.chefx3d.view.common.EntityWrapper;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;
import org.chefx3d.view.common.RuleCollisionChecker;
import org.chefx3d.view.boundingbox.SegmentBoundingBox;
import org.chefx3d.view.common.SurrogateEntityWrapper;
import org.j3d.aviatrix3d.TransformGroup;

/**
 * Manager for rule based collision detection.
 *
 * @author Rex Melton
 * @version $Revision: 1.70 $
 */
class EntityCollisionManager implements RuleCollisionChecker {

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /** The world model */
    private WorldModel model;

    /** The error reporter to log to */
    private ErrorReporter reporter;

    /** Model loader */
    private AV3DLoader modelLoader;

    /** Utility to resolve urls */
    private FileLoader urlResolver;

    /** Working objects */
    private Matrix4f activeMatrix;

    /** Scratch objects used for transform calculations */
    private Vector3f translation;
    private AxisAngle4f rotation;
    private Point3f pnt;
    
    private double[] pos_array;
    private float[] rot_array;
    private float[] scl_array;
    private float[] size_array;
    private float[] vtx0;
    private float[] vtx1;

    /** Bound extents */
    private float[] min;
    private float[] max;

    /** The bounds of the entity being manipulated */
    private OrientedBoundingBox activeBounds;
    
    /** Handler for all segment bounds */
    private SegmentBoundingBox seg_bb;
    
    /** Scratch bounds used during intersection testing */
    private OrientedBoundingBox obb;
    
    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f mtx;
    
    /** The manager of the entities to be handled */
    private AV3DEntityManager entityManager;
    
    /** The map of entity wrappers */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;
    
	/** Bounds to geometry intersection testing */
	private IntersectionUtils isect;
    
    /** The map of surrogate entity wrappers */
    private HashMap<Integer, SurrogateEntityWrapper> surrogateMap;
	
    /** Map entity wrappers of various sorts */
	private HashMap<Integer, EntityWrapper> activeWrapperMap;
	
	/** Stack of matrix objects */
	private ArrayList<Matrix4f> mtx_list;
	
	/** Flag indicating that target surrogates are enabled */
	private boolean enableSurrogates;
	
	/** The collision source surrogate */
	private CollisionSurrogate sourceSurrogate;
	
	/** 
	 * Map of entity to the original SurrogateEntityWrapper state matching that
	 * entity. This is a copy of the original SurrogateEntityWrapper as we have
	 * to change it for evaluating surrogates when temporary surrogates are 
	 * added.
	 */
	private Map<PositionableEntity, SurrogateEntityWrapper> 
		tempSurrogateOriginalMap;
	
    /**
     * Package visibility Constructor
     *
     * @param model The WorldModel
     * @param mgmtObserver The SceneManagerObserver
     * @param reporter The ErrorReporter instance to use
     */
    EntityCollisionManager(
        WorldModel worldModel,
        SceneManagerObserver sceneMgmtObserver,
        ErrorReporter errorReporter) {

        model = worldModel;
        mgmtObserver = sceneMgmtObserver;
        reporter = (errorReporter != null) ? 
            errorReporter : DefaultErrorReporter.getDefaultReporter();

        modelLoader = new AV3DLoader();
        urlResolver = new FileLoader();

        tu = new TransformUtils();
        mtx = new Matrix4f();
        pnt = new Point3f();
        
        activeMatrix = new Matrix4f();
        translation = new Vector3f();
        rotation = new AxisAngle4f();

        pos_array = new double[3];
        rot_array = new float[4];
        scl_array = new float[3];
        size_array = new float[3];
        vtx0 = new float[3];
        vtx1 = new float[3];

        min = new float[3];
        max = new float[3];
        
        obb = new OrientedBoundingBox();
        seg_bb = new SegmentBoundingBox();
        		
		isect = new IntersectionUtils();
		
		surrogateMap = new HashMap<Integer, SurrogateEntityWrapper>();
		activeWrapperMap = new HashMap<Integer, EntityWrapper>();
		
		mtx_list = new ArrayList<Matrix4f>();
		
		sourceSurrogate = new CollisionSurrogate();
		
		tempSurrogateOriginalMap = 
			new HashMap<PositionableEntity, SurrogateEntityWrapper>();
    }

    //----------------------------------------------------------
    // Methods define by RuleCollisionChecker
    //----------------------------------------------------------

    /**
     * Submit a command to the collision detection system for
     * processing and return a list of Entities that the
     * Command's Entity is in collision with. If no collisions
     * are occurring, an empty list is returned.
     *
     * @param command The Command to test
     * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
     * @return The list of Entities in collision.
     */
    public ArrayList<Entity> submitCommand(
    		Command command,
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds) {
    	
		return(
				submitCommand(
						command, 
						true, 
						useEntityExtendedBounds, 
						useTargetsExtendedBounds));
	}

	/** 
	 * Submit a command to the collision detection system for
	 * processing and return a list of Entities that the
	 * Command's Entity is in collision with. If no collisions
	 * are occurring, null is returned.
	 *
	 * @param command The Command to test
	 * @param useSurrogates Flag indicating that surrogate entities should
	 * be used during collision testing.
	 * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
	 * @return The list of Entities in collision, or null.
	 */
	public ArrayList<Entity> submitCommand(
			Command command, 
			boolean useSurrogates,
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds) {
    
        ArrayList<Entity> results = new ArrayList<Entity>();

		enableSurrogates = useSurrogates;
		
        if (command instanceof RuleDataAccessor) {
            
            RuleDataAccessor rda = (RuleDataAccessor)command;
            Entity entity = rda.getEntity();

            if(!validate(command, entity)) {
                return(results);
            }
            
			boolean sourceConfigured = configSourceSurrogate(
				command, 
				entity, 
				useEntityExtendedBounds);
			
			boolean sourceIsTransformable = false;
			if (sourceConfigured) {
				
				initActiveWrapperMap();
				sourceIsTransformable = getTransformToRoot(sourceSurrogate, mtx);
				
				if (sourceIsTransformable) {
					activeBounds.transform(mtx);
				}
			}
			if (!sourceConfigured || !sourceIsTransformable) {
				return(results);
			}
			
            int entityID = entity.getEntityID();
			
			activeWrapperMap.put(entityID, sourceSurrogate);
			transformBounds();
			
			boolean sourceIsZone = entity.isZone();
			
		    Boolean dbg = (Boolean)ApplicationParams.get(ApplicationParams.DEBUG_MODE);
		    boolean debug = false;
		    if (dbg != null) {
		        debug = dbg;
		    }

			if (debug) {
				System.out.println("******** Collision set *********");
			}
           
            for (Iterator<Integer> i = activeWrapperMap.keySet().iterator();
                i.hasNext();) {
                
                int id = i.next();
                if (id != entityID) {
					
					EntityWrapper targetWrapper = activeWrapperMap.get(id);
					Entity targetEntity = targetWrapper.getEntity();
					
					boolean targetIsZone = targetEntity.isZone();
					
					OrientedBoundingBox bounds = null;
					if (useTargetsExtendedBounds) {
						bounds = targetWrapper.getExtendedBounds();
					} else {
                    	bounds = targetWrapper.getBounds();
					}
					AV3DEntityWrapper av3dWrapper = null;
					if (targetWrapper instanceof AV3DEntityWrapper) {
						av3dWrapper = (AV3DEntityWrapper)targetWrapper;
                    	//tu.getLocalToVworld(av3dWrapper.transformGroup, mtx);
                    	//bounds.transform(mtx);
					}
                    
					boolean useEpsilon = sourceIsZone | targetIsZone;
                    if (activeBounds.intersect(bounds, useEpsilon)) {
						
						boolean intersectionFound = true;
						if (targetIsZone) {
							// check bounds against zone geometry
							intersectionFound = isect.check(activeBounds, av3dWrapper, useEpsilon);
							if (debug) {
								System.out.println("Intersection: "+ getIdentifier(targetEntity)
									+": "+ intersectionFound);
							}
						}
						if (intersectionFound) {
							if (debug) {
								System.out.println("Collision: "+ getIdentifier(targetEntity));
							}
							results.add(targetEntity);
						}
                    }
                }
            }
        }
        return(results);
    }

	/////////////////////////////////////////////////////////////////////////////////////
	// test for the extended method
	/*
	public ArrayList<Entity> submitCommand(Command command) {
		if (command instanceof RuleDataAccessor) {
            
            RuleDataAccessor rda = (RuleDataAccessor)command;
            Entity entity = rda.getEntity();

			Map<Entity, ArrayList<Entity>> result_map = submitCommandExtended(command);
			
			ArrayList<Entity> result_list = result_map.get(entity);
			return(result_list);
		} else {
			return(new ArrayList<Entity>());
		}
	}
	*/
	/////////////////////////////////////////////////////////////////////////////////////

    /**
     * Submit a command to the collision detection system for
     * processing. A Map will be returned that contains a list of
	 * colliding Entities for the Entity that is the subject of the
	 * Command, as well as for any children Entities of the Command
	 * Entity. If no collisions are found the list will be empty for
	 * that Entity.
     *
     * @param command The Command to test
     * @param useSurrogates True to use surrogates, false otherwise
     * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 * @param useTargetsExtendedBounds Flag indicating that the extended bounds 
	 * of the target entities in the scene, if set, should be used when checked
	 * against
     * @return The Map of Entities in collision.
     */
	public Map<Entity, ArrayList<Entity>> submitCommandExtended(
			Command command,
			boolean useSurrogates,
			boolean useEntityExtendedBounds,
			boolean useTargetsExtendedBounds) {

        HashMap<Entity, ArrayList<Entity>> results = 
			new HashMap<Entity, ArrayList<Entity>>();

		enableSurrogates = useSurrogates;
		
        if (command instanceof RuleDataAccessor) {
            
            RuleDataAccessor rda = (RuleDataAccessor)command;
            Entity entity = rda.getEntity();

            if(!validate(command, entity)) {
                return(results);
            }
            
            Boolean dbg = (Boolean)ApplicationParams.get(ApplicationParams.DEBUG_MODE);
            boolean debug = false;
            if (dbg != null) {
                debug = dbg;
            }
            
			boolean sourceConfigured = configSourceSurrogate(
				command, 
				entity, 
				useEntityExtendedBounds);
			
			boolean sourceIsTransformable = false;
			if (sourceConfigured) {
				
				initActiveWrapperMap();
				sourceIsTransformable = getTransformToRoot(sourceSurrogate, mtx);
				
				if (sourceIsTransformable) {
					activeBounds.transform(mtx);
				}
			}
			if (!sourceConfigured || !sourceIsTransformable) {
				return(results);
			}
			
            int entityID = entity.getEntityID();
			
			activeWrapperMap.put(entityID, sourceSurrogate);
			transformBounds();
			
			ArrayList<Entity> sourceList = new ArrayList<Entity>();
			sourceList.add(entity);
			getChildren(entity, sourceList);
			
			for (int x = 0; x < sourceList.size(); x++) {
				
				Entity sourceEntity = sourceList.get(x);
				OrientedBoundingBox sourceBounds = null;
				
				if (sourceEntity == entity) {
					sourceBounds = activeBounds;
				} else {
					int sourceID = sourceEntity.getEntityID();
					EntityWrapper sourceWrapper = activeWrapperMap.get(sourceID);
					if (sourceWrapper == null) {
					    continue;
					}
					sourceBounds = sourceWrapper.getBounds();
				}
				
				boolean sourceIsZone = sourceEntity.isZone();
				
				ArrayList<Entity> collisionList = new ArrayList<Entity>();
				results.put(sourceEntity, collisionList);
				
				for (Iterator<EntityWrapper> i = activeWrapperMap.values().iterator(); 
					i.hasNext();) {
					
					EntityWrapper targetWrapper = i.next();
					Entity targetEntity = targetWrapper.getEntity();
					if (targetEntity != sourceEntity) {
						
						if (results.containsKey(targetEntity)) {
							// this entity has already been tested for
							// collisions skip the intersection test 
							// and check the previous results.
							ArrayList<Entity> list = results.get(targetEntity);
							if (list.contains(sourceEntity)) {
								collisionList.add(targetEntity);
							}
							continue;
						}
						
						boolean targetIsZone = targetEntity.isZone();
							
						OrientedBoundingBox bounds = null;
						if (useTargetsExtendedBounds) {
							bounds = targetWrapper.getExtendedBounds();
						} else {
	                    	bounds = targetWrapper.getBounds();
						}
						
	                    AV3DEntityWrapper av3dWrapper = null;
	                    if (targetWrapper instanceof AV3DEntityWrapper) {
	                        av3dWrapper = (AV3DEntityWrapper)targetWrapper;
	                        //tu.getLocalToVworld(av3dWrapper.transformGroup, mtx);
	                        //bounds.transform(mtx);
						}
						
						boolean useEpsilon = sourceIsZone | targetIsZone;
						if (sourceBounds.intersect(bounds, useEpsilon)) {
							
							boolean intersectionFound = true;
							if (targetIsZone) {
								// check bounds against zone geometry
								intersectionFound = isect.check(sourceBounds, av3dWrapper, useEpsilon);
								if (debug) {
									System.out.println("Intersection:  src = "+ getIdentifier(sourceEntity) +
										", target = "+ getIdentifier(targetEntity) +": "+ intersectionFound);
								}
							}
							if (intersectionFound) {
								if (debug) {
									System.out.println("Collision: src = "+ getIdentifier(sourceEntity) +
										", target = "+ getIdentifier(targetEntity));
								}
								collisionList.add(targetEntity);
							}
						}
					}
				}
			}
        }
        return(results);
    }

    /** 
     * Print the current active bounds and collision data
     */
    public void printState() {
        
        System.out.println(activeBounds.toString());
        System.out.println("    position: " + java.util.Arrays.toString(pos_array));
        System.out.println("    rot_array: " + java.util.Arrays.toString(rot_array));
        System.out.println("    scl_array: " + java.util.Arrays.toString(scl_array));
        
    }

	/**
	 * Add a surrogate to the working set
	 *
	 * @param surrogate The SurrogateEntityWrapper to add
	 */
	public void addSurrogate(SurrogateEntityWrapper surrogate) {
		
		Entity sur_entity = surrogate.getEntity();
		int id = sur_entity.getEntityID();
		surrogateMap.put(id, surrogate);
	}
	
	/**
	 * Remove a surrogate from the working set
	 *
	 * @param surrogate The SurrogateEntityWrapper to remove
	 */
	public void removeSurrogate(SurrogateEntityWrapper surrogate) {
		
		int id = surrogate.getEntity().getEntityID();
		surrogateMap.remove(id);
	} 
	
	/**
	 * Clear the surrogate working set
	 */
	public void clearSurrogates() {
		
		clearSidePocketedOriginalSurrogateStates();
		surrogateMap.clear();
		
	}
	
	/**
	 * Print out a formatted list of the surrogates in existence.
	 */
	public void printSurrogates() {
		
		Object[] keys = surrogateMap.keySet().toArray();
		double[] pos = new double[3];
		float[] rot = new float[4];
		float[] scl = new float[3];
		
		System.out.println(">>> Surrogate Map Contents");
		
		for (int i = 0; i < keys.length; i++) {
			
			Integer key = (Integer) keys[i];
			
			SurrogateEntityWrapper surrogate = 
				surrogateMap.get(key);
			
			
			surrogate.getPosition(pos);
			surrogate.getRotation(rot);
			surrogate.getScale(scl);
			
			System.out.println(">>>>>> "+i+
					") Entity: "+surrogate.getEntity().getName()+
					" ID: "+surrogate.getEntity().getEntityID()+
					" ParentEntity: "+surrogate.getParentEntity().getName()+
					" pos: "+Arrays.toString(pos)+
					" rot: "+Arrays.toString(rot)+
					" scl: "+Arrays.toString(scl));
		}
		
		System.out.println(">>> DONE Printing Surrogate Map Contents");
	}
	
	/**
	 * Return the EntityWrapper for the specified entity. If a surrogate
	 * is configured for the entity, it is returned in preference to the
	 * rendering wrapper. If no wrapper is found, null is returned.
	 *
	 * @param entityID The id of the entity
	 */
	public EntityWrapper getEntityWrapper(int entityID) {
		EntityWrapper wrapper = surrogateMap.get(entityID);
		if (wrapper == null) {
			wrapper = surrogateMap.get(entityID);
		}
		return(wrapper);
	}
	
	/**
	 * Return the current surrogate set.
	 *
	 * @return The current surrogate set.
	 */
	public SurrogateEntityWrapper[] getSurrogates() {
		int num_surrogates = surrogateMap.size();
		SurrogateEntityWrapper[] wrappers = new SurrogateEntityWrapper[num_surrogates];
		if (num_surrogates > 0) {
			surrogateMap.values().toArray(wrappers);
		}
		return(wrappers);
	}
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
	
    /**
     * Set the active entity manager 
     *
     * @param entityManager The active entity manager 
     */
    void setEntityManager(AV3DEntityManager entityManager) {
        this.entityManager = entityManager;
        if (entityManager != null) {
            wrapperMap = entityManager.getAV3DEntityWrapperMap();
        } else {
            wrapperMap = null;
        }
    }
	
	/** 
	 * Configure the surrogate to be used as the source during intersection testing
	 *
     * @param command The command that is transforming the entity
     * @param entity The entity to create a proxy surrogate for
     * @param useEntityExtendedBounds Flag indicating that the extended bounds
	 * of the entity should be used, if set, when checking against the scene
	 */
    private boolean configSourceSurrogate(
		Command command, 
		Entity entity, 
		boolean useEntityExtendedBounds) {
		
		//////////////////////////////////////////////////////////////////////
		// retrieve (or configure) the initial bounds object 
		
		int entityID = entity.getEntityID();
    	Entity sceneEntity = model.getEntity(entityID);
    	boolean useWrapperMap = true;
    	
    	/*
    	 * BJY: Check the model url of the incoming entity and the one already
    	 * in the scene. If they are not the same then we have a replacement
    	 * for the existing entity and we should not use the side pocketed
    	 * bounds data for the existing entity in the collision check.
    	 */
    	if (sceneEntity != null){
    		
    		String incomingURL = entity.getModelURL();
    		String existingURL = sceneEntity.getModelURL();
    		
    		if (incomingURL == null && existingURL != null){
  
    			useWrapperMap = false;
    			
    		} else if (incomingURL != null && existingURL == null) {
    			
    			useWrapperMap = false;
    			
    		} else if (incomingURL == null && existingURL == null) {
    			
    			useWrapperMap = true;
    			
    		} else if(!incomingURL.equals(existingURL)){

    			useWrapperMap = false;
    		}
    	}

        if (entity instanceof SegmentEntity) {
            // use the shared bounds object from segment handler
            activeBounds = seg_bb;
            
        } else {
        
            activeBounds = obb;
            if (useWrapperMap && wrapperMap.containsKey(entityID)) {
                // the entity already exists, clone it's bounds
                // object to use
                AV3DEntityWrapper wrapper = wrapperMap.get(entityID);
                
                OrientedBoundingBox bounds = null;
                if (useEntityExtendedBounds) {
                	bounds = wrapper.getExtendedBounds();
                } else {
                	bounds = wrapper.getBounds();
                }  

                activeBounds.copy(bounds);
                
            } else {
                if (entity instanceof PositionableEntity) {
                    
                    // a new entity, create a bounds object to use
                    // and queue up a load of it's geometry
                    PositionableEntity pe = (PositionableEntity)entity;
                    
                    float[] minimum_extent = null;
                    Object obj = entity.getProperty(
                        PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
                        PositionableEntity.MINIMUM_EXTENT_PROP);
                    
                    if (obj != null && obj instanceof float[]) {
                        minimum_extent = (float[])obj;
                        if (minimum_extent.length < 3) {
                            minimum_extent = null;
                        }
                    }
                    
                    // get size
                    pe.getSize(size_array);

                    // get scale
                    pe.getScale(scl_array);                        
                    
                    if (minimum_extent != null) {     
                        
                        // compare the current scale to the minimum scale and 
                        // adjust to use larger of the two if necessary                
                        for (int i = 0; i < scl_array.length; i++) {
                            
                            float size = size_array[i] * scl_array[i];                   
                            if (size < minimum_extent[i]) {
                                scl_array[i] = minimum_extent[i] / size_array[i];
                            }
                        }
                    }
                                      
                    max[0] = size_array[0]/2;
                    max[1] = size_array[1]/2;
                    max[2] = size_array[2]/2;
                    
                    min[0] = -max[0];
                    min[1] = -max[1];
                    min[2] = -max[2];
                    
                    activeBounds.setVertices(min, max);
                    activeBounds.setScale(scl_array);
					
					// configure the bounds border if one is specified
					float[] bounds_border = null;
					obj = entity.getProperty(
						PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
						PositionableEntity.BOUNDS_BORDER_PROP);
					if ((obj != null) && (obj instanceof float[])) {
						bounds_border = (float[])obj;
						if (bounds_border.length < 3) {
							bounds_border = null;
						}
					}
					if (bounds_border != null) {
						activeBounds.setBorder(bounds_border);
					}
					
                    // TODO: we could check the cache for the item and load
                    // it.  loading from the network would be too slow.
                    
                } else {
                    System.out.println("ECM: "+ entity +": Not a PositionableEntity");
					return(false);
                }
            }
        }
		//////////////////////////////////////////////////////////////////////
		// configure the surrogate to account for the command 
		
		// find the surrogate for this entity, if one exists
		SurrogateEntityWrapper entity_sur_wrapper = surrogateMap.get(entityID);
		
        TransformGroup parentTransformGroup = null;
        // special case for segments
        if (command instanceof MoveSegmentCommand) {
            
            MoveSegmentCommand msc = (MoveSegmentCommand)command;
            
            SegmentEntity segment = (SegmentEntity)msc.getEntity();
            
            // Get the wall thickness
            Object prop = segment.getProperty(
                    Entity.EDITABLE_PROPERTIES,
                    SegmentEntity.WALL_THICKNESS_PROP);
            
            float wallThickness = SegmentEntity.DEFAULT_WALL_THICKNESS;
            if (prop instanceof ListProperty) {
                ListProperty list = (ListProperty)prop;
                wallThickness = Float.parseFloat(list.getSelectedValue());
            }
            
            VertexEntity ve0 = segment.getEndVertexEntity();
            float height0 = (Float)ve0.getHeight();
            
            VertexEntity ve1 = segment.getStartVertexEntity();
            float height1 = (Float)ve1.getHeight();
            
            float height = Math.max(height0, height1);
            
            SegmentEntityWrapper sew = 
                (SegmentEntityWrapper)wrapperMap.get(segment.getEntityID());
            parentTransformGroup = 
                sew.getSegmentDetails().getParent();
            tu.getLocalToVworld(parentTransformGroup, mtx);
            mtx.invert();
            
            // the start (or left) vertex position
            msc.getStartVertexEndPosition(pos_array);
            vtx0[0] = (float)pos_array[0];
            vtx0[1] = (float)pos_array[1];
            vtx0[2] = (float)pos_array[2];
            pnt.set(vtx0);
            mtx.transform(pnt);
            pnt.get(vtx0);
            
            // the end (or right) vertex position
            msc.getEndVertexEndPosition(pos_array);
            vtx1[0] = (float)pos_array[0];
            vtx1[1] = (float)pos_array[1];
            vtx1[2] = (float)pos_array[2];
            pnt.set(vtx1);
            mtx.transform(pnt);
            pnt.get(vtx1);
            
            // get the local transform for the segment bounds
            seg_bb.update(vtx0, vtx1, height, wallThickness);
            seg_bb.getMatrix(activeMatrix);
            
			///////////////////////////////////////////////////
			activeMatrix.get(translation);
			pos_array[0] = translation.x;
			pos_array[1] = translation.y;
			pos_array[2] = translation.z;
			
			rotation.set(activeMatrix);
			rot_array[0] = rotation.x;
			rot_array[1] = rotation.y;
			rot_array[2] = rotation.z;
			rot_array[3] = rotation.angle;
			
			scl_array[0] = 1;
			scl_array[1] = 1;
			scl_array[2] = 1;
			///////////////////////////////////////////////////
			
        } else if (command instanceof MoveSegmentTransientCommand) {
            
            MoveSegmentTransientCommand mstc = (MoveSegmentTransientCommand)command;
            
            SegmentEntity segment = (SegmentEntity)mstc.getEntity();

            // Get the wall thickness
            Object prop = segment.getProperty(
                    Entity.EDITABLE_PROPERTIES,
                    SegmentEntity.WALL_THICKNESS_PROP);
            
            float wallThickness = SegmentEntity.DEFAULT_WALL_THICKNESS;
            if (prop instanceof ListProperty) {
                ListProperty list = (ListProperty)prop;
                wallThickness = Float.parseFloat(list.getSelectedValue());
            }

            VertexEntity ve0 = segment.getEndVertexEntity();
            float height0 = ve0.getHeight();
            
            VertexEntity ve1 = segment.getStartVertexEntity();
            float height1 = (Float)ve1.getHeight();
            
            float height = Math.max(height0, height1);
            
            SegmentEntityWrapper sew = 
                (SegmentEntityWrapper)wrapperMap.get(segment.getEntityID());
            parentTransformGroup = 
                sew.getSegmentDetails().getParent();
            tu.getLocalToVworld(parentTransformGroup, mtx);
            mtx.invert();
            
            // the start (or left) vertex position
            mstc.getStartVertexEndPosition(pos_array);
            vtx0[0] = (float)pos_array[0];
            vtx0[1] = (float)pos_array[1];
            vtx0[2] = (float)pos_array[2];
            pnt.set(vtx0);
            mtx.transform(pnt);
            pnt.get(vtx0);
            
            // the end (or right) vertex position
            mstc.getEndVertexEndPosition(pos_array);
            vtx1[0] = (float)pos_array[0];
            vtx1[1] = (float)pos_array[1];
            vtx1[2] = (float)pos_array[2];
            pnt.set(vtx1);
            mtx.transform(pnt);
            pnt.get(vtx1);
            
            // get the local transform for the segment bounds
            seg_bb.update(vtx0, vtx1, height, wallThickness);
            seg_bb.getMatrix(activeMatrix);
        
			///////////////////////////////////////////////////
			activeMatrix.get(translation);
			pos_array[0] = translation.x;
			pos_array[1] = translation.y;
			pos_array[2] = translation.z;
			
			rotation.set(activeMatrix);
			rot_array[0] = rotation.x;
			rot_array[1] = rotation.y;
			rot_array[2] = rotation.z;
			rot_array[3] = rotation.angle;
			
			scl_array[0] = 1;
			scl_array[1] = 1;
			scl_array[2] = 1;
			///////////////////////////////////////////////////
			
        } else if (command instanceof RemoveSegmentCommand){
        	
        	RemoveSegmentCommand rsc = (RemoveSegmentCommand)command;
            
            SegmentEntity segment = (SegmentEntity)rsc.getEntity();

            // Get the wall thickness
            Object prop = segment.getProperty(
                    Entity.EDITABLE_PROPERTIES,
                    SegmentEntity.WALL_THICKNESS_PROP);
            
            float wallThickness = SegmentEntity.DEFAULT_WALL_THICKNESS;
            if (prop instanceof ListProperty) {
                ListProperty list = (ListProperty)prop;                
                wallThickness = Float.parseFloat(list.getSelectedValue());
            }

            VertexEntity ve0 = segment.getEndVertexEntity();
            float height0 = ve0.getHeight();
            
            VertexEntity ve1 = segment.getStartVertexEntity();
            float height1 = (Float)ve1.getHeight();
            
            float height = Math.max(height0, height1);
            
            SegmentEntityWrapper sew = 
                (SegmentEntityWrapper)wrapperMap.get(segment.getEntityID());
            parentTransformGroup = 
                sew.getSegmentDetails().getParent();
            tu.getLocalToVworld(parentTransformGroup, mtx);
            mtx.invert();
            
            // the start (or left) vertex position
            segment.getStartVertexEntity().getPosition(pos_array);
            vtx0[0] = (float)pos_array[0];
            vtx0[1] = (float)pos_array[1];
            vtx0[2] = (float)pos_array[2];
            pnt.set(vtx0);
            mtx.transform(pnt);
            pnt.get(vtx0);
            
            // the end (or right) vertex position
            segment.getEndVertexEntity().getPosition(pos_array);
            vtx1[0] = (float)pos_array[0];
            vtx1[1] = (float)pos_array[1];
            vtx1[2] = (float)pos_array[2];
            pnt.set(vtx1);
            mtx.transform(pnt);
            pnt.get(vtx1);
            
            // get the local transform for the segment bounds
            seg_bb.update(vtx0, vtx1, height, wallThickness);
            seg_bb.getMatrix(activeMatrix);
        	
			///////////////////////////////////////////////////
			activeMatrix.get(translation);
			pos_array[0] = translation.x;
			pos_array[1] = translation.y;
			pos_array[2] = translation.z;
			
			rotation.set(activeMatrix);
			rot_array[0] = rotation.x;
			rot_array[1] = rotation.y;
			rot_array[2] = rotation.z;
			rot_array[3] = rotation.angle;
			
			scl_array[0] = 1;
			scl_array[1] = 1;
			scl_array[2] = 1;
			///////////////////////////////////////////////////
			
        } else {
            	
			PositionableEntity pe = (PositionableEntity)entity;
            
            // get the starting transformation
			if (entity_sur_wrapper != null) {
            	entity_sur_wrapper.getPosition(pos_array);
            	entity_sur_wrapper.getRotation(rot_array);
            	entity_sur_wrapper.getScale(scl_array);
			} else {
            	pe.getPosition(pos_array);
            	pe.getRotation(rot_array);
            	pe.getScale(scl_array);
			}
            
            float[] minimum_extent = null;
            Object obj = entity.getProperty(
                PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
                PositionableEntity.MINIMUM_EXTENT_PROP);
            
            if (obj != null && obj instanceof float[]) {
                minimum_extent = (float[])obj;
                if (minimum_extent.length < 3) {
                    minimum_extent = null;
                }
            }
                        
            // overide with the new data from the command
            String propertyName = null;
            Object value = null;
            if (command instanceof MoveEntityCommand) {
                ((MoveEntityCommand)command).getEndPosition(pos_array);
            } else if (command instanceof MoveEntityTransientCommand) {
                ((MoveEntityTransientCommand)command).getPosition(pos_array);
            } else if (command instanceof RotateEntityCommand) {
                ((RotateEntityCommand)command).getCurrentRotation(rot_array);
            } else if (command instanceof RotateEntityTransientCommand) {
                ((RotateEntityTransientCommand)command).getCurrentRotation(rot_array);
            } else if (command instanceof ChangePropertyCommand) {
                ChangePropertyCommand cpc = (ChangePropertyCommand)command;
                propertyName = cpc.getPropertyName();
                value = cpc.getPropertyValue();
            } else if (command instanceof ChangePropertyTransientCommand) {
                ChangePropertyTransientCommand cptc = (ChangePropertyTransientCommand)command;
                propertyName = cptc.getPropertyName();
                value = cptc.getPropertyValue();
            } else if (command instanceof MoveVertexCommand) {
                ((MoveVertexCommand)command).getEndPosition(pos_array);
            } else if (command instanceof MoveVertexTransientCommand) {
                ((MoveVertexTransientCommand)command).getPosition(pos_array);
            } else if (command instanceof ScaleEntityCommand) {
                ((ScaleEntityCommand)command).getNewPosition(pos_array);
                ((ScaleEntityCommand)command).getNewScale(scl_array);
            } else if (command instanceof ScaleEntityTransientCommand) {
            	((ScaleEntityTransientCommand)command).getPosition(pos_array);
            	((ScaleEntityTransientCommand)command).getScale(scl_array);
            } else if (command instanceof TransitionEntityChildCommand){
            	((TransitionEntityChildCommand)command).getEndPosition(pos_array);
            	((TransitionEntityChildCommand)command).getEndScale(scl_array);
            	
            	Entity endParentEntity = 
            		((TransitionEntityChildCommand)command).getEndParentEntity();
            	
            	//parentWrapper = wrapperMap.get(endParentEntity.getEntityID());
            }
            
            // note: AddEntityChildCommand & AddChildCommand check only
            // with the initial transformational values.
            
            if (propertyName != null) {
                if (propertyName.equals(PositionableEntity.POSITION_PROP)) {
                    double[] value_d = (double[])value;
                    pos_array[0] = value_d[0];
                    pos_array[1] = value_d[1];
                    pos_array[2] = value_d[2];
                } else if (propertyName.equals(PositionableEntity.ROTATION_PROP)) {
                    float[] value_f = (float[])value;
                    rot_array[0] = value_f[0];
                    rot_array[1] = value_f[1];
                    rot_array[2] = value_f[2];
                    rot_array[3] = value_f[3];
                } else if (propertyName.equals(PositionableEntity.SCALE_PROP)) {
                    float[] value_f = (float[])value;
                    scl_array[0] = value_f[0];
                    scl_array[1] = value_f[1];
                    scl_array[2] = value_f[2];
                }
            }
			
            if (minimum_extent != null) {     
                pe.getSize(size_array);
                
                // compare the current scale to the minimum scale and 
                // adjust to use larger of the two if necessary                
                for (int i = 0; i < scl_array.length; i++) {
                    
                    float size = size_array[i] * scl_array[i];                   
                    if (size < minimum_extent[i]) {
                        scl_array[i] = minimum_extent[i] / size_array[i];
                    }
                }
            }
			
            // configure the bounds scale
            activeBounds.setScale(scl_array);
	            
			// configure the bounds border if one is specified
			float[] bounds_border = null;
			obj = entity.getProperty(
				PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
				PositionableEntity.BOUNDS_BORDER_PROP);
			if ((obj != null) && (obj instanceof float[])) {
				bounds_border = (float[])obj;
				if (bounds_border.length < 3) {
					bounds_border = null;
				}
			}
			if (bounds_border != null) {
				activeBounds.setBorder(bounds_border);
			}
        }
		
		int pid = entity.getParentEntityID();
		Entity parentEntity = null;

		// set the parent from the command, if the command
		// defines a parent
		if (command instanceof TransitionEntityChildCommand){
			
			parentEntity = 
				((TransitionEntityChildCommand)command).getEndParentEntity();
			
		} else if (command instanceof AddEntityChildCommand) {
			
			parentEntity = 
				((AddEntityChildCommand)command).getParentEntity();
			
		} else if (command instanceof AddEntityChildTransientCommand) {
			
			parentEntity = 
				((AddEntityChildTransientCommand)command).getParentEntity();
			
		} else if (command instanceof RemoveEntityChildCommand) {
			
			parentEntity = 
				((RemoveEntityChildCommand)command).getParentEntity();
			
		} else {
			// the command does not define a parent
			if (entity_sur_wrapper != null) {
				// initialize the parent from the surrogate, if one exists
				parentEntity = entity_sur_wrapper.getParentEntity();
			} else {
				// otherwise, use the entity's defined parent
				parentEntity = model.getEntity(pid);
			}
		}
		
		if (parentEntity == null) {
			System.out.println("ECM: Can't determine parent for: command: "+ command + ": entity: "+ entity);
			return(false);
		}
		
		//////////////////////////////////////////////////////////////////////
		// configure the surrogate
		sourceSurrogate.setEntity((PositionableEntity)entity);
		sourceSurrogate.setParentEntityID(parentEntity.getEntityID());
		sourceSurrogate.setPosition(pos_array);
		sourceSurrogate.setRotation(rot_array);
		sourceSurrogate.setBounds(activeBounds);
		return(true);
	}
	
    /**
     * Determine whether the command and entity types can be processed
     *
     * @param command The Command instance
     * @param entity The Entity being acted upon by the Command
     * @return true if the command can be processed, false otherwise
     */
    private boolean validate(Command command, Entity entity) {

        boolean cmdStatus = false;

        // check the command types
        if (command instanceof MoveEntityCommand) {
            cmdStatus = true;
        } else if (command instanceof MoveEntityTransientCommand) {
            cmdStatus = true;
        } else if (command instanceof RotateEntityCommand) {
            cmdStatus = true;
        } else if (command instanceof RotateEntityTransientCommand) {
            cmdStatus = true;
        } else if (command instanceof ChangePropertyCommand) {
            cmdStatus = true;
        } else if (command instanceof ChangePropertyTransientCommand) {
            cmdStatus = true;
        } else if (command instanceof AddEntityChildCommand) {
            cmdStatus = true;
        } else if (command instanceof AddEntityCommand) {
            cmdStatus = true;
        } else if (command instanceof MoveSegmentCommand) {
            cmdStatus = true;
        } else if (command instanceof MoveSegmentTransientCommand) {
            cmdStatus = true;
        } else if (command instanceof MoveVertexCommand) {
          cmdStatus = true;
        } else if (command instanceof MoveVertexTransientCommand) {
          cmdStatus = true;
        } else if (command instanceof ScaleEntityCommand) {
          cmdStatus = true;
        } else if (command instanceof ScaleEntityTransientCommand) {
          cmdStatus = true;
        } else if (command instanceof TransitionEntityChildCommand) {
            cmdStatus = true;
        } else if (command instanceof RemoveEntityCommand) {
        	cmdStatus = true;
        } else if (command instanceof RemoveEntityChildCommand){
        	cmdStatus = true;
        } else if (command instanceof RemoveSegmentCommand){
        	cmdStatus = true;
        }
        
        if (!cmdStatus) {
            return(false);
        }
        // check the entity types
        int type = entity.getType();
        if ((type == Entity.TYPE_MODEL) || 
        	(type == Entity.TYPE_MODEL_WITH_ZONES) ||
        	(type == Entity.TYPE_SEGMENT) ||
            (type == Entity.TYPE_VERTEX)) {
            
            return(true);
        } else {
            return(false);
        }
    }
    
	/**
	 * Aggregate the children of the specified entity 
	 * into the argument list
	 *
	 * @param entity The Entity whose children to gather
	 * @param list The List to place them in
	 */
	private void getChildren(Entity entity, List<Entity> list) {
		if (entity.hasChildren()) {
			ArrayList<Entity> children = entity.getChildren();
			list.addAll(children);
			for (int i = 0; i < children.size(); i++) {
				Entity child = children.get(i);
				getChildren(child, list);
			}
		}
	}
	
	/**
	 * Initialize the map of wrappers for collision testing
	 */
	private void initActiveWrapperMap() {
		activeWrapperMap.clear();
		activeWrapperMap.putAll(wrapperMap);
		if (enableSurrogates) {
			activeWrapperMap.putAll(surrogateMap);
		}
	}
	
	/**
	 * Transform the bounds of the active wrappers
	 */
	private void transformBounds() {
		
		int[] inactive = new int[activeWrapperMap.size()];
		int num_inactive = 0;
		
		for (Iterator<Integer> i = activeWrapperMap.keySet().iterator();
			i.hasNext();) {
			
			int id = i.next();
			EntityWrapper wrapper = activeWrapperMap.get(id);
			if (getTransformToRoot(wrapper, mtx)) {
				
				OrientedBoundingBox bounds = wrapper.getBounds();
				bounds.transform(mtx);
				
				OrientedBoundingBox ex_bounds = wrapper.getExtendedBounds();
				if (ex_bounds != bounds) {
					ex_bounds.transform(mtx);
				}
			} else {
				inactive[num_inactive++] = id;
			}
		}
		for (int i = 0; i < num_inactive; i++) {
			activeWrapperMap.remove(inactive[i]);
		}
	}
	
	/**
	 * Initialize the matrix object for the entity to root transform
	 *
	 * @param wrapper The entity wrapper
	 * @param mtx The matrix object to configure
	 * @return true if the matrix has been configured, false otherwise
	 */
	private boolean getTransformToRoot(EntityWrapper wrapper, Matrix4f mtx) {
		
		boolean config = false;
		int idx = 0;
		if (wrapper.isEnabled()) {
			
			initMatrix(wrapper, idx);
			idx++;
			int parentID = getParentID(wrapper);
			
			ContentContainerEntity cce = entityManager.getContentContainerEntity();
			int rootID = cce.getEntityID();
			
			while (parentID != rootID) {
				
				EntityWrapper parent_wrapper = activeWrapperMap.get(parentID);
				if (parent_wrapper != null) {
					
					if (parent_wrapper.isEnabled()) {
						
						initMatrix(parent_wrapper, idx);
						idx++;
						parentID = getParentID(parent_wrapper);
						
					} else {
						// the parent wrapper is disabled,
						// therefore there is no path to root.
						// this is considered a 'normal' condition
						idx = 0;
						break;
					}
				} else {
					Entity e = model.getEntity(parentID);
					if (e != null) {
						if (e instanceof SegmentableEntity) {
							// special case of segmentable entity, must be hard coded.....
							Matrix4f m;
							if (mtx_list.size() <= idx) {
								m = new Matrix4f();
								mtx_list.add(m);
							} else {
								m = mtx_list.get(idx);
							}
							m.setIdentity();
							AxisAngle4f r = new AxisAngle4f(1, 0, 0, (float)Math.PI/2);
							m.setRotation(r);
							idx++;
							
							parentID = e.getParentEntityID();
						} else {
							// no wrapper, not the multi segment, WTF could this be:
							idx = 0;
							System.out.println("ECM: Unidentified entity: "+ e +", wrapper for: "+
								getIdentifier(wrapper.getEntity()) +" is orphaned");
							break;
						}
					} else {
						// the parent entity does not exist
						idx = 0;
						System.out.println("ECM: Invalid parent: "+ parentID +", wrapper for: "+
							getIdentifier(wrapper.getEntity()) +" is orphaned");
						break;
					}
				}
			}
		}
		if (idx > 0) {
			mtx.setIdentity();
			for (int i = idx - 1; i >= 0; i--) {
				Matrix4f m = mtx_list.get(i);
				mtx.mul(m);
			}
			config = true;
		}
		return(config);
	}
	
	/**
	 * Return the ID of the parent of the wrapper
	 *
	 * @param wrapper The entity wrapper
	 * @return The parent ID
	 */
	private int getParentID(EntityWrapper wrapper) {
		int parentID = -1;
		if (wrapper instanceof CollisionSurrogate) {
			parentID = ((CollisionSurrogate)wrapper).getParentEntityID();
		} else if (wrapper instanceof SurrogateEntityWrapper) {
			parentID = ((SurrogateEntityWrapper)wrapper).getParentEntity().getEntityID();
		} else {
			PositionableEntity pe = wrapper.getEntity();
			parentID = pe.getParentEntityID();
		}
		return(parentID);
	}
	
	/**
	 * Initialize the specified matrix with the position and rotation
	 * of the entity.
	 *
	 * @param wrapper The entity wrapper to calculate the matrix values for
	 * @param idx The index of the matrix on the local matrix list
	 * to initialize.
	 */
	private void initMatrix(EntityWrapper wrapper, int idx) {
		
		Matrix4f mtx;
		if (mtx_list.size() <= idx) {
			mtx = new Matrix4f();
			mtx_list.add(mtx);
		} else {
			mtx = mtx_list.get(idx);
		}
		if (wrapper instanceof SurrogateEntityWrapper) {
			SurrogateEntityWrapper surrogate = (SurrogateEntityWrapper)wrapper;
			surrogate.getRotation(rot_array);
			surrogate.getPosition(pos_array);
		} else if (wrapper instanceof SegmentEntityWrapper) {
			SegmentEntityWrapper segment = (SegmentEntityWrapper)wrapper;
			segment.getRotation(rot_array);
			segment.getPosition(pos_array);
		} else {
			PositionableEntity pe = wrapper.getEntity();
			pe.getRotation(rot_array);
			pe.getPosition(pos_array);
		}
		rotation.set(rot_array);
		translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);
		
		mtx.setIdentity();
		mtx.setRotation(rotation);
		mtx.setTranslation(translation);
	}
	
	/**
	 * Return a short String identifier of the argument Entity
	 *
	 * @param entity The entity
	 * @return The identifier
	 */
	private String getIdentifier(Entity entity) {
		return("[id="+ entity.getEntityID() + ", name=\""+ entity.getName() +"\"]");
	}

	/**
	 * Get the whole map of entities to the SurrogateEntityWrapper copy that
	 * expresses the original state of the surrogate. Note, the 
	 * SurrogateEntityWrapper is a copy of the original, not a reference to the
	 * original.
	 * 
	 * @return Copy of map of entities to copy of original surrogate.
	 */
	public Map<Entity, SurrogateEntityWrapper> 
		getSidePocketedOriginalSurrogateStates() {

		Map<Entity, SurrogateEntityWrapper> tempSurrogateMapCopy = 
			new HashMap<Entity, SurrogateEntityWrapper>();
		tempSurrogateMapCopy.putAll(tempSurrogateOriginalMap);
		return tempSurrogateMapCopy;
	}
	
	/**
	 * Get the original SurrogateEntityWrapper matching the entity specified. If
	 * one doesn't exist, null will be returned. Note, the 
	 * SurrogateEntityWrapper returned is a copy of the original, not a 
	 * reference.
	 *  
	 * @param entity Entity to get original SurrogateEntityWrapper for.
	 * @return SurrogateEntityWrapper or null if none found.
	 */
	public SurrogateEntityWrapper getSidePocketedOriginalSurrogateState(
			PositionableEntity entity) {
		
		return tempSurrogateOriginalMap.get(entity);
	}

	/**
	 * Create a copy of the SurrogateEntityWrapper and map it to the entity 
	 * as the original state of the surrogate.
	 * 
	 * @param entity Entity to map originalSurrogate to
	 */
	public void setSidePocketedOriginalSurrogateState(
			PositionableEntity entity) {
		
		// If a surrogate exists for the entity, create a copy and side pocket 
		// it.
		if (surrogateMap.containsKey(entity)) {
			
			SurrogateEntityWrapper originalSurrogate = surrogateMap.get(entity);
			
			double[] position = new double[3];
			float[] rotation = new float[4];
			float[] scale = new float[3];
			boolean isEnabled;
			
			originalSurrogate.getPosition(position);
			originalSurrogate.getRotation(rotation);
			originalSurrogate.getScale(scale);
			isEnabled = originalSurrogate.isEnabled();
			
			DefaultSurrogateEntityWrapper tmpSurrogate = 
				new DefaultSurrogateEntityWrapper(
						entity, 
						originalSurrogate.getParentEntity(), 
						position, 
						rotation, 
						scale);
			
			tmpSurrogate.setEnabled(isEnabled);
			
			tempSurrogateOriginalMap.put(entity, tmpSurrogate);
		}
	}

	/**
	 * Remove the SurrogateEntityWrapper, matching the entity, from the map
	 * of original surrogate states. Reverses the actions of the temporary 
	 * surrogate and restores the surrogate list to its previous state prior
	 * to the temp surrogate action applied by the entity.
	 * 
	 * @param entity Entity entry to remove from map.
	 */
	public void removeSidePocketedOriginalSurrogateState(
			PositionableEntity entity) {
		
		// First check if the temp surrogate map contains the key. If so,
		// check if the key exists in the surrogate list and if so, set it back 
		// to the starting state. If it doesn't create a new one to put back. 
		// Then remove the entity from the temp surrogate map.
		
		if (tempSurrogateOriginalMap.containsKey(entity)) {
			
			if (surrogateMap.containsKey(entity)) {
				
				DefaultSurrogateEntityWrapper tmpSurrogate = 
					(DefaultSurrogateEntityWrapper) 
					tempSurrogateOriginalMap.get(entity);
				
				SurrogateEntityWrapper originalSurrogate = 
					surrogateMap.get(entity);
				
				// Extract values
				double[] position = new double[3];
				float[] rotation = new float[4];
				float[] scale = new float[3];
				boolean isEnabled;
				
				tmpSurrogate.getPosition(position);
				tmpSurrogate.getRotation(rotation);
				tmpSurrogate.getScale(scale);
				isEnabled = tmpSurrogate.isEnabled();
				
				originalSurrogate.setParentEntity(
						tmpSurrogate.getParentEntity());
				originalSurrogate.setPosition(position);
				originalSurrogate.setRotation(rotation);
				originalSurrogate.setScale(scale);
				originalSurrogate.setEnabled(isEnabled);
			
			} else {
				
				DefaultSurrogateEntityWrapper surrogate = 
					(DefaultSurrogateEntityWrapper) 
					tempSurrogateOriginalMap.get(entity);
				
				addSurrogate(surrogate);
			}

			tempSurrogateOriginalMap.remove(entity);
			
		} else {
			
			surrogateMap.remove(entity.getEntityID());
		}
	}

	/**
	 * Clear all original SurrogateEntityWrapper state data, setting each of
	 * the surrogates stored back to their original states, if they still exist
	 * in the live surrogate list. This is done any time clearSurrogates() is 
	 * called.
	 */
	public void clearSidePocketedOriginalSurrogateStates() {
		
		// Call removeTempOriginalSurrogateState for each entity key then 
		// clear map.
		
		Object[] keys = tempSurrogateOriginalMap.keySet().toArray();
		
		for (int i = 0; i < keys.length; i++) {
			
			removeSidePocketedOriginalSurrogateState(
					(PositionableEntity) keys[i]);
		}
		
		tempSurrogateOriginalMap.clear();
	}
}
