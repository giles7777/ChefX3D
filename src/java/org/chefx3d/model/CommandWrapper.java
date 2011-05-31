/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
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

/**
 * An interface that wraps the commands so that they call the 
 * selected method: execute, redo, or undo.
 * 
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public interface CommandWrapper {

    /**
     * Perform the command method
     */
    public void process();

    /**
     * Reject the command method
     */
    public void reject();

}
