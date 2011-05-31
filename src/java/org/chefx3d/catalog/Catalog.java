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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.tool.ToolGroupChild;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A grouping of tools.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>toolGroupAddMsg: Message when an exception is generated in the
 *     toolGroupAdded() callback</li>
 * <li>toolGroupRemoveMsg: Message when an exception is generated in the
 *     toolGroupRemoved() callback</li>
 * <li>toolGroupsAddMsg: Message when an exception is generated in the
 *     toolGroupsAdded() callback</li>
 * <li>toolGroupsRemoveMsg: Message when an exception is generated in the
 *     toolGroupsRemoved() callback</li>
 * </ul>
 *
 * @author Alan Hudson
 * @version $Revision 1.1 $
 */
public class Catalog {
    /** Error message when the user code barfs */
    private static final String GROUPS_ADD_ERR_PROP =
        "org.chefx3d.catalog.Catalog.toolGroupsAddMsg";

    /** Error message when the user code barfs */
    private static final String GROUPS_REMOVE_ERR_PROP =
        "org.chefx3d.catalog.Catalog.toolGroupsRemoveMsg";

    /** Error message when the user code barfs */
    private static final String GROUP_ADD_ERR_PROP =
        "org.chefx3d.catalog.Catalog.toolGroupAddMsg";

    /** Error message when the user code barfs */
    private static final String GROUP_REMOVE_ERR_PROP =
        "org.chefx3d.catalog.Catalog.toolGroupRemoveMsg";

    /** The list of all tools */
    private ArrayList<ToolGroup> tools;

    /** The tool groups mapped by name */
    private HashMap<String, ToolGroup> toolGroupsByID;

    /** The unique catalog name */
    private final String name;

    /** The major version */
    private int majorVersion;

    /** The minor version */
    private int minorVersion;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The listener(s) to this catalog */
    private CatalogListener listener;

