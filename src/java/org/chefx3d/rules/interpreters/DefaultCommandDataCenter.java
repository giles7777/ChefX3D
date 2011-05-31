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

package org.chefx3d.rules.interpreters;

//External Imports
import java.util.ArrayList;
import java.util.LinkedHashMap;

//Internal Imports
import org.chefx3d.model.*;

import org.chefx3d.rules.definitions.*;
import org.chefx3d.rules.rule.RuleEngine;
import org.chefx3d.rules.engines.DefaultRuleEngine;
import org.chefx3d.rules.rule.CommandDataCenter;
import org.chefx3d.rules.rule.Rule;

import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.CheckStatusReportElevation;

import org.chefx3d.view.common.EditorView;
import org.chefx3d.view.common.RuleCollisionChecker;

/**
 * Data object for storing related objects that define Commands and
 * resolve to a specific RuleEngine. Performs lookup and rule retrieval
 * functions
 *
 * @author Ben Yarger
 * @version $Revision: 1.13 $
 */
public class DefaultCommandDataCenter implements CommandDataCenter {
	
    /** Error reporting utility */
    private ErrorReporter errorReporter;

    /** The world model, where the data is stored */
    private WorldModel model;

    /** AV3DView instance */
    private EditorView view;

    /** CommandInterpreterData ArrayList that keeps the mapped data */
    private ArrayList<CommandInterpreterData> commandDataList;

    /** Manages the status level reporting */
    private CheckStatusReportElevation statusManager;

    /**
     * Default constructor 
	 *
     * @param errorReporter The error reporter
	 * @param model The world model
     * @param view The rendering component
	 * @param statusManager The status manager
     */
    public DefaultCommandDataCenter(
        ErrorReporter errorReporter,
		WorldModel model,
        EditorView view, 
		CheckStatusReportElevation statusManager){

        // create local variables to work with
        this.errorReporter = errorReporter;
        this.model = model;
        this.view = view;
		this.statusManager = statusManager;

        commandDataList = new ArrayList<CommandInterpreterData>();

        // now define each engine required
        
        //
        // Add Entity
        //
        createAddEngine();
        
        //
        // Add Entity Transient
        //
        createAddTransientEngine();

        //
        // Position
        //
        createPositionEngine();
        
        //
        // Rotation
        //
        createRotateEngine();
        
        //
        // Scale
        //  
        createScaleEngine();

        //
        // Selection
        //
        createSelectionEngine();
        
        //
        // Vertex height property change
        //
        createVertexHeightEngine();
        
        //
        // Remove Entity
        //
        createRemoveEngine();
        
    }

    //---------------------------------------------------------------
    // Methods defined by CommandDataCenter
    //---------------------------------------------------------------
    
    /**
     * Look for a rule engine associated with the command type passed in.
     *
     * @param command The command to match
     * @return RuleEngine The associated RuleEngine, or null if no match
     */
    public RuleEngine matchCommand(Command command) {

        for(CommandInterpreterData data : commandDataList){

            if(data.containsCommand(command)){

                return data.getRuleEngine();
            }
        }

        return null;
    }
    
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------
    
