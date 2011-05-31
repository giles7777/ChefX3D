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
import java.util.*;

// Listed explicity to avoid clashes with java.util.List
import java.awt.Container;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

import com.l2fprod.common.swing.JOutlookBar;

// Local imports
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.CatalogManagerListener;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogListener;

import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.ViewManager;

/**
 * This outlookToolbar will use icons and a stack outlookToolbar to display
 * items. Only one group will be dsiplayed at a time.
 *
 * @author Russell Dodds
 * @version $Revision: 1.18 $
 */
public class IconToolBar
    implements ToolBar, CatalogManagerListener, CatalogListener {

    /** The panel to place the catalog items */
    private JOutlookBar outlookToolbar;

    /** The world model */
    private WorldModel model;

    /** The view manager */
    private ViewManager viewManager;

    /** The catalog manager */
    private CatalogManager catalogManager;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The catalog that we should be watching for changes on */
    private String catalogName;

    /** Map from the root tool group name to the holding panel */
    private HashMap<String, FlatToolGroupIconPanel> toolnameToPanelMap;

    /** List of all the current tool panels */
    private ArrayList<FlatToolGroupIconPanel> toolPanels;

    /** True to show the tool group's tool */
    private final boolean showToolGroupTools;

    private CatalogFilter catalogFilter;

    /**
     * Create a new tool bar that does not show the tool group tools.
     */
    public IconToolBar(WorldModel model, CatalogManager catalogManager) {
        this(model, catalogManager, false);
    }

    /**
     * Construct a new instance of the toolbar with the option to show or
     * hide the ToolGroup's tool.
     *
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     */
    public IconToolBar(
    		WorldModel model, 
    		CatalogManager catalogManager, 
    		boolean showToolGroupTools) {
    	
        this.model = model;
        this.showToolGroupTools = showToolGroupTools;
        this.catalogManager = catalogManager;

        viewManager = ViewManager.getViewManager();
        catalogManager.addCatalogManagerListener(this);

        outlookToolbar = new JOutlookBar();
        toolnameToPanelMap = new HashMap<String, FlatToolGroupIconPanel>();
        toolPanels = new ArrayList<FlatToolGroupIconPanel>();

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        ToolBarManager.getToolBarManager().addToolBar(this);
    }

    //----------------------------------------------------------
    // Methods defined by Toolbar
    //----------------------------------------------------------

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        for(int i = 0; i < toolPanels.size(); i++) {
            FlatToolGroupIconPanel panel = toolPanels.get(i);
            panel.setTool(tool);
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

    //----------------------------------------------------------
    // Methods defined by AuthoringComponent
    //----------------------------------------------------------

    /**
     * Get the component used to render this.
     *
     * @return The component
     */
    public Object getComponent() {
        return outlookToolbar;
    }

    //----------------------------------------------------------
    // Methods defined by ActionListener
    //----------------------------------------------------------

    /**
     * Process an action event from one of the icon buttons.
     *
     * @param e The event that caused this method to be called
     */
    public void actionPerformed(ActionEvent e) {

        String toolName = e.getActionCommand();

        if (toolName != null) {
            Tool tool = catalogManager.findTool(toolName);
            viewManager.setTool(tool);
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
        if(!name.equals(catalogName))
            return;

        addToolPanel(group);
    }

    /**
     * A group of tool groups have been added.
     *
     * @param name The catalog name
     * @param groups The list of tool groups added
     */
    public void toolGroupsAdded(String name, List<ToolGroup> groups) {
        if(!name.equals(catalogName))
            return;

        for(int i = 0; i < groups.size(); i++)
            addToolPanel(groups.get(i));
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupRemoved(String name, ToolGroup group) {
        if(!name.equals(catalogName))
            return;

        removeToolPanel(group);
    }

    /**
     * A group of tool groups have been removed.
     *
     * @param name The catalog name
     * @param groups The list of tool groups that have been removed
     */
    public void toolGroupsRemoved(String name, List<ToolGroup> groups) {
        if(!name.equals(catalogName))
            return;

        for(int i = 0; i < groups.size(); i++)
            removeToolPanel(groups.get(i));
    }

    // ----------------------------------------------------------
    // Methods defined by CatalogManagerListener
    // ----------------------------------------------------------

    /**
     * A catalog has been added.
     *
     * @param catalog
     */
    public void catalogAdded(Catalog catalog) {
        if(catalogName == null)
            return;

        if(catalogName.equals(catalog.getName()))
            catalog.addCatalogListener(this);
    }

    /**
     * A catalog has been removed.
     *
     * @param catalog
     */
    public void catalogRemoved(Catalog catalog) {
        if(catalogName == null)
            return;

        // If the catalog that we're watching is removed, clean out the
        // outlookToolbar.
        if(catalogName.equals(catalog.getName())) {
            outlookToolbar.removeAll();
            toolnameToPanelMap.clear();
            catalog.removeCatalogListener(this);
        }
    }


    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Change which catalog that this outlookToolbar should be watching for changes
     * on.
     *
     * @param catalogName The name of the catalog to watch.
     */
    public void initCatalog(String catalogName, CatalogFilter catalogFilter) {
        this.catalogName = catalogName;
        this.catalogFilter = catalogFilter;
    }

    /**
     * Add a new panel for the given tool group.
     *
     * @param group The group to add the panel for
     */
    private void addToolPanel(ToolGroup group) {
        
        FlatToolGroupIconPanel panel = 
            new FlatToolGroupIconPanel(model, group, catalogFilter, false);

        String name = group.getName();

        toolnameToPanelMap.put(name, panel);
        toolPanels.add(panel);

        JPanel makeTop = new JPanel(new BorderLayout());
        makeTop.add(panel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(makeTop);
        scroll.setHorizontalScrollBarPolicy(
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        outlookToolbar.addTab(name, scroll);
    }

    /**
     * Remove the panel that belongs to the given tool group.
     *
     * @param group The group to remove the panel of
     */
    private void removeToolPanel(ToolGroup group) {
        String name = group.getName();

        JPanel panel = toolnameToPanelMap.get(name);
        toolnameToPanelMap.remove(name);
        toolPanels.remove(panel);

        // Need to go up 2 panels to get to the scroll pane.
        Container scrollPane = panel.getParent().getParent();
        outlookToolbar.remove(scrollPane);

    }
}
