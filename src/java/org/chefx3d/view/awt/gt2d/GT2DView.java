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

// External imports
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.vecmath.*;

// Explicitly listed due to clash with java.util.List
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Color;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;

// needed for the ruler
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemListener;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;

import org.geotools.factory.CommonFactoryFinder;

import org.geotools.gce.image.WorldImageReader;

import org.geotools.gce.imagemosaic.ImageMosaicReader;

import org.geotools.gce.imagepyramid.ImagePyramidReader;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;

import org.geotools.geometry.jts.ReferencedEnvelope;

import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;

import org.geotools.measure.CoordinateFormat;

import org.geotools.renderer.GTRenderer;

import org.geotools.renderer.lite.StreamingRenderer;

import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;

import org.opengis.coverage.grid.GridCoverage;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.w3c.dom.Element;


// Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.property.AssociateProperty;

import org.chefx3d.actions.awt.AddControlPointAction;
import org.chefx3d.actions.awt.DeleteAction;
import org.chefx3d.actions.awt.HighlightAssociatesAction;

import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SegmentTool;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SegmentableTool;
import org.chefx3d.tool.VertexTool;
import org.chefx3d.toolbar.ToolBarManager;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;
import org.chefx3d.util.PropertyTools;

import org.chefx3d.view.View;
import org.chefx3d.view.ViewManager;

/**
 * A View which is backed by a Geotools referenced image.
 *
 * @author Rex Melton
 * @version $Revision: 1.128 $
 */

public class GT2DView extends AbstractView implements ItemListener {

    /** Default flag for showing the rule */
    private static final boolean DEFAULT_SHOW_RULER = false;

    /** An empty list used for clearing selections */
    private static final List<Entity> EMPTY_ENTITY_LIST =
        Collections.unmodifiableList(new ArrayList<Entity>());

    /** The entity ID  to display(EntityWrapper) */
    private HashMap<Integer, EntityWrapper> entityWrapperMap;

    /** A list of all the entity wrappers used for fast listing of all items */
    private ArrayList<EntityWrapper> wrapperList;


    //////////////////////////////////////////////////////////////////////////////

    /** The base map image context to render */
    private MapContext baseContext;

    /** The area (bounds) of the map to draw. The map coordinate boundaries
    *  used by the renderer */
    private ReferencedEnvelope mapArea;

    /** The last map area drawn. */
    private ReferencedEnvelope oldMapArea;

    /** The referencing system in use */
    private CoordinateReferenceSystem crs;

    /** Formatter of map position coordinates */
    private CoordinateFormat coordFormat;

    /** Position coordinate used by formatter */
    private DirectPosition2D position;

    /** Factory for producing grid coverage of entities layer */
    private GridCoverageFactory gcf;

    /** The style type used for coverage image layers */
    private Style rasterStyle;

    /** The pan/zoom control */
    private PanZoomControl navControl;

    /** GT Utility class instance */
    private ViewUtils viewUtils;

    /** a map of entities to renderers */
    private EntityRendererMapper entityRendererMapper;

    /** The panel containing the rendered map */
    protected ImagePanel mapPanel;

    /** The ruler running across the bottom of the grid */
    private Ruler horizontalRuler;

    /** The ruler running across the side of the grid */
    private Ruler verticalRuler;

    /** Flag to indicate if rulers are enabled or not */
    private boolean rulerEnabled;

    /** The JFrame containing the WallSelector panel. */
    JFrame miniFrame;

    ///////////////////////////////////
    // Inner classes
    ///////////////////////////////////

    /**
     * Image handler panel used to render the specific information of the map.
     */
    class ImagePanel extends  AbstractView.AbstractImagePanel {

         /** The size of the view panel last time we drew */
        protected Rectangle previousPanelBounds;

        /**
         * The GeoTools map renderer. Renders each layer to a common coordinate
         * reference frame
         */
        private GTRenderer renderer;

        /** The WallSelector panel.  Will open a new window.  We now can play with
         * how the panel will work once within the Emerson client. */
        GT2DWallSelector wallSelector;

        /**
         * Construct a default instance of this class.
         */
        ImagePanel(GT2DWallSelector ws) {

            super();

            // The wall selector
            wallSelector = ws;

            // The renderer for all the map layers
            renderer = new StreamingRenderer();

            // feel the need for speed
            RenderingHints hints =
                new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                                   RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            hints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
            hints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
            hints.put(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_SPEED);
            hints.put(RenderingHints.KEY_DITHERING,
                RenderingHints.VALUE_DITHER_DISABLE);

            renderer.setJava2DHints(hints);
        }

        //----------------------------------------------------------
        // Methods defined by JComponent
        //----------------------------------------------------------

        /**
         * Canvas painting
         *
         * @param g canvas graphics object
         */
        public void paintComponent(Graphics g) {

            super.paintComponent(g);

            if (!mapIsAvailable)
                return;

            if (coverageChanged) {
                // render the new coverage image
                Rectangle panelBounds = getBounds();
                panelBounds.x = 0;
                panelBounds.y = 0;

                Graphics2D ig = baseImage.createGraphics();
                ig.setColor(Color.GRAY);
                ig.fillRect(0, 0, panelBounds.width, panelBounds.height);



                renderer.setContext(baseContext);
                renderer.paint(ig, panelBounds, mapArea);
            }

            if(entitiesChanged || coverageChanged) {
                Graphics2D eg = entityImage.createGraphics();
                AffineTransform defaultTransform = eg.getTransform();

                eg.drawImage(baseImage, 0, 0, null);

                if (helperMode != View.HELPER_NONE) {
                    // Render all helpers first
                    for(int i = 0; i < wrapperList.size(); i++) {
                        EntityWrapper eWrapper = wrapperList.get(i);

                        if(!eWrapper.getEntity().isHelper()) {
                            continue;
                        }

                        // render object
                        //eWrapper.getEntity().

                        ToolRenderer toolRenderer =
                            entityRendererMapper.getRenderer(eWrapper.getEntity(),
                                                             currentPlane);
                         eg.setTransform(eWrapper.getXform());

                         if(toolRenderer instanceof SegmentToolRenderer){

                        	 // draw segments to the wallSelector window
                        	 wallSelector.drawSegments(eWrapper);
                         }

                        toolRenderer.draw(eg, eWrapper);

                    }
                }

                // Not really needed, but this is a fail-safe just in case
                // something in the above loop goes completely haywire.
                eg.setTransform(defaultTransform);

                for(int i = 0; i < wrapperList.size(); i++) {
                    EntityWrapper eWrapper = wrapperList.get(i);

                    if (eWrapper.getEntity().isHelper()) {
                        continue;
                    }

                    if (eWrapper == null) {
                        continue;
                    }

                    // render object
                    ToolRenderer toolRenderer =
                        entityRendererMapper.getRenderer(eWrapper.getEntity(),
                                                         currentPlane);
                    eg.setTransform(eWrapper.getXform());

                    toolRenderer.draw(eg, eWrapper);
                }

                // Not really needed, but this is a fail-safe just in case
                // something in the above loop goes completely haywire.
                eg.setTransform(defaultTransform);

                entitiesChanged = false;
                coverageChanged = false;
            }

            // Now draw everything to the main window.
            Graphics2D g2d = (Graphics2D)g;
            g2d.drawImage(entityImage, 0, 0, null);

            AffineTransform defaultTransform = g2d.getTransform();

            // Draw selections directly onto the main surface.
            for(int i = 0; i < selectedEntities.size(); i++) {
                Entity entity = selectedEntities.get(i);
                EntityWrapper eWrapper =
                    entityWrapperMap.get(entity.getEntityID());

                if (eWrapper == null) {
                    continue;
                }

                g2d.setTransform(defaultTransform);

                // render object
                ToolRenderer toolRenderer =
                    entityRendererMapper.getRenderer(eWrapper.getEntity(),
                                                     currentPlane);
                g2d.transform(eWrapper.getXform());

                toolRenderer.drawSelection(g2d, eWrapper);
            }

            // Restore any transforms applied above
            g2d.setTransform(defaultTransform);

            // Draw the tool image/icon next if we have one set.
            if (insideMap && toolImage != null) {
                g2d.drawImage(toolImage, toolTransform, null);

                if (showIconCenter) {
                    g2d.setColor(Color.red);
                    g2d.drawRect(mouseX + iconCenterX, mouseY + iconCenterY,3,3);
                }
            }

            if (boundInProgress) {

                // an area selection is in progress,
                // draw the bounding rectangle
                g.setColor(Color.RED);
                int width = boundRectangle.width;
                int height = boundRectangle.height;
                int x = boundRectangle.x;
                int y = boundRectangle.y;
                if (width < 0) {
                    x += width;
                    width = (-width);
                }
                if (height < 0) {
                    y += height;
                    height = (-height);
                }
                g.drawRect(x, y, width, height);

            }

            if (currentMode == MouseMode.NAVIGATION || currentMode == MouseMode.SELECTION) {
                // navigate mode is active, paint the
                // control on top of the map area
                navControl.paintComponent(g2d);
            }

            if( rulerEnabled ){
                // update distances of the rulers
                horizontalRuler.setValueOfRuler(mapArea.getWidth(),
                                                mapArea.getMinX());
                verticalRuler.setValueOfRuler(mapArea.getHeight(),
                                                -mapArea.getMaxY());
                horizontalRuler.repaint();
                verticalRuler.repaint();
            }
        }

        //----------------------------------------------------------
        // Local Methods
        //----------------------------------------------------------


        /**
         * Update the information about the entities and bounds.
         */
        void updateMapArea() {

            if (!mapIsAvailable)
                return;

            // rem: MUST clear the panel bounds x & y as they are a measure
            // of the relative position of this panel to it's parent - and
            // this will FU the renderer if not zero'ed
            Rectangle panelBounds = getBounds();
            panelBounds.x = 0;
            panelBounds.y = 0;

            // check to see if the coverage image must be regenerated
            boolean coverageChange = false;

            if (!panelBounds.equals(previousPanelBounds) || reset) {

                // the viewer size has changed
                coverageChange = true;
                reset = false;

                if ((previousPanelBounds == null)) {

                    // establish the initial conditions on loading a location
                    previousPanelBounds = panelBounds;

                    // the dimensions of the panel
                    double panelWidth = panelBounds.getWidth();
                    double panelHeight = panelBounds.getHeight();

                    // the map extent
                    double mapWidth = mapArea.getWidth();
                    double mapHeight = mapArea.getHeight();

                    // calculate the new scale
                    double scaleX = panelWidth / mapWidth;
                    double scaleY = panelHeight / mapHeight;

                    // use the smaller scale
                    if (scaleX < scaleY) {
                        mapScale = 1.0 / scaleX;
                    } else {
                        mapScale = 1.0 / scaleY;
                    }

                    // the difference in width and height of the new extent divided by 2
                    double deltaX2 = ((panelWidth * mapScale) - mapWidth) * 0.5;
                    double deltaY2 = ((panelHeight * mapScale) - mapHeight) * 0.5;

                    // the new region of the map to display
                    mapArea = new ReferencedEnvelope(
                        mapArea.getMinX() - deltaX2,
                        mapArea.getMaxX() + deltaX2,
                        mapArea.getMinY() - deltaY2,
                        mapArea.getMaxY() + deltaY2,
                        crs);

                } else {
                    coverageChanged = true;
                    mapArea = viewUtils.rescaleMapArea(
                        mapArea,
                        previousPanelBounds,
                        panelBounds);
                    mapScale = viewUtils.getScale(mapArea, panelBounds);
                    previousPanelBounds = panelBounds;
                }

            }

            // check if the map extent changed
            if (!mapArea.equals(oldMapArea)) {

                // a pan or zoom has occured
                if (!coverageChanged)
                    mapScale = viewUtils.getScale(mapArea, panelBounds);

                coverageChanged = true;
                oldMapArea = mapArea;
            }
        }

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
    } // End of MapPanel inner class


    ////////////////////////////////////////////////////////////////////////////////


    /**
     * Constructor
     */
    public GT2DView(WorldModel model, String imageDir) {
        this(model, imageDir, DEFAULT_SHOW_RULER);
    }

    /**
     * Constructor
     * @param model
     * @param imageDir
     * @param showRuler
     */
    public GT2DView(WorldModel model,
                    String imageDir,
                    boolean showRuler){

        super(model,imageDir);
        rulerEnabled = showRuler;

        this.model = model;
        helperMode = View.HELPER_SELECTED;
        model.addModelListener(this);

        entityWrapperMap = new HashMap<Integer, EntityWrapper>();
        wrapperList = new ArrayList<EntityWrapper>();
        toolTransform = new AffineTransform();

        position = new DirectPosition2D();
        gcf = new GridCoverageFactory();

        initUI();

        toolTransform.scale(iconScaleX,iconScaleY);

        ViewManager.getViewManager().addView(this);

        entityRendererMapper = new DefaultEntityRendererMapper();
    }

    //---------------------------------------------------------
    // Method defined by ItemListener
    //---------------------------------------------------------

    /**
     * Process an itemStateChanged event.
     */
    public void itemStateChanged(ItemEvent e) {

        if(!rulerEnabled)
            return;

        if (e.getStateChange() == ItemEvent.SELECTED) {
            // convert rulers to metric measurement
            isMetric.setText(" m ");
            verticalRuler.setMeasurementSystem(Ruler.MeasurementSystem.METRIC);
            horizontalRuler.setMeasurementSystem(Ruler.MeasurementSystem.METRIC);
        } else {
            // convert rulers to imperial units of measurement
            isMetric.setText(" ft");
            verticalRuler.setMeasurementSystem(Ruler.MeasurementSystem.IMPERIAL);
            horizontalRuler.setMeasurementSystem(Ruler.MeasurementSystem.IMPERIAL);
        }
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
    //TODO needs to be abstract
    public void entityAdded(boolean local, Entity entity) {

//System.out.println("GT2DView.entityAdded(" + entity.getName() + ")");

        int entityID = entity.getEntityID();
        double[] position = new double[] {0 ,0, 0};
        float[] rotation = new float[] {0, 0, 0 ,0};

        if (entity instanceof PositionableEntity) {

            ((PositionableEntity)entity).getPosition(position);
            ((PositionableEntity)entity).getRotation(rotation);

            entity.addEntityPropertyListener(this);
            entity.addEntityChildListener(this);

        }

        EntityWrapper eWrapper = entityWrapperMap.get(entityID);

        // Ignore dups as they are expected in networked environments
        if (eWrapper != null) {
            return;
        }

        if (entity.getType() == Entity.TYPE_WORLD) {
            //String rel_url_string = entity.getModelURL();
            String rel_url_string = entity.getIconURL("");

            String url_string = null;
            if (rel_url_string.endsWith("png")) {
                url_string = rel_url_string;
            } else {
                int pos1 = rel_url_string.lastIndexOf("/") + 1;
                int pos2 = rel_url_string.lastIndexOf(".x3d");
                String baseName = null;

                if (pos2 > 0) {
                    if (pos1 < 0)
                        pos1 = 0;

                    baseName = rel_url_string.substring(pos1,pos2);
                } else {
                    baseName = rel_url_string;
                }

                url_string = imgDir + baseName;
            }

            entityMap.clear();
            entityWrapperMap.clear();
            wrapperList.clear();

            /////////////////////////////////////////////////////////////////
            // determine whether we've been pointed to a directory,
            // which is assumed to be the source for an image pyramid.
            // if not, then we presume that the source is a single image
            // with a predetermined file extension

            URL url = null;
            try {
                File dir = new File(System.getProperty("user.dir"));
                URL dirURL = dir.toURL();
                url = new URL(dirURL, url_string);
            } catch (MalformedURLException mue) {
                errorReporter.errorReport("Bad URL!", mue);
            }
            if (url == null) {
                return;
            }
            URI uri = null;
            try {
                uri = url.toURI();
            } catch (URISyntaxException urise) {
                errorReporter.errorReport("Bad URL!", urise);
            }
            if (uri == null) {
                return;
            }
            File target = new File(uri);
            if (target.exists() && target.isDirectory()) {
                // if a directory - assume it contains a pyramid
            } else {
                // otherwise - assume that the target is a single image

                // add png if necessary
                if (!url_string.endsWith(".png")) {
                    url_string += ".png";
                }

//System.out.println("loading image: " + url_string);

            }

            setLocation(url_string);
            /////////////////////////////////////////////////////////////////

            entityWrapperMap.put(entity.getEntityID(), null);
            entityMap.put(entity.getEntityID(), entity);

            resetState();
        } else {

            // Don't overwrite the wrapper if we already have one for this instance.
            // We should never get here because the entityAdded call should only
            // give us new unique instances, but we just want to be careful.
            if(entityWrapperMap.containsKey(entity.getEntityID()))
                return;


            ToolRenderer tImage = entityRendererMapper.getRenderer(entity,
                                                                   currentPlane);

            imgWidth = tImage.getWidth();
            imgHeight = tImage.getHeight();

            float[] size = new float[3];
            float[] scale = new float[3];
            if (entity instanceof PositionableEntity) {
                ((PositionableEntity)entity).getSize(size);
                ((PositionableEntity)entity).getScale(scale);
            }

            calcScaleFactor(
                    entity.isFixedAspect(),
                    imgWidth,
                    imgHeight,
                    size,
                    scale,
                    tmpScale,
                    tmpCenter,
                    entity.getType());

            // TODO: This will cause problems for networked events but needed for now
            iconCenterX = tmpCenter[0];
            iconCenterY = tmpCenter[1];

            // Set it up with default scale and position right now because it will
            // be corrected by updateEntityScaleAndZoom() shortly.
            EntityWrapper wrapper = new EntityWrapper(
                entity,
                imgWidth,
                imgHeight,
                0,
                0,
                1,
                1,
                mapArea,
                mapPanel,
                false,
                entity.isFixedSize());

            wrapper.setErrorReporter(errorReporter);
            wrapper.setWorldPosition(position);

            tmpRot[0] = rotation[0];
            tmpRot[1] = rotation[1];
            tmpRot[2] = rotation[2];
            tmpRot[3] = rotation[3];

            adjustRotation(false, tmpRot);

            int angle = (int)Math.round(tmpRot[3] * 180.0f / Math.PI);

            wrapper.setHeading(angle);

            entityWrapperMap.put(entity.getEntityID(), wrapper);
            entityMap.put(entity.getEntityID(), entity);
            wrapperList.add(wrapper);

            mapPanel.updateMapArea();
            updateEntityScaleAndZoom(wrapper);

            mapPanel.entityUpdateRequired();
        }
    }

    /**
     * An entity was removed.
     *
     * @param local Is the request local
     * @param entity The entity to remove
     */
     public void entityRemoved(boolean local, Entity entity) {

        if (entity == null)
            return;

        if (entity instanceof PositionableEntity) {
             entity.removeEntityPropertyListener(this);
        }

        EntityWrapper wrapper =
            entityWrapperMap.remove(entity.getEntityID());
        wrapperList.remove(wrapper);

        entityMap.remove(entity.getEntityID());

        // cleanup any associations
        ArrayList<EntityProperty> associatedList =
            (ArrayList<EntityProperty>)entity.getProperties(Entity.ASSOCIATED_ENTITIES);

        for (int i = 0; i < associatedList.size(); i++) {

            EntityProperty property = associatedList.get(i);
            if (property == null)
                continue;

            ArrayList<Entity> parentList =
                (ArrayList<Entity>)property.propertyValue;
            String propertyName = property.propertyName;

            for (int j = 0; j < parentList.size(); j++) {

                Entity parent = (Entity)parentList.get(j);

                //first we need to get the property being updated
                AssociateProperty current =
                    (AssociateProperty)parent.getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            propertyName);

                // update the property value
                AssociateProperty updated =
                    (AssociateProperty)current.clone();
                updated.setValue(null);

                // update the property
                ChangePropertyCommand cmd = new ChangePropertyCommand(
                        parent,
                        propertyGroup,
                        propertyName,
                        current,
                        updated);

                cmd.setErrorReporter(errorReporter);
                model.applyCommand(cmd);

            }

        }

        mapPanel.entityUpdateRequired();
    }

