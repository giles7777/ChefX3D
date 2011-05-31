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

// External Imports
import java.awt.image.BufferedImage;

import java.awt.Color;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.MissingResourceException;

import javax.imageio.ImageIO;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;

import org.j3d.aviatrix3d.*;

import org.j3d.util.I18nManager;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityChildListener;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EnvironmentEntity;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.SegmentableEntity;
import org.chefx3d.model.SegmentEntity;
import org.chefx3d.model.VertexEntity;
import org.chefx3d.model.WorldModel;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * AV3DEntityWrapper implementation for wall segments. Geometry is dynamically 
 * generated based on SegmentEntity parameters.
 *
 * @author Rex Melton
 * @version $Revision: 1.38 $
 */
class SegmentEntityWrapper extends AV3DEntityWrapper implements EntityChildListener {

    /** Default wall color */
    private static final float[] DEFAULT_COLOR =
        new float[]{0.46f, 0.74f, 1f};

	/** Flag indicating that a bump map texture should be utilized if available */
	private static final boolean ENABLE_BUMP_MAP_TEXTURE = true;
	
	/** Key for the bump map texture specifier */
	private static final String BUMP_MAP_TEXTURE = 
		"org.chefx3d.view.awt.av3d.SegmentEntityWrapper.bumpMapImage";
	
	/** Bump map texture image */
	private static BufferedImage bumpMapImage;
	
    /** The world model */
    private WorldModel model;

    /** The parent entity */
    private SegmentableEntity multisegment;

    /** The segment entity */
    protected SegmentEntity segment;

    /** The environment entity */
    private EnvironmentEntity environment;

    /** The wall geometry */
    private TriangleArray triangleArray;

    /** The wall facade geometry */
    private TriangleArray facadeTriangleArray;

    /** The bounds geometry */
    private TriangleArray boundsTriangleArray;

    /** The wall facade 'covering' */
    protected Material facadeMaterial;

    /** Geometry generator */
    private SegmentGeom segGeom;

    /** Bpunds geometry generator */
    private SegmentGeom boundsGeom;

    /** Multisegment data source */
    private SegmentDetails multiSegData;

    /** The parent transform group */
    private TransformGroup parentTransformGroup;

    /** Local map of entitys -embedded- in this segment */
    private HashMap<Integer, PositionableEntity> embeddedEntityMap;

    /** Miter angles for the ends of segments */
    private double[] miter_angle;

    /** The geometry vertex coordinates */
    private float[] coord;

    /** The geometry vertex normals */
    private float[] normal;

    /** The geometry vertex tangents */
    private float[] tangent;

    /** The geometry texture coordinates */
    private float[][] texCoord;

    /** The facade geometry vertex coordinates */
    private float[] facade_coord;

    /** The facade geometry vertex normals */
    private float[] facade_normal;

    /** The facade geometry vertex tangents */
    private float[] facade_tangent;

    /** The facade geometry texture coordinates */
    private float[][] facade_texCoord;

    /** The bounds geometry vertex coordinates */
    private float[] bounds_coord;

    /** The color of the wall */
    private float[] wallColor;

    /* Scratch arrays */
    private float[] vtx0;
    private float[] vtx1;
    private double[] left;
    private double[] rght;

    /** Local copy of the Segment's matrix */
    protected Matrix4f segmentMatrix;

    /** Flag indicating that a change of transformation parameters
    *  requires the matrix to be updated */
    protected boolean changeSegmentMatrix;

    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f mtx;
    private Point3f pnt;
    private Vector3f face;

    /** World parameters, used for visibility handling */
    private Vector3f world_normal;
    private Point3f world_center;

	/**
	 * Static constructor. 
	 */
	static {
		if (ENABLE_BUMP_MAP_TEXTURE) {
			try {
				
				I18nManager i18nMgr = I18nManager.getManager();
				String texture_file_name = i18nMgr.getString(BUMP_MAP_TEXTURE);
				
				FileLoader fileLoader = new FileLoader();
				Object[] fileObjects = fileLoader.getFileURL(texture_file_name, true);
				InputStream is = (InputStream)fileObjects[1];
				bumpMapImage = ImageIO.read(is);
				
			} catch (MissingResourceException mre) {
				// fail silently, defaults to no bump map texture
			} catch (IOException ioe) {
				// fail silently, defaults to no bump map texture
			}
		}
	}
	
