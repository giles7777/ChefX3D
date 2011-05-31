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
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;

// Local imports
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.CatalogManagerListener;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogListener;

import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.tool.ToolGroupChild;
import org.chefx3d.tool.ToolGroupEvent;
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.tool.ToolGroupListener;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.view.ViewManager;

/**
 * This outlookToolbar will use icons and a stack outlookToolbar to display
 * items. Only one group will be dsiplayed at a time.
 *
 * @author Russell Dodds
 * @version $Revision: 1.20 $
 */
public class SingleGroupToolBar
    implements 
        ToolBar, 
        CatalogManagerListener, 
        CatalogListener, 
        ToolGroupListener {

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

    /** List of all the current tool panels */
    private FlatToolGroupIconPanel toolPanel;
    
    /** The group that we should be watching for changes on */
    private String groupName;
    
    /** If true then the setTool method will unselect the tool */
    private boolean unselectTool;
    
    /** Catalog filter for controlling access to products */
    private CatalogFilter catalogFilter;

    /** The tool group being displayed */
    private ToolGroup displayGroup;
    
    /**
     * Construct a new instance of the toolbar with the option to show or
     * hide the ToolGroup's tool.
     *
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     * @param groupName the name of the group to display
     * @param catalogFilter Filter to use on the displayed catalog, can be null
     */
    public SingleGroupToolBar(
            WorldModel model, 
            CatalogManager catalogManager, 
            String groupName, 
            boolean unselectTool,
            boolean areEnabled, 
            CatalogFilter catalogFilter, 
            Color backgroundColor, 
            Color selectedColor, 
            Color highlightColor, 
            boolean displayHoverover) {
        
        this.model = model;
        this.groupName = groupName;
        this.unselectTool = unselectTool;
        this.catalogFilter = catalogFilter;
        
        viewManager = ViewManager.getViewManager();
        this.catalogManager = catalogManager;
        catalogManager.addCatalogManagerListener(this);
        
        toolPanel = new FlatToolGroupIconPanel(
                model, 
                catalogFilter, 
                false, 
                3, 
                new Dimension(80, 80), 
                true, 
                0.95f, 
                areEnabled, 
                backgroundColor, 
                selectedColor, 
                highlightColor, 
                displayHoverover);
       
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
        toolPanel.setTool(tool);
        
        boolean isDisplayed = false;
        if (tool != null && 
                displayGroup != null && 
                displayGroup.getTool(tool.getToolID()) != null) {
            isDisplayed = true;
        }
        
        if (unselectTool && isDisplayed) {
            toolPanel.setTool(null);                
            ViewManager.getViewManager().setTool(null);          
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
        return toolPanel;
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
            catalog.removeCatalogListener(this);
        }
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
        ToolGroupChild tool = (ToolGroupChild)evt.getChild();
        ToolGroupChild parent = tool.getParent();
        
        if (parent != null && groupName == parent.getName()) {
            toolPanel.repaint();
        }
    }

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupAdded(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getChild();
        addToolPanel(group);
    }

    /**
     * A tool has been removed. Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolRemoved(ToolGroupEvent evt) {
        // not implemented
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupRemoved(ToolGroupEvent evt) {
        // not implemented
    }
    
    /**
     * A tool has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolUpdated(ToolGroupEvent evt) {   
        // not implemented
    }

    /**
     * A tool has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupUpdated(ToolGroupEvent evt) {
        // not implemented
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
    public void setCatalog(String catalogName) {
        this.catalogName = catalogName;
    }

    /**
     * Add a new panel for the given tool group.
     *
     * @param group The group to add the panel for
     */
    public void addToolPanel(ToolGroup group) {
               
        group.addToolGroupListener(this);
        
        String name = group.getName();        
        if (groupName.equals(name)) {
            displayGroup = group;
            
            catalogFilter.setCurrentToolGroup(model, displayGroup, toolPanel);
            toolPanel.setRootToolGroup(group);
            toolPanel.repaint();
        }
                    
    }

    /**
     * Remove the panel that belongs to the given tool group.
     *
     * @param group The group to remove the panel of
     */
    public void removeToolPanel(ToolGroup group) {
        
        String name = group.getName();
        if (groupName == name) {
            toolPanel.removeAll();
        }

    }
        
}
