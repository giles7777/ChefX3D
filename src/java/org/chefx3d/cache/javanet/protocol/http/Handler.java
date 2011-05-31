/*****************************************************************************
 *
 *                            (c) Yumetech 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 ****************************************************************************/
package org.chefx3d.cache.javanet.protocol.http;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author djoyce
 */
public class Handler extends sun.net.www.protocol.http.Handler{

    public Handler(String arg0, int arg1) {
        super(arg0, arg1);
    }

    public Handler() {
    }

    @Override
    protected int getDefaultPort() {
        return super.getDefaultPort();
    }

    @Override
    protected URLConnection openConnection(URL arg0) throws IOException {

        return new HttpURLConnection((sun.net.www.protocol.http.HttpURLConnection)super.openConnection(arg0),arg0);
    }

    @Override
    protected URLConnection openConnection(URL arg0, Proxy arg1) throws IOException {
        return super.openConnection(arg0, arg1);
    }
}

