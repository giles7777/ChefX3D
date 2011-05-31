/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.util;

// External Imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Internal Imports
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.model.*;

import org.chefx3d.rules.rule.DefaultRuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.rule.RuleEngine;
import org.chefx3d.rules.rule.CommandDataCenter;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.tool.SimpleTool;

import org.chefx3d.view.common.EditorView;

/**
 * Miscellaneous common utilities.
 *
 * @author Ben Yarger
 * @version $Revision: 1.10 $
 */
public abstract class RuleUtils {
	
    /** the list of properties to check, all values must be Boolean */
    private static List<String> autoAddPropertyNames;

    /** Constant threshold value used to calculate THRESHOLD */
    private static final double INIT_THRESHOLD = 0.25;
    
    /** Command data center to pull command execution list from */
    private static CommandDataCenter commandDataCenter;
    
    private static CatalogManager catalogMgr;
    /**
     * Set the command data center to use in the preValidateCommand method.
     * 
     * @param cdc CommandDataCenter to use
     */
    public static void setCommandDataCenter(CommandDataCenter cdc) {
    	commandDataCenter = cdc;
    }
 
    /**
     * Set the command data center to use in the preValidateCommand method.
     * 
     * @param cdc CommandDataCenter to use
     */
    public static void setCatalogManager(CatalogManager catalogManager) {
    	catalogMgr = catalogManager;
    }

    /**
     * Validate the command against its respective rule engine. This is done
     * outside of the command evaluation loop. This is provided as a means to
     * check on the execution result of a command before it is decided it should
     * be added to the queue.
     * 
     * @param cmd Command to evaluate
     * @return True if it passes rule checks, false if it failed somewhere
     */
    public static boolean preValidateCommand(
    		Command cmd) {
    	
    	// If we don't have the command data center then we can't validate 
    	// commands so return false.
    	if (commandDataCenter == null) {
    		return false;
    	}
    	
        // Look up the rule engine to use
        RuleEngine ruleEngine = commandDataCenter.matchCommand(cmd);

        // process the command through the matched rules engine.  the 
        // command is considered approved if the isApproved flag remains
        // true after all rules are processed
        if (ruleEngine != null) {
            
            // create the status result object used to track state
            RuleEvaluationResult result = new DefaultRuleEvaluationResult();

            // process the list of commands sequentially
            result = ruleEngine.processRules(cmd, result);
            
         	// Return if the approved state of the result
            return result.isApproved();
            
        }
        
        return false;
    }
    
    /**
     * Get the simple tool matching the name.
     *
     * @param name Name of tool to retrieve
     * @return SimpleTool or null if not found
     */
    public static SimpleTool getSimpleToolByName(String id){
        return (SimpleTool)catalogMgr.findTool(id);
    }

    static {
        
        // create the list of properties to check, all values must be Boolean
        autoAddPropertyNames = new ArrayList<String>();
        autoAddPropertyNames.add(ChefX3DRuleProperties.AUTO_ADD_BY_SPAN_USE);
        autoAddPropertyNames.add(ChefX3DRuleProperties.AUTO_ADD_BY_COL_USE);
        autoAddPropertyNames.add(ChefX3DRuleProperties.AUTO_ADD_N_UNITS_PROP_USE);
        autoAddPropertyNames.add(ChefX3DRuleProperties.AUTO_ADD_ENDS_PLACEMENT_USE);
        //autoAddPropertyNames.add(ChefX3DRuleProperties.USE_INVISIBLE_CHILDREN);

    }
    
