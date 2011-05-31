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

import java.util.Calendar;
import java.util.concurrent.*;

// Local imports
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DefaultErrorReporter;

/**
 * Creates a pool of threads.  Then allows objects to request a 
 * resource to be loading into the cache.  When complete the object 
 * is notified the resource is available.
 * 
 * @author Russell Dodds
 * @version $Revision: 1.3 $
 */
public class ResourceLoader  {

    /** The number of worker threads for resource loading */
    private static final int ICON_THREAD_COUNT = 2;
    private static final int DATA_THREAD_COUNT = 3;

    /** The thread pool service */
    private ExecutorService dataPool;
    
    /** The thread pool service */
    private ExecutorService filePool;

    /** The i18n manager to lookup string labels */
    private I18nManager i18nMgr;

    /** The error reporter to log to */
    private ErrorReporter reporter;
       
    private static ResourceLoader instance;
    
    /**
     * Create an instance of the ResourceLoader class.
     *
     * @param poolSize The number of threads to create
     */
    private ResourceLoader() {

        // setup the i18n manager
        i18nMgr = I18nManager.getManager();       

    }    

    /**
     * Get the singleton instance of this object.  Must call the 
     * initialize the first time it is setup.
     * 
     */
    public static ResourceLoader getResourceLoader() {
        if (instance == null) {
            instance = new ResourceLoader();
        }
        return instance;
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------
    
    /**
     * Setup the Thread pool
     * 
     * @param poolSize
     * @param reporter
     */
    public void initialize(int poolSize, ErrorReporter reporter) {
        
        this.reporter = reporter;
 
        // create a fixed sized thread pool service
        dataPool = Executors.newFixedThreadPool(poolSize);
        filePool = Executors.newFixedThreadPool(poolSize);

    }
    
    /**
     * Get a resource.  Used the default LoadHandler class which assumes the
     * resource is a standard URL
     *
     * @param resourceURL The url to load
     * @param loaderListener The object to notify when complete
     */
    public void loadResource(String resourceURL, ResourceLoaderListener loaderListener) {
                
        // create the load handler
        LoadHandler handler = new LoadHandler(resourceURL, loaderListener, reporter);
        
        // use defaults if not initialized
        if (filePool == null) {
            initialize(ICON_THREAD_COUNT, DefaultErrorReporter.getDefaultReporter());
        }
        
        // request the thread to be executed, will happen once 
        // a thread is available
        filePool.execute(handler);

    }  
    
    /**
     * Get a resource.  Uses the provided handler.
     *
     * @param handler The thread runnable to load
     */
    public void loadResource(Runnable handler) {
        
        // use defaults if not initialized
        if (dataPool == null) {
            initialize(DATA_THREAD_COUNT, DefaultErrorReporter.getDefaultReporter());
        }
        
        // request the thread to be executed, will happen once 
        // a thread is available
        dataPool.execute(handler);       
        
    }  
   
}
