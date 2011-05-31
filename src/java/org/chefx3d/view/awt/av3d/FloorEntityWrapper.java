/****************************************************************************
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

//External imports
import java.awt.Color;
import java.util.List;
import java.util.MissingResourceException;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;

import org.j3d.aviatrix3d.*;

import org.j3d.util.I18nManager;
import org.j3d.util.TriangleUtils;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.EnvironmentEntity;
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Handler for a FloorEntity zone.
 *
 * @author Rex Melton
 * @version $Revision: 1.27 $
 */
class FloorEntityWrapper extends AV3DEntityWrapper {

    /** Default floor extent from the origin */
    private static final float DEFAULT_EXTENT = 1000;

	/** Default number of vertices along each floor axis */
	private static final int DEFAULT_VERTEX_PER_AXIS = 5;
	
    /** Vertices per axis param key */
    private static final String FLOOR_VERTEX_PER_AXIS =
        "floorVertexPerAxisValue";

    /** Default floor color */
    private static final float[] DEFAULT_FLOOR_COLOR = new float[]{1f, 0.6f, 0.6f};

	/** Key for a floor model in the i18n properties */
	private static final String FLOOR_MODEL_URL = 
		"org.chefx3d.view.awt.av3d.FloorEntityWrapper.floorModelURL";
	
    /** The environment entity */
    private EnvironmentEntity environment;

    /** The floor geometry */
    private IndexedTriangleArray floorGeom;

    /** Material used to render the floor colour with */
    private Material material;

    /** Array used for setting up the floor color */
    private float[] floorColorArray;

	/** Working geometry tesselation value */
	private int vertexPerAxis;
	
    /**
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param zoneEntity The entity that this wrapper represents
     * @param environmentEntity The EnvironmentEntity
     * @param reporter The instance to use or null
     */
    FloorEntityWrapper(
        SceneManagerObserver mgmtObserver,
        PositionableEntity zoneEntity,
        EnvironmentEntity environmentEntity,
        ErrorReporter reporter) {

        super(mgmtObserver, zoneEntity, null, null, reporter);

        setEnvironmentEntity(environmentEntity);

		vertexPerAxis = DEFAULT_VERTEX_PER_AXIS;
        Object vertexPerAxisValue_object =
            ApplicationParams.get(FLOOR_VERTEX_PER_AXIS);
        if ((vertexPerAxisValue_object != null) &&
			(vertexPerAxisValue_object instanceof Integer)) {

            int tmp_value = ((Integer)vertexPerAxisValue_object).intValue();
            if (tmp_value > 1) {
                vertexPerAxis = tmp_value;
            }
        }

        createFloorGeom();
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

        super.updateNodeBoundsChanges(src);
    }

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {

        if(src == floorGeom) {

            floorGeom.setSingleColor(false, floorColorArray);

        } else if(src == material) {

            material.setDiffuseColor(floorColorArray);

            // TODO: Setting emissive because floor vertices are outside lighting bounds.
            material.setEmissiveColor(floorColorArray);
            material.setDiffuseColor(new float[] {0,0,0});
            material.setAmbientColor(new float[] {0,0,0});
        } else {

            super.updateNodeDataChanges(src);
        }
    }

    //----------------------------------------------------------
    // Methods for EntityPropertyListener
    //----------------------------------------------------------

    public void propertiesUpdated(List<EntityProperty> properties) {
    }

    public void propertyAdded(int entityID, String propertySheet,
        String propertyName) {
    }

    public void propertyRemoved(int entityID, String propertySheet,
        String propertyName) {
    }

    public void propertyUpdated(int entityID, String propertySheet,
        String propertyName, boolean ongoing) {

        if (propertyName.equals(EnvironmentEntity.GROUND_COLOR_PROP)) {

            Color groundColor = environment.getGroundColor();
            configFloorColor(groundColor);
        }
    }

    // ----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Eliminate any references that this object may have to make it
     * eligible for GC.
     */
    protected void dispose() {
        super.dispose();
        if (environment != null) {
            ((Entity)environment).removeEntityPropertyListener(this);
            environment = null;
        }
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

        Color groundColor = null;
        if (environment != null) {
            ((Entity)environment).addEntityPropertyListener(this);
            groundColor = environment.getGroundColor();
        }
        configFloorColor(groundColor);
    }

    /**
     * Ignore. The geometry is dynamically generated.
     */
    protected void loadModel() {
    }

