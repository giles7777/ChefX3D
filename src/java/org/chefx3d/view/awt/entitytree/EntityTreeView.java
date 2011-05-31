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

package org.chefx3d.view.awt.entitytree;

// External Imports
import javax.swing.*;
import javax.swing.tree.*;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Enumeration;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

// Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.view.*;
import org.chefx3d.tool.*;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * The current model view in a nested tree structure
 *
 * @author Russell Dodds
 * @version $Revision: 1.45 $
 */
public class EntityTreeView extends JScrollPane
    implements
        View,
        ModelListener,
        TreeSelectionListener,
        EntityPropertyListener {

    /** The world model */
    private WorldModel model;

    /** List if Entities */
    protected Entity[] entities;

    /** The entity tree */
    private JTree entityTree;

    /** The entity tree */
    private DefaultTreeModel treeModel;

    /** A mapping of entities to nodes */
    private HashMap<Integer, ArrayList<EntityTreeNode>> entityNodes;

    /** The root tree node */
    private WorldTreeNode root;

    /** Are we in associateMode */
    private boolean associateMode;

    /** The ViewManager */
    private ViewManager vmanager;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** Flag to prevent making circular loops happen in selection */
    private boolean selectionInProgress;

    /**
     * View the WorldModel in a tree structure
     *
     * @param model The WorldModel that the tree is representing
     */
    public EntityTreeView(WorldModel model) {

        this.model = model;
        this.vmanager = ViewManager.getViewManager();

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        selectionInProgress = false;

        model.addModelListener(this);

        associateMode = false;
        entityNodes = new HashMap<Integer, ArrayList<EntityTreeNode>>();

        buildTreePanel();

        vmanager.addView(this);

    }

    // ----------------------------------------------------------
    // Methods required by View
    // ----------------------------------------------------------

    public void shutdown(){
        // ignored
    }

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        // ignore
    }

    /**
     * Go into associate mode. The next mouse click will perform
     * a property update
     *
     * @param validTools A list of the valid tools. null string will be all
     *        valid. empty string will be none.
     * @param propertyGroup The grouping the property is a part of
     * @param propertyName The name of the property being associated
     */
    public void enableAssociateMode(
            String[] validTools,
            String propertyGroup,
            String propertyName) {
        associateMode = true;
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
        associateMode = false;
    }


    /**
     * Get the viewID. This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID() {
        // TODO: What to do here
        return -1;
    }

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode) {
        // ignore
    }

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode) {
        // ignore
    }

    /**
     * Return the property data in the required format
     */
    public Object getComponent() {

        return this;

    }

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {
        return null;
        // ignore
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        // ignore
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    // ----------------------------------------------------------
    // Methods required by ModelListener
    // ----------------------------------------------------------

    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The unique entityID assigned by the view
     */
    public void entityAdded(boolean local, Entity entity) {

//System.out.println("EntityTreeView.entityAdded()");

        entity.addEntityPropertyListener(this);

        if (entity.getType() == Entity.TYPE_WORLD) {

            // define the root
            treeModel.nodeStructureChanged(root);


        } else {

            // make sure it is not a duplicate add from the networking
            if (!entityNodes.containsKey(entity.getEntityID())) {

                EntityTreeNode childNode = new EntityTreeNode(entity, root);
                childNode.setErrorReporter(errorReporter);

                treeModel.insertNodeInto(childNode, root, root.getChildCount());

                // add to the entity-node map
                updateEntityNodeMap(childNode.getEntity(), childNode);

            }
        }
    }

    /**
     * An entity was removed.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity being removed
     */
    public void entityRemoved(boolean local, Entity entity) {

        // remove the property listener
        entity.addEntityPropertyListener(this);

        if (entity.getType() != Entity.TYPE_WORLD) {

            ArrayList<EntityTreeNode> nodeList =
                getEntityNodes(entity.getEntityID());

            if (nodeList == null)
                return;

            for (int i = 0; i < nodeList.size(); i++) {

                EntityTreeNode childNode = nodeList.get(i);
                try {
                    treeModel.removeNodeFromParent(childNode);
                } catch (Exception ex) {
                    // TODO: need to understand why the treeModel sometimes
                    // does not contain the child node being deleted
                }


            }

            // remove from the entity-node map
            entityNodes.remove(entity.getEntityID());

         }
    }

    /**
     * The master view has changed.
     *
     * @param local Was this action initiated from the local UI
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
        // ignore
    }

    /**
     * The entity was selected.
     *
     * @param selection The list of selected entities. The last one is the
     *        latest.
     */
//    public void selectionChangedDep(List<Selection> selection) {
//
//        //System.out.println("EntityTreeView.selectionChanged, associateMode: " + associateMode);
//        //System.out.println("    hashCode: " + this.hashCode());
//
//        if (associateMode)
//            return;
//
//        selectionInProgress = true;
//
//        // TODO: deal with multiple selected entities
//
//        // Highlight all the selected entity nodes
//        if (selection.size() < 1) {
//
//            // If nothing is selected then clear the selection list
//            entityTree.setSelectionPath(null);
//
//        } else {
//
//            // get the currently selected entity
//            Selection selectedEntity = selection.get(0);
//            int entityID = selectedEntity.getEntityID();
//            int vertexID = selectedEntity.getVertexID();
//
//            if (entityID >= 0) {
//
//                // traverse the model, looking for ID
//                ArrayList<EntityTreeNode> entityNodes = getEntityNodes(entityID);
//
//                if ((entityNodes != null) && (entityNodes.size() > 0)) {
//
//                    TreePath[] selectionPaths = new TreePath[entityNodes
//                        .size()];
//
//                    for (int i = 0; i < entityNodes.size(); i++) {
//
//                        TreeNode treeNode = entityNodes.get(i);
//
//                        if (vertexID != -1) {
//
//                            selectionPaths[i] = entityTree.getSelectionPath();
//
//                        } else {
//
//                            selectionPaths[i] = getTreePath(treeNode);
//
//                        }
//
//                        if (treeNode.getParent() == null) {
//
//                            // scroll to the actual instance of the entity
//                            // (not associate nodes)
//                            entityTree.scrollPathToVisible(selectionPaths[i]);
//
//                        }
//
//                    }
//                    // highlight the entity throughout the tree
//                    entityTree.setSelectionPaths(selectionPaths);
//
//                } else {
//                    // If nothing is selected set to the location root
//                    entityTree.setSelectionPath(getTreePath(root));
//                }
//
//            } else {
//
//                // If nothing is selected set to the location root
//                if (root != null) {
//                    entityTree.setSelectionPath(getTreePath(root));
//                } else {
//                    // If nothing is selected then clear the selection list
//                    entityTree.setSelectionPath(null);
//                }
//
//            }
//        }
//
//        selectionInProgress = false;
//    }

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
        // ignore
    }

    // ----------------------------------------------------------
    // Methods required by EntityPropertyListener interface
    // ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyAdded(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was removed.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(int entityID,
            String propertySheet, String propertyName) {
        // ignored
    }

    /**
     * A property was updated.
     *
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     * @param ongoing Is this an ongoing change or the final value?
     */
    public void propertyUpdated(
            int entityID,
            String propertySheet,
            String propertyName, boolean ongoig) {
/*
System.out.println("EntityTreeView.propertyUpdated");
System.out.println("    entityID: " + entityID);
System.out.println("    propertySheet: " + propertySheet);
System.out.println("    propertyName: " + propertyName);
System.out.println("    this: " + this);
new Exception().printStackTrace();
*/

        Entity entity = model.getEntity(entityID);

        if(entity != null){

            Object property = entity.getProperty(propertySheet, propertyName);

            if (property instanceof AssociateProperty) {

                AssociateProperty associate = (AssociateProperty)property;

                Object value = associate.getValue();
                if (value == null)
                    return;

                Entity associatedEntity = (Entity)value;

                // Find the parent EntityTreeNode, this is the list
                // of all nodes that have this as an associate
                ArrayList<EntityTreeNode> entityNodes = getEntityNodes(entity.getEntityID());

                for (int i = 0; i < entityNodes.size(); i++) {

                    // Get the entity node
                    EntityTreeNode parentNode = entityNodes.get(i);

                    // remove all the children
                    int len = parentNode.getChildCount() - 1;
                    for (int j = len; j >= 0; j--) {
                        EntityTreeNode childNode = (EntityTreeNode)parentNode.getChildAt(j);
                        treeModel.removeNodeFromParent(childNode);
                    }

                    // now add back all possible associations
                    List<EntityProperty> propertyList = entity.getProperties();
                    for (int j = 0; j < propertyList.size(); j++) {

                        EntityProperty prop = propertyList.get(j);
                        if (prop.propertyValue instanceof AssociateProperty) {

                            associate = (AssociateProperty)prop.propertyValue;

                            value = associate.getValue();
                            if (value == null)
                                continue;

                            associatedEntity = (Entity)value;

                            if (associatedEntity == null)
                                continue;

                            EntityTreeNode childNode =
                                new EntityTreeNode(associatedEntity, parentNode);

                            treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());

                            // add to the entity-node map
                            updateEntityNodeMap(associatedEntity, childNode);

                        }

                    }

                    // expand the tree to include the new node
                    entityTree.expandPath(getTreePath(parentNode));

                }

            }
        }
    }

    /**
     * Multiple properties were updated.  This is a single call
     * back for multiple property updates that are grouped.
     *
     * @param properties - This contains a list of updates as well
     *        as a name and sheet to be used as the call back.
     */
    public void propertiesUpdated(List<EntityProperty> properties) {
        // ignored
    }

