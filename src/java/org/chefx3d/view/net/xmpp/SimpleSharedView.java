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
import java.io.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smackx.muc.*;

import javax.security.auth.callback.*;

import javax.swing.JOptionPane;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

// Local imports
import org.chefx3d.model.*;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DOMUtils;

/**
 * XMPP based code to share a ChefX3D model.  Optimized for < 5 participants
 * sharing a design.
 *
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
public class SimpleSharedView implements ModelListener, CallbackHandler,
    EntityPropertyListener, EntityChildListener, PacketListener {

    // Command integers
    private static final int COMMAND_AddAssociationCommand = 0;
    private static final int COMMAND_AddEntityCommand = 1;
    private static final int COMMAND_AddPropertyCommand = 2;
    private static final int COMMAND_AddVertexCommand = 3;
    private static final int COMMAND_ChangeMasterCommand = 4;
    private static final int COMMAND_ChangePropertyCommand = 5;
    private static final int COMMAND_ChangeViewCommand = 6;
    private static final int COMMAND_ChangeViewTransientCommand = 7;
    private static final int COMMAND_ClearModelCommand = 8;
    private static final int COMMAND_MoveEntityCommand = 9;
    private static final int COMMAND_MoveEntityTransientCommand = 10;
    private static final int COMMAND_RemoveAssociationCommand = 11;
    private static final int COMMAND_RemoveEntityCommand = 12;
    private static final int COMMAND_RotateEntityCommand = 13;
    private static final int COMMAND_RotateEntityTransientCommand = 14;
    private static final int COMMAND_MoveVertexCommand = 15;
    private static final int COMMAND_MoveVertexTransientCommand = 16;

    // Properties which care about
    private static final int PROP_Position = 0;
    private static final int PROP_Rotation = 1;

    /** The world model */
    private WorldModel model;

    /** The controller for the model */
    private CommandController controller;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The server to connect to */
    private String server;

    /** The muc server to connect to */
    private String mucServer;

    /** The port to connect to */
    private int port;

    /** The username to use */
    private String username;

    /** The password to use */
    private String password;

    /** Connection to the XMPP server */
    private XMPPConnection connection;

    /** Multiuser chat room */
    private MultiUserChat chatroom;

    /** The muc jid */
    private String mucJid;

    /** The muc room on the muc server we joining */
    private String mucRoom;

    /** Mapping of Commands to tokens for a switch statement */
    private Map<String, Integer> commandMap;

    /** Mapping of Properties to tokens for a switch statement */
    private Map<String, Integer> propertyMap;

    /** The TransientProcessor for handling transient commands */
    private TransientProcessor transientProcessor;

    /** Ignore model updates */
    private boolean ignoreUpdates;

    /** Temp variable for string building */
    private StringBuilder sbuff = new StringBuilder();

    /** TransactionID map */
    private HashMap<Entity, Integer> transMap;

    /**
     * Contruct a networked view
     *
     * @param model The model to replicate
     * @param controller The controller that manages commands
     * @param server The server to use
     * @param port The port to use.  0  to use the default port
     * @param username The user name to use
     * @param passwd The passwd to use
     */
    public SimpleSharedView(WorldModel model, CommandController controller,
        String server, int port, String mucServer, String room, String username,
        String passwd) {

        this.model = model;
        this.controller = controller;
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = passwd;
        mucRoom = room;
        this.mucServer = mucServer;

        ignoreUpdates = false;
        transMap = new HashMap<Entity, Integer>();

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        sbuff = new StringBuilder();

        model.addModelListener(this);

        initMapping();

        login();
    }

    //----------------------------------------------------------
    // Methods defined by ModelListener
    //----------------------------------------------------------

    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity added to the view
     */
    public void entityAdded(boolean local, Entity entity) {
        entity.addEntityPropertyListener(this);
        entity.addEntityChildListener(this);

        if (ignoreUpdates)
            return;

        String st = AddEntityHandler.serialize(entity);

        sendMessage(st);
    }

    /**
     * An entity was removed.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {
        entity.removeEntityPropertyListener(this);
        entity.removeEntityChildListener(this);

        if (ignoreUpdates)
            return;

        String st = RemoveEntityHandler.serialize(entity);

        sendMessage(st);
    }

    /**
     * User view information changed.
     *
     * @param local Was this action initiated from the local UI
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        if (ignoreUpdates)
            return;

        int transID = 0;    // TODO: Do we need transactions here

        String st = ChangeViewHandler.serialize(transID, pos, rot, fov);

        sendMessage(st);
    }

    /**
     * The master view has changed.
     *
     * @param local Was this action initiated from the local UI
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
        if (ignoreUpdates)
            return;

        String st = ChangeMasterHandler.serialize(viewID);

        sendMessage(st);
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
    }

    //----------------------------------------------------------
    // Methods defined by PropertyListener
    //----------------------------------------------------------


    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void propertyUpdated(int entityID,
            String propertySheet, String propertyName, boolean ongoing) {

        if (ignoreUpdates)
            return;

        // TODO:  Currently just handle the properties we know about
        // TODO:  Need to know which properties or sheets are local to
        //        avoid serializing them

        if (propertySheet.equals("Properties")) {
            Integer val = propertyMap.get(propertyName);

            boolean found = false;

            if (val != null) {
                switch (val.intValue()) {
                    case PROP_Position:
                        Entity entity = model.getEntity(entityID);
                        if (ongoing) {
                            Integer tID = transMap.get(entity);

                            if (tID == null) {
                                tID = new Integer(model.issueTransactionID());
                                transMap.put(entity, tID);
                            }

                            String st = MoveEntityTransientHandler.serialize(entity,
                                tID.intValue());

                            sendMessage(st);
                        } else {

                            Integer tID = transMap.get(entity);
                            int trans = 0;

                            if (tID == null) {
                                System.out.println("***Move Entity without transient???");
                            } else {
                                transMap.remove(entity);

                                transientProcessor.closeTransaction(tID.intValue());

                                trans = tID.intValue();
                            }

                            String st = MoveEntityHandler.serialize(entity, trans);
                            sendMessage(st);
                        }
                        break;
                    case PROP_Rotation:
                        entity = model.getEntity(entityID);
                        if (ongoing) {
                            Integer tID = transMap.get(entity);

                            if (tID == null) {
                                tID = new Integer(model.issueTransactionID());
                                transMap.put(entity, tID);
                            }

                            String st = RotateEntityTransientHandler.serialize(entity,
                                tID.intValue());

                            sendMessage(st);
                        } else {

                            Integer tID = transMap.get(entity);
                            int trans = 0;

                            if (tID == null) {
                                System.out.println("***Rotate Entity without transient???");
                            } else {
                                transMap.remove(entity);

                                transientProcessor.closeTransaction(tID.intValue());

                                trans = tID.intValue();
                            }

                            String st = RotateEntityHandler.serialize(entity, trans);
                            sendMessage(st);
                        }
                        break;
                }
            }
        } else if (propertySheet.equals(VertexEntity.VERTEX_PROPERTY_SHEET)) {
            Integer val = propertyMap.get(propertyName);

            boolean found = false;

            if (val != null) {
                switch (val.intValue()) {
                    case PROP_Position:
                        Entity entity = model.getEntity(entityID);

                        if (entity == null)
                            System.out.println("***Failed to find entity: " + entityID);
                        if (ongoing) {
                            Integer tID = transMap.get(entity);

                            if (tID == null) {
                                tID = new Integer(model.issueTransactionID());
                                transMap.put(entity, tID);
                            }

                            //  TODO: Need to figure out if the entity is a vertex entity
                            String st = MoveVertexTransientHandler.serialize(entity,
                                tID.intValue());

                            sendMessage(st);
                        } else {

                            Integer tID = transMap.get(entity);
                            int trans = 0;

                            if (tID == null) {
                                System.out.println("***Move Entity without transient???");
                            } else {
                                transMap.remove(entity);

                                transientProcessor.closeTransaction(tID.intValue());

                                trans = tID.intValue();
                            }

                            String st = MoveVertexHandler.serialize(entity, trans);
                            sendMessage(st);
                        }
                    break;
                }
            }
        }
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
System.out.println("Properties Updated: " + properties.size());
    }


     //----------------------------------------------------------
     // Methods defined by EntityChildListener
     //----------------------------------------------------------


    /**
     * A child was added.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     */
    public void childAdded(int parent, int child) {
System.out.println("ChildAdded: " + parent + " child: " + child);
    }

    /**
     * A child was removed.
     *
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {
    }

    /**
     * A child was inserted.
     *
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public void childInsertedAt(int parent, int child, int index) {
    }


     //----------------------------------------------------------
     // Methods defined by CallbackHandler
     //----------------------------------------------------------

     public void handle(Callback[] callbacks)  throws IOException, UnsupportedCallbackException {
         for (int i = 0; i < callbacks.length; i++) {
             if (callbacks[i] instanceof TextOutputCallback) {

                 // display the message according to the specified type
                 TextOutputCallback toc = (TextOutputCallback)callbacks[i];
                 switch (toc.getMessageType()) {
                 case TextOutputCallback.INFORMATION:
                     System.out.println(toc.getMessage());
                     break;
                 case TextOutputCallback.ERROR:
                     System.out.println("ERROR: " + toc.getMessage());
                     break;
                 case TextOutputCallback.WARNING:
                     System.out.println("WARNING: " + toc.getMessage());
                     break;
                 default:
                     throw new IOException("Unsupported message type: " +
                                         toc.getMessageType());
                 }

             } else if (callbacks[i] instanceof NameCallback) {

                 // prompt the user for a username
                 NameCallback nc = (NameCallback)callbacks[i];

                 // ignore the provided defaultName
                 System.err.print(nc.getPrompt());
                 System.err.flush();
                 nc.setName((new BufferedReader
                         (new InputStreamReader(System.in))).readLine());

             } else if (callbacks[i] instanceof PasswordCallback) {

                 // prompt the user for sensitive information
                 PasswordCallback pc = (PasswordCallback)callbacks[i];
                 System.err.print(pc.getPrompt());
                 System.err.flush();

                 System.out.println("Need a password");
                 //pc.setPassword(readPassword(System.in));

             } else {
                 throw new UnsupportedCallbackException
                         (callbacks[i], "Unrecognized Callback");
             }
          }
      }

    // ----------------------------------------------------------
    // Methods required by the PacketListener interface
    // ----------------------------------------------------------

    /**
     * Process a packet from the XMPP connection.
     *
     * @param packet The packet to process
     */
    public void processPacket(Packet packet) {
        if (!(packet instanceof Message))
            return;

        Message msg = (Message) packet;
        String xmlString = msg.getBody();

        // sniff first element name to determine which command it is
        int idx = xmlString.indexOf(" ");

        if (idx < 0)
            return;

        String cmdString = xmlString.substring(1, idx);

        Integer val = commandMap.get(cmdString);

        Command cmd = null;

        try {
        if (val != null) {
            switch (val.intValue()) {
            case COMMAND_AddAssociationCommand:
                break;
            case COMMAND_AddEntityCommand:
                cmd = AddEntityHandler.deserialize(model, xmlString);
                break;
            case COMMAND_AddPropertyCommand:
                break;
            case COMMAND_AddVertexCommand:
                break;
            case COMMAND_ChangeMasterCommand:
                cmd = ChangeMasterHandler.deserialize(model, xmlString);
                break;
            case COMMAND_ChangePropertyCommand:
                break;
            case COMMAND_ChangeViewCommand:
                cmd = ChangeViewHandler.deserialize(model, xmlString);
                break;
            case COMMAND_ChangeViewTransientCommand:
                break;
            case COMMAND_ClearModelCommand:
                break;
            case COMMAND_MoveEntityCommand:
                cmd = MoveEntityHandler.deserialize(model, xmlString);
                break;
            case COMMAND_MoveEntityTransientCommand:
                cmd = MoveEntityTransientHandler.deserialize(model, xmlString);

                MoveEntityTransientCommand met = (MoveEntityTransientCommand) cmd;
                Entity entity = met.getEntity();

                // Don't apply local transactions
                if (transMap.get(entity) != null)
                    cmd = null;
                break;
            case COMMAND_MoveVertexCommand:
                cmd = MoveVertexHandler.deserialize(model, xmlString);
                break;
            case COMMAND_MoveVertexTransientCommand:
                cmd = MoveVertexTransientHandler.deserialize(model, xmlString);

                MoveVertexTransientCommand mvt = (MoveVertexTransientCommand) cmd;
                entity = mvt.getEntity();

                // Don't apply local transactions
                if (transMap.get(entity) != null) {
                    cmd = null;
                }
                break;
            case COMMAND_RemoveEntityCommand:
                cmd = RemoveEntityHandler.deserialize(model, xmlString);
                break;
            case COMMAND_RotateEntityCommand:
                cmd = RotateEntityHandler.deserialize(model, xmlString);
                break;
            case COMMAND_RotateEntityTransientCommand:
                cmd = RotateEntityTransientHandler.deserialize(model, xmlString);

                RotateEntityTransientCommand ret = (RotateEntityTransientCommand) cmd;
                entity = ret.getEntity();

                // Don't apply local transactions
                if (transMap.get(entity) != null) {
                    cmd = null;
                }

                break;
            default:
                System.out.println("Got unknown command: " + val.intValue());
            }
        } else {
            System.out.println("*** Can't find mapping for: " + cmdString);
        }
/*
//System.out.println("Executing network command: " + cmd.getDescription() + " class: " + cmd);

            cmd.setErrorReporter(errorReporter);

            if (found) {
                cmd.deserialize(xmlString);

                if (cmd.isTransient()) {
                    transientProcessor.commandArrived(cmd);
                } else {
                    cmd.execute();
                }
            }
*/
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cmd != null) {
            cmd.setErrorReporter(errorReporter);
            try {
                ignoreUpdates = true;

                if (cmd.isTransient())
                    transientProcessor.commandArrived(cmd);
                else
                    model.applyCommand(cmd);
            } finally {
                ignoreUpdates = false;
            }
        }

/*
        // TODO: THink we need this but it breaks things
        if (!cmd.isTransient()) {
            //errorReporter.messageReport("Completed Command: " + cmd.getClass()
            //        + " tid: " + cmd.getTransactionID());
            int tID = cmd.getTransactionID();

            if (tID != 0) {
                transientProcessor.closeTransaction(tID);
            }
        }
*/
    }


     //----------------------------------------------------------
     // Local Methods
     //----------------------------------------------------------

    /**
     * Initialize the command mapping table.
     */
    private void initMapping() {
        commandMap = new HashMap();
        commandMap.put("AddAssociationCommand", new Integer(
                COMMAND_AddAssociationCommand));

        commandMap.put("AddEntityCommand",
                new Integer(COMMAND_AddEntityCommand));

        commandMap.put("AddPropertyCommand", new Integer(
                COMMAND_AddPropertyCommand));

        commandMap.put("AddVertexCommand", new Integer(
                COMMAND_AddVertexCommand));

        commandMap.put("ChangeMasterCommand", new Integer(
                COMMAND_ChangeMasterCommand));

        commandMap.put("ChangePropertyCommand", new Integer(
                COMMAND_ChangePropertyCommand));

        commandMap.put("ChangeViewCommand", new Integer(
                COMMAND_ChangeViewCommand));

        commandMap.put("ChangeViewTransientCommand", new Integer(
                COMMAND_ChangeViewTransientCommand));

        commandMap.put("ClearModelCommand", new Integer(
                COMMAND_ClearModelCommand));

        commandMap.put("MoveEntityCommand", new Integer(
                COMMAND_MoveEntityCommand));

        commandMap.put("MoveT", new Integer(
                COMMAND_MoveEntityTransientCommand));

        commandMap.put("MoveVertexCommand", new Integer(
                COMMAND_MoveVertexCommand));

        commandMap.put("MoveV", new Integer(
                COMMAND_MoveVertexTransientCommand));

        commandMap.put("RemoveAssociationCommand", new Integer(
                COMMAND_RemoveAssociationCommand));

        commandMap.put("RemoveEntityCommand", new Integer(
                COMMAND_RemoveEntityCommand));

        commandMap.put("RotateEntityCommand", new Integer(
                COMMAND_RotateEntityCommand));

        commandMap.put("RotateEntityTransientCommand", new Integer(
                COMMAND_RotateEntityTransientCommand));

        propertyMap = new HashMap();
        propertyMap.put(PositionableEntity.POSITION_PROP, new Integer(
                PROP_Position));
        propertyMap.put(PositionableEntity.ROTATION_PROP, new Integer(
                PROP_Rotation));

    }

    /**
     * Login to the XMPP server
     */
    private void login() {
        try {
            // Authenticate to our local XMPP server
            errorReporter.messageReport("logging in to " + server);

            if (port == 0) {
                ConnectionConfiguration config = new ConnectionConfiguration(server, 5222);
                config.setSASLAuthenticationEnabled(false);

                config.setReconnectionAllowed(true);
                connection = new XMPPConnection(config, this);

            } else {
                ConnectionConfiguration config = new ConnectionConfiguration(server, port, server);
                //config.setSASLAuthenticationEnabled(false);

                //config.setReconnectionAllowed(true);
                connection = new XMPPConnection(config, this);
                //connection = new XMPPConnection(config);
            }

            errorReporter.messageReport("Using user: " + username + " + password: "
                    + password);

            connection.connect();

            connection.login(username, password);
            errorReporter.messageReport("logged in to " + server);

            errorReporter.messageReport(">>> Getting conn ID: "
                    + connection.getConnectionID());
            errorReporter.messageReport(">>> Getting host: " + connection.getHost());
            errorReporter.messageReport(">>> Getting user: " + connection.getUser());
            errorReporter.messageReport(">>> Getting port: " + connection.getPort());
            errorReporter.messageReport(">>> Getting service name: "
                    + connection.getServiceName());

            // Establish a connection to the MUC room
//            mucJid = mucRoom + "@" + "conference." + server;
            mucJid = mucRoom + "@" + "conference." + mucServer;

            if (chatroom == null) {
                chatroom = new MultiUserChat(connection, mucJid);
            }

            errorReporter.messageReport("Joing chatroom: " + mucJid + " as: " + username);
            DiscussionHistory dh = new DiscussionHistory();
            dh.setMaxStanzas(0);

            final List<String> errors = new ArrayList<String>();

            int groupChatCounter = 0;
            while (true) {
                groupChatCounter++;
                String joinName = username;
                if (groupChatCounter > 1) {
                    joinName = joinName + groupChatCounter;
                }

                if (groupChatCounter < 10) {
                    try {

                        chatroom.join(joinName, password, dh, 10000);

                        break;
                    } catch (XMPPException ex) {
                        int code = 0;
                        if (ex.getXMPPError() != null) {
                            code = ex.getXMPPError().getCode();
                        }

                        if (code == 0) {
                            errors.add("No response from server.");
                        }
                        else if (code == 401) {
                            errors.add("The password did not match the room's password.");
                        }
                        else if (code == 403) {
                            errors.add("You have been banned from this room.");
                        }
                        else if (code == 404) {
                            errors.add("The room you are trying to enter does not exist.");
                        }
                        else if (code == 407) {
                            errors.add("You are not a member of this room.\nThis room requires you to be a member to join.");
                        }
                        else if (code != 409) {
                            break;
                        }
                    }
                } else {
                    break;
                }

            }

            if (errors.size() > 0) {
                String error = errors.get(0);
                JOptionPane.showMessageDialog(null, error, "Unable to join the room at this time.", JOptionPane.ERROR_MESSAGE);
                return;
            }

            transientProcessor = new TransientProcessor(chatroom, model,
                    false, 0.5f, 0.5f);
            transientProcessor.start();

            // set up a packet filter to listen for only the things we want
            PacketFilter filter = new AndFilter(new PacketTypeFilter(
                    Message.class), new FromContainsFilter(mucJid));

            // Register the listener.
            connection.addPacketListener(this, filter);
        } catch (Exception e) {
            errorReporter.errorReport("Login Faliure!", e);
        }
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Send a message to the chatroom.
     *
     * @param msg
     */
    private void sendMessage(String msg) {
        try {
            chatroom.sendMessage(msg);
        } catch (Exception e) {
            errorReporter.errorReport("Apply Command Failed", e);
        }
    }
}
