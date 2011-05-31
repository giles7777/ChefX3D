/*****************************************************************************
 *
 *                            (c) Yumetech 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 ****************************************************************************/
package org.chefx3d.cache.protocol.https;

// Standard imports
import java.util.*;

import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

// Application specific imports
import org.ietf.uri.URI;
import org.ietf.uri.URL;
import org.ietf.uri.HttpResourceConnection;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.ParseException;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.ProtocolNotSuppException;
import org.chefx3d.cache.protocol.ProtocolUtils;

/**
 * The default implementation of a HTTP resource connection.
 * <P>
 * The code in this class is heavily influenced by the original
 * HttpURLConnection implementation provided by the
 * <A HREF="http://www.innovation.ch/java/HTTPClient/">Innovation
 * HTTPClient</A>. Due to the technical differences, this is a clean cut
 * implementation of the same strategy.
 * <P>
 *
 * The current implementation ignores the settings for proxy host and
 * proxy port.
 * <P>
 * This connection proxies requests for inputstreams and certain headers through the 
 * ProtocolUtilities object, which then talks to the ClientCache singleton
 * It checks to see if the asset pointed to by the url exists, and if so, then
 * retrieves a stream from the cache instead of making a connection
 * <P>
 * For details on URIs see the IETF working group:
 * <A HREF="http://www.ietf.org/html.charters/urn-charter.html">URN</A>
 * <P>
 *
 * This softare is released under the
 * <A HREF="http://www.gnu.org/copyleft/lgpl.html">GNU LGPL</A>
 * <P>
 *
 * DISCLAIMER:<BR>
 * This software is the under development, incomplete, and is
 * known to contain bugs. This software is made available for
 * review purposes only. Do not rely on this software for
 * production-quality applications or for mission-critical
 * applications.
 * <P>
 *
 * Portions of the APIs for some new features have not
 * been finalized and APIs may change. Some features are
 * not fully implemented in this release. Use at your own risk.
 * <P>
 *
 * <b>"Freshness Checking"</b>
 *
 * Once content has been downloaded, updates will only be checked for if the
 * following system property is set, and the value is 'true'.
 *
 * org.chefx3d.cache.checkForUpdates = true
 *
 * @author  Justin Couch, Daniel Joyce
 * @version 0.7 (27 August 1999)
 */
public class HttpsConnection extends HttpResourceConnection {

    /** Not defined in parent classes, so defined here */
    public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

    /** Protocol utility class*/
    private ProtocolUtils protocolUtils = new ProtocolUtils();

    /**
     *  A header 0 place keeper key. Should never be seen outside this
     * class. Outside users always see "null"
     */
    private static final String HEADER_0_KEY = "header_0";

    /** The name of the default redirect module */
    private static final String REDIRECT_MODULE = "HTTPClient.RedirectionModule";

    /** The name of the default cookie handling module */
    private static final String COOKIE_MODULE = "HTTPClient.CookieModule";

    /** How many bytes should the default size of the output stream be */
    private static final int DEFAULT_OUTPUT_SIZE = 1024;

    /** a list of HTTPConnections */
    private static Map connections = new HashMap();

    /** The RedirectionModule class */
    private static Class redir_module_class;

    /** The CookieModule class */
    private static Class cookie_module_class;

    /** The current connection */
    private HTTPClient.HTTPConnection current_connection;

    /** The resource that we want to fetch on the server*/
    private String resource;

    /** The response from the stream */
    private HTTPResponse response;

    /** The output stream used for POST and PUT */
    private ByteArrayOutputStream output_stream;

    /** The list of header and parameter information */
    private List header_keys = null;

    /** The headers indexed by string as a hash table */
    private Map header_map = null;


    static {
        // get the RedirectionModule class
        try {
            redir_module_class = Class.forName(REDIRECT_MODULE);
        } catch (ClassNotFoundException cnfe) {
            // hmmmm... oh well. Ignore it then
        }

        // get the CookieModule class
        try {
            cookie_module_class = Class.forName(COOKIE_MODULE);
        } catch (ClassNotFoundException cnfe) {
            // hmmmm... oh well. Ignore it then
        }

        // load all the proxy names into the HTTPConnection. The only potential
        // problem is in the order of execution. I think this initializer gets
        // called *after* the base class initializer. If so, that is OK because
        // by then all the proxy values have been read and parsed
        ListIterator iterator = nonProxiedHosts.listIterator();

        while (iterator.hasNext()) {
            try {
                HTTPConnection.dontProxyFor((String) iterator.next());
            } catch (ParseException pe) {
                // ignore it and move on
            }
        }

        HTTPConnection.setProxyServer(proxyHost, proxyPort);
    }

