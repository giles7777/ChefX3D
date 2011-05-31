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
 * Handler for ChangeMaster commands.
 *
 * @author Alan Hudson
 * @version
 */
public class ChangeMasterHandler {
    public ChangeMasterHandler() {
    }

    /**
     * Serialize an entity to an XML string.
     *
     * @param viewID The unique viewID to set as master
     * @return The command as XML
     */
    public static String serialize(long viewID) {

        /*
         * <ChangeMasterCommand viewID='1' />
         */

        StringBuilder sbuff = new StringBuilder();
        sbuff.append("<ChangeMasterCommand viewID='");
        sbuff.append(viewID);
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
        long viewID = Long.parseLong(e.getAttribute("viewID"));

        ChangeMasterCommand cmd = new ChangeMasterCommand(model, viewID);

        return cmd;
    }
}