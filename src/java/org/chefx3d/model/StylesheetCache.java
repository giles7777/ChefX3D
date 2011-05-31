/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

import java.io.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

/**
 * A utility class that caches XSLT stylesheets in memory.
 * 
 * This code is derived from the O'Reilly book Java and XSLT by Eric M. Burke
 * Chapter 5.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.5 $
 */
public class StylesheetCache {
    // map xslt file names to TemplateEntry instances
    private static Map<String, TemplateEntry> cache = new HashMap<String, TemplateEntry>();

    /**
     * Flush all cached stylesheets from memory, emptying the cache.
     */
    public static synchronized void flushAll() {
        cache.clear();
    }

    /**
     * Flush a specific cached stylesheet from memory.
     * 
     * @param xsltFileName the file name of the stylesheet to remove.
     */
    public static synchronized void flush(String xsltFileName) {
        cache.remove(xsltFileName);
    }

    /**
     * Obtain a new Transformer instance for the specified XSLT file name. A new
     * entry will be added to the cache if this is the first request for the
     * specified file name.
     * 
     * @param xsltFileName the file name of an XSLT stylesheet.
     * @return a transformation context for the given stylesheet.
     */
    public static synchronized Transformer newTransformer(String xsltFileName)
            throws TransformerConfigurationException {
        File xsltFile = new File(xsltFileName);

        long xslLastModified = xsltFile.lastModified();
        TemplateEntry entry = (TemplateEntry) cache.get(xsltFileName);

        if (entry != null) {
            // if the file has been modified more recently than the
            // cached stylesheet, remove the entry reference
            if (xslLastModified > entry.lastModified) {
                entry = null;
            }
        }

        // create a new entry in the cache if necessary
        if (entry == null) {
            Source xslSource = new StreamSource(xsltFile);

            TransformerFactory transFact = TransformerFactory.newInstance();
            Templates templates = transFact.newTemplates(xslSource);

            entry = new TemplateEntry(xslLastModified, templates);
            cache.put(xsltFileName, entry);
        }

        return entry.templates.newTransformer();
    }

    // prevent instantiation of this class
    private StylesheetCache() {
    }
}
