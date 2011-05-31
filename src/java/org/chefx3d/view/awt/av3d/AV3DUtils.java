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

import javax.vecmath.Matrix4f;

// Local Imports
import org.chefx3d.model.Entity;

/**
 * Miscellaneous Utilities.
 * 
 * @author Rex Melton
 * @version $Revision: 1.9 $
 */	
public abstract class AV3DUtils {
	
    /** Map of entity type String identifiers, key'ed by type */
    private static HashMap<Integer, String> typeMap;

    /** Static Constructor */
    static {
        typeMap = new HashMap<Integer, String>();
        typeMap.put(new Integer(Entity.TYPE_MODEL), "MODEL");
        typeMap.put(new Integer(Entity.TYPE_WORLD), "WORLD");
        typeMap.put(new Integer(Entity.TYPE_MULTI_SEGMENT), "MULTI_SEGMENT");
        typeMap.put(new Integer(Entity.TYPE_BUILDING), "BUILDING");
        typeMap.put(new Integer(Entity.TYPE_LOCATION), "LOCATION");
        typeMap.put(new Integer(Entity.TYPE_CONTENT_ROOT), "CONTENT_ROOT");
        typeMap.put(new Integer(Entity.TYPE_VIEWPOINT), "VIEWPOINT");
        typeMap.put(new Integer(Entity.TYPE_ENVIRONMENT), "ENVIRONMENT");
        typeMap.put(new Integer(Entity.TYPE_CONTAINER), "CONTAINER");
        typeMap.put(new Integer(Entity.TYPE_SEGMENT), "SEGMENT");
        typeMap.put(new Integer(Entity.TYPE_VERTEX), "VERTEX");
        typeMap.put(new Integer(Entity.TYPE_ZONE), "ZONE");
        typeMap.put(new Integer(Entity.TYPE_SWITCH), "SWITCH");
        typeMap.put(new Integer(Entity.TYPE_TEMPLATE), "TEMPLATE");
        typeMap.put(new Integer(Entity.TYPE_TEMPLATE_CONTAINER), "TEMPLATE_CONTAINER");
        typeMap.put(new Integer(Entity.TYPE_INTERSECTION), "INTERSECTION");
        typeMap.put(new Integer(Entity.TYPE_MODEL_WITH_ZONES), "MODEL_WITH_ZONES");
        typeMap.put(new Integer(Entity.TYPE_MODEL_ZONE), "MODEL_ZONE");
        typeMap.put(new Integer(Entity.TYPE_GROUNDPLANE_ZONE), "GROUNDPLANE_ZONE");
    }

	/**
	 * Restricted Constructor
	 */
	private AV3DUtils() {
	}
	
    /**
     * Return the descriptor of the Entity type
     *
     * @param entity The Entity
     * @return The type descriptor String
     */
    public static String getTypeString(Entity entity) {
        return(typeMap.get(entity.getType()));
    }

    /**
     * Return the descriptor of the Entity type
     *
     * @param entity_type The Entity type identifier
     * @return The type descriptor String
     */
    public static String getTypeString(int entity_type) {
        return(typeMap.get(entity_type));
    }

	/**
	 * Set the shadow property on an Entity
	 * 
	 * @param entity The Entity to set the property to
	 * @param state The state of the property
	 */
	public static void setShadowState(Entity entity, boolean state) {
		entity.setProperty(
			entity.getParamSheetName(),
			Entity.SHADOW_ENTITY_FLAG,
			state,
			false);
		if (entity.hasChildren()) {
			ArrayList<Entity> children = entity.getChildren();
			for (int i = 0; i < children.size(); i++) {
				Entity child = children.get(i);
				setShadowState(child, state);
			}
		}
	}
	
	/**
	 * Return whether the argument entity is a shadow entity
	 *
	 * @param entity The entity to check
	 * @return true if the entity's shadow property is set to true,
	 * false otherwise.
	 */
	public static boolean isShadow(Entity entity) {
		boolean isShadow = false;
		Object isShadowProp = entity.getProperty(
			entity.getParamSheetName(), 
			Entity.SHADOW_ENTITY_FLAG);
		if ((isShadowProp != null) && (isShadowProp instanceof Boolean)) {
			isShadow = ((Boolean)isShadowProp).booleanValue();
		}
		return(isShadow);
	}
	
	/**
	 * Return whether the argument Entity should be embedded in it's parent
	 *
	 * @param entity The Entity to check
	 * @return true if the entity should be embedded, false otherwise
	 */
	public static boolean isEntityEmbedded(Entity entity) {
		
		boolean embedded = false;
		if (entity != null) {
			String category = entity.getCategory();
			if ((category != null) && (
				category.equals("Category.Window") ||
				category.equals("Category.Door"))) {
				
				embedded = true;
			}
		}
		return(embedded);
	}
	
	/**
	 * Copy the contents of a matrix to an array
	 *
	 * @param mtx The matrix
	 * @param array The array
	 */
	public static void toArray(Matrix4f mtx, float[] array) {
		array[0] = mtx.m00;
		array[1] = mtx.m01;
		array[2] = mtx.m02;
		array[3] = mtx.m03;
		array[4] = mtx.m10;
		array[5] = mtx.m11;
		array[6] = mtx.m12;
		array[7] = mtx.m13;
		array[8] = mtx.m20;
		array[9] = mtx.m21;
		array[10] = mtx.m22;
		array[11] = mtx.m23;
		array[12] = mtx.m30;
		array[13] = mtx.m31;
		array[14] = mtx.m32;
		array[15] = mtx.m33;
	}
}
