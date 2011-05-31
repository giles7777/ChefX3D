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

//external imports
import java.awt.Color;
import java.awt.Font;

import java.text.NumberFormat;

import java.util.ArrayList;

import java.util.prefs.Preferences;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.LineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.TransformGroup;

// internal imports
import org.chefx3d.model.*;

import org.chefx3d.preferences.SessionPreferenceConstants;

import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * This class manages the NearestNeighbor overlay component.
 * It is an overlay component with four lines: one to the north, 
 * one to the south, one to the east, and one to the west.  It is
 * currently unsafe, since there are no checks on the .drawLines() 
 * method to ensure that enough coordinates are being passed in to
 * draw the four lines. 
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.18 $
 */
public class NearestNeighborOverlay extends OverlayComponent {
	
	/** Default color for the line (black) */
	private static final Color DEFAULT_LINE_COLOR = Color.black;
	
	/** Active color for the line (blue) */
	private static final Color ACTIVE_LINE_COLOR = Color.blue;
	
	/** transparency to use for the grid lines */
	private static final float TRANSPARENCY = 1f;
	
	/** Default Font for the text label */
	private static final Font DEFAULT_TEXT_FONT = new Font("serif", Font.PLAIN, 10);
	
	/** Default color for the fonts */
	private static final Color DEFAULT_TEXT_COLOR = Color.black;
	
	/** Number of directional indicators */
	private static final int NUM_DIRECTION = 4;
	
	/** Directional indices */
	private static final int NORTH = 0;
	private static final int SOUTH = 1;
	private static final int EAST = 2;
	private static final int WEST = 3;
	
	/** Directional labels */
	private static final String[] LABEL = new String[]{"north", "south", "east", "west"};
	
	/** Nearest Neighbor depth position in the scene.  */
	private static final float NEAREST_NEIGHBOR_Z_POS = -1f;
	
	/** A 'nudge' factor so the labels don't overlap the lines */
	private static final int X = 2;
	
	/** A 'nudge' factor so the labels don't overlap the lines */
	private static final int Y = 8;
	
	/** The appearance to use when drawing lines */
	private Appearance defaultAppearance;
	private Appearance activeAppearance;
	
	/** the screen values (of overlay layer) of the
	* start and end tape measure points.      */
	private float[][] vertices;
		
	/** the ratio of pixels to meters - IE, divide the tape measure vector
	* by this number to get the length of the tape measure in meters. */
	private float pixelsPerMeter;
	
	/** Switches */
	private SwitchGroup[] switchGroup;
	
	/** Line Geometry */
	private LineArray[] line;
	
	/** Line Shape */
	private Shape3D[] lineShape;
	
	/** Text TransformGroup */
	private TransformGroup[] textTG;
	
	/** Text Shape */
	private LegendText[] text;
	
	/** Distances along each line */
	private float[] length;
	
	/** The preferences accessor used to query session values. */
	private Preferences prefs;
	
	/** The active direction index */
	private int activeDirection;
	
	/** Scratch transform objs */
	private Matrix4f mtx;
	private Vector3f translation;
	
	/**
	 * Create a new instance of the TapeMeasure class
	 *
	 */
	public NearestNeighborOverlay(
		SceneManagerObserver mgmtObserverParam,
		AVGeometryBuilder avBuilderParam,
		Unit unitParam) {
		
		super(mgmtObserverParam, avBuilderParam, unitParam);
				
        prefs = Preferences.userRoot().node(
        		SessionPreferenceConstants.SESSION_PREFERENCES_NODE);
        
		int snapDirection = prefs.getInt(
			SessionPreferenceConstants.MEASUREMENT_DIRECTION_KEY, 
			SessionPreferenceConstants.DEFAULT_MEASUREMENT_DIRECTION);
		
		activeDirection = WEST;
		if (snapDirection == SessionPreferenceConstants.MEASUREMENT_DIRECTION_POSITIVE) {
			activeDirection = EAST;
		}
		
		mtx = new Matrix4f();
		translation = new Vector3f();
		
		initSceneGraph();
	}
	
	//----------------------------------------------------------
	// Methods defined by NodeUpdateListener
	//----------------------------------------------------------
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that only change the node's properties, but do not change the bounds.
	 *
	 * @param src The node or Node Component that is to be updated.
	 */
	public void updateNodeDataChanges(Object src){
		
		for (int i = 0; i < NUM_DIRECTION; i++) {
			if (src == lineShape[i]) {
				if (i == activeDirection) {
					lineShape[i].setAppearance(activeAppearance);
				} else {
					lineShape[i].setAppearance(defaultAppearance);
				}
			}
		}
	}
	