    /**
     * Constructor
     *
     * @param model The WorldModel
     * @param mgmtObserver The SceneManagerObserver
     * @param segmentableEntity The SegmentableEntity
     * @param segmentEntity The SegmentEntity
     * @param environmentEntity The EnvironmentEntity
     * @param multiSegData Relational data source of other SegmentEntitys
     * @param reporter The instance to use or null
     */
    SegmentEntityWrapper(
        WorldModel model,
        SceneManagerObserver mgmtObserver,
        SegmentableEntity segmentableEntity,
        SegmentEntity segmentEntity,
        EnvironmentEntity environmentEntity,
        SegmentDetails multiSegData,
        ErrorReporter reporter) {

        super(mgmtObserver, segmentEntity, null, null, reporter);

        this.model = model;
        this.entity = segmentEntity;
        this.segment = segmentEntity;
        this.multisegment = segmentableEntity;
        this.multiSegData = multiSegData;
        this.parentTransformGroup = multiSegData.getParent();

        embeddedEntityMap = new HashMap<Integer, PositionableEntity>();

        segment.addEntityChildListener(this);
        segment.addEntityPropertyListener(this);
        setEnvironmentEntity(environmentEntity);

        segGeom = new SegmentGeom();
        bounds = segGeom.getBounds();
        segmentMatrix = new Matrix4f();

        boundsGeom = new SegmentGeom();
		
        tu = new TransformUtils();
        mtx = new Matrix4f();
        pnt = new Point3f();
        face = new Vector3f();

        world_normal = new Vector3f();
        world_center = new Point3f();

        left = new double[3];
        rght = new double[3];
        vtx0 = new float[3];
        vtx1 = new float[3];
		
		texCoord = new float[1][];
		facade_texCoord = new float[1][];

        transparency = 1.0f;

        initSegment();
        updateSegment();
    }

    //---------------------------------------------------------------
    // Methods requried by NodeUpdateListener
    //---------------------------------------------------------------

    /**
     * Required by NodeUpdateListener: allows updating scene nodes where
     * the adjustment causes the bounding volume to change (move, rotate,
     * size, geometry change, etc).
     */
    public void updateNodeBoundsChanges(Object src) {

        super.updateNodeBoundsChanges(src);

        if (src == triangleArray) {

            triangleArray.setVertices(
                TriangleArray.COORDINATE_3, coord);

        } else if (src == facadeTriangleArray) {

            facadeTriangleArray.setVertices(
                TriangleArray.COORDINATE_3, facade_coord);

		} else if (src == boundsTriangleArray) {

            boundsTriangleArray.setVertices(
                TriangleArray.COORDINATE_3, bounds_coord);

        } else if (src == transformGroup) {

            // note: since the changeGroupMatrix flag is not being
            // set, this configuration of the transformGroup takes
            // 'precedence' over the setting in AV3DEntityWrapper
            if (changeSegmentMatrix) {

                transformGroup.setTransform(segmentMatrix);
                changeSegmentMatrix = false;

                // set up the segment visibility parameters
                updateNormal();
                updateCenter();
            }
        }
    }

    /**
     * Required by NodeUpdateListener: allows updating of scene node attributes
     * such as color.
     */
    public void updateNodeDataChanges(Object src) {

        if (src == triangleArray) {

            triangleArray.setNormals(normal);
			int[] texType = {VertexGeometry.TEXTURE_COORDINATE_2};
            triangleArray.setTextureCoordinates(texType, texCoord, 1);
            triangleArray.setAttributes(5, 4, tangent, false);

        } else if (src == facadeTriangleArray) {

            facadeTriangleArray.setNormals(facade_normal);
			int[] texType = {VertexGeometry.TEXTURE_COORDINATE_2};
            facadeTriangleArray.setTextureCoordinates(texType, facade_texCoord, 1);
            facadeTriangleArray.setAttributes(5, 4, facade_tangent, false);

        } else if (src == material) {

            material.setDiffuseColor(wallColor);
            material.setAmbientColor(wallColor);

            material.setTransparency(transparency);

        } else if (src == facadeMaterial) {

            facadeMaterial.setDiffuseColor(wallColor);
            facadeMaterial.setAmbientColor(wallColor);

            //facadeMaterial.setTransparency(transparency);
        }
    }

