/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.util;

// External imports
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Utility methods for performing transform calculations (Move, scale, rotate).
 *
 * @author Ben Yarger
 * @version $Revision: 1.37 $
 */
public abstract class TransformUtils {
	
	/**
     * Get the position of an entity relative to its parent zone. This process 
     * does not consider any commands that are part of the current evaluation. 
     * This will give a result that is guaranteed correct for the previous frame
     *  only.
     *
     * @param model reference to the WorldModel
     * @param entity Entity of which we want to know the relative position
     * @param exact True to use exact position, false to use previous frame
     * @return double[] xyz pos relative to relativeEntity, or null if it
     * cannot be read
     */
    public static double[] getPositionRelativeToZone(
            WorldModel model,
            Entity entity,
            boolean exact){

        return getRelativePosition(
            model,
            entity,
            SceneHierarchyUtility.findZoneEntity(model, entity),
            false,
            exact);
    }

    /**
     * Get the position of an entity relative to its parent zone. This process 
     * does not consider any commands that are part of the current evaluation. 
     * This will give a result that is guaranteed correct for the previous frame
     *  only. Defaults to not use exact position.
     *
     * @param model reference to the WorldModel
     * @param entity Entity of which we want to know the relative position
     * @return double[] xyz pos relative to relativeEntity, or null if it
     * cannot be read
     */
    public static double[] getPositionRelativeToZone(
            WorldModel model,
            Entity entity){

        return getRelativePosition(
            model,
            entity,
            SceneHierarchyUtility.findZoneEntity(model, entity),
            false);
    }
    
    /**
     * Get the position of an entity relative to its parent zone. This result 
     * includes the up-to-date reflection of commands issued that could have an 
     * impact on the start position of the entity.
     *
     * @param model reference to the WorldModel
     * @param entity Entity of which we want to know the relative position
     * @return double[] xyz pos relative to relativeEntity, or null if it
     * cannot be read
     */
    public static double[] getExactPositionRelativeToZone(
    		WorldModel model,
            Entity entity) {
    	
    	return getExactRelativePosition(
    			model, 
    			entity, 
    			SceneHierarchyUtility.findExactZoneEntity(model, entity), 
    			false);
    }

    /**
     * Get the position of an entity relative to the relativeEntity. If the
     * relativeEntity is not found, the position returned will be relative
     * to the parent zone. This process does not consider any commands that are 
     * part of the current evaluation. This will give a result that is 
     * guaranteed correct for the previous frame only.
     *
     * @param model WorldModel
     * @param startEntity Starting entity to build up position data from
     * @param relativeEntity Entity to stop building position data at
     * @param useStartPosition Set true if position should be derived from
     * start positions
     * @return double[] xyz pos relative to relativeEntity, or null if unable
     * to calculate
     */
    public static double[] getRelativePosition(
            WorldModel model,
            Entity startEntity,
            Entity relativeEntity,
            boolean useStartPosition) {

        double[] posTotal = new double[3];
        
        posTotal = 
        	getRelativePosition(
        		model, 
        		startEntity, 
        		relativeEntity, 
        		useStartPosition, 
        		false);
        
        return posTotal;
    }
    
    /**
     * Get the position of an entity relative to the relativeEntity. If the
     * relativeEntity is not found, the position returned will be relative
     * to the parent zone. This result includes the up-to-date reflection of 
     * commands issued that could have an impact on the start position of the 
     * entity.
     *
     * @param model WorldModel
     * @param startEntity Starting entity to build up position data from
     * @param relativeEntity Entity to stop building position data at
     * @param useStartPosition Set true if position should be derived from
     * start positions
     * @return double[] xyz pos relative to relativeEntity, or null if unable
     * to calculate
     */
    public static double[] getExactRelativePosition(
    		WorldModel model,
            Entity startEntity,
            Entity relativeEntity,
            boolean useStartPosition) {
    	
    	double[] posTotal = new double[3];
        
        posTotal = 
        	getRelativePosition(
        		model, 
        		startEntity, 
        		relativeEntity, 
        		useStartPosition, 
        		true);
        
        return posTotal;
    }
	
    /**
     * Get position relative to the parent zone. Do so by stripping the end
     * position, entity and parent data from the command. This process 
     * does not consider any commands that are part of the current evaluation. 
     * This will give a result that is guaranteed correct for the previous frame
     *  only.
     *
     * @param model WorldModel to reference
     * @param command Command to pull data from
     * @return Position relative to zone, or null if command cannot be read
     */
    public static double[] getPositionRelativeToZone(
            WorldModel model,
            Command command) {

        double[] results;

        results = getPositionRelativeToZone(model, command, false);
        
        return results;
    }
    
    /**
     * Get position relative to the parent zone. Do so by stripping the end
     * position, entity and parent data from the command. This result includes 
     * the up-to-date reflection of commands issued that could have an impact on
     *  the start position of the entity.
     *
     * @param model WorldModel to reference
     * @param command Command to pull data from
     * @return Position relative to zone, or null if command cannot be read
     */
    public static double[] getExactPositionRelativeToZone(
    		WorldModel model,
    		Command command) {
    	
    	double[] results;

        results = getPositionRelativeToZone(model, command, true);
        
        return results;
    }
    
    /**
     * Get the position of an entity relative to the first wall or floor
     * parent found. All other zone types are ignored and position is 
     * calculated only relative to the first wall or floor parent found.
     * Can calculate position based on starting positions or current
     * positions. This process does not consider any commands that are part of 
     * the current evaluation. This will give a result that is guaranteed 
     * correct for the previous frame only.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to examine
     * @param useStartPosition True to use start positions only, false 
     * otherwise
     * @return Position relative to wall or floor or null if not able
     * to calculate
     */
    public static double[] getPositionRelativeToWallOrFloor(
    		WorldModel model,
    		Entity entity,
    		boolean useStartPosition) {
    	
    	return getPositionRelativeToWallOrFloor(
    			model, 
    			entity, 
    			useStartPosition,
    			false);
    }
    
    /**
     * Get the position of an entity relative to the first wall or floor
     * parent found. All other zone types are ignored and position is 
     * calculated only relative to the first wall or floor parent found.
     * Can calculate position based on starting positions or current
     * positions. This result includes the up-to-date reflection of commands 
     * issued that could have an impact on the current position of the entity.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to examine
     * @param useStartPosition True to use start positions only, false 
     * otherwise
     * @return Position relative to wall or floor or null if not able
     * to calculate
     */
    public static double[] getExactPositionRelativeToWallOrFloor(
    		WorldModel model,
    		Entity entity,
    		boolean useStartPosition) {
    	
    	return getPositionRelativeToWallOrFloor(
    			model, 
    			entity, 
    			useStartPosition,
    			true);
    }
    
    /**
     * Get the position of an entity relative to the active zone.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to get position relative to active zone
     * @param exact True to get position data that is correct for the current
     * frame, false to get position data correct for the previous frame
     * @return Resulting position data, null if there was a problem
     */
    public static double[] getPositionRelativeToActiveZone(
    		WorldModel model,
    		PositionableEntity entity,
    		boolean exact) {
    	
    	double[] sceneCoordinates =
    		getPositionInSceneCoordinates(model, entity, exact);
    	
    	if (sceneCoordinates == null) {
    		return null;
    	}
    	
    	PositionableEntity activeZone = (PositionableEntity) 
    		SceneHierarchyUtility.getActiveZoneEntity(model);
    	
    	if (activeZone == null) {
    		return null;
    	}
    	
    	sceneCoordinates = 
    		convertSceneCoordinatesToLocalCoordinates(
    			model, 
    			sceneCoordinates, 
    			activeZone, 
    			exact);
    	
    	return sceneCoordinates;
    }
    
    /**
     * Get the position of the entity. This process does not consider any 
     * commands that are part of the current evaluation. This will give a result
     *  that is guaranteed correct for the previous frame only.
     * 
     * @param pEntity Entity to get current position for
     * @return Current position
     */
    public static double[] getPosition(PositionableEntity entity) {
    	
    	ArrayList<Command> cmdList = (ArrayList<Command>) 
		CommandSequencer.getInstance().getFullCommandList(true);
    	
    	double[] position = new double[3];
    	entity.getPosition(position);
    	
    	// Look for an add command with this entity case and if found, its 
    	// position should be the start position which we know will be set to 
    	// the previous frame's ghost entity position.
    	
    	for (int i = (cmdList.size() - 1); i >= 0; i--) {
    		
    		Command cmd = cmdList.get(i);
    		
    		if (cmd instanceof RuleDataAccessor) {
    			
    			if (((RuleDataAccessor)cmd).getEntity() == entity) {
    				
    				if (cmd instanceof AddEntityChildCommand) {
    				
    					entity.getStartingPosition(position);
    					break;
    					
    				} 
    				
    			} 
    		}
    	}
    	
    	return position;
    }
    
