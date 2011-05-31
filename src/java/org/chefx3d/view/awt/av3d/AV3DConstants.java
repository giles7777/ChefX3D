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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.chefx3d.model.Entity;

// External Imports
// None

// Local Imports
// None

/**
 * Constants used by the AV3DView
 *
 * @author Rex Melton
 * @version $Revision: 1.21 $
 */
public interface AV3DConstants {
	
	/** I wonder what this enum does? */
    public static enum ActionMode {
		NONE, 
		MOVEMENT, 
		SCALE, 
		ROTATE, 
		PLACEMENT,
	};
    
	/** Enumeration of navigation modes */
    public static enum NavigationMode {
		NONE, 
		EXAMINE, 
		PAN, 
		ZOOM, 
		PANZOOM,
	};
    
    /** Editor states */
    public static enum EditorMode {
        INACTIVE,
        SELECTION,
        PLACEMENT,
        ANCHOR_TRANSFORM,
        ENTITY_TRANSFORM,
        ENTITY_TRANSITION,
        NAVIGATION,
    };

	 /** An empty list used for clearing selections */
    public static final List<Entity> EMPTY_ENTITY_LIST =
        Collections.unmodifiableList(new ArrayList<Entity>());
    
    /** The scale factor for event orientation vectors */
    public static final float VECTOR_SCALE_FACTOR_CONSTANT = 10.0f;
    
    public static final double[] DEFAULT_MIN_SCALE_SIZE = new double[]{.1f,.1f,.1f};
	
    public static final double[] DEFAULT_MAX_SCALE_SIZE = new double[]{10000.0f,10000.0f,10000.0f};
    
    /** Default box color */
    public static final float[] DEFAULT_SELECTION_COLOR = new float[]{0, 0, 1};
	
	/** Constant value for perspective projection type */
	public static final String PERSPECTIVE = "perspective";
	
	/** Constant value for orthographic projection type */
	public static final String ORTHOGRAPHIC = "orthographic";
	
	/** Default viewpoint name */
	public static final String DEFAULT_VIEWPOINT_NAME = "Default Viewpoint";
	
    /** Default embedding depth */
    public static final float DEFAULT_EMBEDDING_DEPTH = 0.001f;
	
	/** Height of generated thumbnails */
	public static final int THUMBNAIL_HEIGHT = 700;
	
	/** Width of generated thumbnails */
	public static final int THUMBNAIL_WIDTH = 700;
	
	/** Scalar used to adjust orthographic view frustum parameters 
     * so that the geometry of the current scene takes up
     * a percentage of the viewable screen-space equal to this number. */
	public static final float FILL_PERCENT = 0.75f;
    
}
