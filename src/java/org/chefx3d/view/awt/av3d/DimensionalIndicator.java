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

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.IndexedLineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;

// internal imports
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.av3d.LegendText.Anchor;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * This class manages the Dimensional Indicators
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.29 $
 */
public class DimensionalIndicator extends OverlayComponent {
	
	/** Default Font for the numeric labels */
	private static final Font DEFAULT_NUMBER_FONT = new Font("serif", Font.PLAIN, 10);
	
	/** Default diffuse color for the fonts (black) */
	private static final Color DIFFUSE_FONT_COLOR = Color.black;
	
	/** Default emissive color for the fonts (dark blue) */
	private static final Color EMISSIVE_FONT_COLOR = new Color(13, 30, 125);
	
	/** transparency to use for the dimensional indicators */
    private static final float TRANSPARENCY = 1f;
	
	/** Grid overlay depth position in the scene.  */
    private static final float DIMENSIONAL_INDICATOR_Z_POS = -10f;
	
    /** The distance, in pixels, the bars appear from the selected object */
	private static final int OFFSET = 16;
    
    /** bounds of the dimensional indicator */
    private float[] bounds;
    
    /** real-world (location-layer) width of the horizontal line */
    private float width;
    
    /** real-world (location-layer) height of the vertical line */
    private float height;
	
	/** working variable */
	private AxisAngle4f rotation;
	
	/** working variable */
	private Vector3f translation;
	
	/** working variable */
	private Matrix4f matrix;
	private Matrix4f mtx;
	
	/** for rotating the vertical indicator text */
	private AxisAngle4f rot90;
	    
    private IndexedLineArray vertLineGeom;
    private IndexedLineArray horzLineGeom;
	private LegendText horizontalText;
    private TransformGroup horizontalTextTransformGroup;
	private LegendText verticalText;
    private TransformGroup verticalTextTransformGroup;
    
    /** working variable to hold line array data for the vertical bar */
    private float[] verticalLineVertices;
    
    /** working variable to hold indexed line array information */
	private int[] lineIndices;
	
	/** working variable to hold line array data for the horizontal bar */
	private float[] horizontalLineVertices;
	
