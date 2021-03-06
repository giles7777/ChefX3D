/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009 - 2010
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

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.ui.LoadingProgressListener;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * A temporary entity, used prior to placing actual model in scene.
 *
 * @author Rex Melton
 * @version $Revision: 1.27 $
 */
class ShadowEntityWrapper extends AV3DEntityWrapper implements
    LoadListener, PerFrameObserver {

    /** Default shadow color */
    private static final float[] DEFAULT_COLOR = new float[]{0, 0, 1};
    
    /** Near zero epsilon comparison for having obtained bounds data */
    private static final float EPS = 0.00001f;

    /** The working color */
    private float[] color;

    /** Flag indicating that the color should be changed */
    private boolean changeColor;

    /** Flag indicating the model should be changed */
    private boolean changeModel;

    /** New model nodes */
    private Node[] modelNodes;

    /** The material node */
    private Material material;
    
    /** Flag identifying need to update bounds if all zero */
    private boolean boundsNotSetFlag = false;

    /**
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param entity The entity that the wrapper object is based around
     * @param urlFilter The filter to use for url requests
     * @param reporter The instance to use or null
     */
    ShadowEntityWrapper(
        SceneManagerObserver mgmtObserver,
        PositionableEntity entity,
        URLFilter urlFilter,
        LoadingProgressListener progressListener,
        ErrorReporter reporter){

        super(mgmtObserver, entity, urlFilter, progressListener, reporter);

        mgmtObserver.addObserver(this);
    }

    //----------------------------------------------------------
    // Methods defined by LoadListener
    //----------------------------------------------------------
	
    /**
     * Notification that the model has finished loading.
     *
     * @param nodes The nodes loaded
     */
    public void modelLoaded(Node[] nodes) {
//System.out.println("*****Model Loaded: " + nodes.length + " node: " + nodes[0]);
//        sanitize(nodes);
		if (nodes != null) {
        	modelNodes = nodes;
        	changeModel = true;
		}
    }

    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {

        if (changeModel) {
			
			mgmtObserver.requestBoundsUpdate(contentGroup, this);

            setSwitchGroupIndex(CONTENT_MODEL);
        }
        
        // Fix for bounds not getting established correctly (BJY)
        if(boundsNotSetFlag){

        	updateBoundsValues();
        }
    }

    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {

        super.updateNodeDataChanges(src);

        if ((src == material) && changeColor) {
            material.setEmissiveColor(color);
            changeColor = false;
        }
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the nodes bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {

        super.updateNodeBoundsChanges(src);

        if ((src == contentGroup) && changeModel) {
            contentGroup.removeAllChildren();
/*
            TODO: Comment back out once aviatrix3d bug is fixed
            AppearanceOverride app_ovr = new AppearanceOverride();
            Appearance appearance = new Appearance();
            appearance.setMaterial(material);

            app_ovr.setAppearance(appearance);
            app_ovr.setEnabled(true);
            contentGroup.addChild(app_ovr);
*/

            for(int i = 0; i < modelNodes.length; i++) {
                contentGroup.addChild(modelNodes[i]);
            }
            
            contentGroup.requestBoundsUpdate();
            
            updateBoundsValues();

            changeModel = false;
        }
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Update the bounds values of the shadow entity
     */
    protected void updateBoundsValues(){
    	
        BoundingVolume bv = contentGroup.getBounds();
        if ((bv != null) && !(bv instanceof BoundingVoid)) {

            bv.getCenter(center);
            double[] center_d = new double[3];
            center_d[0] = center[0];
            center_d[1] = center[1];
            center_d[2] = center[2];
            entity.setOriginOffset(center_d);

            float[] min = new float[3];
            float[] max = new float[3];
            bv.getExtents(min, max);
            
            if (Math.abs(min[0]) < EPS &&
            		Math.abs(min[1]) < EPS &&
            		Math.abs(min[2]) < EPS &&
            		Math.abs(max[0]) < EPS &&
            		Math.abs(max[1]) < EPS &&
            		Math.abs(max[2]) < EPS) {
            
            	boundsNotSetFlag = true;
            } else {
            	boundsNotSetFlag = false;
            }
            
            // If all zero , set flag for check me later
            bounds.setVertices(min, max);
        }
    }
    
    /**
     * Eliminate any references that this object may have to make it
     * eligible for GC.
     */
    protected void dispose() {
        super.dispose();
        mgmtObserver.removeObserver(this);
    }

    /**
     * Set the color
     *
     * @param color The color to set
     */
    void setColor(float[] color) {
        if (color == null) {
            this.color = DEFAULT_COLOR;
        } else {
            this.color = color;
        }
        changeColor = true;
		
		mgmtObserver.requestDataUpdate(material, this);
    }

    /**
     * Load the model from the specified argument into the
     * Entity scenegraph structure.
     */
    protected void loadModel() {

        contentGroup = new Group();

        material = new Material();
        material.setEmissiveColor(DEFAULT_COLOR);
        material.setTransparency(0.5f);

        Shape3D shape = generateBoundsGeometry(dim, DEFAULT_COLOR);
        contentGroup.addChild(shape);

        ///////////////////////////////////////////////////
        // get the bounds
        float[] size = new float[3];
        entity.getSize(size);

        float[] scale = new float[3];
        entity.getScale(scale);

        dim[0] = size[0] * scale[0];
        dim[1] = size[1] * scale[1];
        dim[2] = size[2] * scale[2];

        center[0] = 0;
        center[1] = 0;
        center[2] = 0;

		float x = size[0] / 2;
		float y = size[1] / 2;
		float z = size[2] / 2;
        float[] min = new float[]{-x, -y, -z};
        float[] max = new float[]{x, y, z};
        bounds = new OrientedBoundingBox(min, max, scale);

        ///////////////////////////////////////////////////
		Shape3D boundsProxy = generateBoundsGeometry(size, null);
		
        switchGroup = new SwitchGroup();
        switchGroup.addChild(contentGroup);
		switchGroup.addChild(boundsProxy);
        index = CONTENT_NONE;
        switchGroup.setActiveChild(CONTENT_NONE);

        transformModel = new TransformGroup();
        transformModel.addChild(switchGroup);

        configModelMatrix();
        transformModel.setTransform(modelMatrix);

        transformGroup = new TransformGroup();
        transformGroup.addChild(transformModel);

        configGroupMatrix();
        transformGroup.setTransform(groupMatrix);

        sharedNode = new SharedNode();
        sharedNode.setChild(transformGroup);
        
        if (entity.getType() != Entity.TYPE_TEMPLATE_CONTAINER &&
                entity.getType() != Entity.TYPE_TEMPLATE) {

            String url_string = entity.getModelURL();
            AV3DLoader loader = new AV3DLoader(progressListener, urlFilter);
    
            loader.loadThreaded(url_string, true, this);
        }
        
    }

    /**
     * Walk through the nodes. Disable picking.
     */
    private void sanitize(Node[] nodes) {

        for (int i = 0; i < nodes.length; i++) {

            Node node = nodes[i];
            if (node instanceof Group) {
                Group group = (Group)node;
                if (group.numChildren() > 0) {
                    Node[] children = group.getAllChildren();
                    sanitize(children);
                }
            } else if (node instanceof Shape3D) {
                Shape3D shape = (Shape3D)node;
                shape.setPickMask(0);
            }
        }
    }
}
