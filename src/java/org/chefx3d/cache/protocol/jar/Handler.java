/*****************************************************************************
 *                The Virtual Light Company Copyright (c) 1999
 *                               Java Source
 *
 * This code is licensed under the GNU Library GPL. Please read license.txt
 * for the full details. A copy of the LGPL may be found at
 *
 *
 ****************************************************************************/

package org.chefx3d.cache.protocol.jar;

import java.io.IOException;
import java.net.MalformedURLException;

import org.ietf.uri.URI;
import org.ietf.uri.URIUtils;
import org.ietf.uri.ResourceConnection;
import org.ietf.uri.URIResourceStream;
import org.ietf.uri.MalformedURNException;

/**
 * Copy of handler from vlc.net.protocol.jar that
 * returns cache-aware jarConnections
 *
 * @author  Justin Couch, Daniel Joyce
 * @version 0.8 (7 August 2009)
 */
public class Handler extends URIResourceStream
{
  /**
   * Explicit public constructor as required by java reflection.
   * Currently does nothing.
   */
  public Handler()
  {
  }

  /**
   * Open a connection for the given URI. The host and port arguments for
   * this stream type are ignored. If a host is needed for a UNC name
   * then that is included in the path.
   *
   * @param host The host name to connect to
   * @param port The port on the host
   * @param path The path needed to access the resource using the given protocol
   * @exception IOException I/O errors reading from the stream
   * @exception IllegalArgumentException host, port or URI were invalid
   */
  protected ResourceConnection openConnection(String host,
                                              int port,
                                              String path)
    throws IOException, IllegalArgumentException
  {
    ResourceConnection res = null;

    // split the path into the two parts
    int index = path.indexOf('!');
    String uri_str = path.substring(0, index);
    String entry = null;

    if(path.length() > index + 1)
      entry = path.substring(index + 2);

    try
    {
      URI uri = URIUtils.createURI(uri_str);

      res = new JarConnection(uri, entry);
    }
    catch(MalformedURLException mue)
    {
      throw new IllegalArgumentException("Cannot construct JAR file URL");
    }
    catch(MalformedURNException mne)
    {
      throw new IllegalArgumentException("Cannot construct JAR file URN");
    }

    return res;
  }
}
