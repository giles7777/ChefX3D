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
import java.awt.*;

import org.j3d.aviatrix3d.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

import org.ietf.uri.URL;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import javax.media.opengl.GLCapabilities;

import org.j3d.util.MatrixUtils;
import org.j3d.util.TriangleUtils;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

/**
 * Manager for the high quality shading implementation.
 * <p>
 *
 * @author Justin Couch
 * @version $Revision: 1.14 $
 */
class HiQShadingManager {

    /** Render GBuffer depth pass vertex shader file name */
    private static final String SSAO_GBUFFER_VTX_SHADER_FILE =
        "glsl/preview/ssao_normal_vert.glsl";

    /** Render GBuffer depth Fragment shader file name */
    private static final String SSAO_GBUFFER_FRAG_SHADER_FILE =
        "glsl/preview/ssao_normal_frag.glsl";

    /** Screen space ambient occlusion pass vertex shader file name */
    private static final String SSAO_VTX_SHADER_FILE =
        "glsl/preview/deferred_ssao_vert.glsl";

    /** Screen space ambient occlusion fragment shader file name */
    private static final String SSAO_FRAG_SHADER_FILE =
        "glsl/preview/deferred_ssao_frag.glsl";

    /** Post processing anti alias pass vertex shader file name */
    private static final String AA_VTX_SHADER_FILE =
        "glsl/preview/deferred_aa_vert.glsl";

    /** Post processing anti alias fragment shader file name */
    private static final String AA_FRAG_SHADER_FILE =
        "glsl/preview/deferred_aa_frag.glsl";

    /** Post processing bloom fragment shader file name */
    private static final String BLOOM_FRAG_SHADER_FILE =
        "glsl/preview/deferred_bloom_frag.glsl";

    /** Post processing bloom pass vertex shader file name */
    private static final String BLOOM_VTX_SHADER_FILE =
        "glsl/preview/deferred_bloom_vert.glsl";

    /** Render pass vertex shader string */
    private static final String MAT_VTX_SHADER_FILE =
        "glsl/preview/deferred_material_vert.glsl";

    /** Fragment shader file name for the rendering pass */
    private static final String MAT_FRAG_SHADER_FILE =
        "glsl/preview/deferred_material_frag.glsl";

    /** Render pass vertex shader string */
    private static final String LIGHT_VTX_SHADER_FILE =
        "glsl/preview/deferred_light_vert.glsl";

    /** Fragment shader file name for the rendering pass */
    private static final String LIGHT_FRAG_SHADER_FILE =
        "glsl/preview/deferred_light_frag.glsl";

    /** Final stage source combiner vertex shader file name */
    private static final String FINAL_VTX_SHADER_FILE =
        "glsl/preview/deferred_final_vert.glsl";

    /** Final stage source combiner fragment shader file name */
    private static final String FINAL_FRAG_SHADER_FILE =
        "glsl/preview/deferred_final_frag.glsl";


    /**
     * Image file holding a random colour sample map for SSAO
     */
    private static final String RANDOM_MAP_FILE =
        "images/shading/ssaoNoise.png";

    /** Sample radius to make for SSAO. Units in world space */
    private static final float SSAO_SAMPLE_RADIUS = 0.006f;

    /**
     * A local strength modifier when working out how much AO to
     * apply to a pixel.
     */
    private static final float SSAO_LOCAL_STRENGTH = 0.07f;

    /** Global multiplier to make to all AO calculations. */
    private static final float SSAO_GLOBAL_STRENGTH = 1.08f;

    /**
     * Falloff when determining how far from an object boundary AO should
     * be applied. Units in world space.
     */
    private static final float SSAO_FALLOFF = 0.000002f;

    /**
     * An offset amount for sampling in to the random noise texture. Should
     * always be a non-whole number so that we get some bluring from the
     * texture sampling process, resulting in a smoother effect.
     */
    private static final float SSAO_SAMPLE_OFFSET =  18.5f;

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /** Manager of the viewports for resizing */
    private HiQViewportManager viewportManager;

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

    /** The view environment created for the main scene */
    private ViewEnvironment mainSceneEnv;

    /** Shared shader program for lighting passes */
    private ShaderProgram lightingShader;

    /** Texture quad used for the various render passes */
    private QuadArray baseQuad;

    /** The root scene that this manager handles */
    private SimpleScene rootScene;

    /** Loader and general resolver of URLs */
    private AV3DNodeFactory av3dNodeFactory;

    /** File name resolver for each item */
    private FileLoader fileLookup;

    // Sets of flags about what to turn on and off

    /** Enable antialiasing. If not enabled, only works on the main texture */
    private boolean useFSAA;

    /** Enable ambient occlusion lighting */
    private boolean useSSAO;

    /** Enable HDR bloom lighting */
    private boolean useBloom;

    /** Texture unit holding the deferred shading pass only */
    private TextureUnit deferredTexture;

    /** Texture unit holding the fullscreen AA pass only */
    private TextureUnit fsaaTexture;

    /** Texture unit holding the SSAO shading pass only */
    private TextureUnit ssaoTexture;

    /** Texture unit holding the bloom pass only */
    private TextureUnit bloomTexture;

