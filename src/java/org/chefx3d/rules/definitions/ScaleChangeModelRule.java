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

package org.chefx3d.rules.definitions;

// External import

// Internal import
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.SCALE_CHANGE_MODEL_AXIS_VALUES;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.tool.EntityBuilder;

import org.chefx3d.tool.SimpleTool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;

/**
 *  This rule swaps out the current entity for a entity of proper
 *   size if the scale reaches a certain threshold.
 * @author jonhubba
 * @version $Revision: 1.59 $
 */
public class ScaleChangeModelRule extends BaseRule  {

    /** Pop up message when child collision extents check fails */
    private static final String CHILD_COLLISION_FAILURE =
        "org.chefx3d.rules.definitions.ScaleChangeModelRule.childCollisionFailure";
    
    private static final float EPSILON = 0.00001f;

    /** Entity builder to create auto add entities */
    protected EntityBuilder entityBuilder;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleChangeModelRule(
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

        boolean retVal = true;

        // Check if we need to examine any scale change model cases.
        // Look for entity flag to do so.
        Boolean scaleChangeModelFlag =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_FLAG);

        if (!scaleChangeModelFlag) {
            result.setResult(true);
            return(result);
        }

        // We only operate on ScaleEntityCommands
        if (!(command instanceof ScaleEntityCommand)) {
            result.setResult(true);
            return(result);
        }

        // We have to have a PositionableEntity
        PositionableEntity pEntity;

        if (entity instanceof PositionableEntity) {
            pEntity = (PositionableEntity)entity;
        } else {
            String msg = intl_mgr.getString(CHILD_COLLISION_FAILURE);
            popUpMessage.showMessage(msg);
            result.setResult(false);
            return(result);
        }

        // Process the swap
        retVal = changeModelProcess(
                pEntity, model, (ScaleEntityCommand) command);

        if (retVal) {

            // Kill the current command so the scale doesn't happen.
            // Instead do the swap.
            result.setApproved(false);
            result.setNotApprovedAction(
                    NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);

        } else {
            result.setResult(true);
        }

        return(result);

    }


    /**
     * Begins the model change process. Stores the original size of the model
     * Calculates whether or not the model should swap and then if necessary
     * swaps the model.
     *
     * @param entity The current Entity
     * @param model The world Model
     * @param command The Scale Entity Command
     * @return True if successful, false otherwise
     */
    private boolean changeModelProcess(
            PositionableEntity entity,
            WorldModel model,
            ScaleEntityCommand command) {

        SCALE_CHANGE_MODEL_AXIS_VALUES axis = (SCALE_CHANGE_MODEL_AXIS_VALUES)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SCALE_CHANGE_MODEL_AXIS);

        // The rules scale values to determine when a model change needs to swap.
        // These are scale increments that denote a model change.
        float[] ruleScaleValues = (float[])
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_SIZE);

        // Safety check data
        if (axis == null || ruleScaleValues == null) {
            return false;
        }

        // get the current size
        float[] size = new float[3];
        ((PositionableEntity)entity).getSize(size);

        // Get the new scale from the command
        float[] newScale = new float[3];
        command.getNewScale(newScale);

        // Calculate the resulting new size that will result from the new scale.
        float[] trueSize = new float[3];
        trueSize[0] = (newScale[0] * size[0]);
        trueSize[1] = (newScale[1] * size[1]);
        trueSize[2] = (newScale[2] * size[2]);
