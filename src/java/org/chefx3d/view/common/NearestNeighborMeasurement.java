/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.common;

// External imports
import java.util.ArrayList;

// Internal imports
import org.chefx3d.model.*;

/**
 * Defines the requirements for identifying entities in close proximity.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public interface NearestNeighborMeasurement {

    /** Static variable that represents the positive x axis is being check*/
    public static final int POS_X = 0;

    /** Static variable that represents the negative x axis is being check*/
    public static final int NEG_X = 1;

    /** Static variable that represents the positive y axis is being check*/
    public static final int POS_Y = 2;

    /** Static variable that represents the negative y axis is being check*/
    public static final int NEG_Y = 3;

    /** Static variable that represents the negative y axis is being check*/
    public static final int POS_Z = 4;

    /** Static variable that represents the negative y axis is being check*/
    public static final int NEG_Z = 5;

    /**
     * Returns a sorted list of neighboring entities that intersect
     * the 'extended' bounds of the specified entity along a
     * particular direction.
     *
     * @param model Holds all the entity data
     * @param activeZone The current zone being edited
     * @param currentEntity The current entity being checked
     * @param direction The axis direction to check
     * @param boundsAdj The adjustment to the current entity's bounds.
     * @return The list of neighboring entities
     */
    public ArrayList<Entity> nearestNeighbors(
        WorldModel model,
        Entity activeZone,
        PositionableEntity currentEntity,
        int direction,
        float[] boundsAdj);

    /**
     * Returns a sorted list of neighbors that fall on this entities
     * axis in the relative zone
     * @param model Holds all the entity data
     * @param activeZone The current zone being edited
     * @param currentEntity The current entity being checked
     * @param direction The direction to check
     * @return
     */
    public ArrayList<Entity> nearestNeighbors(
            WorldModel model,
            Entity activeZone,
            PositionableEntity currentEntity,
            int direction);
}
