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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chefx3d.model.AddEntityChildCommand;
import org.chefx3d.model.AddEntityChildTransientCommand;
import org.chefx3d.model.ChangePropertyCommand;
import org.chefx3d.model.Command;
import org.chefx3d.model.ComplexEntityData;
import org.chefx3d.model.Entity;
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.ExtrusionEntity;
import org.chefx3d.model.ListProperty;
import org.chefx3d.model.LocationEntity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.RuleDataAccessor;
import org.chefx3d.model.SelectEntityCommand;
import org.chefx3d.model.SelectZoneCommand;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult.NOT_APPROVED_ACTION;
import org.chefx3d.rules.util.AutoAddInvisibleChildrenUtility;
import org.chefx3d.rules.util.CommandSequencer;
import org.chefx3d.rules.util.ExpertSceneManagementUtility;
import org.chefx3d.rules.util.SceneHierarchyUtility;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
 * If a product is flagged as a complex product then handle add and updating
 * the sub part children based on the currently selected options.
 *
 * @author Russell Dodds
 * @version $Revision: 1.40 $
 */
public class ReplaceComplexChildrenRule extends BaseRule {

	/** Illegal collision conditions exist */
	private static final String ILLEGAL_COL_PROP =
		"org.chefx3d.rules.definitions.AddEntityCheckForCollisionsRule.illegalCollisions";

    /** Message to user to indicate global update */
    private String confirmMsg;
    
    /** Mark entities as processed so we don't do things twice */
    private ArrayList<Entity> processedEntities;    

	/**
	 * Constructor
	 *
	 * @param errorReporter Error reporter to use
	 * @param model Collision checker to use
	 * @param view AV3D view to reference
	 */
	public ReplaceComplexChildrenRule (
			ErrorReporter errorReporter,
			WorldModel model,
			EditorView view) {

		super(errorReporter, model, view);

		ruleType = RULE_TYPE.INVIOLABLE;

        String i18nKey =
            "org.chefx3d.rules.definitions.ReplaceComplexChildrenRule.confirmMsg";
        confirmMsg = intl_mgr.getString(i18nKey);

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
		
		processedEntities = new ArrayList<Entity>();

		// check if the entity is a complex product
		Boolean isComplexProduct =
		    (Boolean)RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.IS_COMPLEX_PRODUCT);

		String combination = "";
		if (isComplexProduct) {

	        if (command instanceof AddEntityChildCommand) {

	            // if the product is both a complex product and a sub par or
	            // if it already has sub parts then use the current value, 
	            // otherwise you the default selection value
	            
	            // check if the entity is a complex sub part
	            Boolean isComplexSubPart =
	                (Boolean)RulePropertyAccessor.getRulePropertyValue(
	                        entity,
	                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

	            // get the combination to use
	            List<Entity> children = getComplexSubPartChildren(entity);
	            if (isComplexSubPart || children.size() > 0) {
	                
	                combination = generateCurrentCombination(entity, "", "", "0");

	                // update the property sheet to match the default combination
	                setCurrentOptions(entity, combination, true);

	            } else {
	                
	                combination =
	                    (String)RulePropertyAccessor.getRulePropertyValue(
	                            entity,
	                            ChefX3DRuleProperties.COMPLEX_PRODUCT_DEFAULT_SELECTION);
	            
	                // translate any -1 to 0
	                combination = combination.replaceAll("-1", "0");

	                // update the property sheet to match the default combination
	                setCurrentOptions(entity, combination, false);

	            }	            

	            // add the children
	            addInitialChildEntities(combination, entity, model, command);	            

            } else if (command instanceof AddEntityChildTransientCommand) {
	                
            	// get the default selection
                combination =
                    (String)RulePropertyAccessor.getRulePropertyValue(
                            entity,
                            ChefX3DRuleProperties.COMPLEX_PRODUCT_DEFAULT_SELECTION);
            
                // translate any -1 to 0
                combination = combination.replaceAll("-1", "0");

                // if the entity has children then this is a paste operation
                if (!entity.hasChildren()) {
                    // update the property sheet to match the default combination
                    setCurrentOptions(entity, combination, false);
                }
           
	            // add the children
	            addInitialChildEntities(combination, entity, model, command);	            

            } else if (command instanceof ChangePropertyCommand) {

                // get the current value
                ChangePropertyCommand changeCmd = (ChangePropertyCommand)command;

                String propertySheet = changeCmd.getPropertySheet();
                String propertyName = changeCmd.getPropertyName();
                ListProperty currentList =
                    (ListProperty)entity.getProperty(
                            propertySheet,
                            propertyName);

                String currentValue = currentList.getSelectedValue();
                String newValue =
                    ((ListProperty)changeCmd.getNewValue()).getSelectedValue();
                
                if (currentValue.equals(newValue)) {
                    result.setResult(true);
                    return result;
                }
                
                Boolean autoAddProducts =
                    (Boolean)ApplicationParams.get("autoAddProducts");

                boolean globalUpdate = false;
                if (autoAddProducts != null && autoAddProducts) {
                    globalUpdate = true;
                } else {
                    // first check to see if this is singular or global
                    globalUpdate = popUpConfirm.showMessage(confirmMsg);
                }

	            // the list of complex products to swap
	            List<PositionableEntity> swapList =
	                new ArrayList<PositionableEntity>();

	            if (globalUpdate) {

	                // gather all possible updates
	                findComplexEntities(
	                        view.getActiveLocationEntity(),
	                        propertySheet,
	                        propertyName,
	                        currentValue,
	                        newValue, 
	                        swapList);

	            } else {

	                // we only want to update the current entity
	                swapList.add((PositionableEntity)entity);
	                
	                // make sure all children are dealt with
                    findComplexEntities(
                            entity,
                            propertySheet,
                            propertyName,
                            currentValue,
                            newValue, 
                            swapList);
             
	            }
	            
	            // now process each complex product
	            for (int i = 0; i < swapList.size(); i++) {

	                PositionableEntity complexEntity = swapList.get(i);
	                
                    // update the complex entity
                    updateComplexProduct(
                            model,
                            complexEntity,
                            null, 
                            propertySheet,
                            propertyName,
                            currentValue,
                            newValue);
                    
	            }
	            
	            // send out zone selection notification if the current zone
	            // is a product zone
	            LocationEntity locationEntity = view.getActiveLocationEntity();	  
	            
	            // check the entity to see if the zone is a product 
	            // zone, and if it is then call the command
	            Entity zone = 
	                model.getEntity(locationEntity.getActiveZoneID());
	            
	            if (zone.getType() == Entity.TYPE_MODEL_ZONE) {
	                
	                   SelectZoneCommand zoneCommand = 
	                        new SelectZoneCommand(
	                                locationEntity, 
	                                locationEntity.getActiveZoneID());
	                    CommandSequencer.getInstance().addNewlyIssuedCommand(zoneCommand);

	            }
	        }	        	        
		}

		return result;

	}

	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------

