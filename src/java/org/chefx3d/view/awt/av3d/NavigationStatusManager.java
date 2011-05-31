/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External Imports
import java.util.ArrayList;

import javax.vecmath.Matrix4f;

import org.j3d.aviatrix3d.ViewEnvironment;

// Local Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;


/**
 * Manager class for reporting status on navigation events
 *
 * @author Rex Melton
 * @version $Revision: 1.5 $
 */
class NavigationStatusManager {

    /** Error message for the viewpoint change internal exception */
    private static final String VP_MATRIX_CHANGE_MSG =
        "Exception caught when sending out a viewpointMatrixChanged() call";

    /** Error message for the viewpoint change internal exception */
    private static final String VP_SIZE_CHANGE_MSG =
        "Exception caught when sending out a viewpointSizeChanged() call";

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /** The list of listeners */
    private ArrayList<NavigationStatusListener> listeners;

    /**
     * Constructor
     */
    NavigationStatusManager() {
        listeners = new ArrayList<NavigationStatusListener>();
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Add a NavigationStatusListener.
     *
     * @param nsl The listener.  Duplicates and nulls are ignored.
     */
    void addNavigationStatusListener(NavigationStatusListener nsl) {

//      if (!listeners.contains(nsl)) {
            listeners.add(nsl);
//        }
    }

    /**
     * Remove a NavigationStatusListener.
     *
     * @param nsl The listener
     */
    void removeNavigationStatusListener(NavigationStatusListener nsl) {
        listeners.remove(nsl);
    }

    /**
     * Notify the listeners of a change
     *
     * @param mtx The new view Transform matrix
     */
    void fireViewMatrixChanged(Matrix4f mtx) {

        int num = listeners.size();
        NavigationStatusListener nsl;

        for (int i = 0; i < num; i++) {
            nsl = listeners.get(i);
            try {
                nsl.viewMatrixChanged(mtx);
            } catch(Exception e) {
                errorReporter.errorReport(VP_MATRIX_CHANGE_MSG, e);
            }
        }
    }

    /**
     * Notification that the orthographic viewport size has changed and
     * this is the new frustum details.
     *
     * @param frustumCoords The new coordinates to use in world space
     */
    public void fireViewportSizeChanged(double[] frustumCoords) {

        int num = listeners.size();
        NavigationStatusListener nsl;

        for (int i = 0; i < num; i++) {
            nsl = listeners.get(i);

            try {
                nsl.viewportSizeChanged(frustumCoords);
            } catch(Exception e) {
                errorReporter.errorReport(VP_SIZE_CHANGE_MSG, e);
            }
        }
    }
}
