/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.gt2d;

//External imports
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.vecmath.*;

//Explicitly listed due to clash with java.util.List
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Color;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.net.URL;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.j3d.util.ImageLoader;

// Internal imports
import org.chefx3d.model.AddSegmentCommand;
import org.chefx3d.model.AddVertexCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityChildListener;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.EntitySelectionHelper;
import org.chefx3d.model.ModelListener;
import org.chefx3d.model.MultiCommand;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RotateEntityCommand;
import org.chefx3d.model.RotateEntityTransientCommand;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.Selection;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;
import org.chefx3d.util.PropertyTools;
import org.chefx3d.view.View;
import org.chefx3d.view.ViewManager;

import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.DirectPosition2D;

import org.w3c.dom.Element;


public abstract class AbstractView extends JPanel
    implements
     View,
     ModelListener,
     EntityChildListener,
     EntityPropertyListener,
     MouseMotionListener,
     MouseListener,
     MouseWheelListener,
     KeyListener,
     ActionListener,
     ChangeListener,
     Runnable {


    protected static enum MouseMode {NONE, NAVIGATION, PLACEMENT, SELECTION, ASSOCIATE};

    /** The initial nav control location within the map panel */
    protected static final Point NAV_CONTROL_LOCATION = new Point(8, 8);

    /**
     * The distance in pixels away from a vertex that will result in the vertex
     * being picked.
     */
    protected static final int VERTEX_PICK_RADIUS = 10;

    /**
     * The distance in pixels away from the segment that will result in that
     * segment being considered picked.
     */
    protected static final int SEGMENT_PICK_DISTANCE = 6;

    /** MouseWheel sensitivity for rotation changes.  0 = No Change, multiplier */
    protected static final int MOUSEWHEEL_STEPUP = 10;

    /** The minimum size an icon can be in pixels */
    protected static final int ICON_MINIMUM = 36;

    /** The icon scalar value for the iconic values */
    protected static final float ICON_SCALAR = 1.0f / 3.0f;

    /**
     * The minimum time to pass on a zoom call before we recalculate all
     * the entity state and size. If we get another update in less than this
     * delay time, we do not recalculate anything. Time is in milliseconds.
     */
    protected static final int ZOOM_UPDATE_CHECK_DELAY = 300;

    /** Property name for association image */
    protected static final String ASSOCIATION_2D_IMAGE_PROPERTY = "ASSOCIATION2D.image";

    /** Default fly button image */
    protected static final String DEFAULT_ASSOCIATION_IMAGE = "images/2d/associateIcon.png";

    /** Default open hand cursor image file */
    protected static final String DEFAULT_OPEN_HAND_CURSOR_IMAGE = "images/2d/openHandCursor.png";

    /** Default closed hand cursor image file */
    protected static final String DEFAULT_CLOSED_HAND_CURSOR_IMAGE = "images/2d/closedHandCursor.png";


    /** current state for mouse clicks */
    protected MouseMode currentMode;

    /** previous state for mouse clicks */
    protected MouseMode previousMode;

    // Debug variables
    protected boolean showIconCenter;

    /** The world model */
    protected WorldModel model;

    /** A map of image url to cached Image */
    protected HashMap<String, Image> imageMap;

    /** The current tool image */
    protected Image toolImage;

    /** The current tool segment image */
    protected Image toolSegmentImage;

    /** The current tool */
    protected Tool currentTool;

    /** The current tools width in pixels */
    protected int imgWidth;

    /** The current tools length in pixels */
    protected int imgHeight;

    /** Current mouseX */
    protected int mouseX;

    /** Current mouseY */
    protected int mouseY;

    /**
     * The time that the last zoom updated. Value is extracted from
     * System.currentTimeMillis.
     */
    protected long lastZoomUpdateTime;

    /** The entity ID to entity proper */
    protected HashMap<Integer, Entity> entityMap;

    /** The vertexId to the parent entityId */
    protected HashMap<Integer, Integer> vertexMap;


    /** The tool transform */
    protected AffineTransform toolTransform;

    /** The blank cursor */
    protected Cursor blankCursor;

    /** The associate cursor */
    protected Cursor associateCursor;

    /** Open hand cursor, the default navigate mode cursor. */
    protected Cursor openHandCursor;

    /** Closed hand cursor, used when doing a pan drag operation */
    protected Cursor closedHandCursor;

    /** Cross hair cursor, used when doing a select area operation */
    protected Cursor crossHairCursor;

    /** Are we in associate mode */
    protected boolean associateMode;

    /** The valid types for the next associate */
    protected String[] validTools;

    /** The property group the associate property is a part of */
    protected String propertyGroup;

    /** The prroperty name that stores the associate being performed */
    protected String propertyName;

    /** Are we inside the Map area */
    protected boolean insideMap;

    /** The current entities that are selected according to the model */
    protected ArrayList<Entity> selectedEntities;

    /** Is a drag of an entity ongoing */
    protected boolean entityDragging;

    /** Is a rotation of an entity ongoing */
    protected boolean entityRotating;

    /** If x is a factor of y then swap */
    protected boolean swap;

    /** The current icon scaleX */
    protected float iconScaleX;

    /** The current icon scaleY */
    protected float iconScaleY;

    /** The current icon centerX */
    protected int iconCenterX;

    /** The current icon centerY */
    protected int iconCenterY;

    /** The directory to load images from */
    protected String imgDir;

    /** Is the shift key active currently */
    protected boolean shiftActive;

    /** Are we in a fixed segment authoring mode */
    //private boolean fixedMode;

    /** Should we ignore a drag motion.  Ignores till next mouse release */
    protected boolean ignoreDrag;

    /** What is the fixed segment length */
    protected float segmentLength;

    /** Scratch coordinate - the last segment position in screen space */
    protected int[] lastSegmentPosition;

    /** Are we in a multi-segment operation */
    protected boolean multiSegmentOp;

    /** The current highlighted vertexID */
    //private int highlightedVertexID;

    /** The image size */
    protected Dimension imageSize;

    /** What is the active button */
    protected int activeButton;

    /** How are helper objects displayed */
    protected int helperMode;

    /** A scratch screen position */
    protected int[] screenPos;

    /** Scratch position */
    protected double[] tmpPos;

    /** A scratch rotation */
    protected float[] tmpRot;

    /** A scratch scale */
    protected float[] tmpScale;

    /** A scratch center in pixels */
    protected int[] tmpCenter;

    /** The unique viewID */
    protected long viewID;

    /** The starting position of entity for transient actions */
    protected double[] startPos;
    protected double[] startPos2;

    /** The starting rotation of the entity */
    protected float[] startRot;

    /** The current transactionID for transient commands.  Assume only one can be active. */
    protected int transactionID;

    /** Are we in a transient command */
    protected boolean inTransient;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;

    /** The ToolBarManager */
    protected ToolBarManager toolBarManager;

    /** Constant defining how far to zoom in or out per level */
    protected double zoomFactor;

    /** The current zoom level */
    protected int zoomLevel;

    /** The current scale of the map view. Typically meters per pixel */
    protected double mapScale;

    /** The location of the mouse at the last press or drag event, used for the
     *  initial condition for a pan controlled by a mouse drag and for moving
     *  entities */
    protected Point lastMousePoint;

    /** The panel containing the rendered map */
    protected AbstractImagePanel mapPanel;

    /** UI controls to switch modes between pick & place and navigation modes */
    protected JToolBar toolBar;

    /** Group for the mode switch buttons */
    protected ButtonGroup modeGroup;

    /** Control to enable the pick & place mode */
    protected JToggleButton pickAndPlaceButton;

    /** Flag indicating that pick & place mode is active */
    //private boolean pickAndPlaceIsActive;

    /** Control to enable the navigation mode */
    protected JToggleButton navigateButton;

    /** Flag indicating that navigation mode is active */
    //private boolean navigateIsActive;

    /** Group for the navigate switch buttons */
    protected ButtonGroup navigateGroup;

    /** Control to enable the bounding function */
    protected JToggleButton boundButton;

    /** Control to toggle between metric and imperial units */
    protected JToggleButton isMetric;

    /** Flag indicating that select area function is active */
    protected boolean boundIsActive;

    /** Flag indicating that a select area drag is in progress */
    protected boolean boundInProgress;

    /** Rectangle containing the bound function area */
    protected Rectangle boundRectangle;

    /** Control to enable pan navigation mode */
    protected JToggleButton panButton;

    /** Flag indicating that the pan navigation function is active */
    protected boolean panIsActive;

    /** Flag indicating that a pan drag is in progress */
    protected boolean panInProgress;

    /** The status display of map coordinates */
    protected JTextField statusField;

    /** The status display of map coordinates */
    protected JTextField scaleField;

    /** Progress bar for loading images */
    protected JProgressBar progressBar;

    /** The pan/zoom control */
    protected PanZoomControl navControl;

    /** Flag indicating that the current mouse position is over the pan/zoom control */
    protected boolean isOverNavControl;

    /** The location identifier string parameter for the image reader initialization thread. */
    protected String url_string;

    /** GT Utility class instance */
    protected ViewUtils viewUtils;

    /** Flag indicating that the map renderer and context are ready */
    protected boolean mapIsAvailable;

    /** Flag indicating a location has been set */
    protected boolean locationSelected;

    /** a map of entities to renderers */
    protected EntityRendererMapper entityRendererMapper;

    /** Was there a mousePress/Release event before the click this "frame" */
    protected boolean inMousePressed;

    /** Utility class to construct entities from tools */
    protected EntityBuilder entityBuilder;

    /** The current plane the edits effect */
    protected ViewingFrustum.Plane currentPlane;

    /** A helper class to handle selection easier */
    protected EntitySelectionHelper seletionHelper;

///////////////////////////////////
    // Inner classes
    ///////////////////////////////////



    /**
     * Image handler panel used to render the specific information of the map.
     */
    abstract class AbstractImagePanel extends JComponent {



        /** Flag to say that this object must be restarted */
        protected boolean reset;

        /**
         * The base image of the map. Only updated when the coverage
         * area changes or that the bounds of the window changes.
         */
        protected BufferedImage baseImage;

        /**
         * Composited version of the image that has the base image plus
         * all of the entities rendered on it. It does not contain the
         * current tool icon image.
         */
        protected BufferedImage entityImage;

        /**
         * Flag to indicate that the coverage area has been recalculated
         * and a new image needs to be fetched from GeoTools.
         */
        protected boolean coverageChanged;

        /**
         * One or more entities have changed and so need to be redrawn. In
         * this case we need to clear the base image and re-render everything
         * again in the next frame, rather than just blitting the current
         * composited entityImage.
         */
        protected boolean entitiesChanged;

        /**
         * Construct a default instance of this class.
         */
        AbstractImagePanel() {

            setBackground(Color.GRAY);
            coverageChanged = true;
            entitiesChanged = true;
        }

        //----------------------------------------------------------
        // Methods defined by JComponent
        //----------------------------------------------------------

        /**
         * Standard override of the update method to prevent clearing of
         * of the underlying area since we are going to be covering the entire
         * window area.
         */
        public void update(Graphics g) {
            paint(g);
        }

        /**
         * Change the bounds of the panel.
         *
         * @param x The new x location of this component
         * @param y The new y location of this component
         * @param width The new width of the component
         * @param height The new height of the component
         */
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);

            if ((width > 0) && (height > 0)) {
                updateBufferImages();
                updateMapArea();
                updateEntityScaleAndZoom();
            }
        }

        /**
         * Change the size of this component.
         *
         * @param width The new width of the component
         * @param height The new height of the component
         */
        public void setSize(int width, int height) {
            super.setSize(width, height);

            if ((width > 0) && (height > 0)) {
                updateBufferImages();
                updateMapArea();
                updateEntityScaleAndZoom();
            }
        }

        /**
         * Change the size of this component.
         *
         * @param width The new width of the component
         * @param height The new height of the component
         */
        public void setSize(Dimension d) {
            super.setSize(d);

            if ((d.width > 0) && (d.height > 0)) {
                updateBufferImages();
                updateMapArea();
                updateEntityScaleAndZoom();
            }
        }

        /**
         * Canvas painting
         *
         * @param g canvas graphics object
         */
        public  void paintComponent(Graphics g){
            super.paintComponent(g);
        }

        //----------------------------------------------------------
        // Local Methods
        //----------------------------------------------------------

        /**
         * Method to force the panel to reset all the zoom and may coverage
         * handlin.
         */
        void reset() {
            reset = true;
        }

        /**
         * Inform the panel that one or more entities have been updated and
         * need to be repainted.
         */
        void entityUpdateRequired() {
            entitiesChanged = true;
            repaint();
        }

        /**
         * Inform the panel that the map area (coverage) has been updated and
         * needs to be repainted.
         */
        void coverageUpdateRequired() {
            coverageChanged = true;
            repaint();
        }

        /**
         * Update the information about the entities and bounds.
         */
        abstract void updateMapArea();

        /**
         * The window has changed size, so re-size the buffer images to the
         * new size.
         */
        protected void updateBufferImages() {
            Rectangle panelBounds = getBounds();

            baseImage = new BufferedImage(
                panelBounds.width,
                panelBounds.height,
                BufferedImage.TYPE_INT_ARGB);
            entityImage = new BufferedImage(
                panelBounds.width,
                panelBounds.height,
                BufferedImage.TYPE_INT_ARGB);
        }

    }
 // End of inner classes


    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor
     */
    public AbstractView(WorldModel model, String imageDir) {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        setImageDirectory(imageDir);

        zoomFactor = 1.5;
        mapScale = 1.0;
        showIconCenter = false;

        this.model = model;
        helperMode = View.HELPER_SELECTED;
        model.addModelListener(this);

        entityMap = new HashMap<Integer, Entity>();
        vertexMap = new HashMap<Integer, Integer>();
        selectedEntities = new ArrayList<Entity>();


        toolTransform = new AffineTransform();

        imageMap = new HashMap<String, Image>();

        Toolkit tk = Toolkit.getDefaultToolkit();

        String imgName = PropertyTools.fetchSystemProperty(ASSOCIATION_2D_IMAGE_PROPERTY, DEFAULT_ASSOCIATION_IMAGE);
        Image img = ImageLoader.loadImage(imgName);
        associateCursor = tk.createCustomCursor(img, new Point(), null);

        img = new ImageIcon("blankCursor").getImage();
        blankCursor = tk.createCustomCursor(img, new Point(), null);

        FileLoader loader = new FileLoader();
        Object[] file = loader.getFileURL(DEFAULT_OPEN_HAND_CURSOR_IMAGE);
        img = tk.createImage((URL)file[0]);
        openHandCursor = tk.createCustomCursor(img, new Point(6, 4), null);

        file = loader.getFileURL(DEFAULT_CLOSED_HAND_CURSOR_IMAGE);
        img = tk.createImage((URL)file[0]);
        closedHandCursor = tk.createCustomCursor(img, new Point(7, 7), null);

        crossHairCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        toolBarManager = ToolBarManager.getToolBarManager();

        seletionHelper = 
            EntitySelectionHelper.getEntitySelectionHelper();

        tmpPos = new double[3];
        startPos = new double[3];
        startPos2 = new double[3];
        entityRotating = false;
        startRot = new float[4];
        tmpRot = new float[4];
        tmpScale = new float[2];
        tmpCenter = new int[2];
        swap = false;
        iconScaleX = 0.25f;
        iconScaleY = 0.25f;
        multiSegmentOp = false;
        screenPos = new int[3];
        lastSegmentPosition = new int[3];
        viewID = (long) (Math.random() * Long.MAX_VALUE);
        imageSize = new Dimension(512,512);
        inTransient = false;
        locationSelected = false;
        inMousePressed = false;

        mapIsAvailable = false;

        // default the MouseMode to an uninitialized state
        currentMode = MouseMode.NONE;
        previousMode = MouseMode.NONE;

        // default plane to edit is the top down
        currentPlane = ViewingFrustum.Plane.TOP;

        toolTransform.scale(iconScaleX,iconScaleY);

        ViewManager.getViewManager().addView(this);

    }

    public Dimension getMinimumSize() {
        return imageSize;
    }

    public Dimension getPreferredSize() {
        return imageSize;
    }

    //----------------------------------------------------------
    // Methods required by View
    //----------------------------------------------------------

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {

        if (tool == null) {
            return;
        }

        int type = tool.getToolType();

//System.out.println("Got tool: " + tool + "(" + type + ")");
        if (type == Entity.TYPE_WORLD) {

            setMode(MouseMode.NAVIGATION, true);

            locationSelected = true;

            return;

        } else {

            // check to make sure work has been added, this is in case the
            // file was loaded.
            if (!locationSelected) {
                Entity[] entities = model.getModelData();
                for (int i = 0; i < entities.length; i++) {

                    if ((entities[i] != null) &&
                        (entities[i].getType() == Entity.TYPE_WORLD)) {

                        locationSelected = true;
                        break;

                    }
                }
            }

            if (locationSelected) {

                ViewManager.getViewManager().disableAssociateMode();

                if (type == Entity.TYPE_MULTI_SEGMENT) {
                    setMode(MouseMode.PLACEMENT, true);
                    multiSegmentOp = true;
                } else {
                    setMode(MouseMode.PLACEMENT, true);
                    multiSegmentOp = false;
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "You cannot add a new item until a Location has been selected.",
                    "Add Item Action",
                    JOptionPane.ERROR_MESSAGE);

                mapPanel.repaint();
                return;
            }
        }
        tool.setCurrentView(currentPlane.toString());
        toolImage = getImage(tool.getIcon());
        imgWidth = toolImage.getWidth(null);
        imgHeight = toolImage.getHeight(null);

        float[] size = tool.getSize();
        float[] scale = tool.getScale();

        if (imgWidth < 0 || imgHeight < 0) {
            errorReporter.messageReport("Error processing icon: " + tool.getIcon());
        }

        calcScaleFactor(
                tool.isFixedAspect(),
                imgWidth,
                imgHeight,
                size,
                scale,
                tmpScale,
                tmpCenter,
                tool.getToolType());

        iconScaleX = tmpScale[0];
        iconScaleY = tmpScale[1];

        iconCenterX = tmpCenter[0];
        iconCenterY = tmpCenter[1];

        currentTool = tool;
        toolSegmentImage = getImage(tool.getIcon());

    }

    /**
     * Go into associate mode. The next mouse click will perform
     * a property update
     *
     * @param validTools A list of the valid tools. null string will be all
     *        valid. empty string will be none.
     * @param propertyGroup The grouping the property is a part of
     * @param propertyName The name of the property being associated
     */
    public void enableAssociateMode(
            String[] validTools,
            String propertyGroup,
            String propertyName) {

        // Change to selection state
//System.out.println("GT2DView.enableAssociateMode()");
        setMode(MouseMode.ASSOCIATE, false);

        this.validTools = validTools;
        this.propertyGroup = propertyGroup;
        this.propertyName = propertyName;

        highlightTools(validTools, true);

    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {

//System.out.println("GT2DView.disableAssociateMode()");

        setMode(MouseMode.SELECTION, true);
        associateMode = false;
        ignoreDrag = false;

        highlightTools(null, false);

    }

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode) {
        helperMode = mode;

        mapPanel.repaint();
    }

    /**
     * Get the viewID.  This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID() {
        return viewID;
    }

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode) {
    }

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {

        if (entityBuilder == null) {
            entityBuilder = DefaultEntityBuilder.getEntityBuilder();
        }

        return entityBuilder;
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        this.entityBuilder = entityBuilder;
    }
    //----------------------------------------------------------
    // Methods required by ModelListener
    //----------------------------------------------------------
    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity
     */
    public abstract void entityAdded(boolean local, Entity entity);

    /**
     * An entity was removed.
     *
     * @param local Is the request local
     * @param entity The entity to remove
     */
    public abstract void entityRemoved(boolean local, Entity entity);

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * The master view has changed.
     *
     * @param local Is the request local
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
        // ignore
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public abstract void modelReset(boolean local);

 // ----------------------------------------------------------
    // Methods required by EntityPropertyListener interface
    // ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     *  @param ongoing Is this an ongoing change or the final value?
     */
    public abstract void propertyUpdated(int entityID, String propertySheet,
            String propertyName, boolean ongoing);

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        // ignored
    }

    //----------------------------------------------------------
    // Methods defined by EntityChildListener
    //----------------------------------------------------------

    /**
     * A child was added.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     */
    public abstract void childAdded(int parent, int child);
    /**
     * A child was removed.
     *
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public abstract void childRemoved(int parent, int child);

    /**
     * A child was inserted.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public abstract void childInsertedAt(int parent, int child, int index);
    //---------------------------------------------------------
    // Method defined by ActionListener
    //---------------------------------------------------------

    public abstract void actionPerformed(ActionEvent ae);

    //----------------------------------------------------------
    // Methods required by KeyListener
    //----------------------------------------------------------

    public void keyTyped(KeyEvent ke) {
        // Do nothing
    }

    public abstract void keyPressed(KeyEvent ke);

    /**
     * Notification that a key that was previously pressed is now released.
     *
     * @param ke The event that caused this method to be called
     */
    public void keyReleased(KeyEvent ke) {

        int code = ke.getKeyCode();

        switch(code) {
            case KeyEvent.VK_SHIFT:
                shiftActive = false;
                break;
        }
    }

    //----------------------------------------------------------
    // Methods required by MouseWheelListener
    //----------------------------------------------------------

    /**
     * Controls entity rotation and panel zoom
     *
     * @param mwe The mouse wheel event
     */
    public abstract void mouseWheelMoved(MouseWheelEvent mwe);

    //----------------------------------------------------------
    // Methods required by MouseMotionListener
    //----------------------------------------------------------

    /**
     * Controls the placement of entities on the entity layer and
     * panning of the map area.
     *
     * @param me The MouseEvent
     */
    public abstract void mouseDragged(MouseEvent me);

    /**
     * Controls the drawing parameters of segmented entities on the
     * entity layer and produces mouse over world coordinates.
     *
     * @param me The MouseEvent
     */
    public abstract void mouseMoved(MouseEvent me);

  //----------------------------------------------------------
    // Methods required by MouseListener
    //----------------------------------------------------------

    /**
     * Controls centering the map on the mouse click position
     *
     * @param me The mouse event
     */
    public abstract void mouseClicked(MouseEvent me);

    /**
     * Initiates adding and manipulating entities
     *
     * @param me The mouse event
     */
    public abstract void mousePressed(MouseEvent me);

    /**
     * Terminates entity manipulation
     *
     * @param me The mouse event
     */
    public abstract void mouseReleased(MouseEvent me);

    /**
     * Resets the cursor if an entity tool is active
     *
     * @param me The mouse event
     */
    public abstract void mouseEntered(MouseEvent me);

    /**
     * Resets the cursor, terminates any active entity rotations
     *
     * @param me The mouse event
     */
    public void mouseExited(MouseEvent me) {

        insideMap = false;

        // finish any rotation action
        checkForRotation();

        switch (currentMode) {
            case SELECTION:
                setMode(MouseMode.SELECTION, false);
                break;

            case PLACEMENT:
                setMode(MouseMode.PLACEMENT, false);
                break;

            case NAVIGATION:
                isOverNavControl = false;
                break;
        }

        statusField.setText("");
    }

    //---------------------------------------------------------
    // Method defined by ChangeListener
    //---------------------------------------------------------

    /**
     * Track updates to the zoom control
     */
    public void stateChanged(ChangeEvent ce) {
        int newLevel = navControl.getValue();
        incrementZoomLevel(newLevel - zoomLevel);
    }

    //---------------------------------------------------------
    // Method defined by Runnable
    //---------------------------------------------------------

    public abstract void run();


    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Set the selected entity out of line segments and vertices in the scene.
     *
     * @param entityWrapper A holder entity data.
     * @param segmentID Line segment ID.
     * @param vertexID Vertex point ID.
     * @author Rex Melton, Commented and edited by Sang Park
     */
    protected abstract void setSelectedEntity(EntityWrapper entityWrapper, int segmentID, int vertexID);

    /**
     * Set the operational mode of the View
     *
     * @param mode The mode to set. NAVIGATION, PLACEMENT, or SELECTION
     * @param resetState Reset the currect state variables,
     *          selected tool, cursor, etc.
     */
    public void setMode(MouseMode mode, boolean resetState) {

        // initialize state for navigation mode
        if (resetState) {
            resetState();
        }

        resetNavigateState();

        switch(mode) {
            case NAVIGATION:
                currentMode = MouseMode.NAVIGATION;

                navigateButton.setSelected(true);
                panButton.setEnabled(true);
                boundButton.setEnabled(true);
                isMetric.setEnabled(true);
                if (boundIsActive) {
                    mapPanel.setCursor(crossHairCursor);
                } else if (panIsActive) {
                    mapPanel.setCursor(openHandCursor);
                }
                mapPanel.repaint();

                break;

            case PLACEMENT:
                // initialize state for place mode
                currentMode = MouseMode.PLACEMENT;

                pickAndPlaceButton.setSelected(true);
                panButton.setEnabled(false);
                boundButton.setEnabled(false);
                isMetric.setEnabled(false);

                mapPanel.setCursor(null);
                mapPanel.repaint();

                break;

            case SELECTION:
                // initialize state for pick mode
                currentMode = MouseMode.SELECTION;

                mapPanel.setCursor(null);

                pickAndPlaceButton.setSelected(true);
                panButton.setEnabled(false);
                boundButton.setEnabled(false);
                isMetric.setEnabled(false);

                entityDragging = false;
                mapPanel.repaint();

                break;

            case ASSOCIATE:

                // initialize state for pick mode
                currentMode = MouseMode.ASSOCIATE;

                pickAndPlaceButton.setSelected(true);
                panButton.setEnabled(false);
                boundButton.setEnabled(false);
                isMetric.setEnabled(false);

                entityDragging = false;
                ignoreDrag = true;
                associateMode = true;

                mapPanel.setCursor(associateCursor);
                mapPanel.repaint();

                break;

            default:
                errorReporter.messageReport("GT2DView: Unknown Operation Mode");
        }
    }

    /**
     * Add a segment to the end of the selected sequence
     *
     * @param startVertexID The starting vertex
     * @param endVertexID The ending vertex
     */
    protected abstract void addSegment(Entity entity, int startVertexID, int endVertexID);

    /**
     * Add a vertex at the location.  Then place a segment between
     * the selected vertex and the newly created one.
     *
     * @param entity - The entity to add the vertex to
     * @param x - The screen x position for the requested vertex
     * @param y - The screen y position for the requested vertex
     * @param append - if this appended to the end
     * @param startVertexID - The start of the segment
     */
    protected abstract void addVertexAndSegment(
            Entity entity,
            int x,
            int y,
            int index,
            int startVertexID);

    /**
     * Helper method to end a rotation
     */
    protected void checkForRotation() {

        // if the last action was to rotate the entity, then finalize the command
        if (entityRotating) {

            //should only be one selected
            Entity entity = selectedEntities.get(0);

            adjustRotation(true, tmpRot);

            RotateEntityCommand cmd = new RotateEntityCommand(
                model,
                transactionID,
                entity.getEntityID(),
                tmpRot,
                startRot);
            cmd.setErrorReporter(errorReporter);
            model.applyCommand(cmd);

            inTransient = false;

            entityRotating = false;
        }
    }

    /**
     * Set the heading of the current entity.
     *
     * @param angle The angle in degrees);
     */
    protected void setHeading(int angle) {

        if (!inTransient) {
            transactionID = model.issueTransactionID();
            inTransient = true;
        }

        //should only be one selected
        Entity entity = selectedEntities.get(0);

//System.out.println("setHeading.angle: " + angle);

        tmpRot[0] = 0;
        tmpRot[1] = 1;
        tmpRot[2] = 0;
        tmpRot[3] = (float) (angle / 180.0f) * (float) Math.PI;

        //adjustRotation(true, tmpRot);

        entityRotating = true;

        RotateEntityTransientCommand cmd = new RotateEntityTransientCommand(
            model,
            transactionID,
            entity.getEntityID(),
            tmpRot);

        cmd.setErrorReporter(errorReporter);
        model.applyCommand(cmd);
    }

    /**
     * Find an entity given a screen location.  This will return
     * the closest entity.
     *
     * @param The x position
     * @param The y position
     * @return The entity search return
     */
    protected abstract EntitySearchReturn findEntity(final int x, final int y);

    /**
     * Find all entities given a screen bounding box.  This will return
     * the list of entities.
     *
     * @param bounds The search box
     * @return The entity list
     */
    protected abstract ArrayList<Entity> findEntity(Rectangle bounds);

    /**
    *
    * @param screenPosition
    * @param worldPosition
    * @param rotMatrix
    * @param offsetX
    * @param offsetY
    * @param screenCheck
    */
   protected void getCheckPoint(int[] screenPosition, double[] worldPosition,
           Matrix4f rotMatrix, int offsetX, int offsetY, int[] screenCheck) {

       double[] worldCheck = new double[3];
       Vector3f position;

       // get the check point
       screenCheck[0] = screenPosition[0] + offsetX;
       screenCheck[1] = screenPosition[1] + offsetY;

       convertScreenPosToWorldPos(screenCheck[0], screenCheck[1], worldCheck, false);

       // move check point relative to origin
       worldCheck[0] = worldCheck[0] - worldPosition[0];
       worldCheck[1] = worldCheck[1] - worldPosition[1];
       worldCheck[2] = worldCheck[2] - worldPosition[2];

       // create check vector from origin to check point
       position =
           new Vector3f((float) worldCheck[0],
               (float) worldCheck[1],
               (float) worldCheck[2]);

       // rotate about origin
       rotMatrix.transform(position);

       // traslate back to start
       worldCheck[0] = position.x + worldPosition[0];
       worldCheck[1] = position.y + worldPosition[1];
       worldCheck[2] = position.z + worldPosition[2];

       // change to screen positioning
       convertWorldPosToScreenPos(worldCheck, screenCheck);
   }

   /**
    * Convert mouse coordinates into world coordinates.
    *
    * TODO: This depends on iconCenterX which per tool.  Needs to be passed in.
    *
    * @param panelX The panel x coordinate
    * @param panelY The panel y coordinate
    * @param position The world position in meters
    */
   protected abstract void convertScreenPosToWorldPos(int panelX, int panelY, double[] position, boolean newEntityFlag);


    /**
     * Convert world coordinates in meters to panel pixel location.
     *
     * TODO: This depends on iconCenterX which per tool.  Needs to be passed in.
     *
     * @param position World coordinates
     * @param pixel Mouse coordinates
     */
   protected abstract void convertWorldPosToScreenPos(double[] position, int[] pixel);

   /**
    * Get the value of an attribute as a float
    *
    * @param The attrbiute name
    * @param The parent element
    * @return The value as a float.
    */
   protected float getAttributeFloatValue(String name, Element e) {
       String val = e.getAttribute(name);

       if (val == null) {
           errorReporter.messageReport("No attribute: " + name);
           return 0;
       }

       return Float.parseFloat(val.trim());
   }

  /* *
   *  @param The attrbiute name
   * @param The parent element
   * @return The value as a boolean.
   */
   protected boolean getAttributeBooleanValue(String name, Element e) {
      String val = e.getAttribute(name);

      if (val == null) {
          errorReporter.messageReport("No attribute: " + name);
          return false;
      }

      return val.equalsIgnoreCase("TRUE");
  }

  /**
   * Calculate the scaling factors based on the icon size and the tool size.
   * The goal is to have a correctly scaled icon with some miniumum size representation.
   *
   * @param fixedAspect Is the aspect ratio of the icon fixed
   * @param imgWidth The icon image width in pixels
   * @param imgHeight The icon image height in pixels
   * @param toolWidth The tool width
   * @param toolLength The tool length
   * @param scale The calculated x and y scale
   * @param center The calculated x and y center
   * @param current The current type of the tool
   */
   protected abstract void calcScaleFactor(boolean fixedAspect, int imgWidth, int imgHeight, float[] toolSize,
        float[] toolScale, float[] scaledXAndY, int[] centerOfScaledTool , int toolType);
  /**
   * Create an Image from a url.  Cache the results.
   *
   * @return The Image or null if not found
   */
   protected Image getImage(String url) {

      Image image = (Image) imageMap.get(url);
      if (image == null) {

          // try to retrieve from the classpath
          FileLoader fileLookup = new FileLoader();

          Object[] file = fileLookup.getFileURL(url);
          URL iconURL = (URL)file[0];
          //InputStream iconStream = (InputStream)file[1];

          ImageIcon icon = new ImageIcon(iconURL);

          if (icon == null) {
              errorReporter.messageReport("Can't find image: " + url);
              return null;
          }

          image = icon.getImage();
          imageMap.put(url, image);
      }

      return image;
  }


  /**
   * Adjust a rotation based on the orientation of the image.
   *
   * @param toMap Adjust 3D to the map, or map to 3D
   * @param rot The rotation
   */
   protected void adjustRotation(boolean toMap, float[] rot) {
      // TODO: Need to generalize this
      if (toMap) {
          if (swap) {
              if (mapScale < 0) {
                  rot[3] = - rot[3];
                  rot[3] -= Math.PI / 2;
              }
          }
      } else {
          if (swap) {
              if (mapScale < 0) {
                  rot[3] += Math.PI / 2;
                  rot[3] = - rot[3];
              }
          }
      }
  }

  /**
   * Change the selected entity.
   *
   * @param id The entity selected
   * @param subid The sub entity id
   */
   protected void changeSelection(List<Entity> selected) {

      for (int i = 0; i < selected.size(); i++) {
          Entity e = selected.get(i);

          // send the selecting command
          SelectEntityCommand cmdSelect = 
              new SelectEntityCommand(e, true);
          model.applyCommand(cmdSelect);

      }

  }

   /**
    * Resets all entity management state variables.
    */
    protected abstract void resetState();

   /**
    * Reset the navigation control parameters to a default settings
    */
    protected abstract void resetNavigateState();

  /**
   * Inform the view that a map image reader is being initialized
   */
  public void setIsLoading(boolean isLoading) {
      if (isLoading) {
          progressBar.setIndeterminate(true);
          progressBar.setStringPainted(true);
          progressBar.setString("Loading");
      } else {
          progressBar.setIndeterminate(false);
          progressBar.setValue(0);
          progressBar.setStringPainted(false);
      }
  }

  /**
   * Calculate new parameters given the argument change in zoom levels.
   * Updates and causes a redraw of the map to accommodate the changed
   * level.
   *
   * @param delta The number of levels to change
   */
  protected abstract void incrementZoomLevel(int delta);



  /**
   * Set an explicit zoom level. This assumes that a map area has been
   * set for the zoom level before this happens,
   *
   * @param level The zoom level to use
   */
  protected abstract void setZoomLevel(int level);



  /**
   * Set the directory to load images from.
   *
   * @param dir The image dir
   */
  private void setImageDirectory(String dir) {

      if (dir == null) {
          dir = System.getProperty("user.dir");
          errorReporter.warningReport("No image directory set, using: " + dir, null);
      }

      if (!dir.endsWith("/"))
          imgDir = dir + "/";
      else
          imgDir = dir;
  }

  /**
   * Highlight a set of tools.
   *
   * @param state True to highlight the tool, false to unhighlight all tools
   */
  protected abstract void highlightTools(String[] validTools, boolean state);

  /**
   * Convenience method to update the entity list for scale and zoom
   * based on the map changing size or the panel.
   */
  protected abstract void updateEntityScaleAndZoom();

  /**
   * Convenience method to update a single entity for scale and zoom
   * based on the map changing size or the panel.
   */
  protected abstract void updateEntityScaleAndZoom(EntityWrapper eWrapper);

  /** Set the location.
  *
  * @param url_string The url of the map imagery.
  */
 public abstract void setLocation(String url_string);

 /**
  * Get the rendering component.
  *
  * @return The rendering component
  */
 public abstract JComponent getComponent();

 /**
  * Get the current plane being edited for
  *
  * @return the currentPlane
  */
 public abstract ViewingFrustum.Plane getCurrentPlane();

 /**
  * Set the current plane being edited for
  *
  * @param currentPlane the currentPlane to set
  */
 public abstract void setCurrentPlane(ViewingFrustum.Plane newPlane);




}
