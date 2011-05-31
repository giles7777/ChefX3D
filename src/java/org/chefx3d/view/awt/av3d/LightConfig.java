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

package org.chefx3d.view.awt.av3d;

// External imports
import java.util.StringTokenizer;

import java.io.InputStream;
import java.io.IOException;

import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;

import java.util.Locale;

import javax.xml.parsers.*;

import org.j3d.aviatrix3d.*;

import org.j3d.aviatrix3d.rendering.BoundingVolume;

import org.j3d.util.I18nManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

// Local imports
import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;

/** 
 * Loader for a lighting configuration
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class LightConfig {
	
    /** Error message when we fail to parse a single float in the light file */
    private static final String FLOAT_PARSE_MSG =
        "org.chefx3d.view.awt.av3d.PreviewLayerManager.floatParseMsg";

    /** Error message when we fail to parse a vector in the light file */
    private static final String VECTOR_PARSE_MSG =
        "org.chefx3d.view.awt.av3d.PreviewLayerManager.vectorParseMsg";

    /** Error message when the light colour is not ]0,1] */
    private static final String COLOUR_RANGE_MSG =
        "org.chefx3d.view.awt.av3d.PreviewLayerManager.lightRangeMsg";

    /** Error message when the XML is bad, parsing the light config file */
    private static final String XML_SAX_ERROR_MSG =
        "org.chefx3d.view.awt.av3d.PreviewLayerManager.lightConfigXMLErrorMsg";

    /** Error message when the light config file has a generic IO error */
    private static final String CONFIG_IO_ERROR_MSG =
        "org.chefx3d.view.awt.av3d.PreviewLayerManager.lightConfigIOErrorMsg";

    /** Default ambient light definition */
    private LightConfigData ambientLightConfig;

    /** Point light definitions */
    private LightConfigData[] pointLightConfig;

    /** Directional light definitions */
    private LightConfigData[] directionalLightConfig;

    /** spot light definitions */
    private LightConfigData[] spotLightConfig;

    /**
     * Switch used to hold the point lights used to illuminate the scene.
     * Used to turn them off when we are in high quality mode because lights
     * are manually rendered using the shaders.
     */
    private SwitchGroup lightSwitch;

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

	/** Flag indicating that editor lighting is configured */
	private boolean enable;
	
	/**
	 * Constructor
	 *
	 * @param filename The file to parse for the lighting configuration
     * @param reporter The ErrorReporter instance to use or null
	 */
	LightConfig(String filename, ErrorReporter reporter) {
		
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();

        Object prop = ApplicationParams.get("enableEditorLighting");
        enable = false;

        if ((prop != null) && (prop instanceof Boolean)) {
            enable = ((Boolean)prop).booleanValue();
		}
		if (enable) {
			loadLightingConfig(filename);
		}
	}

	/**
	 * Return the enable state
	 *
	 * @return The enable state
	 */
	boolean isEnabled() {
		return(enable);
	}
	
	/**
	 * Return the configured state
	 *
	 * @return The configured state
	 */
	boolean isConfigured() {
		return(lightSwitch != null);
	}
	
	/**
	 * Return the SwitchGroup containing the lighting
	 *
	 * @return The SwitchGroup containing the lighting, or null if
	 * lighting is not enabled or the config file could not be found.
	 */
	SwitchGroup getSwitchGroup() {
		return(lightSwitch);
	}
	
    /**
     * Load the lighting configuration data from the external config file.
     * Creates lights dynamically from the file and stores them in the
     * internal arrays of class variables for later use.
	 *
	 * @param filename The file to parse for the lighting configuration
     */
    private void loadLightingConfig(String filename) {

        try {
			FileLoader fileLookup = new FileLoader();
			Object[] file = fileLookup.getFileURL(filename, true);
			if (file != null) {
				InputStream is = (InputStream)file[1];
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(is);
				doc.getDocumentElement().normalize();
				
				Element root_element = doc.getDocumentElement();
				
				loadAmbientLight(root_element.getElementsByTagName("ambient"));
				loadPointLights(root_element.getElementsByTagName("pointlight"));
				loadDirectionalLights(root_element.getElementsByTagName("directionallight"));
				loadSpotLights(root_element.getElementsByTagName("spotlight"));
				
				buildNodes();
			}
        } catch (SAXException e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(XML_SAX_ERROR_MSG);

            errorReporter.errorReport(msg, e);
        } catch (IOException ioe) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(CONFIG_IO_ERROR_MSG);

            errorReporter.errorReport(msg, ioe);
        } catch (ParserConfigurationException pce) {
        }
    }

    /**
     * Process the list of ambient lights in the config file. There is only
     * allowed to be one ambient light. Everything after the first is ignored.
     *
     * @param nodes The list of light definitions in XML form
     */
    private void loadAmbientLight(NodeList nodes) {
        if(nodes.getLength() == 0)
            return;

        Element light = (Element)nodes.item(0);

        // Hard code for now. Will come back and load from a file later.
        ambientLightConfig = new LightConfigData();
        ambientLightConfig.type = LightConfigData.LightType.AMBIENT;


        String attr = light.getAttribute("color");
        float[] col = parseColour(attr);

        ambientLightConfig.lightColor[0] = col[0];
        ambientLightConfig.lightColor[1] = col[1];
        ambientLightConfig.lightColor[2] = col[2];
    }

    /**
     * Process the list of point lights in the config file
     *
     * @param nodes The list of light definitions in XML form
     */
    private void loadPointLights(NodeList nodes) {
        int num_lights = nodes.getLength();

        pointLightConfig = new LightConfigData[num_lights];

        for(int i = 0; i < num_lights; i++) {
            Element light = (Element)nodes.item(i);

            pointLightConfig[i] = new LightConfigData();

            String pos_str = light.getAttribute("position");
            String col_str = light.getAttribute("color");
            String att_str = light.getAttribute("attenuation");
            String rad_str = light.getAttribute("radius");

            pointLightConfig[i].lightRadius = parseFloat(rad_str);
            float[] col = parseColour(col_str);

            pointLightConfig[i].lightColor[0] = col[0];
            pointLightConfig[i].lightColor[1] = col[1];
            pointLightConfig[i].lightColor[2] = col[2];


            float[] pos = parseVector(pos_str);
            pointLightConfig[i].lightPosition[0] = pos[0];
            pointLightConfig[i].lightPosition[1] = pos[1];
            pointLightConfig[i].lightPosition[2] = pos[2];

            float[] att = parseVector(att_str);
            pointLightConfig[i].attenuation[0] = att[0];
            pointLightConfig[i].attenuation[1] = att[1];
            pointLightConfig[i].attenuation[2] = att[2];
        }
    }

    /**
     * Process the list of directional lights in the config file
     *
     * @param nodes The list of light definitions in XML form
     */
    private void loadDirectionalLights(NodeList nodes) {
        int num_lights = nodes.getLength();

        directionalLightConfig = new LightConfigData[num_lights];

        for(int i = 0; i < num_lights; i++) {
            Element light = (Element)nodes.item(i);

            directionalLightConfig[i] = new LightConfigData();
            directionalLightConfig[i].type = LightConfigData.LightType.DIRECTIONAL;

            String col_str = light.getAttribute("color");
            String dir_str = light.getAttribute("direction");


            float[] dir = parseVector(dir_str);
            directionalLightConfig[i].direction[0] = dir[0];
            directionalLightConfig[i].direction[1] = dir[1];
            directionalLightConfig[i].direction[2] = dir[2];

            float[] col = parseColour(col_str);
            directionalLightConfig[i].lightColor[0] = col[0];
            directionalLightConfig[i].lightColor[1] = col[1];
            directionalLightConfig[i].lightColor[2] = col[2];
        }
    }

    /**
     * Process the list of spot lights in the config file
     *
     * @param nodes The list of light definitions in XML form
     */
    private void loadSpotLights(NodeList nodes) {
        int num_lights = nodes.getLength();

        spotLightConfig = new LightConfigData[num_lights];

        for(int i = 0; i < num_lights; i++) {
            Element light = (Element)nodes.item(i);

            spotLightConfig[i] = new LightConfigData();
            spotLightConfig[i].type = LightConfigData.LightType.SPOT;

            String ang_str = light.getAttribute("angle");
            String pos_str = light.getAttribute("position");
            String col_str = light.getAttribute("color");
            String att_str = light.getAttribute("attenuation");
            String dir_str = light.getAttribute("direction");

            spotLightConfig[i].angle = parseFloat(ang_str);

            float[] dir = parseVector(dir_str);
            spotLightConfig[i].direction[0] = dir[0];
            spotLightConfig[i].direction[1] = dir[1];
            spotLightConfig[i].direction[2] = dir[2];

            float[] col = parseColour(col_str);
            spotLightConfig[i].lightColor[0] = col[0];
            spotLightConfig[i].lightColor[1] = col[1];
            spotLightConfig[i].lightColor[2] = col[2];

            float[] pos = parseVector(pos_str);
            spotLightConfig[i].lightPosition[0] = pos[0];
            spotLightConfig[i].lightPosition[1] = pos[1];
            spotLightConfig[i].lightPosition[2] = pos[2];

            float[] att = parseVector(att_str);
            spotLightConfig[i].attenuation[0] = att[0];
            spotLightConfig[i].attenuation[1] = att[1];
            spotLightConfig[i].attenuation[2] = att[2];
        }
    }

    /**
     * Parse a string containing a single number and return that as a float. If
     * the parsing fails assume a default value of 0.0.
     *
     * @param str The input string to parse
     * @return The value parsed or 0
     */
    private float parseFloat(String str) {
        float ret_val = 0;

        try {
            ret_val = Float.parseFloat(str);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg_pattern = intl_mgr.getString(FLOAT_PARSE_MSG);

            Locale lcl = intl_mgr.getFoundLocale();

            Object[] msg_args = { str };
            Format[] fmts = { null };
            MessageFormat msg_fmt =
                new MessageFormat(msg_pattern, lcl);
            msg_fmt.setFormats(fmts);
            String msg = msg_fmt.format(msg_args);

            errorReporter.warningReport(msg, null);
        }

        return ret_val;
    }

    /**
     * Parse a string containing 3 numbers that are assumed to be [0,1] colour
     * values.
     *
     * @param str The input string to parse
     * @return An array of length 3 with the parsed numbers
     */
    private float[] parseColour(String str) {
        float[] ret_val = parseVector(str);

        for(int i = 0; i < 3; i++)
            if(ret_val[i] < 0 || ret_val[i] > 1) {
                I18nManager intl_mgr = I18nManager.getManager();
                String msg_pattern = intl_mgr.getString(COLOUR_RANGE_MSG);

                Locale lcl = intl_mgr.getFoundLocale();

                Object[] msg_args = { str };
                Format[] fmts = { null };
                MessageFormat msg_fmt =
                    new MessageFormat(msg_pattern, lcl);
                msg_fmt.setFormats(fmts);
                String msg = msg_fmt.format(msg_args);

                errorReporter.warningReport(msg, null);
            }

        return ret_val;
    }

    /**
     * Parse a string containing 3 numbers and return that as a float array. If
     * the parsing fails on any component, assume a default value of 0.0.
     *
     * @param str The input string to parse
     * @return An array of length 3 with the parsed numbers
     */
    private float[] parseVector(String str) {
        float[] ret_val = new float[3];
        StringTokenizer strtok = new StringTokenizer(str);

        for(int i = 0; i < 3; i++) {
            try {
                String num_str = strtok.nextToken();
                ret_val[i] = Float.parseFloat(num_str);
            } catch(Exception e) {
                I18nManager intl_mgr = I18nManager.getManager();
                String msg_pattern = intl_mgr.getString(VECTOR_PARSE_MSG);

                Locale lcl = intl_mgr.getFoundLocale();

                Object[] msg_args = { str };
                Format[] fmts = { null };
                MessageFormat msg_fmt =
                    new MessageFormat(msg_pattern, lcl);
                msg_fmt.setFormats(fmts);
                String msg = msg_fmt.format(msg_args);
                errorReporter.warningReport(msg, null);
            }
        }

        return ret_val;
    }
	
	
    /**
     * Build up the scene graph objects.
     */
    private void buildNodes() {

        Group lights = new Group();

        if (ambientLightConfig != null) {
            AmbientLight ambientLight = new AmbientLight();
            ambientLight.setAmbientColor(ambientLightConfig.lightColor);
            ambientLight.setGlobalOnly(true);
            ambientLight.setEnabled(true);

            lights.addChild(ambientLight);
        }

        // Set up bounds for the lights

        for (int i = 0; i < pointLightConfig.length; i++) {

            PointLight pl = new PointLight(pointLightConfig[i].lightColor,
                                           pointLightConfig[i].lightPosition);

            BoundingVolume bounds =
                new BoundingSphere(pointLightConfig[i].lightRadius);

            pl.setEnabled(true);
            pl.setGlobalOnly(true);
            pl.setAttenuation(pointLightConfig[i].attenuation[0],
                              pointLightConfig[i].attenuation[1],
                              pointLightConfig[i].attenuation[2]);

            pl.setBounds(bounds);

            lights.addChild(pl);
        }

        for (int i = 0; i < directionalLightConfig.length; i++) {

            DirectionalLight pl = new DirectionalLight(
                                        directionalLightConfig[i].lightColor,
                                        directionalLightConfig[i].direction);

            BoundingVolume bounds =
                new BoundingSphere(directionalLightConfig[i].lightRadius);

            pl.setEnabled(true);
            pl.setGlobalOnly(true);

            pl.setBounds(bounds);

            lights.addChild(pl);
        }

        for (int i = 0; i < spotLightConfig.length; i++) {

            SpotLight pl =
                new SpotLight(spotLightConfig[i].lightColor,
                              spotLightConfig[i].lightPosition,
                              spotLightConfig[i].direction);

            BoundingVolume bounds =
                new BoundingSphere(spotLightConfig[i].lightRadius);

            pl.setCutOffAngle(spotLightConfig[i].angle);
            pl.setAttenuation(spotLightConfig[i].attenuation[0],
                              spotLightConfig[i].attenuation[1],
                              spotLightConfig[i].attenuation[2]);

            pl.setEnabled(true);
            pl.setGlobalOnly(true);
            pl.setBounds(bounds);

            lights.addChild(pl);
        }

        lightSwitch = new SwitchGroup();
        lightSwitch.addChild(lights);
        lightSwitch.setActiveChild(0);
    }
}
