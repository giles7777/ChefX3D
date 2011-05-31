/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006-2007
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

/**
 * A command which needs dead reckoning.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public interface DeadReckonedCommand {
    /**
     * Get the dead reckoning params. Some commands may only use position or
     * orientation.
     * 
     * @param position The position data
     * @param orientation The orientation data
     * @param lVelocity The linear velocity
     * @param aVelocity The angular velocity
     */
    public void getDeadReckoningParams(double[] position, float[] orientation,
            float[] lVelocity, float[] aVelocity);

    /**
     * Set the dead reckoning params. Some commands may only use pos or
     * orientation.
     * 
     * @param position The position data
     * @param orientation The orientation data
     * @param lVelocity The linear velocity
     * @param aVelocity The angular velocity
     */
    public void setDeadReckoningParams(double[] position, float[] orientation,
            float[] lVelocity, float[] aVelocity);
}