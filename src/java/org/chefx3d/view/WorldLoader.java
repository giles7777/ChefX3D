/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view;

// External Imports
import org.web3d.x3d.sai.*;

// Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A world loader on its own thread.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
class WorldLoader extends Thread {
    /** The X3D browser */
    private ExternalBrowser browser;

    /** The url to load */
    private String url;

    /** The world loader listener */
    private WorldLoaderListener listener;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    public WorldLoader(String url, ExternalBrowser browser,
            WorldLoaderListener listener) {
        this.browser = browser;
        this.url = url;
        this.listener = listener;
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Load the world.
     */
    public void run() {
        //errorReporter.messageReport("Loading Location: " + url);
        // Create an X3D scene by loading a file. Blocks till the world is
        // loaded.
        X3DScene mainScene = browser.createX3DFromURL(new String[] { url });

        // Replace the current world with the new one
        browser.replaceWorld(mainScene);

        listener.newWorldLoaded(mainScene);

        browser = null;
        listener = null;
    }
    
    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     * 
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

}