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

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.ImageTextureComponent2D;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.QuadArray;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.Texture;
import org.j3d.aviatrix3d.Texture2D;
import org.j3d.aviatrix3d.TextureComponent;
import org.j3d.aviatrix3d.TextureUnit;
import org.j3d.aviatrix3d.VertexGeometry;

// internal imports
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Class for producing labels for overlays.
 *
 * @author Rex Melton
 * @version $Revision: 1.5 $
 */
class LegendText implements NodeUpdateListener {
	
	/** 
	 * Applies fractional metrics to the FontRenderingContext object. <br> 
	 * Eric Fickenscher says thusly: <br>
	 * If true, split pixels; if false, do not split pixels. <br>
	 * It seems that fractional metrics should be used if the font 
	 * size is about 10 or smaller, and otherwise fractional metrics 
	 * should not be used.  However, it is usually best to try both 
	 * and see which one comes out better. 
	 */
	private static final boolean useFractionalMetrics = true;
	
	/** The default offset */
	private static final float[] DEFAULT_OFFSET = new float[]{0, 0, 0};
	
	/** Identifier of the anchor point of the geometry */
	enum Anchor {
		BOT_LEFT,
		BOT_CENTER,
		BOT_RIGHT,
		CENTER_RIGHT,
		TOP_RIGHT,
		TOP_CENTER,
		TOP_LEFT,
		CENTER_LEFT,
		CENTER};
	
	/** The font's rendering context */
	private FontRenderContext frc;
	
	/** The font to render text with */
	private Font font;
	
	/** The text color */
	private Color textColor;
	
	/** The background color */
	private Color backgroundColor;
	
	/** The scene manager Observer*/
	private SceneManagerObserver mgmtObserver;
	
	/** Rendering components */
	private QuadArray textQuad;
	private Texture2D textTex;
	private ImageTextureComponent2D textComp;
	private float[] texCoords;
	private float[] vertices;
		
	/** The shape */
	private Shape3D textShape;
	
	/** Enable filters */
	private boolean filter_enable;
	/**
	 * Default constructor.
	 */
	LegendText(SceneManagerObserver mgmtObserver) {
		
		this(mgmtObserver, true);
	}
	
	/**
	 * Default constructor.
	 */
	LegendText(SceneManagerObserver mgmtObserver, boolean filter) {
		
		this.mgmtObserver = mgmtObserver;
		filter_enable = filter;
		
		frc = new FontRenderContext(null, true, useFractionalMetrics);
		font = FontColorUtils.getXLargeFont();
		textColor = Color.black;
		
		init();
	}
	
