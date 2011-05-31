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

package org.chefx3d.tool;

// External Imports
// None

// Local imports
// None

/**
 * Marker interface to declare class types that are valid Tools
 *
 * @author Russell Dodds
 * @version $Revision: 1.59 $
 */
public interface Tool {
        
    /**
     * Get the unique ID of the tool
     * 
     * @return The ID
     */
    public String getToolID();

    /**
     * Get the tool's name.
     *
     * @return The name string
     */
    public String getName();

    /**
     * Get the tools type.  
     * 
     * @return The type
     */
    public int getToolType();
        
    /**
     * Get the string describing the tool.
     *
     * @return The description
     */
    public String getDescription();

    /**
     * Get the URL's to use for this tool.
     *
     * @return The list of urls
     */
    public String getURL();

    /**
     * Get the top down icon for this tool
     *
     * @return The icon
     */
    public String getIcon();
   
    /**
     * Get the tool's category. Ideally all tools in the system shall be unique.
     *
     * @return The category string
     */
    public String getCategory();

    /**
     * Get the tool's size. 
     *
     * @return The size array
     */
    public float[] getSize();

    /**
     * Get a specific property.
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @return propValue
     */
    public Object getProperty(
            String propSheet, 
            String propName);
  
    /**
     * Get a specific property.
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @param propValue The value to store
     */
    public void setProperty(
            String propSheet, 
            String propName, 
            Object propValue);

}
