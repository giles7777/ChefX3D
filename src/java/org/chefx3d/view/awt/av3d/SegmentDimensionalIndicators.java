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

//External Imports
import java.awt.Color;
import java.awt.Font;

import java.text.NumberFormat;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.Group;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.Shape3D;
import org.j3d.aviatrix3d.TransformGroup;


//Internal Imports
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * This class implements the drawing of the dimensional indicators for the segments 
 *
 *
 * @author Jon Hubbard
 * @version $Revision: 1.8 $
 */
public class SegmentDimensionalIndicators extends OverlayComponent {

    /** Default Font for the numeric labels */
    private static final Font DEFAULT_NUMBER_FONT = new Font("serif", Font.PLAIN, 10);
    
    /** Default diffuse color for the fonts (black) */
    private static final Color DIFFUSE_FONT_COLOR = Color.black;
    
    /** Default emissive color for the fonts (dark blue) */
    private static final Color EMISSIVE_FONT_COLOR = new Color(13, 30, 125);
    
    /** transparency to use for the dimensional indicators */
    private static final float TRANSPARENCY = 1f;
    
    /** Grid overlay depth position in the scene.  */
    private static final float DIMENSIONAL_INDICATOR_Z_POS = -10;
    
    /** Default LineAttributes (solid line, line thickness of 1)      */
    private static final LineAttributes DEFAULT_LINE_ATTRIBUTES = 
        new LineAttributes();
        
    /** The distance, in pixels, the bars appear from the selected object */
    private static final int OFFSET = 16;
    
    /** bounds of the dimensional indicator */
    private ArrayList<Point2f> startPoints;
    
    /** bounds of the dimensional indicator */
    private ArrayList<Point2f> endPoints;
    
    /** bounds of the dimensional indicator */
    private ArrayList<Vector3d> directionVectors;
    
    /** bounds of the dimensional indicator */
    private ArrayList<Point2f> textPoints;
    
    /** bounds of the dimensional indicator */
    private ArrayList<AxisAngle4d> rotate;
    
    /** bounds of the dimensional indicator */
    private ArrayList<Float> lengths;
    
    /** working variable */
    private Matrix4f matrix;
    
    /** for rotating text of the vertical indicator */
    private Matrix4f ccw90Matrix;
	
    /** the array of lines to be used   */
    private Shape3D lengthLineGroup;
	
    /** Hashmap used to keep track of which length Text node gets which number*/
    private HashMap<Integer,Shape3D> lengthTextNodesMap;
	
    /** Hashmap used to keep track of which length Text Transform gets which moved where*/
    private HashMap<Integer,TransformGroup> lengthTextTransformGroupNodesMap;
	
    /**Stores all the text nodes ( shapes and transforms)*/
    private Group textGroupNode;
   
   /** how many text nods currently in use*/ 
    private int textNodeSize;
    
    /** working variable to hold indexed line array information */
    private int[] lineIndices;
    
    /** working variable to hold line array data for the horizontal bar */
    private float[] lengthLineVertices;
        
    /** Initialize the default line attributes */ 
    static {
        DEFAULT_LINE_ATTRIBUTES.setLineWidth(2);
        DEFAULT_LINE_ATTRIBUTES.setStipplePattern(LineAttributes.PATTERN_SOLID);
    };
    
