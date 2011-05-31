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
import org.j3d.geom.GeometryData;
import org.j3d.geom.TriangulationUtils;

// Local imports
// none

/**
 * Utilities for handling generic geometry actions.
 *
 * @author Alan Hudson, Rex Melton
 * @version $Revision: 1.2 $
 */
class GeometryUtils {
	
	/** local switch constants */
	private static final int FIELD_COLORINDEX = 0;
	private static final int FIELD_NORMALINDEX = 1;
	private static final int FIELD_TEXCOORDINDEX = 2;
	
	/** The produced object */
	GeometryData geomData;
	
	// Cached values, clear on reset
	private int[] lfColorIndex;
	private int[] lfCoordIndex;
	private int[] lfNormalIndex;
	private int[] lfTexCoordIndex;
	private int[] tsCoordIndex;
	private int[] tsNormalIndex;
	private int[] tsColorIndex;
	private int[] tsTexCoordIndex;
	private int[] triangleOutput;
	private int[] normalOutput;
	private int[] colorOutput;
	private int[] texCoordOutput;
	private int[][] vertexToFace;
	private int[] rawVerticesPerFace;
	
	// Local variables to be overwritten each time
	private float[] lfColor;
	private float[] lfCoord;
	private float[] lfNormal;
	private float[] lfTexCoord;
	private float[][] texCoords;
	private float[][] faceNormals;
	private int[] vfColorIndex;
	private int[] vfCoordIndex;
	private int numCoordIndex;
	private int[] vfNormalIndex;
	private int numNormalIndex;
	private int[] vfTexCoordIndex;
	private float[] min;
	private float[] max;
	private boolean vfCcw;
	private boolean vfConvex;
	private boolean vfColorPerVertex;
	private boolean vfNormalPerVertex;
	private float vfCreaseAngle;
	private int maxIndexValue;
	private int polygonCount;
	private int triangleCount;
	private int maxPolySize;
	private int maxIndexCount;
	private int triCnt;
	private int quadCnt;
	private int ngonCnt;
	private int numColorComponents;
	private int numTextureDimensions;
	private float[] normalTmp;
	private TriangulationUtils triangulator;
	private double cosCreaseAngle;
	
	/**
	 * Default Constructor
	 */
	GeometryUtils() {
		geomData = new GeometryData();
	}
	
