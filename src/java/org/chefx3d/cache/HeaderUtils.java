/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chefx3d.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.chefx3d.cache.ClientMimeType;
import org.ietf.uri.URI;

/**
 * Probably not the best place to put this, but getting around make build limitations.
 *
 * The cache is a string-oriented data-store, and these utilities provide
 * a consistent way to retrieve headers and data from the cache.
 *
 * @author djoyce
 */
public class HeaderUtils{

    /** Prefix prepended to URIs for using as key to store headers in the cache */
    public static final String HEADERS_KEY = "header:";

    /** Local holder of headers when loaded from cache */
    private Map<String, String> cachedHeaders = null;

    private List<String> cachedHeaderKeys = null;

    /** Pattern to match URIs */
    private Pattern cacheableURIpattern = null;

    /** Local handle to client cache instance */
    private ClientCache clientCache = ClientCache.getInstance();

    /** String to seperate headers in the saved files */
    private static final String headerSep = "@@@";

    //--------------------------------------------------------------------------
    // Local Methods
    //--------------------------------------------------------------------------
    public void initializeHeaders(String key) {
        cachedHeaders = new HashMap<String, String>();
        cachedHeaderKeys = new ArrayList<String>();
        InputStream is = null;
        BufferedReader br = null;
        try {
            is = ClientCache.getInstance().retrieveAsset(key);
            br = new BufferedReader(new InputStreamReader(is));
            String headerLine = br.readLine();
            String valueLine = br.readLine();
            String[] headerKs = headerLine.split(headerSep);
            String[] headerVs = valueLine.split(headerSep);
            for (int i = 0; i < headerKs.length; i++) {
                cachedHeaderKeys.add(headerKs[i]);
                cachedHeaders.put(headerKs[i], headerVs[i]);
            }
        } catch (IOException ioex) {
            // TODO Logging
        } finally {
            try {
                br.close();
            } catch (Exception ex) {
                // noop
            }
        }
    }

    /**
     * Tries to look a header for the given URI in the cache
     *
     * @param uri to check to see if we have headers for
     * @param headerName the header to lookup
     * @return the value of the header, or null if not found
     */
    public String getHeaderFromCache(URI uri, String headerName) {
        if (cachedHeaders == null) {
            initializeHeaders(keyFromURI(uri));
        }
        return cachedHeaders.get(headerName);
    }

    /**
     * Tries to look a header for the given URI in the cache
     *
     * @param url to check to see if we have headers for
     * @param headerName the header to lookup
     * @return the value of the header, or null if not found
     */
    public String getHeaderFromCache(URL url, String headerName) {
        if (cachedHeaders == null) {
            initializeHeaders(keyFromURL(url));
        }
        return cachedHeaders.get(headerName);
    }

