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

import javax.xml.transform.*;

/**
 * This class represents a value in the template Map.
 * 
 * This code is derived from the O'Reilly book Java and XSLT by Eric M. Burke
 * Chapter 5.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.4 $
 */
class TemplateEntry {
    long lastModified; // when the file was modified

    Templates templates;

    TemplateEntry(long lastModified, Templates templates) {
        this.lastModified = lastModified;
        this.templates = templates;
    }
}