    /**
     * Create the floor geometry
     */
    private void createFloorGeom() {

		////////////////////////////////////////////////////////////
		// rem: this is a hack to use a model for the floor rather
		// than generating the geometry dynamically.
		I18nManager i18nMgr = I18nManager.getManager();
		try {
			String floor_model_url = i18nMgr.getString(FLOOR_MODEL_URL);
			if (floor_model_url != null) {
				alternate_url_string = floor_model_url;
				super.loadModel();
				return;
			}
		} catch (MissingResourceException mre) {
			// fail silently, this is a hack - remember?
		}
		////////////////////////////////////////////////////////////
				
		float min_ext = -DEFAULT_EXTENT;
		float max_ext = DEFAULT_EXTENT;
		
        float[] min = new float[]{min_ext, min_ext, -1};
        float[] max = new float[]{max_ext, max_ext, 0};
        float[] scale = new float[]{1, 1, 1};
        bounds = new OrientedBoundingBox(min, max, scale);

		int intervals = vertexPerAxis - 1;
		float extent = max_ext - min_ext;
		float vertex_spacing = extent / intervals;
		float texCoord_spacing = 1.0f / intervals;
		
		int num_vertices = vertexPerAxis * vertexPerAxis;
		int num_coord = num_vertices * 3;
		int num_texCoord = num_vertices * 2;
		int num_triangles = intervals * intervals * 2;
		int num_indices = num_triangles * 3;
		
		float[] vertices = new float[num_coord];
		float[] normals = new float[num_coord];
		float[][] texCoord = new float[1][];
		texCoord[0] = new float[num_texCoord];
		int[] indices = new int[num_indices];
		
		// build vertices, normals and tex coords
		float y, x;
		float t_y, t_x;
		int c_idx = 0;
		int t_idx = 0;
		for (int y_idx = 0; y_idx < vertexPerAxis; y_idx++) {
			
			y = max_ext - y_idx * vertex_spacing;
			t_y = (intervals - y_idx) * texCoord_spacing;
			
			for (int x_idx = 0; x_idx < vertexPerAxis; x_idx++) {
				
				x = min_ext + x_idx * vertex_spacing;
				t_x = x_idx * texCoord_spacing;
				
				vertices[c_idx] = x;
				vertices[c_idx+1] = y;
				
				normals[c_idx+2] = 1;
				
				texCoord[0][t_idx] = t_x;
				texCoord[0][t_idx+1] = t_y;
				
				c_idx += 3;
				t_idx += 2;
			}
		}
		
		// build the indices
		int row_off;
		int next_row_off;
		int idx = 0;
		for (int y_idx = 0; y_idx < intervals; y_idx++) {
			
			row_off = y_idx * vertexPerAxis;
			next_row_off = row_off + vertexPerAxis;
			
			for (int x_idx = 0; x_idx < intervals; x_idx++) {
				
				indices[idx++] = row_off + x_idx;
				indices[idx++] = next_row_off + x_idx;
				indices[idx++] = row_off + x_idx + 1;
				
				indices[idx++] = row_off + x_idx + 1;
				indices[idx++] = next_row_off + x_idx;
				indices[idx++] = next_row_off + x_idx + 1;
			}
		}
		
		///////////////////////////////////////////////////////////////
		// legacy, simple geometry
		/*
        float[] vertices = {
            min_ext, max_ext, 0,
            min_ext, min_ext, 0,
            max_ext, min_ext, 0,
            max_ext, max_ext, 0};

        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1};

        float[][] texCoord = {
            {0, 1,  0, 0,  1, 0, 1, 1 }
        };
		
        int[] indices = { 0, 1, 2, 0, 2, 3 };
		*/
		///////////////////////////////////////////////////////////////
		
        int[] texType = { VertexGeometry.TEXTURE_COORDINATE_2 };
        float[] tangents = new float[vertices.length * 4 / 3];

        TriangleUtils.createTangents(indices.length / 3,
                                     indices,
                                     vertices,
                                     normals,
                                     texCoord[0],
                                     tangents);

        floorGeom = new IndexedTriangleArray();
        floorGeom.setVertices(TriangleFanArray.COORDINATE_3, vertices, num_vertices);
        floorGeom.setIndices(indices, indices.length);
        floorGeom.setNormals(normals);
        floorGeom.setTextureCoordinates(texType, texCoord, 1);
        floorGeom.setAttributes(5, 4, tangents, false);
        floorGeom.setSingleColor(false, floorColorArray);

        TextureUnit[] tex = {
            AV3DNodeFactory.getDefaultColorMap(),
            AV3DNodeFactory.getDefaultNormalMap()
        };

        material = new Material();
        material.setDiffuseColor(floorColorArray);

        // TODO: Setting emissive because floor vertices are outside lighting bounds.
        material.setEmissiveColor(floorColorArray);
        material.setDiffuseColor(new float[] {0, 0, 0});
        material.setAmbientColor(new float[] {0, 0, 0});
        //material.setAmbientColor(floorColorArray);

        material.setSpecularColor(new float[] {1, 1, 1});
        material.setShininess(0.8f);

        Appearance app = new Appearance();
        app.setMaterial(material);
        app.setTextureUnits(tex, 2);
		
		IndexedTriangleArray proxyGeom = new IndexedTriangleArray();
        proxyGeom.setVertices(TriangleFanArray.COORDINATE_3, vertices, num_vertices);
        proxyGeom.setIndices(indices, indices.length);
		
        Appearance proxyApp = new Appearance();
		proxyApp.setVisible(false);
		
		Shape3D boundsProxy = new Shape3D();
		boundsProxy.setUserData("Bound: entity = "+ entity.getEntityID());
		boundsProxy.setGeometry(proxyGeom);
		boundsProxy.setAppearance(proxyApp);
		
        Shape3D floor = new Shape3D();
        floor.setAppearance(app);
        floor.setGeometry(floorGeom);

        contentGroup = new Group();
        contentGroup.addChild(floor);

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
     * Initialize the floor color
     *
     * @param floorColor The color value
     */
    private void configFloorColor(Color floorColor) {

        if (floorColor == null) {
            floorColorArray = DEFAULT_FLOOR_COLOR;
        } else {
            floorColorArray = floorColor.getColorComponents(null);
        }

        if (floorGeom != null) {
            mgmtObserver.requestDataUpdate(floorGeom, this);
            mgmtObserver.requestDataUpdate(material, this);
        }
    }
}
