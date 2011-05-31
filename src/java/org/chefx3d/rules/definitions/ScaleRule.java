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
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Handles the scale/size change input and determines the actual change
 * that should result based on certain properties. Minimum and maximum size
 * constraints will be used along with any restrictions on the axis that
 * scaling can occur on.
 *
 * @author Ben Yarger
 * @version $Revision: 1.30 $
 */
public class ScaleRule extends BaseRule {

	/** Notification of scale beyond bounds */
	private static final String BEYOND_MIN_X_SCALE =
		"org.chefx3d.rules.definitions.ScaleRule.beyondMinimumXScale";

	/** Notification of scale beyond bounds */
	private static final String BEYOND_MIN_Y_SCALE =
		"org.chefx3d.rules.definitions.ScaleRule.beyondMinimumYScale";

	/** Notification of scale beyond bounds */
	private static final String BEYOND_MIN_Z_SCALE =
		"org.chefx3d.rules.definitions.ScaleRule.beyondMinimumZScale";

	/** Notification of scale beyond bounds */
	private static final String BEYOND_MAX_X_SCALE =
		"org.chefx3d.rules.definitions.ScaleRule.beyondMaximumXScale";

	/** Notification of scale beyond bounds */
	private static final String BEYOND_MAX_Y_SCALE =
		"org.chefx3d.rules.definitions.ScaleRule.beyondMaximumYScale";

	/** Notification of scale beyond bounds */
	private static final String BEYOND_MAX_Z_SCALE =
		"org.chefx3d.rules.definitions.ScaleRule.beyondMaximumZScale";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public ScaleRule(
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

		float[] maximumSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MAXIMUM_OBJECT_SIZE_PROP);
//System.out.println("max = "+ java.util.Arrays.toString(maximumSize));
		float[] minimumSize = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.MINIMUM_OBJECT_SIZE_PROP);
