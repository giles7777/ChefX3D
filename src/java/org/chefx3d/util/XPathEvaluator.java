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

import java.util.HashMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.xpath.*;

/**
 * This class evaluates XPath expressions. It implements a static cache for
 * speed.
 *
 * @author Alan Hudson
 * @version $Revision 1.1 $
 */
public class XPathEvaluator {
    /** The expression cache */
    private static HashMap<String, XPathExpression> exprCache;

    /** The xpath engine */
    private static XPath xpe;

    static {
        exprCache = new HashMap<String, XPathExpression>();

        try {
            XPathFactory xpf = XPathFactory
                    .newInstance(XPathConstants.DOM_OBJECT_MODEL);
            xpe = xpf.newXPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the value of a numerical expression. This method is not thread safe.
     *
     * @param expr The expression
     */
    public static double getValue(String expr, Node dom) {

        if (dom == null) {
            System.out.println("*** Null dom");
            new Exception().printStackTrace();
        }

        Double ret;
        XPathExpression calcSize;

        calcSize = (XPathExpression) exprCache.get(expr);

        try {
            if (calcSize == null) {
                calcSize = xpe.compile(expr);
                exprCache.put(expr, calcSize);
            }

            ret = (Double) calcSize.evaluate(dom, XPathConstants.NUMBER);
        } catch (Exception e) {
            System.out.println("Problems with xpath expression: " + expr);
            e.printStackTrace();
            return 0;
        }

        if (ret.isNaN()) {
            System.out.println("Cannot evaluate expression: " + expr);
            DOMUtils.print(dom);
        }
        return ret.doubleValue();
    }

    /**
     * Get the value of a string expression. This method is not thread safe.
     *
     * @param expr The expression
     */
    public static String getString(String expr, Node dom) {

        if (dom == null) {
            System.out.println("*** Null dom");
            new Exception().printStackTrace();
        }

        String ret;
        XPathExpression calcValue;

        calcValue = (XPathExpression) exprCache.get(expr);

        try {
            if (calcValue == null) {
                calcValue = xpe.compile(expr);
                exprCache.put(expr, calcValue);
            }

            ret = (String) calcValue.evaluate(dom, XPathConstants.STRING);
        } catch (Exception e) {
            System.out.println("Problems with xpath expression: " + expr);
            e.printStackTrace();
            return "";
        }

        if (ret == "") {
            //System.out.println("Cannot evaluate expression: " + expr);
            //DOMUtils.print(dom);
        }
        return ret;
    }

    /**
     * Get the value of a string expression. This method is not thread safe.
     *
     * @param expr The expression
     * @param errorReport Should an error be generated if its not found
     * @param dom The node to search from
     */
    public static Node getNode(String expr, boolean errorReport, Node dom) {

        Node node;

        XPath xpath = XPathFactory.newInstance().newXPath();

        if (dom == null) {
            System.out.println("*** Null dom");
            new Exception().printStackTrace();
        }

        try {

            node = (Node) xpath.evaluate(expr, dom, XPathConstants.NODE);

        } catch (Exception e) {
            System.out.println("Problems with xpath expression: " + expr);
            e.printStackTrace();
            return null;
        }

        if (node == null && errorReport) {
            System.out.println("Cannot evaluate expression: " + expr);
            DOMUtils.print(dom);
        }
        return node;
    }

    /**
     * Get the value of a string expression. This method is not thread safe.
     *
     * @param expr The expression
     * @param errorReport Should an error be generated if its not found
     * @param dom The node to search from
     */
    public static NodeList getNodeList(String expr, boolean errorReport, Node dom) {

        NodeList nodes;

        XPath xpath = XPathFactory.newInstance().newXPath();

        if (dom == null) {
            System.out.println("*** Null dom");
            new Exception().printStackTrace();
        }

        try {

            nodes = (NodeList) xpath.evaluate(expr, dom, XPathConstants.NODESET);

        } catch (Exception e) {
            System.out.println("Problems with xpath expression: " + expr);
            e.printStackTrace();
            return null;
        }

        if (nodes == null && errorReport) {
            System.out.println("Cannot evaluate expression: " + expr);
            DOMUtils.print(dom);
        }
        return nodes;
    }

}