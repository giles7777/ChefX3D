/*****************************************************************************
 *                        Web3d.org Copyright (c) 2006
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.swing.ImageIcon;

// Internal Imports
import org.chefx3d.util.CloneableProperty;
import org.chefx3d.util.FileLoader;

public class ListProperty extends AbstractProperty {

    /** The valid text for the list  */
    protected String[] labels;

    /** The valid keys for the list  */
    protected String[] keys;

    /** The image paths used for the list  */
    protected String[] imagePaths;

    /** The valid images in the list  */
    protected ImageIcon[] validImages;
    
    /** The current index */
    protected int curIndex;
    
    /**
     * Base constructor
     */
    public ListProperty() {
        super();
    }

    /**
     * Constructor that includes the valid list
     * Sets currentSelection to the first item in the list
     *
     * @param validTypes The valid types in the list
     */
    public ListProperty(String[] keys, String[] labels) {

        this();
        
        this.keys = keys;
        this.labels = labels;
        this.validImages = null;

        value = 0;

    }

    /**
     * Constructor that includes the valid list
     * Sets currentSelection to the first item in the list
     *
     * @param validTypes The valid types in the list
     * @param imagePaths The valid images in the list
     */
    public ListProperty(String[] keys, String[] labels, String[] imagePaths) {

        this(keys, labels);

        // convert paths to image objects
        setValidImages(imagePaths);

    }

    // ---------------------------------------------------------------
    // Methods required by CloneableProperty
    // ---------------------------------------------------------------

    public CloneableProperty clone() {

        // Create the new copy
        ListProperty out = new ListProperty();

        out.setValue(value);
        out.setVisible(visible);
        out.setEditable(editable);
        out.setLabels(labels.clone());
        out.setKeys(keys.clone());

        if (imagePaths != null) {
            out.setValidImages(imagePaths.clone());
        }

        return out;
    }    
    
    // ---------------------------------------------------------------
    // Overridden Methods
    // ---------------------------------------------------------------

    /**
     * Set the value of the property
     * 
     * @param value The value to set
     */
    public void setValue(Object value) {        
        Integer index = (Integer)value;
        if (index != null && index >= 0) {
            super.setValue(value);        
            curIndex = (Integer)value;
        }
    }
   
    // ---------------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------------

    /**
     * Get the currently selected value based on the index
     */
    public String getSelectedValue() {
        Integer index = (Integer)getValue();
        if (index < 0 || index >= labels.length) {
            index = curIndex;
        }
        curIndex = index;
        
        return labels[index];
    }
 
    /**
     * Get the currently selected key based on the index
     */
    public String getSelectedKey() {
        Integer index = (Integer)getValue();
        if (index < 0 || index >= keys.length) {
            index = curIndex;
        }
        curIndex = index;
        
        return keys[index];
    }

    /**
     * Initialize the property
     *
     * @param worldModel The model that contains the scene data
     * @param entity The entity assigned to the property
     */
    public void initialize(WorldModel worldModel, Entity entity) {

        this.model = worldModel;
        this.parentEntity = entity;

    }

    /**
     * @return the validTypes
     */
    public String[] getLabels() {
        return labels;
    }

    /**
     * @param validTypes the validTypes to set
     */
    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    /**
     * @return the validTypes
     */
    public String[] getKeys() {
        return keys;
    }

    /**
     * @param validTypes the validTypes to set
     */
    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    /**
     * Get the current set of images used
     *
     * @return the validTypes
     */
    public ImageIcon[] getValidImages() {
        return validImages;
    }

    /**
     * Get the current set of image paths used
     *
     * @return the imagePaths
     */
    public String[] getValidImagePaths() {
        return imagePaths;
    }

    /**
     * Set the current list of images to use
     *
     * @param validImages The array of validImages to set
     */
    public void setValidImages(String[] imagePaths) {

        this.imagePaths = imagePaths;

        // convert paths to image objects
        validImages = new ImageIcon[imagePaths.length];
        for (int i = 0; i < imagePaths.length; i++) {
            validImages[i] = getImage(imagePaths[i]);
        }

    }

    /**
     * Lookup the image using the URL provided
     *
     * @param imageURL
     * @return The Image object
     */
    private ImageIcon getImage(String imageURL) {

        ImageIcon icon = new ImageIcon();

        try {
            // try to retrieve from the classpath
            FileLoader fileLookup = new FileLoader();

            Object[] file = fileLookup.getFileURL(imageURL);

            //now process the raw data into a buffer
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = ((InputStream)file[1]).read(buf)) != -1;) {
                bos.write(buf, 0, readNum);
            }
            byte[] bytes = bos.toByteArray();

            icon = new ImageIcon(bytes);

        } catch (IOException ioe) {
            // TODO: add a dummy icon?
            System.out.println("Error processing image: " + imageURL);
        }
        return icon;

    }
}
