/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
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

// Standard Imports
import java.util.*;

// Application specific imports
import org.chefx3d.AuthoringComponent;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.tool.Tool;

/**
 * This class will create a toolbar of authoring icons. An icon will be either a
 * single tool, or a drop down box of multiple tools.
 *
 * Possible tool actions: Change the underlying world model Select a 3D model to
 * place Author a sequence of points
 *
 * @author Alan Hudson
 * @version $Revision: 1.9 $
 */
public interface ToolBar extends AuthoringComponent {
    /** Place the toolbar vertically */
    public static final int VERTICAL = 0;

    /** Place the toolbar horizontally */
    public static final int HORIZONTAL = 1;

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool);

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter);
}