	/**
	 * Process the entity provided by gathering the new sub parts to add
	 * based on the new combination.  Then swap those sub parts with the
	 * current set.
	 *
	 * @param model The world model that holds the data
	 * @param complexEntity The complex entity being updated
	 * @param propertySheet The property sheet that holds the changing property
	 * @param propertyName The property that is changing
	 * @param currentValue The current value of the property changing
	 * @param newValue The new value of the property changing
	 */
	private void updateComplexProduct(
	        WorldModel model,
	        PositionableEntity complexEntity,
	        PositionableEntity parentEntity,
	        String propertySheet,
	        String propertyName,
	        String currentValue,
	        String newValue) {

	    // get the combination to use
        String combination =
            generateCurrentCombination(
                    complexEntity,
                    propertySheet,
                    propertyName,
                    newValue);
        
        if (combination == null)
        	return;
        
        // get the mapping of old products to new
        Map<PositionableEntity, PositionableEntity> swapMap = 
            generateSwapMap(
                    complexEntity,
                    combination, 
                    propertySheet,
                    propertyName,
                    currentValue, 
                    newValue);
        
        // swap the models
        Iterator<Map.Entry<PositionableEntity, PositionableEntity>> itr =
            swapMap.entrySet().iterator();
        
        while (itr.hasNext()) {

            Map.Entry<PositionableEntity, PositionableEntity> entry = itr.next();
            PositionableEntity swapOutEntity = entry.getKey();
            PositionableEntity swapInEntity = entry.getValue();

            // transfer vital properties and data
            transferProperties(swapOutEntity, swapInEntity);
                    
            // pull them all out before doing the swap so that we don't end up 
            // with extra sub products in the command list
            List<Entity> children = 
                SceneHierarchyUtility.getExactChildren(swapOutEntity);
            
            for (int i = 0; i < children.size(); i++) {

                Entity child = children.get(i);   
                
                // check if the entity is a complex product
                Boolean isComplexSubProduct =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);
                       
                if (isComplexSubProduct) {
                    
                    ExpertSceneManagementUtility.removedDeadCommands(
                            model, collisionChecker, child);
                                          
                } 

                
            }
                                  
            boolean processed = false;
            for (int i = 0; i < processedEntities.size(); i++) {

                Entity check = processedEntities.get(i);   

                processed = SceneHierarchyUtility.isEntityChildOfParent(
                        model,                        
                        swapOutEntity, 
                        check, 
                        false);
                
                if (processed)
                    break;
                
            }

            if (processed) {
                
                // just clean up the commands
                ExpertSceneManagementUtility.removedDeadCommands(
                        model, collisionChecker, swapOutEntity);

            } else {
                
                // remove the child and the pending/newly issued commands
                SceneManagementUtility.removeChild(
                        model, 
                        collisionChecker, 
                        swapOutEntity, 
                        true); 
            }
            
            // get the parent
            PositionableEntity parent = 
                (PositionableEntity)SceneHierarchyUtility.getExactParent(
                      model, swapOutEntity);
                            
            // create the add command and add it to the process stack
            SceneManagementUtility.addChild(
                    model,
                    collisionChecker,
                    swapInEntity,
                    parent,
                    true);
            
            // now copy over children
            SceneManagementUtility.copyChildren(
                    model, 
                    collisionChecker, 
                    view, 
                    catalogManager, 
                    swapOutEntity, 
                    swapInEntity, 
                    null, 
                    null,
                    false, 
                    true, 
                    true, 
                    true, 
                    true, 
                    true, 
                    true);
            
            if (swapOutEntity.isSelected()) {
       
                // make sure the entity is selected
                SelectEntityCommand selectCmd = 
                    new SelectEntityCommand(model, swapInEntity, true);
                CommandSequencer.getInstance().addNewlyIssuedCommand(selectCmd);

            }
            
            // current all auto-add children are not copied over, so we need to 
            // add back any we want to support.
            EntityBuilder entityBuilder = view.getEntityBuilder();
            AutoAddInvisibleChildrenUtility.addInvisibleChildren(
            		model, 
            		(PositionableEntity)swapInEntity, 
            		entityBuilder, 
            		rch);
                       
            // the entity has been dealt with
            processedEntities.add(swapOutEntity);
            
            // Copy miter cut properties. Do this after the add/swap command
            // so that the issuing of a changeProperty command to correct the
            // starting position will be added to the list after the add/swap.
            transferMiterCutProperties(swapOutEntity, swapInEntity);
                        
        }
        
