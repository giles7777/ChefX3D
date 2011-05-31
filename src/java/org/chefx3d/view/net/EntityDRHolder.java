/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.net;

// External Imports
import java.util.*;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.model.Command;
import org.chefx3d.model.WorldModel;

/**
 * Holder of Dead Reckoning information for an entity.
 *
 * @author Alan Hudson
 * @version $Revision: 1.1 $
 */
public class EntityDRHolder {
    /** Previous linear velocity values for averaging. [num][xyz] */
    private float[][] linearVelocityHistory;

    /** The current linear velocity at the client side */
    private float[] linearVelocityClient;

    /** Previous angular velocity values for averaging [num][xyza] */
    private float[][] angularVelocityHistory;

    /** The current angular velocity at the client side */
    private float[] angularVelocityClient;

    /** Is the local model the sender of this transaction */
    private boolean sender;

    /** The current model */
    private WorldModel worldModel;

    /** The startTime of this time sequence */
    private long startTime;

    /** The startTime of the transaction */
    private long startTransactionTime;

    /** The initial position */
    private double[] initialPosition;

    /** The current position */
    private double[] currentPosition;

    /** The current orientation */
    private float[] currentOrientation;

    /** The number of packets sent */
    private int packetsSent;

    /** The current linearVelocity index */
    private int lvIndex;

    /** The current angularVelocity index */
    private int avIndex;

