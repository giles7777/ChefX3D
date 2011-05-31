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

package org.chefx3d.util;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

/**
 * A UserDataHandler that refs all its userData contents.
 * 
 * @author Alan Hudson
 * @version $Revision 1.1 $
 */
public class ReferenceUserDataHandler implements UserDataHandler {
    public void handle(short operation, String key, Object data, Node src,
            Node dst) {
        switch (operation) {
        case NODE_CLONED:
        case NODE_ADOPTED:
        case NODE_IMPORTED:
            dst.setUserData(key, data, this);
            break;
        }
    }
}