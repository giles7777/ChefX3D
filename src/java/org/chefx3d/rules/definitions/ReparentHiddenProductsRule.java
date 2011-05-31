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

package org.chefx3d.rules.definitions;

//External Imports
import java.util.Arrays;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.NearestNeighborMeasurement;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.rules.util.TransformUtils;

/**
 * 
 *
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
public class ReparentHiddenProductsRule extends BaseRule {

	/** Message key */
	private static final String REMOVE_HIDDEN_PRODUCTS_MSG =
		"org.chefx3d.rules.definitions.ReparentHiddenProductsRule.removeHiddenProducts";
	
	/** Calculation objects */
	private Matrix4f mtx;
	private AxisAngle4f rotation;
	private Vector3f translation;
	
	private double[] pos_array;
	private float[] rot_array;
	
    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ReparentHiddenProductsRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
		
		mtx = new Matrix4f();
		rotation = new AxisAngle4f();
		translation = new Vector3f();
	
		pos_array = new double[3];
		rot_array = new float[4];
    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

    /**
     * Perform the rule check
     *
     * @param entity Entity object
     * @param command Command object
     * @param result The state of the rule processing
     * @return boolean True if rule passes, false otherwise
     */
	protected RuleEvaluationResult performCheck( 
		Entity entity,
		Command command,
		RuleEvaluationResult result) {
		
		//boolean evaluate = entity.isModel();
		boolean evaluate = false;
		/*
		Object prop_obj = RulePropertyAccessor.getRulePropertyValue(
			entity,
			ChefX3DRuleProperties.REPARENT_HIDDEN);
		
		if ((prop_obj != null) && (prop_obj instanceof Boolean)) {
			evaluate = ((Boolean)prop_obj).booleanValue();
		}
		*/
		
		if (evaluate && (entity instanceof PositionableEntity)) {
			
			if (command instanceof AddEntityChildCommand) {
				
				PositionableEntity pos_entity = (PositionableEntity)entity;
				AddEntityChildCommand aecc = (AddEntityChildCommand)command;
				
				ZoneEntity zone = SceneHierarchyUtility.findZoneEntity(model, entity);
				
				ArrayList<Entity> hidden_entity_list = rch.getNeighbors(
					//model,
					pos_entity,
					NearestNeighborMeasurement.NEG_Z,
					new float[]{0, 0, 100});
				prune(hidden_entity_list, zone);
				
				int num = hidden_entity_list.size();
				if (num > 0) {
					Entity parent = aecc.getParentEntity();
					if (parent != zone) {
						// if the argument entity is not already being parented
						// to the zone - then make it so
						double[] pos = TransformUtils.getExactPositionRelativeToZone(model, entity);
						aecc.setParentEntity(zone);
						pos_entity.setPosition(pos, false);
						// this should also configure the rotation.....
					}
					Matrix4f pos_entity_mtx = TransformUtils.getTransformsInSceneCoordinates(
						model,
						pos_entity,
						true);
					pos_entity_mtx.invert();
					
					for (int i = 0; i < num; i++) {
						
						PositionableEntity hidden_entity = 
							(PositionableEntity)hidden_entity_list.get(i);
						
						Entity hidden_entity_parent = 
							model.getEntity(hidden_entity.getParentEntityID());
						
						Matrix4f hidden_entity_mtx = TransformUtils.getTransformsInSceneCoordinates(
							model,
							hidden_entity,
							true);
						
						mtx.mul(hidden_entity_mtx, pos_entity_mtx);
						
						mtx.get(translation);
						pos_array[0] = translation.x;
						pos_array[1] = translation.y;
						pos_array[2] = translation.z;
						
						rotation.set(mtx);
						rot_array[0] = rotation.x;
						rot_array[1] = rotation.y;
						rot_array[2] = rotation.z;
						rot_array[3] = rotation.angle;
						
						SceneManagementUtility.changeParent(
							model, 
							collisionChecker, 
							view, 
							catalogManager, 
							hidden_entity, 
							hidden_entity_parent, 
							pos_entity, 
							pos_array, 
							rot_array, 
							true, 
							true);
					}
				}
			} else if (command instanceof RemoveEntityChildCommand) {
				
				PositionableEntity pos_entity = (PositionableEntity)entity;
				RemoveEntityChildCommand recc = (RemoveEntityChildCommand)command;
				
				ZoneEntity zone = SceneHierarchyUtility.findZoneEntity(model, entity);
				
				ArrayList<Entity> hidden_entity_list = rch.getNeighbors(
					//model,
					pos_entity,
					NearestNeighborMeasurement.NEG_Z,
					new float[]{0, 0, 100});
				prune(hidden_entity_list, zone);
//System.out.println("hel = "+ hidden_entity_list);
				int num = hidden_entity_list.size();
				if (num > 0) {
					
					String msg = intl_mgr.getString(REMOVE_HIDDEN_PRODUCTS_MSG);
					boolean removeAll = popUpConfirm.showMessage(msg);
					if (!removeAll) {
						
						SceneManagementUtility.addTempSurrogate(
								collisionChecker, command);
						/////////////////
						for (int i = 0; i < num; i++) {
							Entity hidden_entity = hidden_entity_list.get(i);
//System.out.println("he = "+ hidden_entity);
							PositionableEntity hidden_pe = (PositionableEntity)hidden_entity;
							
							Matrix4f hidden_pe_mtx = TransformUtils.getTransformsInSceneCoordinates(
								model,
								hidden_pe,
								true);
							
							Matrix4f zone_mtx = TransformUtils.getTransformsInSceneCoordinates(
								model,
								zone,
								true);
							zone_mtx.invert();
							
							mtx.mul(hidden_pe_mtx, zone_mtx);
							
							mtx.get(translation);
							pos_array[0] = translation.x;
							pos_array[1] = translation.y;
							pos_array[2] = translation.z;
//System.out.println("xls = "+ translation);
							rotation.set(mtx);
							rot_array[0] = rotation.x;
							rot_array[1] = rotation.y;
							rot_array[2] = rotation.z;
							rot_array[3] = rotation.angle;
//System.out.println("rot = "+ rotation);
							PositionableEntity tmp_entity = (PositionableEntity)hidden_pe.clone(model);
							tmp_entity.setPosition(pos_array, false);
							tmp_entity.setRotation(rot_array, false);
							
							AddEntityChildCommand aecc = new AddEntityChildCommand(
								model,
								model.issueTransactionID(),
								zone,
								tmp_entity,
								true);
							
							Entity target_parent = rch.findAppropriateParent(aecc);
							if (target_parent != null) {
								//System.out.println("target_parent = "+ target_parent);
							} else {
								target_parent = zone;
							}
							
							Entity source_parent = model.getEntity(hidden_pe.getParentEntityID());
							
							SceneManagementUtility.changeParent(
								model, 
								collisionChecker, 
								view, 
								catalogManager, 
								hidden_pe, 
								source_parent, 
								target_parent, 
								pos_array, 
								rot_array, 
								true, 
								true);
						}
						/////////////////
						SceneManagementUtility.removeTempSurrogate(
								collisionChecker, pos_entity);
					}
				}
			}
		}
		
		result.setResult(true);
		return(result);
	}

    //--------------------------------------------------------------------
    // Local methods
    //--------------------------------------------------------------------

	/**
	 * Cleanup the argument entity_list. Remove everything beyond the zone entity. 
	 * Remove any duplicates on the list. Leave only the top level children (i.e.
	 * no descendant entities of entities on the list.)
	 *
	 * @param entity_list The list to process
	 * @param zone The zone entity to work from
	 */
	private void prune(ArrayList<Entity> entity_list, ZoneEntity zone) {
		
		// remove the zone (and beyond) from the list
		int zone_idx = entity_list.indexOf(zone);
		if (zone_idx != -1) {
			for (int i = entity_list.size() - 1; i >= zone_idx; i--) {
				entity_list.remove(i);
			}
		}
		// remove any duplicate entries on the list
		int num = entity_list.size();
		if (num > 1) {
			int idx = 0;
			int[] remove_idx = new int[num];
			Arrays.fill(remove_idx, num);
			for (int i = 0; i < num - 1; i++) {
				Entity e = entity_list.get(i);
				for (int j = i + 1; j < num; j++) {
					Entity t = entity_list.get(j);
					if (t == e) {
						remove_idx[idx++] = j;
					}
				}
			}
			if (idx > 1) {
				Arrays.sort(remove_idx);
			}
			for (int i = idx - 1; i >= 0; i--) {
				entity_list.remove(remove_idx[i]);
			}
		}
		// remove any entities that are parented to other entities on the list
		num = entity_list.size();
		if (num > 1) {
			int idx = 0;
			int[] remove_idx = new int[num];
			Arrays.fill(remove_idx, num);
			for (int i = 0; i < num; i++) {
				Entity e = entity_list.get(i);
				int e_parent_id = e.getParentEntityID();
				for (int j = 0; j < num; j++) {
					Entity t = entity_list.get(j);
					int t_id = t.getEntityID();
					if (t_id == e_parent_id) {
						remove_idx[idx++] = j;
					}
				}
			}
			if (idx > 1) {
				Arrays.sort(remove_idx);
			}
			for (int i = idx - 1; i >= 0; i--) {
				entity_list.remove(remove_idx[i]);
			}
		}
	}
}
