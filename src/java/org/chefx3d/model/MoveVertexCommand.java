/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
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

//External Imports
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Arrays;

import org.w3c.dom.*;

//Internal Imports
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A command for moving a vertex.
 *
 * @author Alan Hudson
 * @version $Revision: 1.17 $
 */
public class MoveVertexCommand implements Command, RuleDataAccessor {

    /** The model */
    private BaseWorldModel model;

    /** The entityID */
    private VertexEntity entity;

    /** The vertexID */
    private int vertexID;

    /** The starting position */
    private double[] startPos;

    /** The ending position */
    private double[] endPos;

    /** The position */
    private double[] pos;

    /** Is this a local add */
    private boolean local;

    /** The transactionID */
    private int transactionID;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;
    
    /** Should the command die */
    private boolean shouldDie = false;

    /**
     * Move a vertex entity.
     *
     * @param model WorldModel
     * @param transID The transactionID
     * @param entity The entity
     * @param endPosition The end position in world coordinates(meters, Y-UP,
     *        X3D System).
     * @param startPosition The start position in world coordinates(meters,
     *        Y-UP, X3D System).
     */
    public MoveVertexCommand(
    		WorldModel model,
            int transID,
            VertexEntity entity,
            double[] endPosition,
            double[] startPosition) {

        // Cast to package definition to access protected methods
    	this.model = (BaseWorldModel)model;
        this.entity = entity;
        this.transactionID = transID;

        this.pos = new double[3];

        this.startPos = new double[3];
        this.startPos[0] = startPosition[0];
        this.startPos[1] = startPosition[1];
        this.startPos[2] = startPosition[2];

        this.endPos = new double[3];
        this.endPos[0] = endPosition[0];
        this.endPos[1] = endPosition[1];
        this.endPos[2] = endPosition[2];

        local = true;

        init();
    }

    public MoveVertexCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;