    /**
     * Construct a connection to the specified url. A cache of
     * HTTPConnections is used to maximize the reuse of these across
     * multiple HttpURLConnections.
     *
     * @exception ProtocolNotSuppException if the protocol is not supported
     */
    public HttpsConnection(String host, int port, String path)
          throws MalformedURLException {
        super(new URL(URI.HTTPS_SCHEME, host, port, path));

        // now setup stuff
        resource = path;

        // try the cache, using the host name

        String php = host + ':' + port;

        current_connection = (HTTPConnection) connections.get(php);

        if (current_connection == null) {
            // Not in cache, so create new one and cache it
            try {
                current_connection =
                      new HTTPConnection(URI.HTTPS_SCHEME, host, port);
                connections.put(php, current_connection);
            } catch (ProtocolNotSuppException pnse) {
                // hmm.. barf
                throw new MalformedURLException(pnse.toString());
            }
        }
    }

    /**
     * Closes all the connections to this server. The connection is stopped
     * but the basic class is kept around in the internal cache.
     */
    public void disconnect() {
        current_connection.stop();
    }

    // should override setFollowRedirects here for the redirect module to deal
    // with.

    // Override setMaxRedirects and pass that number through to the module

    // deal with the set cookie method. Problem is method is static

    // Lots more problems with static methods for proxy information
    /**
     * Connects to the server (if connection not still kept alive) and
     * issues the request.
     *
     * @exception IOException An I/O error occurred during the connection
     */
    public synchronized void connect()
          throws IOException {
        if (connected || (protocolUtils.doesAssetExist(getURI()) &&
              GET_METHOD.equals(method) && !protocolUtils.checkFreshness())) {
            return;
        }

        synchronized (current_connection) {
//      current_connection.setAllowUserInteraction(allowUserInteraction);

            if (followRedirects) {
                current_connection.addModule(redir_module_class, -1);
            } else {
                current_connection.removeModule(redir_module_class);
            }

            try {
                if (GET_METHOD.equals(method)) {
                    // Check freshness of items in cache vs remote host
                    if (protocolUtils.checkFreshness() && protocolUtils.
                          doesAssetExist(getURI())) {

                        NVPair nVPair =
                              new NVPair("If-Modified-Since",
                              protocolUtils.getLastModifiedString(getURI()));

                        response =
                              current_connection.Get(resource, new NVPair[0],
                              new NVPair[]{nVPair});
                    } else {
                        response = current_connection.Get(resource);
                    }
                } else if (POST_METHOD.equals(method)) {
                    output_stream.flush();
                    byte[] data = output_stream.toByteArray();
                    response = current_connection.Post(resource, data);
                } else if (HEAD_METHOD.equals(method)) {
                    response = current_connection.Head(resource);
                } else if (OPTIONS_METHOD.equals(method)) {
                    response = current_connection.Options(resource);
                } else if (PUT_METHOD.equals(method)) {
                    output_stream.flush();
                    byte[] data = output_stream.toByteArray();
                    response = current_connection.Put(resource, data);
                } else if (DELETE_METHOD.equals(method)) {
                    response = current_connection.Delete(resource);
                } else if (TRACE_METHOD.equals(method)) {
                    response = current_connection.Trace(resource);
                }
            } catch (ModuleException e) {
                // If it doesn't exist in the cache, we'll throw
                // a exception
                if (!protocolUtils.doesAssetExist(getURI())) {
                    throw new IOException(e.toString());
                }
            }
        }

        connected = true;
    }

    /**
     * Get the response code. Calls connect() if not connected.
     *
     * @return the http response code returned.
     * @exception IOException An I/O error occurred during the response
     */
    public int getResponseCode()
          throws IOException {
        if (!connected) {
            connect();
        }

        int status = INVALID_HTTP_RESPONSE;

        try {
            status = response.getStatusCode();
        } catch (ModuleException me) {
            throw new IOException(me.toString());
        }

        return status;
    }

    /**
     * Get the response message describing the response code. Calls connect()
     * if not connected.
     *
     * @return the http response message returned with the response code.
     * @exception IOException An I/O error occurred during the response
     */
    public String getResponseMessage()
          throws IOException {
        if (!connected) {
            connect();
        }

        String msg = INVALID_RESPONSE_MSG;

        try {
            msg = response.getReasonLine();
        } catch (ModuleException me) {
            throw new IOException(me.toString());
        }

        return msg;
    }

