/*****************************************************************************
 *                        Web3d.org Copyright (c) 2001 - 2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.util;

// External imports
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import org.ietf.uri.URL;
import org.ietf.uri.ResourceConnection;
import org.ietf.uri.HttpResourceConnection;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A convenience class that returns the URL of a resource located
 * in the JAR, the classpath, or the file system
 * <p>
 * 
 * @author Russell Dodds
 * @version $Revision: 1.15 $
 */
public class FileLoader {

	/** Debug flag for enabling keepAlive on url connections */
	private static final boolean ENABLE_KEEP_ALIVE = false;
	
    /** The number of chances to retry */
    protected static final int RETRY_ATTEMPTS = 3;
    
    /** The connection timeout in miliseconds */
    protected static final int CONNECTION_TIMEOUT = 3000;

    /** The name of the file to load */
    private String filename;
    
    /** The URL return type */
    private int type = 0;
    
    /**
     * Helper class used to load resources.  By default this returns an
     * org.ietf.url.URL.  Use secondary constructor to get a 
     * java.net.URL return type.
     */
    public FileLoader() {
        this(0);
    }
   
    /**
     * Helper class used to load resources
     * 
     * @param type Set the return URL type, 
     *  0 = org.ietf.url.URL 
     *  1 = java.net.URL
     */
    public FileLoader(int type) {
        this.type = type;
    }

    /**
     * Retreive a URL of a file for the provided file path. Looks in the 
     * classpath for the file so the path provided must be fully qualified 
     * relative to the classpath.
     * 
     * @param path The path to lookup. If not found, returns null
     * @param returnResource Flag used to decide to lookup resource stream
     * @return [0] = A URL to the file (either a java.net.URL or a org.ietf.url.URL)
     *         [1] = An InputStream to the file
     */
    public Object[] getFileURL(String path) throws IOException {
        return getFileURL(path, true);
    }
    
