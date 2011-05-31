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

package org.chefx3d.view.awt.av3d;

//External imports
import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.j3d.device.input.TrackerState;

//Internal imports
import org.chefx3d.model.*;

import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.rules.properties.accessors.IgnoreRuleList;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
//import org.chefx3d.rules.util.SceneManagementUtility;

import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.Tool;

import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Add the group of entities contained in a template.
 *
 * @author Rex Melton
 * @version $Revision: 1.40 $
 */
public class AddTemplateEntityResponse
    implements
        TrackerEventResponse,
        CommandListener {

    /** Reference to world model */
    private WorldModel model;

    /** The controlleer to send commands to */
    private CommandController controller;

    /** Reference to error reporter */
    private ErrorReporter reporter;

    /** Reference to entity builder */
    private EntityBuilder entityBuilder;

    /** List of entities in a parented hierarchy */
    private ArrayList<Entity> entityList;

    /** The initial conditions for this action */
    private ActionData actionData;

    /** Instance of hierarchy transformation calculator */
    private TransformUtils tu;

    /** Scratch vecmath objects */
    private Matrix4f mtx1;
    private Matrix4f mtx;
    private Point3f pnt;

    /** Multi-Command list */
    private ArrayList<Command> cmdList;
    private ArrayList<Command> processedCmdList;    
    private ArrayList<Command> selectList;

    /** The currently processing command */
    private Command processCommand;

    /** The number of products processed so far */
    private int count;

    private HashSet<String> ignoreRuleList;
    private HashSet<String> ignoreAutoAddRuleList;

    /** Pocket the templateEntityID here to avoid passing in an extra
     * parameter in the recursive method
     * {@link #separateAndReAddChildren(Entity, Entity, ArrayList)}.     */
    private int templateID;

	/** Utility for aligning the model with the editor grid */
	private EditorGrid editorGrid;
	
    /**
     * Constructor
     *
     * @param model
     * @param controller
     * @param reporter
	 * @param editorGrid
     */
    public AddTemplateEntityResponse(
        WorldModel model,
        CommandController controller,
        ErrorReporter reporter,
		EditorGrid editorGrid) {

        this.model = model;
        this.controller = controller;
        this.reporter = reporter;
		this.editorGrid = editorGrid;
        entityBuilder = DefaultEntityBuilder.getEntityBuilder();

        controller.addCommandHistoryListener(this);

        tu = new TransformUtils();
        mtx1 = new Matrix4f();
        mtx = new Matrix4f();
        pnt = new Point3f();

        entityList = new ArrayList<Entity>();
        cmdList = new ArrayList<Command>();
        processedCmdList = new ArrayList<Command>();
        selectList = new ArrayList<Command>();

        // create the ignore rule list
        ignoreRuleList = new HashSet<String>();
        ignoreRuleList.addAll(IgnoreRuleList.getIgnoreTemplateRuleList());
        
        // create the ignore rule list for the auto-span special case
        ignoreAutoAddRuleList = new HashSet<String>();
        ignoreAutoAddRuleList.addAll(IgnoreRuleList.getIgnoreTemplateRuleList());
        ignoreAutoAddRuleList.remove("org.chefx3d.rules.definitions.AutoAddRule");
        ignoreAutoAddRuleList.remove("org.chefx3d.rules.definitions.AddAutoAddRule");
        ignoreAutoAddRuleList.remove("org.chefx3d.rules.definitions.ReplaceComplexChildrenRule");        

    }

    //---------------------------------------------------------------
    // Methods defined by TrackerEventResponse
    //---------------------------------------------------------------

    /**
     * Begins the processing required to generate a command in response
     * to the input received.
     *
     * @param trackerID The id of the tracker calling the original handler
     * @param trackerState The event that started this whole thing off
     * @param entities The array of entities to handle
     * @param tool The tool that is used in the action (can be null)
     */
    public void doEventResponse(
            int trackerID,
            TrackerState trackerState,
            Entity[] entities,
            Tool tool) {

        // flush the list from previous requests
        cmdList.clear();
        processedCmdList.clear();
        
		ArrayList<Command> cmdsToSort =
			new ArrayList<Command>();

        double[] position =
            (double[])tool.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    PositionableEntity.POSITION_PROP);

        float[] rotation =
            (float[])tool.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    PositionableEntity.ROTATION_PROP);

        if (position == null) {
            position = new double[]{0, 0, 0};
        }

		editorGrid.alignPositionToGrid(position);
		
        if (rotation == null) {
            rotation = new float[]{0, 1, 0, 0};
        }

        // a new entity, a new identity,
		// a service of the entity relocation program
        int templateEntityID = model.issueEntityID();

        // create the entity
        Entity newEntity = entityBuilder.createEntity(
            model,
            templateEntityID,
            position,
            rotation,
            tool);

		// if there is something to relo....
		if ((newEntity != null) &&
				(newEntity instanceof TemplateEntity) &&
				newEntity.hasChildren()) {

		    Entity zoneEntity = null;
		    if (actionData == null) {
		        zoneEntity = entities[0];
		    } else {
		        zoneEntity = actionData.zoneWrapper.entity;

		        // zone to world transform
	            tu.getLocalToVworld(
	                    actionData.zoneWrapper.transformGroup,
	                    mtx);

		    }

			TemplateEntity template = (TemplateEntity)newEntity;

			// need to cast the template to a basic entity
			// before we add it to the scene
            //boolean isKit = false;
            if (template.getCategory().equals("Category.Kit")) {
                createKitEntity(templateEntityID, template, zoneEntity);
                markEntitiesAsKit(template, templateEntityID, true);
            } else if (template.getCategory().equals("Category.Template")) {
            	createTemplateEntity(templateEntityID, template, zoneEntity);
            	markEntitiesAsTemplate(template, templateEntityID);
            }

            // clear out the 'bogus' property
            AV3DUtils.setShadowState(newEntity, false);
            AV3DUtils.setShadowState(template, false);

			// assemble the list of entities in the hierarchy
			entityList.clear();
			initEntities(template);

			// the center of the template
			double[] center = new double[3];
			template.getPosition(center);

			// get the bounds of the template
            float[] bounds = new float[6];
            template.getBounds(bounds);

			ArrayList<Entity> children = template.getChildren();
			int num_children = children.size();
			for (int i = 0; i < num_children; i++) {

				PositionableEntity pe = (PositionableEntity)children.get(i);

                // Handle updating the position
                pe.getPosition(position);

                // what will the new parent be, default in the current zone
                Entity newParent = zoneEntity;

				// This exists in case the child is another kit or template
				// rarely is this going to be the case
	            if (pe.getCategory().equals("Category.Kit")) {
	                int kitEntityID = pe.getEntityID();	              
	                markEntitiesAsKit(template, kitEntityID, false);
	            } else if (pe.getCategory().equals("Category.Template")) {
	            	markEntitiesAsTemplate(template, templateEntityID);
	            } else {

	                // Get the delta position we side pocket in a little bit
	                double[] delta = new double[3];
	                pe.getPosition(delta);
	                
	                String[] parents = (String[])pe.getProperty(
	                		Entity.DEFAULT_ENTITY_PROPERTIES, 
	                		ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

	                position[0] += center[0];
	                position[1] += center[1];
	                position[2] += center[2];

	                if (trackerState != null) {

	                    pnt.x = (float)position[0];
	                    pnt.y = (float)position[1];
	                    pnt.z = (float)position[2];

	                    mtx.transform(pnt);

	                    trackerState.worldPos[0] = pnt.x;
	                    trackerState.worldPos[1] = pnt.y;
	                    trackerState.worldPos[2] = pnt.z;

	                    actionData.pickManager.doPick(trackerState);

	                    // determine the new parent. if the mouse is not over an entity,
	                    // this will be null
	                    PickData pd = null;
	                    ArrayList<PickData> pickList = actionData.pickManager.getResults();
	                    for (int j = 0; j < pickList.size(); j++) {
	                        // find the closest object associated with an entity
	                        // that is NOT the zone entity and is not a part of
	                        // the entity hierarchy that is being transformed
	                        PickData tpd = pickList.get(j);
	                        if ((tpd.object instanceof Entity) &&
	                            !(entityList.contains(tpd.object)) &&
	                            !(tpd.object == pe)) {
	                        	
	                        	// check to make sure it is an allowed parent
	                        	if (parents != null) {
	                        		String[] classifications = 
	                        			(String[])((Entity)tpd.object).getProperty(
	                        				Entity.DEFAULT_ENTITY_PROPERTIES, 
	                        				ChefX3DRuleProperties.CLASSIFICATION_PROP);
	                        		
	                        		if (classifications != null) {
	                        			
	                        			boolean matched = false;
	                        			for (int k = 0; k < parents.length; k++) {
	                        				for (int m = 0; m < classifications.length; m++) {
	                        					
	                        					if (parents[k].equals(classifications[m])) {
	                        						matched = true;
	                        						break;
	                        					}
	                        				}
	                        				
	                        				if (matched)
	                        					break;
	                        				
	                        			}
	                        			                        				
                        				if (!matched)
                        					continue;

	                        		}
	                        		
	                        	}
	                        	
	                        	
	                            if (pd != zoneEntity) {
	                                pd = tpd;
	                                newParent = (Entity)pd.object;
	                            }
	                            break;
	                        }
	                    }

	                } else {
	                    newParent = zoneEntity;
	                }

	                if (newParent != zoneEntity) {

	                    int parentID = newParent.getEntityID();
	                    AV3DEntityWrapper parentWrapper = actionData.wrapperMap.get(parentID);

	                    pnt.x = (float)position[0];
	                    pnt.y = (float)position[1];
	                    pnt.z = (float)position[2];

	                    tu.getLocalToVworld(
	                        parentWrapper.transformGroup,
	                        actionData.zoneWrapper.transformGroup,
	                        mtx1);
	                    mtx1.invert();
	                    mtx1.transform(pnt);

	                    position[0] = pnt.x;
	                    position[1] = pnt.y;
	                    position[2] = pnt.z;

	                }

	                pe.setPosition(position, false);
	                pe.setStartingPosition(position);

	                // Pack in the kit/template center, bounds, entity delta from
	                // center and the kit/template properties. This is so rules
	                // can quickly reference data about the kit/template they
	                // belong to.
	                pe.setProperty(
	                        Entity.DEFAULT_ENTITY_PROPERTIES,
	                        TemplateEntity.TEMPLATE_BOUNDS,
	                        bounds,
	                        false);

	                pe.setProperty(
	                        Entity.DEFAULT_ENTITY_PROPERTIES,
	                        TemplateEntity.TEMPLATE_PROPERTIES,
	                        template.getPropertiesMap(),
	                        false);

	                pe.setProperty(
	                        Entity.DEFAULT_ENTITY_PROPERTIES,
	                        TemplateEntity.TEMPLATE_ENTITY_DELTA_POSITION,
	                        delta,
	                        false);

	            }

	            // Issue the add command
	            AddEntityChildCommand cmd =
	                new AddEntityChildCommand(
	                		model,
	                		model.issueTransactionID(), 
	                		newParent, 
	                		pe, 
	                		false);
	            
	            cmd.setGroupingID(templateEntityID);
	            cmd.setIgnoreRuleList(ignoreRuleList);
                cmd.setErrorReporter(reporter);
                
                Boolean isAutoSpan = 
                	(Boolean)RulePropertyAccessor.getRulePropertyValue(
                        pe,
                        ChefX3DRuleProperties.SPAN_OBJECT_PROP);
            
	            // if this is an auto-span then don't ignore auto-add rules
	            if (isAutoSpan) {
	            	cmd.setIgnoreRuleList(ignoreAutoAddRuleList);
	            }	            

                //
                // instead of adding this command directly
                // to the command list, we will sort it so
                // that parent entities are added before child
                // entities, to avoid illegal collisions, and only
                // then add it to the cmdList.
                //
                cmdsToSort.add(cmd);

			}
			templateID = templateEntityID;
			cmdList.addAll(sortCommandList(cmdsToSort));

			if (cmdList.size() > 0) {
				count = 0;
				
				// set that this is a chained set of commands that needs to finish
				controller.setProcessingChainedCommands(true);
				
				processNextCommand();
			}
		}
    }

    //----------------------------------------------------------
    // Methods defined by CommandListener
    //----------------------------------------------------------

    /**
     * A command was successfully executed
     *
     * @param cmd The command
     */
    public void commandExecuted(Command cmd) {
        if (processCommand == cmd) {
            processNextCommand();
        }
    }

    /**
     * A command was not successfully executed
     *
     * @param cmd The command
     */
    public void commandFailed(Command cmd){
        if (processCommand == cmd) {
            
            boolean continueProcessing = true;
            if (cmd instanceof RuleDataAccessor) {
                Entity entity = ((RuleDataAccessor) cmd).getEntity();
                if (entity.getKitEntityID() >= 0) {
                    continueProcessing = false;
                }
            }
            
            if (continueProcessing) {
                
                processNextCommand();
                
            } else {

                cmdList.clear();
                for (int i = processedCmdList.size() - 1; i >= 0 ; i--) {
                    processCommand = processedCmdList.get(i);
                    if (processCommand instanceof AddEntityChildCommand) {
                        processCommand = 
                            new RemoveEntityChildCommand(
                                    model, 
                                    ((AddEntityChildCommand) processCommand).getParentEntity(), 
                                    ((AddEntityChildCommand) processCommand).getEntity(),
                                    false);
                        ((RemoveEntityChildCommand)processCommand).setBypassRules(true);
                        cmdList.add(processCommand);
                    }
                }
                
                count = 0;            
                processNextCommand();

            }
            
        }
        
    }

    /**
     * A command was successfully undone
     *
     * @param cmd The command
     */
    public void commandUndone(Command cmd) {

    }

    /**
     * A command was successfully redone
     *
     * @param cmd The command
     */
    public void commandRedone(Command cmd) {

    }

    /**
     * The command stack was cleared
     */
    public void commandCleared() {

    }

    //---------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------


    /**
     * This recursive method modifies the parameter addList by
     * removing the child from the parent and then creating a new
     * AddEntityChildCommand.  When a new AddEntityChildCommand
     * (AECC) has been created for each child, we then create a
     * new AECC for the parent.
     *
     * @author Eric Fickenscher
     *
     * @param parent the entity's parent, needed so we can properly
     * specify an AddEntityChildCommand.
     * @param entity The uppermost entity - we recurse downward
     * through all of entity's children
     * @param addList This ArrayList will contain an AddEntityChildCommand
     * for each Entity at and below parameter "entity" when the method
     * returns.
     */
    private void separateAndReAddChildren(
    		Entity parent, 
    		Entity entity,
    		ArrayList<AddEntityChildCommand> addList){

        Boolean isAutoSpan = 
        	(Boolean)RulePropertyAccessor.getRulePropertyValue(
        			parent,
                ChefX3DRuleProperties.SPAN_OBJECT_PROP);
        if (isAutoSpan) {
        	parent.removeChild(entity);
        	return;
        }       	

    	int numChildren = entity.getChildCount()-1;

    	if( numChildren < 0){
    		//
    		// parent has no children; it is a a 'leaf'
    		// in the scenegraph tree of entities.
    		// add it to the cmdList and remove it from its parent
    		//
    		AddEntityChildCommand cmd =
	            new AddEntityChildCommand(
	            		model, 
	            		model.issueTransactionID(), 
	            		parent, 
	            		entity, 
	            		false);
    		
	        cmd.setGroupingID(templateID);
	        cmd.setIgnoreRuleList(ignoreRuleList);
	        cmd.setErrorReporter(reporter);

	        addList.add(cmd);
	        parent.removeChild(entity);

    	} else {
    		// entity has children.  recurse on each of the children
    		for( int i = numChildren; i >= 0; i--){
    			Entity child = entity.getChildAt(i);
    			separateAndReAddChildren(entity, child, addList);
    		}
    		//
    		// now we have added all the children, so it is time
    		// to create an add command for the entity itself
    		//
    		AddEntityChildCommand cmd =
	            new AddEntityChildCommand(
	            		model,
	            		model.issueTransactionID(), 
	            		parent, 
	            		entity, 
	            		false);
    		
	        cmd.setGroupingID(templateID);
	        cmd.setIgnoreRuleList(ignoreRuleList);
	        cmd.setErrorReporter(reporter);

	        addList.add(cmd);
	        parent.removeChild(entity);
    	}
    }


    /**
     * Look the the list and order it in terms of
     * parent entities before child entities.
     * <ol>
     * <li>First, build up a list of all needed add commands.</li>
     * <li>Secondly, make a list of all the unique entity
     * classifications from the add commands.</li>
     * <li>Sort the classification list.  Look at each
     * "allowedParent" - the parent classifications MUST come
     * before the child classifications</li>
     * <li>Finally, iterate through the now-sorted classification
     * list and the list of add commands.  Append the add commands
     * to parameter "cmdList" by order of classification. </li>
     * </ol>
     *
     * @author Eric Fickenscher
     * @param cmdList This list is small to begin with, containing
     * only 'parent' objects (in the case of many templates, that
     * means something like a HangTrack).
     * @return This method modifies and returns cmdList.  On return,
     * cmdList will contain an AddEntityChildCommand for each and
     * every child entity of the entities found the original "cmdList"
     * parameter.
     */
    private ArrayList<Command> sortCommandList(
    		ArrayList<Command> cmdList){

    	//
    	// Build up the full unsorted list of all needed add commands.
    	//
    	ArrayList<AddEntityChildCommand> addList = new
    		ArrayList<AddEntityChildCommand>();

    	int cmdListLen = cmdList.size();
    	for(int parentIdx = 0; parentIdx < cmdListLen; parentIdx++ ){

    		Entity parent = 
    			((RuleDataAccessor)cmdList.get(parentIdx)).getEntity();

    		int numChildren = parent.getChildCount()-1;
    		for( int i = numChildren; i >= 0; i--){
    			Entity child = parent.getChildAt(i);
    			separateAndReAddChildren(parent, child, addList);
    		}
    	}

    	//
    	// Iterate through the unsorted list of add commands
    	// and make a list of all the unique classifications found
    	//
    	ArrayList<String> classifications = new ArrayList<String>();
    	for( AddEntityChildCommand aecc : addList){
    		Entity entity = aecc.getEntity();

    		String[] classificationArray = (String[])
            RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CLASSIFICATION_PROP);

    		if( classificationArray == null ){

    			// entity has a null classification, so
    			// make sure "null" is included once
    			if( !classifications.contains(null))
    				classifications.add(null);

    		} else {

    			// add each classification encountered, if that
    			// classification is not already in the ArrayList
    			for( String classification : classificationArray){
    				if( !classifications.contains(classification))
    					classifications.add(classification);
    			}
    		}
    	}
    	String[] emptyArray = new String[]{};

    	//
    	// At this point we have the full list of add entity
    	// classifications.  We need to look through the list
    	// of commands and compare entity classification
    	// versus allowedParentClassification, and sort the
    	// classification list appropriately.
    	//
    	for( AddEntityChildCommand aecc : addList ){
    		Entity entity = aecc.getEntity();
    		String[] entityClassifications = (String[])
    			RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.CLASSIFICATION_PROP);
    		String[] allowedParents = (String[])
    			RulePropertyAccessor.getRulePropertyValue(
                    entity,
                    ChefX3DRuleProperties.ALLOWED_PARENT_CLASSIFICATIONS);

    		// defensive code
			if( allowedParents == null){
				allowedParents = emptyArray;
			}
			if( entityClassifications == null){
				entityClassifications = emptyArray;
			}

   			//
   			// each parent classification needs to be ahead of ALL
   			// children classifications.
   			//
			for( String entityClassification : entityClassifications){

				int cIndex = classifications.indexOf(entityClassification);

				for( String p : allowedParents ){

					int pIndex = classifications.indexOf(p);

					//
					// if the child classification comes before
					// the parent classification in the classifications list,
					// push the parent in front of the child.
					//
					if( cIndex < pIndex ){
						classifications.remove(pIndex);
						classifications.add(cIndex, p);
						cIndex++;
					}
				}
			}
    	}
		//
		// move nulls to the end of the list
		//
    	int nullIndex = classifications.indexOf(null);
    	if( nullIndex >=0){
    		classifications.remove(null);
    		classifications.add(null);
    	}

    	//
    	// now our classification list is sorted.
    	// iterate through the list of classifications
    	// and the list of addCommands, and add the addCommands
    	// according to the classification ordering.
    	//
    	for( String classification : classifications){
//System.out.println("==============\nadding every : " + classification + "in the list\n");
    		int addListLen = addList.size()-1;
    		for( int i = addListLen; i >= 0; i--){

    			AddEntityChildCommand aecc = addList.get(i);
    			Entity addIt = aecc.getEntity();

    			String[] classifs = (String[])
        			RulePropertyAccessor.getRulePropertyValue(
        				addIt,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

    			if(classifs == null)
    				classifs = new String[]{ null };

    			ArrayList<String> addItClassifs =
    				new ArrayList<String>(java.util.Arrays.asList(classifs));

    			if(addItClassifs.contains(classification)){
    				cmdList.add(aecc);
    				addList.remove(aecc);
//System.out.println("adding " + addIt + "\n\tto " + aecc.getParentEntity().getName());
    			}
    		}
    	}
    	
        // perform a final sort for auto-span products
        cmdListLen = cmdList.size() - 1;
        for (int idx = cmdListLen; idx >= 0; idx--) {
            
            Entity entity = ((RuleDataAccessor)cmdList.get(idx)).getEntity();
            Boolean isAutoSpan = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.SPAN_OBJECT_PROP);
            
            // if this is an auto-span item them we need to make sure it
            // is at the end of the list so that all possible mount points
            // are in the scene
            if (isAutoSpan) {
            	
                Command aecc = cmdList.remove(idx);
                cmdList.add(aecc);
    			
            }

        }  
        
    	return cmdList;
    }


    /**
     * Initialize in preparation for a response
     *
     * @param ad The initial device position of the mouse
     */
    void setActionData(ActionData ad) {
        actionData = ad;
    }

    /**
     * Sets the correct entity builder to use
     * @param builder
     */
    public void setEntityBuilder(EntityBuilder builder){
        entityBuilder = builder;
    }

    /**
     * Initialize the list of entities in a hierarchy
     */
    void initEntities(Entity entity) {
        entityList.add(entity);
        if (entity.hasChildren()) {
            ArrayList<Entity> children = entity.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Entity child = children.get(i);
                initEntities(child);
            }
        }
    }

    /**
     * Initialize the list of entities in a hierarchy as
     * a group/kit by setting the kitEntityID.
     */
    void markEntitiesAsKit(Entity entity, int kitEntityID, boolean flag) {

        if (flag)
            entity.setKitEntityID(kitEntityID);
        else
            if (entity.getKitEntityID() != -1)
                entity.setKitEntityID(kitEntityID);

        if (entity.hasChildren()) {
            ArrayList<Entity> children = entity.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Entity child = children.get(i);
                markEntitiesAsKit(child, kitEntityID, flag);
            }
        }
    }

    /**
     * Initialize the list of entities in a hierarchy as
     * a group/template by setting the templateEntityID.
     */
    void markEntitiesAsTemplate(Entity entity, int templateEntityID) {

        entity.setTemplateEntityID(templateEntityID);

        if (entity.hasChildren()) {
            ArrayList<Entity> children = entity.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Entity child = children.get(i);
                markEntitiesAsTemplate(child, templateEntityID);
            }
        }
    }

    /**
     * Process the next command in the list
     *
     * @param cmd The previous command just completed
     */
    private void processNextCommand() {

        // get the next command
        if (count < cmdList.size()) {
            processCommand = cmdList.get(count);
            controller.execute(processCommand);           
            processedCmdList.add(processCommand);
        } else {
        	// we are done with the chain of commands
        	controller.setProcessingChainedCommands(false);
        }

        count++;

    }

    /**
     * Create the kit and add it to the scene
     *
     * @param kitEntityID ID of the kit to use
     * @param template Template entity to add
     * @param parent Parent to add the template to
     */
    private void createKitEntity(
    		int kitEntityID,
    		Entity template,
    		Entity parent) {

        // create the product tool
        SimpleTool templateTool = new SimpleTool(
                template.getToolID(),
                template.getName(),
                template.getIconURL(null),
                null,
                template.getType(),
                null,
                template.getDescription(),
                new float[] {0.000001f, 0.000001f, 0.000001f},
                new float[] {0.000001f, 0.000001f, 0.000001f},
                MultiplicityConstraint.NO_REQUIREMENT,
                template.getCategory(),
                false,
                false,
                false,
                false,
                template.getPropertiesMap());

        double[] position = new double[] {0.0, -200.0, 0.0};
        float[] rotation = new float[] {0, 0, 1, 0};

        // create the entity way off in space
        Entity newEntity = entityBuilder.createEntity(
            model,
            kitEntityID,
            position,
            rotation,
            templateTool);

        // Correct the relationship values so it can be placed correctly
        newEntity.setProperty(
        		Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP,
        		new String[] {"empty"},
        		false);

        newEntity.setProperty(
        		Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP,
        		new int[] {0},
        		false);

        newEntity.setProperty(
        		Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP,
        		new Enum[] {ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES.EQUAL_TO},
        		false);

        newEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_BOUNDARY,
                false,
                false);

        // add template container to the zone
        AddEntityChildCommand cmd =
            new AddEntityChildCommand(
            		model,
            		model.issueTransactionID(),
            		parent,
            		newEntity,
            		false);
        
        cmd.setBypassRules(true);
        cmd.setErrorReporter(reporter);
        cmd.setGroupingID(kitEntityID);
        cmdList.add(cmd);

    }

    /**
     * Create the template and add it to the scene
     *
     * @param templateEntityID ID of the template to use
     * @param template Template entity to add
     * @param parent Parent to add template to
     */
    private void createTemplateEntity(
    		int templateEntityID,
    		Entity template,
    		Entity parent) {

        // create the product tool
        SimpleTool templateTool = new SimpleTool(
                template.getToolID(),
                template.getName(),
                template.getIconURL(null),
                null,
                template.getType(),
                null,
                template.getDescription(),
                new float[] {0.000001f, 0.000001f, 0.000001f},
                new float[] {0.000001f, 0.000001f, 0.000001f},
                MultiplicityConstraint.NO_REQUIREMENT,
                template.getCategory(),
                false,
                false,
                false,
                false,
                template.getPropertiesMap());

        double[] position = new double[] {0.0, -200.0, 0.0};
        float[] rotation = new float[] {0, 0, 1, 0};

        // create the entity way off in space
        Entity newEntity = entityBuilder.createEntity(
            model,
            templateEntityID,
            position,
            rotation,
            templateTool);

        // Correct the relationship values so it can be placed correctly
        newEntity.setProperty(
        		Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP,
        		new String[] {"empty"},
        		false);

        newEntity.setProperty(
        		Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP,
        		new int[] {0},
        		false);

        newEntity.setProperty(
        		Entity.DEFAULT_ENTITY_PROPERTIES,
        		ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP,
        		new Enum[] {ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES.EQUAL_TO},
        		false);

        newEntity.setProperty(
                Entity.DEFAULT_ENTITY_PROPERTIES,
                ChefX3DRuleProperties.MOVEMENT_RESTRICTED_TO_BOUNDARY,
                false,
                false);


        // add template container to the zone
        AddEntityChildCommand cmd =
            new AddEntityChildCommand(
            		model,
            		model.issueTransactionID(),
            		parent, 
            		newEntity, 
            		false);
        cmd.setBypassRules(true);
        cmd.setErrorReporter(reporter);
        cmdList.add(cmd);

    }
}
