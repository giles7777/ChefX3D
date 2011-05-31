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
import java.awt.Graphics2D;

import java.text.NumberFormat;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.SwitchGroup;
import org.j3d.aviatrix3d.LineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.NodeUpdateListener;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;

// internal imports
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * This class manages the TapeMeasure
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.15 $
 */
public class TapeMeasure extends OverlayComponent {
	
	/** Default diffuse color for the line (black) */
	private static final Color DEFAULT_LINE_COLOR = Color.black;
	
    /** Default Font for the text label */
    private static final Font DEFAULT_TEXT_FONT = new Font("serif", Font.PLAIN, 10);

    /** Default color for the fonts */
    private static final Color DEFAULT_TEXT_COLOR = Color.black;
	
    /** Tape measure depth position in the scene.  */
    private static final float TAPE_MEASURE_Z_POS = -1f;
    
    /** How long should the endcaps of the tape measure be? */
    private static final int ENDCAP_PIXEL_LENGTH = 10; 
    
    /** Are we in the middle of placing a tape measure? */
    private boolean measuring;
    
    /** the ratio of pixels to meters - IE, divide the tape measure vector
    * by this number to get the length of the tape measure in meters. */
    private float pixelsPerMeter;
    
    /** The Vector3f from one end of the tape to the other */
    private Vector3f tapeVector;
    
    /** length of the tape */
    private float tapeLength;
    
    /** the transform holding the text object! */
    private TransformGroup textTransform;
    
	/** text object */
	private LegendText text;
   
    /** the screen values (of overlay layer) of the
     * start and end tape measure points.      */ 
    private float[] line_vertices;
    
	/** Line geometry */
	private LineArray line_geom;
	
	/** Scratch vecmath objects */
    private Vector3f zVector;
	private Vector3f perpendicularVec;
	private Point3f centerOfPerpendicularVec;
	private Vector3f distanceFromTapeVectorStart;
	private Vector3f distanceFromTapeVectorEnd;
	private Matrix4f mtx;
	private Vector3f translation;
	
    /**
     * Create a new instance of the TapeMeasure class
     *
     */
    public TapeMeasure(SceneManagerObserver mgmtObserverParam,
    				   AVGeometryBuilder avBuilderParam,
    				   Unit unitParam){
        super(mgmtObserverParam, avBuilderParam, unitParam);
    	
    	zVector = new Vector3f();
		perpendicularVec = new Vector3f();
		centerOfPerpendicularVec = new Point3f();
		distanceFromTapeVectorStart = new Vector3f();
		distanceFromTapeVectorEnd = new Vector3f();
		mtx = new Matrix4f();
		translation = new Vector3f();
		
    	tapeVector = new Vector3f();
    	tapeLength = 0;
    	
		initSceneGraph();
		
    	measuring = false;
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
    }
    
    /**
     * Notification that its safe to update the node now with any operations
     * that could potentially effect the node's bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeBoundsChanges(Object src) {
    	super.updateNodeBoundsChanges(src);
    	
    	if (src == line_geom){
    		
    		//
    		// cross products give us a perpendicular vector.
    		// Since we are dealing with a two-dimensional plane (x and y)
    		// we choose a vector going down the z-axis so that the cross
    		// product will produce a perpendicular vector that remains in our
    		// two-dimensional x&y plane.
    		//
    		zVector.set(0, 0, 1);
    		perpendicularVec.cross(tapeVector, zVector);
    		perpendicularVec.scale(ENDCAP_PIXEL_LENGTH);
    		    		
    		centerOfPerpendicularVec.set(
    				perpendicularVec.x * 0.5f,
    				perpendicularVec.y * 0.5f,
    				perpendicularVec.z * 0.5f);
    		
    		distanceFromTapeVectorStart.set(
    				line_vertices[0] - centerOfPerpendicularVec.x,
    				line_vertices[1] - centerOfPerpendicularVec.y,
    				line_vertices[2] - centerOfPerpendicularVec.z );
			
    		distanceFromTapeVectorEnd.set(
    				line_vertices[3] - centerOfPerpendicularVec.x,
    				line_vertices[4] - centerOfPerpendicularVec.y,
    				line_vertices[5] - centerOfPerpendicularVec.z );
    		
    		float[] f = new float[] {
    				line_vertices[0],	// the start of the tape measure
    				line_vertices[1], 
    				TAPE_MEASURE_Z_POS, 
    				line_vertices[3], // the end of the tape measure
    				line_vertices[4], 
    				TAPE_MEASURE_Z_POS, 
    				distanceFromTapeVectorStart.x, // the first end segment
    				distanceFromTapeVectorStart.y, 
    				TAPE_MEASURE_Z_POS, 
    				perpendicularVec.x + distanceFromTapeVectorStart.x, 
    				perpendicularVec.y + distanceFromTapeVectorStart.y, 
    				TAPE_MEASURE_Z_POS,
    				distanceFromTapeVectorEnd.x, // the second end segment
					distanceFromTapeVectorEnd.y, 
					TAPE_MEASURE_Z_POS, 
					perpendicularVec.x + distanceFromTapeVectorEnd.x, 
					perpendicularVec.y + distanceFromTapeVectorEnd.y, 
					TAPE_MEASURE_Z_POS
    		};
    		
			line_geom.setVertices(LineArray.COORDINATE_3, f);
    		
    	} else if (src == textTransform){

    		//
    		// now position the text label; if text would be obscured by the line,
    		// then bump out the text a wee little bit so they don't overlap
    		//
    		int xBump = 0;
    		int yBump = 0;
    		if (tapeVector.y != 0){
    			if ((tapeVector.x / tapeVector.y ) < 0){
		    		xBump = 10;
    			}
    			if ((tapeVector.x / tapeVector.y ) < -1){
    				xBump = -10;
    				yBump = -10;
    			}
    		}
			
			translation.set(
				(line_vertices[3] + line_vertices[0]) * 0.5f + xBump,
    			(line_vertices[4] + line_vertices[1]) * 0.5f + yBump,
    			TAPE_MEASURE_Z_POS);
			mtx.setIdentity();
        	mtx.setTranslation(translation);
    		textTransform.setTransform(mtx);
    	}
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------
   
    /**
     * This method updates the start-point of the tape measure and so should
     * be called every time a new tape measure is drawn.  
     * It changes variable measuring to TRUE, so method isMeasuring()
     * will return true after this is called.  
     * @param worldPos a float array with length 2 corresponding to the 
     * overlay layer's view frustum such that worldPos[0] represents the 
     * x-value, and worldPos[1] represents the y-value.
     */
    public void startTape(float[] worldPos){
    	line_vertices[0] = worldPos[0];
        line_vertices[1] = worldPos[1];
        measuring = true;
    }
    
