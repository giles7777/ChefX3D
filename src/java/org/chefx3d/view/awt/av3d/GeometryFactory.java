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
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.*;

import org.j3d.geom.GeometryData;
import org.j3d.geom.BoxGenerator;
import org.j3d.geom.ConeGenerator;
import org.j3d.geom.CylinderGenerator;
import org.j3d.geom.SphereGenerator;

import org.j3d.util.I18nManager;
import org.j3d.util.TriangleUtils;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A factory class that generates aviatrix Geometry Nodes from XNodes.
 *
 * @author Rex Melton
 * @version $Revision: 1.15 $
 */
class GeometryFactory {
	
    /** The minimum number of vertices before using VBO's */
    public static final int VBO_MIN_VERTICES = 1000;

    /** Index of the tangent attribute */
    private static final int TANGENT_ATTRIB_IDX = 5;

	/** The extension string used to determine if attribute arrays are supported */
	private static final String GL_ARB_VERTEX_PROGRAM = "GL_ARB_vertex_program";
	
    /** Warning message aviatrix node cannot be loaded */
    private static final String CANNOT_LOAD_NODE_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.cannotLoadNodeMsg";

    /** Identifiers */
    private static final int BOX = 0;
    private static final int CONE = 1;
    private static final int CYLINDER = 2;
    private static final int SPHERE = 3;
    private static final int POINTSET = 4;
    private static final int LINESET = 5;
    private static final int INDEXEDFACESET = 6;
    private static final int INDEXEDLINESET = 7;
    private static final int INDEXEDTRIANGLESET = 8;
    private static final int INDEXEDTRIANGLEFANSET = 9;
    private static final int INDEXEDTRIANGLESTRIPSET = 10;
    private static final int TRIANGLESET = 11;
    private static final int TRIANGLEFANSET = 12;
    private static final int TRIANGLESTRIPSET = 13;

    /** Mapping of nodes */
    private static HashMap<String, Integer> nodeMap;

	/** The GLInfo instance */
	private GLInfo gl_info;
	
	/** Flag indicating that attribute arrays are supported */
	private boolean attributeArraysSupported;
	
    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** I18N manager for sourcing messages */
    private I18nManager i18n_mgr;

    static {
        nodeMap = new HashMap<String, Integer>();
        nodeMap.put("Box", new Integer(BOX));
        nodeMap.put("Cone", new Integer(CONE));
        nodeMap.put("Cylinder", new Integer(CYLINDER));
        nodeMap.put("Sphere", new Integer(SPHERE));
        nodeMap.put("PointSet", new Integer(POINTSET));
        nodeMap.put("LineSet", new Integer(LINESET));
        nodeMap.put("IndexedFaceSet", new Integer(INDEXEDFACESET));
        nodeMap.put("IndexedLineSet", new Integer(INDEXEDLINESET));
        nodeMap.put("IndexedTriangleSet", new Integer(INDEXEDTRIANGLESET));
        //nodeMap.put("IndexedTriangleFanSet", new Integer(INDEXEDTRIANGLEFANSET));
        //nodeMap.put("IndexedTriangleStripSet", new Integer(INDEXEDTRIANGLESTRIPSET));
        //nodeMap.put("TriangleSet", new Integer(TRIANGLESET));
        //nodeMap.put("TriangleFanSet", new Integer(TRIANGLEFANSET));
        //nodeMap.put("TriangleStripSet", new Integer(TRIANGLESTRIPSET));
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
	 * @param gl_info The GLInfo
     */
    GeometryFactory(ErrorReporter reporter, GLInfo gl_info) {
		
        i18n_mgr = I18nManager.getManager();
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
		
		attributeArraysSupported = false;
		
		this.gl_info = gl_info;
		if (gl_info != null) {
			String[] gl_extensions = gl_info.getExtensions();
			if (gl_extensions != null) {
				for (int i = 0; i < gl_extensions.length; i++) {
					String ext_string = gl_extensions[i];
					if (GL_ARB_VERTEX_PROGRAM.equalsIgnoreCase(ext_string)) {
						attributeArraysSupported = true;
						break;
					}
				}
				
			}
		}
    }

    /**
     * Return the aviatrix Geometry node that cooresponds to the argument XNode
     *
     * @param The XNode
     * @param The emissive color from the geometry's material.
     * @return The aviatrix Geometry node
     */
    Geometry getGeometry(XNode xnode, float[] emissiveColor) {
        Geometry geom = null;
        String node_name = xnode.getNodeName();

        if (nodeMap.containsKey(node_name)) {
            switch(nodeMap.get(node_name)) {
            case BOX:
                geom = getBox(xnode);
                break;
            case CONE:
                geom = getCone(xnode);
                break;
            case CYLINDER:
                geom = getCylinder(xnode);
                break;
            case SPHERE:
                geom = getSphere(xnode);
                break;
            case POINTSET:
                geom = getPointSet(xnode, emissiveColor);
                break;
            case LINESET:
                geom = getLineSet(xnode, emissiveColor);
                break;
            case INDEXEDFACESET:
                geom = getIndexedFaceSet(xnode);
                break;
            case INDEXEDLINESET:
                geom = getIndexedLineSet(xnode);
                break;
            case INDEXEDTRIANGLESET:
                geom = getIndexedTriangleSet(xnode);
                break;
            case INDEXEDTRIANGLEFANSET:
                geom = getIndexedTriangleFanSet(xnode);
                break;
            case INDEXEDTRIANGLESTRIPSET:
                geom = getIndexedTriangleStripSet(xnode);
                break;
            case TRIANGLESET:
                geom = getTriangleSet(xnode);
                break;
            case TRIANGLEFANSET:
                geom = getTriangleFanSet(xnode);
                break;
            case TRIANGLESTRIPSET:
                geom = getTriangleStripSet(xnode);
                break;
            }
        } else {
            String msg = i18n_mgr.getString(CANNOT_LOAD_NODE_MSG) +
                ": "+ xnode.getNodeName();
            errorReporter.warningReport(msg, null);
        }
        return(geom);
    }

    /**
     * Create and return Geometry for a Box
     *
     * @param xnode The XNode
     * @return A Box
     */
    private Geometry getBox(XNode xnode) {

        // defaults
        float[] size = new float[]{2, 2, 2};
        boolean solid = true;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("size")) {
                size = (float [])xnode.getFieldData(field_name);
            } else if (field_name.equals("solid")) {
                solid = (Boolean)xnode.getFieldData(field_name);
            }
        }