	/**
	 * Take an indexed geometry and flatten to a triangle array.
	 *
	 * @param genTexCoords Should texture coordinates be generated if none are provided
	 * @param genNormals Should normals be generated if none are provided
	 * @param vfCoord The coordinate data
	 * @param vfColor The color data, if provided, NULL otherwise
	 * @param numColorComponents The number of color components
	 * @param vfNormal The normal data, if provided, NULL otherwise
	 * @param vfTexCoord The texture data, if provided, NULL otherwise
	 * @param vfCoordIndex The coordinate indices seperated by -1
	 * @param numCoordIndex The number of valid coordinate indices
	 * @param vfColorIndex The color indices.  NULL means use the coord indices
	 * @param vfNormalIndex The normal indices.  NULL means use the coord indices
	 * @param vfTexCoordIndex The texture indices.  NULL means use the coord indices
	 * @param ccw CounterClockWise, true if the data is ccw wound
	 * @param convex true if the data has a convex hull
	 * @param colorPerVertex true if the data has colors per vertex, false for face
	 * @param normalPerVertex true if the data has normals per vertex, false for face
	 * @param creaseAngle Angle to smooth normals if generating normals
	 *
	 * @return true if the created GeometryData object might have something renderable
	 * in it, false otherwise.
	 */
	boolean generateTriangleArrays(
		boolean genTexCoords,
		boolean genNormals, 
		float[] vfCoord, 
		float[] vfColor,
		int numColorComponents,
		float[] vfNormal, 
		float[] vfTexCoord,
		int[] vfCoordIndex,
		int numCoordIndex, 
		int[] vfColorIndex, 
		int[] vfNormalIndex,
		int[] vfTexCoordIndex, 
		boolean ccw, 
		boolean convex,
		boolean colorPerVertex, 
		boolean normalPerVertex, 
		float creaseAngle) {
		
		this.vfCoordIndex = vfCoordIndex;
		this.numCoordIndex = numCoordIndex;
		this.vfNormalIndex = vfNormalIndex;
		this.vfColorIndex = vfColorIndex;
		this.vfTexCoordIndex = vfTexCoordIndex;
		this.numColorComponents = numColorComponents;
		vfCcw = ccw;
		vfConvex = convex;
		vfColorPerVertex = colorPerVertex;
		vfNormalPerVertex = normalPerVertex;
		vfCreaseAngle = creaseAngle;
		
		if (creaseAngle > Math.PI) {
			creaseAngle = (float)Math.PI;
		}
		
		cosCreaseAngle = Math.cos(creaseAngle);
		
		if (!vfConvex) {
			normalTmp = new float[3];
			triangulator = new TriangulationUtils();
		}
		
		geomData.geometryType = GeometryData.TRIANGLES;
		
		if ((vfCoordIndex == null) || (vfCoord == null)) {
			// no coords, no indices, no point....
			return(false);
		}
		
		// Start by fetching the raw info from the component nodes
		int num = vfCoord.length;
		if (num < 3) {
			return(false);
		}
			
		geomData.vertexCount = num / 3;
		
		lfCoord = new float[num];
		System.arraycopy(vfCoord, 0, lfCoord, 0, num);
		
		if (vfColor != null) {
			num = vfColor.length;
			
			lfColor = new float[num];
			System.arraycopy(vfColor, 0, lfColor, 0, num);
		}
		
		if (vfNormal != null) {
			num = vfNormal.length;
			
			lfNormal = new float[num];
			System.arraycopy(vfNormal, 0, lfNormal, 0, num);
		}
		
		// Now build up the index lists. This ensures that all the local copies
		// of index lists now represent exactly the same data in terms of being
		// 1:1 for each vertex value.
		//
		// Rebuild all of them if the coordIndex list changes, otherwise just
		// rebuild the one(s) that changed. max_index will be re-assigned a
		// different value if the coordIndex list has changed, otherwise, don't
		// it should result in the geometry not being reallocated.
		
		int max_index = maxIndexCount;
		
		buildIndexList(FIELD_COLORINDEX);
		buildIndexList(FIELD_TEXCOORDINDEX);
		buildIndexList(FIELD_NORMALINDEX);
		
		// Re-create the geometry if we need to. If not, we work on using the
		// max polygon size and only re-allocate when things get bigger than
		// what we've currently allocated. Do a rough upper-bound guestimation
		// based on the maximum polygon size and that every polygon in the
		// array has that many polygons
		int max_poly_size = checkMaxPolySize();
		
		maxPolySize = max_poly_size;
		
		switch(max_poly_size) {
		case 0:
			max_index = 0;
			//System.out.println("zero sized polygons!");
			return(false);
			
		case 1:
		case 2:
			System.out.println("No valid polygons. Max size " +
				max_poly_size);
			return(false);
			
		case 3:
			max_index = polygonCount * 3;
			break;
			
		case 4:
			max_index = polygonCount * 6;
			break;
			
		default:
			// based on Siedel's algorithm
			max_index = triCnt * 3 + quadCnt * 6 + ngonCnt * (max_poly_size - 2) * 3;
			break;
		}
		
		if (!vfConvex && ((triangleOutput == null) ||
			(triangleOutput.length < maxPolySize * 3))) {
			
			triangleOutput = new int[maxPolySize * 3];
			normalOutput = new int[maxPolySize * 3];
			colorOutput = new int[maxPolySize * 3];
			texCoordOutput = new int[maxPolySize * 3];
		}
		
		int max_coords = lfCoord.length;
		maxIndexValue = max_coords > max_index ? max_coords : max_index;
		
		// resize the index arrays
		
		tsCoordIndex = new int[maxIndexValue];
		tsNormalIndex = new int[maxIndexValue];
		tsTexCoordIndex = new int[maxIndexValue];
		
		if (vfColor != null) {
			tsColorIndex = new int[maxIndexValue];
		}
		
		rebuildFaceLists(true, (vfColor != null));
		
		if (max_index > maxIndexCount) {
			// The index array size has grown, notify downstream users
			maxIndexCount = max_index;
		}
		
		updateCoordinateArray();
		
		if (vfNormal == null) {
			if (genNormals) {
				generateNormals();
			}
		} else {
			updateNormalArray();
		}
		
		if (vfTexCoord == null) {
			if (genTexCoords) {
				generateTextureCoordinates();
			}
		} else {
			int numTexSets = 1;
			int numRealSets = numTexSets;
			
			// first check for max required size
			int max_set_size = vfTexCoord.length;
			int num_tex_comp = 2;
			
			//max_set_size = geomData.vertexCount * num_tex_comp;
			
			lfTexCoord = new float[max_set_size];
			
			//geomData.textureCoordinates = new float[geomData.vertexCount * num_tex_comp];
			
			System.arraycopy(vfTexCoord, 0, lfTexCoord, 0, vfTexCoord.length);
			numTextureDimensions = 2;
			updateTexCoordinateArray(0, numTexSets);
		}

		if (vfColor != null) {
			updateColorArray();
		}
		
		return(true);
	}
	
	/**
	 * Get the counts of triangle, quad, ngons and maxIndexCount.
	 *
	 * @param counts An to fill in the counts
	 */
	public void getCounts(int[] counts) {
		counts[0] = triCnt;
		counts[1] = quadCnt;
		counts[2] = ngonCnt;
		counts[3] = maxIndexCount;
	}
	
