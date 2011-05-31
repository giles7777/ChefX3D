/*****************************************************************************
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

//external imports
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import java.awt.image.BufferedImage;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

import java.awt.geom.Rectangle2D;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Vector3f;

// internal imports
import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Geometry;
import org.j3d.aviatrix3d.ImageTextureComponent2D;
import org.j3d.aviatrix3d.IndexedLineArray;
import org.j3d.aviatrix3d.IndexedLineStripArray;
import org.j3d.aviatrix3d.LineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.LineStripArray;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.QuadArray;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.Texture;
import org.j3d.aviatrix3d.Texture2D;
import org.j3d.aviatrix3d.TextureComponent;
import org.j3d.aviatrix3d.TextureUnit;
import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.TriangleArray;
import org.j3d.aviatrix3d.VertexGeometry;

import org.j3d.renderer.aviatrix3d.geom.Box;
import org.j3d.renderer.aviatrix3d.geom.Sphere;


/**
 * A tool to handle the creation of common objects, such as
 * boxes, lines, and spheres, and that also provides some
 * elementary translation methods to move those objects around.
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.3 $
 */
public class AVGeometryBuilder {

	/** 
	 * Applies fractional metrics according to the third parameter. <br> 
	 * If true, split pixels; if false, do not split pixels. <br>
	 * It seems that fractional metrics should be used if the font 
	 * size is about 10 or smaller, and otherwise fractional metrics 
	 * should not be used.  However, it is usually best to try both 
  	 * and see which one comes out better. 
	 */
	private FontRenderContext frc;

    /** current material appearance. */
    private Appearance currentAppearance;
    
    /** current material transparency. */
    private Material currentMaterial;
    
    /** Working matrix */
    private Matrix4f mtrx;

    /**
     * Default constructor.
     */
    public AVGeometryBuilder(){
    	this(false);
    }
    
    /**
     * Constructor that sets the current Appearance
     * and the current Material to default values.
	 *
     * @param useFractionalMetrics boolean value that answers:
     * Should we use fractional metrics when building fonts?<br> 
	 * If true, split pixels; if false, do not split pixels. <br>
	 * It seems that fractional metrics should be used if the font 
	 * size is about 10 or smaller, and otherwise fractional metrics 
	 * should not be used.  However, it is usually best to try both 
  	 * and see which one comes out better. 
     */
    public AVGeometryBuilder(boolean useFractionalMetrics){

    	frc = new FontRenderContext(null, true, useFractionalMetrics);
        currentMaterial = new Material();
        currentAppearance = new Appearance();
        mtrx = new Matrix4f();
    }
    
    //----------------------------------------------------------
    // Methods for pocketing data to allow creation shortcuts later
    //----------------------------------------------------------
    
    public void setMaterial(Material newMaterial){
    	currentMaterial = newMaterial;
    }

    
    /**
     * This will pocket the input appearance for use in the create()
     * method.
	 *
     * @param newAppearance Appearance to use in the create() method. 
     */
    public void setAppearance(Appearance newAppearance){
    	currentAppearance = newAppearance;
    }
    
    
    
