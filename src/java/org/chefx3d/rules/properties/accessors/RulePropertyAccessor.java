/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.properties.accessors;

//External Imports

//Internal Imports
import org.chefx3d.model.Entity;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.tool.Tool;

/**
 * Utility class for accessing rule properties for entities.
 *
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */
public class RulePropertyAccessor {

	private static RulePropertyAccessor rulePropertyAccessor =
		new RulePropertyAccessor();

	private RulePropertyAccessor(){

	}

    /**
     * Return the value of the specific rule property. If the property is null
     * a default value will be assigned.
     *
     * @param entity Entity to look up rule property for
     * @param property The property name to retrieve
     * @return Object found
     */
    public static Object getRulePropertyValue(Entity entity, String property) {

        Object value = null;
        if (entity == null) {
            value = ChefX3DRuleProperties.getDefaultValue(property);
            return value;
        }

        value =
    		entity.getProperty(
    				Entity.DEFAULT_ENTITY_PROPERTIES,
    				property);

    	if (value == null) {
    		value = ChefX3DRuleProperties.getDefaultValue(property);
    	}

    	return value;
    }

    /**
     * Return the value of the specific rule property. If the property is null
     * a default value will be assigned.
     *
     * @param tool Tool to look up rule property for
     * @param property The property name to retrieve
     * @return Object found
     */
    public static Object getRulePropertyValue(Tool tool, String property) {

    	Object value =
    		tool.getProperty(
    				Entity.DEFAULT_ENTITY_PROPERTIES,
    				property);

    	if (value == null) {

    		value = ChefX3DRuleProperties.getDefaultValue(property);
    	}

    	return value;
    }

    /**
     * Given a tool or an entity, get a specific property.
     * If both the tool and the entity are null, then
     * return null.
     *
     * @author Eric Fickenscher
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @return propValue
     */
    public static Object getProperty(Tool tool,
    								 Entity entity,
    								 String propSheet,
            						 String propName){

    	if( tool != null)
    		return tool.getProperty(propSheet, propName);
    	else if( entity != null )
    		return entity.getProperty(propSheet, propName);
    	else
    		return null;
    }
    
    /**
     * Set the property of the entity at the propSheet and propName with
     * the value.
     * 
     * @param entity Entity to change property for
     * @param propSheet Property sheet to set
     * @param propName Property name in property sheet to set
     * @param propValue Property value to set
     */
    public static void setProperty(
    		Entity entity, 
    		String propSheet, 
    		String propName, 
    		Object propValue) {
    	
    	entity.setProperty(propSheet, propName, propValue, false);
    }
    
    /**
     * Set the specific rule property value of the entity.
     * 
     * @param entity Entity to change rule property for
     * @param propName Rule property name to set
     * @param propValue Property value to set
     */
    public static void setRuleProperty(
    		Entity entity,
    		String propName,
    		Object propValue) {
    	
    	entity.setProperty(
    			Entity.DEFAULT_ENTITY_PROPERTIES, 
    			propName, 
    			propValue, 
    			false);
    }
    
}