    /**
     * Get the value part of a header. Calls connect() if not connected.
     *
     * @param  name the of the header.
     * @return the value of the header, or null if no such header was returned.
     */
    public String getHeaderField(String name) {
        String ret_val = null;

        try {
            if (!connected) {
                connect();
            }

            if (header_keys == null) {
                parseHeaders();
            }

            ret_val = (String) header_map.get(name);
        } catch (Exception e) {
        }

        return ret_val;
    }

    /**
     * Gets header name of the n-th header. Calls connect() if not connected.
     * The name of the 0-th header is <var>null</var>, even though it the
     * 0-th header has a value.
     *
     * @param n which header to return.
     * @return the header name, or null if not that many headers.
     */
    public String getHeaderFieldKey(int n) {
        String ret_val = null;

        if (header_keys == null) {
            parseHeaders();
        }

        if ((n >= 0) && (n < header_keys.size())) {
            ret_val = (String) header_keys.get(n);
        }

        return ret_val;
    }

    /**
     * Gets header value of the n-th header. Calls connect() if not connected.
     * The value of 0-th header is the Status-Line (e.g. "HTTP/1.1 200 Ok").
     *
     * @param n which header to return.
     * @return the header value, or null if not that many headers.
     */
    public String getHeaderField(int n) {
        if (header_keys == null) {
            parseHeaders();
        }

        String ret_val = null;

        if ((n >= 0) && (n < header_keys.size())) {
            Object key = header_keys.get(n);
            ret_val = (String) header_map.get(key);
        }

        return ret_val;
    }

    /**
     * Parse and cache the list of headers. This way, even when the response
     * is closed we can get hold of the header information.
     */
    private void parseHeaders() {
        header_map = new HashMap();
        header_keys = new ArrayList();

        if (protocolUtils.doesAssetExist(getURI()) && GET_METHOD.equals(method) && !protocolUtils.
              checkFreshness()) {
            header_map = protocolUtils.getAllHeaders(getURI());
            header_keys = new ArrayList(header_map.keySet());
        } else {



            try {
                if (!connected) {
                    connect();
                }

                // the 0'th field is special so fix it up
                header_keys.add(HEADER_0_KEY);

                StringBuffer header_zero =
                      new StringBuffer(response.getVersion());
                header_zero.append(' ');
                header_zero.append(response.getStatusCode());
                header_zero.append(' ');
                header_zero.append(response.getReasonLine());

                header_map.put(HEADER_0_KEY, header_zero.toString());

                // fill arrays
                Enumeration headers = response.listHeaders();
                while (headers.hasMoreElements()) {
                    String key = (String) headers.nextElement();
                    header_keys.add(key);
                    header_map.put(key, response.getHeader(key));
                }
                // Store the headers only if we get them back with a HTTP_OK result
                if (protocolUtils.isCachableURI(getURI()) && getResponseCode() == HTTP_OK) {
                    protocolUtils.storeHeadersInCache(getURI(), header_keys, header_map);
                }
            } catch (Exception e) {
                // just ignore it and leave it all blank
            }
        }
    }

    /**
     * Gets an input stream from which the data in the response may be read.
     * If the url is already in the cache, returns a inputstream to the url
     * contents in the cache
     * If the url is not in the cache, checks with ProtocolUtils to see if the
     * url should be cached. If it should, the response is stored in the cache,
     * and then a new inputstream pointing to the response contents in the cache
     * is returned
     * Otherwise, returns the response inputstream
     *
     * Calls connect() if not connected.
     *
     * @return The InputStream from the connection
     * @exception IOException if input not enabled
     * @see ProtocolUtils
     * @see ClientCache
     */
    @Override
    public InputStream getInputStream() throws IOException {
        InputStream stream = null;

        if (protocolUtils.doesAssetExist(getURI()) && GET_METHOD.equals(method) && !protocolUtils.
              checkFreshness()) {
            stream = protocolUtils.retrieveAsset(getURI());
        } else {

            if (!connected) {
                connect();
            }

            int resp = getResponseCode();
            if (resp == HTTP_NOT_MODIFIED) {
                if (protocolUtils.doesAssetExist(getURI())) {
                    stream = protocolUtils.retrieveAsset(getURI());
                } else {
                    throw new IOException("Resource Not Modified " + getURI() + "\n" +
                          "But local copy not found in cache!");
                }
            } else if (resp != HTTP_OK) {
                throw new IOException("File not found. " + getURI() + "\n" +
                      "Response code: " + resp);
            } else {

                try {
                    stream = response.getInputStream();
                    // If this is a cachable url, store the url in the cache
                    // and return the inputstream from there
                    if (protocolUtils.isCachableURI(getURI())) {
                        protocolUtils.storeAsset(getURI(), stream);
                        stream.close();
                        stream = protocolUtils.retrieveAsset(getURI());
                    }
                    String enc = getContentEncoding();
                    if ((enc != null) && enc.equals("x-gzip")) {
                        stream = new GZIPInputStream(stream);
                    }
                } catch (ModuleException e) {
                    throw new IOException(e.toString());
                }
                if (stream == null) {
                    throw new IOException("Unable to get inpustream from connection!");
                }
            }
        }
        return stream;
    }

