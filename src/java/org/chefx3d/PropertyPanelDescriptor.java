/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d;

import org.w3c.dom.Document;

/**
 * A description of a property panel.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public class PropertyPanelDescriptor {
    
    /** The panel name */
    private String name;

    /** The default values */
    private Document defaults;

    /** The url to the schema describing the property space */
    private String schemaURL;

    /**
     * Create a property panel descriptor.
     * 
     * @param name The name to display on the panel
     * @param schemaURL The url to a schema describing the property space
     * @param defaults The default values
     */
    public PropertyPanelDescriptor(
            String name, 
            String schemaURL,
            Document defaults) {
        this.name = name;
        this.defaults = defaults;
        this.schemaURL = schemaURL;
    }

    /**
     * Get the name of the panel.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the schema url
     * 
     * @return The schema
     */
    public String getSchemaURL() {
        return schemaURL;
    }

    /**
     * Get the default values for the properties.
     * 
     * @return The default values as a DOM
     */
    public Document getDefaults() {
        return defaults;
    }
    
}
