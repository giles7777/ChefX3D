/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007
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
import java.util.HashSet;

// Local imports

/**
 * Validates a model against its rules.  Each entity has a multiplicity
 * constraint that must be meet. Other validators can implement
 * this interface for custom logic.
 *
 * @author Alan Hudson
 */
public class DefaultModelValidator implements ModelValidator {
    private HashSet<String> categoriesChecked;

    /**
     * Validate a model against its rules.
     *
     * @param model The model validate
     * @param listener A listener for validation errors.  Null values are ignored
     * @return Is the model valid
     */
    public boolean validate(WorldModel model, ValidationErrorListener listener) {
        categoriesChecked = new HashSet<String>();

        
        Entity[] entities = model.getModelData();        
        int len = entities.length;

        for (int i = 0; i < len; i++) {
            Entity entity = entities[i];

            if (entity == null) {
                // Its expected we will have gaps
                continue;
            }

            MultiplicityConstraint constraint = entity.getConstraint();

            if (constraint != MultiplicityConstraint.NO_REQUIREMENT) {
                String category = entity.getCategory();

                if (!categoriesChecked.contains(category)) {
                    int cnt = countCategory(model, category);

                    switch(constraint) {
                        case SINGLETON:
                            if (cnt != 1) {
                                String msg;

                                if (cnt < 1)
                                    msg = "Model is missing " + category;
                                else
                                    msg = "Model contains too many " + category;

                                listener.error(msg, entity);
                                return false;
                            }
                            break;
                        case ZERO_OR_ONE:
                            if (cnt > 1) {
                                String msg = "Model contains too many " + category;

                                listener.error(msg, entity);
                                return false;
                            }
                            break;
                        case ONE_OR_MORE:
                            if (cnt < 1) {
                                String msg = "Model contains too few " + category;
                                listener.error(msg, entity);

                                return false;
                            }
                        default:
                            System.out.println("Unsupported MultiplicityConstraint");
                            break;
                    }
                }

                categoriesChecked.add(category);
            }
        }

        return true;
    }


    /**
     * Count how many entities with the given name.
     *
     * @param model The model to search
     * @param name The entity name
     * @return The count
     */
    public int countEntitiesByName(WorldModel model, String name) {
        int cnt = 0;
        Entity[] entities = model.getModelData();
        int len = entities.length;

        for (int i = 0; i < len; i++) {
            Entity entity = entities[i];

            if (entity == null) {
                // Its expected we will have gaps
                continue;
            }

            if (entity.getName().equals(name))
                cnt++;
        }

        return cnt;
    }

    /**
     * Count how many entities with the given name.
     *
     * @param model The model to search
     * @param sheet The sheet to search
     * @param xpath The xpath expression to match the entity from below the sheet
     * @return The count
     */
    // TODO: fix this
    public int countEntitiesByXPath(WorldModel model, String sheet, String xpath) {
        int cnt = 0;
        Entity[] toolValues = model.getModelData();
        int len = toolValues.length;

        for (int i = 0; i < len; i++) {
            Entity td = toolValues[i];

            if (td == null) {
                // Its expected we will have gaps
                continue;
            }

            //Document props = td.getProperties(sheet);

            //if (props == null)
            //    continue;

            //Node n = XPathEvaluator.getNode(xpath, false, props);
            //if (n != null)
            //   cnt++;
        }

        return cnt;
    }

    /**
     * For a given xpath check to see if any elements begin with the match value
     * NOTE: The xpath must look for an attribute at this time
     *
     * @param model The model to search
     * @param sheet The sheet to search
     * @param xpath The xpath expression to match the entity from below the sheet
     * @param match The string to check value of
     * @return true/false
     */
    // TODO: fix this
    public boolean elementStartsWith(WorldModel model, String sheet, String xpath, String match) {
        Entity[] toolValues = model.getModelData();
        int len = toolValues.length;

        for (int i = 0; i < len; i++) {
            Entity td = toolValues[i];

            if (td == null) {
                // Its expected we will have gaps
                continue;
            }

            //Document props = td.getProperties(sheet);

            //if (props == null)
            //    continue;

            //NodeList n = XPathEvaluator.getNodeList(xpath, false, props);

            //for (int j = 0; j < n.getLength(); j++) {
            //    if ((n.item(j).getNodeValue() != null) &&
            //        (n.item(j).getNodeValue().startsWith(match))) {
            //        return true;
            //    }
            //}

        }

        return false;
    }

    /**
     * Count how many entities are of a particular category.
     *
     * @param model The model to search
     * @param category The category to count
     * @return The count
     */
    public int countCategory(WorldModel model, String category) {
        int cnt = 0;
        Entity[] entities = model.getModelData();
        int len = entities.length;

        for (int i = 0; i < len; i++) {
            Entity entity = entities[i];

            if (entity == null) {
                // Its expected we will have gaps
                continue;
            }

            if (entity.getCategory().equals(category))
                cnt++;
        }

        return cnt;
    }
}
