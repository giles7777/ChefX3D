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

package org.chefx3d.model;

// external imports
// none

// internal imports
// none

/**
 * Defines the requirements for a model type entity that
 * will require special processing (extrusion) of the model.
 *
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public interface ExtrusionEntity {

    /**
     * Property key for a boolean indicating that the model is an extrusion 'type'.
     * Setting this property to true obligates the entity to provide property values
     * for the CROSS_SECTION_TRANSLATION_PROP, SPINE_VERTICES_PROP, and
     * VISIBLE_PROP and to provide a suitable cross section model that is referenced
     * by the Entity.MODEL_URL_PARAM.
     */
    public static final String IS_EXTRUSION_ENITY_PROP =
        "org.chefx3d.model.ExtrusionEntity.isExtrusionEntity";

    /**
     * Property key for a float array defining the translation of the cross section
     * vertices required to align with the spine. This is a 3 dimensional array of
     * values.
     */
    public static final String CROSS_SECTION_TRANSLATION_PROP =
        "org.chefx3d.model.ExtrusionEntity.crossSectionTranslation";

    /**
     * Property key for a float array containing the vertices of the extrusion spine.
     * A minimum of 2 vertices is required to produce a single section. The number
     * of sections will be the number of vertices minus 1 (one).
     */
    public static final String SPINE_VERTICES_PROP =
        "org.chefx3d.model.ExtrusionEntity.spineVertices";

    /**
     * Property key for a boolean array that determines the visibility of each
     * extrusion section. The array will contain a value per section.
     */
    public static final String VISIBLE_PROP =
        "org.chefx3d.model.ExtrusionEntity.visible";

    /**
     * Property key for a boolean array that determines whether an
     * extrusion should be mitered at adjoining sections. This property is
     * optional, with the default behavior being to miter each junction.
     * The array will contain a value per junction. The number of junctions
     * is the number of sections minus 1 (one).
     */
    public static final String MITER_ENABLE_PROP =
        "org.chefx3d.model.ExtrusionEntity.miterEnable";
}