    /**
     * Check the requirements on the found relationship for the correct
     * number and modifier value.
     *
     * @param numCollisions Number of collisions found
     * @param relNumber Number of relationship collisions required
     * @param relModifier Modifier on relationship collisions required
     * @return boolean True if legal, false otherwise
     */
    public static boolean legalAssociationNumber(
            Integer numCollisions,
            int relNumber,
            ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_VALUES relModifier){

        if(numCollisions == null){
            return false;
        }

        boolean legalAssociation = false;

        switch(relModifier){

            case LESS_THAN:
                if(numCollisions < relNumber){
                    legalAssociation = true;
                }
                break;

            case GREATER_THAN:
                if(numCollisions > relNumber){
                    legalAssociation = true;
                }
                break;

            case LESS_THAN_OR_EQUAL_TO:
                if(numCollisions <= relNumber){
                    legalAssociation = true;
                }
                break;

            case GREATER_THAN_OR_EQUAL_TO:
                if(numCollisions >= relNumber){
                    legalAssociation = true;
                }
                break;

            case EQUAL_TO:
                if(numCollisions == relNumber){
                    legalAssociation = true;
                }
                break;

            case NOT_EQUAL_TO:
                if(numCollisions != relNumber){
                    legalAssociation = true;
                }
                break;

            case NONE:
                legalAssociation = true;
                break;
        }

        return legalAssociation;
    }
	
    /**
     * Finds the closest matching value in the set.
     *
     * @param originalValue Value to find nearest match to in values set
     * @param values Associated array of values to compare against
     * @return double Closest value match from the set
     */
    public static double findClosestValue(double originalValue, float[] values){

        Arrays.sort(values);
        double newValue = values[0];

        // start from the beginning of the ordered snap values and see at what
        // point the original value is less than the snap the snap.
		
        for(int i = 1; i < values.length; i++){

             if(originalValue > ((values[i] - values[i-1])/2.0f + values[i-1])){

                 newValue = values[i];
             } else {
                 
                 if (i < values.length) {
                	 int b = 0;
                	 b++;
                 }
                 break;
             }
        }

        return newValue;
    }
	
    /**
     * Calculate the zoom threshold for tolerance calculations. Zoom factor
     * will return an acceptable offset of .25 m for a distance of 10 units
     * between the zoom position and the active zone. The thresholdConst
     * can be used to implement a fixed threshold value in addition to the
     * calculated value so that a standard result of 0.0 would return whatever
     * the thresholdConst is set to and a result of 0.25 would return
     * 0.25 + thresholdConst.
     *
     * @param view Active AV3DView to get zoom value for
     * @param thresholdConst Specific threshold constant to add, can be null
     * @return threshold value
     */
    public static double getZoomThreshold(EditorView view, Double thresholdConst){

        double zoom = view.getZoneViewZoomAmount();

        if(thresholdConst != null){

            if (thresholdConst > 1.0) {
                thresholdConst = 1.0;
            } else if (thresholdConst < 0) {
                thresholdConst = 0.0;
            }

        } else {
            thresholdConst = 0.0;
        }

        double resultThreshold = zoom / 10.0 * INIT_THRESHOLD + thresholdConst;

        return resultThreshold;
    }
	
    /**
     * Check if the entity is a kit or template entity.
     * 
     * @param entity Entity to check as kit or template
     * @return True if entity is kit or template, false otherwise
     */
    public static boolean isKitOrTemplate(Entity entity) {
    	
    	if (entity.getCategory().equals("Category.Kit") ||
    			entity.getCategory().equals("Category.Template")) {
        	return true;
        }
    	
    	return false;
    }
	
    /**
     * Check if the entity can add any auto-add children by checking the 
     * specific properties associated with each type
     * 
     * @param entity Entity to check 
     * @return True if entity can have auto-add children, false otherwise
     */
    public static boolean canHaveAutoAddChildren(Entity entity) {
        
        //look through the list of possible properties
        for (int j = 0; j < autoAddPropertyNames.size(); j++) {
            
            // is the auto-add flag set to true
            Boolean hasAutoAdds = 
                (Boolean)RulePropertyAccessor.getRulePropertyValue(
                        entity, 
                        autoAddPropertyNames.get(j));
            
            // if one of possible return true
            if (hasAutoAdds != null && hasAutoAdds) {
                return true;
            }
        }

        return false;
    }

}
