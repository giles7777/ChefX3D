/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007-2009
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
import java.io.*;
import java.util.*;
import org.jivesoftware.smackx.muc.*;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.Command;
import org.chefx3d.view.net.EntityDRHolder;

/**
 * Send network updates for transient changes.
 *
 * Transient updates come in at frame rate speeds. We don't need that sort of
 * update rate on the network. This class could be responsible for doing any
 * dead reckoning as well.
 *
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
class TransientProcessor extends Thread {
    /** The minimum amount of time between transient updates */
    private static final int CYCLE_TIME = 20;

    /** The number of velocity values to average over */
    private static final int VELOCITY_AVERAGE_LENGTH = 3;

    /** The default linear velocity tolerance in meters */
    private static final float DEFAULT_LINEAR_TOLERANCE = 0.5f;

    /** The default angular velocity tolerance in meters */
    private static final float DEFAULT_ANGULAR_TOLERANCE = 0.5f;

    /** Should we using dead reckoning */
    private boolean deadReckon;

    /** Multiuser chat room */
    private MultiUserChat chatroom;

    /** The current model */
    private WorldModel worldModel;

    /** The last time we sent a packet */
    private long lastSendTime;

    /** Should we terminate sending */
    private boolean terminate;

    /** The last command received */
    private Command lastCommand;

    /** The completed transactions */
    private Set completedTransactions;

    /** The current transactions */
    private HashMap<Integer, EntityDRHolder> transactions;

    /** The linear tolerance */
    private float linearTolerance;

    /** The angular tolerance */
    private float angularTolerance;

    /** Scratch var for linear velocity */
    private float[] linearVelo;

    /** Scratch var for angular velocity */
    private float[] angularVelo;

    /** Scratch var for pos */
    private double[] pos;

    /** Scratch var for pos */
    private double[] pos2;

    /** Scratch var for orientation */
    private float[] ori;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * Constructor
     *
     * @param room The chatroom to use
     */
    public TransientProcessor(MultiUserChat room, WorldModel model) {
        this(room, model, true, DEFAULT_LINEAR_TOLERANCE,
                DEFAULT_ANGULAR_TOLERANCE);
    }

    /**
     * Constructor
     *
     * @param room The chatroom to use
     */
    public TransientProcessor(MultiUserChat room, WorldModel model,
            boolean deadReckon, float linearTolerance, float angularTolerance) {

        worldModel = model;
        chatroom = room;
        this.deadReckon = deadReckon;
        this.linearTolerance = linearTolerance;
        this.angularTolerance = angularTolerance;

        transactions = new HashMap<Integer, EntityDRHolder>();
        completedTransactions = Collections.synchronizedSet(new HashSet());

        linearVelo = new float[3];
        angularVelo = new float[4];
        pos = new double[3];
        pos2 = new double[3];
        ori = new float[4];

        terminate = false;

        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * A transaction has ended.
     *
     * @param tID The transactionID
     */
    public void closeTransaction(int tID) {
        Integer id = new Integer(tID);

        completedTransactions.add(id);
        transactions.remove(id);
    }

    /**
     * A new command arrived from the network.
     *
     * @param cmd The command.
     */
    public void commandArrived(Command cmd) {
        Integer tID = new Integer(cmd.getTransactionID());

        if (completedTransactions.contains(tID)) {
            System.out.println("Ignoring command: " + cmd);
            return;
        }

        EntityDRHolder edr;

        edr = transactions.get(tID);
/*
        if (cmd instanceof DeadReckonedCommand) {
            ((DeadReckonedCommand) cmd).getDeadReckoningParams(pos, ori,
                    linearVelo, angularVelo);
        }
*/
        if (edr == null) {
            edr = new EntityDRHolder(worldModel, cmd, System
                    .currentTimeMillis(), pos, ori, false,
                    VELOCITY_AVERAGE_LENGTH);
            edr.setErrorReporter(errorReporter);
            edr.setClientVelocity(linearVelo, angularVelo);

            transactions.put(new Integer(tID), edr);
        } else {
            // TODO: Should we include the time in the packet?
            edr.setErrorReporter(errorReporter);
            edr.setStartPosition(pos, System.currentTimeMillis());
            edr.setClientVelocity(linearVelo, angularVelo);
        }

        worldModel.applyCommand(cmd);
    }

    /**
     * Run the updater.
     */
    public void run() {

        while (!terminate) {
            long time = System.currentTimeMillis();

            try {
                Thread.sleep(CYCLE_TIME);
            } catch (Exception e) {
                errorReporter.errorReport("Sleep Cycle Error!", e);
            }
        }

        /*
         * while(!terminate) { Iterator itr =
         * transactions.entrySet().iterator();
         *
         * long currTime = System.currentTimeMillis();
         *
         * while(itr.hasNext()) { Map.Entry entry = (Map.Entry) itr.next();
         * EntityDRHolder edr = (EntityDRHolder) entry.getValue();
         *  // Calc DR position, if above threshold issue new update
         * edr.drPosition(currTime, pos);
         *
         * if (edr.isSender()) { edr.getCurrentPosition(pos2);
         *
         * double dist = distance(pos,pos2);
         *
         * if (!deadReckon || dist > linearTolerance) { if (deadReckon)
         * System.out.println("Above tolerance, distance: " + dist); Command cmd =
         * edr.sendNewPosition(); sendCommand(cmd); } }
         *
         * edr.issueLocalUpdate(pos); }
         *
         * try { Thread.sleep(CYCLE_TIME); } catch(Exception e) {} }
         */
    }

    /**
     * Stop sending packets.
     */
    public void stopSending() {
        terminate = true;
    }

    /**
     * Calc the distance between two positions.
     *
     */
    private double distance(double[] p1, double[] p2) {
        double x, y, z;

        x = p2[0] - p1[0];
        y = p2[1] - p1[1];
        z = p2[2] - p1[2];

        // TODO: Can we use a fast distance calc?
        double ret_val = Math.sqrt(x * x + y * y + z * z);

        return ret_val;
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

}