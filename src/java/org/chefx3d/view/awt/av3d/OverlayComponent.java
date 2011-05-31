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

// external imports
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.TransformGroup;

// internal imports
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Base implementation of an overlay component.
 * Parent node is a SwitchGroup containing a TransformGroup
 * which contains all the actual geometry of the component. 
 * SwitchGroup is used to toggle the visibility of
 * the component via the boolean "enabled".
 *  
 * @author Eric Fickenscher
 * @version $Revision: 1.7 $ 
 */
public abstract class OverlayComponent implements NodeUpdateListener {

    /** Index in the switch for model content */
    protected static final int CONTENT_NONE = -1;

    /** Index in the switch for model content */
    protected static final int CONTENT_DISPLAY = 0;
    
    /** The scene manager Observer*/
    protected SceneManagerObserver mgmtObserver;
    
    /** Utility tool for building common aviatrix shapes */
    protected AVGeometryBuilder avBuilder;
    
    /** SwitchGroup that hides or shows the overlay component */
    protected SwitchGroup switchGroup;

    /** The component grouping node */
    protected TransformGroup compGroup;
    
    /** If TRUE, enable the current component for display,
     * else disable the current component so must remain hidden */
    protected boolean enabled;
    
    /** If TRUE, set the SwitchGroup to display the active content
     * (only usable if 'enabled' is also true).
     * If FALSE, set the SwitchGroup to display nothing. */
    protected boolean display;
    
    /** The current unit. If this component displays a value, it 
     * should be represented with this value */
    protected Unit currentUnit;
    
    /**
     * Restricted constructor
     */
	protected OverlayComponent(
		SceneManagerObserver mgmtObserverParam,
		AVGeometryBuilder avBuilderParam,
		Unit unitParam) {

        mgmtObserver = mgmtObserverParam;
        avBuilder = avBuilderParam;
        currentUnit = unitParam;
        
        compGroup = new TransformGroup();
        switchGroup = new SwitchGroup();
        switchGroup.addChild(compGroup);
        switchGroup.setActiveChild(CONTENT_DISPLAY);
        enabled = true;
        display = true;
    }
    
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
    
    /**
     * Return the parent SwitchGroup (which 
     * contains the TransformGroup holding all the geometry).
     * @return the parent Group node containing the main TransformGroup. 
     */
    public SwitchGroup getSwitchGroup(){
        return(switchGroup);
    }
    
    /**
     * Show or hide the current component.  This method will call
     * SceneManagerObserver's requestBoundsUpdate() method to toggle
     * the SwitchGroup on and off.
     * @param show If TRUE, display the current component,
     * else hide the current component.
     */
    public void display(boolean show) {
        
        if (show != display) {
            display = show;
			mgmtObserver.requestBoundsUpdate(switchGroup, this);
        }
    }
    
    /**
     * Redraw this component.
     */
    public void redraw() {
        
        if (enabled ) {
            display(true);
			mgmtObserver.requestBoundsUpdate(compGroup, this);
        }
    }
	
    /**
     * Enable the current component for display.  This method will always
     * call display(false) to force a 'hide'.
     * <p> This choice was made because the user can still call enable(true) 
     * and display(true) in order to display whenever a component is enabled.  
     * However, this code is defensive, to prevent the user from accidentally
     * displaying 'old' geometry whenever enabling becomes true. 
     * Note too that in ALL instances when we want to disable the component 
     * we want to hide it. 
     * @param show If TRUE, enable the current component for display,
     * else disable the current component.
     */
    public void enable(boolean show){
        enabled = show;
        display(false);
    }
    
    /**
     * Set a new unit of measurement.  This supports toggling between
     * different units of measurement, such as centimeters and inches.
     * Recommended that an updateNodeBoundsChanges(compGroup) is called
     * immediately afterward, so that the new units will display.
     * @param newUnit the new unit of measurement.
     */
    public void setUnitOfMeasurement(Unit newUnit){
        currentUnit = newUnit;
    }
    
    //----------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //----------------------------------------------------------

    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src){
        if (src == switchGroup) {
            if (display && enabled) {
                switchGroup.setActiveChild(CONTENT_DISPLAY);
			} else {
                switchGroup.setActiveChild(CONTENT_NONE);
			}
        }
    }
    
    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {    	
        // ignored here
    }
}