	/**
	 * Build the index list based on the logic defined in the spec.
	 */
	private void buildIndexList(int fieldIndex) {
		int[] src_list;
		int[] final_list;
		boolean per_vertex;
		
		switch(fieldIndex) {
		case FIELD_NORMALINDEX:
			src_list = vfNormalIndex;
			per_vertex = vfNormalPerVertex;
			break;
			
		case FIELD_COLORINDEX:
			src_list = vfColorIndex;
			per_vertex = vfColorPerVertex;
			break;
			
		case FIELD_TEXCOORDINDEX:
			src_list = vfTexCoordIndex;
			per_vertex = true;
			break;
			
		default:
			throw new IllegalArgumentException("Dud index field");
		}
		
		// Construct a per-vertex list for internal use out of whatever
		// the source data is. This is based on the rules for the color field
		// defined in clause 13.3.6 of Part 1 of the X3D abstract spec. Assumes
		// that the coordIndex list is valid. If not set, we wouldn't be
		// rendering
		//
		// if the per-vertex flag is false
		//   if the index list is not empty
		//      per-face indexes are expanded to be per vertex
		//      based on the index list info in the coordIndex
		//   else
		//      build a per-vertex list that just lists each face
		//      for x number of times for the corresponding face in
		//      the coordIndex list
		// else
		//   if the index list is not empty
		//      use the index list
		//   else
		//      use the coordIndex values directly
		if(!per_vertex) {
			final_list = new int[numCoordIndex];
			
			if((src_list != null) && (src_list.length != 0)) {
				// Each index in the list is the index for the face, so just
				// repeat it for the number of times that the coordIndex
				// defines vertices for the face
				int src_pos = 0;
				for(int i = 0; i < numCoordIndex; i++) {
					if(vfCoordIndex[i] != -1)
						final_list[i] = src_list[src_pos];
					else {
						final_list[i] = -1;
						src_pos++;
					}
				}
			} else {
				// We don't have anything, so the list becomes an index starting
				// at 0 and then just incrementing each time we hit a new face
				int src_pos = 0;
				for(int i = 0; i < numCoordIndex; i++) {
					if(vfCoordIndex[i] != -1)
						final_list[i] = src_pos;
					else {
						final_list[i] = -1;
						src_pos++;
					}
				}
			}
		} else {
			if((src_list != null) && (src_list.length != 0)) {
				final_list = src_list;
			} else {
				final_list = vfCoordIndex;
			}
		}
		
		// Now copy it back to the original list
		switch(fieldIndex) {
		case FIELD_NORMALINDEX:
			lfNormalIndex = final_list;
			break;
			
		case FIELD_COLORINDEX:
			lfColorIndex = final_list;
			break;
			
		case FIELD_TEXCOORDINDEX:
			lfTexCoordIndex = final_list;
			break;
		}
	}
	
	/**
	 * Go through the coordIndex array and work out what the maximum polygon
	 * size will be before we've done any processing. It does not define the
	 * current maxPolySize variable.
	 *
	 * @return The maximum size that this check found
	 */
	private int checkMaxPolySize() {
		int cur_size = 0;
		int max_size = 0;
		polygonCount = 0;
		
		for(int i = 0; i < numCoordIndex; i++) {
			if(vfCoordIndex[i] == -1) {
				if (cur_size == 3)
					triCnt++;
				else if (cur_size == 4)
					quadCnt++;
				else
					ngonCnt++;
				
				if(cur_size > max_size)
					max_size = cur_size;
				
				cur_size = 0;
				polygonCount++;
			} else {
				cur_size++;
			}
		}
		
		// One last check on the last index. The spec allows the user to not
		// need to specify -1 as the last value. If we don't check for this,
		// the max size would never be set.
		if((numCoordIndex != 0) && (vfCoordIndex[numCoordIndex - 1] != -1)) {
			if (cur_size == 3)
				triCnt++;
			else if (cur_size == 4)
				quadCnt++;
			else
				ngonCnt++;
			
			if(cur_size > max_size)
				max_size = cur_size;
			
			polygonCount++;
		}
		
		return max_size;
	}
	
	/**
	 * Auto-generate texture coordinates from the bounds of the geometry.
	 */
	private void generateTextureCoordinates() {
		if (min == null)
			min = new float[3];
		if (max == null)
			max = new float[3];
		
		min[0] = Float.POSITIVE_INFINITY;
		min[1] = Float.POSITIVE_INFINITY;
		min[2] = Float.POSITIVE_INFINITY;
		
		max[0] = Float.NEGATIVE_INFINITY;
		max[1] = Float.NEGATIVE_INFINITY;
		max[2] = Float.NEGATIVE_INFINITY;
		
		// Create a bounding box based on the actual bounds described by the
		// index values.
		float val;
		
		for(int i=0; i < geomData.vertexCount; i++) {
			val = geomData.coordinates[i * 3];
			
			if(val < min[0])
				min[0] = val;
			
			if(val > max[0])
				max[0] = val;
			
			val = geomData.coordinates[i * 3 + 1];
			
			if(val < min[1])
				min[1] = val;
			
			if(val > max[1])
				max[1] = val;
			
			val = geomData.coordinates[i * 3 + 2];
			
			if(val < min[2])
				min[2] = val;
			
			if(val > max[2])
				max[2] = val;
		}
		
		float x_size, y_size, z_size;
		
		// Fast Compute of abs(minx) + abs(miny)
		x_size = (min[0] < 0 ? -min[0] : min[0]) +
			(max[0] < 0 ? -max[0] : max[0]);
		y_size = (min[1] < 0 ? -min[1] : min[1]) +
			(max[1] < 0 ? -max[1] : max[1]);
		z_size = (min[2] < 0 ? -min[2] : min[2]) +
			(max[2] < 0 ? -max[2] : max[2]);
		
		// Determine largest sides
		int s;
		int t = 0;
		
		s = ((x_size >= y_size) ?
			((x_size >= z_size) ? 0 : 2) : ((y_size >= z_size) ? 1 : 2));
		
		float sscale = 1;
		float tscale = 1;
		
		switch(s) {
		case 0:
			t = (y_size >= z_size ) ? 1 : 2;
			sscale = 1.0f / x_size;
			tscale = 1.0f / x_size;
			break;
		case 1:
			t = (x_size >= z_size) ? 0 : 2;
			sscale = 1.0f / y_size;
			tscale = 1.0f / y_size;
			break;
		case 2:
			t = (x_size >= y_size) ? 0 : 1;
			sscale = 1.0f / z_size;
			tscale = 1.0f / z_size;
			break;
		}
		
		float soffset = -min[s];
		float toffset = -min[t];
		float fs;
		float ft;
		
		int num_vals = geomData.vertexCount * 2;
		
		if((geomData.textureCoordinates == null) ||
			(geomData.textureCoordinates.length < num_vals)) {
			
			geomData.textureCoordinates = new float[num_vals];
		}
		
		for (int i = 0; i < geomData.vertexCount; i++) {
			fs = ((float)(geomData.coordinates[i * 3 + s] + soffset)) * sscale;
			ft = ((float)(geomData.coordinates[i * 3 + t] + toffset)) * tscale;
			
			geomData.textureCoordinates[i * 2] = fs;
			geomData.textureCoordinates[i * 2 + 1] = ft;
		}
	}
	