    /**
     * Tries to look a header for the given URI in the cache
     *
     * @param url to check to see if we have headers for
     * @param i the i'th header to lookup
     * @return the value of the i'th cached header, or null if not found
     */
    public String getHeaderFromCache(URL url, int i) {
        if (cachedHeaders == null) {
            initializeHeaders(keyFromURL(url));
        }
        try {
            return cachedHeaders.get(cachedHeaderKeys.get(i));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Tries to look up a header name for the given URI in the cache
     *
     * @param url to check to see if we have headers for
     * @param i the i'th header name to lookup
     * @return the name of the i'th cached header, or null if not found
     */
    public String getHeaderKeyFromCache(URL url, int i) {
        if (cachedHeaders == null) {
            initializeHeaders(keyFromURL(url));
        }
        try {
            return cachedHeaders.get(cachedHeaderKeys.get(i));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Return a map of header key/value pairs for a
     * URI, otherwise returns null if the uri is not cached.
     *
     * @param uri to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public Map<String, String> getAllHeaders(URI uri) {
        if (cachedHeaders == null) {
            initializeHeaders(keyFromURI(uri));
        }
        return Collections.unmodifiableMap(cachedHeaders);
    }

    /**
     * Return a map of header key/value pairs for a
     * URL, otherwise returns null if the uri is not cached.
     *
     * @param url to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public Map<String, String> getAllHeaders(URL url) {
        if (cachedHeaders == null) {
            initializeHeaders(keyFromURL(url));
        }
        return Collections.unmodifiableMap(cachedHeaders);
    }

    /**
     * Return a map of header key/value pairs for a
     * URL, otherwise returns null if the uri is not cached.
     *
     * @param url to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public List<String> getHeaderKeys(URL url) {
        if (cachedHeaderKeys == null) {
            initializeHeaders(keyFromURL(url));
        }
        return Collections.unmodifiableList(cachedHeaderKeys);
    }

    /**
     * Return a map of header key/value pairs for a
     * URL, otherwise returns null if the uri is not cached.
     *
     * @param uri to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public List<String> getHeaderKeys(URI uri) {
        if (cachedHeaderKeys == null) {
            initializeHeaders(keyFromURI(uri));
        }
        return Collections.unmodifiableList(cachedHeaderKeys);
    }

    private void storeHeadersInCache(String storageKey, List<String> keys, Map<String, String> headers) throws IOException {
        OutputStream os = null;
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        cachedHeaderKeys = new ArrayList<String>();
        cachedHeaders = new HashMap<String, String>();
        try {
            os = ClientCache.getInstance().storeAsset(storageKey);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
            int i = 0;
            int size = keys.size();
            for (String key : keys) {
                cachedHeaders.put(key, headers.get(key));
                cachedHeaderKeys.add(key);
                headerBuilder.append(key);
                valueBuilder.append(headers.get(key));
                if (i < size - 1) {
                    headerBuilder.append(headerSep);
                    valueBuilder.append(headerSep);
                }
                i++;
            }
            pw.println(headerBuilder.toString());
            //System.err.println(headerBuilder.toString());
            pw.println(valueBuilder.toString());
            //System.err.println(valueBuilder.toString());
            pw.println(storageKey);
            pw.println();
            pw.flush();
            pw.close();
        } catch (IOException ioex) {
            // TODO Logging
            ioex.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (Exception ex) {
                // noop
            }
        }
    }

    /**
     * Stores headers in cache, using the order specified in keys. The headernames
     * in keys must match the names found in headers
     *
     * @param uri the URI to store the headers for
     * @param keys a List containing the header names. Headers will be stored in this order
     * @param headers a Map of header names and values.
     * @throws java.io.IOException
     */
    public void storeHeadersInCache(URI uri, List<String> keys, Map<String, String> headers) throws IOException {
        storeHeadersInCache(keyFromURI(uri), keys, headers);
    }

    /**
     * Stores headers in cache, using the order specified in keys. The headernames
     * in keys must match the names found in headers
     *
     * @param url, the URL to store the headers for
     * @param keys a List containing the header names. Headers will be stored in this order
     * @param headers a Map of header names and values.
     * @throws java.io.IOException
     */
    public void storeHeadersInCache(URL url, List<String> keys, Map<String, String> headers) throws IOException {
        storeHeadersInCache(keyFromURL(url), keys, headers);
    }

    public String getContentTypeFromURI(URI uri) {
        String u = uri.toExternalForm().toLowerCase();
        String mtString = null;
        if (u.contains("catalog")) {
            mtString = ClientMimeType.CATALOG.getMimeType();
        } else if (u.contains("toolgroup")) {
            mtString = ClientMimeType.TOOL_GROUP.getMimeType();
        } else if (u.contains("tool")) {
            mtString = ClientMimeType.TOOL.getMimeType();
        }
        return mtString;
    }

    private String keyFromURI(URI uri) {
        return HEADERS_KEY + uri.toExternalForm();
    }

    private String keyFromURL(URL url) {
        return HEADERS_KEY + url.toExternalForm();
    }

    public static void main(String[] args) {
        String s = "a@@@b@@@c@@@d";
        String[] chunks = s.split("@@@");
        for(String chunk : chunks){
            System.out.println(chunk);
        }
    }
}
