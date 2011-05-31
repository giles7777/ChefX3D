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
package org.chefx3d.cache.content;

// External imports
import org.chefx3d.cache.ClientMimeType;
import java.io.InputStream;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.ietf.uri.ContentHandler;
import org.ietf.uri.ContentHandlerFactory;

/**
 * This contentHandlerFactory is cache aware. It uses configurable parsers
 * to parse inputstreams and return the appropriate objects.
 *
 * This allows it to be configured to accept say JSON, and return customized
 * subclasses of Tool, ToolGroup, or Catalog. Or accept XML, or YAML, or
 * ObjectInputstreams.
 *
 * @author Daniel Joyce
 * @version $Revision: 1.4 $
 */
public class CacheAwareContentHandlerFactory implements ContentHandlerFactory {

    /** next factory to call if this one can't determine an appropriatre content handler */
    private CacheAwareContentHandlerFactory nextFactory = null;

    /** Parser to use to parse Tools */
    private ContentParser<? extends SimpleTool,InputStream> toolParser = null;

    /** Parser to use to parse groups */
    private ContentParser<? extends ToolGroup, InputStream> toolGroupParser = null;

    /** Parser to use to parse catalogs */
    private ContentParser<? extends Catalog,InputStream> catalogParser = null;

    /**
     * A chainable ContentHandlerFactory that is configurable and can produce
     * Tools, ToolGroups, and Catalogs
     *
     * @param toolParser A class instance  capable of parsing a inputStream into Tool objects
     * @param toolGroupParser A class instance  capable of parsing a inputStream into Tool objects
     * @param catalogParser A class instance  capable of parsing a inputStream into Tool objects
     * @param nextFactory The next factory to call if this one can't handle mime type
     * @see ContentParser
     */
    public CacheAwareContentHandlerFactory(ContentParser<? extends SimpleTool,InputStream> toolParser,
            ContentParser<? extends ToolGroup,InputStream> toolGroupParser,
            ContentParser<? extends Catalog,InputStream> catalogParser, CacheAwareContentHandlerFactory nextFactory) {
        this.toolParser = toolParser;
        this.toolGroupParser = toolGroupParser;
        this.catalogParser = catalogParser;
        this.nextFactory = nextFactory;
    }

    public ContentHandler createContentHandler(String mimetype) {
        ClientMimeType cmt = ClientMimeType.lookupClientMimeType(mimetype);
        ContentHandler ch = null;
        try {
            switch (cmt) {
                case CATALOG:
                    ch = new ChefContentHandler(catalogParser);
                    break;
                case TOOL_GROUP:
                    ch = new ChefContentHandler(toolGroupParser);
                    break;
                case TOOL:
                    ch = new ChefContentHandler(toolParser);
                    break;
                default:
                    ch = nextFactory.createContentHandler(mimetype);
                    break;
            }
        } catch (Exception ex) {
        }
        return ch;
    }
}
