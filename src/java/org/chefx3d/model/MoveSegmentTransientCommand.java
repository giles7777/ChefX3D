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

package org.chefx3d.model;

// External Import

// Local Import
import java.util.HashSet;
import java.util.Arrays;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for moving segments.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.8 $
 */
public class MoveSegmentTransientCommand implements Command, DeadReckonedCommand, RuleDataAccessor {

	/** The model */
    private BaseWorldModel model;
    
	/** SegmentEntity to move */
	private SegmentEntity segmentEntity;
	
    /** The description of the <code>Command</code> */
    private String description;
    
    /** The flag to indicate transient status */
    private boolean transientState;
    
    /** The flag to indicate undoable status */
    private boolean undoableState;
    
    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** Is this a local add */
    private boolean local;
    
    /** The transactionID */
    private int transactionID;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;
    
    /** Start vertex's position to move to */
    private double[] startVertexEndPosition = new double[3];
    
    /** Start vertex's starting positon */
    private double[] startVertexStartPosition = new double[3];
    
    /** End vertex's positon to move to */
    private double[] endVertexEndPosition = new double[3];
    
    /** End verex's starting position */
    private double[] endVertexStartPosition = new double[3];
    
    /** Should the command die */
    private boolean shouldDie = false;
    
    
    /**
     * Default constructor
     * 
     * @param model
     * @param transID
     * @param entity
     * @param startVertexEndPosition
     * @param startVertexStartPosition
     * @param endVertexEndPosition
     * @param endVertexStartPosition
     */
    public MoveSegmentTransientCommand(
    		WorldModel model,
    		int transID,
            SegmentEntity entity,
            double[] startVertexEndPosition,
            double[] startVertexStartPosition,
            double[] endVertexEndPosition,
            double[] endVertexStartPosition){
    
    	this.model = (BaseWorldModel) model;
    	this.transactionID = transID;
    	this.segmentEntity = entity;
    	
    	this.startVertexEndPosition[0] = startVertexEndPosition[0];
    	this.startVertexEndPosition[1] = startVertexEndPosition[1];
    	this.startVertexEndPosition[2] = startVertexEndPosition[2];
    	
    	this.startVertexStartPosition[0] = startVertexStartPosition[0];
    	this.startVertexStartPosition[1] = startVertexStartPosition[1];
    	this.startVertexStartPosition[2] = startVertexStartPosition[2];
    	
    	this.endVertexEndPosition[0] = endVertexEndPosition[0];
    	this.endVertexEndPosition[1] = endVertexEndPosition[1];
    	this.endVertexEndPosition[2] = endVertexEndPosition[2];
    	
    	this.endVertexStartPosition[0] = endVertexStartPosition[0];
    	this.endVertexStartPosition[1] = endVertexStartPosition[1];
    	this.endVertexStartPosition[2] = endVertexStartPosition[2];
    	
    	init();
    }
    
    /**
     * Common initialization method.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        transientState = true;
        undoableState = false;
    }
    
    //---------------------------------------------------------------
    // Methods required by Command
    //---------------------------------------------------------------
	
	public void execute() {
		VertexEntity startVertexEntity = segmentEntity.getStartVertexEntity();
		startVertexEntity.setPosition(startVertexEndPosition, transientState);

		VertexEntity endVertexEntity = segmentEntity.getEndVertexEntity();
		endVertexEntity.setPosition(endVertexEndPosition, transientState);
	}

    /**
     * Get the text description of this <code>Command</code>.
     */
	public String getDescription() {

		return description;
	}

    /**
     * Get the transactionID for this command.
     *
     * @return The transactionID
     */
	public int getTransactionID() {

		return transactionID;
	}

    /**
     * Is the command locally generated.
     *
     * @return Is local
     */
	public boolean isLocal() {

		return local;
	}

    /**
     * Get the state of this <code>Command</code>.
     */
	public boolean isTransient() {

		return transientState;
	}

    /**
     * Get the undo setting of this <code>Command</code>. true =
     * <code>Command</code> may be undone false = <code>Command</code> may
     * never undone
     */
	public boolean isUndoable() {

		return undoableState;
	}

    /**
     * Redo the affects of this command.
     */
	public void redo() {
		// Ignore
	}

    /**
     * Set the text description of this <code>Command</code>.
     */
	public void setDescription(String desc) {
		
		description = desc;
	}

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
	public void setErrorReporter(ErrorReporter reporter) {
		
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
	}

    /**
     * Set the local flag.
     *
     * @param isLocal Is this a local update
     */
	public void setLocal(boolean isLocal) {
		
		local = isLocal;
	}

    /**
     * Undo the affects of this command.
     */
	public void undo() {
		// ignore
	}

	//---------------------------------------------------------------
	// Methods required by RuleDataAccessor
	//---------------------------------------------------------------
	
	/**
	 * Get the Entity
	 * 
	 * @return Entity or null if it doesn't exist
	 */
	public Entity getEntity() {
		// TODO Auto-generated method stub
		return segmentEntity;
	}

