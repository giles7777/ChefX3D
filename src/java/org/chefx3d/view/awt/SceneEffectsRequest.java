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

package org.chefx3d.view.awt;

// External Imports
import org.j3d.aviatrix3d.Scene;
import org.j3d.aviatrix3d.rendering.RenderEffectsProcessor;

// Local imports
// None

/**
 * A class that allows for the adding of a RenderEffectsProcessor.
 *
 * @author christopher shankland
 * @version $Revision: 1.7 $
 */
public class SceneEffectsRequest {
    private Scene scene;
    private RenderEffectsProcessor processor;
    private SceneEffectsListener caller;

    /**
     * Constructor.  Just sets the references.
     *
     * @param scene
     * @param processor
     */
    public SceneEffectsRequest(Scene scene,
                               RenderEffectsProcessor processor,
                               SceneEffectsListener listener) {

        if(scene == null)
            throw new IllegalArgumentException("Null scene");

        if(processor == null)
            throw new IllegalArgumentException("Null processor");

        if(listener == null)
            throw new IllegalArgumentException("Null listener");


        this.scene = scene;
        this.processor = processor;
        this.caller = listener;
    }

    /**
     * Called by the SceneManagerObserver at the appropriate time.
     */
    public void setRenderEffectsProcessor() {
        scene.setRenderEffectsProcessor(processor);
        caller.sceneUpdated();
    }

    /**
     * Called by SceneManagerObserver to clean up the render processor
     */
    public void removeRenderEffectsProcessor(){
        scene.setRenderEffectsProcessor(null);
    }
}
