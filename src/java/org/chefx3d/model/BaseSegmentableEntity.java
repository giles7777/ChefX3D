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
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// Internal Imports
import org.chefx3d.util.PropertyUtilities;

/**
 * An object representation of an entity.  A entity consists of:
 *      - parameters that are defined at creation and never changed
 *      - properties that are updated by the end-user or internal processes
 *
 * Properties can be stored in any number of sheets, the sheets are used to
 * group properties an whatever manner is required by the application
 *
 * Selection and HighLight are not model parameters so they can be public here.
 *
 * @author Russell Dodds
 * @version $Revision: 1.21 $
 */
public abstract class BaseSegmentableEntity extends BaseEntity implements
		SegmentableEntity {
	
    /** The tool used to create new vertices */
    protected AbstractVertexTool vertexTool;

    /** The tool used to create new segments */
    protected AbstractSegmentTool segmentTool;
    
	/** A lookup map of vertices identified by an ID */
    protected HashMap<Integer, VertexEntity> vertexMap;
    
    /** A map of the number of segments used by a vertex */
    protected HashMap<VertexEntity, Integer> segmentCount;

    /** A list of vertices, ordered by creation */
    protected ArrayList<VertexEntity> vertexList;
    
    /** A lookup map of segments identified by an ID */
    protected HashMap<Integer, SegmentEntity> segmentMap;

    /** The transform for the sequence */
    protected AffineTransform transform;
    
    
    private int labelCount;
        
    /**
     * Create an entity from default param sheet names.
     * 
     * @param entityID
     * @param toolProperties
     */
	public BaseSegmentableEntity(
	        int entityID, 
	        Map<String, Map<String, Object>> toolProperties, 
            AbstractSegmentTool segmentTool,  
            AbstractVertexTool vertexTool) {
	    
		this(entityID, DEFAULT_ENTITY_PROPERTIES, toolProperties, segmentTool, vertexTool);
	}
		
	/**
	 * Use custom sheet names for params.
	 * 
	 * @param entityID
	 * @param segmentableParamsSheet
	 * @param paramSheetName
	 * @param toolProperties
	 */
	public BaseSegmentableEntity(
	        int entityID, 
	        String propertySheetName, 
	        Map<String, Map<String, Object>> toolProperties, 
	        AbstractSegmentTool segmentTool, 
	        AbstractVertexTool vertexTool) {
	    
		super(entityID, propertySheetName, toolProperties);
		
        this.vertexTool = vertexTool;
        this.segmentTool = segmentTool;
		
        segmentMap = new HashMap<Integer, SegmentEntity>();
        vertexMap = new HashMap<Integer, VertexEntity>();
        vertexList = new ArrayList<VertexEntity>();
        segmentCount = new HashMap<VertexEntity, Integer>();
        labelCount =0;
        
	}
	
	/**
	 * Specify segmentable params from a Map, and use default entity param location.
	 * 
	 * @param entityID
	 * @param segmentableParams
	 * @param toolProperties
	 */
	public BaseSegmentableEntity(
	        int entityID, 
	        String propertySheetName, 
	        Map<String, Object> segmentableParams, 
	        Map<String, Map<String, Object>> toolProperties, 
            AbstractSegmentTool segmentTool,  
            AbstractVertexTool vertexTool) {
	    
		super(entityID, propertySheetName, segmentableParams, toolProperties);
		
	    this.vertexTool = vertexTool;
	    this.segmentTool = segmentTool;
		
        segmentMap = new HashMap<Integer, SegmentEntity>();
        vertexMap = new HashMap<Integer, VertexEntity>();
        vertexList = new ArrayList<VertexEntity>();
        segmentCount = new HashMap<VertexEntity, Integer>();
        
        labelCount =0;
	}
		
	/**
     * Is this entity a line tool
     *
     * @return true if a single line, false if a network of lines
     */
    public boolean isLine() {
        return (Boolean)params.get(IS_LINE_PROP);
    }

    /**
     * Get the fixedLength this entity is assigned to
     *
     * @return true if the segments are fixed length, false otherwise
     */
    public boolean isFixedLength() {

        float length = (Float)params.get(FIXED_LENGTH_PROP);

        if (length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the name of the Tool that created this segment entity
     *
     * @return The tool name
     */
    public String getToolName() {
        return (String)params.get(TOOL_NAME_PROP);
    }

    /**
     * Get a segment
     *
     * @param segmentID - The segment to lookup
     * @return The segment found, null if not found
     */
    public SegmentEntity getSegment(int segmentID) {
        return segmentMap.get(segmentID);
    }

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
    public void addSegment(SegmentEntity segment) {

        // a line does not care about a shape
        segment.setExteriorSegment(false);

        // add the vertex to the lookup map for easy access
        segmentMap.put(segment.getEntityID(), segment);
        
        addChild(segment);
        
        VertexEntity startVertex = segment.getStartVertexEntity();
        VertexEntity endVertex = segment.getEndVertexEntity();
        
        if (segmentCount.containsKey(startVertex)) {
        	segmentCount.put(startVertex,
        			Integer.valueOf(segmentCount.get(startVertex).intValue() + 1));
        }
        
        if (segmentCount.containsKey(endVertex)) {
        	segmentCount.put(endVertex,
        			Integer.valueOf(segmentCount.get(endVertex).intValue() + 1));
        }
    }

    /**
     * DO NOT USE - Use Commands
     * Remove a segment from this entity.
     *
     * @param segmentID - The segment to remove
     */
    public void removeSegment(int segmentID) {

        if (segmentMap.keySet().contains(segmentID)) {

            SegmentEntity segment = segmentMap.get(segmentID);
            segmentMap.remove(segmentID);
            
            VertexEntity startVertex = segment.getStartVertexEntity();
            VertexEntity endVertex = segment.getEndVertexEntity();

            if (segmentCount.containsKey(startVertex)) {
                segmentCount.put(startVertex,
                        Integer.valueOf(segmentCount.get(startVertex).intValue() - 1));
            }

            if (segmentCount.containsKey(endVertex)) {
                segmentCount.put(endVertex,
                        Integer.valueOf(segmentCount.get(endVertex).intValue() - 1));
            }

            removeChild(segment);
        }
    }

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
            int index) {
    	
    	// Get the vertex id
    	int vertexID = vertex.getEntityID();

        // add the vertex to the lookup map for easy access
        vertexMap.put(vertexID, vertex);
        vertexList.add(vertex);

        // add the children list
        if (this.hasChildren() && index >= 0 ) {

            // add the vertex to the ordered list
            insertChildAt(index, vertex);

        } else {
        	
            addChild(vertex);
        }

        segmentCount.put(vertex, Integer.valueOf(0));
        
        // return where is was actually placed
        return getChildIndex(vertexID);
    }

    /**
     * DO NOT USE - Use Commands
     * Move a vertex of this SegmentSequence.
     *
     * @param vertexID The vertexID
     * @param pos The position of the segment
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void moveVertex(int vertexID, double[] pos, boolean ongoing) {

        VertexEntity vertex = vertexMap.get(vertexID);
        vertex.setPosition(pos, ongoing);
    }

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
            String propValue, boolean ongoing) {

        VertexEntity vertex = vertexMap.get(vertexID);
        vertex.setProperty(propSheet, propName, propValue, ongoing);
    }

    /**
     * DO NOT USE - Use Commands
     * Remove a vertex from this entity.
     *
     * @param vertexID - The vertex to remove
     */
    public void removeVertex(int vertexID) {
        
        // get the entity
        VertexEntity vertex = vertexMap.get(vertexID);

        // remove from the children list
        removeChild(vertex);

        // remove the vertex from the lookup map
        vertexMap.remove(vertexID);
                
        // Remove the vertex from the list
        vertexList.remove(vertex);
                  
        segmentCount.remove(vertex);
    }

    /**
     * Get a vertex
     *
     * @param vertexID - The vertex to lookup
     * @return The vertex found, null if not found
     */
    public VertexEntity getVertex(int vertexID) {

        return vertexMap.get(vertexID);
    }
    
    /**
     * Get a vertex by index 
     * 
     * @param vertexIndex - The index of the vertex to look up
     * @return The vertex found, null if not found
     */
    public VertexEntity getVertexByIndex(int vertexIndex){
    	if (vertexIndex >= 0 && vertexIndex < vertexList.size()) {
    		return vertexList.get(Integer.valueOf(vertexIndex));
    	}
    	return null;
    }
    
    /**
     * Convert between vertex id and vertex index
     * 
     * @param vertexID - The ID of the vertex to lookup
     * @return vertexIndex - The index of the vertex, -1 if not found
     */
    public int getVertexIndex(int vertexID){
    	VertexEntity vertex = getVertex(vertexID);
    	if (vertex != null) {
    		return vertexList.indexOf(vertex);
		}
    	return -1;
    }

    /**
     * Get the list of segments. If the entity contains no segments it
     * will return null.
     *
     * @return The list of segments
     */
    public ArrayList<SegmentEntity> getSegments() {

        ArrayList<SegmentEntity> segments = new ArrayList<SegmentEntity>();

        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity check = children.get(i);
            if (check instanceof SegmentEntity) {
                segments.add((SegmentEntity)check);
            }
        }

        return segments;
    }

    /**
     * Get the list of segments containing the specified vertex. 
	 * If the entity contains no segments or the vertex is not 
	 * contained in any available segments, it will return null.
     *
	 * @param ve The VertexEntity to search the segments for.
     * @return The list of segments, or null
     */
    public ArrayList<SegmentEntity> getSegments(VertexEntity ve) {

		ArrayList<SegmentEntity> segments = null;
		
		Integer num = segmentCount.get(ve);
		if ((num != null) && (num > 0)) {
			segments = new ArrayList<SegmentEntity>();
			
			int len = children.size();
			for (int i = 0; i < len; i++) {
				Entity e = children.get(i);
				if (e instanceof SegmentEntity) {
					SegmentEntity se = (SegmentEntity)e;
					if (ve == se.getStartVertexEntity()) {
						segments.add(se);
					} else if (ve == se.getEndVertexEntity()) {
						segments.add(se);
					}
				}
			}
		}
        return segments;
    }

    /**
     * Get the list of vertices. If the entity contains no vertices it
     * will return null.
     *
     * @return The list of vertices
     */
    public ArrayList<VertexEntity> getVertices() {

        ArrayList<VertexEntity> vertices = new ArrayList<VertexEntity>();

        int len = children.size();
        if (len <= 0)
            return null;

        for (int i = 0; i < len; i++) {
            Entity check = children.get(i);
            if (check instanceof VertexEntity) {
                vertices.add((VertexEntity)check);
            }
        }

        return vertices;
    }

    /**
     * Return true is the vertexID matches the starting ID
     *
     * @param vertexID - The vertex to check
     * @return true is vertex is at the start
     */
    public boolean isStart(int vertexID) {
    	int test = getStartVertexID();
        return (test == vertexID);
    }

    /**
     * Get the first vertex of the sequence
     *
     * @return The position or null if no start vertex
     */
    public double[] getStartPosition() {

        int len = children.size();
        if (len <= 0)
            return null;

        VertexEntity vertex = null;
        for (int i = 0; i < len; i++) {
            Entity check = children.get(i);
            if (check instanceof VertexEntity) {
                vertex = (VertexEntity)check;
                break;
            }
        }

        if (vertex == null)
            return null;

        double[] pos = new double[3];
        vertex.getPosition(pos);

        return pos;
    }

    /**
     * Return true is the vertexID matches the ending ID
     *
     * @param vertexID - The vertex to check
     * @return true is vertex is at the end
      */
    public boolean isEnd(int vertexID) {
    	int test = getEndVertexID();
        return (test == vertexID);
    }

    /**
     * Get the last vertex of the sequence
     *
     * @return The position or null if no end vertex
     */
    public double[] getEndPosition() {

        int len = children.size();
        if (len <= 0) {
            return null;
		}

        VertexEntity vertex = null;
        for (int i = len - 1; i >= 0; i--) {
            Entity check = children.get(i);
            if (check instanceof VertexEntity) {
                vertex = (VertexEntity)check;
                break;
            }
        }

        if (vertex == null) {
            return null;
		}

        double[] pos = new double[3];
        vertex.getPosition(pos);

        return pos;
    }

    /**
     * Get the spacial transformation object
     *
     * @return The matrix transform
     */
    public AffineTransform getTransform() {
        return transform;
    }

    /**
     * Set the spacial transformation object
     *
     * @param transform - The matrix transform to set
     */
    public void setTransform(AffineTransform transform) {
        this.transform = transform;
    }

    /**
     * Get the ID of the last vertex of the sequence. If no verticies are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getStartVertexID() {

        int len = vertexList.size();
        if (len <= 0) {
            return -1;
		}

        return vertexList.get(0).getEntityID();
    }

    /**
     * Get the ID of the last vertex of the sequence. If no verticies are
     * defined, -1 is returned
     *
     * @return The ID of the last vertex of the sequence
     */
    public int getEndVertexID() {

        int len = vertexList.size();
        if (len <= 0) {
            return -1;
		}

        return vertexList.get(len-1).getEntityID();
    }
	
    /**
     * Get the first segment in the list
     *
     * @return The the first segment ID
     */
    public int getFirstSegmentID() {

        // get the last vertex ID since the vertices
        // are the only guaranteed ordered set
        int firstVertexID = getStartVertexID();

        int len = children.size();
        if (len <= 0) {
            return -1;
		}

        for (int i = len - 1; i >= 0; i--) {
            Entity check = children.get(i);
            if (check instanceof SegmentEntity &&
               ((SegmentEntity)check).getStartVertexEntity().getEntityID() == firstVertexID) {

                return check.getEntityID();
            }
        }

        return -1;
    }
    
    /**
     * Get the last segment in the list
     *
     * @return The the last segment ID
     */
    public int getLastSegmentID() {

        // get the last vertex ID since the vertices
        // are the only guaranteed ordered set
        int lastVertexID = getEndVertexID();

        int len = children.size();
        if (len <= 0) {
            return -1;
		}

        for (int i = len - 1; i >= 0; i--) {
            Entity check = children.get(i);
            if (check instanceof SegmentEntity &&
                ((SegmentEntity)check).getEndVertexEntity().getEntityID() == lastVertexID) {

                return check.getEntityID();
            }
        }

        return -1;
    }

    /**
     * Does the sequence contain the position provided
     *
     * @param pos - The vertex position
     * @return True if a vertex exists at the position
     */
    public boolean contains(double[] pos) {

        int vertexID = getVertexID(pos);

        return (vertexID >= 0);
    }

    /**
     * Get the vertexId for the position specified,
     *  returns the ID of the first vertex matched
     *
     * @param pos - Vertex position to look for
     * @return The vertexId found, -1 if not found
     */
    public int getVertexID(double[] pos) {

        int vertexId = -1;

        Iterator<Map.Entry<Integer, VertexEntity>> index =
            vertexMap.entrySet().iterator();

        while (index.hasNext()) {

            Map.Entry<Integer, VertexEntity> mapEntry = index.next();

            VertexEntity vertex = mapEntry.getValue();
            double[] position = new double[3];
            vertex.getPosition(position);

            if ((pos[0] == position[0]) &&
                    (pos[1] == position[1]) &&
                    (pos[2] == position[2])) {

                vertexId = mapEntry.getKey();
                break;
            }

        }

        return vertexId;
    }
   
    /**
     * Get the tool used to create vertices
     * 
     * @return The VertexTool to use
     */
    public AbstractVertexTool getVertexTool() {
        return vertexTool;
    }
    
    /**
     * Get the tool used to create segments
     * 
     * @return The SegmentTool to use
     */
    public AbstractSegmentTool getSegmentTool() {
        return segmentTool;
    }
    
    /**
     * Checks to see if a vertex entity has multiple segments connected to it
	 *
     * @param entity the vertex  entity to check
     * @return Returns the numbers of segments connected to the vertex
     */

    public int getSegmentCount(VertexEntity entity){
		if (segmentCount.containsKey(entity)) {
			return segmentCount.get(entity);
		} else {
			return 0;
		}
    }
    
    /**
     * Get the bounds of the segmentable entity's vertices.  If the
     * entity contains no vertices it will return null.
     * 
     * @return an array of length six containing, in order: the minimum
     * x value, maximum x value, minimum y value, maximum y value, minimum
     * z value and maximum z value. 
     * @author Eric Fickenscher
     */
    public double[] getBounds(){
    	
    	// ensure that this segmentable entity contains vertices
    	ArrayList<VertexEntity> vertices = this.getVertices();
    	if(vertices == null || vertices.size() == 0)
    		return null;
    	
    	// grab the first vertex
        VertexEntity vertex = vertices.get(0);
        double[] vertexPos = new double[3];
        vertex.getPosition(vertexPos);

        //
        // these six values represent the real-world minimum
        // and maximum bounds around the SegmentableEntity.
        //
        double minX = vertexPos[0];
        double maxX = vertexPos[0];
        double minY = vertexPos[1];
        double maxY = vertexPos[1];
        double minZ = vertexPos[2];
        double maxZ = vertexPos[2];
    	
    	// iterate through the list of vertices and update 
        // the maximum and minimum bounds
    	for(int i = 0; i < vertices.size(); i++){
            vertex = vertices.get(i);
            vertex.getPosition(vertexPos);

            if (vertexPos[0] > maxX)
                maxX = vertexPos[0];
            if (vertexPos[0] < minX)
                minX = vertexPos[0];
            
            if (vertexPos[1] > maxY)
                maxY = vertexPos[1];
            if (vertexPos[1] < minY)
                minY = vertexPos[1];
            
            if (vertexPos[2] > maxZ)
                maxZ = vertexPos[2];
            if (vertexPos[2] < minZ)
                minZ = vertexPos[2];	
    	}
    	return new double[]{minX, maxX, minY, maxY, minZ, maxZ};
    }

    /**
     *  Generates a letter to be used for the description of the segment
     *  For example, the first wall added is A, the 26th wall is AA, 
     *  the 52nd wall should be AAA due to starting count from 0
     */
    public void setCurrentSegmentDesc(SegmentEntity segment) {
        
        
        Boolean ghost = (Boolean)segment.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES, Entity.SHADOW_ENTITY_FLAG);
        if ((ghost != null)  && (ghost == true)) {
            return;
        }
        
        String  label = "";
        char letter = 'A';
        if (labelCount  == 0) {
            label = "A";

        } else if (labelCount <26){
            label += Character.toString((char)(letter+labelCount));

        } else {
            int letterCount = labelCount%25;
            int tempLabelCount = labelCount - (25*letterCount);

            for(int j = 0;j <letterCount; j++) {
                label += Character.toString((char)(letter+tempLabelCount));
            }
        }
        labelCount ++;
        segment.setDescription(label);
    }

    /**
     * Relabels all of the segments currently in the scene
     * 
     * @param excludeEntityID - for removal purposes to make sure not to count this entity
     * @return
     */
    public void relabelSegments(int excludeEntityID) {
        SegmentEntity excludedSegment = this.getSegment(excludeEntityID);
        
        Boolean ghost = (Boolean)excludedSegment.getProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES, Entity.SHADOW_ENTITY_FLAG);
        if ((ghost != null)  && (ghost == true)) {
            return;
        }
        
        Object[] values = segmentMap.keySet().toArray();
        labelCount = 0;
        ArrayList<Integer> sortList = new ArrayList<Integer>();
        for (int j = 0; j < values.length; j++) {
            
            sortList.add((Integer)values[j]);
        }
        
        Collections.sort(sortList);
        
        for(int i =0; i < values.length;i++) {
            SegmentEntity segment = segmentMap.get(sortList.get(i));
            
            if(segment.getEntityID() == excludeEntityID) {
                continue;
            }
            this.setCurrentSegmentDesc(segment);
            
        }
    }
}