     /**
     * Create the add entity engine
     */
    private void createAddEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(AddEntityCommand.class);
        commandList.add(AddEntityChildCommand.class);
        commandList.add(AddVertexCommand.class);
        commandList.add(AddSegmentCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();     
        
        ruleList.put(InitialAddPositionCorrectionRule.class.toString(), 
                new InitialAddPositionCorrectionRule(errorReporter, model, view));
        ruleList.put(InitialAddTemplateCorrectionRule.class.toString(), 
                new InitialAddTemplateCorrectionRule(errorReporter, model, view));
        ruleList.put(GlobalDepthOffsetRule.class.toString(), 
                new GlobalDepthOffsetRule(errorReporter, model, view));
        ruleList.put(AddRestrictedParentRule.class.toString(), 
                new AddRestrictedParentRule(errorReporter, model, view));
        ruleList.put(AddPermanentParentRule.class.toString(), 
                new AddPermanentParentRule(errorReporter, model, view));
        ruleList.put(ProximityPositionSnapRule.class.toString(), 
                new ProximityPositionSnapRule(errorReporter, model, view));
        ruleList.put(AutoSpanRule.class.toString(), 
                new AutoSpanRule(errorReporter, model, view));
        ruleList.put(SegmentEdgeSnapRule.class.toString(), 
                new SegmentEdgeSnapRule(errorReporter, model, view));
        ruleList.put(StackRule.class.toString(), 
                new StackRule(errorReporter, model, view));
        ruleList.put(MovementSnapsRuleGroup.class.toString(), 
                new MovementSnapsRuleGroup(errorReporter, model, view));
        ruleList.put(FreeFloatingChildRule.class.toString(), 
                new FreeFloatingChildRule(errorReporter, model, view));
        ruleList.put(ReplaceEntityRule.class.toString(), 
                new ReplaceEntityRule(errorReporter, model, view));
        ruleList.put(SegmentEdgeEntityOrientationRule.class.toString(), 
                new SegmentEdgeEntityOrientationRule(errorReporter, model, view));
        ruleList.put(ParentBorderBoundsCheck.class.toString(), 
                new ParentBorderBoundsCheck(errorReporter, model, view));
        ruleList.put(HeightPositionLimitRule.class.toString(), 
                new HeightPositionLimitRule(errorReporter, model, view));
        ruleList.put(SegmentBoundsCheckRule.class.toString(), 
                new SegmentBoundsCheckRule(errorReporter, model, view));
        ruleList.put(EdgeSnapRule.class.toString(), 
        		new EdgeSnapRule(errorReporter, model, view));
        ruleList.put(OverhangLimitRule.class.toString(), 
                new OverhangLimitRule(errorReporter, model, view));
        ruleList.put(AddEntityCheckForCollisionsRule.class.toString(), 
                new AddEntityCheckForCollisionsRule(errorReporter, model, view));
        ruleList.put(AddAutoAddRule.class.toString(), 
                new AddAutoAddRule(errorReporter, model, view));
        ruleList.put(InstallPositionRequirementRule.class.toString(), 
                new InstallPositionRequirementRule(errorReporter, model, view));
        ruleList.put(InstallPositionMultiZoneRequirementRule.class.toString(), 
                new InstallPositionMultiZoneRequirementRule(errorReporter, model, view));
        ruleList.put(AddProductZonesRule.class.toString(), 
        		new AddProductZonesRule(errorReporter, model, view));
        ruleList.put(FlagInternalPropertiesRule.class.toString(), 
                new FlagInternalPropertiesRule(errorReporter, model, view));
        ruleList.put(MultipleParentCorrectionRule.class.toString(), 
                new MultipleParentCorrectionRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                "",
                "",
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }

    /**
     * Create the transient add entity engine
     */
    private void createAddTransientEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(AddEntityChildTransientCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                "",
                "",
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }
    
    /**
     * Create the entity position engine
     */
    private void createPositionEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(MoveEntityCommand.class);
        commandList.add(MoveEntityTransientCommand.class);
        commandList.add(MoveVertexCommand.class);
        commandList.add(MoveVertexTransientCommand.class);
        commandList.add(MoveSegmentCommand.class);
        commandList.add(MoveSegmentTransientCommand.class);
        commandList.add(TransitionEntityChildCommand.class);
        commandList.add(ChangePropertyCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        ruleList.put(IsEditableRule.class.toString(), 
                new IsEditableRule(errorReporter, model, view));
        ruleList.put(InitialMovePositionCorrectionRule.class.toString(), 
                new InitialMovePositionCorrectionRule(errorReporter, model, view));
        ruleList.put(FreeFloatingChildRule.class.toString(), 
                new FreeFloatingChildRule(errorReporter, model, view));
        ruleList.put(GlobalDepthOffsetRule.class.toString(), 
                new GlobalDepthOffsetRule(errorReporter, model, view));
        ruleList.put(ChildrenCollisionCheckRule.class.toString(), 
                new ChildrenCollisionCheckRule(errorReporter, model, view));
        ruleList.put(MovementRestrictedToPlaneRule.class.toString(), 
                new MovementRestrictedToPlaneRule(errorReporter, model, view));
        ruleList.put(ProximityPositionSnapRule.class.toString(), 
                new ProximityPositionSnapRule(errorReporter, model, view));
        ruleList.put(SegmentEdgeSnapRule.class.toString(), 
                new SegmentEdgeSnapRule(errorReporter, model, view));
        ruleList.put(StackRule.class.toString(), 
                new StackRule(errorReporter, model, view));
        ruleList.put(HeightPositionLimitRule.class.toString(), 
                new HeightPositionLimitRule(errorReporter, model, view));
        ruleList.put(ReplaceEntityRule.class.toString(), 
                new ReplaceEntityRule(errorReporter, model, view));
        ruleList.put(SegmentEdgeEntityOrientationRule.class.toString(), 
                new SegmentEdgeEntityOrientationRule(errorReporter, model, view));
        ruleList.put(UpdateSegmentChildrenRelativePositionRule.class.toString(), 
                new UpdateSegmentChildrenRelativePositionRule(errorReporter, model, view));
        ruleList.put(MovementSnapsRuleGroup.class.toString(), 
                new MovementSnapsRuleGroup(errorReporter, model, view));
        ruleList.put(AutoSpanRule.class.toString(), 
                new AutoSpanRule(errorReporter, model, view));
        ruleList.put(CheckAutoSpanNeighborsRule.class.toString(), 
                new CheckAutoSpanNeighborsRule(errorReporter, model, view));
        ruleList.put(OverhangLimitRule.class.toString(), 
                new OverhangLimitRule(errorReporter, model, view));
        ruleList.put(MovementCollisionRuleGroup.class.toString(), 
                new MovementCollisionRuleGroup(errorReporter, model, view));
        ruleList.put(ParentBorderBoundsCheck.class.toString(), 
                new ParentBorderBoundsCheck(errorReporter, model, view));
        ruleList.put(MovementRestrictedParentRule.class.toString(), 
                new MovementRestrictedParentRule(errorReporter, model, view));
        ruleList.put(MovementPermanentParentRule.class.toString(), 
                new MovementPermanentParentRule(errorReporter, model, view));
        ruleList.put(MoveAutoAddRule.class.toString(), 
                new MoveAutoAddRule(errorReporter, model, view));
        ruleList.put(MoveAutoAddedEntityConstraintRule.class.toString(), 
                new MoveAutoAddedEntityConstraintRule(errorReporter, model, view));
        ruleList.put(MovementScalesAttachedChildrenRule.class.toString(), 
                new MovementScalesAttachedChildrenRule(errorReporter, model, view));
        ruleList.put(SegmentBoundsCheckRule.class.toString(), 
                new SegmentBoundsCheckRule(errorReporter, model, view));
        ruleList.put(EdgeSnapRule.class.toString(), 
        		new EdgeSnapRule(errorReporter, model, view));
        ruleList.put(MovementHasObjectCollisionsRule.class.toString(), 
                new MovementHasObjectCollisionsRule(errorReporter, model, view));
        ruleList.put(InstallPositionRequirementRule.class.toString(), 
                new InstallPositionRequirementRule(errorReporter, model, view));
        ruleList.put(InstallPositionMultiZoneRequirementRule.class.toString(), 
                new InstallPositionMultiZoneRequirementRule(errorReporter, model, view));
        ruleList.put(FlagInternalPropertiesRule.class.toString(), 
                new FlagInternalPropertiesRule(errorReporter, model, view));
        ruleList.put(MultipleParentCorrectionRule.class.toString(), 
                new MultipleParentCorrectionRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                PositionableEntity.POSITION_PROP,
                Entity.DEFAULT_ENTITY_PROPERTIES,
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }

    /**
     * Create the entity rotation engine
     */
    private void createRotateEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(RotateEntityCommand.class);
        commandList.add(RotateEntityTransientCommand.class);
        commandList.add(ChangePropertyCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        ruleList.put(IsEditableRule.class.toString(), 
                new IsEditableRule(errorReporter, model, view));
        ruleList.put(CanRotateRule.class.toString(), 
                new CanRotateRule(errorReporter, model, view));
        ruleList.put(CanRotateAlongAxisRule.class.toString(), 
                new CanRotateAlongAxisRule(errorReporter, model, view));
        ruleList.put(ChildrenCollisionCheckRule.class.toString(), 
                new ChildrenCollisionCheckRule(errorReporter, model, view));
        ruleList.put(SnapToRotationValueRule.class.toString(), 
                new SnapToRotationValueRule(errorReporter, model, view));
        ruleList.put(SnapToRotationIncrementRule.class.toString(), 
                new SnapToRotationIncrementRule(errorReporter, model, view));
        ruleList.put(SegmentBoundsCheckRule.class.toString(), 
                new SegmentBoundsCheckRule(errorReporter, model, view));
        ruleList.put(RotationHasObjectCollisionsRule.class.toString(), 
                new RotationHasObjectCollisionsRule(errorReporter, model, view));
        ruleList.put(ReplaceEntityRule.class.toString(), 
                new ReplaceEntityRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                PositionableEntity.ROTATION_PROP,
                Entity.DEFAULT_ENTITY_PROPERTIES,
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }

    /**
     * Create the entity scale engine
     */
    private void createScaleEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(ScaleEntityCommand.class);
        commandList.add(ScaleEntityTransientCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        ruleList.put(IsEditableRule.class.toString(), 
                new IsEditableRule(errorReporter, model, view));
        ruleList.put(CanScaleRule.class.toString(), 
                new CanScaleRule(errorReporter, model, view));
        ruleList.put(FreeFloatingChildRule.class.toString(), 
                new FreeFloatingChildRule(errorReporter, model, view));
        ruleList.put(PositiveScaleOnlyRule.class.toString(), 
                new PositiveScaleOnlyRule(errorReporter, model, view));
        ruleList.put(ScaleRestrictedToAxisOrPlaneRule.class.toString(), 
                new ScaleRestrictedToAxisOrPlaneRule(errorReporter, model, view));
        ruleList.put(ScaleRule.class.toString(), 
                new ScaleRule(errorReporter, model, view));
        ruleList.put(SegmentEdgeSnapRule.class.toString(), 
                new SegmentEdgeSnapRule(errorReporter, model, view));
        ruleList.put(ScaleUsesSnapsRule.class.toString(), 
                new ScaleUsesSnapsRule(errorReporter, model, view));
        ruleList.put(SegmentBoundsCheckRule.class.toString(), 
                new SegmentBoundsCheckRule(errorReporter, model, view));
        ruleList.put(ScaleBoundsCheckRule.class.toString(), 
                new ScaleBoundsCheckRule(errorReporter, model, view));
        ruleList.put(ReplaceEntityRule.class.toString(), 
                new ReplaceEntityRule(errorReporter, model, view));
        ruleList.put(OverhangLimitRule.class.toString(), 
                new OverhangLimitRule(errorReporter, model, view));
        ruleList.put(ChildPositionCorrectionRule.class.toString(), 
                new ChildPositionCorrectionRule(errorReporter, model, view));
        ruleList.put(ChildrenCollisionCheckRule.class.toString(), 
                new ChildrenCollisionCheckRule(errorReporter, model, view));
        ruleList.put(CheckAutoSpanNeighborsRule.class.toString(), 
                new CheckAutoSpanNeighborsRule(errorReporter, model, view));
        ruleList.put(ScaleHasObjectCollisionsRule.class.toString(), 
                new ScaleHasObjectCollisionsRule(errorReporter, model, view));
        ruleList.put(ScaleChangeModelRule.class.toString(), 
                new ScaleChangeModelRule(errorReporter, model, view));
        ruleList.put(ScaleAutoAddRule.class.toString(), 
                new ScaleAutoAddRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                "",
                "",
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }
 
    /**
     * Create the entity selection engine
     */
    private void createSelectionEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(SelectEntityCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        ruleList.put(SingleSelectionRule.class.toString(), 
                new SingleSelectionRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                "",
                "",
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }

    /**
     * Create the vertex entity height chnage engine
     */
    private void createVertexHeightEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(ChangePropertyCommand.class);
        commandList.add(ChangePropertyTransientCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        ruleList.put(VertexHeightPropertyChangeRule.class.toString(), 
                new VertexHeightPropertyChangeRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                VertexEntity.HEIGHT_PROP,
                Entity.EDITABLE_PROPERTIES,
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }

    /**
     * Create the entity remove engine
     */
    private void createRemoveEngine() {
        
        // List of commands to be processed by the ghost add rules list
        ArrayList<Class<? extends Command>> commandList = 
            new ArrayList<Class<? extends Command>>();
        
        commandList.add(RemoveEntityCommand.class);
        commandList.add(RemoveEntityChildCommand.class);
        commandList.add(RemoveSegmentCommand.class);
        commandList.add(RemoveVertexCommand.class);
        
        // List of rules to be checked 
        LinkedHashMap<String, Rule> ruleList = 
            new LinkedHashMap<String, Rule>();
        
        ruleList.put(CanDeleteRule.class.toString(), 
                new CanDeleteRule(errorReporter, model, view));
        ruleList.put(DeleteKitRule.class.toString(), 
                new DeleteKitRule(errorReporter, model, view));
        ruleList.put(DeleteTemplateRule.class.toString(), 
                new DeleteTemplateRule(errorReporter, model, view));
        ruleList.put(DeleteAutoAddRule.class.toString(), 
                new DeleteAutoAddRule(errorReporter, model, view));
        ruleList.put(CheckAutoSpanNeighborsRule.class.toString(), 
                new CheckAutoSpanNeighborsRule(errorReporter, model, view));
        ruleList.put(DeleteCollisionsRule.class.toString(), 
                new DeleteCollisionsRule(errorReporter, model, view));

        // Create the engine
        DefaultRuleEngine engine = 
            new DefaultRuleEngine(
                    errorReporter,  
                    view,
					statusManager, 
                    ruleList);
        
        // Bind the data together for later matching
        CommandInterpreterData data = new CommandInterpreterData(
                "",
                "",
                commandList,
                engine);
        
        // register the command list and engine
        commandDataList.add(data);
        
    }
    
}
