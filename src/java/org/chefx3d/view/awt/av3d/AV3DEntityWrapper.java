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
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.*;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

import org.j3d.geom.BoxGenerator;
import org.j3d.geom.GeometryData;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.VertexEntity;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.ui.LoadingProgressListener;

import org.chefx3d.util.ConfigManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.common.EntityWrapper;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * A holder of entity data for the Aviatrix3D View.
 *
 * @author Jonathon Hubbard
 * @version $Revision: 1.64 $
 */
class AV3DEntityWrapper implements
	EntityWrapper,
	NodeUpdateListener,
	EntityPropertyListener {
	
	/** Index in the switch for no content */
	public static final int CONTENT_NONE = -1;
	
	/** Index in the switch for model content */
	public static final int CONTENT_MODEL = 0;
	
	/** Index in the switch for model content */
	public static final int CONTENT_BOUNDS = 1;
	
	/** Index in the switch for model facade */
	public static final int CONTENT_FACADE = 2;
	
	/** The default bounds geometry color */
	protected static final float[] DEFAULT_BOUNDS_COLOR = new float[]{0, 0, 1};
	
	/** The default bounds transparency value. 
	    1.0 is fully opaque - 0.0 is fully transparent */
	protected static final float DEFAULT_BOUNDS_TRANSPARENCY = 0.0f;
	
	/** Reference to the scene management observer created in AV3DView */
	protected SceneManagerObserver mgmtObserver;
	
	/** The entity that this object is wrapping */
	protected PositionableEntity entity;
	
	/** The SharedNode */
	protected SharedNode sharedNode;
	
	/** The TransformGroup */
	protected TransformGroup transformGroup;
	
	/** The model Group */
	protected TransformGroup transformModel;
	
	/** The SwitchGroup */
	protected SwitchGroup switchGroup;
	
	/** The content Group */
	protected Group contentGroup;
	
	/** The working SwitchGroup index */
	protected int index;
	
	/** Flag indicating that a change of transformation parameters
	*  requires the matrix to be updated */
	protected boolean changeGroupMatrix;
	
	/** Local copy of the group matrix */
	protected Matrix4f groupMatrix;
	
	/** Flag indicating that a change of transformation parameters
	*  requires the matrix to be updated */
	protected boolean changeModelMatrix;
	
	/** Local copy of the model matrix */
	protected Matrix4f modelMatrix;
	
	/** The filter to use for url requests, null use baseURL logic instead */
	protected URLFilter urlFilter;
	
	/** The wall 'covering' */
	protected Material material;
	
	/** The transparency of the wall */
	protected float transparency;
	
	/** Working objects */
	protected Vector3f translation;
	protected AxisAngle4f rotation;
	
	protected double[] pos_array;
	protected float[] rot_array;
	protected float[] scl_array;
	
	protected float[] center;
	protected float[] dim;
	
	/** The error reporter to log to */
	protected ErrorReporter reporter;
	
	/** The default bounds object */
	protected OrientedBoundingBox bounds;
	
	/** The extended bounds object */
	protected OrientedBoundingBox extendedBounds;
	
	/** Array list of entity models to add to the scene */
	private ArrayList<AV3DEntityWrapper> entityToAddList;
	
	/** Array list of entity models to remove from the scene */
	private ArrayList<AV3DEntityWrapper> entityToRemoveList;
	
	/** Array list of entity models to remove from the scene */
	protected ArrayList<WrapperListener> statusListeners;
	
    /** A progress bar notification */
	protected LoadingProgressListener progressListener;

	/** This is a hack to allow the Floor wrapper to substitute a 
	 *  model url without this having to initialize the entity's
	 *  model url parameter */
	protected String alternate_url_string;
	
	/** Appearance node of the bounds geometry, used to make the bounds visible
	 * when the model has been switched out */
	protected Appearance boundsAppearance;
		
	/** Flag indicating the state of bounds visibility */
	protected boolean boundsVisible;
	
	/** Value for bounds transparency */
	protected float boundsTransparency;
	
	/** The enable state of this wrapper. Used to enable intersection testing */
	protected boolean enabled;
	
	/**
	 * Constructor
	 *
	 * @param mgmtObserver Reference to the SceneManagerObserver
	 * @param entity The entity that the wrapper object is based around
	 * @param urlFilter The filter to use for url requests
	 * @param reporter The instance to use or null
	 */
	AV3DEntityWrapper(
		SceneManagerObserver mgmtObserver,
		PositionableEntity entity, 
		URLFilter urlFilter,
		LoadingProgressListener progressListener, 
		ErrorReporter reporter){
		
		this.entity = entity;
		this.mgmtObserver = mgmtObserver;
		this.urlFilter = urlFilter;
		this.progressListener = progressListener;
		this.reporter = (reporter != null) ?
			reporter : DefaultErrorReporter.getDefaultReporter();
		
		enabled = true;
		
		entityToAddList = new ArrayList<AV3DEntityWrapper>();
		entityToRemoveList = new ArrayList<AV3DEntityWrapper>();
		
		groupMatrix = new Matrix4f();
		groupMatrix.setIdentity();
		
		modelMatrix = new Matrix4f();
		modelMatrix.setIdentity();
		
		translation = new Vector3f();
		rotation = new AxisAngle4f();
		
		pos_array = new double[3];
		rot_array = new float[4];
		scl_array = new float[3];
		
		center = new float[3];
		dim = new float[3];
		
		boundsTransparency = DEFAULT_BOUNDS_TRANSPARENCY;
		ConfigManager cm = ConfigManager.getManager();
		String value_string = cm.getProperty("visibleBoundsTransparency");
		if (value_string != null) {
			try {
				float value = Float.parseFloat(value_string);
				if ((value >= 0) && (value <= 1.0)) {
					boundsTransparency = value;
				}
			} catch (NumberFormatException nfe) {
				// should throw out a catchy message here......
			}
		}
		
		loadModel();
		entity.addEntityPropertyListener(this);
	}
	
    //----------------------------------------------------------
    // Methods defined by EntityWrapper
    //----------------------------------------------------------

	/**
	 * Return the Entity for which this wrapper acts as a surrogate.
	 *
	 * @return The Entity for which this wrapper acts as a surrogate.
	 */
	public PositionableEntity getEntity() {
		return(entity);
	}

	/**
	 * Return the default bounds.
	 *
	 * @return The default bounds
	 */
	public OrientedBoundingBox getBounds() {
		return(bounds);
	}
	
	/**
	 * Return the extended bounds. If no extended bounds are
	 * defined, the default bounds object is returned.
	 *
	 * @return The extended bounds if available. Default bounds
	 * otherwise.
	 */
	public OrientedBoundingBox getExtendedBounds() {
		if (extendedBounds != null) {
			return(extendedBounds);
		} else {
			return(bounds);
		}
	}
	
	/**
	 * Set the enable state of the entity wrapper
	 *
	 * @param state The enable state of the entity wrapper
	 */
	public void setEnabled(boolean state) {
		enabled = state;
	}
	
	/**
	 * Return the enable state of the entity wrapper
	 *
	 * @return true if this is enabled, false otherwise
	 */
	public boolean isEnabled() {
		return(enabled);
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
			
			if (changeGroupMatrix) {
				configGroupMatrix();
				transformGroup.setTransform(groupMatrix);
				changeGroupMatrix = false;
			}
			
			int numToRemove = entityToRemoveList.size();
			if (numToRemove > 0) {
				for(int i = 0; i < numToRemove; i++) {
                    AV3DEntityWrapper wrapper = entityToRemoveList.get(i);
                    Node node = wrapper.sharedNode;
					transformGroup.removeChild(node);
				}
				entityToRemoveList.clear();
			}
			
			int numToAdd = entityToAddList.size();
			if (numToAdd > 0) {
				for(int i = 0; i < numToAdd; i++) {
                    AV3DEntityWrapper wrapper = entityToAddList.get(i);
                    Node node = wrapper.sharedNode;
					transformGroup.addChild(node);
				}
				entityToAddList.clear();
			}
		} else if (src == transformModel) {
			
			if (changeModelMatrix) {
				configModelMatrix();
				transformModel.setTransform(modelMatrix);
				changeModelMatrix = false;
			}
		} else if (src == switchGroup) {
			if (index != switchGroup.getActiveChild()) {
				switchGroup.setActiveChild(index);
			}
		}
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The node or Node Component that is to be updated.
	 */
	public void updateNodeDataChanges(Object src) {
		
		if (src == material) {
			
			material.setTransparency(transparency);
			
		} else if (src == boundsAppearance) {
			
			boundsAppearance.setVisible(boundsVisible);
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
		
		if (propertySheet.equals(PositionableEntity.DEFAULT_ENTITY_PROPERTIES)) {
			if (propertyName.equals(PositionableEntity.POSITION_PROP) ||
				propertyName.equals(PositionableEntity.ROTATION_PROP) ||
				propertyName.equals(PositionableEntity.SCALE_PROP)) {
				
				updateTransform();
			}
		} else if (propertySheet.equals(Entity.EDITABLE_PROPERTIES)) {
			if (propertyName.equals(VertexEntity.HEIGHT_PROP)) {
				
				updateTransform();
			}		    
		}
	}
	
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/**
	 * Add a child entity's representation to this transform
	 *
	 * @param wrapper The entity wrapper to add
	 */
	protected void addChild(AV3DEntityWrapper wrapper) {
		
		entityToAddList.add(wrapper);
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
	}
	
	/**
	 * Remove a child entity's representation from this transform
	 *
	 * @param wrapper The entity wrapper to remove
	 */
	protected void removeChild(AV3DEntityWrapper wrapper) {
		
		if (entityToAddList.contains(wrapper)) {
			entityToAddList.remove(wrapper);
		} else {
			entityToRemoveList.add(wrapper);
		}
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
	}
	
	/**
	 * Configure the wall transparency
	 *
	 * @param transparency The transparency
	 */
	protected void setTransparency(float transparency) {
		
		this.transparency = transparency;
		if (material != null) {
			mgmtObserver.requestDataUpdate(material, this);
		}
	}
	
	/**
	 * Eliminate any references that this object may have to make it
	 * eligible for GC.
	 */
	protected void dispose() {
		entity.removeEntityPropertyListener(this);
		////////////////////////////////////////////////////
		// rem: 'pre-mature' nulling of the entity is 
		// suspected of causing NPE's in some circumstances
		//entity = null;
		////////////////////////////////////////////////////
	}
	
	/**
	 * Set the active SwitchGroup
	 *
	 * @param index The index of the group to activate
	 */
	protected void setSwitchGroupIndex(int index) {
		
		this.index = index;
		
		mgmtObserver.requestBoundsUpdate(switchGroup, this);
		
		if (statusListeners != null) {
			for (int i = 0; i < statusListeners.size(); i++) {
				WrapperListener wl = statusListeners.get(i);
				wl.switchGroupChanged(this);
			}
		}
	}
	
	/**
	 * Return the active SwitchGroup index
	 *
	 * @return The index of the active SwitchGroup
	 */
	protected int getSwitchGroupIndex() {
		return(index);
	}
	
	/**
	 * Add a listener for status changes
	 *
	 * @param wl The listener to add
	 */
	protected void addWrapperListener(WrapperListener wl) {
		if (statusListeners == null) {
			statusListeners = new ArrayList<WrapperListener>();
		}
		if(!statusListeners.contains(wl)){
			statusListeners.add(wl);
		}
	}
	
	/**
	 * Remove a listener of status changes
	 *
	 * @param wl The listener to remove
	 */
	protected void removeWrapperListener(WrapperListener wl) {
		if (statusListeners != null) {
			statusListeners.remove(wl);
		}
	}
	
	/**
	 * Recalculate the entity's transfrom
	 */
	protected void updateTransform() {
		
		changeGroupMatrix = true;
		changeModelMatrix = true;
		
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
		mgmtObserver.requestBoundsUpdate(transformModel, this);
	}
	
	/**
	 * Return the center of the model bounds
	 * <p>
	 * TODO: EMF: note that this returns the object's center as it
	 * was when the object was initially created.  It does not return the
	 * 'current' dimensions as they have been affected by the
	 * transform group(s) containing the object.  Which value do we want?
	 * To get real-world center coordinates for a box, for instance, we need to grab
	 * the box's center orgin and translate it according to the
	 * parental transform group.
	 * The getDimensions() call is similarly undefined.
	 *
	 * @param val The array to initialize with the center
	 */
	protected void getCenter(float[] val) {
		val[0] = center[0];
		val[1] = center[1];
		val[2] = center[2];
	}
	
	/**
	 * Return the dimensions of the model bounds.
	 * <p>
	 * TODO: EMF: note that this returns the object's dimensions -
	 * not the dimensions as they have been affected by the
	 * transform group(s) containing the object.  Which do we want?
	 * To get real-world dimensions for a box, for instance, we need to grab
	 * the box's size values and multiply them by the transform group's scale.
	 * The getCenter() call is similarly undefined.
	 *
	 * @param val The array to initialize with the dimensions
	 */
	protected void getDimensions(float[] val) {
		val[0] = dim[0];
		val[1] = dim[1];
		val[2] = dim[2];
	}
	
	/**
	 * Load the Entity scenegraph structure.
	 */
	protected void loadModel() {
		
		contentGroup = new Group();
		
		boolean noModel = false;
		Object noModelProp = entity.getProperty(
			Entity.DEFAULT_ENTITY_PROPERTIES,
			ChefX3DRuleProperties.NO_MODEL_PROP);
		if ((noModelProp != null) && (noModelProp instanceof Boolean)) {
			noModel = (Boolean)noModelProp;
		}
		
		if (!noModel) {
			String url_string;
			if (alternate_url_string != null) {
				url_string = alternate_url_string;
			} else {
				url_string = entity.getModelURL();
			}
			if (url_string != null) {
				
				AV3DLoader loader = new AV3DLoader(progressListener, urlFilter);
				
				Node[] nodes = loader.load(url_string, true);
				
				sanitize(nodes);
				
				for(int i = 0; i < nodes.length; i++) {
					contentGroup.addChild(nodes[i]);
				}
			}
		}
		
		///////////////////////////////////////////////////
		// get the bounds    
		
		float[] size = new float[3];
		entity.getSize(size);
		
		float[] scale = new float[3];
		entity.getScale(scale);
		
		float[] minimum_extent = null;
		Object obj = entity.getProperty(
			PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
			PositionableEntity.MINIMUM_EXTENT_PROP);
		if ((obj != null) && (obj instanceof float[])) {
			minimum_extent = (float[])obj;
			if (minimum_extent.length < 3) {
				minimum_extent = null;
			}
		}
		
		float[] bounds_border = null;
		obj = entity.getProperty(
			PositionableEntity.DEFAULT_ENTITY_PROPERTIES,
			PositionableEntity.BOUNDS_BORDER_PROP);
		if ((obj != null) && (obj instanceof float[])) {
			bounds_border = (float[])obj;
			if (bounds_border.length < 3) {
				bounds_border = null;
			}
		}
		
		////////////////////////////////////////////////////
		// rem: determine the center from the actual bounds
		// of the model. if the entity's notion of size and
		// scale are at odds with the actual model - then
		// this may not behave as expected.
		
		float[] min;
		float[] max;
		
		contentGroup.requestBoundsUpdate();
		BoundingVolume bv = contentGroup.getBounds();
		if ((bv != null) && !(bv instanceof BoundingVoid)) {
			// get the bounds from the loaded geometry
			min = new float[3];
			max = new float[3];
			bv.getExtents(min, max);
			
			size[0] = max[0] - min[0];
			size[1] = max[1] - min[1];
			size[2] = max[2] - min[2];
			
		} else {
			// couldn't determine the bounds. calculate
			// from the entity's parameters
			float x = size[0] / 2;
			float y = size[1] / 2;
			float z = size[2] / 2;
			min = new float[]{-x, -y, -z};
			max = new float[]{x, y, z};
		}
			
		bounds = new OrientedBoundingBox(min, max, scale);
		if (bounds_border != null) {
			bounds.setBorder(bounds_border);
		}
		
		/////////////////////////////////////////////////////////
		// if minimum extents are defined, check whether the entity's
		// extents comply. if not, adjust the bounds and add an
		// invisible piece of geometry to compensate
		
		boolean adjust = false;
		if (minimum_extent != null) {
			for (int i = 0; i < size.length; i++) {
				if (size[i] < minimum_extent[i]) {
					float diff = minimum_extent[i] - size[i];
					float diff2 = diff * 0.5f;
					size[i] = minimum_extent[i];
					min[i] -= diff2;
					max[i] += diff2;
					adjust = true;
				}
			}
		}
		if (adjust) {
			Shape3D adjustedGeom = generateBoundsGeometry(size, null);
			contentGroup.addChild(adjustedGeom);
			
			extendedBounds = new OrientedBoundingBox(min, max, scale);
		}
		
		/////////////////////////////////////////////////////////
		// establish proxy bounds
		Shape3D boundsProxy = generateBoundsGeometry(size, null);
		//boundsProxy.setUserData("Bound: entity = "+ entity.getEntityID());
		boundsProxy.requestBoundsUpdate();
		BoundingGeometry bg = new BoundingGeometry(boundsProxy);
		contentGroup.setBounds(bg);
		
		// note: yet another invisible piece of geometry is added to
		// allow picks when the actual geometry is switched out
		//boundsProxy = generateBoundsGeometry(size, null);
		boundsProxy = generateBoundsGeometry(size, DEFAULT_BOUNDS_COLOR);
		boundsProxy.setUserData("Bound: entity = "+ entity.getEntityID());
		/////////////////////////////////////////////////////////
		
		bounds.getCenter(center);
		bounds.getSize(size);
		
		dim[0] = size[0] * 2;
		dim[1] = size[1] * 2;
		dim[2] = size[2] * 2;
		
		double[] center_d = new double[3];
		center_d[0] = center[0];
		center_d[1] = center[1];
		center_d[2] = center[2];
		entity.setOriginOffset(center_d);
		///////////////////////////////////////////////////
		
		switchGroup = new SwitchGroup();
		switchGroup.addChild(contentGroup);
		switchGroup.addChild(boundsProxy);
		switchGroup.setActiveChild(index);
		
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
		
		// Set the object picking allowed for the geometry
		transformGroup.setPickMask(
			TransformGroup.GENERAL_OBJECT|TransformGroup.COLLIDABLE_OBJECT);
	}
	
	/**
	 * Calculate the entity's group transform
	 */
	protected void configGroupMatrix() {
		
		try{
			// get transformation components
			entity.getPosition(pos_array);
			entity.getRotation(rot_array);
			
			groupMatrix.setIdentity();
			
			rotation.set(rot_array);
			translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);
			
			groupMatrix.setRotation(rotation);
			groupMatrix.setTranslation(translation);
		} catch (NullPointerException npe){
			/*
			 * BJY: entity is suspect at this time. See bug 666 for more 
			 * details. Numerical significance of bug is coincidence.
			 */
			System.out.println("AV3DEntityWrapper : configGroupMatrix ERROR");
			npe.printStackTrace();
		}
	}
	
	/**
	 * Calculate the entity's model transform
	 */
	protected void configModelMatrix() {
		
		// get transformation components
		entity.getScale(scl_array);
		
		// configure the bounds object
		bounds.setScale(scl_array);
		
		// configure the transform matrix
		modelMatrix.setIdentity();
		modelMatrix.m00 = scl_array[0];
		modelMatrix.m11 = scl_array[1];
		modelMatrix.m22 = scl_array[2];
	}
	
	/**
	 * Generate geometry for a bounds representation.
	 *
	 * @param size The size of the box
	 * @param color The color of the box or null. If null,
	 * no Appearance is applied to the shape.
	 * @return The shape
	 */
	protected Shape3D generateBoundsGeometry(float[] size, float[] color) {
		
		TriangleStripArray geom;
		
		GeometryData data = new GeometryData();
		data.geometryType = GeometryData.TRIANGLE_STRIPS;
		data.geometryComponents = GeometryData.NORMAL_DATA;
		
		BoxGenerator generator = new BoxGenerator(size[0], size[1], size[2]);
		generator.generate(data);
		
		geom = new TriangleStripArray(
			false,
			TriangleStripArray.VBO_HINT_STATIC);
		
		geom.setVertices(
			TriangleStripArray.COORDINATE_3,
			data.coordinates,
			data.vertexCount);
		
		geom.setStripCount(data.stripCounts, data.numStrips);
		geom.setNormals(data.normals);
		
		boundsAppearance = new Appearance();
		if (color != null) {
			Material material = new Material();
			material.setEmissiveColor(color);
			material.setTransparency(0.5f);
			
			boundsAppearance.setMaterial(material);
			boundsAppearance.setVisible(false);
		} else {
			boundsAppearance.setVisible(false);
		}
		
		Shape3D shape = new Shape3D();
		shape.setGeometry(geom);
		shape.setAppearance(boundsAppearance);
		
		return(shape);
	}
	
	/**
	 * Set the state of bounds visibility
	 *
	 * @param state The state of bounds visibility
	 */
	protected void setBoundsVisible(boolean state) {
		boundsVisible = state;
		if (boundsAppearance != null) {
			mgmtObserver.requestDataUpdate(boundsAppearance, this);
		}
	}
	
	/**
	 * Walk through the nodes. Enable picking.
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
				shape.setPickMask(Shape3D.GENERAL_OBJECT|Shape3D.COLLIDABLE_OBJECT);
			}
		}
	}
	
	/**
	 * Return a short String identifier of the argument Entity
	 *
	 * @param entity The entity
	 * @return The identifier
	 */
	private String getIdentifier(Entity entity) {
		return("[id="+ entity.getEntityID() + ", name=\""+ entity.getName() +"\"]");
	}
}
