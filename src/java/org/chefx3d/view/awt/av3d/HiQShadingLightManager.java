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
import java.util.HashMap;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import org.j3d.util.MatrixUtils;

// Local imports
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Utility manager for the high quality shading implementation that takes
 * care of the light render passes each frame.
 * <p>
 *
 * @author Justin Couch
 * @version $Revision: 1.6 $
 */
class HiQShadingLightManager
    implements NodeUpdateListener {

    /** The list of render passes taht we're managing */
    private ArrayList<LightRenderPassDetails> renderPasses;

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /** Work var for the frustum planes and projections */
    private Vector4f[] frustumPlanes;

    /** Matrix utility operations */
    private MatrixUtils matrixUtils;

    /** Used when changing the view environment scissors. */
    private HashMap<ViewEnvironment, int[]> viewEnvScissorMap;

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /**
     * The geometry instance that recieves the deferred rendering texture
     * output. We need to update the 4 corner coords of these each time the
     * viewpoint changes position.
     */
    private VertexGeometry textureGeom;

    /** Index of the attribute in TextureGeom that we need to update */
    private int texGeomAttribID;

    /** The stored set of view vectors that are updated each frame */
    private float[] viewVectors;

    /**
     * Construct a default instance of the manager with no managed passes
     *
     * @param mgmtObserver The SceneManagerObserver
     */
    HiQShadingLightManager(SceneManagerObserver mgmtObserver) {
        this.mgmtObserver = mgmtObserver;

        renderPasses = new ArrayList<LightRenderPassDetails>();
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        frustumPlanes = new Vector4f[6];
        for(int i = 0; i < 6; i++)
            frustumPlanes[i] = new Vector4f();

        matrixUtils = new MatrixUtils();
        viewVectors = new float[12]; // 4 coords of the quad x 3 axes
        viewEnvScissorMap = new HashMap<ViewEnvironment, int[]>();

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
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
        if(src instanceof ViewEnvironment) {
            ViewEnvironment env = (ViewEnvironment)src;
            int[] scissor = viewEnvScissorMap.get(env);

            env.setScissorDimensions(scissor[0],
                                     scissor[1],
                                     scissor[2],
                                     scissor[3]);
        } else if(src instanceof RenderPass) {
            RenderPass pass = (RenderPass)src;
            boolean enabled = pass.isEnabled();
            pass.setEnabled(!enabled);
        } else if(src == textureGeom) {
            textureGeom.setAttributes(texGeomAttribID, 3, viewVectors, false);
        }
    }

    //---------------------------------------------------------------
    // Local methods
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
    }

    /**
     * Set the geometry that lighting is being applied to, and the index
     * of the attribute we have to update
     *
     * @param geom The geometry to update
     * @param attrib The index of the attribute to update
     */
    void setLightingGeomerty(VertexGeometry geom, int attrib) {
        textureGeom = geom;
        texGeomAttribID = attrib;
    }

    /**
     * Add a managed render pass to the list of renderables. No checking of
     * duplicates is performed.
     *
     * @param details The detail instance to add
     */
    void addRenderPass(LightRenderPassDetails details) {
        renderPasses.add(details);
    }

    /**
     * Perform an update on the sets of details available now.
     *
     * @param viewMatrix The current view matrix
     * @param aspectRatio Aspect ratio of the main window
     */
    void updateLightPasses(Matrix4f viewMat, ViewEnvironment mainEnv) {
        int[] dimensions = mainEnv.getViewportDimensions();

//System.out.println("view matrix\n" + viewMat);
        int[][] pixels = {
            {0, 0},
            {dimensions[2], 0},
            {dimensions[2], dimensions[3]},
            {0, dimensions[3]}
        };

        for(int i = 0; i < renderPasses.size(); i++) {
            LightRenderPassDetails details = renderPasses.get(i);

            // Need to regenerate the frustum planes each time because the
            // check light pass method trashes them.
            checkLightRenderPass(details,
                                 viewMat,
                                 dimensions[2],
                                 dimensions[3],
                                 pixels,
                                 mainEnv);
        }
    }


    /**
     * Check a single render pass for renderability
     */
    private void checkLightRenderPass(LightRenderPassDetails details,
                                      Matrix4f viewMat,
                                      int windowWidth,
                                      int windowHeight,
                                      int[][] pixels,
                                      ViewEnvironment mainEnv) {


        evaluateLightPass(details,
                          viewMat,
                          windowWidth,
                          windowHeight,
                          (float)mainEnv.getAspectRatio());

        // If there is nothing to update, ignore the rest of the calcs
        if(!details.enabled)
            return;

        // Get the frustum planes for the light pass and then transform them to
        // the world coords.
        mainEnv.generateViewFrustumPlanes(frustumPlanes);

        Matrix4f inv_view = new Matrix4f();
        matrixUtils.inverse(viewMat, inv_view);

        for(int i = 0; i < 6; i++)
            inv_view.transform(frustumPlanes[i]);

        // Generate the world view coordinates for each of the corners.
        float[] proj_mat_arr = new float[16];

        mainEnv.getProjectionMatrix(proj_mat_arr);
        Matrix4f proj_mat = new Matrix4f();
        Matrix4f unproj_mat = new Matrix4f();

        proj_mat.m00 = proj_mat_arr[0];
        proj_mat.m01 = proj_mat_arr[1];
        proj_mat.m02 = proj_mat_arr[2];
        proj_mat.m03 = proj_mat_arr[3];
        proj_mat.m10 = proj_mat_arr[4];
        proj_mat.m11 = proj_mat_arr[5];
        proj_mat.m12 = proj_mat_arr[6];
        proj_mat.m13 = proj_mat_arr[7];
        proj_mat.m20 = proj_mat_arr[8];
        proj_mat.m21 = proj_mat_arr[9];
        proj_mat.m22 = proj_mat_arr[10];
        proj_mat.m23 = proj_mat_arr[11];
        proj_mat.m30 = proj_mat_arr[12];
        proj_mat.m31 = proj_mat_arr[13];
        proj_mat.m32 = proj_mat_arr[14];
        proj_mat.m33 = proj_mat_arr[15];

        Vector3f view_trans =
            new Vector3f(viewMat.m03, viewMat.m13, viewMat.m23);

        Matrix4f model_mat = new Matrix4f();
        matrixUtils.inverse(viewMat, model_mat);

        unproj_mat.mul(model_mat, proj_mat);
        matrixUtils.inverse(unproj_mat, unproj_mat);

        Matrix4f view_rotation = new Matrix4f();
        view_rotation.set(viewMat);
        view_rotation.m03 = 0.0f;
        view_rotation.m13 = 0.0f;
        view_rotation.m23 = 0.0f;

        for(int i = 0; i < 4; i++)
        {
            // Magic number of 10 comes from the deferred shading tutorial
            // that uses 10 here. NFI why.
            // Ignore the viewsize 0 and 1 as those are always zero.
//            float in_x = ((pixels[i][0] - viewSize[0]) / viewSize[2]) * 2 - 1;
//            float in_y = ((pixels[i][1] - viewSize[1]) / viewSize[3]) * 2 - 1;
            float in_x = (pixels[i][0] / (float)windowWidth) * 2 - 1;
            float in_y = (pixels[i][1] / (float)windowHeight) * 2 - 1;
            float in_z = 10  * 2 - 1;

            Vector4f window = new Vector4f(in_x, in_y, in_z, 1);

            unproj_mat.transform(window);

            if(window.w == 0)
                System.out.println("bogus w");

            window.w = 1 / window.w;

            Vector3f v = new Vector3f();
            v.x = window.x * window.w;
            v.y = window.y * window.w;
            v.z = window.z * window.w;

            v.sub(view_trans);
            v.normalize();
            view_rotation.transform(v);

            viewVectors[i * 3] = v.x;
            viewVectors[i * 3 + 1] = v.y;
            viewVectors[i * 3 + 2] = v.z;
        }

/*
System.out.println("view vector update ");
System.out.println(viewVectors[0] + " " + viewVectors[1] + " " + viewVectors[2]);
System.out.println(viewVectors[3] + " " + viewVectors[4] + " " + viewVectors[5]);
System.out.println(viewVectors[6] + " " + viewVectors[7] + " " + viewVectors[8]);
System.out.println(viewVectors[9] + " " + viewVectors[10] + " " + viewVectors[11]);
*/
        mgmtObserver.requestDataUpdate(textureGeom, this);
    }


    /**
     * Create a render pass for a single light at the given position.
     *
     * @param lightPos The position of the light
     * @param radius The light radius to use
     * @return The render pass representing this light or null if there is
     *    no effect to be rendered for this combo
     */
    private void evaluateLightPass(LightRenderPassDetails details,
                                   Matrix4f viewMatrix,
                                   int windowWidth,
                                   int windowHeight,
                                   float aspectRatio) {

        for(int i = 0; i < 5; i++) {
            // calc distance of light from view planes. If the radius of the
            // light is completely outside the viewplane, no point doing this
            // rendering pass.
            float d = (details.lightPosition[0] * frustumPlanes[i].x +
                       details.lightPosition[1] * frustumPlanes[i].y +
                       details.lightPosition[2] * frustumPlanes[i].z) -
                      frustumPlanes[i].w;


            if(d < -details.lightRadius) {
                if(details.enabled) {
                    details.enabled = false;
                    mgmtObserver.requestDataUpdate(details.renderPass, this);
                }

                return;
            }
        }


        // transform light position from global to view space. Need to make the
        // light a 4D vector so that the position is multiplied through
        Vector4f view_light_pos =
            new Vector4f(details.lightPosition[0],
                         details.lightPosition[1],
                         details.lightPosition[2],
                         1);

        viewMatrix.transform(view_light_pos);

        int[] scissor = new int[4];

        int n = calcLightScissor(view_light_pos,
                                 details.lightRadius,
                                 windowWidth,
                                 windowHeight,
                                 scissor,
                                 aspectRatio);

        if(n == 0) {
            if(details.enabled) {
                details.enabled = false;
                mgmtObserver.requestDataUpdate(details.renderPass, this);
            }
        } else {
            if(!details.enabled) {
                details.enabled = true;
                mgmtObserver.requestDataUpdate(details.renderPass, this);
            }

            ViewEnvironment env = details.renderPass.getViewEnvironment();

            int[] old_scissor = env.getScissorDimensions();

            // Only update if we have to
            if(old_scissor[0] != scissor[0] ||
               old_scissor[1] != scissor[1] ||
               old_scissor[2] != scissor[2] ||
               old_scissor[3] != scissor[3]) {

                viewEnvScissorMap.put(env, scissor);

                mgmtObserver.requestDataUpdate(env, this);
            }
        }
    }

    /**
     * Calculate the light scissor space.
     *
     * @param lightPos The light's position in world space
     * @param radius The radius of the light's effect
     * @param sx The width size of the screen in pixels
     * @param sy The height size of the screen in pixels
     * @param scissor The bounds of the scissor to copy in to this
     */
    private int calcLightScissor(Vector4f lightPos,
                                 float radius,
                                 int sx,
                                 int sy,
                                 int[] scissor,
                                 float aspectRatio) {

        int[] rect = { 0, 0, sx, sy };
        float r2 = radius * radius;

        Vector3f l2 = new Vector3f();
        l2.x = lightPos.x * lightPos.x;
        l2.y = lightPos.y * lightPos.y;
        l2.z = lightPos.z * lightPos.z;

        float e1 = 1.2f;
        float e2 = 1.2f * aspectRatio;

        float d = r2 * l2.x - (l2.x + l2.z) * (r2 - l2.z);

        if(d >= 0) {
            d = (float)Math.sqrt(d);

            float nx1 = (radius * lightPos.x + d) / (l2.x + l2.z);
            float nx2 = (radius * lightPos.x - d) / (l2.x + l2.z);

            float nz1 = (radius - nx1 * lightPos.x) / lightPos.z;
            float nz2 = (radius - nx2 * lightPos.x) / lightPos.z;

            float pz1 = (l2.x + l2.z - r2) /
                        (lightPos.z - (nz1 / nx1)  * lightPos.x);
            float pz2 = (l2.x + l2.z - r2) /
                        (lightPos.z - (nz2 / nx2)  * lightPos.x);

            if(pz1 < 0) {
                float fx = nz1 * e1 / nx1;
                int ix = (int)((fx + 1) * sx * 0.5f);

                float px = -pz1 * nz1 / nx1;

                if(px < lightPos.x) {
                    if(rect[0] < ix)
                        rect[0] = ix;
                } else {
                    if(rect[2] > ix)
                        rect[2] = ix;
               }
            }

            if(pz2 < 0) {
                float fx = nz2 * e1 / nx2;
                int ix = (int)((fx + 1) * sx * 0.5f);

                float px = -pz2 * nz2 / nx2;

                if(px < lightPos.x)
                {
                    if(rect[0] < ix)
                        rect[0] = ix;
                } else {
                    if(rect[2] > ix)
                        rect[2] = ix;
               }
            }
        }

        d = r2 * l2.y - (l2.y + l2.z) * (r2 - l2.z);

        if(d >= 0) {
            d = (float)Math.sqrt(d);

            float ny1 = (radius * lightPos.y + d) / (l2.y + l2.z);
            float ny2 = (radius * lightPos.y - d) / (l2.y + l2.z);

            float nz1 = (radius - ny1 * lightPos.y) / lightPos.z;
            float nz2 = (radius - ny2 * lightPos.y) / lightPos.z;

            float pz1 = (l2.y + l2.z - r2) /
                        (lightPos.z - (nz1 / ny1)  * lightPos.y);
            float pz2 = (l2.y + l2.z - r2) /
                        (lightPos.z - (nz2 / ny2)  * lightPos.y);

            if(pz1 < 0) {
                float fy = nz1 * e2 / ny1;
                int iy = (int)((fy + 1) * sy * 0.5f);

                float py = -pz1 * nz1 / ny1;

                if(py < lightPos.y) {
                    if(rect[1] < iy)
                        rect[1] = iy;
                } else {
                    if(rect[3] > iy)
                        rect[3] = iy;
               }
            }

            if(pz2 < 0) {
                float fy = nz2 * e2 / ny2;
                int iy = (int)((fy + 1) * sy * 0.5f);

                float py = -pz2 * nz2 / ny2;

                if(py < lightPos.y) {
                    if(rect[1] < iy)
                        rect[1] = iy;
                } else {
                    if(rect[3] > iy)
                        rect[3] = iy;
               }
            }
        }

        int n = (rect[2]  - rect[0]) * (rect[3] - rect[1]);

        if(n <= 0)
            return 0;

        scissor[0] = rect[0];
        scissor[1] = rect[1];
        scissor[2] = rect[2];
        scissor[3] = rect[3];

        return n;
    }
}