    /**
     * Create a new instance of the DimensionalIndicator class
     */
    public DimensionalIndicator(SceneManagerObserver mgmtObserverParam,
    				   		    AVGeometryBuilder avBuilderParam,
    				   		    Unit unitParam){
        super(mgmtObserverParam, avBuilderParam, unitParam);
    	
        bounds = new float[6];
    	rotation = new AxisAngle4f();
    	translation = new Vector3f();
    	matrix = new Matrix4f();
    	mtx = new Matrix4f();

    	rot90 = new AxisAngle4f(0, 0, 1, (float)(Math.PI/2));
		 
		initSceneGraph();
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
    public void updateNodeBoundsChanges(Object src) {
    	
    	super.updateNodeBoundsChanges(src);
    	if (src == vertLineGeom) {
    		//
        	// create the vertical line group
        	//
    		// the first six points make up the first 
    		// two vertices, used for the top 'cap' to the vertical line
    		//
    		verticalLineVertices[0] = bounds[0] - 20;
    		verticalLineVertices[1] = bounds[2];
    		verticalLineVertices[2] = DIMENSIONAL_INDICATOR_Z_POS;
    		verticalLineVertices[3] = bounds[0] - 12;
    		verticalLineVertices[4] = bounds[2];
    		verticalLineVertices[5] = DIMENSIONAL_INDICATOR_Z_POS;
    		
        	// second six points are for the next two vertices 
    		// that compose the vertical line
    		verticalLineVertices[6] = bounds[0] - OFFSET; // min X
    		verticalLineVertices[7] = bounds[2];		  // min Y
    		verticalLineVertices[8] = DIMENSIONAL_INDICATOR_Z_POS;
    		verticalLineVertices[9] = bounds[0] - OFFSET; // min X
    		verticalLineVertices[10] = bounds[3];		  // MAX y
    		verticalLineVertices[11] = DIMENSIONAL_INDICATOR_Z_POS;
    		
        	// the final six points are for the last two vertices 
    		// that make the bottom cap
    		verticalLineVertices[12] = bounds[0] - 20;
    		verticalLineVertices[13] = bounds[3];
    		verticalLineVertices[14] = DIMENSIONAL_INDICATOR_Z_POS;
    		verticalLineVertices[15] = bounds[0] - 12;
    		verticalLineVertices[16] = bounds[3];
    		verticalLineVertices[17] = DIMENSIONAL_INDICATOR_Z_POS;
        	
    		// configure the indexed line array
			vertLineGeom.setVertices(IndexedLineArray.COORDINATE_3, verticalLineVertices);
			
		} else if (src == horzLineGeom) {
    		//
        	// create the horizontal line group.
        	//
        	// the first six points make the first 
        	// two vertices for the left cap:
        	//
    		horizontalLineVertices[0] = bounds[0];
    		horizontalLineVertices[1] = bounds[2] - 20;
    		horizontalLineVertices[2] = DIMENSIONAL_INDICATOR_Z_POS;
    		horizontalLineVertices[3] = bounds[0];
    		horizontalLineVertices[4] = bounds[2] - 12;
    		horizontalLineVertices[5] = DIMENSIONAL_INDICATOR_Z_POS;
    		
    		// the second set of six points make the two vertices 
    		// for the horizontal line
    		horizontalLineVertices[6] = bounds[0]; // min X
    		horizontalLineVertices[7] = bounds[2] - OFFSET;	// min Y
    		horizontalLineVertices[8] = DIMENSIONAL_INDICATOR_Z_POS;
    		horizontalLineVertices[9] = bounds[1];			// MAX X
    		horizontalLineVertices[10] = bounds[2] - OFFSET;	// min Y
    		horizontalLineVertices[11] = DIMENSIONAL_INDICATOR_Z_POS;
    		
    		// final set of six points define the two vertices for the right cap
    		horizontalLineVertices[12] = bounds[1];
    		horizontalLineVertices[13] = bounds[2] - 20;
    		horizontalLineVertices[14] = DIMENSIONAL_INDICATOR_Z_POS;
    		horizontalLineVertices[15] = bounds[1];
    		horizontalLineVertices[16] = bounds[2] - 12;
    		horizontalLineVertices[17] = DIMENSIONAL_INDICATOR_Z_POS;
        	
        	// create a shape3D out of the indexed line array
        	horzLineGeom.setVertices(IndexedLineArray.COORDINATE_3, horizontalLineVertices);
			
    	} else if ( src == horizontalTextTransformGroup ) {
    		
			translation.set(
				(bounds[0] + bounds[1]) * 0.5f, 
            	bounds[2] - OFFSET, 
            	DIMENSIONAL_INDICATOR_Z_POS);
			
			mtx.setIdentity();
			mtx.setTranslation(translation);
				
    		horizontalTextTransformGroup.setTransform(mtx);
    		
    	} else if ( src == verticalTextTransformGroup ) {
			
    		translation.set(
				bounds[0] - OFFSET - 14, 
				(bounds[2] + bounds[3]) * 0.5f  - 4, 
            	DIMENSIONAL_INDICATOR_Z_POS);
			
    		mtx.setIdentity();
    		mtx.setRotation(rot90);
    		mtx.setTranslation(translation);
    		
    		verticalTextTransformGroup.setTransform(mtx);
    		
    	} else if ( src == compGroup ) {
    		
    		compGroup.setTransform(matrix);
   		
    	} 
    }
    

    /**
     * Notification that its safe to update the node now with any operations
     * that only change the node's properties, but do not change the bounds.
     *
     * @param src The node or Node Component that is to be updated.
     */
    public void updateNodeDataChanges(Object src) {
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    
    /**
     * 
     * @param boundsParam The min bounds of the object before any translation 
     * @param rotate The vector of rotation
     * @param translate The translation vector
     * @param horizLabel The number to display on the horizontal line
     * @param verticalLabel The number to display on the vertical line
     */
    public void drawBounds(float[] boundsParam, 
    					   float[] rotate, 
    					   float[] translate,
    					   float horizLabel,
    					   float verticalLabel){
    	bounds = boundsParam;
    	rotation.set(rotate);
    	rotation.z = 1;
    	rotation.y = 0;
    	translation.set(translate);

		matrix.setIdentity();
		matrix.setRotation(rotation);
		matrix.setTranslation(translation);
		
		width = horizLabel;
		height = verticalLabel;
		
		String vText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
    		UnitConversionUtilities.convertMetersTo(height, currentUnit)) +
			" " + currentUnit.getLabel();
		verticalText.update(vText, 0, Anchor.TOP_LEFT, null);
		
    	String hText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
    		UnitConversionUtilities.convertMetersTo(width, currentUnit)) +
			" " + currentUnit.getLabel();
		horizontalText.update(hText, 0, Anchor.TOP_LEFT, null);
		
		display(true);
		
		mgmtObserver.requestBoundsUpdate(vertLineGeom, this);
		mgmtObserver.requestBoundsUpdate(horzLineGeom, this);
		mgmtObserver.requestBoundsUpdate(horizontalTextTransformGroup, this);
		mgmtObserver.requestBoundsUpdate(verticalTextTransformGroup, this);
		
		mgmtObserver.requestBoundsUpdate(compGroup, this);
    }
	