//    // ----------------------------------------------------------
//    // Methods required by EntityChangeListener
//    // ----------------------------------------------------------
//
//    /**
//     * A segment was added to the sequence.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param segmentID The unique segmentID
//     * @param startVertexID The starting vertexID
//     * @param endVertexID The starting vertexID
//     */
//    public void segmentAdded(boolean local, int entityID,
//            int segmentID, int startVertexID, int endVertexID) {
//    }
//
//    /**
//     * A segment was added to the sequence.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param segmentID The unique segmentID
//     * @param vertexID The starting vertexID
//     */
//    public void segmentSplit(boolean local, int entityID,
//            int segmentID, int vertexID) {
//
//        // ignored
//    }
//
//    /**
//     * A vertex was removed.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param segmentID The segment removed
//     */
//    public void segmentRemoved(boolean local, int entityID,
//            int segmentID) {
//    }
//
//    /**
//     * A vertex was added to an entity.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param vertexID The unique vertexID assigned by the view
//     * @param pos The x position in world coordinates
//     */
//    public void segmentVertexAdded(boolean local, int entityID, int vertexID,
//        double[] position) {
//
//        // Find the parent EntityTreeNode
//        ArrayList<EntityTreeNode> entityNodes = getEntityNodes(entityID);
//
////System.out.println("EntityTreeView.segmentVertexAdded()");
////System.out.println("    entityNodes.size(): " + entityNodes.size());
//
//        for (int i = 0; i < entityNodes.size(); i++) {
//
//            // Get the entity node
//            EntityTreeNode parentNode = entityNodes.get(i);
//
////System.out.println("    parentNode children count: " + parentNode.getChildCount());
//
//            // make sure it doesn't already exist
//            Enumeration children = parentNode.children();
//            while (children.hasMoreElements()) {
//                VertexTreeNode childNode = (VertexTreeNode) children.nextElement();
//                if (childNode.getVertexID() == vertexID) {
//                    return;
//                }
//            }
//
//            // create the child node
////System.out.println("    parentNode name: " + parentNode.getEntity().getName());
//
//            Entity item = model.getEntity(entityID);
//
////System.out.println("    item name: " + item.getName());
//
//
//            VertexTreeNode childNode = new VertexTreeNode(item, vertexID, parentNode);
//            childNode.setErrorReporter(errorReporter);
//
//            treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());
//
//            // add to the entity-node map
//            //updateEntityNodeMap(childNode.getEntity(), childNode);
//
//            // expand the tree to include the new node
//            entityTree.expandPath(getTreePath(parentNode));
//
//        }
//
//    }
//
//    /**
//     * A vertex was updated.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param vertexID The unique vertexID assigned by the view
//     * @param pos The x position in world coordinates
//     */
//    public void segmentVertexUpdated(boolean local, int entityID, int vertexID,
//        String propertyName, String propertySheet, String propertyValue) {
//        // ignore
//    }
//
//    /**
//     * A vertex was moved.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param vertexID The unique vertexID assigned by the view
//     * @param pos The x position in world coordinates
//     */
//    public void segmentVertexMoved(boolean local, int entityID, int vertexID,
//        double[] position) {
//
//
//        // Find the parent EntityTreeNode
//        ArrayList<EntityTreeNode> entityNodes = getEntityNodes(entityID);
//
//        for (int i = 0; i < entityNodes.size(); i++) {
//
//            // Get the entity node
//            EntityTreeNode parentNode = entityNodes.get(i);
//
//            treeModel.reload(parentNode);
//
//            // add to the entity-node map
//            //updateEntityNodeMap(childNode.getEntity(), childNode);
//
//            // expand the tree to include the new node
//            entityTree.expandPath(getTreePath(parentNode));
//
//        }
//
//    }
//
//    /**
//     * A vertex was removed.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID The unique entityID assigned by the view
//     * @param vertexID The unique vertexID assigned by the view
//     * @param pos The x position in world coordinates
//     */
//    public void segmentVertexRemoved(boolean local, int entityID,
//        int vertexID) {
//
//        // Find the parent EntityTreeNode
//        ArrayList<EntityTreeNode> entityNodes = getEntityNodes(entityID);
//
//        for (int i = 0; i < entityNodes.size(); i++) {
//
//            // Get the entity node
//            EntityTreeNode parentNode = entityNodes.get(i);
//
//            // look for the child node
//            for (int j = 0; j < parentNode.getChildCount(); j++) {
//                VertexTreeNode childNode  = (VertexTreeNode) parentNode.getChildAt(j);
//
//                if (vertexID == childNode.getVertexID()) {
//                    treeModel.removeNodeFromParent(childNode);
//                }
//
//            }
//
//            // expand the tree to include the new node
//            entityTree.expandPath(getTreePath(parentNode));
//
//        }
//
//    }
//
//    /**
//     * The entity moved.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID the id
//     * @param position The position in world coordinates(meters, Y-UP, X3D
//     *        System).
//     */
//    public void entityMoved(boolean local, int entityID, double[] position) {
//        // ignore
//    }
//
//    /**
//     * The entity was scaled.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param entityID the id
//     * @param scale The scaling factors(x,y,z)
//     */
//    public void entityScaled(boolean local, int entityID, float[] scale) {
//        // ignore
//    }
//
//    /**
//     * The entity was rotated.
//     *
//     * @param local Was this action initiated from the local UI
//     * @param rotation The rotation(axis + angle in radians)
//     */
//    public void entityRotated(boolean local, int entityID, float[] rotation) {
//        // ignore
//    }

    // ----------------------------------------------------------
    // Methods required by the TreeSelectionListener
    // ----------------------------------------------------------

    /**
     * Callback from the tree to indicate a visual selection has
     * changed.
     */
    public void valueChanged(TreeSelectionEvent e) {

        if (!e.isAddedPath() || selectionInProgress) {
            // ignore deselection events
            return;
        }

        MutableTreeNode node = (MutableTreeNode) e.getPath()
            .getLastPathComponent();

        if (node instanceof EntityTreeNode) {

            Entity entity = ((EntityTreeNode) node).getEntity();

            // send the selecting command
            SelectEntityCommand cmdSelect =
                new SelectEntityCommand(model, entity, true);
            model.applyCommand(cmdSelect);

        } else if (node instanceof VertexTreeNode) {

            int entityID = ((VertexTreeNode) node).getEntityID();
            int vertexID = ((VertexTreeNode) node).getVertexID();

            // create the selection list
            Selection sel = new Selection(entityID, -1, vertexID);
            List<Selection> list = new ArrayList<Selection>(1);
            list.add(sel);

            // send the selecting command
//            SelectEntityCommand cmdSelect =
//                new SelectEntityCommand(model, list);
//            model.applyCommand(cmdSelect);


        } else if (node instanceof WorldTreeNode) {

            Entity entity = ((WorldTreeNode) node).getEntity();

            if (entity != null) {

                // create the selection list
                Selection sel = new Selection(entity.getEntityID(), -1, -1);
                List<Selection> list = new ArrayList<Selection>(1);
                list.add(sel);

                // send the selecting command
                SelectEntityCommand cmdSelect =
                    new SelectEntityCommand(model, entity, true);
                model.applyCommand(cmdSelect);

            }
        }
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Build the TreePath to use to open and scroll the JTree if needed
     *
     * @param treeNode the node to open
     */
    private TreePath getTreePath(TreeNode treeNode) {

        ArrayList<TreeNode> treePath = new ArrayList<TreeNode>();

        treePath.add(treeNode);

        while (treeNode.getParent() != null) {
            treeNode = treeNode.getParent();
            treePath.add(0, treeNode);
        }

        TreeNode[] path = new TreeNode[treePath.size()];
        treePath.toArray(path);

        return new TreePath(path);
    }

    /**
     * Add entry to the map for later lookup
     *
     * @param entity
     * @param node
     */
    private void updateEntityNodeMap(Entity entity, EntityTreeNode node) {

        int entityID = entity.getEntityID();

        if (entityNodes.containsKey(entityID)) {

            ArrayList<EntityTreeNode> nodes = entityNodes.get(entityID);
            nodes.add(node);

        } else {

            ArrayList<EntityTreeNode> newNodes = new ArrayList<EntityTreeNode>();
            newNodes.add(node);

            entityNodes.put(entityID, newNodes);
        }
    }

    /**
     * Returns an ArrayList all nodes with the ID provided
     *
     * @return list of <code>EntityTreeNode</code>
     */
    public ArrayList<EntityTreeNode> getEntityNodes(int entityID) {

        return entityNodes.get(entityID);
    }

    /**
     * Build the JTree UI components
     *
     */
    private void buildTreePanel() {

        // define the root
        root = new WorldTreeNode(model);
        root.setErrorReporter(errorReporter);

        // define the model
        treeModel = new DefaultTreeModel(root);

        // create the JTree
        entityTree = new JTree(treeModel);

        EntityTreeCellRenderer cellRenderer = new EntityTreeCellRenderer();
        cellRenderer.setErrorReporter(errorReporter);
        entityTree.setCellRenderer(cellRenderer);

        entityTree.setRootVisible(true);
        entityTree.setShowsRootHandles(false);

        // Add the selection listener so we know when something has been
        // selected
        entityTree.addTreeSelectionListener(this);

        // only allow a single item to be selected for now
        entityTree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        entityTree.setShowsRootHandles(true);

        // Expand the Tree
        entityTree.expandPath(new TreePath(entityTree.getModel().getRoot()));

        // fianlly, add the JTree to the panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(entityTree, BorderLayout.CENTER);

        JViewport view = this.getViewport();
        view.add(panel);
    }
}