    /**
     * Construct a new manager.
     *
     * @param vpMgr Manager of the viewports that we need to give info
     *    to when rendering
     */
    HiQShadingManager(HiQViewportManager vpMgr) {

        viewportManager = vpMgr;

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        fileLookup = new FileLoader();
        av3dNodeFactory = new AV3DNodeFactory(errorReporter);

        backgroundList = new ArrayList<ColorBackground>();
        viewTxList = new ArrayList<TransformGroup>();
        viewOrientList = new ArrayList<TransformGroup>();
        fullWindowResizeList = new ArrayList<MRTOffscreenTexture2D>();
        halfWindowResizeList = new ArrayList<MRTOffscreenTexture2D>();
        fullEnvResizeList = new ArrayList<Viewport>();
        halfEnvResizeList = new ArrayList<Viewport>();
        fullShaderArgsResizeList = new ArrayList<ShaderArguments>();
        halfShaderArgsResizeList = new ArrayList<ShaderArguments>();
        orthoResizeList = new ArrayList<ViewEnvironment>();
        perspectiveResizeList = new ArrayList<ViewEnvironment>();
        texturedQuadList = new ArrayList<QuadArray>();

        useFSAA = true;
        useSSAO = true;
        useBloom = false;
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
     * Get the root scene that this manager handles.
     */
    SimpleScene getContainedScene() {
        return rootScene;
    }

    /**
     * Get the view environment used to represent the rendered scene. This
     * is not the root view environment, as that is a different object to
     * the one that is off down somewhere in some offscreen buffers.
     *
     * @return The view environment of the real scene
     */
    ViewEnvironment getViewEnvironment() {
        return mainSceneEnv;
    }

    /**
     * Shut down this manager now. Make sure to pull all the stuff from
     * the viewport manager that no longer needs to be managed.
     */
    void shutdown() {
        for(int i = 0; i < backgroundList.size(); i++)
            viewportManager.removeBackground(backgroundList.get(i));

        for(int i = 0; i < viewTxList.size(); i++)
            viewportManager.removeViewTransform(viewTxList.get(i));

        for(int i = 0; i < viewOrientList.size(); i++)
            viewportManager.removeOrientTransform(viewOrientList.get(i));

        for(int i = 0; i < fullWindowResizeList.size(); i++)
            viewportManager.removeFullWindowResize(fullWindowResizeList.get(i));

        for(int i = 0; i < halfWindowResizeList.size(); i++)
            viewportManager.removeHalfWindowResize(halfWindowResizeList.get(i));

        for(int i = 0; i < fullEnvResizeList.size(); i++)
            viewportManager.removeFullWindowResize(fullEnvResizeList.get(i));

        for(int i = 0; i < halfEnvResizeList.size(); i++)
            viewportManager.removeHalfWindowResize(halfEnvResizeList.get(i));

        for(int i = 0; i < fullShaderArgsResizeList.size(); i++)
            viewportManager.removeFullWindowResize(fullShaderArgsResizeList.get(i));

        for(int i = 0; i < halfShaderArgsResizeList.size(); i++)
            viewportManager.removeHalfWindowResize(halfShaderArgsResizeList.get(i));

        for(int i = 0; i < orthoResizeList.size(); i++)
            viewportManager.removeOrthoView(orthoResizeList.get(i));

        for(int i = 0; i < perspectiveResizeList.size(); i++)
            viewportManager.removePerspectiveView(perspectiveResizeList.get(i));

        for(int i = 0; i < texturedQuadList.size(); i++)
            viewportManager.removeTexturedQuad(texturedQuadList.get(i));
    }

    /**
     * Change the state of full screen antialiasing. By default it is on.
     *
     * @param enable True to enable the use of FSAA
     */
    void enableFSAA(boolean enable) {
        useFSAA = enable;

        // do stuff to dynamically change the scene here.
    }

    /**
     * Change the state of screen space ambient occlusion culling.
     * By default it is on.
     *
     * @param enable True to enable the use of SSAO
     */
    void enableSSAO(boolean enable) {
        useSSAO = enable;

        // do stuff to dynamically change the scene here.
    }


    /**
     * Change the state of HDR Bloom rendering. By default it is off.
     *
     * @param enable True to enable the use of HDR
     */
    void enableBloom(boolean enable) {
        useBloom = enable;

        // do stuff to dynamically change the scene here.
    }

    /**
     * After the various flags have been set, construct the internal scene graph
     * and any additional data.
     *
     * @param initialWidth The starting width in pixels of the render
     * @param initialHeight The starting height in pixels of the render
     */
    void initialize(SharedNode sceneContent,
                    Matrix4f viewMatrix,
                    LightConfigData ambientLightConfig,
                    LightConfigData[] pointLightConfig,
                    float[] bgColor,
                    int initialWidth,
                    int initialHeight) {


        setupSceneGraph(sceneContent,
                        viewMatrix,
                        ambientLightConfig,
                        pointLightConfig,
                        bgColor,
                        initialWidth,
                        initialHeight);
    }


    /**
     * Setup the basic scene which consists of a quad and a viewpoint displaying
     * the shared scene graph.
     */
    private void setupSceneGraph(Node sceneContent,
                                 Matrix4f viewMatrix,
                                 LightConfigData ambientLightConfig,
                                 LightConfigData[] pointLightConfig,
                                 float[] bgColor,
                                 int initialWidth,
                                 int initialHeight) {

        createCommonQuad();

        MRTOffscreenTexture2D gbuffer_tex =
            createGBufferTexture(viewMatrix,
                                 bgColor,
                                 sceneContent,
                                 initialWidth,
                                 initialHeight);

        MRTOffscreenTexture2D deferred_tex =
            createDeferredRenderTexture(gbuffer_tex,
                                        ambientLightConfig,
                                        pointLightConfig,
                                        initialWidth,
                                        initialHeight,
                                        viewMatrix);

        deferredTexture = new TextureUnit();
        deferredTexture.setTexture(deferred_tex);

        MRTOffscreenTexture2D aa_tex =
            createAATexture(gbuffer_tex.getRenderTarget(1),
                            deferred_tex,
                            initialWidth,
                            initialHeight);

        fsaaTexture = new TextureUnit();
        fsaaTexture.setTexture(aa_tex);

        MRTOffscreenTexture2D bloom_tex =
            createBloomTexture(deferred_tex,
                               initialWidth,
                               initialHeight);


        bloomTexture = new TextureUnit();
        bloomTexture.setTexture(bloom_tex);

        MRTOffscreenTexture2D ssao_tex =
            createSSAOTexture(viewMatrix,
                              bgColor,
                              sceneContent,
                              initialWidth,
                              initialHeight);

        ssaoTexture = new TextureUnit();
        ssaoTexture.setTexture(ssao_tex);

//        ssao_tex.setMinFilter(Texture.MINFILTER_NICEST);
//        ssao_tex.setMagFilter(Texture.MAGFILTER_NICEST);

        // really should modify the texture loaded based on the flags
        // set.

        String[] vert_shader_txt = loadShaderFile(FINAL_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(FINAL_FRAG_SHADER_FILE);

        ShaderObject vert_shader = new ShaderObject(true);
        vert_shader.setSourceStrings(vert_shader_txt, 1);
        vert_shader.requestInfoLog();
        vert_shader.compile();

        ShaderObject frag_shader = new ShaderObject(false);
        frag_shader.setSourceStrings(frag_shader_txt, 1);
        frag_shader.requestInfoLog();
        frag_shader.compile();

        ShaderProgram shader_prog = new ShaderProgram();
        shader_prog.addShaderObject(vert_shader);
        shader_prog.addShaderObject(frag_shader);
        shader_prog.requestInfoLog();
        shader_prog.link();

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniformSampler("baseMap", 0);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(shader_prog);
        shader.setShaderArguments(shader_args);

        TextureUnit[] tex_unit = new TextureUnit[3];
        int tex_count = 0;

        if(useFSAA)
            tex_unit[0] = fsaaTexture;
        else
            tex_unit[0] = deferredTexture;

        tex_count++;

        if(useSSAO) {
            shader_args.setUniformSampler("ssoaMap", tex_count);
            tex_unit[tex_count] = ssaoTexture;
            tex_count++;
        }

        if(useBloom) {
            shader_args.setUniformSampler("effectMap", tex_count);
            tex_unit[tex_count] = bloomTexture;
            tex_count++;
        }


        Appearance colour_app = new Appearance();
        colour_app.setTextureUnits(tex_unit, tex_count);
        colour_app.setShader(shader);

        Shape3D colour_shape = new Shape3D();
        colour_shape.setGeometry(baseQuad);
        colour_shape.setAppearance(colour_app);

        Viewpoint vp = new Viewpoint();

        Group root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(colour_shape);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);

        ViewEnvironment env = main_scene.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);

        rootScene = main_scene;
    }

