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

import java.util.ArrayList;

import javax.vecmath.Matrix4f;

// Local imports
import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;


import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Manager of viewport resizing and view transform relocation during scene
 * updates of the high quality rendering.
 *
 * @author Justin Couch
 * @version $Revision: 1.8 $
 */
class HiQViewportManager
    implements PerFrameObserver,
               NavigationStatusListener,
               NodeUpdateListener {

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /** Manager of the lighting passes that need to be evaluated per frame */
    private HiQShadingLightManager lightManager;

    /** List of backgrounds that need to be updated each frame */
    private ArrayList<ColorBackground> backgroundList;

    /** List of viewpoint transforms that need to be updated each frame */
    private ArrayList<TransformGroup> viewTxList;

    /** List of viewpoint transforms that need to be updated each frame */
    private ArrayList<TransformGroup> viewOrientList;

    /** Collection of textures to be resized to the full window size */
    private ArrayList<MRTOffscreenTexture2D> fullWindowResizeList;

    /** Collection of textures to be resized to the full window size */
    private ArrayList<Viewport> fullEnvResizeList;

    /** Collection of shader arguments to be updated on full window size */
    private ArrayList<ShaderArguments> fullShaderArgsResizeList;

    /** Collection of textures to be resized to the full window size */
    private ArrayList<MRTOffscreenTexture2D> halfWindowResizeList;

    /** Collection of textures to be resized to the full window size */
    private ArrayList<Viewport> halfEnvResizeList;

    /** Collection of shader arguments to be updated on full window size */
    private ArrayList<ShaderArguments> halfShaderArgsResizeList;

    /**
     * Collection of view environments that need to have the ortho params updated
     * to match the window size.
     */
    private ArrayList<ViewEnvironment> orthoResizeList;

    /**
     * Collection of view environments that need to have the perspective params updated
     * to match the window size.
     */
    private ArrayList<ViewEnvironment> perspectiveResizeList;

    /**
     * Collection of quad geometries that need to have the vertices of the quad
     * updated to match the window aspect ratio.
     */
    private ArrayList<QuadArray> texturedQuadList;

    /** Flag indicating we've just had a window resize */
    private boolean windowSizeChanged;

    /** The size of the window that we should be updating to (x, y, w, h) */
    private int[] windowDimensions;

    /** Flag indicating the background colour has changed */
    private boolean backgroundChanged;

    /** The RGBA colour of the background to use */
    private float[] backgroundColor;

    /** Flag indicating the viewpoint matrix has changed */
    private boolean viewpointChanged;

    /** Where we are in the cycle */
    private Matrix4f vpMatrix;

    /** Flag indicating the viewpoint matrix has changed */
    private boolean orientationChanged;

    /** Where we are in the cycle */
    private Matrix4f orientationMatrix;

    /**
     * Combined matrix of vpMatrix * orientationMatrix for updating the
     * lights in the light manager.
     */
    private Matrix4f combinedViewMatrix;

    /** Ortho viewport size params, calculated from the new window resize */
    private double[] orthoParams;

    /** Set of coordinates to update the textured quads with */
    private float[] quadCoords;

    /** The view environment that wraps the main window */
    private ViewEnvironment mainSceneEnv;


    /**
     * Semi-hack fix for dealing with the refresh needed when changing windows.
     * For some reason, every now and again a timing issue crops up that has
     * the preview window not update. So, we use this to delay by a few frames
     * and re-issue a new viewpoint update, which causes everything to look nice
     * again.
     */
    private int refreshDelayCount;

    /**
     * Construct a default instance of the manager
     */
    HiQViewportManager(SceneManagerObserver mgmtObserver) {
        this.mgmtObserver = mgmtObserver;

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        lightManager = new HiQShadingLightManager(mgmtObserver);

        viewTxList = new ArrayList<TransformGroup>();
        viewOrientList = new ArrayList<TransformGroup>();
        backgroundList = new ArrayList<ColorBackground>();
        fullWindowResizeList = new ArrayList<MRTOffscreenTexture2D>();
        halfWindowResizeList = new ArrayList<MRTOffscreenTexture2D>();
        fullEnvResizeList = new ArrayList<Viewport>();
        halfEnvResizeList = new ArrayList<Viewport>();
        fullShaderArgsResizeList = new ArrayList<ShaderArguments>();
        halfShaderArgsResizeList = new ArrayList<ShaderArguments>();
        orthoResizeList = new ArrayList<ViewEnvironment>();
        perspectiveResizeList = new ArrayList<ViewEnvironment>();
        texturedQuadList = new ArrayList<QuadArray>();

        windowDimensions = new int[4];
        windowSizeChanged = false;

        backgroundColor = new float[4];
        backgroundChanged = false;

        vpMatrix = new Matrix4f();
        vpMatrix.setIdentity();
        viewpointChanged = false;

        orientationMatrix = new Matrix4f();
        orientationMatrix.setIdentity();
        orientationChanged = false;

        combinedViewMatrix = new Matrix4f();
        combinedViewMatrix.setIdentity();

        orthoParams = new double[4];
        quadCoords = new float[12];

        refreshDelayCount = 0;
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {

        if(refreshDelayCount-- == 0)
          viewpointChanged = true;

        if((viewpointChanged || orientationChanged || windowSizeChanged) &&
           mainSceneEnv != null)
            lightManager.updateLightPasses(combinedViewMatrix, mainSceneEnv);

        if(viewpointChanged || windowSizeChanged) {
            for(int i = 0; i < viewTxList.size(); i++) {
                TransformGroup tg = viewTxList.get(i);

                mgmtObserver.requestBoundsUpdate(tg, this);
            }

            viewpointChanged = false;
        }

        if(orientationChanged || windowSizeChanged) {
            for(int i = 0; i < viewOrientList.size(); i++) {
                TransformGroup tg = viewOrientList.get(i);

                mgmtObserver.requestBoundsUpdate(tg, this);
            }

            orientationChanged = false;
        }

        if(backgroundChanged) {
            for(int i = 0; i < backgroundList.size(); i++) {
                ColorBackground bg = backgroundList.get(i);

                mgmtObserver.requestDataUpdate(bg, this);
            }

            backgroundChanged = false;
        }

        if(windowSizeChanged) {

            for(int i = 0; i < orthoResizeList.size(); i++) {
                ViewEnvironment view_env = orthoResizeList.get(i);
                view_env.setOrthoParams(orthoParams[0],
                                        orthoParams[1],
                                        orthoParams[2],
                                        orthoParams[3]);
            }

            double aspect_ratio = (double)orthoParams[3] / orthoParams[2];

            for(int i = 0; i < perspectiveResizeList.size(); i++) {
                ViewEnvironment view_env = perspectiveResizeList.get(i);
                view_env.setAspectRatio(aspect_ratio);
            }

            for(int i = 0; i < texturedQuadList.size(); i++) {
                QuadArray quad = texturedQuadList.get(i);
                mgmtObserver.requestBoundsUpdate(quad, this);
            }

            for(int i = 0; i < fullWindowResizeList.size(); i++) {
                MRTOffscreenTexture2D tex = fullWindowResizeList.get(i);

                mgmtObserver.requestDataUpdate(tex, this);
            }

            for(int i = 0; i < fullShaderArgsResizeList.size(); i++) {
                ShaderArguments args = fullShaderArgsResizeList.get(i);
                mgmtObserver.requestDataUpdate(args, this);
            }

            for(int i = 0; i < fullEnvResizeList.size(); i++) {
                Viewport env = fullEnvResizeList.get(i);

                env.setDimensions(windowDimensions[0],
                                  windowDimensions[1],
                                  windowDimensions[2],
                                  windowDimensions[3]);
            }

            for(int i = 0; i < halfWindowResizeList.size(); i++) {
                MRTOffscreenTexture2D tex = halfWindowResizeList.get(i);
                mgmtObserver.requestDataUpdate(tex, this);
            }

            for(int i = 0; i < halfShaderArgsResizeList.size(); i++) {
                ShaderArguments args = halfShaderArgsResizeList.get(i);
                mgmtObserver.requestDataUpdate(args, this);
            }

            for(int i = 0; i < halfEnvResizeList.size(); i++) {
                // Div 2 on int pixels will round down. Is OK because we're
                // already at half size and using texture scaling
                Viewport env = halfEnvResizeList.get(i);

                env.setDimensions(windowDimensions[0] / 2,
                                  windowDimensions[1] / 2,
                                  windowDimensions[2] / 2,
                                  windowDimensions[3] / 2);
            }

            windowSizeChanged = false;
        }
    }

    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {
        if(src instanceof TransformGroup) {
            TransformGroup tg = (TransformGroup)src;
            if(viewOrientList.contains(tg))
                tg.setTransform(orientationMatrix);
            else
                tg.setTransform(vpMatrix);
        } else if(src instanceof QuadArray) {
            QuadArray quad = (QuadArray)src;
            quad.setVertices(QuadArray.COORDINATE_3, quadCoords);
        }
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
        if(src instanceof MRTOffscreenTexture2D) {
            MRTOffscreenTexture2D tex = (MRTOffscreenTexture2D)src;

            if(fullWindowResizeList.contains(src))
                tex.resize(windowDimensions[2],
                           windowDimensions[3]);
            else
                tex.resize(windowDimensions[2] / 2,
                           windowDimensions[3] / 2);
        } else if(src instanceof ShaderArguments) {
            ShaderArguments args = (ShaderArguments)src;
            float[] tex_size = new float[2];

            if(fullShaderArgsResizeList.contains(src)) {
                tex_size[0] = windowDimensions[2];
                tex_size[1] = windowDimensions[3];
            } else {
                tex_size[0] = windowDimensions[2] * 0.5f;
                tex_size[1] = windowDimensions[3] * 0.5f;
            }

            args.setUniform("texSize", 2, tex_size, 1);
        } else if(src instanceof Background) {
            Background bg = (Background)src;
            bg.setColor(backgroundColor);
        }
    }

    //----------------------------------------------------------
    // Methods defined by NavigationStatusListener
    //----------------------------------------------------------

    /**
     * Notification that the view Transform has changed
     *
     * @param mtx The new view Transform matrix
     */
    public void viewMatrixChanged(Matrix4f mtx) {
        viewpointChanged(mtx);
    }

    /**
     * Notification that the orthographic viewport size has changed and
     * this is the new frustum details.
     *
     * @param frustumCoords The new coordinates to use in world space
     */
    public void viewportSizeChanged(double[] frustumCoords) {
        // ignored for now
    }


    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();

        lightManager.setErrorReporter(errorReporter);
    }

    /**
     * An update has occurred with the external holders, so cause everything
     * to be refreshed.
     */
    void refreshView() {
        refreshDelayCount = 2;
        viewpointChanged = true;
        orientationChanged = true;
    }

    /**
     * Notification that the surface resized.
     */
    void graphicsDeviceResized(int x, int y, int width, int height) {

        // Ignore a width and height of 0. Something went wrong somewhere.
        // If we let it through, it would crash the application. We don't
        // want that. so just ignore it.
        if(width == 0 || height == 0)
            return;

        if(x != windowDimensions[0] || y != windowDimensions[1] ||
           width != windowDimensions[2] || height != windowDimensions[3]) {

            windowDimensions[0] = x;
            windowDimensions[1] = y;
            windowDimensions[2] = width;
            windowDimensions[3] = height;

            windowSizeChanged = true;

            float aspect_ratio = (float)height / width;

            // always set up the ortho to have the width direction as 1.0 and
            // then the height is a percentage of that.
            orthoParams[0] = -1.0f;
            orthoParams[1] =  1.0f;
            orthoParams[2] = -aspect_ratio;
            orthoParams[3] =  aspect_ratio;

            quadCoords[0] = -1.0f;
            quadCoords[1] = -aspect_ratio;
            quadCoords[2] = -0.5f;

            quadCoords[3] = 1.0f;
            quadCoords[4] = -aspect_ratio;
            quadCoords[5] = -0.5f;

            quadCoords[6] = 1.0f;
            quadCoords[7] = aspect_ratio;
            quadCoords[8] = -0.5f;

            quadCoords[9] = -1.0f;
            quadCoords[10] = aspect_ratio;
            quadCoords[11] = -0.5f;
        }
    }

    /**
     * Get the last registered window width.
     *
     * @return Width dimension in pixels
     */
    int getWindowWidth() {
        return windowDimensions[2];
    }

    /**
     * Get the last registered window height.
     *
     * @return Height dimension in pixels
     */
    int getWindowHeight() {
        return windowDimensions[3];
    }

    /**
     * Notification that the background colour has changed to a new
     * value. If the array is less than length 3, the request is ignored.
     * If exactly length 3, the alpha channel is set to opaque
     *
     * @param colour The new colour to set
     */
    void backgroundChanged(float[] colour) {
        if(colour == null || colour.length < 3)
            return;

        backgroundColor[0] = colour[0];
        backgroundColor[1] = colour[1];
        backgroundColor[2] = colour[2];

        if(colour.length > 3)
            backgroundColor[3] = colour[3];
        else
            backgroundColor[3] = 1.0f;

        backgroundChanged = true;
    }

    /**
     * The viewpoint matrix has changed to a new value. Force everything
     * to update now.
     *
     * @param mat The new matrix to associate with the viewpoint
     */
    void viewpointChanged(Matrix4f mat) {
        vpMatrix.set(mat);

        viewpointChanged = true;
        combinedViewMatrix.mul(orientationMatrix, vpMatrix);
    }

    /**
     * The viewpoint matrix has changed to a new value. Force everything
     * to update now.
     *
     * @param mat The new matrix to associate with the viewpoint
     */
    void orientationViewChanged(Matrix4f mat) {
        orientationMatrix.set(mat);

        orientationChanged = true;
        combinedViewMatrix.mul(orientationMatrix, vpMatrix);
    }

    /**
     * Set the view environment that wraps the main window. Used for
     * updating the lighting whenever the window resizes or user navigates.
     * Calling more than once replaces the previously set environment
     *
     * @param env The environment entity to use
     */
    void setMainViewEnvironment(ViewEnvironment env) {
        mainSceneEnv = env;
    }

    /**
     * Set the geometry that lighting is being applied to, and the index
     * of the attribute we have to update
     *
     * @param geom The geometry to update
     * @param attrib The index of the attribute to update
     */
    void setLightingGeometry(VertexGeometry geom, int attrib) {
        lightManager.setLightingGeomerty(geom, attrib);
    }

    /**
     * Add a managed render pass to the list of renderables. No checking of
     * duplicates is performed.
     *
     * @param details The detail instance to add
     */
    void addRenderPass(LightRenderPassDetails details) {
        lightManager.addRenderPass(details);
    }

    /**
     * Add a view environment that needs to have it's ortho params updated.
     *
     * @param view The environment to keep updated
     */
    void addOrthoView(ViewEnvironment view) {
        orthoResizeList.add(view);
    }

    /**
     * Remove a view environment that needs to have it's ortho params updated.
     *
     * @param view The environment to keep remove
     */
    void removeOrthoView(ViewEnvironment view) {
        orthoResizeList.remove(view);
    }

    /**
     * Add a view environment that needs to have it's perspective params
     * updated.
     *
     * @param view The environment to keep updated
     */
    void addPerspectiveView(ViewEnvironment view) {
        perspectiveResizeList.add(view);
    }

    /**
     * Remove a view environment that needs to have it's perspective params updated.
     *
     * @param view The environment to remove
     */
    void removePerspectiveView(ViewEnvironment view) {
        perspectiveResizeList.remove(view);
    }

    /**
     * Add a quad array geometry that we manage the size of relative to the
     * window size.
     *
     * @param quad The geometry to update
     */
    void addTexturedQuad(QuadArray quad) {
        texturedQuadList.add(quad);
    }

    /**
     * Remove a quad array geometry that we manage the size of
     *
     * @param quad The geometry to remove
     */
    void removeTexturedQuad(QuadArray quad) {
        texturedQuadList.remove(quad);
    }

    /**
     * Add a background nodes that needs to be updated from time to time.
     *
     * @param bg The background to keep updated
     */
    void addBackground(ColorBackground bg) {
        if(backgroundList.contains(bg))
            return;

        backgroundList.add(bg);
        // force an update as we may be out of sync with the last colour
        // change
        backgroundChanged = true;
    }

    /**
     * Remove  a background node that needs to be updated from time to time.
     *
     * @param bg The background to keep updated
     */
    void removeBackground(ColorBackground bg) {
        backgroundList.remove(bg);
    }

    /**
     * Add a viewpoint transform that needs to be updated each frame.
     *
     * @param tx The viewpoint transform to keep updated
     */
    void addViewTransform(TransformGroup tx) {
        viewTxList.add(tx);

        // force an update as we may be out of sync with the last vp
        // change
        viewpointChanged = true;
    }

    /**
     * Remove a viewpoint transform that needs to be updated each frame.
     *
     * @param tx The viewpoint transform to keep updated
     */
    void removeViewTransform(TransformGroup tx) {
        viewTxList.remove(tx);
    }

    /**
     * Add a viewpoint transform that needs to be updated each frame.
     *
     * @param tx The viewpoint transform to keep updated
     */
    void addOrientTransform(TransformGroup tx) {
        viewOrientList.add(tx);

        // force an update as we may be out of sync with the last vp
        // change
        orientationChanged = true;
    }

    /**
     * Remove a viewpoint transform that needs to be updated each frame.
     *
     * @param tx The viewpoint transform to keep updated
     */
    void removeOrientTransform(TransformGroup tx) {
        viewOrientList.remove(tx);
    }

    /**
     * Add a full window viewport only to resize.
     *
     * @param env The viewport to manage
     */
    void addFullWindowResize(Viewport env) {
        fullEnvResizeList.add(env);
    }

    /**
     * Add a full window viewport only to resize.
     *
     * @param env The viewport to manage
     */
    void removeFullWindowResize(Viewport env) {
        fullEnvResizeList.remove(env);
    }

    /**
     * Add a half window viewport only to resize.
     *
     * @param env The viewport to manage
     */
    void addHalfWindowResize(Viewport env) {
        halfEnvResizeList.add(env);
    }

    /**
     * Add a half window viewport only to resize.
     *
     * @param env The viewport to manage
     */
    void removeHalfWindowResize(Viewport env) {
        halfEnvResizeList.remove(env);
    }

    /**
     * Add a full window shader args to resize.
     *
     * @param args The set of shader arguments to keep updated
     */
    void addFullWindowResize(ShaderArguments args) {
        fullShaderArgsResizeList.add(args);
    }

    /**
     * Add a full window shader args to resize.
     *
     * @param args The set of shader arguments to keep updated
     */
    void removeFullWindowResize(ShaderArguments args) {
        fullShaderArgsResizeList.remove(args);
    }

    /**
     * Add a half window shader args to resize.
     *
     * @param args The set of shader arguments to keep updated
     */
    void addHalfWindowResize(ShaderArguments args) {
        halfShaderArgsResizeList.add(args);
    }

    /**
     * Add a half window shader args to resize.
     *
     * @param args The set of shader arguments to keep updated
     */
    void removeHalfWindowResize(ShaderArguments args) {
        halfShaderArgsResizeList.remove(args);
    }

    /**
     * Add a full window offscreen texture to resize.
     *
     * @param tex The offscreen texture to manage
     * @param env The viewport to manage that works with the texture
     */
    void addFullWindowResize(MRTOffscreenTexture2D tex) {
        fullWindowResizeList.add(tex);
    }

    /**
     * Add a full window offscreen texture to resize.
     *
     * @param tex The offscreen texture to manage
     * @param env The viewport to manage that works with the texture
     */
    void removeFullWindowResize(MRTOffscreenTexture2D tex) {
        fullWindowResizeList.remove(tex);
    }

    /**
     * Add a half window size offscreen texture to resize.
     *
     * @param tex The offscreen texture to manage
     * @param env The viewport to manage that works with the texture
     */
    void addHalfWindowResize(MRTOffscreenTexture2D tex) {
        halfWindowResizeList.add(tex);
    }

    /**
     * Add a half window size offscreen texture to resize.
     *
     * @param tex The offscreen texture to manage
     * @param env The viewport to manage that works with the texture
     */
    void removeHalfWindowResize(MRTOffscreenTexture2D tex) {
        halfWindowResizeList.remove(tex);
    }
}
