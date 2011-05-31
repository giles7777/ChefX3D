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
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.catalog.CatalogManager;


/**
 * Handler for RemoveEntity commands.
 *
 * @author Alan Hudson
 * @version
 */
public class RemoveEntityHandler {
    public RemoveEntityHandler() {
    }

    /**
     * Serialize the current command to an XML string.
     *
     * @return The command as XML
     */
    public static String serialize(Entity entity){
        /*
         * <RemoveEntityCommand entityID='1' />
         */

        StringBuilder sbuff = new StringBuilder();
        sbuff.append("<RemoveEntityCommand entityID='");
        sbuff.append(entity.getEntityID());
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
        Entity entity = model.getEntity(Integer.parseInt(e.getAttribute("entityID")));

        RemoveEntityCommand cmd = new RemoveEntityCommand(model, entity);

        return cmd;
    }
}