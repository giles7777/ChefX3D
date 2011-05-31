/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009-2010
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

/**
 * Interface for various cache storage implementations
 *
 * @author Daniel Joyce
 * @version $Revision: 1.6 $
 */
public interface CacheStoreInterface {
    /**
     * A filter for removing server names from URLs.  Any url used for
     * a key will be filtered by this regex pattern.
     *
     * @param pattern The regex pattern
     */
    public void setURLFilter(String pattern);

    /**
     * Retrieves the asset pointed to by the key from the cache
     *
     * @param key
     * @return a outputstream to read the asset from. Caller is responsible for
     * closing stream.
     * @throws IOException
     */
    public InputStream retrieveAsset(String key) throws IOException;

    /**
     * Stores content represented by the submitted url into the cache and returns
     * a cache: url that can then be used to retrieve it.
     *
     * @param is a inputstream to read the asset from. Caller is responsible for
     * closing the InputStream
     * @param key the key to store it under
     * @throws IOException
     */
    public void storeAsset(String key, InputStream is) throws IOException;

    /**
     * Returns a outputstream that can be used to store content in the cache
     * at the location pointed to by the key.
     *
     * @param key the key to store it under
     * @return A Outputstream to write the asset to. Caller is responsible for
     * closing the outputstream.
     * @throws IOException
     */
    public OutputStream storeAsset(String key) throws IOException;

    /**
     * Deletes the asset represented by the key from the local cache
     *
     * @param key a key representing the asset to delete
     * @return true if the asset removal was successful, false if the asset doesn't exist
     * @throws IOException
     */
    public boolean removeAsset(String key) throws IOException;

    /**
     * Checks to see if the asset represented by the key exists in
     * the local cache.
     *
     * @param key
     * @return true if the asset pointed to by the key exists in the local cache
     */
    public boolean doesAssetExist(String key);

    /**
     * Returns the size of the asset in the cache.
     *
     * @param key
     * @return the asset size in bytes, otherwise -1 if it can't be determined
     */
    public long getAssetSize(String key);

    /**
     * Returns the last modification time in millis since epoch of the item
     * in the cache.
     *
     * @param key
     * @return the last modified date of the item in the cache, or 0L if
     * not possible to determine
     */
    public long getLastModified(String key);

    /**
     * Computes a storage location for a asset given the key
     *
     * @param key The key to store the asset under
     * @return A URI pointing to the file containing the asset
     * @throws MalformedURLException
     */
    public File getStorageFileForKey(String key) throws MalformedURLException;

    /**
     * Returns true if this implementation is capable of clearing the
     * cache it manages of all cached values
     *
     * @return true if the cache can be cleaned up, false otherwise
     */
    public boolean isCacheClearable();

    /**
     * Clears the cache if isCacheClearable returns true ( ie, the cache
     * impl supports clearing of all entries ).
     *
     * @return false if clearing the cache fails, or the cache does not support
     * dumping of all entries, true if it succeeds.
     */
    public boolean clearCache();

}
