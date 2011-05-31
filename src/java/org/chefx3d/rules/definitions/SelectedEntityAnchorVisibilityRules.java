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

//External Imports
import java.util.ArrayList;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.ChefX3DRuleProperties.SCALE_RESTRICTION_VALUES;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Determines which anchors should be visible around the selected entity.
 * Project specific implementations should extend this class and override
 * projectSpecificAnchorUpdate(boolean[] activeAnchors).
 *
 * @author jonhubba
 * @version $Revision: 1.29 $
 */
public class SelectedEntityAnchorVisibilityRules extends BaseRule  {

    /** A helper class to handle selection easier */
    private EntitySelectionHelper selectionHelper;

    /** This product cannot change size */
    private static final String NOT_SCALABLE_PROP =
        "org.chefx3d.rules.definitions.CanScaleRule.cannotScale";

    /**
     * Anchor values map as follows:
     * activeAnchors[0] = top left scale
     * activeAnchors[1] = top middle scale
     * activeAnchors[2] = top right scale
     * activeAnchors[3] = right scale
     * activeAnchors[4] = bottom right scale
     * activeAnchors[5] = bottom middle scale
     * activeAnchors[6] = bottom left scale
     * activeAnchors[7] = left scale
     * activeAnchors[8] = delete
     * activeAnchors[9] = rotate
     */
    protected boolean[] activeAnchors;

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public SelectedEntityAnchorVisibilityRules(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        activeAnchors = new boolean[]{
                false, false, false, false, false,
                false, false, false, true, false};
        selectionHelper = EntitySelectionHelper.getEntitySelectionHelper();
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

        if (command instanceof SelectEntityCommand) {

            SelectEntityCommand selectCmd = (SelectEntityCommand)command;

			ArrayList<Entity> selectedList =
				new ArrayList<Entity>(selectionHelper.getSelectedList());

            boolean commandEntityIsSelected = selectCmd.isSelected();
			if (commandEntityIsSelected) {
				if (!selectedList.contains(entity)) {
					selectedList.add(entity);
				}
			} else {
				selectedList.remove(entity);
				if (selectedList.size() > 0) {
					entity = selectedList.get(0);
				} else {
					entity = null;
				}
			}
			int num_selected = selectedList.size();

            activeAnchors = new boolean[]{
                false, false, false, false, false,
                false, false, false, false, false};

			if (num_selected > 1) {

				// multiselection, get rid of all the anchors except delete

				activeAnchors[8] = true;
                for(int i = 0; i < num_selected; i++) {
					Entity e = selectedList.get(i);
                    view.setSelectionAnchors(e, activeAnchors);
				}

			} else if (num_selected == 1) {

				// single selection, extract pertinent rules to determine anchor settings

				Boolean canScale = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.CAN_SCALE_PROP);

				if (canScale == null) {
					canScale = true;
				}

				Boolean canRotate = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.CAN_ROTATE_PROP);

				if (canRotate == null) {
					canRotate = false;
				}

				Boolean canDelete = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.CAN_DELETE_PROP);

				if (canDelete == null) {
					canDelete = true;
				}

				Boolean isEditable = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.IS_EDITABLE_PROP);

				if (isEditable == null) {
					isEditable = true;
				}

				if (!isEditable){
					canScale = false;
					canRotate = false;
				}

				Boolean isAutoSpan = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.SPAN_OBJECT_PROP);

				if (isAutoSpan == null) {
					isAutoSpan = false;
				}

				// Set anchors.
				if (canScale && !isAutoSpan) {

					SCALE_RESTRICTION_VALUES scaleRestriction =
						(SCALE_RESTRICTION_VALUES)
						RulePropertyAccessor.getRulePropertyValue(
						entity,
						ChefX3DRuleProperties.SCALE_RESTRICTION_PROP);

					if (scaleRestriction == null) {
						scaleRestriction = SCALE_RESTRICTION_VALUES.NONE;
					}

					switch(scaleRestriction) {

					case XAXIS:
						activeAnchors = new boolean[]{
							false, false, false, true, false,
							false, false, true, false, false};
						break;

					case YAXIS:
						activeAnchors = new boolean[]{
							false, true, false, false, false,
							true, false, false, false, false};
						break;

					case XYPLANE:
						activeAnchors = new boolean[]{
							true, true, true, true, true,
							true, true, true, false, false};
						break;

					case YZPLANE:
						activeAnchors =new boolean[]{
							false, true, false, false, false,
							true, false, false, false, false};
						break;

					case XZPLANE:
						activeAnchors = new boolean[]{
							false, false, false, true, false,
							false, false, true, false, false};
						break;

					case UNIFORM:
						activeAnchors = new boolean[]{
							true, false, true, false, true,
							false, true, false, false, false};
						break;

					case NONE:
						activeAnchors = new boolean[]{
							true, true, true, true, true,
							true, true, true, false, false};
						break;

					}
				}

				activeAnchors[8] = canDelete;
				activeAnchors[9] = canRotate;

				projectSpecificAnchorUpdate(entity);

				view.setSelectionAnchors(entity, activeAnchors);
			}
        }

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
	protected void projectSpecificAnchorUpdate(Entity entity) {
	}
}
