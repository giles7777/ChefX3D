/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.catalog.util;

// External Imports
import java.io.File;
import javax.swing.filechooser.FileFilter;

// Internal Imports
// none

public class X3DFileFilter extends FileFilter {

    /** The default x3d file type to use */
    private static final String DEFAULT_EXTENSION = "x3d";

    /** The current extension to filter by */
    private String extension;
 
    /**
     * Create a filter using the default extension
     *
     */
    X3DFileFilter() {
        this.extension = DEFAULT_EXTENSION;
    }

    /**
     * Create a filter using the provided extension
     * 
     * @param extension The file type to filter by
     */
    X3DFileFilter(String extension) {
        this.extension = extension;
    }
    
    //----------------------------------------------------------
    // Methods required by FileFilter
    //----------------------------------------------------------

    /**
     * Is the File a directory or allowed extension type
     */
    public boolean accept( File dir ) {
        
        if (dir.isDirectory()) {
            return true;
        }

        String extension = getExtension(dir);
        
        if (extension != null) {
            if (extension.equals(this.extension)) {
                return true;
            } else {
                return false;
            }
        }

        return false;

    }
   
    /**
     * A description
     * 
     */
    public String getDescription() {
        return "X3D File (*." + extension + ")";
    }
  
    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Set the filter by extension
     * 
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }
 
    /**
     * Get the filter by extension
     * 
     * @return The file type to filter by 
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Get the extension of a file.
     */  
    private String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

}
