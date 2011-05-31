/*****************************************************************************
 *
 *
 ****************************************************************************/
package org.chefx3d.cache.protocol.jar;

import java.io.*;

import java.util.jar.JarFile;
import java.net.UnknownServiceException;
import java.net.MalformedURLException;

import org.chefx3d.cache.protocol.ProtocolUtils;
import org.ietf.uri.*;

/**
 * A cache-aware wrapper around vlc.net.protocol.jar.JarConnection
 * @author  Daniel Joyce
 */
public class JarConnection extends vlc.net.protocol.jar.JarConnection {

    URI location = null;

    String path = null;

    ProtocolUtils pu = new ProtocolUtils();

    protected JarConnection(URI location, String path)
            throws MalformedURLException {
        super(location, path);
        this.location = location;
        this.path = path;
    }

    @Override
    public void connect() throws IOException {
        super.connect();
    }

    @Override
    public String getContentType() {
        return super.getContentType();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        long jarLastModified = 0;
        long cacheLastModified = 0;
        boolean freshenCache = false;
        InputStream is = null;
        if (pu.doesAssetExist(uri) && !pu.checkFreshness()) {
            return pu.retrieveAsset(uri);
        } else {
            if (pu.checkFreshness()) {
                if (!connected) {
                    connect();
                }
                jarLastModified = getLastModified();
                cacheLastModified = pu.getLastModified(uri);
                freshenCache = jarLastModified > cacheLastModified;
            }
            if (freshenCache || !pu.doesAssetExist(uri)) {
                is = super.getInputStream();
                if (pu.isCachableURI(uri)) {
                    pu.storeAsset(uri, is);
                    is.close();
                    is = pu.retrieveAsset(uri);
                }
            }
            return pu.retrieveAsset(uri);
        }
    }
}