        BoxGenerator generator = new BoxGenerator(size[0], size[1], size[2]);
        GeometryData data = new GeometryData();
        data.geometryType = GeometryData.TRIANGLE_STRIPS;
        data.geometryComponents = GeometryData.NORMAL_DATA |
                                  GeometryData.TEXTURE_2D_DATA;

        generator.generate(data);

        TriangleStripArray tsa = new TriangleStripArray(
            false,
            VertexGeometry.VBO_HINT_STATIC);

        tsa.setVertices(
            TriangleStripArray.COORDINATE_3,
            data.coordinates,
            data.vertexCount);

        tsa.setStripCount(data.stripCounts, data.numStrips);
        tsa.setNormals(data.normals);

        // Make an array of objects for the texture setting
        float[][] textures = {data.textureCoordinates};
        int[] tex_type = {TriangleStripArray.TEXTURE_COORDINATE_2};
        tsa.setTextureCoordinates(tex_type, textures, 1);

        // Setup 4 texture units
        int[] tex_maps = new int[4];

        for(int i=0; i < 4; i++)
            tex_maps[i] = 0;

        tsa.setTextureSetMap(tex_maps,4);

        return(tsa);
    }

    /**
     * Create and return Geometry for a Cone
     *
     * @param xnode The XNode
     * @return A Cone
     */
    private Geometry getCone(XNode xnode) {

        // defaults
        float bottomRadius = 1;
        float height = 2;
        boolean bottom = true;
        boolean side = true;
        boolean solid = true;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("bottomRadius")) {
                bottomRadius = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("height")) {
                height = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("bottom")) {
                bottom = (Boolean)xnode.getFieldData(field_name);
            } else if (field_name.equals("side")) {
                side = (Boolean)xnode.getFieldData(field_name);
            } else if (field_name.equals("solid")) {
                solid = (Boolean)xnode.getFieldData(field_name);
            }
        }

        ConeGenerator generator = new ConeGenerator(
            height,
            bottomRadius,
            24,
            side,
            bottom);

        GeometryData data = new GeometryData();
        data.geometryType = GeometryData.TRIANGLES;
        data.geometryComponents = GeometryData.NORMAL_DATA |
                                  GeometryData.TEXTURE_2D_DATA;

        generator.generate(data);

        TriangleArray ta = new TriangleArray(
            false,
            VertexGeometry.VBO_HINT_STATIC);

        ta.setVertices(
            TriangleArray.COORDINATE_3,
            data.coordinates,
            data.vertexCount);

        ta.setNormals(data.normals);

        // Make an array of objects for the texture setting
        float[][] textures = {data.textureCoordinates};
        int[] tex_type = {TriangleArray.TEXTURE_COORDINATE_2};
        ta.setTextureCoordinates(tex_type, textures, 1);

        // Setup 4 texture units
        int[] tex_maps = new int[4];

        for(int i = 0; i < 4; i++) {
            tex_maps[i] = 0;
        }

        return(ta);
    }

    /**
     * Create and return Geometry for a Cylinder
     *
     * @param xnode The XNode
     * @return A Cylinder
     */
    private Geometry getCylinder(XNode xnode) {

        // defaults
        float radius = 1;
        float height = 2;
        boolean bottom = true;
        boolean side = true;
        boolean top = true;
        boolean solid = true;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("radius")) {
                radius = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("height")) {
                height = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("bottom")) {
                bottom = (Boolean)xnode.getFieldData(field_name);
            } else if (field_name.equals("side")) {
                side = (Boolean)xnode.getFieldData(field_name);
            } else if (field_name.equals("top")) {
                top = (Boolean)xnode.getFieldData(field_name);
            } else if (field_name.equals("solid")) {
                solid = (Boolean)xnode.getFieldData(field_name);
            }
        }

        if (top == false && bottom == false && side == false) {
            return(null);
        }

        CylinderGenerator generator = new CylinderGenerator(
            height,
            radius,
            top,
            bottom,
            side);

        GeometryData data = new GeometryData();
        data.geometryType = GeometryData.TRIANGLE_STRIPS;
        data.geometryComponents = GeometryData.NORMAL_DATA |
                                  GeometryData.TEXTURE_2D_DATA;

        generator.generate(data);

        TriangleStripArray tsa = new TriangleStripArray(
            false,
            VertexGeometry.VBO_HINT_STATIC);

        tsa.setVertices(
            TriangleStripArray.COORDINATE_3,
            data.coordinates,
            data.vertexCount);

        tsa.setStripCount(data.stripCounts, data.numStrips);
        tsa.setNormals(data.normals);

        // Make an array of objects for the texture setting
        float[][] textures = {data.textureCoordinates};
        int[] tex_type = {TriangleStripArray.TEXTURE_COORDINATE_2};

        tsa.setTextureCoordinates(tex_type, textures, 1);

        // Setup 4 texture units
        int[] tex_maps = new int[4];

        for(int i=0; i < 4; i++)
            tex_maps[i] = 0;

        tsa.setTextureSetMap(tex_maps,4);

        return(tsa);
    }

    /**
     * Create and return Geometry for a Sphere
     *
     * @param xnode The XNode
     * @return A Sphere
     */
    private Geometry getSphere(XNode xnode) {

        // defaults
        float radius = 1;
        boolean solid = true;

        String[] fields = xnode.getUsedFieldNames();
        for (int i = 0; i < fields.length; i++) {
            String field_name = fields[i];
            if (field_name.equals("radius")) {
                radius = (Float)xnode.getFieldData(field_name);
            } else if (field_name.equals("solid")) {
                solid = (Boolean)xnode.getFieldData(field_name);
            }
        }

        SphereGenerator generator = new SphereGenerator(radius, 32);
        GeometryData data = new GeometryData();

        data.geometryType = GeometryData.TRIANGLE_STRIPS;
        data.geometryComponents = GeometryData.NORMAL_DATA |
                                  GeometryData.TEXTURE_2D_DATA;

        generator.generate(data);

        TriangleStripArray tsa = new TriangleStripArray(
            false,
            VertexGeometry.VBO_HINT_STATIC);

        tsa.setVertices(
            TriangleStripArray.COORDINATE_3,
            data.coordinates,
            data.vertexCount);

        tsa.setStripCount(data.stripCounts, data.numStrips);
        tsa.setNormals(data.normals);

        // Make an array of objects for the texture setting
        float[][] textures = {data.textureCoordinates};
        int[] tex_type = {TriangleStripArray.TEXTURE_COORDINATE_2};
        tsa.setTextureCoordinates(tex_type, textures, 1);

        // Setup 4 texture units
        int[] tex_maps = new int[4];

        for(int i=0; i < 4; i++)
            tex_maps[i] = 0;

        tsa.setTextureSetMap(tex_maps, 4);

        return(tsa);
    }

    /**
     * Create and return Geometry for a PointSet
     *
     * @param xnode The XNode
     * @param The emissive color from the geometry's material.
     * @return A Geometry node
     */
    private Geometry getPointSet(XNode xnode, float[] emissiveColor) {

        PointArray pa = new PointArray();
        XNode coordinate_xnode = (XNode)xnode.getFieldData("coord");
        if (coordinate_xnode != null) {
            float[] vertices = (float[])coordinate_xnode.getFieldData("point");
            int num_vertices = vertices.length / 3;
            if (vertices != null) {
                pa.setVertices(PointArray.COORDINATE_3, vertices, num_vertices);
            }
        }
        XNode color_xnode = (XNode)xnode.getFieldData("color");
        if (color_xnode != null) {
            boolean has_alpha = false;
            if (color_xnode.getNodeName().equals("ColorRGBA")) {
                has_alpha = true;
            }
            float[] colors = (float[])color_xnode.getFieldData("color");
            if (colors != null) {
                pa.setColors(has_alpha, colors);
            }
        } else {
            pa.setSingleColor(false, emissiveColor);
        }
        return(pa);
    }

    /**
     * Create and return Geometry for a LineSet
     *
     * @param xnode The XNode
     * @param The emissive color from the geometry's material.
     * @return A Geometry
     */
    private Geometry getLineSet(XNode xnode, float[] emissiveColor) {

        LineStripArray lsa = new LineStripArray();
        XNode coordinate_xnode = (XNode)xnode.getFieldData("coord");
        if (coordinate_xnode != null) {
            float[] vertices = (float[])coordinate_xnode.getFieldData("point");
            int num_vertices = vertices.length / 3;
            if (vertices != null) {
                lsa.setVertices(LineStripArray.COORDINATE_3, vertices, num_vertices);
            }
        }
        int[] vertexCount = (int[])xnode.getFieldData("vertexCount");
        if (vertexCount != null) {
            lsa.setStripCount(vertexCount, vertexCount.length);
        }
        XNode color_xnode = (XNode)xnode.getFieldData("color");
        if (color_xnode != null) {
            boolean has_alpha = false;
            if (color_xnode.getNodeName().equals("ColorRGBA")) {
                has_alpha = true;
            }
            float[] colors = (float[])color_xnode.getFieldData("color");
            if (colors != null) {
                lsa.setColors(has_alpha, colors);
            }
        } else {
            lsa.setSingleColor(false, emissiveColor);
        }
        return(lsa);
    }

    /**
     * Create and return Geometry for an IndexedFaceSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getIndexedFaceSet(XNode xnode) {

        // defaults
        float[] coord = null;
        int[] coordIndex = null;

        XNode coordinate_xnode = (XNode)xnode.getFieldData("coord");
        coord = getCoords(coordinate_xnode);
        coordIndex = (int[])xnode.getFieldData("coordIndex");

        TriangleArray ta = null;

        if (coord.length > VBO_MIN_VERTICES) {
            ta = new TriangleArray(true, VertexGeometry.VBO_HINT_STATIC);
        } else {
            ta = new TriangleArray();
        }


        if ((coord != null) && (coordIndex != null)) {

            // more defaults
            boolean ccw = true;
            boolean colorPerVertex = true;
            boolean convex = true;
            boolean normalPerVertex = true;
            boolean solid = true;
            float creaseAngle = 0;
            float[] color = null;
            float[] normal = null;
            float[] texCoord = null;
            int[] colorIndex = null;
            int[] normalIndex = null;
            int[] texCoordIndex = null;

            boolean color_has_alpha = false;

            String[] fields = xnode.getUsedFieldNames();
            for (int i = 0; i < fields.length; i++) {
                String field_name = fields[i];
                if (field_name.equals("color")) {
                    XNode color_xnode = (XNode)xnode.getFieldData(field_name);
                    if (color_xnode != null) {
                        if (color_xnode.getNodeName().equals("ColorRGBA")) {
                            color_has_alpha = true;
                        }
                        color = (float[])color_xnode.getFieldData("color");
                    }
                } else if (field_name.equals("normal")) {
                    XNode normal_xnode = (XNode)xnode.getFieldData(field_name);
                    normal = getNormals(normal_xnode);
                } else if (field_name.equals("texCoord")) {
                    XNode texCoord_xnode = (XNode)xnode.getFieldData(field_name);
                    texCoord = getTexCoords(texCoord_xnode);
                } else if (field_name.equals("creaseAngle")) {
                    creaseAngle = (Float)xnode.getFieldData(field_name);
                } else if (field_name.equals("colorIndex")) {
                    colorIndex = (int[])xnode.getFieldData(field_name);
                } else if (field_name.equals("normalIndex")) {
                    normalIndex = (int[])xnode.getFieldData(field_name);
                } else if (field_name.equals("texCoordIndex")) {
                    texCoordIndex = (int[])xnode.getFieldData(field_name);
                } else if (field_name.equals("ccw")) {
                    ccw = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("colorPerVertex")) {
                    colorPerVertex = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("normalPerVertex")) {
                    normalPerVertex = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("convex")) {
                    convex = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("solid")) {
                    solid = (Boolean)xnode.getFieldData(field_name);
                }
            }

            int numColorComponents = color_has_alpha ? 4 : 3;

            GeometryUtils gutils = new GeometryUtils();
            gutils.generateTriangleArrays(
                true, true,
                coord, color, numColorComponents, normal, texCoord,
                coordIndex, coordIndex.length, colorIndex, normalIndex,
                texCoordIndex, ccw, convex, colorPerVertex, normalPerVertex,
                creaseAngle);

            GeometryData geomData = gutils.geomData;

            ta.setVertices(
                TriangleArray.COORDINATE_3,
                geomData.coordinates,
                geomData.vertexCount);

            ta.setColors(color_has_alpha, geomData.colors);
            ta.setNormals(geomData.normals);

            ///////////////////////////////////////////////////////////
            // hard coding texture params for now

            int numTexSets = 1;
            int numUniqueTexSets = 1;

            int[] texSetMap = new int[numTexSets];
            texSetMap[0] = 0;

            int[] texTypes = new int[numUniqueTexSets];
            texTypes[0] = VertexGeometry.TEXTURE_COORDINATE_2;

            ta.setTextureCoordinates(
                texTypes,
                new float[][]{geomData.textureCoordinates},
                numUniqueTexSets);
            ta.setTextureSetMap(texSetMap, numTexSets);
			
			if (attributeArraysSupported) {
				// rem: not sure that this will have the desired effect....
				int num_vertex = geomData.vertexCount;
				int num_tri = num_vertex / 3;
				float[] tangents = new float[num_vertex * 4];
				
				TriangleUtils.createTangents(num_tri,
					geomData.coordinates,
					geomData.normals,
					geomData.textureCoordinates,
					tangents);
				
				ta.setAttributes(TANGENT_ATTRIB_IDX, 4, tangents, false);
			}
            ///////////////////////////////////////////////////////////
        }
        return(ta);
    }

    /**
     * Create and return Geometry for an IndexedLineSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getIndexedLineSet(XNode xnode) {
        // defaults
        float[] coord = null;
        int[] coordIndex = null;

        XNode coordinate_xnode = (XNode)xnode.getFieldData("coord");
        coord = getCoords(coordinate_xnode);
        coordIndex = (int[])xnode.getFieldData("coordIndex");

        LineStripArray lsa = null;

        if (coord.length > VBO_MIN_VERTICES) {
            lsa = new LineStripArray(true, VertexGeometry.VBO_HINT_STATIC);
        } else {
            lsa = new LineStripArray();
        }


        if ((coord != null) && (coordIndex != null)) {

            // more defaults
            boolean colorPerVertex = true;
            float creaseAngle = 0;
            float[] color = null;
            int[] colorIndex = null;

            boolean color_has_alpha = false;

            String[] fields = xnode.getUsedFieldNames();
            for (int i = 0; i < fields.length; i++) {
                String field_name = fields[i];
                if (field_name.equals("color")) {
                    XNode color_xnode = (XNode)xnode.getFieldData(field_name);
                    if (color_xnode != null) {
                        if (color_xnode.getNodeName().equals("ColorRGBA")) {
                            color_has_alpha = true;
                        }
                        color = (float[])color_xnode.getFieldData("color");
                    }
                } else if (field_name.equals("colorIndex")) {
                    colorIndex = (int[])xnode.getFieldData(field_name);
                } else if (field_name.equals("colorPerVertex")) {
                    colorPerVertex = (Boolean)xnode.getFieldData(field_name);
                }
            }

            int numColorComponents = color_has_alpha ? 4 : 3;

            int num_strips = 0;

            for(int i = 0; i < coordIndex.length; i++) {
                if(coordIndex[i] == -1)
                    num_strips++;
            }

            int num_flat_coords = coordIndex.length - num_strips;

            if((coordIndex.length != 0) && (coordIndex[coordIndex.length - 1] != -1))
                num_strips++;

            int[] stripCounts = new int[coordIndex.length];

            float[] lfCoords = new float[num_flat_coords * 3];

            int strip_idx = 0;
            int vtx_idx = 0;

            for(int i = 0; i < coordIndex.length; i++) {
                if(coordIndex[i] == -1) {
                    // Hide a separate comparison here so that we only
                    // increment when the previous count was not 0. This saves
                    // putting in zero-length strips, which aviatrix does not
                    // like. Zero length strips happen when the user does
                    // something like coordIndex [ 0 1 -1 -1 0 2 ]
                    if(stripCounts[strip_idx] != 0)
                        strip_idx++;
                    else {
                        num_strips--;
                    }
                } else {
                    stripCounts[strip_idx]++;
                    int idx = coordIndex[i] * 3;
                    lfCoords[vtx_idx++] = coord[idx++];
                    lfCoords[vtx_idx++] = coord[idx++];
                    lfCoords[vtx_idx++] = coord[idx];
                }
            }

            int numCoords = vtx_idx;
            int numStripCounts = num_strips;

            lsa.setVertices(LineStripArray.COORDINATE_3,
                                 lfCoords,
                                 numCoords / 3);

            lsa.setStripCount(stripCounts, numStripCounts);

            if (color != null) {
                int num_flat_colors = coordIndex.length - numStripCounts + 1;

                num_flat_colors *= (color_has_alpha ? 4 : 3);

                float[] lfColors = new float[num_flat_colors];

                lfColors = new float[num_flat_colors];

                vtx_idx = 0;

                if(colorPerVertex) {
                    if(colorIndex.length == 0) {
                        if(!color_has_alpha) {
                            for(int i = 0; i < coordIndex.length; i++) {
                                if(coordIndex[i] == -1)
                                    continue;

                                int idx = coordIndex[i] * 3;
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx];
                            }
                        } else {
                            for(int i = 0; i < coordIndex.length; i++) {
                                if(coordIndex[i] == -1)
                                    continue;

                                int idx = coordIndex[i] * 4;
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx];
                            }
                        }
                    } else {
                        if(!color_has_alpha) {
                            for(int i = 0; i < colorIndex.length; i++) {
                                if(colorIndex[i] == -1)
                                    continue;

                                int idx = colorIndex[i] * 3;
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx];
                            }
                        } else {
                            for(int i = 0; i < colorIndex.length; i++) {
                                if(colorIndex[i] == -1)
                                    continue;

                                int idx = colorIndex[i] * 4;
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx++];
                                lfColors[vtx_idx++] = color[idx];
                            }
                        }
                    }
                } else {
                    if(colorIndex.length == 0) {
                        // Colour per vertex is false, and there are no
                        // colour indices, so just copy the same colour value to
                        // each index until we get to the end of this line, then
                        // move to the next colour.
                        if(!color_has_alpha) {
                            int idx = 0;
                            for(int i = 0; i < coordIndex.length; i++) {
                                if(coordIndex[i] == -1) {
                                    idx += 3;
                                    continue;
                                }

                                lfColors[vtx_idx++] = color[idx];
                                lfColors[vtx_idx++] = color[idx + 1];
                                lfColors[vtx_idx++] = color[idx + 2];
                            }
                        } else {
                            int idx = 0;

                            for(int i = 0; i < coordIndex.length; i++) {
                                if(coordIndex[i] == -1) {
                                    idx += 4;
                                    continue;
                                }

                                lfColors[vtx_idx++] = color[idx];
                                lfColors[vtx_idx++] = color[idx + 1];
                                lfColors[vtx_idx++] = color[idx + 2];
                                lfColors[vtx_idx++] = color[idx + 3];
                            }
                        }
                    } else {
                        if(!color_has_alpha) {
                            // Colour per vertex is false, one colour is used
                            // for each polyline of the IndexedLineSet

                            int idxx = 0;
                            int idx = 0;
                            for(int i = 0; i < coordIndex.length; i++) {
                                if(coordIndex[i] == -1){
                                    // next polyline -> next indexcolor
                                    idxx ++;
                                    continue;
                                }
                                idx = colorIndex[idxx] * 3;

                                lfColors[vtx_idx++] = color[idx];
                                lfColors[vtx_idx++] = color[idx+1];
                                lfColors[vtx_idx++] = color[idx+2];
                             }
                        } else {
                            int idxx = 0;
                            int idx = 0;
                            for(int i = 0; i < coordIndex.length; i++) {
                                if(coordIndex[i] == -1){
                                    // next polyline -> next indexcolor
                                    idxx ++;
                                    continue;
                                }
                                idx = colorIndex[idxx] * 4;

                                lfColors[vtx_idx++] = color[idx];
                                lfColors[vtx_idx++] = color[idx+1];
                                lfColors[vtx_idx++] = color[idx+2];
                                lfColors[vtx_idx++] = color[idx+3];
                             }
                        }
                    }
                }

                lsa.setColors(color_has_alpha, lfColors);
            }
        }

        return(lsa);
    }

    /**
     * Create and return Geometry for an IndexedTriangleSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getIndexedTriangleSet(XNode xnode) {

        Geometry geom = null;

        // defaults
        float[] coord = null;
        int[] index = null;

        XNode coordinate_xnode = (XNode)xnode.getFieldData("coord");
        coord = getCoords(coordinate_xnode);
        index = (int[])xnode.getFieldData("index");

        if ((coord != null) && (index != null)) {

            // more defaults
            boolean ccw = true;
            boolean colorPerVertex = true;
            boolean normalPerVertex = true;
            boolean solid = true;
            float[] color = null;
            float[] normal = null;
            float[] texCoord = null;

            boolean color_has_alpha = false;

            String[] fields = xnode.getUsedFieldNames();
            for (int i = 0; i < fields.length; i++) {
                String field_name = fields[i];
                if (field_name.equals("color")) {
                    XNode color_xnode = (XNode)xnode.getFieldData(field_name);
                    if (color_xnode != null) {
                        if (color_xnode.getNodeName().equals("ColorRGBA")) {
                            color_has_alpha = true;
                        }
                        color = (float[])color_xnode.getFieldData("color");
                    }
                } else if (field_name.equals("normal")) {
                    XNode normal_xnode = (XNode)xnode.getFieldData(field_name);
                    normal = getNormals(normal_xnode);
                } else if (field_name.equals("texCoord")) {
                    XNode texCoord_xnode = (XNode)xnode.getFieldData(field_name);
                    texCoord = getTexCoords(texCoord_xnode);
                } else if (field_name.equals("ccw")) {
                    ccw = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("colorPerVertex")) {
                    colorPerVertex = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("normalPerVertex")) {
                    normalPerVertex = (Boolean)xnode.getFieldData(field_name);
                } else if (field_name.equals("solid")) {
                    solid = (Boolean)xnode.getFieldData(field_name);
                }
            }

            if (normal != null ) {

                IndexedTriangleArray ita = null;

                if (coord.length > VBO_MIN_VERTICES) {
                    ita = new IndexedTriangleArray(true, VertexGeometry.VBO_HINT_STATIC);
                } else {
                    ita = new IndexedTriangleArray();
                }

                ita.setVertices(
                    IndexedTriangleArray.COORDINATE_3,
                    coord,
                    coord.length / 3);

                ita.setIndices(index, index.length);

                ita.setColors(color_has_alpha, color);
                ita.setNormals(normal);

                ///////////////////////////////////////////////////////////
                // hard coding texture params for now

                int numTexSets = 1;
                int numUniqueTexSets = 1;

                int[] texSetMap = new int[numTexSets];
                texSetMap[0] = 0;

                int[] texTypes = new int[numUniqueTexSets];
                texTypes[0] = VertexGeometry.TEXTURE_COORDINATE_2;

                ita.setTextureCoordinates(
                    texTypes,
                    new float[][]{texCoord},
                    numUniqueTexSets);
                ita.setTextureSetMap(texSetMap, numTexSets);
                ///////////////////////////////////////////////////////////

				if (attributeArraysSupported) {
					float[] tangents = new float[coord.length * 4 / 3];
					
					TriangleUtils.createTangents(index.length / 3,
						index,
						coord,
						normal,
						texCoord,
						tangents);
					
					ita.setAttributes(TANGENT_ATTRIB_IDX, 4, tangents, false);
				}
				
                geom = ita;

            } else {

                float creaseAngle;
                if (normalPerVertex) {
                    creaseAngle = (float)Math.PI;
                } else {
                    creaseAngle = 0;
                }

                int numIndex = index.length;
                int numTriangles = numIndex / 3;
                int size = numIndex + numTriangles;

                TriangleArray ta = null;

                if (numIndex > VBO_MIN_VERTICES) {
                    ta = new TriangleArray(true, VertexGeometry.VBO_HINT_STATIC);
                } else {
                    ta = new TriangleArray();
                }

                int[] newIndex =  new int[size];

                int idx = 0;
                int nidx = 0;
                while (idx < numIndex - 1) {
                    newIndex[nidx++] = index[idx++];
                    newIndex[nidx++] = index[idx++];
                    newIndex[nidx++] = index[idx++];
                    newIndex[nidx++] = -1;
                }
                index = newIndex;

                int numColorComponents = color_has_alpha ? 4 : 3;

                GeometryUtils gutils = new GeometryUtils();
                gutils.generateTriangleArrays(
                    true, true,
                    coord, color, numColorComponents, normal, texCoord,
                    index, index.length, index, index,
                    index, ccw, true, colorPerVertex, normalPerVertex,
                    creaseAngle);

                GeometryData geomData = gutils.geomData;

                ta.setVertices(
                    TriangleArray.COORDINATE_3,
                    geomData.coordinates,
                    geomData.vertexCount);

                ta.setColors(color_has_alpha, geomData.colors);
                ta.setNormals(geomData.normals);

                ///////////////////////////////////////////////////////////
                // hard coding texture params for now

                int numTexSets = 1;
                int numUniqueTexSets = 1;

                int[] texSetMap = new int[numTexSets];
                texSetMap[0] = 0;

                int[] texTypes = new int[numUniqueTexSets];
                texTypes[0] = VertexGeometry.TEXTURE_COORDINATE_2;

                ta.setTextureCoordinates(
                    texTypes,
                    new float[][]{geomData.textureCoordinates},
                    numUniqueTexSets);
                ta.setTextureSetMap(texSetMap, numTexSets);
                ///////////////////////////////////////////////////////////
				
				if (attributeArraysSupported) {
					// rem: not sure that this will have the desired effect....
					int num_vertex = geomData.vertexCount;
					int num_tri = num_vertex / 3;
					float[] tangents = new float[num_vertex * 4];
					
					TriangleUtils.createTangents(num_tri,
						geomData.coordinates,
						geomData.normals,
						geomData.textureCoordinates,
						tangents);
					
					ta.setAttributes(TANGENT_ATTRIB_IDX, 4, tangents, false);
				}
				
                geom = ta;
            }
        } else {
            geom = new IndexedTriangleArray();
        }
        return(geom);
    }

    /**
     * Create and return Geometry for an IndexedTriangleFanSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getIndexedTriangleFanSet(XNode xnode) {
        return(null);
    }

    /**
     * Create and return Geometry for an IndexedTriangleStripSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getIndexedTriangleStripSet(XNode xnode) {
        return(null);
    }

    /**
     * Create and return Geometry for an TriangleSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getTriangleSet(XNode xnode) {
        return(null);
    }

    /**
     * Create and return Geometry for an TriangleFanSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getTriangleFanSet(XNode xnode) {
        return(null);
    }

    /**
     * Create and return Geometry for an TriangleStripSet
     *
     * @param xnode The XNode
     * @return A Geometry
     */
    private Geometry getTriangleStripSet(XNode xnode) {
        return(null);
    }

    /**
     * Return the array of coordinate values
     *
     * @param coord_xnode The coordinate XNode
     * @return The array of coord values, or null if none exist
     */
    private float[] getCoords(XNode coord_xnode) {
        float[] coords = null;
        if (coord_xnode != null) {
            coords = (float[])coord_xnode.getFieldData("point");
        }
        return(coords);
    }

    /**
     * Return the array of normal values
     *
     * @param normal_xnode The normal XNode
     * @return The array of normal values, or null if none exist
     */
    private float[] getNormals(XNode normal_xnode) {
        float[] normals = null;
        if (normal_xnode != null) {
            normals = (float[])normal_xnode.getFieldData("vector");
        }
        return(normals);
    }

    /**
     * Return the array of texture coordinate values
     *
     * @param texCoord_xnode The texCoord XNode
     * @return The array of texCoord values, or null if none exist
     */
    private float[] getTexCoords(XNode texCoord_xnode) {
        float[] texCoords = null;
        if (texCoord_xnode != null) {
            texCoords = (float[])texCoord_xnode.getFieldData("point");
        }
        return(texCoords);
    }
}
