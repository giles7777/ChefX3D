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
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;

// local imports
import org.chefx3d.util.ApplicationParams;

/**
 * This is a disk-backed implementation of a key-value cache.
 *
 * TODO Switch to filechannels so that file locking can be carried out
 *
 * @author Daniel Joyce
 * @version $Revision: 1.22 $
 */
public class CacheDiskStore implements CacheStoreInterface {

    /** Error Reporter */
    ErrorReporter log = DefaultErrorReporter.getDefaultReporter();

    /** Default name for the cache directory, if given nothing else */
    private static final String DEFAULT_CACHE_NAME = "chefx3dCache";

    /**The number of directories to hash over */
    private int numDirectories;

    /** The number of levels deep to go */
    private int dirDepth;

    /** Default location of where to locate cache files */
    private String storageRoot;

    /** Default name of cache directory */
    private String cacheName;

    /** File that represents the root of the cache */
    private File cacheRoot;

    /** Regex Pattern to remove from urls, null means none */
    private Pattern urlFilter;

    // Uses user home directory as root of disk store
    public CacheDiskStore() {
        initialize(0);
    }

    public CacheDiskStore(String cacheName) {
        this.cacheName = cacheName;
        initialize(0);
    }

    // Must represent a valid directory URI
    public CacheDiskStore(String storageRoot, String cacheName) {
        this.storageRoot = storageRoot;
        this.cacheName = cacheName;
        initialize(2);
    }

    public String getStorageRoot() {
        return storageRoot;
    }

    public void setStorageRoot(String rootPath) {
        this.storageRoot = rootPath;
    }

    public void setNumberOfDirectories(int numDir) {
        this.numDirectories = numDir;
    }

    public int getNumberOfDirectories() {
        return numDirectories;
    }

    public void setDirectoryDepth(int dirDepth) {
        this.dirDepth = dirDepth;
    }

    public int getDirectoryDepth() {
        return dirDepth;
    }

    /**
     * Initialize the cache with the current cache values
     *
     * @param rootType
     *  0: use user location for cache
     *  1: use public location for cache
     *  2: use already defined storage root
     */
    @SuppressWarnings("unchecked")
    void initialize(int rootType) {
        numDirectories = 10;
        dirDepth = 1;

        cacheName = (String)
            ApplicationParams.get(ApplicationParams.CACHE_NAME);

        if (cacheName == null) {
            cacheName = DEFAULT_CACHE_NAME;
        }

        // Check for the OS and place the cache in the right place. We
        // only need to do something different for Windows machines as
        // mac is unix under the covers anyway.
        //

        if (rootType == 0) {

            // Windows goes under the AppData/Local area
            // Unix goes under a dot directory in the user home area.

            storageRoot =
                (String)AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {

                        String userHome = System.getProperty("user.home");

                        String os = System.getProperty("os.name");
                        os = os.toLowerCase();
                        String finalDirectory = null;
                        String appName = (String)
                            ApplicationParams.get(ApplicationParams.APP_NAME);

                        if (os.indexOf("win") >= 0) {
                            if (os.indexOf("xp") >= 0)
                                finalDirectory = userHome + "\\Application Data\\" + appName;
                            else
                                finalDirectory = userHome + "\\AppData\\Local\\" + appName;
                        } else {
                            finalDirectory = userHome + "/." + appName;
                        }

                        return finalDirectory;
                    }
                });

        } else if (rootType == 1) {

            storageRoot =
                (String)AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {

                        // get the application name
                        String appName =
                            (String)ApplicationParams.get(ApplicationParams.APP_NAME);

                        // get the user home
                        String userHome = System.getProperty("user.home");

                        // get the OS to inspect
                        String os = System.getProperty("os.name");
                        os = os.toLowerCase();
                        String finalDirectory = null;

                        if (os.indexOf("win") >= 0) {
                            finalDirectory =
                                System.getenv("SYSTEMDRIVE") + "\\Users\\Public\\" + appName;
                        } else if (os.indexOf("mac") >= 0) {
                            finalDirectory = "/Users/Shared/" + appName;
                        } else {
                            finalDirectory = userHome + "/" + appName;
                        }

                        return finalDirectory;
                    }
                });

        } else {

            // use the currently defined storage root
        }

        cacheRoot = new File(storageRoot, cacheName);