    /**
     * Get the exact position for the entity. This result includes the 
     * up-to-date reflection of commands issued that could have an impact
     * on the current position of the entity, including the current command 
     * being evaluated.
     * 
     * @param entity Entity to get current position for
     * @return Current position
     */
    public static double[] getExactPosition(PositionableEntity entity) {
    	
    	ArrayList<Command> cmdList = (ArrayList<Command>) 
    		CommandSequencer.getInstance().getFullCommandList(true);
    	
    	double[] position = new double[3];
    	entity.getPosition(position);
    	
    	// By starting at the end of the list and working backwards
    	// we are guaranteed to get the last movement command
    	// issued affecting the entity.
    	
    	for (int i = (cmdList.size() - 1); i >= 0; i--) {
    		
    		Command cmd = cmdList.get(i);
    		
    		if (cmd instanceof RuleDataAccessor) {
    			
    			if (((RuleDataAccessor)cmd).getEntity() == entity) {
    				
    				if (cmd instanceof MoveEntityCommand) {
    				
    					((MoveEntityCommand)cmd).getEndPosition(position);
    					break;
    					
    				} else if (cmd instanceof MoveEntityTransientCommand) {
    					
    					((MoveEntityTransientCommand)cmd).getPosition(
    							position);
    					break;
    					
    				} else if (cmd instanceof TransitionEntityChildCommand) {
    					
    					((TransitionEntityChildCommand)cmd).getEndPosition(
    							position);
    					break;
    					
    				} else if (cmd instanceof ScaleEntityCommand) {
    					
    					((ScaleEntityCommand)cmd).getNewPosition(position);
    					break;
    					
    				} else if (cmd instanceof ScaleEntityTransientCommand) {
    					
    					((ScaleEntityTransientCommand)cmd).getPosition(
    							position);
    					break;
    				}
    				
    			} else if (entity instanceof SegmentEntity) {
    				
    				if (cmd instanceof MoveVertexCommand) {
    				
	    				MoveVertexCommand mvVCmd = (MoveVertexCommand) cmd;
	    				VertexEntity vertex = (VertexEntity) mvVCmd.getEntity();
    				
    					if (((SegmentEntity)entity).getStartVertexEntity() == 
    						vertex) {
    						
    						mvVCmd.getPosition(position);
    						position[1] = 0.0;
    						break;
    					}
    				
	    			} else if (cmd instanceof MoveVertexTransientCommand) {
	    				
	    				MoveVertexTransientCommand mvVCmd = 
	    					(MoveVertexTransientCommand) cmd;
	    				VertexEntity vertex = 
	    					(VertexEntity) mvVCmd.getEntity();
    				
    					if (((SegmentEntity)entity).getStartVertexEntity() == 
    						vertex) {
    						
    						mvVCmd.getPosition(position);
    						position[1] = 0.0;
    						break;
    					}
    					
	    			}
    			}
    		}
    	}
    	
    	return position;
    }
    
    /**
     * Get the starting position for an entity. This process does not consider 
     * any commands that are part of the current evaluation. This will give a 
     * result that is guaranteed correct for the previous frame only.
     * 
     * @param pEntity Entity to get start position for
     * @return starting position
     */
    public static double[] getStartPosition(PositionableEntity pEntity) {
	
		double[] position = new double[3];
		pEntity.getStartingPosition(position);
		
		return position;
    }
    
    /**
     * Get the exact starting position for an entity. This result includes the 
     * up-to-date reflection of commands issued that could have an impact
     * on the start position of the entity, including the current command being 
     * evaluated.
     * 
     * @param pEntity Entity to get start position for
     * @return starting position
     */
    public static double[] getExactStartPosition(PositionableEntity pEntity) {
    	
    	ArrayList<Command> cmdList = (ArrayList<Command>) 
		CommandSequencer.getInstance().getFullCommandList(true);
	
		double[] position = new double[3];
		pEntity.getStartingPosition(position);
		
		// By starting at the end of the list and working backwards
		// we are guaranteed to get the last movement command
		// issued affecting the entity.
		
		for (int i = (cmdList.size() - 1); i >= 0; i--) {
			
			Command cmd = cmdList.get(i);
			
			if (cmd instanceof RuleDataAccessor) {
				
				if (cmd instanceof TransitionEntityChildCommand) {
					
					if (((TransitionEntityChildCommand)cmd).getEntity() == 
						pEntity) {
					
						((TransitionEntityChildCommand)cmd).getStartPosition(
								position);
						break;
					}
				}
			}
		}
		
		return position;
    }
    
    /**
     * Get the position of the entity in scene coordinates.
     * </br></br>
     * <pre>
     * 			+Z
     *      ____|_____+y__
     *     /	|		 /
     *    /		|		/
     *   /		|	   /+x		This is the floor plane and scene coordinates
     *  /			  /
     * /_____________/
     * </pre>
     * 
     * @param model WorldModel to reference
     * @param entity Entity to convert to scene coordinates
     * @param exact True to get position that is correct for current set of 
     * commands in queue, false to get position correct for previous frame
     * @return Position in scene coordinates, or null if unable to calculate
     */
    public static double[] getPositionInSceneCoordinates(
    		WorldModel model,
    		PositionableEntity entity,
    		boolean exact) {
    	
    	Matrix4f transform = 
    		getTransformsInSceneCoordinates(model, entity, exact);
    	
    	if (transform == null) {
    		return null;
    	}
    	   	
    	Vector3f vec = new Vector3f(0.0f, 0.0f, 0.0f);
    	transform.get(vec);
    	
    	double[] pos = new double[3];
    	pos[0] = vec.x;
    	pos[1] = vec.y;
    	pos[2] = vec.z;
    	
    	return pos;
    }
    
    /**
     * Calculate the cumulative rotation and position of an entity in scene 
     * coordinates. Scene coordinates are a right handed coordinate system with
     * +x always to the right, +y up and + z is the height above the floor. 
     * Returns the matrix transform to convert to scene coordinates.
     * </br></br>
     * <pre>
     * 			+Z
     *      ____|_____+y__
     *     /	|		 /
     *    /		|		/
     *   /		|	   /+x		This is the floor plane and scene coordinates
     *  /			  /
     * /_____________/
     * </pre>
     * 
     * @param model WorldModel to reference
     * @param entity Entity to get scene coordinate transforms for
     * @param exact True to get exact positions, false otherwise
     * @return Transform matrix, or null if there was a problem
     */
    public static Matrix4f getTransformsInSceneCoordinates(
    		WorldModel model,
    		PositionableEntity entity,
    		boolean exact) {
    	
    	ArrayList<Matrix4f> matList = new ArrayList<Matrix4f>();
    	
    	// Get the initial parent to begin working from
    	Entity parentEntity;
    	double[] accumPos = new double[3];
    	float[] accumRot = new float[4];
    	
    	if (!exact) {
    		parentEntity = SceneHierarchyUtility.getParent(model, entity);
    		accumPos = getPosition(entity);
    		accumRot = getRotation(entity);
    	} else {
    		parentEntity = SceneHierarchyUtility.getExactParent(model, entity);
    		accumPos = getExactPosition(entity);
    		accumRot = getExactRotation(entity);
    	}
    	
    	if (parentEntity == null) {
    		return null;
    	}
    	
    	Matrix4f initialMat = new Matrix4f();
    	initialMat.setIdentity();
    	Vector3f initialTrans = new Vector3f(
    			(float)accumPos[0],
    			(float)accumPos[1],
    			(float)accumPos[2]);
    	initialMat.setTranslation(initialTrans);
    	initialMat.setRotation(new AxisAngle4f(accumRot));
    	matList.add(initialMat);
    	
    	// Begin data accumulation loop
    	while (parentEntity.getType() != Entity.TYPE_CONTENT_ROOT) {
    		
    		Matrix4f mat = new Matrix4f();
    		mat.setIdentity();
    		
    		// If the parentEntity isn't a PositionableEntity then there won't
    		// be any position data to extract, look for SegmentableEntity
    		// special case
    		if (parentEntity instanceof PositionableEntity) {
    		
	    		if (!exact) {
	        		accumPos = getPosition((PositionableEntity)parentEntity);
	        		accumRot = getRotation((PositionableEntity)parentEntity);
	        	} else {
	        		accumPos = getExactPosition((PositionableEntity)parentEntity);
	        		accumRot = getExactRotation((PositionableEntity)parentEntity);
	        	}
	    		
	    		Vector3f trans = new Vector3f(
	        			(float)accumPos[0],
	        			(float)accumPos[1],
	        			(float)accumPos[2]);
	        	mat.setTranslation(trans);
	        	mat.setRotation(new AxisAngle4f(accumRot));
	        	matList.add(mat);
	    		
    		} else if (parentEntity instanceof SegmentableEntity) {

    			////////////////////////////////////////////////////////////////
    			// Taken from MultisegmentManager

    	        // rem: hard coded, relationship between the walls and the floor
    	        AxisAngle4f rot2 = new AxisAngle4f(1, 0, 0, (float)Math.PI/2);
    	        ////////////////////////////////////////////////////////////////
    	        
    	        mat.setRotation(rot2);
    	        matList.add(mat);
    		} 
    		
    		parentEntity = 
    			SceneHierarchyUtility.getExactParent(model, parentEntity);
        	
        	if (parentEntity == null) {
        		return null;
        	}
    	}
    	
    	// Create the cumulative transform based on the transform lists
    	Matrix4f cumulativeMat = new Matrix4f();
    	cumulativeMat.setIdentity();
    	
    	for (int i = (matList.size() - 1); i >= 0; i--) {

    		cumulativeMat.mul(matList.get(i));
    	}
    	    	
    	return cumulativeMat;
    }
    
