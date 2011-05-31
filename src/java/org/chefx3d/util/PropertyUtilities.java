/*****************************************************************************
 *                        Web3d.org Copyright (c) 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.util;

// External Imports
import java.awt.Color;
import java.util.*;
import org.w3c.dom.*;

// Local imports
// None

/**
 * Clone the property values of the hashmap
 * 
 * @author Russell Dodds
 * @version $Revision: 1.16 $
 */
public class PropertyUtilities {
    
	public static Map<String, Map<String, Object>> cloneAll(
	        Map<String, Map<String, Object>> inMap) {
	    
		Map<String, Map<String, Object>> clone = 
		    new HashMap<String, Map<String, Object>>();
		
		Iterator<Map.Entry<String, Map<String, Object>>> index = inMap.entrySet().iterator();
		while (index.hasNext()) {
			Map.Entry<String, Map<String, Object>> entry = index.next();
			String name = entry.getKey();
			clone.put(name, clone(entry.getValue()));
		}
		return clone;
	}
	
    /**
     * Go looking for the named system property.
     * 
     * @param inMap - The map to clone
     * @return - The copied map
     */
    public static Map<String, Object> clone(Map<String, Object> inMap) {
        
        Map<String, Object> clone = new LinkedHashMap<String, Object>();

        if (inMap == null)
            return null;
        
        Iterator<Map.Entry<String, Object>> index = 
            inMap.entrySet().iterator();
        
//System.out.println("PropertyUtilities.clone()");
       
        while (index.hasNext()) {

            Map.Entry<String, Object> mapEntry = index.next();

            // get the key, value pairing
            String name = mapEntry.getKey();
            Object value = mapEntry.getValue();
            
//System.out.println("    name: " + name);
//System.out.println("    value: " + value);
       
            if (value == null) {
                
                // TODO: what do we want to do with properties that are null?
                
            } else if (value instanceof Integer || 
                    value instanceof Float ||
                    value instanceof Double || 
                    value instanceof Boolean || 
                    value instanceof Character || 
                    value instanceof Long || 
                    value instanceof Short) {
                
                clone.put(name, value);
                
            } else if (value.getClass().isEnum()) {

                clone.put(name, value);
                
            } else if (value instanceof Enum[]){
            	
            	Enum[] orig = (Enum[])value;
            	Enum[] copy = new Enum[orig.length];
            	for(int i = 0; i < orig.length; i++) {
            		copy[i] = orig[i];
            	}
            	clone.put(name, copy);
            	
            } else if (value instanceof Color) {

                Color inColor = (Color)value;               
                Color newColor = 
                    new Color(inColor.getRed(), inColor.getGreen(), inColor.getBlue());
                
                clone.put(name, newColor);

            } else if (value instanceof float[]) { 

                float[] orig = (float[])value;
                float[] copy = new float[orig.length];
                for (int i = 0; i < orig.length; i++) {
                    copy[i] = orig[i];
                }
                clone.put(name, copy);

            } else if (value instanceof double[]) {

                double[] orig = (double[])value;
                double[] copy = new double[orig.length];
                for (int i = 0; i < orig.length; i++) {
                    copy[i] = orig[i];
                }
                clone.put(name, copy);  
                
            } else if (value instanceof int[]) {
                
                int[] orig = (int[])value;
                int[] copy = new int[orig.length];
                for (int i = 0; i < orig.length; i++) {
                    copy[i] = orig[i];
                }
                clone.put(name, copy); 
                
			} else if (value instanceof boolean[]) {
                
                boolean[] orig = (boolean[])value;
                boolean[] copy = new boolean[orig.length];
                for (int i = 0; i < orig.length; i++) {
                    copy[i] = orig[i];
                }
                clone.put(name, copy); 
                
            } else if (value instanceof String[]) {
                                
                String[] orig = (String[])value;
                String[] copy = new String[orig.length];
                for (int i = 0; i < orig.length; i++) {
                    copy[i] = orig[i];
                }
                clone.put(name, copy);  
                                
            } else if (value instanceof String) {
                              
                clone.put(name, new String(value.toString()));  
                
            } else if (value instanceof CloneableProperty) {
                
                clone.put(name, ((CloneableProperty)value).clone());  

            } else if (value instanceof Document) {
                
                clone.put(name, ((Document)value).cloneNode(true));  

            } else if (value instanceof Element) {
                
                clone.put(name, ((Element)value).cloneNode(true)); 
                
            } else if (value instanceof ArrayList) {
                
                clone.put(name, ((ArrayList)value).clone()); 
                
            } else if (value instanceof HashMap) {
                
                clone.put(name, ((HashMap)value).clone()); 

            } else  {
                
                clone.put(name, new String(value.toString()));  

            }
            
        }

        return clone;
        
    }
    
}