        // update the property sheet
        setCurrentOptions(complexEntity, combination, true);

	}
    
	/**
	 * Transfer vital properties from the swap out to the swap in entity.  Will
	 * copy the internal properties, editable properties, and the position and
	 * rotation values.
	 * 
	 * @param swapOutEntity The entity being removed
	 * @param swapInEntity The entity being added
	 */
	private void transferProperties(
	        PositionableEntity swapOutEntity,
	        PositionableEntity swapInEntity) {
	    
        // copy internal only properties
        List<String> propKeys =
            ChefX3DRuleProperties.INTERNAL_PROP_LEYS;
        for (int i = 0; i < propKeys.size(); i++) {

            // get the current value
            String propName = propKeys.get(i);
            Object propValue =
                swapOutEntity.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        propName);

            // set the value in the new entity
            swapInEntity.addProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    propName,
                    propValue);
        }

        // copy editable properties
        List<EntityProperty> editableProps =
            swapOutEntity.getProperties(Entity.EDITABLE_PROPERTIES);
        for (int j = 0; j < editableProps.size(); j++) {

            EntityProperty prop = editableProps.get(j);
            swapInEntity.addProperty(
                    prop.propertySheet,
                    prop.propertyName,
                    prop.propertyValue);

        }

        // mark the item as a sub part
        RulePropertyAccessor.setRuleProperty(
                swapInEntity,
                ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART,
                new Boolean(true));

        // if the product is also a complex product then we need
        // copy over the position information
        Boolean isComplexProduct =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    swapOutEntity,
                    ChefX3DRuleProperties.IS_COMPLEX_PRODUCT);

        if (isComplexProduct) {

            double[] pos = new double[3];
            swapOutEntity.getPosition(pos);
            swapInEntity.setPosition(pos, false);

            float[] scale = new float[4];
            swapOutEntity.getScale(scale);
            swapInEntity.setScale(scale);
            
            float[] rot = new float[4];
            swapOutEntity.getRotation(rot);
            swapInEntity.setRotation(rot, false);

        }        
	}
	
	/**
	 * Transfer miter cut rule properties, if they exist, from the swapOutEntity
	 * to the swapInEntity.
	 * 
	 * @param swapOutEntity Entity being swapped out
	 * @param swapInEntity Entity being swapped in
	 */
	private void transferMiterCutProperties(
			PositionableEntity swapOutEntity,
			PositionableEntity swapInEntity) {
		
		Boolean canMiterCut = (Boolean)
			RulePropertyAccessor.getRulePropertyValue(
					swapOutEntity, 
					ChefX3DRuleProperties.MITRE_CAN_CUT);
		
		// If the swapOutEntity cannot miter cut, then there is nothing to
		// do here.
		if (!canMiterCut) {
			return;
		}
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_CAN_CUT, 
				canMiterCut);
		
		// Begin setting all of the other relevant miter cut rule properties
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_CUT_SPINE_LAST_GOOD));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_CUT_VISIBLE_LAST_GOOD));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_CUT_ENABLE_LAST_GOOD, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_CUT_ENABLE_LAST_GOOD));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_SHAPE_TRANSLATION, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_SHAPE_TRANSLATION));

		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_HAS_LINE, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_HAS_LINE));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_USE_LINE, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_USE_LINE));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_AUTO, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_AUTO));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_LINE_BACK_LEFT, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_LINE_BACK_LEFT));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_LINE_FRONT_LEFT));

		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_LINE_FRONT_RIGHT));

		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ChefX3DRuleProperties.MITRE_LINE_BACK_RIGHT, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ChefX3DRuleProperties.MITRE_LINE_BACK_RIGHT));

		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ExtrusionEntity.IS_EXTRUSION_ENITY_PROP, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ExtrusionEntity.IS_EXTRUSION_ENITY_PROP));

		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ExtrusionEntity.CROSS_SECTION_TRANSLATION_PROP));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ExtrusionEntity.SPINE_VERTICES_PROP, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ExtrusionEntity.SPINE_VERTICES_PROP));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ExtrusionEntity.VISIBLE_PROP, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ExtrusionEntity.VISIBLE_PROP));
		
		RulePropertyAccessor.setRuleProperty(
				swapInEntity, 
				ExtrusionEntity.MITER_ENABLE_PROP, 
					RulePropertyAccessor.getRulePropertyValue(
						swapOutEntity, 
						ExtrusionEntity.MITER_ENABLE_PROP));
	
		// Issue a change property command to adjust the starting scale
		// and do so after the add command. AddEntityChildCommand will overwrite
		// the starting scale value with the scale value. In some cases the
		// starting scale will be different so we want to be aware of this
		// and handle that here when doing a swap
			
		float[] startingScale = new float[3];
		((PositionableEntity)swapOutEntity).getStartingScale(startingScale);
        
    	SceneManagementUtility.changeProperty(
    			model, 
    			swapInEntity, 
    			Entity.DEFAULT_ENTITY_PROPERTIES, 
    			PositionableEntity.START_SCALE_PROP, 
    			startingScale, 
    			startingScale, 
    			false);
	}
	
	/**
	 * 
	 * @param complexEntity The entity for which to inspect the children
	 * @param combination The current combination of options
     * @param propertySheet The property sheet that contains the option that 
     * was changed
     * @param propertyName The name of the option that was changed
     * @param currentValue The current value of the property that was changed
     * @param newValue The new value of the property changed
	 * @return The map of old children to new children
	 */
	private LinkedHashMap<PositionableEntity, PositionableEntity> generateSwapMap(
	        PositionableEntity complexEntity, 
	        String combination, 
            String propertySheet,
            String propertyName,
            String currentValue,
            String newValue) {
	    
        // generate the new tools to add
        List<ComplexEntityData> toolList =
            matchCombinations(combination, complexEntity, model);

        // pair the new tools to the items they are replacing,
        // assumes the child order is the correct order
        LinkedHashMap<PositionableEntity, PositionableEntity> swapMap =
            new LinkedHashMap<PositionableEntity, PositionableEntity>();

        // generate the remove commands
        int index = 0;

        // check if the entity is a complex sub part
        Boolean isComplexSubPart =
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    complexEntity,
                    ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

        if (isComplexSubPart) {
            // get the data
            ComplexEntityData complexData = toolList.get(index++);
            
            // make sure to use the same entityID
            complexData.getEntity().setEntityID(complexEntity.getEntityID());
            
            // update the map
            swapMap.put(complexEntity, complexData.getEntity());
        }

        // map current children to the new entries being added
        List<Entity> children = complexEntity.getChildren();
        for (int i = 0; i < children.size(); i++) {

            Entity child = children.get(i);

            // check if the entity is a complex sub part
            isComplexSubPart =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        child,
                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);

            // check if the entity is a complex sub part
            Boolean isComplexProduct =
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        child,
                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT);

            if (!isComplexProduct && isComplexSubPart) {
                // get the data
                ComplexEntityData complexData = toolList.get(index++);
                
                // make sure to use the same entityID
                complexData.getEntity().setEntityID(child.getEntityID());

                // update the map
                swapMap.put((PositionableEntity)child, complexData.getEntity());
            }
        }
        
        return swapMap;
        
	}

	private List<Entity> getComplexSubPartChildren(Entity entity) {
	       
	    List<Entity> subParts = new ArrayList<Entity>();
	    
	    // generate the remove commands
        List<Entity> children = SceneHierarchyUtility.getExactChildren(entity);
        if (children.size() > 0) {
                        
            for (int i = 0; i < children.size(); i++) {

                Entity child = children.get(i);

                // check if the entity is a complex product
                Boolean isComplexSubProduct =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART);
               
                if (isComplexSubProduct) {
                    
                    subParts.add(child);
                    
                    // create the remove command and add it to the process stack
                    //SceneManagementUtility.removeChild(
                    //        model, collisionChecker, child, true);

                }
            }
        }
        
        return subParts;
        

	}
	
	/**
	 * Add the correct set of sub parts for the add case
	 *
	 * @param combination The space separated index list
	 * @param entity The primary complex product entity
	 * @param model The world model that holds the entities
	 * @return true is successful, false otherwise
	 */
	private boolean addInitialChildEntities(
	        String combination,
	        Entity entity,
	        WorldModel model,
	        Command command) {
	    
		boolean hasChildren = false;
		
	    // generate the remove commands, only needs to be done for the 
		// non-transient add child command
        List<Entity> children = getComplexSubPartChildren(entity);
        if (children.size() > 0) {
                    
        	hasChildren = true;
    
            if (!command.isTransient()) {

            	for (int i = 0; i < children.size(); i++) {

            		Entity child = children.get(i);

                    // create the remove command and add it to the process stack
                    SceneManagementUtility.removeChild(
                            model, collisionChecker, child, true);
                }
            }
        }
        
	    // add a temp object to the scene for collision analysis
	    SceneManagementUtility.addTempSurrogate(
	    		rch.getRuleCollisionChecker(), command);

        // generate the add commands
        List<ComplexEntityData> addCommandList =
            matchCombinations(combination, entity, model);

        List<Command> checkList = new ArrayList<Command>();
        
        for (int i = 0; i < addCommandList.size(); i++) {

            ComplexEntityData complexEntityData = addCommandList.get(i);
            Entity childEntity = complexEntityData.getEntity();

            // no need to replace the item during an add
            if (childEntity.getToolID().equals(entity.getToolID())) {

                // mark it as a sub part for easy identification
                RulePropertyAccessor.setRuleProperty(
                        entity,
                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART,
                        new Boolean(true));

            } else {

                // mark it as a sub part for easy identification
                RulePropertyAccessor.setRuleProperty(
                        childEntity,
                        ChefX3DRuleProperties.IS_COMPLEX_PRODUCT_SUBPART,
                        new Boolean(true));

                // mark it as a gen pos center
                RulePropertyAccessor.setRuleProperty(
                        childEntity,
                        ChefX3DRuleProperties.GENERAL_POSITION_PROP,
                        ChefX3DRuleProperties.RELATIVE_GENERAL_POSITION.CENTER);

                double[] pos = complexEntityData.getPosition();

                // update each buffer with the provided position
                RulePropertyAccessor.setRuleProperty(
                        childEntity,
                        ChefX3DRuleProperties.CENTER_HORIZONTAL_POS_BUFF_PROP,
                        new Float(pos[0]));
                RulePropertyAccessor.setRuleProperty(
                        childEntity,
                        ChefX3DRuleProperties.CENTER_VERTICAL_POS_BUFF_PROP,
                        new Float(pos[1]));
                RulePropertyAccessor.setRuleProperty(
                        childEntity,
                        ChefX3DRuleProperties.CENTER_DEPTH_POS_BUFF_PROP,
                        new Float(pos[2]));

                if (command.isTransient()) {
                	
                	// only add the children directly if there were no children 
                	// to start with.  If the entity was pasted then it would 
                	// have children and we want to maintain what they are to 
                	// get the correct combination.
                	if (!hasChildren) {
	
                    	// mark it as a shadow entity
                    	childEntity.setProperty(
                    			Entity.DEFAULT_ENTITY_PROPERTIES, 
                    			Entity.SHADOW_ENTITY_FLAG, 
                    			true, 
                    			false);

	                	// just add it as a child directly
                    	entity.addChild(childEntity);

                	}
                	
                } else {
                	                	
                    // create the add command and add it to the process stack
                    int transactionID = 
                    	SceneManagementUtility.addChild(
                            model,
                            collisionChecker,
                            childEntity,
                            entity,
                            true);
                    
                    // get the command
                    Command cmd = 
                    	CommandSequencer.getInstance().getCommand(transactionID);
                  
                    // create a temp surrogate
            	    SceneManagementUtility.addTempSurrogate(
            	    		rch.getRuleCollisionChecker(), cmd);
            	    
            	    checkList.add(cmd);
                }
            }
        }
        
        // now check the collisions of the children are valid
        if (!command.isTransient() && 
        		entity.getTemplateEntityID() < 0 && 
        		entity.getKitEntityID() < 0) { 
        	
            for (int i = 0; i < checkList.size(); i++) {
            	
            	Command cmd = checkList.get(i);
            	Entity checkEntity = ((RuleDataAccessor)cmd).getEntity();
            	       	
        		// Perform collision check
        		rch.performCollisionCheck(cmd, true, false, false);
        		
        		// Analyze class relationship data against collisions
        		// (first time needs to be performed)
        		rch.performCollisionAnalysisHelper(checkEntity, null, false, null, true);

        		if (rch.hasIllegalCollisionHelper(checkEntity)) {

        		    // If we did not find a legal collision then provide response
        	        String illegalCol = intl_mgr.getString(ILLEGAL_COL_PROP);
        	        popUpMessage.showMessage(illegalCol);

                    result.setResult(false);
                    result.setApproved(false);
                    result.setNotApprovedAction(
                    		NOT_APPROVED_ACTION.RESET_TO_START_ALL_COMMANDS);
                    return false;

        		} 

            }

        }

        return true;

	}

	/**
	 * Use the combination string to determine what children need to be created.
	 *
     * @param combination The space separated index list
     * @param entity The primary complex product entity
     * @param model The world model that holds the entities
	 * @return The list of sub part children to add
	 */
    private List<ComplexEntityData> matchCombinations(
            String combination,
            Entity entity,
            WorldModel model) {

        List<ComplexEntityData> matchedTools =
            new ArrayList<ComplexEntityData>();

        String[] comboValues = combination.split(" ");
        String[] checkValues;

        String comboValue;
        String checkValue;

        ArrayList<String> comboList =
            (ArrayList<String>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_COMBINATIONS);

        ArrayList<String> toolList =
            (ArrayList<String>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_TOOL_IDS);

        ArrayList<double[]> positionList =
            (ArrayList<double[]>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_POSITIONS);

        ArrayList<float[]> rotationList =
            (ArrayList<float[]>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_ROTATIONS);

        if (comboList == null ||
                toolList == null ||
                positionList == null ||
                rotationList == null) {
            return matchedTools;
        }

        for (int i = 0; i < comboList.size(); i++) {

            String combo = comboList.get(i);
            String toolID = toolList.get(i);
            double[] position = positionList.get(i);
            float[] rotation = rotationList.get(i);

            checkValues = combo.split(" ");

            // don't process items that don't contain a complete set
            int len = comboValues.length;
            if (len != checkValues.length) {
                continue;
            }

            boolean match = true;
            for (int j = 0; j < len; j++) {
                comboValue = comboValues[j];
                checkValue = checkValues[j];

                if (!comboValue.equals(checkValue) && !checkValue.equals("-1")) {
                    match = false;
                    break;
                }
            }

            if (match) {

                // try to lookup the tool
                Tool tool = catalogManager.findTool(toolID);
                
                if (tool != null) {

                    // create the entity
                    Entity childEntity =
                        view.getEntityBuilder().createEntity(
                                model,
                                model.issueEntityID(),
                                position,
                                rotation,
                                tool);

                    ComplexEntityData complexChild =
                        new ComplexEntityData(
                                (PositionableEntity)childEntity,
                                position,
                                rotation,
                                combo);

                    // find the spot to place the matched item
                    int index = 0;
                    boolean lessThan = false;
                    for (int j = 0; j < matchedTools.size(); j++) {
                        index = j;
                        lessThan = isLessThan(complexChild, matchedTools.get(j));
                        if (lessThan) {
                            break;
                        }
                    }

                    if (lessThan) {
                    	matchedTools.add(index, complexChild);
                    } else {
                    	matchedTools.add(complexChild);
                    }
                }
            }
        }

        return matchedTools;

    }
    
    /**
     * Compare the two items using the combination string.  If the index list
     * of item 1 is less than item 2 then this returns true.
     *
     * @param complexItem1
     * @param complexItem2
     * @return
     */
    private boolean isLessThan(
            ComplexEntityData complexItem1,
            ComplexEntityData complexItem2) {

        String combo1 = complexItem1.getCombination();
        String combo2 = complexItem2.getCombination();

        String[] comboList1 = combo1.split(" ");
        String[] comboList2 = combo2.split(" ");

        for (int i = 0; i < comboList1.length; i++) {

            Integer value1 = Integer.valueOf(comboList1[i]);
            Integer value2 = Integer.valueOf(comboList2[i]);

            // convert -1's to the largest value possible
            if (value1 == -1) {
                value1 = Integer.MAX_VALUE;
            }

            // convert -1's to the largest value possible
            if (value2 == -1) {
                value1 = Integer.MAX_VALUE;
            }

            if (value1 < value2) {
                return true;
            }

        }

        return false;

    }

    /**
     * Fire off property updates that match the default combination specified.
     * This will set the UI lists to the correct values desired.
     *
     * @param entity The primary complex product item
     * @param combination The space separated index list
     */
    private void setCurrentOptions(
            Entity entity,
            String combination, 
            boolean notifyListeners) {
    	
    	if (combination == null)
    		return;

        // get the list of possible combinations given the current values
        ArrayList<ArrayList<String>> indexList = 
            generateComboLists(entity, combination);
        
        // get all the relevant properties and determine which one is
        // being updated
        List<EntityProperty> properties =
            entity.getProperties(Entity.EDITABLE_PROPERTIES);

        // get the names for the options so we don't use something else
        List<String> optionNames =
            (List<String>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_OPTIONS);

        String[] values = combination.split(" ");

        for (int i = 0; i < properties.size(); i++) {

            // get the property and value
            EntityProperty ep = properties.get(i);

            if (optionNames.contains(ep.propertyName)) {

                // get the allowed list
                ArrayList<String> set = indexList.get(i);
                
                // get the complete list
                String[] optionLabels =
                    (String[])entity.getProperty(
                            Entity.DEFAULT_ENTITY_PROPERTIES,
                            ChefX3DRuleProperties.COMPLEX_PRODUCT_OPTIONS + i);
                
                String[] finalLabels = new String[set.size()];
                String[] finalKeys = new String[set.size()];
                
                int count = 0;
                int actualIndex = 0;
                for (int j = 0; j < optionLabels.length; j++) {
                    
                    String index = String.valueOf(j);
                    if (set.contains(index)) {
                        
                        String label = optionLabels[j];
                        
                        finalKeys[count] = index;
                        finalLabels[count] = label;
                        
                        // the index may have changed in the number of options 
                        // has been reduced
                        if (index.equals(values[i])) {
                        	actualIndex = count;
                        }
                                              
                        count++;

                    }
                }
                                
                // update the possible list values
                ListProperty list = (ListProperty)ep.propertyValue; 
                ListProperty origList = (ListProperty)list.clone(); 
                
                list.setKeys(finalKeys);
                list.setLabels(finalLabels);                
                
                // update the selected index
                int value = Integer.valueOf(actualIndex);
                list.setValue(value);
                
                if (notifyListeners) {

                    // update the property               
                    ChangePropertyCommand chgCmd = 
                        new ChangePropertyCommand(
                                entity, 
                                ep.propertySheet,
                                ep.propertyName, 
                                origList, 
                                list, 
                                model);
                    chgCmd.setBypassRules(true);
                    
                    // add the command to the approved list, this is so it is
                    // added to the command sequence before the entity is removed.
                    // when the undo occurs the add back while happen then the 
                    // change property will be correctly set back.
                    CommandSequencer.getInstance().addApprovedCommand(chgCmd);
                    
                }
                
                // actually update the target entity property
                entity.setProperty(ep.propertySheet, ep.propertyName, list, false);

            }

        }

    }
    
    /**
     * Based on the currently selected combination prune out any options
     * that are not available.
     * 
     * @param entity The entity being updated
     * @param combination The current combination selected
     * @return The list of each option's possible keys
     */
    private ArrayList<ArrayList<String>> generateComboLists(
            Entity entity,
            String combination) {
        
        // get the list of all possible combinations
        ArrayList<String> comboList =
            (ArrayList<String>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_COMBINATIONS);

        String[] comboValues = combination.split(" ");
        String[] checkValues;

        String comboValue;
        String checkValue;

        // add the current combination as a possible
        ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < comboValues.length; i++) {
            
            // create the list to add too
            ArrayList<String> set = new ArrayList<String>();   
            
            // add to the list
            list.add(set);
            
        }  
        
        for (int i = 0; i < comboList.size(); i++) {

            String combo = comboList.get(i); 
            checkValues = combo.split(" ");

            // don't process items that don't contain a complete set
            int len = comboValues.length;
            if (len != checkValues.length) {
                continue;
            }

            int unusedCount = 0;
            for (int j = 0; j < len; j++) {
                comboValue = comboValues[j];
                checkValue = checkValues[j];
                
                if (len == 1) {
                    
                    // handles single color sets to make sure all choices are 
                    // available to pick from
                    ArrayList<String> set = list.get(j);
                    if (!set.contains(checkValue)) {
                        set.add(checkValue);
                    }                 
                
                } else if (comboValue.equals(checkValue)) {
                    
                    // handles those combinations that have the matching 
                    // selected value
                    for (int k = 0; k < len; k++) {
                        checkValue = checkValues[k];
                        if (!checkValue.equals("-1")) {
                            ArrayList<String> set = list.get(k);
                            if (!set.contains(checkValue)) {
                                set.add(checkValue);
                            } 
                        }
                    }  
                    
                } else if (checkValue.equals("-1")) {
                    
                    // handles those combinations with a -1 in the selected 
                    // value, if the unused count is 1 less than the total
                    // length then we have a single value slot and all choices
                    // should be made available
                    unusedCount++;
                }
            }
            
            // this combination only has a single choice set and so it should include
            // all of them
            if (unusedCount >= len - 1) {
                for (int j = 0; j < len; j++) {
                    checkValue = checkValues[j];
                    
                    if (!checkValue.equals("-1")) {
                        ArrayList<String> set = list.get(j);
                        if (!set.contains(checkValue)) {
                            set.add(checkValue);
                        }                   
                    }
                }
            }
        }
        
        return list;
        
    }


    /**
     * Check the children of the entity to see if they are complex products and
     * the property being updated is shared (name sheet, name, and current
     * value), if they are add them to the list.  Then recursively check their
     * children.

     * @param entity The entity to check the children of
     * @param propertySheet The property sheet holding the change
     * @param propertyName  The name of the property that is changing
     * @param currentValue The current value (before being changed)
     * @param newValue The value after being changed
     * @param swapList The complete list of complex products
     */
    private void findComplexEntities(
            Entity entity,
            String propertySheet,
            String propertyName,
            String currentValue,
            String newValue, 
            List<PositionableEntity> swapList) {

        List<Entity> children = SceneHierarchyUtility.getExactChildren(entity);
        if (children.size() > 0) {
                        
            for (int i = 0; i < children.size(); i++) {

                Entity child = children.get(i);

                // check if the entity is a complex product
                Boolean isComplexProduct =
                    (Boolean)RulePropertyAccessor.getRulePropertyValue(
                            child,
                            ChefX3DRuleProperties.IS_COMPLEX_PRODUCT);
               
                if (isComplexProduct) {

                    boolean matched = matchProperties(
                            child, 
                            propertySheet, 
                            propertyName, 
                            currentValue, 
                            newValue);
                    
                    if (matched) {
                        swapList.add((PositionableEntity)child);
                    }
                    
                } 
                    
                // only recurse if not a complex product, we only want to 
                // find the root ones
                findComplexEntities(
                        child,
                        propertySheet,
                        propertyName,
                        currentValue,
                        newValue, 
                        swapList);

            }
        }
    }
    
    private boolean matchProperties(            
            Entity entity,
            String propertySheet,
            String propertyName,
            String currentValue,
            String newValue) {
        
        // check to see if it contains the property and correct value
        ListProperty list =
            (ListProperty)entity.getProperty(
                    propertySheet,
                    propertyName);

        if (list != null) {
            String checkValue = list.getSelectedValue();

            if (checkValue.equals(currentValue)) {
                
                // check that is has the new value as well
                String[] labels = list.getLabels();
                for (int j = 0; j < labels.length; j++) {
                    if (labels[j].equals(newValue)) {
                        return true;
                    }
                }
            }
        }  
        
        return false;

    }

    /**
     * Look for ListProperty's in the editable property sheet.  Get each of the
     * lists current selected index and concatenate them into a combination
     * string.  Example 0 1 0 0
     *
     * @param entity The primary complex product item
     * @param command The command that is updating one of the list items
     * @return The return combination string, a space separated index list
     */
    private String generateCurrentCombination(
            Entity entity,
            String propertySheet,
            String propertyName,
            String propertyValue) {

        // get all the relevant properties
        List<EntityProperty> properties =
            entity.getProperties(Entity.EDITABLE_PROPERTIES);

        // get the names for the options so we don't use something else
        List<String> optionNames =
            (List<String>)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.COMPLEX_PRODUCT_OPTIONS);

        if (optionNames == null) {
        	return null;
        }
        
        List<ListProperty> complexProps = new ArrayList<ListProperty>();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            EntityProperty ep = properties.get(i);

            if (optionNames.contains(ep.propertyName)) {

                // process the value
                if (ep.propertyValue instanceof ListProperty) {

                    // add to the list
                    ListProperty propertyList =
                        (ListProperty)ep.propertyValue;
                    complexProps.add(propertyList);

                    // get the current index
                    int index =
                        Integer.valueOf(propertyList.getSelectedKey());
                    if (ep.propertySheet.equals(propertySheet) &&
                            ep.propertyName.equals(propertyName)) {

                        String[] keys = propertyList.getKeys();
                        String[] labels = propertyList.getLabels();
                        for (int j = 0; j < labels.length; j++) {
                            String key = keys[j];
                            String label = labels[j];
                            if (label.equals(propertyValue)) {
                                index = Integer.valueOf(key);
                                break;
                            }
                        }

                    }

                    builder.append(index);
                    builder.append(" ");
                }
            }
        }
        
        String combination = builder.toString().trim();
                
        // get the list of possible combinations given the current values
        ArrayList<ArrayList<String>> indexList = 
            generateComboLists(entity, combination);
        
        // now check to be sure the combination is available
        boolean matched = true;
        String[] comboValues = combination.split(" ");
        for (int i = 0; i < comboValues.length; i++) {
            String value = comboValues[i];
            ArrayList<String> availableValues = indexList.get(i);
            
            if (availableValues == null || !availableValues.contains(value)) {
                matched = false;
                break;
            }
        }

        if (!matched) {
            combination =
                (String)RulePropertyAccessor.getRulePropertyValue(
                        entity,
                        ChefX3DRuleProperties.COMPLEX_PRODUCT_DEFAULT_SELECTION);
            
            // translate any -1 to 0
            combination = combination.replaceAll("-1", "0");
        }

        return combination;

    }

}
