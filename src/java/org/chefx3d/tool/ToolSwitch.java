/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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

import java.util.List;

// External Imports

// Local imports


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
 * @version $Revision: 1.3 $
 */
public class ToolSwitch extends ToolGroup {

    /**
     * Create a new tool descriptor.
     *
     * @param name The groups name
     * @param type The groups type
     * @param groupID The groups ID
     */
    public ToolSwitch(String name, int type, String groupID, String imagePath) {
        super(name, type, groupID, imagePath);
    }

    //----------------------------------------------------------
    // Overridden Methods
    //----------------------------------------------------------

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
            if(kid instanceof ToolGroup && !(kid instanceof ToolSwitch)) {
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

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Set the current tool this group represents
     *
     * @param tool
     */
    public void setCurrentTool(SimpleTool tool) {
        setTool(tool);
    }
    
}
