/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006
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

import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.DOMSource;

public class DOMUtils {
    // private static final String IDENTITY_TRANSFORM = "<xsl:stylesheet version
    // = '1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'><xsl:output
    // method='xml' omit-xml-declaration='yes' indent='yes'/><xsl:template
    // match='@*|node()'><xsl:copy><xsl:apply-templates
    // select='@*|node()'/></xsl:copy></xsl:template></xsl:stylesheet>";
    private static final String IDENTITY_TRANSFORM = "<xsl:stylesheet version = '1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'><xsl:output method='xml' omit-xml-declaration='yes' indent='yes'/><xsl:template match='node()|@*'> <xsl:copy>   <xsl:apply-templates select='@*'/>   <xsl:apply-templates/>   </xsl:copy> </xsl:template></xsl:stylesheet>";

    /**
     * Print a DOM to standard out.
     *
     * @param node The root node
     */
    public static void print(Node node) {
    	if (node == null) {
    		System.out.println("DOMUtils.print: Null node");
    		return;
    	}
    	
        try {
            DOMSource ds = new DOMSource(node);
            StreamResult result = new StreamResult(System.out);
            TransformerFactory transFact = TransformerFactory.newInstance();

            // Source xslSource = new StreamSource(new
            // StringBufferInputStream(IDENTITY_TRANSFORM));
            Source xslSource = new StreamSource(new StringReader(
                    IDENTITY_TRANSFORM));

            Transformer trans = transFact.newTransformer(xslSource);

            trans.transform(ds, result);
            System.out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Print a DOM to specified stream.
     *
     * @param node The root node
     * @param out The stream to print to
     */
    public static void print(Node node, PrintStream out) {
        try {
            DOMSource ds = new DOMSource(node);
            StreamResult result = new StreamResult(out);
            TransformerFactory transFact = TransformerFactory.newInstance();

            // Source xslSource = new StreamSource(new
            // StringBufferInputStream(IDENTITY_TRANSFORM));
            Source xslSource = new StreamSource(new StringReader(
                    IDENTITY_TRANSFORM));

            Transformer trans = transFact.newTransformer(xslSource);

            trans.transform(ds, result);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Print a DOM to specified stream.
     *
     * @param node The root node
     * @param out The writer to print to
     */
    public static void print(Node node, Writer out) {
        try {
            DOMSource ds = new DOMSource(node);
            StreamResult result = new StreamResult(out);
            TransformerFactory transFact = TransformerFactory.newInstance();

            // Source xslSource = new StreamSource(new
            // StringBufferInputStream(IDENTITY_TRANSFORM));
            Source xslSource = new StreamSource(new StringReader(
                    IDENTITY_TRANSFORM));

            Transformer trans = transFact.newTransformer(xslSource);

            trans.transform(ds, result);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse an XML string into a document.
     *
     * @param xml The xml string fragment
     * @return The parsed XML
     */
    public static Document parseXMLDocument(String xml) {
        StringReader sr = new StringReader(xml);

        DocumentBuilderFactory builderFactory;
        DocumentBuilder builder;

        try {
            builderFactory = DocumentBuilderFactory.newInstance();
            builder = builderFactory.newDocumentBuilder();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Document document = builder.parse(new InputSource(sr));
            return document;
        } catch (FileNotFoundException fnfe) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Parse an XML fragment into a document.
     *
     * @param xml The xml string fragment
     * @return The parsed XML
     */
    public static Document parseXML(String xml) {
        StringReader sr = new StringReader(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml);

        DocumentBuilderFactory builderFactory;
        DocumentBuilder builder;

        try {
            builderFactory = DocumentBuilderFactory.newInstance();
            builder = builderFactory.newDocumentBuilder();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Document document = builder.parse(new InputSource(sr));
            return document;
        } catch (FileNotFoundException fnfe) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Merge two DOM's. Any values in the source DOM will be overwritten by
     * values in the dest DOM. This assumes the dest DOM is a superset of the
     * overrides. All elements will have a 1:1 mapping. Any elements without a
     * 1:1 mapping will be dropped in at the last mapping point. The dest DOM
     * will overwrite element thats in the default
     *
     * @param overrides The value overrides
     * @param dest The destination DOM
     */
    public static void mergeDOM(Node overrides, Document dest) {
        /*
         * This is what will happen in this method:
         *
         * Defaults/Dest:
         *
         * <A> <B foo="0" /> <C val="0" /> <Position> <X>0</X> <Y>0</Y> <Z>0</Z>
         * </Position> </A>
         *
         * Overrides:
         *
         * <A> <B foo="1" bar="2" /> <Position> <X>10</X> <Y>15</Y> <Z>20</Z>
         * <extra>Extra data</extra> </Position> <A>
         *
         *
         * final:
         *
         * <A> <B foo="1" bar="2" /> <C val="0" /> <Position> <X>10</X> <Y>15</Y>
         * <Z>20</Z> <extra> </Position> <A>
         */

        // same element name, merge attributes, replace element content
        // different element names, import dest
        // traverse overrides filling in values
        // System.out.println("mergeDOM: " + overrides.getNodeName());
        if (overrides == null)
            return;

        if (overrides instanceof Element) {
            // need to detect when we leave template. This is likely an
            // expensive way to do that
            NodeList list = dest.getElementsByTagName(overrides.getNodeName());

            if (list.getLength() == 0) {
                // System.out.println("Couldn't find node, inserting: " +
                // overrides.getNodeName());
                Node n = dest.importNode(overrides, true);
                Element p = (Element) findNode(dest, (Element) overrides
                        .getParentNode());
                p.appendChild(n);
                return;
            }
        }

        if (overrides instanceof Text) {
            // replace text
            Element el = (Element) findNode(dest, (Element) overrides
                    .getParentNode());

            if (el == null)
                return;

            Text t = (Text) el.getFirstChild();

            if (t != null)
                t.replaceWholeText(((Text) overrides).getWholeText());

            return;
        }

        if (overrides.hasChildNodes()) {
            NodeList list = overrides.getChildNodes();
            int len = list.getLength();

            for (int i = 0; i < len; i++) {
                mergeDOM(list.item(i), dest);
            }
        } else {
            // Find the node in source, if there:
            // replace contents
            // add/replace attributes

            Element node = (Element) findNode(dest, (Element) overrides);

            if (node == null) {
                System.out.println("Couldn't find node, shouldn't be here!");
            } else {
                NamedNodeMap atts = overrides.getAttributes();
                int len = atts.getLength();

                for (int i = 0; i < len; i++) {
                    Attr srcAtt = (Attr) atts.item(i);
                    node
                            .setAttributeNode((Attr) dest.importNode(srcAtt,
                                    false));
                }
            }
        }
    }

    /**
     * Find a node in a DOM. This is looking for a node which has the same path.
     * Ie /Foo/Bar/MyNode
     *
     * @param source The source to look in
     * @param n The node were are looking for
     * @return The node or null if its not there
     */
    private static Node findNode(Document source, Element n) {
        String name = n.getTagName();
        NodeList list = source.getElementsByTagName(name);

        int len = list.getLength();
        // System.out.println("findNode: " + name + " possibles: " + len);

        if (len == 0)
            return null;

        Element sn;

        for (int i = 0; i < len; i++) {
            sn = (Element) list.item(i);

            if (compareNodeLocation(sn, n))
                return sn;
        }

        return null;
    }

    /**
     * Compare two nodes location in a tree. Returns true if all they have the
     * same XPATH location.
     *
     * @param n1 The first node
     * @param n2 The second node
     * @return true if they have the same location
     */
    private static boolean compareNodeLocation(Element n1, Element n2) {
        // simple check for now, compare parents
        Node p1 = n1.getParentNode();
        Node p2 = n2.getParentNode();

        if (p1 == null && p2 == null)
            return true;
        if (p1 == null && p2 != null)
            return false;
        if (p1 != null && p2 == null)
            return false;

        String name1 = p1.getNodeName();
        String name2 = p2.getNodeName();

        if (name1.equals(name2))
            return true;

        return false;
    }

    /**
     * Gets a single element.  Will issues errors if its finds 0
     * If multiple are found the first element will be returned.
     *
     * @param name The element name
     * @param n The node to search from
     * @param issueError Should an error be issued
     * @param errorMessage The message to issue if in error
     * @return The element or null
     */
    public static Element getSingleElement(String name, Element n, boolean issueError, String errorMessage, ErrorReporter errorReporter) {
        NodeList list = n.getElementsByTagName(name);
        int len = list.getLength();

        if (len <= 0) {
            if (issueError)
                errorReporter.errorReport("No Element found: " + errorMessage, new Exception());
            return null;
        } else {
            return (Element) list.item(0);
        }
    }

    /**
     * Gets a single element.  Will issues errors if its finds 0 or 1+
     * If multiple are found no element will be returned.
     *
     * @param name The element name
     * @param doc The document to search from
     * @param issueError Should an error be issued
     * @param errorMessage The message to issue if in error
     * @return The element or null
     */
    public static Element getSingleElement(
            String name, 
            Document doc, 
            boolean issueError, 
            String errorMessage, 
            ErrorReporter errorReporter) {
        
        NodeList list = doc.getElementsByTagName(name);
        int len = list.getLength();

        if (len <= 0) {
            if (issueError)
                errorReporter.messageReport("No Element found: " + errorMessage);
            return null;
        } else if (len > 1) {
            if (issueError)
                errorReporter.messageReport("Too many Elements found: " + errorMessage);
            return null;
        }

        return (Element) list.item(0);
    }

    /**
     * Find the first element within a list of Elements with a attribute of the value desired.
     *
     * @param attrib The attribute name
     * @param value The value
     * @param list The list to search
     * @return The element or NULL if not found
     */
    public static Element findFirstElement(String attrib, String value, NodeList list) {
        int len = list.getLength();
        Element el;
        Node n;
        String val;

        for(int i=0; i < len; i++) {
            n = list.item(i);
            if (!(n instanceof Element))
                continue;

            el = (Element) n;
            val = el.getAttribute(attrib);
            if (val != null && val.equals(value))
                return el;
        }

        return null;
    }

    public static void main(String[] args) {
        Node defaults = DOMUtils
                .parseXML("<ChefX3D><EntityParams><Cylinder bottom='TRUE' height='2' radius='1' /><test>Test1</test></EntityParams></ChefX3D>");
        Node overrides = DOMUtils
                .parseXML("<ChefX3D><EntityParams><Cylinder bottom='TRUE' height='2' radius='1' side='TRUE' solid='TRUE' top='TRUE' /><test>Test2</test><Foo>My data</Foo></EntityParams></ChefX3D>");

        mergeDOM(overrides, (Document) defaults);

        System.out.println("Final DOM:\n");
        print(defaults);
    }
}