	//----------------------------------------------------------
	// Methods defined by NodeUpdateListener
	//----------------------------------------------------------
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that could potentially effect the node's bounds.
	 *
	 * @param src The Node that is to be updated.
	 */
	public void updateNodeBoundsChanges(Object src) {
		
		if (src == textQuad) {
			textQuad.setVertices(QuadArray.COORDINATE_3, vertices, 4);
		}
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The Node that is to be updated.
	 */
	public void updateNodeDataChanges(Object src) {
		
		if (src == textQuad) {
			
			textQuad.setTextureCoordinates(
				new int[] { VertexGeometry.TEXTURE_COORDINATE_2 },
				new float[][] { texCoords });
			
		} else if (src == textTex) {
			
			textTex.setSources(
				Texture.MODE_BASE_LEVEL, 
				Texture.FORMAT_RGBA,
				new TextureComponent[] { textComp }, 
				1);
			
			if (backgroundColor != null) {
				textTex.setBoundaryModeS(Texture.BM_CLAMP_TO_EDGE);
				textTex.setBoundaryModeT(Texture.BM_CLAMP_TO_EDGE);
			}
		}
	}
	
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/**
	 * Set the font. A null value will revert the font to it's default.
	 *
	 * @param font The new font.
	 */
	void setFont(Font font) {
		if (font == null) {
			this.font = new Font("serif", Font.PLAIN, 16);
		} else {
			this.font = font;
		}
	}
	/**
	 * Set the text color. A null value will revert the 
	 * text to black.
	 *
	 * @param color The Color of the text
	 */
	void setTextColor(Color color) {
		if (color != null) {
			textColor = color;
		} else {
			textColor = Color.black;
		}
	}
	
	/**
	 * Set the background color. A null value will render the
	 * background invisible.
	 *
	 * @param color The Color of the text
	 */
	void setBackgroundColor(Color color) {
		backgroundColor = color;
	}
	
	/**
	 * Return the Shape object for this
	 *
	 * @return The Shape object for this
	 */
	Shape3D getShape() {
		return(textShape);
	}
	
	/**
	 * Return a Shape object for the specified string
	 *
	 * @param text The String to render
	 * @param geom_height The height for the geometry. If 0 (zero) or
	 * less, the size of the geometry is determined by the bounds of
	 * the string as render from the font.
	 * @param anchor The anchor point of the geometry
	 * @param offset The translation to be applied to the geometry. If
	 * null, no additional translation is applied.
	 */
	void update(String text, float geom_height, Anchor anchor, float[] offset) {
		
		// Grab the bounds for the text, to know what size should be
		Rectangle2D bounds = font.getStringBounds(text, frc);
		
		// Grab width and height
		float width = (float)bounds.getWidth();
		float height = (float)bounds.getHeight();
		
		// Get the lowest multiple of 2 for width and height so the texture comes out right
		int p2width = Integer.highestOneBit((int)width) << 1;
		int p2height = Integer.highestOneBit((int)height) << 1;
		
		float geom_width;
		if (geom_height > 0) {
			float scale = geom_height / height;
			geom_width = scale * width;
		} else {
			geom_height = height;
			geom_width = width;
		}
		
		// Create the image that will be the image texture
		BufferedImage buff = new BufferedImage(
			p2width, 
			p2height,
			BufferedImage.TYPE_INT_ARGB);
		
		// Create a graphics instance, and set colors, and then draw the text
		Graphics2D g = buff.createGraphics();
		if (backgroundColor != null) {
			g.setColor(backgroundColor);
			g.fillRect(0, 0, p2width, p2height);
		}
		g.setColor(textColor);
		GlyphVector gv = font.createGlyphVector(frc, text);
		
		// The 0.9 seems to be a good value for relative location within the texture.
		// It is unclear why the unscaled height doesn't look right
		g.drawGlyphVector(gv, 0, p2height * 0.9f);
		
		// create and set the source of a Texture2D object
		textComp = new ImageTextureComponent2D(
			TextureComponent.FORMAT_RGBA,
			p2width, 
			p2height, 
			buff);
		
		mgmtObserver.requestDataUpdate(textTex, this);
		
		float textX = width / (float) p2width;
		float textY = height / (float) p2height;
		
		texCoords[0] = 0;
		texCoords[1] = 0;
		texCoords[2] = textX;
		texCoords[3] = 0;
		texCoords[4] = textX;
		texCoords[5] = textY;
		texCoords[6] = 0;
		texCoords[7] = textY; 
		
		mgmtObserver.requestDataUpdate(textQuad, this);
		
		if (offset != null) {
			setVertices(geom_width, geom_height, 0, anchor, offset, vertices);
		} else {
			setVertices(geom_width, geom_height, 0, anchor, DEFAULT_OFFSET, vertices);
		}
		mgmtObserver.requestBoundsUpdate(textQuad, this);
	}
	
	/**
	 * Configure the vertices of the quad. The vertices are always ordered 
	 * counterclockwise starting at the bottom left corner, as follows:
	 * <pre>
	 *
	 *   3    2
	 *   o----o
	 *   |    |
	 *   o----o
	 *   0    1
	 *
	 * </pre>
	 *
	 * @param height The quad height
	 * @param width The quad width
	 * @param depth The quad depth
	 * @param anchor The position on the quad that is the local origin (x=0, y=0)
	 * @param offset The translation offset to be applied to the anchor position
	 * @param vtx The array to initialize
	 */
	private void setVertices(
		float width, 
		float height, 
		float depth, 
		Anchor anchor,
		float[] offset, 
		float[] vtx) {
		
		float top = 0;
		float bot = 0;
		float rght = 0;
		float left = 0;
		switch(anchor) {
		case BOT_LEFT:
			top = height;
			bot = 0;
			rght = width;
			left = 0;
			break;
		case BOT_CENTER:
			top = height;
			bot = 0;
			rght = width * 0.5f;
			left = -rght;
			break;
		case BOT_RIGHT:
			top = height;
			bot = 0;
			rght = 0;
			left = -width;
			break;
		case CENTER_RIGHT:
			top = height * 0.5f;
			bot = -top;
			rght = 0;
			left = -width;
			break;
		case TOP_RIGHT:
			top = 0;
			bot = -height;
			rght = 0;
			left = -width;
			break;
		case TOP_CENTER:
			top = 0;
			bot = -height;
			rght = width * 0.5f;
			left = -rght;
			break;
		case TOP_LEFT:
			top = 0;
			bot = -height;
			rght = width;
			left = 0;
			break;
		case CENTER_LEFT:
			top = height * 0.5f;
			bot = -top;
			rght = width;
			left = 0;
			break;
		case CENTER:
			top = height * 0.5f;
			bot = -top;
			rght = width * 0.5f;
			left = -rght;
			break;
		}
		vtx[0] = left + offset[0];
		vtx[1] = bot + offset[1];
		vtx[2] = depth + offset[2];
		vtx[3] = rght + offset[0];
		vtx[4] = bot + offset[1];
		vtx[5] = depth + offset[2];
		vtx[6] = rght + offset[0];
		vtx[7] = top + offset[1];
		vtx[8] = depth + offset[2];
		vtx[9] = left + offset[0];
		vtx[10] = top + offset[1];
		vtx[11] = depth + offset[2];
	}
	
	/**
	 * Initialize the rendering components
	 */
	private void init() {
		
		textTex = new Texture2D();
		if (filter_enable) {
			textTex.setMinFilter(Texture.MINFILTER_NICEST);
			textTex.setMagFilter(Texture.MAGFILTER_NICEST);
		}
		
		TextureUnit[] textUnit = new TextureUnit[1];
		textUnit[0] = new TextureUnit();
		textUnit[0].setTexture(textTex);
		Appearance textApp = new Appearance();
		textApp.setTextureUnits(textUnit, 1);
		
		Material textMat = new Material();
		textApp.setMaterial(textMat);
		
		///////////////////////////////////////////////////////////////////////
		 
		vertices = new float[12];
		texCoords = new float[8];
		float[] normals = new float[] { 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1};
		
		textQuad = new QuadArray();
		textQuad.setVertices(QuadArray.COORDINATE_3, vertices, 4);
		textQuad.setNormals(normals);
		textQuad.setTextureCoordinates(
			new int[] { VertexGeometry.TEXTURE_COORDINATE_2 },
			new float[][] { texCoords });
		
		///////////////////////////////////////////////////////////////////////
		
		textShape = new Shape3D();
		textShape.setGeometry(textQuad);
		textShape.setAppearance(textApp);
	}
}