    //----------------------------------------------------------
    // Shortcut methods for creating Appearances and Materials
    //----------------------------------------------------------

    
    /**
     * Return the current appearance.  If user wants to pocket a custom
     * appearance, they should create one and then explicitly call the 
     * setAppearance(Appearance a) method.
	 *
     * @return the current Appearance
     */
     public Appearance appearance(){
     	return currentAppearance;
     }
     
     
     /**
      * Return the current material.  If user wants to pocket a custom
      * material, they should create one and then explicitly call the 
      * setMaterial(Material m) method.
	  *
      * @return the current Material
      */
      public Material material(){
      	return currentMaterial;
      }

     
     /**
      * Return a custom appearance.
	  *
      * @param m Material to attach to the appearance.
      * @param lineAtts A LineAttributes ( can have a specific line width
      * set, and stipple pattern specified with one of the class constants
      * LineAttributes.PATTERN_* ).  Value can be NULL.
      * @return a new Appearance with the specified material and line attributes
      */
     public Appearance appearance(Material m, LineAttributes lineAtts){

  	 	 // attach the lineAttributes and material to an Appearance
  	 	 Appearance appearance = new Appearance();
  	 	 appearance.setLineAttributes(lineAtts);
  	 	 appearance.setMaterial(m);
  	 	 
  	 	 return appearance;
     }
     
     
     /**
      * Return a custom material.
	  *
      * @param diffuse A float array containing the diffuse color
      * components for the line's appearance.
      * @param emissive A float array containing the emissive color
      * components for the line's appearance.
      * @param transparency - The amount of transparency to be used 
      * for this material for the front or combined faces. 
      * A value of 1 is fully opaque and 0 is totally transparent.  
      * @return A Material node with the designated colors and transparency
      */
     public Material material(float[] diffuse, 
    		 				  float[] emissive, 
    		 				  float transparency){
    	 
    	 // set the material
    	 Material m = new Material();
    	 m.setDiffuseColor(diffuse);
    	 m.setEmissiveColor(emissive);
    	 m.setTransparency(transparency);
    	 return m;
     }
     
     
     /**
      * Return a custom material.
	  *
      * @param diffuse - diffuse Color
      * @param emissive - emissive Color
      * @param transparency - The amount of transparency to be used 
      * for this material for the front or combined faces. 
      * A value of 1 is fully opaque and 0 is totally transparent. 
      * @return a Material node with the given colors and transparency
      */
     public Material material(Color diffuse, 
   		  				   	  Color emissive, 
   		  				   	  float transparency){
   	  return material(diffuse.getComponents(new float[6]),
			 	  		  emissive.getComponents(new float[6]),
			 	  		  transparency);
     }
    
    
    //----------------------------------------------------------
    // Methods for adjusting nodes (translation, etc.)
    //----------------------------------------------------------
    
    
    /**
     * Create a transform group with the appropriate translation.
     * 
     * @param xPosition the x-value used to set the translation of the
     * transform group
     * @param yPosition the y-value used to set the translation of the
     * transform group
     * @param zPosition the z-value used to set the translation of the
     * transform group
     * @return a transformGroup that has been translated according to
     * the input parameters.
     */
    public TransformGroup translate(Node node,
                                   float xPosition,
                                   float yPosition,
                                   float zPosition){
        mtrx.setIdentity();
        mtrx.setTranslation(new Vector3f(xPosition, yPosition, zPosition));
        TransformGroup transformGroup = new TransformGroup(mtrx);
        transformGroup.addChild(node);

        return transformGroup;
    }
    