    /**
     * Create the main deferred shaded image texture that will be used for
     * post-processing.
     */
    private MRTOffscreenTexture2D createDeferredRenderTexture(MRTOffscreenTexture2D gBuffer,
                                                              LightConfigData ambientLightConfig,
                                                              LightConfigData[] pointLightConfig,
                                                              int initialWidth,
                                                              int initialHeight,
                                                              Matrix4f viewPos) {

        // Quads to draw to. Don't reuse the baseQuad here because we want to
        // set separate per-vertex colouring for ambient lighting. Just makes
        // sure we don't accidently effect anything else.
        float[] quad_coords = {
              -1, -1, -0.5f,
               1, -1, -0.5f,
               1,  1, -0.5f,
               -1, 1, -0.5f
        };

        float[] quad_normals = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
        float[][] tex_coord = { { 0, 0,  1, 0,  1, 1,  0, 1 } };

        int[] tex_type = { VertexGeometry.TEXTURE_COORDINATE_2 };

        QuadArray real_geom = new QuadArray();
        real_geom.setValidVertexCount(4);
        real_geom.setVertices(QuadArray.COORDINATE_3, quad_coords);
        real_geom.setNormals(quad_normals);
        real_geom.setTextureCoordinates(tex_type, tex_coord, 1);

        if (ambientLightConfig != null)
            real_geom.setSingleColor(false, ambientLightConfig.lightColor);
        else
            real_geom.setSingleColor(false, new float[] {0,0,0});

        viewportManager.addTexturedQuad(real_geom);
        texturedQuadList.add(real_geom);

        viewportManager.setLightingGeometry(real_geom, 5);

        TextureAttributes tex_attr = new TextureAttributes();
        tex_attr.setTextureMode(TextureAttributes.MODE_MODULATE);

        TextureUnit[] tex_unit = { new TextureUnit() };
        tex_unit[0].setTexture(gBuffer);
        tex_unit[0].setTextureAttributes(tex_attr);

        Appearance app_1 = new Appearance();
        app_1.setTextureUnits(tex_unit, 1);

        Shape3D shape_1 = new Shape3D();
        shape_1.setGeometry(real_geom);
        shape_1.setAppearance(app_1);


        SharedNode common_geom = new SharedNode();
        common_geom.setChild(shape_1);

        Viewpoint vp = new Viewpoint();

        Group root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(common_geom);

        // Set up the first render pass which does ambient lighting
        GeneralBufferState ambient_gbs = new GeneralBufferState();
        ambient_gbs.enableBlending(false);
        ambient_gbs.setSourceBlendFactor(GeneralBufferState.BLEND_ONE);
        ambient_gbs.setDestinationBlendFactor(GeneralBufferState.BLEND_ONE);

        DepthBufferState ambient_dbs = new DepthBufferState();
        ambient_dbs.setClearBufferState(true);
        ambient_dbs.enableDepthTest(false);
        ambient_dbs.enableDepthWrite(false);
        ambient_dbs.setDepthFunction(DepthBufferState.FUNCTION_LESS_OR_EQUAL);

        ColorBufferState ambient_cbs = new ColorBufferState();
        ambient_cbs.setClearBufferState(true);
        ambient_cbs.setClearColor(0, 0, 0, 1);

        RenderPass ambient_pass = new RenderPass();
        ambient_pass.setGeneralBufferState(ambient_gbs);
        ambient_pass.setDepthBufferState(ambient_dbs);
        ambient_pass.setColorBufferState(ambient_cbs);
        ambient_pass.setRenderedGeometry(root_grp);
        ambient_pass.setActiveView(vp);

        ViewEnvironment env = ambient_pass.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);

        // Process the light passes
        MultipassScene scene = new MultipassScene();
        scene.addRenderPass(ambient_pass);

