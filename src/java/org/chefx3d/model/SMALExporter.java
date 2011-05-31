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
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.DOMSource;

// Internal Imports

/**
 * Export a world model into the SMAL format.
 *
 * TODO: Right now this is not exactly SMAL. Includes tool params and segments.
 * Not positive that SMAL is actually the right choice for persistance.
 *
 * @author Alan Hudson
 * @version $Revision: 1.25 $
 */
public class SMALExporter extends AbstractExporter implements Exporter {

    /**
     * Save the current world to the specified file as a SMAL file.
     *
     * @param model The world mode
     * @param fw The writer stream
     */
    public void export(WorldModel model, Writer fw) {
        super.export(model, fw);

        Entity[] toolValues = ((BaseWorldModel) model).getModelData();

        int len = toolValues.length;
        double[] pos = new double[3];
        // HashMap styles;

        try {
            StringWriter sw = new StringWriter(1024);

            Result result = new StreamResult(sw);
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write("<SMAL>\n");
            // boolean useDefault = false;

            Transformer trans = null;

            try {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                trans = tFactory.newTransformer();
            } catch (Exception e) {
                e.printStackTrace();
            }

            errorReporter.messageReport("SMAL Models to write: " + len);

            for (int i = 0; i < len; i++) {
                Entity td = toolValues[i];

                if (td == null)
                    continue;

                exportEntity(model, i, "", fw);
            }

            String input = sw.toString();

            String modString = removeXMLHeader(input);

            fw.write(modString.toString());

            fw.write("</SMAL>\n");
            sw.close();
            fw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Output a specific entity to the specified stream.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param substyle The stylesheet version to use. Appends to the normal
     *        version.
     * @param fw The stream to write to
     */
    public void export(WorldModel model, int entityID, String substyle,
            Writer fw) {

        super.export(model, entityID, fw);

        Entity[] toolValues = ((BaseWorldModel) model).getModelData();

        int len = toolValues.length;
        double[] pos = new double[3];
        // HashMap styles;

        try {
            StringWriter sw = new StringWriter(1024);

            Result result = new StreamResult(sw);
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write("<SMAL>\n");

            exportEntity(model, entityID, substyle, fw);

            fw.write("</SMAL>\n");
            sw.close();
            fw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Exports an entity.  No surrounding file format, just the entity data.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param fw The stream to write to
     */
    private void exportEntity(WorldModel model, int entityID, String substyle,
            Writer fw) throws IOException {

        Entity[] toolValues = ((BaseWorldModel) model).getModelData();

        double[] pos = new double[3];

        StringWriter sw = new StringWriter(1024);

        Result result = new StreamResult(sw);
        Transformer trans = null;

        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            trans = tFactory.newTransformer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Entity td = toolValues[entityID];

        if (td == null)
            return;

        sw.write("<!-- Starting entity: ");
        sw.write(td.getEntityID() + " -->\n");

        try {
            Document combined_props = combinePropertySheets(td);
            DOMSource ds = new DOMSource(combined_props);
            trans.transform(ds, result);

            sw.write("<!-- Ending entity: ");
            sw.write(td.getEntityID() + " -->\n");
        } catch (Exception e) {
            errorReporter.errorReport("SMAL Export Error!", e);
        }

        String input = sw.toString();

        String modString = removeXMLHeader(input);
        fw.write(modString.toString());

        sw.close();
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
