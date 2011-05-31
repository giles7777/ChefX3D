/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
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
import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

// Local imports
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.CatalogListener;
import org.chefx3d.catalog.CatalogManagerListener;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.Entity;
import org.chefx3d.view.ViewManager;

/**
 * This toolbar will use multiple trees to allow tool selection. A ToolGroup
 * that contains at least one non model will be a seperate tree. Each tree will
 * also have a global rollup.
 *
 * Direction will be ignored by this toolbar. It will always be vertical.
 *
 * An example might be:
 *
 * Locations: Washington Seattle Yakama Oregon Portland
 *
 * Barriers Water Fence
 *
 * Models Primitives Box Sphere
 *
 * @author Alan Hudson
 * @version $Revision: 1.19 $
 */
class MultiTreeToolBar
    implements ToolBar, TreeSelectionListener, CatalogManagerListener, CatalogListener {
    /** The panel to place the toolbar */
    private JSplitPane toolbar;

    // private JPanel toolbar;

    /** The panel to place the toolsTree */
    private JScrollPane toolsPanel;

    /** The panel to place the modelsTree */
    private JScrollPane modelsPanel;

    /** The world model */
    private WorldModel model;

    /** The view manager */
    private ViewManager viewManager;

    /** The catalog manager */
    private CatalogManager catalogManager;

    /** A map of tools to popup menus */
    // private HashMap<String, JPopupMenu> toolsMap;
    /** A map of MenuItems to ToolInstances */
    // private HashMap<JMenuItem, Tool> miMap;
    /** Should we collapse tools with one item */
    // private boolean collapse;
    /** The direction, horizontal or vertical */
    // private int direction;
    /** The sections map by type(type, ArrayList) */
    private HashMap<String, ArrayList> sections;

    /** The tools tree */
    private JTree toolsTree;

    /** The models tree */
    private JTree modelsTree;

    /** The top level node for the tools section */
    private Vector<Vector> toolsTop;

    /** The top level nodes for the models section */
    private Vector modelsTop;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    public MultiTreeToolBar(
    		WorldModel model, 
    		CatalogManager catalogManager, 
    		int direction,
    		boolean collapseSingletons) {

        // toolsMap = new HashMap<String, JPopupMenu>();
        // miMap = new HashMap<JMenuItem, Tool>();

        // this.collapse = collapseSingletons;
        // this.direction = direction;
        this.model = model;
        viewManager = ViewManager.getViewManager();
        this.catalogManager = catalogManager;
        catalogManager.addCatalogManagerListener(this);

        sections = new HashMap<String, ArrayList>();
        toolsTop = new Vector<Vector>();
        modelsTop = new NamedVector("Models");

        toolbar = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        toolbar.setResizeWeight(0.3);

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        ToolBarManager.getToolBarManager().addToolBar(this);
    }

    /**
     * Get the component used to render this.
     *
     * @return The component
     */
    public Object getComponent() {
        return toolbar;
    }

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        // RUSS: Finish Implement
        if (tool == null) {
            toolsTree.clearSelection();
            modelsTree.clearSelection();
        } else {
            System.out.println("Unhandled case in Toolbar.setTool");
        }
    }

    //----------------------------------------------------------
    // Methods defined by TreeSelectionListener
    //----------------------------------------------------------

    public void valueChanged(TreeSelectionEvent e) {
        Object source = e.getSource();

        if (!e.isAddedPath()) {
            // ignore deselection events
            return;
        }

        if (source == modelsTree) {
            // deselect toolsTree

            toolsTree.clearSelection();
        } else if (source == toolsTree) {
            // deselect modelsTree

            modelsTree.clearSelection();
        } else {
            errorReporter.messageReport("ERROR: Unknown source for valueChanged in: "
                    + this);
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath()
                .getLastPathComponent();

        Object nodeInfo = node.getUserObject();

        if (nodeInfo instanceof ToolWrapper) {
            ToolWrapper tw = (ToolWrapper) nodeInfo;
            viewManager.setTool(tw.getTool());
        }
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
        rebuild();
    }

    /**
     * A group of tool groups have been added.
     *
     * @param name The catalog name
     * @param groups The list of tool groups added
     */
    public void toolGroupsAdded(String name, List<ToolGroup> groups) {
        rebuild();
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupRemoved(String name, ToolGroup group) {
        rebuild();
    }

    /**
     * A group of tool groups have been removed.
     *
     * @param name The catalog name
     * @param groups The list of tool groups that have been removed
     */
    public void toolGroupsRemoved(String name, List<ToolGroup> groups) {
        rebuild();
    }

    //----------------------------------------------------------
    // Methods defined by CatalogManagerListener
    //----------------------------------------------------------

    /**
     * A catalog has been added.
     *
     * @param catalog
     */
    public void catalogAdded(Catalog catalog) {
        catalog.addCatalogListener(this);
    }

    /**
     * A catalog has been removed.
     *
     * @param catalog
     */
    public void catalogRemoved(Catalog catalog) {
        catalog.removeCatalogListener(this);
    }


    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

    /**
     * Rebuild from the current set of tools
     */
    public void rebuild() {
        List<ToolGroup> tools = new ArrayList<ToolGroup>();
        List<Catalog> catalogs = catalogManager.getCatalogs();
        Iterator<Catalog> itr = catalogs.iterator();

        while(itr.hasNext()) {
            Catalog cat = itr.next();
            tools.addAll(cat.getToolGroups());
        }

        ToolGroup td;
        int len = tools.size();

        List children;
        SimpleTool tool;
        ToolGroup childGroup;

        String name;

        modelsTop.clear();
        toolsTop.clear();

        String MODELS = "CHEFX3D_MODELS";
        String WORLDS = "CHEFX3D_WORLDS";

        for (int i = 0; i < len; i++) {
            td = tools.get(i);
            children = td.getChildren();

            name = td.getName();
//System.out.println(name + " len: " + children.size());
            // errorReporter.messageReport(name);

            boolean allModels = true;
            boolean allWorlds = true;

            int toolType;

            for (int j = 0; j < children.size(); j++) {
                Object o = children.get(j);

                if (o instanceof SimpleTool) {
                    tool = (SimpleTool) o;
                    toolType = tool.getToolType();
                    //errorReporter.messageReport(" Tool1: " + tool.getName());

                    if (toolType != Entity.TYPE_MODEL ||
                    		toolType != Entity.TYPE_MODEL_WITH_ZONES) {
                        allModels = false;
                    }
                    if (toolType != Entity.TYPE_WORLD) {
                        allWorlds = false;
                    }
                } else if (o instanceof ToolGroup) {
                    childGroup = (ToolGroup) o;

                    List sub_children = childGroup.getChildren();

                    for (int k = 0; k < sub_children.size(); k++) {
                        if (sub_children.get(k) instanceof SimpleTool) {
                            tool = (SimpleTool) sub_children.get(k);
                            //errorReporter.messageReport(" Tool2: " + tool.getName());

                            toolType = tool.getToolType();

                            if (toolType != Entity.TYPE_MODEL ||
                            		toolType != Entity.TYPE_MODEL_WITH_ZONES) {
                                allModels = false;
                            }
                            if (toolType != Entity.TYPE_WORLD) {
                                allWorlds = false;
                            }
                        }
                    }
                } else {
                    errorReporter.messageReport("Invalid item in Tools: " + o);
                }
            }

            if (allModels) {
                ArrayList models = (ArrayList) sections.get(MODELS);

                if (models == null) {
                    models = new ArrayList();
                    sections.put(MODELS, models);
                }

                // errorReporter.messageReport("Lost title: " + name);
                addSection(modelsTop, name, children);

                // models.addAll(children);
            } else if (allWorlds) {
                ArrayList list = (ArrayList) sections.get(WORLDS);

                if (list == null) {
                    list = new ArrayList();
                    sections.put(WORLDS, list);
                }

                list.addAll(children);
            } else {
                ArrayList list = (ArrayList) sections.get(name);

                if (list == null) {
                    list = new ArrayList();
                    sections.put(name, list);
                }

                list.addAll(children);
            }
        }

        // ArrayList models = (ArrayList) sections.get(MODELS);
        sections.remove(MODELS);
        ArrayList worlds = (ArrayList) sections.get(WORLDS);
        sections.remove(WORLDS);

        addSection(toolsTop, "Locations", worlds);
        // addSection(modelsTop, "Models", models);

        Iterator itr2 = sections.entrySet().iterator();
        Map.Entry entry;

        while (itr2.hasNext()) {
            entry = (Map.Entry) itr2.next();
            addSection(toolsTop, (String) entry.getKey(), (List) entry
                    .getValue());
        }

        sections.clear();

        if (toolsTree != null) {
            toolsTree.removeTreeSelectionListener(this);

            toolsTree = new JTree(toolsTop);
            toolsTree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);
            toolsTree.addTreeSelectionListener(this);
            toolsPanel = new JScrollPane(toolsTree);
            toolbar.setTopComponent(toolsPanel);
            // toolbar.add(toolsPanel, BorderLayout.NORTH);

            modelsTree.removeTreeSelectionListener(this);
            modelsTree = new JTree(modelsTop);
            modelsTree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) modelsTree
                    .getModel().getRoot();
            root.setUserObject("Models");
            /*
             * if (modelsTree.getRowCount() > 0) modelsTree.expandRow(0);
             */
            modelsTree.addTreeSelectionListener(this);

            modelsPanel = new JScrollPane(modelsTree);
            toolbar.setBottomComponent(modelsPanel);
            // toolbar.add(modelsPanel, BorderLayout.SOUTH);
        } else {
            toolsTree = new JTree(toolsTop);
            toolsTree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);
            toolsTree.addTreeSelectionListener(this);
            toolsPanel = new JScrollPane(toolsTree);
            toolbar.setTopComponent(toolsPanel);
            // toolbar.add(toolsPanel, BorderLayout.NORTH);

            modelsTree = new JTree(modelsTop);
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) modelsTree
                    .getModel().getRoot();
            root.setUserObject("Models");

            modelsTree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);
            modelsTree.addTreeSelectionListener(this);
            /*
             * if (modelsTree.getRowCount() > 0) modelsTree.expandRow(0);
             */
            modelsPanel = new JScrollPane(modelsTree);
            toolbar.setBottomComponent(modelsPanel);
            // toolbar.add(modelsPanel, BorderLayout.SOUTH);

        }

        modelsTree.setRootVisible(true);
        // Object user_object = root.getUserObject();

        // errorReporter.messageReport("root: " + root + " class: " + root.getClass());
        // errorReporter.messageReport("user: " + user_object + " class: " +
        // user_object.getClass());
        Icon fouoIcon = new FOUOIcon2();
        toolsTree.setCellRenderer(new MyRenderer(fouoIcon));
    }

    /**
     * Add a section to the tree.
     *
     * @param section The section name
     * @param worlds The list of models
     */
    private void addSection(Vector top, String section, List worlds) {
        SimpleTool tool;
        boolean found = false;

        if (worlds != null) {
            Vector group = null;

            if (top.size() > 0) {
                NamedVector nv = new NamedVector(section);

                int idx = top.indexOf(nv);

                if (idx != -1) {
                    group = (Vector) top.get(idx);
                    found = true;
                }
            }

            if (group == null) {
                group = new NamedVector(section);
            }
            Iterator itr = worlds.iterator();
            Object t;
            ToolGroup tg;

            while (itr.hasNext()) {
                t = itr.next();

                if (t instanceof SimpleTool) {
                    tool = (SimpleTool) t;
                    group.add(new ToolWrapper(tool));
                } else {
                    tg = (ToolGroup) t;
                    List sub_children = tg.getChildren();
                    Vector sub_group = new NamedVector(tg.getName());

                    for (int k = 0; k < sub_children.size(); k++) {
                        if( sub_children.get(k) instanceof SimpleTool) {
                            tool = (SimpleTool) sub_children.get(k);
                            // sub_group.add(tool.getName());
                            sub_group.add(new ToolWrapper(tool));
                        }
                    }

                    group.add(sub_group);
                }
            }

            if (!found)
                top.add(group);
        } else {
            errorReporter.messageReport("Nothing to add for section: " + section);
        }
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

}