        for(int i = 0; i < pointLightConfig.length; i++) {
            LightRenderPassDetails rp_details =
                createLightPass(pointLightConfig[i].lightPosition,
                                pointLightConfig[i].lightColor,
                                pointLightConfig[i].attenuation,
                                pointLightConfig[i].lightRadius,
                                common_geom,
                                gBuffer);

            viewportManager.addRenderPass(rp_details);
            scene.addRenderPass(rp_details.renderPass);
        }

        MultipassViewport viewport = new MultipassViewport();
        viewport.setDimensions(0, 0, initialWidth, initialHeight);
        viewport.setScene(scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(viewport);

        Layer[] layers = { layer };

        // The texture requires its own set of capabilities.
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(false);
        caps.setPbufferRenderToTexture(true);
        caps.setPbufferFloatingPointBuffers(true);
        caps.setAlphaBits(8);

        MRTOffscreenTexture2D off_tex =
            new MRTOffscreenTexture2D(caps, initialWidth, initialHeight, 1);

        off_tex.setClearColor(1, 0, 0, 1);
        off_tex.setRepaintRequired(true);
        off_tex.setLayers(layers, 1);

        viewportManager.addFullWindowResize(off_tex);
        viewportManager.addFullWindowResize(viewport);

        fullWindowResizeList.add(off_tex);
        fullEnvResizeList.add(viewport);


        //fullWindowResizeList.add(off_tex);
        //fullEnvResizeList.add(viewport);

        return off_tex;
    }

    /**
     * Creates the antialias pass output texture from the input texture
     */
    private MRTOffscreenTexture2D createAATexture(Texture normalSource,
                                                  Texture colourSource,
                                                  int initialWidth,
                                                  int initialHeight) {

        TextureAttributes tex_attr = new TextureAttributes();
        tex_attr.setTextureMode(TextureAttributes.MODE_REPLACE);

        TextureUnit[] tex_unit = { new TextureUnit(), new TextureUnit() };
        tex_unit[0].setTexture(colourSource);
        tex_unit[0].setTextureAttributes(tex_attr);
        tex_unit[1].setTexture(normalSource);
        tex_unit[1].setTextureAttributes(tex_attr);

        String[] vert_shader_txt = loadShaderFile(AA_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(AA_FRAG_SHADER_FILE);

        ShaderObject vert_shader = new ShaderObject(true);
        vert_shader.setSourceStrings(vert_shader_txt, 1);
        vert_shader.requestInfoLog();
        vert_shader.compile();

        ShaderObject frag_shader = new ShaderObject(false);
        frag_shader.setSourceStrings(frag_shader_txt, 1);
        frag_shader.requestInfoLog();
        frag_shader.compile();

        ShaderProgram shader_prog = new ShaderProgram();
        shader_prog.addShaderObject(vert_shader);
        shader_prog.addShaderObject(frag_shader);
        shader_prog.requestInfoLog();
        shader_prog.link();

        float[] threshold = { 1.0f };
        float[] tex_size = { initialWidth, initialHeight };

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniform("weight", 1, threshold, 1);
        shader_args.setUniform("texSize", 2, tex_size, 1);
        shader_args.setUniformSampler("colourMap", 0);
        shader_args.setUniformSampler("normalMap", 1);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(shader_prog);
        shader.setShaderArguments(shader_args);

        Appearance app = new Appearance();
        app.setTextureUnits(tex_unit, 2);
        app.setShader(shader);

        Shape3D shape_1 = new Shape3D();
        shape_1.setGeometry(baseQuad);
        shape_1.setAppearance(app);

        Viewpoint vp = new Viewpoint();

        Group root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(shape_1);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);

        ViewEnvironment env = main_scene.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);

        SimpleViewport viewport = new SimpleViewport();
        viewport.setDimensions(0, 0, initialWidth, initialHeight);
        viewport.setScene(main_scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(viewport);

        Layer[] layers = { layer };

        // The texture requires its own set of capabilities.
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(false);
        caps.setPbufferRenderToTexture(true);
        caps.setPbufferFloatingPointBuffers(true);

        MRTOffscreenTexture2D off_tex =
            new MRTOffscreenTexture2D(caps, initialWidth, initialHeight, 1);

        off_tex.setClearColor(0, 0, 0, 1);
        off_tex.setRepaintRequired(true);
        off_tex.setLayers(layers, 1);

        viewportManager.addFullWindowResize(off_tex);
        viewportManager.addFullWindowResize(viewport);
        viewportManager.addFullWindowResize(shader_args);

        fullWindowResizeList.add(off_tex);
        fullEnvResizeList.add(viewport);
        fullShaderArgsResizeList.add(shader_args);

        return off_tex;
    }

    /**
     * Creates the bloom passes output texture from the input texture
     */
    private MRTOffscreenTexture2D createBloomTexture(Texture colourSource,
                                                     int initialWidth,
                                                     int initialHeight) {

        TextureAttributes tex_attr = new TextureAttributes();
        tex_attr.setTextureMode(TextureAttributes.MODE_REPLACE);

        TextureUnit[] tex_unit = { new TextureUnit() };
        tex_unit[0].setTexture(colourSource);
        tex_unit[0].setTextureAttributes(tex_attr);

        String[] vert_shader_txt = loadShaderFile(BLOOM_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(BLOOM_FRAG_SHADER_FILE);

        ShaderObject vert_shader = new ShaderObject(true);
        vert_shader.setSourceStrings(vert_shader_txt, 1);
        vert_shader.requestInfoLog();
        vert_shader.compile();

        ShaderObject frag_shader = new ShaderObject(false);
        frag_shader.setSourceStrings(frag_shader_txt, 1);
        frag_shader.requestInfoLog();
        frag_shader.compile();

        ShaderProgram shader_prog = new ShaderProgram();
        shader_prog.addShaderObject(vert_shader);
        shader_prog.addShaderObject(frag_shader);
        shader_prog.requestInfoLog();
        shader_prog.link();

        float[] h_tex_size = { 1.0f / initialWidth, 0 };
        float[] v_tex_size = { 0, 1.0f / initialHeight };

        ShaderArguments h_shader_args = new ShaderArguments();
        h_shader_args.setUniform("texSize", 2, h_tex_size, 1);
        h_shader_args.setUniformSampler("colourMap", 0);

        GLSLangShader h_shader = new GLSLangShader();
        h_shader.setShaderProgram(shader_prog);
        h_shader.setShaderArguments(h_shader_args);

        Appearance h_app = new Appearance();
        h_app.setTextureUnits(tex_unit, 1);
        h_app.setShader(h_shader);

        Shape3D h_shape = new Shape3D();
        h_shape.setGeometry(baseQuad);
        h_shape.setAppearance(h_app);

        Viewpoint vp = new Viewpoint();

        Group root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(h_shape);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);

        ViewEnvironment env = main_scene.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);

