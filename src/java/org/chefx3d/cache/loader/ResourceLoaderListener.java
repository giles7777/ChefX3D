/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007 - 2009
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.cache.loader;

// External imports
import java.io.InputStream;

// Local imports

/**
 * A listener to notify when a resource has been loaded into the cache.
 * 
 * @author Russell Dodds
 * @version $Revision: 1.4 $
 */
public interface ResourceLoaderListener {

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

    /**
     * The resource requested has been loaded
     * 
     * @param resourceURL The url to load
     * @param resourceStream The input stream of the resource
     */
    public void resourceLoaded(String resourcePath, InputStream resourceStream);
  
    /**
     * The resource requested was not found
     * 
     * @param resourceURL The url to load
     * @param responseCode The response code
     */
    public void resourceNotFound(String resourcePath, int responseCode);

}