    /**
     * Calculate the cumulative rotation and position of an entity relative
	 * to the specified ancestor entity.
	 *
     * @param model WorldModel to reference
     * @param entity Entity to get transformation for
	 * @param ancestorEntity The ancestor entity
     * @param exact True to get exact positions, false otherwise
     * @return Transform matrix, or null if there was a problem
     */
    public static Matrix4f getRelativeTransform(
    		WorldModel model,
    		PositionableEntity entity,
			Entity ancestorEntity,
    		boolean exact) {
    	
    	ArrayList<Matrix4f> matList = new ArrayList<Matrix4f>();
    	
    	// Get the initial parent to begin working from
    	Entity parentEntity;
    	double[] accumPos = new double[3];
    	float[] accumRot = new float[4];
    	
    	if (!exact) {
    		parentEntity = SceneHierarchyUtility.getParent(model, entity);
    		accumPos = getPosition(entity);
    		accumRot = getRotation(entity);
    	} else {
    		parentEntity = SceneHierarchyUtility.getExactParent(model, entity);
    		accumPos = getExactPosition(entity);
    		accumRot = getExactRotation(entity);
    	}
    	
    	if (parentEntity == null) {
    		return null;
    	}
    	
    	Matrix4f initialMat = new Matrix4f();
    	initialMat.setIdentity();
    	Vector3f initialTrans = new Vector3f(
    			(float)accumPos[0],
    			(float)accumPos[1],
    			(float)accumPos[2]);
    	initialMat.setTranslation(initialTrans);
    	initialMat.setRotation(new AxisAngle4f(accumRot));
    	matList.add(initialMat);
    	
    	// Begin data accumulation loop
    	while (parentEntity != ancestorEntity) {
    		
    		Matrix4f mat = new Matrix4f();
    		mat.setIdentity();
    		
    		// If the parentEntity isn't a PositionableEntity then there won't
    		// be any position data to extract, look for SegmentableEntity
    		// special case
    		if (parentEntity instanceof PositionableEntity) {
    		
	    		if (!exact) {
	        		accumPos = getPosition((PositionableEntity)parentEntity);
	        		accumRot = getRotation((PositionableEntity)parentEntity);
	        	} else {
	        		accumPos = getExactPosition((PositionableEntity)parentEntity);
	        		accumRot = getExactRotation((PositionableEntity)parentEntity);
	        	}
	    		
	    		Vector3f trans = new Vector3f(
	        			(float)accumPos[0],
	        			(float)accumPos[1],
	        			(float)accumPos[2]);
	        	mat.setTranslation(trans);
	        	mat.setRotation(new AxisAngle4f(accumRot));
	        	matList.add(mat);
	    		
    		} else if (parentEntity instanceof SegmentableEntity) {

    			////////////////////////////////////////////////////////////////
    			// Taken from MultisegmentManager

    	        // rem: hard coded, relationship between the walls and the floor
    	        AxisAngle4f rot2 = new AxisAngle4f(1, 0, 0, (float)Math.PI/2);
    	        ////////////////////////////////////////////////////////////////
    	        
    	        mat.setRotation(rot2);
    	        matList.add(mat);
    		} 
    		
    		parentEntity = 
    			SceneHierarchyUtility.getExactParent(model, parentEntity);
        	
        	if (parentEntity == null) {
        		return null;
        	}
    	}
    	
    	// Create the cumulative transform based on the transform lists
    	Matrix4f cumulativeMat = new Matrix4f();
    	cumulativeMat.setIdentity();
    	
    	for (int i = (matList.size() - 1); i >= 0; i--) {

    		cumulativeMat.mul(matList.get(i));
    	}
    	    	
    	return cumulativeMat;
    }
    
    /**
     * Convert local coordinates into scene coordinates. Similar to 
     * getPositionInSceneCoordinates except you can convert any position into
     * scene coordinates according to the transforms applied to the entity.
     * 
     * @param model WorldModel to reference
     * @param entity Entity localPosition is applied to
     * @param localPosition Position to convert to scene coordinates that is
     * relative to the entity
     * @param exact True to give a result that is correct for current frame, 
     * false to give a result that is correct only for the previous frame.
     * @return Position in scene coordinates, or null if there was a problem
     */
    public static float[] convertLocalCoordinatesToSceneCoordinates(
    		WorldModel model,
    		PositionableEntity entity,
    		float[] localPosition,
    		boolean exact) {
    	
    	double[] lPos = new double[3];
    	lPos[0] = localPosition[0];
    	lPos[1] = localPosition[1];
    	lPos[2] = localPosition[2];
    	
    	double[] sceneCoords = convertLocalCoordinatesToSceneCoordinates(
    			model, entity, lPos, exact);
    	
    	float[] result = new float[3];
    	result[0] = (float) sceneCoords[0];
    	result[1] = (float) sceneCoords[1];
    	result[2] = (float) sceneCoords[2];
    	
    	return result;
    }
    
    /**
     * Convert local coordinates into scene coordinates. Similar to 
     * getPositionInSceneCoordinates except you can convert any position into
     * scene coordinates according to the transforms applied to the entity.
     * 
     * @param model WorldModel to reference
     * @param entity Entity localPosition is applied to
     * @param localPosition Position to convert to scene coordinates that is
     * relative to the entity
     * @param exact True to give a result that is correct for current frame, 
     * false to give a result that is correct only for the previous frame.
     * @return Position in scene coordinates, or null if there was a problem
     */
    public static double[] convertLocalCoordinatesToSceneCoordinates(
    		WorldModel model,
    		PositionableEntity entity,
    		double[] localPosition,
    		boolean exact) {
		
		double[] entityPos = new double[3];
		
		if (exact) {
			entityPos = getExactPosition(entity);
		} else {
			entityPos = getPosition(entity);
		}
		
		if (entityPos == null) {
			return null;
		}

		Vector3f localVec = new Vector3f(
				(float) localPosition[0],
				(float) localPosition[1],
				(float) localPosition[2]);
		
		Matrix4f localToSceneMat = 
			getTransformsInSceneCoordinates(model, entity, exact);
		
		if (localToSceneMat == null) {
			return null;
		}
		
		localToSceneMat.transform(localVec);
		
		Vector3f sceneCoordBase = new Vector3f();
		localToSceneMat.get(sceneCoordBase);
		
		double[] scenePos = new double[3];
		scenePos[0] = sceneCoordBase.x + localVec.x;
		scenePos[1] = sceneCoordBase.y + localVec.y;
		scenePos[2] = sceneCoordBase.z + localVec.z;
		
		return scenePos;
    }
    
    /**
     * Convert scene coordinates into local coordinates of the targetEntity 
     * specified. Exact position of the entity returns a position consideration
     * that is correct for the known commands applied to the current evaluation.
     * If exact is set to false, the position will only be guaranteed correct
     * for the previous frame.
     * 
     * @param model WorldModel to reference
     * @param sceneCoordinates Scene coordinates to project into target entity 
     * coordinates
     * @param targetEntity Entity whose local coordinate system we want to 
     * convert into
     * @param exact True to use exact position of the target targetEntity, false
     *  to use loose position of the targetEntity
     * @return Relative position to targetEntity, or null if conversion failed
     */
    public static float[] convertSceneCoordinatesToLocalCoordinates(
    		WorldModel model,
    		float[] sceneCoordinates,
    		PositionableEntity targetEntity,
    		boolean exact) {
    
    	double[] testValue = new double[3];
    	testValue[0] = sceneCoordinates[0];
    	testValue[1] = sceneCoordinates[1];
    	testValue[2] = sceneCoordinates[2];
    	
    	double[] result = 
    		convertSceneCoordinatesToLocalCoordinates(
    				model, 
    				testValue, 
    				targetEntity, 
    				exact);
    	
    	float[] returnVal = new float[3];
    	returnVal[0] = (float) result[0];
    	returnVal[1] = (float) result[1];
    	returnVal[2] = (float) result[2];
    	
    	return returnVal;
    }
    
    /**
     * Convert scene coordinates into local coordinates of the targetEntity 
     * specified. Exact position of the entity returns a position consideration
     * that is correct for the known commands applied to the current evaluation.
     * If exact is set to false, the position will only be guaranteed correct
     * for the previous frame.
     * 
     * @param model WorldModel to reference
     * @param sceneCoordinates Scene coordinates to project into target entity 
     * coordinates
     * @param targetEntity Entity whose local coordinate system we want to 
     * convert into
     * @param exact True to use exact position of the target targetEntity, false
     *  to use loose position of the targetEntity
     * @return Relative position to targetEntity, or null if conversion failed
     */
    public static double[] convertSceneCoordinatesToLocalCoordinates(
    		WorldModel model,
    		double[] sceneCoordinates,
    		PositionableEntity targetEntity,
    		boolean exact) {
    	
    	// Safety check
    	if (sceneCoordinates == null) {
    		return null;
    	}
    	
    	// Process is to get the targetEntity's transform matrix for scene
    	// coordinates. Subtract the resulting transform from the scene
    	// coordinates passed in to get the relative transform between points
    	// in the common scene space. Then transform that vector back into 
    	// the local coordinate system of the targetEntity by inverting the 
    	// transform matrix derived from the targetEntity conversion to scene
    	// coordinates. The result of transforming the vector by the inverted
    	// matrix will be the relative position of the sceneCoordinates to
    	// the targetEntity in the targetEntity's local coordinate system.
    		
    	Matrix4f targetMat = new Matrix4f();
    	
    	targetMat = getTransformsInSceneCoordinates(
				model, 
				targetEntity,
				exact);
    	
    	if (targetMat == null) {
    		return null;
    	}
    	
    	Vector3f targetTransform = new Vector3f();
    	targetMat.get(targetTransform);
    	
    	Vector3f sceneToLocal = new Vector3f(
    			(float) sceneCoordinates[0] - targetTransform.x,
    			(float) sceneCoordinates[1] - targetTransform.y,
    			(float) sceneCoordinates[2] - targetTransform.z);
    	
    	targetMat.invert();
    	targetMat.transform(sceneToLocal);
    	
    	double[] result = new double[3];
		result[0] = sceneToLocal.x;
		result[1] = sceneToLocal.y;
		result[2] = sceneToLocal.z;
		
		return result;
    }
    
    /**
     * Convert a position relative to a specific entity into the coordinate
     * system defined by another entity.
     * 
     * @param model WorldModel to reference.
     * @param positionRelativeToSource Position relative to the sourceEntity.
     * @param sourceEntity Entity to convert positionRelativeToSource from.
     * @param targetCoordSystemEntity Target entity whose coordinate system
     * we want to base our new position on.
     * @param exact True to use position data correct for current frame, false
     * to use position data correct only for the previous frame.
     * @return Resulting converted position, or null if there was a problem
     */
    public static double[] convertToCoordinateSystem(
    		WorldModel model,
    		double[] positionRelativeToSource, 
    		PositionableEntity sourceEntity, 
    		PositionableEntity targetCoordSystemEntity,
    		boolean exact) {
    	
    	double[] sceneCoords = 
    		convertLocalCoordinatesToSceneCoordinates(
    				model, 
    				sourceEntity, 
    				positionRelativeToSource, 
    				exact);
    	
    	if (sceneCoords == null) {
    		return null;
    	}
    	
    	double[] targetCoordPos = 
    		convertSceneCoordinatesToLocalCoordinates(
    				model, 
    				sceneCoords, 
    				targetCoordSystemEntity, 
    				exact);
    	
    	return targetCoordPos;
    }
    