System.out.println("cacheRoot: " + cacheRoot);

        if (!cacheRoot.exists()) {
            if (!cacheRoot.mkdirs()) {
                System.err.println(" Error creating directory root for disk cache");
            }
        }

    }

    /**
     * Deletes the asset from the cache
     *
     * @param key
     * @return a boolean indicating whether the deletion succedded.
     */
    public synchronized boolean removeAsset(String key) throws IOException {
        File storageFile = getStorageFileForKey(key);
        // We wait to create the file till storeAsset is called.
        if (storageFile.exists()) {
            storageFile.delete();
            return true;
        }
        return false;
    }

    public synchronized long getAssetSize(String key) {
        long size = -1;
        try {
            File storageFile = getStorageFileForKey(key);
            size = storageFile.length();
        } catch (Exception ex) {
        }
        return size;
    }

    /**
     * Computes a storage location for a asset given the key
     *
     * @param key The key to store the asset under
     * @return A URI pointing to the file containing the asset
     * @throws MalformedURLException
     */
    public File getStorageFileForKey(String key) throws MalformedURLException {
        if (key == null) {
            key = "";
        }

        if (urlFilter != null) {
            Matcher m = urlFilter.matcher(key);
            key = m.replaceAll("");
        }

        String ext = "";
        File storageFile = null;

        StringBuilder fpath = new StringBuilder();
        String FileSep = File.separator;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] hash = md.digest(key.getBytes());
            for (int i = 0; i < hash.length; i++) {
//            System.out.print(String.format("%02x", hash[i]));
            }
            //       System.out.println("");
            int i0 = (hash[0] << 24) + (hash[1] << 16) + (hash[2] << 8) + hash[3];
            int i1 = (hash[4] << 24) + (hash[5] << 16) + (hash[6] << 8) + hash[7];
            int i2 = (hash[8] << 24) + (hash[9] << 16) + (hash[10] << 8) + hash[11];
            int i3 = (hash[12] << 24) + (hash[13] << 16) + (hash[14] << 8) + hash[15];
            int i4 = (hash[16] << 24) + (hash[17] << 16) + (hash[18] << 8) + hash[19];
            // Trim off sign extension...
            long d = i0;
            d = d & 0x00000000FFFFFFFFl;
            for (int i = 0; i < dirDepth; i++) {
                long dirInt = d % numDirectories;
                d -= dirInt;
                d = d / numDirectories;
//            path.append(String.format("%04x",Math.abs(dirInt)));
                fpath.append(String.format("%04x", dirInt));
                //
                if (i < dirDepth - 1) {
                    fpath.append(FileSep);
                }
            }
            String fileName = convertToBase36(i0) + "_" + convertToBase36(i1) + "_" + convertToBase36(i2) + "_" + convertToBase36(i3) + "_" + convertToBase36(i4) + ext;

            storageFile = new File(cacheRoot + FileSep + fpath.toString() + FileSep + fileName);