    /** The command */
    private Command command;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**
     * Constructor.
     *
     * @param model The world model
     * @param cmd The command
     * @param startTime The time the transaction starts
     * @param pos The initial position
     * @param ori The initial orientation
     * @param sender Are we the sender of these values.
     * @param history How many velocity values should we average over
     */
    public EntityDRHolder(WorldModel model, Command cmd, long stime,
            double[] pos, float[] ori, boolean sender, int averageLength) {
        worldModel = model;
        this.sender = sender;
        command = cmd;
        startTime = stime;
        startTransactionTime = stime;
        initialPosition = new double[3];
        initialPosition[0] = pos[0];
        initialPosition[1] = pos[1];
        initialPosition[2] = pos[2];

        currentPosition = new double[3];
        currentPosition[0] = pos[0];
        currentPosition[1] = pos[1];
        currentPosition[2] = pos[2];

        currentOrientation = new float[4];
        currentOrientation[0] = ori[0];
        currentOrientation[1] = ori[1];
        currentOrientation[2] = ori[2];
        currentOrientation[3] = ori[3];

        linearVelocityHistory = new float[averageLength][3];
        linearVelocityClient = new float[3];
        angularVelocityHistory = new float[averageLength][4];
        angularVelocityClient = new float[4];

        lvIndex = 0;
        avIndex = 0;

        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Set the start position.
     *
     * @param pos The new start position
     * @param stime The start time
     */
    public void setStartPosition(double[] pos, long stime) {
        initialPosition[0] = pos[0];
        initialPosition[1] = pos[1];
        initialPosition[2] = pos[2];

        startTime = stime;
    }

    /**
     * Are we the sender of this entity.
     *
     * @return The sender value
     */
    public boolean isSender() {
        return sender;
    }

    /**
     * Send a new position update. Update velocity to average.
     *
     * @return A command representing the new client params.
     */
    public Command sendNewPosition() {
        getVelocityAverages(linearVelocityClient, angularVelocityClient);
/*
        if (command instanceof DeadReckonedCommand) {
            DeadReckonedCommand drcmd = (DeadReckonedCommand) command;

            drcmd.setDeadReckoningParams(currentPosition, currentOrientation,
                    linearVelocityClient, angularVelocityClient);

            System.out.println("new pack ori: "
                    + Arrays.toString(currentOrientation));
            startTime = System.currentTimeMillis();
            initialPosition[0] = currentPosition[0];
            initialPosition[1] = currentPosition[1];
            initialPosition[2] = currentPosition[2];

            packetsSent++;
            float totalTime = (System.currentTimeMillis() - startTransactionTime) / 1000f;

            //errorReporter.messageReport("Pack hz: " + (packetsSent / totalTime)
            //        + " time(s): " + totalTime + " packs: " + packetsSent);
        }
*/
        return command;
    }

    public void issueLocalUpdate(double[] pos) {
/*
        if (command instanceof DeadReckonedCommand) {
            DeadReckonedCommand drcmd = (DeadReckonedCommand) command;

            drcmd.setDeadReckoningParams(pos, currentOrientation,
                    linearVelocityClient, angularVelocityClient);

            command.setLocal(false);
            command.execute();
        }
*/
    }

    /**
     * Set the current position.
     *
     * @param pos The new position
     */
    public synchronized void setCurrentPosition(double[] pos) {
        currentPosition[0] = pos[0];
        currentPosition[1] = pos[1];
        currentPosition[2] = pos[2];
    }

    /**
     * Set the current orientation.
     *
     * @param pos The new position
     */
    public synchronized void setCurrentOrientation(float[] ori) {
        currentOrientation[0] = ori[0];
        currentOrientation[1] = ori[1];
        currentOrientation[2] = ori[2];
    }

    /**
     * Get the current position.
     *
     * @param pos The new position
     */
    public synchronized void getCurrentPosition(double[] pos) {
        pos[0] = currentPosition[0];
        pos[1] = currentPosition[1];
        pos[2] = currentPosition[2];
    }

    /**
     * Set the client velocity.
     */
    public synchronized void setClientVelocity(float[] linearVelo,
            float[] angularVelo) {
        linearVelocityClient[0] = linearVelo[0];
        linearVelocityClient[1] = linearVelo[1];
        linearVelocityClient[2] = linearVelo[2];

        angularVelocityClient[0] = angularVelo[0];
        angularVelocityClient[1] = angularVelo[1];
        angularVelocityClient[2] = angularVelo[2];
        angularVelocityClient[3] = angularVelo[3];
    }

    /**
     * Add a value to the linear velocity history.
     *
     * @param velocity The velocity value.
     */
    public void addLinearVelocity(float[] velocity) {
        linearVelocityHistory[lvIndex][0] = velocity[0];
        linearVelocityHistory[lvIndex][1] = velocity[1];
        linearVelocityHistory[lvIndex][2] = velocity[2];

        lvIndex++;

        if (lvIndex > linearVelocityHistory.length - 1)
            lvIndex = 0;
    }

    /**
     * Add a value to the angular velocity history.
     *
     * @param velocity The velocity value.
     */
    public void addAngularVelocity(float[] velocity) {
        angularVelocityHistory[lvIndex][0] = velocity[0];
        angularVelocityHistory[lvIndex][1] = velocity[1];
        angularVelocityHistory[lvIndex][2] = velocity[2];
        angularVelocityHistory[lvIndex][3] = velocity[3];

        lvIndex++;

        if (lvIndex > linearVelocityHistory.length - 1)
            lvIndex = 0;
    }

    /**
     * Get the linear velocity average.
     *
     * @param avgLinearVelocity Get the linear velocity
     * @param avgAngularVelocity Get the angular velocity
     */
    public void getVelocityAverages(float[] avgLinearVelocity,
            float[] avgAngularVelocity) {
        double lx = 0;
        double ly = 0;
        double lz = 0;

        double ax = 0;
        double ay = 0;
        double az = 0;
        double aa = 0;

        for (int i = 0; i < linearVelocityHistory.length; i++) {
            lx += linearVelocityHistory[i][0];
            ly += linearVelocityHistory[i][1];
            lz += linearVelocityHistory[i][2];
        }

        lx = lx / linearVelocityHistory.length;
        ly = ly / linearVelocityHistory.length;
        lz = lz / linearVelocityHistory.length;

        avgLinearVelocity[0] = (float) lx;
        avgLinearVelocity[1] = (float) ly;
        avgLinearVelocity[2] = (float) lz;

        for (int i = 0; i < angularVelocityHistory.length; i++) {
            ax += angularVelocityHistory[i][0];
            ay += angularVelocityHistory[i][1];
            az += angularVelocityHistory[i][2];
            aa += angularVelocityHistory[i][3];
        }

        ax = ax / angularVelocityHistory.length;
        ay = ay / angularVelocityHistory.length;
        az = az / angularVelocityHistory.length;
        aa = aa / angularVelocityHistory.length;

        avgAngularVelocity[0] = (float) ax;
        avgAngularVelocity[1] = (float) ay;
        avgAngularVelocity[2] = (float) az;
        avgAngularVelocity[3] = (float) aa;
    }

    /**
     * Calculate a new position based on simple dead reckoning. Only uses
     * velocity, no acceleration.
     *
     * @param t The time to calc position for
     * @param newPos The new position
     */
    public synchronized void drPosition(long t, double[] newPos) {
        float dt = (t - startTime) * 0.001f;

        newPos[0] = initialPosition[0] + dt * linearVelocityClient[0];
        newPos[1] = initialPosition[1] + dt * linearVelocityClient[1];
        newPos[2] = initialPosition[2] + dt * linearVelocityClient[2];
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