        init();
    }

    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "MoveVertex -> " + vertexID;

        undoableState = true;
        transientState = false;
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
     * Is the command locally generated.
     *
     * @return Is local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        // make the final move of the entity
        this.pos[0] = endPos[0];
        this.pos[1] = endPos[1];
        this.pos[2] = endPos[2];

        entity.setPosition(pos, transientState);
        entity.setStartingPosition(pos);
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        // return the entity to the starting location
        this.pos[0] = startPos[0];
        this.pos[1] = startPos[1];
        this.pos[2] = startPos[2];

        entity.setPosition(pos, transientState);
        entity.setStartingPosition(pos);
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        execute();
    }

    /**
     * Get the text description of this <code>Command</code>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the text description of this <code>Command</code>.
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Get the state of this <code>Command</code>.
     */
    public boolean isTransient() {
        return transientState;
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
     * Get the undo setting of this <code>Command</code>. true =
     * <code>Command</code> may be undone false = <code>Command</code> may
     * never undone
     */
    public boolean isUndoable() {
        return undoableState;
    }

    /**
     * Serialize this command.
     *
     * @param method What method should we use
     * @param os The stream to output to
     */
    public void serialize(int method, OutputStream os) {
        switch (method) {
        case METHOD_XML:
            /*
             * <MoveEntityCommand entityID='1' px='' py='' pz='' cornerType='' />
             */

            //TODO: need to pass both the segment entity ID and the vertex ID
            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<MoveVertexCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' tID='");
            sbuff.append(transactionID);
            sbuff.append("' vertexID='");
            sbuff.append(vertexID);
            sbuff.append("' px='");
            sbuff.append(pos[0]);
            sbuff.append("' py='");
            sbuff.append(pos[1]);
            sbuff.append("' pz='");
            sbuff.append(pos[2]);
            sbuff.append("' />");

            String st = sbuff.toString();

            PrintStream ps = new PrintStream(os);
            ps.print(st);
            break;
        case METHOD_XML_FAST_INFOSET:
            errorReporter.messageReport("Unsupported serialization method");
            break;
        }
    }

    /**
     * Deserialize a stream
     *
     * @param st The xml string to deserialize
     */
    public void deserialize(String st) {
        Document doc = DOMUtils.parseXML(st);

        Element e = (Element) doc.getFirstChild();

        String d;

        pos = new double[3];
        endPos = new double[3];
        startPos = new double[3];

        d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        endPos[0] = pos[0];
        endPos[1] = pos[1];
        endPos[2] = pos[2];

        int entityID = Integer.parseInt(e.getAttribute("entityID"));

        //TODO: need to pass both the segment entity ID and the vertex ID
        vertexID = Integer.parseInt(e.getAttribute("vertexID"));
        transactionID = Integer.parseInt(e.getAttribute("tID"));

        local = false;
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
     * Gets the start position. If the array passed in is not length 3 then
     * the array is returned null.
     *  
     * @param pos xyz indexed array to be set
     */
    public void getStartPosition(double[] pos){
    	
    	if(pos.length != 3){
    		pos = null;
    		return;
    	}
    	
    	pos[0] = startPos[0];
    	pos[1] = startPos[1];
    	pos[2] = startPos[2];
    }
    
    /**
     * Sets the start position for the command.
     * 
     * @param pos xyz indexed array with values to set
     */
    public void setStartPosition(double[] pos){
    	
    	startPos[0] = pos[0];
    	startPos[1] = pos[1];
    	startPos[2] = pos[2];
    }
    
    /**
     * Gets the end position. If the array passed in is not length 3 then
     * the array is returned null.
     *  
     * @param pos xyz indexed array to be set
     */
    public void getEndPosition(double[] pos){
    	
    	if(pos.length != 3){
    		pos = null;
    		return;
    	}
    	
    	pos[0] = endPos[0];
    	pos[1] = endPos[1];
    	pos[2] = endPos[2];
    }
    
    /**
     * Sets the end position for the command.
     * 
     * @param pos xyz indexed array with values to set
     */
    public void setEndPosition(double[] pos){
    	
    	endPos[0] = pos[0];
    	endPos[1] = pos[1];
    	endPos[2] = pos[2];
    }

    /**
     * Gets the position. If the array passed in is not length 3 then
     * the array is returned null.
     *  
     * @param pos xyz indexed array to be set
     */
    public void getPosition(double[] pos){
    	
    	if(pos.length != 3){
    		pos = null;
    		return;
    	}
    	
    	pos[0] = this.pos[0];
    	pos[1] = this.pos[1];
    	pos[2] = this.pos[2];
    }
    
    /**
     * Sets the position for the command.
     * 
     * @param pos xyz indexed array with values to set
     */
    public void setPosition(double[] pos){
    	
    	this.pos[0] = pos[0];
    	this.pos[1] = pos[1];
    	this.pos[2] = pos[2];
    } 
    
	/**
	 * Get the Entity
	 * 
	 * @return Entity or null if it doesn't exist
	 */
	public Entity getEntity(){
		return entity;
	}
	
	/**
	 * Get the WorldModel
	 * 
	 * @return WorldModel or null if it doesn't exist
	 */
	public WorldModel getWorldModel(){
		return model;
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
        endPos[0] = startPos[0];
        endPos[1] = startPos[1];
        endPos[2] = startPos[2];
        
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
		
		if (externalCommand instanceof MoveVertexCommand) {
			
			if (((MoveVertexCommand)externalCommand).getTransactionID() == 
				this.transactionID){
				
				return true;
				
			}
			
			double[] startPosition = new double[3];
			double[] endPosition = new double[3];
			
			((MoveVertexCommand)externalCommand).getEndPosition(
					endPosition);
			((MoveVertexCommand)externalCommand).getStartPosition(
					startPosition);
			
			if (((MoveVertexCommand)externalCommand).getEntity() != 
				this.entity) {
				
				return false;
				
			} else if (!Arrays.equals(startPosition, this.startPos)) {
				
				return false;
				
			} else if (!Arrays.equals(endPosition, this.endPos)) {
				
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