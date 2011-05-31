/*****************************************************************************
 *                        Web3d.org Copyright (c) 2001
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
import java.util.HashMap;
import org.ietf.uri.FileNameMap;

/**
 * An implementation of a file name mapping for handling VRML file types.
 * <p>
 *
 * When asked for a .wrl file it will return <code>model/vrml</code>. For the
 * reverse mapping it will handle both the old and new types
 * <p>
 *
 * This filename mapping must be registered with the URI class by the user in
 * order to work. You can use the following code to do this:
 *
 * <pre>
 *  import org.ietf.uri.URI;
 *  import org.ietf.uri.FileNameMap;
 *
 *  ...
 *
 *       FileNameMap fn_map = URI.getFileNameMap();
 *       if(!(fn_map instanceof VRMLFileNameMap)) {
 *           fn_map = new VRMLFileNameMap(fn_map);
 *           URI.setFileNameMap(fn_map);
 *       }
 * </pre>
 *
 * @author  Justin Couch, Daniel Joyce
 * @version $Revision: 1.1 $
 */
public class CacheFileNameMap implements FileNameMap
{
    /* TODO since this is essentially a copy of VRML filenameMap,
     * Then we should really use a abstract base class to share
     * common code eventually
     */

    /** A followup map that may help to resolve if we don't */
    private FileNameMap nextMap;

    /**
     * Create a default filename map that does not delegate to any other
     * map if we can't resolve it.
     */
    public CacheFileNameMap() {
        this(null);
    }

    /**
     * Create a filename map that will delegate to the given map if we cannot
     * resolve the name locally. If the parameter is null, no checking will
     * be done.
     *
     * @param map The map to delegate to
     */
    public CacheFileNameMap(FileNameMap map) {
        nextMap = map;
    }

    /**
     * Fetch the content type for the given file name. If we don't
     * understand the file name, return null.
     *
     * @param filename The name of the file to check
     * @return The content type for that file
     */
    public String getContentTypeFor(String filename) {

        int index = filename.lastIndexOf('.');
        String ext = filename.substring(index + 1);
        ext = ext.toLowerCase();

        String ret_val = ClientMimeType.lookupMimeTypeForFileExtension(filename);

        if((ret_val == null) && (nextMap != null)) {
            ret_val = nextMap.getContentTypeFor(filename);
        }

        return ret_val;
    }

    /**
     * Get the standardised extension used for the given MIME type. This
     * provides a reverse mapping feature over the standard
     * {@link java.net.FileNameMap} that only supplies the opposite method.
     *
     * @param mimetype The mime type to check for
     * @return The extension or <CODE>null</CODE> if it cannot be resolved
     */
    public String getFileExtension(String mimetype) {
        String ret_val = ClientMimeType.lookupFileExtensionForMimeType(mimetype);

        if((ret_val == null) && (nextMap != null)) {
            ret_val = nextMap.getFileExtension(mimetype);
        }

        return ret_val;
    }
}
