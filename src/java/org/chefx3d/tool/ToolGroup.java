/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2010
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Describes a group of tools and tool groups.
 * <p>
 *
 * A tool group contains other groups and tools as needed by the catalog
 * structure. In addition, it may contain a tool that is used to directly
 * render or create an entity representation of this group.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>toolAddMsg: Message when an exception is generated in the
 *     toolGroupAdded() callback</li>
 * <li>toolRemoveMsg: Message when an exception is generated in the
 *     toolRemoved() callback</li>
 * <li>toolGroupAddMsg: Message when an exception is generated in the
 *     toolGroupAdded() callback</li>
 * <li>toolGroupRemoveMsg: Message when an exception is generated in the
 *     toolGroupRemoved() callback</li>
 * </ul>
 *
 *
 * @author Alan Hudson
 * @version $Revision: 1.28 $
 */
public class ToolGroup
    implements
        Tool,
        ToolGroupChild,
        Comparable<ToolGroup> {

    /** Error message when the user code barfs */
    protected static final String TOOL_ADD_ERR_PROP =
        "org.chefx3d.tool.ToolGroup.toolAddMsg";

    /** Error message when the user code barfs */
    protected static final String TOOL_REMOVE_ERR_PROP =
        "org.chefx3d.tool.ToolGroup.toolRemoveMsg";

    /** Error message when the user code barfs */
    protected static final String GROUP_ADD_ERR_PROP =
        "org.chefx3d.tool.ToolGroup.toolGroupAddMsg";

    /** Error message when the user code barfs */
    protected static final String GROUP_REMOVE_ERR_PROP =
        "org.chefx3d.tool.ToolGroup.toolGroupRemoveMsg";

    /** Error message when the user code barfs */
    protected static final String TOOL_UPDATE_ERR_PROP =
        "org.chefx3d.tool.ToolGroup.toolUpdateMsg";

    /** Error message when the user code barfs */
    protected static final String GROUP_UPDATE_ERR_PROP =
        "org.chefx3d.tool.ToolGroup.toolGroupUpdateMsg";

    /** Reporter instance for handing out errors */
    protected ErrorReporter errorReporter;

    /** The name of this ToolGroup */
    protected String name;

    /** The type of this ToolGroup */
    protected int type;

    /** The ID of this ToolGroup */
    protected String groupID;

    /** The image of this ToolGroup */
    protected String groupImage;
    
    /** @see VDSTv1.5_TechDesign_20100715.odt, 
     * Category Long Descriptions (1.5.1.11) */
    protected String description; 

    /** ToolGroup can have a tool it represents */
    protected SimpleTool tool;

    /** The list of tools and group children in the group */
    protected List<ToolGroupChild> children;

    /** A list of all the group children of this group */
    protected List<ToolGroup> groupChildren;

    /** A list of all the tool children of this group */
    protected List<ToolGroupChild> toolChildren;

    /** The groupListener(s) for group changes at this level */
    protected ToolGroupListener groupListener;

    /** Tools mapped by their name */
    protected HashMap<String, ToolGroupChild> toolsByIDMap;

    /** Tool groups mapped by their name */
    protected HashMap<String, ToolGroup> groupsByIDMap;

    /** The parent of this tool */
    protected ToolGroupChild toolParent;

    /**
     * Create a new tool descriptor.
     *
     * @param name The groups name, or short description
     * @param type The groups type
     * @param groupID The groups ID
     * @param imagePath String to use for webservice calls to look up the 
     * associated icon
     */
    public ToolGroup(String name, int type, String groupID, String imagePath) {
        this.name = name;
        this.type = type;
        this.groupID = groupID;
        this.groupImage = imagePath;
        description = name;  // use the setter to explicitly set description
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        children = new ArrayList<ToolGroupChild>();
        toolsByIDMap = new HashMap<String, ToolGroupChild>();
        groupsByIDMap = new HashMap<String, ToolGroup>();
        groupChildren = new ArrayList<ToolGroup>();
        toolChildren = new ArrayList<ToolGroupChild>();
     }

    /**
     * Create a new tool descriptor.  The name is used for the ID.
     *
     * @param name The groups name
     */
    public ToolGroup(String name) {
        this(name, -1, name, "");
    }

    /**
     * Create a new tool descriptor that contains the list of tools and
     * tool groups as children.
     *
     * @param name The groups name
     * @param type The groups type
     * @param children The list of children. This can be a Tool or a ToolGroup.
     */
    public ToolGroup(String name, int type, String groupID, List<ToolGroupChild> children) {
        this(name, type, groupID, "");

        deepCopy(children);
    }

    /**
     * Create a new tool descriptor that contains the list of tools and
     * tool groups as children.
     *
     * @param name The groups name
     * @param children The list of children. This can be a Tool or a ToolGroup.
     */
    public ToolGroup(String name, List<ToolGroupChild> children) {
        this(name);

        deepCopy(children);
    }

    //----------------------------------------------------------
    // Methods defined by Comparable<Tool>
    //----------------------------------------------------------

    /**
     * Return compare based on string ordering
     */
    public int compareTo(ToolGroup t) {

        ToolGroup check = (ToolGroup)t;
        return groupID.compareTo(check.groupID);

    }

    //----------------------------------------------------------
    // Methods defined by Object
    //----------------------------------------------------------

    /**
     * override of Objects equals test
     */
    public boolean equals(Object o) {

        if (o instanceof ToolGroup) {
            ToolGroup check = (ToolGroup)o;
            if (groupID.equals(check.groupID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     * Since equals uses only groupID the hashCode of this is groupID's.
     */
    public int hashCode() {
        return groupID.hashCode();
    }

    //----------------------------------------------------------
    // Methods defined by ToolGroupChild
    //----------------------------------------------------------

    /**
     * Return the parent of this tool group child. If there is no parent
     * reference the parent is either the catalog or this is an orphaned item.
     *
     * @return The current parent of this item
     */
    public ToolGroupChild getParent() {
        return toolParent;
    }

    /**
     * Set the tool parent to be this new object. Null clears the reference.
     * Package private because only ToolGroup should be calling this.
     */
    public void setParent(ToolGroupChild parent) {
        toolParent = parent;
    }

    //----------------------------------------------------------
    // Methods defined by Tool
    //----------------------------------------------------------
    /**
     * Get the tool's name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the tool type. Defined in this class as TYPE_*
     *
     * @return The type
     */
    public int getToolType() {
        return type;
    }

    /**
     * Get the unique ID of the tool
     *
     * @return The ID
     */
    public String getToolID() {
        return groupID;
    }

    /**
     * Get the string describing the tool.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the string describing the tool.
     *
     * @return The long description
     */
    public void setDescription(String newDescription) {
        description = newDescription;
    }

    /**
     * Get the URL's to use for this tool.
     *
     * @return The list of urls
     */
    public String getURL() {
        return groupImage;
    }

    /**
     * Get the top down icon for this tool
     *
     * @return The icon
     */
    public String getIcon() {
        return groupImage;
    }

    /**
     * Get the tool's category. Not implemented for ToolGroups,
     * returns null
     *
     * @return The category string
     */
    public String getCategory() {
        return null;
    }

    /**
     * Get the groups size. Not implemented for ToolGroups,
     * returns null
     *
     * @return The size array
     */
    public float[] getSize() {
        return null;
    }


    /**
     * Get a specific property.  Not implemented for ToolGroups,
     * returns null
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @return propValue
     */
    public Object getProperty(
            String propSheet,
            String propName) {
        return null;
    }

    /**
     * Get a specific property.  Not implemented for ToolGroups,
     * sets null
     *
     * @param propSheet The sheet name
     * @param propName The name of the property to set
     * @return propValue
     */
    public void setProperty(
            String propSheet,
            String propName,
            Object propValue) {
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Register an error reporter with the engine so that any errors generated
     * by the loading of script code can be reported in a nice, pretty fashion.
     * Setting a value of null will clear the currently set reporter. If one
     * is already set, the new value replaces the old.
     *
     * @param reporter The instance to use or null
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        // Reset the default only if we are not shutting down the system.
        if(reporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Search in this group's direct children for a tool of the given
     * ID.
     *
     * @param name The ID of the tool to look for
     * @return The matching tool instance or null if not found
     */
    public ToolGroupChild getTool(String toolID) {
        return toolsByIDMap.get(toolID);
    }

    /**
     * Search in this group's direct children for a tool of the given
     * ID.
     *
     * @param name The ID of the tool to look for
     * @return The matching tool instance or null if not found
     */
    public ToolGroupChild getToolByName(String toolName) {
        int len = toolChildren.size();
        for (int i = 0; i < len; i++) {
            ToolGroupChild tool = toolChildren.get(i);
            if (tool.getName().equals(toolName)) {
                return tool;
            }
        }
        return null;
    }

    /**
     * Search in this group's direct children for a tool group of the given
     * ID.
     *
     * @param name The ID of the group to look for
     * @return The matching group instance or null if not found
     */
    public ToolGroup getToolGroup(String toolID) {
        return groupsByIDMap.get(toolID);
    }

    /**
     * Set the tool this group represents
     *
     * @param tool
     */
    public void setTool(SimpleTool tool) {
        if(this.tool != null) {
            this.tool.setParent(null);
        }

        this.tool = tool;

        if(this.tool != null) {
            this.tool.setParent(this);
        }
    }

    /**
     * Get the tool this group represents
     *
     * @return tool
     */
    public SimpleTool getTool() {
        return tool;
    }

    /**
     * Add a tool to the group.  Will replace current tool
     * if it is found in the group.
     *
     * @param tool the tool instance to add
     */
    public void addTool(ToolGroupChild tool) {

        String toolID = tool.getToolID();

        tool.setParent(this);

        if(toolsByIDMap.containsKey(toolID)) {

            // replace
            int index = children.indexOf(tool);
            children.set(index, tool);

            index = toolChildren.indexOf(tool);
            toolChildren.set(index, tool);

            fireToolUpdated(tool);

        } else {

            // append
            children.add(tool);
            toolChildren.add(tool);
            fireToolAdded(tool);

        }

        toolsByIDMap.put(toolID, tool);

    }

    /**
     * Add a group to the group.  Will replace current group
     * if it is found in the group
     *
     * @param group The group instance
     */
    public void addToolGroup(ToolGroup group) {

        String groupID = group.getToolID();

        group.setParent(this);

        if(groupsByIDMap.containsKey(groupID)) {

            // replace
            int index = children.indexOf(group);
            children.set(index, group);

            index = groupChildren.indexOf(group);
            groupChildren.set(index, group);

            fireToolGroupUpdated(group);

        } else {

            // append
            children.add(group);
            groupChildren.add(group);
            fireToolGroupAdded(group);

        }

        groupsByIDMap.put(groupID, group);
        group.setErrorReporter(errorReporter);

    }

    /**
     * Remove a tool from the group
     *
     * @param tool
     */
    public void removeTool(ToolGroupChild tool) {
        String toolID = tool.getToolID();

        if(toolsByIDMap.containsKey(toolID)) {
            children.remove(tool);
            toolChildren.remove(tool);
            toolsByIDMap.remove(toolID);
            fireToolRemoved(tool);
            tool.setParent(null);
        }
    }

    /**
     * Remove a toolgroup from the group. Does nothing if the group is not
     * currently a child of this group.
     *
     * @param group The group instance to remove
     */
    public void removeToolGroup(ToolGroup group) {
        String groupID = group.getToolID();

        if(groupsByIDMap.containsKey(groupID)) {
            children.remove(group);
            groupChildren.remove(group);
            groupsByIDMap.remove(groupID);
            group.setParent(null);
            group.setErrorReporter(null);

            fireToolGroupRemoved(group);
        }
    }

    /**
     * Get all the children of this group.
     *
     * @return The list of children
     */
    public List<ToolGroupChild> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Get all the tool children of this group.
     *
     * @return The list of children
     */
    public List<ToolGroupChild> getTools() {
        return Collections.unmodifiableList(toolChildren);
    }

    /**
     * Get all the group children of this group.
     *
     * @return The list of children
     */
    public List<ToolGroup> getToolGroups() {
        return Collections.unmodifiableList(groupChildren);
    }

    /**
     * Add a new groupListener for this group. Duplicate requests are
     * ignored.
     *
     * @param l The groupListener instance to add
     */
    public void addToolGroupListener(ToolGroupListener l) {
        groupListener = ToolGroupListenerMulticaster.add(groupListener, l);
    }

    /**
     * Delete a groupListener for this group. If not added currently, nothing
     * happens.
     *
     * @param l The groupListener instance to remove
     */
    public void removeToolGroupListener(ToolGroupListener l) {
        groupListener = ToolGroupListenerMulticaster.remove(groupListener, l);
    }

    /**
     * Notify listeners of tools added.
     *
     * @param tools The tools added
     */
    private void fireToolAdded(ToolGroupChild tool) {
        try {
            if(groupListener != null) {
                ToolGroupEvent evt =
                    new ToolGroupEvent(this, tool, ToolGroupEvent.TOOL_ADDED);

                groupListener.toolAdded(evt);
            }
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_ADD_ERR_PROP) +
                         groupListener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Notify listeners of tools removed.
     *
     * @param tools The tools removed
     */
    private void fireToolRemoved(ToolGroupChild tool) {
        try {
            if(groupListener != null) {
                ToolGroupEvent evt =
                    new ToolGroupEvent(this, tool,
                        ToolGroupEvent.TOOL_REMOVED);
                groupListener.toolRemoved(evt);
            }
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_REMOVE_ERR_PROP) +
                         groupListener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param group The toolGroup removed from
     * @param child The child group that was removed
     */
    private void fireToolGroupAdded(ToolGroup child) {
        try {
            if(groupListener != null) {
                ToolGroupEvent evt =
                    new ToolGroupEvent(this, child,
                        ToolGroupEvent.GROUP_ADDED);

                groupListener.toolGroupAdded(evt);
            }
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_ADD_ERR_PROP) +
                         groupListener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been removed. Batched removes will come through the
     * toolsRemoved method.
     *
     * @param group The toolGroup removed from
     * @param child The child group that was removed
     */
    private void fireToolGroupRemoved(ToolGroup child) {
        try {
            if(groupListener != null) {
                ToolGroupEvent evt =
                    new ToolGroupEvent(this, child,
                        ToolGroupEvent.GROUP_REMOVED);

                groupListener.toolGroupRemoved(evt);
            }
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_REMOVE_ERR_PROP) +
                         groupListener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Notify listeners of tool updates.
     *
     * @param tool The tool updated
     */
    private void fireToolUpdated(ToolGroupChild tool) {
        try {
            if(groupListener != null) {
                ToolGroupEvent evt =
                    new ToolGroupEvent(this, tool, ToolGroupEvent.TOOL_UPDATED);

                groupListener.toolUpdated(evt);
            }
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_UPDATE_ERR_PROP) +
                         groupListener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Notify listeners of tool group updates.
     *
     * @param toolGroup The toolGroup updated
     */
    private void fireToolGroupUpdated(ToolGroupChild toolGroup) {
        try {
            if(groupListener != null) {
                ToolGroupEvent evt =
                    new ToolGroupEvent(this, toolGroup, ToolGroupEvent.GROUP_UPDATED);

                groupListener.toolGroupUpdated(evt);
            }
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_UPDATE_ERR_PROP) +
                         groupListener;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * Make a deep copy of a list of children into our local list.
     *
     * @param list The list instance to copy
     */
    protected void deepCopy(List<ToolGroupChild> list) {

        // TODO: This is really a shallow copy need to fix
        children.addAll(list);

        for(int i = 0; i < list.size(); i++) {
            ToolGroupChild kid = list.get(i);
            if(kid instanceof ToolGroup) {
                ToolGroup tg = (ToolGroup)kid;
                groupsByIDMap.put(tg.getToolID(), tg);
                groupChildren.add(tg);
                tg.setParent(this);
            } else {
                ToolGroupChild t = kid;
                toolsByIDMap.put(t.getToolID(), t);
                toolChildren.add(t);
            }
        }
    }

    /**
     * Get the ToolGroup type.  Returns -1 if no type set.
     *
     * @return The group type
     */
    public int getGroupType() {
        return type;
    }

    /**
     * Get the ToolGroup ID.  Returns -1 if no ID set.
     *
     * @return The group ID
     */
    public String getGroupID() {
        return groupID;
    }

    /**
     * Get the ToolGroup image.  Returns "" if no image set.
     *
     * @return The group image
     */
    public String getGroupImage() {
        return groupImage;
    }

}
