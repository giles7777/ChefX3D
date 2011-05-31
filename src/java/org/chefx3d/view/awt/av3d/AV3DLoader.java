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

import java.lang.ref.SoftReference;
	
import java.net.MalformedURLException;

import java.util.*;

import org.ietf.uri.URL;

import org.j3d.aviatrix3d.Node;
import org.j3d.aviatrix3d.SharedGroup;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.ui.LoadingProgressListener;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * A loader that produces an aviatrix representation of an X3D file.
 *
 * @author Rex Melton
 * @version $Revision: 1.23 $
 */
public class AV3DLoader {

    /** Warning message aviatrix node cannot be loaded */
    private static final String CANNOT_LOAD_NODE_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.cannotLoadNodeMsg";

    /** Warning message aviatrix node cannot be loaded */
    private static final String CANNOT_LOAD_FILE_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.cannotLoadFileMsg";

    /** Warning message that a url could not be decoded */
    private static final String INVALID_URL_MSG =
        "org.chefx3d.view.awt.av3d.AV3DLoader.invalidURLMsg";

    /** Enable debugging mode for the XNode representation */
    private static final boolean DEBUG = false;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** I18N manager for sourcing messages */
    private I18nManager i18n_mgr;

    /** The aviatrix node factory */
    private AV3DNodeFactory factory;

    /** The filter to use for url requests, null use baseURL logic instead */
    protected URLFilter urlFilter;

    /** Cache for loaded urls */
	private static HashMap<String, SoftReference<Node[]>> cache;

    /** Which urls are being loaded */
    private static Set<String> inProgress;

    /** Listeners to notify when url is loaded */
    private static Map<String, List<LoadListener>> urlListeners;

	/** The GLInfo instance */
	private static GLInfo gl_info;
	
    /** A progress bar notification */
    private LoadingProgressListener progressListener;

    static {
		cache = new HashMap<String, SoftReference<Node[]>>();
        inProgress = Collections.synchronizedSet(new HashSet<String>());
        urlListeners = Collections.synchronizedMap(new HashMap<String, List<LoadListener>>());
    }

    /**
     * Constructor
     */
    public AV3DLoader() {
        this(null, null, null);
    }