    /**
     * The entity was selected.
     *
     * @param selection The list of selected entities.  The last one is the latest.
     */
    public void selectionChangedDep(List<Selection> selection) {

//System.out.println("GT2DView.selectionChanged()");
//System.out.println("    associateMode: " + associateMode);

        if (associateMode) {
            return;
        }

        int size = selectedEntities.size();

        for(int i = 0; i < size; i++) {
            Entity e = selectedEntities.get(i);

            EntityWrapper wrapper = entityWrapperMap.get(e.getEntityID());

            // Wrapper will be null if this is the Location that is selected.
            if(wrapper != null)
                wrapper.setSelected(false);
        }

        selectedEntities.clear();

        if(selection.size() > 0) {
            // end the rotation action if there is one
            checkForRotation();
            mapPanel.setCursor(null);

            for (int i = 0; i < selection.size(); i++) {
                Selection s = selection.get(i);

                int id = s.getEntityID();
                EntityWrapper wrapper = entityWrapperMap.get(id);

                // Wrapper will be null if this is the Location that is selected.
                if (wrapper !=  null) {
                    wrapper.setSelected(true);
                    Entity entity = entityMap.get(id);

                    selectedEntities.add(entity);
                }
            }
        }

        mapPanel.entityUpdateRequired();
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
        // clear the location panel
        mapIsAvailable = false;
        locationSelected = false;

        // disable nav buttons
        pickAndPlaceButton.setEnabled(false);
        navigateButton.setEnabled(false);
        panButton.setEnabled(false);
        boundButton.setEnabled(false);
        isMetric.setEnabled(false);

        // reset state variables
        resetState();
        resetNavigateState();

        mapPanel.repaint();
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
    public void childAdded(int parent, int child) {

        Entity entity = entityMap.get(parent);
        Entity childEntity = entity.getChildAt(entity.getChildIndex(child));

        // add the child to the lookup maps
        vertexMap.put(child, parent);
        entityMap.put(child, childEntity);

        // register the property listener
        childEntity.addEntityPropertyListener(this);

        // update the view, any property update can effect the look of each entity
        mapPanel.entityUpdateRequired();

        miniFrame.repaint();
    }

    /**
     * A child was removed.
     *
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {

        // add the child to the lookup maps
        vertexMap.remove(child);
        entityMap.remove(child);

        // update the view, any property update can effect the look of each entity
        mapPanel.entityUpdateRequired();

    }

    /**
     * A child was inserted.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public void childInsertedAt(int parent, int child, int index) {

        Entity entity = entityMap.get(parent);
        Entity childEntity = entity.getChildAt(entity.getChildIndex(child));

        // add the child to the lookup maps
        vertexMap.put(child, parent);
        entityMap.put(child, childEntity);

        // register the property listener
        childEntity.addEntityPropertyListener(this);

        // update the view, any property update can effect the look of each entity
        mapPanel.entityUpdateRequired();

    }

    // ----------------------------------------------------------
    // Methods required by EntityPropertyListener interface
    // ----------------------------------------------------------


    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this an ongoing change or the final value?
     */
    //TODO Abstract
    public void propertyUpdated(
            int entityID,
            String propertySheet,
            String propertyName, boolean ongoing) {

        Entity entity = entityMap.get(entityID);

        // check to see if the entity is of the type PositionableEntity

        if (entity instanceof PositionableEntity) {

            if (entity.getType() == Entity.TYPE_WORLD) {
                // TODO: ignore.  What should we do?
                return;
            }

            if (entity instanceof VertexEntity) {

                mapPanel.entityUpdateRequired();
                return;

            }

            EntityWrapper eWrapper = entityWrapperMap.get(entityID);

            PositionableEntity posEntity = (PositionableEntity)entity;

            if (propertyName.equals(PositionableEntity.POSITION_PROP)) {

                double[] pos = new double[3];
                posEntity.getPosition(pos);

                convertWorldPosToScreenPos(pos, screenPos);

//System.out.println("propertyUpdated screenPos: " + java.util.Arrays.toString(screenPos));

                eWrapper.setWorldPosition(pos);
                eWrapper.setScreenPosition(screenPos[0], screenPos[1]);
                eWrapper.updateTransform();

            } else if (propertyName.equals(PositionableEntity.ROTATION_PROP)) {

                // TODO: What to do about full on rotations?
                tmpRot = new float[4];
                posEntity.getRotation(tmpRot);

                int angle = (int)Math.round(tmpRot[3] * 180.0f / Math.PI);

                eWrapper.setHeading(angle);
                eWrapper.updateTransform();

            } else if (propertyName.equals(PositionableEntity.SCALE_PROP)) {

                float[] size = new float[3];
                posEntity.getSize(size);

                float[] scale = new float[3];
                posEntity.getScale(scale);

//errorReporter.messageReport("Got scale change, new scale: " + scale[0] + " " + scale[2]);

                ToolRenderer toolImage =
                    entityRendererMapper.getRenderer(eWrapper.getEntity(),
                                                     currentPlane);
                imgWidth = toolImage.getWidth();
                imgHeight = toolImage.getHeight();

                calcScaleFactor(entity.isFixedAspect(),
                                imgWidth,
                                imgHeight,
                                size,
                                scale,
                                tmpScale,
                                tmpCenter,
                                entity.getType());

                iconCenterX = tmpCenter[0];
                iconCenterY = tmpCenter[1];

                // TODO: the center moving logic is causing issues with the matching 3D view.
//errorReporter.messageReport("Got size change, new scale: " + tmpScale[0] + " " + tmpScale[1]);
                eWrapper.setScale(tmpScale[0], tmpScale[1]);
                eWrapper.updateTransform();

                updateEntityScaleAndZoom();
            }

        }

        // update the view, any property update can effect the look of each entity
        mapPanel.entityUpdateRequired();

    }

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


    //---------------------------------------------------------
    // Method defined by ActionListener
    //---------------------------------------------------------

    /**
     * UI event handlers
     */
    public void actionPerformed(ActionEvent ae) {

        if(currentTool != null) currentTool.setCurrentView(currentPlane.toString());

        Object source = ae.getSource();
        if (source == pickAndPlaceButton) {
            if (previousMode == MouseMode.PLACEMENT) {

                if (currentTool != null) {

                    toolImage = getImage(currentTool.getIcon());
                    imgWidth = toolImage.getWidth(null);
                    imgHeight = toolImage.getHeight(null);

                    float[] size = currentTool.getSize();
                    float[] scale = currentTool.getScale();

                    //errorReporter.messageReport("Tool selected: " + imgWidth + " " + imgHeight);
                    if (imgWidth < 0 || imgHeight < 0) {
                        errorReporter.messageReport("Error processing icon: " + currentTool.getIcon());
                    }

                    calcScaleFactor(currentTool.isFixedAspect(),
                                    imgWidth,
                                    imgHeight,
                                    size,
                                    scale,
                                    tmpScale,
                                    tmpCenter,
                                    currentTool.getToolType());
                    iconScaleX = tmpScale[0];
                    iconScaleY = tmpScale[1];

                    iconCenterX = tmpCenter[0];
                    iconCenterY = tmpCenter[1];
                }

                setMode(MouseMode.PLACEMENT, false);
            } else if (previousMode == MouseMode.ASSOCIATE) {
                setMode(MouseMode.ASSOCIATE, false);
            } else {
                setMode(MouseMode.SELECTION, true);
            }
        } else if (source == navigateButton) {
            previousMode = currentMode;
            setMode(MouseMode.NAVIGATION, false);

        } else if (source == boundButton) {
            boundIsActive = true;
            panIsActive = false;
            mapPanel.setCursor(crossHairCursor);

        }  else if (source == panButton) {
            panIsActive = true;
            boundIsActive = false;
            mapPanel.setCursor(openHandCursor);

        } else if (source == navControl) {
            String command = ae.getActionCommand();
            if (command.equals(PanZoomControl.RESET_COMMAND)) {
                try {
                    ReferencedEnvelope env = baseContext.getLayerBounds();
                    if (env != null) {
                        mapArea = env;
                        mapPanel.reset();
                        zoomLevel = navControl.getMaximum();

                        setZoomLevel(zoomLevel);
                    }
                } catch (IOException ioe) {
                    errorReporter.errorReport("Couldn't reset Map View: ", ioe);
                }
            } else if (command.equals(PanZoomControl.ZOOM_IN_COMMAND)) {
                incrementZoomLevel(-1);
            } else if (command.equals(PanZoomControl.ZOOM_OUT_COMMAND)) {
                incrementZoomLevel(1);
            } else {
                // a panning function selected
                double distance = 0.0;
                double direction = 0.0;
                if (command.equals(PanZoomControl.PAN_UP_COMMAND)) {
                    distance = mapArea.getHeight() / 3.0;
                    direction = ViewUtils.PAN_UP_DIRECTION;

                } else if (command.equals(PanZoomControl.PAN_DOWN_COMMAND)) {
                    distance = mapArea.getHeight() / 3.0;
                    direction = ViewUtils.PAN_DOWN_DIRECTION;

                } else if (command.equals(PanZoomControl.PAN_LEFT_COMMAND)) {
                    distance = mapArea.getWidth() / 3.0;
                    direction = ViewUtils.PAN_LEFT_DIRECTION;

                } else if (command.equals(PanZoomControl.PAN_RIGHT_COMMAND)) {
                    distance = mapArea.getWidth() / 3.0;
                    direction = ViewUtils.PAN_RIGHT_DIRECTION;
                }
                // the new region of the map to display
                mapArea = viewUtils.pan(mapArea, direction, distance);
                updateEntityScaleAndZoom();
            }
        }
    }

    //----------------------------------------------------------
    // Methods required by KeyListener
    //----------------------------------------------------------


    public void keyPressed(KeyEvent ke) {

        int code = ke.getKeyCode();
        switch(code) {

            case KeyEvent.VK_ESCAPE:
                setMode(MouseMode.SELECTION, true);
                toolBarManager.setTool(null);
                ViewManager.getViewManager().disableAssociateMode();
                break;

            case KeyEvent.VK_SHIFT:
                shiftActive = true;
                break;
        }

        Entity entity = null;

        if (selectedEntities.size() >= 1) {
            //errorReporter.messageReport("Moving multiple items is not supported at this time.");
            entity = selectedEntities.get(0);
        }

        switch (currentMode) {
            case SELECTION:
                switch(code) {

                case KeyEvent.VK_DELETE:

                    if (entity != null &&
                        entity instanceof SegmentableEntity &&
                        ((SegmentableEntity)entity).isFixedLength() &&
                        ((SegmentableEntity)entity).getSelectedVertexID() >= 0) {

                        SegmentableEntity segmentEntity = (SegmentableEntity)entity;
                        int vertexID = segmentEntity.getSelectedVertexID();

                        if (!segmentEntity.isEnd(vertexID) && !segmentEntity.isStart(vertexID)) {

                            JOptionPane.showMessageDialog(this,
                                "Cannot delete internal segments in fixed mode.",
                                "Delete Action",
                                JOptionPane.WARNING_MESSAGE);

                            mapPanel.repaint();
                            return;
                        }

                    }
                    break;

                }

            case PLACEMENT:

                int toolType = 0;

                // end the rotation action if there is one
                checkForRotation();

                if (entity != null)
                    toolType = entity.getType();

                switch(code) {
                    case KeyEvent.VK_DOWN:
                        if (entity != null && !associateMode) {
                            if (toolType != Entity.TYPE_MODEL)
                                return;

                            int step;

                            if (shiftActive)
                                step = 1;
                            else
                                step = MOUSEWHEEL_STEPUP;

                            EntityWrapper wrapper =
                                entityWrapperMap.get(entity.getEntityID());

                            setHeading(wrapper.getHeading() + step);
                        }
                        break;

                    case KeyEvent.VK_UP:
                        if (entity != null && !associateMode) {
                            int step;

                            if (toolType != Entity.TYPE_MODEL)
                                return;

                            if (shiftActive)
                                step = 1;
                            else
                                step = MOUSEWHEEL_STEPUP;

                            EntityWrapper wrapper =
                                entityWrapperMap.get(entity.getEntityID());

                            setHeading(wrapper.getHeading() - step);
                        }
                        break;

                    case KeyEvent.VK_ESCAPE:

                        changeSelection(EMPTY_ENTITY_LIST);

                        break;
                }
                break;

            case NAVIGATION:

                double distance;
                double direction;

                switch (code) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_KP_UP:

                        distance = mapArea.getHeight() * 0.33;
                        direction = ViewUtils.PAN_UP_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_KP_DOWN:

                        distance = mapArea.getHeight() * 0.33;
                        direction = ViewUtils.PAN_DOWN_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_KP_LEFT:

                        distance = mapArea.getWidth() * 0.33;
                        direction = ViewUtils.PAN_LEFT_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_KP_RIGHT:

                        distance = mapArea.getWidth() * 0.33;
                        direction = ViewUtils.PAN_RIGHT_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_PAGE_UP:

                        distance = mapArea.getHeight() * 0.8;
                        direction = ViewUtils.PAN_UP_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_PAGE_DOWN:

                        distance = mapArea.getHeight() * 0.8;
                        direction = ViewUtils.PAN_DOWN_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_HOME:

                        distance = mapArea.getWidth() * 0.8;
                        direction = ViewUtils.PAN_LEFT_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_END:

                        distance = mapArea.getWidth() * 0.8;
                        direction = ViewUtils.PAN_RIGHT_DIRECTION;
                        mapArea = viewUtils.pan(mapArea, direction, distance);
                        updateEntityScaleAndZoom();
                        break;

                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_ADD:
                        incrementZoomLevel(-1);
                        break;

                    case KeyEvent.VK_MINUS:
                    case KeyEvent.VK_SUBTRACT:
                        incrementZoomLevel(1);
                        break;
                }
                break;

        } // switch(currentMode)
    }



    //----------------------------------------------------------
    // Methods required by MouseWheelListener
    //----------------------------------------------------------

    /**
     * Controls entity rotation and panel zoom
     *
     * @param mwe The mouse wheel event
     */
    public void mouseWheelMoved(MouseWheelEvent mwe) {

        int wheelRotation = mwe.getWheelRotation();

        switch (currentMode) {
            case SELECTION:
            case PLACEMENT:

                // placement should only have a single selected entity
                if (selectedEntities.size() <= 0) {
                    return;
                }

                Entity entity = selectedEntities.get(0);

                if (entity != null) {

                    int step;
                    int toolType = entity.getType();

                    if (!associateMode) {
                        //if (toolType == Tool.TYPE_WORLD ||
                        //        toolType == Tool.TYPE_MULTI_SEGMENT) {
                        if (toolType != Entity.TYPE_MODEL) {

                            return;

                        }

                        if (shiftActive) {
                            step = 1;
                        } else {
                            step = MOUSEWHEEL_STEPUP;
                        }
                        EntityWrapper eWrapper =
                            entityWrapperMap.get(entity.getEntityID());
                        setHeading(eWrapper.getHeading() + wheelRotation * step);
                    }
                }

                break;

            case NAVIGATION:
                // zoom
                incrementZoomLevel(wheelRotation);
                break;
        }

    }

    //----------------------------------------------------------
    // Methods required by MouseMotionListener
    //----------------------------------------------------------