	/**
	 * Auto-generate normals from the face information. Assumes that we are
	 * now running with the tesselated face.
	 */
	private void generateNormals() {
		int p;
		int i;
		
		for(i = 0; i < triangleCount; i++) {
			
			p = i * 3 * 3;
			
			createFaceNormal(geomData.coordinates, p, 3,  faceNormals[i]);
		}
		
		// Now calculate the normals. If the creaseAngle is zero, that means
		// no averaging - each face has its own normals, so do a special case
		// handler for that to save a lot of calculation
		int num_normals = triangleCount * 9;
		
		if(vfCreaseAngle == 0) {
			if((geomData.normals == null) ||
				(geomData.normals.length < num_normals))
				geomData.normals = new float[num_normals];
			
			// Spread the face normals for the three vertices of the
			// triangle.
			for(i = 0; i < triangleCount; i++) {
				geomData.normals[i * 9] = faceNormals[i][0];
				geomData.normals[i * 9 + 1] = faceNormals[i][1];
				geomData.normals[i * 9 + 2] = faceNormals[i][2];
				
				geomData.normals[i * 9 + 3] = faceNormals[i][0];
				geomData.normals[i * 9 + 4] = faceNormals[i][1];
				geomData.normals[i * 9 + 5] = faceNormals[i][2];
				
				geomData.normals[i * 9 + 6] = faceNormals[i][0];
				geomData.normals[i * 9 + 7] = faceNormals[i][1];
				geomData.normals[i * 9 + 8] = faceNormals[i][2];
			}
		} else {
			if((geomData.normals == null) ||
				(geomData.normals.length < num_normals))
				geomData.normals = new float[num_normals];
			
			// The normal averaging is relatively dumb right now and calculates
			// every vertex of every face rather than only calculating the
			// shared vertices once.  A vector of smoothed face would need
			// to be kept as a single boolean per vertex would not work.
			//
			// for each face
			//    for each vertex
			//        base_normal = face_normal
			//        for each face sharing that vertex
			//           if acos(current_normal dot shared_face) < cosCreaseAngle
			//              add shared normal to base_normal
			//    normalise(base_normal)
			//
			float face_x, face_y, face_z;
			float norm_x, norm_y, norm_z;
			int vertex_idx = 0;
			/*
			for(i = 0; i < maxIndexValue; i++)
							normalCalculated[i] = false;
			*/
			for(i = 0; i < triangleCount; i++) {
				face_x = faceNormals[i][0];
				face_y = faceNormals[i][1];
				face_z = faceNormals[i][2];
				
				for(int j = 0; j < 3; j++) {
					vertex_idx = tsCoordIndex[i * 3 + j];
					
					norm_x = face_x;
					norm_y = face_y;
					norm_z = face_z;
					
					int[] connected_faces = vertexToFace[vertex_idx];
					boolean needs_normalising = false;
					int num_cnx =
						connected_faces == null ? 0 : connected_faces.length;
					
					for(int k = 0; k < num_cnx; k++) {
						int shared_face = connected_faces[k];
						
						if(shared_face == i)
							continue;
						
						// inline dot product
						float dot_prod = faceNormals[shared_face][0] * face_x +
							faceNormals[shared_face][1] * face_y +
							faceNormals[shared_face][2] * face_z;
						
						
						// Check slightly > 1 as float roundoff cause some issues
						if(dot_prod < 1.01f && dot_prod >= cosCreaseAngle) {
							needs_normalising = true;
							
							norm_x += faceNormals[shared_face][0];
							norm_y += faceNormals[shared_face][1];
							norm_z += faceNormals[shared_face][2];
						}
					}
					
					if (needs_normalising) {
						double len = norm_x * norm_x +
							norm_y * norm_y +
							norm_z * norm_z;
						
						if(len != 0) {
							len = 1 / Math.sqrt(len);
							norm_x = (float)(norm_x * len);
							norm_y = (float)(norm_y * len);
							norm_z = (float)(norm_z * len);
						}
					}
					geomData.normals[i * 9 + j * 3] = norm_x;
					geomData.normals[i * 9 + j * 3 + 1] = norm_y;
					geomData.normals[i * 9 + j * 3 + 2] = norm_z;
				}
			}
		}
	}
	
