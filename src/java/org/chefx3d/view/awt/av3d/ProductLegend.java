/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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
import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.prefs.Preferences;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.NodeUpdateListener;

// Local Imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.ZoneEntity;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.preferences.PersistentPreferenceConstants;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * Helper class that produces and displays text labels for
 * products during thumbnail capture ops.
 *
 * @author Rex Melton
 * @version $Revision: 1.7 $
 */
class ProductLegend implements 
	LegendHandler, 
	NodeUpdateListener, 
	PerFrameObserver {
	
	/** Local debug flag */
	private static final boolean DEBUG = false;
	
	/** Text height, in pixels */
	private static final float TEXT_HEIGHT_PIXELS = 15.5f;
	
	/** The scene manager observer */
	private SceneManagerObserver mgmtObserver;
	
	/** The group node to place content into */
	private Group group;
	
	/** The manager of the entities to be handled */
	private AV3DEntityManager entityManager;
	
    /** The map of entity wrappers */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;

	/** The active zone Entity */
	private ZoneEntity activeZoneEntity;
	
	/** The active zone Entity wrapper */
	private AV3DEntityWrapper zoneWrapper;
	
	/** Array list of nodes to be incorporated */
	private ArrayList<Node> nodeAddList;
	
	/** Array list of nodes to be removed */
	private ArrayList<Node> nodeRemoveList;
	
	/** Flag indicating that the label geometry should be generated */
	private boolean config;
	
	/** Flag indicating that the label geometry should be changed */
	private boolean config_labels;
	
	/** Flag indicating that the label display state */
	private boolean enabled;
	
	/** Scratch */
	private Vector3f translation;
	private float[] offset;
	
	/** Local transformation utils */
	private TransformUtils tu;
	private Matrix4f mtx;
	
	/** Flag indicating the measurement system */
	private boolean isMetric;
	
	/** View parameter, in meters per pixel */
	private double ratio;
	
	/** The text height, in meters */
	private float text_height;
	
	/**
	 * Constructor
	 *
	 * @param mgmtObserver The SceneManagerObserver
	 * @param group The grouping node to use for the labels
	 */
	ProductLegend(SceneManagerObserver mgmtObserver, Group group) {
		
		this.mgmtObserver = mgmtObserver;
		mgmtObserver.addObserver(this);
		
		this.group = group;
		
		nodeAddList = new ArrayList<Node>();
		nodeRemoveList = new ArrayList<Node>();
		
		translation = new Vector3f();
		offset = new float[3];
		
		tu = new TransformUtils();
		mtx = new Matrix4f();
		
		String appName = (String)ApplicationParams.get(
			ApplicationParams.APP_NAME);
		Preferences prefs = Preferences.userRoot().node(appName);
		String unitLabel = prefs.get(
			PersistentPreferenceConstants.UNIT_OF_MEASUREMENT, 
			PersistentPreferenceConstants.DEFAULT_UNIT_OF_MEASUREMENT);
		Unit unit = UnitConversionUtilities.getUnitByCode(unitLabel);
		String system_string = UnitConversionUtilities.getMeasurementSystem(unit);
		isMetric = system_string.equals(UnitConversionUtilities.METRIC);
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameObserver
	//---------------------------------------------------------------
	
	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrame() {
		
		if (config) {
			update();
			config = false;
		}
		if (config_labels) {
			mgmtObserver.requestBoundsUpdate(group, this);
		}
	}
	
	//----------------------------------------------------------
	// Methods defined by NodeUpdateListener
	//----------------------------------------------------------
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that could potentially effect the node's bounds.
	 *
	 * @param src The Node that is to be updated.
	 */
	public void updateNodeBoundsChanges(Object src) {
		
		if (src == group) {
			
			int numToAdd = nodeAddList.size();
			if (numToAdd > 0) {
				for (int i = 0; i < numToAdd; i++) {
					Node node = nodeAddList.get(i);
					group.addChild(node);
				}
			}
			int numToRemove = nodeRemoveList.size();
			if (numToRemove > 0) {
				for (int i = 0; i < numToRemove; i++) {
					Node node = nodeRemoveList.get(i);
					group.removeChild(node);
				}
				nodeRemoveList.clear();
			}
			
			nodeRemoveList.addAll(nodeAddList);
			nodeAddList.clear();
			
			config_labels = false;
		}
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The Node that is to be updated.
	 */
	public void updateNodeDataChanges(Object src) {
	}
	
	//----------------------------------------------------------
	// Methods defined by LegendHandler
	//----------------------------------------------------------
	
	/**
	 * Set the active entity manager 
	 *
	 * @param entityManager The active entity manager 
	 */
	public void setEntityManager(AV3DEntityManager entityManager) {
		this.entityManager = entityManager;
		this.wrapperMap = entityManager.getAV3DEntityWrapperMap();
	}
	
	/**
	 * Set the active zone entity
	 *
	 * @param ze The active zone entity
	 */
	public void setActiveZoneEntity(ZoneEntity ze) {
		
		activeZoneEntity = ze;
	}
	
	/**
	 * Enable the display of the components
	 *
	 * @param state The display of the components
	 */
	public void setEnabled(boolean state) {
		
		if (enabled != state) {
			// the display state has changed
			enabled = state;
		}
		config = true;
	}
	
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/**
	 * Update the state of the displayed components
	 */
	void update() {
		if (enabled) {
			if ((entityManager != null) && (activeZoneEntity != null)) {
				// generate the labels
				if (wrapperMap != null) {
					
					// find the entity that the labels will be placed relative to
					zoneWrapper = wrapperMap.get(activeZoneEntity.getEntityID());
					
					if (zoneWrapper != null) {
						if (addLabels(activeZoneEntity) || 
							(nodeRemoveList.size() > 0)) {
							
							config_labels = true;
						}
					}
				}
			}
		} else {
			// queue a request to remove the labels - if they exist
			if (nodeRemoveList.size() > 0) {
				config_labels = true;
			}
		}
	}
				
	/**
	 * Set the display parameter
	 *
	 * @param ratio The display parameter, in meters per pixel
	 */
	void setViewParam(double ratio) {
		this.ratio = ratio;
		text_height = (float)(TEXT_HEIGHT_PIXELS * ratio);
	}
	
	/**
	 * Add labels for products, as necessary.
	 *
	 * @param parent The entity to check for child products
	 */
	private boolean addLabels(Entity parent) {
		boolean labelAdded = false;
		if (parent.hasChildren()) {
			ArrayList<Entity> childList = parent.getChildren();
			for (int i = 0; i < childList.size(); i++) {
				
				Entity child = childList.get(i);
				String abbr = null;
				if (!DEBUG) {
					abbr = (String)child.getProperty(
						Entity.DEFAULT_ENTITY_PROPERTIES,
						ChefX3DRuleProperties.REPORT_ABBREVIATION);
				} else {
					abbr = Integer.toString(child.getEntityID());
				}
				if ((abbr != null) && (abbr.length() > 0)) {
					
					abbr = " "+ abbr +" ";
					
					Boolean showCutLength = (Boolean)child.getProperty(
						Entity.DEFAULT_ENTITY_PROPERTIES,
						ChefX3DRuleProperties.REPORT_SHOW_CUT_LENGTH);
					if ((showCutLength != null) && showCutLength) {
						abbr += "- "+ getLinearLength(child) +" ";
					}
					
					int id = child.getEntityID();
					AV3DEntityWrapper childWrapper = wrapperMap.get(id);
					if (childWrapper != null) {
						tu.getLocalToVworld(
							childWrapper.transformGroup, 
							zoneWrapper.transformGroup, 
							mtx);
						
						mtx.get(translation);
						offset[0] = translation.x;
						offset[1] = translation.y;
						offset[2] = 0;
						
						LegendText lt = new LegendText(mgmtObserver);
						lt.setBackgroundColor(Color.white);
						lt.update(abbr, text_height, Anchor.CENTER, offset);
						
						nodeAddList.add(lt.getShape());
						
						labelAdded |= true;
					}
				}
				labelAdded |= addLabels(child);
			}
		}
		return(labelAdded);
	}
	
    /**
     * Get the linear length of the given product entity. The scale 
     * restriction of the product determines the axis to use
     * @param product The product whose distance we're measuring
     * @return a float equal to the linear length of the product 
     *   along the scale restriction axis
     */
    private String getLinearLength(Entity product){

    	float xbounds, ybounds, zbounds;
    	float xLinLength, yLinLength, zLinLength;
        float[] bounds = new float[6];
        
    	// get product bounds
		((PositionableEntity)product).getBounds(bounds);
        xbounds = bounds[1] - bounds[0];
        ybounds = bounds[3] - bounds[2];
        zbounds = bounds[5] - bounds[4];

		if (isMetric) {
			xLinLength = xbounds;
        	yLinLength = ybounds;
        	zLinLength = zbounds;
		} else {
        	// change the size to the desired unit for linear length
        	xLinLength = UnitConversionUtilities.convertMetersTo(xbounds, Unit.INCHES);
        	yLinLength = UnitConversionUtilities.convertMetersTo(ybounds, Unit.INCHES);
        	zLinLength = UnitConversionUtilities.convertMetersTo(zbounds, Unit.INCHES);
		}
        
        // get any axis restrictions
        ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES scaleRestriction =
            (ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES)
                RulePropertyAccessor.getRulePropertyValue(
                        product,
                        ChefX3DRuleProperties.SCALE_RESTRICTION_PROP);
        
        // get the size based on the restriction, default to x-axis
        float result = 0;
        switch (scaleRestriction) {
            default:
            case XAXIS:
                result = xLinLength;
                break;
            case YAXIS:
                result = yLinLength;
                break;
            case ZAXIS:
                result = zLinLength;
                break;
        }

		String label;
		if (isMetric) {
			label = UnitConversionUtilities.getFormatedNumberDisplay(Unit.METERS, result);
		} else {
			label = UnitConversionUtilities.getFormatedNumberDisplay(Unit.INCHES, result);
		}
		
        return(label);
    }
	
	/**
	 * Return a short String identifier of the argument Entity
	 *
	 * @param entity The entity
	 * @return The identifier
	 */
	private String getIdentifier(Entity entity) {
		return("[id="+ entity.getEntityID() + ", name=\""+ entity.getName() +"\"]");
	}
}