    /**
     * Convert a position relative to a specific entity into the coordinate
     * system defined by another entity.
     * 
     * @param model WorldModel to reference.
     * @param positionRelativeToSource Position relative to the sourceEntity.
     * @param sourceEntity Entity to convert positionRelativeToSource from.
     * @param targetCoordSystemEntity Target entity whose coordinate system
     * we want to base our new position on.
     * @param exact True to use position data correct for current frame, false
     * to use position data correct only for the previous frame.
     * @return Resulting converted position, or null if there was a problem
     */
    public static float[] convertToCoordinateSystem(
    		WorldModel model,
    		float[] positionRelativeToSource, 
    		PositionableEntity sourceEntity, 
    		PositionableEntity targetCoordSystemEntity,
    		boolean exact) {
    	
    	float[] sceneCoords = 
    		convertLocalCoordinatesToSceneCoordinates(
    				model, 
    				sourceEntity, 
    				positionRelativeToSource, 
    				exact);
    	
    	if (sceneCoords == null) {
    		return null;
    	}
    	
    	float[] targetCoordPos = 
    		convertSceneCoordinatesToLocalCoordinates(
    				model, 
    				sceneCoords, 
    				targetCoordSystemEntity, 
    				exact);
    	
    	return targetCoordPos;
    }
    
    /**
     * Get the rotation of the entity. This process does not consider any 
     * commands that are part of the current evaluation. This will 
     * give a result that is guaranteed correct for the previous frame only.
     * 
     * @param entity Entity to get rotation for
     * @return Axis angle equivalent rotation array
     */
    public static float[] getRotation(PositionableEntity entity) {
    	
    	float[] rotation = new float[4];
    	entity.getRotation(rotation);
    	return rotation;
    }
    
    /**
     * Get the rotation of the entity. This result includes the up-to-date 
     * reflection of commands issued that could have an impact on the start 
     * position of the entity, including the current command being evaluated.
     * 
     * @param entity Entity to get rotation for
     * @return Axis angle equivalent rotation array
     */
    public static float[] getExactRotation(PositionableEntity entity) {
    	
    	ArrayList<Command> cmdList = (ArrayList<Command>) 
		CommandSequencer.getInstance().getFullCommandList(true);
	
		float[] rotation = new float[4];
		entity.getRotation(rotation);
		
		// By starting at the end of the list and working backwards
		// we are guaranteed to get the last rotation command
		// issued affecting the entity.
		
		for (int i = (cmdList.size() - 1); i >= 0; i--) {
			
			Command cmd = cmdList.get(i);
			
			if (cmd instanceof RuleDataAccessor) {
				
				if (((RuleDataAccessor) cmd).getEntity() != entity) {
					continue;
				}
				
				if (cmd instanceof RotateEntityCommand) {
					((RotateEntityCommand)cmd).getCurrentRotation(rotation);
					break;
				} else if (cmd instanceof RotateEntityTransientCommand) {
					((RotateEntityTransientCommand)cmd).getCurrentRotation(
							rotation);
					break;
				}
			}
		}
		
		return rotation;
    }
    
    /**
     * Get the starting rotation of the entity. This result is correct for the 
     * state of rotation at the end of the last frame.
     * 
     * @param entity Entity to get rotation for
     * @return Axis angle equivalent rotation array
     */
    public static float[] getStartRotation(PositionableEntity entity) {
    	
    	float[] rotation = new float[4];
    	entity.getStartingRotation(rotation);
    	return rotation;
    }
    
    /**
     * Get the scale of the entity. This process does not consider any 
     * commands that are part of the current evaluation. This will 
     * give a result that is guaranteed correct for the previous frame only.
     * 
     * @param entity Entity to get scale for
     * @return Scale applied to entity
     */
    public static float[] getScale(PositionableEntity entity) {
    	
    	float[] scale = new float[3];
    	entity.getScale(scale);
    	return scale;
    }
    
    /**
     * Get the scale of the entity. This result includes the up-to-date 
     * reflection of commands issued that could have an impact on the start 
     * position of the entity, including the current command being evaluated.
     * 
     * @param entity Entity to get scale for
     * @return Scale applied to entity
     */
    public static float[] getExactScale(PositionableEntity entity) {
    	
    	ArrayList<Command> cmdList = (ArrayList<Command>) 
		CommandSequencer.getInstance().getFullCommandList(true);
	
		float[] scale = new float[3];
		entity.getScale(scale);
		
		// By starting at the end of the list and working backwards
		// we are guaranteed to get the last scale command
		// issued affecting the entity.
		
		for (int i = (cmdList.size() - 1); i >= 0; i--) {
			
			Command cmd = cmdList.get(i);
			
			if (cmd instanceof RuleDataAccessor) {
				
				if (((RuleDataAccessor) cmd).getEntity() != entity) {
					continue;
				}
				
				if (cmd instanceof ScaleEntityCommand) {
					((ScaleEntityCommand)cmd).getNewScale(scale);
					break;
				} else if (cmd instanceof ScaleEntityTransientCommand) {
					((ScaleEntityTransientCommand)cmd).getScale(scale);
					break;
				}
			}
		}
		
		return scale;
    }
    
    /**
     * Get the starting scale of the entity. This result is correct for the 
     * scale applied at the end of the last frame.
     * 
     * @param entity Entity to get starting scale for
     * @return Starting scale applied to entity
     */
    public static float[] getStartingScale(PositionableEntity entity) {
    	
    	float[] scale = new float[3];
    	entity.getStartingScale(scale);
    	return scale;
    }
		
    /**
     * Sort a list of entities in descending position order relative the zone.
     * This process does not consider any commands that are part of the current 
     * evaluation. This will give a result that is guaranteed correct for the 
     * previous frame only.
     *
     * @param model WorldModel to reference
     * @param originalList List of entities to sort
     * @param relativeTarget Target entity to sort entity positions relative
     * to
     * @param axis Axis to do comparisons along
     * @param exact True to use position data correct for current frame, false
     * to use position data correct for previous frame 
     * @return List of entities or null if there was a problem
     */
    public static ArrayList<Entity> sortDescendingRelativePosValueOrder(
            WorldModel model,
            ArrayList<Entity> originalList,
            PositionableEntity relativeTarget,
            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS axis,
            boolean exact){
    	
    	if (originalList == null) {
    		return null;
    	}

        // Performs insertion sort algorithm to build up the sorted list to
        // return.
        ArrayList<Entity> entityList = new ArrayList<Entity>();
        ArrayList<double[]> posList = new ArrayList<double[]>();

        double[] curPos = new double[3];
        double[] sortedEntityPos = new double[3];
        double[] sceneCoords = new double[3];
        PositionableEntity curEntity = null;

        for(int i = 0; i < originalList.size(); i++) {

            // Extract the next entity to sort and get its position
			if (!(originalList.get(i) instanceof PositionableEntity)) {
				continue;
			}
			
			curEntity = (PositionableEntity)originalList.get(i);
			sceneCoords = 
				getPositionInSceneCoordinates(model, curEntity, exact);
			curPos = convertSceneCoordinatesToLocalCoordinates(
					model, sceneCoords, relativeTarget, exact);

            // Perform insertion operation at correct level
            int index = 0;

            do {

                // Handle belongs at bottom case
                if(index >= entityList.size()) {

                    entityList.add(curEntity);
                    posList.add(curPos);
                    break;

                } else {

                    // Compare check
                    sortedEntityPos = posList.get(index);

                    boolean insertBefore = false;

                    switch(axis) {

                        case XAXIS:
                            if(curPos[0] > sortedEntityPos[0]) {
                                insertBefore = true;
                            }
                            break;

                        case YAXIS:
                            if(curPos[1] > sortedEntityPos[1]) {
                                insertBefore = true;
                            }
                            break;

                        case ZAXIS:
                            if(curPos[2] > sortedEntityPos[2]) {
                                insertBefore = true;
                            }
                            break;
                    }

                    if(insertBefore) {

                        entityList.add(index, curEntity);
                        posList.add(index, curPos);
                        break;
                    }
                }

                index++;

            } while (index < entityList.size());

            if(index >= entityList.size()){

                entityList.add(curEntity);
                posList.add(index, curPos);
            }
        }

        return entityList;
    }
	
    /**
     * Sort a list of entities in descending position order. This process 
     * does not consider any commands that are part of the current evaluation. 
     * This will give a result that is guaranteed correct for the previous frame
     *  only.
     *
     * @param originalList List of entities to sort
     * @param axis Axis to do comparisons along
     * @return List of entities or null if there was a problem
     */
    public static ArrayList<Entity> sortDescendingPosValueOrder(
            ArrayList<Entity> originalList,
            ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS axis){

        // Performs insertion sort algorithm to build up the sorted list to
        // return.
        ArrayList<Entity> newList = new ArrayList<Entity>();

        double[] curPos = new double[3];
        double[] sortedEntityPos = new double[3];
        PositionableEntity curEntity = null;
        PositionableEntity sortedEntity = null;

        for(int i = 0; i < originalList.size(); i++) {

            // Extract the next entity to sort and get its position
			Entity entity = originalList.get(i);
			if (!(entity instanceof PositionableEntity)) {
				continue;
			}
			curEntity = (PositionableEntity)entity;
			curPos = getExactPosition(curEntity);

            // Perform insertion operation at correct level
            int index = 0;

            do {

                // Handle belongs at bottom case
                if(index >= newList.size()) {

                    newList.add(curEntity);
                    break;

                } else {

                    // Compare check
                    sortedEntity = (PositionableEntity) newList.get(index);
                    sortedEntityPos = getExactPosition(sortedEntity);

                    boolean insertBefore = false;

                    switch(axis) {

                        case XAXIS:
                            if(curPos[0] > sortedEntityPos[0]) {
                                insertBefore = true;
                            }
                            break;

                        case YAXIS:
                            if(curPos[1] > sortedEntityPos[1]) {
                                insertBefore = true;
                            }
                            break;

                        case ZAXIS:
                            if(curPos[2] > sortedEntityPos[2]) {
                                insertBefore = true;
                            }
                            break;
                    }

                    if(insertBefore) {

                        newList.add(index, curEntity);
                        break;
                    }
                }

                index++;

            } while (index < newList.size());

            if(index >= newList.size()){

                newList.add(curEntity);
            }
        }
		
        return newList;
    }
	
