/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/gpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.ietf.uri.URL;

import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.*;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

/**
 * A factory class that generates aviatrix Nodes from XNodes.
 * <p>
 *
 * Appearance nodes will automatically have two textures placed on them -
 * the colour texture, which is modulated with the base colour, and a surface
 * normal map. For fixed function rendering, the normal map is effectively
 * ignored, so it is configured in a way to not render. It is, however,
 * available for the shader version of the rendering pipeline, where is is
 * assumed the shader will take care of the combination of the various textures
 * needed for rendering.
 *
 * @author Rex Melton
 * @version $Revision: 1.23 $
 */
class AV3DNodeFactory {

    /** Should we generate mip maps */
    public static final boolean GENERATE_MIP_MAPS = true;

    /** What level of anisotropic texture filtering to use */
    public static final int ANISOTROPIC_DEGREE = 2;

    /** Warning message aviatrix node cannot be loaded */
    private static final String CANNOT_LOAD_NODE_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.cannotLoadNodeMsg";

    /** Warning message that a url could not be decoded */
    private static final String INVALID_URL_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.invalidURLMsg";

    /** Warning message that an image could not be loaded */
    private static final String CANNOT_LOAD_IMAGE_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.cannotLoadImageMsg";

    /** High-Side epsilon float = 0 */
    private static final float ZEROEPS = 0.0001f;

    /**
     * Bytes for a 2x2 white RGB image used for when there is
     * no texture image to use on the geometry
     */
    private static final byte[] NO_COLOUR_IMAGE_SRC = {
        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
    };

