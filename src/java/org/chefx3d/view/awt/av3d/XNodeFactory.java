/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/gpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
import java.util.HashMap;

import org.web3d.vrml.lang.FieldConstants;

// Local imports
// None

/**
 * Factory class for producing XNode objects.
 *
 * @author Rex Melton
 * @version $Revision: 1.4 $
 */
class XNodeFactory {

    /** The instance */
    private static XNodeFactory instance;

    /** Map of the known nodes, and their respective info */
    private static HashMap<String, XNode> nodeMap;

    /** The known nodes */
    static {
        nodeMap = new HashMap<String, XNode>();

        /////////////////////////////////////////////////////////////////////
        // note: only initializeOnly and inputOutput fields are enumerated.
        // since inputOnly and outputOnly fields cannot be initialized from
        // a file, they are ignored for the purposes of capturing the data
        // parsed from an x3d file.
        /////////////////////////////////////////////////////////////////////

        HashMap<String, Integer> fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("name", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("reference", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("value", new Integer(FieldConstants.MFINT32));
        nodeMap.put("MetadataInteger", new XNode("MetadataInteger", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("name", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("reference", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("value", new Integer(FieldConstants.MFSTRING));
        nodeMap.put("MetadataString", new XNode("MetadataString", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("name", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("reference", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("value", new Integer(FieldConstants.MFFLOAT));
        nodeMap.put("MetadataFloat", new XNode("MetadataFloat", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("name", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("reference", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("value", new Integer(FieldConstants.MFDOUBLE));
        nodeMap.put("MetadataDouble", new XNode("MetadataDouble", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("name", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("reference", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("value", new Integer(FieldConstants.MFNODE));
        nodeMap.put("MetadataSet", new XNode("MetadataSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("groundAngle", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("groundColor", new Integer(FieldConstants.MFCOLOR));
        fieldMap.put("skyAngle", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("skyColor", new Integer(FieldConstants.MFCOLOR));
        fieldMap.put("backUrl", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("frontUrl", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("leftUrl", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("rightUrl", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("bottomUrl", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("topUrl", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("transparency", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("Background", new XNode("Background", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("fogType", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("visibilityRange", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("Fog", new XNode("Fog", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("center", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("size", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("ProximitySensor", new XNode("ProximitySensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("center", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("size", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("VisibilitySensor", new XNode("VisibilitySensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(1);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        nodeMap.put("BooleanFilter", new XNode("BooleanFilter", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFBOOL));
        nodeMap.put("BooleanSequencer", new XNode("BooleanSequencer", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("toggle", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("BooleanToggle", new XNode("BooleanToggle", fieldMap));

        fieldMap = new HashMap<String, Integer>(1);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        nodeMap.put("BooleanTrigger", new XNode("BooleanTrigger", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFINT32));
        nodeMap.put("IntegerSequencer", new XNode("IntegerSequencer", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("integerKey", new Integer(FieldConstants.SFINT32));
        nodeMap.put("IntegerTrigger", new XNode("IntegerTrigger", fieldMap));

        fieldMap = new HashMap<String, Integer>(1);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        nodeMap.put("TimeTrigger", new XNode("TimeTrigger", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("lineSegments", new Integer(FieldConstants.MFVEC2F));
        nodeMap.put("Polyline2D", new XNode("Polyline2D", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("point", new Integer(FieldConstants.MFVEC2F));
        nodeMap.put("Polypoint2D", new XNode("Polypoint2D", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("size", new Integer(FieldConstants.SFVEC2F));
        nodeMap.put("Rectangle2D", new XNode("Rectangle2D", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("vertices", new Integer(FieldConstants.MFVEC2F));
        nodeMap.put("TriangleSet2D", new XNode("TriangleSet2D", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("size", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("Box", new XNode("Box", fieldMap));

        fieldMap = new HashMap<String, Integer>(6);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("bottomRadius", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("height", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("bottom", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("side", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Cone", new XNode("Cone", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("radius", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("height", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("bottom", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("side", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("top", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Cylinder", new XNode("Cylinder", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("radius", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("Sphere", new XNode("Sphere", fieldMap));

        fieldMap = new HashMap<String, Integer>(17);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("colorIndex", new Integer(FieldConstants.MFINT32));
        fieldMap.put("coordIndex", new Integer(FieldConstants.MFINT32));
        fieldMap.put("texCoordIndex", new Integer(FieldConstants.MFINT32));
        fieldMap.put("normalIndex", new Integer(FieldConstants.MFINT32));
        fieldMap.put("creaseAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("convex", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("IndexedFaceSet", new XNode("IndexedFaceSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(14);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("creaseAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("height", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("xDimension", new Integer(FieldConstants.SFINT32));
        fieldMap.put("xSpacing", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("zDimension", new Integer(FieldConstants.SFINT32));
        fieldMap.put("zSpacing", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("ElevationGrid", new XNode("ElevationGrid", fieldMap));

        fieldMap = new HashMap<String, Integer>(11);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("beginCap", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("convex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("creaseAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("crossSection", new Integer(FieldConstants.MFVEC2F));
        fieldMap.put("endCap", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("orientation", new Integer(FieldConstants.MFROTATION));
        fieldMap.put("scale", new Integer(FieldConstants.MFVEC2F));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("spine", new Integer(FieldConstants.MFVEC3F));
        nodeMap.put("Extrusion", new XNode("Extrusion", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("Group", new XNode("Group", fieldMap));

        fieldMap = new HashMap<String, Integer>(9);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("center", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("rotation", new Integer(FieldConstants.SFROTATION));
        fieldMap.put("scale", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("scaleOrientation", new Integer(FieldConstants.SFROTATION));
        fieldMap.put("translation", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("Transform", new XNode("Transform", fieldMap));

        fieldMap = new HashMap<String, Integer>(9);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("matrix", new Integer(FieldConstants.SFMATRIX4F));
        nodeMap.put("MatrixTransform", new XNode("MatrixTransform", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("title", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("info", new Integer(FieldConstants.MFSTRING));
        nodeMap.put("WorldInfo", new XNode("WorldInfo", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("whichChoice", new Integer(FieldConstants.SFINT32));
        nodeMap.put("Switch", new XNode("Switch", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFVEC3F));
        nodeMap.put("CoordinateInterpolator", new XNode("CoordinateInterpolator", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFROTATION));
        nodeMap.put("OrientationInterpolator", new XNode("OrientationInterpolator", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFVEC3F));
        nodeMap.put("PositionInterpolator", new XNode("PositionInterpolator", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFFLOAT));
        nodeMap.put("ScalarInterpolator", new XNode("ScalarInterpolator", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFCOLOR));
        nodeMap.put("ColorInterpolator", new XNode("ColorInterpolator", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("key", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("keyValue", new Integer(FieldConstants.MFVEC3F));
        nodeMap.put("NormalInterpolator", new XNode("NormalInterpolator", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("KeySensor", new XNode("KeySensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("deletionAllowed", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("StringSensor", new XNode("StringSensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("ambientIntensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("color", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("intensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("on", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("global", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("direction", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("DirectionalLight", new XNode("DirectionalLight", fieldMap));

        fieldMap = new HashMap<String, Integer>(9);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("ambientIntensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("color", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("intensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("on", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("global", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("attenuation", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("location", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("radius", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("PointLight", new XNode("PointLight", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("ambientIntensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("color", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("intensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("on", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("global", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("attenuation", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("beamWidth", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("cutOffAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("direction", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("location", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("radius", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("SpotLight", new XNode("SpotLight", fieldMap));

        fieldMap = new HashMap<String, Integer>(8);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("avatarSize", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("headlight", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("speed", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("type", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("visibilityLimit", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("transitionType", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("transitionTime", new Integer(FieldConstants.MFFLOAT));
        nodeMap.put("NavigationInfo", new XNode("NavigationInfo", fieldMap));

        fieldMap = new HashMap<String, Integer>(8);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("fieldOfView", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("jump", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("orientation", new Integer(FieldConstants.SFROTATION));
        fieldMap.put("position", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("centerOfRotation", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("retainUserOffsets", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Viewpoint", new XNode("Viewpoint", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("axisOfRotation", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("Billboard", new XNode("Billboard", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("collide", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("proxy", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Collision", new XNode("Collision", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("center", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("range", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("forceTransitions", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("LOD", new XNode("LOD", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("children", new Integer(FieldConstants.MFNODE));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("parameter", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("url", new Integer(FieldConstants.MFSTRING));
        nodeMap.put("Anchor", new XNode("Anchor", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("url", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("load", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Inline", new XNode("Inline", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("watchList", new Integer(FieldConstants.MFNODE));
        fieldMap.put("timeOut", new Integer(FieldConstants.SFTIME));
        nodeMap.put("LoadSensor", new XNode("LoadSensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(8);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("autoOffset", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("maxAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("minAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("diskAngle", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("offset", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("CylinderSensor", new XNode("CylinderSensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("autoOffset", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("maxPosition", new Integer(FieldConstants.SFVEC2F));
        fieldMap.put("minPosition", new Integer(FieldConstants.SFVEC2F));
        fieldMap.put("offset", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("PlaneSensor", new XNode("PlaneSensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("autoOffset", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("offset", new Integer(FieldConstants.SFROTATION));
        nodeMap.put("SphereSensor", new XNode("SphereSensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        nodeMap.put("TouchSensor", new XNode("TouchSensor", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.MFCOLOR));
        nodeMap.put("Color", new XNode("Color", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.MFCOLORRGBA));
        nodeMap.put("ColorRGBA", new XNode("ColorRGBA", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("point", new Integer(FieldConstants.MFVEC3F));
        nodeMap.put("Coordinate", new XNode("Coordinate", fieldMap));

        fieldMap = new HashMap<String, Integer>(8);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("colorIndex", new Integer(FieldConstants.MFINT32));
        fieldMap.put("coordIndex", new Integer(FieldConstants.MFINT32));
        nodeMap.put("IndexedLineSet", new XNode("IndexedLineSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("vertexCount", new Integer(FieldConstants.MFINT32));
        nodeMap.put("LineSet", new XNode("LineSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(3);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        nodeMap.put("PointSet", new XNode("PointSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("vector", new Integer(FieldConstants.MFVEC3F));
        nodeMap.put("Normal", new XNode("Normal", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("index", new Integer(FieldConstants.MFINT32));
        nodeMap.put("IndexedTriangleFanSet", new XNode("IndexedTriangleFanSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("index", new Integer(FieldConstants.MFINT32));
        nodeMap.put("IndexedTriangleSet", new XNode("IndexedTriangleSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("index", new Integer(FieldConstants.MFINT32));
        nodeMap.put("IndexedTriangleStripSet", new XNode("IndexedTriangleStripSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("fanCount", new Integer(FieldConstants.MFINT32));
        nodeMap.put("TriangleFanSet", new XNode("TriangleFanSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(11);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        nodeMap.put("TriangleSet", new XNode("TriangleSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(12);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("coord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFNODE));
        fieldMap.put("normal", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("ccw", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("colorPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("normalPerVertex", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("fogCoord", new Integer(FieldConstants.SFNODE));
        fieldMap.put("attrib", new Integer(FieldConstants.SFNODE));
        fieldMap.put("stripCount", new Integer(FieldConstants.MFINT32));
        nodeMap.put("TriangleStripSet", new XNode("TriangleStripSet", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("url", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("mustEvaluate", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("directOutput", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Script", new XNode("Script", fieldMap));

        fieldMap = new HashMap<String, Integer>(8);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("material", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texture", new Integer(FieldConstants.SFNODE));
        fieldMap.put("textureTransform", new Integer(FieldConstants.SFNODE));
        fieldMap.put("lineProperties", new Integer(FieldConstants.SFNODE));
        fieldMap.put("pointProperties", new Integer(FieldConstants.SFNODE));
        fieldMap.put("fillProperties", new Integer(FieldConstants.SFNODE));
        fieldMap.put("textureProperties", new Integer(FieldConstants.SFNODE));
        nodeMap.put("Appearance", new XNode("Appearance", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("ambientIntensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("diffuseColor", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("emissiveColor", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("shininess", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("specularColor", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("transparency", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("Material", new XNode("Material", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("appearance", new Integer(FieldConstants.SFNODE));
        fieldMap.put("geometry", new Integer(FieldConstants.SFNODE));
        fieldMap.put("bboxSize", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("bboxCenter", new Integer(FieldConstants.SFVEC3F));
        nodeMap.put("Shape", new XNode("Shape", fieldMap));

        fieldMap = new HashMap<String, Integer>(4);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("linewidthScaleFactor", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("linetype", new Integer(FieldConstants.SFINT32));
        fieldMap.put("applied", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("LineProperties", new XNode("LineProperties", fieldMap));

        fieldMap = new HashMap<String, Integer>(9);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("loop", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("startTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("stopTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("pauseTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("resumeTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("description", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("pitch", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("url", new Integer(FieldConstants.MFSTRING));
        nodeMap.put("AudioClip", new XNode("AudioClip", fieldMap));

        fieldMap = new HashMap<String, Integer>(11);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("direction", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("intensity", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("location", new Integer(FieldConstants.SFVEC3F));
        fieldMap.put("maxBack", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("maxFront", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("minBack", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("minFront", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("priority", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("source", new Integer(FieldConstants.SFNODE));
        fieldMap.put("spatialize", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Sound", new XNode("Sound", fieldMap));

        fieldMap = new HashMap<String, Integer>(10);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("family", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("horizontal", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("justify", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("language", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("leftToRight", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("size", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("spacing", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("style", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("topToBottom", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("FontStyle", new XNode("FontStyle", fieldMap));

        fieldMap = new HashMap<String, Integer>(6);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("string", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("fontStyle", new Integer(FieldConstants.SFNODE));
        fieldMap.put("length", new Integer(FieldConstants.MFFLOAT));
        fieldMap.put("maxExtent", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("solid", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("Text", new XNode("Text", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("repeatS", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("repeatT", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("textureProperties", new Integer(FieldConstants.SFNODE));
        fieldMap.put("url", new Integer(FieldConstants.MFSTRING));
        nodeMap.put("ImageTexture", new XNode("ImageTexture", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("repeatS", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("repeatT", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("textureProperties", new Integer(FieldConstants.SFNODE));
        fieldMap.put("image", new Integer(FieldConstants.SFIMAGE));
        nodeMap.put("PixelTexture", new XNode("PixelTexture", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("point", new Integer(FieldConstants.MFVEC2F));
        nodeMap.put("TextureCoordinate", new XNode("TextureCoordinate", fieldMap));

        fieldMap = new HashMap<String, Integer>(5);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("center", new Integer(FieldConstants.SFVEC2F));
        fieldMap.put("rotation", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("scale", new Integer(FieldConstants.SFVEC2F));
        fieldMap.put("translation", new Integer(FieldConstants.SFVEC2F));
        nodeMap.put("TextureTransform", new XNode("TextureTransform", fieldMap));

        fieldMap = new HashMap<String, Integer>(7);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("mode", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("texture", new Integer(FieldConstants.MFNODE));
        fieldMap.put("color", new Integer(FieldConstants.SFCOLOR));
        fieldMap.put("alpha", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("function", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("source", new Integer(FieldConstants.MFSTRING));
        nodeMap.put("MultiTexture", new XNode("MultiTexture", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("texCoord", new Integer(FieldConstants.MFNODE));
        nodeMap.put("MultiTextureCoordinate", new XNode("MultiTextureCoordinate", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("textureTransform", new Integer(FieldConstants.MFNODE));
        nodeMap.put("MultiTextureTransform", new XNode("MultiTextureTransform", fieldMap));

        fieldMap = new HashMap<String, Integer>(2);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("mode", new Integer(FieldConstants.SFSTRING));
        nodeMap.put("TextureCoordinateGenerator", new XNode("TextureCoordinateGenerator", fieldMap));

        fieldMap = new HashMap<String, Integer>(10);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("boundaryColor", new Integer(FieldConstants.SFCOLORRGBA));
        fieldMap.put("boundaryWidth", new Integer(FieldConstants.SFINT32));
        fieldMap.put("boundaryModeS", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("boundaryModeT", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("magnificationFilter", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("minificationFilter", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("generateMipMaps", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("anisotropicMode", new Integer(FieldConstants.SFSTRING));
        fieldMap.put("anisotropicFilterDegree", new Integer(FieldConstants.SFFLOAT));
        nodeMap.put("TextureProperties", new XNode("TextureProperties", fieldMap));

        fieldMap = new HashMap<String, Integer>(11);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("loop", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("startTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("stopTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("pauseTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("resumeTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("speed", new Integer(FieldConstants.SFFLOAT));
        fieldMap.put("url", new Integer(FieldConstants.MFSTRING));
        fieldMap.put("repeatS", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("repeatT", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("textureProperties", new Integer(FieldConstants.SFNODE));
        nodeMap.put("MovieTexture", new XNode("MovieTexture", fieldMap));

        fieldMap = new HashMap<String, Integer>(8);
        fieldMap.put("metadata", new Integer(FieldConstants.SFNODE));
        fieldMap.put("loop", new Integer(FieldConstants.SFBOOL));
        fieldMap.put("startTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("stopTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("pauseTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("resumeTime", new Integer(FieldConstants.SFTIME));
        fieldMap.put("cycleInterval", new Integer(FieldConstants.SFTIME));
        fieldMap.put("enabled", new Integer(FieldConstants.SFBOOL));
        nodeMap.put("TimeSensor", new XNode("TimeSensor", fieldMap));
    }

    /**
     * Restricted Constructor
     */
    private XNodeFactory() {
    }

    /**
     * Return the factory instance
     *
     * @return The factory instance
     */
    static XNodeFactory getInstance() {
        if (instance == null) {
            instance = new XNodeFactory();
        }
        return(instance);
    }

    /**
     * Return the XNode object for the argument
     * node name. If the node is unknown, null is
     * returned.
     *
     * @param nodeName The node identifier
     * @return The node object
     */
    XNode get(String nodeName) {
        XNode node = nodeMap.get(nodeName);
        if (node == null) {
            return(null);
        } else {
            return(node.copy());
        }
    }
}
