/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
// none

// Local Imports
import org.chefx3d.model.ZoneEntity;

/**
 * Defines the requirements of a listener for entity configuration changes.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
interface ConfigListener {
	
	/**
	 * Set the active entity manager 
	 *
	 * @param entityManager The active entity manager 
	 */
	public void setEntityManager(AV3DEntityManager entityManager);
	
	/**
	 * Set the active zone entity
	 *
	 * @param ze The active zone entity
	 */
	public void setActiveZoneEntity(ZoneEntity ze);
	
}
