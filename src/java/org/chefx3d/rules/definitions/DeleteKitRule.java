/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2009
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
* Makes sure any delete operation on a kit object removes all
* the individual entities that comprise the kit
*
* @author Russell Dodds
* @version $Revision: 1.17 $
*/
public class DeleteKitRule extends BaseRule  {

    /** The unique ID of the kit being deleted */
    private int kitEntityID;

    /** Delete result in removal pop up */
    private static final String POP_UP_DELETE_REMOVE =
        "org.chefx3d.rules.definitions.DeleteKitRule.deleteRemoveKit";

	/**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public DeleteKitRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.STANDARD;
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

        this.result = result;

	    // ignore rule if this is removing the shadow entity
	    Boolean shadowState = (Boolean)entity.getProperty(
                entity.getParamSheetName(),
                Entity.SHADOW_ENTITY_FLAG);

	    if (shadowState != null && shadowState == true) {
            result.setResult(true);
            return(result);
	    }

	     // save the kitID to use for comparisons
	    kitEntityID = entity.getKitEntityID();

        if (kitEntityID == -1) {
            result.setResult(true);
            return(result);
        }

	    // Search the zone for all items with the matching kitID
	    Entity zoneEntity =
	    	SceneHierarchyUtility.findZoneEntity(model, entity);

	    // we need the zone in order for this to work
	    if (zoneEntity == null) {
            result.setResult(false);
            return(result);
	    }

        // make sure the user wants to do this.
        String msg = intl_mgr.getString(POP_UP_DELETE_REMOVE);
        if (!popUpConfirm.showMessage(msg)) {
            result.setApproved(false);
            result.setResult(false);
            return(result);
        }

        // suppress any other messages
        popUpConfirm.setDisplayPopUp(false);
        popUpConfirm.setConfirmedFlag(true);

	    // get the list of entities to delete
	    ArrayList<Entity> deleteList = new ArrayList<Entity>();
	    getKitEntities(zoneEntity, deleteList);

	    // ensure we don't try to delete the selected entity twice
	    if (deleteList.contains(entity)) {
	        deleteList.remove(entity);
	    }

	    // need to delete the hidden kit entity
	    Entity kitEntity = model.getEntity(kitEntityID);
	    deleteList.add(kitEntity);

	    // create the extra deletes from the list
	    ArrayList<Command> commandList = new ArrayList<Command>();
	    int len = deleteList.size();
	    for (int i = 0; i < len; i++) {

	        // process next item
	        Entity child = deleteList.get(i);

	        if (child == null)
	            continue;

	        // Create the remove command
	        Entity parent = model.getEntity(child.getParentEntityID());
	        RemoveEntityChildCommand cmd =
	            new RemoveEntityChildCommand(model, parent, child);
	        cmd.setBypassRules(true);
	        commandList.add(cmd);

	        /*
	         * Perform collision check so we can remove auto-span items
	         */
	        // perform the collision check
	        rch.performExtendedCollisionCheck(cmd, true, false, false);
	        
	        // get the list of collisions and process them
	        if(rch.collisionEntitiesMap != null) {

	            Iterator<Map.Entry<Entity, ArrayList<Entity>>> itr = 
	                rch.collisionEntitiesMap.entrySet().iterator();
	            
	            while (itr.hasNext()) {
	                
	                Map.Entry<Entity, ArrayList<Entity>> item = itr.next();
	                
	                ArrayList<Entity> collisionList = item.getValue();
	                for (int j = 0; j < collisionList.size(); j++) {

	                    PositionableEntity tmpEntity =
	                        (PositionableEntity)collisionList.get(j);
	                                        
	                    Boolean autoSpan = 
	                        (Boolean)RulePropertyAccessor.getRulePropertyValue(
	                                tmpEntity,
	                                ChefX3DRuleProperties.SPAN_OBJECT_PROP);
	                    
	                    if (autoSpan) {
	                        // Create the remove command
	                        parent = model.getEntity(tmpEntity.getParentEntityID());
	                        cmd = new RemoveEntityChildCommand(model, parent, tmpEntity);
	                        cmd.setBypassRules(true);
	                        commandList.add(cmd);
	                    }
	                }

	            }
	            
	        }
	    }

        // add the new commands to the list of commands to issue
        addNewlyIssuedCommand(commandList);

        result.setResult(true);
        return(result);

	}

	/**
     * Assign any entities to be deleted if they match the kitID
     *
     * @param entity Entity to start search with
     * @param deleteList The list of items to delete
     */
    private void getKitEntities(Entity entity, ArrayList<Entity> deleteList){

        ArrayList<Entity> children = entity.getChildren();
        int len = children.size();
        for (int i = 0; i < len; i++) {
            Entity child = children.get(i);

            int checkID = child.getKitEntityID();
            if (child.getEntityID() == kitEntityID || checkID == kitEntityID) {
                // add the child, skip any children since they will be deleted
                // as part of this entity's deletion
                deleteList.add(child);

            } else {
                getKitEntities(child, deleteList);
            }

        }

    }

}
