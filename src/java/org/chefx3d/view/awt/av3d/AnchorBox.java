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
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.ImageTextureComponent2D;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.Texture;
import org.j3d.aviatrix3d.Texture2D;
import org.j3d.aviatrix3d.TextureAttributes;
import org.j3d.aviatrix3d.TextureComponent;
import org.j3d.aviatrix3d.TextureUnit;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.TriangleFanArray;
import org.j3d.aviatrix3d.TriangleStripArray;
import org.j3d.aviatrix3d.VertexGeometry;

import org.j3d.geom.BoxGenerator;
import org.j3d.geom.GeometryData;

// Local imports
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.util.FileLoader;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

import org.chefx3d.view.common.EditorConstants;

/**
 * Anchor Box container
 *
 * @author Rex Melton
 * @version $Revision: 1.17 $
 */
class AnchorBox implements EditorConstants, NodeUpdateListener {
	
	
	/** String path to 4 way crosshair arrow cursor image*/
    private static final String TEXTURE_DELETE_FILE_PATH=
        "images/2d/delete.png";
    
    /** String path to 4 way crosshair arrow cursor image*/
    private static final String TEXTURE_ANCHOR_FILE_PATH=
        "images/2d/square_box.png";
    
    /** String path to 4 way crosshair arrow cursor image*/
    private static final String TEXTURE_ROTATE_FILE_PATH=
        "images/2d/rotate.png";
	
    /** Reference to the scene management observer */
    private SceneManagerObserver mgmtObserver;

	/** This box's anchor data */
	private AnchorData data;
	
	/** The box's anchor node */
	private AV3DAnchorInformation anchorNode;
	
    /** Flag indicating that a change of transformation parameters
     *  requires the matrix to be updated */
    private boolean changeMatrix;

    /** Local copy of the TransformGroup's matrix */
    private Matrix4f activeMatrix;

    /** Working objects */
    private Vector3f translation;

    /** The TransformGroup */
    TransformGroup transformGroup;
	
    /** The SwitchGroup */
    SwitchGroup switchGroup;
	
	/** The geometry */
	private TriangleStripArray geom;
	
	/** The geometry */
	private Shape3D geomShape;
	
	/** The working color */
	private float[] color;
	
    /** Flag indicating that the color should be changed */
    private boolean changeColor;

	/** Flag indicating whether the box is active or not */
	private boolean enabled;
	
	/** The scale of the box */
	private float scale;
	
	/** scratch vecmath object */
    private Matrix4f mtx0;
	