    /**
     * Constructor
     *
     * @param urlFilter The filter to use for URL handling
     */
    public AV3DLoader(LoadingProgressListener progressListener, URLFilter urlFilter) {
        this(null, progressListener, urlFilter);
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
     */
    public AV3DLoader(ErrorReporter reporter) {
        this(reporter, null, null);
    }

    /**
     * Constructor
     *
     * @param reporter The ErrorReporter to use.
     * @param urlFilter The filter to use for URL handling
     */
    public AV3DLoader(
            ErrorReporter reporter, 
            LoadingProgressListener progressListener, 
            URLFilter urlFilter) {
        
        i18n_mgr = I18nManager.getManager();
        setErrorReporter(reporter);
        factory = new AV3DNodeFactory(errorReporter, gl_info);
        this.urlFilter = urlFilter;
        this.progressListener = progressListener;

    }

    /**
     * Register an error reporter
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = (reporter != null) ?
            reporter : DefaultErrorReporter.getDefaultReporter();
        if (factory != null) {
            factory.setErrorReporter(errorReporter);
        }
    }
	
    /**
     * Set the GLInfo object
     *
	 * @param info The GLInfo
     */
    public static void setGLInfo(GLInfo info) {
		gl_info = info;
	}
		
    /**
     * Return the aviatrix representation of the specified
     * X3D file. If the file could not be loaded, null is
     * returned.  The content will be
     *
     * @param urlString The url of the file to load
     * @param shared Is a shared copy ok?
     * @param listener The listener to notify when done
     */
    public void loadThreaded(
            final String urlString,
            final boolean shared,
            final LoadListener listener) {

        if (urlString == null)
            return;

        if(!inProgress.contains(urlString)) {
            if (urlListeners.get(urlString) == null) {
                ArrayList<LoadListener> list = new ArrayList<LoadListener>();
                list.add(listener);

                urlListeners.put(urlString, list);
                inProgress.add(urlString);
            }
        } else {
            List<LoadListener> list = urlListeners.get(urlString);
            list.add(listener);

            return;
        }

        Thread t = new Thread() {
            public void run() {
                                               
                Node[] nodes = load(urlString, shared);

                inProgress.remove(urlString);

                List<LoadListener> list = urlListeners.get(urlString);
                urlListeners.remove(urlString);

                Iterator<LoadListener> itr = list.iterator();

                while(itr.hasNext()) {
                    LoadListener listener = itr.next();
                    listener.modelLoaded(nodes);
                }

            }            
        };

        t.start();
    }

    /**
     * Return the aviatrix representation of the specified
     * X3D file. If the file could not be loaded, null is
     * returned.  The content will be
     *
     * @param file The file to load
     * @param shared Is a shared copy ok?
     * @param listener The listener to notify when done
     */
    public void loadThreaded(final File file, final boolean shared,
        final LoadListener listener) {

        Thread t = new Thread() {
            public void run() {
                Node[] nodes = load(file, shared);
                listener.modelLoaded(nodes);
            }
        };

        t.start();
    }

    /**
     * Return the aviatrix representation of the specified
     * X3D file. If the file could not be loaded, null is
     * returned.
     *
     * @param urlString The url of the file to load
     * @param shared Is a shared copy ok?
     * @return The aviatrix representation
     */
    public Node[] load(
            String urlString,
            boolean shared) {
//System.out.println("load: "+ urlString +": "+ shared);
        Node[] av3d = null;
        if (urlString != null) {
            if (shared) {
				SoftReference<Node[]> sr = cache.get(urlString);
				if (sr != null) {
					Node[] ret_val = sr.get();
					if (ret_val != null) {
//System.out.println("cache hit: "+ urlString);
						return(ret_val);
					} else {
//System.out.println("cache hit: "+ urlString +": EMPTY");
						cache.remove(urlString);
					}
				} else {
//System.out.println("cache miss: "+ urlString);
				}
            }

            XNodeLoader xl = new XNodeLoader(progressListener, errorReporter);
            XNode scene = xl.load(urlString);
            ///////////////////////////////////////////////////
            if (DEBUG) {
                XNode.print(scene);
            }
            ///////////////////////////////////////////////////
            if (scene != null) {
                String loadedURL = xl.getLoadedURL();
                factory.setBaseURL(getBaseURL(loadedURL));
                factory.setURLFilter(urlFilter, loadedURL);
                av3d = marshal(scene);
            }
            // if (scene == null), presume that an error message has been
            // generated already from the xnode loader or content handler
        } else {
            String msg = i18n_mgr.getString(CANNOT_LOAD_FILE_MSG) +
                ": "+ urlString;
            errorReporter.warningReport(msg, null);
        }

        if ((av3d != null) && shared) {
            SharedGroup snode = new SharedGroup();

            for (int i=0; i < av3d.length; i++) {
                snode.addChild(av3d[i]);
            }

            av3d = new Node[] {snode};
            cache.put(urlString, new SoftReference<Node[]>(av3d));
        }

        return(av3d);
    }

    /**
     * Return the aviatrix representation of the specified
     * X3D file. If the file could not be loaded, null is
     * returned.
     *
     * @param file The file to load
     * @param shared Is a shared copy ok?
     * @return The aviatrix representation
     */
    public Node[] load(File file, boolean shared) {

        Node[] av3d = null;
        if (file != null) {
			if (shared) {
				String f_name = file.toString();
				SoftReference<Node[]> sr = cache.get(f_name);
				if (sr != null) {
					Node[] ret_val = sr.get();
					if (ret_val != null) {
						return(ret_val);
					} else {
						cache.remove(f_name);
					}
				}
			}

            XNodeLoader xl = new XNodeLoader(progressListener, errorReporter);
            XNode scene = xl.load(file);
            ///////////////////////////////////////////////////
            if (DEBUG) {
                XNode.print(scene);
            }
            ///////////////////////////////////////////////////
            if (scene != null) {
                String loadedURL = xl.getLoadedURL();
                factory.setBaseURL(getBaseURL(loadedURL));
                factory.setURLFilter(urlFilter, loadedURL);
                av3d = marshal(scene);
            }
            // if (scene == null), presume that an error message has been
            // generated already from the xnode loader or content handler
        } else {
            String msg = i18n_mgr.getString(CANNOT_LOAD_FILE_MSG) +
                ": "+ file;
            errorReporter.errorReport(msg, null);
        }

        if ((av3d != null) && shared) {
            String f_name = file.toString();
            cache.put(f_name, new SoftReference<Node[]>(av3d));
        }

        return(av3d);
    }

    /**
     * Return the aviatrix representation of the XNode scene
     *
     * @param scene The XNode representation of the scene
     * @return The aviatrix representation
     */
    private Node[] marshal(XNode scene) {

        ArrayList<XNode> xnode_list =
            (ArrayList<XNode>)scene.getFieldData("children");
        
        if (xnode_list == null) {
            Node[] avnodes = new Node[0];
            return avnodes;            
        }
        
        int num_xnode = xnode_list.size();
        ArrayList<Node> avnode_list = new ArrayList<Node>(num_xnode);

        for (int i = 0; i < num_xnode; i++) {
            XNode xnode = xnode_list.get(i);
            Node avnode = factory.getNode(xnode);
            if (avnode != null) {
                avnode_list.add(avnode);
            }
        }

        Node[] avnodes = new Node[avnode_list.size()];
        avnode_list.toArray(avnodes);
        return(avnodes);
    }

    /**
     * Return the base URL
     *
     * @param url The initial URL
     * @return the base URL
     */
    private URL getBaseURL(String urlString) {
        URL baseURL = null;

        try {
            int index = urlString.lastIndexOf("/");
            String base = urlString.substring(0, index+1);
            baseURL = new URL(base);
        } catch (MalformedURLException murle) {
            String msg = i18n_mgr.getString(INVALID_URL_MSG) +
                ": "+ urlString;
            errorReporter.warningReport(msg, murle);
        }

//System.out.println("***BaseURL: " + baseURL);
        return(baseURL);
    }

    /**
     * Return the base URL
     *
     * @param file The File
     * @return the base URL
     */
    private URL getBaseURL(File file) {
        URL baseURL = null;
        baseURL = getBaseURL(file.getAbsolutePath());
        return(baseURL);
    }
}
