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

package org.chefx3d.model;

// External Imports
import java.util.ArrayList;
import java.util.Map;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

// Internal Imports


/**
 * A single segment that is a child of a SegmentableEntity.
 * 
 * @author Russell Dodds
 * @version $Revision: 1.53 $
 */
public class SegmentEntity extends ZoneEntity {

	public static final String SEGMENT_PROPERTY_SHEET = "SegmentEntity.propertySheet";
	
    /** The thickness of the SegmentEntity wall */
    public static final String WALL_THICKNESS_PROP = "org.chefx3d.model.SegmentEntity.wallThickness";
    
    /** The standard facing of the SegmentEntity wall */
    public static final String STANDARD_FACING_PROP = "org.chefx3d.model.SegmentEntity.standardFacing";

	/** The property name for the begin index value */
	//public static final String START_INDEX_PROP = "SegmentEntity.startIndex";

	/** The property name for the end index value */
	//public static final String END_INDEX_PROP = "SegmentEntity.endIndex";
	
	/** The property name for the begin ID value */
	//public static final String START_ID_PROP = "SegmentEntity.startID";

	/** The property name for the end ID value */
	//public static final String END_ID_PROP = "SegmentEntity.endID";

	public static final String EXTERIOR_SEGMENT_PROP = "SegmentEntity.exteriorSegment";

	public static final String GROUP_NODE_PROP = "SegmentEntity.groupNode";

	/** Set a default wall thickness of 4" */
	public static final float DEFAULT_WALL_THICKNESS = 0.1016f;

	/** The length of the vector goes from start vertex to end vertex */
	private float segmentLength;
	
	/** A reference to the start VertexEntity */
	private VertexEntity startVertex;
	
	/** A reference to the end VertexEntity */
	private VertexEntity endVertex;
	
	/** working variable to track first vertex postion */
	private double[] firstVertexPosD;
	
	/** working variable to track end vertex position */
	private double[] secondVertexPosD;
	
	/** set during the getLength() called, represents the 
	 * vector from the start vertex to the end vertex. */
	private Vector3f segmentVector;
		
    /**
     * Create a new segment using properties
     * 
     * @param entityID The segmentID
     * @param sheetName The name of the base sheet
     * @param defaultProperties The property sheet map (sheet -> document)
     */
    public SegmentEntity(
            int entityID, 
            String sheetName,
            Map<String, Map<String, Object>> defaultProperties) {

        super(entityID, Entity.TYPE_SEGMENT, sheetName, defaultProperties);
        init();
    }

    /**
     * Create a new segment using properties
     * 
     * @param entityID The segmentID
     * @param sheetName The name of the base sheet
     * @param params the non-editable params
     * @param defaultProperties The property sheet map (sheet -> document)
     */
    public SegmentEntity(
            int entityID, 
            String sheetName,
            String positionPropertySheet, 
            Map<String, Object> params, 
            Map<String, Map<String, Object>> defaultProperties) {

        super(entityID, sheetName, positionPropertySheet, params, defaultProperties);
        init();
    }

    /**
     * Create a new segment using properties
     * 
     * @param entityID The segmentID
     * @param defaultProperties The property sheet map (sheet -> document)
     */
	public SegmentEntity(
	        int entityID,
			Map<String, Map<String, Object>> defaultProperties) {
	    
		this(entityID, DEFAULT_ENTITY_PROPERTIES, defaultProperties);
		init();
	}
	

	// ---------------------------------------------------------------
	// Methods defined by Object
	// ---------------------------------------------------------------

	/**
	 * Compare the given details to this one to see if they are equal. Equality
	 * is defined as pointing to the same clipPlane source, with the same
	 * transformation value.
	 * 
	 * @param o
	 *            The object to compare against
	 * @return true if these represent identical objects
	 */
	public boolean equals(Object o) {

		if (!(o instanceof SegmentEntity))
			return false;

		Entity e = (Entity) o;

		if (e.getEntityID() != entityID)
			return false;

		return true;
	}

