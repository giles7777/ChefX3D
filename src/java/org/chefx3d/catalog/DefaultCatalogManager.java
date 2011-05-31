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

package org.chefx3d.catalog;

// External Imports
import java.util.*;

// Local imports
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.tool.ToolGroupChild;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DefaultErrorReporter;

/**
 * Manage catalogues.  This class is the main owner for catalogues and tools.
 *
 * @author Alan Hudson
 * @version $Revision 1.1 $
 */
public class DefaultCatalogManager extends CatalogManager {

    /** The singleton class */
    private static DefaultCatalogManager catalogManager;

    /**
     * Get the singleton CatalogManager.
     *
     * @return The CatalogManager
     */
    public static DefaultCatalogManager getCatalogManager() {
        if (catalogManager == null) {
            catalogManager = new DefaultCatalogManager();
        }

        return catalogManager;
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

    /**
     * Search all of the available catalogs to find the named tool. Assumes
     * that tools are uniquely IDed across all catalogs. If they are not, then
     * the first tool found will be returned. The definition of first is
     * undefined and may return different values each time.
     * <p>
     * <b>Warning:</b> Implementation could be extremely slow depending on
     * the nature of the catalogs created and needing to be searched.
     *
     * @return The tool or null if not found
     */
    public Tool findTool(String toolID) {
        for(int i = 0; i < catalogs.size(); i++) {
            Catalog cat = catalogs.get(i);
            List<ToolGroup> groups = cat.getAllGroupsFlattened();
            for(int j = 0; j < groups.size(); j++) {
                ToolGroup tg = groups.get(j);
                Tool tool = tg.getTool(toolID);

                if(tool != null)
                    return tool;
            }
        }

        return null;
    }
    
    /**
     * Search all of the available catalogs to find the named tool. Assumes
     * that tools are uniquely IDed across all catalogs. If they are not, then
     * the first tool found will be returned. The definition of first is
     * undefined and may return different values each time.
     * <p>
     * <b>Warning:</b> Implementation could be extremely slow depending on
     * the nature of the catalogs created and needing to be searched.
     *
     * @return The tool or null if not found
     */
    public Tool findToolByName(String toolName) {
        for(int i = 0; i < catalogs.size(); i++) {
            Catalog cat = catalogs.get(i);
            List<ToolGroup> groups = cat.getAllGroupsFlattened();
            for(int j = 0; j < groups.size(); j++) {
                ToolGroup tg = groups.get(j);
                Tool tool = tg.getToolByName(toolName);

                if(tool != null)
                    return tool;
            }
        }

        return null;
    }
    
    /**
     * Search all of the available catalogs to find the named tool. Assumes
     * that tools are uniquely named across all catalogs. If they are not, then
     * the first tool found will be returned. The definition of first is
     * undefined and may return different values each time.
     * <p>
     * <b>Warning:</b> Implementation could be extremely slow depending on
     * the nature of the catalogs created and needing to be searched.
     *
     * @return The tool or null if not found
     */
    public ToolGroup findToolGroup(String groupID) {
        for(int i = 0; i < catalogs.size(); i++) {
            Catalog cat = catalogs.get(i);
            List<ToolGroup> groups = cat.getAllGroupsFlattened();
            for(int j = 0; j < groups.size(); j++) {
                ToolGroup tg = groups.get(j);
                if(tg.getGroupID().equals(groupID))
                    return tg;
            }
        }

        return null;
    }

    /**
     * Search all of the available catalogs to find the named tool. Assumes
     * that tools are uniquely named across all catalogs. If they are not, then
     * the first tool found will be returned. The definition of first is
     * undefined and may return different values each time.
     * <p>
     * <b>Warning:</b> Implementation could be extremely slow depending on
     * the nature of the catalogs created and needing to be searched.
     *
     * @return The tool or null if not found
     */
    public ToolGroup findToolGroupByName(String groupName) {
        for(int i = 0; i < catalogs.size(); i++) {
            Catalog cat = catalogs.get(i);
            List<ToolGroup> groups = cat.getAllGroupsFlattened();
            for(int j = 0; j < groups.size(); j++) {
                ToolGroup tg = groups.get(j);
                if(tg.getName().equals(groupName))
                    return tg;
            }
        }

        return null;
    }

}
