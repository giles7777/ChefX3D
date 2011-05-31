/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
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

// Internal Imports

/**
 * The implementing class can issue IDs for entities.
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public interface IdIssuer {

	/**
     * Get a unique ID for an entity.
     *
     * @return The unique ID
     */
    public int issueEntityID();

    /**
     * Get a unique ID for a transaction. A transaction is a set of transient
     * commands and the final real command. A transactionID only needs to be
     * unique for a short period of time. 0 is reserved as marking a
     * transactionless command.
     *
     * @return The ID
     */
    public int issueTransactionID();

}
