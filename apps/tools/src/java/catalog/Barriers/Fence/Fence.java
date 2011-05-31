package catalog.Barriers.Fence;

/**
 * A fence generator.
 *
 * Groups the fence segments into 1 shape.  Right now combines n fence segments
 * into one shape.  Later might change based on the angle to avoid transparency issues.
 *
 * Creating a LOD would be good as well
 *
 * @author Alan Hudson
 */
import java.util.Map;

import org.web3d.x3d.sai.*;


public class Fence
    implements X3DScriptImplementation, X3DFieldEventListener {

    private static final boolean CREATE_PANELS = true;
    private static final boolean CREATE_POSTS = true;

    // Input Fields
    private SFInt32 fencesPerShapeField;
    private MFVec3f startField;
    private MFVec3f endField;
    private MFFloat panelHeightField;
    private MFFloat panelTopHeightField;
    private MFInt32 postField;
    private MFInt32 postTopField;
    private MFVec3f postSizeField;
    private MFVec3f postTopSizeField;
    private MFNode postGeometryField;
    private MFNode postTopGeometryField;

    private SFInt32 panelAppearanceField;
    private SFInt32 panelTopAppearanceField;
    private SFInt32 panelRailAppearanceField;
    private SFInt32 postAppearanceField;
    private SFInt32 postTopAppearanceField;
    private MFNode appearanceField;

    // Output Fields
    private MFNode childrenField;

    private Browser browser;

    public Fence() {
    }

    //----------------------------------------------------------
    // Methods defined by X3DScriptImplementation
    //----------------------------------------------------------

    private void foo() {

    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public void setFields(X3DScriptNode externalView, Map fields) {
        fencesPerShapeField = (SFInt32) fields.get("fencesPerShape");
        startField = (MFVec3f)fields.get("start");
        endField = (MFVec3f) fields.get("end");
        panelHeightField = (MFFloat) fields.get("panelHeight");
        panelTopHeightField = (MFFloat) fields.get("panelTopHeight");
        postField = (MFInt32) fields.get("postType");
        postTopField = (MFInt32) fields.get("postTopType");
        postSizeField = (MFVec3f) fields.get("postSize");
        postTopSizeField = (MFVec3f) fields.get("postTopSize");
        postGeometryField = (MFNode) fields.get("postGeometry");
        postTopGeometryField = (MFNode) fields.get("postTopGeometry");

        panelAppearanceField = (SFInt32) fields.get("panelAppearance");
        panelTopAppearanceField = (SFInt32) fields.get("panelTopAppearance");
        panelRailAppearanceField = (SFInt32) fields.get("panelRailAppearance");
        postAppearanceField = (SFInt32) fields.get("postAppearance");
        postTopAppearanceField = (SFInt32) fields.get("postTopAppearance");
        appearanceField = (MFNode) fields.get("appearance");

        childrenField = (MFNode) fields.get("children");
    }

    public void initialize() {
        //System.out.println("Creating Fence geometry");
        int num = 0;
        int len = startField.getSize();
        float[] start = new float[3];
        float[] end = new float[3];
        float height;

        // One shape for a panel, panelTop and post, post Top
        int shape_groups = 4;

        int num_panels = len;
        int num_panels_per_shape = fencesPerShapeField.getValue();
        int num_shapes = (num_panels / num_panels_per_shape) * shape_groups;
        int num_posts_per_shape = num_panels_per_shape;

        //System.out.println("Number of posts: " + len);
        //System.out.println("Number of panels: " + num_panels);
        //System.out.println("Number of shapes: " + num_shapes);
        //System.out.println("Number of panels per shape: " + num_panels_per_shape);
        //System.out.println("Number of posts per shape: " + num_posts_per_shape);

        X3DNode[] shapes = new X3DNode[num_shapes];

        X3DExecutionContext mainScene = browser.getExecutionContext();


        // Create a common Appearance to share

        X3DNode[] appearances = new X3DNode[appearanceField.getSize()];
        appearanceField.getValue(appearances);

        X3DNode panel_appearance = appearances[panelAppearanceField.getValue()];
        X3DNode panelTop_appearance = appearances[panelTopAppearanceField.getValue()];
        X3DNode panelRail_appearance = appearances[panelRailAppearanceField.getValue()];
        X3DNode post_appearance = appearances[postAppearanceField.getValue()];
        X3DNode postTop_appearance = appearances[postTopAppearanceField.getValue()];

        int shape_idx = 0;

        // Create Panels

        X3DNode[] new_shapes = createSegmentGeom(mainScene, panel_appearance,
            postField, postGeometryField, panelHeightField, startField, endField,
            false, null, len, (num_shapes / shape_groups), num_posts_per_shape, true, true, 1, 1);

        for(int i=0; i < new_shapes.length; i++) {
            shapes[shape_idx++] = new_shapes[i];
        }

        // Create PanelTops
        new_shapes = createSegmentGeom(mainScene, panelTop_appearance,
            postField, postGeometryField, panelTopHeightField, startField, endField,
            true, panelHeightField, len, (num_shapes / shape_groups), num_posts_per_shape, true, true, 15 ,10);

        for(int i=0; i < new_shapes.length; i++) {
            X3DNode panel_shape = mainScene.createNode("Shape");
            shapes[shape_idx++] = new_shapes[i];
        }

        // Create Posts
        new_shapes = createVertexGeom(mainScene, post_appearance,
            postField, postGeometryField, postSizeField, startField, endField,
            false, null, (num_shapes / shape_groups), num_posts_per_shape);

        for(int i=0; i < new_shapes.length; i++) {
            X3DNode panel_shape = mainScene.createNode("Shape");
            shapes[shape_idx++] = new_shapes[i];
        }


        // Create PostTops
        new_shapes = createVertexGeom(mainScene, postTop_appearance,
            postTopField, postTopGeometryField, postTopSizeField, startField, endField,
            true, postSizeField, (num_shapes / shape_groups), num_posts_per_shape);

        for(int i=0; i < new_shapes.length; i++) {
            shapes[shape_idx++] = new_shapes[i];
        }

        childrenField.setValue(shapes.length, shapes);

        //System.out.println("Done creating fence geometry");
    }

    public void eventsProcessed() {
    }

    public void shutdown() {
    }

    //----------------------------------------------------------
    // Methods defined by X3DFieldEventListener
    //----------------------------------------------------------

    public void readableFieldChanged(X3DFieldEvent evt) {
    }

    /**
     * Create geometry for horizontal segments
     *
     * @param offset Should we offset vertically
     * @param offsetField A full SFVec3f that we only use the height off for offset
     */
    private X3DNode[] createSegmentGeom(X3DExecutionContext mainScene, X3DNode appearance, MFInt32 geomIndicesField, MFNode geometryField,
        MFFloat heightField, MFVec3f startField, MFVec3f endField, boolean offset,
        MFFloat offsetField, int vertices, int shapesToCreate, int panelsPerShape,
        boolean sRepeatPerMeter, boolean tRepeatPerMeter, float sMeterMult, float tMeterMult) {

        float[] post_coord;
        float[] post_normal;
        float[] post_texCoord;
        int[] post_index;
        X3DNode[] ret_val = new X3DNode[shapesToCreate];
        int shape_idx = 0;
        float[] start = new float[3];
        float[] end = new float[3];
        int num = 0;

        float sRepeat;
        float tRepeat;

        for(int n=0; n < shapesToCreate; n++) {

            int[] index = new int[2 * 3 * vertices];
            float[] point = new float[4 * 3 * vertices];
            float[] tc = new float[4 * 2 * vertices];
            int iidx = 0;
            int pidx = 0;
            int tidx = 0;
            float height;
            float offsetVal = 0;

            // Panel Shape
            X3DNode panel_shape = mainScene.createNode("Shape");
            SFNode shape_appearance = (SFNode) (panel_shape.getField("appearance"));
            shape_appearance.setValue(appearance);

            SFNode shape_geometry = (SFNode) (panel_shape.getField("geometry"));
            X3DNode its = mainScene.createNode("IndexedTriangleSet");
            SFBool its_solid = (SFBool) its.getField("solid");
            its_solid.setValue(false);

            X3DNode coord = mainScene.createNode("Coordinate");
            SFNode its_coord = (SFNode) (its.getField("coord"));
            MFVec3f point_field = (MFVec3f) coord.getField("point");
            X3DNode normal = mainScene.createNode("Normal");
            SFNode its_normal = (SFNode) (its.getField("normal"));
            X3DNode texcoord = mainScene.createNode("TextureCoordinate");
            SFNode its_texcoord = (SFNode) (its.getField("texCoord"));
            MFVec2f tcpoint_field = (MFVec2f) texcoord.getField("point");
            MFInt32 its_index = (MFInt32) (its.getField("index"));

            for(int i=0; i < panelsPerShape; i++) {

                startField.get1Value(num, start);
                endField.get1Value(num,end);
                height = heightField.get1Value(num);

                if ( sRepeatPerMeter ) {
                    // calculate the length of the panel from start to end
                    float x_delta = end[0] - start[0];
                    float y_delta = end[1] - start[1];
                    float z_delta = end[2] - start[2];
                    float length = (float)Math.sqrt( x_delta*x_delta + y_delta*y_delta + z_delta*z_delta );
                    // the s repeat factor is the length, in meters
                    sRepeat = length * sMeterMult;
                } else {
                    // the s repeat factor is 1 per panel
                    sRepeat = 1.0f;
                }

                if ( tRepeatPerMeter ) {
                    // the t repeat factor is the height, in meters
                    tRepeat = height * tMeterMult;
                } else {
                    // the t repeat factor is 1 per panel
                    tRepeat = 1.0f;
                }
                //System.out.println( "sRepeat = "+ sRepeat +", tRepeat = "+ tRepeat );
                if (offset) {
                    offsetVal = offsetField.get1Value(num);
                }

                num++;

                index[iidx++] = i*4;
                index[iidx++] = i*4 + 1;
                index[iidx++] = i*4 + 2;
                index[iidx++] = i*4 + 1;
                index[iidx++] = i*4 + 3;
                index[iidx++] = i*4 + 2;

                point[pidx++] = start[0];
                point[pidx++] = offsetVal + height + start[1];
                point[pidx++] = start[2];

                point[pidx++] = start[0];
                point[pidx++] = offsetVal + 0 + start[1];
                point[pidx++] = start[2];

                point[pidx++] = end[0];
                point[pidx++] = offsetVal + height + end[1];
                point[pidx++] = end[2];

                point[pidx++] = end[0];
                point[pidx++] = offsetVal + 0 + end[1];
                point[pidx++] = end[2];

                tc[tidx++] = 0;
                tc[tidx++] = 0;
                tc[tidx++] = 0;
                tc[tidx++] = tRepeat;
                tc[tidx++] = sRepeat;
                tc[tidx++] = 0;
                tc[tidx++] = sRepeat;
                tc[tidx++] = tRepeat;
            }

            //System.out.println("Points: " + java.util.Arrays.toString(point));
            //System.out.println("Index: " + java.util.Arrays.toString(index));
            //System.out.println("TexCoord: " + java.util.Arrays.toString(tc));

            if (CREATE_PANELS) {
                point_field.setValue(point.length / 3, point);
                tcpoint_field.setValue(tc.length / 2, tc);
                its_coord.setValue(coord);
                its_texcoord.setValue(texcoord);
                its_index.setValue(index.length, index);

                shape_geometry.setValue(its);
            }

            //System.out.println("Placing panel at: " + shape_idx);

            ret_val[shape_idx++] = panel_shape;
        }

        return ret_val;
    }

    /**
     * Create geometry for vertices.
     *
     * @param offset Should we offset vertically
     * @param offsetField A full SFVec3f that we only use the height off for offset
     */
    private X3DNode[] createVertexGeom(X3DExecutionContext mainScene, X3DNode appearance, MFInt32 geomIndicesField, MFNode geometryField,
        MFVec3f sizeField, MFVec3f startField, MFVec3f endField, boolean offset, MFVec3f offsetField, int shapesToCreate, int verticesPerShape) {

        //System.out.println("**** Creating vertex geom: " + geomIndicesField.getSize() + " shapesToCreate: " + shapesToCreate + " verticesPerShape: " + verticesPerShape);
        int coord_size = 0;
        int index_size = 0;
        int texCoord_size = 0;
        int normal_size = 0;
        int num = 0;
        X3DNode[] ret_val = new X3DNode[shapesToCreate];
        int shape_idx = 0;

        //System.out.println("Vertex Geoms to add: " + shapesToCreate);
        for(int n=0; n < shapesToCreate; n++) {
            coord_size = 0;
            index_size = 0;
            texCoord_size = 0;
            normal_size = 0;

            int numVerticesInShape = verticesPerShape;

            if (n == shapesToCreate -1 ) {
                numVerticesInShape++;
            }


            //System.out.println("NumVertices in Shape: " + numVerticesInShape);
            for(int i=0; i < numVerticesInShape; i++) {
                int idx = geomIndicesField.get1Value(i);

                if (idx > geometryField.getSize() - 1) {
                    System.out.println("Invalid geometry index for vertex");
                    continue;
                }

                X3DNode geom_node = geometryField.get1Value(idx);

                MField coord_field = (MField) geom_node.getField("coord");
                coord_size += coord_field.getSize() * 3;

                MField index_field = (MField) geom_node.getField("index");
                index_size += index_field.getSize();

                MField texCoord_field = (MField) geom_node.getField("texCoord");
                texCoord_size += texCoord_field.getSize() * 2;

                MField normal_field = (MField) geom_node.getField("normal");
                normal_size += texCoord_field.getSize() * 3;

            }
            //System.out.println("Coord size: " + coord_size);


            //System.out.println("Handling vertex shape: " + n);
            //            int[] index = new int[index_size / shapesToCreate];
            //            float[] point = new float[coord_size / shapesToCreate];
            //            float[] tc = new float[texCoord_size / shapesToCreate];

            int[] index = new int[index_size];
            float[] point = new float[coord_size];
            float[] tc = new float[texCoord_size];
            int iidx = 0;
            int pidx = 0;
            int tidx = 0;

            iidx = 0;
            pidx = 0;
            tidx = 0;

            // Post Shape
            X3DNode shape = mainScene.createNode("Shape");
            SFNode shape_appearance = (SFNode) (shape.getField("appearance"));
            shape_appearance.setValue(appearance);

            SFNode shape_geometry = (SFNode) (shape.getField("geometry"));
            X3DNode its = mainScene.createNode("IndexedTriangleSet");
            //SFBool its_solid = (SFBool) its.getField("solid");
            //its_solid.setValue(false);

            X3DNode coord = mainScene.createNode("Coordinate");
            SFNode its_coord = (SFNode) (its.getField("coord"));
            MFVec3f point_field = (MFVec3f) coord.getField("point");
            X3DNode normal = mainScene.createNode("Normal");
            SFNode its_normal = (SFNode) (its.getField("normal"));
            MFVec3f vector_field = (MFVec3f) normal.getField("vector");
            X3DNode texcoord = mainScene.createNode("TextureCoordinate");
            SFNode its_texcoord = (SFNode) (its.getField("texCoord"));
            MFVec2f tcpoint_field = (MFVec2f) texcoord.getField("point");
            MFInt32 its_index = (MFInt32) (its.getField("index"));

            float[] size = new float[3];
            int[] indexes = null;
            float[] coords = null;
            float[] texCoords = null;
            float[] normals = null;
            int currPost = -1;
            int post;
            int lastIdx = 0;
            float[] start = new float[3];
            float[] end = new float[3];
            float[] tmp = new float[3];

            //System.out.println("Vertices to add: " + verticesPerShape);
            // Handle all but the end vertices
            for(int i=0; i < verticesPerShape; i++) {
                post = geomIndicesField.get1Value(num);

                //System.out.println("Getting start: " + num);
                startField.get1Value(num, start);
                //System.out.println("translate post: " + java.util.Arrays.toString(start));
                if (post != currPost) {
                    X3DNode geom_node = geometryField.get1Value(post);

                    MFVec3f coord_field = (MFVec3f) geom_node.getField("coord");
                    MFInt32 index_field = (MFInt32) geom_node.getField("index");
                    MFVec2f texCoord_field = (MFVec2f) geom_node.getField("texCoord");
                    MFVec3f normal_field = (MFVec3f) geom_node.getField("normal");

                    indexes = new int[index_field.getSize()];
                    index_field.getValue(indexes);

                    coords = new float[coord_field.getSize() * 3];
                    coord_field.getValue(coords);
                    //System.out.println("coords: " + coords.length);
                    texCoords = new float[texCoord_field.getSize() * 2];
                    texCoord_field.getValue(texCoords);

                    normals = new float[normal_field.getSize() * 3];
                    normal_field.getValue(normals);

                    currPost = post;
                }

                sizeField.get1Value(num, size);

                int highest_idx = -1;
                int val;

                for (int j=0; j < indexes.length; j++) {
                    val = indexes[j] + lastIdx;
                    index[iidx++] = val;

                    if (val > highest_idx)
                        highest_idx = val;
                }

                lastIdx = highest_idx + 1;
                float offsetVal = 0;

                for (int j=0; j < coords.length / 3; j++) {
                    point[pidx++] = (coords[j*3] * size[0]) + start[0];
                    if (offset) {
                        offsetField.get1Value(num, tmp);
                        offsetVal = tmp[1];
                    }
                    point[pidx++] = ((coords[j*3 + 1] + 0.5f) * size[1]) + start[1] + offsetVal;

                    point[pidx++] = (coords[j*3 + 2] * size[2]) + start[2];
                }
                num++;
            }

            //System.out.println("pidx: " + pidx);
            //System.out.println("indexes: " + java.util.Arrays.toString(index));
            //System.out.println("Points1: " + java.util.Arrays.toString(point));

            if (n == shapesToCreate -1 ) {
                //System.out.println("Handling end vertices");
                // Handle the end post

                if (num > geomIndicesField.getSize() - 1) {
                    System.out.println("Error indexing geometry.  Num: " + num);
                }

                post = geomIndicesField.get1Value(num);

                endField.get1Value(endField.getSize() - 1, end);

                if (post != currPost) {
                    X3DNode geom_node = geometryField.get1Value(post);

                    MFVec3f coord_field = (MFVec3f) geom_node.getField("coord");
                    MFInt32 index_field = (MFInt32) geom_node.getField("index");
                    MFVec2f texCoord_field = (MFVec2f) geom_node.getField("texCoord");
                    MFVec3f normal_field = (MFVec3f) geom_node.getField("normal");

                    indexes = new int[index_field.getSize()];
                    index_field.getValue(indexes);

                    coords = new float[coord_field.getSize() * 3];
                    coord_field.getValue(coords);

                    texCoords = new float[texCoord_field.getSize() * 2];
                    texCoord_field.getValue(texCoords);

                    normals = new float[normal_field.getSize() * 3];
                    normal_field.getValue(normals);

                    currPost = post;
                }

                sizeField.get1Value(num, size);

                int highest_idx = -1;
                int val;
                float offsetVal = 0;

                for (int j=0; j < indexes.length; j++) {
                    val = indexes[j] + lastIdx;
                    index[iidx++] = val;

                    if (val > highest_idx)
                        highest_idx = val;
                }

                lastIdx = highest_idx + 1;

                for (int j=0; j < coords.length / 3; j++) {
                    point[pidx++] = (coords[j*3] * size[0]) + end[0];

                    if (offset) {
                        offsetField.get1Value(num, tmp);
                        offsetVal = tmp[1];
                    }
                    point[pidx++] = ((coords[j*3 + 1] + 0.5f) * size[1]) + end[1] + offsetVal;
                    point[pidx++] = (coords[j*3 + 2] * size[2]) + end[2];
                }

                //System.out.println("pidx: " + pidx);
                //    System.out.println("Points2: " + java.util.Arrays.toString(point));
                //    System.out.println("Index: " + java.util.Arrays.toString(index));

            }

            //System.out.println("TexCoord: " + java.util.Arrays.toString(tc));

            if (CREATE_POSTS) {
                point_field.setValue(point.length / 3, point);
                tcpoint_field.setValue(texCoords.length / 2, texCoords);
                vector_field.setValue(normals.length / 3, normals);
                its_coord.setValue(coord);
                its_texcoord.setValue(texcoord);
                its_index.setValue(index.length, index);

                shape_geometry.setValue(its);
            }

            //System.out.println("Add vertex shape to: " + (shape_idx) + " size: " + ret_val.length);
            ret_val[shape_idx++] = shape;
        }

        return ret_val;
    }
}
