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

package org.chefx3d.cache.event;

/**
 * Objects interested in cache status events should implement this
 * interface.
 *
 * @author daniel
 */
public interface CacheStatusListener {
    /**
     * A asset was removed from the cache
     *
     * @param evt A Cache Status Event
     */
    public void assetRemoved(CacheStatusChangeEvent evt);

    /**
     * A asset was successfully downloaded and added to the
     * cache
     *
     * @param evt A Cache Status Event
     */
    public void assetDownloaded(CacheStatusChangeEvent evt);

    /**
     * A asset is currently being proxied by the cache
     *
     * @param evt A Cache Status Event
     */
    public void assetProxied(CacheStatusChangeEvent evt);
}