    /**
     * Return a matrix that's been translated by the input point
     * 
     * @param xPosition the x-value used to set the translation of the
     * transform group
     * @param yPosition the y-value used to set the translation of the
     * transform group
     * @param zPosition the z-value used to set the translation of the
     * transform group
     */
    public Matrix4f getTransform(float xPosition,
                          	  	 float yPosition,
                          	  	 float zPosition){
        mtrx.setIdentity();
        mtrx.setTranslation(new Vector3f(xPosition, yPosition, zPosition));
        return mtrx;
    }
    
    
    //----------------------------------------------------------
    // Methods for creating org.j3d.aviatrix3d.Geometry objects
    //----------------------------------------------------------
    
    
    /**
     * Creates a solid Geometry LineArray.  
     *
     * @param vertices float array with vertices already set - at least six points
     * per line segment, three points per vertex (ie: x0, y0, z0, x1, y1, z1).
     *
     * @return a Geometry object containing a LineArray with the given vertices
     */
     public Geometry lineArray(float[] vertices){

         // set the geometry of the line
         LineArray lineArray = new LineArray();
         lineArray.setVertices(LineArray.COORDINATE_3, vertices);
         return lineArray;
    }
     
     
     /**
      * Creates a solid Geometry LineStripArray.  
      *
      * @param vertices float array with vertices already set.  Assumes the vertices
      * are chained to together, three points per vertex.  Thus vertices of length 9
      * implies two line segments, ordered: x0, y0, z0, x1, y1, z1, x2, y2, z2.
      *
      * @return a Geometry object containing a LineStripArray with the given vertices
      */
      public Geometry lineStripArray(float[] vertices){

          // set the geometry of the line
    	  LineStripArray lineStripArray = new LineStripArray();
    	  lineStripArray.setVertices(LineArray.COORDINATE_3, vertices);
          return lineStripArray;
     }
      
    
    /**
     * Creates an IndexedLineArray.  Because this is NOT a strip array,
     * each end of each segment must be specified, but you do NOT need to
     * include a "-1" value to separate each segment.  In other words,
     * indices of { 4, 5, 8, 2} mean two total segments: one segment goes 
     * from 4 to 5 and one goes from 8 to 2.  There are no 'connected' segments
     * in the manner of an IndexedLineStripArray.
     *
     * @param vertices float array with vertices already set - at least six points
     * per line segment, three points per vertex (ie: x0, y0, z0, x1, y1, z1).
     * @param indices int[] array, this method assumes you want to use ALL
     * the values in the indices array, so be careful!
     * @return Geometry with the given LineArray as geometry
     */
     public Geometry indexedLineArray(float[] vertices,
    		 						  int[] indices){

    	// set the geometry of the line
    	 IndexedLineArray indexedLineArray = new IndexedLineArray();
    	 indexedLineArray.setVertices(IndexedLineArray.COORDINATE_3, vertices);
    	 indexedLineArray.setIndices(indices, indices.length);
    	 
    	 return indexedLineArray;
     }
     
     
     /**
      * Create an indexed line strip array.  Because this is a strip array,
      * each set of indices (separated by a "-1" value) is assumed to be one 
      * long chain of vertices.  In other words, indices of { 7, 2, 4 } mean
      * two connected segments: one segment goes from vertex 7 to 2, and another
      * segment goes from vertex 2 to 4.  Any 'separate' segment chains must
      * be explicitly separated by included a "-1" in the array.  Thus indices
      * of { 0, 1, -1, 2, 3 } indicate two separate segments: one segment goes
      * from vertex 0 to 1, and another segment goes from 2 to 3.     
      * Note the difference from a 'regular' indexedLineArray!
      * 
      * @param vertices float[] array with 3 coordinate points per vertex
      * @param indices int[] array, this method assumes you want to use ALL
      * the values in the indices array, so be careful!
      * @return An IndexedLineStripArray with specified vertices and indices.
      */
     public Geometry indexedLineStripArray(float[] vertices,
    		 							   int[] indices){
    	 
    	 IndexedLineStripArray indexedLineStripArray = 
    		 new IndexedLineStripArray();
    	 indexedLineStripArray.setVertices(IndexedLineStripArray.COORDINATE_3, 
    		vertices);
    	 indexedLineStripArray.setIndices(indices, indices.length);
    	 
    	 return indexedLineStripArray;
     }
    
      
      //----------------------------------------------------------
      // Methods for creating org.j3d.renderer.aviatrix3d.geom objects
      //----------------------------------------------------------
      
      
    /**
     * Creates an untranslated Box.  Uses the given 
     * appearance to color the Box.
     *
     * @param width The width of the box (x-value)
     * @param height The height of the box (y-value)
     * @param depth The depth of the box (z-value)
     * @param appearance the material apperance of the box
     *
     * @return a Box with the input dimensions and color
     */
    public Box box(float width,
    			   float height,
            	   float depth,
            	   Appearance appearance){

        // create the box of input size and current appearance
        return new Box(width, height, depth, appearance);
    }
    
    
    /**
     * Construct a sphere with all the values customisable.
	 *
     * @param radius - The radius of the base of the sphere
     * @param faces - The number of faces to use around the side
     * @param appearance - The appearance to use
     * @return A sphere
     */
    public Sphere sphere(float radius,
    					 int faces,
    					 Appearance appearance){
    	// create a sphere with the specified values
    	return new Sphere(radius, faces, appearance);
    }
    
    

    
    

     


     
     
     
     
     
     
//     public Appearance appearance(Material m, LineAttributes lineAtts){
//
//    	 // attach the lineAttributes and material to an Appearance
//    	 Appearance appearance = new Appearance();
//    	 appearance.setLineAttributes(lineAtts);
//    	 appearance.setMaterial(m);
//
//    	 return appearance;    	 
//     }
//     
//     
//     /**
//      * This is the 'parent' appearance() method, meaning that 
//      * all other appearance methods call this one.
//      * 
//      * @param diffuse A float array containing the diffuse color
//      * components for the line's appearance.
//      * @param emissive A float array containing the emissive color
//      * components for the line's appearance.
//      * @param lineAtts A LineAttributes ( can have a specific line width
//      * set, and stipple pattern specified with one of the class constants
//      * LineAttributes.PATTERN_* ).
//      * @return An Appearance Node with the designated line attributes and
//      * color. 
//      */
//     public Appearance appearance(float[] diffuse, 
//				 				 float[] emissive, 
//				 				 LineAttributes lineAtts){
//
//    	 Material m = this.material(diffuse, emissive, DEFAULT_TRANSPARENCY);
//    	 
//    	 // set the material for the line
//    	 Material m = new Material();
//    	 m.setDiffuseColor(diffuse);
//    	 m.setEmissiveColor(emissive);
//    	 m.setTransparency(transparency);
//
//    	 // attach the lineAttributes and material to an Appearance
//    	 Appearance appearance = new Appearance();
//    	 appearance.setLineAttributes(lineAtts);
//    	 appearance.setMaterial(m);
//
//    	 return appearance;
//     }
//     
//     
//     /**
//      * Call the appearance(float[] diffuse, float[] emissive,
//      * LineAttributes lineAtts) method by using the current values
//      * pocketed in the diffuseComponents, emissiveComponents, and
//      * lineAttributes method.
//      * @return An Appearance node
//      */
//     public Appearance appearance(){
//    	 return appearance(diffuseComponents, 
//    			 		   emissiveComponents,
//    			 		   lineAttributes);
//     }
//     
//     
//     /**
//      * Call the appearance(float[] diffuse, float[] emissive,
//      * LineAttributes lineAtts) method by using the current Color
//      * values and the specified LineAttributes paramterer. 
//      * 
//      * @param lineAtts A LineAttributes object.  It can have a 
//      * specific line width set, and stipple pattern specified 
//      * with one of the class constants LineAttributes.PATTERN_* ).
//      * @return An Appearance node
//      */
//     public Appearance appearance(LineAttributes lineAtts){
//    	 return appearance(diffuseComponents, 
//    			 		   emissiveComponents,
//    			 		   lineAtts);
//     }
//     
//     
//     /**
//      * Create an Appearance Node with the designated line attributes and
//      * color.  Note that the lineAttributes could be null if desired.
//      * @param diffuse diffuse Color
//      * @param emissive emissive Color 
//      * @param lineAtts A LineAttributes object.  It can have a 
//      * specific line width set, and stipple pattern specified 
//      * with one of the class constants LineAttributes.PATTERN_* ).
//      * @return An Appearance node
//      */
//     public Appearance appearance(Color diffuse, 
//    		 					  Color emissive, 
//    		 					  LineAttributes lineAtts){
//
//    	 return appearance(diffuse.getComponents(new float[6]),
//	    			 	  emissive.getComponents(new float[6]),
//	    			 	  lineAtts);
//     }
     
    
    