    /**
     * Determines if a wall is at a specific angle with the adjacent wall.
     * 
     * @param model WorldModel
     * @param entity Current entity to find if the wall it is on is at the 
     * correct angle
     * @param positive When true checks the right end of the wall if false 
     * checks the left end
     * @param angle The angle to check.
     * @return false, the wall is at the right angle or there are no adjacent 
     * walls, true the wall is at a wrong angle
     */
    public static boolean isWallAtSpecificAngle(
            WorldModel model,
            Entity entity,
            Entity parentEntity,
            boolean positive,
            int angle) {

        Entity entityParentZone =  
        	SceneHierarchyUtility.findZoneEntity(model, entity);

        if (parentEntity == null) {
            entityParentZone =  
            	SceneHierarchyUtility.findZoneEntity(model, entity);
        } else {
            entityParentZone = parentEntity;
        }

        if (entityParentZone == null ||
               entityParentZone.getType() != Entity.TYPE_SEGMENT){

            return false;
        }

        // Checks to determine if either wall is not a 90 degree
        // if it is not does not allow the autospan to occur
        Vector3d entityDirectionVector =
            ((SegmentEntity)entityParentZone).getFaceVector();

        Vector3d adjacentDirectionVector = null;
        double currentAngle = 0;
        int angleTruncate= 0;

        SegmentEntity adjacentWall = null;

        SegmentableEntity segmentable =
            (SegmentableEntity)model.getEntity(entityParentZone.getParentEntityID());

        if (positive) {
            adjacentWall = SceneHierarchyUtility.findAdjacentWall(segmentable,
                    ((SegmentEntity)entityParentZone).getEndVertexEntity(),
                     positive);

        } else {

             adjacentWall = 
            	 SceneHierarchyUtility.findAdjacentWall(segmentable,
                    ((SegmentEntity)entityParentZone).getStartVertexEntity(),
                     positive);
        }

        if(adjacentWall == null ||
                adjacentWall.getType() != Entity.TYPE_SEGMENT) {
            return false;
        }

        adjacentDirectionVector = ((SegmentEntity)adjacentWall).getFaceVector();
        currentAngle = entityDirectionVector.angle(adjacentDirectionVector);
        angleTruncate = (int)Math.round(Math.toDegrees(currentAngle));

        if (angleTruncate != angle) {
            return true;
        }

        return false;

    }
	
    /**
     * Get the zone relative start and end positions for the transition entity
     * child command.
     *
     * @param model WorldModel to reference
     * @param command TransitionEntityChildCommand to extract data from
     * @param relativeStart Relative start position
     * @param relativeEnd Releative end position
     */
    public static void getTransitionEntityChildZoneValues(
            WorldModel model,
            TransitionEntityChildCommand command,
            double[] relativeStart,
            double[] relativeEnd) {

        double[] startParentRelPos =
            getPositionRelativeToZone(
                    model,
                    command.getStartParentEntity());

        double[] endParentRelPos =
            getPositionRelativeToZone(
                    model,
                    command.getEndParentEntity());

        command.getStartPosition(relativeStart);
        command.getEndPosition(relativeEnd);

        relativeStart[0] += startParentRelPos[0];
        relativeStart[1] += startParentRelPos[1];
        relativeStart[2] += startParentRelPos[2];

        relativeEnd[0] += endParentRelPos[0];
        relativeEnd[1] += endParentRelPos[1];
        relativeEnd[2] += endParentRelPos[2];
    }
    
    /**
     * Check for uniform scale operations. These may be unbalanced, but the
     * goal is to see if the bounds are no longer within an epsilon value of
     * the starting bounds.
     * 
     * @param entity Entity to check.
     * @param axis Axis to check along.
     * @return True if uniform, false if not uniform,
     * null if NONE TARGET_ADJUSTMENT_AXIS chosen.
     */
    public static Boolean isUniformScale(
    		PositionableEntity entity,
    		TARGET_ADJUSTMENT_AXIS axis) {
    	
    	double EPSILON = 0.00001;
    	
    	// Determine if the current scale is uniform. Do this by checking the
    	// current bounds against the starting bounds. If the edges along the
    	// adjustment axis are both edges have moved then we have some form of
    	// uniform scale, even if it isn't balanced.
    	
    	// Get our base data
    	float[] startingBounds = BoundsUtils.getStartingBounds(entity);
    	float[] newBounds = BoundsUtils.getBounds(entity, true);
    	
    	double[] startPos = getStartPosition(entity);
    	double[] newPos = getExactPosition(entity);
    	
    	// Set our fields to fill for analysis
    	double newPosEdge = 0;
    	double oldPosEdge = 0;
    	double newNegEdge = 0;
    	double oldNegEdge = 0;
    	
    	switch (axis) {
    	
		case XAXIS:
			newPosEdge = newPos[0] + newBounds[1];
			newNegEdge = newPos[0] + newBounds[0];
			oldPosEdge = startPos[0] + startingBounds[1];
			oldNegEdge = startPos[0] + startingBounds[0];
			break;
			
		case YAXIS:
			newPosEdge = newPos[1] + newBounds[3];
			newNegEdge = newPos[1] + newBounds[2];
			oldPosEdge = startPos[1] + startingBounds[3];
			oldNegEdge = startPos[1] + startingBounds[2];
			break;
			
		case ZAXIS:
			newPosEdge = newPos[2] + newBounds[5];
			newNegEdge = newPos[2] + newBounds[4];
			oldPosEdge = startPos[2] + startingBounds[5];
			oldNegEdge = startPos[2] + startingBounds[4];
			break;
			
		default:
			return null;
		}
    	
    	if (Math.abs(newPosEdge - oldPosEdge) > EPSILON &&
    			Math.abs(newNegEdge - oldNegEdge) > EPSILON) {
    		
    		return true;
    	}
    	
    	return false;
    }
	
    /**
     * Checks if scale change is an increase in the overall size. NOTE: will
     * return null if the values are all equal!
     *
     * @param newPos New position
     * @param startPos Start position
     * @param axis Axis to compare along
     * @return True if in positive direction, false if in negative direction,
     * null if NONE TARGET_ADJUSTMENT_AXIS chosen.
     */
    public static Boolean isScaleInPositiveDirection(
            double[] newPos,
            double[] startPos,
            TARGET_ADJUSTMENT_AXIS axis) {

        // Determine the scale edge being scaled and the direction.
        // To do this we need to know if the scale is increasing or decreasing
        // and the resulting direction of the scale offset.
        // Here are the possible combinations:
        //
        // 1) isIncreasingScale (true) & isPositiveDirection (true)
        // = pos edge scale increase
        //
        // 2) isIncreasingScale (true) & isPositiveDirection (false)
        // = neg edge scale increase
        //
        // 3) isIncreasingScale (false) & ositiveDirection (true)
        // = pos edge scale decrease
        //
        // 4) isIncreasingScale (false) & isPositiveDirection (false)
        // = neg edge scale decrease
        //
        // Also storing the fixed entity correction which is the offset that
        // will be applied to all fixed children to keep them in position in
        // world space.

        boolean isPositiveDirection = false;

        switch(axis){

            case XAXIS:

                // Special case handling where we cannot determine scale edge
                // just return an empty command list.
                if (newPos[0] == startPos[0]) {

                    return null;
                }

                if (newPos[0] > startPos[0]) {
                    isPositiveDirection = true;
                } else {
                    isPositiveDirection = false;
                }

                break;

            case YAXIS:

                // Special case handling where we cannot determine scale edge
                // just return an empty command list.
                if (newPos[1] == startPos[1]) {

                    return null;
                }

                if (newPos[1] > startPos[1]) {
                    isPositiveDirection = true;
                } else {
                    isPositiveDirection = false;
                }

                break;

            case ZAXIS:

                // Special case handling where we cannot determine scale edge
                // just return an empty command list.
                if (newPos[2] == startPos[2]) {

                    return null;
                }

                if (newPos[2] > startPos[2]) {
                    isPositiveDirection = true;
                } else {
                    isPositiveDirection = false;
                }

                break;

            default:
                return null;
        }

        return isPositiveDirection;
    }
	
    /**
     * Checks if scale change is an increase in the overall size. NOTE: will
     * return null if the values are all equal!
     *
     * @param newScale New scale
     * @param startScale Starting scale
     * @param axis Axis to perform adjustment along
     * @return True if in positive direction, false if in negative direction,
     * null if NONE TARGET_ADJUSTMENT_AXIS chosen.
     */
    public static Boolean isScaleIncreasing(
            float[] newScale,
            float[] startScale,
            TARGET_ADJUSTMENT_AXIS axis) {

        // Determine the scale edge being scaled and the direction.
        // To do this we need to know if the scale is increasing or decreasing
        // and the resulting direction of the scale offset.
        // Here are the possible combinations:
        //
        // 1) isIncreasingScale (true) & isPositiveDirection (true)
        // = pos edge scale increase
        //
        // 2) isIncreasingScale (true) & isPositiveDirection (false)
        // = neg edge scale increase
        //
        // 3) isIncreasingScale (false) & ositiveDirection (true)
        // = pos edge scale decrease
        //
        // 4) isIncreasingScale (false) & isPositiveDirection (false)
        // = neg edge scale decrease
        //
        // Also storing the fixed entity correction which is the offset that
        // will be applied to all fixed children to keep them in position in
        // world space.

        boolean isIncreasingScale = false;

        switch(axis){

            case XAXIS:

                // Special case handling where we cannot determine scale edge
                // just return an empty command list.
                if (newScale[0] == startScale[0]) {

                    return null;
                }

                if (newScale[0] > startScale[0]) {
                    isIncreasingScale = true;
                } else {
                    isIncreasingScale = false;
                }

                break;

            case YAXIS:

                // Special case handling where we cannot determine scale edge
                // just return an empty command list.
                if (newScale[1] == startScale[1]) {

                    return null;
                }

                if (newScale[1] > startScale[1]) {
                    isIncreasingScale = true;
                } else {
                    isIncreasingScale = false;
                }

                break;

            case ZAXIS:

                // Special case handling where we cannot determine scale edge
                // just return an empty command list.
                if (newScale[2] == startScale[2]) {

                    return null;
                }

                if (newScale[2] > startScale[2]) {
                    isIncreasingScale = true;
                } else {
                    isIncreasingScale = false;
                }

                break;

            default:
                return null;
        }

        return isIncreasingScale;
    }
	
