package org.chefx3d.model;

public interface AbstractVertexTool {

    /**
     * Is the tool a line tool or does it allow multiple paths.
     *
     * @return True if its restricted to a single line
     */
    public boolean isLine();

    /**
     * Is the tool a line tool or does it allow multiple paths.
     *
     * @return True if its restricted to a single line
     */
    public float getSegmentLength();

    /**
     * Does this tool create a basic square shape by default.
     * 
     * @return the createDefaultShape
     */
    public boolean isCreateDefaultShape();

    /**
     * Sets whether this tool create a basic square shape by default.
     * 
     * @param createDefaultShape the createDefaultShape to set
     */
    public void setCreateDefaultShape(boolean createDefaultShape);

}