    /**
     * This method updates the end-point of the tape measure, but it does 
     * NOT change variable measuring, thus making it useful for transitional
     * tape measure usage.
     * @param worldPos a float array with length 2 corresponding to the 
     * overlay layer's view frustum such that worldPos[0] represents the 
     * x-value, and worldPos[1] represents the y-value.
     */
    public void continueTape(float[] worldPos){
    	line_vertices[3] = worldPos[0];
        line_vertices[4] = worldPos[1];
    }
    
    /**
     * This method updates the end-point of the tape measure.
     * It method sets variable measuring to false, so method isMeasuring()
     * will return false after this is called.  This method should be
     * called when a tape measure is completed (ie, when we expect the next 
     * usage of the tape measure to begin from a fresh starting point).
     * @param worldPos a float array with length 2 corresponding to the 
     * overlay layer's view frustum such that worldPos[0] represents the 
     * x-value, and worldPos[1] represents the y-value.
     */
    public void endTape(float[] worldPos){
    	line_vertices[3] = worldPos[0];
        line_vertices[4] = worldPos[1];
        measuring = false;
    }
    
    /**
     * Show or hide the tape measure.  This method will call
     * SceneManagerObserver's requestBoundsUpdate() method to toggle
     * the SwitchGroup on and off.
     * @param show If TRUE, display the current component,
     * else hide the current component.
     */
    public void display(boolean show){
        
        display = show;
        
        mgmtObserver.requestBoundsUpdate(switchGroup, this);
    }
    
    
    /**
     * Use the values in line_vertices to
     * update the geometry of the tape measure.
     * @param ratio is the ratio of pixels to meters - IE, divide
     * by this number to get the length of the tape measure in meters
     */
    public void drawMeasure(double ratio){
    	pixelsPerMeter = (float)ratio;
    	
    	if ( enabled ){
    		display(true);
			
    		tapeVector.set(line_vertices[3] - line_vertices[0],
    					   line_vertices[4] - line_vertices[1],
    					   line_vertices[5] - line_vertices[2]);

    		tapeLength = tapeVector.length() / pixelsPerMeter;
    		tapeVector.normalize();
			
			String lenText = UnitConversionUtilities.getFormatedNumberDisplay(
				currentUnit, 
				UnitConversionUtilities.convertMetersTo(tapeLength, currentUnit)) +
				" " + currentUnit.getLabel();
			text.update(lenText, 0, Anchor.TOP_LEFT, null);
			
    		mgmtObserver.requestBoundsUpdate(line_geom, this);
    		mgmtObserver.requestBoundsUpdate(textTransform, this);
		}
    }
    
    /**
     * 
     * @return TRUE if the tape measure has been started, FALSE if
     * a tape measure segment has already been drawn.
     */
    public boolean isStarted() {
    	return measuring;
    }
	
	/**
	 * Initialize the scene graph components
	 */
	private void initSceneGraph() {
		
    	//
    	// add the component geometry to the root grouping node  
    	//
		float[] diffuse = DEFAULT_LINE_COLOR.getRGBComponents(null);
		float[] emissive = DEFAULT_LINE_COLOR.getRGBComponents(null);
		
		Material line_material = new Material();
		line_material.setDiffuseColor(diffuse);
    	line_material.setEmissiveColor(emissive);
		
		LineAttributes line_attributes = new LineAttributes();
		line_attributes.setLineWidth(2);
        line_attributes.setStipplePattern(LineAttributes.PATTERN_DASH);
		
		Appearance line_appearance = new Appearance();
		line_appearance.setMaterial(line_material);
		line_appearance.setLineAttributes(line_attributes);
		
		line_geom = new LineArray();
		line_vertices = new float[]{0, 0, TAPE_MEASURE_Z_POS, 0, 0, TAPE_MEASURE_Z_POS};
        line_geom.setVertices(LineArray.COORDINATE_3, line_vertices);
		
		Shape3D line_shape = new Shape3D();
		line_shape.setAppearance(line_appearance);
		line_shape.setGeometry(line_geom);
        
    	compGroup.addChild(line_shape);
				
		text = new LegendText(mgmtObserver, false);
		text.setFont(DEFAULT_TEXT_FONT);
		text.setTextColor(DEFAULT_TEXT_COLOR);
		text.update("450", 0, Anchor.TOP_LEFT, null);
		
    	textTransform = new TransformGroup();
    	textTransform.addChild(text.getShape());
    	
    	compGroup.addChild(textTransform);
	}
}
