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

package org.chefx3d.rules.util;

//External imports

//Internal imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.ui.PopUpMessage;
import org.chefx3d.view.common.EditorView;
import org.j3d.util.I18nManager;

/**
* Utility wrapper for converting miter cut adjustments into auto add operations
* that will play well with the miter cut products.
*
* @author Ben Yarger
* @version $Revision: 1.5 $
*/
public abstract class AutoAddMiterWrapper {

	/*
	 * Basically, we will be removing all auto adds, every time, and then on 
	 * non transient commands we will perform an auto add based on a move, never
	 * a scale type operation. If the command is an add, we will process as and
	 * add. Scaling auto add for miter breaks things too badly to be handled.
	 */
	
	/** Cannot add because of auto add collision message */
    private static final String POP_UP_NO_ADD =
        "org.chefx3d.rules.definitions.AddAutoAddRule.addCanceled";
    
    /** Cannot move because of auto add collision message */
    private static final String POP_UP_NO_MOVE =
        "org.chefx3d.rules.definitions.MoveAutoAddRule.moveCanceled";
    
    /** Displays a pop up message */
    private static PopUpMessage popUpMessage = PopUpMessage.getInstance();
    
    /** Translation utility */
    private static I18nManager intl_mgr = I18nManager.getManager();
    
    
	//--------------------------------------------------------------------------
    // Public methods
    //--------------------------------------------------------------------------
    
	public static boolean processMiterAutoAdd(
			WorldModel model,
			PositionableEntity miterEntity,
			Command command,
			RuleCollisionHandler rch,
			EditorView view) {
		
		// Only operate on miter entities
		Boolean canMiterCut = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    miterEntity,
                    ChefX3DRuleProperties.MITRE_CAN_CUT);
        
        if (!canMiterCut) {
        	return true;
        }
        
        // If the entity doesn't perform any auto add operations, bail
        if (!AutoAddUtility.performsAutoAddOperations(miterEntity)) {
        	return true;
        }
        
        // Clear all auto adds that aren't critical
        AutoAddUtility.removeNonCriticalAutoAdds(model, miterEntity, rch, view);
		
		// Process add, then non transient commands, then transient commands.
		if (command instanceof AddEntityChildCommand) {
			
			addCommandProcessing(
					model, 
					miterEntity, 
					(AddEntityChildCommand) command, 
					rch, 
					view);
			
		} else if (command instanceof MoveEntityCommand || 
				(command instanceof TransitionEntityChildCommand && 
						!command.isTransient())) {
			
			// Temporarily set the scale of the miterEntity so we can get the
			// correct evaluation.
			
			float[] newScale = new float[3];
			float[] oldScale = new float[3];
			miterEntity.getScale(oldScale);
			miterEntity.getStartingScale(newScale);
			
			miterEntity.setScale(newScale);
			
			moveCommandProcessing(model, miterEntity, command, rch, view);
			
			miterEntity.setScale(oldScale);
			
		} else if (command instanceof ScaleEntityCommand) {
			
			ScaleEntityCommand scaleCmd = (ScaleEntityCommand) command;
			
			double[] startPos = new double[3];
			double[] endPos = new double[3];
			
			scaleCmd.getOldPosition(startPos);
			scaleCmd.getNewPosition(endPos);
			
			MoveEntityCommand mvCmd = new MoveEntityCommand(
					model, 
					model.issueTransactionID(), 
					miterEntity, 
					endPos, 
					startPos);
			
			moveCommandProcessing(model, miterEntity, mvCmd, rch, view);

		}
		
