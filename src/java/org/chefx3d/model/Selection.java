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
 * A holder of selection data.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
public class Selection {
    /** The entityID selected */
    private int entityID;

    /** The subID selected if a multi-part tool */
    private int segmentID;
    
    /** The vertex id of a multi-part tool */
    private int vertexID;

    /**
     * Constructor for a typical entity.
     * 
     * @param entityID The entityID
     */
    public Selection(int entityID) {
        this.entityID = entityID;
        segmentID = -1;
        vertexID = -1;
    }

    /**
     * Constructor for a sub entity selection.
     * 
     * @param entityID The entityID
     * @param segmentID The segment ID
     * @param vertexID The vertex ID
     */
    public Selection(int entityID, int segmentID, int vertexID) {
        this.entityID = entityID;
        this.segmentID = segmentID;
        this.vertexID = vertexID;
    }

    /**
     * Get the entityID.
     * 
     * @return The entityID
     */
    public int getEntityID() {
        return entityID;
    }

    /**
     * Get the vertexID.
     * 
     * @return The vertexID or -1 if no sub part
     */
    public int getVertexID() {
        return vertexID;
    }
    
    /**
     * Get the segmentID.
     * 
     * @return The segmentID or -1 if no sub part
     */
    public int getSegmentID() {
        return segmentID;
    }    
}