    /**
     * Controls the placement of entities on the entity layer and
     * panning of the map area.
     *
     * @param me The MouseEvent
     */
    public void mouseDragged(MouseEvent me) {

//System.out.println("GT2DView.mouseDragged");
//System.out.println("    mode: " + currentMode);
//System.out.println("    entityDragging: " + entityDragging);

        Point currentMousePoint = me.getPoint();

        // update the rulers
        if(rulerEnabled){
            verticalRuler.updateMouseCoords(me.getX(), me.getY());
            horizontalRuler.updateMouseCoords(me.getX(), me.getY());
        }

        // the difference between the current and previous mouse position
        int deltaX = lastMousePoint.x - currentMousePoint.x;
        int deltaY = lastMousePoint.y - currentMousePoint.y;

        lastMousePoint = currentMousePoint;

        Entity entity = null;

        if (selectedEntities.size() >= 1) {
            //errorReporter.messageReport("Moving multiple items is not supported at this time.");
            entity = selectedEntities.get(0);
        }

        switch (currentMode) {
            case SELECTION:

                if (entity != null) {

                    // check whether an entity is being relocated
                    if (activeButton != MouseEvent.BUTTON1) {
                        return;
                    }

                    if (ignoreDrag) {
                        entityDragging = false;

                        // set a threshhold for move to fire warnings
                        int threshhold = 10;

                        // get the location of the selected vertex
                        int[] screenPosition = new int[2];
                        double[] worldPosition = new double[3];

                        if (entity instanceof PositionableEntity) {

                            if (entity instanceof SegmentableEntity &&
                                ((SegmentableEntity)entity).getSelectedVertexID()!= -1) {

                                worldPosition = ((SegmentableEntity)entity).getSelectedVertexPosition();
                            }

                        } else {
                            ((PositionableEntity)entity).getPosition(worldPosition);
                        }

                        convertWorldPosToScreenPos(worldPosition, screenPosition);

                        // get how far we've moved from the original position
                        deltaX = Math.abs(currentMousePoint.x - screenPosition[0]);
                        deltaY = Math.abs(currentMousePoint.y - screenPosition[1]);

                        // if we've moved too far, fire warning
                        if ((deltaX > threshhold) || (deltaY > threshhold)) {

                            if (entity instanceof SegmentableEntity) {

                                if (entity.getType() == Entity.TYPE_BUILDING) {

                                    entityDragging = false;

                                } else {

                                    SegmentableEntity segmentableEntity = (SegmentableEntity)entity;

                                    SegmentEntity segment = segmentableEntity.getSelectedSegment();
                                    if(segment != null) {

                                        // If we have a selected segment, then
                                        // insert a new vertex here and start the
                                        // drag process.

                                        // stack the commands together
                                        ArrayList<Command> commandList = new ArrayList<Command>();

                                        int startVertexID = segment.getStartID();
                                        int endVertexID = segment.getEndID();

                                        int vertexOrder = segmentableEntity.getChildIndex(startVertexID);

                                        // if this is the first segment then we need to
                                        // place the new after the first vertex
                                        int firstVertexID = segmentableEntity.getStartVertexID();
                                        int lastVertexID = segmentableEntity.getEndVertexID();
                                        if (firstVertexID == startVertexID || lastVertexID == endVertexID) {
                                            vertexOrder++;
                                        }

//System.out.println("    startVertexID: " + startVertexID);
//System.out.println("    firstVertexID: " + firstVertexID);
//System.out.println("    vertexOrder: " + vertexOrder);
//System.out.println("    endVertexID: " + endVertexID);

                                        // add the remove segment command
                                        RemoveSegmentCommand segmentCmd =
                                            new RemoveSegmentCommand(
                                                    segmentableEntity,
                                                    segment.getEntityID());
                                        segmentCmd.setErrorReporter(errorReporter);
                                        commandList.add(segmentCmd);

                                        // create the vertex command
                                        convertScreenPosToWorldPos(
                                                currentMousePoint.x,
                                                currentMousePoint.y,
                                                tmpPos, false);

                                        int middleVertexID = model.issueEntityID();

                                        VertexTool vertexTool =
                                            (VertexTool)segmentableEntity.getVertexTool();

                                        Entity newVertexEntity =
                                            entityBuilder.createEntity(
                                                model,
                                                middleVertexID,
                                                tmpPos,
                                                new float[] {0,1,0,0},
                                                vertexTool);

                                        AddVertexCommand vertexCmd =
                                            new AddVertexCommand(
                                                    segmentableEntity,
                                                    (VertexEntity)newVertexEntity,
                                                    vertexOrder);
                                        vertexCmd.setErrorReporter(errorReporter);
                                        commandList.add(vertexCmd);

                                        // update where it begins
                                        startPos[0] = tmpPos[0];
                                        startPos[1] = tmpPos[1];
                                        startPos[2] = tmpPos[2];

                                        // create the segment command
                                        int currentSegmentID = model.issueEntityID();

                                        SegmentTool segmentTool =
                                            (SegmentTool)segmentableEntity.getSegmentTool();

                                        SegmentEntity newSegmentEntity =
                                            (SegmentEntity)entityBuilder.createEntity(
                                                model,
                                                currentSegmentID,
                                                new double[] {0,0,0},
                                                new float[] {0,1,0,0},
                                                segmentTool);
                                        newSegmentEntity.setStartIndex(startVertexID);
                                        newSegmentEntity.setEndIndex(middleVertexID);

                                        AddSegmentCommand segmentCmd1 =
                                            new AddSegmentCommand(
                                                    segmentableEntity,
                                                    newSegmentEntity);
                                        commandList.add(segmentCmd1);

                                        // create the segment command
                                        currentSegmentID = model.issueEntityID();

                                        newSegmentEntity =
                                            (SegmentEntity)entityBuilder.createEntity(
                                                model,
                                                currentSegmentID,
                                                new double[] {0,0,0},
                                                new float[] {0,1,0,0},
                                                segmentTool);
                                        newSegmentEntity.setStartIndex(middleVertexID);
                                        newSegmentEntity.setEndIndex(endVertexID);

                                        segmentCmd1 =
                                            new AddSegmentCommand(
                                                    segmentableEntity,
                                                    newSegmentEntity);
                                        commandList.add(segmentCmd1);

                                        MultiCommand stack = new MultiCommand(
                                                commandList,
                                                "Split Segment -> " + currentSegmentID);
                                        stack.setErrorReporter(errorReporter);
                                        model.applyCommand(stack);

                                        List<Entity> selected2 = new ArrayList<Entity>();
                                        ((SegmentableEntity)entity).setSelectedVertexID(middleVertexID);
                                        ((SegmentableEntity)entity).setSelectedSegmentID(-1);
                                        selected2.add(entity);

                                        changeSelection(selected2);

                                     } else {

                                        JOptionPane.showMessageDialog(this,
                                            "Movement of entire segmented objects are not supported at this time.",
                                            "Placement Action",
                                            JOptionPane.WARNING_MESSAGE);
                                        mapPanel.repaint();
                                        return;

                                    }

                                    if (((SegmentableEntity)entity).isFixedLength()) {
                                        JOptionPane.showMessageDialog(this,
                                            "Movement of internal vertex of fixed length fences are not supported at this time.",
                                            "Placement Action",
                                            JOptionPane.WARNING_MESSAGE);
                                        mapPanel.repaint();
                                        return;
                                    }

                                }

                            } else {
                                entityDragging = false;
                            }

                        } else {
                            return;
                        }
                    }

                    if (!entityDragging) {
                        entityDragging = true;
                        setMode(MouseMode.PLACEMENT, false);
                        return;
                    }
                } else {
                    if (boundInProgress) {
                         // the drag is controlling a bounds selection
                        boundRectangle.width = currentMousePoint.x - boundRectangle.x;
                        boundRectangle.height = currentMousePoint.y - boundRectangle.y;

                        mapPanel.setCursor(crossHairCursor);

                        mapPanel.repaint();
                    } else {
                        navControl.mouseDragged(me);
                    }
                }

                break;

            case PLACEMENT:

                if (entityDragging) {

                    if (entity instanceof SegmentableEntity) {

                        double[] position = new double[3];
                        double[] position2 = new double[3];

                        SegmentableEntity segmentEntity = (SegmentableEntity)entity;

                        if (entity.getType() == Entity.TYPE_BUILDING) {

                            VertexEntity vertex = segmentEntity.getSelectedVertex();
                            SegmentEntity segment = segmentEntity.getSelectedSegment();

                            if (vertex != null) {

                                double[] vertexPos = new double[3];
                                vertex.getPosition(vertexPos);

                                position[0] = vertexPos[0] - deltaX*mapScale;
                                position[1] = vertexPos[1];
                                position[2] = vertexPos[2] - deltaY*mapScale;

                                if (!inTransient) {
                                    transactionID = model.issueTransactionID();
                                    inTransient = true;
                                }

                                // TODO: Need to calc velo
                                MoveVertexTransientCommand cmd = new MoveVertexTransientCommand(
                                    model,
                                	transactionID,
                                    vertex,
                                    position,
                                    new float[3]);
                                cmd.setErrorReporter(errorReporter);
                                model.applyCommand(cmd);

                                // Clear all entities of highlights except current one
                                Entity[] entities = model.getModelData();

                                int len = entities.length;
                                for(int i=0; i < len; i++) {
                                    if (entities[i] == null)
                                        continue;

                                    entities[i].setHighlighted(false);
                                    if (entities[i] instanceof SegmentableEntity) {
                                        ((SegmentableEntity)entities[i]).setHighlightedVertexID(-1);
                                    }

                                }

                                // find out if we need to highlight another vertex
                                EntitySearchReturn over_entity =
                                    findEntity(currentMousePoint.x, currentMousePoint.y);
                                EntityWrapper eWrapper = over_entity.getEntityWrapper();

                                ArrayList<Entity> selectedList = 
                                    seletionHelper.getSelectedList();
                                
                                if(selectedList.size() > 0) {
                                    Entity targetEntity = selectedList.get(0);
                                    Entity associateEntity = model.getEntity(eWrapper.getEntityID());
    
                                    if (eWrapper != null &&
                                        associateEntity.getEntityID() == targetEntity.getEntityID()) {
    
                                        Entity overEntity = eWrapper.getEntity();
    
                                        if (overEntity instanceof SegmentableEntity) {
                                            int overVertexID = over_entity.getVertexID();
    
                                            // Highlight the one found
                                            if (vertex.getEntityID() != overVertexID) {
                                                ((SegmentableEntity)entity).setHighlightedVertexID(overVertexID);
                                            }
    
                                        }
    
                                    }
                                }
                            } else if (segment != null) {

                                // stack the commands together
                                ArrayList<Command> commandList = new ArrayList<Command>();

                                // move the segment by moving the points at each end

                                VertexEntity vertex1 =
                                    segmentEntity.getVertex(segment.getStartID());
                                vertex1.getPosition(position);

                                if (!inTransient) {
                                    transactionID = model.issueTransactionID();

                                    // set the start position to the current position,
                                    // in case of multiple moves on the same selection
                                    startPos[0] = position[0];
                                    startPos[1] = position[1];
                                    startPos[2] = position[2];

                                }

                                position[0] -= deltaX*mapScale;
                                position[2] -= deltaY*mapScale;

                                MoveVertexTransientCommand cmd1 =
                                    new MoveVertexTransientCommand(
                                    		model,
                                            transactionID,
                                            vertex1,
                                            position,
                                            new float[3]);

                                cmd1.setErrorReporter(errorReporter);
                                commandList.add(cmd1);

                                VertexEntity vertex2 =
                                    segmentEntity.getVertex(segment.getEndIndex());
                                vertex2.getPosition(position2);

                                if (!inTransient) {
                                    inTransient = true;

                                    // set the start position to the current position,
                                    // in case of multiple moves on the same selection
                                    startPos2[0] = position2[0];
                                    startPos2[1] = position2[1];
                                    startPos2[2] = position2[2];

                                }

                                position2[0] -= deltaX*mapScale;
                                position2[2] -= deltaY*mapScale;

                                MoveVertexTransientCommand cmd2 =
                                    new MoveVertexTransientCommand(
                                    		model,
                                            transactionID,
                                            vertex2,
                                            position2,
                                            new float[3]);

                                cmd2.setErrorReporter(errorReporter);
                                commandList.add(cmd2);

                                MultiTransientCommand stack =
                                    new MultiTransientCommand(
                                        commandList,
                                        "Move Segment -> " + segment.getEntityID());
                                stack.setErrorReporter(errorReporter);
                                model.applyCommand(stack);

                            } else {

                                if (entity instanceof PositionableEntity) {
                                    ((PositionableEntity)entity).getPosition(position);
                                }

                                position[0] -= deltaX * mapScale;
                                position[2] -= deltaY * mapScale;

                                if (!inTransient) {
                                    transactionID = model.issueTransactionID();
                                    inTransient = true;
                                }

                                // TODO: Need to calc velo
                                MoveEntityTransientCommand cmd = new MoveEntityTransientCommand(
                                    model,
                                    transactionID,
                                    entity.getEntityID(),
                                    position,
                                    new float[3]);
                                cmd.setErrorReporter(errorReporter);
                                model.applyCommand(cmd);

                            }

                            mapPanel.entityUpdateRequired();

                        } else {

                            if (((SegmentableEntity)entity).isFixedLength()) {

                                ArrayList<VertexEntity> vertices = ((SegmentableEntity)entity).getVertices();

                                if (segmentEntity.isStart(segmentEntity.getSelectedVertexID())) {
                                    vertices.get(1).getPosition(position);
                                } else if (segmentEntity.isEnd(segmentEntity.getSelectedVertexID())) {
                                    vertices.get(vertices.size() - 2).getPosition(position);
                                }

                                int screenPos[] = new int[2];
                                convertWorldPosToScreenPos(position, screenPos);

                                float x = 0;
                                float y = 0;

                                x = currentMousePoint.x - screenPos[0];
                                y = currentMousePoint.y - screenPos[1];

                                float len = (float) Math.sqrt(x * x + y * y);

                                if (len > 0) {
                                    x = x / len;
                                    y = y / len;
                                }

                                x = screenPos[0] + Math.round(x * segmentLength / Math.abs((mapScale)));
                                y = screenPos[1] + Math.round(y * segmentLength / Math.abs((mapScale)));

                                convertScreenPosToWorldPos(Math.round(x), Math.round(y), position, false);

                            } else {

                                VertexEntity vertex = segmentEntity.getSelectedVertex();
                                double[] vertexPos = new double[3];
                                vertex.getPosition(vertexPos);

                                position[0] = vertexPos[0] - deltaX*mapScale;
                                position[1] = vertexPos[1];
                                position[2] = vertexPos[2] - deltaY*mapScale;
                            }


                            if (!inTransient) {
                                transactionID = model.issueTransactionID();
                                inTransient = true;
                            }

                            //currentEntity.getEntity().getSelectedVertexID()
                            // TODO: Need to calc velo
                            int vertexID = ((SegmentableEntity)entity).getSelectedVertexID();
                            VertexEntity vertex = ((SegmentableEntity)entity).getVertex(vertexID);

                            MoveVertexTransientCommand cmd = new MoveVertexTransientCommand(
                            	model,
                                transactionID,
                                vertex,
                                position,
                                new float[3]);
                            cmd.setErrorReporter(errorReporter);
                            model.applyCommand(cmd);

                        }

                    } else {

                        double[] position = new double[] {0, 0, 0};
                        if (entity instanceof PositionableEntity) {
                            ((PositionableEntity)entity).getPosition(position);
                        }

                        // update axis based on the current set face
                        switch (currentPlane) {
                            case TOP:
                                position[0] -= deltaX*mapScale;
                                position[2] -= deltaY*mapScale;
                                break;
                            case RIGHT:
                                position[1] += deltaY*mapScale;
                                position[2] += deltaX*mapScale;
                                break;
                            case LEFT:
                                position[1] += deltaY*mapScale;
                                position[2] -= deltaX*mapScale;
                                break;
                            case FRONT:
                                position[0] -= deltaX*mapScale;
                                position[1] += deltaY*mapScale;

                                break;

                        }

                        if (!inTransient) {
                            transactionID = model.issueTransactionID();
                            inTransient = true;
                        }

                        // TODO: Need to calc velo
                        MoveEntityTransientCommand cmd = new MoveEntityTransientCommand(
                            model,
                            transactionID,
                            entity.getEntityID(),
                            position,
                            new float[3]);
                        cmd.setErrorReporter(errorReporter);
                        model.applyCommand(cmd);

                    }
                }
                break;

            case NAVIGATION:
                if (!mapIsAvailable) {
                    return;
                }

                if (boundInProgress) {
                    // the drag is controlling a bounds selection
                    boundRectangle.width = currentMousePoint.x - boundRectangle.x;
                    boundRectangle.height = currentMousePoint.y - boundRectangle.y;

                    mapPanel.repaint();

                } else if (panInProgress) {
                    // the drag is controlling a pan operation

                    Rectangle panelBounds = mapPanel.getBounds();

                    // the dimensions of the panel
                    double panelWidth = panelBounds.getWidth();
                    double panelHeight = panelBounds.getHeight();

                    // the offset from the center of the panel, by the
                    // drag amount from the last mouse position
                    double centerOffsetX = (double)(panelBounds.width/2 + deltaX);
                    double centerOffsetY = (double)(panelBounds.height/2 + deltaY);

                    // the map extent
                    double mapWidth = mapArea.getWidth();
                    double mapHeight = mapArea.getHeight();

                    // calculate the current map coordinates that will become the new center
                    double mapX = (centerOffsetX * mapWidth / panelWidth) + mapArea.getMinX();
                    double mapY =  mapArea.getMaxY() - (centerOffsetY * mapHeight / panelHeight);

                    double mapWidth2 = mapWidth / 2.0;
                    double mapHeight2 = mapHeight / 2.0;

                    // the new region of the map to display - dragged to follow the mouse
                    mapArea = new ReferencedEnvelope(
                        mapX - mapWidth2,
                        mapX + mapWidth2,
                        mapY - mapHeight2,
                        mapY + mapHeight2,
                        crs);

                    updateEntityScaleAndZoom();
                } else {
                    navControl.mouseDragged(me);
                }
                break;

        }

    }

