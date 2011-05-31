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
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.chefx3d.model.Command;
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.ZoneEntity;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.rules.rule.RuleEvaluationResult;
import org.chefx3d.rules.util.SceneManagementUtility;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.ProductZoneTool;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.common.EditorView;

/**
 * If a product is flagged as having product zones, this rule creates those
 * zones and adds them to the command queue to be executed. Product zones
 * are targets on a product that can be selected to orient the product in
 * such a fashion that other products can be added to it. The actual parenting
 * of those children is to the respective product zone which is parented to
 * the product with zones that specifies it.
 *
 * A product zone is represented by an invisible box set to the dimensions
 * specified in the properties for the product with zones.
 *
 * @author Ben Yarger
 * @version $Revision: 1.18 $
 */
public class AddProductZonesRule extends BaseRule {

	/**
	 * Z axis normal faces the positive z axis to match the
	 * initial normal calculation orientation.
	 */
	private static final Vector3f zAxisNormal =
		new Vector3f(0.0f, 0.0f, 1.0f);

	/**
	 * Y axis down vector when looking at the default plane position
	 * using the zAxisNormal.
	 */
	private static final Vector3f yAxisDownVec =
		new Vector3f(0.0f, -1.0f, 0.0f);

	/**
	 * Constructor
	 *
	 * @param errorReporter Error reporter to use
	 * @param model Collision checker to use
	 * @param view AV3D view to reference
	 */
	public AddProductZonesRule (
			ErrorReporter errorReporter,
			WorldModel model,
			EditorView view) {

		super(errorReporter, model, view);

		ruleType = RULE_TYPE.INVIOLABLE;

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

		// Check if the entity has product zones.
		// If there are no zones, we are done
		Integer numZones = (Integer)
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_COUNT);

		if (numZones < 1) {
			result.setResult(true);
			return result;
		}

		// We have zones to create, so get on with it
		createZones(model, entity, numZones);

