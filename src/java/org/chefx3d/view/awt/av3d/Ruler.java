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

// external imports
import java.awt.Color;
import java.awt.Font;

import java.text.NumberFormat;

import java.util.ArrayList;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.LineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.QuadArray;
import org.j3d.aviatrix3d.TransformGroup;

// internal imports
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * A class to manage rulers.
 * @author Eric Fickenscher
 * @version $Revision: 1.46 $
 */
public class Ruler extends OverlayComponent {

	/** A list of the available ruler orientations */
    public enum Orientation { VERTICAL, HORIZONTAL };
    
    /** numbers to use for growing the 'units per mark' values when scaling */
    private int[] scale;
    
    /** Maximum number of pixels between hash marks */
    private static final int MAX_PIXELS_BETWEEN_HASH_MARKS = 180;

    /** Minimum number of pixels between hash marks */
    private static final int MIN_PIXELS_BETWEEN_HASH_MARKS = 5;
    
    /** Minimum number of pixels between hash marks */
    private static final int MIN_PIXELS_BETWEEN_LABELS = 30;
    
    /** The length, in pixels, of the labeled hash marks */  
    private static final int LABELED_HASH_MARK_LENGTH = 12;
    
    /** The length, in pixels, of the unlabeled hash marks */
    private static final int UNLABELED_HASH_MARK_LENGTH = 2;
    
    /** The pixel thickness of a ruler.  Note that this can be height, in the case
     * of a horizontally-aligned ruler, or it can be width, in the case of a
     * vertically-aligned ruler.  It is the compliment to pixelsAcross */
    public static final int RULER_FATNESS = 15;
    
    /** Label depth position in the scene.  Should be greater than the Z-
     * position of the box, so that it appears in front of the box. */
    private static final float HASH_MARK_Z_POS = -2f;
    
    /** Box POSITIONAL depth in the scene.  Should be less than the
     * hash mark Z-position in the scene, so that it appears behind
     * the hash marks.     */
    private static final float BOX_Z_POS = -15;
    
    /** Default color for the ruler background */
    private static final float[] DEFAULT_BACKGROUND_COLOR =
        Color.white.getComponents(new float[6]);

    /** Default diffuse color for the hash marks.  */
    private static final Color DIFFUSE_HASHMARK_COLOR = Color.black;

    /** Default emissive color for the hash marks.  */
    private static final Color EMISSIVE_HASHMARK_COLOR = Color.black;
    
    /** transparency to use for the hash marks of the ruler */
    private static final float TRANSPARENCY = 1f;
    