    //----------------------------------------------------------
    // Methods for creating Shape3D objects
    //----------------------------------------------------------

    
    /**
     * The 'simplest' way to create a Shape3D object, since it has the
     * fewest parameters.  Uses the AVGeomtryBuilder's currentAppearance.
     * 
     * @param geometry Geometry node to use for the Shape3D's geometry
     * @return a Shape3D object with the AVGeomtryBuilder's currentAppearance.
     */
    public Shape3D create(Geometry geometry){

   	 // create the Shape3D with the input geometry and current appearance
        Shape3D s = new Shape3D();
        s.setGeometry(geometry);
        s.setAppearance(currentAppearance);
        s.setPickMask(Shape3D.SINGLE_PICK_TYPE);

        return s; 
    }
    
    /**
     * Create a Shape3D object with the given geometry and appearance
     * @param geometry Geometry node to use for the Shape3D's geometry
     * @param appearance Appearance node to use for the Shape3D's geometry
     * @return Shape3D object with the given geometry and appearance
     */
    public Shape3D create(Geometry geometry, Appearance appearance){

   	 // create the Shape3D with the appropriate geometry and appearance
        Shape3D s = new Shape3D();
        s.setGeometry(geometry);
        s.setAppearance(appearance);
        s.setPickMask(Shape3D.SINGLE_PICK_TYPE);

        return s; 
    }
    
     
    //----------------------------------------------------------
    // Methods for creating Shape3D text objects
    //----------------------------------------------------------

     
  	/**
  	 * Create a text box containing the text specified, in the font and color specified, on a 
  	 * background of the default editor background, using fraction metrics if specified.
  	 * 
  	 * @param text - Text to be rendered
  	 * @param font - The style of text to be rendered
  	 * @param fontColor - The color of the text
  	 * 
  	 * @return textShape - The Aviatrix3D Shape3D containing the text box
  	 */
  	public Shape3D generateTextShape(String text, Font font, Color fontColor) {
  		return generateTextShape(text, font, fontColor, Color.black);
  	}

