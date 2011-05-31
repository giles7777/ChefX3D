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

// External Imports
import java.util.Random;

import org.j3d.aviatrix3d.*;

// Internal Imports
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * A holder of product zone entity data for the Aviatrix3D View.
 *
 * Product zones start out centered about the origin and then moved relative
 * to the parent product that defines them. Any children are directly parented
 * to the product zone that is the active zone for editing. No parenting to
 * a product zone should be allowed if the product zone is not the active zone.
 *
 * @author Ben Yarger
 * @version $Revision: 1.14 $
 */
public class ProductZoneEntityWrapper extends AV3DEntityWrapper {

    //-------------------------------------------------------------------------
    // Constructor
    //-------------------------------------------------------------------------

    ProductZoneEntityWrapper(
            SceneManagerObserver mgmtObserver,
            PositionableEntity entity,
            ErrorReporter reporter) {

        super(mgmtObserver, entity, null, null, reporter);

    }

    //-------------------------------------------------------------------------
    // Local methods
    //-------------------------------------------------------------------------

    /**
     * Override load the Entity scenegraph structure.
     */
    protected void loadModel() {

        float[] size = new float[3];
        double[] position = new double[3];
        float[] rotation = new float[4];

        entity.getSize(size);
        entity.getPosition(position);
        entity.getRotation(rotation);

        //-------------------------------------------------
        // Calculate the bounds from the entity parameters
        //-------------------------------------------------

        float[] scale = new float[3];
        entity.getScale(scale);

        // couldn't determine the bounds. calculate
        // from the entity's parameters
        float x = size[0] * scale[0] / 2;
        float y = size[1] * scale[1] / 2;
        float z = size[2] * scale[2] / 2;
        float[] min = new float[]{-x, -y, -z};
        float[] max = new float[]{x, y, z};

        bounds = new OrientedBoundingBox(min, max, scale);

        //-------------------------------------------------
        // Create the actual geometry
        //-------------------------------------------------

        float width = size[0] * scale[0] / 2.0f;
        float height = size[1] * scale[1] / 2.0f;

        float[] vertices = {
                -width, height, 0.0f,
                -width, -height, 0.0f,
                width, -height, 0.0f,
                width, height, 0.0f};

        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1};

        int[] indices = { 0, 1, 2, 0, 2, 3 };

/*
        System.out.println();
        System.out.println();
        System.out.println("ProductZoneEntityWrapper");
        System.out.println("Entity: "+entity.getName());
        System.out.println("size: "+Arrays.toString(size));
        System.out.println("position: "+Arrays.toString(position));
        System.out.println("rotation: "+Arrays.toString(rotation));
        System.out.println("width: "+width);
        System.out.println("height: "+height);
*/

        // Initialize the AV3D nodes

        IndexedTriangleArray productZoneGeom = new IndexedTriangleArray();
        productZoneGeom.setVertices(TriangleFanArray.COORDINATE_3, vertices, 4);
        productZoneGeom.setIndices(indices, indices.length);
        productZoneGeom.setNormals(normals);

        float r = 0.6f;
        float g = 0.6f;
        float b = 0.6f;

        float[] colors = {r, g, b};

        Shape3D shape = new Shape3D();
        shape.setGeometry(productZoneGeom);

        material = new Material();
        material.setTransparency(0);
        material.setAmbientColor(colors);

        Appearance app = new Appearance();
        app.setMaterial(material);

        shape.setAppearance(app);

        // Reuse the AV3DEntityWrapper fields that are standardized
        // for the scene structure, as well as the transform
        // matrix configurators already in place.

        contentGroup = new Group();
        contentGroup.addChild(shape);

        switchGroup = new SwitchGroup();
        switchGroup.addChild(contentGroup);
        switchGroup.setActiveChild(0);

        transformModel = new TransformGroup();
        transformModel.addChild(switchGroup);
        transformModel.setTransform(modelMatrix);

        transformGroup = new TransformGroup();
        transformGroup.addChild(transformModel);

        configGroupMatrix();
        transformGroup.setTransform(groupMatrix);

        sharedNode = new SharedNode();
        sharedNode.setChild(transformGroup);
    }
}