    /**
     * Controls the drawing parameters of segmented entities on the
     * entity layer and produces mouse over world coordinates.
     *
     * @param me The MouseEvent
     */
    public void mouseMoved(MouseEvent me) {
        inMousePressed = false;

        Point currentMousePoint = me.getPoint();

        switch (currentMode) {

        case SELECTION:
        case PLACEMENT:
            mouseX = me.getX();
            mouseY = me.getY();

            Entity entity = null;

            if (selectedEntities.size() >= 1) {
                //errorReporter.messageReport("Moving multiple items is not supported at this time.");
                entity = selectedEntities.get(0);
            }

//System.out.println("GT2DView.mouseMoved");
//System.out.println("    entity: " + entity);
//System.out.println("    multiSegmentOp: " + multiSegmentOp);

            if (entity != null &&
                multiSegmentOp == true &&
                entity instanceof SegmentableEntity &&
                ((SegmentableEntity)entity).isVertexSelected()) {

                EntitySearchReturn over_entity =
                    findEntity(currentMousePoint.x, currentMousePoint.y);
                EntityWrapper eWrapper = over_entity.getEntityWrapper();

                int vertexID = -1;

                if (eWrapper != null) {
                    vertexID = over_entity.getVertexID();
                }

                // Clear all entities of highlights except current one
                Entity[] entities = model.getModelData();

                int len = entities.length;

                for(int i=0; i < len; i++) {
                    if (entities[i] == null)
                        continue;

                    if (entities[i] instanceof SegmentableEntity) {
                        ((SegmentableEntity)entities[i]).setHighlightedVertexID(-1);
                    }

                }

                // Highlight the one found
                if (entity != null && vertexID != -1) {
                    ((SegmentableEntity)entity).setHighlightedVertexID(vertexID);
                }

                mapPanel.entityUpdateRequired();
            }

            if ((entity != null)
                && entity instanceof SegmentableEntity
                && ((SegmentableEntity)entity).isFixedLength()
                && multiSegmentOp == true
                && ((SegmentableEntity)entity).isVertexSelected()) {

                float x = 0;
                float y = 0;

                convertWorldPosToScreenPos(
                        ((SegmentableEntity)entity).getEndPosition(),
                        lastSegmentPosition);

                x = mouseX - lastSegmentPosition[0];
                y = mouseY - lastSegmentPosition[1];

                float len = (float) Math.sqrt(x * x + y * y);

                if (len > 0) {
                    x = x / len;
                    y = y / len;
                }

                x = lastSegmentPosition[0] + Math.round(x * segmentLength / Math.abs((mapScale)));
                y = lastSegmentPosition[1] + Math.round(y * segmentLength / Math.abs((mapScale)));

                // adjust for the icon size
                float imgWidth = toolSegmentImage.getWidth(null)* iconScaleX;
                float imgHeight = toolSegmentImage.getHeight(null) * iconScaleY;

                x = x - Math.round(imgWidth / 2);
                y = y - Math.round(imgHeight / 2);

                toolTransform.setToTranslation(x, y);
                toolTransform.scale(iconScaleX,iconScaleY);

            } else {

                toolTransform.setToTranslation(mouseX-iconCenterX,mouseY-iconCenterY);
                toolTransform.scale(iconScaleX,iconScaleY);
            }

            if (toolImage != null) {
                mapPanel.repaint();
            }

            break;

        case NAVIGATION:
            boolean overControl = navControl.contains(currentMousePoint);
            if (overControl) {
                if (isOverNavControl) {
                    navControl.mouseMoved(me);
                } else {
                    navControl.mouseEntered(me);
                    isOverNavControl = true;
                }
            } else {
                if (isOverNavControl) {
                    navControl.mouseExited(me);
                    if (boundIsActive) {
                        mapPanel.setCursor(crossHairCursor);
                    } else if (panIsActive) {
                        mapPanel.setCursor(openHandCursor);
                    }
                }
                isOverNavControl = false;
            }

            break;

        }

        // calculate the world coordinates of the mouse position
        if (mapIsAvailable) {

            // the current mouse position
            double _mouseX = (double)currentMousePoint.x;
            double _mouseY = (double)currentMousePoint.y;

            // the map extent
            double mapWidth = mapArea.getWidth();
            double mapHeight = mapArea.getHeight();

            Rectangle panelBounds = mapPanel.getBounds();

            // the dimensions of the panel
            double panelWidth = panelBounds.getWidth();
            double panelHeight = panelBounds.getHeight();

            // translate the mouse position to world coordinates
            double x = (_mouseX * mapWidth / panelWidth) + mapArea.getMinX();
            double y = -(((_mouseY * mapHeight) / panelHeight) - mapArea.getMaxY());
            if(currentPlane == ViewingFrustum.Plane.TOP ) y *= -1;
            position.x = x;
            position.y = y;

            if (coordFormat != null) {
                statusField.setText(coordFormat.format(position));
            }

            // update the rulers
            if(rulerEnabled){
                verticalRuler.updateMouseCoords(me.getX(), me.getY());
                horizontalRuler.updateMouseCoords(me.getX(), me.getY());
            }
        }

        if (!mapPanel.isFocusOwner()) {
            mapPanel.requestFocusInWindow();
        }
    }

    //----------------------------------------------------------
    // Methods required by MouseListener
    //----------------------------------------------------------

    /**
     * Controls centering the map on the mouse click position
     *
     * @param me The mouse event
     */
    public void mouseClicked(MouseEvent me) {

//System.out.println("GT2DView.mouseClicked");
//System.out.println("    mode: " + currentMode);
//System.out.println("    inMousePressed: " + inMousePressed);

        if (activeButton == MouseEvent.BUTTON1) {

            EntitySearchReturn entReturn;
            EntityWrapper eWrapper;

            switch (currentMode) {
                case ASSOCIATE:
                    break;

                case SELECTION:
                    if (inMousePressed) {
                        return;
                    }

                    boundInProgress = false;

                    // look for a single entity
                    entReturn = findEntity(lastMousePoint.x, lastMousePoint.y);
                    eWrapper = entReturn.getEntityWrapper();

                    int vertexID = entReturn.getVertexID();
                    int segmentID = entReturn.getSegmentID();

                    // nothing found, set selection to location
                    if (eWrapper == null) {

                        changeSelection(EMPTY_ENTITY_LIST);

                        setMode(MouseMode.SELECTION, true);
                    } else {
                        setSelectedEntity(eWrapper, segmentID, vertexID);
                    }
                    break;

                case PLACEMENT:
                    break;

                case NAVIGATION:

                    if (!mapIsAvailable) {
                        return;
                    }

                    Point currentMousePoint = me.getPoint();

                    if (navControl.contains(currentMousePoint)) {
                        navControl.mouseClicked(me);

                    } else if (boundIsActive) {
                        return;

                    } else if (panIsActive && (me.getClickCount() > 1)) {
                        // on a double click

                        int button = me.getButton();
                        int delta = 0;
                        if (button == MouseEvent.BUTTON1) {
                            if (zoomLevel > navControl.getMinimum()) {
                                delta = -1;
                            } else {
                                // can't zoom in any farther
                                return;
                            }
                        } else if (button == MouseEvent.BUTTON3) {
                            if (zoomLevel < navControl.getMaximum()) {
                                delta = 1;
                            } else {
                                // can't zoom out any farther
                                return;
                            }
                        }
                        // and with 'room to zoom'

                        // pan to center on the mouse position
                        mapArea = viewUtils.panToPoint(
                            mapArea,
                            mapPanel.getBounds(),
                            currentMousePoint);

                        // then zoom in or out depending on the button that was clicked

                        incrementZoomLevel(delta);
                    }
                    break;
            }
        } else if (activeButton == MouseEvent.BUTTON3) {
            setMode(MouseMode.SELECTION, true);
            ViewManager.getViewManager().disableAssociateMode();
            toolBarManager.setTool(null);
        }
        inMousePressed = false;
    }

    /**
     * Initiates adding and manipulating entities
     *
     * @param me The mouse event
     */
    public void mousePressed(MouseEvent me) {

//System.out.println("GT2DView.mousePressed");
//System.out.println("    mode: " + currentMode);

        inMousePressed = true;

        // save the location for potential panning operations
        activeButton = me.getButton();
        lastMousePoint = me.getPoint();

        // end the rotation action if there is one
        checkForRotation();

        if (activeButton == MouseEvent.BUTTON1) {

            switch (currentMode) {
                case ASSOCIATE:
                    break;

                case SELECTION:
                    // see if nothing is selected, if not then
                    // start the bound process for multi-select
System.out.println("(" + lastMousePoint.x + ", " + lastMousePoint.y + ")");
                    // look for a single entity
                    EntitySearchReturn entReturn =
                        findEntity(lastMousePoint.x, lastMousePoint.y);
                    EntityWrapper eWrapper = entReturn.getEntityWrapper();

                    int vertexID = entReturn.getVertexID();
                    int segmentID = entReturn.getSegmentID();

//System.out.println("    eWrapper: " + eWrapper);

                    // nothing found, set selection to location
                    if (eWrapper == null) {

                        boundInProgress = true;
                        boundRectangle.x = lastMousePoint.x;
                        boundRectangle.y = lastMousePoint.y;
                        boundRectangle.width = 0;
                        boundRectangle.height = 0;

                        changeSelection(EMPTY_ENTITY_LIST);

                    } else {

                        setSelectedEntity(eWrapper, segmentID, vertexID);

                    }
                    break;

                case PLACEMENT:
                    break;

                case NAVIGATION:
                    if (navControl.contains(lastMousePoint)) {
                        navControl.mousePressed(me);

                    } else if (boundIsActive) {
                        boundInProgress = true;
                        boundRectangle.x = lastMousePoint.x;
                        boundRectangle.y = lastMousePoint.y;
                        boundRectangle.width = 0;
                        boundRectangle.height = 0;

                    } else {
                        panInProgress = true;
                        mapPanel.setCursor(closedHandCursor);
                    }

                    break;
            }

            // Set focus so keys will work
            mapPanel.requestFocusInWindow();

        } else if(activeButton == MouseEvent.BUTTON3) {
            setMode(MouseMode.SELECTION, true);
            ViewManager.getViewManager().disableAssociateMode();
            toolBarManager.setTool(null);
        }
    }

