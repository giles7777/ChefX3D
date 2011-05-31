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

package org.chefx3d.view.awt;

// Standard Imports
import java.util.Arrays;

// Application specific imports
// None

/**
 * An container for the identifiers and data necessary to configure
 * the elevation component of an Entity.
 *
 * @author Rex Melton
 * @version $Revision: 1.1 $
 */
class EntityConfigData {
	
	/** The entityID */
	final int entityID; 
	
	/** The vertexID, if the entity is segmented */
	final int vertexID;
	
	/** The initial position of the entity or vertex to configure */
	final double[] position;
	
	/**
	 * Constructor for a non-segmented entity
	 */
	EntityConfigData(int entityID, double[] position) {
		this(entityID, -1, position);
	}

	/**
	 * Constructor for a segmented entity
	 */
	EntityConfigData(int entityID, int vertexID, double[] position) {
		this.entityID = entityID;
		this.vertexID = vertexID;
		this.position = position;
	}
		
	/**
	 * Return whether this identifier is of a segmented entity
	 *
	 * @return true if the entity is segmented, false otherwise
	 */
	boolean isSegmented() {
		return(vertexID != -1);
	}
	
	/**
	 * Return a String description of this
	 *
	 * @return a String description of this
	 */
	public String toString() {
		return(entityID +": "+ vertexID +": "+ java.util.Arrays.toString( position ));
	}
	
	/** 
	 * Return whether this is equivalent to the argument
	 *
	 * @return true if the objects are equivalent, false otherwise
	 */
	public boolean equals(EntityConfigData that) {
		return(
			(this.entityID == that.entityID)&&
			(this.vertexID == that.vertexID)&&
			Arrays.equals(this.position, that.position));
	}
}