	/**
	 * Calculate the hashcode for this object.
	 * 
	 * @return The entityID
	 */
	public int hashCode() {
		// TODO: Not a very good hash
		return entityID;
	}
	
    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public SegmentEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();
        
        // Create the new copy
        SegmentEntity clonedEntity =
            new SegmentEntity(clonedID,
                              propertySheetName,
                              positionPropertySheet, 
                              params, 
                              properties);

        // copy all the other data over
        clonedEntity.children = new ArrayList<Entity>();
    	
    	int len = children.size();
    	for (int i = 0; i < len; i++) {   		
    		Entity clone = children.get(i).clone(issuer);  
    		clone.setParentEntityID(clonedID);
    		clonedEntity.children.add(clone);
    	}
        clonedEntity.segmentLength = segmentLength;
        
        // clone the start and end vertex
        clonedEntity.startVertex = startVertex.clone(issuer);
        clonedEntity.endVertex = endVertex.clone(issuer);
        
        // now update the properties
        //int startID = clonedEntity.startVertex.getEntityID();
        //clonedEntity.setStartID(startID);
        
        //int endID = clonedEntity.endVertex.getEntityID();
        //clonedEntity.setEndID(endID);
       
        return(clonedEntity);
   
    }


	// ----------------------------------------------------------
	// Methods Required for Entity
	// ----------------------------------------------------------

	/**
	 * Get the type of this entity
	 * 
	 * @return The type property
	 */
	public int getType() {
		return entityType;
	}
	
    /**
     * Check if the type of this entity is one of the zone types
     * 
     * @return True if one of the zone types, false otherwise
     */
    public boolean isZone() {
    	return true;
    }

    // ----------------------------------------------------------
    // Overridden Methods
    // ----------------------------------------------------------
	
	/**
     * Get the size of this entity.
     * 
     * @param size The size to return
     */
    public void getSize(float[] size) {
        
        float[] bounds = new float[6];
        getBounds(bounds);
     
        size[0] = bounds[1] - bounds[0];
        size[1] = bounds[3] - bounds[2];
        size[2] = bounds[5] - bounds[4];
    }	
	
	// ----------------------------------------------------------
	// Local Methods
	// ----------------------------------------------------------
    
    
    /** 
     * Initialize basic variables. 
     */
	private void init(){
		segmentLength = -1;
        firstVertexPosD = new double[3];
		secondVertexPosD = new double[3];
		segmentVector = new Vector3f(); 
	}
	
    
    /**
     * Get the length of this segment (from start vertex to end vertex).
     * This method also builds the segmentVector.
     * 
     * @return float length 
     * @author Eric Fickenscher
     */
    public float getLength(){
		//
		//  Get the vertex positions
		//
		startVertex.getPosition(firstVertexPosD);
		endVertex.getPosition(secondVertexPosD);

		//
		// Use positions to generate the vector (end - start)
		// Set Z to zero so the get the length along the floor, if we use Z
		// then when the wall is angled it will be wrong
		//
		segmentVector.set((float)(firstVertexPosD[0] - secondVertexPosD[0]),
						  (float)(firstVertexPosD[1] - secondVertexPosD[1]), 0f); 
		segmentLength = segmentVector.length();
		
		return segmentLength;
    }
    
    
    /**
     * This method returns the height of the higher of
     * the two vertices.
     * 
     * @return height of the highest vertex
     * @author Eric Fickenscher
     */
    public float getHeight(){
	
    	float v1Height = startVertex.getHeight();
    	float v2Height = endVertex.getHeight();
    	
    	return (v1Height > v2Height) ? v1Height : v2Height;
    }	
    
    /**
     * Get the position of the entity. For segment entities, this is always
     * the position of the start vertex with the height at the floor.
     *
     * @param pos The position
     */
    public void getPosition(double[] pos) {

    	startVertex.getPosition(pos);
		pos[2] = -pos[1];
    	pos[1] = 0.0;
    }
    
    /**
     * Get the current rotation of the entity. For segment entities, this is a 
     * rotation about the y axis dictated by the position of the start and
     * end vertices.
     *
     * @param rot The rotation to return
     */
    public void getRotation(float[] rot) {

    	// Calculate the angle of the wall		
		double[] p0 = new double[3];
		double[] p1 = new double[3];
		
		// StartVertex will always be the left vertex, EndVertex will always
		// refer to the right vertex.
		startVertex.getPosition(p0);
		endVertex.getPosition(p1);
		
		// vertices are spec'ed on the xy plane
		double delta_x = p1[0] - p0[0];
		double delta_y = p1[1] - p0[1];
		
		float angle = 0;
		if ((delta_x != 0) || (delta_y != 0)) {
			angle = (float)Math.atan2(delta_y, delta_x);
		} 
		/////////////////////////////////////////////////////////////////

		rot[0] = 0.0f;
		rot[1] = 1.0f;
		rot[2] = 0.0f;
		rot[3] = angle;
    }
	
	/**
	 * Set the starting vertex.
	 * 
	 * @param startIndex
	 *            The start vertex
	 */
	public void setStartVertex(VertexEntity newStartVertex) {
		startVertex = newStartVertex;
	}
	
	/**
	 * Set the starting vertex.
	 * 
	 * @param startIndex
	 *            The start vertex
	 */
	public void setEndVertex(VertexEntity newEndVertex) {
		endVertex = newEndVertex;
		
		//getBounds(new float[6]);
	}
	
	/**
	 * 
	 * @return an array of length 24 containing the x, y, and z
	 * points of each vertex of this segment.
	 */
	public float[] generateVertices(){

		// build the segmentVector and record its length
		getLength();

		//
        //  Get the vertex heights
        //
		Float firstVertexHeight = (Float) startVertex.getProperty(
        		Entity.EDITABLE_PROPERTIES, 
        		VertexEntity.HEIGHT_PROP);
        
        Float secondVertexHeight = (Float) endVertex.getProperty(
        		Entity.EDITABLE_PROPERTIES, 
        		VertexEntity.HEIGHT_PROP);
		
        if(firstVertexHeight == null || secondVertexHeight == null){
        	//TODO VertexHeight = DEFAULT_HEIGHT;
        }
        
		// Get the wall thickness
        Object prop = getProperty(
                Entity.EDITABLE_PROPERTIES,
                SegmentEntity.WALL_THICKNESS_PROP);
        
        float wallThickness = SegmentEntity.DEFAULT_WALL_THICKNESS;
        if (prop instanceof ListProperty) {
            ListProperty list = (ListProperty)prop;
            wallThickness = Float.parseFloat(list.getSelectedValue());
        } 
		
		//-------------------------------------------------------
		// Get down to business and do the math!!!!
		//-------------------------------------------------------
		float[] existingVertices = new float[24];		
		float[] newCoords = 
		    generateSquareEndCoordinates(
				segmentVector, 
				firstVertexPosD, 
				firstVertexHeight,
				wallThickness);
		
		existingVertices[12] = newCoords[0];
		existingVertices[13] = newCoords[1];
		existingVertices[14] = newCoords[2];
		
		existingVertices[15] = newCoords[3];
		existingVertices[16] = newCoords[4];
		existingVertices[17] = newCoords[5];
		
		existingVertices[18] = newCoords[6];
		existingVertices[19] = newCoords[7];
		existingVertices[20] = newCoords[8];
		
		existingVertices[21] = newCoords[9];
		existingVertices[22] = newCoords[10];
		existingVertices[23] = newCoords[11];

			
		newCoords = 
		    generateSquareEndCoordinates(
				segmentVector, 
				secondVertexPosD, 
				secondVertexHeight,
				wallThickness);
			
		existingVertices[0] = newCoords[0];
		existingVertices[1] = newCoords[1];
		existingVertices[2] = newCoords[2];
		
		existingVertices[3] = newCoords[3];
		existingVertices[4] = newCoords[4];
		existingVertices[5] = newCoords[5];
		
		existingVertices[6] = newCoords[6];
		existingVertices[7] = newCoords[7];
		existingVertices[8] = newCoords[8];
		
		existingVertices[9] = newCoords[9];
		existingVertices[10] = newCoords[10];
		existingVertices[11] = newCoords[11];
		
		return existingVertices;
	}
	
	/**
	 * Get the bounds in the local coordinate system of the wall. X axis is the
	 * length of the wall, Y axis is the height of the wall, Z axis is the depth
	 * of the wall.
	 * 
	 * @param bounds float[] length 6
	 */
	public void getLocalBounds(float[] bounds) {
		
		float length = getLength();
		float height = getHeight();
		
        Object prop = getProperty(
                Entity.EDITABLE_PROPERTIES,
                SegmentEntity.WALL_THICKNESS_PROP);
        
        float wallThickness = SegmentEntity.DEFAULT_WALL_THICKNESS;
        if (prop instanceof AbstractProperty) {
            ListProperty list = (ListProperty)prop;
            wallThickness = Float.parseFloat(list.getSelectedValue());
        }
        
        bounds[0] = -length/2.0f;
        bounds[1] = length/2.0f;
        bounds[2] = -wallThickness/2.0f;
        bounds[3] = wallThickness/2.0f;
        bounds[4] = -height/2.0f;
        bounds[5] = height/2.0f;
	}
	
	/**
	 * Set the bounds of the segment into the given
	 * float array (which must be at least length 6).
	 * @author Eric Fickenscher
	 * @param bounds a float array with length at least 6
	 */
	public void getBounds(float[] bounds){
		
		if(startVertex == null || endVertex == null){
			return;
		}
		
		float[] vertices = generateVertices();

		int i = 0;
		
		float xMin = vertices[i];
		float xMax = vertices[i++];
		float yMin = vertices[i];
		float yMax = vertices[i++];
		float zMin = vertices[i];
		float zMax = vertices[i++];
		
		while( i < 24 ){
			
			if(vertices[i] < xMin )
				xMin = vertices[i];
			if(vertices[i] > xMax )
				xMax = vertices[i];
			
			i++;
			
			if(vertices[i] < yMin )
				yMin = vertices[i];
			if(vertices[i] > yMax )
				yMax = vertices[i];

			i++;
			
			if(vertices[i] < zMin )
				zMin = vertices[i];
			if(vertices[i] > zMax )
				zMax = vertices[i];
			
			i++;
		}
		bounds[0] = xMin;
		bounds[1] = xMax;
		bounds[2] = yMin;
		bounds[3] = yMax;
		bounds[4] = zMin;
		bounds[5] = zMax;
		
		//System.out.println(java.util.Arrays.toString(bounds));
	}
	
	/*
	 * Generate the squared off end coordinates for a segment. Returns the 
	 * coordinates in following order (vector shown is vectorA) 0, 1, 2, 3
	 * 
	 *    					0-------3
	 *    					|       |
	 *    					|  	/	|
	 *    					|  /	|
	 *    					1-/-----2
	 *                       /
	 *                     |/_
	 * 
	 * @param vectorA Vector3f representing the segment
     * @param vertexPosition double[] xyz position of squared off end vertex 
     * @param vertexHeight the vertex height to use
     * @param wallThickness The depth of a wall (how thick is the wall)
	 */
	private float[] generateSquareEndCoordinates(
	        Vector3f vectorOriginal, 
	        double[] vertexPosition, 
	        float vertexHeight,
	        float wallThickness){
			    
		Vector3f vectorA = new Vector3f();
		Vector3f upVec = new Vector3f(0.0f, 0.0f, 1.0f);
		float[] newCoordinates = new float[12];

		vectorA.cross(upVec, vectorOriginal);
		vectorA.normalize();
		
		float xAdjustment = vectorA.x * (wallThickness);
		float yAdjustment = vectorA.y * (wallThickness);
		
		//
		// Establish new coordinates
		// ---------------------------------------------------------
		// EMF: Note how I have switched the vertex creation from a
		// x-z plane into one that uses the x-y plane.  This should be
		// more consistent with our new concept of treating all views
		// as 'front on'.  (Compare to commented-out code below)
		// ---------------------------------------------------------
		newCoordinates[0] = (float)vertexPosition[0]; 
		newCoordinates[1] = (float)vertexPosition[1]; 
		newCoordinates[2] = vertexHeight;
		
		newCoordinates[3] = (float)vertexPosition[0]; 
		newCoordinates[4] = (float)vertexPosition[1] - yAdjustment;
		newCoordinates[5] = 0.0f;
		
		newCoordinates[6] = (float)vertexPosition[0] - xAdjustment;
		newCoordinates[7] = (float)vertexPosition[1] - yAdjustment;
		newCoordinates[8] = 0.0f;
		
		newCoordinates[9] = (float)vertexPosition[0] - xAdjustment;
		newCoordinates[10] = (float)vertexPosition[1];
		newCoordinates[11] = vertexHeight;
		/*
		newCoordinates[0] = (float)vertexPosition[0]; //+ xAdjustment;
		newCoordinates[1] = vertexHeight;
		newCoordinates[2] = (float)vertexPosition[2]; // + zAdjustment;
		
		newCoordinates[3] = (float)vertexPosition[0]; // + xAdjustment;
		newCoordinates[4] = 0.0f;
		newCoordinates[5] = (float)vertexPosition[2]; // + zAdjustment;
		
		newCoordinates[6] = (float)vertexPosition[0] - xAdjustment;
		newCoordinates[7] = 0.0f;
		newCoordinates[8] = (float)vertexPosition[2] - zAdjustment;
		
		newCoordinates[9] = (float)vertexPosition[0] - xAdjustment;
		newCoordinates[10] = vertexHeight;
		newCoordinates[11] = (float)vertexPosition[2] - zAdjustment;
		*/
		return newCoordinates;
	}
	
	/**
	 * Is this segment used to define the exterior shape
	 * 
	 * @return the exteriorSegment
	 */
	public boolean isExteriorSegment() {
		return (Boolean) getProperty(propertySheetName, EXTERIOR_SEGMENT_PROP);
	}

	
	/**
	 * Set whether this segment is used to define the exterior shape
	 * 
	 * @param exteriorSegment
	 *            the exteriorSegment to set
	 */
	public void setExteriorSegment(boolean exteriorSegment) {
		setProperty(propertySheetName, EXTERIOR_SEGMENT_PROP, exteriorSegment,
				false);
	}

	
	/**
	 * Get the startVertexEntity
	 * 
	 * @return VertexEntity
	 */
	public VertexEntity getStartVertexEntity() {

        boolean facing = getStandardFacing();
        
        if (facing) {
            return startVertex;
        } else {
            return endVertex;
        }
        
	}
	
	
	/**
	 * Get the endVertexEntity
	 * 
	 * @return VertexEntity
	 */
	public VertexEntity getEndVertexEntity() {
	    
	    boolean facing = getStandardFacing();
	    
        if (facing) {
            return endVertex;
        } else {
            return startVertex;
        }
        
	}
	
    /**
     * Return the vector defining the face of the segment
     * from left to right.  Is determined by the facing property.
     *
     * @param segment The segment entity
     * @return The face vector
     */
    public Vector3d getFaceVector() {
        
        double[] rght = new double[3];
        double[] left = new double[3];
		
		VertexEntity rght_ve = getEndVertexEntity();
		VertexEntity left_ve = getStartVertexEntity();
		
		rght_ve.getPosition(rght);
		left_ve.getPosition(left);
		
        Vector3d face = new Vector3d(
            rght[0] - left[0],
            rght[1] - left[1],
            0);
        
        return(face);
    }

    /**
     * 
     * @return
     */
	public boolean getStandardFacing() {
        
        // Get the standard facing
        Object prop = getProperty(
                Entity.EDITABLE_PROPERTIES,
                SegmentEntity.STANDARD_FACING_PROP);
        
        boolean facing = true;
        if (prop instanceof Boolean) {
            facing = (Boolean)prop;
        }

        return facing;
	}
}
