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
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.DefaultCatalogManager;


/**
 * Handler for AddEntity commands.
 *
 * @author Alan Hudson
 * @version
 */
public class AddEntityHandler {
	
    public AddEntityHandler() {
    }

    /**
     * Serialize the current command to an XML string.
     *
     * @return The command as XML
     */
    public static String serialize(Entity entity){
        double[] pos = (double[]) entity.getProperty("Properties", PositionableEntity.POSITION_PROP);
        float[] rot = (float[]) entity.getProperty("Properties", PositionableEntity.ROTATION_PROP);

        /*
         * <AddEntityCommand entityID='1' px='' py='' pz='' rotx='' roty=''
         * rotz='' rota='' name='' />
         */

        StringBuilder sbuff = new StringBuilder();

        sbuff.append("<AddEntityCommand entityID='");
        sbuff.append(entity.getEntityID());
        sbuff.append("' px='");
        sbuff.append(pos[0]);
        sbuff.append("' py='");
        sbuff.append(pos[1]);
        sbuff.append("' pz='");
        sbuff.append(pos[2]);

        if (rot != null) {
            sbuff.append("' rotx='");
            sbuff.append(rot[0]);
            sbuff.append("' roty='");
            sbuff.append(rot[1]);
            sbuff.append("' rotz='");
            sbuff.append(rot[2]);
            sbuff.append("' rota='");
            sbuff.append(rot[3]);
        }

        sbuff.append("' name='");
        sbuff.append(entity.getName());
        sbuff.append("' />");

        String st = sbuff.toString();

        return st;
    }

    /**
     * Deserialize a string into a command.
     *
     * @param model The world model
     * @param xml The XML string
     * @return The command or null if it failed or is a dup
     */
    public static Command deserialize(WorldModel model, String xml) {
        Document doc = DOMUtils.parseXML(xml);

        Element e = (Element) doc.getFirstChild();
        String toolName = e.getAttribute("name");

        Tool tool = DefaultCatalogManager.getCatalogManager().findTool(toolName);

        if (tool == null)
            System.out.println("Cannot find tool: " + toolName);

        boolean local = false;

        String d;

        double[] pos = new double[3];
        d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        float[] rot = new float[4];

        d = e.getAttribute("rotx");

        if (d.length() > 0)
            rot[0] = Float.parseFloat(d);
        d = e.getAttribute("roty");
        if (d.length() > 0)
            rot[1] = Float.parseFloat(d);
        d = e.getAttribute("rotz");
        if (d.length() > 0)
            rot[2] = Float.parseFloat(d);
        d = e.getAttribute("rota");
        if (d.length() > 0)
            rot[3] = Float.parseFloat(d);

        int entityID = Integer.parseInt(e.getAttribute("entityID"));

        Entity entity = model.getEntity(entityID);

        if (entity == null) {

            // Emulate sender logic, issueID before building entity
            int issueID = model.issueEntityID();
            if (issueID != entityID) {
                // Not sure what to do in this case, continue with original ID and hope for the best
                System.out.println("**** IssueID does not equal new entityID:  issued: " + issueID + " new: " + entityID);
            }

            EntityBuilder builder = DefaultEntityBuilder.getEntityBuilder();
            entity = builder.createEntity(model, entityID, pos, rot, tool);

            // Used to be applied directly, likely kills undo stack
            AddEntityCommand cmd = new AddEntityCommand(model, entity);

            return cmd;
        } else
            return null;
    }
}