//System.out.println("StorageFileURL: " + storageFile.getAbsolutePath());

        } catch (NoSuchAlgorithmException nsaex) {
            // TODO Use whatever logger we decide on
            System.err.println(" Error loading SHA-1 algorithm");
        }
        return storageFile;
    }

    public synchronized void storeAsset(String key, InputStream is) throws IOException {

        File storageFile = getStorageFileForKey(key);

        if (!storageFile.exists()) {
            storageFile.getParentFile().mkdirs();
            storageFile.createNewFile();
        }
        int buffSize = 4 * 1024;
        int bytesRead = 0;
        byte[] buffer = new byte[buffSize];
        OutputStream os = null;
        try {
            os = new FileOutputStream(storageFile);
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } finally {
            try {
                os.close();
            } catch (Exception ex) {
                //noop;
            }
        }
    //return new URL("cache:"+ext, null, -1, url.getPath());
    }

    public synchronized OutputStream storeAsset(String key) throws IOException {

        File storageFile = getStorageFileForKey(key);
        if (!storageFile.exists()) {
            storageFile.getParentFile().mkdirs();
            storageFile.createNewFile();
        }
        return new FileOutputStream(storageFile);
    }

    public synchronized InputStream retrieveAsset(String key) throws IOException {
        /*
         * There is a potential race condition here, given this method
         * returns immeadiately.
         *
         * The fileInputStream may then be reading from a file that
         * is being modified by a storeAsset call, leading to a corrupted
         * view of the cache.
         *
         * In the interim, we will return a stream wrapping a byte array,
         * but the performance may not be very good if file sizes
         * are large.
         *
         * In the future, will use some of work queue to order reads/writes
         * and a custom inputStream impl, or perhaps copy files to a new
         *
         * Another possibility is to copy the file to a temp file before returning
         * a inputstream, and using some kind of phatomreferences + a queue
         * to manage cleanup of temp files. Advantage is higher throughput
         *
         * Or selective locking/sleeping with file channel writes/reads.
         */
        //return new FileInputStream(getStorageFileForKey(key));
        byte[] buffer = null;
        FileChannel fChan = null;
        FileInputStream fIn = null;
        boolean loaded = false;

        try {
            fIn = new FileInputStream(getStorageFileForKey(key));
            fChan = fIn.getChannel();
            long fSize = fChan.size();
            ByteBuffer bBuffer = ByteBuffer.allocate((int) fSize);
            fChan.read(bBuffer);
            bBuffer.rewind();
            buffer = bBuffer.array();
            loaded = true;
        } finally {
            if (!loaded) {
                System.out.println("Could not load: " + key);
            }

            try {
                fChan.close();
            } catch (Exception ex) {
                //noop
            }
            try {
                fIn.close();
            } catch (Exception ex) {
                //noop
            }

        }
        return new ByteArrayInputStream(buffer);
    }

    public synchronized boolean doesAssetExist(String key) {
        boolean exists = false;
        try {
            File storageFile = getStorageFileForKey(key);
            exists = storageFile.exists();
        } catch (Exception ex) {
            // todo logging
        }
        return exists;
    }

    protected String convertToBase36(int i) {
        long l = i;
        // done to handle sign-extension.
        l = l & 0x00000000FFFFFFFFl;
        //System.out.println(l);
        char[] result = {'0', '0', '0', '0', '0', '0', '0'};
        int rIdx = result.length - 1;
        int base = 36;
        String digits = "0123456789abcdefghijklmnopqrstuvwxyz";
        while (l != 0) {
            int digitIdx = (int) (l % 36);
            l = l - digitIdx;
            l = l / base;
            result[rIdx] = digits.charAt(Math.abs(digitIdx));
            rIdx--;
        }
        return new String(result);
    }

    public boolean isCacheClearable() {
        return true;
    }

    public synchronized boolean clearCache() {
        return deleteDirectory(cacheRoot);
    }

    private boolean deleteDirectory(File path) {
        boolean success = true;
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    success = success && deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return success && (path.delete());
    }

    /**
     * Returns the Last Modified time for the asset pointed to by key, or 0
     * if it can't be determined. Uses File.lastModified.
     *
     * @param key
     * @return the last modified time in millis since epoch, or 0 if it
     * can't be determined.
     */
    public long getLastModified(String key) {
        long lastMod = 0;
        try {
            File f = getStorageFileForKey(key);
            lastMod =
                    f.lastModified();
        } catch (Exception ex) {
            log.errorReport("Error getting last modified date for " + key + ", returning " + lastMod, ex);
        }

        return lastMod;
    }

    /**
     * A filter for removing server names from URLs.  Any url used for
     * a key will be filtered by this regex pattern.
     *
     * @param pattern The regex pattern
     */
    public void setURLFilter(String pattern) {
        urlFilter = Pattern.compile(pattern);
    }
}
