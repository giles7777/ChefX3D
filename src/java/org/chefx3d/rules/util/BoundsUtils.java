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
import java.util.Arrays;
import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.AxisAngle4f;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.view.boundingbox.ExtrusionBoundingBox;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;
import org.chefx3d.view.boundingbox.SegmentBoundingBox;

/**
 * Utility methods for performing various bounding calculations.
 *
 * @author Ben Yarger
 * @version $Revision: 1.37 $
 */
public abstract class BoundsUtils {
	
    /** Constant threshold value used in angle calculations */
    private static final double ANGLE_CHECK_THRESHOLD = 0.0001;

    /** Auto-span Overlap threshold */
    public static final float SPAN_OVERLAP_THRESHOLD = 0.002f;

    /**
     * Get the local bounds of this entity with exact bounds option.
     *
     * @param entity The entity to get the bounds forhe
     * @param exact True to get bounds in current frame, false to get bounds
     * in previous frame.
     * @return bounds result
     */
    public static float[] getBounds(
            PositionableEntity entity,
            boolean exact){
  	
    	float[] bounds = new float[6];
        float[] size = new float[3];
        float[] scale = new float[3];
        double[] originOffset = new double[3];

        entity.getSize(size);
        entity.getScale(scale);
        entity.getOriginOffset(originOffset);
        
        // Handle the special extrusion bounds case, otherwise do standard
        // processing.
		Object isExtrusionProp = entity.getProperty(
			entity.getParamSheetName(),
			ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);
		
		if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
			
			boolean isExtrusion = ((Boolean)isExtrusionProp).booleanValue();
			
			if (isExtrusion) {
				
				if (exact) {
					
					// This is in no way perfect. But for the case where
					// scaling is occurring the spine is constricted to be just
					// the front edge. In that case this works. Likely isn't
					// perfectly accurate though for the final non-transient
					// case but we are ok with that for now.
					float[] startingScale = new float[3];
					entity.getStartingScale(startingScale);
					
					float[] exactScale = TransformUtils.getExactScale(entity);
					
					// If exactScale is the default state of 1,1,1 set it to
					// startingScale.
					if (Arrays.equals(
							exactScale, 
							new float[] {1.0f, 1.0f, 1.0f})) {
						
						exactScale[0] = startingScale[0];
						exactScale[1] = startingScale[1];
						exactScale[2] = startingScale[2];
					}
					
					bounds[0] = -(exactScale[0] * size[0])/2.0f;
			        bounds[1] = (exactScale[0] * size[0])/2.0f;
			        bounds[2] = -(exactScale[1] * size[1])/2.0f;
			        bounds[3] = (exactScale[1] * size[1])/2.0f;
			        bounds[4] = -(exactScale[2] * size[2])/2.0f;
			        bounds[5] = (exactScale[2] * size[2])/2.0f;		
				
				} else {
					
					ExtrusionBoundingBox ebb = new ExtrusionBoundingBox();
					
					// Get the translation applied to the extrusion shape
					float[] cs_translation = (float[]) 
						entity.getProperty(
								Entity.DEFAULT_ENTITY_PROPERTIES, 
								ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);
					
					if (cs_translation == null) {
						cs_translation = new float[] {0.0f, 0.0f, 0.0f};
					}
					
					// Calculate the extrusion shape min and max extents
					float[] sizeParam = new float[3];
					entity.getSize(sizeParam);
				
					float[] cs_extent = new float[6];
					// min extents
					cs_extent[0] = 0.0f;
					cs_extent[1] = -(sizeParam[1]/2.0f);
					cs_extent[2] = -(sizeParam[2]/2.0f);
					
					// max extents
					cs_extent[3] = 0.0f;
					cs_extent[4] = (sizeParam[1]/2.0f);
					cs_extent[5] = (sizeParam[2]/2.0f);
					
					// Get the spline of the extrusion
					float[] spine = (float[])
						entity.getProperty(
								Entity.DEFAULT_ENTITY_PROPERTIES, 
								ExtrusionEntity.SPINE_VERTICES_PROP);
					
					// Get the visibility values of the extrusion
					boolean[] visible = (boolean[])
						entity.getProperty(
								Entity.DEFAULT_ENTITY_PROPERTIES, 
								ExtrusionEntity.VISIBLE_PROP);
					
					boolean[] miterEnable = (boolean[])
						entity.getProperty(
								Entity.DEFAULT_ENTITY_PROPERTIES, 
								ExtrusionEntity.MITER_ENABLE_PROP);
					
					ebb.update(
							cs_extent, 
							cs_translation, 
							spine, 
							visible, 
							miterEnable);
					
					float[] min = new float[3];
					float[] max = new float[3];
					ebb.getExtents(min, max);
					
					bounds[0] = -(max[0] - min[0])/2.0f;
			        bounds[1] = (max[0] - min[0])/2.0f;
			        bounds[2] = -(max[1] - min[1])/2.0f;
			        bounds[3] = (max[1] - min[1])/2.0f;
			        bounds[4] = -(max[2] - min[2])/2.0f;
			        bounds[5] = (max[2] - min[2])/2.0f;
			        
				}
			}
			
		} else {
        
	        if (exact) {
	        	
	        	// Get all of the commands on the queues
	         	ArrayList<Command> cmdList = (ArrayList<Command>) 
	         		CommandSequencer.getInstance().getFullCommandList(true);
	         	
	         	// By starting at the end of the list and working backwards
	         	// we are guaranteed to get the last command
	         	// issued affecting the entity.
	         	
	         	for (int i = (cmdList.size() - 1); i >= 0; i--) {
	         		
	         		Command cmd = cmdList.get(i);
	         		
	         		if (cmd instanceof RuleDataAccessor) {
	         			
	         			// If it isn't an entity match, skip to next command
	         			if (((RuleDataAccessor)cmd).getEntity() != entity) {
	         				continue;
	         			}
	         		
	         			// Try and match the command to something we care about.
	         			// Something that will adjust the parenting of the entity.
	     	    		if (cmd instanceof ScaleEntityCommand) {
	     	    			
	     	    			((ScaleEntityCommand)cmd).getNewScale(scale);
	     	    			break;
	     	    			
	     	    		} else if (cmd instanceof ScaleEntityTransientCommand) {
	     	    			
	     	    			((ScaleEntityTransientCommand)cmd).getScale(scale);
	     	    			break;
	     	    			
	     	    		}
	         		}   
	         	}
	        }
	
	        float halfXWidth = (size[0]*scale[0])/2f;
	        float halfYWidth = (size[1]*scale[1])/2f;
	        float halfZWidth = (size[2]*scale[2])/2f;
	
	        bounds[0] = -halfXWidth + (float)originOffset[0];
	        bounds[1] = halfXWidth + (float)originOffset[0];
	        bounds[2] = -halfYWidth + (float)originOffset[1];
	        bounds[3] = halfYWidth + (float)originOffset[1];
	        bounds[4] = -halfZWidth + (float)originOffset[2];
	        bounds[5] = halfZWidth + (float)originOffset[2];
		}
        
