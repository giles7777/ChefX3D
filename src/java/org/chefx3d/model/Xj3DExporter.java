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
package org.chefx3d.model;

// External Imports
import java.io.*;
import java.util.*;
import java.text.NumberFormat;
import org.web3d.vrml.sav.*;
import org.web3d.x3d.sai.*;

// Internal Imports
import org.chefx3d.util.*;

/**
 * Export a world model into one of the formats supported by Xj3D.  This will use
 * Xj3D's SAV interfaces to allow choices of formats and encodings.
 *
 * @author Alan Hudson
 * @version $Revision: 1.48 $
 */
public class Xj3DExporter implements Exporter {
    /** Formats supported by this interface */
    public enum ExternalFormat { VRML, X3D };

    /** Maximum number of digits for floats */
    private static final int MAXIMUM_DIGITS = 4;

    /** Formater for numbers */
    protected NumberFormat nFormater;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;

    /**
     * Constructor.
     *
     * @param format The format to use
     * @param version The spec major and minor version number(such as 3.2)
     * @param profile The profile to use when exporting
     * @param components The components to add to the profile. Null means none.
     * @param levels The components levels
     */
    public Xj3DExporter(
            ExternalFormat format,
            String version,
            String profile,
            String[] components,
            int[] levels) {

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        nFormater = NumberFormat.getNumberInstance();
        nFormater.setMaximumFractionDigits(MAXIMUM_DIGITS);
        nFormater.setGroupingUsed(false);

    }


    /**
     * Output a specific entity to the specified stream.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param mainScene The X3D scene to write to
     */
    public X3DNode export(
            WorldModel model,
            int entityID,
            X3DScene mainScene,
            String worldURL) {

        // not supported
        return null;
    }


    /**
     * Output a specific entity to the specified stream.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param writer The stream to write to
     */
    public void export(
            WorldModel model,
            int entityID,
            Writer handler) {
/*
        Entity e = model.getEntity(entityID);

        if (e == null) {
            errorReporter.errorReport("Cannot find entity", null);
            return;
        }

        try {
            serializeEntity(e, handler);
        } catch(IOException ioe) {
            errorReporter.errorReport("IO Error in export", ioe);
        }
*/
    }

    /**
     * Output the World Model to the specified stream.
     *
     * @param model The world model to export
     * @param fw The stream to write to
     */
    public void export(WorldModel model, Writer fw) {
        // not implemented
    }

    /**
     * Output a specific entity to the specified file.
     *
     * @param model The world model to export
     * @param name The entity to export
     * @param file - The file to write to
     */
    public void export(WorldModel model, String name, File file) {
        Entity[] entities = ((BaseWorldModel) model).getModelData();
/*
        CDFFilter filter = new CDFFilter();
        ContentHandler chainHandler = filter.setupFilterChain(new String[] {"Identity"},
            file.toString(), new String[] {});

        int len = entities.length;

        try {
            for (int i = 0; i < len; i++) {
                Entity entity = entities[i];

                if (entity == null) {
                    // Its expected we will have gaps
                    continue;
                }

                serializeEntity(entity, chainHandler);
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
*/
    }

