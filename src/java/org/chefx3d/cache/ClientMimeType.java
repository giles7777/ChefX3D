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
import java.util.HashMap;
import java.util.Map;
import org.ietf.uri.FileNameMap;

/**
 * Enums with dictionaries and convenience methods to manage the lookup of
 * mimetypes explicitly handled by the cache content handlers
 *
 * @see ToolContentHandler
 * @see ToolGroupContentHandler
 * @see CatalogContentHandler
 *
 * @author djoyce
 */
public enum ClientMimeType {

    CATALOG("ctg", "application/x-chefx3d-catalog"),
    TOOL_GROUP("tlg", "application/x-chefx3d-toolgroup"),
    TOOL("tl", "application/x-chefx3d-tool"),
    CAD("ccd", "application/x-common-client-cad"),
    PNG("png", "image/png");

    private final String fileExtension;

    private final String mimeType;

    private static Map<String, ClientMimeType> fileExtToCMType = null;

    private static Map<String, ClientMimeType> mTypeToCMType = null;

    private FileNameMap nextMap = null;

    /* Enum instances are intialized before static initializers are called, so we
     * have to do things like this. IE, build the enums first, then build
     *our lookup maps.
     */

    static {
        fileExtToCMType = new HashMap<String, ClientMimeType>();
        mTypeToCMType = new HashMap<String, ClientMimeType>();
        for (ClientMimeType cmt : ClientMimeType.values()) {
            fileExtToCMType.put(cmt.fileExtension, cmt);
            mTypeToCMType.put(cmt.mimeType, cmt);
        }
    }

    ClientMimeType(String fileExtension, String mimeType) {
        this.fileExtension = fileExtension;
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static String lookupMimeTypeForFileExtension(String fileExtension) {
        ClientMimeType cmt = lookupClientMimeType(fileExtension);
        if (cmt == null) {
            return null;
        }
        return cmt.getMimeType();
    }

    public static String lookupFileExtensionForMimeType(String mimeType) {
        ClientMimeType cmt = lookupClientMimeType(mimeType);
        if (cmt == null) {
            return null;
        }
        return cmt.fileExtension;
    }

    public static ClientMimeType lookupClientMimeType(String ExtensionOrMimeType) {
        ClientMimeType cmt = mTypeToCMType.get(ExtensionOrMimeType);
        if (cmt == null) {
            cmt = fileExtToCMType.get(ExtensionOrMimeType);
        }
        return cmt;
    }
}
