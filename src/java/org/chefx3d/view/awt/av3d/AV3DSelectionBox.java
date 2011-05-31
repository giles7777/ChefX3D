/****************************************************************************
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
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.IndexedLineStripArray;
import org.j3d.aviatrix3d.LineStripArray;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.TransformGroup;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.common.EditorConstants;

/**
 * Handles the selection Box creation
 *
 * @author jonhubba
 * @version $Revision: 1.40 $
 */
public class AV3DSelectionBox implements  
    AV3DConstants, 
    EditorConstants, 
    NodeUpdateListener, 
    EntityPropertyListener, 
    PerFrameObserver,
    WrapperListener {
    
    /** Size in pixels of the anchor boxes */
    private static final float ANCHOR_PIXELS = 12.0f;
    
    /** Size in pixels of the border between the object
     *  bounds and the anchor box centers */
    private static final float BORDER_PIXELS = 6.0f;
    
    /** Represents an active anchors capability */
    private static final AnchorData[] ANCHOR = new AnchorData[] {  
        AnchorData.NORTHWEST, 
        AnchorData.NORTH, 
        AnchorData.NORTHEAST,
        AnchorData.EAST,
        AnchorData.SOUTHEAST,
        AnchorData.SOUTH,
        AnchorData.SOUTHWEST,
        AnchorData.WEST,
        AnchorData.DELETE,
        AnchorData.ROTATE,
    };
    
    /** Reference to the scene management observer */
    private SceneManagerObserver mgmtObserver;
    
    /** The working entity */
    private PositionableEntity entity;
    
    /** The working entity wrapper */
    private AV3DEntityWrapper wrapper;
    
    /** The box container */
    TransformGroup transformGroup;
    
    /** The SwitchGroup */
    protected SwitchGroup switchGroup;

    /** The working SwitchGroup index */
    protected int index;

    /** The box outline */
    private IndexedLineStripArray outline;
    
    /** The array of AnchorBox geometry objects */
    private AnchorBox[] anchor;
    
    /** The box outline coordinates */
    private float[] outline_coord;
    
    /** Flag indicating that a change of transformation parameters
    *  requires the matrix to be updated */
    private boolean changeMatrix;
    
    /** Local copy of the TransformGroup's matrix */
    private Matrix4f activeMatrix;
    
    /** Scratch arrays */
    private double[] pos_array;
    private float[] rot_array;
    private float[] scl_array;
    
    /** Working objects */
    private Vector3f translation;
    private AxisAngle4f rotation;
    
    /** Dimensions of the entity's bounding box */
    private float[] dimension;
    
    /** Size of the selection box */
    private float[] lineColor;
    
    /** The center point to build the selectionBox Around */
    private float[] center;
    
    /** Represents which anchors are active and not */
    private boolean[] activeAnchors;
    
    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f mtx;
    
    /** The working parent entity wrapper */
    private AV3DEntityWrapper parentWrapper;
    
    /** The working zone entity wrapper */
    private AV3DEntityWrapper zoneWrapper;
    
    /** Flag indicating that the properties of a segment have changed */
    private boolean segmentChanged;
    
    /** The scale factor of the viewport in pixels / meter */
    private float scale;
    
	/** Delay for recalculating the position after instantiation */
	private int frameDelay;
	
	/** Flag indicating that the entity is an extrusion type, which means
	 *  that it's bounds can change dynamically */
	private boolean isExtrusion;
			
    /** 
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param wrapper The entity wrapper to handle
     * @param parentWrapper The parent entity wrapper
     * @param zoneWrapper The active zone entity wrapper
     * @param scale The scale factor of the viewport in pixels / meter
     */
    AV3DSelectionBox(
        SceneManagerObserver mgmtObserver,
        AV3DEntityWrapper wrapper,
        AV3DEntityWrapper parentWrapper,
        AV3DEntityWrapper zoneWrapper,
        float scale,
        boolean[] activeAnchorFlags) {
        
        this.mgmtObserver = mgmtObserver;
        this.wrapper = wrapper;
        entity = wrapper.entity;
        this.parentWrapper = parentWrapper;
        this.zoneWrapper = zoneWrapper;
        this.scale = scale;
        
        activeMatrix = new Matrix4f();
        translation = new Vector3f();
        rotation = new AxisAngle4f();
        
        tu = new TransformUtils();
        mtx = new Matrix4f();
        
        pos_array = new double[3];
        rot_array = new float[4];
        scl_array = new float[3];
        
        dimension = new float[3];
        center = new float[3];
        
        activeAnchors = activeAnchorFlags;
        
        outline_coord = new float[12];
        
        index = (wrapper.getSwitchGroupIndex() >= 0) ? 0 : -1;
        
		isExtrusion = false;
		Object isExtrusionProp = entity.getProperty(
			entity.getParamSheetName(),
			ExtrusionEntity.IS_EXTRUSION_ENITY_PROP);
		
		if ((isExtrusionProp != null) && (isExtrusionProp instanceof Boolean)) {
			isExtrusion = ((Boolean)isExtrusionProp).booleanValue();
		}
		
        // initialize the selection box geometry
        initGeometry();
        config();
        
        wrapper.addWrapperListener(this);
        
        frameDelay = 2;
		
        // initialize the observer callback last
        this.mgmtObserver.addObserver(this);
    }
    
    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------
    
    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds. Generally speaking
     * it is assumed in most cases that the src Object passed in is a
     * SharedNode and is generally treated like one.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {
        
        if (src == transformGroup) {
            if (changeMatrix) {
                transformGroup.setTransform(activeMatrix);
                transformGroup.requestBoundsUpdate();
                changeMatrix = false;
            }
        } else if (src == outline) {
			
            outline.setVertices(LineStripArray.COORDINATE_3, outline_coord);
            
        } else if (src == switchGroup) {
            
            switchGroup.setActiveChild(index);
        }
    }
    
    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
        if (src == outline) {
            outline.setSingleColor(false, lineColor);
        }
    }
    
    //---------------------------------------------------------------
    // Methods defined by PerFrameObserver
    //---------------------------------------------------------------

    /**
     * A new frame tick is observed, so do some processing now.
     */
    public void processNextFrame() {
        if (segmentChanged) {
            configBox();
            configMatrix();
            segmentChanged = false;
        }
		if ((frameDelay > 0) && (--frameDelay == 0)) {
			configMatrix();
		}
    }

    //----------------------------------------------------------
    // Methods for EntityPropertyListener
    //----------------------------------------------------------
    
    public void propertiesUpdated(List<EntityProperty> properties) {
    }
    
    public void propertyAdded(
        int entityID,
        String propertySheet,
        String propertyName) {
    }
    
    public void propertyRemoved(
        int entityID,
        String propertySheet,
        String propertyName) {
    }
    
    public void propertyUpdated(
        int entityID,
        String propertySheet,
        String propertyName,
        boolean ongoing) {
        
        if (entity.getType() == Entity.TYPE_SEGMENT) {
            
            segmentChanged = true;

        } else if (propertyName.equals(PositionableEntity.POSITION_PROP)){
            
            configMatrix();
            
        } else if (propertyName.equals(PositionableEntity.ROTATION_PROP)) {
            
            configMatrix();
            
        } else if (propertyName.equals(PositionableEntity.SCALE_PROP)) {
            
            configBox();
            
        }
    }
    
    //----------------------------------------------------------
    // Methods define by WrapperListener
    //----------------------------------------------------------
    
    /**
     * Notification that the active switch group has changed
     * on the argument.
     *
     * @param src The wrapper object that has experienced a change
     */
    public void switchGroupChanged(Object src) {
        if (src == wrapper) {
            int idx = (wrapper.getSwitchGroupIndex() >= 0) ? 0 : -1;
            if (idx != index) {
                
                index = idx;
                
				mgmtObserver.requestBoundsUpdate(switchGroup, this);
            }
        }
    }
    
	/**
	 * Notification that the geometry (and bounds) have changed
	 * on the argument.
	 *
	 * @param src The wrapper object that has experienced a change
	 */
	public void geometryChanged(Object src) {
		if (src == wrapper) {
			configBox();
		}
	}
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
    
    /**
     * Return the associated entity
     *
     * @return The entity
     */
    Entity getEntity() {
        return(entity);
    }
    
    /**
     * Eliminate any references that this object may have to make it
     * eligible for GC.
     */
    void dispose() {

        this.mgmtObserver.removeObserver(this);
        if (entity != null) {
            
            entity.removeEntityPropertyListener(this);
            if (entity.getType() == Entity.TYPE_SEGMENT) {
                
                SegmentEntity segment = (SegmentEntity)entity;
                
                VertexEntity v0 = segment.getStartVertexEntity();
                VertexEntity v1 = segment.getEndVertexEntity();
                
                if (v0 != null) {
                    v0.removeEntityPropertyListener(this);
                }
                if (v1 != null) {
                    v1.removeEntityPropertyListener(this);
                }
            }
            entity = null;
        }
        if (wrapper != null) {
            wrapper.removeWrapperListener(this);
            wrapper = null;
        }
    }
    
    /**
     * Set the color
     *
     * @param color The color to set
     */
    void setColor(float[] color) {
        
        for (int i = 0; i < anchor.length; i++) {
            anchor[i].setColor(color);
        }
        
        if (color == null) {
            lineColor = AV3DConstants.DEFAULT_SELECTION_COLOR;
        } else {
            lineColor = color;
        }
        
		mgmtObserver.requestDataUpdate(outline, this);
    }
    
    /** 
     * Set the state of the specified anchor
     *
     * @param ad The AnchorData enum to set
     * @param state The enable state
     */
    void setAnchorState(AnchorData ad, boolean state) {
        for (int i = 0; i < ANCHOR.length; i++) {
            if (ANCHOR[i] == ad) {
                anchor[i].setEnabled(state);
                break;
            }
        }
    }
    
    /**
     * Set the array of the active anchors to display. 
     */
    void setActiveAnchors(boolean[] activeAnchors){
        for (int i = 0; i < activeAnchors.length; i++) {
            anchor[i].setEnabled(activeAnchors[i]);			
        }
    }
    
    /**
     * Returns a boolean array of the active anchors to display. 
     */
    boolean[] getActiveAnchors(){
        return(activeAnchors);
    }
    
    /**
     * Set the scale factor of the viewport
     *
     * @param scale The scale factor of the viewport in pixels / meter
     */
    void setScale(float scale) {
        this.scale = scale;
        float anchorScale = ANCHOR_PIXELS / scale;
        for (int i = 0; i < anchor.length; i++) {
            anchor[i].setScale(anchorScale);			
        }
    }
    
    /**
     * Configure the listeners and selection box geometry
     */
    private void config() {
        
        if (entity.getType() == Entity.TYPE_SEGMENT) {
            
            SegmentEntity segment = (SegmentEntity)entity;
            
            VertexEntity v0 = segment.getStartVertexEntity();
            v0.addEntityPropertyListener(this);
            
            VertexEntity v1 = segment.getEndVertexEntity();
            v1.addEntityPropertyListener(this);
        }
        entity.addEntityPropertyListener(this);
        
        configBox();
        configMatrix();
    }
    
    /**
     * Calculate the local transform
     */
    private void configMatrix() {
        
        // get transformation components
        if (entity.getType() == Entity.TYPE_SEGMENT) {
            
            ((SegmentEntityWrapper)wrapper).getPosition(pos_array);
            ((SegmentEntityWrapper)wrapper).getRotation(rot_array);
            
            ////////////////////////////////////////////////
            // rem: TODO - make this a general case xform
            double py = pos_array[1];
            pos_array[1] = -pos_array[2];
            pos_array[2] = py;
            
            float ry = rot_array[1];
            rot_array[1] = -rot_array[2];
            rot_array[2] = ry;
            rot_array[3] = rot_array[3];
            ////////////////////////////////////////////////
            
        } else {
            
            entity.getPosition(pos_array);
            entity.getRotation(rot_array);
        }
        
        // configure the transform matrix
        activeMatrix.setIdentity();
        
        rotation.set(rot_array);
        translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);
        
        activeMatrix.setRotation(rotation);
        activeMatrix.setTranslation(translation);
        
        if ((parentWrapper != null) && (zoneWrapper != null)) {
            if (parentWrapper != zoneWrapper) {
                // account for parent transforms
                tu.getLocalToVworld(
                    parentWrapper.transformGroup, 
                    zoneWrapper.transformGroup, 
                    mtx);
                activeMatrix.mul(mtx, activeMatrix);
            }
        }
        
        changeMatrix = true;
        
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
    }
    
    /**
     * Recalculate the anchor and line positions
     */
    private void configBox() {
    	
        if (entity.getType() == Entity.TYPE_SEGMENT) {
            
            scl_array[0] = 1;
            scl_array[1] = 1;
            scl_array[2] = 1;
            
            wrapper.getCenter(center);
            wrapper.getDimensions(dimension);
            
            ////////////////////////////////////////////////
            // rem: TODO - make this a general case xform
            float y = center[1];
            center[1] = -center[2];
            center[2] = y;
            
            y = dimension[1];
            dimension[1] = dimension[2];
            dimension[2] = y;
            ////////////////////////////////////////////////
            
        } else {
            
			if (isExtrusion) {
				
				scl_array[0] = 1;
				scl_array[1] = 1;
				scl_array[2] = 1;
				
				wrapper.getCenter(center);
				wrapper.getDimensions(dimension);
				
			} else {
				
				entity.getScale(scl_array);
				
				wrapper.getCenter(center);
				
				float[] size = new float[3];
				entity.getSize(size);
				
				// Positive bounds - negative bounds
				dimension[0] = size[0];// - bounds[0];
				dimension[1] = size[1];// - bounds[2];
				dimension[2] = size[2];// - bounds[4];
			}
        }
        
        center[2] = 0;
        
        float width = 0;
        float height = 0;

        /////////////////////////////////////////////////////////////////////
        
        float border_inset = BORDER_PIXELS / scale;
        
        //width = (scl_array[0] * dimension[0] / 2) + 0.05f;
        width = (scl_array[0] * dimension[0] / 2) + border_inset;
        //height = (scl_array[1] * dimension[1] / 2) + 0.05f;
        height = (scl_array[1] * dimension[1] / 2) + border_inset;
        //depth = (scl_array[2] * dimension[2] / 2) + 0.2f; 
        
        translation.set(center[0] - width, center[1] + height, center[2]);
        anchor[0].moveTo(translation);
        
        translation.set(center[0], center[1] + height, center[2]);
        anchor[1].moveTo(translation);
        
        translation.set(center[0] + width, center[1] + height, center[2]);
        anchor[2].moveTo(translation);
        
        translation.set(center[0] + width, center[1], center[2]);
        anchor[3].moveTo(translation);
        
        translation.set(center[0] + width, center[1] - height, center[2]);
        anchor[4].moveTo(translation);
        
        translation.set(center[0], center[1] - height,  center[2]);
        anchor[5].moveTo(translation);
        
        translation.set(center[0] - width, center[1] - height, center[2]);
        anchor[6].moveTo(translation);
        
        translation.set(center[0] - width, center[1], center[2]);
        anchor[7].moveTo(translation);
        
        float offset = 1.5f;
        //translation.set(center[0] - width - 0.2f, center[1] + height + 0.1f, center[2]);
        translation.set(center[0] - width - border_inset * offset, 
            center[1] + height + border_inset * offset, center[2]);
        anchor[8].moveTo(translation);
        
        //translation.set(center[0] + width + 0.2f, center[1] + height + 0.1f, center[2]);
        translation.set(center[0] + width + border_inset * offset, 
            center[1] + height + border_inset * offset, center[2]);
        anchor[9].moveTo(translation);
        
        outline_coord = new float[]{
            center[0] - width, center[1] + height, center[2],
            center[0] + width, center[1] + height, center[2],
            center[0] + width, center[1] - height, center[2],
            center[0] - width, center[1] - height, center[2]};

        /////////////////////////////////////////////////////////////////////
        
		mgmtObserver.requestBoundsUpdate(outline, this);
        /////////////////////////////////////////////////////////////////////
    }
    
    /** 
     * Initialize
     */
    private void initGeometry() {
        
        transformGroup = new TransformGroup();
        transformGroup.setPickMask(TransformGroup.GENERAL_OBJECT);
        
        switchGroup = new SwitchGroup();
        switchGroup.addChild(transformGroup);
        switchGroup.setActiveChild(index);

        lineColor = AV3DConstants.DEFAULT_SELECTION_COLOR;
        
        outline = new IndexedLineStripArray();
        outline.setValidVertexCount(3);
        outline.setVertices(LineStripArray.COORDINATE_3, outline_coord);
        outline.setIndices(new int[]{0, 1, 2, 3, 0}, 5);
        outline.setStripCount(new int[]{5}, 1);
        
        outline.setSingleColor(false, lineColor);
        Shape3D shape = new Shape3D();
        shape.setPickMask(0);
        shape.setGeometry(outline);
        
        transformGroup.addChild(shape);
        
        anchor = new AnchorBox[ANCHOR.length];
        //float[] motion_anchor_size = new float[]{0.3f, 0.3f, 0.3f};
        //float[] rotate_anchor_size = new float[]{0.3f, 0.3f, 0.3f};
        //float[] delete_anchor_size = new float[]{0.3f, 0.3f, 0.3f};
        //float[] delete_anchor_size = new float[]{0.225f, 0.225f, 0.225f};
        //float[] motion_anchor_size = new float[]{0.10f, 0.10f, 0.10f};
        //float[] rotate_anchor_size = new float[]{0.15f, 0.15f, 0.15f};
        //float[] delete_anchor_size = new float[]{0.15f, 0.15f, 0.15f};
        float anchorScale = ANCHOR_PIXELS / scale;
        
        for (int i = 0; i < ANCHOR.length; i++) {
            
            if ((ANCHOR[i] == AnchorData.DELETE)) {
                anchor[i] = new AnchorBox(mgmtObserver, ANCHOR[i], entity, anchorScale);
            } else if(ANCHOR[i] == AnchorData.ROTATE){
                anchor[i] = new AnchorBox(mgmtObserver, ANCHOR[i], entity, anchorScale);
            } else{
                anchor[i] = new AnchorBox(mgmtObserver, ANCHOR[i], entity, anchorScale);
            }
            anchor[i].setEnabled(activeAnchors[i]);
            transformGroup.addChild(anchor[i].switchGroup);
        }
    }
}


