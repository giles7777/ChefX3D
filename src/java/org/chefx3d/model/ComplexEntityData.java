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

package org.chefx3d.model;

// External Imports

// Internal Imports

/**
 *
 * @author Russell Dodds
 * 
 * @version $Revision: 1.5 $
 */
public class ComplexEntityData {
    
    private PositionableEntity entity;
    private double[] position;
    private float[] rotation;
    private String combination;
    
    public ComplexEntityData(
            PositionableEntity entity, 
            double[] position, 
            float[] rotation, 
            String combination) {
        
        this.entity = entity;
        
        this.position = new double[3];
        this.position[0] = position[0];
        this.position[1] = position[1];
        this.position[2] = position[2];
        
        this.rotation = new float[4];
        this.rotation[0] = rotation[0];
        this.rotation[1] = rotation[1];
        this.rotation[2] = rotation[2];
        this.rotation[3] = rotation[3];
        
        this.combination = combination;
        
    }
        
    /**
     * @return the tool
     */
    public PositionableEntity getEntity() {
        return entity;
    }
    /**
     * @param entity the tool to set
     */
    public void setEntity(PositionableEntity entity) {
        this.entity = entity;
    }
    /**
     * @return the position
     */
    public double[] getPosition() {
        return position;
    }
    /**
     * @param position the position to set
     */
    public void setPosition(double[] position) {
        this.position[0] = position[0];
        this.position[1] = position[1];
        this.position[2] = position[2];
   }
    /**
     * @return the rotation
     */
    public float[] getRotation() {
        return rotation;
    }
    /**
     * @param rotation the rotation to set
     */
    public void setRotation(float[] rotation) {
        this.rotation[0] = rotation[0];
        this.rotation[1] = rotation[1];
        this.rotation[2] = rotation[2];
        this.rotation[3] = rotation[3];
    }

    /**
     * @return the combination
     */
    public String getCombination() {
        return combination;
    }

    /**
     * @param combination the combination to set
     */
    public void setCombination(String combination) {
        this.combination = combination;
    }
    
}
