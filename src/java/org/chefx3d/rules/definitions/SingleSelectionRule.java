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
import java.util.ArrayList;

//Internal Imports
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntitySelectionHelper;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.WorldModel;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Rule for single selection.  When a selection occurs,
 * if Boolean SINGLE_SELECTION_ONLY [CX.singleSel] is true,
 * deselect any others.
 *
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.10 $
 */
public class SingleSelectionRule extends BaseRule  {

    /** A helper class to handle selection easier */
    private EntitySelectionHelper selectionHelper;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SingleSelectionRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        selectionHelper = EntitySelectionHelper.getEntitySelectionHelper();
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

        if(command instanceof SelectEntityCommand) {

            SelectEntityCommand selectCmd = (SelectEntityCommand)command;
            ArrayList<Entity> selectedList = selectionHelper.getSelectedList();
            int len = selectedList.size();

            //
            // if the current item is being selected and it demands
            // single selection, we will do special processing below
            //
            Boolean entityDemandsSingleSelection = (Boolean)
				RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SINGLE_SELECTION_ONLY);

            if( entityDemandsSingleSelection == null)
            	entityDemandsSingleSelection = false;



            if (len >= 1 && entityDemandsSingleSelection && selectCmd.isSelected()) {

				//
				// iterate through the list of already-selected entities to
            	// find all those that ALSO demand single selection. Only the
            	// most recent selection should occur, in that case.
				//
				for(Entity entitySel : selectedList){

                	Boolean singleSelected = (Boolean)
                    	RulePropertyAccessor.getRulePropertyValue(
                    			entitySel,
                    			ChefX3DRuleProperties.SINGLE_SELECTION_ONLY);

                    //
                    // deselect the already-selected entity so that
    				// we select only the 'incoming' selected entity.
                    //
                	if( singleSelected != null && singleSelected){
                		SelectEntityCommand unselect =
                            new SelectEntityCommand(model, entitySel, false);
                		addNewlyIssuedCommand(unselect);
                    }
                }
            }
        } // end if(command instanceof SelectEntityCommand)

        result.setResult(true);
        return(result);
    }

    /**
     * Override this method to implement project specific anchor visibility.
     * Will still use the default implementation as a standardized baseline.
     *
     * @param activeAnchors boolean[] of anchor visibility values
     * @param entity Selected entity
     */
    protected void projectSpecificAnchorUpdate(Entity entity){}
}
