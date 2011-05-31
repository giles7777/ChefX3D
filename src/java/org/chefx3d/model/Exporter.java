/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
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


//External Imports
import java.io.File;
import java.io.Writer;

// Internal Imports
// none

/**
 * An Entity Exporter
 *
 * @author Alan Hudson
 * @version $Revision: 1.24 $
 */
public interface Exporter {
    
    /**
     * Output a specific entity to the specified stream.
     *
     * @param model The world model to export
     * @param entityID The entity to export
     * @param fw The stream to write to
     */
    public void export(WorldModel model, int entityID, Writer fw);

    /**
     * Output the World Model to the specified stream.
     *
     * @param model The world model to export
     * @param fw The stream to write to
     */
    public void export(WorldModel model, Writer fw);

    /**
     * Output a specific entity to the specified file.
     *
     * @param model The world model to export
     * @param name The entity to export
     * @param file - The file to write to
     */
    public void export(WorldModel model, String name, File file);
    
}
