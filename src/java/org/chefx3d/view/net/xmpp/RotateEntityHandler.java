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
 * Handler for RotateEntity commands.
 *
 * @author Alan Hudson
 * @version
 */
public class RotateEntityHandler {
    public RotateEntityHandler() {
    }

    /**
     * Serialize an entity to an XML string.
     *
     * @param entity The entity to serialize
     * @param transactionID The unique transaction ID we are closing
     * @return The command as XML
     */
    public static String serialize(Entity entity, int transactionID) {
        /*
         * <RotateEntityCommand entityID='1' rotx='' roty='' rotz='' />
         */

        float[] rot = (float[]) entity.getProperty("Properties", PositionableEntity.ROTATION_PROP);

        StringBuilder sbuff = new StringBuilder();
        sbuff.append("<RotateEntityCommand entityID='");
        sbuff.append(entity.getEntityID());
        sbuff.append("' tID='");
        sbuff.append(transactionID);
        sbuff.append("' rotx='");
        sbuff.append(rot[0]);
        sbuff.append("' roty='");
        sbuff.append(rot[1]);
        sbuff.append("' rotz='");
        sbuff.append(rot[2]);
        sbuff.append("' rota='");
        sbuff.append(rot[3]);
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

        String d;

        float[] rot = new float[4];
        float[] endRot = new float[4];

        d = e.getAttribute("rotx");
        rot[0] = Float.parseFloat(d);
        d = e.getAttribute("roty");
        rot[1] = Float.parseFloat(d);
        d = e.getAttribute("rotz");
        rot[2] = Float.parseFloat(d);
        d = e.getAttribute("rota");
        rot[3] = Float.parseFloat(d);

        endRot[0] = rot[0];
        endRot[1] = rot[1];
        endRot[2] = rot[2];
        endRot[3] = rot[3];

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        int transID = Integer.parseInt(e.getAttribute("tID"));
        Entity entity = (PositionableEntity)model.getEntity(entityID);

        RotateEntityCommand cmd = 
            new RotateEntityCommand(
                    model, 
                    transID, 
                    (PositionableEntity)entity,
                    endRot, 
                    endRot);

        return cmd;
    }
}