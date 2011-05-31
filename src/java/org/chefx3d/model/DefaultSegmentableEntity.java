package org.chefx3d.model;

import java.util.ArrayList;
import java.util.Map;

public class DefaultSegmentableEntity extends BaseSegmentableEntity {
	
 
	/**
	 * Create an entity from default param sheet names.
	 * 
	 * @param entityID
	 * @param toolProperties
	 */
    public DefaultSegmentableEntity(
            int entityID, 
	        Map<String, Map<String, Object>> toolProperties, 
	        AbstractSegmentTool segmentTool,  
	        AbstractVertexTool vertexTool) {
       
	    super(entityID, DEFAULT_ENTITY_PROPERTIES, toolProperties, segmentTool, vertexTool);
	  
    }
       
	/**
	 * Use custom sheet names for params.
	 * 
	 * @param entityID
	 * @param segmentableParamsSheet
	 * @param paramSheetName
	 * @param toolProperties
	 */
    public DefaultSegmentableEntity(
            int entityID, 
            String propertySheetName, 
            Map<String, Map<String, Object>> toolProperties, 
            AbstractSegmentTool segmentTool, 
            AbstractVertexTool vertexTool) {
       
        super(entityID, propertySheetName, toolProperties, segmentTool, vertexTool);
    }
   
    /**
     * Specify segmentable params from a Map, and use default entity param location.
     * 
     * @param entityID
     * @param segmentableParams
     * @param toolProperties
     */
    public DefaultSegmentableEntity(
            int entityID, 
            String propertySheetName, 
            Map<String, Object> segmentableParams,           
            Map<String, Map<String, Object>> toolProperties, 
            AbstractSegmentTool segmentTool,  
            AbstractVertexTool vertexTool) {
       
        super(entityID, propertySheetName, segmentableParams, toolProperties, segmentTool, vertexTool);
    }

    /**
     * Get the type of this entity
     *
     * @return The type property
     */
	public int getType() {
		return TYPE_MULTI_SEGMENT;
	}
	
    /**
     * Check if the type of this entity is one of the zone types
     * 
     * @return True if one of the zone types, false otherwise
     */
    public boolean isZone() {
    	return false;
    }
    
    /**
     * Check if the type of this entity is one of the model types
     * 
     * @return True if one of the model types, false otherwise
     */
    public boolean isModel() {
    	return false;
    }

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public DefaultSegmentableEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();
        
        // Create the new copy
        DefaultSegmentableEntity clonedEntity =
            new DefaultSegmentableEntity(
                    clonedID,
                    propertySheetName, 
                    params,
                    properties, 
                    segmentTool, 
                    vertexTool);

        // copy all the other data over
        clonedEntity.children = new ArrayList<Entity>();
    	
    	int len = children.size();
    	for (int i = 0; i < len; i++) {   		
    		Entity clone = children.get(i).clone(issuer);  
			clone.setParentEntityID(clonedID);   		
    		clonedEntity.children.add(clone);
    	}

        return clonedEntity;
        
    }


  
}
