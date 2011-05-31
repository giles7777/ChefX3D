/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.properties.accessors;

//External Imports

//Internal Imports
import java.util.HashSet;

/**
 * Utility class for ignoring rules
 *
 * @author Russell Dodds
 * @version $Revision: 1.14 $
 */
public class IgnoreRuleList {

    private static HashSet<String> ignoreOpenRuleList;
    private static HashSet<String> ignorePasteRuleList;
    private static HashSet<String> ignoreTemplateRuleList;

	private IgnoreRuleList() {}

    /**
     * Return the list of rules to ignore during an add
     *
     * @return Object The list to ignore
     */
    public static HashSet<String> getIgnoreOpenRuleList() {
        if (ignoreOpenRuleList == null)
            generateIgnoreOpenRuleList();

    	return ignoreOpenRuleList;
    }

    /**
     * Return the list of rules to ignore during a paste action
     *
     * @return Object The list to ignore
     */
    public static HashSet<String> getIgnorePasteRuleList() {
        if (ignorePasteRuleList == null)
            generateIgnorePasteRuleList();

        return ignorePasteRuleList;
    }

    /**
     * Return the list of rules to ignore during a add template action
     *
     * @return Object The list to ignore
     */
    public static HashSet<String> getIgnoreTemplateRuleList() {
        if (ignoreTemplateRuleList == null)
            generateIgnoreTemplateRuleList();

        return ignoreTemplateRuleList;
    }

    private static void generateIgnoreOpenRuleList() {
        ignoreOpenRuleList = new HashSet<String>();
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.SegmentBoundsCheckRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.SegmentEdgeSnapRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.InitialAddPositionCorrectionRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.InitialPositionCorrectionRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AutoPositionRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AutoAddRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AddAutoAddRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.ScaleChangeModelRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.MovementUsesAbsoluteSnapsRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.MovementUsesIncrementalSnapsRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.SegmentEdgeEntityOrientationRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AutoSpanRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AddEntityCheckForCollisionsRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.OverhangLimitRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AddRestrictedParentRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AddPermanentParentRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.InstallPositionRequirementRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.InstallPositionMultiZoneRequirementRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.MultipleParentCorrectionRule");
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.ReplaceComplexChildrenRule");    
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.ProximityPositionSnapRule");            
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.AddProductZonesRule");  
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.ReplaceEntityRule");          
        ignoreOpenRuleList.add("org.chefx3d.rules.definitions.HeightPositionLimitRule");          
    }

    private static void generateIgnorePasteRuleList() {
        ignorePasteRuleList = new HashSet<String>();
        ignorePasteRuleList.add("org.chefx3d.rules.definitions.SegmentBoundsCheckRule");
        ignorePasteRuleList.add("org.chefx3d.rules.definitions.ScaleChangeModelRule");
        ignorePasteRuleList.add("org.chefx3d.rules.definitions.ReplaceComplexChildrenRule");
        ignorePasteRuleList.add("org.chefx3d.rules.definitions.AddProductZonesRule");            
    }

    private static void generateIgnoreTemplateRuleList() {
        ignoreTemplateRuleList = new HashSet<String>();
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.SegmentBoundsCheckRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.InitialAddPositionCorrectionRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.AutoAddRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.AddAutoAddRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.ScaleChangeModelRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.SegmentEdgeEntityOrientationRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.FreeFloatingChildRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.MultipleParentCorrectionRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.ReplaceComplexChildrenRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.NudgeAutoAddRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.OverhangLimitRule");
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.AddProductZonesRule");            
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.ReplaceEntityRule");  
        ignoreTemplateRuleList.add("org.chefx3d.rules.definitions.ChildPositionCorrectionRule");  
        
   }

}
