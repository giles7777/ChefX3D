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
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.GridLayout;

// Local imports
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogListener;
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.CatalogManagerListener;

import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.model.WorldModel;
import org.chefx3d.view.ViewManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * This class will create a toolbar of authoring icons. An icon will be either a
 * single tool, or a drop down box of multiple tools.
 *
 * Possible tool actions: Change the underlying world model Select a 3D model to
 * place Author a sequence of points
 *
 * @author Alan Hudson
 * @version $Revision: 1.15 $
 */
public class DefaultToolBar
    implements ToolBar, ActionListener, CatalogManagerListener, CatalogListener {

    private JPanel toolbar;

    /** The world model */
    private WorldModel model;

    /** The view manager */
    private ViewManager viewManager;

    /** The catalog manager */
    private CatalogManager catalogManager;

    /** A map of tools to popup menus */
    private HashMap<String, JPopupMenu> toolsMap;

    /** A map of MenuItems to ToolInstances */
    private HashMap<JMenuItem, Tool> miMap;

    /** The current popup */
    private JPopupMenu popup;

    /** Should we collapse tools with one item */
    private boolean collapse;

    /** The direction, horizontal or vertical */
    private int direction;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The name of the catalog we are showing */
    private String catalogName;

    public DefaultToolBar(
    		WorldModel model, 
    		CatalogManager catalogManager, 
    		int direction, 
    		boolean collapseSingletons) {

        collapse = collapseSingletons;

        toolsMap = new HashMap<String, JPopupMenu>();
        miMap = new HashMap<JMenuItem, Tool>();
        toolbar = new JPanel();
        this.direction = direction;
        this.model = model;
        this.catalogManager = catalogManager;
        
        viewManager = ViewManager.getViewManager();

        catalogManager.addCatalogManagerListener(this);

        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    //----------------------------------------------------------
    // Methods defined by ActionListener
    //----------------------------------------------------------

    /**
     * Some random action event occurred on a menu or button. Process it and
     * make the tool active now.
     *
     * @param e The event that caused this method to be called
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem mi = (JMenuItem) e.getSource();
        SimpleTool tool = (SimpleTool) miMap.get(mi);

        viewManager.setTool(tool);
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
    // Methods required by the CatalogManagerListener interface
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
     *
     * @param catalogName
     */
    public void setCatalog(String catalogName) {
        this.catalogName = catalogName;
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
        // RUSS: Implement
    }

    /**
     * Add a tool.
     *
     * @param tool The new tool
     * @param level A user provided level used for icon/color mapping
     */
    public void addTool(ToolGroup tool, int level) {
        errorReporter.messageReport("addTool not implemented yet");
    }

    /**
     * Remove a tool.
     *
     * @param tool The tool to remove
     */
    public void removeTool(ToolGroup tool) {
        errorReporter.messageReport("removeTool not implemented yet");
    }

    /**
     * Rebuild from the current list of tools
     */
    private void rebuild() {

        List<ToolGroup> tools;
        if (catalogName == null) {
            tools = new ArrayList<ToolGroup>();
            List<Catalog> catalogs = catalogManager.getCatalogs();
            Iterator<Catalog> itr = catalogs.iterator();

            while(itr.hasNext()) {
                Catalog cat = itr.next();
                tools.addAll(cat.getToolGroups());
            }
        } else {
            Catalog cat = catalogManager.getCatalog(catalogName);
            tools = cat.getAllGroupsFlattened();
        }

        ToolGroup td;
        int len = tools.size();

        List children;
        SimpleTool tool;
        ToolGroup childGroup;

        String name;
        JMenu submenu;
        JMenuItem menuItem;

        int level = 0;
        for (int i = 0; i < len; i++) {
            td = tools.get(i);
            children = td.getChildren();

            name = td.getName();
            popup = (JPopupMenu) toolsMap.get(name);

            if (popup == null)
                popup = new JPopupMenu();

            for (int j = 0; j < children.size(); j++) {
                Object o = children.get(j);

                if (o instanceof SimpleTool) {
                    tool = (SimpleTool) o;
                    if (collapse) {
                        if (level == 0)
                            menuItem = new JMenuItem(tool.getName());
                        else
                            menuItem = new JMenuItem(tool.getName(),
                                    new FOUOIcon());

                        miMap.put(menuItem, tool);
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    } else {
                        submenu = new JMenu(tool.getName());

                        if (level == 0)
                            menuItem = new JMenuItem(tool.getName());
                        else
                            menuItem = new JMenuItem(tool.getName(),
                                    new FOUOIcon());

                        miMap.put(menuItem, tool);
                        menuItem.addActionListener(this);
                        submenu.add(menuItem);
                        popup.add(submenu);
                    }
                } else if (o instanceof ToolGroup) {
                    childGroup = (ToolGroup) o;

                    submenu = new JMenu(childGroup.getName());
                    List sub_children = childGroup.getChildren();

                    for (int k = 0; k < sub_children.size(); k++) {
                        if (sub_children.get(k) instanceof SimpleTool) {
                            tool = (SimpleTool) sub_children.get(k);
                        } else {
                            tool = ((ToolGroup)sub_children.get(k)).getTool();
                        }

                        if (tool != null) {
                            if (level == 0)
                                menuItem = new JMenuItem(tool.getName());
                            else
                                menuItem = new JMenuItem(tool.getName(),
                                        new FOUOIcon());

                            miMap.put(menuItem, tool);
                            menuItem.addActionListener(this);
                            submenu.add(menuItem);
                        }
                    }

                    popup.add(submenu);
                } else {
                    errorReporter.messageReport("Invalid item in Tools: " + o);
                }
            }

            toolsMap.put(name, popup);
        }

        toolbar.removeAll();

        Collection values = toolsMap.entrySet();
        Iterator itr = values.iterator();
        Map.Entry entry;

        if (direction == ToolBar.HORIZONTAL)
            toolbar.setLayout(new GridLayout(1, toolsMap.size()));
        else
            toolbar.setLayout(new GridLayout(toolsMap.size(), 1));

        while (itr.hasNext()) {
            entry = (Map.Entry) itr.next();

            JButton jb = new JButton((String) entry.getKey());
            PopupListener puListener = new PopupListener((JPopupMenu) entry
                    .getValue());
            jb.addMouseListener(puListener);
            toolbar.add(jb);
        }
    }


    /**
     * Register an error reporter with the instance
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

class FOUOIcon implements Icon, SwingConstants {
    private int width = 9;

    private int height = 18;

    private int[] xPoints = new int[4];

    private int[] yPoints = new int[4];

    public FOUOIcon() {
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
