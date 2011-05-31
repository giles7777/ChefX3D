/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
import org.j3d.device.input.TrackerState;

// Internal imports
import org.chefx3d.model.Entity;
import org.chefx3d.tool.Tool;

/**
 * Interface for responding to tracker events. It is expected that tracker
 * implementations will have a TrackerEventResponse for each unique entity
 * type and input type as is required to accommodate user interactions. All
 * implementations of TrackerEventResponse should have a constructor that
 * has the following parameters: WorldModel model, ErrorReporter reporter.
 *
 * @author Ben Yarger, Jonathon Hubbard
 * @version $Revision: 1.5 $
 */
interface TrackerEventResponse {

    /**
     * Begins the processing required to generate a command in response
     * to the input received.
     *
     * @param trackerID The id of the tracker calling the original handler
     * @param trackerState The event that started this whole thing off
     * @param entities The array of entities to handle
     * @param tool The tool that is used in the action (can be null)
     */
    void doEventResponse(
            int trackerID,
            TrackerState trackerState,
            Entity[] entities,
            Tool tool);
}
