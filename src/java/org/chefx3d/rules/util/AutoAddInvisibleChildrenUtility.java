/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.util;

//External imports
import java.util.ArrayList;

//Internal imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.util.RuleCollisionHandler;
import org.chefx3d.rules.util.RuleUtils;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;

/**
 * Utility methods for processing auto add invisible children operations.
 *
 * @author Ben Yarger
 * @version $Revision: 1.2 $
 */
public abstract class AutoAddInvisibleChildrenUtility {

	/**
	 * Check if the entity performs auto add invisible operations.
	 * 
	 * @param entity Entity to check
	 * @return True if performs auto add invisible, false otherwise
	 */
	public static boolean performsAutoAddInvisible(Entity entity) {
		
		// Check if we do auto add by span, if not just return true
		Boolean placeAutoAddInvisible = (Boolean)
	   		RulePropertyAccessor.getRulePropertyValue(
	   				entity, 
	   				ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_USE);
		
		return placeAutoAddInvisible.booleanValue();
	}
	
	/**
     * Adds one of each invisible child listed to the parent entity.
     * 
     * @param model WorldModel to reference.
     * @param parentEntity Parent entity to attempt to auto add invisible 
     * children to.
     * @param entityBuilder EntityBuilder to use when creating invisible 
     * children.
     * @return ArrayList<Command> add invisible children command list, can come
     * back empty.
     */
    public static void addInvisibleChildren(
    		WorldModel model, 
    		Entity parentEntity,
    		EntityBuilder entityBuilder,
    		RuleCollisionHandler rch){
    	
    	Boolean placeInvisibleChildren = (Boolean)
    		RulePropertyAccessor.getRulePropertyValue(
    				parentEntity, 
    				ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_USE);
    	
    	// If not using, return empty array list
    	if (!placeInvisibleChildren) {
    		return;
    	}
    		
    	// Get the entity ID's to add
        String[] invisibleChildren = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    parentEntity,
                    ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_PROP);

        if(invisibleChildren != null){

            for(int i = 0; i < invisibleChildren.length; i++){

                SimpleTool simpleTool =
                    RuleUtils.getSimpleToolByName(invisibleChildren[i]);

                if(simpleTool != null){
                	
                	// Create the new invisible entity
                    int entityID = model.issueEntityID();

                    Entity newEntity = entityBuilder.createEntity(
                            model,
                            entityID,
                            new double[] {0.0, -200.0, 0.0},
                            new float[] {0.0f, 1.0f, 0.0f, 0.0f},
                            simpleTool);

                    // clear out the 'bogus' property
                    newEntity.setProperty(
                            newEntity.getParamSheetName(),
                            Entity.SHADOW_ENTITY_FLAG,
                            false,
                            false);

                    // Set the is auto add flag
                    newEntity.setProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT,
                            true,
                            false);

                    //
                    // EMF: The following code should fix bug 810.
                    // We want to avoid adding invisible children in 
                    // a situation where those children already exist.
                    //                    
                    boolean addInvisibleChild = true;
                    ArrayList<Entity> chitlins = parentEntity.getChildren();
                    String newEntityToolID = newEntity.getToolID();
                    
                    for( Entity child : chitlins ){
                    	if(child.getToolID().equals(newEntityToolID)){
                    		addInvisibleChild = false;
                    		break;
                    	}                    		
                    }
                    
                    if( addInvisibleChild ){
                    	
                    	SceneManagementUtility.addChild(
                    			model, 
                    			rch.getRuleCollisionChecker(), 
                    			newEntity, 
                    			parentEntity,
                    			true);
                    }
                }
            }
        }
    }
    
    /**
	 * Get the auto add by collision tool IDs for the parentEntity.
	 * 
	 * @param parentEntity Entity to get auto add by collision tool id's from
	 * @return String[] copy of ids, or null if not found
	 */
	public static String[] getAutoAddToolIDS(Entity parentEntity) {
		   
		// First check if the parentEntity performs auto add by collision
		Boolean performAutoAddByCollision = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parentEntity, 
					ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_USE);
		   
		if (!performAutoAddByCollision) {
			return null;
		}
		   
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parentEntity,
				ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_PROP);
		
		if (autoPlaceObjectsProp == null) {
			return null;
		}
		
		String[] results = new String[autoPlaceObjectsProp.length]; 
		
		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
			results[i] = autoPlaceObjectsProp[i];
		}
		
		return results;
		
	}
    
	/**
	 * Check if the child is an auto add invisible child of the parent.
	 * 
	 * @param model WorldModel to reference
	 * @param child Child entity to examine
	 * @param parent Parent entity to check against
	 * @return True if child is an auto add invisible child of parent, false
	 * otherwise
	 */
	public static boolean isAutoAddChildOfParent(
			WorldModel model, 
			Entity child, 
			Entity parent) {
		
		// First check if the parentEntity performs auto add by collision
		Boolean performAutoAddByCollision = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					parent, 
					ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_USE);
		   
		if (!performAutoAddByCollision) {
			return false;
		}
		
		// Check if the child is an auto add of the parent
		if (!AutoAddUtility.isAutoAddChildOfParent(child, parent)) {
			return false;
		}
		
		// Get the tool ids of the auto add invisible children to create
		String[] autoPlaceObjectsProp = (String[])
			RulePropertyAccessor.getRulePropertyValue(
				parent,
				ChefX3DRuleProperties.AUTO_ADD_INVISIBLE_CHILDREN_PROP);
	
		if (autoPlaceObjectsProp == null) {
			return false;
		}
		
		// Get the primary tool ID to compare against the list of tool ID's.
		String primaryToolID = 
			AutoAddUtility.getPrimaryAutoAddToolID(child, parent);
		
		for (int i = 0; i < autoPlaceObjectsProp.length; i++) {
			
			if (primaryToolID.equals(autoPlaceObjectsProp[i])) {
				return true;
			}
		}
		
		return false;
	}
}
