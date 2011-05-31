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

// Local imports
// None

/**
 * A listener for changes in the individual tool groups.
 *
 * @author Alan Hudson
 * @version $Revision: 1.2 $
 */
public interface ToolGroupListener {

    /**
     * A tool has been added.  Batched additions will come through
     * the toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolAdded(ToolGroupEvent evt);

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupAdded(ToolGroupEvent evt);

    /**
     * A tool has been removed. Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolRemoved(ToolGroupEvent evt);

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupRemoved(ToolGroupEvent evt);
    
    /**
     * A tool has been updated. 
     *
     * @param evt The event that caused this method to be called
     */
    public void toolUpdated(ToolGroupEvent evt);

    /**
     * A tool group has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupUpdated(ToolGroupEvent evt);
  
}
