/****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
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

// Local imports
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.view.common.EditorConstants;

/**
 * An object meant to be fed into the selection anchors
 * This objects holds information that may be needed later
 * 
 * @author jonhubba
 * @version $Revision: 1.4 $
 */
class AV3DAnchorInformation implements EditorConstants {

	/** Position of the anchor */
	private double[] position;
	
	/** The anchor identifier */
	private AnchorData anchorData;
	
	/** The associated entity */
	private PositionableEntity entity;
	
	/**
	 * Default Constructor
	 */
	AV3DAnchorInformation(){
		this(AnchorData.NONE, null);
	}

	/**
	 * Constructor
	 *
	 * @param anchorData The anchor identifier
	 * @param entity The associated entity
	 */
	AV3DAnchorInformation(AnchorData anchorData, PositionableEntity entity) {
		position = new double[3];
		this.anchorData = anchorData;
		this.entity = entity;
	}

	/** 
	 * Returns the position as double[]
	 * @return the position
	 */
	double[] getPosition(){
		return position;
	}
	
	/**
	 * Returns the data flag
	 *
	 * @return the AnchorData enum
	 */
	AnchorData getAnchorDataFlag(){
		return(anchorData);
	}
	
	/**
	 * Returns the Entity
	 *
	 * @return the associated Entity
	 */
	PositionableEntity getEntity(){
		return(entity);
	}
	
	/** 
	 * Sets the position
	 *
	 * @param pos The position
	 */
	void setPosition(double[] pos){
		position[0] = pos[0];
		position[1] = pos[1];
		position[2] = pos[2];
	}
}