    /**
     * Returns the error stream if the connection failed
     * but the server sent useful data nonetheless.
     * <P>
     * This method will not cause a connection to be initiated.
     *
     * @return an InputStream, or null if either the connection hasn't
     *         been established yet or no error occured
     */
    public InputStream getErrorStream() {

        // If it exists in our local, obviously there was no error grabbing it!
        try {
            if (protocolUtils.doesAssetExist(getURI())) {
                return null;
            }
        } catch (Exception ex) {
            // TODO Logging
            return null;
        }

        InputStream stream = null;
        try {
            // !doInput || removed
            if (!(!connected ||
                  response.getStatusCode() < 300 ||
                  getContentLength() <= 0)) {
                stream = response.getInputStream();
            }
        } catch (Exception e) {
        }

        return stream;
    }

    /**
     * Get an output stream for this URL. The output stream is a place where
     * the application may place raw data (such as a file) to be placed on the
     * server using the POST or PUT methods. For the current implementation, the
     * type is hardcoded to <CODE>application/octet-stream</CODE>.
     * <P>
     * The application must finish using this stream <I>before</I> calling
     * connect. At the point that connect is called, the bytes in this stream
     * are extracted and sent to the server as is.
     * <P>
     * If multiple calls are made to this method, the same instance is always
     * returned. Therefore, if you wish to put new data in, you will need to
     * reset the stream before beginning.
     *
     * @return An output stream to write information to
     * @exception IOException The connection is open or other I/O error
     */
    public OutputStream getOutputStream()
          throws IOException {
        if (connected) {
            throw new IOException("The stream is currently open");
        }

        if (output_stream != null) {
            output_stream = new ByteArrayOutputStream(DEFAULT_OUTPUT_SIZE);
        }

        return output_stream;
    }

    /**
     * Gets the URI for this connection. If we're connect()'d and the request
     * was redirected then the URL returned is that of the final request.
     *
     * @return the final URI, or null if any exception occured.
     */
    public URI getURI() {
        URI ret_val = null;

        if (connected) {
            try {
                java.net.URL java_url = response.getEffectiveURI().toURL();

                if (java_url != null) {
                    ret_val = new URL(java_url);
                }
            } catch (Exception e) {
            }
        } else {
            ret_val = super.getURI();
        }

        return ret_val;
    }

    /**
     * Shows if request are being made through an http proxy or directly.
     *
     * @return true if an http proxy is being used.
     */
    public boolean usingProxy() {
        return (current_connection.getProxyHost() != null);
    }

    //----------------------------------------------------------------------
    // Defined in HttpResourceConnection
    //----------------------------------------------------------------------
    /**
     * Overrides null version in org.ietf.uri.ResourceConnection
     *
     * @return the content encoding
     */
    @Override
    public String getContentEncoding() {
        return getHeaderField(CONTENT_ENCODING_HEADER);
    }

    @Override
    public int getContentLength() {
        parseHeaders();
        int size = -1;
        if (protocolUtils.doesAssetExist(getURI())) {
            long assetSize = protocolUtils.getAssetSize(getURI());
            if (assetSize > Integer.MAX_VALUE) {
                size = -1;
            } else {
                size = (int) assetSize;
            }
        } else {
            try {
                String len = getHeaderField(CONTENT_LENGTH_HEADER);
                size = Integer.parseInt(len);
            } catch (Exception ex) {
            }
        }
        return size;
    }
}