    /**
     * Bytes for a 2x2 blue RGB image used for when there is
     * no normal map. Blue represents straight out normals from the
     * surface of the object.
     */
    private static final byte[] NO_NORMALS_IMAGE_SRC = {
        (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0xFF,
        (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0xFF,
    };

    /** White color for ignoring diffuse */
    private static float[] ignoreColor;

    /** Mapping of nodes */
    private static HashMap<String, Integer> nodeMap;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** I18N manager for sourcing messages */
    private I18nManager i18nMgr;

    /** Geometry factory */
    private GeometryFactory geomFactory;

    /** The base URL of the parent */
    private URL baseURL;

    /** The filter to use for url requests, null use baseURL logic instead */
    private URLFilter urlFilter;

    /** The parent URL used for URL filtering, null use baseURL logic instead */
    private String parentURL;

	/** The GLInfo instance */
	private GLInfo gl_info;
	
    /**
     * Shared default normal texture for when none is supplied by the underlying
     * loaded model.
     */
    private static TextureUnit sharedDefaultNormalMap;

    /**
     * Shared default normal texture for when none is supplied by the underlying
     * loaded model.
     */
    private static TextureUnit sharedDefaultColourMap;

    /**
     * Static constructor to build up list of known nodes to transform.
     */
    static {
        nodeMap = new HashMap<String, Integer>();
        nodeMap.put("Shape", new Integer(0));
        nodeMap.put("Group", new Integer(1));
        nodeMap.put("Transform", new Integer(2));
        nodeMap.put("MatrixTransform", new Integer(3));

        ignoreColor = new float[] {1,1,1};

        createDefaultTextures();
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
     */
    AV3DNodeFactory(ErrorReporter reporter) {
        this(reporter, null);
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
	 * @param gl_info The GLInfo
     */
    AV3DNodeFactory(ErrorReporter reporter, GLInfo gl_info) {
        i18nMgr = I18nManager.getManager();
        setErrorReporter(reporter);
		this.gl_info = gl_info;
    }

    /**
     * Register an error reporter
     *
     * @param reporter The new ErrorReporter to use.
     */
    void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Set the base URL to use when constructing from relative URL's
     *
     * @param baseURL The base URL of the parent
     */
    void setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Set the URL filter to use
     *
     * @param urlFilter The URL filter
     * @param parentURL The parent URL
     */
    void setURLFilter(URLFilter filter, String parentURL) {
        this.urlFilter = filter;
        this.parentURL = parentURL;
    }

    /**
     * Return the aviatrix node that cooresponds to the argument XNode
     *
     * @param The XNode
     * @return The aviatrix node
     */
    Node getNode(XNode xnode) {
        Node n = null;
        String node_name = xnode.getNodeName();
        if (nodeMap.containsKey(node_name)) {
            switch(nodeMap.get(node_name)) {
                case 0:
                    n = getShape3D(xnode);
                    break;
                case 1:
                    n = getGroup(xnode);
                    break;
                case 2:
                    n = getTransformGroup(xnode);
                    break;
                case 3:
                    n = getMatrixTransformGroup(xnode);
                    break;
            }
        } else {
            String msg = i18nMgr.getString(CANNOT_LOAD_NODE_MSG) +
                ": "+ xnode.getNodeName();
            errorReporter.warningReport(msg, null);
        }
        return(n);
    }

    /**
     * Get the default shared normal map texture.
     *
     * @param a full texture unit for the normal map
     */
    static TextureUnit getDefaultNormalMap() {
        return sharedDefaultNormalMap;
    }

    /**
     * Get the default shared colour map texture. Assumes that the
     * material node is set up with diffuse colour as this is used
     * in modulate mode.
     *
     * @param a full texture unit for the colour map
     */
    static TextureUnit getDefaultColorMap() {
        return sharedDefaultColourMap;
    }

    /**
     * Create and return a Group
     *
     * @param xnode The XNode
     * @return A Group
     */
    private Group getGroup(XNode xnode) {
        Group group = new Group();
        ArrayList<XNode> child_list =
            (ArrayList<XNode>)xnode.getFieldData("children");
        if (child_list != null) {
            int num_child = child_list.size();
            for (int i = 0; i < num_child; i++) {
                Node node = getNode(child_list.get(i));
                if (node != null) {
                    group.addChild(node);
                }
            }
        }
        return(group);
    }

    /**
     * Create and return a TransformGroup
     *
     * @param xnode The XNode
     * @return A Group
     */
    private TransformGroup getTransformGroup(XNode xnode) {
        TransformGroup group = new TransformGroup();
        ArrayList<XNode> child_list =
            (ArrayList<XNode>)xnode.getFieldData("children");
        if (child_list != null) {
            int num_child = child_list.size();
            for (int i = 0; i < num_child; i++) {
                Node node = getNode(child_list.get(i));
                if (node != null) {
                    group.addChild(node);
                }
            }
        }
        // default values
        float[] center = new float[]{0, 0, 0};
        float[] rotation = new float[]{0, 0, 1, 0};
        float[] scale = new float[]{1, 1, 1};
        float[] scaleOrientation = new float[]{0, 0, 1, 0};
        float[] translation = new float[] {0, 0, 0};

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("center")) {
                center = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("rotation")) {
                rotation = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("scale")) {
                scale = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("scaleOrientation")) {
                scaleOrientation = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("translation")) {
                translation = (float[])xnode.getFieldData(field_name);
            }
        }

        ///////////////////////////////////////////////////////////////////
        // shamelessly ripped off from:
        // org.web3d.vrml.renderer.common.nodes.group.Transform

        Matrix4f tmatrix = new Matrix4f();

        Vector3f tempVec = new Vector3f();
        AxisAngle4f tempAxis = new AxisAngle4f();
        Matrix4f tempMtx1 = new Matrix4f();
        Matrix4f tempMtx2 = new Matrix4f();

        tempVec.x = -center[0];
        tempVec.y = -center[1];
        tempVec.z = -center[2];

        tmatrix.setIdentity();
        tmatrix.setTranslation(tempVec);

        float scaleVal = 1.0f;

        if (floatEq(scale[0], scale[1]) &&
            floatEq(scale[0], scale[2])) {

            scaleVal = scale[0];
            tempMtx1.set(scaleVal);

        } else {
            // non-uniform scale
            tempAxis.x = scaleOrientation[0];
            tempAxis.y = scaleOrientation[1];
            tempAxis.z = scaleOrientation[2];
            tempAxis.angle = -scaleOrientation[3];

            double tempAxisNormalizer =
                1 / Math.sqrt(tempAxis.x * tempAxis.x +
                              tempAxis.y * tempAxis.y +
                              tempAxis.z * tempAxis.z);

            tempAxis.x *= tempAxisNormalizer;
            tempAxis.y *= tempAxisNormalizer;
            tempAxis.z *= tempAxisNormalizer;

            tempMtx1.set(tempAxis);
            tempMtx2.mul(tempMtx1, tmatrix);

            // Set the scale by individually setting each element
            tempMtx1.setIdentity();
            tempMtx1.m00 = scale[0];
            tempMtx1.m11 = scale[1];
            tempMtx1.m22 = scale[2];

            tmatrix.mul(tempMtx1, tempMtx2);

            tempAxis.x = scaleOrientation[0];
            tempAxis.y = scaleOrientation[1];
            tempAxis.z = scaleOrientation[2];
            tempAxis.angle = scaleOrientation[3];
            tempMtx1.set(tempAxis);
        }

        tempMtx2.mul(tempMtx1, tmatrix);

        float magSq = rotation[0] * rotation[0] +
                      rotation[1] * rotation[1] +
                      rotation[2] * rotation[2];

        if(magSq < ZEROEPS) {
            tempAxis.x = 0;
            tempAxis.y = 0;
            tempAxis.z = 1;
            tempAxis.angle = 0;
        } else {
            if ((magSq > 1.01) || (magSq < 0.99)) {

                float mag = (float)(1 / Math.sqrt(magSq));
                tempAxis.x = rotation[0] * mag;
                tempAxis.y = rotation[1] * mag;
                tempAxis.z = rotation[2] * mag;
            } else {
                tempAxis.x = rotation[0];
                tempAxis.y = rotation[1];
                tempAxis.z = rotation[2];
            }

            tempAxis.angle = rotation[3];
        }

        tempMtx1.set(tempAxis);

        tmatrix.mul(tempMtx1, tempMtx2);

        tempVec.x = center[0];
        tempVec.y = center[1];
        tempVec.z = center[2];

        tempMtx1.setIdentity();
        tempMtx1.setTranslation(tempVec);

        tempMtx2.mul(tempMtx1, tmatrix);

        tempVec.x = translation[0];
        tempVec.y = translation[1];
        tempVec.z = translation[2];

        tempMtx1.setIdentity();
        tempMtx1.setTranslation(tempVec);

        tmatrix.mul(tempMtx1, tempMtx2);
        ///////////////////////////////////////////////////////////////////

        group.setTransform(tmatrix);

        return(group);
    }

    /**
     * Create and return a TransformGroup
     *
     * @param xnode The XNode
     * @return A Group
     */
    private TransformGroup getMatrixTransformGroup(XNode xnode) {
        TransformGroup group = new TransformGroup();
        ArrayList<XNode> child_list =
            (ArrayList<XNode>)xnode.getFieldData("children");
        if (child_list != null) {
            int num_child = child_list.size();
            for (int i = 0; i < num_child; i++) {
                Node node = getNode(child_list.get(i));
                if (node != null) {
                    group.addChild(node);
                }
            }
        }
        // default values
        float[] matrix = new float[16];

        // Initialize to Identity
        matrix[0] = 1;
        matrix[5] = 1;
        matrix[10] = 1;
        matrix[15] = 1;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("matrix")) {
                matrix = (float[])xnode.getFieldData(field_name);
            }
        }

        Matrix4f tmatrix = new Matrix4f(matrix);
        tmatrix.transpose();
        group.setTransform(tmatrix);

        return(group);
    }

