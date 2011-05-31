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

package org.chefx3d.rules.util;

//External Imports
import java.util.ArrayList;

import javax.vecmath.Vector3d;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.view.common.EditorView;

/**
* Shared routines related installation position requirements.
*
* @author Ben Yarger
* @version $Revision: 1.3 $
*/
public abstract class InstallPositionRequirementUtility {
	
    /**
     * Magic does not exist (DNE) value.
     * Special value used to denote no position specific requirement exists
     * for matched relationship. All three x,y,z values will be set to this
     * for the case to be ignored.
     */
    private static double MAGIC_DNE_VALUE = -1000.0;

    /**
     * Checks to see if the analysisEntity has imposed collision requirements
     * on it by other entities colliding with it.
     *
     * @param model WorldModel to reference
     * @param command Command adjusting analysisEntity
     * @param analysisEntity Entity to perform the checks against
     * @param rch RuleCollisionHandler to reference
     * @param view EditorView to reference
     * @return True if there are imposed requirements, false otherwise
     */
    public static boolean hasPositionCollisionRequirementImposed(
            WorldModel model,
            Command command,
            Entity analysisEntity,
            RuleCollisionHandler rch,
            EditorView view) {

        if (!(analysisEntity instanceof PositionableEntity)) {
            return false;
        }

        Entity startingEntity = null;
        Entity endingEntity = null;
        double[] startPosition = new double[3];
        double[] endPosition = new double[3];

        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand)command).getEndPosition(endPosition);
            ((MoveEntityCommand)command).getStartPosition(startPosition);

            ((MoveEntityCommand)command).setEndPosition(startPosition);

        } else if (command instanceof MoveEntityTransientCommand) {

            ((MoveEntityTransientCommand)command).getPosition(endPosition);
            ((PositionableEntity)analysisEntity).getStartingPosition(
                    startPosition);

            ((MoveEntityTransientCommand)command).setPosition(startPosition);

        } else if (command instanceof TransitionEntityChildCommand) {

            endingEntity =
                ((TransitionEntityChildCommand)command).getEndParentEntity();
            startingEntity =
                ((TransitionEntityChildCommand)command).getStartParentEntity();

            ((TransitionEntityChildCommand)command).getEndPosition(
                    endPosition);
            ((TransitionEntityChildCommand)command).getStartPosition(
                    startPosition);

            ((TransitionEntityChildCommand)command).setEndParentEntity(
                    startingEntity);
            ((TransitionEntityChildCommand)command).setEndPosition(
                    startPosition);

        } else {
            return false;
        }

        // Gather our collision set
        rch.performCollisionCheck(command, true, true, true);

        // Add the start position as a surrogate for looped checks
        SceneManagementUtility.addSurrogate(
        		rch.getRuleCollisionChecker(), command);

        ArrayList<Entity> testSubjects = new  ArrayList<Entity>();
        if (rch.collisionEntities != null) {
            testSubjects = new ArrayList<Entity>(rch.collisionEntities);
        }
        
        boolean result = false;

        for (int i = 0; i < testSubjects.size(); i++) {

            Entity collisionEntity = testSubjects.get(i);

            // Don't bother with non positionable entities
            if (!(collisionEntity instanceof PositionableEntity)) {
                continue;
            }

            // Don't bother with entities that don't use collision
            // positioning.
            Boolean usesPositionRule = (Boolean)
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.COLLISION_POSITION_REQUIREMENTS);

            if (usesPositionRule == null || usesPositionRule == false) {
                continue;
            }

            // Create our reset position command
            double[] position = new double[3];
            ((PositionableEntity)collisionEntity).getPosition(position);

            MoveEntityCommand tmpMvCmd =
                new MoveEntityCommand(
                        model,
                        model.issueEntityID(),
                        (PositionableEntity)collisionEntity,
                        position,
                        position);

            // Perform collision check to see what we are working with.
            // Requires doing collision analysis
            rch.performCollisionCheck(tmpMvCmd, true, false, false);

            String[] classRelationship = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.RELATIONSHIP_CLASSIFICATION_PROP);

            int[] relationshipAmount = (int[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.RELATIONSHIP_AMOUNT_PROP);

            Enum[] relModifier = (Enum[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.RELATIONSHIP_MODIFIER_PROP);

            // Perform collision analysis, if returns false it is because
            // requisite data could not be extracted.
            if(!rch.performCollisionAnalysisHelper(
                    collisionEntity,
                    null,
                    false,
                    null,
                    true)){

                continue;
            }

            // If no collisions exist, don't process any further
            if(rch.collisionEntities == null || rch.collisionEntities.size() == 0){

                continue;
            }

            // If illegal collision results exist don't process any further
            if (rch.hasIllegalCollisionHelper(collisionEntity)) {

            	result = true;
            	break;

            }

            // Retrieve the legal classification index. If -1 stop execution
            int legalIndex =
                rch.hasLegalCollisions(
                		rch.getCollisionResults().getEntityMatchCountMap(),
                        classRelationship,
                        relationshipAmount,
                        relModifier);

            if (legalIndex < 0) {

                legalIndex =
                    rch.hasLegalCollisions(
                    		rch.getCollisionResults().getWallMatchCountMap(),
                            classRelationship,
                            relationshipAmount,
                            relModifier);

                if(legalIndex < 0){

                    legalIndex =
                        rch.checkCrossCategoryCollisions(
                                classRelationship,
                                relationshipAmount,
                                relModifier);

                    if (legalIndex < 0) {

                        continue;
                    }
                }
            }

            // Get the starting and ending index of position values that match
            // the relationship being analyzed.
            String classRelationshipVal = classRelationship[legalIndex];
            int relationshipAmountVal = relationshipAmount[legalIndex];

            int startPosIndex =
                calculateStartPosIndex(
                    classRelationship,
                    relationshipAmount,
                    legalIndex);

            int endPosIndex =
                calculateEndPosIndex(
                        classRelationship,
                        relationshipAmount,
                        legalIndex);

            // Grab required data sets
            float[] xPosValues = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.COLLISION_POSITION_X_AXIS_VALUES);

            float[] yPosValues = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.COLLISION_POSITION_Y_AXIS_VALUES);

            float[] zPosValues = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.COLLISION_POSITION_Z_AXIS_VALUES);

            float[] posTolerance = (float[])
                RulePropertyAccessor.getRulePropertyValue(
                    collisionEntity,
                    ChefX3DRuleProperties.COLLISION_POSITION_TOLERANCE);

            Enum[] targetAdjustmentAxis = (Enum[])
                RulePropertyAccessor.getRulePropertyValue(
                        collisionEntity,
                        ChefX3DRuleProperties.COLLISION_TARGET_ADJUSTMENT_AXIS);

            if(xPosValues == null ||
                    yPosValues == null ||
                    zPosValues == null ||
                    posTolerance == null ||
                    targetAdjustmentAxis == null){

                continue;
            }

            // Generate the precise list of relationships to satisfy
            String[] relationships =
                buildFullRelationshipList(
                        classRelationshipVal,
                        relationshipAmountVal);

            // Generate the position collision data objects for evaluation
            PositionCollisionData[] posColData =
                buildPositionCollisionData(
                    model,
                    collisionEntity,
                    view,
                    xPosValues,
                    yPosValues,
                    zPosValues,
                    posTolerance,
                    targetAdjustmentAxis,
                    startPosIndex,
                    endPosIndex,
                    relationships,
                    rch);

            // Make sure we got position collision data
            if(posColData == null){

                continue;
            }

            for (int w = 0; w < posColData.length; w++) {
                if (posColData[w].getEntity() == analysisEntity) {
                    result = true;
                    break;
                }
            }
            
            if (result) {
            	break;
            }
        }
        
        // Reset values
        if (command instanceof MoveEntityCommand) {

            ((MoveEntityCommand)command).setEndPosition(endPosition);

        } else if (command instanceof MoveEntityTransientCommand) {

            ((MoveEntityTransientCommand)command).setPosition(endPosition);

        } else if (command instanceof TransitionEntityChildCommand) {

            ((TransitionEntityChildCommand)command).setEndParentEntity(
                    endingEntity);
            ((TransitionEntityChildCommand)command).setEndPosition(
                    endPosition);

        }

        // Clean up addSurrogate addition just before loop procedure
        SceneManagementUtility.removeSurrogate(
        		rch.getRuleCollisionChecker(), 
        		(PositionableEntity)analysisEntity);
        
        return result;
    }
    
    /**
     * Determine the starting index for position value analysis
     *
     * @param classRelationships Assigned class relationships
     * @param relationshipAmounts Assigned relationship amounts
     * @param matchingClassIndex Legal class relationship index
     * @return starting position index to work from
     */
    public static int calculateStartPosIndex(
            String[] classRelationships,
            int[] relationshipAmounts,
            int matchingClassIndex){

        int startIndex = 0;

        for(int i = 0; i < matchingClassIndex; i++){

            if(classRelationships[i].contains(":")){

                String[] splitList = classRelationships[i].split(":");

                startIndex += (splitList.length * relationshipAmounts[i]);

            } else {

                startIndex += relationshipAmounts[i];
            }
        }

        return startIndex;
    }
    
    /**
     * Determine the ending index for position value analysis
     *
     * @param classRelationships Assigned class relationships
     * @param relationshipAmounts Assigned relationship amounts
     * @param matchingClassIndex Legal class relationship index
     * @return starting position index to work from
     */
    public static int calculateEndPosIndex(
            String[] classRelationships,
            int[] relationshipAmounts,
            int matchingClassIndex){

        int endIndex = 0;

        for(int i = 0; i < matchingClassIndex+1; i++){

            if(classRelationships[i].contains(":")){

                String[] splitList = classRelationships[i].split(":");

                endIndex += (splitList.length * relationshipAmounts[i]);

            } else {

                endIndex += relationshipAmounts[i];
            }
        }

        return endIndex;
    }
    
    /**
     * Build the full relationship set that we need to line up positions with.
     * Splits list up into string array.
     *
     * @param classRelationship Single class relationship satisfied by
     * collisions
     * @param relationshipAmount Single relationship amount tied to
     * classRelationship
     * @return String[] of relationships
     */
    public static String[] buildFullRelationshipList(
            String classRelationship,
            int relationshipAmount){

        String[] relationships;

        if(classRelationship.contains(":")){

            String[] tokens = classRelationship.split(":");
            relationships =
                new String[tokens.length*relationshipAmount];

            for(int i = 0; i < relationships.length; i++){

                relationships[i] = tokens[((int)(i/relationshipAmount))];
            }

        } else {

            relationships = new String[relationshipAmount];

            for(int i = 0; i < relationships.length; i++){

                relationships[i] = classRelationship;
            }
        }

        return relationships;
    }
    
    /**
     * Generates the PositionCollisionData[] set used by all command
     * processing routines.
     *
     * @param model WorldModel to reference
     * @param entity Entity with specific relative positions to check
     * @param view EditorView to reference
     * @param xPosValues X axis position set
     * @param yPosValues Y axis position set
     * @param zPosValues Z axis position set
     * @param posTolerance Position tolerance set
     * @param targetAdjustmentAxis Target adjustment axis set
     * @param startPosIndex Position data start index
     * @param endPosIndex Position data end index
     * @param relationships Relationships to match against
     * @param rch RuleCollisionHandler to reference
     * @return PositionCollisionData[] or null if not able to generate it
     */
    public static PositionCollisionData[] buildPositionCollisionData(
            WorldModel model,
            Entity entity,
            EditorView view,
            float[] xPosValues,
            float[] yPosValues,
            float[] zPosValues,
            float[] posTolerance,
            Enum[] targetAdjustmentAxis,
            int startPosIndex,
            int endPosIndex,
            String[] relationships,
            RuleCollisionHandler rch){

        PositionCollisionData[] posColData =
            new PositionCollisionData[relationships.length];

        ArrayList<Entity> loopSet = new ArrayList<Entity>();

        ArrayList<Entity> entityMatches =
        	rch.getCollisionResults().getEntityMatches();

        ArrayList<Entity> wallEntityMatches =
        	rch.getCollisionResults().getWallEntityMatches();

        if (entityMatches.size() > 0) {
            loopSet = new ArrayList<Entity>(entityMatches);
        } else if (wallEntityMatches.size() > 0) {
            loopSet = new ArrayList<Entity>(wallEntityMatches);
        }

        // Fill up the PositionCollisionData array with position matched data.
        for(int i = 0; i < loopSet.size(); i++){

            Entity entityMatch = loopSet.get(i);

            String[] entityMatchClass = (String[])
                RulePropertyAccessor.getRulePropertyValue(
                        entityMatch,
                        ChefX3DRuleProperties.CLASSIFICATION_PROP);

            // Loop through the relationship parameter and attempt to match
            // against a classification of the current entity collision under
            // analysis. If we have a match either create the position
            // collision data set, or update it if the distance is less than
            // the one in existence.
            for(int j = 0; j < relationships.length; j++){

                // NPE safety check
                int checkIndex = startPosIndex + j;

                if (checkIndex >= xPosValues.length ||
                        checkIndex >= yPosValues.length ||
                        checkIndex >= zPosValues.length ||
                        checkIndex >= posTolerance.length ||
                        checkIndex >= targetAdjustmentAxis.length) {

                    return null;
                }

                // Perform special index check to see if the MAGIC_DNE_VALUE
                // applies to the positions set. If it does, then we ignore
                // any position requirements for this relationship and don't
                // bother creating position collision data for it.
                if (xPosValues[checkIndex] == MAGIC_DNE_VALUE &&
                        yPosValues[checkIndex] == MAGIC_DNE_VALUE &&
                        zPosValues[checkIndex] == MAGIC_DNE_VALUE) {

                    continue;
                }

                for(int w = 0; w < entityMatchClass.length; w++){

                    if(relationships[j].equals(entityMatchClass[w])){

                        if(posColData[j] == null ||
                                !posColData[j].hasEntity()){

                            posColData[j] = new PositionCollisionData();

                            double[] distance = new double[3];

                            if(checkPositionProximity(
                                    entity,
                                    entityMatch,
                                    model,
                                    view,
                                    xPosValues[checkIndex],
                                    yPosValues[checkIndex],
                                    zPosValues[checkIndex],
                                    posTolerance[checkIndex],
                                    targetAdjustmentAxis[checkIndex],
                                    distance)){

                                posColData[j].setEntity(entityMatch);
                                posColData[j].setDistance(distance);
                                posColData[j].setAdjustmentAxis(
                                        targetAdjustmentAxis[startPosIndex+j]);

                            }

                        } else {

                            double[] distance = new double[3];

                            if(checkPositionProximity(
                                    entity,
                                    entityMatch,
                                    model,
                                    view,
                                    xPosValues[checkIndex],
                                    yPosValues[checkIndex],
                                    zPosValues[checkIndex],
                                    posTolerance[checkIndex],
                                    targetAdjustmentAxis[checkIndex],
                                    distance)){

                                double[] existingDistance = new double[3];

                                posColData[j].getDistance(existingDistance);

                                Vector3d existingVec = new Vector3d(existingDistance);
                                Vector3d newVec = new Vector3d(distance);

                                if (newVec.length() < existingVec.length()) {

                                    posColData[j].setEntity(entityMatch);
                                    posColData[j].setDistance(distance);
                                    posColData[j].setAdjustmentAxis(
                                            targetAdjustmentAxis[startPosIndex+j]);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Make sure all spots have been filled. It is possible that this could
        // happen in which case we return null. Remove null entries that were
        // created when no position requirement is required.
        int dataSetSize = 0;

        for(int j = 0; j < posColData.length; j++){

            if (posColData[j] == null) {

                continue;
            }

            dataSetSize++;
        }

        PositionCollisionData[] finalPosColData =
            new PositionCollisionData[dataSetSize];

        int copyIndex = 0;

        for(int i = 0; i < posColData.length; i++){

            if (posColData[i] == null) {
                continue;
            }

            if (!posColData[i].hasEntity()) {
                return null;
            }

            posColData[i].performCanMoveEntityCheck(model, entity, rch);

            finalPosColData[copyIndex] = posColData[i];
            copyIndex++;
        }

        return finalPosColData;

    }
    
    //--------------------------------------------------------------------------
    // Private methods
    //--------------------------------------------------------------------------
    
    
    
    /**
     * Checks the position requirement of the original entity against its
     * matched counter part. The position requirements will be evaluated
     * against the matched counter part and with regards to the adjustment
     * axis specified. The posTolerance value impacts the zoom tolerance.
     *
     * @param originalEntity Entity being evaluated for position requirements
     * @param matchedEntity Entity that fits relationship requirement
     * @param model World model to reference
     * @param view EditorView to reference
     * @param xPosRequirement X position requirement
     * @param yPosRequirement Y position requirement
     * @param zPosRequirement Z position requirement
     * @param posTolerance Tolerance value to assist w/ evaluating legal offset
     * @param adjustmentAxis Axis adjustment can be made on
     * @param offset Offset from position values
     * @return True if valid offset calculated, false otherwise
     */
    private static boolean checkPositionProximity(
            Entity originalEntity,
            Entity matchedEntity,
            WorldModel model,
            EditorView view,
            float xPosRequirement,
            float yPosRequirement,
            float zPosRequirement,
            float posTolerance,
            Enum adjustmentAxis,
            double[] offset){

        if(!(originalEntity instanceof PositionableEntity) ||
                !(matchedEntity instanceof PositionableEntity)){

            return false;
        }

        float[] matchedEntityBounds = new float[6];
        double[] matchedEntityPos = new double[3];
        double[] originalEntityPos = new double[3];
        double[] originalEntityPoint = new double[3];

        // Get bounds data
        if (matchedEntity.isModel()) {
            ((PositionableEntity)matchedEntity).getBounds(matchedEntityBounds);
        } else {
            ((SegmentEntity)matchedEntity).getLocalBounds(matchedEntityBounds);
        }

        // Handle segment entity case
        if (matchedEntity instanceof SegmentEntity) {

            float tmpTwo = matchedEntityBounds[2];
            float tmpThree = matchedEntityBounds[3];

            matchedEntityBounds[2] = matchedEntityBounds[4];
            matchedEntityBounds[3] = matchedEntityBounds[5];

            matchedEntityBounds[4] = tmpTwo;
            matchedEntityBounds[5] = tmpThree;

        }

        // Get positions relative to parent zone. Confirm same zone.
        Entity originalEntityParentZone =
            SceneHierarchyUtility.findZoneEntity(model, originalEntity);

        Entity matchedEntityParentZone =
            SceneHierarchyUtility.findZoneEntity(model, matchedEntity);

        if(originalEntityParentZone.getEntityID() !=
            matchedEntityParentZone.getEntityID()){

            return false;
        }

        originalEntityPos =
            TransformUtils.getExactRelativePosition(
                    model,
                    originalEntity,
                    originalEntityParentZone,
                    false);

        if (matchedEntity.isModel()) {
            matchedEntityPos =
                TransformUtils.getExactRelativePosition(
                        model,
                        matchedEntity,
                        matchedEntityParentZone,
                        false);
        } else {

            matchedEntityPos[0] = Math.abs(matchedEntityBounds[0]);
            matchedEntityPos[1] = Math.abs(matchedEntityBounds[2]);
            matchedEntityPos[2] = Math.abs(matchedEntityBounds[4]);
        }

        originalEntityPoint[0] = originalEntityPos[0] + xPosRequirement;
        originalEntityPoint[1] = originalEntityPos[1] + yPosRequirement;
        originalEntityPoint[2] = originalEntityPos[2] + zPosRequirement;

        // Get meaningful data out of the adjustmentAxis value
        ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS
            targetPosAdjAxis =
                (ChefX3DRuleProperties.TARGET_ADJUSTMENT_AXIS)
                adjustmentAxis;

        // Check position requirement out.
        // Based on the adjustment axis isolate values from the primary
        // Entity on the other two axis.
        double[] matchIntersectionPoint = new double[3];

        switch(targetPosAdjAxis){

        case XAXIS:
            matchIntersectionPoint[0] = matchedEntityPos[0];
            matchIntersectionPoint[1] = originalEntityPoint[1];
            matchIntersectionPoint[2] = originalEntityPoint[2];
            break;

        case YAXIS:
            matchIntersectionPoint[0] = originalEntityPoint[0];
            matchIntersectionPoint[1] = matchedEntityPos[1];
            matchIntersectionPoint[2] = originalEntityPoint[2];
            break;

        case ZAXIS:
            matchIntersectionPoint[0] = originalEntityPoint[0];
            matchIntersectionPoint[1] = originalEntityPoint[1];
            matchIntersectionPoint[2] = matchedEntityPos[2];
            break;

        }

        // Examine bounds cases
        float[] matchedBoundsZoneVal = new float[6];

        matchedBoundsZoneVal[0] =
            (float)matchedEntityPos[0] + matchedEntityBounds[0];

        matchedBoundsZoneVal[1] =
            (float)matchedEntityPos[0] + matchedEntityBounds[1];

        matchedBoundsZoneVal[2] =
            (float)matchedEntityPos[1] + matchedEntityBounds[2];

        matchedBoundsZoneVal[3] =
            (float)matchedEntityPos[1] + matchedEntityBounds[3];

        matchedBoundsZoneVal[4] =
            (float)matchedEntityPos[2] + matchedEntityBounds[4];

        matchedBoundsZoneVal[5] =
            (float)matchedEntityPos[2] + matchedEntityBounds[5];

        // Compare all axis besides z because we are working in the x y
        // plane only
        if (matchedBoundsZoneVal[0] > matchIntersectionPoint[0] ||
                matchedBoundsZoneVal[1] < matchIntersectionPoint[0] ||
                matchedBoundsZoneVal[2] > matchIntersectionPoint[1] ||
                matchedBoundsZoneVal[3] < matchIntersectionPoint[1]) {
            return false;
        }

        // Handle segment entity case
        if (matchedEntity instanceof SegmentEntity) {

            offset[0] = 0.0;
            offset[1] = 0.0;
            offset[2] = 0.0;

            return true;
        }

        // Calculate the offset vector
        Vector3d offsetVec = new Vector3d(
                matchIntersectionPoint[0] - originalEntityPoint[0],
                matchIntersectionPoint[1] - originalEntityPoint[1],
                matchIntersectionPoint[2] - originalEntityPoint[2]);

        // Calculate the threshold based on the zoom amount
        double threshold = 
        	RuleUtils.getZoomThreshold(view, (double)posTolerance);

        if(offsetVec.length() > threshold){
            return false;
        }

        offset[0] = offsetVec.x;
        offset[1] = offsetVec.y;
        offset[2] = offsetVec.z;

        return true;
    }
}