	/**
	 * Retesselate the faces coord index array. This will build all of the
	 * arrays to the correct structure of triangles only. It assumes that
	 * all of the lf* arrays are now the same length and represent the
	 * same face information.
	 *
	 * @param required True if we have to update everything
	 */
	private void rebuildFaceLists(boolean required, boolean hasColor) {
		int i;
		
		// First make a fast lookup list of the number of vertices that
		// each face contains.
		//if(required || (changeFlags & BaseIndexedGeometryNode.COORDS_INDEX_CHANGED) != 0) {
			if((rawVerticesPerFace == null) || (rawVerticesPerFace.length < polygonCount))
				rawVerticesPerFace = new int[polygonCount];
			
			rawVerticesPerFace[0] = 0;
			int current_face = 0;
			
			for(i = 0; i < numCoordIndex; i++) {
				if(vfCoordIndex[i] != -1) {
					rawVerticesPerFace[current_face]++;
				} else {
					current_face++;
					if(current_face < polygonCount)
						rawVerticesPerFace[current_face] = 0;
				}
			}
			
			if((lfCoordIndex == null) || (lfCoordIndex.length != numCoordIndex))
				lfCoordIndex = new int[numCoordIndex];
			
			if(true || vfCcw) {
				System.arraycopy(vfCoordIndex, 0, lfCoordIndex, 0, numCoordIndex);
			} else {
				// Re-arrange order of index lists if ccw.
				int first = 0;
				int last;
				int cnt;
				
				// do coords separately from the others
				for(i = 0; i < polygonCount; i++) {
					cnt = 1;
					last = first + rawVerticesPerFace[i];
					for(int j = first; j < last; j++) {
						lfCoordIndex[last - cnt] = vfCoordIndex[j];
						cnt++;
					}
					
					first = last + 1;
					if(i != (polygonCount - 1))
						lfCoordIndex[last] = -1;
				}
				
				// Now tex coord etc
				first = 0;
				int half_last;
				int tmp_index;
				if(hasColor) {
					for(i = 0; i < polygonCount; i++) {
						cnt = 1;
						last = first + rawVerticesPerFace[i];
						half_last = first + (rawVerticesPerFace[i] >> 1);
						
						for(int j = first; j < half_last; j++) {
							tmp_index = lfTexCoordIndex[j];
							lfTexCoordIndex[j] = lfTexCoordIndex[last - cnt];
							lfTexCoordIndex[last - cnt] = tmp_index;
							
							tmp_index = lfColorIndex[j];
							lfColorIndex[j] = lfColorIndex[last - cnt];
							lfColorIndex[last - cnt] = tmp_index;
							
							tmp_index = lfNormalIndex[j];
							lfNormalIndex[j] = lfNormalIndex[last - cnt];
							lfNormalIndex[last - cnt] = tmp_index;
							
							cnt++;
						}
						
						first = last + 1;
					}
				} else {
					for(i = 0; i < polygonCount; i++) {
						cnt = 1;
						last = first + rawVerticesPerFace[i];
						half_last = first + (rawVerticesPerFace[i] >> 1);
						
						for(int j = first; j < half_last; j++) {
							tmp_index = lfTexCoordIndex[j];
							lfTexCoordIndex[j] = lfTexCoordIndex[last - cnt];
							lfTexCoordIndex[last - cnt] = tmp_index;
							
							tmp_index = lfNormalIndex[j];
							lfNormalIndex[j] = lfNormalIndex[last - cnt];
							lfNormalIndex[last - cnt] = tmp_index;
							
							cnt++;
						}
						
						first = last + 1;
					}
				}
			}
		//}
		
		if(vfConvex)
			buildConvexPolygons(required, hasColor);
		else
			buildConcavePolygons(required, hasColor);
		
		// Go through the triangle list and build the connection lists
		//if(required || (changeFlags & BaseIndexedGeometryNode.COORDS_INDEX_CHANGED) != 0) {
			if((faceNormals == null) || (faceNormals.length < triangleCount) )
				faceNormals = new float[triangleCount][3];
			
			maxIndexValue = 0;
			
			for(i = 0; i < numCoordIndex; i++) {
				if(vfCoordIndex[i] > maxIndexValue)
					maxIndexValue = vfCoordIndex[i];
			}
			
			int tri_index_cnt = triangleCount * 3;
			if((vertexToFace == null) ||
				(vertexToFace.length < maxIndexValue + 1))
				vertexToFace = new int[maxIndexValue + 1][];
			
			// run through the tesselated index list and build another vertex
			// user count listing. The array is a temporary that will be reused
			// a number of times.
			int[] idx_tmp = new int[maxIndexValue + 1];
			
			for(i = 0; i < tri_index_cnt; i++)
				idx_tmp[tsCoordIndex[i]]++;
			
			// Finish allocating the vertexToFace list of the correct size
			// using the vertex_user_count list above
			for(i = 0; i < maxIndexValue + 1; i++) {
				if((vertexToFace[i] == null) ||
					(vertexToFace[i].length < idx_tmp[i]))
					vertexToFace[i] = new int[idx_tmp[i]];
			}
			
			// Finally, build the vertexToFace list. Use a temporary list to
			// keep track of where we are in the list as we're setting the face
			// numbers. First clear the temp array.
			for(i = 0; i < maxIndexValue + 1; i++)
				idx_tmp[i] = 0;
			
			//int current_face = 0;
			current_face = 0;
			
			for(i = 0; i < tri_index_cnt; i++) {
				int idx = tsCoordIndex[i];
				int pos = idx_tmp[idx];
				vertexToFace[idx][pos] = current_face;
				idx_tmp[idx]++;
				
				if((i % 3) == 2)
					current_face++;
			}
			
			/*
			if((normalCalculated == null) ||
							normalCalculated.length < maxIndexValue + 1) {
							
							normalCalculated = new boolean[maxIndexValue + 1];
							finalNormals = new float[(maxIndexValue + 1) * 3];
						}
			*/
		//}
	}
	
