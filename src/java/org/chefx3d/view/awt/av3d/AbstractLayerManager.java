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

// External imports
import org.j3d.aviatrix3d.*;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

// Local Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Base implementation of a LayerManager
 *
 * @author Rex Melton
 * @version $Revision: 1.10 $
 */
abstract class AbstractLayerManager implements LayerManager {

    /** Reporter instance for handing out errors */
    protected ErrorReporter errorReporter;

    /** The layer identifier and index */
    protected int id;

    /** The base layer */
    protected SimpleLayer layer;

    /** The Scene instance */
    protected SimpleScene scene;

    /** The Viewport instance */
    protected SimpleViewport viewport;

    /** The root grouping node */
    protected Group rootGroup;

    /** The initial Viewpoint */
    protected Viewpoint viewpoint;

    /** The Viewpoint's TransformGroup */
    protected TransformGroup viewpointTransform;

    /** The NavigationManager */
    protected NavigationManager navManager;

    /**
     * Restricted Constructor
     *
     * @param id The layer id
     * @param dim The initial viewport dimensions in [x, y, width, height]
     */
    protected AbstractLayerManager(int id, int[] dim) {

        this.id = id;

        // working from the bottom of the scenegraph upwards:

        //
        // create the main viewpoint
        //
        viewpoint = new Viewpoint();

        //
        // create a transform group to hold that viewpoint
        viewpointTransform = new TransformGroup();
        viewpointTransform.addChild(viewpoint);

        /*
         * If we wanted to rotate or translate the viewpoint,
         * do so like this:
         *
        Vector3f translation = new Vector3f(0, 10, 0);
        AxisAngle4f rotation = new AxisAngle4f(-1, 0, 0, ((float)Math.PI/2));
        Matrix4f mtx = new Matrix4f();
        mtx.setIdentity();
        mtx.setRotation(rotation);
        mtx.setTranslation(translation);

        viewpointTransform.setTransform(mtx);
        */

        // create a group to hold the transform group as well
        // as additional geometry
        rootGroup = new Group();
        rootGroup.addChild(viewpointTransform);

        //
        // create a simple scene to render the geometry
        //
        scene = new SimpleScene();
        scene.setRenderedGeometry(rootGroup);
        scene.setActiveView(viewpoint);

        //
        // create a viewport to set the scene
        //
        viewport = new SimpleViewport();
        viewport.setDimensions(dim[0], dim[1], dim[2], dim[3]);
        viewport.setScene(scene);

        //
        // use layer to contain the viewport
        //
        layer = new SimpleLayer();
        layer.setViewport(viewport);
    }

    //---------------------------------------------------------------
    // Methods defined by LayerManager
    //---------------------------------------------------------------

    /**
     * Return the Layer id
     *
     * @return The Layer id
     */
    public int getId() {
        return(id);
    }

    /**
     * Return the Layer object
     *
     * @return The Layer object
     */
    public Layer getLayer() {
        return(layer);
    }

    /**
     * Return the BoundingVolume of the content in this Layer
     *
     * @return The BoundingVolume
     */
    public BoundingVolume getBounds() {
        return(rootGroup.getBounds());
    }

    /**
     * Fetch the transform that holds the current viewpoint
     * information.
     *
     * @return Tranform node with a viewpoint under it
     */
    public TransformGroup getViewpointTransform() {
        return(viewpointTransform);
    }

    /**
     * Get the view environment used to render the scene with.
     *
     * @return The current configured view environment
     */
    public ViewEnvironment getViewEnvironment() {
        SimpleViewport viewport = (SimpleViewport)layer.getViewport();
        SimpleScene scene = (SimpleScene)viewport.getScene();

        return(scene.getViewEnvironment());
    }

    //------------------------------------------------------------------------
    // Local Methods
    //------------------------------------------------------------------------

    /**
     * Register an error reporter with the engine so that any errors generated
     * by the loading of script code can be reported in a nice, pretty fashion.
     * Setting a value of null will clear the currently set reporter. If one
     * is already set, the new value replaces the old.
     *
     * @param reporter The instance to use or null
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Return the navigation manager
     *
     * @return The navigation manager
     */
    NavigationManager getNavigationManager() {
        return(navManager);
    }

    /**
     * Get the root contained scene used for the whole window. Used to
     * register a callback for thumbnails.
     *
     * @return The scene instance at the root of the graph
     */
    public Scene getScene() {
        return scene;
    }
}
