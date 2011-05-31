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
import java.util.*;

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
 * @version $Revision: 1.28 $
 */
public class AddVertexCommand implements Command, RuleDataAccessor {

    /** The entityID */
    private SegmentableEntity entity;

    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;
    
    /** The vertex */
    VertexEntity vertex;

    /** Is this a local add */
    private boolean local;

    /** The index order of the vertex for lines */
    private int index;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /**   Boolean that says whether 
     * the vertex was either a single vertex
     *  or part of a wall */
    private boolean singleVertexFlag;
    
    /** Should the command die */
    private boolean shouldDie = false;
    
    /**
     * Add a segment to an entity.
     *
     * @param entity - The SegmentableEntity to add a vertex to
     * @param vertexID - The unique ID of the vertex
     * @param position - The position in world coordinates(meters, Y-UP, X3D
     *        System).
     * @param index - The position in the vertex list to add this to, -1 is append
     * @param vertexProps - The list of user defined properties
     */
    public AddVertexCommand(
            SegmentableEntity entity, 
            VertexEntity vertex,
            int index, 
            boolean isUndoable) {
        
        
        // Cast to package definition to access protected methods
        this.entity = entity;        
        this.vertex = vertex;
        this.index = index;
        undoableState = isUndoable;
        
        singleVertexFlag = false;
        local = true;

        init();

    }
    
    /**
     * Add a segment to an entity.
     *
     * @param entity - The SegmentableEntity to add a vertex to
     * @param vertexID - The unique ID of the vertex
     * @param position - The position in world coordinates(meters, Y-UP, X3D
     *        System).
     * @param index - The position in the vertex list to add this to, -1 is append
     * @param vertexProps - The list of user defined properties
     */
    public AddVertexCommand(
            SegmentableEntity entity, 
            VertexEntity vertex,
            int index) {
        
        this(entity, vertex, index, true);
        
    }
    
    /**
     * Add a segment to an entity.
     *
     * @param entityId The entity to change
     */
    public AddVertexCommand(int entityId) {
        // Cast to package definition to access protected methods
        //this.entity = entity;        

        init();
        undoableState = true;
    }


    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "AddVertex -> " + vertex.getEntityID();

        transientState = false;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        
        index = entity.addVertex(vertex, index);
        vertex.setParentEntityID(entity.getEntityID());
                       
        double[] pos = new double[3];
        vertex.getPosition(pos);
        vertex.setStartingPosition(pos);
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
        entity.removeVertex(vertex.getEntityID());
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
     * Get the transient state of this <code>Command</code>.
     */
    public boolean isTransient() {
        return transientState;
    }

    /**
     * Set the transient state of this <code>Command</code>.
     */
    public void setTransient(boolean bool) {
        transientState = bool;
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
             * <AddVertexCommand entityID='' vertexID=''
             *  px='' py='' pz='' />
             */
        	double[] pos = new double[3];
        	vertex.getPosition(pos);
        	
            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<AddVertexCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' vertexID='");
            sbuff.append(vertex.getEntityID());
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

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        int vertexID = Integer.parseInt(e.getAttribute("vertexID"));

        String d;
        double[] pos = new double[3];
        d = e.getAttribute("px");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("py");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("pz");
        pos[2] = Double.parseDouble(d);

        local = false;
        
        HashMap<String, Map<String, Object>> defaultProps = new HashMap<String, Map<String, Object>>();
        HashMap<String, Object> defaultParams = new HashMap<String, Object>();
        defaultParams.put(VertexEntity.POSITION_PROP, pos);
        defaultProps.put(VertexEntity.DEFAULT_ENTITY_PROPERTIES, defaultParams);
        vertex = new VertexEntity(vertexID, defaultProps);

        init();
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
     * Get the SegmentableEntity vertex will be added to
     * 
     * @return SegmentableEntity
     */
    public SegmentableEntity getSegmentableEntity(){
    	
    	return entity;
    }
    
    public void setSingleVertexFlag(boolean flag) {
        singleVertexFlag = flag;
    }
    
    public boolean getSingleVertexFlag() {
        return singleVertexFlag;
    }

    //---------------------------------------------------------------
    // Methods required by rule data accessor
    //---------------------------------------------------------------
	
    /**
     * Get the VertexEntity
     * 
     * @return Entity
     */
    public Entity getEntity() {

		return vertex;
	}

    /**
     * Get the world model
     * 
     * @return WorldModel
     */
	public WorldModel getWorldModel() {

		return null;
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
		
		if (externalCommand instanceof AddVertexCommand) {
		
			if (((AddVertexCommand)externalCommand).getEntity() != 
				this.vertex) {
				
				return false;
				
			} else if (((AddVertexCommand)externalCommand).
					getSegmentableEntity() != this.entity) {
				
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