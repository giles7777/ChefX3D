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

import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.TransitionEntityChildCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.TransformUtils;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
 * Determines if the entity could be parented to multiple items.  Instead of
 * just picking one of those items it instead looks for the shared ancestor
 * and uses that instead.  Position information is updated.
 *
 * If no shared ancestor is found then it re-parents to the parent of the
 * original parent in hopes that this is at least the parent of several of
 * the items.
 *
 * If the item can only be parented to a single item the rule is skipped.
 *
 * @author Russell Dodds
 * @version $Revision: 1.24 $
 */
public class MultipleParentCorrectionRule extends BaseRule {

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MultipleParentCorrectionRule(
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

		// Don't process non transient commands
		if (command.isTransient()) {
            result.setResult(true);
            return(result);
		}

	    // get the list of possible parents
	    ArrayList<Entity> parentList = 
	        SceneHierarchyUtility.findPossibleParents(command, model, rch);

	    // only continue if necessary
	    if (parentList != null && parentList.size() > 1) {

	        // working variables
            double[] position = new double[3];

	        // get the new parent
	        Entity newParentEntity = 
	            SceneHierarchyUtility.getSharedParent(
	            		model, entity, parentList);
	        
	        // If the newParentEntity is null, don't change anything.
	        if (newParentEntity == null) {
	        	result.setResult(true);
	            return(result);
	        }
	        
	        // Get the current frame position of the entity in scene coords
	        position = 
	        	TransformUtils.getPositionInSceneCoordinates(
	        			model, 
	        			(PositionableEntity)entity, 
	        			true);
	        
	        // Convert the position to local coordinates relative to 
            // the new parent.
            position = 
            	TransformUtils.convertSceneCoordinatesToLocalCoordinates(
            			model, 
            			position, 
            			(PositionableEntity) newParentEntity, 
            			true);

	        // Apply results to appropriate command
	        if (command instanceof AddEntityChildCommand) {

	            // set the new parent
	            ((AddEntityChildCommand)command).setParentEntity(
	            		newParentEntity);

                // adjust the position
	            ((PositionableEntity)entity).setPosition(position, false);

	        } else if (command instanceof TransitionEntityChildCommand) {

                // get the original parent
                Entity origParentEntity =
                    ((TransitionEntityChildCommand)
                    		command).getEndParentEntity();

                // if this is being transfered to the zone then ignore it.
                if (origParentEntity instanceof ZoneEntity) {
                    result.setResult(true);
                    return(result);
                }

                // set the new parent
                ((TransitionEntityChildCommand)command).setEndParentEntity(
                		newParentEntity);

                // adjust the position
                ((TransitionEntityChildCommand)command).setEndPosition(
                		position);

	        }

	    }

        result.setResult(true);
        return(result);
	}

}