	/**
	 * Initialize the scene graph components
	 */
	private void initSceneGraph() {
		
        float[] diffuse = DIFFUSE_FONT_COLOR.getRGBComponents(null);
		float[] emissive = DIFFUSE_FONT_COLOR.getRGBComponents(null);
		
		Material line_material = new Material();
		line_material.setDiffuseColor(diffuse);
    	line_material.setEmissiveColor(emissive);
		
		LineAttributes line_attributes = new LineAttributes();
		line_attributes.setLineWidth(2);
        line_attributes.setStipplePattern(LineAttributes.PATTERN_SOLID);
		
		Appearance line_appearance = new Appearance();
		line_appearance.setMaterial(line_material);
		line_appearance.setLineAttributes(line_attributes);
		   
        //
        // variables to build the indexed line arrays
        //
        verticalLineVertices = new float[18];
        lineIndices = new int[]{0, 1, 2, 3, 4, 5};
    	horizontalLineVertices = new float[18];
    	
    	//
    	// create and set the four components:
    	//
    	// first create the vertical line
    	vertLineGeom = new IndexedLineArray();
		vertLineGeom.setVertices(IndexedLineArray.COORDINATE_3, verticalLineVertices);
		vertLineGeom.setIndices(lineIndices, lineIndices.length);
		
		Shape3D vertLineShape = new Shape3D();
		vertLineShape.setGeometry(vertLineGeom);
		vertLineShape.setAppearance(line_appearance);
		
    	compGroup.addChild(vertLineShape);
    	
    	// create the horizontal line
    	horzLineGeom = new IndexedLineArray();
		horzLineGeom.setVertices(IndexedLineArray.COORDINATE_3, horizontalLineVertices);
		horzLineGeom.setIndices(lineIndices, lineIndices.length);
		
		Shape3D horzLineShape = new Shape3D();
		horzLineShape.setGeometry(horzLineGeom);
		horzLineShape.setAppearance(line_appearance);
		
    	compGroup.addChild(horzLineShape);
		
		// horizontal line text
		String hText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
			UnitConversionUtilities.convertMetersTo(width, currentUnit));
		
		horizontalText = new LegendText(mgmtObserver, false);
		horizontalText.setFont(DEFAULT_NUMBER_FONT);
		horizontalText.setTextColor(Color.black);
		horizontalText.update(hText, 0, Anchor.TOP_LEFT, null);
			
		horizontalTextTransformGroup = new TransformGroup();
		horizontalTextTransformGroup.addChild(horizontalText.getShape());
		
		compGroup.addChild(horizontalTextTransformGroup);
		
		// vertical line text
		String vText = UnitConversionUtilities.getFormatedNumberDisplay(
			currentUnit, 
			UnitConversionUtilities.convertMetersTo(height, currentUnit));
		
		verticalText = new LegendText(mgmtObserver, false);
		verticalText.setFont(DEFAULT_NUMBER_FONT);
		verticalText.setTextColor(Color.black);
		verticalText.update(vText, 0, Anchor.TOP_LEFT, null);
				
		verticalTextTransformGroup = new TransformGroup();
		verticalTextTransformGroup.addChild(verticalText.getShape());
		
		compGroup.addChild(verticalTextTransformGroup);
	}
}