		result.setResult(true);
		return result;
	}

	//-------------------------------------------------------------------------
	// Private methods
	//-------------------------------------------------------------------------

	/**
	 * Create the new product zones by stripping out the relevant rule
	 * properties and handing that data off to addNewZone to create
	 * each new entity and issue the command for it.
	 *
	 * @param model WorldModel to reference
	 * @param entity Entity to add product zones to
	 * @param int Number of zones to add
	 */
	private void createZones(WorldModel model, Entity entity, int numZones) {

		// Start by collecting all the necessary data

		float[] zoneXPos = (float[])
		RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.PRODUCT_ZONE_POINT_X);

		float[] zoneYPos = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_POINT_Y);

		float[] zoneZPos = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_POINT_Z);

		float[] zoneXNormal = (float[])
		RulePropertyAccessor.getRulePropertyValue(
				entity,
				ChefX3DRuleProperties.PRODUCT_ZONE_NORMAL_X);

		float[] zoneYNormal = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_NORMAL_Y);

		float[] zoneZNormal = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_NORMAL_Z);

		float[] zoneWidth = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_WIDTH);

		float[] zoneHeight = (float[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_HEIGHT);

		String[] zoneName = (String[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_NAMES);

		String[] zoneClassifications = (String[])
			RulePropertyAccessor.getRulePropertyValue(
					entity,
					ChefX3DRuleProperties.PRODUCT_ZONE_CLASSIFICATIONS);

		// Create each new indexed product zone
		for (int i = 0; i < numZones; i++) {

			// Be safe about this since we believe that the number of zones
			// matches the number of indices, but we can't be 100% sure.
			try {

				addNewZone(
						model,
						entity,
						zoneXPos[i],
						zoneYPos[i],
						zoneZPos[i],
						zoneXNormal[i],
						zoneYNormal[i],
						zoneZNormal[i],
						zoneWidth[i],
						zoneHeight[i],
						zoneName[i],
						zoneClassifications[i]);

			} catch (ArrayIndexOutOfBoundsException e) {

				System.out.println(
						"AddProductZonesRule number of zones to add does " +
						"not match data. Exception Caught:");
				e.printStackTrace();
				break;
			}
		}
	}

	/**
	 * Create an individual product zone. Creates the entity, sets the
	 * properties and adds the entity to the scene via the
	 * SceneManagementUtility.
	 *
	 * @param model WorldModel to reference
	 * @param parent Parent of product zone
	 * @param xPos X axis position relative to parent
	 * @param yPos Y axis position relative to parent
	 * @param zPos Z axis position relative to parent
	 * @param xNormal X axis normal vector component
	 * @param yNormal Y axis normal vector component
	 * @param zNormal Z axis normal vector component
	 * @param width Width of product zone
	 * @param height Height of product zone
	 * @param name Name of product zone
	 * @param classification Classification name of product zone
	 */
	private void addNewZone(
			WorldModel model,
			Entity parent,
			float xPos,
			float yPos,
			float zPos,
			float xNormal,
			float yNormal,
			float zNormal,
			float width,
			float height,
			String name,
			String classification) {

		EntityBuilder entityBuilder = view.getEntityBuilder();

		int entityID = model.issueEntityID();
		double[] position = new double[] {xPos, yPos, zPos};

		// Steps to calculate the rotation...
		//
		// Note: We assume in all cases that product zones will share the
		// same y axis up orientation when editing in a product zone
		// that all other zones use. This is an important point regarding
		// how the calculations are performed, particularly, part 2.
		//
		// Part 1 - Calculate axis angle rotation
		// 1) Calculate the cross product of the normal relative to the
		// default plane normal vector (0.0f, 0.0f, 1.0f). This will
		// be the rotation axis.
		// 2) Calculate the dot product of the normal relative to the
		// default plane normal vector (0.0f, 0.0f, 1.0f). Take the acos()
		// of this result and that is the angle of the rotation.
		// 3) Combine those into a single axis angle rotation.
		//
		// Part 2 - Correct for roll
		// Because part 1 doesn't preserve the initial roll angle of
		// the default plane, we need to account for this in a second
		// axis angle rotation.
		// 1) Calculate the cross product of the flattened xy axis
		// normal values with the local down vector of the default plane
		// (0.0, -1.0, 0.0).
		// 2) Calculate the dot product of the flattened xy axis
		// normal values with the local down vector of the default plane
		// (0.0, -1.0, 0.0).
		// 3) Combine those into a single axis angle rotation.
		//
		// Part 3 - Combine all
		// Create a matrix A from part 1's axis angle rotation and create
		// a matrix B from part 2's axis angle rotation.
		// Multiple matrix A by matrix B and create a new axis angle rotation
		// from the result. This axis angle rotation is the combination of
		// Part 1 and 2 and will give us the perfect result.

		// Part 1
		Vector3f zoneNormal = new Vector3f(xNormal, yNormal, zNormal);
		zoneNormal.normalize();

		float angle = zAxisNormal.dot(zoneNormal);
		angle = (float) Math.acos(angle);

		Vector3f rotationAxis = new Vector3f();
		rotationAxis.cross(zAxisNormal, zoneNormal);
		rotationAxis.normalize();

		AxisAngle4f rot = new AxisAngle4f(
				rotationAxis.x,
				rotationAxis.y,
				rotationAxis.z,
				angle);

		// Part 2
		Vector3f xAxisComponent = new Vector3f(xNormal, yNormal, 0.0f);
		xAxisComponent.normalize();

		Vector3f zAxisRotationVector = new Vector3f();
		zAxisRotationVector.cross(yAxisDownVec, xAxisComponent);
		zAxisRotationVector.normalize();

		float zAxisRotationAngle = yAxisDownVec.dot(xAxisComponent);
		zAxisRotationAngle = (float) Math.acos(zAxisRotationAngle);

		AxisAngle4f secondRotation = new AxisAngle4f(
				zAxisRotationVector.x,
				zAxisRotationVector.y,
				zAxisRotationVector.z,
				zAxisRotationAngle);

		// Correct parallel vector cases
		if (Float.isNaN(rot.x)) {
			rot.x = zAxisNormal.x;
			rot.y = zAxisNormal.y;
			rot.z = zAxisNormal.z;
			rot.angle = 0.0f;
		} else if (Float.isNaN(secondRotation.x)) {

			if (yNormal == -1.0f) {
				secondRotation.x = 0.0f;
				secondRotation.y = 0.0f;
				secondRotation.z = 1.0f;
				secondRotation.angle = 0.0f;
			} else {
				rot.x = 0.0f;
				rot.y = 0.0f;
				rot.z = 1.0f;
				rot.angle = 3.14f;

				secondRotation.x = 1.0f;
				secondRotation.y = 0.0f;
				secondRotation.z = 0.0f;
				secondRotation.angle = (float) Math.PI/2.0f;
			}
		}

		// Part 3
		Matrix4f mat1 = new Matrix4f();
		mat1.setIdentity();
		mat1.set(rot);

		Matrix4f mat2 = new Matrix4f();
		mat2.setIdentity();
		mat2.set(secondRotation);

		mat1.mul(mat2);

		AxisAngle4f finalRot = new AxisAngle4f();
		finalRot.set(mat1);


/*
		// Debug output of incoming data and resulting calculations
		System.out.println();
		System.out.println();
		System.out.println("Create zone with following values: ");
		System.out.println("Position: "+xPos+" "+yPos+" "+zPos);
		System.out.println("Normal: "+xNormal+" "+yNormal+" "+zNormal);
		System.out.println("Width: "+width);
		System.out.println("Height: "+height);
		System.out.println("Name: "+name);
		System.out.println("Classification: "+classification);
		System.out.println("Normalized normal vec: "+zoneNormal.toString());
		System.out.println("Normalized rot vec: "+rotationAxis.toString());
		System.out.println("Position: "+Arrays.toString(position));
		System.out.println("Rotation: "+rot.toString());
		System.out.println("Second axis rotation: "+
				secondRotation.toString());
		System.out.println("Final Rotation: "+finalRot.toString());
*/

		// Assemble the official rotation to apply
		float[] rotation = new float[4];
		rotation[0] = finalRot.x;
		rotation[1] = finalRot.y;
		rotation[2] = finalRot.z;
		rotation[3] = finalRot.angle;

		// Create the properties
        Map<String, Map<String, Object>> properties =
        	new HashMap<String, Map<String, Object>>();

        Map<String, Object> entityParams =
        	new HashMap<String, Object>();
        Map<String, Object> associateProperties =
        	new HashMap<String, Object>();
        Map<String, Object> entityProperties =
        	new HashMap<String, Object>();
        Map<String, Object> propertyValidators =
        	new HashMap<String, Object>();

        // Add rule properties
        entityProperties.put(ChefX3DRuleProperties.IS_EDITABLE_PROP, false);
        entityProperties.put(ChefX3DRuleProperties.CAN_DELETE_PROP, false);
        entityProperties.put(ChefX3DRuleProperties.CAN_ROTATE_PROP, false);
        entityProperties.put(ChefX3DRuleProperties.CAN_SCALE_PROP, false);
        entityProperties.put(ChefX3DRuleProperties.CLASSIFICATION_PROP,
        		new String[] {classification});
        entityProperties.put(ChefX3DRuleProperties.HIDE_IN_CATALOG, true);
        entityProperties.put(ChefX3DRuleProperties.SHOW_IN_REPORT, false);

        // add the base properties
        properties.put(Entity.DEFAULT_ENTITY_PROPERTIES, entityProperties);
        properties.put(Entity.ENTITY_PARAMS, entityParams);
        properties.put(Entity.ASSOCIATED_ENTITIES, associateProperties);
        properties.put(Entity.PROPERTY_VALIDATORS, propertyValidators);

		// Create the tool
		ProductZoneTool tool = 
		    new ProductZoneTool(
				name,
				name,
                new float[] {1.0f, 1.0f, 1.0f},
                new float[] {width, height, 0.0f},
				properties);

		// Create the entity
		ZoneEntity productZoneEntity = (ZoneEntity)
			entityBuilder.createEntity(
				model,
				entityID,
				position,
				rotation,
				tool);

		// Add the entity if it is not null
		if (productZoneEntity != null) {

			SceneManagementUtility.addChild(
					model,
					collisionChecker,
					productZoneEntity,
					parent,
					false);
		}
	}

}
