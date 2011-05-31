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
 * Handler for MoveVertexTransient commands.
 *
 * @author Alan Hudson
 * @version
 */
public class MoveVertexTransientHandler {
    public MoveVertexTransientHandler() {
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
         * <MoveVertexTransientCommand entityID='1' px='' py='' pz='' />
         */

        double[] pos = (double[]) entity.getProperty(VertexEntity.VERTEX_PROPERTY_SHEET,
        		PositionableEntity.POSITION_PROP);

        StringBuilder sbuff = new StringBuilder();
        sbuff.append("<MoveV entityID='");
        sbuff.append(entity.getEntityID());
        sbuff.append("' tID='");
        sbuff.append(transactionID);
        sbuff.append("' px='");
        sbuff.append(pos[0]);
        sbuff.append("' py='");
        sbuff.append(pos[1]);
        sbuff.append("' pz='");
        sbuff.append(pos[2]);
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

        String d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        float[] velocity = new float[3];

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        VertexEntity entity = (VertexEntity) model.getEntity(entityID);
        int transactionID = Integer.parseInt(e.getAttribute("tID"));

        MoveVertexTransientCommand cmd = new MoveVertexTransientCommand(
        		model,
        		transactionID, 
        		entity,
        		pos, 
        		velocity);

        return cmd;
    }
}