        return bounds;
    }
    
    /**
     * Get the starting bounds of an entity. Starting bounds is the last known
     * good bounds of the entity before any transient commands were applied.
     * 
     * @param entity Entity to get starting bounds for.
     * @return Bounds of the entity
     */
    public static float[] getStartingBounds(
    		PositionableEntity entity) {

    	float[] startingBounds = new float[6];
    	
    	// Handle the special extrusion bounds case, otherwise do standard
        // processing.
		Object isExtrusionProp = entity.getProperty(
			entity.getParamSheetName(),
			ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);
		
		if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
			
			boolean isExtrusion = ((Boolean)isExtrusionProp).booleanValue();
			
			if (isExtrusion) {
				
				ExtrusionBoundingBox ebb = new ExtrusionBoundingBox();
				
				// Get the translation applied to the extrusion shape
				float[] cs_translation = (float[]) 
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);
				
				if (cs_translation == null) {
					cs_translation = new float[] {0.0f, 0.0f, 0.0f};
				}
				
				// Calculate the extrusion shape min and max extents
				float[] sizeParam = new float[3];
				entity.getSize(sizeParam);
			
				float[] cs_extent = new float[6];
				// min extents
				cs_extent[0] = 0.0f;
				cs_extent[1] = -(sizeParam[1]/2.0f);
				cs_extent[2] = -(sizeParam[2]/2.0f);
				
				// max extents
				cs_extent[3] = 0.0f;
				cs_extent[4] = (sizeParam[1]/2.0f);
				cs_extent[5] = (sizeParam[2]/2.0f);
				
				// Get the spline of the extrusion
				float[] spine = (float[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
				
				// Get the visibility values of the extrusion
				boolean[] visible = (boolean[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.VISIBLE_PROP);
				
				boolean[] miterEnable = (boolean[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.MITER_ENABLE_PROP);
				
				ebb.update(
						cs_extent, 
						cs_translation, 
						spine, 
						visible, 
						miterEnable);
				
				float[] min = new float[3];
				float[] max = new float[3];
				ebb.getExtents(min, max);
				
				startingBounds[0] = -(max[0] - min[0])/2.0f;
				startingBounds[1] = (max[0] - min[0])/2.0f;
				startingBounds[2] = -(max[1] - min[1])/2.0f;
				startingBounds[3] = (max[1] - min[1])/2.0f;
				startingBounds[4] = -(max[2] - min[2])/2.0f;
				startingBounds[5] = (max[2] - min[2])/2.0f;
			}
		} else {
    	
	    	float[] startingScale = new float[3];
	    	float[] size = new float[3];
	    	double[] originOffset = new double[3];
	        
	    	entity.getSize(size);
	    	entity.getStartingScale(startingScale);
	    	entity.getOriginOffset(originOffset);
	    	
	    	startingBounds[0] = 
	    		- (size[0] * startingScale[0] / 2.0f) + (float)originOffset[0];
	    	startingBounds[1] = 
	    		(size[0] * startingScale[0] / 2.0f) + (float)originOffset[0];
	    	startingBounds[2] = 
	    		- (size[1] * startingScale[1] / 2.0f) + (float)originOffset[1];
	    	startingBounds[3] = 
	    		(size[1] * startingScale[1] / 2.0f) + (float)originOffset[1];
	    	startingBounds[4] = 
	    		- (size[2] * startingScale[2] / 2.0f) + (float)originOffset[2];
	    	startingBounds[5] = 
	    		(size[2] * startingScale[2] / 2.0f) + (float)originOffset[2];
	    	
		}
    	
    	return startingBounds;
    }

    /**
     * Checks the child object against the segment bounds to see
     * if it is within the bounds of the segment. Returns true if it is
     * within bounds false otherwise.
     *
     * @param model WorldModel
     * @param entity Entity
     * @param leftVertexHeight Float if null, value found will be used
     * @param rightVertexHeight Float if null, value found will be used
     * @param newWallLength Double if null, the length of the segment found will
     *  be used
     * @return True if in bounds, false otherwise
     */
    public static boolean performSegmentBoundsCheck(
            WorldModel model,
            PositionableEntity entity,
            Float leftVertexHeight,
            Float rightVertexHeight,
            Double newWallLength){

    	ZoneEntity parentZone = 
    		SceneHierarchyUtility.findExactZoneEntity(model, entity);
    	
    	if (parentZone == null) {
    		return false;
    	}
    	
        if (parentZone.getType() == Entity.TYPE_SEGMENT){

            SegmentEntity wallEntity = (SegmentEntity)parentZone;

            // Get vectors and extract positions
            VertexEntity startVertexEntity = 
            	wallEntity.getStartVertexEntity();
            VertexEntity endVertexEntity = 
            	wallEntity.getEndVertexEntity();

            double[] startVertexPos = new double[3];
            double[] endVertexPos = new double[3];

            startVertexEntity.getPosition(startVertexPos);
            endVertexEntity.getPosition(endVertexPos);

            double wallLength = 0;
            
            if(newWallLength == null) {
	            Vector3d wallVec = new Vector3d(
	                    endVertexPos[0] - startVertexPos[0],
	                    endVertexPos[1] - startVertexPos[1],
	                    0.0);
	
	            wallLength = wallVec.length();
            } else {
                wallLength = newWallLength;
            }

            // Create the 4 wall coordinates
            double topLeftHeight = 0.0;
            double topRightHeight = 0.0;

            if(leftVertexHeight != null){
                topLeftHeight = leftVertexHeight;
            } else {
                topLeftHeight = startVertexEntity.getHeight();
            }

            if(rightVertexHeight != null){
                topRightHeight = rightVertexHeight;
            } else {
                topRightHeight = endVertexEntity.getHeight();
            }

            double[] topLeft = {
                    0.0,
                    topLeftHeight,
                    0.0};

            double[] bottomLeft = {
                    0.0,
                    0.0,
                    0.0};

            double[] bottomRight = {
                    wallLength,
                    0.0,
                    0.0};

            double[] topRight = {
                    wallLength,
                    topRightHeight,
                    0.0};

            // Get the final entity bounds
            float[] bounds = getBounds(entity, true);
            
            // Get the final entity position, flatten it to the x,y plane.
            double[] pos = 
            	TransformUtils.getPositionRelativeToZone(model, entity, true);
            
            if (pos == null) {
            	return false;
            }
            
            pos[2] = 0.0;

            // Perform special case adjustment to auto spanning products
            Boolean spanObject = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.SPAN_OBJECT_PROP);

            if (spanObject != null && spanObject) {
                bounds[0] += SPAN_OVERLAP_THRESHOLD;
                bounds[1] -= SPAN_OVERLAP_THRESHOLD;
            }

            //----------------------------------------------------------
            // Create the 4 coordinates to test (top left, bottom left,
            // bottom right, top right) based on the object position and
            // bounds.
            //----------------------------------------------------------
            double[] testCoords = {
                    pos[0]+bounds[0],
                    pos[1]+bounds[3],
                    0.0,

                    pos[0]+bounds[0],
                    pos[1]+bounds[2],
                    0.0,

                    pos[0]+bounds[1],
                    pos[1]+bounds[2],
                    0.0,

                    pos[0]+bounds[1],
                    pos[1]+bounds[3],
                    0.0};

            // Perform polygon angle summation to determine if inside or 
            // outside wall. Do this by creating a vector from each point of the
            // positioned bounds of our entity to each corner of the wall, and
            // then calculating the total angle of those vectors.
            
            Vector3d topLeftVector;
            Vector3d bottomLeftVector;
            Vector3d bottomRightVector;
            Vector3d topRightVector;

            for(int i = 0; i < 4; i++){

                topLeftVector = new Vector3d(
                        topLeft[0] - testCoords[i*3],
                        topLeft[1] - testCoords[i*3+1],
                        topLeft[2] - testCoords[i*3+2]);

                bottomLeftVector = new Vector3d(
                        bottomLeft[0] - testCoords[i*3],
                        bottomLeft[1] - testCoords[i*3+1],
                        bottomLeft[2] - testCoords[i*3+2]);

                bottomRightVector = new Vector3d(
                        bottomRight[0] - testCoords[i*3],
                        bottomRight[1] - testCoords[i*3+1],
                        bottomRight[2] - testCoords[i*3+2]);

                topRightVector = new Vector3d(
                        topRight[0] - testCoords[i*3],
                        topRight[1] - testCoords[i*3+1],
                        topRight[2] - testCoords[i*3+2]);

                // Angle summation only works for triangles, so we need to
                // do this angle check twice (two triangles per wall).
                // We will check the following combination:
                // 1) top left, bottom left, top right
                // 2) top right, bottom left, bottom right
                // As long as one of these evaulate to true, we are ok
                
                double radianTestA = 0.0;
                
                double radians1 = topLeftVector.angle(bottomLeftVector);
                double radians2 = bottomLeftVector.angle(topRightVector);
                double radians3 = topRightVector.angle(topLeftVector);
                
                radianTestA = radians1 + radians2 + radians3;
                
                double radianTestB = 0.0;
                
                radians1 = topRightVector.angle(bottomLeftVector);
                radians2 = bottomLeftVector.angle(bottomRightVector);
                radians3 = bottomRightVector.angle(topRightVector);
                
                radianTestB = radians1 + radians2 + radians3;
                
                double resultA = Math.abs((2*Math.PI)-radianTestA);
                double resultB = Math.abs((2*Math.PI)-radianTestB);

                if(resultA > ANGLE_CHECK_THRESHOLD && 
                		resultB > ANGLE_CHECK_THRESHOLD){
                    return false;
                }
            }
        
        } else if (parentZone.getType() == Entity.TYPE_MODEL_ZONE) {
        	
        	
        	
        }

        return true;
    }
	
    /**
     * Checks the child object bounds against the parent bounds to see
     * if it is within the bounds of the parent. Returns true if it is
     * within bounds false otherwise.
     *
     * @param childEntity Entity
     * @param parentEntity Entity
     * @return True if in bounds, false otherwise
     */
    public static boolean performParentBoundsCheck(
            WorldModel model,
            Entity childEntity,
            Entity parentEntity){

        if (parentEntity instanceof BasePositionableEntity &&
                childEntity instanceof BasePositionableEntity){

            BasePositionableEntity parentBPE =
                (BasePositionableEntity) parentEntity;

            BasePositionableEntity childBPE =
                (BasePositionableEntity) childEntity;

            // Get the parent bounds and pos
            double[] parentPos = new double[3];
            float[] parentBounds = new float[6];

            parentBPE.getPosition(parentPos);
            parentBPE.getBounds(parentBounds);

            // Create the 4 parent coordinates
            double[] topLeft = {
                    parentPos[0] + parentBounds[0],
                    parentPos[1] + parentBounds[3],
                    0.0};

            double[] bottomLeft = {
                    parentPos[0] + parentBounds[0],
                    parentPos[1] + parentBounds[2],
                    0.0};

            double[] bottomRight = {
                    parentPos[0] + parentBounds[1],
                    parentPos[1] + parentBounds[2],
                    0.0};

            double[] topRight = {
                    parentPos[0] + parentBounds[1],
                    parentPos[1] + parentBounds[3],
                    0.0};

            // Get the child bounds and pos
            double[] pos = new double[3];
            float[] bounds = new float[6];

            childBPE.getBounds(bounds);
            childBPE.getPosition(pos);
            pos = TransformUtils.getPositionRelativeToZone(model, childBPE);
            pos[2] = 0.0;

            /*
             * Create the 4 coordinates to test (top left, bottom left,
             * bottom right, top right) based on the object position and
             * bounds.
             */
            double[] testCoords = {
                    pos[0] + bounds[0],
                    pos[1] + bounds[3],
                    pos[2],

                    pos[0] + bounds[0],
                    pos[1] + bounds[2],
                    pos[2],

                    pos[0] + bounds[1],
                    pos[1] + bounds[2],
                    pos[2],

                    pos[0] + bounds[1],
                    pos[1] + bounds[3],
                    pos[2]};

            // Perform polygon angle summation to determine if inside or 
            // outside wall
            Vector3d topLeftVector;
            Vector3d bottomLeftVector;
            Vector3d bottomRightVector;
            Vector3d topRightVector;

            for(int i = 0; i < 4; i++){

                topLeftVector = new Vector3d(
                        topLeft[0] - testCoords[i*3],
                        topLeft[1] - testCoords[i*3+1],
                        topLeft[2] - testCoords[i*3+2]);

                bottomLeftVector = new Vector3d(
                        bottomLeft[0] - testCoords[i*3],
                        bottomLeft[1] - testCoords[i*3+1],
                        bottomLeft[2] - testCoords[i*3+2]);

                bottomRightVector = new Vector3d(
                        bottomRight[0] - testCoords[i*3],
                        bottomRight[1] - testCoords[i*3+1],
                        bottomRight[2] - testCoords[i*3+2]);

                topRightVector = new Vector3d(
                        topRight[0] - testCoords[i*3],
                        topRight[1] - testCoords[i*3+1],
                        topRight[2] - testCoords[i*3+2]);

                double radians = 0.0;
                radians += topLeftVector.angle(bottomLeftVector);
                radians += bottomLeftVector.angle(bottomRightVector);
                radians += bottomRightVector.angle(topRightVector);
                radians += topRightVector.angle(topLeftVector);

                if(Math.abs((2*Math.PI)-radians) > 0.0001){
                    return false;
                }
            }

        }

        return true;
    }
    
    /**
	 * Calculate the multi bounds of the set of entities. The position returned
	 * is relative to the localCoordinateTarget passed in. All results are exact
	 * in that they reference the command queues to determine positions and
	 * bounds.
	 * 
	 * @param model WorldModel to reference
	 * @param multiBounds Bounds results
	 * @param multiCenter Center position result
	 * @param entitySet Entity list to evaluate, or null to evaluate 
	 * collisionEntities
	 * @param localCoordinateTarget Entity to calculate multiBounds and 
	 * multiCenter relative to
	 * @param exact True to use next frame values, false to use previous frame
	 * values
	 * @return True if successful, false otherwise
	 */
	public static boolean getMultiBounds(
			WorldModel model, 
			float[] multiBounds, 
			double[] multiCenter,
			ArrayList<Entity> entitySet,
			PositionableEntity localCoordinateTarget,
			boolean exact){

		if (model == null || 
				entitySet == null || 
				localCoordinateTarget == null) {
			
			return false;
		}
		
		float[] maxExtents = new float[3];
		float[] minExtents = new float[3];
		float[] tmpPos = new float[3];
		
		// Calculate the multi collision bounds and center
		// The bounds is based on the minimum exterior bounds along the
		// + and - directions of all axis.
		for(int i=0; i < entitySet.size(); i++){

			if(entitySet.get(i) instanceof PositionableEntity){
				
				PositionableEntity tmpEntity = 
					(PositionableEntity) entitySet.get(i);
				
				OrientedBoundingBox boundingBox = 
					getOrientedBoundingBox(model, tmpEntity, true, exact);
				
				if (boundingBox == null) {
					return false;
				}
				
				// Extract the bounds of the indexed collision entity				
				boundingBox.getCenter(tmpPos);
				boundingBox.getExtents(minExtents, maxExtents);
				
				// if the entity is a segment then switch the x and y values. 
				// it seems the rotation value of the segment is not used
				// in the transform process.
				/*
				if (tmpEntity instanceof SegmentEntity) {
					float tmp = minExtents[0];
					minExtents[0] = minExtents[1];
					minExtents[1] = tmp;
					tmp = maxExtents[0];
					maxExtents[0] = maxExtents[1];
					maxExtents[1] = tmp;
				}
				*/
				float[] convertedCenter = 
					TransformUtils.convertSceneCoordinatesToLocalCoordinates(
							model, tmpPos, localCoordinateTarget, exact);
				
				if (convertedCenter == null) {
					return false;
				}
				
				if (minExtents == null) {
					return false;
				}
				
				if (maxExtents == null) {
					return false;
				}
				
				if(i == 0){
					
					multiBounds[0] = minExtents[0];
					multiBounds[1] = maxExtents[0];
					multiBounds[2] = minExtents[1];
					multiBounds[3] = maxExtents[1];
					multiBounds[4] = minExtents[2];
					multiBounds[5] = maxExtents[2];
					
				} else {
					
					multiBounds[0] = Math.min(
							minExtents[0], 
							multiBounds[0]);
					
					multiBounds[1] = Math.max(
							maxExtents[0], 
							multiBounds[1]);
					
					multiBounds[2] = Math.min(
							minExtents[1], 
							multiBounds[2]);
					
					multiBounds[3] = Math.max(
							maxExtents[1], 
							multiBounds[3]);
					
					multiBounds[4] = Math.min(
							minExtents[2], 
							multiBounds[4]);
					
					multiBounds[5] = Math.max(
							maxExtents[2], 
							multiBounds[5]);
					
				}
			}
		}
		
		// Calculate bounds center and adjust bounds to be relative 
		// to the center
		multiCenter[0] = 
			multiBounds[0] + ((multiBounds[1] - multiBounds[0]) / 2.0);
		
		multiCenter[1] = 
			multiBounds[2] + ((multiBounds[3] - multiBounds[2]) / 2.0);
		
		multiCenter[2] = 
			multiBounds[4] + ((multiBounds[5] - multiBounds[4]) / 2.0);
		
		double[] convertedCenter = 
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
					model, multiCenter, localCoordinateTarget, exact);
		
		if (convertedCenter == null) {
			return false;
		}
		
		multiCenter[0] = convertedCenter[0];
		multiCenter[1] = convertedCenter[1];
		multiCenter[2] = convertedCenter[2];
		
		// Separate out the min and max extents to convert back to local coords
		minExtents[0] = multiBounds[0];
		minExtents[1] = multiBounds[2];
		minExtents[2] = multiBounds[4];
		
		maxExtents[0] = multiBounds[1];
		maxExtents[1] = multiBounds[3];
		maxExtents[2] = multiBounds[5];

		minExtents = TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, minExtents, localCoordinateTarget, exact);
		
		if (minExtents == null) {
			return false;
		}
		
		maxExtents = TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, maxExtents, localCoordinateTarget, exact);
		
		if (maxExtents == null) {
			return false;
		}
		
		// Make sure we put the max and min values into the correct extents
		// set. Converting scene coordinates to local coordinates for bounds
		// values when the bounds values are max and min in scene coordinates
		// means we have to do an extra analysis to put maximum local values
		// together and minimum local values together before calculating the
		// full multiBounds result.
		float[] tempMaximum = new float[3];
		float[] tempMinimum = new float[3];
		
		tempMaximum[0] = Math.max(maxExtents[0], minExtents[0]);
		tempMaximum[1] = Math.max(maxExtents[1], minExtents[1]);
		tempMaximum[2] = Math.max(maxExtents[2], minExtents[2]);
		
		tempMinimum[0] = Math.min(maxExtents[0], minExtents[0]);
		tempMinimum[1] = Math.min(maxExtents[1], minExtents[1]);
		tempMinimum[2] = Math.min(maxExtents[2], minExtents[2]);
		
		minExtents[0] = tempMinimum[0];
		minExtents[1] = tempMinimum[1];
		minExtents[2] = tempMinimum[2];
		
		maxExtents[0] = tempMaximum[0];
		maxExtents[1] = tempMaximum[1];
		maxExtents[2] = tempMaximum[2];
		
		// Calculate the full multiBounds result.
		multiBounds[0] = (float) (minExtents[0] - multiCenter[0]);
		multiBounds[1] = (float) (maxExtents[0] - multiCenter[0]);
		multiBounds[2] = (float) (minExtents[1] - multiCenter[1]);
		multiBounds[3] = (float) (maxExtents[1] - multiCenter[1]);
		multiBounds[4] = (float) (minExtents[2] - multiCenter[2]);
		multiBounds[5] = (float) (maxExtents[2] - multiCenter[2]);
		
		return true;
	}
	
    
    /**
     * Create an OrientedBoundingBox. The bounding box can be setup in either
     * scene or local coordinates with optionally exact scene state 
     * consideration. The resulting OrientedBoundingBox will have the 
     * appropriate transforms already applied to it for its position in the 
     * scene.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to get OrientedBoundingBox for
     * @param inSceneCoordinates True to get OrientedBoundingBox in scene 
     * coordinates, false for local coordinates
     * @param exact True to get the exact state, false for the previous
     * state
     * @return OrientedBoundingBox, or null if there was a problem
     */
    public static OrientedBoundingBox getOrientedBoundingBox(
    		WorldModel model,
    		PositionableEntity entity, 
    		boolean inSceneCoordinates,
    		boolean exact) {
    	float[] scale = new float[3];
    	float[] min = new float[3];
    	float[] max = new float[3];
    	
    	OrientedBoundingBox boundingBox = null;
    	
    	// Need to special case this for walls and extrusion entities
    	// Handle the special extrusion bounds case, otherwise do standard
        // processing.
		Object isExtrusionProp = entity.getProperty(
			entity.getParamSheetName(),
			ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);
		
		if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
			
			boolean isExtrusion = ((Boolean)isExtrusionProp).booleanValue();
			
			if (isExtrusion) {
				 
				boundingBox = new ExtrusionBoundingBox();
				
				// Get the translation applied to the extrusion shape
				float[] cs_translation = (float[]) 
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP);
				
				// Calculate the extrusion shape min and max extents
				float[] sizeParam = (float[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							PositionableEntity.SIZE_PARAM);
				
				if (sizeParam == null) {
					return null;
				}
			
				float[] cs_extent = new float[6];
				// min extents
				cs_extent[0] = 0.0f;
				cs_extent[1] = -(sizeParam[1]/2.0f) - cs_translation[1];
				cs_extent[2] = -(sizeParam[2]/2.0f) - cs_translation[2];
				
				// max extents
				cs_extent[3] = 0.0f;
				cs_extent[4] = (sizeParam[1]/2.0f) + cs_translation[1];
				cs_extent[5] = (sizeParam[2]/2.0f) + cs_translation[2];
				
				// Get the spline of the extrusion
				float[] spine = (float[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.SPINE_VERTICES_PROP);
				
				// Get the visibility values of the extrusion
				boolean[] visible = (boolean[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.VISIBLE_PROP);
				
				boolean[] miterEnable = (boolean[])
					entity.getProperty(
							Entity.DEFAULT_ENTITY_PROPERTIES, 
							ExtrusionEntity.MITER_ENABLE_PROP);
				
				((ExtrusionBoundingBox)boundingBox).update(
						cs_extent, 
						cs_translation, 
						spine, 
						visible, 
						miterEnable);
			}
			
		} else if (entity instanceof SegmentEntity) {
    		
    		SegmentEntity segment = (SegmentEntity) entity;
    		
            // the wall thickness
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
            float height1 = ve1.getHeight();
            
			// the wall height
            float height = Math.max(height0, height1);
            
    		double[] startVertexPos = null;
    		double[] endVertexPos = null;
    		
    		if (exact) {

    			startVertexPos = TransformUtils.getExactPosition(ve1);
    			endVertexPos = TransformUtils.getExactPosition(ve0);
    			
    		} else {
    			
    			startVertexPos = TransformUtils.getPosition(ve1);
    			endVertexPos = TransformUtils.getPosition(ve0);
    		}
    		
        	//////////////////////////////////////////////////////////////
        	// rem: hard coded, relationship between the walls and the floor
    		float[] startPos = new float[3];
    		startPos[0] = (float) startVertexPos[0];
    		startPos[1] = (float) startVertexPos[2];
    		startPos[2] = -(float) startVertexPos[1];
    		
    		float[] endPos = new float[3];
    		endPos[0] = (float) endVertexPos[0];
    		endPos[1] = (float) endVertexPos[2];
    		endPos[2] = -(float) endVertexPos[1];
    		//////////////////////////////////////////////////////////////
			
    		boundingBox = new SegmentBoundingBox();
    		((SegmentBoundingBox)boundingBox).update(
    				startPos, endPos, height, wallThickness);
    		
    	} else {
    	
	    	// Pull out the bounds data and scale, then create the oriented bounding
	    	// box based on those values.
	    	float[] size = new float[3];

	    	entity.getSize(size);
	    	
	    	if (exact) {
	    		scale = TransformUtils.getExactScale(entity);
	    	} else {
	    		scale = TransformUtils.getScale(entity);
	    	}
    	
	    	max[0] = size[0] / 2.0f;
	    	max[1] = size[1] / 2.0f;
	    	max[2] = size[2] / 2.0f;
	    	
	    	min[0] = -max[0];
	    	min[1] = -max[1];
	    	min[2] = -max[2];
	    	
	    	boundingBox = 
	    		new OrientedBoundingBox(min, max, scale);
    	}
    	
    	// Pull out the transform data, create the transform matrix and apply
    	// it to the oriented bounding box.
    	
    	Matrix4f transformMat = new Matrix4f();
    	
    	if (inSceneCoordinates) {
    		
    		transformMat = 
				TransformUtils.getTransformsInSceneCoordinates(
						model, entity, exact);

			if (transformMat == null) {
				return null;
			}
    		
    	} else {
    		
    		double[] position = new double[3];
        	float[] rotation = new float[4];
    		
    		if (exact) {
    			position = TransformUtils.getExactPosition(entity);
    			rotation = TransformUtils.getExactRotation(entity);
    		} else {
    			position = TransformUtils.getPosition(entity);
    			rotation = TransformUtils.getRotation(entity);
    		}
    		
    		Vector3f translation = new Vector3f(
        			(float) position[0],
        			(float) position[1],
        			(float) position[2]);
    		
    		AxisAngle4f axisAngleRot = new AxisAngle4f(rotation);
        	
        	transformMat.setIdentity();
        	transformMat.setTranslation(translation);
        	transformMat.setRotation(axisAngleRot);
    		
    	}

    	boundingBox.transform(transformMat);
    	
    	return boundingBox;
    }
    
    /**
     * Get the horizontal gap between oriented bounding boxes calculated in the
     * local coordinate system of the sourceEntity's parent. This will return
     * a result for entities regardless of the branch of the hierarchy they
     * exist in. For example, entities on two different wall segments will still
     * be evaluated correctly. Think of this as doing a conversion that will 
     * give the closest edge-to-edge distance between two entities as though
     * they had a common direct parent, even if that isn't the case.
     * 
     * @param model WorldModel to reference
     * @param sourceEntity Source entity to get gap relative to
     * @param targetEntity Target entity to calculate gap distance against
     * @param exact True to use values that take into consideration the current
     * command queues, false to get values that are guaranteed correct for the
     * previous frame only
	 * @param closestTargetAxisPoint Updated with the point on the target
	 * closest to the sourceEntity that is used in the distance calculation. 
	 * Value will be in coordinates relative to sourceEntity's parent.
     * @return Gap distance or, Float.NaN if unable to calculate
     */
    public static float getHorizontalGapBetweenBoxes(
    		WorldModel model,
    		PositionableEntity sourceEntity,
    		PositionableEntity targetEntity,
    		boolean exact,
    		double[] closestTargetAxisPoint) {
 	
    	// get bounding boxes, initially in local coordinates
    	OrientedBoundingBox sourceBox = 
    		getOrientedBoundingBox(model, sourceEntity, false, true);
    	
    	OrientedBoundingBox targetBox = 
    		getOrientedBoundingBox(model, targetEntity, false, true);
    	
    	if (sourceBox == null || targetBox == null) {
    		return Float.NaN;
    	}
    	
    	// get the parent of the source entity
    	PositionableEntity sourceParent = (PositionableEntity) 
    		SceneHierarchyUtility.getExactParent(model, sourceEntity);
    	
    	if (sourceParent == null) {
    		return Float.NaN;
    	}
    	
		Matrix4f s_mtx = TransformUtils.getTransformsInSceneCoordinates(
			model, sourceParent, exact);
		
		// transform the target bounds into the source coord system
		Matrix4f t_mtx = TransformUtils.getTransformsInSceneCoordinates(
			model, targetEntity, exact);
		
		s_mtx.invert();
		t_mtx.mul(s_mtx, t_mtx);
		
		targetBox.transform(t_mtx);
		
    	// Get the vector describing the targetEntity - sourceEntity position
    	// in order to determine if the targetEntity is to the left or right
    	// of the sourceEntity.
    	Point3f source_center = new Point3f();
		sourceBox.getCenter(source_center);
		
		Point3f target_center = new Point3f();
		targetBox.getCenter(target_center);
		
		Vector3f vec = new Vector3f(
			 source_center.x - target_center.x,
			 source_center.y - target_center.y,
			 source_center.z - target_center.z);
		
    	// get the extents of the sourceEntity and targetEntity
    	float[] sourceEntityMaxExtents = new float[3];
    	float[] sourceEntityMinExtents = new float[3];
    	sourceBox.getExtents(sourceEntityMinExtents, sourceEntityMaxExtents);
		
    	float[] targetEntityMaxExtents = new float[3];
    	float[] targetEntityMinExtents = new float[3];
    	targetBox.getExtents(targetEntityMinExtents, targetEntityMaxExtents);
		
		// sort the local x extents
		float[] f = new float[]{
			targetEntityMinExtents[0],
			targetEntityMaxExtents[0],
			sourceEntityMinExtents[0],
			sourceEntityMaxExtents[0]};
		Arrays.sort(f);
		
		// the gap is in the middle
		float gap = 0;
		
		if (vec.x < 0) {
			gap = f[2] - f[1];
		} else if (vec.x > 0) {
			gap = f[1] - f[2];
    	} else {
    		return Float.NaN;
    	}
		
		// If f[1] matches the targetEntityMaxExtents[0] value then we want
		// to grab that position, otherwise grab the min position
		if (targetEntityMaxExtents[0] == f[1]) {
			closestTargetAxisPoint[0] = targetEntityMaxExtents[0];
			closestTargetAxisPoint[1] = targetEntityMaxExtents[1];
			closestTargetAxisPoint[2] = targetEntityMaxExtents[2];
		} else {
			closestTargetAxisPoint[0] = targetEntityMinExtents[0];
			closestTargetAxisPoint[1] = targetEntityMinExtents[1];
			closestTargetAxisPoint[2] = targetEntityMinExtents[2];
		}
		
		// These values are in scene coordinates. We need to convert them to
		// local coordinates relative to the sourceEntity's parent
		
		Entity parentEntity = 
			SceneHierarchyUtility.getExactParent(model, sourceEntity);
		
		if (parentEntity == null) {
			
			return Float.NaN;
		}
		
		closestTargetAxisPoint = 
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, 
				closestTargetAxisPoint, 
				(PositionableEntity) parentEntity, 
				true);
		
    	/*
    	// DEBUG POSITION OUTPUT
    	System.out.println("********************************************");
    	double[] pos = new double[3];
    	sourceEntity.getPosition(pos);
    	System.out.println("sourceEntity pos: "+Arrays.toString(pos));
    	pos = TransformUtils.getExactPosition(sourceEntity);
    	System.out.println("sourceEntity pos EXACT: "+Arrays.toString(pos));

    	pos = TransformUtils.getPositionInSceneCoordinates(model, sourceEntity, true);
    	System.out.println("sourceEntity pos scene coords exact: "+Arrays.toString(pos));
    	ZoneEntity zone = SceneHierarchyUtility.findExactZoneEntity(model, sourceEntity);
    	pos = TransformUtils.convertSceneCoordinatesToLocalCoordinates(model, pos, zone, true);
    	System.out.println("sourceEntity pos scene coords converted: "+Arrays.toString(pos));
    	
    	EntityUtils eu = new EntityUtils(model);
    	Matrix4f matrix = new Matrix4f();
    	eu.getTransformToRoot(sourceEntity, matrix);
    	Vector3f translation = new Vector3f();
    	matrix.get(translation);
    	System.out.println("sourceEntity pos EntityUtil result: "+translation.toString());
    	System.out.println("gap: "+gap);
    	*/
    	
    	return gap;
    }
   
    /**
     * Get the vertical gap between oriented bounding boxes calculated in the
     * local coordinate system of the sourceEntity's parent. This will return
     * a result for entities regardless of the branch of the hierarchy they
     * exist in. For example, entities on two different wall segments will still
     * be evaluated correctly. Think of this as doing a conversion that will 
     * give the closest edge-to-edge distance between two entities as though
     * they had a common direct parent, even if that isn't the case.
     * 
     * @param model WorldModel to reference
     * @param sourceEntity Source entity to get gap relative to
     * @param targetEntity Target entity to calculate gap distance against
     * @param exact True to use values that take into consideration the current
     * command queues, false to get values that are guaranteed correct for the
     * previous frame only
	 * @param closestTargetAxisPoint Updated with the point on the target
	 * closest to the sourceEntity that is used in the distance calculation. 
	 * Value will be in coordinates relative to sourceEntity's parent.
     * @return Gap distance or, Float.NaN if unable to calculate
     */
    public static float getVerticalGapBetweenBoxes(
    		WorldModel model,
    		PositionableEntity sourceEntity,
    		PositionableEntity targetEntity,
    		boolean exact,
    		double[] closestTargetAxisPoint) {

    	// Establish bounding boxes in scene coordinates, and get extents in 
    	// zone coordinates
    	OrientedBoundingBox sourceBox = 
    		getOrientedBoundingBox(model, sourceEntity, true, true);
    	
    	OrientedBoundingBox targetBox = 
    		getOrientedBoundingBox(model, targetEntity, true, true);
    	
    	if (sourceBox == null || targetBox == null) {
    		return Float.NaN;
    	}
    	
    	// Get the parent of the source entity to convert scene coordinates
    	// relative to.
    	PositionableEntity sourceParent = (PositionableEntity) 
    		SceneHierarchyUtility.getExactParent(model, sourceEntity);
    	
    	if (sourceParent == null) {
    		return Float.NaN;
    	}
    	
		Matrix4f s_mtx = TransformUtils.getTransformsInSceneCoordinates(
			model, sourceParent, exact);
		
		// transform the target bounds into the source coord system
		Matrix4f t_mtx = TransformUtils.getTransformsInSceneCoordinates(
			model, targetEntity, exact);
		
		s_mtx.invert();
		t_mtx.mul(s_mtx, t_mtx);
		
		targetBox.transform(t_mtx);
		
    	// Get the vector describing the targetEntity - sourceEntity position
    	// in order to determine if the targetEntity is to the left or right
    	// of the sourceEntity.
    	Point3f source_center = new Point3f();
		sourceBox.getCenter(source_center);
		
		Point3f target_center = new Point3f();
		targetBox.getCenter(target_center);
		
		Vector3f vec = new Vector3f(
			 source_center.x - target_center.x,
			 source_center.y - target_center.y,
			 source_center.z - target_center.z);
		
    	// get the extents of the sourceEntity and targetEntity
    	float[] sourceEntityMaxExtents = new float[3];
    	float[] sourceEntityMinExtents = new float[3];
    	sourceBox.getExtents(sourceEntityMinExtents, sourceEntityMaxExtents);
		
    	float[] targetEntityMaxExtents = new float[3];
    	float[] targetEntityMinExtents = new float[3];
    	targetBox.getExtents(targetEntityMinExtents, targetEntityMaxExtents);
		
		// sort the local y extents
		float[] f = new float[]{
			targetEntityMinExtents[1],
			targetEntityMaxExtents[1],
			sourceEntityMinExtents[1],
			sourceEntityMaxExtents[1]};
		Arrays.sort(f);
		
		// the gap is in the middle
		float gap = 0;
		
		if (vec.y < 0) {
			gap = f[2] - f[1];
		} else if (vec.y > 0) {
			gap = f[1] - f[2];
    	} else {
    		return Float.NaN;
    	}
		
		// If f[1] matches the targetEntityMaxExtents[0] value then we want
		// to grab that position, otherwise grab the min position
		if (targetEntityMaxExtents[1] == f[1]) {
			closestTargetAxisPoint[0] = targetEntityMaxExtents[0];
			closestTargetAxisPoint[1] = targetEntityMaxExtents[1];
			closestTargetAxisPoint[2] = targetEntityMaxExtents[2];
		} else {
			closestTargetAxisPoint[0] = targetEntityMinExtents[0];
			closestTargetAxisPoint[1] = targetEntityMinExtents[1];
			closestTargetAxisPoint[2] = targetEntityMinExtents[2];
		}
		
		// These values are in scene coordinates. We need to convert them to
		// local coordinates relative to the sourceEntity's parent
		
		Entity parentEntity = 
			SceneHierarchyUtility.getExactParent(model, sourceEntity);
		
		if (parentEntity == null) {
			
			return Float.NaN;
		}
		
		closestTargetAxisPoint = 
			TransformUtils.convertSceneCoordinatesToLocalCoordinates(
				model, 
				closestTargetAxisPoint, 
				(PositionableEntity) parentEntity, 
				true);
		
    	/*
    	// DEBUG POSITION OUTPUT
    	System.out.println("********************************************");
    	double[] pos = new double[3];
    	sourceEntity.getPosition(pos);
    	System.out.println("sourceEntity pos: "+Arrays.toString(pos));
    	pos = TransformUtils.getExactPosition(sourceEntity);
    	System.out.println("sourceEntity pos EXACT: "+Arrays.toString(pos));

    	pos = TransformUtils.getPositionInSceneCoordinates(model, sourceEntity, true);
    	System.out.println("sourceEntity pos scene coords exact: "+Arrays.toString(pos));
    	ZoneEntity zone = SceneHierarchyUtility.findExactZoneEntity(model, sourceEntity);
    	pos = TransformUtils.convertSceneCoordinatesToLocalCoordinates(model, pos, zone, true);
    	System.out.println("sourceEntity pos scene coords converted: "+Arrays.toString(pos));
    	
    	EntityUtils eu = new EntityUtils(model);
    	Matrix4f matrix = new Matrix4f();
    	eu.getTransformToRoot(sourceEntity, matrix);
    	Vector3f translation = new Vector3f();
    	matrix.get(translation);
    	System.out.println("sourceEntity pos EntityUtil result: "+translation.toString());
    	System.out.println("gap: "+gap);
    	*/
    	
    	return gap;
    }
    
    /**
     * Uses cumulative bounds logic to get the extents of colliding items
     * Then determines if the bounds of the entity minus the bounds of
     * the colliding obj (usually a standard) are under the overhang limit.
     * 
     * @param model WorldModel to reference
     * @param command Command to perform collision tests with
     * @param entity Entity affected by command
     * @param ignoreEntityIdList Entity ID's to ignore in collision results
     * @param rch RuleCollisionHandler to use
     * @return True if there is an overhang limit violation, false otherwise
     */
	/*
    public static boolean checkOverhangLimit (
            WorldModel model,
            Command command,
            Entity entity,
            int[] ignoreEntityIdList,
            RuleCollisionHandler rch) {

        // check for collisions
        rch.performCollisionCheck(command, true, false, false);
        
        rch.printCollisionEntitiesList(false);

        // do the analysis
        rch.performCollisionAnalysisHelper(
                entity, null, false, ignoreEntityIdList, true);

        // see if there are any illegal collisions
        boolean illegalCollisions = rch.hasIllegalCollisionHelper(entity);
        
        ArrayList<Entity> entityMatches =
        	rch.getChildrenMatches().getEntityMatches();
        
        ArrayList<Entity> floorEntityMatches =
        	rch.getChildrenMatches().getFloorEntityMatches();
        
        ArrayList<Entity> wallEntityMatches =
        	rch.getChildrenMatches().getWallEntityMatches();

        // Handle the > 1 collision and == 1 collision cases here, handing off
        // the gen pos calculation to the specific routines for appropriate
        // processing.
        if (entityMatches.size() >= 1 &&
                !illegalCollisions &&
                floorEntityMatches.size() == 0 &&
                wallEntityMatches.size() <= 1) {

            Float overHangLimit =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.OVERHANG_LIMIT);

            Float overHangMinimum =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.OVERHANG_MINIMUM);

            // Calculate the multi collision bounds and center
            // The bounds is based on the minimum exterior bounds along the
            // + and - directions of all axis.
            float[] multiBounds = new float[6];
            double[] multiCenter = new double[3];
            ArrayList<Entity> multiBoundsSet = new ArrayList<Entity>();

            for (int i=0; i < rch.collisionEntities.size(); i++) {

                Entity tmpEntity = rch.collisionEntities.get(i);

                if (SceneHierarchyUtility.isEntityChildOfParent(
                		model, tmpEntity, entity, true)) {
                    continue;
                }

                multiBoundsSet.add(tmpEntity);
            }
            
            // Get the activeZoneEntity that we want to get the multiBounds
            // relative to.
            Entity activeZone = 
            	SceneHierarchyUtility.getActiveZoneEntity(model);
            
            getMultiBounds(
            		model, 
            		multiBounds, 
            		multiCenter,
            		multiBoundsSet, 
            		(PositionableEntity) activeZone);

            multiBounds[0] = (float)multiCenter[0] + multiBounds[0];
            multiBounds[1] = (float)multiCenter[0] + multiBounds[1];
            multiBounds[2] = (float)multiCenter[1] + multiBounds[2];
            multiBounds[3] = (float)multiCenter[1] + multiBounds[3];
            multiBounds[4] = (float)multiCenter[2] + multiBounds[4];
            multiBounds[5] = (float)multiCenter[2] + multiBounds[5];
           
            if( multiBounds == null){
                // EMF and Russ: adding null pointer prevention to keep 
            	// you safe!
                return true;
            }

            // Get the entity's values to compare with
            float[] entityExtents = new float[6];
            
            float[] entityBounds = 
            	getBounds((PositionableEntity) entity, true);
            double[] entityPos =
                TransformUtils.getExactRelativePosition(
                        model,
                        entity,
                        activeZone,
                        false);

            if(command instanceof AddEntityChildCommand) {
                entity.setParentEntityID(-1);
            }

            entityExtents[0] = (float) entityPos[0] + entityBounds[0];
            entityExtents[1] = (float) entityPos[0] + entityBounds[1];
            entityExtents[2] = (float) entityPos[1] + entityBounds[2];
            entityExtents[3] = (float) entityPos[1] + entityBounds[3];
            entityExtents[4] = (float) entityPos[2] + entityBounds[4];
            entityExtents[5] = (float) entityPos[2] + entityBounds[5];

            // check for bounds X AXIS only
            if((multiBounds[0] - entityExtents[0]) > overHangLimit ||
                    (entityExtents[1] - multiBounds[1]) > overHangLimit ||
                    (multiBounds[0] - entityExtents[0]) < overHangMinimum ||
                    (entityExtents[1] - multiBounds[1]) < overHangMinimum) {

            	return true;

            } else {

            	return false;
            }
        }
        
        return true;
    }
	*/
	public static boolean checkOverhangLimit (
            WorldModel model,
            Command command,
            Entity entity,
            int[] ignoreEntityIdList,
            RuleCollisionHandler rch) {

        // check for collisions
        rch.performCollisionCheck(command, true, false, true);
        
//        rch.printCollisionEntitiesList(false);

        // do the analysis
        rch.performCollisionAnalysisHelper(
                entity, null, false, ignoreEntityIdList, true);

        // see if there are any illegal collisions
        boolean illegalCollisions = rch.hasIllegalCollisionHelper(entity);
        
        ArrayList<Entity> entityMatches =
        	rch.getChildrenMatches().getEntityMatches();
        
        ArrayList<Entity> floorEntityMatches =
        	rch.getChildrenMatches().getFloorEntityMatches();
        
        ArrayList<Entity> wallEntityMatches =
        	rch.getChildrenMatches().getWallEntityMatches();

        // Handle the > 1 collision and == 1 collision cases here, handing off
        // the gen pos calculation to the specific routines for appropriate
        // processing.
        if (entityMatches.size() >= 1 &&
                !illegalCollisions &&
                floorEntityMatches.size() == 0 &&
                wallEntityMatches.size() <= 1) {

            Float overHangLimit =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.OVERHANG_LIMIT);

            Float overHangMinimum =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.OVERHANG_MINIMUM);

            // Calculate the multi collision bounds and center
            // The bounds is based on the minimum exterior bounds along the
            // + and - directions of all axis.
            ArrayList<Entity> multiBoundsSet = new ArrayList<Entity>();
            for (int i=0; i < rch.collisionEntities.size(); i++) {

                Entity tmpEntity = rch.collisionEntities.get(i);

                if (SceneHierarchyUtility.isEntityChildOfParent(
                		model, tmpEntity, entity, true)) {
                    continue;
                }

                multiBoundsSet.add(tmpEntity);
            }
			
            // Get the activeZoneEntity that we want to get the multiBounds
            // relative to.
            Entity activeZone = 
            	SceneHierarchyUtility.getActiveZoneEntity(model);

			OrientedBoundingBox cum_bounds = getCumulativeBounds(
				model,
				multiBoundsSet, 
				activeZone);
			
			if (cum_bounds != null) {
				
				PositionableEntity pe = (PositionableEntity)entity;
				Matrix4f mtx = TransformUtils.getRelativeTransform(
					model,
					pe,
					activeZone,
					true);
				
				if (mtx != null) {
					
					OrientedBoundingBox entity_bounds = 
						getOrientedBoundingBox(model, pe, false, true);
					
					if (entity_bounds != null) {
						
						entity_bounds.transform(mtx);
						
						float[] emin = new float[3];
						float[] emax = new float[3];
						float[] cmin = new float[3];
						float[] cmax = new float[3];
						
						entity_bounds.getExtents(emin, emax);
						cum_bounds.getExtents(cmin, cmax);
						
						float left_overhang = cmin[0] - emin[0];
						float rght_overhang = emax[0] - cmax[0];
						
						// rem: visually, this appears to work with the test cases.
						// but this if statement looks suspect... 
						if ((left_overhang > overHangLimit) ||
							(rght_overhang > overHangLimit) ||
							(left_overhang < overHangMinimum) ||
							(rght_overhang < overHangMinimum)) {
							
							return true;
							
						} else {
							
							return false;
						}
					}
				}
			}
        }
        
        return true;
    }
	
	/**
	 * Return the cumulative bounds of the set of entities
	 * relative to the specified ancestor entity. If an entity
	 * in the list is not a descendant of the ancestor, it is 
	 * ignored.
	 *
	 * @param model The world model
	 * @param entitySet The entities to process
	 * @param ancestorEntity The ancestor entity
	 * @return The cumulative bounds of the set of entities, or null
	 * if none of the entities in the list were descendants of the
	 * specified ancestor - or none of the entities bounds could be determined.
	 */
	private static OrientedBoundingBox getCumulativeBounds(
		WorldModel model,
		ArrayList<Entity> entitySet, 
		Entity ancestorEntity){
		
		float[] emin = new float[3];
		float[] emax = new float[3];
		float[] cmin = new float[3];
		float[] cmax = new float[3];
		
		boolean config = false;
		for (int i = 0; i < entitySet.size(); i++) {
			
			PositionableEntity pe = (PositionableEntity)entitySet.get(i);
			
			Matrix4f mtx = TransformUtils.getRelativeTransform(
				model,
				pe,
				ancestorEntity,
				true);
			
			if (mtx != null) {
				OrientedBoundingBox obb = 
					getOrientedBoundingBox(model, pe, false, false);
				
				if (obb != null) {
					
					obb.transform(mtx);
					obb.getExtents(emin, emax);
					
					if (!config) {
						cmin[0] = emin[0];
						cmin[1] = emin[1];
						cmin[2] = emin[2];
						cmax[0] = emax[0];
						cmax[1] = emax[1];
						cmax[2] = emax[2];
						config = true;
					} else {
						if (cmin[0] > emin[0]) {
							cmin[0] = emin[0];
						}
						if (cmin[1] > emin[1]) {
							cmin[1] = emin[1];
						}
						if (cmin[2] > emin[2]) {
							cmin[2] = emin[2];
						}
						if (cmax[0] < emax[0]) {
							cmax[0] = emax[0];
						}
						if (cmax[1] < emax[1]) {
							cmax[1] = emax[1];
						}
						if (cmax[2] < emax[2]) {
							cmax[2] = emax[2];
						}
					}
				}
			}
		}
		
		OrientedBoundingBox cum_bounds = null;
		if (config) {
			float[] scale = new float[]{1, 1, 1};
			cum_bounds = new OrientedBoundingBox(cmin, cmax, scale);
		}
		return(cum_bounds);
	}
}
