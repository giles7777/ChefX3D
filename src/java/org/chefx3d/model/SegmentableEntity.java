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

// External Imports
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

// Internal Imports

/**
 * Defines whether an Entity is a Multi-Segment object
 *
 * @author Russell Dodds
 * @version $Revision: 1.29 $
 */
public interface SegmentableEntity extends Entity {

	/** Default sheet name for properties defined in this interface */
	public static final String SEGMENTABLE_PARAMS = "SegmentableEntity.segmentableParams";
	
    /** Constant param names for consistent lookups */
    public static final String TOOL_NAME_PROP = "SegmentableEntity.toolName";
    public static final String IS_LINE_PROP = "SegmentableEntity.isLine";
    public static final String FIXED_LENGTH_PROP = "SegmentableEntity.fixedLength";
    public static final String BRIDGE_VERTICES_ACTION = "SegmentableEntity.bridgeVertices";
    public static final String AFFINE_TRANSFORM_PROP = "SegmentableEntity.affineTransform";

     /**
     * Is this entity a line tool
     *
     * @return true if a single line, false if a network of lines
     */
    public boolean isLine();

    /**
     * Get the fixedLength this entity is assigned to
     *
     * @return true if the segments are fixed length, false otherwise
     */
    public boolean isFixedLength();

    /**
     * Get the name of the Tool that created this segment entity
     *
     * @return The tool name
     */
    public String getToolName();

    /**
     * Get a segment
     *
     * @param segmentID - The segment to lookup
     * @return The segment found, null if not found
     */
    public SegmentEntity getSegment(int segmentID);

    /**
     * DO NOT USE - Use Commands
     * Add a segment to this tool.  Appends to the end of
     * the list
     *
     * @param segmentID - The id issued by the model
     * @param startVertexID - The start vertexID, must be a child
     * @param endVertexID - The start vertexID, must be a child
     * @param exteriorSegment - Is this used to define the exterior shape
     * @param segmentProps - the properties
     */
    public void addSegment(SegmentEntity segment);

    /**
     * DO NOT USE - Use Commands
     * Remove a segment from this entity.
     *
     * @param segmentID - The segment to remove
     */
    public void removeSegment(int segmentID);

    /**
     * DO NOT USE - Use Commands
     * Add a vertex to this entity.
     *
     * @param vertexID - The ID issued by the model
     * @param pos - The position is world coordinates
     * @param index - The ordered position in the list
     * @param vertexProps - The properties
     * @return - Where the vertex was actually placed in the list
     */
    public int addVertex(
            VertexEntity vertex,
            int index);

    /**
     * DO NOT USE - Use Commands
     * Move a vertex of this SegmentSequence.
     *
     * @param vertexID The vertexID
     * @param pos The position of the segment
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void moveVertex(int vertexID, double[] pos, boolean ongoing);

    /**
     * DO NOT USE - Use Commands
     * Update a vertex of this SegmentSequence.
     *
     * @param vertexID The vertexID
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @param propValue The property value
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void updateVertex(
            int vertexID,
            String propSheet,
            String propName,
            String propValue, boolean ongoing);

    /**
     * DO NOT USE - Use Commands
     * Remove a vertex from this entity.
     *
     * @param vertexID - The vertex to remove
     */
    public void removeVertex(int vertexID);

    /**
     * Get a vertex
     *
     * @param vertexID - The vertex to lookup
     * @return The vertex found, null if not found
     */
    public VertexEntity getVertex(int vertexID);

    /**
     * Get a vertex by index 
     * 
     * @param vertexIndex - The index of the vertex to look up
     * @return The vertex found, null if not found
     */
    public VertexEntity getVertexByIndex(int vertexIndex);
    
    /**
     * Convert between vertex id and vertex index
     * 
     * @param vertexID - The ID of the vertex to lookup
     * @return vertexIndex - The index of the vertex, -1 if not found
     */
    public int getVertexIndex(int vertexID);
    
    /**
     * Get the list of segments. If the entity contains no segments it
     * will return null.
     *
     * @return The list of segments
     */
    public ArrayList<SegmentEntity> getSegments();

    /**
     * Get the list of segments containing the specified vertex. 
	 * If the entity contains no segments or the vertex is not 
	 * contained in any available segments, it will return null.
     *
	 * @param ve The VertexEntity to search the segments for.
     * @return The list of segments, or null
     */
    public ArrayList<SegmentEntity> getSegments(VertexEntity ve);
	
    /**
     * Get the list of vertices. If the entity contains no vertices it
     * will return null.
     *
     * @return The list of vertices
     */
    public ArrayList<VertexEntity> getVertices();
    
    /**
     * Get the bounds of the segmentable entity's vertices.  If the
     * entity contains no vertices it will return null.
     * 
     * @return an array of length six containing, in order: the minimum
     * x value, maximum x value, minimum y value, maximum y value, minimum
     * z value and maximum z value. 
     */
    public double[] getBounds();

    /**
     * Return true is the vertexID matches the starting ID
     *
     * @param vertexID - The vertex to check
     * @return true is vertex is at the start
     */
    public boolean isStart(int vertexID);

    /**
     * Get the first vertex of the sequence
     *
     * @return The position or null if no start vertex
     */
    public double[] getStartPosition();

    /**
     * Return true is the vertexID matches the ending ID
     *
     * @param vertexID - The vertex to check
     * @return true is vertex is at the end
      */
    public boolean isEnd(int vertexID);
    
    /**
     * Get the last vertex of the sequence
     *
     * @return The position or null if no end vertex
     */
    public double[] getEndPosition();

    /**
     * Get the spacial transformation object
     *
     * @return The matrix transform
     */
    public AffineTransform getTransform();

    /**
     * Set the spacial transformation object
     *
     * @param transform - The matrix transform to set
     */
    public void setTransform(AffineTransform transform);

    /**
     * Get the ID of the last vertex of the sequence. If no verticies are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getStartVertexID();
    
    /**
     * Get the first segment in the list
     *
     * @return The the first segment ID
     */
    public int getFirstSegmentID();

    /**
     * Get the ID of the last vertex of the sequence. If no verticies are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getEndVertexID();

    /**
     * Get the last segment in the list
     *
     * @return The the last segment ID
     */
    public int getLastSegmentID();

    /**
     * Does the sequence contain the position provided
     *
     * @param pos - The vertex position
     * @return True if a vertex exists at the position
     */
    public boolean contains(double[] pos);

    /**
     * Get the vertexId for the position specified,
     *  returns the ID of the first vertex matched
     *
     * @param pos - Vertex position to look for
     * @return The vertexId found, -1 if not found
     */
    public int getVertexID(double[] pos);
    
    /**
     * Get the tool used to create vertices
     * 
     * @return The VertexTool to use
     */
    public AbstractVertexTool getVertexTool();
    
    /**
     * Get the tool used to create segments
     * 
     * @return The SegmentTool to use
     */
    public AbstractSegmentTool getSegmentTool();
    
    /**
     * Checks to see if a vertex entity has multiple segments connected to it
     * @param entity the vertex  entity to check
     * @return Returns the numbers of segments connected to the vertex
     */
    public int getSegmentCount(VertexEntity entity);
    
           
 
}