    /**
     * Export the Java class objects to the file system.
     *
     * @param entity The entity to write
     */
    private void serializeEntity(Entity e, ContentHandler handler) throws IOException {
System.out.println("Serialize entity: " + e);
        switch (e.getType()) {

            // This is where it all begins, the design. This is of type SceneEntity,
            // and serves as the root parent for everything.
            case Entity.TYPE_WORLD:
//                writer.write("#VRML V2.0 utf8\n");
//                writer.write("\n\n#Exported by Sales Design Accelerator from Yumetech, Inc.\n");
                if (e.hasChildren()) {
                    Iterator<Entity> roomIterator = e.getChildren().iterator();
                    while (roomIterator.hasNext()) {
                        serializeEntity(roomIterator.next(), handler);
                    }
                }
                break;

            // This is a LocationEntity, and represents a Room in the closetmaid
            // scheme of things. A room will contain exactly one ViewpointContainerEntity
            // and one ContentContainerEntity.
            case Entity.TYPE_LOCATION:
                if (e.hasChildren()) {
                    Iterator<Entity> roomChildren = e.getChildren().iterator();
                    while (roomChildren.hasNext()) {
                        serializeEntity(roomChildren.next(), handler);
                    }
                }
                break;


            // Will be an instance of a ViewpointContainerEntity
            // It therefore doesn't need any properties, just serves
            // as a typed container for viewpoints.
            case Entity.TYPE_CONTAINER:
                // process all the child viewpoints
                if (e.hasChildren()) {
                    List<ViewpointEntity> viewpoints =
                        ((ViewpointContainerEntity) e).getViewpoints();
                    Iterator<ViewpointEntity> vpIterator = viewpoints.iterator();
                    while (vpIterator.hasNext()) {
                        serializeEntity(vpIterator.next(), handler);
                    }
                }
                break;

            // Will be an instance of a DefaultEntity, and contains
            // all the renderable content within a room. A content
            // container in this case has 2 types of children, zones
            // and an optional wall plan.
            case Entity.TYPE_CONTENT_ROOT:
                if (e.hasChildren()) {
                    Iterator<Entity> contentIterator = e.getChildren().iterator();
                    while (contentIterator.hasNext()) {
                        serializeEntity(contentIterator.next(), handler);
                    }
                }
                break;


            // This is a ProductEntity, and is the only project specific entity so
            // far, a model can have arbitrarily deep nestings of other models, but
            // no other types of children are allowed.
            case Entity.TYPE_MODEL:
            case Entity.TYPE_TEMPLATE_CONTAINER:
/*
                if (e instanceof PositionableEntity) {
                    double[] pos = new double[3];
                    float[] scale = new float[3];
                    float[] rot = new float[4];

                    PositionableEntity pe = (PositionableEntity) e;
                    pe.getPosition(pos);
                    pe.getScale(scale);
                    pe.getRotation(rot);

                    writer.write("Transform {\n");

                    writer.write("   translation ");
                    writer.write(nFormater.format(pos[0]));
                    writer.write(" ");
                    writer.write(nFormater.format(pos[1]));
                    writer.write(" ");
                    writer.write(nFormater.format(pos[2]));
                    writer.write("\n");

                    if (scale[0] != 1 && scale[1] != 1 && scale[2] != 1) {
                        writer.write("   scale ");
                        writer.write(nFormater.format(scale[0]));
                        writer.write(" ");
                        writer.write(nFormater.format(scale[1]));
                        writer.write(" ");
                        writer.write(nFormater.format(scale[2]));
                        writer.write(" ");
                        writer.write("\n");
                    }

                    if (rot[3] != 0) {
                        writer.write("   rotation ");
                        writer.write(nFormater.format(rot[0]));
                        writer.write(" ");
                        writer.write(nFormater.format(rot[1]));
                        writer.write(" ");
                        writer.write(nFormater.format(rot[2]));
                        writer.write(" ");
                        writer.write(nFormater.format(rot[3]));
                        writer.write(" ");
                        writer.write("\n");
                    }
                    writer.write("   children [\n");
                } else {
                    writer.write("Group {\n");
                    writer.write("   children [");
                }

                writer.write("Inline {\n");
                String url = e.getModelURL();
                if (url != null) {
                    writer.write("   url [\"");
                    writer.write(url);
                    writer.write("\"]");
                }
                writer.write("}\n");
                if (e.hasChildren()) {
                    Iterator<Entity> children = e.getChildren().iterator();
                    while (children.hasNext()) {
                        serializeEntity(children.next(), handler);
                    }
                }

                writer.write("]}\n");
*/
                break;

            // This is the optional wall plan, which is of type
            // DefaultSegmentableEntity
            // it will contain VertexEntity and SegmentEntity children.
            case Entity.TYPE_MULTI_SEGMENT:
                SegmentableEntity wallPlan = ((SegmentableEntity) e);
                if (wallPlan.hasChildren()) {
                    Iterator<VertexEntity> vertices = wallPlan.getVertices()
                            .iterator();
                    while (vertices.hasNext()) {
                        serializeEntity(vertices.next(), handler);
                    }
                    Iterator<SegmentEntity> segments = wallPlan.getSegments()
                            .iterator();
                    while (segments.hasNext()) {
                        serializeEntity(segments.next(), handler);
                    }
                }
                break;

            // This is a Wall, of type SegmentEntity, and will contain products. The
            // parent
            // of a Wall must be a SegmentableEntity.
            case Entity.TYPE_SEGMENT:
/*
                 // lets get the start and end vertex IDs
                SegmentEntity segment = (SegmentEntity)e;
                attributes.put(DesignXMLConstants.CLASS_ATT, e.getClass().getName());
                attributes.put(
                        DesignXMLConstants.START_VERTEX_ATT,
                        segment.getStartVertexEntity().getEntityID());
                attributes.put(
                        DesignXMLConstants.END_VERTEX_ATT,
                        segment.getEndVertexEntity().getEntityID());

                serializer.addElement(DesignXMLConstants.ZONE_TAG, attributes);
                serializeProperties(e);
                if (e.hasChildren()) {
                    serializer.addElement(DesignXMLConstants.OBJECTS_TAG, null);
                    Iterator<Entity> wallProducts = e.getChildren().iterator();
                    while (wallProducts.hasNext()) {
                        serializeEntity(wallProducts.next(), null, handler);
                    }
                    serializer.endElement();
                }
                serializer.endElement();
*/
                break;

            // This is a vertex, and has no data model representation, except to
            // hold position
            // information for Segments. The parent of a vertex must be a
            // SegmentableEntity.
            case Entity.TYPE_VERTEX:
                break;

            // A viewpoint, which is of type ViewpointEntity, this represents a
            // camera location
            // that can be used to generate snapshots of the scene. It is a child of
            // a
            // ViewpointContainerEntity.
            case Entity.TYPE_VIEWPOINT:
                break;


            // This is a zone... which can have products
            case Entity.TYPE_ZONE:
                if (e.hasChildren()) {
                    Iterator<Entity> zoneProducts = e.getChildren().iterator();
                    while (zoneProducts.hasNext()) {
                        serializeEntity(zoneProducts.next(), handler);
                    }
                }
                break;
        }
    }

}
