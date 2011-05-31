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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// Local imports
import org.chefx3d.model.Entity;

import org.j3d.device.input.TrackerState;

/**
 * Class to monitor entity selections and determine if
 * a sub part selection is called for.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class SelectionMonitor {
	
	/** The monitor active period */
	private static final long TIMER_PERIOD = 5000;
	
	/** The tolerance, in pixels */
	private static final float PIXEL_TOLERANCE = 2;
	
	/** The timer */
	private Timer timer;
	
	/** Current active timer task */
	private TimerTask resetTask;
	
	/** The currently selected entity */
	private Entity currentPick;
	
	/** The last selected device position */
	private float[] position;
	
	/**
	 * Constructor
	 */
	SelectionMonitor() {
		timer = new Timer();
		position = new float[2];
	}
	
	/**
	 * Return whether the monitor is currectly active
	 *
	 * @return true if the monitor is active, false otherwise
	 */
	boolean isActive() {
		return(currentPick != null);
	}
	
	/**
	 * Clear the monitor state
	 */
	void reset() {
		if (resetTask != null) {
			resetTask.cancel();
			currentPick = null;
		}
	}
	
	/** 
	 * Return the entity that should be selected next
	 *
	 * @param evt The current device parameters
	 * @param pick_list The objects picked at the current device position
	 * @return The next Entity to select, or null if the pick_list is empty
	 */
	Entity check(TrackerState evt, ArrayList<PickData> pick_list) {
		
		if (currentPick == null) {
			// the monitor is not active
			if (!pick_list.isEmpty()) {
				// if an object has been selected,
				// start the monitor
				PickData pd = pick_list.get(0);
				currentPick = (Entity)pd.object;
				
				resetTask = new ResetTask();
				timer.schedule(resetTask, TIMER_PERIOD);
			}
		} else {
			// the monitor is active
			float delta_x = position[0] - evt.devicePos[0];
			float delta_y = position[1] - evt.devicePos[1];
			float delta = (float)Math.sqrt(delta_x * delta_x + delta_y * delta_y);
			if (delta > PIXEL_TOLERANCE) {
				// the device has moved outside
				// of it's allowed range of motion
				reset();
				if (!pick_list.isEmpty()) {
					// select the closest object
					PickData pd = pick_list.get(0);
					currentPick = (Entity)pd.object;
					
					resetTask = new ResetTask();
					timer.schedule(resetTask, TIMER_PERIOD);
				}
			} else {
				// the device is with it's range
				if (pick_list.isEmpty()) {
					// nothing to select
					resetTask.cancel();
					currentPick = null;
					
				} else {
					// find the next entity in sort order,
					// after the current
					int idx = 0;
					int max = pick_list.size();
					for (int i = 0; i < max; i++) {
						PickData pd = pick_list.get(i);
						if (currentPick == pd.object) {
							idx = i + 1;
							if (idx == max) {
								idx = 0;
							}
							break;
						}
					}
					// reset the monitor timer
					resetTask.cancel();
					
					PickData pd = pick_list.get(idx);
					currentPick = (Entity)pd.object;
					
					resetTask = new ResetTask();
					timer.schedule(resetTask, TIMER_PERIOD);
				}
			}
		}
		position[0] = evt.devicePos[0];
		position[1] = evt.devicePos[1];
		return(currentPick);
	}
	
	/**
	 * Task to reset the monitor state when the timer expires
	 */
	class ResetTask extends TimerTask {
		
		/**
		 * Constructor
		 */
		ResetTask() {
		}
		
		/**
		 * Clear the monitor state
		 */
		public void run() {
			reset();
		}
	}
}
