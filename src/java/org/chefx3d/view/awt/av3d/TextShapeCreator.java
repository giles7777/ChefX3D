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

// External imports
import java.awt.Color;
import java.awt.Font;

import java.util.ArrayList;

import org.j3d.aviatrix3d.IndexedTriangleArray;
import org.j3d.aviatrix3d.Shape3D;

import org.j3d.geom.CharacterCreator;
import org.j3d.geom.CharacterData;

import org.j3d.util.TriangleUtils;

// Local imports
// none

/**
 * A class that generates geometry for text strings. Currently this 
 * produces only a single character.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class TextShapeCreator {
	
	/** Index of the tangent attribute */
	private static final int TANGENT_ATTRIB_IDX = 5;
	
	/** The character of last resort */
	private static final char DEFAULT_CHAR = '-';
	
	/** The font to use */
	private Font font;
	
	/** Character generation utility */
	private CharacterCreator creator;
	
	/** Created geometry */
	private ArrayList geom_data = new ArrayList();
	
	/** Array to hold the color components */
	private float[] color_array;
	
	/**
	 * Constructor
	 *
	 * @param font The font to create characters with
	 */
	TextShapeCreator(Font font) {
		this.font = font;
		creator = new CharacterCreator(font, 0.01);
		geom_data = new ArrayList();
	}
	
	/**
	 * Return the shape object containing a representation of the text.
	 * Currently this only generates geometry for the first character in
	 * the string and only supports capital letters from A-Z.
	 *
	 * @param text The text to generate a shape for
	 * @param color The color to apply to the geometry
	 * @return The shape object
	 */
	Shape3D getShape(String text, Color color) {
		
		char c = text.charAt(0);
		
		if ((c < 'A') || (c > 'Z')) {
			c = DEFAULT_CHAR;
		}
		
		Shape3D shape = null;
		geom_data.clear();
		char[] text_char = new char[]{c};
		int num_char = 1;
		try {
			creator.createCharacterTriangles(text_char, num_char, geom_data);
			CharacterData char_data = (CharacterData)geom_data.get(0);
			IndexedTriangleArray ita = getITA(char_data);
			if (ita != null) {
				
				color_array = color.getComponents(color_array);
				ita.setSingleColor(false, color_array);
				
				shape = new Shape3D();
				shape.setGeometry(ita);
			}
		} catch (Exception e) {
		}
		if (shape == null) {
			shape = getShape(String.valueOf(DEFAULT_CHAR), color);
		}
		return(shape);
	}
	
	/**
	 * Create and return the geometry for the character in the argument.
	 *
	 * @param char_data The container of the generated character data
	 * @return An IndexedTriangleArray representing the character, or
	 * null if one could not be produced.
	 */
	private IndexedTriangleArray getITA(CharacterData char_data) {
		
		char_data.coordinates.rewind();
		int num_coord = char_data.coordinates.limit();
		float[] coord = new float[num_coord];
		char_data.coordinates.get(coord);
		transform(coord);
		
		char_data.coordIndex.rewind();
		int num_index = char_data.coordIndex.limit();
		int[] index = new int[num_index];
		char_data.coordIndex.get(index);
		
		IndexedTriangleArray ita = null;
		if ((num_coord > 0) && (num_index >0)) {
			float[] normal = new float[num_coord];
			int num_vertex = num_coord / 3;
			for (int i = 0; i < num_vertex; i++) {
				normal[i * 3 + 2] = 1;
			}
			
			float[] tangents = new float[num_vertex * 4];
			float[] texCoord = new float[num_vertex * 2];
			
			TriangleUtils.createTangents(
				num_index / 3,
				index,
				coord,
				normal,
				texCoord,
				tangents);
			
			ita = new IndexedTriangleArray();
			
			ita.setVertices(
				IndexedTriangleArray.COORDINATE_3,
				coord,
				num_vertex);
			
			ita.setIndices(index, num_index);
			ita.setNormals(normal);
			ita.setAttributes(TANGENT_ATTRIB_IDX, 4, tangents, false);
		}
		return(ita);
	}
	
	/**
	 * Transform the geometry coordinates such that the text is a
	 * uniform height of 1.0 above the baseline, with the left
	 * most x coordinate at 0.
	 *
	 * @param coord The coord array to process
	 */
	private void transform(float[] coord) {
		
		float[] bounds = getBounds(coord);
		
		float x_min = bounds[0];
		float y_max = bounds[3];
		float scale = 1.0f / y_max;
		
		int num_vertex = coord.length / 3;
		
		for (int n = 0; n < num_vertex; n++) {
			int i = n * 3;
			coord[i] -= x_min;
			coord[i] *= scale;
			coord[i+1] *= scale;
		}
	}
	
	/**
	 * Return the bounds of the coordinate array.
	 * 
	 * @param coord The coord array to process
	 */
	private float[] getBounds(float[] coord){
		
		float x_min = coord[0];
		float x_max = coord[0];
		float y_min = coord[1];
		float y_max = coord[1];
		float z_min = coord[2];
		float z_max = coord[2];
		
		int num_vertex = coord.length / 3;
		int idx = 0;
		float value;
		for (int i = 1; i < num_vertex; i++) {
			
			idx = i * 3;
			
			value = coord[idx];
			if (value < x_min) {
				x_min = value;
			} else if (value > x_max) {
				x_max = value;
			}
			idx++;
			
			value = coord[idx];
			if (value < y_min) {
				y_min = value;
			} else if (value > y_max) {
				y_max = value;
			}
			idx++;
			
			value = coord[idx];
			if (value < z_min) {
				z_min = value;
			} else if (value > z_max) {
				z_max = value;
			}
			idx++;
		}
		float[] bound = new float[]{x_min, x_max, y_min, y_max, z_min, z_max};
		return(bound);
	}
}