	/**
	 * There are simple polygons, so just rebuild the index list using the
	 * assumption that they're convex. A polygon is tesellated using the
	 * first vertex to any following pairs effectively as a fan from the
	 * first point.
	 *
	 * @param required True if we have to update everything
	 * @param hasColor True if we have explicit colour values
	 */
	private void buildConvexPolygons(boolean required, boolean hasColor) {
		
		int ts_index = 0;
		int face_index = 0;
		int i, j;
		
		//if(required || (changeFlags & BaseIndexedGeometryNode.COORDS_INDEX_CHANGED) != 0) {
			for(i = 0; i < lfCoordIndex.length; i++) {
				
				if(rawVerticesPerFace[face_index] < 3) {
					i += rawVerticesPerFace[face_index];
					face_index++;
					continue;
				}
				
				for(j = 0; j < rawVerticesPerFace[face_index] - 2; j++) {
					if(i + 3 + j > lfCoordIndex.length) {
						System.out.println("Invalid coord index in IndexedFaceSet");
						j = rawVerticesPerFace[face_index] - 2;
						break;
					}
					
					tsCoordIndex[ts_index] = lfCoordIndex[i];
					tsCoordIndex[ts_index + 1] = lfCoordIndex[i + 1 + j];
					tsCoordIndex[ts_index + 2] = lfCoordIndex[i + 2 + j];
					ts_index += 3;
				}
				
				i += j + 2;
				face_index++;
			}
			
			triangleCount = ts_index / 3;
		//}
		
		//if(required || (changeFlags & BaseIndexedGeometryNode.NORMALS_INDEX_CHANGED) != 0) {
			ts_index = 0;
			face_index = 0;
			for(i = 0; i < lfCoordIndex.length; i++) {
				
				if(rawVerticesPerFace[face_index] < 3) {
					i += rawVerticesPerFace[face_index];
					face_index++;
					continue;
				}
				
				for(j = 0; j < rawVerticesPerFace[face_index] - 2; j++) {
					if(i + 3 + j > lfNormalIndex.length) {
						System.out.println("Invalid normal index in IndexedFaceSet");
						j = rawVerticesPerFace[face_index] - 2;
						break;
					}
					
					tsNormalIndex[ts_index] = lfNormalIndex[i];
					tsNormalIndex[ts_index + 1] = lfNormalIndex[i + 1 + j];
					tsNormalIndex[ts_index + 2] = lfNormalIndex[i + 2 + j];
					ts_index += 3;
				}
				
				i += j + 2;
				face_index++;
			}
		//}
		
		//if(required || (changeFlags & BaseIndexedGeometryNode.TEXCOORDS_INDEX_CHANGED) != 0) {
			ts_index = 0;
			face_index = 0;
			
			for(i = 0; i < lfCoordIndex.length; i++) {
				
				if(rawVerticesPerFace[face_index] < 3) {
					i += rawVerticesPerFace[face_index];
					face_index++;
					continue;
				}
				
				for(j = 0; j < rawVerticesPerFace[face_index] - 2; j++) {
					if(i + 3 + j > lfTexCoordIndex.length) {
						System.out.println("Invalid texture index in IndexedFaceSet");
						j = rawVerticesPerFace[face_index] - 2;
						break;
					}
					
					tsTexCoordIndex[ts_index] = lfTexCoordIndex[i];
					tsTexCoordIndex[ts_index + 1] = lfTexCoordIndex[i + 1 + j];
					tsTexCoordIndex[ts_index + 2] = lfTexCoordIndex[i + 2 + j];
					ts_index += 3;
				}
				
				i += j + 2;
				face_index++;
			}
		//}
		
		//if(hasColor &&
		//	(required || ((changeFlags & BaseIndexedGeometryNode.COLORS_INDEX_CHANGED) != 0))) {
		if (hasColor) {
			
			ts_index = 0;
			face_index = 0;
			
			for(i = 0; i < lfCoordIndex.length; i++) {
				if(rawVerticesPerFace[face_index] < 3) {
					i += rawVerticesPerFace[face_index];
					face_index++;
					continue;
				}
				
				for(j = 0; j < rawVerticesPerFace[face_index] - 2; j++) {
					if(i + 3 + j > lfColorIndex.length) {
						System.out.println("Invalid color index in IndexedFaceSet");
						j = rawVerticesPerFace[face_index] - 2;
						break;
					}
					
					tsColorIndex[ts_index] = lfColorIndex[i];
					tsColorIndex[ts_index + 1] = lfColorIndex[i + 1 + j];
					tsColorIndex[ts_index + 2] = lfColorIndex[i + 2 + j];
					ts_index += 3;
				}
				
				i += j + 2;
				face_index++;
			}
		}
	}
	
	
	/**
	 * There are complex polygons that may have convex portions, so rebuild the
	 * index list using Siedel's Algorithm.
	 *
	 * @param required True if we have to update everything
	 * @param hasColor True if we have explicit colour values
	 */
	private void buildConcavePolygons(boolean required, boolean hasColor) {
		
		// If we have triangles, don't bother with this routine
		if(maxPolySize < 4) {
			buildConvexPolygons(required, hasColor);
			return;
		}
		
		//if(required || (changeFlags & BaseIndexedGeometryNode.COORDS_INDEX_CHANGED) != 0) {
			int ts_index = 0;
			int face_index = 0;
			int i, j;
			
			for(i = 0; i < lfCoordIndex.length; i++) {
				
				if(rawVerticesPerFace[face_index] < 3) {
					i += rawVerticesPerFace[face_index];
					face_index++;
					continue;
				}
				
				// Need to pre-calculate the normal here for the face.
				createFaceNormal(lfCoord,
					lfCoordIndex,
					i,
					rawVerticesPerFace[face_index],
					normalTmp);
				
				int num_tris =
					triangulator.triangulateConcavePolygon(lfCoord,
					i,
					rawVerticesPerFace[face_index],
					lfCoordIndex,
					i,
					lfNormalIndex,
					i,
					lfColorIndex,
					i,
					lfTexCoordIndex,
					triangleOutput,
					normalOutput,
					colorOutput,
					texCoordOutput,
					normalTmp);
				
				// Check for errors during triangulation. This will be a
				// negative number if there was. Still has valid triangles
				// though, so negate to get positive value for later use.
				if(num_tris < 0) {
					System.out.print("Invalid poly face is ");
					System.out.println(face_index);
					System.out.print("index list is ");
					
					for(int a = 0; a < rawVerticesPerFace[face_index]; a++) {
						System.out.print(lfCoordIndex[i + a]);
						System.out.print(' ');
					}
					
					System.out.println(-1);
					num_tris = -num_tris;
				}
				
				for(j = 0; j < num_tris; j++) {
					tsCoordIndex[ts_index] = triangleOutput[j * 3];
					tsCoordIndex[ts_index + 1] = triangleOutput[j * 3 + 1];
					tsCoordIndex[ts_index + 2] = triangleOutput[j * 3 + 2];
					
					tsNormalIndex[ts_index] = normalOutput[j * 3];
					tsNormalIndex[ts_index + 1] = normalOutput[j * 3 + 1];
					tsNormalIndex[ts_index + 2] = normalOutput[j * 3 + 2];
					
					tsTexCoordIndex[ts_index] = texCoordOutput[j * 3];
					tsTexCoordIndex[ts_index + 1] = texCoordOutput[j * 3 + 1];
					tsTexCoordIndex[ts_index + 2] = texCoordOutput[j * 3 + 2];
					
					if(hasColor) {
						tsColorIndex[ts_index] = colorOutput[j * 3];
						tsColorIndex[ts_index + 1] = colorOutput[j * 3 + 1];
						tsColorIndex[ts_index + 2] = colorOutput[j * 3 + 2];
					}
					
					ts_index += 3;
				}
				
				// Triangulate based on the face.
				i += rawVerticesPerFace[face_index];
				face_index++;
			}
			
			triangleCount = ts_index / 3;
		//}
	}
	
