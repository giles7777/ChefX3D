/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chefx3d.cache;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.chefx3d.cache.HeaderUtils;
import sun.net.www.protocol.http.HttpURLConnection;

/**
 * This runnable downloads a currently proxied asset, and stores it and its
 * headers in the client cache.
 *
 * @author djoyce
 */
class ProxiedItemDownloaderRunnable implements Runnable {

    /** uri string to download */
    String uriString = null;

    /** Client Cache */
    ClientCache clientCache = ClientCache.getInstance();

    /** Header processing and storage utilities */
    HeaderUtils headerUtils = new HeaderUtils();

    public ProxiedItemDownloaderRunnable(String uriString) {
        this.uriString = uriString;
    }

    /**
     * Starts the runnable to download the asset
     *
     * TODO Support proxy settings.
     * TODO Support cache headers.
     */
    public void run() {
        // Always catch all exceptions/errors in the runnable, else the
        // running thread will get stuck, potentially jamming the threadpool.
        try {
            if (!clientCache.doesAssetExist(uriString)) {
                HttpURLConnection httpConnection = new HttpURLConnection(new URL(uriString), null);
                InputStream is = httpConnection.getInputStream();
                clientCache.storeAsset(uriString, is);
                storeHeaders(httpConnection);
                is.close();
            }
        } catch (Exception ex) {
            // TODO logging
        }
    }

    private void storeHeaders(HttpURLConnection httpConnection) {
        Map<String, List<Object>> rawHeaders = httpConnection.getHeaderFields();
        Map<String, String> headers = new HashMap<String, String>();
        List<String> keys = new ArrayList<String>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            keys.add(httpConnection.getHeaderFieldKey(i));
            //System.out.println("Added Header "+httpConnection.getHeaderFieldKey(i));
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
        try {
            headerUtils.storeHeadersInCache(httpConnection.getURL(), keys, headers);
        } catch (Exception ex) {
            //System.err.println("Caught exception!");
            //ex.printStackTrace();
        }
    }
}
