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

package org.chefx3d.catalog;

// External Imports
import java.util.List;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.tool.ToolGroup;

/**
 * A class which implements efficient and thread-safe multi-cast event
 * dispatching for the events defined in this package.
 * <p>
 *
 * This class will manage an immutable structure consisting of a chain of
 * event listeners and will dispatch events to those listeners.  Because
 * the structure is immutable, it is safe to use this API to add/remove
 * listeners during the process of an event dispatch operation.
 * <p>
 *
 * An example of how this class could be used to implement a new
 * component which fires "action" events:
 *
 * <pre><code>
 * public myComponent extends Component {
 *   CatalogListener listener = null;
 *
 *   public void addCatalogListener(CatalogListener l) {
 *     listener = CatalogListenerMulticaster.add(listener, l);
 *   }
 *
 *   public void removeCatalogListener(CatalogListener l) {
 *     listener = CatalogListenerMulticaster.remove(listener, l);
 *   }
 *
 *   public void catalogChanged(<i>catalogevent evt</i>) {
 *     if(listener != null) {
 *       listener.catalogChanged(evt);
 *   }
 * }
 * </code></pre>
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>toolGroupAddMsg: Message when an exception is generated in the
 *     toolGroupAdded() callback</li>
 * <li>toolGroupRemoveMsg: Message when an exception is generated in the
 *     toolGroupRemoved() callback</li>
 * <li>toolGroupsAddMsg: Message when an exception is generated in the
 *     toolGroupsAdded() callback</li>
 * <li>toolGroupsRemoveMsg: Message when an exception is generated in the
 *     toolGroupsRemoved() callback</li>
 * </ul>
 *
 * @author  Justin Couch
 * @version $Revision: 1.4 $
 */
public class CatalogListenerMulticaster
    implements CatalogListener {

    /** Error message when the user code barfs */
    private static final String GROUPS_ADD_ERR_PROP =
        "org.chefx3d.catalog.CatalogListenerMulticaster.toolGroupsAddMsg";

    /** Error message when the user code barfs */
    private static final String GROUPS_REMOVE_ERR_PROP =
        "org.chefx3d.catalog.CatalogListenerMulticaster.toolGroupsRemoveMsg";

    /** Error message when the user code barfs */
    private static final String GROUP_ADD_ERR_PROP =
        "org.chefx3d.catalog.CatalogListenerMulticaster.toolGroupAddMsg";

    /** Error message when the user code barfs */
    private static final String GROUP_REMOVE_ERR_PROP =
        "org.chefx3d.catalog.CatalogListenerMulticaster.toolGroupRemoveMsg";

    /** The node listeners in use by this class */
    private final CatalogListener a;

    /** The node listeners in use by this class */
    private final CatalogListener b;

    /** Reporter instance for handing out errors */
    private static ErrorReporter errorReporter =
        DefaultErrorReporter.getDefaultReporter();

    /**
     * Creates an event multicaster instance which chains listener-a
     * with listener-b. Input parameters <code>a</code> and <code>b</code>
     * should not be <code>null</code>, though implementations may vary in
     * choosing whether or not to throw <code>NullPointerException</code>
     * in that case.
     * @param a listener-a
     * @param b listener-b
     */
    CatalogListenerMulticaster(CatalogListener a, CatalogListener b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Removes a listener from this multicaster and returns the
     * resulting multicast listener.
     * @param oldl the listener to be removed
     */
    CatalogListener remove(CatalogListener oldl) {

        if(oldl == a)
            return b;

        if(oldl == b)
            return a;

        CatalogListener a2 = removeInternal(a, oldl);
        CatalogListener b2 = removeInternal(b, oldl);

        if (a2 == a && b2 == b) {
            return this;  // it's not here
        }

        return addInternal(a2, b2);
    }

    /**
     * Register an error reporter with the engine so that any errors generated
     * by the loading of script code can be reported in a nice, pretty fashion.
     * Setting a value of null will clear the currently set reporter. If one
     * is already set, the new value replaces the old.
     *
     * @param reporter The instance to use or null
     */
    public static void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        // Reset the default only if we are not shutting down the system.
        if(reporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Adds input-method-listener-a with input-method-listener-b and
     * returns the resulting multicast listener.
     * @param a input-method-listener-a
     * @param b input-method-listener-b
     */
    public static CatalogListener add(CatalogListener a,
                                      CatalogListener b) {
        return (CatalogListener)addInternal(a, b);
    }

    /**
     * Removes the old component-listener from component-listener-l and
     * returns the resulting multicast listener.
     * @param l component-listener-l
     * @param oldl the component-listener being removed
     */
    public static CatalogListener remove(CatalogListener l,
                                         CatalogListener oldl) {
        return (CatalogListener)removeInternal(l, oldl);
    }

    //----------------------------------------------------------
    // Methods defined by CatalogListener
    //----------------------------------------------------------

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupAdded(String name, ToolGroup group) {
        try {
            a.toolGroupAdded(name, group);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_ADD_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupAdded(name, group);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_ADD_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A group of tools have been added.
     *
     * @param name The catalog name
     * @param groups The list of tools
     */
    public void toolGroupsAdded(String name, List<ToolGroup> groups) {
        try {
            a.toolGroupsAdded(name, groups);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUPS_ADD_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupsAdded(name, groups);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUPS_ADD_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupRemoved(String name, ToolGroup group) {
        try {
            a.toolGroupRemoved(name, group);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_REMOVE_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupRemoved(name, group);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_REMOVE_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A group of tools have been removed.
     *
     * @param name The catalog name
     * @param groups The list of tool groups that have been removed
     */
    public void toolGroupsRemoved(String name, List<ToolGroup> groups) {
        try {
            a.toolGroupsRemoved(name, groups);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUPS_ADD_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupsRemoved(name, groups);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUPS_REMOVE_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Returns the resulting multicast listener from adding listener-a
     * and listener-b together.
     * If listener-a is null, it returns listener-b;
     * If listener-b is null, it returns listener-a
     * If neither are null, then it creates and returns
     *     a new CatalogMulticaster instance which chains a with b.
     * @param a event listener-a
     * @param b event listener-b
     */
    private static CatalogListener addInternal(CatalogListener a,
                                               CatalogListener b) {
        if(a == null)
            return b;

        if(b == null)
            return a;

        return new CatalogListenerMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener after removing the
     * old listener from listener-l.
     * If listener-l equals the old listener OR listener-l is null,
     * returns null.
     * Else if listener-l is an instance of CatalogMulticaster,
     * then it removes the old listener from it.
     * Else, returns listener l.
     * @param l the listener being removed from
     * @param oldl the listener being removed
     */
    private static CatalogListener removeInternal(CatalogListener l,
                                                          CatalogListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof CatalogListenerMulticaster) {
            return ((CatalogListenerMulticaster)l).remove(oldl);
        } else {
            return l;   // it's not here
        }
    }
}
