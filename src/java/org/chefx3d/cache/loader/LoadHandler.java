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
import org.j3d.util.I18nManager;

import java.io.InputStream;
import java.io.IOException;

// Local imports
import org.chefx3d.cache.ClientCache;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;


/**
 * @author Russell Dodds
 * @version $Revision: 1.7 $
 */
public class LoadHandler 
    implements 
        Runnable {

    /** The i18n manager to lookup string labels */
    private I18nManager i18nMgr;
    
    /** The client cache manager */
    private ClientCache clientCache;

    /** The URL of the resource to load with this thread */
    private String resourcePath;
  
    /** The ResourceLoaderListener to notify when complete */
    private ResourceLoaderListener loaderListener;
    
    /** The error reporter to log to */
    private ErrorReporter reporter;

    /** A file loader that tries several ways to access a resource */
    private FileLoader fileLookup;
    
    /**
     * Create an instance of the LoadHandler class.
     *
     * @param resourceURL The url to load
     * @param loaderListener The object to notify when complete
     */
    public LoadHandler(
            String resourcePath, 
            ResourceLoaderListener loaderListener, 
            ErrorReporter reporter) {

        this.resourcePath = resourcePath;
        this.loaderListener = loaderListener;
        this.reporter = reporter;
        
        // setup the i18n manager
        i18nMgr = I18nManager.getManager();  
        
        // setup the client cache manager
        clientCache = ClientCache.getInstance();
        
        // setup the file loader utility
        fileLookup = new FileLoader(1);
        
    }    

    //----------------------------------------------------------
    // Methods required by Runnable
    //----------------------------------------------------------

    public void run() {
        
        Object[] result = new Object[4];

        try {
            
            InputStream resourceStream = null;
            
            // check the cache for the resource
            if (clientCache.doesAssetExist(resourcePath)) {
                
                // send the stream back to the requester
                resourceStream = clientCache.retrieveAsset(resourcePath);                           
                
                // notify the requester that it is loaded
                loaderListener.resourceLoaded(resourcePath, resourceStream);

            } else {
                
                // retrieve the resource, this should store 
                // it in the cache auto-magically for us
                result = fileLookup.getFileURL(resourcePath);
                
                if (result[1] != null) {
                    resourceStream = (InputStream)result[1];
                    
                    // notify the requester that it is loaded
                    loaderListener.resourceLoaded(resourcePath, resourceStream);

                } else {
                    loaderListener.resourceNotFound(resourcePath, (Integer)result[3]);
                }
                            
            }
            
        } catch (IOException io) {  
            
            if (result == null || result[3] == null) {
                loaderListener.resourceNotFound(resourcePath, 500);     
            } else {
                loaderListener.resourceNotFound(resourcePath, (Integer)result[3]);     
            }
                  
        } catch (Exception ex) {  
            
            if (result == null || result[3] == null) {
                loaderListener.resourceNotFound(resourcePath, 500);     
            } else {
                loaderListener.resourceNotFound(resourcePath, (Integer)result[3]);     
            }
            
        }
                
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

    
}
