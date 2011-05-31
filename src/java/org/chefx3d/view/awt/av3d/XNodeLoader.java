/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/gpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;

import org.ietf.uri.URL;
import org.ietf.uri.ResourceConnection;
import org.ietf.uri.event.ProgressEvent;

import org.j3d.util.I18nManager;

import org.web3d.vrml.parser.VRMLParserFactory;

import org.web3d.vrml.sav.InputSource;
import org.web3d.vrml.sav.VRMLReader;

// Local imports
import org.chefx3d.ui.LoadingProgressListener;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;


/**
 * Intermediate loader that produces an XNode representation from
 * an X3D file. This representation can then be traversed to produce
 * an alternative representation.
 *
 * @author Rex Melton
 * @version $Revision: 1.9 $
 */
class XNodeLoader {

    /** Error message when loading an X3D file causes an exception */
    private static final String LOAD_ERROR =
        "org.chefx3d.view.awt.av3d.XNodeLoader.loadErrorMsg";

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** I18N manager for sourcing messages */
    private I18nManager i18nMgr;

    /** The root URL of the last loaded model */
    private String loadedURL;

    /** A progress bar notification */
    private LoadingProgressListener progressListener;

    /**
     * Constructor
     */
    public XNodeLoader() {
        this(null, null);
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
     */
    XNodeLoader(LoadingProgressListener progressListener, ErrorReporter reporter) {
        i18nMgr = I18nManager.getManager();
        this.progressListener = progressListener;
        setErrorReporter(reporter);
    }

    /**
     * Register an error reporter
     *
     * @param reporter The new ErrorReporter to use.
     */
    void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the fully qualified URL of the model that was previously loaded.
     */
    String getLoadedURL() {
        return loadedURL;
    }

    /**
     * Return the XNode representation of the specified
     * X3D file.
     *
     * @param url The url of the file to load
     * @return The XNode hierarchy containing the scene instance
     */
    XNode load(String urlString) {
        XNode scene = null;

        // try to retrieve from classpath (look in packaged directories)
        try {
            FileLoader fileLookup = new FileLoader();
            Object[] file = fileLookup.getFileURL(urlString, true);
            
            String baseURL = "";
            int len = 0;
            if (file[0] instanceof URL) {
                URL modelURL = (URL)file[0];                       
                loadedURL = modelURL.toExternalForm();
            } else if (file[0] instanceof java.net.URL) {
                java.net.URL modelURL = (java.net.URL)file[0];                       
                loadedURL = modelURL.toExternalForm();                            
            }
            InputStream modelStream = (InputStream)file[1];
            len = (Integer)file[2];
            
            int index = loadedURL.lastIndexOf("/");
            baseURL = loadedURL.substring(0, index + 1);
   
            // set the progress listener size
            if (progressListener != null)
                progressListener.setMaxSize(len);

            // we need to use this constructor, otherwise
            // the resource is not retrieved correctly
            InputSource is = new InputSource(baseURL, modelStream);
            is.setProgressListener(progressListener);
            is.setReadProgressListener(progressListener, 100);          
            
            X3DContentHandler ch = new X3DContentHandler(errorReporter);
            VRMLParserFactory fac = VRMLParserFactory.newVRMLParserFactory();
            VRMLReader rdr = fac.newVRMLReader();
            rdr.setContentHandler(ch);
 
            rdr.parse(is);
            is.close();
            scene = ch.getScene();

        } catch (IOException ioe) {
            String msg = i18nMgr.getString(LOAD_ERROR) +
            ": "+ urlString;
            errorReporter.errorReport(msg, ioe);       
        }

        return scene;
    }

    /**
     * Return the XNode representation of the specified
     * X3D file.
     *
     * @param file The file to load
     * @return The XNode hierarchy containing the scene instance
     */
    XNode load(File file) {
        XNode scene = null;

        InputSource is = new InputSource(file);
        X3DContentHandler ch = new X3DContentHandler(errorReporter);
        VRMLParserFactory fac = VRMLParserFactory.newVRMLParserFactory();
        VRMLReader rdr = fac.newVRMLReader();
        rdr.setContentHandler(ch);

        try {
            rdr.parse(is);
            is.close();
            scene = ch.getScene();
			
			loadedURL = file.toURI().toURL().toExternalForm();
				
        } catch(Exception e){
            String msg = i18nMgr.getString(LOAD_ERROR) +
                ": "+ file.getAbsolutePath();
            errorReporter.errorReport(msg, e);
		}
        return scene;
    }
}
