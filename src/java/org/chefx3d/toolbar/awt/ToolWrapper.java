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

package org.chefx3d.toolbar.awt;

// External Imports

// Internal Imports
import org.chefx3d.tool.SimpleTool;

/**
 * @author Alan Hudson
 * @version $Revision: 1.2 $
 */

class ToolWrapper {
    private SimpleTool tool;

    public ToolWrapper(SimpleTool tool) {
        this.tool = tool;
    }

    public SimpleTool getTool() {
        return tool;
    }

    public String toString() {
        return tool.getName();
    }
}
