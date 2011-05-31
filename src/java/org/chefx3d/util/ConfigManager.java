/*****************************************************************************
 *                        Web3d.org Copyright (c) 2005-2007
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

import java.io.InputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;


public class ConfigManager {

    /** The global manager instance shared by everyone */
    private static ConfigManager instance;

    /** The loaded properties store */
    private static Properties prop;

    private ConfigManager() {
        prop = new Properties();
    }    

    /**
     * Get the global instance of the config manager.  The loadConfig method 
     * must be called to provide data
     */
    public static ConfigManager getManager() {

        if(instance == null)
            instance = new ConfigManager();

        return instance;
    }

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

    /**
     * Load the properties file into the memory store.
     * 
     * @param filename
     */
    public void loadConfig(final String filename) {
        
        FileLoader loader = new FileLoader();
        
        try {
            
            // load the file
            Object[] result = loader.getFileURL(filename, true);            
            InputStream is = (InputStream)result[1];
            
            // place the properties into memory
            if (is != null) {
                prop.load(is); 
            } else {
                throw new IOException();
            }
            
        } catch (IOException ioe) {
            System.out.println("Loading of config file failed: " + filename);
        }
        
    }
    
    /**
     * Searches for the property with the specified key in this property list. 
     * If the key is not found in this property list, the default property 
     * list, and its defaults, recursively, are then checked. The method 
     * returns null if the property is not found. 
     * 
     * @param key the hashtable key.
     * @return the value in this property list with the specified key value.
     */
    public String getProperty(String key) {
        return String.valueOf(prop.get(key));        
    }
    
    /**
     * Searches for the property with the specified key in this property list. 
     * If the key is not found in this property list, the default property 
     * list, and its defaults, recursively, are then checked. The method 
     * returns the default value argument if the property is not found. 
     * 
     * @param key the hashtable key.
     * @param defaultValue a default value. 
     * @return the value in this property list with the specified key value.
     */
    public String getProperty(String key, String defaultValue) {
        Object value = prop.get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return String.valueOf(value);  
        }
    }
    
    /**
     * Simple helper method to print the current contents of the property 
     * object.
     */
    public void printProperties() {
        prop.list(System.out);
    }

}
