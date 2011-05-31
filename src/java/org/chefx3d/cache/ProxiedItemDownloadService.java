/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chefx3d.cache;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.SwingUtilities;
import org.chefx3d.cache.event.CacheStatusChangeEvent;
import org.chefx3d.cache.event.CacheStatusListener;
import org.ietf.uri.URI;

/**
 *
 * @author djoyce
 */
public class ProxiedItemDownloadService {

    private enum MessageType {

        PROXIED, DOWNLOADED, REMOVED
    };

    /**
     * Executor for downloading models/icons currently otherwise proxied
     */
    Executor proxiedItemDownloader = Executors.newFixedThreadPool(1);

    //BlockingQueue<ProxiedItemDownloaderRunnable> proxiedItemsToDownload = new LinkedBlockingQueue<ProxiedItemDownloaderRunnable>(50);

    /** Handle to the client cache */
    ClientCache clientCache = ClientCache.getInstance();

    /** Set for storing listeners */
    private final Map<String, Set<Reference<CacheStatusListener>>> cacheStatusListeners = Collections.synchronizedMap(new HashMap<String, Set<Reference<CacheStatusListener>>>());

    /** This is a singleton */
    private ProxiedItemDownloadService() {
    }

    //---------------------------------------------------------------------
    // Local Methods
    //---------------------------------------------------------------------

    public void scheduleProxiedItemForDownload(URL url){
        proxiedItemDownloader.execute(new ProxiedItemDownloaderRunnable(url.toExternalForm()));
    }

    public void scheduleProxiedItemForDownload(URI uri){
        proxiedItemDownloader.execute(new ProxiedItemDownloaderRunnable(uri.toExternalForm()));
    }

    /**
     * Registers a CacheStatusChangeListener with the download service
     * so it is notified of cache status change events.
     *
     * @param cacheStatusListener a listener to register
     * @param itemURL, the url of the item you are interested in getting status for
     */
    public void registerCacheStatusChangeListener(CacheStatusListener cacheStatusListener, URL itemURL) {
        synchronized (cacheStatusListeners) {
            String key = itemURL.toExternalForm();
            Set listeners = cacheStatusListeners.get(key);
            if (listeners == null) {
                listeners = new HashSet<CacheStatusListener>();
                cacheStatusListeners.put(key, listeners);
            }
            listeners.add(new WeakReference<CacheStatusListener>(cacheStatusListener));
        }
    }

    /**
     * Registers a CacheStatusChangeListener with the download service
     * so it is notified of cache status change events.
     *
     * @param cacheStatusListener a listener to register
     * @param itemURI, the uri of the item you are interested in getting status for
     */
    public void registerCacheStatusChangeListener(CacheStatusListener cacheStatusListener, URI itemURI) {
        synchronized (cacheStatusListeners) {
            String key = itemURI.toExternalForm();
            Set listeners = cacheStatusListeners.get(key);
            if (listeners == null) {
                listeners = new HashSet<CacheStatusListener>();
                cacheStatusListeners.put(key, listeners);
            }
            listeners.add(new WeakReference<CacheStatusListener>(cacheStatusListener));
        }
    }

    /**
     * Fires Cache Status Change Events to the registered listeners.
     */
    private void fireCacheStatusEvents(List<CacheStatusChangeEvent> cacheStatusChangeEvents) {
        // TODO split out and break up if it causes too much of a delay
        SwingUtilities.invokeLater(new FireStatusChangeEventsRunnable(cacheStatusChangeEvents));
    }

    //---------------------------------------------------------------------
    // Runnable Utility class for firing events
    //---------------------------------------------------------------------
    class FireStatusChangeEventsRunnable implements Runnable {

        private List<CacheStatusChangeEvent> cacheStatusEvents = null;

        public FireStatusChangeEventsRunnable(List<CacheStatusChangeEvent> cacheStatusEvents) {
            this.cacheStatusEvents = cacheStatusEvents;
        }

        public void run() {
            for (CacheStatusChangeEvent csce : cacheStatusEvents) {
                Set<Reference<CacheStatusListener>> deadListeners = new HashSet<Reference<CacheStatusListener>>();
                String target = csce.getAssetName();
                // First fire the specific listeners
                synchronized (cacheStatusListeners) {
                    Set<Reference<CacheStatusListener>> listeners = cacheStatusListeners.get(target);
                    for (Reference<CacheStatusListener> listenerRef : listeners) {
                        CacheStatusListener csl = listenerRef.get();
                        if (csl == null) {
                            deadListeners.add(listenerRef);
                        } else {
                            switch (csce.getEventType()) {
                                case CacheStatusChangeEvent.ASSET_PROXIED:
                                    csl.assetProxied(csce);
                                    break;
                                case CacheStatusChangeEvent.ASSET_DOWNLOADED:
                                    csl.assetDownloaded(csce);
                                    break;
                                case CacheStatusChangeEvent.ASSET_REMOVED:
                                    csl.assetRemoved(csce);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    listeners.remove(deadListeners);
                    // Then fire the events to listeners interested in getting all
                    // cache change events
                    deadListeners = new HashSet<Reference<CacheStatusListener>>();
                    listeners = cacheStatusListeners.get(null);
                    for (Reference<CacheStatusListener> listenerRef : listeners) {
                        CacheStatusListener csl = listenerRef.get();
                        if (csl == null) {
                            deadListeners.add(listenerRef);
                        } else {
                            switch (csce.getEventType()) {
                                case CacheStatusChangeEvent.ASSET_PROXIED:
                                    csl.assetProxied(csce);
                                    break;
                                case CacheStatusChangeEvent.ASSET_DOWNLOADED:
                                    csl.assetDownloaded(csce);
                                    break;
                                case CacheStatusChangeEvent.ASSET_REMOVED:
                                    csl.assetRemoved(csce);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    listeners.remove(deadListeners);
                }
            }
        }
    }

//    class EventFirer {
//
//        private CacheStatusChangeEvent event;
//
//        private CacheStatusListener listener;
//
//        private MessageType mt;
//
//        private EventFirer(MessageType mt, CacheStatusListener listener, CacheStatusChangeEvent event) {
//            this.event = event;
//            this.listener = listener;
//            this.mt = mt;
//        }
//
//        private void fireEvent() {
//            switch (mt) {
//                case DOWNLOADED:
//                    listener.assetDownloaded(event);
//                    break;
//                case PROXIED:
//                    listener.assetProxied(event);
//                    break;
//                case REMOVED:
//                    listener.assetRemoved(event);
//                    break;
//                default:
//                    //noop
//                    break;
//            }
//            // Remove reference to the listener, just to be safe.
//            listener = null;
//        }
//    }
    //---------------------------------------------------------------------
    // Methods to manage singleton instance
    //---------------------------------------------------------------------
    public static ProxiedItemDownloadService getInstance() {
        // Thread safe and lazy way to create a singleton
        return CacheDownloadServiceHolder.INSTANCE;
    }

    private static class CacheDownloadServiceHolder {

        private static final ProxiedItemDownloadService INSTANCE = new ProxiedItemDownloadService();
    }

    //---------------------------------------------------------------------
    // Methods inherited from Object
    //---------------------------------------------------------------------
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("This is a singleton, clone is not supported!");
    }
}
