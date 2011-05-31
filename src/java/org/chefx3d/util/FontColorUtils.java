/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.util;

// External Imports
import java.awt.Color;
import java.awt.Font;


/**
 * Utilities class, for UI things
 *
 * @author Russell Dodds
 * @version $Revision: 1.4 $
 */
public class FontColorUtils {

    /**
     * Get the font to use as the title
     */
    public static Font getTitleFont() {

        ConfigManager configMgr = ConfigManager.getManager();

        String fontName =
            configMgr.getProperty("title.font");
        Integer fontStyle =
            Integer.parseInt(configMgr.getProperty("title.fontStyle"));
        Integer fontSize =
            Integer.parseInt(configMgr.getProperty("title.fontSize"));

        return new Font(fontName, fontStyle, fontSize);

    }
    
    /**
     * Get the font to use as the small text
     */
    public static Font getXSmallFont() {

        ConfigManager configMgr = ConfigManager.getManager();

        String fontName =
            configMgr.getProperty("xsmall.font");
        Integer fontStyle =
            Integer.parseInt(configMgr.getProperty("xsmall.fontStyle"));
        Integer fontSize =
            Integer.parseInt(configMgr.getProperty("xsmall.fontSize"));

        return new Font(fontName, fontStyle, fontSize);

    }

    /**
     * Get the font to use as the small text
     */
    public static Font getSmallFont() {

        ConfigManager configMgr = ConfigManager.getManager();

        String fontName =
            configMgr.getProperty("small.font");
        Integer fontStyle =
            Integer.parseInt(configMgr.getProperty("small.fontStyle"));
        Integer fontSize =
            Integer.parseInt(configMgr.getProperty("small.fontSize"));

        return new Font(fontName, fontStyle, fontSize);

    }

    /**
     * Get the font to use as the medium text
     */
    public static Font getMediumFont() {

        ConfigManager configMgr = ConfigManager.getManager();

        String fontName =
            configMgr.getProperty("medium.font");
        Integer fontStyle =
            Integer.parseInt(configMgr.getProperty("medium.fontStyle"));
        Integer fontSize =
            Integer.parseInt(configMgr.getProperty("medium.fontSize"));

        return new Font(fontName, fontStyle, fontSize);

    }

    /**
     * Get the font to use as the large text
     */
    public static Font getLargeFont() {

        ConfigManager configMgr = ConfigManager.getManager();

        String fontName =
            configMgr.getProperty("large.font");
        Integer fontStyle =
            Integer.parseInt(configMgr.getProperty("large.fontStyle"));
        Integer fontSize =
            Integer.parseInt(configMgr.getProperty("large.fontSize"));

        return new Font(fontName, fontStyle, fontSize);

    }

    /**
     * Get the font to use as the large text
     */
    public static Font getXLargeFont() {

        ConfigManager configMgr = ConfigManager.getManager();

        String fontName =
            configMgr.getProperty("xlarge.font");
        Integer fontStyle =
            Integer.parseInt(configMgr.getProperty("xlarge.fontStyle"));
        Integer fontSize =
            Integer.parseInt(configMgr.getProperty("xlarge.fontSize"));

        return new Font(fontName, fontStyle, fontSize);

    }


    /**
     * Get the background color to use
     */
    public static Color getBackgroundColor() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("backgroundColor");
        String[] colorComps = colorName.split(",");

        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

    /**
     * Get the foreground color to use
     */
    public static Color getForegroundColor() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("foregroundColor");
        String[] colorComps = colorName.split(",");
        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

    /**
     * Get the constast color to use
     */
    public static Color getContrastColor() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("contrastColor");
        String[] colorComps = colorName.split(",");
        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

    /**
     * Get the selected color to use
     */
    public static Color getSelectedColor(boolean alpha) {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("selectedColor");
        String[] colorComps = colorName.split(",");

        if (alpha) {
            return new Color(
                    Integer.parseInt(colorComps[0].trim()),
                    Integer.parseInt(colorComps[1].trim()),
                    Integer.parseInt(colorComps[2].trim()),
                    Integer.parseInt(colorComps[3].trim()));
        } else {
            return new Color(
                    Integer.parseInt(colorComps[0].trim()),
                    Integer.parseInt(colorComps[1].trim()),
                    Integer.parseInt(colorComps[2].trim()));
        }
    }

    /**
     * Get the highlight color to use
     */
    public static Color getHighlightColor(boolean alpha) {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("highlightColor");
        String[] colorComps = colorName.split(",");
        if (alpha) {
            return new Color(
                    Integer.parseInt(colorComps[0].trim()),
                    Integer.parseInt(colorComps[1].trim()),
                    Integer.parseInt(colorComps[2].trim()),
                    Integer.parseInt(colorComps[3].trim()));
        } else {
            return new Color(
                    Integer.parseInt(colorComps[0].trim()),
                    Integer.parseInt(colorComps[1].trim()),
                    Integer.parseInt(colorComps[2].trim()));
        }
    }


    /**
     * Get the floor color to use
     */
    public static Color getFloorColor() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("floorColor");
        String[] colorComps = colorName.split(",");

        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

    /**
     * Get the floor color to use
     */
    public static Color getSkyColor() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("skyColor");
        String[] colorComps = colorName.split(",");

        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

    /**
     * Get the shared color 1 to use
     */
    public static Color getSharedColor1() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("sharedColor1");
        String[] colorComps = colorName.split(",");

        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

    /**
     * Get the shared color 2 to use
     */
    public static Color getSharedColor2() {

        ConfigManager configMgr = ConfigManager.getManager();

        String colorName = configMgr.getProperty("sharedColor2");
        String[] colorComps = colorName.split(",");

        return new Color(
                Integer.parseInt(colorComps[0].trim()),
                Integer.parseInt(colorComps[1].trim()),
                Integer.parseInt(colorComps[2].trim()));
    }

}
