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

import org.chefx3d.view.common.EditorView;

/**
 * Determines if there is a restriction on the rotation axis.
 *
 * @author Ben Yarger
 * @version $Revision: 1.23 $
 */
public class CanRotateAlongAxisRule extends BaseRule {

    private static float zeroTollerance = 0.0001f;

    /** Status message when rotation is restricted */
    private static final String ROTATE_RESTRICTED_PROP =
        "org.chefx3d.rules.definitions.CanRotateAlongAxisRule.rotationRestricted";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public CanRotateAlongAxisRule(
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
     * Check if there is a restriction on rotation axis. If the current
     * editor plane is the same as the restricted axis of rotation, then
     * permit the rotation.
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

        ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS rotRestriction =
            (ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.ROTATION_AXIS_RESTRICTION_PROP);

        if(rotRestriction != null){

            float[] curAxisAngleRotation = new float[] {
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f};

            if(command instanceof RotateEntityCommand){
                ((RotateEntityCommand)command).getCurrentRotation(
                        curAxisAngleRotation);
            } else {
                ((RotateEntityTransientCommand)command).getCurrentRotation(
                        curAxisAngleRotation);
            }

            if(curAxisAngleRotation == null){
                result.setResult(false);
                return(result);
            }

            String msg = intl_mgr.getString(ROTATE_RESTRICTED_PROP);

            /*
             * To account for small error in transform calculation, if the
             * other two axis of rotation are greater than the set tolerance
             * then prevent the rotation.
             */
            boolean good = false;

            switch(rotRestriction){

                case NONE:
                    good = true;
                    break;
                case XAXIS:

                    good =
                        tolleranceCheck(
                                curAxisAngleRotation[1],
                                curAxisAngleRotation[2]);
                    break;

                case YAXIS:

                    good =
                        tolleranceCheck(
                                curAxisAngleRotation[0],
                                curAxisAngleRotation[2]);

                    break;

                case ZAXIS:

                    good =
                        tolleranceCheck(
                                curAxisAngleRotation[0],
                                curAxisAngleRotation[1]);

                    break;
            }

            if (!good) {

                if(command.isTransient()){

                    ((RotateEntityTransientCommand)command).
                    setCommandShouldDie(true);

/*                  float[] rot = new float[4];

                    ((PositionableEntity)entity).getRotation(rot);

                    ((RotateEntityTransientCommand)command).setCurrentRotation(
                            rot[0],
                            rot[1],
                            rot[2],
                            rot[3]);
*/              }

                statusBar.setMessage(msg);

                result.setResult(false);
                return(result);
            }
        }

        result.setResult(true);
        return(result);
    }

    /**
     * Determine if axis values are within constraint tollerance.
     *
     * @param axisA float axis value
     * @param axisB float axis value
     * @return true if inside legal tollerance, false otherwise
     */
    private boolean tolleranceCheck(float axisA, float axisB){

        if(Math.abs(axisA) > zeroTollerance){

            return false;
        } else if (Math.abs(axisB) > zeroTollerance){

            return false;
        }

        return true;
    }
}