    /**
     * Compares two floats to determine if they are equal or very close
     *
     * @param val1 The first value to compare
     * @param val2 The second value to compare
     * @return True if they are equal within the given epsilon
     */
    private boolean floatEq(float val1, float val2) {
        float diff = val1 - val2;

        if(diff < 0)
            diff *= -1;

        return (diff < ZEROEPS);
    }

    /**
     * Create and return a Shape3D
     *
     * @param xnode The XNode
     * @return A Shape3D
     */
    private Shape3D getShape3D(XNode xnode) {
        Shape3D shape = new Shape3D();
        XNode appearance_xnode = (XNode)xnode.getFieldData("appearance");
        float[] emissiveColor = new float[3];
        if (appearance_xnode != null) {
            Appearance appearance = getAppearance(appearance_xnode);
            shape.setAppearance(appearance);
            Material material = appearance.getMaterial();
            if (material != null) {
                material.getEmissiveColor(emissiveColor);
            }
        }
        XNode geometry_xnode = (XNode)xnode.getFieldData("geometry");
        if (geometry_xnode != null) {
			if (geomFactory == null) {
        		geomFactory = new GeometryFactory(errorReporter, gl_info);
			}
            Geometry geometry = geomFactory.getGeometry(geometry_xnode, emissiveColor);
            shape.setGeometry(geometry);
        }
        return(shape);
    }