    /**
     * Terminates entity manipulation
     *
     * @param me The mouse event
     */
    public void mouseReleased(MouseEvent me) {
        if(currentTool!=null)currentTool.setCurrentView(currentPlane.toString());
        Point currentMousePoint = me.getPoint();

        if(me.isPopupTrigger()) {
            EntitySearchReturn entReturn = findEntity(me.getX(), me.getY());
            EntityWrapper eWrapper = entReturn.getEntityWrapper();

            if(eWrapper != null) {

                Entity entity = eWrapper.getEntity();
                if (entity instanceof SegmentableEntity) {

                    SegmentableEntity segmentEntity = (SegmentableEntity)entity;

                    segmentEntity.setSelectedVertexID(entReturn.getVertexID());
                    segmentEntity.setSelectedSegmentID(entReturn.getSegmentID());

                }

                // Populate a new menu
                JPopupMenu menu = new JPopupMenu();

                DeleteAction delete =
                    new DeleteAction(true, null, model);
                delete.setEnabled(true);

                // get the world coords to use
                convertScreenPosToWorldPos(me.getX(), me.getY(), tmpPos, false);
                AddControlPointAction addControlPoint =
                    new AddControlPointAction(true, null, model, eWrapper.getEntityID(), tmpPos);

                HighlightAssociatesAction highlight =
                    new HighlightAssociatesAction(true, null, model, eWrapper.getEntityID());
                highlight.setEnabled(true);

                menu.add(highlight);
                menu.add(new ShowChildrenAction(eWrapper, mapPanel));
                menu.addSeparator();
                menu.add(addControlPoint);
                menu.add(delete);

                menu.show(mapPanel, me.getX(), me.getY());

                return;
            }
        }

        // If it wasn't a popup trigger that was over an entity, then we
        // can do other actions.
        if (activeButton == MouseEvent.BUTTON1) {

            Entity entity = null;

            if (selectedEntities.size() >= 1) {
                //errorReporter.messageReport("Moving multiple items is not supported at this time.");
                entity = selectedEntities.get(0);
                //System.out.println("    entityID: " + entity.getEntityID());
            }

            EntitySearchReturn entReturn;
            EntityWrapper eWrapper;


            switch (currentMode) {
                case ASSOCIATE:

                    entReturn =
                        findEntity(lastMousePoint.x, lastMousePoint.y);

                    eWrapper =
                        entReturn.getEntityWrapper();

                    if (eWrapper == null) {

                        errorReporter.messageReport("Cannot Associate, No Entity Found");

                    } else {

                        ArrayList<Entity> selectedList = 
                            seletionHelper.getSelectedList();
                        
                        if(selectedList.size() > 0) {
                            Entity targetEntity = selectedList.get(0);
                            Entity associateEntity = model.getEntity(eWrapper.getEntityID());
    
                            for (int i = 0; i < validTools.length; i++) {
    
                                if (validTools[i].equals(associateEntity.getCategory())) {
    
                                    if (propertyGroup.equals(SegmentableEntity.BRIDGE_VERTICES_ACTION)) {
    
                                        // find out if we need to highlight another vertex
                                        EntitySearchReturn over_entity =
                                            findEntity(currentMousePoint.x, currentMousePoint.y);
                                        eWrapper = over_entity.getEntityWrapper();
    
                                        SegmentableEntity segmentEntity = (SegmentableEntity)entity;
                                        int vertexID = segmentEntity.getSelectedVertexID();
                                        int highlightedVertexID = -1;
                                        if (eWrapper != null &&
                                            associateEntity.getEntityID() == targetEntity.getEntityID()) {
    
                                            Entity overEntity = eWrapper.getEntity();
                                            if (overEntity instanceof SegmentableEntity) {
                                                if (vertexID != over_entity.getVertexID()) {
                                                    highlightedVertexID = over_entity.getVertexID();
                                                }
                                            }
                                        }
    
                                        if (highlightedVertexID >= 0) {
                                            addSegment(
                                                    segmentEntity,
                                                    vertexID,
                                                    highlightedVertexID);
                                        }
    
                                    } else {
    
                                        //first we need to get the property being updated
                                        AssociateProperty current =
                                            (AssociateProperty)targetEntity.getProperty(
                                                    Entity.DEFAULT_ENTITY_PROPERTIES,
                                                    propertyName);
    
                                        // update the property value
                                        AssociateProperty updated =
                                            (AssociateProperty)current.clone();
                                        updated.setValue(associateEntity);
    
                                        // update the property
                                        ChangePropertyTransientCommand cmd =
                                            new ChangePropertyTransientCommand(
                                                targetEntity,
                                                propertyGroup,
                                                propertyName,
                                                updated);
    
                                        cmd.setErrorReporter(errorReporter);
                                        model.applyCommand(cmd);
    
                                        // now update the child with parent information
                                        Object targetValue =
                                            associateEntity.getProperty(
                                                    Entity.ASSOCIATED_ENTITIES,
                                                propertyName);
    
                                        ArrayList<Entity> targetList = new ArrayList<Entity>();
                                        if (targetValue != null) {
                                            targetList =  (ArrayList<Entity>)targetValue;
                                        }
                                        targetList.add(targetEntity);
    
                                        associateEntity.setProperty(
                                                Entity.ASSOCIATED_ENTITIES,
                                                propertyName,
                                                targetList, false);
    
                                    }
    
                                    break;
                                }
                            }
                        }

                    }

                    // disable association mode
                    ViewManager viewManager = ViewManager.getViewManager();
                    viewManager.disableAssociateMode();

                    setMode(MouseMode.SELECTION, false);

                    break;

                case SELECTION:
                     if (boundInProgress) {

                        int width = boundRectangle.width;
                        int height = boundRectangle.height;
                        if ((width == 0) || (height == 0)) {
                            // can't select a zero area
                            boundInProgress = false;
                            setMode(MouseMode.SELECTION, false);
                            return;
                        }

                        //TODO: get all entities in selected region
//                        System.out.println("SELECT MULTIPLE ENTITIES HERE!");
                        selectedEntities = findEntity(boundRectangle);

                        boundInProgress = false;
                        setMode(MouseMode.SELECTION, false);
                    }
                     // Commented out by Sang Park
                     // Below code was forcing the vertex to be selected.   When a line segment vertex is
                     // first selected, whole segment needs to be selected.  And when it is selected again then the
                     // vertex should be selected.
                     /* else {

                        // look for a single entity
                        entReturn = findEntity(lastMousePoint.x, lastMousePoint.y);
                        eWrapper = entReturn.getEntityWrapper();

                        int vertexID = entReturn.getVertexID();
                        int segmentID = entReturn.getSegmentID();

                        // nothing found, set selection to location
                        if (eWrapper == null) {
                            boundInProgress = true;
                            boundRectangle.x = lastMousePoint.x;
                            boundRectangle.y = lastMousePoint.y;
                            boundRectangle.width = 0;
                            boundRectangle.height = 0;

                            changeSelection(EMPTY_ENTITY_LIST);
                        } else {
                            setSelectedEntity(eWrapper, segmentID, vertexID);
                        }
                    }*/

                    break;

                case PLACEMENT:

                    // end the rotation action if there is one
                    checkForRotation();

                    ignoreDrag = false;

                    if (entityDragging && (entity != null)) {

                        // Finalize drag items around
                        entityDragging = false;

                        // the difference between the current and previous mouse position
                        int deltaX = lastMousePoint.x - currentMousePoint.x;
                        int deltaY = lastMousePoint.y - currentMousePoint.y;

                        //lastMousePoint = currentMousePoint;
                        double[] position = new double[3];
                        double[] position2 = new double[3];

                        if (entity instanceof SegmentableEntity) {

                            // Clear all entities of highlights except current one
                            Entity[] entities = model.getModelData();

                            int len = entities.length;
                            for(int i=0; i < len; i++) {
                                if (entities[i] == null)
                                    continue;

                                entities[i].setHighlighted(false);
                                if (entities[i] instanceof SegmentableEntity) {
                                    ((SegmentableEntity)entities[i]).setHighlightedVertexID(-1);
                                }

                            }

                            SegmentableEntity segmentEntity = (SegmentableEntity)entity;

                            // always move vertices if selected
                            if (segmentEntity.isVertexSelected()) {

                                // check to see if we need to merge these points
                                int vertexID = segmentEntity.getSelectedVertexID();

                                double[] current = segmentEntity.getSelectedVertexPosition();

                                position[0] = current[0] - deltaX*mapScale;
                                position[2] = current[2] - deltaY*mapScale;

                                VertexEntity vertex = segmentEntity.getVertex(vertexID);

                                MoveVertexCommand cmd = new MoveVertexCommand(
                                	model,
                                    transactionID,
                                    vertex,
                                    position,
                                    startPos);

                                cmd.setErrorReporter(errorReporter);
                                model.applyCommand(cmd);

                                // set the start position to the current position,
                                // in case of multiple moves on the same selection
                                startPos = position;

                            }

                            if (entity.getType() == Entity.TYPE_BUILDING) {

                                SegmentEntity segment = segmentEntity.getSelectedSegment();

                                if (segment != null) {

                                    // stack the commands together
                                    ArrayList<Command> commandList = new ArrayList<Command>();

                                    // move the segment by moving the points at each end

                                    VertexEntity vertex =
                                        segmentEntity.getVertex(segment.getStartID());
                                    vertex.getPosition(position);

                                    position[0] -= deltaX*mapScale;
                                    position[2] -= deltaY*mapScale;

                                    MoveVertexCommand cmd1 =
                                        new MoveVertexCommand(
                                        		model,
                                                transactionID,
                                                vertex,
                                                position,
                                                startPos);

                                    cmd1.setErrorReporter(errorReporter);
                                    commandList.add(cmd1);

                                    // set the start position to the current position,
                                    // in case of multiple moves on the same selection
                                    startPos = position;

                                    VertexEntity vertex2 =
                                        segmentEntity.getVertex(segment.getEndID());
                                    vertex2.getPosition(position2);

                                    position2[0] -= deltaX*mapScale;
                                    position2[2] -= deltaY*mapScale;

                                    MoveVertexCommand cmd2 =
                                        new MoveVertexCommand(
                                        		model,
                                                transactionID,
                                                vertex2,
                                                position2,
                                                startPos2);

                                    cmd2.setErrorReporter(errorReporter);
                                    commandList.add(cmd2);

                                    MultiCommand stack = new MultiCommand(
                                            commandList,
                                            "Move Segment -> " + segment.getEntityID());
                                    stack.setErrorReporter(errorReporter);
                                    model.applyCommand(stack);

                                    // set the start position to the current position,
                                    // in case of multiple moves on the same selection
                                    startPos2 = position2;

                                } else {

                                    if (entity instanceof PositionableEntity) {
                                        ((PositionableEntity)entity).getPosition(position);
                                    }

                                    // update axis based on the current set face
                                    switch (currentPlane) {
                                        case TOP:
                                            position[0] -= deltaX*mapScale;
                                            position[2] -= deltaY*mapScale;
                                            break;
                                        case RIGHT:
                                            position[1] += deltaY*mapScale;
                                            position[2] += deltaX*mapScale;
                                            break;
                                        case LEFT:
                                            position[1] += deltaY*mapScale;
                                            position[2] -= deltaX*mapScale;
                                            break;
                                        case FRONT:
                                            position[0] -= deltaX*mapScale;
                                            position[1] += deltaY*mapScale;

                                            break;

                                    }

//System.out.println("position: " + java.util.Arrays.toString(position));

                                    MoveEntityCommand cmd = new MoveEntityCommand(
                                            model,
                                            transactionID,
                                            entity.getEntityID(),
                                            position,
                                            startPos);
                                    cmd.setErrorReporter(errorReporter);
                                    model.applyCommand(cmd);

                                    // set the start position to the current position,
                                    // in case of multiple moves on the same selection
                                    startPos = position;

                                }

                            }

                        } else {

                            if (entity instanceof PositionableEntity) {

                                ((PositionableEntity)entity).getPosition(position);

                                // update axis based on the current set face
                                switch (currentPlane) {
                                    case TOP:
                                        position[0] -= deltaX*mapScale;
                                        position[2] -= deltaY*mapScale;
                                        break;
                                    case RIGHT:
                                        position[1] += deltaX*mapScale;
                                        position[2] += deltaY*mapScale;
                                        break;
                                    case LEFT:
                                        position[1] += deltaY*mapScale;
                                        position[2] -= deltaX*mapScale;
                                        break;
                                    case FRONT:
                                        position[0] -= deltaX*mapScale;
                                        position[1] += deltaY*mapScale;

                                        break;

                                }

                                MoveEntityCommand cmd = new MoveEntityCommand(
                                    model,
                                    transactionID,
                                    entity.getEntityID(),
                                    position,
                                    startPos);
                                cmd.setErrorReporter(errorReporter);
                                model.applyCommand(cmd);

                                // set the start position to the current position,
                                // in case of multiple moves on the same selection
                                startPos = position;
                            }
                        }


                        inTransient = false;
                        setMode(MouseMode.SELECTION, false);

                    } else if (!entityDragging && (currentTool != null)) {
                        //int id = 0;
                        EntityBuilder builder = getEntityBuilder();
                        Entity newEntity;

                        int entityID = model.issueEntityID();

                        // Place new items
                        switch(currentTool.getToolType()) {
                            case Entity.TYPE_MODEL:
                            case Entity.TYPE_BUILDING:
                                convertScreenPosToWorldPos(
                                        currentMousePoint.x,
                                        currentMousePoint.y,
                                        tmpPos,
                                        true);

                                startPos[0] = tmpPos[0];
                                startPos[1] = tmpPos[1];
                                startPos[2] = tmpPos[2];

                                tmpRot[0] = 0;
                                tmpRot[1] = 1;
                                tmpRot[2] = 0;
                                tmpRot[3] = 0;

                                startRot = tmpRot.clone();
                                adjustRotation(true, startRot);
                                adjustRotation(true, tmpRot);

                                newEntity = builder.createEntity(model, entityID,
                                        tmpPos, tmpRot, currentTool);

                                AddEntityCommand cmd =
                                    new AddEntityCommand(model, newEntity);
                                model.applyCommand(cmd);

                                multiSegmentOp = false;

                                List<Entity> selected = new ArrayList<Entity>();
                                selected.add(newEntity);

                                changeSelection(selected);
                                break;

                            case Entity.TYPE_WORLD:
                                // Need to clear current world?  Or at least change size of icons
                                errorReporter.messageReport("Not implemented yet");
                                break;

                            case Entity.TYPE_MULTI_SEGMENT:
                                // Add the entity the first time through

System.out.println("TYPE_MULTI_SEGMENT");
                                SegmentableEntity segmentableEntity = null;
                                if (entity instanceof SegmentableEntity) {
                                    segmentableEntity = (SegmentableEntity)entity;
                                }

                                if (segmentableEntity != null &&
                                        segmentableEntity.getToolName().equals(currentTool.getName())) {

                                    if (segmentableEntity.isVertexSelected()) {

                                        int startVertexID = ((SegmentableEntity)entity).getSelectedVertexID();
                                        int endVertexID = -1;

                                        //MultiSegmentTool mst = (MultiSegmentTool) entity.getTool();

                                        if (((SegmentableEntity)entity).isLine()) {
                                            SegmentableEntity fence = ((SegmentableEntity)entity);
                                            startVertexID = fence.getVertexID(fence.getEndPosition());

                                            addVertexAndSegment(
                                                    entity,
                                                    currentMousePoint.x,
                                                    currentMousePoint.y,
                                                    -1,
                                                    startVertexID);

                                        } else if (((SegmentableEntity)entity).getHighlightedVertexID() == -1) {

                                            addVertexAndSegment(
                                                    entity,
                                                    currentMousePoint.x,
                                                    currentMousePoint.y,
                                                    -1,
                                                    startVertexID);

                                        } else {
                                            endVertexID = ((SegmentableEntity)entity).getHighlightedVertexID();

                                            addSegment(entity, startVertexID, endVertexID);
                                        }


                                        mapPanel.entityUpdateRequired();
                                        break;

                                    } else if (segmentableEntity.isSegmentSelected()) {

                                        // TODO: combine the add vertex and split segment

                                        // remove the current segment
                                        // add the new vertex
                                        // add segment 1
                                        // add segment 2

                                        // stack the commands together
                                        ArrayList<Command> commandList = new ArrayList<Command>();

                                        SegmentEntity segment =
                                            ((SegmentableEntity)entity).getSelectedSegment();

                                        int startVertexID = segment.getStartID();
                                        int endVertexID = segment.getEndID();

                                        int vertexOrder = ((SegmentableEntity)entity).getChildIndex(startVertexID);

                                        // if this is the first segment then we need to
                                        // place the new after the first vertex
                                        int firstVertexID = ((SegmentableEntity)entity).getStartVertexID();
                                        int lastVertexID = ((SegmentableEntity)entity).getEndVertexID();
                                        if (firstVertexID == startVertexID || lastVertexID == endVertexID) {
                                            vertexOrder++;
                                        }

                                        // add the remove segment command
                                        RemoveSegmentCommand segmentCmd =
                                            new RemoveSegmentCommand(
                                                    segmentableEntity,
                                                    segment.getEntityID());
                                        segmentCmd.setErrorReporter(errorReporter);
                                        commandList.add(segmentCmd);


                                        // create the vertex command
                                        convertScreenPosToWorldPos(
                                                currentMousePoint.x,
                                                currentMousePoint.y,
                                                tmpPos,
                                                true);
                                        /*Author Jonathon Hubbard
                                         * Formula used to place the vertex
                                         * correctly on the line
                                         * First retrieve the index of the two vertexs of the line
                                         * then retrieve the positions
                                         */
                                        double[] startPos = new double[3];
                                        double[] endPos = new double[3];
                                        int startIndex=segmentableEntity.getChildIndex(startVertexID);
                                        int endIndex=segmentableEntity.getChildIndex(endVertexID);


                                        ((VertexEntity)
                                                 ((SegmentableEntity)entity)
                                                    .getChildAt(startIndex))
                                                        .getPosition(startPos);
                                        ((VertexEntity)
                                                  ((SegmentableEntity)entity)
                                                        .getChildAt(endIndex))
                                                          .getPosition(endPos);

                                        /* The full forumla for this process is:
                                         * tempPoint - startPoint - u(endPoint - startPoint)] dot (endPoint - startPoint)
                                         * So first we Solve for delta X and Delta z since y is always the same
                                         * then we solve for u
                                         */
                                        double xDelta=startPos[0]-endPos[0];
                                        double zDelta=startPos[2]-endPos[2];
                                        double u=(( tmpPos[0]-startPos[0])*xDelta+
                                                ( tmpPos[2]-startPos[2])*zDelta)/
                                                (Math.pow(xDelta,2)+Math.pow(zDelta,2));

                                        //Finally we take the equation of the line and subsistute
                                        //u*delta or u(endPoint - startPoint)
                                        tmpPos[0]=startPos[0]+u*xDelta;
                                        tmpPos[2]=startPos[2]+u*zDelta;
// System.out.println("x: "+tmpPos[0]+" y: "+tmpPos[1]);
                                        //TODO
                                        //Change the currentMousePoint to the new location


                                        int middleVertexID = model.issueEntityID();

                                        VertexTool vertexTool =
                                            (VertexTool)segmentableEntity.getVertexTool();

                                        Entity newVertexEntity =
                                            entityBuilder.createEntity(
                                                model,
                                                middleVertexID,
                                                tmpPos,
                                                new float[] {0,1,0,0},
                                                vertexTool);

                                        AddVertexCommand vertexCmd =
                                            new AddVertexCommand(
                                                    segmentableEntity,
                                                    (VertexEntity)newVertexEntity,
                                                    vertexOrder);
                                        vertexCmd.setErrorReporter(errorReporter);
                                        commandList.add(vertexCmd);

                                        // create the segment command
                                        int currentSegmentID = model.issueEntityID();

                                        SegmentTool segmentTool =
                                            (SegmentTool)segmentableEntity.getSegmentTool();

                                        SegmentEntity newSegmentEntity =
                                            (SegmentEntity)entityBuilder.createEntity(
                                                model,
                                                currentSegmentID,
                                                new double[] {0,0,0},
                                                new float[] {0,1,0,0},
                                                segmentTool);
                                        newSegmentEntity.setStartIndex(startVertexID);
                                        newSegmentEntity.setEndIndex(middleVertexID);

                                        AddSegmentCommand segmentCmd1 =
                                            new AddSegmentCommand(
                                                    segmentableEntity,
                                                    newSegmentEntity);
                                        commandList.add(segmentCmd1);

                                        // create the segment command
                                        currentSegmentID = model.issueEntityID();

                                        newSegmentEntity =
                                            (SegmentEntity)entityBuilder.createEntity(
                                                model,
                                                currentSegmentID,
                                                new double[] {0,0,0},
                                                new float[] {0,1,0,0},
                                                segmentTool);
                                        newSegmentEntity.setStartIndex(middleVertexID);
                                        newSegmentEntity.setEndIndex(endVertexID);

                                        segmentCmd1 =
                                            new AddSegmentCommand(
                                                    segmentableEntity,
                                                    newSegmentEntity);
                                        commandList.add(segmentCmd1);


                                        MultiCommand stack = new MultiCommand(
                                                commandList,
                                                "Split Segment -> " + currentSegmentID);
                                        stack.setErrorReporter(errorReporter);
                                        model.applyCommand(stack);

                                        List<Entity> selected2 = new ArrayList<Entity>();
                                        ((SegmentableEntity)entity).setSelectedVertexID(middleVertexID);
                                        ((SegmentableEntity)entity).setSelectedSegmentID(-1);
                                        selected2.add(entity);

                                        changeSelection(selected2);

                                        mapPanel.entityUpdateRequired();
                                        break;

                                    }

                                } else {

                                    convertScreenPosToWorldPos(currentMousePoint.x,
                                            currentMousePoint.y, tmpPos, true);

                                    newEntity =
                                        builder.createEntity(model, entityID, tmpPos,
                                                new float[] {0,1,0,0}, currentTool);

                                    // add the base entity
                                    AddEntityCommand entityCmd =
                                        new AddEntityCommand(
                                                model,
                                                newEntity);
                                    entityCmd.setErrorReporter(errorReporter);

                                    // stack the commands together
                                    ArrayList<Command> commandList = new ArrayList<Command>();
                                    commandList.add(entityCmd);

                                    segmentLength = ((SegmentableTool)currentTool).getSegmentLength();

                                    boolean createShape =
                                        ((SegmentableTool)currentTool).isCreateDefaultShape();

                                    if (!createShape) {

                                        // add the vertex entity
                                        int endVertexID = model.issueEntityID();

                                        VertexTool vertexTool =
                                            (VertexTool)segmentableEntity.getVertexTool();

                                        Entity newVertexEntity =
                                            entityBuilder.createEntity(
                                                model,
                                                endVertexID,
                                                tmpPos,
                                                new float[] {0,1,0,0},
                                                vertexTool);

                                        AddVertexCommand vertexCmd =
                                            new AddVertexCommand(
                                                    (SegmentableEntity)newEntity,
                                                    (VertexEntity)newVertexEntity,
                                                    -1);
                                        vertexCmd.setErrorReporter(errorReporter);

                                        commandList.add(vertexCmd);

                                        ((SegmentableEntity)newEntity).setSelectedVertexID(endVertexID);
                                        ((SegmentableEntity)newEntity).setHighlightedVertexID(-1);

                                    }

                                    MultiCommand stack = new MultiCommand(
                                            commandList,
                                            "Add Entity -> " + entityID);
                                    stack.setErrorReporter(errorReporter);
                                    model.applyCommand(stack);


                                    List<Entity> selected2 = new ArrayList<Entity>();
                                    selected2.add(newEntity);

                                    changeSelection(selected2);

                                    mapPanel.entityUpdateRequired();

                                }

                                break;
                            default:
                                errorReporter.messageReport("Unhandled tooltype: " + Tool.mapType(currentTool.getToolType()));
                        }
                    }

                    break;

                case NAVIGATION:

                    if (boundInProgress) {

                        int width = boundRectangle.width;
                        int height = boundRectangle.height;
                        if ((width == 0) || (height == 0)) {
                            // can't select a zero area
                            boundInProgress = false;
                            mapPanel.repaint();
                            return;
                        }
                        double x_min = (double)boundRectangle.x;
                        double y_min = (double)boundRectangle.y;
                        double x_max;
                        double y_max;
                        // rearrange the coords in case the drag has been
                        // working in a negative direction
                        if (width < 0) {
                            x_min += width;
                            x_max = x_min - width;
                        } else {
                            x_max = x_min + width;
                        }
                        if (height < 0) {
                            y_min += height;
                            y_max = y_min - height;
                        } else {
                            y_max = y_min + height;
                        }

                        double x_dim = (x_max - x_min);
                        double x_center = x_max - x_dim/2;

                        double y_dim = (y_max - y_min);
                        double y_center = y_max - y_dim/2;

                        Rectangle panelBounds = mapPanel.getBounds();

                        // pan to center the map on the center of the selected area
                        mapArea = viewUtils.panToPoint(
                            mapArea,
                            panelBounds,
                            new Point((int)x_center, (int)y_center));

                        int[] minXY=new int[3];
                        int[] maxXY=new int[3];
                        double[] screenPos = new double[3];
                        screenPos[0]=mapArea.getMinX();
                        screenPos[1]=mapArea.getMinY();
                        screenPos[2]=mapArea.getMinY();

                        convertWorldPosToScreenPos(screenPos, minXY);
                        screenPos[0]=mapArea.getMaxX();
                        screenPos[1]=mapArea.getMaxY();
                        screenPos[2]=mapArea.getMaxY();
                        convertWorldPosToScreenPos(screenPos, maxXY);


                        // the dimensions of the panel
                        double panelWidth = panelBounds.getWidth();
                        double panelHeight = panelBounds.getHeight();

                        // the map extent
                        double mapWidth = mapArea.getWidth();
                        double mapHeight = mapArea.getHeight();

                        // calculate the map width & height of the selected area
                        double boundWidth = x_dim * mapWidth / panelWidth;
                        double boundHeight = y_dim * mapHeight / panelHeight;

                        // pick the smaller, add a bit
                        //double targetMapSpan = Math.min(boundWidth, boundHeight) * 1.2;
                        // no, lets pick the larger......
                        double targetMapSpan = Math.max(boundWidth, boundHeight) * 1.2;

                        // determine the zoom increments necessary for the target
                        // span to fit into the map area.
                        int levels = 0;
                        double zoomWidth = mapWidth;
                        while(zoomWidth > targetMapSpan) {
                            zoomWidth /= zoomFactor;
                            levels--;
                        }
                        boundInProgress = false;
                        if (!navControl.contains(currentMousePoint)) {
                            mapPanel.setCursor(crossHairCursor);
                        }

                        // calculate the zoom map area
                        incrementZoomLevel(levels);

                    } else if (panInProgress) {
                        panInProgress = false;
                        if (!navControl.contains(currentMousePoint)) {
                            mapPanel.setCursor(openHandCursor);
                        }
                    } else {
                        navControl.mouseReleased(me);
                        if (!navControl.contains(currentMousePoint)) {
                            if (boundIsActive) {
                                mapPanel.setCursor(crossHairCursor);
                            } else if (panIsActive) {
                                mapPanel.setCursor(openHandCursor);
                            }
                        }
                    }

                    break;
            }

        }

    }

