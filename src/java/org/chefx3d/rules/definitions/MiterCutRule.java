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
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.*;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
 * Perform mitre cut adjustments. Either to auto generate the mitre line or 
 * use the mire line of the parent to create the correct mitre cut effect.
 *
 * @author Russell Dodds
 * @version $Revision: 1.29 $
 */
public class MiterCutRule extends BaseRule {

    /** The miter object cannot be added */
    private static final String CANNOT_ADD_MSG =
        "org.chefx3d.rules.definitions.MiterCutRule.addFailed";
    
    /** The miter object cannot be moved */
    private static final String CANNOT_MOVE_MSG =
        "org.chefx3d.rules.definitions.MiterCutRule.moveFailed";
    
    /** Confirm miter cut message */
    private static final String CONFIRM_MITER = 
    	"org.chefx3d.rules.definitions.MiterCutRule.confirmMiter";
    
    /** Ready to use miter cut message for confirm dialog */
    private static String CONFIRM_MSG;
    
    /** Epsilon value */
    private static final float EPSILON = 0.0001f;

    /** 
     * The distance threshold used to assume two spline vertices are 
     * overlapping and thus should be treated as the same point 
     */
    public static final float DISTANCE_THRESHOLD = 0.01f;
    
    /** 
     * Internal use property to denote which side of the auto miter product
     * should be adjusted. If an entity has this set it will not nudge any
     * other entities. Legal values are those from MITER_SIDE_TO_PROCESS.
     */
    private static final String MITER_SIDE_PROP = "MiterSideProp";
    
    /** Denotes which side of a miter entity to process during a nudge. */
    private static enum MITER_SIDE_TO_PROCESS {POSITIVE, NEGATIVE};

	/**
	 * Constructor
	 *
	 * @param errorReporter Error reporter to use
	 * @param model Collision checker to use
	 * @param view AV3D view to reference
	 */
	public MiterCutRule (
			ErrorReporter errorReporter,
			WorldModel model,
			EditorView view) {

		super(errorReporter, model, view);

		ruleType = RULE_TYPE.STANDARD;
		
		CONFIRM_MSG = intl_mgr.getString(CONFIRM_MITER);

	}
	
	//--------------------------------------------------------------------------
	// Exposed general reset method
	//--------------------------------------------------------------------------
	
	/**
     * Reset the ExtrusionEntity miter line to its last known good state.
     * 
     * @param model WorldModel to reference
     * @param entity ExtrusionEntity to set back
     * @param command Command acting on entity
     * @param rcc RuleCollisionChecker to reference
     */
    public static void handleMiterFailure(
    		WorldModel model,
    		PositionableEntity entity,
    		Command command) {
    	
    	// Only operate on non transient commands
    	if (command.isTransient()) {
    		return;
    	}
    	
        // check the entity is a miter entity
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (!canMiterCut) {
        	return;
        }
        
        // First things first, if it is a ScaleEntityCommand, reset the start
        // scale to 1.0, 1.0, 1.0.
        // If it is a MoveEntityCommand or TransitionEntityChildCommand, issue
        // a ScaleEntityCommand to reset the scale to 1.0, 1.0, 1.0.
        // Next, issue a change property command to correct the starting scale
        // since ScaleEntityCommands set the starting scale and we want to
        // have that go back to the last known good starting scale, not 
        // 1.0, 1.0, 1.0.
        double[] position = TransformUtils.getStartPosition(entity);
        
        if (command instanceof ScaleEntityCommand) {
        	
        	((ScaleEntityCommand)command).setOldScale(
        			new float[] {1.0f, 1.0f, 1.0f});
        	
        	((ScaleEntityCommand)command).setOldPosition(position);
        	
        } else if (command instanceof MoveEntityCommand || 
        		command instanceof TransitionEntityChildCommand) {
        	
        	int scaleChangeTID = SceneManagementUtility.changeProperty(
        			model, 
        			entity, 
        			Entity.DEFAULT_ENTITY_PROPERTIES, 
        			PositionableEntity.SCALE_PROP, 
        			new float[] {1.0f, 1.0f, 1.0f}, 
        			new float[] {1.0f, 1.0f, 1.0f}, 
        			false);
        			
        	Command cmd = 
        		CommandSequencer.getInstance().getCommand(scaleChangeTID);
        	
        	CommandSequencer.getInstance().addApprovedCommand(cmd);
        	CommandSequencer.getInstance().removeNewlyIssuedCommand(
        			scaleChangeTID);
        }
        
        float[] startingScale = new float[3];
		entity.getStartingScale(startingScale);

		int sspTID = SceneManagementUtility.changeProperty(
				model, 
				entity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				PositionableEntity.START_SCALE_PROP, 
				startingScale, 
				startingScale, 
				false);
		
		Command cmdSSP = 
    		CommandSequencer.getInstance().getCommand(sspTID);
    	
    	CommandSequencer.getInstance().addApprovedCommand(cmdSSP);
    	CommandSequencer.getInstance().removeNewlyIssuedCommand(sspTID);

        // Get the last known good spine and issue a change property command
        // to set it back.
        float[] oldSpineValue = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
		
		float[] newSpineValue = new float[oldSpineValue.length];
		for (int i = 0; i < oldSpineValue.length; i++) {
			newSpineValue[i] = oldSpineValue[i];
		}
    	
		int svpTID = SceneManagementUtility.changeProperty(
				model, 
				entity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				ExtrusionEntity.SPINE_VERTICES_PROP, 
				newSpineValue, 
				newSpineValue, 
				false);
		
		Command cmdSVP = 
    		CommandSequencer.getInstance().getCommand(svpTID);
    	
    	CommandSequencer.getInstance().addApprovedCommand(cmdSVP);
    	CommandSequencer.getInstance().removeNewlyIssuedCommand(svpTID);
		
		// Get the last known good miter cut visibility and set it back.
		boolean[] oldVisibleValue = (boolean[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD);
		
		boolean[] newVisibleValue = new boolean[oldVisibleValue.length];
		for (int i = 0; i < oldVisibleValue.length; i++) {
			newVisibleValue[i] = oldVisibleValue[i];
		}

		int vpTID = SceneManagementUtility.changeProperty(
				model, 
				entity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				ExtrusionEntity.VISIBLE_PROP, 
				newVisibleValue, 
				newVisibleValue, 
				false);
		
		Command cmdVP = 
    		CommandSequencer.getInstance().getCommand(vpTID);
    	
    	CommandSequencer.getInstance().addApprovedCommand(cmdVP);
    	CommandSequencer.getInstance().removeNewlyIssuedCommand(vpTID);
		
		// Get the last known enable miter values and set it back.
		boolean[] oldEnableValue = (boolean[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ChefX3DRuleProperties.MITRE_CUT_ENABLE_LAST_GOOD);
		
		boolean[] newEnableValue = new boolean[oldEnableValue.length];
		for (int i = 0; i < oldEnableValue.length; i++) {
			newEnableValue[i] = oldEnableValue[i];
		}
		
		int mepTID = SceneManagementUtility.changeProperty(
				model, 
				entity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				ExtrusionEntity.MITER_ENABLE_PROP, 
				newEnableValue, 
				newEnableValue, 
				false);
		
		Command cmdMEP = 
    		CommandSequencer.getInstance().getCommand(mepTID);
    	
    	CommandSequencer.getInstance().addApprovedCommand(cmdMEP);
    	CommandSequencer.getInstance().removeNewlyIssuedCommand(mepTID);
    }

    //-----------------------------------------------------------
    // BaseRule Methods required to implement
    //-----------------------------------------------------------

	@Override
	protected RuleEvaluationResult performCheck(
			Entity entity,
			Command command,
			RuleEvaluationResult result) {

		this.result = result;

		// check to make sure this is a command we should be processing
		boolean processCmd = validateCommand(entity, command);
		
		if (processCmd) {	    

			// Handle the Add, Remove/Move/Transition and scale cases.
			// The actual processing branches in each section to handle either
			// Extrusion entities that follow mitre lines (like crown
			// molding) or Extrusion entities that create their own mitre
			// lines and mitre cut their own ends (like top shelves).
            if (command instanceof AddEntityChildCommand) {
                
                addEntity(entity, (AddEntityChildCommand)command, result);
                
            } else if (command instanceof RemoveEntityChildCommand || 
                    command instanceof MoveEntityCommand || 
                    command instanceof MoveEntityTransientCommand ||
                    command instanceof TransitionEntityChildCommand) {
                
            	moveOrDeleteEntity(entity, command, result);
                
            } else if (command instanceof ScaleEntityTransientCommand ||
            		command instanceof ScaleEntityCommand) {
            	
            	scaleEntity((PositionableEntity)entity, command, result);
            	
            	AutoAddMiterWrapper.processMiterAutoAdd(
                		model, 
                		(PositionableEntity) entity, 
                		command, 
                		rch, 
                		view);
            	
            	// We want to kill the scale command since scaling an extrude
            	// is a very bad idea. Extrudes just change the spine, they
            	// never actually scale.
            	result.setApproved(false);
            	result.setNotApprovedAction(
            			NOT_APPROVED_ACTION.CLEAR_CURRENT_COMMAND_NO_RESET);
            	result.setResult(false);
            	return result;
            }

            AutoAddMiterWrapper.processMiterAutoAdd(
            		model, 
            		(PositionableEntity) entity, 
            		command, 
            		rch, 
            		view);
		}

		result.setResult(true);
		return result;

	}

	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------

    /**
     * Handle remove and move commands for Extrusion entities that follow
     * mitre lines and Extrusion entities that generate mite lines.
     * 
     * If the entity moving entity can mitre cut, then adjust it for transient
     * and non transient movement. On the non-
     * 
     * @param entity The entity being removed
     * @param command The remove or move command 
     * @param result The result object
     */
	private void moveOrDeleteEntity(
	        Entity entity,
	        Command command,
	        RuleEvaluationResult result) {
		
		// Check for the case where we use a miter line (crown molding)
		Boolean useMiterLine = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_USE_LINE);

        // Check for the case where we have a miter line (crown molding parent)
        Boolean hasMiterLine = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_HAS_LINE);
        
        // Check for the case where we generate the miter line (top shelf)
        Boolean autoMitreLine = (Boolean)
    		RulePropertyAccessor.getRulePropertyValue(
    			entity, 
    			ChefX3DRuleProperties.MITRE_AUTO);
        
        //-----------------------
        // Handle the remove case
        //-----------------------
        if (command instanceof RemoveEntityChildCommand) {
        	
        	processRemoveCommand((PositionableEntity)entity);
        	return;
        }
        
        //-----------------------
		// HANDLE THE MOVE CASES:
        //-----------------------
        
		// If the command is transient, revert back to the starting shape.
		// The starting shape is just the front spine visible and set to the
		// width of the x size value. For the autoMitreLine case, no resizing
        // is done, we just square off the ends.
        //----------------------------------------------------------------------
		// Because an extrusion is made up of a size that doesn't depict the 
        // starting size of the 3D extrusion, getting size and scale will not
        // yield a useful result. Only bounds can do this because it is a 
        // different calculation than size*scale for extrusions. Therefore
        // the only baseline that we can validly move in is the default one
        // we start out with which is just the primary small front edge of the
        // spine. Therefore we just revert to that when moving in a transient
        // state.
		//----------------------------------------------------------------------
		// We assume that all transient commands entering this method are 
		// MoveEntityTransientCommands.
        //----------------------------------------------------------------------
        
        
        // Branch out between auto miter line extrusions and extrusions that 
        // use miter lines.
        if (autoMitreLine) {
        	
        	// This check is to handle transient commands moving an 
    		// extrusion entity that follows a mitre line and auto generates
    		// that mitre line. For this case, all we want to do is square 
    		// off the ends and make sure the two outside lengths aren't
    		// visible. Example would be top shelf.

    		float[] spine = (float[])
				RulePropertyAccessor.getRulePropertyValue(
					entity, 
					ExtrusionEntity.SPINE_VERTICES_PROP);
	
			spine[0] = spine[3] - 0.25f;
			spine[1] = spine[4];
			spine[2] = spine[5];
			
			spine[9] = spine[6] + 0.25f;
			spine[10] = spine[7];
			spine[11] = spine[8];
			
			// set the spline coords
	        RulePropertyAccessor.setProperty(
	                entity, 
	                Entity.DEFAULT_ENTITY_PROPERTIES, 
	                ExtrusionEntity.SPINE_VERTICES_PROP, 
	                spine);

			generateAutoMiterLines((PositionableEntity)entity, command);
	        	
        	
        } else if (useMiterLine) {
        	
        	if (command.isTransient()) {
				// This check is to handle transient commands moving an 
				// extrusion entity that follows a mitre cut but doesn't auto 
				// generate the mitre line. Example would be crown molding.
				
				// Issue a change property command for the spine shape
				float[] size = new float[3];
				((PositionableEntity)entity).getSize(size);
				
				float width = size[0];
				float[] spine = new float[] {
	                    (-width/2.0f - 0.25f), 0f, 0f, 
	                    (-width/2.0f), 0f, 0f, 
	                    (width/2.0f), 0f, 0f, 
	                    (width/2.0f + 0.25f), 0f, 0f};
				
				Object originalSpineValue = 
					RulePropertyAccessor.getRulePropertyValue(
							entity, 
							ExtrusionEntity.SPINE_VERTICES_PROP);
				
				SceneManagementUtility.changeProperty(
						model, 
						entity, 
						Entity.DEFAULT_ENTITY_PROPERTIES, 
						ExtrusionEntity.SPINE_VERTICES_PROP, 
						originalSpineValue, 
						spine, 
						false);
	 
	            // Issue a change property command for the visibile prop
				boolean[] visible = new boolean[] {false, true, false};
				
				Object originalVisibleValue = 
					RulePropertyAccessor.getRulePropertyValue(
							entity, 
							ExtrusionEntity.VISIBLE_PROP);
				
				SceneManagementUtility.changeProperty(
						model, 
						entity, 
						Entity.DEFAULT_ENTITY_PROPERTIES, 
						ExtrusionEntity.VISIBLE_PROP, 
						originalVisibleValue, 
						visible, 
						false);
				
				return;
			
			} else {
				
				Entity parent = 
					SceneHierarchyUtility.getExactParent(model, entity);
				// look to the neighbors to adjust the sides of the miter 
				// object.
		        generateMiterLinesFromParent(
		                (PositionableEntity)entity, 
		                (PositionableEntity)parent, 
		                command);
		        
			}
        	
        } else if (hasMiterLine && !command.isTransient()) {
			
			// This section is to handle non-transient MoveEntityCommand and
			// TransitionEntityChildCommand moving an extrusion entity that 
			// follows a mitre cut but doesn't auto generate the mitre line. 
			// Example would be crown molding.

            // get the collision entity miter product if it exists
            Entity miterEntity = getMiterChild(entity, false);
            
            if (miterEntity == null) {
            	return;
            }

	        // look to the neighbors to adjust the sides of the miter object.
	        generateMiterLinesFromParent(
	                (PositionableEntity)miterEntity, 
	                (PositionableEntity)entity, 
	                command);
        }
     
	}
	