    //----------------------------------------------------------
    // EntityChildListener Methods
    //----------------------------------------------------------

    /**
     * Adds the child to the parent and then starts the model loading process.
     *
     * @param parent Entity ID of parent
     * @param child Entity ID of child
     */
    public void childAdded(int parent, int child) {

        if (!embeddedEntityMap.containsKey(child)) {
            Entity childEntity = segment.getChildAt(segment.getChildIndex(child));

            if (childEntity.isModel()) {

                Boolean shadowState = (Boolean)childEntity.getProperty(
                    entity.getParamSheetName(),
                    Entity.SHADOW_ENTITY_FLAG);
                
                if (shadowState == null) {
                    shadowState = false;
                }

                if (!shadowState) {

                    PositionableEntity pe = (PositionableEntity)childEntity;
                    String category = pe.getCategory();

                    if (category.equals("Category.Window")) {

                        embeddedEntityMap.put(child, pe);
                        pe.addEntityPropertyListener(this);
                        segGeom.addWindow(pe);
                        updateSegment();

                    } else if (category.equals("Category.Door")) {

                        embeddedEntityMap.put(child, pe);
                        pe.addEntityPropertyListener(this);
                        segGeom.addDoor(pe);
                        updateSegment();
                    }
                }
            }
        }
    }

    /**
     * Add the child at the specific location in the list of parent object
     * children.
     *
     * @param parent Entity ID of parent
     * @param child Entity ID of child
     * @param index index to add the child to in the parent child list
     */
    public void childInsertedAt(int parent, int child, int index) {
        childAdded(parent, child);
    }