	/**
	 * Update the coordinate array based on the triangle list.
	 */
	private void updateCoordinateArray() {
		int max_tri_index = triangleCount * 3;
		int pos;
		int idx = 0;
		
		if((geomData.coordinates == null) ||
			(geomData.coordinates.length != max_tri_index * 3))
			geomData.coordinates = new float[max_tri_index * 3];
		
		
		for(int i = 0; i < max_tri_index; i++) {
			pos = tsCoordIndex[i] * 3;
			
			geomData.coordinates[idx++] = lfCoord[pos];
			geomData.coordinates[idx++] = lfCoord[pos + 1];
			geomData.coordinates[idx++] = lfCoord[pos + 2];
		}
		
		geomData.vertexCount = max_tri_index;
		
	}
	
	/**
	 * Update the coordinate array based on the triangle list.
	 */
	private void updateColorArray() {
		int max_tri_index = triangleCount * 3;
		int pos;
		int idx = 0;
		
		if((geomData.colors == null) ||
			(geomData.colors.length < max_tri_index * numColorComponents))
			geomData.colors = new float[max_tri_index * numColorComponents];
		
		switch(numColorComponents) {
		case 1:
		case 2:
		case 3:
			for(int i = 0; i < max_tri_index; i++) {
				pos = tsColorIndex[i] * 3;
				
				if (pos + 3 > lfColor.length || pos < 0) {
					System.out.println("Invalid color index in IndexedFaceSet");
					geomData.colors = null;
					return;
				}
				
				geomData.colors[i * 3] = lfColor[pos];
				geomData.colors[i * 3 + 1] = lfColor[pos + 1];
				geomData.colors[i * 3 + 2] = lfColor[pos + 2];
			}
			
			break;
			
		case 4:
			for(int i = 0; i < max_tri_index; i++) {
				pos = tsColorIndex[i] * 4;
				
				if (pos + 4 > lfColor.length || pos < 0) {
					System.out.println("Invalid color index in IndexedFaceSet");
					geomData.colors = null;
					return;
				}
				
				geomData.colors[i * 4] = lfColor[pos];
				geomData.colors[i * 4 + 1] = lfColor[pos + 1];
				geomData.colors[i * 4 + 2] = lfColor[pos + 2];
				geomData.colors[i * 4 + 3] = lfColor[pos + 3];
			}
			break;
		}
	}
	
	/**
	 * Update the coordinate array based on the triangle list.
	 */
	private void updateNormalArray() {
		int max_tri_index = triangleCount * 3;
		int pos;
		int idx = 0;
		
		if((geomData.normals == null) ||
			(geomData.normals.length < max_tri_index * 3))
			geomData.normals = new float[max_tri_index * 3];
		
		for(int i = 0; i < max_tri_index; i++) {
			pos = tsNormalIndex[i] * 3;
			
			if (pos + 3 > lfNormal.length || pos < 0) {
				System.out.println("Invalid normal index in IndexedFaceSet");
				geomData.colors = null;
				return;
			}
			
			geomData.normals[i * 3]     = lfNormal[pos];
			geomData.normals[i * 3 + 1] = lfNormal[pos + 1];
			geomData.normals[i * 3 + 2] = lfNormal[pos + 2];
		}
	}
	