    /**
     * Produces double[] xyz values of axis specific distance between
     * firstEntity and secondEntity. Calculated as firstEntity - secondEntity. 
     * The result will be correct, even if the entities do not share a common
     * parent. The distance will be relative to the secondEntity.
     *
     * @param model WorldModel
     * @param firstEntity First Entity
     * @param secondEntity Second Entity
     * @param exact True to get distance based on existing commands in command
     * queue, false to get distance for previous frame
     * @return double[] xyz order distance values, or null if cannot compute
     */
    public static double[] getDistanceBetweenEntities(
    		WorldModel model, 
    		PositionableEntity firstEntity, 
    		PositionableEntity secondEntity,
    		boolean exact){
    	
    	double[] firstEntityScenePos = 
    		getPositionInSceneCoordinates(model, firstEntity, exact);
    	
    	if (firstEntityScenePos == null) {
    		return null;
    	}
    	
    	double[] relativeDistance = 
    		convertSceneCoordinatesToLocalCoordinates(
    				model, firstEntityScenePos, secondEntity, exact);
    	
    	return relativeDistance;
    	
    }
    
    /**
     * Produces double[] xyz values of axis specific distance between
     * firstEntity and secondEntity. Calculated as firstEntity - secondEntity.
     * This calculates the distance in scene coordinates and gives back a result
     * in scene coordinates. If exact is false this process will not consider 
     * any commands that are part of the current evaluation. This will give a 
     * result that is guaranteed correct for the previous frame only. If exact
     * is true, then the result will be guaranteed correct for the current state
     * of commands in the queue.
     *
     * @param model WorldModel
     * @param firstEntity First Entity
     * @param secondEntity Second Entity
     * @param exact True to use exact positions, false to use previous frame
     * @return double[] xyz order distance values in scene coordinates, 
     * or null if cannot compute
     */
    public static double[] getDistanceBetweenEntitiesSceneCoordinates(
    		WorldModel model, 
    		PositionableEntity firstEntity, 
    		PositionableEntity secondEntity,
    		boolean exact){
        
        Matrix4f firstEntityMatrix;
        Matrix4f secondEntityMatrix;
        
        firstEntityMatrix = getTransformsInSceneCoordinates(
        		model, 
        		firstEntity,
        		exact);
        
        if (firstEntityMatrix == null) {
        	return null;
        }
        
        secondEntityMatrix =getTransformsInSceneCoordinates(
        		model, 
        		secondEntity, 
        		exact);
        	
        if (secondEntityMatrix == null) {
        	return null;
        }
        
        Vector3f firstEntityVec = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f secondEntityVec = new Vector3f(0.0f, 0.0f, 0.0f);
        
        firstEntityMatrix.transform(firstEntityVec);
        secondEntityMatrix.transform(secondEntityVec);

        // Calculate distance
        double[] distanceVals = new double[3];

        distanceVals[0] = firstEntityVec.x - secondEntityVec.x;
        distanceVals[1] = firstEntityVec.y - secondEntityVec.y;
        distanceVals[2] = firstEntityVec.z - secondEntityVec.z;

        return distanceVals;
    }
    
    /**
	 * Process the move command to determine the closest possible position, 
	 * within the tolerance, that is not colliding with the collision set. 
	 * Uses divide and conquer and OrientedBoundingBoxes to derive the result.
	 * All processing is done in scene coordinates with exact position and
	 * parent data, meaning it is as current as it can be, and then converts 
	 * the final position back to the coordinate system of the entity.
	 * </br></br>
	 * There is an important distiction in how the OrientedBoundingBoxes are
	 * generated by this routine. For all target entities, and the testing 
	 * entity, the data is exact, meaning it is correct for the current frame. 
	 * This means all scale, rotation and position data for the 
	 * OrientedBoundingBoxes is correct for the current frame. If any adjustment
	 * to the testing bounding box is needed, supply that transform in the 
	 * optionalTransform parameter and it will be multiplied into the transform
	 * used when testing.
	 *  
	 * @param model WorldModel to reference
	 * @param entity Entity to process
	 * @param optionalTransform Optional transform to apply to entity's oriented 
	 * bounding box, will be multiplied with the calculated transform and should
	 * be in scene coordinates in order to avoid corrupting the resulting data.
	 * @param collisionList List of collisions to process against
	 * @param DISTANCE_TOLERANCE Distance tolerance required for an acceptable 
	 * calculation. This is the distance between edges.
	 * @param EXTENSION_DISTANCE Distance to extend each end of the test 
	 * segment. This is the distance to extend each end of the line segment 
	 * used for testing so we don't end up always in a collision position.
	 * @return New position, or null if there was a problem
	 */
	public static double[] divideAndConquerMoveCommands(
			WorldModel model,
			PositionableEntity entity,
			Matrix4f optionalTransform,
			ArrayList<Entity> collisionList,
			float DISTANCE_TOLERANCE,
			double EXTENSION_DISTANCE) {
		
		////////////////////////////////////////////////////////////////////////
		// NOTE: we will do all of our calculations in scene coordinates and
		// then convert the final result back to local coordinates at the end.
		////////////////////////////////////////////////////////////////////////
		
		// Get the future position and previous position in local coordinates
		// for later use.
		double[] futureScenePosLocal = 
			TransformUtils.getExactPosition(entity);
		
		// Get the future and previous positions in scene coordinates to use
		// in the divide and conquer procedure.
		double[] testEndPosition = 
			TransformUtils.getPositionInSceneCoordinates(model, entity, true);
		
		double[] testStartPosition =
			TransformUtils.getPositionInSceneCoordinates(model, entity, false);
	
		// Create the normalized direction vector 
		// (futureScenePos - previousScenePos)
		Vector3d directionVec = new Vector3d(
				testEndPosition[0] - testStartPosition[0],
				testEndPosition[1] - testStartPosition[1],
				testEndPosition[2] - testStartPosition[2]);
		
		directionVec.normalize();
		directionVec.scale(EXTENSION_DISTANCE);
		
		// Establish the start and end test points that define our line segment.
		// We do this to preserve the original scene start and end scene 
		// coordinates for use later.
		testEndPosition[0] += directionVec.x;
		testEndPosition[1] += directionVec.y;
		testEndPosition[2] += directionVec.z;
		
		directionVec.negate();
		
		testStartPosition[0] += directionVec.x;
		testStartPosition[1] += directionVec.y;
		testStartPosition[2] += directionVec.z;		
	
		// Create the list of Oriented Bounding Boxes for each of the collisions
		ArrayList<OrientedBoundingBox> boundingBoxList = 
			new ArrayList<OrientedBoundingBox>();
		
		for (Entity collision : collisionList) {
			
			OrientedBoundingBox box = BoundsUtils.getOrientedBoundingBox(
					model, (PositionableEntity) collision, true, true);
			
			// If it comes back null, processing fails
			if (box == null) {
				return null;
			}
			
			boundingBoxList.add(box);
		}
		
		// Create the Oriented Bounding Box for our entity to test with but 
		// from the previous frame. Bail if it comes back null. We can't test 
		// without it.
		OrientedBoundingBox testBox = 
			BoundsUtils.getOrientedBoundingBox(model, entity, true, true);
		
		if (testBox == null) {
			return null;
		}		
		
		// Apply initial transform if supplied
		Matrix4f currentTransform = testBox.getTransform();
		AxisAngle4f existingRot = new AxisAngle4f();
		existingRot.set(currentTransform);
		currentTransform.setIdentity();
		currentTransform.setRotation(existingRot);
		
		if (optionalTransform != null) {
			optionalTransform.mul(currentTransform);
		} else {
			optionalTransform = new Matrix4f();
			optionalTransform.setIdentity();
			optionalTransform.mul(currentTransform);
		}
		
		double[] resultPosition = 
			divideAndConquer(
					testEndPosition, 
					testStartPosition, 
					DISTANCE_TOLERANCE, 
					testBox, 
					boundingBoxList,
					optionalTransform);
		
		
		// Convert the final position to the local coordinate system used in
		// the previous frame.
		PositionableEntity parentEntity = (PositionableEntity) 
			SceneHierarchyUtility.getExactParent(model, entity);
		
		double[] localFuturePos = 
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, 
				resultPosition, 
				parentEntity, 
				true);
		
		if (localFuturePos == null) {
			return null;
		}
		
		// Now test along the y and x axis relative to this new local position
		// to see if we can slide in either position based on the cursor
		// position.

		// Test Y axis first ---------------------------------------------------		
		
		float[] clampedY = new float[3];
		clampedY[0] = (float) localFuturePos[0];
		clampedY[1] = (float) futureScenePosLocal[1];
		clampedY[2] = (float) futureScenePosLocal[2];
		
		clampedY = 
			convertLocalCoordinatesToSceneCoordinates(
					model, parentEntity, clampedY, true);
		
		// Create the normalized direction vector 
		// (futureScenePos - previousScenePos)
		directionVec = new Vector3d(
				clampedY[0] - resultPosition[0],
				clampedY[1] - resultPosition[1],
				clampedY[2] - resultPosition[2]);
		
		directionVec.normalize();
		directionVec.scale(EXTENSION_DISTANCE);
		
		boolean skipYClamp = false;
		
		if (Double.isNaN(directionVec.x) || 
				Double.isNaN(directionVec.y) || 
				Double.isNaN(directionVec.z)) {
			
			skipYClamp = true;
		}
				
		if (!skipYClamp) {
			
			// Establish the start and end test points that define our line 
			// segment. These must be in scene coordinates.
			// We do this to preserve the original scene start and end scene 
			// coordinates for use later.
			testEndPosition[0] = clampedY[0];
			testEndPosition[1] = clampedY[1];
			testEndPosition[2] = clampedY[2];
			
			directionVec.negate();
			
			testStartPosition[0] = resultPosition[0] + directionVec.x;
			testStartPosition[1] = resultPosition[1] + directionVec.y;
			testStartPosition[2] = resultPosition[2] + directionVec.z;	
		
			resultPosition = 
				divideAndConquer(
						testEndPosition, 
						testStartPosition, 
						DISTANCE_TOLERANCE, 
						testBox, 
						boundingBoxList,
						optionalTransform);
			
		}
		
