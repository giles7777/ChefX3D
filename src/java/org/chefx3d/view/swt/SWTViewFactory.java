/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.swt;

// External Imports
import java.util.Map;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.View;
import org.chefx3d.view.ViewFactory;
import org.chefx3d.model.WorldModel;

/**
 * Factory for creating Views
 *
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
public class SWTViewFactory implements ViewFactory {
    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * A new SWTViewFactory
     */
    public SWTViewFactory() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Create a view.
     *
     * @return The View
     */
    public View createView(WorldModel model, int type) {
        return null;
    }

    /**
     * Create a view.
     *
     * @param type The type of view to create
     * @param params View specific parameters.
     * @return The View
     */
    public View createView(WorldModel model, int type, Map params) {
        return null;
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