        SimpleViewport viewport = new SimpleViewport();
        viewport.setDimensions(0, 0, initialWidth / 2, initialHeight / 2);
        viewport.setScene(main_scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(viewport);

        Layer[] layers = { layer };

        // The texture requires its own set of capabilities.
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(false);
        caps.setPbufferRenderToTexture(true);
        caps.setPbufferFloatingPointBuffers(true);

        MRTOffscreenTexture2D horizontal_bloom_tex =
            new MRTOffscreenTexture2D(caps, initialWidth / 2, initialHeight / 2, 1);

        horizontal_bloom_tex.setClearColor(0, 0, 0, 1);
        horizontal_bloom_tex.setRepaintRequired(true);
        horizontal_bloom_tex.setLayers(layers, 1);

        viewportManager.addHalfWindowResize(horizontal_bloom_tex);
        viewportManager.addHalfWindowResize(viewport);

        halfWindowResizeList.add(horizontal_bloom_tex);
        halfEnvResizeList.add(viewport);

        // Now create the vertical bloom texture by using the horizontal as the
        // input source. Everything else is almost identical

        ShaderArguments v_shader_args = new ShaderArguments();
        v_shader_args.setUniform("texSize", 2, v_tex_size, 1);
        v_shader_args.setUniformSampler("colourMap", 0);

        GLSLangShader v_shader = new GLSLangShader();
        v_shader.setShaderProgram(shader_prog);
        v_shader.setShaderArguments(v_shader_args);

        TextureUnit[] v_tex_unit = { new TextureUnit() };
        v_tex_unit[0].setTexture(horizontal_bloom_tex);
        v_tex_unit[0].setTextureAttributes(tex_attr);

        Appearance v_app = new Appearance();
        v_app.setTextureUnits(v_tex_unit, 1);
        v_app.setShader(v_shader);

        Shape3D v_shape = new Shape3D();
        v_shape.setGeometry(baseQuad);
        v_shape.setAppearance(v_app);

        vp = new Viewpoint();

        root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(v_shape);

        main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);

        env = main_scene.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);

        viewport = new SimpleViewport();
        viewport.setDimensions(0, 0, initialWidth / 2, initialHeight / 2);
        viewport.setScene(main_scene);

        layer = new SimpleLayer();
        layer.setViewport(viewport);

        layers = new Layer[1];
        layers[0] = layer;

        MRTOffscreenTexture2D vertical_bloom_tex =
            new MRTOffscreenTexture2D(caps, initialWidth / 2, initialHeight / 2, 1);

        vertical_bloom_tex.setClearColor(0, 0, 0, 1);
        vertical_bloom_tex.setRepaintRequired(true);
        vertical_bloom_tex.setLayers(layers, 1);

        viewportManager.addHalfWindowResize(vertical_bloom_tex);
        viewportManager.addHalfWindowResize(viewport);

        halfWindowResizeList.add(vertical_bloom_tex);
        halfEnvResizeList.add(viewport);

