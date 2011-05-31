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
 * Data container for storing PositionableEntity properties as a single bundle.
 *
 * @author Alan Hudson
 * @version $Revision: 1.2 $
 */
public class PositionableData {
    public double[] pos;
    public float[] rot;
    public float[] scale;

    public PositionableData() {
        pos = new double[3];
        rot = new float[4];
        scale = new float[3];
    }

    public PositionableData(double[] pos, float[] rot, float[] scale) {
        this();

        copyArray(pos, this.pos);
        copyArray(rot, this.rot);
        copyArray(scale, this.scale);
    }

    /**
     * Copy the values of an array into another array.  Arrays must be the same size.
     *
     * @param src The source
     * @param dest The dest
     */
    private void copyArray(float[] src, float[] dest) {
        if (src.length != dest.length)
            throw new IllegalArgumentException("Source and Destination arrays are not the same size");

        int len = src.length;

        for(int i=0; i < len; i++) {
            dest[i] = src[i];
        }
    }

    /**
     * Copy the values of an array into another array.  Arrays must be the same size.
     *
     * @param src The source
     * @param dest The dest
     */
    private void copyArray(double[] src, double[] dest) {
        if (src.length != dest.length)
            throw new IllegalArgumentException("Source and Destination arrays are not the same size");

        int len = src.length;

        for(int i=0; i < len; i++) {
            dest[i] = src[i];
        }
    }
}