  	/**
  	 * Create a text box with custom colors in float array form.  Only RGB components will be
  	 * used.
  	 * 
  	 * @param text - Text to be rendered
  	 * @param font - The style of text to be renderd
  	 * @param fontColor - RGB components for the font color
  	 * @param backgroundColor - RGB components for the background color
  	 * 
  	 * @return textShape - The Aviatrix3D Shape3D containing the text box
  	 */
  	public Shape3D generateTextShape(String text, Font font,
  			float[] fontColor, float[] backgroundColor) {
  		
  		// Create java.awt colors from the components given, and generate away.
  		return generateTextShape(text, font, new Color(fontColor[0],
  				fontColor[1], fontColor[2]), new Color(backgroundColor[0],
  				backgroundColor[1], backgroundColor[2]));
  	}
  	
  	
  	/** 
  	 * @param text
  	 * @param font
  	 * @return The geometry
  	 */
  	public Geometry generateTextGeometry(String text, 
  										 Font font) {

  		// Grab the bounds for the text, to know what size should be
  		Rectangle2D bounds = font.getStringBounds(text, frc);

  		// Grab width and height
  		float width = (float)bounds.getWidth();
  		float height = (float)bounds.getHeight();

  		// Get the lowest multiple of 2 for width and height so the texture comes out right
  		int happyWidth = Integer.highestOneBit((int)width) << 1;
  		int happyHeight = Integer.highestOneBit((int)height) << 1;
  		
  		// Convert everything into aviatrix semantics
  		// Scale the texture coords to account for the resizing to powers of 2
  		float textX = width / (float) happyWidth;
  		float textY = height / (float) happyHeight;

  		float[] textCoords = new float[] { textX, 	0, 
  										   textX, 	textY,
  										   0, 		textY, 
  										   0, 		0 };
  		
  		float[] vertices = new float[] { width, -height, 	0,
								 		 width, 0, 			0,
								 		 0, 	0, 			0,  
								 		 0, 	-height, 	0 };

		float[] normals = new float[] { 0, 0, 1, 
										0, 0, 1, 
										0, 0, 1, 
										0, 0, 1};

  		// Make the shape, and teture it.
  		QuadArray textQuad = new QuadArray();
  		textQuad.setVertices(QuadArray.COORDINATE_3, vertices,
  				vertices.length / 3);
  		textQuad.setNormals(normals);
  		textQuad.setTextureCoordinates(
  				new int[] { VertexGeometry.TEXTURE_COORDINATE_2 },
  				new float[][] { textCoords });

  		return textQuad;
  	}
  	