class FOUOIcon2 implements Icon, SwingConstants {
    private int width = 9;

    private int height = 18;

    private int[] xPoints = new int[4];

    private int[] yPoints = new int[4];

    public FOUOIcon2() {
        xPoints[0] = 0;
        yPoints[0] = -1;
        xPoints[1] = 0;
        yPoints[1] = height;
        xPoints[2] = width;
        yPoints[2] = height / 2;
        xPoints[3] = width;
        yPoints[3] = height / 2 - 1;
    }

    public int getIconHeight() {
        return height;
    }

    public int getIconWidth() {
        return width;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (c.isEnabled()) {
            g.setColor(Color.red);
        } else {
            g.setColor(Color.gray);
        }

        g.translate(x, y);
        g.fillPolygon(xPoints, yPoints, xPoints.length);
        g.translate(-x, -y); // Restore graphics object
        g.setColor(c.getForeground());
    }
}

// TODO: Move to seperate class once selection is handled, name might change
class NamedVector<E> extends Vector<E> {

    /** version id */
    private static final long serialVersionUID = 1L;

    private String name;

    public NamedVector(String name) {
        // super = new Vector<Object>();
        this.name = name;
    }

    public NamedVector(String name, E elements[]) {
        this.name = name;
        for (int i = 0, n = elements.length; i < n; i++) {
            add(elements[i]);
        }
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    // ---------------------------------------------------------------
    // Methods defined by Object
    // ---------------------------------------------------------------

    /**
     * Compare this object for equality to the given object.
     *
     * @param o The object to be compared
     * @return True if these represent the same values
     */
    public boolean equals(Object o) {
        if (!(o instanceof NamedVector))
            return false;
        else {
            NamedVector o2 = (NamedVector) o;

            if (name.equals(o2.getName())) {
                return true;
            } else {
                return false;
            }
        }

    }

    /**
     * Compare this object for order to the given object.
     *
     * @param o The object to be compared
     * @return zero if equals, negative less, positive greater
     */
    public int compareTo(Object o) throws ClassCastException {
        NamedVector nv = (NamedVector) o;

        return name.compareTo(nv.getName());
    }
}

class MyRenderer extends DefaultTreeCellRenderer {
    Icon tutorialIcon;

    /** version id */
    private static final long serialVersionUID = 1L;

    public MyRenderer(Icon icon) {
        tutorialIcon = icon;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                row, hasFocus);

        if (leaf && isFOUO(value)) {
            setIcon(tutorialIcon);
            setToolTipText("This book is in the Tutorial series.");
        } else {
            setToolTipText(null); // no tool tip
        }

        return this;
    }

    protected boolean isFOUO(Object value) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

        Object o = node.getUserObject();

        if (!(o instanceof ToolWrapper))
            return false;

        ToolWrapper nodeInfo = (ToolWrapper) o;

        SimpleTool tool = nodeInfo.getTool();

        return false;
    }
}
