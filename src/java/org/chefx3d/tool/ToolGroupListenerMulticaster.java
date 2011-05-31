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

package org.chefx3d.tool;

// External Imports
import java.util.List;

import org.j3d.util.I18nManager;


// Local imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

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
 *   ToolGroupListener listener = null;
 *
 *   public void addToolGroupListener(ToolGroupListener l) {
 *     listener = ToolGroupListenerMulticaster.add(listener, l);
 *   }
 *
 *   public void removeToolGroupListener(ToolGroupListener l) {
 *     listener = ToolGroupListenerMulticaster.remove(listener, l);
 *   }
 *
 *   public void <i>toolChanged</i>(ToolGroupEvent evt) {
 *     if(listener != null) {
 *       listener.toolChanged(evt);
 *   }
 * }
 * </code></pre>
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>toolAddMsg: Message when an exception is generated in the
 *     toolGroupAdded() callback</li>
 * <li>toolRemoveMsg: Message when an exception is generated in the
 *     toolRemoved() callback</li>
 * <li>toolGroupAddMsg: Message when an exception is generated in the
 *     toolGroupAdded() callback</li>
 * <li>toolGroupRemoveMsg: Message when an exception is generated in the
 *     toolGroupRemoved() callback</li>
 * </ul>
 *
 * @author  Justin Couch
 * @version $Revision: 1.3 $
 */
public class ToolGroupListenerMulticaster
    implements ToolGroupListener {

    /** Error message when the user code barfs */
    private static final String TOOL_ADD_ERR_PROP =
        "org.chefx3d.tool.ToolGroupListenerMulticaster.toolAddMsg";

    /** Error message when the user code barfs */
    private static final String TOOL_REMOVE_ERR_PROP =
        "org.chefx3d.tool.ToolGroupListenerMulticaster.toolRemoveMsg";

    /** Error message when the user code barfs */
    private static final String GROUP_ADD_ERR_PROP =
        "org.chefx3d.tool.ToolGroupListenerMulticaster.toolGroupAddMsg";

    /** Error message when the user code barfs */
    private static final String GROUP_REMOVE_ERR_PROP =
        "org.chefx3d.tool.ToolGroupListenerMulticaster.toolGroupRemoveMsg";

    /** Error message when the user code barfs */
    protected static final String TOOL_UPDATE_ERR_PROP =
        "org.chefx3d.tool.ToolGroupListenerMulticaster.toolUpdateMsg";

    /** Error message when the user code barfs */
    protected static final String GROUP_UPDATE_ERR_PROP =
        "org.chefx3d.tool.ToolGroupListenerMulticaster.toolGroupUpdateMsg";

    /** The node listeners in use by this class */
    private final ToolGroupListener a;

    /** The node listeners in use by this class */
    private final ToolGroupListener b;

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
    ToolGroupListenerMulticaster(ToolGroupListener a, ToolGroupListener b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Removes a listener from this multicaster and returns the
     * resulting multicast listener.
     * @param oldl the listener to be removed
     */
    ToolGroupListener remove(ToolGroupListener oldl) {

        if(oldl == a)
            return b;

        if(oldl == b)
            return a;

        ToolGroupListener a2 = removeInternal(a, oldl);
        ToolGroupListener b2 = removeInternal(b, oldl);

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
    public static ToolGroupListener add(ToolGroupListener a,
                                        ToolGroupListener b) {
        return (ToolGroupListener)addInternal(a, b);
    }

    /**
     * Removes the old component-listener from component-listener-l and
     * returns the resulting multicast listener.
     * @param l component-listener-l
     * @param oldl the component-listener being removed
     */
    public static ToolGroupListener remove(ToolGroupListener l,
                                           ToolGroupListener oldl) {
        return (ToolGroupListener)removeInternal(l, oldl);
    }

    //----------------------------------------------------------
    // Methods defined by ToolGroupListener
    //----------------------------------------------------------

    /**
     * A tool has been added.  Batched additions will come through
     * the toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolAdded(ToolGroupEvent evt) {
        try {
            a.toolAdded(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_ADD_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolAdded(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_ADD_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolRemoved(ToolGroupEvent evt) {
        try {
            a.toolRemoved(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_REMOVE_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolRemoved(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_REMOVE_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolUpdated(ToolGroupEvent evt) {
        try {
            a.toolUpdated(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_UPDATE_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolUpdated(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(TOOL_UPDATE_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupAdded(ToolGroupEvent evt) {
        try {
            a.toolGroupAdded(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_ADD_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupAdded(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_ADD_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupRemoved(ToolGroupEvent evt) {
        try {
            a.toolGroupRemoved(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_REMOVE_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupRemoved(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_REMOVE_ERR_PROP) + b;

            errorReporter.errorReport(msg, e);
        }
    }

    /**
     * A tool group has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupUpdated(ToolGroupEvent evt) {
        try {
            a.toolGroupUpdated(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_UPDATE_ERR_PROP) + a;

            errorReporter.errorReport(msg, e);
        }

        try {
            b.toolGroupUpdated(evt);
        } catch(Exception e) {
            I18nManager intl_mgr = I18nManager.getManager();
            String msg = intl_mgr.getString(GROUP_UPDATE_ERR_PROP) + b;

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
     *     a new ToolGroupMulticaster instance which chains a with b.
     * @param a event listener-a
     * @param b event listener-b
     */
    private static ToolGroupListener addInternal(ToolGroupListener a,
                                                        ToolGroupListener b) {
        if(a == null)
            return b;

        if(b == null)
            return a;

        return new ToolGroupListenerMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener after removing the
     * old listener from listener-l.
     * If listener-l equals the old listener OR listener-l is null,
     * returns null.
     * Else if listener-l is an instance of ToolGroupMulticaster,
     * then it removes the old listener from it.
     * Else, returns listener l.
     * @param l the listener being removed from
     * @param oldl the listener being removed
     */
    private static ToolGroupListener removeInternal(ToolGroupListener l,
                                                          ToolGroupListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof ToolGroupListenerMulticaster) {
            return ((ToolGroupListenerMulticaster)l).remove(oldl);
        } else {
            return l;   // it's not here
        }
    }
}