  	/**
  	 * @param text
  	 * @param font
  	 * @param fontColor
  	 * @param backgroundColor
  	 * @return The appearance
  	 */
  	public Appearance generateTextAppearance(String text, Font font,
  			Color fontColor, Color backgroundColor) {

  		// Grab the bounds for the text, to know what size should be
  		Rectangle2D bounds = font.getStringBounds(text, frc);

  		// Grab width and height
  		float width = (float)bounds.getWidth();
  		float height = (float)bounds.getHeight();

  		// Get the lowest multiple of 2 for width and height so the texture comes out right
  		int happyWidth = Integer.highestOneBit((int)width) << 1;
  		int happyHeight = Integer.highestOneBit((int)height) << 1;
  		
  		// Create the image that will be the image texture
  		BufferedImage buff = new BufferedImage(happyWidth, happyHeight,
  				BufferedImage.TYPE_INT_ARGB);
  		
  		// Create a graphics instance, and set colors, and then draw the text
  		Graphics2D g = buff.createGraphics();
  		//g.setBackground(backgroundColor);
		//g.setColor(backgroundColor);
		//g.fillRect(0, 0, happyWidth, happyHeight);
  		g.setColor(fontColor);
  		GlyphVector gv = font.createGlyphVector(frc, text);
  		
  		// The 0.9 seems to be a good value for relative location within the texture.
  		// It is unclear why the unscaled height doesn't look right
  		//g.drawGlyphVector(gv, 0, happyHeight * 0.9f);
		float h_scaled = ((float)happyHeight) - (((float)happyHeight) - height) * 0.5f;
		g.drawGlyphVector(gv, 0, h_scaled);


  		// 
  		// create and set the source of a Texture2D object
  		//
  		Texture2D textTex = new Texture2D();
  		ImageTextureComponent2D textComp = 
  			new ImageTextureComponent2D(TextureComponent.FORMAT_RGBA,
  										happyWidth, 
  										happyHeight, 
  										buff);
  		textTex.setSources(Texture.MODE_BASE_LEVEL, 
  						   Texture.FORMAT_RGBA,
  						   new TextureComponent[] { textComp }, 
  						   1);
  		
  		//
  		// build a length-one array of texture units and
  		// set the only texture unit to equal a Texture2D, then use it to
  		// set the appearance's texture units
  		//
  		TextureUnit[] textUnit = new TextureUnit[1];
  		textUnit[0] = new TextureUnit();
  		textUnit[0].setTexture(textTex);
  		Appearance textApp = new Appearance();
  		textApp.setTextureUnits(textUnit, 1);

  		// set the material of the appearance
  		Material textMat = new Material();
  		//textMat.setEmissiveColor(new float[] { 1f, 0f, 0f });
  		textApp.setMaterial(textMat);

  		return textApp;
  	}
  	
