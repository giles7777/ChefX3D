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

//External Imports
import java.util.ArrayList;
import java.util.Map;


// Internal Imports

/**
 * An object representation of an Emerson Product.  
 * 
 * @author Russell Dodds
 * @version $Revision: 1.10 $
 */
public class ZoneEntity extends BasePositionableEntity 
    implements Cloneable {
	
	/** 
	 * Entity type of the ZoneEntity. This should never be TYPE_SEGMENT since
	 * there is an implementation of SegmentEntity. Legal values would be
	 * TYPE_ZONE, TYPE_MODEL_WITH_ZONES, TYPE_MODEL_ZONE, 
	 * TYPE_GROUNDPLANE_ZONE
	 */
	protected int entityType = Entity.TYPE_ZONE;

	/**
	 * Default constructor
	 * 
	 * @param entityID The entityID
	 * @param entityType The entity type for this zone
	 * @param sheetName The name of the sheet that contains the properties
	 * @param positionableSheetName The name of the sheet that contains the 
	 * position properties
	 * @param toolProperties The properties of an entity as defined by the tool
	 */
    public ZoneEntity(
    		int entityID, 
    		int entityType,
    		String sheetName, 
    		String positionableSheetName, 
    		Map<String, Map<String, Object>> toolProperties) {
    	
    	super(entityID, sheetName, positionableSheetName, toolProperties);
    	
    	setEntityType(entityType);
    }
    
    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param entityType The entity type for this zone
     * @param propertySheetName The name of the sheet that contains the properties
     * @param toolProperties The properties of an entity as defined by the tool
     */
    public ZoneEntity(
            int entityID,     
            int entityType,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties) {
        
        this(entityID, 
        		entityType, 
        		propertySheetName, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
        		toolProperties);
        
    }
    
    /**
     * Create a new segment using properties
     * 
     * @param entityID The segmentID
     * @param sheetName The name of the base sheet
     * @param params the non-editable params
     * @param defaultProperties The property sheet map (sheet -> document)
     */
    public ZoneEntity(
            int entityID, 
            String propertySheetName,
            String positionableSheetName, 
            Map<String, Object> params, 
            Map<String, Map<String, Object>> defaultProperties) {

        super(entityID, 
        		propertySheetName, 
        		positionableSheetName, 
        		params, 
        		defaultProperties);
    }

    
    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param entityType The entity type for this zone
     * @param propertySheetName The name of the sheet that contains the properties
     * @param toolProperties The properties of an entity as defined by the tool
     * @param toolParams The properties of the entity
     */
    public ZoneEntity(
            int entityID,
            int entityType,
            Map<String, Map<String, Object>> defaultProperties) {
        
        this(entityID, 
        		entityType, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
        		defaultProperties);
        
    }
    
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
    
    /**
     * Check if the type of this entity is one of the model types
     * 
     * @return True if one of the model types, false otherwise
     */
    public boolean isModel() {
    	return false;
    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public ZoneEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();
    	
        // Create the new copy
        ZoneEntity clonedEntity =
            new ZoneEntity(clonedID,
            				  propertySheetName,
            				  positionPropertySheet,
            				  params, 
                              properties);
        
        // make sure to correctly set the zone type
        clonedEntity.setEntityType((Integer)params.get(Entity.TYPE_PARAM));

        // copy all the other data over
        clonedEntity.children = new ArrayList<Entity>();
    	
    	int len = children.size();
    	for (int i = 0; i < len; i++) {   		
    		Entity clone = children.get(i).clone(issuer); 
			clone.setParentEntityID(clonedID);    		
    		clonedEntity.children.add(clone);
    	}

        return(clonedEntity);
   
    }
    
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

    /**
     * Validate that the entity type to save is a valid zone type. If not,
     * set the entity type to the generic TYPE_ZONE.
     */
    private void setEntityType(int entityType) {
    	
    	if (entityType == Entity.TYPE_ZONE ||
    			entityType == Entity.TYPE_MODEL_ZONE ||
    			entityType == Entity.TYPE_GROUNDPLANE_ZONE ||
    			entityType == Entity.TYPE_SEGMENT) {
    		
    		this.entityType = entityType;
    	} else {
    		this.entityType = Entity.TYPE_ZONE;
    	}
    }
}