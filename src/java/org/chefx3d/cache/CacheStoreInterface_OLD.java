/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package org.chefx3d.cache;

// External imports
import java.io.IOException;
import java.net.MalformedURLException;
import org.ietf.uri.ResourceConnection;
import org.ietf.uri.URL;

/**
 * Interface for various cache storage implementations
 *
 * @author Daniel Joyce
 * @version $Revision: 1.2 $
 */
public interface CacheStoreInterface_OLD {

    /**
     * Stores content represented by the submitted url into the cache and returns
     * a cache: url that can then be used to retrieve it.
     *
     * @param url remote url to store
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     */
    public void storeAsset(URL url) throws MalformedURLException, IOException;

    /**
     * Deletes the asset represented by the submitted url from the local cache
     *
     * @param url, a cache url representing the cache item to remove
     * @return true if the asset removal was successful, false if the asset doesn't exist
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     */
    public boolean removeAsset(URL url) throws MalformedURLException, IOException;

    /**
     * Checks to see if the asset represented by the submitted url exists in
     * the local cache.
     *
     * @param url
     * @return true if the asset pointed to by the url exists in the local cache
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     */
    public boolean doesAssetExist(URL url) throws MalformedURLException, IOException;

//    /**
//     * Returns a url that can be used to read the asset pointed to by the url.
//     *
//     * @param url
//     * @return
//     * @throws java.net.MalformedURLException
//     * @throws java.io.IOException
//     */
//    public URL retrieveAsset(URL url) throws MalformedURLException, IOException;

    /**
     * Returns a resourceconnection to read the submitted URL.
     *
     * @param url
     * @return
     * @throws java.io.IOException
     * @see ResourceConnection
     */
    public ResourceConnection getConnection(URL url) throws IOException;
}
