/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.util;

//External Imports
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

//Internal Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.ListProperty;

/**
 * Utility class for exposing data about the world.
 *
 * @author Ben Yarger
 * @version $Revision: 1.4 $
 */
public abstract class ExposeDataUtility {

	/**
	 * Expose all properties assigned to an entity.
	 * 
	 * @param entity Entity to expose
	 */
	public static void exposeEntity(Entity entity) {
		
	    if (entity == null) {
	        return;
	    }
	    
		Map<String, Map<String, Object>> propMap = entity.getPropertiesMap();
		
		System.out.println(
				"Exposing properties assigned to: "+entity.getName()+
				" || ID: "+entity.getEntityID());
		
	    if (propMap == null) {
	        return;
	    }

		Object[] keys = (Object[]) propMap.keySet().toArray();
		
		for (int w = 0; w < keys.length; w++) {
			
			System.out.println((w+1)+") Property Sheet: "+((String)keys[w]));
			
			Map<String, Object> properties = propMap.get(keys[w]);
			Object[] propertyKeys = (Object[]) properties.keySet().toArray();
			String value;
			
			for (int x = 0; x < propertyKeys.length; x++) {

				// Do the conversion to a string that is human readable
				value = convertDataToString(properties.get(propertyKeys[x]));
				
				System.out.println(
						" -- property "+(x+1)+": "+
						((String)propertyKeys[x])+" ("+value+")");
			}
		}
	}
	
	//--------------------------------------------------------------------------
	// Private methods
	//--------------------------------------------------------------------------
	
	/**
	 * Convert the object to a string representation that is human readable. 
	 * If something is missing that you need to expose, it is up to you to fill 
	 * it in and then share it with the rest of us!
	 * 
	 * @param obj Object to convert
	 * @return String representation of obj
	 */
	private static String convertDataToString(Object obj) {
		
		String value = "";
		
		if (obj == null) {
			value = "null data - nothing to expose";
		} else if (obj instanceof int[]) {
			value = Arrays.toString((int[])obj);
		} else if (obj instanceof double[]) {
			value = Arrays.toString((double[])obj);
		} else if (obj instanceof float[]) {
			value = Arrays.toString((float[])obj);
		} else if (obj instanceof boolean[]) {
			value = Arrays.toString((boolean[])obj);
		} else if (obj instanceof Enum[]) {
			Enum[] enumArray = (Enum[]) obj;
			value = "";
			for (int i = 0; i < enumArray.length; i++) {
				value += enumArray[i].toString()+", ";
			}
		} else if (obj instanceof String[]) {
			value = Arrays.toString((String[])obj);
		} else if (obj instanceof String) {
			value = (String) obj;
		} else if (obj instanceof Integer) {
			value = Integer.toString((Integer)obj);
		} else if (obj instanceof Double) {
			value = Double.toString((Double)obj);
		} else if (obj instanceof Float) {
			value = Float.toString((Float)obj);
		} else if (obj instanceof ArrayList<?>) {
			value = ((ArrayList<?>)obj).toString();
		} else if (obj instanceof Boolean) {
			value = Boolean.toString((Boolean)obj);
		} else if (obj instanceof Enum) {
			value = ((Enum)obj).toString();
		} else if (obj instanceof Entity) {
			value = "Entity: "+((Entity)obj).getName()+
				" || ID: "+((Entity)obj).getEntityID();
		} else if (obj instanceof ListProperty) {
			value = "selected value: "+((ListProperty)obj).getSelectedValue()+
				" || selected key: "+((ListProperty)obj).getSelectedKey();
		} else if (obj instanceof Color) {
			value = ((Color)obj).toString();
		} else {
			
			Class type = obj.getClass();
			
			if (type.isArray()) {
				
				Class dataType = type.getComponentType();
				
				value = "unknown array type [type: "+type.toString()+
					" dataType: "+dataType.toString()+"]";
			
			} else {
				value = "unknown object type [type: "+type.toString()+"]";
			}
		}
		
		return value;
	}
}