//System.out.println("min = "+ java.util.Arrays.toString(minimumSize));
		// Get required values
		float[] scale = new float[3];
		float[] startScale = new float[3];
        float[] size = new float[3];
        double[] pos = new double[3];
        double[] startPos = new double[3];
		float[] rotation = new float[4];

        if (command instanceof ScaleEntityCommand){

            ((ScaleEntityCommand)command).getNewScale(scale);
            ((ScaleEntityCommand)command).getNewPosition(pos);

        } else if (command instanceof ScaleEntityTransientCommand){

        	((ScaleEntityTransientCommand)command).getScale(scale);
        	((ScaleEntityTransientCommand)command).getPosition(pos);

        }

		if (entity instanceof PositionableEntity) {
			PositionableEntity pe = (PositionableEntity)entity;
			pe.getStartingPosition(startPos);
			pe.getSize(size);
			pe.getStartingScale(startScale);
			pe.getStartingRotation(rotation);
		}

		boolean legalSizeChange = isLegalSizeChange(
			scale,
			startScale,
			pos,
			startPos,
			size,
			maximumSize,
			minimumSize,
			rotation);

		if(!legalSizeChange){

	        if (command instanceof ScaleEntityCommand){

	            ((ScaleEntityCommand)command).setNewScale(scale);
	            ((ScaleEntityCommand)command).setNewPosition(pos);

	        } else if (command instanceof ScaleEntityTransientCommand){

	        	((ScaleEntityTransientCommand)command).setScale(scale);
	        	((ScaleEntityTransientCommand)command).setPosition(pos);

	        }

	        result.setResult(false);
	        return(result);
		}

        result.setResult(true);
        return(result);
	}

	/**
	 * Make sure the size change values fall within the limits of the
	 * maximum and minimum size values. If not, correct the corrected
	 * scale value and position value will be set in the respective
	 * parameter objects.
	 *
	 * @param scale float[] x,y,z ordered values (scale change) will be
	 * updated if the scale is beyond the min or max value.
	 * @param oldScale float[] x,y,z ordered values (last known good scale)
	 * @param pos double[] x,y,z ordered values (change in position) will be
	 * updated if the scale is beyond the min or max value.
	 * @param oldPos double[] x,y,z ordered values (last known good pos)
	 * @param defaultSize float[] x,y,z default size
	 * @param maximumSize float[] x,y,z ordered values (maximum size)
	 * @param minimumSize float[] x,y,z ordered values (minimum size)
	 * @return true if legal size change, false otherwise
	 */
	private boolean isLegalSizeChange(
		float[] scale,
		float[] oldScale,
		double[] pos,
		double[] oldPos,
		float[] defaultSize,
		float[] maximumSize,
		float[] minimumSize,
		float[] rotation){

		boolean legalScale = true;
		String statusMsg = "";

		float[] deltaScale = new float[] {
			(scale[0] - oldScale[0]),
			(scale[1] - oldScale[1]),
			(scale[2] - oldScale[2]),
		};

		double[] deltaPos = new double[] {
			(pos[0] - oldPos[0]),
			(pos[1] - oldPos[1]),
			(pos[2] - oldPos[2]),
		};

		float[] currentSize = new float[3];
		float[] oldSize = new float[3];
		for (int i = 0; i < 3; i++) {

			oldSize[i] = oldScale[i] * defaultSize[i];
			currentSize[i] = scale[i] * defaultSize[i];

			if ((deltaScale[i] != 0)) {
			    
			    // convert to integer so that floating point errors are negated
	            int checkSize = Math.round(currentSize[i] * 1000);
                
				if (maximumSize != null) {
				    
				    int maxSize = (int)Math.round(maximumSize[i] * 1000) + 1;
				    if (checkSize > maxSize) {

    					scale[i] = maximumSize[i] / defaultSize[i];
    					currentSize[i] = scale[i] * defaultSize[i];
    
    					switch (i) {
    					case 0:
    						statusMsg = intl_mgr.getString(BEYOND_MAX_X_SCALE);
    						break;
    					case 1:
    						statusMsg = intl_mgr.getString(BEYOND_MAX_Y_SCALE);
    						break;
    					case 2:
    						statusMsg = intl_mgr.getString(BEYOND_MAX_Z_SCALE);
    						break;
    					}
    					legalScale = false;
				    }
				}
				
				if (minimumSize != null) {
				    
	                int minSize = (int)Math.round(minimumSize[i] * 1000) - 1;
				    if (checkSize < minSize) {

    					scale[i] = minimumSize[i] / defaultSize[i];
    					currentSize[i] = scale[i] * defaultSize[i];
    
    					switch (i) {
    					case 0:
    						statusMsg = intl_mgr.getString(BEYOND_MIN_X_SCALE);
    						break;
    					case 1:
    						statusMsg = intl_mgr.getString(BEYOND_MIN_Y_SCALE);
    						break;
    					case 2:
    						statusMsg = intl_mgr.getString(BEYOND_MIN_Z_SCALE);
    						break;
    					}
    					legalScale = false;
				    }
				}
			}
		}
		if (!legalScale) {
			statusBar.setMessage(statusMsg);
//System.out.println(">>>>>>>>>>>>>>>>>>>>>> legalScale = "+ legalScale);
			// get the change in size
			float[] deltaSize = new float[3];
			for (int i = 0; i < 3; i++) {
				deltaSize[i] = oldSize[i] - currentSize[i];
			}
//System.out.println("delta scale = "+ deltaScale[0] +", "+ deltaScale[1] +", "+ deltaScale[2]);
//System.out.println("oldSize = "+ oldSize[0] +", "+ oldSize[1] +", "+ oldSize[2]);
//System.out.println("currentSize = "+ currentSize[0] +", "+ currentSize[1] +", "+ currentSize[2]);
//System.out.println("deltaSize = "+ deltaSize[0] +", "+ deltaSize[1] +", "+ deltaSize[2]);

			// mangle the position changed vector to get the 'sign'
			// parameter for scaling the position changes
			AxisAngle4f aa = new AxisAngle4f(rotation);
			aa.angle = -aa.angle;
			Matrix3f mtx = new Matrix3f();
			mtx.setIdentity();
			mtx.set(aa);

			Vector3f sign = new Vector3f((float)deltaPos[0], (float)deltaPos[1], (float)deltaPos[2]);
			mtx.transform(sign);
//System.out.println("X delta pos = "+ sign);

			sign.x = Math.signum(sign.x * deltaScale[0]);
			sign.y = Math.signum(sign.y * deltaScale[1]);
			sign.z = Math.signum(sign.z * deltaScale[2]);
			sign.negate(); // why ?
			float[] sgn = new float[3];
			sign.get(sgn);
//System.out.println("sign = "+ sign);

			// calculate the new position
          	Vector3f vec = new Vector3f(
				(float)oldPos[0], (float)oldPos[1], (float)oldPos[2]);

			aa.angle = -aa.angle;
			mtx.setIdentity();
			mtx.set(aa);

			// for each component that's changed, add in the delta
			Vector3f dir = new Vector3f();
			for (int i = 0; i < 3; i++) {
				if (deltaScale[i] != 0) {
					float[] f = new float[3];
					f[i] = 1;
					dir.set(f);
//System.out.println("dir = "+ dir);
					mtx.transform(dir);
//System.out.println("X dir = "+ dir);
					dir.scale(sgn[i] * deltaSize[i] * 0.5f);
//System.out.println("S dir = "+ dir);
					vec.add(dir);
				}
			}
			pos[0] = vec.x;
			pos[1] = vec.y;
			pos[2] = vec.z;
		}

		return(legalScale);
	}
}
