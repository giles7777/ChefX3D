/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.toolbar.awt;

// External imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Local imports
import org.chefx3d.tool.*;

/**
 * An implementation of the MutableTreeNode that represents a single ToolGroup
 * or Tool instance within a JTree.
 * <p>
 *
 * @author Justin Couch
 * @version $Revision: 1.3 $
 */
class ToolGroupTreeNode implements MutableTreeNode {

    /** The Tool/ToolGroup node this represents */
    private ToolGroupChild toolItem;

    /** Random user object supplied instance */
    private Object userData;

    /**
     * The direct children of this node. The list contains both attribute and
     * element children of this node. All attributes appear first in the list.
     */
    private ArrayList<ToolGroupTreeNode> children;

    /** Mapping of children objects to TreeNodes for reverse lookups */
    private HashMap<ToolGroupChild, ToolGroupTreeNode> childMap;

    /** The parent node of this one */
    private ToolGroupTreeNode parent;

    /**
     * Flag to indicate we've loaded the kids for this node. Package access
     * because it is set directly by the DOMTreeModel
     */
    boolean kidsLoaded;

    /**
     * Create an instance of the tree node that represents the given DOM Node.
     * If the node allows events, then this will register itself as a
     * listener.
     *
     * @param node The DOM node this tree node represents
     */
    public ToolGroupTreeNode(ToolGroupChild toolItem, ToolGroupTreeNode parent) {
        this.toolItem = toolItem;
        this.parent = parent;

        children = new ArrayList<ToolGroupTreeNode>();
        childMap = new HashMap<ToolGroupChild, ToolGroupTreeNode>();

        kidsLoaded = false;
    }

    public String toString() {
        if(toolItem == null)
            return "Catalog root";
        else
            return toolItem.getName();
    }

    //----------------------------------------------------------
    // Methods defined by MutatableTreeNode.
    //----------------------------------------------------------

    /**
     * Insert the child at the given position.
     *
     * @param child The new child to insert
     * @param index The position to insert the child into
     */
    public void insert(MutableTreeNode child, int index) {
        if(index == children.size())
            children.add((ToolGroupTreeNode)child);
        else
            children.add(index, (ToolGroupTreeNode)child);

        ToolGroupChild tcg = ((ToolGroupTreeNode)child).getToolObject();
        childMap.put(tcg, (ToolGroupTreeNode)child);
        kidsLoaded = true;       
    }

    /**
     * Remove the child at the given index position. If there is no child
     * there it will do nothing.
     */
    public void remove(int index) {
        ToolGroupTreeNode child = children.get(index);
        ToolGroupChild tcg = ((ToolGroupTreeNode)child).getToolObject();
        children.remove(index);
        childMap.remove(tcg);
    }

    /**
     * Remove the given tree node from the list of children of this node.
     *
     * @param child The node to remove
     */
    public void remove(MutableTreeNode child) {
        children.remove(child);

        ToolGroupChild tcg = ((ToolGroupTreeNode)child).getToolObject();
        childMap.remove(tcg);
    }

    /**
     * Remove this node from it's parent. If this is the root node then this
     * will ignore the request.
     */
    public void removeFromParent() {
        if(parent == null)
            return;

        parent.remove(this);
    }

    /**
     * Set the parent node of this node to the new value.
     *
     * @param parent The new node to use as a parent
     */
    public void setParent(MutableTreeNode parent) {
        this.parent = (ToolGroupTreeNode)parent;
    }

    /**
     * Add some user data to this object.
     *
     * @param obj The data to be stored
     */
    public void setUserObject(Object obj) {
        userData = obj;
    }

    //----------------------------------------------------------
    // Methods defined by TreeNode.
    //----------------------------------------------------------

    /**
     * Get the list of children of this node as an enumeration. If the node
     * could have children, but does not at the moment, it will return an
     * empty enumeration.
     *
     * @return An enumeration, possibly empty of the children
     */
    public Enumeration children() {
        return Collections.enumeration(children);
    }

    /**
     * Check to see if this node allows children. For the purposes of the DOM
     * view of the world, a leaf and allowing children are the same thing. We
     * do not consider whether the node is an X3D node type or not.
     *
     * @return true if this node allows children
     */
    public boolean getAllowsChildren() {
        return !(toolItem instanceof SimpleTool);
    }

    /**
     * Get the child at the given index position. If there is no child there
     * it will return null.
     *
     * @param index The position to check
     * @return The tree node at the index
     */
    public TreeNode getChildAt(int index) {
        return children.get(index);
    }

    /**
     * Get the number of children of this node. The children count is of the
     * tree node children, not the DOM children. Tree children includes the
     * attributes as well
     *
     * @return The number of children of this child
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Get the index of the given tree node.
     *
     * @param node The node to find the index of
     * @return The index of the given node or -1 if not found
     */
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    /**
     * Get the parent node of this node. If this is the root of the tree, the
     * return value is null.
     *
     * @return The parent node
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Check to see if this instance is a leaf node
     *
     * @return true if this is a leaf and cannot have children
     */
    public boolean isLeaf() {
        if(toolItem instanceof SimpleTool)
            return true;

        ToolGroup tg = (ToolGroup)toolItem;
        return (tg.getChildren().size() == 0);
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

    /**
     * Get the DOM node that this tree node represents. Used by the renderer
     * to build custom information about the node type.
     *
     * @return The DOM node
     */
    ToolGroupChild getToolObject() {
        return toolItem;
    }

    /**
     * Get the user data stored in this object.
     *
     * @return The currently set user data
     */
    public Object getUserData() {
        return userData;
    }

    /**
     * Do a reverse lookup of the children to find the tree node that
     * corresponds to the given Node instance.
     */
    ToolGroupTreeNode getTreeNodeChild(ToolGroupChild child) {
        return (ToolGroupTreeNode)childMap.get(child);
    }
}
