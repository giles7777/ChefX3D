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

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.TransformUtils;

/**
* Determines if Entity is colliding with other objects.
*
* @author Ben Yarger
* @version $Revision: 1.42 $
*/
public class ScaleHasObjectCollisionsRule extends BaseRule {

    /** Status message when mouse button released and collisions exist */
    private static final String SCALE_COL_PROP =
        "org.chefx3d.rules.definitions.ScaleHasObjectCollisionsRule.cannotScale";

    /** Status message when illegal collisions exist for scale commands */
    private static final String SCALE_TRANS_COL_PROP =
        "org.chefx3d.rules.definitions.ScaleHasObjectCollisionsRule.collisionsExist";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleHasObjectCollisionsRule(
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
        
        // If this is an ExtrusionEntity temporarily adjust our command to
        // evaluate the scale correctly.
        float[] originalScale = 
        	TransformUtils.getExactScale(
        			(PositionableEntity)entity);
        
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (canMiterCut) {
        	float[] tmpScale = new float[] {1.0f, 1.0f, 1.0f};
        	
        	if (command instanceof ScaleEntityCommand) {
        		((ScaleEntityCommand)command).setNewScale(tmpScale);
        	} else if (command instanceof ScaleEntityTransientCommand){
        		((ScaleEntityTransientCommand)command).setScale(tmpScale);
        	}
        }

        // Perform collision check - initial point at which a check should
        // be performed for scale rules.
        rch.performExtendedCollisionCheck(command, true, false, false);
        
        // If dealing with an ExtrusionEntity, set our scale data back
        if (canMiterCut) {        	
        	if (command instanceof ScaleEntityCommand) {
        		((ScaleEntityCommand)command).setNewScale(originalScale);
        	} else if (command instanceof ScaleEntityTransientCommand){
        		((ScaleEntityTransientCommand)command).setScale(originalScale);
        	}
        }

        // If collisionEntities is null (no collisions occurred) then return false
        if(rch.collisionEntitiesMap == null){

            result.setResult(true);
            return(result);
        }

        // Debug
        //rch.printCollisionEntitiesMap();

        // Perform collision analysis
        rch.performExtendedCollisionAnalysisHelper(null, false, null);

        // If we are colliding with objects not in the relationship
        // classification specified, illegal collisions exist.
        if(rch.hasIllegalCollisionExtendedHelper()){

            if(command instanceof ScaleEntityCommand){

                String msg = intl_mgr.getString(SCALE_COL_PROP);
                popUpMessage.showMessage(msg);

                result.setApproved(false);
                result.setNotApprovedAction(
                		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                result.setStatusValue(ELEVATION_LEVEL.SEVERE);

            } else {

                String msg = intl_mgr.getString(SCALE_TRANS_COL_PROP);
                statusBar.setMessage(msg);
                
                result.setStatusValue(ELEVATION_LEVEL.SEVERE);
            }
            result.setResult(false);
            return(result);
        }

        result.setResult(true);
        return(result);
    }

    //---------------------------------------------------------------
    // Local methods
    //---------------------------------------------------------------
    
    
}
