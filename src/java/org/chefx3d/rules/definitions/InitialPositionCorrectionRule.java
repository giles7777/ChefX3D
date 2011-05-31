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
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;

import org.chefx3d.rules.util.BoundsUtils;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SetRelativePositionUtility;
import org.chefx3d.rules.util.TransformUtils;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorView;

/**
 * Provides shared initial position methods used by initial position correction
 * classes. These classes take care of the missing response handling actions
 * coming from the ChefX3D response classes. This allows for the separation
 * between rules and ChefX3D, keeping rules as an independent accessory but not
 * requisite for ChefX3D projects.
 *
 * The purpose of the initial position corrections are to handle any parenting
 * and positioning needs of the incoming position related command. These are
 * going to be either move or add related commands.
 *
 * @author Ben Yarger
 * @version $Revision: 1.56 $
 */
public abstract class InitialPositionCorrectionRule extends BaseRule {

    protected static double EMBED_DEPTH = 0.0010;

    /**
     * Constructor
     * 
     * @param errorReporter
     * @param model
     * @param view
     */
    public InitialPositionCorrectionRule(
            ErrorReporter errorReporter,
            WorldModel model,
            EditorView view){

        super(errorReporter, model, view);

    }

    /**
     * Calculate the changes to the depth of the entity position based on the
     * parent and the flags evaluated. Checks that the entity position relative
     * to parent will is in a correctly depth stacked position to start with.
     *
     * We know that the starting values from any response class will have the
     * entity stacked as far forward as the nearest z depth bounds entity.
     * In other words, if a tower base has a 10" drawer inside of it and the
     * drawer's handle sticks out further than the front of the tower base, any
     * entity being placed over this assembly can be expected to have its back
     * bounds positioned at the front bounds of the 10" drawer.
     *
     * @param command Command to do collision check on
     * @param model WorldModel to reference
     * @param entity Entity to change position for
     * @param parentEntity Parent to evaluate entity against
     * @param entityPosition Position of entity to adjust
     * @return depth change value (negative to move back towards zone)
     */
    protected double calculateDepthChanges(
            Command command,
            WorldModel model,
            Entity entity,
            Entity parentEntity) {

        double depthChange = 0.0;
        double planeEveningOffset = 0.0;

        float[] entityBounds = new float[6];
        float[] parentBounds = new float[6];

        if (entity instanceof PositionableEntity &&
                parentEntity instanceof PositionableEntity) {
            
            entityBounds = BoundsUtils.getBounds((PositionableEntity)entity, true);
            parentBounds = BoundsUtils.getBounds((PositionableEntity)parentEntity, true);

        } else {
            return depthChange;
        }

        // Make sure we are correctly stacked position wise relative
        // to our parent.
        double[] zoneRelativeChildPos =
            TransformUtils.getExactPositionRelativeToZone(model, command);

        double[] zoneRelativeParentPos =
            TransformUtils.getPositionRelativeToZone(model, parentEntity);

        if (zoneRelativeChildPos == null ||
                zoneRelativeParentPos == null) {
            return depthChange;
        }

        double parentForwardBounds =
            zoneRelativeParentPos[2] + parentBounds[5];
        double childBackBounds =
            zoneRelativeChildPos[2] + entityBounds[4];

        // Special casing for zones and segments
        if (parentEntity instanceof SegmentEntity) {
            ((SegmentEntity)parentEntity).getLocalBounds(parentBounds);
            parentForwardBounds = parentBounds[3];
        } else if (parentEntity instanceof ZoneEntity) {
            parentForwardBounds = 0.0;
        }

        if ((childBackBounds - parentForwardBounds) > 0.0) {
            planeEveningOffset = parentForwardBounds - childBackBounds;
            zoneRelativeChildPos[2] -= (childBackBounds - parentForwardBounds);
        } else if ((parentForwardBounds - childBackBounds) > 0.0) {
            planeEveningOffset = parentForwardBounds - childBackBounds;
            zoneRelativeChildPos[2] += (parentForwardBounds - childBackBounds);
        }

        // Set our parent is zone flag
        boolean parentIsZone = false;

        if (parentEntity.isZone()) {
            parentIsZone = true;
        }

        //--------------------------------
        // Extract properties for analysis
        //--------------------------------
        Boolean placeInsideParent = (Boolean)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.PLACED_INSIDE_PARENT);