	/**
	 * Get the WorldModel
	 * 
	 * @return WorldModel or null if it doesn't exist
	 */
	public WorldModel getWorldModel() {

		return model;
	}

	//---------------------------------------------------------------
	// Methods required by DeadReckoning
	//---------------------------------------------------------------
	
	public void getDeadReckoningParams(double[] position, float[] orientation,
			float[] velocity, float[] velocity2) {
		// TODO Auto-generated method stub
		
	}

	public void setDeadReckoningParams(double[] position, float[] orientation,
			float[] velocity, float[] velocity2) {
		// TODO Auto-generated method stub
		
	}

	//---------------------------------------------------------------
	// Local methods
	//---------------------------------------------------------------
	
	/**
	 * Get the start vertex end position
	 * 
	 * @param pos Returns position
	 */
	public void getStartVertexEndPosition(double[] pos) {
		
		pos[0] = startVertexEndPosition[0];
		pos[1] = startVertexEndPosition[1];
		pos[2] = startVertexEndPosition[2];
	}
	
	/**
	 * Set the start vertex end position
	 * 
	 * @param pos New position
	 */
	public void setStartVertexEndPosition(double[] pos) {
		
		startVertexEndPosition[0] = pos[0];
		startVertexEndPosition[1] = pos[1];
		startVertexEndPosition[2] = pos[2];
	}
	
	/**
	 * Get the end vertex end position
	 * 
	 * @param pos Returns position
	 */
	public void getEndVertexEndPosition(double[] pos) {
		
		pos[0] = endVertexEndPosition[0];
		pos[1] = endVertexEndPosition[1];
		pos[2] = endVertexEndPosition[2];
	}

	/**
	 * Set the end vertex end position
	 * @param pos New position
	 */
	public void setEndVertexEndPosition(double[] pos) {
		
		endVertexEndPosition[0] = pos[0];
		endVertexEndPosition[1] = pos[1];
		endVertexEndPosition[2] = pos[2];
	}
	
	/**
	 * get the start vertex start position
	 * 
	 * @param pos Returns position
	 */
	public void getStartVertexStartPosition(double[] pos){
		
		pos[0] = startVertexStartPosition[0];
		pos[1] = startVertexStartPosition[1];
		pos[2] = startVertexStartPosition[2];
	}
	
	/**
	 * Get the end vertex start position
	 * 
	 * @param pos Returns position
	 */
	public void getEndVertexStartPosition(double[] pos){
		
		pos[0] = endVertexStartPosition[0];
		pos[1] = endVertexStartPosition[1];
		pos[2] = endVertexStartPosition[2];
	}

	public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

    public void resetToStart() {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * Set the die state of the command. Setting this to true will
     * only cause the command to die if the rule engine execution
     * returns false.
     * 
     * @param die True to have command die and not execute
     */
	public void setCommandShouldDie(boolean die) {
		
		shouldDie = die;
	}

	/**
	 * Get the die value of the command.
	 * 
	 * @return True to have command die, false otherwise
	 */
	public boolean shouldCommandDie() {

		return shouldDie;
	}
	
	/**
	 * Compare external command to this one to see if they are the same.
	 * 
	 * @param externalCommand command to compare against
	 * @return True if the same, false otherwise
	 */
	public boolean isEqualTo(Command externalCommand) {
		
		if (externalCommand instanceof MoveSegmentTransientCommand) {
		
			if (((MoveSegmentTransientCommand)externalCommand).
					getTransactionID() == this.transactionID) {
				
				return true;
				
			}
			
			double[] endVertexEndPos = new double[3];
			double[] endVertexStartPos = new double[3];
			double[] startVertexEndPos = new double[3];
			double[] startVertexStartPos = new double[3];
			
			((MoveSegmentTransientCommand)externalCommand).
				getEndVertexEndPosition(endVertexEndPos);
			((MoveSegmentTransientCommand)externalCommand).
				getEndVertexStartPosition(endVertexStartPos);
			((MoveSegmentTransientCommand)externalCommand).
				getStartVertexEndPosition(startVertexEndPos);
			((MoveSegmentTransientCommand)externalCommand).
				getStartVertexStartPosition(startVertexStartPos);
			
			if (((MoveSegmentTransientCommand)externalCommand).getEntity() != 
				this.segmentEntity) {
				
				return false;
				
			} else if (!Arrays.equals(
					endVertexEndPos, 
					this.endVertexEndPosition)) {
				
				return false;
				
			} else if (!Arrays.equals(
					endVertexStartPos, 
					this.endVertexStartPosition)) {
				
				return false;
				
			} else if (!Arrays.equals(
					startVertexEndPos, 
					this.startVertexEndPosition)) {
				
				return false;
				
			} else if (!Arrays.equals(
					startVertexStartPos, 
					this.startVertexStartPosition)) {
				
				return false;
				
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Override object's equals method
	 */
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof Command) {
			return isEqualTo((Command)obj);
		}
		
		return false;
	}
}