    /**
     * Resets the cursor if an entity tool is active
     *
     * @param me The mouse event
     */
    public void mouseEntered(MouseEvent me) {

        switch (currentMode) {
            case SELECTION:
                insideMap = true;

                if (boundIsActive) {
                    mapPanel.setCursor(crossHairCursor);
                }

                mapPanel.repaint();
                break;

            case PLACEMENT:
                insideMap = true;
                mapPanel.repaint();
                break;

            case NAVIGATION:
                isOverNavControl = false;
                if (boundIsActive) {
                    mapPanel.setCursor(crossHairCursor);
                } else {
                    mapPanel.setCursor(openHandCursor);
                }
                break;
        }

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

    /**
     * Initialization of the map reader.
     */
    // a separate thread to allow the UI to refresh the
    // progress bar while the reader is initialized
    public void run() {

//System.out.println("*** STARTING THREAD ***");

        // try to retrieve from the classpath
        FileLoader fileLookup = new FileLoader();
        Object[] file = fileLookup.getFileURL(url_string);
        URL iconURL = (URL)file[0];
        InputStream iconStream = (InputStream)file[1];

//System.out.println("    url_string: " + url_string);
//System.out.println("    iconURL: " + iconURL);
//System.out.println("    iconURL.getProtocol: " + iconURL.getProtocol());

        String contentType = "directory";

        try {
            URLConnection conn = iconURL.openConnection();
            contentType = conn.getContentType();
        } catch (Exception e) {
            errorReporter.errorReport("Bad URL!", e);
        }

        URI uri = null;

        if (iconURL.getProtocol().equals("file")) {
            try {
                uri = iconURL.toURI();
            } catch (URISyntaxException urise) {
                errorReporter.errorReport("Bad URL!", urise);
            }

        } else {
            URL url = null;
            try {
                File dir = new File(System.getProperty("user.dir"));
                URL dirURL = dir.toURI().toURL();
                url = new URL(dirURL, url_string);
            } catch (MalformedURLException mue) {
                errorReporter.errorReport("Bad URL!", mue);
            }
            if (url == null) {
                setIsLoading(false);
                return;
            }
            try {
                uri = url.toURI();
            } catch (URISyntaxException urise) {
                errorReporter.errorReport("Bad URL!", urise);
            }

        }

        if (uri == null) {
            setIsLoading(false);
            return;
        }

        AbstractGridCoverage2DReader gridCoverReader = null;
        try {
            if (contentType.startsWith("image")) {
                // if a file - assume that it's an image file and
                // use the world reader

                // check to see if the file exists
                File check = new File(uri);
                if (check.exists()) {

                    gridCoverReader = new WorldImageReader(iconURL);

                } else {
                    // grab the files out of the JAR and try that
                    String tempdir = System.getProperty("java.io.tmpdir");

                    if ( !(tempdir.endsWith("/") || tempdir.endsWith("\\")) ) {
                        tempdir = tempdir + System.getProperty("file.separator");
                    }


                    // get the name of the image
                    int pos1 = url_string.lastIndexOf("/") + 1;
                    String baseDirectory = url_string.substring(0, pos1);
                    String imageFile = url_string.substring(pos1);

                    pos1 = imageFile.lastIndexOf(".");
                    String imageName = imageFile.substring(0, pos1);
                    String extName = imageFile.substring(pos1);

                    // copy the image file
                    File tempImage = new File(tempdir + imageName + extName);
                    tempImage.deleteOnExit();
                    try {
                        DataOutputStream out =
                            new DataOutputStream(
                                    new BufferedOutputStream(
                                            new FileOutputStream(tempImage)));
                        int c;
                        while((c = iconStream.read()) != -1) {
                            out.writeByte(c);
                        }
                        iconStream.close();
                        out.close();
                    }
                    catch(IOException e) {
                        System.err.println("Error Writing/Reading Streams.");
                    }

                    // copy the project file
                    file = fileLookup.getFileURL(baseDirectory + imageName + ".prj");
                    iconURL = (URL)file[0];
                    iconStream = (InputStream)file[1];

                    File tempFile = new File(tempdir + imageName + ".prj");
                    tempFile.deleteOnExit();
                    try {
                        DataOutputStream out =
                            new DataOutputStream(
                                    new BufferedOutputStream(
                                            new FileOutputStream(tempFile)));
                        int c;
                        while((c = iconStream.read()) != -1) {
                            out.writeByte(c);
                        }
                        iconStream.close();
                        out.close();
                    }
                    catch(IOException e) {
                        System.err.println("Error Writing/Reading Streams.");
                    }

                    // copy the world file
                    file = fileLookup.getFileURL(baseDirectory + imageName + ".wld");
                    iconURL = (URL)file[0];
                    iconStream = (InputStream)file[1];

                    tempFile = new File(tempdir + imageName + ".wld");
                    tempFile.deleteOnExit();
                    try {
                        DataOutputStream out =
                            new DataOutputStream(
                                    new BufferedOutputStream(
                                            new FileOutputStream(tempFile)));
                        int c;
                        while((c = iconStream.read()) != -1) {
                            out.writeByte(c);
                        }
                        iconStream.close();
                        out.close();
                    }
                    catch(IOException e) {
                        System.err.println("Error Writing/Reading Streams.");
                    }

                    // create the image reader
                    gridCoverReader = new WorldImageReader(tempImage);

                }

            } else {
                // otherwise, check with the directory for a particular file
                String baseURL = iconURL.toExternalForm();
                String path = baseURL + "pyramid.shp";

                Object[] checkFile = fileLookup.getFileURL(path);
                URL checkURL = (URL)checkFile[0];
                InputStream checkStream = (InputStream)checkFile[1];

                if (checkURL != null) {
                    // if a shapefile exists - assume that this is a single mosiac level
                    gridCoverReader = new ImageMosaicReader(checkStream, null);
                } else {

                    path = baseURL + "pyramid.properties";

                    checkFile = fileLookup.getFileURL(path);
                    checkURL = (URL)checkFile[0];
                    checkStream = (InputStream)checkFile[1];

                    if (checkURL != null) {
                        // if a properties file exists - assume that this is a pyramid
                        gridCoverReader = new ImagePyramidReader(checkStream, null);
                    }

                }
            }
        } catch (IOException ioe) {
            errorReporter.errorReport("File Error!", ioe);
        }

        if (gridCoverReader == null) {
            System.out.println("Failed to load grid reader!!!  url: " + iconURL.getPath());
            // no reader, no joy - punt.
            setIsLoading(false);
            return;
        }

        // the bounds of the mapped area
        GeneralEnvelope ge = gridCoverReader.getOriginalEnvelope();

        // the coordinate reference system is defined in the projection
        // file and is read in by the image reader
        crs = ge.getCoordinateReferenceSystem();
        viewUtils = new ViewUtils(crs);

        // coordinate formatter
        coordFormat = new CoordinateFormat();
        coordFormat.setCoordinateReferenceSystem(crs);

        // the initial map area encompasses the entire bounds of the
        // referenced image and is calculated from values in the world file
        //mapArea = new ReferencedEnvelope(ge, crs);
        mapArea = new ReferencedEnvelope(ge);

        GridCoverage gridCover = null;
        try {
            gridCover = gridCoverReader.read(null);
        } catch (Exception e) {
            errorReporter.errorReport("Grid Error!", e);
        }

        // the rendering context for the background map coverage image
        baseContext = new DefaultMapContext(crs);

        StyleFactory factory = CommonFactoryFinder.getStyleFactory(null);
        StyleBuilder sb = new StyleBuilder(factory);
        RasterSymbolizer rs = sb.createRasterSymbolizer();
        rasterStyle = sb.createStyle(rs);

        baseContext.addLayer(gridCoverReader, rasterStyle);

        // rem: is this vestigal ??? ///////////////////////////////
        // set up the panel dimensions to have a common scale
        double width = mapArea.getWidth();
        double height = mapArea.getHeight();
        int ratio = (int)(512 * height / width);
        imageSize = new Dimension(512, ratio);

        ////////////////////////////////////////////////////////////
        // configure the state of the UI controls

        boolean isPickModeSelected = pickAndPlaceButton.isSelected();
        boolean isNavModeSelected = navigateButton.isSelected();

        // default to navigation mode if a mode has not yet been chosen
        if (!(isNavModeSelected || isPickModeSelected)) {
            navigateButton.setSelected(true);
            currentMode = MouseMode.NAVIGATION;
        }

        pickAndPlaceButton.setEnabled(true);
        navigateButton.setEnabled(true);

        boolean isPanFuncSelected = panButton.isSelected();
        boolean isBoundFuncSelected = boundButton.isSelected();

        // default to pan navigation function if a function has not yet been chosen
        if (!(isPanFuncSelected || isBoundFuncSelected)) {
            panButton.setSelected(true);
            panIsActive = true;
        }

        if (currentMode == MouseMode.NAVIGATION) {
            panButton.setEnabled(true);
            boundButton.setEnabled(true);
            isMetric.setEnabled(true);
        } else {
            panButton.setEnabled(false);
            boundButton.setEnabled(false);
            isMetric.setEnabled(false);
        }

        setIsLoading(false);

        zoomLevel = navControl.getMaximum();
        navControl.setValue(zoomLevel);

        switch (currentMode) {
            case SELECTION:
                mapPanel.setCursor(null);
                break;

            case PLACEMENT:
                mapPanel.setCursor(null);
                break;

            case NAVIGATION:
                if (boundIsActive) {
                    mapPanel.setCursor(crossHairCursor);
                } else if (panIsActive) {
                    mapPanel.setCursor(openHandCursor);
                } else {
                    errorReporter.messageReport("GT2DView: Unknown Navigation Function");
                }

                break;
            default:
                errorReporter.messageReport("GT2DView: Unknown Operation Mode");
        }

        ////////////////////////////////////////////////////////////
        // display the map
        mapPanel.coverageUpdateRequired();

        mapIsAvailable = true;

    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Set the mappings between entities and renderers. A null value clears
     * the current mapper instance and returns to the default mapping.
     *
     * @param mapper The mapper instance to use or null
     */
    public void setEntityRendererMapper(EntityRendererMapper mapper) {
        entityRendererMapper = mapper;

        if(entityRendererMapper == null)
            entityRendererMapper = new DefaultEntityRendererMapper();
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

    /**
     * Set the selected entity out of line segments and vertices in the scene.
     *
     * @param entityWrapper A holder entity data.
     * @param segmentID Line segment ID.
     * @param vertexID Vertex point ID.
     * @author Rex Melton, Commented and edited by Sang Park
     */
    protected void setSelectedEntity(EntityWrapper entityWrapper, int segmentID, int vertexID) {

        // save the starting rotation, so we can perform an undo
        startRot[0] = 0;
        startRot[1] = 1;
        startRot[2] = 0;
        startRot[3] = (float) (entityWrapper.getHeading() / 180.0f) * (float) Math.PI;
        adjustRotation(true, startRot);

        // save the starting position, so we can perform an undo
        Entity entity = entityWrapper.getEntity();
        startPos = new double[3];

        if (entity instanceof SegmentableEntity) {
            ((SegmentableEntity)entity).setSelectedVertexID(vertexID);
            ((SegmentableEntity)entity).setSelectedSegmentID(segmentID);

            multiSegmentOp = true;
            if (((SegmentableEntity)entity).getSelectedVertexID() != -1) {
                // use the vertex position
                //System.out.println("    startPos: " + startPos[0] +  ", " + startPos[2]);

                startPos = ((SegmentableEntity)entity).getSelectedVertexPosition();
            } else {
                // otherwise use the entity position
                ((PositionableEntity)entity).getPosition(startPos);
            }
        } else {
            // otherwise use the entity position
            ((PositionableEntity)entity).getPosition(startPos);
        }

//System.out.println("GT2DView.setSelectedEntity");
//System.out.println("    entity: " + entity);
//System.out.println("    segmentID: " + segmentID);
//System.out.println("    vertexID: " + vertexID);
//System.out.println("    startPos: " + startPos[0] + ", " + startPos[1] + ", " + startPos[2]);

        if (vertexID != -1) {

            VertexEntity vertex = ((SegmentableEntity)entity).getVertex(vertexID);
            vertex.addEntityPropertyListener(this);

        }

        String iconURL = entity.getIconURL(" ");
        Image image = (Image) imageMap.get(iconURL);

        if (image == null) {
            image = getImage(iconURL);
        }

        int imgWidth = image.getWidth(null);
        int imgHeight = image.getHeight(null);

        float[] size = new float[3];
        ((PositionableEntity)entity).getSize(size);

        float[] scale = new float[3];
        ((PositionableEntity)entity).getScale(scale);

        calcScaleFactor(entity.isFixedAspect(), imgWidth, imgHeight,
                size, scale, tmpScale, tmpCenter, entity.getType());

        iconScaleX = tmpScale[0];
        iconScaleY = tmpScale[1];

        iconCenterX = tmpCenter[0];
        iconCenterY = tmpCenter[1];

        // stop allowing dragging for now
        ignoreDrag = false;
        if (entity instanceof SegmentableEntity) {

            if (vertexID == -1) {
                ignoreDrag = true;
            } else if (((SegmentableEntity)entity).isFixedLength()) {

                SegmentableEntity segmentEntity = ((SegmentableEntity)entity);

                if (!segmentEntity.isEnd(vertexID) && !segmentEntity.isStart(vertexID)) {
                    ignoreDrag = true;
                }
            }
        }

        List<Entity> selected = new ArrayList<Entity>();
        selected.add(entity);

        changeSelection(selected);
        setMode(MouseMode.SELECTION, true);
    }

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
    protected void addSegment(Entity entity, int startVertexID, int endVertexID){

        if (entity instanceof SegmentableEntity) {
            multiSegmentOp = true;

            SegmentableEntity segmentEntity = (SegmentableEntity)entity;

            int currentSegmentID = model.issueEntityID();

            // Before adding check to make sure it doesn't exist
            //if (currentSegmentID > 0) {

                //if (!((SegmentableEntity)entity).isVertexSelected()) {
                //    ((SegmentableEntity)entity).setSelectedSegmentID(fenceSegment.getLastSegmentID());
                //}
            //}

            SegmentTool segmentTool =
                (SegmentTool)segmentEntity.getSegmentTool();

            SegmentEntity newSegmentEntity =
                (SegmentEntity)entityBuilder.createEntity(
                    model,
                    currentSegmentID,
                    new double[] {0,0,0},
                    new float[] {0,1,0,0},
                    segmentTool);
            newSegmentEntity.setStartIndex(startVertexID);
            newSegmentEntity.setEndIndex(endVertexID);

            AddSegmentCommand segmentCmd1 =
                new AddSegmentCommand(
                        segmentEntity,
                        newSegmentEntity);

            // TODO: How to deal with undo sets?

            model.applyCommand(segmentCmd1);

            segmentEntity.setSelectedVertexID(endVertexID);
            segmentEntity.setHighlightedVertexID(-1);

            List<Entity> selected = new ArrayList<Entity>();
            selected.add(entity);

            changeSelection(selected);

            mapPanel.entityUpdateRequired();

        }

    }

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
    protected void addVertexAndSegment(
            Entity entity,
            int x,
            int y,
            int index,
            int startVertexID) {

        if (entity instanceof SegmentableEntity) {
            multiSegmentOp = true;

            SegmentableEntity segmentableEntity = (SegmentableEntity)entity;

            if (segmentableEntity.isFixedLength()) {

                float newx;
                float newy;

                if (((SegmentableEntity)entity).getSelectedVertexID() == -1) {

                    x = mouseX;
                    y = mouseY;

                } else {
                    convertWorldPosToScreenPos(segmentableEntity.getEndPosition(),
                                               lastSegmentPosition);

                    newx = mouseX - lastSegmentPosition[0];
                    newy = mouseY - lastSegmentPosition[1];

                    float len = (float) Math.sqrt(newx * newx + newy * newy);

                    if (len > 0) {
                        newx = newx / len;
                        newy = newy / len;
                    }

                    x = (int) (lastSegmentPosition[0] +
                        Math.round(newx * segmentLength / Math.abs((mapScale))));
                    y = (int) (lastSegmentPosition[1] +
                        Math.round(newy * segmentLength / Math.abs((mapScale))));
                }
            }

            convertScreenPosToWorldPos(x, y, tmpPos, false);

            // Before adding check to make sure it doesn't exist
            if (((SegmentableEntity)entity).getSelectedVertexID() > 0) {

                if (!segmentableEntity.isVertexSelected()) {
                    segmentableEntity.setSelectedVertexID(segmentableEntity.getEndVertexID());
                }
                if (segmentableEntity.contains(tmpPos)) {

                    JOptionPane.showMessageDialog(this,
                            "You cannot add a vertex to a position that already has a vertex defined.",
                            "Placement Action",
                            JOptionPane.WARNING_MESSAGE);
                    mapPanel.repaint();
                    return;
                }
            }

            // create the vertex command
            int endVertexID = model.issueEntityID();

            VertexTool vertexTool =
                (VertexTool)segmentableEntity.getVertexTool();

            Entity newVertexEntity =
                entityBuilder.createEntity(
                    model,
                    endVertexID,
                    tmpPos,
                    new float[] {0,1,0,0},
                    vertexTool);

            AddVertexCommand vertexCmd =
                new AddVertexCommand(
                        segmentableEntity,
                        (VertexEntity)newVertexEntity,
                        index);
            vertexCmd.setErrorReporter(errorReporter);

            // create the segment command
            int currentSegmentID = model.issueEntityID();

            SegmentTool segmentTool =
                (SegmentTool)segmentableEntity.getSegmentTool();

            SegmentEntity newSegmentEntity =
                (SegmentEntity)entityBuilder.createEntity(
                    model,
                    currentSegmentID,
                    new double[] {0,0,0},
                    new float[] {0,1,0,0},
                    segmentTool);
            newSegmentEntity.setStartIndex(startVertexID);
            newSegmentEntity.setEndIndex(endVertexID);

            AddSegmentCommand segmentCmd =
                new AddSegmentCommand(
                        segmentableEntity,
                        newSegmentEntity);
            segmentCmd.setErrorReporter(errorReporter);

            // stack the commands together
            ArrayList<Command> commandList = new ArrayList<Command>();
            commandList.add(vertexCmd);
            commandList.add(segmentCmd);

            MultiCommand stack = new MultiCommand(
                    commandList,
                    "Add Segment -> " + currentSegmentID);
            stack.setErrorReporter(errorReporter);
            model.applyCommand(stack);

            // set the selection
            segmentableEntity.setSelectedVertexID(endVertexID);
            segmentableEntity.setHighlightedVertexID(-1);

            List<Entity> selected = new ArrayList<Entity>();
            selected.add(entity);

            changeSelection(selected);

        }

    }

    /**
     * Find an entity given a screen location.  This will return
     * the closest entity.
     *
     * @param The x position
     * @param The y position
     * @return The entity search return
     */
    protected EntitySearchReturn findEntity(final int x, final int y) {

        if (wrapperList.size() == 0)
            return new EntitySearchReturn(null, -1, -1);

        int vertexID = -1;
        int segmentID = -1;
        EntityWrapper closest = null;
        // The closest helpers (ie: isHelper() returns TRUE)
        ArrayList<EntityWrapper> closestHelpers =
            new ArrayList<EntityWrapper>();
        ArrayList<Double> helperDistances = new ArrayList<Double>();

        // The closest non-helpers (ie: isHelper() returns FALSE)
        ArrayList<EntityWrapper> closestNonHelpers =
            new ArrayList<EntityWrapper>();
        ArrayList<Double> nonHelperDistances = new ArrayList<Double>();

        int[] entityPosition = new int[2];
        int[] startVertexPos = new int[2];
        int[] endVertexPos = new int[2];

        for(int i = 0; i < wrapperList.size(); i++) {

            EntityWrapper eWrapper = (EntityWrapper)wrapperList.get(i);
            Entity entity = eWrapper.getEntity();

            // For segmented picking, we first check to see if we are near a
            // vertex. If we are, that is always picked in preference to the
            // segment. Otherwise, we look to see if we are near a segment
            // and select that.
            //
            if (entity instanceof SegmentableEntity) {

                ArrayList<VertexEntity> vertices = ((SegmentableEntity)entity).getVertices();

                int vertexFound = -1;
                int segmentFound = -1;

                Polygon building =  new Polygon();
                double[] entityPos = new double[3];
                float[] entityRot = new float[4];
                if (entity.getType() == Entity.TYPE_BUILDING) {
                    // get the base position
                    ((PositionableEntity)entity).getPosition(entityPos);
                    ((PositionableEntity)entity).getRotation(entityRot);
                }

                // first check to see if we are within the bounds of the building
                for (int j = 0; j < vertices.size(); j++) {

                    VertexEntity vtx = vertices.get(j);

                    // get the vertex in screen position
                    double[] pos = new double[3];
                    vtx.getPosition(pos);

                    // adjust the location
                    pos[0] += entityPos[0];
                    pos[1] += entityPos[1];
                    pos[2] += entityPos[2];

                    convertWorldPosToScreenPos(pos, entityPosition);

                    // the distance from the mouse position to the center of the entity
                    double dx = x - entityPosition[0];
                    double dy = y - entityPosition[1];
                    double distance = dx * dx + dy * dy;

                    if (distance <= VERTEX_PICK_RADIUS * VERTEX_PICK_RADIUS) {

                        closest = eWrapper;
                        vertexFound = vtx.getEntityID();
                        if(entity.isHelper()){
                             closestHelpers.add(closest);
                               helperDistances.add(distance);
                        } else {
                               closestNonHelpers.add(closest);
                               nonHelperDistances.add(distance);
                        }
                    }

                    building.addPoint(entityPosition[0], entityPosition[1]);
                }

                // select the building if in bounds
                if (entity.getType() == Entity.TYPE_BUILDING &&
                    building.contains(x, y)) {

                    closest = eWrapper;
                    if(entity.isHelper()){
                        closestHelpers.add(closest);
                        helperDistances.add(1d);
                    } else {
                        closestNonHelpers.add(closest);
                        nonHelperDistances.add(1d);
                    }

                }

                ArrayList<SegmentEntity> segments = ((SegmentableEntity)entity).getSegments();

                // Didn't find a matching vertex? Well then let's see if a
                // segment is close.
                if(vertexFound == -1) {
                    for (int j = 0; j < segments.size(); j++) {
                        SegmentEntity segment = segments.get(j);

                        VertexEntity startVertex =

((SegmentableEntity)entity).getVertex(segment.getStartID());
                        VertexEntity endVertex =

((SegmentableEntity)entity).getVertex(segment.getEndID());

                        // get the vertex in screen position
                        double[] startPos = new double[3];
                        startVertex.getPosition(startPos);

                        // adjust the location
                        startPos[0] += entityPos[0];
                        startPos[1] += entityPos[1];
                        startPos[2] += entityPos[2];

                        convertWorldPosToScreenPos(
                                startPos,
                                startVertexPos);
                        double[] endPos = new double[3];
                        endVertex.getPosition(endPos);

                        // adjust the location
                        endPos[0] += entityPos[0];
                        endPos[1] += entityPos[1];
                        endPos[2] += entityPos[2];

                        convertWorldPosToScreenPos(
                                endPos,
                                endVertexPos);

                        // if the distance between the two vertices of this
                        // segment is less than the segment pick distance
                        // then the results from the per-vertex test above
                        // must have been dodgy. Can't do much about that, so
                        // let's just assume that they are not, and keep going.

                        // Rename so the equations are simpler to read
                        float x1 = startVertexPos[0];
                        float y1 = startVertexPos[1];
                        float x2 = endVertexPos[0];
                        float y2 = endVertexPos[1];

                        //System.out.println("x1: " + x1);
                        //System.out.println("y1: " + y1);
                        //System.out.println("x2: " + x2);
                        //System.out.println("y2: " + y2);

                        float u = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) /
                            ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));

                        //System.out.println("Up: " + ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)));
                        //System.out.println("Down: " + ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
                        //System.out.println("u: " + u);

                        if(u < 0 || u > 1) {
                            continue;
                        }

                        float px = x1 + u * (x2 - x1);
                        float py = y1 + u * (y2 - y1);

                        double distance = (x - px) * (x - px) + (y - py) * (y - py);

                        if(distance <= SEGMENT_PICK_DISTANCE * SEGMENT_PICK_DISTANCE) {

                            closest = eWrapper;
                            segmentFound = segment.getEntityID();
                            if(entity.isHelper()){
                                 closestHelpers.add(closest);
                                   helperDistances.add(distance);
                            } else {
                                   closestNonHelpers.add(closest);
                                   nonHelperDistances.add(distance);
                            }
                        }
                    }

                }

                // Did we really pick this segmented entity or not? If both
                // the vertex and segment ID are negative then we didn't
                // actually intersect with this segmented object, so we will
                // now ignore it and move on to checking the next item.
                if(vertexFound >= 0 || segmentFound >= 0) {
                    vertexID = vertexFound;
                    segmentID = segmentFound;
                    break;
                }

            } else {
                eWrapper.getScreenPosition(entityPosition);

                // an approximate bounding circle radius
                float sizeX2 = eWrapper.getIconWidth() * 0.5f;
                float sizeY2 = eWrapper.getIconHeight() * 0.5f;

                double entityBoundsRadius = sizeX2 * sizeX2 + sizeY2 * sizeY2;

                // the distance from the mouse position to the center of the entity
                double dx = x - entityPosition[0];
                double dy = y - entityPosition[1];
                double distance = dx * dx + dy * dy;

                if (distance <= entityBoundsRadius) {
                    // the mouse position is inside the bounding circle of the
                    // non-segmented entity.
                    closest = eWrapper;
                    if(entity.isHelper()){
                        closestHelpers.add(closest);
                        helperDistances.add(distance);
                    } else {
                        closestNonHelpers.add(closest);
                        nonHelperDistances.add(distance);
                    }
                }
            }
        }
        double minDistance = Double.MAX_VALUE;
        int minDistanceIndex = -1;

        // first, find the closest NonHelpers
        for(int i = 0; i < closestNonHelpers.size(); i++){
            if(nonHelperDistances.get(i) < minDistance ){
                minDistance = nonHelperDistances.get(i);
                minDistanceIndex = i;
            }
        }
        if (minDistance < Double.MAX_VALUE){

            return new EntitySearchReturn(
                    closestNonHelpers.get(minDistanceIndex),
                    -1,
                    vertexID);
        }

        // if we haven't found a nonHelper, find the closest Helper
        for (int i = 0; i < closestHelpers.size(); i++){

            if(helperDistances.get(i) < minDistance ){
                minDistance = helperDistances.get(i);
                minDistanceIndex = i;
            }
        }

        // if we haven't found a helper
        if(minDistanceIndex == -1 || (currentPlane != ViewingFrustum.Plane.TOP)) {
            return new EntitySearchReturn(null, -1, vertexID);
        }

        EntityWrapper closestHelper = closestHelpers.get(minDistanceIndex);
        boolean entitySelected = closestHelper.isSelected();
        Entity entity = closestHelper.getEntity();

        int segSelected = -1;
        int vertSelected = -1;

        if (entity instanceof SegmentableEntity) {
            segSelected = ((SegmentableEntity)entity).getSelectedSegmentID();
            vertSelected = ((SegmentableEntity)entity).getSelectedVertexID();
        }

        if (entity.getType() == Entity.TYPE_BUILDING) {
            return new EntitySearchReturn(closestHelper, segmentID, vertexID);
        } else {
            if (!closestHelper.isSelected() || (entitySelected && segSelected > -1 && vertSelected > -1)) {
                return new EntitySearchReturn(closestHelper, -1,-1);
            } else if (segSelected < 0 && segmentID > -1) {
                return new EntitySearchReturn(closestHelper, segmentID, -1);
            }
        }

        return new EntitySearchReturn(closestHelper, -1, vertexID);

    }

