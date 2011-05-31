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
import java.io.FileReader;

// Internal Imports
// none

/**
 * An Entity Importer
 *
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
public interface Importer {
    
    /**
     * Output the World Model to the specified stream.
     *
     * @param model The world model to import to
     * @param reader The stream to read from
     */
    public void importModel(WorldModel model, FileReader reader) throws LoadToolException;

}
