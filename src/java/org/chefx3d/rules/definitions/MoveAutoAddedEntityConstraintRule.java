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

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.*;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation.ELEVATION_LEVEL;
import org.chefx3d.view.common.EditorView;

/**
 * Any movement of an auto add product must consider the safe installation
 * requirements imposed by the use of the auto add algorithm. If the span
 * between objects is exceeded, we need to add a another entity 1/2 the
 * distance back in the opposite direction of travel. This operation will
 * be done on the non-transient move case.
 *
 * Any auto add product with product attached to it will not be allowed to
 * move, unless that product is also an auto add product. Handling of
 * ancillary auto add for attached product will occur when needed. An extra
 * instance will be added if moved beyond span requirements of auto add
 * product attached.
 *
 * @author Ben Yarger
 * @version $Revision: 1.63 $
 */
public class MoveAutoAddedEntityConstraintRule extends BaseRule {

	/** Illegal installation message */
	private static final String POP_UP_MSG_PROP =
		"org.chefx3d.rules.definitions.MoveAutoAddEntityConstraintRule.illegalInstall";

	/** Position installation requirements message */
	private static final String STATUS_BAR_POS_REQ =
		"org.chefx3d.rules.definitions.MoveAutoAddEntityConstraintRule.installPosReq";

    /**
     * Constructor
     *
     * @param errorReporter
     * @param model
     * @param view
     */
    public MoveAutoAddedEntityConstraintRule(
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

		Boolean isAutoAddProduct = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

		// Don't both with non auto add products.
		// This rule only impacts products that are added
		// as auto add products.
		if(isAutoAddProduct == null || isAutoAddProduct == false){
            result.setResult(true);
            return(result);
		}

		// Don't bother with non-positionable entities.
		if(!(entity instanceof PositionableEntity)){
            result.setResult(true);
            return(result);
		}

		// Prevent any entities from moving that are
		// supporting installation position requirement
		// entities.
		if (blockInstalltionRequiredEntityMovement(
				model,
				command,
				entity)) {

		    result.setStatusValue(ELEVATION_LEVEL.SEVERE);

	    	result.setNotApprovedAction(
	    			NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
	        result.setApproved(false);

            result.setResult(false);
            return(result);

		}

		// Don't go any further for transient commands
		if (command.isTransient()) {

			if (command instanceof TransitionEntityChildCommand) {

				TransitionEntityChildCommand tecc =
					(TransitionEntityChildCommand) command;

				double[] startPosition = new double[3];
				tecc.getStartPosition(startPosition);

				float[] startRot = new float[4];
				tecc.getStartingRotation(startRot);

				TransitionEntityChildCommand testCmd =
					new TransitionEntityChildCommand(
							model,
							(PositionableEntity) entity,
							tecc.getStartParentEntity(),
							startPosition,
							startRot,
							tecc.getStartParentEntity(),
							startPosition,
							startRot,
							true);

				// Perform the collision check
				rch.performCollisionCheck(testCmd, true, false, false);

				if (rch.collisionEntities != null) {

	                // Remove any auto adds
	                for(int i = 0; i < rch.collisionEntities.size(); i++){

	                    Entity tmpEntity = rch.collisionEntities.get(i);

	                    // Only evaluate if of type model
	                    if (!tmpEntity.isModel()) {
	                        continue;
	                    }

	                    Boolean tmpIsAutoAddProduct = (Boolean)
	                        RulePropertyAccessor.getRulePropertyValue(
	                            tmpEntity,
	                            ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

	                    if(tmpIsAutoAddProduct != null &&
	                            tmpIsAutoAddProduct == true){

	                        Entity parent =
	                            model.getEntity(tmpEntity.getParentEntityID());

	                        if (parent == null) {
	                            continue;
	                        }
	                        
	                        // Side pocket any children that are getting removed
	                        parent.addStartingChild(tmpEntity);
	                        
	                        SceneManagementUtility.removeChild(
	                        		model, collisionChecker, tmpEntity, true);
	                        
	                    }
	                }
				}
			}

            result.setResult(true);
            return(result);
		}

		// Need to alter the end values to be the start position for the
		// purpose of evaluating the starting collision conditions.
		// What we are trying to do is evaluate the collision conditions
		// from the starting position of the moving entity. These collision
		// conditions are tested to see if they use installation position
		// requirements and if so, we check to see if the moving entity
		// is responsible for satisfying one of those requirements.
		double[] cmdStartPos = new double[3];
		double[] cmdEndPos = new double[3];
		Entity transitionEndParent = null;
		Entity transitionStartParent = null;
		int parentEntityID = entity.getParentEntityID();

		if(command instanceof MoveEntityCommand){

			((MoveEntityCommand)command).getEndPosition(cmdEndPos);
			((MoveEntityCommand)command).getStartPosition(cmdStartPos);

			((MoveEntityCommand)command).setEndPosition(cmdStartPos);

		} else if (command instanceof MoveEntityTransientCommand){

			((MoveEntityTransientCommand)command).getPosition(cmdEndPos);
			((PositionableEntity)entity).getStartingPosition(cmdStartPos);

			((MoveEntityTransientCommand)command).setPosition(cmdStartPos);

		} else if (command instanceof TransitionEntityChildCommand){

			((TransitionEntityChildCommand)command).getEndPosition(
					cmdEndPos);
			((TransitionEntityChildCommand)command).getStartPosition(
					cmdStartPos);

			transitionEndParent =
				((TransitionEntityChildCommand)command).getEndParentEntity();
			parentEntityID = transitionEndParent.getEntityID();
			transitionStartParent =
				((TransitionEntityChildCommand)command).getStartParentEntity();

			((TransitionEntityChildCommand)command).setEndPosition(
					cmdStartPos);
			((TransitionEntityChildCommand)command).setEndParentEntity(
					transitionStartParent);
		}

		// Don't allow movement if collision with non auto add products exist.
		// If non auto add product exists, reset to start and exit rule check.
		// We will capture the auto add products we need to move here.
		ArrayList<Entity> autoAddCollisionsToMove = new ArrayList<Entity>();

		// Perform the collision check
		rch.performCollisionCheck(command, true, false, false);

		// Reset the end positions back for the commands
		if(command instanceof MoveEntityCommand){
			((MoveEntityCommand)command).setEndPosition(cmdEndPos);
		} else if (command instanceof MoveEntityTransientCommand){
			((MoveEntityTransientCommand)command).setPosition(cmdEndPos);
		} else if (command instanceof TransitionEntityChildCommand){
			((TransitionEntityChildCommand)command).setEndPosition(cmdEndPos);
			((TransitionEntityChildCommand)command).setEndParentEntity(
					transitionEndParent);
		}

		// The dependent entity set, otherwise known as the list of entities
		// that are in collision with the moving entity at its start position.
		ArrayList<Entity> dependentCheckSet =
			new ArrayList<Entity>(rch.collisionEntities);

		int len = dependentCheckSet.size();
		for (int i = len - 1; i >= 0; i--) {

		    Entity check = dependentCheckSet.get(i);
			if (check.getEntityID() == parentEntityID) {
				dependentCheckSet.remove(i);
			} else if (!check.isModel()) {
			    dependentCheckSet.remove(i);
			}
		}

		// Generate ids to ignore list. These are the entity ids of
		// things like parents, etc that we don't want to consider.
		ArrayList<Integer> idsToIgnoreList = new ArrayList<Integer>();
		idsToIgnoreList.add(entity.getEntityID());

		if (transitionEndParent != null) {
			idsToIgnoreList.add(transitionEndParent.getEntityID());
		}

		if (transitionStartParent != null) {
			idsToIgnoreList.add(transitionStartParent.getEntityID());
		}

		int[] idsToIgnoreArray = new int[idsToIgnoreList.size()];

		for (int i = 0; i < idsToIgnoreList.size(); i++) {

			idsToIgnoreArray[i] = idsToIgnoreList.get(i);
		}

		boolean attached =
			rch.hasDependantProductAttached(
					model,
					dependentCheckSet,
					idsToIgnoreArray,
					false,
					rch,
					view);

		// If there are dependent products pull out any auto add products.
		// If any dependents are not auto add then do not allow movement.
		if (attached) {

			for(int i = 0; i < dependentCheckSet.size(); i++){

				Entity tmpEntity = dependentCheckSet.get(i);

				// Only dependent if of type model
				if (!tmpEntity.isModel()) {
					continue;
				}

				// Opting to check for auto add by collision here instead of checking
				// for can scale since this is more specific than can scale and serves
				// the purpose of identifying shelves.
				Boolean tmpIsAutoAddProduct = (Boolean)
					RulePropertyAccessor.getRulePropertyValue(
						tmpEntity,
						ChefX3DRuleProperties.IS_AUTO_ADD_PRODUCT);

				if(tmpIsAutoAddProduct == null || tmpIsAutoAddProduct == false){

					// Final check, if this entity places auto add products
					// by collision, then allow. Otherwise, protest and put up
					// the message box.
					Boolean autoAddByCollision = (Boolean)
						RulePropertyAccessor.getRulePropertyValue(
								tmpEntity,
								ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);

					if (!autoAddByCollision) {

						//-----------------------------------------------------
						//-----------------------------------------------------
						// NEED TO KILL THE COMMAND INSTEAD OF RESET IT SO THAT
						// ALL NEWLY ISSUED COMMANDS DIE!
						//-----------------------------------------------------
						//-----------------------------------------------------
						//((RuleDataAccessor)command).resetToStart();

						String msg = intl_mgr.getString(POP_UP_MSG_PROP);
						popUpMessage.showMessage(msg);

			            result.setStatusValue(ELEVATION_LEVEL.SEVERE);

			            if (!command.isTransient()) {
			                result.setApproved(false);
			            }
			            result.setResult(false);
			            return(result);
					}

				} else {

					autoAddCollisionsToMove.add(tmpEntity);
				}
			}
		}

		//---------------------------------------------------------------------
		//---------------------------------------------------------------------
		// Expected current state of affairs:
		// Transient and non transient auto add product collision case has
		// been handled. In addition, any auto add products that need to be
		// moved with the auto place product being moved have been stored.
		//---------------------------------------------------------------------
		//---------------------------------------------------------------------

		// First thing we need to do is determine the direction of movement
		// from the command based on the auto add axis. This is extracted
		// from the parent. So we start by finding the auto adding parent.
		Entity parentAutoAddSource = null;

		if(command instanceof MoveEntityCommand){

			parentEntityID = entity.getParentEntityID();
			parentAutoAddSource = model.getEntity(parentEntityID);

		} else if (command instanceof TransitionEntityChildCommand){

			parentAutoAddSource =
				((TransitionEntityChildCommand)command).getStartParentEntity();
			
		}

		// If we couldn't find the auto adding parent, bail out.
		if(parentAutoAddSource == null){

			//-----------------------------------------------------------------
			//-----------------------------------------------------------------
			// NEED TO KILL THE COMMAND INSTEAD OF RESET IT SO THAT ALL NEWLY
			// ISSUED COMMANDS DIE.
			//-----------------------------------------------------------------
			//-----------------------------------------------------------------
			//((RuleDataAccessor)command).resetToStart();

			String msg = intl_mgr.getString(POP_UP_MSG_PROP);
			popUpMessage.showMessage(msg);

            result.setStatusValue(ELEVATION_LEVEL.SEVERE);

            if (!command.isTransient()) {
                result.setApproved(false);
            }
            result.setResult(false);
            return(result);
		}
		
		//----------------------------------------------------------------------
		// Recalculate auto add by span
		//----------------------------------------------------------------------
		
		// Add the surrogate to evaluate against
        SceneManagementUtility.addTempSurrogate(
           		collisionChecker, command);
        
        // Temporarily adjust the position of the moving entity to where it
        // will be to correctly evaluate auto add conditions.
        double[] originalPos = new double[3];
        ((PositionableEntity)entity).getPosition(originalPos);
        
        double[] tmpPos = 
        	TransformUtils.getExactPosition(
        			(PositionableEntity)entity);
        
        ((PositionableEntity)entity).setPosition(tmpPos, false);
        
        // Get the needed missing pieces
        EntityBuilder entityBuilder = view.getEntityBuilder();
        Entity parentEntityParentEntity = 
        	SceneHierarchyUtility.getExactParent(
        			model, parentAutoAddSource);

        if (!AutoAddBySpanUtility.autoAddBySpan(
        		model, 
        		(PositionableEntity) parentAutoAddSource, 
        		parentEntityParentEntity, 
        		rch,
        		entityBuilder)) {
        	
        	((PositionableEntity)entity).setPosition(originalPos, false);
        	
        	SceneManagementUtility.removeTempSurrogate(
               		collisionChecker, (PositionableEntity) entity);
        	
        	String msg = intl_mgr.getString(POP_UP_MSG_PROP);
            popUpMessage.showMessage(msg);
        	
        	result.setResult(false);
        	result.setApproved(false);
        	return(result);
        }
        
        ((PositionableEntity)entity).setPosition(originalPos, false);
        
        SceneManagementUtility.removeTempSurrogate(
           		collisionChecker, (PositionableEntity) entity);

        result.setResult(true);
        return(result);
	}

	//---------------------------------------------------------------
	// Private methods
	//---------------------------------------------------------------

	/**
	 * Check to see if the entity should be prevented from moving because
	 * it is supporting another entity that uses installation position
	 * requirements.
	 *
	 * @param model WorldModel to reference
	 * @param command Command to examine
	 * @param entity Entity to examine
	 */
	private boolean blockInstalltionRequiredEntityMovement(
			WorldModel model,
			Command command,
			Entity entity) {

		// Don't allow movement for anything that is satisfying a position
		// collision requirement for another entity.
		boolean resultA = InstallPositionRequirementUtility.
			hasPositionCollisionRequirementImposed(
					model, 
					command, 
					entity,
					rch, 
					view);
		
		boolean resultB = InstallPositionMultiZoneRequirementUtility.
			hasPositionCollisionRequirementsImposed(
				model, 
				command, 
				entity, 
				rch, 
				view);
		
		if (resultA || resultB) {

			double[] entityStartingPos = new double[3];
			((PositionableEntity)entity).getStartingPosition(
					entityStartingPos);

			if (command instanceof MoveEntityCommand) {

				((MoveEntityCommand)command).setEndPosition(entityStartingPos);

			} else if (command instanceof MoveEntityTransientCommand) {

				int parentEntityID = entity.getParentEntityID();
				Entity parentEntity = model.getEntity(parentEntityID);

				double[] startPosition = new double[3];
					((PositionableEntity)entity).getStartingPosition(
							startPosition);

				double[] parentPos =
					TransformUtils.getPositionRelativeToZone(model, parentEntity);

				startPosition[0] += parentPos[0];
				startPosition[1] += parentPos[1];
				startPosition[2] += parentPos[2];

				((MoveEntityTransientCommand)command).setPosition(
						entityStartingPos);

			} else if (command instanceof TransitionEntityChildCommand) {

				double[] startPos = new double[3];

				Entity startParent =
					((TransitionEntityChildCommand)
							command).getStartParentEntity();
				((TransitionEntityChildCommand)command).getStartPosition(
						startPos);

				((TransitionEntityChildCommand)command).setEndParentEntity(
						startParent);
				((TransitionEntityChildCommand)command).setEndPosition(
						startPos);
			}

			String msg = intl_mgr.getString(STATUS_BAR_POS_REQ);
			statusBar.setMessage(msg);
			return true;
		}

		return false;
	}
}