  	/**
  	 * Create a text box with custom colors, font, text and a choice on fractional metrics.  It 
  	 * seems that fractional metrics should be used if the font size is about 10 or smaller, and 
  	 * otherwise fractional metrics should not be used.  However, it is usually best to try both 
  	 * and see which one comes out better.
  	 * 
  	 * @param text - Text to be rendered
  	 * @param font - The style of text to be rendered
  	 * @param fontColor - The color of the text
  	 * @param backgroundColor - The color of the background around the text
  	 * 
  	 * @return textShape - The Aviatrix3D Shape3D containing the text box
  	 */
  	public Shape3D generateTextShape(String text, Font font,
  			Color fontColor, Color backgroundColor) {
/*
  		// Determines if fractional metrics should be used or not
  		FontRenderContext frc = new FontRenderContext(null, true, fractionalMetrics);

  		// Grab the bounds for the text, to know what size should be
  		Rectangle2D bounds = font.getStringBounds(text, frc);

  		// Grab width and height
  		float width = (float)bounds.getWidth();
  		float height = (float)bounds.getHeight();

  		// Get the lowest multiple of 2 for width and height so the texture comes out right
  		int happyWidth = Integer.highestOneBit((int)width) << 1;
  		int happyHeight = Integer.highestOneBit((int)height) << 1;
  		
  		// Create the image that will be the image texture
  		BufferedImage buff = new BufferedImage(happyWidth, happyHeight,
  				BufferedImage.TYPE_INT_ARGB);
  		
  		// Create a graphics instance, and set colors, and then draw the text
  		Graphics2D g = buff.createGraphics();
  		g.setBackground(backgroundColor);
  		g.setColor(fontColor);
  		GlyphVector gv = font.createGlyphVector(frc, text);
  		
  		// The 0.9 seems to be a good value for relative location within the texture.
  		// It is unclear why the unscaled height doesn't look right
  		g.drawGlyphVector(gv, 0, happyHeight * 0.9f);

  		// Convert everything into aviatrix semantics
  		// Scale the texture coords to account for the resizing to powers of 2
  		float textX = width / (float) happyWidth;
  		float textY = height / (float) happyHeight;

  		float[] textCoords = new float[] { textX, 	0, 
  										   textX, 	textY,
  										   0, 		textY, 
  										   0, 		0 };
  		float[] vertices = new float[12];
  		float[] normals = new float[12];
  		
		vertices = new float[] { width, -height, 	0,
								 width, 0, 			0,
								 0, 	0, 			0,  
								 0, 	-height, 	0 };

		normals = new float[] { 0, 0, 1, 
								0, 0, 1, 
								0, 0, 1, 
								0, 0, 1};


  		// Make the shape, and teture it.
  		QuadArray textQuad = new QuadArray();
  		textQuad.setVertices(QuadArray.COORDINATE_3, vertices,
  				vertices.length / 3);
  		textQuad.setNormals(normals);
  		textQuad.setTextureCoordinates(
  				new int[] { VertexGeometry.TEXTURE_COORDINATE_2 },
  				new float[][] { textCoords });

  		Appearance textApp = new Appearance();
  		ImageTextureComponent2D textComp = new ImageTextureComponent2D(
  				TextureComponent.FORMAT_RGBA, happyWidth, happyHeight, buff);

  		Texture2D textTex = new Texture2D();

  		textTex.setSources(Texture.MODE_BASE_LEVEL, Texture.FORMAT_RGBA,
  				new TextureComponent[] { textComp }, 1);

  		TextureUnit[] textUnit = new TextureUnit[1];
  		textUnit[0] = new TextureUnit();
  		textUnit[0].setTexture(textTex);
  		textApp.setTextureUnits(textUnit, 1);

  		Material textMat = new Material();
  		textMat.setEmissiveColor(new float[] { 1f, 0f, 0f });
  		textApp.setMaterial(textMat);

  		// Set the geometry, and a gray material to see if the texture isn't showing
  		Shape3D textShape = new Shape3D();
  		textShape.setGeometry(textQuad);
  		textShape.setAppearance(textApp);
*/
  		//////////////////////////////////////////////////////
  		// 
  		// EMF: Making this change not because it's faster, but to separate
  		// the appearance-creation from the geometry creation.  This separation
  		// allows us to update text with a Text.setAppearance() and Text.setGeometry()
  		// call, rather than using Text's parent to do a Parent.setChild( new Text())
  		// call.  Requested by Justin; in theory it means less memory churn.
  		//
  		/////////////////////////////////////////////////////
  		
  		Shape3D textShape = new Shape3D();
  		textShape.setGeometry( generateTextGeometry(text, font));
  		textShape.setAppearance(generateTextAppearance(
  				text, font, fontColor, backgroundColor));
  		return textShape;
  	}

}
