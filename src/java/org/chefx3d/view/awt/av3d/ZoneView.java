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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.j3d.aviatrix3d.TransformGroup;
import org.j3d.aviatrix3d.ViewEnvironment;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Helper class for calculating the view parameters based on
 * the 'zone' that is active.
 *
 * @author Rex Melton
 * @version $Revision: 1.32 $
 */
class ZoneView {
	
    /** Axis vectors for calculations */
    static final Vector3f POS_X_AXIS = new Vector3f(1, 0, 0);
    static final Vector3f NEG_X_AXIS = new Vector3f(-1, 0, 0);
    static final Vector3f POS_Y_AXIS = new Vector3f(0, 1, 0);
    static final Vector3f NEG_Y_AXIS = new Vector3f(0, -1, 0);
    static final Vector3f POS_Z_AXIS = new Vector3f(0, 0, 1);
    static final Vector3f NEG_Z_AXIS = new Vector3f(0, 0, -1);

    /** The ViewEnvironment */
    private ViewEnvironment viewEnv;

    /** The world model */
    private WorldModel model;

    /** Command Execution manager */
    private CommandController controller;

    /** The navigation status manager */
    private NavigationStatusManager statusManager;

    /** The active location Entity */
    private LocationEntity le;

    /** Scratch computation objects */
    private Vector3f view_translation;
    private Matrix4f view_matrix;

    private Vector3f face;

    private double[] left;
    private double[] rght;

    private float[] center;
    private float[] rotation;
    private double[] position;
    private double[] bounds;
    private float[] array;
    private double[] frustum;
    private double[] startingFrustum;
	
	private float[] min;
	private float[] max;

    /** Local transformation utils */
    private TransformUtils tu;
    private Matrix4f zone_matrix;
    
    /** The manager of the entities to be handled */
    private AV3DEntityManager entityManager;
    
    /** The map of entity wrappers */
    private HashMap<Integer, AV3DEntityWrapper> wrapperMap;
        
