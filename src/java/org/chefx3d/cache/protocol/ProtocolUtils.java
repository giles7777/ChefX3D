/*****************************************************************************
 *
 *                            (c) Yumetech 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 ****************************************************************************/
package org.chefx3d.cache.protocol;

// External imports
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.ietf.uri.URI;

// Local imports
import org.chefx3d.cache.HeaderUtils;
import org.chefx3d.cache.ClientCache;
import org.chefx3d.cache.ProxiedItemDownloadService;
import org.chefx3d.cache.ClientMimeType;

/**
 * ProtocolUtils is used by various cache-aware protocol handlers to make
 * talking to the cache easier.
 * 
 * The cache is a string-oriented data-store, and these utilities provide
 * a consistent way to retrieve headers and data from the cache.
 *
 * WARNING: URLS/URIs used for caching will have query strings removed
 * when building the key to store assets under! This means 2 Urls that differ
 * only in query parameters may collide if they really do represent different assets!
 *
 * RESTy URLs/URIs please!
 *
 * <b>"Freshness Checking"</b>
 *
 * Once content has been downloaded, updates will only be checked for if the
 * following system property is set, and the value is 'true'.
 *
 * org.chefx3d.cache.checkForUpdates = true
 *
 *
 * @author djoyce
 */
public class ProtocolUtils {

    /** Regex for determining if URI should be cached
     * TODO Should be configurable
     */
    private static String cacheableURIregex = ".*";

    /** Map of uri regexs to the proxies that should be used to replace them until they
     * can be downloaded
     */
    private static final Map<Pattern, String> patternToProxyKeyMap =
          new ConcurrentHashMap<Pattern, String>();

    /** Pattern to match URIs */
    private Pattern cacheableURIpattern = null;

    /** Local handle to client cache instance */
    private ClientCache clientCache = ClientCache.getInstance();

    /** Local handle to client cache instance */
    private ProxiedItemDownloadService downloadService =
          ProxiedItemDownloadService.getInstance();

    /** Local headerUtils instance */
    HeaderUtils headerUtils = new HeaderUtils();

    /** Format string for HTTP date formats */
    private static final String httpDateFormatString =
          "EEE, d MMM yyyy HH:mm:ss z";

    /** Error reporter */
    ErrorReporter log = DefaultErrorReporter.getDefaultReporter();

    public ProtocolUtils() {
        cacheableURIpattern = Pattern.compile(cacheableURIregex);
    }

    //--------------------------------------------------------------------------
    // Local Static Methods
    //--------------------------------------------------------------------------
    /**
     * Returns the currently configured regex string used to determine if
     * a uri/url should be cached.
     *
     * @return the regex string used to determine if a uri/url is cachable
     */
    public static String getCacheableURIregex() {
        return cacheableURIregex;
    }

    /**
     * Sets the URI/URL regex string used to determine if a given uri/url
     * should be cached.
     *
     * @param cacheableURIregex the regex string to use to determine if
     * a uri/url should be cached
     */
    public static void setCacheableURIregex(String cacheableURIregex) {
        ProtocolUtils.cacheableURIregex = cacheableURIregex;
    }

    // TODO Store settings in cache?
//    public static addNewProxyItem(String regex, InputStream is){
//        UUID uuid = UUID.randomUUID();
//        String key = "Protocol_Utils_"+uuid.toString();
//        Pattern p = Pattern.compile(regex);
//        patternToProxyKeyMap.put(p, key);
//
//    }

    //--------------------------------------------------------------------------
    // Local Methods
    //--------------------------------------------------------------------------
    private void initializeHeaders(String key) {
        headerUtils.initializeHeaders(key);
    }

    /**
     * Tries to look a header for the given URI in the cache
     *
     * @param uri to check to see if we have headers for
     * @param headerName the header to lookup
     * @return the value of the header, or null if not found
     */
    public String getHeaderFromCache(URI uri, String headerName) {
        return headerUtils.getHeaderFromCache(stripQueryParams(uri), headerName);
    }

