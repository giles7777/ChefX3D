/*****************************************************************************
 *
 *                            (c) Yumetech 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 ****************************************************************************/
package org.chefx3d.cache.javanet.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chefx3d.cache.HeaderUtils;
import org.chefx3d.cache.protocol.ProtocolUtils;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Http connection proxy for java.net handlers that passes through to the
 * cache if needed
 *
 * Only handles caching of results where method is GET.
 *
 * Delegates all calls to sun.net.www.protocol.http.HttpURLConnection
 *
 * <b>"Freshness Checking"</b>
 *
 * Once content has been downloaded, updates will only be checked for if the
 * following system property is set, and the value is 'true'.
 *
 * org.chefx3d.cache.checkForUpdates = true
 *
 * @author djoyce
 */
public class HttpURLConnection extends java.net.HttpURLConnection {

    /** Java http connection to delegate to. */
    private sun.net.www.protocol.http.HttpURLConnection httpConnection = null;

    /** ProtocolUtilities instance */
    private ProtocolUtils protocolUtils = new ProtocolUtils();

    /** Input stream from the wrapped connection or cache store */
    private InputStream iStream = null;

    /** Error stream from the wrapped connection or cache store */
    private InputStream eStream = null;

    /** Keeps track of exception when tried to open stream */
    private IOException connectionException = null;

    /** Used for headers, either from the wrapped connection or the client cache */
    private Map<String, String> headers = null;

    /** Used for headers, either from the wrapped connection or the client cache */
    private List<String> keys = null;

    /** ErrorReporter for logging */
    private ErrorReporter log = DefaultErrorReporter.getDefaultReporter();

    /** Whether the wrapped connection is connected */
    private boolean wrappedConnected = false;

    private void setupConnection() {
        //log.showLevel(ErrorReporter.DEBUG);
        log.debugReport("Setting up wrapped connectionn to " + url, null);
        try {

            if (!connected) {
                // If we are using GET, we need to see if it is in the cache first
                if ("GET".equals(httpConnection.getRequestMethod()) &&
                      protocolUtils.isCachableURL(url)) {
                    if (protocolUtils.doesAssetExist(url)) {
                        if (protocolUtils.checkFreshness()) {
                            httpConnection.setIfModifiedSince(
                                  protocolUtils.getLastModified(url));
                            log.debugReport("Checking freshness", null);
                        } else {
                            // Not checking freshness, simply return cached values.
                            log.debugReport("No freshness check, loading " +
                                  "from cache", null);
                            initializeFromCache();
                            return;
                        }
                    }
                    // Not in cache, having to download it.
                    log.debugReport("Possibly downloading content for caching!", null);
                    try {
                        httpConnection.connect();
                        wrappedConnected = true;
                        log.debugReport("Got response " +
                              httpConnection.getResponseCode(), null);
                        if (httpConnection.getResponseCode() == HTTP_OK) {
                            log.debugReport("Downloading content for caching!",
                                  null);
                            iStream = httpConnection.getInputStream();
                            processWrappedConnectionHeaders();
                            protocolUtils.storeHeadersInCache(url,
                                  keys, headers);
                            protocolUtils.storeAsset(url, iStream);
                            // Now that it is cached, use the cache to look up the values.
                            initializeFromCache();
                            return;
                        } else if (httpConnection.getResponseCode() !=
                              HTTP_NOT_MODIFIED) {
                            // Still try and set up headers, get inputstream?
                            processWrappedConnectionHeaders();
                            log.warningReport("URL " + url + " is not in cache, " +
                                  "but got response code " +
                                  httpConnection.getResponseCode() +
                                  " when trying to download!", null);
                            return;
                        }
                    } catch (Exception ex) {
                        log.warningReport("Caught exception while trying to " +
                              "download from host, reusing potentially stale " +
                              "value from cache for " + url, null);
                    }
                    initializeFromCache();
                } else {
                    // Do the usual...
                    initializeFromWrappedConnection();
                }
            }
        } catch (Exception ex) {
            log.errorReport("Encountered error while trying to initialize connection", ex);
            connectionException = new IOException("Error occured during connection setup");
        }
        log.debugReport("Finished setting up wrapped connectionn", null);
    }

