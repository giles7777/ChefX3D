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
 * Handler for MoveEntity commands.
 *
 * @author Alan Hudson
 * @version
 */
public class MoveEntityTransientHandler {
    public MoveEntityTransientHandler() {
    }

    /**
     * Serialize an entity to an XML string.
     *
     * @param entity The entity to serialize
     * @param transactionID The unique transaction ID we are starting
     * @return The command as XML
     */
    public static String serialize(Entity entity, int transactionID) {
        /*
         * <MoveEntityTransientCommand entityID='1' tID='' px='' py='' pz=''
         * vx='' vy='' vz=''/>
         */

        double[] pos = (double[]) entity.getProperty("Properties", PositionableEntity.POSITION_PROP);
        int entityID = entity.getEntityID();

        StringBuilder sbuff = new StringBuilder();
        sbuff.append("<MoveT entityID='");
        sbuff.append(entityID);
        sbuff.append("' tID='");
        sbuff.append(transactionID);
        sbuff.append("' px='");
        sbuff.append(String.format("%.3f", pos[0]));
        sbuff.append("' py='");
        sbuff.append(String.format("%.3f", pos[1]));
        sbuff.append("' pz='");
        sbuff.append(String.format("%.3f", pos[2]));

        // TODO: LinearVelocity was not a property need to add back
/*
        sbuff.append("' vx='");
        sbuff.append(String.format("%.3f", linearVelocity[0]));
        sbuff.append("' vy='");
        sbuff.append(String.format("%.3f", linearVelocity[1]));
        sbuff.append("' vz='");
        sbuff.append(String.format("%.3f", linearVelocity[2]));
*/
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

        double[] pos = new double[3];
        d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        float[] linearVelocity = new float[3];

/*
        String f;

        linearVelocity = new float[3];
        f = e.getAttribute("vx");
        linearVelocity[0] = Float.parseFloat(f);
        f = e.getAttribute("vy");
        linearVelocity[1] = Float.parseFloat(f);
        f = e.getAttribute("vz");
        linearVelocity[2] = Float.parseFloat(f);
*/

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        int transactionID = Integer.parseInt(e.getAttribute("tID"));
        Entity entity = (PositionableEntity)model.getEntity(entityID);

        MoveEntityTransientCommand cmd = new MoveEntityTransientCommand(model,
            transactionID, entityID, pos, linearVelocity);

        return cmd;
    }
}