    /**
     * Handle add commands
     * 
     * @param entity The entity being added
     * @param command The add command 
     * @param result The result object
     */
	private void addEntity(
	        Entity entity,
	        AddEntityChildCommand command,
            RuleEvaluationResult result) {
	    	    
        //  check the entity is a miter entity
        Boolean followsMiterLine = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_USE_LINE);
        
        // Look for auto miter line flag
        Boolean autoMitreLine = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_AUTO);

        // does the item being placed need to follow a miter line or does it
        // generate it's own miter path?
        if (followsMiterLine && !autoMitreLine) {
            
            Entity parent = command.getParentEntity();
            
            // get the collision entity miter product if it exists
            Entity miterEntity = getMiterChild(parent, false);
            
            // if there is no miter child then there is no adding allowed
            if (miterEntity != null) {
                
                // pop up a failed message
                String failedMsg = intl_mgr.getString(CANNOT_ADD_MSG);
                popUpMessage.showMessage(failedMsg);
                statusBar.setMessage(failedMsg);
                
                // set the failed result
                result.setResult(false);
                result.setApproved(false);
                return;
            }

            // first check the parent is a miter path entity
            Boolean hasMiterLine = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        parent,
                        ChefX3DRuleProperties.MITRE_HAS_LINE);

            if (hasMiterLine) {
                
                // look to the neighbors to adjust the sides of the miter object
                generateMiterLinesFromParent(
                        (PositionableEntity)entity, 
                        (PositionableEntity)parent, 
                        command);

            }
            
        } else if (autoMitreLine) {
        	
        	generateAutoMiterLines((PositionableEntity)entity, command);
        	
		}

	}
	
	/**
	 * Handle scale commands. Presumes that scale is only going to occur on 
	 * extrusion entities that have auto mitre line set. Validation method
	 * checks for this, so we don't have to here. Also assumes that the calling
	 * method will cancel the scale command correctly.
	 * 
	 * @param entity The entity being added
     * @param command The scale command 
     * @param result The result object
	 */
	private void scaleEntity(
			PositionableEntity entity, 
			Command command,
			RuleEvaluationResult result) {
		
		// Can only scale auto miter line extrusions.
		Boolean autoMiterLine = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					entity, 
					ChefX3DRuleProperties.MITRE_AUTO);
		
		if (!autoMiterLine) {
			return;
		}
		
		// Begin adjustments
		double[] newPos = new double[3];
		float[] scaleData = new float[3];
		float[] size = new float[3];
		
		newPos = TransformUtils.getExactPosition(entity);
		entity.getSize(size);
		
		if (command instanceof ScaleEntityCommand) {
			
			float[] startingScale = new float[3];
			entity.getStartingScale(startingScale);
			
			((ScaleEntityCommand)command).getNewScale(scaleData);

			// We have to be sneaky and set the starting scale to whatever
			// scale we end up with even though we cancel the scale command here
			// so that we can pick up where we left off correctly.
			SceneManagementUtility.changeProperty(
					model, 
					entity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					PositionableEntity.START_SCALE_PROP, 
					startingScale, 
					scaleData, 
					false);
			
			((PositionableEntity)entity).setStartingScale(scaleData);
			
		} else {
			
			((ScaleEntityTransientCommand)command).getScale(scaleData);
			
		}

		float width = (size[0] * scaleData[0]);
    	
		float[] spine = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ExtrusionEntity.SPINE_VERTICES_PROP);
		
		spine[0] = -width/2.0f - 0.25f;
		spine[1] = spine[4];
		spine[2] = spine[5];
		
		spine[3] = -width/2.0f;
		
		spine[6] = width/2.0f;
		
		spine[9] = width/2.0f + 0.25f;
		spine[10] = spine[7];
		spine[11] = spine[8];
		
		// set the spline coords
        RulePropertyAccessor.setProperty(
                entity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                spine);
	
        // Perform miter cuts
		generateAutoMiterLines(entity, command);
		
		// Make sure we adjust position by any imbalance created between the 
		// outside extents of the spine.
		if (!command.isTransient()) {
		
			double[] adjustment = getSpineBlancingAdjustment(entity, command);

			newPos[0] += adjustment[0];
			newPos[1] += adjustment[1];
			newPos[2] += adjustment[2];

			//-----------------------------------------------------------------
			// Set the starting scale
			//-----------------------------------------------------------------
			setStartingScale(entity, (ScaleEntityCommand) command);
				
		}
      
        // Move the entity by the scale position adjustment from the scale 
        // command.
        SceneManagementUtility.moveEntity(
        		model, 
        		collisionChecker, 
        		(PositionableEntity) entity, 
        		null, 
        		newPos, 
        		command.isTransient(), 
        		true);
        
        // Do this for scales so the nudges will see our entity.
        SceneManagementUtility.removeSurrogate(collisionChecker, entity);
	}
	
	/**
	 * Process remove commands by nudging all appropriate miter neighbors.
	 * 
	 * @param miterEntity The miterEntity being removed.
	 */
	private void processRemoveCommand(PositionableEntity miterEntity) {
		
		// First we need to know if the miterEntity auto generates miter lines
		// or just uses miter lines. This will determine what we set 
		// targetEntity to.		
		Boolean autoMiterLine = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					miterEntity, 
					ChefX3DRuleProperties.MITRE_AUTO);
		
		// Get the parent of the miterEntity to pass along when adjusting sides
		PositionableEntity targetParent = (PositionableEntity) 
			SceneHierarchyUtility.getExactParent(model, miterEntity);
		
		// Dummy command to test collision with
		Command dummyCmd = null;
		
		if (autoMiterLine) {
			
			// Get the exact position to test at.
			double[] dummyTestPos = TransformUtils.getExactPosition(
					miterEntity);
			float[] bounds = BoundsUtils.getBounds(miterEntity, true);
			float[] size = new float[3];
			miterEntity.getSize(size);
			
			float[] scale = new float[3];
			scale[0] = (bounds[1] - bounds[0]) / size[0];
			scale[1] = (bounds[3] - bounds[2]) / size[1];
			scale[2] = (bounds[5] - bounds[4]) / size[2];
			
			dummyCmd = 
				new TransitionEntityChildCommand(
						model, 
						miterEntity, 
						targetParent, 
						dummyTestPos, 
						new float[] {0.0f, 0.0f, 1.0f, 0.0f}, 
						scale, 
						targetParent,
						dummyTestPos, 
						new float[] {0.0f, 0.0f, 1.0f, 0.0f}, 
						scale, 
						false);
		
		} else {
			
			// create a temporary command to check the collisions of the parent
		    double[] pos = TransformUtils.getExactPosition(targetParent);
		    float[] startScale = TransformUtils.getScale(targetParent);
		    float[] endScale = new float[] {
		            startScale[0] + startScale[0] * 0.1f, 
		            startScale[1] + startScale[0] * 0.1f, 
		            startScale[2] + startScale[0] * 0.1f};
		    
		    dummyCmd =
		        new ScaleEntityCommand(
		                model, 
		                0, 
		                targetParent, 
		                pos, 
		                pos, 
		                endScale, 
		                startScale);
		}

		// Find all collisions with the miterEntity
		rch.performCollisionCheck(dummyCmd, true, false, false);
		
		// Bail out if there are illegal collisions
		rch.performCollisionAnalysisHelper(
				miterEntity, 
				null, 
				false, 
				null, 
				true);
		
		if (rch.hasIllegalCollisionExtendedHelper()) {
			return;
		}
		
		// Copy the collisions to a new list so we don't lose them.
		List<Entity> collisionList = new ArrayList<Entity>();
        collisionList.addAll(rch.collisionEntities);

		//-------------------------------
        // Look for a match to merge with
        //-------------------------------
        if (collisionList != null) {

            // Look for other auto adjusting miter entities to work with.
            for(Entity compareEntity : collisionList){

                // If it has a miter line or uses a miter line, nudge it
            	Boolean compareHasMiterLine = (Boolean)
            		RulePropertyAccessor.getRulePropertyValue(
            				compareEntity, 
            				ChefX3DRuleProperties.MITRE_HAS_LINE);
            	
            	Boolean compareAutoMiterLine = (Boolean)
            		RulePropertyAccessor.getRulePropertyValue(
            				compareEntity, 
            				ChefX3DRuleProperties.MITRE_AUTO);
            	
            	if (!compareHasMiterLine && !compareAutoMiterLine) {
            		continue;
            	}
            	
            	// compareEntity must be a PositionableEntity
            	if (!(compareEntity instanceof PositionableEntity)) {
            		continue;
            	}
            	
                // Perform a nudge on the compareEntity.
                // Perform a check to avoid cyclic loop in commands being
                // issued.
                List<Command> commands = 
                    CommandSequencer.getInstance().getFullCommandList(true);
             
                boolean nudge = true;
                
                for (int i = 0; i < commands.size(); i++) {
                    Command cmd = commands.get(i);
                    if (cmd instanceof RuleDataAccessor) {
                        Entity check = ((RuleDataAccessor)cmd).getEntity();
                        if (check == compareEntity) {
                            nudge = false;
                            break;
                        }
                    }
                }
               
                if (nudge) {
                	
                	// Remove auto adds from the miter entity 
                	// to nudge so we have clean auto add analysis
                	AutoAddUtility.removeNonCriticalAutoAdds(
                			model, compareEntity, rch, view);

                    // nudge the miter object
                    SceneManagementUtility.nudgeEntity(
                            model, 
                            collisionChecker, 
                            (PositionableEntity)compareEntity, 
                            false);
                }
            }
        }
	}
	
	/**
	 * Generate the miter lines by looking at the neighboring parents and 
	 * correctly adjusting the vertex points based on them. This does not adjust
	 * auto miter lines.
	 * 
     * @param miterEntity The miter entity being affected
     * @param targetParent The parent the miter is being applied to 
	 * @param command The command being processed
	 */
	private void generateMiterLinesFromParent(
	        PositionableEntity miterEntity, 
	        PositionableEntity targetParent, 
	        Command command) {
	    
	    // reset to the basic config
	    generateDefaultMiterLines(miterEntity, targetParent, command);
	    
	    // create a temporary command to check the collisions of the parent
	    double[] pos = TransformUtils.getPosition(targetParent);
	    float[] startScale = TransformUtils.getScale(targetParent);
	    float[] endScale = new float[] {
	            startScale[0] + startScale[0] * 0.1f, 
	            startScale[1] + startScale[0] * 0.1f, 
	            startScale[2] + startScale[0] * 0.1f};
	    
	    ScaleEntityCommand tmpCmd =
	        new ScaleEntityCommand(
	                model, 
	                0, 
	                targetParent, 
	                pos, 
	                pos, 
	                endScale, 
	                startScale);
	    	    
	    // Perform collision check
        rch.performCollisionCheck(tmpCmd, true, false, false);
                
        List<Entity> collisionList = new ArrayList<Entity>();
        collisionList.addAll(rch.collisionEntities);

		//-------------------------------
        // Look for a match to merge with
        //-------------------------------
        if (collisionList != null) {
            
            PositionableEntity activeZone = 
                (PositionableEntity)SceneHierarchyUtility.getActiveZoneEntity(
                		model);
           
            // get the front left and front right points for comparison                     
            float[] point = getPointInLocalCoords(
                    activeZone, 
                    targetParent, 
                    ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT);
                                
            Point3f mitreLineFrontLeft = 
                new Point3f(point[0], 0, point[2]);

            point = getPointInLocalCoords(
                    activeZone, 
                    targetParent, 
                    ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT);
                                
            Point3f mitreLineFrontRight = 
                new Point3f(point[0], 0, point[2]);
            
            // which distance check was the shortest
            boolean leftSide = true;

            for(Entity compareEntity : collisionList){

            	// If the compareEntity is our targetParent, skip it. We have
            	// already adjusted ourselves to the targetParent.
                if (compareEntity == targetParent)
                    continue;

                // The compareMitre of the compareEntity. If there isn't one
                // then there is no way we can be colliding with it so don't
                // do any adjustment.
                Entity compareMiter = getMiterChild(compareEntity, true);
                
                if (compareMiter == null || compareMiter == miterEntity)
                	continue;

                // consider products have miter lines
                Boolean hasMiterLine = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            compareEntity,
                            ChefX3DRuleProperties.MITRE_HAS_LINE);

                if (hasMiterLine) {
                    
                    // compare the front spline segments to find the shortest
                    // distance between the two entity; this will be segment
                    // we need to adjust
                    
                    // need to convert the position to be relative to the 
                    // active zone, the item may be ion an adjacent zone
                    
                    // get the front left and front right points for comparison                     
                    point = getPointInLocalCoords(
                            activeZone, 
                            (PositionableEntity)compareEntity, 
                            ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT);
                                        
                    Point3f compareLineFrontLeft = 
                        new Point3f(point[0], 0, point[2]);
  
                    point = getPointInLocalCoords(
                            activeZone, 
                            (PositionableEntity)compareEntity, 
                            ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT);
                    
                    Point3f compareLineFrontRight = 
                        new Point3f(point[0], 0, point[2]);
 
                    // get distance from product A left front to product B 
                    // left front
                    float distance =
                        mitreLineFrontLeft.distance(compareLineFrontLeft);                   
                    leftSide = true;
                    
                    // get distance from product A left front to product B 
                    // right front                   
                    float compareDistance = 
                        mitreLineFrontLeft.distance(compareLineFrontRight);
                    if (distance > compareDistance) {
                        distance = compareDistance;
                        leftSide = true;
                    }
                    
                    // get distance from product A right front to product B 
                    // left front                   
                    compareDistance = 
                        mitreLineFrontRight.distance(compareLineFrontLeft);
                    if (distance > compareDistance) {
                        distance = compareDistance;
                        leftSide = false;
                    }
                    
                    // get distance from product A right front to product B 
                    // left front                   
                    compareDistance = 
                        mitreLineFrontRight.distance(compareLineFrontRight);
                    if (distance > compareDistance) {
                        distance = compareDistance;
                        leftSide = false;
                    }

                    adjustMiterSides(
                            miterEntity, 
                            targetParent, 
                            compareEntity, 
                            distance, 
                            leftSide,
                            command);
                    
                    // Perform a nudge on the compareEntity.
                    // Perform a check to avoid cyclic loop in commands being
                    // issued.
                    // If the compareEntity is an auto miter entity,
                	// then nudge its miter child
                    
                    Entity nudgeEntity = compareEntity;
                    
                    Boolean autoMiter = (Boolean)
                    	RulePropertyAccessor.getRulePropertyValue(
                    			nudgeEntity, 
                    			ChefX3DRuleProperties.MITRE_AUTO);
                    
                    if (autoMiter) {
                    	
                    	nudgeEntity = getMiterChild(compareEntity, true);
                    }
                    
                    if (nudgeEntity != null) {
                    	
	                    List<Command> commands = 
	                        CommandSequencer.getInstance().getFullCommandList(
	                        		true);
	                 
	                    boolean nudge = true;
	                    
	                    for (int i = 0; i < commands.size(); i++) {
	                        Command cmd = commands.get(i);
	                        if (cmd instanceof RuleDataAccessor) {
	                            Entity check = 
	                            	((RuleDataAccessor)cmd).getEntity();
	                            if (check == nudgeEntity) {
	                                nudge = false;
	                                break;
	                            }
	                        }
	                    }
	                   
	                    if (nudge) {
	
	                    	// Remove auto adds from the miter entity 
	                    	// to nudge so we have clean auto add analysis
	                    	AutoAddUtility.removeNonCriticalAutoAdds(
	                    			model, compareEntity, rch, view);
	                    	
	                        // nudge the miter object
	                        SceneManagementUtility.nudgeEntity(
	                                model, 
	                                collisionChecker, 
	                                (PositionableEntity)nudgeEntity, 
	                                false);
	                    }
                    }
                }                
            }
        }
        
        // Nudge the starting mitre collisions if this is a MoveEntityCommand
        // or a non-transient TransitionEntityChildCommand.
        if (!command.isTransient()) {
        	
        	PositionableEntity testEntity = null;
        	
        	Boolean miterEntityHasMiterLine = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        miterEntity,
                        ChefX3DRuleProperties.MITRE_HAS_LINE);
        	
        	if (miterEntityHasMiterLine) {
        		testEntity = miterEntity;
        	} else {
        		testEntity = (PositionableEntity) 
        			SceneHierarchyUtility.getExactStartParent(
        					model, miterEntity);
        	}
        	
    		pos = TransformUtils.getStartPosition(testEntity);
    	    startScale = TransformUtils.getScale(testEntity);
    	    endScale = new float[] {
    	            startScale[0] + startScale[0] * 0.1f, 
    	            startScale[1] + startScale[0] * 0.1f, 
    	            startScale[2] + startScale[0] * 0.1f};
            
            tmpCmd =
                new ScaleEntityCommand(
                        model, 
                        0, 
                        testEntity,
                        pos, 
                        pos, 
                        endScale, 
                        startScale);
 
            // Perform collision check
            rch.performCollisionCheck(tmpCmd, true, false, false);
            
            collisionList.clear();
            
            for (int i = 0; i < rch.collisionEntities.size(); i++) {
                Entity compare = rch.collisionEntities.get(i);
                if (!collisionList.contains(compare)) {
                    collisionList.add(compare);
                }
            }

            // Check each compare entity for mitre cut children that
            // we haven't already nudged but need to.
            for(Entity compareEntity : collisionList){

            	// If the compareEntity is our targetParent, skip it. We 
            	// have already adjusted ourselves to the targetParent.
                if (compareEntity == testEntity) {
                    continue;
                }
                
                // The the compareMitre of the compareEntity. If there isn't
                // one then there is no way we can be colliding with it so 
                // don't do any adjustment.
                Entity compareMiter = getMiterChild(compareEntity, true);
                
                if (compareMiter == null || compareMiter == miterEntity) {
                	continue;
                }
                
                // consider products have miter lines, and that don't auto 
                // genreate them
                Boolean hasMiterLine = 
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            compareEntity,
                            ChefX3DRuleProperties.MITRE_HAS_LINE);
                
                Boolean autoMiter = (Boolean)
	            	RulePropertyAccessor.getRulePropertyValue(
	            			compareEntity, 
	            			ChefX3DRuleProperties.MITRE_AUTO);

                if (hasMiterLine && !autoMiter) {

                    // Perform a nudge on the compareEntity.
                    // Perform a check to avoid cyclic loop in commands 
                	// being issued.
                    List<Command> commands = 
                        CommandSequencer.getInstance().getFullCommandList(
                        		true);
                 
                    boolean nudge = true;
                    
                    for (int i = 0; i < commands.size(); i++) {
                        Command cmd = commands.get(i);
                        if (cmd instanceof RuleDataAccessor) {
                            Entity check = 
                            	((RuleDataAccessor)cmd).getEntity();
                            if (check == compareEntity) {
                                nudge = false;
                                break;
                            }
                        }
                    }
                   
                    if (nudge) {
                    	
                    	// Remove auto adds from the miter entity 
                    	// to nudge so we have clean auto add analysis
                    	AutoAddUtility.removeNonCriticalAutoAdds(
                    			model, compareEntity, rch, view);

                        // nudge the miter object
                        SceneManagementUtility.nudgeEntity(
                                model, 
                                collisionChecker, 
                                (PositionableEntity)compareEntity, 
                                false);
                    }
                }                
            }
        }
	}
	
	/**
	 * Generate the miter lines for non-transient commands by looking at the 
	 * collisions and adjusting the auto miter lines accordingly.
	 * 
     * @param miterEntity The miter entity being affected
	 * @param command The command being processed
	 */
	private void generateAutoMiterLines(
	        PositionableEntity miterEntity,
	        Command command) {

		// If the command is not transient, calculate the miter or butt cut
		// ends for the auto miter line entity.
		if (!command.isTransient()) {
			
			// Determine if we need to prompt again.
			MITER_SIDE_TO_PROCESS sideToProcess = 
        		(MITER_SIDE_TO_PROCESS)
        		RulePropertyAccessor.getRulePropertyValue(
        				miterEntity, 
        				MITER_SIDE_PROP);

			// Get the parent of the miterEntity to pass along when adjusting 
			// sides.
			PositionableEntity targetParent = (PositionableEntity) 
				SceneHierarchyUtility.getExactParent(model, miterEntity);
			
			// Get the exact position, bounds and size to test at.
			double[] dummyTestPos = 
				TransformUtils.getExactPosition(miterEntity);
			
			float[] size = new float[3];
			miterEntity.getSize(size);
			
			float[] scale = new float[3];
			scale[0] = 1.1f;
			scale[1] = 1.1f;
			scale[2] = 1.1f;
			
			// Create the dummy command.
			TransitionEntityChildCommand dummyCmd = 
				new TransitionEntityChildCommand(
						model, 
						miterEntity, 
						targetParent, 
						dummyTestPos, 
						new float[] {0.0f, 0.0f, 1.0f, 0.0f}, 
						scale, 
						targetParent,
						dummyTestPos, 
						new float[] {0.0f, 0.0f, 1.0f, 0.0f}, 
						scale, 
						false);
			
			// Find all collisions with the miterEntity
			rch.performCollisionCheck(dummyCmd, true, false, false);
			
			// Bail out if there are illegal collisions
			rch.performCollisionAnalysisHelper(
					miterEntity, 
					null, 
					false, 
					null, 
					true);
			
			if (rch.hasIllegalCollisionExtendedHelper()) {
				return;
			}
			
			// Copy the collisions to a new list so we don't lose them.
			List<Entity> collisionList = new ArrayList<Entity>();
		    collisionList.addAll(rch.collisionEntities);
		
			//-------------------------------
		    // Look for a match to miter with
		    //-------------------------------
		    if (collisionList != null) {
		       
		        // get the front left and front right points for auto miter
		        // line already in existence. If sideToProcess is set, grab
		    	// the last known good shape to updated as the spine.
		        float[] spine1;
		        
		        if (sideToProcess == null) {
		        	
		        	spine1 = (float[])
			        	RulePropertyAccessor.getRulePropertyValue(
			                miterEntity,
			                ExtrusionEntity.SPINE_VERTICES_PROP);
		        } else {
		        	
		        	float[] tmp = (float[])
		        		RulePropertyAccessor.getRulePropertyValue(
	        				miterEntity, 
	        				ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
		        	
		        	if (tmp == null) {
		        		return;
		        	}
		        	
		        	spine1 = new float[tmp.length];
		        	
		        	for (int i = 0; i < tmp.length; i++) {
		        		spine1[i] = tmp[i];
		        	}
		        }
		                            
		        Point3f mitreLineFrontLeft = 
		            new Point3f(spine1[3], 0, spine1[5]);
		                            
		        Point3f mitreLineFrontRight = 
		            new Point3f(spine1[6], 0, spine1[8]);
		        
		        // which distance check was the shortest
		        boolean leftSide = true;
		
		        // Look for other auto adjusting miter entities to work with.
		        for(Entity compareEntity : collisionList){
		        	
		            // Look for auto miter line flag
		            Boolean autoMitreLine = 
		                (Boolean)RulePropertyAccessor.getRulePropertyValue(
		                        compareEntity,
		                        ChefX3DRuleProperties.MITRE_AUTO);
		            
		            if (autoMitreLine) {
		                
		                // compare the front spline segments to find the 
		            	// shortest distance between the two entity; this will 
		            	// be segment we need to adjust
		                
		                // need to convert the position to be relative to the 
		                // active zone, the item may be ion an adjacent zone
		                
		                // get the front left and front right points for 
		            	//comparison 
		            	float[] spine2 = (float[])
		                	RulePropertyAccessor.getRulePropertyValue(
		                        compareEntity,
		                        ExtrusionEntity.SPINE_VERTICES_PROP);
		            	
		            	// Front left
		            	float[] compareFrontLeft = 
		            		new float[] {spine2[3], 0, spine2[5]};
		            	
		            	compareFrontLeft = getPointInLocalCoords(
		            			(PositionableEntity) miterEntity, 
		            			(PositionableEntity) compareEntity, 
		            			compareFrontLeft);
		            	
		            	Point3f compareLineFrontLeft = 
		            		new Point3f(compareFrontLeft);
		            	
		            	// Front right
		            	float[] compareFrontRight = 
		            		new float[] {spine2[6], 0, spine2[8]};
		            	
		            	compareFrontRight = getPointInLocalCoords(
		            			(PositionableEntity) miterEntity, 
		            			(PositionableEntity) compareEntity, 
		            			compareFrontRight);
		            	
		            	Point3f compareLineFrontRight = 
		                    new Point3f(compareFrontRight);
		            	
		                // get distance from product A left front to product B 
		                // left front
		                float distance =
		                    mitreLineFrontLeft.distance(compareLineFrontLeft);                   
		                leftSide = true;
		                
		                // get distance from product A left front to product B 
		                // right front                   
		                float compareDistance = 
		                    mitreLineFrontLeft.distance(compareLineFrontRight);
		                if (distance > compareDistance) {
		                    distance = compareDistance;
		                    leftSide = true;
		                }
		                
		                // get distance from product A right front to product B 
		                // left front                   
		                compareDistance = 
		                    mitreLineFrontRight.distance(compareLineFrontLeft);
		                if (distance > compareDistance) {
		                    distance = compareDistance;
		                    leftSide = false;
		                }
		                
		                // If we have a collision with another autoMitreLine 
		                // product ask the user if they want to perform a miter 
		                // cut. If so do the mitre cut operations. Otherwise, 
		                // do a butt cut. Note that the butt cut does not nudge 
		                // the collisions.
		            	boolean doMitreCut = true;
		            	
		            	if (sideToProcess == null) {
		            		
		            		doMitreCut = popUpConfirm.showMessage(CONFIRM_MSG);
		            		
		            	} else {
		            		
		            		if (sideToProcess == 
		            					MITER_SIDE_TO_PROCESS.NEGATIVE && 
		            					leftSide == false) {
		            			
		            			continue;
		            			
		            		} else if (sideToProcess == 
		            					MITER_SIDE_TO_PROCESS.POSITIVE && 
		            					leftSide == true){
		            			
		            			continue;
		            		}
		            	}
		            	
		            	if (!doMitreCut) {
		            		
		            		float[] miterEntityFrontLeft = new float[] {
		            				spine1[3],
		            				spine1[4],
		            				spine1[5]};
		            		
		            		float[] miterEntityFrontRight = new float[] {
		            				spine1[6],
		            				spine1[7],
		            				spine1[8]};
		            		
		            		float[] collisionEntityFrontLeft = new float[] {
		            				spine2[3],
		            				spine2[4],
		            				spine2[5]};
		            		
		            		float[] collisionEntityFrontRight = new float[] {
		            				spine2[6],
		            				spine2[7],
		            				spine2[8]};
		            		
		            		adjustButtCut(
		            				miterEntity, 
		            				miterEntityFrontLeft, 
		            				miterEntityFrontRight, 
		            				compareEntity, 
		            				collisionEntityFrontLeft, 
		            				collisionEntityFrontRight, 
		            				leftSide);
		            		
		            		return;
		            		
		            	} else {
		
		                    adjustAutoMiterSides(
		                            miterEntity, 
		                            spine1,
		                            targetParent, 
		                            compareEntity,
		                            spine2,
		                            distance, 
		                            leftSide);
		            	}

		                // Perform a nudge on the compareEntity.
		            	// If the miterEntity has the MITER_SIDE_PROP set then
		            	// don't nudge anything and set MITER_SIDE_PROP to null.
		            	// If the miterEntity doesn't have MITER_SIDE_PROP set
		            	// then nudge the compareEntity and make sure to set the
		            	// MITER_SIDE_PROP so we know which end to apply the 
		            	// miter cut to.
		                // Perform a check to avoid cyclic loop in commands 
		            	// being issued.
		            	
		            	if (sideToProcess == null) {
		            		
		                    List<Command> commands = 
		                        CommandSequencer.getInstance().
		                        	getFullCommandList(
		                        		true);
		                 
		                    boolean nudge = true;
		                    
		                    for (int i = 0; i < commands.size(); i++) {
		                        Command cmd = commands.get(i);
		                        if (cmd instanceof RuleDataAccessor) {
		                            Entity check = 
		                            	((RuleDataAccessor)cmd).getEntity();
		                            if (check == compareEntity) {
		                            	nudge = false;
		                                break;
		                            }
		                        }
		                    }
		                   
		                    if (nudge) {
		                    	
		                    	// If the current miter side is the left side,
		                    	// then the compareEntity is to the left
		                    	// of the miter entity. If the miter side is
		                    	// the right side, the compareEntity is to the
		                    	// right of the miter entity. We want to mark
		                    	// which end of the compare entity we are 
		                    	// nudging should have the miter cut calculated
		                    	// for. This will always be the opposite of the
		                    	// leftSide value.
		                    	MITER_SIDE_TO_PROCESS newSideToProcess = 
		                    		MITER_SIDE_TO_PROCESS.NEGATIVE;
		                    	
		                    	if (leftSide) {
		                    		newSideToProcess = 
		                    			MITER_SIDE_TO_PROCESS.POSITIVE;
		                    	} else {
		                    		newSideToProcess = 
		                    			MITER_SIDE_TO_PROCESS.NEGATIVE;
		                    	}
		                    	
		                    	RulePropertyAccessor.setRuleProperty(
		                    			compareEntity, 
		                    			MITER_SIDE_PROP, 
		                    			newSideToProcess);
		                    	
		                    	// Remove auto adds from the miter entity 
		                    	// to nudge so we have clean auto add analysis
		                    	AutoAddUtility.removeNonCriticalAutoAdds(
		                    			model, compareEntity, rch, view);
	
		                    	// nudge the miter object
			                    SceneManagementUtility.nudgeEntity(
			                            model, 
			                            collisionChecker, 
			                            (PositionableEntity)compareEntity, 
			                            false);
			                }
		                    
		            	} else {
		            		
		            		RulePropertyAccessor.setRuleProperty(
		            				miterEntity, 
		            				MITER_SIDE_PROP, 
		            				null);
		            	}
		            }                
		        }
		    }
		    
		    //------------------------------------------------------------------
		    // Nudge any starting collisions
		    //------------------------------------------------------------------
		    
		    if (sideToProcess == null) {
		    	
			    targetParent = (PositionableEntity) 
					SceneHierarchyUtility.getExactStartParent(
							model, miterEntity);
				
				// Get the exact position, bounds and size to test at.
				dummyTestPos = 
					TransformUtils.getExactStartPosition(miterEntity);
				
				scale = TransformUtils.getStartingScale(miterEntity);
				
				float[] bounds = BoundsUtils.getBounds(miterEntity, true);
				float[] startingBounds = BoundsUtils.getStartingBounds(
						miterEntity);
				float[] initSize = new float[3];
				miterEntity.getSize(initSize);
				
				scale[0] = (
						startingBounds[1] - startingBounds[0]) / 
						(bounds[1] - bounds[0]);
				
				// Create the dummy command.
				dummyCmd = 
					new TransitionEntityChildCommand(
							model, 
							miterEntity, 
							targetParent, 
							dummyTestPos, 
							new float[] {0.0f, 0.0f, 1.0f, 0.0f}, 
							scale, 
							targetParent,
							dummyTestPos, 
							new float[] {0.0f, 0.0f, 1.0f, 0.0f}, 
							scale, 
							false);
				
				// Find all collisions with the miterEntity
				rch.performCollisionCheck(dummyCmd, true, false, false);
				
				// Bail out if there are illegal collisions
				rch.performCollisionAnalysisHelper(
						miterEntity, 
						null, 
						false, 
						null, 
						true);
				
				if (rch.hasIllegalCollisionExtendedHelper()) {
					return;
				}
				
				// Copy the collisions to a new list so we don't lose them.
				collisionList.clear();
			    collisionList.addAll(rch.collisionEntities);
			
				//-------------------------------
			    // Look for a starting state match to nudge
			    //-------------------------------
			    if (collisionList != null) {
			
			        // Look for other auto adjusting miter entities to work with
			        for(Entity compareEntity : collisionList){
			
			            // Look for auto miter line flag
			            Boolean autoMitreLine = 
			                (Boolean)RulePropertyAccessor.getRulePropertyValue(
			                        compareEntity,
			                        ChefX3DRuleProperties.MITRE_AUTO);
			
			            if (autoMitreLine) {
			                
			                // Perform a nudge on the compareEntity.
			                // Perform a check to avoid cyclic loop in commands 
			            	// being issued.
		                    List<Command> commands = 
		                        CommandSequencer.getInstance().
		                        getFullCommandList(
		                        		true);
		                 
		                    boolean nudge = true;
		                    
		                    for (int i = 0; i < commands.size(); i++) {
		                        Command cmd = commands.get(i);
		                        if (cmd instanceof RuleDataAccessor) {
		                            Entity check = 
		                            	((RuleDataAccessor)cmd).getEntity();
		                            if (check == compareEntity) {
		                                nudge = false;
		                                break;
		                            }
		                        }
		                    }
		                   
		                    if (nudge) {
	
			                    // nudge the miter object
			                    SceneManagementUtility.nudgeEntity(
			                            model, 
			                            collisionChecker, 
			                            (PositionableEntity)compareEntity, 
			                            false);
			                }
			            }                
			        }
			    }
		    }
		}
        
        // For both transient and non-transient commands, adjust the 
		// spine, but if the command is non-transient also adjust the 
		// last good spine line.
        
        float[] spine = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				miterEntity, 
				ExtrusionEntity.SPINE_VERTICES_PROP);
	
		float[] oldSpineValue = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
				miterEntity, 
				ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
		
		if (!command.isTransient()) {
			
			float[] newSpineValue = new float[spine.length];
			for (int i = 0; i < spine.length; i++) {
				newSpineValue[i] = spine[i];
			}
	    	
	    	SceneManagementUtility.changeProperty(
					model, 
					miterEntity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD, 
					oldSpineValue, 
					newSpineValue, 
					command.isTransient());
		
		}
		
		float[] newOldSpine = new float[spine.length];
		for (int i = 0; i < spine.length; i++) {
			newOldSpine[i] = spine[i];
		}
    
	    SceneManagementUtility.changeProperty(
				model, 
				miterEntity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				ExtrusionEntity.SPINE_VERTICES_PROP, 
				oldSpineValue, 
				newOldSpine, 
				command.isTransient());
	    
	    // Set the miter line points for things like crown molding to use.
	    float[] mitreLineBackLeft = new float[2];
	    mitreLineBackLeft[0] = newOldSpine[3];
	    mitreLineBackLeft[1] = newOldSpine[5];
        
	    RulePropertyAccessor.setRuleProperty(
	    		miterEntity, 
	    		ChefX3DRuleProperties.MITRE_LINE_BACK_LEFT, 
	    		mitreLineBackLeft);
        
	    float[] mitreLineFrontLeft = new float[2];
	    mitreLineFrontLeft[0] = newOldSpine[3];
	    mitreLineFrontLeft[1] = newOldSpine[5];
        
	    RulePropertyAccessor.setRuleProperty(
	    		miterEntity, 
	    		ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT, 
	    		mitreLineFrontLeft);
        
	    float[] mitreLineFrontRight = new float[2];
	    mitreLineFrontRight[0] = newOldSpine[6];
	    mitreLineFrontRight[1] = newOldSpine[8];
	    
        RulePropertyAccessor.setRuleProperty(
	    		miterEntity, 
	    		ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT, 
	    		mitreLineFrontRight);
        
        float[] mitreLineBackRight = new float[2];
	    mitreLineBackRight[0] = newOldSpine[6];
	    mitreLineBackRight[1] = newOldSpine[8];
        
	    RulePropertyAccessor.setRuleProperty(
	    		miterEntity, 
	    		ChefX3DRuleProperties.MITRE_LINE_BACK_RIGHT, 
	    		mitreLineBackRight);
	    
	}
	
	/**
	 * Set the properties for a basic miter line around the target parent.
	 * 
	 * @param miterEntity The miter entity
	 * @param targetParent The target parent to apply the miter entity to
	 * @param commadn The command
	 */
    private void generateDefaultMiterLines(
            Entity miterEntity, 
            Entity targetParent,
            Command command) {
        
        float[] mitreLineBackLeft = new float[] {0, 0};
        float[] mitreLineFrontLeft = new float[] {0, 0};
        float[] mitreLineFrontRight = new float[] {0, 0};
        float[] mitreLineBackRight = new float[] {0, 0};
        
        // consider products have miter lines
        Boolean hasMiterLine = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    targetParent,
                    ChefX3DRuleProperties.MITRE_HAS_LINE);

        if (hasMiterLine) {
            
            mitreLineBackLeft = 
                (float[])RulePropertyAccessor.getRulePropertyValue(
                        targetParent,
                        ChefX3DRuleProperties.MITRE_LINE_BACK_LEFT);
            
            mitreLineFrontLeft = 
                (float[])RulePropertyAccessor.getRulePropertyValue(
                        targetParent,
                        ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT);
            
            mitreLineFrontRight = 
                (float[])RulePropertyAccessor.getRulePropertyValue(
                        targetParent,
                        ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT);
            
            mitreLineBackRight = 
                (float[])RulePropertyAccessor.getRulePropertyValue(
                        targetParent,
                        ChefX3DRuleProperties.MITRE_LINE_BACK_RIGHT);

        } else {
        	return;
        }
        
        
        float[] spine = new float[] {
                mitreLineBackLeft[0], 0, mitreLineBackLeft[1], 
                mitreLineFrontLeft[0], 0, mitreLineFrontLeft[1], 
                mitreLineFrontRight[0], 0, mitreLineFrontRight[1], 
                mitreLineBackRight[0], 0, mitreLineBackRight[1]};
        
        // If the command is not transient, set the last good spine data
        float[] oldSpineValue = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
					miterEntity, 
					ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
        
		if (!command.isTransient()) {

			float[] newSpineValue = new float[spine.length];
			for (int i = 0; i < spine.length; i++) {
				newSpineValue[i] = spine[i];
			}
	    	
	    	SceneManagementUtility.changeProperty(
					model, 
					miterEntity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD, 
					oldSpineValue, 
					newSpineValue, 
					command.isTransient());
		
		}
		
		float[] newOldSpine = new float[spine.length];
		for (int i = 0; i < spine.length; i++) {
			newOldSpine[i] = spine[i];
		}
        
        // set the spline coords
        RulePropertyAccessor.setProperty(
                miterEntity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                newOldSpine);
        
        SceneManagementUtility.changeProperty(
        		model, 
        		miterEntity, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                oldSpineValue,
                newOldSpine, 
        		command.isTransient());
        
        // If the command is not transient, set the last good visible data
        
        boolean[] visible = new boolean[] {true, true, true};
        
        boolean[] oldVisibleValue = (boolean[]) 
		RulePropertyAccessor.getRulePropertyValue(
			miterEntity, 
			ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD);
        
		if (!command.isTransient()) {

			boolean[] newVisibleValue = new boolean[visible.length];
			for (int i = 0; i < visible.length; i++) {
				newVisibleValue[i] = visible[i];
			}
	    	
	    	SceneManagementUtility.changeProperty(
					model, 
					miterEntity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD, 
					oldVisibleValue, 
					newVisibleValue, 
					command.isTransient());
		
		}
		
		boolean[] newOldVisible = new boolean[visible.length];
		for (int i = 0; i < visible.length; i++) {
			newOldVisible[i] = visible[i];
		}
        
        // set segment visibility
        RulePropertyAccessor.setProperty(
                miterEntity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.VISIBLE_PROP, 
                newOldVisible);
        
        SceneManagementUtility.changeProperty(
        		model, 
        		miterEntity, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.VISIBLE_PROP, 
                oldVisibleValue,
                newOldVisible, 
        		command.isTransient());

    }
		
    /**
     * Adjust the spline data of the miter entity by using the collision
     * entities front face vertices.
     * 
     * @param miterEntity The miter entity to update the spline for 
     * @param parent The parent of the miter entity
     * @param collisionEntity The entity beside the parent that also has a 
     * miter entity
     * @param distance The distance between two closet front face points
     * @param leftSide Is the collision entity on the left (true) or right 
     * (false).
     * @param command The command
     */
	private void adjustMiterSides(
            Entity miterEntity, 
            Entity parent, 
            Entity collisionEntity, 
	        float distance, 
	        boolean leftSide,
	        Command command) {
	    
	    // get the collision entity miter product if it exists
	    Entity miterCollision = getMiterChild(collisionEntity, true);
	    
	    // if there is no miter child then there is no adjustment required
	    if (miterCollision == null)
	        return;
	    
        // get the current vertices
        float[] spline1 = 
            (float[])RulePropertyAccessor.getRulePropertyValue(
                    miterEntity, 
                    ExtrusionEntity.SPINE_VERTICES_PROP);          

        // get the current segment visibility
        boolean[] visible1 = 
            (boolean[])RulePropertyAccessor.getRulePropertyValue(
                    miterEntity, 
                    ExtrusionEntity.VISIBLE_PROP);
        if (visible1 == null) {
            visible1 = new boolean[] {true, true, true};
        }
        
        if (distance < DISTANCE_THRESHOLD) {
            // the two points are overlapping.  
            // 1. take the back point of the current entity and move it to the 
            // front point of the collision entity that is not the overlapping 
            // point.  make that segment hidden.  
            // 2. take the back point of the collision entity and move it to 
            // the front point of the current entity that is not the 
            // overlapping point. make that segment hidden. 
                        
            // define a working variable
            float[] p;
                       
            if (leftSide) {
                
                p = getPointInLocalCoords(
                        (PositionableEntity)parent, 
                        (PositionableEntity)collisionEntity, 
                        ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT);

                spline1[0] = p[0];
                spline1[1] = 0;
                spline1[2] = p[2];
                    
                // make the first segment hidden
                visible1[0] = false;
                                                
            } else {
                
                // deal with the entity    
                p = getPointInLocalCoords(
                        (PositionableEntity)parent, 
                        (PositionableEntity)collisionEntity, 
                        ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT);
                
                spline1[9] = p[0];
                spline1[10] = 0;
                spline1[11] = p[2];
                    
                // make the first segment hidden
                visible1[2] = false;
                                
            }
                        
        } else {
        	return;
        }
        
		// If the command is not transient, set the last good spine data
        float[] oldSpineValue = (float[]) 
		RulePropertyAccessor.getRulePropertyValue(
			miterEntity, 
			ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
        
		if (!command.isTransient()) {

			float[] newSpineValue = new float[spline1.length];
			for (int i = 0; i < spline1.length; i++) {
				newSpineValue[i] = spline1[i];
			}
	    	
	    	SceneManagementUtility.changeProperty(
					model, 
					miterEntity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD, 
					oldSpineValue, 
					newSpineValue, 
					command.isTransient());
		
		}
		
		float[] newOldSpine = new float[spline1.length];
		for (int i = 0; i < spline1.length; i++) {
			newOldSpine[i] = spline1[i];
		}
        
        // set the spline coords
        RulePropertyAccessor.setProperty(
                miterEntity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                newOldSpine);
        
        SceneManagementUtility.changeProperty(
        		model, 
        		miterEntity, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                oldSpineValue,
                newOldSpine, 
        		command.isTransient());
        
        // If the command is not transient, set the last good visible data
        boolean[] oldVisibleValue = (boolean[]) 
		RulePropertyAccessor.getRulePropertyValue(
			miterEntity, 
			ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD);
        
		if (!command.isTransient()) {

			boolean[] newVisibleValue = new boolean[visible1.length];
			for (int i = 0; i < visible1.length; i++) {
				newVisibleValue[i] = visible1[i];
			}
	    	
	    	SceneManagementUtility.changeProperty(
					model, 
					miterEntity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD, 
					oldVisibleValue, 
					newVisibleValue, 
					command.isTransient());
		
		}
		
		boolean[] newOldVisible = new boolean[visible1.length];
		for (int i = 0; i < visible1.length; i++) {
			newOldVisible[i] = visible1[i];
		}
        
        // set segment visibility
        RulePropertyAccessor.setProperty(
                miterEntity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.VISIBLE_PROP, 
                newOldVisible);
        
        SceneManagementUtility.changeProperty(
        		model, 
        		miterEntity, 
        		Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.VISIBLE_PROP, 
                oldVisibleValue,
                newOldVisible, 
        		command.isTransient());
               
	}
	
	/**
     * Adjust the spline data of the auto miter entity by using the collision
     * entities front face vertices.
     * 
     * @param miterEntity The miter entity to update the spline for 
     * @param spline1 miterEntity spine to use
     * @param parent The parent of the miter entity
     * @param collisionEntity The entity beside the parent that also has a 
     * miter entity
     * @param spline2 collisionEntity spine to use
     * @param distance The distance between two closet front face points
     * @param leftSide Is the collision entity on the left (true) or right 
     * (false).
     */
	private void adjustAutoMiterSides(
            Entity miterEntity, 
            float[] spline1,
            Entity parent, 
            Entity collisionEntity, 
            float[] spline2,
	        float distance, 
	        boolean leftSide) {
/*		  
        // get the current vertices
        float[] spline1 = 
            (float[])RulePropertyAccessor.getRulePropertyValue(
                    miterEntity, 
                    ExtrusionEntity.SPINE_VERTICES_PROP)
       
    	// Handle the special case where the auto mitering miterEntity
    	// and miterCollision closest vertices are not within the acceptable
    	// range to do the miter cut.
    	// What we need to do in that case is to extend the correct front
    	// edge vertices from each entity to overlap. We extend along the
    	// front face line of each front face.
    	
        // get the current vertices
        float[] spline2 = 
            (float[])RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity, 
                    ExtrusionEntity.SPINE_VERTICES_PROP);
*/        
    	// Get all front edge vertices of the collisionMiter entity.
    	float[] collisionMiterFrontLeft = new float[3];
    	collisionMiterFrontLeft[0] = spline2[3];
    	collisionMiterFrontLeft[1] = spline2[4];
    	collisionMiterFrontLeft[2] = spline2[5];
    	
    	collisionMiterFrontLeft = getPointInLocalCoords(
                (PositionableEntity)miterEntity, 
                (PositionableEntity)collisionEntity, 
                collisionMiterFrontLeft);
    	
    	if (collisionMiterFrontLeft == null) {
    		return;
    	}
    	
    	float[] collisionMiterFrontRight = new float[3];
    	collisionMiterFrontRight[0] = spline2[6];
    	collisionMiterFrontRight[1] = spline2[7];
    	collisionMiterFrontRight[2] = spline2[8];
    	
    	collisionMiterFrontRight = getPointInLocalCoords(
                (PositionableEntity)miterEntity, 
                (PositionableEntity)collisionEntity, 
                collisionMiterFrontRight);
    	
    	if (collisionMiterFrontRight == null) {
    		return;
    	}

    	// Break this down into a left side or right side problem relative
    	// to the miterEntity.
    	if (leftSide) {

    		// aStartPoint is the spline1 front right corner.
    		float[] aStartPoint = new float[3];
    		aStartPoint[0] = spline1[6];
    		aStartPoint[1] = spline1[7];
    		aStartPoint[2] = spline1[8];

    		// aVec is the vector from spline1 front right corner to 
    		// front left corner.
    		float[] aVec = new float[3];
    		aVec[0] = spline1[3] - spline1[6];
    		aVec[1] = spline1[4] - spline1[7];
    		aVec[2] = spline1[5] - spline1[8];
  		
    		// bStartPoint is the collisionMiter front left corner.
    		float[] bStartPoint = new float[3];
    		bStartPoint[0] = collisionMiterFrontLeft[0];
    		bStartPoint[1] = collisionMiterFrontLeft[1];
    		bStartPoint[2] = collisionMiterFrontLeft[2];
    		
    		// bVec is the vector from collisionMiter front left to
    		// collisionMiter front right.
    		float[] bVec = new float[3];
    		bVec[0] = 
    			collisionMiterFrontRight[0] - collisionMiterFrontLeft[0];
    		bVec[1] = 
    			collisionMiterFrontRight[1] - collisionMiterFrontLeft[1];
    		bVec[2] = 
    			(collisionMiterFrontRight[2] - collisionMiterFrontLeft[2]);
	
    		// Get the point of intersection
    		float[] intersectionPoint = 
    			getIntersectionPoint(aStartPoint, aVec, bStartPoint, bVec);
  		
    		if (intersectionPoint == null) {
    			return;
    		}
 
    		// Set the front left corner to the intersection, and the back
    		// left corner to the front left corner of the collisionMiter
    		// entity.    		
    		spline1[0] = collisionMiterFrontLeft[0];
    		spline1[1] = collisionMiterFrontLeft[1];
    		spline1[2] = collisionMiterFrontLeft[2];
    		
    		spline1[3] = intersectionPoint[0];
    		spline1[4] = intersectionPoint[1];
    		spline1[5] = intersectionPoint[2];
    		
    	} else {
  		
    		// aStartPoint is the spline1 front left corner.
    		float[] aStartPoint = new float[3];
    		aStartPoint[0] = spline1[3];
    		aStartPoint[1] = spline1[4];
    		aStartPoint[2] = spline1[5];

    		// aVec is the vector from spline1 front left corner to 
    		// front right corner.
    		float[] aVec = new float[3];
    		aVec[0] = spline1[6] - spline1[3];
    		aVec[1] = spline1[7] - spline1[4];
    		aVec[2] = spline1[8] - spline1[5];

    		// bStartPoint is the collisionMiter front right corner.
    		float[] bStartPoint = new float[3];
    		bStartPoint[0] = collisionMiterFrontRight[0];
    		bStartPoint[1] = collisionMiterFrontRight[1];
    		bStartPoint[2] = collisionMiterFrontRight[2];

    		// bVec is the vector from collisionMiter front right to
    		// collisionMiter front left.
    		float[] bVec = new float[3];
    		bVec[0] = 
    			collisionMiterFrontLeft[0] - collisionMiterFrontRight[0];
    		bVec[1] = 
    			collisionMiterFrontLeft[1] - collisionMiterFrontRight[1];
    		bVec[2] = 
    			(collisionMiterFrontLeft[2] - collisionMiterFrontRight[2]);
  		
    		// Get the point of intersection
    		float[] intersectionPoint = 
    			getIntersectionPoint(aStartPoint, aVec, bStartPoint, bVec);
    		
    		if (intersectionPoint == null) {
    			return;
    		}

    		// Set the front right corner to the intersection, and the back
    		// right corner to the front right corner of the collisionMiter
    		// entity.

    		spline1[9] = collisionMiterFrontRight[0];
    		spline1[10] = collisionMiterFrontRight[1];
    		spline1[11] = collisionMiterFrontRight[2];
    		
    		spline1[6] = intersectionPoint[0];
    		spline1[7] = intersectionPoint[1];
    		spline1[8] = intersectionPoint[2];
    		
    	}
   	      
        // set the spline coords
        RulePropertyAccessor.setProperty(
                miterEntity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                spline1);
                    
	}
	
	/**
	 * Calculate the adjustment to align miter ends in a butt cut.
	 * 
	 * @param miterEntity Entity to apply miter cut to 
	 * @param miterEntityFrontLeft Front left spine corner position
	 * @param miterEntityFrontRight Front right spine corner position
	 * @param collisionEntity Miter entity in collision with miterEntity
	 * @param collisionEntityFrontLeft Front left spine corner of 
	 * collisionEntity
	 * @param collisionEntityFrontRight Front right spine corner of 
	 * collisionEntity
	 * @param leftSide True if the collisionEntity is oriented to the left
	 * of the miterEntity, false if it is to the right
	 */
	private void adjustButtCut(
            Entity miterEntity,
            float[] miterEntityFrontLeft,
            float[] miterEntityFrontRight,
            Entity collisionEntity, 
            float[] collisionEntityFrontLeft,
            float[] collisionEntityFrontRight,
	        boolean leftSide) {
		
		float[] spine = (float[])
	    	RulePropertyAccessor.getRulePropertyValue(
	            miterEntity,
	            ExtrusionEntity.SPINE_VERTICES_PROP);
		
		final float angleEpsilon = 0.0001f;
		
		// Can only butt cut between 90 degree angled pieces and 180 degree
		// angled pieces.
		Vector3f miterEntityVec = new Vector3f();
		Vector3f collisionEntityVec = new Vector3f();
		
		float angle = 0.0f;
		
		if (leftSide) {
			
			miterEntityVec.set(
					(miterEntityFrontRight[0] - miterEntityFrontLeft[0]), 
					(miterEntityFrontRight[1] - miterEntityFrontLeft[1]), 
					(miterEntityFrontRight[2] - miterEntityFrontLeft[2]));
			
			collisionEntityVec.set(
					collisionEntityFrontLeft[0] - collisionEntityFrontRight[0],
					collisionEntityFrontLeft[1] - collisionEntityFrontRight[1],
					collisionEntityFrontLeft[2] - collisionEntityFrontRight[2]);
			
			angle = collisionEntityVec.angle(miterEntityVec);
			
		} else {
			
			miterEntityVec.set(
					(miterEntityFrontLeft[0] - miterEntityFrontRight[0]), 
					(miterEntityFrontLeft[1] - miterEntityFrontRight[1]), 
					(miterEntityFrontLeft[2] - miterEntityFrontRight[2]));
			
			collisionEntityVec.set(
					collisionEntityFrontRight[0] - collisionEntityFrontLeft[0],
					collisionEntityFrontRight[1] - collisionEntityFrontLeft[1],
					collisionEntityFrontRight[2] - collisionEntityFrontLeft[2]);
			
			angle = miterEntityVec.angle(collisionEntityVec);
			
		}
		
		// Validate the angle
		if ((angle - 1.57079633f) > angleEpsilon && 
				(angle - 3.14159265f) > angleEpsilon) {
			return;
		}
		
		// Now determine the butt cut correction to apply.
		// Do this by getting the bounds of the collision entity and use the 
		// front depth bounds in local coordinates as our front edge.
		float[] collisionEntityBounds = BoundsUtils.getBounds(
				(PositionableEntity) collisionEntity, true);
		
		double[] positionRelativeToSource = 
			new double[] {0.0, 0.0, collisionEntityBounds[5]};
		
		double[] collisionEntityPos = 
			TransformUtils.convertToCoordinateSystem(
					model, 
					positionRelativeToSource, 
					(PositionableEntity) collisionEntity, 
					(PositionableEntity) miterEntity, 
					true);
		
		if (leftSide) {
			
			spine[3] = (float)collisionEntityPos[0];
			
			spine[0] = spine[3] - 0.25f;
			spine[1] = spine[4];
			spine[2] = spine[5];

		} else {
			
			spine[6] = (float)collisionEntityPos[0];
			
			spine[9] = spine[6] + 0.25f;
			spine[10] = spine[7];
			spine[11] = spine[8];
		}
		
		// set the spline coords
        RulePropertyAccessor.setProperty(
                miterEntity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                spine);

	}
		
	/**
	 * The named vertex's position begins in the secondary entities coordinate 
	 * space.  This method will translate the vertex position from the secondary 
	 * entity's coordinate space to the primary entity's coordinate space.
	 * 
     * @param toEntity The primary entity where you want the position relative to
     * @param fromEntity The secondary entity where the position is 
	 * currently relative to
	 * @param vertexName The name of the secondary vertex 
	 * @return The position relative to the primary item
	 */
	private float[] getPointInLocalCoords(
            PositionableEntity toEntity, 
            PositionableEntity fromEntity, 
	        String vertexName) {
	     
        // get the vertex point for comparison 
        float[] point = 
            (float[])RulePropertyAccessor.getRulePropertyValue(fromEntity, vertexName);

        if (point == null || point.length < 2) {
            point = new float[] {0, 0};
        }
        
        // pad it with a 0 y-axis value
        float[] pos = new float[] {point[0], 0, point[1]};
        
        // now get the point in relative coords
        return getPointInLocalCoords(toEntity, fromEntity, pos);
                
 	}
	
	/**
     * The named vertex's position begins in the secondary entities coordinate 
     * space.  This method will translate the vertex position from the secondary 
     * entity's coordinate space to the primary entity's coordinate space.
     * 
     * @param toEntity The primary entity where you want the position relative to
     * @param fromEntity The secondary entity where the position is 
     * currently relative to
     * @param pos The position to translate
     * @return The position relative to the toEntity
     */
    private float[] getPointInLocalCoords(
            PositionableEntity toEntity, 
            PositionableEntity fromEntity, 
            float[] pos) {
         
        // convert vector to the entities space
        float[] scenePos = 
            TransformUtils.convertLocalCoordinatesToSceneCoordinates(
                    model, 
                    fromEntity, 
                    pos, 
                    true);   
        
        float[] localPos = 
            TransformUtils.convertSceneCoordinatesToLocalCoordinates(
                model, 
                scenePos, 
                toEntity, 
                true);
             
        return localPos;
        
    }

	
	/**
     * Check all possible children  of the entity for a miter cut object.  
     * Returns null if not found.
     * 
     * @param entity The entity to check its children
     * @return The first miter child found, or null if none are found
     */
    private Entity getMiterChild(Entity entity, boolean exact) {
        
        ArrayList<Entity> children;
        
        if (exact) {
            children = SceneHierarchyUtility.getExactChildren(entity);
        } else {
            children = entity.getChildren();
        }
            
        for (int i = 0; i < children.size(); i++) {
            Entity child = children.get(i);
            
            if (child == entity)
            	continue;
            
            // first check the entity is a miter entity
            Boolean canMiterCut = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        child,
                        ChefX3DRuleProperties.MITRE_CAN_CUT);

            if (canMiterCut) {
                return child;
            }
        }
        
        return null;

    }
    
    /**
     * Set the starting scale data for ScaleEntityCommand so the next scale
     * sequence will behave correctly.
     * 
     * @param entity Extrusion entity being scaled
     * @param scaleCmd ScaleEntityCommand acting on entity
     */
    private void setStartingScale(
    		PositionableEntity entity,
    		ScaleEntityCommand scaleCmd) {
    	
    	float[] spine = (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ExtrusionEntity.SPINE_VERTICES_PROP);
		
    	float[] finalScale = new float[3];
		float[] startingScale = new float[3];
		float[] size = new float[3];
		entity.getStartingScale(startingScale);
		entity.getSize(size);
		
		finalScale[0] = (spine[6] - spine[3])/size[0];
		finalScale[1] = 1.0f;
		finalScale[2] = 1.0f;

		// We have to be sneaky and set the starting scale to whatever
		// scale we end up with even though we cancel the scale command here
		// so that we can pick up where we left off correctly.
		SceneManagementUtility.changeProperty(
				model, 
				entity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				PositionableEntity.START_SCALE_PROP, 
				startingScale, 
				finalScale, 
				false);
		
		((PositionableEntity)entity).setStartingScale(finalScale);
    }
    
    /**
     * Calculate the spine balancing adjustment to apply to the position 
     * of the extrusion entity. Also issues change property commands for any
     * adjustments to the spine vertex positions.
     * 
     * @param entity Extrusion entity to evaluate
     * @param command Command acting on entity
     * @return Adjustment to apply to entity position, or null if not calcualted
     */
    private double[] getSpineBlancingAdjustment(
    		Entity entity,
    		Command command) {
    	
    	double[] adjustment = new double[3];
    	
    	float[] spine= (float[])
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ExtrusionEntity.SPINE_VERTICES_PROP);
    	
    	// Get the farthest left spine vertex and the farthest right.
    	// Check to see what the imbalance is and pass that back in the 
    	// double array of position adjustments to apply.
    	
    	double farLeft = spine[3];
    	double farRight = spine[6];
    	
    	double midPoint = (farRight + farLeft) / 2.0;
    	
    	spine[0] -= midPoint;
    	spine[3] -= midPoint;
    	spine[6] -= midPoint;
    	spine[9] -= midPoint;
    	
    	// set the spline coords
        RulePropertyAccessor.setProperty(
                entity, 
                Entity.DEFAULT_ENTITY_PROPERTIES, 
                ExtrusionEntity.SPINE_VERTICES_PROP, 
                spine);
        
        // Set the changes as commands and property values
        float[] oldSpineValue = (float[]) 
			RulePropertyAccessor.getRulePropertyValue(
				entity, 
				ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD);
        
        if (!command.isTransient()) {
			
			float[] newSpineValue = new float[spine.length];
			for (int i = 0; i < spine.length; i++) {
				newSpineValue[i] = spine[i];
			}
	    	
	    	SceneManagementUtility.changeProperty(
					model, 
					entity, 
					Entity.DEFAULT_ENTITY_PROPERTIES, 
					ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD, 
					oldSpineValue, 
					newSpineValue, 
					command.isTransient());
		
		}
		
		float[] newOldSpine = new float[spine.length];
		for (int i = 0; i < spine.length; i++) {
			newOldSpine[i] = spine[i];
		}
    
	    SceneManagementUtility.changeProperty(
				model, 
				entity, 
				Entity.DEFAULT_ENTITY_PROPERTIES, 
				ExtrusionEntity.SPINE_VERTICES_PROP, 
				oldSpineValue, 
				newOldSpine, 
				command.isTransient());
	    
	    // Set the miter line points for things like crown molding to use.
	    float[] mitreLineBackLeft = new float[2];
	    mitreLineBackLeft[0] = newOldSpine[3];
	    mitreLineBackLeft[1] = newOldSpine[5];
        
	    RulePropertyAccessor.setRuleProperty(
	    		entity, 
	    		ChefX3DRuleProperties.MITRE_LINE_BACK_LEFT, 
	    		mitreLineBackLeft);
        
	    float[] mitreLineFrontLeft = new float[2];
	    mitreLineFrontLeft[0] = newOldSpine[3];
	    mitreLineFrontLeft[1] = newOldSpine[5];
        
	    RulePropertyAccessor.setRuleProperty(
	    		entity, 
	    		ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT, 
	    		mitreLineFrontLeft);
        
	    float[] mitreLineFrontRight = new float[2];
	    mitreLineFrontRight[0] = newOldSpine[6];
	    mitreLineFrontRight[1] = newOldSpine[8];
	    
        RulePropertyAccessor.setRuleProperty(
	    		entity, 
	    		ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT, 
	    		mitreLineFrontRight);
        
        float[] mitreLineBackRight = new float[2];
	    mitreLineBackRight[0] = newOldSpine[6];
	    mitreLineBackRight[1] = newOldSpine[8];
        
	    RulePropertyAccessor.setRuleProperty(
	    		entity, 
	    		ChefX3DRuleProperties.MITRE_LINE_BACK_RIGHT, 
	    		mitreLineBackRight);
	    
	    adjustment[0] = midPoint;
	    adjustment[1] = 0.0;
	    adjustment[2] = 0.0;
	    
	    return adjustment;
    }

	
	/**
	 * Check to ensure a valid command is being processed
	 *
	 * @param command The command being validated
	 * @return True if valid to continue, false otherwise
	 */
	private boolean validateCommand(Entity entity, Command command) {

	    boolean valid = false;
	    
        // check the entity is a miter entity
        Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        //  check the entity is a miter entity
        Boolean hasMiterLine = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.MITRE_HAS_LINE);
        
        Boolean autoMiterLine = (Boolean)
        	RulePropertyAccessor.getRulePropertyValue(
        			entity, 
        			ChefX3DRuleProperties.MITRE_AUTO);
                
        // now check the command is one we need to case for
        if (command instanceof AddEntityChildCommand) {
            
            if (canMiterCut) {
                valid = true;
            }
            
        } else if (command instanceof RemoveEntityChildCommand) {
            
            if (canMiterCut || hasMiterLine) {
                valid = true;
            }
            
        } else if (command instanceof MoveEntityCommand || 
        		command instanceof MoveEntityTransientCommand ||
                command instanceof TransitionEntityChildCommand ) {
            
            if (canMiterCut || hasMiterLine) {
                valid = true;
            }
             
        } else if (command instanceof ScaleEntityCommand ||
        		command instanceof ScaleEntityTransientCommand) {
        
        	if (canMiterCut || autoMiterLine) {
        		valid = true;
        	}
        	
        }

	    return valid;

	}

	/**
	 * Calculate the intersection point between two lines. Assumes the lines 
	 * are co-planar and they are in the same coordinate system.
	 * 
	 * @param aStartPoint Start point for ray A.
	 * @param aVec Vector of ray A.
	 * @param bStartPoint Start point for ray B.
	 * @param bVec Vector of ray B.
	 * @return Point of intersection or null if none exists.
	 */
	private float[] getIntersectionPoint(
			float[] aStartPoint, 
			float[] aVec, 
			float[] bStartPoint, 
			float[] bVec) {
		
		float[] intersectionPoint = new float[3];
		
		//----------------------------------------------------------------------
		// Note, we are going to flatten on the y in all cases here.
		// If the difference in height is too great, we will cancel the
		// calculation and return null.
		//----------------------------------------------------------------------
		if (DISTANCE_THRESHOLD < Math.abs(aVec[1] - bVec[1])) {
			return null;
		}

		// Get the slopes, and check for division by zero cases.
		if (Math.abs(aVec[0]) < EPSILON && Math.abs(bVec[0]) < EPSILON) {
			
			return null;
			
		} else if (Math.abs(aVec[0]) < EPSILON) {
			
			// calculate the b slope
			float bSlope = bVec[2]/bVec[0];
			
			intersectionPoint[0] = aStartPoint[0];
			intersectionPoint[1] = aStartPoint[1]+aVec[1];
			intersectionPoint[2] = bSlope * aStartPoint[0] + bStartPoint[2];
			
			return intersectionPoint;
			
		} else if (Math.abs(bVec[0]) < EPSILON) {

			// calculate the a slope
			float aSlope = aVec[2]/aVec[0];
			
			intersectionPoint[0] = bStartPoint[0];
			intersectionPoint[1] = bStartPoint[1]+bVec[1];
			intersectionPoint[2] = aSlope * bStartPoint[0] + aStartPoint[2];
			
			return intersectionPoint;
			
		}
		
		// Since we are flattening on the y, and are dealing with a top down
		// orientation we know that the +Z axis is going to point in the -y
		// direction. We need to correct this for all our values before we
		// begin to perform our calculations.
		aStartPoint[2] *= -1;
		bStartPoint[2] *= -1;
		aVec[2] *= -1;
		bVec[2] *= -1;
		
		// Do standard calculation to determine intersection point.
		float aSlope = aVec[2]/aVec[0];
		float bSlope = bVec[2]/bVec[0];
		
		// If aSlope and bSlope are the same, then we have parallel lines.
		// There will be no point of intersection.
		if (0.0001 > Math.abs(aSlope - bSlope)) {
			return null;
		}
		
		// Y = mx + b is the equation of the line we will use.
		// (WE ARE FLATTENING ON THE Y AXIS...)
		// We will end up with two equations:
		// So for equation A we have ... Y = aSlope * x + b 
		// where b = aStartPoint[z] - (aSlope * aStartPoint[x]). 
		// For equation B we have .... Y = bSlope * x + b
		// where b = bStartPoint[z] - (bSlope * bStartPoint[x]).
		// We then solve the two equations first for x, and then for y.
		
		float aB = aStartPoint[2] - (aSlope * aStartPoint[0]);
		float bB = bStartPoint[2] - (bSlope * bStartPoint[0]);
		
		float x = (bB - aB) / (aSlope - bSlope);
		float y = aSlope * x + aB;
		
		intersectionPoint[0] = x;
		
		intersectionPoint[1] = aStartPoint[1];
		
		intersectionPoint[2] = -y;
		
		return intersectionPoint;
	}
}
