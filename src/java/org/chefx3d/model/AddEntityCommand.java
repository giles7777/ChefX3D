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

import org.w3c.dom.*;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DOMUtils;

/**
 * A command for adding a new entity.
 *
 * @author Alan Hudson
 * @version $Revision: 1.45 $
 */
public class AddEntityCommand implements Command, RuleDataAccessor {
    /** The model */
    private BaseWorldModel model;

    /** The entity */
    private Entity entity;

    /** Is this a local add */
    private boolean local;

    /** The position */
    private double[] pos;

    /** The rotation */
    private float[] rot;

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
     * Add an entity.
     *
     * @param model The model to change
     * @param entity The unique entity
     */
    public AddEntityCommand(WorldModel model, Entity entity) {

        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        this.entity = entity;

        pos = new double[3];
        rot = new float[4];
        if (entity instanceof PositionableEntity) {
            ((PositionableEntity)entity).getPosition(pos);
            ((PositionableEntity)entity).getRotation(rot);
        }

        description = "AddEntity -> " + entity.getName();

        local = true;

        init();
    }

    public AddEntityCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
    }

    /**
     * Common initialization.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        if (entity.getType() == Entity.TYPE_WORLD) {
            undoableState = false;
        } else {
            undoableState = true;
        }
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
        model.addEntity(local, entity, null);
        
        if(entity instanceof PositionableEntity){
        	((PositionableEntity)entity).setStartingPosition(pos);
        	((PositionableEntity)entity).setStartingRotation(rot);
        	
        	float[] startingScale = new float[3];
        	((PositionableEntity)entity).getScale(startingScale);
        	((PositionableEntity)entity).setStartingScale(startingScale);
        }
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        model.removeEntity(local, entity, null);
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
             * <AddEntityCommand entityID='1' px='' py='' pz='' rotx='' roty=''
             * rotz='' rota='' name='' />
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<AddEntityCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' px='");
            sbuff.append(pos[0]);
            sbuff.append("' py='");
            sbuff.append(pos[1]);
            sbuff.append("' pz='");
            sbuff.append(pos[2]);
            sbuff.append("' rotx='");
            sbuff.append(rot[0]);
            sbuff.append("' roty='");
            sbuff.append(rot[1]);
            sbuff.append("' rotz='");
            sbuff.append(rot[2]);
            sbuff.append("' rota='");
            sbuff.append(rot[3]);
            sbuff.append("' name='");
            sbuff.append(entity.getName());
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

//        Element e = (Element) doc.getFirstChild();
//        String toolName = e.getAttribute("name");
//
//        System.out.println("ToolName: " + toolName);
//        Tool tool = CatalogManager.getCatalogManager().findTool(toolName);
//
//        if (tool == null)
//            System.out.println("Cannot find tool: " + toolName);
//
//        local = false;
//
//        String d;
//
//        pos = new double[3];
//        d = e.getAttribute("px");
//        pos[0] = Double.parseDouble(d);
//        d = e.getAttribute("py");
//        pos[1] = Double.parseDouble(d);
//        d = e.getAttribute("pz");
//        pos[2] = Double.parseDouble(d);
//
//        rot = new float[4];
//
//        d = e.getAttribute("rotx");
//        rot[0] = Float.parseFloat(d);
//        d = e.getAttribute("roty");
//        rot[1] = Float.parseFloat(d);
//        d = e.getAttribute("rotz");
//        rot[2] = Float.parseFloat(d);
//        d = e.getAttribute("rota");
//        rot[3] = Float.parseFloat(d);
//
//        int entityID = Integer.parseInt(e.getAttribute("entityID"));
//
//        entity = model.getEntity(entityID);
//
//        if (entity == null) {
//
//            EntityBuilder builder = EntityBuilder.getEntityBuilder();
//            entity = builder.createEntity(model, entityID, pos, rot, tool);
//
//            model.addEntity(local, entity, null);
//
//        }
//
//        description = "AddEntity -> " + entity.getName();
//
//        init();
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
     * Retrieve the entity
     * 
     * @return Entity
     */
    public Entity getEntity(){
    	return entity;
    }

    /**
     * Retrieve the world model
     */
	public WorldModel getWorldModel() {

		return model;
	}
	
	/**
	 * Update the pos value stored
	 * 
	 * @param pos
	 */
	public void setPosition(double[] pos){
		
		this.pos[0] = pos[0];
		this.pos[1] = pos[1];
		this.pos[2] = pos[2];
	}
	
	/**
	 * Get the pos value stored
	 * 
	 * @param pos
	 */
	public void getPosition(double[] pos){
		
		pos[0] = this.pos[0];
		pos[1] = this.pos[1];
		pos[2] = this.pos[2];
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
		
		if (externalCommand instanceof AddEntityCommand) {
		
			if (((AddEntityCommand)externalCommand).getEntity() != 
				this.entity) {
				
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
