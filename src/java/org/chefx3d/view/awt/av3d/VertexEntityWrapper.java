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
import java.awt.Color;

import org.j3d.aviatrix3d.*;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

import org.j3d.renderer.aviatrix3d.geom.Sphere;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.VertexEntity;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Handler of VertexEntity models
 *
 * @author Rex Melton
 * @version $Revision: 1.11 $
 */
class VertexEntityWrapper extends AV3DEntityWrapper {

    /** The radius of the sphere */
    private static final float RADIUS = 0.2032f;

    /**
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param entity The entity that the wrapper object is based around
     * @param reporter The instance to use or null
     */
    VertexEntityWrapper(
        SceneManagerObserver mgmtObserver,
        VertexEntity entity,
        ErrorReporter reporter) {

        super(mgmtObserver, entity, null, null, reporter);
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
        // vertices don't have children
    }

    /**
     * Remove a child entity's representation from this transform
     *
     * @param wrapper The entity wrapper to remove
     */
    protected void removeChild(AV3DEntityWrapper wrapper) {
        // vertices don't have children
    }

    /**
     * Calculate the entity's group transform
     */
    protected void configGroupMatrix() {

        if (entity == null)
            return;

        // get transformation components
        entity.getPosition(pos_array);

        float height =
            (float)ChefX3DRuleProperties.MAXIMUM_WALL_HEIGHT;

        Object objHeight = entity.getProperty(
            Entity.EDITABLE_PROPERTIES,
            VertexEntity.HEIGHT_PROP);

        if (objHeight != null) {
            height = (Float) objHeight;
        }
        pos_array[2] = 10;//height + RADIUS;

        // rem: do verticies really have rotation?
        entity.getRotation(rot_array);

        groupMatrix.setIdentity();

        rotation.set(rot_array);
        translation.set((float)pos_array[0], (float)pos_array[1], (float)pos_array[2]);

        groupMatrix.setRotation(rotation);
        groupMatrix.setTranslation(translation);
    }

    /**
     * Calculate the entity's model transform
     */
    protected void configModelMatrix() {

        if (entity == null)
            return;

        // get transformation components
        entity.getScale(scl_array);

        // rem: do verticies really have scale?

        // configure the transform matrix
        modelMatrix.setIdentity();
        modelMatrix.m00 = scl_array[0];
        modelMatrix.m11 = scl_array[1];
        modelMatrix.m22 = scl_array[2];
    }

    /**
     * Load the scenegraph structure.
     */
    protected void loadModel() {

        Sphere sphereGeom = new Sphere(RADIUS);

        TextureUnit[] tex = {
            AV3DNodeFactory.getDefaultColorMap(),
            AV3DNodeFactory.getDefaultNormalMap()
        };

        AVGeometryBuilder avGeomBuilder = new AVGeometryBuilder();

        material = avGeomBuilder.material(Color.GREEN, Color.GREEN, 0.0f);
        Appearance app = avGeomBuilder.appearance(material , null);
        app.setTextureUnits(tex, 2);

        sphereGeom.setAppearance(app);

        contentGroup = new Group();
        contentGroup.addChild(sphereGeom);

        ///////////////////////////////////////////////////
        // get the bounds
        float[] size = new float[3];
        entity.getSize(size);

        float[] scale = new float[3];
        entity.getScale(scale);

        dim[0] = size[0] * scale[0];
        dim[1] = size[1] * scale[1];
        dim[2] = size[2] * scale[2];

        float[] min = new float[3];
        float[] max = new float[3];

        contentGroup.requestBoundsUpdate();
        BoundingVolume bv = contentGroup.getBounds();
        if ((bv != null) && !(bv instanceof BoundingVoid)) {
            bv.getCenter(center);
            double[] center_d = new double[3];
            center_d[0] = center[0];
            center_d[1] = center[1];
            center_d[2] = center[2];
            entity.setOriginOffset(center_d);

            bv.getExtents(min, max);

        } else {

			float x = size[0] / 2;
			float y = size[1] / 2;
			float z = size[2] / 2;
			min = new float[]{-x, -y, -z};
			max = new float[]{x, y, z};
        }

        bounds = new OrientedBoundingBox(min, max, scale);
        ///////////////////////////////////////////////////
		Shape3D boundsProxy = new Sphere(RADIUS);
		boundsProxy.setUserData("Bound: entity = "+ entity.getEntityID());
        Appearance proxyApp = new Appearance();
		proxyApp.setVisible(false);
		boundsProxy.setAppearance(proxyApp);
		
        switchGroup = new SwitchGroup();
        switchGroup.addChild(contentGroup);
		switchGroup.addChild(boundsProxy);
        switchGroup.setActiveChild(0);

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
}
