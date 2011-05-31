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
import java.util.HashMap;

// Local Imports
// None

/**
 * A holder and manager of application parameters
 * <p>
 *
 * Parameters are, typically, read in during the class start up time from
 * whatever environment started the app. This can then be queried at any
 * time during the running of the application for the values of those
 * properties.
 *
 * @author Russell Dodds
 * @version $Revision: 1.8 $
 */
public class ApplicationParams {

    /** The singleton class */
    private static ApplicationParams applicationParams;

    /** Map of application parameters name = value */
    private static HashMap<String, Object> params;

    /** Constant lookup for application name */
    public static String APP_NAME = "appName";

    /** Constant lookup for enable cache */
    public static String ENABLE_CACHE = "enableCache";

    /** Constant for enabling cache freshness checking */
    public static String CHECK_CACHE_FRESHNESS = "checkCacheFreshness";

    /** Constant lookup for the ssessionID */
    public static String SESSION_ID = "sessionID";

    /** Constant lookup for the version */
    public static String APP_VERSION = "appVersion";

    /** Constant lookup for the debug mode */
    public static String DEBUG_MODE = "debug";

    /** Constant lookup for enabling rules checking */
    public static String ENABLE_RULES = "enableRules";

    /** Constant lookup for the name of the cache */
    public static String CACHE_NAME = "cacheName";

    /**
     * Private constructor to prevent direct instantiation.
     */
    private ApplicationParams() {
        params = new HashMap<String, Object>();
    }

    /**
     *
     * @param name
     * @param value
     */
    public static void put(String name, Object value) {
        if (applicationParams == null) {
            applicationParams = new ApplicationParams();
        }

        params.put(name, value);
    }

    /**
     *
     * @param name
     * @param value
     */
    public static void remove(String name) {
        if (applicationParams == null) {
            return;
        }

        params.remove(name);
    }

    /**
     *
     * @param name
     * @return
     */
    public static Object get(String name) {
        if (applicationParams == null) {
            return null;
        }

        return params.get(name);
    }

    /**
     * Check to see if the named property is defined. Returns
     * true if it is defined, otherwise false.
     *
     * @param name The name of the parameter to check on
     * @return true when the parameter is defined, false otherwise
     */
    public static boolean contains(String name) {
        if (applicationParams == null) {
            return false;
        }

        return params.containsKey(name);
    }

    /**
     *
     * @param name
     * @return
     */
    public static HashMap<String, Object> getAll() {
        if (applicationParams == null) {
            return null;
        }

        return params;

    }

}
