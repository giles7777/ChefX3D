/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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
// None

//Internal Imports
// None

/**
 * Factory for creating WorldModels.
 *
 * @deprecated This factory is no longer useful. You should directly create
 *    your world model from now on
 * @author Alan Hudson
 * @version $Revision: 1.8 $
 */
public class WorldModelFactory {
    public static final int NETWORK_NONE = 0;

    public static final int NETWORK_XMPP = 1;

    /**
     * Create a WorldModel.
     *
     * @param controller The controller that manages commands
     * @return The WorldModel
     */
    public static WorldModel createModel(CommandController controller) {
        return createModel(controller, NETWORK_NONE, "", "", "", "");
    }

    /**
     * Create a WorldModel.
     *
     * @param controller The controller that manages commands
     * @param network What network to use
     * @param server The server to use
     * @param resource The source to connect to
     * @param username The user name to use
     * @param passwd The passwd to use
     * @return The WorldModel
     */
    public static WorldModel createModel(CommandController controller, int network, String server,
            String resource, String username, String passwd) {

        switch (network) {
            case NETWORK_NONE:
                return new DefaultWorldModel(controller);
            case NETWORK_XMPP:
            default:
                System.out.println("Unknown network: " + network);
        }

        return new DefaultWorldModel(controller);
    }
}
