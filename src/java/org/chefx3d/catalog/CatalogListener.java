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

// Local imports
import org.chefx3d.tool.ToolGroup;

/**
 * A listener for changes in the individual catalogs.
 *
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public interface CatalogListener {

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param name The catalog name
     * @param group The toolGroup added to
     */
    public void toolGroupAdded(String name, ToolGroup group);

    /**
     * A group of tool groups have been added to this catalog.
     *
     * @param name The catalog name
     * @param groups The list of tool groups added
     */
    public void toolGroupsAdded(String name, List<ToolGroup> groups);

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupRemoved(String name, ToolGroup group);

    /**
     * A group of tool groups have been removed.
     *
     * @param name The catalog name
     * @param groups The list of tool groups that have been removed
     */
    public void toolGroupsRemoved(String name, List<ToolGroup> groups);
}