    /**
     * Find all entities given a screen bounding box.  This will return
     * the list of entities.
     *
     * @param bounds The search box
     * @return The entity list
     */
    protected ArrayList<Entity> findEntity(Rectangle bounds) {

        ArrayList<Entity> entityList = new ArrayList<Entity>();

        if (entityWrapperMap.size() > 0) {

            int[] screenPosition = new int[2];
            double[] worldPosition = new double[3];

            int[] screenCheck = new int[2];
            Matrix4f rotMatrix;

            int width = boundRectangle.width;
            int height = boundRectangle.height;

            int x_min = boundRectangle.x;
            int y_min = boundRectangle.y;
            int x_max = width;
            int y_max = height;

            // rearrange the coords in case the drag has been
            // working in a negative direction
            if (width < 0) {
                x_min += width;
                x_max = x_min - width;
            } else {
                x_max = x_min + width;
            }
            if (height < 0) {
                y_min += height;
                y_max = y_min - height;
            } else {
                y_max = y_min + height;
            }

            for(int i = 0; i < wrapperList.size(); i++) {
                EntityWrapper eWrapper = wrapperList.get(i);

                // get the screen postion and heading
                eWrapper.getScreenPosition(screenPosition);
                eWrapper.getWorldPosition(worldPosition);
                float radians = eWrapper.getHeadingRadians();

                // Set the rotation to use
                rotMatrix = new Matrix4f();
                rotMatrix.setIdentity();
                rotMatrix.rotY(radians);

                // Sget the offset to use
                int sizeX2 = (int) Math.round(eWrapper.getIconWidth() * 0.5);
                int sizeY2 = (int) Math.round(eWrapper.getIconHeight() * 0.5);

                //****** Left Top Coner ******//
                getCheckPoint(screenPosition, worldPosition, rotMatrix,
                        -sizeX2, -sizeY2, screenCheck);

                // if outside the left-hand corner then stop
                if ((screenCheck[0] < x_min) || (screenCheck[0] > x_max) ||
                    (screenCheck[1] < y_min) || (screenCheck[1] > y_max)) {
                    continue;
                }

                //****** Right Top Coner ******//
                getCheckPoint(screenPosition, worldPosition, rotMatrix,
                        sizeX2, -sizeY2, screenCheck);

                // if outside the left-hand corner then stop
                if ((screenCheck[0] < x_min) || (screenCheck[0] > x_max) ||
                    (screenCheck[1] < y_min) || (screenCheck[1] > y_max)) {
                    continue;
                }

                //****** Right Bottom Coner ******//
                getCheckPoint(screenPosition, worldPosition, rotMatrix,
                        sizeX2, sizeY2, screenCheck);

                // if outside the left-hand corner then stop
                if ((screenCheck[0] < x_min) || (screenCheck[0] > x_max) ||
                    (screenCheck[1] < y_min) || (screenCheck[1] > y_max)) {
                    continue;
                }

                //****** Left Bottom Coner ******//
                getCheckPoint(screenPosition, worldPosition, rotMatrix,
                        -sizeX2, sizeY2, screenCheck);

                // if outside the left-hand corner then stop
                if ((screenCheck[0] < x_min) || (screenCheck[0] > x_max) ||
                    (screenCheck[1] < y_min) || (screenCheck[1] > y_max)) {
                    continue;
                }

                entityList.add(eWrapper.getEntity());

            }
        }

        return entityList;
    }


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
    protected void convertScreenPosToWorldPos(int panelX, int panelY, double[] position, boolean newEntityFlag) {

        if (!mapIsAvailable)
            return;

        // the map extent
        double mapWidth = mapArea.getWidth();
        double mapHeight = mapArea.getHeight();

        Rectangle panelBounds = mapPanel.getBounds();

        // the dimensions of the panel
        double panelWidth = panelBounds.getWidth();
        double panelHeight = panelBounds.getHeight();

        // translate the mouse position to map coordinates
        double x = (panelX * mapWidth / panelWidth) + mapArea.getMinX();
        double y = -(((panelY * mapHeight) / panelHeight) - mapArea.getMaxY());
        if(currentPlane == ViewingFrustum.Plane.TOP ) y *= -1;
        // get the current size
        float[] size = new float[3];
        if (currentTool != null) {
            size = currentTool.getSize();
        }

        // update axis based on the current set face
        switch (currentPlane) {
            case TOP:
                position[0] = x;
                position[2] = y;

                if(newEntityFlag){
                    position[1] = size[1] * 0.5;
                }
                break;
            case RIGHT:
                position[1] = y;
                position[2] = -x;

                if(newEntityFlag){
                    position[0] = size[0] * 0.5;
                }
                break;
            case LEFT:
                position[1] = y;
                position[2] = x;

                if(newEntityFlag){
                    position[0] = size[0] * 0.5;
                }
                break;
            case FRONT:
                position[0] = x;
                position[1] = y;

                if(newEntityFlag){
                    position[2] = size[2] * 0.5;
                }
                break;
         }
    }

