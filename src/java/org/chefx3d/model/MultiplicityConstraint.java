package org.chefx3d.model;

/**
 * Constraints on how many entities of a certain category may exists in a model.
 *
 * @author Alan Hudson
 */
public enum MultiplicityConstraint {
   NO_REQUIREMENT,    // No restaint on how many of a category is in the model
   SINGLETON,         // Only one of a category is allowed in the model
   ZERO_OR_ONE,       // Zero or one of a category is allowed
   ONE_OR_MORE        // One or more is allowed
};
