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

package org.chefx3d.view.common;

// External imports
// none

// Local imports
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.view.boundingbox.OrientedBoundingBox;

/**
 * Defines the requirements of an entity wrapper.
 * 
 * @author Rex Melton
 * @version $Revision: 1.3 $
 */
public interface EntityWrapper {
	
	/**
	 * Return the Entity that this wrapper represents.
	 *
	 * @return The Entity that this wrapper represents.
	 */
	public PositionableEntity getEntity();

	/**
	 * Return the default bounds.
	 *
	 * @return The default bounds
	 */
	public OrientedBoundingBox getBounds();
	
	/**
	 * Return the extended bounds. If no extended bounds are
	 * defined, the default bounds object is returned.
	 *
	 * @return The extended bounds if available. Default bounds
	 * otherwise.
	 */
	public OrientedBoundingBox getExtendedBounds();
	
	/**
	 * Set the enable state of the entity wrapper
	 *
	 * @param state The enable state of the entity wrapper
	 */
	public void setEnabled(boolean state);
	
	/**
	 * Return the enable state of the entity wrapper
	 *
	 * @return true if this is enabled, false otherwise
	 */
	public boolean isEnabled();
}