    private void initializeFromWrappedConnection() {
        log.debugReport("Not caching simply using wrapped connection!", null);
        try {
            httpConnection.connect();
            wrappedConnected = true;
            processWrappedConnectionHeaders();
            responseCode = httpConnection.getResponseCode();
            responseMessage = httpConnection.getResponseMessage();
            connected = true;
            iStream = httpConnection.getInputStream();
        } catch (IOException ex) {
            log.errorReport("Got response code " + responseMessage, null);
            log.errorReport("Got response message " + responseCode, null);
            // Silencing stack trace, since 500 errors caused by soapfaults can cause these issues.
            //            log.errorReport("Error trying to use wrapped connection!", ex);
            log.errorReport("Error trying to use wrapped connection!", ex);

            connectionException = ex;
        }
        log.debugReport("Finished using wrapped connection!", null);
    }

    private void initializeFromCache() {
        log.debugReport("Initializing from cache!", null);
        try {
            if (protocolUtils.doesAssetExist(url)) {
                log.debugReport("Asset exists, configuring connnection using cached values!", null);
                headers = protocolUtils.getAllHeaders(url);
                keys = protocolUtils.getHeaderKeys(url);
                responseCode = HTTP_OK;
                responseMessage = "OK";
                iStream = protocolUtils.retrieveAsset(url);
                connected = true;
            } else {
                log.warningReport("Danger: Asset does not exist in cache! All subsequent operations will likely fail!", null);
                responseCode = HTTP_NOT_FOUND;
                responseMessage = "Not Found";
                headers = new HashMap<String, String>();
                keys = new ArrayList<String>();
                connectionException = new IOException("Asset not found in cache, but should have been there!");
                iStream = null;
                connected = true;
            }
        } catch (Exception ex) {
            log.errorReport("Error loading from asset from cache", ex);
            connectionException = new IOException("Error occured initializing from cache!");
        }
    }

