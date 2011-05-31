/*****************************************************************************
 *                        Web3d.org Copyright (c) 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

// External Imports
import java.util.List;
import java.util.ArrayList;

// Internal Imports
import org.chefx3d.util.CloneableProperty;

/**
 * A property that represents an association between two entities
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public class AssociateProperty extends AbstractProperty {

    /** The valid types(Tool names) */
    protected String[] validTypes;

    /** The name of the property */
    protected String propertyName;

    /**
     * Base constructor, should only be used for cloning
     */
    protected AssociateProperty() {}

    /**
     * Constructor that includes the valid associateable types
     *
     * @param validTypes
     */
    public AssociateProperty(
            String propertyName,
            String[] validTypes) {

        this.propertyName = propertyName;
        this.validTypes = validTypes;

    }

    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------

    /**
     * Create a copy of the Property
     */
    public CloneableProperty clone() {

        // Create the new copy
        AssociateProperty out = new AssociateProperty();

        out.model = model;
        out.parentEntity = parentEntity;

        out.propertyName = new String(propertyName);
        out.value = value;
        out.visible = visible;
        out.validTypes = validTypes.clone();

        return out;
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        // ignored
    }

    // ---------------------------------------------------------------
    // Methods Overridden from AbstractProperty
    // ---------------------------------------------------------------

    /**
     * Initialize the property
     *
     * @param worldModel The model that contains the scene data
     * @param entity The entity assigned to the property
     */
    public void initialize(WorldModel worldModel, Entity entity) {

        super.initialize(worldModel, entity);

    }

    /**
     * Set the value of the property
     *
     * @param value The value to set
     */
    public void setValue(Object value) {

        if (this.value instanceof Entity) {

            // grab the current value
            Entity associateEntity = (Entity)this.value;

            // check if the are any associations
            ArrayList<Entity> associateList =
                (ArrayList<Entity>)associateEntity.getProperty(
                        Entity.ASSOCIATED_ENTITIES,
                        propertyName);

            // update if necessary
            if (associateList != null && associateList.contains(this.parentEntity)) {
                associateList.remove(this.parentEntity);

                associateEntity.setProperty(
                        Entity.ASSOCIATED_ENTITIES,
                        propertyName,
                        associateList, false);
            }

        }

        super.setValue(value);

    }

    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

    /**
     * @return the validTypes
     */
    public String[] getValidTypes() {
        return validTypes;
    }

    /**
     * @param validTypes the validTypes to set
     */
    public void setValidTypes(String[] validTypes) {
        this.validTypes = validTypes;
    }

}
