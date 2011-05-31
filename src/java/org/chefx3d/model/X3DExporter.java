/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
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
import org.web3d.x3d.sai.*;

// Internal Imports
import org.chefx3d.util.*;

/**
 * Export a world model into the X3D format.
 *
 * @author Alan Hudson
 * @version $Revision: 1.48 $
 */
public class X3DExporter extends AbstractExporter implements Exporter {

    /** The header string to add. */
    private String header;

    /**
     * Constructor.
     *
     * @param version The spec major and minor version number
     * @param profile The profile to use when exporting
     * @param components The components to add to the profile. Null means none.
     * @param levels The components levels
     */
    public X3DExporter(
            String version,
            String profile,
            String[] components,
            int[] levels) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<X3D profile=\"");
        sb.append(profile);
        sb.append("\" version=\"");
        sb.append(version);
        sb.append("\">\n");
        sb.append("<head>\n");

        if (components != null && levels != null) {
            int len = Math.min(components.length, levels.length);

            for (int i = 0; i < len; i++) {
                sb.append("\t<component name=\"");
                sb.append(components[i]);
                sb.append("\" level=\"");
                sb.append(levels[i]);
                sb.append("\" />\n");
            }
        }

        sb.append("</head>\n");
        sb.append("<Scene>\n");

        header = sb.toString();

        errorReporter = DefaultErrorReporter.getDefaultReporter();

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

        // get the entity
        Entity entity = model.getEntity(entityID);

        if (entity == null) {
            errorReporter.messageReport("Cannot find model to export: " + entityID);
            return null;
        }

        if (entity.isController()) {
            return null;
        }

        try {

            X3DNode group;

            // if the entity has a position then place it
            if (entity instanceof PositionableEntity) {

                double[] position = new double[3];
                float[] rotation = new float[4];
                float[] scale = new float[3];

                ((PositionableEntity)entity).getPosition(position);
                ((PositionableEntity)entity).getRotation(rotation);
                ((PositionableEntity)entity).getScale(scale);

                float[] pos = new float[] {
                        (float)position[0],
                        (float)position[1],
                        (float)position[2]};

                // create the transform group node
                group = mainScene.createNode("Transform");
                SFVec3f translationField = (SFVec3f)(group.getField("translation"));
                translationField.setValue(pos);
                SFRotation rotationField = (SFRotation)(group.getField("rotation"));
                rotationField.setValue(rotation);
                SFVec3f scaleField = (SFVec3f)(group.getField("scale"));
                scaleField.setValue(scale);

            } else {

                // create the transform group node
                group = mainScene.createNode("Group");

            }

            // create the inline node
            String url = worldURL + entity.getModelURL();

            X3DNode inline = mainScene.createNode("Inline");
            MFString urlField = (MFString)(inline.getField("url"));
            urlField.setValue(1, new String[] {url});

            // grab the child node to append to
            MFNode childrenField = (MFNode)(group.getField("children"));

            // add inline to the group/transform
            childrenField.append(inline);

            return group;

        } catch (Exception ex) {
            errorReporter.errorReport("Error.", ex);
        }

        return null;
    }


    /**
     * Output a specific entity to the specified stream.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param fw The stream to write to
     */
    public void export(
            WorldModel model,
            int entityID,
            Writer fw,
            String worldURL) {

        super.export(model, entityID, fw);

        Entity[] toolValues = ((BaseWorldModel) model).getModelData();

        Entity entity = toolValues[entityID];

        if (entity == null) {
            //errorReporter.messageReport("Cannot find model to export: " + entityID);
            return;
        }

        if (entity.isController()) {
            return;
        }

        try {
            fw.write(header);

            fw.write("<!-- Begin entity: " + entity.getEntityID() + "-->\n");

            // if the entity has a position then place it
            if (entity instanceof PositionableEntity) {

                double[] position = new double[3];
                float[] rotation = new float[4];
                float[] scale = new float[3];

                ((PositionableEntity)entity).getPosition(position);
                ((PositionableEntity)entity).getRotation(rotation);
                ((PositionableEntity)entity).getScale(scale);

                fw.write("    <Transform " +
                        "translation='" + position[0] + " " + position[1] + " " + position[2] + "' " +
                        "rotation='" + rotation[0] + " " + rotation[1] + " " + rotation[2] + " " + rotation[3] + "' " +
                        "scale='" + scale[0] + " " + scale[1] + " " + scale[2] + "' >\n");


            }

            // inline the model
            String url = entity.getModelURL();

            fw.write("        <Inline url='" + worldURL + url + "' />\n");

            // if the entity has a position then place it
            if (entity instanceof PositionableEntity) {
                fw.write("    </Transform>\n");
            }

            fw.write("<!-- End entity: " + entity.getEntityID() + "-->\n");

            fw.write("</Scene>\n");
            fw.write("</X3D>\n");

            fw.close();
        } catch (IOException ioe) {
            errorReporter.errorReport("IO Error.", ioe);
        }
    }

    /**
     * Output the World Model to the specified stream.
     *
     * @param model The world model to export
     * @param fw The stream to write to
     */
    public void export(WorldModel model, Writer fw) {

        super.export(model, fw);

        Entity[] entities = ((BaseWorldModel) model).getModelData();

        int len = entities.length;

        try {

            fw.write(header);

            for (int i = 0; i < len; i++) {
                Entity entity = entities[i];

                if (entity == null) {
                    // Its expected we will have gaps
                    continue;
                }

                if (entity.isController()) {
                    continue;
                }

                fw.write("<!-- Begin entity: " + entity.getEntityID() + "-->\n");

                // if the entity has a position then place it
                if (entity instanceof PositionableEntity) {

                    double[] position = new double[3];
                    float[] rotation = new float[4];
                    float[] scale = new float[3];

                    ((PositionableEntity)entity).getPosition(position);
                    ((PositionableEntity)entity).getRotation(rotation);
                    ((PositionableEntity)entity).getScale(scale);

                    fw.write("    <Transform " +
                            "translation='" + position[0] + " " + position[1] + " " + position[2] + "' " +
                            "rotation='" + rotation[0] + " " + rotation[1] + " " + rotation[2] + " " + rotation[3] + "' " +
                            "scale='" + scale[0] + " " + scale[1] + " " + scale[2] + "' >\n");


                }

                // inline the model
                String url = entity.getModelURL();
                fw.write("        <Inline url='" + url + "' />\n");

                // if the entity has a position then place it
                if (entity instanceof PositionableEntity) {
                    fw.write("    </Transform>\n");
                }

                fw.write("<!-- End entity: " + entity.getEntityID() + "-->\n");

            }

            fw.write("</Scene>\n");
            fw.write("</X3D>\n");
            fw.close();

        } catch (IOException ioe) {
            errorReporter.errorReport("IO Error.", ioe);
        }
    }

    /**
     * Output a specific entity to the specified file.
     *
     * @param model The world model to export
     * @param name The entity to export
     * @param file - The file to write to
     */
    public void export(WorldModel model, String name, File file) {
        // not implemented
    }

}
