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

package org.chefx3d.view.awt.gt2d;
/**
 *  Searches out the entity  and returns 
 * @author Jonathon Hubbard
 * @version $Revision: 1.2 $
 */
public class EntitySearchReturn {
	
	  /** The entity */
    private EntityWrapper entityWrapper;
    
  

    /** The vertex selected if a multiSegment entity */
    private int vertexID;

    /** The segment selected if a multiSegment entity */
    private int segmentID;

    /**
     * Create a search record.
     *
     * @param entity The entity selected or null if nothing found.
     * @param segmentID The segmentID found if multisegmented or -1
     * @param vertexID The vertexID found if multisegmented or -1
     */
    public EntitySearchReturn(EntityWrapper entity, int segmentID, int vertexID) {
        this.entityWrapper = entity;
     
        this.vertexID = vertexID;
        this.segmentID = segmentID;
    }    
   
    /**
     * Get the entity.
     *
     * @return The entity
     */
    public EntityWrapper getEntityWrapper() {
        return entityWrapper;
    }
  

    /**
     * Get the vertexID.
     *
     * @return The vertexID
     */
    public int getVertexID() {
        return vertexID;
    }

    /**
     * Get the segmentID.
     *
     * @return The segmentID
     */
    public int getSegmentID() {
        return segmentID;
    }

}