	/**
	 * Notification that its safe to update the node now with any operations
	 * that could potentially effect the node's bounds.
	 *
	 * @param src The node or Node Component that is to be updated.
	 */
	public void updateNodeBoundsChanges(Object src) {
		super.updateNodeBoundsChanges(src);
		
		for (int i = 0; i < NUM_DIRECTION; i++) {
			
			if (src == line[i]) {
				
				line[i].setVertices(LineArray.COORDINATE_3, vertices[i]);
				
			} else if (src == textTG[i]) {
				
				switch(i) {
				case NORTH:
				case SOUTH:
					translation.set(
						vertices[i][0] + X, 
						(vertices[i][1] + vertices[i][4])/2 + Y, 
						NEAREST_NEIGHBOR_Z_POS);
					break;
					
				case EAST:
				case WEST:
					translation.set(
						(vertices[i][0] + vertices[i][3])/2,
						vertices[i][1], 
						NEAREST_NEIGHBOR_Z_POS);
					break;
				}
				mtx.setIdentity();
        		mtx.setTranslation(translation);
				textTG[i].setTransform(mtx);
			}
		}
	}
	
	//----------------------------------------------------------
	// OverlayComponent Methods
	//----------------------------------------------------------
	
    /**
     * Show or hide the current component.  This method will call
     * SceneManagerObserver's requestBoundsUpdate() method to toggle
     * the SwitchGroup on and off.
     * @param show If TRUE, display the current component,
     * else hide the current component.
     */
    public void display(boolean show) {
        super.display(show);
        if (show) {
			int snapDirection = prefs.getInt(
				SessionPreferenceConstants.MEASUREMENT_DIRECTION_KEY, 
				SessionPreferenceConstants.DEFAULT_MEASUREMENT_DIRECTION);
			
			int direction = WEST;
			if (snapDirection == SessionPreferenceConstants.MEASUREMENT_DIRECTION_POSITIVE) {
				direction = EAST;
			}
			if (direction != activeDirection) {
				activeDirection = direction;
				mgmtObserver.requestDataUpdate(lineShape[EAST], this);
				mgmtObserver.requestDataUpdate(lineShape[WEST], this);
			}
        }
    }
    
	//----------------------------------------------------------
	// Local Methods
	//----------------------------------------------------------
	
	/**
	 * @param f the float array to use to draw the new grid. 
	 * Every six points defines the start and end point of a line.
	 * @param ratio is the ratio of pixels to meters - IE, divide
	 * by this number to get the length of the tape measure in meters
	 */
	void drawLines(
		Point3f northStart, Point3f northEnd,
		Point3f southStart, Point3f southEnd,
		Point3f eastStart, Point3f eastEnd,
		Point3f westStart, Point3f westEnd, double ratio){
		
		vertices[NORTH][0] = northStart.x;
		vertices[NORTH][1] = northStart.y;
		vertices[NORTH][2] = northStart.z;
		vertices[NORTH][3] = northEnd.x;
		vertices[NORTH][4] = northEnd.y;
		vertices[NORTH][5] = northEnd.z;
		
		vertices[SOUTH][0] = southStart.x;
		vertices[SOUTH][1] = southStart.y;
		vertices[SOUTH][2] = southStart.z;
		vertices[SOUTH][3] = southEnd.x;
		vertices[SOUTH][4] = southEnd.y;
		vertices[SOUTH][5] = southEnd.z;
		
		vertices[EAST][0] = eastStart.x;
		vertices[EAST][1] = eastStart.y;
		vertices[EAST][2] = eastStart.z;
		vertices[EAST][3] = eastEnd.x;
		vertices[EAST][4] = eastEnd.y;
		vertices[EAST][5] = eastEnd.z;
		
		vertices[WEST][0] = westStart.x;
		vertices[WEST][1] = westStart.y;
		vertices[WEST][2] = westStart.z;
		vertices[WEST][3] = westEnd.x;
		vertices[WEST][4] = westEnd.y;
		vertices[WEST][5] = westEnd.z;
		
		pixelsPerMeter = (float)ratio;
		
		length[NORTH] = (vertices[NORTH][4] - vertices[NORTH][1]) / pixelsPerMeter;
		length[SOUTH] = (vertices[SOUTH][1] - vertices[SOUTH][4]) / pixelsPerMeter;
		length[EAST] = (vertices[EAST][3] - vertices[EAST][0]) / pixelsPerMeter;
		length[WEST] = (vertices[WEST][0] - vertices[WEST][3]) / pixelsPerMeter;
		
		String unitText = " " + currentUnit.getLabel();
		
		String northText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
			UnitConversionUtilities.convertMetersTo(length[NORTH], currentUnit)) +
			unitText;
		text[NORTH].update(northText, 0, Anchor.TOP_LEFT, null);
		