    /**
     * Create a new instance of the TapeMeasure class
     *
     */
    public SegmentDimensionalIndicators(
                       SceneManagerObserver mgmtObserverParam,
                       AVGeometryBuilder avBuilderParam,
                       Unit unitParam){
        super(mgmtObserverParam, avBuilderParam, unitParam);
        
       // geometry = new TransformGroup();
        startPoints = new ArrayList<Point2f>();
        endPoints = new ArrayList<Point2f>();
        textPoints = new ArrayList<Point2f>();
        directionVectors = new ArrayList<Vector3d>();
        lengths = new ArrayList<Float>();
        lengthTextNodesMap = new HashMap<Integer,Shape3D>();
        lengthTextTransformGroupNodesMap =  new HashMap<Integer,TransformGroup>();
        textGroupNode = new Group();
        
        matrix = new Matrix4f();

        Material m = avBuilder.material(
			DIFFUSE_FONT_COLOR, 
            EMISSIVE_FONT_COLOR, 
			TRANSPARENCY);
        Appearance lineAppearance = avBuilder.appearance(
			m, 
			DEFAULT_LINE_ATTRIBUTES);
        
        ccw90Matrix = new Matrix4f();
                
        //
        // variables to build the indexed line arrays
        //
        lineIndices = new int[] { 0, 1};
        lengthLineVertices = new float[18];
        
        //
        // create and set the four components:
        //
        // first create the vertical line
        
        float[] lengthTestLineVertices = new float[]{0,0,0,0,0,0};
  
        // the array of lines to be used 
        lengthLineGroup = avBuilder.create(
                avBuilder.indexedLineArray(lengthTestLineVertices, 
                        lineIndices), 
                        lineAppearance);
        compGroup.addChild(lengthLineGroup);
        
        compGroup.addChild(textGroupNode);
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
        if (src == lengthLineGroup) {
			//
			// create the horizontal line group.
			//
			// the first six points make the first 
			// two vertices for the left cap:
			//
			if (lengths != null) {
				
				int len = lengths.size();
				if (len > 0) {
					lengthLineVertices = new float[18*len];
					int[] lengthlLineVerticesIndices = new int[6*len];
					int j = 0;
					for (int i = 0; i < len; i++) {
						
						float startX = startPoints.get(i).x;
						float startY = startPoints.get(i).y;
						float endX = endPoints.get(i).x;
						float endY = endPoints.get(i).y;
						
						//The line
						lengthlLineVerticesIndices[j] = j++;
						lengthLineVertices[0+(i*18)] = startX;
						lengthLineVertices[1+(i*18)] = startY;
						lengthLineVertices[2+(i*18)] = DIMENSIONAL_INDICATOR_Z_POS;
						
						lengthlLineVerticesIndices[j] = j++;
						lengthLineVertices[3+(i*18)] = endX;
						lengthLineVertices[4+(i*18)] = endY;
						lengthLineVertices[5+(i*18)] = DIMENSIONAL_INDICATOR_Z_POS;
						
						Vector3d direction = directionVectors.get(i);
						Vector3d outsidePoint = new Vector3d(
							direction.x,
							direction.y,
							direction.z);
						
						Vector3d insidePoint = new Vector3d(
							direction.x,
							direction.y,
							direction.z);
						
						insidePoint.negate();
						
						//starting point end cap
						//Inside end cap point
						lengthlLineVerticesIndices[j]=j++;
						lengthLineVertices[6+(i*18)] = (float)(startX +(insidePoint.x * 10));
						lengthLineVertices[7+(i*18)] = (float)(startY +(insidePoint.y * 10));
						lengthLineVertices[8+(i*18)] = DIMENSIONAL_INDICATOR_Z_POS;
						
						//outside end cap point
						lengthlLineVerticesIndices[j]=j++;
						lengthLineVertices[9+(i*18)] =  (float)(startX +(outsidePoint.x * 10));
						lengthLineVertices[10+(i*18)] = (float)(startY +(outsidePoint.y * 10));
						lengthLineVertices[11+(i*18)] = DIMENSIONAL_INDICATOR_Z_POS;
						
						//end point end cap
						//Inside end cap point
						lengthlLineVerticesIndices[j]=j++;
						lengthLineVertices[12+(i*18)] = (float)(endX +(insidePoint.x * 10));
						lengthLineVertices[13+(i*18)] = (float)(endY +(insidePoint.y * 10));
						lengthLineVertices[14+(i*18)] = DIMENSIONAL_INDICATOR_Z_POS;
						
						//outside end cap point
						lengthlLineVerticesIndices[j]=j++;
						lengthLineVertices[15+(i*18)] = (float)(endX +(outsidePoint.x * 10));
						lengthLineVertices[16+(i*18)] = (float)(endY +(outsidePoint.y * 10));
						lengthLineVertices[17+(i*18)] = DIMENSIONAL_INDICATOR_Z_POS;
					}
					
					// create a shape3D out of the indexed line array
					lengthLineGroup.setGeometry(
						avBuilder.indexedLineArray(lengthLineVertices, 
						lengthlLineVerticesIndices));
				} else {
					lengthLineGroup.setGeometry(null);
				}
			} else {
				lengthLineGroup.setGeometry(null);
			}
        } else if (lengthTextNodesMap.containsValue(src)) {
            
            Shape3D lengthText=(Shape3D)src;
            Object[] keySet = lengthTextNodesMap.keySet().toArray();
            
            int index = 0;
            for (int i = 0; i < keySet.length; i++) {
                Integer indexKey = (Integer)keySet[i];
                if (lengthTextNodesMap.get(indexKey) == lengthText) {
                    index = indexKey;
                    break;
                }
            }
			if (lengths != null) {
				
				int len = lengths.size();
				if (len > 0 && index < len) {
					
					float length = lengths.get(index);
					
					String text = 
						UnitConversionUtilities.getFormatedNumberDisplay(
								currentUnit, 
								UnitConversionUtilities.convertMetersTo(
										length, currentUnit));
					
					lengthText.setGeometry(
						avBuilder.generateTextGeometry(
								text + " " + currentUnit.getLabel(), 
								DEFAULT_NUMBER_FONT));
				}
			}
        } else if (lengthTextTransformGroupNodesMap.containsValue(src )) {
            
            TransformGroup textGroup=(TransformGroup)src;
            Object[] keySet = lengthTextNodesMap.keySet().toArray();
            
            int index = 0;
            for (int i = 0; i < keySet.length; i++) {
                Integer indexKey =(Integer)keySet[i];
                if (lengthTextTransformGroupNodesMap.get(keySet[i]) == textGroup) {
                    index = indexKey;
                    break;
                }
            }
			
			if (rotate != null) {
				
				int len = rotate.size();
				if (len > 0 && index < len) {
					
					AxisAngle4d rotateTest = rotate.get(index);
					ccw90Matrix.setIdentity();
					
					ccw90Matrix.set(rotateTest);
					ccw90Matrix.setTranslation(new Vector3f(
						textPoints.get(index).x, 
						textPoints.get(index).y, 
						DIMENSIONAL_INDICATOR_Z_POS));
					
					textGroup.setTransform(ccw90Matrix);
				}
			}
        } else if( src == compGroup ){
            
            compGroup.setTransform(matrix);
        
        } else if ( src == textGroupNode ) {
            
            textGroupNode.removeAllChildren();   
            lengthTextNodesMap.clear();
            lengthTextTransformGroupNodesMap.clear();
            
            for (int i = 0; i < textNodeSize; i++) {
                
				String text = 
					UnitConversionUtilities.getFormatedNumberDisplay(
							currentUnit, 
							0);

                Shape3D lengthText = avBuilder.generateTextShape(
                		text + " " + currentUnit.getLabel(), 
                		DEFAULT_NUMBER_FONT, 
                		Color.black, 
                		Color.black);
                        
                TransformGroup lengthTextTransformGroup = new TransformGroup();
                lengthTextTransformGroup.addChild(lengthText);
                
                lengthTextNodesMap.put(i,lengthText);
                lengthTextTransformGroupNodesMap.put(i,lengthTextTransformGroup);
                textGroupNode.addChild(lengthTextTransformGroup);
				
                mgmtObserver.requestBoundsUpdate(lengthText, this);
                mgmtObserver.requestDataUpdate(lengthText, this);
                mgmtObserver.requestBoundsUpdate(
                        lengthTextTransformGroup, this);
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
        
        if (lengthTextNodesMap.containsValue(src)) {
            Shape3D lengthText=(Shape3D)src;
            Object[] keySet = lengthTextNodesMap.keySet().toArray();
            
            int index = 0;
            for (int i = 0; i < keySet.length; i++) {
                Integer indexKey =(Integer) keySet[i];
                if (lengthTextNodesMap.get(keySet[i]) == lengthText) {
                    index = indexKey;
                    break;
                }
            }
			if (lengths != null) {
				
				int len = lengths.size();
				if (len > 0 && index < len) {
					
					float length = lengths.get(index);
					
					String text = 
						UnitConversionUtilities.getFormatedNumberDisplay(
								currentUnit, 
								UnitConversionUtilities.convertMetersTo(
										length, currentUnit));

					lengthText.setAppearance(
						avBuilder.generateTextAppearance(
								text + " " + currentUnit.getLabel(), 
								DEFAULT_NUMBER_FONT, 
								Color.black, 
								Color.black));
				}
			}
        }
    }
    
    
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * 
     * @param startPoints - start points of the vector
     * @param endPoints - end points of the vector
     * @param directionVector - the directional vector of the segments
     * @param lengths - the length of the segment
     * @param textPoints - the position of where the text should go
     * 
     * @param rotate - the axisAngle4d of rotation 
     * ( this is created by taking the direction vector 
     * , cross product with 1,0,0 , and then using Vector3d.angle as the angle)
     */
    public void drawBounds(ArrayList<Point2f> startPoints,
                           ArrayList<Point2f> endPoints,
                           ArrayList<Vector3d> directionVector,
                           ArrayList<Float> lengths,
                           ArrayList<Point2f> textPoints,
                           ArrayList<AxisAngle4d> rotate){
        
        this.startPoints = startPoints;
        this.endPoints = endPoints;
        this.lengths = lengths;
        this.textPoints = textPoints;
        this.directionVectors = directionVector;
        this.rotate = rotate;

        display(true);

        mgmtObserver.requestBoundsUpdate(lengthLineGroup, this);

        for (int i = 0; i < lengthTextNodesMap.size(); i++) {
            Shape3D lengthText = lengthTextNodesMap.get(i);
            TransformGroup textTransformGroup = 
                lengthTextTransformGroupNodesMap.get(i);

            mgmtObserver.requestBoundsUpdate(lengthText, this);
            mgmtObserver.requestDataUpdate(lengthText, this);
            mgmtObserver.requestBoundsUpdate(
                    textTransformGroup, this);
        }
    }
    
    /**
     * If the number of text Nodes differes from the current amount of text nodes
     * then it causes the text group node to repopulate with the correct amount
     * if numberOfTextNodes = 0, clears the textGroupNode
     * @param numberOfTextNodes
     */
    public void createTextNodes(int numberOfTextNodes) {
        if (textNodeSize != numberOfTextNodes) {
            textNodeSize = numberOfTextNodes;
            mgmtObserver.requestBoundsUpdate(textGroupNode, this);
        }
    }
}