    /**
     * Construct an instance of the catalog with the given name and
     * version information.
     *
     * @param name - The name of the catalog
     * @param majorVersion - The catalog major version number
     * @param minorVersion - The catalog minor version number
     */
    public Catalog(String name, int majorVersion, int minorVersion) {
        this.name = name;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;

        tools = new ArrayList<ToolGroup>();
        toolGroupsByID = new HashMap<String, ToolGroup>();

        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the name of the catalog.
     *
     * @return The catalog name
     */
    public String getName() {
        return name;
    }

    /**
     * Add a tool to the named group.
     *
     * @param groupName The group to add to
     * @param tool The new tool
     */
    public void addTool(String groupID, ToolGroupChild tool) {

        ToolGroup group = toolGroupsByID.get(groupID);

        // Adding it to the group will fire a group listener event so no
        // need to do anything locally.
        if (group != null) {
            group.addTool(tool);
        }
    }

    /**
     * Remove a tool.
     *
     * @param groupName The group to remove from
     * @param tool The tool
     */
    public void removeTool(String groupID, ToolGroupChild tool) {

        ToolGroup group = toolGroupsByID.get(groupID);

        // Adding it to the group will fire a group listener event so no
        // need to do anything locally.
        if (group != null) {
            group.removeTool(tool);
        }
    }

    /**
     * Add a toolGroup.
     *
     * @param group The new ToolGroup
     */
    public void addToolGroup(ToolGroup group) {

        ToolGroup check = toolGroupsByID.get(group.getGroupID());

        // if the group exists merge them
        if (check != null) {
            // Ignore the merge if the two tool groups are the same
            // instance.
            if(check == group)
                return;

            for (int i = 0; i < group.getChildren().size(); i++) {

                if (group.getChildren().get(i) instanceof ToolGroupChild) {
                    check.addTool((ToolGroupChild) group.getChildren().get(i));
                } else {
                    check.addToolGroup((ToolGroup) group.getChildren().get(i));
                }
            }
        } else {
            tools.add(group);
            toolGroupsByID.put(group.getGroupID(), group);
            fireToolGroupAdded(name, group);
        }
    }

    /**
     * Remove a toolGroup.
     *
     * @param group The ToolGroup to remove
     */
    public void removeToolGroup(ToolGroup group) {

        String groupID = group.getGroupID();

        if(!toolGroupsByID.containsKey(groupID)) {
            String msg = "Catalog " + name +
                         " does not contain ToolGroup " +
                         groupID + " to remove it";
            errorReporter.errorReport(msg, null);
        } else {
            tools.remove(group);
            toolGroupsByID.remove(groupID);

            fireToolGroupRemoved(name, group);
        }
    }

    /**
     * Add a list of tools.
     *
     * @param newTools A list of Tools
     */
    public void addTools(List<ToolGroup> newTools) {

        for(int i = 0; i < newTools.size(); i++) {
            ToolGroup group = newTools.get(i);
            addToolGroup(group);
        }
    }

    /**
     * Remove a list of tools.
     *
     * @param oldTools A list of Tools
     */
    public void removeTools(List<ToolGroup> oldTools) {

        ArrayList<ToolGroup> removed = new ArrayList<ToolGroup>();

        for(int i = 0; i < oldTools.size(); i++) {
            ToolGroup grp = oldTools.get(i);
            String groupID = grp.getGroupID();

            if(!toolGroupsByID.containsKey(groupID)) {
                String msg = "Catalog " + name +
                             " does not contain ToolGroup " +
                             groupID + " to remove it";
                errorReporter.errorReport(msg, null);
            } else {
                toolGroupsByID.remove(groupID);
                removed.add(grp);
            }
        }

        tools.removeAll(oldTools);
        fireToolsRemoved(name, oldTools);
    }

    /**
     * Get the toolGroup in this catalog.
     *
     * @param groupID - The groupID to lookup
     * @return The toolGroup
     */
    public ToolGroup getToolGroup(String groupID) {
        return toolGroupsByID.get(groupID);
    }

    /**
     * Get the list of top-level tool groups in this catalog. The list
     * returned is a new unmodifiable list of the items in this catalog.
     *
     * @return A new list instance containing the top-level groups.
     */
    public List<ToolGroup> getToolGroups() {
        return Collections.unmodifiableList(tools);
    }

    /**
     * Return all the ToolGroups held by the catalog.
     *
     * @return A new list instance containing the flattened groups.
     */
    public List<ToolGroup> getAllGroupsFlattened() {

        List<ToolGroup> ret_val = new ArrayList<ToolGroup>();

        getGroups(tools, ret_val);

        return ret_val;
    }
    
    /**
     * Get a list of all the tools being managed.  This will
     * be a flat list with no ToolGroups.
     *
     * @return The current list of tools
     */
    public List<Tool> getAllToolsFlattened() {
        ArrayList<Tool> toolList = new ArrayList<Tool>();

        List<ToolGroup> groups = getAllGroupsFlattened();

        for(int j = 0; j < groups.size(); j++) {
            ToolGroup tg = groups.get(j);

            List<ToolGroupChild> kids = tg.getTools();
            toolList.addAll(kids);
        }

        return toolList;
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
    public Tool findTool(String toolID) {
        List<ToolGroup> groups = getAllGroupsFlattened();
        for(int j = 0; j < groups.size(); j++) {
            ToolGroup tg = groups.get(j);
            Tool tool = tg.getTool(toolID);

            if(tool != null)
                return tool;
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
        List<ToolGroup> groups = getAllGroupsFlattened();
        for(int j = 0; j < groups.size(); j++) {
            ToolGroup tg = groups.get(j);
            Tool tool = tg.getToolByName(toolName);

            if(tool != null)
                return tool;
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
        List<ToolGroup> groups = getAllGroupsFlattened();
        for(int j = 0; j < groups.size(); j++) {
            ToolGroup tg = groups.get(j);
            if(tg.getGroupID().equals(groupID))
                return tg;
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
        List<ToolGroup> groups = getAllGroupsFlattened();
        for(int j = 0; j < groups.size(); j++) {
            ToolGroup tg = groups.get(j);
            if(tg.getName().equals(groupName))
                return tg;
        }

        return null;
    } 
    
    /**
     * Add a CatalogListener. Duplicates are ignored.
     *
     * @param l The listener
     */
    public void addCatalogListener(CatalogListener l) {
        listener = CatalogListenerMulticaster.add(listener, l);
    }

    /**
     * Remove a CatalogListener. If it is not currently registered, the
     * request is silently ignored.
     *
     * @param l The listener
     */
    public void removeCatalogListener(CatalogListener l) {
        listener = CatalogListenerMulticaster.remove(listener, l);
    }

    /**
     * Traverse a list of ToolGroups and Tools to generate
     * a flat list of ToolGroups
     *
     * @param source - The source list
     * @param dest - The destination list
     */
    private void getGroups(List<ToolGroup> source, List<ToolGroup> dest) {
        int len = source.size();

        for(int i=0; i < len; i++) {
            Object o = source.get(i);

            if (o instanceof ToolGroup) {
                ToolGroup group = (ToolGroup)o;
                dest.add(group);

                List<ToolGroup> children = group.getToolGroups();
                getGroups(children, dest);
            }
        }
    }

    /**
     * Notify listeners of tools added.
     *
     * @param name - The catalog added to
     * @param groups - The groups added
     */
    private void fireToolsAdded(String name, List<ToolGroup> groups) {
        try {
            if(listener != null)
                listener.toolGroupsAdded(name, groups);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUPS_ADD_ERR_PROP) +
                         listener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Notify listeners of tools removed.
     *
     * @param name - The catalog changed
     * @param groups - The tools removed
     */
    private void fireToolsRemoved(String name, List<ToolGroup> groups) {
        try {
            if(listener != null)
                listener.toolGroupsRemoved(name, groups);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUPS_REMOVE_ERR_PROP) +
                         listener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Notify listeners of groups added.
     *
     * @param name - The catalog name
     * @param group - The toolGroup removed from
     */
    private void fireToolGroupAdded(String name, ToolGroup group) {
        try {
            if(listener != null)
                listener.toolGroupAdded(name, group);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_ADD_ERR_PROP) +
                         listener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Notify listeners of groups removed.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    private void fireToolGroupRemoved(String name, ToolGroup group) {
        try {
            if(listener != null)
                listener.toolGroupRemoved(name, group);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_REMOVE_ERR_PROP) +
                         listener;

            errorReporter.errorReport(msg, e);
        }
    }
    
    /**
     * A helper function to print the current contents of the catalog
     */
    public void printCatalog() {
        
        System.out.println("");
        System.out.println("------------- CATALOG -------------");
        
        int tab = 0;
        int len = tools.size();
        for (int i = 0; i < len; i++) {
            ToolGroup group = tools.get(i);
            traverseGroup(group, tab);
        }
        System.out.println("-----------------------------------");
        System.out.println("");
        
    }
    
    /**
     * Prints the  current ToolGroups Tools and then
     * traverses the children ToolGroups.
     * 
     * @param parent The ToolGroup to inspect
     */
    private void traverseGroup(ToolGroup parent, int tab) {
        
        
        // print group
        printTab(tab);
        System.out.println(parent.getName() + " [ToolGroup]");
        
        // print tools
        List<ToolGroupChild> tools = parent.getTools();
        int len = tools.size();
        for (int i = 0; i < len; i++) {
            ToolGroupChild tool = tools.get(i);
            printTab(tab + 1);
            System.out.println(tool.getName() + " [Tool]");
        }
  
        // print group
        List<ToolGroup> groups = parent.getToolGroups();
        len = groups.size();
        for (int i = 0; i < len; i++) {
            ToolGroup group = groups.get(i);
            traverseGroup(group, tab + 1);
        }
       
    }
    
    /**
     * Print a tab
     * 
     * @param tab How many tabs to print
     */
    private void printTab(int tab) {
        for (int i = 0; i < tab; i++) {
            System.out.print("    ");
        }
    }
}
