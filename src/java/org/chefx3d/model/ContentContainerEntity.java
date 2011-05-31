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
 * @version $Revision: 1.7 $
 */
public class ContentContainerEntity extends BaseEntity 
    implements Cloneable {
    
    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The name of the sheet that contains the properties
     * @param toolProperties The properties of an entity as defined by the tool
     */
    public ContentContainerEntity(
            int entityID,
            String propertySheetName,
            Map<String, Map<String, Object>> toolProperties) {
        
        super(entityID, propertySheetName, toolProperties);
        
    }
    
    /**
     * The constructor. The entity properties will be copied from the tool
     * defaults.
     *
     * @param entityID The entity ID
     * @param propertySheetName The name of the sheet that contains the properties
     * @param toolProperties The properties of an entity as defined by the tool
     * @param toolParams The properties of the entity
     */
    public ContentContainerEntity(
            int entityID,
            Map<String, Map<String, Object>> defaultProperties) {
        
        this(entityID, Entity.DEFAULT_ENTITY_PROPERTIES, defaultProperties);
        
    }
    
    /**
     * Get the type of this entity
     *
     * @return The type property
     */
    public int getType() {
    	return TYPE_CONTENT_ROOT;
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
    public ContentContainerEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();

        ContentContainerEntity clonedEntity =
            new ContentContainerEntity(clonedID,
            				  propertySheetName,
                              properties);

        // copy all the other data over
        //clonedEntity.children = (ArrayList<Entity>)children.clone();

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
 
}