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
 * @version $Revision: 1.42 $
 */
public class AddSegmentCommand implements Command, RuleDataAccessor {

    /** The model */
    private WorldModel model;

    /** The SegmentableEntity */
    private SegmentableEntity entity;
    
    private SegmentEntity segment;
    
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
        
    public AddSegmentCommand(
            WorldModel model,
            SegmentableEntity entity,
            SegmentEntity segment) {
        
        this(model, entity, segment, true);
        
    }
    
    public AddSegmentCommand(
            WorldModel model,
            SegmentableEntity entity,
            SegmentEntity segment, 
            boolean isUndoable) {
        
        this.entity = entity;
        this.segment = segment;
        this.model = model;
        undoableState = isUndoable;
    }

    /**
     * Add a segment to an entity.
     *
     * @param model The model to change
     */
    public AddSegmentCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model =  model;
        undoableState = true;
        init();
    }


    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "AddSegment -> " + segment.getEntityID();
        transientState = false;
    }

    /**
     * Execute the command.
     */
    public void execute() { 
        if(entity instanceof BaseSegmentableEntity)
            ((BaseSegmentableEntity)entity).setCurrentSegmentDesc(segment);
        entity.addSegment(segment);
        segment.setParentEntityID(entity.getEntityID());
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
        entity.removeSegment(segment.getEntityID());
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
             * <AddSegmentCommand entityID='' vertexID=''
             *  px='' py='' pz='' />
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<AddSegmentCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' segmentID='");
            sbuff.append(segment.getEntityID());
            sbuff.append("' startVertexID='");
            sbuff.append(segment.getStartVertexEntity().getEntityID());
            sbuff.append("' endVertexID='");
            sbuff.append(segment.getEndVertexEntity().getEntityID());
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
        entity = (SegmentableEntity)model.getEntity(entityID);
        
        int segmentID = Integer.parseInt(e.getAttribute("segmentID"));
        int startVertexID = Integer.parseInt(e.getAttribute("startVertexID"));
        int endVertexID = Integer.parseInt(e.getAttribute("endVertexID"));
        
        HashMap<String, Map<String, Object>> defaultProperties = new HashMap<String, Map<String, Object>>();
        HashMap<String, Object> segmentProps = new HashMap<String, Object>();
        //segmentProps.put(SegmentEntity.START_INDEX_PROP, startVertexID);
        //segmentProps.put(SegmentEntity.END_INDEX_PROP, endVertexID);
        
        // Chris - TODO: This was being hard set in the original constructor
        // 				 so I am setting it here...  I know not what I do.
        segmentProps.put(SegmentEntity.EXTERIOR_SEGMENT_PROP, true);
        
        defaultProperties.put(SegmentEntity.DEFAULT_ENTITY_PROPERTIES, segmentProps);
        
        segment = new SegmentEntity(segmentID, defaultProperties);
        

        local = false;

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

    
    public SegmentableEntity getSegmentableEntity() {
        return entity;
    }
    
    public Entity getEntity() {
        // TODO Auto-generated method stub
        return segment;
    }

    public WorldModel getWorldModel() {
        // TODO Auto-generated method stub
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
		
		if (externalCommand instanceof AddSegmentCommand) {
		
			if (((AddSegmentCommand)externalCommand).getEntity() != 
				this.segment) {
				
				return false;
				
			} else if (((AddSegmentCommand)externalCommand).
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