    /**
     * Retreive a URL of a file for the provided file path. Looks in the 
     * classpath for the file so the path provided must be fully qualified 
     * relative to the classpath.
     * 
     * @param path The path to lookup. If not found, returns null
     * @return [0] = A URL to the file
     *         [1] = An InputStream to the file
     *         [2] = The length [int] of the file
     *         [3] = The return value
     */
    public Object[] getFileURL(String path, boolean returnResource) throws IOException {

        // set the file name
        this.filename = path;        

        Object[] retVal = new Object[4];
        
        if (filename == null)
            return retVal;
                        
        // if URL or URI then just get the object
        if (filename.startsWith("http:") || 
            filename.startsWith("file:")) {


            if (type == 0) {
                
                URL url = new URL(filename);
                
                if (returnResource) {
                    
                    if (filename.startsWith("http:")) {
                        
                        // set the timeout
                        HttpResourceConnection conn = 
                            (HttpResourceConnection)url.getResource();
                        conn.connect();
                        
                        // get the stream
                        try {
                            retVal[1] = conn.getInputStream();
                            retVal[2] = conn.getContentLength();
                            retVal[3] = conn.getResponseCode();
                        } catch (IOException ioe) {
                            retVal[3] = conn.getResponseCode();
                            throw ioe;
                        }

                    } else if (filename.startsWith("file:")) {
                        // set the timeout
                        ResourceConnection conn = 
                            url.getResource();
                        conn.connect();
                        
                        // get the stream
                        retVal[1] = conn.getInputStream();
                        retVal[2] = conn.getContentLength();
                        retVal[3] = HttpResourceConnection.HTTP_OK;
     
                    }
                                      
                }
                retVal[0] = url;
                
                
            } else {
                
                java.net.URL url = new java.net.URL(filename);
                
                if (returnResource) {
                    
                    java.net.HttpURLConnection connection = 
                        (java.net.HttpURLConnection)url.openConnection();
                    connection.setConnectTimeout(CONNECTION_TIMEOUT);
					if (!ENABLE_KEEP_ALIVE) {
						connection.setRequestProperty("Connection", "Close");
					}
                    
                    // get the stream
                    try {
                        //retVal[1] = url.openStream();
						retVal[1] = connection.getInputStream();
                        retVal[2] = connection.getContentLength();
                        retVal[3] = connection.getResponseCode();
                    } catch (IOException ioe) {
                        retVal[3] = connection.getResponseCode();
                        throw ioe;
                    }
                }
                retVal[0] = url;
            }
                                     
            
        } else if (filename.startsWith("https:")) {
                               
            java.net.URL url = new java.net.URL(filename);
            
            if (returnResource) {
                
                java.net.HttpURLConnection connection = 
                    (java.net.HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
				if (!ENABLE_KEEP_ALIVE) {
					connection.setRequestProperty("Connection", "Close");
				}

                // get the stream
                try {
                    //retVal[1] = url.openStream();
					retVal[1] = connection.getInputStream();
                    retVal[2] = connection.getContentLength();
                    retVal[3] = connection.getResponseCode();
                } catch (IOException ioe) {
                    retVal[3] = connection.getResponseCode();
                    throw ioe;
                }
 
            }
            retVal[0] = url;
            
        } else {
            
            // need to handle the case where its already been processed once by
            // the FileLoader, revert it back to the relative path
            if (filename.startsWith("jar:")) {
                int index = filename.indexOf("!");
                filename = filename.substring(index + 1);
                if (filename.startsWith("/")) {
                    filename = filename.substring(1);
                }
            }
 
            if (returnResource) {
                
                // try to retrieve from the classpath   
                retVal = (Object[])AccessController.doPrivileged(
                        new PrivilegedAction() {
                            public Object run() {
  
                                ClassLoader cl = ClassLoader.getSystemClassLoader();   
                                InputStream is = cl.getSystemResourceAsStream(filename);                                  
                                java.net.URL url = cl.getSystemResource(filename);

                                // WebStart fallback
                                if(url == null) {
                                    cl = FileLoader.class.getClassLoader();
                                    is = cl.getResourceAsStream(filename);
                                    url = cl.getResource(filename);
                                }                               
                                
                                return new Object[] {
                                        url, 
                                        is, 
                                        0, 
                                        HttpResourceConnection.HTTP_OK};
                            }
                        }
                    ); 
           
            } else {
                
                // try to retrieve from the classpath   
                retVal = (Object[])AccessController.doPrivileged(
                        new PrivilegedAction() {
                            public Object run() {
                                 
                                ClassLoader cl = ClassLoader.getSystemClassLoader();   
                                java.net.URL url = cl.getSystemResource(filename);

                                // WebStart fallback
                                if(url == null) {
                                    cl = FileLoader.class.getClassLoader();
                                    url = cl.getResource(filename);
                                }
                                
                                return new Object[] {
                                        url, 
                                        null, 
                                        0, 
                                        HttpResourceConnection.HTTP_OK};
                            }
                        }
                    ); 
      
            }
            
            // case the java.net.URL if necessary
            if (type == 0 && retVal[0] instanceof java.net.URL) {               
                retVal[0] = new URL((java.net.URL)retVal[0]);
            }
            
            if (retVal[0] == null) {
                
                // as a last resort look for it on the 
                // file system relative to the application
                File iconFile = new File(filename);            
                if (iconFile.exists()) {
                        
                    if (type == 0) {
                        
                        URL url = new URL(iconFile.getAbsolutePath());
                        
                        if (returnResource) {
                            
                            HttpResourceConnection conn = 
                                (HttpResourceConnection)url.getResource();
                            conn.connect();
                            
                            retVal[1] = conn.getInputStream();
                            retVal[2] = (int)iconFile.length();
                            retVal[3] = HttpResourceConnection.HTTP_OK;
                        }
                        retVal[0] = url;
                        
                        
                    } else {
                        
                        java.net.URL url = new java.net.URL(iconFile.getAbsolutePath());
                        
                        if (returnResource) {
                            
                            java.net.URLConnection connection = url.openConnection();
                            connection.setConnectTimeout(CONNECTION_TIMEOUT);
							if (!ENABLE_KEEP_ALIVE) {
								connection.setRequestProperty("Connection", "Close");
							}

                            // get the stream
                            //retVal[1] = url.openStream();
							retVal[1] = connection.getInputStream();
                            retVal[2] = connection.getContentLength();
                            retVal[3] = HttpResourceConnection.HTTP_OK;
                        }
                        retVal[0] = url;                        
                        
                    }
                             
                } 

            } 
            
        }
 
        return retVal;
        
    }

}