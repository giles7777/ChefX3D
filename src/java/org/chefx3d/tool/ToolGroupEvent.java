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
import java.util.EventObject;

// Local imports
// None

/**
 * An event class for listening to changes from the {@link ToolGroup}
 * class
 *
 * @author Justin Couch
 * @version $Revision: 1.3 $
 */
public class ToolGroupEvent extends EventObject {

    /** The event type is for a tool added to a group */
    public static final int TOOL_ADDED = 1;

    /** The event type is for a group added to a group */
    public static final int GROUP_ADDED = 2;

    /** The event type is for a tool removed a group */
    public static final int TOOL_REMOVED = 3;

    /** The event type is for a group removed from a group */
    public static final int GROUP_REMOVED = 4;

    /** The event type is for a tool being updated */
    public static final int TOOL_UPDATED = 5;

    /** The event type is for a group being updated */
    public static final int GROUP_UPDATED = 6;

    /** The type of event that this object represents */
    private final int eventType;

    /** The child object of the event */
    private ToolGroupChild childObject;

    /**
     * Construct a new event object instance that uses the give group as
     * the parent and child for the source.
     *
     * @param parent The parent object
     * @param child The child instance
     * @param type The type of event that this is
     */
    public ToolGroupEvent(ToolGroupChild parent, ToolGroupChild child, int type) {
        super(parent);

        childObject = child;
        eventType = type;
    }

    /**
     * Get the type of the event. Returns one of the constants defined in this
     * class.
     *
     * @return An eventy type constant
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * Get the child object that the parent refers to. This needs to be cast to
     * the appropriate type based on the event type.
     */
    public ToolGroupChild getChild() {
        return childObject;
    }
}