		return true;
	}
	
	//--------------------------------------------------------------------------
	// Private methods
	//--------------------------------------------------------------------------
	
	/**
	 * Perform the add command processing.
	 * 
	 * @param model WorldModel to reference
	 * @param miterEntity Entity to process
	 * @param command AddEntityChildCommand to work with
	 * @param rch RuleCollisionHandler to reference
	 * @param view EditorView to reference
	 * @return True if successful, false otherwise
	 */
	private static boolean addCommandProcessing(
			WorldModel model,
			PositionableEntity miterEntity,
			AddEntityChildCommand command,
			RuleCollisionHandler rch,
			EditorView view) {
		
		EntityBuilder entityBuilder = view.getEntityBuilder();
		
		Entity parentEntity = 
			((AddEntityChildCommand)command).getParentEntity();
        
        double[] entityPosRelativeToZone = 
        	TransformUtils.getExactPositionRelativeToZone(
        			model,
        			miterEntity);
		
		// Add the surrogate to evaluate against
        SceneManagementUtility.addSurrogate(
           		rch.getRuleCollisionChecker(), command);

/*
  
  		These are to stay disabled until a future time when we can confirm
  		they work correctly.
        
        if (!AutoAddBySpanUtility.autoAddBySpan(
        		model, 
        		miterEntity, 
        		parentEntity, 
        		rch,
        		entityBuilder)) {
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
            
            SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);
        	
        	return false;
        }
        
        if (!AutoAddByCollisionUtility.autoAddByCollision(
        		model, 
        		command, 
        		miterEntity, 
        		parentEntity, 
        		entityPosRelativeToZone, 
        		rch, 
        		entityBuilder)) {
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
            
            SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);
        	
        	return false;
        }
        
        if (!AutoAddByPositionUtility.autoAddByPosition(
        		model, 
        		miterEntity, 
        		parentEntity, 
        		rch,
        		entityBuilder)) {
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
        	
            SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);
            
        	return false;
        }
*/        
        if (!AutoAddEndsUtility.addAutoAddEnds(
        		model, 
        		miterEntity, 
        		parentEntity, 
        		rch, 
        		entityBuilder)) {
        	
        	// Remove the surrogate added by this method for testing
        	SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);
        	
        	String msg = intl_mgr.getString(POP_UP_NO_ADD);
            popUpMessage.showMessage(msg);
        	
        	return false;
        }
        
        AutoAddInvisibleChildrenUtility.addInvisibleChildren(
        		model, 
        		miterEntity, 
        		entityBuilder, 
        		rch);
        
        // Remove the surrogate added by this method for testing
        SceneManagementUtility.removeSurrogate(
           		rch.getRuleCollisionChecker(), miterEntity);

        return true;
	}
	
	/**
	 * Perform the move command processing. Scales will be converted to a temp
	 * move command that will be evaluated here.
	 * 
	 * @param model WorldModel to reference
	 * @param miterEntity Entity to process
	 * @param command AddEntityChildCommand to work with
	 * @param rch RuleCollisionHandler to reference
	 * @param view EditorView to reference
	 * @return True if successful, false otherwise
	 */
	private static boolean moveCommandProcessing(
			WorldModel model,
			PositionableEntity miterEntity,
			Command command,
			RuleCollisionHandler rch,
			EditorView view) {
		
		// If not a MoveEntityCommand or TransitionEntityChildCommand that is
		// not transient, then bail
		if (!(command instanceof MoveEntityCommand) && 
				(!(command instanceof TransitionEntityChildCommand) && 
						command.isTransient())) {
			
			return false;
		}
		
		EntityBuilder entityBuilder = view.getEntityBuilder();
		
		PositionableEntity parentEntity = (PositionableEntity) 
			SceneHierarchyUtility.getExactParent(model, miterEntity);
		
		// Add the surrogate to evaluate against
        SceneManagementUtility.addSurrogate(
           		rch.getRuleCollisionChecker(), command);

/*
        
  		These are to stay disabled until a future time when we can confirm
  		they work correctly.
        
        if (!AutoAddBySpanUtility.moveAutoAdd(
        		model,
        		command,
        		miterEntity,
        		parentEntity,
        		rch,
        		entityBuilder)) {

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);
           	
           	SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);

           	return false;
        }

        if (!AutoAddByCollisionUtility.moveAutoAdd(
        		model,
        		command,
        		miterEntity,
        		parentEntity,
        		null,
        		rch,
        		entityBuilder)) {

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);
           	
           	SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);

           	return false;
        }

        if (!AutoAddByPositionUtility.moveAutoAdd(
        		model,
        		command,
        		miterEntity,
        		parentEntity,
        		rch,
        		entityBuilder)) {

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);
           	
           	SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);

           	return false;
        }
*/
        if (!AutoAddEndsUtility.moveAutoAdd(
        		model,
        		command,
        		miterEntity,
        		parentEntity,
        		rch,
        		entityBuilder)) {

           	String msg = intl_mgr.getString(POP_UP_NO_MOVE);
           	popUpMessage.showMessage(msg);
           	
           	// Remove the surrogate added by this method for testing
           	SceneManagementUtility.removeSurrogate(
               		rch.getRuleCollisionChecker(), miterEntity);

           	return false;
        }
        
       	// Remove the surrogate added by this method for testing
       	SceneManagementUtility.removeSurrogate(
           		rch.getRuleCollisionChecker(), miterEntity);

        return true;
	}

}
