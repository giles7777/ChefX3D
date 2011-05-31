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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// internal imports
// none

/**
 * A grouping of viewpoint entities
 * 
 * @author Christopher Shankland
 * @version $Revision: 1.16 $
 */
public class ViewpointContainerEntity extends BaseEntity {
    
    /**
     * The base constructor.  
     * 
     * @param entityID The unique ID
     */
	public ViewpointContainerEntity(int entityID, Map<String, Map<String, Object>> properties) {
		this(entityID, Entity.DEFAULT_ENTITY_PROPERTIES, properties);
	}
	
	/**
     * The base constructor.  
     * 
     * @param entityID The unique ID
     */
	public ViewpointContainerEntity(int entityID, String propertySheetName, Map<String, Map<String, Object>> properties) {
		super(entityID, propertySheetName, properties);
	}
	
    /**
     * The load constructor.  Will populate the viewpoint list with 
     * the provided list
     * 
     * @param entityID The unique ID
	 * @param viewpoints An initial list of viewpoints
     */
    public ViewpointContainerEntity(
            int entityID,
            Map<String, Map<String, Object>> properties,
            List<ViewpointEntity> viewpoints) {
       
        super(entityID, properties);
        
        // add all the view points
        int len = viewpoints.size();
        for (int i = 0; i < len; i++) {
            ViewpointEntity viewpoint = viewpoints.get(i);
            addChild(viewpoint);
        }
    }
    
    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param roomEntityID The room entity ID
     * @param viewpointListID The viewpoint container list ID
     * @param toolProperties The properties of an entity as defined by the tool
     */
    private ViewpointContainerEntity(
            int entityID, 
            String propertySheetName,
            Map<String, Object> toolParams,
            Map<String, Map<String, Object>> toolProperties) {
        
        super(entityID, propertySheetName, toolParams, toolProperties);
    }

    /**
     * Get the type of this entity
     *
     * @return The type property
     */
    public int getType() {
    	return TYPE_CONTAINER;
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
	    
    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public ViewpointContainerEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();
    	
        // Create the new copy
        ViewpointContainerEntity clonedEntity =
            new ViewpointContainerEntity(clonedID,
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
    // Local Methods
    // ----------------------------------------------------------

	/**
	 * Helper method to add new viewpoints
	 * 
	 * @param viewpoint The viewpoint to add
	 */
	public void addViewpoint(ViewpointEntity viewpoint) {
	    addChild(viewpoint);
	}
	
	/**
	 * Helper method to get the current list of viewpoints
	 * 
	 * @return The list of viewpoints
	 */
	public List<ViewpointEntity> getViewpoints() {
	    
	    List<ViewpointEntity> viewpoints = new ArrayList<ViewpointEntity>();
	    int len = children.size();
	    for (int i = 0; i < len; i++) {
			Entity entity = children.get(i);
            if (entity instanceof ViewpointEntity) {              
                viewpoints.add((ViewpointEntity)entity);
            }
	    }
	    return viewpoints;
	}
	
    /**
     * Check the children of the scene for a viewpoint with the
     * name provided, return it if found, return null otherwise
     * 
     * @param entityID The ID of the viewpoint
     */
    public ViewpointEntity getViewpoint(int entityID) {
        
		ViewpointEntity ve = null;
		
        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity entity = children.get(i);
            if (entity instanceof ViewpointEntity) {              
                if (entity.getEntityID() == entityID) {
                    ve = (ViewpointEntity)entity;
					break;
                }
            }
        }
        return(ve);
    }
}
