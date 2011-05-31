/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009-
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.net.xmpp;

// External Imports
import java.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

// Local imports
import org.chefx3d.model.*;
import org.chefx3d.util.DOMUtils;

/**
 * Handler for ChangeView commands.
 *
 * @author Alan Hudson
 * @version
 */
public class ChangeViewHandler {
    public ChangeViewHandler() {
    }

    /**
     * Serialize an entity to an XML string.
     *
     * @param transID The transactionID
     * @param pos The position in world coordinates(meters, Y-UP, X3D
     *        System).
     * @param rot The orientation
     * @param fov The field of view
     * @return The command as XML
     */
    public static String serialize(int transID, double[] pos,
            float[] rot, float fov) {
        /*
         * <ChangeViewCommand entityID='1' px='' py='' pz='' />
         */

        /*
         * <ChangeViewCommand tID='' x='' y='' z='' rx='' ry='' rz='' ra=''
         * fov=''/>
         */

        // TODO: double length is pretty damn long, cut to float for now?
        StringBuilder sbuff = new StringBuilder();

        // TODO: can't stop it padding 0 on right, impossible with this
        // class?
        sbuff.append("<ChangeViewCommand tID='");
        sbuff.append(transID);
        sbuff.append("' x='");
        sbuff.append(String.format((Locale) null, "%.3f", pos[0]));
        sbuff.append("' y='");
        sbuff.append(String.format((Locale) null, "%.3f", pos[1]));
        sbuff.append("' z='");
        sbuff.append(String.format((Locale) null, "%.3f", pos[2]));
        sbuff.append("' rx='");
        sbuff.append(String.format((Locale) null, "%.3f", rot[0]));
        sbuff.append("' ry='");
        sbuff.append(String.format((Locale) null, "%.3f", rot[1]));
        sbuff.append("' rz='");
        sbuff.append(String.format((Locale) null, "%.3f", rot[2]));
        sbuff.append("' ra='");
        sbuff.append(String.format((Locale) null, "%.3f", rot[3]));
        sbuff.append("' fov='");
        sbuff.append(String.format((Locale) null, "%.2f", fov));
        sbuff.append("' />");

        String st = sbuff.toString();

        return st;
    }

    /**
     * Deserialize a string into a command.
     *
     * @param model The world model
     * @param xml The XML string
     * @return The command or null if it failed
     */
    public static Command deserialize(WorldModel model, String xml) {
        Document doc = DOMUtils.parseXML(xml);

        Element e = (Element) doc.getFirstChild();

        double[] pos = new double[3];
        String d;

        pos = new double[3];

        d = e.getAttribute("x");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("y");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("z");
        pos[2] = Double.parseDouble(d);

        float[] rot = new float[4];

        d = e.getAttribute("rx");
        rot[0] = Float.parseFloat(d);
        d = e.getAttribute("ry");
        rot[1] = Float.parseFloat(d);
        d = e.getAttribute("rz");
        rot[2] = Float.parseFloat(d);
        d = e.getAttribute("ra");
        rot[3] = Float.parseFloat(d);

        d = e.getAttribute("fov");
        float fov = Float.parseFloat(d);
        int transactionID = Integer.parseInt(e.getAttribute("tID"));

        ChangeViewCommand cmd = new ChangeViewCommand(model, transactionID,
            pos, rot, fov);

        return cmd;
    }
}