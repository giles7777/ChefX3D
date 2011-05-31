/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2006-2007
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there"s a problem you get to fix it.
 *
 ****************************************************************************/

package demos;

// External Imports
import java.util.*;

import java.awt.EventQueue;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.MultiplicityConstraint;
import org.chefx3d.model.WorldModelFactory;

import org.chefx3d.property.AssociateProperty;
import org.chefx3d.model.ListProperty;
import org.chefx3d.property.ColorValidator;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.tool.*;
import org.chefx3d.view.net.ViewpointController;
import org.chefx3d.view.View;
import org.chefx3d.view.awt.gt2d.GT2DView;
import org.chefx3d.view.awt.gt2d.ViewingFrustum;
import org.chefx3d.view.net.xmpp.SimpleSharedView;

/**
 * A networked example that uses XMPP.
 *
 * @author Alan Hudson
 * @version
 */
public class XMPPExample extends SimpleExample {

    private SimpleSharedView nview;

    private static final String USAGE_MSG =
      "Usage: XMPPExample [options] [filename]\n" +
      "  -help                   Prints out this help message\n" +
      "  -server n           Connects to specified server\n";

    private static final String SERVER = "nasa-xmpp.yumetech.com";
    private static final int PORT = 5222;
    private static final String MUCSERVER = "nasa-xmpp.yumetech.com";
    private static final String ROOM = "mercury";
    private static final String USERNAME = "tester0";
    private static final String PASSWORD = "tester0";


    public XMPPExample(String server, int port, String mucServer,
        String room, String username, String passwd) {

        nview = new SimpleSharedView(model, controller, server, port,
           mucServer, room, username, passwd);

        nview.setErrorReporter(errorReporter);

        View[] views = new View[] {view3d};
        ViewpointController vpController = new ViewpointController(views);
/*
        System.out.println("Hiding 3D view");
        externalViewer.setVisible(false);
        externalViewer.removeNotify();
*/
    }


    public static void main(final String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Set System L&F
                    UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
                } catch (UnsupportedLookAndFeelException e) {
                    // handle exception
                } catch (ClassNotFoundException e) {
                   // handle exception
                } catch (InstantiationException e) {
                   // handle exception
                } catch (IllegalAccessException e) {
                   // handle exception
                }

                int lastUsed = -1;

                String server = SERVER;
                int port = PORT;

                String user = USERNAME;
                String password = PASSWORD;
                String room = ROOM;
                String muc = MUCSERVER;

                for(int i = 0; i < args.length; i++) {
                    if(args[i].startsWith("-")) {
                        if(args[i].equals("-server")) {
                            lastUsed = i;
                            server = args[++i];
                        } else if(args[i].equals("-user")) {
                            lastUsed = i;
                            user = args[++i];
                        } else if(args[i].equals("-password")) {
                            lastUsed = i;
                            password = args[++i];
                        } else if(args[i].equals("-room")) {
                            lastUsed = i;
                            room = args[++i];
                        } else if(args[i].equals("-muc")) {
                            lastUsed = i;
                            muc = args[++i];
                        } else if(args[i].equals("-port")) {
                            lastUsed = i;
                            String val = args[++i];
                            port = Integer.valueOf(val).intValue();
                        } else if(args[i].equals("-help")) {
                            System.out.println(USAGE_MSG);
                            return;
                        } else if (args[i].startsWith("-")) {
                            System.out.println("Unknown flag: " + args[i]);
                            lastUsed = i;
                        }
                    }
                }

                // The last argument is the filename parameter
                String filename = null;

                if((args.length > 0) && (lastUsed + 1 < args.length)) {
                    filename = args[args.length - 1];

                    // TODO: Doing nothing with filename right now
                }

                XMPPExample example = new XMPPExample(server, port, muc, room, user, password);
            }
        });
    }
}
