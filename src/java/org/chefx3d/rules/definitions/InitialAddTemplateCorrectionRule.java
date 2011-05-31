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
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.rule.RuleEvaluationResult;

import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Handles initial position corrections for entities that are part of a
 * template or kit. Looks for movement restriction on template or kit
 * and then a wall floor relationship and if both are found places
 * entity at calculated height above floor appropriate for kit or
 * template.
 *
 * @author Ben Yarger
 * @version $Revision: 1.14 $
 */
public class InitialAddTemplateCorrectionRule extends
		InitialAddPositionCorrectionRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public InitialAddTemplateCorrectionRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

        //ruleType = RULE_TYPE.INVIOLABLE;

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

        //this.result = result;

		// Check for add commands
		if (command instanceof AddEntityCommand) {
			;
		} else if (command instanceof AddEntityChildCommand) {
			;
		} else if (command instanceof AddEntityChildTransientCommand) {
			;
		} else {
            result.setResult(true);
            return(result);
		}

		// See if it is a member of a kit or template
		int kitID = entity.getKitEntityID();
		int templateID = entity.getTemplateEntityID();

		// If we have a kit or template, perform any relevant adjustments
		if (kitID != -1 || templateID != -1) {

		    HashMap<String, Map<String, Object>> parentPropertyMap = null;
		    Object propertyMap = entity.getProperty(
		    		Entity.DEFAULT_ENTITY_PROPERTIES,
		    		TemplateEntity.TEMPLATE_PROPERTIES);

		    // when pasting a part of the kit/template the property is a String,
		    // this should just be ignored in this case.
		    if (propertyMap instanceof HashMap) {
		        // We want to see if the template or kit has movement restraints.
	            // If so, apply them accordingly here to the entity.
		        parentPropertyMap =
		            (HashMap<String, Map<String, Object>>)entity.getProperty(
	                        Entity.DEFAULT_ENTITY_PROPERTIES,
	                        TemplateEntity.TEMPLATE_PROPERTIES);
		    }

			if (parentPropertyMap == null) {
	            result.setResult(true);
	            return(result);
			}

			HashMap<String, Object> parentDefaultEntityProperties =
				(HashMap<String, Object>)
				parentPropertyMap.get(Entity.DEFAULT_ENTITY_PROPERTIES);

			if (parentDefaultEntityProperties == null) {
	            result.setResult(true);
	            return(result);
			}

			// Apply parent entity kit or template movement restrictions
			// to the child if wall:floor is found
			processTemplateOrKitMovementRestriction(
					parentDefaultEntityProperties,
					entity);

			// Lastly, get the appropriate parent and fake the response class
			// adjustment that is otherwise expected to occur when moving a
			// single entity via the mouse.
			Entity parent = rch.findAppropriateParent(command);

			if (parent != null) {

				double[] relativePosition = new double[3];

				double[] parentPosition =
					TransformUtils.getPositionRelativeToZone(model, parent);
				double[] entityPosition =
					TransformUtils.getPositionRelativeToZone(model, command);

				if (parentPosition != null && entityPosition != null) {

					relativePosition[0] =
						entityPosition[0] - parentPosition[0];
					relativePosition[1] =
						entityPosition[1] - parentPosition[1];
					relativePosition[2] =
						entityPosition[2] - parentPosition[2];

					if (command instanceof AddEntityChildCommand) {

						((AddEntityChildCommand)
								command).setParentEntity(parent);

						((PositionableEntity)entity).setPosition(
								relativePosition,
								false);

					} else if (command instanceof
							AddEntityChildTransientCommand) {

						((AddEntityChildTransientCommand)
								command).setParentEntity(parent);

						((PositionableEntity)entity).setPosition(
								relativePosition,
								false);
					}
				}
			}

			// Apply standard initial position correction procedures
			// to this entity if it is parented to the wall.

	        // TODO: performCheck should be in a utility class
			//InitialAddPositionCorrectionRule rule =
	        //    new InitialAddPositionCorrectionRule(errorReporter, model, view);

			//rule.outsideRuleCall(entity, model, command);
			super.performCheck(entity, command, result);

		}

        result.setResult(true);
        return(result);
	}

	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------

	private void processTemplateOrKitMovementRestriction(
			HashMap<String, Object> parentDefaultEntityProperties,
			Entity entity) {

		ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES
			parentMoveRes =
			(ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES)
			parentDefaultEntityProperties.get(
					ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_PLANE_PROP);

		if (parentMoveRes == null ||
				parentMoveRes ==
					ChefX3DRuleProperties.MOVEMENT_PLANE_RESTRICTION_VALUES.NONE) {
			return;
		}

		// Now see if there is a relationship that includes wall and floor
		String[] classRelationships = (String[])
			parentDefaultEntityProperties.get(
					ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);

		if (!hasWallFloorRelationship(classRelationships)) {
			return;
		}

		// Now that we have gotten to here, we know that movement
		// restrictions exist on the template or kit that the entity
		// belongs to. We also know that the parent has a relationship that
		// contains wall and floor. So, we need to make sure that all
		// entities get adjusted here to reflect this.
		float[] templateBounds = (float[])
			entity.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				TemplateEntity.TEMPLATE_BOUNDS);

		double[] delta = (double[])
			entity.getProperty(
					Entity.DEFAULT_ENTITY_PROPERTIES,
					TemplateEntity.TEMPLATE_ENTITY_DELTA_POSITION);

		double[] placementPosition = new double[3];
		((PositionableEntity)entity).getPosition(placementPosition);

		// Adjust the height to where it should auto position to
		placementPosition[1] = Math.abs(templateBounds[2]) + delta[1];

		((PositionableEntity)entity).setPosition(placementPosition, false);
	}

	/**
	 * Check to see if the any relationship classifications contains both
	 * wall and floor relationships.
	 *
	 * @param classRelationship Classifications to check
	 * @return True if wall floor found, false otherwise
	 */
	private boolean hasWallFloorRelationship(String[] classRelationship) {

		if(classRelationship != null) {

			for(int i = 0; i < classRelationship.length; i++){

				StringTokenizer st =
					new StringTokenizer(classRelationship[i], ":");
				String token;

				boolean containsWall = false;
				boolean containsFloor = false;

				while(st.hasMoreTokens()) {

					token = st.nextToken();

					if (token.equalsIgnoreCase(
							ChefX3DRuleProperties.WALL_CONST)) {

						containsWall = true;

					} else if (token.equalsIgnoreCase(
							ChefX3DRuleProperties.FLOOR_CONST)) {

						containsFloor = true;

					}

					if (containsWall && containsFloor) {
						return true;
					}
				}
			}
		}

		return false;
	}
}
