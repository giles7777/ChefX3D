/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
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

// Internal Imports

/**
 * A single vertex.  Will be a child of a SegmentableEntity
 *
 * @author Russell Dodds
 * @version $Revision: 1.31 $
 */
public class VertexEntity extends BasePositionableEntity {
   
	public static final String VERTEX_PROPERTY_SHEET = "VertexEntity.propertySheet";

	/** 
	 * If the vertex is being used to represent walls or fences then it will
	 * have a height.
	 */ 
	public static final String HEIGHT_PROP = "org.chefx3d.model.VertexEntity.height";
   
    /**
     * Create a new vertex using properties
     * 
     * defaultProperties cannot be null, and it must include a mapping for:
     * sheetName -> Entity.POSITION_PROP
     *
     * @param entityID The vertexID
     * @param position The world coordinate system location
     * @param defaultProperties The property sheet map (sheet -> propMap)
     */
    public VertexEntity(
            int entityID,
            Map<String, Map<String, Object>> toolProperties) {
        
    	this(entityID, DEFAULT_ENTITY_PROPERTIES, toolProperties);   
    }

	/**
	 * Construct with default param sheet name.
	 * 
	 * @param entityID
	 * @param positionParamsSheet
	 * @param toolProperties
	 */
	public VertexEntity(
	        int entityID, 
	        String propertySheetName,
			Map<String, Map<String, Object>> toolProperties) {
	    
	    this(entityID, propertySheetName, DEFAULT_ENTITY_PROPERTIES, toolProperties);
	}

	/**
	 * The constructor. The entity properties will be copied from the tool
	 * defaults.
	 * 
	 * @param entityID
	 *            The entity ID
	 * @param baseSheetName
	 *            The name of the sheet that contains the basic properties
	 * @param positonableSheetName
	 *            The name of the sheet that contains the position properties
	 * @param toolProperties
	 *            The properties of an entity as defined by the tool
	 */
	public VertexEntity(
	        int entityID, 
	        String propertySheetName,
			String positionPropertySheet,
			Map<String, Map<String, Object>> toolProperties) {

		super(entityID, propertySheetName, positionPropertySheet, toolProperties);
	
	}

	/**
	 * The constructor. The entity properties will be copied from the tool
	 * defaults.
	 * 
	 * @param entityID
	 *            The entity ID
	 * @param baseSheetName
	 *            The name of the sheet that contains the basic properties
	 * @param positonableSheetName
	 *            The name of the sheet that contains the position properties
	 * @param toolProperties
	 *            The properties of an entity as defined by the tool
	 * @param toolParams
	 *            The params of an entity
	 */
	private VertexEntity(
	        int entityID, 
	        String propertySheet, 
	        String positionPropertySheet,
			Map<String, Object> toolParams,
			Map<String, Map<String, Object>> toolProperties) {
	    
		super(entityID, propertySheet, positionPropertySheet, toolParams, toolProperties);
	
	}

	
    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

	/**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public VertexEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();
    	
        // Create the new copy
        VertexEntity clonedEntity =
            new VertexEntity(clonedID,
                              propertySheetName,
                              positionPropertySheet, 
                              params,
                              properties);

        // copy all the other data over
        clonedEntity.children = new ArrayList<Entity>();
    	
    	int len = children.size();
    	for (int i = 0; i < len; i++) {   		
    		Entity clone = children.get(i).clone(issuer);    		
    		clonedEntity.children.add(clone);
    	}

        return clonedEntity;
    }

    /**
     * Compare the given details to this one to see if they are equal. Equality
     * is defined as pointing to the same clipPlane source, with the same
     * transformation value.
     *
     * @param o The object to compare against
     * @return true if these represent identical objects
     */
    public boolean equals(Object o) {

        if (!(o instanceof VertexEntity))
            return false;

        Entity e = (Entity) o;

        if (e.getEntityID() != entityID)
            return false;

        return true;
    }

    //----------------------------------------------------------
    // Methods defined by Entity
    //----------------------------------------------------------

	public int getType() {
		return TYPE_VERTEX;
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
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
	
    /**
     * Get the height of the vertex
     *
     * @return The height property, 0 if not set
     */
    public Float getHeight() {
        
        Object height = getProperty(Entity.EDITABLE_PROPERTIES, HEIGHT_PROP);
        if (height == null) {
            return 0f;
        } else {
            return (Float)height;
        }
        
    }
    
    /**
     * Set the height of this vertex
     * 
     * @param height - The new height
     */
    public void setHeight(float height) {
        setProperty(Entity.EDITABLE_PROPERTIES, HEIGHT_PROP, height, false);
    }

}
