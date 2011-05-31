/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;

import javax.media.opengl.GLCapabilities;

import javax.vecmath.*;

import org.ietf.uri.URL;

import org.j3d.aviatrix3d.*;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

import org.chefx3d.view.awt.scenemanager.*;

/**
 * Manager for the line art shading implementation.
 *
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
class FaxQShadingManager implements PerFrameObserver {
	
	/** Local debug */
	private static final boolean DEBUG = true;
	
    /** Render pass vertex shader string */
    private static final String MAT_VTX_SHADER_FILE =
        "glsl/lineart/edge_depth_vert.glsl";

    /** Fragment shader file name for the rendering pass */
    private static final String MAT_FRAG_SHADER_FILE =
        "glsl/lineart/edge_depth_frag.glsl";

    /** Render pass vertex shader string */
    private static final String RENDER_VTX_SHADER_FILE =
        "glsl/lineart/edge_render_vert.glsl";

    /** Fragment shader file name for the rendering pass */
    private static final String RENDER_FRAG_SHADER_FILE =
        "glsl/lineart/edge_render_frag.glsl";

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /** The view environment created for the scene */
    private ViewEnvironment mainSceneEnv;

    /** The root scene that this manager handles */
    private SimpleScene rootScene;

    /** File name resolver for each item */
    private FileLoader fileLookup;

    /** Manager of the viewports for resizing */
    private HiQViewportManager viewportManager;

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
	
    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;
	
	int render_shader_cnt;
	ShaderObject render_vert_shader;
    ShaderObject render_frag_shader;
    ShaderProgram render_shader_prog;
	
	int depth_shader_cnt;
	ShaderObject depth_vert_shader;
    ShaderObject depth_frag_shader;
    ShaderProgram depth_shader_prog;
	
	/**
	 * Constructor
	 *
     * @param reporter The ErrorReporter to use.
	 */
	FaxQShadingManager(
		ErrorReporter reporter,
		HiQViewportManager viewportManager,
		SceneManagerObserver mgmtObserver) {
		
		setErrorReporter(reporter);
		this.viewportManager = viewportManager;
        fileLookup = new FileLoader();
		
		this.mgmtObserver = mgmtObserver;
		mgmtObserver.addObserver(this);
		
        viewTxList = new ArrayList<TransformGroup>();
        viewOrientList = new ArrayList<TransformGroup>();
        fullWindowResizeList = new ArrayList<MRTOffscreenTexture2D>();
        fullEnvResizeList = new ArrayList<Viewport>();
        fullShaderArgsResizeList = new ArrayList<ShaderArguments>();
        orthoResizeList = new ArrayList<ViewEnvironment>();
        perspectiveResizeList = new ArrayList<ViewEnvironment>();
        texturedQuadList = new ArrayList<QuadArray>();
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameObserver
	//---------------------------------------------------------------
	
	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrame() {
		if (DEBUG) {
			if (render_shader_cnt > 0) {
				render_shader_cnt--;
				if (render_shader_cnt == 0) {
					System.out.println("render vert log " + render_vert_shader.getLastInfoLog());
					System.out.println("render frag log " + render_frag_shader.getLastInfoLog());
					System.out.println("render link log " + render_shader_prog.getLastInfoLog());
				}
			}
			if (depth_shader_cnt > 0) {
				depth_shader_cnt--;
				if (depth_shader_cnt == 0) {
					System.out.println("depth vert log " + depth_vert_shader.getLastInfoLog());
					System.out.println("depth frag log " + depth_frag_shader.getLastInfoLog());
					System.out.println("depth link log " + depth_shader_prog.getLastInfoLog());
				}
			}
		}
	}
	
    //---------------------------------------------------------------
    // Local methods
    //---------------------------------------------------------------

    /**
     * Shut down this manager now. Make sure to pull all the stuff from
     * the viewport manager that no longer needs to be managed.
     */
	void shutdown() {
		for (int i = 0; i < viewTxList.size(); i++) {
			viewportManager.removeViewTransform(viewTxList.get(i));
		}
		for (int i = 0; i < viewOrientList.size(); i++) {
			viewportManager.removeOrientTransform(viewOrientList.get(i));
		}
		for (int i = 0; i < fullWindowResizeList.size(); i++) {
			viewportManager.removeFullWindowResize(fullWindowResizeList.get(i));
		}
		for (int i = 0; i < fullEnvResizeList.size(); i++) {
			viewportManager.removeFullWindowResize(fullEnvResizeList.get(i));
		}
		for (int i = 0; i < fullShaderArgsResizeList.size(); i++) {
			viewportManager.removeFullWindowResize(fullShaderArgsResizeList.get(i));
		}
		for (int i = 0; i < orthoResizeList.size(); i++) {
			viewportManager.removeOrthoView(orthoResizeList.get(i));
		}
		for (int i = 0; i < perspectiveResizeList.size(); i++) {
			viewportManager.removePerspectiveView(perspectiveResizeList.get(i));
		}
		for (int i = 0; i < texturedQuadList.size(); i++) {
			viewportManager.removeTexturedQuad(texturedQuadList.get(i));
		}
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
     * Get the root scene that this manager handles.
     */
    SimpleScene getContainedScene() {
        return(rootScene);
    }

    /**
     * Get the view environment used to represent the rendered scene. This
     * is not the root view environment, as that is a different object to
     * the one that is off down somewhere in some offscreen buffers.
     *
     * @return The view environment of the real scene
     */
    ViewEnvironment getViewEnvironment() {
        return(mainSceneEnv);
    }
	
    /**
     * After the various flags have been set, construct the internal scene graph
     * and any additional data.
     *
	 * @param sceneContent The 'real' scene content
	 * @param viewMatrix The viewpoint transformation
     * @param initialWidth The starting width in pixels of the render
     * @param initialHeight The starting height in pixels of the render
     */
    void initialize(
		SharedNode sceneContent,
		Matrix4f viewMatrix,
		int initialWidth,
		int initialHeight) {
		
        // quad to draw to
        float[] quad_coords = { -1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0 };
        float[] quad_normals = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
        float[][] tex_coord = { { 0, 0,  1, 0,  1, 1,  0, 1 } };

        int[] tex_type = { VertexGeometry.TEXTURE_COORDINATE_2 };
        float[] ambient_blend = { 1, 1, 1 };

        QuadArray base_quad = new QuadArray();
        base_quad.setValidVertexCount(4);
        base_quad.setVertices(TriangleArray.COORDINATE_3, quad_coords);
        base_quad.setNormals(quad_normals);
        base_quad.setTextureCoordinates(tex_type, tex_coord, 1);
        base_quad.setSingleColor(false, ambient_blend);

        viewportManager.addTexturedQuad(base_quad);
		texturedQuadList.add(base_quad);
		
        MRTOffscreenTexture2D off_tex = createRenderTargetTexture(
			sceneContent,
			viewMatrix,
			initialWidth,
			initialHeight);

		TextureUnit[] tex_unit = { new TextureUnit() };
        tex_unit[0].setTexture(off_tex);

        // Create the depth render shader
        String[] vert_shader_txt = loadShaderFile(RENDER_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(RENDER_FRAG_SHADER_FILE);

        render_vert_shader = new ShaderObject(true);
        render_vert_shader.setSourceStrings(vert_shader_txt, 1);
        render_vert_shader.requestInfoLog();
        render_vert_shader.compile();

        render_frag_shader = new ShaderObject(false);
        render_frag_shader.setSourceStrings(frag_shader_txt, 1);
        render_frag_shader.requestInfoLog();
        render_frag_shader.compile();

        render_shader_prog = new ShaderProgram();
        render_shader_prog.addShaderObject(render_vert_shader);
        render_shader_prog.addShaderObject(render_frag_shader);
        render_shader_prog.requestInfoLog();
        render_shader_prog.link();

        float[] threshold = {1.0f};
        float[] tex_size = {initialWidth, initialHeight};

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniform("weight", 1, threshold, 1);
        shader_args.setUniform("texSize", 2, tex_size, 1);
        shader_args.setUniformSampler("normalMap", 0);

        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(render_shader_prog);
        shader.setShaderArguments(shader_args);

        Appearance app_1 = new Appearance();
        app_1.setShader(shader);
		app_1.setTextureUnits(tex_unit, 1);

        Shape3D shape_1 = new Shape3D();
        shape_1.setGeometry(base_quad);
        shape_1.setAppearance(app_1);

        Viewpoint vp = new Viewpoint();

        Vector3f render_view_pos = new Vector3f(0, 0, 0.9f);
		
        Matrix4f view_mat = new Matrix4f();
        view_mat.setIdentity();
        view_mat.setTranslation(render_view_pos);

        TransformGroup tx = new TransformGroup();
        tx.setTransform(view_mat);
        tx.addChild(vp);

        SharedNode common_geom = new SharedNode();
        common_geom.setChild(shape_1);

        Group root_grp = new Group();
        root_grp.addChild(tx);
        root_grp.addChild(common_geom);

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
		render_shader_cnt = 4;
	}
	
    /**
     * Create the contents of the offscreen texture that is being rendered
     */
    private MRTOffscreenTexture2D createRenderTargetTexture(
		SharedNode sceneContent,
		Matrix4f viewMatrix,
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
        nav_tx.setTransform(viewMatrix);
        nav_tx.addChild(vp_tx);

        // Create the gbuffer shader
        String[] vert_shader_txt = loadShaderFile(MAT_VTX_SHADER_FILE);
        String[] frag_shader_txt = loadShaderFile(MAT_FRAG_SHADER_FILE);

        depth_vert_shader = new ShaderObject(true);
        depth_vert_shader.setSourceStrings(vert_shader_txt, 1);
        depth_vert_shader.requestInfoLog();
        depth_vert_shader.compile();

        depth_frag_shader = new ShaderObject(false);
        depth_frag_shader.setSourceStrings(frag_shader_txt, 1);
        depth_frag_shader.requestInfoLog();
        depth_frag_shader.compile();

        depth_shader_prog = new ShaderProgram();
        depth_shader_prog.addShaderObject(depth_vert_shader);
        depth_shader_prog.addShaderObject(depth_frag_shader);
        depth_shader_prog.bindAttributeName("tangent", 5);
        depth_shader_prog.requestInfoLog();
        depth_shader_prog.link();

        ShaderArguments shader_args = new ShaderArguments();
        shader_args.setUniformSampler("normalMap", 1);
		
        GLSLangShader shader = new GLSLangShader();
        shader.setShaderProgram(depth_shader_prog);
        shader.setShaderArguments(shader_args);

        Appearance global_app = new Appearance();
        global_app.setShader(shader);

        AppearanceOverride app_ovr = new AppearanceOverride();
        app_ovr.setEnabled(true);
        app_ovr.setLocalAppearanceOnly(false);
        app_ovr.setAppearance(global_app);

        Group root_grp = new Group();
        root_grp.addChild(app_ovr);
        root_grp.addChild(nav_tx);
        root_grp.addChild(sceneContent);

        SimpleScene main_scene = new SimpleScene();
        main_scene.setRenderedGeometry(root_grp);
        main_scene.setActiveView(vp);

        mainSceneEnv = main_scene.getViewEnvironment();
        mainSceneEnv.setNearClipDistance(0.5);
        mainSceneEnv.setFarClipDistance(200);
		
		viewportManager.setMainViewEnvironment(mainSceneEnv);
        viewportManager.addPerspectiveView(mainSceneEnv);
        perspectiveResizeList.add(mainSceneEnv);
		
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
        caps.setPbufferFloatingPointBuffers(false);
        caps.setDepthBits(24);

        MRTOffscreenTexture2D off_tex =
            new MRTOffscreenTexture2D(caps, initialWidth, initialHeight, 2, false);

        off_tex.setClearColor(0.5f, 0.5f, 0.5f, 1);
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
		
		depth_shader_cnt = 4;
        return(off_tex);
    }

    /**
     * Load the shader file. Find it relative to the classpath.
     *
     * @param name The name of the file to load
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
