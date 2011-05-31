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

//Internal Imports
import java.util.ArrayList;

import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.RemoveEntityChildCommand;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.util.SceneHierarchyUtility;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
* Makes sure any delete operation on a template object removes the template
* entity if the object removed is the last child of that template
*
* @author Ben Yarger
* @version $Revision: 1.14 $
*/
public class DeleteTemplateRule extends BaseRule  {

	/**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public DeleteTemplateRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        ruleType = RULE_TYPE.INVIOLABLE;
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
	    int templateEntityID = entity.getTemplateEntityID();

        if (templateEntityID == -1) {
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

        // Get the template entity and see how many children are left.
        // If there are no more children, delete the template
        Entity templateEntity = model.getEntity(templateEntityID);

        if (templateEntity == null) {
            result.setResult(true);
            return(result);
        }

        // Get the children of the template
        ArrayList<Entity> templateChildren = new ArrayList<Entity>();
       	getTemplateEntities(zoneEntity, templateEntityID, templateChildren);

        if (templateChildren != null) {

        	// We have to consider auto span products that are attached
        	// to one of the walls and to the last product. If this case
        	// exists, go ahead and remove the template, because, as soon
        	// as the product that the auto span is connected to removes
        	// so too will the auto span product.
        	int autoSpanProductCound = 0;

        	for (int i = 0; i < templateChildren.size(); i++) {

        		Boolean isAutoSpan = (Boolean)
        			RulePropertyAccessor.getRulePropertyValue(
        					templateChildren.get(i),
        					ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        		if (isAutoSpan) {
        			autoSpanProductCound++;
        		}
        	}

        	// If the number of auto span entities, plus one equals the size
        	// of the children set, then remove the template.
        	if (templateChildren.size() == 0 || 
        			autoSpanProductCound + 1 == templateChildren.size()) {

	        	int templateParentID = templateEntity.getParentEntityID();
	        	Entity templateParentEntity = model.getEntity(templateParentID);

	        	RemoveEntityChildCommand removeCmd =
	        		new RemoveEntityChildCommand(
	        				model,
	        				templateParentEntity,
	        				templateEntity);
	        	removeCmd.setBypassRules(true);
	        	
	        	addNewlyIssuedCommand(removeCmd);
        	}
        }

        result.setResult(true);
        return(result);
	}

	/**
     * Assign any entities to be deleted if they match the templateEntityID
     *
     * @param entity Entity to start search with
     * @param templateEntityID Template entity id to match
     * @param foundChildren contains the entities found by the search
     */
    private void getTemplateEntities(
    		Entity entity,
    		int templateEntityID,
    		ArrayList<Entity> foundChildren){

        ArrayList<Entity> children = 
        	SceneHierarchyUtility.getExactChildren(entity);
        int len = children.size();

        for (int i = 0; i < len; i++) {

            Entity child = children.get(i);
            int checkID = child.getTemplateEntityID();

            if (checkID == templateEntityID) {
                foundChildren.add(child);
            } else {
                getTemplateEntities(child, templateEntityID, foundChildren);
            }
        }
    }
}
