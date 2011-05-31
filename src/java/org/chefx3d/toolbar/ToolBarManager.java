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

package org.chefx3d.toolbar;

// External Imports
import java.util.ArrayList;
import java.util.Iterator;

// Internal Imports
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * The manager of all toolbars. Used to communicate non model information.
 *
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public class ToolBarManager {
    /** The singleton manager */
    private static ToolBarManager manager;

    /** The list of managed toolbars */
    private ArrayList<ToolBar> toolbars;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * Default Constructor.
     */
    ToolBarManager() {
        toolbars = new ArrayList<ToolBar>();
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the ToolBarManager
     *
     * @return The singleton toolbar manager
     */
    public static ToolBarManager getToolBarManager() {
        if (manager == null) {
            manager = new ToolBarManager();
            return manager;
        }

        return manager;
    }

    /**
     * Add a toolbar to be managed. Duplicates will be ignored.
     *
     * @param toolbar The toolbar to add
     */
    public void addToolBar(ToolBar toolbar) {
        if (!toolbars.contains(toolbar))
            toolbars.add(toolbar);
    }

    /**
     * Remove a toolbar from management. If the toolbar is not managed it will be
     * ignored.
     *
     * @param toolbar The toolbar to remove.
     */
    public void removeToolBar(ToolBar toolbar) {
        toolbars.remove(toolbar);
    }

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        
        ToolBar[] list = new ToolBar[toolbars.size()];       
        toolbars.toArray(list);
        
        for (int i = 0; i < list.length; i++) {
            ToolBar toolbar = list[i];
            toolbar.setTool(tool);
        }
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
}