    private void processWrappedConnectionHeaders() {
        //log.debugReport("Processing wrapped connection headers", null);
        Map<String, List<Object>> rawHeaders = httpConnection.getHeaderFields();
        headers = new HashMap<String, String>();
        keys = new ArrayList<String>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            keys.add(httpConnection.getHeaderFieldKey(i));
            //log.debugReport("Added Header " + httpConnection.getHeaderFieldKey(i), null);
            List<Object> rawHeaderValues = rawHeaders.get(httpConnection.getHeaderFieldKey(i));
            String headerValue = "";
            for (int j = 0; j < rawHeaderValues.size(); j++) {
                headerValue += rawHeaderValues.get(j);
                if (j < rawHeaderValues.size() - 1) {
                    headerValue += ", ";
                }
            }
            headers.put(httpConnection.getHeaderFieldKey(i), headerValue);
        }
        //log.debugReport("Finished processing wrapped connection headers", null);
    }

    private void teardownConnection() {
        iStream = null;
        keys = new ArrayList<String>();
        headers = new HashMap<String, String>();
        connected = false;
        connectionException = null;
    }

    public boolean getUseCaches() {
        return httpConnection.getUseCaches();
    }

    public URL getURL() {
        return httpConnection.getURL();
    }

    public long getLastModified() {
        return httpConnection.getLastModified();
    }

    public long getIfModifiedSince() {
        return httpConnection.getIfModifiedSince();
    }

    public int getHeaderFieldInt(String arg0, int arg1) {
        return httpConnection.getHeaderFieldInt(arg0, arg1);
    }

    public long getExpiration() {
        return httpConnection.getExpiration();
    }

    public boolean getDefaultUseCaches() {
        return httpConnection.getDefaultUseCaches();
    }

    public long getDate() {
        return httpConnection.getDate();
    }

    public Object getContent(Class[] arg0) throws IOException {
        return httpConnection.getContent(arg0);
    }

    public Object getContent() throws IOException {
        return httpConnection.getContent();
    }

    public boolean getAllowUserInteraction() {
        return httpConnection.getAllowUserInteraction();
    }

    public void setRequestMethod(String arg0) throws ProtocolException {
        httpConnection.setRequestMethod(arg0);
    }

    public void setInstanceFollowRedirects(boolean arg0) {
        httpConnection.setInstanceFollowRedirects(arg0);
    }

    public void setFixedLengthStreamingMode(int arg0) {
        httpConnection.setFixedLengthStreamingMode(arg0);
    }

    public void setChunkedStreamingMode(int arg0) {
        httpConnection.setChunkedStreamingMode(arg0);
    }

    public String getRequestMethod() {
        return httpConnection.getRequestMethod();
    }

    public Permission getPermission() throws IOException {
        return httpConnection.getPermission();
    }

    public boolean getInstanceFollowRedirects() {
        return httpConnection.getInstanceFollowRedirects();
    }

    public long getHeaderFieldDate(String arg0, long arg1) {
        return httpConnection.getHeaderFieldDate(arg0, arg1);
    }

    public boolean usingProxy() {
        return httpConnection.usingProxy();
    }

    public void setRequestProperty(String arg0, String arg1) {
        httpConnection.setRequestProperty(arg0, arg1);
    }

    public void setReadTimeout(int arg0) {
        httpConnection.setReadTimeout(arg0);
    }

    public void setConnectTimeout(int arg0) {
        httpConnection.setConnectTimeout(arg0);
    }

    public String getRequestProperty(String arg0) {
        return httpConnection.getRequestProperty(arg0);
    }

    public Map getRequestProperties() {
        return httpConnection.getRequestProperties();
    }

    public int getReadTimeout() {
        return httpConnection.getReadTimeout();
    }

    public int getConnectTimeout() {
        return httpConnection.getConnectTimeout();
    }

    public synchronized void doTunneling() throws IOException {
        httpConnection.doTunneling();
    }

    public synchronized void disconnect() {
        log.debugReport("disconnect() method called!", null);
        teardownConnection();
        if (wrappedConnected) {
            httpConnection.disconnect();
            wrappedConnected = false;
        }
    }

    public synchronized void connect() throws IOException {
        log.debugReport("connect() method called!", null);
        setupConnection();
        if (connectionException != null) {
            throw connectionException;
        }
    }

    public void addRequestProperty(String arg0, String arg1) {
        httpConnection.addRequestProperty(arg0, arg1);
    }

    protected HttpURLConnection(sun.net.www.protocol.http.HttpURLConnection httpConnection, URL url) {
        super(url);
        this.connected = false;
        this.httpConnection = httpConnection;
    }

    public synchronized OutputStream getOutputStream() throws IOException {
        return httpConnection.getOutputStream();
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        setupConnection();
        if (connectionException != null) {
            throw connectionException;
        }
        return iStream;
    }

    @Override
    public Map getHeaderFields() {
        setupConnection();
        return headers;
    }

    @Override
    public String getHeaderFieldKey(int arg0) {
        setupConnection();
        return keys.get(arg0);
    }

    @Override
    public String getHeaderField(int arg0) {
        setupConnection();
        return headers.get(keys.get(arg0));
    }

    @Override
    public String getHeaderField(String arg0) {
        setupConnection();
        return headers.get(arg0);
    }

    @Override
    public InputStream getErrorStream() {
        setupConnection();
        if (protocolUtils.doesAssetExist(url) && httpConnection.getRequestMethod().equals("GET")) {
            return null; // well it is in the cache, so obviously no errors occured
        }
        return httpConnection.getErrorStream();
    }

    @Override
    public String getContentEncoding() {
        return getHeaderField("Content-Encoding");
    }

    @Override
    public int getContentLength() {
        setupConnection();
        if (protocolUtils.doesAssetExist(url) && httpConnection.getRequestMethod().equals("GET")) {
            int len = -1;
            try {
                len = (int) protocolUtils.getAssetSize(url);
            } catch (Exception ex) {
            }
            return len;
        }
        return httpConnection.getContentLength();
    }

    @Override
    public String getContentType() {
        return getHeaderField("Content-Type");
    }

    @Override
    public boolean getDoInput() {
        return httpConnection.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
        return httpConnection.getDoOutput();
    }

    @Override
    public void setDoInput(boolean arg0) {
        httpConnection.setDoInput(arg0);
    }

    @Override
    public void setDoOutput(boolean arg0) {
        httpConnection.setDoOutput(arg0);
    }

    @Override
    public int getResponseCode() throws IOException {
        return responseCode;
    }

    @Override
    public String getResponseMessage() throws IOException {
        return responseMessage;
    }
}