    /**
     * Create and return an Appearance
     *
     * @param xnode The XNode
     * @return An Appearance
     */
    private Appearance getAppearance(XNode xnode) {

        Appearance appearance = new Appearance();
        XNode material_xnode = (XNode)xnode.getFieldData("material");
        XNode texture_xnode = (XNode)xnode.getFieldData("texture");

        if (material_xnode != null) {
            Material material = getMaterial(material_xnode, texture_xnode);
            appearance.setMaterial(material);
        }

        // JC: Loading is assuming a single texture coming in with no
        // normal maps. We're putting in the default normal map here anyway
        // because later on we will provide the ability to have nicer looking
        // rendering based on the materials provided.
        TextureUnit[] textures = new TextureUnit[2];
        textures[1] = sharedDefaultNormalMap;

        if (texture_xnode != null) {
            TextureUnit tu = getTextureUnit(texture_xnode);
            if (tu != null) {
                XNode textureTransform_xnode =
                    (XNode)xnode.getFieldData("textureTransform");
                if (textureTransform_xnode != null) {
                    Matrix4f tt = getTextureTransform(textureTransform_xnode);
                    tu.setTextureTransform(tt);
                }

                textures[0] = tu;
            } else {
                textures[0] = sharedDefaultColourMap;
            }
        } else {
            textures[0] = sharedDefaultColourMap;
        }

        appearance.setTextureUnits(textures, 2);

        return(appearance);
    }

