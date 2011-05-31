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

// External Imports
import javax.swing.tree.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.j3d.util.I18nManager;

// Internal Imports
import org.chefx3d.tool.*;

import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogListener;

import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Custom tree model that looks after a single catalog.
 * <p>
 * The tree model shows all catalog entries except for the tools that
 * a directly used from a ToolGroup structure. That is implicit in this
 * model.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>nodeInsertMsg: Message when an exception is generated in the
 *     treeNodesInserted() callback</li>
 * <li>nodeRemoveMsg: Message when an exception is generated in the
 *     treeNodesRemoved() callback</li>
 * <li>nodeStructureChangeMsg: Message when an exception is generated in the
 *     treeStructureChanged() callback</li>
 * <li>nodeChangeMsg: Message when an exception is generated in the
 *     treeNodesChanged() callback</li>
 * </ul>
 *
 * @author Justin Couch
 * @version $Revision: 1.6 $
 */
class CatalogTreeModel
    implements TreeModel, CatalogListener, ToolGroupListener {

    /** Error message when the user code barfs */
    private static final String NODE_ADD_ERR_PROP =
        "org.chefx3d.toolbar.awt.CatalogTreeModel.nodeInsertMsg";

    /** Error message when the user code barfs */
    private static final String NODE_REMOVE_ERR_PROP =
        "org.chefx3d.toolbar.awt.CatalogTreeModel.nodeRemoveMsg";

    /** Error message when the user code barfs */
    private static final String STRUCTURE_CHANGE_ERR_PROP =
        "org.chefx3d.toolbar.awt.CatalogTreeModel.nodeStructureChangeMsg";

    /** Error message when the user code barfs */
    private static final String NODE_CHANGE_ERR_PROP =
        "org.chefx3d.toolbar.awt.CatalogTreeModel.nodeChangeMsg";

    /** The tree node representing the root of the tree */
    private ToolGroupTreeNode catalogRoot;

    /** The catalog that this tree model is managing */
    private final Catalog catalog;

    /** The list of listeners registered with this model */
    private ArrayList<TreeModelListener> listeners;

    /** Reporter instance for handing out errors */
    private ErrorReporter errorReporter;

    /**
     * Construct a new tree model around this catalog.
     *
     * @param cat The catalog to handle.
     */
    CatalogTreeModel(Catalog cat) {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        catalog = cat;
        catalog.addCatalogListener(this);

        catalogRoot = new ToolGroupTreeNode(null, null);
        listeners = new ArrayList<TreeModelListener>();
    }

    //----------------------------------------------------------
    // Methods defined by TreeModel
    //----------------------------------------------------------

    /**
     * Get the child of the given parent at that index.
     *
     * @param parent The parent node to ask
     * @param index The position to get the child for
     * @return The TreeNode object at that position
     */
    public Object getChild(Object parent, int index) {
        ToolGroupTreeNode treeNode = (ToolGroupTreeNode)parent;
        ToolGroupTreeNode kid = (ToolGroupTreeNode)treeNode.getChildAt(index);

        // check to see if the kids have been built.
        if(!kid.kidsLoaded) {
            buildChildren(kid);
            kid.kidsLoaded = true;
        }

        return kid;
    }

    /**
     * Get the number of children the given parent contains. The number is
     * both child elements and attributes as this tree will show both.
     *
     * @param parent The parent to quiz for the number of children
     * @return The number of children of that parent
     */
    public int getChildCount(Object parent) {
        ToolGroupTreeNode treeNode = (ToolGroupTreeNode)parent;
        return treeNode.getChildCount();
    }

    /**
     * Get the index of the given child in the parent node.
     *
     * @param parent The parent node to check for
     * @param child The child to find the index of
     * @return The position of the child in the parent
     */
    public int getIndexOfChild(Object parent, Object child) {
        ToolGroupTreeNode treeNode = (ToolGroupTreeNode)parent;
        return treeNode.getIndex((TreeNode)child);
    }

    /**
     * Get the object that represents the root of this tree model.
     *
     * @return The root document object
     */
    public Object getRoot() {
        return catalogRoot;
    }

    /**
     * Check to see if the given node is a leaf node. Leaf nodes are
     * determined if the DOM object does not support children.
     *
     * @param child The child node to check
     * @return True if the DOM node is a leaf
     */
    public boolean isLeaf(Object child) {
        ToolGroupTreeNode treeNode = (ToolGroupTreeNode)child;
        ToolGroupChild kid = treeNode.getToolObject();

        return (kid instanceof SimpleTool);
    }

    /**
     * Notification that the UI has changed the value to the destination
     * object to the new value. We should never change anything like that in
     * this case.
     *
     * @param path The path to the object that changed
     * @param value The new value for the Node
     */
    public void valueForPathChanged(TreePath path, Object value) {
    }

    /**
     * Add a tree listener to this model. Only one instance of the listener
     * can be added at a time. A second call to add the same instance will
     * silently ignore the request.
     *
     * @param l The listener to be added
     */
    public void addTreeModelListener(TreeModelListener l) {
        if((l != null) && !listeners.contains(l))
            listeners.add(l);
    }

    /**
     * Remove the tree listener from this model. If the instance is not
     * known about the request is silently ignored.
     *
     * @param l The listener to be removed
     */
    public void removeTreeModelListener(TreeModelListener l) {
        if((l != null) && listeners.contains(l))
            listeners.remove(l);
    }

    //----------------------------------------------------------
    // Methods defined by CatalogListener
    //----------------------------------------------------------

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param name The catalog name
     * @param group The toolGroup added to
     */
    public void toolGroupAdded(String name, ToolGroup group) {
        if(!name.equals(catalog.getName()))
            return;

        TreePath path = new TreePath(catalogRoot);
        Object[] children = new Object[1];
        int[] indicies = new int[1];

        ToolGroupTreeNode child = new ToolGroupTreeNode(group, catalogRoot);
        catalogRoot.insert(child, catalogRoot.getChildCount());

        int index = catalogRoot.getIndex(child);
        indicies[0] = index;
        children[0] = child;

//System.out.println("Catalog root adding group " + group.getName());

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesInserted(treeEvent);

        group.addToolGroupListener(this);
    }

    /**
     * A group of tool groups have been added.
     *
     * @param name The catalog name
     * @param groups The list of tool groups added
     */
    public void toolGroupsAdded(String name, List<ToolGroup> groups) {
        if(!name.equals(catalog.getName()))
            return;

        TreePath path = new TreePath(catalogRoot);
        Object[] children = new Object[groups.size()];
        int[] indicies = new int[groups.size()];

        for(int i = 0; i < groups.size(); i++) {
            ToolGroup group = groups.get(i);
            ToolGroupTreeNode child = new ToolGroupTreeNode(group, catalogRoot);
            catalogRoot.insert(child, catalogRoot.getChildCount());

            int index = catalogRoot.getIndex(child);
            indicies[i] = index;
            children[i] = child;
//System.out.println("Catalog bulk root adding group " + group.getName());
        }

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesInserted(treeEvent);

        // Add listeners later to make sure we don't end up with any weird
        // side effects of a fast building tree.
        for(int i = 0; i < groups.size(); i++) {
            ToolGroup group = groups.get(i);
            group.addToolGroupListener(this);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupRemoved(String name, ToolGroup group) {
        if(!name.equals(catalog.getName()))
            return;

        group.removeToolGroupListener(this);

        TreePath path = new TreePath(catalogRoot);
        Object[] children = new Object[1];
        int[] indicies = new int[1];

        ToolGroupTreeNode child = catalogRoot.getTreeNodeChild(group);
        int index = catalogRoot.getIndex(child);
        indicies[0] = index;
        children[0] = child;

        catalogRoot.remove(child);

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesRemoved(treeEvent);
    }

    /**
     * A group of tool groups have been removed.
     *
     * @param name The catalog name
     * @param groups The list of tool groups that have been removed
     */
    public void toolGroupsRemoved(String name, List<ToolGroup> groups) {
        if(!name.equals(catalog.getName()))
            return;

        TreePath path = new TreePath(catalogRoot);
        Object[] children = new Object[groups.size()];
        int[] indicies = new int[groups.size()];

        for(int i = 0; i < groups.size(); i++) {
            ToolGroup group = groups.get(i);

            group.removeToolGroupListener(this);

            ToolGroupTreeNode child = catalogRoot.getTreeNodeChild(group);

            int index = catalogRoot.getIndex(child);
            indicies[i] = index;
            children[i] = child;

            catalogRoot.remove(child);
        }

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesRemoved(treeEvent);
    }

    //----------------------------------------------------------
    // Methods defined by ToolGroupListener
    //----------------------------------------------------------

    /**
     * A tool has been added.  Batched additions will come through
     * the toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolAdded(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getSource();
        SimpleTool tool = (SimpleTool)evt.getChild();

//System.out.println("tool added " + group.getName() + " kid " + tool.getName());

        TreePath path = buildTreePath(group);

        Object[] children = new Object[1];
        int[] indicies = new int[1];
        int index;

        ToolGroupTreeNode parent = (ToolGroupTreeNode)path.getLastPathComponent();
        ToolGroupTreeNode child = new ToolGroupTreeNode(tool, parent);
        parent.insert(child, parent.getChildCount());

        index = getIndexOfChild(parent, child);
        indicies[0] = index;
        children[0] = child;

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesInserted(treeEvent);
    }

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupAdded(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getSource();
        ToolGroup tool = (ToolGroup)evt.getChild();

        TreePath path = buildTreePath(group);

//System.out.println("group added " + group.getName() + " kid " + tool.getName());

        Object[] children = new Object[1];
        int[] indicies = new int[1];
        int index;

        ToolGroupTreeNode parent =
            (ToolGroupTreeNode)path.getLastPathComponent();
        ToolGroupTreeNode child = new ToolGroupTreeNode(tool, parent);
        parent.insert(child, parent.getChildCount());

        index = getIndexOfChild(parent, child);
        indicies[0] = index;
        children[0] = child;

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesInserted(treeEvent);

        tool.addToolGroupListener(this);
    }

    /**
     * A tool has been removed. Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolRemoved(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getSource();
        SimpleTool tool = (SimpleTool)evt.getChild();

        TreePath path = buildTreePath(group);

        Object[] children = new Object[1];
        int[] indicies = new int[1];
        int index;

        ToolGroupTreeNode parent =
            (ToolGroupTreeNode)path.getLastPathComponent();
        ToolGroupTreeNode child = parent.getTreeNodeChild(tool);
        index = getIndexOfChild(parent, child);
        indicies[0] = index;
        children[0] = child;

        parent.remove(child);

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesRemoved(treeEvent);
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupRemoved(ToolGroupEvent evt) {

        ToolGroup group = (ToolGroup)evt.getSource();
        ToolGroup tool = (ToolGroup)evt.getChild();

        tool.removeToolGroupListener(this);

        TreePath path = buildTreePath(group);

        Object[] children = new Object[1];
        int[] indicies = new int[1];
        int index;

        ToolGroupTreeNode parent =
            (ToolGroupTreeNode)path.getLastPathComponent();
        ToolGroupTreeNode child = parent.getTreeNodeChild(tool);
        index = getIndexOfChild(parent, child);
        indicies[0] = index;
        children[0] = child;

        parent.remove(child);

        TreeModelEvent treeEvent =
            new TreeModelEvent(this, path, indicies, children);
        fireTreeNodesRemoved(treeEvent);
    }
    
    /**
     * A tool has been updated. NOT IMPLEMENTED YET.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolUpdated(ToolGroupEvent evt) {
    }
    
    /**
     * A tool group has been updated. NOT IMPLEMENTED YET.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupUpdated(ToolGroupEvent evt) {
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------


    /**
     * Register an error reporter with the engine so that any errors generated
     * by the loading of script code can be reported in a nice, pretty fashion.
     * Setting a value of null will clear the currently set reporter. If one
     * is already set, the new value replaces the old.
     *
     * @param reporter The instance to use or null
     */
    void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        // Reset the default only if we are not shutting down the system.
        if(reporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Build a TreePath object that represents the path to the given tool node.
     * The path uses the Node.getParentNode() method to build a reverse order
     * tree. With this, we then find the TreeNode corresponding to each DOM
     * Node and create a TreePath from that.
     *
     * @param dest The end node
     * @return A array of objects representing the given node
     */
    TreePath buildTreePath(ToolGroupChild dest) {
        ArrayList<ToolGroupChild> list = new ArrayList<ToolGroupChild>();

        ToolGroupChild node = dest;

        // Build a node list working from this node back to the root
        while(node != null) {
            list.add(node);
            node = node.getParent();
        }

        int i;
        int size = list.size();
        ToolGroupChild[] nodePath = new ToolGroupChild[size];

        for(i = 0; i < size; i++) {
            nodePath[i] = list.get(size - i - 1);
        }

        // Now, from the root, build the replacement tree map.
        ToolGroupTreeNode[] path = new ToolGroupTreeNode[size + 1];

        path[0] = catalogRoot;

        for(i = 1; i <= size; i++) {
            ToolGroupChild tempPath = nodePath[i - 1];

            if (tempPath == null ||
                path[i - 1] == null ||
                path[i - 1].getTreeNodeChild(tempPath) == null) {
                return null;
            }
            path[i] = path[i - 1].getTreeNodeChild(tempPath);
        }

        return new TreePath(path);
    }

    /**
     * Build children objects for the requested node type. If the node is an
     * attribute then do nothing. Listeners will be added to the root item but
     * not the children. This is to prevent odd mixups if the children have
     * listeners but have not yet had their children built. The viewable tree
     * would get very mixed up then
     *
     * @param root The root object to add children for
     */
    private void buildChildren(ToolGroupTreeNode root) {
        ToolGroupChild toolItem = root.getToolObject();

        if(toolItem instanceof ToolGroup) {
            ToolGroup tg = (ToolGroup)toolItem;
            List<ToolGroupChild> kids = tg.getChildren();

            for(int i = 0; i < kids.size(); i++) {
                ToolGroupChild child = kids.get(i);
                ToolGroupTreeNode kid =
                    new ToolGroupTreeNode(child, root);

                root.insert(kid, i);
            }
        }
    }

    /**
     * Send an event to the listeners instructing that the collection of nodes
     * has changed.
     *
     * @param evt The event to be sent
     */
    private void fireTreeNodesChanged(TreeModelEvent evt) {
        Iterator itr = listeners.iterator();
        TreeModelListener l;

        while(itr.hasNext()) {
            l = (TreeModelListener)itr.next();
            try {
                l.treeNodesChanged(evt);
            } catch(Exception e) {
                I18nManager intl_mgr = I18nManager.getManager();
                String msg = intl_mgr.getString(NODE_CHANGE_ERR_PROP) + l;

                errorReporter.errorReport(msg, e);
            }
        }
    }

    /**
     * Send an event to the listeners instructing that some nodes have been
     * added.
     *
     * @param evt The event to be sent
     */
    private void fireTreeNodesInserted(TreeModelEvent evt) {
        Iterator itr = listeners.iterator();
        TreeModelListener l;

        while(itr.hasNext()) {
            l = (TreeModelListener)itr.next();
            try {
                l.treeNodesInserted(evt);
            } catch(Exception e) {
                I18nManager intl_mgr = I18nManager.getManager();
                String msg = intl_mgr.getString(NODE_ADD_ERR_PROP) + l;

                errorReporter.errorReport(msg, e);
            }
        }
    }

    /**
     * Send an event to the listeners instructing that some nodes have been
     * removed.
     *
     * @param evt The event to be sent
     */
    private void fireTreeNodesRemoved(TreeModelEvent evt) {
        Iterator itr = listeners.iterator();
        TreeModelListener l;

        while(itr.hasNext()) {
            l = (TreeModelListener)itr.next();
            try {
                l.treeNodesRemoved(evt);
            } catch(Exception e) {
                I18nManager intl_mgr = I18nManager.getManager();
                String msg = intl_mgr.getString(NODE_REMOVE_ERR_PROP) + l;

                errorReporter.errorReport(msg, e);
            }
        }
    }

    /**
     * Send an event to the listeners instructing that the largescale structure
     * of nodes has changed.
     *
     * @param evt The event to be sent
     */
    private void fireTreeStructureChanged(TreeModelEvent evt) {
        Iterator itr = listeners.iterator();
        TreeModelListener l;

        while(itr.hasNext()) {
            l = (TreeModelListener)itr.next();
            try {
                l.treeStructureChanged(evt);
            } catch(Exception e) {
                I18nManager intl_mgr = I18nManager.getManager();
                String msg = intl_mgr.getString(STRUCTURE_CHANGE_ERR_PROP) + l;

                errorReporter.errorReport(msg, e);
            }
        }
    }
}
