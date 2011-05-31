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

// External Imports
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

//Internal Imports
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A command for adding a segment to an entity.
 *
 * @author Alan Hudson
 * @version $Revision: 1.19 $
 */
public class RemoveVertexCommand implements Command, RuleDataAccessor {

    /** The entityID */
    private SegmentableEntity entity;

    /** The vertexID */
    private int vertexID;

    /** The VertexEntity to remove */
    private VertexEntity vertex;
    
    /** The WorldModel to change */
    private WorldModel model;
    
    /** The index of the vertex for ordering */
    private int index;
    
    /** Is this a local add */
    private boolean local;

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
     * Add a segment to an entity.
     *
     * @param model The WorldModel to change
     * @param entity The segment entity
     * @param vertexID The vertex to be removed
     */
    public RemoveVertexCommand(
    		WorldModel model, 
    		SegmentableEntity entity, 
    		int vertexID) {
    	
        this(model, entity, vertexID, true);
    }
    
    /**
     * Add a segment to an entity.
     *
     * @param model The WorldModel to change
     * @param entity The segment entity
     * @param vertexID The vertex to be removed
     */
    public RemoveVertexCommand(
    		WorldModel model, 
    		SegmentableEntity entity, 
    		int vertexID, 
    		boolean isUndoable) {
               
        // Cast to package definition to access protected methods
    	this.model = model;
        this.entity = entity;
        this.vertexID = vertexID;
        undoableState = isUndoable;
        
        vertex = entity.getVertex(vertexID);     
        index = entity.getChildIndex(vertexID);
        
        local = true;

        init();

    }
    

    /**
     * Add a segment to an entity.
     *
     * @param model The model to change
     */
    public RemoveVertexCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        //this.model = (BaseWorldModel) model;

        init();
        undoableState = true;
    }


    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "RemoveVertex -> " + vertexID;
        transientState = true;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        entity.removeVertex(vertexID);
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
     * Undo the affects of this command.
     */
    public void undo() {
        entity.addVertex(vertex, index);
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
        return 0;
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
             * <RemoveVertexCommand entityID='' vertexID='' />
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<RemoveVertexCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' vertexID='");
            sbuff.append(vertexID);
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
        
//        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        vertexID = Integer.parseInt(e.getAttribute("vertexID"));

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

    public HashSet<String> getIgnoreRuleList() {

        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

    //---------------------------------------------------------------
    // Methods required by RuleDataAccessor
    //---------------------------------------------------------------
	
    /**
     * Get the vertex to be deleted
     */
    public Entity getEntity() {

		return vertex;
	}

    /**
     * Get the WorldModel
     */
	public WorldModel getWorldModel() {

		return model;
	}

	/**
	 * Reset the command to the starting state
	 */
	public void resetToStart() {
		// Nothing to reset for remove commands
		
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
		
		if (externalCommand instanceof RemoveVertexCommand) {
		
			if (((RemoveVertexCommand)externalCommand).getEntity() != 
				this.vertex) {
				
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