//viewportManager.addFullWindowResize(shader_args);

        return vertical_bloom_tex;
    }

    /**
     * Create the contents of the offscreen texture that is being rendered
     */
    private MRTOffscreenTexture2D createGBufferTexture(Matrix4f viewPos,
                                                       float[] bgColor,
                                                       Node sceneContent,
                                                       int initialWidth,
                                                       int initialHeight) {

        Viewpoint vp = new Viewpoint();

        Vector3f translation = new Vector3f(0, 1.5f, 10);
        Matrix4f mtx = new Matrix4f();
        mtx.setIdentity();
        mtx.setTranslation(translation);

        TransformGroup vp_tx = new TransformGroup();
        vp_tx.setTransform(mtx);
        vp_tx.addChild(vp);

        TransformGroup nav_tx = new TransformGroup();
        nav_tx.setTransform(viewPos);
        nav_tx.addChild(vp_tx);

        // Create the gbuffer shader
        String[] vert_shader_txt = loadShaderFile(MAT_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(MAT_FRAG_SHADER_FILE);

        ShaderObject vert_shader = new ShaderObject(true);
        vert_shader.setSourceStrings(vert_shader_txt, 1);
        vert_shader.requestInfoLog();
        vert_shader.compile();

        ShaderObject frag_shader = new ShaderObject(false);
        frag_shader.setSourceStrings(frag_shader_txt, 1);
        frag_shader.requestInfoLog();
        frag_shader.compile();

        ShaderProgram shader_prog = new ShaderProgram();
        shader_prog.addShaderObject(vert_shader);
        shader_prog.addShaderObject(frag_shader);
        shader_prog.bindAttributeName("tangent", 5);
        shader_prog.requestInfoLog();
        shader_prog.link();

        float[] tile = { 1.0f };

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniform("tileScale", 1, tile, 1);
        shader_args.setUniformSampler("colourMap", 0);
        shader_args.setUniformSampler("normalMap", 1);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(shader_prog);
        shader.setShaderArguments(shader_args);

        Appearance global_app = new Appearance();
        global_app.setShader(shader);

        AppearanceOverride app_ovr = new AppearanceOverride();
        app_ovr.setEnabled(true);
        app_ovr.setLocalAppearanceOnly(false);
        app_ovr.setAppearance(global_app);

        ColorBackground bg = new ColorBackground();
        bg.setColor(bgColor);

        Group root_grp = new Group();
        root_grp.addChild(app_ovr);
        root_grp.addChild(nav_tx);
        root_grp.addChild(bg);
        root_grp.addChild(sceneContent);

        viewportManager.addBackground(bg);
        backgroundList.add(bg);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);
        main_scene.setActiveBackground(bg);

        mainSceneEnv = main_scene.getViewEnvironment();
        mainSceneEnv.setFarClipDistance(1000);

        viewportManager.setMainViewEnvironment(mainSceneEnv);
        viewportManager.addPerspectiveView(mainSceneEnv);
        perspectiveResizeList.add(mainSceneEnv);


/*
 Use if no depth texture support
float near_clip = (float)mainSceneEnv.getNearClipDistance();
float far_clip = (float)mainSceneEnv.getFarClipDistance();
float[] d_planes =
{
    -far_clip / (far_clip - near_clip),
    -far_clip * near_clip / (far_clip - near_clip)
};

shader_args.setUniform("planes", 2, d_planes, 1);
*/

        // Then the basic layer and viewport at the top:
        SimpleViewport viewport = new SimpleViewport();
        viewport.setDimensions(0, 0, initialWidth, initialHeight);
        viewport.setScene(main_scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(viewport);

        Layer[] layers = { layer };

        // The texture requires its own set of capabilities.
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(false);
        caps.setPbufferRenderToTexture(true);
        caps.setPbufferFloatingPointBuffers(true);
        caps.setDepthBits(24);
        caps.setAlphaBits(8);

        MRTOffscreenTexture2D off_tex =
            new MRTOffscreenTexture2D(caps, initialWidth, initialHeight, 3, true);

// Use when no depth texture support
//            new MRTOffscreenTexture2D(caps, initialWidth, initialHeight, 4, false);

        off_tex.setClearColor(bgColor[0], bgColor[1], bgColor[2], 1);
        off_tex.setRepaintRequired(true);
        off_tex.setLayers(layers, 1);

        viewportManager.addViewTransform(vp_tx);
        viewportManager.addOrientTransform(nav_tx);
        viewportManager.addFullWindowResize(off_tex);
        viewportManager.addFullWindowResize(viewport);

        viewTxList.add(vp_tx);
        viewOrientList.add(nav_tx);
        fullWindowResizeList.add(off_tex);
        fullEnvResizeList.add(viewport);

        return off_tex;
    }

    /**
     * Creates the antialias pass output texture from the input texture
     */
    private MRTOffscreenTexture2D createSSAOTexture(Matrix4f viewPos,
                                                    float[] bgColor,
                                                    Node sceneContent,
                                                    int initialWidth,
                                                    int initialHeight) {

        MRTOffscreenTexture2D gbuffer_tex =
            createSSAOGBufferTexture(viewPos,
                                     bgColor,
                                     sceneContent,
                                     initialWidth,
                                     initialHeight);

        XNodeFactory node_fac = XNodeFactory.getInstance();
        XNode tex_node = node_fac.get("ImageTexture");

        URL modelURL = null;
        try {
            Object[] file = fileLookup.getFileURL(RANDOM_MAP_FILE, false);
            modelURL = (URL)file[0];
        } catch (IOException ioe) {
            System.out.println("Could not load: " + RANDOM_MAP_FILE);
        }

        String[] urls = { modelURL.toExternalForm() };

        tex_node.addFieldData("url", urls);

        av3dNodeFactory.setBaseURL(modelURL);

        TextureUnit random_tex = av3dNodeFactory.getTextureUnit(tex_node);

        TextureUnit[] tex_unit = {
            new TextureUnit(),
            random_tex,
            new TextureUnit()
        };

        tex_unit[0].setTexture(gbuffer_tex);
        tex_unit[2].setTexture(gbuffer_tex.getDepthRenderTarget());

        String[] vert_shader_txt = loadShaderFile(SSAO_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(SSAO_FRAG_SHADER_FILE);

        ShaderObject vert_shader = new ShaderObject(true);
        vert_shader.setSourceStrings(vert_shader_txt, 1);
        vert_shader.requestInfoLog();
        vert_shader.compile();

        ShaderObject frag_shader = new ShaderObject(false);
        frag_shader.setSourceStrings(frag_shader_txt, 1);
        frag_shader.requestInfoLog();
        frag_shader.compile();

        ShaderProgram shader_prog = new ShaderProgram();
        shader_prog.addShaderObject(vert_shader);
        shader_prog.addShaderObject(frag_shader);
        shader_prog.requestInfoLog();
        shader_prog.link();

        float[] strength = { SSAO_LOCAL_STRENGTH };
        float[] global_strength = { SSAO_GLOBAL_STRENGTH };
        float[] falloff = { SSAO_FALLOFF };
        float[] offset = { SSAO_SAMPLE_OFFSET };
        float[] sample_radius = { SSAO_SAMPLE_RADIUS };

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniform("globalStrength", 1, global_strength, 1);
        shader_args.setUniform("strength", 1, strength, 1);
        shader_args.setUniform("offset", 1, offset, 1);
        shader_args.setUniform("falloff", 1, falloff, 1);
        shader_args.setUniform("sampleRadius", 1, sample_radius, 1);
        shader_args.setUniformSampler("normalMap", 0);
        shader_args.setUniformSampler("randomNoiseMap", 1);
        shader_args.setUniformSampler("depthMap", 2);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(shader_prog);
        shader.setShaderArguments(shader_args);

        Appearance app = new Appearance();
        app.setTextureUnits(tex_unit, 3);
        app.setShader(shader);

        Shape3D shape = new Shape3D();
        shape.setGeometry(baseQuad);
        shape.setAppearance(app);

        Viewpoint vp = new Viewpoint();

        Group root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(shape);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);

        ViewEnvironment env = main_scene.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);

        SimpleViewport viewport = new SimpleViewport();
        viewport.setDimensions(0, 0, initialWidth / 2, initialHeight / 2);
        viewport.setScene(main_scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(viewport);

        Layer[] layers = { layer };

        // The texture requires its own set of capabilities.
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(false);
        caps.setPbufferRenderToTexture(true);
        caps.setPbufferFloatingPointBuffers(true);

        MRTOffscreenTexture2D off_tex =
            new MRTOffscreenTexture2D(caps, initialWidth / 2, initialHeight / 2, 1);

        off_tex.setClearColor(0, 0, 0, 1);
        off_tex.setRepaintRequired(true);
        off_tex.setLayers(layers, 1);

        viewportManager.addHalfWindowResize(off_tex);
        viewportManager.addHalfWindowResize(viewport);

        halfWindowResizeList.add(off_tex);
        halfEnvResizeList.add(viewport);

        return off_tex;
    }

    /**
     * Create the contents of the offscreen texture that is being rendered
     */
    private MRTOffscreenTexture2D createSSAOGBufferTexture(Matrix4f viewPos,
                                                           float[] bgColor,
                                                           Node sceneContent,
                                                           int initialWidth,
                                                           int initialHeight) {

        Viewpoint vp = new Viewpoint();

        Vector3f translation = new Vector3f(0, 1.5f, 10);
        Matrix4f mtx = new Matrix4f();
        mtx.setIdentity();
        mtx.setTranslation(translation);

        TransformGroup vp_tx = new TransformGroup();
        vp_tx.setTransform(mtx);
        vp_tx.addChild(vp);

        TransformGroup nav_tx = new TransformGroup();
        nav_tx.setTransform(viewPos);
        nav_tx.addChild(vp_tx);

        // Create the gbuffer shader
        String[] vert_shader_txt = loadShaderFile(SSAO_GBUFFER_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(SSAO_GBUFFER_FRAG_SHADER_FILE);

        ShaderObject vert_shader = new ShaderObject(true);
        vert_shader.setSourceStrings(vert_shader_txt, 1);
        vert_shader.requestInfoLog();
        vert_shader.compile();

        ShaderObject frag_shader = new ShaderObject(false);
        frag_shader.setSourceStrings(frag_shader_txt, 1);
        frag_shader.requestInfoLog();
        frag_shader.compile();

        ShaderProgram shader_prog = new ShaderProgram();
        shader_prog.addShaderObject(vert_shader);
        shader_prog.addShaderObject(frag_shader);
        shader_prog.requestInfoLog();
        shader_prog.link();

        float[] depth_scale = { 1.0f / 3000.0f };

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniform("depthScale", 1, depth_scale, 1);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(shader_prog);
        shader.setShaderArguments(shader_args);

        Appearance app = new Appearance();
        app.setShader(shader);

        AppearanceOverride app_ovr = new AppearanceOverride();
        app_ovr.setEnabled(true);
        app_ovr.setAppearance(app);

        ColorBackground bg = new ColorBackground();
        bg.setColor(bgColor);

        Group root_grp = new Group();
        root_grp.addChild(app_ovr);
        root_grp.addChild(nav_tx);
        root_grp.addChild(bg);
        root_grp.addChild(sceneContent);

        viewportManager.addBackground(bg);
        backgroundList.add(bg);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);
        main_scene.setActiveBackground(bg);

        ViewEnvironment env = main_scene.getViewEnvironment();
        env.setFarClipDistance(1000);

        viewportManager.addPerspectiveView(env);
        perspectiveResizeList.add(env);

        // Then the basic layer and viewport at the top:
        SimpleViewport viewport = new SimpleViewport();
        viewport.setDimensions(0, 0, initialWidth / 2, initialHeight / 2);
        viewport.setScene(main_scene);

        SimpleLayer layer = new SimpleLayer();
        layer.setViewport(viewport);

        Layer[] layers = { layer };

        // The texture requires its own set of capabilities.
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(false);
        caps.setPbufferRenderToTexture(true);
        caps.setPbufferFloatingPointBuffers(true);
        caps.setDepthBits(24);
        caps.setAlphaBits(8);

        MRTOffscreenTexture2D off_tex =
            new MRTOffscreenTexture2D(caps,
                                      true,
                                      initialWidth / 2,
                                      initialHeight / 2,
                                      1,
                                      true);

        off_tex.setClearColor(bgColor[0], bgColor[1], bgColor[2], 1);
        off_tex.setRepaintRequired(true);
        off_tex.setLayers(layers, 1);

        viewportManager.addViewTransform(vp_tx);
        viewportManager.addOrientTransform(nav_tx);
        viewportManager.addHalfWindowResize(off_tex);
        viewportManager.addHalfWindowResize(viewport);

        viewTxList.add(vp_tx);
        viewOrientList.add(nav_tx);
        halfWindowResizeList.add(off_tex);
        halfEnvResizeList.add(viewport);

        return off_tex;
    }

    /**
     * Create a render pass for a single light at the given position.
     *
     * @param lightPos The position of the light
     * @param radius The light radius to use
     * @return The render pass representing this light or null if there is
     *    no effect to be rendered for this combo
     */
    private LightRenderPassDetails
        createLightPass(float[] lightPos,
                        float[] lightColour,
                        float[] lightAttenuation,
                        float radius,
                        SharedNode sceneGeom,
                        MRTOffscreenTexture2D gbuffer) {

        if(lightingShader == null) {
            // Create the gbuffer shader
            String[] vert_shader_txt = loadShaderFile(LIGHT_VTX_SHADER_FILE);
            String[] frag_shader_txt = loadShaderFile(LIGHT_FRAG_SHADER_FILE);

            ShaderObject vert_shader = new ShaderObject(true);
            vert_shader.setSourceStrings(vert_shader_txt, 1);
            vert_shader.requestInfoLog();
            vert_shader.compile();

            ShaderObject frag_shader = new ShaderObject(false);
            frag_shader.setSourceStrings(frag_shader_txt, 1);
            frag_shader.requestInfoLog();
            frag_shader.compile();

            lightingShader = new ShaderProgram();
            lightingShader.addShaderObject(vert_shader);
            lightingShader.addShaderObject(frag_shader);
            lightingShader.bindAttributeName("viewCoords", 5);
            lightingShader.requestInfoLog();
            lightingShader.link();
        }


        // Global appearance overrride for the light rendering pass
        float near_clip = (float)mainSceneEnv.getNearClipDistance();
        float far_clip = (float)mainSceneEnv.getFarClipDistance();
//        float[] l_rad = { radius };

        float[] d_planes = {
            -far_clip / (far_clip - near_clip),
            -far_clip * near_clip / (far_clip - near_clip)
        };

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniform("lightPos", 3, lightPos, 1);
//        shader_args.setUniform("lightRadius", 1, l_rad, 1);
        shader_args.setUniform("lightColor", 3, lightColour, 1);
        shader_args.setUniform("attenuation", 3, lightAttenuation, 1);
        shader_args.setUniform("planes", 2, d_planes, 1);

        shader_args.setUniformSampler("diffuseMap", 0);
        shader_args.setUniformSampler("normalMap", 1);
        shader_args.setUniformSampler("specularMap", 2);
        shader_args.setUniformSampler("depthMap", 3);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(lightingShader);
        shader.setShaderArguments(shader_args);

        TextureUnit[] tex_unit = new TextureUnit[4];
        tex_unit[0] = new TextureUnit();
        tex_unit[0].setTexture(gbuffer);

        tex_unit[1] = new TextureUnit();
        tex_unit[1].setTexture(gbuffer.getRenderTarget(1));

        tex_unit[2] = new TextureUnit();
        tex_unit[2].setTexture(gbuffer.getRenderTarget(2));

        tex_unit[3] = new TextureUnit();
        tex_unit[3].setTexture(gbuffer.getDepthRenderTarget());

// Use when no depth texture support
//        tex_unit[3].setTexture(gbuffer.getRenderTarget(3));

        Appearance global_app = new Appearance();
        global_app.setShader(shader);
        global_app.setTextureUnits(tex_unit, 4);

        AppearanceOverride app_ovr = new AppearanceOverride();
        app_ovr.setEnabled(true);
        app_ovr.setAppearance(global_app);

        Viewpoint vp = new Viewpoint();

        Group root_grp = new Group();
        root_grp.addChild(vp);
        root_grp.addChild(sceneGeom);
        root_grp.addChild(app_ovr);

        GeneralBufferState gbs = new GeneralBufferState();
        gbs.enableBlending(true);
        gbs.setSourceBlendFactor(GeneralBufferState.BLEND_ONE);
        gbs.setDestinationBlendFactor(GeneralBufferState.BLEND_ONE);

        DepthBufferState dbs = new DepthBufferState();
        dbs.setClearBufferState(false);
        dbs.enableDepthTest(false);
        dbs.enableDepthWrite(false);
        dbs.setDepthFunction(DepthBufferState.FUNCTION_LESS_OR_EQUAL);

        ColorBufferState cbs = new ColorBufferState();
        cbs.setClearBufferState(false);

        RenderPass light_pass = new RenderPass();
        light_pass.setGeneralBufferState(gbs);
        light_pass.setDepthBufferState(dbs);
        light_pass.setColorBufferState(cbs);
        light_pass.setRenderedGeometry(root_grp);
        light_pass.setActiveView(vp);

        ViewEnvironment env = light_pass.getViewEnvironment();
        env.setProjectionType(ViewEnvironment.ORTHOGRAPHIC_PROJECTION);
        env.setClipDistance(-1, 1);
        env.setOrthoParams(-1, 1, -1, 1);

        viewportManager.addOrthoView(env);
        orthoResizeList.add(env);


        return new LightRenderPassDetails(light_pass, radius, lightPos);
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
                                 int[] scissor) {

        int[] rect = { 0, 0, sx, sy };
        float r2 = radius * radius;

        Vector3f l2 = new Vector3f();
        l2.x = lightPos.x * lightPos.x;
        l2.y = lightPos.y * lightPos.y;
        l2.z = lightPos.z * lightPos.z;

        float e1 = 1.2f;
        float e2 = 1.2f * (float)mainSceneEnv.getAspectRatio();

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

    /**
     * Create the shared quad definition that most geometry uses
     */
    private void createCommonQuad() {
        float[] quad_coords = {
            -1, -1, -0.5f,
             1, -1, -0.5f,
             1,  1, -0.5f,
            -1,  1, -0.5f
        };

        float[] quad_normals = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
        float[][] tex_coord = { { 0, 0,  1, 0,  1, 1,  0, 1 } };

        int[] tex_type = { VertexGeometry.TEXTURE_COORDINATE_2 };

        QuadArray real_geom = new QuadArray();
        real_geom.setValidVertexCount(4);
        real_geom.setVertices(QuadArray.COORDINATE_3, quad_coords);
        real_geom.setNormals(quad_normals);
        real_geom.setTextureCoordinates(tex_type, tex_coord, 1);

        baseQuad = real_geom;

        viewportManager.addTexturedQuad(baseQuad);
        texturedQuadList.add(baseQuad);
    }

    /**
     * Load the shader file. Find it relative to the classpath.
     *
     * @param file THe name of the file to load
     */
    private String[] loadShaderFile(String name) {

        String ret_val = null;
        URL url = null;

        try {
            Object[] file = fileLookup.getFileURL(name, true);

            url = (URL)file[0];
            InputStreamReader isr = new InputStreamReader((InputStream)file[1]);
            StringBuffer buf = new StringBuffer();
            char[] read_buf = new char[1024];
            int num_read = 0;

            while((num_read = isr.read(read_buf, 0, 1024)) != -1)
                buf.append(read_buf, 0, num_read);

            isr.close();

            ret_val = buf.toString();

        } catch(IOException ioe) {
// TODO: Internationalise me!
            String msg = "I/O error reading shader file for URL " + url;
            errorReporter.warningReport(msg, null);
        }

        return new String[] { ret_val };
    }
}