	/**
	 * Constructor
	 *
     * @param mgmtObserver Reference to the SceneManagerObserver
	 * @param data The AnchorData object
	 * @param entity The associated Entity
	 * @param scale The scale of the box
	 */
	AnchorBox(
		SceneManagerObserver mgmtObserver, 
		AnchorData data,
		PositionableEntity entity,
		float scale) {
		
		this.mgmtObserver = mgmtObserver;
		this.data = data;
		this.scale = scale;
		
		anchorNode = new AV3DAnchorInformation(
			data,
			entity);
		
        activeMatrix = new Matrix4f();
        translation = new Vector3f();
        mtx0 = new Matrix4f();
		
		color = AV3DConstants.DEFAULT_SELECTION_COLOR;
		enabled = true;
		
		initGeometry();
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
                changeMatrix = false;
            }
		} else if (src == switchGroup) {
			
			if (enabled) {
				switchGroup.setActiveChild(0);
			} else {
				switchGroup.setActiveChild(-1);
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
		
		if ((src instanceof Material) && changeColor) {
			((Material)src).setAmbientColor(color);
			changeColor = false;
		}
    }
	
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Re-position the box
	 *
	 * @param translation The new position of the box
     */
    void moveTo(Vector3f translation) {
		
		this.translation.set(translation);
        configMatrix();
    }
	
	/**
	 * Set the scale factor
	 *
	 * @param scale The scale of the box
	 */
	void setScale(float scale) {
		
		this.scale = scale;
        configMatrix();
	}
	
	/**
	 * Turn the box on or off
	 */
	void setEnabled(boolean state) {
	    if (enabled != state) {
			
	        enabled = state;
			
			mgmtObserver.requestBoundsUpdate(switchGroup, this);
		}
	}
	
	/**
	 * Set the color
	 *
	 * @param color The color to set
	 */
	void setColor(float[] color) {
		if (color == null) {
			this.color = AV3DConstants.DEFAULT_SELECTION_COLOR;
			} else {
			this.color = color;
		}
		changeColor = true;
		Material material = geomShape.getAppearance().getMaterial();
		
		mgmtObserver.requestDataUpdate(material, this);
	}
	
    /**
     * Calculate the transform
     */
    private void configMatrix() {
        // configure the transform matrix
        activeMatrix.setIdentity();
        activeMatrix.m00 = scale;
        activeMatrix.m11 = scale;
        activeMatrix.m22 = scale;

        mtx0.setIdentity();
        mtx0.setTranslation(translation);

        activeMatrix.mul(mtx0, activeMatrix);

        changeMatrix = true;
		
		mgmtObserver.requestBoundsUpdate(transformGroup, this);
    }
	
	/** 
	 * Initialize
	 */
	private void initGeometry() {
		
		GeometryData geomData = new GeometryData();
		geomData.geometryType = GeometryData.TRIANGLE_STRIPS;
		geomData.geometryComponents = GeometryData.NORMAL_DATA |
										GeometryData.TEXTURE_2D_DATA;
		 
		BoxGenerator generator;
		
		String filePath= null;
		switch(data){
			case DELETE:
				filePath = TEXTURE_DELETE_FILE_PATH;
		        generator = new BoxGenerator(1f, 1f, 1f);
		        generator.generate(geomData);

				break;
			case ROTATE:
				filePath = TEXTURE_ROTATE_FILE_PATH;
		        generator = new BoxGenerator(1f, 1f, 1f);
		        generator.generate(geomData);

				break;
			default:
		        generator = new BoxGenerator(0.5f, 0.5f, 0.5f);
	            generator.generate(geomData);

				filePath = TEXTURE_ANCHOR_FILE_PATH;
				break;			
		}		
		
		geomShape = createTextureGeom(filePath, geomData);
		geomShape.setPickMask(Shape3D.GENERAL_OBJECT);
		
		
		transformGroup = new TransformGroup();
		transformGroup.addChild(geomShape);
		transformGroup.setUserData(anchorNode);
		transformGroup.setPickMask(TransformGroup.GENERAL_OBJECT);
		

        switchGroup = new SwitchGroup();
        switchGroup.addChild(transformGroup);
        switchGroup.setActiveChild(0);
		
        // configure the transform matrix
        activeMatrix.setIdentity();
        activeMatrix.m00 = scale;
        activeMatrix.m11 = scale;
        activeMatrix.m22 = scale;

		transformGroup.setTransform(activeMatrix);
		//////////////////////////////////////////////////////////////
		// rem: this is absolutely bogus, but the boxes weren't 
		// picking in the selection layer. NFI why.
        TriangleFanArray tfa = new TriangleFanArray();

        float[] vertices = {
            -1, 1, 0,
            -1, -1, 0,
            1, -1, 0,
            1, 1, 0};

        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1};
		
        tfa.setVertices(TriangleFanArray.COORDINATE_3, vertices, 4);
        tfa.setFanCount(new int[]{4}, 1);
        tfa.setNormals(normals);
        tfa.setSingleColor(true, new float[]{1, 1, 1, 0});

        Shape3D geom2 = new Shape3D();
        geom2.setGeometry(tfa);
		
		transformGroup.addChild(geom2);
		//////////////////////////////////////////////////////////////
	}
	
	
	private Shape3D createTextureGeom(String filePath, GeometryData geomData){
		BufferedImage srcImage = null;
		srcImage = loadImage(filePath);
		int format = TextureComponent.FORMAT_RGB;
		
			switch(srcImage.getType()) {
	            case BufferedImage.TYPE_3BYTE_BGR:
	            case BufferedImage.TYPE_CUSTOM:
	            case BufferedImage.TYPE_INT_RGB:
	               break;
	
	            case BufferedImage.TYPE_4BYTE_ABGR:
	            case BufferedImage.TYPE_INT_ARGB:
	                format = TextureComponent.FORMAT_RGBA;
	                break;

                case BufferedImage.TYPE_BYTE_INDEXED:
                case BufferedImage.TYPE_BYTE_BINARY:	
                	ColorModel cm = srcImage.getColorModel();
                    if (cm.hasAlpha()) {
                       format = TextureComponent.FORMAT_RGBA;
                    } else {
                       format = TextureComponent.FORMAT_RGB;
                    }
                    break;

                default:
                   System.out.println("Texture Defaults to RGB: " + srcImage);
	        }

		
		  ImageTextureComponent2D img_comp =
		          new ImageTextureComponent2D(
		        		  format,
		        		  srcImage.getWidth(),
		        		  srcImage.getHeight(),
		        		  srcImage);
		
		int[] tex_type = { VertexGeometry.TEXTURE_COORDINATE_2 };
	    float[][] tex_coord = new float[1][geomData.vertexCount * 2];

		System.arraycopy(geomData.textureCoordinates, 0, tex_coord[0], 0,
                  geomData.vertexCount * 2); 
		  
        geom = new TriangleStripArray(
            false,
            TriangleStripArray.VBO_HINT_STATIC);

		geom.setVertices(TriangleStripArray.COORDINATE_3,
			geomData.coordinates,
			geomData.vertexCount);
		geom.setStripCount(geomData.stripCounts, geomData.numStrips);	     
		geom.setNormals(geomData.normals);
		geom.setTextureCoordinates(tex_type, tex_coord, 1);
      
        Texture2D texture = new Texture2D();
        texture.setSources(Texture.MODE_BASE_LEVEL,
                          Texture.FORMAT_RGBA,
                          new TextureComponent[] {img_comp},
                          1);
        
        TextureAttributes modulate_ta = new TextureAttributes();
        modulate_ta.setTextureMode(TextureAttributes.MODE_MODULATE);

        TextureUnit[] tu = new TextureUnit[1];
        tu[0] = new TextureUnit();
        tu[0].setTexture(texture);
        tu[0].setTextureAttributes(modulate_ta);
        
        
        Material mat = new Material();
        mat.setAmbientColor(AV3DConstants.DEFAULT_SELECTION_COLOR);
        
        Appearance app = new Appearance();       
        app.setMaterial(mat);
        app.setTextureUnits(tu, 1);
        
        Shape3D shape = new Shape3D();
		shape.setGeometry(geom);
		shape.setAppearance(app);
		
		return shape;

	}
	
	 /**
     * Load a single image
     */
    private BufferedImage loadImage(String name) {
        BufferedImage img_comp = null;

        try {
        	FileLoader loader = new FileLoader();
        	Object[]  fileResults  =  loader.getFileURL(name);
           
        	if(fileResults[1] == null) {
        		System.out.println(" File: " + name + " not found for texturing");
        		return null;
        	}

            BufferedInputStream stream = new BufferedInputStream((InputStream)fileResults[1]);
            img_comp = ImageIO.read(stream);
        }
        catch(IOException ioe) {
            System.out.println("Error reading image: " + ioe);
        }

        return img_comp;
    }
}
