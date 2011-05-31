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

// external imports
import java.util.ArrayList;
import java.util.Map;

// internal imports
import org.chefx3d.model.Entity;

/**
 * A viewpoint within the location.
 * 
 * @author Christopher Shankland
 * @version $Revision: 1.15 $
 */
public class ViewpointEntity extends BaseEntity {

    /** The viewpoint transform matrix prop */
    public static final String VIEW_MATRIX_PROP = "ViewpointEntity.viewMatrix";

    /** The default viewpoint transform matrix prop */
    public static final String START_VIEW_MATRIX_PROP = "ViewpointEntity.startViewMatrix";

    /** The viewpoint ortho params prop */
    public static final String ORTHO_PARAMS_PROP = "ViewpointEntity.orthoParams";

    /** The viewpoint projection type prop */
    public static final String PROJECTION_TYPE_PROP = "ViewpointEntity.projectionType";

    /** The view identifier prop */
    public static final String VIEW_IDENTIFIER_PROP = "ViewpointEntity.viewIdentifier";

    /**
     * Create a viewpoint entity.
     * 
     * @param entityID The unique ID
     * @param viewpointName The descriptive name
     * @param matrix The viewpoint transform matrix, must be a float array size 16.
     */
	public ViewpointEntity(
	        int entityID,
	        Map<String, Map<String, Object>> defaultProperties) {
			
		super(entityID, defaultProperties);
	}
	
	/**
     * Create a viewpoint entity.
     * 
     * @param entityID The unique ID
     * @param viewpointName The descriptive name
     * @param matrix The viewpoint transform matrix, must be a float array size 16.
     */
	public ViewpointEntity(
	        int entityID,
	        String propertySheetName,
	        Map<String, Map<String, Object>> defaultProperties) {
			
		super(entityID, propertySheetName, defaultProperties);
	}
	
    /**
     * Create a viewpoint entity.
     * 
     * @param entityID The unique ID
     * @param viewpointName The descriptive name
     * @param matrix The viewpoint transform matrix, must be a float array size 16.
     */
    public ViewpointEntity(
            int entityID,
            String propertySheetName,
            Map<String, Object> toolParams, 
            Map<String, Map<String, Object>> defaultProperties) {
            
        super(entityID, propertySheetName, toolParams, defaultProperties);
    }
	   
    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public ViewpointEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();
    	
        // Create the new copy
        ViewpointEntity clonedEntity =
            new ViewpointEntity(clonedID,
                              propertySheetName,
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

    	return clonedEntity;
    }

    // ----------------------------------------------------------
    // Overridden Methods
    // ----------------------------------------------------------

    /**
     * Get the type of this entity
     *
     * @return The type property
     */
    public int getType() {
        return Entity.TYPE_VIEWPOINT;
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
}
