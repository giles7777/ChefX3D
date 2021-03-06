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
import java.awt.*;
import java.awt.event.*;

import org.j3d.aviatrix3d.*;
import org.j3d.aviatrix3d.pipeline.graphics.*;

import javax.media.opengl.GLCapabilities;

import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.j3d.aviatrix3d.output.graphics.SimpleAWTSurface;
import org.j3d.aviatrix3d.pipeline.OutputDevice;

import org.j3d.aviatrix3d.management.MultiThreadRenderManager;
import org.j3d.aviatrix3d.management.RenderManager;
import org.j3d.aviatrix3d.management.SingleDisplayCollection;
import org.j3d.aviatrix3d.management.SingleThreadRenderManager;

import org.j3d.renderer.aviatrix3d.util.SystemPerformanceListener;

import org.j3d.util.I18nManager;

// Local Imports
import org.chefx3d.model.CommandController;
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.SimpleTool;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.DeviceManager;
import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * A non-editable perspective view of the editor scene graph.
 * Provides interactive navigation, but no editing functions.
 *
 * @author Rex Melton
 * @version $Revision: 1.31 $
 */
public class Preview extends JPanel implements
    AV3DConstants,
    SurfaceInfoListener,
    SystemPerformanceListener,
    Thumbnailable,
    Runnable {

	/** Full color render mode identifier */
	public static final int RENDER_FULL_COLOR = 0;

	/** Line art render mode identifier */
	public static final int RENDER_LINE_ART = 1;

    /** Identifier of this */
    private static final String VIEW_ID = "Preview";

    /** Error message if openGL cannot be loaded */
    private static final String OPENGL_INIT_FAILED_PROP =
        "org.chefx3d.view.awt.av3d.AV3DView.openGLInitFailedMsg";

    /** GL Version prefix string */
    private static final String GL_VERSION_PROP =
        "org.chefx3d.view.awt.av3d.Preview.glVersionPrefix";

    /** GL vendor name prefix string */
    private static final String GL_VENDOR_PROP =
        "org.chefx3d.view.awt.av3d.Preview.glVendorPrefix";

    /** Message when we are delibrately forcing low quality rendering */
    private static final String LOW_QUALITY_REND_PROP =
        "org.chefx3d.view.awt.av3d.Preview.lowQualityRenderingOnlyMsg";

    /** Default framecycle interval for the preview in milliseconds per frame */
    private static final int DEFAULT_FPS = 10;

    /** Clamped framecycle interval for when we've reached maximum visuals */
    private static final int CLAMPED_FPS = 30;

    /** The default initial viewport dimensions for the location layer */
    private static final int[] PREVIEW_VIEWPORT_DIMENSION =
        new int[]{0, 0, 100, 100};

    /** Default number of antialiasing samples. */
    private static final int DEFAULT_ANTIALIASING_SAMPLES = 1;

    /** Default max number of antialiasing samples. */
    private static final int DEFAULT_MAX_ANTIALIASING_SAMPLES = 8;

    /** Antialiasing samples param key */
    private static final String ANTIALIASING_SAMPLES =
        "previewAntialiasingSamples";

    /** The current number of antialiasing samples. */
    private int antialiasingSamples = 1;

    /** The current maximum number of antialiasing samples. */
    private int maxAntialiasingSamples = 1;

    /** I18N manager for sourcing messages */
    private I18nManager i18Mgr;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** Manager for the scene graph handling */
    private RenderManager renderManager;

    /** Manager for the layers etc */
    private SingleDisplayCollection displayManager;

    /** Our drawing surface */
    private GraphicsOutputDevice graphicsSurface;

    /**
     * The component to use for the graphics surface. May end up being
     * the warning label if JOGL fails to initialise properly so always
     * use this in preference to the graphicsSurface.getSurfaceObject().
     */
    private Component graphicsComponent;

    /** graphics pipeline */
    private DefaultGraphicsPipeline graphicsPipeline;

    /** The scene manager Observer*/
    private SceneManagerObserver mgmtObserver;

    /** The world model */
    private WorldModel model;

    /** flag indicating that the viewport has been resized */
    private boolean resizeOccured;

    /** Preview layer manager */
    private PreviewLayerManager preview;

    /** Preview legend layer manager */
    private PreviewLegendLayerManager legend;

    /** The filter to use for url requests, null use baseURL logic instead */
    private URLFilter urlFilter;

    /** flag to prevent initialization from happening twice */
    private boolean initialized;

    /** Flag indicating that the fixed pipeline is required */
    private boolean useFixedPipeline;

    /** Flag indicating the state of the performance monitor */
    private boolean monitorEnabled;

    /** GLInfo object */
    private GLInfo glinfo;

    /** Is the framerate clamped? */
    private boolean framerateClamped;

    /** Reference to this class to be passed to a thread */
    private SystemPerformanceListener spl;

	/** Flag indicating the current render mode */
	private int currentRenderMode;

    /**
     * Constructor
     *
     * @param window The Window object that is our prime ancestor
     * @param model The WorldModel object
     * @param renderManager The render manager that we're adding ourselves to
     * @param sceneMgr The scene manager for updates
     * @param urlFilter The filter to use for url requests
     * @param glinfo A GLInfo object, or null if none has been obtained yet.
     */
    public Preview(
        Window window,
        WorldModel model,
        RenderManager renderManager,
        SceneManagerObserver sceneMgr,
        ErrorReporter reporter,
        URLFilter urlFilter,
        GLInfo glinfo) {

        super(new BorderLayout());

        setErrorReporter(reporter);

        this.renderManager = renderManager;
        mgmtObserver = sceneMgr;

        this.model = model;
        this.urlFilter = urlFilter;

        i18Mgr = I18nManager.getManager();

        antialiasingSamples = DEFAULT_ANTIALIASING_SAMPLES;
        Object antialiasingSamplesValue_object =
            ApplicationParams.get(ANTIALIASING_SAMPLES);
        if ((antialiasingSamplesValue_object != null) &&
            (antialiasingSamplesValue_object instanceof Integer)) {

            int tmp_value = ((Integer)antialiasingSamplesValue_object).intValue();
            if (tmp_value > 1) {
                antialiasingSamples = tmp_value;
            }
        }

        this.glinfo = glinfo;
        if (glinfo == null) {
            // start the thread to determine the max antialiasing samples
            Thread thread = new Thread(this);
            thread.start();
        } else {
            int max_supported = glinfo.getMaximumNumSamples();

            maxAntialiasingSamples = (max_supported > DEFAULT_MAX_ANTIALIASING_SAMPLES) ?
                DEFAULT_MAX_ANTIALIASING_SAMPLES : max_supported;
        }
		////////////////////////////////////////////////////////////////////
		// rem: pushing the glinfo object to the loader here is a shortcut.
		// it probably should be set through the constructor, or made
		// available to the loader from some static source. But... the
		// glinfo object is instantiated in the yumetech common package,
		// and is set throught this constructor to the av3d package.
		// so..... this was quick and easy.
		AV3DLoader.setGLInfo(glinfo);
		////////////////////////////////////////////////////////////////////

        setupAviatrix();
        setupSceneGraph();

        Object prop = ApplicationParams.get("enableHQRendering");
        if ((prop != null) && (prop instanceof Boolean)) {
            useFixedPipeline = ((Boolean)prop).booleanValue();
        }

        if (!useFixedPipeline) {

            // wait 10 seconds before monitoring performance to allow the
            // application to fully initialize
            spl = this;
            Thread t = new Thread() {
                public void run() {

                    try {
                        Thread.sleep(10000);
                    } catch (Exception ex) {}

                    mgmtObserver.addPerformanceListener(spl, 1);
                    monitorEnabled = true;

                }
            };

            t.start();

        }

        add(graphicsComponent, BorderLayout.CENTER);

        framerateClamped = false;
		currentRenderMode = RENDER_FULL_COLOR;
    }

    //----------------------------------------------------------
    // Methods defined by Component
    //----------------------------------------------------------

    /**
     * Notification that the panel now has a native component. Use this
     * to start the render manager.
     */
    @Override
    public void addNotify() {
        super.addNotify();

        NavigationManager nm = preview.getNavigationManager();
        nm.setNavigationMode(NavigationMode.EXAMINE);

        renderManager.addDisplay(displayManager);
        mgmtObserver.addUIObserver(preview);
        preview.refreshView();

        this.revalidate();
    }

    /**
     * Notification that this is destined to be removed from it's parent
     * component. Shutdown the render manager.
     */
    @Override
    public void removeNotify() {
        renderManager.removeDisplay(displayManager);
        mgmtObserver.removeUIObserver(preview);

        super.removeNotify();
    }

    /**
     *
     */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    //---------------------------------------------------------------
    // Methods defined by SurfaceInfoListener
    //---------------------------------------------------------------

    /**
     * Notification that the graphics output device has changed GL context
     * and this is the collection of new information.
     *
     * @param surface The output surface that caused the new info
     * @param info The collected set of information known
     */
    public void surfaceInfoChanged(OutputDevice surface, SurfaceInfo info) {

        boolean mustUseFFRendering =
            !ShaderUtils.checkForShaderAvailability(info);

        String glVersionPrefix = i18Mgr.getString(GL_VERSION_PROP);
        String glVendorPrefix = i18Mgr.getString(GL_VENDOR_PROP);

        StringBuilder bldr = new StringBuilder(glVersionPrefix);

        Locale lcl = i18Mgr.getFoundLocale();
        NumberFormat nformater = NumberFormat.getNumberInstance(lcl);

        float version = info.getGLMajorVersion() +
                        info.getGLMinorVersion() * 0.1f;

        bldr.append(nformater.format(version));

        errorReporter.messageReport(bldr.toString());
        errorReporter.messageReport(glVendorPrefix + info.getVendorString());

        if(mustUseFFRendering) {
            String msg = i18Mgr.getString(LOW_QUALITY_REND_PROP);
            errorReporter.messageReport(msg);
        } else {
            // Start somewhere in the middle
        }

        preview.forceFixedFunctionRendering(mustUseFFRendering);
    }

    //----------------------------------------------------------
    // Methods defined by SystemPerformanceListener
    //----------------------------------------------------------

    /**
     * Notification of a performance downgrade is required in the system.
     * This listener should attempt to reduce it's performance demands now if
     * it is able to and return true. If not able to reduce it the return
     * false.
     *
     * @return True if the performance demands were decreased
     */
    public boolean downgradePerformance() {
        if (monitorEnabled) {

            if (framerateClamped) {
                renderManager.setMinimumFrameInterval(DEFAULT_FPS);
                framerateClamped = false;
            }

            if (antialiasingSamples > 1) {
                if (!((DefaultNavigationManager)preview.getNavigationManager()).isActive()) {
                    antialiasingSamples /= 2;
                    changeSurface();
                    return(true);
                } else {
                    return(false);
                }
            } else {
                return(false);
            }
        } else {
            return(false);
        }
    }

    /**
     * Notification of a performance upgrade is required by the system. This
     * listener is free to increase performance demands of the system. If it
     * does upgrade, return true, otherwise return false.
     *
     * @return True if the performance demands were increased
     */
    public boolean upgradePerformance() {
        if (monitorEnabled) {
            if (antialiasingSamples < maxAntialiasingSamples) {
                if (!((DefaultNavigationManager)preview.getNavigationManager()).isActive()) {
                    antialiasingSamples *= 2;

                    changeSurface();
                    return(true);
                } else {
                    return(false);
                }
            } else {

                // Now clamp framete
                renderManager.setMinimumFrameInterval(CLAMPED_FPS);

                return(false);
            }
        } else {
            return(false);
        }
    }

    //----------------------------------------------------------
    // Methods defined by Thumbnailable
    //----------------------------------------------------------

    /**
     * @return a new ThumbnailData object
     */
    public ThumbnailData getThumbnailData() {
        return new ThumbnailData(null,
                                 this,
                                 preview.getRootScene(),
                                 mgmtObserver,
                                 mgmtObserver.getCommandController(),
                                 model);
    }

    //----------------------------------------------------------
    // Methods defined by Runnable
    //----------------------------------------------------------

    /**
     * Thread for getting max number of samples
     */
    public void run() {
        glinfo = new GLInfo();
        int max_supported = glinfo.getMaximumNumSamples();
        maxAntialiasingSamples = (max_supported > DEFAULT_MAX_ANTIALIASING_SAMPLES) ?
            DEFAULT_MAX_ANTIALIASING_SAMPLES : max_supported;
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

	/**
	 * Set the rendering mode
	 *
	 * @param mode The mode
	 */
	public void setRenderingMode(int mode) {
		if (mode == RENDER_FULL_COLOR) {
			currentRenderMode = RENDER_FULL_COLOR;
			preview.enableLineArtRendering(false);
		} else if (mode == RENDER_LINE_ART) {
			currentRenderMode = RENDER_LINE_ART;
			preview.enableLineArtRendering(true);
		}
	}

	/**
	 * Return the current rendering mode
	 *
	 * @return The mode
	 */
	public int getRenderingMode() {
		return(currentRenderMode);
	}

	/**
	 * Return whether the rendering mode is supported
	 *
	 * @param mode The mode to check
	 * @return true if the mode is available, false otherwise
	 */
	public boolean isRenderingModeSupported(int mode) {
		boolean supported = true;
		if (mode == RENDER_FULL_COLOR) {
			supported = true;
		} else if (mode == RENDER_LINE_ART) {
			supported = preview.isLineArtRenderingSupported();
		} else {
			supported = false;
		}
		return(supported);
	}

    /**
     * Clean up any references
     */
    public void shutdown() {
        renderManager.disableInternalShutdown();
        renderManager.shutdown();
        mgmtObserver.appShutdown();
        graphicsSurface.dispose();
    }

    /**
     * Return the identifier string of this
     *
     * @return The identifier string of this
     */
    public String getIdentifier() {
        return(VIEW_ID);
    }

    /**
     * Return the SceneManagerObserver
     *
     * @return The SceneManagerObserver
     */
    public SceneManagerObserver getSceneManagerObserver() {
        return(mgmtObserver);
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
        if (preview != null) {
            preview.setErrorReporter(errorReporter);
        }
    }

    /**
     * Return the NavigationManager
     *
     * @return The NavigationManager
     */
    public NavigationManager getNavigationManager() {
        return(preview.getNavigationManager());
    }

    /**
     * Return the LayerManager
     *
     * @return The LayerManager
     */
    public LayerManager getLayerManager() {
        return(preview);
    }

    /**
     * Return the legend LayerManager
     *
     * @return The legend LayerManager
     */
    public LayerManager getLegendLayerManager() {
        return(legend);
    }

    /**
     * Change the performance monitoring state
     *
     * @param state true to turn monitoring on, false to turn off
     */
    void enablePerformanceMonitor(boolean state) {
        ////////////////////////////////////////////////////////////////////
        // rem: this is probably what we should do.....
        // but the aviatrix performance monitor is broken for
        // removing the listener, so we just 'disable' locally
        // with a flag
        //if (state) {
        //  mgmtObserver.addPerformanceListener(this, PERFORMANCE_PRIORITY);
        //} else {
        //  mgmtObserver.removePerformanceListener(this);
        //}
        ////////////////////////////////////////////////////////////////////
        monitorEnabled = state;
    }

    /**
     * Setup the aviatrix pipeline
     */
    private void setupAviatrix() {

        // Only build a scene manager if we don't have one given to us
        // to share.
        if(renderManager == null) {
            // Assemble a simple single-threaded pipeline.
            renderManager = new SingleThreadRenderManager();
            renderManager.setMinimumFrameInterval(DEFAULT_FPS);
        }

        GraphicsCullStage culler = new FrustumCullStage();
        culler.setOffscreenCheckEnabled(true);

        GraphicsSortStage sorter = new StateAndTransparencyDepthSortStage();
        try {
            GLCapabilities caps = new GLCapabilities();
            caps.setDoubleBuffered(true);
            caps.setHardwareAccelerated(true);

            if (antialiasingSamples > 1) {
                caps.setSampleBuffers(true);
                caps.setNumSamples(antialiasingSamples);
            }

//            graphicsSurface = new SimpleAWTSurface(caps);
            graphicsSurface = new org.j3d.aviatrix3d.output.graphics.DebugAWTSurface(caps);
        } catch(UnsatisfiedLinkError usl) {
            String msg = i18Mgr.getString(OPENGL_INIT_FAILED_PROP);
            graphicsComponent = new JLabel(msg);
            return;
        }

        graphicsSurface.addSurfaceInfoListener(this);

        graphicsComponent = (Component)graphicsSurface.getSurfaceObject();

        graphicsPipeline = new DefaultGraphicsPipeline();
        graphicsPipeline.setCuller(culler);
        graphicsPipeline.setSorter(sorter);
        graphicsPipeline.setGraphicsOutputDevice(graphicsSurface);

        displayManager = new SingleDisplayCollection();
        displayManager.addPipeline(graphicsPipeline);
    }

    /**
     * Setup the basic scene
     */
    private void setupSceneGraph() {

        DeviceManager deviceManager = mgmtObserver.getDeviceManager();
		NavigationStatusManager navStatusManager = new NavigationStatusManager();

        preview = new PreviewLayerManager(
            0,
            PREVIEW_VIEWPORT_DIMENSION,
            VIEW_ID,
            model,
            mgmtObserver.getCommandController(),
            errorReporter,
            mgmtObserver,
            deviceManager,
			navStatusManager,
            urlFilter,
			glinfo);

		legend = new PreviewLegendLayerManager(
        	1,
        	PREVIEW_VIEWPORT_DIMENSION,
        	model,
        	errorReporter,
        	mgmtObserver,
        	navStatusManager);

        Layer[] layers = { preview.getLayer(), legend.getLayer() };

        displayManager.setLayers(layers, layers.length);

        deviceManager.addTrackedSurface(
			graphicsSurface,
            preview.getUserInputHandler());

        graphicsSurface.addGraphicsResizeListener(preview);
        graphicsSurface.addGraphicsResizeListener(legend);

        renderManager.setApplicationObserver(mgmtObserver);
    }

    /**
     * Replace the graphics surface. Happens as a result of performance
     * monitoring determining that the frame rate can be increased, or
     * should be decreased to maintain interactivity. The replacement
     * will have more (or less) antialiasing samples.
     */
    private void changeSurface() {

        // disable
        renderManager.removeDisplay(displayManager);
        mgmtObserver.removeUIObserver(preview);

        // clear out the old
        this.remove(graphicsComponent);

        graphicsSurface.removeSurfaceInfoListener(this);

        //deviceManager.removeTrackedSurface(
        //  graphicsSurface,
        //    preview.getUserInputHandler());

        graphicsSurface.removeGraphicsResizeListener(preview);
        graphicsSurface.removeGraphicsResizeListener(legend);

        // create the new
        GLCapabilities caps = new GLCapabilities();
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);

        if (antialiasingSamples > 1) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(antialiasingSamples);
        }

        graphicsSurface = new SimpleAWTSurface(caps);

        graphicsSurface.addSurfaceInfoListener(this);
        graphicsSurface.addGraphicsResizeListener(preview);
        graphicsSurface.addGraphicsResizeListener(legend);

        DeviceManager deviceManager = mgmtObserver.getDeviceManager();
        deviceManager.addTrackedSurface(
            graphicsSurface,
            preview.getUserInputHandler());

        graphicsPipeline.setGraphicsOutputDevice(graphicsSurface);

        graphicsComponent = (Component)graphicsSurface.getSurfaceObject();
        this.add(graphicsComponent, BorderLayout.CENTER);

        this.revalidate();

        // re-enable
        renderManager.addDisplay(displayManager);
        mgmtObserver.addUIObserver(preview);

        ((DefaultNavigationManager)preview.getNavigationManager()).refresh();
    }
}

