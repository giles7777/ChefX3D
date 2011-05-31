package org.chefx3d.model;

import java.util.ArrayList;
import java.util.Map;

public class DefaultEntity extends BasePositionableEntity {

	public DefaultEntity(int entityID,
			Map<String, Map<String, Object>> toolProperties) {
		super(entityID, toolProperties);
		// TODO Auto-generated constructor stub
	}

	public DefaultEntity(int entityID, String positionParamsSheet,
			Map<String, Map<String, Object>> toolProperties) {
		super(entityID, positionParamsSheet, toolProperties);
		// TODO Auto-generated constructor stub
	}

	public DefaultEntity(int entityID, String paramSheetName,
			String positionParamsSheet,
			Map<String, Map<String, Object>> toolProperties) {
		super(entityID, paramSheetName, positionParamsSheet, toolProperties);
		// TODO Auto-generated constructor stub
	}

	private DefaultEntity(int entityID,
	        String propertySheet,
	        String positionParamsSheet,
			Map<String, Object> toolParams,
			Map<String, Map<String, Object>> toolProperties) {
		super(entityID, propertySheet, positionParamsSheet, toolParams, toolProperties);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getType() {
		return TYPE_MODEL;
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
    	return true;
    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

	/**
     * Create a copy of the Entity
     *
     * @param issuer The data model
     */
    public DefaultEntity clone(IdIssuer issuer) {

    	int clonedID = issuer.issueEntityID();

        // Create the new copy
        DefaultEntity clonedEntity =
            new DefaultEntity(clonedID,
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

        return(clonedEntity);
    }

}
