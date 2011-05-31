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

// External Imports
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

/**
 * The client cache implements a simple storage solution for caching items
 * via keys associated with some sort of storage location. Internally, this
 * implementation uses a instance of CacheDiskStore to store cached items
 * on disk
 *
 * TODO Ensure that storage/retrieval methods are thread safe
 *
 * @author Daniel Joyce
 * @version $Revision: 1.13 $
 * @see CacheDiskStore
 */
public class ClientCache implements CacheStoreInterface {

    /** Use on-disk store */
    protected CacheStoreInterface cacheStore = new CacheDiskStore();

    /** Whether or not freshness of content should be checked by all storers/retrievers
     * of cached information
     */
    private boolean checkFreshness = false;

    private ClientCache() {
        // This is a singleton
    }

    //-----------------------------------------------------------------------
    // Methods inherited from CacheStoreInterface
    //-----------------------------------------------------------------------
    public InputStream retrieveAsset(String key) throws IOException {
        return cacheStore.retrieveAsset(key);
    }

    public void storeAsset(String key, InputStream is) throws IOException {
        cacheStore.storeAsset(key, is);
    }

    public OutputStream storeAsset(String key) throws IOException {
        return cacheStore.storeAsset(key);
    }

    public boolean removeAsset(String key) throws IOException {
        return cacheStore.removeAsset(key);
    }

    public boolean doesAssetExist(String key) {
        return cacheStore.doesAssetExist(key);
    }

    public long getAssetSize(String key) {
        return cacheStore.getAssetSize(key);
    }

    public File getStorageFileForKey(String key) throws MalformedURLException {
        return cacheStore.getStorageFileForKey(key);
    }

    public long getLastModified(String key) {
        return cacheStore.getLastModified(key);
    }

    public boolean isCheckFreshness() {
        return checkFreshness;
    }

    public void setCheckFreshness(boolean checkFreshness) {
        this.checkFreshness = checkFreshness;
    }

    public boolean isCacheClearable(){
        return cacheStore.isCacheClearable();
    }

    public boolean clearCache(){
        return cacheStore.clearCache();
    }

    /**
     * A filter for removing server names from URLs.  Any url used for
     * a key will be filtered by this regex pattern.
     *
     * @param pattern The regex pattern
     */
    public void setURLFilter(String pattern) {
        cacheStore.setURLFilter(pattern);
    }

    //---------------------------------------------------------------------
    // Methods to manage singleton instance
    //---------------------------------------------------------------------
    public static ClientCache getInstance() {
        // Thread safe and lazy way to create a singleton
        return ClientCacheHolder.INSTANCE;
    }

    private static class ClientCacheHolder {

        private static final ClientCache INSTANCE = new ClientCache();
    }

    //---------------------------------------------------------------------
    // Methods inherited from Object
    //---------------------------------------------------------------------
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("This is a singleton, clone is not supported!");
    }

    //---------------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------------

    /**
     * Get the root cache file path
     *
     * @return The path to the root of the cache
     */
    public String getStorageRoot() {
        return ((CacheDiskStore)cacheStore).getStorageRoot();
    }

    /**
     * Get the root cache file path
     *
     * @return The path to the root of the cache
     */
    public void setStorageRoot(String rootPath) {
        ((CacheDiskStore)cacheStore).setStorageRoot(rootPath);
    }

    /**
     * Initialize the cache with the current cahce values
     *
     * @param rootType
     *  0: use user location for cache
     *  1: use public location for cache
     *  2: use already defined storage root
     */
    public void initialize(int rootType) {
        ((CacheDiskStore)cacheStore).initialize(rootType);
    }

}