    /** Default Font for the measurement shorthand */
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 10);

    /** Default color for the fonts */
    private static final Color DEFAULT_FONT_COLOR = Color.black;

	/** orientation of the current ruler */
    private Orientation orientation;
        
    /** Current style of number formatting desired */
    private NumberFormat formatter;
    
    /** If the ruler is horizontal, how many pixels wide is the scene?
     *  If the ruler is vertical, how many pixels high is the scene?  */
    private int screenPixels;
    
    /** If the ruler is horizontal, how many meters wide is the scene?  
     * If the ruler is vertical, how many meters high is the scene?  */ 
    private float screenMeters;
    
    /** The coordinates, in meters, of the center of the scene. */
    private float screenCenter;
    
    /** The coordinates, in meters, of the start point of the ruler.
     * Should normally be equal to screenCenter - screenMeters/2   */
    private float screenStart;
    
    /** The coordinates, in meters, of the end point of the ruler.
     * Should normally be equal to screenCenter + screenMeters/2   */
    private float screenEnd;
    
    /** The coordinates, in meters, of the start point of the ruler. */
    private float rulerStart;
    
    /** The coordinates, in meters, of the end point of the ruler. */
    private float rulerEnd;
    
    /** The coordinates, in pixels, of the start point of the ruler. */
    private float rulerStartInPixels;
    
    /** The coordinates, in pixels, of the end point of the ruler. */
    private float rulerEndInPixels;
    
    /** working variable for the line array used to draw hash marks */
    private float[] marks;
    
    /** working variable for the string array used to label the hash marks. */
	private ArrayList<String> labels;
    
    /** working variable for the array used to position the hash-mark-labels. */
	private ArrayList<Float> labelPixelPositions;
    
    /** If TRUE, ruler will draw from one side of the 
     * screen to the other.  Only FALSE if the ruler is being 
     * drawn from one specific starting point to a specific end point. */
    private boolean rulerShouldCoverScreen;
    
    /** Length of hash marks, in pixels. Set during the label(float) method. */
    private int hashMarkPixelLength;
    
    /** pixels per meter */
    private float pixelsPerMeter;
    
    /** an array of the appropriate measurement units */
    private Unit[] measurement;
    
    /** the number of meters between each hash mark */
    private float metersPerMark;
    
    /** the number of meters between each labeled hash mark */
    private float metersPerLabel;
    
    /** What is the current labeled unit of measurement? */ 
    private Unit labeledUnit;
    
    /** Number of units between each labeled hash mark */
    private int unitsPerLabel;
    
	/** Hashmark geometry */
	private LineArray line_geom;
	
	/** Ruler background geometry */
	private QuadArray box_geom;
	
	/** Vertices for the ruler background geometry */
	private float[] box_vertices;
	
	/** Grouping node containing the text labels */
	private Group textGroup;
	
	/** Array list of text nodes to be added */
	private ArrayList<LegendText> textAddList;
	
	/** Array list of text nodes currently in the scene */
	private ArrayList<LegendText> textCurrentList;
	
	/** Array list of text nodes to be removed */
	private ArrayList<LegendText> textRemoveList;
	
	/** Scratch translation for text updates */
	private float[] offset;
	
    /**
     * Constructor
     * @param mgmtObserverParam The SceneManagerObserver
     * @param avBuilderParam Utility tool for building common Aviatrix shapes.
     * @param orientationParam is the ruler oriented horizontally or
     * vertically?
     * @param availableScreenSizeInPixels If the ruler is horizontal,
     * how many pixels wide is the scene?  If the ruler is vertical,
     * how many pixels high is the scene?
     * @param screenSizeInMeters If the ruler is horizontal, how many
     * meters wide is the scene?  If the ruler is vertical, how many
     * meters high is the scene? 
     * @param screenCenterPointInMeters What are the coordinates, in
     * meters, of the center of the scene? 
     */
	public Ruler(SceneManagerObserver mgmtObserverParam,
				 AVGeometryBuilder avBuilderParam,
				 Unit unitParam,
				 Orientation orientationParam,
				 int availableScreenSizeInPixels,
				 float screenSizeInMeters,
				 float screenCenterPointInMeters){
		
		super(mgmtObserverParam, avBuilderParam, unitParam);

		//
		// initializations
		//
		orientation = orientationParam;
		screenPixels = availableScreenSizeInPixels;
		screenMeters = screenSizeInMeters;
		screenCenter = screenCenterPointInMeters;		
		screenStart = screenCenter - screenMeters/2;
		screenEnd = screenCenter + screenMeters/2;
		rulerStart = screenStart -1; 
		rulerEnd = screenEnd + 1;
		formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
		labels = new ArrayList<String>();
		labelPixelPositions = new ArrayList<Float>();
		rulerShouldCoverScreen = true;
		
		String currentSystem = UnitConversionUtilities.getMeasurementSystem(currentUnit);
		if (currentSystem == UnitConversionUtilities.IMPERIAL) {
			
		    measurement = new Unit[]{
				Unit.INCHES, 
				Unit.FEET, 
				Unit.MILES};
			
            scale = new int[]{1, 2, 3, 4, 12, 24, 120, 240, 480 };
			
		} else {
			
            measurement = new Unit[]{ 
				Unit.MILLIMETERS, 
				Unit.CENTIMETERS, 
                Unit.METERS, 
				Unit.KILOMETERS};
			
            scale = new int[]{ 1, 2, 5, 10, 25, 50, 100, 250, 500 };
		}
		
		offset = new float[3];
	
		textAddList = new ArrayList<LegendText>();
		textCurrentList = new ArrayList<LegendText>();
		textRemoveList = new ArrayList<LegendText>();
		
		initSceneGraph();
	}

    //------------------------------------------------------------------------
    // Methods defined by OverlayComponent, overwritten here:
    //------------------------------------------------------------------------
	
    /**
     * Redraw this component.
     */
    public void redraw(){
        
        if (enabled) {
            display(true);
			
			buildLineSetForHashMarks();
			mgmtObserver.requestBoundsUpdate(line_geom, this);
			
			buildBoxVertices();
			mgmtObserver.requestBoundsUpdate(box_geom, this);
			
			createLabels();
        }
    }
	
    /**
     * Set a new unit of measurement.  This supports toggling between
     * different units of measurement, such as centimeters and inches.
     * Recommended that an updateNodeBoundsChanges(compGroup) is called
     * immediately afterward, so that the new units will display.
     * @param newUnit the new unit of measurement.
     */
    public void setUnitOfMeasurement(Unit newUnit){
		
    	super.setUnitOfMeasurement(newUnit);
		
    	measurement = new Unit[]{newUnit};
    	redraw();
    }
	
    //------------------------------------------------------------------------
    // Methods defined by NodeUpdateListener
    //------------------------------------------------------------------------
	
    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {
    	
    	super.updateNodeBoundsChanges(src);
		
		if (enabled) { 
			if (src == textGroup) {
				
				int numToAdd = textAddList.size();
				if (numToAdd > 0) {
					for (int i = 0; i < numToAdd; i++) {
						LegendText text = textAddList.get(i);
						textGroup.addChild(text.getShape());
					}
					textCurrentList.addAll(textAddList);
					textAddList.clear();
				}
				int numToRemove = textRemoveList.size();
				if (numToRemove > 0) {
					for (int i = 0; i < numToRemove; i++) {
						LegendText text = textRemoveList.get(i);
						textGroup.removeChild(text.getShape());
					}
					textRemoveList.clear();
				}
			} else if (src == line_geom) {
				
				line_geom.setVertices(LineArray.COORDINATE_3, marks);
				
			} else if (src == box_geom) {
				
				box_geom.setVertices(QuadArray.COORDINATE_3, box_vertices, 4);
			}
		}
    }

    //-------------------------------------------------------------------------
    // Local Methods
    //-------------------------------------------------------------------------
    
    /**
     * @return the current labeled unit of measurement.
     */
    public Unit getUnit(){
    	return labeledUnit;
    }
	
	/**
	 * Draw the ruler from one specific point to another specific point.
	 * The ruler may happen to cover the screen, but it may not be visible
	 * at all if user pans far away from the ruler segment.  This method is
	 * the only way to set "rulerShouldCoverScreen" to FALSE; the only way to
	 * reset that variable so that the ruler will _always_ cover the entire
	 * screen is to call the method drawFullRuler();
	 * @param begin The coordinates, in meters, of the origin of the ruler.
	 * @param end The coordinates, in meters, of the end of the ruler.
	 */
	public void setRulerStartAndEnd(float begin, float end){
		rulerShouldCoverScreen = false;
		rulerStart = begin;
		rulerEnd = end;
	}
	
	/**
	 * 
	 * @param screenCenterPointInMeters The new center point of viewable space
	 */
	public void setScreenCenter(float screenCenterPointInMeters){
		screenCenter = screenCenterPointInMeters;
		screenStart = screenCenter - screenMeters/2;
		screenEnd = screenCenter + screenMeters/2;
		if (rulerShouldCoverScreen) {
			rulerStart = (float)Math.floor(screenStart);
			rulerEnd = (float)Math.ceil(screenEnd);
		}
	}
	
	/**
	 * This method is the only way to set "rulerShouldCoverScreen" to TRUE;
	 * if one wants to draw a ruler segment instead then one should call the 
	 * method setRulerStartAndEnd(float begin, float end);
	 */
	public void drawFullRuler(){
		rulerShouldCoverScreen = true;
	}
	
	/**
	 * 
	 * @param availableScreenSizeInPixels
	 */
	public void setScreenPixelSize(int availableScreenSizeInPixels){
		screenPixels = availableScreenSizeInPixels;
	}
	
	/**
	 * 
	 * @param screenSizeInMeters
	 */
	public void setScreenSizeMeters(float screenSizeInMeters){
		screenMeters = screenSizeInMeters;
		screenStart = screenCenter - screenMeters/2;
		screenEnd = screenCenter + screenMeters/2;
		if (rulerShouldCoverScreen) {
			rulerStart = (float)Math.floor(screenStart);
			rulerEnd = (float)Math.ceil(screenEnd);
		}
	}
	
	/**
	 * Call this method to calculate the ideal meters per hash mark
	 * and meters per labeled hash mark.
	 */
	private void calculateMetersBetweenHashMarks(){
		
		float pixelsPerHashMark;
		float unitsAcross;
		
		float minPixelsPer = 500000; // arbitrary initialization
		int minMeasurementIndex = -1;
		int minScaleIndex = -1;
		int labelMeasurementIndex = -1;
		int labelScaleIndex = -1;
		
		//
		// iterate through all the possible measurement/scale
		// combinations and find the one that fits best in our
		// desired range
		//
		for (int i = 0; i < measurement.length; i++) {

			unitsAcross = UnitConversionUtilities.convertMetersTo(
				screenMeters,
				measurement[i]);
			
			for (int j = 0; j < scale.length; j++) {
				pixelsPerHashMark = screenPixels / (unitsAcross / scale[j]);
				
				if (pixelsPerHashMark >= MAX_PIXELS_BETWEEN_HASH_MARKS) {
					break;
				}

				if (pixelsPerHashMark >= MIN_PIXELS_BETWEEN_HASH_MARKS) {
					if (pixelsPerHashMark <= minPixelsPer){
						minPixelsPer = pixelsPerHashMark;
						minMeasurementIndex = i;
						minScaleIndex = j;
					}
				}
				
				if (pixelsPerHashMark >= MIN_PIXELS_BETWEEN_LABELS ) {
					labelMeasurementIndex = i;
					labelScaleIndex = j;
					break;
				}
			}
		}
		//
		// sometimes we can't zoom enough.  This code limits 
		// index-out-of-bounds errors by trying to halve the measurement
		// 
		if (labelScaleIndex < 0) {
			float[] fraction = new float[]{0.5f, 0.25f, 0.125f, 0.0625f};
			
			for (int i = 0; i < measurement.length; i++) {

				unitsAcross = UnitConversionUtilities.convertMetersTo(
					screenMeters, 
					measurement[i]);
				
				for (int j = 0; j < fraction.length; j++) {
					pixelsPerHashMark = screenPixels / (unitsAcross / fraction[j]);
					
					if (pixelsPerHashMark >= MAX_PIXELS_BETWEEN_HASH_MARKS) {
						break;
					}

					if (pixelsPerHashMark >= MIN_PIXELS_BETWEEN_HASH_MARKS) {
						if (pixelsPerHashMark <= minPixelsPer) {
							minPixelsPer = pixelsPerHashMark;
							minMeasurementIndex = i;
							minScaleIndex = j;
						}
					}
					
					if (pixelsPerHashMark >= MIN_PIXELS_BETWEEN_LABELS) {
						labelMeasurementIndex = i;
						labelScaleIndex = j;
						break;
					}
				}
			}
		}	
		
		// make sure none of the indexes are ever -1 
        if (minScaleIndex < 0) {
            minScaleIndex = 0;
        }
        if (minMeasurementIndex < 0) {
            minMeasurementIndex = 0;
        }
        if (labelScaleIndex < 0) {
            labelScaleIndex = 0;
        }
        if (labelMeasurementIndex < 0) {
            labelMeasurementIndex = 0;
        }
		
		metersPerMark = UnitConversionUtilities.convertUnitsToMeters(
			scale[minScaleIndex], 
	        measurement[minMeasurementIndex]);
		
		metersPerLabel = UnitConversionUtilities.convertUnitsToMeters(
			scale[labelScaleIndex], 
			measurement[labelMeasurementIndex]); 
		
		labeledUnit = measurement[labelMeasurementIndex];
		unitsPerLabel = scale[labelScaleIndex];
	}
	
	/**
	 * Builds three arrays: one float array containing data used
	 * to create the line set for the hash marks, one String array
	 * containing the values to use for the labels, and one float 
	 * array used to position the labels.
	 */
	private void buildLineSetForHashMarks(){
		
		//
		// figure out the units and scale we are going to use
		//
		float units = rulerStart;
		calculateMetersBetweenHashMarks();
				
		//
		// to get an even starting value for our 'units' variable, divide
		// it by scale, floor that value and re-multiply by unitsPerLabel:
		//
		float rulerStartInLabeledUnits = 
			UnitConversionUtilities.convertMetersTo(rulerStart, labeledUnit);
		
		rulerStartInLabeledUnits /= unitsPerLabel;
		
		rulerStartInLabeledUnits = 
			(float)Math.floor(rulerStartInLabeledUnits) * unitsPerLabel;
		
		rulerStartInLabeledUnits = UnitConversionUtilities.convertUnitsToMeters(
			rulerStartInLabeledUnits, 
			labeledUnit);
		
		units = rulerStartInLabeledUnits; 

		//
		// variables to track pixel positioning
		//
		pixelsPerMeter =  screenPixels / screenMeters;
		rulerStartInPixels = (units - screenStart) * pixelsPerMeter;
		float pixelPosition = rulerStartInPixels;
		rulerEndInPixels = (rulerEnd - screenStart) * pixelsPerMeter;
		float pixelsBetweenMarks = pixelsPerMeter * metersPerMark;
		
		//
		// set the arrays to the proper size
		//
		int numberOfMarks = (int)(1 +
			(rulerEndInPixels - rulerStartInPixels) / pixelsBetweenMarks);
		
		marks = new float[numberOfMarks * 6];
		labels.clear();
		labelPixelPositions.clear();		
		
		switch(orientation) {
		case HORIZONTAL:
			
			for (int i = 0; i < marks.length; ){
				units = ((i/6) * metersPerMark) + rulerStartInLabeledUnits;
				if (label(units)){ // lot's o' formatting. for reals.
					labels.add(formatter.format(Math.round(
						UnitConversionUtilities.convertMetersTo(units, labeledUnit))));
					labelPixelPositions.add(pixelPosition);
				}
				
				marks[i++] = (float)((int)pixelPosition);
				marks[i++] = 0;
				marks[i++] = HASH_MARK_Z_POS;
				marks[i++] = (float)((int)pixelPosition);
				marks[i++] = hashMarkPixelLength;
				marks[i++] = HASH_MARK_Z_POS;
				pixelPosition += pixelsBetweenMarks;
			}
			
			break;
		case VERTICAL:
			
			for (int i = 0; i < marks.length; ){
				units = ((i/6) * metersPerMark) + rulerStartInLabeledUnits;
				if (label(units)){
					labels.add(formatter.format(
						Math.round(UnitConversionUtilities.convertMetersTo(units, labeledUnit))));
					labelPixelPositions.add(pixelPosition);
				}
				
				marks[i++] = 0;
				marks[i++] = (float)((int)pixelPosition);
				marks[i++] = HASH_MARK_Z_POS;
				marks[i++] = hashMarkPixelLength;
				marks[i++] = (float)((int)pixelPosition);
				marks[i++] = HASH_MARK_Z_POS;
				pixelPosition += pixelsBetweenMarks;
			}
			break;
		}
	}
	
	/**
	 * If it is an appropriate place to draw a label, make the
	 * hash mark length longer, otherwise draw a short tic mark.
	 * Return a boolean value: Should we label this coordinate?
	 * @param coordinate
	 * @return
	 */
	private boolean label(float coordinate){
		
		float modVal = Math.abs(coordinate % metersPerLabel);
		float closeness = .0001f; 
		
		boolean labelHashMark;
		if ((modVal < closeness) || (modVal > (metersPerLabel - closeness))){
			hashMarkPixelLength = LABELED_HASH_MARK_LENGTH;
			labelHashMark = true;
			
		} else{
			hashMarkPixelLength = UNLABELED_HASH_MARK_LENGTH;
			labelHashMark = false;
		}
		return(labelHashMark);
	}
	
    /**
     * @return a group node containing a bunch of transform groups
     * containing text shapes for the horizontal ruler
     */
	private void createLabels() {
		
		boolean doUpdate = false;
		int num_current = textCurrentList.size();
		int num_labels = labels.size();
        for (int i = 0; i < num_labels; i++){

			LegendText text = null;
			if (i < num_current) {
				text = textCurrentList.get(i);
			} else {
				text = new LegendText(mgmtObserver, false);
				text.setFont(LABEL_FONT);
				text.setTextColor(DEFAULT_FONT_COLOR);
				textAddList.add(text);
				doUpdate = true;
			}
			if (orientation == Orientation.HORIZONTAL) {
				offset[0] = labelPixelPositions.get(i) + 2;
				offset[1] = 12;
				offset[2] = -5;
			} else {
				offset[0] = 2;
				offset[1] = labelPixelPositions.get(i) - 2;
				offset[2] = -5;
			}
			text.update(labels.get(i), 0, Anchor.TOP_LEFT, offset);
        }
		if (num_current > num_labels) {
			for (int i = num_current - 1; i >= num_labels; i--) {
				LegendText text = textCurrentList.remove(i);
				textRemoveList.add(text);
			}
			doUpdate = true;
		}
		if (doUpdate) {
			mgmtObserver.requestBoundsUpdate(textGroup, this);
		}
    }
	
	/**
	 * Calculate the box vertices
	 */
	private void buildBoxVertices() {
		
		float x_min, x_max, y_min, y_max;
		
		if (orientation == Orientation.HORIZONTAL) {
			
			x_min = rulerStartInPixels;
			x_max = rulerEndInPixels;
			
			y_min = 0;
			y_max = RULER_FATNESS;
			
		} else {
			
			x_min = 0;
			x_max = RULER_FATNESS;
			
			y_min = rulerStartInPixels;
			y_max = rulerEndInPixels;
		}
		
		box_vertices[0] = x_min;
		box_vertices[1] = y_min;
		
		box_vertices[3] = x_max;
		box_vertices[4] = y_min;
		
		box_vertices[6] = x_max;
		box_vertices[7] = y_max;
		
		box_vertices[9] = x_min;
		box_vertices[10] = y_max;
	}
	
	/**
	 * Initialize the scene graph components
	 */
	private void initSceneGraph() {
		
		// create hashmark geometry
        float[] diffuse = DIFFUSE_HASHMARK_COLOR.getRGBComponents(null);
		float[] emissive = EMISSIVE_HASHMARK_COLOR.getRGBComponents(null);
		
		Material line_material = new Material();
		line_material.setDiffuseColor(diffuse);
    	line_material.setEmissiveColor(emissive);
		
		LineAttributes line_attributes = new LineAttributes();
		line_attributes.setLineWidth(1);
        line_attributes.setStipplePattern(LineAttributes.PATTERN_SOLID);
		
		Appearance line_appearance = new Appearance();
		line_appearance.setMaterial(line_material);
		line_appearance.setLineAttributes(line_attributes);
		
		line_geom = new LineArray();
		buildLineSetForHashMarks();
        line_geom.setVertices(LineArray.COORDINATE_3, marks);
		
		Shape3D line_shape = new Shape3D();
		line_shape.setAppearance(line_appearance);
		line_shape.setGeometry(line_geom);
        
		compGroup.addChild(line_shape);
		
		// create ruler background geometry
		Material box_material = new Material();
		box_material.setDiffuseColor(DEFAULT_BACKGROUND_COLOR);
    	box_material.setEmissiveColor(DEFAULT_BACKGROUND_COLOR);
		
		Appearance box_appearance = new Appearance();
		box_appearance.setMaterial(box_material);
		
		box_vertices = new float[12];
		box_vertices[2] = BOX_Z_POS;
		box_vertices[5] = BOX_Z_POS;
		box_vertices[8] = BOX_Z_POS;
		box_vertices[11] = BOX_Z_POS;
		
		rulerStartInPixels = 0;
		rulerEndInPixels = screenPixels;
		buildBoxVertices();
		
		float[] box_normals = new float[] { 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1, 
			0, 0, 1};
		
		box_geom = new QuadArray();
		box_geom.setVertices(QuadArray.COORDINATE_3, box_vertices, 4);
		box_geom.setNormals(box_normals);
		
		Shape3D box_shape = new Shape3D();
		box_shape.setAppearance(box_appearance);
		box_shape.setGeometry(box_geom);
		
		compGroup.addChild(box_shape);
		
		textGroup = new Group();
		
		compGroup.addChild(textGroup);
		
		// create text labels	
		createLabels();
	}
}