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
 *   EntityPropertyListener listener = null;
 *
 *   public void addEntityPropertyListener(EntityPropertyListener l) {
 *     listener = EntityPropertyListenerMulticaster.add(listener, l);
 *   }
 *
 *   public void removeEntityPropertyListener(EntityPropertyListener l) {
 *     listener = EntityPropertyListenerMulticaster.remove(listener, l);
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
 * @version $Revision: 1.1 $
 */
public class EntityPropertyListenerMulticaster
    implements EntityPropertyListener {

    /** The node listeners in use by this class */
    private final EntityPropertyListener a;

    /** The node listeners in use by this class */
    private final EntityPropertyListener b;

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
    EntityPropertyListenerMulticaster(EntityPropertyListener a, EntityPropertyListener b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Removes a listener from this multicaster and returns the
     * resulting multicast listener.
     * @param oldl the listener to be removed
     */
    EntityPropertyListener remove(EntityPropertyListener oldl) {

        if(oldl == a)
            return b;

        if(oldl == b)
            return a;

        EntityPropertyListener a2 = removeInternal(a, oldl);
        EntityPropertyListener b2 = removeInternal(b, oldl);

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
    public static EntityPropertyListener add(EntityPropertyListener a,
                                        EntityPropertyListener b) {
        return (EntityPropertyListener)addInternal(a, b);
    }

    /**
     * Removes the old component-listener from component-listener-l and
     * returns the resulting multicast listener.
     * @param l component-listener-l
     * @param oldl the component-listener being removed
     */
    public static EntityPropertyListener remove(EntityPropertyListener l,
                                           EntityPropertyListener oldl) {
        return (EntityPropertyListener)removeInternal(l, oldl);
    }

    //----------------------------------------------------------
    // Methods defined by EntityPropertyListener
    //----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
        
        a.propertyAdded(entityID, propertySheet, propertyName);
        b.propertyAdded(entityID, propertySheet, propertyName);
        
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
        
        a.propertyRemoved(entityID, propertySheet, propertyName);
        b.propertyRemoved(entityID, propertySheet, propertyName);
        
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this property update an ongoing change like a transient position or the final value
     */
    public void propertyUpdated(int entityID,
            String propertySheet, String propertyName, boolean ongoing) {
        
        a.propertyUpdated(entityID, propertySheet, propertyName, ongoing);
        b.propertyUpdated(entityID, propertySheet, propertyName, ongoing);
        
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        
        a.propertiesUpdated(properties);
        b.propertiesUpdated(properties);
        
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
    private static EntityPropertyListener addInternal(EntityPropertyListener a,
                                                        EntityPropertyListener b) {
        if(a == null)
            return b;

        if(b == null)
            return a;

        return new EntityPropertyListenerMulticaster(a, b);
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
    private static EntityPropertyListener removeInternal(EntityPropertyListener l,
                                                          EntityPropertyListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof EntityPropertyListenerMulticaster) {
            return ((EntityPropertyListenerMulticaster)l).remove(oldl);
        } else {
            return l;   // it's not here
        }
    }
}
