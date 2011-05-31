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

package org.chefx3d.toolbar.swt;

// External Imports

// Internal Imports
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.toolbar.ToolBarFactory;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.model.WorldModel;
import org.chefx3d.view.ViewManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Factory for creating toolbars.
 *
 * @author Alan Hudson
 * @version $Revision: 1.6 $
 */
public class SWTToolBarFactory implements ToolBarFactory {

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * A new SWTToolBarFactory instance
     */
    public SWTToolBarFactory() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Create a new ToolBar instance.
     *
     * @param direction What direction it should go, VERTICAL or HORIZONATAL
     * @param collapse Should singleton tools be collapsed or kept as single
     *        menus.
     * @return The ToolBar
     */
    public ToolBar createToolBar(
    		WorldModel model, 
    		CatalogManager catalogManager, 
    		int direction, 
    		boolean collapse) {

        // return new DefaultToolBar(model, direction, collapse);
        return null;
    }

    /**
     * Register an error reporter with the instance
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