    /**
     * Convert world coordinates in meters to panel pixel location.
     *
     * TODO: This depends on iconCenterX which per tool.  Needs to be passed in.
     *
     * @param position World coordinates
     * @param pixel Mouse coordinates
     */
    protected void convertWorldPosToScreenPos(double[] position, int[] pixel) {

        if (!mapIsAvailable)
            return;

        // the map extent
        double mapWidth = mapArea.getWidth();
        double mapHeight = mapArea.getHeight();

        Rectangle panelBounds = mapPanel.getBounds();

        // the dimensions of the panel
        double panelWidth = panelBounds.getWidth();
        double panelHeight = panelBounds.getHeight();

        // convert world coordinates to panel coordinates
/*
int x = (int)Math.round(((position[0] - mapArea.getMinX()) * panelWidth) / mapWidth);
int y1 = (int)Math.round(((-position[1] + mapArea.getMaxY()) * panelHeight) / mapHeight);
int y2 = (int)Math.round(((-position[1] - mapArea.getMinY()) * panelHeight) / mapHeight);
int z1 = (int)Math.round(((-position[2] - mapArea.getMinX()) * panelHeight) / mapHeight);
int z2 = (int)Math.round(((position[2] + mapArea.getMaxY()) * panelHeight) / mapHeight);




System.out.println("GT2DView.convertWorldPosToScreenPos()");
System.out.println("    position[0]: " + position[0]);
System.out.println("    mapArea.getMinX(): " +  mapArea.getMinX());
System.out.println("    mapArea.getMaxX(): " +  mapArea.getMaxX());
System.out.println("    panelWidth: " + panelWidth);
System.out.println("    mapWidth: " + mapWidth);
System.out.println("    x = ((" + position[0] + " + " + mapArea.getMaxX() + ") * " + panelWidth + ") / " + mapWidth);
System.out.println("    x = " + x);
System.out.println("    position[1]: " + position[1]);
System.out.println("    mapArea.getMaxY(): " +  mapArea.getMaxY());
System.out.println("    panelHeight: " + panelHeight);
System.out.println("    mapHeight: " + mapHeight);
System.out.println("    y1 = ((" + position[1] + " + " + mapArea.getMaxY() + ") * " + panelHeight + ") / " + mapHeight);
System.out.println("    y1 = " + y1);
System.out.println("    position[2]: " + position[2]);
System.out.println("    mapArea.getMinX(): " +  mapArea.getMinX());
System.out.println("    panelHeight: " + panelHeight);
System.out.println("    mapHeight: " + mapHeight);
System.out.println("    z = ((" + position[2] + " + " + mapArea.getMaxX() + ") * " + panelHeight + ") / " + mapHeight);
System.out.println("    z1 = " + z1);
System.out.println("    z2 = " + z2);
*/


        // update axis based on the current set face
        switch (currentPlane) {
            case TOP:
                double temp= mapArea.getMinX();
                double temp1=position[0];
                double temp2=temp1-temp;
                pixel[0] = (int)Math.round(((position[0] - mapArea.getMinX()) * panelWidth) / mapWidth);
                pixel[1] = (int)Math.round(((position[2] + mapArea.getMaxY()) * panelHeight) / mapHeight);
                break;
            case RIGHT:
                pixel[0] = (int)Math.round(((-position[2] - mapArea.getMinX()) * panelHeight) / mapHeight);
                pixel[1] = (int)Math.round(((-position[1] + mapArea.getMaxY()) * panelHeight) / mapHeight);
                break;
            case LEFT:
                pixel[0] = (int)Math.round(((position[2] - mapArea.getMinX()) * panelHeight) / mapHeight);
                pixel[1] = (int)Math.round(((-position[1] + mapArea.getMaxY()) * panelHeight) / mapHeight);
                break;
            case FRONT:
                pixel[0] = (int)Math.round(((position[0] - mapArea.getMinX()) * panelWidth) / mapWidth);
                pixel[1] = (int)Math.round(((-position[1] + mapArea.getMaxY()) * panelHeight) / mapHeight);
                break;

        }

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
     protected  void calcScaleFactor(boolean fixedAspect, int imgWidth, int imgHeight, float[] toolSize,
            float[] toolScale, float[] scaledXAndY, int[] centerOfScaledTool , int toolType) {

        float iconScaleX = 0;
        float iconScaleY = 0;

        float scale_f = (float)(mapScale);

        boolean debuggingMethod = false;
        if (debuggingMethod){
            System.out.println("**********************************");
            System.out.println("begin calc scale factor output");
            System.out.println("**********************************");
            System.out.println("fixedAspec:" + fixedAspect);
            System.out.println("imgWidth:" + imgWidth);
            System.out.println("imgHeight:" + imgHeight);
            System.out.println("toolWidth:" + toolSize[0]*toolScale[0] );
            System.out.println("toolLength:" + toolSize[1]*toolScale[1]);
            System.out.println("toolDepth:" + toolSize[2]*toolScale[2] );
        }

        // set top down as default
        float toolWidth = toolSize[0] * toolScale[0];
        float toolHeight = toolSize[2] * toolScale[2];

        switch (currentPlane) {
            case TOP:
                toolWidth = toolSize[0] * toolScale[0];
                toolHeight = toolSize[2] * toolScale[2];
                break;
            case LEFT:
            case RIGHT:
                toolWidth = toolSize[2] * toolScale[2];
                toolHeight = toolSize[1] * toolScale[1];
                break;
            case FRONT:

                toolWidth = toolSize[0] * toolScale[0];
                toolHeight = toolSize[1] * toolScale[1];
                break;
        }

        float scale = (float)(mapScale);

        // assume that 'scale' is meters per pixel, and that toolWidth
        // is meters.  The division gives us the size of the tool in pixels
        float toolWidthInPixels = (toolWidth / scale);
        float toolHeightInPixels = (toolHeight/ scale);

        if ( toolWidthInPixels < ICON_MINIMUM )
            toolWidthInPixels = ICON_MINIMUM;

        if ( toolHeightInPixels < ICON_MINIMUM )
            toolHeightInPixels = ICON_MINIMUM;

        // calculate how much we will need to scale the icon to match the
        // size of the tool [should we add a Math.abs() around this result?]
        iconScaleX = Math.abs(toolWidthInPixels / imgWidth);
        iconScaleY = Math.abs(toolHeightInPixels / imgHeight);

        // EMF: I've not tested this fixed-aspect code, this is
        // same as before only cleaned up slightly to avoid redundancy
        if(fixedAspect){
            if (toolHeight > toolWidth) {
                iconScaleX = Math.abs(  toolWidthInPixels /
                                    (imgWidth * toolWidth / toolHeight));

            } else if (toolHeight < toolWidth) {
                iconScaleY = Math.abs(  toolHeightInPixels /
                                    (imgHeight * toolHeight / toolWidth));
            }
        }

        if(debuggingMethod)
            errorReporter.messageReport("Icon scale: " + iconScaleX + " " +
                    iconScaleY + " size x: " + imgWidth * iconScaleX + " y: "
                    + imgHeight * iconScaleY);


        // TODO: Explain why we treat multiSegments differently
        if(toolType == Entity.TYPE_MULTI_SEGMENT) {
            iconScaleX *= ICON_SCALAR;
            iconScaleY *= ICON_SCALAR;
        }

        scaledXAndY[0] = iconScaleX;
        scaledXAndY[1] = iconScaleY;


        // Finally, set the centerOfScaledTool points
        centerOfScaledTool[0] = (int) Math.ceil( (imgWidth / 2) * iconScaleX);
        centerOfScaledTool[1] = (int) Math.ceil( (imgHeight/ 2) * iconScaleY);

        // TODO: this fixedAspect code needs closer examination
        if( fixedAspect ) {
            if (toolHeight >= toolWidth) {
                //center[0] = (int) Math.ceil((imgWidth * toolWidth / toolLength) / 2 * iconScaleX);
                //              center[1]  = (int) Math.ceil(imgHeight / 2 * iconScaleY);
                centerOfScaledTool[0] = (int) Math.ceil(imgWidth / 2 * iconScaleY);
                centerOfScaledTool[1] = (int) Math.ceil(imgHeight / 2 * iconScaleX);

            } else {
                centerOfScaledTool[1] = (int) Math.ceil((imgHeight * toolHeight / toolWidth) / 2 * iconScaleY);
            }
        }

        if(debuggingMethod){
            errorReporter.messageReport("Icon size: " + (imgWidth * iconScaleX) + " " + (imgHeight * iconScaleY) + " center: " + centerOfScaledTool[0] + " " + centerOfScaledTool[1]);
            System.out.println("scale:" + scaledXAndY[0] + " " + scaledXAndY[1]);
            System.out.println("center:" + centerOfScaledTool[0] + " " + centerOfScaledTool[1]);
            System.out.println("toolType:" + toolType);
            System.out.println("End *********************************");
        }
    }

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

    /**
     * Get the value of an attribute as a boolean
     *
     * @param The attrbiute name
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
     * Calculate the scaling factor for the entity image and
     * return it in the argument array
     *
     * @param eWrapper The entity instance for which to calculate the scale factor
     * @param scale The argument array to initialize with the scale factor
     */
    private void calcEntityScale(EntityWrapper eWrapper, float[] scale) {

        ToolRenderer image = entityRendererMapper.getRenderer(eWrapper.getEntity(),
                                                              currentPlane);
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        float[] size = new float[] {0, 0, 0};
        float[] curScale = new float[] {1, 1, 1};
        if (eWrapper.getEntity() instanceof PositionableEntity) {
            ((PositionableEntity)eWrapper.getEntity()).getSize(size);
            ((PositionableEntity)eWrapper.getEntity()).getScale(curScale);
        }

        calcScaleFactor(
            eWrapper.getEntity().isFixedAspect(),
            imgWidth,
            imgHeight,
            size,
            curScale,
            scale,
            tmpCenter,
            eWrapper.getEntity().getType());

    }



    /**
     * Resets all entity management state variables.
     */
    protected void resetState() {
        toolImage = null;
        currentTool = null;
        multiSegmentOp = false;
        mapPanel.setCursor(null);
    }

    /**
     * Reset the navigation control parameters to a default settings
     */
    protected void resetNavigateState() {
        isOverNavControl = false;
        panInProgress = false;
        boundInProgress = false;
    }


    /**
     * Calculate new parameters given the argument change in zoom levels.
     * Updates and causes a redraw of the map to accommodate the changed
     * level.
     *
     * @param delta The number of levels to change
     */
    protected void incrementZoomLevel(int delta) {

        // No point doing anything if we don't have a map right now.
        if (!mapIsAvailable)
            return;

        int newLevel = zoomLevel + delta;
        int minLevel = navControl.getMinimum();
        int maxLevel = navControl.getMaximum();

        if (newLevel < minLevel) {
            newLevel = minLevel;
        } else if (newLevel > maxLevel) {
            newLevel = maxLevel;
        }

        int levelChange = newLevel - zoomLevel;

        // If no change, then exit now, otherwise continue on and
        // recalculate all, causing a repaint of the window.
        if (levelChange == 0)
            return;

        double centerX = mapArea.getWidth() / 2.0;
        double centerY = mapArea.getHeight() / 2.0;

        double mapX = centerX + mapArea.getMinX();
        double mapY = centerY + mapArea.getMinY();

        double zlevel = 1.0;
        if (levelChange < 0) {
            zlevel = zoomFactor * Math.pow(zoomFactor, (-1 - levelChange));
        } else {
            zlevel = 1.0 / (zoomFactor * Math.pow(zoomFactor, levelChange - 1));
        }

        // the new region of the map to display
        mapArea = new ReferencedEnvelope(
            mapX - (centerX / zlevel),
            mapX + (centerX / zlevel),
            mapY - (centerY / zlevel),
            mapY + (centerY / zlevel),
            crs);

        setZoomLevel(newLevel);

    }

    /**
     * Set an explicit zoom level. This assumes that a map area has been
     * set for the zoom level before this happens,
     *
     * @param level The zoom level to use
     */
    protected void setZoomLevel(int level) {
        zoomLevel = level;

        lastZoomUpdateTime = System.currentTimeMillis();

        javax.swing.Timer timer =
            new javax.swing.Timer(ZOOM_UPDATE_CHECK_DELAY + 10, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    long delay = System.currentTimeMillis() - lastZoomUpdateTime;
                    if(delay >= ZOOM_UPDATE_CHECK_DELAY) {
                        updateEntityScaleAndZoom();

                        mapPanel.repaint();
                    }
                }
            });

        timer.setRepeats(false);
        timer.start();

        navControl.setValue(zoomLevel);
    }


    /**
     * Initialize the User Interface, toolbar, map panel & status bar
     */
    private void initUI() {
        this.setLayout(new BorderLayout());

        toolBar = new JToolBar();

        Toolkit tk = Toolkit.getDefaultToolkit();

        FileLoader loader = new FileLoader();

        Object[] file = loader.getFileURL("images/2d/selectIcon.png");
        Image image = tk.createImage((URL)file[0]);

        pickAndPlaceButton = new JToggleButton(new ImageIcon(image));
        pickAndPlaceButton.setToolTipText("Pick & Place Mode");
        pickAndPlaceButton.setEnabled(false);
        pickAndPlaceButton.addActionListener(this);

        file = loader.getFileURL("images/2d/panIcon.png");
        image = tk.createImage((URL)file[0]);
        navigateButton = new JToggleButton(new ImageIcon(image));
        navigateButton.setToolTipText("Navigate Mode");
        navigateButton.setEnabled(false);
        navigateButton.addActionListener(this);

        modeGroup = new ButtonGroup();
        modeGroup.add(navigateButton);
        modeGroup.add(pickAndPlaceButton);

        file = loader.getFileURL("images/2d/openHandIcon.png");
        image = tk.createImage((URL)file[0]);
        panButton = new JToggleButton(new ImageIcon(image));
        panButton.setToolTipText("Pan");
        panButton.setEnabled(false);
        panButton.addActionListener(this);

        file = loader.getFileURL("images/2d/boundIcon.png");
        image = tk.createImage((URL)file[0]);
        boundButton = new JToggleButton(new ImageIcon(image));
        boundButton.setToolTipText("Select Area");
        boundButton.setEnabled(false);
        boundButton.addActionListener(this);
        boundRectangle = new Rectangle();

        // create the button to switch between metric and imperial
        isMetric = new JToggleButton(" m ", true);
        isMetric.setFont(new Font("SansSerif", Font.PLAIN, 11));
        isMetric.setMargin(new Insets(2,2,2,2));
        isMetric.setEnabled(false);
        isMetric.addItemListener(this);
        isMetric.setBorderPainted(true);


        navigateGroup = new ButtonGroup();
        navigateGroup.add(panButton);
        navigateGroup.add(boundButton);

        toolBar.add(pickAndPlaceButton);
        toolBar.add(navigateButton);
        toolBar.addSeparator();
        toolBar.add(panButton);
        toolBar.add(boundButton);
        toolBar.add(isMetric);

        this.add(toolBar, BorderLayout.NORTH);


        //
        // MiniFrame to hold the WallSelector and observe
        // its behavior
        //
        miniFrame = new JFrame();
        Dimension miniFrameSize = new Dimension(300, 250);

        miniFrame.setPreferredSize(miniFrameSize);
        miniFrame.setMinimumSize(miniFrameSize);
        GT2DWallSelector miniFrameWallSelector =
        	new GT2DWallSelector();

        miniFrame.add(miniFrameWallSelector);
        miniFrame.pack();
        miniFrame.setVisible(true);

        mapPanel = new ImagePanel(miniFrameWallSelector);
        mapPanel.setPreferredSize(new Dimension(512, 512));

        mapPanel.addMouseListener(this);
        mapPanel.addMouseMotionListener(this);
        mapPanel.addMouseWheelListener(this);
        mapPanel.addKeyListener(this);

        navControl = new PanZoomControl(mapPanel, NAV_CONTROL_LOCATION);
        navControl.addActionListener(this);
        navControl.addChangeListener(this);

        this.add(mapPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new GridLayout(1, 3, 1, 1));

        statusField = new JTextField();
        statusField.setHorizontalAlignment(SwingConstants.CENTER);
        statusField.setEditable(false);
        statusPanel.add(statusField);

        scaleField = new JTextField();
        scaleField.setHorizontalAlignment(SwingConstants.CENTER);
        scaleField.setEditable(false);
        statusPanel.add(scaleField);

        progressBar = new JProgressBar(0, 100);
        statusPanel.add(progressBar);

        //this.add(statusPanel, BorderLayout.SOUTH);
        JPanel bottomPanel = new JPanel(new BorderLayout());

        if( rulerEnabled ){
            // create the row and column headers
            horizontalRuler = new Ruler( Ruler.Orientation.HORIZONTAL,
                                         Ruler.MeasurementSystem.METRIC,
                                         80, true);

            verticalRuler = new Ruler(  Ruler.Orientation.VERTICAL,
                                        Ruler.MeasurementSystem.METRIC,
                                        120, true);

            this.add(verticalRuler, BorderLayout.WEST);
            bottomPanel.add(horizontalRuler, BorderLayout.CENTER);
        }

        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        this.add(bottomPanel, BorderLayout.SOUTH);

    }


    /**
     * Highlight a set of tools.
     *
     * @param state True to highlight the tool, false to unhighlight all tools
     */
    protected void highlightTools(String[] validTools, boolean state) {
        // Highlight all acceptable tools
        Entity[] entities = model.getModelData();

        int len = entities.length;

//System.out.println("Highlighting tools: " + state);
        for(int i=0; i < len; i++) {
            if (entities[i] == null)
                continue;

            if (state) {
                for(int j=0; j < validTools.length; j++) {
                    if (entities[i].getCategory().equals(validTools[j])) {
                        entities[i].setHighlighted(true);
//System.out.println("Found entity to highlite: " + entities[i]);
                        break;
                    }
                }
            } else {
                entities[i].setHighlighted(false);
            }
        }

        mapPanel.entityUpdateRequired();
        //mapPanel.repaint();
    }

    /**
     * Convenience method to update the entity list for scale and zoom
     * based on the map changing size or the panel.
     */
    protected void updateEntityScaleAndZoom() {

        mapPanel.updateMapArea();

        for(int i = 0; i < wrapperList.size(); i++) {
            EntityWrapper eWrapper = wrapperList.get(i);
            updateEntityScaleAndZoom(eWrapper);
        }

        mapPanel.coverageUpdateRequired();
    }

    /**
     * Convenience method to update a single entity for scale and zoom
     * based on the map changing size or the panel.
     */
    protected void updateEntityScaleAndZoom(EntityWrapper eWrapper) {

        // set the current map info
        eWrapper.setGTMapArea(mapArea);

        if (eWrapper.getEntity() instanceof SegmentableEntity) {

            if (eWrapper.getEntity().getType() == Entity.TYPE_BUILDING) {

                eWrapper.setScale(1, 1);

                // get the position of the entity in world coordinates
                eWrapper.getWorldPosition(tmpPos);

                // convert world coordinates to viewer coordinates
                int[] pixel = new int[2];
                convertWorldPosToScreenPos(tmpPos, pixel);
                eWrapper.setScreenPosition(pixel[0], pixel[1]);
                eWrapper.updateTransform();

            } else {
/*
                eWrapper.setScale(1, 1);
                eWrapper.getWorldPosition(tmpPos);
                int[] pixel = new int[2];
                convertWorldPosToScreenPos(tmpPos, pixel);
                eWrapper.setScreenPosition(pixel[0], pixel[1]);
*/

                eWrapper.setScale(1, 1);
                eWrapper.setScreenPosition(0, 0);
                eWrapper.updateTransform();

            }

        } else {
            // get and set the scale factor
            if (eWrapper.isFixedSize()) {
                eWrapper.setScale(1, 1);
            } else {
                // calculate the scale factor of the entity relative to the map scale
                float[] eScale = new float[2];

                calcEntityScale(eWrapper, eScale);
                eWrapper.setScale(eScale[0], eScale[1]);
            }

            // get the position of the entity in world coordinates
            eWrapper.getWorldPosition(tmpPos);
            System.out.println(": " + java.util.Arrays.toString(tmpPos));
            // convert world coordinates to viewer coordinates
            int[] pixel = new int[2];
            convertWorldPosToScreenPos(tmpPos, pixel);
            eWrapper.setScreenPosition(pixel[0], pixel[1]);
            eWrapper.updateTransform();

        }

    }

    /**
     * Set the location.
     *
     * @param url_string The url of the map imagery.
     */
    public void setLocation(String url_string) {

        // disable the ui while the load is in process
        mapIsAvailable = false;

        setIsLoading(true);
        pickAndPlaceButton.setEnabled(false);
        navigateButton.setEnabled(false);
        boundButton.setEnabled(false);
        isMetric.setEnabled(false);
        panButton.setEnabled(false);

        // force a recalculation of map scaling in the paint routine
        mapPanel.reset();


        // start the loader thread
        this.url_string = url_string;
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Get the rendering component.
     *
     * @return The rendering component
     */
    public JComponent getComponent() {
        return this;
    }

    /**
     * Get the current plane being edited for
     *
     * @return the currentPlane
     */
    public ViewingFrustum.Plane getCurrentPlane() {
        return currentPlane;
    }

    /**
     * Set the current plane being edited for
     *
     * @param currentPlane the currentPlane to set
     */
    public void setCurrentPlane(ViewingFrustum.Plane newPlane) {

        currentPlane = newPlane;
        updateEntityScaleAndZoom();
        if(currentTool != null){
            setTool(currentTool);
        }
//        mapPanel.entityUpdateRequired();

//        mapPanel.repaint();
    }
}