    /**
     * Tries to look a header for the given URI in the cache
     *
     * @param url to check to see if we have headers for
     * @param headerName the header to lookup
     * @return the value of the header, or null if not found
     */
    public String getHeaderFromCache(URL url, String headerName) {
        return headerUtils.getHeaderFromCache(stripQueryParams(url), headerName);
    }

    /**
     * Tries to look a header for the given URI in the cache
     *
     * @param url to check to see if we have headers for
     * @param i the i'th header to lookup
     * @return the value of the i'th cached header, or null if not found
     */
    public String getHeaderFromCache(URL url, int i) {
        return headerUtils.getHeaderKeyFromCache(stripQueryParams(url), i);
    }

    /**
     * Tries to look up a header name for the given URI in the cache
     *
     * @param url to check to see if we have headers for
     * @param i the i'th header name to lookup
     * @return the name of the i'th cached header, or null if not found
     */
    public String getHeaderKeyFromCache(URL url, int i) {
        return headerUtils.getHeaderFromCache(stripQueryParams(url), i);
    }

    /**
     * Return a map of header key/value pairs for a
     * URI, otherwise returns null if the uri is not cached.
     *
     * @param uri to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public Map<String, String> getAllHeaders(URI uri) {
        return headerUtils.getAllHeaders(stripQueryParams(uri));
    }

    /**
     * Return a map of header key/value pairs for a
     * URL, otherwise returns null if the uri is not cached.
     *
     * @param url to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public Map<String, String> getAllHeaders(URL url) {
        return headerUtils.getAllHeaders(stripQueryParams(url));
    }

    /**
     * Return a map of header key/value pairs for a
     * URI, otherwise returns null if the uri is not cached.
     *
     * @param uri to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public List<String> getHeaderKeys(URI uri) {
        return headerUtils.getHeaderKeys(stripQueryParams(uri));
    }

    /**
     * Return a map of header key/value pairs for a
     * URL, otherwise returns null if the uri is not cached.
     *
     * @param url to check to see if we have headers for
     * @return a map of headers as key/value pairs
     */
    public List<String> getHeaderKeys(URL url) {
        return headerUtils.getHeaderKeys(stripQueryParams(url));
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
    public void storeHeadersInCache(URI uri, List<String> keys, Map<String, String> headers)
          throws IOException {
        headerUtils.storeHeadersInCache(stripQueryParams(uri), keys, headers);
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
    public void storeHeadersInCache(URL url, List<String> keys, Map<String, String> headers)
          throws IOException {
        headerUtils.storeHeadersInCache(stripQueryParams(url), keys, headers);
    }

    /**
     * Tries to figure out the content type from the URI
     *
     * @param uri
     * @return
     */
    public String getContentTypeFromURI(URI uri) {
        String u = stripQueryParams(uri).toExternalForm().toLowerCase();
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

    /**
     * Is the URI one we want to cache?
     *
     * @param uri
     * @return true if the URI has been registered as cachable, false otherwise
     */
    public boolean isCachableURI(URI uri) {
        URI foo = stripQueryParams(uri);
        if (foo == null) {
            return false;
        } else {
            return cacheableURIpattern.matcher(foo.toExternalForm()).find();
        }
    }

    /**
     * Is the URL one we want to cache?
     *
     * @param url
     * @return true if the URL has been registered as cachable, false otherwise
     */
    public boolean isCachableURL(URL url) {
        return cacheableURIpattern.matcher(stripQueryParams(url).toExternalForm()).
              find();
    }

    /**
     * Returns a proxy key for the given itemkey, if the itemKey has
     * been configured as Proxyable
     *
     * @param itemKey
     * @return a String re
     */
    private String getItemProxy(String itemKey) {
        String proxyKey = null;
        for (Pattern p : patternToProxyKeyMap.keySet()) {
            if (p.matcher(itemKey).find()) {
                proxyKey = patternToProxyKeyMap.get(p);
            }
        }
        return proxyKey;
    }

    /**
     * Returns a stream that can be used to read a asset for the given URL
     *
     * Caller is responsiblef for closing inputstream
     *
     * @param url the url the cached asset is stored under.
     * @return a inputstream to read the cached asset from
     * @throws java.io.IOException
     */
    public InputStream retrieveAsset(URL url) throws IOException {
        URL url2 = stripQueryParams(url);
        if (!doesAssetExist(url2)) {
            String proxyKey = getItemProxy(url2.toExternalForm());
            if (proxyKey != null) {
                downloadService.scheduleProxiedItemForDownload(url2);
                return clientCache.retrieveAsset(proxyKey);
            }
        }
        return clientCache.retrieveAsset(url2.toExternalForm());
    }

    /**
     * Returns a stream that can be used to read a asset for the given URI
     *
     * Caller is responsiblef for closing inputstream
     *
     * @param uri the uri the cached asset is stored under.
     * @return a inputstream to read the cached asset from
     * @throws java.io.IOException
     */
    public InputStream retrieveAsset(URI uri) throws IOException {
        URI uri2 = stripQueryParams(uri);
        if (!doesAssetExist(uri2)) {
            String proxyKey = getItemProxy(uri2.toExternalForm());
            if (proxyKey != null) {
                downloadService.scheduleProxiedItemForDownload(uri2);
                return clientCache.retrieveAsset(proxyKey);
            }
        }
        return clientCache.retrieveAsset(uri2.toExternalForm());
    }

    /**
     * Store a asset using the given url as a key
     *
     * @param url
     * @param is
     * @throws java.io.IOException
     */
    public void storeAsset(URL url, InputStream is) throws IOException {
        clientCache.storeAsset(stripQueryParams(url).toExternalForm(), is);
    }

    /**
     * Store a asset using the given uri as a key
     *
     * @param uri
     * @param is
     * @throws java.io.IOException
     */
    public void storeAsset(URI uri, InputStream is) throws IOException {
        clientCache.storeAsset(stripQueryParams(uri).toExternalForm(), is);
    }

    /**
     * Store a asset using the given url as a key
     *
     * @param url the url to store the asset under
     * @return a outputstream to write the asset to for storage, caller is responsible for closing!
     * @throws java.io.IOException
     */
    public OutputStream storeAsset(URL url) throws IOException {
        return clientCache.storeAsset(stripQueryParams(url).toExternalForm());
    }

    /**
     * Store a asset using the given uri as a key
     *
     * @param uri the uri to store the asset under
     * @return a outputstream to write the asset to for storage, caller is responsible for closing!
     * @throws java.io.IOException
     */
    public OutputStream storeAsset(URI uri) throws IOException {
        return clientCache.storeAsset(stripQueryParams(uri).toExternalForm());
    }

    /**
     * Does a local copy of the asset exist for the given url
     *
     * @param url the url the asset might be stored under
     * @return true if a local copy exists
     */
    public boolean doesAssetExist(URL url) {
        return clientCache.doesAssetExist(stripQueryParams(url).toExternalForm());
    }

    /**
     * Does a local copy of the asset exist for the given uri
     *
     * @param uri the uri the asset might be stored under
     * @return true if a local copy exists
     */
    public boolean doesAssetExist(URI uri) {
        URI foo = stripQueryParams(uri);
        if (foo == null) {
            return false;
        } else {
            return clientCache.doesAssetExist(foo.toExternalForm());
        }
    }

    /**
     * Returns the on-disk size for the local asset pointed to by url
     * @param url
     * @return the asset size in bytes or -1 if it can't be determined
     */
    public long getAssetSize(URL url) {
        return clientCache.getAssetSize(stripQueryParams(url).toExternalForm());
    }

    /**
     * Returns the on-disk size for the local asset pointed to by uri
     * @param uri
     * @return the asset size in bytes or -1 if it can't be determined
     */
    public long getAssetSize(URI uri) {
        return clientCache.getAssetSize(stripQueryParams(uri).toExternalForm());
    }

//    public boolean checkCacheFreshness(URI uri) {
//        int responseCode = -1;
//        boolean fresh = false;
//        try {
//            HttpURLConnection connection = new HttpURLConnection(new URL(uri.toExternalForm()), null);
//            long freshnessTime = clientCache.getLastModified(stripQueryParams(uri).toExternalForm());
//            SimpleDateFormat sdf = new SimpleDateFormat(httpDateFormat);
//            Date d = new Date(freshnessTime);
//            connection.addRequestProperty("If-Modified-Since", sdf.format(d));
//            responseCode = connection.getResponseCode();
//        } catch (Exception ex) {
//            log.errorReport("Error testing " + uri + " for freshness!", ex);
//        }
//        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
//            fresh = true;
//        }
//        return fresh;
//    }
    /**
     * Returns a copy of the URI with the query string removed
     *
     * @param uri the uri to strip the query string from.
     * @return a new uri with the query string removed
     */
    public URI stripQueryParams(URI uri) {
        String uString = uri.toExternalForm();
        URI newURI = null;
        int qidx = uString.indexOf("?");
        if (qidx < 0) {
            qidx = uString.length();
        }
        String newUrlString = uString.substring(0, qidx);
        try {
            newURI = new org.ietf.uri.URL(newUrlString);
        } catch (Exception ex) {
            log.errorReport("Error stripping query parameters from " + uri, ex);
        }
        return newURI;
    }

    /**
     * Returns a new URL with the query string removed
     *
     * @param url the url to strop
     * @return a new url with the query string removed
     */
    public URL stripQueryParams(URL url) {
        String uString = url.toExternalForm();
        URL newURL = null;
        int qidx = uString.indexOf("?");
        if (qidx < 0) {
            qidx = uString.length();
        }
        String newUrlString = uString.substring(0, qidx);
        try {
            newURL = new URL(newUrlString);
        } catch (Exception ex) {
            log.errorReport("Error stripping query parameters from " + url, ex);
        }
        return newURL;
    }

    /**
     * If true, means clients updating the cache should check for freshness.
     *
     * @see ClientCache
     *
     * @return whether checking for cache updates is enabled/disabled
     */
    public boolean checkFreshness() {
        return clientCache.isCheckFreshness();
    }

    /**
     * Gets the last modified time ( in millis since epoch ) for the asset
     * pointed to by url.
     * @param url
     * @return
     */
    public long getLastModified(URL url) {
        return clientCache.getLastModified(stripQueryParams(url).toExternalForm());
    }

    /**
     * Gets the last modified time ( in millis since epoch ) for the asset
     * pointed to by uri.
     * @param uri
     * @return
     */
    public long getLastModified(URI uri) {
        return clientCache.getLastModified(stripQueryParams(uri).toExternalForm());
    }

    /**
     * Gets the last modified time formatted appropriately for use
     * by the If-Modified-Since header
     *
     * @param url
     * @return A String suitable for use in the If-Modified-Since header
     */
    public String getLastModifiedString(URL url) {
        long timeStamp = clientCache.getLastModified(
              stripQueryParams(url).toExternalForm());
        SimpleDateFormat httpDateFormat = new SimpleDateFormat(
              httpDateFormatString, Locale.US);
        Date d = new Date();
        d.setTime(timeStamp);
        return httpDateFormat.format(d);
    }

    /**
     * Gets the last modified time formatted appropriately for use
     * by the If-Modified-Since header
     * 
     * @param uri
     * @return A String suitable for use in the If-Modified-Since header
     */
    public String getLastModifiedString(URI uri) {
        long timeStamp = clientCache.getLastModified(
              stripQueryParams(uri).toExternalForm());
        SimpleDateFormat httpDateFormat = new SimpleDateFormat(
              httpDateFormatString, Locale.US);
        Date d = new Date();
        d.setTime(timeStamp);
        return httpDateFormat.format(d);
    }
}
