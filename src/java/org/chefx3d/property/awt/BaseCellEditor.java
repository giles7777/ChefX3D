/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property.awt;

//External Imports
import java.awt.*;

import javax.swing.*;

// Internal Imports
import org.chefx3d.model.EntityProperty;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.PropertyValidator;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.Entity;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.util.UnitConversionUtilities;
import org.chefx3d.util.UnitConversionUtilities.Unit;
import org.chefx3d.property.VectorValidator;


/**
 * All editors should implement this class call the validate method
 *
 * @author Russell Dodds
 * @version $Revision: 1.9 $
 */
public abstract class BaseCellEditor extends AbstractCellEditor {

    /** The property information */
    protected WorldModel model;

    /** The component used to center dialogs */
    protected Component frame;

    /** The property information */
    protected EntityProperty entityProperty;

    /** The current unit of measurement, default is meters */
    protected Unit unitOfMeasure;

    protected String unitLabel;
    
    /**
     * Creates a default table cell editor.
     *
     * @param parentFrame
     * @param entityProperty
     */
    public BaseCellEditor(
            WorldModel worldModel,
            Component parentFrame,
            EntityProperty entityProperty, 
            Unit unitOfMeasure) {

        this.model = worldModel;
        this.frame = parentFrame;
        this.entityProperty = entityProperty;
        this.unitOfMeasure = unitOfMeasure;

        unitLabel = UnitConversionUtilities.getLabel(unitOfMeasure);
                
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Perform the validation check
     *
     * @param value The text or value of the edit
     */
    protected boolean validate(Object value) {

        boolean valid = true;

        int entityID = entityProperty.entityID;
        Entity entity = model.getEntity(entityID);

        // look to see if we need to validate the data
        PropertyValidator validator = null;

		// if entity is not null, then it is main entity property.
        if (entity != null) {

			Object check =
				entity.getProperty(Entity.PROPERTY_VALIDATORS, entityProperty.propertyName);

			if (check instanceof PropertyValidator) {

				validator = (PropertyValidator)check;
			} else if (entityProperty.propertyName.equals(PositionableEntity.POSITION_PROP) ||
                entityProperty.propertyName.equals(PositionableEntity.SCALE_PROP)) {

				validator =
					new VectorValidator(VectorValidator.numberTypes.FLOAT, 3);

			} else if (entityProperty.propertyName.equals(PositionableEntity.ROTATION_PROP)) {

				validator =
					new VectorValidator(VectorValidator.numberTypes.FLOAT, 4);

			}

        }
        // if entity is null then it is sub entity property (Way point vertices, way point lines, etc).
        else {

			if (entityProperty.propertyName.equals(PositionableEntity.POSITION_PROP) ||
                entityProperty.propertyName.equals(PositionableEntity.SCALE_PROP)) {

				validator =
					new VectorValidator(VectorValidator.numberTypes.FLOAT, 3);

			} else if (entityProperty.propertyName.equals(PositionableEntity.ROTATION_PROP)) {

				validator =
					new VectorValidator(VectorValidator.numberTypes.FLOAT, 4);

			}
		}

        if (validator != null) {

        	valid = validator.validate(value);

            if (!valid) {
                JOptionPane.showMessageDialog(
                        frame,
                        validator.getMessage(),
                        "Data Validation Error",
                        JOptionPane.OK_OPTION);
            }

        }

        return valid;

    }

}