    /**
     * Removes the child from the parent. The request to remove the model is
     * made and on the next render pass it will be removed from the scene.
     *
     * @param parent Entity ID of the parent
     * @param child Entity ID of the child
     */
    public void childRemoved(int parent, int child) {

        if (embeddedEntityMap.containsKey(child)) {

            PositionableEntity pe = embeddedEntityMap.remove(child);
            pe.removeEntityPropertyListener(this);
            String category = pe.getCategory();

            boolean update = false;
            if (category.equals("Category.Window")) {

                segGeom.removeWindow(pe);
                update = true;

            } else if (category.equals("Category.Door")) {

                segGeom.removeDoor(pe);
                update = true;
            }

            if (update) {
                updateSegment();
            }
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

        if (embeddedEntityMap.containsKey(entityID)) {
            PositionableEntity pe = embeddedEntityMap.get(entityID);
            if (ongoing) {
                if (segGeom.contains(pe)) {
                    String category = pe.getCategory();
                    if (category.equals("Category.Window")) {
                        segGeom.removeWindow(pe);
                    } else if (category.equals("Category.Door")) {
                        segGeom.removeDoor(pe);
                    }
                    updateSegment();
                }
            } else {
                if (!segGeom.contains(pe)) {
                    String category = pe.getCategory();
                    if (category.equals("Category.Window")) {
                        segGeom.addWindow(pe);
                    } else if (category.equals("Category.Door")) {
                        segGeom.addDoor(pe);
                    }
                }
                updateSegment();
            }
        } else if (propertyName.equals(EnvironmentEntity.SHARED_COLOR1_PROP)) {

            Color color = environment.getSharedColor1();
            configColor(color);

        } else if (propertyName.equals(SegmentEntity.STANDARD_FACING_PROP)) {

            updateSegment();
        }
    }

    //---------------------------------------------------------------
    // Local methods
    //---------------------------------------------------------------

    /**
     * Add a child entity's representation to this transform
     *
     * @param wrapper The entity wrapper to add
     */
    protected void addChild(AV3DEntityWrapper wrapper) {

        super.addChild(wrapper);
        childAdded(-1, wrapper.entity.getEntityID());
    }

    /**
     * Remove a child entity's representation from this transform
     *
     * @param wrapper The entity wrapper to remove
     */
    protected void removeChild(AV3DEntityWrapper wrapper) {

        super.removeChild(wrapper);
        childRemoved(-1, wrapper.entity.getEntityID());
    }

    /**
     * Return the center of the model bounds
     *
     * @param val The array to initialize with the center
     */
    protected void getCenter(float[] val) {
        segGeom.getCenter(val);
    }

    /**
     * Return the dimensions of the model bounds.
     *
     * @param val The array to initialize with the dimensions
     */
    protected void getDimensions(float[] val) {
        segGeom.getDimensions(val);
    }

    /**
     * Get the current position
     *
     * @param pos The array to initialize with the position
     */
    void getPosition(double[] pos) {
        segGeom.getPosition(pos);
    }

    /**
     * Get the current rotation
     *
     * @param rot The array to initialize with the rotation
     */
    void getRotation(float[] rot) {
        segGeom.getRotation(rot);
    }

    /**
     * Configure the EnvironmentEntity
     *
     * @param ee The EnvironmentEntity
     */
    void setEnvironmentEntity(EnvironmentEntity ee) {

        if (environment != null) {
            ((Entity)environment).removeEntityPropertyListener(this);
        }
        environment = ee;

        Color color = null;
        if (environment != null) {
            ((Entity)environment).addEntityPropertyListener(this);
            color = environment.getSharedColor1();
        }
        configColor(color);
    }

    /**
     * Configure the wall color
     *
     * @param color The wall Color, or null which will revert
     * the color to the default
     */
    private void configColor(Color color) {

        if (color != null) {
            wallColor = color.getColorComponents(null);
        } else {
            wallColor = DEFAULT_COLOR;
        }
        if (material != null) {

            mgmtObserver.requestDataUpdate(material, this);
        }
        if (facadeMaterial != null) {

            mgmtObserver.requestDataUpdate(facadeMaterial, this);
        }
    }

    /**
     * Generate the segment geometry from it's vertices
     */
    void updateSegment() {

        // the vertex entities
        VertexEntity left_ve = segment.getStartVertexEntity();
        VertexEntity rght_ve = segment.getEndVertexEntity();
        
        // the vertex positions, in local coordinates of the
        // multisegment
        multiSegData.toLocal(left_ve, vtx0);
        multiSegData.toLocal(rght_ve, vtx1);

        // the vertex heights
        float left_height = left_ve.getHeight();
        float rght_height = rght_ve.getHeight();

        // wall thickness
        float wallThickness = getSegmentThickness();

        // miter angle for adjoining segments
        miter_angle = multiSegData.getSegmentMiter(segment, miter_angle);

        // run the geometry generator
        segGeom.createGeom(
            vtx0, left_height, miter_angle[0],
            vtx1, rght_height, miter_angle[1],
            wallThickness);

        // retrieve the data for the node update
        coord = segGeom.getCoords();
        normal = segGeom.getNormals();
		texCoord[0] = segGeom.getTexCoords();
		tangent = segGeom.getTangents();

        facade_coord = segGeom.getFacadeCoords();
        facade_normal = segGeom.getFacadeNormals();
		facade_texCoord[0] = segGeom.getFacadeTexCoords();
		facade_tangent = segGeom.getFacadeTangents();

        segGeom.getMatrix(segmentMatrix);

        // run the bounds geometry generator
        boundsGeom.createGeom(
            vtx0, left_height, 0,
            vtx1, rght_height, 0,
            wallThickness);
		
        bounds_coord = boundsGeom.getCoords();

        // queue the updates
        if (coord != null) {

            mgmtObserver.requestBoundsUpdate(triangleArray, this);
            mgmtObserver.requestDataUpdate(triangleArray, this);

            mgmtObserver.requestBoundsUpdate(facadeTriangleArray, this);
            mgmtObserver.requestDataUpdate(facadeTriangleArray, this);

            mgmtObserver.requestBoundsUpdate(boundsTriangleArray, this);
			
            changeSegmentMatrix = true;
            mgmtObserver.requestBoundsUpdate(transformGroup, this);
        }
    }

    /**
     * Return the normal vector of the editing zone, in world space.
     *
     * @param normal The object to initialize with the normal vector
     */
    void getWorldNormal(Vector3f normal) {
        normal.set(world_normal);
    }

    /**
     * Return the center point of the editing zone, in world space.
     *
     * @param point The object to initialize with the normal vector
     */
    void getWorldCenter(Point3f point) {
        point.set(world_center);
    }

    /**
     * Return the multisegment handler
     *
     * @return The multisegment handler
     */
    SegmentDetails getSegmentDetails() {
        return(multiSegData);
    }

    /**
     * Ignore. The geometry is dynamically generated.
     */
    protected void loadModel() {
    }

    /**
     * Ignore. The segment's transform is calculated during
     * geometry generation - and is not stored in the entity.
     */
    protected void updateTransform() {
    }

    /**
     * Ignore. The segment's transform is calculated during
     * geometry generation - and is not stored in the entity.
     */
    protected void configGroupMatrix() {
    }

    /**
     * Ignore. The segment's transform is calculated during
     * geometry generation - and is not stored in the entity.
     */
    protected void configModelMatrix() {
    }

    /**
     * Eliminate any references that this object may have to make it
     * eligible for GC.
     */
    protected void dispose() {
        super.dispose();
        segment.removeEntityChildListener(this);
        segment.removeEntityPropertyListener(this);
        if (environment != null) {
            ((Entity)environment).removeEntityPropertyListener(this);
            environment = null;
        }
        segment = null;
        multisegment = null;
        multiSegData = null;

        embeddedEntityMap.clear();
    }

    /**
     * Update the normal vector of the editing zone, in world coords.
     */
    private void updateNormal() {
        world_normal.set(0, 0, 1);
        tu.getLocalToVworld(transformGroup, mtx);
        mtx.transform(world_normal);
    }

    /**
     * Update the center point of the editing zone, in world coords.
     */
    private void updateCenter() {
        
        if (segment == null)
            return;

        VertexEntity rght_ve = segment.getEndVertexEntity();
        VertexEntity left_ve = segment.getStartVertexEntity();

        rght_ve.getPosition(rght);
        left_ve.getPosition(left);

        float rght_height = rght_ve.getHeight();
        float left_height = left_ve.getHeight();

        // take the 'highest' as the bounding height
        float height = Math.max(rght_height, left_height);

        face.set(
            (float)(rght[0] - left[0]),
            (float)(rght[1] - left[1]),
            (float)0);

        // width of the wall
        float width = face.length();

        // set the viewpoint to the center of the wall
        world_center.set((width / 2), (height / 2), 0);
        tu.getLocalToVworld(transformGroup, mtx);
        mtx.transform(world_center);
    }

    /**
     * Create the node structure for this wrapper
     */
    private void initSegment() {

        // Create the triangle array
        triangleArray = new TriangleArray();
		
		TextureUnit[] tex;
		if (bumpMapImage != null) {
			tex = new TextureUnit[] {
				getBumpMapTextureUnit()
			};
		} else {
			tex = new TextureUnit[] {
            	AV3DNodeFactory.getDefaultColorMap(),
            	AV3DNodeFactory.getDefaultNormalMap()
        	};
		}
		int num_tex = tex.length;
		
        // Create the material
        material = new Material();
        material.setDiffuseColor(wallColor);
        material.setAmbientColor(wallColor);
        material.setSpecularColor(new float[] {1, 1, 1 });
        material.setShininess(0.8f);

        Appearance app = new Appearance();
        app.setMaterial(material);
        app.setTextureUnits(tex, num_tex);

        // Add the triangle array to a shape object
        Shape3D shape = new Shape3D();
        shape.setAppearance(app);
        shape.setGeometry(triangleArray);

        // Add the shape (triangle array shape parent)
        contentGroup = new Group();
        contentGroup.addChild(shape);

        // Create the triangle array
        facadeTriangleArray = new TriangleArray();

        // Create the material
        facadeMaterial = new Material();
        facadeMaterial.setDiffuseColor(wallColor);
        facadeMaterial.setAmbientColor(wallColor);
        facadeMaterial.setSpecularColor(new float[] {1, 1, 1 });
        facadeMaterial.setShininess(0.8f);

        Appearance facadeApp = new Appearance();
        facadeApp.setMaterial(facadeMaterial);
        facadeApp.setTextureUnits(tex, num_tex);

        // Add the triangle array to a shape object
        Shape3D facadeShape = new Shape3D();
        facadeShape.setAppearance(facadeApp);
        facadeShape.setGeometry(facadeTriangleArray);

        // Add the shape (triangle array shape parent)
        Group facadeGroup = new Group();
        facadeGroup.addChild(facadeShape);

        // Create the bounds triangle array
        boundsTriangleArray = new TriangleArray();
        Appearance proxyApp = new Appearance();
		proxyApp.setVisible(false);
		
        Shape3D boundsProxy = new Shape3D();
		boundsProxy.setUserData("Bound: entity = "+ entity.getEntityID());
		boundsProxy.setGeometry(boundsTriangleArray);
		boundsProxy.setAppearance(proxyApp);

        // Create switch group
        switchGroup = new SwitchGroup();
        switchGroup.addChild(contentGroup);
        switchGroup.addChild(boundsProxy);
        switchGroup.addChild(facadeGroup);
        switchGroup.setActiveChild(0);

        transformModel = new TransformGroup();
        transformModel.addChild(switchGroup);

        //configModelMatrix();
        //transformModel.setTransform(modelMatrix);

        transformGroup = new TransformGroup();
        transformGroup.addChild(transformModel);

        //configGroupMatrix();
        //transformGroup.setTransform(groupMatrix);

        sharedNode = new SharedNode();
        sharedNode.setChild(transformGroup);
    }

    /**
     * Return the segment depth
     *
     * @return The segment depth
     */
    private float getSegmentThickness() {

        Object prop =
            segment.getProperty(
                Entity.EDITABLE_PROPERTIES,
                SegmentEntity.WALL_THICKNESS_PROP);

        float depth = SegmentEntity.DEFAULT_WALL_THICKNESS;
        if (prop instanceof ListProperty) {
            ListProperty list = (ListProperty)prop;
            depth = Float.parseFloat(list.getSelectedValue());
        }

        return (depth);
    }
	
    /**
     * Create and return a TextureUnit
	 *
     * @return A texture unit node representing the bump map texture
     */
    private TextureUnit getBumpMapTextureUnit() {

		// defaults
		boolean repeatS = true;
		boolean repeatT = true;
		
		Texture2D texture = new Texture2D();
		
		int sMode = repeatS ? Texture.BM_WRAP : Texture.BM_CLAMP_TO_EDGE;
		texture.setBoundaryModeS(sMode);
		
		int tMode = repeatT ? Texture.BM_WRAP : Texture.BM_CLAMP_TO_EDGE;
		texture.setBoundaryModeT(tMode);
		
		TextureComponent tc = new ImageTextureComponent2D(
			TextureComponent.FORMAT_RGB,
			bumpMapImage.getWidth(),
			bumpMapImage.getHeight(),
			bumpMapImage);
		
		int texType = Texture.FORMAT_RGB;
		switch(bumpMapImage.getColorModel().getNumComponents()) {
		case 1:
			texType = Texture.FORMAT_LUMINANCE;
			break;
			
		case 2:
			texType = Texture.FORMAT_LUMINANCE_ALPHA;
			break;
			
		case 3:
			texType = Texture.FORMAT_RGB;
			break;
			
		case 4:
			texType = Texture.FORMAT_RGBA;
			break;
		}
		
		texture.setSources(Texture2D.MODE_MIPMAP,
			texType,
			new TextureComponent[]{tc},
			1);
		
		texture.setGenerateMipMap(true);
		texture.setGenerateMipMapHint(Texture.GENERATE_MIPMAP_NICEST);
		texture.setMagFilter(Texture.MAGFILTER_BASE_LEVEL_LINEAR);
		texture.setMinFilter(Texture.MINFILTER_MULTI_LEVEL_LINEAR);
		
		texture.setAnisotropicFilterMode(Texture.ANISOTROPIC_MODE_SINGLE);
		texture.setAnisotropicFilterDegree(2);
		
		TextureUnit tu = new TextureUnit(texture, null, null);
		TextureAttributes attribs = new TextureAttributes();
		attribs.setTextureMode(TextureAttributes.MODE_MODULATE);
		tu.setTextureAttributes(attribs);
		
        return(tu);
    }
}
