package org.chefx3d.model;

import java.util.Map;

public interface AbstractSegmentTool {

    /**
     * Get all the properties of the tool
     * 
     * @return A map of properties (sheet -> map [name -> value])
     */
    public Map<String, Map<String, Object>> getProperties();

}