    /**
     * Constructor
     *
     * @param viewEnv The ViewEnvironment
     * @param model The WorldModel
     * @param controller The CommandController
     * @param statusManager The navigation status reporter
     */
    ZoneView(
        ViewEnvironment viewEnv,
        WorldModel model,
        CommandController controller,
        NavigationStatusManager statusManager) {

        this.viewEnv = viewEnv;
        this.model = model;
        this.controller = controller;
        this.statusManager = statusManager;
        
        tu = new TransformUtils();
        zone_matrix = new Matrix4f();
        
        view_translation = new Vector3f();
        view_matrix = new Matrix4f();

        face = new Vector3f();

        left = new double[3];
        rght = new double[3];

        center = new float[3];
        rotation = new float[4];
        position = new double[3];
        bounds = new double[4];
        array = new float[16];
        frustum = new double[6];
        startingFrustum = new double[6];
		
        min = new float[3];
        max = new float[3];
    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------

    /**
     * Get the zoom amount as a value of the distance from the active zone.
     * 
     * @return double Distance from active zone
     */
    double getZoomAmount(){

    	viewEnv.getViewFrustum(frustum);
        double zoom = view_translation.z * (frustum[1] / startingFrustum[1]);
        return zoom;
    }
    
    /** 
     * Set the active location entity
     *
     * @param le The new location entity
     */
    void setLocationEntity(LocationEntity le) {
        this.le = le;
    }
    
    /**
     * Set the active entity manager 
     *
     * @param entityManager The active entity manager 
     */
    void setEntityManager(AV3DEntityManager entityManager) {
        this.entityManager = entityManager;
        if (entityManager != null) {
            wrapperMap = entityManager.getAV3DEntityWrapperMap();
        } else {
            wrapperMap = null;
        }
    }
    
    /**
     * Configure the view parameters of the default orthographic
	 * editor viewpoint based on the zone
     *
     * @param ze The current editing zone
     */
    void configView(ZoneEntity ze) {
		
        if ((ze == null) || (le == null) || (wrapperMap == null)) {
            return;
        }
        
        ViewpointContainerEntity vce =
            le.getViewpointContainerEntity();

        if (vce.hasChildren()) {
            // rem: hard coded, only one orthographic viewpoint is used
            ViewpointEntity ve = null;
			List<ViewpointEntity> viewpoints = vce.getViewpoints();
			for (Iterator<ViewpointEntity> i = viewpoints.iterator(); i.hasNext();) {
				ViewpointEntity vex = i.next();
				
				String proj_type = (String)vex.getProperty(
					Entity.DEFAULT_ENTITY_PROPERTIES,
					ViewpointEntity.PROJECTION_TYPE_PROP);
				
				String vp_name = (String)vex.getProperty(
					Entity.DEFAULT_ENTITY_PROPERTIES,
					Entity.NAME_PROP);
				
				if (proj_type.equals(AV3DConstants.ORTHOGRAPHIC) &&
					vp_name.equals(AV3DConstants.DEFAULT_VIEWPOINT_NAME)) {
					ve = vex;
					break;
				}
			}
			if (ve != null) {
				configView(ze, ve);
			}
		}
	}
	
    /**
     * Configure the view parameters of the argument
	 * viewpoint based on the zone
     *
     * @param ze The current editing zone
     * @param ve The viewpoint to configure
     */
    void configView(ZoneEntity ze, ViewpointEntity ve) {
		
        if ((ze == null) || (ve == null) || (le == null) || (wrapperMap == null)) {
            return;
        }
		
		int zone_type = ze.getType();
		AV3DEntityWrapper zone_wrapper = wrapperMap.get(ze.getEntityID());
		tu.getLocalToVworld(zone_wrapper.transformGroup, zone_matrix);
		
		if (zone_type == Entity.TYPE_GROUNDPLANE_ZONE) {
			
			// default viewpoint [0, 0, 10]
			center[0] = 0;
			center[1] = 0;
			center[2] = 10;
			
			rotation[0] = 0;
			rotation[1] = 1;
			rotation[2] = 0;
			rotation[3] = 0;
			
			float[] width_x_height = null;
			
			ContentContainerEntity cce =
				le.getContentContainerEntity();
			if (cce != null) {
				ArrayList<Entity> content = cce.getChildren();
				for (int i = 0; i < content.size(); i++) {
					Entity e = content.get(i);
					if (e.getType() == Entity.TYPE_MULTI_SEGMENT) {
						SegmentableEntity mse = (SegmentableEntity)e;
						ArrayList<VertexEntity> vertices = mse.getVertices();
						
						// use the vertices to determine the viewing bounds
						if (vertices != null && vertices.size() > 0) {
							int num_vertex = vertices.size();
							if (num_vertex == 1) {
								VertexEntity vtx = vertices.get(0);
								vtx.getPosition(position);
								center[0] = (float)position[0];
								center[1] = (float)position[1];
								center[2] = vtx.getHeight();
							} else {
								float max_height = 0;
								bounds[0] = Double.MAX_VALUE;
								bounds[1] = -Double.MAX_VALUE;
								bounds[2] = Double.MAX_VALUE;
								bounds[3] = -Double.MAX_VALUE;
								for (int j = 0; j < vertices.size(); j++) {
									VertexEntity vtx = vertices.get(j);
									if (vtx != null) {
										vtx.getPosition(position);
										if (position[0] < bounds[0]) {
											bounds[0] = position[0];
										}
										if (position[0] > bounds[1]) {
											bounds[1] = position[0];
										}
										if (position[1] < bounds[2]) {
											bounds[2] = position[1];
										}
										if (position[1] > bounds[3]) {
											bounds[3] = position[1];
										}
										float height = vtx.getHeight();
										if (height > max_height) {
											max_height = height;
										}
									}
								}
								
								center[0] = (float)(bounds[1] + bounds[0])/2;
								center[1] = (float)(bounds[3] + bounds[2])/2;
								center[2] = 10 + max_height;
								
								width_x_height = new float[2];
								width_x_height[0] = (float)(bounds[1] - bounds[0]);
								width_x_height[1] = (float)(bounds[3] - bounds[2]);
							}
							break;
						}
						
					} else if (e.getType() == Entity.TYPE_GROUNDPLANE_ZONE) {
						
						float[] size = new float[3];
						ze.getSize(size);
						
						float[] scale = new float[3];
						ze.getScale(scale);
						
						float width = size[0] * scale[0];
						float height = size[1] * scale[1];
						
						center[0] = 0;
						center[1] = 0;
						center[2] = 10;
						
						width_x_height = new float[2];
						width_x_height[0] = width;
						width_x_height[1] = height;
						
					}
				} 
			}
			
			// set the viewpoint to the center of the bounds
			// of the vertices
			view_translation.set(center);
			
			view_matrix.setIdentity();
			view_matrix.setTranslation(view_translation);
			
			// transform from local to world
			view_matrix.mul(zone_matrix, view_matrix);
			
			AV3DUtils.toArray(view_matrix, array);
			
			ChangePropertyTransientCommand cptc = new ChangePropertyTransientCommand(
				ve,
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ViewpointEntity.VIEW_MATRIX_PROP,
				array,
				model);
			controller.execute(cptc);
			if (statusManager != null) {
				statusManager.fireViewMatrixChanged(view_matrix);
			}
			
			if (width_x_height != null) {
				configViewEnvironment(width_x_height[0], width_x_height[1], ve);
			}
			
		} else if (zone_type == Entity.TYPE_SEGMENT) {
			
			SegmentEntity se = (SegmentEntity)ze;
			
			VertexEntity rght_ve = se.getEndVertexEntity();
			VertexEntity left_ve = se.getStartVertexEntity();
			
			rght_ve.getPosition(rght);
			left_ve.getPosition(left);
			
			float rght_height = rght_ve.getHeight();
			float left_height = left_ve.getHeight();
			
			// take the 'highest' as the bounding height
			float height = Math.max(rght_height, left_height);
			
			face.set(
				(float)(rght[0] - left[0]),
				(float)(rght[1] - left[1]),
				(float)0);
			
			// width of the wall
			float width = face.length();
			
			// TODO: need to get the bounds in order to set the depth properly....
			float depth = Math.max(width, height) * 2;
			
			// set the viewpoint to the center of the wall
			view_translation.set(
				width / 2,
				height / 2,
				depth);
			
			view_matrix.setIdentity();
			view_matrix.setTranslation(view_translation);
			
			// transform from local to world
			view_matrix.mul(zone_matrix, view_matrix);
			
			AV3DUtils.toArray(view_matrix, array);
			
			ChangePropertyTransientCommand cptc = new ChangePropertyTransientCommand(
				ve,
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ViewpointEntity.VIEW_MATRIX_PROP,
				array,
				model);
			controller.execute(cptc);
			if (statusManager != null) {
				statusManager.fireViewMatrixChanged(view_matrix);
			}
			
			configViewEnvironment(width, height, ve);
			
		} else if (zone_type == Entity.TYPE_MODEL_ZONE) {
			
			OrientedBoundingBox bounds = zone_wrapper.getBounds();
			bounds.getVertices(min, max);
			
			float center_x = (max[0] + min[0]) * 0.5f;
			float center_y = (max[1] + min[1]) * 0.5f;
			
			float width = max[0] - min[0];
			float height = max[1] - min[1];
			
			// TODO: need to get the bounds in order to set the depth properly....
			float depth = Math.max(width, height) * 2;
			
			// set the viewpoint to the center of the wall
			view_translation.set(
				center_x,
				center_y,
				depth);
			
			view_matrix.setIdentity();
			view_matrix.setTranslation(view_translation);
			
			// transform from local to world
			view_matrix.mul(zone_matrix, view_matrix);
			
			AV3DUtils.toArray(view_matrix, array);
			
			ChangePropertyTransientCommand cptc = new ChangePropertyTransientCommand(
				ve,
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ViewpointEntity.VIEW_MATRIX_PROP,
				array,
				model);
			controller.execute(cptc);
			if (statusManager != null) {
				statusManager.fireViewMatrixChanged(view_matrix);
			}
			
			configViewEnvironment(width, height, ve);
			
		} else {
			System.out.println("Don't know how to configure view for "+ ze);
		}
    }

    /**
     * Set the ViewEnvironment parameters for the specified
     * content bounds and viewpoint.  Calling this method will adjust
     * the viewpoint so that the geometry of the current scene will take up
     * a percentage of the scene equal to the FILL_PERCENT constant. 
     *
     * @param width The max horizontal dimension of the content in the viewport
     * @param height The max vertical dimension of the content in the viewport
     * @param ve The ViewpointEntity to configure
     */
    private void configViewEnvironment(float width, float height, ViewpointEntity ve) {        
        
        float c_ratio = width / height;

        viewEnv.getViewFrustum(frustum);

        double f_width = frustum[1] - frustum[0];
        double f_height = frustum[3] - frustum[2];

        double f_ratio = f_width / f_height;

        width /= AV3DConstants.FILL_PERCENT;
        height /= AV3DConstants.FILL_PERCENT;

        if (c_ratio > 1.0f) {

            f_width = width;
            f_height = f_width / f_ratio;

        } else {

            f_height = height;
            f_width = f_height * f_ratio;
        }

        frustum[0] = -f_width / 2;
        frustum[1] = f_width / 2;
        frustum[2] = -f_height / 2;
        frustum[3] = f_height / 2;
        
        /*
         * bjy: set the startingFrustum so a usable zoom value can be 
         * calculated for use in user interface tools.
         */
        startingFrustum[0] = frustum[0];
        startingFrustum[1] = frustum[1];
        startingFrustum[2] = frustum[2];
        startingFrustum[3] = frustum[3];
        
        ChangePropertyTransientCommand cptc = new ChangePropertyTransientCommand(
            ve,
            Entity.DEFAULT_ENTITY_PROPERTIES,
            ViewpointEntity.ORTHO_PARAMS_PROP,
            frustum,
            model);
        controller.execute(cptc);
    }
}