/*
        // If this entity is an auto span, we want to remove the auto span
        // overlap component from the trueSize value used to determine the
        // model to swap to.
        Boolean autoSpan = (Boolean)
	        RulePropertyAccessor.getRulePropertyValue(
	            entity,
	            ChefX3DRuleProperties.SPAN_OBJECT_PROP);

	    if(autoSpan != null && autoSpan == true){
	        trueSize[0] -= BoundsUtils.SPAN_OVERLAP_THRESHOLD;
	        trueSize[1] -= BoundsUtils.SPAN_OVERLAP_THRESHOLD;
	        trueSize[2] -= BoundsUtils.SPAN_OVERLAP_THRESHOLD;
	    }
*/	    
	    // Apply the epsilon to remove the floating point variablity in our
	    // comparisons later.
	    trueSize[0] -= EPSILON;
        trueSize[1] -= EPSILON;
        trueSize[2] -= EPSILON;

        // Search for the size to swap to and then do it
        int index = 0;

        // record the first index where trueSize[axis] is less than
        // or equal to ruleScaleValues[index], or simply record the
        // final index of ruleScaleValues.
        for (int i = 0; i < ruleScaleValues.length; i++) {

            switch (axis) {

                case XAXIS:
                    if (trueSize[0] <= ruleScaleValues[i]) {
                        i = ruleScaleValues.length;
                    } else {
                        index++;
                    }
                    break;

                case YAXIS:
                    if (trueSize[1] <= ruleScaleValues[i]) {
                        i = ruleScaleValues.length;
                    } else {
                        index++;
                    }
                    break;

                case ZAXIS:
                    if (trueSize[2] <= ruleScaleValues[i]) {
                        i = ruleScaleValues.length;
                    } else {
                        index++;
                    }
                    break;
            }
        }
        
        // Make sure we prevent any array index out of bounds issue.
        if (index >= ruleScaleValues.length) {
        	index = ruleScaleValues.length - 1;
        }

        // find the switch tool
        String[] productIDArray = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.SCALE_CHANGE_MODEL_PROD);

        // Get the tool id matching the swap index passed in
        String id = productIDArray[index];

        SimpleTool tool =
            (SimpleTool) catalogManager.findTool(id);

        if (tool == null) {
            String msg = intl_mgr.getString(CHILD_COLLISION_FAILURE);
            popUpMessage.showMessage(msg);

            return false;
        }

        return swapModel(model, entity, tool, command);

    }

    /**
     * Issue the actual swap commands.
     *
     * @param model WorldModel to reference
     * @param entity Entity to swap out
     * @param tool SimpleTool to use to create swap replacement
     * @param scaleCmd ScaleEntityCommand applied to entity
     * @return True if successful, false otherwise
     */
    private boolean swapModel(
            WorldModel model,
            PositionableEntity entity,
            SimpleTool tool,
            ScaleEntityCommand scaleCmd) {

        // get the position data
        double[] position = new double[] {0, 0, 0};
        float[] scale = new float[] {1, 1, 1};
        float[] size = new float[] {1, 1, 1};
        float[] rotation = new float[] {0, 1, 0, 0};

        scaleCmd.getNewPosition(position);
        scaleCmd.getNewScale(scale);
        entity.getRotation(rotation);
        entity.getSize(size);

        // Create the new entity
        PositionableEntity swapEntity = (PositionableEntity)
            view.getEntityBuilder().createEntity(
                model,
                model.issueEntityID(),
                position,
                rotation,
                tool);

        //------------------------------------------------------
        // Copy over the specific properties we need to preserve
        //------------------------------------------------------
        
        // copy internal only properties
        List<String> propKeys =
            ChefX3DRuleProperties.INTERNAL_PROP_LEYS;
        for (int i = 0; i < propKeys.size(); i++) {

            // get the current value
            String propName = propKeys.get(i);
            Object propValue =
                entity.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        propName);

            // set the value in the new entity
            swapEntity.addProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    propName,
                    propValue);
        }

        // copy editable properties
        List<EntityProperty> editableProps =
            entity.getProperties(Entity.EDITABLE_PROPERTIES);
        for (int j = 0; j < editableProps.size(); j++) {

            EntityProperty prop = editableProps.get(j);
            swapEntity.addProperty(
                    prop.propertySheet,
                    prop.propertyName,
                    prop.propertyValue);

        }
        
        //-----------------------------------------------------------------
        // Done copying over specific properties that need to be preserved.
        //-----------------------------------------------------------------

        // Calculate proper scale of new swapped model.
        // This needs to be done to scale the target model correctly to match
        // the final size of the source model of the swap.
        float[] newEntityScale = new float[] {1, 1, 1};
        float[] newEntitySize = new float[3];

        swapEntity.getScale(newEntityScale);
        swapEntity.getSize(newEntitySize);

        float[] oldEntityScale = new float[3];
        float[] oldEntitySize = new float[3];

        scaleCmd.getNewScale(oldEntityScale);
        entity.getSize(oldEntitySize);

        newEntityScale[0] =
            oldEntityScale[0] * oldEntitySize[0] / newEntitySize[0];
        newEntityScale[1] =
            oldEntityScale[1] * oldEntitySize[1] / newEntitySize[1];
        newEntityScale[2] =
            oldEntityScale[2] * oldEntitySize[2] / newEntitySize[2];

        swapEntity.setScale(newEntityScale);
        
        // Pull out the start children data
        HashMap<Entity, PositionableData> startChildrenData = 
        	scaleCmd.getStartChildrenData();
        
        Entity[] startChildren = null;

        if (startChildrenData != null) {
        	
	        Object[] keys = startChildrenData.keySet().toArray();
	        startChildren = new Entity[keys.length];
	        
	        for (int i = 0; i < keys.length; i++) {
	        	startChildren[i] = (Entity) keys[i];
	        }
        }

        // Finish off with the swap
        int transactionID = SceneManagementUtility.swapEntity(
                model,
                rch,
                view,
                catalogManager,
                swapEntity,
                entity,
                startChildren,
                false,
                false);

        if (transactionID < 0) {
            return false;
        }

        return true;

    }
}