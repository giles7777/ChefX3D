package org.chefx3d.rules.catalog;

//External Imports
import java.util.*;

import javax.swing.JToggleButton;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.tool.ToolGroupChild;
import org.chefx3d.tool.ToolSwitch;
import org.chefx3d.toolbar.awt.CatalogFilter;
import org.chefx3d.toolbar.awt.ToolIconPanel;

/**
 * Catalog filtering rules. Looks at the objects in the scene to determine
 * which catalog products to make available to the user. Decisions are based
 * on the relationship rules.
 *
 * @author Ben Yarger
 * @version $Revision: 1.24 $
 */
public class GeneralCatalogFilter 
    implements 
        CatalogFilter, 
        EntityChildListener, 
        ModelListener, 
        EntityPropertyListener {

	private ToolGroup toolGroup;

	private ToolIconPanel iconPanel;

	private HashMap<Integer, String[]> idToClassMap;

	private boolean hideButton;

	WorldModel model;

	private boolean isEnabled;
	
	/** The current zoneID so we can ignore re-requests */
	private int currentZoneID;
	
	/** The current zone entity */
	private ZoneEntity zoneEntity;
	
	/** The type of classification currently active, ie what zone type */
	private String currentClassification;
	
	/** The current summation of all classifications in the current zone */
	private HashSet<String> zoneClassifcations;
	
	/** A flag used to skip size filtering */
	private boolean checkProductSize;
	
	
	public GeneralCatalogFilter(boolean hideButton){

		idToClassMap = new HashMap<Integer, String[]>();
		zoneClassifcations = new HashSet<String>();
		
		this.hideButton = hideButton;
		isEnabled = false;
		currentZoneID = -1;
		
		checkProductSize = true;
	}

	//---------------------------------------------------------------
	// Methods required by CatalogFilter
	//---------------------------------------------------------------
	
    /**
     * Sets the currently active group
     * 
     * @param model
     * @param toolGroup
     * @param flatPanel
     */
	public void setCurrentToolGroup(
			WorldModel model,
			ToolGroup toolGroup,
			ToolIconPanel iconPanel) {

		if(this.model != null){
			this.model.removeModelListener(this);
		}

		this.model = model;
		this.toolGroup = toolGroup;
		this.iconPanel = iconPanel;

		this.model.addModelListener(this);

		updateToolButtonStates();
	}
	
    /**
     * Refreshes the status of the currently active group
     */
    public void refreshCurrentToolGroup() {
	    updateToolButtonStates();
	}
    
    /**
     * Process the catalog to enable whatever should be enabled
     * 
     * @param The state to set, true is enabled.
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        updateToolButtonStates();
    }

	//---------------------------------------------------------------
	// Methods required by EntityChildListener
	//---------------------------------------------------------------
	public void childAdded(int parent, int child) {

		Entity entity = model.getEntity(child);

		if (entity == null)
		    return;
		
		String[] classification = (String[]) entity.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.CLASSIFICATION_PROP);

        Boolean isShadow = 
            (Boolean)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES, 
                    Entity.SHADOW_ENTITY_FLAG); 
        if (isShadow == null) {
            isShadow = false;
        }

        if(classification != null && !isShadow){
			idToClassMap.put(child, classification);
			for (int i = 0; i < classification.length; i++) {
			    zoneClassifcations.add(classification[i]);
			}			
		}
        
        if (entity.hasChildren()) {
            updateClassifcations(entity);
        }

		entity.addEntityChildListener(this);
		
		if (entity instanceof LocationEntity) {
		    entity.addEntityPropertyListener(this);
		}

		updateToolButtonStates();
		
	}
	
	public void childInsertedAt(int parent, int child, int index) {

		Entity entity = model.getEntity(child);

		if (entity == null)
		    return;
		
		String[] classification = (String[]) entity.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.CLASSIFICATION_PROP);

        Boolean isShadow = 
            (Boolean)entity.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES, 
                    Entity.SHADOW_ENTITY_FLAG); 
        if (isShadow == null) {
            isShadow = false;
        }

		if(classification != null && !isShadow){
			idToClassMap.put(child, classification);
            for (int i = 0; i < classification.length; i++) {
                zoneClassifcations.add(classification[i]);
            }
		}

        if (entity.hasChildren()) {
            updateClassifcations(entity);
        }

		entity.addEntityChildListener(this);

	    if (entity instanceof LocationEntity) {
	        entity.addEntityPropertyListener(this);
	    }

		updateToolButtonStates();
		
	}

	public void childRemoved(int parent, int child) {
             
		idToClassMap.remove(child);
		
        // copy all the current values into a local list
        zoneClassifcations.clear();  
        if (currentClassification != null) {
            zoneClassifcations.add(currentClassification);
            getAllZoneClassifcations(zoneEntity, zoneClassifcations);                    
        }

		updateToolButtonStates();
		
	}

	//---------------------------------------------------------------
	// Methods required by model listener
	//---------------------------------------------------------------
	public void entityAdded(boolean local, Entity entity) {

		String[] classification = (String[]) entity.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ChefX3DRuleProperties.CLASSIFICATION_PROP);

		if(classification != null){
			idToClassMap.put(entity.getEntityID(), classification);
		}

		entity.addEntityChildListener(this);

		updateToolButtonStates();
	}

	public void entityRemoved(boolean local, Entity entity) {

	    // clear the list since this can only be a scene removal
		idToClassMap.clear();
		updateToolButtonStates();
	}

	public void masterChanged(boolean local, long viewID) {
		// IGNORE
	}

	public void modelReset(boolean local) {
		// IGNORE
	}

	public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
		// IGNORE
	}

	
    //----------------------------------------------------------
    // Methods for EntityPropertyListener
    //----------------------------------------------------------

    public void propertiesUpdated(List<EntityProperty> properties) {
        // TODO: should probably do something with this.......
    }

    public void propertyAdded(int entityID, String propertySheet,
        String propertyName) {

    }

    public void propertyRemoved(int entityID, String propertySheet,
        String propertyName) {

    }

    public void propertyUpdated(int entityID, String propertySheet,
        String propertyName, boolean ongoing) {

        Entity entity = model.getEntity(entityID);

        if (entity instanceof LocationEntity && 
                propertyName.equals(LocationEntity.ACTIVE_ZONE_PROP)) {
            
            // update the active zone
            int zoneID = (Integer)entity.getProperty(propertySheet, propertyName);
            if (zoneID != currentZoneID) {
                currentZoneID = zoneID;
                
                zoneEntity = (ZoneEntity)model.getEntity(currentZoneID);
 
                if (zoneEntity == null)
                    return;
                
                // check the CX.class property for the floor
                String[] relClass = (String[])zoneEntity.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES, 
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);
                
                currentClassification = null;
                if (relClass != null && relClass.length > 0) {
                    currentClassification = relClass[0];
                }
                
                // copy all the current values into a local list
                zoneClassifcations.clear();  
                if (currentClassification != null) {
                    zoneClassifcations.add(currentClassification);
                    getAllZoneClassifcations(zoneEntity, zoneClassifcations);                    
                }
                
                updateToolButtonStates();
            }
            
        }
           
    }

	//---------------------------------------------------------------
	// Local methods
	//---------------------------------------------------------------
	
	/**
	 * Update the tool button states. If hideButton == true, then the
	 * buttons are added and removed from the panel to force reordering.
	 * Otherwise, they are disabled and enabled.
	 */
	private void updateToolButtonStates(){

		List<ToolGroupChild> tools;
		
		try{
			tools = Collections.synchronizedList(toolGroup.getChildren());
		} catch (NullPointerException npe){
		    // no tool group has been assigned yet
			return;
		}

		if(tools != null){
			
		    int len = tools.size();
			for(int i = 0; i < len; i++){
			    
			    ToolGroupChild toolChild = tools.get(i);
			    
				if (!isEnabled) {
	    
				    JToggleButton button = iconPanel.getButton(toolChild.getToolID());
				    if(button != null){
				        button.setEnabled(false);
				        button.revalidate();
		            }
				    
				} else {
			
				    if (toolChild instanceof ToolSwitch){
				 
	    				ToolSwitch toolSwitch = (ToolSwitch) toolChild;
	    
	    				SimpleTool tool = toolSwitch.getTool();
	    
	    				processTool(tool, toolSwitch);
	    				
	                } else if (toolChild instanceof ToolGroup){
	                    
	                    JToggleButton button = iconPanel.getButton(toolChild.getToolID());
	                    if(button != null){
	                        button.setEnabled(true);
	                        button.revalidate();
	                    }            
	                    
	                } else if (toolChild instanceof SimpleTool){
	    
	    				SimpleTool tool = (SimpleTool) toolChild;
	    
	    				processTool(tool, null);
	    				
	    			}
				    
				}
			}
		} else {
			System.out.println("No tools returned from ToolGroup for category.");
		}
	}

	/**
	 * Based on the tool passed in, set the related JToggleButton state for
	 * correct presentation to the user.
	 *
	 * @param tool SimpleTool to check
	 * @param toolSwitch ToolSwitch to use for button lookup, can be null
	 */
	private void processTool(SimpleTool tool, ToolSwitch toolSwitch){
		
	    if (tool == null)
	        return;
	    
	    HashSet<String> checkClassifcations = new HashSet<String>();
	    checkClassifcations.addAll(zoneClassifcations);
	                           
        /*
         * If NO_MODEL property is set
         */	    
        Boolean noModel = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    tool, 
                    ChefX3DRuleProperties.NO_MODEL_PROP);
        
        if (noModel) {
        	
        	JToggleButton button = iconPanel.getButton(tool.getToolID());

			if(button == null){

				if(toolSwitch != null){
					button = iconPanel.getButton(toolSwitch.getToolID());
				}
			}
			
			if(button != null){
				iconPanel.remove(button);
				iconPanel.revalidate();
			}
			
			return;
        }
        
        /*
         * If HIDE_IN_CATALOG property is set
         */
        Boolean hideInCatalog = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    tool, 
                    ChefX3DRuleProperties.HIDE_IN_CATALOG);
         
        if (hideInCatalog) {
        	
        	JToggleButton button = iconPanel.getButton(tool.getToolID());

			if(button == null){

				if(toolSwitch != null){
					button = iconPanel.getButton(toolSwitch.getToolID());
				}
			}
			
			if(button != null){
				iconPanel.remove(button);
				iconPanel.revalidate();
			}
			
			return;
        }
        
        Boolean autoSpanObj = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    tool, 
                    ChefX3DRuleProperties.SPAN_OBJECT_PROP);

        if (checkProductSize && !autoSpanObj) {
            /*
             * If too large then do not allow it to be selected
             */
            float[] size = (float[])tool.getSize();
            float[] scale = (float[])tool.getScale();
            
            if (size != null && size.length == 3 && 
                scale != null && scale.length == 3 && 
                zoneEntity != null) {
                
                size[0] *= scale[0];
                size[1] *= scale[1];
                size[2] *= scale[2];
                
                float[] bounds = new float[6];
                zoneEntity.getBounds(bounds);
                
                float[] check = new float[3];
                
                if (zoneEntity instanceof SegmentEntity) {
                    
                    check[0] = ((SegmentEntity)zoneEntity).getLength();
                    check[1] = ((SegmentEntity)zoneEntity).getHeight();
                    
                } else {
                    
                    check[0] = Math.abs(bounds[0]) + bounds[1];
                    check[1] = Math.abs(bounds[2]) + bounds[3];
                    
                }
                
                
                if (size[0] > check[0] || size[1] > check[1]) {
                    
                    JToggleButton button = iconPanel.getButton(tool.getToolID());
    
                    if(button == null){
                        if(toolSwitch != null){
                            button = iconPanel.getButton(toolSwitch.getToolID());
                        }
                    }
    
                    if(button != null){
                        if(hideButton){
                            iconPanel.remove(button);
                            button.revalidate();
                        } else {
                            button.setEnabled(false);
                            button.revalidate();
                        }
                    }
                    return;
    
                }
            }
        }

		/*
		 * 1) If there is no class relationship restriction, always
		 * show the button.
		 *
		 * 2) Look through the idToClassMap values for a match between
		 * the class relationship restriction and class objects already
		 * in the scene. If found, enable button. If not, remove the
		 * button.
		 */
        String[] requiredRelationships = 
            (String[])RulePropertyAccessor.getRulePropertyValue(
                    tool, 
                    ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);
        
		if(requiredRelationships == null){

			JToggleButton button = iconPanel.getButton(tool.getToolID());

			if(button == null){

				if(toolSwitch != null){
					button = iconPanel.getButton(toolSwitch.getToolID());
				}
			}

			if(button != null){
				if(hideButton){
					//button.setVisible(true);
					iconPanel.add(button);
					button.revalidate();
				} else {
					button.setEnabled(true);
					button.revalidate();
				}
			}

		} else {

			boolean activeButton = false;

			for(int j = 0; j < requiredRelationships.length; j++){

			    // If the relationship is of type empty then skip the rest
				if (requiredRelationships[j].toLowerCase().equals("empty")){
					
					activeButton = true;
					break;
					
				}
				
				if(requiredRelationships[j].contains(":")){
				    
		            // need to handle the multi-relationship case such as wall:floor
				
					StringTokenizer st = new StringTokenizer(requiredRelationships[j], ":");
					String token;
					
					while(st.hasMoreTokens()){

						token = st.nextToken();
						boolean matchesFound = false;
						
						if(checkClassifcations != null){
							
							for(Iterator iter = checkClassifcations.iterator(); iter.hasNext();){
								
								String check = (String) iter.next();
		                        if (check == null)
		                            continue;
				
								if(token.toLowerCase().equals(check.toLowerCase())){
									matchesFound = true;
									break;
								}
			
								if(matchesFound){
									break;
								}
							}
						}
	
						if(!matchesFound){
							activeButton = false;
							break;
                        } else if (token.equals(currentClassification)){
                            activeButton = true;
                            break;
                        } else if (!st.hasMoreTokens()){
							activeButton = true;
						}
					}
					
				} else {

				    // need to handle the single case such as floor
				    
					if(checkClassifcations != null){
						
						for(Iterator iter = checkClassifcations.iterator(); iter.hasNext();){
		
							String check = (String)iter.next();
							if (check == null)
							    continue;
							
							if(requiredRelationships[j].toLowerCase().equals(check.toLowerCase())){	
								activeButton = true;
								break;
							}
		
							if(activeButton){
								break;
							}
						}
					}
				}

				if(activeButton){
					break;
				}
			}
			
			if(activeButton){
			    
				JToggleButton button = iconPanel.getButton(tool.getToolID());

				if(button == null){

					if(toolSwitch != null){
						button = iconPanel.getButton(toolSwitch.getToolID());
					}
				}

				if(button != null){
					if(hideButton){
						//button.setVisible(true);
						iconPanel.add(button);
						button.revalidate();
					} else {
						button.setEnabled(true);
						button.revalidate();
					}
				}

			} else {

				JToggleButton button = iconPanel.getButton(tool.getToolID());

				if(button == null){

					if(toolSwitch != null){
						button = iconPanel.getButton(toolSwitch.getToolID());
					}
				}

				if(button != null){
					if(hideButton){
						iconPanel.remove(button);
						button.revalidate();
					} else {
						button.setEnabled(false);
						button.revalidate();
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param entity
	 * @param zoneClassifcations
	 */
	private void getAllZoneClassifcations(Entity entity, HashSet<String> zoneClassifcations) {
	    
	    if (entity == null)
	        return;
	    
	    ArrayList<Entity> children = entity.getChildren();
	    for (int i = 0; i < children.size(); i++) {
	        // get the child's classifications
	        Entity child = children.get(i);	   
	        int id = child.getEntityID();	
	        
	        Boolean isShadow = 
	            (Boolean)child.getProperty(
	                    Entity.DEFAULT_ENTITY_PROPERTIES, 
	                    Entity.SHADOW_ENTITY_FLAG); 
	        if (isShadow == null) {
	            isShadow = false;
	        }
	        
	        String[] classification = idToClassMap.get(id);
	        if (classification != null && !isShadow) {
    	        for (int j = 0; j < classification.length; j++) {
    	            zoneClassifcations.add(classification[j]);
    	        }
	        }
	        
	        // recurse
	        getAllZoneClassifcations(child, zoneClassifcations);
	        
	    }
	    
	}
	
	/**
     * Recurse the list of children and add any classifications to the list
     * 
     * @param entity
     */
    private void updateClassifcations(Entity entity) {
        
        int len = entity.getChildCount();
        for (int i = 0; i < len; i++) {
            Entity child = entity.getChildAt(i);
            
            if (child == null)
                return;
                
            String[] classification = 
                (String[])child.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);
            
            Boolean isShadow = 
                (Boolean)child.getProperty(
                        Entity.DEFAULT_ENTITY_PROPERTIES, 
                        Entity.SHADOW_ENTITY_FLAG);
            
            if (isShadow == null) {
                isShadow = false;
            }

            if(classification != null && !isShadow){
                idToClassMap.put(child.getEntityID(), classification);
                for (int j = 0; j < classification.length; j++) {
                    zoneClassifcations.add(classification[j]);
                }           
            }
            
            if (child.hasChildren()) {
                updateClassifcations(child);
            }

        }

    }


    /**
     * @return the ignoreSize
     */
    public boolean isCheckProductSize() {
        return checkProductSize;
    }

    /**
     * @param ignoreSize the ignoreSize to set
     */
    public void setCheckProductSize(boolean checkProductSize) {
        this.checkProductSize = checkProductSize;
    }
    
}
