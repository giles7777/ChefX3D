package catalog.Locations.Grid;

/**
 * Generate a grid.
 */
import java.util.Map;

import org.web3d.x3d.sai.*;

public class GridGenerator implements X3DScriptImplementation {

    private MFNode shapes;

    // private float majorSpacing;
    private float minorSpacing;

    private int number;

    private float size;

    private Browser browser;

    public GridGenerator() {
    }

    // ----------------------------------------------------------
    // Methods defined by X3DScriptImplementation
    // ----------------------------------------------------------

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public void setFields(X3DScriptNode externalView, Map fields) {

        shapes = (MFNode) fields.get("shapes");
        minorSpacing = ((SFFloat) fields.get("minorSpacing")).getValue();
        // majorSpacing = ((SFFloat) fields.get("majorSpacing")).getValue();
        number = ((SFInt32) fields.get("number")).getValue();
        size = ((SFFloat) fields.get("size")).getValue();
    }

    public void initialize() {
        //System.out.println("Creating Grid");
        X3DExecutionContext scene = browser.getExecutionContext();
        X3DNode ils = scene.createNode("IndexedLineSet");
        X3DNode coord = scene.createNode("Coordinate");
        SFNode geometryField;
        SFNode appearanceField;
        SFNode coordField;
        MFVec3f pointField;
        X3DNode color;
        SFNode colorField;
        MFColor cField;
        float[] colors;
        float[] points;
        int idxs[];
        MFInt32 colorIndexField;
        MFInt32 coordIndexField;
        int idx;

        X3DNode axisGeometry;
        X3DNode minorGeometry;
        // X3DNode majorGeometry;

        coordField = (SFNode) ils.getField("coord");
        coordField.setValue(coord);

        pointField = (MFVec3f) coord.getField("point");

        points = new float[3 * (number - 1) * 2 * 2];
        int halfNum = (number - 1) / 2;
        float halfSize = size / 2;
        idx = 0;
        for (int i = 0; i < number; i++) {
            if (i - halfNum == 0)
                continue;
            points[idx++] = minorSpacing * (i - halfNum);
            points[idx++] = 0;
            points[idx++] = -halfSize;
            points[idx++] = minorSpacing * (i - halfNum);
            points[idx++] = 0;
            points[idx++] = halfSize;
        }

        for (int i = 0; i < number; i++) {
            if (i - halfNum == 0)
                continue;
            points[idx++] = -halfSize;
            points[idx++] = 0;
            points[idx++] = minorSpacing * (i - halfNum);
            points[idx++] = halfSize;
            points[idx++] = 0;
            points[idx++] = minorSpacing * (i - halfNum);
        }

        pointField.setValue(points.length / 3, points);

        coordIndexField = (MFInt32) ils.getField("coordIndex");

        idxs = new int[(number - 1) * 3 * 2];
        idx = 0;

        for (int i = 0; i < (number - 1) * 2; i++) {
            idxs[i * 3] = idx++;
            idxs[i * 3 + 1] = idx++;
            idxs[i * 3 + 2] = -1;
        }

        coordIndexField.setValue(idxs.length, idxs);

        minorGeometry = scene.createNode("Shape");
        geometryField = (SFNode) minorGeometry.getField("geometry");
        geometryField.setValue(ils);

        // Axis Geometry
        ils = scene.createNode("IndexedLineSet");
        coord = scene.createNode("Coordinate");
        color = scene.createNode("Color");

        coordField = (SFNode) ils.getField("coord");
        coordField.setValue(coord);

        colorField = (SFNode) ils.getField("color");
        colorField.setValue(color);

        pointField = (MFVec3f) coord.getField("point");

        points = new float[] { halfSize, 0, 0, -halfSize, 0, 0, 0, 0, halfSize,
                0, 0, -halfSize };
        pointField.setValue(points.length / 3, points);

        cField = (MFColor) color.getField("color");

        colors = new float[] { 0, 0, 1 };
        cField.setValue(colors.length / 3, colors);

        coordIndexField = (MFInt32) ils.getField("coordIndex");

        idxs = new int[] { 0, 1, -1, 2, 3, -1 };
        coordIndexField.setValue(idxs.length, idxs);

        colorIndexField = (MFInt32) ils.getField("colorIndex");

        idxs = new int[] { 0, 0, 0, 0 };
        colorIndexField.setValue(idxs.length, idxs);

        axisGeometry = scene.createNode("Shape");
        geometryField = (SFNode) axisGeometry.getField("geometry");
        geometryField.setValue(ils);
        appearanceField = (SFNode) axisGeometry.getField("appearance");
        X3DNode app = scene.createNode("Appearance");
        X3DNode lineprops = scene.createNode("LineProperties");
        SFFloat linewidthField = (SFFloat) lineprops
                .getField("linewidthScaleFactor");
        linewidthField.setValue(2);
        SFNode lpField = (SFNode) app.getField("lineProperties");
        lpField.setValue(lineprops);
        appearanceField.setValue(app);

        shapes.setValue(2, new X3DNode[] { minorGeometry, axisGeometry });
    }

    public void eventsProcessed() {
    }

    public void shutdown() {
    }

    /*
     * private void createMinorLines() { }
     *
     * private void createAxisLines() { X3DExecutionContext scene =
     * browser.getExecutionContext(); X3DNode ils; X3DNode coord; SFNode
     * coordField; MFVec3f pointField; X3DNode color; SFNode colorField; MFColor
     * cField; float[] colors; float[] points; int idxs[]; MFInt32
     * colorIndexField; MFInt32 coordIndexField; int idx;
     *  }
     */

}
