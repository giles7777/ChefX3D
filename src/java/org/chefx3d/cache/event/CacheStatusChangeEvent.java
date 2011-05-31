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
 *
 *
 * @author daniel
 */
public class CacheStatusChangeEvent {

    /**
     * For textures and icons
     */
    public static final int ASSET_TYPE_IMAGE = 1;

    /**
     * For X3D Models
     */
    public static final int ASSET_TYPE_MODEL = 2;

    public static final int ASSET_PROXIED = 3;

    public static final int ASSET_DOWNLOADED = 4;

    public static final int ASSET_REMOVED = 5;

    private int eventType = -1;

    private int assetType = -1;

    private String assetName = null;

    /**
     * This event is fired when the cache status of a asset has changed
     *
     * @param eventType
     * @param assetType The type of asset whose cache status has changed
     * @param assetName The name of the asset resource whose status has changed
     * @see CacheStatusListener
     */
    public CacheStatusChangeEvent(int eventType, int assetType, String assetName) {
        this.eventType = eventType;
        this.assetName = assetName;
        this.assetType = assetType;
    }

    /**
     *
     * @return the name of the asset this event applies to
     */
    public String getAssetName() {
        return assetName;
    }

    /**
     *
     * @return the type of the asset that this event applies to.
     */
    public int getAssetType() {
        return assetType;
    }

    public int getEventType() {
        return eventType;
    }
}
