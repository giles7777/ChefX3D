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
 * A adapter class for the CacheStatusListenerInterface
 *
 * @author djoyce
 * @see CacheStatusListener
 */
public class CacheStatusAdapter implements CacheStatusListener{

    public void assetRemoved(CacheStatusChangeEvent evt) {
    }

    public void assetDownloaded(CacheStatusChangeEvent evt) {
    }

    public void assetProxied(CacheStatusChangeEvent evt) {
    }
}
