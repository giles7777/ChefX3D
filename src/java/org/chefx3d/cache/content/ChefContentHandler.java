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

// Remote imports
import java.io.IOException;
import org.chefx3d.cache.ClientCache;
import org.ietf.uri.ContentHandler;
import org.ietf.uri.ResourceConnection;

/**
 *
 * @author daniel
 */
public class ChefContentHandler  extends ContentHandler {

    private ContentParser cp = null;

    protected ChefContentHandler(){
    }

    public ChefContentHandler(ContentParser cp){
        this.cp = cp;
    }

    protected ClientCache getClientCache(){
        return ClientCache.getInstance();
    }

    @Override
    public Object getContent(ResourceConnection arg0) throws IOException {
        return cp.parse(arg0.getInputStream());
    }
}
