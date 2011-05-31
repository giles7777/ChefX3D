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

import org.j3d.aviatrix3d.Appearance;
import org.j3d.aviatrix3d.LineArray;
import org.j3d.aviatrix3d.LineAttributes;
import org.j3d.aviatrix3d.Material;
import org.j3d.aviatrix3d.Shape3D;

// internal imports
import org.chefx3d.util.UnitConversionUtilities.Unit;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * This class manages the GridOverlay
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.11 $
 */
public class GridOverlay extends OverlayComponent {

    /** Default diffuse color for the line (black) */
    private static final Color DEFAULT_LINE_COLOR = Color.black;
    
    /** Transparency to use for the grid lines */
    private static final float TRANSPARENCY = .2f;

    /** The screen values (of overlay layer) of the
     * start and end grid points. */
    private float[] grid_vertices;

	/** Grid line geometry */
	private LineArray line_geom;
	
    /**
     * Constructor
     */
    public GridOverlay(SceneManagerObserver mgmtObserverParam,
                       AVGeometryBuilder avBuilderParam,
                       Unit unitParam){
        super(mgmtObserverParam, avBuilderParam, unitParam);

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

        if ( src == line_geom ) {
			
        	line_geom.setVertices(LineArray.COORDINATE_3, grid_vertices);
        }
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * @param f the float array to use to draw the new grid. 
     * Every six points defines the start and end point of a line.
     */
    public void drawGridOverlay(float[] f){
    	
    	grid_vertices = f;
    	
    	display(true);
		mgmtObserver.requestBoundsUpdate(line_geom, this);
    }
	
	/**
	 * Initialize the scene graph components
	 */
	private void initSceneGraph() {
		
		// create hashmark geometry
        float[] diffuse = DEFAULT_LINE_COLOR.getRGBComponents(null);
		float[] emissive = DEFAULT_LINE_COLOR.getRGBComponents(null);
		
		Material line_material = new Material();
		line_material.setDiffuseColor(diffuse);
    	line_material.setEmissiveColor(emissive);
		line_material.setTransparency(TRANSPARENCY);
		
		LineAttributes line_attributes = new LineAttributes();
		line_attributes.setLineWidth(1);
        line_attributes.setStipplePattern(LineAttributes.PATTERN_SOLID);
		
		Appearance line_appearance = new Appearance();
		line_appearance.setMaterial(line_material);
		line_appearance.setLineAttributes(line_attributes);
		
		line_geom = new LineArray();
		grid_vertices = new float[]{ 0, 0, -.001f, 0, 0, -.001f};
        line_geom.setVertices(LineArray.COORDINATE_3, grid_vertices);
		
		Shape3D line_shape = new Shape3D();
		line_shape.setAppearance(line_appearance);
		line_shape.setGeometry(line_geom);
        
		compGroup.addChild(line_shape);
	}
}