	/**
	 * Update the coordinate array based on the triangle list.
	 *
	 * @param tset The texture set to write to
	 * @param numTexSets The max texture set
	 */
	private void updateTexCoordinateArray(int tset, int numTexSets) {
		int max_tri_index = triangleCount * 3;
		int pos;
		int idx = 0;
		int max_set_size = 0;
		
		max_set_size = geomData.vertexCount * numTextureDimensions;
		
		geomData.textureCoordinates = new float[max_set_size];
		
		switch(numTextureDimensions) {
		case 2:
			for(int i = 0; i < max_tri_index; i++) {
				pos = tsTexCoordIndex[i] * 2;
				
				if(pos > lfTexCoord.length - 2 || pos < 0) {
					System.out.println("Invalid index in texture coordinate, ignoring");
				} else {
					geomData.textureCoordinates[i * 2] = lfTexCoord[pos];
					geomData.textureCoordinates[i * 2 + 1] = lfTexCoord[pos + 1];
				}
			}
			break;
			/*
		case 3:
			for(int i = 0; i < max_tri_index; i++) {
				pos = tsTexCoordIndex[i] * 3;
				geomData.textureCoordinates[tset][i * 3] = lfTexCoord[pos];
				geomData.textureCoordinates[tset][i * 3 + 1] = lfTexCoord[pos + 1];
				geomData.textureCoordinates[tset][i * 3 + 2] = lfTexCoord[pos + 2];
			}
			break;
			
		case 4:
			for(int i = 0; i < max_tri_index; i++) {
				pos = tsTexCoordIndex[i] * 4;
				geomData.textureCoordinates[tset][i * 4]     = lfTexCoord[pos];
				geomData.textureCoordinates[tset][i * 4 + 1] = lfTexCoord[pos + 1];
				geomData.textureCoordinates[tset][i * 4 + 2] = lfTexCoord[pos + 2];
				geomData.textureCoordinates[tset][i * 4 + 3] = lfTexCoord[pos + 3];
			}
			break;
			*/
		}
	}
	
	/**
	 * Convenience method to create a normal for the given vertex coordinates
	 * and normal array. This performs a cross product of the two vectors
	 * described by the middle and two end points.
	 *
	 * @param coords The coordinate array to read values from
	 * @param p The index of the middle point
	 * @param p1 The index of the first point
	 * @param p2 The index of the second point
	 * @param res A temporary value containing the normal value
	 */
	private void createFaceNormal(float[] coords, int start, int numVertex, float[] res) {
		
		// Uses the Newell method to calculate the face normal
		float nx = 0;
		float ny = 0;
		float nz = 0;
		int x = start;
		int y = start + 1;
		int z = start + 2;
		
		for(int i = 0; i < numVertex - 1; i++) {
			nx += (coords[y] - coords[y + 3]) * (coords[z] + coords[z + 3]);
			ny += (coords[z] - coords[z + 3]) * (coords[x] + coords[x + 3]);
			nz += (coords[x] - coords[x + 3]) * (coords[y] + coords[y + 3]);
			
			y += 3;
			x += 3;
			z += 3;
		}
		
		// The last vertex uses the start position
		nx += (coords[y] - coords[start + 1]) * (coords[z] + coords[start + 2]);
		ny += (coords[z] - coords[start + 2]) * (coords[x] + coords[start]);
		nz += (coords[x] - coords[start]) * (coords[y] + coords[start + 1]);
		
		res[0] = nx;
		res[1] = ny;
		res[2] = nz;
		
		double len = nx * nx + ny * ny + nz * nz;
		if(len != 0) {
			len = (vfCcw ? 1 : -1) / Math.sqrt(len);
			res[0] *= len;
			res[1] *= len;
			res[2] *= len;
		}
	}
	
	/**
	 * Convenience method to create a normal for the given vertex coordinates
	 * and normal array. This performs a cross product of the two vectors
	 * described by the middle and two end points.
	 *
	 * @param coords The coordinate array to read values from
	 * @param p The index of the middle point
	 * @param p1 The index of the first point
	 * @param p2 The index of the second point
	 * @param res A temporary value containing the normal value
	 */
	private void createFaceNormal(float[] coords,
		int[] coordIndex,
		int start,
		int numVertex,
		float[] res) {
		
		// Uses the Newell method to calculate the face normal
		float nx = 0;
		float ny = 0;
		float nz = 0;
		int x1, y1, z1, x2, y2, z2;
		
		x1 = coordIndex[start] * 3;
		y1 = x1 + 1;
		z1 = x1 + 2;
		
		for(int i = 0; i < numVertex - 1; i++) {
			
			x2 = coordIndex[start + i + 1] * 3;
			y2 = x2 + 1;
			z2 = x2 + 2;
			
			nx += (coords[y1] - coords[y2]) * (coords[z1] + coords[z2]);
			ny += (coords[z1] - coords[z2]) * (coords[x1] + coords[x2]);
			nz += (coords[x1] - coords[x2]) * (coords[y1] + coords[y2]);
			
			x1 = x2;
			y1 = y2;
			z1 = z2;
		}
		
		// The last vertex uses the start position
		x2 = coordIndex[start] * 3;
		y2 = x2 + 1;
		z2 = x2 + 2;
		
		nx += (coords[y1] - coords[y2]) * (coords[z1] + coords[z2]);
		ny += (coords[z1] - coords[z2]) * (coords[x1] + coords[x2]);
		nz += (coords[x1] - coords[x2]) * (coords[y1] + coords[y2]);
		
		res[0] = nx;
		res[1] = ny;
		res[2] = nz;
		
		double len = nx * nx + ny * ny + nz * nz;
		if(len != 0) {
			len = (vfCcw ? 1 : -1) / Math.sqrt(len);
			res[0] *= len;
			res[1] *= len;
			res[2] *= len;
		}
	}
}