    /**
     * Create and return a TextureTransform matrix
     *
     * @param xnode The XNode
     * @return A TextureTransform matrix
     */
    private Matrix4f getTextureTransform(XNode xnode) {

        Matrix4f matrix = new Matrix4f();
        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();
        Matrix4f T = new Matrix4f();
        Matrix4f C = new Matrix4f();
        Matrix4f R = new Matrix4f();
        Matrix4f S = new Matrix4f();
        AxisAngle4f al = new AxisAngle4f();

        // default values
        float[] center = new float[]{0, 0};
        float rotation = 0;
        float[] scale = new float[]{1, 1};
        float[] translation = new float[]{0, 0};

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("center")) {
                center = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("rotation")) {
                rotation = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("scale")) {
                scale = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("translation")) {
                translation = (float[])xnode.getFieldData(field_name);
            }
        }

        v1.x = translation[0];
        v1.y = translation[1];
        v1.z = 0;

        T.setIdentity();
        T.setTranslation(v1);

        v2.x = -center[0];
        v2.y = -center[1];
        v2.z = 0;

        C.setIdentity();
        C.setTranslation(v2);

        al.x = 0;
        al.y = 0;
        al.z = 1;
        al.angle = rotation;

        R.setIdentity();
        R.setRotation(al);

        S.setIdentity();
        S.m00 = scale[0];
        S.m11 = scale[1];
        S.m22 = 1.0f;

        matrix.setIdentity();

        matrix.mul(C);
        matrix.mul(S);
        matrix.mul(R);

        v2.negate();
        C.setIdentity();
        C.set(v2);

        matrix.mul(C);
        matrix.mul(T);

        return(matrix);
    }

    /**
     * Create and return a Material
     *
     * @param xnode The XNode
     * @param texture The texture node or null
     * @return A Material
     */
    private Material getMaterial(XNode xnode, XNode texture) {

        // default values
        float ambientIntensity = 0.2f;
        float[] diffuseColor = new float[]{0.8f, 0.8f, 0.8f};
        float[] emissiveColor = new float[]{0, 0, 0};
        float shininess = 0.2f;
        float[] specularColor = new float[]{ 0, 0, 0};
        float transparency = 0;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("ambientIntensity")) {
                ambientIntensity = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("diffuseColor")) {
                diffuseColor = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("emissiveColor")) {
                emissiveColor = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("shininess")) {
                shininess = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("specularColor")) {
                specularColor = (float[])xnode.getFieldData(field_name);
            } else if (field_name.equals("transparency")) {
                transparency = (Float)xnode.getFieldData(field_name);
            }
        }

        float[] ambientColor = new float[3];
        ambientColor[0] = diffuseColor[0] * ambientIntensity;
        ambientColor[1] = diffuseColor[1] * ambientIntensity;
        ambientColor[2] = diffuseColor[2] * ambientIntensity;

        if (texture != null) {
            // TODO: Should only do this for 3 or 4 component textures.
            // For now do it always and get intensity textures wrong
            diffuseColor[0] = ignoreColor[0];
            diffuseColor[1] = ignoreColor[1];
            diffuseColor[2] = ignoreColor[2];
        }

        transparency = 1.0f - transparency;

        Material material = new Material(
            ambientColor,
            emissiveColor,
            diffuseColor,
            specularColor,
            shininess,
            transparency);

        return(material);
    }

    /**
     * Create and return a TextureUnit corresponding to the given node
     * definition. Will load the referenced URL.
     *
     * @param xnode The XNode to generate data from.
     * @return A texture unit node representing the basic texture
     */
    TextureUnit getTextureUnit(XNode xnode) {

        if (!xnode.getNodeName().equals("ImageTexture"))
            return null;

        TextureUnit tu = null;

        // defaults
        boolean repeatS = true;
        boolean repeatT = true;
        String[] url = null;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("url")) {
                url = (String[])xnode.getFieldData(field_name);
            } else if (field_name.equals("repeatS")) {
                repeatS = (Boolean)xnode.getFieldData(field_name);
            } else if (field_name.equals("repeatT")) {
                repeatT = (Boolean)xnode.getFieldData(field_name);
            }
        }

        if (url != null) {
            URL[] convertedURLs = getURL(url);

            // Only work with the first one currently
            URL imgURL = convertedURLs[0];
            if (imgURL != null) {
                BufferedImage image = null;
                try {

                    FileLoader fileLookup = new FileLoader();
                    Object[] file = fileLookup.getFileURL(imgURL.toExternalForm(), true);
                    InputStream modelStream = (InputStream)file[1];
                    image = ImageIO.read(modelStream);

                } catch (IOException ioe) {
                    String msg = i18nMgr.getString(CANNOT_LOAD_IMAGE_MSG) +
                        ": "+ imgURL;
                    errorReporter.warningReport(msg, null);
                } catch (IllegalArgumentException iae) {
                    String msg = i18nMgr.getString(CANNOT_LOAD_IMAGE_MSG) +
                        ": "+ imgURL;
                    errorReporter.warningReport(msg, null);
                }

                if (image != null) {

                    Texture2D texture = new Texture2D();

                    int sMode = repeatS ? Texture.BM_WRAP : Texture.BM_CLAMP_TO_EDGE;
                    texture.setBoundaryModeS(sMode);

                    int tMode = repeatT ? Texture.BM_WRAP : Texture.BM_CLAMP_TO_EDGE;
                    texture.setBoundaryModeT(tMode);

                    TextureComponent tc = new ImageTextureComponent2D(
                        TextureComponent.FORMAT_RGB,
                        image.getWidth(),
                        image.getHeight(),
                        image);

                    int texType = Texture.FORMAT_RGB;
                    switch(image.getColorModel().getNumComponents()) {
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

                    if (GENERATE_MIP_MAPS) {
                        texture.setSources(Texture2D.MODE_MIPMAP,
                            texType,
                            new TextureComponent[]{tc},
                            1);

                        texture.setGenerateMipMap(true);
                        texture.setGenerateMipMapHint(Texture.GENERATE_MIPMAP_NICEST);
                        texture.setMagFilter(Texture.MAGFILTER_BASE_LEVEL_LINEAR);
                        texture.setMinFilter(Texture.MINFILTER_MULTI_LEVEL_LINEAR);

                    } else {
                        texture.setSources(
                            Texture2D.MODE_BASE_LEVEL,
                            texType,
                            new TextureComponent[]{tc},
                            1);
                    }

                    if (ANISOTROPIC_DEGREE > 1) {
                        texture.setAnisotropicFilterMode(Texture.ANISOTROPIC_MODE_SINGLE);
                        texture.setAnisotropicFilterDegree(ANISOTROPIC_DEGREE);
                    }

                    tu = new TextureUnit(texture, null, null);
                    TextureAttributes attribs = new TextureAttributes();
                    attribs.setTextureMode(TextureAttributes.MODE_MODULATE);
                    tu.setTextureAttributes(attribs);
                }
            }
        }

        return(tu);
    }

    /**
     * Convenience initialisation method to create the default shared
     * textures used by geometry.
     */
    private static void createDefaultTextures() {
        TextureComponent2D no_normal_comp =
            new ByteTextureComponent2D(TextureComponent.FORMAT_RGB,
                                       2,
                                       2,
                                       NO_NORMALS_IMAGE_SRC);

        TextureComponent2D no_texture_comp =
            new ByteTextureComponent2D(TextureComponent.FORMAT_RGB,
                                       2,
                                       2,
                                       NO_COLOUR_IMAGE_SRC);

        Texture2D colour_tex =
            new Texture2D(Texture2D.FORMAT_RGB, no_texture_comp);
        colour_tex.setBoundaryModeS(Texture.BM_WRAP);
        colour_tex.setBoundaryModeT(Texture.BM_WRAP);

        TextureAttributes attribs = new TextureAttributes();
        attribs.setTextureMode(TextureAttributes.MODE_MODULATE);

        sharedDefaultColourMap = new TextureUnit();
        sharedDefaultColourMap.setTexture(colour_tex);
        sharedDefaultColourMap.setTextureAttributes(attribs);

        Texture2D normal_tex =
            new Texture2D(Texture2D.FORMAT_RGB, no_normal_comp);
        normal_tex.setBoundaryModeS(Texture.BM_WRAP);
        normal_tex.setBoundaryModeT(Texture.BM_WRAP);

        // set up the texture modes for this texture so that it is ignored
        // in normal fixed function rendering, but will still be usable
        // by the shaders
        attribs = new TextureAttributes();
        attribs.setTextureMode(TextureAttributes.MODE_COMBINE);
        attribs.setCombineMode(false,  TextureAttributes.COMBINE_MODULATE);
        attribs.setCombineMode(true,  TextureAttributes.COMBINE_MODULATE);
        attribs.setCombineSource(false, 0,  TextureAttributes.SOURCE_PREVIOUS_UNIT);
        attribs.setCombineSource(true,  0, TextureAttributes.SOURCE_PREVIOUS_UNIT);
        attribs.setCombineSource(false, 1,  TextureAttributes.SOURCE_CONSTANT_COLOR);
        attribs.setCombineSource(true,  1, TextureAttributes.SOURCE_CONSTANT_COLOR);
        attribs.setBlendColor(1, 1, 1, 1);

        sharedDefaultNormalMap = new TextureUnit();
        sharedDefaultNormalMap.setTexture(normal_tex);
        sharedDefaultNormalMap.setTextureAttributes(attribs);
    }

    /**
     * Return a URL if one can be decoded, null otherwise
     *
     * @param url_strings An array of url candidates.
     * @return The list of decoded URLs
     */
    private URL[] getURL(String[] url_strings) {

        URL[] retVal = new URL[url_strings.length];

        for(int i = 0; i < url_strings.length; i++) {
            URL url = null;
            String url_string = url_strings[i];

            if (url_string.startsWith("\"")) {
                url_string = url_string.substring(1, (url_string.length()-1));
            }

            try {
                // chances are, the url is relative and this
                // will ex-out for "no protocol"
                url = new URL(url_string);
            } catch (MalformedURLException murle) {
            }

            if (url == null) {
                if (urlFilter != null) {
                    String filtered = null;
                    try {
                        filtered = urlFilter.filterURL(parentURL, url_string);
                        url = new URL(filtered);
                    } catch(MalformedURLException murle) {
                        String msg = i18nMgr.getString(INVALID_URL_MSG) +
                            ": "+ filtered;
                        errorReporter.warningReport(msg, null);
                    }
                } else {
                    try {
                        ///////////////////////////////////////////////////////
						// rem: this produced a 'bad' result, not sure why....
                        //url = new URL(baseURL, url_string);
						///////////////////////////////////////////////////////
						url = new URL(baseURL.toExternalForm() + url_string);
                    } catch (MalformedURLException murle) {
                        String msg = i18nMgr.getString(INVALID_URL_MSG) +
                            ": "+ url_string;
                        errorReporter.warningReport(msg, null);
                    }
                }
            }

            retVal[i] = url;
        }

        return retVal;
    }
}