		String southText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
			UnitConversionUtilities.convertMetersTo(length[SOUTH], currentUnit)) +
			unitText;
		text[SOUTH].update(southText, 0, Anchor.TOP_LEFT, null);
		
		String eastText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
			UnitConversionUtilities.convertMetersTo(length[EAST], currentUnit)) +
			unitText;
		text[EAST].update(eastText, 0, Anchor.TOP_LEFT, null);
		
		String westText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
			UnitConversionUtilities.convertMetersTo(length[WEST], currentUnit)) +
			unitText;
		text[WEST].update(westText, 0, Anchor.TOP_LEFT, null);
		
		display(true);
		
		mgmtObserver.requestBoundsUpdate(line[NORTH], this);
		mgmtObserver.requestBoundsUpdate(textTG[NORTH], this);
		
		mgmtObserver.requestBoundsUpdate(line[SOUTH], this);
		mgmtObserver.requestBoundsUpdate(textTG[SOUTH], this);
		
		mgmtObserver.requestBoundsUpdate(line[EAST], this);
		mgmtObserver.requestBoundsUpdate(textTG[EAST], this);
		
		mgmtObserver.requestBoundsUpdate(line[WEST], this);
		mgmtObserver.requestBoundsUpdate(textTG[WEST], this);
	}
	
	/**
	 * Initialize the scene graph components
	 */
	private void initSceneGraph() {
		
		LineAttributes line_attributes = new LineAttributes();
		line_attributes.setLineWidth(1);
        line_attributes.setStipplePattern(LineAttributes.PATTERN_SOLID);
		
		float[] default_color = DEFAULT_LINE_COLOR.getRGBComponents(null);
		
		Material default_material = new Material();
		default_material.setDiffuseColor(default_color);
    	default_material.setEmissiveColor(default_color);
		
		defaultAppearance = new Appearance();
		defaultAppearance.setMaterial(default_material);
		defaultAppearance.setLineAttributes(line_attributes);
		
		float[] active_color = ACTIVE_LINE_COLOR.getRGBComponents(null);
		
		Material active_material = new Material();
		active_material.setDiffuseColor(active_color);
    	active_material.setEmissiveColor(active_color);
		
		activeAppearance = new Appearance();
		activeAppearance.setMaterial(active_material);
		activeAppearance.setLineAttributes(line_attributes);
		
		//
		// add the component geometry to the root grouping node  
		//
		vertices = new float[NUM_DIRECTION][];
		length = new float[NUM_DIRECTION];
		switchGroup = new SwitchGroup[NUM_DIRECTION];
		line = new LineArray[NUM_DIRECTION];
		lineShape = new Shape3D[NUM_DIRECTION];
		textTG = new TransformGroup[NUM_DIRECTION];
		text = new LegendText[NUM_DIRECTION];
		
		for (int i = 0; i < NUM_DIRECTION; i++) {
			
			vertices[i] = new float[6];
			
			switchGroup[i] = new SwitchGroup();
			
			Group group = new Group();
			lineShape[i] = new Shape3D();
			line[i] = new LineArray();
			lineShape[i].setGeometry(line[i]);
			if (i == activeDirection) {
				lineShape[i].setAppearance(activeAppearance);
			} else {
				lineShape[i].setAppearance(defaultAppearance);
			}
			
			group.addChild(lineShape[i]);
			
			text[i] = new LegendText(mgmtObserver, false);
			text[i].setFont(DEFAULT_TEXT_FONT);
			text[i].setTextColor(DEFAULT_TEXT_COLOR);
			text[i].update(LABEL[i], 0, Anchor.TOP_LEFT, null);
			
			textTG[i] = new TransformGroup();
			textTG[i].addChild(text[i].getShape());
			
			group.addChild(textTG[i]);
			
			switchGroup[i].addChild(group);
			switchGroup[i].setActiveChild(0);
			
			compGroup.addChild(switchGroup[i]);
		}
	}
}