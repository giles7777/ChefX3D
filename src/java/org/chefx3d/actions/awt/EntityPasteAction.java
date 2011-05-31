/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.actions.awt;

// External imports
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.KeyStroke;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.ViewManager;
import org.chefx3d.tool.PasteTool;

/**
 * An action that can be used to paste an Entity into the model.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Russell Dodds
 * @version $Revision: 1.33 $
 */
public class EntityPasteAction extends AbstractAction 
	implements       
		ModelListener, 
        EntityChildListener, 
        EntitySelectionListener, 
        ClipboardListener,
        CommandListener {

    /** Name of the property to get the action description */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.EntityPasteAction.description";

    /** Name of the property to get the action name */
    private static final String TITLE_PROP =
        "org.chefx3d.actions.awt.EntityPasteAction.title";

    /** The world model */
    private WorldModel model;
    
    /** The clipboard to remember items */
    private CopyPasteClipboard clipboard;    

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The current scene */
    private SceneEntity currentScene;

    /** The current location */
    private LocationEntity currentLocation;

    /** Map of locations, keyed by id */
    private HashMap<Integer, LocationEntity> locationMap;

    //private List<VertexEntity> vertices;
    
	/** i18n support */
    private I18nManager intl_mgr;
    
    
    /** List of all the segments that need to be added, if we've 
     * copy/pasted after selecting multiple segments.
     */ 
    private ArrayList<SegmentEntity> segmentsToPaste;
    
    /** This variable is used if we've copy/pasted after selecting 
     * multiple segments.  It stores the last command executed so that 
     * we know when a particular paste command has finished executing.
     * To position the next paste segment correctly, we need to know 
     * that the command has finished, regardless whether it succeeded 
     * or failed.
     */
    private MultiCommand lastCommand;
    
    /** Variable to track our position in the segmentsToPaste array */
    private int pasteIndex;

    /**
     * Create an instance of the action class.
     *
     * @param standAlone Is this standalone or in a menu
     * @param icon The icon
     * @param model The world model
     * @param buffer The source of the Entity to paste.
     */
    public EntityPasteAction(
            boolean iconOnly, 
            Icon icon, 
            WorldModel model, 
            CopyPasteClipboard clipboard) {
        
        intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(TITLE_PROP));

        this.model = model;
        this.model.addModelListener(this);
		
        this.clipboard = clipboard;
        this.clipboard.addClipboardListener(this);

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_V,
            KeyEvent.CTRL_MASK);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_V));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));

        setEnabled(false);
        locationMap = new HashMap<Integer, LocationEntity>();
        
        lastCommand = null;
        pasteIndex = 0;
        segmentsToPaste = new ArrayList<SegmentEntity>();

    }

    //----------------------------------------------------------
    // Methods required by the ActionListener interface
    //----------------------------------------------------------

    /**
     * An action has been performed. If an Entity is available in
     * copy buffer, add it to the model.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {
              
		if(errorReporter == null) {
			errorReporter = DefaultErrorReporter.getDefaultReporter();
		}
		
        // get the entities off the clip board
        List<Entity> entityList = clipboard.getEntityList();
		
        if ((entityList != null) && !entityList.isEmpty()) {

        	// List<Entity> entityToPasteList = new ArrayList<Entity>();

        	int len = entityList.size();
        	if (len == 1) {

        		Entity entityToPaste = entityList.get(0);
        		int type = entityToPaste.getType();

        		switch(type) {
        		case Entity.TYPE_MODEL:
        		case Entity.TYPE_MODEL_WITH_ZONES:
        			// a single model
        			pasteEntity(entityToPaste.clone(model));
        			break;

        		case Entity.TYPE_SEGMENT:
        			SegmentEntity segmentToPaste = 
        				(SegmentEntity)entityToPaste.clone(model);
        			model.applyCommand(pasteSegment(segmentToPaste));
        		}

        	} else if (len > 1) {

        		ArrayList<Entity> pasteList = new ArrayList<Entity>();
        		pasteList.addAll(entityList);
        		ArrayList<Entity> children = new ArrayList<Entity>();

        		// find the children of all the paste-ables,
        		// identify if there are segments in the list,
        		// remove any that are not candidates to copy
        		int num = len;
        		int num_segments = 0;
        		for (int i = len - 1; i >= 0; i--) {
        			Entity entity = pasteList.get(i);
        			int type = entity.getType();
        			switch (type) {
        			case Entity.TYPE_SEGMENT:
        				num_segments++;
        			case Entity.TYPE_MODEL:
        			case Entity.TYPE_MODEL_WITH_ZONES:
        				ActionUtils.getChildren(entity, children);
        				break;
        			default:
        				pasteList.remove(i);
        			num--;
        			}
        		}
        		if (num > 0) {
        			len = num;
        			if (num_segments == 0) {
        				// only models
        				double[] position = new double[3];
        				double[] max = new double[]{
        						-Double.MAX_VALUE, 
        						-Double.MAX_VALUE, 
        						-Double.MAX_VALUE};
        				double[] min = new double[]{
        						Double.MAX_VALUE, 
        						Double.MAX_VALUE, 
        						Double.MAX_VALUE};

        				for (int i = len - 1; i >= 0; i--) {

        					// clear any selections that are parented
        					// by other selections, find the center of
        					// the remaining pasteList set
        					PositionableEntity entity = (PositionableEntity)pasteList.get(i);
        					if (children.contains(entity)) {

        						pasteList.remove(i);
        						num--;

        					} else {
        						entity.getPosition(position);
        						if (position[0] < min[0]) {
        							min[0] = position[0];
        						} 
        						if (position[0] > max[0]) {
        							max[0] = position[0];
        						} 
        						if (position[1] < min[1]) {
        							min[1] = position[1];
        						} 
        						if (position[1] > max[1]) {
        							max[1] = position[1];
        						} 
        						if (position[2] < min[2]) {
        							min[2] = position[2];
        						} 
        						if (position[2] > max[2]) {
        							max[2] = position[2];
        						} 
        					}
        				}
        				if (num == 1) {
        					// only one model left
        					pasteEntity(pasteList.get(0).clone(model));

        				} else if (num > 1) {
        					// multiple models left, group them into a
        					// container, reconfigure their positions so
        					// they drag nicely during the shadow op
        					TemplateEntity containerEntity = new TemplateEntity(
        							model.issueEntityID(), 
        							createPropertyMap()); 

        					double[] center = new double[]{
        							((max[0] + min[0]) * 0.5),
        							((max[1] + min[1]) * 0.5),
        							((max[2] + min[2]) * 0.5)};

        					// clone the entities
        					for (int i = 0; i < num; i++) {

        						Entity entityToPaste = pasteList.get(i);
        						PositionableEntity clonedEntity = 
        							(PositionableEntity)entityToPaste.clone(model);

        						clonedEntity.getPosition(position);
        						position[0] -= center[0];
        						position[1] -= center[1];
        						// position[2] -= center[2];
        						clonedEntity.setPosition(position, false);
        						clonedEntity.setStartingPosition(position);

        						containerEntity.addChild(clonedEntity);
        					}
        					pasteEntity(containerEntity);
        				}
        			} else if (num_segments == 1) {

        				//
        				// pasting a single segment.
        				// ignore the models
        				//
        				for (int i = 0; i < len; i++) {
        					PositionableEntity entity = (PositionableEntity)pasteList.get(i);

        					if (entity instanceof SegmentEntity) {

        						SegmentEntity segmentToPaste = (SegmentEntity)entity;
        						segmentToPaste = segmentToPaste.clone(model);
        						model.applyCommand(pasteSegment(segmentToPaste));

        						break;
        					}
        				}
        			} else {
        				//
        				// pasting multiple segments
        				//
        				ArrayList<SegmentEntity> pasteSegments =
        					new ArrayList<SegmentEntity>();

        				for (int i = len-1; i >= 0; i--) {
        					PositionableEntity entity = (PositionableEntity)pasteList.get(i);

        					if (entity instanceof SegmentEntity) {

        						SegmentEntity se = (SegmentEntity)entity;
        						se = se.clone(model);
        						pasteSegments.add(se);
        					}
        				}

        				pasteSegments(pasteSegments);
        			}
        		}
        	}
        }
    }
    
    //----------------------------------------------------------
    // Methods required by the ClipboardListener interface
    //----------------------------------------------------------
  
    /**
     * Notify listeners of a status change to the clip board contents
     * 
     * @param hasEntities true if contents, false otherwise
     */
    public void clipboardUpdated(boolean hasEntities) {
        
        // enable button
        if(hasEntities) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }

    }
        
    //----------------------------------------------------------
    // Methods required by the ModelListener interface
    //----------------------------------------------------------

    /**
     * An entity was added.
     */
    public void entityAdded(boolean local, Entity entity){
        if (entity instanceof SceneEntity) {
			if (currentScene != null) {
				if (locationMap.size() > 0) {
					for (Iterator<Integer> i = locationMap.keySet().iterator(); i.hasNext();) {
						LocationEntity le = locationMap.remove(i.next());
						le.removeEntitySelectionListener(this);
					}
				}
			}
            currentScene = (SceneEntity)entity;
            entity.addEntityChildListener(this);
			if (entity.hasChildren()) {
				ArrayList<Entity> children = entity.getChildren();
				for (int i = 0; i < children.size(); i++) {
					Entity child = children.get(i);
					if (child instanceof LocationEntity) {
						LocationEntity le = (LocationEntity)child;
						le.addEntitySelectionListener(this);
						locationMap.put(child.getEntityID(), le);
					}
				}
			}
        }
    }

    /**
     * An entity was removed. If the removed Entity was selected,
     * disable the copy function until a new Entity is selected.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed from the view
     */
    public void entityRemoved(boolean local, Entity entity) {
        if ((entity instanceof SceneEntity) && (entity == currentScene)) {
            entity.removeEntityChildListener(this);
			if (locationMap.size() > 0) {
				for (Iterator<Integer> i = locationMap.keySet().iterator(); i.hasNext();) {
					LocationEntity le = locationMap.get(i.next());
					le.removeEntitySelectionListener(this);
				}
				locationMap.clear();
			}
            currentScene = null;
            currentLocation = null;
        }
    }

    /**
     * Ignored.
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * Ignored.
     */
    public void masterChanged(boolean local, long viewID) {
        // ignore
    }

    /**
     * Ignored.
     */
    public void modelReset(boolean local) {
        // ignore
    }

    // ---------------------------------------------------------------
    // Methods defined by EntityChildListener
    // ---------------------------------------------------------------

    /**
     * A child was added.
     * 
     * @param parent The entity which changed
     * @param child The child which was added
     */
    public void childAdded(int parent, int child) { 
		
        Entity entity = model.getEntity(child);
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.addEntitySelectionListener(this);
            locationMap.put(child, le);
        }
    }
    
    /**
     * A child was removed.
     * 
     * @param parent The entity which changed
     * @param child The child which was removed
     */
    public void childRemoved(int parent, int child) {
		
        if (locationMap.containsKey(child)) {
            LocationEntity le = locationMap.remove(child);
            le.removeEntitySelectionListener(this);
            if (le == currentLocation) {
                currentLocation = null;
            }
        }
    }

    /**
     * A child was inserted.
     * 
     * @param parent The entity which changed
     * @param child The child which was added
     * @param index The index the child was placed at
     */
    public void childInsertedAt(int parent, int child, int index) {
		
		Entity entity = model.getEntity(child);
        if (entity instanceof LocationEntity) {
            LocationEntity le = (LocationEntity)entity;
            le.addEntitySelectionListener(this);
            locationMap.put(child, le);
        }
    }
    
    // ---------------------------------------------------------------
    // Methods defined by EntitySelectionListener
    // ---------------------------------------------------------------

    /**
     * An entity has been selected
     * 
     * @param entityID The entity which changed
     * @param selected Status of selecting
     */
    public void selectionChanged(int entityID, boolean selected) {
		if (selected) {
			if (locationMap.containsKey(entityID)) {
				LocationEntity le = locationMap.get(entityID);
				currentLocation = le;
			}
		}
    }
    
    /**
     * An entity has been highlighted
     * 
     * @param entityID The entity which changed
     * @param highlighted Status of highlighting
     */
    public void highlightChanged(int entityID, boolean highlighted) {
        // ignore
    } 
    

    // ---------------------------------------------------------------
    // Methods inherited from CommandListener
    // ---------------------------------------------------------------
    
    
	public void commandCleared() {
		// TODO Auto-generated method stub
		
	}

	/** Command finished successfully */
	public void commandExecuted(Command cmd) {
		if(cmd == lastCommand)
			pasteSegments();
		
	}

	/** Command finished unsuccessfully */
	public void commandFailed(Command cmd) {
		if(cmd == lastCommand)
			pasteSegments();
		
	}

	public void commandRedone(Command cmd) {
		// TODO Auto-generated method stub
		
	}

	public void commandUndone(Command cmd) {
		// TODO Auto-generated method stub
		
	}

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
	
    /**
     * Paste segment directly on to the end of the current
     * SegmentableEntity.  If none exists, will add the given
     * SegmentEntity so that the startVertex lines up on the origin
     * of the floor (0, 0).
     * @param pasteSegment assuming a non-null SegmentEntity.
     * @author Eric Fickenscher
     */
    private MultiCommand pasteSegment(SegmentEntity pasteSegment) {
    	
    	ArrayList<Command> commandList = new ArrayList<Command>();
        
    	//
    	// calculate the difference between the start and end vertex
    	// so we can properly position the end vertex once the start is moved.
    	//    	
    	VertexEntity ps_v1 = pasteSegment.getStartVertexEntity();
    	VertexEntity ps_v2 = pasteSegment.getEndVertexEntity();
    	
    	double[] ps_v1Pos = new double[3];
    	double[] ps_v2Pos = new double[3];
    	ps_v1.getPosition(ps_v1Pos);
    	ps_v2.getPosition(ps_v2Pos);
    	    	
    	double xDiff = ps_v2Pos[0] - ps_v1Pos[0];
    	double yDiff = ps_v2Pos[1] - ps_v1Pos[1];
    	double zDiff = ps_v2Pos[2] - ps_v1Pos[2];
    	    	
    	//
        // find the SegmentableEntity, specifically to
    	// get the last vertex of the last segment
    	//
        SegmentableEntity segmentableEntity = null;                 
        List<Entity> contents = currentLocation.getContents();
        int len = contents.size();
        for (int i = 0; i < len; i++){
            Entity check = contents.get(i);
            if (check instanceof SegmentableEntity) {
                segmentableEntity = (SegmentableEntity)check;
                break;
            }
        }
        //
        // if a segmentable entity exists, use the endVertex
        // from the last segment to set the position of the 
        // pasteSegment's start vertex.  Otherwise, set the
        // pasteSegement's start vertex to the origin
        //
        double[] lastPos = new double[]{ 0, 0, 0};
        VertexEntity lastVertex = null;
        
        if(segmentableEntity != null){
        	int endVertexID = segmentableEntity.getEndVertexID();
        	lastVertex = segmentableEntity.getVertex(endVertexID);
        } 
        
        if( lastVertex != null){
        	pasteSegment.setStartVertex(lastVertex);
        	lastVertex.getPosition(lastPos);
            
        } else {
        	// add start vertex
        	ps_v1.setPosition(lastPos, false);
            AddVertexCommand vertexCmd = 
                new AddVertexCommand(
                        segmentableEntity,
                        ps_v1, 
                        -1);  
            commandList.add(vertexCmd);
        }
        
        //
        // adjust the end vertex by the difference calculated
        // earlier.  Now it will be offset from the pasteEntity's
        // startVertex exactly as it was before.
        //
        lastPos[0] += xDiff;
        lastPos[1] += yDiff;
        lastPos[2] += zDiff;
        ps_v2.setPosition(lastPos, false);
        
        // add the end vertex
        AddVertexCommand vertexCmd = 
            new AddVertexCommand(
                    segmentableEntity,
                    ps_v2, 
                    -1);  
        commandList.add(vertexCmd);

        // add segment command   
        AddSegmentCommand segmentCmd = 
            new AddSegmentCommand(
                    model, 
                    segmentableEntity,
                    pasteSegment);   
        commandList.add(segmentCmd);

        MultiCommand multiCmd = 
            new MultiCommand(
                    commandList, 
                    "Entity Paste Action" + " -> " + ps_v2.getEntityID(), 
                    true, 
                    false);
        
        return multiCmd;
        
    }

    
    /**
     * Reset counter {@link #pasteIndex} and reset
     * paste list {@link #segmentsToPaste}.  Fire off a 
     * {@link #pasteSegment(SegmentEntity)} command.  
     * Successive {@link #pasteSegment(SegmentEntity)} methods
     * will be fired by methods {@link #commandExecuted(Command)} 
     * and {@link #commandFailed(Command)}, which will increment
     * {@link #pasteIndex} until we've attempted a paste with
     * every SegmentEntity in {@link #segmentsToPaste}.
     * @param pasteSegments an ArrayList of SegmentEntities.
     * @author Eric Fickenscher
     */
    private void pasteSegments(ArrayList<SegmentEntity> pasteSegments){
        	
    	segmentsToPaste = pasteSegments;
    	pasteIndex = 0;
    	
    	lastCommand = pasteSegment(segmentsToPaste.get(pasteIndex));
    	model.applyCommand(lastCommand);
    }
    
    
    /**
     * !Do not call this method directly unless you are sure
     * that {@link #pasteIndex} and {@link #segmentsToPaste}
     * have both been initialized properly.
     * Should only be called by {@link #pasteSegments(ArrayList)},
     * {@link #commandExecuted(Command)}, 
     * or {@link #commandFailed(Command)}.  
     * @author Eric Fickenscher
     */
    private void pasteSegments(){
    	
    	pasteIndex ++;
    	if(pasteIndex < segmentsToPaste.size()){
	    	lastCommand = pasteSegment(segmentsToPaste.get(pasteIndex));
    		model.applyCommand(lastCommand);
    	}
    }
       
    
    /**
     * A little helper method to print the start and end vertices of
     * all the vertices in the current segmentable entity.
     * If there is no current SegmentableEntity, this won't print anything.
     * 
     * @author Eric Fickenscher
     */
    private void printSegmentableEntity(){
        
    	SegmentableEntity segmentableEntity = null;                 
        List<Entity> contents = currentLocation.getContents();
        
        int len = contents.size();
        for (int i = 0; i < len; i++){
            Entity check = contents.get(i);
            if (check instanceof SegmentableEntity) {
                segmentableEntity = (SegmentableEntity)check;
                break;
            }
        }
        
        if(segmentableEntity == null)
        	return;
        
        ArrayList<SegmentEntity> segments = segmentableEntity.getSegments();
        VertexEntity vertex;
        double[] vertexPos = new double[3];
	    int i = 0;
	    for(SegmentEntity segment : segments){
	    		
	    	vertex = segment.getStartVertexEntity();
	    	vertex.getPosition(vertexPos);
		    System.out.println(++i + ". start: " + java.util.Arrays.toString(vertexPos));
	    	
	    	vertex = segment.getEndVertexEntity();
	    	vertex.getPosition(vertexPos);
	    	System.out.println("\tend: " + java.util.Arrays.toString(vertexPos));
    	}

    }
    
    
    /**
     * Paste a product entity
     * 
     * @param entityToPaste
     */
    private void pasteEntity(Entity entityToPaste) {
        
        float[] scale = new float[3];                   
        ((PositionableEntity)entityToPaste).getScale(scale);
        
        float[] size = new float[3];                   
        ((PositionableEntity)entityToPaste).getSize(size);

        // tell the views about the new tool
        PasteTool pasteTool = new PasteTool(entityToPaste, size, scale);   
       
        ViewManager viewMgr = ViewManager.getViewManager();
        viewMgr.setTool(pasteTool);

    }
    
	/**
	 * This method is mysterious due to it's lack of javadoc.
	 */
	private Map<String,Map<String, Object>> createPropertyMap() {
	    
        // window
        HashMap<String, Object> defaultProps = new HashMap<String, Object>();
        
        // size will always be 1, the scale can be used to 
        // correctly size the ghost box.
        float[] size = new float[] {0, 0, 0};                   
        float[] scale = new float[] {1, 1, 1};  
       
        HashMap<String, Object> defaultParams = new HashMap<String, Object>();
        defaultParams.put(Entity.TYPE_PARAM, Entity.TYPE_TEMPLATE_CONTAINER);
        defaultParams.put(Entity.MODEL_URL_PARAM, null);
        defaultParams.put(PositionableEntity.SIZE_PARAM, size);

        Map<String,Map<String, Object>> entityProperties =
            new HashMap<String,Map<String, Object>>();
        entityProperties.put(Entity.DEFAULT_ENTITY_PROPERTIES, defaultProps);
        entityProperties.put(Entity.ENTITY_PARAMS, defaultParams);
        
        return entityProperties;
	}
    /**
     * Make sure not to paste the same entity twice
     * 
     * @param vertex
     * @return
     */
			/*
    private boolean isNewVertex(VertexEntity vertex) {
      
        double[] pos1 = new double[3];
        vertex.getPosition(pos1);
        
        double[] pos2 = new double[3];

        int len = vertices.size();
        for (int i = 0; i < len; i++) {
            VertexEntity check = vertices.get(i);
            check.getPosition(pos2);
            
            if (pos1[0] == pos2[0] && pos1[1] == pos2[1]) {
                return false;
            }
        }
      
        vertices.add(vertex);
        
        return true;
    }
*/


}