		localFuturePos = 
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, 
				resultPosition, 
				parentEntity, 
				true);
		
		if (localFuturePos == null) {
			return null;
		}
		
		// Test X axis next
		
		float[] clampedX = new float[3];
		clampedX[0] = (float) futureScenePosLocal[0];
		clampedX[1] = (float) localFuturePos[1];
		clampedX[2] = (float) futureScenePosLocal[2];
		
		clampedX = 
			convertLocalCoordinatesToSceneCoordinates(
					model, parentEntity, clampedX, true);
		
		// Create the normalized direction vector 
		// (futureScenePos - previousScenePos)
		directionVec = new Vector3d(
				clampedX[0] - resultPosition[0],
				clampedX[1] - resultPosition[1],
				clampedX[2] - resultPosition[2]);
		
		directionVec.normalize();
		directionVec.scale(EXTENSION_DISTANCE);
		
		boolean skipXClamp = false;
		
		if (Double.isNaN(directionVec.x) || 
				Double.isNaN(directionVec.y) || 
				Double.isNaN(directionVec.z)) {
			skipXClamp = true;
		}
		
		if (!skipXClamp) {
			
			// Establish the start and end test points that define our line segment.
			// We do this to preserve the original scene start and end scene 
			// coordinates for use later.
			testEndPosition[0] = clampedX[0];// + directionVec.x;
			testEndPosition[1] = clampedX[1];// + directionVec.y;
			testEndPosition[2] = clampedX[2];// + directionVec.z;
			
			directionVec.negate();
			
			testStartPosition[0] = resultPosition[0] + directionVec.x;
			testStartPosition[1] = resultPosition[1] + directionVec.y;
			testStartPosition[2] = resultPosition[2] + directionVec.z;
			
			resultPosition = 
				divideAndConquer(
						testEndPosition, 
						testStartPosition, 
						DISTANCE_TOLERANCE, 
						testBox, 
						boundingBoxList,
						optionalTransform);
			
		}
		
		localFuturePos = 
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, 
				resultPosition, 
				parentEntity, 
				true);
		
		if (localFuturePos == null) {
			return null;
		}
			
		// Finally, flatten to current frame's z
		localFuturePos[2] = futureScenePosLocal[2];
			
		return localFuturePos;
	}
   
    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------
	
	/**
	 * Perform divide and conquer search algorithm to find the first position, 
	 * within DISTANCE_TOLERANCE, that is not colliding with the target bounding
	 *  boxes.
	 *  
	 * @param testEndPosition Proposed end position that results in collision 
	 * with targets
	 * @param testStartPosition Previous position that is not in collision with
	 * any targets
	 * @param DISTANCE_TOLERANCE distance between test points at which we will
	 * stop searching if there are no collision
	 * @param testBox OrientedBoundingBox to test with
	 * @param boundingBoxList ArrayList of OrientedBoundingBox targets to test
	 * against.
	 * @param optionalTransform Optional transform to apply to entity's oriented 
	 * bounding box, will be multiplied with the calculated transform and should
	 * be in scene coordinates in order to avoid corrupting the resulting data.
	 * @return First closest position where there are no collisions with targets
	 * , or null if there was a problem
	 */
	private static double[] divideAndConquer(
			double[] testEndPosition, 
			double[] testStartPosition,
			float DISTANCE_TOLERANCE,
			OrientedBoundingBox testBox,
			ArrayList<OrientedBoundingBox> boundingBoxList,
			Matrix4f optionalTransform) {
		
		// Prepare data for divide and conquer process.
		// We will always track the head, tail and test positions.
		// These values will be used to generate the transformVec applied to
		// the testBox that represents our moving entity.
		Point3f headTestPosition = new Point3f(
				(float)testEndPosition[0],
				(float)testEndPosition[1],
				(float)testEndPosition[2]);
		
		Point3f tailTestPosition = new Point3f(
				(float)testStartPosition[0],
				(float)testStartPosition[1],
				(float)testStartPosition[2]);
		
		Point3f testPosition = new Point3f();
		
		Vector3f transformVec = new Vector3f();
		transformVec.sub(headTestPosition, tailTestPosition);
		
		// Get the test position that is 1/2 the length of the transformVec that
		// represents our head position - start position.
		float length = transformVec.length();
		transformVec.normalize();
		transformVec.scale(length/2.0f);
		
		// Set our test position to the tail position + the transformVec.
		// Then set ouir transformVec to the value of testPosition and use
		// that to translate the testBox to that position.
		testPosition.x = (tailTestPosition.x + transformVec.x);
		testPosition.y = (tailTestPosition.y + transformVec.y);
		testPosition.z = (tailTestPosition.z + transformVec.z);
		
		transformVec.set(testPosition);
		
		if (Float.isNaN(transformVec.x) || 
				Float.isNaN(transformVec.y) || 
				Float.isNaN(transformVec.z)) {
			return null;
		}
		
		// Transform the testBox to the testPosition.
		Matrix4f transformMatrix = new Matrix4f();
		transformMatrix.setIdentity();
		transformMatrix.setTranslation(transformVec);
		
		// Apply optional transform if supplied
		if (optionalTransform != null) {
			transformMatrix.mul(optionalTransform);
		}
		
		testBox.transform(transformMatrix);
		
		// This is the value that will end up holing out final transform
		// position that is the position just before collision occurs.
		Point3f finalTransform = new Point3f();
		
		// Let's get this party started! Loop until there isn't a collision
		// and the legth is less than the DISTANCE_TOLERANCE
		boolean collision = false;
		int counter = 0;
		while ((length > DISTANCE_TOLERANCE || collision) && counter < 1000) {
			
			counter++;
			collision = false;
			
			for (OrientedBoundingBox box : boundingBoxList) {
				if (testBox.intersect(box)) {
					collision = true;
					break;
				}
			}
			
			// If there was a collision, head back towards the tail.
			// If there wasn't a collision, head towards the head.
			if (collision) {
				headTestPosition.set(testPosition);
				transformVec.sub(headTestPosition, tailTestPosition);
			} else {
				finalTransform.set(testPosition);
				tailTestPosition.set(testPosition);
				transformVec.sub(headTestPosition, tailTestPosition);
			}
			
			length = transformVec.length();
			transformVec.normalize();
			transformVec.scale(length/2.0f);
			
			testPosition.x = (tailTestPosition.x + transformVec.x);
			testPosition.y = (tailTestPosition.y + transformVec.y);
			testPosition.z = (tailTestPosition.z + transformVec.z);
			
			transformVec.set(testPosition);
		
			// temporary measure
			if (Float.isNaN(transformVec.x) || 
					Float.isNaN(transformVec.y) || 
					Float.isNaN(transformVec.z)) {
				
				return null;
			}
			
			transformMatrix.setIdentity();
			transformMatrix.setTranslation(transformVec);
			
			// Apply optional transform if supplied
			if (optionalTransform != null) {
				transformMatrix.mul(optionalTransform);
			}
			
			testBox.transform(transformMatrix);
		}
		
		double[] finalPosition = new double[3];

		finalPosition[0] = finalTransform.x;
		finalPosition[1] = finalTransform.y;
		finalPosition[2] = finalTransform.z;
		
		return finalPosition;
	}
    
    /**
     * Get the position of an entity relative to the relativeEntity. If the
     * relativeEntity is not found, the position returned will be relative
     * to the parent zone. If we hit the location entity before the specified 
     * relativeEntity then we will return at that point. This should never be
     * the case.
     *
     * @param model WorldModel
     * @param startEntity Starting entity to build up position data from
     * @param relativeEntity Entity to stop building position data at
     * @param useStartPosition Set true if position should be derived from
     * start positions
     * @param exact True to get the exact current frame results, false to get
     * the previous frame results
     * @return double[] xyz pos relative to relativeEntity, or null if unable
     * to calculate
     */
    private static double[] getRelativePosition(
            WorldModel model,
            Entity startEntity,
            Entity relativeEntity,
            boolean useStartPosition,
            boolean exact){

        if (startEntity == null ||
                relativeEntity == null) {
            return null;
        }
        
        // If startEntity and relativeEntity are the same then return 
        // zero position.
        if (startEntity == relativeEntity) {
        	return (new double[] {0.0, 0.0, 0.0});
        }

        double[] posTotal = new double[3];
        double[] tmpPos = new double[3];

        int parentEntityID = -1;
        Entity parentEntity = null;
        
        if (!exact) {
        	parentEntityID = startEntity.getParentEntityID();
        	parentEntity = model.getEntity(parentEntityID);
        } else {
        	parentEntity = 
        		SceneHierarchyUtility.getExactParent(
        				model, startEntity);
        	
        	if (parentEntity != null) {
        		parentEntityID = parentEntity.getEntityID();
        	}
        }

        // Check for initial parent ID if this is an add instance that
        // is not yet in the scene. This is identified by the parent entity ID
        // being equal to -1. In this case, look for side pocketed parent ID.
        if (parentEntityID == -1) {

            Integer tmpParentEntityID = (Integer)
                RulePropertyAccessor.getRulePropertyValue(
                        startEntity,
                        ChefX3DRuleProperties.INITAL_ADD_PARENT);

            if (tmpParentEntityID != null) {
                parentEntityID = tmpParentEntityID;
                parentEntity = model.getEntity(parentEntityID);
            }
        }
        
        // Get the start position or current position
        if (!exact) {
        	
	        if (useStartPosition){
	        	posTotal = 
	        		getStartPosition((PositionableEntity)startEntity);
	        } else {
	        	posTotal = getPosition((PositionableEntity)startEntity);
	        }
	        
        } else {
        	
        	if (useStartPosition){
	        	posTotal = 
	        		getExactStartPosition((PositionableEntity)startEntity);
	        } else {
	        	posTotal = getExactPosition((PositionableEntity)startEntity);
	        }
        	
        }
        
        // Have to have a parent entity to continue search
        if(parentEntity == null ||
        		!(parentEntity instanceof PositionableEntity)){

            return null;
        }

        // Work up the tree
        while(parentEntity.getType() != Entity.TYPE_LOCATION){

            // If we match the relative entity we have searched far back
            // enough.
            if (parentEntity.getEntityID() ==
                relativeEntity.getEntityID()) {
                break;
            }

            if(parentEntity instanceof PositionableEntity){

            	if (!exact) {
            		
	                if(useStartPosition){
	                	tmpPos = 
	                		getStartPosition((PositionableEntity)parentEntity);
	                } else {
	                	tmpPos = getPosition((PositionableEntity)parentEntity);
	                }
	                
            	} else {
            		
            		if(useStartPosition){
	                	tmpPos = 
	                		getExactStartPosition(
	                				(PositionableEntity)parentEntity);
	                } else {
	                	tmpPos = getExactPosition(
	                			(PositionableEntity)parentEntity);
	                }
            		
            	}

                posTotal[0] = posTotal[0] + tmpPos[0];
                posTotal[1] = posTotal[1] + tmpPos[1];
                posTotal[2] = posTotal[2] + tmpPos[2];

                if (!exact) {
                	parentEntityID = parentEntity.getParentEntityID();
                	parentEntity = model.getEntity(parentEntityID);
                } else {
                	parentEntity = 
                		SceneHierarchyUtility.getExactParent(
                				model, parentEntity);
                	
                	if (parentEntity == null) {
                		parentEntityID = -1;
                	} else {
                		parentEntityID = parentEntity.getEntityID();
                	}
                }

                // Check for initial parent ID if this is an add instance that
                // is not yet in the scene. This is identified by the parent entity ID
                // being equal to -1. In this case, look for side pocketed parent ID.
                if (parentEntityID == -1) {

                    Integer tmpParentEntityID = (Integer)
                        RulePropertyAccessor.getRulePropertyValue(
                                parentEntity,
                                ChefX3DRuleProperties.INITAL_ADD_PARENT);

                    if (tmpParentEntityID != null) {
                        parentEntityID = tmpParentEntityID;
                    } else {
                    	return null;
                    }
                    
                    parentEntity = model.getEntity(parentEntityID);
                }
                
            } else {

                return null;
            }
        }

        return posTotal;
    }
    
    /**
     * Get position relative to the parent zone. Do so by stripping the end
     * position, entity and parent data from the command.
     *
     * @param model WorldModel to reference
     * @param command Command to pull data from
     * @param boolean True to get exact positions, false otherwise
     * @return Position relative to zone, or null if command cannot be read
     */
    private static double[] getPositionRelativeToZone(
    		WorldModel model,
    		Command command,
    		boolean exact) {
    	
    	PositionableEntity entity = null;
        double[] endPosition = new double[3];
        double[] currentPosition = new double[3];
        int endParentEntityID = -1;
        int currentParentEntityID = -1;

        double[] results;

        if (command instanceof AddEntityChildCommand) {

            entity = (PositionableEntity)
                ((AddEntityChildCommand)command).getEntity();
            entity.getPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID =
                ((AddEntityChildCommand)
                        command).getParentEntity().getEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof AddEntityChildTransientCommand) {

            entity = (PositionableEntity)
                ((AddEntityChildTransientCommand)command).getEntity();
            entity.getPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID = entity.getParentEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof AddEntityCommand) {

            entity = (PositionableEntity)
                ((AddEntityChildTransientCommand)command).getEntity();
            entity.getPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID = entity.getParentEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof MoveEntityTransientCommand) {

            entity = (PositionableEntity)
                ((MoveEntityTransientCommand)command).getEntity();
            ((MoveEntityTransientCommand)command).getPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID = entity.getParentEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof MoveEntityCommand) {

            entity = (PositionableEntity)
                ((MoveEntityCommand)command).getEntity();
            ((MoveEntityCommand)command).getEndPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID = entity.getParentEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof TransitionEntityChildCommand) {

            entity = (PositionableEntity)
                ((TransitionEntityChildCommand)command).getEntity();
            ((TransitionEntityChildCommand)
                    command).getEndPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID =
                ((TransitionEntityChildCommand)
                        command).getEndParentEntity().getEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof ScaleEntityCommand) {

            entity = (PositionableEntity)
                ((ScaleEntityCommand)command).getEntity();
            ((ScaleEntityCommand)command).getNewPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID = entity.getParentEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else if (command instanceof ScaleEntityTransientCommand) {

            entity = (PositionableEntity)
                ((ScaleEntityTransientCommand)command).getEntity();
            ((ScaleEntityTransientCommand)command).getPosition(endPosition);
            entity.getPosition(currentPosition);
            endParentEntityID = entity.getParentEntityID();
            currentParentEntityID = entity.getParentEntityID();

        } else {

            return null;
        }

        // Default return value if could not determine entity from
        // accepted commands
        if (entity == null) {
            return null;
        }

        if (!exact) {
	        // Set the values for our testing purposes
	        entity.setPosition(endPosition, false);
	        entity.setParentEntityID(endParentEntityID);
	
	        // Perform test
	        results = getRelativePosition(
	                model,
	                entity,
	                SceneHierarchyUtility.findZoneEntity(model, entity),
	                false);
        } else {
        	
        	endPosition = getExactPosition(entity);
        	
        	Entity parent = SceneHierarchyUtility.getExactParent(model, entity);
        	
        	if (parent == null) {
        		return null;
        	}
        	
        	endParentEntityID = parent.getEntityID();
        	
        	// Set the values for our testing purposes
	        entity.setPosition(endPosition, false);
	        entity.setParentEntityID(endParentEntityID);
	
	        // Perform test
        	results = getExactRelativePosition(
        			model, 
        			entity, 
        			SceneHierarchyUtility.findExactZoneEntity(model, entity), 
        			false);
        }

        // Set the values back post testing
        entity.setPosition(currentPosition, false);
        entity.setParentEntityID(currentParentEntityID);
        
        return results;
    }
    
    /**
     * Get the position of an entity relative to the first wall or floor
     * parent found. All other zone types are ignored and position is 
     * calculated only relative to the first wall or floor parent found.
     * Can calculate position based on starting positions or current
     * positions as well as exact positions or not.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to examine
     * @param useStartPosition True to use start positions only, false 
     * otherwise
     * @param exact True to get exact positions, false otherwise
     * @return Position relative to wall or floor or null if not able
     * to calculate
     */
    private static double[] getPositionRelativeToWallOrFloor(
    		WorldModel model,
    		Entity entity,
    		boolean useStartPosition,
    		boolean exact) {
    	
    	if (entity == null) {
            return null;
        }

        double[] posTotal = new double[3];
        double[] tmpPos = new double[3];

        int parentEntityID = -1;
        
        if (!exact) {
        	parentEntityID = entity.getParentEntityID();
        } else {
        	parentEntityID = 
        		SceneHierarchyUtility.getExactParent(
        				model, entity).getEntityID();
        }

        // Check for initial parent ID if this is an add instance that
        // is not yet in the scene. This is identified by the parent entity ID
        // being equal to -1. In this case, look for side pocketed parent ID.
        if (parentEntityID == -1) {

            Integer tmpParentEntityID = (Integer)
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.INITAL_ADD_PARENT);

            if (tmpParentEntityID != null) {
                parentEntityID = tmpParentEntityID;
            } else {
            	return null;
            }
        }
        
        // Get the position of the entity, either current or start
        if (!exact) {
        	
	        if (useStartPosition){
	        	posTotal = getStartPosition((PositionableEntity)entity);
	        } else {
	        	posTotal = getPosition((PositionableEntity)entity);
	        }
	        
        } else {
        	
        	if (useStartPosition){
	        	posTotal = getExactStartPosition((PositionableEntity)entity);
	        } else {
	        	posTotal = getExactPosition((PositionableEntity)entity);
	        }
        	
        }

        // If the entity is a wall or floor, then we are done
        if (entity.getType() == Entity.TYPE_SEGMENT || 
        		entity.getType() == Entity.TYPE_GROUNDPLANE_ZONE) {
        	
            return posTotal;
            
        }
        
        // Check the parent entity, and possibly bail if it is null
        // or not a positionable entity
        Entity parentEntity = model.getEntity(parentEntityID);
        
        if(parentEntity == null || 
        		!(parentEntity instanceof PositionableEntity)){
            return null;
        }

        while(parentEntity.getType() != Entity.TYPE_SEGMENT && 
        		parentEntity.getType() != Entity.TYPE_MODEL_ZONE){

            if(parentEntity instanceof PositionableEntity){

            	if (!exact) {
            		
	                if(useStartPosition){
	                	tmpPos = 
	                		getStartPosition((PositionableEntity)parentEntity);
	                } else {
	                	tmpPos = getPosition((PositionableEntity)parentEntity);
	                }
	                
            	} else {
            		
            		if(useStartPosition){
	                	tmpPos = 
	                		getExactStartPosition(
	                				(PositionableEntity)parentEntity);
	                } else {
	                	tmpPos = getExactPosition(
	                			(PositionableEntity)parentEntity);
	                }
            		
            	}

                posTotal[0] = posTotal[0] + tmpPos[0];
                posTotal[1] = posTotal[1] + tmpPos[1];
                posTotal[2] = posTotal[2] + tmpPos[2];
                
                if (!exact) {
                	parentEntityID = parentEntity.getParentEntityID();
                } else {
                	parentEntityID = 
                		SceneHierarchyUtility.getExactParent(
                				model, parentEntity).getEntityID();
                }

                // Check for initial parent ID if this is an add instance that
                // is not yet in the scene. This is identified by the parent entity ID
                // being equal to -1. In this case, look for side pocketed parent ID.
                if (parentEntityID == -1) {

                    Integer tmpParentEntityID = (Integer)
                        RulePropertyAccessor.getRulePropertyValue(
                                parentEntity,
                                ChefX3DRuleProperties.INITAL_ADD_PARENT);

                    if (tmpParentEntityID != null) {
                        parentEntityID = tmpParentEntityID;
                    } else {
                    	return null;
                    }
                }

                parentEntity = model.getEntity(parentEntityID);

            } else {

                return null;
            }
        }

        return posTotal;
    }
    
}