        Float embedDepth = (Float)
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.EMBED_DEPTH_PROP);

        String[] allowedParentClassifications = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

        // If there are allowed parents in use, this is our flag to move back
        // to be in collision with that parent.
        if (allowedParentClassifications != null && !parentIsZone) {

            // make sure the parentEntity matches the allowed classifications
            String[] parentClassifications = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                        parentEntity,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

            boolean legalMatch = false;

            if (parentClassifications != null) {

                for (int i = 0; i < allowedParentClassifications.length; i++) {

                    for (int j = 0; j < parentClassifications.length; j++) {

                        if (allowedParentClassifications[i].equalsIgnoreCase(
                                parentClassifications[j])) {

                            legalMatch = true;
                            break;
                        }
                    }

                    if (legalMatch) {
                        break;
                    }
                }
            }

            if (legalMatch) {

                if (!parentEntity.isModel()) {
                    zoneRelativeParentPos = new double[3];
                    zoneRelativeParentPos[0] = 0.0;
                    zoneRelativeParentPos[1] = 0.0;
                    zoneRelativeParentPos[2] = 0.0;
                }

                depthChange =
                    (zoneRelativeParentPos[2] + parentBounds[5]) -
                    (zoneRelativeChildPos[2] + entityBounds[4]);
            }
        }

        // Place inside parent. If depth of child is greater than parent,
        // make sure child is pushed forward the difference. Otherwise zero
        // out the relative z position.
        if (placeInsideParent && !parentIsZone) {

            // Only do this if the parent entity is a type model
            if (parentEntity.isModel()) {

                // Push this forward some multiplier on the embed depth
                // to make sure we put it in the parent, but not out the
                // back or close enough to collide with anything behind
                // the parent.

                depthChange =
                    zoneRelativeParentPos[2] - zoneRelativeChildPos[2];

                // If the depth of the entity is greater than the depth of
                // the parent + EMBED_DEPTH * 3 then we have to move the
                // entity forward enough so it wont stick out the back.
                // The reason we include EMBED_DEPTH * 3 is that after all
                // correct positions have been calculated. There will be a
                // 1 mm overlap between each entity depth wise. Therefore,
                // the distance between our stacking entity and the stacking
                // entities parent parent is 2mm less than the total depth
                // of the parent and stack relative to the front bounds of
                // the parent parent. We want to add in that extra 1 mm
                // for collision checking safety.
                //
                // Because this is place inside parent and we are experiencing
                // this condition, the only way we are going to be able to
                // get this entity into the correct position is to bump it out
                // the back bounds of the child and let gen pos fix the 
                // problem. Otherwise we are dealing with tolerances that are 
                // too small that rounding error is munging it all up. The old 
                // calculation has been left in but commented out in case we 
                // wanted to revisit this in the future.
                if (entityBounds[4] < (parentBounds[4]+(EMBED_DEPTH*3))) {

                    double oversizeAllowance =
                        Math.abs(entityBounds[4]);

                    //double oversizeAllowance =
                    //  entityBounds[4] - parentBounds[4];
                    //
                    //oversizeAllowance = Math.abs(oversizeAllowance);

                    depthChange += oversizeAllowance + (EMBED_DEPTH*3);

                }
            }
        }

        // Make sure depth change is zero if parent is zone
        if (parentIsZone) {

            if (zoneRelativeChildPos[2] + entityBounds[4] > 0.0){
                depthChange =
                    Math.abs(entityBounds[4]) - zoneRelativeChildPos[2];
            } else {
                depthChange = 0.0;
            }
        }

        // Add in the custom embedDepth if set
        if (embedDepth != null) {
            depthChange = depthChange - embedDepth;// + EMBED_DEPTH;
        } else {
            depthChange = depthChange - EMBED_DEPTH;
        }

        // Add in the plane evening offset as the last action.
        depthChange += planeEveningOffset;

        return depthChange;
    }

    /**
     * Certain types of models are embedded into the cut-outs
     * in the segment, rather than being placed on the surface
     * This should override the depth amount set as we assume
     * doors and windows are being set into walls and can then
     * easily determine what the actual depth value should be.
     *
     * @param entity Entity to examine
     * @param position position of entity to adjust depth for
     * @param isTransient Is the entity in a transient state
     */
    protected void handleDoorAndWindowCase(
            Entity entity,
            double[] position,
            boolean isTransient) {

        String category = entity.getCategory();

        if ((category != null) && (
            category.equals("Category.Window") ||
            category.equals("Category.Door"))) {

            Float embedDepth =
                (Float)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.EMBED_DEPTH_PROP);

            if (embedDepth != null) {
                if (isTransient) {
                    position[2] = embedDepth - EMBED_DEPTH;
                } else {
                    position[2] = -embedDepth;
                }
            }
        }
    }
    
    /**
     * Certain types of models are embedded into the cut-outs
     * in the segment, rather than being placed on the surface
     * This should override the depth amount set as we assume
     * doors and windows are being set into walls and can then
     * easily determine what the actual depth value should be.
     *
     * @param entity Entity to examine
     * @param position position of entity to adjust depth for
     * @param isTransient Is the entity in a transient state
     */
    protected void handleAutoSpanCase(
            Entity entity,
            double[] position,
            boolean isTransient) {

        Boolean autoSpan = (Boolean)
        RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        if(autoSpan != null && autoSpan){
            Float zOffset = (Float)
            RulePropertyAccessor.getRulePropertyValue(
                entity,
                ChefX3DRuleProperties.SPAN_OBJECT_DEPTH_OFFSET_PROP);

            position[2] = zOffset;

        }
    }


    /**
     * Apply genPos (general positioning) to all entities.
     *
     * @param model WorldModel to reference
     * @param command Command being evaluated by rules
     */
    protected void generalGenPosCalc(
            WorldModel model,
            Command command) {

        Entity parentEntity = null;
        double[] position = new double[3];

        // Get and prep the position data for the set relative position calc
        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand) command).getEndPosition(position);

        } else if (command instanceof MoveEntityTransientCommand) {

            MoveEntityTransientCommand mvCmd =
                (MoveEntityTransientCommand) command;

            mvCmd.getPosition(position);

            parentEntity = mvCmd.getPickParentEntity();

            // Adjust the position to remove the position relative to the 
        	// zone of the pick parent if the current parent is the 
        	// active zone.
            Entity activeZone = 
            	SceneHierarchyUtility.getActiveZoneEntity(model);
            Entity exactParent = 
            	SceneHierarchyUtility.getExactParent(model, mvCmd.getEntity());
            
            if (parentEntity instanceof PositionableEntity &&
                    parentEntity.isModel() &&
                    activeZone == exactParent) {

                double[] pickParentZonePos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, parentEntity);

                if (pickParentZonePos != null) {
                    position[0] -= pickParentZonePos[0];
                    position[1] -= pickParentZonePos[1];
                    position[2] -= pickParentZonePos[2];
                }
            }

        } else if (command instanceof TransitionEntityChildCommand) {

            ((TransitionEntityChildCommand)command).getEndPosition(position);

        } else if (command instanceof AddEntityChildCommand) {

            Entity addEntity = ((AddEntityChildCommand)command).getEntity();

            if (addEntity instanceof PositionableEntity) {
                ((PositionableEntity)addEntity).getPosition(position);
            } else {
                return;
            }

        } else if (command instanceof AddEntityChildTransientCommand) {

            Entity addEntity =
                ((AddEntityChildTransientCommand)command).getEntity();

            if (addEntity instanceof PositionableEntity) {
                ((PositionableEntity)addEntity).getPosition(position);
            } else {
                return;
            }

        } else {
            return;
        }

        // Calculate set relative position
        SetRelativePositionUtility.setRelativePosition(
        		command, 
        		model, 
        		position, 
        		(PositionableEntity) parentEntity, 
        		rch);

        // Update the set relative position change in the respective
        // command.
        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand) command).setEndPosition(position);

        } else if (command instanceof MoveEntityTransientCommand) {

            MoveEntityTransientCommand mvCmd =
                (MoveEntityTransientCommand) command;

            Entity pickParent = mvCmd.getPickParentEntity();
            
            // Adjust the position to remove the position relative to the 
        	// zone of the pick parent if the current parent is the 
        	// active zone.
            Entity activeZone = 
            	SceneHierarchyUtility.getActiveZoneEntity(model);
            Entity exactParent = 
            	SceneHierarchyUtility.getExactParent(model, mvCmd.getEntity());

            if (pickParent instanceof PositionableEntity &&
                    pickParent.isModel() &&
                    activeZone == exactParent) {

                double[] pickParentZonePos =
                    TransformUtils.getPositionRelativeToZone(
                    		model, pickParent);

                if (pickParentZonePos != null) {
                    position[0] += pickParentZonePos[0];
                    position[1] += pickParentZonePos[1];
                    position[2] += pickParentZonePos[2];
                }
            }

            mvCmd.setPosition(position);

        } else if (command instanceof TransitionEntityChildCommand) {

            ((TransitionEntityChildCommand)command).setEndPosition(position);

        } else if (command instanceof AddEntityChildCommand) {

            Entity addEntity = ((AddEntityChildCommand)command).getEntity();

            if (addEntity instanceof PositionableEntity) {
                ((PositionableEntity)addEntity).setPosition(position, false);
            } else {
                return;
            }

        } else if (command instanceof AddEntityChildTransientCommand) {

            Entity addEntity =
                ((AddEntityChildTransientCommand)command).getEntity();

            if (addEntity instanceof PositionableEntity) {
                ((PositionableEntity)addEntity).setPosition(position, false);
            } else {
                return;
            }
        }
    }

    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

}
