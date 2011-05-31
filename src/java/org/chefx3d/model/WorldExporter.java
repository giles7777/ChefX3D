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

import java.io.Writer;

/**
 * Export a world model into some format.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
public interface WorldExporter {
    /**
     * Output the World Model to the specified stream.
     * 
     * @param model The world model to export
     * @param writer The stream to write to
     */
    public void export(WorldModel model, Writer writer);
}